package com.kh.tbrr.data.models;

public class CombatConditionModifiers {
    private int accuracyBonus;
    private int avoidanceBonus;
    private int damageReduction;
    private double damageMultiplier;
    private boolean preventMovement;
    private boolean preventAction;
    private int dotDamage; // 継続ダメージ（沖、火傍等すべての要因は単一フィールドで管理）

    public CombatConditionModifiers() {
        this.damageMultiplier = 1.0;
    }

    public int getAccuracyBonus() { return accuracyBonus; }
    public void setAccuracyBonus(int accuracyBonus) { this.accuracyBonus = accuracyBonus; }

    public int getAvoidanceBonus() { return avoidanceBonus; }
    public void setAvoidanceBonus(int avoidanceBonus) { this.avoidanceBonus = avoidanceBonus; }

    public int getDamageReduction() { return damageReduction; }
    public void setDamageReduction(int damageReduction) { this.damageReduction = damageReduction; }

    public double getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(double damageMultiplier) { this.damageMultiplier = damageMultiplier; }

    public boolean isPreventMovement() { return preventMovement; }
    public void setPreventMovement(boolean preventMovement) { this.preventMovement = preventMovement; }

    public boolean isPreventAction() { return preventAction; }
    public void setPreventAction(boolean preventAction) { this.preventAction = preventAction; }

    public int getDotDamage() { return dotDamage; }
    public void setDotDamage(int dotDamage) { this.dotDamage = dotDamage; }

    private int hindranceChance; // 阻害確率デフォルト値（%）。intensity が 0 の場合のフォールバック。
    public int getHindranceChance() { return hindranceChance; }
    public void setHindranceChance(int hindranceChance) { this.hindranceChance = hindranceChance; }
}
