package com.kh.tbrr.ui;

import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * ゲーム中のコンフィグダイアログ
 * メインメニューに戻る、音量設定、ヘルプなどの機能を提供
 */
public class ConfigDialog {

    private Stage dialogStage;
    private Stage parentStage;
    private Runnable onReturnToMainMenu;
    private Runnable onSuspendGame; // 中断用コールバック
    private double bgmVolume = 0.8;
    private double seVolume = 0.8;

    public ConfigDialog(Stage parentStage, Runnable onReturnToMainMenu) {
        this.parentStage = parentStage;
        this.onReturnToMainMenu = onReturnToMainMenu;
    }

    public void setOnSuspendGame(Runnable onSuspendGame) {
        this.onSuspendGame = onSuspendGame;
    }

    public void show() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(parentStage);
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.setTitle("コンフィグ");
        dialogStage.setResizable(false);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab generalTab = createGeneralTab();
        Tab soundTab = createSoundTab();
        Tab helpTab = createHelpTab();

        tabPane.getTabs().addAll(generalTab, soundTab, helpTab);

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #2b2b2b;");

        Button closeButton = new Button("閉じる");
        closeButton.setPrefWidth(100);
        closeButton.setOnAction(e -> dialogStage.close());

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().add(closeButton);

        root.getChildren().addAll(tabPane, buttonBox);

        Scene scene = new Scene(root, 500, 400);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private Tab createGeneralTab() {
        Tab tab = new Tab("一般");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #3a3a3a;");

        Label titleLabel = new Label("ゲーム設定");
        titleLabel.setFont(Font.font("Arial", 18));
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Button returnToMenuButton = new Button("タイトルに戻る"); // 現在、実質的にメインメニューがタイトルである。
        returnToMenuButton.setPrefWidth(250);
        returnToMenuButton.setPrefHeight(40);
        returnToMenuButton.setFont(Font.font("Arial", 14));
        returnToMenuButton.setOnAction(e -> confirmReturnToMainMenu());

        Button exitGameButton = new Button("ゲームを終了する");
        exitGameButton.setPrefWidth(250);
        exitGameButton.setPrefHeight(40);
        exitGameButton.setFont(Font.font("Arial", 14));
        exitGameButton.setOnAction(e -> confirmExitGame());

        content.getChildren().addAll(titleLabel, returnToMenuButton, exitGameButton);

        Button suspendGameButton = new Button("ゲームを中断する（タイトルへ）");
        suspendGameButton.setPrefWidth(250);
        suspendGameButton.setPrefHeight(40);
        suspendGameButton.setFont(Font.font("Arial", 14));
        suspendGameButton.setOnAction(e -> confirmSuspendGame());

        // 中断コールバックがない場合はボタンを無効化
        if (onSuspendGame == null) {
            suspendGameButton.setDisable(true);
        }

        content.getChildren().clear();
        content.getChildren().addAll(titleLabel, returnToMenuButton, suspendGameButton, exitGameButton);

        tab.setContent(content);
        return tab;
    }

