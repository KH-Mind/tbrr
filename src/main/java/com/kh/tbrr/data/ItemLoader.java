package com.kh.tbrr.data;

import java.io.FileReader;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kh.tbrr.data.models.Item;

public class ItemLoader {
    public static List<Item> loadItems(String path) {
        try (FileReader reader = new FileReader(path)) {
            List<Item> items = new Gson().fromJson(reader, new TypeToken<List<Item>>(){}.getType());
            for (Item item : items) {
                ItemRegistry.register(item); // ← ここで名前を登録
            }
            return items;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
