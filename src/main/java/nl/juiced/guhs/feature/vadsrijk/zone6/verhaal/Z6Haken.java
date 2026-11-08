package nl.juiced.guhs.feature.vadsrijk.zone6.verhaal;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestStap;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.vadsrijk.wereld.Uitkijken;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * Zone 6 on top of zone 1's quest hooks (NpcHaken already counts talk objectives):
 * <ul>
 *   <li>the start: the great swim plays once z5_hq10 is handed in (when nothing else runs), then z6_hq01 starts by itself;</li>
 *   <li>NPC clicks before the normal quest dialogue ({@code QuestMotor.KLIK_VOOR}): the bultguh at Hobbel, haggling, soup for the
 *       refugees, Moeder Bron (asleep / cheering her up), the mirror where Meester Glans stood, the veiled lady Ringeling, and the
 *       hub NPCs of u-huizen / u-gilden while those slices are not in the tree;</li>
 *   <li>the spots, counted IN STORY ORDER (every earlier objective of that quest done) with their own radius: the chores, the
 *       caravan road, the beacons (they open the star puzzle), the telescope, the exam roofs, the cave crystals, the Spiegelzaal,
 *       the roofs chase, the mirage, the tower; the dungeon entrance (Z6VerhaalApi.KERKER or the fallback scene);</li>
 *   <li>secrets: 8 knuffels, 3 treasures, 4 lookouts; side-quest finds only while their quest runs;</li>
 *   <li>the tracker hop between the Zijdeland and the rest (QuestStap.ROUTES).</li>
 * </ul>
 * Per-player memory in {@code extra("npc")}: maps z6_bakens (Z6Puzzels), z6_knuffels, z6_schatten, the find kinds (z6_taart ...),
 * z6_soep, z6_gegeven (one-off moments), z6_brieven.
 */
public final class Z6Haken {
    public static final String KNUFFELS = "z6_knuffels", SCHATTEN = "z6_schatten", SOEP = "z6_soep", GEGEVEN = "z6_gegeven";
    public static final String HQ01 = "z6_hq01_land", HQ02 = "z6_hq02_karavaan", HQ03 = "z6_hq03_duinen", HQ04 = "z6_hq04_sultane",
            HQ05 = "z6_hq05_palmbaken", HQ06 = "z6_hq06_tapijt", HQ07 = "z6_hq07_zoutvlakte", HQ08 = "z6_hq08_sterrenwacht",
            HQ09 = "z6_hq09_spiegelzaal", HQ10 = "z6_hq10_moederoase";
    public static final List<String> HOOFD = List.of(HQ01, HQ02, HQ03, HQ04, HQ05, HQ06, HQ07, HQ08, HQ09, HQ10);
    /** Zone 5's last main quest: after it the great swim plays. */
    public static final String Z5_LAATSTE = "z5_hq10_stormrand";

    /** A puzzle at a spot: the meld it counts, the poi, the puzzle, the beacon it lights ("" = none). */
    record PuzzelPlek(String meld, String poi, String puzzel, String baken) {
    }

    static final List<PuzzelPlek> PUZZEL_PLEKKEN = List.of(
            new PuzzelPlek("z6_puzzel:oefen", "z6_kijker", "oefen", ""),
            new PuzzelPlek("z6_baken:palm", "z6_palmbaken", "palm", "palm"),
            new PuzzelPlek("z6_baken:zout", "z6_zoutbaken", "zout", "zout"),
            new PuzzelPlek("z6_baken:ruine", "z6_ruinebaken", "ruine", "ruine"),
            new PuzzelPlek("z6_baken:duin", "z6_duinbaken", "duin", "duin"),
            new PuzzelPlek("z6_baken:oase", "z6_oasebaken", "oase", "oase"),
            new PuzzelPlek("z6_puzzel:examen_1", "z6_examen_1", "examen_1", ""),
            new PuzzelPlek("z6_puzzel:examen_2", "z6_examen_2", "examen_2", ""),
            new PuzzelPlek("z6_puzzel:examen_3", "z6_examen_3", "examen_3", ""),
            new PuzzelPlek("z6_puzzel:examen_4", "z6_examen_4", "examen_4", ""),
            new PuzzelPlek("z6_puzzel:lied", "z6_grot_kristallen", "lied", ""));

    /** Which puzzle spot a player stands at now (so a puzzle opens once per visit, not every check). */
    private static final Map<UUID, String> BIJ = new HashMap<>();
    /** Players for whom the dungeon was started at the entrance this visit. */
    private static final Set<UUID> KERKER_GESTART = java.util.concurrent.ConcurrentHashMap.newKeySet(), WORMELIEN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private Z6Haken() {
    }

