package nl.juiced.guhs.feature.snuffel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.network.ModNetworking;

/**
 * Sniffing and digging, server side. While a dog holds the sniff pose ({@link Hondvorm#SNUFFELT}) its nose is asked a few
 * times a second what it smells ({@link Geurbronnen#ruik}); the answer goes to that player's scent meter (kind = colour,
 * strength, "you are on the spot") and to the companion, which floats towards the scent and points. No trails: only the
 * meter and the companion. On the spot the dog digs ({@link #graaf}: a second and a half of flying dirt): a buried source
 * comes up. A source that is not buried is found by sniffing right next to it for a second.
 */
public final class Snuffelen {
    /** How long a dig takes, and how long a dog rests before the next one. */
    public static final int GRAAF_TICKS = 30, GRAAF_RUST = 8;
    /** A source that is not buried is found after this many ticks of sniffing next to it. */
    public static final int DICHTBIJ_TICKS = 20;
    private static final int METER_TICKS = 4;

    private static final class Staat {
        long graafTot = -1, rustTot;
        int dichtbij;
        @Nullable
        String dichtbijBron;
        boolean meter;
        @Nullable
        Vec3 doel;
        boolean opPlek;
    }

    private static final Map<UUID, Staat> STAAT = new ConcurrentHashMap<>();

    private Snuffelen() {
    }

    private static Staat staat(ServerPlayer p) {
        return STAAT.computeIfAbsent(p.getUUID(), u -> new Staat());
    }

    /** Is this dog digging right now? */
    public static boolean graaft(ServerPlayer p) {
        Staat s = STAAT.get(p.getUUID());
        return s != null && s.graafTot >= 0;
    }

    /** Where this dog's nose points now (the companion goes there); null: it smells nothing. */
    @Nullable
    public static Vec3 doel(ServerPlayer p) {
        Staat s = STAAT.get(p.getUUID());
        return s == null ? null : s.doel;
    }

    /** Does this dog stand on the spot of what it smells? */
    public static boolean opPlek(ServerPlayer p) {
        Staat s = STAAT.get(p.getUUID());
        return s != null && s.doel != null && s.opPlek;
    }

    /** Every tick for a dog on an island. */
    static void tick(ServerPlayer p) {
        Staat s = staat(p);
        long nu = p.level().getGameTime();
        if (s.graafTot >= 0) {
            if (nu >= s.graafTot) {
                s.graafTot = -1;
                s.rustTot = nu + GRAAF_RUST;
                graafKlaar(p);
            } else if (nu % 3 == 0) {
                aarde(p, 5);
            }
        }
        if (!Hondvorm.snuffelt(p)) {
            if (s.meter) {
                stop(p);
            }
            return;
        }
        if ((nu + p.getId()) % METER_TICKS != 0) {
            return;
        }
        Geurbronnen.Neus neus = Cutscenes.bezig(p) ? null : Geurbronnen.ruik(p);
        s.meter = true;
        if (neus == null) {
            s.doel = null;
            s.opPlek = false;
            s.dichtbij = 0;
            s.dichtbijBron = null;
            ModNetworking.sendTo(p, SnuffelPayloads.Meter.NIETS);
        } else {
            Geurbronnen.Bron b = neus.bron();
            s.doel = b.plek();
            s.opPlek = neus.opDePlek();
            ModNetworking.sendTo(p, new SnuffelPayloads.Meter(b.geur().soort().ordinal(), neus.sterkte(), neus.opDePlek(), b.graven()));
            if (neus.opDePlek() && !b.graven()) {
                // not buried: a good long sniff right next to it is enough
                if (b.id().equals(s.dichtbijBron)) {
                    s.dichtbij += METER_TICKS;
                } else {
                    s.dichtbijBron = b.id();
                    s.dichtbij = METER_TICKS;
                }
                if (s.dichtbij >= DICHTBIJ_TICKS) {
                    s.dichtbij = 0;
                    s.dichtbijBron = null;
                    Geurbronnen.vind(p, b);
                }
            } else {
                s.dichtbij = 0;
                s.dichtbijBron = null;
            }
        }
        if ((nu + p.getId()) % (METER_TICKS * 3) == 0) {
            float toon = neus == null ? 1f : 1f + neus.sterkte() * 0.5f;
            p.level().playSound(null, p.blockPosition(), SnuffelFeature.SNUF_GELUID.get(), SoundSource.PLAYERS, 0.45f, toon);
        }
    }

