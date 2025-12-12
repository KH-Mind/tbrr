package com.kh.tbrr.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能名と戦闘ステータス値のマッピングを管理するクラス
 * 
 * 各技能から自動的に算出され、キャラクターは以下のステータスを持つ:
 * - might (強靭): 身体の強さ、フィジカル（STR、CONS主体）
 * - insight (聡明): 賢さ、頭脳（大体INTとWIS）
 * - finesse (機敏): 器用さ、身のこなし（DEX主体）
 * - presence (風格): 人を動かす力、カリスマ（WISとCHA）
 * - sensuality (官能性): 隠しステータス、そういうゲームにするなら使う
 */
public class SkillStatsMapper {

    /**
     * 戦闘ステータスを保持するレコード
     */
    public record CombatStats(int might, int insight, int finesse, int presence, int sensuality) {
        public static CombatStats of(int might, int insight, int finesse, int presence) {
            return new CombatStats(might, insight, finesse, presence, 0);
        }

        public static CombatStats of(int might, int insight, int finesse, int presence, int sensuality) {
            return new CombatStats(might, insight, finesse, presence, sensuality);
        }
    }

    private static final Map<String, CombatStats> SKILL_STATS = new HashMap<>();

    static {
        // 基本能力値（6種） - 各合計8点
        // might, insight, finesse, presence, sensuality
        SKILL_STATS.put("筋力", CombatStats.of(6, 0, 1, 1, 0));
        SKILL_STATS.put("敏捷力", CombatStats.of(1, 1, 6, 0, 0));
        SKILL_STATS.put("耐久力", CombatStats.of(6, 0, 0, 2, 0));
        SKILL_STATS.put("知力", CombatStats.of(0, 8, 0, 0, 0));
        SKILL_STATS.put("判断力", CombatStats.of(0, 4, 0, 4, 0));
        SKILL_STATS.put("魅力", CombatStats.of(0, 0, 0, 8, 0));

        // その他の技能 - 各合計6点
        // might, insight, finesse, presence, sensuality
        SKILL_STATS.put("運動", CombatStats.of(4, 0, 2, 0, 0));
        SKILL_STATS.put("軽業", CombatStats.of(2, 0, 4, 0, 0));
        SKILL_STATS.put("隠密", CombatStats.of(0, 2, 4, 0, 0));
        SKILL_STATS.put("自然の知識", CombatStats.of(1, 3, 1, 1, 0));
        SKILL_STATS.put("魔法の知識", CombatStats.of(0, 4, 1, 1, 0));
        SKILL_STATS.put("古代の知識", CombatStats.of(0, 4, 0, 2, 0));
        SKILL_STATS.put("話術", CombatStats.of(0, 2, 0, 4, 0));
        SKILL_STATS.put("解錠術", CombatStats.of(0, 2, 4, 0, 0));
        SKILL_STATS.put("料理", CombatStats.of(2, 2, 2, 0, 0));
        SKILL_STATS.put("経世", CombatStats.of(0, 2, 0, 4, 0));
        SKILL_STATS.put("薬識", CombatStats.of(0, 6, 0, 0, 0));
        SKILL_STATS.put("機巧", CombatStats.of(0, 3, 3, 0, 0));
    }

    /**
     * 技能名から戦闘ステータスを取得
     * 
     * @param skillName 技能名（日本語）
     * @return CombatStats。見つからない場合はすべて0のステータスを返す
     */
    public static CombatStats getStats(String skillName) {
        return SKILL_STATS.getOrDefault(skillName, CombatStats.of(0, 0, 0, 0));
    }

    /**
     * 指定された技能名がステータスを持っているか確認
     * 
     * @param skillName 技能名
     * @return ステータスが存在する場合true
     */
    public static boolean hasStats(String skillName) {
        return SKILL_STATS.containsKey(skillName);
    }

    /**
     * すべてのステータスを取得（読み取り専用）
     * 
     * @return 技能名とステータスのマップ
     */
    public static Map<String, CombatStats> getAllStats() {
        return new HashMap<>(SKILL_STATS);
    }

    /**
     * ステータス名の日本語表示名を取得
     */
    public static String getStatDisplayName(String statKey) {
        return switch (statKey) {
            case "might" -> "強靭";
            case "insight" -> "聡明";
            case "finesse" -> "機敏";
            case "presence" -> "風格";
            case "sensuality" -> "官能性";
            default -> statKey;
        };
    }
}
