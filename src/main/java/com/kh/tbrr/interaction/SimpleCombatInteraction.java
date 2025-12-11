package com.kh.tbrr.interaction;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * シンプル戦闘インタラクション（HP削り合い型）
 * 
 * プレイヤーと敵がターン制で行動を選択し、HPを削り合う。
 * 戦闘用HPを使用し、結果を既存のhpChange形式（low_damage等）で返す。
 */
public class SimpleCombatInteraction implements InteractionHandler {

    private static final String TYPE = "simple_combat";
    private final Random random = new Random();

    // 戦闘用HP（固定値、後で調整可能）
    private static final int COMBAT_HP_MAX = 100;

    // UI参照
    private StackPane subWindowPane;

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

        // 敵データをパラメータから取得
        String enemyName = getStringParam(params, "enemyName", "未知の敵");
        int enemyMight = getIntParam(params, "enemyMight", 10);
        int enemyInsight = getIntParam(params, "enemyInsight", 10);
        int enemyFinesse = getIntParam(params, "enemyFinesse", 10);
        int enemyPresence = getIntParam(params, "enemyPresence", 10);

        // プレイヤーステータス取得
        CombatStats playerStats = player.getCombatStats();

        Platform.runLater(() -> {
            showCombatUI(future, params, player, playerStats,
                    enemyName, enemyMight, enemyInsight, enemyFinesse, enemyPresence);
        });

