package com.kh.tbrr.battle.data;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class CombatDataLoader {
    private static final Gson GSON = new Gson();
    private static CombatBaseRules baseRules;
    private static Map<String, AbilityData> abilities = new HashMap<>();
    private static Map<String, StanceData> stances = new HashMap<>();

    public static CombatBaseRules getBaseRules() {
        if (baseRules == null) {
            baseRules = loadJson("/data/battle/combat_base_rules.json", CombatBaseRules.class);
        }
        return baseRules;
    }

    public static AbilityData getAbility(String id) {
        if (!abilities.containsKey(id)) {
            AbilityData data = loadJson("/data/battle/abilities/" + id + ".json", AbilityData.class);
            if (data != null) {
                abilities.put(id, data);
            }
        }
        return abilities.get(id);
    }

    public static StanceData getStance(String id) {
        if (!stances.containsKey(id)) {
            StanceData data = loadJson("/data/battle/stances/" + id + ".json", StanceData.class);
            if (data != null) {
                stances.put(id, data);
            }
        }
        return stances.get(id);
    }

    private static <T> T loadJson(String path, Class<T> clazz) {
        try (InputStream is = CombatDataLoader.class.getResourceAsStream(path)) {
            if (is != null) {
                return GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), clazz);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
