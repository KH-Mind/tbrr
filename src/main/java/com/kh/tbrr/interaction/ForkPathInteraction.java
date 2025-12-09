package com.kh.tbrr.interaction;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.kh.tbrr.data.models.Player;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 分かれ道インタラクション
 * 左右の矢印をクリックまたは矢印キーで選択
 */
public class ForkPathInteraction implements InteractionHandler {

    private static final String TYPE = "fork_path";

    // UI参照
    private StackPane subWindowPane;

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
        String leftLabel = "左";
        String rightLabel = "右";
        String leftResult = "left";
        String rightResult = "right";

        if (params.containsKey("leftLabel")) {
            leftLabel = String.valueOf(params.get("leftLabel"));
        }
        if (params.containsKey("rightLabel")) {
            rightLabel = String.valueOf(params.get("rightLabel"));
        }
        if (params.containsKey("leftResult")) {
            leftResult = String.valueOf(params.get("leftResult"));
        }
        if (params.containsKey("rightResult")) {
            rightResult = String.valueOf(params.get("rightResult"));
        }

        final String finalLeftLabel = leftLabel;
        final String finalRightLabel = rightLabel;
        final String finalLeftResult = leftResult;
        final String finalRightResult = rightResult;
        final Map<String, Object> finalParams = params;

        Platform.runLater(() -> {
            showForkPathUI(finalLeftLabel, finalRightLabel, finalLeftResult, finalRightResult, future, finalParams);
        });

        return future;
    }

    /**
     * 分かれ道UIを表示
     */
    @SuppressWarnings("unchecked")
    private void showForkPathUI(String leftLabelText, String rightLabelText,
            String leftResult, String rightResult,
            CompletableFuture<InteractionResult> future,
            Map<String, Object> params) {
        if (subWindowPane == null) {
            future.complete(new InteractionResult("failure"));
            return;
        }

        // InteractionUIHelperでコンテナを作成（背景画像対応）
        VBox container = InteractionUIHelper.createContainer(params, subWindowPane, 0);
        container.setAlignment(Pos.CENTER);

        // メインレイアウト（BorderPane）
        BorderPane layout = new BorderPane();
        layout.setPrefSize(subWindowPane.getWidth() > 0 ? subWindowPane.getWidth() : 400,
                subWindowPane.getHeight() > 0 ? subWindowPane.getHeight() : 300);

        // 選択状態
        final boolean[] selected = { false };

        // 選択処理
        Runnable selectLeft = () -> {
            if (selected[0])
                return;
            selected[0] = true;
            showSelectionResult(container, leftLabelText, future, leftResult);
        };

        Runnable selectRight = () -> {
            if (selected[0])
                return;
            selected[0] = true;
            showSelectionResult(container, rightLabelText, future, rightResult);
        };

        // 左矢印
        Label leftArrow = createArrowLabel("←", leftLabelText, selectLeft);
        BorderPane.setAlignment(leftArrow, Pos.BOTTOM_LEFT);

        // 右矢印
        Label rightArrow = createArrowLabel("→", rightLabelText, selectRight);
        BorderPane.setAlignment(rightArrow, Pos.BOTTOM_RIGHT);

        // 配置
        VBox leftBox = new VBox(5, leftArrow);
        leftBox.setAlignment(Pos.BOTTOM_LEFT);
        leftBox.setPadding(new javafx.geometry.Insets(0, 0, 20, 20));

        VBox rightBox = new VBox(5, rightArrow);
        rightBox.setAlignment(Pos.BOTTOM_RIGHT);
        rightBox.setPadding(new javafx.geometry.Insets(0, 20, 20, 0));

        layout.setLeft(leftBox);
        layout.setRight(rightBox);

        // 上部にタイトル/説明
        Label titleLabel = new Label("どちらの道を選ぶ？");
        titleLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle("-fx-effect: dropshadow(gaussian, black, 3, 0.5, 1, 1);");

        VBox topBox = new VBox(titleLabel);
        topBox.setAlignment(Pos.TOP_CENTER);
        topBox.setPadding(new javafx.geometry.Insets(20, 0, 0, 0));
        layout.setTop(topBox);

        container.getChildren().add(layout);

        // キーボードイベント
        container.setFocusTraversable(true);
        container.requestFocus();
        container.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case LEFT:
                case NUMPAD4:
                    Platform.runLater(selectLeft);
                    break;
                case RIGHT:
                case NUMPAD6:
                    Platform.runLater(selectRight);
                    break;
                default:
                    break;
            }
        });

        // ○ボタン用入力コールバックを設定（矢印ボタン対応）
        if (params != null && params.containsKey("_inputCallback")) {
            java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>> callbackRef = (java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>>) params
                    .get("_inputCallback");
            callbackRef.set(input -> {
                switch (input) {
                    case "LEFT":
                        Platform.runLater(selectLeft);
                        break;
                    case "RIGHT":
                        Platform.runLater(selectRight);
                        break;
                    default:
                        break;
                }
            });
        }
    }

    /**
     * 矢印ラベルを作成
     */
    private Label createArrowLabel(String arrow, String labelText, Runnable onSelect) {
        Label arrowLabel = new Label(arrow + "\n" + labelText);
        arrowLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 36));
        arrowLabel.setTextFill(Color.WHITE);
        arrowLabel.setAlignment(Pos.CENTER);
        arrowLabel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.5);" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 15 25;" +
                        "-fx-effect: dropshadow(gaussian, black, 5, 0.3, 2, 2);");
        arrowLabel.setCursor(Cursor.HAND);

        // ホバー効果
        arrowLabel.setOnMouseEntered(e -> {
            arrowLabel.setStyle(
                    "-fx-background-color: rgba(100, 150, 255, 0.7);" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 15 25;" +
                            "-fx-effect: dropshadow(gaussian, cyan, 10, 0.5, 0, 0);");
            arrowLabel.setTextFill(Color.YELLOW);
        });

        arrowLabel.setOnMouseExited(e -> {
            arrowLabel.setStyle(
                    "-fx-background-color: rgba(0, 0, 0, 0.5);" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 15 25;" +
                            "-fx-effect: dropshadow(gaussian, black, 5, 0.3, 2, 2);");
            arrowLabel.setTextFill(Color.WHITE);
        });

        // クリックイベント
        arrowLabel.setOnMouseClicked(e -> onSelect.run());

        return arrowLabel;
    }

    /**
     * 選択結果を表示
     */
    private void showSelectionResult(VBox container, String selectedLabel,
            CompletableFuture<InteractionResult> future, String resultKey) {
        // 入力を無効化
        container.setOnMouseClicked(null);
        container.setOnKeyPressed(null);

        // 結果表示
        Label resultLabel = new Label("「" + selectedLabel + "」を選択！");
        resultLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 28));
        resultLabel.setTextFill(Color.GOLD);
        resultLabel.setStyle("-fx-effect: dropshadow(gaussian, black, 5, 0.5, 2, 2);");

        VBox resultBox = new VBox(resultLabel);
        resultBox.setAlignment(Pos.CENTER);
        container.getChildren().clear();
        container.getChildren().add(resultBox);
        container.setAlignment(Pos.CENTER);

        // 少し待ってから結果を返す
        Timeline delay = new Timeline(new KeyFrame(javafx.util.Duration.millis(1000), ev -> {
            future.complete(new InteractionResult(resultKey));
        }));
        delay.play();
    }
}
