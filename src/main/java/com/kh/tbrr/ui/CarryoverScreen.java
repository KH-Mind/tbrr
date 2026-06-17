package com.kh.tbrr.ui;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.kh.tbrr.battle.data.AbilityData;
import com.kh.tbrr.battle.data.CombatDataLoader;
import com.kh.tbrr.battle.data.TraitData;
import com.kh.tbrr.battle.data.TraitRegistry;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.manager.ImageManager;
import com.kh.tbrr.system.CharacterLoader;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * 引継ぎ選択画面
 * FatedOneキャラクターが死亡したとき、この周回で得たアビリティ・特徴の中から
 * 1つを選択して次周回に引き継ぐための画面。
 *
 * ゲームスレッドから requestCarryover() を呼び、CountDownLatch で
 * 選択完了まで待機する設計。
 */
public class CarryoverScreen {

    private Stage stage;
    private ImageManager imageManager;

    // 引継ぎ選択に関する状態
    private String selectedId = null; // 選択されたアビリティ/特徴のID
    private boolean isAbility = false; // true=アビリティ, false=特徴

    // 仮フレーバーメッセージ（後でロア担当が差し替え）
    private static final String FLAVOR_TEXT = "　足元に薄く水が張ったような、果てしなく続く真っ白な空間。\n\n" +
            "「おかえりなさい。……また、随分と傷ついてしまったのですね」\n\n" +
            "　声の主は、裸足で水面に立つ無垢な少女だった。\n" +
            "　その傍らには、彼女を守るように黒い大型の獣が鎮座し、男のような低い声で唸った。\n\n" +
            "『ちっ……また無様な死に方をして戻ってきたか。アンタも懲りないねぇ。\n" +
            "　まぁいい、嬢ちゃんの気まぐれだ。俺がアンタの魂を書き換えてやる』\n\n" +
            "　あなたはもう一度、歩き始める機会を与えられた。";

    private static final String CLOSING_TEXT = "「……それでもあなたは、またあの有限の世界へ戻るのですね」\n\n" +
            "　少女は少し寂しそうに微笑み、獣は鼻を鳴らした。\n\n" +
            "『行け。そして這いつくばってでも生きてみせろ。嬢ちゃんが退屈しない結末を期待してるぜ』\n\n" +
            "　光が静かに消え、あなたは再び歩き始めた。";

    public CarryoverScreen(Stage stage) {
        this.stage = stage;
        this.imageManager = new ImageManager();
    }

    /**
     * 引継ぎ選択をゲームスレッドから呼び出すメソッド。
     * JavaFXスレッドで画面を開き、プレイヤーが選択するまでブロックする。
     * 選択完了後にリセット処理とJSON上書き保存を行う。
     *
     * @param player     対象のPlayerデータ（更新される）
     * @param onComplete 完了後に呼ぶコールバック（ゲームスレッドに戻る）
     */
    public void requestCarryover(Player player, Runnable onComplete) {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> show(player, latch::countDown));

