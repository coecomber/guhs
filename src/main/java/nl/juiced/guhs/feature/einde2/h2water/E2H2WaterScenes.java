package nl.juiced.guhs.feature.einde2.h2water;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.balto.BaltoFeature;
import nl.juiced.guhs.feature.einde2.appa.E2AppaFeature;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.castschurk.E2CastSchurkFeature;
import nl.juiced.guhs.feature.einde2.geestwezens.E2GeestWezensFeature;
import nl.juiced.guhs.feature.einde2.geestwezens.KohEntity;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.registry.ModEntities;

/**
 * einde2 h2_water: the thirteen cutscenes of chapter 2 (CONTRACT_E2_B 5; main's 1.4.4 engine: after every spoken line the
 * scene waits for the viewer's click, the parts without text run on). Texts: tools/features/einde2_h2_water_tekst.py
 * ({@code scene.guhs.einde2_h2_water_<naam>.*}).
 * <p>
 * The scenes in the Waterstam are written in TEMPLATE coordinates of the build ({@link S} turns them into positions
 * relative to the scene's anchor marker); the self-check of tools/features/einde2_h2_water.py reads every {@code cam(} /
 * {@code knip(} line of this file and proves against the build that no camera stands in a block and nothing of the
 * building stands between a camera and what it looks at. The scenes of the Geestenwereld and the snow plain are written
 * relative to their anchor ({@code S.rel}: anchor = 0,0,0), the party room of the wrong dimension stands in the open air
 * above the oasis (block displays, lit by themselves).
 * <p>
 * Running time (without the waits): aankomst 15 s, pakguh 28 s, blauwe_geest 29 s, maanvis 26 s, <b>geest_reis 54 s</b>,
 * koh 21 s, la_woede 14 s, <b>yue_maan 53.5 s</b>, <b>golf 50.5 s</b>, sneeuwstorm 26 s, team 20 s, knipoog_balto 9.8 s,
 * knipoog_sjokkel 8.5 s.
 */
public final class E2H2WaterScenes {
    public static Cutscene AANKOMST, PAKGUH, BLAUWE_GEEST, MAANVIS, GEEST_REIS, KOH, LA_WOEDE, YUE_MAAN, GOLF, STORM, TEAM, KNIPOOG_BALTO,
            KNIPOOG_SJOKKEL;

    private E2H2WaterScenes() {
    }

    /** Every scene of the chapter (dev command, wipe). */
    public static List<Cutscene> alle() {
        return List.of(AANKOMST, PAKGUH, BLAUWE_GEEST, MAANVIS, GEEST_REIS, KOH, LA_WOEDE, YUE_MAAN, GOLF, STORM, TEAM, KNIPOOG_BALTO, KNIPOOG_SJOKKEL);
    }

    /** Registers them (E2H2WaterFeature.register, both sides). */
    static void registreer() {
        AANKOMST = aankomst();
        PAKGUH = pakguh();
        BLAUWE_GEEST = blauweGeest();
        MAANVIS = maanvis();
        GEEST_REIS = geestReis();
        KOH = koh();
        LA_WOEDE = laWoede();
        YUE_MAAN = yueMaan();
        GOLF = golf();
        STORM = storm();
        TEAM = team();
        KNIPOOG_BALTO = knipoogBalto();
        KNIPOOG_SJOKKEL = knipoogSjokkel();
    }

    // =====================================================================================================================
    // the helper: a scene in template coordinates of its build
    // =====================================================================================================================

    /** A scene being written: positions in template coordinates of the build, the anchor marker's block at (ax, ay, az). */
    static final class S {
        final Cutscene.Builder b;
        final double ax, ay, az;

        S(String naam, int duur, double ax, double ay, double az) {
            this.b = Cutscene.maak("einde2_h2_water_" + naam).duur(duur).bij(E2Verhaalijn.ID).kaart(E2H2Water.KAART);
            this.ax = ax;
            this.ay = ay;
            this.az = az;
        }

        /** A scene written relative to its anchor (the Geestenwereld, the snow plain). */
        static S rel(String naam, int duur) {
            return new S(naam, duur, 0, 0, 0);
        }

        Vec3 r(double x, double y, double z) {
            return new Vec3(x - ax, y - ay, z - az);
        }

        S cam(int t, double x, double y, double z, double kx, double ky, double kz) {
            b.camera(t, r(x, y, z), r(kx, ky, kz));
            return this;
        }

        S knip(int t, double x, double y, double z, double kx, double ky, double kz) {
            b.cameraKnip(t, r(x, y, z), r(kx, ky, kz));
            return this;
        }

        S volg(int t, double x, double y, double z, String acteur) {
            b.cameraVolgt(t, r(x, y, z), acteur);
            return this;
        }

        S npc(String naam, Kind kind, double x, double y, double z, float yaw) {
            b.npc(naam, kind, r(x, y, z), yaw);
            return this;
        }

        S guh(String naam, GuhVariant variant, double x, double y, double z, float yaw) {
            b.guh(naam, variant, r(x, y, z), yaw);
            return this;
        }

        S acteur(String naam, Supplier<? extends EntityType<?>> type, double x, double y, double z, float yaw, Consumer<CompoundTag> nbt) {
            b.acteur(naam, type, r(x, y, z), yaw, nbt);
            return this;
        }

        S speler(double x, double y, double z, float yaw) {
            b.speler(r(x, y, z), yaw);
            return this;
        }

        S loop(String acteur, int t0, int t1, double x, double y, double z) {
            b.loop(acteur, t0, t1, r(x, y, z));
            return this;
        }

        /** The actor jumps to a spot (during a cut or a fade). */
        S zet(String acteur, int t, double x, double y, double z) {
            b.loop(acteur, t, t + 1, r(x, y, z));
            return this;
        }

        S kijk(String acteur, int t, double x, double y, double z) {
            b.kijk(acteur, t, r(x, y, z));
            return this;
        }

        S anim(String acteur, int t, String naam) {
            b.animatie(acteur, t, naam);
            return this;
        }

        S zeg(int t, String spreker, String key, int ticks) {
            b.zeg(t, spreker, key, ticks);
            return this;
        }

        S geluid(int t, Supplier<SoundEvent> s, float volume, float pitch) {
            b.geluid(t, s, volume, pitch);
            return this;
        }

        S deeltjes(int t, ParticleOptions p, double x, double y, double z, int n, double spreiding) {
            b.deeltjes(t, p, r(x, y, z), n, spreiding);
            return this;
        }

        S schud(int t, float kracht, int ticks) {
            b.schud(t, kracht, ticks);
            return this;
        }

        S zwart(int t0, int t1) {
            b.zwart(t0, t1);
            return this;
        }

        S verberg(double straal) {
            b.verbergEcht(straal);
            return this;
        }

        Cutscene klaar() {
            return b.registreer();
        }
    }

    /** The id of the line the scenes belong to (the Guhdex replay button). */
    static final class E2Verhaalijn {
        static final String ID = nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal.WATER;
    }

    // --- actor data ----------------------------------------------------------------------------------------------------------------