    private Tab createSoundTab() {
        Tab tab = new Tab("サウンド");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #3a3a3a;");

        Label titleLabel = new Label("音量設定");
        titleLabel.setFont(Font.font("Arial", 18));
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        VBox bgmBox = new VBox(5);
        Label bgmLabel = new Label("BGM音量: " + (int) (bgmVolume * 100) + "%");
        bgmLabel.setStyle("-fx-text-fill: white;");

        Slider bgmSlider = new Slider(0, 100, bgmVolume * 100);
        bgmSlider.setShowTickLabels(true);
        bgmSlider.setShowTickMarks(true);
        bgmSlider.setMajorTickUnit(25);
        bgmSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            bgmVolume = newVal.doubleValue() / 100.0;
            bgmLabel.setText("BGM音量: " + newVal.intValue() + "%");
        });

        bgmBox.getChildren().addAll(bgmLabel, bgmSlider);

        VBox seBox = new VBox(5);
        Label seLabel = new Label("SE音量: " + (int) (seVolume * 100) + "%");
        seLabel.setStyle("-fx-text-fill: white;");

        Slider seSlider = new Slider(0, 100, seVolume * 100);
        seSlider.setShowTickLabels(true);
        seSlider.setShowTickMarks(true);
        seSlider.setMajorTickUnit(25);
        seSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            seVolume = newVal.doubleValue() / 100.0;
            seLabel.setText("SE音量: " + newVal.intValue() + "%");
        });

        seBox.getChildren().addAll(seLabel, seSlider);

        Label notImplementedLabel = new Label("※ BGM/SE機能は未実装です");
        notImplementedLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

        content.getChildren().addAll(titleLabel, bgmBox, seBox, notImplementedLabel);

        tab.setContent(content);
        return tab;
    }

    private Tab createHelpTab() {
        Tab tab = new Tab("ヘルプ");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #3a3a3a;");

        Label titleLabel = new Label("ゲームの遊び方");
        titleLabel.setFont(Font.font("Arial", 18));
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label helpText = new Label(
                "【基本操作】\n" +
                        "・数字キーまたはテンキーで選択肢を入力\n" +
                        "・Enterキーで決定\n" +
                        "\n" +
                        "【ゲームの目的】\n" +
                        "・シナリオのクリア条件を達成する\n" +
                        "・HP(体力)が0になるとゲームオーバー\n" +
                        "・AP(行動力)を消費して行動する\n" +
                        "\n" +
                        "【アイコンについて】\n" +
                        "・アイコンにマウスを乗せると説明が表示されます\n" +
                        "・技能、アイテム、状態異常の詳細を確認できます");
        helpText.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 13px;");
        helpText.setWrapText(true);

        content.getChildren().addAll(titleLabel, helpText);

        tab.setContent(content);
        return tab;
    }

    private void confirmReturnToMainMenu() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("確認");
        confirmDialog.setHeaderText("メインメニューに戻りますか？");
        confirmDialog.setContentText("現在のゲームは破棄されます。\nメインメニューに戻ってよろしいですか？");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            dialogStage.close();
            if (onReturnToMainMenu != null) {
                onReturnToMainMenu.run();
            }
        }
    }

    private void confirmExitGame() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("確認");
        confirmDialog.setHeaderText("ゲームを終了しますか？");
        confirmDialog.setContentText("現在のゲームは破棄されます。\nゲームを終了してよろしいですか？");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            dialogStage.close();
            Platform.exit();
        }
    }

    private void confirmSuspendGame() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("確認");
        confirmDialog.setHeaderText("ゲームを中断しますか？");
        confirmDialog.setContentText("現在の状態を保存してメインメニューに戻ります。\n再開時はこの状態から始まります。");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // GameEngineからGameStateを取得する必要があるが、
                // ConfigDialogはGameEngineを知らない。
                // しかし、GameStateはシングルトンではないため、どこかから渡してもらう必要がある。
                // 設計上、ConfigDialogはUIの一部であり、ロジックへのアクセスが弱い。

                // 解決策: onReturnToMainMenuのようなコールバックを追加するか、
                // GameEngineへの参照を渡すように変更する。
                // ここでは、GameStateへのアクセス手段がないため、
                // 簡易的に「GameEngine.getInstance()」的なものがあればよいが、ない。

                // ConfigDialogのコンストラクタを変更してGameEngineを受け取るのが正しいが、
                // 呼び出し元（JavaFXUI）の変更も必要になる。

                // 一旦、JavaFXUIに「中断処理」を委譲するコールバックを追加する形にするのが安全。
                if (onSuspendGame != null) {
                    onSuspendGame.run();
                    dialogStage.close();
                } else {
                    // コールバックが設定されていない場合（既存コードとの互換性のため）
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("エラー");
                    errorAlert.setContentText("中断機能が正しく設定されていません。");
                    errorAlert.showAndWait();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("エラー");
                errorAlert.setContentText("保存に失敗しました: " + e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }

    public double getBgmVolume() {
        return bgmVolume;
    }

    public double getSeVolume() {
        return seVolume;
    }

    public void setBgmVolume(double volume) {
        this.bgmVolume = Math.max(0.0, Math.min(1.0, volume));
    }

    public void setSeVolume(double volume) {
        this.seVolume = Math.max(0.0, Math.min(1.0, volume));
    }
}