package nl.juiced.guhs.feature.einde2.spd1start;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 5.2), slice sp_d1_start: the narrator card of part 1 and the six scenes of steps 0-4, main's
 * engine. Every spoken line is a {@code zeg} and waits for the viewer's click (1.4.4); the lengths below are the running time
 * without those waits. Positions are relative to the anchor named with each scene (template coordinates of Bikinibodem: the
 * copy is never turned). Texts: tools/features/einde2_sp_d1_start_tekst.py {@code SCENES}.
 * <ul>
 *   <li>{@link #BRIEF} (20.7 s): the bubble pops, Sponsguh's voice reads his job ad, the narrator. Anchor: the player's block
 *       (anywhere in the Guheinde: the camera steps out of walls).</li>
 *   <li>{@link #AANKOMST} (28 s): over the dome and down into the town, De Krokante Knabbel, the bubbelstroom. Anchor:
 *       {@code aankomst_speler} on the island above the dome.</li>
 *   <li>{@link #WELKOM} (27.2 s): Sponsguh runs up, Gerrit creeps up ("njeg"), Patguh, off to work. Anchor: {@code bubbel_onder}
 *       on the plein.</li>
 *   <li>{@link #SOLLICITATIE} (29.8 s): Krabguh counts his knabbels, the interview, Octo sighs at the till, the cap. Anchor:
 *       {@code krabguh} in his office.</li>
 *   <li>{@link #BAKKEN_UITLEG} (21.4 s): Sponsguh's grill lesson, the five crates. Anchor: {@code grill_speler}.</li>
 *   <li>{@link #BAKKEN_KLAAR} (13.1 s): "een natuurtalent!", Octo sighs through the hatch, Patguh jumps outside. Anchor:
 *       {@code grill_speler}.</li>
 * </ul>
 * Speakers without an actor ({@code verteller}, Sponsguh's voice in the letter) are named by the scene's own name keys.
 * The cast's story animations ({@code rennen}, {@code huil_blij}, ...) are sp_cast's: until it is merged they play nothing.
 */
public final class D1Scenes {
    private static final String LIJN = SpVerhaal.RECEPT;

    private D1Scenes() {
    }

    private static Cutscene.Builder maak(String naam) {
        return Cutscene.maak("einde2_sp_d1_start_" + naam).bij(LIJN).kaart(E2SpD1Start.KAART);
    }

    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    /** An item display as an actor (the bubble, the letter, the crates' contents). */
    private static Consumer<CompoundTag> item(String id, float schaal, String billboard) {
        return tag -> {
            CompoundTag item = new CompoundTag();
            item.putString("id", id);
            item.putInt("count", 1);
            tag.put("item", item);
            tag.putString("item_display", "fixed");
            tag.putString("billboard", billboard);
            tag.put("transformation", transformatie(schaal, 0f));
        };
    }

    /** A crate (a barrel standing up, scaled down) as an actor. */
    private static Consumer<CompoundTag> krat() {
        return tag -> {
            CompoundTag state = new CompoundTag();
            state.putString("Name", "minecraft:barrel");
            CompoundTag props = new CompoundTag();
            props.putString("facing", "up");
            props.putString("open", "false");
            state.put("Properties", props);
            tag.put("block_state", state);
            tag.put("transformation", transformatie(Bakken.KRAT_SCHAAL, -Bakken.KRAT_SCHAAL / 2f));
        };
    }

    private static CompoundTag transformatie(float schaal, float verschuif) {
        CompoundTag t = new CompoundTag();
        t.put("translation", floats(verschuif, 0, verschuif));
        t.put("left_rotation", floats(0, 0, 0, 1));
        t.put("scale", floats(schaal, schaal, schaal));
        t.put("right_rotation", floats(0, 0, 0, 1));
        return t;
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float f : values) {
            list.add(FloatTag.valueOf(f));
        }
        return list;
    }

    // =====================================================================================================================
    // brief: anchor = the player's block (anywhere in the Guheinde). The bubble hangs in front of the player (north), pops,
    // the letter drifts to them; then the camera rises and looks out over the void, where Bikinibodem lies deep below.
    // =====================================================================================================================
    private static final Vec3 BEL = v(0.5, 1.5, -1.8);

    public static final Cutscene BRIEF = maak("brief").duur(414).verbergEcht(3).cameraOntwijkt()
            .speler(v(0.5, 0, 0.5), 180f)
            .acteur("bel", () -> EntityType.ITEM_DISPLAY, BEL, 0f, item("guhs:zeepbel", 0.9f, "fixed"))
            .acteur("brief", () -> EntityType.ITEM_DISPLAY, BEL.add(0, 0.05, 0), 0f, item("minecraft:paper", 0.45f, "vertical"))
            .zwart(0, 14)
            .camera(0, v(2.2, 2.0, 1.6), BEL)
            .camera(40, v(1.8, 1.8, 0.9), BEL)
            .deeltjes(10, ParticleTypes.END_ROD, BEL, 8, 0.5)
            .geluid(30, E2SponsguhFeature.BELLEN, 1.0f, 1.3f)
            .geluid(30, () -> SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 1.0f)
            .deeltjes(30, ParticleTypes.BUBBLE_POP, BEL, 40, 0.5)
            .deeltjes(30, ParticleTypes.SPLASH, BEL, 20, 0.4)
            .loop("bel", 30, 31, v(0.5, -8, -1.8))
            .loop("brief", 32, 70, v(0.5, 1.3, -1.0))
            .zeg(36, "verteller", "plop", 70)
            .cameraKnip(110, v(1.25, 1.55, -0.2), v(0.5, 1.35, -1.0))
            .zeg(112, "sponsguh", "vacature", 90)
            .zeg(206, "sponsguh", "eisen", 90)
            .geluid(206, E2SponsguhFeature.SPONSGUH_LACH, 0.8f, 1.2f)
            .cameraKnip(300, v(3.5, 4.0, 4.5), v(0.5, -2.0, -24.0))
            .camera(414, v(3.0, 6.5, 6.0), v(0.5, -10.0, -30.0))
            .geluid(300, SpAudio.geluid("stinger.verteller"), 1.0f, 1.0f)
            .zeg(304, "verteller", "bikinibodem", 92)
            .zwart(400, 414)
            .registreer();

    // =====================================================================================================================
    // aankomst: anchor = aankomst_speler (template 106, 108, 112), the middle of the island above the dome. The dome's top is
    // 27 below, the town's floor 92 below (the plein right under the island); the door of De Krokante Knabbel is at
    // (33.5, -92, -6); the bubbelstroom down starts at (0, 0, -4) (its tube is open to the south).
    // =====================================================================================================================
    public static final Cutscene AANKOMST = maak("aankomst").duur(560).verbergEcht(8)
            .speler(v(0.5, 0, 0.5), 180f)
            .zwart(0, 16)
            // the island and the bubble from outside
            .camera(0, v(26, 16, 36), v(0.5, -4, -2))
            .camera(150, v(16, 9, 24), v(0.5, -2, -2))
            .zeg(20, "verteller", "eiland", 110)
            // inside the dome, down to the town
            .cameraKnip(150, v(14, -44, 30), v(2, -92, -2))
            .camera(300, v(6, -62, 20), v(10, -92, -8))
            .deeltjes(152, ParticleTypes.END_ROD, v(8, -60, 14), 30, 6.0)
            .zeg(160, "verteller", "stad", 120)
            // De Krokante Knabbel
            .cameraKnip(300, v(20, -84, 8), v(33.5, -91.2, -6))
            .camera(440, v(26, -86, 4), v(33.5, -91.2, -6))
            .zeg(310, "verteller", "knabbel", 110)
            // back on the island: the bubbelstroom
            .cameraKnip(440, v(-2.5, 2.4, 4.5), v(0.5, 1.0, -3.5))
            .camera(560, v(-1.5, 2.0, 3.0), v(0.5, 1.2, -3.5))
            .deeltjes(444, ParticleTypes.BUBBLE_POP, v(0.5, 1.5, -4.0), 30, 0.8)
            .geluid(444, E2SponsguhFeature.BELLEN, 1.0f, 0.9f)
            .zeg(446, "verteller", "bubbel", 100)
            .zwart(548, 560)
            .registreer();

    // =====================================================================================================================
    // welkom: anchor = bubbel_onder (106, 16, 109), right south of the tube of the stream down (x -2..2, z -5..-1). The
    // Hoofdstraat runs east (z +3); the tube up is at z 8..11. Sponsguh runs in from the east, Gerrit creeps after him the whole
    // scene, Patguh comes round the tube up from the north-west.
    // =====================================================================================================================
    public static final Cutscene WELKOM = maak("welkom").duur(544).verbergEcht(5)
            .speler(v(0.5, 0, 0.5), -90f)
            .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, v(18.5, 0, 3.5), 90f)
            .acteur("gerrit", E2SponsguhFeature.GERRIT, v(21.5, 0, 4.0), 90f)
            .acteur("patguh", E2SponsguhFeature.PATGUH, v(-5.5, 0, 10.5), 160f)
            .zwart(0, 10)
            .camera(0, v(-3.5, 2.6, 6.5), v(6, 1.0, 1.5))
            .camera(100, v(-2.6, 2.2, 5.0), v(4, 0.9, 1.2))
            .loop("sponsguh", 8, 64, v(2.5, 0, 0.8))
            .animatie("sponsguh", 8, "rennen")
            .loop("gerrit", 10, 300, v(4.0, 0, 2.2))
            .kijk("sponsguh", 66, v(0.5, 1.2, 0.5))
            .animatie("sponsguh", 66, "juich")
            .geluid(66, E2SponsguhFeature.SPONSGUH_LACH, 1.0f, 1.1f)
            .zeg(20, "sponsguh", "hallo", 80)
            .cameraKnipVolgt(104, v(0.9, 1.3, 2.6), "sponsguh")
            .animatie("sponsguh", 106, "praat")
            .zeg(104, "sponsguh", "ik_ben", 90)
            // Gerrit, still on his way
            .cameraKnipVolgt(198, v(7.0, 0.9, 5.2), "gerrit")
            .geluid(200, E2SponsguhFeature.GERRIT_NJEG, 1.0f, 1.0f)
            .zeg(198, "gerrit", "njeg", 40)
            .zeg(240, "sponsguh", "gerrit", 70)
            // Patguh
            .loop("patguh", 230, 300, v(-0.8, 0, 2.6))
            .kijk("patguh", 302, v(0.5, 1.2, 0.5))
            .cameraKnipVolgt(312, v(1.8, 1.9, 5.8), "patguh")
            .animatie("patguh", 314, "juich")
            .zeg(312, "patguh", "patguh", 90)
            // off to work
            .cameraKnip(406, v(-4.5, 3.0, 5.5), v(3.0, 1.0, 1.2))
            .animatie("sponsguh", 408, "spring")
            .zeg(406, "sponsguh", "mee", 90)
            .loop("sponsguh", 500, 540, v(9.5, 0, 3.5))
            .loop("patguh", 504, 544, v(6.5, 0, 3.5))
            .zwart(530, 544)
            .registreer();

    // =====================================================================================================================
    // sollicitatie: anchor = krabguh (147, 16, 87) in his office (x -4..4, z -4..2; the desk at z -2, the door at z 3). Octo
    // stands behind the till in the hall at (-6.5, 0, 4.5) (octo_kassa), seen across the counter from the hall.
    // =====================================================================================================================
    public static final Cutscene SOLLICITATIE = maak("sollicitatie").duur(596).verbergEcht(9)
            .acteur("krabguh", E2SponsguhFeature.MENEER_KRABGUH, v(0.5, 0, 0.5), 180f)
            .speler(v(0.5, 0, 2.5), 180f)
            .acteur("octo", E2SponsguhFeature.OCTO_MIKA, v(-6.5, 0, 4.5), 0f)
            .zwart(0, 10)
            // counting his knabbels, his back to the door
            .camera(0, v(-1.6, 1.5, -1.0), v(0.5, 0.9, 0.5))
            .animatie("krabguh", 0, "tel_geld")
            .geluid(12, () -> SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f)
            .zeg(12, "krabguh", "tellen", 80)
            // he turns round: a job seeker
            .kijk("krabguh", 96, v(0.5, 1.6, 2.5))
            .animatie("krabguh", 96, "schrik")
            .cameraKnip(96, v(-1.8, 2.0, 2.9), v(0.5, 0.9, 0.6))
            .zeg(98, "krabguh", "sollicitant", 70)
            .cameraKnipVolgt(172, v(1.9, 1.3, 1.6), "krabguh")
            .animatie("krabguh", 174, "praat")
            .zeg(172, "krabguh", "vragen", 100)
            // you nod
            .cameraKnip(276, v(2.5, 2.4, 0.0), v(0.5, 1.5, 2.5))
            .zeg(276, "verteller", "knik", 50)
            // hired, and the cap
            .cameraKnip(330, v(3.2, 2.1, 2.6), v(0.5, 1.0, 1.5))
            .animatie("krabguh", 332, "juich")
            .zeg(330, "krabguh", "aangenomen", 90)
            .deeltjes(350, ParticleTypes.HAPPY_VILLAGER, v(0.5, 2.0, 2.5), 12, 0.3)
            .geluid(350, () -> SoundEvents.ITEM_PICKUP, 0.8f, 1.2f)
            // Octo at the till
            .cameraKnip(424, v(-6.5, 2.4, 9.0), v(-6.5, 2.5, 4.5))
            .animatie("octo", 426, "zucht")
            .geluid(430, E2SponsguhFeature.OCTO_KLARINET, 0.7f, 0.6f)
            .zeg(426, "octo", "zucht", 70)
            // to the grill!
            .cameraKnip(500, v(-1.8, 2.0, 2.9), v(0.5, 0.9, 0.6))
            .animatie("krabguh", 502, "praat")
            .zeg(500, "krabguh", "grill", 80)
            .zwart(584, 596)
            .registreer();

    // =====================================================================================================================
    // bakken_uitleg: anchor = grill_speler (134, 16, 84), the grill one block north (z -1). The kitchen is x -6..7, z -1..5;
    // the five crates stand round the spot (this slice's markers d1s_krat_1..5, at the offsets of Bakken.KRATTEN_REL).
    // =====================================================================================================================
    public static final Cutscene BAKKEN_UITLEG;

    static {
        Cutscene.Builder b = maak("bakken_uitleg").duur(428).verbergEcht(4)
                .speler(v(0.5, 0, 0.5), 180f)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, v(2.5, 0, 0.5), 90f);
        GrillSpel.Deel[] delen = GrillSpel.Deel.values();
        for (int i = 0; i < delen.length; i++) {
            Vec3 k = Bakken.KRATTEN_REL.get(i);
            b.acteur("krat" + i, () -> EntityType.BLOCK_DISPLAY, k.add(0.5, 0, 0.5), 0f, krat());
            b.acteur("deel" + i, () -> EntityType.ITEM_DISPLAY, k.add(0.5, Bakken.ITEM_HOOGTE, 0.5), 0f, item(delen[i].item, 0.5f, "vertical"));
        }
        BAKKEN_UITLEG = b
                .camera(0, v(-2.5, 2.1, 4.2), v(1.5, 0.9, 0.3))
                .animatie("sponsguh", 8, "juich")
                .zeg(8, "sponsguh", "keuken", 70)
                // the grill and the ticket
                .cameraKnip(82, v(0.5, 2.0, 1.9), v(0.5, 0.9, -0.6))
                .geluid(84, E2SponsguhFeature.GRILL_SIS, 0.8f, 1.0f)
                .deeltjes(84, ParticleTypes.SMOKE, v(0.5, 1.1, -0.5), 10, 0.2)
                .animatie("sponsguh", 84, "bak")
                .zeg(82, "sponsguh", "bonnetje", 90)
                // the crates
                .cameraKnip(176, v(0.5, 2.7, -0.3), v(0.6, 0.4, 2.0))
                .camera(280, v(0.9, 2.8, -0.4), v(0.8, 0.4, 2.0))
                .zeg(176, "sponsguh", "kratten", 100)
                // a wrong part, no harm done
                .cameraKnipVolgt(280, v(1.2, 1.2, 2.3), "sponsguh")
                .animatie("sponsguh", 282, "praat")
                .zeg(280, "sponsguh", "fout", 90)
                .animatie("sponsguh", 374, "huil_blij")
                .zeg(374, "sponsguh", "trots", 54)
                .registreer();
    }

    // =====================================================================================================================
    // bakken_klaar: anchor = grill_speler. Octo leans into the serving hatch (x -1..2, z 6, one and two blocks up) from the hall;
    // Patguh jumps on the Hoofdstraat in front of the restaurant's door (5.5, 0, 24.5).
    // =====================================================================================================================
    public static final Cutscene BAKKEN_KLAAR = maak("bakken_klaar").duur(262).verbergEcht(10)
            .speler(v(0.5, 0, 0.5), -90f)
            .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, v(2.5, 0, 0.5), 90f)
            .acteur("octo", E2SponsguhFeature.OCTO_MIKA, v(1.5, 0, 7.5), 180f)
            .acteur("patguh", E2SponsguhFeature.PATGUH, v(5.5, 0, 24.5), 0f)
            .camera(0, v(-1.5, 1.9, 3.2), v(1.6, 0.9, 0.5))
            .animatie("sponsguh", 4, "juich")
            .geluid(4, E2SponsguhFeature.SPONSGUH_LACH, 1.0f, 1.0f)
            .zeg(8, "sponsguh", "natuurtalent", 80)
            .cameraKnip(92, v(0.8, 2.3, 3.6), v(1.5, 2.4, 7.0))
            .animatie("octo", 94, "zucht")
            .zeg(92, "octo", "twee", 70)
            .cameraKnip(166, v(3.0, 2.0, 29.0), v(5.5, 1.1, 24.5))
            .animatie("patguh", 168, "spring")
            .animatie("patguh", 190, "spring")
            .animatie("patguh", 212, "spring")
            .zeg(170, "patguh", "kwallen", 80)
            .zwart(250, 262)
            .registreer();

    /** Every story scene of the slice (the dev command, the test). */
    public static final List<Cutscene> ALLE = List.of(BRIEF, AANKOMST, WELKOM, SOLLICITATIE, BAKKEN_UITLEG, BAKKEN_KLAAR);

    // =====================================================================================================================
    // the look scenes of the step hints (StapHint, ~3 s, one caption each)
    // =====================================================================================================================
    public static final Cutscene KIJK_BUBBEL = StapHint.kijkScene("einde2_sp_d1_start_kijk_bubbel", true);
    public static final Cutscene KIJK_KNABBEL = StapHint.kijkScene("einde2_sp_d1_start_kijk_knabbel", true);

    /** (register) The narrator card of part 1 (four lines and the drawn map of Bikinibodem); the scenes register on load. */
    static void registreer() {
        Verteller.registreer(E2SpD1Start.KAART, 4, LIJN);
    }
}