    static void registreer() {
        Haken.VERSLAGEN.add(Z6Haken::verslagen);
        Haken.GEBIED_BETREDEN.add(Z6Haken::gebied);
        QuestMotor.KLIK_VOOR.add(Z6Haken::klikVoor);
        QuestMotor.ACTIES.put("z6_vlieg", (p, a) -> Z6Verhaal.vlieg(p));
        nl.juiced.guhs.feature.vadsrijk.questlopen.QlHaken.SCENE.add(Z6Verhaal::bezig);   // U-QUESTLOPEN
        QuestMotor.ACTIES.put("z6_klaar", (p, a) -> Z6Verhaal.zone6Klaar(p));
        QuestMotor.ACTIES.put("z6_tapijt", (p, a) -> Z6Verhaal.tapijt(p));
        QuestMotor.ACTIES.put("z6_kwastjes", (p, a) -> Z6Verhaal.uiterlijk(p, "z6_kwastjes"));
        QuestMotor.ACTIES.put("z6_eerste_karavaan", (p, a) -> Z6Verhaal.uiterlijk(p, "z6_eerste_karavaan"));
        QuestMotor.ACTIES.put("z6_huisdier", (p, a) -> Z6Verhaal.huisdier(p, a));
        QuestMotor.ACTIES.put("z6_sjacheraar", (p, a) -> nl.juiced.guhs.feature.vadsrijk.economie.Cosmetica.geefTitel(p, "vr_sjacheraar"));
        QuestStap.ROUTES.add(Z6Haken::route);
    }

    // ---- quest helpers -------------------------------------------------------------------------------------------------------------

    static boolean actief(ServerPlayer p, String quest) {
        return Questlog.actief(p, quest);
    }

    static boolean doelKlaar(ServerPlayer p, String quest, int i) {
        Quest q = Quests.van(quest);
        return q != null && i < q.doelen().size() && Questlog.doelKlaar(p, q, i);
    }

