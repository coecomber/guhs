package nl.juiced.guhs.feature.snuffeldorp;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhwaiispellen.GuhwaiiSpellenBlocks;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.vogels.VogelsFeature;

/**
 * The four scenes of Snuffeldorp (the growth scene of the tree is the kern's, {@code Boom.GROEI}). Every position is
 * relative to the scene's anchor block in the unturned island (x east, z south; (0.5, 0, 0.5) = the middle of the anchor
 * block's floor); tools/features/snuffel_dorp_bouw.py {@code controleer} checks that the island has the floor these
 * scripts walk on. Sound effects only (the mod's own and vanilla's), no music; the camera glides and shakes at most once,
 * softly. {@code verbergEcht} hides EVERY real entity near the anchor while a scene plays (the residents whose actors play,
 * other dogs), so the scene at the tree does not use it. Their Dutch lines: tools/features/snuffel_dorp_tekst.py {@code SCENES}.
 * <ul>
 *   <li>{@link #WAKKER} (15 s, anchor = the beach spot): black, waves and gulls, Jutje Kwispel's snout above you, your
 *   first look at your own paws, she runs ahead to the village.</li>
 *   <li>{@link #MAATJE} (15 s, anchor = the air block on the well's south rim): a bucket rattles and falls off the well all
 *   by itself, nobody sees who did it; only you see the companion rise out of the well, and it sees that you see it.</li>
 *   <li>{@link #SPOOR} (10 s, anchor = the tree's foot, turned like the growth scene): you dig between the stones, father's
 *   scarf, and the camera rises to look past the tree at the roadblock and the hills behind it.</li>
 *   <li>{@link #AFVAART} (5 s, anchor = the harbour spot on the jetty): you hop on Kapitein Zoutsnoet's boat, he casts off,
 *   black (then the kern brings you home).</li>
 * </ul>
 */
public final class DorpScenes {
    private static final String LIJN = "snuffeleiland";

    private static void speler(CompoundTag tag) {
        tag.putBoolean("Speler", true);
    }

    public static final Cutscene WAKKER = Cutscene.maak("snuffeldorp_wakker").duur(300).bij(LIJN).verbergEcht(34)
            .acteur("ik", SnuffelFeature.SNUFFEL_HOND, new Vec3(0.5, 0, 0.5), 180, DorpScenes::speler)
            .acteur("jutje", SnuffelFeature.SNUFFEL_HOND, new Vec3(7.5, 0, -10.5), 30, tag -> tag.putString("Bewoner", "redder"))
            // black: the sea, a gull, something sniffing
            .zwart(0, 44)
            .geluid(3, GuhwaiiSpellenBlocks.GOLF_BREEKT, 0.8f, 1f)
            .geluid(14, VogelsFeature.KLIEW, 0.7f, 1f)
            .geluid(34, VogelsFeature.KLIEW, 0.5f, 1.15f)
            .zeg(8, "", "golven", 36)
            .animatie("ik", 1, "snuffel")
            .loop("jutje", 16, 84, new Vec3(1.6, 0, -1.3))
            // a snout above you (the camera lies in the sand next to your own head)
            .camera(0, new Vec3(-0.9, 0.3, 1.4), new Vec3(1.2, 0.8, -1.0))
            .camera(204, new Vec3(-0.6, 0.35, 1.2), new Vec3(1.2, 0.8, -1.0))
            .kijk("jutje", 86, new Vec3(0.5, 0.2, 0.5))
            .animatie("jutje", 88, "snuffel")
            .geluid(90, SnuffelFeature.SNUF_GELUID, 1f, 1f)
            .geluid(104, SnuffelFeature.SNUF_GELUID, 1f, 1.1f)
            .zeg(92, "jutje", "hallo", 56)
            .animatie("jutje", 150, "blaf")
            .geluid(151, SnuffelFeature.BLAF_GELUID, 0.9f, 1f)
            .zeg(152, "jutje", "wakker", 50)
            .animatie("jutje", 166, "kwispel")
            // your own paws
            .cameraKnip(205, new Vec3(0.5, 1.15, 1.35), new Vec3(0.5, 0.05, -0.35))
            .camera(254, new Vec3(0.5, 1.0, 1.25), new Vec3(0.5, 0.05, -0.35))
            .geluid(210, SnuffelFeature.NJEG_GELUID, 0.9f, 1f)
            .zeg(207, "", "pootjes", 46)
            // up, and after her
            .cameraKnip(255, new Vec3(-4.5, 1.7, 3.5), new Vec3(1.5, 0.5, -2.0))
            .camera(300, new Vec3(-4.0, 1.8, 3.0), new Vec3(4.0, 0.5, -6.0))
            .animatie("ik", 255, "")
            .animatie("ik", 262, "kwispel")
            .geluid(262, SnuffelFeature.KWISPEL_GELUID, 0.8f, 1f)
            .zeg(257, "jutje", "kom", 42)
            .animatie("jutje", 268, "")
            .loop("jutje", 270, 300, new Vec3(7.5, 0, -11.5))
            .registreer();

