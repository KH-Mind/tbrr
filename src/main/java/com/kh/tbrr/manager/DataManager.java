package com.kh.tbrr.manager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.kh.tbrr.data.models.GameEvent;
import com.kh.tbrr.data.models.GameMap;
import com.kh.tbrr.data.models.Item;
import com.kh.tbrr.data.models.Scenario;
import com.kh.tbrr.system.DeveloperMode;

/**
 * データ管理クラス（リニューアル版）
 * JSONファイルからゲームデータを読み込む
 * ポータブル版対応：ファイルシステム優先、なければJAR内リソースから読み込む
 */
public class DataManager {
	private Gson gson;
	private DeveloperMode developerMode;

	// リソースのルートパス（相対パス）
	private static final String DATA_ROOT = "data/";

	// 死亡エンディングのサブフォルダ
	private static final String[] DEATH_FOLDERS = {
			"common", // 共通の死亡エンド
			"unique", // ユニークな死亡エンド
			"monster", // モンスター関連の死亡エンド
			"traps", // 罠関連の死亡エンド
			"remnant"
	};

	// キャッシュ

	private Map<String, Item> itemCache;
	private Map<String, GameEvent> eventCache;
	private Map<String, Scenario> scenarioCache;
	private Map<String, GameMap> mapCache;
	private Map<String, List<String>> deathEndings = new HashMap<>();

	/**
	 * コンストラクタ
	 */
	public DataManager(DeveloperMode developerMode) {
		this.developerMode = developerMode;
		this.gson = new GsonBuilder().setPrettyPrinting().create();

		this.itemCache = new HashMap<>();
		this.eventCache = new HashMap<>();
		this.scenarioCache = new HashMap<>();
		this.mapCache = new HashMap<>();
		this.deathEndings = new HashMap<>();
		loadDeathEndings();
	}

	public void setDataPath(String path) {
		// Deprecated or unused
	}

	/**
	 * 汎用読み込みメソッド
	 * 1. ローカルファイルシステム (実行ディレクトリ/path) を確認
	 * 2. なければクラスパス (JAR内/path) を確認
	 * 3. jpackageポータブル版用のパス (app/data/...) を確認
	 */
	private String loadResourceContent(String path) throws IOException {
		// 1. ローカルファイルシステム (優先: MOD/ユーザーデータ)
		File file = new File(path);
		if (file.exists()) {
			return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		}

		// 2. 開発環境用フォールバック (src/main/resources)
		File devFile = new File("src/main/resources/" + path);
		if (devFile.exists()) {
			return new String(Files.readAllBytes(devFile.toPath()), StandardCharsets.UTF_8);
		}

		// 3. jpackageポータブル版用パス（app/data/...）
		String jpackagePath = "app/" + path;
		File jpackageFile = new File(jpackagePath);
		if (jpackageFile.exists()) {
			return new String(Files.readAllBytes(jpackageFile.toPath()), StandardCharsets.UTF_8);
		}

		// 4. クラスパス (JAR内 / ビルド済みリソース)
		String resourcePath = path.replace("\\", "/");

		// A. 相対パスで試行
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
			if (is != null) {
				return new String(is.readAllBytes(), StandardCharsets.UTF_8);
			}
		}

		// B. 絶対パス（先頭スラッシュあり）で試行 - これがjpackage等で重要
		String absolutePath = "/" + resourcePath;
		try (InputStream is = getClass().getResourceAsStream(absolutePath)) {
			if (is != null) {
				return new String(is.readAllBytes(), StandardCharsets.UTF_8);
			}
		}

		// C. data/ を除いたパスでも試行 (念のため)
		if (resourcePath.startsWith("data/")) {
			String strippedPath = resourcePath.substring(5);
			try (InputStream is = getClass().getClassLoader().getResourceAsStream(strippedPath)) {
				if (is != null) {
					return new String(is.readAllBytes(), StandardCharsets.UTF_8);
				}
			}
			// 絶対パスでも試行
			try (InputStream is = getClass().getResourceAsStream("/" + strippedPath)) {
				if (is != null) {
					return new String(is.readAllBytes(), StandardCharsets.UTF_8);
				}
			}
		}

