package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.einde2.castschurk.Wezens;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;

/**
 * einde2 phase B, slice h4_vuur, step 9: the Drakendans of the Zonnekrijgers (DESIGN 5, chapter 4) as a movement puzzle on
 * the round terrace: the seven steps of {@code drakendans_speler} light up one after the other (only for this player) and
 * you step on each in time, while Zuka (an actor only you see) dances the mirrored half ({@code drakendans_zuka}). A step
 * missed: the flames go out and you begin again, slowly and together. All seven: the dragons come ({@link Scenes#DRAKEN}),
 * and after it Zuka is one of the team ({@code zuka_team}) and you can firebend ({@code geleerd}).
 * <p>
 * fix3-h4: the scene {@link Scenes#ZONNEKRIJGERS} sets it up first (E2H4Vuur), and the dance itself got its own drama: a war
 * drum beats under it (vanilla note-block drums and a low horn, only for this player), the two resident draakguhs in their
 * caves wake up, breathe fire towards each tile you dance and roar together halfway, and the braziers on the eight pillars of
 * the terrace flare up one by one. The tiles are lit through a map that tolerates two dancers on one block (the crash of
 * playtest 2: both halves of the old route shared their first and last tile, and {@code Map.of} refuses a duplicate key).
 */
public final class Drakendans {
    /** Ticks you have for each step (fix2-h4: 5 s, was 4). */
    public static final int TIJD = 100;

    private static final class Dans {
        final List<Vec3> jij, zuka;
        int stap, over = TIJD, tel;
        @Nullable
        GuhNpcEntity zukaActeur;

        Dans(List<Vec3> jij, List<Vec3> zuka) {
            this.jij = jij;
            this.zuka = zuka;
        }
    }

    private static final Map<UUID, Dans> DANSEN = new ConcurrentHashMap<>();

    private Drakendans() {
    }

    public static boolean actief(ServerPlayer p) {
        return DANSEN.containsKey(p.getUUID());
    }

    static void start(ServerPlayer p) {
        List<Vec3> jij = Plek.pad(p, "drakendans_speler"), zuka = Plek.pad(p, "drakendans_zuka");
        if (jij.isEmpty() || zuka.size() != jij.size() || actief(p)) {
            return;
        }
        Dans d = new Dans(jij, zuka);
        d.zukaActeur = E2H4Vuur.npc(p, GuhNpcEntity.Kind.ZUKA_PRINS, zuka.get(0), 180f);
        DANSEN.put(p.getUUID(), d);
        E2H4Vuur.zeg(p, "drakendans.start");
        E2H4Vuur.titel(p, "drakendans.titel", "drakendans.sub");
        licht(p, d);
        // the masters wake up: both dragons look out of their caves, a growl rolls over the terrace, every brazier flares once
        for (CastWezen draak : draken(p)) {
            draak.zetAnimatie("praat");
        }
        Schijn.geluid(p, SoundEvents.ENDER_DRAGON_GROWL, p.blockPosition(), 0.35f, 1.4f);
        for (int k = 0; k < 8; k++) {
            Vec3 pilaar = pilaar(p, k);
            if (pilaar != null) {
                Schijn.deeltjes(p, ParticleTypes.FLAME, pilaar, 12, 0.2, 0.04);
            }
        }
    }

