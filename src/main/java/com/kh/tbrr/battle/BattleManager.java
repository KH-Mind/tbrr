package com.kh.tbrr.battle;

import com.kh.tbrr.data.models.Item;
import com.kh.tbrr.data.models.CombatConditionData;
import com.kh.tbrr.data.ItemRegistry;
import com.kh.tbrr.data.CombatConditionRegistry;
import com.kh.tbrr.ui.GameUI;
import com.kh.tbrr.ui.JavaFXUI;
import com.kh.tbrr.manager.DataManager;
import com.kh.tbrr.core.GameState;

import java.util.Random;

import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.battle.data.*;

public class BattleManager {

    /** 戦闘の終了結果を表すenum */
    public enum BattleResult {
        VICTORY, // 敵を倒した
        DEFEAT, // 戦闘不能になった
        FLED // 逃走成功
    }

    /** 最後の戦闘結果（EventProcessorから参照する） */
    private BattleResult lastResult = BattleResult.DEFEAT;
    private GameUI ui;
    private Player player;
    private BattleState state;

    private DataManager dataManager;
    private Random random;

    public BattleManager(GameUI ui, Player player, DataManager dataManager) {
        this.ui = ui;
        this.player = player;
        this.dataManager = dataManager;
        this.random = new Random();
    }

    public BattleState getState() {
        return state;
    }

    public String getDeathCause() {
        if (state != null && state.getCurrentEnemy() != null) {
            String cause = state.getCurrentEnemy().getDeathCause();
            return (cause != null && !cause.isEmpty()) ? cause : "generic";
        }
        return "generic";
    }

    public BattleResult getLastResult() {
        return lastResult;
    }

    /**
     * 現在のプレイヤーの恒常特徴（Trait）と、装備由来のTrait、スタンスから得られる一時特徴を合算して返す。
     */
    private java.util.List<TraitData> getActivePlayerTraits() {
        java.util.List<TraitData> list = new java.util.ArrayList<>();
        // getEffectiveTraits() により永続Trait + 装備由来Traitを合算して取得
        for (String id : player.getEffectiveTraits()) {
            TraitData trait = TraitRegistry.getTraitById(id);
            if (trait != null)
                list.add(trait);
        }

        // スタンスからの一時特徴を合算
        if (state != null) {
            String stanceName = state.getCurrentPlayerStance();
            StanceData sd = CombatDataLoader.getStanceByName(stanceName);
            if (sd != null && sd.getGrantedTraitIds() != null) {
                for (String id : sd.getGrantedTraitIds()) {
                    TraitData trait = TraitRegistry.getTraitById(id);
                    if (trait != null)
                        list.add(trait);
                }
            }
        }
        return list;
    }

    /**
     * 現在の敵の特徴（Trait）を返す（将来の敵のスタンス実装等を見据えた一元化）。
     */
    private java.util.List<TraitData> getActiveEnemyTraits() {
        java.util.List<TraitData> list = new java.util.ArrayList<>();
        if (state != null && state.getCurrentEnemy() != null && state.getCurrentEnemy().getTraits() != null) {
            for (String id : state.getCurrentEnemy().getTraits()) {
                TraitData trait = TraitRegistry.getTraitById(id);
                if (trait != null)
                    list.add(trait);
            }
        }
        return list;
    }

    public BattleResult startBattle(String enemyId, GameState gameState) {
        state = new BattleState();
        CombatConditionRegistry.loadAll(); // 戦闘用状態異常データの読み込み
        CombatDataLoader.loadAllTraits(); // 特徴（Trait）データの読み込み（二重読み込み防止済み）
        CombatDataLoader.loadAllStances(); // スタンスデータの読み込み（UI表示名からの自動検索用）

        // 使用可能なスタンスリストを構築してUIに渡す
        java.util.List<String> stanceNames = new java.util.ArrayList<>();
        for (StanceData sd : CombatDataLoader.getAllStances()) {
            if (sd.isDefault() || (player.getStances() != null && player.getStances().contains(sd.getId()))) {
                stanceNames.add(sd.getName());
            }
        }
        ui.updateAvailableStances(stanceNames);

        EnemyData enemy = dataManager.loadEnemyData(enemyId);
        if (enemy == null) {
            ui.print("【エラー】敵データの読み込みに失敗しました: " + enemyId);
            return lastResult;
        }

        // --- 脅威度スケーリングの適用 ---
        // scalingブロックがある場合のみ計算する。ない敵（ボス・固有敵等）は固定強度のまま。
        // threat_level statusEffectには「フロア由来 + イベント由来」の合算値が入っている。
        if (enemy.getScaling() != null) {
            int totalThreat = player.getStatusEffectValue("threat_level");

            if (totalThreat > 0) {
                EnemyData.EnemyScaling sc = enemy.getScaling();
                int newHp = enemy.getMaxHp() + totalThreat * sc.getHpPerLevel();
                enemy.setMaxHp(newHp);
                enemy.setHp(newHp);
                enemy.setMight(enemy.getMight() + totalThreat * sc.getMightPerLevel());
                enemy.setInsight(enemy.getInsight() + totalThreat * sc.getInsightPerLevel());
                enemy.setFinesse(enemy.getFinesse() + totalThreat * sc.getFinessePerLevel());
                enemy.setPresence(enemy.getPresence() + totalThreat * sc.getPresencePerLevel());
                enemy.setSensuality(enemy.getSensuality() + totalThreat * sc.getSensualityPerLevel());
                if (System.getProperty("tbrr.debug") != null) {
                    System.err.println("[SCALING] " + enemy.getName()
                            + " totalThreat=" + totalThreat
                            + " HP=" + newHp + " Might=" + enemy.getMight());
                }
            }
        }

        state.setCurrentEnemy(enemy);
        if (enemy.getInitialCombatConditions() != null) {
            for (var cond : enemy.getInitialCombatConditions()) {
                state.getEnemyConditions()
                        .add(new BattleState.ActiveCombatCondition(cond.getConditionId(), cond.getDuration()));
            }
        }

        // --- SP初期化 ---
        player.setCurrentSp(player.calcInitialSp());
        enemy.setCurrentSp(enemy.getInitialSp());

        ui.print("【バトル開始！】 " + enemy.getName() + " に遭遇した！");

        if (ui instanceof JavaFXUI) {
            JavaFXUI jfxUi = (JavaFXUI) ui;
            jfxUi.setBattleMode(true);
            // サブウィンドウに戦闘情報パネルを表示する
            jfxUi.showBattlePanel();

            jfxUi.setStanceChangeListener(stanceName -> {
                HitChanceDetails pChance = getSimulatedPlayerHitChance(stanceName);
                HitChanceDetails eChance = getSimulatedEnemyHitChance();
                jfxUi.updateBattlePanel(
                        state.getTurnCount(), state.getDistance(),
                        player, enemy,
                        state.getPlayerConditions(), state.getEnemyConditions(),
                        pChance, eChance);
            });

            // 背景と敵画像の表示
            if (enemy.getBattleBackground() != null && !enemy.getBattleBackground().isEmpty()) {
                ui.showImage("background", enemy.getBattleBackground());
            } else {
                ui.showImage("background", "bg_000.png"); // デフォルトの戦闘背景
            }
            if (enemy.getImagePath() != null && !enemy.getImagePath().isEmpty()) {
                ui.showImage("enemy", enemy.getImagePath());
            }

            boolean battleEnded = false;
            while (!battleEnded) {
                ui.print(" ");
                ui.print("--- ターン " + state.getTurnCount() + " --- [現在距離: " + state.getDistance()
                        + " | SP: " + player.getCurrentSp()
                        + " / 敵SP: " + enemy.getCurrentSp() + "]");
                ui.print("【敵】" + enemy.getName() + " (HP: " + enemy.getHp() + "/" + enemy.getMaxHp() + ")");
                ui.print("コマンドを選択してください。");

                // サブウィンドウの戦闘情報をターン開始時に一括更新
                HitChanceDetails pChance = getSimulatedPlayerHitChance();
                HitChanceDetails eChance = getSimulatedEnemyHitChance();
                jfxUi.updateBattlePanel(
                        state.getTurnCount(), state.getDistance(),
                        player, enemy,
                        state.getPlayerConditions(), state.getEnemyConditions(),
                        pChance, eChance);
                // ターン间切りログ
                jfxUi.appendBattleLog("\u2500── ターン " + state.getTurnCount() + " ───");

                // 逃走可否をUIへ通知
                // 距離3以上かつcanFlee=trueなら「逃げる」を表示する
                // （後退+1で距離4に達するため、距離3からでも逃走できる）
                boolean fleeAvailable = (state.getDistance() >= 3) && enemy.isCanFlee();
                ui.updateFleeAvailability(fleeAvailable);

                BattleCommand cmd = jfxUi.getBattleCommand();
                if (cmd != null) {
                    // ターン開始時点で今回選んだスタンスをStateに記憶させておく
                    state.setCurrentPlayerStance(cmd.getStance() != null ? cmd.getStance() : "なし");

                    // プレイヤーの行動順ボーナスを計算
                    int playerInitBonus = 0;
                    for (TraitData td : getActivePlayerTraits()) {
                        if (td.getInitiativeBonus() != 0)
                            playerInitBonus += td.getInitiativeBonus();
                    }

                    // 敵の行動順ボーナスを計算
                    int enemyInitBonus = 0;
                    for (TraitData td : getActiveEnemyTraits()) {
                        if (td.getInitiativeBonus() != 0)
                            enemyInitBonus += td.getInitiativeBonus();
                    }

                    // --- イニシアチブ判定（機敏 + 1d6 + ボーナス） ---
                    int playerFinesse = (player.getCombatStats() != null) ? player.getCombatStats().finesse() : 0;
                    int enemyFinesse = enemy.getFinesse();
                    int playerInitiative = playerFinesse + DiceRoller.roll("1d6") + playerInitBonus;
                    int enemyInitiative = enemyFinesse + DiceRoller.roll("1d6") + enemyInitBonus;

                    boolean playerGoesFirst = playerInitiative >= enemyInitiative;
                    String initMsg = playerGoesFirst ? "(プレイヤー先行)" : "(敵先行)";

                    String pBonusStr = playerInitBonus != 0
                            ? " (補正 " + (playerInitBonus > 0 ? "+" : "") + playerInitBonus + ")"
                            : "";
                    String eBonusStr = enemyInitBonus != 0
                            ? " (補正 " + (enemyInitBonus > 0 ? "+" : "") + enemyInitBonus + ")"
                            : "";

                    ui.print("　[行動順判定: プレイヤー " + playerInitiative + pBonusStr + " vs 敵 " + enemyInitiative + eBonusStr
                            + "] " + initMsg);

                    if (playerGoesFirst) {
                        // プレイヤー先行
                        ui.print("＞プレイヤーの行動: " + cmd.toString());
                        boolean escapeSuccess = processPlayerTurn(cmd, enemy);
                        if (escapeSuccess) {
                            lastResult = BattleResult.FLED;
                            battleEnded = true;
                            continue;
                        }
                        if (enemy.getHp() <= 0) {
                            ui.print("【勝利！】 " + enemy.getName() + " を倒した！");
                            lastResult = BattleResult.VICTORY;
                            battleEnded = true;
                            continue;
                        }
                        // 敵のターン
                        processEnemyTurn(enemy);
                        if (player.getHp() <= 0) {
                            lastResult = BattleResult.DEFEAT;
                            battleEnded = true;
                            continue;
                        }
                    } else {
                        // 敵先行
                        processEnemyTurn(enemy);
                        if (player.getHp() <= 0) {
                            lastResult = BattleResult.DEFEAT;
                            battleEnded = true;
                            continue;
                        }
                        // プレイヤーのターン
                        ui.print("＞プレイヤーの行動: " + cmd.toString());
                        boolean escapeSuccess = processPlayerTurn(cmd, enemy);
                        if (escapeSuccess) {
                            lastResult = BattleResult.FLED;
                            battleEnded = true;
                            continue;
                        }
                        if (enemy.getHp() <= 0) {
                            ui.print("【勝利！】 " + enemy.getName() + " を倒した！");
                            lastResult = BattleResult.VICTORY;
                            battleEnded = true;
                            continue;
                        }
                    }

                    // --- ターン終了処理（ステータス更新）---
                    // --- 行動後SP自動回復（信念等のTraitによる）---
                    for (TraitData trait : getActivePlayerTraits()) {
                        ScalingData spRegen = trait.getSpRegenAfterAction();
                        if (spRegen != null) {
                            int regen = calculateSpGainForPlayer(spRegen);
                            int newSp = Math.min(com.kh.tbrr.data.models.Player.SP_MAX, player.getCurrentSp() + regen);
                            player.setCurrentSp(newSp);
                            ui.print("　[" + trait.getName() + "] SP+" + regen + "（現在SP: " + newSp + "）");
                        }
                    }
                    ui.printPlayerStatus(player);
                    // プレイヤーの状態 → プレイヤーがDOT対象
                    updateConditionsForTarget(state.getPlayerConditions(), player, null);
                    // 敵の状態 → 敵がDOT対象
                    updateConditionsForTarget(state.getEnemyConditions(), null, state.getCurrentEnemy());

                    // 継続ダメージ等によるHP0判定
                    if (player.getCurrentHP() <= 0) {
                        lastResult = BattleResult.DEFEAT;
                        battleEnded = true;
                        continue;
                    }
                    if (state.getCurrentEnemy().getHp() <= 0) {
                        ui.print("【勝利！】 " + state.getCurrentEnemy().getName() + " を倒した！");
                        lastResult = BattleResult.VICTORY;
                        battleEnded = true;
                        continue;
                    }

                    state.incrementTurn();
                    ui.refreshPlayerBasicStatus(player);
                }
            }

            // 戦闘終了時にUIを通常モードに戻す
            jfxUi.setBattleMode(false);
            jfxUi.hideBattlePanel(); // 戦闘パネルを閉じて背景画像に戻す
            ui.showImage("enemy", ""); // 敵画像を消去（非表示にする）
            player.setCurrentSp(0); // 戦闘終了時にSPをリセット

            // ★敗北時のワンクッション追加
            if (lastResult == BattleResult.DEFEAT) {
                ui.print("【敗北】 " + player.getName() + " は力尽きた……");
                ui.waitForEnter();
            }
        }
        return lastResult;
    }

