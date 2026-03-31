package com.kh.tbrr.core;

import java.util.List;

import com.kh.tbrr.data.models.GameEvent;
import com.kh.tbrr.data.models.GameMap;
import com.kh.tbrr.data.models.Item;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.data.models.Scenario;
import com.kh.tbrr.event.EventProcessor;
import com.kh.tbrr.manager.DataManager;
import com.kh.tbrr.manager.DeathManager;
import com.kh.tbrr.manager.EventManager;
import com.kh.tbrr.manager.PersonalityManager;
import com.kh.tbrr.manager.RandomWordsManager;
import com.kh.tbrr.manager.ScenarioManager;
import com.kh.tbrr.system.CharacterLoader;
import com.kh.tbrr.system.DeveloperMode;
import com.kh.tbrr.ui.GameUI;
import com.kh.tbrr.utils.TextReplacer;

/**
 * ゲームエンジン
 */
public class GameEngine {
	// コアシステム
	private GameUI ui; // ConsoleUI から GameUI に変更
	private DataManager dataManager;

	// 開発者モード用
	private DeveloperMode developerMode;

	// ゲーム状態
	private Player player;
	private GameState gameState;

	// サブシステム
	// private CharacterCreator characterCreator; コメントアウト
	private EventManager eventManager;
	private EventProcessor eventProcessor;
	private DeathManager deathManager;

	// シナリオ管理
	private PersonalityManager personalityManager;
	private ScenarioManager scenarioManager;
	private RandomWordsManager randomWordsManager;
	private com.kh.tbrr.manager.ImageManager imageManager;
	private com.kh.tbrr.manager.AudioManager audioManager;

	// 現在のシナリオ
	private Scenario currentScenario;

