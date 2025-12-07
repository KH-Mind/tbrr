package com.kh.tbrr.system;

import java.util.Map;
import java.util.Random;

import com.kh.tbrr.data.models.Personality;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.data.models.Player.RaceType;
import com.kh.tbrr.manager.PersonalityManager;
import com.kh.tbrr.ui.ConsoleUI;

/**
 * @deprecated GUI版(CharacterCreationScreen)を使用してください。
 *             CUI版は開発初期のテスト用として残しています。
 */
@Deprecated
public class CharacterCreator {
    private final ConsoleUI ui;
    private final Random random;
    private final PersonalityManager personalityManager;

    public CharacterCreator(ConsoleUI ui, PersonalityManager personalityManager) {
        this.ui = ui;
        this.random = new Random();
        this.personalityManager = personalityManager;
    }

    public Player createCharacter() {
        Player player = new Player();

        ui.printSeparator();
        ui.print("       キャラクター作成");
        ui.printSeparator();

        selectName(player);
        selectEnglishName(player);
        selectRaceName(player);
        selectRaceType(player);
        selectGender(player);
        selectGenderIdentity(player);
        selectJob(player);
        selectBackground(player);
        selectPersonality(player);
        selectBodyType(player);
        selectClothing(player);
        selectCharmPoints(player);
        selectCruelWorld(player);
        selectFatedOne(player);

        ui.printSeparator();
        ui.print(player.getCharacterSheet());
        ui.print("このキャラクターで開始しますか? (y/n): ");
        String confirm = ui.getInput().trim().toLowerCase();
        if ("y".equals(confirm)) {
            ui.print("キャラクター作成完了");
            return player;
        } else {
            ui.print("作り直します");
            return createCharacter();
        }
    }

    private void selectName(Player player) {
        ui.print("名前を入力してください: ");
        String name = ui.getInput().trim();
        if (name.isEmpty()) {
            name = "冒険者";
        }
        player.setName(name);
    }

    private void selectEnglishName(Player player) {
        ui.print("英語表記（ファイル名に使います。\n例：もし名前がアリスであれば Alice 、関羽であれば Guan_Yu と入力するなどしてください。: ");
        String en = ui.getInput().trim();
        while (en.isEmpty()) {
            ui.print("英語表記は必須です。英語表記を入力してください: ");
            en = ui.getInput().trim();
        }
        player.setEnglishName(en);
    }

    private void selectRaceName(Player player) {
        ui.print("種族名を入力してください。Enterでデフォルト：人間 です。 ");
        String raceName = ui.getInput().trim();
        if (raceName.isEmpty()) {
            raceName = "人間";
        }
        player.setRaceName(raceName);
    }

    private void selectRaceType(Player player) {
        ui.print("\n種族タイプを選択してください:");
        ui.print("1. 標準 (HP:100, AP:20)");
        ui.print("2. 前衛 (HP:125, AP:15)");
        ui.print("3. 後衛 (HP:75, AP:25)");
        ui.print("選択 (1-3): ");
        int choice = getIntInput(1, 3, 1);
        switch (choice) {
            case 1:
                player.setRaceType(RaceType.STANDARD);
                break;
            case 2:
                player.setRaceType(RaceType.FRONTLINE);
                break;
            case 3:
                player.setRaceType(RaceType.BACKLINE);
                break;
            default:
                player.setRaceType(RaceType.STANDARD);
                break;
        }
        // 初期の所持金を弄るならココ！
        player.setMoney(30);
        player.setMaxMoney(100);
    }

    private void selectGender(Player player) {
        ui.print("\n性別を選択してください:");
        ui.print("1. 男性");
        ui.print("2. 女性");
        ui.print("3. その他");
        ui.print("選択 (1-3, Enter で 2): ");
        int choice = getIntInput(1, 3, 2);
        switch (choice) {
            case 1:
                player.setGender(Player.Gender.MALE);
                break;
            case 2:
                player.setGender(Player.Gender.FEMALE);
                break;
            case 3:
                player.setGender(Player.Gender.OTHER);
                break;
            default:
                player.setGender(Player.Gender.FEMALE);
                break;
        }
    }

