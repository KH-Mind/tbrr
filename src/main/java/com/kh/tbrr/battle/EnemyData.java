package com.kh.tbrr.battle;

public class EnemyData {
    private String id;
    private String name;
    private int hp;
    private int maxHp;
    private int might;
    private int finesse;
    private int insight;
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

    // 脅威度スケーリング: 脅威度1ごとに加算されるステータス値（nullの場合はスケーリングなし）
    public static class EnemyScaling {
        private int hpPerLevel = 0;
        private int mightPerLevel = 0;
        private int finessePerLevel = 0;
        private int insightPerLevel = 0;
        private int presencePerLevel = 0;
        private int sensualityPerLevel = 0;

        public int getHpPerLevel() { return hpPerLevel; }
        public int getMightPerLevel() { return mightPerLevel; }
        public int getFinessePerLevel() { return finessePerLevel; }
        public int getInsightPerLevel() { return insightPerLevel; }
        public int getPresencePerLevel() { return presencePerLevel; }
        public int getSensualityPerLevel() { return sensualityPerLevel; }
    }

    private EnemyScaling scaling; // nullの場合はスケーリングなし（ボス・固有敵等）

    // 敵ランク（作者側の難易度目印。0=未設定。将来の闘技場フィルタ等での活用を想定）
    private int rank = 0;

    // 固定値ダメージ軽減（金属系の敵等にのみ設定。プレイヤーの攻撃ダメージからこの値を引く）
    private int damageReduction = 0; // デフォルト0（JSONに書かなければ機能しない）

    // SP（シールドポイント）
    private int initialSp = 0;  // JSONで設定可能（省略時は0）
    private int currentSp = 0;  // 戦闘中の現在SP

    // --- AIロジック用追加フィールド ---
    public static class AIActionChoice {
        private String ability;
        private int weight;
        private String nameOverride; // 画面表示用のアビリティ名上書き
        private java.util.List<String> description; // 自由描写テキスト

        public String getAbility() { return ability; }
        public int getWeight() { return weight; }
        public String getNameOverride() { return nameOverride; }
        public java.util.List<String> getDescription() { return description; }
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
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public int getMight() { return might; }
    public void setMight(int might) { this.might = might; }
    public int getFinesse() { return finesse; }
    public void setFinesse(int finesse) { this.finesse = finesse; }
    public int getInsight() { return insight; }
    public void setInsight(int insight) { this.insight = insight; }
    public int getPresence() { return presence; }
    public void setPresence(int presence) { this.presence = presence; }
    public int getSensuality() { return sensuality; }
    public void setSensuality(int sensuality) { this.sensuality = sensuality; }
    public int getMoveSpeed() { return moveSpeed; }
    public int getActionCount() { return actionCount; }
    public String getImagePath() { return imagePath; }
    public String getBattleBackground() { return battleBackground; }
    public String getDeathCause() { return deathCause; }
    public boolean isCanFlee() { return canFlee; }
    public java.util.List<String> getTraits() { return traits; }
    public java.util.List<com.kh.tbrr.battle.BattleState.ActiveCombatCondition> getInitialCombatConditions() { return initialCombatConditions; }

    // ランク・スケーリング・ダメージ軽減
    public int getRank() { return rank; }
    public EnemyScaling getScaling() { return scaling; }

    // damageReduction
    public int getDamageReduction() { return damageReduction; }
    public void setDamageReduction(int damageReduction) { this.damageReduction = damageReduction; }

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
    public DamageResult applyBattleDamage(int damage, boolean isPenetrating) {
        int spAbsorbed = 0;
        if (!isPenetrating && currentSp > 0) {
            spAbsorbed = Math.min(currentSp, damage);
            setCurrentSp(currentSp - spAbsorbed);
        }
        int remainingDamage = damage - spAbsorbed;
        int oldHp = hp;
        if (remainingDamage > 0) {
            hp = Math.max(0, hp - remainingDamage);
        }
        int hpDamage = oldHp - hp;
        return new DamageResult(hpDamage, spAbsorbed);
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
            case "FINESSE" -> finesse;
            case "INSIGHT" -> insight;
            case "PRESENCE" -> presence;
            case "SENSUALITY" -> sensuality;
            default -> 0;
        };
    }
}
