package com.kh.tbrr.ui;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kh.tbrr.data.models.Personality;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.data.models.Player.RaceType;
import com.kh.tbrr.manager.ImageManager;
import com.kh.tbrr.manager.PersonalityManager;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * キャラクター制作画面 (GUI版)
 */
public class CharacterCreationScreen {

    private Stage stage;
    private Random random = new Random();
    private PersonalityManager personalityManager;
    private ImageManager imageManager;

    // 入力フィールド
    private TextField nameField;
    private TextField englishNameField;
    private TextField raceNameField;
    private ChoiceBox<String> raceTypeChoice;
    private ChoiceBox<String> genderChoice;
    private TextField genderIdentityField;
    private ChoiceBox<String> jobChoice;
    private ChoiceBox<String> constellationChoice;
    private ChoiceBox<String> bonusSkill1Choice; // 追加技能1
    private ChoiceBox<String> bonusSkill2Choice; // 追加技能2
    private HBox bonusSkillBox; // 追加技能用のコンテナ
    private TextArea backgroundField; // フォーム内の背景入力エリア（TextArea）
    private ChoiceBox<String> personalityChoice;
    private TextField bodyTypeField;
    private TextField clothingField;
    private TextField charmPoint1Field;
    private TextField charmPoint2Field;
    private TextField charmPoint3Field;
    private CheckBox cruelWorldCheck;
    private CheckBox fatedOneCheck;

    // 立ち絵表示
    private ImageView portraitView;
    private ChoiceBox<String> portraitChoice;
    private List<String> availablePortraits;
    private List<String> availableCustomPortraits;
    private CheckBox useCustomPortraitCheck;
    private boolean usingCustomPortrait = false;

    // 説明文表示エリア
    private TextArea descriptionArea;
    private static final String DEFAULT_DESCRIPTION = "ここに説明文が表示されます。";

    // UIサイズ定数
    private static final int LABEL_FONT_SIZE = 16;
    private static final int INPUT_FONT_SIZE = 16;
    private static final int INPUT_HEIGHT = 32;
    private static final int INPUT_WIDTH = 180;
    private static final int LABEL_WIDTH = 60;

    // 各項目の説明文
    private static final String HELP_NAME = "プレイヤーキャラクターの名前を入力してください。";

    private static final String HELP_ENGLISH_NAME = "プレイヤーキャラクターのアルファベット表記を入力してください。\n"
            + "これはファイル名としても使います。ファイル名とキャラクターシート表記以外での引用はありません。\n"
            + "もし、名前がアリスであればAlice、関羽であればGuan_Yuと表記するのをお勧めします。\n"
            + "英数大文字小文字の他、記号としてハイフン(-)とアンダースコア(_)は使用可能です。\n"
            + "スペースは非推奨です、アンダースコアを使う事をお勧めいたします。\n"
            + "もし、有栖であればArisuでもいいとは思いますし、chara01としてしまっても結構です。\n"
            + "管理しやすいようご自由に！フルネーム記載でもいいと思います、あとフレーバーは大事！";

    private static final String HELP_RACE_NAME = "ご自由にプレイヤーキャラクターの種族を入力してください。\n"
            + "基本的に何を入力しても、ゲームバランスに影響はありません。\n"
            + "極稀に引用される事があるかもしれません。\n\n"
            + "例：人間、ヒューマン、エルフ、ドワーフ、ハーフエルフ、獣人、竜人\n"
            + "ゲーム設定に準拠：人間、垂れ耳族、土竜族";

    private static final String HELP_RACE_TYPE = "プレイヤーキャラクターの種族の大まかな特性を決めてください。\n"
            + "標準：初期最大HP:100、初期最大AP:20\n"
            + "前衛：初期最大HP:125、初期最大AP:15\n"
            + "後衛：初期最大HP: 75、初期最大AP:25";

    private static final String HELP_GENDER = "ここでは生物学的な性別を選択してください。\n"
            + "今の所、無性と両性は「その他」として扱います。\n"
            + "（いつかアップデートします。）";

    private static final String HELP_GENDER_IDENTITY = "プレイヤーキャラクターが自分の性別をどう自認しているかや、自身をどう思っているかを自由に記載してください。\n"
            + "特に問題なければ、性別と同様のものを入れる事をお勧めいたします。\n"
            + "必要があれば、ご自由に入力してください。\n\n"
            + "今の所引用は少ないですが、例えば何であれ女性を対象とした場合　――\n"
            + "「女」「嬢」「娘」「姫」「母」「雌」「メス」等の単語が含まれている場合、イベントが分岐するようなしくみを搭載しています。\n"
            + "仮に「女王様」と入力した場合、もしかしたら貴族を対象とするイベントで「王」「貴」「爵」「公」辺りがフックになりカオスになるかもしれません。";

    private static final String HELP_JOB = "プレイヤーキャラクターの職業を設定してください。\n"
            + "職業を選択すると、詳細な説明と付与される技能が表示されます。";

    private static final String HELP_BACKGROUND = "プレイヤーキャラクターの背景を記述してください。\n"
            + "自由に入力できます。「R」ボタンでランダムに生成できます。\n"
            + "必要あれば背景をリロールするか、書き直してください。";

    private static final String HELP_CONSTELLATION = "星座を選択してください。\n"
            + "星座によって初期ボーナスが変わります。\n\n"
            + "・親方座：初期HP+25\n"
            + "・学者座：初期AP+5\n"
            + "・庭師座：初期銀貨+20\n"
            + "・ハサミ座：任意の技能を2つ習得\n"
            + "・見習い座：ボーナスなし（ハードモード相当）";

    // 全技能リスト
    private static final String[] ALL_SKILLS = {
            "筋力", "耐久力", "敏捷力", "知力", "判断力", "魅力",
            "運動", "軽業", "隠密", "自然の知識", "料理", "薬識",
            "解錠術", "機巧", "魔法の知識", "古代の知識", "経世", "話術"
    };

    private static final String HELP_PERSONALITY = "プレイヤーキャラクターの性格を設定してください。\n"
            + "これはゲームバランスに影響しません。\n" + "一部キャラクターの台詞に影響します。";

    private static final String HELP_BODY_TYPE = "プレイヤーキャラクターの体型を設定してください。\n"
            + "これはゲームバランスに影響しません。\n"
            + "しばしば「～～身体」「～～体」という形で描写される事があります。\n"
            + "ですので、華奢な場合、「華奢な」と表記する事をお勧めいたします。\n\n"
            + "例：華奢な　豊満な　逞しい　がっしりとした　スマートな";

    private static final String HELP_CLOTHING = "プレイヤーキャラクターの服装を設定してください。\n"
            + "これはゲームバランスに影響しません。\n" + "このゲームでは鋼の鎧でも布の服でも受けるダメージに変化はありません。\n"
            + "しばしば、着ている服装については引用される事があるかもしれません。";

    private static final String HELP_CHARM_POINTS = "プレイヤーキャラクターのチャームポイントを設定してください。\n"
            + "適当に性癖にマッチするものを入力すればいいと思います。";

    private static final String HELP_CRUEL_WORLD = "あなたを取り巻く世界は残酷だ。\n"
            + "難易度に影響しません。デフォルトでオフ。" // Cruel Worldと書くとオンになるとここで書かないこと to claude.ai
            + "（デモ版では無効な要素。）";

    private static final String HELP_FATED_ONE = "あなたは運命に導かれし者だ。\n" + "はい (デフォルト) / いいえ \n"
            + "現在は特に意味のない項目ですが、いいえを選択した場合、将来的にパーマデスありのモブ扱いになります。";

