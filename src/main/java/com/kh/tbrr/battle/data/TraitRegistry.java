package com.kh.tbrr.battle.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 全特徴（Trait）データを保持するレジストリ。
 * 旧クラス名: PassiveRegistry
 *
 * CombatDataLoader.loadAllTraits() によってJSONから一括登録される。
 * BattleManager等からはIDで参照する。
 */
public class TraitRegistry {

    private static final Map<String, TraitData> TRAITS = new HashMap<>();

    public static void register(TraitData trait) {
        if (trait != null && trait.getId() != null) {
            TRAITS.put(trait.getId(), trait);
        }
    }

    public static TraitData getTraitById(String id) {
        return TRAITS.get(id);
    }

    public static Collection<TraitData> getAllTraits() {
        return TRAITS.values();
    }

    /** テスト・リセット用（通常は使用しない） */
    public static void clear() {
        TRAITS.clear();
    }
}
