package com.kh.tbrr.interaction;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.kh.tbrr.data.models.Player;

/**
 * インタラクションハンドラーのインターフェース
 * 全てのミニゲーム・判定・戦闘はこれを実装する
 */
public interface InteractionHandler {

    /**
     * インタラクション種別ID（例: "coin_toss", "battle"）
     * 
     * @return インタラクションタイプ文字列
     */
    String getType();

    /**
     * インタラクションを実行（非同期）
     * 
     * @param params JSON由来のパラメータ
     * @param player 現在のプレイヤー
     * @return 結果を返すFuture
     */
    CompletableFuture<InteractionResult> execute(Map<String, Object> params, Player player);
}
