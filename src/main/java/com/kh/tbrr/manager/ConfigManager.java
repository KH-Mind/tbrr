package com.kh.tbrr.manager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * グローバル設定のセーブ・ロードを管理するシングルトンクラス
 */
public class ConfigManager {

    private static ConfigManager instance;
    private ConfigData configData;
    
    private static final String CONFIG_DIR = "userdata";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.json";
    
    private final Gson gson;

    private ConfigManager() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        configData = new ConfigData();
        loadConfig();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public ConfigData getConfigData() {
        return configData;
    }

    /**
     * コンフィグをファイルから読み込む
     */
    public void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                ConfigData loadedData = gson.fromJson(reader, ConfigData.class);
                if (loadedData != null) {
                    this.configData = loadedData;
                }
            } catch (IOException e) {
                System.err.println("[ConfigManager] コンフィグの読み込みに失敗しました: " + e.getMessage());
            }
        }
    }

    /**
     * コンフィグをファイルに保存し、同時にAudioManagerへ音量を反映する
     */
    public void saveConfig() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(configData, writer);
        } catch (IOException e) {
            System.err.println("[ConfigManager] コンフィグの保存に失敗しました: " + e.getMessage());
        }
        
        // 保存と同時にAudioManagerに音量を反映
        AudioManager.getInstance().setBGMVolume(configData.getBgmVolume());
        AudioManager.getInstance().setSEVolume(configData.getSeVolume());
    }
}