    static void tick(ServerPlayer p) {
        Dans d = DANSEN.get(p.getUUID());
        if (d == null) {
            return;
        }
        if (!Plek.in(p, "drakendans")) {
            E2H4Vuur.balk(p, "drakendans.weg");
            stop(p, false);
            return;
        }
        drum(p, d);
        Vec3 doel = d.jij.get(d.stap);
        if (p.getBlockX() == (int) Math.floor(doel.x) && p.getBlockZ() == (int) Math.floor(doel.z) && Math.abs(p.getY() - doel.y) < 1.5) {
            // the step is danced: Zuka does his, the flames rise a little higher
            Schijn.deeltjes(p, ParticleTypes.FLAME, doel.add(0, 0.2, 0), 14, 0.25, 0.05);
            Schijn.deeltjes(p, ParticleTypes.SOUL_FIRE_FLAME, d.zuka.get(d.stap).add(0, 0.2, 0), 14, 0.25, 0.05);
            Schijn.geluid(p, SoundEvents.FIRECHARGE_USE, BlockPos.containing(doel), 0.5f, 1.4f + 0.05f * d.stap);
            if (d.zukaActeur != null) {
                d.zukaActeur.speel("vuur");
            }
            adem(p, d, doel);
            d.stap++;
            d.over = TIJD;
            if (d.stap >= d.jij.size()) {
                klaar(p);
                return;
            }
            if (d.zukaActeur != null) {
                Vec3 z = d.zuka.get(d.stap);
                d.zukaActeur.snapTo(z.x, z.y, z.z, d.zukaActeur.getYRot(), 0);
            }
            licht(p, d);
            E2H4Vuur.balk(p, "drakendans.stap", d.stap, d.jij.size());
            return;
        }
        if (--d.over <= 0) {
            // too late (fix2-h4: no more back to the first step): the flames flicker, the same tile lights up again
            Schijn.geluid(p, SoundEvents.FIRE_EXTINGUISH, p.blockPosition(), 0.6f, 1.0f);
            E2H4Vuur.balk(p, "drakendans.opnieuw");
            d.over = TIJD + 20;
            licht(p, d);
        } else if (d.over % 10 == 0) {
            Schijn.deeltjes(p, ParticleTypes.SMALL_FLAME, doel.add(0, 0.1, 0), 6, 0.3, 0.01);
        }
    }

    private static void licht(ServerPlayer p, Dans d) {
        Oplichten.zet(p, tegels(d.jij.get(d.stap), d.zuka.get(d.stap)));
    }

