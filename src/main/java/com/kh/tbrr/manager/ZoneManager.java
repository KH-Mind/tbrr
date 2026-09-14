package com.kh.tbrr.manager;

import com.kh.tbrr.core.GameState;
import com.kh.tbrr.data.models.GameEvent;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.data.models.ZoneData;
import com.kh.tbrr.data.models.ZoneNode;
import com.kh.tbrr.event.EventProcessor;
import com.kh.tbrr.ui.GameUI;
import com.kh.tbrr.utils.TextReplacer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ゾーン探索システムの管理クラス。
 * TownManager に似た構造を持ち、ゾーン探索の主ループを担当する。
 *
 * 設計方針:
 *   ゾーンへの入口は必ず EventProcessor の enter_zone コマンド経由とする。
 *   このクラスは EventProcessor から直接呼び出されるため、TownManager は
 *   ZoneManager を一切知らない。
 */
public class ZoneManager {

    private final GameUI ui;
    private final DataManager dataManager;
    private final EventProcessor eventProcessor;

    /** exit_zone コマンドで返す戻り値の定数 */
    public static final String EXIT_ZONE_SIGNAL = "exit_zone";

    public ZoneManager(GameUI ui, DataManager dataManager, EventProcessor eventProcessor) {
        this.ui = ui;
        this.dataManager = dataManager;
        this.eventProcessor = eventProcessor;
    }

    /**
     * ゾーン探索を開始する。
     * EventProcessor の enter_zone コマンドハンドラから呼び出される。
     *
     * @param zoneId    ゾーンID（例: "tavern_zone"）
     * @param player    プレイヤー
     * @param gameState ゲーム状態
     */
    public void startZone(String zoneId, Player player, GameState gameState) {
        // ゾーンデータ取得
        ZoneData zone = dataManager.loadZone(zoneId);
        if (zone == null) {
            System.err.println("[ZoneManager] ゾーンデータが見つかりません: " + zoneId);
            ui.print("【システム】ゾーンデータの読み込みに失敗しました。");
            return;
        }

        System.out.println("[ZoneManager] ゾーン開始: " + zone.getName());

        // ゾーン探索ループ
        boolean exitRequested = false;
        while (!exitRequested && !gameState.isGameOver()) {

            // マップを表示し、ノードクリックを待機
            ZoneNode clickedNode = waitForNodeClick(zone);

            if (clickedNode == null) {
                // クリックがnullになるのは異常系（割り込み等）
                System.err.println("[ZoneManager] ノードクリックの待機が中断されました。");
                break;
            }

            System.out.println("[ZoneManager] ノードクリック: " + clickedNode.getId()
                    + " -> イベント: " + clickedNode.getEventId());

            // ノードのイベントを処理
            String result = processNodeEvent(clickedNode, player, gameState);

            // exit_zone シグナルが返ってきたらループを抜ける
            if (EXIT_ZONE_SIGNAL.equals(result)) {
                exitRequested = true;
            } else {
                // イベント終了後、マップ再表示前にゾーン固有のメッセージを表示
                printMapMessage(zone, player);
            }
        }

        // ゾーン終了処理
        ui.clearZoneMap();

        // zone: プレフィックスのローカルフラグを一括削除
        gameState.clearFlagsWithPrefix("zone:");
        System.out.println("[ZoneManager] ゾーン終了。zone:フラグをクリアしました。");
    }

    /**
     * UIにマップを表示し、プレイヤーがノードをクリックするまで待機する。
     *
     * @param zone ゾーン定義
     * @return クリックされたノード。待機が中断された場合はnull
     */
    private ZoneNode waitForNodeClick(ZoneData zone) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ZoneNode> clickedNode = new AtomicReference<>();

        // UIにゾーンマップを表示し、クリックコールバックを設定
        ui.showZoneMap(zone, node -> {
            clickedNode.set(node);
            latch.countDown();
        });

        // ゲームスレッドでクリックを待機
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[ZoneManager] ノードクリック待機が割り込まれました。");
            return null;
        }

        return clickedNode.get();
    }

    /**
     * クリックされたノードのイベントを EventProcessor で処理する。
     * isInRecursiveEvent=true にすることで、イベント終了後の
     * 「Enterキーを押してください」プロンプトを抑制する。
     *
     * @param node      クリックされたノード
     * @param player    プレイヤー
     * @param gameState ゲーム状態
     * @return EventProcessor の processEvent 戻り値。"exit_zone" なら退出シグナル
     */
    private String processNodeEvent(ZoneNode node, Player player, GameState gameState) {
        if (node.getEventId() == null || node.getEventId().isEmpty()) {
            System.err.println("[ZoneManager] ノード " + node.getId() + " にイベントIDが設定されていません。");
            return null;
        }

        GameEvent event = dataManager.loadEvent(node.getEventId());
        if (event == null) {
            System.err.println("[ZoneManager] イベントが見つかりません: " + node.getEventId());
            ui.print("【システム】イベントデータの読み込みに失敗しました。");
            return null;
        }

        // isInRecursiveEvent=true にしてEnterプロンプトを抑制する
        // ゾーン内イベントはマップへの「帰還」がメインの進行なので、
        // Enter確認よりもマップ再表示の方が自然な流れになる
        boolean wasInRecursive = gameState.isInRecursiveEvent();
        gameState.setInRecursiveEvent(true);
        String result = eventProcessor.processEvent(event, player, gameState);
        gameState.setInRecursiveEvent(wasInRecursive);

        return result;
    }

    /**
     * ゾーンのmapMessageをTextReplacer経由で表示する。
     * mapMessageが設定されていない場合は何もしない。
     *
     * @param zone   ゾーンデータ
     * @param player プレイヤー（プレースホルダー置換に使用）
     */
    private void printMapMessage(ZoneData zone, Player player) {
        if (zone.getMapMessage() != null && !zone.getMapMessage().isEmpty()) {
            ui.print("");
            ui.print(TextReplacer.replace(zone.getMapMessage(), player));
        }
    }
}
