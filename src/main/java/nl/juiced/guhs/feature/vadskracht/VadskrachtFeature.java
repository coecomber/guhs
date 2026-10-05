package nl.juiced.guhs.feature.vadskracht;

import java.util.function.Consumer;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhWheelPartBlock;
import nl.juiced.guhs.registry.ModBlockEntities;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * Vadskracht (bbq2, foundation F1): the power of the guh machines. Sources (the Guhrad...) give whole VK per second,
 * Guhdraad joins everything into a net, machines ask what they need, and when they ask more than there is the WHOLE net
 * stands still ({@link VadsKracht}, {@link VadsNet}). There is no meter: looking at any vadskracht block shows the state of
 * its net under the crosshair ({@link VadsUitlezing}, {@code client.VadsHover}).
 * <p>
 * For the machines of the tech slices this package has the base classes ({@link MachineBlock} + {@link MachineBlockEntity}
 * with the face {@link Snoet}, {@link Meerblok} + {@link MachineDeelBlock} for machines bigger than one block,
 * {@link BatterijBlock} + {@link BatterijBlockEntity}), the item and fluid helpers ({@link Kisten}, {@link Sauzen},
 * {@link SausTank}) and all numbers ({@link VadsGetallen}). It also holds what exists already: the Guhdraad
 * ({@code block.GuhWireBlock}), the Guhrad ({@code block.entity.GuhWheelBlockEntity}, {@link GuhradKracht}) and the Guhoven
 * ({@code feature.guhoven}). tools/features/vadskracht.py makes the resources (the tag, the texts, the face painter, the
 * machine and battery models, the Guhrad table).
 */
