package com.kh.tbrr.battle.data;

import java.util.List;
import java.util.Map;

public class AbilityData {
    private String id;
    private String name;
    /** 射程テーブルの参照キー（melee/ranged/projectile/sweep/spear）。
     *  null の場合は武器の rangeType にフォールバック。武器も無ければ "melee" で確定。 */
    private String rangeType;
    /** 攻撃の属性タグ（例: ["magic","fire"], ["physical"]）。
     *  将来の耐性・特効・マスタリー拡張の基盤。武器の武器カテゴリタグとは別物。 */
    private List<String> tags;
    /** アビリティ個別の距離テーブル。キーは距離(String)、値は"HIT"/"MISS"/"BONUS"。
     *  指定した距離のみ上書き。rangeType より優先される（Ability is King）。 */
    private Map<String, String> rangeOverride;
    private int apCost;
    private Check check;
    private List<String> description;
    private java.util.Map<String, com.kh.tbrr.data.models.CombatConditionModifiers> combatConditionModifiers;

    public static class Check {
        private Integer baseChance;
        private String attackerStat;
        private String defenderStat;
        private String damageDice;
        /** ステータス名 → 倍率 のマップ。複数ステータスによる複合スケーリングに対応。
         *  例: {"might": 0.5} / {"insight": 0.5, "presence": 0.5} */
        private java.util.Map<String, Double> scalings;

        public Integer getBaseChance() { return baseChance; }
        public String getAttackerStat() { return attackerStat; }
        public String getDefenderStat() { return defenderStat; }
        public String getDamageDice() { return damageDice; }
        public java.util.Map<String, Double> getScalings() { return scalings; }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRangeType() { return rangeType; }
    public List<String> getTags() { return tags; }
    public Map<String, String> getRangeOverride() { return rangeOverride; }
    public int getApCost() { return apCost; }
    public Check getCheck() { return check; }
    public List<String> getDescription() { return description; }
    public java.util.Map<String, com.kh.tbrr.data.models.CombatConditionModifiers> getCombatConditionModifiers() { return combatConditionModifiers; }

    /** 命中時に距離をこの値に強制上書きする（null なら何もしない）。
     *  例: 稲妻の衝撃が命中したら強制的に距離2にする → 2 を指定。 */
    private Integer forceDistanceTo;
    private Boolean inheritWeapon;

    public Boolean getInheritWeapon() { return inheritWeapon; }
    public void setInheritWeapon(Boolean inheritWeapon) { this.inheritWeapon = inheritWeapon; }

    private List<ConditionApplication> applyCombatConditions;

    public static class ConditionApplication {
        private String conditionId;
        private int chance; // %
        private int duration;
        private int intensity; // 強度（dotのダメージ量、hindranceの確率%など。0の場合はJSON定義側のデフォルト値を使用）

        public String getConditionId() { return conditionId; }
        public int getChance() { return chance; }
        public int getDuration() { return duration; }
        public int getIntensity() { return intensity; }
    }

    public List<ConditionApplication> getApplyCombatConditions() {
        return applyCombatConditions;
    }

    public Integer getForceDistanceTo() { return forceDistanceTo; }
    
    // --- 追加: 二刀流および武器自動持ち替えのフラグ ---
    private Boolean triggerOffHandAttack;
    public Boolean getTriggerOffHandAttack() { return triggerOffHandAttack; }
    
    private Boolean triggerAutoWeaponSwitch;
    public Boolean getTriggerAutoWeaponSwitch() { return triggerAutoWeaponSwitch; }

    // --- 追加: リスト式自動アップグレード用フィールド ---
    private Map<String, String> upgrades;
    private Boolean countAbilityAsLevel;

    public Map<String, String> getUpgrades() { return upgrades; }
    public boolean isCountAbilityAsLevel() { return Boolean.TRUE.equals(countAbilityAsLevel); }

    // --- 追加: アビリティ使用時のSP増加定義 ---
    /** アビリティ使用時に発動者のSPを増加させる計算式。nullの場合はSP増加なし。 */
    private ScalingData spGain;

    public ScalingData getSpGain() { return spGain; }
    public void setSpGain(ScalingData spGain) { this.spGain = spGain; }
}
