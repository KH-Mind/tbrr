package com.kh.tbrr.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * ランダム単語辞書を管理するクラス
 * data/word_lists/*.json から単語リストを読み込み、カテゴリ別にランダムな単語を提供
 * 
 * 【新しいJSONファイルの追加方法】
 * 1. テンプレートに従ってJSONファイルを作成(例: weapons.json)
 * 2. src/main/resources/data/word_lists/ に配置
 * 3. このファイルの WORD_LIST_FILES 配列に追加(例: "weapons")
 * 
 * これだけで自動的に読み込まれます!
 */
public class RandomWordsManager {
	private Map<String, List<String>> categories;
	private Random random;
	private Gson gson;

	/**
	 * 読み込むJSONファイルのリスト(拡張子.jsonは不要)
	 * 
	 * 【新しいファイルを追加する場合】
	 * この配列に名前を追加してください。
	 * 例: "weapons" を追加 → data/word_lists/weapons.json が読み込まれます
	 * 
	 * ファイルが存在しない場合は自動的にスキップされます(エラーにはなりません)
	 */
	private static final String[] WORD_LIST_FILES = {
			// === 汎用 ===
			"nature", // 自然関連(小動物、植物など)
			"colors", // 色関連

			// シナリオ用
			"scenery",

			// === システム用 ===
			"Various_game_overs", // ゲームオーバー関連
			"player_background", // キャラ作成時用
			"npc_quote01", // NPCのランダム文章
			"npc_backgrounds_02", // NPCのランダム背景

			// === 星を見る (意味深)(主に対策) ===
			"orbital_resonance",
			"orbital_path",
			"stellar_formation",
			"event_horizon",

			// === カスタム用(試しにここから、分量増えたら分けよう) ===
			"custom1", // カスタム用スロット1
			"custom2", // カスタム用スロット2
			"custom3" // カスタム用スロット3

	};

	public RandomWordsManager() {
		this.gson = new Gson();
		this.categories = new HashMap<>();
		this.random = new Random();
		loadRandomWords();
	}

	/**
	 * ランダム単語辞書を読み込む
	 * 1. まず data/word_lists/*.json から読み込み (複数ファイル)
	 * 2. 見つからなければ data/random_words.json から読み込み (後方互換性)
	 * 3. どちらも見つからなければデフォルト値を使用
	 */
	private void loadRandomWords() {
		// 1. word_lists/ フォルダから複数ファイルを読み込み
		if (loadFromWordListsFolder()) {
			// System.out.println("✅ ランダム単語辞書読み込み完了 (word_lists): " + categories.size() +
			// "カテゴリ");
			// System.out.println("[DEBUG] 読み込まれたカテゴリ一覧:");
			// for (String cat : categories.keySet()) {
			// int count = categories.get(cat) != null ? categories.get(cat).size() : 0;
			// System.out.println(" - " + cat + ": " + count + "個");
			// }
			return;
		}

		// 2. random_words.json (単一ファイル) から読み込み (後方互換性)
		if (loadFromSingleFile()) {
			// System.out.println("✅ ランダム単語辞書読み込み完了 (random_words.json): " +
			// categories.size() + "カテゴリ");
			return;
		}

		// 3. どちらも見つからない場合はデフォルト値
		System.out.println("[WARNING] ランダム単語辞書が見つかりません。デフォルト値を使用します。");
		loadDefaultWords();
	}

	/**
	 * data/word_lists/ フォルダから複数のJSONファイルを読み込み
	 * 
	 * @return 読み込みに成功した場合true
	 */
	private boolean loadFromWordListsFolder() {
		int loadedFiles = 0;

		for (String fileName : WORD_LIST_FILES) {
			String resourcePath = "data/word_lists/" + fileName + ".json";

			// 方法1: クラスローダーで読み込み(JARファイル対応)
			InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);

			// 方法2: 失敗した場合、Fileで直接読み込み(開発環境用)
			if (is == null) {
				try {
					// target/classes/ を試す
					File file = new File("target/classes/" + resourcePath);
					if (!file.exists()) {
						// src/main/resources/ を試す
						file = new File("src/main/resources/" + resourcePath);
					}

					if (file.exists()) {
						is = new FileInputStream(file);
						// System.out.println("[DEBUG] File APIで読み込み: " + fileName + ".json");
					} else {
						// ファイルが見つからない場合はスキップ
						// System.out.println("[DEBUG] ファイルが見つからない: " + fileName + ".json (スキップ)");
						continue;
					}
				} catch (Exception e) {
					// エラーが発生した場合はスキップ
					System.out.println("[ERROR] " + fileName + ".json の読み込みでエラー: " + e.getMessage());
					continue;
				}
			} else {
				// System.out.println("[DEBUG] クラスローダーで読み込み: " + fileName + ".json");
			}

			// JSONファイルを読み込み
			try (InputStream stream = is) {
				InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
				JsonObject root = gson.fromJson(reader, JsonObject.class);

				// "categories" フィールドからマップを取得
				TypeToken<Map<String, List<String>>> typeToken = new TypeToken<Map<String, List<String>>>() {
				};
				Map<String, List<String>> loadedCategories = gson.fromJson(root.get("categories"), typeToken.getType());

				if (loadedCategories != null) {
					// 既存のカテゴリにマージ(上書きせずに追加)
					for (Map.Entry<String, List<String>> entry : loadedCategories.entrySet()) {
						String category = entry.getKey();
						List<String> words = entry.getValue();

						// デバッグ: カテゴリの内容を表示
						String status = (words == null || words.isEmpty()) ? "(空)" : "(" + words.size() + "個)";
						// System.out.println("[DEBUG] カテゴリ: " + category + " " + status);

						if (categories.containsKey(category)) {
							// 既存のカテゴリに追加
							categories.get(category).addAll(entry.getValue());
						} else {
							// 新規カテゴリとして追加
							categories.put(category, new ArrayList<>(entry.getValue()));
						}
					}
					loadedFiles++;
					// System.out.println(" ✓ " + fileName + ".json 読み込み完了 (" +
					// loadedCategories.size() + "カテゴリ)");
				}

			} catch (Exception e) {
				// エラーが発生した場合はスキップ
				System.out.println("[ERROR] " + fileName + ".json の解析でエラー: " + e.getMessage());
				e.printStackTrace();
			}
		}