    private void selectGenderIdentity(Player player) {
        ui.print("\n性自認を入力してください。");
        ui.print("どのような性別として周りから扱われるかを記載してください。自由入力です。");
        ui.print("入力 (Enterで デフォルト: 女性): ");
        String genderIdentityInput = ui.getInput().trim();
        if (genderIdentityInput.isEmpty()) {
            genderIdentityInput = "女性";
        }
        player.setGenderIdentity(genderIdentityInput);
    }

    private void selectJob(Player player) {
        ui.print("\n職業を選択してください:");
        ui.print("1. 戦士（スキル：筋力、耐久力、運動、軽業、自然の知識、料理）");
        ui.print("2. 魔法使い（スキル：知力、耐久力、魔法の知識、古代の知識、薬識、機巧）");
        ui.print("3. レンジャー（スキル：敏捷力、判断力、隠密、自然の知識、料理、薬識）");
        ui.print("4. 盗賊（スキル：敏捷力、知力、軽業、隠密、解錠術、機巧）");
        ui.print("5. 商人（スキル：判断力、魅力、話術、経世、解錠術、魔法の知識）");
        ui.print("6. 旅芸人（スキル：筋力、魅力、運動、古代の知識、経世、話術）");
        ui.print("7. 観光客（技能なし、ハードモード）");
        ui.print("選択 (1-7): ");
        int choice = getIntInput(1, 7, 1);
        switch (choice) {
            case 1:
                player.setJob("戦士");
                player.addBaseSkill("筋力");
                player.addBaseSkill("耐久力");
                player.addBaseSkill("運動");
                player.addBaseSkill("軽業");
                player.addBaseSkill("自然の知識");
                player.addBaseSkill("料理");
                player.addItem("steel_axe");
                break;
            case 2:
                player.setJob("魔法使い");
                player.addBaseSkill("知力");
                player.addBaseSkill("耐久力");
                player.addBaseSkill("魔法の知識");
                player.addBaseSkill("古代の知識");
                player.addBaseSkill("薬識");
                player.addBaseSkill("機巧");
                player.addItem("magic_staff");
                break;
            case 3:
                player.setJob("レンジャー");
                player.addBaseSkill("敏捷力");
                player.addBaseSkill("判断力");
                player.addBaseSkill("隠密");
                player.addBaseSkill("自然の知識");
                player.addBaseSkill("料理");
                player.addBaseSkill("薬識");
                player.addItem("hunter_bow");
                break;
            case 4:
                player.setJob("盗賊");
                player.addBaseSkill("敏捷力");
                player.addBaseSkill("知力");
                player.addBaseSkill("軽業");
                player.addBaseSkill("隠密");
                player.addBaseSkill("解錠術");
                player.addBaseSkill("機巧");
                player.addItem("thieves_tools");
                break;
            case 5:
                player.setJob("商人");
                player.addBaseSkill("判断力");
                player.addBaseSkill("魅力");
                player.addBaseSkill("話術");
                player.addBaseSkill("経世");
                player.addBaseSkill("解錠術");
                player.addBaseSkill("魔法の知識");
                player.addItem("merchant_ledger");
                break;
            case 6:
                player.setJob("旅芸人");
                player.addBaseSkill("筋力");
                player.addBaseSkill("魅力");
                player.addBaseSkill("運動");
                player.addBaseSkill("古代の知識");
                player.addBaseSkill("経世");
                player.addBaseSkill("話術");
                player.addItem("performance_kit");
                break;
            case 7:
                player.setJob("観光客");
                // 技能なし
                break;
            default:
                player.setJob("戦士");
                player.addBaseSkill("筋力");
                player.addBaseSkill("耐久力");
                player.addBaseSkill("運動");
                player.addBaseSkill("軽業");
                player.addBaseSkill("自然の知識");
                player.addBaseSkill("料理");
                player.addItem("steel_axe");
                break;
        }
    }

