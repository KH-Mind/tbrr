package com.kh.tbrr.ui;

import java.util.List;

import com.kh.tbrr.data.models.GraveRecord;
import com.kh.tbrr.manager.GraveyardManager;

/**
 * 墓地などの補助メニューを表示するUIクラス
 * ※旧蘇生システムは削除済み。墓地閲覧はGraveyardManagerの静的メソッドで行う。
 * ※GameStateへの依存も削除済み（GraveyardManagerがstaticになったため不要）
 */
public class ExtraMenuUI {
    private ConsoleUI ui;

    public ExtraMenuUI(ConsoleUI ui) {
        this.ui = ui;
    }

    public void showGraveyardMenu() {
        List<GraveRecord> records = GraveyardManager.loadAllRecords();

        ui.print("━━━━━━━━━━━━━━━━━━━━━━━━");
        ui.print("       墓地");
        ui.print("━━━━━━━━━━━━━━━━━━━━━━━━");

        if (records.isEmpty()) {
            ui.print("まだ誰も倒れていない。");
        } else {
            for (GraveRecord record : records) {
                String line = "- " + record.getCharacterName()
                    + "（" + record.getCharacterJob() + "）"
                    + " / フロア：" + record.getFloor()
                    + " / 死因：" + record.getDeathEvent();
                ui.print(line);
            }
        }

        ui.print("━━━━━━━━━━━━━━━━━━━━━━━━");
        ui.waitForEnter();
    }
}
