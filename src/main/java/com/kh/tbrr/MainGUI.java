package com.kh.tbrr;

import com.kh.tbrr.ui.MainMenuScreen;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX GUI版のメインクラス
 * メインメニューから開始する
 */
public class MainGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        // ウィンドウの「×」ボタンを押したときに完全にプロセスを終了する
        primaryStage.setOnCloseRequest(e -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });

        // メインメニューを直接表示
        MainMenuScreen mainMenu = new MainMenuScreen(primaryStage);
        mainMenu.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}