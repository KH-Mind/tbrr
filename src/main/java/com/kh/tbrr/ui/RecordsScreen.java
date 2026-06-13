package com.kh.tbrr.ui;

import com.kh.tbrr.manager.ImageManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * 実績・冒険の記録 画面
 * メインメニューの「実績・冒険の記録」ボタンから遷移する中間メニュー。
 * 「実績」と「冒険の記録」の2項目を持つ。
 */
public class RecordsScreen {

    private Stage stage;
    private ImageManager imageManager;

    public RecordsScreen(Stage stage) {
        this.stage = stage;
        this.imageManager = new ImageManager();
    }

    /**
     * 画面を表示する
     *
     * @param onBack 「戻る」ボタンを押したときの処理（メインメニューへ戻るコールバック）
     */
    public void show(Runnable onBack) {
        // 背景画像
        Image backgroundImage = imageManager.loadBackground("mainmenu.png");
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setFitWidth(1600);
        backgroundView.setFitHeight(900);
        backgroundView.setPreserveRatio(false);

        // ボタン配置VBox
        VBox buttonBox = new VBox(20);
        buttonBox.setPadding(new Insets(50));
        buttonBox.setAlignment(Pos.BOTTOM_RIGHT);

        Button achievementsButton = createMenuButton("実績");
        Button adventureButton = createMenuButton("冒険の記録");
        Button backButton = createMenuButton("戻る");

        // 実績：まだ未実装なので工事中ダイアログ
        achievementsButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("実績");
            alert.setHeaderText("実績");
            alert.setContentText("実績機能は未実装です。\n今後のアップデートで追加予定です。");
            alert.showAndWait();
        });

        // 冒険の記録：次の中間メニューへ
        adventureButton.setOnAction(e -> {
            AdventureRecordsScreen adventureScreen = new AdventureRecordsScreen(stage);
            adventureScreen.show(() -> show(onBack));
        });

        // 戻る
        backButton.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });

        buttonBox.getChildren().addAll(achievementsButton, adventureButton, backButton);

        StackPane root = new StackPane();
        root.getChildren().addAll(backgroundView, buttonBox);

        Scene scene = new Scene(root, 1600, 900);
        stage.setScene(scene);
        stage.setTitle("TBRR - 実績・冒険の記録");
        stage.show();
    }

    /**
     * メニューボタンを作成（MainMenuScreenと同じスタイル）
     */
    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(350);
        button.setPrefHeight(55);
        button.setFont(Font.font("Arial", 18));
        button.setStyle(
                "-fx-background-color: #444444; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #666666; " +
                        "-fx-border-width: 2px;");

        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: #555555; " +
                            "-fx-text-fill: white; " +
                            "-fx-border-color: #888888; " +
                            "-fx-border-width: 2px;");
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: #444444; " +
                            "-fx-text-fill: white; " +
                            "-fx-border-color: #666666; " +
                            "-fx-border-width: 2px;");
        });

        return button;
    }
}
