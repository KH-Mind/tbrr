package com.kh.tbrr.manager;

import java.io.InputStream;

import javafx.scene.image.Image;

/**
 * アイコン画像（32×32）の読み込みを管理するヘルパークラス
 * 
 * 技能、アイテム、状態異常のアイコン画像を統一的に扱う
 */
public class IconHelper {

    private static final String ICON_BASE_PATH = "/data/images/icons/";
    private static final int ICON_SIZE = 32;

    /**
     * アイコン画像を読み込む
     * 
     * @param fileName ファイル名（例: "strength.png", "torch.png"）
     * @return 読み込んだImage。失敗した場合はnull
     */
    public static Image loadIcon(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        try {
            String path = ICON_BASE_PATH + fileName;
            InputStream is = IconHelper.class.getResourceAsStream(path);

            if (is != null) {
                Image image = new Image(is, ICON_SIZE, ICON_SIZE, true, true);
                is.close();
                return image;
            } else {
                System.err.println("[IconHelper] アイコンが見つかりません: " + path);
                return null;
            }
        } catch (Exception e) {
            System.err.println("[IconHelper] アイコン読み込みエラー: " + fileName);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 技能名からアイコン画像を読み込む
     * 
     * @param skillName 技能名（日本語）
     * @return 読み込んだImage。マッピングまたは画像が見つからない場合はnull
     */
    public static Image loadSkillIcon(String skillName) {
        String fileName = com.kh.tbrr.data.SkillIconMapper.getIconFileName(skillName);
        if (fileName == null) {
            System.err.println("[IconHelper] 技能のマッピングが見つかりません: " + skillName);
            return null;
        }
        return loadIcon(fileName);
    }

    /**
     * 技能名から説明文を取得
     * 
     * @param skillName 技能名（日本語）
     * @return 説明文
     */
    public static String getSkillDescription(String skillName) {
        return com.kh.tbrr.data.SkillDescriptionMapper.getDescription(skillName);
    }

    /**
     * アイテムIDからアイコン画像を読み込む
     * 
     * @param itemId アイテムID（例: "torch", "rope"）
     * @return 読み込んだImage。画像が見つからない場合はnull
     */
    public static Image loadItemIcon(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        return loadIcon(itemId + ".png");
    }

    /**
     * アイテムIDから説明文を取得
     * 
     * @param itemId アイテムID
     * @return 説明文。見つからない場合はアイテム名を返す
     */
    public static String getItemDescription(String itemId) {
        com.kh.tbrr.data.models.Item item = com.kh.tbrr.data.ItemRegistry.getItemById(itemId);
        if (item != null) {
            // ツールチップに表示する情報：アイテム名と説明
            StringBuilder desc = new StringBuilder();
            desc.append(item.getName()); // 日本語名を表示
            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                desc.append("\n").append(item.getDescription());
            }
            return desc.toString();
        }
        // アイテムが見つからない場合はIDを返す
        return itemId;
    }

    /**
     * 状態異常IDからアイコン画像を読み込む
     * 
     * @param effectId 状態異常ID（例: "poison", "hunger"）
     * @return 読み込んだImage。画像が見つからない場合はnull
     */
    public static Image loadStatusEffectIcon(String effectId) {
        if (effectId == null || effectId.isEmpty()) {
            return null;
        }
        return loadIcon(effectId + ".png");
    }

    /**
     * 状態異常IDから説明文を取得
     * 
     * @param effectId 状態異常ID
     * @return 説明文。見つからない場合は状態異常名を返す
     */
    public static String getStatusEffectDescription(String effectId) {
        com.kh.tbrr.data.models.StatusEffect effect = com.kh.tbrr.data.StatusEffectRegistry
                .getStatusEffectById(effectId);
        if (effect != null && effect.getDescription() != null && !effect.getDescription().isEmpty()) {
            return effect.getDescription();
        }
        // descriptionがない場合は状態異常名を返す
        String effectName = com.kh.tbrr.data.StatusEffectRegistry.getNameById(effectId);
        return effectName != null ? effectName : effectId;
    }

    /**
     * プレースホルダー画像を生成（アイコンが見つからない場合の代替）
     * 
     * @return 空の32×32画像
     */
    public static Image createPlaceholder() {
        // 透明な32×32の画像を返す
        // 実際には何も表示されないが、レイアウトは保持される
        return new Image(IconHelper.class.getResourceAsStream("/data/images/icons/placeholder.png"),
                ICON_SIZE, ICON_SIZE, true, true);
    }
}