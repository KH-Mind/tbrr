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
 * タイムライン型バトルインタラクション
 * 
 * 行動を「時間軸」に配置し、敵の行動と交差するタイミングで効果が変わる。
 * プレイヤーは「いつ何をするか」を決める。
 */
public class TimelineCombatInteraction implements InteractionHandler {

    private static final String TYPE = "timeline_combat";
    private final Random random = new Random();

    // 戦闘用HP
    private static final int COMBAT_HP_MAX = 100;

    // タイムラインスロット数
    private static final int TIMELINE_SLOTS = 5;

    // UI参照
    private StackPane subWindowPane;

    // タイムライン配置
    private String[] playerTimeline = new String[TIMELINE_SLOTS];
    private String[] enemyTimeline = new String[TIMELINE_SLOTS];
    private Button[] slotButtons = new Button[TIMELINE_SLOTS];

    // 選択中の行動
    private String selectedAction = null;

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

        // タイムライン初期化
        for (int i = 0; i < TIMELINE_SLOTS; i++) {
            playerTimeline[i] = null;
            enemyTimeline[i] = null;
        }
        selectedAction = null;

        // 敵データをパラメータから取得
        String enemyName = getStringParam(params, "enemyName", "未知の敵");
        int enemyMight = getIntParam(params, "enemyMight", 10);
        int enemyInsight = getIntParam(params, "enemyInsight", 10);
        int enemyFinesse = getIntParam(params, "enemyFinesse", 10);
        int enemyPresence = getIntParam(params, "enemyPresence", 10);

        // プレイヤーステータス
        CombatStats playerStats = player.getCombatStats();

        // 敵のタイムラインを生成
        generateEnemyTimeline(enemyMight, enemyFinesse);

        Platform.runLater(() -> {
            showTimelineUI(future, params, player, playerStats,
                    enemyName, enemyMight, enemyInsight, enemyFinesse, enemyPresence);
        });

