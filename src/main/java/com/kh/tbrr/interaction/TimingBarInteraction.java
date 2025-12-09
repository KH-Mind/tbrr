package com.kh.tbrr.interaction;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.kh.tbrr.data.models.Player;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * タイミングバーインタラクション
 * 動くバーを止めて成功ゾーンに入れるミニゲーム
 */
public class TimingBarInteraction implements InteractionHandler {

    private static final String TYPE = "timing_bar";

    // UI参照
    private StackPane subWindowPane;
    private Timeline barAnimation;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<InteractionResult> execute(Map<String, Object> params, Player player) {
        CompletableFuture<InteractionResult> future = new CompletableFuture<>();

        // UI参照を取得
        if (params.containsKey("_subWindowPane")) {
            subWindowPane = (StackPane) params.get("_subWindowPane");
        }

        // パラメータから設定を取得
        double successZoneStart = 0.4; // 成功ゾーン開始位置（0.0-1.0）
        double successZoneEnd = 0.6; // 成功ゾーン終了位置（0.0-1.0）
        double barSpeed = 2.0; // バーの速度（往復/秒）
        int attempts = 1; // 試行回数

        if (params.containsKey("successZoneStart")) {
            Object val = params.get("successZoneStart");
            if (val instanceof Number) {
                successZoneStart = ((Number) val).doubleValue();
            }
        }
        if (params.containsKey("successZoneEnd")) {
            Object val = params.get("successZoneEnd");
            if (val instanceof Number) {
                successZoneEnd = ((Number) val).doubleValue();
            }
        }
        if (params.containsKey("barSpeed")) {
            Object val = params.get("barSpeed");
            if (val instanceof Number) {
                barSpeed = ((Number) val).doubleValue();
            }
        }
        if (params.containsKey("attempts")) {
            Object val = params.get("attempts");
            if (val instanceof Number) {
                attempts = ((Number) val).intValue();
            }
        }

        final double finalSuccessZoneStart = successZoneStart;
        final double finalSuccessZoneEnd = successZoneEnd;
        final double finalBarSpeed = barSpeed;
        final int finalAttempts = attempts;
        final Map<String, Object> finalParams = params;

        Platform.runLater(() -> {
            showTimingBarUI(finalSuccessZoneStart, finalSuccessZoneEnd, finalBarSpeed,
                    finalAttempts, future, finalParams);
        });

        return future;
    }

    /**
     * タイミングバーUIを表示
     */
    @SuppressWarnings("unchecked")
    private void showTimingBarUI(double successZoneStart, double successZoneEnd,
            double barSpeed, int attempts,
            CompletableFuture<InteractionResult> future,
            Map<String, Object> params) {
        if (subWindowPane == null) {
            future.complete(new InteractionResult("failure"));
            return;
        }

        // 前回のアニメーションを停止
        if (barAnimation != null) {
            barAnimation.stop();
        }

        // InteractionUIHelperでコンテナを作成（背景画像対応）
        VBox container = InteractionUIHelper.createContainer(params, subWindowPane, 20);
        container.setAlignment(Pos.CENTER);

        // タイトル
        Label titleLabel = new Label("タイミングを合わせろ！");
        titleLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);

        // 残り試行回数
        Label attemptsLabel = new Label("残り " + attempts + " 回");
        attemptsLabel.setFont(Font.font("Meiryo", 18));
        attemptsLabel.setTextFill(Color.LIGHTGRAY);

        // バーの寸法
        double barWidth = 350;
        double barHeight = 40;

        // 背景バー（赤ゾーン）
        Rectangle backgroundBar = new Rectangle(barWidth, barHeight);
        backgroundBar.setFill(Color.DARKRED);
        backgroundBar.setStroke(Color.BLACK);
        backgroundBar.setStrokeWidth(2);
        backgroundBar.setArcWidth(5);
        backgroundBar.setArcHeight(5);

        // 成功ゾーン（黄色/緑）
        double successWidth = (successZoneEnd - successZoneStart) * barWidth;
        double successX = successZoneStart * barWidth;
        Rectangle successZone = new Rectangle(successWidth, barHeight);
        successZone.setFill(Color.GOLD);
        successZone.setTranslateX(successX - barWidth / 2 + successWidth / 2);

        // 動くバー（インジケーター）
        Rectangle indicator = new Rectangle(6, barHeight + 10);
        indicator.setFill(Color.WHITE);
        indicator.setStroke(Color.BLACK);
        indicator.setStrokeWidth(1);

