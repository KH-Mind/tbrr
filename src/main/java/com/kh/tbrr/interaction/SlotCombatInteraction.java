package com.kh.tbrr.interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import com.kh.tbrr.data.SkillStatsMapper.CombatStats;
import com.kh.tbrr.data.models.Player;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * スロット配置型バトルインタラクション
 * 
 * プレイヤーの技能/アイテムを戦闘スロットに配置し、
 * 配置によってシナジー効果が発生する。
 */
public class SlotCombatInteraction implements InteractionHandler {

    private static final String TYPE = "slot_combat";
    private final Random random = new Random();

    // 戦闘用HP
    private static final int COMBAT_HP_MAX = 100;

    // スロット数（前衛3、後衛3）
    private static final int SLOT_COUNT = 6;

    // UI参照
    private StackPane subWindowPane;

    // 配置されたスキル
    private String[] slots = new String[SLOT_COUNT];
    private Label[] slotLabels = new Label[SLOT_COUNT];

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public CompletableFuture<InteractionResult> execute(Map<String, Object> params, Player player) {
        CompletableFuture<InteractionResult> future = new CompletableFuture<>();

        // UI参照を取得
        if (params.containsKey("_subWindowPane")) {
            subWindowPane = (StackPane) params.get("_subWindowPane");
        }

        // スロット初期化
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = null;
        }

        // 敵データをパラメータから取得
        String enemyName = getStringParam(params, "enemyName", "未知の敵");
        int enemyMight = getIntParam(params, "enemyMight", 10);
        int enemyInsight = getIntParam(params, "enemyInsight", 10);
        int enemyFinesse = getIntParam(params, "enemyFinesse", 10);
        int enemyPresence = getIntParam(params, "enemyPresence", 10);

        // プレイヤーの技能リスト取得
        List<String> playerSkills = player.getEffectiveSkills();

        Platform.runLater(() -> {
            showSlotUI(future, params, player, playerSkills,
                    enemyName, enemyMight, enemyInsight, enemyFinesse, enemyPresence);
        });

