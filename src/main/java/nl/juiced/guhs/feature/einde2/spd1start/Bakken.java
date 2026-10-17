package nl.juiced.guhs.feature.einde2.spd1start;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.sponsguh.Besteltafel;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 "Sponsguh", slice sp_d1_start, step {@value SpVerhaal#R_BAKKEN} (CONTRACT_E2_C 7.3): the grill game in the kitchen of
 * De Krokante Knabbel, per player. Standing behind the grill ({@code grill_speler}) starts it (the first time after Sponsguh's
 * lesson, scene {@code bakken_uitleg}). Then, for that player only:
 * <ul>
 *   <li>five labelled crates round the grill (this slice's markers {@code d1s_krat_1..5}: a small barrel, the part floating over
 *       it, its name), each with an invisible box to click (right or left);</li>
 *   <li>a customer rings the bell of an order table, and the order ticket floats over the grill: the parts from the bottom up,
 *       the next one marked; the burger grows visibly on the grill with each right part;</li>
 *   <li>a wrong part: the burger flops (a soft "plof", never a fail), that order starts over; nothing else is lost;</li>
 *   <li>the counter "Bestelling n/5" in the action bar; five orders = a big title and a sound, the time as a score only (the best
 *       one is kept), then the scene {@code bakken_klaar} and the line goes on (step 5, sp_d1_stad).</li>
 * </ul>
 * No clock: nothing hurries you. Walking away (more than {@value #WEG} blocks), dying or leaving stops the game; the orders done
 * so far stay done (the line's counter {@value E2SpD1Start#T_BESTELD}) and it starts again behind the grill. All state that
 * must outlive a break is in the line; the entities live only while the game runs ({@link D1Tijdelijk}).
 */
final class Bakken {
    /** The size of a crate (a barrel scaled down), the height of the part over it, of its name, of the click box. */
    static final float KRAT_SCHAAL = 0.6f, KNOP_HOOG = 1.7f;
    static final double ITEM_HOOGTE = 0.95, NAAM_HOOGTE = 1.5;
    /**
     * Where the crates stand, relative to grill_speler (the markers d1s_krat_1..5 of einde2_sp_d1_start_plekken are exactly
     * these; the scene bakken_uitleg shows them there): bun, burger, cheese, sea lettuce, pickle.
     */
    static final List<Vec3> KRATTEN_REL = List.of(new Vec3(-2, 0, 0), new Vec3(-2, 0, 2), new Vec3(0, 0, 3), new Vec3(2, 0, 2), new Vec3(4, 0, 1));
    /** How close (blocks, flat) to grill_speler starts the game; how far away stops it. */
    static final double START = 1.8, WEG = 10;
    /** Ticks between an order done and the next bell; the shortest time between two clicks that count. */
    static final int PAUZE = 30, KLIK_RUST = 4;
    /** Layers of the burger: height of the first one over the grill's top, and between two. */
    static final double LAAG_0 = 0.08, LAAG = 0.075;

    private record Krat(GrillSpel.Deel deel, Display.BlockDisplay krat, Display.ItemDisplay item, Display.TextDisplay naam, Interaction knop) {
        void ruimOp() {
            D1Tijdelijk.weg(krat);
            D1Tijdelijk.weg(item);
            D1Tijdelijk.weg(naam);
            D1Tijdelijk.weg(knop);
        }
    }

    /** One player's game. */
    static final class Spel {
        final ServerLevel level;
        final BlockPos grillSpeler;
        final Vec3 grillTop;
        final GrillSpel regels;
        final List<Krat> kratten = new ArrayList<>();
        final List<Display.ItemDisplay> lagen = new ArrayList<>();
        @Nullable
        Display.TextDisplay bon;
        final long start;
        long laatsteKlik, volgendeOp;
        boolean klaar;

        Spel(ServerLevel level, BlockPos grillSpeler, GrillSpel regels, long start) {
            this.level = level;
            this.grillSpeler = grillSpeler;
            this.grillTop = Vec3.atBottomCenterOf(grillSpeler.north()).add(0, 1.0, 0);
            this.regels = regels;
            this.start = start;
        }

        void ruimOp() {
            kratten.forEach(Krat::ruimOp);
            kratten.clear();
            lagen.forEach(D1Tijdelijk::weg);
            lagen.clear();
            D1Tijdelijk.weg(bon);
            bon = null;
        }
    }

    private static final Map<UUID, Spel> SPELLEN = new ConcurrentHashMap<>();

    private Bakken() {
    }

    private static long nu(ServerPlayer p) {
        return p.level().getServer().getTickCount();
    }

    /** The game of this player (null: none running). */
    @Nullable
    static Spel spel(ServerPlayer p) {
        return SPELLEN.get(p.getUUID());
    }

    /** Is the game on for this player (the variant text 4_b)? */
    static boolean bezig(ServerPlayer p) {
        Spel s = SPELLEN.get(p.getUUID());
        return s != null && !s.klaar;
    }

    // =====================================================================================================================
    // the step tick
    // =====================================================================================================================

    /** Every few ticks while step 4 is on and the player is in Bikinibodem. */
    static void tick(ServerPlayer p) {
        ServerLevel g = p.level();
        Spel s = SPELLEN.get(p.getUUID());
        if (s != null) {
            if (s.klaar) {
                return;
            }
            if (!p.isAlive() || p.level() != s.level || flat(p.position(), Vec3.atBottomCenterOf(s.grillSpeler)) > WEG) {
                stop(p);
                p.sendOverlayMessage(E2SpD1Start.tekst("bakken.gestopt").withStyle(ChatFormatting.GOLD));
                return;
            }
            long nu = nu(p);
            if (s.volgendeOp > 0 && nu >= s.volgendeOp) {
                s.volgendeOp = 0;
                nieuweBestelling(p, s);
            }
            if (nu % 20 < 5) {
                balk(p, s);
            }
            return;
        }
        BlockPos gs = E2SpD1Start.plek(g, "grill_speler");
        if (gs == null || Cutscenes.bezig(p)) {
            return;
        }
        Vec3 plek = Vec3.atBottomCenterOf(gs);
        double d = flat(p.position(), plek);
        if (d <= START && Math.abs(p.getY() - plek.y) < 2) {
            if (!Cutscenes.gezien(p, D1Scenes.BAKKEN_UITLEG.id())) {
                if (!Cutscenes.speel(p, D1Scenes.BAKKEN_UITLEG, gs, Rotation.NONE, Bakken::start)) {
                    start(p);
                }
            } else {
                start(p);
            }
        } else if (d < 12 && nu(p) % 100 < 5) {
            p.sendOverlayMessage(E2SpD1Start.tekst("bakken.naar_grill").withStyle(ChatFormatting.GOLD));
        }
    }

    private static double flat(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    // =====================================================================================================================
    // start, orders, clicks
    // =====================================================================================================================

    /** Starts the game at the grill of Bikinibodem (the daarna of the lesson, the tick, the dev command). */
    static void start(ServerPlayer p) {
        if (E2SpD1Start.stap(p) != SpVerhaal.R_BAKKEN || SPELLEN.containsKey(p.getUUID())) {
            return;
        }
        ServerLevel g = E2SpD1Start.guheinde(p.level().getServer());
        BlockPos gs = E2SpD1Start.plek(g, "grill_speler");
        List<BlockPos> kratten = E2SpD1Start.kratten(g);
        if (gs == null || kratten.isEmpty() || p.level() != g) {
            return;
        }
        start(p, g, gs, kratten);
    }

    /** Starts the game at this grill spot with these crate spots (tests use their own floor). */
    @Nullable
    static Spel start(ServerPlayer p, ServerLevel level, BlockPos grillSpeler, List<BlockPos> kratPlekken) {
        stop(p);
        Spel s = new Spel(level, grillSpeler, new GrillSpel(E2SpD1Start.lijn().teller(p, E2SpD1Start.T_BESTELD)), nu(p));
        GrillSpel.Deel[] delen = GrillSpel.Deel.values();
        for (int i = 0; i < delen.length && i < kratPlekken.size(); i++) {
            Vec3 voet = Vec3.atBottomCenterOf(kratPlekken.get(i));
            CompoundTag props = new CompoundTag();
            props.putString("facing", "up");
            props.putString("open", "false");
            Display.BlockDisplay krat = D1Tijdelijk.blok(level, voet, "minecraft:barrel", props, KRAT_SCHAAL);
            Display.ItemDisplay item = D1Tijdelijk.item(level, voet.add(0, ITEM_HOOGTE, 0), delen[i].item, 0.5f, false, "vertical");
            Display.TextDisplay naam = D1Tijdelijk.tekst(level, voet.add(0, NAAM_HOOGTE, 0), deelNaam(delen[i]).withStyle(ChatFormatting.YELLOW),
                    0.55f, 0x90241A12, 120);
            Interaction knop = D1Tijdelijk.knop(level, voet, 0.9f, KNOP_HOOG);
            if (krat == null || item == null || naam == null || knop == null) {
                s.ruimOp();
                return null;
            }
            s.kratten.add(new Krat(delen[i], D1Tijdelijk.zet(level, krat, p), D1Tijdelijk.zet(level, item, p), D1Tijdelijk.zet(level, naam, p),
                    D1Tijdelijk.zet(level, knop, p)));
            level.sendParticles(p, ParticleTypes.POOF, true, false, voet.x, voet.y + 0.5, voet.z, 6, 0.2, 0.2, 0.2, 0.01);
        }
        SPELLEN.put(p.getUUID(), s);
        E2SpD1Start.titel(p, E2SpD1Start.tekst("bakken.titel").withStyle(ChatFormatting.GOLD),
                E2SpD1Start.tekst("bakken.regel").withStyle(ChatFormatting.YELLOW), 50);
        SpAudio.stinger(p, "deuntje.bakken");
        if (s.regels.klaar() > 0) {
            p.sendSystemMessage(E2SpD1Start.tekst("bakken.verder", s.regels.klaar(), GrillSpel.AANTAL).withStyle(ChatFormatting.GRAY));
        }
        nieuweBestelling(p, s);
        return s;
    }

    /** The next order: a bell at an order table, the ticket. */
    private static void nieuweBestelling(ServerPlayer p, Spel s) {
        int n = s.regels.klaar();
        BlockPos tafel = E2SpD1Start.plek(s.level, "besteltafel_" + (n % 3 + 1));
        if (tafel != null) {
            Besteltafel.bel(s.level, tafel, p);
        } else {
            s.level.playSound(null, s.grillSpeler, E2SponsguhFeature.BESTELBEL.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        }
        p.sendOverlayMessage(E2SpD1Start.tekst("bakken.bel", n + 1, GrillSpel.AANTAL).withStyle(ChatFormatting.GOLD));
        bon(p, s);
        balk(p, s);
    }

    /** A click on an entity of the game: the crate's part goes on the grill. True when it was one of this player's crates. */
    static boolean klik(ServerPlayer p, Entity e) {
        Spel s = SPELLEN.get(p.getUUID());
        if (s == null) {
            return false;
        }
        for (Krat k : s.kratten) {
            if (e == k.knop || e == k.krat || e == k.item || e == k.naam) {
                leg(p, k.deel);
                return true;
            }
        }
        return false;
    }

    /** Puts a part on this player's grill (a click; tests). */
    static GrillSpel.Uitslag leg(ServerPlayer p, GrillSpel.Deel deel) {
        Spel s = SPELLEN.get(p.getUUID());
        long nu = nu(p);
        if (s == null || s.klaar || nu - s.laatsteKlik < KLIK_RUST) {
            return GrillSpel.Uitslag.NIETS;
        }
        if (s.volgendeOp > 0) {
            p.sendOverlayMessage(E2SpD1Start.tekst("bakken.wacht").withStyle(ChatFormatting.GRAY));
            return GrillSpel.Uitslag.NIETS;
        }
        s.laatsteKlik = nu;
        int laag = s.regels.gelegd();
        GrillSpel.Uitslag u = s.regels.leg(deel);
        switch (u) {
            case GOED -> {
                laag(p, s, deel, laag);
                s.level.playSound(null, s.grillSpeler, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.0f + laag * 0.12f);
                s.level.playSound(null, s.grillSpeler, E2SponsguhFeature.GRILL_SIS.get(), SoundSource.BLOCKS, 0.4f, 1.2f);
                bon(p, s);
            }
            case FOUT -> {
                plof(p, s);
                bon(p, s);
            }
            case BESTELLING_KLAAR -> {
                laag(p, s, deel, laag);
                E2SpD1Start.lijn().teller(p, E2SpD1Start.T_BESTELD, s.regels.klaar());
                s.level.playSound(null, s.grillSpeler, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
                SpAudio.stinger(p, "stinger.opgelost");
                E2SpD1Start.titel(p, Component.empty(), E2SpD1Start.tekst("bakken.bestelling_klaar", s.regels.klaar(), GrillSpel.AANTAL)
                        .withStyle(ChatFormatting.GREEN), 25);
                s.volgendeOp = nu + PAUZE;
                StapHint.later(p, PAUZE - 6, q -> wisLagen(q, true));
                D1Tijdelijk.weg(s.bon);                     // (no ticket until the next bell)
                s.bon = null;
                balk(p, s);
            }
            case ALLES_KLAAR -> {
                laag(p, s, deel, laag);
                E2SpD1Start.lijn().teller(p, E2SpD1Start.T_BESTELD, s.regels.klaar());
                klaar(p, s);
            }
            default -> {
            }
        }
        return u;
    }

    /** (dev) the order on the grill done by itself: its parts, right ones, one after the other. False: no game on. */
    static boolean maakBestelling(ServerPlayer p) {
        Spel s = SPELLEN.get(p.getUUID());
        if (s == null || s.klaar) {
            return false;
        }
        s.volgendeOp = 0;
        List<GrillSpel.Deel> b = s.regels.bestelling();
        if (b == null) {
            return false;
        }
        for (int i = s.regels.gelegd(); i < b.size(); i++) {
            s.laatsteKlik = -KLIK_RUST;
            leg(p, b.get(i));
        }
        return true;
    }

    /** The burger flops: the layers go, a soft "plof", the order starts over (no penalty). */
    private static void plof(ServerPlayer p, Spel s) {
        for (Display.ItemDisplay l : s.lagen) {
            Vec3 at = l.position();
            s.level.sendParticles(p, ParticleTypes.POOF, true, false, at.x, at.y, at.z, 3, 0.1, 0.05, 0.1, 0.01);
        }
        wisLagen(p, false);
        s.level.sendParticles(p, ParticleTypes.SMOKE, true, false, s.grillTop.x, s.grillTop.y + 0.2, s.grillTop.z, 8, 0.2, 0.1, 0.2, 0.01);
        s.level.playSound(null, s.grillSpeler, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.8f, 0.8f);
        SpAudio.stinger(p, "stinger.mislukt");
        p.sendOverlayMessage(E2SpD1Start.tekst("bakken.plof").withStyle(ChatFormatting.RED));
        if (E2SpD1Start.lijn().eenmalig(p, "sp_d1_start_plof_tip")) {
            E2SpD1Start.zie(p, "bakken.plof_tip");
        }
    }

    /** A layer of the burger on the grill (flat, for this player). */
    private static void laag(ServerPlayer p, Spel s, GrillSpel.Deel deel, int hoogte) {
        Vec3 at = s.grillTop.add(0, LAAG_0 + hoogte * LAAG, 0);
        Display.ItemDisplay d = D1Tijdelijk.item(s.level, at, deel.item, 0.55f, true, "fixed");
        if (d != null) {
            s.lagen.add(D1Tijdelijk.zet(s.level, d, p));
        }
        s.level.sendParticles(p, ParticleTypes.SMOKE, true, false, at.x, at.y + 0.1, at.z, 2, 0.1, 0.02, 0.1, 0.01);
    }

    /** Takes the burger off the grill (done: it goes to the customer with a puff). */
    private static void wisLagen(ServerPlayer p, boolean geserveerd) {
        Spel s = SPELLEN.get(p.getUUID());
        if (s == null) {
            return;
        }
        if (geserveerd && !s.lagen.isEmpty()) {
            s.level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, false, s.grillTop.x, s.grillTop.y + 0.3, s.grillTop.z, 6, 0.2, 0.15, 0.2, 0.0);
        }
        s.lagen.forEach(D1Tijdelijk::weg);
        s.lagen.clear();
    }

    /** The order ticket over the grill: who ordered, and the parts from the bottom up (done, next, still to come). */
    private static void bon(ServerPlayer p, Spel s) {
        D1Tijdelijk.weg(s.bon);
        s.bon = null;
        List<GrillSpel.Deel> b = s.regels.bestelling();
        if (b == null) {
            return;
        }
        int n = s.regels.klaar();
        MutableComponent t = E2SpD1Start.tekst("bakken.bon.kop", n + 1, GrillSpel.AANTAL).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("\n")).append(E2SpD1Start.tekst("bakken.klant." + n).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("\n")).append(E2SpD1Start.tekst("bakken.bon.onder").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        for (int i = 0; i < b.size(); i++) {
            MutableComponent regel;
            if (i < s.regels.gelegd()) {
                regel = Component.literal("✔ ").append(deelNaam(b.get(i))).withStyle(ChatFormatting.GREEN);
            } else if (i == s.regels.gelegd()) {
                regel = Component.literal("➜ ").append(deelNaam(b.get(i))).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
            } else {
                regel = Component.literal("· ").append(deelNaam(b.get(i))).withStyle(ChatFormatting.WHITE);
            }
            t.append(Component.literal("\n")).append(regel);
        }
        Display.TextDisplay d = D1Tijdelijk.tekst(s.level, s.grillTop.add(0, 1.15, 0.15), t, 0.5f, 0xC0F4E9C8, 150);
        if (d != null) {
            s.bon = D1Tijdelijk.zet(s.level, d, p);
        }
    }

    /** "Bestelling 2/5 · Volgende: Kaas" over the hotbar. */
    private static void balk(ServerPlayer p, Spel s) {
        GrillSpel.Deel v = s.volgendeOp > 0 ? null : s.regels.volgende();
        int nr = Math.min(GrillSpel.AANTAL, s.regels.klaar() + (s.volgendeOp > 0 ? 0 : 1));
        MutableComponent t = E2SpD1Start.tekst("bakken.teller", nr, GrillSpel.AANTAL).withStyle(ChatFormatting.GOLD);
        if (v != null) {
            t.append(Component.literal("  ·  ").withStyle(ChatFormatting.GRAY)).append(E2SpD1Start.tekst("bakken.volgende", deelNaam(v)).withStyle(ChatFormatting.YELLOW));
        }
        p.sendOverlayMessage(t);
    }

    static MutableComponent deelNaam(GrillSpel.Deel d) {
        return E2SpD1Start.tekst("deel." + d.id());
    }

    // =====================================================================================================================
    // the end
    // =====================================================================================================================

    /** Five orders: the title, the sound, the time (a score only), then the scene bakken_klaar and the next step. */
    private static void klaar(ServerPlayer p, Spel s) {
        s.klaar = true;
        int sec = (int) Math.max(1, (nu(p) - s.start) / 20);
        int beste = E2SpD1Start.lijn().teller(p, E2SpD1Start.T_BESTE_TIJD);
        boolean record = beste == 0 || sec < beste;
        if (record) {
            E2SpD1Start.lijn().teller(p, E2SpD1Start.T_BESTE_TIJD, sec);
        }
        E2SpD1Start.titel(p, E2SpD1Start.tekst("bakken.klaar.titel").withStyle(ChatFormatting.GOLD),
                E2SpD1Start.tekst("bakken.klaar.onder").withStyle(ChatFormatting.YELLOW), 60);
        s.level.playSound(null, s.grillSpeler, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1.0f);
        SpAudio.stinger(p, "stinger.lach");
        p.sendSystemMessage(E2SpD1Start.tekst(record ? "bakken.tijd.record" : "bakken.tijd", tijd(sec), tijd(record ? sec : beste))
                .withStyle(ChatFormatting.AQUA));
        StapHint.later(p, 40, Bakken::klaarScene);
    }

    static String tijd(int sec) {
        return sec / 60 + ":" + (sec % 60 < 10 ? "0" : "") + sec % 60;
    }

    private static void klaarScene(ServerPlayer p) {
        Spel s = SPELLEN.get(p.getUUID());
        BlockPos gs = s != null ? s.grillSpeler : E2SpD1Start.plek(E2SpD1Start.guheinde(p.level().getServer()), "grill_speler");
        if (s != null) {
            s.ruimOp();
        }
        if (E2SpD1Start.stap(p) != SpVerhaal.R_BAKKEN) {
            SPELLEN.remove(p.getUUID());
            return;
        }
        if (gs == null || !p.isAlive() || !Cutscenes.speel(p, D1Scenes.BAKKEN_KLAAR, gs, Rotation.NONE, Bakken::afronden)) {
            afronden(p);
        }
    }

    /** The step is done: the counter is cleared (CONTRACT_E2_C 2.4) and the line goes on to sp_d1_stad. */
    static void afronden(ServerPlayer p) {
        Spel s = SPELLEN.remove(p.getUUID());
        if (s != null) {
            s.ruimOp();
        }
        if (E2SpD1Start.stap(p) == SpVerhaal.R_BAKKEN) {
            E2SpD1Start.lijn().teller(p, E2SpD1Start.T_BESTELD, 0);
            E2SpD1Start.lijn().verder(p, SpVerhaal.R_BAKKEN);
        }
    }

    /** Stops this player's game (the entities go; the orders done stay in the line). */
    static void stop(ServerPlayer p) {
        Spel s = SPELLEN.remove(p.getUUID());
        if (s != null) {
            s.ruimOp();
        }
    }

    static void vergeet(UUID speler) {
        Spel s = SPELLEN.remove(speler);
        if (s != null) {
            s.ruimOp();
        }
    }
}
