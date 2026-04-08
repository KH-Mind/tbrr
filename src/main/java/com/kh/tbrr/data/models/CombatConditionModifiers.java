package com.kh.tbrr.data.models;

public class CombatConditionModifiers {
    private int accuracyBonus;
    private int avoidanceBonus;
    private int damageReduction;
    private double damageMultiplier;
    private boolean preventMovement;
    private boolean preventAction;
    private int dotPhysical; // 物理継続ダメージ
    private int dotMagical;  // 魔法継続ダメージ

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

    public int getDotPhysical() { return dotPhysical; }
    public void setDotPhysical(int dotPhysical) { this.dotPhysical = dotPhysical; }

    public int getDotMagical() { return dotMagical; }
    public void setDotMagical(int dotMagical) { this.dotMagical = dotMagical; }
}
