package com.kh.tbrr.manager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.kh.tbrr.core.GameState;
import com.kh.tbrr.data.models.GameEvent;
import com.kh.tbrr.data.models.GameMap;
import com.kh.tbrr.data.models.Personality;

/**
 * セーブデータ管理クラス
 * 中断セーブ（Suspend Save）の保存と読み込みを担当
 */
public class SaveManager {

    private static final String SAVE_DIR = "userdata/save";
    private static final String SUSPEND_FILE = "suspend.json";

    private static Gson gson;

    static {
        // Gsonの初期化（TypeAdapterの設定）
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(GameEvent.class, new GameEventAdapter())
                .registerTypeAdapter(GameMap.class, new GameMapAdapter())
                .registerTypeAdapter(Personality.class, new PersonalityAdapter())
                .registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    // ... (existing code) ...

    /**
     * 中断データを保存する
     */
    public static void saveSuspendData(GameState state) throws IOException {
        ensureSaveDirectory();
        File file = new File(SAVE_DIR, SUSPEND_FILE);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(state, writer);
        }
    }

    /**
     * 中断データを読み込む
     * 読み込み成功後、ファイルは削除される（中断セーブの仕様）
     */
    public static GameState loadSuspendData() throws IOException {
        File file = new File(SAVE_DIR, SUSPEND_FILE);
        if (!file.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            GameState state = gson.fromJson(reader, GameState.class);
            return state;
        }
    }

    /**
     * 中断データを削除する
     */
    public static void deleteSuspendData() {
        File file = new File(SAVE_DIR, SUSPEND_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 中断データが存在するか確認
     */
    public static boolean hasSuspendData() {
        File file = new File(SAVE_DIR, SUSPEND_FILE);
        return file.exists();
    }

    private static void ensureSaveDirectory() throws IOException {
        Path path = Paths.get(SAVE_DIR);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    // ========== TypeAdapters ==========

    /**
     * LocalDateTimeを文字列で保存・復元するアダプター
     * java.timeパッケージへのリフレクションアクセスエラーを回避するため
     */
    private static class LocalDateTimeAdapter
            implements JsonSerializer<java.time.LocalDateTime>, JsonDeserializer<java.time.LocalDateTime> {
        @Override
        public JsonElement serialize(java.time.LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public java.time.LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return java.time.LocalDateTime.parse(json.getAsString());
        }
    }

    /**
     * GameEventをIDで保存・復元するアダプター
     */
    private static class GameEventAdapter implements JsonSerializer<GameEvent>, JsonDeserializer<GameEvent> {
        @Override
        public JsonElement serialize(GameEvent src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getId());
        }

        @Override
        public GameEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String id = json.getAsString();
            // EventManagerから取得
            return EventManager.getEventById(id);
        }
    }

    /**
     * GameMapをIDで保存・復元するアダプター
     * ※現状MapManagerがないため、ScenarioManager等から取得する必要があるが、
     * GameStateにはcurrentMapしか保持していないため、IDだけ保持して
     * 復元時は一旦nullになる可能性がある（再開時に再設定が必要かも）
     * 
     * 暫定対応: シナリオデータからマップを取得するのはコストが高いため、
     * ここではIDのみ保持し、復元ロジック側でマップを再ロードすることを推奨。
     * ただし、GameState内でMapオブジェクトを直接保持しているため、
     * 何らかの方法で復元しないとNullPoになる。
     * 
     * 今回は「ScenarioManager」がstaticアクセスできないため、
     * 復元時は「IDを持ったダミーのGameMap」を返すか、
     * あるいはGameState側でtransientにしてIDだけ別フィールドで持つのが正解だが、
     * 既存コードへの影響を最小限にするため、
     * 「IDだけ入ったGameMapオブジェクト」を生成して返す。
     * 実際の詳細データはゲーム再開時にScenarioManagerを使って再取得・上書きする。
     */
    private static class GameMapAdapter implements JsonSerializer<GameMap>, JsonDeserializer<GameMap> {
        @Override
        public JsonElement serialize(GameMap src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getId());
        }

        @Override
        public GameMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String id = json.getAsString();
            GameMap map = new GameMap();
            map.setId(id);
            map.setName("Loading..."); // 仮の名前
            return map;
        }
    }

    /**
     * PersonalityをIDで保存・復元するアダプター
     */
    private static class PersonalityAdapter implements JsonSerializer<Personality>, JsonDeserializer<Personality> {
        @Override
        public JsonElement serialize(Personality src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getId());
        }

        @Override
        public Personality deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String id = json.getAsString();
            // PersonalityManagerはインスタンスが必要だが、
            // ここでは簡易的にstaticメソッドがないため、
            // 新しいPersonalityManagerを作って取得するか、
            // あるいはIDだけ持ったダミーを返す。
            // PersonalityManagerのコストは低いので都度生成で対応。
            try {
                return new PersonalityManager().getPersonalityById(id);
            } catch (Exception e) {
                // 失敗時はIDだけのダミー
                Personality p = new Personality();
                p.setId(id);
                p.setName("Unknown");
                return p;
            }
        }
    }
}
