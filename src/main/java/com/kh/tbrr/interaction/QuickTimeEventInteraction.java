package com.kh.tbrr.interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import com.kh.tbrr.data.models.Player;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
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
 * Quick Time Event (QTE) インタラクション
 * 制限時間内に指定された順序でコマンドを入力する
 */
public class QuickTimeEventInteraction implements InteractionHandler {

    private static final String TYPE = "quick_time_event";

    // UI参照
    private StackPane subWindowPane;

    // 入力キーの定義
    private enum KeyType {
        UP("↑"), DOWN("↓"), LEFT("←"), RIGHT("→"), ACTION("○");

        final String label;

        KeyType(String label) {
            this.label = label;
        }
    }

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

        // パラメータ設定
        int sequenceLength = 5;
        double timeLimit = 5.0; // 秒
        // 使用するキーの種類（デフォルトは全種類）

        if (params.containsKey("sequenceLength")) {
            Object val = params.get("sequenceLength");
            if (val instanceof Number) {
                sequenceLength = ((Number) val).intValue();
            }
        }
        if (params.containsKey("timeLimit")) {
            Object val = params.get("timeLimit");
            if (val instanceof Number) {
                timeLimit = ((Number) val).doubleValue();
            }
        }

        final int finalSequenceLength = sequenceLength;
        final double finalTimeLimit = timeLimit;
        final Map<String, Object> finalParams = params;

        Platform.runLater(() -> {
            showQTEUI(finalSequenceLength, finalTimeLimit, future, finalParams);
        });