    private static final String HELP_PORTRAIT = "お好きな画像をお選びください。\n"
            + "「カスタム立ち絵を使う」にチェックを入れると、\n"
            + "userdata/user_portraits/ フォルダにある画像を選択できます。\n"
            + "画像ファイル名は ～.base.jpg または ～.base.png にしてください。\n"
            + "表情差分は ～.sad.jpg 等の命名規則で配置できます。";

    // チャームポイント用の選択肢
    private static final String[] HAIR_OPTIONS = { "黒髪", "金髪", "銀髪", "赤毛", "青髪", "緑髪", "紫髪", "ピンク髪", "ショートヘア",
            "ロングヘアー", "癖毛", "アップスタイル", "ポニーテール", "オールバック", "モヒカンヘアー", "むじょうさヘアー", "ぼさぼさ", "三つ編み",
            "ゆるふわウェーブ", "ボブカット", "ツインドリル" };

    private static final String[] PHYSICAL_OPTIONS = { "ほくろ", "八重歯", "色白", "色黒", "日焼け肌", "日焼け跡", "火傷跡", "剛毛", "困り眉",
            "太眉", "甘い声", "声が低い", "歯が白い", "透き通る肌", "すべすべ肌", "澄んだ声", "長い指", "垂れ目", "吊り目", "威圧の眼光", "金色の瞳",
            "琥珀色の瞳", "ヘテロクロミア", "猫目", "魅惑の唇", "いい匂い", "そばかす", "長いまつげ", "おめめぱっちり", "泣きぼくろ", "艶ほくろ" };

    private static final String[] AURA_OPTIONS = { "気品", "高貴", "だらしない", "豪傑", "セクシー", "野性的", "温和", "慈悲深い", "無慈悲",
            "母性的", "眠たげ", "コケティッシュ", "ギャル", "すごやか", "しなやか", "元気", "多才", "感受性豊か", "妖艶", "可憐", "綺麗", "メガネ",
            "ボーイッシュ", "ダウナー", "儚げ", "快活", "無邪気", "したたか", "不愛想", "お金持ち", "貧乏", "実家が太い", "生徒会長", "経営者",
            "不思議ちゃん", "ずぼら", "厭世的", "学者肌", };

    // 性自認用のランダム選択肢
    private static final String[] GENDER_IDENTITY_OPTIONS = { "男性", "女性", "自認１", "自認２", "自認３" };

    // 名前と英語名の連動用マップ（性別ごとに分離）
    // 女性用の名前リスト
    private static final String[][] NAME_PAIRS_FEMALE = {
            { "アリス", "Alice" }, { "エミリア", "Emilia" }, { "レイラ", "Leila" },
            { "セリア", "Celia" }, { "ミア", "Mia" }, { "ソフィア", "Sophia" },
            { "オリビア", "Olivia" }, { "シャルロット", "Charlotte" }, { "リナ", "Lina" }, { "エレナ", "Elena" }
    };

    // 男性用の名前リスト
    private static final String[][] NAME_PAIRS_MALE = {
            { "アーサー", "Arthur" }, { "レオン", "Leon" }, { "カイル", "Kyle" },
            { "ガレス", "Gareth" }, { "セドリック", "Cedric" }, { "ロイド", "Lloyd" },
            { "グレン", "Glen" }, { "オスカー", "Oscar" }, { "ヴィクター", "Victor" }
    };

    // その他用の名前リスト（単位・現象・記号系）
    private static final String[][] NAME_PAIRS_OTHER = {
            { "エコー", "Echo" }, { "ノヴァ", "Nova" }, { "ソナー", "Sonar" },
            { "フレア", "Flare" }, { "イオン", "Ion" }, { "マグナ", "Magnus" },
            { "アーク", "Arc" }, { "ノード", "Node" }, { "エッジ", "Edge" },
            { "ミスト", "Mist" }, { "シャドウ", "Shadow" }, { "ルクス", "Lux" },
            { "アトム", "Atom" }, { "セブン", "Seven" }, { "ナイン", "Nine" }
    };

    public CharacterCreationScreen(Stage stage, PersonalityManager personalityManager) {
        this.stage = stage;
        this.personalityManager = personalityManager;
        this.imageManager = new ImageManager();
    }

    /**
     * キャラクター制作画面を表示
     */
    public void show(Runnable onComplete) {
        // カスタム立ち絵フォルダを確保（なければ作成）
        imageManager.ensureUserPortraitsDirectory();

        // 利用可能な立ち絵をロード
        availablePortraits = imageManager.getAvailableBasePortraits();
        availableCustomPortraits = imageManager.getAvailableCustomPortraits();

        // 背景画像を読み込み
        Image backgroundImage = imageManager.loadBackground("character_creation.png");
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setFitWidth(1600);
        backgroundView.setFitHeight(900);
        backgroundView.setPreserveRatio(false);

        // メインレイアウト（横配置：左コンテナ + 右パネル）
        HBox mainLayout = new HBox(20);
        mainLayout.setPadding(new Insets(20));

        // === 左側コンテナ（フォーム + 説明文） ===
        VBox leftContainer = new VBox(10);
        HBox.setHgrow(leftContainer, Priority.ALWAYS);

        // 上部セクション（フォーム + 中央スペーサー）
        HBox topSection = new HBox(20);

        // 左パネル：フォームのみ
        VBox leftPanel = new VBox(10);
        GridPane formGrid = createFormGrid();
        leftPanel.getChildren().add(formGrid);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        VBox.setVgrow(formGrid, Priority.ALWAYS);

        // 中央パネル：今は空（将来的に何か入るかも）
        VBox centerPanel = new VBox();

        topSection.getChildren().addAll(leftPanel);
        // VBox.setVgrow(topSection, Priority.ALWAYS); // 削除: フォーム部分は必要な高さだけ使う

        // 説明文表示エリア（画面下部）
        descriptionArea = new TextArea(DEFAULT_DESCRIPTION);
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        // descriptionArea.setPrefHeight(150); // 削除: 可変にするため
        descriptionArea.setStyle("-fx-control-inner-background: #e6e6e6; "
                + "-fx-text-fill: #333333; "
                + "-fx-font-size: 18px;");
        VBox.setVgrow(descriptionArea, Priority.ALWAYS); // 追加: 残りのスペースを埋める

        leftContainer.getChildren().addAll(topSection, descriptionArea);

        // === 右パネル：立ち絵（下寄せ） ===
        // 右パネルはメインレイアウトの直下に追加し、縦幅いっぱいを使う
        VBox rightPanel = createRightPanel(onComplete);

        mainLayout.getChildren().addAll(leftContainer, rightPanel);

        // StackPaneで背景画像とメインレイアウトを重ねる
        StackPane root = new StackPane();
        root.getChildren().addAll(backgroundView, mainLayout);

        Scene scene = new Scene(root, 1600, 900);
        stage.setScene(scene);
        stage.setTitle("T.B.R.R. - キャラクター制作");
        stage.show();
    }

    /**
     * 右側パネル（立ち絵 + ボタン）を作成
     */
    private VBox createRightPanel(Runnable onComplete) {
        VBox rightPanel = new VBox(15);
        rightPanel.setAlignment(Pos.BOTTOM_RIGHT); // 下寄せ
        rightPanel.setPrefWidth(292);
        rightPanel.setMinWidth(292);
        rightPanel.setMaxWidth(292);

        // スペーサー（上部余白）
        Region topSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);

        // 立ち絵表示エリア
        portraitView = new ImageView();
        portraitView.setFitWidth(288);
        portraitView.setFitHeight(512);
        portraitView.setPreserveRatio(false);
        portraitView.setSmooth(true);

