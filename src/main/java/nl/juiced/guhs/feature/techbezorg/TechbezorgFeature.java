package nl.juiced.guhs.feature.techbezorg;

import java.util.function.Consumer;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
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
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModItems;

/**
 * bbq2 (tech-bezorg): the Bezorgguhtje and its delivery round.
 * <ul>
 *   <li>{@link BezorgguhtjeEntity the Bezorgguhtje} ({@code guhs:bezorgguhtje}): a mini-guh on a step with a far too big
 *       backpack; its own model and animations; a counting Guhdex page ({@code GuhVariant.BEZORGGUHTJE}).</li>
 *   <li>{@link StepstationBlock the Stepstation} ({@code guhs:stepstation}): a guh machine (vadskracht, a face) where one
 *       guhtje lives. It keeps the backpack ({@link Bezorgnet#RUGZAK} stacks) and the round, makes its guhtje, and makes
 *       a new one when it is really gone: nothing is ever lost with a guhtje.</li>
 *   <li>{@link HaltepaaltjeBlock the Haltepaaltje} ({@code guhs:haltepaaltje}): a stop next to a chest or machine, within
 *       {@link Bezorgnet#BEREIK} blocks of its station, at most {@link Bezorgnet#MAX_HALTES} per station: ophalen or
 *       afleveren, with a filter. Items go through the item capability ({@code vadskracht.Kisten}), so chests, machines,
 *       the Bank Guh and the Hapluikje all work.</li>
 *   <li>{@link FluitjeItem the Bezorgguhtje-fluitje} ({@code guhs:bezorgguhtje_fluitje}): calls your guhtje to you with its
 *       backpack (sneaking: all of yours ride home). The reward of the Rookguh-vuurtoren (slice toren-peper gives it).</li>
 * </ul>
 * Everything only works in loaded chunks ({@link Bezorgnet}). Resources: tools/features/tech_bezorg.py (the guhtje's looks:
 * tech_bezorg_modellen.py). Op command for checks: {@code /guhs techbezorg} (and, in dev runs, {@code proef}).
 * The public fields STEPSTATION, HALTEPAALTJE, BEZORGGUHTJE_FLUITJE and BEZORGGUHTJE are the fixed ids of CONTRACT_130 7.
 */
