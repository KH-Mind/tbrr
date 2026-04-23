package com.kh.tbrr.ui;

import com.kh.tbrr.data.ItemRegistry;
import com.kh.tbrr.data.models.Item;
import com.kh.tbrr.data.models.Player;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;

import java.util.List;

/**
 * ドロップ時専用の装備割り当てパネル。
 * 入手した新アイテムを「入手アイテム欄」に独立表示し、
 * 既存の予備スロットを一切変更せずに安全に装備セッティングができる。
 *
 * 通常の EquipmentPanel とは別クラスとして実装しているため、
 * 既存の装備管理UIへの影響はゼロ。
 */
public class DropEquipmentPanel extends VBox {

    private final Player player;
    private final Item pendingItem;          // 入手した（まだ配置先が決まっていない）アイテム
    private boolean pendingPlaced = false;   // pendingItem がどこかのスロットに配置されたか
    private Runnable onDecide;               // 「決定」ボタン押下時のコールバック（ラッチ解放用）

    // 選択状態管理
    private boolean pendingSelected = false; // 入手アイテム欄がクリックされた状態か
    private int selectedReserveIndex = -1;
    private String selectedEquippedSlot = null; // "MAIN", "ACC0", "ACC1", "ACC2"

    public DropEquipmentPanel(Player player, Item pendingItem, Runnable onDecide) {
        this.player = player;
        this.pendingItem = pendingItem;
        this.onDecide = onDecide;

        setPrefSize(450, 450);
        setMinSize(450, 450);
        setMaxSize(450, 450);
        setStyle("-fx-background-color: #2b2b2b; -fx-border-color: #cc8800; -fx-border-width: 2px;");
        setPadding(new Insets(8, 15, 8, 15));
        setSpacing(6);

        refreshUI();
    }

    /** pendingItem がどこかに配置済みか返す（JavaFXUI 側からの問い合わせ用） */
    public boolean isPendingPlaced() {
        return pendingPlaced;
    }

    // ============================================================
    // UI構築
    // ============================================================