    public static final Cutscene MAATJE = Cutscene.maak("snuffeldorp_maatje").duur(300).bij(LIJN).verbergEcht(14)
            .acteur("emmer", () -> EntityType.ITEM_DISPLAY, new Vec3(0.5, 0.5, 0.5), 0, tag -> {
                CompoundTag emmer = new CompoundTag();
                emmer.putString("id", "minecraft:bucket");
                emmer.putInt("count", 1);
                tag.put("item", emmer);
                tag.putString("billboard", "center");
            })
            .acteur("ik", SnuffelFeature.SNUFFEL_HOND, new Vec3(1.5, -1, 5.5), 180, DorpScenes::speler)
            .acteur("pup", SnuffelFeature.SNUFFEL_HOND, new Vec3(3.5, -1, 2.5), 135, tag -> tag.putString("Bewoner", "pup"))
            .acteur("bakker", SnuffelFeature.SNUFFEL_HOND, new Vec3(-4.5, -1, 1.5), -90, tag -> tag.putString("Bewoner", "bakker"))
            .acteur("maatje", SnuffelFeature.SNUFFEL_MAATJE, new Vec3(0.5, -1.6, -0.5), 0, tag -> {
                tag.putBoolean("Speler", true);
                tag.putBoolean("Ondeugend", true);
            })
            .camera(0, new Vec3(5.5, 1.8, 7.5), new Vec3(0.5, 0.3, 0.5))
            .camera(148, new Vec3(4.0, 1.3, 5.5), new Vec3(0.5, 0.3, 0.8))
            // the bucket rattles, hops and falls off the well
            .animatie("emmer", 20, "spring")
            .geluid(20, () -> SoundEvents.BUCKET_FILL, 0.7f, 1.3f)
            .zeg(16, "", "rammel", 34)
            .animatie("emmer", 52, "spring")
            .geluid(52, () -> SoundEvents.BUCKET_FILL, 0.8f, 1.5f)
            .animatie("pup", 56, "blaf")
            .geluid(57, SnuffelFeature.BLAF_GELUID, 0.7f, 1.4f)
            .zeg(58, "pup", "kef", 34)
            .loop("emmer", 84, 94, new Vec3(0.5, -0.5, 1.7))
            .geluid(94, () -> SoundEvents.BUCKET_EMPTY, 1f, 0.9f)
            .deeltjes(94, ParticleTypes.SPLASH, new Vec3(0.5, -0.8, 1.7), 26, 0.35)
            .schud(94, 0.12f, 6)
            .kijk("bakker", 96, new Vec3(0.5, -0.5, 1.7))
            .animatie("bakker", 100, "blaf")
            .zeg(100, "bakker", "alweer", 48)
            // only you see who did it
            .cameraKnip(150, new Vec3(2.6, 0.0, 4.6), new Vec3(0.5, 1.2, -0.3))
            .camera(300, new Vec3(2.9, 0.2, 5.0), new Vec3(0.9, 0.9, 1.5))
            .loop("maatje", 152, 172, new Vec3(0.5, 1.2, -0.3))
            .geluid(154, SnuffelFeature.MAATJE_GELUID, 1f, 1f)
            .animatie("maatje", 172, "ondeugend")
            .zeg(158, "", "alleen_jij", 50)
            .kijk("maatje", 208, new Vec3(1.5, -0.4, 5.5))
            .animatie("maatje", 210, "idle")
            .zeg(212, "maatje", "zien", 46)
            .animatie("ik", 230, "kwispel")
            .loop("maatje", 258, 284, new Vec3(1.6, -0.1, 4.5))
            .geluid(262, SnuffelFeature.MAATJE_GELUID, 1f, 1.2f)
            .zeg(260, "maatje", "blijf", 40)
            .animatie("maatje", 286, "blij")
            .registreer();