		String errorMsg = "Resource not found: " + path;
		throw new IOException(errorMsg);
	}

	/**
	 * リソースが存在するかチェック
	 */
	/**
	 * リソースが存在するかチェック
	 */
	private boolean resourceExists(String path) {
		// 1. ローカルファイル
		File file = new File(path);
		if (file.exists())
			return true;

		// 2. 開発環境用フォールバック (src/main/resources)
		File devFile = new File("src/main/resources/" + path);
		if (devFile.exists())
			return true;

		// 3. jpackageポータブル版用パス (app/data/...)
		String jpackagePath = "app/" + path;
		File jpackageFile = new File(jpackagePath);
		if (jpackageFile.exists())
			return true;

		// 4. クラスパス
		String resourcePath = path.replace("\\", "/");
		return getClass().getClassLoader().getResource(resourcePath) != null;
	}

	public List<Item> loadAllItemsFromFile(String filename) {
		try {
			String filePath = DATA_ROOT + "items/" + filename;
			String json = loadResourceContent(filePath);

			// TypeToken を明示的に使う
			TypeToken<List<Item>> token = new TypeToken<>() {
			};
			List<Item> items = gson.fromJson(json, token.getType());

			// ファイル名からレアリティを推測して設定
			String rarity = null;
			if (filename.contains("common")) {
				rarity = "common";
			} else if (filename.contains("magic")) {
				rarity = "magic";
			} else if (filename.contains("unique")) {
				rarity = "unique";
			} else if (filename.contains("job")) {
				rarity = "job";
			}

			if (rarity != null && items != null) {
				for (Item item : items) {
					if (item.getRarity() == null) {
						item.setRarity(rarity);
					}
				}
			}

			return items != null ? items : List.of();

		} catch (IOException e) {
			System.err.println("アイテム一覧の読み込みに失敗: " + filename);
			e.printStackTrace();
			return List.of();
		}
	}

	public Item loadItem(String itemId) {
		if (itemCache.containsKey(itemId)) {
			return itemCache.get(itemId);
		}

		try {
			String filePath = DATA_ROOT + "items/" + itemId + ".json";
			String json = loadResourceContent(filePath);
			Item item = gson.fromJson(json, Item.class);
			itemCache.put(itemId, item);
			return item;
		} catch (IOException e) {
			System.err.println("アイテムデータの読み込みに失敗: " + itemId);
			e.printStackTrace();
			return null;
		}
	}

	public List<String> getAllItemIds() {
		return getFileNamesInDirectory(DATA_ROOT + "items/");
	}

	/**
	 * 状態異常一覧をファイルから読み込む
	 */
	public List<com.kh.tbrr.data.models.StatusEffect> loadAllStatusEffectsFromFile(String filename) {
		try {
			String filePath = DATA_ROOT + "status_effects/" + filename;
			String json = loadResourceContent(filePath);

			TypeToken<List<com.kh.tbrr.data.models.StatusEffect>> token = new TypeToken<>() {
			};
			return gson.fromJson(json, token.getType());

		} catch (IOException e) {
			System.err.println("状態異常一覧の読み込みに失敗: " + filename);
			e.printStackTrace();
			return List.of();
		}
	}

	// イベントフォルダのリストを定数化（または動的取得）
	private static final String[] EVENT_SUBDIRS = {
			"system", "common", "unique", "landscapes", "traps", "hazard", "npc",
			"encounter", "friendly", "slime", "test", "untested", "creature",
			"celestial_alignment", "memorial_chamber",
			"old_manor", "encounters", "special", "boss" // 重複や漏れを統合
	};

	public GameEvent loadEvent(String eventId) {
		if (eventCache.containsKey(eventId)) {
			return eventCache.get(eventId);
		}

		for (String subdir : EVENT_SUBDIRS) {
			String filePath = DATA_ROOT + "events/" + subdir + "/" + eventId + ".json";

			// Debug print
			// System.out.println("Checking event: " + filePath);

			if (resourceExists(filePath)) {
				try {
					System.out.println("[DEBUG] Found event file: " + filePath);
					String json = loadResourceContent(filePath);
					GameEvent event = gson.fromJson(json, GameEvent.class);
					if (event != null) {
						eventCache.put(eventId, event);
						return event;
					} else {
						System.err.println("[ERROR] Parsed event is null: " + eventId);
					}
				} catch (Exception e) {
					System.err.println("[ERROR] JSON読み込みエラー: " + eventId + " (" + filePath + ")");
					e.printStackTrace();
					return null;
				}
			}
		}

		System.err.println("[ERROR] イベントファイルが見つかりません: " + eventId);
		return null;
	}

	public List<GameEvent> getEventsByTag(String tag) {
		List<GameEvent> result = new ArrayList<>();

		for (String subdir : EVENT_SUBDIRS) {
			String dirPath = DATA_ROOT + "events/" + subdir + "/";
			List<String> eventIds = getFileNamesInDirectory(dirPath);

			for (String eventId : eventIds) {
				GameEvent event = loadEvent(eventId);
				if (event != null && event.getTags().contains(tag)) {
					result.add(event);
				}
			}
		}

		return result;
	}

	public List<GameEvent> getEventsByTags(List<String> tags) {
		List<GameEvent> result = new ArrayList<>();

		for (String subdir : EVENT_SUBDIRS) {
			String dirPath = DATA_ROOT + "events/" + subdir + "/";
			List<String> eventIds = getFileNamesInDirectory(dirPath);

			for (String eventId : eventIds) {
				GameEvent event = loadEvent(eventId);
				if (event != null && event.getTags().containsAll(tags)) {
					result.add(event);
				}
			}
		}

		return result;
	}

	public GameMap loadMap(String id) {
		if (mapCache.containsKey(id)) {
			return mapCache.get(id);
		}

		try {
			String filePath = DATA_ROOT + "maps/" + id + ".json";
			String json = loadResourceContent(filePath);
			GameMap map = gson.fromJson(json, GameMap.class);
			mapCache.put(id, map);
			return map;
		} catch (IOException e) {
			System.err.println("マップデータの読み込みに失敗: " + id);
			e.printStackTrace();
			return null;
		}
	}

	public List<String> getAllMapIds() {
		return getFileNamesInDirectory(DATA_ROOT + "maps/");
	}

	public List<String> getMapIdsInFolder(String folderName) {
		// ファイルシステムのみ走査
		List<String> result = new ArrayList<>();

		File folder = new File(DATA_ROOT + "maps/" + folderName);
		if (!folder.exists() || !folder.isDirectory())
			return result;

		File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
		if (files == null)
			return result;

		for (File file : files) {
			String fileName = file.getName();
			String id = fileName.replace(".json", "");
			result.add(id);
		}

		return result;
	}

	// 再帰探索メソッド
	public List<String> getAllMapFileNamesRecursively(String resourcePath) {
		List<String> result = new ArrayList<>();

		// マップファイルのリストを明示的に定義（JavaFX/モジュールシステム対応）
		String[] folders = { "wilderness", "cave", "dungeon", "town", "unique" };
		String[][] fileNames = {
				// wilderness
				{ "plains", "swamp", "forest", "island", "mountain" },
				// cave
				{ "abandoned_mine", "beast_den", "rock_cave", "stalactite_cave", "subterranean_cave" },
				// dungeon
				{ "ancient_ruins", "bandit_hideout", "necromancer_lair", "underground_passage", "waterway" },
				// town
				{ "slum", "town", "village" },
				// unique
				{ "cursed_estate", "memorial_chamber", "test_area" }
		};

		System.out.println("[INFO] マップファイルリストを構築中...");

		for (int i = 0; i < folders.length; i++) {
			String folder = folders[i];
			for (String fileName : fileNames[i]) {
				String fullPath = folder + "/" + fileName;
				// resourcePath引数は無視して DATA_ROOT を基準にする
				String checkPath = DATA_ROOT + "maps/" + fullPath + ".json";

				if (resourceExists(checkPath)) {
					result.add(fullPath);
					if (developerMode != null && developerMode.isDebugVisible()) {
						System.out.println("[DEBUG] 追加: " + fullPath);
					}
				} else {
					System.err.println("[WARNING] ファイルが見つかりません: " + checkPath);
				}
			}
		}

		System.out.println("[INFO] マップファイルリスト構築完了: " + result.size() + "件");

		if (result.isEmpty()) {
			System.err.println("[ERROR] マップが一つも読み込めませんでした！");
			System.err.println("[ERROR] 以下を確認してください：");
			System.err.println("  1. target/classes/data/maps/ にファイルが存在するか");
			System.err.println("  2. プロジェクトをクリーン＆ビルドしたか");
		}

		return result;
	}

	public Scenario loadScenario(String id) {
		if (scenarioCache.containsKey(id)) {
			return scenarioCache.get(id);
		}

		try {
			String filePath = DATA_ROOT + "scenarios/" + id + ".json";
			String json = loadResourceContent(filePath);
			Scenario scenario = gson.fromJson(json, Scenario.class);
			scenarioCache.put(id, scenario);
			return scenario;
		} catch (IOException e) {
			if (developerMode != null && developerMode.isDebugVisible()) {
				System.err.println("[DEBUG] シナリオ読み込みエラー: " + e.getMessage());
			}
			return null;
		}
	}

	public List<String> getAllScenarioIds() {
		// 1. scenario_list.txt から読み込み (JAR/ビルド済み環境用)
		List<String> list = loadIdList("scenarios/scenario_list.txt");
		if (list != null && !list.isEmpty()) {
			return list;
		}

		// 2. ファイルシステム走査 (開発環境用)
		// "data/scenarios/" または "src/main/resources/data/scenarios/" を探す
		return getFileNamesInDirectory(DATA_ROOT + "scenarios/");
	}

	/**
	 * リソース内のリストファイル(txt)を読み込んでリスト化する
	 */
	private List<String> loadIdList(String relativePath) {
		try {
			String content = loadResourceContent(DATA_ROOT + relativePath);
			String[] lines = content.split("\\r?\\n");
			List<String> list = new ArrayList<>();
			for (String line : lines) {
				line = line.trim();
				if (!line.isEmpty()) {
					// パスからファイル名(ID)を抽出
					// 例: "src/main/resources/data/scenarios/test.json" -> "test"
					// 例: "test.json" -> "test"
					String fileName = new File(line).getName();
					if (fileName.endsWith(".json")) {
						list.add(fileName.replace(".json", ""));
					}
				}
			}
			return list;
		} catch (IOException e) {
			// リストファイルがない場合はnullを返す
			return null;
		}
	}

	/**
	 * deathJsonが存在するかチェック（リソース or File）
	 */
	public boolean deathJsonExists(String deathKey) {
		for (String folder : DEATH_FOLDERS) {
			String path = DATA_ROOT + "deaths/" + folder + "/" + deathKey + ".json";
			if (resourceExists(path))
				return true;
		}
		// 後方互換
		String oldPath = DATA_ROOT + "deaths/" + deathKey + ".json";
		if (resourceExists(oldPath))
			return true;

		return false;
	}

	/**
	 * ディレクトリ内のファイル名一覧を取得
	 * ※ JAR内のディレクトリ一覧は取得できないため、ローカルファイルのみ対象
	 */
	public List<String> getFileNamesInDirectory(String dirPath) {
		List<String> fileNames = new ArrayList<>();

		// 開発環境対応: src/main/resources/ をプレフィックスとして試す
		File dir = new File(dirPath);
		if (!dir.exists()) {
			File devDir = new File("src/main/resources/" + dirPath);
			if (devDir.exists()) {
				dir = devDir;
			}
		}

		try {
			if (dir.exists() && dir.isDirectory()) {
				File[] files = dir.listFiles();
				if (files != null) {
					for (File file : files) {
						if (file.isFile() && file.getName().endsWith(".json")) {
							String fileName = file.getName().replace(".json", "");
							fileNames.add(fileName);
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("ディレクトリの読み込みに失敗: " + dirPath);
			e.printStackTrace();
		}

		return fileNames;
	}

	/**
	 * 死亡エンディングを読み込む（メインメソッド）
	 * リソース -> File の順に試行
	 */
	public void loadDeathEndings() {
		System.out.println("[DataManager] 死亡エンディングを読み込み中...");
		int loadedFiles = 0;

		for (String folder : DEATH_FOLDERS) {
			// 各フォルダのindex.jsonを読み込む
			String indexPath = DATA_ROOT + "deaths/" + folder + "/index.json";

			try {
				String indexJson = loadResourceContent(indexPath);
				JsonObject indexObj = gson.fromJson(indexJson, JsonObject.class);

				if (indexObj.has("deaths")) {
					JsonArray deathsArray = indexObj.getAsJsonArray("deaths");

					// インデックスに記載された各ファイルを読み込む
					for (JsonElement elem : deathsArray) {
						String deathId = elem.getAsString();
						String deathFilePath = DATA_ROOT + "deaths/" + folder + "/" + deathId + ".json";

						if (loadSingleDeathFile(deathFilePath)) {
							loadedFiles++;
						}
					}
				}
			} catch (IOException e) {
				// インデックスがない場合はスキップ（またはローカルフォルダ走査などを実装可能）
				if (developerMode != null && developerMode.isDebugVisible()) {
					System.out.println("[DEBUG] インデックス読み込み失敗: " + indexPath);
				}
			}
		}

		if (loadedFiles > 0) {
			System.out.println("✅ 死亡エンディング読み込み完了: " + deathEndings.size() + "ファイル");
		} else {
			System.out.println("⚠ 死亡エンディングファイルが見つかりません。");
		}
	}

	private boolean loadSingleDeathFile(String path) {
		try {
			String json = loadResourceContent(path);
			JsonElement root = gson.fromJson(json, JsonElement.class);

			if (root.isJsonObject()) {
				JsonObject obj = root.getAsJsonObject();

				if (obj.has("endings")) {
					// パスからファイル名(キー)を抽出
					String fileName = path.substring(path.lastIndexOf('/') + 1);
					String key = fileName.replace(".json", "");

					JsonArray arr = obj.getAsJsonArray("endings");
					List<String> endings = new ArrayList<>();
					for (JsonElement e : arr) {
						endings.add(e.getAsString());
					}
					deathEndings.put(key, endings);
					return true;
				}
			}
		} catch (Exception e) {
			System.err.println("[ERROR] ファイル読み込みエラー: " + path + " - " + e.getMessage());
		}

		return false;
	}

	public List<String> getDeathEndings(String cause) {
		return deathEndings.getOrDefault(cause, deathEndings.get("death_by_generic"));
	}

	/**
	 * センシティブな死亡エンディングを取得
	 */
	public List<String> getSensitiveDeathEndings(String cause) {
		JsonObject obj = loadDeathJson(cause);
		if (obj != null && obj.has("sensitiveEndings")) {
			JsonArray arr = obj.getAsJsonArray("sensitiveEndings");
			List<String> sensitive = new ArrayList<>();
			for (JsonElement e : arr) {
				sensitive.add(e.getAsString());
			}
			return sensitive;
		}
		return List.of();
	}

	/**
	 * センシティブなフォローアップを取得
	 */
	public List<String> getSensitiveDeathFollowups(String cause) {
		JsonObject obj = loadDeathJson(cause);
		if (obj != null && obj.has("sensitiveFollowups")) {
			JsonArray arr = obj.getAsJsonArray("sensitiveFollowups");
			List<String> sensitive = new ArrayList<>();
			for (JsonElement e : arr) {
				sensitive.add(e.getAsString());
			}
			return sensitive;
		}
		return List.of();
	}

	/**
	 * フォローアップを取得
	 */
	public List<String> getDeathFollowups(String cause) {
		JsonObject obj = loadDeathJson(cause);
		if (obj != null && obj.has("followups")) {
			JsonArray arr = obj.getAsJsonArray("followups");
			List<String> followups = new ArrayList<>();
			for (JsonElement e : arr) {
				followups.add(e.getAsString());
			}
			return followups;
		}
		return null;
	}

	/**
	 * 死亡JSONオブジェクトを読み込む
	 */
	public JsonObject loadDeathJson(String deathKey) {
		for (String folder : DEATH_FOLDERS) {
			String path = DATA_ROOT + "deaths/" + folder + "/" + deathKey + ".json";
			try {
				String json = loadResourceContent(path);
				return gson.fromJson(json, JsonObject.class);
			} catch (IOException e) {
				// continue
			}
		}
		// 後方互換
		String oldPath = DATA_ROOT + "deaths/" + deathKey + ".json";
		try {
			String json = loadResourceContent(oldPath);
			return gson.fromJson(json, JsonObject.class);
		} catch (IOException e) {
			// continue
		}

		System.err.println("[DataManager] loadDeathJson 読み込み失敗: " + deathKey);
		return null;
	}

	/**
	 * sensitiveフラグを取得
	 */
	public boolean isDeathSensitive(String deathKey) {
		JsonObject obj = loadDeathJson(deathKey);
		if (obj != null && obj.has("sensitive")) {
			return obj.get("sensitive").getAsBoolean();
		}
		return false;
	}

	public void clearCache() {

		itemCache.clear();
		eventCache.clear();
		scenarioCache.clear();
		mapCache.clear();
	}

	public void clearItemCache() {
		itemCache.clear();
	}

	public void clearEventCache() {
		eventCache.clear();
	}

	public void clearScenarioCache() {
		scenarioCache.clear();
	}

	public void clearMapCache() {
		mapCache.clear();
	}
}