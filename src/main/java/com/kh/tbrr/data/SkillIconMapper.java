package com.kh.tbrr.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能名（日本語）とアイコン画像ファイル名（英語）のマッピングを管理するクラス
 * 
 * 技能はハードコードされた文字列として管理されているため、
 * 表示用のアイコン画像ファイル名へのマッピングが必要
 */
public class SkillIconMapper {

    private static final Map<String, String> SKILL_TO_ICON = new HashMap<>();

    static {
        // 基本技能（D&D準拠の6能力値）
        SKILL_TO_ICON.put("筋力", "strength.png");
        SKILL_TO_ICON.put("敏捷力", "dexterity.png");
        SKILL_TO_ICON.put("耐久力", "constitution.png");
        SKILL_TO_ICON.put("知力", "intelligence.png");
        SKILL_TO_ICON.put("判断力", "wisdom.png");
        SKILL_TO_ICON.put("魅力", "charisma.png");

        // その他の技能
        SKILL_TO_ICON.put("運動", "athletic.png");
        SKILL_TO_ICON.put("軽業", "acrobatics.png");
        SKILL_TO_ICON.put("隠密", "stealth.png");
        SKILL_TO_ICON.put("自然の知識", "nature.png");
        SKILL_TO_ICON.put("魔法の知識", "arcana.png");
        SKILL_TO_ICON.put("話術", "rhetoric.png");
        SKILL_TO_ICON.put("解錠術", "lockpicking.png");
        SKILL_TO_ICON.put("料理", "cooking.png");
        SKILL_TO_ICON.put("商才", "commerce.png");
        SKILL_TO_ICON.put("古代の知識", "history.png");
        SKILL_TO_ICON.put("薬識", "medicine.png");
        SKILL_TO_ICON.put("機巧", "mechanics.png");
    }

    /**
     * 技能名からアイコンファイル名を取得
     * 
     * @param skillName 技能名（日本語）
     * @return アイコンファイル名（例: "strength.png"）。見つからない場合はnull
     */
    public static String getIconFileName(String skillName) {
        return SKILL_TO_ICON.get(skillName);
    }

    /**
     * 指定された技能名がマッピングに存在するか確認
     * 
     * @param skillName 技能名
     * @return 存在する場合true
     */
    public static boolean hasMapping(String skillName) {
        return SKILL_TO_ICON.containsKey(skillName);
    }

    /**
     * すべてのマッピングを取得（読み取り専用）
     * 
     * @return 技能名とアイコンファイル名のマップ
     */
    public static Map<String, String> getAllMappings() {
        return new HashMap<>(SKILL_TO_ICON);
    }
}