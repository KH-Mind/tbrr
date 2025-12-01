package com.kh.tbrr.data.models;

import java.util.List;

public class GameMap {
	private String id;
	private String name;
	private String description;
	private List<String> tags;
	private List<String> eventPool;
	private String backgroundImage; // 後方互換性のため残す
	private List<String> backgroundImages; // 複数の背景画像（ランダム選択用）
	private String subImage; // サブウィンドウ用画像
	private String entryEventId; // マップ入場時のイベントID（オプション）

	public GameMap() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public List<String> getEventPool() {
		return eventPool;
	}

	public void setEventPool(List<String> eventPool) {
		this.eventPool = eventPool;
	}

	public String getBackgroundImage() {
		return backgroundImage;
	}

	public void setBackgroundImage(String backgroundImage) {
		this.backgroundImage = backgroundImage;
	}

	public List<String> getBackgroundImages() {
		return backgroundImages;
	}

	public void setBackgroundImages(List<String> backgroundImages) {
		this.backgroundImages = backgroundImages;
	}

	public String getSubImage() {
		return subImage;
	}

	public void setSubImage(String subImage) {
		this.subImage = subImage;
	}

	public String getEntryEventId() {
	    return entryEventId;
	}
	
	public boolean hasTag(String tag) {

		return tags != null && tags.contains(tag);
	}

	@Override
	public String toString() {
		return name + " (" + id + ")";
	}
}
