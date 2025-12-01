package com.kh.tbrr.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kh.tbrr.data.models.GameEvent;
import com.kh.tbrr.data.models.GameMap;
import com.kh.tbrr.data.models.GraveRecord;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.system.CharacterLoader;

/**
 * ゲーム状態管理クラス
 * プレイ中の状態、フラグ、カウンター、墓地などを管理
 */
public class GameState {
	// 基本状態
	private Player currentPlayer;
	private int currentFloor = 0;
	private int maxFloor;
	private String currentScenario;
	private boolean isGameOver;
	private boolean isVictory;
	private String alternateEndingId;

	// 墓地
	private Map<String, GraveRecord> graveyard = new HashMap<>();
	private String lastDeathCause;

	// フラグ・カウンター
	private Set<String> flags;
	private Map<String, Integer> counters;

	// マップ
	private GameMap currentMap;
	private int eventCount = 0;

	// 画像管理
	private String currentBackgroundImage; // 現在表示中の背景画像
	private String currentSubImage; // 現在表示中のサブウィンドウ画像
	// イベント進行状態の管理用フラグ
	// nextEventId による再帰イベント中かどうかを判定するために使用
	// true の間はイベント達成数（eventCount）を加算しない
	private boolean isInRecursiveEvent = false;

	// 重要ログ用：最後にログを出力したフロア番号
	private int lastLoggedFloor = -1;

	// ↑↑↑ 後で整理しような ↑↑↑

	// ========== イベント山札管理 ==========

	private List<String> remainingEventDeck = new ArrayList<>();
	private String currentEventPoolId = null; // 現在のイベントプールID

	public List<String> getRemainingEventDeck() {
		return remainingEventDeck;
	}

	public void setRemainingEventDeck(List<String> deck) {
		this.remainingEventDeck = new ArrayList<>(deck);
	}

	public void removeEventFromDeck(String eventId) {
		remainingEventDeck.remove(eventId);
	}

	// イベントプールIDを取得
	public String getCurrentEventPoolId() {
		return currentEventPoolId;
	}

	// イベントプールIDを設定し、異なる場合はデッキをクリア
	public void setCurrentEventPoolId(String poolId) {
		if (poolId == null || !poolId.equals(this.currentEventPoolId)) {
			this.remainingEventDeck.clear(); // デッキをクリア
			this.currentEventPoolId = poolId;
		}
	}

	// 現在のイベント
	private GameEvent currentEvent;

	// コンストラクタ
	public GameState() {
		this.currentFloor = 0;
		this.maxFloor = 10;
		this.currentScenario = "";
		this.isGameOver = false;
		this.isVictory = false;
		this.flags = new HashSet<>();
		this.counters = new HashMap<>();
	}

	// ========== 死亡・墓地管理 ==========

	public void markCharacterAsLost(Player player) {
		GraveRecord record = new GraveRecord(
				player.getId(),
				player.getName(),
				player.isFatedOne(),
				lastDeathCause,
				currentFloor,
				LocalDateTime.now());
		graveyard.put(player.getId(), record);
	}

	public void setLastDeathCause(String cause) {
		this.lastDeathCause = cause;
	}

	public String getLastDeathCause() {
		return lastDeathCause;
	}

	public boolean reviveCharacter(String charId) {
		GraveRecord record = graveyard.get(charId);
		if (record == null || record.isRevived())
			return false;
		if (!record.isFated())
			return false;

		Player revived = CharacterLoader.loadFromFile(charId);
		if (revived == null)
			return false;

		record.setRevived(true);
		setCurrentPlayer(revived); // プレイヤーを再登録
		return true;
	}

	public List<GraveRecord> getGraveyardRecords() {
		return new ArrayList<>(graveyard.values());
	}

	// 現在のプレイヤー

	public void setCurrentPlayer(Player player) {
		this.currentPlayer = player;
	}

	public Player getCurrentPlayer() {
		return currentPlayer;
	}

	// ========== フロア管理 ==========

	public void advanceFloor() {
		this.currentFloor++;
	}

	public int getCurrentFloor() {
		return currentFloor;
	}

	public void setCurrentFloor(int floor) {
		this.currentFloor = floor;
	}

	public int getMaxFloor() {
		return maxFloor;
	}

	public void setMaxFloor(int maxFloor) {
		this.maxFloor = maxFloor;
	}

	// イベントのカウント
	public void incrementEventCount() {
		eventCount++;
	}

	public int getEventCount() {
		return eventCount;
	}

	// 再帰イベント中かどうかを設定します。
	// @param value true の場合、再帰イベント中として扱います
	public void setInRecursiveEvent(boolean value) {
		isInRecursiveEvent = value;
	}

