package com.kh.tbrr.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.kh.tbrr.core.GameState;
import com.kh.tbrr.data.models.GameEvent;
import com.kh.tbrr.data.models.GameMap;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.data.models.Scenario;
import com.kh.tbrr.event.EventProcessor;
import com.kh.tbrr.system.DeveloperMode;

/**
 * イベント管理クラス (Week 6統合版)
 * イベントの抽選・選択・処理を担当
 */
public class EventManager {
	private DataManager dataManager;
	private DeveloperMode developerMode;
	private ScenarioManager scenarioManager;
	private EventProcessor eventProcessor;
	private Random random;

	public EventManager(DataManager dataManager, EventProcessor eventProcessor, ScenarioManager scenarioManager,
			DeveloperMode developerMode) {
		this.dataManager = dataManager;
		this.eventProcessor = eventProcessor;
		this.scenarioManager = scenarioManager;
		this.developerMode = developerMode;
		this.random = new Random();
	}

	/**
	 * イベントを選択 (GameEngineから呼ばれる)
	 * 
	 * @param eventPool イベントプール
	 * @param player    プレイヤー
	 * @param gameState ゲーム状態
	 * @return 選択されたイベントID
	 */
	private boolean canTriggerEvent(GameEvent event, Player player) {
		if (event.getRequiredItems() == null || event.getRequiredItems().isEmpty()) {
			return true;
		}
		for (String itemId : event.getRequiredItems()) {
			if (!player.hasItem(itemId)) {
				return false;
			}
		}
		return true;
	}

	public String selectEvent(List<String> eventPool, Player player, GameState gameState) {
		// 元のランダム選択（コメントアウト）
		// return eventPool.get(random.nextInt(eventPool.size()));

		// 山札が空なら再構築（再シャッフル）
		if (gameState.getRemainingEventDeck().isEmpty()) {
			gameState.setRemainingEventDeck(eventPool);
		}

		List<String> deck = gameState.getRemainingEventDeck();
		if (deck.isEmpty())
			return null;

		// ランダムに1枚引く
		String eventId = deck.get(random.nextInt(deck.size()));
		gameState.removeEventFromDeck(eventId); // 山札から除外

		return eventId;
	}

	/**
	 * イベントを処理 (GameEngineから呼ばれる)
	 * 
	 * @param eventId   イベントID
	 * @param player    プレイヤー
	 * @param gameState ゲーム状態
	 * @return 処理成功したか
	 */
	public boolean processEvent(String eventId, Player player, GameState gameState) {
		GameEvent event = dataManager.loadEvent(eventId);

		if (event == null) {
			System.err.println("[ERROR] イベントが見つかりません: " + eventId);
			return false;
		}

		// EventProcessorで実際の処理を実行
		eventProcessor.processEvent(event, player, gameState);

		return true;
	}

	/**
	 * ランダムイベントを発生させる
	 * 
	 * @param area      現在のエリア
	 * @param player    プレイヤー
	 * @param gameState ゲーム状態
	 */
	public void triggerRandomEvent(Scenario.StageConfig area, Player player, GameState gameState) {
		int floor = gameState.getCurrentFloor();

		// ★追加: 1. グローバル強制イベントをチェック（最優先）
		String forcedEventId = checkGlobalForcedEvents(player, gameState);
		if (forcedEventId != null) {
			if (developerMode != null && developerMode.isDebugVisible()) {
				System.err.println("[DEBUG] Global forced event triggered: " + forcedEventId);
			}
			triggerEvent(forcedEventId, player, gameState);
			return;
		}

		// 2. fixedEventsのキーをString型で確認
		if (area.getFixedEvents() != null && area.getFixedEvents().containsKey(String.valueOf(floor))) {
			String fixedEventId = area.getFixedEvents().get(String.valueOf(floor));

			if (developerMode != null && developerMode.isDebugVisible()) {
				System.err.println("[DEBUG] Fixed event triggered for floor " + floor + ": " + fixedEventId);
			}

			triggerEvent(fixedEventId, player, gameState);
			return;
		}

		// ★修正: 3. イベントプールを構築（グローバル条件付きイベント含む）
		List<String> eventPool = buildEventPool(area, player, gameState);

		if (eventPool == null || eventPool.isEmpty()) {
			if (developerMode != null && developerMode.isDebugVisible()) {
				System.err.println("[DEBUG] No eventPool available, using fallback");
			}
			triggerFallbackEvent(player, gameState);
			return;
		}

		// イベントプールIDを生成し、変わった場合はデッキをリセット
		String eventPoolId = generateEventPoolId(area, gameState.getCurrentMap(), eventPool);
		gameState.setCurrentEventPoolId(eventPoolId);

		if (developerMode != null && developerMode.isDebugVisible()) {
			System.out.println("[DEBUG] EventPoolId: " + eventPoolId);
			System.out.println("[DEBUG] Current eventPool: " + eventPool);
			System.out.println("[DEBUG] Remaining deck: " + gameState.getRemainingEventDeck());
		}

		// 山札式でイベントIDを選択
		String eventId = selectEvent(eventPool, player, gameState);
		if (eventId == null) {
			triggerFallbackEvent(player, gameState);
			return;
		}

		GameEvent event = dataManager.loadEvent(eventId);
		if (event != null && canTriggerEvent(event, player)) {
			eventProcessor.processEvent(event, player, gameState);
		} else {
			triggerFallbackEvent(player, gameState);
		}
	}

