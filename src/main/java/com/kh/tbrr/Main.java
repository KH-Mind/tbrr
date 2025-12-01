package com.kh.tbrr;

import com.kh.tbrr.ui.MainMenu;

/**
 * メインクラス（エントリーポイント）
 * ゲームを起動
 */
public class Main {
    public static void main(String[] args) {
        try {
            // メインメニューを起動
            MainMenu mainMenu = new MainMenu();
            mainMenu.show();

        } catch (Exception e) {
            System.err.println("予期しないエラーが発生しました:");
            e.printStackTrace();
        }
    }
}