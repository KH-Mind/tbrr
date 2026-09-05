package com.kh.tbrr.manager;

/**
 * ゲームのグローバル設定データを保持するクラス
 * Gsonを使って userdata/config.json にシリアライズ・デシリアライズされる
 */
public class ConfigData {
    private double bgmVolume = 0.5;
    private double seVolume = 0.7;

    public ConfigData() {
    }

    public double getBgmVolume() {
        return bgmVolume;
    }

    public void setBgmVolume(double bgmVolume) {
        this.bgmVolume = bgmVolume;
    }

    public double getSeVolume() {
        return seVolume;
    }

    public void setSeVolume(double seVolume) {
        this.seVolume = seVolume;
    }
}
