package com.kh.tbrr.data.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * イベントデータモデル
 * JSONから読み込まれるイベント情報を保持
 */
public class GameEvent {
	// 基本情報
	private String id;
	private String title;
	private List<String> tags;
	private List<String> description;

	// 条件・フラグ
	private Map<String, Object> requirements;
	private Map<String, Object> effects;
	private List<String> requiredItems;

	// 選択肢
	private List<Choice> choices;

	// ヘルパーヒント
	private String helperHint;
	private boolean suppressHelperHint; // ★追加: ヒント表示を抑制するかどうか

	// 連鎖イベント
	private String nextEventId;

	// ボス戦フラグ
	private boolean isBossEvent;
	private String bossId;

	// 死亡関連
	private boolean isDeathEvent;
	private String deathCause;

	// 画像変更フィールド（イベント表示時に変更される画像）
	private String backgroundImageChange; // 背景画像変更
	private String subImageChange; // サブウィンドウ画像変更

	// 追加: イベント開始時の自動効果（選択肢を選ぶ前に適用される）
	private InitialEffects initialEffects;

	// ★追加: イベント開始時のSE
	private String soundEffect;

	// インタラクション（ミニゲーム・判定・戦闘など）
	private String interaction;
	private Map<String, Object> interactionParams;

	// コンストラクタ
	public GameEvent() {
		this.tags = new ArrayList<>();
		this.description = new ArrayList<>();
		this.requirements = new HashMap<>();
		this.effects = new HashMap<>();
		this.choices = new ArrayList<>();
	}

	// ======== ゲッター・セッター ========

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public List<String> getDescription() {
		return description;
	}

	public void setDescription(List<String> description) {
		this.description = description;
	}

	public Map<String, Object> getRequirements() {
		return requirements;
	}

	public void setRequirements(Map<String, Object> requirements) {
		this.requirements = requirements;
	}

	public Map<String, Object> getEffects() {
		return effects;
	}

	public List<String> getRequiredItems() {
		return requiredItems;
	}

	public void setRequiredItems(List<String> requiredItems) {
		this.requiredItems = requiredItems;
	}

	public void setEffects(Map<String, Object> effects) {
		this.effects = effects;
	}

	public List<Choice> getChoices() {
		return choices;
	}

	public void setChoices(List<Choice> choices) {
		this.choices = choices;
	}

	public String getHelperHint() {
		return helperHint;
	}

	public void setHelperHint(String helperHint) {
		this.helperHint = helperHint;
	}

	public boolean isSuppressHelperHint() {
		return suppressHelperHint;
	}

	public void setSuppressHelperHint(boolean suppressHelperHint) {
		this.suppressHelperHint = suppressHelperHint;
	}

	public String getNextEventId() {
		return nextEventId;
	}

	public void setNextEventId(String nextEventId) {
		this.nextEventId = nextEventId;
	}

	public boolean isBossEvent() {
		return isBossEvent;
	}

	public void setBossEvent(boolean bossEvent) {
		isBossEvent = bossEvent;
	}

	public String getBossId() {
		return bossId;
	}

	public void setBossId(String bossId) {
		this.bossId = bossId;
	}

	public boolean isDeathEvent() {
		return isDeathEvent;
	}

	public void setDeathEvent(boolean deathEvent) {
		isDeathEvent = deathEvent;
	}

	public String getDeathCause() {
		return deathCause;
	}

	public void setDeathCause(String deathCause) {
		this.deathCause = deathCause;
	}

	public String getBackgroundImageChange() {
		return backgroundImageChange;
	}

	public void setBackgroundImageChange(String backgroundImageChange) {
		this.backgroundImageChange = backgroundImageChange;
	}

	public String getSubImageChange() {
		return subImageChange;
	}

	public void setSubImageChange(String subImageChange) {
		this.subImageChange = subImageChange;
	}

	// ★追加: イベント開始時の自動効果
	public InitialEffects getInitialEffects() {
		return initialEffects;
	}

	// ★追加: イベント開始時のSE
	public String getSoundEffect() {
		return soundEffect;
	}

	public void setSoundEffect(String soundEffect) {
		this.soundEffect = soundEffect;
	}

	public void setInitialEffects(InitialEffects initialEffects) {
		this.initialEffects = initialEffects;
	}

	// インタラクション関連のgetter/setter
	public String getInteraction() {
		return interaction;
	}

	public void setInteraction(String interaction) {
		this.interaction = interaction;
	}

	public Map<String, Object> getInteractionParams() {
		return interactionParams;
	}

	public void setInteractionParams(Map<String, Object> interactionParams) {
		this.interactionParams = interactionParams;
	}

	// ======== 内部クラス: InitialEffects ========