    /** Does an active quest wait for this objective ("soort:waarde" or "poi:x") now, i.e. every EARLIER objective of it is done? */
    static boolean wachtOp(ServerPlayer p, String doel) {
        for (Quest q : Questlog.actieve(p)) {
            if (volgende(p, q, doel) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** The index of the open objective {@code doel} of q when every earlier one is done, else -1. */
    static int volgende(ServerPlayer p, Quest q, String doel) {
        for (int i = 0; i < q.doelen().size(); i++) {
            Quest.Doel d = q.doelen().get(i);
            boolean klaar = Questlog.doelKlaar(p, q, i);
            if (!klaar && d.doel().equals(doel)) {
                return i;
            }
            if (!klaar) {
                return -1;
            }
        }
        return -1;
    }

    static boolean eenmaligNog(ServerPlayer p, String id) {
        return !Questlog.map(p, GEGEVEN).getBooleanOr(id, false);
    }

    /** A one-off story moment, remembered so it never comes twice. */
    static boolean eenmalig(ServerPlayer p, String id) {
        CompoundTag m = Questlog.map(p, GEGEVEN);
        if (m.getBooleanOr(id, false)) {
            return false;
        }
        m.putBoolean(id, true);
        return true;
    }

    // ---- NPC clicks -----------------------------------------------------------------------------------------------------------------

    static boolean klikVoor(ServerPlayer p, RijkGuh g) {
        String id = g.npcId();
        if (id.startsWith("kk_poort_rozenkloof") || id.startsWith("kk_poort_saffraanpoort")) {
            return !Z6Verhaal.bestaat("huizen.Poorten") && binnenkort(p, g, "kwartiermeester");
        }
        if (id.startsWith("bond_bord_rozenkloof") || id.startsWith("bond_bord_saffraanpoort")) {
            return !Z6Verhaal.bestaat("gilden.BondNpcs") && binnenkort(p, g, "bondenbord");
        }
        if (!id.startsWith("z6_")) {
            return false;
        }
        switch (id) {
            case "z6_hobbel" -> {
                if (wachtOp(p, "z6:bultguh")) {   // hq02: your own bultguh
                    Quest q = Quests.van(HQ02);
                    List<String> sprekers = q == null ? null : q.praat().get(id);
                    if (sprekers != null) {
                        Praat.scene(p, Z6Verhaal.SLEUTEL, QuestMotor.regels(p, g, sprekers, q.sleutel() + ".praat." + id + "."));
                    }
                    Z6Verhaal.bultguh(p);
                    return true;
                }
            }
            case "z6_knoopje" -> {
                if (wachtOp(p, "z6:geweven")) {   // hq06: she weaves the flying carpet
                    Quest q = Quests.van(HQ06);
                    List<String> sprekers = q == null ? null : q.praat().get(id);
                    if (sprekers != null) {
                        Praat.scene(p, Z6Verhaal.SLEUTEL, QuestMotor.regels(p, g, sprekers, q.sleutel() + ".praat." + id + "."));
                    }
                    Z6Verhaal.tapijt(p);
                    NpcApi.meld(p, "z6", "geweven");
                    return true;
                }
            }
            case "z6_bron" -> {
                if (wachtOp(p, "z6_bron:opgevrolijkt")) {
                    if (!Z6Verhaal.bezig(p)) {
                        Z6Verhaal.speel(p, "bron", Z6VerhaalPlekken.OASE);
                    }
                    return true;
                }
                if (!Z6VerhaalApi.bronWakker(p)) {
                    Praat.scene(p, Z6Verhaal.SLEUTEL, List.of(new Praat.Regel(g, "", Z6Verhaal.Z + "bron.slaapt")));
                    return true;
                }
            }
            case "z6_glans" -> {
                if (Z6VerhaalApi.ontmaskerd(p)) {
                    Praat.scene(p, Z6Verhaal.SLEUTEL, List.of(new Praat.Regel(null, Z6Verhaal.Z + "glans.spiegel.naam", Z6Verhaal.Z + "glans.spiegel")));
                    return true;
                }
            }
            case "z6_ringeling" -> {
                boolean vriend = Questlog.vlag(p, Z6Verhaal.VLAG_RINGELING);
                List<Praat.Regel> r = new java.util.ArrayList<>();
                if (vriend) {
                    for (int i = 0; i < 4; i++) {
                        r.add(new Praat.Regel(g, Z6Verhaal.Z + "ringeling.naam", Z6Verhaal.Z + "ringeling.na." + i));
                    }
                } else {
                    r.add(new Praat.Regel(g, "", Z6Verhaal.Z + "ringeling.voor.0"));
                }
                Praat.scene(p, Z6Verhaal.SLEUTEL, r);
                return true;
            }
            case "z6_kardemom", "z6_pistache", "z6_lokum" -> {
                String puz = "afdingen_" + id.substring(3);
                if (wachtOp(p, "z6_puzzel:" + puz)) {
                    Z6Puzzels.open(p, puz, "", g);
                    return true;
                }
            }
            default -> {
                if (id.startsWith("z6_vluchteling_") && wachtOp(p, "z6_soep:kom")) {
                    soep(p, g);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean binnenkort(ServerPlayer p, RijkGuh g, String wat) {
        Praat.scene(p, Z6Verhaal.SLEUTEL, List.of(new Praat.Regel(g, "", Z6Verhaal.Z + "binnenkort." + wat)));
        return true;
    }

    /** z6_s15: a bowl of soup for one of the three refugees (once each). */
    static void soep(ServerPlayer p, RijkGuh g) {
        CompoundTag m = Questlog.map(p, SOEP);
        if (m.getBooleanOr(g.npcId(), false)) {
            Praat.scene(p, Z6Verhaal.SLEUTEL, List.of(new Praat.Regel(g, "", Z6Verhaal.Z + "soep.al")));
            return;
        }
        m.putBoolean(g.npcId(), true);
        NpcApi.meld(p, "z6_soep", "kom");
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(p, ParticleTypes.HEART, true, true, g.getX(), g.getY() + 1.6, g.getZ(), 4, 0.3, 0.2, 0.3, 0.02);
        }
        p.level().playSound(null, g.blockPosition(), SoundEvents.HONEY_DRINK.value(), SoundSource.PLAYERS, 1f, 1.2f);
        Praat.scene(p, Z6Verhaal.SLEUTEL, List.of(new Praat.Regel(g, "", Z6Verhaal.Z + "soep.kom", m.size())));
    }

    // ---- bosses + areas -----------------------------------------------------------------------------------------------------------

    /** Kills by entity id ("vr_z6_..."): Wormelien (side quest), the Verzonken Sterrenguh (the compass), Ringeling (the raid's end). */
    static void verslagen(Haken.Verslagen v) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(v.mob().getType()).getPath();
        if (!type.startsWith("vr_z6_")) {
            return;
        }
        Set<ServerPlayer> wie = new LinkedHashSet<>();
        for (ServerPlayer p : v.helpers()) {
            wie.add(p);
            wie.addAll(Partijen.ledenInBuurt(p, 48));
        }
        for (ServerPlayer p : wie) {
            if (type.contains("wormelien")) {
                NpcApi.meld(p, "z6_baas", "wormelien");
            } else if (type.contains("sterrenguh")) {
                if (actief(p, HQ08)) {
                    Z6Verhaal.kompas(p);
                }
            } else if (type.contains("ringeling")) {
                Z6VerhaalApi.ringelingOpgevrolijkt(p);
            }
        }
    }

    static void gebied(ServerPlayer p, String gebied) {
        if (Z6VerhaalPlekken.STEDEN.contains(gebied)) {
            Tips.toon(p, "vadsrijk.npc.tip.z6_veilig");
        }
    }

    /** The tracker: a target in the Zijdeland while you are elsewhere (or the other way round): first the airship. */
    static QuestStap route(ServerPlayer p, QuestStap s) {
        BlockPos d = s.doel();
        if (d == null || !Vadsrijk.in(p)) {
            return s;
        }
        boolean ik = Z6VerhaalPlekken.zone6(p.getX(), p.getY(), p.getZ()), doel = Z6VerhaalPlekken.zone6(d.getX(), d.getY(), d.getZ());
        if (ik == doel) {
            return s;
        }
        if (doel) {
            if (nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5VerhaalPlekken.zone5(p.getX(), p.getY(), p.getZ())) {
                // in Hoogvads: line E from the Staarthaven
                return new QuestStap(s.quest(), s.hoofd(), s.titel(), s.wat(), Component.translatable(QuestStap.PLEK + "poi.z6_staarthaven"),
                        Z6VerhaalPlekken.STAARTHAVEN, s.extra());
            }
            if (p.getZ() > 800) {
                return s;   // (the camp island: zone 1's own hop goes first)
            }
            String piloot = nl.juiced.guhs.feature.vadsrijk.zone3.npc.Z3NpcPlekken.zone3(p.getX(), p.getZ()) ? "z3_kapitein"
                    : p.getZ() < -1700 ? "z2_kapitein" : p.getX() > 1700 ? "z3_kapitein" : "luchtschipper";
            Component waar = Component.translatable(QuestStap.P + "luchtschip", s.waar(), QuestStap.naam(piloot), QuestStap.npcPlek(piloot));
            return new QuestStap(s.quest(), s.hoofd(), s.titel(), s.wat(), waar, QuestStap.npcPos(p, piloot), s.extra());
        }
        if (d.getX() > 2400 && d.getY() >= 150) {
            // back up to Hoogvads: line E from Rozenkloof
            return new QuestStap(s.quest(), s.hoofd(), s.titel(), s.wat(), Component.translatable(QuestStap.PLEK + "poi.z6_luchthaven"),
                    Z6VerhaalPlekken.AANKOMST, s.extra());
        }
        Component waar = Component.translatable(QuestStap.P + "luchtschip", s.waar(), QuestStap.naam("z6_zandloper"), QuestStap.npcPlek("z6_zandloper"));
        return new QuestStap(s.quest(), s.hoofd(), s.titel(), s.wat(), waar, QuestStap.npcPos(p, "z6_zandloper"), s.extra());
    }

    // ---- the ticker ----------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        long t = server.getTickCount();
        if (t % 10 != 7) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator()) {
                continue;
            }
            if (t % 100 == 7) {
                start(p);
                Z6Verhaal.beloningen(p);
                uitgesteld(p);
            }
            if (!Z6VerhaalPlekken.zone6(p.getX(), p.getY(), p.getZ()) && !NpcHaken.bij(p, Z6VerhaalPlekken.STAARTHAVEN, 20, 20)) {
                BIJ.remove(p.getUUID());
                continue;   // (everyone outside the Zijdeland skips all of this)
            }
            if (RijkScenes.bezig(p) || Z6Verhaal.bezig(p)) {
                continue;
            }
            stap(p, sl);
        }
    }

    /** z6_hq01 starts by itself once line E is open (the swim seen). */
    static void start(ServerPlayer p) {
        Quest q = Quests.van(HQ01);
        if (q != null && Questlog.vlag(p, Z6Verhaal.VLAG_LIJN_E) && Questlog.beschikbaar(p, q)) {
            Questlog.neemAan(p, q);
        }
    }

    /** Films that wait for a quiet moment: the great swim after zone 5, Ringeling after the raid. */
    static void uitgesteld(ServerPlayer p) {
        if (RijkScenes.bezig(p) || Z6Verhaal.bezig(p) || Praat.bezig(p)) {
            return;
        }
        if (Questlog.klaar(p, Z5_LAATSTE) && !Questlog.vlag(p, Z6Verhaal.VLAG_LIJN_E)) {
            Z6Verhaal.wolkwaltocht(p);
        } else if (Questlog.vlag(p, Z6Verhaal.VLAG_RINGELING) && !Spelers.data(p).gezien.contains("npc:z6_ringeling")) {
            Z6Verhaal.ringelingScene(p);
        }
    }

    /** One check of everything zone 6 does for this player (also called by the gametest). */
    static void stap(ServerPlayer p, ServerLevel sl) {
        plekken(p, sl);
        verhaal(p, sl);
        geheimen(p, sl);
    }

    /** Zone-6 poi objectives (GEBIED "poi:z6_*"), in story order, with their own radius and height, plus what happens there. */
    static void plekken(ServerPlayer p, ServerLevel sl) {
        for (Quest q : Questlog.actieve(p)) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (Questlog.doelKlaar(p, q, i)) {
                    continue;
                }
                if (d.type() == Quest.Doel.Type.GEBIED && d.doel().startsWith("poi:z6_")) {
                    String poi = d.doel().substring(4);
                    BlockPos b = Z6VerhaalPlekken.poi(poi);
                    if (b != null && NpcHaken.bij(p, b, Z6VerhaalPlekken.POI_STRAAL, Z6VerhaalPlekken.POI_HOOGTE)) {
                        Questlog.tel(p, d.type(), x -> x == d, 1);
                        bijPlek(p, sl, poi, b);
                    }
                }
                break;   // (only the first open objective of each quest: story order)
            }
        }
    }

