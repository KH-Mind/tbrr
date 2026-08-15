package com.kh.tbrr.ui;

import com.kh.tbrr.data.ItemRegistry;
import com.kh.tbrr.data.StatusEffectRegistry;
import com.kh.tbrr.data.TraitRegistry;
import com.kh.tbrr.data.models.Item;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.data.models.TraitData;
import com.kh.tbrr.data.SkillStatsMapper.CombatStats;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * キャラクターステータスを表示するカスタムダイアログ。
 * Alert の代わりに Stage ベースで実装し、ゲームスレッドをブロックしない。
 * 特徴（Trait）はキャラクター固有分と装備付与分を別枠で表示する。
 */
public class StatusScreen extends Stage {

    // ─── スタイル定数 ───────────────────────────────────────
    private static final String BG_COLOR            = "#2b2b2b"; // メインテキストウィンドウ・重要ログと同色
    private static final String SECTION_TITLE_COLOR = "#c9a84c"; // ゴールド
    private static final String TEXT_COLOR          = "#dddddd";
    private static final String DESC_COLOR          = "#aaaaaa";
    private static final String EQUIP_TRAIT_COLOR   = "#7ec8e3"; // 水色（装備由来）
    private static final String VALUE_COLOR         = "#ffffff";
    private static final String TRAIT_BLOCK_COLOR   = "#3a3a3a"; // 装備ボタン・スロットと同色

    public StatusScreen(Player player) {
        setTitle("キャラクターシート");
        setResizable(true);
        setWidth(1024);
        setHeight(600);

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: " + BG_COLOR + ";");

        // ─── ヘッダー（閉じるボタンのみ） ───
        content.getChildren().add(makeHeader());

        // ─── 基本情報 ───
        content.getChildren().add(makeSectionTitle("基本情報"));
        content.getChildren().add(makeBasicInfo(player));
        content.getChildren().add(new Separator());

        // ─── ステータス ───
        content.getChildren().add(makeSectionTitle("ステータス"));
        content.getChildren().add(makeStatsBlock(player));
        content.getChildren().add(new Separator());

        // ─── 技能 ───
        content.getChildren().add(makeSectionTitle("技能"));
        content.getChildren().add(makeSkillsBlock(player));
        content.getChildren().add(new Separator());

        // ─── 所持品 ───
        content.getChildren().add(makeSectionTitle("所持品"));
        content.getChildren().add(makeInventoryBlock(player));
        content.getChildren().add(new Separator());

        // ─── 装備品 ───
        content.getChildren().add(makeSectionTitle("装備品"));
        content.getChildren().add(makeEquipmentBlock(player));
        content.getChildren().add(new Separator());

        // ─── 特徴（キャラクター固有） ───
        content.getChildren().add(makeSectionTitle("特徴（キャラクター固有）"));
        content.getChildren().add(makeCharacterTraitsBlock(player));

        // ─── 特徴（装備付与） ───
        VBox equipTraitsBlock = makeEquipTraitsBlock(player);
        if (equipTraitsBlock != null) {
            content.getChildren().add(new Separator());
            content.getChildren().add(makeSectionTitle("特徴（装備付与）"));
            content.getChildren().add(equipTraitsBlock);
        }

        // ─── 状態異常 ───
        VBox statusEffectsBlock = makeStatusEffectsBlock(player);
        if (statusEffectsBlock != null) {
            content.getChildren().add(new Separator());
            content.getChildren().add(makeSectionTitle("状態異常"));
            content.getChildren().add(statusEffectsBlock);
        }

        // ─── スクロールペイン ───
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
            "-fx-background-color: " + BG_COLOR + "; " +
            "-fx-background: " + BG_COLOR + ";"
        );

