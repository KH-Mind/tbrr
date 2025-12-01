package com.kh.tbrr.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kh.tbrr.data.models.Item;

public class ItemRegistry {
	private static final Map<String, String> itemNameMap = new HashMap<>();
	private static final Map<String, Item> itemMap = new HashMap<>();

	public static void register(Item item) {
		itemNameMap.put(item.getId(), item.getName());
		itemMap.put(item.getId(), item);
	}

	public static String getNameById(String id) {
		return itemNameMap.get(id);
	}

	public static Item getItemById(String id) {
		return itemMap.get(id);
	}

	public static void clear() {
		itemNameMap.clear();
		itemMap.clear();
	}

	/**
	 * レアリティ指定でランダム喪失可能なアイテムIDリストを取得
	 */
	public static List<String> getLosableItemIdsByRarity(String rarity) {
		List<String> result = new ArrayList<>();
		for (Map.Entry<String, Item> entry : itemMap.entrySet()) {
			Item item = entry.getValue();
			if (rarity.equals(item.getRarity()) && item.isLosableRandom()) {
				result.add(entry.getKey());
			}
		}
		return result;
	}

}
