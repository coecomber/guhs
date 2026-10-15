package nl.juiced.guhs.feature.einde2.h4vuur;

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
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;

/**
 * einde2 phase B, slice h4_vuur, step 9: the Drakendans of the Zonnekrijgers (DESIGN 5, chapter 4) as a movement puzzle on
 * the round terrace: the seven steps of {@code drakendans_speler} light up one after the other (only for this player) and
 * you step on each in time, while Zuka (an actor only you see) dances the mirrored half ({@code drakendans_zuka}). A step
 * missed: the flames go out and you begin again, slowly and together. All seven: the dragons come ({@link Scenes#DRAKEN}),
 * and after it Zuka is one of the team ({@code zuka_team}) and you can firebend ({@code geleerd}).
 */
public final class Drakendans {
    /** Ticks you have for each step. */
    public static final int TIJD = 80;

    private static final class Dans {
        final List<Vec3> jij, zuka;
        int stap, over = TIJD;
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
        licht(p, d);
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
        Vec3 doel = d.jij.get(d.stap);
        if (p.getBlockX() == (int) Math.floor(doel.x) && p.getBlockZ() == (int) Math.floor(doel.z) && Math.abs(p.getY() - doel.y) < 1.5) {
            // the step is danced: Zuka does his, the flames rise a little higher
            Schijn.deeltjes(p, ParticleTypes.FLAME, doel.add(0, 0.2, 0), 14, 0.25, 0.05);
            Schijn.deeltjes(p, ParticleTypes.SOUL_FIRE_FLAME, d.zuka.get(d.stap).add(0, 0.2, 0), 14, 0.25, 0.05);
            Schijn.geluid(p, SoundEvents.FIRECHARGE_USE, BlockPos.containing(doel), 0.5f, 1.4f + 0.05f * d.stap);
            if (d.zukaActeur != null) {
                d.zukaActeur.speel("vuur");
            }
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
            // too late: start again from the first step
            Schijn.geluid(p, SoundEvents.FIRE_EXTINGUISH, p.blockPosition(), 0.6f, 1.0f);
            E2H4Vuur.balk(p, "drakendans.opnieuw");
            d.stap = 0;
            d.over = TIJD + 20;
            if (d.zukaActeur != null) {
                Vec3 z = d.zuka.get(0);
                d.zukaActeur.snapTo(z.x, z.y, z.z, d.zukaActeur.getYRot(), 0);
            }
            licht(p, d);
        } else if (d.over % 10 == 0) {
            Schijn.deeltjes(p, ParticleTypes.SMALL_FLAME, doel.add(0, 0.1, 0), 6, 0.3, 0.01);
        }
    }

    private static void licht(ServerPlayer p, Dans d) {
        Oplichten.zet(p, Map.of(BlockPos.containing(d.jij.get(d.stap)).below(), Blocks.SHROOMLIGHT.defaultBlockState(),
                BlockPos.containing(d.zuka.get(d.stap)).below(), Blocks.SEA_LANTERN.defaultBlockState()));
    }

    private static void klaar(ServerPlayer p) {
        stop(p, true);
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

    /** (dev, tests) the dance as if every step was danced. */
    static void winVoorTest(ServerPlayer p) {
        stop(p, false);
        E2H4Vuur.VUUR.vlag(p, E2Verhaal.V_ZUKA_TEAM, true);
        E2H4Vuur.VUUR.vlag(p, E2Verhaal.V_GELEERD, true);
        E2H4Vuur.VUUR.verder(p, E2H4Vuur.V_DRAKENDANS);
    }
}
