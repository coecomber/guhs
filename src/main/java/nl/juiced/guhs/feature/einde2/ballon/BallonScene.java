package nl.juiced.guhs.feature.einde2.ballon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.E2AppaFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 phase C, slice trap5_ballon: the scene of the launch, {@code einde2_trap5_ballon_vredesballon} (CONTRACT_E2_B 5:
 * target 30-45 s). Running time without the waits: 870 ticks = 43.5 s (over 30 s: in the report). Every spoken line stands
 * still until the viewer clicks (1.4.4).
 * <p>
 * Anchor = {@code vredesballon_anker} (the corner of the overlays; the temple is never turned). The actor balloon
 * ({@link BallonActeur}, the finished balloon of the overlay at full size) stands where the overlay's balloon stood; the
 * overlay's moving parts are hidden for the viewer meanwhile ({@link BallonLagen}). The player and the Werktuigguh stand in
 * the basket and rise with it; it flies over the temple to the camp, where the team stands at their {@code team_*} markers
 * (the same spots where reizen puts them; the real ones are hidden by {@link Cutscene.Builder#verbergEcht}) and knabbels rain
 * down ({@link BallonActeur}: "strooi"). The last lines look ahead: there is always a place further on.
 */
public final class BallonScene {
    /** Where the overlay's balloon stands: the middle of its 25 x 25, on the row of the basket's floor. */
    static final Vec3 BALLON = new Vec3(12.5, 2, 12.5);
    /** The rise and the flight: how far up in the yard, and the spot above the camp. */
    static final double OP = 26;
    static final Vec3 SPELER = new Vec3(11.0, 3, 14.6), WERKTUIGGUH = new Vec3(14.0, 3, 14.6);
    /** The team at their markers (relative to the anchor; the sidecar's numbers as the fallback). */
    static final Vec3 AANG = rel("team_aang", -64, -8, 54), KATARA = rel("team_katara", -58, -8, 54), SOKKA = rel("team_sokka", -60, -8, 57),
            TOPH = rel("team_toph", -62, -8, 57), ZUKA = rel("team_zuka", -60, -8, 52), MOMO = rel("team_momo", -62, -8, 52),
            APPA = rel("appa_kamp", -70, -8, 60);
    /** The basket above the camp. */
    static final Vec3 BOVEN = new Vec3(AANG.x + 3, AANG.y + 24, AANG.z - 4);

    private static final int OPSTIJG = 120, OPSTIJG_KLAAR = 290, VLUCHT_KLAAR = 500;

    public static final Cutscene VREDESBALLON = Cutscene.maak("einde2_trap5_ballon_vredesballon").bij(Vredesballon.ID).duur(870).verbergEcht(110)
            .acteur("ballon", E2BallonFeature.VREDESBALLON_ACTEUR, BALLON, 0f)
            .speler(SPELER, 0f)
            .npc("werktuigguh", Kind.WERKTUIGGUH, WERKTUIGGUH, 0f)
            .npc("aang", Kind.AANG_GUH, AANG, -90f)
            .npc("katara", Kind.KATARA_GUH, KATARA, 90f)
            .npc("sokka", Kind.SOKKA_GUH, SOKKA, 180f)
            .npc("toph", Kind.TOPH_GUH, TOPH, 180f)
            .npc("zuka", Kind.ZUKA_VUURHEER, ZUKA, 0f)
            .acteur("momo", E2AppaFeature.MOMO_GUH, MOMO, 0f)
            .acteur("appa", E2AppaFeature.APPA_GUH, APPA, 135f)
            .zwart(0, 16)
            // --- the yard: ready ---------------------------------------------------------------------------------------------
            .camera(0, v(23, 1.6, 27), v(12.5, 9, 12.5))
            .camera(60, v(21, 2.0, 25), v(12.5, 10, 12.5))
            .zeg(12, "werktuigguh", "klaar", 50)
            .animatie("werktuigguh", 14, "eureka")
            .geluid(60, () -> SoundEvents.FIRECHARGE_USE, 1.0f, 0.7f)
            .geluid(64, () -> SoundEvents.BLAZE_SHOOT, 0.6f, 0.6f)
            .deeltjes(60, ParticleTypes.FLAME, v(12.5, 14, 12.5), 40, 0.6)
            .deeltjes(64, ParticleTypes.LARGE_SMOKE, v(12.5, 16, 12.5), 12, 1.0)
            .cameraKnip(62, v(7, 2.2, 22), v(12.5, 30, 12.5))
            .camera(118, v(6.5, 2.4, 21), v(12.5, 33, 12.5))
            .zeg(66, "", "loeit", 50)
            .deeltjes(90, ParticleTypes.END_ROD, v(7.5, 33, 12.5), 20, 1.2)
            .deeltjes(90, ParticleTypes.END_ROD, v(17.5, 33, 12.5), 20, 1.2)
            // --- the rise -----------------------------------------------------------------------------------------------------
            .loop("ballon", OPSTIJG, OPSTIJG_KLAAR, BALLON.add(0, OP, 0))
            .loop(Cutscene.SPELER, OPSTIJG, OPSTIJG_KLAAR, SPELER.add(0, OP, 0))
            .loop("werktuigguh", OPSTIJG, OPSTIJG_KLAAR, WERKTUIGGUH.add(0, OP, 0))
            .cameraKnipVolgt(OPSTIJG, v(22, 1.6, 27), "ballon")
            .cameraVolgt(OPSTIJG_KLAAR, v(30, 18, 36), "ballon")
            .schud(OPSTIJG, 0.4f, 10)
            .geluid(OPSTIJG, () -> SoundEvents.FIRECHARGE_USE, 1.0f, 0.5f)
            .geluid(OPSTIJG + 4, () -> SoundEvents.WOOL_PLACE, 1.0f, 0.5f)
            .deeltjes(OPSTIJG, ParticleTypes.CLOUD, v(12.5, 0.5, 12.5), 60, 4.0)
            .zeg(170, "werktuigguh", "vroeger", 50)
            .animatie("werktuigguh", 172, "praat")
            // --- the flight over the temple -----------------------------------------------------------------------------------
            .loop("ballon", OPSTIJG_KLAAR, VLUCHT_KLAAR, BOVEN)
            .loop(Cutscene.SPELER, OPSTIJG_KLAAR, VLUCHT_KLAAR, BOVEN.add(SPELER.subtract(BALLON)))
            .loop("werktuigguh", OPSTIJG_KLAAR, VLUCHT_KLAAR, BOVEN.add(WERKTUIGGUH.subtract(BALLON)))
            .cameraKnipVolgt(OPSTIJG_KLAAR, v(-10, 40, 95), "ballon")
            .cameraVolgt(VLUCHT_KLAAR, v(-35, 28, 92), "ballon")
            .zeg(300, "", "luiken", 50)
            .animatie("ballon", 340, "strooi")
            .geluid(340, () -> SoundEvents.BUNDLE_DROP_CONTENTS, 1.0f, 0.9f)
            .geluid(344, () -> SoundEvents.PLAYER_LEVELUP, 0.5f, 1.6f)
            // --- the camp: knabbels raining on the team -----------------------------------------------------------------------
            .cameraKnipVolgt(VLUCHT_KLAAR, AANG.add(9, 1.4, 8), "ballon")
            .camera(718, AANG.add(7, 1.8, 10), BOVEN.add(0, 4, 0))
            .deeltjes(VLUCHT_KLAAR + 2, ParticleTypes.HAPPY_VILLAGER, AANG.add(3, 1.5, 0), 40, 3.0)
            .zeg(506, "aang", "regen", 40)
            .animatie("aang", 506, "spring")
            .kijk("aang", 506, BOVEN)
            .kijk("katara", 506, BOVEN)
            .kijk("sokka", 520, BOVEN)
            .kijk("toph", 520, AANG)
            .kijk("zuka", 520, BOVEN)
            .geluid(506, () -> SoundEvents.PLAYER_LEVELUP, 0.6f, 1.3f)
            .zeg(556, "sokka", "sokka", 45)
            .animatie("sokka", 556, "spring")
            .loop("momo", 556, 600, SOKKA.add(1.2, 0, -0.4))
            .zeg(610, "toph", "toph", 45)
            .zeg(664, "zuka", "zuka", 45)
            .deeltjes(664, ParticleTypes.HAPPY_VILLAGER, ZUKA.add(0, 2, 0), 12, 0.6)
            // --- up in the basket: the look ahead -----------------------------------------------------------------------------
            .cameraKnipVolgt(720, BOVEN.add(WERKTUIGGUH.subtract(BALLON)).add(4.5, 1.8, 6.5), "werktuigguh")
            .zeg(726, "werktuigguh", "slot", 50)
            .animatie("werktuigguh", 728, "praat")
            .cameraKnip(790, BOVEN.add(16, 10, 28), BOVEN.add(0, 14, 0))
            .camera(868, BOVEN.add(34, 20, 58), BOVEN.add(0, 16, 0))
            .zeg(796, "", "einde", 50)
            .zwart(850, 870)
            .registreer();

    private BallonScene() {
    }

    /** (Feature.register) loads the class: the scene is registered. */
    static void laad() {
    }

    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    /** A marker of the temple relative to the anchor (the feet of an npc spot: its block's middle), or the fallback. */
    private static Vec3 rel(String id, double x, double y, double z) {
        Plekken.Plek p = Plekken.plek(Vredesballon.STRUCTUUR, id);
        Plekken.Plek a = Plekken.plek(Vredesballon.STRUCTUUR, Vredesballon.ANKER);
        if (p == null || a == null) {
            return new Vec3(x + 0.5, y, z + 0.5);
        }
        BlockPos d = p.pos().subtract(a.pos());
        return new Vec3(d.getX() + 0.5, d.getY(), d.getZ() + 0.5);
    }
}
