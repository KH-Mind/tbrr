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

public class EquipmentPanel extends VBox {
    private Player player;
    private Runnable onClose;
    private Runnable onEquipmentChanged;
    
    // 選択状態の管理2
    private int selectedReserveIndex = -1;
    private String selectedEquippedSlot = null; // "MAIN", "ACC0", "ACC1", "ACC2"

    public EquipmentPanel(Player player, Runnable onClose, Runnable onEquipmentChanged) {
        this.player = player;
        this.onClose = onClose;
        this.onEquipmentChanged = onEquipmentChanged;
        
        setPrefSize(450, 450);
        setMinSize(450, 450);
        setMaxSize(450, 450);
        setStyle("-fx-background-color: #2b2b2b; -fx-border-color: #666666; -fx-border-width: 2px;");
        setPadding(new Insets(10, 15, 10, 15));
        setSpacing(10);
        
        refreshUI();
    }

    private void refreshUI() {
        getChildren().clear();
        
        // ヘッダー部
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("=== 装備管理 ===");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button closeBtn = new Button("× 閉じる");
        closeBtn.setStyle("-fx-background-color: #aa3333; -fx-text-fill: white; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> {
            if (onClose != null) onClose.run();
        });
        
        header.getChildren().addAll(title, spacer, closeBtn);
        getChildren().add(header);
        
        // --- 装備中エリア ---
        Label equippedLabel = new Label("▼ 装備中のアイテム");
        equippedLabel.setStyle("-fx-text-fill: #aaaaff; -fx-font-size: 14px;");
        getChildren().add(equippedLabel);
        
        // メイン武器
        getChildren().add(createEquippedButton("メイン武器", "MAIN", player.getEquippedMainWeapon()));
        
        // アクセサリー 1〜3
        List<String> accs = player.getEquippedAccessories();
        for (int i = 0; i < 3; i++) {
            String accId = (i < accs.size()) ? accs.get(i) : null;
            getChildren().add(createEquippedButton("アクセサリー " + (i + 1), "ACC" + i, accId));
        }
        
        // 区切り線
        Region separator = new Region();
        separator.setMinHeight(2);
        separator.setStyle("-fx-background-color: #555555;");
        VBox.setMargin(separator, new Insets(5, 0, 5, 0));
        getChildren().add(separator);
        
        // --- 予備スロットエリア ---
        Label reserveLabel = new Label("▼ 予備スロット (上限: " + player.getMaxReserveSlots() + ")");
        reserveLabel.setStyle("-fx-text-fill: #aaffaa; -fx-font-size: 14px;");
        getChildren().add(reserveLabel);
        
        List<String> reserves = player.getReserveEquipments();
        for (int i = 0; i < player.getMaxReserveSlots(); i++) {
            String resId = (i < reserves.size()) ? reserves.get(i) : null;
            getChildren().add(createReserveButton(i, resId));
        }
        
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        getChildren().add(bottomSpacer);
        
        // ステータスメッセージ
        Label hint = new Label("※アイテムをクリックして着脱・入れ替え");
        hint.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");
        getChildren().add(hint);
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
        
        // 選択状態のハイライト
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

    // --- ロジックコア ---

    private void handleEquippedSlotClick(String slotType, String itemId) {
        // 空枠をクリックして何も選択されていない場合は無視
        if (itemId == null && selectedReserveIndex == -1 && selectedEquippedSlot == null) {
            return;
        }

        if (selectedReserveIndex != -1) {
            // [予備枠] が選択されている状態 -> スワップ実行！
            String reserveItemId = player.getReserveEquipments().get(selectedReserveIndex);
            Item resItem = ItemRegistry.getItemById(reserveItemId);
            if (resItem == null) return;
            
            // 装備可能なタイプかチェック
            if (slotType.equals("MAIN") && !"WEAPON".equals(resItem.getEquipmentCategory())) {
                showLog("武器以外はメイン枠に直接装備できません。");
                return;
            }
            if (slotType.startsWith("ACC") && !"ACCESSORY".equals(resItem.getEquipmentCategory())) {
                showLog("種類が違うアイテムは交換できません。");
                return;
            }
            
            // スワップ実行
            player.getReserveEquipments().remove(selectedReserveIndex);
            if (itemId != null) {
                // 元々装備していたものを予備へ送る
                player.getReserveEquipments().add(selectedReserveIndex, itemId);
            }
            
            setEquipSlot(slotType, reserveItemId);
            
            selectedReserveIndex = -1;
            selectedEquippedSlot = null;
            if (onEquipmentChanged != null) onEquipmentChanged.run();
            refreshUI();
            
        } else {
            // 何も選択されていない状態で装備中のアイテム(空でない)をクリック
            // -> 予備枠に空きがあれば外して移動する。空きがなければこれを選択状態にする
            if (itemId != null) {
                if (player.getReserveEquipments().size() < player.getMaxReserveSlots()) {
                    // 外して予備へ
                    player.getReserveEquipments().add(itemId);
                    setEquipSlot(slotType, null);
                    if (onEquipmentChanged != null) onEquipmentChanged.run();
                } else {
                    // 予備がパンパンなので「選択状態」にする
                    if (selectedEquippedSlot != null && selectedEquippedSlot.equals(slotType)) {
                        selectedEquippedSlot = null; // 選択解除
                    } else {
                        selectedEquippedSlot = slotType;
                    }
                }
                refreshUI();
            } else if (selectedEquippedSlot != null) {
                // すでに同エリアのアクティブがある場合はスワップ（アクセの移動など）
                String equippedId = getEquipSlot(selectedEquippedSlot);
                setEquipSlot(selectedEquippedSlot, null);
                setEquipSlot(slotType, equippedId);
                selectedEquippedSlot = null;
                if (onEquipmentChanged != null) onEquipmentChanged.run();
                refreshUI();
            }
        }
    }

