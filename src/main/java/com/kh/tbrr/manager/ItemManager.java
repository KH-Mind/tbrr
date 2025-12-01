package com.kh.tbrr.manager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kh.tbrr.data.models.Item;

/**
 * アイテム管理クラス
 */
public class ItemManager {
    private static ItemManager instance;
    private Map<String, Item> items;
    private Gson gson;
    private Random random;

    private ItemManager() {
        this.items = new HashMap<>();
        this.gson = new Gson();
        this.random = new Random();
        loadItems();
    }

    public static ItemManager getInstance() {
        if (instance == null) {
            instance = new ItemManager();
        }
        return instance;
    }

    /**
     * 全アイテムを読み込む
     */
    private void loadItems() {
        // コモンアイテム
        loadItemsFromFile("data/items/common_items.json");
        
        // 魔法アイテム
        loadItemsFromFile("data/items/magic_items.json");
        
        // 職業アイテム
        loadItemsFromFile("data/items/job_items.json");
        
        System.out.println("アイテムを読み込みました: " + items.size() + "個");
    }

    /**
     * JSONファイルからアイテムを読み込む
     */
    private void loadItemsFromFile(String path) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is == null) {
                System.err.println("ファイルが見つかりません: " + path);
                return;
            }

            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<Item>>(){}.getType();
            List<Item> itemList = gson.fromJson(reader, listType);

            for (Item item : itemList) {
                items.put(item.getId(), item);
            }

            reader.close();
        } catch (Exception e) {
            System.err.println("アイテム読み込みエラー (" + path + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * IDでアイテムを取得
     */
    public Item getItem(String id) {
        return items.get(id);
    }

    /**
     * ランダムなコモンアイテムを取得
     */
    public Item getRandomCommonItem() {
        List<Item> commonItems = new ArrayList<>();
        for (Item item : items.values()) {
            if ("common".equals(item.getRarity())) {
                commonItems.add(item);
            }
        }
        
        if (commonItems.isEmpty()) {
            return null;
        }
        
        return commonItems.get(random.nextInt(commonItems.size()));
    }

    /**
     * 全アイテムのリストを取得
     */
    public List<Item> getAllItems() {
        return new ArrayList<>(items.values());
    }

    /**
     * レアリティ別にアイテムを取得
     */
    public List<Item> getItemsByRarity(String rarity) {
        List<Item> result = new ArrayList<>();
        for (Item item : items.values()) {
            if (rarity.equals(item.getRarity())) {
                result.add(item);
            }
        }
        return result;
    }
}