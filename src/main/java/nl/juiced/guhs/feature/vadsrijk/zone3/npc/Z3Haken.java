package nl.juiced.guhs.feature.vadsrijk.zone3.npc;

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
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
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
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * Zone 3 on top of zone 1's quest hooks (NpcHaken already counts defeats, areas, campfires, talk objectives, escorts):
 * <ul>
 *   <li>the story triggers of the main line: the starter Pluimguh (hq03, {@code Rijdieren.kiesStarter}), the first ride and glide,
 *       the nest flashback (hq05), Piepje (hq06), the 7 lantern rings + the cages (hq10), flash F1 after Graaf Piepvel, the
 *       kettles (hq11), the cave lanterns (hq12), the egg (hq13), the ride + Vahoegnir + the finale (hq14);</li>
 *   <li>the heart splinters: 1 from zone 2, 4 here (nest, werf, cape pin, the Nestwachter);</li>
 *   <li>named zone-3 bosses reported to quests: {@code meld(p, "z3_<baas>", "verslagen")} (also seen here by entity id);</li>
 *   <li>secrets: 10 knuffels, 5 clues to the Mika-imker, 5 treasures, 8 boomvolk reliefs, the Grauwe brief; side-quest finds
 *       (6 Pluimkuikens, 4 sleeping lights) only while their quest runs.</li>
 * </ul>
 * Per-player memory in {@code extra("npc")}: maps z3_knuffels, z3_aanwijzingen, z3_schatten, z3_reliefs, z3_kuikens, z3_lichtjes,
 * z3_grotlantaarns, z3_gegeven (one-off story gifts).
 */
public final class Z3Haken {
    public static final String KNUFFELS = "z3_knuffels", AANWIJZINGEN = "z3_aanwijzingen", SCHATTEN = "z3_schatten", RELIEFS = "z3_reliefs",
            KUIKENS = "z3_kuikens", LICHTJES = "z3_lichtjes", GROT = "z3_grotlantaarns", GEGEVEN = "z3_gegeven";
    public static final String HQ01 = "z3_hq01_boom", HQ02 = "z3_hq02_bezoek", HQ03 = "z3_hq03_pluimstal", HQ04 = "z3_hq04_vlekken", HQ05 = "z3_hq05_nest",
            HQ06 = "z3_hq06_voetjes", HQ07 = "z3_hq07_hangnest", HQ08 = "z3_hq08_werf", HQ09 = "z3_hq09_spiraal", HQ10 = "z3_hq10_piepvel",
            HQ11 = "z3_hq11_wortel", HQ12 = "z3_hq12_duister", HQ13 = "z3_hq13_hart", HQ14 = "z3_hq14_vahoegnir", HQ15 = "z3_hq15_hoogvads";
    /** Boss name in an entity type id ("vr_z3_baas_piepvel") -> the quest event name ("z3_piepvel"). */
    static final String[][] BAZEN = {{"taleggio", "z3_taleggio"}, {"brokkel", "z3_brokkel"}, {"roquefort", "z3_roquefort"},
            {"wortelwurger", "z3_wortelwurger"}, {"bleu", "z3_bleu"}, {"piepvel", "z3_piepvel"}, {"imker", "z3_imker"},
            {"kruinroofvogel", "z3_kruinroofvogel"}, {"nestwachter", "z3_nestwachter"}};
    private static final Map<UUID, Long> VAHOEGNIR_START = new ConcurrentHashMap<>();

    private Z3Haken() {
    }

