package com.kh.tbrr.data;

import java.io.FileReader;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kh.tbrr.data.models.StatusEffect;

/**
 * 状態異常ローダー
 * JSONファイルから状態異常を読み込む
 */
public class StatusEffectLoader {
    /**
     * JSONファイルから状態異常リストを読み込み、レジストリに登録
     */
    public static List<StatusEffect> loadStatusEffects(String path) {
        try (FileReader reader = new FileReader(path)) {
            List<StatusEffect> effects = new Gson().fromJson(reader, new TypeToken<List<StatusEffect>>(){}.getType());
            for (StatusEffect effect : effects) {
                StatusEffectRegistry.register(effect);
            }
            return effects;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}