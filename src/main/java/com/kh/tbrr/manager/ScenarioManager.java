package com.kh.tbrr.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.kh.tbrr.core.GameState;
import com.kh.tbrr.data.models.GameMap;
import com.kh.tbrr.data.models.Scenario;
import com.kh.tbrr.system.DeveloperMode;

/**
 * シナリオ・マップ管理クラス
 */
public class ScenarioManager {
	private java.util.Map<String, Scenario> scenarios;
	private java.util.Map<String, GameMap> maps;
	private DataManager dataManager;
	private Gson gson;
	private Random random;
	private DeveloperMode developerMode;

	/**
	 * コンストラクタ (DataManager受け取り版)
	 */
	public ScenarioManager(DataManager dataManager, DeveloperMode developerMode) {
		// if (developerMode != null && developerMode.isDebugVisible()) {
		// System.err.println("[DEBUG] ScenarioManager constructed: " + this);
		// }
		this.dataManager = dataManager;
		this.developerMode = developerMode;
		this.scenarios = new HashMap<>();
		this.maps = new HashMap<>();
		this.gson = new Gson();
		this.random = new Random();

		loadMaps();
		loadScenarios();
	}

	/**
	 * シナリオファイルを読み込む
	 */
	private void loadScenarios() {
		// DataManagerから全シナリオIDを取得（自動読み込み）
		List<String> scenarioIds = dataManager.getAllScenarioIds();

		// System.out.println("[INFO] シナリオファイル検出数: " + scenarioIds.size());

		if (scenarioIds.isEmpty()) {
			System.err.println("[WARNING] シナリオファイルが1つも見つかりませんでした");
			System.err.println("[WARNING] src/main/resources/data/scenarios/ フォルダを確認してください");
		}

		for (String scenarioId : scenarioIds) {

			InputStream is = null;

			// ★★★ 複数のパターンを試す ★★★
			// パターン1: スラッシュあり、getResourceAsStream
			is = getClass().getResourceAsStream("/data/scenarios/" + scenarioId + ".json");

			// パターン2: スラッシュなし、ClassLoader
			if (is == null) {
				is = getClass().getClassLoader().getResourceAsStream("data/scenarios/" + scenarioId + ".json");
			}

			// パターン3: Thread ClassLoader
			if (is == null) {
				is = Thread.currentThread().getContextClassLoader()
						.getResourceAsStream("data/scenarios/" + scenarioId + ".json");
			}

			if (is == null) {
				System.err.println("[WARNING] シナリオファイルが見つかりません: " + scenarioId + ".json");
				continue;
			}

			try {
				// System.err.println("[INFO] シナリオ読み込み開始: " + scenarioId);

				InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
				Scenario scenario = gson.fromJson(reader, Scenario.class);

				if (scenario != null && scenario.getId() != null) {
					scenarios.put(scenario.getId(), scenario);
					// System.err.println("[SUCCESS] 読み込み成功: " + scenario.getId());
				} else {
					System.err.println("[WARNING] 読み込み失敗（IDなし）: " + scenarioId);
				}

			} catch (Exception e) {
				System.err.println("[ERROR] シナリオ読み込み中にエラーが発生しました: " + scenarioId);
				e.printStackTrace();
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch (IOException e) {
					// ignore
				}
			}
		}

		// System.out.println("[INFO] シナリオデータ読み込み完了: " + scenarios.size() + "種類");
	}

	/**
	 * シナリオを取得
	 */
	public Scenario getScenario(String id) {
		return scenarios.get(id);
	}

	/**
	 * マップを取得
	 */
	public GameMap getMap(String id) {
		return maps.get(id);
	}

	/**
	 * タグに一致するマップをランダムで取得
	 */
	public GameMap getRandomMapByTag(String tag) {
		List<GameMap> candidates = new ArrayList<>();
		for (GameMap map : maps.values()) {
			if (map.hasTag(tag)) {
				candidates.add(map);
			}
		}
		return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
	}

	/**
	 * フロア設定に基づいて地形（マップ）を選択
	 * 
	 * @param stageConfig フロアの設定（Scenario.StageConfig）
	 * @param gameState   現在のゲーム状態
	 * @return 選択された地形（GameMap）
	 */
	public GameMap getMapForArea(Scenario.StageConfig stageConfig, GameState gameState) {
		// System.err.println("[DEBUG][ScenarioManager] selectMap called");
		// System.err.println("[DEBUG][ScenarioManager] inheritMap flag: " +
		// (stageConfig != null ? stageConfig.isInheritMap() : "N/A"));
		// System.err.println("[DEBUG][ScenarioManager] current selected map: " +
		// (gameState.getCurrentMap() != null ? gameState.getCurrentMap().getId() :
		// "null"));

		if (stageConfig.isInheritMap()) {
			if (developerMode != null && developerMode.isDebugVisible()) {
				// System.err.println("[DEBUG] inheritMap=true → returning current selected
				// map");
			}
			return gameState.getCurrentMap(); // 継続使用
		}

		if (developerMode != null && developerMode.isDebugVisible()) {
			// System.err.println("[DEBUG] selectMap called. mapPool=" +
			// stageConfig.getMapPool());
		}

		List<GameMap> candidates = resolveMapPool(stageConfig.getMapPool());
		// System.err.println("[DEBUG][ScenarioManager] candidates.size: " +
		// candidates.size());

		if (developerMode != null && developerMode.isDebugVisible()) {
			// System.err.println("[DEBUG] resolveMapPool returned " + candidates.size() + "
			// candidates");
		}

		if (candidates.isEmpty()) {
			if (developerMode != null && developerMode.isDebugVisible()) {
				// System.err.println("[DEBUG] selectMap: no candidates → returning null");
			}
			return null;
		}

		GameMap selected = candidates.get(random.nextInt(candidates.size()));
		// System.err.println("[DEBUG][ScenarioManager] selected map: " +
		// (selected != null ? selected.getId() : "null"));

		if (developerMode != null && developerMode.isDebugVisible()) {
			// System.err.println("[DEBUG] selectMap: returning selected map = " +
			// selected.getId());
		}

		gameState.setCurrentMap(selected); // 抽選結果を記録

		return selected;
	}