        // バーを重ねるStackPane
        StackPane barPane = new StackPane();
        barPane.getChildren().addAll(backgroundBar, successZone, indicator);
        barPane.setMaxWidth(barWidth);

        // 説明
        Label instructionLabel = new Label("○ボタン/Enter/クリックで止める！");
        instructionLabel.setFont(Font.font("Meiryo", 16));
        instructionLabel.setTextFill(Color.LIGHTGRAY);

        container.getChildren().addAll(titleLabel, attemptsLabel, barPane, instructionLabel);

        // ゲーム状態
        final int[] remainingAttempts = { attempts };
        final boolean[] stopped = { false };
        final double[] position = { 0.0 }; // 0.0-1.0
        final int[] direction = { 1 }; // 1=右, -1=左

        // バーアニメーション
        double animationInterval = 16; // 約60fps
        double movePerFrame = (barSpeed * 2) / (1000.0 / animationInterval);

        barAnimation = new Timeline();
        barAnimation.setCycleCount(Animation.INDEFINITE);
        barAnimation.getKeyFrames().add(new KeyFrame(Duration.millis(animationInterval), e -> {
            if (stopped[0])
                return;

            // 位置を更新
            position[0] += movePerFrame * direction[0];

            // 端で反転
            if (position[0] >= 1.0) {
                position[0] = 1.0;
                direction[0] = -1;
            } else if (position[0] <= 0.0) {
                position[0] = 0.0;
                direction[0] = 1;
            }

            // インジケーターの位置を更新
            double indicatorX = (position[0] - 0.5) * barWidth;
            indicator.setTranslateX(indicatorX);
        }));
        barAnimation.play();

        // 停止処理
        Runnable stopAction = () -> {
            if (stopped[0])
                return;
            stopped[0] = true;
            barAnimation.pause();

            // 判定
            boolean inZone = position[0] >= successZoneStart && position[0] <= successZoneEnd;

            if (inZone) {
                // 成功
                indicator.setFill(Color.LIMEGREEN);
                showResult(container, true, future);
            } else {
                // 失敗
                remainingAttempts[0]--;
                indicator.setFill(Color.TOMATO);

                if (remainingAttempts[0] > 0) {
                    // 再試行
                    attemptsLabel.setText("残り " + remainingAttempts[0] + " 回");
                    instructionLabel.setText("もう一度！");

                    Timeline retryDelay = new Timeline(new KeyFrame(Duration.millis(800), ev -> {
                        stopped[0] = false;
                        indicator.setFill(Color.WHITE);
                        barAnimation.play();
                    }));
                    retryDelay.play();
                } else {
                    // 最終失敗
                    showResult(container, false, future);
                }
            }
        };

        // クリックイベント
        container.setOnMouseClicked(e -> Platform.runLater(stopAction));

        // キーボードイベント
        container.setFocusTraversable(true);
        container.requestFocus();
        container.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER ||
                    e.getCode() == javafx.scene.input.KeyCode.SPACE ||
                    e.getCode() == javafx.scene.input.KeyCode.NUMPAD5) {
                Platform.runLater(stopAction);
            }
        });

        // ○ボタン用入力コールバックを設定
        if (params != null && params.containsKey("_inputCallback")) {
            java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>> callbackRef = (java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>>) params
                    .get("_inputCallback");
            callbackRef.set(input -> {
                if ("ACTION".equals(input)) {
                    Platform.runLater(stopAction);
                }
            });
        }
    }

    /**
     * 結果を表示
     */
    private void showResult(VBox container, boolean success,
            CompletableFuture<InteractionResult> future) {
        // 入力を無効化
        container.setOnMouseClicked(null);
        container.setOnKeyPressed(null);

        // アニメーションを停止
        if (barAnimation != null) {
            barAnimation.stop();
        }

        // 結果表示
        Label resultLabel = new Label(success ? "成功！" : "失敗...");
        resultLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 36));
        resultLabel.setTextFill(success ? Color.LIMEGREEN : Color.TOMATO);

        container.getChildren().add(resultLabel);

        // 少し待ってから結果を返す
        String resultKey = success ? "success" : "failure";
        Timeline delay = new Timeline(new KeyFrame(Duration.millis(1200), ev -> {
            future.complete(new InteractionResult(resultKey));
        }));
        delay.play();
    }
}
