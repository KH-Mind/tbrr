package com.kh.tbrr.ui;

import java.util.List;

import com.kh.tbrr.core.GameState;
import com.kh.tbrr.data.models.GraveRecord;
import com.kh.tbrr.manager.GraveyardManager;

/**
 * 墓地などの補助メニューを表示するUIクラス
 */
public class ExtraMenuUI {
    private GameState gameState;
    private ConsoleUI ui;

    public ExtraMenuUI(GameState gameState, ConsoleUI ui) {
        this.gameState = gameState;
        this.ui = ui;
    }

    public void showGraveyardMenu() {
        GraveyardManager manager = new GraveyardManager(gameState);
        List<GraveRecord> records = manager.getAllRecords();

        ui.print("━━━━━━━━━━━━━━━━━━━━━━━━");
        ui.print("       墓地");
        ui.print("━━━━━━━━━━━━━━━━━━━━━━━━");

        for (GraveRecord record : records) {
            String line = "- " + record.getName()
                + "（死因：" + record.getDeathCause()
                + " / フロア：" + record.getFloor() + "）";

            if (record.isRevived()) {
                line += " [蘇生済]";
            } else if (record.isFated()) {
                line += " ★蘇生可能";
            }

            ui.print(line);
        }

        ui.print("━━━━━━━━━━━━━━━━━━━━━━━━");
        ui.print("蘇生したいキャラIDを入力（キャンセルは空欄）:");
        String input = ui.getInput();

        if (!input.isEmpty()) {
            boolean success = manager.revive(input);
            ui.print(success ? "蘇生に成功しました！" : "蘇生できませんでした。");
        }

        ui.waitForEnter();
    }
}