    /**
     * The dog starts to dig where it stands (the client's dig key, or a test). False when it cannot now: no dog, in the
     * air or the water, already digging, or watching a scene.
     */
    public static boolean graaf(ServerPlayer p) {
        if (!Hondvorm.actief(p) || !p.onGround() || p.isInWater() || Cutscenes.bezig(p)) {
            return false;
        }
        Staat s = staat(p);
        long nu = p.level().getGameTime();
        if (s.graafTot >= 0 || nu < s.rustTot) {
            return false;
        }
        s.graafTot = nu + GRAAF_TICKS;
        Hondvorm.gebaar(p, Hondvorm.GRAAF, GRAAF_TICKS);
        p.level().playSound(null, p.blockPosition(), SnuffelFeature.GRAAF_GELUID.get(), SoundSource.PLAYERS, 0.8f, 1f);
        return true;
    }

    private static void graafKlaar(ServerPlayer p) {
        if (!Hondvorm.actief(p)) {
            return;
        }
        Geurbronnen.Bron b = Geurbronnen.graafbaarBij(p);
        if (b != null) {
            aarde(p, 18);
            Geurbronnen.vind(p, b);
        } else {
            p.sendOverlayMessage(Component.translatable("gui.guhs.snuffel.graaf_niets").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Dirt of the ground under the dog flies around. */
    private static void aarde(ServerPlayer p, int aantal) {
        ServerLevel level = p.level();
        BlockPos onder = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        BlockState grond = level.getBlockState(onder);
        if (grond.isAir()) {
            return;
        }
        Vec3 voor = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(0.45));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, grond), voor.x, p.getY() + 0.15, voor.z, aantal, 0.2, 0.1, 0.2, 0.08);
    }

    /** A find: sparkles and a sound where it was, and a line on the finder's screen. */
    static void vondst(ServerPlayer p, Geurbronnen.Bron b, boolean nieuweGeur) {
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, b.plek().x, b.plek().y + 0.5, b.plek().z, 12, 0.35, 0.3, 0.35, 0.02);
        level.playSound(null, BlockPos.containing(b.plek()), SnuffelFeature.GEVONDEN_GELUID.get(), SoundSource.PLAYERS, 0.8f, 1f);
        if (!nieuweGeur) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.snuffel.gevonden", b.geur().naam()).withColor(b.geur().soort().kleur() & 0xFFFFFF));
        }
        Maatjes.blij(p);
        Staat s = staat(p);
        s.doel = null;
        s.opPlek = false;
    }

    /** The nose comes up: the meter goes quiet. */
    static void stop(ServerPlayer p) {
        Staat s = STAAT.get(p.getUUID());
        if (s == null) {
            return;
        }
        s.doel = null;
        s.opPlek = false;
        s.dichtbij = 0;
        s.dichtbijBron = null;
        if (s.meter) {
            s.meter = false;
            ModNetworking.sendTo(p, SnuffelPayloads.Meter.NIETS);
        }
    }

    static void vergeet(UUID id) {
        STAAT.remove(id);
    }

    static void opStop() {
        STAAT.clear();
    }

    /** (Tests) finishes the dig of this player now. */
    static void graafNu(ServerPlayer p) {
        Staat s = staat(p);
        if (s.graafTot >= 0) {
            s.graafTot = -1;
            graafKlaar(p);
        }
    }
}