    static void registreer() {
        Haken.VERSLAGEN.add(Z3Haken::verslagen);
        Haken.GEBIED_BETREDEN.add(Z3Haken::gebied);
        QuestMotor.KLIK_VOOR.add(Z3Haken::klikVoor);
        QuestMotor.ACTIES.put("z3_vlieg", (p, a) -> Z3Verhaal.vlieg(p));
        QuestMotor.ACTIES.put("z3_bam", (p, a) -> Z3Verhaal.bam(p));
        QuestMotor.ACTIES.put("z3_starter", (p, a) -> starter(p));
        QuestMotor.ACTIES.put("z3_honinglift", (p, a) -> {
            Questlog.zetVlag(p, "z3_honinglift", true);
            NpcRollen.doe("z3_honinglift_open", p, "");
            p.sendSystemMessage(Component.translatable(Z3Verhaal.Z + "honinglift.open").withStyle(ChatFormatting.GOLD));
        });
        QuestMotor.ACTIES.put("z3_hq14", (p, a) -> {
            Quest q = Quests.van(HQ14);
            if (q != null && Questlog.status(p, q.id()) == 0) {
                Questlog.neemAan(p, q);
            }
            Z3Verhaal.rit(p);
        });
        QuestMotor.ACTIES.put("z3_rit", (p, a) -> Z3Verhaal.rit(p));
        QuestMotor.ACTIES.put("z3_cliffhanger", (p, a) -> Z3Verhaal.cliffhanger(p));
        nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren.STARTER_GEKOZEN.add((p, k) -> NpcApi.meld(p, "rijdier", "starter"));
        nl.juiced.guhs.feature.vadsrijk.npc.QuestStap.ROUTES.add(Z3Haken::route);
    }

    // ---- NPC clicks ----------------------------------------------------------------------------------------------------------------

    static boolean klikVoor(ServerPlayer p, RijkGuh g) {
        String id = g.npcId();
        if (id.equals("z3_veertje") && Questlog.actief(p, HQ03) && !doelKlaar(p, HQ03, 0)) {
            starter(p);   // (closed the choice without picking: open it again)
            return true;
        }
        if (id.equals("z3_nestwachter") && Questlog.actief(p, HQ14) && !doelKlaar(p, HQ14, 0) && !Z3Verhaal.bezig(p)) {
            Z3Verhaal.rit(p);   // (the ride was cut short: again from the Nestkamer)
            return true;
        }
        return false;
    }

    /** z3_hq03: the stalmeester's choice of the three starter Pluimguhs ({@code Rijdieren.kiesStarter}). */
    static void starter(ServerPlayer p) {
        if (nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren.heeftStarter(p)) {
            NpcApi.meld(p, "rijdier", "starter");   // (already picked one: the step counts at once)
            return;
        }
        if (!nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren.kiesStarter(p)) {
            NpcApi.meld(p, "rijdier", "starter");
        }
    }