	/**
	 * マップファイルを読み込む
	 */

	private void loadMaps() {
		List<String> mapFileNames = dataManager.getAllMapFileNamesRecursively("data/maps");

		// System.err.println("[INFO] マップファイル読み込み開始: " + mapFileNames.size() + "件");

		if (developerMode != null && developerMode.isDebugVisible()) {
			// System.out.println("[DEBUG] map fileNames found: " + mapFileNames);
		}

		int successCount = 0;
		for (String fileName : mapFileNames) {
			InputStream is = null;

			// ★★★ 複数のパターンを試す ★★★
			// パターン1: スラッシュあり、getResourceAsStream
			is = getClass().getResourceAsStream("/data/maps/" + fileName + ".json");

			// パターン2: スラッシュなし、ClassLoader
			if (is == null) {
				is = getClass().getClassLoader().getResourceAsStream("data/maps/" + fileName + ".json");
			}

			// パターン3: Thread ClassLoader
			if (is == null) {
				is = Thread.currentThread().getContextClassLoader()
						.getResourceAsStream("data/maps/" + fileName + ".json");
			}

			if (is == null) {
				System.err.println("[WARNING] マップファイルが見つかりません: " + fileName + ".json");
				continue;
			}

			try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				GameMap map = gson.fromJson(reader, GameMap.class);
				if (map != null) {
					if (developerMode != null && developerMode.isDebugVisible()) {
						// System.out.println("[DEBUG] loaded map ID: " + map.getId());
					}
					if (map.getId() != null) {
						maps.put(map.getId(), map);
						successCount++;
						// System.err.println("[SUCCESS] マップ読み込み成功: " + map.getId() + " (" + fileName +
						// ")");
					} else {
						System.err.println("[WARNING] map.getId() が null です。ファイル: " + fileName + ".json");
					}
				} else {
					System.err.println("[WARNING] GameMap が null です。ファイル: " + fileName + ".json");
				}
			} catch (IOException e) {
				System.err.println("[ERROR] マップ読み込みエラー: " + fileName + ".json");
				e.printStackTrace();
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch (IOException e) {
					// ignore
				}
			}
		}

		// System.err.println("[INFO] マップデータ読み込み完了: " + successCount + "/" +
		// mapFileNames.size() + " 成功");
	}

	/**
	 * すべてのシナリオIDを取得
	 */
	public List<String> getAllScenarioIds() {
		return new ArrayList<>(scenarios.keySet());
	}

	/**
	 * すべてのシナリオを取得
	 */
	public List<Scenario> getAllScenarios() {
		return new ArrayList<>(scenarios.values());
	}

	/**
	 * すべてのマップを取得
	 */
	public List<GameMap> getAllMaps() {
		return new ArrayList<>(maps.values());
	}

	/**
	 * mapPoolの記述（folder:xxx, fixed:xxx, 通常ID）を展開して、使用可能なGameMapリストに変換する
	 */
	public List<GameMap> resolveMapPool(List<String> mapPoolRaw) {
		if (developerMode != null && developerMode.isDebugVisible()) {
			// System.err.println("[DEBUG] resolveMapPool called on: " + this);
		}
		List<GameMap> result = new ArrayList<>();

		for (String entry : mapPoolRaw) {
			if (developerMode != null && developerMode.isDebugVisible()) {
				// System.err.println("[DEBUG] processing entry: " + entry);
			}
			if (entry.startsWith("folder:")) {
				String folderName = entry.substring("folder:".length());

				if (developerMode != null && developerMode.isDebugVisible()) {
					// System.err.println("[DEBUG] resolveMapPool: folder=" + folderName);
				}

				// マップキャッシュから、指定されたタグを持つマップをフィルタリング
				for (GameMap map : maps.values()) {
					if (map.hasTag(folderName)) {
						result.add(map);
						if (developerMode != null && developerMode.isDebugVisible()) {
							// System.err.println("[DEBUG] added map with tag " + folderName + ": " +
							// map.getId());
						}
					}
				}
			} else if (entry.startsWith("fixed:")) {
				String mapId = entry.substring("fixed:".length());
				GameMap map = getMap(mapId);
				if (developerMode != null && developerMode.isDebugVisible()) {
					// System.err.println("[DEBUG] getMap(" + mapId + ") → " + map);
				}
				if (map != null)
					result.add(map);
			} else {
				GameMap map = getMap(entry);
				if (developerMode != null && developerMode.isDebugVisible()) {
					// System.err.println("[DEBUG] getMap(" + entry + ") → " + map);
				}
				if (map != null)
					result.add(map);
			}
		}

		return result;
	}

	/**
	 * mapPoolの中から最初に使えるマップIDを取り出す（fixed:xxx や通常IDを優先）
	 */
	public String getFirstMapId(List<String> mapPoolRaw) {
		for (String entry : mapPoolRaw) {
			if (entry.startsWith("fixed:")) {
				return entry.substring("fixed:".length());
			} else if (!entry.startsWith("folder:")) {
				return entry;
			}
		}
		return null;
	}

}