package com.kh.tbrr.ui;

import java.util.List;

import com.kh.tbrr.data.models.Player;

/**
 * ゲームUIの共通インターフェース
 * ConsoleUI と JavaFXUI の両方で実装される
 */
public interface GameUI {

	/**
	 * テキストを出力
	 */
	void print(String message);

	/**
	 * エラーメッセージを出力
	 */
	void printError(String message);

	/**
	 * 警告メッセージを出力
	 */
	void printWarning(String message);

	/**
	 * タイトルバーを表示
	 */
	void printTitleBar(String title);

	/**
	 * 区切り線を表示
	 */
	void printSeparator();

	/**
	 * 画面クリア
	 */
	void clearScreen();

	/**
	 * ユーザー入力を取得
	 */
	String getInput();

	/**
	 * ユーザー入力を取得 (プロンプト付き)
	 */
	String getInput(String prompt);

	/**
	 * 数値選択を取得
	 * @param max 最大値
	 * @return 選択された数値 (1~max)
	 */
	int getPlayerChoice(int max);

	/**
	 * 数値選択を取得（プレイヤー情報付き）
	 * @param max 最大値
	 * @param currentPlayer 現在のプレイヤー
	 * @return 選択された数値 (1~max)
	 */
	int getPlayerChoice(int max, Player currentPlayer);

	/**
	 * 開発者モードチェック付き選択
	 */
	int getPlayerChoiceWithDevCheck(int max, boolean checkDevMode);

	/**
	 * Yes/Noの選択を取得
	 */
	boolean getYesNo();

	/**
	 * Enterキー待機
	 */
	void waitForEnter();

	/**
	 * プレイヤーの状態を表示（HP/AP/スキル/アイテム）
	 */
	void printPlayerStatus(Player player);

	/**
	 * 選択肢を表示して選択を取得
	 * @param choices 選択肢のリスト
	 * @return 選択されたインデックス (0-based)
	 */
	int showChoices(List<String> choices);

	/**
	 * フロア情報を表示
	 * @param floorNumber フロア番号
	 * @param areaName エリア名
	 */
	void showFloorInfo(int floorNumber, String areaName);

	/**
	 * イベント情報を表示（GUIの専用エリアに表示）
	 * @param eventTitle イベントのタイトル
	 */
	void showEventInfo(String eventTitle);

	/**
	 * 画像を表示（背景、立ち絵、イベント画像など）
	 * @param imageType 画像の種類 ("background", "character", "event")
	 * @param imagePath 画像のパス
	 */
	void showImage(String imageType, String imagePath);
	
	/**
	 * 立ち絵の表情を変更
	 * @param expression 表情名 (smile, sad, surprise, pain など)
	 */
	void changePortraitExpression(String expression);
	
	/**
	 * 立ち絵の表情を基本表情にリセット
	 */
	void resetPortraitExpression();

	/**
	 * 重要なログを表示（HP/AP/お金/アイテムの変化など）
	 * @param message 重要なログメッセージ
	 */
	void printImportantLog(String message);

	/**
	 * UIをクローズ
	 */
	void close();
}
