package nl.juiced.guhs.feature.vadsrijk.zone3.npc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcHaken;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone 3, slice npc (few, focused): (1) every zone-3 quest step names WHO and WHERE (an explicit target the tracker can point at,
 * with a "where" text) and every NPC has a spot, name and "where" text; (2) the whole zone-3 main chain walked through the shared
 * quest API (each objective through its real hook, hand-in by clicking the NPC guh) up to {@code zone3_klaar} and Vahoegje, with
 * the starter-mount step through the real {@code Rijdieren} choice, and the airship gate to Honingtak.
 */
public class Z3NpcGameTests {
    private static final String B = "vadsrijk_z3_npc";

    private static List<Quest> z3() {
        List<Quest> l = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (q.id().startsWith("z3_")) {
                l.add(q);
            }
        }
        return l;
    }

    @GuhTest(template = "empty", batch = B)
    public static void elkeStapWieEnWaar(GameTestHelper helper) {
        Language t = Language.getInstance();
        List<Quest> z3 = z3();
        long hoofd = z3.stream().filter(Quest::hoofd).count(), zij = z3.stream().filter(q -> q.soort() == Quest.Soort.ZIJ).count();
        helper.assertTrue(hoofd == 15 && zij >= 3, "zone 3: 15 main and a few side quests: " + hoofd + "/" + zij);
        helper.assertTrue(Quests.van("hq01_welkom") != null && Quests.van("z2_hq01_splinterpieken") != null, "zones 1 and 2 still load");
        for (String id : Z3NpcPlekken.NPC.keySet()) {
            NpcDef d = Npcs.van(id);
            helper.assertTrue(d != null && d.plek() != null, "NPC " + id + " is registered with a spot");
            helper.assertTrue(t.has(d.naamKey()) && t.has("vadsrijk.npc.plek.npc." + id), "NPC " + id + ": name + where text");
            helper.assertTrue(Z3NpcPlekken.zone3(d.plek().getX()), "NPC " + id + " stands in zone 3");
        }
        for (Quest q : z3) {
            for (String wie : List.of(q.gever(), q.inleveren())) {
                helper.assertTrue(Npcs.van(wie) != null && (t.has("vadsrijk.npc.plek.npc." + wie) || !wie.startsWith("z3_")),
                        q.id() + ": who (" + wie + ") and where");
            }
            helper.assertTrue(t.has(q.titelKey()) && t.has(q.tekstKey()) && t.has(q.sleutel() + ".bezig"), q.id() + ": texts");
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                helper.assertTrue(t.has(q.doelKey(i)), q.id() + " step " + i + ": what");
                helper.assertTrue(!d.plek().isEmpty() && QuestPlek.pos(helper.getLevel(), d.plek()) != null, q.id() + " step " + i + ": a target (" + d.plek() + ")");
                boolean andereZone = d.plek().startsWith("npc:") && !d.plek().startsWith("npc:z3_");
                helper.assertTrue(andereZone || t.has(QuestPlek.tekstKey(d.plek())), q.id() + " step " + i + ": where text " + QuestPlek.tekstKey(d.plek()));
                if (d.type() == Quest.Doel.Type.VERZAMEL) {
                    helper.assertTrue(Questlog.item(d.doel()) != net.minecraft.world.item.Items.AIR, q.id() + ": item " + d.doel());
                }
            }
        }
        helper.assertTrue(Huisdieren.van(Z3NpcRegistry.VAHOEGJE) != null && t.has("vadsrijk.huisdier.vahoegje.naam"), "Vahoegje is a pet");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 600)
    public static void hoofdlijnViaApi(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        Rijdieren.reset(p);
        Huisdieren.reset(p);
        Z3NpcRegistry.gebieden();
        // the chain, in story order
        List<Quest> hoofd = z3().stream().filter(Quest::hoofd).toList();
        String vorige = "z2_hq12_hartsplinter";
        int x = 0;
        for (Quest q : hoofd) {
            helper.assertTrue(q.na().contains(vorige), q.id() + " follows " + vorige);
            vorige = q.id();
            if (Questlog.status(p, q.id()) == 0) {
                Questlog.neemAan(p, q);
            }
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (Questlog.doelKlaar(p, q, i) || d.type() == Quest.Doel.Type.PRAAT && d.doel().equals(q.inleveren())) {
                    continue;   // (already counted by an earlier hook, or the hand-in click does it)
                }
                doe(helper, p, q, d, x++);
                helper.assertTrue(Questlog.doelKlaar(p, q, i), q.id() + " step " + i + " (" + d.type() + " " + d.doel() + ") counted");
            }
            RijkGuh inl = guh(helper, q.inleveren(), x++);
            QuestMotor.klik(p, inl);
            helper.assertTrue(Questlog.klaar(p, q.id()), q.id() + " handed in at " + q.inleveren());
            QuestMotor.afronden(p);   // (the dialogue closes: the quest action runs: lift, ride, cliffhanger)
            inl.discard();
        }
        helper.assertTrue(Z3NpcApi.zone3Klaar(p), "the finale set zone3_klaar");
        helper.assertTrue(Huisdieren.heeft(p, Z3NpcRegistry.VAHOEGJE), "the egg hatched: Vahoegje is yours");
        helper.assertTrue(Rijdieren.heeft(p, RijdierSoort.PLUIMGUH) && Rijdieren.heeftStarter(p), "the starter Pluimguh from the Pluimstal");
        helper.assertTrue(Questlog.map(p, Z3Haken.GEGEVEN).getBooleanOr("splinter_wachter", false), "all splinters handed out");
        Z3Verhaal.vergeet(p);
        Z3Haken.vergeet(p);
        Rijdieren.reset(p);
        Huisdieren.reset(p);
        Questlog.wisAlles(p);
        helper.succeed();
    }

    /** Completes one objective through the hook it really uses in game. */
    private static void doe(GameTestHelper helper, ServerPlayer p, Quest q, Quest.Doel d, int x) {
        String gebied = d.doel().startsWith("gebied:") ? d.doel().substring(7) : "";
        switch (d.type()) {
            case PRAAT -> {
                RijkGuh g = guh(helper, d.doel(), x);
                QuestMotor.klik(p, g);
                QuestMotor.afronden(p);
                g.discard();
            }
            case GEBIED -> {
                BlockPos b = Z3NpcPlekken.poi(d.doel().substring(4));
                net.minecraft.world.phys.Vec3 was = p.position();
                p.setPos(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
                Z3Haken.stap(p, helper.getLevel(), 0);   // (also the story triggers at that spot: splinters, Piepje, the kajuit...)
                p.setPos(was.x, was.y, was.z);   // (back before the next tick: no far chunks for the test level)
            }
            case GEHEIM -> {
                if (d.doel().equals("teller:" + Z3Haken.AANWIJZINGEN)) {
                    Z3NpcPlekken.AANWIJZING.forEach((id, b) -> Z3Haken.vind(p, helper.getLevel(), Z3Haken.AANWIJZINGEN, id, b));
                }
            }
            case OPVROLIJK, ELITE, BAAS -> Questlog.tel(p, d.type(), y -> NpcHaken.pastMob(y.doel(), "guhs:vr_z3_proef", gebied), d.n());
            case VERZAMEL -> Questlog.geef(p, new ItemStack(Questlog.item(d.doel()), d.n()));
            case MELD -> meld(helper, p, q, d);
            default -> helper.fail(q.id() + ": no test path for " + d.type());
        }
    }

    private static void meld(GameTestHelper helper, ServerPlayer p, Quest q, Quest.Doel d) {
        String[] s = d.doel().split(":", 2);
        switch (d.doel()) {
            case "rijdier:starter" -> {
                // the real path: the stalmeester's action opens the choice, the player picks pink; STARTER_GEKOZEN counts the step
                Z3Haken.starter(p);
                helper.assertTrue(Rijdieren.geefStarter(p, RijdierSoort.PluimKleur.ROZE), "a starter Pluimguh can be picked once");
                helper.assertTrue(!Rijdieren.kiesStarter(p), "the choice does not open twice");
            }
            case "z3:nest_herinnering", "z3:flash_f1", "z3:ei", "z3:finale" -> {
                // the scene the hook starts at that spot (the nest, the Pelszaal, the Nestkamer); run it to its end at once
                String plek = switch (d.doel()) {
                    case "z3:nest_herinnering", "z3:finale" -> "z3_kruintop";
                    case "z3:ei" -> "z3_nestkamer";
                    default -> "z3_pelszaal";
                };
                Z3Verhaal.spoel(p, helper.getLevel().getServer());   // (a scene that already runs: e.g. the nest memory, the ride)
                if (Questlog.waarde(p, q, q.doelen().indexOf(d)) < d.n()) {
                    BlockPos b = Z3NpcPlekken.poi(plek);
                    net.minecraft.world.phys.Vec3 was = p.position();
                    p.setPos(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
                    Z3Haken.stap(p, helper.getLevel(), 0);
                    p.setPos(was.x, was.y, was.z);
                    helper.assertTrue(Z3Verhaal.bezig(p), d.doel() + ": its scene plays");
                    Z3Verhaal.spoel(p, helper.getLevel().getServer());
                }
            }
            default -> {
                for (int i = 0; i < d.n(); i++) {
                    NpcApi.meld(p, s[0], s[1]);
                }
            }
        }
    }

    private static RijkGuh guh(GameTestHelper helper, String id, int x) {
        RijkGuh g = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(1 + x % 3, 1, 1 + (x / 3) % 3));
        g.stelIn(Npcs.van(id));
        return g;
    }
}
