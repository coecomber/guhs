package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;

/** The economie slice's registry objects: items ({@code vr_item_}), deco blocks and mount entities ({@code vr_eco_}). */
public final class EcoSpul {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    // ---- Vadsmunten ---------------------------------------------------------------------------------------------------------
    public static final DeferredItem<EcoItems.Munt> MUNT_BRONS = ITEMS.registerItem("vr_item_munt_brons", p -> new EcoItems.Munt(p, 1),
            () -> new Item.Properties().stacksTo(99));
    public static final DeferredItem<EcoItems.Munt> MUNT_ZILVER = ITEMS.registerItem("vr_item_munt_zilver", p -> new EcoItems.Munt(p, 100),
            () -> new Item.Properties().stacksTo(99));
    public static final DeferredItem<EcoItems.Munt> MUNT_GOUD = ITEMS.registerItem("vr_item_munt_goud", p -> new EcoItems.Munt(p, 10000),
            () -> new Item.Properties().stacksTo(99).rarity(Rarity.UNCOMMON));

    // ---- drankjes -----------------------------------------------------------------------------------------------------------
    /** (balans 2026-11) 120 Vads, the cheap zone-1 potion. */
    public static final DeferredItem<EcoItems.Drankje> KLEIN_VADSDRANKJE = drankje("vr_item_klein_vadsdrankje", EcoItems.Drankje.Soort.KLEIN_VADS);
    public static final DeferredItem<EcoItems.Drankje> VADSDRANKJE = drankje("vr_item_vadsdrankje", EcoItems.Drankje.Soort.VADS);
    public static final DeferredItem<EcoItems.Drankje> ENERGIEDRANKJE = drankje("vr_item_energiedrankje", EcoItems.Drankje.Soort.ENERGIE);
    public static final DeferredItem<EcoItems.Drankje> VAHOEGDRANKJE = drankje("vr_item_vahoegdrankje", EcoItems.Drankje.Soort.VAHOEG);
    public static final DeferredItem<EcoItems.Drankje> SNELPOOTJESDRANKJE = drankje("vr_item_snelpootjesdrankje", EcoItems.Drankje.Soort.SNELPOOTJES);

    // ---- smid materials -----------------------------------------------------------------------------------------------------
    public static final DeferredItem<EcoItems.Uitleg> VERBETERSTEENTJE = ITEMS.registerItem("vr_item_verbetersteentje", EcoItems.Uitleg::new,
            () -> new Item.Properties().stacksTo(99));
    public static final DeferredItem<EcoItems.Uitleg> HERROLKNABBEL = ITEMS.registerItem("vr_item_herrolknabbel", EcoItems.Uitleg::new,
            () -> new Item.Properties().stacksTo(99).rarity(Rarity.RARE));
    public static final DeferredItem<EcoItems.Uitleg> PLUISDRAAD = ITEMS.registerItem("vr_item_pluisdraad", EcoItems.Uitleg::new,
            () -> new Item.Properties().stacksTo(99));

