package com.kh.tbrr.data.models;

public class CombatConditionData {
    private String id;
    private String name;
    private String description;
    private String type; // "BUFF", "DEBUFF", "NEUTRAL"
    private int duration; // ターン数（-1で永続）
    private CombatConditionModifiers modifiers;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public CombatConditionModifiers getModifiers() { return modifiers; }
    public void setModifiers(CombatConditionModifiers modifiers) { this.modifiers = modifiers; }
}
