package nl.juiced.guhs.feature.vadsrijk.questlopen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.karakter.Karakter;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestStap;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * U-QUESTLOPEN, the server side of the admin screen that clicks through the main line (zones 1-6) for playtesting. One session
 * per player (in memory; OP only; the CURRENT character only: a character switch resets it). "Volgende":
 * <ol>
 *   <li>the level goes to at least the step's minimum (or the "expected" level, toggle);</li>
 *   <li>the step is done through the real hooks ({@link QlDoe}: real NPC click / spot / area first, then the hook, then the
 *       counter), so flags, rewards and unlocks happen as in the game;</li>
 *   <li>the screen closes while dialogues and scenes play (opening, BAM, flights, Vahoegnir, the Wolkwal swim, flashes, arrival
 *       films: all through the real code) and waits until everything is quiet ({@link #rustig});</li>
 *   <li>then it teleports you to the next step's spot (a floor you can stand on) and opens again.</li>
 * </ol>
 * Boss steps get two buttons instead: "Zelf vechten" (into the arena: the real intro + title card, you fight) and "Laat hem
 * verslaan" (same, and after the intro the boss is beaten from your side: its real defeat runs: cheer-up scene, loot chest, story
 * moment). "Spring naar" fast-forwards with all output muted (KernPayloads) and the zones' scene flush.
 */
public final class QuestLoper {
    static final Logger LOG = LogUtils.getLogger();
    static final String K = "vadsrijk.questlopen.";
    /** Wait at most this long for one scene / dialogue (then the screen comes back anyway). */
    static final int MAX_WACHT = 20 * 60 * 5;

    enum Fase {
        RUST, NAAR, WACHT, AANKOMST, BAAS_IN, VERSLA, VECHT, SPOEL, HERHAAL
    }

    static final class Sessie {
        int slot = -1;
        boolean begrepen;
        boolean verwacht;   // level mode: false = minimum, true = expected
        boolean adv, rijdier;
        Fase fase = Fase.RUST;
        int stap = -1, poging;
        long sinds;
        int rust;
        boolean versla, introGezien;
        int slagen;
        @Nullable
        BlockPos baasPlek;
        int springNaar = -1;
        boolean stil;
        @Nullable
        Component melding;
        final List<CustomPacketPayload> scenes = new ArrayList<>();
        int herhaal;
        long herhaalOp;
        int gespoeld;
    }

    private static final Map<UUID, Sessie> SESSIES = new ConcurrentHashMap<>();

    private QuestLoper() {
    }

    static Sessie sessie(ServerPlayer p) {
        Sessie s = SESSIES.computeIfAbsent(p.getUUID(), k -> new Sessie());
        int slot = Karakter.slot(p);
        if (s.slot != slot) {   // (another character: start over, warn again)
            Sessie n = new Sessie();
            n.slot = slot;
            SESSIES.put(p.getUUID(), n);
            return n;
        }
        return s;
    }

    @Nullable
    static Sessie bestaand(ServerPlayer p) {
        return SESSIES.get(p.getUUID());
    }

    public static boolean op(ServerPlayer p) {
        return p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static List<QlStap> lijst(ServerPlayer p, Sessie s) {
        return QlStappen.lijst(Spelers.data(p).klasse, s.adv, s.rijdier);
    }

    static long nu(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        return server == null ? 0 : server.getTickCount();
    }

    /** Nothing of the story is playing for this player: no talking screen, no RijkScene, no zone scene. */
    public static boolean rustig(ServerPlayer p) {
        if (Praat.lopend(p) != null || RijkScenes.bezig(p)) {
            return false;
        }
        return zonesRustig(p);
    }

    static boolean zonesRustig(ServerPlayer p) {
        for (var h : QlHaken.SCENE) {
            try {
                if (h.test(p)) {
                    return false;
                }
            } catch (RuntimeException e) {
                LOG.warn("questlopen: scene check", e);
            }
        }
        return true;
    }

    // ---- actions (buttons; the command opens) ----------------------------------------------------------------------------------------

    public static void open(ServerPlayer p) {
        if (!op(p)) {
            return;
        }
        Sessie s = sessie(p);
        if (s.fase == Fase.RUST || s.fase == Fase.VECHT || s.fase == Fase.SPOEL) {
            stuur(p, s, true);
        } else {
            s.melding = Component.translatable(K + "melding.wacht");
            stuur(p, s, true);
        }
    }

    public static void actie(ServerPlayer p, String wat, int arg) {
        if (!op(p)) {
            return;   // (never for non-OPs, also not via a hand-made packet)
        }
        Sessie s = sessie(p);
        if (wat.equals("open")) {
            open(p);
            return;
        }
        if (wat.equals("begrepen")) {
            s.begrepen = true;
            stuur(p, s, false);
            return;
        }
        if (wat.equals("level") || wat.equals("adv") || wat.equals("rijdier")) {
            switch (wat) {
                case "level" -> s.verwacht = arg != 0;
                case "adv" -> s.adv = arg != 0;
                default -> s.rijdier = arg != 0;
            }
            stuur(p, s, false);
            return;
        }
        if (!s.begrepen) {
            return;
        }
        boolean bezig = s.fase != Fase.RUST && s.fase != Fase.VECHT;
        if (bezig && !wat.equals("stop")) {
            s.melding = Component.translatable(K + "melding.wacht");
            stuur(p, s, false);
            return;
        }
        s.melding = null;
        switch (wat) {
            case "volgende" -> volgende(p, s);
            case "zelf" -> baas(p, s, false);
            case "versla" -> {
                if (s.fase == Fase.VECHT) {
                    s.fase = Fase.VERSLA;
                    s.sinds = nu(p);
                    s.slagen = 0;
                    sluit(p);
                } else {
                    baas(p, s, true);
                }
            }
            case "terug" -> terug(p, s);
            case "spring" -> spring(p, s, arg);
            case "herhaal" -> herhaal(p, s);
            case "teleport" -> alleenTeleport(p, s);
            case "reset" -> reset(p, s);
            case "stop" -> {
                if (s.fase == Fase.SPOEL) {
                    eindeSpoel(p, s, false);
                } else {
                    s.fase = Fase.RUST;
                    stuur(p, s, false);
                }
            }
            default -> {
            }
        }
    }

    static void volgende(ServerPlayer p, Sessie s) {
        List<QlStap> l = lijst(p, s);
        QlStap st = QlStappen.huidig(p, l);
        if (st == null) {
            s.melding = Component.translatable(K + "melding.klaar").withStyle(ChatFormatting.GOLD);
            stuur(p, s, false);
            return;
        }
        if (st.baas()) {
            baas(p, s, true);
            return;
        }
        zetLevelVoor(p, s, st);
        s.scenes.clear();
        s.stap = st.nr();
        BlockPos b = QlPlek.van(p, st);
        if (b != null && (!nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.in(p) || QlPlek.afstand(p, b) > 16)) {
            QlPlek.naar(p, b, afstand(st));
            s.fase = Fase.NAAR;   // (first let the chunks + NPCs load)
            s.sinds = nu(p);
            sluit(p);
            return;
        }
        voer(p, s, st);
    }

    static void voer(ServerPlayer p, Sessie s, QlStap st) {
        s.stap = st.nr();
        s.poging = 0;
        s.rust = 0;
        s.fase = Fase.WACHT;
        s.sinds = nu(p);
        sluit(p);
        try {
            QlDoe.echt(p, st, false);
        } catch (RuntimeException e) {
            LOG.warn("questlopen: step {} failed", st, e);
        }
    }

    /** A boss step: into the arena (its real intro plays), then you fight or it is beaten for you. */
    static void baas(ServerPlayer p, Sessie s, boolean versla) {
        List<QlStap> l = lijst(p, s);
        QlStap st = QlStappen.huidig(p, l);
        if (st == null || !st.baas()) {
            volgende(p, s);
            return;
        }
        zetLevelVoor(p, s, st);
        s.scenes.clear();
        s.stap = st.nr();
        s.versla = versla;
        s.introGezien = false;
        s.slagen = 0;
        s.baasPlek = QlPlek.baasPos(st);
        s.sinds = nu(p);
        sluit(p);
        if (s.baasPlek == null) {
            s.melding = Component.translatable(K + "melding.geen_baas");
            s.fase = Fase.WACHT;
            s.poging = 1;
            s.rust = 0;
            QlDoe.haak(p, st);
            return;
        }
        Arena a = Arenas.op(s.baasPlek.getX() + 0.5, s.baasPlek.getY() + 0.5, s.baasPlek.getZ() + 0.5);
        if (a != null) {
            QlPlek.naar(p, QlPlek.inArena(a), 0);
        } else {
            QlPlek.naar(p, s.baasPlek, 6);
        }
        s.fase = Fase.BAAS_IN;
    }

    static void terug(ServerPlayer p, Sessie s) {
        List<QlStap> l = lijst(p, s);
        QlStap cur = QlStappen.huidig(p, l);
        int c = cur == null ? l.size() : cur.nr();
        int doel = c - 1;
        for (int poging = 0; doel >= 0 && poging < 6; poging++) {
            QlStappen.zetStand(p, l, doel);
            QlStap nu = QlStappen.huidig(p, l);
            if (nu == null || nu.nr() <= doel) {
                break;
            }
            doel--;   // (a step that counts by itself, e.g. "have level n": one more back)
        }
        if (doel < 0) {
            stuur(p, s, false);
            return;
        }
        s.melding = Component.translatable(K + "melding.terug");
        naarStap(p, s, l.get(doel));
    }

    static void spring(ServerPlayer p, Sessie s, int naar) {
        List<QlStap> l = lijst(p, s);
        if (naar < 0 || naar >= l.size()) {
            return;
        }
        QlStap cur = QlStappen.huidig(p, l);
        int c = cur == null ? l.size() : cur.nr();
        if (naar <= c) {
            QlStappen.zetStand(p, l, naar);
            s.melding = Component.translatable(K + "melding.terug");
            zetLevelVoor(p, s, l.get(naar));
            naarStap(p, s, l.get(naar));
            return;
        }
        s.springNaar = naar;
        s.fase = Fase.SPOEL;
        s.stil = true;
        s.rust = 0;
        s.sinds = nu(p);
        s.gespoeld = 0;
        s.melding = Component.translatable(K + "melding.spoelen", c + 1, naar + 1);
        stuur(p, s, false);
    }

    static void herhaal(ServerPlayer p, Sessie s) {
        if (s.scenes.isEmpty()) {
            s.melding = Component.translatable(K + "melding.geen_scene");
            stuur(p, s, false);
            return;
        }
        s.fase = Fase.HERHAAL;
        s.herhaal = 0;
        s.herhaalOp = nu(p) + 5;
        s.rust = 0;
        sluit(p);
    }

    static void alleenTeleport(ServerPlayer p, Sessie s) {
        List<QlStap> l = lijst(p, s);
        QlStap st = QlStappen.huidig(p, l);
        if (st == null) {
            return;
        }
        naarStap(p, s, st);
    }

    static void reset(ServerPlayer p, Sessie s) {
        Questlog.wisAlles(p);
        Spelers.data(p).gezien.remove("npc:opening");
        Spelers.data(p).gezien.remove("npc:bam");
        s.scenes.clear();
        s.melding = Component.translatable(K + "melding.reset");
        List<QlStap> l = lijst(p, s);
        naarStap(p, s, l.get(0));
    }

    /** Teleport to a step's spot and come back when quiet (arrival triggers may play). */
    static void naarStap(ServerPlayer p, Sessie s, QlStap st) {
        BlockPos b = QlPlek.van(p, st);   // (a boss step: the arena door, outside)
        if (b != null) {
            QlPlek.naar(p, b, afstand(st));
        }
        s.fase = Fase.AANKOMST;
        s.sinds = nu(p);
        s.rust = 0;
        sluit(p);
    }

    static int afstand(QlStap st) {
        Quest.Doel d = st.d();
        boolean npc = st.soort() == QlStap.Soort.NEEM || st.soort() == QlStap.Soort.LEVER
                || d != null && (d.type() == Quest.Doel.Type.PRAAT || d.type() == Quest.Doel.Type.ESCORTE || d.plek().startsWith("npc:"));
        return npc ? 2 : 0;
    }

    static void zetLevelVoor(ServerPlayer p, Sessie s, QlStap st) {
        int lv = s.verwacht ? st.verwachtLv() : st.minLv();
        Quest q = st.q();
        QlDoe.zetLevel(p, st.klasseLv() && q != null ? q.klasse() : null, lv);
    }

    // ---- the tick -------------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        for (var e : SESSIES.entrySet()) {
            Sessie s = e.getValue();
            if (s.fase == Fase.RUST) {
                continue;
            }
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                if (s.stil) {
                    s.stil = false;
                }
                s.fase = Fase.RUST;
                continue;
            }
            try {
                tik(p, s);
            } catch (RuntimeException ex) {
                LOG.warn("questlopen: tick", ex);
                s.stil = false;
                s.fase = Fase.RUST;
                s.melding = Component.literal("Fout: " + ex);
                stuur(p, s, true);
            }
        }
    }

    static void tik(ServerPlayer p, Sessie s) {
        long nu = nu(p);
        List<QlStap> l = lijst(p, s);
        QlStap st = s.stap >= 0 && s.stap < l.size() ? l.get(s.stap) : null;
        switch (s.fase) {
            case NAAR -> {
                if (nu - s.sinds >= 20 && st != null) {
                    voer(p, s, st);
                }
            }
            case WACHT -> {
                if (st == null) {
                    s.fase = Fase.RUST;
                    return;
                }
                if (!rustig(p)) {
                    s.rust = 0;
                    if (nu - s.sinds > MAX_WACHT) {
                        s.melding = Component.translatable(K + "melding.hangt");
                        s.fase = Fase.RUST;
                        stuur(p, s, true);
                    }
                    return;
                }
                if (++s.rust < 10) {
                    return;
                }
                if (QlStappen.klaar(p, st)) {
                    naStap(p, s, l);
                    return;
                }
                s.poging++;
                s.rust = 0;
                if (s.poging == 1) {
                    QlDoe.haak(p, st);
                } else if (s.poging == 2) {
                    QlDoe.dwing(p, st);
                } else {
                    s.melding = Component.translatable(K + "melding.vast");
                    s.fase = Fase.RUST;
                    stuur(p, s, true);
                }
            }
            case AANKOMST -> {
                if (!rustig(p)) {
                    s.rust = 0;
                    if (nu - s.sinds > MAX_WACHT) {
                        s.fase = Fase.RUST;
                        stuur(p, s, true);
                    }
                    return;
                }
                if (++s.rust >= 20) {
                    s.fase = Fase.RUST;
                    stuur(p, s, true);
                }
            }
            case BAAS_IN -> {
                if (st == null || QlStappen.klaar(p, st)) {
                    naarWacht(p, s, 0);
                    return;
                }
                VadsrijkBaas b = vindBaas(p, s, st);
                if (b == null) {
                    if (nu - s.sinds > 200) {
                        s.melding = Component.translatable(K + "melding.geen_baas");
                        naarWacht(p, s, 1);
                        QlDoe.haak(p, st);
                    }
                    return;
                }
                if (b.inIntro() || RijkScenes.bezig(p)) {
                    s.introGezien = true;
                    return;
                }
                if (!s.introGezien && nu - s.sinds < 60) {
                    return;
                }
                s.sinds = nu;
                s.slagen = 0;
                if (s.versla) {
                    s.fase = Fase.VERSLA;
                } else {
                    s.fase = Fase.VECHT;
                    p.sendSystemMessage(Component.translatable(K + "vecht").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            case VERSLA -> {
                if (st == null || QlStappen.klaar(p, st)) {
                    naarWacht(p, s, 0);
                    return;
                }
                VadsrijkBaas b = vindBaas(p, s, st);
                if (b == null || b.geplofd()) {
                    naarWacht(p, s, 0);   // (its defeat is playing: wait for the cheer-up scene + the story moment)
                    return;
                }
                if ((nu - s.sinds) % 5 != 0) {
                    return;
                }
                s.slagen++;
                if (s.slagen <= 8) {
                    b.krijgSchade(b.maxVads() * 0.15 + 1, p);
                } else if (s.slagen <= 14) {
                    b.krijgSchade(b.vads() + 1, p);
                } else {
                    s.melding = Component.translatable(K + "melding.immuun");
                    naarWacht(p, s, 1);
                    QlDoe.haak(p, st);
                }
            }
            case VECHT -> {
                if (st == null || QlStappen.klaar(p, st)) {
                    naarWacht(p, s, 0);
                }
            }
            case SPOEL -> spoelTik(p, s, l);
            case HERHAAL -> {
                if (s.herhaal < s.scenes.size()) {
                    if (nu >= s.herhaalOp) {
                        CustomPacketPayload x = s.scenes.get(s.herhaal++);
                        if (x instanceof KernPayloads.Scene sc) {
                            RijkScenes.speel(p, sc.scene());
                            s.herhaalOp = nu + sc.scene().duur() + 5;
                        } else if (x instanceof KernPayloads.Titel t) {
                            KernPayloads.stuur(p, t);
                            s.herhaalOp = nu + t.ticks() + 5;
                        }
                    }
                    return;
                }
                if (nu >= s.herhaalOp && rustig(p)) {
                    s.fase = Fase.RUST;
                    stuur(p, s, true);
                }
            }
            default -> {
            }
        }
    }

    static void naarWacht(ServerPlayer p, Sessie s, int poging) {
        s.fase = Fase.WACHT;
        s.poging = poging;
        s.rust = 0;
        s.sinds = nu(p);
    }

    /** The step is done: level + teleport for the next one, then back when quiet. */
    static void naStap(ServerPlayer p, Sessie s, List<QlStap> l) {
        QlStap nxt = QlStappen.huidig(p, l);
        if (nxt != null) {
            zetLevelVoor(p, s, nxt);
            BlockPos b = QlPlek.van(p, nxt);
            if (b != null && (!nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.in(p) || QlPlek.afstand(p, b) > 6)) {
                QlPlek.naar(p, b, afstand(nxt));
            }
        }
        s.fase = Fase.AANKOMST;
        s.sinds = nu(p);
        s.rust = 0;
    }

    @Nullable
    static VadsrijkBaas vindBaas(ServerPlayer p, Sessie s, QlStap st) {
        ServerLevel sl = QlPlek.level(p);
        if (sl == null || s.baasPlek == null) {
            return null;
        }
        Arena a = Arenas.op(s.baasPlek.getX() + 0.5, s.baasPlek.getY() + 0.5, s.baasPlek.getZ() + 0.5);
        if (a != null) {
            VadsrijkBaas b = Arenas.baas(sl, a.id());
            if (b != null) {
                return b;
            }
        }
        Quest.Doel d = st.d();
        String type = d != null && d.type() == Quest.Doel.Type.BAAS && !d.doel().startsWith("gebied:") ? d.doel() : null;
        VadsrijkBaas beste = null;
        double bd = Double.MAX_VALUE;
        for (VadsrijkBaas b : sl.getEntitiesOfClass(VadsrijkBaas.class, new AABB(s.baasPlek).inflate(48), x -> !x.geplofd() && x.isAlive())) {
            if (type != null && !BuiltInRegistries.ENTITY_TYPE.getKey(b.getType()).toString().equals(type.contains(":") ? type : "guhs:" + type)) {
                continue;
            }
            double dd = b.distanceToSqr(s.baasPlek.getX(), s.baasPlek.getY(), s.baasPlek.getZ());
            if (dd < bd) {
                bd = dd;
                beste = b;
            }
        }
        return beste;
    }

    // ---- "Spring naar": fast-forward, muted --------------------------------------------------------------------------------------

    static void spoelTik(ServerPlayer p, Sessie s, List<QlStap> l) {
        long nu = nu(p);
        RijkScenes.vergeet(p);   // (films are muted: only the zones' server-side scene runners are waited for)
        if (Praat.lopend(p) != null) {
            QuestMotor.afronden(p);
            Praat.vergeet(p);
        }
        if (!zonesRustig(p)) {
            QlDoe.spoel(p);
            if (!zonesRustig(p)) {
                s.rust = 0;
                if (nu - s.sinds < 20 * 60) {
                    return;
                }
            }
        }
        if (++s.rust < 2) {
            return;
        }
        s.rust = 0;
        s.sinds = nu;
        QlStap cur = QlStappen.huidig(p, l);
        if (cur == null || cur.nr() >= s.springNaar) {
            eindeSpoel(p, s, true);
            return;
        }
        stilStap(p, s, cur);
        s.gespoeld++;
        if (s.gespoeld % 5 == 0) {
            s.melding = Component.translatable(K + "melding.spoelen", cur.nr() + 1, s.springNaar + 1);
            stuur(p, s, false);
        }
    }

    /** One step silently, all three tiers at once (what the robot does). */
    static void stilStap(ServerPlayer p, Sessie s, QlStap cur) {
        zetLevelVoor(p, s, cur);
        try {
            if (!cur.baas()) {
                QlDoe.echt(p, cur, true);
            }
            if (!QlStappen.klaar(p, cur)) {
                QlDoe.haak(p, cur);
                QlDoe.spoel(p);
            }
            if (!QlStappen.klaar(p, cur)) {
                QlDoe.dwing(p, cur);
            }
        } catch (RuntimeException e) {
            LOG.warn("questlopen: fast-forward step {}", cur, e);
            QlDoe.dwing(p, cur);
        }
        QuestMotor.afronden(p);
        Praat.vergeet(p);
    }

    static void eindeSpoel(ServerPlayer p, Sessie s, boolean teleport) {
        s.stil = false;
        RijkScenes.vergeet(p);
        Praat.sluit(p);
        QuestMotor.vergeet(p);
        KernPayloads.stuurHud(p);
        Questlog.kijkLive(p);
        s.melding = Component.translatable(K + "melding.gesprongen");
        List<QlStap> l = lijst(p, s);
        if (teleport && s.springNaar >= 0 && s.springNaar < l.size()) {
            QlStap st = l.get(s.springNaar);
            zetLevelVoor(p, s, st);
            naarStap(p, s, st);
        } else {
            s.fase = Fase.RUST;
            stuur(p, s, true);
        }
        s.springNaar = -1;
    }

    /** (tests) "Spring naar" without waiting for scenes between the steps (the robot's way). */
    public static void springDirect(ServerPlayer p, boolean adv, boolean rijdier, int naar) {
        Sessie s = sessie(p);
        s.adv = adv;
        s.rijdier = rijdier;
        s.stil = true;
        try {
            List<QlStap> l = lijst(p, s);
            for (int n = 0; n < l.size() + 5; n++) {
                QlStap cur = QlStappen.huidig(p, l);
                if (cur == null || cur.nr() >= naar) {
                    break;
                }
                stilStap(p, s, cur);
                QlDoe.spoel(p);
            }
        } finally {
            s.stil = false;
            s.fase = Fase.RUST;
        }
    }

    // ---- payload hook (KernPayloads.VOOR_STUUR) ------------------------------------------------------------------------------------

    /** true = swallow (muted fast-forward); records the scenes and title cards of the current step for "Herhaal scène". */
    static boolean voorStuur(ServerPlayer p, CustomPacketPayload x) {
        Sessie s = SESSIES.get(p.getUUID());
        if (s == null) {
            return false;
        }
        if (s.stil) {
            return x instanceof KernPayloads.Scene || x instanceof KernPayloads.Titel;
        }
        if (s.fase != Fase.HERHAAL && s.fase != Fase.RUST && (x instanceof KernPayloads.Scene || x instanceof KernPayloads.Titel) && s.scenes.size() < 16) {
            s.scenes.add(x);
        }
        return false;
    }

    // ---- to the client -------------------------------------------------------------------------------------------------------------

    static void sluit(ServerPlayer p) {
        CompoundTag m = new CompoundTag();
        m.putBoolean("sluit", true);
        QlNet.stuur(p, new QlNet.Stand(m, List.of()));
    }

    static void stuur(ServerPlayer p, Sessie s, boolean open) {
        List<Component> t = new ArrayList<>();
        CompoundTag m = new CompoundTag();
        m.putBoolean("open", open);
        m.putString("karakter", Karakter.naam(p));
        m.putBoolean("begrepen", s.begrepen);
        m.putBoolean("verwacht", s.verwacht);
        m.putBoolean("adv", s.adv);
        m.putBoolean("rijdier", s.rijdier);
        m.putString("fase", s.fase.name());
        m.putInt("level", Spelers.level(p));
        m.putInt("klasseLevel", Spelers.klasseLevel(p));
        m.putBoolean("scenes", !s.scenes.isEmpty());
        if (s.melding != null) {
            m.putInt("melding", t.size());
            t.add(s.melding);
        }
        List<QlStap> l = lijst(p, s);
        QlStap cur = QlStappen.huidig(p, l);
        m.putInt("huidig", cur == null ? -1 : cur.nr());
        ListTag st = new ListTag();
        for (QlStap x : l) {
            CompoundTag c = new CompoundTag();
            c.putInt("z", x.zone());
            c.putInt("s", x.soort().ordinal());
            c.putInt("lv", x.minLv());
            c.putInt("vl", x.verwachtLv());
            c.putBoolean("kl", x.klasseLv());
            c.putBoolean("x", x.extra());
            c.putBoolean("k", QlStappen.klaar(p, x));
            c.putBoolean("b", x.baas());
            c.putString("q", x.quest());
            c.putInt("t", t.size());
            teksten(p, x, t);
            st.add(c);
        }
        m.put("stappen", st);
        QlNet.stuur(p, new QlNet.Stand(m, t));
    }

    /** The 4 texts of a step: quest title, what, where, who. */
    static void teksten(ServerPlayer p, QlStap x, List<Component> t) {
        Quest q = x.q();
        if (x.soort() == QlStap.Soort.OPENING || q == null) {
            t.add(Component.translatable(K + "opening"));
            t.add(Component.translatable(K + "opening.wat"));
            t.add(QuestStap.gebiedPlek("knuffelkamp"));
            t.add(Component.empty());
            return;
        }
        t.add(Component.translatable(q.titelKey()));
        switch (x.soort()) {
            case NEEM -> {
                t.add(Component.translatable(K + "wat.neem"));
                t.add(QuestStap.npcPlek(q.gever()));
                t.add(QuestStap.naam(q.gever()));
            }
            case LEVER -> {
                t.add(Component.translatable(K + "wat.lever"));
                t.add(QuestStap.npcPlek(q.inleveren()));
                t.add(QuestStap.naam(q.inleveren()));
            }
            default -> {
                Quest.Doel d = x.d();
                var wat = Component.translatable(q.doelKey(x.doel())).copy();
                if (d != null && d.n() > 1) {
                    wat.append(Component.literal(" (" + d.n() + "×)"));
                }
                t.add(wat);
                t.add(waar(q, d));
                String wie = d == null ? "" : d.plek().startsWith("npc:") ? d.plek().substring(4)
                        : d.type() == Quest.Doel.Type.PRAAT || d.type() == Quest.Doel.Type.ESCORTE ? d.doel() : "";
                t.add(wie.isEmpty() ? Component.empty() : QuestStap.naam(wie));
            }
        }
    }

    static Component waar(Quest q, @Nullable Quest.Doel d) {
        if (d == null) {
            return Component.empty();
        }
        if (!d.plek().isEmpty()) {
            return Component.translatable(QuestPlek.tekstKey(d.plek()));
        }
        String P = QuestStap.PLEK;
        return switch (d.type()) {
            case PRAAT, ESCORTE -> QuestStap.npcPlek(d.doel());
            case GEBIED, GEHEIM -> d.doel().startsWith("poi:") ? QuestStap.poiPlek(d.doel().substring(4))
                    : d.type() == Quest.Doel.Type.GEBIED ? QuestStap.gebiedPlek(d.doel().split("\\|")[0]) : Component.translatable(P + "overal." + d.doel());
            case KAMPVUUR -> d.doel().equals("*") ? Component.translatable(P + "overal") : Component.translatable(P + "kampvuur." + d.doel());
            case OPVROLIJK, ELITE -> d.doel().startsWith("gebied:") ? QuestStap.gebiedPlek(d.doel().substring(7).split("\\|")[0]) : Component.translatable(P + "overal");
            case VERZAMEL -> d.bron().startsWith("gebied:") ? QuestStap.gebiedPlek(d.bron().substring(7).split("\\|")[0]) : Component.translatable(P + "overal");
            case BAAS -> Component.translatable(P + "baas." + (d.doel().startsWith("gebied:") ? d.doel().substring(7) : d.doel().substring(d.doel().indexOf(':') + 1)));
            case KLASSE -> QuestStap.npcPlek("priesteres");
            case POP -> Component.translatable(P + "training");
            case MELD -> d.doel().startsWith("quiz:") ? QuestStap.npcPlek(q.gever())
                    : d.doel().equals("zwamtempel:deur") ? QuestStap.poiPlek("tempelpoort") : Component.translatable(P + "overal");
            default -> Component.translatable(P + "overal");
        };
    }
}
