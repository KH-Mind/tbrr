package com.kh.tbrr.data.models;

/**
 * ゾーンマップ上の個別クリックポイント（ノード）を定義するデータクラス。
 * Gsonが自動パースする。
 */
public class ZoneNode {

    /** ノードの一意なID（例: "node_master"） */
    private String id;

    /** マウスカーソルを乗せたときに表示される名前（例: "酒場のマスター"） */
    private String displayName;

    /** サブウィンドウ内のX座標（0〜450） */
    private int x;

    /** サブウィンドウ内のY座標（0〜450） */
    private int y;

    /**
     * アイコン種別（"default", "person", "door" 等）
     * UIでの描画スタイルを決定する
     */
    private String iconType;

    /** クリックした際に呼び出すイベントのID */
    private String eventId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public String getIconType() { return iconType; }
    public void setIconType(String iconType) { this.iconType = iconType; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
}