		return loadedFiles > 0;
	}

	/**
	 * data/random_words.json から読み込み(後方互換性のため)
	 * 
	 * @return 読み込みに成功した場合true
	 */
	private boolean loadFromSingleFile() {
		try (InputStream is = getClass().getClassLoader()
				.getResourceAsStream("data/random_words.json")) {

			if (is == null) {
				return false;
			}

			InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
			JsonObject root = gson.fromJson(reader, JsonObject.class);

			// "categories" フィールドからマップを取得
			TypeToken<Map<String, List<String>>> typeToken = new TypeToken<Map<String, List<String>>>() {
			};
			categories = gson.fromJson(root.get("categories"), typeToken.getType());

			return categories != null && !categories.isEmpty();

		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * デフォルトの単語リストを読み込む(JSONが見つからない場合のフォールバック)
	 */
	private void loadDefaultWords() {
		categories.put("小動物", List.of(
				"兎", "鼠", "リス", "イタチ", "モグラ", "ハリネズミ",
				"フェレット", "モモンガ", "ハムスター", "トカゲ", "カエル",
				"蛇", "亀", "小鳥", "カラス", "フクロウ", "コウモリ"));

		categories.put("植物", List.of(
				"苔", "キノコ", "シダ", "雑草", "蔦", "低木",
				"花", "根っこ", "木の実"));

		categories.put("色", List.of(
				"赤い", "青い", "緑の", "黄色い", "紫の",
				"黒い", "白い", "灰色の", "茶色い", "銀色の"));
	}

	/**
	 * 指定カテゴリからランダムな単語を取得
	 * 
	 * @param category カテゴリ名(例: "小動物", "植物", "色")
	 * @return ランダムに選ばれた単語、カテゴリが存在しない場合は "???"
	 */
	public String getRandomWord(String category) {
		// System.out.println("[DEBUG] getRandomWord呼び出し: カテゴリ=\"" + category + "\"");

		List<String> words = categories.get(category);

		if (words == null) {
			// System.out.println("[DEBUG] → カテゴリが存在しません → \"???\"");
			return "???";
		}

		if (words.isEmpty()) {
			// System.out.println("[DEBUG] → カテゴリは空です → \"???\"");
			return "???";
		}

		String result = words.get(random.nextInt(words.size()));
		// System.out.println("[DEBUG] → 結果: \"" + result + "\"");
		return result;
	}

	/**
	 * カテゴリが存在するかチェック
	 * 
	 * @param category カテゴリ名
	 * @return 存在する場合true
	 */
	public boolean hasCategory(String category) {
		return categories.containsKey(category);
	}

	/**
	 * すべてのカテゴリ名を取得
	 * 
	 * @return カテゴリ名のリスト
	 */
	public List<String> getAllCategories() {
		return new ArrayList<>(categories.keySet());
	}

	/**
	 * 指定カテゴリの単語リストを取得
	 * 
	 * @param category カテゴリ名
	 * @return 単語のリスト、存在しない場合は空リスト
	 */
	public List<String> getWords(String category) {
		return categories.getOrDefault(category, new ArrayList<>());
	}

	/**
	 * カテゴリと単語リストを追加(動的追加用)
	 * 
	 * @param category カテゴリ名
	 * @param words    単語のリスト
	 */
	public void addCategory(String category, List<String> words) {
		categories.put(category, new ArrayList<>(words));
		// System.out.println("✅ カテゴリ「" + category + "」を追加: " + words.size() + "個の単語");
	}
}