    private void handleReserveSlotClick(int index, String itemId) {
        // 空枠をクリックして何も選択されていない場合は無視
        if (itemId == null && selectedEquippedSlot == null && selectedReserveIndex == -1) {
            return;
        }
        
        if (selectedEquippedSlot != null) {
            // [装備枠] が選択されている状態 -> スワップ実行
            String equippedItemId = getEquipSlot(selectedEquippedSlot);
            
            if (itemId != null) {
                Item resItem = ItemRegistry.getItemById(itemId);
                if (selectedEquippedSlot.equals("MAIN") && !"WEAPON".equals(resItem.getEquipmentCategory())) {
                    showLog("種類が合わないため交換できません。");
                    return;
                }
                if (selectedEquippedSlot.startsWith("ACC") && !"ACCESSORY".equals(resItem.getEquipmentCategory())) {
                    showLog("種類が合わないため交換できません。");
                    return;
                }
            }
            
            // スワップ処理
            if (itemId != null) {
                player.getReserveEquipments().set(index, equippedItemId);
                setEquipSlot(selectedEquippedSlot, itemId);
            } else {
                // 予備枠が空ならそこへ送る
                if (index < player.getReserveEquipments().size()) {
                    player.getReserveEquipments().set(index, equippedItemId);
                } else {
                    player.getReserveEquipments().add(equippedItemId);
                }
                setEquipSlot(selectedEquippedSlot, null);
            }
            
            selectedEquippedSlot = null;
            selectedReserveIndex = -1;
            if (onEquipmentChanged != null) onEquipmentChanged.run();
            refreshUI();
            
        } else {
            // 予備アイテムをクリック
            if (itemId != null) {
                Item resItem = ItemRegistry.getItemById(itemId);
                if (resItem == null) return;
                
                boolean equipped = false;
                if ("WEAPON".equals(resItem.getEquipmentCategory())) {
                    if (player.getEquippedMainWeapon() == null) {
                        player.setEquippedMainWeapon(itemId);
                        player.getReserveEquipments().remove(index);
                        equipped = true;
                    }
                } else if ("ACCESSORY".equals(resItem.getEquipmentCategory())) {
                    List<String> accs = player.getEquippedAccessories();
                    if (accs.size() < 3) {
                        accs.add(itemId);
                        player.getReserveEquipments().remove(index);
                        equipped = true;
                    }
                }
                
                if (equipped) {
                    selectedReserveIndex = -1;
                    if (onEquipmentChanged != null) onEquipmentChanged.run();
                } else {
                    // 空き枠がないのでこれを選択状態にする
                    if (selectedReserveIndex == index) {
                        selectedReserveIndex = -1; // 選択解除
                    } else {
                        selectedReserveIndex = index;
                    }
                }
                refreshUI();
            } else if (selectedReserveIndex != -1) {
                // 予備リスト内の順序変更
                String movingItem = player.getReserveEquipments().get(selectedReserveIndex);
                player.getReserveEquipments().remove(selectedReserveIndex);
                player.getReserveEquipments().add(movingItem); 
                selectedReserveIndex = -1;
                refreshUI();
            }
        }
    }
    
    // ヘルパーメソッド群
    private String getEquipSlot(String slotType) {
        if (slotType.equals("MAIN")) return player.getEquippedMainWeapon();
        if (slotType.startsWith("ACC")) {
            int idx = Integer.parseInt(slotType.replace("ACC", ""));
            List<String> accs = player.getEquippedAccessories();
            if (idx < accs.size()) return accs.get(idx);
        }
        return null;
    }
    
    private void setEquipSlot(String slotType, String itemId) {
        if (slotType.equals("MAIN")) {
            player.setEquippedMainWeapon(itemId);
        } else if (slotType.startsWith("ACC")) {
            int idx = Integer.parseInt(slotType.replace("ACC", ""));
            List<String> accs = player.getEquippedAccessories();
            while (accs.size() <= idx) {
                accs.add(null);
            }
            accs.set(idx, itemId);
            // null除去してリストを詰める
            accs.removeIf(val -> val == null);
        }
    }

    private void showLog(String text) {
        System.out.println(text);
        // Alertなどで出すことも可能
    }
}
