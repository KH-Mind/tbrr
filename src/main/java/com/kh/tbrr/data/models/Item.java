package com.kh.tbrr.data.models;

import java.util.ArrayList;
import java.util.List;

/**
 * アイテムデータモデル
 */
public class Item {
	private String id;
	private String name;
	private String rarity; // common, magic, unique, job
	private String description;
	private List<String> grantedSkills; // このアイテムが付与する技能
	private boolean consumable; // 消費アイテムか
	private boolean losableRandom; // ランダム喪失の対象か（デフォルト: true）
	private int attackBonus; // 攻撃力ボーナス
	private java.util.Map<String, Integer> combatStats; // 戦闘ステータス（might, insight, finesse, presence, sensuality）

	// コンストラクタ
	public Item() {
		this.grantedSkills = new ArrayList<>();
		this.consumable = false;
		this.losableRandom = true;
		this.attackBonus = 0;
		this.combatStats = new java.util.HashMap<>();
	}

	// Getters and Setters
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

	public String getRarity() {
		return rarity;
	}

	public void setRarity(String rarity) {
		this.rarity = rarity;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getGrantedSkills() {
		return grantedSkills;
	}

	public void setGrantedSkills(List<String> grantedSkills) {
		this.grantedSkills = grantedSkills;
	}

	public boolean isConsumable() {
		return consumable;
	}

	public void setConsumable(boolean consumable) {
		this.consumable = consumable;
	}

	public int getAttackBonus() {
		return attackBonus;
	}

	public boolean isLosableRandom() {
		return losableRandom;
	}

	public void setLosableRandom(boolean losableRandom) {
		this.losableRandom = losableRandom;
	}

	public void setAttackBonus(int attackBonus) {
		this.attackBonus = attackBonus;
	}

	@Override
	public String toString() {
		return name + " (" + rarity + ")";
	}

	// 戦闘ステータス関連
	public java.util.Map<String, Integer> getCombatStats() {
		return combatStats;
	}

	public void setCombatStats(java.util.Map<String, Integer> combatStats) {
		this.combatStats = combatStats;
	}

	public int getCombatStat(String statName) {
		return combatStats != null ? combatStats.getOrDefault(statName, 0) : 0;
	}
}