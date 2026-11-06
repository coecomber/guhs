package nl.juiced.guhs.feature.vadsrijk.hud;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * U-HUD: emotes on the server. Starting one sends {@link UHudPayloads.EmoteSpeel} to the player and everyone tracking them
 * (a player who comes into view later gets the running loop emote too), plays its sound and particles. A loop emote stops when
 * you move; KNUFFEL also hugs the nearest player within {@link #KNUFFEL_AFSTAND}. Works everywhere (not only in the Vadsrijk).
 */
public final class Emotes {
    public static final double KNUFFEL_AFSTAND = 3.5;
    private static final int WACHT = 10;   // ticks between two emotes (anti-spam)

    /** A running emote: what, since when (game time), where you stood. */
    public record Actief(Emote emote, long start, Vec3 waar) {
    }

    private static final Map<UUID, Actief> ACTIEF = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAATST = new ConcurrentHashMap<>();

    private Emotes() {
    }

    @Nullable
    public static Actief actief(Player p) {
        return ACTIEF.get(p.getUUID());
    }

    /**
     * Plays an emote; returns the lang key of a message for the player (null = fine, nothing to say).
     * {@code melden}: send that message to the player's action bar.
     */
    @Nullable
    public static String speel(ServerPlayer p, Emote e, boolean melden) {
        String nee = magNiet(p);
        long nu = p.level().getGameTime();
        if (nee == null && nu - LAATST.getOrDefault(p.getUUID(), -1000L) < WACHT) {
            nee = "vadsrijk.uhud.emote.even_wachten";
        }
        if (nee != null) {
            if (melden) {
                p.sendSystemMessage(Component.translatable(nee), true);
            }
            return nee;
        }
        LAATST.put(p.getUUID(), nu);
        start(p, e, nu);
        String melding = null;
        if (e == Emote.KNUFFEL) {
            ServerPlayer ander = knuffelMaatje(p);
            if (ander != null) {
                start(ander, Emote.KNUFFEL, nu);
                ander.sendSystemMessage(Component.translatable("vadsrijk.uhud.emote.knuffel.jij", p.getDisplayName()), true);
                p.sendSystemMessage(Component.translatable("vadsrijk.uhud.emote.knuffel.ander", ander.getDisplayName()), true);
                if (p.level() instanceof ServerLevel sl) {
                    Vec3 m = p.position().add(ander.position()).scale(0.5);
                    sl.sendParticles(ParticleTypes.HEART, m.x, m.y + 1.9, m.z, 6, 0.4, 0.3, 0.4, 0.02);
                }
            } else {
                melding = "vadsrijk.uhud.emote.knuffel.niemand";
                if (melden) {
                    p.sendSystemMessage(Component.translatable(melding), true);
                }
            }
        }
        return melding;
    }

    /** Why p can't emote now (lang key), or null. */
    @Nullable
    static String magNiet(ServerPlayer p) {
        if (p.isSpectator() || !p.isAlive()) {
            return "vadsrijk.uhud.emote.nu_niet";
        }
        if (p.isPassenger() || p.isFallFlying() || p.isSwimming() || p.getAbilities().flying || p.isSleeping()) {
            return "vadsrijk.uhud.emote.nu_niet";
        }
        return null;
    }

    private static void start(ServerPlayer p, Emote e, long nu) {
        ACTIEF.put(p.getUUID(), new Actief(e, nu, p.position()));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new UHudPayloads.EmoteSpeel(p.getId(), e.id, 0));
        effect(p, e, true);
    }

    public static void stop(ServerPlayer p) {
        if (ACTIEF.remove(p.getUUID()) != null) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new UHudPayloads.EmoteSpeel(p.getId(), "", 0));
        }
    }

    /** The nearest other player within {@link #KNUFFEL_AFSTAND} (not a spectator), or null. */
    @Nullable
    public static ServerPlayer knuffelMaatje(ServerPlayer p) {
        ServerPlayer beste = null;
        double d = KNUFFEL_AFSTAND * KNUFFEL_AFSTAND;
        for (Player o : p.level().players()) {
            if (o != p && o instanceof ServerPlayer sp && !sp.isSpectator() && sp.isAlive() && sp.distanceToSqr(p) <= d) {
                d = sp.distanceToSqr(p);
                beste = sp;
            }
        }
        return beste;
    }

    /** Every tick: loop emotes stop when you move; timed ones end; loops make their sound/particles now and then. */
    static void tick(MinecraftServer server) {
        if (ACTIEF.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Actief> en : ACTIEF.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            Actief a = en.getValue();
            if (p == null) {
                ACTIEF.remove(en.getKey());
                continue;
            }
            long t = p.level().getGameTime() - a.start();
            if (bewogen(p, a) || magNiet(p) != null) {
                stop(p);
            } else if (!a.emote().loop && t >= a.emote().ticks) {
                ACTIEF.remove(en.getKey());   // (the clients end it by themselves)
            } else if (a.emote().loop && t > 0 && t % (a.emote() == Emote.SLAAP ? 50 : 16) == 0) {
                effect(p, a.emote(), false);
            }
        }
    }

    /** Moved more than a little from where the emote started (looking around is fine). */
    static boolean bewogen(Player p, Actief a) {
        Vec3 w = a.waar();
        double dx = p.getX() - w.x, dz = p.getZ() - w.z;
        return dx * dx + dz * dz > 0.2 * 0.2 || Math.abs(p.getY() - w.y) > 0.6;
    }

    /** A player came into view of {@code kijker}: show them a running loop emote too. */
    static void inBeeld(ServerPlayer kijker, Player doel) {
        Actief a = ACTIEF.get(doel.getUUID());
        if (a != null && (a.emote().loop || doel.level().getGameTime() - a.start() < a.emote().ticks)) {
            PacketDistributor.sendToPlayer(kijker, new UHudPayloads.EmoteSpeel(doel.getId(), a.emote().id,
                    (int) Math.min(100000, doel.level().getGameTime() - a.start())));
        }
    }

    static void vergeet(UUID id) {
        ACTIEF.remove(id);
        LAATST.remove(id);
    }

    // ---- sound + particles ------------------------------------------------------------------------------------------------------

    private static void effect(ServerPlayer p, Emote e, boolean begin) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        double x = p.getX(), y = p.getY(), z = p.getZ();
        switch (e) {
            case ZWAAI -> {
                deeltjes(sl, ParticleTypes.HAPPY_VILLAGER, p.position().add(zij(p, -0.4)).add(0, 2.2, 0), 3, 0.15);
                geluid(sl, p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 1.7f);
            }
            case KNUFFEL -> geluid(sl, p, SoundEvents.CAT_PURR_BABY.value(), 0.8f, 1.1f);
            case DANS -> {
                deeltjes(sl, ParticleTypes.NOTE, new Vec3(x, y + 2.2, z), 2, 0.4);
                geluid(sl, p, SoundEvents.NOTE_BLOCK_BELL.value(), 0.5f, 0.8f + p.getRandom().nextFloat() * 0.8f);
            }
            case JUICH -> {
                deeltjes(sl, ParticleTypes.TOTEM_OF_UNDYING, new Vec3(x, y + 2.0, z), 14, 0.4);
                geluid(sl, p, SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.6f, 1.2f);
            }
            case ZIT -> {
                if (begin) {
                    deeltjes(sl, ParticleTypes.POOF, new Vec3(x, y + 0.2, z), 4, 0.2);
                    geluid(sl, p, SoundEvents.WOOL_PLACE, 0.8f, 0.9f);
                }
            }
            case SLAAP -> {
                deeltjes(sl, ParticleTypes.CLOUD, new Vec3(x, y + 0.6, z), 1, 0.05);
                geluid(sl, p, SoundEvents.FOX_SLEEP, 0.6f, 1.2f);
            }
            case LACH -> {
                deeltjes(sl, ParticleTypes.HAPPY_VILLAGER, new Vec3(x, y + 2.0, z), 5, 0.3);
                geluid(sl, p, SoundEvents.VILLAGER_CELEBRATE, 0.7f, 1.6f);
            }
            case WIJS -> {
                Vec3 kijk = p.getLookAngle();
                for (int i = 1; i <= 5; i++) {
                    Vec3 v = p.getEyePosition().add(kijk.scale(0.8 + i * 0.6));
                    sl.sendParticles(ParticleTypes.CRIT, v.x, v.y, v.z, 1, 0, 0, 0, 0);
                }
                geluid(sl, p, SoundEvents.ITEM_PICKUP, 0.5f, 1.4f);
            }
            case HARTJE -> {
                deeltjes(sl, ParticleTypes.HEART, new Vec3(x, y + 2.5, z), 5, 0.3);
                geluid(sl, p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
            }
        }
    }

    private static Vec3 zij(Player p, double afstand) {
        double r = Math.toRadians(p.yBodyRot);
        return new Vec3(Math.cos(r) * afstand, 0, Math.sin(r) * afstand);
    }

    private static void deeltjes(ServerLevel sl, ParticleOptions d, Vec3 v, int n, double spreid) {
        sl.sendParticles(d, v.x, v.y, v.z, n, spreid, spreid * 0.5, spreid, 0.02);
    }

    private static void geluid(ServerLevel sl, ServerPlayer p, SoundEvent s, float vol, float pitch) {
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, vol, pitch);
    }
}
