package com.kh.tbrr.data.models;

import java.util.ArrayList;
import java.util.List;

import com.kh.tbrr.battle.data.TraitData;
import com.kh.tbrr.battle.data.TraitRegistry;
import com.kh.tbrr.data.ItemRegistry;
import com.kh.tbrr.data.SkillStatsMapper;
import com.kh.tbrr.data.SkillStatsMapper.CombatStats;

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

    // SP（シールドポイント）：戦闘専用の耐久バッファー
    /** SPの上限値（オーバーフロー防止のソフトキャップ） */
    public static final int SP_MAX = 9999;
    /** 現在のSP（戦闘開始時に初期化、戦闘終了時にリセット） */
    private int currentSp = 0;

    // 立ち絵情報 portrait info
    private String portraitId; // 立ち絵の接頭辞 (例: ranger01)
    private String portraitFileName; // 選択された完全なファイル名 (例: ranger01.base.jpg)

    // 追加されたエイリアスメソッド ← low_damageとか実装した際に色々弄ったから、ちょっと怪しいかも！
    public int getCurrentHP() {
        return getHp();
    }

    public int getMaxHP() {
        return getEffectiveMaxHp();
    }

    public int getCurrentAP() {
        return getAp();
    }

    public int getMaxAP() {
        return getEffectiveMaxAp();
    }

    // skill sources
    private List<String> baseSkills; // 職業・背景による恒久スキル

    // lists
    private List<String> skills;

    // アビリティ（技・魔法）の3分類
    private List<String> baseAbilities; // キャラ作成時から持つアビリティ（職業・初期ボーナス）
    private List<String> inheritedAbilities; // 過去の引継ぎで恒久化したアビリティ
    private List<String> abilities; // その周で得たアビリティ（一時取得）

    // passivesフィールドはtraitsに統合済み
    private List<String> stances; // 習得しているスタンス用リスト

    // 特徴/Traitの3分類
    private List<String> baseTraits; // キャラ作成時から持つ特徴（職業由来等）
    private List<String> inheritedTraits; // 過去の引継ぎで恒久化した特徴
    private List<String> traits; // その周で得た特徴（一時取得）

    private List<String> inventory;

    // equipments
    private String equippedMainWeapon; // Weapon item ID

    // accessories and reserve (Phase 3)
    private List<String> equippedAccessories; // max 3, Accessory item IDs
    private List<String> reserveEquipments;

    // status effects (プレイヤー状態、状態異常系の管理用)
    private java.util.Map<String, Integer> statusEffects;

    // flags
    private boolean cruelWorldEnabled = false;
    private boolean isFatedOne;

    // グレード: 死亡した回数（引継ぎ回数）。最大20。
    // ※このゲームのグレードは一般的なRPGの「レベル」とは異なり、死んだ回数を表す。
    private int grade = 0;

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
        this.baseAbilities = new ArrayList<>();
        this.inheritedAbilities = new ArrayList<>();
        this.abilities = new ArrayList<>();
        this.stances = new ArrayList<>();
        this.baseTraits = new ArrayList<>();
        this.inheritedTraits = new ArrayList<>();
        this.traits = new ArrayList<>();
        this.inventory = new ArrayList<>();
        this.charmPoints = new ArrayList<>();

        // Phase 3 additions
        this.equippedAccessories = new ArrayList<>();
        this.reserveEquipments = new ArrayList<>();
        this.statusEffects = new java.util.HashMap<>();

        this.isFatedOne = true;
        this.money = 30;
        this.maxMoney = 100;
        this.gender = Gender.FEMALE; // デフォルト値
        this.genderIdentity = "女性"; // デフォルト値
        this.grade = 0;
    }

    // ========== SP管理 ==========

    /**
     * 現在のSPを返す。
     */
    public int getCurrentSp() {
        return currentSp;
    }

    /**
     * 現在のSPをセットする（0〜SP_MAXでクランプ）。
     */
    public void setCurrentSp(int sp) {
        this.currentSp = Math.max(0, Math.min(SP_MAX, sp));
    }

    /**
     * SPを増減する（0〜SP_MAXでクランプ）。
     * 戦闘中にSPを回復するアビリティ等から呼び出す。
     */
    public void modifySp(int amount) {
        setCurrentSp(this.currentSp + amount);
    }

    /**
     * 戦闘開始時の初期SPを計算して返す。
     * 基本値20 + 特徴（Trait）の statBonuses["initial_sp"] を合算する。
     * 装備由来のTraitも含む（getEffectiveTraits() 経由）。
     */
    public int calcInitialSp() {
        int base = 20;
        int bonus = 0;
        for (String traitId : getEffectiveTraits()) {
            com.kh.tbrr.battle.data.TraitData td = com.kh.tbrr.battle.data.TraitRegistry.getTraitById(traitId);
            if (td != null && td.getStatBonuses() != null) {
                bonus += td.getStatBonuses().getOrDefault("initial_sp", 0);
            }
        }
        return Math.min(SP_MAX, base + bonus);
    }

    /**
     * 戦闘中の被ダメージ処理（SP → HP の順）。
     * <p>
     * isPenetrating=false（通常攻撃）の場合: まずSPでダメージを受け、0を下回った超過分がHPに通る。<br>
     * isPenetrating=true（将来の貫通攻撃）の場合: SPをバイパスしてダメージをそのままHPに適用する。
     * </p>
     * 
     * @param damage        受けるダメージ量（正の整数）
     * @param isPenetrating trueの場合はSPを無視してHPに直接ダメージ
     * @return 実際にHPに通ったダメージ量（ログ表示用）
     */
    public int applyBattleDamage(int damage, boolean isPenetrating) {
        if (isPenetrating || currentSp <= 0) {
            // 貫通ダメージ、またはSPが既に0の場合はHPに直接
            modifyHp(-damage);
            return damage;
        }
        // SPで先にダメージを受ける
        int spAbsorbed = Math.min(currentSp, damage);
        int overflow = damage - spAbsorbed;
        setCurrentSp(currentSp - spAbsorbed);
        if (overflow > 0) {
            modifyHp(-overflow);
        }
        return overflow; // HPに通ったダメージ量
    }

    // HP/AP/お金 の増減をやるやつ
    public void modifyHp(int amount) {
        this.hp += amount;
        if (this.hp < 0) {
            this.hp = 0;
        }
        if (this.hp > getEffectiveMaxHp()) {
            this.hp = getEffectiveMaxHp();
        }
    }

    public void modifyAp(int amount) {
        this.ap += amount;
        if (this.ap < 0) {
            this.ap = 0;
        }
        if (this.ap > getEffectiveMaxAp()) {
            this.ap = getEffectiveMaxAp();
        }
    }

    public void modifyMoney(int amount) {
        this.money += amount;
        if (this.money < 0) {
            this.money = 0;
        }
        if (this.money > getEffectiveMaxMoney()) {
            this.money = getEffectiveMaxMoney();
        }
    }

    // ゲーム開始時にバフ込みの最大値まで全回復する
    public void fullHeal() {
        this.hp = getEffectiveMaxHp();
        this.ap = getEffectiveMaxAp();
    }

    public String getStatusString() {
        return String.format("[%s] HP:%d/%d AP:%d/%d 銀貨:%d/%d",
                name != null ? name : "冒険者",
                hp, getEffectiveMaxHp(), ap, getEffectiveMaxAp(), money, getEffectiveMaxMoney());
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
        if (itemId.equals("liquor") && inventory.contains("favorite_liquor")) {
            return true;
        }
        if (itemId.equals("antidote") && inventory.contains("medicinal_herbs")) {
            return true;
        }
        if (itemId.equals("torch") && inventory.contains("infinite_light")) {
            return true;
        }
        if (itemId.equals("dagger") && inventory.contains("thieves_tools")) {
            return true;
        }
        if (itemId.equals("expedition_map") && inventory.contains("merchant_ledger")) {
            return true;
        }
        if (itemId.equals("silver_knife") && inventory.contains("holy_silver_scissors")) {
            return true;
        }
        if (itemId.equals("mirror") && inventory.contains("hand_mirror")) {
            return true;
        }
        if (itemId.equals("coin_pouch") && inventory.contains("treasure")) {
            return true;
        }
        if (itemId.equals("pickaxe") && inventory.contains("holy_silver_shovel")) {
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
        // インベントリから消えた場合は装備スロットからも外す
        if (itemId.equals(equippedMainWeapon)) {
            equippedMainWeapon = null;
        }
        if (equippedAccessories != null) {
            equippedAccessories.remove(itemId);
        }
    }

    /**
     * 所持している（あるいは装備している）武器と装飾品の合計数を取得。
     * 上限（計6つ）のチェックに使う。
     */
    public int getEquipmentCount() {
        int count = 0;
        for (String id : inventory) {
            Item item = ItemRegistry.getItemById(id);
            if (item != null) {
                String cat = item.getEquipmentCategory();
                if ("WEAPON".equalsIgnoreCase(cat) || "ACCESSORY".equalsIgnoreCase(cat)) {
                    count++;
                }
            }
        }
        return count;
    }

    // --- 装備操作メソッド群 ---

    public String getEquippedMainWeapon() {
        return equippedMainWeapon;
    }

    public void equipMainWeapon(String itemId) {
        if (itemId == null || inventory.contains(itemId)) {
            this.equippedMainWeapon = itemId;
        }
    }

    public List<String> getEquippedAccessories() {
        return equippedAccessories;
    }

    public void equipAccessory(String itemId) {
        if (itemId != null && inventory.contains(itemId) && !equippedAccessories.contains(itemId)) {
            if (equippedAccessories.size() < 3) {
                equippedAccessories.add(itemId);
            }
        }
    }

    public void unequipAccessory(String itemId) {
        if (itemId != null) {
            equippedAccessories.remove(itemId);
        }
    }

    public boolean hasSkill(String skillName) {
        return getEffectiveSkills().contains(skillName);
    }

    public void addSkill(String skillName) {
        if (!skills.contains(skillName)) {
            skills.add(skillName);
        }
    }

    public void removeSkill(String skillName) {
        skills.remove(skillName);
        baseSkills.remove(skillName);
    }

    public boolean hasTrait(String traitName) {
        return traits.contains(traitName);
    }

    public void addTrait(String traitName) {
        // 各Traitの効果上限はTrait自身の処理ロジック側で管理する（重複チェックなし）
        traits.add(traitName);
    }

    public void removeTrait(String traitName) {
        traits.remove(traitName);
    }

    public void removeAllTraits(String traitName) {
        traits.removeIf(t -> t.equals(traitName));
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
     * 有効な特徴（Trait）リストを取得する。
     * base(職業由来) + inherited(引継ぎ済み) + traits(その周) の3段階を合算し、
     * さらに現在装備中のアイテムが持つ grantedTraits を動的に合算する。
     * アイテムの装備を外せば効果も消える。
     */
    public List<String> getEffectiveTraits() {
        // nullチェック（Gsonで読み込み時にフィールドがない場合nullになる場合の対処）
        if (baseTraits == null)
            baseTraits = new ArrayList<>();
        if (inheritedTraits == null)
            inheritedTraits = new ArrayList<>();
        if (traits == null)
            traits = new ArrayList<>();

        // 3段階を順番に結合: 初期→引継ぎ済み→その周
        List<String> effective = new ArrayList<>();
        effective.addAll(baseTraits);
        effective.addAll(inheritedTraits);
        effective.addAll(traits);

        // メイン武器の grantedTraits
        if (equippedMainWeapon != null) {
            Item weapon = ItemRegistry.getItemById(equippedMainWeapon);
            if (weapon != null && weapon.getGrantedTraits() != null) {
                for (String traitId : weapon.getGrantedTraits()) {
                    effective.add(traitId);
                }
            }
        }

        // 装備中アクセサリの grantedTraits
        if (equippedAccessories != null) {
            for (String accId : equippedAccessories) {
                Item acc = ItemRegistry.getItemById(accId);
                if (acc != null && acc.getGrantedTraits() != null) {
                    for (String traitId : acc.getGrantedTraits()) {
                        if (!effective.contains(traitId))
                            effective.add(traitId);
                    }
                }
            }
        }

        // 特徴が付与する別の特徴を再帰的に展開する
        boolean addedNew = true;
        while (addedNew) {
            addedNew = false;
            List<String> currentSnapshot = new ArrayList<>(effective);
            for (String traitId : currentSnapshot) {
                TraitData td = TraitRegistry.getTraitById(traitId);
                if (td != null && td.getGrantedTraits() != null) {
                    for (String granted : td.getGrantedTraits()) {
                        if (!effective.contains(granted)) {
                            effective.add(granted);
                            addedNew = true;
                        }
                    }
                }
            }
        }

        return effective;
    }

    /**
     * 状態異常を削除
     */
    public void removeStatusEffect(String effectId) {
        statusEffects.remove(effectId);
    }

    /**
     * 全ての状態異常を一括クリアする。
     * 引継ぎ処理後の初期化などで使用する。
     */
    public void clearAllStatusEffects() {
        if (statusEffects != null) {
            statusEffects.clear();
        }
    }

    /**
     * 全ての状態異常を取得
     */
    public java.util.Map<String, Integer> getStatusEffects() {
        if (statusEffects == null)
            statusEffects = new java.util.HashMap<>();
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
     * 全アビリティリストを取得する。
     * base(職業由来) + inherited(引継ぎ済み) + abilities(その周) の3段階を合算し、
     * さらに有効な特徴(Trait)によって自動付与されるアビリティも含む。
     */
    public List<String> getEffectiveAbilities() {
        // nullチェック（Gsonで読み込み時にフィールドがない場合nullになる場合の対処）
        if (baseAbilities == null)
            baseAbilities = new ArrayList<>();
        if (inheritedAbilities == null)
            inheritedAbilities = new ArrayList<>();
        if (abilities == null)
            abilities = new ArrayList<>();

        // 3段階を順番に結合: 初期→引継ぎ済み→その周
        List<String> effAbilities = new ArrayList<>();
        effAbilities.addAll(baseAbilities);
        effAbilities.addAll(inheritedAbilities);
        effAbilities.addAll(abilities);

        // 有効な特徴から自動付与アビリティを追加
        List<String> effTraits = getEffectiveTraits();
        for (String traitId : effTraits) {
            TraitData td = TraitRegistry.getTraitById(traitId);
            if (td != null && td.getGrantedAbilities() != null) {
                for (String grantedAbility : td.getGrantedAbilities()) {
                    effAbilities.add(grantedAbility);
                }
            }
        }
        return effAbilities;
    }

    /**
     * 戦闘ステータスを計算して取得
     * 技能（baseSkills + アイテム由来）とアイテムのcombatStatsを合算
     * 
     * @return 戦闘ステータス（might, insight, finesse, presence, sensuality）
     */
    public CombatStats getCombatStats() {
        int totalMight = 0;
        int totalInsight = 0;
        int totalFinesse = 0;
        int totalPresence = 0;
        int totalSensuality = 0;

        // 技能からステータスを加算
        for (String skillName : getEffectiveSkills()) {
            CombatStats skillStats = SkillStatsMapper.getStats(skillName);
            totalMight += skillStats.might();
            totalInsight += skillStats.insight();
            totalFinesse += skillStats.finesse();
            totalPresence += skillStats.presence();
            totalSensuality += skillStats.sensuality();
        }

        // アイテムからステータスを加算
        for (String itemId : inventory) {
            Item item = ItemRegistry.getItemById(itemId);
            if (item != null) {
                totalMight += item.getCombatStat("might");
                totalInsight += item.getCombatStat("insight");
                totalFinesse += item.getCombatStat("finesse");
                totalPresence += item.getCombatStat("presence");
                totalSensuality += item.getCombatStat("sensuality");
            }
        }

        // 特徴(Trait)からステータスを加算（装備由来のTraitも含む）
        for (String traitId : getEffectiveTraits()) {
            com.kh.tbrr.battle.data.TraitData td = com.kh.tbrr.battle.data.TraitRegistry.getTraitById(traitId);
            if (td != null && td.getStatBonuses() != null) {
                totalMight += td.getStatBonuses().getOrDefault("might", 0);
                totalInsight += td.getStatBonuses().getOrDefault("insight", 0);
                totalFinesse += td.getStatBonuses().getOrDefault("finesse", 0);
                totalPresence += td.getStatBonuses().getOrDefault("presence", 0);
                totalSensuality += td.getStatBonuses().getOrDefault("sensuality", 0);
            }
        }

        return CombatStats.of(totalMight, totalInsight, totalFinesse, totalPresence, totalSensuality);
    }

    /**
     * 戦闘ステータスを文字列で取得（表示用）
     */
    public String getCombatStatsString() {
        CombatStats stats = getCombatStats();
        return String.format("強靭:%d 聡明:%d 機敏:%d 風格:%d",
                stats.might(), stats.insight(), stats.finesse(), stats.presence());
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
        sb.append("性格: ").append(personality != null ? personality.getName() : "未設定").append("\n");
        sb.append("グレード: ").append(grade).append(" / 20\n\n");

        sb.append("体型: ").append(bodyType != null ? bodyType : "未設定").append("\n");
        sb.append("服装: ").append(clothing != null ? clothing : "未設定").append("\n");
        sb.append("チャームポイント: ").append(charmPoints.isEmpty() ? "なし" : String.join(", ", charmPoints))
                .append("\n\n");

        sb.append("HP: ").append(hp).append("/").append(getEffectiveMaxHp()).append("\n");
        sb.append("AP: ").append(ap).append("/").append(getEffectiveMaxAp()).append("\n\n");
        sb.append("銀貨: ").append(money).append("/").append(getEffectiveMaxMoney()).append("\n");

        // 戦闘ステータス表示
        CombatStats combatStats = getCombatStats();
        sb.append("戦闘ステータス: ");
        sb.append("強靭:").append(combatStats.might()).append(" ");
        sb.append("聡明:").append(combatStats.insight()).append(" ");
        sb.append("機敏:").append(combatStats.finesse()).append(" ");
        sb.append("風格:").append(combatStats.presence()).append("\n");
        // sb.append("官能性:").append(combatStats.sensuality()).append("\n"); //
        // 現状は隠しステータスとする
        sb.append("\n");

        // スキル表示（baseSkills + アイテム由来）
        List<String> effectiveSkills = getEffectiveSkills();
        sb.append("技能: ").append(effectiveSkills.isEmpty() ? "なし" : String.join(", ", effectiveSkills)).append("\n");

        // 特徴表示（3分類: 初期 → 引継ぎ済み → その周）
        java.util.List<String> allTraitIds = new java.util.ArrayList<>();
        if (baseTraits != null)
            allTraitIds.addAll(baseTraits);
        if (inheritedTraits != null)
            allTraitIds.addAll(inheritedTraits);
        if (traits != null)
            allTraitIds.addAll(traits);

        if (allTraitIds.isEmpty()) {
            sb.append("特徴: なし\n");
        } else {
            sb.append("特徴: ");
            java.util.List<String> traitNames = new java.util.ArrayList<>();
            for (String traitId : allTraitIds) {
                com.kh.tbrr.battle.data.TraitData td = com.kh.tbrr.battle.data.TraitRegistry.getTraitById(traitId);
                traitNames.add(td != null ? td.getName() : traitId);
            }
            sb.append(String.join(", ", traitNames)).append("\n");
        }

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

    public int getEffectiveMaxHp() {
        int bonus = 0;
        for (String traitId : getEffectiveTraits()) {
            com.kh.tbrr.battle.data.TraitData td = com.kh.tbrr.battle.data.TraitRegistry.getTraitById(traitId);
            if (td != null && td.getStatBonuses() != null) {
                bonus += td.getStatBonuses().getOrDefault("max_hp", 0);
            }
        }
        return maxHp + bonus;
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

    public int getEffectiveMaxAp() {
        int bonus = 0;
        for (String traitId : getEffectiveTraits()) {
            com.kh.tbrr.battle.data.TraitData td = com.kh.tbrr.battle.data.TraitRegistry.getTraitById(traitId);
            if (td != null && td.getStatBonuses() != null) {
                bonus += td.getStatBonuses().getOrDefault("max_ap", 0);
            }
        }
        return maxAp + bonus;
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

    public int getEffectiveMaxMoney() {
        int bonus = 0;
        for (String traitId : getEffectiveTraits()) {
            com.kh.tbrr.battle.data.TraitData td = com.kh.tbrr.battle.data.TraitRegistry.getTraitById(traitId);
            if (td != null && td.getStatBonuses() != null) {
                bonus += td.getStatBonuses().getOrDefault("max_money", 0);
            }
        }
        return maxMoney + bonus;
    }

    public void setMaxMoney(int maxMoney) {
        this.maxMoney = maxMoney;
    }

    public void setEquippedMainWeapon(String equippedMainWeapon) {
        this.equippedMainWeapon = equippedMainWeapon;
    }

    public void setEquippedAccessories(List<String> equippedAccessories) {
        this.equippedAccessories = equippedAccessories;
    }

    public List<String> getReserveEquipments() {
        return reserveEquipments;
    }

    public void setReserveEquipments(List<String> reserveEquipments) {
        this.reserveEquipments = reserveEquipments;
    }

    /**
     * 予備スロットの最大数を返す。
     * デフォルト1 + 「物持ち」(big_bag) Traitの所持数を加算。上限は3。
     * JSONフィールド maxReserveSlots は後方互換用に残すが、ゲーム中はこのメソッドで取得すること。
     */
    public int getMaxReserveSlots() {
        long bigBagCount = 0;
        // 3分類全てを対象にする（getEffectiveTraitsで合算済み）
        for (String traitId : getEffectiveTraits()) {
            com.kh.tbrr.battle.data.TraitData td = com.kh.tbrr.battle.data.TraitRegistry.getTraitById(traitId);
            if (td != null && "BIG_BAG".equals(td.getSystemicEffect())) {
                bigBagCount++;
            }
        }
        return (int) Math.min(3, 1 + bigBagCount);
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

    public List<String> getAbilities() {
        return abilities;
    }

    public void setAbilities(List<String> abilities) {
        this.abilities = abilities;
    }

    public void addAbility(String abilityId) {
        if (!abilities.contains(abilityId)) {
            abilities.add(abilityId);
        }
    }

    public List<String> getStances() {
        return stances;
    }

    public void setStances(List<String> stances) {
        this.stances = stances;
    }

    public void addStance(String stanceId) {
        if (!stances.contains(stanceId)) {
            stances.add(stanceId);
        }
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

    // ========== グレード ==========

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = Math.max(0, Math.min(20, grade));
    }

    /**
     * グレードを1増加する。最大20まで。
     * FatedOneの引継ぎ処理時に呼び出す。
     */
    public void incrementGrade() {
        this.grade = Math.min(20, this.grade + 1);
    }

    // ========== アビリティ 3分類 getter/setter ==========

    public List<String> getBaseAbilities() {
        if (baseAbilities == null)
            baseAbilities = new ArrayList<>();
        return baseAbilities;
    }

    public void setBaseAbilities(List<String> baseAbilities) {
        this.baseAbilities = baseAbilities != null ? baseAbilities : new ArrayList<>();
    }

    public List<String> getInheritedAbilities() {
        if (inheritedAbilities == null)
            inheritedAbilities = new ArrayList<>();
        return inheritedAbilities;
    }

    public void setInheritedAbilities(List<String> inheritedAbilities) {
        this.inheritedAbilities = inheritedAbilities != null ? inheritedAbilities : new ArrayList<>();
    }

    // ========== 特徴 3分類 getter/setter ==========

    public List<String> getBaseTraits() {
        if (baseTraits == null)
            baseTraits = new ArrayList<>();
        return baseTraits;
    }

    public void setBaseTraits(List<String> baseTraits) {
        this.baseTraits = baseTraits != null ? baseTraits : new ArrayList<>();
    }

    public List<String> getInheritedTraits() {
        if (inheritedTraits == null)
            inheritedTraits = new ArrayList<>();
        return inheritedTraits;
    }

    public void setInheritedTraits(List<String> inheritedTraits) {
        this.inheritedTraits = inheritedTraits != null ? inheritedTraits : new ArrayList<>();
    }
}