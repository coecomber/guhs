package nl.juiced.guhs.feature.techsaus;

import java.util.function.Consumer;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * bbq2 (tech-vloeistof): sauce through hoses. The Sauspomp lifts sauce out of a source block, the Sausslang carries it, the
 * Sausvat keeps it (tap it with a bucket), and three machines use it on vadskracht: the Brouwautomaat (the Guhbrouwketel that
 * brews by itself), the Frituurautomaat (the frying pan that fries by itself) and the Grillkoolpers (frituursaus + water =
 * grillkool). The sauces are the four of the Guh-technologie ({@code Sauzen.TECHNIEK}): kaassaus, kaasfrituursaus, water and
 * milk. How sauce moves: {@link Slangen}; every number: {@link SausGetallen}. The Guhbrouwketel and the frying pan you work by
 * hand are untouched.
 * <p>
 * Resources (models with a guh face, textures, texts, recipes, advancements, the test room): tools/features/tech_vloeistof.py.
 * The fields below are the fixed ids of CONTRACT_130 section 7.
 */
public final class TechsausFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Guhs.MODID);

    /** The sauce in a Sausvat that was broken: it stays on the item ({@code guhs:techsaus_inhoud}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> INHOUD = COMPONENTS.registerComponentType(
            "techsaus_inhoud", b -> b.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    /** A machine with a face: tough as iron, pink on the map, and its model is not a full cube. */
    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PINK).strength(3.0f, 6f).noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false).pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<SauspompBlock> SAUSPOMP = BLOCKS.registerBlock("sauspomp", SauspompBlock::new, TechsausFeature::machine);
    public static final DeferredItem<BlockItem> SAUSPOMP_ITEM = blokItem(SAUSPOMP);
    public static final DeferredBlock<SausslangBlock> SAUSSLANG = BLOCKS.registerBlock("sausslang", SausslangBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.6f, 3f).sound(SoundType.WOOL).noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));
    public static final DeferredItem<BlockItem> SAUSSLANG_ITEM = blokItem(SAUSSLANG);
    public static final DeferredBlock<SausvatBlock> SAUSVAT = BLOCKS.registerBlock("sausvat", SausvatBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).mapColor(MapColor.TERRACOTTA_ORANGE).strength(2.5f, 6f).noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> SAUSVAT_ITEM = blokItem(SAUSVAT);
    public static final DeferredBlock<BrouwautomaatBlock> BROUWAUTOMAAT = BLOCKS.registerBlock("brouwautomaat", BrouwautomaatBlock::new,
            TechsausFeature::machine);
    public static final DeferredItem<BlockItem> BROUWAUTOMAAT_ITEM = blokItem(BROUWAUTOMAAT);
    public static final DeferredBlock<FrituurautomaatBlock> FRITUURAUTOMAAT = BLOCKS.registerBlock("frituurautomaat", FrituurautomaatBlock::new,
            TechsausFeature::machine);
    public static final DeferredItem<BlockItem> FRITUURAUTOMAAT_ITEM = blokItem(FRITUURAUTOMAAT);
    public static final DeferredBlock<GrillkoolpersBlock> GRILLKOOLPERS = BLOCKS.registerBlock("grillkoolpers", GrillkoolpersBlock::new,
            TechsausFeature::machine);
    public static final DeferredItem<BlockItem> GRILLKOOLPERS_ITEM = blokItem(GRILLKOOLPERS);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SauspompBlockEntity>> SAUSPOMP_BE = BLOCK_ENTITIES.register("sauspomp",
            () -> new BlockEntityType<>(SauspompBlockEntity::new, SAUSPOMP.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SausvatBlockEntity>> SAUSVAT_BE = BLOCK_ENTITIES.register("sausvat",
            () -> new BlockEntityType<>(SausvatBlockEntity::new, SAUSVAT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrouwautomaatBlockEntity>> BROUWAUTOMAAT_BE = BLOCK_ENTITIES.register(
            "brouwautomaat", () -> new BlockEntityType<>(BrouwautomaatBlockEntity::new, BROUWAUTOMAAT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FrituurautomaatBlockEntity>> FRITUURAUTOMAAT_BE = BLOCK_ENTITIES.register(
            "frituurautomaat", () -> new BlockEntityType<>(FrituurautomaatBlockEntity::new, FRITUURAUTOMAAT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrillkoolpersBlockEntity>> GRILLKOOLPERS_BE = BLOCK_ENTITIES.register(
            "grillkoolpers", () -> new BlockEntityType<>(GrillkoolpersBlockEntity::new, GRILLKOOLPERS.get()));

    /** The item of a sauce block: its name is the block's, and it carries a grey line of what it is for. */
    private static DeferredItem<BlockItem> blokItem(DeferredBlock<? extends Block> block) {
        return ITEMS.registerItem(block.getId().getPath(), p -> new SausBlokItem(block.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener(TechsausFeature::capabilities);
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof net.minecraft.world.level.Level level) {
                Slangen.vergeet(level);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> Slangen.vergeet(null));
        NeoForge.EVENT_BUS.addListener(SausCommando::register);
    }

    /**
     * The four machines are vadskracht knopen with item slots for pipes, hoppers and chore guhs ({@code Kisten.van}); all five
     * blocks with a tank show it to hoses and pipes ({@code Sauzen.van}): a machine only lets sauce in, the pump only out, the
     * vat both. The upper block of the Grillkoolpers is a {@code machine_deel}: it answers with what its kern has.
     */
    private static void capabilities(RegisterCapabilitiesEvent event) {
        VadskrachtFeature.machineCapabilities(event, SAUSPOMP_BE.get());
        VadskrachtFeature.machineCapabilities(event, BROUWAUTOMAAT_BE.get());
        VadskrachtFeature.machineCapabilities(event, FRITUURAUTOMAAT_BE.get());
        VadskrachtFeature.machineCapabilities(event, GRILLKOOLPERS_BE.get());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, SAUSPOMP_BE.get(), SausMachineBlockEntity::sausHandler);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, BROUWAUTOMAAT_BE.get(), SausMachineBlockEntity::sausHandler);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, FRITUURAUTOMAAT_BE.get(), SausMachineBlockEntity::sausHandler);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, GRILLKOOLPERS_BE.get(), SausMachineBlockEntity::sausHandler);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, SAUSVAT_BE.get(), (vat, kant) -> vat.tank());
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(SAUSPOMP_ITEM.get()));
        output.accept(new ItemStack(SAUSSLANG_ITEM.get()));
        output.accept(new ItemStack(SAUSVAT_ITEM.get()));
        output.accept(new ItemStack(BROUWAUTOMAAT_ITEM.get()));
        output.accept(new ItemStack(FRITUURAUTOMAAT_ITEM.get()));
        output.accept(new ItemStack(GRILLKOOLPERS_ITEM.get()));
    }

    private TechsausFeature() {
    }
}