	/**
	 * タグベースでイベントを発生させる
	 */
	private void triggerEventByTags(List<String> tags, Player player, GameState gameState) {
		if (tags == null || tags.isEmpty()) {
			triggerFallbackEvent(player, gameState);
			return;
		}

		// タグに一致するイベントを検索
		List<GameEvent> matchingEvents = dataManager.getEventsByTags(tags);

		if (!matchingEvents.isEmpty()) {
			GameEvent event = matchingEvents.get(random.nextInt(matchingEvents.size()));
			eventProcessor.processEvent(event, player, gameState);
		} else {
			// 個別タグで再検索
			for (String tag : tags) {
				List<GameEvent> events = dataManager.getEventsByTag(tag);
				if (!events.isEmpty()) {
					GameEvent event = events.get(random.nextInt(events.size()));
					eventProcessor.processEvent(event, player, gameState);
					return;
				}
			}

			// それでも見つからない場合
			triggerFallbackEvent(player, gameState);
		}
	}

	/**
	 * 特定のイベントを発生させる
	 */
	public void triggerEvent(String eventId, Player player, GameState gameState) {
		GameEvent event = dataManager.loadEvent(eventId);

		if (event != null) {
			eventProcessor.processEvent(event, player, gameState);
		} else {
			triggerFallbackEvent(player, gameState);
		}
	}

	/**
	 * フォールバックイベント (ダミーイベント)
	 */
	private void triggerFallbackEvent(Player player, GameState gameState) {
		// ダミーイベントを生成
		GameEvent fallback = new GameEvent();
		fallback.setId("fallback_event");
		fallback.setTitle("何もない通路");

		List<String> desc = new ArrayList<>();
		desc.add("あなたは何もない通路を進んだ。");
		desc.add("特に何も起こらなかった。");
		fallback.setDescription(desc);

		// 選択肢1つ
		GameEvent.Choice choice = new GameEvent.Choice();
		choice.setText("先に進む");
		choice.setSuccessRate(100);

		GameEvent.Result result = new GameEvent.Result();
		List<String> resultDesc = new ArrayList<>();
		resultDesc.add("あなたは前に進んだ。");
		result.setDescription(resultDesc);
		choice.setSuccess(result);

		List<GameEvent.Choice> choices = new ArrayList<>();
		choices.add(choice);
		fallback.setChoices(choices);

		eventProcessor.processEvent(fallback, player, gameState);
	}

	/**
	 * システムイベントを発生させる
	 */
	public void triggerSystemEvent(String systemEventType, Player player, GameState gameState) {
		List<GameEvent> systemEvents = dataManager.getEventsByTag("system");

		// 指定されたタイプのイベントを検索
		for (GameEvent event : systemEvents) {
			if (event.getTags().contains(systemEventType)) {
				eventProcessor.processEvent(event, player, gameState);
				return;
			}
		}

		// 見つからない場合はフォールバック
		triggerFallbackEvent(player, gameState);
	}

	/**
	 * ボス戦イベントを発生させる
	 */
	public void triggerBossEvent(String bossId, Player player, GameState gameState) {
		List<GameEvent> bossEvents = dataManager.getEventsByTag("boss");

		for (GameEvent event : bossEvents) {
			if (event.isBossEvent() && event.getBossId().equals(bossId)) {
				eventProcessor.processEvent(event, player, gameState);
				return;
			}
		}

		// 見つからない場合はフォールバック
		triggerFallbackEvent(player, gameState);
	}

