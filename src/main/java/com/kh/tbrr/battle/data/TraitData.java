package com.kh.tbrr.battle.data;

import java.util.List;
import java.util.Map;

/**
 * 特徴（Trait）のデータクラス。
 * 旧クラス名: PassiveData
 *
 * typeの種類:
 * MASTERY         - 武器習熟（targetTagsのタグを持つ武器に習熟ボーナス）
 * CRIT_MULTIPLIER - クリティカル倍率の上書き
 * SYSTEMIC        - 戦闘フローそのものを拡張する特殊特徴（二刀流など）
 * INITIATIVE      - イニシアチブ判定へのボーナス
 */
public class TraitData {
    private String id;
    private String name;
    private String description;
    private String type; // "MASTERY", "CRIT_MULTIPLIER", "SYSTEMIC", "INITIATIVE" 等

    // --- MASTERY 型用フィールド ---
    private List<String> targetTags; // 対象となる武器タグ（OR条件） (例: "dagger", "sword")
    private List<String> requiredTags; // 必須となる対象タグ（AND条件） (例: "magic", "attack")
    private int level;               // マスタリーレベル

    // --- CRIT_MULTIPLIER 型用フィールド ---
    private double critMultiplier; // クリティカル倍率上書き値（例: 2.0）

    // --- SYSTEMIC 型用フィールド ---
    private String systemicEffect;                    // "DUAL_WIELD" など
    private List<String> offHandFreeTagConditions;    // ペナルティ免除条件タグ
    private int offHandHitPenalty;                    // オフハンド命中ペナルティ（例: -20）
    private double offHandDamageMultiplier;           // オフハンドダメージ倍率（例: 0.5）

    // --- INITIATIVE 型用フィールド ---
    private int initiativeBonus; // イニシアチブ判定への固定値ボーナス（例: 10, -10, 999）

    // -----------------------------------------------------------------------
    // 将来拡張用フィールド（現時点では読み込みのみ。処理ロジックは未実装）
    // -----------------------------------------------------------------------

    /**
     * このTraitを持つキャラクターが得る技能のリスト。
     * 例: ["腕力"] → getEffectiveSkills() に反映予定。
     */
    private List<String> grantedSkills;

    /**
     * このTraitが自動で付与する別の特徴(Trait)のリスト。
     */
    private List<String> grantedTraits;

    /**
     * このTraitを持つキャラクターが習得するアビリティIDのリスト。
     * 例: ["misty_step"] → getEffectiveAbilities() に反映予定。
     */
    private List<String> grantedAbilities;

    /**
     * このTraitを持つキャラクターが得るステータスボーナス。
     * 例: {"might": 5} → getCombatStats() に反映予定。
     */
    private Map<String, Integer> statBonuses;

    // --- ゲッター ---

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getType() { return type; }

    public List<String> getTargetTags() { return targetTags; }
    public List<String> getRequiredTags() { return requiredTags; }
    public int getLevel() { return level; }

    public double getCritMultiplier() { return critMultiplier; }

    public String getSystemicEffect() { return systemicEffect; }
    public List<String> getOffHandFreeTagConditions() { return offHandFreeTagConditions; }
    public int getOffHandHitPenalty() { return offHandHitPenalty; }
    public double getOffHandDamageMultiplier() { return offHandDamageMultiplier; }

    public int getInitiativeBonus() { return initiativeBonus; }

    public List<String> getGrantedSkills() { return grantedSkills; }
    public List<String> getGrantedTraits() { return grantedTraits; }
    public List<String> getGrantedAbilities() { return grantedAbilities; }
    public Map<String, Integer> getStatBonuses() { return statBonuses; }

    // --- セッター ---

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setType(String type) { this.type = type; }
    public void setTargetTags(List<String> targetTags) { this.targetTags = targetTags; }
    public void setRequiredTags(List<String> requiredTags) { this.requiredTags = requiredTags; }
    public void setLevel(int level) { this.level = level; }
    public void setCritMultiplier(double critMultiplier) { this.critMultiplier = critMultiplier; }
    public void setSystemicEffect(String systemicEffect) { this.systemicEffect = systemicEffect; }
    public void setOffHandFreeTagConditions(List<String> offHandFreeTagConditions) { this.offHandFreeTagConditions = offHandFreeTagConditions; }
    public void setOffHandHitPenalty(int offHandHitPenalty) { this.offHandHitPenalty = offHandHitPenalty; }
    public void setOffHandDamageMultiplier(double offHandDamageMultiplier) { this.offHandDamageMultiplier = offHandDamageMultiplier; }
    public void setInitiativeBonus(int initiativeBonus) { this.initiativeBonus = initiativeBonus; }
    public void setGrantedSkills(List<String> grantedSkills) { this.grantedSkills = grantedSkills; }
    public void setGrantedTraits(List<String> grantedTraits) { this.grantedTraits = grantedTraits; }
    public void setGrantedAbilities(List<String> grantedAbilities) { this.grantedAbilities = grantedAbilities; }
    public void setStatBonuses(Map<String, Integer> statBonuses) { this.statBonuses = statBonuses; }
}
