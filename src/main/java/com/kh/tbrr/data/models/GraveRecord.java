package com.kh.tbrr.data.models;

import java.time.LocalDateTime;

public class GraveRecord {
    private String id;
    private String name;
    private boolean fated;
    private String deathCause;
    private int floor;
    private LocalDateTime timestamp;
    private boolean revived;

    public GraveRecord(String id, String name, boolean fated, String deathCause, int floor, LocalDateTime timestamp) {
        this.id = id;
        this.name = name;
        this.fated = fated;
        this.deathCause = deathCause;
        this.floor = floor;
        this.timestamp = timestamp;
        this.revived = false;
    }

    // Getter
    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isFated() { return fated; }
    public String getDeathCause() { return deathCause; }
    public int getFloor() { return floor; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isRevived() { return revived; }

    // Setter
    public void setRevived(boolean revived) { this.revived = revived; }
}
