package com.kh.tbrr.battle;

public enum StatType {
    MIGHT("強靭"),
    FINESSE("機敏"),
    INSIGHT("聡明"),
    PRESENCE("風格"),
    SENSUALITY("官能");

    private String displayName;
    
    StatType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
