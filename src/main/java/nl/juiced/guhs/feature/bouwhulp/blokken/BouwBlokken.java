package nl.juiced.guhs.feature.bouwhulp.blokken;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhFurnitureBlock;
import nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Behang;
import nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Detail;
import nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Knuffelblok;
import nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Kozijn;
import nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Lantaarnpaal;
import nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Luik;
import nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Schoorsteen;
import nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Vogelhuisje;
import nl.juiced.guhs.feature.bouwhulp.blokken.VormBlokken.DakHalf;
import nl.juiced.guhs.feature.bouwhulp.blokken.VormBlokken.DakKwart;
import nl.juiced.guhs.feature.bouwhulp.blokken.VormBlokken.Hoekje;
import nl.juiced.guhs.feature.bouwhulp.blokken.VormBlokken.PaalBalk;
import nl.juiced.guhs.feature.bouwhulp.blokken.VormBlokken.VerticalePlaat;
import nl.juiced.guhs.registry.ModCreativeTabs;

/**
 * Op Safari! (1.6.0) deel 2, slice F11: the new building blocks (design 2.7; resources and the block list {@link BouwLijst}:
 * tools/features/bouwhulp_blokken.py). All craftable (mostly the stonecutter), all in their own creative tab "Guhs: Bouwen"
 * (the other bouwhulp slices add their tools there through {@link #TAB_EXTRA}).
 * <ul>
 *   <li>vormhulp: verticale platen + hoekjes for 12 woods, 13 stones and the guh blocks; palen + balken for the woods;
 *       daken (half and quarter slope) for the woods, 4 stones and the roof tiles ({@link VormBlokken});</li>
 *   <li>pastelbakstenen + pleister in 16 colours (block, trap, plaat, muur), dakpannen in 6, guh-tegels, behang,
 *       knuffelblokken ({@link DetailBlokken});</li>
 *   <li>guh-kozijnen, luiken, glas-in-lood (+ ruiten), patrijspoorten, dakgoot, regenpijp, guh-schoorsteen, dakkapellen;</li>
 *   <li>tuin &amp; straat: guh-hekjes + poortjes, guh-lantaarnpaal, deurmat "Welkom guh", bankjes, vogelhuisje.</li>
 * </ul>
 */
