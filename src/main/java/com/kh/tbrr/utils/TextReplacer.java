package com.kh.tbrr.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kh.tbrr.data.models.Personality;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.manager.RandomWordsManager; // ← 追加

/**
 * テキスト内のプレースホルダーを動的に置き換えるユーティリティクラス
 */
public class TextReplacer {
	private static final Random random = new Random();
	private static RandomWordsManager randomWordsManager; // ← 追加

	// ランダム単語リスト
	private static final String[] SMALL_ANIMALS = {
			"fb", "fb", "fb", "fb", "fb", "fb",
			"fb", "fb", "fb", "fb", "fb",
			"fb", "fb", "fb", "fb", "fb", "fb"
	};

	private static final String[] PLANTS = {
			"苔", "キノコ", "シダ", "雑草", "蔦", "低木",
			"花", "根っこ", "木の実"
	};

	private static final String[] COLORS = {
			"赤い", "青い", "緑の", "黄色い", "紫の",
			"黒い", "白い", "灰色の", "茶色い", "銀色の"
	};

	// プレースホルダーのパターン
	private static final Pattern NAME_PATTERN = Pattern.compile("\\[Name\\]");
	private static final Pattern DIALOGUE_PATTERN = Pattern.compile("\\(([^)]+)\\)");

	// ★ 追加: RandomWordsManagerを設定するメソッド
	/**
	 * RandomWordsManagerを設定（初期化時に一度だけ呼ぶ）
	 * 
	 * @param manager RandomWordsManagerのインスタンス
	 */
	public static void setRandomWordsManager(RandomWordsManager manager) {
		randomWordsManager = manager;
	}

	/**
	 * テキスト内のすべてのプレースホルダーを置き換える
	 * 
	 * @param text   元のテキスト
	 * @param player プレイヤー情報
	 * @return 置き換え後のテキスト
	 */
	public static String replace(String text, Player player) {
		if (text == null || text.isEmpty()) {
			return text;
		}

		// ステップ1: {random:カテゴリ} を先に置き換え
		// （この結果に[Name]などが含まれる可能性があるため、最初に処理）
		text = replaceRandomWords(text);

		// ステップ2: [Name]をプレイヤー名に置き換え
		text = replaceName(text, player);

		// ステップ3: 追加プレースホルダー置き換え
		text = text.replace("[raceName]", player.getRaceName());
		text = text.replace("[PersonalityName]",
				player.getPersonality() != null ? player.getPersonality().getName() : "???");
		text = text.replace("[bodyType]", player.getBodyType() != null ? player.getBodyType() : "???");
		text = text.replace("[clothing]", player.getClothing() != null ? player.getClothing() : "???");

		// ステップ4: 口上プレースホルダーを置き換え
		text = replaceDialogue(text, player);

		return text;
	}

	/**
	 * [Name]をプレイヤー名に置き換える
	 */
	private static String replaceName(String text, Player player) {
		return NAME_PATTERN.matcher(text).replaceAll(player.getName());
	}

	/**
	 * {random:カテゴリ} または {カテゴリ} をランダム単語に置き換え
	 */
	private static String replaceRandomWords(String text) {
		// {random:カテゴリ} と {カテゴリ} の両方に対応
		Pattern pattern = Pattern.compile("\\{(?:random:)?([^}]+)\\}");
		Matcher matcher = pattern.matcher(text);
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String category = matcher.group(1);
			String word = getRandomWord(category);
			matcher.appendReplacement(sb, Matcher.quoteReplacement(word));
		}