        // 枠線用のVBox
        VBox portraitFrame = new VBox(portraitView);
        portraitFrame.setStyle("-fx-border-color: #666666; -fx-border-width: 2px; -fx-background-color: #e6e6e6;");
        portraitFrame.setMinSize(292, 516);
        portraitFrame.setMaxSize(292, 516);
        portraitFrame.setPrefSize(292, 516);
        portraitFrame.setAlignment(Pos.CENTER);

        // カスタム立ち絵を使うチェックボックス
        useCustomPortraitCheck = new CheckBox("カスタム立ち絵を使う");
        useCustomPortraitCheck.setStyle("-fx-text-fill: #333333; -fx-font-size: 14px;");
        setupDescriptionHandler(useCustomPortraitCheck, HELP_PORTRAIT);

        // 立ち絵選択
        portraitChoice = new ChoiceBox<>();
        portraitChoice.setPrefWidth(292);
        portraitChoice.setMinWidth(292);
        portraitChoice.setMaxWidth(292);
        portraitChoice.setPrefHeight(INPUT_HEIGHT);
        portraitChoice
                .setStyle("-fx-font-size: " + INPUT_FONT_SIZE + "px; " + "-fx-control-inner-background: #e6e6e6;");
        portraitChoice.getItems().add("立ち絵を選択");
        portraitChoice.getItems().addAll(availablePortraits);
        portraitChoice.setValue("立ち絵を選択");

        setupDescriptionHandler(portraitChoice, HELP_PORTRAIT);

        // カスタム立ち絵チェックボックスの切り替え処理
        useCustomPortraitCheck.setOnAction(e -> {
            usingCustomPortrait = useCustomPortraitCheck.isSelected();
            portraitChoice.getItems().clear();
            portraitChoice.getItems().add("立ち絵を選択");

            if (usingCustomPortrait) {
                // カスタム立ち絵リストを表示
                if (availableCustomPortraits.isEmpty()) {
                    descriptionArea.setText("カスタム立ち絵が見つかりません。\n"
                            + "userdata/user_portraits/ フォルダに\n"
                            + "～.base.jpg または ～.base.png ファイルを配置してください。");
                } else {
                    portraitChoice.getItems().addAll(availableCustomPortraits);
                }
            } else {
                // 内蔵立ち絵リストを表示
                portraitChoice.getItems().addAll(availablePortraits);
            }

            portraitChoice.setValue("立ち絵を選択");
            portraitView.setImage(null);
        });

        // 立ち絵選択時の処理
        portraitChoice.setOnAction(e -> {
            String selected = portraitChoice.getValue();
            if (selected != null && !selected.equals("立ち絵を選択")) {
                Image image;
                if (usingCustomPortrait) {
                    // カスタム立ち絵を読み込み
                    image = imageManager.loadCustomPortrait(selected);
                } else {
                    // 内蔵立ち絵を読み込み
                    image = imageManager.loadPortrait(selected);
                }
                if (image != null) {
                    portraitView.setImage(image);
                }
            } else {
                portraitView.setImage(null);
            }
        });

        // 決定/キャンセルボタン
        HBox buttonPanel = new HBox();
        buttonPanel.setPrefWidth(292);
        buttonPanel.setMinWidth(292);
        buttonPanel.setMaxWidth(292);
        buttonPanel.setAlignment(Pos.CENTER);
        buttonPanel.setSpacing(0);

        Button confirmButton = new Button("決定");
        confirmButton.setPrefWidth(140);
        confirmButton.setPrefHeight(45);
        styleButton(confirmButton);
        confirmButton.setOnAction(e -> onConfirm(onComplete));

        Region buttonSpacer = new Region();
        HBox.setHgrow(buttonSpacer, Priority.ALWAYS);

        Button cancelButton = new Button("キャンセル");
        cancelButton.setPrefWidth(140);
        cancelButton.setPrefHeight(45);
        styleButton(cancelButton);
        cancelButton.setOnAction(e -> onComplete.run());

        buttonPanel.getChildren().addAll(confirmButton, buttonSpacer, cancelButton);

