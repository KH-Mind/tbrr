package com.kh.tbrr.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import com.kh.tbrr.data.models.PassiveData;

public class PassiveRegistry {
    private static final Map<String, PassiveData> PASSIVES = new HashMap<>();

    public static void register(PassiveData passive) {
        if (passive != null && passive.getId() != null) {
            PASSIVES.put(passive.getId(), passive);
        }
    }

    public static PassiveData getPassiveById(String id) {
        return PASSIVES.get(id);
    }

    public static Collection<PassiveData> getAllPassives() {
        return PASSIVES.values();
    }

    public static void clear() {
        PASSIVES.clear();
    }
}