        return future;
    }

    /**
     * スロット配置UIを表示
     */
    private void showSlotUI(CompletableFuture<InteractionResult> future, Map<String, Object> params,
            Player player, List<String> playerSkills,
            String enemyName, int enemyMight, int enemyInsight, int enemyFinesse, int enemyPresence) {

        if (subWindowPane == null) {
            // UIが無い場合は自動判定
            CombatStats stats = player.getCombatStats();
            boolean win = stats.might() + stats.finesse() > enemyMight + enemyFinesse;
            future.complete(new InteractionResult(win ? "success" : "failure"));
            return;
        }

        // コンテナ作成
        VBox container = InteractionUIHelper.createContainer(params, subWindowPane, 10);
        container.setAlignment(Pos.TOP_CENTER);

        // タイトル
        Label titleLabel = new Label("━━━ SLOT COMBAT: " + enemyName + " ━━━");
        titleLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.WHITE);

        // 敵ステータス表示
        Label enemyStatsLabel = new Label(String.format(
                "%s: 強靭%d 聡明%d 機敏%d 風格%d",
                enemyName, enemyMight, enemyInsight, enemyFinesse, enemyPresence));
        enemyStatsLabel.setFont(Font.font("Meiryo", 12));
        enemyStatsLabel.setTextFill(Color.LIGHTCORAL);

        // スロットエリア
        VBox slotArea = createSlotArea();

        // 技能選択エリア
        Label skillLabel = new Label("【配置する技能を選択】");
        skillLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 14));
        skillLabel.setTextFill(Color.LIGHTYELLOW);

        HBox skillButtons = new HBox(5);
        skillButtons.setAlignment(Pos.CENTER);

        for (String skill : playerSkills) {
            Button btn = new Button(skill);
            btn.setFont(Font.font("Meiryo", 11));
            btn.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
            btn.setOnAction(e -> {
                // 空いているスロットに配置
                for (int i = 0; i < SLOT_COUNT; i++) {
                    if (slots[i] == null) {
                        slots[i] = skill;
                        slotLabels[i].setText(skill);
                        slotLabels[i].setStyle("-fx-background-color: #446; -fx-text-fill: white; -fx-padding: 10;");
                        btn.setDisable(true);
                        updateSynergyDisplay(slotArea);
                        break;
                    }
                }
            });
            skillButtons.getChildren().add(btn);
        }

        // シナジー表示
        Label synergyLabel = new Label("");
        synergyLabel.setFont(Font.font("Meiryo", 12));
        synergyLabel.setTextFill(Color.GOLD);
        synergyLabel.setId("synergyLabel");

        // 戦闘開始ボタン
        Button startBtn = new Button("戦闘開始");
        startBtn.setFont(Font.font("Meiryo", FontWeight.BOLD, 16));
        startBtn.setStyle("-fx-background-color: #c44; -fx-text-fill: white;");
        startBtn.setPrefWidth(200);
        startBtn.setPrefHeight(40);
        startBtn.setOnAction(e -> {
            executeCombat(future, container, player, slots,
                    enemyName, enemyMight, enemyInsight, enemyFinesse, enemyPresence);
        });

        // クリアボタン
        Button clearBtn = new Button("配置クリア");
        clearBtn.setFont(Font.font("Meiryo", 12));
        clearBtn.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
        clearBtn.setOnAction(e -> {
            for (int i = 0; i < SLOT_COUNT; i++) {
                slots[i] = null;
                slotLabels[i].setText("空");
                slotLabels[i].setStyle("-fx-background-color: #333; -fx-text-fill: #888; -fx-padding: 10;");
            }
            for (var node : skillButtons.getChildren()) {
                if (node instanceof Button) {
                    ((Button) node).setDisable(false);
                }
            }
            updateSynergyDisplay(slotArea);
        });

        HBox buttonBox = new HBox(10, clearBtn, startBtn);
        buttonBox.setAlignment(Pos.CENTER);

        container.getChildren().addAll(
                titleLabel, enemyStatsLabel, slotArea, skillLabel, skillButtons, synergyLabel, buttonBox);

        // synergyLabelの参照を保存
        slotArea.setUserData(synergyLabel);
    }

    /**
     * スロットエリアを作成
     */
    private VBox createSlotArea() {
        VBox area = new VBox(5);
        area.setAlignment(Pos.CENTER);
        area.setPadding(new Insets(10));

        Label frontLabel = new Label("【前衛】攻撃向き");
        frontLabel.setFont(Font.font("Meiryo", 11));
        frontLabel.setTextFill(Color.LIGHTGRAY);

        GridPane frontRow = new GridPane();
        frontRow.setHgap(5);
        frontRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Label slot = new Label("空");
            slot.setMinWidth(80);
            slot.setMinHeight(40);
            slot.setAlignment(Pos.CENTER);
            slot.setStyle("-fx-background-color: #333; -fx-text-fill: #888; -fx-padding: 10;");
            slotLabels[i] = slot;
            frontRow.add(slot, i, 0);
        }

        Label backLabel = new Label("【後衛】支援向き");
        backLabel.setFont(Font.font("Meiryo", 11));
        backLabel.setTextFill(Color.LIGHTGRAY);

        GridPane backRow = new GridPane();
        backRow.setHgap(5);
        backRow.setAlignment(Pos.CENTER);
        for (int i = 3; i < 6; i++) {
            Label slot = new Label("空");
            slot.setMinWidth(80);
            slot.setMinHeight(40);
            slot.setAlignment(Pos.CENTER);
            slot.setStyle("-fx-background-color: #333; -fx-text-fill: #888; -fx-padding: 10;");
            slotLabels[i] = slot;
            backRow.add(slot, i - 3, 0);
        }

        area.getChildren().addAll(frontLabel, frontRow, backLabel, backRow);
        return area;
    }

    /**
     * シナジー表示を更新
     */
    private void updateSynergyDisplay(VBox slotArea) {
        Label synergyLabel = (Label) slotArea.getScene().lookup("#synergyLabel");
        if (synergyLabel == null)
            return;

        List<String> synergies = detectSynergies();
        if (synergies.isEmpty()) {
            synergyLabel.setText("");
        } else {
            synergyLabel.setText("★シナジー発動: " + String.join(", ", synergies));
        }
    }

    /**
     * シナジーを検出
     */
    private List<String> detectSynergies() {
        List<String> synergies = new ArrayList<>();
        List<String> placed = new ArrayList<>();
        for (String s : slots) {
            if (s != null)
                placed.add(s);
        }

        // シナジー定義
        if (placed.contains("隠密") && placed.contains("軽業")) {
            synergies.add("暗殺（攻撃2倍）");
        }
        if (placed.contains("筋力") && placed.contains("耐久力")) {
            synergies.add("鉄壁（防御2倍）");
        }
        if (placed.contains("魔法の知識") && placed.contains("話術")) {
            synergies.add("威圧（敵攻撃-50%）");
        }
        if (placed.contains("敏捷力") && placed.contains("運動")) {
            synergies.add("疾風（先制攻撃）");
        }
        if (placed.contains("知力") && placed.contains("判断力")) {
            synergies.add("洞察（敵行動読み）");
        }

        return synergies;
    }

    /**
     * 戦闘を実行
     */
    private void executeCombat(CompletableFuture<InteractionResult> future, VBox container,
            Player player, String[] slots,
            String enemyName, int enemyMight, int enemyInsight, int enemyFinesse, int enemyPresence) {

        container.getChildren().clear();

        // 戦闘ログ
        VBox logArea = new VBox(5);
        logArea.setAlignment(Pos.TOP_LEFT);
        logArea.setPadding(new Insets(10));

        Label titleLabel = new Label("━━━ 戦闘開始 ━━━");
        titleLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.WHITE);
        logArea.getChildren().add(titleLabel);

        // HPバー
        int[] playerHp = { COMBAT_HP_MAX };
        int[] enemyHp = { COMBAT_HP_MAX };

        HBox playerHpBar = createHpBar(player.getName(), playerHp[0], Color.LIMEGREEN);
        HBox enemyHpBar = createHpBar(enemyName, enemyHp[0], Color.TOMATO);
        logArea.getChildren().addAll(playerHpBar, enemyHpBar);

        container.getChildren().add(logArea);

        // ステータス計算
        CombatStats playerStats = player.getCombatStats();
        int playerAttack = playerStats.might();
        int playerDefense = playerStats.finesse();

        // 配置ボーナス
        int frontBonus = 0;
        int backBonus = 0;
        for (int i = 0; i < 3; i++) {
            if (slots[i] != null)
                frontBonus += 3; // 前衛配置ボーナス
        }
        for (int i = 3; i < 6; i++) {
            if (slots[i] != null)
                backBonus += 2; // 後衛配置ボーナス
        }

        // シナジー効果
        List<String> synergies = detectSynergies();
        double attackMult = 1.0;
        double defenseMult = 1.0;
        double enemyAttackMult = 1.0;
        final boolean[] preemptive = { false };

        for (String syn : synergies) {
            if (syn.contains("暗殺"))
                attackMult *= 2.0;
            if (syn.contains("鉄壁"))
                defenseMult *= 2.0;
            if (syn.contains("威圧"))
                enemyAttackMult *= 0.5;
            if (syn.contains("疾風"))
                preemptive[0] = true;
        }

        int finalPlayerAttack = (int) ((playerAttack + frontBonus) * attackMult);
        int finalPlayerDefense = (int) ((playerDefense + backBonus) * defenseMult);
        int finalEnemyAttack = (int) (enemyMight * enemyAttackMult);

        // 戦闘ログ追加
        addLogLine(logArea, "配置ボーナス: 前衛+" + frontBonus + " 後衛+" + backBonus, Color.LIGHTGRAY);
        if (!synergies.isEmpty()) {
            addLogLine(logArea, "★シナジー発動: " + String.join(", ", synergies), Color.GOLD);
        }

        // 戦闘処理（オートバトル）
        Timeline combat = new Timeline();
        int[] turn = { 0 };

        KeyFrame frame = new KeyFrame(Duration.millis(800), e -> {
            turn[0]++;

            // 先制攻撃
            if (turn[0] == 1 && preemptive[0]) {
                int dmg = finalPlayerAttack + random.nextInt(5);
                enemyHp[0] = Math.max(0, enemyHp[0] - dmg);
                addLogLine(logArea, "【先制攻撃】" + player.getName() + "の攻撃！ " + dmg + "ダメージ！", Color.LIMEGREEN);
                updateHpBar(enemyHpBar, enemyName, enemyHp[0], Color.TOMATO);
            }

            // 通常ターン
            if (enemyHp[0] > 0) {
                int playerDmg = finalPlayerAttack + random.nextInt(5);
                enemyHp[0] = Math.max(0, enemyHp[0] - playerDmg);
                addLogLine(logArea, player.getName() + "の攻撃！ " + playerDmg + "ダメージ！", Color.LIGHTGREEN);
                updateHpBar(enemyHpBar, enemyName, enemyHp[0], Color.TOMATO);
            }

            if (enemyHp[0] > 0 && playerHp[0] > 0) {
                int enemyDmg = Math.max(0, finalEnemyAttack + random.nextInt(5) - finalPlayerDefense / 2);
                playerHp[0] = Math.max(0, playerHp[0] - enemyDmg);
                addLogLine(logArea, enemyName + "の攻撃！ " + enemyDmg + "ダメージ！", Color.LIGHTCORAL);
                updateHpBar(playerHpBar, player.getName(), playerHp[0], Color.LIMEGREEN);
            }

            // 勝敗判定
            if (enemyHp[0] <= 0 || playerHp[0] <= 0 || turn[0] >= 5) {
                combat.stop();
                boolean victory = enemyHp[0] <= 0 || (playerHp[0] > 0 && turn[0] >= 5 && playerHp[0] > enemyHp[0]);
                showResult(logArea, victory, playerHp[0], future);
            }
        });

        combat.getKeyFrames().add(frame);
        combat.setCycleCount(10);
        combat.play();
    }

    /**
     * 結果表示
     */
    private void showResult(VBox logArea, boolean victory, int remainingHp,
            CompletableFuture<InteractionResult> future) {
        String resultText = victory ? "━━━ 勝利！ ━━━" : "━━━ 敗北... ━━━";
        Color resultColor = victory ? Color.GOLD : Color.TOMATO;

        Label resultLabel = new Label(resultText);
        resultLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 20));
        resultLabel.setTextFill(resultColor);
        logArea.getChildren().add(resultLabel);

        Timeline delay = new Timeline(new KeyFrame(Duration.millis(2000), ev -> {
            future.complete(new InteractionResult(victory ? "success" : "failure"));
        }));
        delay.play();
    }

    // ========== ユーティリティ ==========

    private void addLogLine(VBox logArea, String text, Color color) {
        Label line = new Label(text);
        line.setFont(Font.font("Meiryo", 12));
        line.setTextFill(color);
        logArea.getChildren().add(line);
    }

    private HBox createHpBar(String name, int hp, Color color) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(name + ":");
        nameLabel.setFont(Font.font("Meiryo", 12));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setMinWidth(80);

        ProgressBar bar = new ProgressBar((double) hp / COMBAT_HP_MAX);
        bar.setPrefWidth(150);
        bar.setStyle("-fx-accent: " + toRgbString(color) + ";");

        Label hpLabel = new Label(hp + "/" + COMBAT_HP_MAX);
        hpLabel.setFont(Font.font("Meiryo", 11));
        hpLabel.setTextFill(Color.LIGHTGRAY);

        box.getChildren().addAll(nameLabel, bar, hpLabel);
        return box;
    }

    private void updateHpBar(HBox hpBar, String name, int hp, Color color) {
        ProgressBar bar = (ProgressBar) hpBar.getChildren().get(1);
        Label hpLabel = (Label) hpBar.getChildren().get(2);
        bar.setProgress((double) hp / COMBAT_HP_MAX);
        hpLabel.setText(hp + "/" + COMBAT_HP_MAX);
    }

    private String toRgbString(Color color) {
        return String.format("rgb(%d,%d,%d)",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params != null && params.containsKey(key)) {
            Object val = params.get(key);
            return val != null ? val.toString() : defaultValue;
        }
        return defaultValue;
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params != null && params.containsKey(key)) {
            Object val = params.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
        }
        return defaultValue;
    }
}
