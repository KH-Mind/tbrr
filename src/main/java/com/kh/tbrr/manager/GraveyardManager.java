package com.kh.tbrr.manager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kh.tbrr.data.models.GraveRecord;

/**
 * 墓地管理クラス（再整備版）
 *
 * 役割：死亡記録（GraveRecord）をJSONファイルとして永続保存し、一覧を読み込む。
 * 保存先：userdata/memory/graveyard/
 *
 * 設計方針：
 *  - GameState（ゲームセッション）に依存せず、staticメソッドでどこからでも呼べる。
 *  - 1キャラの死亡記録 = 1ファイル（UUID.json）で管理する。
 *  - フォルダが存在しない場合は自動生成する。
 */
public class GraveyardManager {

    /** 保存先ディレクトリ */
    private static final String GRAVEYARD_DIR = "userdata/memory/graveyard";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 死亡記録をJSONファイルとして保存する。
     * ファイル名はUUIDで自動生成（重複しない）。
     *
     * @param record 保存するGraveRecord
     */
    public static void saveRecord(GraveRecord record) {
        try {
            ensureGraveyardDirectory();
            String filename = UUID.randomUUID().toString() + ".json";
            File file = new File(GRAVEYARD_DIR, filename);
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(record, writer);
            }
            System.out.println("[GraveyardManager] 墓地に記録しました: " + filename);
        } catch (IOException e) {
            System.err.println("[GraveyardManager] 墓地への保存に失敗しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 墓地フォルダ内の全記録を読み込んでリストで返す。
     * フォルダが存在しない、またはファイルがない場合は空リストを返す。
     *
     * @return GraveRecordのリスト（ファイルが多い順）
     */
    public static List<GraveRecord> loadAllRecords() {
        List<GraveRecord> records = new ArrayList<>();
        File dir = new File(GRAVEYARD_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            return records;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return records;
        }

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                GraveRecord record = gson.fromJson(reader, GraveRecord.class);
                if (record != null) {
                    records.add(record);
                }
            } catch (IOException e) {
                System.err.println("[GraveyardManager] 記録の読み込みに失敗しました: " + file.getName());
            }
        }

        return records;
    }

    /**
     * 保存先ディレクトリが存在しない場合、自動的に作成する。
     *
     * @throws IOException ディレクトリ作成に失敗した場合
     */
    private static void ensureGraveyardDirectory() throws IOException {
        Path path = Paths.get(GRAVEYARD_DIR);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            System.out.println("[GraveyardManager] 墓地フォルダを作成しました: " + GRAVEYARD_DIR);
        }
    }
}
