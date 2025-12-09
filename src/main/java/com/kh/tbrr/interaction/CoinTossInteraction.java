package com.kh.tbrr.interaction;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.kh.tbrr.data.models.Player;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * コイントスインタラクション
 * サブウィンドウにコインを表示し、クリックまたは○ボタンで投げる
 */
public class CoinTossInteraction implements InteractionHandler {

    private static final String TYPE = "coin_toss";
    private final Random random = new Random();

    // UI参照（初期化時に設定）
    private StackPane subWindowPane;
    private java.util.function.Consumer<String> inputHandler;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public CompletableFuture<InteractionResult> execute(Map<String, Object> params, Player player) {
        CompletableFuture<InteractionResult> future = new CompletableFuture<>();

        // UI参照を取得（JavaFXUIから設定される）
        if (params.containsKey("_subWindowPane")) {
            subWindowPane = (StackPane) params.get("_subWindowPane");
        }
        if (params.containsKey("_inputHandler")) {
            inputHandler = (java.util.function.Consumer<String>) params.get("_inputHandler");
        }

        // パラメータから成功確率を取得（デフォルト50%）
        double successChance = 0.5;
        if (params.containsKey("successChance")) {
            Object chance = params.get("successChance");
            if (chance instanceof Number) {
                successChance = ((Number) chance).doubleValue();
            }
        }

        final double finalSuccessChance = successChance;
        final Map<String, Object> finalParams = params;

        Platform.runLater(() -> {
            showCoinTossUI(finalSuccessChance, future, finalParams);
        });

        return future;
    }

    /**
     * コイントスUIを表示
     */
    @SuppressWarnings("unchecked")
    private void showCoinTossUI(double successChance, CompletableFuture<InteractionResult> future,
            Map<String, Object> params) {
        if (subWindowPane == null) {
            // UIが設定されていない場合は直接判定
            boolean success = random.nextDouble() < successChance;
            future.complete(new InteractionResult(success ? "success" : "failure"));
            return;
        }

        // InteractionUIHelperでコンテナを作成（背景画像対応）
        VBox container = InteractionUIHelper.createContainer(params, subWindowPane, 20);

        // タイトル
        Label titleLabel = new Label("コイントス");
        titleLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);

        // コイン（円で表現）
        StackPane coinPane = new StackPane();
        Circle coin = new Circle(80);
        coin.setFill(Color.GOLD);
        coin.setStroke(Color.DARKGOLDENROD);
        coin.setStrokeWidth(4);

        Label coinLabel = new Label("?");
        coinLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 48));
        coinLabel.setTextFill(Color.DARKGOLDENROD);

        coinPane.getChildren().addAll(coin, coinLabel);

        // 説明
        Label instructionLabel = new Label("○ボタン/Enter/クリックでコインを投げる");
        instructionLabel.setFont(Font.font("Meiryo", 16));
        instructionLabel.setTextFill(Color.LIGHTGRAY);

        container.getChildren().addAll(titleLabel, coinPane, instructionLabel);

        // アクション実行フラグ（二重実行防止）
        final boolean[] actionExecuted = { false };

        // コイントス実行のランナブル
        Runnable flipAction = () -> {
            if (actionExecuted[0])
                return;
            actionExecuted[0] = true;
            container.setOnMouseClicked(null);
            container.setOnKeyPressed(null);
            flipCoin(coinPane, coin, coinLabel, successChance, future, container);
        };

        // クリックイベント
        container.setOnMouseClicked(e -> flipAction.run());

        // キーボードイベント（EnterまたはSpace）
        container.setFocusTraversable(true);
        container.requestFocus();
        container.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER ||
                    e.getCode() == javafx.scene.input.KeyCode.SPACE ||
                    e.getCode() == javafx.scene.input.KeyCode.NUMPAD5) {
                flipAction.run();
            }
        });

        // ○ボタン用入力コールバックを設定（EventProcessorから渡される）
        if (params != null && params.containsKey("_inputCallback")) {
            java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>> callbackRef = (java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>>) params
                    .get("_inputCallback");
            callbackRef.set(input -> {
                if ("ACTION".equals(input)) {
                    Platform.runLater(flipAction);
                }
            });
        }
    }

    /**
     * コインを投げるアニメーション
     */
    private void flipCoin(StackPane coinPane, Circle coin, Label coinLabel,
            double successChance, CompletableFuture<InteractionResult> future,
            VBox container) {
        // 結果を決定
        boolean success = random.nextDouble() < successChance;
        String resultKey = success ? "success" : "failure";
        String resultText = success ? "表" : "裏";
        Color resultColor = success ? Color.LIMEGREEN : Color.TOMATO;

        // アニメーション（スケールでひっくり返る効果を表現）
        Timeline timeline = new Timeline();

        // 縮小
        KeyFrame shrink = new KeyFrame(Duration.millis(200),
                new KeyValue(coinPane.scaleXProperty(), 0.1),
                new KeyValue(coinPane.scaleYProperty(), 1.0));

        // 拡大（結果表示）
        KeyFrame expand = new KeyFrame(Duration.millis(400),
                e -> {
                    coinLabel.setText(resultText);
                    coin.setFill(resultColor);
                },
                new KeyValue(coinPane.scaleXProperty(), 1.0),
                new KeyValue(coinPane.scaleYProperty(), 1.0));

        timeline.getKeyFrames().addAll(shrink, expand);

        // アニメーション完了後
        timeline.setOnFinished(e -> {
            // 結果表示を追加
            Label resultLabel = new Label(success ? "成功！" : "失敗...");
            resultLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 32));
            resultLabel.setTextFill(resultColor);
            container.getChildren().add(resultLabel);

            // 少し待ってから結果を返す
            Timeline delay = new Timeline(new KeyFrame(Duration.millis(1500), ev -> {
                future.complete(new InteractionResult(resultKey));
            }));
            delay.play();
        });

        timeline.play();
    }

    /**
     * サブウィンドウを設定（JavaFXUIから呼び出し）
     */
    public void setSubWindowPane(StackPane pane) {
        this.subWindowPane = pane;
    }
}
