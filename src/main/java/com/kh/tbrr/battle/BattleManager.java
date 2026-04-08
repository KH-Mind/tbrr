package com.kh.tbrr.battle;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.ui.GameUI;
import com.kh.tbrr.battle.data.*;
import com.google.gson.Gson;

public class BattleManager {
    private GameUI ui;
    private Player player;
    private BattleState state;
    private Random random;

    public BattleManager(GameUI ui, Player player) {
        this.ui = ui;
        this.player = player;
        this.random = new Random();
    }

    public String getDeathCause() {
        if (state != null && state.getCurrentEnemy() != null) {
            String cause = state.getCurrentEnemy().getDeathCause();
            return (cause != null && !cause.isEmpty()) ? cause : "generic";
        }
        return "generic";
    }

    private EnemyData loadEnemyData(String enemyId) {
        try {
            String path = "/data/enemies/" + enemyId + ".json";
            InputStream is = getClass().getResourceAsStream(path);
            if (is != null) {
                Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                return new Gson().fromJson(reader, EnemyData.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void startBattle(String enemyId) {
        state = new BattleState();
        com.kh.tbrr.data.CombatConditionRegistry.loadAll(); // 戦闘用状態異常データの読み込み
        
        EnemyData enemy = loadEnemyData(enemyId);
        if (enemy == null) {
            ui.print("【エラー】敵データの読み込みに失敗しました: " + enemyId);
            return;
        }
        state.setCurrentEnemy(enemy);
        if (enemy.getInitialCombatConditions() != null) {
            for (var cond : enemy.getInitialCombatConditions()) {
                state.getEnemyConditions().add(new BattleState.ActiveCombatCondition(cond.getConditionId(), cond.getDuration()));
            }
        }
        
        ui.print("【バトル開始！】 " + enemy.getName() + " に遭遇した！");

        if (ui instanceof com.kh.tbrr.ui.JavaFXUI) {
            com.kh.tbrr.ui.JavaFXUI jfxUi = (com.kh.tbrr.ui.JavaFXUI) ui;
            jfxUi.setBattleMode(true);

            // 背景と敵画像の表示
            if(enemy.getBattleBackground() != null && !enemy.getBattleBackground().isEmpty()) {
                ui.showImage("background", enemy.getBattleBackground());
            } else {
                ui.showImage("background", "bg_000.png"); // デフォルトの戦闘背景
            }
            if(enemy.getImagePath() != null && !enemy.getImagePath().isEmpty()) {
                ui.showImage("enemy", enemy.getImagePath());
            }

            boolean battleEnded = false;
            while (!battleEnded) {
                ui.print(" ");
                ui.print("--- ターン " + state.getTurnCount() + " --- [現在距離: " + state.getDistance() + "]");
                ui.print("【敵】" + enemy.getName() + " (HP: " + enemy.getHp() + "/" + enemy.getMaxHp() + ")");
                ui.print("コマンドを選択してください。");
                
                BattleCommand cmd = jfxUi.getBattleCommand();
                if (cmd != null) {
                    ui.print("＞プレイヤーの行動: " + cmd.toString());

                    // --- プレイヤーのターン処理 ---
                    boolean escapeSuccess = processPlayerTurn(cmd, enemy);
                    if (escapeSuccess) {
                    	// 逃走完了
                        battleEnded = true;
                        continue;
                    }

                    // 敵の死亡判定
                    if (enemy.getHp() <= 0) {
                        ui.print("【勝利！】 " + enemy.getName() + " を倒した！");
                        battleEnded = true;
                        continue;
                    }

                    // --- 敵のターン処理 ---
                    processEnemyTurn(enemy);
                    
                    // プレイヤーの死亡判定
                    if (player.getHp() <= 0) {
                        ui.print("【敗北】 プレイヤーのHPが0になった……");
                        battleEnded = true;
                        continue;
                    }
                    
                    // --- ターン終了処理（ステータス更新） ---
                    updateConditions(state.getPlayerConditions());
                    updateConditions(state.getEnemyConditions());
                    
                    state.incrementTurn();
                }
            }

            // 戦闘終了時にUIを通常モードに戻す
            jfxUi.setBattleMode(false);
            ui.showImage("enemy", ""); // 敵画像を消去（非表示にする）
        }
    }

    private boolean processPlayerTurn(BattleCommand cmd, EnemyData enemy) {
        // [0] ターン開始時に自身の防御状態を解除
        state.setPlayerDefending(false);
        // [0.5] 今回のターンのスタンス(構え)を保持（相手ターンの割り込み等で使用）
        state.setCurrentPlayerStance(cmd.getStance() != null ? cmd.getStance() : "なし");
        
        CombatBaseRules baseRules = CombatDataLoader.getBaseRules();

        // [1] スタンス（頭）と技（特殊）の適用：アビリティの決定
        String abilityId = "basic_attack"; // デフォルト
        
        // 技(special)が指定されていた場合、そちらを優先する（"なし"以外）
        if (cmd.getSpecial() != null && !cmd.getSpecial().equals("なし")) {
            if (player.getAbilities() != null) {
                for (String id : player.getAbilities()) {
                    AbilityData data = CombatDataLoader.getAbility(id);
                    if (data != null && data.getName().equals(cmd.getSpecial())) {
                        abilityId = id;
                        break;
                    }
                }
            }
        }
        
        // 画面(UI)から送信される「日本語名称」を内部IDへ変換（※将来的にUIが直接IDを投げるようになれば不要になる仮の処理）
        String stanceName = cmd.getStance();
        String stanceId = switch (stanceName != null ? stanceName : "") {
            case "魔法攻撃" -> "magic_stance";
            case "射撃の名手" -> "shooting_stance";
            default -> null;
        };

        StanceData stanceData = stanceId != null ? CombatDataLoader.getStance(stanceId) : null;
        
        if (stanceData != null) {
            if (stanceData.getApCost() > 0) {
                player.modifyAp(-stanceData.getApCost());
            }
            if (stanceData.getMessage() != null && !stanceData.getMessage().isEmpty()) {
                ui.print(stanceData.getMessage());
            }
            if (stanceData.getOverrideAbilityId() != null && !stanceData.getOverrideAbilityId().isEmpty()) {
                abilityId = stanceData.getOverrideAbilityId();
            }
        }

        AbilityData ability = CombatDataLoader.getAbility(abilityId);
        if (ability == null) {
            ui.print("【エラー】アビリティデータが見つかりません: " + abilityId);
            return false;
        }

        // 武器オブジェクトを射程判定より前に取得しておく
        com.kh.tbrr.data.models.Item weapon = null;
        String mainWeaponId = player.getEquippedMainWeapon();
        if (mainWeaponId != null) {
            weapon = com.kh.tbrr.data.ItemRegistry.getItemById(mainWeaponId);
        }

        // [2] ムーブ（足）の解決：距離の確定
        String move = cmd.getMove();
        int moveAmount = 1;
        if ("全力移動".equals(cmd.getAction())) {
            moveAmount = 2; // 全力移動なら2マス動く
        }

        if ("前進".equals(move)) {
            state.setDistance(Math.max(0, state.getDistance() - moveAmount));
            ui.print("　プレイヤーは前進した。（現在距離: " + state.getDistance() + "）");
        } else if ("後退".equals(move)) {
            state.setDistance(Math.min(4, state.getDistance() + moveAmount));
            ui.print("　プレイヤーは後退した。（現在距離: " + state.getDistance() + "）");
        }

        // [3] アクション（腕）の解決：確定した距離に基づく判定
        String action = cmd.getAction();
        if ("逃げる".equals(action)) {
            ui.print("　戦闘から無事に逃走した！");
            return true; 
        }

        if ("防御".equals(action)) {
            state.setPlayerDefending(true);
            ui.print("　身を固めた。（次の自身のターンまで回避率上昇、被ダメージ半減）");
            return false;
        }

        if ("全力移動".equals(action)) {
            ui.print("　攻撃を行わず、全力で機動した。");
            return false;
        }

        if ("攻撃".equals(action) || (cmd.getSpecial() != null && !cmd.getSpecial().equals("なし"))) {
            // 距離による結果（HIT/MISS/BONUS）の判定
            String rangeResult = resolveRangeResult(baseRules, ability, weapon, stanceData, state.getDistance());

            if ("MISS".equals(rangeResult)) {
                ui.print("　ミス！距離が適していません。 (" + ability.getName() + ")");
                return false;
            }

            // 命中判定（防御側ステータスが未設定の場合は必中）
            int atkStatVal = getCombatStat(player, ability.getCheck().getAttackerStat());
            String defStatName = ability.getCheck().getDefenderStat();
            
            HitResult result;
            if (defStatName == null || defStatName.isEmpty()) {
                result = new HitResult(true, false); // 防御側ステータスの指定がなければ必中
            } else {
                int defStatVal = enemy.getStatByName(defStatName);
                Integer overrideChance = ability.getCheck().getBaseChance();
                result = checkHit(atkStatVal, defStatVal, overrideChance, state.isEnemyDefending(), state.getPlayerConditions(), state.getEnemyConditions());
            }

            if (result.isHit) {
                // ダイス計算: 未指定・WEAPONなら武器ダイスを使用
                String dice = ability.getCheck().getDamageDice();
                if (dice == null || dice.isEmpty() || "WEAPON".equalsIgnoreCase(dice)) {
                    dice = "1d4"; // 素手/武器フォールバック
                    if (weapon != null && weapon.getDamageDice() != null && !weapon.getDamageDice().isEmpty()) {
                        dice = weapon.getDamageDice();
                    }
                }
                int diceRoll = DiceRoller.roll(dice);
                
                // --- マスタリーの計算 ---
                int masteryLevel = calculateMasteryLevel(weapon);
                int masteryDiceSum = calculateMasteryDice(masteryLevel);
                int masteryFixedBonus = calculateMasteryFixedBonus(masteryLevel);

                // ステータス加算計算（暫定的に0.5倍を適用）
                int rawStatVal = getCombatStat(player, ability.getCheck().getScalingStat());
                int scalingStatVal = (int)(rawStatVal * 0.5);
                int baseDamage = diceRoll + masteryDiceSum + masteryFixedBonus + scalingStatVal;

                // クリティカル処理（BONUS判定 または ダイス1〜5）
                boolean isCritical = "BONUS".equals(rangeResult) || result.isCritical;
                double critMult = isCritical ? resolveCritMultiplier(player) : 1.0;
                
                // CombatCondition によるダメージ倍率の適用
                double conditionMult = calcConditionDamageMultiplier(state.getPlayerConditions());
                int totalDamage = (int)(baseDamage * critMult * conditionMult);

                if (state.isEnemyDefending()) {
                    totalDamage /= 2; // 防御中はダメージ半減
                }

                enemy.setHp(enemy.getHp() - totalDamage);
                
                String diceMsg = "(基礎ダイス:" + diceRoll + (masteryLevel > 0 ? " + 習熟追加" + masteryLevel + "d4" : "") + " + 習熟固定:" + masteryFixedBonus + " + ステ修正:" + scalingStatVal + ")";
                String critMsg = isCritical ? " 【クリティカル！" + critMult + "倍】" : "";
                ui.print("　命中！ " + enemy.getName() + " に " + totalDamage + " のダメージ！ " + diceMsg + critMsg);

                // --- CombatCondition付与の処理 ---
                if (ability.getApplyCombatConditions() != null) {
                    for (var app : ability.getApplyCombatConditions()) {
                        int r = random.nextInt(100) + 1; // 1-100
                        if (r <= app.getChance()) {
                            boolean found = false;
                            for (var c : state.getEnemyConditions()) {
                                if (c.getConditionId().equals(app.getConditionId())) {
                                    c.setDuration(app.getDuration()); // 上書き
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                state.getEnemyConditions().add(new BattleState.ActiveCombatCondition(app.getConditionId(), app.getDuration()));
                            }
                            com.kh.tbrr.data.models.CombatConditionData cData = com.kh.tbrr.data.CombatConditionRegistry.getConditionById(app.getConditionId());
                            if (cData != null) {
                                ui.print("　★ " + enemy.getName() + " は [" + cData.getName() + "] になった！");
                            }
                        }
                    }
                }
            } else {
                ui.print("　かわされた！（ミス！）");
            }
        }
        return false;
    }

    private int calculateMasteryLevel(com.kh.tbrr.data.models.Item weapon) {
        int masteryLevel = 0;
        if (weapon != null && weapon.getTags() != null) {
            for (String passiveId : player.getPassives()) {
                com.kh.tbrr.data.models.PassiveData passive = com.kh.tbrr.data.PassiveRegistry.getPassiveById(passiveId);
                if (passive != null && "MASTERY".equals(passive.getType()) && passive.getTargetTags() != null) {
                    boolean match = weapon.getTags().stream().anyMatch(tag -> passive.getTargetTags().contains(tag));
                    if (match) {
                        masteryLevel += passive.getLevel();
                    }
                }
            }
        }
        return Math.min(10, masteryLevel);
    }

    private int calculateMasteryDice(int masteryLevel) {
        int sum = 0;
        for (int i = 0; i < masteryLevel; i++) {
            sum += DiceRoller.roll("1d4");
        }
        return sum;
    }

    private int calculateMasteryFixedBonus(int masteryLevel) {
        if (masteryLevel <= 0) return 0;
        return (masteryLevel - 1) * (masteryLevel + 2) / 2;
    }

    private String resolveRangeResult(CombatBaseRules rules, AbilityData ability,
            com.kh.tbrr.data.models.Item weapon, StanceData stance, int distance) {
        // 1. 武器の rangeOverride が存在する場合は最優先
        if (weapon != null && weapon.getRangeOverride() != null) {
            String custom = weapon.getRangeOverride().get(String.valueOf(distance));
            if (custom != null) return custom;
        }
        // 2. スタンスがアビリティを差し替えた場合は、その ability.getType() を使用
        boolean abilityOverridden = stance != null
                && stance.getOverrideAbilityId() != null
                && !stance.getOverrideAbilityId().isEmpty();
        if (abilityOverridden) {
            return rules.getRangeResult(ability.getType(), distance);
        }
        // 3. ノースタンスの場合は武器の rangeType を使用（未指定は melee デフォルト）
        String weaponRange = (weapon != null && weapon.getRangeType() != null)
                ? weapon.getRangeType() : "melee";
        return rules.getRangeResult(weaponRange, distance);
    }

    private double resolveCritMultiplier(Player p) {
        CombatBaseRules rules = CombatDataLoader.getBaseRules();
        double base = rules.getDamage().getCritMultiplier(); // デフォルト 1.5
        // CRIT_MULTIPLIER型パッシブが存在する場合、最大値で上書き
        double override = p.getPassives().stream()
                .map(id -> com.kh.tbrr.data.PassiveRegistry.getPassiveById(id))
                .filter(passive -> passive != null
                        && "CRIT_MULTIPLIER".equals(passive.getType())
                        && passive.getCritMultiplier() > 0)
                .mapToDouble(com.kh.tbrr.data.models.PassiveData::getCritMultiplier)
                .max()
                .orElse(base);
        return override;
    }

    private int getCombatStat(Player p, String statName) {
        if (statName == null || statName.isEmpty()) return 0;
        
        var stats = p.getCombatStats();
        return switch (statName.toLowerCase()) {
            case "might" -> stats.might();
            case "insight" -> stats.insight();
            case "finesse" -> stats.finesse();
            case "presence" -> stats.presence();
            case "sensuality" -> stats.sensuality();
            default -> 0;
        };
    }

    // 敵のターン処理（プレイヤーと同様のロジックに今後統合予定だが、一旦素手1d4ロジックのみ適用）
    private void processEnemyTurn(EnemyData enemy) {
        state.setEnemyDefending(false); // 敵ターン開始時に自身の防御状態を解除
        
        // --- 相手のスタンスによる特殊トリガー（対抗呪文など）の判定枠 ---
        String pStanceStr = state.getCurrentPlayerStance();
        // TODO: ここで特注の割り込み処理（魔法の無効化など）を記述する予定
        
        ui.print("＞敵の行動: [" + enemy.getName() + " の攻撃]");
        CombatBaseRules baseRules = CombatDataLoader.getBaseRules();
        
        int distance = state.getDistance();
        
        // 敵AI：距離2以上なら詰め、1以下なら攻撃
        if (distance >= 2) {
            state.setDistance(Math.max(0, distance - 1));
            ui.print("　" + enemy.getName() + " は詰め寄ってきた。（現在距離: " + state.getDistance() + "）");
        } else {
            // 基本素手攻撃（basic_attack相当 / 敵も1d4+強靭）
            AbilityData ability = CombatDataLoader.getAbility("basic_attack");
            if (ability == null) {
                ui.print("【エラー】敵のアビリティデータが見つかりません: basic_attack");
                return;
            }
            String rangeResult = baseRules.getRangeResult(ability.getType(), distance);

            // 敵も「機敏vs機敏」で命中判定を行う（ユーザー原案準拠）
            HitResult result = checkHit(enemy.getFinesse(), player.getCombatStats().finesse(), null, state.isPlayerDefending(), state.getEnemyConditions(), state.getPlayerConditions());
            if (result.isHit) {
                int diceRoll = DiceRoller.roll("1d4");
                
                // 敵もベースルールの全局補正を適用
                int scalingStatVal = (int)(enemy.getMight() * baseRules.getGlobalStatScaling());
                boolean enemyCrit = "BONUS".equals(rangeResult) || result.isCritical;
                double conditionMult = calcConditionDamageMultiplier(state.getEnemyConditions());
                int totalDamage = (int)((diceRoll + scalingStatVal) * (enemyCrit ? baseRules.getDamage().getCritMultiplier() : 1.0) * conditionMult);
                
                // アクセサリによるダメージ軽減
                int reduction = 0;
                if (player.getEquippedAccessories() != null) {
                    for (String accId : player.getEquippedAccessories()) {
                        com.kh.tbrr.data.models.Item acc = com.kh.tbrr.data.ItemRegistry.getItemById(accId);
                        if (acc != null) {
                            reduction += acc.getDamageReduction();
                        }
                    }
                }
                totalDamage = Math.max(0, totalDamage - reduction);
                
                if (state.isPlayerDefending()) {
                    totalDamage /= 2; // プレイヤーが防御していればダメージ半減
                }
                
                player.modifyHp(-totalDamage);
                
                String playerName = player.getName() != null ? player.getName() : "冒険者";
                String reduceMsg = reduction > 0 ? "（" + reduction + "ダメージ軽減）" : "";
                ui.print("　" + enemy.getName() + " の攻撃！ " + playerName + " に " + totalDamage + " のダメージ！" + reduceMsg);
                ui.printPlayerStatus(player); // 右パネルのHP/AP表示を更新
            } else {
                ui.print("　回避した！");
            }
        }
    }

    private HitResult checkHit(int attackerStat, int defenderStat, Integer overrideBaseChance, boolean targetDefending, 
            java.util.List<BattleState.ActiveCombatCondition> atkConds, java.util.List<BattleState.ActiveCombatCondition> defConds) {
        CombatBaseRules rules = CombatDataLoader.getBaseRules();
        int base = overrideBaseChance != null ? overrideBaseChance : rules.getAccuracy().getBaseChance();
        int diff = attackerStat - defenderStat;
        
        int hitChance = base;
        for (var mod : rules.getAccuracy().getModifiers()) {
            int min = mod.getDiff().get(0);
            int max = mod.getDiff().get(1);
            if (diff >= min && diff <= max) {
                hitChance += mod.getBonus();
                break;
            }
        }

        hitChance = Math.max(rules.getAccuracy().getMin(), Math.min(rules.getAccuracy().getMax(), hitChance));
        
        if (targetDefending) {
            hitChance -= 20; // 防御中は命中率自体を引き下げる
        }
        // CombatConditionの命中・回避補正を適用
        hitChance += calcConditionAccuracyBonus(atkConds);
        hitChance -= calcConditionAvoidanceBonus(defConds);
        
        int roll = random.nextInt(100) + 1; // 1 〜 100
        
        // 96〜100 はファンブル（絶対ミス）
        if (roll >= 96) {
            return new HitResult(false, false);
        }
        // 1〜5 はクリティカル（絶対命中・BONUS同様のクリティカル倍率）
        if (roll <= 5) {
            return new HitResult(true, true);
        }
        
        return new HitResult(roll <= hitChance, false);
    }

    private void updateConditions(java.util.List<BattleState.ActiveCombatCondition> conditions) {
        java.util.Iterator<BattleState.ActiveCombatCondition> it = conditions.iterator();
        while (it.hasNext()) {
            BattleState.ActiveCombatCondition c = it.next();
            if (c.getDuration() > 0) {
                c.decrementDuration();
                if (c.getDuration() == 0) {
                    it.remove(); // 効果切れ
                }
            }
        }
    }

    private int calcConditionAccuracyBonus(java.util.List<BattleState.ActiveCombatCondition> conditions) {
        if (conditions == null) return 0;
        int bonus = 0;
        for (BattleState.ActiveCombatCondition c : conditions) {
            com.kh.tbrr.data.models.CombatConditionData data = com.kh.tbrr.data.CombatConditionRegistry.getConditionById(c.getConditionId());
            if (data != null && data.getModifiers() != null) {
                bonus += data.getModifiers().getAccuracyBonus();
            }
        }
        return bonus;
    }

    private int calcConditionAvoidanceBonus(java.util.List<BattleState.ActiveCombatCondition> conditions) {
        if (conditions == null) return 0;
        int bonus = 0;
        for (BattleState.ActiveCombatCondition c : conditions) {
            com.kh.tbrr.data.models.CombatConditionData data = com.kh.tbrr.data.CombatConditionRegistry.getConditionById(c.getConditionId());
            if (data != null && data.getModifiers() != null) {
                bonus += data.getModifiers().getAvoidanceBonus();
            }
        }
        return bonus;
    }

    private double calcConditionDamageMultiplier(java.util.List<BattleState.ActiveCombatCondition> conditions) {
        if (conditions == null) return 1.0;
        double mult = 1.0;
        for (BattleState.ActiveCombatCondition c : conditions) {
            com.kh.tbrr.data.models.CombatConditionData data = com.kh.tbrr.data.CombatConditionRegistry.getConditionById(c.getConditionId());
            if (data != null && data.getModifiers() != null) {
                mult *= data.getModifiers().getDamageMultiplier();
            }
        }
        return mult;
    }

    private static class HitResult {
        public final boolean isHit;
        public final boolean isCritical;
        public HitResult(boolean isHit, boolean isCritical) {
            this.isHit = isHit;
            this.isCritical = isCritical;
        }
    }
}