    private void refreshUI() {
        getChildren().clear();

        // ---- 入手アイテム欄 ----
        if (!pendingPlaced) {
            Label newItemLabel = new Label("▼ 入手したアイテム (クリックして選択、配置先のスロットをクリックして装備)");
            newItemLabel.setStyle("-fx-text-fill: #ffcc44; -fx-font-size: 12px;");
            getChildren().add(newItemLabel);
            getChildren().add(createPendingItemButton());
        } else {
            Label placedLabel = new Label("✔ 配置完了！");
            placedLabel.setStyle("-fx-text-fill: #88ff88; -fx-font-size: 13px;");
            getChildren().add(placedLabel);
        }

        // ---- 区切り ----
        Region sep1 = makeSeparator();
        getChildren().add(sep1);

        // ---- 装備中エリア ----
        Label equippedLabel = new Label("▼ 装備中のアイテム");
        equippedLabel.setStyle("-fx-text-fill: #aaaaff; -fx-font-size: 13px;");
        getChildren().add(equippedLabel);

        getChildren().add(createEquippedButton("メイン武器", "MAIN", player.getEquippedMainWeapon()));

        List<String> accs = player.getEquippedAccessories();
        for (int i = 0; i < 3; i++) {
            String accId = (i < accs.size()) ? accs.get(i) : null;
            getChildren().add(createEquippedButton("アクセサリー " + (i + 1), "ACC" + i, accId));
        }

        // ---- 区切り ----
        Region sep2 = makeSeparator();
        getChildren().add(sep2);

        // ---- 予備スロットエリア（常に3枠固定表示・未開放は識別表示）----
        List<String> reserves = player.getReserveEquipments();
        // 常に3枠分描画（拡張時に新スロットが追加される場所を予約）
        for (int i = 0; i < 3; i++) {
            if (i < player.getMaxReserveSlots()) {
                // 有効なスロット
                String resId = (i < reserves.size()) ? reserves.get(i) : null;
                getChildren().add(createReserveButton(i, resId));
            } else {
                // 未開放スロット（表示のみ、クリック不可）
                getChildren().add(createLockedReserveButton(i));
            }
        }

        // ---- スペーサー ----
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        getChildren().add(bottomSpacer);

        // ---- 決定ボタン ----
        Button decideBtn = new Button("決定して閉じる");
        decideBtn.setPrefWidth(Double.MAX_VALUE);
        decideBtn.setStyle("-fx-background-color: #446644; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        decideBtn.setOnAction(e -> {
            if (onDecide != null) onDecide.run();
        });
        getChildren().add(decideBtn);
    }

    // ============================================================
    // ボタン生成
    // ============================================================

    private Button createPendingItemButton() {
        String itemName = pendingItem != null ? pendingItem.getName() : "（不明）";
        String category = pendingItem != null ? pendingItem.getEquipmentCategory() : "";
        String categoryStr = "WEAPON".equals(category) ? "武器" : "ACCESSORY".equals(category) ? "装飾品" : "";
        String label = "【" + categoryStr + "】 " + itemName;

        Button btn = new Button(label);
        btn.setPrefWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(javafx.scene.text.Font.font("Meiryo", 13));

        if (pendingSelected) {
            btn.setStyle("-fx-background-color: #cc8800; -fx-text-fill: white; -fx-border-color: #ffee88; -fx-border-width: 2px;");
        } else {
            btn.setStyle("-fx-background-color: #554400; -fx-text-fill: #ffee88; -fx-border-color: #aa7700; -fx-border-width: 1px;");
        }

        btn.setOnAction(e -> {
            pendingSelected = !pendingSelected;
            selectedReserveIndex = -1;
            selectedEquippedSlot = null;
            refreshUI();
        });
        return btn;
    }

    private Button createEquippedButton(String title, String slotType, String itemId) {
        String itemName = "（空き）";
        if (itemId != null) {
            Item item = ItemRegistry.getItemById(itemId);
            if (item != null) itemName = item.getName();
        }

        Button btn = new Button(title + ": " + itemName);
        btn.setPrefWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(javafx.scene.text.Font.font("Meiryo", 13));

        if (slotType.equals(selectedEquippedSlot)) {
            btn.setStyle("-fx-background-color: #aa9933; -fx-text-fill: white; -fx-border-color: #ffffaa; -fx-border-width: 2px;");
        } else {
            btn.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: white; -fx-border-color: #555555; -fx-border-width: 1px;");
        }

        btn.setOnAction(e -> handleEquippedSlotClick(slotType, itemId));
        return btn;
    }

    private Button createReserveButton(int index, String itemId) {
        String itemName = "（なし）";
        if (itemId != null) {
            Item item = ItemRegistry.getItemById(itemId);
            if (item != null) itemName = item.getName();
        }

        Button btn = new Button("予備枠 [" + (index + 1) + "]: " + itemName);
        btn.setPrefWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(javafx.scene.text.Font.font("Meiryo", 13));

        if (index == selectedReserveIndex) {
            btn.setStyle("-fx-background-color: #338833; -fx-text-fill: white; -fx-border-color: #aaffaa; -fx-border-width: 2px;");
        } else {
            btn.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: white; -fx-border-color: #555555; -fx-border-width: 1px;");
        }

        btn.setOnAction(e -> handleReserveSlotClick(index, itemId));
        return btn;
    }

    /** 未開放スロット（表示のみ、クリック不可） */
    private Button createLockedReserveButton(int index) {
        Button btn = new Button("予備枠 [" + (index + 1) + "]: （未開放）");
        btn.setPrefWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(javafx.scene.text.Font.font("Meiryo", 13));
        btn.setStyle("-fx-background-color: #252525; -fx-text-fill: #555555; -fx-border-color: #333333; -fx-border-width: 1px;");
        btn.setDisable(true);
        return btn;
    }

    private Region makeSeparator() {
        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setStyle("-fx-background-color: #555555;");
        VBox.setMargin(sep, new Insets(4, 0, 4, 0));
        return sep;
    }

    // ============================================================
    // クリックロジック
    // ============================================================

    private void handleEquippedSlotClick(String slotType, String itemId) {
        if (pendingSelected && !pendingPlaced) {
            // 入手アイテムをこの装備スロットに置く
            if (!isCompatible(pendingItem, slotType)) {
                // カテゴリ不一致（武器 → アクセサリー枠 など）は無視
                return;
            }
            // 現在の装備を予備スロットに空きがあれば逃がし、pendingItemを装備
            if (itemId != null) {
                // 既存装備を予備へ押し出す（空きチェック）
                if (player.getReserveEquipments().size() < player.getMaxReserveSlots()) {
                    player.getReserveEquipments().add(itemId);
                } else {
                    // 予備に空きなし → 既存装備は破棄（捨てる）
                }
            }
            setEquipSlot(slotType, pendingItem.getId());
            pendingPlaced = true;
            pendingSelected = false;
            selectedEquippedSlot = null;
            selectedReserveIndex = -1;
            refreshUI();
            return;
        }

        // 通常の装備スロット操作（既存装備同士の入れ替え）
        if (selectedReserveIndex != -1) {
            String reserveItemId = player.getReserveEquipments().get(selectedReserveIndex);
            Item resItem = ItemRegistry.getItemById(reserveItemId);
            if (resItem == null) return;
            if (!isCompatible(resItem, slotType)) return;

            player.getReserveEquipments().remove(selectedReserveIndex);
            if (itemId != null) {
                player.getReserveEquipments().add(selectedReserveIndex, itemId);
            }
            setEquipSlot(slotType, reserveItemId);
            selectedReserveIndex = -1;
            selectedEquippedSlot = null;
            refreshUI();
        } else {
            if (itemId != null) {
                if (player.getReserveEquipments().size() < player.getMaxReserveSlots()) {
                    player.getReserveEquipments().add(itemId);
                    setEquipSlot(slotType, null);
                } else {
                    selectedEquippedSlot = selectedEquippedSlot != null && selectedEquippedSlot.equals(slotType)
                            ? null : slotType;
                }
                refreshUI();
            } else if (selectedEquippedSlot != null) {
                String equippedId = getEquipSlot(selectedEquippedSlot);
                setEquipSlot(selectedEquippedSlot, null);
                setEquipSlot(slotType, equippedId);
                selectedEquippedSlot = null;
                refreshUI();
            }
        }
    }

    private void handleReserveSlotClick(int index, String itemId) {
        if (pendingSelected && !pendingPlaced) {
            // 入手アイテムを予備スロットへ
            if (itemId != null) {
                // 既存の予備アイテムと入れ替え（既存は破棄）
                player.getReserveEquipments().set(index, pendingItem.getId());
            } else {
                // 空き予備スロットに入れる
                if (index < player.getReserveEquipments().size()) {
                    player.getReserveEquipments().set(index, pendingItem.getId());
                } else {
                    player.getReserveEquipments().add(pendingItem.getId());
                }
            }
            pendingPlaced = true;
            pendingSelected = false;
            selectedReserveIndex = -1;
            selectedEquippedSlot = null;
            refreshUI();
            return;
        }

        // 通常の予備スロット操作（既存装備同士の入れ替え）
        if (selectedEquippedSlot != null) {
            String equippedItemId = getEquipSlot(selectedEquippedSlot);
            if (itemId != null) {
                Item resItem = ItemRegistry.getItemById(itemId);
                if (resItem != null && !isCompatible(resItem, selectedEquippedSlot)) return;
                player.getReserveEquipments().set(index, equippedItemId);
                setEquipSlot(selectedEquippedSlot, itemId);
            } else {
                if (index < player.getReserveEquipments().size()) {
                    player.getReserveEquipments().set(index, equippedItemId);
                } else {
                    player.getReserveEquipments().add(equippedItemId);
                }
                setEquipSlot(selectedEquippedSlot, null);
            }
            selectedEquippedSlot = null;
            selectedReserveIndex = -1;
            refreshUI();
        } else {
            if (itemId != null) {
                if (selectedReserveIndex == index) {
                    selectedReserveIndex = -1;
                } else {
                    selectedReserveIndex = index;
                }
                refreshUI();
            }
        }
    }

    // ============================================================
    // ヘルパーメソッド
    // ============================================================

    /** スロットタイプとアイテムカテゴリが一致するか確認 */
    private boolean isCompatible(Item item, String slotType) {
        if (item == null) return false;
        String cat = item.getEquipmentCategory();
        if ("MAIN".equals(slotType)) return "WEAPON".equalsIgnoreCase(cat);
        if (slotType != null && slotType.startsWith("ACC")) return "ACCESSORY".equalsIgnoreCase(cat);
        return false;
    }

    private String getEquipSlot(String slotType) {
        if ("MAIN".equals(slotType)) return player.getEquippedMainWeapon();
        if (slotType != null && slotType.startsWith("ACC")) {
            int idx = Integer.parseInt(slotType.replace("ACC", ""));
            List<String> accs = player.getEquippedAccessories();
            if (idx < accs.size()) return accs.get(idx);
        }
        return null;
    }

    private void setEquipSlot(String slotType, String itemId) {
        if ("MAIN".equals(slotType)) {
            player.setEquippedMainWeapon(itemId);
        } else if (slotType != null && slotType.startsWith("ACC")) {
            int idx = Integer.parseInt(slotType.replace("ACC", ""));
            List<String> accs = player.getEquippedAccessories();
            while (accs.size() <= idx) accs.add(null);
            accs.set(idx, itemId);
            accs.removeIf(val -> val == null);
        }
    }
}
