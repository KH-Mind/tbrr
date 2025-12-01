package com.kh.tbrr.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.kh.tbrr.data.ItemRegistry;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.system.DeveloperMode;

/**
 * コンソールUI (開発者モード起動対応版)
 */
public class ConsoleUI implements GameUI {
	private Scanner scanner;
	private String lastRawInput;
	private DeveloperMode developerMode;

	// DeveloperMode を受け取れるコンストラクタ（null 許容）
	public ConsoleUI(DeveloperMode developerMode) {
		this.scanner = new Scanner(System.in);
		this.developerMode = developerMode;
	}

	/**
	 * テキストを出力
	 */
	@Override
	public void print(String message) {
		System.out.println(message);
	}

	/**
	 * エラーメッセージを出力
	 */
	@Override
	public void printError(String message) {
		System.out.println("【エラー】" + message);
	}

	/**
	 * 警告メッセージを出力
	 */
	@Override
	public void printWarning(String message) {
		System.out.println("【警告】" + message);
	}

	/**
	 * タイトルバーを出力
	 */
	@Override
	public void printTitleBar(String title) {
		printSeparator();
		System.out.println(" " + title);
		printSeparator();
	}

	// GUI化用のやつ

	@Override
	public int showChoices(List<String> choices) {
		if (choices == null || choices.isEmpty()) {
			printError("選択肢がありません。");
			return -1;
		}

		for (int i = 0; i < choices.size(); i++) {
			System.out.println((i + 1) + ". " + choices.get(i));
		}

		return getPlayerChoice(choices.size()) - 1; // 0-basedで返す
	}

	/**
	 * 区切り線を出力
	 */
	public void printSeparator() {
		System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━");
	}

	/**
	 * 画面クリア (簡易版)
	 */
	public void clearScreen() {
		// コンソールをクリアする代わりに空行を出力
		for (int i = 0; i < 50; i++) {
			System.out.println();
		}
	}

	/**
	 * ユーザー入力を取得
	 * ここで 開発者モード用 コマンドを常にチェックする (どこでも 開発者モードに)
	 */
	public String getInput() {
		String input = scanner.nextLine().trim();
		if (developerMode != null && input != null && !input.isEmpty()) {
			developerMode.handleDevCommand(input, developerMode.getCurrentPlayer());
		}
		return input;
	}

	/**
	 * ユーザー入力を取得 (プロンプト付き)
	 * ここでも 開発者モードのコマンドが入力されたかをチェックする
	 */
	public String getInput(String prompt) {
		System.out.print(prompt);
		String input = scanner.nextLine().trim();
		if (developerMode != null && input != null && !input.isEmpty()) {
			developerMode.handleDevCommand(input, developerMode.getCurrentPlayer());
		}
		return input;
	}

	/**
	 * 数値選択を取得
	 * @param max 最大値
	 * @return 選択された数値 (1~max) または -999 (開発者モード中は選択をスキップ)
	 */

	public int getPlayerChoice(int max) {
		// キャラクターが未定義の場面では null を渡す
		return getPlayerChoice(max, null);
	}

	public int getPlayerChoice(int max, Player currentPlayer) {
		if (max <= 0) {
			System.out.println("【システム】選択肢がありません。");
			return -1;
		}

		while (true) {
			System.out.print("選択 (1-" + max + "): ");
			String input = scanner.nextLine().trim();

			// 開発者モードコマンド処理（admin/debug/player.sethpなど）
			if (developerMode != null && input != null && !input.isEmpty()) {
				developerMode.handleDevCommand(input, developerMode.getCurrentPlayer()); // ✅ これだけでOK
			}

			// ステータス確認コマンド（数字以外の入力）
			if (currentPlayer != null && !input.matches("\\d+")) {
				if (input.equalsIgnoreCase("status") || input.equalsIgnoreCase("ステータス")
						|| input.equalsIgnoreCase("s")) {
					printSeparator();
					System.out.println(currentPlayer.getCharacterSheet());
					waitForEnter(); // 一時停止
					continue; // 再度選択を促す
				}
			}

			// 数値入力処理
			try {
				int choice = Integer.parseInt(input);
				if (choice >= 1 && choice <= max) {
					return choice;
				} else {
					System.out.println("1から" + max + "の数字を入力してください。");
				}
			} catch (NumberFormatException e) {
				System.out.println("正しい数字を入力してください。");
			}
		}
	}

	/**
	 * 数値選択を取得(開発者モード判定付き)
	 * 「admin」と入力されたら-999を返す
	 *
	 * @param max 最大値
	 * @param checkDevMode 開発者モードチェックを行うか
	 * @return 選択された数値 (1~max) または -999 (dev入力時) または -999 (開発者モード有効時)
	 */
	public int getPlayerChoiceWithDevCheck(int max, boolean checkDevMode) {
		// 開発者モードが有効でも自動スキップはしない
		// admin と入力されたときだけ -999 を返す

		while (true) {
			System.out.print("選択 (1-" + max + "): ");
			String input = scanner.nextLine().trim();

			if (developerMode != null && input != null && !input.isEmpty()) {
				developerMode.handleDevCommand(input, developerMode.getCurrentPlayer()); // ✅ これだけでOK
			}

			try {
				int choice = Integer.parseInt(input);
				if (choice >= 1 && choice <= max) {
					return choice;
				} else {
					System.out.println("1から" + max + "の数字を入力してください。");
				}
			} catch (NumberFormatException e) {
				System.out.println("正しい数字を入力してください。");
			}
		}
	}

