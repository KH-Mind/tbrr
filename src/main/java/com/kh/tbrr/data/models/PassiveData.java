package com.kh.tbrr.data.models;

import java.util.List;

public class PassiveData {
    private String id;
    private String name;
    private String description;
    private String type; // "MASTERY", "STAT_BOOST", "CRIT_MULTIPLIER" 等
    private List<String> targetTags; // 対象となる武器タグ (例: "dagger", "sword")
    private int level; // マスタリーレベル
    private double critMultiplier; // CRIT_MULTIPLIER型のパッシブ用（例: 2.0）

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public List<String> getTargetTags() {
        return targetTags;
    }
    public void setTargetTags(List<String> targetTags) {
        this.targetTags = targetTags;
    }
    public int getLevel() {
        return level;
    }
    public void setLevel(int level) {
        this.level = level;
    }
    public double getCritMultiplier() {
        return critMultiplier;
    }
    public void setCritMultiplier(double critMultiplier) {
        this.critMultiplier = critMultiplier;
    }
}
