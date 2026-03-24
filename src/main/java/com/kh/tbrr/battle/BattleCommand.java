package com.kh.tbrr.battle;

public class BattleCommand {
    private String move;
    private String action;
    private String stance;
    private String special;

    public BattleCommand(String move, String action, String stance, String special) {
        this.move = move;
        this.action = action;
        this.stance = stance;
        this.special = special;
    }

    public String getMove() { return move; }
    public String getAction() { return action; }
    public String getStance() { return stance; }
    public String getSpecial() { return special; }
    
    @Override
    public String toString() {
        return String.format("[ムーブ: %s | アクション: %s | スタンス: %s | 特殊: %s]", move, action, stance, special);
    }
}