        try {
            latch.await(); // ゲームスレッドはここで待機（UIが選択を確定するまでブロック）
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 待機解除後: 選択結果をPlayerに反映
        try {
            applyCarryoverResult(player);
        } catch (Exception e) {
            System.err.println("[CarryoverScreen] applyCarryoverResult でエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            // エラーが発生してもゲームは続行できるよう、コールバックは必ず呼ぶ
        }

        // コールバックを呼ぶ
        if (onComplete != null) {
            onComplete.run();
        }
    }

    /**
     * 引継ぎ画面をJavaFXスレッドで表示する。
     *
     * @param player    対象プレイヤー
     * @param onConfirm 決定確定時のコールバック（ラッチ解放用）
     */
    private void show(Player player, Runnable onConfirm) {
        // 背景画像
        Image backgroundImage = imageManager.loadBackground("carryover_scene.png");
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setFitWidth(1600);
        backgroundView.setFitHeight(900);
        backgroundView.setPreserveRatio(false);

        // 半透明オーバーレイ
        javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle(1600, 900);
        overlay.setFill(javafx.scene.paint.Color.color(0, 0, 0, 0.75));

        // コンテンツ全体
        VBox contentBox = new VBox(24);
        contentBox.setPadding(new Insets(50, 100, 50, 100));
        contentBox.setAlignment(Pos.TOP_CENTER);

        // タイトル
        Label titleLabel = new Label("── 引継ぎ ──");
        titleLabel.setFont(Font.font("Arial", 30));
        titleLabel.setStyle("-fx-text-fill: #d4af37;");

        // フレーバーテキスト
        TextArea flavorArea = new TextArea(FLAVOR_TEXT);
        flavorArea.setEditable(false);
        flavorArea.setWrapText(true);
        flavorArea.setFocusTraversable(false);
        flavorArea.setPrefHeight(220);
        flavorArea.setMaxWidth(900);
        flavorArea.setStyle(
                "-fx-control-inner-background: #2b2b2b; " +
                        "-fx-background-color: #2b2b2b; " +
                        "-fx-text-fill: #dddddd; " +
                        "-fx-font-size: 15px; " +
                        "-fx-border-color: #555555; " +
                        "-fx-border-width: 1px; " +
                        "-fx-focus-color: transparent; " +
                        "-fx-faint-focus-color: transparent;");

        // 選択肢ラベル
        Label selectionLabel = new Label("以下から1つを選び、次の旅に持ち帰れ");
        selectionLabel.setFont(Font.font("Arial", 18));
        selectionLabel.setStyle("-fx-text-fill: #aaaaaa;");

        // 選択肢リストの構築
        List<CarryoverItem> candidates = buildCandidates(player);

        ToggleGroup toggleGroup = new ToggleGroup();
        VBox radioBox = new VBox(12);
        radioBox.setPadding(new Insets(10, 20, 10, 20));
        radioBox.setMaxWidth(860);
        radioBox.setStyle(
                "-fx-background-color: #2b2b2b; " +
                        "-fx-border-color: #555555; " +
                        "-fx-border-width: 1px;");

        for (CarryoverItem item : candidates) {
            RadioButton rb = new RadioButton();
            rb.setToggleGroup(toggleGroup);
            rb.setUserData(item);
            rb.setFont(Font.font("Arial", 16));
            rb.setStyle("-fx-text-fill: #dddddd;");

            // 表示テキスト: [種別] 名前 — 説明
            String typeLabel = item.isAbility ? "【アビリティ】" : "【特徴】";
            String display = typeLabel + " " + item.displayName;
            if (item.description != null && !item.description.isEmpty()) {
                display += "  ―  " + item.description;
            }
            rb.setText(display);

            rb.setOnAction(e -> {
                CarryoverItem sel = (CarryoverItem) rb.getUserData();
                selectedId = sel.id;
                isAbility = sel.isAbility;
            });

            radioBox.getChildren().add(rb);
        }

        ScrollPane scrollPane = new ScrollPane(radioBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(360);
        scrollPane.setMaxWidth(900);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // 決定ボタン
        Button confirmBtn = createButton("決定");
        confirmBtn.setOnAction(e -> {
            if (selectedId == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "引き継ぐものを選択してください。", ButtonType.OK);
                alert.setHeaderText(null);
                alert.showAndWait();
                return;
            }
            String itemName = getDisplayNameById(selectedId, isAbility);
            String confirmMsg = (isAbility ? "アビリティ「" : "特徴「") + itemName + "」を引き継ぎますか？";
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION, confirmMsg, ButtonType.YES, ButtonType.NO);
            confirmAlert.setHeaderText(null);
            confirmAlert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.YES) {
                    showClosingMessage(onConfirm);
                }
            });
        });

        contentBox.getChildren().addAll(titleLabel, flavorArea, selectionLabel, scrollPane, confirmBtn);

        StackPane root = new StackPane();
        root.getChildren().addAll(backgroundView, overlay, contentBox);

        Scene scene = new Scene(root, 1600, 900);
        stage.setScene(scene);
        stage.setTitle("T.B.R.R.");
        stage.show();
    }

    /**
     * 締めくくりメッセージを表示し、ボタン押下後にラッチを解放する。
     */
    private void showClosingMessage(Runnable onConfirm) {
        Image backgroundImage = imageManager.loadBackground("carryover_scene.png");
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setFitWidth(1600);
        backgroundView.setFitHeight(900);
        backgroundView.setPreserveRatio(false);

        javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle(1600, 900);
        overlay.setFill(javafx.scene.paint.Color.color(0, 0, 0, 0.75));

        VBox contentBox = new VBox(30);
        contentBox.setPadding(new Insets(200, 100, 50, 100));
        contentBox.setAlignment(Pos.TOP_CENTER);

        TextArea closingArea = new TextArea(CLOSING_TEXT);
        closingArea.setEditable(false);
        closingArea.setWrapText(true);
        closingArea.setFocusTraversable(false);
        closingArea.setPrefHeight(160);
        closingArea.setMaxWidth(800);
        closingArea.setStyle(
                "-fx-control-inner-background: #2b2b2b; " +
                        "-fx-background-color: #2b2b2b; " +
                        "-fx-text-fill: #dddddd; " +
                        "-fx-font-size: 16px; " +
                        "-fx-border-color: #555555; " +
                        "-fx-border-width: 1px; " +
                        "-fx-focus-color: transparent; " +
                        "-fx-faint-focus-color: transparent;");

        Button continueBtn = createButton("旅を続ける");
        continueBtn.setOnAction(e -> onConfirm.run()); // ラッチを解放

        contentBox.getChildren().addAll(closingArea, continueBtn);

        StackPane root = new StackPane();
        root.getChildren().addAll(backgroundView, overlay, contentBox);

        Scene scene = new Scene(root, 1600, 900);
        stage.setScene(scene);
    }

    /**
     * 引継ぎ候補リストを構築する。
     * player.getAbilities() + player.getTraits() を元に構築。
     * 両方空でも hp_plus_20 を必ず含める。
     */
    private List<CarryoverItem> buildCandidates(Player player) {
        List<CarryoverItem> list = new ArrayList<>();

        // その周で得たアビリティ
        if (player.getAbilities() != null) {
            for (String abilityId : player.getAbilities()) {
                AbilityData ad = CombatDataLoader.getAbility(abilityId);
                String name = (ad != null) ? ad.getName() : abilityId;
                String desc = (ad != null && ad.getDescription() != null)
                        ? String.join("", ad.getDescription())
                        : "";
                list.add(new CarryoverItem(abilityId, name, desc, true));
            }
        }

        // その周で得た特徴（baseTraits/inheritedTraitsは含まない）
        if (player.getTraits() != null) {
            for (String traitId : player.getTraits()) {
                TraitData td = TraitRegistry.getTraitById(traitId);
                String name = (td != null) ? td.getName() : traitId;
                String desc = (td != null) ? td.getDescription() : "";
                list.add(new CarryoverItem(traitId, name, desc, false));
            }
        }

        // フォールバック: 候補が空なら追加HP（hp_plus_20）を必ず追加
        if (list.isEmpty()) {
            TraitData fallback = TraitRegistry.getTraitById("hp_plus_20");
            String name = (fallback != null) ? fallback.getName() : "追加HP";
            String desc = (fallback != null) ? fallback.getDescription() : "最大HPが20増加する。";
            list.add(new CarryoverItem("hp_plus_20", name, desc, false));
        }

        return list;
    }

    /**
     * 選択結果をPlayerデータに反映する。
     * inherited系に選択したものを追加、一時リストをクリア、
     * 装備・money・statusEffectsをjobs.jsonの初期データでリセットする。
     */
    private void applyCarryoverResult(Player player) {
        if (selectedId == null)
            return;

        // 選択した1つを inherited に移動
        if (isAbility) {
            player.getInheritedAbilities().add(selectedId);
        } else {
            player.getInheritedTraits().add(selectedId);
        }

        // その周の一時アビリティ・特徴をクリア
        // nullの場合はgetterが初期化してくれるのでそのまま呼ぶ
        player.getAbilities().clear();
        player.getTraits().clear();

        // grade +1
        player.incrementGrade();

        // jobs.jsonから初期データを読み込んでリセット
        resetEquipmentFromJobData(player);

        // HP/AP を最大値まで回復
        player.setHp(player.getEffectiveMaxHp());
        player.setAp(player.getEffectiveMaxAp());

        // money を初期値に戻す
        player.setMoney(30);

        // statusEffects をクリア（clearAllStatusEffects内でnullチェック済み）
        player.clearAllStatusEffects();

        // キャラシJSONを上書き保存
        CharacterLoader loader = new CharacterLoader();
        boolean saved = loader.overwriteCharacter(player);
        if (!saved) {
            System.err.println("[CarryoverScreen] 警告: キャラシの上書き保存に失敗しました。");
        }
    }

    /**
     * jobs.jsonから対象職業の初期装備データを読み込み、Playerに再適用する。
     * CharacterCreationScreen.applyJobSkills() と同様のロジック。
     * インベントリ・装備を初期化した上で職業初期データで再構成する。
     */
    private void resetEquipmentFromJobData(Player player) {
        String job = player.getJob();
        if (job == null || job.isEmpty()) {
            System.err.println("[CarryoverScreen] resetEquipmentFromJobData: jobが未設定です。");
            return;
        }

        // インベントリ・装備をクリア
        player.getInventory().clear();
        player.setEquippedMainWeapon(null);
        player.setEquippedAccessories(new ArrayList<>());
        player.setReserveEquipments(new ArrayList<>());

        // jobs.jsonから対象職業を読み込む
        try {
            InputStream is = getClass().getResourceAsStream("/data/jobs.json");
            if (is == null) {
                System.err.println("[CarryoverScreen] jobs.json が見つかりません。");
                return;
            }
            JsonArray jobsArray = new Gson().fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8), JsonArray.class);
            is.close();

            for (JsonElement elem : jobsArray) {
                JsonObject jobObj = elem.getAsJsonObject();
                if (!job.equals(jobObj.get("id").getAsString()))
                    continue;

                // flavorItem（所持のみ）
                JsonElement flavorElem = jobObj.get("flavorItem");
                if (flavorElem != null && !flavorElem.isJsonNull()) {
                    player.addItem(flavorElem.getAsString());
                }

                // mainWeapon（所持・装備）
                JsonElement mainWeaponElem = jobObj.get("mainWeapon");
                if (mainWeaponElem != null && !mainWeaponElem.isJsonNull()) {
                    String weaponId = mainWeaponElem.getAsString();
                    player.addItem(weaponId);
                    player.equipMainWeapon(weaponId);
                }

                // reserveWeapons（所持・予備スロット）
                JsonArray reserveArr = jobObj.getAsJsonArray("reserveWeapons");
                if (reserveArr != null) {
                    for (JsonElement r : reserveArr) {
                        String reserveId = r.getAsString();
                        player.addItem(reserveId);
                        player.getReserveEquipments().add(reserveId);
                    }
                }

                // accessories（所持・装備）
                JsonArray accessoryArr = jobObj.getAsJsonArray("accessories");
                if (accessoryArr != null) {
                    for (JsonElement a : accessoryArr) {
                        String accessoryId = a.getAsString();
                        player.addItem(accessoryId);
                        player.equipAccessory(accessoryId);
                    }
                }

                break;
            }
        } catch (Exception e) {
            System.err.println("[CarryoverScreen] jobs.json の読み込みに失敗しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * IDと種別から表示名を取得するヘルパー
     */
    private String getDisplayNameById(String id, boolean isAbilityType) {
        if (isAbilityType) {
            AbilityData ad = CombatDataLoader.getAbility(id);
            return (ad != null) ? ad.getName() : id;
        } else {
            TraitData td = TraitRegistry.getTraitById(id);
            return (td != null) ? td.getName() : id;
        }
    }

    /**
     * ボタンを作成（GraveyardScreenと同じスタイル）
     */
    private Button createButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(350);
        button.setPrefHeight(55);
        button.setFont(Font.font("Arial", 18));
        button.setStyle(
                "-fx-background-color: #444444; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #666666; " +
                        "-fx-border-width: 2px;");
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #555555; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #888888; " +
                        "-fx-border-width: 2px;"));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: #444444; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #666666; " +
                        "-fx-border-width: 2px;"));
        return button;
    }

    /**
     * 引継ぎ候補を表現する内部クラス
     */
    private static class CarryoverItem {
        final String id;
        final String displayName;
        final String description;
        final boolean isAbility;

        CarryoverItem(String id, String displayName, String description, boolean isAbility) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.isAbility = isAbility;
        }
    }
}
