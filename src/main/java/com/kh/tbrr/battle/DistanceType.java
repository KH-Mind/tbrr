package com.kh.tbrr.battle;

public enum DistanceType {
    MELEE("近接"),
    RANGED("射撃"),
    SPECIAL("特殊");

    private String displayName;
    
    DistanceType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
