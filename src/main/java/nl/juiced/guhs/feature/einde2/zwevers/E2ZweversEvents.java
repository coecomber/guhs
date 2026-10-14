package nl.juiced.guhs.feature.einde2.zwevers;

import java.util.List;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Game events of the zwevers slice: the three top-ups (swarms, element animals, the whale), the soft landing after a
 * bounce on a Kruimelkwal, the player's Walviszicht flag for its client at login, and the op commands for testing and the
 * autocheck ({@code /guhs einde2 zwevers ...}).
 */
public final class E2ZweversEvents {
    private E2ZweversEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Kruimelkwallen.onLevelTick(event);
        Elementdieren.onLevelTick(event);
        Leegtewalvissen.onLevelTick(event);
    }

    /** Whoever bounced on a Kruimelkwal lands softly the next time (within {@link KruimelkwalEntity#ZACHT_LANDEN} ticks). */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity e = event.getEntity();
        long stuiter = e.getPersistentData().getLongOr(KruimelkwalEntity.GESTUITERD, Long.MIN_VALUE);
        if (stuiter == Long.MIN_VALUE) {
            return;
        }
        e.getPersistentData().remove(KruimelkwalEntity.GESTUITERD);
        if (zachtGeland(e.level().getGameTime(), stuiter)) {
            event.setCanceled(true);
        }
    }

    /** Is a landing at this game time still covered by a bounce at that one? */
    public static boolean zachtGeland(long nu, long stuiter) {
        return nu >= stuiter && nu - stuiter <= KruimelkwalEntity.ZACHT_LANDEN;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Walviszicht.stuur(p);
        }
    }

    /**
     * /guhs einde2 zwevers rij (the kwal and the four animals in a row at your feet, they stay), zwerm (a swarm around
     * you), walvis (a whale ahead of you; you need the flag to see it), walviszicht aan|uit [player] (the flag of
     * {@link Walviszicht}), slee (the nearest otter-pinguïnguh becomes a sled). Op 2. None of them changes a block.
     */
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        var zwevers = Commands.literal("zwevers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("rij").executes(c -> rij(c.getSource().getPlayerOrException())))
                .then(Commands.literal("zwerm").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    return Kruimelkwallen.zwerm(p.level(), p.blockPosition().above(2), p.getRandom()).size();
                }))
                .then(Commands.literal("walvis").executes(c -> walvis(c.getSource().getPlayerOrException())))
                .then(Commands.literal("walviszicht")
                        .then(Commands.literal("aan").executes(c -> zicht(c.getSource(), c.getSource().getPlayerOrException(), true))
                                .then(Commands.argument("speler", EntityArgument.player())
                                        .executes(c -> zicht(c.getSource(), EntityArgument.getPlayer(c, "speler"), true))))
                        .then(Commands.literal("uit").executes(c -> zicht(c.getSource(), c.getSource().getPlayerOrException(), false))
                                .then(Commands.argument("speler", EntityArgument.player())
                                        .executes(c -> zicht(c.getSource(), EntityArgument.getPlayer(c, "speler"), false)))))
                .then(Commands.literal("slee").executes(c -> slee(c.getSource().getPlayerOrException())));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(zwevers)));
    }

    /** The kwal and the four animals, 1.5 blocks apart along x from the player's spot, facing south; they stay. */
    static int rij(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition();
        int n = 0;
        for (EntityType<? extends Mob> soort : List.<EntityType<? extends Mob>>of(E2ZweversFeature.KRUIMELKWAL.get(), E2ZweversFeature.VLEERMUIS_LEMURGUH.get(),
                E2ZweversFeature.OTTER_PINGUINGUH.get(), E2ZweversFeature.DAS_MOLGUH.get(), E2ZweversFeature.SCHILDPAD_EENDGUH.get())) {
            Mob dier = soort.create(level, EntitySpawnReason.TRIGGERED);
            if (dier == null) {
                continue;
            }
            dier.snapTo(pos.getX() + 0.5 + n * 1.5, pos.getY() + (dier instanceof KruimelkwalEntity ? 0.6 : 0), pos.getZ() + 0.5, 0f, 0f);
            dier.setYHeadRot(0f);
            dier.setYBodyRot(0f);
            dier.setPersistenceRequired();
            if (dier instanceof Elementdier e) {
                e.setHonk(dier.blockPosition(), 1);
            } else if (dier instanceof KruimelkwalEntity k) {
                k.setThuis(dier.position(), 1);
            }
            level.addFreshEntity(dier);
            n++;
        }
        return n;
    }

    /** A come-and-go whale 30 blocks ahead of the player and 8 below, crossing from left to right. */
    static int walvis(ServerPlayer player) {
        Vec3 kijk = player.getLookAngle().multiply(1, 0, 1);
        kijk = kijk.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : kijk.normalize();
        Vec3 plek = player.position().add(kijk.scale(50)).add(0, -14, 0);
        LeegtewalvisguhEntity walvis = Leegtewalvissen.op(player.level(), plek, player.getYRot() - 90f);
        return walvis == null ? 0 : 1;
    }

    private static int zicht(CommandSourceStack source, ServerPlayer player, boolean ziet) {
        Walviszicht.zet(player, ziet);
        source.sendSuccess(() -> Component.translatable("gui.guhs.einde2_zwevers.commando.walviszicht_" + (ziet ? "aan" : "uit"), player.getDisplayName()), true);
        return 1;
    }

    private static int slee(ServerPlayer player) {
        OtterPinguinguhEntity best = null;
        for (OtterPinguinguhEntity o : player.level().getEntities(E2ZweversFeature.OTTER_PINGUINGUH.get(), player.getBoundingBox().inflate(16), o -> o.isAlive())) {
            if (best == null || o.distanceToSqr(player) < best.distanceToSqr(player)) {
                best = o;
            }
        }
        if (best == null) {
            return 0;
        }
        best.laatRijden(true);
        return 1;
    }
}
