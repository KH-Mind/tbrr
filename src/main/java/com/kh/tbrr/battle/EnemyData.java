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
    private boolean canFlee = false; // 逃走可能かどうか（明示的にtrueと書かないと逃走不可）
    private java.util.List<String> traits; // 敵が所持する特徴（Trait）
    private java.util.List<com.kh.tbrr.battle.BattleState.ActiveCombatCondition> initialCombatConditions;

    // SP（シールドポイント）
    private int initialSp = 0;  // JSONで設定可能（省略時は0）
    private int currentSp = 0;  // 戦闘中の現在SP

    // --- AIロジック用追加フィールド ---
    public static class AIActionChoice {
        private String ability;
        private int weight;
        private String nameOverride; // 画面表示用のアビリティ名上書き
        
        public String getAbility() { return ability; }
        public int getWeight() { return weight; }
        public String getNameOverride() { return nameOverride; }
    }

    public static class AIActionRule {
        private String condition;
        private java.util.List<AIActionChoice> actions;
        private int maxUses = -1;
        public String getCondition() { return condition; }
        public java.util.List<AIActionChoice> getActions() { return actions; }
        public int getMaxUses() { return maxUses; }
    }

    private String aiType = "predator"; // デフォルトの移動ロジック
    private java.util.List<AIActionRule> actionRules;
    private transient java.util.Map<AIActionRule, Integer> ruleUsageCounts;

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
    public boolean isCanFlee() { return canFlee; }
    public java.util.List<String> getTraits() { return traits; }
    public java.util.List<com.kh.tbrr.battle.BattleState.ActiveCombatCondition> getInitialCombatConditions() { return initialCombatConditions; }

    // SP関連
    public int getInitialSp() { return initialSp; }
    public int getCurrentSp() { return currentSp; }

    /**
     * 敵の現在SPをセットする（0〜9999でクランプ）。
     */
    public void setCurrentSp(int sp) {
        this.currentSp = Math.max(0, Math.min(9999, sp));
    }

    /**
     * 敵のSPを増減する。
     */
    public void modifySp(int amount) {
        setCurrentSp(this.currentSp + amount);
    }

    /**
     * 戦闘中の被ダメージ処理（SP → HP の順）。
     * @param damage       受けるダメージ量（正の整数）
     * @param isPenetrating trueの場合はSPを無視してHPに直接ダメージ（将来の貫通攻撃実装用）
     * @return 実際にHPに通ったダメージ量（ログ表示用）
     */
    public int applyBattleDamage(int damage, boolean isPenetrating) {
        if (isPenetrating || currentSp <= 0) {
            int newHp = Math.max(0, hp - damage);
            int actualDamage = hp - newHp;
            hp = newHp;
            return actualDamage;
        }
        int spAbsorbed = Math.min(currentSp, damage);
        int overflow = damage - spAbsorbed;
        setCurrentSp(currentSp - spAbsorbed);
        if (overflow > 0) {
            hp = Math.max(0, hp - overflow);
        }
        return overflow;
    }

    public String getAiType() { return aiType; }
    public java.util.List<AIActionRule> getActionRules() { return actionRules; }
    public java.util.Map<AIActionRule, Integer> getRuleUsageCounts() {
        if (ruleUsageCounts == null) {
            ruleUsageCounts = new java.util.HashMap<>();
        }
        return ruleUsageCounts;
    }

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
