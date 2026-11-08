package nl.juiced.guhs.feature.vadsrijk.zone4.verhaal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcHaken;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestStap;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * Zone 4 on top of zone 1's quest hooks (NpcHaken already counts defeats, areas, campfires, talk objectives):
 * <ul>
 *   <li>the story triggers: arriving in zone 4 (hq01), the sailboat (hq02), the Knuffeltij knuffels (a FIXED number, only while
 *       their quest runs: 6 on the beach, 4 big ones with a soft flash each, 2 in the wreck), the mist flash (hq04), the wreck +
 *       Kootje (hq05), the party + Moeder Wolkwal (hq06);</li>
 *   <li>the side-quest finds (kite parts, pearls, shells, bees, flowers, lantern hooks, Nevel's letters) and small games: the kite
 *       contest (s02), the lens (s08), the dance (s15), beach volleyball (s06, a click game), fishing (s03), the bottle (s16);</li>
 *   <li>named zone-4 bosses reported to quests ({@code meld(p, "z4_zeeslurf", "opgevrolijkt")}, {@code "z4_vliegervanger", "verslagen"}),
 *       also by entity id "vr_z4_..." holding the boss name;</li>
 *   <li>secrets: 8 knuffels, 6 flessenpost notes (lore of the Keizer), 4 treasures, 4 viewpoints.</li>
 * </ul>
 * Per-player memory in {@code extra("npc")}: maps z4_tijknuffels, z4_grote, z4_wrakknuffels, z4_knuffels, z4_flessenpost,
 * z4_schatten, z4_uitzichten, the side-quest finds (named after their teller), z4_gegeven (one-off moments).
 */
public final class Z4Haken {
    public static final String TIJ = "z4_tijknuffels", GROOT = "z4_grote", WRAK = "z4_wrakknuffels", KNUFFELS = "z4_knuffels", FLES = "z4_flessenpost",
            SCHATTEN = "z4_schatten", UITZICHTEN = "z4_uitzichten", GEGEVEN = "z4_gegeven";
    public static final String VLIEGERSPUL = "z4_vliegerspul", PARELS = "z4_parels", SCHELPEN = "z4_schelpen", BIJTJES = "z4_bijtjes", BLOEMEN = "z4_bloemen",
            LAMPIONHAKEN = "z4_lampionhaken", MISTBRIEVEN = "z4_mistbrieven";
    public static final String HQ01 = "z4_hq01_lampionkust", HQ02 = "z4_hq02_knuffeltij", HQ03 = "z4_hq03_waarvandaan", HQ04 = "z4_hq04_mist",
            HQ05 = "z4_hq05_vliegervanger", HQ06 = "z4_hq06_feest", HQ07 = "z4_hq07_opstijgpier";
    public static final String S02 = "z4_s02_wedstrijd", S03 = "z4_s03_vissen", S05 = "z4_s05_zeeslurf", S06 = "z4_s06_volleybal", S08 = "z4_s08_vuurtoren",
            S15 = "z4_s15_strandavond", S16 = "z4_s16_flessenpost";
    /** Boss name in an entity type id ("vr_z4_baas_zeeslurf") -> the quest event. */
    static final String[][] BAZEN = {{"zeeslurf", "z4_zeeslurf", "opgevrolijkt"}, {"vliegervanger", "z4_vliegervanger", "verslagen"},
            {"kootje", "z4_vliegervanger", "verslagen"}};
    /** Seconds a player has stood at a spot for a small game (kite / lens / dance). */
    private static final Map<UUID, int[]> BLIJF = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BAAS_START = new ConcurrentHashMap<>();
    /** Beach volleyball: player -> {my points, Smash's points, the side the ball goes (0 left, 1 middle, 2 right)}. */
    private static final Map<UUID, int[]> VOLLEY = new ConcurrentHashMap<>();
    static final int VOLLEY_OPTIE = 60;

    private Z4Haken() {
    }

    static void registreer() {
        Haken.VERSLAGEN.add(Z4Haken::verslagen);
        Haken.GEBIED_BETREDEN.add(Z4Haken::gebied);
        QuestMotor.KLIK_VOOR.add(Z4Haken::klikVoor);
        QuestMotor.ACTIES.put("z4_vlieg", (p, a) -> Z4Verhaal.vlieg(p));
        QuestMotor.ACTIES.put("z4_cliffhanger", (p, a) -> Z4Verhaal.cliffhanger(p));
        QuestStap.ROUTES.add(Z4Haken::route);
        nl.juiced.guhs.feature.vadsrijk.questrobot.QuestRobot.PLEK_HAKEN.add((p, sl) -> stap(p, sl, 0));   // U-QUESTROBOT: the spot triggers
        nl.juiced.guhs.feature.vadsrijk.questrobot.QuestRobot.SPOEL.add(Z4Verhaal::spoel);   // U-QUESTROBOT
        nl.juiced.guhs.feature.vadsrijk.questlopen.QlHaken.SCENE.add(Z4Verhaal::bezig);   // U-QUESTLOPEN
        Praat.luister(Z4Verhaal.SLEUTEL, Z4Haken::antwoord);
    }

    // ---- NPC clicks ----------------------------------------------------------------------------------------------------------------

    static boolean klikVoor(ServerPlayer p, RijkGuh g) {
        String id = g.npcId();
        if (id.equals("z4_zeilverhuur") && Questlog.actief(p, HQ02) && !doelKlaar(p, HQ02, 0)) {
            zeilboot(p, g);
            return true;
        }
        if (id.equals("z4_volleybal") && Questlog.actief(p, S06) && !doelKlaar(p, S06, 0)) {
            volleySlag(p, g, null);
            return true;
        }
        return false;
    }

    /** z4_hq02 step 0: your own sailboat (t-z4boot binds {@code z4_zeilboot_geef}: unlock/give the boat). */
    static void zeilboot(ServerPlayer p, @Nullable Entity zeiltje) {
        boolean echt = NpcRollen.doe("z4_zeilboot_geef", p, "");
        NpcApi.meld(p, "z4", "zeilboot");
        Questlog.zetVlag(p, "z4_zeilboot", true);
        Tips.toon(p, "vadsrijk.npc.tip.z4_boot");
        List<Praat.Regel> r = new ArrayList<>();
        r.add(new Praat.Regel(zeiltje, "", Z4Verhaal.Z + (echt ? "rol.zeilboot" : "rol.zeilboot.nog")));
        r.add(new Praat.Regel(null, "vadsrijk.npc.verteller", Z4Verhaal.Z + "zeilboot.klaar"));
        Praat.scene(p, Z4Verhaal.SLEUTEL, r);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS, 1f, 1.1f);
    }

    // ---- beach volleyball (z4_s06): Smash serves, you guess the side; three points wins ----------------------------------------------

    static void volleySlag(ServerPlayer p, @Nullable Entity smash, @Nullable Boolean vorige) {
        int[] s = VOLLEY.computeIfAbsent(p.getUUID(), k -> new int[3]);
        s[2] = p.getRandom().nextInt(3);
        List<Praat.Regel> r = new ArrayList<>();
        String naam = Z4Verhaal.Z + "volley.naam";
        if (vorige != null) {
            r.add(new Praat.Regel(smash, naam, Z4Verhaal.Z + (vorige ? "volley.punt" : "volley.mis")));
        } else {
            r.add(new Praat.Regel(smash, naam, Z4Verhaal.Z + "volley.uitleg"));
        }
        r.add(new Praat.Regel(smash, naam, Z4Verhaal.Z + "volley.serve", s[0], s[1]));
        r.add(new Praat.Regel(smash, naam, Z4Verhaal.Z + "volley.hint." + s[2]));
        Praat.scene(p, Z4Verhaal.SLEUTEL, r, new Praat.Optie(VOLLEY_OPTIE, Z4Verhaal.Z + "volley.links"),
                new Praat.Optie(VOLLEY_OPTIE + 1, Z4Verhaal.Z + "volley.midden"), new Praat.Optie(VOLLEY_OPTIE + 2, Z4Verhaal.Z + "volley.rechts"));
    }

    /** An answer in one of our dialogues (only the volleyball game has choices). */
    static void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (optie < VOLLEY_OPTIE || optie > VOLLEY_OPTIE + 2) {
            if (optie < 0) {
                Praat.sluit(p);
            }
            return;
        }
        volleyAntwoord(p, spreker, optie - VOLLEY_OPTIE);
    }

    /** (also the gametest) The player guessed side {@code kant} (0 left, 1 middle, 2 right). */
    static void volleyAntwoord(ServerPlayer p, @Nullable Entity smash, int kant) {
        int[] s = VOLLEY.get(p.getUUID());
        if (s == null) {
            return;
        }
        boolean raak = kant == s[2];
        s[raak ? 0 : 1]++;
        if (s[0] >= 3) {
            VOLLEY.remove(p.getUUID());
            NpcApi.meld(p, "z4", "volleybal");
            Praat.scene(p, Z4Verhaal.SLEUTEL, List.of(new Praat.Regel(smash, Z4Verhaal.Z + "volley.naam", Z4Verhaal.Z + "volley.gewonnen")));
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.4f);
            return;
        }
        volleySlag(p, smash, raak);
    }

    /** (tests) the side Smash will hit next, or -1. */
    static int volleyKant(ServerPlayer p) {
        int[] s = VOLLEY.get(p.getUUID());
        return s == null ? -1 : s[2];
    }

    // ---- kills, areas, fishing, the tracker -----------------------------------------------------------------------------------------

    static void verslagen(Haken.Verslagen v) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(v.mob().getType()).getPath();
        if (!type.startsWith("vr_z4_")) {
            return;
        }
        for (String[] b : BAZEN) {
            if (type.contains(b[0])) {
                Set<ServerPlayer> wie = new LinkedHashSet<>();
                for (ServerPlayer p : v.helpers()) {
                    wie.add(p);
                    wie.addAll(Partijen.ledenInBuurt(p, 48));
                }
                for (ServerPlayer p : wie) {
                    NpcApi.meld(p, b[1], b[2]);
                }
                return;
            }
        }
    }

    static void gebied(ServerPlayer p, String gebied) {
        if (gebied.startsWith("z4_")) {
            Tips.toon(p, "vadsrijk.npc.tip.z4_opvrolijken");
            if (Questlog.actief(p, HQ01)) {
                NpcApi.meld(p, "z4", "aangekomen");
            }
        }
    }

    /** A fish caught with a rod in zone 4 (NeoForge ItemFishedEvent; t-ambacht's Vissen may also report {@code meld(p,"z4_vis","gevangen")}). */
    static void gevist(ServerPlayer p) {
        if (!Vadsrijk.in(p) || !Z4VerhaalPlekken.zone4(p.getX(), p.getZ())) {
            return;
        }
        NpcApi.meld(p, "z4_vis", "gevangen");
        Quest q = Quests.van(S03);
        if (q != null && Questlog.actief(p, S03)) {
            p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + "vis.gevangen", Math.min(Questlog.waarde(p, q, 0), q.doelen().get(0).n()),
                    q.doelen().get(0).n()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The tracker: in zone 4 with a target outside it: first Windvaan's airship; outside with a target inside: the line to the coast. */
    static QuestStap route(ServerPlayer p, QuestStap s) {
        BlockPos d = s.doel();
        if (d == null || !Vadsrijk.in(p) || !Questlog.klaar(p, "hq05_klodderreus")) {
            return s;
        }
        boolean ik = Z4VerhaalPlekken.zone4(p.getX(), p.getZ()), doel = Z4VerhaalPlekken.zone4(d.getX(), d.getZ());
        if (ik == doel) {
            return s;
        }
        String piloot;
        if (ik) {
            piloot = "z4_luchthaven";
        } else if (nl.juiced.guhs.feature.vadsrijk.zone3.npc.Z3NpcPlekken.zone3(p.getX(), p.getZ())) {
            piloot = "z3_kapitein";
        } else {
            return s;   // (zone 1/2/camp: their own hops lead to the Boom first; from there the step above)
        }
        Component waar = Component.translatable(QuestStap.P + "luchtschip", s.waar(), QuestStap.naam(piloot), QuestStap.npcPlek(piloot));
        return new QuestStap(s.quest(), s.hoofd(), s.titel(), s.wat(), waar, QuestStap.npcPos(p, piloot), s.extra());
    }

    // ---- the ticker ------------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        long t = server.getTickCount();
        if (t % 10 != 3) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || !Z4VerhaalPlekken.zone4(p.getX(), p.getZ())) {
                continue;
            }
            if (RijkScenes.bezig(p) || Z4Verhaal.bezig(p)) {
                continue;
            }
            stap(p, sl, t);
        }
    }

    /** One check of everything zone 4 does for this player (also the gametest; t = server ticks, every 10). */
    static void stap(ServerPlayer p, ServerLevel sl, long t) {
        plekken(p);
        verhaal(p, sl, t);
        spelletjes(p, sl);
        geheimen(p, sl);
    }

    /** Zone-4 poi objectives with their own radius and height (the big knuffels count when you cheer them up, see verhaal). */
    static void plekken(ServerPlayer p) {
        for (Quest q : Questlog.actieve(p)) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if ((d.type() == Quest.Doel.Type.GEBIED || d.type() == Quest.Doel.Type.GEHEIM) && d.doel().startsWith("poi:z4_")
                        && !d.doel().startsWith("poi:z4_grote_knuffel_") && !Questlog.doelKlaar(p, q, i)) {
                    BlockPos b = Z4VerhaalPlekken.poi(d.doel().substring(4));
                    if (b != null && NpcHaken.bij(p, b, Z4VerhaalPlekken.POI_STRAAL, Z4VerhaalPlekken.POI_HOOGTE)) {
                        Questlog.tel(p, d.type(), x -> x == d, 1);
                    }
                }
            }
        }
    }

    public static boolean doelKlaar(ServerPlayer p, String quest, int i) {
        Quest q = Quests.van(quest);
        return q != null && i < q.doelen().size() && Questlog.doelKlaar(p, q, i);
    }

    static boolean bij(ServerPlayer p, String poi, double straal) {
        BlockPos b = Z4VerhaalPlekken.poi(poi);
        return b != null && NpcHaken.bij(p, b, straal, 8);
    }

    /** A one-off moment (remembered so it never comes twice). */
    static boolean eenmalig(ServerPlayer p, String id) {
        CompoundTag m = Questlog.map(p, GEGEVEN);
        if (m.getBooleanOr(id, false)) {
            return false;
        }
        m.putBoolean(id, true);
        return true;
    }

    static void verhaal(ServerPlayer p, ServerLevel sl, long t) {
        // hq01: standing in zone 4 counts as arrived (also without an area registered here)
        if (Questlog.actief(p, HQ01) && !doelKlaar(p, HQ01, 0)) {
            NpcApi.meld(p, "z4", "aangekomen");
        }
        // hq04: at the grey gate: the mist flash (once)
        if (Questlog.actief(p, HQ04) && doelKlaar(p, HQ04, 0) && !doelKlaar(p, HQ04, 1)) {
            if (!Spelers.data(p).gezien.contains("npc:z4_mist")) {
                Z4Verhaal.mist(p);
                return;
            }
            NpcApi.meld(p, "z4", "mistflash");   // (the scene was cut short)
        }
        // hq05: at the wreck: Kootje (the boss slice when bound, else his scene); then the 2 stolen knuffels in the hold
        if (Questlog.actief(p, HQ05) && doelKlaar(p, HQ05, 0) && !doelKlaar(p, HQ05, 1) && bij(p, "z4_wrak_ruim", 10)
                && t - BAAS_START.getOrDefault(p.getUUID(), -10000L) > 20 * 30) {
            BAAS_START.put(p.getUUID(), t);
            Z4Verhaal.wrak(p);
        }
        // s05: the Grauwe Zeeslurf in the deepest grotto
        if (Questlog.actief(p, S05) && !doelKlaar(p, S05, 0) && bij(p, "z4_grotje_1", 10)
                && t - BAAS_START.getOrDefault(p.getUUID(), -10000L) > 20 * 30) {
            BAAS_START.put(p.getUUID(), t);
            Z4Verhaal.zeeslurf(p);
        }
        // hq06: lanterns, kites and lemonade fetched: at the party spot the party starts (then Moeder Wolkwal)
        if (Questlog.actief(p, HQ06) && doelKlaar(p, HQ06, 0) && doelKlaar(p, HQ06, 1) && doelKlaar(p, HQ06, 2)) {
            if (!doelKlaar(p, HQ06, 3) && bij(p, "z4_feestplek", 10)) {
                Z4Verhaal.feest(p);
                return;
            }
            if (doelKlaar(p, HQ06, 3) && !doelKlaar(p, HQ06, 4)) {
                Z4Verhaal.wolkwal(p);   // (the party was cut short before the whale came)
                return;
            }
        }
        // s16: the bottle into the Kringstroom
        if (doelKlaar(p, S16, 0) && Questlog.actief(p, S16) && eenmalig(p, "flessenpost")) {
            neem(p, Z4VerhaalRegistry.item("flesje"));
            sl.sendParticles(p, ParticleTypes.SPLASH, true, true, p.getX(), p.getY() + 0.5, p.getZ(), 30, 1, 0.3, 1, 0.1);
            p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + "flessenpost.weg").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        }
        // the Knuffeltij knuffels (only inside the story)
        knuffeltij(p, sl);
    }

    /** Take one of this item from the player (a quest item that leaves: the bottle). */
    static void neem(ServerPlayer p, Item item) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(item)) {
                st.shrink(1);
                return;
            }
        }
    }

    // ---- the Knuffeltij (story only) -----------------------------------------------------------------------------------------------

    static void knuffeltij(ServerPlayer p, ServerLevel sl) {
        if (Questlog.actief(p, HQ02) && doelKlaar(p, HQ02, 1)) {
            zoek(p, sl, Z4VerhaalPlekken.TIJKNUFFEL, TIJ, new DustParticleOptions(0x9A93A2, 1.4f), 3);
        }
        if (Questlog.actief(p, HQ03)) {
            zoek(p, sl, Z4VerhaalPlekken.GROTE_KNUFFEL, GROOT, new DustParticleOptions(0x9A93A2, 2.4f), 5);
        }
        if (Questlog.actief(p, HQ05) && doelKlaar(p, HQ05, 1)) {
            zoek(p, sl, Z4VerhaalPlekken.WRAKKNUFFEL, WRAK, new DustParticleOptions(0xFF9AD0, 1.4f), 3);
        }
    }

    /** How many of the 12 Knuffeltij knuffels this player has cheered up. */
    public static int knuffeltijTeller(ServerPlayer p) {
        return Questlog.map(p, TIJ).size() + Questlog.map(p, GROOT).size() + Questlog.map(p, WRAK).size();
    }

    static void knuffelOpgevrolijkt(ServerPlayer p, ServerLevel sl, String soort, String id, BlockPos b) {
        Vec3 c = Vec3.atCenterOf(b);
        sl.sendParticles(p, ParticleTypes.HEART, false, false, c.x, c.y + 0.8, c.z, 10, 0.5, 0.4, 0.5, 0.05);
        sl.sendParticles(p, new DustParticleOptions(0xFF9AD0, 1.6f), false, false, c.x, c.y + 0.6, c.z, 30, 0.6, 0.5, 0.6, 0);
        p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
        int n = knuffeltijTeller(p);
        String Z = Z4Verhaal.Z;
        List<Praat.Regel> r = new ArrayList<>();
        String naam = Z + "tijknuffel." + id + ".naam";
        if (soort.equals(GROOT)) {
            // a soft flash of the grey east: three memories, the screen greyed a moment, then he turns pink
            RijkScenes.titel(p, Component.translatable(Z + "groot.titel").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    Component.translatable(naam), 50, 0x9A93A2);
            for (int i = 0; i < 3; i++) {
                r.add(new Praat.Regel(null, naam, Z + "tijknuffel." + id + ".verhaal." + i));
            }
            r.add(new Praat.Regel(null, "vadsrijk.npc.verteller", Z + "groot.roze"));
            sl.sendParticles(p, ParticleTypes.WHITE_ASH, true, true, p.getX(), p.getY() + 1.5, p.getZ(), 80, 4, 2, 4, 0);
            Z4Verhaal.geluid(p, SoundEvents.AMBIENT_BASALT_DELTAS_MOOD.value(), 0.8f, 0.7f);
            Quest q = Quests.van(HQ03);
            String poi = "poi:z4_grote_knuffel_" + id.substring(id.indexOf('_') + 1);
            if (q != null) {
                Questlog.tel(p, Quest.Doel.Type.GEBIED, d -> d.doel().equals(poi), 1);
            }
        } else {
            r.add(new Praat.Regel(null, naam, Z + "tijknuffel." + id + ".verhaal"));
        }
        r.add(new Praat.Regel(null, "vadsrijk.npc.verteller", Z + "knuffel.opgevrolijkt", n, Z4VerhaalPlekken.KNUFFELTIJ_TOTAAL));
        if (n >= Z4VerhaalPlekken.KNUFFELTIJ_TOTAAL) {
            r.add(new Praat.Regel(null, "vadsrijk.npc.verteller", Z + "knuffel.alle"));
            GuhAdvancements.grant(p, "vr_z4_knuffeltij_alle");
        }
        Praat.scene(p, Z4Verhaal.SLEUTEL, r);
        Tips.toon(p, "vadsrijk.npc.tip.z4_knuffel");
    }

    // ---- small games: the kite contest (s02), the lens (s08), the dance (s15) -------------------------------------------------------

    static void spelletjes(ServerPlayer p, ServerLevel sl) {
        int[] s = BLIJF.computeIfAbsent(p.getUUID(), k -> new int[3]);
        // the kite contest: 30 s at the pole with your kite in your bag (each call = 10 ticks: count half seconds)
        if (Questlog.actief(p, S02) && !doelKlaar(p, S02, 0)) {
            boolean daar = bij(p, "z4_wedstrijdpaal", 9);
            boolean vlieger = p.getInventory().contains(new ItemStack(Z4VerhaalRegistry.item("vlieger")));
            if (daar && !vlieger && s[0] == 0 && eenmalig(p, "geen_vlieger")) {
                p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + "vlieger.geen").withStyle(ChatFormatting.GRAY));
            }
            if (daar && vlieger) {
                if (s[0] == 0) {
                    p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + "vlieger.start").withStyle(ChatFormatting.AQUA));
                }
                s[0]++;
                sl.sendParticles(p, new DustParticleOptions(0xFF9AD0, 1.8f), true, true, p.getX() + 3, p.getY() + 12 + (s[0] % 6), p.getZ() - 4, 6, 0.6,
                        0.6, 0.6, 0);
                if (s[0] % 20 == 0 && s[0] < 60) {
                    p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + "vlieger.windstoot", s[0] / 2).withStyle(ChatFormatting.AQUA));
                    p.level().playSound(null, p.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.4f, 1.5f);
                }
                if (s[0] >= 60) {
                    s[0] = 0;
                    NpcApi.meld(p, "z4", "vliegerwedstrijd");
                    RijkScenes.titel(p, Component.translatable(Z4Verhaal.Z + "vlieger.gewonnen").withStyle(ChatFormatting.GOLD), Component.empty(), 60,
                            0xFFD54A);
                }
            } else if (s[0] > 0) {
                s[0] = 0;
                p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + "vlieger.weg").withStyle(ChatFormatting.GRAY));
            }
        }
        // the lens: 5 s at the top of the lighthouse
        if (Questlog.actief(p, S08) && doelKlaar(p, S08, 0) && !doelKlaar(p, S08, 1)) {
            if (bij(p, "z4_vuurtoren_top", 7)) {
                if (s[1]++ == 0) {
                    p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + "lens.bezig").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                }
                sl.sendParticles(p, ParticleTypes.WAX_ON, true, true, p.getX(), p.getY() + 1.5, p.getZ(), 4, 0.6, 0.4, 0.6, 0.02);
                if (s[1] >= 10) {
                    s[1] = 0;
                    NpcApi.meld(p, "z4", "lens");
                    p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + "lens.klaar").withStyle(ChatFormatting.GOLD));
                    p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.2f);
                }
            } else {
                s[1] = 0;
            }
        }
        // the dance: 10 s at the Strandfeest campfire
        if (Questlog.actief(p, S15) && !doelKlaar(p, S15, 0)) {
            if (bij(p, "z4_strandfeest_kampvuur", 7)) {
                if (s[2]++ == 0) {
                    p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + "dans.start").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
                }
                sl.sendParticles(p, ParticleTypes.NOTE, true, true, p.getX(), p.getY() + 2, p.getZ(), 2, 0.6, 0.3, 0.6, 0.5);
                if (s[2] >= 20) {
                    s[2] = 0;
                    NpcApi.meld(p, "z4", "dans");
                    p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + "dans.klaar").withStyle(ChatFormatting.LIGHT_PURPLE));
                    sl.sendParticles(p, ParticleTypes.HEART, true, true, p.getX(), p.getY() + 1.5, p.getZ(), 10, 1, 0.5, 1, 0.02);
                }
            } else {
                s[2] = 0;
            }
        }
    }

    // ---- finds: side-quest pickups and secrets --------------------------------------------------------------------------------------

    static void geheimen(ServerPlayer p, ServerLevel sl) {
        zoek(p, sl, Z4VerhaalPlekken.KNUFFEL, KNUFFELS, ParticleTypes.HEART, true);
        zoek(p, sl, Z4VerhaalPlekken.FLESSENPOST, FLES, ParticleTypes.GLOW, true);
        zoek(p, sl, Z4VerhaalPlekken.SCHAT, SCHATTEN, ParticleTypes.WAX_ON, true);
        zoek(p, sl, Z4VerhaalPlekken.UITZICHT, UITZICHTEN, ParticleTypes.END_ROD, true, Z4VerhaalPlekken.POI_STRAAL);
        zoek(p, sl, Z4VerhaalPlekken.VLIEGERSPUL, VLIEGERSPUL, ParticleTypes.CLOUD, Questlog.actief(p, "z4_s01_vlieger"));
        zoek(p, sl, Z4VerhaalPlekken.PARELS, PARELS, ParticleTypes.GLOW, Questlog.actief(p, "z4_s04_parels"));
        zoek(p, sl, Z4VerhaalPlekken.SCHELPEN, SCHELPEN, ParticleTypes.WAX_ON, Questlog.actief(p, "z4_s07_limonade"));
        zoek(p, sl, Z4VerhaalPlekken.BIJTJES, BIJTJES, ParticleTypes.WAX_ON, Questlog.actief(p, "z4_s09_bijen"));
        zoek(p, sl, Z4VerhaalPlekken.BLOEMEN, BLOEMEN, ParticleTypes.CHERRY_LEAVES, Questlog.actief(p, "z4_s10_kransen"));
        zoek(p, sl, Z4VerhaalPlekken.LAMPIONHAKEN, LAMPIONHAKEN, ParticleTypes.WAX_ON, Questlog.actief(p, "z4_s14_lampionnen"));
        zoek(p, sl, Z4VerhaalPlekken.MISTBRIEVEN, MISTBRIEVEN, ParticleTypes.ENCHANT, Questlog.actief(p, "z4_s12_mistbrieven"));
    }

    private static void zoek(ServerPlayer p, ServerLevel sl, Map<String, BlockPos> plekken, String soort, ParticleOptions glinster, boolean mag) {
        zoek(p, sl, plekken, soort, glinster, mag, Z4VerhaalPlekken.VIND_STRAAL);
    }

    private static void zoek(ServerPlayer p, ServerLevel sl, Map<String, BlockPos> plekken, String soort, ParticleOptions glinster, int straal) {
        zoek(p, sl, plekken, soort, glinster, true, straal);
    }

    private static void zoek(ServerPlayer p, ServerLevel sl, Map<String, BlockPos> plekken, String soort, ParticleOptions glinster, boolean mag, int straal) {
        if (!mag) {
            return;
        }
        CompoundTag gevonden = Questlog.map(p, soort);
        for (Map.Entry<String, BlockPos> e : plekken.entrySet()) {
            if (gevonden.getBooleanOr(e.getKey(), false)) {
                continue;
            }
            BlockPos b = e.getValue();
            double dx = p.getX() - (b.getX() + 0.5), dz = p.getZ() - (b.getZ() + 0.5);
            if (dx * dx + dz * dz < 16 * 16 && Math.abs(p.getY() - b.getY()) < 12) {
                sl.sendParticles(p, glinster, false, false, b.getX() + 0.5, b.getY() + 0.8, b.getZ() + 0.5, 2, 0.25, 0.3, 0.25, 0.01);
            }
            if (NpcHaken.bij(p, b, straal, Z4VerhaalPlekken.VIND_HOOGTE + 1)) {
                vind(p, sl, soort, e.getKey(), b);
                return;   // (one find per check: each opens its own little dialogue)
            }
        }
    }

    public static void vind(ServerPlayer p, ServerLevel sl, String soort, String id, BlockPos b) {
        CompoundTag gevonden = Questlog.map(p, soort);
        if (gevonden.getBooleanOr(id, false)) {
            return;
        }
        gevonden.putBoolean(id, true);
        int n = gevonden.size();
        Vec3 c = Vec3.atCenterOf(b);
        String Z = Z4Verhaal.Z;
        switch (soort) {
            case TIJ, GROOT, WRAK -> knuffelOpgevrolijkt(p, sl, soort, id, b);
            case KNUFFELS -> {
                sl.sendParticles(p, ParticleTypes.HEART, false, false, c.x, c.y + 0.8, c.z, 8, 0.4, 0.4, 0.4, 0.05);
                p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
                p.sendSystemMessage(Component.translatable(Z + "knuffel.gevonden", Component.translatable(Z + "knuffel." + id), n,
                        Z4VerhaalPlekken.KNUFFEL.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (n >= Z4VerhaalPlekken.KNUFFEL.size()) {
                    GuhAdvancements.grant(p, "vr_z4_knuffels_alle");
                }
            }
            case FLES -> {
                p.level().playSound(null, b, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1f, 1.1f);
                List<Praat.Regel> r = new ArrayList<>();
                r.add(new Praat.Regel(null, Z + "fles.titel." + id, Z + "fles." + id));
                r.add(new Praat.Regel(null, Z + "fles.naam", Z + "fles.teller", n, Z4VerhaalPlekken.FLESSENPOST.size()));
                if (n >= Z4VerhaalPlekken.FLESSENPOST.size()) {
                    r.add(new Praat.Regel(null, "vadsrijk.npc.verteller", Z + "fles.alle"));
                    GuhAdvancements.grant(p, "vr_z4_flessenpost_alle");
                }
                Praat.scene(p, Z4Verhaal.SLEUTEL, r);
            }
            case SCHATTEN -> {
                p.level().playSound(null, b, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.3f);
                RijkScenes.titel(p, Component.translatable(Z + "schat.titel").withStyle(ChatFormatting.GOLD), Component.translatable(Z + "schat." + id), 60,
                        0xFFD24A);
                nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType bt = stukVoor(p);
                List<ItemStack> buit = bt == null ? List.of()
                        : List.of(nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen.stack(nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen.rol(bt,
                                Math.max(1, Spelers.level(p)), id.equals("mist") ? nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid.EPISCH
                                        : nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid.ZELDZAAM, p.getRandom())));
                nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel.laatVallen(p, c.add(0, 0.8, 0),
                        new nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel.Resultaat(buit, 15000), 2);
                GuhAdvancements.grant(p, "vr_z4_schat_" + id);
            }
            case UITZICHTEN -> {
                p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
                RijkScenes.titel(p, Component.translatable(Z + "uitzicht.titel." + id).withStyle(ChatFormatting.AQUA), Component.translatable(Z + "uitzicht." + id),
                        90, 0xBFE8FF);
                p.sendSystemMessage(Component.translatable(Z + "uitzicht.teller", n, Z4VerhaalPlekken.UITZICHT.size()).withStyle(ChatFormatting.AQUA));
                if (n >= Z4VerhaalPlekken.UITZICHT.size()) {
                    GuhAdvancements.grant(p, "vr_z4_uitzichten_alle");
                }
            }
            case MISTBRIEVEN -> {
                p.level().playSound(null, b, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
                Praat.scene(p, Z4Verhaal.SLEUTEL, List.of(new Praat.Regel(null, Z + "mistbrief.naam", Z + "mistbrief.mistbrief_" + id.substring(id.lastIndexOf('_') + 1)),
                        new Praat.Regel(null, Z + "mistbrief.naam", Z + "vind.mistbrieven", n, Z4VerhaalPlekken.MISTBRIEVEN.size())));
            }
            default -> {   // the side-quest pickups
                Map<String, BlockPos> alle = switch (soort) {
                    case VLIEGERSPUL -> Z4VerhaalPlekken.VLIEGERSPUL;
                    case PARELS -> Z4VerhaalPlekken.PARELS;
                    case SCHELPEN -> Z4VerhaalPlekken.SCHELPEN;
                    case BIJTJES -> Z4VerhaalPlekken.BIJTJES;
                    case BLOEMEN -> Z4VerhaalPlekken.BLOEMEN;
                    default -> Z4VerhaalPlekken.LAMPIONHAKEN;
                };
                sl.sendParticles(p, soort.equals(LAMPIONHAKEN) ? new DustParticleOptions(0xFFD54A, 2f) : ParticleTypes.HAPPY_VILLAGER, false, false, c.x,
                        c.y + 0.8, c.z, 14, 0.4, 0.4, 0.4, 0.05);
                p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.6f);
                p.sendSystemMessage(Component.translatable(Z + "vind." + soort.substring(3), n, alle.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        Questlog.tel(p, Quest.Doel.Type.GEHEIM, d -> d.doel().equals("teller:" + soort), 1);
        Questlog.kijkLive(p);
    }

    /** A random piece the player can use: a weapon of their class or any armour/jewellery (as zone 2/3's treasures). */
    @Nullable
    static nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType stukVoor(ServerPlayer p) {
        String k = Spelers.data(p).klasse;
        List<nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType> l = new ArrayList<>();
        for (nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType t : nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes.alle()) {
            if (t.slot() != nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.WAPEN || k.equals(t.klasse())) {
                l.add(t);
            }
        }
        return l.isEmpty() ? null : l.get(p.getRandom().nextInt(l.size()));
    }

    /** The maps this slice keeps per player (dev reset / tests). */
    static final String[] MAPPEN = {TIJ, GROOT, WRAK, KNUFFELS, FLES, SCHATTEN, UITZICHTEN, GEGEVEN, VLIEGERSPUL, PARELS, SCHELPEN, BIJTJES, BLOEMEN,
            LAMPIONHAKEN, MISTBRIEVEN};

    /** (dev/tests) */
    static void vergeet(ServerPlayer p) {
        BLIJF.remove(p.getUUID());
        BAAS_START.remove(p.getUUID());
        VOLLEY.remove(p.getUUID());
        for (String m : MAPPEN) {
            Questlog.data(p).remove(m);
        }
    }
}