        return future;
    }

    /**
     * 戦闘UIを表示
     */
    private void showCombatUI(CompletableFuture<InteractionResult> future, Map<String, Object> params,
            Player player, CombatStats playerStats,
            String enemyName, int enemyMight, int enemyInsight, int enemyFinesse, int enemyPresence) {

        if (subWindowPane == null) {
            // UIが無い場合は自動判定
            boolean win = playerStats.might() + playerStats.finesse() > enemyMight + enemyFinesse;
            future.complete(new InteractionResult(win ? "success" : "failure"));
            return;
        }

        // 戦闘用HP
        final int[] playerCombatHp = { COMBAT_HP_MAX };
        final int[] enemyCombatHp = { COMBAT_HP_MAX };

        // コンテナ作成
        VBox container = InteractionUIHelper.createContainer(params, subWindowPane, 15);
        container.setAlignment(Pos.TOP_CENTER);

        // タイトル
        Label titleLabel = new Label("━━━ COMBAT: " + enemyName + " ━━━");
        titleLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.WHITE);

        // HPバー表示エリア
        VBox hpArea = createHpDisplayArea(player.getName(), playerCombatHp[0], enemyName, enemyCombatHp[0]);

        // ステータス表示
        Label playerStatsLabel = new Label(String.format(
                "あなた: 強靭%d 聡明%d 機敏%d 風格%d",
                playerStats.might(), playerStats.insight(), playerStats.finesse(), playerStats.presence()));
        playerStatsLabel.setFont(Font.font("Meiryo", 12));
        playerStatsLabel.setTextFill(Color.LIGHTGRAY);

        Label enemyStatsLabel = new Label(String.format(
                "%s: 強靭%d 聡明%d 機敏%d 風格%d",
                enemyName, enemyMight, enemyInsight, enemyFinesse, enemyPresence));
        enemyStatsLabel.setFont(Font.font("Meiryo", 12));
        enemyStatsLabel.setTextFill(Color.LIGHTCORAL);

        // 行動選択ボタン
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button attackBtn = createActionButton("通常攻撃", "強靭で攻撃", Color.CRIMSON);
        Button defendBtn = createActionButton("防御", "被ダメ軽減", Color.STEELBLUE);
        Button tacticsBtn = createActionButton("策略", "聡明で攻撃", Color.MEDIUMPURPLE);
        Button evadeBtn = createActionButton("回避", "機敏で回避", Color.LIMEGREEN);

        buttonBox.getChildren().addAll(attackBtn, defendBtn, tacticsBtn, evadeBtn);

        // 戦闘ログ
        Label logLabel = new Label("行動を選択してください");
        logLabel.setFont(Font.font("Meiryo", 14));
        logLabel.setTextFill(Color.LIGHTYELLOW);
        logLabel.setWrapText(true);

        container.getChildren().addAll(titleLabel, hpArea, playerStatsLabel, enemyStatsLabel, buttonBox, logLabel);

        // 行動処理
        attackBtn.setOnAction(e -> processTurn(future, container, hpArea, logLabel,
                playerCombatHp, enemyCombatHp, player.getName(), enemyName,
                playerStats, enemyMight, enemyInsight, enemyFinesse, enemyPresence,
                "attack"));
        defendBtn.setOnAction(e -> processTurn(future, container, hpArea, logLabel,
                playerCombatHp, enemyCombatHp, player.getName(), enemyName,
                playerStats, enemyMight, enemyInsight, enemyFinesse, enemyPresence,
                "defend"));
        tacticsBtn.setOnAction(e -> processTurn(future, container, hpArea, logLabel,
                playerCombatHp, enemyCombatHp, player.getName(), enemyName,
                playerStats, enemyMight, enemyInsight, enemyFinesse, enemyPresence,
                "tactics"));
        evadeBtn.setOnAction(e -> processTurn(future, container, hpArea, logLabel,
                playerCombatHp, enemyCombatHp, player.getName(), enemyName,
                playerStats, enemyMight, enemyInsight, enemyFinesse, enemyPresence,
                "evade"));
    }

    /**
     * 1ターン処理
     */
    private void processTurn(CompletableFuture<InteractionResult> future, VBox container, VBox hpArea, Label logLabel,
            int[] playerHp, int[] enemyHp, String playerName, String enemyName,
            CombatStats playerStats, int enemyMight, int enemyInsight, int enemyFinesse, int enemyPresence,
            String playerAction) {

        // 敵の行動（ランダム）
        String[] enemyActions = { "attack", "defend", "tactics", "evade" };
        String enemyAction = enemyActions[random.nextInt(enemyActions.length)];

        // プレイヤーのダメージ計算
        int playerDamage = calculateDamage(playerStats, playerAction, enemyMight, enemyFinesse, enemyAction);
        // 敵のダメージ計算
        int enemyDamage = calculateEnemyDamage(enemyMight, enemyInsight, enemyFinesse, enemyAction,
                playerStats, playerAction);

        // HP適用
        enemyHp[0] = Math.max(0, enemyHp[0] - playerDamage);
        playerHp[0] = Math.max(0, playerHp[0] - enemyDamage);

        // ログ更新
        String log = String.format("%s: %s → %dダメージ\n%s: %s → %dダメージ",
                playerName, getActionName(playerAction), playerDamage,
                enemyName, getActionName(enemyAction), enemyDamage);
        logLabel.setText(log);

        // HPバー更新
        updateHpDisplay(hpArea, playerName, playerHp[0], enemyName, enemyHp[0]);

        // 勝敗判定
        if (enemyHp[0] <= 0) {
            // 勝利
            String result = calculateResult(playerHp[0]);
            Timeline delay = new Timeline(new KeyFrame(Duration.millis(1500), ev -> {
                showResult(container, true, playerHp[0], future, result);
            }));
            delay.play();
        } else if (playerHp[0] <= 0) {
            // 敗北
            Timeline delay = new Timeline(new KeyFrame(Duration.millis(1500), ev -> {
                showResult(container, false, 0, future, "failure");
            }));
            delay.play();
        }
    }

    /**
     * プレイヤーのダメージ計算
     */
    private int calculateDamage(CombatStats player, String action, int enemyMight, int enemyFinesse,
            String enemyAction) {
        int baseDamage = switch (action) {
            case "attack" -> player.might() + random.nextInt(5);
            case "tactics" -> player.insight() + random.nextInt(5);
            case "defend" -> 0;
            case "evade" -> player.finesse() / 2;
            default -> 5;
        };

        // 敵が防御中はダメージ半減
        if ("defend".equals(enemyAction)) {
            baseDamage /= 2;
        }

        return Math.max(0, baseDamage);
    }

    /**
     * 敵のダメージ計算
     */
    private int calculateEnemyDamage(int enemyMight, int enemyInsight, int enemyFinesse, String enemyAction,
            CombatStats player, String playerAction) {
        int baseDamage = switch (enemyAction) {
            case "attack" -> enemyMight + random.nextInt(5);
            case "tactics" -> enemyInsight + random.nextInt(5);
            case "defend" -> 0;
            case "evade" -> enemyFinesse / 2;
            default -> 5;
        };

        // プレイヤーが防御中はダメージ半減
        if ("defend".equals(playerAction)) {
            baseDamage /= 2;
        }

        // プレイヤーが回避中は機敏判定
        if ("evade".equals(playerAction)) {
            if (random.nextInt(100) < player.finesse() * 2) {
                baseDamage = 0; // 回避成功
            }
        }

        return Math.max(0, baseDamage);
    }

    /**
     * 戦闘結果を計算（残りHP%から）
     */
    private String calculateResult(int remainingHp) {
        double hpPercent = (double) remainingHp / COMBAT_HP_MAX * 100;
        if (hpPercent >= 90) {
            return "flawless"; // 無傷
        } else if (hpPercent >= 70) {
            return "low_damage";
        } else if (hpPercent >= 40) {
            return "medium_damage";
        } else {
            return "large_damage";
        }
    }

    /**
     * 結果表示
     */
    private void showResult(VBox container, boolean victory, int remainingHp,
            CompletableFuture<InteractionResult> future, String resultKey) {
        container.getChildren().clear();

        String resultText = victory ? "勝利！" : "敗北...";
        Color resultColor = victory ? Color.GOLD : Color.TOMATO;

        Label resultLabel = new Label(resultText);
        resultLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 36));
        resultLabel.setTextFill(resultColor);

        String damageText = switch (resultKey) {
            case "flawless" -> "ほぼ無傷で勝利した！";
            case "low_damage" -> "軽傷を負ったが勝利した。";
            case "medium_damage" -> "それなりの傷を負いながらも勝利した。";
            case "large_damage" -> "満身創痍だが、なんとか勝利した...";
            case "failure" -> "力尽きてしまった...";
            default -> "";
        };

        Label damageLabel = new Label(damageText);
        damageLabel.setFont(Font.font("Meiryo", 16));
        damageLabel.setTextFill(Color.LIGHTGRAY);

        container.getChildren().addAll(resultLabel, damageLabel);

        // 少し待ってから結果を返す
        Timeline delay = new Timeline(new KeyFrame(Duration.millis(2000), ev -> {
            // resultKeyに勝敗、将来的にdamageLevelも含められるように
            // 現在は単純にsuccess/failureで返す
            future.complete(new InteractionResult(victory ? "success" : "failure"));
        }));
        delay.play();
    }

    // ========== ユーティリティ ==========

    private VBox createHpDisplayArea(String playerName, int playerHp, String enemyName, int enemyHp) {
        VBox area = new VBox(5);
        area.setAlignment(Pos.CENTER);
        area.setPadding(new Insets(10));

        // プレイヤーHP
        HBox playerHpBox = createHpBar(playerName, playerHp, Color.LIMEGREEN);
        // 敵HP
        HBox enemyHpBox = createHpBar(enemyName, enemyHp, Color.TOMATO);

        area.getChildren().addAll(playerHpBox, enemyHpBox);
        return area;
    }

    private HBox createHpBar(String name, int hp, Color color) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(name + ":");
        nameLabel.setFont(Font.font("Meiryo", 14));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setMinWidth(100);

        ProgressBar bar = new ProgressBar((double) hp / COMBAT_HP_MAX);
        bar.setPrefWidth(200);
        bar.setStyle("-fx-accent: " + toRgbString(color) + ";");

        Label hpLabel = new Label(hp + "/" + COMBAT_HP_MAX);
        hpLabel.setFont(Font.font("Meiryo", 12));
        hpLabel.setTextFill(Color.LIGHTGRAY);

        box.getChildren().addAll(nameLabel, bar, hpLabel);
        return box;
    }

    private void updateHpDisplay(VBox hpArea, String playerName, int playerHp, String enemyName, int enemyHp) {
        hpArea.getChildren().clear();
        hpArea.getChildren().addAll(
                createHpBar(playerName, playerHp, Color.LIMEGREEN),
                createHpBar(enemyName, enemyHp, Color.TOMATO));
    }

    private Button createActionButton(String text, String tooltip, Color color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Meiryo", FontWeight.BOLD, 14));
        btn.setStyle("-fx-background-color: " + toRgbString(color) + "; -fx-text-fill: white;");
        btn.setPrefWidth(100);
        btn.setPrefHeight(50);
        return btn;
    }

    private String getActionName(String action) {
        return switch (action) {
            case "attack" -> "通常攻撃";
            case "defend" -> "防御";
            case "tactics" -> "策略";
            case "evade" -> "回避";
            default -> action;
        };
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
