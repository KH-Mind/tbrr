package com.kh.tbrr.battle;

public class BattleState {
    private int turnCount;
    private int distance; // 0: 至近, 1: 近, 2: 中, 3: 遠, 4: 超遠
    private EnemyData currentEnemy;

    // --- バトル状態フラグ ---
    private boolean playerDefending = false;
    private boolean enemyDefending = false;
    
    // --- 状態異常（転倒、掴み、飛行など）用リスト ---
    private java.util.List<ActiveCombatCondition> playerConditions = new java.util.ArrayList<>();
    private java.util.List<ActiveCombatCondition> enemyConditions = new java.util.ArrayList<>();

    // --- 自身のターン後もスタンス(構え)を記憶するための変数 ---
    private String currentPlayerStance = "なし";

    public BattleState() {
        this.turnCount = 1;
        this.distance = 1; // 基本は1: 近距離スタート
    }

    public int getTurnCount() { return turnCount; }
    public void incrementTurn() { turnCount++; }
    
    public int getDistance() { return distance; }
    public void setDistance(int distance) { this.distance = distance; }
    
    public EnemyData getCurrentEnemy() { return currentEnemy; }
    public void setCurrentEnemy(EnemyData currentEnemy) { this.currentEnemy = currentEnemy; }

    // --- 防御関連 Getter/Setter ---
    public boolean isPlayerDefending() { return playerDefending; }
    public void setPlayerDefending(boolean defending) { this.playerDefending = defending; }
    
    public boolean isEnemyDefending() { return enemyDefending; }
    public void setEnemyDefending(boolean defending) { this.enemyDefending = defending; }

    // --- 状態異常関連 ---
    public java.util.List<ActiveCombatCondition> getPlayerConditions() { return playerConditions; }
    public java.util.List<ActiveCombatCondition> getEnemyConditions() { return enemyConditions; }

    // --- スタンス記憶関連 ---
    public String getCurrentPlayerStance() { return currentPlayerStance; }
    public void setCurrentPlayerStance(String stance) { this.currentPlayerStance = stance; }

    // --- 内部データクラス ---
    public static class ActiveCombatCondition {
        private String conditionId;
        private int duration; // 残り効果ターン数 (-1は永続)

        public ActiveCombatCondition(String conditionId, int duration) {
            this.conditionId = conditionId;
            this.duration = duration;
        }

        public String getConditionId() { return conditionId; }
        public int getDuration() { return duration; }
        
        public void setDuration(int duration) { this.duration = duration; }
        public void decrementDuration() { 
            if (this.duration > 0) this.duration--; 
        }
    }
}