    /**
     * The two tiles of one step: yours (shroomlight) and Zuka's (sea lantern). fix3-h4: when both are the same block (the old
     * build, or a build that is ever changed so), yours wins instead of {@code Map.of} throwing "duplicate key".
     */
    static Map<BlockPos, BlockState> tegels(Vec3 jij, Vec3 zuka) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        m.put(BlockPos.containing(jij).below(), Blocks.SHROOMLIGHT.defaultBlockState());
        m.putIfAbsent(BlockPos.containing(zuka).below(), Blocks.SEA_LANTERN.defaultBlockState());
        return m;
    }

    /** (fix3-h4) The war drum under the dance, for this player only: a big beat every second, a small one between, a horn. */
    private static void drum(ServerPlayer p, Dans d) {
        int t = d.tel++;
        if (t % 10 != 0) {
            return;
        }
        BlockPos waar = p.blockPosition();
        boolean groot = t % 20 == 0;
        Schijn.geluid(p, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), waar, groot ? 0.9f : 0.5f, groot ? 0.5f : 0.7f);
        if (t % 80 == 0) {
            Schijn.geluid(p, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), waar, 0.45f, 0.5f + 0.06f * Math.min(d.stap, 6));
        }
        if (t % 40 == 20) {
            Schijn.geluid(p, SoundEvents.NOTE_BLOCK_HAT.value(), waar, 0.35f, 0.8f);
        }
    }

    /**
     * (fix3-h4) A danced step: a dragon breathes a stream of fire from its cave over the tile (red over yours, blue over
     * Zuka's, taking turns), the brazier of the next pillar flares, and halfway both masters roar together.
     */
    private static void adem(ServerPlayer p, Dans d, Vec3 doel) {
        List<CastWezen> draken = draken(p);
        if (!draken.isEmpty()) {
            CastWezen draak = draken.get(d.stap % draken.size());
            draak.zetAnimatie("vuur");
            Vec3 bek = draak.getEyePosition();
            Vec3 boven = (d.stap % 2 == 0 ? doel : d.zuka.get(d.stap)).add(0, 3.5, 0);
            for (int i = 1; i <= 10; i++) {
                Schijn.deeltjes(p, d.stap % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME, bek.lerp(boven, i / 10.0), 4, 0.15, 0.01);
            }
            Schijn.geluid(p, SoundEvents.BLAZE_SHOOT, BlockPos.containing(bek), 0.5f, 0.7f);
        }
        Vec3 pilaar = pilaar(p, d.stap);
        if (pilaar != null) {
            Schijn.deeltjes(p, ParticleTypes.FLAME, pilaar, 30, 0.25, 0.08);
            Schijn.deeltjes(p, ParticleTypes.LAVA, pilaar, 4, 0.2, 0.0);
        }
        if (d.stap == d.jij.size() / 2) {
            for (CastWezen draak : draken) {
                draak.zetAnimatie("vuur");
            }
            Schijn.geluid(p, SoundEvents.ENDER_DRAGON_GROWL, p.blockPosition(), 0.6f, 1.2f);
            Schijn.deeltjes(p, ParticleTypes.FIREWORK, doel.add(0, 2.5, 0), 30, 1.2, 0.05);
            E2H4Vuur.balk(p, "drakendans.brul");
        }
    }

    /** (fix3-h4) The resident draakguhs of the terrace (west first: red, then blue); empty when they are not there. */
    private static List<CastWezen> draken(ServerPlayer p) {
        Vec3 m = Plek.midden(p, "drakendans");
        if (m == null) {
            return List.of();
        }
        List<CastWezen> uit = new ArrayList<>(p.level().getEntitiesOfClass(CastWezen.class, new AABB(m, m).inflate(28, 12, 28),
                w -> w.isAlive() && w.wezens() == Wezens.DRAAKGUH));
        uit.sort(Comparator.comparingDouble(CastWezen::getX));
        return uit;
    }

    /** (fix3-h4) The top of pillar k (0-7) round the terrace, where its brazier burns (the build: 8.2 out, 22.5 + 45 k degrees). */
    @Nullable
    private static Vec3 pilaar(ServerPlayer p, int k) {
        Vec3 m = Plek.midden(p, "drakendans");
        if (m == null) {
            return null;
        }
        double a = Math.toRadians(22.5 + 45 * (k % 8));
        return new Vec3(Math.floor(m.x) + Math.round(8.2 * Math.cos(a)) + 0.5, m.y + 5.2, Math.floor(m.z) + Math.round(8.2 * Math.sin(a)) + 0.5);
    }

    private static void klaar(ServerPlayer p) {
        stop(p, true);
        Schijn.geluid(p, SoundEvents.PLAYER_LEVELUP, p.blockPosition(), 0.8f, 1.2f);
        E2H4Vuur.titel(p, "drakendans.titel_klaar", null);
        E2H4Vuur.speel(p, Scenes.DRAKEN, "trap_boven", q -> {
            E2H4Vuur.VUUR.vlag(q, E2Verhaal.V_ZUKA_TEAM, true);
            E2H4Vuur.VUUR.vlag(q, E2Verhaal.V_GELEERD, true);
            if (E2H4Vuur.VUUR.verder(q, E2H4Vuur.V_DRAKENDANS)) {
                E2H4Vuur.zeg(q, "drakendans.geleerd");
                E2H4Vuur.zeg(q, "drakendans.toneel");
            }
        });
    }

    static void stop(ServerPlayer p, boolean gewonnen) {
        Dans d = DANSEN.remove(p.getUUID());
        if (d != null) {
            Oplichten.wis(p);
            if (d.zukaActeur != null) {
                d.zukaActeur.discard();
            }
        }
    }

    /** (tests) the step the player dances now (-1: no dance). */
    static int stap(ServerPlayer p) {
        Dans d = DANSEN.get(p.getUUID());
        return d == null ? -1 : d.stap;
    }

    /** (tests) the spot of the tile the player must step on now (null: no dance). */
    @Nullable
    static Vec3 doel(ServerPlayer p) {
        Dans d = DANSEN.get(p.getUUID());
        return d == null ? null : d.jij.get(d.stap);
    }

    /** (dev, tests) the dance as if every step was danced. */
    static void winVoorTest(ServerPlayer p) {
        stop(p, false);
        E2H4Vuur.VUUR.vlag(p, E2Verhaal.V_ZUKA_TEAM, true);
        E2H4Vuur.VUUR.vlag(p, E2Verhaal.V_GELEERD, true);
        E2H4Vuur.VUUR.verder(p, E2H4Vuur.V_DRAKENDANS);
    }
}
