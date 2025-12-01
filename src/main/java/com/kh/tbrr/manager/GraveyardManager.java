package com.kh.tbrr.manager;

import java.util.List;

import com.kh.tbrr.core.GameState;
import com.kh.tbrr.data.models.GraveRecord;

/**
 * 墓地管理クラス
 * 死亡キャラの記録・蘇生・閲覧を担当
 */
public class GraveyardManager {
    private GameState gameState;

    public GraveyardManager(GameState gameState) {
        this.gameState = gameState;
    }

    /**
     * 墓地一覧を取得
     */
    public List<GraveRecord> getAllRecords() {
        return gameState.getGraveyardRecords();
    }

    /**
     * キャラIDで蘇生を試みる
     */
    public boolean revive(String charId) {
        return gameState.reviveCharacter(charId);
    }

    /**
     * 蘇生可能かどうか判定（UI用）
     */
    public boolean canRevive(String charId) {
        GraveRecord record = gameState.getGraveyardRecords().stream()
            .filter(r -> r.getId().equals(charId))
            .findFirst()
            .orElse(null);
        return record != null && !record.isRevived() && record.isFated();
    }
}