    static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }

    private static CompoundTag vorm(float sx, float sy, float sz) {
        CompoundTag v = new CompoundTag();
        v.put("translation", floats(0f, 0f, 0f));
        v.put("left_rotation", floats(0f, 0f, 0f, 1f));
        v.put("scale", floats(sx, sy, sz));
        v.put("right_rotation", floats(0f, 0f, 0f, 1f));
        return v;
    }

    private static CompoundTag licht() {
        CompoundTag l = new CompoundTag();
        l.putInt("block", 15);
        l.putInt("sky", 15);
        return l;
    }

    /** A block display (its corner is the actor's spot), lit by itself. */
    static Consumer<CompoundTag> blok(String naam, float sx, float sy, float sz) {
        return blok(naam, null, sx, sy, sz);
    }

    static Consumer<CompoundTag> blok(String naam, String[] eigenschappen, float sx, float sy, float sz) {
        return tag -> {
            CompoundTag b = new CompoundTag();
            b.putString("Name", naam);
            if (eigenschappen != null) {
                CompoundTag p = new CompoundTag();
                for (int i = 0; i + 1 < eigenschappen.length; i += 2) {
                    p.putString(eigenschappen[i], eigenschappen[i + 1]);
                }
                b.put("Properties", p);
            }
            tag.put("block_state", b);
            tag.put("transformation", vorm(sx, sy, sz));
            tag.put("brightness", licht());
        };
    }

    /** An item display (a thing in the air: a feather, a barrel, a chest), lit by itself. */
    static Consumer<CompoundTag> ding(String item, float schaal) {
        return tag -> {
            CompoundTag stapel = new CompoundTag();
            stapel.putString("id", item);
            stapel.putInt("count", 1);
            tag.put("item", stapel);
            tag.putString("item_display", "ground");
            tag.putString("billboard", "vertical");
            tag.put("transformation", vorm(schaal, schaal, schaal));
            tag.put("brightness", licht());
        };
    }

    /** A text display with a translated line (the banner of the party). */
    static Consumer<CompoundTag> tekst(String key, float schaal, int achtergrond) {
        return tag -> {
            CompoundTag t = new CompoundTag();
            t.putString("translate", key);
            t.putString("color", "#FF4FA8");
            t.putBoolean("bold", true);
            tag.put("text", t);
            tag.putString("billboard", "center");
            tag.putInt("line_width", 400);
            tag.putInt("background", achtergrond);
            tag.put("transformation", vorm(schaal, schaal, schaal));
            tag.put("brightness", licht());
        };
    }

    /** A creature with a look (NBT Soort), standing still. */
    static Consumer<CompoundTag> soort(int soort) {
        return tag -> {
            tag.putInt("Soort", soort);
            tag.putBoolean("Stil", true);
        };
    }

    /** A creature with a lasting pose (NBT Houding), standing still. */
    static Consumer<CompoundTag> houding(String houding) {
        return tag -> {
            tag.putString("Houding", houding);
            tag.putBoolean("Stil", true);
        };
    }

    /** A spirit koi (0 Tui, 1 La) at the vanilla scale attribute (giant in the visions). */
    static Consumer<CompoundTag> koi(int soort, double schaal) {
        return tag -> {
            tag.putInt("Soort", soort);
            tag.putBoolean("Stil", true);
            ListTag attr = new ListTag();
            CompoundTag s = new CompoundTag();
            s.putString("id", "minecraft:scale");
            s.putDouble("base", schaal);
            attr.add(s);
            tag.put("attributes", attr);
        };
    }

    private static Supplier<SoundEvent> vanilla(net.minecraft.core.Holder<SoundEvent> h) {
        return h::value;
    }

    // =====================================================================================================================
    // 0. aankomst: Appa lands outside the ice wall, the gate guard, "Pakguh is de beste"
    // =====================================================================================================================

    private static Cutscene aankomst() {
        S s = new S("aankomst", 300, 126, 31, 159);
        s.acteur("appa", E2AppaFeature.APPA_GUH, 126.5, 48, 196.5, 180, null)
                .speler(126.5, 51.2, 196.8, 180)
                .npc("aang", Kind.AANG_GUH, 127.6, 51.2, 196.2, 180)
                .acteur("momo", E2AppaFeature.MOMO_GUH, 125.6, 51.6, 197.2, 180, null)
                .npc("wachter", Kind.WATERSTAM_WACHTER, 117.5, 31, 155.5, -66)
                .verberg(12)
                .zwart(-8, 4);
        // 1. the ice wall from above the sea
        s.cam(0, 112.5, 58, 222.5, 114.5, 40, 156.5).cam(70, 113.5, 54, 210.5, 116.5, 38, 156.5)
                .zeg(10, "", "intro", 56);
        // 2. Appa comes in and lands on the ice
        s.loop("appa", 0, 92, 127.5, 31, 162.5)
                .loop(Cutscene.SPELER, 0, 92, 127.0, 34.2, 162.6).loop("aang", 0, 92, 128.1, 34.2, 162.1).loop("momo", 0, 92, 126.1, 34.6, 163.2)
                .knip(72, 136.5, 37, 170.5, 127.5, 33, 163.5).cam(104, 135.5, 35.5, 168.5, 126.5, 32.5, 160.5)
                .geluid(74, E2AppaFeature.BRUL, 1.0f, 1.0f)
                .deeltjes(92, ParticleTypes.SNOWFLAKE, 127.5, 31.6, 162.5, 50, 2.0)
                .loop(Cutscene.SPELER, 98, 110, 125.5, 31, 158.5).loop("aang", 102, 114, 127.5, 31, 158.0).loop("momo", 104, 116, 126.5, 31, 157.4);
        // 3. halt! who goes there?
        s.knip(118, 121.5, 33.4, 165.5, 122.5, 32.6, 157.5)
                .kijk("wachter", 118, 125.5, 32, 158.5).anim("wachter", 120, "halt")
                .zeg(122, "wachter", "halt", 48)
                .kijk(Cutscene.SPELER, 124, 117.5, 32.5, 155.5).kijk("aang", 124, 117.5, 32.5, 155.5)
                .anim("aang", 172, "lach").zeg(172, "aang", "aang", 48);
        // 4. open the gate
        s.knip(222, 128.5, 33.8, 163.5, 112.5, 34, 152.5).cam(298, 127.5, 34.6, 161.5, 112.5, 34.5, 151.5)
                .zeg(224, "wachter", "open", 40)
                .geluid(250, () -> SoundEvents.IRON_DOOR_OPEN, 1.0f, 0.6f)
                .zeg(266, "aang", "pakguh", 30);
        return s.klaar();
    }

    // =====================================================================================================================
    // 1. pakguh: "alleen guhs met een snor"; Katara-guh challenges him and wins his respect
    // =====================================================================================================================

    private static Cutscene pakguh() {
        S s = new S("pakguh", 560, 112, 37, 79);
        s.npc("pakguh", Kind.PAKGUH, 112.5, 37, 79.5, 0)
                .npc("katara", Kind.KATARA_GUH, 109.5, 37, 85.5, 180)
                .npc("sokka", Kind.SOKKA_GUH, 118.5, 37, 90.5, 150)
                .npc("aang", Kind.AANG_GUH, 114.5, 37, 86.0, 165)
                .speler(112.5, 37, 85.5, 180)
                .verberg(16)
                .zwart(-8, 4);
        // 1. the master looks you over
        s.cam(0, 116.5, 39.4, 91.5, 112.5, 38.2, 80.5).cam(120, 115.5, 39.2, 89.5, 112.5, 38.3, 80.0)
                .zeg(12, "pakguh", "dus", 66)
                .anim("aang", 80, "lach").zeg(82, "aang", "graag", 40);
        // 2. the moustache rule
        s.knip(126, 112.5, 38.9, 83.0, 112.5, 38.5, 79.5)
                .anim("pakguh", 130, "snor").zeg(132, "pakguh", "snor", 76);
        // 3. Katara-guh has had enough
        s.knip(210, 106.0, 39.0, 88.5, 109.5, 38.2, 85.5)
                .loop("katara", 212, 236, 110.5, 37, 83.0).kijk("katara", 236, 112.5, 38, 79.5)
                .zeg(214, "katara", "domste", 58)
                .knip(274, 114.8, 39.0, 82.5, 112.0, 38.3, 80.5)
                .anim("pakguh", 276, "neus_op").zeg(276, "pakguh", "oud", 48)
                .knip(326, 108.0, 39.2, 86.0, 110.5, 38.2, 83.0)
                .anim("katara", 326, "armen").zeg(328, "katara", "uitdaging", 60);
        // 4. the duel on the square: whips of water, a splash, nobody hurt
        s.knip(390, 121.5, 41.0, 87.5, 111.5, 38.0, 81.0)
                .anim("katara", 392, "waterzweep").anim("pakguh", 404, "waterzweep")
                .loop("katara", 396, 414, 109.5, 37, 84.0).loop("katara", 416, 432, 110.5, 37, 83.0);
        for (int t = 394; t <= 432; t += 6) {
            s.deeltjes(t, ParticleTypes.SPLASH, 111.5, 38.6, 81.5 + ((t / 6) % 2) * 0.8, 30, 0.6);
        }
        s.geluid(394, () -> SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.0f, 1.2f)
                .geluid(408, () -> SoundEvents.GENERIC_SPLASH, 1.0f, 0.9f)
                .schud(410, 0.5f, 12)
                .deeltjes(424, ParticleTypes.SNOWFLAKE, 111.5, 38.2, 81.5, 40, 1.2);
        // 5. he gives in
        s.knip(436, 113.0, 38.9, 83.5, 112.5, 38.4, 79.5)
                .anim("pakguh", 438, "snor").zeg(440, "pakguh", "zweep", 44)
                .zeg(486, "pakguh", "goed", 30)
                .knip(518, 115.5, 39.2, 88.5, 111.5, 38.2, 84.0)
                .kijk("katara", 518, 112.5, 38, 85.5).zeg(520, "katara", "kom_op", 36);
        return s.klaar();
    }

    // =====================================================================================================================
    // 9. blauwe_geest: in the brig of the VNS Schroeiplek the Blue Spirit puts the guard to sleep and opens your cell
    // =====================================================================================================================

    private static Cutscene blauweGeest() {
        S s = new S("blauwe_geest", 580, 181, 29, 201);
        s.speler(179.5, 29, 197.5, -90)
                .npc("wacht", Kind.VUURNATIE_MIKA, 183.5, 29, 196.5, 90)
                .npc("geest", Kind.ZUKA_BLAUWE_GEEST, 185.5, 29, 194.5, 135)
                .acteur("veer", () -> EntityType.ITEM_DISPLAY, 185.5, 30.4, 194.5, 0, ding("minecraft:feather", 0.4f))
                .verberg(12)
                .zwart(-8, 6);
        // 1. the brig: tied to a post, dark, damp, smells of charcoal
        s.anim(Cutscene.SPELER, 2, "buk")
                .cam(0, 182.5, 30.8, 194.6, 179.5, 29.8, 197.5).cam(70, 182.2, 30.9, 194.8, 179.8, 29.9, 197.3)
                .zeg(10, "", "cel", 56);
        // 2. something comes down the stairs: the Blue Spirit, behind the guard, a feather
        s.loop("geest", 72, 120, 184.6, 29, 195.6).loop("veer", 72, 120, 184.6, 30.4, 195.6)
                .knip(70, 180.5, 30.6, 195.0, 184.0, 30.0, 196.5)
                .loop("veer", 124, 132, 183.9, 30.6, 196.3)
                .geluid(134, () -> SoundEvents.FOX_SLEEP, 1.0f, 0.7f)
                .zeg(138, "", "wacht", 52);
        // 3. to you; the ropes come loose
        s.loop("veer", 192, 196, 184.6, 27.0, 195.6)
                .loop("geest", 194, 222, 180.6, 29, 196.9)
                .knip(194, 182.0, 30.4, 198.2, 180.5, 30.5, 197.0)
                .geluid(224, () -> SoundEvents.WOOL_BREAK, 1.0f, 0.9f)
                .anim(Cutscene.SPELER, 226, "sta")
                .zeg(230, Cutscene.SPELER, "wie", 40);
        // 4. he says nothing: a finger on the mask; he points at the stairs, the way out
        s.anim("geest", 272, "buig")
                .zeg(274, "", "stil", 56)
                .kijk("geest", 332, 185.5, 31, 194.0)
                .zeg(336, "", "weg", 58)
                .loop("geest", 398, 440, 185.5, 29, 194.4)
                .knip(398, 180.0, 30.8, 195.2, 185.5, 30.5, 194.5)
                .zeg(448, Cutscene.SPELER, "dank", 40)
                .zeg(496, "", "uitleg", 64);
        return s.klaar();
    }

    // =====================================================================================================================
    // 10. maanvis: Zhao-Mika takes Tui from the oasis; the world turns grey
    // =====================================================================================================================

    private static Cutscene maanvis() {
        S s = new S("maanvis", 520, 112, 43, 20);
        s.speler(108.5, 43, 26.5, 180)
                .npc("aang", Kind.AANG_GUH, 106.5, 43, 25.5, 160)
                .npc("zhao", Kind.ZHAO_MIKA, 112.5, 43, 31.5, 180)
                .npc("soldaat1", Kind.VUURNATIE_MIKA, 111.0, 43, 32.0, 180)
                .npc("soldaat2", Kind.VUURNATIE_MIKA, 114.0, 43, 32.0, 180)
                .acteur("tui", E2CastSchurkFeature.MAANVIS, 109.5, 42.2, 20.5, 90, houding("zwem"))
                .acteur("la", E2CastSchurkFeature.OCEAANVIS, 115.5, 42.2, 20.5, 270, houding("zwem"))
                .verberg(12)
                .zwart(-8, 4);
        // 1. the warm oasis, the two fish
        s.cam(0, 112.5, 47.5, 13.5, 112.5, 43.5, 24.5).cam(84, 112.0, 47.0, 14.5, 112.0, 43.0, 22.5)
                .loop("tui", 0, 80, 110.5, 42.2, 18.8).loop("la", 0, 80, 114.5, 42.2, 22.2)
                .zeg(10, "", "oase", 64);
        // 2. Zhao-Mika walks in with two soldiers
        s.loop("zhao", 84, 132, 112.5, 43, 25.0).loop("soldaat1", 84, 132, 110.5, 43, 26.8).loop("soldaat2", 84, 132, 114.5, 43, 26.8)
                .knip(86, 116.5, 45.0, 22.5, 112.5, 44.0, 28.5)
                .cam(134, 116.0, 45.0, 22.0, 112.5, 44.2, 25.5)
                .anim("zhao", 140, "opschep").zeg(140, "zhao", "geschiedenis", 66)
                .kijk(Cutscene.SPELER, 140, 112.5, 44, 25).kijk("aang", 140, 112.5, 44, 25)
                .knip(208, 107.5, 44.6, 23.2, 107.5, 44.2, 26.0)
                .zeg(210, "aang", "nee", 40);
        // 3. a net, a splash: Tui is in the bag
        s.knip(250, 107.5, 45.2, 22.0, 110.5, 43.2, 21.5)
                .loop("soldaat1", 252, 270, 110.5, 43, 22.6)
                .loop("tui", 276, 292, 110.5, 44.2, 22.4).loop("tui", 300, 322, 112.4, 44.4, 24.4)
                .loop("soldaat1", 300, 322, 111.3, 43, 25.2)
                .deeltjes(278, ParticleTypes.SPLASH, 110.5, 42.6, 21.5, 40, 0.5)
                .geluid(278, () -> SoundEvents.GENERIC_SPLASH, 1.0f, 1.1f)
                .anim("tui", 292, "gevangen")
                .knip(326, 115.5, 45.0, 22.0, 112.5, 44.5, 25.0)
                .anim("zhao", 330, "opschep").zeg(330, "zhao", "lach", 60);
        // 4. La alone; the colour drains out of everything
        s.knip(396, 117.5, 44.5, 23.5, 115.0, 42.5, 20.5)
                .loop("la", 396, 470, 113.8, 42.2, 18.4)
                .loop("zhao", 410, 470, 112.5, 43, 33.0).loop("soldaat1", 410, 470, 111.0, 43, 33.0).loop("soldaat2", 410, 470, 114.0, 43, 33.0)
                .loop("tui", 410, 470, 112.4, 44.4, 32.4);
        for (int t = 400; t <= 460; t += 10) {
            s.deeltjes(t, ParticleTypes.WHITE_ASH, 112.5, 46, 20.5, 60, 5.0);
        }
        s.zeg(402, "", "grijs", 64)
                .knip(470, 107.0, 44.6, 23.0, 107.0, 44.2, 25.5)
                .kijk("aang", 470, 108.5, 44, 26.5)
                .zeg(472, "aang", "koh", 36)
                .zwart(508, 519);
        return s.klaar();
    }

    // =====================================================================================================================
    // 11. geest_reis: the meditation, "super saiyan", the wrong dimension (the surprise party), "Oh wacht.. mijn fout..."
    // =====================================================================================================================

    /** The party room of the wrong dimension: in the open air high above the oasis (template y 100, above the build). */
    private static final double FY = 100;

    private static Cutscene geestReis() {
        S s = new S("geest_reis", 1080, 112, 43, 20);
        s.speler(110.5, 43, 23.5, 180)
                .npc("aang", Kind.AANG_GUH, 113.5, 43, 23.5, 180)
                .verberg(10)
                .zwart(-8, 4);
        // the room: floor, walls, ceiling, streamers, a table with a cake (and the cake once it sank), balloons, the banner
        s.acteur("vloer", () -> EntityType.BLOCK_DISPLAY, 104, FY - 0.3, 12, 0, blok("minecraft:pink_concrete", 16f, 0.3f, 16f))
                .acteur("muur_n", () -> EntityType.BLOCK_DISPLAY, 104, FY, 12, 0, blok("minecraft:white_concrete", 16f, 6f, 0.3f))
                .acteur("muur_z", () -> EntityType.BLOCK_DISPLAY, 104, FY, 27.7, 0, blok("minecraft:white_concrete", 16f, 6f, 0.3f))
                .acteur("muur_w", () -> EntityType.BLOCK_DISPLAY, 104, FY, 12, 0, blok("minecraft:white_concrete", 0.3f, 6f, 16f))
                .acteur("muur_o", () -> EntityType.BLOCK_DISPLAY, 119.7, FY, 12, 0, blok("minecraft:white_concrete", 0.3f, 6f, 16f))
                .acteur("plafond", () -> EntityType.BLOCK_DISPLAY, 104, FY + 6, 12, 0, blok("minecraft:white_concrete", 16f, 0.3f, 16f))
                .acteur("slinger_1", () -> EntityType.BLOCK_DISPLAY, 104.3, FY + 5.0, 12.4, 0, blok("minecraft:pink_wool", 15.4f, 0.25f, 0.1f))
                .acteur("slinger_2", () -> EntityType.BLOCK_DISPLAY, 104.3, FY + 4.6, 12.4, 0, blok("minecraft:yellow_wool", 15.4f, 0.2f, 0.1f))
                .acteur("slinger_3", () -> EntityType.BLOCK_DISPLAY, 104.3, FY + 5.0, 27.5, 0, blok("minecraft:light_blue_wool", 15.4f, 0.25f, 0.1f))
                .acteur("slinger_4", () -> EntityType.BLOCK_DISPLAY, 104.3, FY + 4.6, 27.5, 0, blok("minecraft:lime_wool", 15.4f, 0.2f, 0.1f))
                .acteur("tafel", () -> EntityType.BLOCK_DISPLAY, 110.5, FY, 19.0, 0, blok("minecraft:oak_planks", 3f, 0.9f, 1.6f))
                .acteur("taart", () -> EntityType.BLOCK_DISPLAY, 111.5, FY + 0.9, 19.3, 0, blok("minecraft:cake", 1f, 1f, 1f))
                .acteur("taart_in", () -> EntityType.BLOCK_DISPLAY, 111.5, FY - 3, 19.3, 0, blok("minecraft:cake", new String[]{"bites", "6"}, 1f, 0.6f, 1f))
                .acteur("ballon_1", () -> EntityType.BLOCK_DISPLAY, 106.0, FY + 4.3, 22.0, 0, blok("minecraft:red_wool", 0.7f, 0.8f, 0.7f))
                .acteur("ballon_2", () -> EntityType.BLOCK_DISPLAY, 117.2, FY + 4.1, 21.0, 0, blok("minecraft:yellow_wool", 0.7f, 0.8f, 0.7f))
                .acteur("ballon_3", () -> EntityType.BLOCK_DISPLAY, 108.2, FY + 4.4, 16.5, 0, blok("minecraft:light_blue_wool", 0.7f, 0.8f, 0.7f))
                .acteur("ballon_4", () -> EntityType.BLOCK_DISPLAY, 115.6, FY + 4.2, 25.5, 0, blok("minecraft:pink_wool", 0.7f, 0.8f, 0.7f))
                .acteur("spandoek", () -> EntityType.TEXT_DISPLAY, 112.0, FY + 3.6, 26.9, 0,
                        tekst("scene.guhs.einde2_h2_water_geest_reis.spandoek", 1.6f, 0x60FFFFFF))
                .guh("guh1", GuhVariant.NORMAL, 108.5, FY, 23.5, 180)
                .guh("guh2", GuhVariant.MINT, 111.0, FY, 24.6, 180)
                .acteur("mika1", ModEntities.MIKA, 113.6, FY, 23.8, 180, null)
                .guh("guh3", GuhVariant.CHOCO, 116.0, FY, 23.0, 180)
                .acteur("mika2", ModEntities.MIKA, 109.6, FY, 26.0, 180, null)
                .guh("guh4", GuhVariant.RAINBOW, 114.5, FY, 26.2, 180)
                .acteur("mika3", ModEntities.MIKA, 117.0, FY, 25.6, 160, null);
        String[] gasten = {"guh1", "guh2", "mika1", "guh3", "mika2", "guh4", "mika3"};
        // 1. by the pond: eyes shut, breathe
        s.cam(0, 112.0, 45.5, 28.0, 112.0, 43.9, 22.5).cam(200, 112.0, 44.6, 26.2, 112.0, 43.9, 22.8)
                .anim(Cutscene.SPELER, 4, "buk").anim("aang", 4, "buk")
                .zeg(20, "aang", "zitten", 50)
                .zeg(90, "", "gloed", 60);
        for (int t = 96; t <= 212; t += 8) {
            float k = Math.min(1f, (t - 90) / 100f);
            s.deeltjes(t, ParticleTypes.GLOW, 110.5, 44.0, 23.5, (int) (6 + 20 * k), 0.5);
            s.deeltjes(t, ParticleTypes.GLOW, 113.5, 44.0, 23.5, (int) (6 + 20 * k), 0.5);
            if (t > 150) {
                s.deeltjes(t, ParticleTypes.END_ROD, 112.0, 44.2, 23.5, 12, 1.2);
            }
        }
        s.anim("aang", 168, "lach").zeg(170, "aang", "jaaa", 40)
                .schud(200, 0.8f, 16)
                .zwart(212, 238);
        // 2. through a sort of portal into a VERY festive room
        s.anim(Cutscene.SPELER, 226, "sta").anim("aang", 226, "sta")
                .zet(Cutscene.SPELER, 226, 110.8, FY, 15.4).zet("aang", 226, 113.4, FY, 15.4)
                .kijk(Cutscene.SPELER, 228, 111.5, FY + 1, 24).kijk("aang", 228, 112.5, FY + 1, 24)
                .knip(226, 112.5, FY + 2.6, 26.6, 112.0, FY + 1.2, 15.0)
                .deeltjes(238, ParticleTypes.REVERSE_PORTAL, 112.0, FY + 1.5, 13.2, 80, 1.0)
                .geluid(238, () -> SoundEvents.PORTAL_TRAVEL, 0.3f, 1.6f)
                .geluid(242, E2Audio.geluid("stinger.feestje"), 1.0f, 1.0f)
                .geluid(242, () -> SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f, 1.0f)
                .geluid(246, () -> SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0f, 1.2f);
        for (int t = 242; t <= 300; t += 6) {
            s.deeltjes(t, ParticleTypes.TOTEM_OF_UNDYING, 112.0, FY + 3.5, 21.0, 40, 3.0);
        }
        for (String g : gasten) {
            s.anim(g, 244, "spring").anim(g, 262, "spring");
        }
        s.zeg(244, "iedereen", "gefeli", 80);
        // 3. ... ... the awkward part
        for (String g : gasten) {
            s.kijk(g, 326, 112.0, FY + 1, 15.4);
        }
        s.knip(326, 112.2, FY + 1.7, 19.6, 112.1, FY + 1.5, 15.4)
                .zeg(330, Cutscene.SPELER, "stil1", 40)
                .knip(372, 112.0, FY + 1.9, 17.4, 112.5, FY + 1.2, 24.5)
                .zeg(376, "iedereen", "stil2", 40)
                .knip(420, 112.6, FY + 1.6, 18.8, 113.4, FY + 1.5, 15.4)
                .anim("aang", 428, "praat").zeg(430, "aang", "sorry", 50)
                .knip(484, 113.8, FY + 2.0, 18.0, 112.5, FY + 1.2, 24.5)
                .zeg(488, "iedereen", "stil3", 40)
                .knip(532, 111.4, FY + 1.6, 18.6, 110.8, FY + 1.5, 15.4)
                .zeg(536, Cutscene.SPELER, "dimensie", 62)
                .knip(602, 112.2, FY + 1.9, 18.0, 113.0, FY + 1.2, 24.0)
                .zeg(606, "iedereen", "vadsjes", 62);
        // 4. the cake sinks
        s.knip(670, 113.6, FY + 1.8, 17.2, 112.0, FY + 1.1, 19.8)
                .zet("taart", 676, 111.5, FY - 3, 19.3).zet("taart_in", 676, 111.5, FY + 0.9, 19.3)
                .geluid(676, () -> SoundEvents.HONEY_BLOCK_SLIDE, 1.0f, 0.7f)
                .zeg(680, "", "taart", 40)
                .knip(722, 112.0, FY + 1.7, 19.0, 111.4, FY + 1.5, 15.4)
                .zeg(724, Cutscene.SPELER, "njeg", 50)
                .knip(776, 113.0, FY + 1.6, 18.0, 113.4, FY + 1.5, 15.4)
                .anim("aang", 778, "lach").zeg(780, "aang", "fout", 66);
        // 5. back through the portal; your bodies stay by the pond, your souls go on: this time to the right world
        s.deeltjes(850, ParticleTypes.REVERSE_PORTAL, 112.0, FY + 1.5, 15.0, 120, 1.4)
                .geluid(850, () -> SoundEvents.PORTAL_TRAVEL, 0.3f, 1.9f)
                .zwart(856, 884)
                .zet(Cutscene.SPELER, 870, 110.5, 43, 23.5).zet("aang", 870, 113.5, 43, 23.5)
                .anim(Cutscene.SPELER, 872, "buk").anim("aang", 872, "buk")
                .kijk(Cutscene.SPELER, 872, 110.5, 44, 20).kijk("aang", 872, 113.5, 44, 20)
                .knip(870, 112.0, 44.6, 26.0, 112.0, 43.9, 22.8)
                .cam(1070, 112.0, 52.0, 26.5, 112.0, 44.0, 21.5);
        for (int t = 884; t <= 1060; t += 12) {
            s.deeltjes(t, ParticleTypes.GLOW, 110.5, 44.0, 23.5, 8, 0.4).deeltjes(t, ParticleTypes.GLOW, 113.5, 44.0, 23.5, 8, 0.4);
        }
        s.zeg(900, "", "lijf", 64)
                .zeg(990, "aang", "echt", 44)
                .zwart(1050, 1079);
        return s.klaar();
    }

    // =====================================================================================================================
    // 12. koh: the face stealer in his cave (the test itself is play: Geestreis)
    // =====================================================================================================================

    private static Cutscene koh() {
        S s = S.rel("koh", 420);
        s.speler(0.5, 0, 9.5, 180)
                .npc("aang", Kind.AANG_GUH, 2.5, 0, 10.0, 170)
                .acteur("koh", E2GeestWezensFeature.KOH, 0.5, 0, -7.0, 0, soort(KohEntity.MASKER))
                .verberg(16)
                .zwart(-8, 4);
        s.cam(0, 4.5, 3.0, 13.5, 0.5, 2.0, 1.0)
                .zeg(10, "", "grot", 58)
                .loop("koh", 70, 124, 0.5, 0, 0.5)
                .geluid(72, E2Audio.geluid("geest.koh_kraak"), 1.0f, 0.8f)
                .knip(118, 1.5, 1.4, 6.5, 0.5, 3.0, 0.5)
                .zeg(130, "aang", "waarschuw", 70)
                .knip(204, 0.0, 2.0, 7.5, 0.5, 3.4, 0.5)
                .zeg(210, "koh", "welkom", 60)
                .zeg(280, "koh", "vraag", 70)
                .knip(354, 3.5, 1.8, 11.5, 1.5, 1.4, 9.5)
                .anim("aang", 356, "lach").zeg(360, "aang", "succes", 44);
        return s.klaar();
    }

    // =====================================================================================================================
    // 13. la_woede: at the Hemelvijver a vision of the angry ocean spirit
    // =====================================================================================================================

    private static Cutscene laWoede() {
        S s = S.rel("la_woede", 280);
        s.speler(0.5, 0, 0.5, -90)
                .npc("aang", Kind.AANG_GUH, -0.5, 0, 2.2, -100)
                .acteur("la", E2GeestWezensFeature.GEEST_KOI, 30.0, -10, 0.5, 90, koi(1, 9.0))
                .zwart(-8, 4);
        s.cam(0, -2.5, 2.8, 4.5, 24.0, 6.0, 0.5)
                .zeg(10, "", "vijver", 50)
                .loop("la", 60, 112, 26.0, 2.0, 0.5)
                .geluid(64, () -> SoundEvents.ELDER_GUARDIAN_CURSE, 0.8f, 0.6f)
                .schud(96, 1.2f, 20);
        for (int t = 64; t <= 112; t += 8) {
            s.deeltjes(t, ParticleTypes.SPLASH, 26.0, 0.0, 0.5, 80, 4.0);
        }
        s.knip(110, 2.5, 1.0, 1.5, 24.0, 8.0, 0.5)
                .zeg(120, "", "visioen", 60)
                .zeg(190, "aang", "maan", 56)
                .loop("la", 230, 262, 30.0, -10, 0.5)
                .zwart(255, 279);
        return s.klaar();
    }

    // =====================================================================================================================
    // 15. yue_maan: the moon fish back in the pond; Yue-guh gives back the light the moon once gave her
    // =====================================================================================================================

    private static Cutscene yueMaan() {
        S s = new S("yue_maan", 1070, 112, 43, 20);
        s.speler(110.5, 43, 25.0, 180)
                .npc("yue", Kind.YUE_GUH, 113.5, 43, 25.5, 180)
                .npc("katara", Kind.KATARA_GUH, 108.5, 43, 26.0, 160)
                .npc("sokka", Kind.SOKKA_GUH, 114.8, 43, 26.5, 200)
                .npc("aang", Kind.AANG_GUH, 107.0, 43, 25.0, 170)
                .acteur("tui", E2CastSchurkFeature.MAANVIS, 110.6, 43.6, 24.4, 0, houding("gevangen"))
                .acteur("la", E2CastSchurkFeature.OCEAANVIS, 115.5, 42.2, 20.5, 270, houding("zwem"))
                .npc("yue_geest", Kind.YUE_GUH_MAAN, 112.5, 38.5, 20.5, 180)
                .verberg(12)
                .zwart(-8, 4);
        // 1. the fish back in the pond, but too weak
        s.cam(0, 112.5, 47.5, 27.5, 112.5, 43.5, 21.5).cam(110, 112.0, 46.5, 27.0, 111.5, 43.0, 21.5)
                .loop("tui", 20, 50, 110.5, 42.3, 21.2)
                .deeltjes(50, ParticleTypes.SPLASH, 110.5, 42.6, 21.2, 20, 0.3)
                .loop("la", 0, 110, 113.8, 42.2, 18.6)
                .zeg(10, "", "terug", 60)
                .zeg(80, "katara", "nee", 40);
        // 2. Yue-guh: the light the moon gave her
        s.knip(120, 115.0, 44.5, 27.5, 113.5, 44.2, 25.5)
                .kijk("yue", 122, 110.5, 43, 21)
                .zeg(130, "yue", "ik", 70)
                .knip(206, 117.5, 44.5, 26.0, 114.0, 44.0, 26.0)
                .kijk("sokka", 206, 113.5, 44, 25.5).kijk("yue", 212, 114.8, 44, 26.5)
                .zeg(210, "sokka", "niet", 50)
                .zeg(270, "yue", "geven", 80)
                .zeg(360, "sokka", "beloof", 50)
                .zeg(420, "yue", "belofte", 44);
        // 3. she walks to the pond; light rises; she becomes the moon spirit
        s.loop("yue", 470, 520, 112.5, 43, 23.0)
                .knip(470, 112.5, 44.5, 27.5, 112.5, 44.0, 21.5).cam(600, 112.5, 47.5, 27.0, 112.5, 44.5, 20.5)
                .anim("yue", 522, "maan");
        for (int t = 480; t <= 640; t += 8) {
            s.deeltjes(t, ParticleTypes.END_ROD, 112.5, 44.0 + (t - 480) / 40.0, 21.5, 14, 0.6);
        }
        s.deeltjes(600, ParticleTypes.END_ROD, 112.5, 44.5, 22.0, 120, 1.6)
                .zet("yue", 602, 112.5, 38.0, 23.0)
                .loop("yue_geest", 604, 640, 112.5, 43.2, 20.5)
                .geluid(604, () -> SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.8f)
                .geluid(620, () -> SoundEvents.BEACON_ACTIVATE, 0.8f, 1.2f);
        // 4. Tui lives; the colour comes back
        s.loop("tui", 640, 680, 109.4, 42.3, 19.4).loop("tui", 680, 720, 110.6, 42.3, 18.2).loop("tui", 720, 760, 111.6, 42.3, 19.4)
                .loop("la", 640, 700, 115.2, 42.3, 21.4).loop("la", 700, 760, 114.4, 42.3, 22.4)
                .zeg(650, "", "licht", 70)
                .knip(726, 112.5, 44.6, 24.5, 112.5, 44.6, 20.5)
                .zeg(730, "yue_geest", "dank", 60)
                .knip(788, 106.8, 44.6, 24.0, 108.5, 44.2, 26.0)
                .zeg(790, "katara", "dankje", 40)
                .kijk("sokka", 836, 112.5, 60, 18)
                .knip(836, 116.5, 44.8, 28.0, 114.8, 44.2, 26.5)
                .zeg(840, "sokka", "zwaai", 60).anim("sokka", 842, "zwaai");
        // 5. the moon over the Waterstam, a little pink, with two tiny ears if you look well
        s.knip(902, 112.5, 43.9, 22.4, 112.5, 70.0, 17.5).cam(1060, 112.5, 44.4, 23.0, 112.5, 76.0, 16.0)
                .zeg(910, "", "maan", 70)
                .zeg(990, "aang", "nu", 56)
                .zwart(1052, 1069);
        return s.klaar();
    }

    // =====================================================================================================================
    // 16. golf: on the Golfkansel; La rises; a wave as high as the wall carries the fleet away, gently; Zuka in the shadow
    // =====================================================================================================================

    /** The three ships of the fleet as block displays (laag_vloot: middle x, bow z, half width, length; overlay at 50, 31, 172). */
    private static final int[][] SCHEPEN = {{60, 184, 3, 26}, {95, 174, 4, 34}, {130, 186, 3, 26}};

    private static Cutscene golf() {
        S s = new S("golf", 1010, 112, 52, 148);
        s.speler(112.5, 52, 148.5, 0)
                .npc("aang", Kind.AANG_GUH, 110.5, 52, 148.0, 10)
                .npc("katara", Kind.KATARA_GUH, 114.5, 52, 148.0, -10)
                .npc("zhao", Kind.ZHAO_MIKA, 182.5, 43, 184.5, 180)
                .acteur("ton", () -> EntityType.ITEM_DISPLAY, 182.5, 40.0, 184.5, 0, ding("minecraft:barrel", 1.4f))
                .acteur("la", E2GeestWezensFeature.GEEST_KOI, 112.5, 18, 175.5, 180, koi(1, 10.0))
                .npc("zuka", Kind.ZUKA_BALLING, 108.5, 52, 145.6, 135)
                .verberg(10)
                .zwart(-8, 4);
        for (int i = 0; i < SCHEPEN.length; i++) {
            int xm = SCHEPEN[i][0], z0 = SCHEPEN[i][1], hb = SCHEPEN[i][2], len = SCHEPEN[i][3];
            int mid = z0 + len / 2;
            s.acteur("romp" + i, () -> EntityType.BLOCK_DISPLAY, xm - hb, 31, z0, 0, blok("guhs:roosterijzer", 2 * hb + 1, 4, len))
                    .acteur("reling" + i, () -> EntityType.BLOCK_DISPLAY, xm - hb, 35, z0 + 1, 0, blok("minecraft:red_nether_bricks", 2 * hb + 1, 1, len - 2))
                    .acteur("toren" + i, () -> EntityType.BLOCK_DISPLAY, xm - 2, 35, mid - 1, 0, blok("guhs:gepolijst_roosterijzer", 5, 8, 6))
                    .acteur("dak" + i, () -> EntityType.BLOCK_DISPLAY, xm - 2, 43, mid - 1, 0, blok("minecraft:red_nether_bricks", 5, 1, 6))
                    .acteur("pijp" + i, () -> EntityType.BLOCK_DISPLAY, xm, 35, mid + 7, 0, blok("guhs:roosterijzer_pilaar", 1, 6, 1));
            // the wave carries each ship away, slowly, out to sea (it rides up on the swell and down again)
            for (String deel : new String[]{"romp", "reling", "toren", "dak", "pijp"}) {
                double x = deel.equals("romp") || deel.equals("reling") ? xm - hb : deel.equals("pijp") ? xm : xm - 2;
                double y = deel.equals("romp") ? 31 : deel.equals("dak") ? 43 : 35;
                double z = deel.equals("romp") ? z0 : deel.equals("reling") ? z0 + 1 : deel.equals("pijp") ? mid + 7 : mid - 1;
                s.loop(deel + i, 470 + i * 8, 560 + i * 8, x, y + 3.5, z + 18).loop(deel + i, 560 + i * 8, 700, x, y - 1.0, z + 46);
            }
        }
        // 1. the fleet in front of the wall
        s.cam(0, 112.5, 58.5, 143.5, 100.0, 38.0, 195.0).cam(140, 112.5, 59.0, 144.5, 104.0, 38.0, 192.0)
                .zeg(10, "", "vloot", 70)
                .kijk("katara", 90, 112.5, 53, 148.5).zeg(90, "katara", "kun", 50)
                .kijk("aang", 148, 112.5, 53, 148.5).zeg(150, "aang", "samen", 60);
        // 2. your eyes glow
        s.knip(218, 112.5, 53.5, 154.5, 112.5, 53.4, 148.5)
                .anim(Cutscene.SPELER, 220, "spring")
                .schud(240, 0.4f, 60)
                .zeg(240, "", "gloed", 58);
        for (int t = 220; t <= 420; t += 8) {
            s.deeltjes(t, ParticleTypes.GLOW, 112.5, 53.6, 148.5, 10, 0.5).deeltjes(t, ParticleTypes.END_ROD, 112.5, 54.2, 148.5, 4, 0.8);
        }
        // 3. La rises out of the sea
        s.loop("la", 300, 380, 112.5, 34, 174.0)
                .knip(298, 150.5, 45.0, 152.5, 112.5, 38.0, 175.5)
                .geluid(300, E2Audio.geluid("geest.koi_zang"), 1.0f, 0.7f)
                .zeg(330, "", "la", 60)
                .knip(396, 112.5, 53.4, 153.2, 112.5, 53.4, 148.5)
                .zeg(400, Cutscene.SPELER, "zachtjes", 40);
        // 4. THE WAVE: a wall of water rolls out over the sea and lifts the ships away, gently
        s.knip(440, 112.5, 58.0, 143.5, 100.0, 33.0, 200.0).cam(520, 112.5, 59.0, 144.5, 100.0, 33.0, 206.0)
                .schud(450, 1.5f, 60)
                .geluid(444, () -> SoundEvents.GENERIC_SPLASH, 1.0f, 0.5f)
                .geluid(470, () -> SoundEvents.AMBIENT_UNDERWATER_ENTER, 1.0f, 0.7f)
                .loop("la", 440, 560, 112.5, 34, 205.0);
        for (int t = 440; t <= 600; t += 8) {
            double z = 166 + (t - 440) * 0.36;
            for (int x = 52; x <= 140; x += 8) {
                s.deeltjes(t, ParticleTypes.SPLASH, x, 36.5, z, 22, 1.6).deeltjes(t, ParticleTypes.CLOUD, x, 39.0, z, 3, 1.8);
            }
        }
        s.zeg(470, "", "golf", 80);
        // Zhao-Mika is carried off his bridge onto a barrel
        s.knip(522, 165.5, 40.0, 175.5, 178.0, 36.0, 195.0)
                .loop("zhao", 524, 560, 176.5, 31.6, 196.5).loop("ton", 524, 560, 176.5, 30.6, 196.5)
                .loop("zhao", 560, 700, 168.5, 31.6, 236.5).loop("ton", 560, 700, 168.5, 30.6, 236.5)
                .anim("zhao", 560, "woede").zeg(562, "zhao", "nee", 54);
        // 5. nobody hurt; the sea goes quiet
        s.knip(636, 112.5, 66.0, 215.0, 112.5, 53.5, 148.5)
                .loop("la", 700, 780, 112.5, 18, 190.0)
                .zeg(640, "", "niemand", 60)
                .knip(716, 115.5, 53.6, 152.0, 113.0, 53.2, 148.3)
                .kijk("katara", 716, 112.5, 53, 148.5).zeg(720, "katara", "jij", 50)
                .kijk("aang", 778, 112.5, 53, 148.5).zeg(780, "aang", "trots", 50)
                .knip(840, 112.5, 54.5, 153.0, 112.5, 53.3, 148.3)
                .zeg(850, "", "rust", 60);
        // 6. ...but somebody else has been waiting, in the shadow of the tower
        s.knip(916, 115.5, 54.0, 150.5, 109.0, 53.0, 146.0)
                .zeg(920, "", "maar", 50)
                .loop("zuka", 962, 978, 111.6, 52, 147.8)
                .zwart(974, 1009);
        return s.klaar();
    }

    // =====================================================================================================================
    // 17. sneeuwstorm: you wake in the blizzard; Zuka keeps you; far away a little light
    // =====================================================================================================================

    private static Cutscene storm() {
        S s = S.rel("sneeuwstorm", 520);
        s.speler(0.5, 0, 0.5, -90)
                .npc("zuka", Kind.ZUKA_BALLING, 2.5, 0, 1.5, 120)
                .verberg(8)
                .zwart(-8, 30);
        s.cam(0, -2.0, 1.8, -3.0, 1.5, 1.2, 0.5).cam(240, -1.5, 2.0, -2.5, 1.5, 1.4, 0.8)
                .anim(Cutscene.SPELER, 0, "buk").anim(Cutscene.SPELER, 60, "sta");
        for (int t = 0; t <= 510; t += 10) {
            s.deeltjes(t, ParticleTypes.SNOWFLAKE, 0.5, 2.0, 0.5, 60, 4.0).deeltjes(t, ParticleTypes.WHITE_ASH, 0.5, 2.0, 0.5, 30, 5.0);
        }
        s.geluid(0, () -> SoundEvents.WEATHER_RAIN, 0.6f, 0.5f)
                .zeg(30, "", "wit", 60)
                .knip(96, 0.0, 1.6, 4.0, 2.5, 1.6, 1.5)
                .zeg(100, "zuka", "eindelijk", 70)
                .zeg(180, Cutscene.SPELER, "waar", 40)
                .zeg(230, "zuka", "wachten", 70)
                .anim("zuka", 308, "mijn_eer").zeg(310, "zuka", "eer", 40)
                .knip(356, -1.5, 2.2, -3.0, 12.0, 1.8, -8.0)
                .zeg(360, "", "koud", 70)
                .zeg(440, "", "ga", 60);
        return s.klaar();
    }

    // =====================================================================================================================
    // 17 -> done. team: Katara-guh and Appa find you; nobody stays behind in the snow; they come along
    // =====================================================================================================================

    private static Cutscene team() {
        S s = S.rel("team", 400);
        s.speler(-3.5, 0, 0.5, -90)
                .npc("katara", Kind.KATARA_GUH, 1.0, 0, 0.5, 90)
                .npc("sokka", Kind.SOKKA_GUH, 1.5, 0, 2.5, 100)
                .npc("aang", Kind.AANG_GUH, 0.5, 0, -1.5, 80)
                .acteur("appa", E2AppaFeature.APPA_GUH, 5.5, 0, -1.5, 90, null)
                .acteur("momo", E2AppaFeature.MOMO_GUH, 2.0, 0, -0.5, 90, null)
                .zwart(-8, 4);
        for (int t = 0; t <= 390; t += 12) {
            s.deeltjes(t, ParticleTypes.SNOWFLAKE, 0.5, 2.0, 0.5, Math.max(4, 50 - t / 8), 4.0);
        }
        s.cam(0, -6.5, 2.2, 4.0, 0.5, 1.5, 0.0).cam(130, -6.0, 2.0, 3.5, 0.0, 1.4, 0.5)
                .geluid(14, E2AppaFeature.BRUL, 1.0f, 1.0f)
                .zeg(10, "katara", "daar", 50)
                .loop("katara", 60, 90, -1.5, 0, 0.5)
                .zeg(70, "sokka", "gevonden", 60)
                .knip(136, -1.0, 1.7, 3.5, -3.5, 1.6, 0.5)
                .zeg(140, Cutscene.SPELER, "zuka", 50)
                .knip(196, -4.5, 1.8, 3.0, -1.5, 1.6, 0.5)
                .zeg(200, "katara", "niemand_achter", 70)
                .knip(266, -6.0, 2.4, 3.5, 1.0, 1.6, 0.0)
                .zeg(270, "aang", "team", 50)
                .zeg(320, "katara", "ja", 40)
                .anim("sokka", 358, "plan").zeg(360, "sokka", "plan", 30);
        return s.klaar();
    }

    // =====================================================================================================================
    // the winks
    // =====================================================================================================================

    /** Wink 2: Baltoguh trudges onto the training square during Pakguh's lesson, looks at the ice city: wrong again. */
    private static Cutscene knipoogBalto() {
        S s = new S("knipoog_balto", 196, 100, 38, 87);
        s.guh("balto", GuhVariant.BALTOGUH, 80.5, 37, 89.5, -90)
                .acteur("kistje", () -> EntityType.ITEM_DISPLAY, 79.3, 37.3, 89.5, 0, ding("minecraft:chest", 0.5f))
                .npc("pakguh", Kind.PAKGUH, 112.5, 37, 79.5, 0)
                .speler(112.5, 37, 85.5, 180)
                .verberg(14)
                .zwart(-8, 4);
        s.cam(0, 104.5, 40.0, 96.5, 99.0, 38.0, 88.5).cam(120, 104.0, 39.8, 96.0, 97.5, 38.2, 89.0)
                .loop("balto", 4, 70, 97.5, 37, 89.5).loop("kistje", 4, 70, 96.3, 37.3, 89.5)
                .geluid(40, BaltoFeature.SNUIF, 1.0f, 1.0f)
                .kijk("balto", 72, 112.5, 45, 60)
                .zeg(74, "balto", "sneeuw", 48)
                .kijk("balto", 124, 112.5, 38, 79.5)
                .zeg(126, "balto", "verkeerd", 38)
                .loop("balto", 164, 196, 84.5, 37, 91.5).loop("kistje", 164, 196, 85.7, 37.3, 91.5)
                .knip(164, 108.5, 39.4, 86.5, 112.5, 38.4, 79.5)
                .kijk("pakguh", 164, 98, 38, 89)
                .anim("pakguh", 166, "snor").zeg(166, "pakguh", "snor", 28);
        return s.klaar();
    }

    /** Wink 1 goes on: Sjokkel creeps along the rim of the palace terrace, a little further than in chapter 1. */
    private static Cutscene knipoogSjokkel() {
        S s = new S("knipoog_sjokkel", 170, 90, 44, 75);
        s.acteur("sjokkel", LanddiertjesFeature.SHUCKLE, 85.5, 44, 75.5, -90, null)
                .zwart(-8, 4);
        s.cam(0, 99.0, 39.0, 86.0, 89.0, 44.3, 75.5).cam(168, 98.5, 39.4, 85.5, 91.0, 44.3, 75.5)
                .loop("sjokkel", 4, 166, 91.5, 44, 75.5)
                .geluid(10, LanddiertjesFeature.SHUCKLE_GELUID, 1.0f, 1.0f)
                .zeg(16, "", "sjokkel", 60)
                .zeg(96, "", "bijna", 60);
        return s.klaar();
    }
}
