package com.kh.tbrr.data.models;

import java.util.ArrayList;
import java.util.List;

import com.kh.tbrr.data.ItemRegistry;

public class Player {
    // 基本 basic
    private String id;
    private String name;
    private String englishName; // キャラのファイル名として使う
    private String raceName; // プレイヤーに好きに記述してもらう
    private RaceType raceType; // このゲームの所謂「種族によるステータス差」は種族名ではなく種族タイプで決める
    private Gender gender; // 性別（三択：男性、女性、その他）
    private String genderIdentity; // 性自認（自由記載）

    // 職と背景 job/background
    // RPGにおけるキャラクターの職は「クラス」と表現する方が好みだが、「クラスクラス」という表現が面倒くさいのでjobとする。(ジョブクラスという表現も大概なんだが）
    private String job;
    private String background;
    private String constellation; // 星座（ボーナス選択用）

    // 性格/personality 別途性格から口上をインポートする
    private Personality personality;

    // 色々/details
    private String backstory;
    private String bodyType;
    private String clothing;
    private List<String> charmPoints;

    // スタッツ/stats
    private int hp;
    private int maxHp;
    private int ap;
    private int maxAp;
    private int money;
    private int maxMoney; // このゲームにおける所持金の最大値は100とする。
    private int attackPower;

    // 立ち絵情報 portrait info
    private String portraitId; // 立ち絵の接頭辞 (例: ranger01)
    private String portraitFileName; // 選択された完全なファイル名 (例: ranger01.base.jpg)

    // 追加されたエイリアスメソッド ← low_damageとか実装した際に色々弄ったから、ちょっと怪しいかも！
    public int getCurrentHP() {
        return getHp();
    }

    public int getMaxHP() {
        return getMaxHp();
    }

    public int getCurrentAP() {
        return getAp();
    }

    public int getMaxAP() {
        return getMaxAp();
    }

    // skill sources
    private List<String> baseSkills; // 職業・背景による恒久スキル

    // lists
    private List<String> skills;
    private List<String> traits;
    private List<String> inventory;

    // status effects (プレイヤー状態、状態異常系の管理用)
    private java.util.Map<String, Integer> statusEffects;

    // flags
    private boolean cruelWorldEnabled = false;
    private boolean isFatedOne;

    public enum RaceType {
        STANDARD("標準", 100, 20), FRONTLINE("前衛", 125, 15), BACKLINE("後衛", 75, 25);

        private final String displayName;
        private final int baseHp;
        private final int baseAp;

        RaceType(String displayName, int baseHp, int baseAp) {
            this.displayName = displayName;
            this.baseHp = baseHp;
            this.baseAp = baseAp;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getBaseHp() {
            return baseHp;
        }

        public int getBaseAp() {
            return baseAp;
        }
    }

    // 性別enum（三択：男性、女性、その他）
    public enum Gender {
        MALE("男性"), FEMALE("女性"), OTHER("その他");

        private final String displayName;

