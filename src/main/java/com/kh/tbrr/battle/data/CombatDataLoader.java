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
    private static boolean passivesLoaded = false;

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

    private static boolean stancesLoaded = false;

    /**
     * 現在存在するスタンスを初期ロードする。
     * UIの名前からの逆引き用。
     */
    public static void loadAllStances() {
        if (stancesLoaded) return;
        getStance("stance_fast");
        getStance("stance_godspeed");
        getStance("stance_wait");
        stancesLoaded = true;
    }

    /**
     * 表示名からロード済みのスタンスデータを検索して返す。
     */
    public static StanceData getStanceByName(String name) {
        if (name == null || name.isEmpty() || "なし".equals(name)) return null;
        for (StanceData data : stances.values()) {
            if (name.equals(data.getName())) {
                return data;
            }
        }
        return null;
    }

    /**
     * ロード済みの全スタンスデータを返す。
     */
    public static java.util.Collection<StanceData> getAllStances() {
        return stances.values();
    }

    /**
     * 全パッシブJSONを読み込んでPassiveRegistryへ登録する。
     * battle開始時に1度だけ呼ばれる想定（二重読み込み防止済み）。
     */
    public static void loadAllPassives() {
        if (passivesLoaded) return;
        loadPassivesFromFile("/data/battle/passives/basic_passives.json");
        loadPassivesFromFile("/data/battle/passives/class_passives.json");
        loadPassivesFromFile("/data/battle/passives/systemic_passives.json");
        loadPassivesFromFile("/data/battle/passives/initiative_passives.json");
        passivesLoaded = true;
    }

    private static void loadPassivesFromFile(String path) {
        try (InputStream is = CombatDataLoader.class.getResourceAsStream(path)) {
            if (is == null) {
                System.out.println("[CombatDataLoader] Passive file not found (skipped): " + path);
                return;
            }
            PassiveData[] passives = GSON.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8), PassiveData[].class);
            if (passives != null) {
                for (PassiveData p : passives) {
                    PassiveRegistry.register(p);
                }
                System.out.println("[CombatDataLoader] Loaded " + passives.length + " passives from: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
