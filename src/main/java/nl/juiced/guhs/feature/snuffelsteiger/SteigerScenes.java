package nl.juiced.guhs.feature.snuffelsteiger;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * The cutscenes of the dock (DESIGN_VERHALENPAD C "Getting there"; texts: tools/features/snuffel_steiger.py). All four are
 * anchored on the first plank of the pier ({@link Steiger#VOET}) and written in the template's own coordinates
 * ({@link Steiger#punt}), so they fit every copy however it is turned. Sound effects only, no music; calm camera shake.
 * <ul>
 *   <li>{@link #FEEST} (20 s): the lantern feast on the pier, the little brother or sister sneaks out to look and
 *   collapses;</li>
 *   <li>{@link #OVERTOCHT} (25 s): out with the captain, the sky turns, he wants to go back, you jump overboard and swim
 *   on; it ends in black (the island's waking-up scene starts in black);</li>
 *   <li>{@link #VAART} (6 s): every later crossing with the captain;</li>
 *   <li>{@link #THUISKOMST} (7 s): the boat brings you home to the pier.</li>
 * </ul>
 * In the Guhmensie the player is a player (the stand-in with their own skin); the family and the neighbours are dogs. The
 * puppy is the viewer's own puppy (their chosen breed and coat; a red shiba before they chose).
 */
public final class SteigerScenes {
    public static final int FEEST_DUUR = 400, OVERTOCHT_DUUR = 500, VAART_DUUR = 120, THUISKOMST_DUUR = 140;
    /** How far out (template z) the storm plays: open sea, the structure type checked that it is. */
    static final double ZEE_Z = 60;

    public static Cutscene FEEST, OVERTOCHT, VAART, THUISKOMST;

    private SteigerScenes() {
    }

    private static Vec3 p(double x, double y, double z) {
        return Steiger.punt(x, y, z);
    }

    /** Where the boat's keel is when the boat lies at this template x, z. */
    private static Vec3 boot(double x, double z, double dy) {
        return p(x, Steiger.BOOT.y + dy, z);
    }

    /** The boat and who is aboard move as one (the bow looks to +z, out to sea). */
    private static Cutscene.Builder vaar(Cutscene.Builder b, int t0, int t1, double x, double z, double dy, boolean spelerAanBoord) {
        return vaar(b, t0, t1, x, z, dy, spelerAanBoord, 1);
    }

    /** ...boeg: +1 = the bow looks to +z (out to sea), -1 = to -z (coming in): the captain is always at the stern. */
    private static Cutscene.Builder vaar(Cutscene.Builder b, int t0, int t1, double x, double z, double dy, boolean spelerAanBoord, int boeg) {
        b.loop("boot", t0, t1, boot(x, z, dy));
        b.loop("kapitein", t0, t1, boot(x, z + boeg * Steiger.BOOT_ROER, dy + Steiger.BOOT_DEK));
        if (spelerAanBoord) {
            b.loop(Cutscene.SPELER, t0, t1, boot(x, z + boeg * Steiger.BOOT_BOEG, dy + Steiger.BOOT_DEK));
        }
        return b;
    }

    private static Cutscene.Builder schip(Cutscene.Builder b, double x, double z, boolean spelerAanBoord) {
        return schip(b, x, z, spelerAanBoord, 1);
    }

    private static Cutscene.Builder schip(Cutscene.Builder b, double x, double z, boolean spelerAanBoord, int boeg) {
        float yaw = boeg > 0 ? Steiger.BOOT_YAW : Steiger.BOOT_YAW + 180f;
        b.acteur("boot", SnuffelsteigerFeature.STEIGER_BOOT, boot(x, z, 0), yaw);
        b.acteur("kapitein", SnuffelFeature.SNUFFEL_HOND, boot(x, z + boeg * Steiger.BOOT_ROER, Steiger.BOOT_DEK), yaw, tag -> tag.putString("Bewoner", "kapitein"));
        if (spelerAanBoord) {
            b.speler(boot(x, z + boeg * Steiger.BOOT_BOEG, Steiger.BOOT_DEK), yaw);
        }
        return b;
    }

    static void registreer() {
        FEEST = feest();
        OVERTOCHT = overtocht();
        VAART = vaart();
        THUISKOMST = thuiskomst();
    }

    // =====================================================================================================================
    // 1. the lantern feast and the collapse
    // =====================================================================================================================
    private static Cutscene feest() {
        Vec3 val = p(10.5, 0, 18.5);                 // where the puppy goes down: on the pier, between its two lantern arches
        Cutscene.Builder b = Cutscene.maak("snuffelsteiger_feest").duur(FEEST_DUUR).bij("snuffeleiland").verbergEcht(48)
                .speler(p(16.5, 0, 8.5), 180)
                .acteur("boot", SnuffelsteigerFeature.STEIGER_BOOT, Steiger.punt(Steiger.BOOT), Steiger.BOOT_YAW)
                .acteur("buur", SnuffelFeature.SNUFFEL_HOND, p(9.6, 0, 21.5), 200, tag -> {
                    tag.putString("Ras", "golden");
                    tag.putString("Kleur", "rood");
                })
                .acteur("kapitein", SnuffelFeature.SNUFFEL_HOND, p(11.4, 0, 23.5), 160, tag -> tag.putString("Bewoner", "kapitein"))
                .acteur("gast1", SnuffelFeature.SNUFFEL_HOND, p(10.5, 0, 25.5), 180, tag -> {
                    tag.putString("Ras", "corgi");
                    tag.putString("Kleur", "sable");
                })
                .acteur("gast2", SnuffelFeature.SNUFFEL_HOND, p(8.5, 0, 28.5), 150, tag -> {
                    tag.putString("Ras", "teckel");
                    tag.putString("Kleur", "choco");
                })
                .acteur("gast3", SnuffelFeature.SNUFFEL_HOND, p(11.5, 0, 28.5), 210, tag -> {
                    tag.putString("Ras", "jackrussell");
                    tag.putString("Kleur", "zwart");
                })
                .acteur("pup", SnuffelsteigerFeature.STEIGER_BEWONER, p(7.5, 0, 10.5), 0, tag -> {
                    tag.putBoolean("Speler", true);
                    tag.putBoolean("Pup", true);
                });
        // --- the feast: the whole pier with its lampions, from over the water (0 - 90) ---
        b.camera(0, p(23, 6, 31), p(10.5, 1, 21)).camera(90, p(19, 4, 27), p(10.5, 1, 21));
        b.loop(Cutscene.SPELER, 5, 70, p(11.5, 0, 12.5)).kijk(Cutscene.SPELER, 70, p(10.5, 0, 22));
        b.animatie("gast1", 1, "kwispel").animatie("gast2", 1, "zit").animatie("buur", 1, "kwispel");
        b.loop("gast3", 20, 60, p(9.5, 0, 26.5)).animatie("gast3", 62, "kwispel");
        b.geluid(4, SnuffelsteigerFeature.BEL_GELUID, 0.6f, 1.2f);
        b.geluid(34, SnuffelFeature.BLAF_GELUID, 0.7f, 1.1f).geluid(58, SnuffelFeature.BLAF_GELUID, 0.6f, 1.3f).geluid(76, SnuffelFeature.NJEG_GELUID, 0.7f, 1f);
        b.zeg(8, "", "avond", 70);
        b.animatie("kapitein", 44, "blaf").animatie("kapitein", 56, "").zeg(44, "kapitein", "ahoi", 46);
        // --- the door of the cottage: somebody who should be in bed (90 - 236) ---
        b.cameraKnip(91, p(11.5, 1.4, 13.5), p(7.5, 0.5, 10.5)).cameraVolgt(140, p(12.5, 1.3, 14.5), "pup").cameraVolgt(232, p(13.2, 1.1, 17.2), "pup");
        b.zeg(96, "", "bed", 64);
        b.loop("pup", 112, 150, p(9.5, 0, 12.5)).loop("pup", 150, 204, val);
        b.geluid(150, SnuffelFeature.NJEG_GELUID, 0.8f, 1.5f).zeg(164, "pup", "kef", 44);
        b.animatie("pup", 206, "kwispel").geluid(208, SnuffelFeature.KWISPEL_GELUID, 0.8f, 1.2f);
        b.kijk("buur", 206, val).animatie("buur", 206, "").zeg(212, "buur", "mandje", 38);
        // --- it goes wrong (236 - 300) ---
        b.cameraKnip(236, p(12.2, 0.7, 19.9), val.add(0, 0.3, 0)).camera(300, p(11.9, 0.6, 19.6), val.add(0, 0.25, 0));
        b.animatie("pup", 240, "").zeg(244, "pup", "duizelig", 40).geluid(244, SnuffelsteigerFeature.PIEP_GELUID, 0.7f, 1.5f);
        b.animatie("pup", 262, "zit").animatie("pup", 280, SteigerBewoner.LIG).geluid(281, SnuffelsteigerFeature.PLOF_GELUID, 0.9f, 0.8f);
        // --- everybody comes running (300 - 400) ---
        b.cameraKnip(300, p(15, 2.6, 22.5), val.add(0, 0.4, 0)).camera(392, p(16.5, 4.6, 24.5), val.add(0, 0.4, 0));
        b.loop("buur", 300, 318, p(10.0, 0, 19.6)).kijk("buur", 318, val);
        b.loop(Cutscene.SPELER, 300, 334, p(11.3, 0, 17.6)).kijk(Cutscene.SPELER, 334, val).animatie(Cutscene.SPELER, 338, "buk");
        b.loop("kapitein", 304, 328, p(11.4, 0, 19.9)).kijk("kapitein", 328, val);
        b.animatie("gast1", 300, "").kijk("gast1", 300, val).animatie("gast2", 300, "").kijk("gast2", 300, val).animatie("gast3", 300, "").kijk("gast3", 300, val);
        b.geluid(302, SnuffelFeature.BLAF_GELUID, 0.7f, 0.9f);
        b.zeg(306, "buur", "gloeit", 44).zeg(352, "kapitein", "binnen", 40);
        b.zwart(384, FEEST_DUUR);
        return b.registreer();
    }

    // =====================================================================================================================
    // 3. the crossing: the storm and the jump
    // =====================================================================================================================
    private static Cutscene overtocht() {
        double x = Steiger.BOOT.x, z0 = Steiger.BOOT.z;
        Cutscene.Builder b = Cutscene.maak("snuffelsteiger_overtocht").duur(OVERTOCHT_DUUR).bij("snuffeleiland").verbergEcht(48);
        schip(b, x, z0, false);
        b.speler(p(12.5, 0, 29.5), -90);
        // --- aboard (0 - 70): seen low from the water, the pier and the cottage behind ---
        b.camera(0, p(19.5, -0.9, 33.5), p(13.6, -1.2, 28.5)).camera(70, p(18.5, -1.1, 32.5), p(13.9, -1.2, 28.8));
        b.animatie(Cutscene.SPELER, 14, "spring").loop(Cutscene.SPELER, 14, 26, boot(x, z0 + Steiger.BOOT_BOEG, Steiger.BOOT_DEK));
        b.kijk(Cutscene.SPELER, 27, p(x, 0, z0 + 20));
        b.geluid(26, SnuffelsteigerFeature.PLANK_GELUID, 1f, 0.8f);
        b.animatie("kapitein", 30, "blaf").animatie("kapitein", 42, "").zeg(30, "kapitein", "los", 62);
        b.geluid(66, SnuffelsteigerFeature.RIEM_GELUID, 0.9f, 0.8f);
        // --- out (70 - 150): slowly away from the pier, the camera stays behind on it ---
        b.cameraKnip(71, p(10.5, 1.7, 26.5), p(x, -1.4, z0 + 4)).camera(150, p(10.9, 1.9, 28.5), p(x, -1.4, z0 + 9));
        vaar(b, 70, 150, x, z0 + 6, 0, true);
        b.zeg(84, "", "uit", 60);
        b.geluid(110, SnuffelsteigerFeature.RIEM_GELUID, 0.7f, 0.9f);
        // --- a blink of black: out on the open sea (150 - 166) ---
        b.zwart(148, 164);
        vaar(b, 158, 159, x, ZEE_Z, 0, true);
        // --- the sky turns (166 - 290): the boat goes up and down more and more, rain, thunder ---
        b.cameraKnip(159, p(x + 5.2, -0.7, ZEE_Z + 4.6), p(x, -1.3, ZEE_Z)).camera(290, p(x - 4.6, -0.5, ZEE_Z + 5.4), p(x, -1.3, ZEE_Z));
        int t = 166;
        boolean op = true;
        while (t < 404) {
            // (gentle swell first, then waves: higher and quicker)
            double hoog = t < 215 ? 0.12 : t < 290 ? 0.26 : 0.36;
            int half = t < 215 ? 20 : t < 290 ? 14 : 11;
            vaar(b, t, Math.min(404, t + half), x, ZEE_Z, op ? hoog : -hoog, t < 404);
            op = !op;
            t += half;
        }
        b.zeg(174, "kapitein", "lucht", 66);
        b.geluid(214, SnuffelsteigerFeature.DONDER_GELUID, 0.5f, 0.8f).animatie("boot", 215, SteigerBoot.STORM);
        for (int r = 215; r < 480; r += 3) {
            double rz = r < 404 ? ZEE_Z : ZEE_Z + 3;
            b.deeltjes(r, ParticleTypes.FALLING_WATER, p(x + 0.5, 3.4, rz + 0.5), 14, 3.2);
            if (r % 6 == 0) {
                b.deeltjes(r, ParticleTypes.RAIN, p(x, Steiger.WATER + 0.1, rz), 12, 3.0);
            }
            if (r % 21 == 0) {
                b.deeltjes(r, ParticleTypes.SPLASH, p(x, Steiger.WATER + 0.2, rz + 2.6), 16, 0.7);
            }
        }
        b.schud(250, 0.4f, 50).geluid(252, SnuffelsteigerFeature.GOLF_GELUID, 0.8f, 0.9f);
        b.animatie("kapitein", 262, "blaf").animatie("kapitein", 276, "").zeg(262, "kapitein", "golven", 56);
        // --- "I am turning back" (290 - 404): close on the two of them ---
        b.cameraKnip(291, p(x + 2.4, -0.8, ZEE_Z + 3.3), p(x, -1.1, ZEE_Z - 0.2)).camera(404, p(x + 2.9, -0.7, ZEE_Z + 2.4), p(x, -1.1, ZEE_Z));
        b.kijk("kapitein", 292, p(x, 0, ZEE_Z + 6));
        b.zeg(298, "kapitein", "terug", 60);
        b.geluid(330, SnuffelsteigerFeature.DONDER_GELUID, 0.6f, 0.9f).schud(331, 0.5f, 40).geluid(338, SnuffelsteigerFeature.GOLF_GELUID, 0.9f, 0.8f);
        b.zeg(362, Cutscene.SPELER, "nee", 44);
        // --- the jump (404 - 500) ---
        b.cameraKnip(405, p(x + 6.5, 0.3, ZEE_Z + 6.5), p(x + 1, -1.6, ZEE_Z + 1));
        vaar(b, 404, 500, x, ZEE_Z, 0.1, false);
        Vec3 plons = p(x + 2.4, Steiger.WATER - 1.25, ZEE_Z + 2.2);
        b.animatie(Cutscene.SPELER, 412, "spring").loop(Cutscene.SPELER, 412, 426, plons);
        b.geluid(425, SnuffelFeature.REIS_GELUID, 1f, 0.9f).deeltjes(426, ParticleTypes.SPLASH, plons.add(0, 1.3, 0), 40, 0.5);
        b.deeltjes(427, ParticleTypes.BUBBLE_POP, plons.add(0, 1.3, 0), 12, 0.4);
        b.kijk("kapitein", 428, plons).animatie("kapitein", 432, "blaf").animatie("kapitein", 446, "").zeg(432, "kapitein", "landrot", 52);
        b.loop(Cutscene.SPELER, 430, 500, p(x + 9, Steiger.WATER - 1.25, ZEE_Z + 13));
        b.cameraVolgt(444, p(x + 7.5, 0.6, ZEE_Z + 5.5), Cutscene.SPELER).cameraVolgt(500, p(x + 9.5, 1.4, ZEE_Z + 7), Cutscene.SPELER);
        b.schud(462, 0.5f, 30).geluid(464, SnuffelsteigerFeature.GOLF_GELUID, 1f, 0.7f).geluid(466, SnuffelsteigerFeature.DONDER_GELUID, 0.5f, 0.7f);
        b.zeg(452, "", "zwart", 44);
        b.zwart(482, OVERTOCHT_DUUR);
        return b.registreer();
    }

    // =====================================================================================================================
    // every later crossing: calm weather, a short one
    // =====================================================================================================================
    private static Cutscene vaart() {
        double x = Steiger.BOOT.x, z0 = Steiger.BOOT.z;
        Cutscene.Builder b = Cutscene.maak("snuffelsteiger_vaart").duur(VAART_DUUR).verbergEcht(48);
        schip(b, x, z0, false);
        b.speler(p(12.5, 0, 29.5), -90);
        b.camera(0, p(10.5, 1.7, 26.5), p(x, -1.4, z0 + 3)).camera(VAART_DUUR, p(10.9, 1.9, 28.5), p(x, -1.4, z0 + 8));
        b.animatie(Cutscene.SPELER, 8, "spring").loop(Cutscene.SPELER, 8, 20, boot(x, z0 + Steiger.BOOT_BOEG, Steiger.BOOT_DEK));
        b.kijk(Cutscene.SPELER, 21, p(x, 0, z0 + 20));
        b.geluid(20, SnuffelsteigerFeature.PLANK_GELUID, 1f, 0.8f).geluid(36, SnuffelsteigerFeature.RIEM_GELUID, 0.9f, 0.8f);
        b.zeg(22, "kapitein", "weer", 70);
        vaar(b, 36, VAART_DUUR, x, z0 + 6, 0, true);
        b.zwart(VAART_DUUR - 16, VAART_DUUR);
        return b.registreer();
    }

    // =====================================================================================================================
    // home again: the boat comes in
    // =====================================================================================================================
    private static Cutscene thuiskomst() {
        double x = Steiger.BOOT.x, z0 = Steiger.BOOT.z;
        Cutscene.Builder b = Cutscene.maak("snuffelsteiger_thuiskomst").duur(THUISKOMST_DUUR).verbergEcht(48);
        schip(b, x, z0 + 6.5, true, -1);
        b.acteur("buur", SnuffelFeature.SNUFFEL_HOND, p(9.5, 0, 29.5), -70, tag -> {
            tag.putString("Ras", "golden");
            tag.putString("Kleur", "rood");
        });
        b.zwart(-10, 22);
        b.camera(0, p(8.5, 1.9, 25.5), p(x, -1.3, z0 + 5)).camera(110, p(9.5, 1.7, 26.5), p(x - 0.6, -1.0, z0 + 0.5));
        vaar(b, 8, 100, x, z0, 0, true, -1);
        b.geluid(12, SnuffelsteigerFeature.RIEM_GELUID, 0.8f, 0.9f).geluid(60, SnuffelsteigerFeature.RIEM_GELUID, 0.7f, 1f);
        b.animatie("buur", 1, "kwispel").geluid(26, SnuffelFeature.BLAF_GELUID, 0.7f, 1.1f).zeg(24, "buur", "daar", 44);
        b.zeg(72, "kapitein", "thuis", 50);
        b.animatie(Cutscene.SPELER, 106, "spring").loop(Cutscene.SPELER, 106, 120, p(12.5, 0, 29.5));
        b.geluid(119, SnuffelsteigerFeature.PLANK_GELUID, 1f, 0.8f).kijk(Cutscene.SPELER, 121, p(9.5, 0, 29.5));
        // (it ends in black: the boat that lies moored there looks out to sea again)
        b.zwart(THUISKOMST_DUUR - 12, THUISKOMST_DUUR);
        return b.registreer();
    }
}
