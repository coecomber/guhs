package nl.juiced.guhs.feature.vadsrijk.zone5.verhaal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * Zone 5 on top of zone 1's quest hooks (NpcHaken already counts talk objectives, escorts, areas):
 * <ul>
 *   <li>the start: z5_hq01 starts by itself once zone 4 is done (flag {@code zone4_klaar}) or at the first arrival;</li>
 *   <li>NPC clicks before the normal quest dialogue ({@code QuestMotor.KLIK_VOOR}): the interrogations (Z5Speurwerk), handing out
 *       soup to the refugees, the cardboard Chancellor after the unmasking;</li>
 *   <li>the spots (own radius + height: the city is high): the clues at the lectern / lockers / logbook, the blowhole (the finale,
 *       {@link Z5Finale#start}), the eye balcony (the lullaby), the traitor's desk (the Grauwe brief), the east pier (the
 *       cliffhanger), the side-quest spots;</li>
 *   <li>secrets: 8 knuffels, 3 treasures, 4 lookouts; side-quest finds (8 cloud berries, 3 balls of wool) only while their quest runs;</li>
 *   <li>the tracker hop between Hoogvads and the rest (QuestStap.ROUTES).</li>
 * </ul>
 * Per-player memory in {@code extra("npc")}: maps z5_bewijs (Z5Speurwerk), z5_knuffels, z5_schatten, z5_bessen, z5_bollen, z5_soep,
 * z5_gegeven (one-off story moments).
 */
public final class Z5Haken {
    public static final String KNUFFELS = "z5_knuffels", SCHATTEN = "z5_schatten", BESSEN = "z5_bessen", BOLLEN = "z5_bollen", SOEP = "z5_soep",
            GEGEVEN = "z5_gegeven";
    public static final String HQ01 = "z5_hq01_stad", HQ02 = "z5_hq02_verraad", HQ03 = "z5_hq03_mol", HQ04 = "z5_hq04_soep", HQ05 = "z5_hq05_logboek",
            HQ06 = "z5_hq06_verhoren", HQ07 = "z5_hq07_bewijsbord", HQ08 = "z5_hq08_wolkwaldroom", HQ09 = "z5_hq09_slaaplied", HQ10 = "z5_hq10_stormrand";
    public static final List<String> HOOFD = List.of(HQ01, HQ02, HQ03, HQ04, HQ05, HQ06, HQ07, HQ08, HQ09, HQ10);

    private Z5Haken() {
    }

    static void registreer() {
        Haken.VERSLAGEN.add(Z5Haken::verslagen);
        Haken.GEBIED_BETREDEN.add(Z5Haken::gebied);
        QuestMotor.KLIK_VOOR.add(Z5Haken::klikVoor);
        QuestMotor.ACTIES.put("z5_vlieg", (p, a) -> Z5Verhaal.vlieg(p));
        QuestMotor.ACTIES.put("z5_klaar", (p, a) -> Z5Verhaal.zone5Klaar(p));
        QuestMotor.ACTIES.put("z5_cliffhanger", (p, a) -> Z5Verhaal.cliffhanger(p));
        QuestMotor.ACTIES.put("z5_ontmasker", (p, a) -> Z5Verhaal.ontmasker(p));
        QuestStap.ROUTES.add(Z5Haken::route);
        nl.juiced.guhs.feature.vadsrijk.questrobot.QuestRobot.PLEK_HAKEN.add(Z5Haken::stap);   // U-QUESTROBOT: the spot triggers
        nl.juiced.guhs.feature.vadsrijk.questrobot.QuestRobot.SPOEL.add(Z5Verhaal::spoel);   // U-QUESTROBOT
        nl.juiced.guhs.feature.vadsrijk.questlopen.QlHaken.SCENE.add(Z5Verhaal::bezig);   // U-QUESTLOPEN
        nl.juiced.guhs.feature.vadsrijk.questlopen.QlHaken.SCENE.add(Z5Finale::bezig);   // U-QUESTLOPEN
    }

    // ---- quest helpers -------------------------------------------------------------------------------------------------------------

    static boolean actief(ServerPlayer p, String quest) {
        return Questlog.actief(p, quest);
    }

    static boolean doelKlaar(ServerPlayer p, String quest, int i) {
        Quest q = Quests.van(quest);
        return q != null && i < q.doelen().size() && Questlog.doelKlaar(p, q, i);
    }

    /**
     * Does an active quest wait for this MELD event now ("soort:waarde")? Only when every EARLIER objective of that quest is done,
     * except earlier interrogations (two interrogations of one quest may be done in any order).
     */
    static boolean wachtOp(ServerPlayer p, String doel) {
        for (Quest q : Questlog.actieve(p)) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (d.type() != Quest.Doel.Type.MELD || !d.doel().equals(doel) || Questlog.doelKlaar(p, q, i)) {
                    continue;
                }
                boolean vrij = true;
                for (int j = 0; j < i; j++) {
                    if (!Questlog.doelKlaar(p, q, j) && !q.doelen().get(j).doel().startsWith("z5_verhoor:")) {
                        vrij = false;
                    }
                }
                if (vrij) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Not given yet? (does not mark it) */
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
        if (!id.startsWith("z5_")) {
            return false;
        }
        if (id.equals("z5_inspecteur") && wachtOp(p, "z5:inspecteur")) {
            // hq02: the first meeting (he is also the hand-in NPC, so the shared "talk to" lines would be skipped)
            NpcApi.meld(p, "z5", "inspecteur");
            Quest q = Quests.van(HQ02);
            List<String> sprekers = q == null ? null : q.praat().get(id);
            if (sprekers != null) {
                Praat.scene(p, Z5Verhaal.SLEUTEL, QuestMotor.regels(p, g, sprekers, q.sleutel() + ".praat." + id + "."));
            }
            return true;
        }
        Z5Speurwerk.Verhoor v = Z5Speurwerk.open(p, id);
        if (v != null) {
            Z5Speurwerk.verhoor(p, g, v);
            return true;
        }
        if (id.startsWith("z5_vluchteling_") && wachtOp(p, "z5_soep:kom")) {
            soep(p, g);
            return true;
        }
        if (id.equals("z5_zachtpels") && Z5VerhaalApi.ontmaskerd(p)) {
            Praat.scene(p, Z5Verhaal.SLEUTEL, List.of(new Praat.Regel(null, "vadsrijk.npc.verteller", Z5Verhaal.Z + "zachtpels.karton")));
            return true;
        }
        if (id.equals("z5_mientje") && wachtOp(p, "z5:herinnering") && !Z5Verhaal.bezig(p)) {
            Z5Verhaal.herinnering(p);   // (the memory was cut short: again)
            return true;
        }
        return false;
    }

    /** z5_hq04: a bowl of soup for one of the four refugees in the queue (once each). */
    static void soep(ServerPlayer p, RijkGuh g) {
        CompoundTag m = Questlog.map(p, SOEP);
        if (m.getBooleanOr(g.npcId(), false)) {
            Praat.scene(p, Z5Verhaal.SLEUTEL, List.of(new Praat.Regel(g, "", Z5Verhaal.Z + "soep.al")));
            return;
        }
        m.putBoolean(g.npcId(), true);
        NpcApi.meld(p, "z5_soep", "kom");
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(p, ParticleTypes.HEART, true, true, g.getX(), g.getY() + 1.6, g.getZ(), 4, 0.3, 0.2, 0.3, 0.02);
        }
        p.level().playSound(null, g.blockPosition(), SoundEvents.HONEY_DRINK.value(), SoundSource.PLAYERS, 1f, 1.2f);
        Praat.scene(p, Z5Verhaal.SLEUTEL, List.of(new Praat.Regel(g, "", Z5Verhaal.Z + "soep.kom", m.size())));
    }

    // ---- bosses + areas -----------------------------------------------------------------------------------------------------------

    /** Finale kills seen by entity id ("vr_z5_..." with "zilverstaart" / "droom" in it): the whole party nearby counts. */
    static void verslagen(Haken.Verslagen v) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(v.mob().getType()).getPath();
        if (!type.startsWith("vr_z5_")) {
            return;
        }
        boolean spion = type.contains("zilverstaart"), droom = type.contains("droom");
        if (!spion && !droom) {
            return;
        }
        Set<ServerPlayer> wie = new LinkedHashSet<>();
        for (ServerPlayer p : v.voorQuests()) {
            wie.add(p);
            wie.addAll(Partijen.ledenInBuurt(p, 48));
        }
        for (ServerPlayer p : wie) {
            if (spion) {
                Z5Finale.spionVerslagen(p);
            } else {
                Z5Finale.droomOpgevrolijkt(p);
            }
        }
    }

    static void gebied(ServerPlayer p, String gebied) {
        if (gebied.startsWith("z5_") && !gebied.equals("z5_wolkwaldroom")) {
            Tips.toon(p, "vadsrijk.npc.tip.z5_veilig");
        }
    }

    /** The tracker: a target in Hoogvads while you are elsewhere (or the other way round): first the airship. */
    static QuestStap route(ServerPlayer p, QuestStap s) {
        BlockPos d = s.doel();
        if (d == null || !Vadsrijk.in(p)) {
            return s;
        }
        boolean ik = Z5VerhaalPlekken.zone5(p.getX(), p.getY(), p.getZ()), doel = Z5VerhaalPlekken.zone5(d.getX(), d.getY(), d.getZ());
        if (ik == doel) {
            return s;
        }
        if (doel && d.getY() >= Z5VerhaalPlekken.VANAF_Y && !Z5Verhaal.aankomstGezien(p)) {
            // (not up there yet: the Opstijgpier in zone 4)
            return new QuestStap(s.quest(), s.hoofd(), s.titel(), s.wat(), Component.translatable(QuestStap.PLEK + "poi.z5_opstijgpier"),
                    Z5VerhaalPlekken.OPSTIJGPIER, s.extra());
        }
        String piloot;
        if (ik) {
            piloot = "z5_havenmeester";
        } else if (p.getZ() > 800) {
            return s;   // (the camp island: zone 1's own hop goes first)
        } else {
            piloot = nl.juiced.guhs.feature.vadsrijk.zone3.npc.Z3NpcPlekken.zone3(p.getX(), p.getZ()) ? "z3_kapitein"
                    : p.getZ() < -1700 ? "z2_kapitein" : p.getX() > 1700 ? "z3_kapitein" : "luchtschipper";
        }
        Component waar = Component.translatable(QuestStap.P + "luchtschip", s.waar(), QuestStap.naam(piloot), QuestStap.npcPlek(piloot));
        return new QuestStap(s.quest(), s.hoofd(), s.titel(), s.wat(), waar, QuestStap.npcPos(p, piloot), s.extra());
    }

    // ---- the ticker ----------------------------------------------------------------------------------------------------------------

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
            if (p.isSpectator()) {
                continue;
            }
            if (t % 100 == 3) {
                start(p);
            }
            if (!Z5VerhaalPlekken.zone5(p.getX(), p.getY(), p.getZ()) && !NpcHaken.bij(p, Z5VerhaalPlekken.OPSTIJGPIER, 40, 40)) {
                continue;   // (everyone outside Hoogvads skips all of this)
            }
            if (RijkScenes.bezig(p) || Z5Verhaal.bezig(p)) {
                continue;
            }
            stap(p, sl);
        }
    }

    /** z5_hq01 starts by itself once zone 4 is done (t-z4verhaal's flag), so the tracker points to the Opstijgpier. */
    static void start(ServerPlayer p) {
        Quest q = Quests.van(HQ01);
        if (q != null && Questlog.vlag(p, Z5Verhaal.VLAG_ZONE4) && Questlog.beschikbaar(p, q)) {
            Questlog.neemAan(p, q);
        }
    }

    /** One check of everything zone 5 does for this player (also called by the gametest). */
    static void stap(ServerPlayer p, ServerLevel sl) {
        plekken(p, sl);
        verhaal(p, sl);
        geheimen(p, sl);
    }

    /** Zone-5 poi objectives with their own radius and height, plus what happens at some of them. */
    static void plekken(ServerPlayer p, ServerLevel sl) {
        for (Quest q : Questlog.actieve(p)) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (d.type() != Quest.Doel.Type.GEBIED || !d.doel().startsWith("poi:z5_") || Questlog.doelKlaar(p, q, i)) {
                    continue;
                }
                String poi = d.doel().substring(4);
                BlockPos b = Z5VerhaalPlekken.poi(poi);
                if (b != null && NpcHaken.bij(p, b, Z5VerhaalPlekken.POI_STRAAL, Z5VerhaalPlekken.POI_HOOGTE)) {
                    Questlog.tel(p, d.type(), x -> x == d, 1);
                    bijPlek(p, sl, poi, b);
                }
            }
        }
    }

    /** A poi objective was just reached. */
    static void bijPlek(ServerPlayer p, ServerLevel sl, String poi, BlockPos b) {
        Vec3 c = Vec3.atCenterOf(b);
        switch (poi) {
            case "z5_lessenaar" -> Z5Speurwerk.vind(p, "haren");
            case "z5_kastje" -> {
                Z5Speurwerk.vind(p, "bevelbrief");
                if (eenmalig(p, "bevelbrief")) {
                    Questlog.geef(p, new ItemStack(Z5VerhaalRegistry.item("bevelbrief")));
                }
            }
            case "z5_logboek" -> Z5Speurwerk.vind(p, "logboek");
            case "z5_spuitgat" -> {
                if (actief(p, HQ08)) {
                    Z5Finale.start(p);
                } else {
                    Z5Verhaal.zeg(p, "spuitgat.adem", ChatFormatting.AQUA);
                }
            }
            case "z5_spuitgat_noord", "z5_spuitgat_zuid" -> {
                Z5Verhaal.zeg(p, "spuitgat.adem", ChatFormatting.AQUA);
                sl.sendParticles(p, ParticleTypes.CLOUD, true, true, c.x, c.y + 2, c.z, 30, 0.8, 3, 0.8, 0.05);
            }
            case "z5_ooggaarde_balkon" -> {
                if (actief(p, HQ09)) {
                    Z5Verhaal.ontwaken(p);
                }
            }
            case "z5_kanselarij" -> Z5Verhaal.brief(p);
            case "z5_pier_oost" -> {
                if (actief(p, HQ10)) {
                    Z5Verhaal.cliffhanger(p);
                }
            }
            case "z5_sterrenwacht" -> {
                if (actief(p, "z5_s04_oog")) {
                    Z5Verhaal.zeg(p, "oog.gemeten", ChatFormatting.DARK_PURPLE);
                    NpcApi.meld(p, "z5", "oog_gemeten");
                }
            }
            case "z5_flap" -> {
                if (eenmalig(p, "flap")) {
                    Questlog.geef(p, new ItemStack(Z5VerhaalRegistry.item("flap")));
                    Z5Verhaal.zeg(p, "flap.gevonden", ChatFormatting.LIGHT_PURPLE);
                }
            }
            case "z5_bloesemboom_1", "z5_bloesemboom_2", "z5_bloesemboom_3", "z5_bloesemboom_4" -> {
                Z5Verhaal.zeg(p, "bloesem.water", ChatFormatting.LIGHT_PURPLE);
                sl.sendParticles(p, ParticleTypes.CHERRY_LEAVES, true, true, c.x, c.y + 3, c.z, 40, 2, 2, 2, 0.02);
                sl.sendParticles(p, ParticleTypes.SPLASH, true, true, c.x, c.y + 1, c.z, 20, 0.8, 0.3, 0.8, 0.1);
            }
            case "z5_roosvenster_noord", "z5_roosvenster_zuid", "z5_eerste_knuffel" -> {
                Z5Verhaal.zeg(p, "roosvenster", ChatFormatting.GOLD);
                sl.sendParticles(p, new DustParticleOptions(0xFFD24A, 1.6f), true, true, c.x, c.y + 2, c.z, 40, 1.5, 2, 1.5, 0);
            }
            default -> {
            }
        }
    }

    /** Story moments that are not a single spot. */
    static void verhaal(ServerPlayer p, ServerLevel sl) {
        // hq01: standing in Hoogvads counts as arrived (when the arrival came another way: a /tp, t-luchtschip's line)
        if (actief(p, HQ01) && !doelKlaar(p, HQ01, 0) && Z5VerhaalPlekken.zone5(p.getX(), p.getY(), p.getZ())) {
            NpcApi.meld(p, "z5", "aangekomen");
        }
        // hq04: the memory follows Mientje's interrogation
        if (wachtOp(p, "z5:herinnering") && doelKlaar(p, HQ04, 2) && !Spelers.data(p).gezien.contains("npc:z5_herinnering") && !Praat.bezig(p)) {
            Z5Verhaal.herinnering(p);
            return;
        }
        // hq07: the accusation counted but the board was closed another way: the unmasking still comes
        if (wachtOp(p, "z5:ontmaskerd") && !Praat.bezig(p)) {
            Z5Verhaal.ontmasker(p);
            return;
        }
        // hq08: a phase of the finale that is not running (left the arena, a fight that did not start): again at the blowhole
        if (actief(p, HQ08) && doelKlaar(p, HQ08, 0) && !Z5Finale.bezig(p) && NpcHaken.bij(p, Z5VerhaalPlekken.SPUITGAT, 8, 8)) {
            Z5Finale.start(p);
        }
        // hq09 / hq10: back at the spot after a cut-short scene
        if (wachtOp(p, "z5:wolkwal_wakker") && NpcHaken.bij(p, Z5VerhaalPlekken.BALKON_OOG, 6, 6)) {
            Z5Verhaal.ontwaken(p);
        }
        if (wachtOp(p, "z5:cliffhanger") && NpcHaken.bij(p, Z5VerhaalPlekken.POI.get("z5_pier_oost"), 6, 6)) {
            Z5Verhaal.cliffhanger(p);
        }
        // the Grauwe brief can be found without the quest too, once the traitor is unmasked
        if (Z5VerhaalApi.ontmaskerd(p) && eenmaligNog(p, "grauwe_brief") && NpcHaken.bij(p, Z5VerhaalPlekken.POI.get("z5_kanselarij"), 3, 3)) {
            Z5Verhaal.brief(p);
        }
        // the lookouts (kern panorama + the book)
        for (String id : Z5VerhaalPlekken.UITKIJK_IDS) {
            BlockPos b = Z5VerhaalPlekken.poi(id);
            if (b != null && NpcHaken.bij(p, b, 4, 4) && Spelers.data(p).gezien.add("vr_w_uitkijk_" + id)) {
                Uitkijken.panorama(p, new WereldPlekken.Uitkijk(id, b));
                if (Z5VerhaalPlekken.UITKIJK_IDS.stream().allMatch(u -> Spelers.data(p).gezien.contains("vr_w_uitkijk_" + u))) {
                    GuhAdvancements.grant(p, "vr_z5_uitkijken_alle");
                }
                return;
            }
        }
    }

    // ---- secrets -----------------------------------------------------------------------------------------------------------------------

    static void geheimen(ServerPlayer p, ServerLevel sl) {
        zoek(p, sl, Z5VerhaalPlekken.KNUFFEL, KNUFFELS, ParticleTypes.HEART, true);
        zoek(p, sl, Z5VerhaalPlekken.SCHAT, SCHATTEN, ParticleTypes.WAX_ON, true);
        zoek(p, sl, Z5VerhaalPlekken.BES, BESSEN, ParticleTypes.CHERRY_LEAVES, actief(p, "z5_s01_bessen"));
        zoek(p, sl, Z5VerhaalPlekken.BOL, BOLLEN, new DustParticleOptions(0xFF8AD0, 1.2f), actief(p, "z5_s10_wol"));
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
            if (NpcHaken.bij(p, b, Z5VerhaalPlekken.VIND_STRAAL, Z5VerhaalPlekken.VIND_HOOGTE)) {
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
        String Z = Z5Verhaal.Z;
        switch (soort) {
            case KNUFFELS -> {
                sl.sendParticles(p, ParticleTypes.HEART, false, false, c.x, c.y + 0.8, c.z, 8, 0.4, 0.4, 0.4, 0.05);
                p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
                p.sendSystemMessage(Component.translatable(Z + "knuffel.gevonden", Component.translatable(Z + "knuffel." + id), n,
                        Z5VerhaalPlekken.KNUFFEL.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (n >= Z5VerhaalPlekken.KNUFFEL.size()) {
                    GuhAdvancements.grant(p, "vr_z5_knuffels_alle");
                }
            }
            case SCHATTEN -> {
                p.level().playSound(null, b, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.3f);
                RijkScenes.titel(p, Component.translatable(Z + "schat.titel").withStyle(ChatFormatting.GOLD), Component.translatable(Z + "schat." + id), 60,
                        0xFFD24A);
                nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType bt = stukVoor(p);
                List<ItemStack> buit = bt == null ? List.of()
                        : List.of(nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen.stack(nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen.rol(bt,
                                Math.max(1, Spelers.level(p)), id.equals("staartvin") ? nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid.EPISCH
                                        : nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid.ZELDZAAM, p.getRandom())));
                nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel.laatVallen(p, c.add(0, 0.8, 0),
                        new nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel.Resultaat(buit, 15000), 2);
                GuhAdvancements.grant(p, "vr_z5_schat_" + id);
            }
            case BESSEN, BOLLEN -> {
                boolean bes = soort.equals(BESSEN);
                sl.sendParticles(p, bes ? ParticleTypes.CHERRY_LEAVES : ParticleTypes.HEART, false, false, c.x, c.y + 0.8, c.z, 12, 0.4, 0.4, 0.4, 0.05);
                p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.6f);
                Questlog.geef(p, new ItemStack(Z5VerhaalRegistry.item(bes ? "wolkbes" : "wolbol")));
                p.sendSystemMessage(Component.translatable(Z + (bes ? "bes" : "bol") + ".gevonden", n,
                        (bes ? Z5VerhaalPlekken.BES : Z5VerhaalPlekken.BOL).size()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            default -> {
            }
        }
        Questlog.tel(p, Quest.Doel.Type.GEHEIM, d -> d.doel().equals("teller:" + soort), 1);
        Questlog.kijkLive(p);
    }

    /** (dev/tests) */
    static void vergeet(ServerPlayer p) {
        for (String m : new String[] {KNUFFELS, SCHATTEN, BESSEN, BOLLEN, SOEP, GEGEVEN, "z5_brieven"}) {
            Questlog.data(p).remove(m);
        }
        Z5Finale.vergeet(p);
        for (String id : Z5VerhaalPlekken.UITKIJK_IDS) {
            Spelers.data(p).gezien.remove("vr_w_uitkijk_" + id);
        }
        Spelers.data(p).gezien.removeIf(s -> s.startsWith("npc:z5_"));
    }

    /** A random piece the player can use: a weapon of their class or any armour/jewellery (as zones 2 and 3's treasures). */
    @Nullable
    static nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType stukVoor(ServerPlayer p) {
        return nl.juiced.guhs.feature.vadsrijk.kern.loot.ZonePools.stuk(5, Spelers.data(p).klasse, p.getRandom());   // 1101-balans: only this zone's pool
    }
}
