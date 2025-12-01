package com.kh.tbrr.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * タイトル画面（最初の画面）
 * STARTボタンのみを表示する
 */
public class TitleScreen {
    
    private Stage stage;
    
    public TitleScreen(Stage stage) {
        this.stage = stage;
    }
    
    /**
     * タイトル画面を表示
     */
    public void show() {
        VBox root = new VBox(30);
        root.setPadding(new Insets(50));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #333132;");
        
        // タイトル
        Label titleLabel = new Label("TBRR");
        titleLabel.setFont(Font.font("Arial", 64));
        titleLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold;");
        
        Label subtitleLabel = new Label("Text-Based Roguelite RPG");
        subtitleLabel.setFont(Font.font("Arial", 24));
        subtitleLabel.setStyle("-fx-text-fill: #aaaaaa;");
        
        // バージョン情報
        Label versionLabel = new Label("GUI Version 0.2");
        versionLabel.setFont(Font.font("Arial", 14));
        versionLabel.setStyle("-fx-text-fill: #666666;");
        
        // スペーサー
        Region spacer = new Region();
        spacer.setPrefHeight(80);
        
        // STARTボタン
        Button startButton = new Button("START");
        startButton.setPrefWidth(300);
        startButton.setPrefHeight(60);
        startButton.setFont(Font.font("Arial", 24));
        startButton.setStyle(
            "-fx-background-color: #4a4a4a; " +
            "-fx-text-fill: white; " +
            "-fx-border-color: #6a6a6a; " +
            "-fx-border-width: 3px; " +
            "-fx-font-weight: bold;"
        );
        
        // ホバー効果
        startButton.setOnMouseEntered(e -> {
            startButton.setStyle(
                "-fx-background-color: #5a5a5a; " +
                "-fx-text-fill: white; " +
                "-fx-border-color: #8a8a8a; " +
                "-fx-border-width: 3px; " +
                "-fx-font-weight: bold;"
            );
        });
        
        startButton.setOnMouseExited(e -> {
            startButton.setStyle(
                "-fx-background-color: #4a4a4a; " +
                "-fx-text-fill: white; " +
                "-fx-border-color: #6a6a6a; " +
                "-fx-border-width: 3px; " +
                "-fx-font-weight: bold;"
            );
        });
        
        // STARTボタンのイベント
        startButton.setOnAction(e -> onStart());
        
        // クレジット
        Label creditLabel = new Label("Press START to begin");
        creditLabel.setFont(Font.font("Arial", 14));
        creditLabel.setStyle("-fx-text-fill: #555555;");
        
        // レイアウトに追加
        root.getChildren().addAll(
            titleLabel,
            subtitleLabel,
            versionLabel,
            spacer,
            startButton,
            creditLabel
        );
        
        Scene scene = new Scene(root, 1600, 900);
        stage.setScene(scene);
        stage.setTitle("TBRR - Title");
        stage.show();
    }
    
    /**
     * STARTボタンが押された時の処理
     * メインメニューを表示
     */
    private void onStart() {
        MainMenuScreen mainMenu = new MainMenuScreen(stage);
        mainMenu.show();
    }
}