public final class BouwBlokken {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Guhs.MODID);

    /** Every block of this slice by id (creative order: {@link BouwLijst#ALLE}). */
    public static final Map<String, DeferredBlock<?>> PER_ID = new LinkedHashMap<>();
    /** Other bouwhulp slices (the Bouwkeet, the vormstaf, the kwasten...): add your items to the Bouwen tab here. */
    public static final List<Consumer<Consumer<ItemStack>>> TAB_EXTRA = new ArrayList<>();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("bouwen", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.guhs.bouwen"))
            .withTabsBefore(ModCreativeTabs.GUHS_TAB.getKey())
            .icon(() -> new ItemStack(blok("pastelbaksteen_roze")))
            .displayItems((params, output) -> {
                for (String id : BouwLijst.ALLE) {
                    output.accept(new ItemStack(blok(id)));
                }
                TAB_EXTRA.forEach(c -> c.accept(output::accept));
            })
            .build());

    static {
        // vormhulp: vanilla wood and stone
        for (String[] h : BouwLijst.HOUT) {
            Supplier<BlockBehaviour.Properties> p = () -> BlockBehaviour.Properties.ofFullCopy(vanilla(h[1]));
            vormhulp(h[0], p);
            // (not ofFullCopy: a log's map colour reads its axis, which a post has not)
            Supplier<BlockBehaviour.Properties> log = () -> BlockBehaviour.Properties.of().mapColor(vanilla(h[1]).defaultMapColor()).strength(2f)
                    .sound(vanilla(h[1]).defaultBlockState().getSoundType()).ignitedByLava().noOcclusion();
            reg(h[0] + "_paal", q -> new PaalBalk(q, false), log);
            reg(h[0] + "_balk", q -> new PaalBalk(q, true), log);
            daken(h[0], p);
        }
        for (String[] s : BouwLijst.STEEN) {
            vormhulp(s[0], () -> BlockBehaviour.Properties.ofFullCopy(vanilla(s[1])));
        }
        for (String s : BouwLijst.DAK_STEEN) {
            String blok = java.util.Arrays.stream(BouwLijst.STEEN).filter(r -> r[0].equals(s)).findFirst().orElseThrow()[1];
            daken(s, () -> BlockBehaviour.Properties.ofFullCopy(vanilla(blok)));
        }
        // pastel bricks and plaster
        for (String soort : new String[]{"pastelbaksteen", "pleister"}) {
            for (String[] k : BouwLijst.KLEUREN) {
                String n = soort + "_" + k[0];
                MapColor kleur = DyeColor.byName(k[1], DyeColor.PINK).getMapColor();
                Supplier<BlockBehaviour.Properties> p = soort.equals("pastelbaksteen")
                        ? () -> BlockBehaviour.Properties.of().mapColor(kleur).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2f, 6f)
                        : () -> BlockBehaviour.Properties.of().mapColor(kleur).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.25f, 4.2f);
                DeferredBlock<Block> blok = reg(n, Block::new, p);
                reg(n + "_trap", q -> new StairBlock(blok.get().defaultBlockState(), q), p);
                reg(n + "_plaat", SlabBlock::new, p);
                reg(n + "_muur", WallBlock::new, () -> p.get().forceSolidOn());
                vormhulp(n, p);
            }
        }
        // roof tiles
        for (String[] d : BouwLijst.DAKPANNEN) {
            String n = "dakpan_" + d[0];
            Supplier<BlockBehaviour.Properties> p = () -> BlockBehaviour.Properties.ofFullCopy(vanilla(d[1]));
            DeferredBlock<Block> blok = reg(n, Block::new, p);
            reg(n + "_trap", q -> new StairBlock(blok.get().defaultBlockState(), q), p);
            reg(n + "_plaat", SlabBlock::new, p);
            daken(n, p);
            vormhulp(n, p);
        }
        // tiles, wallpaper, snuggle blocks
        for (String t : BouwLijst.TEGELS) {
            Supplier<BlockBehaviour.Properties> p = () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_GLAZED_TERRACOTTA).pushReaction(PushReaction.NORMAL);
            reg("tegel_" + t, Block::new, p);
            reg("tegel_" + t + "_plaat", SlabBlock::new, p);
        }
        for (String b : BouwLijst.BEHANG) {
            reg("behang_" + b, Behang::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f).sound(SoundType.WOOD).ignitedByLava());
        }
        for (String k : BouwLijst.KNUFFEL) {
            reg("knuffelblok_" + k, Knuffelblok::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_WOOL).strength(0.6f));
        }
        // windows, shutters, glass
        for (String v : BouwLijst.VERF) {
            reg("guh_kozijn_" + v, Kozijn::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
            reg("guh_luik_" + v, Luik::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()
                    .isValidSpawn((s, l, pos, e) -> false));
        }
        for (String g : BouwLijst.GLAS) {
            reg("glasinlood_" + g, TransparentBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
            reg("glasinlood_" + g + "_ruit", IronBarsBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS_PANE));
        }
        for (String g : BouwLijst.PATRIJS) {
            reg("patrijspoort_" + g, TransparentBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(1.5f).sound(SoundType.COPPER));
        }
        // roof details
        Supplier<BlockBehaviour.Properties> zink = () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f).sound(SoundType.COPPER).noOcclusion();
        reg("dakgoot", q -> new Detail(q, true, new double[]{0, 11, 10, 16, 16, 16}), zink);
        reg("regenpijp", q -> new Detail(q, true, new double[]{6, 0, 11, 10, 16, 16}), zink);
        reg("guh_schoorsteen", Schoorsteen::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).mapColor(MapColor.COLOR_PINK).noOcclusion());
        for (String d : BouwLijst.DAKKAPEL) {
            reg("dakkapel_" + d, q -> new Detail(q, false, new double[]{2, 0, 4, 14, 10, 16}, new double[]{1, 9, 2, 15, 15, 16}),
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f).sound(SoundType.WOOD).noOcclusion());
        }
        // garden and street
        for (String v : BouwLijst.VERF) {
            reg("guh_hekje_" + v, FenceBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE));
            reg("guh_poortje_" + v, q -> new FenceGateBlock(WoodType.OAK, q), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE));
        }
        reg("guh_lantaarnpaal", Lantaarnpaal::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(2f)
                .sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.DESTROY)
                .lightLevel(s -> s.getValue(Lantaarnpaal.HALF) == DoubleBlockHalf.UPPER ? 15 : 0));
        reg("deurmat_welkom", q -> new Detail(q, false, new double[]{0, 0, 0, 16, 1, 16}),
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_CARPET));
        for (String b : BouwLijst.BANKJES) {
            reg("guh_bankje_" + b, q -> new GuhFurnitureBlock(q, 0.45, new double[]{0, 6, 3, 16, 7.5, 13.5}, new double[]{0, 7.5, 12.5, 16, 15, 14},
                    new double[]{1, 0, 4, 3, 6, 6}, new double[]{13, 0, 4, 15, 6, 6}, new double[]{1, 0, 11.5, 3, 6, 13}, new double[]{13, 0, 11.5, 15, 6, 13}),
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
        }
        reg("vogelhuisje", Vogelhuisje::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1f).sound(SoundType.WOOD).noOcclusion());

        for (DeferredBlock<?> b : PER_ID.values()) {
            ITEMS.registerSimpleBlockItem(b);
        }
    }

    private static void vormhulp(String id, Supplier<BlockBehaviour.Properties> p) {
        reg(id + "_vplaat", VerticalePlaat::new, p);
        reg(id + "_hoekje", Hoekje::new, p);
    }

    private static void daken(String id, Supplier<BlockBehaviour.Properties> p) {
        reg(id + "_dak_half", DakHalf::new, () -> p.get().noOcclusion());
        reg(id + "_dak_kwart", DakKwart::new, () -> p.get().noOcclusion());
    }

    private static <B extends Block> DeferredBlock<B> reg(String id, Function<BlockBehaviour.Properties, B> maker, Supplier<BlockBehaviour.Properties> p) {
        DeferredBlock<B> b = BLOCKS.registerBlock(id, maker, p);
        PER_ID.put(id, b);
        return b;
    }

    private static Block vanilla(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
    }

    /** The block of this slice with that id. */
    public static Block blok(String id) {
        return PER_ID.get(id).get();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
    }

    private BouwBlokken() {
    }
}