    private void selectBackground(Player player) {
        ui.print("\n背景を選択してください (Enter デフォルト 冒険者):");
        ui.print("1. 冒険者 (スキル: 筋力)");
        ui.print("2. 村の勇者 (スキル: 魅力)");
        ui.print("3. 賢者の弟子 (スキル: 古代の知識)");
        ui.print("4. 路地育ち (スキル: 隠密)");
        ui.print("5. 異世界転生者（すべての補助技能を持つ、イージーモード）");
        ui.print("6. ろくでなし（技能なし、ハードモード）");
        ui.print("選択 (1-6, Enter 1): ");
        int c = getIntInput(1, 6, 1);

        switch (c) {
            case 1:
                player.setBackground("冒険者");
                player.addBaseSkill("筋力");
                break;
            case 2:
                player.setBackground("村の勇者");
                player.addBaseSkill("魅力");
                break;
            case 3:
                player.setBackground("賢者の弟子");
                player.addBaseSkill("古代の知識");
                break;
            case 4:
                player.setBackground("路地育ち");
                player.addBaseSkill("隠密");
                break;
            case 5:
                player.setBackground("異世界転生者");
                player.addBaseSkill("軽業");
                player.addBaseSkill("隠密");
                player.addBaseSkill("自然の知識");
                player.addBaseSkill("話術");
                player.addBaseSkill("解錠術");
                player.addBaseSkill("料理");
                player.addBaseSkill("経世");
                player.addBaseSkill("薬識");
                player.addBaseSkill("機巧");
                break;
            case 6:
                player.setBackground("ろくでなし");
                // 技能なし
                break;
            default:
                player.setBackground("冒険者");
                player.addBaseSkill("筋力");
                break;
        }
    }

    private void selectPersonality(Player player) {
        ui.print("\n性格を選択してください:");
        Map<Integer, Personality> choices = personalityManager.getPersonalityChoices();
        for (var e : choices.entrySet()) {
            ui.print(e.getKey() + ". " + e.getValue().getName() + " - " + e.getValue().getDescription());
        }
        ui.print("選択 (1-" + choices.size() + "): ");
        int choice = getIntInput(1, choices.size(), 1);
        Personality sel = choices.get(choice);
        if (sel != null) {
            player.setPersonality(sel);
        } else {
            player.setPersonality(personalityManager.getDefaultPersonality());
        }
    }

    private void selectBodyType(Player player) {
        ui.print("\n体型を入力してください、「～身体」と表現される事があります。 \n(例: 華奢な, 細身な, 小柄な, 豊満な, 逞しい, 引き締まった) Enter で 華奢: ");
        String s = ui.getInput().trim();
        if (s.isEmpty()) {
            s = "華奢な";
        }
        player.setBodyType(s);
    }

    private void selectClothing(Player player) {
        ui.print("服装を入力してください。 Enter で 旅人の服: ");
        String s = ui.getInput().trim();
        if (s.isEmpty()) {
            s = "旅人の服";
        }
        player.setClothing(s);
    }