    /** A poi objective was just reached. */
    static void bijPlek(ServerPlayer p, ServerLevel sl, String poi, BlockPos b) {
        Vec3 c = Vec3.atCenterOf(b);
        switch (poi) {
            case "z6_klus_dadels" -> Z6Verhaal.zeg(p, "klus.dadels", ChatFormatting.LIGHT_PURPLE);
            case "z6_klus_water" -> {
                Z6Verhaal.zeg(p, "klus.water", ChatFormatting.LIGHT_PURPLE);
                sl.sendParticles(p, ParticleTypes.SPLASH, true, true, c.x, c.y + 1, c.z, 30, 0.8, 0.3, 0.8, 0.1);
            }
            case "z6_klus_tapijten" -> Z6Verhaal.zeg(p, "klus.tapijten", ChatFormatting.LIGHT_PURPLE);
            case "z6_feest" -> {
                Z6Verhaal.zeg(p, "feest", ChatFormatting.LIGHT_PURPLE);
                sl.sendParticles(p, ParticleTypes.HEART, true, true, c.x, c.y + 1.5, c.z, 20, 1.5, 0.6, 1.5, 0.05);
            }
            case "z6_tuin_1", "z6_tuin_2", "z6_tuin_3" -> {
                Z6Verhaal.zeg(p, "tuin.water", ChatFormatting.LIGHT_PURPLE);
                sl.sendParticles(p, ParticleTypes.CHERRY_LEAVES, true, true, c.x, c.y + 3, c.z, 40, 2, 2, 2, 0.02);
            }
            case "z6_noria_1", "z6_noria_2", "z6_noria_3" -> {
                Z6Verhaal.zeg(p, "noria", ChatFormatting.AQUA);
                sl.sendParticles(p, ParticleTypes.SPLASH, true, true, c.x, c.y + 2, c.z, 40, 1.5, 1, 1.5, 0.1);
            }
            case "z6_waterval_1", "z6_waterval_2", "z6_waterval_3" -> Z6Verhaal.zeg(p, "waterval", ChatFormatting.AQUA);
            case "z6_ziedershuisje" -> Z6Verhaal.zeg(p, "ziedershuisje", ChatFormatting.WHITE);
            case "z6_mikakamp" -> {
                Z6Verhaal.brief(p);
                if (eenmalig(p, "grauwe_brief_item")) {
                    Questlog.geef(p, new ItemStack(Z6VerhaalRegistry.item("grauwe_brief")));
                }
            }
            case "z6_spiegelzaal" -> Z6Verhaal.speel(p, "ontmasker", null);
            case "z6_dak_3" -> Z6Verhaal.speel(p, "ontsnapt", null);
            case "z6_luchtspiegeling" -> Z6Verhaal.speel(p, "luchtspiegeling", null);
            case "z6_grot_schat" -> {
                Z6Verhaal.zeg(p, "grot.schat", ChatFormatting.GOLD);
                p.level().playSound(null, b, SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1f, 1f);
            }
            default -> {
            }
        }
    }

