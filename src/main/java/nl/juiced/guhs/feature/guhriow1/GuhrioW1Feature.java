package nl.juiced.guhs.feature.guhriow1;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * bbq2 (guhrio-w1): world 1 of Super Guhrio, "de binnentuin": levels 1-1 (de binnentuin) and 1-2 (de heggentuin) of the
 * Kasteel van de Grote Nether-Mika. A painted garden inside the castle walls that teaches walking, jumping, ?-blocks and
 * Guhmba's, with three big vadsmunten, a secret room and a flagpole in each level, and Pad-guh's "de prinses is in een
 * ander kasteeldeel" at the end of the world.
 * <ul>
 *     <li>The levels are data: tools/features/guhrio_w1_bouw.py builds them with the engine's lane builder into the castle
 *     (feature/guhrio plays them).</li>
 *     <li>This class registers the blocks they are painted with (grass on earth, a hedge, a cloud; and for the painted
 *     wall behind the lane: sky, far leaves, far clouds, hills with eyes), the little plants of the lawn, and the two
 *     invisible pieces of {@link GuhrioW1Blocks} (the tip and the secret).</li>
 *     <li>{@link Binnentuin} is what the world does: tips, secrets, the vadsmunten per level, Pad-guh's thanks.</li>
 * </ul>
 * Resources come from tools/features/guhrio_w1.py (+ guhrio_w1_bouw, guhrio_w1_tex).
 */
public final class GuhrioW1Feature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    // what you walk on in the garden
    public static final DeferredBlock<Block> GRAS = verf("guhriow1_gras", MapColor.GRASS, SoundType.GRASS);
    public static final DeferredBlock<Block> AARDE = verf("guhriow1_aarde", MapColor.DIRT, SoundType.GRAVEL);
    public static final DeferredBlock<Block> HEG = verf("guhriow1_heg", MapColor.PLANT, SoundType.AZALEA_LEAVES);
    public static final DeferredBlock<Block> WOLK = verf("guhriow1_wolk", MapColor.SNOW, SoundType.WOOL);
    // what the wall behind the lane is painted with
    public static final DeferredBlock<Block> LUCHT = verf("guhriow1_lucht", MapColor.COLOR_LIGHT_BLUE, SoundType.WOOL);
    public static final DeferredBlock<Block> LOOF = verf("guhriow1_loof", MapColor.COLOR_CYAN, SoundType.AZALEA_LEAVES);
    public static final DeferredBlock<Block> WOLK_VER = verf("guhriow1_wolk_ver", MapColor.COLOR_LIGHT_BLUE, SoundType.WOOL);
    public static final DeferredBlock<Block> WOLK_SNOET = verf("guhriow1_wolk_snoet", MapColor.COLOR_LIGHT_BLUE, SoundType.WOOL);
    public static final DeferredBlock<Block> HEUVEL = verf("guhriow1_heuvel", MapColor.COLOR_LIGHT_GREEN, SoundType.GRASS);
    public static final DeferredBlock<Block> HEUVEL_OGEN = verf("guhriow1_heuvel_ogen", MapColor.COLOR_LIGHT_GREEN, SoundType.GRASS);

    public static final DeferredBlock<GuhrioW1Blocks.Plantje> BLOEM = BLOCKS.registerBlock("guhriow1_bloem", GuhrioW1Blocks.Plantje::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak().noOcclusion().sound(SoundType.GRASS)
                    .isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<GuhrioW1Blocks.TipBlok> TIP = BLOCKS.registerBlock("guhriow1_tip", GuhrioW1Blocks.TipBlok::new,
            GuhrioW1Feature::onzichtbaar);
    public static final DeferredBlock<GuhrioW1Blocks.GeheimBlok> GEHEIM = BLOCKS.registerBlock("guhriow1_geheim", GuhrioW1Blocks.GeheimBlok::new,
            GuhrioW1Feature::onzichtbaar);

    public static final List<DeferredItem<BlockItem>> BLOK_ITEMS = List.of(
            ITEMS.registerSimpleBlockItem(GRAS), ITEMS.registerSimpleBlockItem(AARDE), ITEMS.registerSimpleBlockItem(HEG),
            ITEMS.registerSimpleBlockItem(WOLK), ITEMS.registerSimpleBlockItem(LUCHT), ITEMS.registerSimpleBlockItem(LOOF),
            ITEMS.registerSimpleBlockItem(WOLK_VER), ITEMS.registerSimpleBlockItem(WOLK_SNOET), ITEMS.registerSimpleBlockItem(HEUVEL),
            ITEMS.registerSimpleBlockItem(HEUVEL_OGEN), ITEMS.registerSimpleBlockItem(BLOEM), ITEMS.registerSimpleBlockItem(TIP),
            ITEMS.registerSimpleBlockItem(GEHEIM));

    /** A block of the painted garden: plain, soft, breaks by hand. */
    private static DeferredBlock<Block> verf(String id, MapColor kleur, SoundType geluid) {
        return BLOCKS.registerSimpleBlock(id, () -> BlockBehaviour.Properties.of().mapColor(kleur).strength(0.8f, 3f).sound(geluid));
    }

    /** An invisible piece of a level (like the spots of the engine's creatures): nothing can break or push it. */
    private static BlockBehaviour.Properties onzichtbaar() {
        return BlockBehaviour.Properties.of().noCollision().noLootTable().strength(-1f, 3600000f).noOcclusion()
                .isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        Binnentuin.registreer();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> item : BLOK_ITEMS) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private GuhrioW1Feature() {
    }
}