        Gender(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // プレイヤーキャラクターの初期値設定
    public Player() {
        this.baseSkills = new ArrayList<>();
        this.skills = new ArrayList<>();
        this.traits = new ArrayList<>();
        this.inventory = new ArrayList<>();
        this.charmPoints = new ArrayList<>();
        this.statusEffects = new java.util.HashMap<>();

        this.isFatedOne = true;
        this.attackPower = 10;
        this.money = 30;
        this.maxMoney = 100;
        this.gender = Gender.FEMALE; // デフォルト値
        this.genderIdentity = "女性"; // デフォルト値
    }

    // HP/AP/お金 の増減をやるやつ
    public void modifyHp(int amount) {
        this.hp += amount;
        if (this.hp < 0) {
            this.hp = 0;
        }
        if (this.hp > this.maxHp) {
            this.hp = this.maxHp;
        }
    }

    public void modifyAp(int amount) {
        this.ap += amount;
        if (this.ap < 0) {
            this.ap = 0;
        }
        if (this.ap > this.maxAp) {
            this.ap = this.maxAp;
        }
    }

    public void modifyMoney(int amount) {
        this.money += amount;
        if (this.money < 0) {
            this.money = 0;
        }
        if (this.money > this.maxMoney) {
            this.money = this.maxMoney;
        }
    }

    public String getStatusString() {
        return String.format("[%s] HP:%d/%d AP:%d/%d 銀貨:%d/%d",
                name != null ? name : "冒険者",
                hp, maxHp, ap, maxAp, money, maxMoney);
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    // items/skills/traits/charm
    public boolean hasItem(String itemId) {
        if (inventory.contains(itemId)) {
            return true;
        }

        // 職業アイテムによるみなし所持判定
        if (itemId.equals("crowbar") && inventory.contains("steel_axe")) {
            return true;
        }
        if (itemId.equals("crossbow") && inventory.contains("hunter_bow")) {
            return true;
        }
        if (itemId.equals("torch") && inventory.contains("magic_staff")) {
            return true;
        }
        if (itemId.equals("dagger") && inventory.contains("thieves_tools")) {
            return true;
        }
        if (itemId.equals("expedition_map") && inventory.contains("merchant_ledger")) {
            return true;
        }
        if (itemId.equals("pouch") && inventory.contains("performance_kit")) {
            return true;
        }

        return false;
    }

    public void addItem(String itemId) {
        if (!inventory.contains(itemId)) {
            inventory.add(itemId);
        }
    }

    public void removeItem(String itemId) {
        inventory.remove(itemId);
    }

    public boolean hasSkill(String skillName) {
        return getEffectiveSkills().contains(skillName);
    }

    public void addSkill(String skillName) {
        if (!skills.contains(skillName)) {
            skills.add(skillName);
        }
    }

    public boolean hasTrait(String traitName) {
        return traits.contains(traitName);
    }

    public void addTrait(String traitName) {
        if (!traits.contains(traitName)) {
            traits.add(traitName);
        }
    }

    public void addCharmPoint(String charmPoint) {
        if (charmPoints.size() < 3) {
            charmPoints.add(charmPoint);
        }
    }

    // ========== 状態異常管理 ==========

    /**
     * 指定された状態異常を持っているか
     */
    public boolean hasStatusEffect(String effectId) {
        return statusEffects.containsKey(effectId);
    }

    /**
     * 状態異常の値を取得（持っていない場合は0を返す）
     */
    public int getStatusEffectValue(String effectId) {
        return statusEffects.getOrDefault(effectId, 0);
    }

    /**
     * 状態異常を設定（値付き）
     */
    public void setStatusEffect(String effectId, int value) {
        com.kh.tbrr.data.models.StatusEffect effect = com.kh.tbrr.data.StatusEffectRegistry
                .getStatusEffectById(effectId);
        if (effect != null) {
            // 最小値・最大値でクランプ
            int clampedValue = Math.max(effect.getMinValue(), Math.min(value, effect.getMaxValue()));

            // 削除条件: 最小値未満、または（最小値でallowZeroがfalseの場合）
            boolean shouldRemove = clampedValue < effect.getMinValue()
                    || (clampedValue == effect.getMinValue() && !effect.isAllowZero());

            if (shouldRemove) {
                statusEffects.remove(effectId);
            } else {
                statusEffects.put(effectId, clampedValue);
            }
        }
    }

    /**
     * 状態異常を変更（増減）
     */
    public void modifyStatusEffect(String effectId, int amount) {
        int currentValue = getStatusEffectValue(effectId);
        setStatusEffect(effectId, currentValue + amount);
    }

    /**
     * 状態異常を変更（自動初期化付き）
     * 状態異常が未設定の場合、defaultValueで初期化してから変更する
     * defaultValueが定義されていない場合は通常のmodifyStatusEffectと同じ動作
     */
    public void modifyStatusEffectWithInit(String effectId, int amount) {
        // 既に設定されている場合は通常の処理
        if (hasStatusEffect(effectId)) {
            modifyStatusEffect(effectId, amount);
            return;
        }

        // 未設定の場合、StatusEffectRegistryから定義を取得
        com.kh.tbrr.data.models.StatusEffect effect = com.kh.tbrr.data.StatusEffectRegistry
                .getStatusEffectById(effectId);

        if (effect != null && effect.getDefaultValue() != null) {
            // defaultValueが定義されている場合: 初期値 + 変更量で設定
            int initialValue = effect.getDefaultValue() + amount;
            int clampedValue = Math.max(effect.getMinValue(),
                    Math.min(initialValue, effect.getMaxValue()));

            // allowZeroがtrueの場合は最小値でも設定、falseの場合は最小値より大きい場合のみ設定
            boolean shouldSet = effect.isAllowZero()
                    ? clampedValue >= effect.getMinValue()
                    : clampedValue > effect.getMinValue();

            if (shouldSet) {
                statusEffects.put(effectId, clampedValue);
            }
        } else {
            // defaultValueが未定義の場合: 通常の処理（0から開始）
            modifyStatusEffect(effectId, amount);
        }
    }

    /**
     * 状態異常を削除
     */
    public void removeStatusEffect(String effectId) {
        statusEffects.remove(effectId);
    }

    /**
     * 全ての状態異常を取得
     */
    public java.util.Map<String, Integer> getStatusEffects() {
        return new java.util.HashMap<>(statusEffects);
    }

    public List<String> getEffectiveSkills() {
        List<String> effective = new ArrayList<>(baseSkills); // 職業・背景スキル

        for (String itemId : inventory) {
            Item item = ItemRegistry.getItemById(itemId);
            if (item != null && item.getGrantedSkills() != null) {
                for (String skill : item.getGrantedSkills()) {
                    if (!effective.contains(skill)) {
                        effective.add(skill);
                    }
                }
            }
        }

        return effective;
    }

    // キャラクターシート
    public String getCharacterSheet() {
        StringBuilder sb = new StringBuilder();
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("           キャラクターシート\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("名前: ").append(name != null ? name : "未設定").append("\n");
        sb.append("英名: ").append(englishName != null ? englishName : "未設定").append("\n");
        sb.append("種族: ").append(raceName != null ? raceName : "未設定");
        sb.append(" (").append(raceType != null ? raceType.getDisplayName() : "未設定").append(")\n");
        sb.append("性別: ").append(gender != null ? gender.getDisplayName() : "未設定").append("\n");
        sb.append("自認: ").append(genderIdentity != null ? genderIdentity : "未設定").append("\n");

        sb.append("職業: ").append(job != null ? job : "未設定").append("\n");
        sb.append("背景: ").append(background != null ? background : "未設定").append("\n");
        sb.append("星座: ").append(constellation != null ? constellation : "未設定").append("\n");
        sb.append("性格: ").append(personality != null ? personality.getName() : "未設定").append("\n\n");

        sb.append("体型: ").append(bodyType != null ? bodyType : "未設定").append("\n");
        sb.append("服装: ").append(clothing != null ? clothing : "未設定").append("\n");
        sb.append("チャームポイント: ").append(charmPoints.isEmpty() ? "なし" : String.join(", ", charmPoints))
                .append("\n\n");

        sb.append("HP: ").append(hp).append("/").append(maxHp).append("\n");
        sb.append("AP: ").append(ap).append("/").append(maxAp).append("\n\n");
        sb.append("銀貨: ").append(money).append("/").append(maxMoney).append("\n");
        sb.append("攻撃力: ").append(attackPower).append("\n\n");

        // スキル表示（baseSkills + アイテム由来）
        List<String> effectiveSkills = getEffectiveSkills();
        sb.append("スキル: ").append(effectiveSkills.isEmpty() ? "なし" : String.join(", ", effectiveSkills)).append("\n");

        // 所持品表示（名前変換付き）
        if (inventory.isEmpty()) {
            sb.append("所持品: なし\n");
        } else {
            sb.append("所持品:\n");
            for (String itemId : inventory) {
                String name = ItemRegistry.getNameById(itemId);
                sb.append("  - ").append(name != null ? name : itemId).append("\n");
            }
        }

        // 状態異常表示
        if (!statusEffects.isEmpty()) {
            sb.append("状態異常:\n");
            for (java.util.Map.Entry<String, Integer> entry : statusEffects.entrySet()) {
                String effectId = entry.getKey();
                int value = entry.getValue();
                String effectName = com.kh.tbrr.data.StatusEffectRegistry.getNameById(effectId);
                sb.append("  - ").append(effectName != null ? effectName : effectId)
                        .append(": ").append(value).append("\n");
            }
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        return sb.toString();
    }

    // ゲッターとセッター
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
        this.id = englishName; // ← ここでidを初期化
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public RaceType getRaceType() {
        return raceType;
    }

    public void setRaceType(RaceType raceType) {
        this.raceType = raceType;
        this.maxHp = raceType.getBaseHp();
        this.hp = this.maxHp;
        this.maxAp = raceType.getBaseAp();
        this.ap = this.maxAp;
    }

    /**
     * 性別を取得（三択：男性、女性、その他）
     */
    public Gender getGender() {
        return gender;
    }

    /**
     * 性別を設定（三択：男性、女性、その他）
     */
    public void setGender(Gender gender) {
        this.gender = gender;
    }

    /**
     * 性自認を取得（自由記載）
     */
    public String getGenderIdentity() {
        return genderIdentity;
    }

    /**
     * 性自認を設定（自由記載）
     */
    public void setGenderIdentity(String genderIdentity) {
        this.genderIdentity = genderIdentity;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getConstellation() {
        return constellation;
    }

    public void setConstellation(String constellation) {
        this.constellation = constellation;
    }

    public Personality getPersonality() {
        return personality;
    }

    public void setPersonality(Personality personality) {
        this.personality = personality;
    }

    public String getBackstory() {
        return backstory;
    }

    public void setBackstory(String backstory) {
        this.backstory = backstory;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public String getClothing() {
        return clothing;
    }

    public void setClothing(String clothing) {
        this.clothing = clothing;
    }

    public List<String> getCharmPoints() {
        return charmPoints;
    }

    public void setCharmPoints(List<String> charmPoints) {
        this.charmPoints = charmPoints;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getAp() {
        return ap;
    }

    public void setAp(int ap) {
        this.ap = ap;
    }

    public int getMaxAp() {
        return maxAp;
    }

    public void setMaxAp(int maxAp) {
        this.maxAp = maxAp;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getMaxMoney() {
        return maxMoney;
    }

    public void setMaxMoney(int maxMoney) {
        this.maxMoney = maxMoney;
    }

    public int getAttackPower() {
        return attackPower;
    }

    public void setAttackPower(int attackPower) {
        this.attackPower = attackPower;
    }

    public List<String> getBaseSkills() {
        return baseSkills;
    }

    public void setBaseSkills(List<String> baseSkills) {
        this.baseSkills = baseSkills;
    }

    public void addBaseSkill(String skillName) {
        if (!baseSkills.contains(skillName)) {
            baseSkills.add(skillName);
        }
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getTraits() {
        return traits;
    }

    public void setTraits(List<String> traits) {
        this.traits = traits;
    }

    public List<String> getInventory() {
        return inventory;
    }

    public void setInventory(List<String> inventory) {
        this.inventory = inventory;
    }

    public boolean isCruelWorldEnabled() {
        return cruelWorldEnabled;
    }

    public void setCruelWorldEnabled(boolean enabled) {
        this.cruelWorldEnabled = enabled;
    }

    public boolean isFatedOne() {
        return isFatedOne;
    }

    public void setFatedOne(boolean fatedOne) {
        isFatedOne = fatedOne;
    }

    // 立ち絵関連のgetter/setter
    public String getPortraitId() {
        return portraitId;
    }

    public void setPortraitId(String portraitId) {
        this.portraitId = portraitId;
    }

    public String getPortraitFileName() {
        return portraitFileName;
    }

    public void setPortraitFileName(String portraitFileName) {
        this.portraitFileName = portraitFileName;

        // ファイル名から接頭辞を自動抽出
        if (portraitFileName != null && !portraitFileName.isEmpty()) {
            // "ranger01.base.jpg" -> "ranger01"
            int dotIndex = portraitFileName.indexOf(".base.");
            if (dotIndex > 0) {
                this.portraitId = portraitFileName.substring(0, dotIndex);
            }
        }
    }
}