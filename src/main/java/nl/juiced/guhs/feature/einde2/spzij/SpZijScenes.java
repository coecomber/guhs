package nl.juiced.guhs.feature.einde2.spzij;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 "Sponsguh", slice sp_zij (CONTRACT_E2_C 5.2): the five scenes of the side lines (texts {@code scene.guhs.<id>.*},
 * tools/features/einde2_sp_zij.py {@code SCENES}). Every spoken line waits for a click (1.4.4). Each scene's anchor is a
 * marker of Bikinibodem; the cameras of the scripts are also recorded in the build (tools/features/einde2_sp_zij_plekken.py,
 * {@code zij_<scene>_camera}) so the build's self-check proves a free line to the speakers. Running times (the waits not
 * counted):
 * <ul>
 *   <li>{@link #SANDY} {@code einde2_sp_zij_sandy}: 21 s, Sandy-guh welcomes you in her tree dome, the bubble helmet, she misses
 *       her pink sky, karate?;</li>
 *   <li>{@link #RAKET} {@code einde2_sp_zij_sandy_raket}: 26 s, the parts in, the countdown, the rocket goes up a tiny bit
 *       (its nose cone flies and lands again), her pink sky, the luchthelm;</li>
 *   <li>{@link #GUHLANDER} {@code einde2_sp_zij_guhlander}: 23.5 s, the mist, the ghost ship over the Schelpstraat, the
 *       captain;</li>
 *   <li>{@link #HAKKER} {@code einde2_sp_zij_hakker}: 30 s, "De Nacht van de Hakkende Hakker" by candle light (it was onions),
 *       then the three lost lanterns;</li>
 *   <li>{@link #HELDEN} {@code einde2_sp_zij_helden}: 23.5 s, Zeemeerman and Mosselguh, the lost belt in the invisible
 *       boatmobile.</li>
 * </ul>
 */
public final class SpZijScenes {
    public static final String VOOR = "einde2_sp_zij_";

    /** Sandy-guh's welcome: anchor = {@code bikinibodem/sandy} (she stands there, you a few steps south of her). */
    public static final Cutscene SANDY;

    static {
        Cutscene.Builder b = Cutscene.maak(VOOR + "sandy").duur(420).bij(SpZijLijnen.SANDY).verbergEcht(4)
                .speler(new Vec3(0.5, 0, 4.5), 180f)
                .acteur("sandy", E2SponsguhFeature.SANDY_GUH, new Vec3(0.5, 0, 0.5), 0f)
                .camera(0, new Vec3(4.8, 2.0, 5.6), new Vec3(0.5, 1.3, 2.5))
                .camera(120, new Vec3(4.2, 1.9, 4.4), new Vec3(0.5, 1.3, 2.5))
                .cameraKnip(130, new Vec3(1.6, 1.8, 6.8), new Vec3(0.5, 1.2, 0.5))
                .camera(200, new Vec3(1.3, 1.7, 6.2), new Vec3(0.5, 1.2, 0.5))
                .cameraKnip(205, new Vec3(0.5, 1.8, 7.4), new Vec3(0.5, 1.7, 4.5))
                .camera(270, new Vec3(0.6, 1.8, 7.0), new Vec3(0.5, 1.7, 4.5))
                .cameraKnip(275, new Vec3(1.6, 1.8, 6.8), new Vec3(0.5, 1.2, 0.5))
                .cameraKnip(340, new Vec3(4.8, 2.3, 5.2), new Vec3(0.5, 1.3, 2.5))
                .camera(420, new Vec3(4.6, 2.2, 4.6), new Vec3(0.5, 1.3, 2.5))
                .zeg(20, "sandy", "hallo", 60)
                .zeg(85, "sandy", "lucht", 60)
                .zeg(150, "sandy", "helm", 50)
                .zeg(215, "sandy", "grap", 55)
                .zeg(280, "sandy", "roze", 55)
                .zeg(345, "sandy", "karate", 60)
                .animatie("sandy", 20, "juich")
                .animatie("sandy", 85, "praat")
                .animatie("sandy", 150, "praat")
                .animatie("sandy", 215, "lach")
                .animatie("sandy", 280, "praat")
                .animatie("sandy", 345, "karate")
                .geluid(170, E2SponsguhFeature.BELLEN, 1f, 1.2f)
                .geluid(215, E2SponsguhFeature.BELLEN, 0.6f, 1.6f);
        for (int t = 170; t <= 260; t += 15) {
            b.deeltjes(t, ParticleTypes.BUBBLE_POP, new Vec3(0.5, 1.9, 4.5), 8, 0.35);
        }
        SANDY = b.registreer();
    }

    /** The rocket goes up (a little): anchor = {@code bikinibodem/raket} (the middle of the 3 x 6 x 3 rocket at its foot). */
    public static final Cutscene RAKET;

    static {
        Cutscene.Builder b = Cutscene.maak(VOOR + "sandy_raket").duur(520).bij(SpZijLijnen.SANDY).verbergEcht(7)
                .speler(new Vec3(2.0, 0, 4.0), 160f)
                .acteur("sandy", E2SponsguhFeature.SANDY_GUH, new Vec3(3.5, 0, 1.5), 90f)
                .acteur("neus", () -> EntityType.ITEM_DISPLAY, new Vec3(0.5, 6.4, 0.5), 0f,
                        tag -> tag.merge(SpZijEigen.dingData("minecraft:firework_rocket", 1.6f)))
                .camera(0, new Vec3(6.5, 2.6, 7.5), new Vec3(1.5, 2.4, 1.0))
                .camera(150, new Vec3(6.0, 2.4, 6.8), new Vec3(1.5, 2.4, 1.0))
                .cameraKnip(160, new Vec3(4.5, 1.9, 4.6), new Vec3(2.6, 1.3, 2.0))
                .camera(220, new Vec3(4.3, 1.9, 4.3), new Vec3(2.6, 1.3, 2.0))
                .cameraKnip(225, new Vec3(5.5, 1.5, 6.5), new Vec3(0.5, 5.0, 0.5))
                .camera(270, new Vec3(5.5, 1.5, 6.5), new Vec3(0.5, 10.5, 0.5))
                .camera(300, new Vec3(5.5, 1.6, 6.5), new Vec3(0.5, 6.0, 0.5))
                .cameraKnip(310, new Vec3(4.5, 1.9, 4.6), new Vec3(2.6, 1.3, 2.0))
                .camera(440, new Vec3(4.3, 1.9, 4.2), new Vec3(2.6, 1.3, 2.0))
                .cameraKnip(445, new Vec3(6.5, 2.4, 7.0), new Vec3(1.5, 2.2, 1.5))
                .camera(520, new Vec3(6.2, 2.4, 6.6), new Vec3(1.5, 2.2, 1.5))
                .zeg(20, "sandy", "onderdelen", 60)
                .zeg(160, "sandy", "aftellen", 55)
                .zeg(320, "sandy", "sprongetje", 55)
                .zeg(385, "sandy", "roze", 55)
                .zeg(450, "sandy", "helm", 60)
                .animatie("sandy", 20, "juich")
                .loop("sandy", 80, 105, new Vec3(2.0, 0, 0.6))
                .kijk("sandy", 106, new Vec3(0.5, 2, 0.5))
                .loop("sandy", 150, 158, new Vec3(3.5, 0, 1.5))
                .kijk("sandy", 159, new Vec3(0.5, 4, 0.5))
                .animatie("sandy", 160, "praat")
                .animatie("sandy", 300, "schrik")
                .kijk("sandy", 315, new Vec3(2.0, 1.5, 4.0))
                .animatie("sandy", 320, "lach")
                .animatie("sandy", 385, "praat")
                .animatie("sandy", 450, "juich")
                .loop("neus", 232, 268, new Vec3(0.5, 11.6, 0.5))
                .loop("neus", 272, 298, new Vec3(0.5, 6.4, 0.5))
                .geluid(110, () -> SoundEvents.ANVIL_USE, 0.6f, 1.4f)
                .geluid(130, () -> SoundEvents.ANVIL_USE, 0.6f, 1.7f)
                .geluid(230, () -> SoundEvents.FIREWORK_ROCKET_LAUNCH, 1f, 0.6f)
                .geluid(240, () -> SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 0.7f)
                .geluid(298, () -> SoundEvents.SLIME_JUMP, 1f, 0.6f)
                .schud(228, 1.4f, 45);
        for (int t = 110; t <= 140; t += 10) {
            b.deeltjes(t, ParticleTypes.CRIT, new Vec3(0.9, 1.2, 0.9), 10, 0.4);
        }
        for (int t = 228; t <= 290; t += 6) {
            b.deeltjes(t, ParticleTypes.FLAME, new Vec3(0.5, 0.3, 0.5), 10, 0.5);
            b.deeltjes(t, ParticleTypes.CAMPFIRE_COSY_SMOKE, new Vec3(0.5, 0.6, 0.5), 3, 0.8);
        }
        b.deeltjes(300, ParticleTypes.POOF, new Vec3(0.5, 6.5, 0.5), 16, 0.5);
        RAKET = b.registreer();
    }

    /** The ghost ship appears: anchor = {@code bikinibodem/guhlander_trap} (the mooring under the ship's bubble stream). */
    public static final Cutscene GUHLANDER;

    static {
        Cutscene.Builder b = Cutscene.maak(VOOR + "guhlander").duur(470).bij(SpZijLijnen.GUHLANDER).verbergEcht(36)
                .speler(new Vec3(0.5, 0, 1.5), 160f)
                .acteur("kapitein", E2SponsguhFeature.SPOOKKAPITEIN, new Vec3(-5.5, 31, -0.5), 20f)
                .camera(0, new Vec3(6.5, 2.0, 8.5), new Vec3(0.5, 6.0, -2.0))
                .camera(100, new Vec3(6.5, 1.6, 8.5), new Vec3(-3.0, 24.0, -1.0))
                .camera(145, new Vec3(6.4, 1.6, 8.4), new Vec3(-3.0, 30.0, -1.0))
                .cameraKnip(150, new Vec3(-5.5, 33.5, 6.5), new Vec3(-5.5, 32.4, -0.5))
                .camera(295, new Vec3(-5.3, 33.3, 5.8), new Vec3(-5.5, 32.4, -0.5))
                .cameraKnip(300, new Vec3(2.5, 1.8, 5.0), new Vec3(0.5, 1.4, 1.5))
                .camera(375, new Vec3(2.3, 1.8, 4.6), new Vec3(0.5, 1.4, 1.5))
                .cameraKnip(380, new Vec3(6.5, 2.0, 8.5), new Vec3(-1.0, 16.0, -0.5))
                .camera(470, new Vec3(6.2, 2.4, 8.2), new Vec3(-1.0, 24.0, -0.5))
                .zeg(30, "verteller", "mist", 60)
                .zeg(160, "kapitein", "boe", 60)
                .zeg(230, "kapitein", "levend", 60)
                .zeg(310, "kapitein", "boord", 60)
                .zeg(390, "verteller", "kolom", 60)
                .animatie("kapitein", 150, "spook")
                .animatie("kapitein", 230, "praat")
                .animatie("kapitein", 310, "lach")
                .geluid(5, SpAudio.geluid("stinger.verteller"), 1f, 1f)
                .geluid(20, () -> SoundEvents.BELL_BLOCK, 0.8f, 0.5f)
                .geluid(110, () -> SoundEvents.GHAST_AMBIENT, 0.5f, 0.5f)
                .geluid(155, () -> SoundEvents.GHAST_AMBIENT, 0.6f, 0.7f)
                .geluid(390, E2SponsguhFeature.BELLEN, 1f, 0.8f);
        for (int t = 0; t <= 140; t += 10) {
            b.deeltjes(t, ParticleTypes.ASH, new Vec3(0.5, 3.0, 0.5), 20, 4.0);
            b.deeltjes(t, ParticleTypes.SOUL, new Vec3(-3.0, 30.0 - t / 10.0, -1.0), 6, 3.0);
        }
        for (int t = 380; t <= 460; t += 10) {
            b.deeltjes(t, ParticleTypes.BUBBLE_POP, new Vec3(0.5, 2.0 + (t - 380) / 4.0, -0.5), 8, 0.4);
        }
        GUHLANDER = b.registreer();
    }

    /** The scary story by candle light: anchor = {@code bikinibodem/guhlander_dek} (the middle of the deck). */
    public static final Cutscene HAKKER;

    static {
        Cutscene.Builder b = Cutscene.maak(VOOR + "hakker").duur(600).bij(SpZijLijnen.GUHLANDER).verbergEcht(4)
                .speler(new Vec3(2.5, 0, 0.5), 90f)
                .acteur("kapitein", E2SponsguhFeature.SPOOKKAPITEIN, new Vec3(-0.5, 0, 0.5), -90f)
                .acteur("kaars", () -> EntityType.ITEM_DISPLAY, new Vec3(1.0, 0.1, 0.5), 0f,
                        tag -> tag.merge(SpZijEigen.dingData("minecraft:candle", 0.8f)))
                .zwart(0, 40)
                .camera(0, new Vec3(1.2, 1.8, 4.0), new Vec3(1.0, 1.2, 0.5))
                .camera(115, new Vec3(1.1, 1.7, 3.6), new Vec3(1.0, 1.2, 0.5))
                .cameraKnip(120, new Vec3(0.9, 1.9, 2.4), new Vec3(-0.5, 1.7, 0.5))
                .camera(260, new Vec3(0.6, 1.9, 2.0), new Vec3(-0.5, 1.8, 0.5))
                .camera(275, new Vec3(0.2, 1.9, 1.6), new Vec3(-0.5, 1.9, 0.5))
                .cameraKnip(330, new Vec3(1.2, 1.8, 4.0), new Vec3(1.0, 1.2, 0.5))
                .camera(470, new Vec3(1.4, 1.9, 4.4), new Vec3(1.0, 1.2, 0.5))
                .cameraKnip(475, new Vec3(4.5, 2.2, 3.0), new Vec3(1.0, 1.3, 0.5))
                .camera(600, new Vec3(4.8, 2.4, 3.4), new Vec3(1.5, 1.2, 0.5))
                .zeg(5, "verteller", "kaars", 50)
                .zeg(60, "kapitein", "verhaal", 60)
                .zeg(130, "kapitein", "nacht", 60)
                .zeg(200, "kapitein", "dichter", 60)
                .zeg(270, "kapitein", "boe", 50)
                .zeg(340, "kapitein", "uien", 60)
                .zeg(410, "kapitein", "lantaarns", 60)
                .zeg(485, "kapitein", "vraag", 65)
                .animatie("kapitein", 60, "praat")
                .animatie("kapitein", 130, "praat")
                .animatie("kapitein", 270, "spook")
                .animatie("kapitein", 340, "lach")
                .animatie("kapitein", 410, "praat")
                .animatie("kapitein", 485, "praat")
                .geluid(5, SpAudio.geluid("stinger.verteller"), 1f, 1f)
                .geluid(150, () -> SoundEvents.WOOD_HIT, 0.8f, 0.8f)
                .geluid(165, () -> SoundEvents.WOOD_HIT, 0.9f, 0.8f)
                .geluid(180, () -> SoundEvents.WOOD_HIT, 1f, 0.7f)
                .geluid(215, () -> SoundEvents.WOOD_HIT, 1f, 0.9f)
                .geluid(230, () -> SoundEvents.WOOD_HIT, 1f, 0.9f)
                .geluid(270, () -> SoundEvents.GHAST_AMBIENT, 0.7f, 0.9f)
                .geluid(565, () -> SoundEvents.PHANTOM_FLAP, 1f, 0.7f)
                .schud(270, 1.2f, 20);
        for (int t = 40; t <= 560; t += 20) {
            b.deeltjes(t, ParticleTypes.SMALL_FLAME, new Vec3(1.0, 0.75, 0.5), 2, 0.02);
        }
        b.deeltjes(565, ParticleTypes.CLOUD, new Vec3(2.5, 0.8, 0.5), 30, 0.6);
        HAKKER = b.registreer();
    }

    /** The two old heroes: anchor = {@code bikinibodem/zeemeerman} (Mosselguh stands two blocks south of him). */
    public static final Cutscene HELDEN = Cutscene.maak(VOOR + "helden").duur(470).bij(SpZijLijnen.ZEEMEERMAN).verbergEcht(5)
            .speler(new Vec3(-3.0, 0, 1.5), -90f)
            .acteur("zeemeerman", E2SponsguhFeature.ZEEMEERMAN_GUH, new Vec3(0.5, 0, 0.5), 90f)
            .acteur("mosselguh", E2SponsguhFeature.MOSSELGUH, new Vec3(0.5, 0, 2.5), 90f)
            .camera(0, new Vec3(-1.0, 2.2, 5.6), new Vec3(-0.6, 1.1, 1.5))
            .camera(135, new Vec3(-1.1, 2.1, 5.2), new Vec3(-0.6, 1.1, 1.5))
            .cameraKnip(140, new Vec3(-2.4, 1.9, 3.4), new Vec3(0.5, 1.1, 1.0))
            .camera(270, new Vec3(-2.2, 1.9, 3.1), new Vec3(0.5, 1.1, 1.2))
            .cameraKnip(275, new Vec3(-1.6, 1.7, 0.2), new Vec3(0.5, 1.0, 1.6))
            .camera(395, new Vec3(-1.5, 1.7, 0.5), new Vec3(0.5, 1.0, 1.6))
            .cameraKnip(400, new Vec3(-1.0, 2.2, 5.6), new Vec3(-0.6, 1.1, 1.5))
            .camera(470, new Vec3(-1.2, 2.3, 6.0), new Vec3(-0.6, 1.1, 1.5))
            .zeg(20, "zeemeerman", "ta_daa", 55)
            .zeg(80, "mosselguh", "en", 50)
            .zeg(150, "zeemeerman", "bezoek", 60)
            .zeg(215, "mosselguh", "riem", 60)
            .zeg(285, "zeemeerman", "kwijt", 60)
            .zeg(345, "mosselguh", "onzichtbaar", 50)
            .zeg(405, "zeemeerman", "ga", 55)
            .animatie("zeemeerman", 15, "held")
            .animatie("mosselguh", 75, "held")
            .animatie("zeemeerman", 150, "praat")
            .animatie("mosselguh", 215, "praat")
            .animatie("zeemeerman", 285, "schrik")
            .animatie("mosselguh", 345, "praat")
            .animatie("zeemeerman", 405, "held")
            .geluid(15, () -> SoundEvents.NOTE_BLOCK_PLING.value(), 0.8f, 1.2f)
            .geluid(80, () -> SoundEvents.NOTE_BLOCK_PLING.value(), 0.8f, 1.5f)
            .geluid(405, () -> SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 1.8f)
            .registreer();

    private SpZijScenes() {
    }

    /** From E2SpZijFeature.register (common code: both sides must know the scenes): loads this class. */
    static void register() {
        // (the scenes register themselves when this class loads)
    }
}