    /** Story moments that are not a plain GEBIED objective: melds at spots, puzzles, the dungeon. */
    static void verhaal(ServerPlayer p, ServerLevel sl) {
        // hq01: standing in the Zijdeland counts as arrived (the airship line, a /tp, a luxury flight)
        if (wachtOp(p, "z6:aangekomen") && Z6VerhaalPlekken.zone6(p.getX(), p.getY(), p.getZ())) {
            if (!Z6Verhaal.aankomstGezien(p)) {
                Z6Verhaal.aankomst(p);   // (its end counts the arrival)
                return;
            }
            NpcApi.meld(p, "z6", "aangekomen");
        }
        // hq01: the swim was cut short: again at the Staarthaven
        if (wachtOp(p, "z6:wolkwaltocht") && NpcHaken.bij(p, Z6VerhaalPlekken.STAARTHAVEN, 8, 8)) {
            Z6Verhaal.wolkwaltocht(p);
            return;
        }
        // hq03: Bobbeltje behind the dune, Wormelien in the distance
        if (wachtOp(p, "z6:bobbeltje") && NpcHaken.bij(p, Z6VerhaalPlekken.poi("z6_bobbeltje"), 6, 8)) {
            Z6Verhaal.zeg(p, "bobbeltje", ChatFormatting.LIGHT_PURPLE);
            NpcApi.meld(p, "z6", "bobbeltje");
        }
        if (wachtOp(p, "z6:wormelien") && NpcHaken.bij(p, Z6VerhaalPlekken.poi("z6_wormelien_zicht"), 6, 8)) {
            Z6Verhaal.zeg(p, "wormelien.zicht", ChatFormatting.LIGHT_PURPLE);
            BlockPos w = Z6VerhaalPlekken.poi("z6_wormelien");
            sl.sendParticles(p, new DustParticleOptions(0xF0A0B8, 3f), true, true, w.getX(), w.getY() + 2, w.getZ(), 120, 8, 3, 8, 0);
            NpcApi.meld(p, "z6", "wormelien");
        }
        // hq07: the Zoutkolos wakes once the Zoutbaken burns
        if (wachtOp(p, "z6:zoutkolos") && NpcHaken.bij(p, Z6VerhaalPlekken.BAKEN.get("zout"), 12, 10)) {
            Z6Verhaal.speel(p, "zoutkolos", null);
            return;
        }
        // hq08: the dungeon
        if (wachtOp(p, "z6_sterrenwacht:kompas") && NpcHaken.bij(p, Z6VerhaalPlekken.KERKER, 5, 6)) {
            if (KERKER_GESTART.add(p.getUUID())) {
                if (!Z6VerhaalApi.startKerker(p)) {
                    Z6Verhaal.speel(p, "kerker", null);   // (no dungeon in this tree: the fallback scene ends with the compass)
                    return;
                }
            }
        } else {
            KERKER_GESTART.remove(p.getUUID());
        }
        // side quest 13: Koningin Wormelien appears when you come to her dunes (u-z6wezens' field boss; once per visit)
        if (wachtOp(p, "z6_baas:wormelien") && NpcHaken.bij(p, Z6VerhaalPlekken.poi("z6_wormelien"), 24, 16)) {
            if (WORMELIEN.add(p.getUUID())) {
                Z6Verhaal.reflectie(p, "wormelien", "zone6.wezens.baas.Z6Bazen#start");
            }
        } else {
            WORMELIEN.remove(p.getUUID());
        }
        // hq09 / hq10: a cut-short scene again at its spot
        if (wachtOp(p, "z6:ontmaskerd") && NpcHaken.bij(p, Z6VerhaalPlekken.SPIEGELZAAL, 6, 6)) {
            Z6Verhaal.speel(p, "ontmasker", null);
            return;
        }
        if (wachtOp(p, "z6:ontsnapt") && NpcHaken.bij(p, Z6VerhaalPlekken.OOSTPOORT, 6, 8)) {
            Z6Verhaal.speel(p, "ontsnapt", null);
            return;
        }
        if (wachtOp(p, "z6:toren") && NpcHaken.bij(p, Z6VerhaalPlekken.TORENVOET, 8, 10)) {
            Z6Verhaal.speel(p, "toren", null);
            return;
        }
        // the puzzles at their spots (once per visit)
        String hier = null;
        for (PuzzelPlek pp : PUZZEL_PLEKKEN) {
            BlockPos b = Z6VerhaalPlekken.poi(pp.poi());
            if (b == null || !NpcHaken.bij(p, b, Z6VerhaalPlekken.POI_STRAAL, Z6VerhaalPlekken.POI_HOOGTE)) {
                continue;
            }
            if (!pp.baken().isEmpty() && Z6Puzzels.brandt(p, pp.baken())) {
                if (wachtOp(p, pp.meld())) {
                    NpcApi.meld(p, "z6_baken", pp.baken());   // (lit before the quest asked: counts now)
                }
                if (!pp.poi().equals(BIJ.get(p.getUUID()))) {
                    p.sendOverlayMessage(Component.translatable(Z6Verhaal.Z + "baken.al", Component.translatable(Z6Verhaal.Z + "bakens." + pp.baken()))
                            .withStyle(ChatFormatting.GOLD));
                }
                hier = pp.poi();
                continue;
            }
            if (!wachtOp(p, pp.meld())) {
                continue;
            }
            hier = pp.poi();
            if (!pp.poi().equals(BIJ.get(p.getUUID())) && !Praat.bezig(p)) {
                Z6Puzzels.open(p, pp.puzzel(), pp.baken(), null);
            }
        }
        if (hier == null) {
            BIJ.remove(p.getUUID());
        } else {
            BIJ.put(p.getUUID(), hier);
        }
        // the Grauwe brief can be found without the quest too (later)
        if (Questlog.klaar(p, HQ07) && eenmaligNog(p, "grauwe_brief") && NpcHaken.bij(p, Z6VerhaalPlekken.poi("z6_mikakamp"), 3, 3)) {
            Z6Verhaal.brief(p);
        }
        // the lookouts (kern panorama + the book)
        for (String id : Z6VerhaalPlekken.UITKIJK_IDS) {
            BlockPos b = Z6VerhaalPlekken.poi(id);
            if (b != null && NpcHaken.bij(p, b, 4, 4) && Spelers.data(p).gezien.add("vr_w_uitkijk_" + id)) {
                Uitkijken.panorama(p, new WereldPlekken.Uitkijk(id, b));
                if (Z6VerhaalPlekken.UITKIJK_IDS.stream().allMatch(u -> Spelers.data(p).gezien.contains("vr_w_uitkijk_" + u))) {
                    GuhAdvancements.grant(p, "vr_z6_uitkijken_alle");
                }
                return;
            }
        }
    }