    /** The bosses of zone 3 reported to quests (z3-bazen also calls NpcApi.meld itself; double counts are harmless: n = 1). */
    static void verslagen(Haken.Verslagen v) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(v.mob().getType()).getPath();
        if (!type.startsWith("vr_z3_")) {
            return;
        }
        String baas = null;
        for (String[] b : BAZEN) {
            if (type.contains(b[0])) {
                baas = b[1];
                break;
            }
        }
        if (baas == null) {
            return;
        }
        Set<ServerPlayer> wie = new LinkedHashSet<>();
        for (ServerPlayer p : v.helpers()) {
            wie.add(p);
            wie.addAll(Partijen.ledenInBuurt(p, 48));
        }
        for (ServerPlayer p : wie) {
            NpcApi.meld(p, baas, baas.equals("z3_nestwachter") ? "opgevrolijkt" : "verslagen");
        }
    }

    static void gebied(ServerPlayer p, String gebied) {
        switch (gebied) {
            case "z3_honingtak" -> NpcApi.meld(p, "z3", "aangekomen");
            case "z3_kruinzee", "z3_takkenbanen", "z3_wortelvelden" -> Tips.toon(p, "vadsrijk.npc.tip.z3_monsters");
            case "z3_wortelduister", "z3_hart" -> Tips.toon(p, "vadsrijk.npc.tip.z3_lantaarn");
            default -> {
            }
        }
    }

    /** The tracker: in zone 3 with a target outside it (or the other way round): first the airship at the mast / in Kandijkom. */
    static nl.juiced.guhs.feature.vadsrijk.npc.QuestStap route(ServerPlayer p, nl.juiced.guhs.feature.vadsrijk.npc.QuestStap s) {
        BlockPos d = s.doel();
        if (d == null || !Vadsrijk.in(p) || !Questlog.klaar(p, "hq05_klodderreus")) {
            return s;
        }
        boolean ik = Z3NpcPlekken.zone3(p.getX(), p.getZ()), doel = Z3NpcPlekken.zone3(d.getX(), d.getZ());
        if (ik == doel || nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.inKamp(p.getX(), p.getZ()) || p.getZ() > 800) {
            return s;   // (same side, or on the camp island: zone 1's hop goes first)
        }
        String piloot = ik ? "z3_kapitein" : nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.inZone2(p.getX(), p.getZ()) ? "z2_kapitein" : "luchtschipper";
        Component waar = Component.translatable(nl.juiced.guhs.feature.vadsrijk.npc.QuestStap.P + "luchtschip", s.waar(),
                nl.juiced.guhs.feature.vadsrijk.npc.QuestStap.naam(piloot), nl.juiced.guhs.feature.vadsrijk.npc.QuestStap.npcPlek(piloot));
        return new nl.juiced.guhs.feature.vadsrijk.npc.QuestStap(s.quest(), s.hoofd(), s.titel(), s.wat(), waar,
                nl.juiced.guhs.feature.vadsrijk.npc.QuestStap.npcPos(p, piloot), s.extra());
    }

    // ---- the ticker ------------------------------------------------------------------------------------------------------------------

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
            if (p.isSpectator() || !Z3NpcPlekken.zone3(p.getX(), p.getZ())) {
                continue;   // (zone 1/2 players skip all of this)
            }
            if (RijkScenes.bezig(p) || Z3Verhaal.bezig(p)) {
                continue;
            }
            stap(p, sl, t);
        }
    }

    /** One check of everything zone 3 does for this player (also called by the gametest). */
    static void stap(ServerPlayer p, ServerLevel sl, long t) {
        plekken(p);
        verhaal(p, sl, t);
        geheimen(p, sl);
    }

    /** Zone-3 poi objectives with their own radius and height (the tree is tall: zone 1's ticker uses a flat 10 blocks). */
    static void plekken(ServerPlayer p) {
        for (Quest q : Questlog.actieve(p)) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if ((d.type() == Quest.Doel.Type.GEBIED || d.type() == Quest.Doel.Type.GEHEIM) && d.doel().startsWith("poi:z3_") && !Questlog.doelKlaar(p, q, i)) {
                    BlockPos b = Z3NpcPlekken.poi(d.doel().substring(4));
                    if (b != null && NpcHaken.bij(p, b, Z3NpcPlekken.POI_STRAAL, Z3NpcPlekken.POI_HOOGTE)) {
                        Questlog.tel(p, d.type(), x -> x == d, 1);
                    }
                }
            }
        }
    }

    static boolean doelKlaar(ServerPlayer p, String quest, int i) {
        Quest q = Quests.van(quest);
        return q != null && i < q.doelen().size() && Questlog.doelKlaar(p, q, i);
    }

    private static boolean bij(ServerPlayer p, String poi, double straal) {
        BlockPos b = Z3NpcPlekken.poi(poi);
        return b != null && NpcHaken.bij(p, b, straal, 6);
    }

    /** Not given yet? (does not mark it) */
    static boolean eenmaligNog(ServerPlayer p, String id) {
        return !Questlog.map(p, GEGEVEN).getBooleanOr(id, false);
    }

    /** A one-off story gift (item + line), remembered so it never comes twice. */
    static boolean eenmalig(ServerPlayer p, String id) {
        CompoundTag m = Questlog.map(p, GEGEVEN);
        if (m.getBooleanOr(id, false)) {
            return false;
        }
        m.putBoolean(id, true);
        return true;
    }

    /** The heart splinters: zone 2 gave the first; here nest, werf, cape, wachter. */
    static void splinter(ServerPlayer p, String id) {
        if (!eenmalig(p, "splinter_" + id)) {
            return;
        }
        Questlog.geef(p, new ItemStack(Z3NpcRegistry.item("hartsplinter")));
        int n = 1 + (int) List.of("nest", "werf", "cape", "wachter").stream().filter(s -> Questlog.map(p, GEGEVEN).getBooleanOr("splinter_" + s, false)).count();
        RijkScenes.titel(p, Component.translatable(Z3Verhaal.Z + "splinter.titel", n).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                Component.translatable(Z3Verhaal.Z + "splinter." + id), 70, 0xFF8AD0);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.7f);
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(p, ParticleTypes.HEART, true, true, p.getX(), p.getY() + 1.5, p.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
        }
    }

    static void verhaal(ServerPlayer p, ServerLevel sl, long t) {
        // hq01: standing in Honingtak counts as arrived (also when the kern area is not y-aware yet)
        nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.Gebied hier = nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.op(p.getX(), p.getY(), p.getZ());
        if (Questlog.actief(p, HQ01) && !doelKlaar(p, HQ01, 0) && hier != null && hier.id().equals("z3_honingtak")) {
            NpcApi.meld(p, "z3", "aangekomen");
        }
        // hq03: on the Pluimguh for the first time, the glide, the title
        if (Questlog.actief(p, HQ03) && doelKlaar(p, HQ03, 0) && !doelKlaar(p, HQ03, 1)
                && p.getVehicle() instanceof nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity) {
            NpcApi.meld(p, "z3", "opgestegen");
            Tips.toon(p, "vadsrijk.npc.tip.z3_zweef");
        }
        if (Questlog.actief(p, HQ03) && doelKlaar(p, HQ03, 2) && !doelKlaar(p, HQ03, 3)
                && p.getVehicle() instanceof nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity) {
            BlockPos z = Z3NpcPlekken.poi("z3_zweefplatform");
            double dx = p.getX() - z.getX(), dz = p.getZ() - z.getZ();
            if (p.getY() < z.getY() - 8 && dx * dx + dz * dz < 90 * 90) {
                NpcApi.meld(p, "z3", "gezweefd");
            }
        }
        if (Questlog.actief(p, HQ03) && doelKlaar(p, HQ03, 3) && eenmalig(p, "eerste_rit")) {
            RijkScenes.titel(p, Component.translatable(Z3Verhaal.Z + "rit.titel").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable(Z3Verhaal.Z + "rit.onder"), 70, 0xFFD24A);
        }
        // hq04: a sprayed leaf patch is cleaned when you get there
        if (Questlog.actief(p, HQ04)) {
            for (int i = 1; i <= 3; i++) {
                if (doelKlaar(p, HQ04, i - 1) && eenmalig(p, "vlek_" + i)) {
                    Vec3 c = Vec3.atCenterOf(Z3NpcPlekken.poi("z3_vlek_" + i));
                    sl.sendParticles(p, ParticleTypes.CHERRY_LEAVES, true, true, c.x, c.y + 1, c.z, 60, 3, 1, 3, 0.02);
                    sl.sendParticles(p, ParticleTypes.SPLASH, true, true, c.x, c.y + 1, c.z, 40, 2, 0.5, 2, 0.1);
                    p.sendSystemMessage(Component.translatable(Z3Verhaal.Z + "vlek.schoon").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        }
        if (doelKlaar(p, HQ04, 4) && (Questlog.actief(p, HQ04) || Questlog.klaar(p, HQ04)) && eenmalig(p, "mika_bevel")) {
            Questlog.geef(p, new ItemStack(Z3NpcRegistry.item("mika_bevel")));
            Z3Verhaal.zeg(p, "bevel", ChatFormatting.GRAY);
        }
        // hq05: at the old nest: the flashback (once), then splinter 2
        if (Questlog.actief(p, HQ05) && doelKlaar(p, HQ05, 0) && !doelKlaar(p, HQ05, 1) && !Spelers.data(p).gezien.contains("npc:z3_nest")) {
            Z3Verhaal.nest(p);
            return;
        }
        if (Questlog.actief(p, HQ05) && doelKlaar(p, HQ05, 1) && !doelKlaar(p, HQ05, 2)) {
            BlockPos s = Z3NpcPlekken.poi("z3_nestsplinter");
            sl.sendParticles(p, new DustParticleOptions(0xFF8AD0, 1.2f), true, false, s.getX() + 0.5, s.getY() + 0.6, s.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0);
        }
        if (doelKlaar(p, HQ05, 2) && (Questlog.actief(p, HQ05) || Questlog.klaar(p, HQ05))) {
            splinter(p, "nest");
        }
        // hq06: Piepje in the Raattrossen climbs into your bag
        if (Questlog.actief(p, HQ06) && doelKlaar(p, HQ06, 1) && eenmalig(p, "piepje")) {
            Questlog.geef(p, new ItemStack(Z3NpcRegistry.item("piepje")));
            Praat.scene(p, Z3Verhaal.SLEUTEL, List.of(new Praat.Regel(null, "vadsrijk.npc.verteller", Z3Verhaal.Z + "piepje.0"),
                    new Praat.Regel(null, Z3Verhaal.Z + "piepje.naam", Z3Verhaal.Z + "piepje.1"),
                    new Praat.Regel(null, "vadsrijk.npc.verteller", Z3Verhaal.Z + "piepje.2")));
        }
        // hq08: the captain's cabin: the map, splinter 3, Bleu's letter, the first Grauwe brief
        if (doelKlaar(p, HQ08, 3) && (Questlog.actief(p, HQ08) || Questlog.klaar(p, HQ08))) {
            if (eenmalig(p, "kajuit")) {
                Questlog.geef(p, new ItemStack(Z3NpcRegistry.item("boomvolkkaart")));
                Questlog.geef(p, new ItemStack(Z3NpcRegistry.item("bleu_brief")));
                Questlog.geef(p, new ItemStack(Z3NpcRegistry.item("grauwe_brief")));
                Questlog.map(p, "z3_brieven").putBoolean("krabbel_1", true);
                GuhAdvancements.grant(p, "vr_z3_grauwe_brief");
            }
            splinter(p, "werf");
        }
        // hq10: the 7 lantern rings, the cages, the Nestlantaarn halfway; after the Count: flash F1; the cape pin
        if (Questlog.actief(p, HQ10)) {
            ringen(p, sl);
            if (doelKlaar(p, HQ10, 1) && !doelKlaar(p, HQ10, 2) && !Spelers.data(p).gezien.contains("npc:z3_flash_f1")) {
                if (Z3Koppelingen.f1Gezien(p)) {
                    Spelers.data(p).gezien.add("npc:z3_flash_f1");   // K-INT: z3-bazen's arm flash was the F1
                } else {
                    Z3Verhaal.flashF1(p);   // (fallback: this player was not in the Pelszaal when the arm came)
                    return;
                }
            }
            if (doelKlaar(p, HQ10, 1) && !doelKlaar(p, HQ10, 2) && Spelers.data(p).gezien.contains("npc:z3_flash_f1")) {
                NpcApi.meld(p, "z3", "flash_f1");   // (the scene was cut short)
            }
            if (doelKlaar(p, HQ10, 3)) {
                splinter(p, "cape");
            }
        }
        // hq11: the kettles (z3-wereld/bazen may report them too)
        if (Questlog.actief(p, HQ11)) {
            if (!doelKlaar(p, HQ11, 1) && bij(p, "z3_ketel_1", 5)) {
                ketel(p, sl, "een", "z3_ketel_1");
            }
            if (!doelKlaar(p, HQ11, 2) && bij(p, "z3_ketel_2", 5)) {
                ketel(p, sl, "twee", "z3_ketel_2");
            }
        }
        // hq12: the 4 cave lanterns around the gate of the Hart
        if (Questlog.actief(p, HQ12)) {
            if (doelKlaar(p, HQ12, 1) && eenmalig(p, "klopje")) {
                Z3Verhaal.zeg(p, "duister.klopje", ChatFormatting.LIGHT_PURPLE);
                Z3Verhaal.geluid(p, SoundEvents.WARDEN_HEARTBEAT, 1.5f, 1.9f);
            }
            CompoundTag m = Questlog.map(p, GROT);
            for (int i = 1; i <= 4; i++) {
                String id = "z3_grotlantaarn_" + i;
                if (!m.getBooleanOr(id, false) && bij(p, id, 3)) {
                    m.putBoolean(id, true);
                    Vec3 c = Vec3.atCenterOf(Z3NpcPlekken.poi(id));
                    sl.sendParticles(p, ParticleTypes.FLAME, true, true, c.x, c.y + 1, c.z, 20, 0.2, 0.3, 0.2, 0.01);
                    p.level().playSound(null, BlockPos.containing(c), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
                    NpcRollen.doe("z3_grotlantaarn", p, String.valueOf(i));
                    NpcApi.meld(p, "z3_grotlantaarn", "aan");
                }
            }
        }
        // hq13: the egg in the Nestkamer, once the three bosses are done; splinter 5 from the Nestwachter
        if (doelKlaar(p, HQ13, 1) && (Questlog.actief(p, HQ13) || Questlog.klaar(p, HQ13))) {
            splinter(p, "wachter");
        }
        if (Questlog.actief(p, HQ13) && doelKlaar(p, HQ13, 0) && doelKlaar(p, HQ13, 1) && doelKlaar(p, HQ13, 2) && !doelKlaar(p, HQ13, 3)
                && bij(p, "z3_nestkamer", 6) && !Spelers.data(p).gezien.contains("npc:z3_ei")) {
            Z3Verhaal.ei(p);
            return;
        }
        // hq14: back at the nest after a cut-short ride; Vahoegnir; the finale
        if (Questlog.actief(p, HQ14)) {
            if (!doelKlaar(p, HQ14, 0) && bij(p, "z3_kruintop", 20)) {
                NpcApi.meld(p, "z3", "ontsnapt");
            }
            if (doelKlaar(p, HQ14, 0) && !doelKlaar(p, HQ14, 1) && bij(p, "z3_kruintop", 30)
                    && t - VAHOEGNIR_START.getOrDefault(p.getUUID(), -10000L) > 20 * 30) {
                startVahoegnir(p);
            }
            if (doelKlaar(p, HQ14, 1) && !doelKlaar(p, HQ14, 2) && Z3Koppelingen.wachtOpTroon(p)) {
                return;   // K-INT: z3-bazen's ending + throne room run; the bloom follows on "troonzaal_klaar"
            }
            if (doelKlaar(p, HQ14, 1) && !doelKlaar(p, HQ14, 2) && !Spelers.data(p).gezien.contains("npc:z3_finale")) {
                Z3Verhaal.finale(p);
                return;
            }
            if (doelKlaar(p, HQ14, 1) && !doelKlaar(p, HQ14, 2)) {
                Z3Verhaal.bloei(p);   // (the finale was cut short)
            }
        }
        Z3Koppelingen.imkerTik(p);   // K-INT: the Mika-imker secret's lore scene
        // after hq10: the fingerprint as big as a door
        if (Questlog.klaar(p, HQ10) && bij(p, "z3_vingerafdruk", 8) && eenmalig(p, "vingerafdruk")) {
            Z3Verhaal.zeg(p, "vingerafdruk", ChatFormatting.DARK_PURPLE);
        }
    }

    /** At the old nest with the ride done: z3-bazen's deelbaas fight (bound), else the story goes straight on. */
    static void startVahoegnir(ServerPlayer p) {
        VAHOEGNIR_START.put(p.getUUID(), p.level().getServer().getTickCount() + 0L);
        if (NpcRollen.doe("z3_vahoegnir", p, "") || Z3Rollen.reflectie(p, "", "zone3.bazen.Vahoegnir#start", "zone3.bazen.vahoegnir.Vahoegnir#start",
                "zone3.bazen.Z3Bazen#vahoegnir")) {
            return;
        }
        // (no deelbaas in this tree: the dragon lands, and the splinters do the rest)
        Z3Verhaal.zeg(p, "vahoegnir.zonder", ChatFormatting.LIGHT_PURPLE);
        NpcApi.meld(p, "z3_vahoegnir", "opgevrolijkt");
    }

    /** z3_hq10: the next unlit ring when you stand at it; a cage opens and the mice run off; the Nestlantaarn at ring 4. */
    static void ringen(ServerPlayer p, ServerLevel sl) {
        Quest q = Quests.van(HQ10);
        if (q == null || Questlog.doelKlaar(p, q, 0)) {
            return;
        }
        int n = Questlog.waarde(p, q, 0) + 1;
        if (!bij(p, "z3_ring_" + n, 5)) {
            return;
        }
        Vec3 c = Vec3.atCenterOf(Z3NpcPlekken.poi("z3_ring_" + n));
        sl.sendParticles(p, ParticleTypes.FLAME, true, true, c.x, c.y + 1, c.z, 40, 4, 0.3, 4, 0.01);
        sl.sendParticles(p, new DustParticleOptions(0xFFD24A, 2.0f), true, true, c.x, c.y + 1.5, c.z, 80, 8, 0.4, 8, 0);
        sl.sendParticles(p, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BARS.defaultBlockState()), true, true, c.x + 2, c.y + 1, c.z, 20, 0.3,
                0.3, 0.3, 0.1);
        p.level().playSound(null, BlockPos.containing(c), SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1f, 1.4f);
        p.level().playSound(null, BlockPos.containing(c), SoundEvents.BAT_AMBIENT, SoundSource.PLAYERS, 1f, 2f);   // (piep piep)
        NpcRollen.doe("z3_ring_aan", p, String.valueOf(n));   // (z3-wereld: the ring's lantern paths light up for this player)
        NpcApi.meld(p, "z3_lantaarn", "ring");
        p.sendSystemMessage(Component.translatable(Z3Verhaal.Z + "ring.aan", n).withStyle(ChatFormatting.GOLD));
        if (n == 4 && eenmalig(p, "nestlantaarn")) {
            Questlog.geef(p, new ItemStack(Z3NpcRegistry.item("nestlantaarn")));
            Praat.scene(p, Z3Verhaal.SLEUTEL, List.of(new Praat.Regel(null, "vadsrijk.npc.naam.z3_knoest", Z3Verhaal.Z + "nestlantaarn.0"),
                    new Praat.Regel(null, "vadsrijk.npc.naam.z3_knoest", Z3Verhaal.Z + "nestlantaarn.1")));
            Questlog.zetVlag(p, "z3_nestlantaarn", true);
        }
    }

    static void ketel(ServerPlayer p, ServerLevel sl, String id, String poi) {
        Vec3 c = Vec3.atCenterOf(Z3NpcPlekken.poi(poi));
        sl.sendParticles(p, ParticleTypes.LARGE_SMOKE, true, true, c.x, c.y + 2, c.z, 40, 1.5, 1, 1.5, 0.02);
        sl.sendParticles(p, ParticleTypes.CHERRY_LEAVES, true, true, c.x, c.y + 2, c.z, 80, 10, 2, 10, 0.02);
        p.level().playSound(null, BlockPos.containing(c), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.2f, 0.8f);
        NpcRollen.doe("z3_ketel_uit", p, id);
        NpcApi.meld(p, "z3_ketel", id);
        p.sendSystemMessage(Component.translatable(Z3Verhaal.Z + "ketel.uit").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // ---- secrets -----------------------------------------------------------------------------------------------------------------------

    static void geheimen(ServerPlayer p, ServerLevel sl) {
        zoek(p, sl, Z3NpcPlekken.KNUFFEL, KNUFFELS, ParticleTypes.HEART, true);
        zoek(p, sl, Z3NpcPlekken.AANWIJZING, AANWIJZINGEN, ParticleTypes.ENCHANT, true);
        zoek(p, sl, Z3NpcPlekken.SCHAT, SCHATTEN, ParticleTypes.WAX_ON, true);
        zoek(p, sl, Z3NpcPlekken.RELIEF, RELIEFS, ParticleTypes.END_ROD, true);
        zoek(p, sl, Z3NpcPlekken.KUIKEN, KUIKENS, ParticleTypes.CHERRY_LEAVES, Questlog.actief(p, "z3_s01_kuikens"));
        zoek(p, sl, Z3NpcPlekken.LICHTJE, LICHTJES, ParticleTypes.GLOW, Questlog.actief(p, "z3_s04_lichtjes"));
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
            if (NpcHaken.bij(p, b, Z3NpcPlekken.VIND_STRAAL, Z3NpcPlekken.VIND_HOOGTE)) {
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
        String Z = Z3Verhaal.Z;
        switch (soort) {
            case KNUFFELS -> {
                sl.sendParticles(p, ParticleTypes.HEART, false, false, c.x, c.y + 0.8, c.z, 8, 0.4, 0.4, 0.4, 0.05);
                p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
                p.sendSystemMessage(Component.translatable(Z + "knuffel.gevonden", Component.translatable(Z + "knuffel." + id), n, Z3NpcPlekken.KNUFFEL.size())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                if (n >= Z3NpcPlekken.KNUFFEL.size()) {
                    GuhAdvancements.grant(p, "vr_z3_knuffels_alle");
                }
            }
            case AANWIJZINGEN -> {
                p.level().playSound(null, b, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
                Praat.scene(p, Z3Verhaal.SLEUTEL, List.of(new Praat.Regel(null, Z + "aanwijzing.naam", Z + "aanwijzing." + id),
                        new Praat.Regel(null, Z + "aanwijzing.naam", Z + "aanwijzing.teller", n, Z3NpcPlekken.AANWIJZING.size())));
                Quest q = Quests.van("z3_geheim_imker");
                if (q != null && Questlog.status(p, q.id()) == 0) {
                    Questlog.neemAan(p, q);
                }
                if (n >= Z3NpcPlekken.AANWIJZING.size()) {
                    GuhAdvancements.grant(p, "vr_z3_aanwijzingen_alle");
                    p.sendSystemMessage(Component.translatable(Z + "aanwijzing.alle").withStyle(ChatFormatting.DARK_PURPLE));
                    if (!NpcRollen.doe("z3_imker", p, "")) {
                        Z3Rollen.reflectie(p, "", "zone3.bazen.MikaImker#ontgrendel", "zone3.bazen.imker.MikaImker#ontgrendel");
                    }
                }
            }
            case SCHATTEN -> {
                p.level().playSound(null, b, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.3f);
                RijkScenes.titel(p, Component.translatable(Z + "schat.titel").withStyle(ChatFormatting.GOLD), Component.translatable(Z + "schat." + id), 60,
                        0xFFD24A);
                nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType bt = stukVoor(p);
                List<ItemStack> buit = bt == null ? List.of()
                        : List.of(nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen.stack(nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen.rol(bt,
                                Math.max(1, Spelers.level(p)), id.equals("onder_de_tak") ? nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid.EPISCH
                                        : nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid.ZELDZAAM, p.getRandom())));
                nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel.laatVallen(p, c.add(0, 0.8, 0),
                        new nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel.Resultaat(buit, 9000), 2);
                GuhAdvancements.grant(p, "vr_z3_schat_" + id);
            }
            case RELIEFS -> {
                p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
                List<Praat.Regel> r = new ArrayList<>();
                r.add(new Praat.Regel(null, Z + "relief.naam", Z + "relief." + id));
                r.add(new Praat.Regel(null, Z + "relief.naam", Z + "relief.teller", n, Z3NpcPlekken.RELIEF.size()));
                if (n >= Z3NpcPlekken.RELIEF.size()) {
                    r.add(new Praat.Regel(null, "vadsrijk.npc.verteller", Z + "relief.alle"));
                    GuhAdvancements.grant(p, "vr_z3_reliefs_alle");
                }
                Praat.scene(p, Z3Verhaal.SLEUTEL, r);
            }
            case KUIKENS, LICHTJES -> {
                sl.sendParticles(p, soort.equals(KUIKENS) ? ParticleTypes.CHERRY_LEAVES : ParticleTypes.GLOW, false, false, c.x, c.y + 0.8, c.z, 16, 0.4,
                        0.4, 0.4, 0.05);
                p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f,
                        1.6f);
                p.sendSystemMessage(Component.translatable(Z + (soort.equals(KUIKENS) ? "kuiken" : "lichtje") + ".gevonden", n,
                        (soort.equals(KUIKENS) ? Z3NpcPlekken.KUIKEN : Z3NpcPlekken.LICHTJE).size()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            default -> {
            }
        }
        Questlog.tel(p, Quest.Doel.Type.GEHEIM, d -> d.doel().equals("teller:" + soort), 1);
        Questlog.kijkLive(p);
    }

    /** A random piece the player can use: a weapon of their class or any armour/jewellery (as zone 2's treasures). */
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

    /** (dev/tests) */
    static void vergeet(ServerPlayer p) {
        VAHOEGNIR_START.remove(p.getUUID());
        for (String m : new String[] {KNUFFELS, AANWIJZINGEN, SCHATTEN, RELIEFS, KUIKENS, LICHTJES, GROT, GEGEVEN, "z3_brieven"}) {
            Questlog.data(p).remove(m);
        }
    }

    @Nullable
    static String baasVan(String typePad) {
        for (String[] b : BAZEN) {
            if (typePad.contains(b[0])) {
                return b[1];
            }
        }
        return null;
    }
}