	/**
	 * Yes/Noの選択を取得
	 */
	public boolean getYesNo() {
		while (true) {
			System.out.print("よろしいですか？ (y/n): ");
			String input = scanner.nextLine().trim().toLowerCase();

			if (developerMode != null && input != null && !input.isEmpty()) {
				developerMode.handleDevCommand(input, developerMode.getCurrentPlayer());
			}

			if (input.equals("y") || input.equals("yes")) {
				return true;
			} else if (input.equals("n") || input.equals("no")) {
				return false;
			} else {
				System.out.println("y または n を入力してください。");
			}
		}
	}

	/**
	 * Enterキー待機
	 */
	public void waitForEnter() {
		System.out.print("Press Enter to continue...");
		String line = scanner.nextLine();
		if (developerMode != null && line != null && !line.trim().isEmpty()) {
			developerMode.handleDevCommand(line.trim(), developerMode.getCurrentPlayer());
		}
	}

	/**
	 * フロア情報を表示
	 */
	@Override
	public void showFloorInfo(int floorNumber, String areaName) {
		printSeparator();
		System.out.println("【フロア" + floorNumber + "】 " + areaName);
		printSeparator();
	}

	/**
	 * イベント情報を表示（コンソールではタイトルとして表示）
	 */
	@Override
	public void showEventInfo(String eventTitle) {
		if (eventTitle != null && !eventTitle.isEmpty()) {
			printSeparator();
			System.out.println(" " + eventTitle);
			printSeparator();
		}
	}

	/**
	 * 画像を表示（コンソールでは画像パス表示のみ）
	 */
	@Override
	public void showImage(String imageType, String imagePath) {
		// コンソールでは画像を表示できないので、情報のみ表示
		if (developerMode != null && developerMode.isDebugVisible()) {
			System.out.println("[画像: " + imageType + " - " + imagePath + "]");
		}
	}
	
	/**
	 * 立ち絵の表情を変更（コンソール版では何もしない）
	 */
	@Override
	public void changePortraitExpression(String expression) {
		// コンソール版では立ち絵がないため、デバッグ情報のみ表示
		if (developerMode != null && developerMode.isDebugVisible()) {
			System.out.println("[表情変更: " + expression + "]");
		}
	}
	
	/**
	 * 立ち絵の表情をリセット（コンソール版では何もしない）
	 */
	@Override
	public void resetPortraitExpression() {
		// コンソール版では立ち絵がないため、何もしない
		if (developerMode != null && developerMode.isDebugVisible()) {
			System.out.println("[表情リセット: base]");
		}
	}

	/**
	 * プレイヤーの状態を表示（HP/AP/スキル/アイテム）
	 */
	public void printPlayerStatus(Player player) {
		printSeparator();
		System.out.println(player.getStatusString()); // 例: [ユミィ] HP:100/100 AP:20/20

		// スキル表示（baseSkills + アイテム由来）
		List<String> skills = player.getEffectiveSkills();
		if (skills != null && !skills.isEmpty()) {
			System.out.println("技能: " + String.join(" / ", skills));
		}

		// アイテム表示（名前変換付き）
		List<String> itemIds = player.getInventory();
		if (itemIds != null && !itemIds.isEmpty()) {
			List<String> itemNames = new ArrayList<>();
			for (String id : itemIds) {
				String name = ItemRegistry.getNameById(id);
				itemNames.add(name != null ? name : id);
			}
			System.out.println("所持品: " + String.join(" / ", itemNames));
		}

		// 状態異常表示
		java.util.Map<String, Integer> statusEffects = player.getStatusEffects();
		if (statusEffects != null && !statusEffects.isEmpty()) {
			List<String> statusNames = new ArrayList<>();
			for (java.util.Map.Entry<String, Integer> entry : statusEffects.entrySet()) {
				String effectId = entry.getKey();
				int value = entry.getValue();
				String effectName = com.kh.tbrr.data.StatusEffectRegistry.getNameById(effectId);
				statusNames.add((effectName != null ? effectName : effectId) + ":" + value);
			}
			System.out.println("状態異常: " + String.join(" / ", statusNames));
		}

		printSeparator();
	}

	/**
	 * 重要なログを表示（コンソール版では通常のprintと同じ動作）
	 */
	@Override
	public void printImportantLog(String message) {
		print(message);
	}

	/**
	 * クローズ
	 */
	public void close() {
		scanner.close();
	}
}