package com.kh.tbrr.battle.data;

import java.util.Map;
import java.util.List;

public class CombatBaseRules {
    private Map<String, Map<String, String>> distanceRules;
    private AccuracyRules accuracy;
    private DamageRules damage;
    private double globalStatScaling = 1.0;

    public static class AccuracyRules {
        private int baseChance;
        private List<Modifier> modifiers;
        private int min;
        private int max;

        public int getBaseChance() { return baseChance; }
        public List<Modifier> getModifiers() { return modifiers; }
        public int getMin() { return min; }
        public int getMax() { return max; }
    }

    public static class Modifier {
        private List<Integer> diff;
        private int bonus;

        public List<Integer> getDiff() { return diff; }
        public int getBonus() { return bonus; }
    }

    public static class DamageRules {
        private double critMultiplier; // クリティカルダメージ倍率（デフォルト 1.5）
        public double getCritMultiplier() { return critMultiplier > 0 ? critMultiplier : 1.5; }
    }

    public Map<String, Map<String, String>> getDistanceRules() { return distanceRules; }
    public AccuracyRules getAccuracy() { return accuracy; }
    public DamageRules getDamage() { return damage; }
    public double getGlobalStatScaling() { return globalStatScaling; }

    public String getRangeResult(String type, int distance) {
        if (type == null) return "MISS";
        String lowerType = type.toLowerCase();
        if (distanceRules != null && distanceRules.containsKey(lowerType)) {
            return distanceRules.get(lowerType).getOrDefault(String.valueOf(distance), "MISS");
        }
        return "MISS";
    }
}