    // ---- mounts ---------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<EcoItems.Fluitje> RIJFLUITJE = ITEMS.registerItem("vr_item_rijfluitje", EcoItems.Fluitje::new,
            () -> new Item.Properties().stacksTo(1));
    /** Rare loot (bazen slice: Koning Klodder): unlocks the Klodderros. Id {@code guhs:vr_item_zadel_klodderros}. */
    public static final DeferredItem<EcoItems.Zadel> ZADEL_KLODDERROS = ITEMS.registerItem("vr_item_zadel_klodderros",
            p -> new EcoItems.Zadel(p, RijdierSoort.KLODDERROS), () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<EcoItems.Zadel> ZADEL_HOEDHUPPER = ITEMS.registerItem("vr_item_zadel_hoedhupper",
            p -> new EcoItems.Zadel(p, RijdierSoort.HOEDHUPPER), () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    /** G-MOUNTS: saddles of the stal mounts (also usable as quest/loot rewards), and a Pluimguh egg per colour (hatches = unlocks it). */
    public static final DeferredItem<EcoItems.Zadel> ZADEL_WOLKENSCHAAPJE = ITEMS.registerItem("vr_item_zadel_wolkenschaapje",
            p -> new EcoItems.Zadel(p, RijdierSoort.WOLKENSCHAAPJE), () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<EcoItems.Zadel> ZADEL_SLAKKIE = ITEMS.registerItem("vr_item_zadel_slakkie",
            p -> new EcoItems.Zadel(p, RijdierSoort.SLAKKIE), () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<EcoItems.Zadel> ZADEL_KNUFFELHERT = ITEMS.registerItem("vr_item_zadel_knuffelhert",
            p -> new EcoItems.Zadel(p, RijdierSoort.KNUFFELHERT), () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<EcoItems.Zadel> ZADEL_GUHWOLF = ITEMS.registerItem("vr_item_zadel_guhwolf",
            p -> new EcoItems.Zadel(p, RijdierSoort.GUHWOLF), () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final Map<RijdierSoort.PluimKleur, DeferredItem<EcoItems.PluimEi>> PLUIMEI = new java.util.EnumMap<>(RijdierSoort.PluimKleur.class);

    static {
        for (RijdierSoort.PluimKleur k : RijdierSoort.PluimKleur.values()) {
            PLUIMEI.put(k, ITEMS.registerItem("vr_item_pluimei_" + k.id(), p -> new EcoItems.PluimEi(p, k),
                    () -> new Item.Properties().stacksTo(1).rarity(k == RijdierSoort.PluimKleur.GOUD || k == RijdierSoort.PluimKleur.STERRENNACHT
                            ? Rarity.EPIC : Rarity.RARE)));
        }
    }

    public static final DeferredHolder<EntityType<?>, EntityType<RijdierEntity>> HOEDHUPPER = rijdier("vr_eco_hoedhupper", 1.1f, 1.3f);
    public static final DeferredHolder<EntityType<?>, EntityType<RijdierEntity>> KLODDERROS = rijdier("vr_eco_klodderros", 1.3f, 1.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<RijdierEntity>> PLUIMGUH = rijdier("vr_eco_pluimguh", 1.4f, 2.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<RijdierEntity>> KNUFFELHERT = rijdier("vr_eco_knuffelhert", 1.3f, 2.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<RijdierEntity>> GUHWOLF = rijdier("vr_eco_guhwolf", 1.3f, 2.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<RijdierEntity>> WOLKENSCHAAPJE = rijdier("vr_eco_wolkenschaapje", 1.2f, 1.2f);
    public static final DeferredHolder<EntityType<?>, EntityType<RijdierEntity>> SLAKKIE = rijdier("vr_eco_slakkie", 1.1f, 1.5f);

    // (gear: economie has no base types of its own since the 1.10.0 integration; see EcoBasisTypes)

    // ---- trophies / deco (cosmetic, they go home) -----------------------------------------------------------------------------
    public static final DeferredBlock<DecoBlock> MINI_KONING_KLODDER = deco("vr_eco_mini_koning_klodder", MapColor.COLOR_LIGHT_BLUE, SoundType.SLIME_BLOCK,
            Block.box(3, 0, 3, 13, 12, 13));
    public static final DeferredBlock<DecoBlock> OPPERZWAM_BEELD = deco("vr_eco_opperzwam_beeld", MapColor.COLOR_RED, SoundType.STONE,
            Block.box(2, 0, 2, 14, 16, 14));
    public static final DeferredBlock<DecoBlock> KLODDERREUS_KNUFFEL = deco("vr_eco_klodderreus_knuffel", MapColor.COLOR_GRAY, SoundType.WOOL,
            Block.box(2, 0, 2, 14, 13, 14));
    public static final DeferredBlock<DecoBlock> VADSRIJK_VAANDEL = deco("vr_eco_vadsrijk_vaandel", MapColor.COLOR_PINK, SoundType.WOOL,
            Block.box(6, 0, 6, 10, 16, 10));
    public static final List<DeferredBlock<DecoBlock>> DECO = List.of(MINI_KONING_KLODDER, OPPERZWAM_BEELD, KLODDERREUS_KNUFFEL, VADSRIJK_VAANDEL);

    static {
        for (DeferredBlock<DecoBlock> b : DECO) {
            ITEMS.registerItem(b.getId().getPath(), p -> new net.minecraft.world.item.BlockItem(b.get(), p), () -> new Item.Properties().rarity(Rarity.UNCOMMON));
        }
    }

    private EcoSpul() {
    }

    private static DeferredItem<EcoItems.Drankje> drankje(String id, EcoItems.Drankje.Soort s) {
        return ITEMS.registerItem(id, p -> new EcoItems.Drankje(p, s), () -> Drinken.eigenschappen(s.gedeeld()));
    }

    private static DeferredBlock<DecoBlock> deco(String id, MapColor kleur, SoundType geluid, net.minecraft.world.phys.shapes.VoxelShape vorm) {
        return BLOCKS.registerBlock(id, p -> new DecoBlock(p, vorm),
                () -> BlockBehaviour.Properties.of().mapColor(kleur).strength(0.6f).sound(geluid).noOcclusion());
    }

    private static DeferredHolder<EntityType<?>, EntityType<RijdierEntity>> rijdier(String id, float b, float h) {
        return ENTITIES.register(id, () -> EntityType.Builder.<RijdierEntity>of(RijdierEntity::new, MobCategory.MISC).sized(b, h).eyeHeight(h * 0.8f)
                .clientTrackingRange(10).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    public static DeferredHolder<EntityType<?>, EntityType<RijdierEntity>> holder(RijdierSoort s) {
        return switch (s) {
            case PLUIMGUH -> PLUIMGUH;
            case HOEDHUPPER -> HOEDHUPPER;
            case KLODDERROS -> KLODDERROS;
            case KNUFFELHERT -> KNUFFELHERT;
            case GUHWOLF -> GUHWOLF;
            case WOLKENSCHAAPJE -> WOLKENSCHAAPJE;
            case SLAKKIE -> SLAKKIE;
        };
    }

    public static EntityType<RijdierEntity> type(RijdierSoort s) {
        return holder(s).get();
    }

    public static RijdierSoort soort(EntityType<?> t) {
        for (RijdierSoort s : RijdierSoort.values()) {
            if (holder(s).get() == t) {
                return s;
            }
        }
        return RijdierSoort.HOEDHUPPER;
    }

    /** The icon of a mount (screen, HUD): its saddle, or for the Pluimguh the egg of that colour. */
    public static net.minecraft.world.item.ItemStack icoon(RijdierSoort s, RijdierSoort.PluimKleur k) {
        Item i = switch (s) {
            case PLUIMGUH -> PLUIMEI.get(k).get();
            case HOEDHUPPER -> ZADEL_HOEDHUPPER.get();
            case KLODDERROS -> ZADEL_KLODDERROS.get();
            case KNUFFELHERT -> ZADEL_KNUFFELHERT.get();
            case GUHWOLF -> ZADEL_GUHWOLF.get();
            case WOLKENSCHAAPJE -> ZADEL_WOLKENSCHAAPJE.get();
            case SLAKKIE -> ZADEL_SLAKKIE.get();
        };
        return new net.minecraft.world.item.ItemStack(i);
    }
}