    // (nothing real is hidden here: the tree itself and the real companion next to the dog play along)
    public static final Cutscene SPOOR = Cutscene.maak("snuffeldorp_spoor").duur(200).bij(LIJN)
            .acteur("ik", SnuffelFeature.SNUFFEL_HOND, new Vec3(0.5, 0, 3.4), 180, DorpScenes::speler)
            .camera(0, new Vec3(3.4, 1.3, 6.2), new Vec3(0.5, 0.5, 2.8))
            .camera(118, new Vec3(3.0, 1.5, 5.6), new Vec3(0.5, 0.5, 2.8))
            .animatie("ik", 1, "graaf")
            .geluid(4, SnuffelFeature.GRAAF_GELUID, 1f, 1f)
            .geluid(18, SnuffelFeature.GRAAF_GELUID, 1f, 1.1f)
            .geluid(32, SnuffelFeature.GRAAF_GELUID, 1f, 0.95f)
            .zeg(6, "", "graaf", 46)
            .animatie("ik", 56, "snuffel")
            .geluid(58, SnuffelFeature.GEVONDEN_GELUID, 1f, 1f)
            .zeg(60, "", "sjaal", 58)
            // the camera rises behind you and looks past the tree at the roadblock and the hills
            .camera(200, new Vec3(0.5, 6.5, -7.0), new Vec3(0.5, 2.0, 30.0))
            .animatie("ik", 122, "")
            .kijk("ik", 122, new Vec3(0.5, 1.0, 30.0))
            .animatie("ik", 126, "blaf")
            .geluid(127, SnuffelFeature.BLAF_GELUID, 1f, 1f)
            .zeg(124, "", "spoor", 72)
            .registreer();

    public static final Cutscene AFVAART = Cutscene.maak("snuffeldorp_afvaart").duur(100).bij(LIJN).verbergEcht(8)
            .acteur("ik", SnuffelFeature.SNUFFEL_HOND, new Vec3(0.5, 0, 0.5), 0, DorpScenes::speler)
            .acteur("kapitein", SnuffelFeature.SNUFFEL_HOND, new Vec3(3.5, 0, 0.5), 90, tag -> tag.putString("Bewoner", "kapitein"))
            .camera(0, new Vec3(-6.5, 3.2, -4.5), new Vec3(1.5, 1.0, 2.5))
            .camera(100, new Vec3(-7.5, 3.6, -5.5), new Vec3(1.5, 1.0, 2.5))
            .loop("ik", 6, 40, new Vec3(0.5, 0, 3.5))
            .animatie("kapitein", 8, "blaf")
            .geluid(9, SnuffelFeature.BLAF_GELUID, 0.9f, 0.8f)
            .zeg(8, "kapitein", "los", 56)
            .animatie("ik", 44, "kwispel")
            .geluid(66, SnuffelFeature.REIS_GELUID, 0.8f, 1f)
            .zwart(72, 100)
            .registreer();

    private DorpScenes() {
    }

    static void init() {
        // (loads the class: the scenes must be registered on both sides before a world starts)
    }
}
