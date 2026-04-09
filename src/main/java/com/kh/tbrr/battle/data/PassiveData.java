package com.kh.tbrr.battle.data;

import java.util.List;

/**
 * パッシブスキルのデータクラス。
 * 旧: com.kh.tbrr.data.models.PassiveData
 *
 * typeの種類:
 *   MASTERY        - 武器習熟（targetTagsのタグを持つ武器に習熟ボーナス）
 *   CRIT_MULTIPLIER- クリティカル倍率の上書き
 *   SYSTEMIC       - 戦闘フローそのものを拡張する特殊パッシブ（二刀流など）
 */
public class PassiveData {
    private String id;
    private String name;
    private String description;
    private String type;                  // "MASTERY", "CRIT_MULTIPLIER", "SYSTEMIC" 等
    private List<String> targetTags;      // 対象となる武器タグ (例: "dagger", "sword")
    private int level;                    // マスタリーレベル
    private double critMultiplier;        // CRIT_MULTIPLIER型用（例: 2.0）

    // --- SYSTEMIC 型パッシブ用フィールド ---
    private String systemicEffect;        // "DUAL_WIELD" など
    private List<String> offHandFreeTagConditions; // これらのタグを持つオフハンド武器はペナルティ免除
    private int offHandHitPenalty;        // オフハンド攻撃の命中ペナルティ（例: -20）
    private double offHandDamageMultiplier; // オフハンド攻撃のダメージ倍率（例: 0.5）

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<String> getTargetTags() { return targetTags; }
    public void setTargetTags(List<String> targetTags) { this.targetTags = targetTags; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public double getCritMultiplier() { return critMultiplier; }
    public void setCritMultiplier(double critMultiplier) { this.critMultiplier = critMultiplier; }
    public String getSystemicEffect() { return systemicEffect; }
    public void setSystemicEffect(String systemicEffect) { this.systemicEffect = systemicEffect; }
    public List<String> getOffHandFreeTagConditions() { return offHandFreeTagConditions; }
    public void setOffHandFreeTagConditions(List<String> offHandFreeTagConditions) { this.offHandFreeTagConditions = offHandFreeTagConditions; }
    public int getOffHandHitPenalty() { return offHandHitPenalty; }
    public void setOffHandHitPenalty(int offHandHitPenalty) { this.offHandHitPenalty = offHandHitPenalty; }
    public double getOffHandDamageMultiplier() { return offHandDamageMultiplier; }
    public void setOffHandDamageMultiplier(double offHandDamageMultiplier) { this.offHandDamageMultiplier = offHandDamageMultiplier; }
}
