package nl.juiced.guhs.feature.techbuis;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
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
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * bbq2 (tech-buizen): the Knabbelbuizen and what belongs to them.
 * <ul>
 *   <li><b>Knabbelbuis</b> ({@link BuisBlock}): a see-through tube; no block entity, never ticks.</li>
 *   <li><b>Richtingstuk</b> ({@link RichtingBlock}) and <b>Filterstuk</b> ({@link FilterBlock}, needs vadskracht): the pieces
 *       that take items out of what is behind them (any item capability: a chest, a machine, the Bank Guh, a Hapluikje...)
 *       and send them through the tubes to where they fit ({@link BuisStukBlockEntity}, {@link BuisRoutes}). The items you
 *       see rolling are drawn by the client ({@code client.BuisRitten}); on the server an item on its way is one line in
 *       the piece that sent it.</li>
 *   <li><b>Opzuiger</b> ({@link OpzuigerBlock}): slurps up loose items.</li>
 *   <li>The sensors ({@link SensorBlock}): <b>Voorraadmeter</b> (how much is in a chest or bank), <b>Snuffelsensor</b> (guhs,
 *       Mika's, players nearby), <b>Guhklok</b> (a pulse every so often, or day / night) and <b>Guhteller</b> (every N-th
 *       pulse). They give a redstone signal; a redstone signal locks a Richtingstuk or Filterstuk.</li>
 * </ul>
 * The rules and numbers of the tubes are in {@link Buizen}. Resources: tools/features/tech_buizen.py. The eight block
 * fields are the fixed ids of CONTRACT_130 7.
 */
