package com.kh.tbrr.data.models;

/**
 * 状態異常データモデル
 */
public class StatusEffect {
    private String id;
    private String name;
    private String category; // common, unique
    private String description;
    private int maxValue;
    private int minValue;
    private Integer defaultValue; // 初期値（nullable、未設定の場合はnull）
    private boolean allowZero; // 0でも削除しないか（デフォルト: false）

    // コンストラクタ
    public StatusEffect() {
        this.maxValue = 100;
        this.minValue = 0;
        this.allowZero = false;
    }

    // Getters and Setters
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    public Integer getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Integer defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isAllowZero() {
        return allowZero;
    }

    public void setAllowZero(boolean allowZero) {
        this.allowZero = allowZero;
    }

    @Override
    public String toString() {
        return name + " (" + category + ")";
    }
}