		matcher.appendTail(sb);
		return sb.toString();
	}

	/**
	 * カテゴリからランダムな単語を取得
	 */
	private static String getRandomWord(String category) {
		// RandomWordsManagerが設定されていればそれを使用
		if (randomWordsManager != null) {
			return randomWordsManager.getRandomWord(category);
		}

		// フォールバック: ハードコードされたデフォルト値
		String[] words;

		switch (category) {
			case "小動物":
				words = SMALL_ANIMALS;
				break;
			case "植物":
				words = PLANTS;
				break;
			case "色":
				words = COLORS;
				break;
			default:
				return "???";
		}

		return words[random.nextInt(words.length)];
	}

	/**
	 * 口上プレースホルダーを性格JSONから選択した台詞に置き換える
	 * 例: (攻撃時の口上) → 「えいっ!」
	 */
	private static String replaceDialogue(String text, Player player) {
		Personality personality = player.getPersonality();
		if (personality == null) {
			return text;
		}

		StringBuffer result = new StringBuffer();
		Matcher matcher = DIALOGUE_PATTERN.matcher(text);

		while (matcher.find()) {
			String placeholder = matcher.group(1);
			String replacement = getDialogueReplacement(placeholder, personality);

			if (replacement != null) {
				matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
			}
		}
		matcher.appendTail(result);

		return result.toString();
	}

	/**
	 * プレースホルダーに対応する台詞を性格から取得
	 */
	private static String getDialogueReplacement(String placeholder, Personality personality) {
		String key = convertPlaceholderToKey(placeholder);

		if (key == null) {
			return null; // 対応するキーが見つからない場合はそのまま
		}

		List<String> dialogues = personality.getDialogue(key);

		if (dialogues == null || dialogues.isEmpty()) {
			return null;
		}

		// リストからランダムに1つ選択
		return dialogues.get(random.nextInt(dialogues.size()));
	}

	/**
	 * プレースホルダー文字列をJSONのキーに変換
	 * 柔軟に対応できるよう、複数のパターンをサポート
	 */
	private static String convertPlaceholderToKey(String placeholder) {
		// 正規化: 空白を除去、小文字化
		String normalized = placeholder.trim().toLowerCase();

		// パターンマッチング
		if (normalized.matches(".*攻撃.*口上.*")) {
			return "attack";
		} else if (normalized.matches(".*小.*ダメージ.*口上.*") ||
				normalized.matches(".*被.*ダメージ.*口上.*") ||
				normalized.matches(".*ダメージ.*受け.*口上.*")) {
			return "damaged_light";
		} else if (normalized.matches(".*大.*ダメージ.*口上.*") ||
				normalized.matches(".*重傷.*口上.*")) {
			return "damaged_heavy";
		} else if (normalized.matches(".*死.*口上.*") ||
				normalized.matches(".*悲痛.*叫び.*") ||
				normalized.matches(".*断末魔.*")) {
			return "death_scream";
		} else if (normalized.matches(".*回避.*口上.*") ||
				normalized.matches(".*避け.*口上.*")) {
			return "dodge_success";
		} else if (normalized.matches(".*罠.*発見.*口上.*") ||
				normalized.matches(".*罠.*見つけ.*口上.*")) {
			return "trap_found";
		} else if (normalized.matches(".*アイテム.*入手.*口上.*") ||
				normalized.matches(".*アイテム.*取得.*口上.*")) {
			return "item_get";
		} else if (normalized.matches(".*休憩.*口上.*") ||
				normalized.matches(".*休息.*口上.*")) {
			return "rest";
		} else if (normalized.matches(".*勝利.*口上.*") ||
				normalized.matches(".*勝っ.*口上.*")) {
			return "victory";
		} else if (normalized.matches(".*恍惚.*口上.*")) {
			return "ecstasy";
		} else if (normalized.matches(".*朦朧.*口上.*")) {
			return "dazed";
		} else if (normalized.matches(".*苦悶.*口上.*")) {
			return "agony";
		} else if (normalized.matches(".*喘.*口上.*") ||
				normalized.matches(".*息苦.*口上.*")) {
			return "panting";
		} else if (normalized.matches(".*溺れ.*口上.*")) {
			return "drowning";
		} else if (normalized.matches(".*悲鳴.*口上.*")) {
			return "scream";
		} else if (normalized.matches(".*驚愕.*口上.*")) {
			return "shock";
		} else if (normalized.matches(".*嫌悪.*口上.*")) {
			return "disgust";
		} else if (normalized.matches(".*拒否.*口上.*")) {
			return "refusal";
		} else if (normalized.matches(".*メタ死.*口上.*")) {
			return "meta_death";
		}

		return null; // 対応するキーが見つからない
	}

	/**
	 * 複数の文字列に対して一括置き換え
	 */
	public static List<String> replaceAll(List<String> texts, Player player) {
		if (texts == null) {
			return null;
		}

		List<String> result = new ArrayList<>();
		for (String text : texts) {
			result.add(replace(text, player));
		}
		return result;
	}
}