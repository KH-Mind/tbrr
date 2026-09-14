package com.kh.tbrr.data.models;

import java.util.List;

/**
 * ゾーン全体を定義するデータクラス。
 * Gsonがzones/*.jsonから自動パースする。
 */
public class ZoneData {

    /** ゾーンID（例: "tavern_zone"） */
    private String id;

    /** 表示名（例: "冒険者の酒場"） */
    private String name;

    /** サブウィンドウに表示する背景画像ファイル名（例: "tavern.png"） */
    private String backgroundImage;

    /** 霧あり（未探索隠蔽）にするか。falseで全公開 */
    private boolean fogOfWar;

    /** マップ上に配置するノードのリスト */
    private List<ZoneNode> nodes;

    /**
     * イベント終了後にマップを再表示する前に表示するメッセージ。
     * TextReplacer経由で処理されるため [Name] や {カテゴリ} 等のプレースホルダーが使える。
     * 省略した場合は何も表示されない。
     */
    private String mapMessage;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBackgroundImage() { return backgroundImage; }
    public void setBackgroundImage(String backgroundImage) { this.backgroundImage = backgroundImage; }

    public boolean isFogOfWar() { return fogOfWar; }
    public void setFogOfWar(boolean fogOfWar) { this.fogOfWar = fogOfWar; }

    public List<ZoneNode> getNodes() { return nodes; }
    public void setNodes(List<ZoneNode> nodes) { this.nodes = nodes; }

    public String getMapMessage() { return mapMessage; }
    public void setMapMessage(String mapMessage) { this.mapMessage = mapMessage; }
}