	/**
	 * イベント開始時に自動的に適用される効果
	 * 選択肢を選ぶ前に処理される
	 */
	public static class InitialEffects {
		private List<String> itemsGained; // 入手するアイテムリスト
		private List<String> itemsLost; // 失うアイテムリスト
		private List<String> skillsGained; // 習得するスキルリスト
		private List<String> skillsLost; // 失うスキルリスト
		private List<String> flagsToAdd; // 追加するフラグ
		private List<String> flagsToRemove; // 削除するフラグ
		private Object hpChange; // HP変化
		private Object apChange; // AP変化
		private Object moneyChange; // お金変化
		private Map<String, Object> statusEffectChanges; // 状態異常変化
		private String message; // 表示メッセージ（オプション）

		public InitialEffects() {
			this.itemsGained = new ArrayList<>();
			this.itemsLost = new ArrayList<>();
			this.skillsGained = new ArrayList<>();
			this.skillsLost = new ArrayList<>();
			this.flagsToAdd = new ArrayList<>();
			this.flagsToRemove = new ArrayList<>();
		}

		// Getter/Setter
		public List<String> getFlagsToAdd() {
			return flagsToAdd;
		}

		public void setFlagsToAdd(List<String> flagsToAdd) {
			this.flagsToAdd = flagsToAdd;
		}

		public List<String> getFlagsToRemove() {
			return flagsToRemove;
		}

		public void setFlagsToRemove(List<String> flagsToRemove) {
			this.flagsToRemove = flagsToRemove;
		}

		public List<String> getItemsGained() {
			return itemsGained;
		}

		public void setItemsGained(List<String> itemsGained) {
			this.itemsGained = itemsGained;
		}

		public List<String> getItemsLost() {
			return itemsLost;
		}

		public void setItemsLost(List<String> itemsLost) {
			this.itemsLost = itemsLost;
		}

		public List<String> getSkillsGained() {
			return skillsGained;
		}

		public void setSkillsGained(List<String> skillsGained) {
			this.skillsGained = skillsGained;
		}

		public List<String> getSkillsLost() {
			return skillsLost;
		}

		public void setSkillsLost(List<String> skillsLost) {
			this.skillsLost = skillsLost;
		}

		public Object getHpChange() {
			return hpChange;
		}

		public void setHpChange(Object hpChange) {
			this.hpChange = hpChange;
		}

		public Object getApChange() {
			return apChange;
		}

		public void setApChange(Object apChange) {
			this.apChange = apChange;
		}

		public Object getMoneyChange() {
			return moneyChange;
		}

		public void setMoneyChange(Object moneyChange) {
			this.moneyChange = moneyChange;
		}

		public Map<String, Object> getStatusEffectChanges() {
			return statusEffectChanges;
		}