    // ---- secrets + finds ------------------------------------------------------------------------------------------------------------

    static void geheimen(ServerPlayer p, ServerLevel sl) {
        zoek(p, sl, Z6VerhaalPlekken.KNUFFEL, KNUFFELS, ParticleTypes.HEART, true);
        zoek(p, sl, Z6VerhaalPlekken.SCHAT, SCHATTEN, ParticleTypes.WAX_ON, true);
        for (Map.Entry<String, Map<String, BlockPos>> e : Z6VerhaalPlekken.VONDST.entrySet()) {
            String quest = Z6VerhaalPlekken.VONDST_QUEST.get(e.getKey());
            if (actief(p, quest)) {
                zoek(p, sl, e.getValue(), e.getKey(), glinster(e.getKey()), true);
            }
        }
    }

    static ParticleOptions glinster(String soort) {
        return switch (soort) {
            case "z6_taart", "z6_post" -> new DustParticleOptions(0xFFD84A, 1.2f);
            case "z6_kwastjes" -> new DustParticleOptions(0xB080FF, 1.2f);
            case "z6_eendjes" -> ParticleTypes.SPLASH;
            case "z6_dadels" -> new DustParticleOptions(0x8A5A2A, 1.2f);
            case "z6_zoutkreeft" -> new DustParticleOptions(0xFF9AC0, 1.2f);
            case "z6_tabletten" -> new DustParticleOptions(0x60A0FF, 1.2f);
            default -> ParticleTypes.END_ROD;
        };
    }

