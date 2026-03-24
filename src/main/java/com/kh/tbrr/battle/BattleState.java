package com.kh.tbrr.battle;

public class BattleState {
    private int turnCount;
    private int distance; // 0: 至近, 1: 近, 2: 中, 3: 遠, 4: 超遠
    private EnemyData currentEnemy;

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
}
