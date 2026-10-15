package nl.juiced.guhs.feature.ringh5;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.ring.Cast;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-h5): the narrator card and the three camera scenes of chapter 5 (texts: tools/features/ring_h5_tekst.py; the
 * cast and its named animations: feature/ring, guhs_work130/reports/slice_ring-kern.md section 8). Every position is
 * relative to the scene's anchor block, in the coordinates of the unturned build (the engine turns them with the copy).
 * <ul>
 *   <li>{@link #BOROMIKA} (anchor: the camp fire, {@link Plekken#VUUR_KAMP}): Boromika's moment. He means to help, the ring
 *       gets the better of him, he reaches for it, Sam-guh jumps in between, and he is deeply ashamed.</li>
 *   <li>{@link #OOG} (anchor: the crest of the ridge, {@link Plekken#UITKIJK}): Smikagol shows the valley: the gate, the Eye
 *       (the real one: the camera flies up to it), its light on the ground.</li>
 *   <li>{@link #GUHDALF} (anchor: in front of the side door, {@link Plekken#DEUR_SCENE}): the door is locked, the Nine ride
 *       in, and Guhdalf de Witte returns on top of the wall: he blinds the riders and draws the Eye's gaze.</li>
 * </ul>
 */
public final class Scenes {
    /** The narrator card of the chapter (the drawn map) and how many lines it has. */
    public static final String KAART = "ring_h5";
    public static final int KAART_REGELS = 4;

    private static final String LIJN = "ring_h5";

    // --- Boromika's moment --------------------------------------------------------------------------------------------------
    // the fire is the anchor; Boromika sits at (4.5, 0, -0.5), the player stands south of the fire, Sam-guh beside them
    public static final Cutscene BOROMIKA = Cutscene.maak("ringh5_boromika").duur(600).bij(LIJN).kaart(KAART).verbergEcht(12)
            .speler(new Vec3(0.5, 0, 3.5), 180)
            .npc("boromika", GuhNpcEntity.Kind.BOROMIKA, new Vec3(4.5, 0, -0.5), 120)
            .guh("sam", GuhVariant.SAM_GUH, new Vec3(-2.5, 0, 2.5), 250)
            .camera(0, new Vec3(7.0, 3.2, 8.5), new Vec3(1.5, 1.0, 1.5))
            .camera(140, new Vec3(5.0, 2.2, 7.0), new Vec3(1.5, 1.0, 2.0))
            .cameraKnip(150, new Vec3(-0.5, 1.5, 6.0), new Vec3(2.5, 0.8, 2.5))
            .camera(235, new Vec3(-0.2, 1.4, 5.6), new Vec3(2.5, 0.8, 2.5))
            .cameraKnip(240, new Vec3(4.5, 2.6, 6.5), new Vec3(1.0, 0.8, 3.5))
            .camera(310, new Vec3(4.5, 2.4, 6.0), new Vec3(1.5, 0.8, 3.0))
            .cameraKnipVolgt(318, new Vec3(1.0, 1.0, 5.0), "boromika")
            .cameraVolgt(480, new Vec3(0.2, 1.2, 4.2), "boromika")
            .cameraKnip(486, new Vec3(6.5, 2.8, 7.5), new Vec3(1.5, 0.8, 2.5))
            .camera(590, new Vec3(8.0, 3.6, 9.0), new Vec3(1.5, 0.8, 2.5))
            .kijk(Cutscene.SPELER, 0, new Vec3(0.5, 1, 0.5))
            .kijk("sam", 0, new Vec3(0.5, 1, 0.5))
            .zeg(10, "", "begin", 55)
            .loop("boromika", 24, 68, new Vec3(2.5, 0, 2.5))
            .kijk("boromika", 68, new Vec3(0.5, 1, 3.5))
            .kijk(Cutscene.SPELER, 60, new Vec3(2.5, 1, 2.5))
            .animatie("boromika", 72, "praat")
            .zeg(72, "boromika", "tonen", 72)
            .animatie("boromika", 150, "wijs")
            .zeg(152, "boromika", "hapje", 84)
            .animatie("boromika", 240, "grijp")
            .geluid(240, ModSounds.MIKA_AMBIENT, 1.2f, 0.7f)
            .loop(Cutscene.SPELER, 242, 256, new Vec3(0.5, 0, 4.6))
            .loop("sam", 238, 254, new Vec3(1.5, 0, 3.6))
            .kijk("sam", 254, new Vec3(2.5, 1, 2.5))
            .animatie("sam", 256, "ruzie")
            .schud(244, 0.6f, 12)
            .zeg(250, "sam", "foei", 58)
            .animatie("boromika", 312, "schrik")
            .animatie("sam", 318, "")
            .zeg(322, "boromika", "wat_deed_ik", 74)
            .animatie("boromika", 398, "schaam")
            .zeg(402, "boromika", "vergeef", 80)
            .animatie("boromika", 486, "buig")
            .zeg(490, "boromika", "wacht", 92)
            .animatie(Cutscene.SPELER, 560, "zwaai")
            .zwart(586, 600)
            .registreer();

    // --- the first look at the Eye ------------------------------------------------------------------------------------------
    // the crest of the ridge is the anchor; the valley lies towards +z: the gate at z + 56, the Eye at (0.5, 40, 69.5)
    private static final Vec3 OOG_REL = new Vec3(0.5, 39.6, 69.0), POORT_REL = new Vec3(0.5, 6.0, 56.0);
    public static final Cutscene OOG = Cutscene.maak("ringh5_oog").duur(560).bij(LIJN).kaart(KAART).verbergEcht(6)
            .speler(new Vec3(0.5, 0, 0.5), 0)
            .acteur("smikagol", RingFeature.SMIKAGOL, new Vec3(1.7, 0, 1.2), 0)
            .camera(0, new Vec3(-0.6, 2.0, -3.6), new Vec3(0.5, 5.0, 30.0))
            .camera(78, new Vec3(-0.4, 2.6, -2.6), new Vec3(0.5, 7.0, 40.0))
            .camera(80, new Vec3(0.5, 9.0, 14.0), POORT_REL)
            .camera(198, new Vec3(0.5, 12.0, 40.0), new Vec3(0.5, 9.0, 56.0))
            .cameraKnip(202, new Vec3(0.5, 34.0, 50.0), OOG_REL)
            .camera(306, new Vec3(1.5, 38.5, 59.0), OOG_REL)
            .cameraKnip(312, new Vec3(-16.0, 24.0, 4.0), new Vec3(-16.0, -3.0, 20.0))
            .camera(428, new Vec3(-22.0, 22.0, 10.0), new Vec3(-22.0, -3.0, 24.0))
            .cameraKnip(434, new Vec3(1.8, 1.4, 4.4), new Vec3(1.0, 0.8, 0.8))
            .camera(550, new Vec3(2.4, 1.6, 5.2), new Vec3(1.0, 0.8, 0.8))
            .animatie("smikagol", 6, "wijs")
            .zeg(10, "smikagol", "daar", 66)
            .zeg(100, "", "dicht", 70)
            .schud(204, 0.5f, 30)
            .geluid(204, () -> SoundEvents.WARDEN_HEARTBEAT, 1.5f, 0.6f)
            .geluid(236, () -> SoundEvents.WARDEN_HEARTBEAT, 1.5f, 0.6f)
            .zeg(212, "smikagol", "oog", 92)
            .zeg(318, "smikagol", "licht", 108)
            .kijk("smikagol", 434, new Vec3(0.5, 1, 0.5))
            .kijk(Cutscene.SPELER, 434, new Vec3(1.7, 0.6, 1.2))
            .animatie("smikagol", 436, "sluip")
            .zeg(438, "smikagol", "sluipweg", 106)
            .zwart(546, 560)
            .registreer();

    // --- Guhdalf de Witte returns -------------------------------------------------------------------------------------------
    // the cell in front of the side door is the anchor: the door at z + 3 (x - 1 .. 1), the top of the wall's parapet at y + 18,
    // the lane runs away towards +x (the gap of het Wachthek at x + 12, z - 2 .. - 1)
    private static final Vec3 OP_DE_MUUR = new Vec3(0.5, 18.0, 3.5);
    public static final Cutscene GUHDALF = Cutscene.maak("ringh5_guhdalf").duur(620).bij(LIJN).kaart(KAART).verbergEcht(40)
            .speler(new Vec3(0.5, 0, 0.5), 0)
            .acteur("smikagol", RingFeature.SMIKAGOL, new Vec3(0.5, 0, 2.3), 0)
            .guh("sam", GuhVariant.SAM_GUH, new Vec3(-1.4, 0, 0.3), 320)
            .acteur("guhdalf", ModEntities.GUH_NPC, OP_DE_MUUR, 180, Cast.acteur(GuhNpcEntity.Kind.GUHDALF, "wit"))
            .acteur("ruiter1", RingFeature.KNEKEL_RUITER, new Vec3(30.5, 0, -1.5), 90)
            .acteur("ruiter2", RingFeature.KNEKEL_RUITER, new Vec3(34.5, 0, -2.5), 90)
            .acteur("ruiter3", RingFeature.KNEKEL_RUITER, new Vec3(38.5, 0, -1.5), 90)
            .acteur("ruiter4", RingFeature.KNEKEL_RUITER, new Vec3(42.5, 0, -2.5), 90)
            .acteur("ruiter5", RingFeature.KNEKEL_RUITER, new Vec3(46.5, 0, -1.5), 90)
            .camera(0, new Vec3(4.2, 1.7, -2.6), new Vec3(0.5, 1.0, 2.5))
            .camera(78, new Vec3(3.6, 1.6, -1.6), new Vec3(0.5, 1.0, 2.5))
            .cameraKnip(84, new Vec3(2.2, 1.5, 1.6), new Vec3(20.0, 1.2, -1.5))
            .camera(236, new Vec3(1.6, 1.6, 1.8), new Vec3(12.0, 1.4, -1.5))
            .cameraKnip(250, new Vec3(2.5, 1.0, -7.0), OP_DE_MUUR.add(0, 0.9, 0))
            .camera(352, new Vec3(1.5, 3.0, -5.0), OP_DE_MUUR.add(0, 0.9, 0))
            .cameraKnip(358, new Vec3(3.0, 3.2, 3.0), new Vec3(9.5, 1.2, -1.5))
            .camera(430, new Vec3(3.0, 4.0, 3.0), new Vec3(16.0, 1.2, -1.5))
            .cameraKnip(436, new Vec3(22.0, 30.0, -8.0), new Vec3(35.5, 42.5, 15.5))
            .camera(500, new Vec3(26.0, 34.0, -2.0), new Vec3(35.5, 42.5, 15.5))
            .cameraKnip(506, new Vec3(3.4, 1.7, -2.4), new Vec3(0.5, 1.0, 2.5))
            .camera(610, new Vec3(2.6, 1.6, -1.2), new Vec3(0.5, 1.0, 4.5))
            .kijk("sam", 0, new Vec3(0.5, 1, 2.5))
            .animatie("smikagol", 4, "grijp")
            .geluid(6, () -> SoundEvents.IRON_DOOR_CLOSE, 0.8f, 1.4f)
            .zeg(8, "smikagol", "op_slot", 72)
            .geluid(60, () -> SoundEvents.SKELETON_HORSE_AMBIENT, 1.4f, 0.5f)
            .kijk(Cutscene.SPELER, 84, new Vec3(20, 1, -1.5))
            .kijk("sam", 84, new Vec3(20, 1, -1.5))
            .loop("ruiter1", 60, 240, new Vec3(8.5, 0, -1.5))
            .loop("ruiter2", 70, 244, new Vec3(11.5, 0, -1.5))
            .loop("ruiter3", 80, 248, new Vec3(14.5, 0, -1.5))
            .loop("ruiter4", 90, 250, new Vec3(17.5, 0, -1.5))
            .loop("ruiter5", 100, 250, new Vec3(20.5, 0, -1.5))
            .zeg(92, "", "hoeven", 66)
            .animatie("sam", 164, "schrik")
            .zeg(166, "sam", "schiet_op", 60)
            // the light on the wall
            .deeltjes(250, ParticleTypes.END_ROD, OP_DE_MUUR.add(0, 1.2, 0), 80, 1.2)
            .geluid(250, () -> SoundEvents.BEACON_ACTIVATE, 1.6f, 1.4f)
            .geluid(252, () -> SoundEvents.TOTEM_USE, 0.7f, 1.2f)
            .schud(250, 1.0f, 16)
            .animatie("guhdalf", 252, "toover")
            .zeg(262, "guhdalf", "achteruit", 90)
            .animatie("ruiter1", 300, "schrik")
            .animatie("ruiter2", 304, "schrik")
            .animatie("ruiter3", 308, "schrik")
            .animatie("ruiter4", 312, "schrik")
            .animatie("ruiter5", 316, "schrik")
            .deeltjes(358, ParticleTypes.END_ROD, new Vec3(10.0, 1.6, -1.5), 60, 2.0)
            .zeg(364, "", "verblind", 62)
            .loop("ruiter1", 372, 470, new Vec3(30.5, 0, -1.5))
            .loop("ruiter2", 368, 470, new Vec3(34.5, 0, -1.5))
            .loop("ruiter3", 364, 470, new Vec3(38.5, 0, -1.5))
            .loop("ruiter4", 360, 470, new Vec3(42.5, 0, -1.5))
            .loop("ruiter5", 358, 470, new Vec3(46.5, 0, -1.5))
            // "kijk eens hier, Sausron": the Eye turns (the real one; Hoofdstuk points its light at the wall for the scene)
            .geluid(440, () -> SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.6f, 1.0f)
            .geluid(456, () -> SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.6f, 1.0f)
            .zeg(440, "guhdalf", "vuurwerk", 62)
            .kijk(Cutscene.SPELER, 506, new Vec3(0.5, 1, 2.5))
            .kijk("sam", 506, new Vec3(0.5, 1, 2.5))
            .animatie("sam", 506, "")
            .animatie("guhdalf", 506, "wijs")
            .zeg(510, "guhdalf", "ren", 88)
            .geluid(540, () -> SoundEvents.IRON_DOOR_OPEN, 1.0f, 0.8f)
            .animatie("smikagol", 544, "juich")
            .loop("smikagol", 562, 604, new Vec3(0.5, 0, 7.5))
            .zwart(606, 620)
            .registreer();

    /** Registers the card (the scenes register themselves when this class loads). */
    static void registreer() {
        Verteller.registreer(KAART, KAART_REGELS, LIJN);
    }

    private Scenes() {
    }
}
