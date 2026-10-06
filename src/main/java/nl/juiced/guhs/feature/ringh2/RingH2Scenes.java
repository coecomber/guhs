package nl.juiced.guhs.feature.ringh2;

import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * bbq2 (ring-h2): the two cutscenes of Guhvendel (texts scene.guhs.&lt;id&gt;.*: tools/features/ring_h2.py). Both are
 * anchored on the stone table in the middle of the council ring ({@link Guhvendel#KRING}); every position below is a
 * template coordinate relative to that block (y 0 = the floor of the ring, x east, z south; the high seat is at z -5, the
 * entrance at x -7), turned with the copy by the engine. The real characters around the ring are hidden for the viewer
 * while a scene plays.
 * <ul>
 *   <li>{@link #RAAD} "De Raad van Guhrond" (after the bell): the ring is laid on the stone, Boromika wants to eat it
 *       "for safety", Gimguh bites it and loses a tooth, and then everybody argues about who may eat it.</li>
 *   <li>{@link #GENOOTSCHAP} "Het Reisgenootschap" (after the player said "ik neem de ring wel mee"): one by one they
 *       join, Sam-guh jumps out of the bushes, Merrie and Pippguh run in, and Guhrond counts nine.</li>
 * </ul>
 * The ring on the stone is an item display actor ({@link #ring}); the animations are ring-kern's named ones
 * (feature/ring/client/CastAnimaties).
 */
public final class RingH2Scenes {
    private static final String GUHROND = "guhrond", GUHDALF = "guhdalf", ARAGUH = "araguh", LEGUHLAS = "leguhlas", GIMGUH = "gimguh",
            BOROMIKA = "boromika", MERRIE = "merrie", PIPPGUH = "pippguh", SAM = "sam", RING = "ring", SPELER = Cutscene.SPELER;

    // the seats (the council set of the template sits on exactly these)
    private static final Vec3 Z_GUHROND = new Vec3(0.5, 0.5, -4.5), Z_GUHDALF = new Vec3(3.5, 0.5, -3.5), Z_ARAGUH = new Vec3(-2.5, 0.5, -3.5),
            Z_LEGUHLAS = new Vec3(5.5, 0.5, -0.5), Z_GIMGUH = new Vec3(5.5, 0.5, 1.5), Z_BOROMIKA = new Vec3(3.5, 0.5, 4.5);
    /** The ring on the stone, and where it waits before it is laid there (inside the stone: nobody sees it). */
    private static final Vec3 OP_STEEN = new Vec3(0.5, 1.3, 0.5), IN_STEEN = new Vec3(0.5, 0.25, 0.5);
    /** Where the player stands at the stone, the entrance, and the hiding places outside the ring. */
    private static final Vec3 BIJ_STEEN = new Vec3(-0.9, 0, 0.5), INGANG = new Vec3(-5.5, 0, 0.5), STRUIK_SAM = new Vec3(-8.0, -1, 4.5),
            ACHTER_MERRIE = new Vec3(8.5, -1, 4.5), ACHTER_PIPPGUH = new Vec3(9.5, -1, 2.5);
    private static final Vec3 KIJK_STEEN = new Vec3(0.5, 1.2, 0.5), KIJK_GUHROND = new Vec3(0.5, 1.3, -4.5);

    public static final Cutscene RAAD = raad();
    public static final Cutscene GENOOTSCHAP = genootschap();

    /** The Knabbelring as an actor: an item display that turns towards the camera. */
    static Consumer<CompoundTag> ring() {
        return tag -> {
            CompoundTag item = new CompoundTag();
            item.putString("id", "guhs:knabbelring");
            item.putInt("count", 1);
            tag.put("item", item);
            tag.putString("item_display", "ground");
            tag.putString("billboard", "vertical");
            CompoundTag vorm = new CompoundTag();
            vorm.put("translation", floats(0f, 0f, 0f));
            vorm.put("left_rotation", floats(0f, 0f, 0f, 1f));
            vorm.put("scale", floats(1.4f, 1.4f, 1.4f));
            vorm.put("right_rotation", floats(0f, 0f, 0f, 1f));
            tag.put("transformation", vorm);
        };
    }

    private static ListTag floats(float... waarden) {
        ListTag list = new ListTag();
        for (float f : waarden) {
            list.add(FloatTag.valueOf(f));
        }
        return list;
    }

    private static Cutscene.Builder raadskring(String id, int duur) {
        return Cutscene.maak(id).duur(duur).bij("ring_h2").kaart(Guhvendel.KAART).verbergEcht(14)
                .npc(GUHROND, Kind.GUHROND, Z_GUHROND, 0)
                .npc(GUHDALF, Kind.GUHDALF, Z_GUHDALF, 45)
                .npc(ARAGUH, Kind.ARAGUH, Z_ARAGUH, 315)
                .npc(LEGUHLAS, Kind.LEGUHLAS, Z_LEGUHLAS, 90)
                .npc(GIMGUH, Kind.GIMGUH, Z_GIMGUH, 90)
                .npc(BOROMIKA, Kind.BOROMIKA, Z_BOROMIKA, 135);
    }

    private static Cutscene raad() {
        Cutscene.Builder s = raadskring("ringh2_raad", 1340)
                .speler(INGANG, 270)
                .acteur(RING, () -> EntityType.ITEM_DISPLAY, IN_STEEN, 0, ring())
                .zwart(0, 14);
        // 1. the ring of pillars from outside, the fall behind it; the player walks in
        s.camera(0, new Vec3(-12.5, 7.5, 9.5), new Vec3(0.5, 1.0, -0.5))
                .camera(110, new Vec3(-8.5, 4.6, 6.5), new Vec3(0.5, 1.2, -1.0))
                .zeg(16, "", "begin", 84)
                .geluid(4, () -> SoundEvents.BELL_BLOCK, 1.2f, 0.8f)
                .loop(SPELER, 10, 70, new Vec3(-2.5, 0, 1.5))
                .kijk(SPELER, 72, KIJK_GUHROND);
        // 2. Guhrond opens the council
        s.cameraKnip(120, new Vec3(-2.6, 1.7, 3.2), KIJK_GUHROND)
                .camera(270, new Vec3(-1.8, 1.6, 2.4), KIJK_GUHROND)
                .animatie(GUHROND, 124, "praat").zeg(124, GUHROND, "welkom", 88)
                .animatie(GUHROND, 216, "wijs").zeg(218, GUHROND, "leg", 56);
        // 3. the ring on the stone
        s.cameraKnip(280, new Vec3(3.6, 2.2, 3.0), KIJK_STEEN)
                .camera(400, new Vec3(2.6, 1.9, 2.4), KIJK_STEEN)
                .animatie(GUHROND, 280, "")
                .loop(SPELER, 284, 314, BIJ_STEEN).kijk(SPELER, 316, KIJK_STEEN)
                .animatie(SPELER, 318, "zwaai")
                .loop(RING, 322, 326, OP_STEEN)
                .geluid(324, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 0.7f)
                .deeltjes(326, ParticleTypes.END_ROD, new Vec3(0.5, 1.45, 0.5), 10, 0.2)
                .zeg(332, "", "ring", 66);
        for (String wie : new String[]{GUHDALF, ARAGUH, LEGUHLAS, GIMGUH, BOROMIKA}) {
            s.kijk(wie, 328, KIJK_STEEN).animatie(wie, 330, "kijk");
        }
        // 4. Boromika wants it "for safety"; Guhdalf thunders
        s.cameraKnipVolgt(404, new Vec3(0.2, 1.6, 2.0), BOROMIKA)
                .animatie(BOROMIKA, 406, "praat").zeg(406, BOROMIKA, "geef", 86)
                .loop(BOROMIKA, 440, 480, new Vec3(1.7, 0, 1.7))
                .animatie(BOROMIKA, 482, "grijp")
                .cameraKnip(494, new Vec3(-1.5, 1.8, -0.5), new Vec3(3.5, 1.4, -3.5))
                .animatie(GUHDALF, 492, "toover")
                .geluid(496, () -> SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f, 1.2f)
                .schud(496, 0.9f, 14)
                .zeg(498, GUHDALF, "boromika", 54)
                .animatie(BOROMIKA, 500, "schrik")
                .animatie(BOROMIKA, 520, "schaam")
                .loop(BOROMIKA, 522, 556, Z_BOROMIKA)
                .animatie(GUHDALF, 552, "");
        // 5. what has to happen with it
        s.cameraKnip(556, new Vec3(-2.6, 1.7, 3.2), KIJK_GUHROND)
                .animatie(GUHROND, 558, "praat").zeg(558, GUHROND, "frituur", 102)
                .animatie(BOROMIKA, 600, "");
        // 6. Gimguh bites it
        s.cameraKnipVolgt(664, new Vec3(1.2, 1.5, -2.2), GIMGUH)
                .animatie(GUHROND, 664, "")
                .zeg(666, GIMGUH, "wachten", 48)
                .loop(GIMGUH, 690, 716, new Vec3(1.8, 0, 0.9)).kijk(GIMGUH, 717, KIJK_STEEN)
                .animatie(GIMGUH, 718, "eet")
                .geluid(732, () -> SoundEvents.ANVIL_PLACE, 0.5f, 1.7f)
                .schud(732, 0.5f, 8)
                .deeltjes(732, ParticleTypes.CRIT, new Vec3(0.9, 1.3, 0.6), 8, 0.2)
                .animatie(GIMGUH, 734, "schrik").zeg(738, GIMGUH, "tand", 60)
                .animatie(LEGUHLAS, 760, "lach")
                .cameraKnipVolgt(802, new Vec3(2.2, 1.5, 0.2), LEGUHLAS)
                .zeg(804, LEGUHLAS, "nul", 76)
                .loop(GIMGUH, 804, 834, Z_GIMGUH).kijk(GIMGUH, 836, Z_LEGUHLAS)
                .animatie(GIMGUH, 884, "ruzie").animatie(LEGUHLAS, 890, "ruzie")
                .zeg(884, GIMGUH, "half", 58);
        // 7. Boromika again: one does not simply walk
        s.cameraKnipVolgt(946, new Vec3(0.8, 1.5, 1.6), BOROMIKA)
                .animatie(GIMGUH, 946, "").animatie(LEGUHLAS, 946, "")
                .animatie(BOROMIKA, 948, "praat").zeg(948, BOROMIKA, "wandel", 100);
        // 8. and then everybody at once, seen from above, the ring in the middle
        s.cameraKnip(1052, new Vec3(0.5, 7.2, 1.3), new Vec3(0.5, 1.0, 0.5))
                .camera(1240, new Vec3(0.5, 3.6, 1.5), new Vec3(0.5, 1.2, 0.5))
                .animatie(GUHROND, 1060, "schud")
                .zeg(1056, ARAGUH, "ik", 44).zeg(1102, LEGUHLAS, "ogen", 44).zeg(1148, GIMGUH, "stukken", 44).zeg(1194, GUHDALF, "dwazen", 46);
        for (String wie : new String[]{GUHDALF, ARAGUH, LEGUHLAS, GIMGUH, BOROMIKA}) {
            s.animatie(wie, 1054, "ruzie");
        }
        for (int i = 0; i < 5; i++) {
            s.geluid(1060 + i * 38, () -> SoundEvents.VILLAGER_NO, 0.7f, 0.8f + i * 0.12f)
                    .deeltjes(1064 + i * 38, ParticleTypes.ANGRY_VILLAGER, new Vec3(0.5 + 3.6 * Math.cos(i * 1.3), 1.7, 0.5 + 3.6 * Math.sin(i * 1.3)), 2, 0.3);
        }
        // 9. the player looks at the ring
        s.cameraKnipVolgt(1244, new Vec3(1.2, 1.5, 2.2), SPELER)
                .cameraVolgt(1330, new Vec3(0.6, 1.5, 1.8), SPELER)
                .zeg(1248, "", "eind", 82)
                .zwart(1326, 1340);
        return s.registreer();
    }

    private static Cutscene genootschap() {
        Cutscene.Builder s = raadskring("ringh2_genootschap", 1132)
                .speler(BIJ_STEEN, 270)
                .acteur(RING, () -> EntityType.ITEM_DISPLAY, OP_STEEN, 0, ring())
                .npc(MERRIE, Kind.MERRIE, ACHTER_MERRIE, 80)
                .npc(PIPPGUH, Kind.PIPPGUH, ACHTER_PIPPGUH, 90)
                .guh(SAM, GuhVariant.SAM_GUH, STRUIK_SAM, 300)
                .zwart(0, 12);
        String[] raad = {GUHDALF, ARAGUH, LEGUHLAS, GIMGUH, BOROMIKA};
        Vec3 hoofd = new Vec3(-0.9, 1.5, 0.5);
        // 1. the player speaks up; the argument stops
        s.camera(0, new Vec3(2.4, 1.7, 2.0), hoofd).camera(140, new Vec3(1.8, 1.6, 1.5), hoofd)
                .animatie(SPELER, 16, "zwaai").zeg(14, SPELER, "ik", 70)
                .zeg(92, "", "stil", 52);
        for (String wie : raad) {
            s.animatie(wie, 1, "ruzie").animatie(wie, 40, "kijk").kijk(wie, 40, hoofd);
        }
        s.kijk(GUHROND, 40, hoofd);
        // 2. Guhdalf
        s.cameraKnip(150, new Vec3(-1.2, 1.7, -0.2), new Vec3(3.0, 1.4, -3.0))
                .animatie(GUHDALF, 156, "praat").zeg(156, GUHDALF, "last", 86)
                .loop(GUHDALF, 152, 190, new Vec3(1.2, 0, -1.6));
        // 3. Araguh kneels
        s.cameraKnipVolgt(246, new Vec3(-0.4, 1.5, -0.6), ARAGUH)
                .animatie(GUHDALF, 246, "")
                .loop(ARAGUH, 248, 278, new Vec3(-2.0, 0, -1.3)).kijk(ARAGUH, 279, hoofd)
                .animatie(ARAGUH, 280, "kniel").zeg(252, ARAGUH, "zwaard", 82);
        // 4. the bow and the axe
        s.cameraKnip(338, new Vec3(1.4, 1.6, 0.5), new Vec3(5.5, 1.2, 0.5))
                .animatie(LEGUHLAS, 340, "buig").zeg(340, LEGUHLAS, "boog", 40)
                .animatie(GIMGUH, 384, "juich").zeg(384, GIMGUH, "bijl", 60);
        // 5. Boromika carries the sandwiches
        s.cameraKnipVolgt(448, new Vec3(1.2, 1.5, 2.2), BOROMIKA)
                .animatie(GIMGUH, 448, "").animatie(LEGUHLAS, 448, "")
                .animatie(BOROMIKA, 450, "buig").zeg(450, BOROMIKA, "broodjes", 80);
        // 6. Sam-guh out of the bushes
        s.geluid(532, () -> SoundEvents.GRASS_BREAK, 1f, 0.8f)
                .cameraKnipVolgt(536, new Vec3(-3.6, 1.6, 0.2), SAM)
                .loop(SAM, 536, 576, new Vec3(-2.2, 0, 1.7)).kijk(SAM, 577, KIJK_GUHROND)
                .zeg(540, SAM, "hee", 74)
                .animatie(SAM, 578, "spring")
                .cameraKnip(620, new Vec3(-2.6, 1.7, 3.2), KIJK_GUHROND)
                .animatie(GUHROND, 622, "praat").zeg(622, GUHROND, "scheiden", 94);
        // 7. Merrie and Pippguh
        s.cameraKnip(720, new Vec3(1.5, 2.3, -1.5), new Vec3(5.0, 0.8, 3.0))
                .animatie(GUHROND, 720, "")
                .loop(MERRIE, 722, 762, new Vec3(2.6, 0, 2.2)).kijk(MERRIE, 764, KIJK_GUHROND)
                .loop(PIPPGUH, 728, 770, new Vec3(1.6, 0, 3.1)).kijk(PIPPGUH, 772, KIJK_GUHROND)
                .zeg(724, MERRIE, "ook", 70)
                .animatie(PIPPGUH, 800, "praat").zeg(798, PIPPGUH, "verstand", 84)
                .animatie(MERRIE, 886, "lach").zeg(886, MERRIE, "afvallen", 44)
                .animatie(PIPPGUH, 886, "");
        // 8. nine companions
        s.cameraKnip(934, new Vec3(-3.4, 2.6, 4.4), new Vec3(0.5, 1.0, -0.5))
                .camera(1124, new Vec3(-5.4, 3.6, 6.0), new Vec3(0.5, 1.0, -0.5))
                .animatie(GUHROND, 936, "wijs").zeg(936, GUHROND, "negen", 100)
                .geluid(1040, () -> SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1f)
                .deeltjes(1040, ParticleTypes.HAPPY_VILLAGER, new Vec3(0.5, 1.6, 0.5), 30, 2.4)
                .animatie(SAM, 1042, "spring")
                .zeg(1052, PIPPGUH, "waarheen", 66)
                .zwart(1118, 1132);
        for (String wie : new String[]{GUHDALF, ARAGUH, LEGUHLAS, GIMGUH, BOROMIKA, MERRIE}) {
            s.animatie(wie, 1040, "juich");
        }
        s.animatie(PIPPGUH, 1052, "praat");
        return s.registreer();
    }

    /** Called from RingH2Feature.register so both scenes are known on both sides before anything asks for them. */
    static void registreer() {
        // (the static fields above do the work: this only makes sure the class is loaded)
    }

    private RingH2Scenes() {
    }
}
