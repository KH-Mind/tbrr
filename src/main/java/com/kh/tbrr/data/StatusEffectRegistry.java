package com.kh.tbrr.data;

import java.util.HashMap;
import java.util.Map;

import com.kh.tbrr.data.models.StatusEffect;

/**
 * 状態異常レジストリ
 * IDと状態異常オブジェクトのマッピングを管理
 */
public class StatusEffectRegistry {
    private static final Map<String, String> statusEffectNameMap = new HashMap<>();
    private static final Map<String, StatusEffect> statusEffectMap = new HashMap<>();

    /**
     * 状態異常を登録
     */
    public static void register(StatusEffect effect) {
        statusEffectNameMap.put(effect.getId(), effect.getName());
        statusEffectMap.put(effect.getId(), effect);
    }

    /**
     * IDから名前を取得
     */
    public static String getNameById(String id) {
        return statusEffectNameMap.get(id);
    }

    /**
     * IDから状態異常オブジェクトを取得
     */
    public static StatusEffect getStatusEffectById(String id) {
        return statusEffectMap.get(id);
    }

    /**
     * レジストリをクリア
     */
    public static void clear() {
        statusEffectNameMap.clear();
        statusEffectMap.clear();
    }
}