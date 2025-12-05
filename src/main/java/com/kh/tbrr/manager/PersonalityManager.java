package com.kh.tbrr.manager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.kh.tbrr.data.models.Personality;

/**
 * 性格データ管理クラス
 * personalities.jsonの読み込みと性格データの提供
 */
public class PersonalityManager {
    private Map<String, Personality> personalities;
    private List<Personality> personalityList;
    private Gson gson;

    public PersonalityManager() {
        this.gson = new Gson();
        this.personalities = new HashMap<>();
        this.personalityList = new ArrayList<>();
        loadPersonalities();
    }

    /**
     * personalities/*.jsonから分割ファイルを読み込む
     * 
     * 対応ファイル: cheerful.json, noble.json, lady.json
     */
    private void loadPersonalities() {
        String[] files = { "cheerful", "noble", "lady" };

        for (String fileId : files) {
            // JavaFX対応: getClass().getResourceAsStreamを使用（先頭に/を付ける）
            try (InputStream is = getClass()
                    .getResourceAsStream("/data/personalities/" + fileId + ".json")) {

                if (is == null) {
                    System.err.println("⚠ ファイルが見つかりません: /data/personalities/" + fileId + ".json");
                    continue;
                }

                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                Personality personality = gson.fromJson(reader, Personality.class);

                if (personality != null && personality.getId() != null) {
                    personalities.put(personality.getId(), personality);
                    personalityList.add(personality);
                    System.out.println("✅ 性格読み込み成功: " + personality.getName() + " (ID: " + personality.getId() + ")");
                } else {
                    System.err.println("⚠ 性格データが不正です: " + fileId + ".json");
                }

            } catch (Exception e) {
                System.err.println("⚠ " + fileId + ".json読み込みエラー: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("✅ 性格データ読み込み完了: " + personalities.size() + "種類");
    }

    /**
     * IDで性格を取得
     */
    public Personality getPersonality(String id) {
        return personalities.get(id);
    }

    /**
     * すべての性格リストを取得
     */
    public List<Personality> getAllPersonalities() {
        return new ArrayList<>(personalityList);
    }

    /**
     * 性格の選択肢を表示用に取得
     */
    public Map<Integer, Personality> getPersonalityChoices() {
        Map<Integer, Personality> choices = new HashMap<>();
        for (int i = 0; i < personalityList.size(); i++) {
            choices.put(i + 1, personalityList.get(i));
        }
        return choices;
    }

    /**
     * 性格が存在するかチェック
     */
    public boolean hasPersonality(String id) {
        return personalities.containsKey(id);
    }

    /**
     * デフォルト性格を取得（活発）
     */
    public Personality getDefaultPersonality() {
        return personalities.getOrDefault("cheerful", personalityList.isEmpty() ? null : personalityList.get(0));
    }

    // ========== Static Access for SaveManager ==========

    /**
     * IDから性格を取得する（静的アクセス用）
     * SaveManagerから利用される
     */
    public static Personality getPersonalityById(String id) {
        // PersonalityManagerを一時的にインスタンス化して取得
        // コストはかかるが、頻繁に呼ばれるわけではないため許容
        try {
            return new PersonalityManager().getPersonality(id);
        } catch (Exception e) {
            e.printStackTrace();
            // 失敗時はIDだけのダミー
            Personality p = new Personality();
            p.setId(id);
            p.setName("Unknown");
            return p;
        }
    }
}