        return future;
    }

    /**
     * QTE UIを表示
     */
    @SuppressWarnings("unchecked")
    private void showQTEUI(int sequenceLength, double timeLimit,
            CompletableFuture<InteractionResult> future,
            Map<String, Object> params) {
        if (subWindowPane == null) {
            future.complete(new InteractionResult("failure"));
            return;
        }

        // InteractionUIHelperでコンテナを作成
        VBox container = InteractionUIHelper.createContainer(params, subWindowPane, 20);
        container.setAlignment(Pos.CENTER);

        // タイトル
        Label titleLabel = new Label("コマンドを入力せよ！");
        titleLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);

        // シーケンス生成
        List<KeyType> sequence = generateSequence(sequenceLength);

        // シーケンス表示用コンテナ
        HBox sequenceBox = new HBox(15);
        sequenceBox.setAlignment(Pos.CENTER);

        List<Label> keyLabels = new ArrayList<>();
        for (KeyType key : sequence) {
            Label l = new Label(key.label);
            l.setFont(Font.font("Meiryo", FontWeight.BOLD, 32));
            l.setTextFill(Color.GRAY); // 未入力状態
            l.setStyle(
                    "-fx-border-color: gray; -fx-border-width: 2px; -fx-padding: 5px 15px; -fx-background-radius: 5; -fx-border-radius: 5;");
            l.setMinWidth(60);
            l.setAlignment(Pos.CENTER);
            keyLabels.add(l);
            sequenceBox.getChildren().add(l);
        }

        // 最初のキーをハイライト
        highlightKey(keyLabels.get(0), true);

        // タイマーバー
        ProgressBar timerBar = new ProgressBar(1.0);
        timerBar.setPrefWidth(400);
        timerBar.setStyle("-fx-accent: limegreen;");

        Label timerLabel = new Label(String.format("%.1f", timeLimit));
        timerLabel.setFont(Font.font("Meiryo", 20));
        timerLabel.setTextFill(Color.WHITE);

        container.getChildren().addAll(titleLabel, sequenceBox, timerBar, timerLabel);

        // ゲーム状態
        final int[] currentIndex = { 0 };
        final boolean[] gameOver = { false };
        final double[] timeRemaining = { timeLimit };

        // タイマー処理
        Timeline timer = new Timeline();
        timer.setCycleCount(Timeline.INDEFINITE);
        // フレームレート考慮して更新（約60fps）
        double tick = 0.016;
        timer.getKeyFrames().add(new KeyFrame(Duration.seconds(tick), e -> {
            if (gameOver[0])
                return;

            timeRemaining[0] -= tick;
            if (timeRemaining[0] <= 0) {
                timeRemaining[0] = 0;
                gameOver[0] = true;
                timer.stop();
                showResult(container, false, future);
            }

            // 表示更新
            timerLabel.setText(String.format("%.1f", timeRemaining[0]));
            double progress = timeRemaining[0] / timeLimit;
            timerBar.setProgress(progress);

            // 色変更
            if (progress < 0.3) {
                timerBar.setStyle("-fx-accent: red;");
            } else if (progress < 0.6) {
                timerBar.setStyle("-fx-accent: gold;");
            }
        }));
        timer.play();

        // 入力を受け取るメソッド
        java.util.function.Consumer<KeyType> inputProcessor = (inputKey) -> {
            if (gameOver[0])
                return;

            KeyType targetKey = sequence.get(currentIndex[0]);

            if (inputKey == targetKey) {
                // 正解
                Label currentLabel = keyLabels.get(currentIndex[0]);
                highlightKey(currentLabel, false); // ハイライト解除（完了色へ）
                currentLabel.setTextFill(Color.LIMEGREEN);
                currentLabel.setStyle(
                        "-fx-border-color: limegreen; -fx-border-width: 2px; -fx-padding: 5px 15px; -fx-background-radius: 5; -fx-border-radius: 5;");

                currentIndex[0]++;

                if (currentIndex[0] >= sequenceLength) {
                    // 全クリア
                    gameOver[0] = true;
                    timer.stop();
                    showResult(container, true, future);
                } else {
                    // 次のキーをハイライト
                    highlightKey(keyLabels.get(currentIndex[0]), true);
                }
            } else {
                // 不正解 - 今回はシンプルに無視
            }
        };

        // キーボードイベント
        container.setFocusTraversable(true);
        container.requestFocus();
        container.setOnKeyPressed(e -> {
            KeyType input = null;
            switch (e.getCode()) {
                case UP:
                case NUMPAD8:
                    input = KeyType.UP;
                    break;
                case DOWN:
                case NUMPAD2:
                    input = KeyType.DOWN;
                    break;
                case LEFT:
                case NUMPAD4:
                    input = KeyType.LEFT;
                    break;
                case RIGHT:
                case NUMPAD6:
                    input = KeyType.RIGHT;
                    break;
                case ENTER:
                case SPACE:
                case Z:
                case NUMPAD5:
                    input = KeyType.ACTION;
                    break;
                default:
                    break;
            }
            if (input != null) {
                final KeyType finalInput = input;
                Platform.runLater(() -> inputProcessor.accept(finalInput));
            }
        });

        // ○ボタン用入力コールバックを設定
        if (params != null && params.containsKey("_inputCallback")) {
            java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>> callbackRef = (java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>>) params
                    .get("_inputCallback");
            callbackRef.set(inputStr -> {
                KeyType input = null;
                switch (inputStr) {
                    case "UP":
                        input = KeyType.UP;
                        break;
                    case "DOWN":
                        input = KeyType.DOWN;
                        break;
                    case "LEFT":
                        input = KeyType.LEFT;
                        break;
                    case "RIGHT":
                        input = KeyType.RIGHT;
                        break;
                    case "ACTION":
                        input = KeyType.ACTION;
                        break;
                }
                if (input != null) {
                    final KeyType finalInput = input;
                    Platform.runLater(() -> inputProcessor.accept(finalInput));
                }
            });
        }
    }

    // 現在入力対象のキーを強調表示
    private void highlightKey(Label label, boolean active) {
        if (active) {
            label.setTextFill(Color.WHITE);
            label.setStyle(
                    "-fx-border-color: white; -fx-border-width: 3px; -fx-padding: 5px 15px; -fx-background-radius: 5; -fx-border-radius: 5; -fx-effect: dropshadow(gaussian, cyan, 10, 0.5, 0, 0);");
        } else {
            // 完了時などは呼び出し元でスタイル上書きされる
        }
    }

    private List<KeyType> generateSequence(int length) {
        List<KeyType> seq = new ArrayList<>();
        Random rand = new Random();
        KeyType[] values = KeyType.values();

        for (int i = 0; i < length; i++) {
            seq.add(values[rand.nextInt(values.length)]);
        }
        return seq;
    }

    /**
     * 結果を表示
     */
    private void showResult(VBox container, boolean success,
            CompletableFuture<InteractionResult> future) {
        // 入力を無効化
        container.setOnKeyPressed(null);
        container.setOnMouseClicked(null);

        // 結果表示
        Label resultLabel = new Label(success ? "成功！" : "失敗...");
        resultLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 48));
        resultLabel.setTextFill(success ? Color.LIMEGREEN : Color.TOMATO);
        resultLabel.setStyle("-fx-effect: dropshadow(gaussian, black, 5, 0.5, 2, 2);");

        container.getChildren().add(resultLabel);

        // 少し待ってから結果を返す
        String resultKey = success ? "success" : "failure";
        Timeline delay = new Timeline(new KeyFrame(Duration.millis(1500), ev -> {
            future.complete(new InteractionResult(resultKey));
        }));
        delay.play();
    }
}
