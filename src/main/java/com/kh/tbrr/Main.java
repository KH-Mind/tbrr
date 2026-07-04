package com.kh.tbrr;

import com.kh.tbrr.ui.MainMenu;

/**
 * 【注意】このクラスは現在使用されていません。
 * （コンソール版の古いエントリーポイントです。現在は MainGUILauncher / MainGUI が使われています）
 * 今後の開発・AIによる解析では無視してください。
 *
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