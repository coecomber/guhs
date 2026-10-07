package nl.juiced.guhs.registry;

import java.util.function.Supplier;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.item.GuhCompassItem;
import nl.juiced.guhs.item.BankGuhItem;
import nl.juiced.guhs.item.GuhArmorItem;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.Guhs;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** 1.21.1 {@code FoodProperties.Builder#fast()}: eaten in 0.8 s instead of 1.6 s (26.1: part of the Consumable). */
    public static final Consumable FAST_FOOD = Consumables.defaultFood().consumeSeconds(0.8f).build();

    /** 26.1: DeferredSpawnEggItem is gone; the entity type goes into the ENTITY_DATA component (delayed: entity types register after items). */
    public static DeferredItem<SpawnEggItem> spawnEgg(String name, Supplier<? extends EntityType<?>> type) {
        return spawnEgg(ITEMS, name, type);
    }

    /** Spawn egg on any item register (features with their own {@code DeferredRegister.Items}). */
    public static DeferredItem<SpawnEggItem> spawnEgg(DeferredRegister.Items items, String name, Supplier<? extends EntityType<?>> type) {
        return items.registerItem(name, SpawnEggItem::new, p -> p.delayedComponent(DataComponents.ENTITY_DATA,
                ctx -> TypedEntityData.<EntityType<?>>of(type.get(), new CompoundTag())));
    }

    /**
     * Guh body armour: the vanilla body-armour value of the material (like wolf/horse armour), only for guhs, never
     * damaged. Equipping stays our own right-click on the guh (GuhEntity) and the wardrobe slot.
     */
    private static Item.Properties guhArmor(ArmorMaterial material) {
        return new Item.Properties().stacksTo(1)
                .attributes(material.createAttributes(ArmorType.BODY))
                .delayedComponent(DataComponents.EQUIPPABLE, ctx -> Equippable.builder(EquipmentSlot.BODY)
                        .setEquipSound(material.equipSound())
                        .setAllowedEntities(HolderSet.direct(ModEntities.GUH.get().builtInRegistryHolder()))
                        .setDamageOnHurt(false)
                        .build());
    }

    /** Tames, heals and breeds guhs. Also a tasty snack for players. */
    public static final DeferredItem<Item> KAAS_KNABBELS = ITEMS.registerSimpleItem("kaas_knabbels",
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).build(), FAST_FOOD));

    /** A guh music disc: "Ze hangen aan me veh" (in the jukebox at the guh picnic, see quest/PicknickMuziek). */
    public static final DeferredItem<net.minecraft.world.item.Item> MUSIC_DISC_ZE_HANGEN = ITEMS.registerSimpleItem("music_disc_ze_hangen",
            () -> new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)
                    .jukeboxPlayable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG, Guhs.id("ze_hangen"))));
    public static final DeferredItem<SpawnEggItem> GUH_SPAWN_EGG = spawnEgg("guh_spawn_egg", ModEntities.GUH);

    /** Fat dropped by Mika. Charges the frying pan (+64 fries). */
    public static final DeferredItem<Item> MIKA_VET = ITEMS.registerSimpleItem("mika_vet", () -> new Item.Properties());

    /** Fried kaas knabbels: instantly heal a tamed guh to full health. Also a proper meal for players. */
    public static final DeferredItem<Item> GEFRITUURDE_KAASKNABBELS = ITEMS.registerSimpleItem("gefrituurde_kaasknabbels",
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7f).build()));

    public static final DeferredItem<SpawnEggItem> MIKA_SPAWN_EGG = spawnEgg("mika_spawn_egg", ModEntities.MIKA);

    /** A tamed guh you picked up (sneak + right-click). */
    public static final DeferredItem<PickedUpGuhItem> PICKED_UP_GUH = ITEMS.registerItem("picked_up_guh",
            PickedUpGuhItem::new, () -> new Item.Properties().stacksTo(1));

    public static final DeferredItem<BankGuhItem> BANK_GUH = ITEMS.registerItem("bank_guh",
            props -> new BankGuhItem(ModBlocks.BANK_GUH.get(), props), () -> new Item.Properties().stacksTo(1));

    public static final DeferredItem<BucketItem> KAAS_SAUS_BUCKET = ITEMS.registerItem("kaas_saus_bucket",
            props -> new BucketItem(ModFluids.KAAS_SAUS.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    /** Guh armour: right-click your tamed guh with it. */
    public static final DeferredItem<GuhArmorItem> IRON_GUH_ARMOR = ITEMS.registerItem("iron_guh_armor",
            props -> new GuhArmorItem(GuhArmorItem.Tier.IRON, props), () -> guhArmor(ArmorMaterials.IRON));
    public static final DeferredItem<GuhArmorItem> DIAMOND_GUH_ARMOR = ITEMS.registerItem("diamond_guh_armor",
            props -> new GuhArmorItem(GuhArmorItem.Tier.DIAMOND, props), () -> guhArmor(ArmorMaterials.DIAMOND));
    public static final DeferredItem<GuhArmorItem> NETHERITE_GUH_ARMOR = ITEMS.registerItem("netherite_guh_armor",
            props -> new GuhArmorItem(GuhArmorItem.Tier.NETHERITE, props), () -> guhArmor(ArmorMaterials.NETHERITE).fireResistant());

    // --- Vahoege Vads: diamond-tier, unbreakable, kept on death ---------------------------------------------------
    public static final DeferredItem<Item> VAHOEGE_VADS = ITEMS.registerSimpleItem("vahoege_vads", () -> new Item.Properties());
    public static final DeferredItem<Item> VAHOEGE_VADS_INGOT = ITEMS.registerSimpleItem("vahoege_vads_ingot", () -> new Item.Properties());

    private static Item.Properties unbreakable() {
        return new Item.Properties().component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }

    /** Vads gear: repaired with the ingot (overrides the material's tag-based repairable). */
    private static Item.Properties vadsRepair(Item.Properties props) {
        return props.repairable(VAHOEGE_VADS_INGOT.get());
    }

    // 26.1: SwordItem/PickaxeItem/DiggerItem/ArmorItem are gone - tool and armour behaviour are item components
    // (Item.Properties#sword/pickaxe/humanoidArmor); AxeItem/ShovelItem/HoeItem still exist for their right-click actions.
    public static final DeferredItem<Item> VADS_SWORD = ITEMS.registerItem("vahoege_vads_sword",
            Item::new, () -> vadsRepair(unbreakable().sword(ModArmorMaterials.VADS_TIER, 3.0f, -2.4f)));
    public static final DeferredItem<Item> VADS_PICKAXE = ITEMS.registerItem("vahoege_vads_pickaxe",
            Item::new, () -> vadsRepair(unbreakable().pickaxe(ModArmorMaterials.VADS_TIER, 1.0f, -2.8f)));
    public static final DeferredItem<AxeItem> VADS_AXE = ITEMS.registerItem("vahoege_vads_axe",
            props -> new AxeItem(ModArmorMaterials.VADS_TIER, 5.0f, -3.0f, vadsRepair(props)), () -> unbreakable());
    public static final DeferredItem<ShovelItem> VADS_SHOVEL = ITEMS.registerItem("vahoege_vads_shovel",
            props -> new ShovelItem(ModArmorMaterials.VADS_TIER, 1.5f, -3.0f, vadsRepair(props)), () -> unbreakable());
    public static final DeferredItem<HoeItem> VADS_HOE = ITEMS.registerItem("vahoege_vads_hoe",
            props -> new HoeItem(ModArmorMaterials.VADS_TIER, -3.0f, 0.0f, vadsRepair(props)), () -> unbreakable());

    /** Pickaxe + axe + shovel in one. */
    public static final DeferredItem<nl.juiced.guhs.item.PaxelItem> VADS_PAXEL = ITEMS.registerItem("vahoege_vads_paxel",
            nl.juiced.guhs.item.PaxelItem::new,
            () -> vadsRepair(nl.juiced.guhs.item.PaxelItem.properties(unbreakable(), ModArmorMaterials.VADS_TIER, 4.0f, -2.9f)));

    /** 1.3.1: shears of vahoege vads (the raw vads, in the vanilla shears shape) that never break. */
    public static final DeferredItem<nl.juiced.guhs.item.VadsSchaarItem> VADS_SHEARS = ITEMS.registerItem("vahoege_vads_shears",
            nl.juiced.guhs.item.VadsSchaarItem::new, () -> nl.juiced.guhs.item.VadsSchaarItem.properties(new Item.Properties()));

    public static final DeferredItem<Item> VADS_HELMET = ITEMS.registerItem("vahoege_vads_helmet",
            Item::new, () -> vadsRepair(unbreakable().humanoidArmor(ModArmorMaterials.VAHOEGE_VADS, ArmorType.HELMET)));
    public static final DeferredItem<Item> VADS_CHESTPLATE = ITEMS.registerItem("vahoege_vads_chestplate",
            Item::new, () -> vadsRepair(unbreakable().humanoidArmor(ModArmorMaterials.VAHOEGE_VADS, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> VADS_LEGGINGS = ITEMS.registerItem("vahoege_vads_leggings",
            Item::new, () -> vadsRepair(unbreakable().humanoidArmor(ModArmorMaterials.VAHOEGE_VADS, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> VADS_BOOTS = ITEMS.registerItem("vahoege_vads_boots",
            Item::new, () -> vadsRepair(unbreakable().humanoidArmor(ModArmorMaterials.VAHOEGE_VADS, ArmorType.BOOTS)));

    // --- guh clothes: one item per GuhClothes (the kleermaker, structure loot); the Mika-jager's swatter ------------
    public static final java.util.Map<nl.juiced.guhs.entity.GuhClothes, DeferredItem<nl.juiced.guhs.item.GuhClothingItem>> CLOTHES =
            new java.util.EnumMap<>(nl.juiced.guhs.entity.GuhClothes.class);

    private static DeferredItem<nl.juiced.guhs.item.GuhClothingItem> clothing(nl.juiced.guhs.entity.GuhClothes clothes) {
        return CLOTHES.computeIfAbsent(clothes, c -> ITEMS.registerItem(c.id(),
                props -> new nl.juiced.guhs.item.GuhClothingItem(c, props),
                () -> nl.juiced.guhs.item.GuhClothingItem.properties(c, new Item.Properties().stacksTo(1))));
    }

    public static final DeferredItem<nl.juiced.guhs.item.GuhClothingItem> PINK_ONESIE = clothing(nl.juiced.guhs.entity.GuhClothes.PINK_ONESIE);
    public static final DeferredItem<nl.juiced.guhs.item.GuhClothingItem> STRIPED_SWEATER = clothing(nl.juiced.guhs.entity.GuhClothes.STRIPED_SWEATER);
    public static final DeferredItem<nl.juiced.guhs.item.GuhClothingItem> RAINCOAT = clothing(nl.juiced.guhs.entity.GuhClothes.RAINCOAT);
    public static final DeferredItem<nl.juiced.guhs.item.GuhClothingItem> CHEF_JACKET = clothing(nl.juiced.guhs.entity.GuhClothes.CHEF_JACKET);
    public static final DeferredItem<nl.juiced.guhs.item.GuhClothingItem> RAIN_HAT = clothing(nl.juiced.guhs.entity.GuhClothes.RAIN_HAT);
    public static final DeferredItem<nl.juiced.guhs.item.GuhClothingItem> PARTY_HAT = clothing(nl.juiced.guhs.entity.GuhClothes.PARTY_HAT);
    public static final DeferredItem<nl.juiced.guhs.item.GuhClothingItem> CHEF_HAT = clothing(nl.juiced.guhs.entity.GuhClothes.CHEF_HAT);
    public static final DeferredItem<nl.juiced.guhs.item.GuhClothingItem> RED_BOWTIE = clothing(nl.juiced.guhs.entity.GuhClothes.RED_BOWTIE);
    public static final DeferredItem<nl.juiced.guhs.item.GuhClothingItem> BLACK_BOWTIE = clothing(nl.juiced.guhs.entity.GuhClothes.BLACK_BOWTIE);
    public static final DeferredItem<nl.juiced.guhs.item.GuhClothingItem> GUH_BACKPACK = clothing(nl.juiced.guhs.entity.GuhClothes.GUH_BACKPACK);

    static {
        for (nl.juiced.guhs.entity.GuhClothes c : nl.juiced.guhs.entity.GuhClothes.values()) {
            clothing(c); // all the others
        }
    }

    /** Iron sword stats, never breaks, five times the damage on Mikas (Big Mika too). */
    public static final DeferredItem<nl.juiced.guhs.item.MikaMepperItem> MIKA_MEPPER = ITEMS.registerItem("mika_mepper",
            nl.juiced.guhs.item.MikaMepperItem::new, () -> unbreakable().sword(net.minecraft.world.item.ToolMaterial.IRON, 3.0f, -2.4f));

    /** The clothing item for a piece of guh clothing. */
    public static Item clothingItem(nl.juiced.guhs.entity.GuhClothes clothes) {
        return CLOTHES.get(clothes).get();
    }

    // --- the quests: the lost guh (-> your own stomach), the broken sled, the Guhdex ------------------------------------
    public static final DeferredItem<Item> GUH_BUIKFLUITJE = ITEMS.registerItem("guh_buikfluitje",
            nl.juiced.guhs.item.QuestItems.Whistle::new, () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<GuhCompassItem> MIKA_SPOORKOMPAS = ITEMS.registerItem("mika_spoorkompas",
            props -> new GuhCompassItem(GuhCompassItem.MIKA_KAMP, props), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<GuhCompassItem> TAARTKRUIMELS = ITEMS.registerItem("taartkruimels",
            props -> new GuhCompassItem(nl.juiced.guhs.quest.GuhQuests.GUH_PICNIC, props), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> VERLOREN_GUH_TAART = ITEMS.registerItem("verloren_guh_taart",
            nl.juiced.guhs.item.QuestItems.Described::new, () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> GUH_BALLON = ITEMS.registerItem("guh_ballon",
            nl.juiced.guhs.item.QuestItems.Described::new, () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> GUH_KRISTAL = ITEMS.registerSimpleItem("guh_kristal", () -> new Item.Properties());
    public static final DeferredItem<Item> GUH_KRISTAL_VERREKIJKER = ITEMS.registerItem("guh_kristal_verrekijker",
            nl.juiced.guhs.item.QuestItems.Spyglass::new, () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> GUHDEX = ITEMS.registerItem("guhdex",
            nl.juiced.guhs.item.QuestItems.Dex::new, () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SLEEGLIJDER = ITEMS.registerItem("sleeglijder",
            nl.juiced.guhs.item.QuestItems.Described::new, () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> GUH_BELLETJE = ITEMS.registerItem("guh_belletje",
            nl.juiced.guhs.item.QuestItems.Described::new, () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> ROZE_LINT = ITEMS.registerItem("roze_lint",
            nl.juiced.guhs.item.QuestItems.Described::new, () -> new Item.Properties().stacksTo(1));
    /** Needed (not used up) to craft sled rails; the Slee-guh's reward. */
    public static final DeferredItem<Item> SLEEBOUWERSBOEK = ITEMS.registerItem("sleebouwersboek",
            props -> new nl.juiced.guhs.item.QuestItems.Described(props) {
                @Override
                public net.minecraft.world.item.ItemStackTemplate getCraftingRemainder(net.minecraft.world.item.ItemInstance instance) {
                    return new net.minecraft.world.item.ItemStackTemplate(instance.typeHolder(), 1, instance instanceof ItemStack stack
                            ? stack.getComponentsPatch() : net.minecraft.core.component.DataComponentPatch.EMPTY);
                }
            }, () -> new Item.Properties().stacksTo(1));
    // --- the guh sled and its rails ---------------------------------------------------------------------------------
    public static final DeferredItem<Item> GUH_SLEE = ITEMS.registerItem("guh_slee", nl.juiced.guhs.item.SledItem::new, () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SLEERAIL_RECHT = ITEMS.registerItem("sleerail_recht",
            props -> new nl.juiced.guhs.item.SleeRailItem(nl.juiced.guhs.item.SleeRailItem.Kind.STRAIGHT, props), () -> new Item.Properties());
    public static final DeferredItem<Item> SLEERAIL_BOCHT = ITEMS.registerItem("sleerail_bocht",
            props -> new nl.juiced.guhs.item.SleeRailItem(nl.juiced.guhs.item.SleeRailItem.Kind.CURVE, props), () -> new Item.Properties());
    public static final DeferredItem<Item> SLEERAIL_HELLING = ITEMS.registerItem("sleerail_helling",
            props -> new nl.juiced.guhs.item.SleeRailItem(nl.juiced.guhs.item.SleeRailItem.Kind.SLOPE, props), () -> new Item.Properties());
    // the coaster pieces (the guh kermis)
    public static final DeferredItem<Item> SLEERAIL_DROP = ITEMS.registerItem("sleerail_drop",
            props -> new nl.juiced.guhs.item.SleeRailItem(nl.juiced.guhs.item.SleeRailItem.Kind.DROP, props), () -> new Item.Properties());
    public static final DeferredItem<Item> SLEERAIL_KURKENTREKKER = ITEMS.registerItem("sleerail_kurkentrekker",
            props -> new nl.juiced.guhs.item.SleeRailItem(nl.juiced.guhs.item.SleeRailItem.Kind.SPIRAL, props), () -> new Item.Properties());
    public static final DeferredItem<Item> SLEERAIL_SCHANS = ITEMS.registerItem("sleerail_schans",
            props -> new nl.juiced.guhs.item.SleeRailItem(nl.juiced.guhs.item.SleeRailItem.Kind.JUMP, props), () -> new Item.Properties());
    /** One for every lap of the guh kermis coaster; the Kermis-guh sells the kermis outfit for them. */
    public static final DeferredItem<Item> KERMISBON = ITEMS.registerSimpleItem("kermisbon", () -> new Item.Properties());
    /** For finding all hidden guhs in the verstopguh house; Verstopguhtje sells the detective outfit for them. */
    public static final DeferredItem<Item> VERSTOPGUHTICKET = ITEMS.registerSimpleItem("verstopguhticket", () -> new Item.Properties());
    /** Points to the verstopguh house; sold by the vads temmer. */
    public static final DeferredItem<GuhCompassItem> VERSTOPKOMPAS = ITEMS.registerItem("verstopkompas",
            props -> new GuhCompassItem(GuhCompassItem.VERSTOPGUH_HUIS, props), () -> new Item.Properties().stacksTo(1));
    /** Looks for whatever you choose (right-click): replaces the single-purpose guh compasses. */
    public static final DeferredItem<nl.juiced.guhs.item.SuperkompasItem> SUPERKOMPAS = ITEMS.registerItem("guhmensie_superkompas",
            nl.juiced.guhs.item.SuperkompasItem::new, () -> new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));
    /** Puts down a Reisguh (a waypoint) wherever you like in the Guhmension. */
    public static final DeferredItem<nl.juiced.guhs.item.ReisguhFluitjeItem> REISGUH_FLUITJE = ITEMS.registerItem("reisguh_fluitje",
            nl.juiced.guhs.item.ReisguhFluitjeItem::new, () -> new Item.Properties().stacksTo(16));
    /** Points to the (legendary rare) guh castle; sold by a master vads temmer. */
    public static final DeferredItem<GuhCompassItem> KONINGSKOMPAS = ITEMS.registerItem("koningskompas",
            props -> new GuhCompassItem(GuhCompassItem.GUH_KASTEEL, props), () -> new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
    /** Points to the guh kermis; sold by the vads temmer. */
    public static final DeferredItem<GuhCompassItem> KERMISKOMPAS = ITEMS.registerItem("kermiskompas",
            props -> new GuhCompassItem(GuhCompassItem.GUH_KERMIS, props), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<BucketItem> MAAGZUUR_BUCKET = ITEMS.registerItem("maagzuur_bucket",
            props -> new BucketItem(ModFluids.MAAGZUUR.get(), props), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    // --- guh compasses (found in guh structure chests; work in the Guhmension) -----------------------------------
    public static final DeferredItem<GuhCompassItem> GUH_CAVE_COMPASS = ITEMS.registerItem("guh_cave_compass",
            props -> new GuhCompassItem(GuhCompassItem.GUH_CAVES, props), () -> new Item.Properties().stacksTo(1));
    /** Points to Moeder Vadsig's very rare shrine (the start of the guh stomach quest); sold by the vads temmer. */
    public static final DeferredItem<GuhCompassItem> HEILIGDOM_KOMPAS = ITEMS.registerItem("heiligdom_kompas",
            props -> new GuhCompassItem(GuhCompassItem.VADSIG_HEILIGDOM, props), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<GuhCompassItem> CHALLENGE_COMPASS = ITEMS.registerItem("challenge_compass",
            props -> new GuhCompassItem(GuhCompassItem.CHALLENGING_GUH_CAVES, props), () -> new Item.Properties().stacksTo(1));

    // --- the guh sea: guh fish ------------------------------------------------------------------------------------
    public static final DeferredItem<Item> GUH_VIS = ITEMS.registerSimpleItem("guh_vis",
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).build()));
    public static final DeferredItem<Item> GEBAKKEN_GUH_VIS = ITEMS.registerSimpleItem("gebakken_guh_vis",
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f).build()));
    public static final DeferredItem<net.minecraft.world.item.MobBucketItem> GUH_VIS_BUCKET = ITEMS.registerItem("guh_vis_bucket",
            props -> new net.minecraft.world.item.MobBucketItem(ModEntities.GUH_VIS.get(), net.minecraft.world.level.material.Fluids.WATER,
                    net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_FISH, props),
            () -> new Item.Properties().stacksTo(1).component(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY));
    public static final DeferredItem<SpawnEggItem> GUH_VIS_SPAWN_EGG = spawnEgg("guh_vis_spawn_egg", ModEntities.GUH_VIS);
    public static final DeferredItem<net.minecraft.world.item.PlaceOnWaterBlockItem> GUH_WATERLELIE = ITEMS.registerItem("guh_waterlelie",
            props -> new net.minecraft.world.item.PlaceOnWaterBlockItem(ModBlocks.GUH_WATERLELIE.get(), props), () -> new Item.Properties());

    // --- guh bees, guh slimes, Nether Mikas ------------------------------------------------------------------------
    /** Cheese honey from a full knabbelkorf: like honey, clears poison. */
    public static final DeferredItem<Item> KAASHONING = ITEMS.registerSimpleItem("kaashoning",
            // 26.1: HoneyBottleItem is gone; honey's behaviour (drink always, 2 s, clears poison, bottle back) is Consumables.HONEY_BOTTLE
            () -> new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16)
                    .food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.2f).alwaysEdible().build(), Consumables.HONEY_BOTTLE)
                    .usingConvertsTo(Items.GLASS_BOTTLE));
    public static final DeferredItem<Item> GUH_SLIMEBALL = ITEMS.registerSimpleItem("guh_slimeball", () -> new Item.Properties());
    public static final DeferredItem<SpawnEggItem> GUH_BEE_SPAWN_EGG = spawnEgg("guh_bee_spawn_egg", ModEntities.GUH_BEE);
    public static final DeferredItem<SpawnEggItem> GUH_SLIME_SPAWN_EGG = spawnEgg("guh_slime_spawn_egg", ModEntities.GUH_SLIME);
    public static final DeferredItem<SpawnEggItem> NETHER_MIKA_SPAWN_EGG = spawnEgg("nether_mika_spawn_egg", ModEntities.NETHER_MIKA);

    // --- food (2.0.0) ------------------------------------------------------------------------------------------------
    public static final DeferredItem<net.minecraft.world.item.BlockItem> GUH_TAART = ITEMS.registerItem("guh_taart",
            props -> new net.minecraft.world.item.BlockItem(ModBlocks.GUH_TAART.get(), props), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> GUH_CUPCAKE = ITEMS.registerSimpleItem("guh_cupcake",
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.4f).build()));
    public static final java.util.Map<String, DeferredItem<Item>> MACARONS = new java.util.LinkedHashMap<>();
    public static final DeferredItem<Item> KAASFONDUE = ITEMS.registerSimpleItem("kaasfondue", () -> new Item.Properties().stacksTo(1)
            .food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.8f).build()).usingConvertsTo(Items.BOWL));
    public static final DeferredItem<Item> KAASKNABBEL_MILKSHAKE = ITEMS.registerItem("kaasknabbel_milkshake", nl.juiced.guhs.item.MilkshakeItem::new,
            () -> new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).alwaysEdible().build(), Consumables.defaultDrink().build()));
    public static final DeferredItem<BlockItem> KAASKNABBELZAADJES = ITEMS.registerItem("kaasknabbelzaadjes",
            props -> new BlockItem(ModBlocks.KAASKNABBELPLANT.get(), props), () -> new Item.Properties().useItemDescriptionPrefix());

    static {
        for (String colour : new String[]{"roze", "mint", "citroen", "choco"}) {
            MACARONS.put(colour, ITEMS.registerSimpleItem("macaron_" + colour,
                    () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build(), FAST_FOOD)));
        }
        for (Object block : new Object[]{ModBlocks.GUH_STOEL, ModBlocks.GUH_TAFEL, ModBlocks.GUH_BANK, ModBlocks.GUH_KAST, ModBlocks.VLAGGETJES,
                ModBlocks.KAASBLOEM, ModBlocks.GUHOORTJES, ModBlocks.ROZE_GUHBLOEM, ModBlocks.KNABBELROOS, ModBlocks.ROZE_GRAS, ModBlocks.ZAADBAK}) {
            ITEMS.registerSimpleBlockItem((net.neoforged.neoforge.registries.DeferredBlock<?>) block);
        }
        ModBlocks.LAMPIONNEN.values().forEach(ITEMS::registerSimpleBlockItem);
        ModBlocks.ZITZAKKEN.values().forEach(ITEMS::registerSimpleBlockItem);
        ModBlocks.KUSSENS.values().forEach(ITEMS::registerSimpleBlockItem);
    }

    static {
        ITEMS.registerSimpleBlockItem(ModBlocks.KNABBELKORF);
        ITEMS.registerSimpleBlockItem(ModBlocks.EENRICHTINGSGLAS);
        ITEMS.registerSimpleBlockItem(ModBlocks.KONINGSTROON);
        ITEMS.registerSimpleBlockItem(ModBlocks.GUHBLOESEM_LOG);
        ITEMS.registerSimpleBlockItem(ModBlocks.GUHBLOESEM_PLANKS);
        ITEMS.registerSimpleBlockItem(ModBlocks.GUHBLOESEM_LEAVES);
        ITEMS.registerSimpleBlockItem(ModBlocks.GUHBLOESEM_SAPLING);
        ITEMS.registerSimpleBlockItem(ModBlocks.ROZE_SLIJMBLOK);
        ITEMS.registerSimpleBlockItem(ModBlocks.GUH_KRISTAL_CLUSTER);
        ITEMS.registerSimpleBlockItem(ModBlocks.GUH_KRISTAL_BLOK);
        ITEMS.registerSimpleBlockItem(ModBlocks.GUH_KRISTAL_LAMP);
        ITEMS.registerSimpleBlockItem(ModBlocks.GUH_KRISTALSTEEN);
        ITEMS.registerSimpleBlockItem(ModBlocks.KAASKNABBEL_STONE);
        ITEMS.registerSimpleBlockItem(ModBlocks.KAASKNABBEL_DEEPSLATE);
        ITEMS.registerSimpleBlockItem(ModBlocks.KAASKNABBEL_DIRT);
        ITEMS.registerSimpleBlockItem(ModBlocks.KAASKNABBEL_COBBLESTONE);
        ITEMS.registerSimpleBlockItem(ModBlocks.BLOCK_OF_KAASKNABBELS);
        ITEMS.registerSimpleBlockItem(ModBlocks.COMPRESSED_SUPER_VAHOEGE_VADS);
        ITEMS.registerSimpleBlockItem(ModBlocks.BLOCK_OF_VAHOEGE_VADS);   // 1.3.1
        ITEMS.registerSimpleBlockItem(ModBlocks.FRYING_PAN);
        ITEMS.registerSimpleBlockItem(ModBlocks.GUH_WHEEL);
        ITEMS.registerSimpleBlockItem(ModBlocks.GUH_WIRE);
        ITEMS.registerSimpleBlockItem(ModBlocks.GUH_SPAWNER);
        ITEMS.registerSimpleBlockItem(ModBlocks.KNABBELBAK);
        ITEMS.registerSimpleBlockItem(ModBlocks.NAAITAFEL);
        ITEMS.registerSimpleBlockItem(ModBlocks.VADSAAMBEELD);
        ITEMS.registerSimpleBlockItem(ModBlocks.BUIZENBANK);
        ITEMS.registerSimpleBlockItem(ModBlocks.MIKATROFEE);
        ITEMS.registerSimpleBlockItem(ModBlocks.VERTEERDE_KAASKNABBELS);

    }

    private ModItems() {
    }
}