		public void setStatusEffectChanges(Map<String, Object> statusEffectChanges) {
			this.statusEffectChanges = statusEffectChanges;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	// ======== 内部クラス: Choice ========

	/**
	 * イベントの選択肢
	 */
	public static class Choice {
		private String text;
		private Map<String, Object> conditions;
		private int successRate;
		private int apCost;

		// 選択肢の表示条件
		private String displayCondition;

		// 成功時の結果
		private Result success;

		// 失敗時の結果
		private Result failure;

		// 必須アイテム/スキル
		private String requiredItem;
		private String requiredSkill;

		// requirements (統合フィールド)
		private String requirements;

		// results (success/failureのリスト版)
		private List<Result> results;

		public Choice() {
			this.conditions = new HashMap<>();
			this.successRate = 100;
			this.apCost = 0;
			this.results = new ArrayList<>();
		}

		// ゲッター・セッター

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public Map<String, Object> getConditions() {
			return conditions;
		}

		public void setConditions(Map<String, Object> conditions) {
			this.conditions = conditions;
		}

		public int getSuccessRate() {
			return successRate;
		}

		public void setSuccessRate(int successRate) {
			this.successRate = successRate;
		}

		public int getApCost() {
			return apCost;
		}

		public void setApCost(int apCost) {
			this.apCost = apCost;
		}

		public String getDisplayCondition() {
			return displayCondition;
		}

		public void setDisplayCondition(String displayCondition) {
			this.displayCondition = displayCondition;
		}

		public Result getSuccess() {
			return success;
		}

		public void setSuccess(Result success) {
			this.success = success;
		}

		public Result getFailure() {
			return failure;
		}

		public void setFailure(Result failure) {
			this.failure = failure;
		}

		public String getRequiredItem() {
			return requiredItem;
		}

		public void setRequiredItem(String requiredItem) {
			this.requiredItem = requiredItem;
		}

		public String getRequiredSkill() {
			return requiredSkill;
		}

		public void setRequiredSkill(String requiredSkill) {
			this.requiredSkill = requiredSkill;
		}

		public String getRequirements() {
			if (requirements != null) {
				return requirements;
			}
			if (requiredSkill != null) {
				return "skill:" + requiredSkill;
			}
			if (requiredItem != null) {
				return "item:" + requiredItem;
			}
			return null;
		}

		public void setRequirements(String requirements) {
			this.requirements = requirements;
		}

		public List<Result> getResults() {
			if (results != null && !results.isEmpty()) {
				return results;
			}

			results = new ArrayList<>();
			if (success != null) {
				success.setChance(successRate);
				results.add(success);
			}
			if (failure != null) {
				failure.setChance(100 - successRate);
				results.add(failure);
			}

			return results;
		}

		public void setResults(List<Result> results) {
			this.results = results;
		}
	}

	// ======== 内部クラス: Result ========

	/**
	 * 選択肢の結果
	 */
	public static class Result {
		private String type;
		private List<String> description;
		private Object hpChange; // intまたはStringを受け付ける(low_damage と書くとランダム小ダメージに出来るようにするため)
		private Object apChange; // intまたはStringを受け付ける
		private Object moneyChange; // intまたはStringを受け付ける
		private String itemGained; // 単一アイテム
		private String itemLost; // 単一アイテム
		private List<String> itemsGained; // 複数アイテム入手
		private List<String> itemsLost; // 複数アイテム喪失
		private String itemGainedRandom; // ランダム入手 (例: "common", "magic")
		private String itemLostRandom; // ランダム喪失 (例: "common", "magic")
		private List<String> itemLostRandomList; // 複数ランダム喪失 (例: ["common", "common", "magic"])
		private String skillGained;
		private String skillLost; // 単一スキル喪失
		private List<String> skillsGained; // 複数スキル習得
		private List<String> skillsLost; // 複数スキル喪失
		private int chance;
		private String condition;
		private String nextEventId;
		private boolean isDeath;
		private String clothingChange;
		private String jobChange;
		private String backgroundChange;
		private String bodyTypeChange;
		private String raceNameChange;
		private String raceTypeChange;
		private String genderChange;
		private String genderIdentityChange;

		// 立ち絵表情変更フィールド
		private String expressionChange;

		// 状態異常変更フィールド
		private Map<String, Object> statusEffectChanges;

		// 死亡情報フィールド
		private DeathData death;

		// 画像変更フィールド
		private String backgroundImageChange; // 背景画像変更
		private String subImageChange; // サブウィンドウ画像変更

		// SE再生フィールド
		private String soundEffect;

		// アナザーエンディング関連フィールド
		private String alternateEndingId;

		// フラグ操作フィールド
		private List<String> flagsToAdd;
		private List<String> flagsToRemove;

		// インタラクション（Result内での呼び出し用）
		private String interaction;
		private Map<String, Object> interactionParams;

		public Result() {
			this.description = new ArrayList<>();
			this.chance = 100;
			this.itemsGained = new ArrayList<>();
			this.itemsLost = new ArrayList<>();
			this.skillsGained = new ArrayList<>();
			this.skillsLost = new ArrayList<>();
			this.flagsToAdd = new ArrayList<>();
			this.flagsToRemove = new ArrayList<>();
		}

		// ゲッター・セッター

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public List<String> getDescription() {
			return description;
		}

		public void setDescription(List<String> description) {
			this.description = description;
		}

		public List<String> getText() {
			return description;
		}

		public void setText(List<String> description) {
			this.description = description;
		}

		public Object getHpChange() {
			return hpChange;
		}

		public void setHpChange(Object hpChange) {
			this.hpChange = hpChange;
		}

		public Object getApChange() {
			return apChange;
		}

		public void setApChange(Object apChange) {
			this.apChange = apChange;
		}

		public Object getMoneyChange() {
			return moneyChange;
		}

		public void setMoneyChange(Object moneyChange) {
			this.moneyChange = moneyChange;
		}

		public String getItemGained() {
			return itemGained;
		}

		public void setItemGained(String itemGained) {
			this.itemGained = itemGained;
		}

		public String getItemLost() {
			return itemLost;
		}

		public void setItemLost(String itemLost) {
			this.itemLost = itemLost;
		}

		// 複数アイテム入手/喪失
		public List<String> getItemsGained() {
			return itemsGained;
		}

		public void setItemsGained(List<String> itemsGained) {
			this.itemsGained = itemsGained;
		}

		public List<String> getItemsLost() {
			return itemsLost;
		}

		public void setItemsLost(List<String> itemsLost) {
			this.itemsLost = itemsLost;
		}

		public String getItemGainedRandom() {
			return itemGainedRandom;
		}

		public void setItemGainedRandom(String itemGainedRandom) {
			this.itemGainedRandom = itemGainedRandom;
		}

		public String getItemLostRandom() {
			return itemLostRandom;
		}

		public void setItemLostRandom(String itemLostRandom) {
			this.itemLostRandom = itemLostRandom;
		}

		public String getSkillGained() {
			return skillGained;
		}

		public void setSkillGained(String skillGained) {
			this.skillGained = skillGained;
		}

		public String getSkillLost() {
			return skillLost;
		}

		public void setSkillLost(String skillLost) {
			this.skillLost = skillLost;
		}

		public List<String> getItemLostRandomList() {
			return itemLostRandomList;
		}

		public void setItemLostRandomList(List<String> itemLostRandomList) {
			this.itemLostRandomList = itemLostRandomList;
		}

		// 複数スキル習得
		public List<String> getSkillsGained() {
			return skillsGained;
		}

		public void setSkillsGained(List<String> skillsGained) {
			this.skillsGained = skillsGained;
		}

		public List<String> getSkillsLost() {
			return skillsLost;
		}

		public void setSkillsLost(List<String> skillsLost) {
			this.skillsLost = skillsLost;
		}

		public int getChance() {
			return chance;
		}

		public void setChance(int chance) {
			this.chance = chance;
		}

		public String getNextEventId() {
			return nextEventId;
		}

		public void setNextEventId(String nextEventId) {
			this.nextEventId = nextEventId;
		}

		public boolean isDeath() {
			return isDeath;
		}

		public void setDeath(boolean death) {
			this.isDeath = death;
		}

		public DeathData getDeath() {
			return death;
		}

		public void setDeath(DeathData death) {
			this.death = death;
		}

		public String getCondition() {
			return condition;
		}

		public void setCondition(String condition) {
			this.condition = condition;
		}

		public String getClothingChange() {
			return clothingChange;
		}

		public String getJobChange() {
			return jobChange;
		}

		public String getBackgroundChange() {
			return backgroundChange;
		}

		public String getBodyTypeChange() {
			return bodyTypeChange;
		}

		public String getRaceNameChange() {
			return raceNameChange;
		}

		public String getRaceTypeChange() {
			return raceTypeChange;
		}

		public String getGenderChange() {
			return genderChange;
		}

		public String getGenderIdentityChange() {
			return genderIdentityChange;
		}

		public Map<String, Object> getStatusEffectChanges() {
			return statusEffectChanges;
		}

		public String getExpressionChange() {
			return expressionChange;
		}

		public void setExpressionChange(String expressionChange) {
			this.expressionChange = expressionChange;
		}

		public void setStatusEffectChanges(Map<String, Object> statusEffectChanges) {
			this.statusEffectChanges = statusEffectChanges;
		}

		public String getBackgroundImageChange() {
			return backgroundImageChange;
		}

		public void setBackgroundImageChange(String backgroundImageChange) {
			this.backgroundImageChange = backgroundImageChange;
		}

		public String getSubImageChange() {
			return subImageChange;
		}

		public void setSubImageChange(String subImageChange) {
			this.subImageChange = subImageChange;
		}

		public String getSoundEffect() {
			return soundEffect;
		}

		public void setSoundEffect(String soundEffect) {
			this.soundEffect = soundEffect;
		}

		public String getAlternateEndingId() {
			return alternateEndingId;
		}

		public void setAlternateEndingId(String alternateEndingId) {
			this.alternateEndingId = alternateEndingId;
		}

		public boolean isAlternateEnding() {
			return alternateEndingId != null && !alternateEndingId.isEmpty();
		}

		public List<String> getFlagsToAdd() {
			return flagsToAdd;
		}

		public void setFlagsToAdd(List<String> flagsToAdd) {
			this.flagsToAdd = flagsToAdd;
		}

		public List<String> getFlagsToRemove() {
			return flagsToRemove;
		}

		public void setFlagsToRemove(List<String> flagsToRemove) {
			this.flagsToRemove = flagsToRemove;
		}

		// インタラクション関連のgetter/setter
		public String getInteraction() {
			return interaction;
		}

		public void setInteraction(String interaction) {
			this.interaction = interaction;
		}

		public Map<String, Object> getInteractionParams() {
			return interactionParams;
		}

		public void setInteractionParams(Map<String, Object> interactionParams) {
			this.interactionParams = interactionParams;
		}

	}

	// ======== 内部クラス: DeathData ========

	/**
	 * 死亡情報（Result内の "death" オブジェクトを受け取る）
	 */
	public static class DeathData {
		private List<String> description;
		private String deathCause;

		public List<String> getDescription() {
			return description;
		}

		public void setDescription(List<String> description) {
			this.description = description;
		}

		public String getDeathCause() {
			return deathCause;
		}

		public void setDeathCause(String deathCause) {
			this.deathCause = deathCause;
		}
	}
}