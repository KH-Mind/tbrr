package com.kh.tbrr.battle;

public class EnemyData {
    private String id;
    private String name;
    private int hp;
    private int maxHp;
    private int might;
    private int insight;
    private int finesse;
    private int presence;
    private int sensuality;
    private int moveSpeed = 1;
    private int actionCount = 1;
    private String imagePath;
    private String battleBackground;
    private String deathCause = "generic"; // 敗北時の死因（デフォルトはgeneric）
    private java.util.List<String> passives; // 敵が所持するパッシブ
    private java.util.List<com.kh.tbrr.battle.BattleState.ActiveCombatCondition> initialCombatConditions;

    public String getId() { return id; }
    public String getName() { return name; }
    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }
    public int getMaxHp() { return maxHp; }
    public int getMight() { return might; }
    public int getInsight() { return insight; }
    public int getFinesse() { return finesse; }
    public int getPresence() { return presence; }
    public int getSensuality() { return sensuality; }
    public int getMoveSpeed() { return moveSpeed; }
    public int getActionCount() { return actionCount; }
    public String getImagePath() { return imagePath; }
    public String getBattleBackground() { return battleBackground; }
    public String getDeathCause() { return deathCause; }
    public java.util.List<String> getPassives() { return passives; }
    public java.util.List<com.kh.tbrr.battle.BattleState.ActiveCombatCondition> getInitialCombatConditions() { return initialCombatConditions; }

    public int getStatByName(String statName) {
        return switch (statName.toUpperCase()) {
            case "MIGHT" -> might;
            case "INSIGHT" -> insight;
            case "FINESSE" -> finesse;
            case "PRESENCE" -> presence;
            case "SENSUALITY" -> sensuality;
            default -> 0;
        };
    }
}