public final class VadskrachtFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    /** The ready-made part block of machines bigger than one block ({@link Meerblok}). */
    public static final DeferredBlock<MachineDeelBlock> MACHINE_DEEL = BLOCKS.registerBlock("machine_deel", MachineDeelBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.0f, 6f).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.BLOCK));

    // --- for game tests and dev worlds (no items, no creative tab) ---
    /** A source without a guh: {@code kracht * 10} VK, no cap per net ({@link TestbronBlock}). */
    public static final DeferredBlock<TestbronBlock> TESTBRON = BLOCKS.registerBlock("vadskracht_testbron", TestbronBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PINK).noLootTable());
    /** A battery of {@link VadsGetallen#BATTERIJ} VK ({@link BatterijBlock}). */
    public static final DeferredBlock<BatterijBlock> TESTBATTERIJ = BLOCKS.registerBlock("vadskracht_testbatterij",
            p -> new BatterijBlock(p, VadsGetallen.BATTERIJ, () -> VadskrachtFeature.TESTBATTERIJ_BE.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PINK).noLootTable());
    /** The smallest machine ({@link TestMachineBlock}), and one of 2 x 2 blocks. */
    public static final DeferredBlock<TestMachineBlock> TESTMACHINE = BLOCKS.registerBlock("vadskracht_testmachine",
            p -> new TestMachineBlock(p, 1, 1),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PINK).noLootTable());
    public static final DeferredBlock<TestMachineBlock> TESTMACHINE_GROOT = BLOCKS.registerBlock("vadskracht_testmachine_groot",
            p -> new TestMachineBlock(p, 2, 2),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PINK).noLootTable());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestbronBlock.Kern>> TESTBRON_BE = BLOCK_ENTITIES.register(
            "vadskracht_testbron", () -> new BlockEntityType<>(TestbronBlock.Kern::new, TESTBRON.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatterijBlockEntity>> TESTBATTERIJ_BE = BLOCK_ENTITIES.register(
            "vadskracht_testbatterij", () -> new BlockEntityType<>(
                    (pos, state) -> new BatterijBlockEntity(VadskrachtFeature.TESTBATTERIJ_BE.get(), pos, state), TESTBATTERIJ.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestMachineBlock.Kern>> TESTMACHINE_BE = BLOCK_ENTITIES.register(
            "vadskracht_testmachine", () -> new BlockEntityType<>(TestMachineBlock.Kern::new, TESTMACHINE.get(), TESTMACHINE_GROOT.get()));

    public static void register(IEventBus modBus) {
        NeoForgeMod.enableMilkFluid();   // minecraft:milk is a real fluid (the fourth fluid of the Guh-technologie, Sauzen.melk)
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(VadskrachtFeature::capabilities);
        // the nets: one tick per level, rebuilt around chunks that come and go, forgotten with their level
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                VadsNetten.van(level).tick();
            }
        });
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                VadsNetten.van(level).chunkErbij(event.getChunk().getPos());
            }
        });
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                VadsNetten.van(level).chunkWeg(event.getChunk().getPos());
            }
        });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> VadsNetten.vergeet(event.getLevel()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> VadsNetten.vergeetAlles());
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) -> event.addListener(Guhs.id("vadskracht_guhrad"), GuhradKracht.LADER));
        NeoForge.EVENT_BUS.addListener(VadskrachtFeature::commands);
    }

    public static void payloads(PayloadRegistrar registrar) {
        VadsPayloads.register(registrar);
    }

    /** Nothing: the test blocks have no items, and the Guhrad, the Guhdraad and the Guhoven are in the tab already. */
    public static void creative(Consumer<ItemStack> output) {
    }

    // =====================================================================================================================
    // capabilities
    // =====================================================================================================================

    private static void capabilities(RegisterCapabilitiesEvent event) {
        // the Guhrad: the wheel block's block entity is the source, its 8 invisible part blocks belong to it
        knoopCapability(event, ModBlockEntities.GUH_WHEEL.get());
        event.registerBlock(VadsKracht.KNOOP, (level, pos, state, be, context) -> {
            BlockPos wiel = GuhWheelPartBlock.wheelPos(state, pos);
            return level.isLoaded(wiel) && level.getBlockEntity(wiel) instanceof VadsKnoop knoop ? knoop : null;
        }, ModBlocks.GUH_WHEEL_PART.get());
        deelCapabilities(event, MACHINE_DEEL.get());
        knoopCapability(event, TESTBRON_BE.get());
        knoopCapability(event, TESTBATTERIJ_BE.get());
        machineCapabilities(event, TESTMACHINE_BE.get());
    }

    /** Registers the block entities of this type as vadskracht knopen (they must implement {@link VadsKnoop}). */
    public static void knoopCapability(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        event.registerBlockEntity(VadsKracht.KNOOP, type, (be, context) -> be instanceof VadsKnoop knoop ? knoop : null);
    }

    /**
     * Everything a machine's block entity type needs: it is a vadskracht knoop, and pipes and hoppers reach its inventory
     * ({@link MachineBlockEntity#handler}).
     */
    public static void machineCapabilities(RegisterCapabilitiesEvent event, BlockEntityType<? extends MachineBlockEntity> type) {
        event.registerBlockEntity(VadsKracht.KNOOP, type, (be, context) -> be);
        event.registerBlockEntity(Capabilities.Item.BLOCK, type, MachineBlockEntity::handler);
    }

    /**
     * The part blocks of machines bigger than one block answer with what their kern has: the vadskracht knoop, the items and
     * the fluids. (Done already for {@link #MACHINE_DEEL}; call it for a part block of your own.)
     */
    public static void deelCapabilities(RegisterCapabilitiesEvent event, Block... delen) {
        event.registerBlock(VadsKracht.KNOOP, (level, pos, state, be, context) -> {
            BlockPos kern = MachineDeelBlock.kern(state, pos);
            return level.isLoaded(kern) ? level.getCapability(VadsKracht.KNOOP, kern, null) : null;
        }, delen);
        event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, be, kant) -> {
            BlockPos kern = MachineDeelBlock.kern(state, pos);
            return level.isLoaded(kern) ? level.getCapability(Capabilities.Item.BLOCK, kern, kant) : null;
        }, delen);
        event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, be, kant) -> {
            BlockPos kern = MachineDeelBlock.kern(state, pos);
            return level.isLoaded(kern) ? level.getCapability(Capabilities.Fluid.BLOCK, kern, kant) : null;
        }, delen);
    }

    // =====================================================================================================================
    // op command (dev checks and AutoCheck scripts): /guhs vadskracht [x y z]
    // =====================================================================================================================

    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("vadskracht")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> lees(ctx.getSource(), null))
                .then(Commands.argument("plek", BlockPosArgument.blockPos())
                        .executes(ctx -> lees(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "plek"))))));
    }

    /** Says the hover readout of the block at this spot (or the block the player looks at) in chat. */
    private static int lees(CommandSourceStack source, @javax.annotation.Nullable BlockPos plek) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        BlockPos pos = plek;
        if (pos == null) {
            HitResult hit = source.getPlayerOrException().pick(8, 1f, false);
            if (!(hit instanceof BlockHitResult blok) || hit.getType() != HitResult.Type.BLOCK) {
                source.sendFailure(Component.translatable("commands.guhs.vadskracht.niets"));
                return 0;
            }
            pos = blok.getBlockPos();
        }
        VadsNet net = VadsKracht.net(level, pos);
        if (net == VadsNet.EMPTY) {
            source.sendFailure(Component.translatable("commands.guhs.vadskracht.niets"));
            return 0;
        }
        for (Component regel : VadsKracht.regels(level, pos)) {
            source.sendSuccess(() -> regel, false);
        }
        source.sendSuccess(() -> Component.translatable("commands.guhs.vadskracht.net", net.status().name(), net.grootte(), net.knopen().size(),
                VadsNetten.van(level).aantal()), false);
        return net.draait() ? 1 : 0;
    }

    private VadskrachtFeature() {
    }
}
