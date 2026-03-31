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
        
        EnemyData enemy = loadEnemyData(enemyId);
        if (enemy == null) {
            ui.print("【エラー】敵データの読み込みに失敗しました: " + enemyId);
            return;
        }
        state.setCurrentEnemy(enemy);
        
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
                    
                    state.incrementTurn();
                }
            }

            // 戦闘終了時にUIを通常モードに戻す
            jfxUi.setBattleMode(false);
            ui.showImage("enemy", ""); // 敵画像を消去（非表示にする）
        }
    }

    private boolean processPlayerTurn(BattleCommand cmd, EnemyData enemy) {
        CombatBaseRules baseRules = CombatDataLoader.getBaseRules();

        // [1] スタンス（頭）の適用：アビリティの決定
        String abilityId = "basic_attack"; // デフォルト
        
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

        // [2] ムーブ（足）の解決：距離の確定
        String move = cmd.getMove();
        if ("前進".equals(move)) {
            state.setDistance(Math.max(0, state.getDistance() - 1));
            ui.print("　プレイヤーは前進した。（現在距離: " + state.getDistance() + "）");
        } else if ("後退".equals(move)) {
            state.setDistance(Math.min(4, state.getDistance() + 1));
            ui.print("　プレイヤーは後退した。（現在距離: " + state.getDistance() + "）");
        }

        // [3] アクション（腕）の解決：確定した距離に基づく判定
        String action = cmd.getAction();
        if ("逃げる".equals(action)) {
            ui.print("　戦闘から無事に逃走した！");
            return true; 
        }

        if ("攻撃".equals(action)) {
            // 距離による結果（HIT/MISS/BONUS）の判定
            String rangeResult = baseRules.getRangeResult(ability.getType(), state.getDistance());
            
            if ("MISS".equals(rangeResult)) {
                ui.print("　ミス！距離が適していません。 (" + ability.getName() + ")");
                return false;
            }

            // 命中判定（防御側ステータスが未設定の場合は必中）
            int atkStatVal = getCombatStat(player, ability.getCheck().getAttackerStat());
            String defStatName = ability.getCheck().getDefenderStat();
            
            boolean isHit;
            if (defStatName == null || defStatName.isEmpty()) {
                isHit = true; // 防御側ステータスの指定がなければ必中
            } else {
                int defStatVal = enemy.getStatByName(defStatName);
                Integer overrideChance = ability.getCheck().getBaseChance();
                isHit = checkHit(atkStatVal, defStatVal, overrideChance);
            }

            if (isHit) {
                // ダメージ計算: ダイス(未指定・WEAPONなら武器ダイス=現在1d4) + ステータス
                String dice = ability.getCheck().getDamageDice();
                com.kh.tbrr.data.models.Item weapon = null;
                if (dice == null || dice.isEmpty() || "WEAPON".equalsIgnoreCase(dice)) {
                    dice = "1d4"; // 素手/武器フォールバック（将来武器データから取得）
                    
                    // 装備中のメイン武器があれば、そのダイスで上書き
                    String mainWeaponId = player.getEquippedMainWeapon();
                    if (mainWeaponId != null) {
                        weapon = com.kh.tbrr.data.ItemRegistry.getItemById(mainWeaponId);
                        if (weapon != null && weapon.getDamageDice() != null && !weapon.getDamageDice().isEmpty()) {
                            dice = weapon.getDamageDice();
                        }
                    }
                }
                int diceRoll = DiceRoller.roll(dice);
                
                // --- マスタリーの計算 ---
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
                masteryLevel = Math.min(10, masteryLevel); // 上限10
                
                int masteryDiceSum = 0;
                for (int i = 0; i < masteryLevel; i++) {
                    masteryDiceSum += DiceRoller.roll("1d4"); // Lv1毎に1d4追加
                }
                
                int masteryFixedBonus = 0;
                if (masteryLevel > 0) {
                    // 指数関数的な固定ダメージ (0, 2, 5, 9, 14, 20...)
                    masteryFixedBonus = (masteryLevel - 1) * (masteryLevel + 2) / 2;
                }

                // ステータス加算計算（暫定的に0.5倍を適用）
                int rawStatVal = getCombatStat(player, ability.getCheck().getScalingStat());
                int scalingStatVal = (int)(rawStatVal * 0.5);
                int baseDamage = diceRoll + masteryDiceSum + masteryFixedBonus + scalingStatVal;

                // 距離ボーナス (+2)
                int bonus = "BONUS".equals(rangeResult) ? baseRules.getDamage().getDistanceBonusValue() : 0;
                int totalDamage = baseDamage + bonus;

                enemy.setHp(enemy.getHp() - totalDamage);
                
                String diceMsg = "(基礎ダイス:" + diceRoll + (masteryLevel > 0 ? " + 習熟追加" + masteryLevel + "d4" : "") + " + 習熟固定:" + masteryFixedBonus + " + ステ修正:" + scalingStatVal + ")";
                String bonusMsg = bonus > 0 ? " [距離ボーナス+" + bonus + "]" : "";
                ui.print("　命中！ " + enemy.getName() + " に " + totalDamage + " のダメージ！ " + diceMsg + bonusMsg);
            } else {
                ui.print("　かわされた！（ミス！）");
            }
        }
        return false;
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
            if (checkHit(enemy.getFinesse(), player.getCombatStats().finesse(), null)) {
                int diceRoll = DiceRoller.roll("1d4");
                
                // 敵もベースルールの全局補正を適用
                int scalingStatVal = (int)(enemy.getMight() * baseRules.getGlobalStatScaling());
                int totalDamage = diceRoll + scalingStatVal + ("BONUS".equals(rangeResult) ? baseRules.getDamage().getDistanceBonusValue() : 0);
                
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

    private boolean checkHit(int attackerStat, int defenderStat, Integer overrideBaseChance) {
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
        return random.nextInt(100) + 1 <= hitChance;
    }

}