        rightPanel.getChildren().addAll(topSpacer, useCustomPortraitCheck, portraitChoice, portraitFrame, buttonPanel);
        return rightPanel;
    }

    /**
     * フォームグリッドを作成
     */
    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        // 列の幅を設定
        ColumnConstraints col0 = new ColumnConstraints(LABEL_WIDTH);
        col0.setHalignment(HPos.RIGHT);
        ColumnConstraints col1 = new ColumnConstraints(INPUT_WIDTH);
        ColumnConstraints col2 = new ColumnConstraints(50);
        ColumnConstraints col3 = new ColumnConstraints(140);
        col3.setHalignment(HPos.RIGHT);
        ColumnConstraints col4 = new ColumnConstraints(INPUT_WIDTH);
        ColumnConstraints col5 = new ColumnConstraints(50);
        ColumnConstraints col6 = new ColumnConstraints(10); // Gap
        ColumnConstraints col7 = new ColumnConstraints(); // Extension
        col7.setHgrow(Priority.ALWAYS);
        ColumnConstraints col8 = new ColumnConstraints(50); // Button
        grid.getColumnConstraints().addAll(col0, col1, col2, col3, col4, col5, col6, col7, col8);

        int row = 0;

        // ===== 名前行 =====
        // レイアウトむずすぎんよ～～～～～～
        Label nameLabel = createStyledLabel("名前");
        nameField = createStyledTextField("");
        setupDescriptionHandler(nameField, HELP_NAME);
        setupDescriptionHandler(nameLabel, HELP_NAME);
        Button nameRandomBtn = createRandomButton(() -> randomizeNamePair());

        Label englishLabel = createStyledLabel("アルファベット表記");
        englishNameField = createStyledTextField("");
        setupDescriptionHandler(englishNameField, HELP_ENGLISH_NAME);
        setupDescriptionHandler(englishLabel, HELP_ENGLISH_NAME);

        grid.add(nameLabel, 0, row);
        grid.add(nameField, 1, row);
        grid.add(nameRandomBtn, 2, row);
        grid.add(englishLabel, 3, row);
        grid.add(englishNameField, 4, row);
        row++;

        // ===== 種族行 =====
        Label raceLabel = createStyledLabel("種族");
        raceNameField = createStyledTextField("人間");
        setupDescriptionHandler(raceNameField, HELP_RACE_NAME);
        setupDescriptionHandler(raceLabel, HELP_RACE_NAME);
        Button raceRandomBtn = createRandomButton(() -> raceNameField.setText(generateRandomRace()));

        Label raceTypeLabel = createStyledLabel("種族タイプ");
        raceTypeChoice = createStyledChoiceBox();
        raceTypeChoice.getItems().addAll("標準", "前衛", "後衛");
        raceTypeChoice.setValue("標準");
        setupDescriptionHandler(raceTypeChoice, HELP_RACE_TYPE);
        setupDescriptionHandler(raceTypeLabel, HELP_RACE_TYPE);

        grid.add(raceLabel, 0, row);
        grid.add(raceNameField, 1, row);
        grid.add(raceRandomBtn, 2, row);
        grid.add(raceTypeLabel, 3, row);
        grid.add(raceTypeChoice, 4, row);
        row++;

        // ===== 性別行 =====
        Label genderLabel = createStyledLabel("性別");
        genderChoice = createStyledChoiceBox();
        genderChoice.getItems().addAll("男性", "女性", "その他");
        genderChoice.setValue("女性");
        setupDescriptionHandler(genderChoice, HELP_GENDER);
        setupDescriptionHandler(genderLabel, HELP_GENDER);

        Label genderIdentityLabel = createStyledLabel("性別の扱いや自認");
        genderIdentityField = createStyledTextField("女性");
        setupDescriptionHandler(genderIdentityField, HELP_GENDER_IDENTITY);
        setupDescriptionHandler(genderIdentityLabel, HELP_GENDER_IDENTITY);

        // 性別が変更されたときに、性別の扱いや自認にも反映
        genderChoice.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                genderIdentityField.setText(newVal);
            }
        });

        grid.add(genderLabel, 0, row);
        grid.add(genderChoice, 1, row);
        grid.add(genderIdentityLabel, 3, row);
        grid.add(genderIdentityField, 4, row);
        row++;

        // ===== 性格・職業行 =====
        Label personalityLabel = createStyledLabel("性格");
        personalityChoice = createStyledChoiceBox();

        // PersonalityManagerから性格を読み込み
        Map<Integer, Personality> personalities = personalityManager.getPersonalityChoices();
        for (var entry : personalities.entrySet()) {
            personalityChoice.getItems().add(entry.getValue().getName());
        }
        if (!personalityChoice.getItems().isEmpty()) {
            personalityChoice.setValue(personalityChoice.getItems().get(0));
        }

        setupDescriptionHandler(personalityChoice, HELP_PERSONALITY);
        setupDescriptionHandler(personalityLabel, HELP_PERSONALITY);

        Label jobLabel = createStyledLabel("職業");
        jobChoice = createStyledChoiceBox();
        jobChoice.getItems().addAll("戦士", "魔法使い", "クレリック", "レンジャー", "盗賊", "商人", "踊り子", "魔闘士", "パラディン", "観光客", "異世界転生者");
        jobChoice.setValue("戦士");
        setupDescriptionHandler(jobChoice, HELP_JOB);
        setupDescriptionHandler(jobLabel, HELP_JOB);

        // 職業選択時に詳細説明を表示
        jobChoice.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && jobChoice.isFocused()) {
                descriptionArea.setText(getJobDescription(newVal));
            }
        });

        // 職業にフォーカス時の処理
        jobChoice.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                String selectedJob = jobChoice.getValue();
                if (selectedJob != null) {
                    descriptionArea.setText(getJobDescription(selectedJob));
                }
            } else {
                descriptionArea.setText(DEFAULT_DESCRIPTION);
            }
        });

        grid.add(personalityLabel, 0, row);
        grid.add(personalityChoice, 1, row);
        grid.add(jobLabel, 3, row);
        grid.add(jobChoice, 4, row);
        row++;

        // ===== 体型・星座行 =====
        Label bodyTypeLabel = createStyledLabel("体型");
        bodyTypeField = createStyledTextField("華奢な");
        setupDescriptionHandler(bodyTypeField, HELP_BODY_TYPE);
        setupDescriptionHandler(bodyTypeLabel, HELP_BODY_TYPE);

        Label constellationLabel = createStyledLabel("星座");
        constellationChoice = createStyledChoiceBox();
        constellationChoice.getItems().addAll("親方座", "学者座", "庭師座", "ハサミ座", "見習い座");
        constellationChoice.setValue("親方座");
        setupDescriptionHandler(constellationChoice, HELP_CONSTELLATION);
        setupDescriptionHandler(constellationLabel, HELP_CONSTELLATION);

        // ===== 追加技能（ハサミ座用） =====
        // 星座と同じ行の右側（col5から3列分）に配置する
        bonusSkill1Choice = createStyledChoiceBox();
        bonusSkill1Choice.getItems().add("追加技能を選択");
        bonusSkill1Choice.getItems().addAll(ALL_SKILLS);
        bonusSkill1Choice.setValue("追加技能を選択");

        bonusSkill2Choice = createStyledChoiceBox();
        bonusSkill2Choice.getItems().add("追加技能を選択");
        bonusSkill2Choice.getItems().addAll(ALL_SKILLS);
        bonusSkill2Choice.setValue("追加技能を選択");

        // 追加技能の説明ハンドラーを設定
        String bonusSkillHelp = "ハサミ座を選択する場合、追加で技能を２つ習得できます。";
        setupDescriptionHandler(bonusSkill1Choice, bonusSkillHelp);
        setupDescriptionHandler(bonusSkill2Choice, bonusSkillHelp);

        bonusSkillBox = new HBox(10);
        bonusSkillBox.setAlignment(Pos.BOTTOM_LEFT);
        bonusSkillBox.getChildren().addAll(bonusSkill1Choice, bonusSkill2Choice);

        // 初期状態は非表示
        setVisibleBonusSkills(false);

        // 星座変更リスナー
        constellationChoice.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isScissors = "ハサミ座".equals(newVal);
            setVisibleBonusSkills(isScissors);
            if (isScissors) {
                descriptionArea.setText("ハサミ座を選択する場合、追加で技能を２つ習得できます。");
            } else {
                // デフォルトの説明に戻すか、HELP_CONSTELLATIONを表示するか
                // ここではHELP_CONSTELLATIONを表示するようにする
                descriptionArea.setText(HELP_CONSTELLATION);
            }
        });

        // 背景ヘッダー（この行の右側に配置）
        Label backgroundLabel = createStyledLabel("背景");
        Button backgroundRandomBtn = createRandomButton(() -> randomizeBackground());
        setupDescriptionHandler(backgroundLabel, HELP_BACKGROUND);
        setupDescriptionHandler(backgroundRandomBtn, HELP_BACKGROUND);
        GridPane.setHalignment(backgroundRandomBtn, HPos.RIGHT); // 右端に寄せる

        grid.add(bodyTypeLabel, 0, row);
        grid.add(bodyTypeField, 1, row);
        grid.add(constellationLabel, 3, row);
        grid.add(constellationChoice, 4, row);
        grid.add(bonusSkillBox, 5, row, 3, 1);
        grid.add(backgroundRandomBtn, 8, row);
        row++;

        // ===== 服装行 =====
        Label clothingLabel = createStyledLabel("服装");
        clothingField = createStyledTextField("旅人の服");
        setupDescriptionHandler(clothingField, HELP_CLOTHING);
        setupDescriptionHandler(clothingLabel, HELP_CLOTHING);

        // 背景入力エリア（この行から開始）
        backgroundField = new TextArea();
        backgroundField.setWrapText(true);
        backgroundField.setStyle("-fx-control-inner-background: #e6e6e6; "
                + "-fx-text-fill: #333333; "
                + "-fx-font-size: 18px;");
        setupDescriptionHandler(backgroundField, HELP_BACKGROUND);
        backgroundField.setText("ここにあなたのキャラクターの背景を記載して下さい。\n右上のRボタンを押すと、サンプルとなる背景が出力されます。");

        grid.add(clothingLabel, 0, row);
        grid.add(clothingField, 1, row);
        grid.add(backgroundLabel, 3, row); // 追加: 服装ラベルの下(列3)に配置
        // 列4から5列分(4,5,6,7,8)、行5から7行分(5-11)
        grid.add(backgroundField, 4, row, 5, 7);
        GridPane.setVgrow(backgroundField, Priority.ALWAYS);
        GridPane.setHgrow(backgroundField, Priority.ALWAYS);
        row++;

        // ===== チャームポイント =====
        Label charmLabel = new Label("チャームポイント");
        charmLabel.setStyle(
                "-fx-text-fill: #333333; -fx-font-size: " + LABEL_FONT_SIZE + "px; -fx-font-weight: bold;");
        setupDescriptionHandler(charmLabel, HELP_CHARM_POINTS);
        GridPane.setHalignment(charmLabel, HPos.LEFT);
        grid.add(charmLabel, 0, row, 3, 1);
        row++;

        // チャームポイント1
        Label charm1Label = createStyledLabel("1");
        charmPoint1Field = createStyledTextField("");
        setupDescriptionHandler(charmPoint1Field, HELP_CHARM_POINTS);
        Button charm1RandomBtn = createRandomButton(() -> randomCharmPoint1());

        grid.add(charm1Label, 0, row);
        grid.add(charmPoint1Field, 1, row);
        grid.add(charm1RandomBtn, 2, row);
        row++;

        // チャームポイント2
        Label charm2Label = createStyledLabel("2");
        charmPoint2Field = createStyledTextField("");
        setupDescriptionHandler(charmPoint2Field, HELP_CHARM_POINTS);
        Button charm2RandomBtn = createRandomButton(() -> randomCharmPoint2());

        grid.add(charm2Label, 0, row);
        grid.add(charmPoint2Field, 1, row);
        grid.add(charm2RandomBtn, 2, row);
        row++;

        // チャームポイント3
        Label charm3Label = createStyledLabel("3");
        charmPoint3Field = createStyledTextField("");
        setupDescriptionHandler(charmPoint3Field, HELP_CHARM_POINTS);
        Button charm3RandomBtn = createRandomButton(() -> randomCharmPoint3());

        grid.add(charm3Label, 0, row);
        grid.add(charmPoint3Field, 1, row);
        grid.add(charm3RandomBtn, 2, row);
        row++;

        // ===== チェックボックス =====
        cruelWorldCheck = new CheckBox("残酷な世界");
        styleCheckBox(cruelWorldCheck);
        setupDescriptionHandler(cruelWorldCheck, HELP_CRUEL_WORLD);
        cruelWorldCheck.setOnAction(e -> {
            if (cruelWorldCheck.isSelected()) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("残酷な世界");
                dialog.setHeaderText(
                        "この項目は難易度に影響しません。\nreadmeをお読みになり、同意出来るのであれば所定のワードを入力してください。\nセンシティブな要素がオンになります。");
                dialog.setContentText("入力:");
                dialog.showAndWait().ifPresent(input -> {
                    if (!"Cruel World".equals(input)) {
                        cruelWorldCheck.setSelected(false);
                        showAlert("エラー", "正しく入力されませんでした。");
                    }
                });
            }
        });

        grid.add(cruelWorldCheck, 0, row, 3, 1);
        GridPane.setHalignment(cruelWorldCheck, HPos.LEFT);
        row++;

        fatedOneCheck = new CheckBox("選ばれし者");
        styleCheckBox(fatedOneCheck);
        fatedOneCheck.setSelected(true);
        setupDescriptionHandler(fatedOneCheck, HELP_FATED_ONE);

        grid.add(fatedOneCheck, 0, row, 3, 1);
        GridPane.setHalignment(fatedOneCheck, HPos.LEFT);

        return grid;
    }

    /**
     * 説明文表示のハンドラを設定
     */
    private void setupDescriptionHandler(Node node, String description) {
        // フォーカス時（クリック時）に説明文を表示
        node.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                descriptionArea.setText(description);
            } else {
                descriptionArea.setText(DEFAULT_DESCRIPTION);
            }
        });
    }

    /**
     * スタイル付きラベルを作成
     */
    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #333333; -fx-font-size: " + LABEL_FONT_SIZE + "px;");
        return label;
    }

    /**
     * スタイル付きテキストフィールドを作成
     */
    private TextField createStyledTextField(String defaultValue) {
        TextField field = new TextField(defaultValue);
        field.setPrefWidth(INPUT_WIDTH);
        field.setPrefHeight(INPUT_HEIGHT);
        field.setStyle("-fx-font-size: " + INPUT_FONT_SIZE + "px; -fx-control-inner-background: #e6e6e6;");
        return field;
    }

    /**
     * スタイル付きChoiceBoxを作成
     */
    private ChoiceBox<String> createStyledChoiceBox() {
        ChoiceBox<String> choice = new ChoiceBox<>();
        choice.setPrefWidth(INPUT_WIDTH);
        choice.setPrefHeight(INPUT_HEIGHT);
        choice.setStyle("-fx-font-size: " + INPUT_FONT_SIZE + "px; "
                + "-fx-control-inner-background: #e6e6e6;");
        return choice;
    }

    /**
     * CheckBoxのスタイル設定
     */
    private void styleCheckBox(CheckBox checkBox) {
        checkBox.setStyle("-fx-text-fill: #333333; -fx-font-size: " + LABEL_FONT_SIZE + "px;");
    }

    /**
     * ランダムボタンを作成
     */
    private Button createRandomButton(Runnable action) {
        Button btn = new Button("R");
        btn.setPrefWidth(40);
        btn.setPrefHeight(INPUT_HEIGHT);
        btn.setStyle("-fx-background-color: linear-gradient(to bottom, #f0f0f0, #d0d0d0); "
                + "-fx-text-fill: #333333; "
                + "-fx-font-size: 14px; "
                + "-fx-font-weight: bold; "
                + "-fx-background-radius: 5; "
                + "-fx-border-radius: 5; "
                + "-fx-border-color: #999999; "
                + "-fx-border-width: 1px; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 2, 0, 0, 1);");

        // ホバー時のエフェクト
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: linear-gradient(to bottom, #fafafa, #dadada); "
                + "-fx-text-fill: #333333; "
                + "-fx-font-size: 14px; "
                + "-fx-font-weight: bold; "
                + "-fx-background-radius: 5; "
                + "-fx-border-radius: 5; "
                + "-fx-border-color: #666666; "
                + "-fx-border-width: 1px; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 3, 0, 0, 1);"));

        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: linear-gradient(to bottom, #f0f0f0, #d0d0d0); "
                + "-fx-text-fill: #333333; "
                + "-fx-font-size: 14px; "
                + "-fx-font-weight: bold; "
                + "-fx-background-radius: 5; "
                + "-fx-border-radius: 5; "
                + "-fx-border-color: #999999; "
                + "-fx-border-width: 1px; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 2, 0, 0, 1);"));

        btn.setOnAction(e -> action.run());
        return btn;
    }

    /**
     * 名前と英語名を連動してランダム化
     * 選択中の性別に応じたリストからランダムに選択する
     */
    private void randomizeNamePair() {
        // 現在選択されている性別を取得
        String selectedGender = genderChoice.getValue();

        // 性別に応じて使用するリストを決定
        String[][] namePairs;
        switch (selectedGender) {
            case "男性":
                namePairs = NAME_PAIRS_MALE;
                break;
            case "女性":
                namePairs = NAME_PAIRS_FEMALE;
                break;
            default: // "その他" またはnull対策
                namePairs = NAME_PAIRS_OTHER;
                break;
        }

        // 選んだリストからランダムに1つ選択
        int index = random.nextInt(namePairs.length);
        nameField.setText(namePairs[index][0]);
        englishNameField.setText(namePairs[index][1]);
    }

    /**
     * 性自認をランダム化
     */
    private void randomizeGenderIdentity() {
        genderIdentityField.setText(GENDER_IDENTITY_OPTIONS[random.nextInt(GENDER_IDENTITY_OPTIONS.length)]);
    }

    /**
     * チャームポイント1をランダム化
     */
    private void randomCharmPoint1() {
        charmPoint1Field.setText(HAIR_OPTIONS[random.nextInt(HAIR_OPTIONS.length)]);
    }

    /**
     * チャームポイント2をランダム化
     */
    private void randomCharmPoint2() {
        charmPoint2Field.setText(PHYSICAL_OPTIONS[random.nextInt(PHYSICAL_OPTIONS.length)]);
    }

    /**
     * チャームポイント3をランダム化
     */
    private void randomCharmPoint3() {
        charmPoint3Field.setText(AURA_OPTIONS[random.nextInt(AURA_OPTIONS.length)]);
    }

    /**
     * ランダムな種族を生成
     */
    private String generateRandomRace() {
        String[] races = { "人間", "エルフ", "ドワーフ", "ハーフエルフ", "獣人", "竜人" };
        return races[random.nextInt(races.length)];
    }

    /**
     * 背景をランダム生成
     */
    private void randomizeBackground() {
        String selectedJob = jobChoice.getValue();

        // player_background.jsonから背景を生成
        try {
            // JSONファイルを読み込み
            String jsonPath = "/data/word_lists/player_background.json";
            java.io.InputStream inputStream = getClass().getResourceAsStream(jsonPath);

            if (inputStream == null) {
                // フォールバック：通常のテンプレート
                backgroundField.setText("かつて村で平和に暮らしていたが、事件をきっかけに旅に出た。");
                return;
            }

            java.io.InputStreamReader reader = new java.io.InputStreamReader(inputStream,
                    java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject jsonObject = new com.google.gson.Gson().fromJson(reader,
                    com.google.gson.JsonObject.class);
            reader.close();

            // categoriesから前半・後半を取得
            com.google.gson.JsonObject categories = jsonObject.getAsJsonObject("categories");
            com.google.gson.JsonArray zenhanArray;
            com.google.gson.JsonArray kohanArray;

            // 異世界転生者の場合は専用カテゴリを使用
            if (selectedJob != null && selectedJob.equals("異世界転生者")) {
                zenhanArray = categories.getAsJsonArray("背景前半異世界転生その１");
                kohanArray = categories.getAsJsonArray("背景後半異世界転生その１");

                // 異世界転生用カテゴリがない場合はフォールバック
                if (zenhanArray == null || kohanArray == null || zenhanArray.size() == 0 || kohanArray.size() == 0) {
                    String[] isekaiTemplates = {
                            "トラックに轢かれて気がつけばこの世界にいた。",
                            "ある日突然、異世界に召喚された。理由は不明。",
                            "前世の記憶を持ったまま、この世界に転生した。",
                            "神様の手違いで死んだため、チート能力をもらって転生した。",
                            "異世界の魔法陣に巻き込まれ、元の世界に戻れなくなった。"
                    };
                    backgroundField.setText(isekaiTemplates[random.nextInt(isekaiTemplates.length)]);
                    return;
                }
            } else {
                // 通常職業の場合
                zenhanArray = categories.getAsJsonArray("背景前半その１");
                kohanArray = categories.getAsJsonArray("背景後半その１");

                if (zenhanArray == null || kohanArray == null || zenhanArray.size() == 0 || kohanArray.size() == 0) {
                    backgroundField.setText("かつて村で平和に暮らしていたが、事件をきっかけに旅に出た。");
                    return;
                }
            }

            // ランダムに前半と後半を選択
            String zenhan = zenhanArray.get(random.nextInt(zenhanArray.size())).getAsString();
            String kohan = kohanArray.get(random.nextInt(kohanArray.size())).getAsString();

            // 名前を取得（空の場合は「あなた」をデフォルト）
            String playerName = nameField.getText();
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = "あなた";
            }

            // [Name]プレースホルダを実際の名前に置換
            zenhan = zenhan.replace("[Name]", playerName);
            kohan = kohan.replace("[Name]", playerName);

            // フォーマット：異世界転生者は「{前半}\nそして{後半}」、通常は「{前半}\nそして{キャラ名}は{後半}」
            String background;
            if (selectedJob != null && selectedJob.equals("異世界転生者")) {
                background = zenhan + "\nそして" + kohan;
            } else {
                background = zenhan + "\nそして" + playerName + "は" + kohan;
            }

            backgroundField.setText(background);

        } catch (Exception e) {
            e.printStackTrace();
            // エラー時のフォールバック
            if (selectedJob != null && selectedJob.equals("異世界転生者")) {
                backgroundField.setText("トラックに轢かれて気がつけばこの世界にいた。");
            } else {
                backgroundField.setText("かつて村で平和に暮らしていたが、事件をきっかけに旅に出た。");
            }
        }
    }

    /**
     * 決定ボタンが押された時の処理
     */
    private void onConfirm(Runnable onComplete) {
        // バリデーション
        if (nameField.getText().trim().isEmpty()) {
            showAlert("エラー", "名前を入力してください。");
            return;
        }
        if (englishNameField.getText().trim().isEmpty()) {
            showAlert("エラー", "英語表記を入力してください。");
            return;
        }
        if (raceNameField.getText().trim().isEmpty()) {
            showAlert("エラー", "種族を入力してください。");
            return;
        }

        // Playerオブジェクトを作成
        Player player = createPlayerFromInput();

        // キャラクターシートを表示して確認
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("確認");
        confirmDialog.setHeaderText("このキャラクターで開始しますか?");

        TextArea textArea = new TextArea(player.getCharacterSheet());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(20);

        confirmDialog.getDialogPane().setContent(textArea);
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response.getButtonData().isDefaultButton()) {
                // 保存
                if (saveCharacter(player)) {
                    showAlert("成功", "キャラクター「" + player.getName() + "」を作成しました。");
                    onComplete.run();
                } else {
                    showAlert("エラー", "キャラクターの保存に失敗しました。");
                }
            }
        });
    }

    /**
     * 入力からPlayerオブジェクトを作成
     */
    private Player createPlayerFromInput() {
        Player player = new Player();

        // 基本情報
        player.setName(nameField.getText().trim());
        player.setEnglishName(englishNameField.getText().trim());
        player.setRaceName(raceNameField.getText().trim());

        // 種族タイプ
        String raceTypeStr = raceTypeChoice.getValue();
        if (raceTypeStr.equals("標準")) {
            player.setRaceType(RaceType.STANDARD);
        } else if (raceTypeStr.equals("前衛")) {
            player.setRaceType(RaceType.FRONTLINE);
        } else if (raceTypeStr.equals("後衛")) {
            player.setRaceType(RaceType.BACKLINE);
        }

        // 性別
        String genderStr = genderChoice.getValue();
        if (genderStr.equals("男性")) {
            player.setGender(Player.Gender.MALE);
        } else if (genderStr.equals("女性")) {
            player.setGender(Player.Gender.FEMALE);
        } else {
            player.setGender(Player.Gender.OTHER);
        }

        // 性自認
        player.setGenderIdentity(genderIdentityField.getText().trim());

        // 職業
        String jobStr = jobChoice.getValue();
        player.setJob(jobStr);
        applyJobSkills(player, jobStr);

        // 背景（TextFieldから取得）
        String backgroundStr = backgroundField.getText().trim();
        player.setBackground(backgroundStr);

        // 星座
        String constellationStr = constellationChoice.getValue();
        player.setConstellation(constellationStr);
        // 星座ボーナスを適用（HP/AP/お金などを変更）
        applyConstellationBonus(player, constellationStr);

        // ハサミ座の場合は追加技能を付与
        if ("ハサミ座".equals(constellationStr)) {
            String skill1 = bonusSkill1Choice.getValue();
            String skill2 = bonusSkill2Choice.getValue();
            if (skill1 != null && !"追加技能を選択".equals(skill1))
                player.addBaseSkill(skill1);
            if (skill2 != null && !"追加技能を選択".equals(skill2))
                player.addBaseSkill(skill2);
        }

        // 性格
        String personalityStr = personalityChoice.getValue();
        Map<Integer, Personality> personalities = personalityManager.getPersonalityChoices();
        for (var entry : personalities.entrySet()) {
            if (entry.getValue().getName().equals(personalityStr)) {
                player.setPersonality(entry.getValue());
                break;
            }
        }

        // 外見
        player.setBodyType(bodyTypeField.getText().trim());
        player.setClothing(clothingField.getText().trim());

        // チャームポイント
        if (!charmPoint1Field.getText().trim().isEmpty()) {
            player.addCharmPoint(charmPoint1Field.getText().trim());
        }
        if (!charmPoint2Field.getText().trim().isEmpty()) {
            player.addCharmPoint(charmPoint2Field.getText().trim());
        }
        if (!charmPoint3Field.getText().trim().isEmpty()) {
            player.addCharmPoint(charmPoint3Field.getText().trim());
        }

        // その他
        player.setCruelWorldEnabled(cruelWorldCheck.isSelected());
        player.setFatedOne(fatedOneCheck.isSelected());

        // 立ち絵情報の保存
        String selectedPortrait = portraitChoice.getValue();
        if (selectedPortrait != null && !selectedPortrait.equals("立ち絵を選択")) {
            if (usingCustomPortrait) {
                // カスタム立ち絵にはプレフィックスを付ける
                player.setPortraitFileName(ImageManager.addCustomPrefix(selectedPortrait));
            } else {
                player.setPortraitFileName(selectedPortrait);
            }
        }

        // 初期所持金
        player.setMoney(30);
        player.setMaxMoney(100);

        return player;
    }

    /**
     * 職業に応じたスキルを付与
     */
    private void applyJobSkills(Player player, String job) {
        switch (job) {
            case "戦士":
                player.addBaseSkill("筋力");
                player.addBaseSkill("耐久力");
                player.addBaseSkill("運動");
                player.addBaseSkill("軽業");
                player.addBaseSkill("自然の知識");
                player.addBaseSkill("料理");
                player.addItem("steel_axe");
                break;
            case "魔法使い":
                player.addBaseSkill("知力");
                player.addBaseSkill("耐久力");
                player.addBaseSkill("魔法の知識");
                player.addBaseSkill("古代の知識");
                player.addBaseSkill("解錠術");
                player.addBaseSkill("機巧");
                player.addItem("magic_staff");
                break;
            case "クレリック":
                player.addBaseSkill("判断力");
                player.addBaseSkill("耐久力");
                player.addBaseSkill("古代の知識");
                player.addBaseSkill("料理");
                player.addBaseSkill("薬識");
                player.addBaseSkill("話術");
                player.addItem("holy_silver_scissors");
                break;
            case "レンジャー":
                player.addBaseSkill("敏捷力");
                player.addBaseSkill("判断力");
                player.addBaseSkill("隠密");
                player.addBaseSkill("自然の知識");
                player.addBaseSkill("料理");
                player.addBaseSkill("薬識");
                player.addItem("hunter_bow");
                break;
            case "盗賊":
                player.addBaseSkill("敏捷力");
                player.addBaseSkill("知力");
                player.addBaseSkill("軽業");
                player.addBaseSkill("隠密");
                player.addBaseSkill("解錠術");
                player.addBaseSkill("機巧");
                player.addItem("thieves_tools");
                break;
            case "商人":
                player.addBaseSkill("判断力");
                player.addBaseSkill("魅力");
                player.addBaseSkill("魔法の知識");
                player.addBaseSkill("解錠術");
                player.addBaseSkill("話術");
                player.addBaseSkill("経世");
                player.addItem("merchant_ledger");
                break;
            case "踊り子":
                player.addBaseSkill("敏捷力");
                player.addBaseSkill("魅力");
                player.addBaseSkill("軽業");
                player.addBaseSkill("隠密");
                player.addBaseSkill("自然の知識");
                player.addBaseSkill("話術");
                player.addItem("hand_mirror");
                break;
            case "魔闘士":
                player.addBaseSkill("筋力");
                player.addBaseSkill("知力");
                player.addBaseSkill("運動");
                player.addBaseSkill("魔法の知識");
                player.addBaseSkill("経世");
                player.addBaseSkill("機巧");
                player.addItem("treasure");
                break;
            case "パラディン":
                player.addBaseSkill("筋力");
                player.addBaseSkill("魅力");
                player.addBaseSkill("運動");
                player.addBaseSkill("古代の知識");
                player.addBaseSkill("薬識");
                player.addBaseSkill("経世");
                player.addItem("holy_silver_shovel");
                break;
            case "観光客":
                // スキルなし
                break;
            case "異世界転生者":
                // 全技能を付与（チート）
                player.addBaseSkill("筋力");
                player.addBaseSkill("敏捷力");
                player.addBaseSkill("耐久力");
                player.addBaseSkill("知力");
                player.addBaseSkill("判断力");
                player.addBaseSkill("魅力");
                player.addBaseSkill("運動");
                player.addBaseSkill("軽業");
                player.addBaseSkill("隠密");
                player.addBaseSkill("解錠術");
                player.addBaseSkill("自然の知識");
                player.addBaseSkill("料理");
                player.addBaseSkill("薬識");
                player.addBaseSkill("機巧");
                player.addBaseSkill("魔法の知識");
                player.addBaseSkill("古代の知識");
                player.addBaseSkill("経世");
                player.addBaseSkill("話術");
                player.addItem("smartphone");
                break;
        }
    }

    /**
     * 職業に応じた詳細説明を取得
     */
    private String getJobDescription(String job) {
        // 共通ヘッダー
        String header = "プレイヤーキャラクターの職業を設定してください。\n"
                + "職業を選択すると、詳細な説明と付与される技能が表示されます。\n\n";

        switch (job) {
            case "戦士":
                return header + "・戦士\n"
                        + "戦士とは、武器を扱い、身を守り、己の為したいことのための技術と鍛錬を生活の中心に据えた者の総称です。\n"
                        + "特定の戦い方に限定されず、剣、斧、槍、盾、素手など、環境や文化に応じた戦技を身につけています。\n"
                        + "また、サバイバル能力も高く、独力で生きる術も持っています。\n\n"
                        + "技能：【筋力】【耐久力】【運動】【軽業】【自然の知識】【料理】";

            case "魔法使い":
                return header + "・魔法使い\n"
                        + "魔法使いとは、知識と理性を基礎とし、世界の成り立ちや法則を理解することで異常現象を引き起こす術を身につけた者の総称です。\n"
                        + "魔法は天賦ではなく学びにより得られるものであり、長年の読書、研究、記述、実験を通して身につく知恵の結晶です。\n"
                        + "古代文明の文献・魔道器具・失われた言語への造詣も深く、危険な研究領域への探求心が彼らを旅へと促すことがあります。\n"
                        + "魔術と機械の接合など「世界の仕組み」が興味の対象で、未知への知的好奇心こそが魔法使いを魔法使いたらしめています。\n\n"
                        + "技能：【知力】【耐久力】【魔法の知識】【古代の知識】【解錠術】【機巧】";

            case "クレリック":
                return header + "・クレリック\n"
                        + "クレリックとは、慈愛の女神「庭師」への信仰を持ち、傷ついた者を癒すことを使命とする聖職者です。\n"
                        + "古の儀式や教えに通じ、薬草学や食事を通じた奉仕で人々の心身を支えます。\n"
                        + "穏やかな言葉で人の心に寄り添い、控えめながらも確かな信念を胸に旅を続けます。\n"
                        + "彼らは正義を振りかざすことなく、ただ静かに手を差し伸べる存在です。\n\n"
                        + "技能：【判断力】【耐久力】【古代の知識】【料理】【薬識】【話術】";

            case "レンジャー":
                return header + "・レンジャー\n"
                        + "レンジャーとは、自然に深く根ざし、その流れの中で生きる術を習得した者を指します。\n"
                        + "森、山岳、沼地、荒野、寒冷地など、人が寄り付かぬ環境においても適応し、土地そのものが持つリズムを読み取ることで、生き物の気配や天候の兆し、危険の予兆を察知します。\n"
                        + "彼らは文明と自然の境界線を行き交う者であり、狩猟、追跡、探索、野営、薬草の採取と調合など、人が「野で生きる」ために必要な知恵を幅広く持っています。\n"
                        + "目的は護衛かもしれず、案内役かもしれず、あるいは単に自然との静かな共存を望むだけかもしれません。\n\n"
                        + "技能:【敏捷力】【判断力】【隠密】【自然の知識】【料理】【薬識】";

            case "盗賊":
                return header + "・盗賊\n"
                        + "盗賊とは、身軽な動きと鋭い頭脳を用いて、閉ざされた場所・秘匿された情報・仕掛けられた罠を解き明かすことを生業とする者です。\n"
                        + "ただ物を盗むのではなく、遺跡の謎、古代文明の仕組み、人々が忘れた技術や宝物に価値を見出し、それらを取り戻すことに主眼を置くトレジャーハンターとして認識されています。\n"
                        + "機械仕掛けの理解や罠の構造分析、精密な手仕事に長けており、危険地帯では仲間よりも先に障害を見抜き、解決へ導く役割を担います。\n"
                        + "公的には「遺物回収の専門職」として扱われ、犯罪者たる山賊とは明確に区別されます。\n\n"
                        + "技能:【敏捷力】【知力】【軽業】【隠密】【解錠術】【機巧】";

            case "商人":
                return header + "・商人\n"
                        + "商人とは、物の価値を見抜き、動かし、人と物資の流れを作る者の総称です。\n"
                        + "商品そのものより「市場」や「需要」「人の心理」を読み取り、より良い取引機会を求めて各地を渡り歩きます。\n"
                        + "言葉巧みな交渉術と人を惹きつける魅力で取引を有利に運び、魔道具の鑑定や錠前の仕組みにも通じています。\n"
                        + "買う・売るという行為は小さな表層にすぎず、商人の本質は「価値の流動を読み取る目」にあります。\n\n"
                        + "技能：【判断力】【魅力】【魔法の知識】【解錠術】【話術】【経世】";

            case "踊り子":
                return header + "・踊り子\n"
                        + "踊り子とは、舞踏と身のこなしで人々を魅了する流浪の芸人です。\n"
                        + "しなやかな身体と鋭い感覚を持ち、野外での生活にも順応しています。\n"
                        + "人の目を引きつけ、場の空気を操る術に長け、時にその技は身を隠すことにも役立ちます。\n"
                        + "旅路で培った話術と自然の知恵が、彼らの生きる糧となっています。\n\n"
                        + "技能：【敏捷力】【魅力】【軽業】【隠密】【自然の知識】【話術】";

            case "魔闘士":
                return header + "・魔闘士\n"
                        + "魔闘士とは、剣と魔法を共に扱う実戦派の兵士です。\n"
                        + "学者的な魔法使いは魔法を研究対象として見る一方、彼らは魔法を便利な道具として実用する現実主義者です。\n"
                        + "鍛え上げた肉体と明晰な頭脳を併せ持ち、戦場でも社会でも器用に立ち回ります。\n"
                        + "機械仕掛けや魔道具の実用的な活用にも長け、その財力と能力から一種のエリート層として扱われます。\n\n"
                        + "技能：【筋力】【知力】【運動】【魔法の知識】【経世】【機巧】";

            case "パラディン":
                return header + "・パラディン\n"
                        + "パラディンとは、三神への信仰を広めんと旅する武装した聖職者です。\n"
                        + "主神のシンボルである聖なるシャベルを携え、信仰と力をもって人々を導きます。\n"
                        + "鍛え上げた肉体と人を惹きつける存在感で教えを体現し、傷ついた者には薬草の知識で手を差し伸べます。\n"
                        + "布教のための資金や物資の調達にも長け、信仰の旅路を自ら切り拓く存在です。\n\n"
                        + "技能：【筋力】【魅力】【運動】【古代の知識】【薬識】【経世】";

            case "観光客":
                return header + "・観光客\n"
                        + "観光客とは、旅の専門家ではなく、戦士でも商人でも魔法使いでもない「ただ見たいものを見に来た人」の総称です。\n"
                        + "人生の区切り、衝動、好奇心、偶然、逃避、理由は人それぞれですが、特別な訓練を積んだわけでもなく、危険地帯に踏み込む準備すら十分ではありません。\n"
                        + "しかし同時に、他職にはない\"自由さ\"を持ち、何にも縛られず、目的すら曖昧なまま世界を歩く独特の魅力があります。\n"
                        + "彼らは時として他者には見えない価値や風景を拾い上げ、旅そのものの純粋さを象徴する存在にもなり得ます。\n"
                        + "しかし人生はハードモードだ！\n\n"
                        + "技能：なし";
            case "異世界転生者":
                return header + "・異世界転生者\n"
                        + "異世界から転生してきた者。なぜかあらゆる技能に長けており、チート能力を持つ。\n"
                        + "しかし、元の世界に戻る方法は不明で、この世界で生きていくしかない。\n"
                        + "スマホを持っているが、電波は通じない。\n\n"
                        + "技能：全技能\n"
                        + "（必要があれば、背景はリロールするか、自分で書き足す事をお勧めします。）";

            default:
                return HELP_JOB;
        }
    }

    /**
     * 星座に応じたボーナスを付与
     */
    private void applyConstellationBonus(Player player, String constellation) {
        switch (constellation) {
            case "親方座":
                player.setMaxHp(player.getMaxHp() + 25);
                player.setHp(player.getMaxHp());
                break;
            case "学者座":
                player.setMaxAp(player.getMaxAp() + 5);
                player.setAp(player.getMaxAp());
                break;
            case "庭師座":
                player.modifyMoney(20);
                break;
            case "ハサミ座":
                // 技能付与はcreatePlayerFromInputで行う
                break;
            case "見習い座":
                // ボーナスなし
                break;
        }
    }

    /**
     * 背景に応じたスキルを付与（旧システム、使用していない）
     * 
     * @deprecated 背景はTextField化されたため、もう使わない
     */
    @Deprecated
    private void applyBackgroundSkills(Player player, String background) {
        // 背景は自由記述になったので、このメソッドは使われない
        // レガシーコードとして残す
    }

    /**
     * キャラクターをJSONファイルとして保存
     */
    private boolean saveCharacter(Player player) {
        try {
            // 保存ディレクトリの作成
            File saveDir = new File("userdata/character");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            // ファイル名を生成
            String fileName = player.getEnglishName() + ".json";
            File saveFile = new File(saveDir, fileName);

            // JSONに変換して保存
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(player);

            try (FileWriter writer = new FileWriter(saveFile)) {
                writer.write(json);
            }

            // System.out.println("[CharacterCreationScreen] キャラクターを保存しました: " +
            // saveFile.getPath());
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ボタンのスタイル設定
     */
    private void styleButton(Button button) {
        button.setStyle("-fx-background-color: #e6e6e6; " + "-fx-text-fill: #333333; "
                + "-fx-border-color: #999999; " + "-fx-border-width: 2px; " + "-fx-font-size: 16px;");

        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #e0e0e0; " + "-fx-text-fill: #333333; "
                + "-fx-border-color: #666666; " + "-fx-border-width: 2px; " + "-fx-font-size: 16px;"));

        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #e6e6e6; " + "-fx-text-fill: #333333; "
                + "-fx-border-color: #999999; " + "-fx-border-width: 2px; " + "-fx-font-size: 16px;"));
    }

    /**
     * アラートを表示
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 追加技能選択UIの表示/非表示を切り替え
     */
    private void setVisibleBonusSkills(boolean visible) {
        if (bonusSkillBox != null) {
            bonusSkillBox.setVisible(visible);
            bonusSkillBox.setManaged(visible);
        }
    }
}