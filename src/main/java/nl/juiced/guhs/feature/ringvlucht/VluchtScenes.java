package nl.juiced.guhs.feature.ringvlucht;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.registry.ModSounds;

/**
 * 1.4.2 (rookguh): the flight, a short cutscene (8 s; texts: tools/features/ring_vlucht.py SCENE_TEKSTEN). Anchored on the
 * block the player stands on, turned so that the way up and away is as open as it gets ({@link #openste}): the Rookguh
 * floats down, the player (and Sam-guh, when he walks along: {@link #MET_SAM}) climbs on its back, up they go and off, and the
 * picture goes black. Then {@code Vlucht.land} puts the player down at the destination. In scene coordinates the player
 * looks south (+z), the way they fly.
 */
public final class VluchtScenes {
    public static Cutscene ZONDER_SAM, MET_SAM;
    /** How long a flight takes (ticks). */
    public static final int DUUR = 160;
    /** Points the flight passes (scene coordinates): where the way up and away must be open. */
    private static final List<Vec3> PAD = List.of(new Vec3(0.5, 1.5, 2.2), new Vec3(0.5, 3.0, 4.0), new Vec3(0.5, 4.5, 5.5), new Vec3(0.5, 6.5, 8.0),
            new Vec3(0.5, 7.5, 11.0), new Vec3(0.5, 8.0, 14.0), new Vec3(0.5, 8.5, 18.0), new Vec3(-3.5, 2.4, -2.0), new Vec3(-4.5, 3.0, 1.0));

    static void registreer() {
        ZONDER_SAM = scene("ringvlucht_vlucht", false);
        MET_SAM = scene("ringvlucht_vlucht_sam", true);
    }

    private static Cutscene scene(String id, boolean sam) {
        Cutscene.Builder b = Cutscene.maak(id).duur(DUUR).verbergEcht(6).cameraOntwijkt()
                .speler(new Vec3(0.5, 0, 0.5), 0)
                .acteur("rookguh", RingVluchtFeature.ROOKGUH, new Vec3(0.5, 4.5, 6.0), 180);
        if (sam) {
            b.guh("sam", GuhVariant.SAM_GUH, new Vec3(1.8, 0, 0.3), 0);
        }
        // the Rookguh floats down
        b.zwart(0, 10)
                .camera(0, new Vec3(-3.5, 2.4, -2.0), new Vec3(0.5, 1.4, 2.5)).camera(70, new Vec3(-3.0, 2.0, -0.6), new Vec3(0.5, 1.4, 2.4))
                .loop("rookguh", 0, 30, new Vec3(0.5, 0.4, 2.4)).kijk("rookguh", 30, new Vec3(0.5, 1.0, 0.5))
                .deeltjes(30, ParticleTypes.CLOUD, new Vec3(0.5, 0.6, 2.4), 16, 0.7)
                .zeg(26, "rookguh", "instappen", 46);
        // climb on its back
        b.animatie(Cutscene.SPELER, 44, "spring").loop(Cutscene.SPELER, 44, 56, new Vec3(0.5, 1.7, 2.3))
                .geluid(46, () -> ModSounds.GUH_HAPPY.get(), 1.0f, 1.3f);
        if (sam) {
            b.zeg(40, "sam", "sam", 36).animatie("sam", 50, "spring").loop("sam", 50, 62, new Vec3(1.0, 1.7, 2.8));
        }
        b.zeg(70, "rookguh", "hou_vast", 36);
        // up...
        b.loop("rookguh", 80, 124, new Vec3(0.5, 6.0, 8.0)).loop(Cutscene.SPELER, 80, 124, new Vec3(0.5, 7.3, 7.9))
                .cameraVolgt(80, new Vec3(-4.5, 3.0, 1.0), "rookguh").cameraVolgt(124, new Vec3(-5.0, 6.0, 3.0), "rookguh")
                .deeltjes(82, ParticleTypes.CLOUD, new Vec3(0.5, 0.6, 2.4), 24, 0.9)
                .geluid(84, () -> SoundEvents.PHANTOM_FLAP, 0.8f, 1.4f).geluid(104, () -> SoundEvents.PHANTOM_FLAP, 0.8f, 1.2f);
        if (sam) {
            b.loop("sam", 80, 124, new Vec3(1.0, 7.3, 8.4)).zeg(96, "sam", "sam_juich", 30).animatie("sam", 96, "juich");
        }
        // ...and away
        b.zeg(110, "", "weg", 40)
                .loop("rookguh", 124, DUUR, new Vec3(0.5, 8.5, 22.0)).loop(Cutscene.SPELER, 124, DUUR, new Vec3(0.5, 9.8, 21.9))
                .deeltjes(126, ParticleTypes.HEART, new Vec3(0.5, 8.0, 9.0), 6, 0.8)
                .zwart(138, DUUR);
        if (sam) {
            b.loop("sam", 124, DUUR, new Vec3(1.0, 9.8, 22.4));
        }
        return b.registreer();
    }

    /** The turn of the scene (around the anchor) whose way up and away has the fewest blocks in it. */
    static Rotation openste(ServerLevel level, BlockPos anker) {
        Rotation beste = Rotation.NONE;
        int besteN = Integer.MAX_VALUE;
        for (Rotation r : new Rotation[]{Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90}) {
            int n = 0;
            for (Vec3 p : PAD) {
                BlockPos at = BlockPos.containing(Cutscene.wereld(anker, r, p));
                if (level.isLoaded(at) && level.getBlockState(at).blocksMotion()) {
                    n++;
                }
                if (level.isLoaded(at.above()) && level.getBlockState(at.above()).blocksMotion()) {
                    n++;
                }
            }
            if (n < besteN) {
                beste = r;
                besteN = n;
            }
        }
        return beste;
    }

    private VluchtScenes() {
    }
}
