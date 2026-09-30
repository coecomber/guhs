package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * The blocks, items, entity and sounds of the surf beach of Guhwai'i (3.0, guhwaii-spellen): the schelpjesmunt (the coin
 * of surfing and hula), Lilo-guh's loaned surfplankje, the surf board entity, the Tiki decorations of Tikiguh's stall
 * (tiki_*: the torch, the masks, the statue, the thatch, the garland, the shell lantern, the board rack, the flower mat,
 * the bar stool and the radio) and the hula songs and beach sounds (tools/remix/make_hula.py).
 */
public final class GuhwaiiSpellenBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- items ---------------------------------------------------------------------------------------------------------
    /** The schelpjesmunt: a pink-and-cream shell coin with a tiny guh face, earned by surfing and dancing the hula. */
    public static final DeferredItem<Item> SCHELPJESMUNT = ITEMS.register("schelpjesmunt",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.UNCOMMON), false));
    /** Lilo-guh's surfplankje that she lends you for a game (loaned: it goes back to her afterwards). */
    public static final DeferredItem<Item> SURFPLANKJE_LEEN = ITEMS.register("surfplankje_leen",
            () -> new LoreItem(new Item.Properties().stacksTo(1), true));

    // --- the Tiki decorations ---------------------------------------------------------------------------------------------
    /** All the Tiki blocks in the order of the shop and the creative tab. */
    public static final List<DeferredBlock<? extends Block>> TIKI = new ArrayList<>();

    private static BlockBehaviour.Properties hout(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(1.0f).sound(SoundType.BAMBOO_WOOD);
    }

    public static final DeferredBlock<TikiFakkelBlock> TIKI_FAKKEL = tiki("tiki_fakkel", TikiFakkelBlock::new,
            hout(MapColor.COLOR_BROWN).noOcclusion().lightLevel(s -> 15));
    public static final DeferredBlock<TikiBlock> TIKI_MASKER = tiki("tiki_masker",
            p -> new TikiBlock(p, new double[][]{{2, 1, 13, 14, 15, 16}}, true), hout(MapColor.COLOR_ORANGE).noOcclusion());
    public static final DeferredBlock<TikiBlock> TIKI_MASKER_ROZE = tiki("tiki_masker_roze",
            p -> new TikiBlock(p, new double[][]{{2, 1, 13, 14, 15, 16}}, true), hout(MapColor.COLOR_PINK).noOcclusion());
    public static final DeferredBlock<TikiBlock> TIKI_BEELD = tiki("tiki_beeld",
            p -> new TikiBlock(p, new double[][]{{3, 0, 3, 13, 16, 13}}, false), hout(MapColor.COLOR_BROWN).noOcclusion());
    public static final DeferredBlock<Block> TIKI_RIETDAK = tiki("tiki_rietdak", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.6f).sound(SoundType.GRASS));
    public static final DeferredBlock<StairBlock> TIKI_RIETDAK_TRAP = tiki("tiki_rietdak_trap",
            p -> new StairBlock(TIKI_RIETDAK.get().defaultBlockState(), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.6f).sound(SoundType.GRASS));
    public static final DeferredBlock<SlabBlock> TIKI_RIETDAK_PLAAT = tiki("tiki_rietdak_plaat", SlabBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.6f).sound(SoundType.GRASS));
    public static final DeferredBlock<TikiBlock> HIBISCUS_SLINGER = tiki("tiki_bloemenslinger",
            p -> new TikiBlock(p, new double[][]{{0, 6, 14, 16, 14, 16}}, true),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.2f).sound(SoundType.AZALEA_LEAVES).noOcclusion().noCollission());
    public static final DeferredBlock<TikiBlock> SCHELPJES_LAMPION = tiki("tiki_schelpjeslampion",
            p -> new TikiBlock(p, new double[][]{{5, 2, 5, 11, 12, 11}, {7.5, 12, 7.5, 8.5, 16, 8.5}}, false),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.3f).sound(SoundType.BAMBOO_WOOD).noOcclusion().lightLevel(s -> 13));
    public static final DeferredBlock<TikiBlock> SURFPLANK_REK = tiki("tiki_surfplankrek",
            p -> new TikiBlock(p, new double[][]{{0, 0, 9, 16, 16, 16}}, false), hout(MapColor.COLOR_LIGHT_BLUE).noOcclusion());
    public static final DeferredBlock<BloemenmatBlock> HULA_BLOEMENMAT = tiki("tiki_bloemenmat", BloemenmatBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.1f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredBlock<TikiBlock> TIKI_KRUK = tiki("tiki_kruk",
            p -> new TikiBlock(p, new double[][]{{4, 0, 4, 12, 10, 12}}, false), hout(MapColor.COLOR_BROWN).noOcclusion());
    public static final DeferredBlock<RadiootjeBlock> TIKI_RADIOOTJE = tiki("tiki_radiootje", RadiootjeBlock::new, hout(MapColor.COLOR_ORANGE).noOcclusion());

    private static <B extends Block> DeferredBlock<B> tiki(String id, java.util.function.Function<BlockBehaviour.Properties, B> maker,
                                                           BlockBehaviour.Properties props) {
        DeferredBlock<B> block = BLOCKS.registerBlock(id, maker, () -> props);
        ITEMS.register(id, () -> new TikiBlockItem(block.get(), new Item.Properties()));
        TIKI.add(block);
        return block;
    }

    /** A Tiki block's item, with a little line about it (lang block.guhs.&lt;id&gt;.lore). */
    public static class TikiBlockItem extends BlockItem {
        public TikiBlockItem(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    // --- the surf board ---------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<SurfPlankEntity>> SURFPLANK = ENTITIES.register("guhwaiispellen_surfplank",
            () -> EntityType.Builder.<SurfPlankEntity>of(SurfPlankEntity::new, MobCategory.MISC).sized(1.1f, 0.3f).clientTrackingRange(10)
                    .updateInterval(1).noSave().noSummon().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guhwaiispellen_surfplank"))));

    // --- sounds -----------------------------------------------------------------------------------------------------------
    private static final Map<HulaLiedje, DeferredHolder<SoundEvent, SoundEvent>> LIEDJES = new EnumMap<>(HulaLiedje.class);

    static {
        for (HulaLiedje l : HulaLiedje.values()) {
            LIEDJES.put(l, SOUNDS.register(l.geluid(), () -> SoundEvent.createVariableRangeEvent(Guhs.id(l.geluid()))));
        }
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> UKELELE = geluid("guhwaiispellen.ukelele_tokkel");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLF_BREEKT = geluid("guhwaiispellen.golf_breekt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHELPJE = geluid("guhwaiispellen.schelpje");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALOHA = geluid("guhwaiispellen.aloha");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLONS = geluid("guhwaiispellen.plons");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Guhs.id(id)));
    }

    /** The sound event of a hula song (streamed). */
    public static Holder<SoundEvent> lied(HulaLiedje l) {
        return LIEDJES.get(l);
    }

    private GuhwaiiSpellenBlocks() {
    }
}
