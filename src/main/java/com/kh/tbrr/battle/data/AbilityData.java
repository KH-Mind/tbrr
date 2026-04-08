package com.kh.tbrr.battle.data;

import java.util.List;

public class AbilityData {
    private String id;
    private String name;
    private String type;
    private int apCost;
    private Check check;
    private List<String> description;
    private java.util.Map<String, com.kh.tbrr.data.models.CombatConditionModifiers> combatConditionModifiers;

    public static class Check {
        private Integer baseChance;
        private String attackerStat;
        private String defenderStat;
        private String damageDice;
        private String scalingStat;
        private Double statScaling;

        public Integer getBaseChance() { return baseChance; }
        public String getAttackerStat() { return attackerStat; }
        public String getDefenderStat() { return defenderStat; }
        public String getDamageDice() { return damageDice; }
        public String getScalingStat() { return scalingStat; }
        public Double getStatScaling() { return statScaling; }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getApCost() { return apCost; }
    public Check getCheck() { return check; }
    public List<String> getDescription() { return description; }
    public java.util.Map<String, com.kh.tbrr.data.models.CombatConditionModifiers> getCombatConditionModifiers() { return combatConditionModifiers; }

    private List<ConditionApplication> applyCombatConditions;

    public static class ConditionApplication {
        private String conditionId;
        private int chance; // %
        private int duration;

        public String getConditionId() { return conditionId; }
        public int getChance() { return chance; }
        public int getDuration() { return duration; }
    }

    public List<ConditionApplication> getApplyCombatConditions() {
        return applyCombatConditions;
    }
}