	// 現在、再帰イベント中かどうかを返します。
	public boolean isInRecursiveEvent() {
		return isInRecursiveEvent;
	}

	// ========== エリア管理 ==========

	private String currentAreaName;

	public void setCurrentAreaName(String areaName) {
		this.currentAreaName = areaName;
	}

	public String getCurrentAreaName() {
		return currentAreaName;
	}

	// ========== マップ管理 ==========

	public GameMap getCurrentMap() {
		return currentMap;
	}

	public void setCurrentMap(GameMap currentMap) {
		this.currentMap = currentMap;
	}

	// ========== 画像管理 ==========

	public String getCurrentBackgroundImage() {
		return currentBackgroundImage;
	}

	public void setCurrentBackgroundImage(String currentBackgroundImage) {
		this.currentBackgroundImage = currentBackgroundImage;
	}

	public String getCurrentSubImage() {
		return currentSubImage;
	}

	public void setCurrentSubImage(String currentSubImage) {
		this.currentSubImage = currentSubImage;
	}

	// 現在のイベント（死亡時のタグ参照用）
	public GameEvent getCurrentEvent() {
		return currentEvent;
	}

	public void setCurrentEvent(GameEvent currentEvent) {
		this.currentEvent = currentEvent;
	}

	// ========== シナリオ管理 ==========

	public String getCurrentScenario() {
		return currentScenario;
	}

	public void setCurrentScenario(String scenarioId) {
		this.currentScenario = scenarioId;
	}

	// ========== 死亡履歴 ==========

	public void recordDeath(String cause) {
		this.lastDeathCause = cause;
		// 死亡履歴の保存が必要なら後で拡張
		// 現在はメモリ上の graveyard に保存されるため、ここでの永続化は不要
	}

	public int getUnlockedDeathCount() {
		// 実装予定。現在は初期リリース用に1件固定
		// 将来的には実績や周回要素で解放数を増やす予定
		return 1;
	}

	// ========== ゲーム状態管理 ==========

	public boolean isGameOver() {
		return isGameOver;
	}

	public void setGameOver(boolean gameOver) {
		this.isGameOver = gameOver;
	}

	public boolean isVictory() {
		return isVictory;
	}

	public void setVictory(boolean victory) {
		this.isVictory = victory;
	}
	// ========== アナザーエンディング管理 ==========

	public String getAlternateEndingId() {
		return alternateEndingId;
	}

	public void setAlternateEndingId(String endingId) {
		this.alternateEndingId = endingId;
	}

	public boolean hasAlternateEnding() {
		return alternateEndingId != null && !alternateEndingId.isEmpty();
	}

	// ========== フラグ管理 ==========

	public void setFlag(String flag) {
		flags.add(flag);
	}

	public boolean hasFlag(String flag) {
		return flags.contains(flag);
	}

	public void removeFlag(String flag) {
		flags.remove(flag);
	}

	public List<String> getAllFlags() {
		return new ArrayList<>(flags);
	}

	public void clearFlags() {
		flags.clear();
	}

	// ========== カウンター管理 ==========

	public void incrementCounter(String key) {
		counters.put(key, counters.getOrDefault(key, 0) + 1);
	}

	public void incrementCounter(String key, int amount) {
		counters.put(key, counters.getOrDefault(key, 0) + amount);
	}

	public void setCounter(String key, int value) {
		counters.put(key, value);
	}

	public int getCounter(String key) {
		return counters.getOrDefault(key, 0);
	}

	public boolean hasCounter(String key) {
		return counters.containsKey(key);
	}

	public void resetCounter(String key) {
		counters.remove(key);
	}

	public void clearCounters() {
		counters.clear();
	}

	// ========== 重要ログ用 ==========

	public int getLastLoggedFloor() {
		return lastLoggedFloor;
	}

	public void setLastLoggedFloor(int floor) {
		this.lastLoggedFloor = floor;
	}

	// ========== リセット ==========

	public void reset() {
		this.currentFloor = 0;
		this.isGameOver = false;
		this.isVictory = false;
		this.alternateEndingId = null;
		this.flags.clear();
		this.counters.clear();
		this.lastLoggedFloor = -1;
	}

	public void fullReset() {
		reset();
		this.currentScenario = "";
		this.maxFloor = 10;
	}

	// ========== デバッグ用 ==========

	@Override
	public String toString() {
		return String.format(
				"GameState[Floor:%d/%d, Scenario:%s, GameOver:%b, Victory:%b, Flags:%d, Counters:%d]",
				currentFloor, maxFloor, currentScenario, isGameOver, isVictory,
				flags.size(), counters.size());
	}
}