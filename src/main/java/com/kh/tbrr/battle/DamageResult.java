package com.kh.tbrr.battle;

/**
 * ダメージ処理の結果（実際に減ったHPと吸収されたSP）を保持するレコードクラス。
 */
public record DamageResult(int actualHpDamage, int actualSpDamage) {
}