        Scene scene = new Scene(scrollPane, 1024, 600);
        scene.setFill(javafx.scene.paint.Color.web(BG_COLOR));
        setScene(scene);
    }

    // ─── ヘッダー（閉じるボタンのみ） ────────────────────────────
    private HBox makeHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_RIGHT);

        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("× 閉じる");
        closeBtn.setStyle(
            "-fx-background-color: #663333; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> close());

        header.getChildren().add(closeBtn);
        return header;
    }

    // ─── セクションタイトル ───────────────────────────────────
    private Label makeSectionTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("MS Gothic", FontWeight.BOLD, 15));
        label.setStyle("-fx-text-fill: " + SECTION_TITLE_COLOR + ";");
        return label;
    }

    // ─── 基本情報ブロック ─────────────────────────────────────
    private VBox makeBasicInfo(Player player) {
        VBox box = new VBox(4);

        String raceName = player.getRaceName() != null ? player.getRaceName() : "未設定";
        String raceType = player.getRaceType() != null ? player.getRaceType().getDisplayName() : "未設定";

        addInfoRow(box, "名前",           player.getName()           != null ? player.getName()           : "未設定");
        addInfoRow(box, "種族",           raceName + " (" + raceType + ")");
        addInfoRow(box, "性別",           player.getGender()         != null ? player.getGender().getDisplayName() : "未設定");
        addInfoRow(box, "性自認",         player.getGenderIdentity() != null ? player.getGenderIdentity() : "未設定");
        addInfoRow(box, "職業",           player.getJob()            != null ? player.getJob()            : "未設定");
        addInfoRow(box, "背景",           player.getBackground()     != null ? player.getBackground()     : "未設定");
        addInfoRow(box, "星座",           player.getConstellation()  != null ? player.getConstellation()  : "未設定");
        addInfoRow(box, "性格",           player.getPersonality()    != null ? player.getPersonality().getName() : "未設定");
        addInfoRow(box, "体型",           player.getBodyType()       != null ? player.getBodyType()       : "未設定");
        addInfoRow(box, "服装",           player.getClothing()       != null ? player.getClothing()       : "未設定");

        List<String> charmPoints = player.getCharmPoints();
        addInfoRow(box, "チャームポイント",
            (charmPoints == null || charmPoints.isEmpty()) ? "なし" : String.join(", ", charmPoints));

        return box;
    }

    // ─── ステータスブロック ───────────────────────────────────
    private VBox makeStatsBlock(Player player) {
        VBox box = new VBox(4);
        addInfoRow(box, "HP",   player.getHp()    + " / " + player.getEffectiveMaxHp());
        addInfoRow(box, "AP",   player.getAp()    + " / " + player.getEffectiveMaxAp());
        addInfoRow(box, "銀貨", player.getMoney() + " / " + player.getEffectiveMaxMoney());

        CombatStats cs = player.getCombatStats();
        addInfoRow(box, "強靭（Might）",   String.valueOf(cs.might()));
        addInfoRow(box, "機敏（Finesse）", String.valueOf(cs.finesse()));
        addInfoRow(box, "聡明（Insight）", String.valueOf(cs.insight()));
        addInfoRow(box, "風格（Presence）",String.valueOf(cs.presence()));
        addInfoRow(box, "グレード",       player.getGrade() + " / 20");
        return box;
    }

    // ─── 技能ブロック ─────────────────────────────────────────
    private VBox makeSkillsBlock(Player player) {
        VBox box = new VBox(4);
        List<String> skills = player.getEffectiveSkills();
        Label label = new Label(skills.isEmpty() ? "なし" : String.join(", ", skills));
        label.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-family: 'MS Gothic'; -fx-font-size: 14;");
        label.setWrapText(true);
        box.getChildren().add(label);
        return box;
    }

    // ─── 特徴（キャラクター固有）ブロック ────────────────────────
    private VBox makeCharacterTraitsBlock(Player player) {
        VBox box = new VBox(8);

        // baseTraits + inheritedTraits + traits を収集
        // getEffectiveTraits() の再帰展開（特徴→特徴付与）は内部的なメカニクスなので表示しない
        List<String> traitIds = new ArrayList<>();
        if (player.getBaseTraits()      != null) traitIds.addAll(player.getBaseTraits());
        if (player.getInheritedTraits() != null) traitIds.addAll(player.getInheritedTraits());
        if (player.getTraits()          != null) traitIds.addAll(player.getTraits());

        if (traitIds.isEmpty()) {
            box.getChildren().add(makeSimpleLabel("なし"));
            return box;
        }

        for (String traitId : traitIds) {
            TraitData td = TraitRegistry.getTraitById(traitId);
            box.getChildren().add(makeTraitBlock(
                td != null ? td.getName()        : traitId,
                td != null ? td.getDescription() : null,
                EQUIP_TRAIT_COLOR  // 装備付与特徴と同じ水色
            ));
        }
        return box;
    }

    // ─── 特徴（装備付与）ブロック ─────────────────────────────
    /**
     * 装備中のアイテムが持つ grantedTraits を表示する。
     * 何も付与していない（または装備していない）場合は null を返す。
     */
    private VBox makeEquipTraitsBlock(Player player) {
        VBox outerBox = new VBox(10);
        boolean hasAny = false;

        // メイン武器
        String weaponId = player.getEquippedMainWeapon();
        if (weaponId != null) {
            Item weapon = ItemRegistry.getItemById(weaponId);
            if (weapon != null && weapon.getGrantedTraits() != null && !weapon.getGrantedTraits().isEmpty()) {
                hasAny = true;
                String weaponName = ItemRegistry.getNameById(weaponId);
                outerBox.getChildren().add(makeEquipSubHeader(
                    (weaponName != null ? weaponName : weaponId) + " より"
                ));
                for (String traitId : weapon.getGrantedTraits()) {
                    TraitData td = TraitRegistry.getTraitById(traitId);
                    outerBox.getChildren().add(makeTraitBlock(
                        td != null ? td.getName()        : traitId,
                        td != null ? td.getDescription() : null,
                        EQUIP_TRAIT_COLOR
                    ));
                }
            }
        }

        // アクセサリ
        List<String> accessories = player.getEquippedAccessories();
        if (accessories != null) {
            for (String accId : accessories) {
                Item acc = ItemRegistry.getItemById(accId);
                if (acc != null && acc.getGrantedTraits() != null && !acc.getGrantedTraits().isEmpty()) {
                    hasAny = true;
                    String accName = ItemRegistry.getNameById(accId);
                    outerBox.getChildren().add(makeEquipSubHeader(
                        (accName != null ? accName : accId) + " より"
                    ));
                    for (String traitId : acc.getGrantedTraits()) {
                        TraitData td = TraitRegistry.getTraitById(traitId);
                        outerBox.getChildren().add(makeTraitBlock(
                            td != null ? td.getName()        : traitId,
                            td != null ? td.getDescription() : null,
                            EQUIP_TRAIT_COLOR
                        ));
                    }
                }
            }
        }

        return hasAny ? outerBox : null;
    }

    // ─── 所持品ブロック ───────────────────────────────────────
    private VBox makeInventoryBlock(Player player) {
        VBox box = new VBox(4);
        List<String> inventory = player.getInventory();
        if (inventory == null || inventory.isEmpty()) {
            box.getChildren().add(makeSimpleLabel("なし"));
            return box;
        }
        List<String> names = new ArrayList<>();
        for (String itemId : inventory) {
            String name = ItemRegistry.getNameById(itemId);
            names.add(name != null ? name : itemId);
        }
        Label label = new Label(String.join(", ", names));
        label.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-family: 'MS Gothic'; -fx-font-size: 14;");
        label.setWrapText(true);
        box.getChildren().add(label);
        return box;
    }

    // ─── 装備品ブロック ────────────────────────────────────────────
    /**
     * 装備中のアイテム（メイン武器・アクセサリ・予備スロット）を表示する。
     */
    private VBox makeEquipmentBlock(Player player) {
        VBox box = new VBox(4);

        // メイン武器
        String weaponId = player.getEquippedMainWeapon();
        String weaponName = (weaponId != null) ? ItemRegistry.getNameById(weaponId) : null;
        addInfoRow(box, "メイン武器",
            (weaponName != null) ? weaponName : "（なし）");

        // 装備中アクセサリ
        List<String> accs = player.getEquippedAccessories();
        if (accs != null && !accs.isEmpty()) {
            List<String> accNames = new ArrayList<>();
            for (String accId : accs) {
                String n = ItemRegistry.getNameById(accId);
                accNames.add(n != null ? n : accId);
            }
            addInfoRow(box, "アクセサリ", String.join("、", accNames));
        } else {
            addInfoRow(box, "アクセサリ", "（なし）");
        }

        // 予備スロット
        List<String> reserves = player.getReserveEquipments();
        if (reserves != null && !reserves.isEmpty()) {
            List<String> resNames = new ArrayList<>();
            for (String resId : reserves) {
                String n = ItemRegistry.getNameById(resId);
                resNames.add(n != null ? n : resId);
            }
            addInfoRow(box, "予備スロット", String.join("、", resNames));
        } else {
            addInfoRow(box, "予備スロット", "（なし）");
        }

        return box;
    }

    // ─── 状態異常ブロック ──────────────────────────────────────
    /**
     * 状態異常がある場合のみ VBox を返す。なければ null。
     */
    private VBox makeStatusEffectsBlock(Player player) {
        java.util.Map<String, Integer> effects = player.getStatusEffects();
        if (effects == null || effects.isEmpty()) return null;

        VBox box = new VBox(4);
        for (java.util.Map.Entry<String, Integer> entry : effects.entrySet()) {
            String name = StatusEffectRegistry.getNameById(entry.getKey());
            addInfoRow(box, name != null ? name : entry.getKey(), String.valueOf(entry.getValue()));
        }
        return box;
    }

    // ─── 共通ユーティリティ ────────────────────────────────────

    /** 「ラベル: 値」の1行を作る */
    private void addInfoRow(VBox parent, String key, String value) {
        HBox row = new HBox(8);
        Label keyLabel = new Label(key + ":");
        keyLabel.setStyle(
            "-fx-text-fill: " + DESC_COLOR + "; " +
            "-fx-font-family: 'MS Gothic'; " +
            "-fx-font-size: 14; " +
            "-fx-min-width: 150;"
        );
        Label valLabel = new Label(value);
        valLabel.setStyle("-fx-text-fill: " + VALUE_COLOR + "; -fx-font-family: 'MS Gothic'; -fx-font-size: 14;");
        valLabel.setWrapText(true);
        row.getChildren().addAll(keyLabel, valLabel);
        parent.getChildren().add(row);
    }

    /** 特徴1件分のブロック（名前 + 説明） */
    private VBox makeTraitBlock(String traitName, String description, String nameColor) {
        VBox block = new VBox(2);
        block.setPadding(new Insets(4, 8, 4, 8));
        block.setStyle(
            "-fx-background-color: " + TRAIT_BLOCK_COLOR + "; " +
            "-fx-border-color: #555555; " +
            "-fx-border-width: 0 0 0 3;"
        );

        Label nameLabel = new Label("◆ " + traitName);
        nameLabel.setFont(Font.font("MS Gothic", FontWeight.BOLD, 13));
        nameLabel.setStyle("-fx-text-fill: " + nameColor + ";");
        block.getChildren().add(nameLabel);

        if (description != null && !description.isEmpty()) {
            Label descLabel = new Label(description);
            descLabel.setStyle(
                "-fx-text-fill: " + DESC_COLOR + "; " +
                "-fx-font-family: 'MS Gothic'; " +
                "-fx-font-size: 12;"
            );
            descLabel.setWrapText(true);
            block.getChildren().add(descLabel);
        }
        return block;
    }

    /** 装備由来のサブヘッダー（「〇〇 より」） */
    private Label makeEquipSubHeader(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("MS Gothic", FontWeight.BOLD, 13));
        label.setStyle("-fx-text-fill: " + EQUIP_TRAIT_COLOR + ";");
        label.setPadding(new Insets(4, 0, 0, 0));
        return label;
    }

    /** シンプルな単行テキストラベル */
    private Label makeSimpleLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + DESC_COLOR + "; -fx-font-family: 'MS Gothic';");
        return label;
    }
}