    private void selectCharmPoints(Player player) {
        ui.print("\nチャームポイントを1つ入力してください、自由入力です。:");

        // カテゴリ1: 髪の色・髪型
        String[] hairOptions = {
                "黒髪", "金髪", "銀髪", "赤毛", "青髪", "緑髪", "紫髪", "ピンク髪",
                "ショートヘア", "ロングヘアー", "癖毛", "アップスタイル", "ポニーテール", "オールバック", "モヒカンヘアー",
                "むぞうさヘアー", "ぼさぼさ", "ポニーテール", "三つ編み", "ゆるふわウェーブ", "ボブカット", "ツインドリル"
        };

        // カテゴリ2: 身体的特徴
        String[] physicalOptions = {
                "ほくろ", "八重歯", "色白", "色黒", "日焼け肌", "日焼け跡", "火傷跡", "剛毛", "困り眉", "太眉",
                "甘い声", "声が低い", "歯が白い", "透き通る肌", "すべすべ肌", "澄んだ声",
                "長い指", "垂れ目", "吊り目", "威圧の眼光", "金色の瞳", "琥珀色の瞳", "ヘテロクロミア", "猫目",
                "魅惑の唇", "いい匂い", "そばかす", "長いまつげ", "おめめぱっちり", "泣きぼくろ", "艶ほくろ"
        };

        // カテゴリ3: 雰囲気・キャラクター性
        String[] auraOptions = {
                "気品", "高貴", "だらしない", "豪傑", "セクシー", "野性的", "温和", "慈悲深い", "無慈悲", "母性的",
                "眠たげ", "コケティッシュ", "ギャル", "すこやか", "しなやか", "元気", "多才", "感受性豊か",
                "妖艶", "可憐", "華麗", "メガネ", "ボーイッシュ", "ダウナー", "儚げ", "快活", "無邪気", "したたか", "不愛想",
                "お金持ち", "貧乏", "実家が太い", "生徒会長", "経営者", "不思議ちゃん", "ずぼら", "厭世的", "学者肌",
        };

        // チャームポイント1（髪）
        ui.print("チャームポイント1 (Enter でランダムな髪の色・髪型): ");
        String c1 = ui.getInput().trim();
        if (c1.isEmpty()) {
            c1 = hairOptions[random.nextInt(hairOptions.length)];
        }
        player.addCharmPoint(c1);

        // チャームポイント2（身体的特徴）
        ui.print("チャームポイント2 (Enter でランダムな身体的特徴): ");
        String c2 = ui.getInput().trim();
        if (c2.isEmpty()) {
            c2 = physicalOptions[random.nextInt(physicalOptions.length)];
        }
        player.addCharmPoint(c2);

        // チャームポイント3（雰囲気・キャラクター性）
        ui.print("チャームポイント3 (Enter でランダムな雰囲気・キャラクター性): ");
        String c3 = ui.getInput().trim();
        if (c3.isEmpty()) {
            c3 = auraOptions[random.nextInt(auraOptions.length)];
        }
        player.addCharmPoint(c3);
    }

    private void selectCruelWorld(Player player) {
        ui.print("\nあなたを取り巻く世界は残酷だ。");
        ui.print("難易度に影響しません。デフォルトでオフ。詳細についてはreadme.txtをお読みください。");
        ui.print("入力（Enterで無効）: ");
        String input = ui.getInput().trim();
        if ("Cruel World".equalsIgnoreCase(input)) {
            player.setCruelWorldEnabled(true);
            ui.print("残酷な世界モードが有効になりました。");
        } else {
            player.setCruelWorldEnabled(false);
            ui.print("通常の世界で開始します。");
        }
    }

    private void selectFatedOne(Player player) {
        ui.print("\nあなたは運命に導かれし者だ。 ");
        ui.print("はい (デフォルト) / いいえ \n現在は特に意味のない項目ですが、いいえを選択した場合、将来的にパーマデスありのモブ扱いになります。 ");
        ui.print("選択 (Enter デフォルト true): ");
        String in = ui.getInput().trim().toLowerCase();
        if ("false".equals(in) || "f".equals(in)) {
            player.setFatedOne(false);
            ui.print("パーマデスモード有効");
        } else {
            player.setFatedOne(true);
        }
    }

    private int getIntInput(int min, int max, int defaultValue) {
        String input = ui.getInput().trim();
        if (input.isEmpty()) {
            return defaultValue;
        }
        try {
            int v = Integer.parseInt(input);
            if (v >= min && v <= max) {
                return v;
            }
            ui.print("範囲外です。デフォルトを使用します: " + defaultValue);
            return defaultValue;
        } catch (NumberFormatException e) {
            ui.print("無効な入力。デフォルトを使用します: " + defaultValue);
            return defaultValue;
        }
    }
}