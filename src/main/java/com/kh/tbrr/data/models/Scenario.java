package com.kh.tbrr.data.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * シナリオクラス
 * ゲームモード全体を管理
 */
public class Scenario {
	private String id; // シナリオID
	private String name; // 表示名
	private String description; // 説明文
	private String difficulty; // 難易度
	private int totalFloors; // 総フロア数

	// JSONの"areas"を"stageConfigs"にマッピング
	@SerializedName("areas")
	private List<StageConfig> stageConfigs; // フロア設定リスト

	private List<String> availableItems; // 入手可能アイテム
	private List<String> availableHelpers; // 登場ヘルパー
	private String prologue; // プロローグ
	private String epilogue; // エピローグ

	// 画像関連
	private String thumbnailImage; // シナリオ概要画面用の背景画像（「このシナリオを選びますか？」の画面）
	private String subImage; // ★追加: シナリオ概要画面用のサブウィンドウ画像

	// ★追加: グローバル条件付きイベント（全フロア共通）
	private List<ConditionalEventEntry> globalConditionalEvents;
	private List<ForcedEventEntry> globalForcedEvents;

	/**
	 * フロア設定クラス
	 * 各フロアの地形選択ルールとイベント設定を管理
	 */
	public static class StageConfig {
		private String name; // フロア名(オプション、デバッグ用)
		private List<String> tags; // タグ
		private List<String> mapPool; // マッププール(地形候補)
		private List<String> eventPool; // イベントプール
		private Map<String, String> fixedEvents; // 固定イベント

		private boolean inheritMap = false; // 前フロアの地形を継続使用するか
		private boolean suppressMapEntryEvent = false; // マップ入場イベントを抑制
		private String mapEntryEventOverride; // マップ入場イベントを上書き

		// ★新規追加: マップ選択機能
		private boolean allowPlayerChoice = false; // プレイヤーに選択させるか
		private List<MapChoice> mapChoices; // 選択肢リスト

		/**
		 * マップ選択肢クラス
		 * プレイヤーが選択できるマップの情報を保持
		 */
		public static class MapChoice {
			private String mapId; // マップID (例: "town/town", "town/village")
			private String displayName; // 表示名 (例: "賑やかな町")
			private String description; // 説明文

			// コンストラクタ
			public MapChoice() {
			}

			// Getter/Setter
			public String getMapId() {
				return mapId;
			}

			public void setMapId(String mapId) {
				this.mapId = mapId;
			}

			public String getDisplayName() {
				return displayName;
			}

			public void setDisplayName(String displayName) {
				this.displayName = displayName;
			}

			public String getDescription() {
				return description;
			}

			public void setDescription(String description) {
				this.description = description;
			}
		}

		public boolean isInheritMap() {
			return inheritMap;
		}

		public void setInheritMap(boolean inheritMap) {
			this.inheritMap = inheritMap;
		}

		public StageConfig() {
			this.tags = new ArrayList<>();
			this.mapPool = new ArrayList<>();
			this.eventPool = new ArrayList<>();
			this.fixedEvents = new HashMap<>();
		}

		// Getter/Setter
		public String getName() {
			return name != null ? name : "(unnamed stage)";
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}

		public List<String> getMapPool() {
			return mapPool;
		}

		public void setMapPool(List<String> mapPool) {
			this.mapPool = mapPool;
		}

		public List<String> getEventPool() {
			return eventPool;
		}

		public void setEventPool(List<String> eventPool) {
			this.eventPool = eventPool;
		}

		public Map<String, String> getFixedEvents() {
			return fixedEvents;
		}

		public void setFixedEvents(Map<String, String> fixedEvents) {
			this.fixedEvents = fixedEvents;
		}

		public boolean isSuppressMapEntryEvent() {
			return suppressMapEntryEvent;
		}

		public void setSuppressMapEntryEvent(boolean suppressMapEntryEvent) {
			this.suppressMapEntryEvent = suppressMapEntryEvent;
		}

		public String getMapEntryEventOverride() {
			return mapEntryEventOverride;
		}

		public void setMapEntryEventOverride(String mapEntryEventOverride) {
			this.mapEntryEventOverride = mapEntryEventOverride;
		}

		// ★新規追加: マップ選択機能のGetter/Setter
		public boolean isAllowPlayerChoice() {
			return allowPlayerChoice;
		}

		public void setAllowPlayerChoice(boolean allowPlayerChoice) {
			this.allowPlayerChoice = allowPlayerChoice;
		}

		public List<MapChoice> getMapChoices() {
			return mapChoices;
		}

