package nl.juiced.guhs.feature.techmachine;

import java.util.function.Consumer;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * bbq2 (tech-machines): the guh machines that do the work for you. All of them run on vadskracht (feature/vadskracht),
 * have a snoet with the three faces (asleep without vadskracht, happy with it, surprised when they are stuck) and a little
 * animation of their own ({@code client.MachineRenderer}):
 * <ul>
 *   <li>{@link #OOGSTER}: cuts the ripe crops of the field in front of its snoet and plants them again ({@link Oogst}).</li>
 *   <li>{@link #KNABBELAAR}: gnaws away the block in front of it, like an iron pickaxe would, about one block per two
 *       seconds. Never containers or machines, never in a protected building or somebody else's huisje.</li>
 *   <li>{@link #NEERZETTER}: puts the blocks from its tummy down in front of it.</li>
 *   <li>{@link #VADSMOLEN}: the molentje on vadskracht, two blocks high: grinds four times as fast and more than graan
 *       ({@link Maalrecepten}, data).</li>
 *   <li>{@link #TEKENTAFEL} + {@link #BOUWTEKENING}: lay a recipe on the table and it is drawn on a Bouwtekening
 *       ({@link Bouwtekening}: the recipe in a data component, readable by anything: {@link Bouwtekeningen}).</li>
 *   <li>{@link #KNUTSELMACHINE}: crafts what the Bouwtekening in it shows, from what pipes and guhs bring.</li>
 *   <li>{@link #PLANTAGEBAK}: a bed of three by three blocks; any sapling or zwammetje in it is a real tree within a
 *       minute ({@link Plantagebakken} is what the chore slice calls).</li>
 * </ul>
 * Resources: tools/features/tech_machines.py (+ tech_machines_modellen.py, tech_machines_tekst.py).
 * The fields with a fixed id of CONTRACT_130 7 keep their names; the generic types are the real classes now.
 */
public final class TechmachineFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Guhs.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Guhs.MODID);

    /** Blocks the Knabbelaar never gnaws (next to everything with a block entity and every vadskracht block). */
    public static final TagKey<Block> KNABBELT_NIET = TagKey.create(Registries.BLOCK, Guhs.id("techmachine_knabbelt_niet"));

    private static BlockBehaviour.Properties machine(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(2.5f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                .pushReaction(PushReaction.BLOCK);
    }

    // --- the blocks (fixed ids) ---
    public static final DeferredBlock<OogsterBlock> OOGSTER = BLOCKS.registerBlock("oogster", OogsterBlock::new,
            () -> machine(MapColor.COLOR_LIGHT_GREEN).noOcclusion());
    public static final DeferredItem<BlockItem> OOGSTER_ITEM = ITEMS.registerSimpleBlockItem(OOGSTER);
    public static final DeferredBlock<KnabbelaarBlock> KNABBELAAR = BLOCKS.registerBlock("knabbelaar", KnabbelaarBlock::new,
            () -> machine(MapColor.COLOR_PINK).noOcclusion());
    public static final DeferredItem<BlockItem> KNABBELAAR_ITEM = ITEMS.registerSimpleBlockItem(KNABBELAAR);
    public static final DeferredBlock<NeerzetterBlock> NEERZETTER = BLOCKS.registerBlock("neerzetter", NeerzetterBlock::new,
            () -> machine(MapColor.COLOR_LIGHT_BLUE).noOcclusion());
    public static final DeferredItem<BlockItem> NEERZETTER_ITEM = ITEMS.registerSimpleBlockItem(NEERZETTER);
    public static final DeferredBlock<KnutselmachineBlock> KNUTSELMACHINE = BLOCKS.registerBlock("knutselmachine", KnutselmachineBlock::new,
            () -> machine(MapColor.COLOR_ORANGE).noOcclusion());
    public static final DeferredItem<BlockItem> KNUTSELMACHINE_ITEM = ITEMS.registerSimpleBlockItem(KNUTSELMACHINE);
    public static final DeferredBlock<TekentafelBlock> TEKENTAFEL = BLOCKS.registerBlock("tekentafel", TekentafelBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 3f).sound(SoundType.WOOD).noOcclusion()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> TEKENTAFEL_ITEM = ITEMS.registerSimpleBlockItem(TEKENTAFEL);
    public static final DeferredBlock<PlantagebakBlock> PLANTAGEBAK = BLOCKS.registerBlock("plantagebak", PlantagebakBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 3f).sound(SoundType.WOOD).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> PLANTAGEBAK_ITEM = ITEMS.registerSimpleBlockItem(PLANTAGEBAK);
    public static final DeferredBlock<VadsmolenBlock> VADSMOLEN = BLOCKS.registerBlock("vadsmolen", VadsmolenBlock::new,
            () -> machine(MapColor.SAND).noOcclusion());
    public static final DeferredItem<BlockItem> VADSMOLEN_ITEM = ITEMS.registerSimpleBlockItem(VADSMOLEN);
    public static final DeferredItem<BouwtekeningItem> BOUWTEKENING = ITEMS.registerItem("bouwtekening", BouwtekeningItem::new,
            () -> new net.minecraft.world.item.Item.Properties());

    /** The eight other blocks of a Plantagebak (no item: they come and go with the kern). */
    public static final DeferredBlock<PlantagebakDeelBlock> PLANTAGEBAK_DEEL = BLOCKS.registerBlock("techmachine_plantagebak_deel",
            PlantagebakDeelBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 3f).sound(SoundType.WOOD)
                    .noLootTable().pushReaction(PushReaction.BLOCK));

    // --- the recipe on a Bouwtekening ---
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Bouwtekening>> TEKENING = COMPONENTS.registerComponentType(
            "bouwtekening", b -> b.persistent(Bouwtekening.CODEC).networkSynchronized(Bouwtekening.STREAM_CODEC).cacheEncoding());

    // --- block entities ---
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OogsterBlockEntity>> OOGSTER_BE = BLOCK_ENTITIES.register("oogster",
            () -> new BlockEntityType<>(OogsterBlockEntity::new, OOGSTER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KnabbelaarBlockEntity>> KNABBELAAR_BE = BLOCK_ENTITIES.register("knabbelaar",
            () -> new BlockEntityType<>(KnabbelaarBlockEntity::new, KNABBELAAR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NeerzetterBlockEntity>> NEERZETTER_BE = BLOCK_ENTITIES.register("neerzetter",
            () -> new BlockEntityType<>(NeerzetterBlockEntity::new, NEERZETTER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KnutselmachineBlockEntity>> KNUTSELMACHINE_BE = BLOCK_ENTITIES.register(
            "knutselmachine", () -> new BlockEntityType<>(KnutselmachineBlockEntity::new, KNUTSELMACHINE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlantagebakBlockEntity>> PLANTAGEBAK_BE = BLOCK_ENTITIES.register(
            "plantagebak", () -> new BlockEntityType<>(PlantagebakBlockEntity::new, PLANTAGEBAK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VadsmolenBlockEntity>> VADSMOLEN_BE = BLOCK_ENTITIES.register("vadsmolen",
            () -> new BlockEntityType<>(VadsmolenBlockEntity::new, VADSMOLEN.get()));

    // --- menus ---
    /** The screen of every machine (which one: {@link MachineSoort}, sent along). */
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE_MENU = MENUS.register("techmachine_machine",
            () -> IMenuTypeExtension.create(MachineMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<TekentafelMenu>> TEKENTAFEL_MENU = MENUS.register("techmachine_tekentafel",
            () -> IMenuTypeExtension.create(TekentafelMenu::new));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener(TechmachineFeature::capabilities);
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) -> event.addListener(Guhs.id("techmachine_malen"), Maalrecepten.LADER));
        NeoForge.EVENT_BUS.addListener((BlockGrowFeatureEvent event) -> PlantagebakBlockEntity.groeit(event));
        NeoForge.EVENT_BUS.addListener(TechmachineFeature::commands);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        VadskrachtFeature.machineCapabilities(event, OOGSTER_BE.get());
        VadskrachtFeature.machineCapabilities(event, KNABBELAAR_BE.get());
        VadskrachtFeature.machineCapabilities(event, NEERZETTER_BE.get());
        VadskrachtFeature.machineCapabilities(event, KNUTSELMACHINE_BE.get());
        VadskrachtFeature.machineCapabilities(event, PLANTAGEBAK_BE.get());
        VadskrachtFeature.machineCapabilities(event, VADSMOLEN_BE.get());
        VadskrachtFeature.deelCapabilities(event, PLANTAGEBAK_DEEL.get());
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(VADSMOLEN_ITEM.get()));
        output.accept(new ItemStack(OOGSTER_ITEM.get()));
        output.accept(new ItemStack(KNABBELAAR_ITEM.get()));
        output.accept(new ItemStack(NEERZETTER_ITEM.get()));
        output.accept(new ItemStack(PLANTAGEBAK_ITEM.get()));
        output.accept(new ItemStack(TEKENTAFEL_ITEM.get()));
        output.accept(new ItemStack(BOUWTEKENING.get()));
        output.accept(new ItemStack(KNUTSELMACHINE_ITEM.get()));
    }

    // =====================================================================================================================
    // /guhs techmachine demo: a row of every machine with something to do, for the visual checks (dev runs only)
    // =====================================================================================================================

    private static void commands(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;   // it builds blocks in the world: never on a real server
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("techmachine")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("demo").executes(ctx -> demo(ctx.getSource())))));
    }

    /**
     * Builds the demo row south of the player on a floor of smooth stone: a test source, Guhdraad, and every machine facing
     * north (towards the player) with work in front of it. Returns the number of machines.
     */
    private static int demo(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        BlockPos hoek = player.blockPosition().offset(-12, 0, 4);
        for (BlockPos p : BlockPos.betweenClosed(hoek.offset(-1, -1, -3), hoek.offset(26, -1, 6))) {
            level.setBlockAndUpdate(p, Blocks.SMOOTH_STONE.defaultBlockState());
        }
        for (BlockPos p : BlockPos.betweenClosed(hoek.offset(-1, 0, -3), hoek.offset(26, 12, 6))) {
            level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        }
        // the wire along the back, the source at its west end (plenty: 15 x 10 VK)
        for (int x = 0; x <= 25; x++) {
            level.setBlockAndUpdate(hoek.offset(x, 0, 2), ModBlocks.GUH_WIRE.get().defaultBlockState());
        }
        level.setBlockAndUpdate(hoek.offset(-1, 0, 2), VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 15));
        Direction noord = Direction.NORTH;
        // the molen with graan
        zet(level, hoek.offset(1, 0, 1), VADSMOLEN.get(), noord);
        if (level.getBlockEntity(hoek.offset(1, 0, 1)) instanceof VadsmolenBlockEntity molen) {
            molen.vakken().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.BONE), 64);
        }
        // the Oogster with a field of ripe wheat
        zet(level, hoek.offset(5, 0, 1), OOGSTER.get(), noord);
        for (BlockPos p : BlockPos.betweenClosed(hoek.offset(3, -1, -3), hoek.offset(7, -1, 0))) {
            level.setBlockAndUpdate(p, Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE, 7));
            level.setBlockAndUpdate(p.above(), Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7));
        }
        // the Knabbelaar with a wall of stone to eat, the Neerzetter with a tummy full of planks
        zet(level, hoek.offset(10, 0, 1), KNABBELAAR.get(), noord);
        level.setBlockAndUpdate(hoek.offset(10, 0, 0), Blocks.COBBLESTONE.defaultBlockState());
        zet(level, hoek.offset(13, 0, 1), NEERZETTER.get(), noord);
        if (level.getBlockEntity(hoek.offset(13, 0, 1)) instanceof NeerzetterBlockEntity zetter) {
            zetter.vakken().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.OAK_PLANKS), 4);
        }
        // the Tekentafel and the Knutselmachine with a drawing of a chest and planks
        zet(level, hoek.offset(16, 0, 1), TEKENTAFEL.get(), noord);
        zet(level, hoek.offset(18, 0, 1), KNUTSELMACHINE.get(), noord);
        if (level.getBlockEntity(hoek.offset(18, 0, 1)) instanceof KnutselmachineBlockEntity knutsel) {
            java.util.List<ItemStack> rooster = new java.util.ArrayList<>();
            for (int i = 0; i < 9; i++) {
                rooster.add(i == 4 ? ItemStack.EMPTY : new ItemStack(Items.OAK_PLANKS));
            }
            ItemStack tekening = Bouwtekeningen.teken(level, rooster);
            if (!tekening.isEmpty()) {
                knutsel.vakken().set(KnutselmachineBlockEntity.TEKENING, net.neoforged.neoforge.transfer.item.ItemResource.of(tekening), 1);
                knutsel.vakken().set(KnutselmachineBlockEntity.VOORRAAD, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.OAK_PLANKS), 64);
            }
        }
        // the Plantagebak with an oak sapling
        zet(level, hoek.offset(22, 0, -1), PLANTAGEBAK.get(), noord);
        if (level.getBlockEntity(hoek.offset(22, 0, -1)) instanceof PlantagebakBlockEntity bak) {
            bak.plant(new ItemStack(Items.OAK_SAPLING, 4));
        }
        source.sendSuccess(() -> Component.literal("techmachine demo: " + hoek.toShortString()), false);
        return 7;
    }

    private static void zet(ServerLevel level, BlockPos pos, Block block, Direction facing) {
        BlockState state = block.defaultBlockState();
        level.setBlockAndUpdate(pos, state.hasProperty(MachineBlock.FACING) ? state.setValue(MachineBlock.FACING, facing) : state);
    }

    private TechmachineFeature() {
    }
}