public final class TechbuisFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Guhs.MODID);

    private static BlockBehaviour.Properties glas(float sterkte) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(sterkte, 3f).sound(SoundType.GLASS).noOcclusion()
                .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)
                .isRedstoneConductor((state, level, pos) -> false).pushReaction(PushReaction.BLOCK);
    }

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PINK).strength(3f, 6f);
    }

    /** A sensor gives a redstone signal itself: like a redstone block it does not pass on what powers it. */
    private static BlockBehaviour.Properties sensor() {
        return machine().isRedstoneConductor((state, level, pos) -> false);
    }

    // --- the tubes ---
    public static final DeferredBlock<BuisBlock> KNABBELBUIS = BLOCKS.registerBlock("knabbelbuis", BuisBlock::new, () -> glas(0.5f));
    public static final DeferredItem<BlockItem> KNABBELBUIS_ITEM = item("knabbelbuis", KNABBELBUIS);
    public static final DeferredBlock<FilterBlock> KNABBELBUIS_FILTER = BLOCKS.registerBlock("knabbelbuis_filter", FilterBlock::new, () -> glas(0.8f));
    public static final DeferredItem<BlockItem> KNABBELBUIS_FILTER_ITEM = item("knabbelbuis_filter", KNABBELBUIS_FILTER);
    public static final DeferredBlock<RichtingBlock> KNABBELBUIS_RICHTING = BLOCKS.registerBlock("knabbelbuis_richting", RichtingBlock::new, () -> glas(0.8f));
    public static final DeferredItem<BlockItem> KNABBELBUIS_RICHTING_ITEM = item("knabbelbuis_richting", KNABBELBUIS_RICHTING);

    // --- the Opzuiger ---
    public static final DeferredBlock<OpzuigerBlock> OPZUIGER = BLOCKS.registerBlock("opzuiger", OpzuigerBlock::new, TechbuisFeature::machine);
    public static final DeferredItem<BlockItem> OPZUIGER_ITEM = item("opzuiger", OPZUIGER);

    // --- the sensors ---
    public static final DeferredBlock<SensorBlock> VOORRAADMETER = BLOCKS.registerBlock("voorraadmeter",
            p -> new SensorBlock(p, VoorraadmeterBlockEntity::new, false), TechbuisFeature::sensor);
    public static final DeferredItem<BlockItem> VOORRAADMETER_ITEM = item("voorraadmeter", VOORRAADMETER);
    public static final DeferredBlock<SensorBlock> SNUFFELSENSOR = BLOCKS.registerBlock("snuffelsensor",
            p -> new SensorBlock(p, SnuffelsensorBlockEntity::new, false), TechbuisFeature::sensor);
    public static final DeferredItem<BlockItem> SNUFFELSENSOR_ITEM = item("snuffelsensor", SNUFFELSENSOR);
    public static final DeferredBlock<SensorBlock> GUHKLOK = BLOCKS.registerBlock("guhklok",
            p -> new SensorBlock(p, GuhklokBlockEntity::new, false), TechbuisFeature::sensor);
    public static final DeferredItem<BlockItem> GUHKLOK_ITEM = item("guhklok", GUHKLOK);
    public static final DeferredBlock<SensorBlock> GUHTELLER = BLOCKS.registerBlock("guhteller",
            p -> new SensorBlock(p, GuhtellerBlockEntity::new, true), TechbuisFeature::sensor);
    public static final DeferredItem<BlockItem> GUHTELLER_ITEM = item("guhteller", GUHTELLER);

    // --- block entities ---
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BuisStukBlockEntity>> RICHTING_BE = BLOCK_ENTITIES.register(
            "knabbelbuis_richting", () -> new BlockEntityType<>(
                    (pos, state) -> new BuisStukBlockEntity(TechbuisFeature.RICHTING_BE.get(), pos, state), KNABBELBUIS_RICHTING.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FilterBlockEntity>> FILTER_BE = BLOCK_ENTITIES.register(
            "knabbelbuis_filter", () -> new BlockEntityType<>(FilterBlockEntity::new, KNABBELBUIS_FILTER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OpzuigerBlockEntity>> OPZUIGER_BE = BLOCK_ENTITIES.register(
            "opzuiger", () -> new BlockEntityType<>(OpzuigerBlockEntity::new, OPZUIGER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VoorraadmeterBlockEntity>> VOORRAADMETER_BE = BLOCK_ENTITIES.register(
            "voorraadmeter", () -> new BlockEntityType<>(VoorraadmeterBlockEntity::new, VOORRAADMETER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SnuffelsensorBlockEntity>> SNUFFELSENSOR_BE = BLOCK_ENTITIES.register(
            "snuffelsensor", () -> new BlockEntityType<>(SnuffelsensorBlockEntity::new, SNUFFELSENSOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuhklokBlockEntity>> GUHKLOK_BE = BLOCK_ENTITIES.register(
            "guhklok", () -> new BlockEntityType<>(GuhklokBlockEntity::new, GUHKLOK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuhtellerBlockEntity>> GUHTELLER_BE = BLOCK_ENTITIES.register(
            "guhteller", () -> new BlockEntityType<>(GuhtellerBlockEntity::new, GUHTELLER.get()));

    // --- menus ---
    public static final DeferredHolder<MenuType<?>, MenuType<FilterMenu>> FILTER_MENU = MENUS.register("knabbelbuis_filter",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new FilterMenu(TechbuisFeature.FILTER_MENU.get(), id, inventory, FilterMenu.VAKKEN_FILTER)));
    public static final DeferredHolder<MenuType<?>, MenuType<FilterMenu>> VOORRAADMETER_MENU = MENUS.register("voorraadmeter",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new FilterMenu(TechbuisFeature.VOORRAADMETER_MENU.get(), id, inventory, FilterMenu.VAKKEN_METER)));
    public static final DeferredHolder<MenuType<?>, MenuType<OpzuigerMenu>> OPZUIGER_MENU = MENUS.register("opzuiger",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new OpzuigerMenu(id, inventory)));

    /** A block item with the line {@code block.guhs.<id>.lore} under its name. */
    private static DeferredItem<BlockItem> item(String id, Supplier<? extends Block> block) {
        return ITEMS.registerItem(id, p -> new BlockItem(block.get(), p) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                tooltip.accept(Component.translatable("block.guhs." + id + ".lore").withStyle(ChatFormatting.GRAY));
            }
        }, () -> new Item.Properties());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(TechbuisFeature::capabilities);
        NeoForge.EVENT_BUS.addListener(TechbuisFeature::commands);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        // a hopper may push into the back of a piece; the tubes themselves hold nothing
        event.registerBlockEntity(Capabilities.Item.BLOCK, RICHTING_BE.get(), BuisStukBlockEntity::handler);
        event.registerBlockEntity(Capabilities.Item.BLOCK, FILTER_BE.get(), BuisStukBlockEntity::handler);
        VadskrachtFeature.knoopCapability(event, FILTER_BE.get());
        VadskrachtFeature.machineCapabilities(event, OPZUIGER_BE.get());
        // the sensors have no slots: only the vadskracht
        VadskrachtFeature.knoopCapability(event, VOORRAADMETER_BE.get());
        VadskrachtFeature.knoopCapability(event, SNUFFELSENSOR_BE.get());
        VadskrachtFeature.knoopCapability(event, GUHKLOK_BE.get());
        VadskrachtFeature.knoopCapability(event, GUHTELLER_BE.get());
    }

    /**
     * Op command (dev checks and AutoCheck scripts): {@code /guhs techbuis <x y z>} says what the Richtingstuk or Filterstuk
     * there is doing and every place it can send to. Returns the number of places.
     */
    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("techbuis")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("plek", BlockPosArgument.blockPos()).executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "plek");
                    if (!(source.getLevel().getBlockEntity(pos) instanceof BuisStukBlockEntity stuk)) {
                        source.sendFailure(Component.translatable("gui.guhs.techbuis.commando.geen_stuk"));
                        return 0;
                    }
                    source.sendSuccess(stuk::stand, false);
                    for (BuisRoutes.Route route : stuk.routes()) {
                        source.sendSuccess(() -> Component.translatable("gui.guhs.techbuis.commando.plek",
                                source.getLevel().getBlockState(route.doel()).getBlock().getName(), route.doel().toShortString(),
                                route.kant().getName(), route.lengte(), route.stukken().size()), false);
                    }
                    return stuk.routes().size();
                }))));
    }

    public static void payloads(PayloadRegistrar registrar) {
        BuisPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KNABBELBUIS_ITEM.get()));
        output.accept(new ItemStack(KNABBELBUIS_RICHTING_ITEM.get()));
        output.accept(new ItemStack(KNABBELBUIS_FILTER_ITEM.get()));
        output.accept(new ItemStack(OPZUIGER_ITEM.get()));
        output.accept(new ItemStack(VOORRAADMETER_ITEM.get()));
        output.accept(new ItemStack(SNUFFELSENSOR_ITEM.get()));
        output.accept(new ItemStack(GUHKLOK_ITEM.get()));
        output.accept(new ItemStack(GUHTELLER_ITEM.get()));
    }

    private TechbuisFeature() {
    }
}
