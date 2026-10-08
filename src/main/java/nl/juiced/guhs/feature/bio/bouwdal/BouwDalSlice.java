package nl.juiced.guhs.feature.bio.bouwdal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.bio.BioZelftest;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.sterrenwacht.Buiten;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * biomes3 slice "bouw-dal": the Klaterdal mini structures and het weebhuisje. Resources: tools/features/bio_bouw_dal.py.
 * <ul>
 *   <li>The structures themselves are data ({@code guhs:bio_plek}); the open tea house has a thee-guh ({@link #THEEGUH}).</li>
 *   <li>Het weebhuisje: Evivads and Nielsvads ({@link #WEEB}), who leave for Japan by balloon ({@link WeebBallonEntity})
 *       when you talk to them and are back a game day later with a present ({@link WeebHuis}, {@link WeebHuizen},
 *       {@link Cadeaus}).</li>
 *   <li>The presents: the twelve blocks of the verzamelreeks ({@link #REEKS}), four outfits (GuhClothes JAPAN_*), four
 *       foods ({@link #ETEN}; a guh eats them too: item tag guhs:band/snacks).</li>
 *   <li>The fixed things of the house ({@code weeb_*}): blocks without an item, which cannot be broken.</li>
 * </ul>
 */
public final class BouwDalSlice {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final String KLEDINGBRON = "weeb_japan";
    public static final Buiten.Bescherming BESCHERMING = new Buiten.Bescherming("weebhuisje", "gui.guhs.weeb.beschermd");

    private static BlockBehaviour.Properties japan() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.4f).sound(SoundType.DECORATED_POT).noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties vast() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(-1f, 3600000f).sound(SoundType.WOOD).noOcclusion().noLootTable()
                .pushReaction(PushReaction.BLOCK);
    }

    private static VoxelShape doos(double x0, double y0, double z0, double x1, double y1, double z1) {
        return Block.box(x0, y0, z0, x1, y1, z1);
    }

    // --- the verzamelreeks (the order of Cadeaus.REEKS) ----------------------------------------------------------------
    public static final Map<String, DeferredBlock<KleinBlok>> REEKS = new LinkedHashMap<>();
    public static final List<DeferredItem<? extends Item>> ITEMLIJST = new ArrayList<>();

    private static void reeks(String id, VoxelShape vorm) {
        DeferredBlock<KleinBlok> blok = "japan_windgong".equals(id)
                ? BLOCKS.registerBlock(id, p -> new KleinBlok.Windgong(p, vorm), () -> japan())
                : BLOCKS.registerBlock(id, p -> new KleinBlok(p, vorm, null), () -> "japan_lampion".equals(id) ? japan().lightLevel(s -> 12) : japan());
        REEKS.put(id, blok);
        ITEMLIJST.add(ITEMS.registerItem(id, p -> new CadeauBlokItem(blok.get(), p), p -> p.useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON)));
    }

    static {
        reeks("japan_geluksguh", doos(3, 0, 4, 13, 13, 12));
        reeks("japan_lampion", doos(4, 0.5, 4, 12, 16, 12));
        reeks("japan_mini_torii", doos(0, 0, 6, 16, 16, 10));
        reeks("japan_ramenkom", doos(2.5, 0, 2.5, 13.5, 6.5, 13.5));
        reeks("japan_daruma_guh", doos(3.5, 0, 3.5, 12.5, 13, 12.5));
        reeks("japan_waaier", doos(1, 0, 6.5, 15, 16, 9.5));
        reeks("japan_theeservies", doos(1.5, 0, 3, 14.5, 7, 13));
        reeks("japan_kokeshi_guh", doos(4.8, 0, 5, 11.2, 15, 11));
        reeks("japan_koinobori", doos(6, 0, 6, 16, 16, 10));
        reeks("japan_bonsai_schaaltje", doos(1.5, 0, 3.5, 14.5, 12, 12.5));
        reeks("japan_windgong", doos(5, 0.5, 5, 11, 16, 11));
        reeks("japan_maneki_knabbel", doos(3.5, 0, 4.5, 12.5, 13, 11.5));
    }

    // --- the fixed things of het weebhuisje (no items) ------------------------------------------------------------------
    public static final DeferredBlock<KleinBlok.Soort> FIGUURTJES = BLOCKS.registerBlock("weeb_figuurtjes",
            p -> new KleinBlok.Soort(p, doos(0, 0, 8, 16, 16, 16), "gui.guhs.weeb.klik.figuurtjes"), () -> vast());
    public static final DeferredBlock<KleinBlok.Soort> POSTER = BLOCKS.registerBlock("weeb_poster",
            p -> new KleinBlok.Soort(p, doos(1, 1, 15, 15, 15, 16), "gui.guhs.weeb.klik.poster"), () -> vast().noCollision());
    public static final DeferredBlock<KleinBlok> MANGASTAPEL = BLOCKS.registerBlock("weeb_mangastapel",
            p -> new KleinBlok(p, doos(3, 0, 3.5, 14.5, 11, 14), "gui.guhs.weeb.klik.mangastapel"), () -> vast());
    public static final DeferredBlock<KleinBlok> DAKIMAKURA = BLOCKS.registerBlock("weeb_dakimakura",
            p -> new KleinBlok(p, doos(1.5, 0, 9, 14.5, 3, 15), "gui.guhs.weeb.klik.dakimakura"), () -> vast());
    public static final DeferredBlock<KleinBlok> LOKET = BLOCKS.registerBlock("weeb_loket",
            p -> new KleinBlok(p, doos(0, 0, 3, 16, 11, 13), "gui.guhs.weeb.klik.loket"), () -> vast());
    public static final DeferredBlock<KleinBlok> NUMMERAUTOMAAT = BLOCKS.registerBlock("weeb_nummerautomaat",
            p -> new KleinBlok(p, doos(4, 0, 5, 12, 16, 11), "gui.guhs.weeb.klik.nummerautomaat"), () -> vast());
    public static final DeferredBlock<KleinBlok.Stand> LOKETBORD = BLOCKS.registerBlock("weeb_loketbord",
            p -> new KleinBlok.Stand(p, doos(1.5, 0, 6.5, 14.5, 7.5, 9.5), "gui.guhs.weeb.klik.loketbord", false), () -> vast().noCollision());
    public static final DeferredBlock<KleinBlok.Stand> BRIEFJE = BLOCKS.registerBlock("weeb_briefje",
            p -> new KleinBlok.Stand(p, doos(2, 3, 15, 14.5, 13, 16), "gui.guhs.weeb.klik.briefje", true), () -> vast().noCollision());

    // --- the foods ----------------------------------------------------------------------------------------------------------
    public static final Map<String, DeferredItem<Item>> ETEN = new LinkedHashMap<>();

    private static void eten(String id, int voeding, float verzadiging, boolean kom) {
        DeferredItem<Item> item = ITEMS.registerItem(id, EtenItem::new, () -> {
            Item.Properties p = new Item.Properties().food(new FoodProperties.Builder().nutrition(voeding).saturationModifier(verzadiging).build());
            return kom ? p.stacksTo(16).usingConvertsTo(Items.BOWL) : p;
        });
        ETEN.put(id, item);
        ITEMLIJST.add(item);
    }

    static {
        eten("japan_sushi", 4, 0.5f, false);
        eten("japan_ramen", 9, 0.8f, true);
        eten("japan_mochi", 3, 0.3f, false);
        eten("japan_onigiri", 5, 0.6f, false);
    }

    public static final DeferredHolder<EntityType<?>, EntityType<WeebBallonEntity>> WEEB_BALLON = ENTITIES.register("weeb_ballon",
            () -> EntityType.Builder.<WeebBallonEntity>of(WeebBallonEntity::new, MobCategory.MISC).sized(1.8f, 1.2f).clientTrackingRange(16)
                    .updateInterval(3).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("weeb_ballon"))));

    /** A block of the verzamelreeks as an item: one grey line that says where it comes from. */
    public static class CadeauBlokItem extends BlockItem {
        public CadeauBlokItem(Block block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("gui.guhs.japan.lore").withStyle(ChatFormatting.GRAY));
        }
    }

    public static class EtenItem extends Item {
        public EtenItem(Item.Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("gui.guhs.japan.eten").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Evivads and Nielsvads: everything they say and do is the house's ({@link WeebHuis#praat}). */
    public static final NpcRole WEEB = (npc, player) -> {
        if (player.level() instanceof ServerLevel level) {
            WeebHuizen.bij(level, npc.blockPosition()).praat(level, npc, player);
        }
    };

    /** The thee-guh of the open tea house: a few calm words. */
    public static final NpcRole THEEGUH = (npc, player) ->
            GuhQuests.say(player, npc, "quest.guhs.dal.theeguh." + (1 + player.getRandom().nextInt(3)));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        NpcRollen.zet(GuhNpcEntity.Kind.WEEB_EVIVADS, WEEB);
        NpcRollen.zet(GuhNpcEntity.Kind.WEEB_NIELSVADS, WEEB);
        NpcRollen.zet(GuhNpcEntity.Kind.DAL_THEEGUH, THEEGUH);
        for (var c : Cadeaus.OUTFITS) {
            KledingBronnen.bron(c, KLEDINGBRON);
        }
        SuperkompasItem.voegToe("knus", "weebhuisje");
        BESCHERMING.register();
        NeoForge.EVENT_BUS.addListener(WeebHuizen::onServerTick);
        NeoForge.EVENT_BUS.addListener(BouwDalCheck::commando);
        BioZelftest.registreer("bouw-dal", BouwDalCheck::zelftest);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<? extends Item> item : ITEMLIJST) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private BouwDalSlice() {
    }
}
