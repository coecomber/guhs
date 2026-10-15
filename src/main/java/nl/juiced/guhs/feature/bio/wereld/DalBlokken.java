package nl.juiced.guhs.feature.bio.wereld;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhDecoBlocks;

/**
 * biomes3 wereld, the Klaterdal: the few blocks and sounds that are the biome's own (resources: tools/features/bio_wereld_dal.py).
 * <ul>
 *   <li>{@code klaterdal_mos} and {@code klaterdal_mos_tapijt}: the valley's soft ground cover, a pale mint moss with pink
 *       flecks (vanilla's bright moss green clashed with the pink ground; this one sits in the family of the guh-bamboe);</li>
 *   <li>{@code klaterdal_riet}: the reed of the river banks, cream stalks with pink plumes;</li>
 *   <li>{@code klaterdal_bonsaiblad}: the pale leaf pads of the crooked little trees on the rocks;</li>
 *   <li>sounds {@code klaterdal.muziek} (the koto piece), {@code klaterdal.sfeer} (the babbling river, a loop) and
 *       {@code klaterdal.windgong} (a wind chime now and then); the biome file names them, only the client plays them.</li>
 * </ul>
 */
public final class DalBlokken {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredBlock<Block> MOS = BLOCKS.registerBlock("klaterdal_mos", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK).mapColor(MapColor.COLOR_LIGHT_GREEN));
    public static final DeferredBlock<CarpetBlock> MOS_TAPIJT = BLOCKS.registerBlock("klaterdal_mos_tapijt", CarpetBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_CARPET).mapColor(MapColor.COLOR_LIGHT_GREEN));
    public static final DeferredBlock<GuhDecoBlocks.RozeGras> RIET = BLOCKS.registerBlock("klaterdal_riet", GuhDecoBlocks.RozeGras::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<Bonsaiblad> BONSAIBLAD = BLOCKS.registerBlock("klaterdal_bonsaiblad", Bonsaiblad::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AZALEA_LEAVES).mapColor(MapColor.COLOR_LIGHT_GREEN));

    public static final DeferredItem<BlockItem> MOS_ITEM = ITEMS.registerSimpleBlockItem(MOS);
    public static final DeferredItem<BlockItem> MOS_TAPIJT_ITEM = ITEMS.registerSimpleBlockItem(MOS_TAPIJT);
    public static final DeferredItem<BlockItem> RIET_ITEM = ITEMS.registerSimpleBlockItem(RIET);
    public static final DeferredItem<BlockItem> BONSAIBLAD_ITEM = ITEMS.registerSimpleBlockItem(BONSAIBLAD);

    public static final DeferredHolder<SoundEvent, SoundEvent> MUZIEK = sound("klaterdal.muziek");
    public static final DeferredHolder<SoundEvent, SoundEvent> SFEER = sound("klaterdal.sfeer");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINDGONG = sound("klaterdal.windgong");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    /** Leaves that drop nothing from the air (the pads of a bonsai are too small for falling leaves). */
    public static class Bonsaiblad extends LeavesBlock {
        public static final MapCodec<Bonsaiblad> CODEC = simpleCodec(Bonsaiblad::new);

        public Bonsaiblad(Properties properties) {
            super(0f, properties);
        }

        @Override
        public MapCodec<Bonsaiblad> codec() {
            return CODEC;
        }

        @Override
        protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        }
    }

    /** Every block of the Klaterdal's own (the tests walk it). */
    public static List<DeferredBlock<? extends Block>> blokken() {
        return List.of(MOS, MOS_TAPIJT, RIET, BONSAIBLAD);
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(DalCommando::registreer);
        DalCommando.zelftest();
    }

    static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> item : List.of(MOS_ITEM, MOS_TAPIJT_ITEM, RIET_ITEM, BONSAIBLAD_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private DalBlokken() {
    }
}
