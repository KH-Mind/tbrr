package com.kh.tbrr.data.models;

import java.util.List;
import java.util.Map;

/**
 * キャラクターの性格を表すクラス
 */
public class Personality {
    private String id;
    private String name;
    private String description;
    private Map<String, List<String>> dialogue; // 口上データ
    
    public Personality() {
    }
    
    public Personality(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
    
    // ゲッター・セッター
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
    
    public Map<String, List<String>> getDialogue() {
        return dialogue;
    }
    
    public void setDialogue(Map<String, List<String>> dialogue) {
        this.dialogue = dialogue;
    }
    
    /**
     * 指定されたキーに対応する口上リストを取得
     * @param key 口上のキー (例: "attack", "damaged_light")
     * @return 口上のリスト、存在しない場合はnull
     */
    public List<String> getDialogue(String key) {
        if (dialogue == null) {
            return null;
        }
        return dialogue.get(key);
    }
    
    @Override
    public String toString() {
        return name + " (" + id + "): " + description;
    }
}