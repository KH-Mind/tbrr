package com.kh.tbrr.battle.data;

import java.util.List;

public class StanceData {
    private String id;
    private String name;
    private String overrideAbilityId;
    private int apCost;
    private String message;
    private List<String> description;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getOverrideAbilityId() { return overrideAbilityId; }
    public int getApCost() { return apCost; }
    public String getMessage() { return message; }
    public List<String> getDescription() { return description; }
}
