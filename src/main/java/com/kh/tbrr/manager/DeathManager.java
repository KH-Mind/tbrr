package com.kh.tbrr.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.google.gson.JsonObject;
import com.kh.tbrr.core.GameState;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.ui.GameUI;
import com.kh.tbrr.utils.TextReplacer;

/**
 * 死亡エンディング管理クラス
 * 死因に応じた死亡テキストの選択・表示を担当
 */
public class DeathManager {
	private GameUI ui; // ConsoleUI から GameUI に変更
	private DataManager dataManager;
	private Random random;

	public DeathManager(GameUI ui, DataManager dataManager) {
		this.ui = ui;
		this.dataManager = dataManager;
		this.random = new Random();
	}

	public boolean isDeathSensitive(String deathKey) {
		JsonObject obj = dataManager.loadDeathJson(deathKey); // death_by_xxx.json を読み込む内部メソッド
		if (obj != null && obj.has("sensitive")) {
			return obj.get("sensitive").getAsBoolean();
		}
		return false;
	}

	// 運命に導かれしものの定義
	public boolean checkFatedOne(Player player) {
		return player != null && player.isFatedOne();
	}

	/**
	 * 死亡処理のメイン
	 */
	public void processDeath(String deathCause, Player player, GameState gameState) {
	    System.out.println("[DEBUG] deathCause = " + deathCause);
	    
	    //ここではまだGAME OVERを表示しない
	    
	    List<String> endings = null;

		// ① deathCause が未設定ならエラー表示して中断
		if (deathCause == null || deathCause.isEmpty()) {
			ui.print("【エラー】死亡イベントに死因が設定されていません。");
			ui.print("deathCause が null または空文字です。");
			ui.print("汎用死亡エンドを使いたい場合は deathCause を \"generic\" に設定してください。");
			ui.print("");
			return;
		}

		// ② deathCause が指定されていれば優先
		String deathKey = "death_by_" + deathCause;

		// ここでファイル存在チェックを追加
		if (!dataManager.deathJsonExists(deathKey)) {
			ui.print("【警告】指定された死因 \"" + deathCause + "\" に対応する死亡イベントファイルが存在しません。");
			ui.print("汎用死亡エンドにフォールバックする可能性があります。");
			ui.print("");
		}

		// センシティブチェックを含めた死亡エンド取得処理
		endings = new ArrayList<>(dataManager.getDeathEndings(deathKey)); // 通常の死亡文を取得

		if (player.isCruelWorldEnabled()) {
			// センシティブ設定がONなら、追加でセンシティブな死亡文も取得
			List<String> sensitive = dataManager.getSensitiveDeathEndings(deathKey);
			if (sensitive != null)
				endings.addAll(sensitive);
		}

		// ③ deathCause が無効 or 読み込めなかった場合 → イベントタグから探す
		if ((endings == null || endings.isEmpty()) && gameState != null && gameState.getCurrentEvent() != null) {
			List<String> tags = gameState.getCurrentEvent().getTags();
			if (tags != null) {
				for (String tag : tags) {
					if (tag != null && !tag.equalsIgnoreCase("none")) {
						String tagKey = tag.toLowerCase();
						endings = new ArrayList<>(dataManager.getDeathEndings(tagKey));
						if (player.isCruelWorldEnabled()) {
							List<String> sensitive = dataManager.getSensitiveDeathEndings(tagKey);
							if (sensitive != null)
								endings.addAll(sensitive);
						}
						if (!endings.isEmpty()) {
							break; // 最初に見つかったタグで採用
						}
					}
				}
			}
		}

		// ④ それでも見つからなければ汎用死亡エンドへ
		if (endings == null || endings.isEmpty()) {
			endings = new ArrayList<>(dataManager.getDeathEndings("generic"));
			if (player.isCruelWorldEnabled()) {
				List<String> sensitive = dataManager.getSensitiveDeathEndings("generic");
				if (sensitive != null)
					endings.addAll(sensitive);
			}
		}

		// ⑤ 最終表示処理
				
		ui.waitForEnter(); // ここでEnter待ち
		ui.print("");
		ui.print("━━━━━━━━━━━━━━━━━━━━━━━━");
		ui.print("        GAME OVER");
		ui.print("━━━━━━━━━━━━━━━━━━━━━━━━");
		ui.print("");
		
		
		if (endings != null && !endings.isEmpty()) {
			String selected = endings.get(random.nextInt(endings.size()));
			selected = TextReplacer.replace(selected, player); // プレースホルダを保持したまま置換
			ui.print(selected);
			// ui.print("");

			// followups 表示処理（センシティブ対応済み）
			List<String> followups = dataManager.getDeathFollowups(deathKey);
			if (followups == null)
				followups = new ArrayList<>();
			if (player.isCruelWorldEnabled()) {
				List<String> sensitiveFollowups = dataManager.getSensitiveDeathFollowups(deathKey);
				if (sensitiveFollowups != null)
					followups.addAll(sensitiveFollowups);
			}

			if (!followups.isEmpty()) {
				String extra = followups.get(random.nextInt(followups.size()));
				extra = TextReplacer.replace(extra, player); // プレースホルダを保持したまま置換
				ui.print(extra);
				// ui.print(""); ←改行は任意で調整
			}
		} else {
			ui.print("【死亡エンドですらないエンド】");
			ui.print(player.getName() + "これが出た場合は汎用死亡エンドすら読めてないのでバグです。");
			ui.print("");
		}

		// 死亡後の「やられちまったぜ」的な表記　Elonaの　しくしく　とか　今夜は眠れないな　の表示のやつのリスペクト
		ui.print("");
		List<String> deathMessages = Arrays.asList(
				"やられちまったぜ",
				"{ゲームオーバー色々１}",
				"{ゲームオーバー色々２}");
		String selected = deathMessages.get(random.nextInt(deathMessages.size()));
		selected = TextReplacer.replace(selected, player);
		ui.print(selected);
		// ui.print("");
		ui.waitForEnter();

		// 運命に導かれし者の判定
		if (player.isFatedOne()) {
			ui.print("しかし、運命はまだ" + player.getName() + "を見放していない。");
			ui.print("");
		} else {
			ui.print(player.getName() + "は永遠に失われた。");
			ui.print("このキャラクターは二度と使用できません。");
			ui.print("");
			gameState.markCharacterAsLost(player); // キャラ削除処理（仮）
		}

		gameState.setGameOver(true);
		gameState.recordDeath(deathCause != null ? deathCause : "generic");

		showDeathStatistics(gameState);
	}

	/**
	 * 死亡後の統計表示
	 */
	private void showDeathStatistics(GameState gameState) {
		ui.print("━━━━━━━━━━━━━━━━━━━━━━━━");
		ui.print("  冒険の記録");
		ui.print("━━━━━━━━━━━━━━━━━━━━━━━━");
		ui.print("到達フロア: " + gameState.getCurrentFloor());
		ui.print("討伐数: " + gameState.getCounter("monsters_defeated"));
		ui.print("発見アイテム数: " + gameState.getCounter("items_found"));
		ui.print("");
		ui.print("解禁済み死亡エンド: " + gameState.getUnlockedDeathCount() + " 種類");
		ui.print("━━━━━━━━━━━━━━━━━━━━━━━━");
		ui.print("");

	}
}
