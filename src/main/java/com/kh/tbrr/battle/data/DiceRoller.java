package com.kh.tbrr.battle.data;

import java.util.Random;

public class DiceRoller {
    private static final Random RANDOM = new Random();

    /**
     * "1d4", "2d6+3", "10" 等の文字列をパースして合計を返す
     */
    public static int roll(String diceStr) {
        if (diceStr == null || diceStr.isEmpty()) return 0;
        
        diceStr = diceStr.toLowerCase().replaceAll("\\s+", "");
        
        try {
            // "10" のような固定値かチェック
            if (!diceStr.contains("d")) {
                return Integer.parseInt(diceStr);
            }

            // "2d6+3" などプラス補正の分離
            int bonus = 0;
            if (diceStr.contains("+")) {
                String[] splitPlus = diceStr.split("\\+");
                diceStr = splitPlus[0];
                bonus = Integer.parseInt(splitPlus[1]);
            } else if (diceStr.contains("-")) {
                String[] splitMinus = diceStr.split("-");
                diceStr = splitMinus[0];
                bonus = -Integer.parseInt(splitMinus[1]);
            }

            // "2d6" 等のパース
            String[] parts = diceStr.split("d");
            int count = parts[0].isEmpty() ? 1 : Integer.parseInt(parts[0]);
            int sides = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            
            int sum = 0;
            for (int i = 0; i < count; i++) {
                sum += RANDOM.nextInt(sides) + 1;
            }
            return sum + bonus;
            
        } catch (Exception e) {
            System.err.println("Dice roll error: " + diceStr);
        }
        return 0;
    }
}