		public void setMapChoices(List<MapChoice> mapChoices) {
			this.mapChoices = mapChoices;
		}
	}

	/**
	 * 条件付きイベントエントリ
	 * プレイヤーの状態に応じてイベントプールに追加されるイベント
	 */
	public static class ConditionalEventEntry {
		private String eventId;
		private String condition;
		private String description; // オプション、デバッグ用

		public String getEventId() {
			return eventId;
		}

		public void setEventId(String eventId) {
			this.eventId = eventId;
		}

		public String getCondition() {
			return condition;
		}

		public void setCondition(String condition) {
			this.condition = condition;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	/**
	 * 強制トリガーイベントエントリ
	 * 条件を満たすと確定で発生するイベント
	 */
	public static class ForcedEventEntry {
		private String eventId;
		private String condition;
		private int priority; // 優先度（複数条件を満たす場合、高い方を優先）
		private String description; // オプション、デバッグ用

		public String getEventId() {
			return eventId;
		}

		public void setEventId(String eventId) {
			this.eventId = eventId;
		}

		public String getCondition() {
			return condition;
		}

		public void setCondition(String condition) {
			this.condition = condition;
		}

		public int getPriority() {
			return priority;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	// コンストラクタ
	public Scenario() {
		this.stageConfigs = new ArrayList<>();

		this.availableItems = new ArrayList<>();
		this.availableHelpers = new ArrayList<>();
		this.globalConditionalEvents = new ArrayList<>();
		this.globalForcedEvents = new ArrayList<>();
	}

	// Getter/Setter
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(String difficulty) {
		this.difficulty = difficulty;
	}

	public int getTotalFloors() {
		return stageConfigs != null ? stageConfigs.size() : 0;
	}

	public void setTotalFloors(int totalFloors) {
		this.totalFloors = totalFloors;
	}

	/**
	 * フロア設定リストを取得
	 * 
	 * @return フロア設定リスト
	 */
	public List<StageConfig> getStageConfigs() {
		return stageConfigs;
	}

	/**
	 * フロア設定リストを設定
	 * 
	 * @param stageConfigs フロア設定リスト
	 */
	public void setStageConfigs(List<StageConfig> stageConfigs) {
		this.stageConfigs = stageConfigs;
	}

	public List<String> getAvailableItems() {
		return availableItems;
	}

	public void setAvailableItems(List<String> availableItems) {
		this.availableItems = availableItems;
	}

	public List<String> getAvailableHelpers() {
		return availableHelpers;
	}

	public void setAvailableHelpers(List<String> availableHelpers) {
		this.availableHelpers = availableHelpers;
	}

	public String getPrologue() {
		return prologue;
	}

	public void setPrologue(String prologue) {
		this.prologue = prologue;
	}

	public String getEpilogue() {
		return epilogue;
	}

	public void setEpilogue(String epilogue) {
		this.epilogue = epilogue;
	}

	public String getThumbnailImage() {
		return thumbnailImage;
	}

	public void setThumbnailImage(String thumbnailImage) {
		this.thumbnailImage = thumbnailImage;
	}

	// ★追加: サブウィンドウ画像のGetter/Setter
	public String getSubImage() {
		return subImage;
	}

	public void setSubImage(String subImage) {
		this.subImage = subImage;
	}

	// ★追加: グローバル条件付きイベントのGetter/Setter
	public List<ConditionalEventEntry> getGlobalConditionalEvents() {
		return globalConditionalEvents;
	}

	public void setGlobalConditionalEvents(List<ConditionalEventEntry> globalConditionalEvents) {
		this.globalConditionalEvents = globalConditionalEvents;
	}

	public List<ForcedEventEntry> getGlobalForcedEvents() {
		return globalForcedEvents;
	}

	public void setGlobalForcedEvents(List<ForcedEventEntry> globalForcedEvents) {
		this.globalForcedEvents = globalForcedEvents;
	}

	/**
	 * フロア番号から対応するフロア設定を取得
	 * 
	 * @param floor フロア番号（0始まり）
	 * @return 対応するフロア設定、見つからない場合はnull
	 */
	public StageConfig getStageConfigByFloor(int floor) {
		if (stageConfigs == null || stageConfigs.isEmpty()) {
			return null;
		}

		// フロア番号が配列のインデックスに直接対応
		if (floor < 0 || floor >= stageConfigs.size()) {
			return null;
		}

		return stageConfigs.get(floor);
	}

	@Override
	public String toString() {
		return name + " (" + id + ")";
	}
}