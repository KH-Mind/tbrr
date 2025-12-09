package com.kh.tbrr.interaction;

import java.util.Map;

/**
 * インタラクションの結果を保持するクラス
 * resultKeyは各インタラクションが定義する結果キー（例: "success", "failure", "perfect"など）
 */
public class InteractionResult {

    private String resultKey; // "success", "failure", "perfect", "victory" など
    private Map<String, Object> data; // 追加データ（スコア等）

    /**
     * 結果キーのみで作成
     * 
     * @param resultKey 結果キー
     */
    public InteractionResult(String resultKey) {
        this.resultKey = resultKey;
    }

    /**
     * 結果キーと追加データで作成
     * 
     * @param resultKey 結果キー
     * @param data      追加データ
     */
    public InteractionResult(String resultKey, Map<String, Object> data) {
        this.resultKey = resultKey;
        this.data = data;
    }

    public String getResultKey() {
        return resultKey;
    }

    public void setResultKey(String resultKey) {
        this.resultKey = resultKey;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "InteractionResult{resultKey='" + resultKey + "', data=" + data + "}";
    }
}