public final class TechbezorgFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- the Stepstation ---
    public static final DeferredBlock<StepstationBlock> STEPSTATION = BLOCKS.registerBlock("stepstation", StepstationBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_MAGENTA).strength(2.0f, 6.0f));
    public static final DeferredItem<BlockItem> STEPSTATION_ITEM = ITEMS.registerItem("stepstation",
            p -> new LoreBlockItem(STEPSTATION.get(), p, "block.guhs.stepstation.lore"), () -> new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StepstationBlockEntity>> STEPSTATION_BE = BLOCK_ENTITIES.register("stepstation",
            () -> new BlockEntityType<>(StepstationBlockEntity::new, STEPSTATION.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<StepstationMenu>> STEPSTATION_MENU = MENUS.register("techbezorg_stepstation",
            () -> IMenuTypeExtension.create(StepstationMenu::new));

    // --- the Haltepaaltje ---
    public static final DeferredBlock<HaltepaaltjeBlock> HALTEPAALTJE = BLOCKS.registerBlock("haltepaaltje", HaltepaaltjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f, 3.0f).sound(SoundType.LANTERN).noCollision()
                    .noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> HALTEPAALTJE_ITEM = ITEMS.registerItem("haltepaaltje",
            p -> new LoreBlockItem(HALTEPAALTJE.get(), p, "block.guhs.haltepaaltje.lore"), () -> new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HaltepaaltjeBlockEntity>> HALTEPAALTJE_BE = BLOCK_ENTITIES.register("haltepaaltje",
            () -> new BlockEntityType<>(HaltepaaltjeBlockEntity::new, HALTEPAALTJE.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<HalteMenu>> HALTE_MENU = MENUS.register("techbezorg_halte",
            () -> IMenuTypeExtension.create(HalteMenu::new));

    // --- the whistle, the guhtje ---
    public static final DeferredItem<FluitjeItem> BEZORGGUHTJE_FLUITJE = ITEMS.registerItem("bezorgguhtje_fluitje", FluitjeItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredHolder<EntityType<?>, EntityType<BezorgguhtjeEntity>> BEZORGGUHTJE = ENTITY_TYPES.register("bezorgguhtje",
            () -> EntityType.Builder.of(BezorgguhtjeEntity::new, MobCategory.MISC).sized(0.55f, 0.8f).eyeHeight(0.6f).fireImmune()
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("bezorgguhtje"))));
    public static final DeferredItem<SpawnEggItem> BEZORGGUHTJE_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "bezorgguhtje_spawn_egg", BEZORGGUHTJE);

    // --- sounds (vanilla sounds, pitched, in sounds.json) ---
    public static final DeferredHolder<SoundEvent, SoundEvent> BEL = sound("techbezorg.bel");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLUITJE = sound("techbezorg.fluitje");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOP = sound("techbezorg.hop");
    public static final DeferredHolder<SoundEvent, SoundEvent> RITS = sound("techbezorg.rits");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUHTJE = sound("techbezorg.guhtje");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** A block item with one grey line under its name (lang key given). */
    private static final class LoreBlockItem extends BlockItem {
        private final String sleutel;

        LoreBlockItem(Block block, Properties p, String sleutel) {
            super(block, p);
            this.sleutel = sleutel;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(sleutel).withStyle(ChatFormatting.GRAY));
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(BEZORGGUHTJE.get(), BezorgguhtjeEntity.createAttributes().build()));
        // the station is a vadskracht knoop; nothing reaches its backpack through the item capability (no pipes into the bag)
        modBus.addListener((RegisterCapabilitiesEvent event) -> VadskrachtFeature.knoopCapability(event, STEPSTATION_BE.get()));
        GuhDex.creaturePage(GuhVariant.BEZORGGUHTJE, BEZORGGUHTJE);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> Bezorgnet.vergeetAlles());
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                Bezorgnet.vergeet(level.dimension());
            }
        });
        NeoForge.EVENT_BUS.addListener(TechbezorgFeature::commands);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(STEPSTATION_ITEM.get()));
        output.accept(new ItemStack(HALTEPAALTJE_ITEM.get()));
        output.accept(new ItemStack(BEZORGGUHTJE_FLUITJE.get()));
        output.accept(new ItemStack(BEZORGGUHTJE_SPAWN_EGG.get()));
    }

    // =====================================================================================================================
    // op command (dev checks and AutoCheck scripts): /guhs techbezorg [proef | fluit | huis]
    // =====================================================================================================================

    private static void commands(RegisterCommandsEvent event) {
        var techbezorg = Commands.literal("techbezorg").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> stand(ctx.getSource()))
                .then(Commands.literal("fluit").executes(ctx -> FluitjeItem.fluit(ctx.getSource().getPlayerOrException(), false)))
                .then(Commands.literal("huis").executes(ctx -> FluitjeItem.fluit(ctx.getSource().getPlayerOrException(), true)));
        if (!FMLEnvironment.isProduction()) {
            // (dev runs only: it puts blocks in the world)
            techbezorg.then(Commands.literal("proef").executes(ctx -> proef(ctx.getSource())));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(techbezorg));
    }

    /** Says what every loaded Stepstation within reach of the player is doing. Returns how many there are. */
    private static int stand(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer speler = source.getPlayerOrException();
        var stations = Bezorgnet.stations(speler.level(), speler.blockPosition());
        for (StepstationBlockEntity station : stations) {
            BlockPos p = station.getBlockPos();
            source.sendSuccess(() -> Component.literal(p.getX() + " " + p.getY() + " " + p.getZ() + ": ").append(station.standTekst())
                    .append(Component.literal(" [" + station.fase() + ", haltes " + station.haltes().size() + ", rugzak " + station.gevuld()
                            + "/" + Bezorgnet.RUGZAK + ", gebracht " + station.gebracht() + (station.heeftKracht() ? "" : ", geen vadskracht") + "]")), false);
        }
        if (stations.isEmpty()) {
            source.sendFailure(Component.literal("Geen Stepstation binnen " + Bezorgnet.BEREIK + " blokken"));
        }
        return stations.size();
    }

    /**
     * Dev runs only: a small working round in front of the player (only where there is air): a test source, a Stepstation,
     * a chest with kaasknabbels and cobblestone behind an "ophalen" pole, and an empty chest behind an "afleveren" pole
     * whose filter only lets the kaasknabbels through.
     */
    private static int proef(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer speler = source.getPlayerOrException();
        ServerLevel level = speler.level();
        Direction voor = speler.getDirection();
        Direction rechts = voor.getClockWise();
        BlockPos station = speler.blockPosition().relative(voor, 3);
        BlockPos bron = station.relative(rechts.getOpposite());
        BlockPos kistA = station.relative(rechts, 5), paalA = kistA.relative(voor.getOpposite());
        BlockPos kistB = station.relative(voor, 6).relative(rechts, 2), paalB = kistB.relative(voor.getOpposite());
        for (BlockPos plek : new BlockPos[] {station, bron, kistA, paalA, kistB, paalB}) {
            if (!level.getBlockState(plek).isAir()) {
                source.sendFailure(Component.literal("Geen ruimte op " + plek.toShortString()));
                return 0;
            }
        }
        level.setBlockAndUpdate(bron, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 1));
        level.setBlockAndUpdate(station, STEPSTATION.get().defaultBlockState().setValue(MachineBlock.FACING, voor.getOpposite()));
        level.setBlockAndUpdate(kistA, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(kistB, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(kistA) instanceof net.minecraft.world.Container kist) {
            kist.setItem(0, new ItemStack(ModItems.KAAS_KNABBELS.get(), 64));
            kist.setItem(1, new ItemStack(Blocks.COBBLESTONE, 64));
        }
        BlockState paal = HALTEPAALTJE.get().defaultBlockState().setValue(HaltepaaltjeBlock.FACING, voor);
        level.setBlockAndUpdate(paalA, paal.setValue(HaltepaaltjeBlock.OPHALEN, true));
        level.setBlockAndUpdate(paalB, paal.setValue(HaltepaaltjeBlock.OPHALEN, false));
        if (level.getBlockEntity(station) instanceof StepstationBlockEntity s) {
            s.zetEigenaar(speler.getUUID());
            for (BlockPos p : new BlockPos[] {paalA, paalB}) {
                if (level.getBlockEntity(p) instanceof HaltepaaltjeBlockEntity h) {
                    s.koppel(h);
                }
            }
        }
        if (level.getBlockEntity(paalB) instanceof HaltepaaltjeBlockEntity h) {
            h.zetFilter(0, new ItemStack(ModItems.KAAS_KNABBELS.get()));
        }
        source.sendSuccess(() -> Component.literal("Proefronde gebouwd: station " + station.toShortString()), false);
        return 1;
    }

    private TechbezorgFeature() {
    }
}
