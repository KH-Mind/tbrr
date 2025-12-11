package com.kh.tbrr.interaction;

import java.util.HashMap;
import java.util.Map;

/**
 * インタラクションハンドラーを登録・検索するレジストリ
 */
public class InteractionRegistry {

    private static final Map<String, InteractionHandler> handlers = new HashMap<>();

    /**
     * ハンドラーを登録
     * 
     * @param handler 登録するハンドラー
     */
    public static void register(InteractionHandler handler) {
        handlers.put(handler.getType(), handler);
        System.out.println("[InteractionRegistry] 登録: " + handler.getType());
    }

    /**
     * タイプからハンドラーを取得
     * 
     * @param type インタラクションタイプ
     * @return ハンドラー（見つからない場合はnull）
     */
    public static InteractionHandler get(String type) {
        return handlers.get(type);
    }

    /**
     * ハンドラーが登録されているか確認
     * 
     * @param type インタラクションタイプ
     * @return 登録されている場合true
     */
    public static boolean has(String type) {
        return handlers.containsKey(type);
    }

    /**
     * 全てのハンドラーを初期化・登録
     * アプリケーション起動時に呼び出す
     */
    public static void initialize() {
        // Phase 2: ミニゲーム
        register(new CoinTossInteraction());
        register(new ButtonMashInteraction());
        register(new SlidePuzzleInteraction());
        register(new ForkPathInteraction());
        register(new TimingBarInteraction());
        register(new QuickTimeEventInteraction());

        // Phase 3: 戦闘システム
        register(new SimpleCombatInteraction());
        System.out.println("[InteractionRegistry] 初期化完了（登録数: " + handlers.size() + "）");
    }

    /**
     * 登録されているハンドラー数を取得
     * 
     * @return ハンドラー数
     */
    public static int size() {
        return handlers.size();
    }
}
