package nl.juiced.guhs.feature.vadsrijk.questrobot;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcHaken;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPlekken;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestStap;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * U-QUESTROBOT: plays every main and side quest of zones 1..n start to finish through the REAL quest engine, and writes down
 * where something gets stuck. Per quest: wait until it is free (previous quests, a level gate: the robot "grinds" by setting the
 * level, which is reported when the gate is above the cap), take it by clicking its giver ({@link QuestMotor#klik} + the "yes"
 * answer), do every objective through the hook the game uses for it, hand it in by clicking the turn-in NPC, and close the
 * dialogue (its actie_klaar runs: scenes, flags, the next zone). Every dialogue page that opens is checked for text (nl_nl and
 * en_us). What the robot cannot play for real (a fight, a minigame) is reported by the hook that the real thing calls:
 * <ul>
 *   <li>PRAAT / ESCORTE (start) / hand-in / offer: {@link QuestMotor#klik} on the live NPC guh, or a stand-in made from its
 *       {@link NpcDef} when it is not loaded;</li>
 *   <li>GEBIED/GEHEIM "poi:x": the player is put on the spot and the ticker's poi check runs ({@link NpcHaken#robotPlekken});</li>
 *   <li>GEBIED area: {@link Haken#GEBIED_BETREDEN}; KAMPVUUR: {@link Haken#KAMPVUUR} at the area's campfire;</li>
 *   <li>OPVROLIJK / ELITE / BAAS / VERZAMEL drops: {@link Haken#VERSLAGEN} with a mob of the right kind standing in the right area
 *       ("unavoidable combat outcome");</li>
 *   <li>secrets: {@link NpcHaken#robotVind}; LEVEL: the level is set; MELD: {@link NpcApi#meld} (the static half checks that
 *       some code really reports that key).</li>
 * </ul>
 * Quest data is read from {@link Quests#alle()}, so zone 6 (quests_z6.json, once Quests reads it) is played automatically.
 */
public final class QuestRobot {
    public static final int CAP = Levels.MAX;
    /**
     * The zones' own "player is at this spot" ticks (story triggers: scenes, splinters, the finale...): run after the robot puts its
     * player on a spot. Zones 3-5 add theirs ({@code ZxHaken.stap}); marked U-QUESTROBOT there.
     */
    public static final List<java.util.function.BiConsumer<ServerPlayer, ServerLevel>> PLEK_HAKEN = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** The zones' "finish the scene that runs now" (ZxVerhaal.spoel): run after every spot, click and hand-in. */
    public static final List<java.util.function.Consumer<ServerPlayer>> SPOEL = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** Zones with puzzles in dialogue: solve an open one with the right answers (true when one was open). The real puzzle path. */
    public static final List<java.util.function.Predicate<ServerPlayer>> LOS_OP = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** One quest's outcome. */
    public static final class Uitslag {
        public final String quest;
        public final int zone;
        public final String soort;
        public String status = "niet gestart";   // gehaald / vast / niet gestart
        public String stap = "";
        public String waarom = "";
        public final List<String> notities = new ArrayList<>();
        public final List<String> problemen = new ArrayList<>();

        Uitslag(Quest q) {
            this.quest = q.id();
            this.zone = ZoneBoeken.zoneVan(q.id());
            this.soort = q.soort().name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** A spot a quest sends you to (for the standability check on the map tiles: tools/vadsrijk/questrobot/rapport.py). */
    public record Doel(String quest, String wat, String id, BlockPos pos) {
    }

    /** The whole run. */
    public static final class Rapport {
        public final Map<String, Uitslag> quests = new LinkedHashMap<>();
        public final List<String> algemeen = new ArrayList<>();
        public final List<Doel> doelen = new ArrayList<>();
        public int totZone;
        public long ms;
    }

    private final ServerPlayer p;
    private final ServerLevel npcLevel;
    private final Rapport r = new Rapport();
    private final Map<String, RijkGuh> invallers = new HashMap<>();
    @Nullable
    private Uitslag bezig;

    private QuestRobot(ServerPlayer p) {
        this.p = p;
        ServerLevel v = p.level().getServer() == null ? null : Vadsrijk.level(p.level().getServer());
        this.npcLevel = v != null ? v : (ServerLevel) p.level();
    }

    // ---- dialogue recorder (one Praat.versierder for the whole game; only records the robot's player) -------------------------
    private static final Map<UUID, QuestRobot> ACTIEF = new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean versierderAan;
    @Nullable
    private static Map<String, String> nlTekst;

    static synchronized void init() {
        if (!versierderAan) {
            versierderAan = true;
            Praat.versierder((sp, sleutel, spreker, data) -> {
                QuestRobot robot = ACTIEF.get(sp.getUUID());
                if (robot != null) {
                    robot.dialoog(sleutel, data);
                }
            });
        }
    }

    /** Plays zones 1..totZone (6 = everything there is) with this player; its quest state, level and inventory are wiped. */
    public static Rapport speel(ServerPlayer p, int totZone) {
        init();
        QuestRobot robot = new QuestRobot(p);
        robot.r.totZone = totZone;
        long t0 = System.currentTimeMillis();
        ACTIEF.put(p.getUUID(), robot);
        try {
            robot.alles(totZone);
        } finally {
            ACTIEF.remove(p.getUUID());
            for (RijkGuh g : robot.invallers.values()) {
                g.discard();
            }
            Praat.vergeet(p);
            QuestMotor.vergeet(p);
        }
        robot.r.ms = System.currentTimeMillis() - t0;
        return robot.r;
    }

    // ---- the run -----------------------------------------------------------------------------------------------------------------

    private void alles(int totZone) {
        npcTeksten();
        Questlog.wisAlles(p);
        p.getInventory().clearContent();
        Spelers.zetLevel(p, 1);
        List<Quest> lijst = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (ZoneBoeken.zoneVan(q.id()) <= totZone) {
                lijst.add(q);
                r.quests.put(q.id(), new Uitslag(q));
            }
        }
        Set<Integer> zonesKlaarGemeld = new java.util.HashSet<>();
        for (int ronde = 0; ronde < lijst.size() + 5; ronde++) {
            boolean voortgang = false;
            for (Quest q : lijst) {
                Uitslag u = r.quests.get(q.id());
                if (Questlog.klaar(p, q.id())) {
                    continue;
                }
                bezig = u;
                try {
                    if (speelEen(q, u)) {
                        voortgang = true;
                    }
                } catch (RuntimeException e) {
                    vast(u, u.stap.isEmpty() ? "?" : u.stap, "uitzondering: " + e);
                    com.mojang.logging.LogUtils.getLogger().warn("questrobot: {} crashed", q.id(), e);
                } finally {
                    bezig = null;
                    Praat.vergeet(p);
                }
                if (u.status.equals("vast") && !Questlog.klaar(p, q.id())) {
                    forceer(q, u);   // (so the chain behind it is still played and checked)
                    voortgang = true;
                }
                zoneVlaggen(lijst, zonesKlaarGemeld);
            }
            if (!voortgang) {
                break;
            }
        }
        // what never started: why
        for (Quest q : lijst) {
            Uitslag u = r.quests.get(q.id());
            if (Questlog.klaar(p, q.id()) && !u.status.equals("vast")) {
                u.status = "gehaald";
            } else if (u.status.equals("niet gestart")) {
                u.waarom = waaromNiet(q);
            }
        }
    }

    /** Every NPC guh: a name and its idle lines in nl_nl and en_us (a regenerated lang file once lost 20 NPC names). */
    private void npcTeksten() {
        List<String> mis = new ArrayList<>();
        for (NpcDef d : Npcs.alle()) {
            List<String> keys = new ArrayList<>(List.of(d.naamKey(), "vadsrijk.npc.praat." + d.id() + ".1"));
            for (String k : keys) {
                if (!Language.getInstance().has(k) || !nl().containsKey(k)) {
                    mis.add(k);
                }
            }
        }
        r.algemeen.add("NPC-teksten: " + Npcs.alle().size() + " NPC's, " + mis.size() + " ontbrekende naam/praat-teksten"
                + (mis.isEmpty() ? "" : "  << FOUT: " + String.join(", ", mis.subList(0, Math.min(12, mis.size()))) + (mis.size() > 12 ? ", ..." : "")));
    }

    /** True when this quest moved on (taken or handed in). */
    private boolean speelEen(Quest q, Uitslag u) {
        boolean bewogen = false;
        if (Questlog.status(p, q.id()) == 0) {
            if (!vrij(q)) {
                return false;
            }
            int lv = Questlog.lvVoor(p, q);
            if (lv >= q.lv() && !Questlog.beschikbaar(p, q) && q.na().stream().anyMatch(n -> n.startsWith("@"))) {
                return false;   // (the mount gate: wait until a mount is owned; reported at the end when it never opens)
            }
            if (lv < q.lv()) {
                if (q.lv() > CAP) {
                    vast(u, "aannemen", "levelpoort lv " + q.lv() + " is hoger dan de cap " + CAP);
                    return false;
                }
                zetLevel(q.klasse(), q.lv());
                u.notities.add("level " + lv + " -> " + q.lv() + " (levelpoort)");
            }
            u.stap = "aannemen";
            neemAan(q, u);
            if (Questlog.status(p, q.id()) == 0) {
                if (!u.status.equals("vast")) {
                    vast(u, "aannemen", "de gever biedt de quest niet aan");
                }
                return false;
            }
            bewogen = true;
        }
        if (!Questlog.actief(p, q.id())) {
            return bewogen;
        }
        for (int i = 0; i < q.doelen().size(); i++) {
            if (Questlog.doelKlaar(p, q, i)) {
                continue;
            }
            Quest.Doel d = q.doelen().get(i);
            u.stap = "stap " + i + " (" + d.type().name().toLowerCase(java.util.Locale.ROOT) + " " + d.doel() + ")";
            trackerCheck(q, i, u);
            doe(q, i, d, u);
            if (u.status.equals("vast")) {
                return bewogen;
            }
            if (!Questlog.doelKlaar(p, q, i) && Questlog.actief(p, q.id())) {
                vast(u, u.stap, "telt niet mee na de haak (" + Questlog.waarde(p, q, i) + "/" + d.n() + ")");
                return bewogen;
            }
            if (!Questlog.actief(p, q.id())) {
                break;   // (the click of a talk step also handed it in)
            }
        }
        if (Questlog.actief(p, q.id())) {
            u.stap = "inleveren bij " + q.inleveren();
            if (!Questlog.allesKlaar(p, q)) {
                vast(u, u.stap, "niet alle stappen klaar");
                return bewogen;
            }
            klik(q.inleveren(), u);
            if (!Questlog.klaar(p, q.id())) {
                vast(u, u.stap, "klikken op " + q.inleveren() + " levert de quest niet in");
                return bewogen;
            }
        }
        QuestMotor.afronden(p);   // (the hand-in dialogue closes: actie_klaar runs)
        spoel();
        Praat.vergeet(p);
        u.status = "gehaald";
        u.stap = "";
        return true;
    }

    /** A stuck quest is pushed through by hand (reported as "vast" + "doorgeduwd"), so everything after it is still played. */
    private void forceer(Quest q, Uitslag u) {
        try {
            if (Questlog.status(p, q.id()) == 0) {
                zetLevel(q.klasse(), q.lv());
                Questlog.neemAan(p, q);
            }
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (Questlog.doelKlaar(p, q, i)) {
                    continue;
                }
                switch (d.type()) {
                    case VERZAMEL -> Questlog.geef(p, new ItemStack(Questlog.item(d.doel()), d.n()));
                    case LEVEL -> zetLevel(q.klasse(), d.n());
                    case GEHEIM -> {
                        CompoundTag t = Questlog.map(p, switch (d.doel()) {
                            case "knuffel" -> "knuffels";
                            case "aanwijzing" -> "aanwijzingen";
                            case "schat" -> "schatten";
                            default -> d.doel().startsWith("teller:") ? d.doel().substring(7) : "robot_geheim";
                        });
                        for (int k = 0; k < d.n(); k++) {
                            t.putBoolean("robot_" + k, true);
                        }
                        Questlog.tel(p, d.type(), x -> x == d, d.n());
                    }
                    default -> Questlog.tel(p, d.type(), x -> x == d, d.n());
                }
            }
            Questlog.voltooi(p, q);
            QuestMotor.doeActie(p, q.actieKlaar(), null);
            QuestMotor.afronden(p);
            Praat.vergeet(p);
            u.notities.add("DOORGEDUWD door de robot (vastgelopen: " + u.waarom + ")");
        } catch (RuntimeException e) {
            u.notities.add("doorduwen mislukte: " + e);
        }
    }

    private boolean vrij(Quest q) {
        for (String na : q.na()) {
            if (na.startsWith("@")) {
                continue;   // (RijdierQuests.POORT: Questlog.beschikbaar asks it)
            }
            if (!Questlog.klaar(p, na)) {
                return false;
            }
        }
        return true;
    }

    private String waaromNiet(Quest q) {
        List<String> open = new ArrayList<>();
        for (String na : q.na()) {
            if (na.startsWith("@")) {
                if (!Questlog.beschikbaar(p, q) && vrij(q)) {
                    open.add(na + " (poort dicht: nog geen rijdier?)");
                }
                continue;
            }
            if (Quests.van(na) == null) {
                open.add(na + " (bestaat niet)");
            } else if (!Questlog.klaar(p, na)) {
                Uitslag v = r.quests.get(na);
                open.add(na + (v == null ? " (andere zone)" : " (" + v.status + ")"));
            }
        }
        if (q.klasse() != null && nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.van(q.klasse()) == null) {
            open.add("klasse " + q.klasse() + " bestaat niet");
        }
        return open.isEmpty() ? "nooit beschikbaar geworden (onbekend waarom)" : "wacht op " + String.join(", ", open);
    }

    // ---- taking a quest ----------------------------------------------------------------------------------------------------------

    private void neemAan(Quest q, Uitslag u) {
        if (q.auto() && !q.hoofd()) {
            // (an auto side/secret quest is never offered by a click: something in the world starts it)
            if (q.id().equals("geheim_spion")) {
                for (var e : NpcPlekken.AANWIJZING.entrySet()) {   // (the first clue starts it)
                    NpcHaken.robotVind(p, npcLevel, "aanwijzingen", e.getKey(), e.getValue());
                    break;
                }
            }
            if (Questlog.status(p, q.id()) == 0 && Questlog.beschikbaar(p, q)) {
                u.notities.add("auto-quest: door de robot gestart (de echte start is een wereldmoment)");
                Questlog.neemAan(p, q);
            }
            return;
        }
        if (q.auto() && Questlog.beschikbaar(p, q)) {
            // an auto main quest should have been started by the previous quest's action; the giver offers it as a fallback
            u.notities.add("auto-hoofdquest startte niet vanzelf na de vorige quest; via de gever aangenomen");
        }
        if (!Questlog.beschikbaar(p, q)) {
            vast(u, "aannemen", "Questlog.beschikbaar = false (lv " + Questlog.lvVoor(p, q) + "/" + q.lv() + ")");
            return;
        }
        for (int poging = 0; poging < 6 && Questlog.status(p, q.id()) == 0; poging++) {
            RijkGuh g = npc(q.gever(), u);
            if (g == null) {
                return;
            }
            Praat.vergeet(p);
            QuestMotor.vergeet(p);
            QuestMotor.klik(p, g);
            QuestMotor.kiesVoorTest(p, QuestMotor.AANNEMEN_OPTIE);
            QuestMotor.afronden(p);
        }
    }

    // ---- objectives --------------------------------------------------------------------------------------------------------------

    private void doe(Quest q, int i, Quest.Doel d, Uitslag u) {
        switch (d.type()) {
            case PRAAT -> klik(d.doel(), u);
            case GEBIED, GEHEIM -> {
                if (d.doel().startsWith("poi:")) {
                    naarPoi(q, d.doel().substring(4), u);
                } else if (d.type() == Quest.Doel.Type.GEBIED) {
                    String g = d.doel().split("\\|")[0];
                    if (Gebieden.van(g) == null) {
                        vast(u, u.stap, "gebied '" + g + "' bestaat niet: kern meldt het betreden nooit");
                        return;
                    }
                    Haken.GEBIED_BETREDEN.forEach(h -> h.accept(p, g));
                } else {
                    geheim(q, i, d, u);
                }
            }
            case KAMPVUUR -> {
                BlockPos b = NpcPlekken.KAMPVUUR.get(d.doel());
                if (b == null && d.doel().equals("*")) {
                    b = NpcPlekken.KAMPVUUR.values().iterator().next();
                }
                if (b == null) {
                    vast(u, u.stap, "geen kampvuur bekend voor gebied " + d.doel() + " (NpcPlekken.KAMPVUUR)");
                    return;
                }
                doel(q, "kampvuur", d.doel(), b);
                Gebieden.Gebied g = Gebieden.op(b);
                if (!d.doel().equals("*") && (g == null || !g.id().equals(d.doel()))) {
                    u.problemen.add(u.stap + ": het kampvuur van " + d.doel() + " ligt in gebied " + (g == null ? "geen" : g.id()) + " (telt dan niet)");
                }
                BlockPos fb = b;
                Haken.KAMPVUUR.forEach(h -> h.accept(p, fb));
            }
            case OPVROLIJK, ELITE, BAAS -> {
                VadsrijkMob mob = mob(d.doel(), d.type() == Quest.Doel.Type.BAAS, d.type() == Quest.Doel.Type.ELITE, u);
                if (mob == null) {
                    return;
                }
                for (int k = 0; k < d.n() && !Questlog.doelKlaar(p, q, i) && Questlog.actief(p, q.id()); k++) {
                    verslagen(mob);
                }
                if (d.type() == Quest.Doel.Type.BAAS) {
                    u.notities.add("baas " + d.doel() + ": 'verslagen'-haak afgevuurd (gevecht niet gespeeld)");
                }
            }
            case VERZAMEL -> verzamel(q, i, d, u);
            case ESCORTE -> {
                RijkGuh g = npc(d.doel(), u);
                if (g == null) {
                    return;
                }
                QuestMotor.klik(p, g);
                Praat.vergeet(p);
                QuestMotor.vergeet(p);
                if (!p.getUUID().equals(g.volgt())) {
                    vast(u, u.stap, d.doel() + " gaat niet volgen na een klik");
                    return;
                }
                String poi = d.bron().startsWith("poi:") ? d.bron().substring(4) : d.bron();
                BlockPos naar = NpcPlekken.poi(poi);
                if (naar == null) {
                    vast(u, u.stap, "escorte-bestemming poi '" + poi + "' staat niet in NpcPlekken.POI: de aankomst telt nooit");
                    return;
                }
                doel(q, "escorte", poi, naar);
                g.volg(null);
                Questlog.tel(p, Quest.Doel.Type.ESCORTE, x -> x == d, 1);
                u.notities.add("escorte " + d.doel() + " -> " + poi + ": aankomst gesimuleerd (lopen niet gespeeld)");
            }
            case LEVEL -> {
                if (d.n() > CAP) {
                    vast(u, u.stap, "level " + d.n() + " > cap " + CAP);
                    return;
                }
                zetLevel(q.klasse(), d.n());
            }
            case POP -> {
                Haken.BLOK_GERAAKT.forEach(h -> h.accept(p, NpcPlekken.TRAINING));
                if (!Questlog.doelKlaar(p, q, i)) {
                    Questlog.tel(p, Quest.Doel.Type.POP, x -> x == d, d.n());
                    u.notities.add("oefenpoppen: geteld zonder echte poppen (geen wereld hier)");
                }
            }
            case KLASSE -> NpcApi.meld(p, "klasse", Spelers.data(p).klasse);
            case MELD -> {
                if (meldViaPlek(q, i, d, u)) {
                    return;
                }
                int c = d.doel().indexOf(':');
                String soort = c < 0 ? d.doel() : d.doel().substring(0, c), wat = c < 0 ? "" : d.doel().substring(c + 1);
                for (int k = 0; k < d.n() && !Questlog.doelKlaar(p, q, i); k++) {
                    NpcApi.meld(p, soort, wat);
                }
                u.notities.add("meld " + d.doel() + " via NpcApi.meld (minigame/moment zelf niet gespeeld)");
            }
        }
    }

    private void naarPoi(Quest q, String poi, Uitslag u) {
        BlockPos b = NpcPlekken.poi(poi);
        if (b == null) {
            BlockPos qp = QuestPlek.POI.get(poi);
            if (qp == null) {
                vast(u, u.stap, "poi '" + poi + "' staat niet in NpcPlekken.POI en niet in QuestPlek.POI (nergens bekend)");
                return;
            }
            // (zone 6: its pois are only for the tracker; Z6Haken counts the arrival itself, in story order: its PLEK_HAKEN do that)
            // (not counted afterwards = "telt niet mee na de haak" in speelStappen)
            doel(q, "poi", poi, qp);
            opPlek(qp);
            return;
        }
        doel(q, "poi", poi, b);
        opPlek(b);
    }

    /** Stand on this spot for one tick of the quest tickers (npc poi check + the zones' story triggers), then back. */
    private void opPlek(BlockPos b) {
        net.minecraft.world.phys.Vec3 was = p.position();
        p.setPos(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        try {
            NpcHaken.robotPlekken(p);
            for (var h : PLEK_HAKEN) {
                try {
                    h.accept(p, npcLevel);
                } catch (RuntimeException ex) {
                    if (bezig != null) {
                        bezig.problemen.add("plek-haak gooit bij " + b.toShortString() + ": " + ex);
                    }
                }
            }
            losOp(bezig);
            spoel();
        } finally {
            p.setPos(was.x, was.y, was.z);
        }
    }

    private void losOp(Uitslag u) {
        for (var l : LOS_OP) {
            if (l.test(p) && u != null) {
                u.notities.add("puzzel in de dialoog opgelost (de goede antwoorden gekozen)");
            }
        }
    }

    private void spoel() {
        nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes.vergeet(p);   // (time-based "in a scene": the robot does not wait)
        for (var s : SPOEL) {
            try {
                s.accept(p);
            } catch (RuntimeException ex) {
                if (bezig != null) {
                    bezig.problemen.add("scene afspoelen gooit: " + ex);
                }
            }
        }
    }

    /**
     * A MELD step with a spot: go there (poi/baas/gebied: the zone's triggers run) or click that NPC; true when that alone made it
     * count (the real path; e.g. the zone-3 finale scene ends in "z3:finale" + zone3_klaar).
     */
    private boolean meldViaPlek(Quest q, int i, Quest.Doel d, Uitslag u) {
        String plek = d.plek();
        if (plek.isEmpty()) {
            return false;
        }
        for (int k = 0; k < d.n() + 1 && !Questlog.doelKlaar(p, q, i) && Questlog.actief(p, q.id()); k++) {
            int voor = Questlog.waarde(p, q, i);
            if (plek.startsWith("npc:")) {
                klik(plek.substring(4), u);
                losOp(u);
                QuestMotor.afronden(p);
                spoel();
            } else {
                BlockPos b = QuestPlek.pos(npcLevel, plek);
                if (b == null) {
                    return false;
                }
                opPlek(b);
            }
            if (Questlog.waarde(p, q, i) == voor) {
                break;
            }
        }
        if (Questlog.doelKlaar(p, q, i) || !Questlog.actief(p, q.id())) {
            u.notities.add("meld " + d.doel() + ": echt geteld via " + plek);
            return true;
        }
        return false;
    }

    private void geheim(Quest q, int i, Quest.Doel d, Uitslag u) {
        String soort = switch (d.doel()) {
            case "knuffel" -> "knuffels";
            case "aanwijzing" -> "aanwijzingen";
            case "schat" -> "schatten";
            default -> null;
        };
        if (soort != null) {
            Map<String, BlockPos> m = soort.equals("knuffels") ? NpcPlekken.KNUFFEL : soort.equals("aanwijzingen") ? NpcPlekken.AANWIJZING : NpcPlekken.SCHAT;
            if (m.size() < d.n()) {
                u.problemen.add(u.stap + ": er zijn maar " + m.size() + " " + soort + " in de wereld, de quest wil er " + d.n());
            }
            for (var e : m.entrySet()) {
                doel(q, soort, e.getKey(), e.getValue());
                NpcHaken.robotVind(p, npcLevel, soort, e.getKey(), e.getValue());
                Praat.vergeet(p);
            }
            return;
        }
        if (d.doel().startsWith("teller:")) {
            String map = d.doel().substring(7);
            CompoundTag t = Questlog.map(p, map);
            for (int k = 0; t.size() < d.n() && k < d.n(); k++) {
                t.putBoolean("robot_" + k, true);
            }
            Questlog.kijkLive(p);
            u.notities.add("teller " + map + " gevuld door de robot (" + d.n() + "x verzamelen niet gespeeld)");
            return;
        }
        vast(u, u.stap, "onbekend geheim-doel '" + d.doel() + "'");
    }

    private void verzamel(Quest q, int i, Quest.Doel d, Uitslag u) {
        if (Questlog.item(d.doel()) == Items.AIR) {
            vast(u, u.stap, "item " + d.doel() + " bestaat niet");
            return;
        }
        if (d.kans() > 0) {
            String bron = d.bron().isEmpty() ? "*" : d.bron();
            VadsrijkMob mob = mob(bron, false, false, u);
            if (mob == null) {
                return;
            }
            int max = Math.max(60, (int) Math.ceil(d.n() / d.kans() * 6));
            int k = 0;
            for (; k < max && !Questlog.doelKlaar(p, q, i) && Questlog.actief(p, q.id()); k++) {
                verslagen(mob);
            }
            u.notities.add(d.doel() + ": " + d.n() + " gevallen na " + k + " keer opvrolijken in " + bron);
            return;
        }
        Questlog.geef(p, new ItemStack(Questlog.item(d.doel()), d.n()));
        u.notities.add(d.doel() + ": valt nergens van (kans 0); door de robot gegeven");
    }

    // ---- helpers -----------------------------------------------------------------------------------------------------------------

    private void klik(String npc, Uitslag u) {
        RijkGuh g = npc(npc, u);
        if (g == null) {
            return;
        }
        Praat.vergeet(p);
        QuestMotor.vergeet(p);
        QuestMotor.klik(p, g);
        spoel();
    }

    /** The live NPC guh, or a stand-in made from its definition (not added to the world). */
    @Nullable
    private RijkGuh npc(String id, Uitslag u) {
        NpcDef d = Npcs.van(id);
        if (d == null) {
            vast(u, u.stap, "NPC '" + id + "' bestaat niet (Npcs)");
            return null;
        }
        if (d.plek() == null) {
            u.problemen.add("NPC " + id + " heeft geen plek");
        } else {
            doel(u.quest, "npc", id, d.plek());
        }
        RijkGuh echt = Npcs.levend(npcLevel, id);
        if (echt != null) {
            return echt;
        }
        RijkGuh g = invallers.get(id);
        if (g == null) {
            g = NpcRegistry.GUH.get().create(p.level(), EntitySpawnReason.EVENT);
            if (g == null) {
                vast(u, u.stap, "kon geen invaller voor NPC " + id + " maken");
                return null;
            }
            g.stelIn(d);
            invallers.put(id, g);
        }
        g.snapTo(p.getX() + 1, p.getY(), p.getZ());
        return g;
    }

    private final Map<String, VadsrijkMob> mobs = new HashMap<>();
    @Nullable
    private EntityType<?> eenBaas;

    /** A mob that fits an objective's target ("*", "gebied:x" or an entity type id), standing where it counts. */
    @Nullable
    private VadsrijkMob mob(String doel, boolean baas, boolean elite, Uitslag u) {
        String sleutel = doel + (baas ? "#baas" : "") + (elite ? "#elite" : "");
        VadsrijkMob m = mobs.get(sleutel);
        if (m != null) {
            return m;
        }
        BlockPos plek = null;
        Entity e;
        if (doel.equals("*") || doel.startsWith("gebied:")) {
            if (doel.startsWith("gebied:")) {
                for (String g : doel.substring(7).split("\\|")) {   // ("gebied:a|b": any of these areas)
                    if (Gebieden.van(g) == null) {
                        vast(u, u.stap, "gebied '" + g + "' bestaat niet (Gebieden)");
                        return null;
                    }
                    if (plek == null) {
                        plek = plekIn(g, baas);
                    }
                }
                if (plek == null) {
                    vast(u, u.stap, "geen enkele bekende plek (tracker/kampvuur/arena/midden) ligt in gebied '" + doel.substring(7) + "': opvrolijken daar telt nooit");
                    return null;
                }
            }
            e = baas ? baasType().create(p.level(), EntitySpawnReason.EVENT) : KernRegistry.PROEFKLODDER.get().create(p.level(), EntitySpawnReason.EVENT);
        } else {
            Identifier id = Identifier.tryParse(doel.contains(":") ? doel : "guhs:" + doel);
            if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                vast(u, u.stap, "wezen '" + doel + "' bestaat niet (entity registry)");
                return null;
            }
            e = BuiltInRegistries.ENTITY_TYPE.getValue(id).create(p.level(), EntitySpawnReason.EVENT);
        }
        if (!(e instanceof VadsrijkMob vm)) {
            vast(u, u.stap, "'" + doel + "' is geen Vadsrijk-wezen (VERSLAGEN-haak vuurt er nooit voor)");
            return null;
        }
        if (baas && !(vm instanceof VadsrijkBaas) && !doel.endsWith("vr_klodderreus")) {
            u.problemen.add(u.stap + ": " + doel + " is geen VadsrijkBaas (BAAS-stap telt alleen bazen)");
        }
        vm.stelIn(Math.max(1, Spelers.level(p)), null, elite, Set.of());
        BlockPos b = plek != null ? plek : p.blockPosition();
        vm.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        mobs.put(sleutel, vm);
        return vm;
    }

    private EntityType<?> baasType() {
        if (eenBaas == null) {
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                if (!BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals("guhs")) {
                    continue;
                }
                try {
                    Entity e = t.create(p.level(), EntitySpawnReason.EVENT);
                    if (e instanceof VadsrijkBaas) {
                        eenBaas = t;
                        e.discard();
                        break;
                    }
                    if (e != null) {
                        e.discard();
                    }
                } catch (RuntimeException ex) {
                    // (some entities cannot be made outside the world: skip)
                }
            }
        }
        return eenBaas;
    }

    /** A known spot inside area g (Gebieden.op == g), for a mob that must "stand there". */
    @Nullable
    private BlockPos plekIn(String g, boolean baas) {
        List<BlockPos> kandidaten = new ArrayList<>();
        if (baas) {
            kandidaten.add(QuestPlek.BAAS.get(g));
            kandidaten.add(NpcPlekken.BAAS.get(g));
        }
        kandidaten.add(QuestPlek.GEBIED.get(g));
        kandidaten.add(NpcPlekken.GEBIED_DOEL.get(g));
        kandidaten.add(NpcPlekken.KAMPVUUR.get(g));
        Gebieden.Gebied geb = Gebieden.van(g);
        if (geb != null) {
            int y = geb.laag() != null && geb.laag().heeftY() ? (Math.max(geb.laag().minY(), -64) + Math.min(geb.laag().maxY(), 320)) / 2 : 80;
            int cx = geb.laag() != null && geb.laag().rMax() > 0 ? geb.laag().cx() + (geb.laag().rMin() + geb.laag().rMax()) / 2 : (geb.minX() + geb.maxX()) / 2;
            int cz = geb.laag() != null && geb.laag().rMax() > 0 ? geb.laag().cz() : (geb.minZ() + geb.maxZ()) / 2;
            kandidaten.add(new BlockPos(cx, y, cz));
        }
        for (BlockPos b : kandidaten) {
            if (b == null) {
                continue;
            }
            for (int dy : new int[] {0, 1, 2, 4, -2, 8}) {
                Gebieden.Gebied x = Gebieden.op(b.getX() + 0.5, b.getY() + dy, b.getZ() + 0.5);
                if (x != null && x.id().equals(g)) {
                    return b.above(dy);
                }
            }
        }
        return null;
    }

    private void verslagen(VadsrijkMob mob) {
        Haken.Verslagen v = new Haken.Verslagen(mob, Set.of(p));
        for (var h : Haken.VERSLAGEN) {
            try {
                h.accept(v);
            } catch (RuntimeException ex) {
                if (bezig != null) {
                    bezig.problemen.add("VERSLAGEN-luisteraar gooit: " + ex);
                }
            }
        }
    }

    private void zetLevel(@Nullable String klasse, int lv) {
        lv = Math.min(CAP, lv);
        if (klasse != null) {
            SpelerData d = Spelers.data(p);
            if (d.level(klasse) < lv) {
                d.xp.put(klasse, Levels.totaalVoor(lv));
            }
            if (Spelers.level(p) < lv) {
                Spelers.zetGeneriek(p, lv);
            }
            Spelers.sync(p);
            Questlog.kijkLive(p);
            return;
        }
        if (Spelers.level(p) < lv) {
            Spelers.zetLevel(p, lv);
        }
        Questlog.kijkLive(p);
    }

    /** The tracker must say what and where for this step (and point somewhere, except "anywhere" steps). */
    private void trackerCheck(Quest q, int i, Uitslag u) {
        try {
            QuestStap s = QuestStap.van(p, q);
            for (Component c : new Component[] {s.wat(), s.waar()}) {
                for (String k : sleutels(c)) {
                    tekstCheck(k, u, "tracker " + u.stap);
                }
            }
            Quest.Doel d = q.doelen().get(i);
            boolean overal = d.type() == Quest.Doel.Type.LEVEL || d.type() == Quest.Doel.Type.MELD && d.plek().isEmpty()
                    || d.doel().equals("*") && d.plek().isEmpty() || d.type() == Quest.Doel.Type.VERZAMEL && (d.bron().isEmpty() || d.bron().equals("*"))
                    || d.type() == Quest.Doel.Type.GEHEIM && !d.doel().startsWith("poi:") && d.plek().isEmpty();
            if (s.doel() == null && !overal) {
                u.problemen.add(u.stap + ": de tracker wijst nergens heen (geen doelplek)");
            }
            if (!d.plek().isEmpty()) {
                BlockPos b = QuestPlek.pos(npcLevel, d.plek());
                if (b == null) {
                    u.problemen.add(u.stap + ": tracker-plek '" + d.plek() + "' is onbekend (QuestPlek)");
                } else {
                    doel(q, "plek", d.plek(), b);
                }
            }
        } catch (RuntimeException e) {
            u.problemen.add(u.stap + ": tracker gooit " + e);
        }
    }

    // ---- zone flags + airship gates ------------------------------------------------------------------------------------------------

    /** zone -> [flag that the end of the zone sets, the airship line to the NEXT zone]. */
    static final Map<Integer, String[]> ZONES = Map.of(
            1, new String[] {"zone1_klaar", "kandijkom"},
            2, new String[] {"zone2_klaar", "schemerwortel"},
            3, new String[] {"zone3_klaar", "lampionkust"},
            4, new String[] {"zone4_klaar", "hoogvads"},
            5, new String[] {"zone5_klaar", "zijdeland"},
            6, new String[] {"zone6_klaar", ""});

    private void zoneVlaggen(List<Quest> lijst, Set<Integer> gemeld) {
        for (int z = 1; z <= 6; z++) {
            if (gemeld.contains(z)) {
                continue;
            }
            boolean heeft = false, klaar = true;
            for (Quest q : lijst) {
                if (q.hoofd() && ZoneBoeken.zoneVan(q.id()) == z) {
                    heeft = true;
                    klaar &= Questlog.klaar(p, q.id());
                }
            }
            if (!heeft || !klaar) {
                continue;
            }
            gemeld.add(z);
            String[] v = ZONES.get(z);
            boolean vlag = Questlog.vlag(p, v[0]);
            r.algemeen.add("zone " + z + " hoofdverhaal klaar: vlag " + v[0] + " = " + vlag + (vlag ? "" : "  << FOUT: de vlag is niet gezet"));
            if (z == 5 && Quests.alle().keySet().stream().anyMatch(id -> ZoneBoeken.zoneVan(id) == 6)) {
                // (zone 6: line E to the Zijdeland opens after the great swim, u-z6verhaal Z6Verhaal.VLAG_LIJN_E)
                spoel();   // (a quiet moment after the last zone-5 quest: the great swim plays to its end)
                boolean e = Questlog.vlag(p, "z6_lijn_e_open");
                r.algemeen.add("  vlag z6_lijn_e_open (lijn E naar het Zijdeland) = " + e + (e ? "" : "  << FOUT: zone 6 blijft dicht"));
            }
            if (!v[1].isEmpty()) {
                // the next zone's first main quest's level (the robot would grind to it): the line must be open then
                int lv = Spelers.level(p);
                for (Quest q : Quests.alle().values()) {
                    if (q.hoofd() && ZoneBoeken.zoneVan(q.id()) == z + 1) {
                        lv = Math.max(lv, q.lv());
                        break;
                    }
                }
                int oud = Spelers.level(p);
                if (oud < lv) {
                    Spelers.zetGeneriek(p, lv);
                }
                String slot = QuestMotor.reisSlot(p, v[1]);
                r.algemeen.add("  luchtschip naar " + v[1] + " (lv " + Spelers.level(p) + "): " + (slot == null ? "open" : "DICHT: " + slot + "  << FOUT"));
                boolean zichtbaar = QuestMotor.reisDoelen(p, "luchtschipper").contains(v[1]) || QuestMotor.reisDoelen(p, "kapitein").contains(v[1])
                        || QuestMotor.reisDoelen(p, "z2_kapitein").contains(v[1]) || QuestMotor.reisDoelen(p, "z3_kapitein").contains(v[1])
                        || QuestMotor.reisDoelen(p, "z5_havenmeester").contains(v[1]);
                if (!zichtbaar) {
                    r.algemeen.add("  (bestemming " + v[1] + " staat bij geen enkele kapitein in de dialoogkeuzes; alleen via reiskaart/eigen dok)");
                }
            }
        }
    }

    // ---- dialogue texts ------------------------------------------------------------------------------------------------------------

    private void dialoog(@Nullable String sleutel, CompoundTag data) {
        Uitslag u = bezig;
        if (u == null) {
            return;
        }
        List<String> keys = new ArrayList<>();
        data.getString("Tekst").ifPresent(keys::add);
        if (data.get("Paginas") instanceof ListTag l) {
            for (Tag t : l) {
                if (t instanceof CompoundTag pg) {
                    pg.getString("Tekst").ifPresent(keys::add);
                    pg.getString("Naam").filter(s -> !s.isEmpty()).ifPresent(keys::add);
                }
            }
        }
        if (data.get("Opties") instanceof ListTag l) {
            for (Tag t : l) {
                if (t instanceof CompoundTag o) {
                    o.getString("Tekst").ifPresent(keys::add);
                }
            }
        }
        if (keys.isEmpty()) {
            u.problemen.add(u.stap + ": een gesprek zonder tekst (" + sleutel + ")");
        }
        for (String k : keys) {
            tekstCheck(k, u, "dialoog " + u.stap);
        }
    }

    private void tekstCheck(String k, Uitslag u, String waar) {
        if (k.isEmpty()) {
            u.problemen.add(waar + ": lege tekstsleutel");
            return;
        }
        if (!Language.getInstance().has(k)) {
            u.problemen.add(waar + ": tekst " + k + " ontbreekt (en_us)");
            return;
        }
        if (k.startsWith("vadsrijk.") || k.startsWith("guhs.")) {
            String t = nl().get(k);
            if (t == null) {
                u.problemen.add(waar + ": tekst " + k + " ontbreekt in nl_nl");
            } else if (t.isBlank()) {
                u.problemen.add(waar + ": tekst " + k + " is leeg in nl_nl");
            }
        }
    }

    private static synchronized Map<String, String> nl() {
        if (nlTekst == null) {
            Map<String, String> m = new HashMap<>();
            try (InputStream in = QuestRobot.class.getResourceAsStream("/assets/guhs/lang/nl_nl.json")) {
                if (in != null) {
                    JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (var e : o.entrySet()) {
                        m.put(e.getKey(), e.getValue().getAsString());
                    }
                }
            } catch (Exception e) {
                com.mojang.logging.LogUtils.getLogger().warn("questrobot: nl_nl.json not readable", e);
            }
            nlTekst = m;
        }
        return nlTekst;
    }

    static java.util.Set<String> sleutels(Component c) {
        java.util.Set<String> l = new java.util.HashSet<>();
        if (c.getContents() instanceof TranslatableContents t) {
            l.add(t.getKey());
            for (Object a : t.getArgs()) {
                if (a instanceof Component x) {
                    l.addAll(sleutels(x));
                }
            }
        }
        for (Component x : c.getSiblings()) {
            l.addAll(sleutels(x));
        }
        return l;
    }

    private void doel(Quest q, String wat, String id, BlockPos b) {
        doel(q.id(), wat, id, b);
    }

    private void doel(String quest, String wat, String id, BlockPos b) {
        for (Doel d : r.doelen) {
            if (d.wat().equals(wat) && d.id().equals(id)) {
                return;
            }
        }
        r.doelen.add(new Doel(quest, wat, id, b.immutable()));
    }

    private static void vast(Uitslag u, String stap, String waarom) {
        u.status = "vast";
        u.stap = stap;
        u.waarom = waarom;
    }
}
