package com.kh.tbrr.data;

import com.google.gson.Gson;
import com.kh.tbrr.data.models.CombatConditionData;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CombatConditionRegistry {
    private static final Map<String, CombatConditionData> conditionMap = new HashMap<>();
    private static boolean initialized = false;

    public static void loadAll() {
        if (initialized) return;
        Gson gson = new Gson();
        loadConditionsFromFile("/data/battle/combat_conditions/common_combat_conditions.json", gson);
        initialized = true;
    }

    private static void loadConditionsFromFile(String resourcePath, Gson gson) {
        try {
            InputStream is = CombatConditionRegistry.class.getResourceAsStream(resourcePath);
            if (is == null) {
                System.out.println("[CombatConditionRegistry] File not found: " + resourcePath);
                return; // ファイルがなくてもスキップ（テスト等）
            }
            Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            CombatConditionData[] conditions = gson.fromJson(reader, CombatConditionData[].class);
            if (conditions != null) {
                for (CombatConditionData cond : conditions) {
                    conditionMap.put(cond.getId(), cond);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CombatConditionData getConditionById(String id) {
        return conditionMap.get(id);
    }
}
