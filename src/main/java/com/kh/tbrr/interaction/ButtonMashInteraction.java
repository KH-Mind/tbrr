package com.kh.tbrr.interaction;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.kh.tbrr.data.models.Player;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * ボタン連打インタラクション
 * 制限時間内にボタンを連打し、目標回数以上で成功
 */
public class ButtonMashInteraction implements InteractionHandler {

    private static final String TYPE = "button_mash";

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

        // パラメータから設定を取得
        int targetCount = 20; // デフォルト目標回数
        int timeLimit = 5; // デフォルト制限時間（秒）

        if (params.containsKey("targetCount")) {
            Object val = params.get("targetCount");
            if (val instanceof Number) {
                targetCount = ((Number) val).intValue();
            }
        }
        if (params.containsKey("timeLimit")) {
            Object val = params.get("timeLimit");
            if (val instanceof Number) {
                timeLimit = ((Number) val).intValue();
            }
        }

        final int finalTargetCount = targetCount;
        final int finalTimeLimit = timeLimit;
        final Map<String, Object> finalParams = params;

        Platform.runLater(() -> {
            showButtonMashUI(finalTargetCount, finalTimeLimit, future, finalParams);
        });

        return future;
    }

    /**
     * ボタン連打UIを表示
     */
    @SuppressWarnings("unchecked")
    private void showButtonMashUI(int targetCount, int timeLimit,
            CompletableFuture<InteractionResult> future,
            Map<String, Object> params) {
        if (subWindowPane == null) {
            // UIが設定されていない場合は失敗
            future.complete(new InteractionResult("failure"));
            return;
        }

        // InteractionUIHelperでコンテナを作成（背景画像対応）
        VBox container = InteractionUIHelper.createContainer(params, subWindowPane, 15);

        // タイトル
        Label titleLabel = new Label("連打対決！");
        titleLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);

        // 目標表示
        Label targetLabel = new Label("目標: " + targetCount + " 回");
        targetLabel.setFont(Font.font("Meiryo", 18));
        targetLabel.setTextFill(Color.LIGHTGRAY);

        // カウント表示
        Label countLabel = new Label("0");
        countLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 72));
        countLabel.setTextFill(Color.GOLD);

        // 残り時間
        Label timerLabel = new Label("残り " + timeLimit + " 秒");
        timerLabel.setFont(Font.font("Meiryo", 20));
        timerLabel.setTextFill(Color.LIGHTCORAL);

        // プログレスバー
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setStyle("-fx-accent: gold;");

        // 説明
        Label instructionLabel = new Label("○ボタン/Enter/クリックで連打！");
        instructionLabel.setFont(Font.font("Meiryo", 16));
        instructionLabel.setTextFill(Color.LIGHTGRAY);

        container.getChildren().addAll(titleLabel, targetLabel, countLabel,
                timerLabel, progressBar, instructionLabel);

        // ゲーム状態
        final int[] count = { 0 };
        final boolean[] gameOver = { false };
        final boolean[] gameStarted = { false }; // スタート前フラグ
        final int[] timeRemaining = { timeLimit };

        // 連打処理
        Runnable mashAction = () -> {
            if (!gameStarted[0] || gameOver[0]) // スタート前または終了後は無視
                return;
            count[0]++;
            countLabel.setText(String.valueOf(count[0]));
            double progress = Math.min(1.0, (double) count[0] / targetCount);
            progressBar.setProgress(progress);

            // 目標達成で色変更
            if (count[0] >= targetCount) {
                countLabel.setTextFill(Color.LIMEGREEN);
            }
        };

        // クリックイベント
        container.setOnMouseClicked(e -> {
            Platform.runLater(mashAction);
        });

        // キーボードイベント
        container.setFocusTraversable(true);
        container.requestFocus();
        container.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER ||
                    e.getCode() == javafx.scene.input.KeyCode.SPACE ||
                    e.getCode() == javafx.scene.input.KeyCode.NUMPAD5) {
                Platform.runLater(mashAction);
            }
        });

        // ○ボタン用入力コールバックを設定
        if (params != null && params.containsKey("_inputCallback")) {
            java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>> callbackRef = (java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>>) params
                    .get("_inputCallback");
            callbackRef.set(input -> {
                if ("ACTION".equals(input)) {
                    Platform.runLater(mashAction);
                }
            });
        }

        // タイマー
        Timeline timer = new Timeline();
        timer.setCycleCount(timeLimit);
        timer.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> {
            timeRemaining[0]--;
            timerLabel.setText("残り " + timeRemaining[0] + " 秒");

            // 残り時間が少なくなったら色変更
            if (timeRemaining[0] <= 2) {
                timerLabel.setTextFill(Color.RED);
            }
        }));

        timer.setOnFinished(e -> {
            gameOver[0] = true;
            container.setOnMouseClicked(null);
            container.setOnKeyPressed(null);

            // 結果判定
            boolean success = count[0] >= targetCount;
            String resultKey = success ? "success" : "failure";

            // 結果表示
            Label resultLabel = new Label(success ? "成功！" : "失敗...");
            resultLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 36));
            resultLabel.setTextFill(success ? Color.LIMEGREEN : Color.TOMATO);

            instructionLabel.setText(count[0] + " 回連打！");
            container.getChildren().add(resultLabel);

            // 少し待ってから結果を返す
            Timeline delay = new Timeline(new KeyFrame(Duration.millis(1500), ev -> {
                future.complete(new InteractionResult(resultKey));
            }));
            delay.play();
        });

        // ゲーム開始（1秒後）
        Timeline startDelay = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            gameStarted[0] = true; // ここからカウント開始
            instructionLabel.setText("スタート！");
            timer.play();
        }));
        startDelay.play();
    }
}