	/**
	 * コンストラクタ
	 */
	public GameEngine(DeveloperMode developerMode, GameUI ui, DataManager dataManager) {
		this.developerMode = developerMode;
		this.ui = ui;
		this.dataManager = dataManager;

		this.developerMode.setUI(ui);

		for (Item item : dataManager.loadAllItemsFromFile("common_items.json")) {
			com.kh.tbrr.data.ItemRegistry.register(item);
		}
		for (Item item : dataManager.loadAllItemsFromFile("magic_items.json")) {
			com.kh.tbrr.data.ItemRegistry.register(item);
		}
		for (Item item : dataManager.loadAllItemsFromFile("unique_items.json")) {
			com.kh.tbrr.data.ItemRegistry.register(item);
		}
		for (Item item : dataManager.loadAllItemsFromFile("common_weapons.json")) {
			com.kh.tbrr.data.ItemRegistry.register(item);
		}
		for (Item item : dataManager.loadAllItemsFromFile("rare_weapons.json")) {
			com.kh.tbrr.data.ItemRegistry.register(item);
		}
		for (Item item : dataManager.loadAllItemsFromFile("epic_weapons.json")) {
			com.kh.tbrr.data.ItemRegistry.register(item);
		}
		for (Item item : dataManager.loadAllItemsFromFile("legendary_weapons.json")) {
			com.kh.tbrr.data.ItemRegistry.register(item);
		}
		for (Item item : dataManager.loadAllItemsFromFile("accessories.json")) {
			com.kh.tbrr.data.ItemRegistry.register(item);
		}
		
		// --- パッシブデータのロード ---
		try (java.io.InputStream is = getClass().getResourceAsStream("/data/passives/basic_passives.json")) {
			if (is != null) {
				java.io.Reader reader = new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8);
				java.util.List<com.kh.tbrr.data.models.PassiveData> passives = new com.google.gson.Gson().fromJson(reader, 
				        new com.google.gson.reflect.TypeToken<java.util.List<com.kh.tbrr.data.models.PassiveData>>(){}.getType());
				for (com.kh.tbrr.data.models.PassiveData p : passives) {
					com.kh.tbrr.data.PassiveRegistry.register(p);
				}
			} else {
				System.err.println("Warning: basic_passives.json not found in resources!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// イベントデータのロード登録
		for (com.kh.tbrr.data.models.StatusEffect effect : dataManager
				.loadAllStatusEffectsFromFile("common_status_effects.json")) {
			com.kh.tbrr.data.StatusEffectRegistry.register(effect);
		}
		for (com.kh.tbrr.data.models.StatusEffect effect : dataManager
				.loadAllStatusEffectsFromFile("unique_status_effects.json")) {
			com.kh.tbrr.data.StatusEffectRegistry.register(effect);
		}

		// 管理クラス初期化
		this.personalityManager = new PersonalityManager();
		this.scenarioManager = new ScenarioManager(dataManager, developerMode);

		// 追加: RandomWordsManagerの初期化とTextReplacerへの設定
		this.randomWordsManager = new RandomWordsManager();
		TextReplacer.setRandomWordsManager(randomWordsManager);
		// ImageManagerの初期化
		this.imageManager = new com.kh.tbrr.manager.ImageManager();
		// AudioManagerの初期化
		this.audioManager = new com.kh.tbrr.manager.AudioManager();

		// サブシステム初期化（UI が必要なものは ui を渡す）
		this.deathManager = new DeathManager(ui, dataManager);
		this.eventProcessor = new EventProcessor(ui, deathManager, developerMode, dataManager, audioManager);
		this.eventManager = new EventManager(dataManager, eventProcessor, scenarioManager, developerMode);

		// インタラクションシステム初期化
		com.kh.tbrr.interaction.InteractionRegistry.initialize();
	}

	/**
	 * 新規ゲーム開始
	 */
	public void startNewGame(String scenarioId) {

		dataManager.clearEventCache();
		this.currentScenario = dataManager.loadScenario(scenarioId);
		if (currentScenario == null) {
			ui.printError("シナリオが見つかりません: " + scenarioId);
			return;
		}

		ui.printTitleBar("キャラクター読み込み");

		CharacterLoader loader = new CharacterLoader();
		var files = loader.listSavedCharacters();
		if (files.isEmpty()) {
			ui.print("保存済みキャラが見つかりません。先にキャラクター編集で作成してください。");
			ui.waitForEnter();
			return;
		}

		ui.print("保存済みキャラ一覧:");
		for (int i = 0; i < files.size(); i++) {
			ui.print((i + 1) + ". " + files.get(i));
		}
		ui.print("選択 (1-" + files.size() + "): ");
		int idx = ui.getPlayerChoice(files.size());

		if (idx < 1 || idx > files.size()) {
			ui.print("無効な選択です。");
			return;
		}

		Player loaded = loader.loadCharacter(files.get(idx - 1));
		if (loaded == null) {
			ui.print("読み込みに失敗しました。");
			return;
		}

		this.player = loaded;

		// 🔧 テストシナリオの場合はキャラ読み込み後に開発者モードをONにする
		if (scenarioId.equals("test_scenario")) {

		}

		this.gameState = new GameState();
		gameState.setCurrentPlayer(player); // ★追加
		gameState.setCurrentScenario(scenarioId);
		gameState.setMaxFloor(currentScenario.getTotalFloors());

		// フロアを0から開始
		gameState.setCurrentFloor(0);

		showPrologue();
		gameLoop();
	}

	/**
	 * 新規ゲーム開始（プレイヤー指定版）
	 * GUI版でキャラクター選択後に使用
	 */
	public void startNewGameWithPlayer(String scenarioId, Player player) {
		dataManager.clearEventCache();
		this.currentScenario = dataManager.loadScenario(scenarioId);
		if (currentScenario == null) {
			ui.printError("シナリオが見つかりません: " + scenarioId);
			return;
		}

		this.player = player;

		this.gameState = new GameState();
		gameState.setCurrentPlayer(player); // ★追加: GameStateにプレイヤーをセット
		gameState.setCurrentScenario(scenarioId);

		// フロア設定数をフロア数とする
		int totalFloors = currentScenario.getStageConfigs().size();
		gameState.setMaxFloor(totalFloors);

		// フロアを0から開始
		gameState.setCurrentFloor(0);

		showPrologue();
		gameLoop();
	}

	/**
	 * ゲーム再開（中断データから）
	 */
	public void resumeGame(GameState state) {
		this.gameState = state;
		this.player = state.getCurrentPlayer();

		String scenarioId = state.getCurrentScenario();
		this.currentScenario = dataManager.loadScenario(scenarioId);
		if (currentScenario == null) {
			ui.printError("シナリオが見つかりません: " + scenarioId);
			return;
		}

		// マップ情報の復元（IDからロード）
		if (state.getCurrentMap() != null && state.getCurrentMap().getId() != null) {
			// DataManager.loadMapはサブフォルダに対応していない可能性があるため、
			// 全マップをロード済みのScenarioManagerから取得する
			GameMap map = scenarioManager.getMap(state.getCurrentMap().getId());
			if (map != null) {
				state.setCurrentMap(map);
			} else {
				// マップが見つからない場合のフォールバック（IDのみ保持）
				// またはエラーログを出力
				System.err.println(
						"[ERROR] ResumeGame: Map not found in ScenarioManager: " + state.getCurrentMap().getId());
			}

			// 背景画像の復元
			if (state.getCurrentBackgroundImage() != null) {
				ui.showImage("background", state.getCurrentBackgroundImage());
			}
			if (state.getCurrentSubImage() != null) {
				ui.showImage("sub", state.getCurrentSubImage());
			}
		}

		// イベント情報の復元（IDからロード）
		if (state.getCurrentEvent() != null && state.getCurrentEvent().getId() != null) {
			GameEvent event = dataManager.loadEvent(state.getCurrentEvent().getId());
			state.setCurrentEvent(event);
		}
		// ゲームループ再開
		gameLoop();
	}

	/**
	 * プロローグ表示
	 */
	private void showPrologue() {
		String prologueId = currentScenario.getPrologue();
		if (prologueId != null && !prologueId.isEmpty()) {
			// プロローグ中はフロア表示を「プロローグ」にする
			ui.showFloorInfo(-1, "---");

			ui.printTitleBar(currentScenario.getName());

			// プロローグがイベントIDの場合は、そのイベントを再生
			GameEvent prologueEvent = dataManager.loadEvent(prologueId);
			if (prologueEvent != null) {
				eventProcessor.processEvent(prologueEvent, player, gameState);
			} else {
				// イベントが見つからない場合は、テキストとして表示
				ui.print(prologueId);
				ui.print("");
				ui.waitForEnter();
			}
		}
	}

	/**
	 * エピローグ表示
	 */
	private void showEpilogue() {
		String epilogueId = currentScenario.getEpilogue();
		if (epilogueId != null && !epilogueId.isEmpty()) {
			// エピローグがイベントIDの場合は、そのイベントを再生
			GameEvent epilogueEvent = dataManager.loadEvent(epilogueId);
			if (epilogueEvent != null) {
				eventProcessor.processEvent(epilogueEvent, player, gameState);
			} else {
				// イベントが見つからない場合は、テキストとして表示
				ui.print(epilogueId);
				ui.print("");
				ui.waitForEnter();
			}
		}
	}

	/**
	 * ゲームループ
	 */
	private void gameLoop() {
		while (!gameState.isGameOver()) {
			int currentFloor = gameState.getCurrentFloor();

			// 勝利判定：全フロアをクリアしたか
			if (currentFloor >= currentScenario.getStageConfigs().size()) {
				victory();
				return;
			}

			// 現在のフロアに対応する設定を取得
			Scenario.StageConfig stageConfig = currentScenario.getStageConfigByFloor(currentFloor);
			if (stageConfig == null) {
				ui.printError("フロア設定が見つかりません: フロア " + currentFloor);
				break;
			}

			// 地形（マップ）を選択（フロア情報表示の前に実行）
			// 中断再開時など、既にマップが設定されている場合はそれを使用する
			GameMap selectedTerrain = gameState.getCurrentMap();
			if (selectedTerrain == null) {
				selectedTerrain = selectMap();
				gameState.setCurrentMap(selectedTerrain);

				// 背景画像をランダムに選択（新規選択時のみ）
				if (selectedTerrain != null) {
					initializeMapImages(selectedTerrain);
				}
			} else {
				// 既存マップがある場合でも、画像が未設定なら初期化（念のため）
				if (gameState.getCurrentBackgroundImage() == null) {
					initializeMapImages(selectedTerrain);
				}
			}

			if (selectedTerrain == null) {
				ui.printError("地形が見つかりません。");
				break;
			}
			if (gameState.isGameOver())
				return;

			// フロア情報表示（地形選択後に実行）
			showFloorInfo();
			if (gameState.isGameOver())
				return;

			// マップ入場イベント処理（マップが変わった場合のみ）
			processMapEntryEvent(stageConfig, selectedTerrain);
			if (gameState.isGameOver())
				return;

			// イベント処理
			processFloorEvent(selectedTerrain);
			if (gameState.isGameOver())
				return;

			// アナザーエンディングチェック
			if (gameState.hasAlternateEnding()) {
				processAlternateEnding();
				return;
			}

			// デバッグ出力追加
			System.err.println("[GAME_LOOP] イベント処理後 - alternateEndingId: " + gameState.getAlternateEndingId());
			System.err.println("[GAME_LOOP] hasAlternateEnding: " + gameState.hasAlternateEnding());

			if (gameState.isGameOver())
				return;

			// フロア進行
			advanceFloor();
			if (gameState.isGameOver())
				return;
		}

		endGame();
	}

	/**
	 * フロア情報表示
	 */
	private void showFloorInfo() {
		// 現在のフロア番号と選ばれたマップ（地形）を表示
		int currentFloor = gameState.getCurrentFloor();
		GameMap selectedMap = gameState.getCurrentMap();

		String terrainName = (selectedMap != null && selectedMap.getName() != null)
				? selectedMap.getName()
				: "???";

		// GUIでは専用エリアに表示、コンソールではテキストとして表示
		// 内部的には0始まりだが、表示上は1始まりにする
		ui.showFloorInfo(currentFloor + 1, terrainName);

		// プレイヤーのステータス（HP/AP/スキル/アイテム）を完全表示
		ui.printPlayerStatus(player);

		// ※区切り線はprintPlayerStatus()内で表示されるので不要
	}

	/**
	 * 地形（マップ）を選択
	 * ScenarioManagerに現在のフロアに対応する地形を選択させる
	 */
	private GameMap selectMap() {
		var stageConfig = currentScenario.getStageConfigByFloor(gameState.getCurrentFloor());

		GameMap selectedTerrain = null;

		// ★新規: プレイヤー選択が有効か?
		if (stageConfig.isAllowPlayerChoice() &&
				stageConfig.getMapChoices() != null &&
				!stageConfig.getMapChoices().isEmpty()) {

			selectedTerrain = selectMapByPlayerChoice(stageConfig);
		} else {
			// 既存の処理(ランダム選択)
			selectedTerrain = scenarioManager.getMapForArea(stageConfig, gameState);
		}

		gameState.setCurrentMap(selectedTerrain);

		if (selectedTerrain == null) {
			ui.printError("[エラー]地形が見つかりません。");
			if (developerMode != null && developerMode.isDebugVisible()) {
				ui.print("[DEBUG] 地形選定に失敗しました。mapPool=" + stageConfig.getMapPool());
			}
			ui.printError("リソースファイルが正しく読み込まれていない可能性があります。");
			ui.printError("プロジェクトをクリーン&ビルドしてから再実行してください。");
		}

		return selectedTerrain;
	}

	/**
	 * プレイヤーにマップを選択させる
	 * 
	 * @param stageConfig フロア設定
	 * @return 選択されたマップ
	 */
	private GameMap selectMapByPlayerChoice(Scenario.StageConfig stageConfig) {
		ui.print("");
		ui.print("━━━━━━━━━━━━━━━━━━━━");
		ui.print("あなたの辿り着いたのは...");
		ui.print("");

		List<Scenario.StageConfig.MapChoice> choices = stageConfig.getMapChoices();

		// 選択肢を表示
		for (int i = 0; i < choices.size(); i++) {
			Scenario.StageConfig.MapChoice choice = choices.get(i);
			ui.print((i + 1) + ". " + choice.getDisplayName());
			if (choice.getDescription() != null && !choice.getDescription().isEmpty()) {
				ui.print("   " + choice.getDescription());
			}
			ui.print("");
		}

		ui.print("━━━━━━━━━━━━━━━━━━━━");

		// プレイヤーの選択を取得
		int selection = ui.getPlayerChoice(choices.size());

		if (selection < 1 || selection > choices.size()) {
			ui.printError("無効な選択です。最初の選択肢を使用します。");
			selection = 1;
		}

		Scenario.StageConfig.MapChoice selected = choices.get(selection - 1);

		ui.print("");
		ui.print("【" + selected.getDisplayName() + "】へ向かった。");
		ui.print("");
		ui.waitForEnter();

		// マップを読み込み
		return dataManager.loadMap(selected.getMapId());
	}

	/**
	 * フロアイベント処理
	 */
	private boolean processFloorEvent(GameMap map) {
		// ★★★ ここから追加 ★★★
		if (map == null) {
			ui.printError("地形が選択できませんでした。ゲームを続行できません。");
			return false;
		}

		if (developerMode != null) {
			developerMode.setCurrentPlayer(player);
		}

		Scenario.StageConfig currentStageConfig = currentScenario.getStageConfigByFloor(gameState.getCurrentFloor());
		if (currentStageConfig == null) {
			ui.printError("エリアが見つかりません。");
			return true;
		}

		// イベント発生処理（固定イベント・マップイベント・フォールバック含む）
		eventManager.triggerRandomEvent(currentStageConfig, player, gameState);

		return true;
	}

	/**
	 * マップ入場イベント処理
	 * マップが変わった時に、入場イベントを再生する
	 */
	private void processMapEntryEvent(Scenario.StageConfig stageConfig, GameMap map) {
		// inheritMap = true の場合、マップは変わっていないので入場イベントは再生しない
		if (stageConfig.isInheritMap()) {
			return;
		}

		// シナリオで抑制されている場合は何も再生しない
		if (stageConfig.isSuppressMapEntryEvent()) {
			if (developerMode != null && developerMode.isDebugVisible()) {
				ui.print("[DEBUG] マップ入場イベントが抑制されています");
			}
			return;
		}

		// 再生するイベントIDを決定
		String entryEventId = null;

		// 優先度1: シナリオで上書きされている場合
		if (stageConfig.getMapEntryEventOverride() != null && !stageConfig.getMapEntryEventOverride().isEmpty()) {
			entryEventId = stageConfig.getMapEntryEventOverride();
			if (developerMode != null && developerMode.isDebugVisible()) {
				ui.print("[DEBUG] マップ入場イベント（上書き）: " + entryEventId);
			}
		}
		// 優先度2: マップにデフォルト入場イベントがある場合
		else if (map != null && map.getEntryEventId() != null && !map.getEntryEventId().isEmpty()) {
			entryEventId = map.getEntryEventId();
			if (developerMode != null && developerMode.isDebugVisible()) {
				ui.print("[DEBUG] マップ入場イベント（デフォルト）: " + entryEventId);
			}
		}

		// イベントIDがない場合は何もしない
		if (entryEventId == null) {
			return;
		}

		// イベントを読み込んで再生
		GameEvent entryEvent = dataManager.loadEvent(entryEventId);
		if (entryEvent != null) {
			if (developerMode != null && developerMode.isDebugVisible()) {
				ui.print("[DEBUG] マップ入場イベントを再生: " + entryEventId);
			}
			eventProcessor.processEvent(entryEvent, player, gameState);
		} else {
			if (developerMode != null && developerMode.isDebugVisible()) {
				ui.print("[DEBUG] マップ入場イベントが見つかりません: " + entryEventId);
			}
		}
	}

	/**
	 * フロア進行
	 */
	private void advanceFloor() {
		gameState.advanceFloor(); // currentFloor++

		// 次のエリアがinheritMapでない場合、マップをクリア
		int nextFloor = gameState.getCurrentFloor();
		if (nextFloor < currentScenario.getStageConfigs().size()) {
			Scenario.StageConfig nextStageConfig = currentScenario.getStageConfigByFloor(nextFloor);
			if (nextStageConfig != null && !nextStageConfig.isInheritMap()) {
				gameState.setCurrentMap(null);
			}
		}

		// フロア進行時に表情を基本表情にリセット
		ui.resetPortraitExpression();
	}

	/**
	 * アナザーエンディング処理
	 */
	private void processAlternateEnding() {
		String endingId = gameState.getAlternateEndingId();

		ui.printTitleBar("アナザーエンディング");
		ui.print("");

		// エンディングイベントを再生
		if (endingId != null && !endingId.isEmpty()) {
			GameEvent endingEvent = dataManager.loadEvent(endingId);
			if (endingEvent != null) {
				eventProcessor.processEvent(endingEvent, player, gameState);
			} else {
				ui.print("エンディングイベントが見つかりません: " + endingId);
			}
		}

		ui.print("");
		showStatistics();

		gameState.setGameOver(true);
		gameState.setAlternateEndingId(null);
	}

	/**
	 * 勝利エンド
	 */
	private void victory() {
		ui.printTitleBar("クリア!");
		ui.print("");

		gameState.setVictory(true);

		ui.print(player.getName() + "は冒険を成し遂げた!");
		ui.print("");

		// エピローグを表示
		showEpilogue();

		showStatistics();
	}

	/**
	 * ゲーム終了処理
	 */
	private void endGame() {
		if (gameState.isGameOver())
			return;

		if (!gameState.isVictory()) {
			boolean canContinue = deathManager.checkFatedOne(player);
			if (!canContinue) {
				ui.print("このキャラクターは墓地に送られます...");
			}
		}

		ui.waitForEnter();

		if (developerMode != null) {
			String devInput = ui.getInput("開発者コマンド（Enterでスキップ）: ");
			if (devInput != null && !devInput.trim().isEmpty()) {
				developerMode.handleDevCommand(devInput, player);
			}
		}
	}

	/**
	 * 統計表示
	 */
	private void showStatistics() {
		ui.printSeparator();
		ui.print("【冒険の記録】");
		ui.print("到達フロア: " + gameState.getCurrentFloor());
		ui.print("討伐数: " + gameState.getCounter("monsters_defeated"));
		ui.print("発見アイテム数: " + gameState.getCounter("items_found"));
		ui.print("イベント達成数: " + gameState.getCounter("events_completed"));
		ui.printSeparator();
		ui.print("");
		// 追加: ユーザーにエンター入力を待機
		ui.waitForEnter();
	}

	/**
	 * シャットダウン
	 */
	public void shutdown() {
		ui.print("ゲームを終了します。");
		if (audioManager != null) {
			audioManager.dispose();
		}
		ui.close();
	}

	// ゲッター
	public Player getPlayer() {
		return player;
	}

	public GameState getGameState() {
		return gameState;
	}

	/**
	 * マップ画像を初期化
	 * マップの背景画像リストからランダムに選択し、サブ画像も設定
	 */
	private void initializeMapImages(GameMap map) {
		if (map == null) {
			return;
		}

		// 背景画像の処理
		String selectedBgImage = null;

		// まずbackgroundImagesリストから選択を試みる
		if (map.getBackgroundImages() != null && !map.getBackgroundImages().isEmpty()) {
			selectedBgImage = imageManager.selectRandomBackgroundFileName(map.getBackgroundImages());
		}
		// backgroundImagesが無い場合は、単一のbackgroundImageを使用（後方互換性）
		else if (map.getBackgroundImage() != null && !map.getBackgroundImage().isEmpty()) {
			selectedBgImage = map.getBackgroundImage();
		}

		// 選択された背景画像をGameStateに保存し、UIに表示
		if (selectedBgImage != null) {
			gameState.setCurrentBackgroundImage(selectedBgImage);
			ui.showImage("background", selectedBgImage);

			if (developerMode != null && developerMode.isDebugVisible()) {
				ui.print("[DEBUG] 背景画像を設定: " + selectedBgImage);
			}
		}

		// サブ画像の処理
		if (map.getSubImage() != null && !map.getSubImage().isEmpty()) {
			gameState.setCurrentSubImage(map.getSubImage());
			ui.showImage("sub", map.getSubImage());

			if (developerMode != null && developerMode.isDebugVisible()) {
				ui.print("[DEBUG] サブ画像を設定: " + map.getSubImage());
			}
		} else {
			// サブ画像が指定されていない場合はクリア
			gameState.setCurrentSubImage(null);
			ui.showImage("sub", null);
		}
	}

	/**
	 * イベント後に画像をリセット
	 * イベントで画像変更がなかった場合、基本背景に戻す
	 */
	private void resetEventImages() {
		GameMap currentMap = gameState.getCurrentMap();
		if (currentMap == null) {
			return;
		}

		// 現在の背景画像がマップの背景画像リストに含まれていない場合、
		// ランダムに新しい背景を選択
		String currentBg = gameState.getCurrentBackgroundImage();
		boolean needsReset = true;

		if (currentBg != null && currentMap.getBackgroundImages() != null) {
			needsReset = !currentMap.getBackgroundImages().contains(currentBg);
		}

		if (needsReset) {
			String newBgImage = null;
			if (currentMap.getBackgroundImages() != null && !currentMap.getBackgroundImages().isEmpty()) {
				newBgImage = imageManager.selectRandomBackgroundFileName(currentMap.getBackgroundImages());
			} else if (currentMap.getBackgroundImage() != null) {
				newBgImage = currentMap.getBackgroundImage();
			}

			if (newBgImage != null) {
				gameState.setCurrentBackgroundImage(newBgImage);
				ui.showImage("background", newBgImage);

				if (developerMode != null && developerMode.isDebugVisible()) {
					ui.print("[DEBUG] 背景画像をリセット: " + newBgImage);
				}
			}
		}

		// サブ画像もリセット（イベントで変更されていた場合）
		String currentSub = gameState.getCurrentSubImage();
		String mapSubImage = currentMap.getSubImage();

		if (currentSub != null && !currentSub.equals(mapSubImage)) {
			gameState.setCurrentSubImage(mapSubImage);
			ui.showImage("sub", mapSubImage);

			if (developerMode != null && developerMode.isDebugVisible()) {
				ui.print("[DEBUG] サブ画像をリセット: " + (mapSubImage != null ? mapSubImage : "(なし)"));
			}
		}
	}

}