        return future;
    }

    /**
     * 敵のタイムラインを生成
     */
    private void generateEnemyTimeline(int enemyMight, int enemyFinesse) {
        // 敵のパターン: 強い敵ほど攻撃的
        String[] actions = { "接近", "攻撃", "防御", "待機" };

        // 基本パターン: 接近→攻撃→待機...
        enemyTimeline[0] = "接近";
        enemyTimeline[1] = random.nextBoolean() ? "攻撃" : "待機";
        enemyTimeline[2] = "攻撃";
        enemyTimeline[3] = random.nextBoolean() ? "防御" : "待機";
        enemyTimeline[4] = "攻撃";
    }

    /**
     * タイムラインUIを表示
     */
    private void showTimelineUI(CompletableFuture<InteractionResult> future, Map<String, Object> params,
            Player player, CombatStats playerStats,
            String enemyName, int enemyMight, int enemyInsight, int enemyFinesse, int enemyPresence) {

        if (subWindowPane == null) {
            // UIが無い場合は自動判定
            boolean win = playerStats.might() + playerStats.finesse() > enemyMight + enemyFinesse;
            future.complete(new InteractionResult(win ? "success" : "failure"));
            return;
        }

        // コンテナ作成
        VBox container = InteractionUIHelper.createContainer(params, subWindowPane, 10);
        container.setAlignment(Pos.TOP_CENTER);

        // タイトル
        Label titleLabel = new Label("━━━ TIMELINE COMBAT: " + enemyName + " ━━━");
        titleLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.WHITE);

        // 説明
        Label descLabel = new Label("行動を選んでタイムラインに配置せよ！");
        descLabel.setFont(Font.font("Meiryo", 12));
        descLabel.setTextFill(Color.LIGHTGRAY);

        // タイムライン表示エリア
        VBox timelineArea = createTimelineArea(playerStats, enemyMight, enemyInsight);

        // 行動選択ボタン
        Label actionLabel = new Label("【行動を選択】");
        actionLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 12));
        actionLabel.setTextFill(Color.LIGHTYELLOW);

        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);

        Button approachBtn = createActionSelectButton("接近", "近づく", Color.STEELBLUE, playerStats);
        Button attackBtn = createActionSelectButton("攻撃", "ダメージ", Color.CRIMSON, playerStats);
        Button shootBtn = createActionSelectButton("射撃", "遠距離", Color.DARKORANGE, playerStats);
        Button defendBtn = createActionSelectButton("防御", "被ダメ減", Color.SLATEGRAY, playerStats);
        Button evadeBtn = createActionSelectButton("回避", "回避", Color.LIMEGREEN, playerStats);

        actionButtons.getChildren().addAll(approachBtn, attackBtn, shootBtn, defendBtn, evadeBtn);

        // 選択中行動表示
        Label selectedLabel = new Label("選択中: なし");
        selectedLabel.setFont(Font.font("Meiryo", 12));
        selectedLabel.setTextFill(Color.GOLD);
        selectedLabel.setId("selectedLabel");

        // 実行ボタン
        Button executeBtn = new Button("戦闘実行");
        executeBtn.setFont(Font.font("Meiryo", FontWeight.BOLD, 16));
        executeBtn.setStyle("-fx-background-color: #c44; -fx-text-fill: white;");
        executeBtn.setPrefWidth(200);
        executeBtn.setPrefHeight(40);
        executeBtn.setOnAction(e -> {
            executeCombat(future, container, player, playerStats,
                    enemyName, enemyMight, enemyInsight, enemyFinesse, enemyPresence);
        });

        // クリアボタン
        Button clearBtn = new Button("クリア");
        clearBtn.setFont(Font.font("Meiryo", 12));
        clearBtn.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
        clearBtn.setOnAction(e -> {
            for (int i = 0; i < TIMELINE_SLOTS; i++) {
                playerTimeline[i] = null;
                slotButtons[i].setText("空");
                slotButtons[i].setStyle("-fx-background-color: #444; -fx-text-fill: #888;");
            }
            selectedAction = null;
            selectedLabel.setText("選択中: なし");
        });

        HBox buttonBox = new HBox(10, clearBtn, executeBtn);
        buttonBox.setAlignment(Pos.CENTER);

        container.getChildren().addAll(
                titleLabel, descLabel, timelineArea, actionLabel, actionButtons, selectedLabel, buttonBox);

        // 行動選択ボタンのイベント設定
        setupActionSelectButton(approachBtn, "接近", selectedLabel);
        setupActionSelectButton(attackBtn, "攻撃", selectedLabel);
        setupActionSelectButton(shootBtn, "射撃", selectedLabel);
        setupActionSelectButton(defendBtn, "防御", selectedLabel);
        setupActionSelectButton(evadeBtn, "回避", selectedLabel);
    }

    /**
     * タイムラインエリアを作成
     */
    private VBox createTimelineArea(CombatStats playerStats, int enemyMight, int enemyInsight) {
        VBox area = new VBox(5);
        area.setAlignment(Pos.CENTER);
        area.setPadding(new Insets(10));

        // 時間軸ラベル
        HBox timeLabels = new HBox(5);
        timeLabels.setAlignment(Pos.CENTER);
        for (int i = 0; i < TIMELINE_SLOTS; i++) {
            Label lbl = new Label("[" + (i + 1) + "]");
            lbl.setMinWidth(60);
            lbl.setAlignment(Pos.CENTER);
            lbl.setFont(Font.font("Meiryo", 10));
            lbl.setTextFill(Color.GRAY);
            timeLabels.getChildren().add(lbl);
        }

        // 敵タイムライン
        Label enemyLabel = new Label("敵:");
        enemyLabel.setFont(Font.font("Meiryo", 11));
        enemyLabel.setTextFill(Color.LIGHTCORAL);

        HBox enemyRow = new HBox(5);
        enemyRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < TIMELINE_SLOTS; i++) {
            String action = enemyTimeline[i];
            // 聡明が高いと敵の行動が見える
            boolean visible = (i == 0) || (i == 2) || (playerStats.insight() > 15);
            Label slot = new Label(visible ? action : "???");
            slot.setMinWidth(60);
            slot.setMinHeight(30);
            slot.setAlignment(Pos.CENTER);
            slot.setStyle("-fx-background-color: #533; -fx-text-fill: " + (visible ? "white" : "#888") + ";");
            enemyRow.getChildren().add(slot);
        }

        // プレイヤータイムライン
        Label playerLabel = new Label("自分:");
        playerLabel.setFont(Font.font("Meiryo", 11));
        playerLabel.setTextFill(Color.LIGHTGREEN);

        HBox playerRow = new HBox(5);
        playerRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < TIMELINE_SLOTS; i++) {
            Button slot = new Button("空");
            slot.setMinWidth(60);
            slot.setMinHeight(30);
            slot.setStyle("-fx-background-color: #444; -fx-text-fill: #888;");
            final int slotIndex = i;
            slot.setOnAction(e -> placeAction(slotIndex));
            slotButtons[i] = slot;
            playerRow.getChildren().add(slot);
        }

        HBox enemyBox = new HBox(10, enemyLabel, enemyRow);
        enemyBox.setAlignment(Pos.CENTER);
        HBox playerBox = new HBox(10, playerLabel, playerRow);
        playerBox.setAlignment(Pos.CENTER);

        area.getChildren().addAll(timeLabels, enemyBox, playerBox);
        return area;
    }

    /**
     * 行動選択ボタンを作成
     */
    private Button createActionSelectButton(String action, String desc, Color color, CombatStats stats) {
        Button btn = new Button(action);
        btn.setFont(Font.font("Meiryo", 11));
        btn.setStyle("-fx-background-color: " + toRgbString(color) + "; -fx-text-fill: white;");
        btn.setPrefWidth(60);
        return btn;
    }

    /**
     * 行動選択ボタンのイベント設定
     */
    private void setupActionSelectButton(Button btn, String action, Label selectedLabel) {
        btn.setOnAction(e -> {
            selectedAction = action;
            selectedLabel.setText("選択中: " + action);
        });
    }

    /**
     * タイムラインに行動を配置
     */
    private void placeAction(int slotIndex) {
        if (selectedAction == null)
            return;
        if (playerTimeline[slotIndex] != null)
            return; // 既に配置済み

        playerTimeline[slotIndex] = selectedAction;
        slotButtons[slotIndex].setText(selectedAction);
        slotButtons[slotIndex].setStyle("-fx-background-color: #446; -fx-text-fill: white;");
    }

    /**
     * 戦闘を実行
     */
    private void executeCombat(CompletableFuture<InteractionResult> future, VBox container,
            Player player, CombatStats playerStats,
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
        final int[] playerHp = { COMBAT_HP_MAX };
        final int[] enemyHp = { COMBAT_HP_MAX };

        HBox playerHpBar = createHpBar(player.getName(), playerHp[0], Color.LIMEGREEN);
        HBox enemyHpBar = createHpBar(enemyName, enemyHp[0], Color.TOMATO);
        logArea.getChildren().addAll(playerHpBar, enemyHpBar);

        container.getChildren().add(logArea);

        // 距離管理（近づくと近接攻撃可能）
        final boolean[] inRange = { false };

        // ターンごとに処理
        Timeline combat = new Timeline();
        final int[] currentSlot = { 0 };

        KeyFrame frame = new KeyFrame(Duration.millis(1000), e -> {
            int slot = currentSlot[0];
            if (slot >= TIMELINE_SLOTS) {
                combat.stop();
                boolean victory = enemyHp[0] <= 0 || (playerHp[0] > 0 && playerHp[0] >= enemyHp[0]);
                showResult(logArea, victory, playerHp[0], future);
                return;
            }

            String pAction = playerTimeline[slot] != null ? playerTimeline[slot] : "待機";
            String eAction = enemyTimeline[slot] != null ? enemyTimeline[slot] : "待機";

            addLogLine(logArea, "【ターン" + (slot + 1) + "】", Color.WHITE);

            // プレイヤー行動
            int pDamage = processAction(pAction, playerStats, enemyMight, enemyFinesse, inRange, true, logArea,
                    player.getName());
            if (pDamage > 0) {
                enemyHp[0] = Math.max(0, enemyHp[0] - pDamage);
                addLogLine(logArea, "→ " + enemyName + "に" + pDamage + "ダメージ！", Color.LIGHTGREEN);
                updateHpBar(enemyHpBar, enemyName, enemyHp[0], Color.TOMATO);
            }

            // 敵行動
            int eDamage = processEnemyAction(eAction, enemyMight, enemyFinesse, playerStats, inRange, logArea,
                    enemyName);
            if (eDamage > 0) {
                playerHp[0] = Math.max(0, playerHp[0] - eDamage);
                addLogLine(logArea, "→ " + player.getName() + "に" + eDamage + "ダメージ！", Color.LIGHTCORAL);
                updateHpBar(playerHpBar, player.getName(), playerHp[0], Color.LIMEGREEN);
            }

            // 勝敗判定
            if (playerHp[0] <= 0) {
                combat.stop();
                showResult(logArea, false, 0, future);
                return;
            }
            if (enemyHp[0] <= 0) {
                combat.stop();
                showResult(logArea, true, playerHp[0], future);
                return;
            }

            currentSlot[0]++;
        });

        combat.getKeyFrames().add(frame);
        combat.setCycleCount(TIMELINE_SLOTS + 1);
        combat.play();
    }

    /**
     * プレイヤー行動を処理
     */
    private int processAction(String action, CombatStats stats, int enemyMight, int enemyFinesse,
            boolean[] inRange, boolean isPlayer, VBox logArea, String name) {
        switch (action) {
            case "接近":
                inRange[0] = true;
                addLogLine(logArea, name + "は接近した！", Color.LIGHTBLUE);
                return 0;
            case "攻撃":
                if (inRange[0]) {
                    addLogLine(logArea, name + "の近接攻撃！", Color.LIGHTGREEN);
                    return stats.might() + random.nextInt(5);
                } else {
                    addLogLine(logArea, name + "は攻撃しようとしたが距離が遠い！", Color.GRAY);
                    return 0;
                }
            case "射撃":
                addLogLine(logArea, name + "の射撃！", Color.ORANGE);
                return stats.finesse() + random.nextInt(3);
            case "防御":
                addLogLine(logArea, name + "は防御態勢を取った", Color.SLATEGRAY);
                return 0;
            case "回避":
                addLogLine(logArea, name + "は回避態勢を取った", Color.LIMEGREEN);
                return 0;
            default:
                addLogLine(logArea, name + "は様子を見ている", Color.GRAY);
                return 0;
        }
    }

    /**
     * 敵行動を処理
     */
    private int processEnemyAction(String action, int enemyMight, int enemyFinesse, CombatStats playerStats,
            boolean[] inRange, VBox logArea, String name) {
        switch (action) {
            case "接近":
                inRange[0] = true;
                addLogLine(logArea, name + "が接近してきた！", Color.LIGHTCORAL);
                return 0;
            case "攻撃":
                if (inRange[0]) {
                    addLogLine(logArea, name + "の攻撃！", Color.LIGHTCORAL);
                    return enemyMight + random.nextInt(5);
                } else {
                    addLogLine(logArea, name + "は攻撃できない（距離が遠い）", Color.GRAY);
                    return 0;
                }
            case "防御":
                addLogLine(logArea, name + "は防御態勢", Color.GRAY);
                return 0;
            default:
                addLogLine(logArea, name + "は待機している", Color.GRAY);
                return 0;
        }
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
        line.setFont(Font.font("Meiryo", 11));
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
