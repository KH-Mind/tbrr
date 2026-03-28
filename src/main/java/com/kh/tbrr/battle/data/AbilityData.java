package com.kh.tbrr.battle.data;

import java.util.List;

public class AbilityData {
    private String id;
    private String name;
    private String type;
    private int apCost;
    private Check check;
    private List<String> description;

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
}
