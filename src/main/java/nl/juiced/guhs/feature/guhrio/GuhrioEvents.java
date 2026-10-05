package nl.juiced.guhs.feature.guhrio;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;

/**
 * Super Guhrio's hooks into the game: the players' and the server's tick, everything that must end a level cleanly (log
 * out, death, another dimension; being taken away is noticed by the tick), no damage and no mounting in a level, and the
 * dev command (op 2):
 * <pre>
 * /guhs guhrio testlevel [pos]     build the test level: its start block there (default: two blocks in front of you),
 *                                  running the way you look
 * /guhs guhrio testhoek [pos]      the same for the little level with a corner in its lane
 * /guhs guhrio start [pos]         enter the level of the start block there (default: the nearest within 48 blocks)
 * /guhs guhrio stop                leave the level
 * /guhs guhrio terug               back to your flag
 * /guhs guhrio kracht &lt;0|1&gt;        take / give the Superknabbel
 * /guhs guhrio level &lt;pos&gt; &lt;id&gt;    tell the start block there which level it starts
 * /guhs guhrio info                where the server has you
 * </pre>
 */
public final class GuhrioEvents {
    /** The test levels' templates and level files (tools/features/guhrio.py). */
    public static final String TESTLEVEL = "guhrio_testlevel", TESTHOEK = "guhrio_testhoek";

    private GuhrioEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GuhrioSpel.tick(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        GuhrioSpel.serverTick(event.getServer());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GuhrioSpel.stop(player, GuhrioSpel.Einde.UITGELOGD);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GuhrioSpel.stop(player, GuhrioSpel.Einde.DIMENSIE);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GuhrioSpel.stop(player, GuhrioSpel.Einde.DOOD);
        }
    }

    /** Nobody is hurt in a level (only what gets through everything: /kill, the void far below). */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && GuhrioSpel.sessie(player) != null
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** No boats, carts or riding guhs in a lane (a mount that belongs to the level comes with the levels). */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityMounting() instanceof ServerPlayer player && GuhrioSpel.sessie(player) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        GuhrioSpel.vergeetAlles();
    }

    /** The data packs were read again: the level files too. */
    @SubscribeEvent
    public static void onDatapack(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            GuhrioLevel.vergeet();
        }
    }

    // =====================================================================================================================

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        var guhrio = Commands.literal("guhrio").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("testlevel").executes(c -> testlevel(c.getSource(), TESTLEVEL, null))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> testlevel(c.getSource(), TESTLEVEL, BlockPosArgument.getBlockPos(c, "pos")))))
                .then(Commands.literal("testhoek").executes(c -> testlevel(c.getSource(), TESTHOEK, null))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> testlevel(c.getSource(), TESTHOEK, BlockPosArgument.getBlockPos(c, "pos")))))
                .then(Commands.literal("start").executes(c -> start(c.getSource(), null))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> start(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos")))))
                .then(Commands.literal("stop").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    boolean bezig = GuhrioSpel.sessie(p) != null;
                    GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
                    return bezig ? 1 : 0;
                }))
                .then(Commands.literal("terug").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
                    if (s == null) {
                        return 0;
                    }
                    GuhrioSpel.terug(p, s, 0);
                    return 1;
                }))
                .then(Commands.literal("kracht").then(Commands.argument("n", IntegerArgumentType.integer(0, GuhrioSpel.Kracht.values().length - 1)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
                    if (s == null) {
                        return 0;
                    }
                    GuhrioSpel.zetKracht(p, s, GuhrioSpel.Kracht.values()[IntegerArgumentType.getInteger(c, "n")]);
                    return 1;
                })))
                .then(Commands.literal("level").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
                            if (!(c.getSource().getLevel().getBlockEntity(pos) instanceof GuhrioBlocks.StartBlockEntity be)) {
                                c.getSource().sendFailure(Component.literal("No Guhrio start block at " + pos.toShortString()));
                                return 0;
                            }
                            be.zetLevel(StringArgumentType.getString(c, "id"));
                            return 1;
                        }))))
                .then(Commands.literal("info").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
                    if (s == null) {
                        c.getSource().sendSuccess(() -> Component.literal("Guhrio: not in a level"), false);
                        return 0;
                    }
                    Baan.Plek plek = s.lane().plek(p.getX(), p.getZ());
                    String tekst = String.format(java.util.Locale.ROOT,
                            "Guhrio: level %s, lane %d (%s) s=%.2f y=%.2f off=%.3f, coins %d, time %s, power %s, flag %s, pieces %d, creatures %d",
                            s.level().level().id(), s.baan, s.lane().id, plek.s(), p.getY(), plek.naast(), s.munten, GuhrioSpel.tijd(s.ticks),
                            s.kracht, s.vlag, s.actief.stukken.size(), s.actief.wezens.size());
                    c.getSource().sendSuccess(() -> Component.literal(tekst), false);
                    return 1;
                }));
        event.getDispatcher().register(Commands.literal("guhs").then(guhrio));
    }

    private static int testlevel(CommandSourceStack source, String naam, @Nullable BlockPos pos) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Direction kant = p.getDirection();
        BlockPos anker = pos != null ? pos : p.blockPosition().relative(kant, 2);
        if (!plaatsTestlevel(source.getLevel(), naam, anker, kant)) {
            source.sendFailure(Component.literal("The template guhs:" + naam + " is missing (or has no start block)"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Guhrio test level built: the start block is at " + anker.toShortString()
                + " (running " + kant.getName() + "). Walk into it, or /guhs guhrio start"), true);
        return 1;
    }

    /**
     * Puts a test level's template in the world so that its start block lands on {@code anker} and the level runs
     * towards {@code kant} (the templates are built running east).
     */
    public static boolean plaatsTestlevel(ServerLevel level, String naam, BlockPos anker, Direction kant) {
        StructureTemplate template = level.getStructureManager().get(Guhs.id(naam)).orElse(null);
        if (template == null) {
            return false;
        }
        Rotation draai = switch (kant) {
            case SOUTH -> Rotation.CLOCKWISE_90;
            case WEST -> Rotation.CLOCKWISE_180;
            case NORTH -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(draai);
        List<StructureTemplate.StructureBlockInfo> start = template.filterBlocks(BlockPos.ZERO, settings, GuhrioFeature.STARTBLOK.get());
        if (start.isEmpty()) {
            return false;
        }
        BlockPos hoek = anker.subtract(start.get(0).pos());
        template.placeInWorld(level, hoek, hoek, settings, level.getRandom(), 2);
        return true;
    }

    private static int start(CommandSourceStack source, @Nullable BlockPos pos) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        if (pos == null) {
            double beste = Double.MAX_VALUE;
            for (BlockPos q : BlockPos.betweenClosed(p.blockPosition().offset(-48, -16, -48), p.blockPosition().offset(48, 16, 48))) {
                if (level.isLoaded(q) && level.getBlockState(q).getBlock() instanceof GuhrioBlocks.StartBlok && q.distSqr(p.blockPosition()) < beste) {
                    beste = q.distSqr(p.blockPosition());
                    pos = q.immutable();
                }
            }
        }
        if (pos == null || !GuhrioSpel.start(p, pos)) {
            source.sendFailure(Component.literal("No Guhrio level could be started" + (pos == null ? " (no start block within 48 blocks)" : " at " + pos.toShortString())));
            return 0;
        }
        return 1;
    }
}