    /**
     * アビリティがリスト式自動アップグレードに対応している場合、プレイヤーの所持数に応じて上位のアビリティデータに変換して返す。
     * 条件を満たさない場合は元のアビリティデータをそのまま返す。
     */
    public static AbilityData resolveAbilityUpgradeStatic(AbilityData baseAbility, Player player) {
        if (baseAbility != null && baseAbility.isCountAbilityAsLevel() && baseAbility.getUpgrades() != null) {
            int level = 0;
            if (player != null && player.getEffectiveAbilities() != null) {
                // 所持リストの中から同じIDのアビリティを数える
                for (String id : player.getEffectiveAbilities()) {
                    if (id.equals(baseAbility.getId())) {
                        level++;
                    }
                }
            }

            if (level > 1) {
                int maxLevel = 1;
                for (String key : baseAbility.getUpgrades().keySet()) {
                    try {
                        int k = Integer.parseInt(key);
                        if (k <= level && k > maxLevel) {
                            maxLevel = k;
                        }
                    } catch (NumberFormatException e) {
                        // 数値としてパースできないキーは無視
                    }
                }

                if (maxLevel > 1) {
                    String upgradedId = baseAbility.getUpgrades().get(String.valueOf(maxLevel));
                    if (upgradedId != null) {
                        AbilityData upgraded = CombatDataLoader.getAbility(upgradedId);
                        if (upgraded != null) {
                            return upgraded;
                        }
                    }
                }
            }
        }
        return baseAbility;
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
            if (player.getEffectiveAbilities() != null) {
                for (String id : player.getEffectiveAbilities()) {
                    AbilityData data = CombatDataLoader.getAbility(id);
                    // アップグレード版がある場合は解決してから名前を比較する
                    data = resolveAbilityUpgradeStatic(data, player);
                    if (data != null && data.getName().equals(cmd.getSpecial())) {
                        abilityId = data.getId();
                        break;
                    }
                }
            }
        }

        // 画面(UI)から送信される「日本語名称」を内部IDへ変換
        String stanceName = cmd.getStance();
        StanceData stanceData = CombatDataLoader.getStanceByName(stanceName);

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
        Item weapon = null;
        String mainWeaponId = player.getEquippedMainWeapon();
        if (mainWeaponId != null) {
            weapon = ItemRegistry.getItemById(mainWeaponId);
        }

        // [2] ムーブ（足）の解決：距離の確定
        String move = cmd.getMove();
        int moveAmount = 1;
        if ("全力移動".equals(cmd.getAction())) {
            moveAmount = 2; // 全力移動なら2マス動く
        }

        // --- movement_hindrance チェック（プレイヤー）---
        int movHindrance = getMaxConditionIntensity(state.getPlayerConditions(), "movement_hindrance");
        if (movHindrance > 0 && !"停止".equals(move)) {
            if (random.nextInt(100) + 1 <= movHindrance) {
                String pNameMov = player.getName() != null ? player.getName() : "冒険者";
                ui.print("　移動阻害により、" + pNameMov + " の移動は失敗した！（停止）");
                move = "停止"; // 前進・後退処理ブロックをスキップ
            }
        }

        if ("前進".equals(move)) {
            state.setDistance(Math.max(0, state.getDistance() - moveAmount));
            ui.print("　プレイヤーは前進した。（現在距離: " + state.getDistance() + "）");
        } else if ("後退".equals(move)) {
            if (state.getDistance() == 0) {
                boolean enemyHasVigilance = getActiveEnemyTraits().stream()
                        .anyMatch(t -> t != null && "SYSTEMIC".equals(t.getType())
                                && "VIGILANCE".equals(t.getSystemicEffect()));
                if (enemyHasVigilance) {
                    ui.print("　" + enemy.getName() + " はプレイヤーが離れる隙を見逃さなかった！（警戒心による機会攻撃）");
                    executeEnemyOpportunityAttack(player);
                }
            }
            // プレイヤーが死んでいなければ後退する
            if (player.getHp() > 0) {
                state.setDistance(Math.min(4, state.getDistance() + moveAmount));
                ui.print("　プレイヤーは後退した。（現在距離: " + state.getDistance() + "）");
            }
        }

