package nl.juiced.guhs.feature.ringh6;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.ring.Cast;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ring.Smikagol;
import nl.juiced.guhs.feature.ringh5.RingH5Feature;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-h6): the end of the story in three cutscenes (texts: tools/features/ring_h6_tekst.py SCENES).
 * <ul>
 *   <li>{@link #FRITUUR} "De Frituurspleet" (the balcony over the pool; anchor = the spot "rand" of the mountain): you can't
 *       quite throw the ring in, Smikagol grabs it, dances, slips, splashes into the frying basket, and climbs out golden
 *       brown and beaming with a fried ring that everybody shares. The Eye of Sausron peers into the crater, gets a piece,
 *       blinks contentedly and takes a nap.</li>
 *   <li>{@link #VLUCHT} "De Rookguhs komen" (the same anchor): the frituur boils over; the Rookguhjes you freed come back
 *       with their big family and lift everybody off the mountain. Then the player is put down at home ({@link Thuis}).</li>
 *   <li>{@link #FEEST} "Het feest in de Gouw" (anchor = a flat spot at home): Guhdalf de Witte crowns Araguh with a crown of
 *       knabbels, the fellowship bickers and bows, fireworks. Its end finishes the questline, and ring-kern hands out the
 *       rewards of the whole story.</li>
 * </ul>
 * All positions are template coordinates relative to the anchor block (x + = east, into the crater: the pool lies east of
 * the balcony; the frituur in the middle of the frying basket is at (7, -2, 0)). The balcony is 5 deep (x -2 .. 3) and 8
 * wide (z -4 .. 4); west of it is the rock of the rim, so every camera stands on the balcony or over the pool. The
 * self-check of tools/features/ring_h6.py fails the build when the mountain no longer has its balcony, pool, basket and
 * gantry where these scenes expect them.
 * <p>
 * The flow survives a logout at any point: the step only moves in a scene's {@code daarna}; the flag {@link #GEFRITUURD}
 * remembers that the first scene is over (the ring is gone for good), so a player who comes back sees the flight only.
 * <p>
 * The Eye is ring-h5's entity ({@code RingH5Feature.OOG_VAN_SAUSRON}) as an actor. That slice draws it about 8 blocks wide
 * and 4 high with its feet 2 blocks under its middle, looking half down by itself, and knows the scene animations "slaap",
 * "zoek" and "tevreden" (the pleased squint; any other name is its open stare): the scenes ask for "kijk" (the stare),
 * "tevreden" and "slaap". {@link #OOG} is where its feet are: beside the gantry, in air the mountain's template carves
 * (tools/features/ring_h6.py checks that the whole Eye fits there). Flames at its middle mark the spot, so the story reads
 * the same on a branch where that slice's entity is still the invisible placeholder.
 */
public final class Finale {
    /** Questline flag: the ring is fried (scene 1 is over), the flight home still has to happen. */
    public static final String GEFRITUURD = "gefrituurd";
    /** The frituur in the middle of the frying basket (its surface), and a little above it. */
    private static final Vec3 SAUS = new Vec3(7.0, -2.0, 0.0), BOVEN_MAND = new Vec3(7.0, -0.9, 0.0);
    /** Where the Eye looks in from (its feet): high in the crater, beside the gantry, under the smoke hole. It faces the balcony. */
    private static final Vec3 OOG = new Vec3(12.0, 13.0, -6.0);
    private static final float OOG_YAW = 60f;
    /** The middle of the Eye, and just over its upper lid. */
    private static final Vec3 OOG_MIDDEN = OOG.add(0, 2.0, 0), OOG_BOVEN = OOG.add(0, 4.5, 0);

    public static Cutscene FRITUUR, VLUCHT, FEEST;

    /** (RingH6Feature.register, both sides) registers the three scenes. */
    static void registreer() {
        FRITUUR = frituur();
        VLUCHT = vlucht();
        FEEST = feest();
    }

    private static Cutscene frituur() {
        Cutscene.Builder b = Cutscene.maak("ringh6_frituur").duur(1470).bij("ring_h6").kaart(Klim.KAART).verbergEcht(24)
                .speler(new Vec3(-1.5, 0, 0.5), -90)
                .guh("sam", GuhVariant.SAM_GUH, new Vec3(-1.6, 0, 2.2), -90)
                .acteur("smikagol", RingFeature.SMIKAGOL, new Vec3(-0.5, 0, -3.2), -60)
                .acteur("krokant", RingH6Feature.KROKANTE_SMIKAGOL, new Vec3(7.0, -9.0, 0.0), 90)
                .acteur("oog", RingH5Feature.OOG_VAN_SAUSRON, OOG, OOG_YAW);
        // 1. the crater
        b.zwart(0, 20)
                .camera(0, new Vec3(-1.0, 7.0, -3.0), new Vec3(6.0, -1.5, 0.5)).camera(90, new Vec3(-1.5, 2.4, 3.2), new Vec3(4.0, -0.5, 0.0))
                .zeg(20, "", "aankomst", 75)
                .geluid(10, () -> SoundEvents.LAVA_AMBIENT, 1.0f, 0.7f)
                .deeltjes(40, ParticleTypes.CAMPFIRE_COSY_SMOKE, SAUS.add(0, 2.5, 0), 8, 2.0);
        // 2. to the edge; the ring will not let go
        b.loop(Cutscene.SPELER, 95, 150, new Vec3(1.6, 0, 0.5)).loop("sam", 105, 160, new Vec3(0.4, 0, 2.2))
                .kijk("sam", 160, new Vec3(1.6, 1, 0.5))
                .zeg(100, "sam", "gooi", 75).animatie("sam", 105, "wijs")
                .cameraKnip(181, new Vec3(3.6, 1.3, 3.2), new Vec3(1.6, 1.2, 0.5)).camera(260, new Vec3(3.4, 1.5, 2.6), new Vec3(1.6, 1.3, 0.5))
                .animatie(Cutscene.SPELER, 185, "buk").zeg(185, "", "twijfel", 80)
                .zeg(270, "sam", "niet_eten", 55).animatie("sam", 270, "schrik").animatie(Cutscene.SPELER, 275, "sta");
        // 3. Smikagol
        b.cameraKnipVolgt(326, new Vec3(1.2, 0.8, -4.6), "smikagol")
                .animatie("smikagol", 326, "sluip").loop("smikagol", 330, 372, new Vec3(1.1, 0, -0.5))
                .zeg(330, "smikagol", "mijn", 50)
                .animatie("smikagol", 374, "grijp").geluid(376, () -> ModSounds.MIKA_AMBIENT.get(), 1.2f, 1.9f).schud(377, 0.6f, 10)
                .animatie(Cutscene.SPELER, 378, "spring").animatie("sam", 378, "schrik")
                .zeg(382, "", "gegrepen", 50)
                .cameraKnipVolgt(436, new Vec3(-1.6, 1.6, 1.4), "smikagol")
                .loop("smikagol", 436, 462, new Vec3(2.3, 0, -2.0)).animatie("smikagol", 440, "juich")
                .zeg(440, "smikagol", "dans", 70)
                .loop("smikagol", 470, 490, new Vec3(2.3, 0, 0.9)).loop("smikagol", 490, 508, new Vec3(2.45, 0, -0.8))
                // ...and off the edge he goes, into the basket
                .animatie("smikagol", 512, "val").zeg(512, "smikagol", "oeps", 28)
                .loop("smikagol", 512, 528, SAUS.add(-0.8, 0, -0.4)).loop("smikagol", 528, 540, new Vec3(6.2, -9.0, -0.4))
                .cameraKnip(529, new Vec3(-0.8, 3.4, 3.8), new Vec3(6.5, -1.8, 0.0))
                .deeltjes(529, ParticleTypes.LAVA, BOVEN_MAND.add(-0.8, 0, -0.4), 40, 0.7)
                .deeltjes(531, ParticleTypes.CLOUD, BOVEN_MAND.add(-0.8, 0.4, -0.4), 30, 0.8)
                .geluid(529, () -> SoundEvents.GENERIC_SPLASH, 1.4f, 0.6f).geluid(531, () -> SoundEvents.FIRE_EXTINGUISH, 1.0f, 0.7f)
                .schud(529, 0.9f, 16);
        // 4. silence, bubbles
        b.zeg(548, "", "stil", 70).animatie("sam", 548, "kijk").animatie(Cutscene.SPELER, 548, "buk")
                .deeltjes(560, ParticleTypes.LAVA, BOVEN_MAND, 6, 1.2).deeltjes(585, ParticleTypes.LAVA, BOVEN_MAND, 8, 1.2)
                .deeltjes(610, ParticleTypes.CAMPFIRE_COSY_SMOKE, BOVEN_MAND.add(0, 0.6, 0), 10, 1.4).geluid(585, () -> SoundEvents.LAVA_POP, 1.2f, 0.8f)
                .zeg(625, "sam", "ach", 70).animatie(Cutscene.SPELER, 625, "sta");
        // 5. PLOP
        b.cameraKnip(701, new Vec3(1.8, 1.4, 3.6), new Vec3(6.4, -0.6, 0.0))
                .loop("krokant", 702, 716, SAUS.add(-0.6, 0.3, 0.0)).animatie("krokant", 702, "spring")
                .deeltjes(704, ParticleTypes.WAX_ON, BOVEN_MAND, 40, 0.9).deeltjes(706, ParticleTypes.LAVA, BOVEN_MAND, 14, 0.6)
                .geluid(704, () -> SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.2f, 1.4f).geluid(708, () -> ModSounds.GUH_HAPPY.get(), 1.0f, 1.7f)
                .zeg(706, "", "krokant", 70).animatie("krokant", 720, "juich")
                .loop("krokant", 780, 812, new Vec3(2.3, 0, -0.6)).animatie("krokant", 780, "spring")
                .cameraKnipVolgt(781, new Vec3(-1.6, 1.5, -2.8), "krokant")
                .deeltjes(815, ParticleTypes.WAX_ON, new Vec3(2.3, 0.6, -0.6), 16, 0.5)
                .zeg(816, "krokant", "lekker", 85).animatie("krokant", 816, "juich").animatie("sam", 816, "lach");
        // 6. sharing
        b.animatie("krokant", 905, "draag").zeg(905, "krokant", "delen", 75).kijk("krokant", 905, new Vec3(1.6, 1, 0.5))
                .zeg(985, "sam", "delen_sam", 65).animatie("sam", 985, "juich")
                .cameraKnip(1052, new Vec3(4.6, 1.6, 0.4), new Vec3(1.4, 0.8, 0.6))
                .animatie(Cutscene.SPELER, 1055, "zwaai").animatie("sam", 1055, "eet").animatie("krokant", 1055, "eet")
                .deeltjes(1060, ParticleTypes.HEART, new Vec3(1.5, 1.6, 0.6), 8, 0.9).geluid(1058, () -> ModSounds.GUH_EAT.get(), 1.0f, 1.0f)
                .geluid(1072, () -> ModSounds.GUH_EAT.get(), 1.0f, 1.3f)
                .zeg(1056, "", "iedereen", 80);
        // 7. the Eye gets a piece, blinks and naps
        b.cameraKnip(1140, new Vec3(-1.6, 1.4, 0.5), OOG_MIDDEN.add(0, -1.5, 0)).camera(1440, new Vec3(-1.2, 1.2, 0.5), OOG_MIDDEN)
                .animatie("oog", 1140, "kijk").deeltjes(1141, ParticleTypes.FLAME, OOG_MIDDEN, 50, 2.2).deeltjes(1170, ParticleTypes.FLAME, OOG_MIDDEN, 30, 2.2)
                .zeg(1142, "", "oog", 85)
                .zeg(1230, "krokant", "voor_oog", 55).animatie("krokant", 1230, "wijs")
                .deeltjes(1236, ParticleTypes.WAX_ON, new Vec3(5.5, 6.0, -2.4), 10, 0.4).deeltjes(1244, ParticleTypes.WAX_ON, new Vec3(9.5, 11.5, -4.6), 10, 0.4)
                .animatie("oog", 1290, "tevreden").zeg(1292, "", "oog_eet", 70).deeltjes(1300, ParticleTypes.HEART, OOG_BOVEN, 6, 1.6)
                .animatie("oog", 1368, "slaap").zeg(1370, "", "dutje", 75).deeltjes(1390, ParticleTypes.CAMPFIRE_COSY_SMOKE, OOG_BOVEN, 4, 0.8)
                .zwart(1445, 1470);
        return b.registreer();
    }

    private static Cutscene vlucht() {
        Cutscene.Builder b = Cutscene.maak("ringh6_vlucht").duur(370).bij("ring_h6").kaart(Klim.KAART).verbergEcht(24)
                .speler(new Vec3(1.5, 0, 0.5), -90)
                .guh("sam", GuhVariant.SAM_GUH, new Vec3(0.4, 0, 2.2), -90)
                .acteur("krokant", RingH6Feature.KROKANTE_SMIKAGOL, new Vec3(2.3, 0, -0.6), 0)
                .acteur("oog", RingH5Feature.OOG_VAN_SAUSRON, OOG, OOG_YAW)
                .acteur("kleintje", RingH6Feature.ROOKGUH, new Vec3(8.0, 22.0, -1.0), 90)
                .acteur("kleintje2", RingH6Feature.ROOKGUH, new Vec3(6.5, 23.0, 1.5), 90)
                .acteur("kleintje3", RingH6Feature.ROOKGUH, new Vec3(9.0, 24.0, 0.5), 90)
                .acteur("groot", SpiesburchtFeature.ROOKGUH, new Vec3(7.0, 26.0, 0.0), 90)
                .acteur("groot2", SpiesburchtFeature.ROOKGUH, new Vec3(8.0, 29.0, 3.0), 90);
        // the mountain boils over
        b.zwart(0, 15).animatie("oog", 0, "slaap")
                .camera(0, new Vec3(-1.6, 2.2, -3.2), new Vec3(4.5, -0.5, 0.5))
                .schud(22, 1.2f, 70).geluid(22, () -> SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 0.5f).geluid(50, () -> SoundEvents.LAVA_POP, 1.4f, 0.6f)
                .deeltjes(25, ParticleTypes.LAVA, BOVEN_MAND, 50, 2.2).deeltjes(55, ParticleTypes.LAVA, BOVEN_MAND, 60, 2.4)
                .deeltjes(85, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, BOVEN_MAND.add(0, 1, 0), 20, 2.0)
                .animatie("sam", 26, "schrik").animatie("krokant", 26, "schrik").animatie(Cutscene.SPELER, 28, "spring")
                .zeg(28, "sam", "borrelt", 65);
        // the little ones come first, the big ones right behind
        b.loop("kleintje", 90, 140, new Vec3(0.2, 2.6, -2.2)).loop("kleintje2", 94, 146, new Vec3(-1.2, 2.8, 2.6))
                .loop("kleintje3", 100, 150, new Vec3(3.0, 3.0, 2.0))
                .zeg(102, "kleintje", "komen", 60)
                .cameraKnip(166, new Vec3(-1.5, 0.9, -2.9), new Vec3(3.0, 5.0, 0.5))
                .loop("groot", 150, 222, new Vec3(1.5, 3.4, 0.5)).loop("groot2", 160, 230, new Vec3(0.6, 3.6, 3.6))
                .zeg(170, "", "rookguhs", 70).animatie("sam", 180, "juich");
        // up and away
        b.zeg(246, "krokant", "ook_mee", 55).animatie("krokant", 246, "grijp")
                .loop(Cutscene.SPELER, 260, 350, new Vec3(7.0, 19.0, 0.5)).loop("groot", 260, 350, new Vec3(7.0, 22.4, 0.5))
                .loop("sam", 268, 355, new Vec3(6.0, 19.0, 3.0)).loop("groot2", 268, 355, new Vec3(6.0, 22.6, 3.0))
                .loop("krokant", 274, 358, new Vec3(8.0, 18.0, -1.6)).loop("kleintje", 274, 358, new Vec3(8.0, 19.6, -1.6))
                .loop("kleintje2", 270, 358, new Vec3(5.0, 21.0, 1.0)).loop("kleintje3", 276, 358, new Vec3(9.0, 22.0, 1.5))
                .cameraKnipVolgt(261, new Vec3(-1.6, 1.1, 2.9), Cutscene.SPELER)
                .deeltjes(262, ParticleTypes.CLOUD, new Vec3(1.5, 0.6, 0.5), 20, 0.8)
                .zeg(305, "", "naar_huis", 55)
                .zwart(345, 370);
        return b.registreer();
    }

    private static Cutscene feest() {
        Cutscene.Builder b = Cutscene.maak("ringh6_feest").duur(960).bij("ring_h6").kaart(Klim.KAART).verbergEcht(14).cameraOntwijkt()
                .speler(new Vec3(0.5, 0, 3.5), 180)
                .guh("sam", GuhVariant.SAM_GUH, new Vec3(2.0, 0, 3.9), 180)
                .acteur("guhdalf", ModEntities.GUH_NPC, new Vec3(0.5, 0, -3.0), 0, Cast.acteur(GuhNpcEntity.Kind.GUHDALF, "wit"))
                .npc("araguh", GuhNpcEntity.Kind.ARAGUH, new Vec3(0.5, 0, -1.0), 180)
                .npc("leguhlas", GuhNpcEntity.Kind.LEGUHLAS, new Vec3(-3.4, 0, 0.2), -70)
                .npc("gimguh", GuhNpcEntity.Kind.GIMGUH, new Vec3(-2.8, 0, 1.9), -60)
                .npc("merrie", GuhNpcEntity.Kind.MERRIE, new Vec3(4.2, 0, 0.2), 70)
                .npc("pippguh", GuhNpcEntity.Kind.PIPPGUH, new Vec3(3.6, 0, 1.9), 60)
                .npc("boromika", GuhNpcEntity.Kind.BOROMIKA, new Vec3(4.6, 0, -2.2), 50)
                .acteur("krokant", RingFeature.SMIKAGOL, new Vec3(-3.8, 0, -2.2), -50);
        b.zwart(0, 20)
                .camera(0, new Vec3(-6.5, 3.4, 7.0), new Vec3(0.5, 0.8, -0.5)).camera(110, new Vec3(-3.0, 2.2, 6.4), new Vec3(0.5, 0.8, -1.0))
                .zeg(20, "", "thuis", 80)
                .deeltjes(30, ParticleTypes.FIREWORK, new Vec3(0.5, 6, -6), 30, 2.0).geluid(30, () -> SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f)
                .geluid(52, () -> SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f)
                .cameraKnip(111, new Vec3(1.8, 1.3, 0.6), new Vec3(0.5, 1.0, -3.0))
                .zeg(112, "guhdalf", "welkom", 85).animatie("guhdalf", 112, "praat").animatie(Cutscene.SPELER, 150, "zwaai")
                .cameraKnip(205, new Vec3(1.2, 1.0, 0.6), new Vec3(3.9, 0.8, 1.2))
                .zeg(206, "pippguh", "tweede", 55).animatie("pippguh", 206, "juich")
                .zeg(266, "merrie", "stil", 55).animatie("merrie", 266, "schud");
        // the crown
        b.cameraKnip(330, new Vec3(-2.2, 1.2, 1.6), new Vec3(0.5, 0.9, -1.6)).camera(470, new Vec3(-1.4, 1.3, 1.2), new Vec3(0.5, 1.0, -1.4))
                .kijk("araguh", 330, new Vec3(0.5, 1, -3.0)).animatie("araguh", 332, "kniel")
                .zeg(334, "guhdalf", "kroon", 95).animatie("guhdalf", 334, "toover")
                .animatie("araguh", 420, "kroon")
                .deeltjes(420, ParticleTypes.WAX_ON, new Vec3(0.5, 1.6, -1.0), 30, 0.5).deeltjes(424, ParticleTypes.FIREWORK, new Vec3(0.5, 5, -3), 40, 1.6)
                .geluid(420, () -> SoundEvents.PLAYER_LEVELUP, 1.0f, 1.2f).geluid(426, () -> SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f, 1.0f)
                .kijk("araguh", 440, new Vec3(0.5, 1, 3.5))
                .animatie("leguhlas", 432, "juich").animatie("gimguh", 432, "juich").animatie("merrie", 432, "juich").animatie("pippguh", 432, "juich")
                .animatie("boromika", 432, "juich").animatie("krokant", 432, "juich").animatie("sam", 432, "juich")
                .cameraKnip(471, new Vec3(0.5, 1.2, 1.6), new Vec3(0.5, 1.0, -1.0))
                .zeg(472, "araguh", "koning", 80)
                .animatie("leguhlas", 500, "buig").animatie("gimguh", 500, "buig").animatie("merrie", 500, "buig").animatie("pippguh", 500, "buig")
                .animatie("boromika", 500, "buig").animatie("guhdalf", 500, "buig");
        // the friends
        b.cameraKnip(560, new Vec3(-0.6, 1.1, 2.6), new Vec3(-3.1, 0.8, 1.0))
                .zeg(562, "gimguh", "tel", 60).animatie("gimguh", 562, "juich").animatie("leguhlas", 562, "")
                .zeg(628, "leguhlas", "tel_meer", 55).animatie("leguhlas", 628, "lach").animatie("gimguh", 640, "ruzie")
                .cameraKnip(690, new Vec3(1.6, 1.1, 0.8), new Vec3(4.6, 0.8, -2.2))
                .zeg(692, "boromika", "sorry", 70).animatie("boromika", 692, "schaam")
                .cameraKnip(768, new Vec3(-1.2, 1.0, 0.4), new Vec3(-3.8, 0.7, -2.2))
                .zeg(770, "krokant", "vis", 60).animatie("krokant", 770, "draag")
                .cameraKnip(836, new Vec3(-1.6, 1.3, 5.6), new Vec3(1.2, 0.9, 3.7))
                .zeg(838, "sam", "naar_huis", 70).animatie("sam", 838, "kijk").kijk("sam", 838, new Vec3(0.5, 1, 3.5))
                .kijk(Cutscene.SPELER, 850, new Vec3(2.0, 0.8, 3.9))
                .cameraKnip(905, new Vec3(-7.0, 4.0, 8.0), new Vec3(0.5, 1.0, 0.0)).camera(955, new Vec3(-9.0, 6.0, 10.0), new Vec3(0.5, 1.5, 0.0))
                .zeg(908, "", "einde", 50)
                .deeltjes(906, ParticleTypes.FIREWORK, new Vec3(0.5, 7, -2), 60, 3.0).deeltjes(925, ParticleTypes.FIREWORK, new Vec3(-3, 8, 2), 50, 3.0)
                .geluid(906, () -> SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f).geluid(926, () -> SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0f, 0.9f)
                .zwart(940, 960);
        return b.registreer();
    }

    // =====================================================================================================================
    // the flow
    // =====================================================================================================================

    /** Step 5, on the balcony: the first scene. In its daarna the ring is gone for good and the flight starts. */
    static void frituur(ServerPlayer p, Berg.Kopie berg) {
        Verhaallijn lijn = RingH6Feature.LIJN;
        if (!Duwtje.mag(p) || !Ring.aanZet(p, lijn, 5) || lijn.vlag(p, GEFRITUURD)) {
            return;
        }
        if (!Ring.heeft(p)) {
            Ring.geef(p);                                     // (lost on the way up: Sam-guh kept it; a scene without the ring is no scene)
        }
        if (p.isPassenger()) {
            p.stopRiding();
        }
        BlockPos anker = berg.wereld("rand");
        Rotation draai = berg.draai();
        Cutscenes.speel(p, FRITUUR, anker, draai, q -> {
            naFrituur(q);
            vlucht(q, Berg.zoek(q));
        });
    }

    /** The ring is fried and shared: it is gone, Smikagol the guide is gone (he comes back as a buddy), a piece to keep. */
    static void naFrituur(ServerPlayer p) {
        Verhaallijn lijn = RingH6Feature.LIJN;
        lijn.vlag(p, GEFRITUURD, true);
        Ring.neem(p);
        Ring.zetZwaarte(p, 0, 1);
        Smikagol.stuurWeg(p);
        if (lijn.eenmalig(p, "stukje")) {
            Minigames.give(p, new ItemStack(RingH6Feature.STUKJE.get()));
            p.sendSystemMessage(Component.translatable("quest.guhs.ringh6.stukje").withStyle(ChatFormatting.GOLD));
        }
        Ring.behaald(p, "ring_h6_gefrituurd");
    }

    /**
     * Step 5 with the ring fried: the Rookguhs come (the second scene, on the balcony), then home. A player who is not on
     * the mountain any more (they logged out and walked off) is brought home without the scene.
     */
    static void vlucht(ServerPlayer p, @Nullable Berg.Kopie berg) {
        Verhaallijn lijn = RingH6Feature.LIJN;
        if (!Ring.aanZet(p, lijn, 5) || !lijn.vlag(p, GEFRITUURD)) {
            return;
        }
        if (berg == null) {
            if (Duwtje.mag(p)) {
                Thuis.breng(p);
            }
            return;
        }
        Cutscenes.speel(p, VLUCHT, berg.wereld("rand"), berg.draai(), Thuis::breng);
    }

    private Finale() {
    }
}
