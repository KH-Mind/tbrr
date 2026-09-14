package com.kh.tbrr.manager;

import com.kh.tbrr.core.GameState;
import com.kh.tbrr.data.models.GameEvent;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.event.EventProcessor;
import com.kh.tbrr.ui.GameUI;
import com.kh.tbrr.utils.TextReplacer;

/**
 * 街フェーズ管理クラス
 * 街に到着した際の行動選択ループを担当する。
 * フロアカウントは消費しない（GameEngineのgameLoop内でadvanceFloor()の前に呼ばれる）。
 *
 * 将来の拡張:
 *   - visitWeaponShop() 等を追加して施設を増やす
 *   - TODO: セーブ機能本実装時: runTownPhase() 実行中のセーブ対応を行う
 *
 * 設計方針:
 *   TownManagerはZoneManagerを知らない。
 *   施設への入場は各施設の入場イベントJSON（例: tavern_enter.json）を
 *   EventProcessor経由で処理することで行い、そのイベント内の
 *   command: "enter_zone" をEventProcessorが検知してゾーンに遷移する。
 */
public class TownManager {

    /** 街フェーズ開始時に付与するtownAPの初期値（暫定）。 */
    private static final int INITIAL_TOWN_AP = 3;

    private final GameUI ui;
    private final DataManager dataManager;
    private final EventProcessor eventProcessor;

    public TownManager(GameUI ui, DataManager dataManager, EventProcessor eventProcessor) {
        this.ui = ui;
        this.dataManager = dataManager;
        this.eventProcessor = eventProcessor;
    }

    /**
     * 街フェーズを実行する。
     * townAPを初期化し、行動力が残っている間（または出発を選ぶまで）施設選択ループを回す。
     *
     * @param player    プレイヤー
     * @param gameState ゲーム状態
     */
    public void runTownPhase(Player player, GameState gameState) {
        // 行動力を初期化
        gameState.setTownAP(INITIAL_TOWN_AP);

        // 街入場イベントを再生（初回1回のみ）
        GameEvent entryEvent = dataManager.loadEvent("town_entry");
        if (entryEvent != null) {
            eventProcessor.processEvent(entryEvent, player, gameState);
        } else {
            ui.print(TextReplacer.replace("[Name]は町に辿り着いた。", player));
            ui.print("");
            ui.waitForEnter();
        }

        // TODO: セーブ機能本実装時: ここで「街でのセーブ」または「街直前セーブ」を実装する
        // 現在は街フェーズ中のセーブは不可（セーブシステム本実装まで暫定）

        // 施設選択ループ
        while (gameState.getTownAP() > 0 && !gameState.isGameOver()) {
            showTownMenu(player, gameState);
        }

        // フェーズ終了メッセージ
        ui.print("");
        ui.print(TextReplacer.replace("[Name]は旅を再開した。", player));
        ui.print("");
        ui.waitForEnter();
    }

    /**
     * 施設選択メニューを表示し、選択を処理する。
     *
     * @param player    プレイヤー
     * @param gameState ゲーム状態
     */
    private void showTownMenu(Player player, GameState gameState) {
        ui.print("");
        ui.print("【町での残り行動可能数：" + gameState.getTownAP() + "】");
        ui.print("━━━━━━━━━━━━━━━━━━━━━━");
        ui.print("どこへ行きますか？");
        ui.print("1. 宿屋に行く");
        ui.print("2. 酒場に入る");
        // 将来の施設はここに追加する
        // 3. 武器屋に行く
        // ...
        ui.print("3. 出発する（行動力が残っていても出発できます）");
        ui.print("━━━━━━━━━━━━━━━━━━━━━━");

        int choice = ui.getPlayerChoice(3, player);

        switch (choice) {
            case 1:
                visitInn(player, gameState);
                break;
            case 2:
                visitTavern(player, gameState);
                break;
            case 3:
                // 出発を選択：townAPを0にしてループを終了させる
                gameState.setTownAP(0);
                break;
            default:
                ui.print("【システム】無効な選択です。");
                break;
        }
    }

    /**
     * 宿屋を利用する。
     * inn01イベントをそのまま呼び出し、終了後にtownAPを1消費する。
     *
     * @param player    プレイヤー
     * @param gameState ゲーム状態
     */
    private void visitInn(Player player, GameState gameState) {
        GameEvent innEvent = dataManager.loadEvent("inn01");
        if (innEvent != null) {
            eventProcessor.processEvent(innEvent, player, gameState);
        } else {
            ui.print("【システム】宿屋のイベントが見つかりませんでした。");
        }
        // 施設利用後、行動力を1消費
        gameState.setTownAP(gameState.getTownAP() - 1);
    }

    /**
     * 酒場に入る。
     * tavern_enter.json を EventProcessor 経由で処理する。
     * そのイベント内の command: "enter_zone" を EventProcessor が検知し、
     * ZoneManager.startZone() を呼び出してゾーン探索へ遷移する。
     * TownManager 自身は ZoneManager を知らない。
     *
     * @param player    プレイヤー
     * @param gameState ゲーム状態
     */
    private void visitTavern(Player player, GameState gameState) {
        GameEvent tavernEnterEvent = dataManager.loadEvent("tavern_enter");
        if (tavernEnterEvent != null) {
            eventProcessor.processEvent(tavernEnterEvent, player, gameState);
        } else {
            ui.print("【システム】酒場の入場イベントが見つかりませんでした。");
        }
        // ゾーン探索から戻った後、行動力を1消費
        gameState.setTownAP(gameState.getTownAP() - 1);
    }
}