        // [3] アクション（腕）の解決：確定した距離に基づく判定
        String action = cmd.getAction();
        if ("逃げる".equals(action)) {
            // 逃走条件: 距離4 かつ 敵が逃走可能フラグを持つ
            if (state.getDistance() >= 4 && enemy.isCanFlee()) {
                ui.print("　戦闘から無事に逃走した！");
                return true;
            } else if (!enemy.isCanFlee()) {
                ui.print("　この敵からは逃げられない！");
            } else {
                ui.print("　逃走には距離が足りない！（距離4まで離れる必要がある）");
            }
            return false;
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
            // --- action_hindrance チェック（プレイヤー）---
            int actHindrance = getMaxConditionIntensity(state.getPlayerConditions(), "action_hindrance");
            if (actHindrance > 0) {
                if (random.nextInt(100) + 1 <= actHindrance) {
                    String pNameAct = player.getName() != null ? player.getName() : "冒険者";
                    ui.print("　行動阻害により、" + pNameAct + " の行動は失敗した！（AP消費なし）");
                    return false;
                }
            }

            // --- AP不足チェックと転倒処理 ---
            int originalApCost = ability.getApCost();
            int actualApCost = originalApCost;
            if (originalApCost > 0) {
                int reduction = player.getApCostReductionFor(ability.getTags());
                actualApCost = Math.max(1, originalApCost - reduction);

                if (player.getAp() < actualApCost) {
                    String playerName = player.getName() != null ? player.getName() : "冒険者";
                    ui.print("　" + playerName + " は満身創痍で「" + ability.getName() + "」を出す気力が無い…。");
                    ui.print("　" + playerName + " は無様に転んでしまった！");

                    // 転倒 (prone) を1ターン付与
                    boolean foundProne = false;
                    for (var c : state.getPlayerConditions()) {
                        if (c.getConditionId().equals("prone")) {
                            c.setDuration(1);
                            foundProne = true;
                            break;
                        }
                    }
                    if (!foundProne) {
                        state.getPlayerConditions().add(new BattleState.ActiveCombatCondition("prone", 1));
                    }
                    ui.print("　★ " + playerName + " は [転倒] になった！");

                    return false; // 以降の攻撃処理をすべて不発にする
                } else {
                    player.modifyAp(-actualApCost);
                }
            }

            // 距離による結果（HIT/MISS/BONUS）の判定
            String rangeResult = resolveRangeResult(baseRules, ability, weapon, stanceData, state.getDistance());

            if ("MISS".equals(rangeResult)) {
                // 武器自動持ち替え（アビリティで明示的に許可されている場合のみ）
                boolean canAutoSwitch = Boolean.TRUE.equals(ability.getTriggerAutoWeaponSwitch());
                Item switchedWeapon = canAutoSwitch ? resolveAutoWeaponSwitch(weapon) : null;
                if (switchedWeapon != null) {
                    // 予備武器で射程を再判定
                    String switchedRange = resolveRangeResult(baseRules, ability, switchedWeapon, stanceData,
                            state.getDistance());
                    if (!"MISS".equals(switchedRange)) {
                        ui.print("　【武器の自動切り替え】" + switchedWeapon.getName() + " で攻撃を解決する。");
                        weapon = switchedWeapon;
                        rangeResult = switchedRange;
                    }
                }
            }

            if ("MISS".equals(rangeResult)) {
                ui.print("　ミス！距離が適していません。 (" + ability.getName() + ")");
                return false;
            }

            // --- SP増加処理（checkを持たないSPバフ系アビリティはここで終了）---
            ScalingData spGain = ability.getSpGain();
            if (spGain != null) {
                int gain = calculateSpGainForPlayer(spGain);
                int newSp = Math.min(com.kh.tbrr.data.models.Player.SP_MAX, player.getCurrentSp() + gain);
                player.setCurrentSp(newSp);
                String playerName = player.getName() != null ? player.getName() : "冒険者";
                ui.print("　" + playerName + " のSPが " + gain + " 回復した！（現在SP: " + newSp + "）");
                ui.printPlayerStatus(player);
            }

            // check が null（SPバフ専用アビリティ等）の場合は攻撃処理を行わず終了
            if (ability.getCheck() == null) {
                return false;
            }

            // 命中判定
            String atkStatName = ability.getCheck().getAttackerStat();
            // アビリティ側の指定がなければ武器側の指定を見る。それも無ければデフォルト(finesse)
            if (atkStatName == null || atkStatName.isEmpty()) {
                if (weapon != null && weapon.getWeaponAtkStat() != null && !weapon.getWeaponAtkStat().isEmpty()) {
                    atkStatName = weapon.getWeaponAtkStat();
                } else {
                    atkStatName = "finesse";
                }
            }
            int atkStatVal = getCombatStat(player, atkStatName);

            String defStatName = ability.getCheck().getDefenderStat();
            if (defStatName == null || defStatName.isEmpty())
                defStatName = "finesse";
            int defStatVal = enemy.getStatByName(defStatName);

            Integer overrideChance = ability.getCheck().getBaseChance();
            HitResult result = checkHit(atkStatVal, defStatVal, overrideChance, state.isEnemyDefending(),
                    state.getPlayerConditions(), state.getEnemyConditions(), getActivePlayerTraits(),
                    getActiveEnemyTraits());

            if (result.isHit) {
                // ダイス計算: 未指定・WEAPONなら武器ダイスを使用
                String dice = ability.getCheck().getDamageDice();
                boolean usesWeaponDice = (dice == null || dice.isEmpty() || "WEAPON".equalsIgnoreCase(dice));
                if (usesWeaponDice) {
                    dice = "1d4"; // 素手/武器フォールバック
                    if (weapon != null && weapon.getDamageDice() != null && !weapon.getDamageDice().isEmpty()) {
                        dice = weapon.getDamageDice();
                    }
                }
                int diceRoll = DiceRoller.roll(dice);

                // --- マスタリーの計算 ---
                // アビリティのタグと、装備している武器のタグを合算（マージ）して判定に渡す
                java.util.List<String> tagsForMastery = new java.util.ArrayList<>();
                if (ability.getTags() != null) {
                    tagsForMastery.addAll(ability.getTags());
                }

                if (Boolean.TRUE.equals(ability.getInheritWeapon()) && weapon != null && weapon.getTags() != null) {
                    tagsForMastery.addAll(weapon.getTags());
                }

                int masteryLevel = calculateMasteryLevel(tagsForMastery);
                int masteryDiceSum = calculateMasteryDice(masteryLevel);
                int masteryFixedBonus = calculateMasteryFixedBonus(masteryLevel);

                // ステータス加算計算（複合スケーリング対応）
                int scalingStatVal = 0;
                java.util.Map<String, Double> scalings = ability.getCheck().getScalings();
                if (scalings != null && !scalings.isEmpty()) {
                    // アビリティに scalings が指定されている場合: 複合スケーリング計算
                    for (java.util.Map.Entry<String, Double> entry : scalings.entrySet()) {
                        scalingStatVal += (int) (getCombatStat(player, entry.getKey()) * entry.getValue());
                    }
                } else {
                    // アビリティに scalings がない場合: 武器のスケーリング設定にフォールバック
                    String fallbackStat = (weapon != null && weapon.getWeaponScalingStat() != null
                            && !weapon.getWeaponScalingStat().isEmpty())
                            ? weapon.getWeaponScalingStat() : "might";
                    double fallbackScale = (weapon != null && weapon.getWeaponStatScaling() != null)
                            ? weapon.getWeaponStatScaling() : 0.5;
                    scalingStatVal = (int) (getCombatStat(player, fallbackStat) * fallbackScale);
                }
                int baseDamage = diceRoll + masteryDiceSum + masteryFixedBonus + scalingStatVal;

                // クリティカル処理（BONUS判定 または ダイス1〜5）
                boolean isCritical = "BONUS".equals(rangeResult) || result.isCritical;
                double critMult = isCritical ? resolveCritMultiplier(player) : 1.0;

                // CombatCondition によるダメージ倍率の適用
                double conditionMult = calcConditionDamageMultiplier(state.getPlayerConditions());
                int totalDamage = (int) (baseDamage * critMult * conditionMult);

                if (state.isEnemyDefending()) {
                    totalDamage /= 2; // 防御中はダメージ半減
                }

                // 敵の固定ダメージ軽減（金属系の敵等に設定されるdamageReduction）
                int enemyDmgReduction = enemy.getDamageReduction();
                // 敵のTraitによるダメージ軽減
                for (com.kh.tbrr.battle.data.TraitData t : getActiveEnemyTraits()) {
                    if (t != null) {
                        enemyDmgReduction += t.getDamageReduction();
                    }
                }

                if (enemyDmgReduction > 0) {
                    totalDamage = Math.max(0, totalDamage - enemyDmgReduction);
                }

                // ダメージ処理（SP→HPの順）
                boolean isPenetrating = false;
                DamageResult dmgResult = enemy.applyBattleDamage(totalDamage, isPenetrating);
                int hpDamage = dmgResult.actualHpDamage();
                int spAbsorbed = dmgResult.actualSpDamage();

                String diceMsg = "(基礎ダイス:" + diceRoll + (masteryLevel > 0 ? " + 習熟追加" + masteryLevel + "d4" : "")
                        + " + 習熟固定:" + masteryFixedBonus + " + ステ修正:" + scalingStatVal + ")";
                String critMsg = isCritical ? " 【クリティカル！" + critMult + "倍】" : "";
                String reductionMsg = enemyDmgReduction > 0 ? "（" + enemyDmgReduction + "ダメージ軽減）" : "";
                String spMsg = spAbsorbed > 0 ? "（SP" + spAbsorbed + "吸収、HPに" + hpDamage + "通った）" : "";
                ui.print("　命中！ " + enemy.getName() + " に " + totalDamage + " のダメージ！ " + diceMsg + critMsg + reductionMsg
                        + spMsg);
                // サブウィンドウのログに要約を追記
                if (ui instanceof JavaFXUI) {
                    String logSummary = "命中！" + enemy.getName() + "に" + totalDamage + "ダメージ"
                            + (isCritical ? "　[クリティカル]" : "")
                            + (spAbsorbed > 0 ? "　SP" + spAbsorbed + "吸収" : "");
                    ((JavaFXUI) ui).appendBattleLog(logSummary);
                }

                // --- CombatCondition付与の処理 ---
                if (ability.getApplyCombatConditions() != null) {
                    for (var app : ability.getApplyCombatConditions()) {
                        int r = random.nextInt(100) + 1; // 1-100
                        if (r <= app.getChance()) {
                            applyCondition(state.getEnemyConditions(), app.getConditionId(),
                                    app.getIntensity(), app.getDuration());
                            CombatConditionData cData = CombatConditionRegistry
                                    .getConditionById(app.getConditionId());
                            if (cData != null) {
                                ui.print("　★ " + enemy.getName() + " は [" + cData.getName() + "] になった！");
                            }
                        }
                    }
                }

                // --- アビリティによる強制距離変更（ノックバック等）---
                if (ability.getForceDistanceTo() != null) {
                    int forced = Math.max(0, Math.min(4, ability.getForceDistanceTo()));
                    state.setDistance(forced);
                    ui.print("　★ 吹き飛ばし！ 距離が " + forced + " に変化した。");
                }

                // --- DUAL_WIELD（二刀流）処理 ---
                // アビリティで明示的に許可されている場合のみオフハンド追撃を行う
                boolean canOffHand = Boolean.TRUE.equals(ability.getTriggerOffHandAttack());
                if (canOffHand) {
                    processOffHandAttack(enemy, ability, baseRules);
                }

            } else {
                ui.print("　かわされた！（ミス！）");
                // サブウィンドウのログに要約を追記
                if (ui instanceof JavaFXUI) {
                    ((JavaFXUI) ui).appendBattleLog("攻撃がかわされた！");
                }
            }
        }
        return false;
    }

    /**
     * DUAL_WIELD（二刀流）パッシブが存在する場合、予備スロット0の武器（オフハンド）で追加攻撃を行う。
     * - オフハンド武器が dagger または offhand タグを持つ場合：ペナルティなし
     * - それ以外：命中率 offHandHitPenalty（デフォルト-20%）、ダメージ
     * offHandDamageMultiplier（デフォルト0.5倍）
     * - オフハンド武器のcombatStatsボーナスは適用しない（仕様）
     */
    private void processOffHandAttack(EnemyData enemy, AbilityData ability, CombatBaseRules baseRules) {
        // 二刀流特徴（Trait）を探す
        final TraitData dualWield = getActivePlayerTraits().stream()
                .filter(t -> t != null && "SYSTEMIC".equals(t.getType()) && "DUAL_WIELD".equals(t.getSystemicEffect()))
                .findFirst()
                .orElse(null);
        if (dualWield == null)
            return; // 二刀流特徴なし → スキップ

        // 予備スロット0をオフハンド武器として取得
        java.util.List<String> reserves = player.getReserveEquipments();
        if (reserves == null || reserves.isEmpty()) {
            return; // 予備武器なし → スキップ
        }
        String offHandId = reserves.get(0);
        Item offHandWeapon = ItemRegistry.getItemById(offHandId);
        if (offHandWeapon == null || offHandWeapon.getDamageDice() == null) {
            return; // 予備スロットが空か武器でない → スキップ
        }

        ui.print("　【二刀流】オフハンドで追加攻撃！（" + offHandWeapon.getName() + "）");

        // ペナルティ免除条件の判定：offHandFreeTagConditionsのいずれかのタグを持つか
        boolean penaltyFree = false;
        if (offHandWeapon.getTags() != null && dualWield.getOffHandFreeTagConditions() != null) {
            penaltyFree = offHandWeapon.getTags().stream()
                    .anyMatch(tag -> dualWield.getOffHandFreeTagConditions().contains(tag));
        }

        // 命中判定（ペナルティ考慮）
        int atkStat = getCombatStat(player, ability.getCheck().getAttackerStat());
        String defStatName = ability.getCheck().getDefenderStat();
        int defStat = (defStatName != null && !defStatName.isEmpty()) ? enemy.getStatByName(defStatName) : 0;

        // ペナルティ: 命中ベース値をoffHandHitPenalty分だけ補正してcheckHitを呼ぶ
        // overrideBaseChanceを使ってペナルティを適用する
        int baseChanceOverride = baseRules.getAccuracy().getBaseChance()
                + (penaltyFree ? 0 : dualWield.getOffHandHitPenalty());
        HitResult offResult = checkHit(atkStat, defStat, baseChanceOverride, state.isEnemyDefending(),
                state.getPlayerConditions(), state.getEnemyConditions(), getActivePlayerTraits(),
                getActiveEnemyTraits());

        if (offResult.isHit) {
            // ダメージ計算（オフハンド武器ダイス使用、マスタリー・ステ補正は計算する）
            String offDice = offHandWeapon.getDamageDice();
            int offRoll = DiceRoller.roll(offDice);
            int offMastery = calculateMasteryLevel(offHandWeapon.getTags());
            int offMasteryDice = calculateMasteryDice(offMastery);
            int offMasteryFixed = calculateMasteryFixedBonus(offMastery);
            // ステータス加算計算（複合スケーリング対応）
            int offScaling = 0;
            java.util.Map<String, Double> offScalings = ability.getCheck().getScalings();
            if (offScalings != null && !offScalings.isEmpty()) {
                for (java.util.Map.Entry<String, Double> entry : offScalings.entrySet()) {
                    offScaling += (int) (getCombatStat(player, entry.getKey()) * entry.getValue());
                }
            } else {
                offScaling = (int) (getCombatStat(player, "might") * 0.5);
            }
            int offBase = offRoll + offMasteryDice + offMasteryFixed + offScaling;

            // ダメージ倍率の適用（ペナルティなしなら1.0、ありなら offHandDamageMultiplier）
            double offDmgMult = penaltyFree ? 1.0 : dualWield.getOffHandDamageMultiplier();
            if (offResult.isCritical) {
                offDmgMult *= resolveCritMultiplier(player);
            }
            int offDamage = (int) (offBase * offDmgMult);
            if (state.isEnemyDefending()) {
                offDamage /= 2;
            }

            enemy.setHp(enemy.getHp() - offDamage);
            String penaltyMsg = penaltyFree ? "（ペナルティなし）"
                    : "（命中" + dualWield.getOffHandHitPenalty() + "%・ダメージ×" + offDmgMult + "）";
            String critMsg2 = offResult.isCritical ? " 【クリティカル！】" : "";
            ui.print("　　オフハンド命中！ " + enemy.getName() + " に " + offDamage + " のダメージ！" + penaltyMsg + critMsg2);
        } else {
            ui.print("　　オフハンドは外れた！");
        }
    }

    private int calculateMasteryLevel(java.util.List<String> tags) {
        int masteryLevel = 0;
        if (tags != null) {
            for (TraitData trait : getActivePlayerTraits()) {
                if (trait != null && "MASTERY".equals(trait.getType())) {
                    boolean match = false;

                    // targetTags（OR条件）のチェック
                    if (trait.getTargetTags() != null && !trait.getTargetTags().isEmpty()) {
                        match = tags.stream().anyMatch(tag -> trait.getTargetTags().contains(tag));
                    }

                    // requiredTags（AND条件）のチェック
                    if (trait.getRequiredTags() != null && !trait.getRequiredTags().isEmpty()) {
                        boolean allMatch = trait.getRequiredTags().stream().allMatch(tag -> tags.contains(tag));
                        if (trait.getTargetTags() != null && !trait.getTargetTags().isEmpty()) {
                            match = match && allMatch; // 両方設定されている場合は両方満たす
                        } else {
                            match = allMatch; // requiredTagsのみ設定されている場合
                        }
                    }

                    if (match) {
                        masteryLevel += trait.getLevel();
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
        if (masteryLevel <= 0)
            return 0;
        return (masteryLevel - 1) * (masteryLevel + 2) / 2;
    }

    /**
     * 射程判定のフォールバックチェーン（Ability is King の実装）。
     * 優先順：アビリティ rangeOverride > アビリティ rangeType
     * > 武器 rangeOverride > 武器 rangeType > デフォルト "melee"
     *
     * アビリティに rangeType を指定しない（null）技は、武器の射程をそのまま引き継ぐ。
     * 武器も未装備なら最終フォールバック "melee" で素手扱い（1d4+0+強靭*0.5）となる。
     */
    private String resolveRangeResult(CombatBaseRules rules, AbilityData ability,
            Item weapon, StanceData stance, int distance) {
        String distKey = String.valueOf(distance);

        // 1. アビリティの rangeOverride が存在する距離なら最優先
        if (ability.getRangeOverride() != null) {
            String custom = ability.getRangeOverride().get(distKey);
            if (custom != null)
                return custom;
        }
        // 2. アビリティの rangeType が明示されていればそれを使用
        if (ability.getRangeType() != null && !ability.getRangeType().isEmpty()) {
            // "self" は距離に関わらず常にHIT（バフ・SP回復などの自己対象アビリティ用）
            if ("self".equals(ability.getRangeType())) {
                return "HIT";
            }
            return rules.getRangeResult(ability.getRangeType(), distance);
        }
        // 3. 武器の rangeOverride が存在する距離なら使用
        if (weapon != null && weapon.getRangeOverride() != null) {
            String custom = weapon.getRangeOverride().get(distKey);
            if (custom != null)
                return custom;
        }
        // 4. 武器の rangeType を使用
        if (weapon != null && weapon.getRangeType() != null && !weapon.getRangeType().isEmpty()) {
            return rules.getRangeResult(weapon.getRangeType(), distance);
        }
        // 5. 最終フォールバック（素手 = 近接扱い）
        return rules.getRangeResult("melee", distance);
    }

    /**
     * AUTO_WEAPON_SWITCH パッシブが有効な場合、予備スロット0の武器を返す。
     * - メイン武器がMISSになる距離で発動。
     * - 装備の入れ替えは行わず、攻撃解決のみ予備武器で行う。
     * - パッシブがない・予備武器がない場合は null を返す。
     */
    private Item resolveAutoWeaponSwitch(Item mainWeapon) {
        // AUTO_WEAPON_SWITCH特徴（Trait）を確認
        boolean hasTrait = getActivePlayerTraits().stream()
                .anyMatch(t -> t != null
                        && "SYSTEMIC".equals(t.getType())
                        && "AUTO_WEAPON_SWITCH".equals(t.getSystemicEffect()));
        if (!hasTrait)
            return null;

        // 予備スロット0の武器を取得
        java.util.List<String> reserves = player.getReserveEquipments();
        if (reserves == null || reserves.isEmpty())
            return null;
        String reserveId = reserves.get(0);
        Item reserveWeapon = ItemRegistry.getItemById(reserveId);
        if (reserveWeapon == null || reserveWeapon.getDamageDice() == null)
            return null;

        return reserveWeapon;
    }

    private double resolveCritMultiplier(Player p) {
        CombatBaseRules rules = CombatDataLoader.getBaseRules();
        double base = rules.getDamage().getCritMultiplier(); // デフォルト 1.5
        // CRIT_MULTIPLIER型特徴が存在する場合、最大値で上書き
        double override = getActivePlayerTraits().stream()
                .filter(trait -> trait != null
                        && "CRIT_MULTIPLIER".equals(trait.getType())
                        && trait.getCritMultiplier() > 0)
                .mapToDouble(TraitData::getCritMultiplier)
                .max()
                .orElse(base);
        return override;
    }

    private int getCombatStat(Player p, String statName) {
        if (statName == null || statName.isEmpty())
            return 0;

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

    // 敵のターン処理
    private void processEnemyTurn(EnemyData enemy) {
        state.setEnemyDefending(false); // 敵ターン開始時に自身の防御状態を解除

        // --- 相手のスタンスによる特殊トリガー（対抗呪文など）の判定枠 ---
        String pStanceStr = state.getCurrentPlayerStance();
        // TODO: ここで特注の割り込み処理（魔法の無効化など）を記述する予定

        ui.print("＞【" + enemy.getName() + "の行動】");
        CombatBaseRules baseRules = CombatDataLoader.getBaseRules();

        // --- 1. ムーブの実行 ---
        // --- movement_hindrance チェック（敵）---
        boolean enemyCanMove = true;
        int enemyMovHindrance = getMaxConditionIntensity(state.getEnemyConditions(), "movement_hindrance");
        if (enemyMovHindrance > 0) {
            if (random.nextInt(100) + 1 <= enemyMovHindrance) {
                ui.print("　移動阻害により、" + enemy.getName() + " の移動は失敗した！");
                enemyCanMove = false;
            }
        }

        if (enemyCanMove) {
            if ("predator".equalsIgnoreCase(enemy.getAiType())) {
                // Predator: 可能なら前進する
                if (state.getDistance() > 0) {
                    state.setDistance(state.getDistance() - 1);
                    ui.print("　" + enemy.getName() + " は詰め寄ってきた。（現在距離: " + state.getDistance() + "）");
                }
            } else if ("coward".equalsIgnoreCase(enemy.getAiType())) {
                // Coward: 可能なら後退して距離を取る
                if (state.getDistance() < 4) {
                    if (state.getDistance() == 0 && getActivePlayerTraits().stream().anyMatch(t -> t != null
                            && "SYSTEMIC".equals(t.getType()) && "VIGILANCE".equals(t.getSystemicEffect()))) {
                        String pName = (player.getName() != null && !player.getName().isEmpty()) ? player.getName()
                                : "冒険者";
                        ui.print("　" + pName + " は " + enemy.getName() + " が離れる隙を見逃さなかった！（警戒心による機会攻撃）");
                        executePlayerOpportunityAttack(enemy);
                    }
                    if (enemy.getHp() > 0) {
                        state.setDistance(state.getDistance() + 1);
                        ui.print("　" + enemy.getName() + " はじりじりと後退して距離を取った。（現在距離: " + state.getDistance() + "）");
                    }
                }
            } else if ("midrange".equalsIgnoreCase(enemy.getAiType())) {
                // Midrange: 距離2を維持しようとする
                if (state.getDistance() > 2) {
                    state.setDistance(state.getDistance() - 1);
                    ui.print("　" + enemy.getName() + " は間合いを詰めてきた。（現在距離: " + state.getDistance() + "）");
                } else if (state.getDistance() < 2) {
                    if (state.getDistance() == 0 && getActivePlayerTraits().stream().anyMatch(t -> t != null
                            && "SYSTEMIC".equals(t.getType()) && "VIGILANCE".equals(t.getSystemicEffect()))) {
                        String pName = (player.getName() != null && !player.getName().isEmpty()) ? player.getName()
                                : "冒険者";
                        ui.print("　" + pName + " は " + enemy.getName() + " が離れる隙を見逃さなかった！（警戒心による機会攻撃）");
                        executePlayerOpportunityAttack(enemy);
                    }
                    if (enemy.getHp() > 0) {
                        state.setDistance(state.getDistance() + 1);
                        ui.print("　" + enemy.getName() + " は後退して間合いを取った。（現在距離: " + state.getDistance() + "）");
                    }
                }
            } else if ("stationary".equalsIgnoreCase(enemy.getAiType())) {
                // Stationary: 一切移動しない（固定砲台、壁、城門など）
                // 移動メッセージも出さず、距離の変化も起こさない
            }
        }

        // --- 2. ルール評価とアクション決定 ---
        EnemyData.AIActionChoice selectedChoice = null;
        String selectedAbilityId = null;
        EnemyData.AIActionRule matchedRule = null;

        if (enemy.getActionRules() != null) {
            for (EnemyData.AIActionRule rule : enemy.getActionRules()) {
                // 使用回数チェック
                int usedCount = enemy.getRuleUsageCounts().getOrDefault(rule, 0);
                if (rule.getMaxUses() != -1 && usedCount >= rule.getMaxUses()) {
                    continue; // 回数上限に達している場合はスキップ
                }

                // 条件チェック（移動後の距離で判定）
                boolean conditionMet = evaluateEnemyCondition(rule.getCondition(), state.getDistance(), enemy, player);
                if (conditionMet) {
                    matchedRule = rule;
                    selectedChoice = selectAbilityFromRule(rule);
                    if (selectedChoice != null) {
                        selectedAbilityId = selectedChoice.getAbility();
                    }
                    break;
                }
            }
        }

        // --- 3. アクションの実行 ---
        // --- action_hindrance チェック（敵）---
        if (selectedAbilityId != null) {
            int enemyActHindrance = getMaxConditionIntensity(state.getEnemyConditions(), "action_hindrance");
            if (enemyActHindrance > 0 && random.nextInt(100) + 1 <= enemyActHindrance) {
                ui.print("　行動阻害により、" + enemy.getName() + " の行動は失敗した！");
                selectedAbilityId = null; // 行動キャンセル
            }
        }
        if (selectedAbilityId != null) {
            // 回数カウントアップ
            enemy.getRuleUsageCounts().put(matchedRule, enemy.getRuleUsageCounts().getOrDefault(matchedRule, 0) + 1);

            if ("flee".equalsIgnoreCase(selectedAbilityId)) {
                // 特殊行動: 逃走 (現状メッセージのみ)
                ui.print("　" + enemy.getName() + " は逃げ出した！");
                enemy.setHp(0); // とりあえず倒した扱いにして戦闘終了させる
            } else {
                AbilityData ability = CombatDataLoader.getAbility(selectedAbilityId);
                if (ability == null) {
                    ui.print("【エラー】敵のアビリティデータが見つかりません: " + selectedAbilityId);
                    return;
                }
                String nameOverride = (selectedChoice != null) ? selectedChoice.getNameOverride() : null;
                java.util.List<String> description = (selectedChoice != null) ? selectedChoice.getDescription() : null;
                executeEnemyAttack(enemy, ability, baseRules, nameOverride, description);
            }
        } else {
            ui.print("　" + enemy.getName() + " は様子をうかがっている……。（行動できる技がない）");
        }
    }

    // 条件評価ヘルパーメソッド
    private boolean evaluateEnemyCondition(String conditionStr, int currentDistance, EnemyData enemy, Player player) {
        if (conditionStr == null || conditionStr.isEmpty() || "ALWAYS".equalsIgnoreCase(conditionStr)) {
            return true;
        }
        if (conditionStr.startsWith("DISTANCE_>=_")) {
            int val = Integer.parseInt(conditionStr.substring(12));
            return currentDistance >= val;
        }
        if (conditionStr.startsWith("DISTANCE_<=_")) {
            int val = Integer.parseInt(conditionStr.substring(12));
            return currentDistance <= val;
        }
        return false;
    }

    // 重み付け抽選ヘルパーメソッド
    private EnemyData.AIActionChoice selectAbilityFromRule(EnemyData.AIActionRule rule) {
        if (rule.getActions() == null || rule.getActions().isEmpty())
            return null;
        int totalWeight = 0;
        for (EnemyData.AIActionChoice choice : rule.getActions()) {
            totalWeight += choice.getWeight();
        }
        if (totalWeight <= 0)
            return rule.getActions().get(0);

        int r = random.nextInt(totalWeight);
        int current = 0;
        for (EnemyData.AIActionChoice choice : rule.getActions()) {
            current += choice.getWeight();
            if (r < current) {
                return choice;
            }
        }
        return rule.getActions().get(0); // フォールバック
    }

    private void executeEnemyAttack(EnemyData enemy, AbilityData ability, CombatBaseRules baseRules,
            String nameOverride, java.util.List<String> description) {
        String abilityName = (nameOverride != null && !nameOverride.isEmpty()) ? nameOverride : ability.getName();

        // idle(非戦闘)タグを持つ行動なら、様子を見る等のテキストを出して終了
        if (ability.getTags() != null && ability.getTags().contains("idle")) {
            ui.print("　" + enemy.getName() + " は " + abilityName + "。");
            return;
        }

        ui.print("　" + enemy.getName() + "の攻撃！[" + abilityName + "]");

        if (description != null && !description.isEmpty()) {
            for (String line : description) {
                String replacedLine = com.kh.tbrr.utils.TextReplacer.replace(line, player);
                ui.print("　" + replacedLine);
            }
        }

        int distance = state.getDistance();
        String rangeResult = resolveRangeResult(baseRules, ability, null, null, distance);

        if ("MISS".equals(rangeResult)) {
            ui.print("　ミス！距離が適していません。 (" + abilityName + ")");
            return;
        }

        // 命中判定
        String atkStatName = ability.getCheck().getAttackerStat();
        if (atkStatName == null || atkStatName.isEmpty())
            atkStatName = "finesse";
        int atkStatVal = enemy.getStatByName(atkStatName);

        String defStatName = ability.getCheck().getDefenderStat();
        if (defStatName == null || defStatName.isEmpty())
            defStatName = "finesse";
        int defStatVal = getCombatStat(player, defStatName);

        Integer overrideChance = ability.getCheck().getBaseChance();
        HitResult result = checkHit(atkStatVal, defStatVal, overrideChance, state.isPlayerDefending(),
                state.getEnemyConditions(), state.getPlayerConditions(), getActiveEnemyTraits(),
                getActivePlayerTraits());

        if (result.isHit) {
            String dice = ability.getCheck().getDamageDice();
            if (dice == null || dice.isEmpty() || "WEAPON".equalsIgnoreCase(dice)) {
                dice = "1d4"; // 敵のデフォルト
            }
            int diceRoll = DiceRoller.roll(dice);

            // ステータス加算計算（複合スケーリング対応）
            int scalingStatVal = 0;
            java.util.Map<String, Double> scalings = ability.getCheck().getScalings();
            if (scalings != null && !scalings.isEmpty()) {
                for (java.util.Map.Entry<String, Double> entry : scalings.entrySet()) {
                    scalingStatVal += (int) (enemy.getStatByName(entry.getKey()) * entry.getValue());
                }
            } else {
                scalingStatVal = (int) (enemy.getStatByName("might") * 0.5);
            }
            int baseDamage = diceRoll + scalingStatVal;

            boolean isCritical = "BONUS".equals(rangeResult) || result.isCritical;
            double critMult = isCritical ? baseRules.getDamage().getCritMultiplier() : 1.0;

            double conditionMult = calcConditionDamageMultiplier(state.getEnemyConditions());
            int totalDamage = (int) (baseDamage * critMult * conditionMult);

            // アクセサリによるダメージ軽減
            int reduction = 0;
            if (player.getEquippedAccessories() != null) {
                for (String accId : player.getEquippedAccessories()) {
                    Item acc = ItemRegistry.getItemById(accId);
                    if (acc != null) {
                        reduction += acc.getDamageReduction();
                    }
                }
            }
            // 防御側（プレイヤー）Traitによるダメージ軽減
            for (com.kh.tbrr.battle.data.TraitData t : getActivePlayerTraits()) {
                if (t != null) {
                    reduction += t.getDamageReduction();
                }
            }

            totalDamage = Math.max(0, totalDamage - reduction);

            if (state.isPlayerDefending()) {
                totalDamage /= 2; // プレイヤーが防御していればダメージ半減
            }

            // ダメージ処理（SP→HPの順）
            boolean isPenetrating = false;
            DamageResult dmgResult = player.applyBattleDamage(totalDamage, isPenetrating);
            int hpDamage = dmgResult.actualHpDamage();
            int spAbsorbed = dmgResult.actualSpDamage();

            String playerName = player.getName() != null ? player.getName() : "冒険者";
            String reduceMsg = reduction > 0 ? "（" + reduction + "ダメージ軽減）" : "";
            String critMsg = isCritical ? " 【クリティカル！" + critMult + "倍】" : "";
            String spMsg = spAbsorbed > 0 ? "（SP" + spAbsorbed + "吸収、HPに" + hpDamage + "通った）" : "";
            ui.print("→ " + playerName + " に " + totalDamage + " のダメージ！" + reduceMsg + critMsg + spMsg);
            // サブウィンドウのログに要約を追記
            if (ui instanceof JavaFXUI) {
                String logSummary = enemy.getName() + "の攻撃！" + totalDamage + "ダメージ"
                        + (isCritical ? "　[クリティカル]" : "")
                        + (spAbsorbed > 0 ? "　SP" + spAbsorbed + "吸収" : "");
                ((JavaFXUI) ui).appendBattleLog(logSummary);
            }
            ui.printPlayerStatus(player); // 右パネルのHP/AP表示を更新

            // --- 敵アビリティによる CombatCondition 付与 ---
            if (ability.getApplyCombatConditions() != null) {
                for (var app : ability.getApplyCombatConditions()) {
                    int r = random.nextInt(100) + 1;
                    if (r <= app.getChance()) {
                        applyCondition(state.getPlayerConditions(), app.getConditionId(),
                                app.getIntensity(), app.getDuration());
                        CombatConditionData cData = CombatConditionRegistry
                                .getConditionById(app.getConditionId());
                        if (cData != null) {
                            ui.print("　★ " + playerName + " は [" + cData.getName() + "] になった！");
                        }
                    }
                }
            }
        } else {
            String playerName = player.getName() != null ? player.getName() : "冒険者";
            ui.print("→ ...しかし、" + playerName + " は回避した！");
            // サブウィンドウのログに要約を追記
            if (ui instanceof JavaFXUI) {
                ((JavaFXUI) ui).appendBattleLog(enemy.getName() + "の攻撃を回避！");
            }
        }
    }

    private void executePlayerOpportunityAttack(EnemyData enemy) {
        CombatBaseRules baseRules = CombatDataLoader.getBaseRules();
        AbilityData ability = CombatDataLoader.getAbility("basic_attack");
        if (ability == null)
            return;
        Item weapon = null;
        String mainWeaponId = player.getEquippedMainWeapon();
        if (mainWeaponId != null) {
            weapon = ItemRegistry.getItemById(mainWeaponId);
        }

        String atkStatName = ability.getCheck().getAttackerStat();
        if (atkStatName == null || atkStatName.isEmpty())
            atkStatName = "finesse";
        int atkStatVal = getCombatStat(player, atkStatName);

        String defStatName = ability.getCheck().getDefenderStat();
        if (defStatName == null || defStatName.isEmpty())
            defStatName = "finesse";
        int defStatVal = enemy.getStatByName(defStatName);

        HitResult result = checkHit(atkStatVal, defStatVal, ability.getCheck().getBaseChance(),
                state.isEnemyDefending(),
                state.getPlayerConditions(), state.getEnemyConditions(), getActivePlayerTraits(),
                getActiveEnemyTraits());

        if (result.isHit) {
            String dice = ability.getCheck().getDamageDice();
            if (dice == null || dice.isEmpty() || "WEAPON".equalsIgnoreCase(dice)) {
                dice = "1d4";
                if (weapon != null && weapon.getDamageDice() != null && !weapon.getDamageDice().isEmpty()) {
                    dice = weapon.getDamageDice();
                }
            }
            int diceRoll = DiceRoller.roll(dice);

            boolean usesWeaponDice = (ability.getCheck().getDamageDice() == null
                    || ability.getCheck().getDamageDice().isEmpty()
                    || "WEAPON".equalsIgnoreCase(ability.getCheck().getDamageDice()));

            // アビリティのタグと武器のタグを合算
            java.util.List<String> tagsForMastery = new java.util.ArrayList<>();
            if (ability.getTags() != null) {
                tagsForMastery.addAll(ability.getTags());
            }
            if (weapon != null && weapon.getTags() != null) {
                tagsForMastery.addAll(weapon.getTags());
            }
            int masteryLevel = calculateMasteryLevel(tagsForMastery);

            int masteryDiceSum = calculateMasteryDice(masteryLevel);
            int masteryFixedBonus = calculateMasteryFixedBonus(masteryLevel);

            // ステータス加算計算（複合スケーリング対応）
            int scalingStatVal = 0;
            java.util.Map<String, Double> oaScalings = ability.getCheck().getScalings();
            if (oaScalings != null && !oaScalings.isEmpty()) {
                for (java.util.Map.Entry<String, Double> entry : oaScalings.entrySet()) {
                    scalingStatVal += (int) (getCombatStat(player, entry.getKey()) * entry.getValue());
                }
            } else {
                scalingStatVal = (int) (getCombatStat(player, "might") * 0.5);
            }

            int baseDamage = diceRoll + masteryDiceSum + masteryFixedBonus + scalingStatVal;
            double critMult = result.isCritical ? resolveCritMultiplier(player) : 1.0;
            double conditionMult = calcConditionDamageMultiplier(state.getPlayerConditions());
            int totalDamage = (int) (baseDamage * critMult * conditionMult);

            if (state.isEnemyDefending()) {
                totalDamage /= 2;
            }

            // 機会攻撃ダメージ処理（SP→HPの順）
            DamageResult dmgResult = enemy.applyBattleDamage(totalDamage, false);
            int hpDamage = dmgResult.actualHpDamage();
            int spAbsorbed = dmgResult.actualSpDamage();

            String critMsg = result.isCritical ? " 【クリティカル！" + critMult + "倍】" : "";
            String spMsg = spAbsorbed > 0 ? "（SP" + spAbsorbed + "吸収、HPに" + hpDamage + "通った）" : "";
            ui.print("　機会攻撃命中！ " + enemy.getName() + " に " + totalDamage + " のダメージ！" + critMsg + spMsg);

            processOffHandAttack(enemy, ability, baseRules);
        } else {
            ui.print("　機会攻撃はかわされた！");
        }
    }

    private void executeEnemyOpportunityAttack(Player p) {
        CombatBaseRules baseRules = CombatDataLoader.getBaseRules();
        HitResult result = checkHit(state.getCurrentEnemy().getFinesse(), p.getCombatStats().finesse(), null,
                state.isPlayerDefending(), state.getEnemyConditions(), state.getPlayerConditions(),
                getActiveEnemyTraits(), getActivePlayerTraits());

        if (result.isHit) {
            int diceRoll = DiceRoller.roll("1d4");
            int scalingStatVal = (int) (state.getCurrentEnemy().getMight() * baseRules.getGlobalStatScaling());
            double conditionMult = calcConditionDamageMultiplier(state.getEnemyConditions());
            int totalDamage = (int) ((diceRoll + scalingStatVal)
                    * (result.isCritical ? baseRules.getDamage().getCritMultiplier() : 1.0) * conditionMult);

            int reduction = 0;
            if (p.getEquippedAccessories() != null) {
                for (String accId : p.getEquippedAccessories()) {
                    Item acc = ItemRegistry.getItemById(accId);
                    if (acc != null) {
                        reduction += acc.getDamageReduction();
                    }
                }
            }
            totalDamage = Math.max(0, totalDamage - reduction);
            if (state.isPlayerDefending())
                totalDamage /= 2;

            // 敵機会攻撃のダメージ処理（SP→HPの順）
            boolean isPenetrating = false;
            DamageResult dmgResult = p.applyBattleDamage(totalDamage, isPenetrating);
            int hpDamage = dmgResult.actualHpDamage();
            int spAbsorbed = dmgResult.actualSpDamage();

            String reduceMsg = reduction > 0 ? "（" + reduction + "ダメージ軽減）" : "";
            String spMsg = spAbsorbed > 0 ? "（SP" + spAbsorbed + "吸収、HPに" + hpDamage + "通った）" : "";
            ui.print("　機会攻撃命中！ プレイヤーに " + totalDamage + " のダメージ！" + reduceMsg + spMsg);
            ui.printPlayerStatus(p);
        } else {
            ui.print("　機会攻撃を回避した！");
        }
    }

    public static class HitChanceDetails {
        public int finalChance;
        public int statMod;
        public int traitMod;
        public int condMod;

        public HitChanceDetails(int finalChance, int statMod, int traitMod, int condMod) {
            this.finalChance = finalChance;
            this.statMod = statMod;
            this.traitMod = traitMod;
            this.condMod = condMod;
        }
    }

    public HitChanceDetails getSimulatedPlayerHitChance() {
        return getSimulatedPlayerHitChance(null);
    }

    public HitChanceDetails getSimulatedPlayerHitChance(String previewStanceName) {
        if (state == null || state.getCurrentEnemy() == null)
            return new HitChanceDetails(0, 0, 0, 0);

        int previewFinesse = player.getCombatStats().finesse();
        java.util.List<com.kh.tbrr.battle.data.TraitData> previewTraits = new java.util.ArrayList<>(
                getActivePlayerTraits());

        if (previewStanceName != null && !previewStanceName.isEmpty() && !previewStanceName.equals("なし")) {
            com.kh.tbrr.battle.data.StanceData sd = com.kh.tbrr.battle.data.CombatDataLoader
                    .getStanceByName(previewStanceName);
            if (sd != null && sd.getGrantedTraitIds() != null) {
                for (String traitId : sd.getGrantedTraitIds()) {
                    com.kh.tbrr.battle.data.TraitData td = com.kh.tbrr.battle.data.TraitRegistry.getTraitById(traitId);
                    if (td != null) {
                        if (td.getStatBonuses() != null) {
                            previewFinesse += td.getStatBonuses().getOrDefault("finesse", 0);
                        }
                        previewTraits.add(td);
                    }
                }
            }
        }

        return calculateHitChanceDetails(previewFinesse, state.getCurrentEnemy().getFinesse(), null,
                state.isEnemyDefending(), state.getPlayerConditions(), state.getEnemyConditions(), previewTraits,
                getActiveEnemyTraits());
    }

    public HitChanceDetails getSimulatedEnemyHitChance() {
        if (state == null || state.getCurrentEnemy() == null)
            return new HitChanceDetails(0, 0, 0, 0);
        return calculateHitChanceDetails(state.getCurrentEnemy().getFinesse(), player.getCombatStats().finesse(), null,
                state.isPlayerDefending(), state.getEnemyConditions(), state.getPlayerConditions(),
                getActiveEnemyTraits(),
                getActivePlayerTraits());
    }

    private HitChanceDetails calculateHitChanceDetails(int attackerStat, int defenderStat, Integer overrideBaseChance,
            boolean targetDefending,
            java.util.List<BattleState.ActiveCombatCondition> atkConds,
            java.util.List<BattleState.ActiveCombatCondition> defConds,
            java.util.List<com.kh.tbrr.battle.data.TraitData> atkTraits,
            java.util.List<com.kh.tbrr.battle.data.TraitData> defTraits) {

        if (atkTraits == null)
            atkTraits = java.util.Collections.emptyList();
        if (defTraits == null)
            defTraits = java.util.Collections.emptyList();

        CombatBaseRules rules = CombatDataLoader.getBaseRules();
        int base = overrideBaseChance != null ? overrideBaseChance : rules.getAccuracy().getBaseChance();
        int diff = attackerStat - defenderStat;

        int statMod = 0;
        for (var mod : rules.getAccuracy().getModifiers()) {
            int min = mod.getDiff().get(0);
            int max = mod.getDiff().get(1);
            if (diff >= min && diff <= max) {
                statMod = mod.getBonus();
                break;
            }
        }

        int condMod = 0;
        if (targetDefending) {
            condMod -= 20; // 防御アクション中は命中率自体を引き下げる
        }
        condMod += calcConditionAccuracyBonus(atkConds);
        condMod -= calcConditionAvoidanceBonus(defConds);

        int traitMod = 0;
        // 飛行判定
        // 被攻撃側のみが飛行を持っていて、攻撃側が飛行を持っていない場合のみ、回避+20（命中-20）
        boolean attackerHasFlying = atkTraits.stream()
                .anyMatch(t -> t != null && "SYSTEMIC".equals(t.getType()) && "FLYING".equals(t.getSystemicEffect()));
        boolean defenderHasFlying = defTraits.stream()
                .anyMatch(t -> t != null && "SYSTEMIC".equals(t.getType()) && "FLYING".equals(t.getSystemicEffect()));
        if (defenderHasFlying && !attackerHasFlying) {
            traitMod -= 20;
        }

        // 防御側のTraitによる回避ボーナス（命中率減算）
        for (com.kh.tbrr.battle.data.TraitData t : defTraits) {
            if (t != null) {
                traitMod -= t.getEvasionBonus();
            }
        }

        // 攻撃側のTraitによる命中ボーナス
        for (com.kh.tbrr.battle.data.TraitData t : atkTraits) {
            if (t != null) {
                traitMod += t.getHitChanceBonus();
            }
        }

        int hitChance = base + statMod + condMod + traitMod;
        hitChance = Math.max(rules.getAccuracy().getMin(), Math.min(rules.getAccuracy().getMax(), hitChance));

        return new HitChanceDetails(hitChance, statMod, traitMod, condMod);
    }

    private HitResult checkHit(int attackerStat, int defenderStat, Integer overrideBaseChance, boolean targetDefending,
            java.util.List<BattleState.ActiveCombatCondition> atkConds,
            java.util.List<BattleState.ActiveCombatCondition> defConds,
            java.util.List<com.kh.tbrr.battle.data.TraitData> atkTraits,
            java.util.List<com.kh.tbrr.battle.data.TraitData> defTraits) {

        HitChanceDetails details = calculateHitChanceDetails(attackerStat, defenderStat, overrideBaseChance,
                targetDefending, atkConds, defConds, atkTraits, defTraits);
        int hitChance = details.finalChance;

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
        updateConditionsForTarget(conditions, null, null);
    }

    /**
     * ターン終了時の状態異常更新。DOTダメージを対象に与えてからターンを減算する。
     * 
     * @param conditions   更新対象の状態リスト
     * @param targetPlayer DOTダメージを受けるプレイヤー（nullなら敵対象）
     * @param targetEnemy  DOTダメージを受ける敵（nullならプレイヤー対象）
     */
    private void updateConditionsForTarget(
            java.util.List<BattleState.ActiveCombatCondition> conditions,
            Player targetPlayer, EnemyData targetEnemy) {
        java.util.Iterator<BattleState.ActiveCombatCondition> it = conditions.iterator();
        while (it.hasNext()) {
            BattleState.ActiveCombatCondition c = it.next();
            CombatConditionData data = CombatConditionRegistry.getConditionById(c.getConditionId());

            // --- DOTダメージ処理 ---
            if (data != null && data.getModifiers() != null) {
                // intensity > 0 ならアビリティ側で指定された強度、なければJSON定義のデフォルト値を使用
                int dotDmg = 0;
                if (data.getModifiers().getDotPhysical() > 0 || data.getModifiers().getDotMagical() > 0) {
                    dotDmg = c.getIntensity() > 0
                            ? c.getIntensity()
                            : (data.getModifiers().getDotPhysical() + data.getModifiers().getDotMagical());
                }
                if (dotDmg > 0) {
                    if (targetPlayer != null) {
                        DamageResult dotResult = targetPlayer.applyBattleDamage(dotDmg, false);
                        String pName = targetPlayer.getName() != null ? targetPlayer.getName() : "冒険者";
                        ui.print("　[" + data.getName() + "] " + pName + " が " + dotDmg + " の継続ダメージを受けた！"
                                + (dotResult.actualSpDamage() > 0 ? "（SP" + dotResult.actualSpDamage() + "吸収）" : ""));
                    } else if (targetEnemy != null) {
                        DamageResult dotResult = targetEnemy.applyBattleDamage(dotDmg, false);
                        ui.print("　[" + data.getName() + "] " + targetEnemy.getName() + " が " + dotDmg + " の継続ダメージを受けた！"
                                + (dotResult.actualSpDamage() > 0 ? "（SP" + dotResult.actualSpDamage() + "吸収）" : ""));
                    }
                }
            }

            // --- ターン減算と効果切れ処理 ---
            if (c.getDuration() > 0) {
                c.decrementDuration();
                if (c.getDuration() == 0) {
                    it.remove(); // 効果切れ
                    if (data != null) {
                        ui.print("　[" + data.getName() + "] の効果が切れた。");
                    }
                }
            }
        }
    }

    private int calcConditionAccuracyBonus(java.util.List<BattleState.ActiveCombatCondition> conditions) {
        if (conditions == null)
            return 0;
        int bonus = 0;
        for (BattleState.ActiveCombatCondition c : conditions) {
            CombatConditionData data = CombatConditionRegistry
                    .getConditionById(c.getConditionId());
            if (data != null && data.getModifiers() != null) {
                bonus += data.getModifiers().getAccuracyBonus();
            }
        }
        return bonus;
    }

    private int calcConditionAvoidanceBonus(java.util.List<BattleState.ActiveCombatCondition> conditions) {
        if (conditions == null)
            return 0;
        int bonus = 0;
        for (BattleState.ActiveCombatCondition c : conditions) {
            CombatConditionData data = CombatConditionRegistry
                    .getConditionById(c.getConditionId());
            if (data != null && data.getModifiers() != null) {
                bonus += data.getModifiers().getAvoidanceBonus();
            }
        }
        return bonus;
    }

    private double calcConditionDamageMultiplier(java.util.List<BattleState.ActiveCombatCondition> conditions) {
        if (conditions == null)
            return 1.0;
        double mult = 1.0;
        for (BattleState.ActiveCombatCondition c : conditions) {
            CombatConditionData data = CombatConditionRegistry
                    .getConditionById(c.getConditionId());
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

    /**
     * 戦闘状態を対象リストに付与する共通メソッド。
     * CombatConditionData の stackRule に従って重複時の挙動を分岐する。
     *
     * <ul>
     * <li>ACCUMULATE : 強度を合算し、ターンは新しい値で上書きする（dot向け）</li>
     * <li>MAX_ONLY : 複数インスタンスをそのまま保持する（判定時に最大強度を参照）</li>
     * <li>EXTEND : ターンを加算して延長する</li>
     * <li>REFRESH : ターンの大きい方を採用する（デフォルト）</li>
     * </ul>
     */
    private void applyCondition(
            java.util.List<BattleState.ActiveCombatCondition> conditions,
            String conditionId, int intensity, int duration) {

        CombatConditionData def = CombatConditionRegistry.getConditionById(conditionId);
        String rule = (def != null && def.getStackRule() != null) ? def.getStackRule() : "REFRESH";

        switch (rule) {
            case "ACCUMULATE": {
                // 既存のインスタンスを探して強度を合算、ターンは新しい値で上書き
                java.util.Optional<BattleState.ActiveCombatCondition> existing = conditions.stream()
                        .filter(c -> c.getConditionId().equals(conditionId)).findFirst();
                if (existing.isPresent()) {
                    existing.get().setIntensity(existing.get().getIntensity() + intensity);
                    existing.get().setDuration(duration);
                } else {
                    conditions.add(new BattleState.ActiveCombatCondition(conditionId, duration, intensity));
                }
                break;
            }
            case "MAX_ONLY": {
                // 複数インスタンスをそのまま保持（判定側で最大強度を使う）
                conditions.add(new BattleState.ActiveCombatCondition(conditionId, duration, intensity));
                break;
            }
            case "EXTEND": {
                // ターンを加算して延長
                java.util.Optional<BattleState.ActiveCombatCondition> existing = conditions.stream()
                        .filter(c -> c.getConditionId().equals(conditionId)).findFirst();
                if (existing.isPresent()) {
                    existing.get().setDuration(existing.get().getDuration() + duration);
                } else {
                    conditions.add(new BattleState.ActiveCombatCondition(conditionId, duration, intensity));
                }
                break;
            }
            default: // "REFRESH": ターンの大きい方を採用
                java.util.Optional<BattleState.ActiveCombatCondition> existing = conditions.stream()
                        .filter(c -> c.getConditionId().equals(conditionId)).findFirst();
                if (existing.isPresent()) {
                    if (duration > existing.get().getDuration()) {
                        existing.get().setDuration(duration);
                    }
                } else {
                    conditions.add(new BattleState.ActiveCombatCondition(conditionId, duration, intensity));
                }
                break;
        }
    }

    /**
     * MAX_ONLY 方式の状態について、対象リストの中から指定IDの最大 intensity を返す。
     * 該当する状態がなければ 0 を返す。
     */
    private int getMaxConditionIntensity(
            java.util.List<BattleState.ActiveCombatCondition> conditions, String conditionId) {
        return conditions.stream()
                .filter(c -> c.getConditionId().equals(conditionId))
                .mapToInt(BattleState.ActiveCombatCondition::getIntensity)
                .max()
                .orElse(0);
    }

    /**
     * ScalingData に基づいてプレイヤーのSP増加量を計算する。
     * BattleManager の getCombatStat() を通じてステータスを解決する。
     *
     * @param scalingData SP計算式の定義
     * @return 計算されたSP増加量（最小値0）
     */
    private int calculateSpGainForPlayer(ScalingData scalingData) {
        int total = scalingData.getBase();
        if (scalingData.getScalings() != null) {
            for (java.util.Map.Entry<String, Double> entry : scalingData.getScalings().entrySet()) {
                total += (int) (getCombatStat(player, entry.getKey()) * entry.getValue());
            }
        }
        return Math.max(0, total);
    }
}