    private static void zoek(ServerPlayer p, ServerLevel sl, Map<String, BlockPos> plekken, String soort, ParticleOptions glinster, boolean mag) {
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
            if (dx * dx + dz * dz < 14 * 14 && Math.abs(p.getY() - b.getY()) < 12) {
                sl.sendParticles(p, glinster, false, false, b.getX() + 0.5, b.getY() + 0.8, b.getZ() + 0.5, 2, 0.25, 0.3, 0.25, 0.01);
            }
            if (NpcHaken.bij(p, b, Z6VerhaalPlekken.VIND_STRAAL, Z6VerhaalPlekken.VIND_HOOGTE)) {
                vind(p, sl, soort, e.getKey(), b);
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
        String Z = Z6Verhaal.Z;
        switch (soort) {
            case KNUFFELS -> {
                sl.sendParticles(p, ParticleTypes.HEART, false, false, c.x, c.y + 0.8, c.z, 8, 0.4, 0.4, 0.4, 0.05);
                p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
                p.sendSystemMessage(Component.translatable(Z + "knuffel.gevonden", Component.translatable(Z + "knuffel." + id), n,
                        Z6VerhaalPlekken.KNUFFEL.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (n >= Z6VerhaalPlekken.KNUFFEL.size()) {
                    GuhAdvancements.grant(p, "vr_z6_knuffels_alle");
                }
            }
            case SCHATTEN -> {
                p.level().playSound(null, b, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.3f);
                RijkScenes.titel(p, Component.translatable(Z + "schat.titel").withStyle(ChatFormatting.GOLD), Component.translatable(Z + "schat." + id), 60,
                        0xFFD24A);
                nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType bt = stukVoor(p);
                List<ItemStack> buit = bt == null ? List.of()
                        : List.of(nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen.stack(nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen.rol(bt,
                                Math.max(1, Spelers.level(p)), id.equals("mesa") ? nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid.EPISCH
                                        : nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid.ZELDZAAM, p.getRandom())));
                nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel.laatVallen(p, c.add(0, 0.8, 0),
                        new nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel.Resultaat(buit, 20000), 2);
                GuhAdvancements.grant(p, "vr_z6_schat_" + id);
            }
            default -> {   // a side-quest find
                sl.sendParticles(p, glinster(soort), false, false, c.x, c.y + 0.8, c.z, 14, 0.4, 0.4, 0.4, 0.05);
                p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.6f);
                String item = switch (soort) {
                    case "z6_taart" -> "ingredient";
                    case "z6_post" -> null;
                    case "z6_kwastjes" -> "kwastje";
                    case "z6_eendjes" -> "badeendje";
                    case "z6_dadels" -> "dadels";
                    case "z6_zoutkreeft" -> "zoutkreeftje";
                    case "z6_tabletten" -> "tablet";
                    case "z6_cocons" -> "cocon";
                    default -> null;
                };
                if (item != null) {
                    Questlog.geef(p, new ItemStack(Z6VerhaalRegistry.item(item)));
                }
                p.sendSystemMessage(Component.translatable(Z + "vondst." + soort, n, Z6VerhaalPlekken.VONDST.get(soort).size())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                if (soort.equals("z6_tabletten")) {
                    Praat.scene(p, Z6Verhaal.SLEUTEL, List.of(new Praat.Regel(null, Z + "tablet.naam", Z + "tablet." + id)));
                }
            }
        }
        Questlog.tel(p, Quest.Doel.Type.GEHEIM, d -> d.doel().equals("teller:" + soort), 1);
        Questlog.kijkLive(p);
    }

    /** A random piece the player can use: a weapon of their class or any armour/jewellery (as the other zones' treasures). */
    @Nullable
    static nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType stukVoor(ServerPlayer p) {
        String k = Spelers.data(p).klasse;
        List<nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType> l = new java.util.ArrayList<>();
        for (nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType t : nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes.alle()) {
            if (t.slot() != nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.WAPEN || k.equals(t.klasse())) {
                l.add(t);
            }
        }
        return l.isEmpty() ? null : l.get(p.getRandom().nextInt(l.size()));
    }

    /** (dev/tests) */
    static void vergeet(ServerPlayer p) {
        for (String m : new String[] {KNUFFELS, SCHATTEN, SOEP, GEGEVEN, "z6_brieven"}) {
            Questlog.data(p).remove(m);
        }
        for (String m : Z6VerhaalPlekken.VONDST.keySet()) {
            Questlog.data(p).remove(m);
        }
        for (String id : Z6VerhaalPlekken.UITKIJK_IDS) {
            Spelers.data(p).gezien.remove("vr_w_uitkijk_" + id);
        }
        Spelers.data(p).gezien.removeIf(s -> s.startsWith("npc:z6_"));
        BIJ.remove(p.getUUID());
        KERKER_GESTART.remove(p.getUUID());
    }

    /** (tests) Forget which puzzle spot the player stood at. */
    static void vergeetBij(ServerPlayer p) {
        BIJ.remove(p.getUUID());
        KERKER_GESTART.remove(p.getUUID());
    }

    @Nullable
    static String bij(ServerPlayer p) {
        return BIJ.get(p.getUUID());
    }
}