	/**
	 * イベントプールIDを生成
	 * マップIDやイベントプールの内容からユニークなIDを作る
	 */
	private String generateEventPoolId(Scenario.StageConfig area, GameMap map, List<String> eventPool) {
		if (eventPool == null || eventPool.isEmpty()) {
			return "empty";
		}

		// マップIDとイベントプールの組み合わせでIDを生成
		StringBuilder id = new StringBuilder();
		if (map != null) {
			id.append("map:").append(map.getId()).append("|");
		}
		id.append("events:").append(String.join(",", eventPool));

		return id.toString();
	}

	/**
	 * グローバル強制イベントをチェック
	 * 条件を満たす最優先のイベントIDを返す
	 */
	private String checkGlobalForcedEvents(Player player, GameState gameState) {
		// 現在のシナリオIDを取得
		String scenarioId = gameState.getCurrentScenario();
		if (scenarioId == null || scenarioId.isEmpty()) {
			return null;
		}

		// ScenarioManagerからScenarioオブジェクトを取得
		Scenario scenario = scenarioManager.getScenario(scenarioId);
		if (scenario == null || scenario.getGlobalForcedEvents() == null
				|| scenario.getGlobalForcedEvents().isEmpty()) {
			return null;
		}

		// 優先度順にソート（降順）
		List<Scenario.ForcedEventEntry> sorted = new ArrayList<>(scenario.getGlobalForcedEvents());
		sorted.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

		// 条件を満たす最初のイベントを返す
		for (Scenario.ForcedEventEntry entry : sorted) {
			if (eventProcessor.matchesCondition(entry.getCondition(), player, gameState)) {
				return entry.getEventId();
			}
		}

		return null;
	}

	/**
	 * イベントプールを構築
	 * 基本プール + グローバル条件付きイベントを含む
	 */
	private List<String> buildEventPool(Scenario.StageConfig area, Player player, GameState gameState) {
		List<String> pool = new ArrayList<>();

		// 1. 基本イベントプールを追加
		if (area.getEventPool() != null) {
			pool.addAll(area.getEventPool());
		}

		// 2. GameMapのイベントプールも追加
		GameMap currentMap = gameState.getCurrentMap();
		if (currentMap != null && currentMap.getEventPool() != null) {
			pool.addAll(currentMap.getEventPool());
		}

		// 3. グローバル条件付きイベントをチェックして追加
		String scenarioId = gameState.getCurrentScenario();
		if (scenarioId != null && !scenarioId.isEmpty()) {
			// ScenarioManagerからScenarioオブジェクトを取得
			Scenario scenario = scenarioManager.getScenario(scenarioId);
			if (scenario != null && scenario.getGlobalConditionalEvents() != null) {
				for (Scenario.ConditionalEventEntry entry : scenario.getGlobalConditionalEvents()) {
					if (eventProcessor.matchesCondition(entry.getCondition(), player, gameState)) {
						pool.add(entry.getEventId());
						if (developerMode != null && developerMode.isDebugVisible()) {
							System.err.println("[DEBUG] Conditional event added to pool: " + entry.getEventId());
						}
					}
				}
			}
		}

		return pool;
	}

	// ========== Static Access for SaveManager ==========

	/**
	 * IDからイベントを取得する（静的アクセス用）
	 * SaveManagerから利用されることを想定
	 * 注意: DataManagerのインスタンスが必要なため、
	 * アプリケーション起動時にインスタンスを登録する仕組みが必要。
	 * ここでは簡易的に、DataManagerがシングルトン的に振る舞うか、
	 * あるいはロード時に一時的にダミーを返す実装とする。
	 * 
	 * 現状のアーキテクチャでは静的アクセスが難しいため、
	 * SaveManager側で「IDだけ保持したダミーイベント」を返し、
	 * 実際のゲーム再開時にイベントを再ロードする設計を推奨するが、
	 * GameStateがGameEventオブジェクトを保持しているため、
	 * ここでは「IDだけ入ったGameEvent」を返す。
	 */
	public static GameEvent getEventById(String id) {
		// 本来はDataManager.loadEvent(id)を呼びたいが、static参照がない。
		// そのため、IDだけセットしたオブジェクトを返す。
		// 実際のデータはゲーム進行時に再取得されることを期待する。
		GameEvent event = new GameEvent();
		event.setId(id);
		event.setTitle("Loading...");
		return event;
	}
}