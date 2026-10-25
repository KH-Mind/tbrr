package com.kh.tbrr.data.models;

/**
 * 戦闘状態（CombatCondition）の付与設定。
 * アビリティ・武器・スタンスなど、付与元を問わず共通で使用する。
 */
public class ConditionApplication {
    private String conditionId;
    private int chance; // %
    private int duration;
    private int intensity; // 強度（dotのダメージ量、hindranceの確率%など。0の場合はJSON定義側のデフォルト値を使用）
    /** trueの場合、SP防壁（spBlockableな状態異常をSPが防ぐ仕様）を貫通して付与する。省略時はfalse。 */
    private boolean ignoreSpBlock;

    /** Gson（リフレクション生成）用のデフォルトコンストラクタ。 */
    public ConditionApplication() {
    }

    public String getConditionId() { return conditionId; }
    public int getChance() { return chance; }
    public int getDuration() { return duration; }
    public int getIntensity() { return intensity; }
    public boolean isIgnoreSpBlock() { return ignoreSpBlock; }
}
