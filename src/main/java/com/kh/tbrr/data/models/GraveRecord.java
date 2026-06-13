package com.kh.tbrr.data.models;

/**
 * 墓地レコード（死亡記録）モデル
 * ユーザーデータとして userdata/memory/graveyard/ にJSONで保存される。
 * 保存項目は4項目：キャラクター名・職・到達フロア・死亡イベント
 */
public class GraveRecord {
    /** キャラクター名（日本語名） */
    private String characterName;
    /** 職業名 */
    private String characterJob;
    /** 死亡時の到達フロア数 */
    private int floor;
    /** 死亡したイベントのキー（deathCause） */
    private String deathEvent;

    /**
     * コンストラクタ
     *
     * @param characterName キャラクター名
     * @param characterJob  職業名
     * @param floor         死亡時の到達フロア数
     * @param deathEvent    死亡イベントキー（deathCause）
     */
    public GraveRecord(String characterName, String characterJob, int floor, String deathEvent) {
        this.characterName = characterName;
        this.characterJob = characterJob;
        this.floor = floor;
        this.deathEvent = deathEvent;
    }

    // ========== Getter ==========

    public String getCharacterName() {
        return characterName;
    }

    public String getCharacterJob() {
        return characterJob;
    }

    public int getFloor() {
        return floor;
    }

    public String getDeathEvent() {
        return deathEvent;
    }
}
