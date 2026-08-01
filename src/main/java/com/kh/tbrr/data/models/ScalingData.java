package com.kh.tbrr.data.models;

import java.util.Map;

/**
 * 汎用スケーリング定義クラス。
 * 固定値（base）と、複数のステータスに対する倍率（scalings）を組み合わせた計算式を表す。
 *
 * 計算式: base + Σ( statValue × scaling )
 *
 * 使用例（JSON）:
 *   "spGain": { "base": 10, "scalings": { "insight": 0.5, "presence": 0.5 } }
 *   → SPを「10 + insight×0.5 + presence×0.5」増加させる
 *
 * 注意: ステータス値の取得はBattleManager.getCombatStatまたはEnemyData.getStatByNameを通じて行う。
 *       このクラス自体はステータス解決を行わず、データ定義のみを持つ。
 */
public class ScalingData {

    /** 固定値（スケーリング前の基本量） */
    private int base;

    /** ステータス名 → 倍率 のマップ。nullの場合は固定値のみを使用する */
    private Map<String, Double> scalings;

    public int getBase() {
        return base;
    }

    public Map<String, Double> getScalings() {
        return scalings;
    }

    /**
     * 敵のステータスを参照して合計値を計算する。
     * 敵の場合はEnemyData.getStatByName() が利用できるためこのクラスで直接計算できる。
     *
     * @param enemy 計算対象の敵
     * @return base + Σ(stat × scaling) の計算結果（最小値0）
     */
    public int calculate(com.kh.tbrr.data.models.EnemyData enemy) {
        int total = base;
        if (scalings != null) {
            for (Map.Entry<String, Double> entry : scalings.entrySet()) {
                int statVal = enemy.getStatByName(entry.getKey());
                total += (int) (statVal * entry.getValue());
            }
        }
        return Math.max(0, total);
    }
}
