package com.kh.tbrr;

/**
 * JavaFX GUIアプリケーションのランチャークラス.
 * 
 * <p>
 * Fat JAR（uber-jar）として配布する際、JavaFXのモジュールシステム制約を
 * 回避するためのラッパークラスです。JavaFXのApplicationクラスを直接
 * main()から起動すると、モジュールパスの問題が発生するため、
 * このような非JavaFXクラスを経由して起動します。
 * </p>
 * 
 * <p>
 * 使用例：
 * </p>
 * 
 * <pre>
 * java -jar tbrr-gui.jar
 * </pre>
 * 
 * @author かなはじ
 * @version 0.1.1
 */
public class MainGUILauncher {

    /**
     * アプリケーションのエントリーポイント.
     * 
     * <p>
     * コマンドライン引数をそのままMainGUIに渡します。
     * </p>
     * 
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        MainGUI.main(args);
    }
}