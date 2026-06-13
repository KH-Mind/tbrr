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
 * 冒険の記録 画面
 * RecordsScreenの「冒険の記録」から遷移する中間メニュー。
 * 「生還者」と「墓地」の2項目を持つ。
 */
public class AdventureRecordsScreen {

    private Stage stage;
    private ImageManager imageManager;

    public AdventureRecordsScreen(Stage stage) {
        this.stage = stage;
        this.imageManager = new ImageManager();
    }

    /**
     * 画面を表示する
     *
     * @param onBack 「戻る」ボタンを押したときの処理（RecordsScreenへ戻るコールバック）
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

        Button survivorsButton = createMenuButton("生還者");
        Button graveyardButton = createMenuButton("墓地");
        Button backButton = createMenuButton("戻る");

        // 生還者：まだ未実装なので工事中ダイアログ
        survivorsButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("生還者");
            alert.setHeaderText("生還者");
            alert.setContentText("生還者記録機能は未実装です。\n今後のアップデートで追加予定です。");
            alert.showAndWait();
        });

        // 墓地：GraveyardScreenへ
        graveyardButton.setOnAction(e -> {
            GraveyardScreen graveyardScreen = new GraveyardScreen(stage);
            graveyardScreen.show(() -> show(onBack));
        });

        // 戻る
        backButton.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });

        buttonBox.getChildren().addAll(survivorsButton, graveyardButton, backButton);

        StackPane root = new StackPane();
        root.getChildren().addAll(backgroundView, buttonBox);

        Scene scene = new Scene(root, 1600, 900);
        stage.setScene(scene);
        stage.setTitle("TBRR - 冒険の記録");
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
