package nl.juiced.guhs.feature.guhwaii;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The vadsigheid-scanner's measurement. It works on EVERY guh (wild, tamed, a story guh, big or small): the meter
 * VADSIGHEIDSNIVEAU fills up (beep... beep... beepbeepbeep), goes past "Vads", "Heel vads", "VAHOEG"... and breaks right
 * through the top: ONBEREKENBAAR VAHOEG! (A guh is never "te vads": there is simply no number big enough.)
 * <p>
 * The one who clicked gets the big screen (payload {@link GuhwaiiPayloads.Scan}, client {@code ScannerScherm}); everybody
 * near hears the beeps and the pop, sees the confetti and reads the result. {@link #METING_TICKS} after the click the guh does
 * a VAHOEG jump. One measurement at a time per scanner.
 */
public final class Scanner {
    /** How long the meter takes to break through (ticks; the screen does the same). */
    public static final int METING_TICKS = 64;
    /** How far the others hear/read it. */
    public static final int HOOR = 16;
    public static final String RESULTAAT = "gui.guhs.guhwaii.scanner.onberekenbaar";

    /** A measurement: which guh, and what came out (always ONBEREKENBAAR VAHOEG). */
    public record Meting(GuhEntity guh, String naam, String uitkomst) {
    }

    private record Lopend(ServerLevel level, BlockPos pos, UUID guh, @Nullable UUID speler, long start) {
    }

    private static final Map<String, Lopend> LOPEND = new ConcurrentHashMap<>();

    private Scanner() {
    }

    private static String sleutel(ServerLevel level, BlockPos pos) {
        return level.dimension().identifier() + "|" + pos.asLong();
    }

    /** Is a measurement running on this scanner? */
    public static boolean bezig(ServerLevel level, BlockPos pos) {
        return LOPEND.containsKey(sleutel(level, pos));
    }

    /** The guh standing on the scan plate (any guh), or null. */
    @Nullable
    public static GuhEntity opDePlaat(ServerLevel level, BlockPos pos) {
        AABB plaat = new AABB(pos).expandTowards(0, 1.2, 0).inflate(0.15, 0, 0.15);
        return level.getEntitiesOfClass(GuhEntity.class, plaat, g -> g.getType() == ModEntities.GUH.get() && g.isAlive()).stream()
                .min(Comparator.comparingDouble(g -> g.position().distanceToSqr(Vec3.atBottomCenterOf(pos)))).orElse(null);
    }

    /**
     * A click on the scanner: the guh on the plate, else a picked-up guh from your hand, else your nearest own guh hops on,
     * else any guh close by; then measure. Returns the measurement, or null (nobody to measure, or already measuring).
     */
    @Nullable
    public static Meting gebruik(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack hand) {
        if (bezig(level, pos)) {
            GuhQuests.hint(player, "gui.guhs.guhwaii.scanner.bezig");
            return null;
        }
        GuhEntity guh = opDePlaat(level, pos);
        Vec3 plaat = Vec3.atBottomCenterOf(pos).add(0, 3 / 16.0, 0);
        if (guh == null && hand.getItem() instanceof PickedUpGuhItem) {
            CompoundTag data = PickedUpGuhItem.guhData(hand);
            if (data != null) {
                Entity e = PickedUpGuhItem.release(level, data, plaat.x, plaat.y, plaat.z, player.getYRot() + 180f);
                if (e instanceof GuhEntity g) {
                    hand.shrink(1);
                    guh = g;
                }
            }
        }
        if (guh == null) {
            List<GuhEntity> dichtbij = level.getEntitiesOfClass(GuhEntity.class, new AABB(pos).inflate(12),
                    g -> g.getType() == ModEntities.GUH.get() && g.isAlive() && !g.isPassenger() && !g.isVehicle());
            guh = dichtbij.stream().filter(g -> g.isOwnedBy(player)).min(Comparator.comparingDouble(g -> g.distanceToSqr(plaat))).orElse(null);
            if (guh == null) {
                guh = dichtbij.stream().filter(g -> g.distanceToSqr(plaat) < 6 * 6).min(Comparator.comparingDouble(g -> g.distanceToSqr(plaat)))
                        .orElse(null);
            }
            if (guh != null) {
                // hop! onto the plate
                level.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.4, guh.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                guh.getNavigation().stop();
                guh.teleportTo(plaat.x, plaat.y, plaat.z);
                level.sendParticles(ParticleTypes.POOF, plaat.x, plaat.y + 0.4, plaat.z, 6, 0.2, 0.2, 0.2, 0.02);
            }
        }
        if (guh == null) {
            GuhQuests.hint(player, "gui.guhs.guhwaii.scanner.leeg");
            return null;
        }
        return scan(level, pos, guh, player);
    }

    /** Measures this guh on the scanner at pos (the screen for the player, the sounds and the result for everybody near). */
    public static Meting scan(ServerLevel level, BlockPos pos, GuhEntity guh, @Nullable ServerPlayer player) {
        String naam = guh.getDisplayName().getString();
        Meting m = new Meting(guh, naam, RESULTAAT);
        guh.getNavigation().stop();
        if (player != null) {
            guh.getLookControl().setLookAt(player);
            ModNetworking.sendTo(player, new GuhwaiiPayloads.Scan(guh.getId(), naam, guh.getVariant().displayName().getString()));
            GuhwaiiFeature.advancement(player, "scanner");
        }
        LOPEND.put(sleutel(level, pos), new Lopend(level, pos.immutable(), guh.getUUID(), player == null ? null : player.getUUID(), level.getGameTime()));
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.6f);
        return m;
    }

    /** (server tick) the running measurements: beeps getting faster and higher, then the pop at the top. */
    static void tick() {
        if (LOPEND.isEmpty()) {
            return;
        }
        for (var it = LOPEND.entrySet().iterator(); it.hasNext(); ) {
            Lopend l = it.next().getValue();
            long t = l.level().getGameTime() - l.start();
            BlockPos pos = l.pos();
            if (t < METING_TICKS) {
                // beep... beep... beep-beep-beep (the gaps shrink, the pitch climbs)
                int gat = t < 24 ? 8 : t < 44 ? 4 : 2;
                if (t % gat == 0) {
                    float pitch = 0.6f + 1.4f * t / METING_TICKS;
                    l.level().playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.7f, pitch);
                }
                if (t % 6 == 0) {
                    l.level().sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.05);
                }
                continue;
            }
            it.remove();
            klaar(l);
        }
    }

    /** The meter breaks through: pop, confetti, the guh jumps VAHOEG, and everybody near reads it. */
    private static void klaar(Lopend l) {
        ServerLevel level = l.level();
        BlockPos pos = l.pos();
        double x = pos.getX() + 0.5, y = pos.getY() + 1.4, z = pos.getZ() + 0.5;
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.BLOCKS, 1f, 1.2f);
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.BLOCKS, 0.8f, 1f);
        level.sendParticles(ParticleTypes.FIREWORK, x, y + 0.6, z, 30, 0.5, 0.5, 0.5, 0.15);
        level.sendParticles(ParticleTypes.HEART, x, y, z, 8, 0.6, 0.4, 0.6, 0.05);
        level.sendParticles(ParticleTypes.POOF, x, y + 0.8, z, 10, 0.3, 0.2, 0.3, 0.05);
        Entity e = level.getEntity(l.guh());
        String naam = e == null ? "?" : e.getDisplayName().getString();
        if (e instanceof GuhEntity guh && guh.isAlive()) {
            guh.playSound(ModSounds.GUH_HAPPY.get(), 1f, guh.getVoicePitch());
            if (GuhEmotes.canStart(guh)) {
                guh.emotes.start(Emote.VAHOEG, false, GuhEmotes.Source.SELF);
            }
        }
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().closerThan(pos, HOOR)) {
                p.sendSystemMessage(Component.translatable("gui.guhs.guhwaii.scanner.uitslag", naam).withStyle(s -> s.withColor(0x7FE6FF)));
            }
        }
    }

    /** (tests) forget the running measurements. */
    static void vergeet() {
        LOPEND.clear();
    }
}
