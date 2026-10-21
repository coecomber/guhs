package nl.juiced.guhs.feature.bouwhulp;

import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlock;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetPayloads;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetUpgradeItem;
import nl.juiced.guhs.feature.bouwhulp.keet.Bouwguhs;
import nl.juiced.guhs.feature.bouwhulp.keet.KeetTabs;
import nl.juiced.guhs.feature.bouwhulp.kern.KernHaken;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.feature.bouwhulp.kern.SpookPayloads;
import nl.juiced.guhs.feature.bouwhulp.kern.Spookbouw;
import nl.juiced.guhs.feature.bouwhulp.kern.Spoken;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * 1.6.0 "Op Safari!" deel 2: de Bouwhulp (design doc part 2, SAFARI_CONTRACT.md "Deel 2"). This is the kern (F8): the
 * spookbouw ({@code kern}: {@link Spookbouw}, {@link Ontwerp}, {@link Spook}, {@link Spoken},
 * {@link nl.juiced.guhs.feature.bouwhulp.kern.Materiaal}) and the Bouwkeet ({@code keet}: the block, its levels and
 * upgrades, the bouwguhs, the tabbed menu). F9 (tools, blueprints), F10 (helpers) and F11 (blocks) build on it.
 * Resources: tools/features/bouwhulp.py.
 */
public final class BouwhulpFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final DeferredBlock<BouwkeetBlock> BOUWKEET = BLOCKS.registerBlock("bouwkeet", BouwkeetBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2.5f, 6f).noOcclusion().sound(SoundType.WOOD)
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> BOUWKEET_ITEM = ITEMS.registerSimpleBlockItem(BOUWKEET);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BouwkeetBlockEntity>> BOUWKEET_BE = BLOCK_ENTITIES.register("bouwkeet",
            () -> new BlockEntityType<>(BouwkeetBlockEntity::new, BOUWKEET.get()));

    /** The four upgrades, level 1..4 (an own choice of names and recipes: BOUWHULP_KEUZES.md). */
    public static final DeferredItem<BouwkeetUpgradeItem> UPGRADE_GUHRAD = upgrade("bouwkeet_upgrade_guhrad", BouwNiveau.GUHRAD, Rarity.COMMON);
    public static final DeferredItem<BouwkeetUpgradeItem> UPGRADE_ZOUT = upgrade("bouwkeet_upgrade_zout", BouwNiveau.ZOUT, Rarity.UNCOMMON);
    public static final DeferredItem<BouwkeetUpgradeItem> UPGRADE_SAUS = upgrade("bouwkeet_upgrade_saus", BouwNiveau.SAUS, Rarity.RARE);
    public static final DeferredItem<BouwkeetUpgradeItem> UPGRADE_GLOEI = upgrade("bouwkeet_upgrade_gloei", BouwNiveau.GLOEI, Rarity.EPIC);

    private static DeferredItem<BouwkeetUpgradeItem> upgrade(String id, BouwNiveau naar, Rarity rarity) {
        return ITEMS.registerItem(id, p -> new BouwkeetUpgradeItem(p, naar), () -> new Item.Properties().stacksTo(16).rarity(rarity));
    }

    /** The upgrade item that brings a keet to this level. */
    public static Item upgrade(BouwNiveau naar) {
        return switch (naar) {
            case HANDWERK, GUHRAD -> UPGRADE_GUHRAD.get();
            case ZOUT -> UPGRADE_ZOUT.get();
            case SAUS -> UPGRADE_SAUS.get();
            case GLOEI -> UPGRADE_GLOEI.get();
        };
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> VadskrachtFeature.machineCapabilities(event, BOUWKEET_BE.get()));
        KeetTabs.register();
        Bouwguhs.register();
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                KernHaken.tick(level);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                SpookPayloads.stuurAlles(p);
                Spoken.van(p.level()).geefTegoed(p.level(), p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                SpookPayloads.stuurAlles(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (!event.getLevel().isClientSide() && KernHaken.oudeVlag(event.getEntity())) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(BouwhulpFeature::commands);
    }

    public static void payloads(PayloadRegistrar registrar) {
        SpookPayloads.register(registrar);
        BouwkeetPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BOUWKEET_ITEM.get()));
        output.accept(new ItemStack(UPGRADE_GUHRAD.get()));
        output.accept(new ItemStack(UPGRADE_ZOUT.get()));
        output.accept(new ItemStack(UPGRADE_SAUS.get()));
        output.accept(new ItemStack(UPGRADE_GLOEI.get()));
    }

    /** A player's name, also when offline (the server's name cache), or a piece of the id. */
    public static String naamVan(MinecraftServer server, UUID id) {
        return server.services().nameToIdCache().get(id).map(net.minecraft.server.players.NameAndId::name).orElse(id.toString().substring(0, 8));
    }

    // =====================================================================================================================
    // op commands (dev checks): /guhs bouwhulp proef | niveau <0..4> | terug
    // =====================================================================================================================

    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bouwhulp")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("proef").executes(ctx -> proef(ctx.getSource())))
                .then(Commands.literal("terug").executes(ctx -> Spookbouw.ongedaan(ctx.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("niveau").then(Commands.argument("n", IntegerArgumentType.integer(0, 4))
                        .executes(ctx -> niveau(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "n")))))));
    }

    /** A test ghost (the proefhuisje) a few blocks in front of the player, with its control panel. */
    private static int proef(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        BlockPos voor = p.blockPosition().relative(p.getDirection(), 4).offset(-2, 0, -2);
        Spook s = Spookbouw.start(p, KeetTabs.proefhuisje(), voor, Rotation.NONE, Mirror.NONE);
        if (s == null) {
            return 0;
        }
        Spookbouw.zetNaam(p.level(), s, Component.translatable("gui.guhs.bouwkeet.proefhuisje").getString());
        Spookbouw.toonBediening(p, s);
        return 1;
    }

    /** Sets the level of the keet the player looks at. */
    private static int niveau(CommandSourceStack source, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        HitResult hit = p.pick(8, 1f, false);
        if (!(hit instanceof BlockHitResult b) || hit.getType() != HitResult.Type.BLOCK) {
            return 0;
        }
        BlockPos pos = b.getBlockPos();
        if (p.level().getBlockState(pos).getBlock() instanceof nl.juiced.guhs.feature.vadskracht.MachineDeelBlock) {
            pos = nl.juiced.guhs.feature.vadskracht.MachineDeelBlock.kern(p.level().getBlockState(pos), pos);
        }
        if (!(p.level().getBlockEntity(pos) instanceof BouwkeetBlockEntity keet)) {
            source.sendFailure(Component.translatable("gui.guhs.bouwkeet.commando.geen"));
            return 0;
        }
        keet.zetNiveau(n);
        source.sendSuccess(() -> Component.translatable("gui.guhs.bouwkeet.commando.niveau", n), false);
        return 1;
    }

    private BouwhulpFeature() {
    }
}
