package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 phase B, slice geest_spel: its five cutscenes (CONTRACT_E2_B 5). Each is anchored at a marker of the build and
 * written in coordinates relative to it; where the build has a camera marker ({@code wan_camera}) or the spots of the other
 * actors, the scene reads them from the sidecar when it is registered, else it uses a sensible shot of its own (and the
 * camera steps out of any wall: {@link Cutscene.Builder#cameraOntwijkt}).
 * <ul>
 *   <li>{@value #REIGER} (big, 57 s): the six tiles glow, the stone lotus opens, the Oude Reigerguh wakes and tells the history
 *       of the order; the archive opens;</li>
 *   <li>{@value #WAN} (big, 58 s): the eighth island, Wan-guh, the first: the origin;</li>
 *   <li>{@value #HEI_BAI} (21 s): the acorn, Hei Bai-guh calms down, the wood blooms;</li>
 *   <li>{@value #KLOK} (13 s): the Geestenklok, then silence, and every spirit looks at you;</li>
 *   <li>{@value #MAAN} (14 s): the moon night: Tui comes down through the mist.</li>
 * </ul>
 * Texts: tools/features/einde2_geest_spel_tekst.py ({@code scene.guhs.einde2_geest_spel_*}).
 */
public final class GeestScenes {
    public static final String REIGER = "einde2_geest_spel_reiger", WAN = "einde2_geest_spel_wan", HEI_BAI = "einde2_geest_spel_hei_bai_bloei",
            KLOK = "einde2_geest_spel_geestenklok", MAAN = "einde2_geest_spel_maannacht";
    /** The anchor marker of each scene. */
    public static final String ANKER_REIGER = "stenen_lotus", ANKER_WAN = "wan_onthulling", ANKER_HEI_BAI = "eikel_plek", ANKER_KLOK = "geestenklok",
            ANKER_MAAN = "plek_hemelvijver";
    public static final List<String> ALLE = List.of(REIGER, WAN, HEI_BAI, KLOK, MAAN);

    private static Cutscene reiger, wan, heiBai, klok, maan;

    private GeestScenes() {
    }

    // =====================================================================================================================
    // playing
    // =====================================================================================================================

    /** The scene of this id (null: unknown). */
    @Nullable
    public static Cutscene van(String id) {
        return Cutscene.van(id);
    }

    /** The anchor marker of a scene. */
    public static String anker(String id) {
        return switch (id) {
            case REIGER -> ANKER_REIGER;
            case WAN -> ANKER_WAN;
            case HEI_BAI -> ANKER_HEI_BAI;
            case KLOK -> ANKER_KLOK;
            default -> ANKER_MAAN;
        };
    }

    /**
     * Plays a scene for this player at its anchor; when the build has no anchor (yet) or the player is watching something
     * already, {@code daarna} runs at once instead (the story never gets stuck on a missing marker). True: it plays.
     */
    public static boolean speel(ServerPlayer p, String id, @Nullable Consumer<ServerPlayer> daarna) {
        Cutscene s = Cutscene.van(id);
        BlockPos anker = GeestPlek.pos(anker(id));
        if (s != null && anker != null && Cutscenes.speel(p, s, anker, Rotation.NONE, daarna)) {
            return true;
        }
        if (daarna != null) {
            daarna.accept(p);
        }
        return false;
    }

    // =====================================================================================================================
    // the scripts
    // =====================================================================================================================

    static void register() {
        reiger = reiger();
        wan = wan();
        heiBai = heiBai();
        klok = klok();
        maan = maan();
    }

    /** The marker {@code naar} seen from the marker {@code van} (template = world offsets); null when one is missing. */
    @Nullable
    static Vec3 rel(String van, String naar) {
        BlockPos a = GeestPlek.pos(van), b = GeestPlek.pos(naar);
        return a == null || b == null ? null : new Vec3(b.getX() - a.getX() + 0.5, b.getY() - a.getY(), b.getZ() - a.getZ() + 0.5);
    }

    /** The horizontal unit vector from the anchor towards that point (south when there is none). */
    private static Vec3 richting(@Nullable Vec3 naar) {
        if (naar == null || naar.horizontalDistanceSqr() < 0.01) {
            return new Vec3(0, 0, 1);
        }
        Vec3 h = new Vec3(naar.x, 0, naar.z);
        return h.normalize();
    }

    private static float yawNaar(Vec3 van, Vec3 naar) {
        return (float) (Math.toDegrees(Math.atan2(naar.z - van.z, naar.x - van.x)) - 90.0);
    }

    static Supplier<EntityType<?>> type(String id) {
        return () -> {
            Identifier rl = Identifier.parse(id);
            return BuiltInRegistries.ENTITY_TYPE.containsKey(rl) ? BuiltInRegistries.ENTITY_TYPE.getValue(rl) : EntityType.ARMOR_STAND;
        };
    }

    private static Consumer<CompoundTag> nbt(int soort, @Nullable String houding) {
        return t -> {
            if (soort >= 0) {
                t.putInt("Soort", soort);
            }
            t.putBoolean("Stil", true);
            t.putBoolean("NoAI", true);
            if (houding != null) {
                t.putString("Houding", houding);
            }
        };
    }

    /** The midpoint of the six pedestals seen from the stone lotus (where the player stands when it opens). */
    @Nullable
    private static Vec3 voetstukken() {
        double x = 0, y = 0, z = 0;
        int n = 0;
        for (int i = 1; i <= 6; i++) {
            Vec3 v = rel(ANKER_REIGER, "tegel_voetstuk_" + i);
            if (v != null) {
                x += v.x;
                y += v.y;
                z += v.z;
                n++;
            }
        }
        return n == 0 ? null : new Vec3(x / n, y / n, z / n);
    }

    private static Cutscene reiger() {
        Vec3 l = new Vec3(0.5, 0, 0.5);
        Vec3 voet = voetstukken();
        Vec3 r = richting(voet);
        Vec3 opzij = new Vec3(-r.z, 0, r.x);
        Vec3 speler = voet != null ? new Vec3(voet.x, 0, voet.z) : l.add(r.scale(6));
        Vec3 vogel = rel(ANKER_REIGER, "reiger");
        if (vogel == null) {
            vogel = l.add(r.scale(-3));
        }
        Vec3 kop = vogel.add(0, 4.5, 0);
        Cutscene.Builder b = Cutscene.maak(REIGER).duur(1140).bij(GeestLijnen.WITTE_LOTUS).cameraOntwijkt().verbergEcht(10)
                .speler(speler, yawNaar(speler, l))
                .acteur("reiger", type("guhs:oude_reigerguh"), vogel, yawNaar(vogel, speler), nbt(-1, "slaap"))
                // the six tiles glow, one by one
                .camera(0, l.add(r.scale(14)).add(opzij.scale(4)).add(0, 7, 0), l.add(0, 1, 0))
                .camera(130, l.add(r.scale(9)).add(opzij.scale(-3)).add(0, 4, 0), l.add(0, 1, 0))
                .zeg(60, "", "r1", 70)
                // the lotus opens
                .cameraKnip(140, l.add(r.scale(5)).add(0, 2.5, 0), l.add(0, 1.2, 0))
                .schud(150, 0.6f, 30)
                .zeg(200, "", "r2", 60)
                // the reiger wakes
                .camera(240, l.add(r.scale(8)).add(opzij.scale(5)).add(0, 3, 0), kop)
                .animatie("reiger", 240, "ontwaak")
                .zeg(300, "reiger", "r3", 80)
                .cameraKnipVolgt(380, speler.add(opzij.scale(2.5)).add(r.scale(2)).add(0, 2, 0), Cutscene.SPELER)
                .zeg(390, "reiger", "r4", 80)
                .cameraKnip(470, l.add(r.scale(11)).add(opzij.scale(-6)).add(0, 6, 0), kop)
                .camera(700, l.add(r.scale(11)).add(opzij.scale(6)).add(0, 6, 0), kop)
                .animatie("reiger", 470, "spreek")
                .zeg(480, "reiger", "r5", 90)
                .zeg(580, "reiger", "r6", 90)
                .zeg(680, "reiger", "r7", 90)
                .cameraKnip(780, kop.add(r.scale(5)).add(0, -0.5, 0), kop)
                .zeg(790, "reiger", "r8", 90)
                .zeg(890, "reiger", "r9", 100)
                .cameraKnip(1000, l.add(r.scale(10)).add(0, 9, 0), l.add(r.scale(-6)))
                .zeg(1010, "", "r10", 80)
                .kijk(Cutscene.SPELER, 240, kop);
        for (int i = 1; i <= 6; i++) {
            Vec3 v = rel(ANKER_REIGER, "tegel_voetstuk_" + i);
            if (v != null) {
                b.deeltjes(10 + i * 18, ParticleTypes.END_ROD, v.add(0, 1.2, 0), 14, 0.25);
            }
        }
        b.deeltjes(150, ParticleTypes.END_ROD, l.add(0, 1.5, 0), 80, 1.6).deeltjes(170, ParticleTypes.GLOW, l.add(0, 1, 0), 60, 2.0)
                .deeltjes(250, ParticleTypes.END_ROD, kop, 50, 1.4).deeltjes(1000, ParticleTypes.END_ROD, l.add(r.scale(-6)).add(0, 2, 0), 70, 1.6)
                .geluid(150, E2Audio.geluid("stinger.opgelost"), 0.9f, 1.0f).geluid(245, E2Audio.geluid("geest.reiger"), 1.0f, 1.0f)
                .geluid(1000, E2Audio.geluid("stinger.tegel"), 0.9f, 0.8f);
        return b.registreer();
    }

    private static Cutscene wan() {
        Vec3 w = new Vec3(0.5, 0, 0.5);
        Vec3 cam = rel(ANKER_WAN, "wan_camera");
        Vec3 r = richting(cam);
        Vec3 opzij = new Vec3(-r.z, 0, r.x);
        Vec3 speler = w.add(r.scale(4));
        Vec3 camHoog = cam != null ? cam : w.add(r.scale(9)).add(0, 3, 0);
        Vec3 kop = w.add(0, 1.4, 0);
        return Cutscene.maak(WAN).duur(1160).bij(GeestLijnen.WAN).cameraOntwijkt().verbergEcht(8)
                .speler(speler, yawNaar(speler, w))
                .acteur("wan", type("guhs:wan_guh"), w, yawNaar(w, speler), nbt(-1, null))
                .camera(0, w.add(r.scale(18)).add(opzij.scale(6)).add(0, 8, 0), w.add(0, 1, 0))
                .camera(160, camHoog, kop)
                .zeg(40, "", "w1", 80)
                .zeg(170, "wan", "w2", 70)
                .cameraKnip(250, w.add(r.scale(2.2)).add(opzij.scale(0.8)).add(0, 1.5, 0), kop)
                .zeg(260, "", "w3", 90)
                .zeg(360, "wan", "w4", 90)
                .cameraKnipVolgt(450, speler.add(opzij.scale(-2.5)).add(r.scale(1.5)).add(0, 1.8, 0), Cutscene.SPELER)
                .zeg(460, "wan", "w5", 90)
                .cameraKnip(550, w.add(r.scale(6)).add(opzij.scale(4)).add(0, 2.5, 0), kop)
                .zeg(560, "wan", "w6", 80)
                .zeg(640, "wan", "w7", 60)
                .camera(780, w.add(r.scale(6)).add(opzij.scale(-4)).add(0, 3.5, 0), kop)
                .zeg(710, "wan", "w8", 100)
                .cameraKnip(820, w.add(r.scale(3)).add(0, 1.6, 0), kop)
                .zeg(830, "wan", "w9", 90)
                .zeg(920, "wan", "w10", 100)
                .animatie("wan", 1030, "verdwijn")
                .cameraKnip(1020, w.add(r.scale(8)).add(0, 4, 0), w.add(0, 1, 0))
                .zeg(1050, "", "w11", 90)
                .deeltjes(1040, ParticleTypes.END_ROD, kop, 120, 0.8).deeltjes(1060, ParticleTypes.GLOW, w.add(0, 0.3, 0), 40, 0.5)
                .deeltjes(560, ParticleTypes.GLOW, w.add(0, 2.5, 0), 30, 0.6)
                .geluid(0, E2Audio.geluid("geest.maannacht"), 0.7f, 0.8f).geluid(1035, E2Audio.geluid("stinger.opgelost"), 0.8f, 1.2f)
                .kijk(Cutscene.SPELER, 0, kop)
                .registreer();
    }

    private static Cutscene heiBai() {
        Vec3 e = new Vec3(0.5, 0, 0.5);
        Vec3 bai = rel(ANKER_HEI_BAI, "hei_bai");
        if (bai == null) {
            bai = e.add(-4, 0, 3);
        }
        Vec3 r = richting(new Vec3(-bai.x, 0, -bai.z));
        Vec3 opzij = new Vec3(-r.z, 0, r.x);
        Vec3 speler = e.add(r.scale(1.8));
        Vec3 weg = bai.add(0, -12, 0);
        Vec3 kop = bai.add(0, 3.5, 0);
        return Cutscene.maak(HEI_BAI).duur(420).bij(GeestLijnen.HEI_BAI).cameraOntwijkt().verbergEcht(12)
                .speler(speler, yawNaar(speler, e))
                .acteur("reus", type("guhs:hei_bai_guh"), bai, yawNaar(bai, speler), nbt(1, null))
                .acteur("bai", type("guhs:hei_bai_guh"), weg, yawNaar(bai, speler), nbt(0, null))
                .camera(0, e.add(r.scale(7)).add(opzij.scale(3)).add(0, 3, 0), e.add(0, 0.5, 0))
                .animatie(Cutscene.SPELER, 10, "buk").animatie(Cutscene.SPELER, 50, "sta")
                .zeg(30, "", "h1", 70)
                .camera(110, e.add(r.scale(9)).add(opzij.scale(-4)).add(0, 4, 0), kop)
                .animatie("reus", 120, "kalm")
                .zeg(130, "", "h2", 70)
                .zwart(200, 240)
                .loop("reus", 205, 210, weg.add(3, 0, 0))
                .loop("bai", 212, 218, bai)
                .cameraKnip(225, e.add(r.scale(4)).add(opzij.scale(2)).add(0, 1.6, 0), bai.add(0, 0.6, 0))
                .zeg(250, "", "h3", 70)
                .camera(400, e.add(r.scale(12)).add(opzij.scale(6)).add(0, 7, 0), e)
                .zeg(330, "bai", "h4", 50)
                .deeltjes(30, ParticleTypes.HAPPY_VILLAGER, e.add(0, 0.3, 0), 10, 0.3)
                .deeltjes(245, ParticleTypes.HAPPY_VILLAGER, e.add(0, 0.5, 0), 60, 5.0)
                .deeltjes(260, ParticleTypes.CHERRY_LEAVES, e.add(0, 4, 0), 80, 7.0)
                .deeltjes(300, ParticleTypes.CHERRY_LEAVES, e.add(0, 5, 0), 80, 9.0)
                .geluid(240, E2Audio.geluid("stinger.opgelost"), 0.8f, 1.1f)
                .registreer();
    }

    private static Cutscene klok() {
        Vec3 k = new Vec3(0.5, 0, 0.5);
        Vec3 r = new Vec3(0, 0, 1);
        Vec3 speler = k.add(r.scale(3));
        Cutscene.Builder b = Cutscene.maak(KLOK).duur(260).bij(GeestLijnen.VERSCHIJNINGEN).cameraOntwijkt()
                .speler(speler, 180f)
                .camera(0, k.add(5, 2, 6), k.add(0, 2, 0))
                .geluid(5, E2Audio.geluid("geest.klok"), 1.2f, 0.7f)
                .schud(8, 0.4f, 25)
                .zeg(30, "", "k1", 50)
                .camera(110, k.add(-7, 6, 9), speler.add(0, 1, 0))
                .zeg(130, "", "k2", 70)
                .geluid(120, E2Audio.geluid("geest.koh_stilte"), 0.7f, 1.0f);
        // the spirits round the bell turn to look at you
        String[][] geesten = {{"guhs:lantaarngeest", "0"}, {"guhs:libelkonijnguh", "-1"}, {"guhs:lantaarngeest", "2"}, {"guhs:libelkonijnguh", "-1"}};
        for (int i = 0; i < geesten.length; i++) {
            double hoek = Math.PI * 2 * i / geesten.length + 0.4;
            Vec3 at = speler.add(Math.cos(hoek) * 5, 1.0, Math.sin(hoek) * 5);
            b.acteur("geest" + i, type(geesten[i][0]), at, (float) Math.toDegrees(hoek), nbt(Integer.parseInt(geesten[i][1]), null))
                    .kijk("geest" + i, 100 + i * 6, speler.add(0, 1.4, 0));
        }
        return b.registreer();
    }

    private static Cutscene maan() {
        Vec3 v = new Vec3(0.5, 0, 0.5);
        Vec3 vijver = rel(ANKER_MAAN, "koi_tui");
        Vec3 r = richting(vijver);
        Vec3 doel = vijver != null ? vijver : v.add(r.scale(16)).add(0, 6, 0);
        Vec3 boven = doel.add(0, 34, 0);
        Vec3 speler = v.add(r.scale(1.5));
        return Cutscene.maak(MAAN).duur(280).bij(GeestLijnen.VERSCHIJNINGEN).cameraOntwijkt()
                .speler(speler, yawNaar(speler, doel))
                .acteur("tui", type("guhs:geest_koi"), boven, 0f, nbt(0, null))
                .camera(0, v.add(r.scale(-6)).add(0, 3, 0), doel.add(0, 14, 0))
                .loop("tui", 10, 150, doel.add(0, 2, 0))
                .geluid(0, E2Audio.geluid("geest.maannacht"), 1.0f, 1.0f)
                .zeg(40, "", "m1", 70)
                .camera(160, speler.add(r.scale(-3)).add(0, 1.4, 0), doel.add(0, 2, 0))
                .zeg(170, "", "m2", 80)
                .deeltjes(150, ParticleTypes.END_ROD, doel, 80, 4.0).deeltjes(200, ParticleTypes.GLOW, doel.add(0, -2, 0), 60, 6.0)
                .kijk(Cutscene.SPELER, 0, boven)
                .registreer();
    }

    /** (tests, report) the running time of a scene in seconds (without the clicks). */
    public static double seconden(String id) {
        Cutscene s = Cutscene.van(id);
        return s == null ? 0 : s.duur() / 20.0;
    }
}
