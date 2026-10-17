package nl.juiced.guhs.feature.einde2.spd2weg;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.E2AppaFeature;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.spcast.SpCast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 "Sponsguh", slice sp_d2_weg: the scripts of the seven story scenes of part 2 steps 5-8 (CONTRACT_E2_C 5.2; texts:
 * tools/features/einde2_sp_d2_weg_tekst.py SCENES, keys {@code scene.guhs.einde2_sp_d2_weg_<naam>.*}).
 * <p>
 * Every scene is written in the TEMPLATE coordinates of Schelpenstad (the numbers of the build: report
 * slice_bouw_schelpenstad.md and its sidecar) and turned into coordinates relative to its anchor by {@link Sc#t}, so the
 * script reads like the floor plan. Schelpenstad is a leegte_bouw: never turned. Every camera steps out of a wall
 * ({@code cameraOntwijkt}); every spoken line waits for a click (1.4.4), only the monsters' song runs on
 * ({@link SpAudio#lied}). Real residents within the scene's reach are hidden while it plays ({@code verbergEcht}), and so are
 * the player's own walking companions ({@link Volgers}): the actors stand in for them.
 * <p>
 * The looks of the cast ({@link SpCast}: the moustache, dried) go into an actor's NBT through {@link SpCast#nbt}
 * ({@link #look}); a look that changes in the middle of a scene is a second actor that takes over (one is
 * moved away, the other in, on the same tick, under a puff of particles).
 */
public final class D2WegScenes {
    public static final String ID = "einde2_sp_d2_weg_";
    /** The anchors that are not a plain marker: a point of a pad ("pad#index", negative = from the end). */
    public static final String MONSTERS_ANKER = "pad_omlaag#-1", LAARS_ANKER = "dennis_route#" + E2SpD2Weg.POORT_PUNT;

    public static Cutscene KNIPOOG_APPA, SNOR, MONSTERS, DENNIS_SNOR, LAARS, CYCLOOP, SPROEIER;
    /** Scene name -> {least, most} seconds (CONTRACT_E2_C 5.2; the joke and the trench song are allowed past 30 s: report). */
    public static final Map<String, int[]> LENGTES = Map.of("knipoog_appa", new int[] {5, 10}, "snor", new int[] {20, 30},
            "monsters", new int[] {30, 45}, "dennis_snor", new int[] {20, 40}, "laars", new int[] {10, 20}, "cycloop", new int[] {30, 45},
            "sproeier", new int[] {30, 45});

    private D2WegScenes() {
    }

    static void registreer() {
        KNIPOOG_APPA = knipoogAppa();
        SNOR = snor();
        MONSTERS = monsters();
        DENNIS_SNOR = dennisSnor();
        LAARS = laars();
        CYCLOOP = cycloop();
        SPROEIER = sproeier();
    }

    // =====================================================================================================================
    // anchors and helpers
    // =====================================================================================================================

    /** The template block of an anchor (a marker, or "pad#i"); null when the sidecar does not have it. */
    @Nullable
    static BlockPos sjabloon(String anker) {
        int h = anker.indexOf('#');
        Plekken.Plek p = Plekken.plek(E2SpD2Weg.S, h < 0 ? anker : anker.substring(0, h));
        if (p == null) {
            return null;
        }
        if (h < 0) {
            return p.pos();
        }
        if (p.punten().isEmpty()) {
            return null;
        }
        int i = Integer.parseInt(anker.substring(h + 1));
        return p.punten().get(Math.floorMod(i, p.punten().size()));
    }

    /** The world block of an anchor. */
    @Nullable
    static BlockPos anker(ServerLevel g, String anker) {
        BlockPos t = sjabloon(anker);
        return t == null ? null : E2SpD2Weg.sjabloon(g, t);
    }

    /** A scene being written: its builder and its anchor's template block. */
    private static final class Sc {
        final Cutscene.Builder b;
        final BlockPos a;

        Sc(String naam, int duur, String anker, BlockPos reserve) {
            BlockPos s = sjabloon(anker);
            this.a = s != null ? s : reserve;
            this.b = Cutscene.maak(ID + naam).duur(duur).bij(SpVerhaal.KROON).cameraOntwijkt();
        }

        /** A template position (block centres are .5) relative to the anchor. */
        Vec3 t(double x, double y, double z) {
            return new Vec3(x - a.getX(), y - a.getY(), z - a.getZ());
        }

        /** Far under the floor: where an actor waits until it takes over. */
        Vec3 weg(double x, double z) {
            return t(x, -40, z);
        }

        Sc cam(int tick, double x, double y, double z, double kx, double ky, double kz) {
            b.camera(tick, t(x, y, z), t(kx, ky, kz));
            return this;
        }

        Sc knip(int tick, double x, double y, double z, double kx, double ky, double kz) {
            b.cameraKnip(tick, t(x, y, z), t(kx, ky, kz));
            return this;
        }

        Sc loop(String wie, int t0, int t1, double x, double y, double z) {
            b.loop(wie, t0, t1, t(x, y, z));
            return this;
        }

        /** Swaps an actor in (from under the floor) on one tick. */
        Sc in(String wie, int tick, double x, double y, double z) {
            b.loop(wie, tick, tick + 1, t(x, y, z));
            return this;
        }

        /** Swaps an actor away (under the floor) on one tick. */
        Sc uit(String wie, int tick, double x, double z) {
            b.loop(wie, tick, tick + 1, weg(x, z));
            return this;
        }

        Sc kijk(String wie, int tick, double x, double y, double z) {
            b.kijk(wie, tick, t(x, y, z));
            return this;
        }

        Sc anim(String wie, int tick, String naam) {
            b.animatie(wie, tick, naam);
            return this;
        }

        Sc zeg(int tick, String wie, String key, int ticks) {
            b.zeg(tick, wie, key, ticks);
            return this;
        }

        Sc geluid(int tick, Supplier<SoundEvent> s, float volume, float pitch) {
            b.geluid(tick, s, volume, pitch);
            return this;
        }

        Sc deeltjes(int tick, net.minecraft.core.particles.ParticleOptions p, double x, double y, double z, int n, double spreiding) {
            b.deeltjes(tick, p, t(x, y, z), n, spreiding);
            return this;
        }

        Sc acteur(String naam, Supplier<? extends EntityType<?>> type, double x, double y, double z, float yaw, @Nullable Consumer<CompoundTag> nbt) {
            b.acteur(naam, type, t(x, y, z), yaw, nbt);
            return this;
        }

        Sc acteurWeg(String naam, Supplier<? extends EntityType<?>> type, double x, double z, float yaw, @Nullable Consumer<CompoundTag> nbt) {
            b.acteur(naam, type, weg(x, z), yaw, nbt);
            return this;
        }
    }

    /** A cast actor that stands on its mark, with these looks on (the stand-in's way: persistent data). */
    static Consumer<CompoundTag> wezen(String... looks) {
        return wezen(-1, false, 1.0, looks);
    }

    /** The same with a Soort (-1: none), Dennis' motorbike and a scale (1: normal). */
    static Consumer<CompoundTag> wezen(int soort, boolean motor, double schaal, String... looks) {
        return tag -> {
            tag.putBoolean("Stil", true);
            if (soort >= 0) {
                tag.putInt("Soort", soort);
            }
            if (motor) {
                tag.putBoolean("OpMotor", true);
            }
            if (schaal != 1.0) {
                ListTag attr = new ListTag();
                CompoundTag s = new CompoundTag();
                s.putString("id", "minecraft:scale");
                s.putDouble("base", schaal);
                attr.add(s);
                tag.put("attributes", attr);
            }
            for (String l : looks) {
                look(tag, l);
            }
        };
    }

    /** A look of {@link SpCast} in an actor's NBT (merge_sp2: sp_cast's own NBT form, "Looks", as d2_heen / d2_finale). */
    static void look(CompoundTag tag, String look) {
        SpCast.nbt(tag, look);
    }

    /** An item display (always facing the camera, fully lit) of this item at this scale. */
    static Consumer<CompoundTag> ding(String item, float schaal) {
        return tag -> {
            CompoundTag stapel = new CompoundTag();
            stapel.putString("id", item);
            stapel.putInt("count", 1);
            tag.put("item", stapel);
            tag.putString("item_display", "ground");
            tag.putString("billboard", "vertical");
            CompoundTag vorm = new CompoundTag();
            vorm.put("translation", floats(0f, 0f, 0f));
            vorm.put("left_rotation", floats(0f, 0f, 0f, 1f));
            vorm.put("scale", floats(schaal, schaal, schaal));
            vorm.put("right_rotation", floats(0f, 0f, 0f, 1f));
            tag.put("transformation", vorm);
            CompoundTag licht = new CompoundTag();
            licht.putInt("sky", 15);
            licht.putInt("block", 15);
            tag.put("brightness", licht);
        };
    }

    static ListTag floats(float... waarden) {
        ListTag lijst = new ListTag();
        for (float f : waarden) {
            lijst.add(FloatTag.valueOf(f));
        }
        return lijst;
    }

    private static final Supplier<EntityType<?>> ITEM = () -> EntityType.ITEM_DISPLAY;

    // =====================================================================================================================
    // 5. wink 9: Aang-guh and Appa-guh fly over the loopgraaf (anchor knipoog_appa_sponsguh, template (87, 22, 45)). 10 s.
    // =====================================================================================================================

    private static Cutscene knipoogAppa() {
        Sc s = new Sc("knipoog_appa", 200, "knipoog_appa_sponsguh", new BlockPos(87, 22, 45));
        s.b.verbergEcht(12).speler(s.t(85.5, 22, 44.5), -90f);
        s.acteur("sponsguh", E2SponsguhFeature.SPONSGUH, 87.5, 22, 45.5, -90f, wezen())
                .acteur("patguh", E2SponsguhFeature.PATGUH, 87.5, 22, 43.5, -90f, wezen())
                .acteur("appa", E2AppaFeature.APPA_GUH, 82.5, 36, 46.5, -90f, null);
        s.b.npc("aang", Kind.AANG_GUH, s.t(82.5, 39.2, 46.5), -90f);
        s.b.zwart(0, 8);
        // the two at the rim from behind, low; then the camera looks up with them
        s.cam(0, 80.5, 23.6, 40.5, 87.5, 23.2, 44.5).cam(55, 80.5, 23.4, 40.5, 108.5, 36.0, 47.5)
                .geluid(8, E2AppaFeature.FLUITJE, 1.0f, 1.0f)
                .loop("appa", 8, 150, 136.5, 36, 49.5).loop("aang", 8, 150, 136.5, 39.2, 49.5)
                .kijk("sponsguh", 14, 96, 36, 47).kijk("patguh", 14, 96, 36, 47).kijk(Cutscene.SPELER, 14, 96, 36, 47)
                .kijk("sponsguh", 60, 112, 36, 48).kijk("patguh", 60, 112, 36, 48)
                .anim("aang", 40, "zwaai").zeg(42, "aang", "vliegen", 46)
                .geluid(64, E2AppaFeature.FLUITJE, 0.8f, 1.25f);
        // they look at each other... and walk anyway
        s.knip(98, 90.6, 23.35, 44.5, 87.5, 23.0, 44.5)
                .kijk("sponsguh", 100, 87.5, 23, 43.5).kijk("patguh", 100, 87.5, 23, 45.5)
                .zeg(112, "patguh", "vleugels", 32).anim("patguh", 112, "praat")
                .zeg(148, "sponsguh", "lopen", 36).anim("sponsguh", 148, "praat")
                .loop("sponsguh", 186, 200, 87.5, 22, 48.0).loop("patguh", 187, 200, 87.8, 22, 46.0);
        return s.b.registreer();
    }

    // =====================================================================================================================
    // 5. snor: Mindy-guh's moustaches of knabbelkoordraad, "WIJ ZIJN MANNEN!" (anchor snor_plek, (87, 22, 53)). 26 s.
    // =====================================================================================================================

    private static Cutscene snor() {
        Sc s = new Sc("snor", 520, "snor_plek", new BlockPos(87, 22, 53));
        s.b.verbergEcht(14).speler(s.t(87.6, 22, 48.6), 0f);
        s.acteur("sponsguh", E2SponsguhFeature.SPONSGUH, 86.7, 22, 49.8, 0f, wezen())
                .acteur("patguh", E2SponsguhFeature.PATGUH, 88.4, 22, 50.0, 0f, wezen())
                .acteurWeg("sponsguh_snor", E2SponsguhFeature.SPONSGUH, 86.7, 49.8, 0f, wezen(SpCast.SNOR))
                .acteurWeg("patguh_snor", E2SponsguhFeature.PATGUH, 88.4, 50.0, 0f, wezen(SpCast.SNOR))
                .acteurWeg("mindy", E2SponsguhFeature.MINDY_GUH, 86.5, 52.5, 180f, wezen());
        s.b.zwart(0, 10);
        s.cam(0, 90.6, 23.9, 51.6, 86.6, 23.2, 50.8).cam(60, 90.3, 23.7, 51.4, 86.6, 23.2, 51.4);
        // Mindy-guh appears in a sparkle
        s.in("mindy", 10, 86.5, 22, 52.5).kijk("mindy", 12, 87.5, 23, 49.8)
                .deeltjes(10, ParticleTypes.END_ROD, 86.5, 23.0, 52.5, 30, 0.6).deeltjes(10, ParticleTypes.HAPPY_VILLAGER, 86.5, 23.0, 52.5, 16, 0.7)
                .geluid(10, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f)
                .anim("sponsguh", 12, "schrik").anim("patguh", 12, "schrik");
        s.knip(16, 87.9, 23.55, 50.4, 86.5, 23.45, 52.5).zeg(18, "mindy", "hallo", 60).anim("mindy", 18, "zwaai");
        s.knip(84, 85.1, 23.25, 51.6, 86.7, 22.95, 49.8).zeg(86, "sponsguh", "monsters", 62).anim("sponsguh", 86, "praat");
        s.knip(152, 90.0, 23.5, 51.2, 88.4, 23.25, 50.0).zeg(154, "patguh", "kinderen", 40).anim("patguh", 154, "praat");
        // the moustaches: a twist of knabbelkoordraad under every nose
        s.knip(198, 90.2, 24.3, 53.6, 87.4, 23.1, 50.4).zeg(200, "mindy", "snor", 50).anim("mindy", 205, "geef")
                .deeltjes(226, ParticleTypes.WAX_ON, 86.7, 23.0, 50.0, 12, 0.2).deeltjes(226, ParticleTypes.WAX_ON, 88.4, 23.3, 50.2, 12, 0.2)
                .geluid(226, () -> SoundEvents.WOOL_PLACE, 1.0f, 1.3f).geluid(232, () -> SoundEvents.WOOL_PLACE, 1.0f, 1.1f)
                .uit("sponsguh", 228, 86.7, 49.8).in("sponsguh_snor", 228, 86.7, 22, 49.8)
                .uit("patguh", 228, 88.4, 50.0).in("patguh_snor", 228, 88.4, 22, 50.0)
                .kijk("sponsguh_snor", 230, 86.5, 23.4, 52.5).kijk("patguh_snor", 230, 86.5, 23.4, 52.5);
        // close: two very manly faces
        s.knip(258, 87.6, 23.2, 51.8, 87.55, 23.05, 49.9)
                .kijk("sponsguh_snor", 260, 88.4, 23.2, 50.0).kijk("patguh_snor", 260, 86.7, 23.0, 49.8)
                .zeg(262, "sponsguh_snor", "harig", 40).anim("sponsguh_snor", 262, "praat")
                .zeg(306, "patguh_snor", "wij", 30).anim("patguh_snor", 306, "praat")
                .zeg(340, "beiden", "mannen", 36).anim("sponsguh_snor", 340, "juich").anim("patguh_snor", 340, "juich")
                .geluid(340, SpAudio.geluid("stinger.snor"), 1.0f, 1.0f);
        s.b.schud(340, 0.4f, 12);
        s.knip(380, 89.4, 23.6, 47.4, 87.4, 23.0, 50.2)
                .kijk("sponsguh_snor", 380, 87.6, 23.5, 48.6).zeg(382, "sponsguh_snor", "herkent", 50).anim("sponsguh_snor", 382, "lach");
        s.knip(436, 87.9, 23.55, 50.4, 86.5, 23.45, 52.5).zeg(438, "mindy", "ga", 55).anim("mindy", 438, "zwaai")
                .deeltjes(496, ParticleTypes.END_ROD, 86.5, 23.0, 52.5, 30, 0.6).geluid(496, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.5f)
                .uit("mindy", 496, 86.5, 52.5);
        return s.b.registreer();
    }

    // =====================================================================================================================
    // 6. monsters: at the bottom of the loopgraaf they roar... and then they sing (anchor: the foot of pad_omlaag, (102, 4, 63))
    // =====================================================================================================================

    /** The monsters of the scene: spot (template), yaw, soort. They stand round the trio's end spot (105.7, 57.6). */
    private static final double[][] MONSTER = {
            {103.6, 58.4, -110, 0}, {104.5, 52.5, 10, 1}, {109.5, 54.5, 45, 2}, {110.5, 59.5, 100, 0}, {101.0, 60.5, -110, 1}, {107.5, 62.5, 170, 2}};

    private static Cutscene monsters() {
        Sc s = new Sc("monsters", 600, MONSTERS_ANKER, new BlockPos(102, 4, 63));
        s.b.verbergEcht(56).speler(s.t(102.6, 4, 65.0), 200f);
        s.acteur("sponsguh", E2SponsguhFeature.SPONSGUH, 101.8, 4, 64.2, 200f, wezen(SpCast.SNOR))
                .acteur("patguh", E2SponsguhFeature.PATGUH, 103.4, 4, 64.6, 200f, wezen(SpCast.SNOR));
        for (int i = 0; i < MONSTER.length; i++) {
            double[] m = MONSTER[i];
            double dx = m[0] - 105.7, dz = m[1] - 57.6, d = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
            // they wait three blocks further out in the dark (only their glowing eyes), and creep in
            s.acteur("monster_" + (i + 1), E2SponsguhFeature.LOOPGRAAFMONSTER, m[0] + dx / d * 3, 4, m[1] + dz / d * 3, (float) m[2],
                    wezen((int) m[3], false, 1.0));
        }
        s.b.zwart(0, 12);
        // 1. into the dark, hand in hand
        s.cam(0, 100.0, 6.4, 66.8, 104.5, 5.0, 59.5).cam(64, 101.0, 6.0, 64.0, 105.0, 5.0, 57.5)
                .geluid(4, () -> SoundEvents.AMBIENT_CAVE.value(), 0.7f, 0.8f)
                .loop("sponsguh", 4, 64, 104.8, 4, 57.6).loop(Cutscene.SPELER, 4, 64, 105.7, 4, 57.0).loop("patguh", 4, 64, 106.7, 4, 58.0)
                .zeg(14, "sponsguh", "donker", 50).anim("sponsguh", 14, "praat");
        for (int i = 0; i < MONSTER.length; i++) {
            s.loop("monster_" + (i + 1), 40, 100, MONSTER[i][0], 4, MONSTER[i][1]);
        }
        s.knip(66, 103.2, 5.4, 60.6, 104.0, 5.1, 57.8).zeg(70, "patguh", "hand", 36).anim("patguh", 70, "praat")
                .kijk("sponsguh", 106, 103.6, 5.8, 58.4).anim("sponsguh", 108, "schrik");
        // 2. ROAR
        s.knip(112, 105.7, 10.5, 64.5, 105.7, 4.5, 56.5)
                .geluid(112, E2SponsguhFeature.MONSTER_BRUL, 1.0f, 0.8f).geluid(118, E2SponsguhFeature.MONSTER_BRUL, 1.0f, 0.6f)
                .anim("sponsguh", 114, "schrik").anim("patguh", 114, "schrik").anim(Cutscene.SPELER, 114, "spring");
        s.b.schud(112, 1.0f, 24);
        for (int i = 0; i < MONSTER.length; i++) {
            s.anim("monster_" + (i + 1), 112 + i * 2, "brul");
        }
        s.knip(138, 107.2, 5.1, 55.0, 105.0, 4.9, 57.6).zeg(140, "sponsguh", "mannen", 60).anim("sponsguh", 140, "praat");
        // 3. they lean in... one of them hums a note... and they all sing
        s.knip(205, 105.7, 8.0, 62.5, 105.7, 5.0, 56.5);
        for (int i = 0; i < MONSTER.length; i++) {
            double[] m = MONSTER[i];
            s.loop("monster_" + (i + 1), 205, 230, m[0] + (105.7 - m[0]) * 0.25, 4, m[1] + (57.6 - m[1]) * 0.25);
        }
        // (merge_sp2: the recording opens with one monster humming a phrase; no extra MONSTER_ZING hum on top of it)
        s.anim("monster_1", 244, "zing");
        int lied = SpAudio.lied(s.b, "monsters", 244);
        int t = 244;
        for (int i = 0; i < MONSTER.length; i++) {
            s.anim("monster_" + (i + 1), t + 4 + i * 3, "zing");
            for (int w = t + 60; w < t + lied; w += 80) {
                s.anim("monster_" + (i + 1), w + i * 5, "wieg");
            }
        }
        s.anim("sponsguh", t + lied / 2, "juich").anim("patguh", t + lied / 2 + 10, "juich")
                .cam(t, 111.0, 6.0, 61.0, 105.7, 5.0, 56.5).cam(t + lied / 3, 111.5, 6.5, 51.5, 105.7, 5.0, 57.0)
                .cam(t + 2 * lied / 3, 100.0, 6.5, 51.0, 105.7, 5.0, 57.0).cam(t + lied, 99.5, 7.0, 61.5, 105.7, 5.0, 57.0);
        for (int n = t; n < t + lied; n += 40) {
            s.deeltjes(n, ParticleTypes.NOTE, 105.7, 7.0, 57.5, 6, 3.0);
        }
        // 4. sniff, and on
        int e = t + lied + 8;
        s.knip(e - 2, 107.2, 5.1, 55.0, 105.0, 4.9, 57.6).zeg(e, "patguh", "mooi", 36).anim("patguh", e, "praat")
                .zeg(e + 40, "sponsguh", "dank", 55).anim("sponsguh", e + 40, "juich");
        for (int i = 0; i < MONSTER.length; i++) {
            s.anim("monster_" + (i + 1), e + 44 + i * 2, "juich");
        }
        s.b.duur(e + 110);
        return s.b.registreer();
    }

    // =====================================================================================================================
    // 7. dennis_snor: THE joke (CONTRACT_E2_C 5.3). Dennis-Mika looks for "een vierkant en een ster", sees two neat
    //    gentlemen with a moustache, rides on politely... a sneeze, a moustache falls, a long silence, a slow U-turn.
    //    Anchor loopgraaf_rand_oost (129, 22, 50); the road down the hill steps one lower at x 134, 138, 142, 146, 150.
    // =====================================================================================================================

    private static Cutscene dennisSnor() {
        Sc s = new Sc("dennis_snor", 770, "loopgraaf_rand_oost", new BlockPos(129, 22, 50));
        s.b.verbergEcht(18).speler(s.t(132.5, 22, 50.5), -90f);
        s.acteur("sponsguh", E2SponsguhFeature.SPONSGUH, 131.6, 22, 49.0, -90f, wezen(SpCast.SNOR))
                .acteur("patguh", E2SponsguhFeature.PATGUH, 131.6, 22, 52.0, -90f, wezen(SpCast.SNOR))
                .acteurWeg("patguh_kaal", E2SponsguhFeature.PATGUH, 131.6, 52.0, -90f, wezen())
                .acteur("dennis", E2SponsguhFeature.DENNIS_MIKA, 153.5, 17, 45.5, 90f, wezen(-1, true, 1.0))
                .acteurWeg("snor", ITEM, 132.1, 52.0, 0f, ding("guhs:einde2_sp_d2_weg_snor", 0.45f));
        s.b.zwart(0, 10);
        // 1. up! and nobody recognised us
        s.cam(0, 137.0, 23.4, 54.0, 132.0, 23.0, 50.5).cam(50, 136.5, 23.2, 53.5, 132.0, 23.0, 50.5)
                .zeg(12, "sponsguh", "gehaald", 42).anim("sponsguh", 12, "juich");
        // 2. an engine down the road
        s.knip(56, 130.0, 24.0, 47.5, 146.0, 19.5, 48.0)
                .geluid(56, E2SponsguhFeature.MOBIEL_MOTOR, 1.0f, 0.6f).geluid(80, E2SponsguhFeature.MOBIEL_MOTOR, 1.0f, 0.7f)
                .anim("dennis", 56, "rij")
                .loop("dennis", 58, 76, 147.5, 18, 47.5).loop("dennis", 76, 92, 141.5, 20, 49.5).loop("dennis", 92, 106, 137.5, 21, 50.5)
                .anim("dennis", 100, "remmen").geluid(102, () -> SoundEvents.GRINDSTONE_USE, 0.8f, 0.7f)
                .kijk("dennis", 108, 131.8, 23, 50.5);
        s.knip(110, 134.6, 23.3, 48.4, 131.8, 23.0, 50.3).zeg(112, "sponsguh", "oei", 50).anim("sponsguh", 112, "praat");
        // 3. he studies them: the square one... the star... the one in between
        s.knip(165, 134.2, 22.9, 52.4, 137.5, 22.8, 50.5)
                .kijk("dennis", 170, 131.6, 23, 49.0).kijk("dennis", 192, 131.6, 23.3, 52.0).kijk("dennis", 214, 132.5, 23.5, 50.5)
                .geluid(172, E2SponsguhFeature.MIKA_BROM, 0.8f, 0.8f)
                .zeg(222, "dennis", "zoek", 56).anim("dennis", 222, "praat");
        s.knip(280, 134.0, 22.7, 48.0, 131.6, 22.9, 49.2).zeg(282, "sponsguh", "keurig", 46).anim("sponsguh", 282, "praat");
        s.knip(330, 133.8, 23.0, 53.3, 131.6, 23.2, 52.0).zeg(332, "patguh", "keurig2", 28).anim("patguh", 332, "trots");
        s.knip(362, 134.2, 22.9, 52.4, 137.5, 22.8, 50.5).zeg(364, "dennis", "dag", 52).anim("dennis", 364, "praat");
        // 4. he rides on, politely, towards the loopgraaf (the square and the star must be back there)
        s.knip(418, 136.5, 23.6, 55.0, 131.0, 22.8, 51.5).geluid(418, E2SponsguhFeature.MOBIEL_MOTOR, 0.9f, 0.9f)
                .loop("dennis", 418, 440, 134.5, 22, 53.0).loop("dennis", 440, 462, 130.0, 22, 53.0).loop("dennis", 462, 476, 128.6, 22, 51.2)
                .kijk("sponsguh", 430, 133, 23, 53).kijk("patguh", 430, 133, 23, 53.5).kijk("sponsguh", 456, 129, 23, 53).kijk("patguh", 456, 129, 23, 53);
        // 5. ha... ha... HATSJOE: Patguh's moustache flies off
        s.knip(478, 134.2, 23.3, 53.4, 131.7, 23.3, 52.0).kijk("patguh", 478, 134.0, 23.4, 53.0)
                .zeg(480, "patguh", "nies", 26).anim("patguh", 480, "nies")
                .uit("patguh", 506, 131.6, 52.0).in("patguh_kaal", 506, 131.6, 22, 52.0).kijk("patguh_kaal", 507, 134.0, 23.4, 53.0)
                .in("snor", 506, 132.1, 23.15, 52.0).loop("snor", 507, 520, 132.9, 22.05, 52.4)
                .deeltjes(506, ParticleTypes.CLOUD, 132.1, 23.2, 52.0, 8, 0.15).geluid(506, () -> SoundEvents.WOOL_BREAK, 1.0f, 1.4f)
                .geluid(519, () -> SoundEvents.WOOL_FALL, 0.8f, 1.2f);
        // 6. the long silence: the moustache on the road... Patguh's bare face... Dennis' back. He brakes.
        s.knip(522, 133.6, 22.9, 53.2, 132.9, 22.1, 52.4)
                .knip(548, 134.2, 23.3, 53.4, 131.7, 23.3, 52.0).anim("patguh_kaal", 550, "schrik")
                .kijk("sponsguh", 552, 131.6, 23.2, 52.0).kijk("patguh_kaal", 552, 131.6, 23.0, 49.0)
                .knip(572, 130.8, 23.6, 55.2, 128.7, 23.0, 51.3).anim("dennis", 574, "remmen").geluid(574, () -> SoundEvents.GRINDSTONE_USE, 1.0f, 0.55f)
                // ...and turns his bike round. Very. Slowly.
                .kijk("dennis", 586, 128.6, 23, 54.5).kijk("dennis", 600, 130.0, 23, 53.5).kijk("dennis", 614, 132.0, 23, 51.0)
                .knip(612, 130.6, 23.2, 50.9, 128.7, 23.1, 51.3)
                .zeg(622, "dennis", "vierkant", 30).zeg(656, "dennis", "ster", 30).anim("dennis", 656, "praat")
                .geluid(654, SpAudio.geluid("stinger.mislukt"), 0.8f, 0.7f);
        // 7. RUN!
        s.knip(690, 135.0, 23.0, 50.6, 131.8, 23.0, 50.6).zeg(692, "sponsguh", "rennen", 24).anim("sponsguh", 692, "rennen")
                .anim("patguh_kaal", 692, "schrik")
                .cam(720, 130.0, 25.0, 47.0, 138.0, 21.5, 50.5)
                .loop("sponsguh", 720, 760, 140.5, 20, 49.8).loop(Cutscene.SPELER, 722, 760, 141.5, 20, 50.5)
                .loop("patguh_kaal", 724, 762, 140.6, 20, 51.4)
                .geluid(724, E2SponsguhFeature.MOBIEL_MOTOR, 1.0f, 1.25f).anim("dennis", 724, "rij").kijk("dennis", 724, 140.0, 22, 50.5);
        s.b.zwart(748, 770);
        return s.b.registreer();
    }

    // =====================================================================================================================
    // 7. laars: through the gate, Dennis right behind... a giant lead diving boot comes down (anchor: dennis_route under the
    //    town gate, (166, 14, 42)). Dennis is fine: flat as a pancake, and walks off wobbling. 17 s.
    // =====================================================================================================================

    private static Cutscene laars() {
        Sc s = new Sc("laars", 336, LAARS_ANKER, new BlockPos(166, 14, 42));
        s.b.verbergEcht(16).speler(s.t(158.5, 15, 42.5), -90f);
        s.acteur("sponsguh", E2SponsguhFeature.SPONSGUH, 157.6, 15, 41.2, -90f, wezen(SpCast.SNOR))
                .acteur("patguh_kaal", E2SponsguhFeature.PATGUH, 157.8, 15, 43.8, -90f, wezen())
                .acteur("dennis", E2SponsguhFeature.DENNIS_MIKA, 149.5, 17, 45.0, -90f, wezen(-1, true, 1.0))
                .acteurWeg("dennis_plat", E2SponsguhFeature.DENNIS_MIKA, 162.5, 42.5, 90f, wezen())
                .acteur("laars", ITEM, 162.5, 44, 42.5, 0f, ding("minecraft:netherite_boots", 6.5f));
        s.cam(0, 173.0, 16.8, 46.0, 161.0, 15.0, 42.5).cam(44, 172.0, 16.6, 45.6, 162.5, 15.0, 42.5)
                .loop("sponsguh", 0, 40, 168.4, 14, 41.0).loop(Cutscene.SPELER, 0, 40, 169.5, 14, 42.5).loop("patguh_kaal", 0, 42, 168.6, 14, 44.0)
                .anim("sponsguh", 0, "rennen").anim("dennis", 0, "rij").geluid(0, E2SponsguhFeature.MOBIEL_MOTOR, 1.0f, 1.2f)
                .loop("dennis", 0, 48, 162.5, 14, 42.5)
                // a shadow... a whistle from above...
                .geluid(36, () -> SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 0.5f).deeltjes(44, ParticleTypes.SQUID_INK, 162.5, 14.1, 42.5, 24, 0.9)
                .loop("laars", 42, 56, 162.5, 16.6, 42.5)
                // BOING
                .geluid(56, () -> SoundEvents.ANVIL_LAND, 1.0f, 0.6f).deeltjes(56, ParticleTypes.CLOUD, 162.5, 14.4, 42.5, 40, 1.2)
                .uit("dennis", 56, 162.5, 42.5).in("dennis_plat", 56, 162.5, 14, 42.5).anim("dennis_plat", 57, "plat")
                .anim("sponsguh", 58, "schrik").anim("patguh_kaal", 58, "schrik")
                .kijk("sponsguh", 60, 162.5, 15, 42.5).kijk("patguh_kaal", 60, 162.5, 15, 42.5).kijk(Cutscene.SPELER, 60, 162.5, 15, 42.5);
        s.b.schud(56, 1.3f, 14);
        // the boot goes up again: Dennis, very flat
        s.knip(70, 166.0, 15.6, 44.2, 162.5, 14.6, 42.5).loop("laars", 76, 104, 162.5, 44, 42.5)
                .zeg(110, "dennis_plat", "au", 56).anim("dennis_plat", 110, "plat")
                .loop("dennis_plat", 170, 260, 152.5, 17, 45.5);
        // a very big foot... a diving boot... and a funny feeling about that souvenir shop
        s.knip(168, 171.0, 15.6, 44.6, 168.5, 15.0, 42.3).zeg(172, "patguh_kaal", "voet", 40).anim("patguh_kaal", 172, "praat")
                .zeg(216, "sponsguh", "laars", 60).anim("sponsguh", 216, "praat").kijk("sponsguh", 216, 184, 20, 31)
                .cam(216, 171.0, 15.6, 44.6, 168.5, 15.0, 42.3).cam(276, 170.5, 16.0, 45.5, 184.0, 22.0, 30.0)
                .kijk("patguh_kaal", 276, 184, 20, 31).kijk(Cutscene.SPELER, 276, 184, 20, 31)
                .zeg(280, "patguh_kaal", "sleutelhanger", 44).anim("patguh_kaal", 280, "juich");
        return s.b.registreer();
    }

    // =====================================================================================================================
    // 8. cycloop: the shop full of dried souvenirs, the crown in its case, the Cycloop grabs all three, the drying lamp
    //    (anchor droogtafel, (184, 14, 16); the front room z 21-30, the drying room z 14-19 behind an opening x 180-189). 35 s.
    // =====================================================================================================================

    private static Cutscene cycloop() {
        Sc s = new Sc("cycloop", 700, "droogtafel", new BlockPos(184, 14, 16));
        s.b.verbergEcht(16).speler(s.t(184.5, 14, 29.5), 180f);
        s.acteur("sponsguh", E2SponsguhFeature.SPONSGUH, 183.4, 14, 29.2, 180f, wezen(SpCast.SNOR))
                .acteur("patguh_kaal", E2SponsguhFeature.PATGUH, 185.6, 14, 29.4, 180f, wezen())
                .acteurWeg("sponsguh_droog", E2SponsguhFeature.SPONSGUH, 183.5, 16.5, 90f, wezen(-1, false, 0.85, SpCast.SNOR, SpCast.DROOG))
                .acteurWeg("patguh_droog", E2SponsguhFeature.PATGUH, 185.5, 16.5, -90f, wezen(-1, false, 0.85, SpCast.DROOG))
                .acteurWeg("cycloop", E2SponsguhFeature.CYCLOOP_MIKA, 178.5, 17.5, 0f, wezen())
                .acteur("kroon", ITEM, 184.5, 15.25, 25.5, 0f, ding("minecraft:golden_helmet", 0.55f))
                .acteurWeg("traan", ITEM, 185.6, 16.45, 0f, ding("minecraft:ghast_tear", 0.3f));
        s.b.zwart(0, 14);
        // 1. in through the door: shelves full of dried things
        s.cam(0, 188.5, 17.6, 29.5, 182.0, 15.0, 23.5).cam(18, 188.2, 17.4, 29.2, 181.5, 15.0, 23.0)
                .loop("sponsguh", 0, 60, 183.3, 14, 27.3).loop(Cutscene.SPELER, 0, 60, 184.5, 14, 27.6).loop("patguh_kaal", 0, 60, 185.7, 14, 27.5)
                .knip(20, 179.0, 15.8, 24.0, 175.5, 15.3, 21.5).cam(86, 178.6, 15.6, 23.4, 176.5, 15.2, 21.4)
                .zeg(22, "sponsguh", "souvenirs", 64).anim("sponsguh", 22, "praat");
        // 2. the crown!
        s.knip(88, 184.5, 15.9, 27.2, 184.5, 15.3, 25.5).deeltjes(90, ParticleTypes.END_ROD, 184.5, 15.6, 25.5, 8, 0.2)
                .geluid(90, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.0f)
                .zeg(92, "patguh_kaal", "kroon", 44).anim("patguh_kaal", 92, "juich");
        s.knip(138, 186.6, 15.4, 28.8, 184.0, 15.2, 27.4).zeg(140, "sponsguh", "plankmika", 60).anim("sponsguh", 140, "praat");
        // 3. heavy steps... behind the crown, the Cycloop
        s.geluid(202, () -> SoundEvents.IRON_GOLEM_STEP, 1.0f, 0.6f).geluid(216, () -> SoundEvents.IRON_GOLEM_STEP, 1.0f, 0.6f)
                .geluid(230, () -> SoundEvents.IRON_GOLEM_STEP, 1.0f, 0.55f)
                .in("cycloop", 200, 178.5, 14, 17.5).loop("cycloop", 201, 240, 182.4, 14, 23.0).kijk("cycloop", 241, 184.5, 15, 27.5)
                .anim("sponsguh", 232, "schrik").anim("patguh_kaal", 232, "schrik").anim(Cutscene.SPELER, 232, "spring")
                .kijk("sponsguh", 232, 182.4, 16, 23).kijk("patguh_kaal", 232, 182.4, 16, 23).kijk(Cutscene.SPELER, 232, 182.4, 16, 23)
                .knip(232, 184.9, 14.5, 26.4, 182.4, 16.2, 23.0)
                .zeg(244, "cycloop", "nieuw", 58).anim("cycloop", 244, "praat");
        s.b.schud(202, 0.4f, 6).schud(216, 0.4f, 6).schud(230, 0.5f, 6);
        // 4. he grabs all three and carries them over the crown to the drying table
        s.anim("cycloop", 304, "grijp").zeg(306, "sponsguh", "help", 44).anim("sponsguh", 306, "schrik")
                .knip(312, 188.0, 17.2, 21.5, 184.0, 15.2, 19.5).cam(352, 187.6, 16.8, 20.0, 184.0, 15.2, 17.0)
                .loop("sponsguh", 312, 332, 183.5, 16.2, 22.0).loop("sponsguh", 332, 352, 183.5, 15, 16.5)
                .loop("patguh_kaal", 312, 332, 185.5, 16.2, 22.0).loop("patguh_kaal", 332, 352, 185.5, 15, 16.5)
                .loop(Cutscene.SPELER, 312, 332, 184.5, 15.2, 22.0).loop(Cutscene.SPELER, 332, 352, 184.5, 14, 18.5)
                .loop("cycloop", 312, 352, 182.0, 14, 18.0).kijk("cycloop", 353, 184.5, 15, 16.5);
        // 5. click: the drying lamp
        s.geluid(358, () -> SoundEvents.LEVER_CLICK, 1.0f, 0.8f).deeltjes(360, ParticleTypes.FLAME, 184.5, 17.8, 16.5, 10, 0.2)
                .deeltjes(360, ParticleTypes.WHITE_ASH, 184.5, 16.2, 16.5, 30, 0.6).geluid(362, () -> SoundEvents.FURNACE_FIRE_CRACKLE, 1.0f, 1.0f)
                .knip(364, 180.5, 15.9, 19.2, 182.0, 15.9, 18.0).zeg(366, "cycloop", "lamp", 60).anim("cycloop", 366, "lach")
                .loop("cycloop", 430, 480, 192.5, 14, 27.5);
        // 6. crinkly: the dried ones take over
        s.knip(438, 185.9, 16.4, 18.3, 184.5, 15.3, 16.5)
                .uit("sponsguh", 440, 183.5, 16.5).in("sponsguh_droog", 440, 183.5, 15, 16.5)
                .uit("patguh_kaal", 440, 185.5, 16.5).in("patguh_droog", 440, 185.5, 15, 16.5)
                .deeltjes(440, ParticleTypes.WHITE_ASH, 184.5, 15.5, 16.5, 30, 0.7).geluid(440, () -> SoundEvents.FIRE_EXTINGUISH, 0.5f, 1.6f)
                .zeg(444, "sponsguh_droog", "knisperig", 56)
                .zeg(504, "patguh_droog", "huilen", 46);
        // 7. a tear! and the socket on the wall
        s.knip(550, 186.6, 15.8, 17.6, 185.6, 15.3, 16.5).deeltjes(552, ParticleTypes.FALLING_WATER, 185.6, 15.5, 16.4, 3, 0.02)
                .in("traan", 556, 185.6, 15.35, 16.45).geluid(556, () -> SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 1.0f, 1.2f)
                .zeg(562, "sponsguh_droog", "traan", 56)
                .cam(562, 186.6, 15.8, 17.6, 185.6, 15.3, 16.5).cam(616, 186.4, 16.2, 18.0, 187.5, 15.4, 14.3)
                .zeg(622, "patguh_droog", "stopcontact", 66);
        return s.b.registreer();
    }

    // =====================================================================================================================
    // 8. sproeier: the tear rolls into the socket, sparks, the lamp pops, the sprinklers, everybody comes back to life, the
    //    dried souvenir guhs too; the Cycloop is soaked (and gets a towel); the crown. Anchor droogtafel. 35 s.
    // =====================================================================================================================

    /** The six sprinklers of the shop (template, the block under the ceiling). */
    static final int[][] SPROEIERS = {{184, 18, 18}, {176, 18, 16}, {192, 18, 16}, {176, 18, 26}, {192, 18, 26}, {185, 18, 23}};
    /** The souvenir guhs that come alive: spot (template) and soort. */
    private static final double[][] SOUVENIR_GUHS = {{178.5, 22.6, 0}, {190.5, 22.6, 1}, {179.5, 26.5, 2}, {189.5, 26.5, 3}, {181.5, 28.6, 4},
            {187.5, 28.6, 5}};

    private static Cutscene sproeier() {
        Sc s = new Sc("sproeier", 700, "droogtafel", new BlockPos(184, 14, 16));
        s.b.verbergEcht(16).speler(s.t(184.5, 14, 18.6), 180f);
        s.acteur("sponsguh_droog", E2SponsguhFeature.SPONSGUH, 183.5, 15, 16.5, 90f, wezen(-1, false, 0.85, SpCast.SNOR, SpCast.DROOG))
                .acteur("patguh_droog", E2SponsguhFeature.PATGUH, 185.5, 15, 16.5, -90f, wezen(-1, false, 0.85, SpCast.DROOG))
                .acteurWeg("sponsguh", E2SponsguhFeature.SPONSGUH, 183.5, 16.5, 0f, wezen(SpCast.SNOR))
                .acteurWeg("patguh_kaal", E2SponsguhFeature.PATGUH, 185.5, 16.5, 0f, wezen())
                .acteur("cycloop", E2SponsguhFeature.CYCLOOP_MIKA, 192.5, 14, 27.5, 90f, wezen())
                .acteur("kroon", ITEM, 184.5, 15.25, 25.5, 0f, ding("minecraft:golden_helmet", 0.55f))
                .acteur("traan", ITEM, 187.5, 14.15, 14.9, 0f, ding("minecraft:ghast_tear", 0.3f));
        for (int i = 0; i < SOUVENIR_GUHS.length; i++) {
            double[] g = SOUVENIR_GUHS[i];
            s.acteurWeg("guh" + (i + 1), E2SponsguhFeature.BIKINIBODEM_GUH, g[0], g[1], 180f, wezen((int) g[2], false, 1.0));
        }
        // 1. plip: the tear in the socket. Sparks run up the cord to the lamp... POP
        s.cam(0, 187.5, 15.4, 16.6, 187.5, 15.0, 14.3).loop("traan", 0, 16, 187.5, 15.35, 14.4)
                .geluid(16, () -> SoundEvents.FIRE_EXTINGUISH, 0.8f, 1.4f).uit("traan", 18, 187.5, 14.5)
                .deeltjes(20, ParticleTypes.ELECTRIC_SPARK, 187.5, 15.5, 14.3, 20, 0.2).geluid(20, () -> SoundEvents.REDSTONE_TORCH_BURNOUT, 1.0f, 1.0f)
                .deeltjes(26, ParticleTypes.ELECTRIC_SPARK, 187.5, 16.5, 14.5, 10, 0.15).deeltjes(30, ParticleTypes.ELECTRIC_SPARK, 187.5, 17.5, 14.5, 10, 0.15)
                .deeltjes(34, ParticleTypes.ELECTRIC_SPARK, 187.5, 18.4, 15.5, 10, 0.15).deeltjes(38, ParticleTypes.ELECTRIC_SPARK, 186.5, 18.4, 16.5, 10, 0.15)
                .deeltjes(42, ParticleTypes.ELECTRIC_SPARK, 185.5, 18.4, 16.5, 10, 0.15)
                .cam(20, 187.5, 15.4, 16.6, 187.5, 15.0, 14.3).cam(44, 186.5, 16.0, 19.0, 184.5, 18.0, 16.5)
                .geluid(46, () -> SoundEvents.GLASS_BREAK, 1.0f, 1.2f).deeltjes(46, ParticleTypes.LARGE_SMOKE, 184.5, 17.6, 16.5, 20, 0.3)
                .deeltjes(46, ParticleTypes.ELECTRIC_SPARK, 184.5, 17.6, 16.5, 30, 0.4);
        s.b.schud(46, 0.7f, 10);
        // 2. the sprinklers: rain in the shop
        for (int n = 56; n <= 640; n += 8) {
            for (int[] sp : SPROEIERS) {
                s.deeltjes(n, ParticleTypes.FALLING_WATER, sp[0] + 0.5, 17.7, sp[2] + 0.5, 18, 0.7);
                if (n % 24 == 8) {
                    s.deeltjes(n, ParticleTypes.SPLASH, sp[0] + 0.5, 14.2, sp[2] + 0.5, 10, 0.8);
                }
            }
        }
        s.geluid(56, () -> SoundEvents.WEATHER_RAIN, 1.0f, 1.2f).geluid(150, () -> SoundEvents.WEATHER_RAIN, 1.0f, 1.2f)
                .geluid(250, () -> SoundEvents.WEATHER_RAIN, 1.0f, 1.2f).geluid(350, () -> SoundEvents.WEATHER_RAIN, 1.0f, 1.2f)
                .geluid(450, () -> SoundEvents.WEATHER_RAIN, 1.0f, 1.2f).geluid(550, () -> SoundEvents.WEATHER_RAIN, 1.0f, 1.2f);
        // 3. water! they puff up and jump down
        s.knip(60, 185.9, 16.4, 18.3, 184.5, 15.3, 16.5)
                .uit("sponsguh_droog", 66, 183.5, 16.5).in("sponsguh", 66, 183.5, 15, 16.5)
                .uit("patguh_droog", 66, 185.5, 16.5).in("patguh_kaal", 66, 185.5, 15, 16.5)
                .deeltjes(66, ParticleTypes.SPLASH, 184.5, 15.6, 16.5, 40, 0.8).geluid(66, () -> SoundEvents.GENERIC_SPLASH, 1.0f, 1.3f)
                .loop("sponsguh", 70, 84, 182.6, 14, 18.0).loop("patguh_kaal", 72, 86, 186.4, 14, 18.0)
                .kijk("sponsguh", 86, 184.5, 15.5, 18.6).kijk("patguh_kaal", 86, 184.5, 15.5, 18.6)
                .zeg(72, "sponsguh", "water", 40).anim("sponsguh", 72, "juich")
                .zeg(116, "patguh_kaal", "ster", 40).anim("patguh_kaal", 116, "juich");
        // 4. the dried souvenirs: guhs! alive!
        s.knip(158, 184.5, 17.4, 19.4, 184.5, 15.0, 25.5);
        for (int i = 0; i < SOUVENIR_GUHS.length; i++) {
            double[] g = SOUVENIR_GUHS[i];
            s.in("guh" + (i + 1), 160 + i * 3, g[0], 14, g[1]).deeltjes(160 + i * 3, ParticleTypes.SPLASH, g[0], 14.6, g[1], 16, 0.4)
                    .deeltjes(160 + i * 3, ParticleTypes.BUBBLE_POP, g[0], 15.0, g[1], 10, 0.4)
                    .kijk("guh" + (i + 1), 162 + i * 3, 184.5, 15, 18.6).anim("guh" + (i + 1), 166 + i * 3, "juich").anim("guh" + (i + 1), 210 + i * 4, "dans");
        }
        s.geluid(160, () -> SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 1.0f).geluid(166, () -> SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 1.3f)
                .zeg(170, "guh1", "leven", 50);
        // 5. the Cycloop runs in and sits down in a puddle
        s.loop("cycloop", 222, 250, 186.0, 14, 21.5).anim("cycloop", 250, "schrik").geluid(250, () -> SoundEvents.GENERIC_SPLASH, 1.0f, 0.8f)
                .loop("cycloop", 250, 262, 186.3, 13.6, 21.4).anim("cycloop", 252, "nat")
                .knip(248, 188.6, 15.2, 19.4, 186.2, 15.4, 21.5).zeg(264, "cycloop", "nat", 56)
                .kijk("sponsguh", 250, 186.2, 15.5, 21.5).kijk("patguh_kaal", 250, 186.2, 15.5, 21.5)
                .knip(322, 185.4, 15.4, 20.0, 186.4, 15.0, 18.0).zeg(324, "patguh_kaal", "ik", 40).anim("patguh_kaal", 324, "praat");
        // 6. a souvenir guh brings him a towel. Guhs are like that.
        s.loop("guh1", 368, 396, 185.0, 14, 21.0).kijk("guh1", 397, 186.3, 15, 21.4)
                .knip(396, 188.6, 15.2, 19.4, 186.0, 15.0, 21.3).zeg(400, "cycloop", "handdoek", 40).anim("cycloop", 400, "praat");
        // 7. the crown of King Neptuguh
        s.loop("sponsguh", 444, 456, 181.8, 14, 23.0).loop("sponsguh", 456, 470, 181.8, 14, 26.8).loop("sponsguh", 470, 486, 184.5, 14, 26.7)
                .loop("patguh_kaal", 444, 466, 186.6, 14, 22.5).loop("patguh_kaal", 466, 490, 186.4, 14, 27.6)
                .loop(Cutscene.SPELER, 450, 470, 186.3, 14, 22.5).loop(Cutscene.SPELER, 470, 490, 186.3, 14, 27.8)
                .kijk("sponsguh", 487, 184.5, 15.5, 25.5).loop("kroon", 488, 500, 184.5, 15.75, 26.7)
                .kijk("sponsguh", 501, 184.5, 15.5, 29.4)
                .knip(486, 184.5, 15.3, 29.4, 184.5, 15.3, 26.5).deeltjes(500, ParticleTypes.END_ROD, 184.5, 16.0, 26.7, 16, 0.3)
                .geluid(500, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.0f).geluid(506, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.3f)
                .zeg(504, "sponsguh", "kroon", 60).anim("sponsguh", 504, "juich")
                .zeg(566, "patguh_kaal", "thuis", 54).anim("patguh_kaal", 566, "praat")
                .zeg(622, "sponsguh", "pier", 54).anim("sponsguh", 622, "praat")
                .cam(676, 184.5, 17.6, 30.0, 184.5, 15.0, 24.0);
        for (int i = 0; i < SOUVENIR_GUHS.length; i++) {
            s.anim("guh" + (i + 1), 676 + i * 2, "juich");
        }
        return s.b.registreer();
    }
}
