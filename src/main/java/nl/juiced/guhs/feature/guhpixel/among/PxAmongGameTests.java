package nl.juiced.guhs.feature.guhpixel.among;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhpixel.GuhpixelTitels;
import nl.juiced.guhs.feature.guhpixel.Kluis;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxTest;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.among.model.Balans;
import nl.juiced.guhs.feature.guhpixel.among.model.Deelnemer;
import nl.juiced.guhs.feature.guhpixel.among.model.Ronde;
import nl.juiced.guhs.feature.guhpixel.among.model.Schip;
import nl.juiced.guhs.feature.guhpixel.among.model.Simulatie;
import nl.juiced.guhs.feature.guhpixel.among.model.TaakStand;
import nl.juiced.guhs.feature.guhpixel.among.model.Uitspraak;
import nl.juiced.guhs.feature.guhpixel.among.model.Vergadering;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.gametest.GuhTest;
import org.slf4j.Logger;

/**
 * Game tests of Among Guhs, the engine (batch px_among; run with {@code -Pgt=px_among}).
 * <ul>
 *   <li>the ship: the layout table against the arena template, every spot reachable, doors that close;</li>
 *   <li>the rules without a world ({@code among.model}): roles and tasks, pushing with witnesses, reporting, a whole meeting,
 *   how statements are weighed (a lie that is caught, a lie that works), sabotage, droomguhs, every way to win;</li>
 *   <li>a round on the real ship with a mock player as the Mika and one as crew: guh NPCs, panels, the button, vents, lights,
 *   doors, the droomguh in spectator mode, the pay-out, the inventory and the game mode coming back;</li>
 *   <li>the queue with two players, "klaar" and the difficulty; the rewards with the daily cap; the titles;</li>
 *   <li>the headless simulation of NPC-only rounds: the Mika must win 35-65% on Normaal (the numbers go to the log, lines
 *   starting with {@code [px_among sim]}).</li>
 * </ul>
 */
public class PxAmongGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BATCH = "px_among";
    private static final String KLEIN = "px_test_16", SCHIP = "px_test_96";

    private static void positie(Ronde r, int idx, int knoop) {
        Schip.Knoop k = r.schip.knopen.get(knoop);
        r.zetPositie(idx, k.x(), k.z(), 0f);
    }

    private static void ticks(Ronde r, int n) {
        for (int i = 0; i < n && r.fase != Ronde.Fase.KLAAR; i++) {
            r.tick();
        }
    }

    // =====================================================================================================================
    // the ship
    // =====================================================================================================================
    @GuhTest(template = KLEIN, batch = BATCH)
    public static void schipKloptMetDeTemplate(GameTestHelper helper) {
        Schip schip = Schip.standaard();
        StructureTemplate template = helper.getLevel().getStructureManager().get(AmongSlice.ARENA.template()).orElse(null);
        helper.assertTrue(template != null, "the ship template exists");
        helper.assertTrue(template.getSize().equals(AmongSlice.MAAT) && schip.breedte == 75 && schip.hoogte == 9 && schip.diepte == 51,
                "the template and the table have the arena's size: " + template.getSize());
        Set<BlockPos> panelen = new HashSet<>(), luiken = new HashSet<>(), knoppen = new HashSet<>();
        template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), AmongSlice.TAAKPANEEL.get()).forEach(b -> panelen.add(b.pos()));
        template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), AmongSlice.VENTILATIELUIK.get()).forEach(b -> luiken.add(b.pos()));
        template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), AmongSlice.NOODKNOP.get()).forEach(b -> knoppen.add(b.pos()));
        helper.assertTrue(panelen.size() == schip.panelen.size() && schip.panelen.size() == 21, "21 panels: " + panelen.size());
        for (Schip.Paneel p : schip.panelen) {
            helper.assertTrue(panelen.contains(new BlockPos(p.bx(), p.by(), p.bz())), "panel " + p.id() + " stands where the table says");
        }
        helper.assertTrue(luiken.size() == 8 && schip.luiken.size() == 8, "8 vents");
        for (Schip.Luik l : schip.luiken) {
            helper.assertTrue(luiken.contains(new BlockPos(l.x(), l.y(), l.z())) && !schip.netwerk(l).isEmpty(), "vent " + l.id() + " with a network");
        }
        helper.assertTrue(knoppen.equals(Set.of(new BlockPos(schip.knopX, schip.knopY, schip.knopZ))), "the button on the table");
        helper.assertTrue(schip.kamers().size() == 9 && schip.zones.size() == 17 && schip.stoelen.size() == 10 && schip.bedden.size() == 10,
                "nine rooms, eight corridors, ten seats, ten beds");
        helper.assertTrue(schip.taken.size() == 16 && schip.lichtPaneel != null && schip.alarmPanelen.size() == 2, "sixteen tasks, the repair panels");
        Set<String> soorten = new HashSet<>();
        for (Schip.Taak t : schip.taken) {
            soorten.add(t.soort());
            for (int p : t.panelen()) {
                helper.assertTrue(schip.panelen.get(p).soort() == Schip.PaneelSoort.TAAK, "task " + t.id() + " uses task panels only");
            }
        }
        helper.assertTrue(soorten.equals(Set.of("worstjes", "pasje", "kruimelbak", "pindasaus", "sorteren", "dromen", "wegen", "schakelaars")),
                "the eight classic kinds: " + soorten);
        // walking: everything is reachable from the Kantine; a room with its doors shut is not
        int kantine = schip.hub(schip.zoneVan("kantine"));
        for (Schip.Knoop k : schip.knopen) {
            helper.assertTrue(schip.pad(kantine, k.idx(), Set.of()) != null, "node " + k.id() + " can be reached");
        }
        int ziekenboeg = schip.zoneVan("ziekenboeg");
        Set<Integer> dicht = new HashSet<>();
        schip.deurenVan(ziekenboeg).forEach(d -> dicht.add(d.idx()));
        helper.assertTrue(dicht.size() == 1 && schip.pad(kantine, schip.hub(ziekenboeg), dicht) == null, "the Ziekenboeg has one door; shut = no way in");
        helper.assertTrue(schip.zoneOp(37.5, 9.5) == schip.zoneVan("kantine") && schip.zoneOp(23.5, 9.5) == schip.zoneVan("gang_h1")
                && schip.zoneOp(1, 1) == -1 && schip.naast(schip.zoneVan("kantine"), schip.zoneVan("gang_h1"))
                && !schip.naast(schip.zoneVan("kantine"), schip.zoneVan("slaapzaal")), "zones and what lies next to what");
        helper.succeed();
    }

    // =====================================================================================================================
    // the rules (no world)
    // =====================================================================================================================
    @GuhTest(template = KLEIN, batch = BATCH)
    public static void rollenDuwenMeldenStemmen(GameTestHelper helper) {
        Schip schip = Schip.standaard();
        Balans b = Balans.normaal();
        b.vergeetKans = 0;
        Ronde r = new Ronde(schip, b, 42L, 1, new int[]{0});
        helper.assertTrue(r.deelnemers.size() == 9 && r.wakker(Deelnemer.Rol.MIKA) == 1 && r.wakker(Deelnemer.Rol.CREW) == 8 && r.d(0).mika()
                && !r.d(0).npc && r.d(1).npc, "nine participants, one Mika: the player");
        Set<Object> kleuren = new HashSet<>();
        for (Deelnemer d : r.deelnemers) {
            kleuren.add(d.kleur);
            helper.assertTrue(d.taken.size() == b.takenPerGuh && d.zone == schip.zoneVan("kantine"), "everybody has tasks and starts in the Kantine");
        }
        helper.assertTrue(kleuren.size() == 9 && r.stappenTotaal() >= 8 * b.takenPerGuh && r.stappenKlaar() == 0, "nine colours, the crew's task bar");
        // pushing: the cooldown, the reach, the role
        helper.assertTrue(r.duw(0, 1) == Ronde.Antwoord.AFKOEL, "the pillow starts on cooldown");
        r.d(0).duwAfkoel = 0;
        r.zetPositie(0, r.d(1).x + 6, r.d(1).z, 0f);
        helper.assertTrue(r.duw(0, 1) == Ronde.Antwoord.TE_VER, "too far away");
        helper.assertTrue(r.duw(1, 2) == Ronde.Antwoord.MAG_NIET, "crew cannot push");
        r.zetPositie(0, r.d(1).x + 1.5, r.d(1).z, 0f);
        helper.assertTrue(r.duw(0, 1) == Ronde.Antwoord.OK && !r.d(1).wakker && r.d(1).lichaam && r.d(0).duwAfkoel == b.duwAfkoel
                && r.aantalDuwen == 1, "pushed asleep: a sleeper lies there, the cooldown runs");
        helper.assertTrue(r.duw(0, 2) == Ronde.Antwoord.AFKOEL && r.duw(0, 1) == Ronde.Antwoord.MAG_NIET, "not again, and not a sleeper");
        int getuigen = 0;
        for (Deelnemer d : r.deelnemers) {
            if (d.npc && d.wakker && d.brein.geheugen.zagDuwDoor == 0) {
                getuigen++;
            }
        }
        helper.assertTrue(getuigen == 7, "all seven other guhs at the table saw it: " + getuigen);
        // one of them reports it within two seconds (the NPCs notice a sleeper)
        ticks(r, 45);
        helper.assertTrue(r.fase == Ronde.Fase.VERGADERING && r.vergadering.slaper == 1 && r.d(r.vergadering.oproeper).npc && !r.d(1).lichaam,
                "an NPC reported the sleeper: a meeting");
        Vergadering v = r.vergadering;
        helper.assertTrue(v.uitspraken.get(0).soort() == Uitspraak.Soort.GEVONDEN && v.stap == Vergadering.Stap.BESPREKEN, "the reporter speaks first");
        helper.assertTrue(!r.stem(0, 2) && r.uitspraak(0, Uitspraak.Soort.WAAR_IK, -1, schip.zoneVan("navigatie")), "no voting yet, but talking");
        ticks(r, b.bespreekTijd);
        int duwGezegd = 0;
        for (Uitspraak u : v.uitspraken) {
            if (u.soort() == Uitspraak.Soort.DUW && u.wie() == 0) {
                duwGezegd++;
            }
        }
        helper.assertTrue(v.stap == Vergadering.Stap.STEMMEN && duwGezegd == 7, "every witness says who pushed: " + duwGezegd);
        helper.assertTrue(r.stem(0, 2) && !r.stem(0, 3), "one vote each");
        ticks(r, b.stemTijd + b.uitslagTijd + 5);
        helper.assertTrue(r.fase == Ronde.Fase.KLAAR && r.winnaar == Deelnemer.Rol.CREW && r.einde == Ronde.Einde.MIKAS_WEG
                && r.vorigeVergadering.weg == 0 && r.vorigeVergadering.wegMika && r.d(0).weggestemd, "the Mika is voted out: the crew wins");
        helper.succeed();
    }

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void uitsprakenWordenGewogen(GameTestHelper helper) {
        Schip schip = Schip.standaard();
        int slaapzaal = schip.zoneVan("slaapzaal"), schild = schip.zoneVan("schildkamer");
        // (1) a claim an NPC knows to be wrong makes the SPEAKER suspect, and the NPC says so
        Ronde r = new Ronde(schip, Balans.normaal(), 7L, 1, new int[]{8});
        Deelnemer getuige = r.d(3);
        getuige.brein.geheugen.gezienTick[0] = r.tick;
        getuige.brein.geheugen.gezienZone[0] = slaapzaal;
        helper.assertTrue(r.devVergadering(0) && r.fase == Ronde.Fase.VERGADERING, "a meeting by the button");
        double voor = getuige.brein.geheugen.verdenking[0];
        helper.assertTrue(r.uitspraak(0, Uitspraak.Soort.WAAR_IK, -1, schild), "the player claims the Schildkamer");
        helper.assertTrue(getuige.brein.geheugen.verdenking[0] >= voor + 2.4, "who saw the player in the Slaapzaal a moment ago does not believe it");
        helper.assertTrue(r.d(4).brein.geheugen.verdenking[0] < 1.0, "who saw nothing has no reason to doubt");
        ticks(r, r.balans.bespreekTijd + 200);
        boolean kloptNiet = false;
        for (Uitspraak u : r.vergadering.uitspraken) {
            kloptNiet |= u.soort() == Uitspraak.Soort.KLOPT_NIET && u.spreker() == 3 && u.wie() == 0 && u.zone() == slaapzaal;
        }
        helper.assertTrue(kloptNiet, "and says so: 'dat klopt niet'");
        for (int i = 0; i < r.balans.maxUitspraken + 2; i++) {
            r.uitspraak(0, Uitspraak.Soort.NIKS, -1, -1);
        }
        helper.assertTrue(r.d(0).uitspraken == r.balans.maxUitspraken && !r.uitspraak(0, Uitspraak.Soort.SUS, 2, -1)
                && !r.uitspraak(0, Uitspraak.Soort.VERDENK, 0, -1), "a limit per meeting; only the ready-made kinds; not about yourself");

        // (2) a lie nobody can check works: the NPCs weigh it and vote an innocent guh out
        Ronde r2 = new Ronde(schip, Balans.normaal(), 8L, 1, new int[]{8});
        r2.devVergadering(0);
        helper.assertTrue(r2.uitspraak(0, Uitspraak.Soort.DUW, 3, -1), "the player says it saw 3 push");
        helper.assertTrue(r2.d(4).brein.geheugen.verdenking[3] >= 2.5 && r2.d(3).brein.geheugen.verdenking[0] >= 4.0,
                "the others suspect 3 now; 3 itself knows the player lies");
        // vouching takes suspicion away again
        double verdacht = r2.d(5).brein.geheugen.verdenking[3];
        r2.uitspraak(0, Uitspraak.Soort.STA_IN, 3, -1);
        helper.assertTrue(r2.d(5).brein.geheugen.verdenking[3] < verdacht - 0.8, "'ik sta in voor' lowers it");
        r2.uitspraak(0, Uitspraak.Soort.DUW, 3, -1);
        ticks(r2, r2.balans.bespreekTijd + 1);
        helper.assertTrue(r2.stem(0, 3), "the player votes too");
        ticks(r2, r2.balans.stemTijd + r2.balans.uitslagTijd + 5);
        Vergadering v = r2.vorigeVergadering;
        helper.assertTrue(v != null && v.weg == 3 && !v.wegMika && r2.d(3).weggestemd && !r2.d(3).wakker && v.stemmen[3] == 0,
                "3 is voted out, wrongly (and voted for the player): " + (v == null ? "no meeting" : v.weg));
        helper.assertTrue(r2.fase == Ronde.Fase.SPEL && r2.wakker(Deelnemer.Rol.CREW) == 7, "the round goes on with one crew guh less");
        helper.succeed();
    }

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void takenSabotageDroomguhEnWinst(GameTestHelper helper) {
        Schip schip = Schip.standaard();
        Balans b = Balans.normaal();
        b.deelnemers = 4;       // four real players, no NPCs: nothing happens by itself
        Ronde r = new Ronde(schip, b, 5L, 4, new int[]{0});
        // a task step: at the panel, long enough
        TaakStand taak = r.d(1).taken.get(0);
        Schip.Paneel paneel = schip.panelen.get(taak.paneel());
        helper.assertTrue(r.beginTaak(1, paneel.idx()) == null, "not at the panel: nothing to do");
        positie(r, 1, paneel.knoop());
        helper.assertTrue(r.beginTaak(1, paneel.idx()) == taak && !r.taakKlaar(1, paneel.idx(), 80), "started; 'done' at once is refused");
        ticks(r, 80);
        helper.assertTrue(r.taakKlaar(1, paneel.idx(), 80) && taak.stap == 1 && r.stappenKlaar() == 1 && !r.taakKlaar(1, paneel.idx(), 80),
                "after the wait the step counts, once");
        // the Mika only pretends
        TaakStand nep = r.d(0).taken.get(0);
        positie(r, 0, schip.panelen.get(nep.paneel()).knoop());
        helper.assertTrue(r.beginTaak(0, nep.paneel()) == nep, "the Mika can stand at a panel");
        ticks(r, 80);
        helper.assertTrue(r.taakKlaar(0, nep.paneel(), 80) && nep.stap == 0 && r.stappenKlaar() == 1, "but its work counts for nothing");
        // lights out
        helper.assertTrue(r.saboteer(0, Ronde.Sabotage.LICHT, -1) == Ronde.Antwoord.AFKOEL && r.saboteer(1, Ronde.Sabotage.LICHT, -1) == Ronde.Antwoord.MAG_NIET,
                "sabotage has a cooldown and is for Mikas");
        r.saboteerAfkoel = 0;
        helper.assertTrue(r.saboteer(0, Ronde.Sabotage.LICHT, -1) == Ronde.Antwoord.OK && r.donker() && r.zicht(r.d(1)) == b.zichtDonker
                && r.zicht(r.d(0)) == b.zicht && r.saboteer(0, Ronde.Sabotage.ALARM, -1) == Ronde.Antwoord.AFKOEL, "lights out: the crew sees little, one sabotage at a time");
        positie(r, 1, schip.hub(schip.zoneVan("slaapzaal")));
        helper.assertTrue(!r.herstel(1, schip.lichtPaneel.idx()), "not fixed from afar");
        positie(r, 1, schip.lichtPaneel.knoop());
        helper.assertTrue(r.herstel(1, schip.lichtPaneel.idx()) && !r.donker() && r.saboteerAfkoel == b.saboteerAfkoel, "fixed in Elektra");
        // doors
        int ziekenboeg = schip.zoneVan("ziekenboeg"), kantine = schip.hub(schip.zoneVan("kantine"));
        r.saboteerAfkoel = 0;
        helper.assertTrue(r.saboteer(0, Ronde.Sabotage.DEUREN, schip.zoneVan("gang_vm")) == Ronde.Antwoord.MAG_NIET
                && r.saboteer(0, Ronde.Sabotage.DEUREN, ziekenboeg) == Ronde.Antwoord.OK && r.dichteDeuren.size() == 1
                && schip.pad(kantine, schip.hub(ziekenboeg), r.dichteDeuren) == null, "the doors of a room shut");
        ticks(r, b.deurTijd + 1);
        helper.assertTrue(r.dichteDeuren.isEmpty() && r.sabotage == Ronde.Sabotage.GEEN, "and open again by themselves");
        // the Knabbelalarm, fixed at both panels
        r.saboteerAfkoel = 0;
        r.saboteer(0, Ronde.Sabotage.ALARM, -1);
        positie(r, 1, schip.alarmPanelen.get(0).knoop());
        positie(r, 2, schip.alarmPanelen.get(1).knoop());
        helper.assertTrue(r.knopKan(r.d(1)) == Ronde.Antwoord.NIET_NU, "no button during the alarm");
        helper.assertTrue(r.herstel(1, schip.alarmPanelen.get(0).idx()) && r.sabotage == Ronde.Sabotage.ALARM && !r.herstel(1, schip.alarmPanelen.get(0).idx())
                && r.herstel(2, schip.alarmPanelen.get(1).idx()) && r.sabotage == Ronde.Sabotage.GEEN, "both codes: the alarm stops");
        // vents: only a Mika, only inside one network
        Schip.Luik van = schip.luikIn(schip.zoneVan("reactor")), naar = schip.netwerk(van).get(0), ander = schip.luikIn(schip.zoneVan("navigatie"));
        positie(r, 0, van.knoop());
        positie(r, 1, van.knoop());
        helper.assertTrue(r.luik(1, van.idx(), naar.idx()) == Ronde.Antwoord.MAG_NIET && r.luik(0, van.idx(), ander.idx()) == Ronde.Antwoord.MAG_NIET
                && r.luik(0, van.idx(), naar.idx()) == Ronde.Antwoord.OK && r.d(0).zone == naar.kamer() && r.luik(0, naar.idx(), van.idx()) == Ronde.Antwoord.AFKOEL,
                "through the vent to another room of the network");
        // a droomguh still does tasks; all tasks done = the crew wins
        r.d(0).duwAfkoel = 0;
        r.zetPositie(2, r.d(0).x + 1, r.d(0).z, 0f);
        helper.assertTrue(r.duw(0, 2) == Ronde.Antwoord.OK && !r.d(2).wakker && r.fase == Ronde.Fase.SPEL, "2 is pushed asleep; two crew are still awake");
        TaakStand droomTaak = r.d(2).taken.get(0);
        positie(r, 2, schip.panelen.get(droomTaak.paneel()).knoop());
        helper.assertTrue(r.beginTaak(2, droomTaak.paneel()) == droomTaak && r.knop(2) == Ronde.Antwoord.MAG_NIET, "a droomguh works on, but cannot call a meeting");
        ticks(r, 80);
        helper.assertTrue(r.taakKlaar(2, droomTaak.paneel(), 80) && droomTaak.stap == 1, "and its step counts");
        r.devTakenKlaar(1);
        r.devTakenKlaar(2);
        helper.assertTrue(r.fase == Ronde.Fase.SPEL, "not yet: 3 has tasks left");
        r.devTakenKlaar(3);
        helper.assertTrue(r.fase == Ronde.Fase.KLAAR && r.winnaar == Deelnemer.Rol.CREW && r.einde == Ronde.Einde.TAKEN, "all tasks done: the crew wins");

        // the other endings
        Ronde alarm = new Ronde(schip, b, 6L, 4, new int[]{0});
        alarm.saboteerAfkoel = 0;
        alarm.saboteer(0, Ronde.Sabotage.ALARM, -1);
        ticks(alarm, b.alarmTijd - 1);
        helper.assertTrue(alarm.fase == Ronde.Fase.SPEL, "the alarm runs 45 seconds");
        ticks(alarm, 2);
        helper.assertTrue(alarm.winnaar == Deelnemer.Rol.MIKA && alarm.einde == Ronde.Einde.ALARM, "nobody fixed it: the Mika wins");
        Ronde overmacht = new Ronde(schip, b, 7L, 4, new int[]{0});
        for (int doel = 1; doel <= 2; doel++) {
            overmacht.d(0).duwAfkoel = 0;
            overmacht.zetPositie(doel, overmacht.d(0).x + 1, overmacht.d(0).z, 0f);
            overmacht.duw(0, doel);
        }
        helper.assertTrue(overmacht.winnaar == Deelnemer.Rol.MIKA && overmacht.einde == Ronde.Einde.OVERMACHT, "one Mika, one crew awake: the Mika wins");
        Ronde weg = new Ronde(schip, b, 8L, 4, new int[]{0});
        weg.verlaat(0);
        helper.assertTrue(weg.winnaar == Deelnemer.Rol.CREW && weg.einde == Ronde.Einde.MIKAS_WEG, "the only Mika left the game: the crew wins");
        helper.succeed();
    }

    // =====================================================================================================================
    // on the real ship
    // =====================================================================================================================
    private static CompoundTag opties(String rol, boolean lastig) {
        CompoundTag t = new CompoundTag();
        t.putString("Rol", rol);
        t.putBoolean("Lastig", lastig);
        t.putLong("Seed", 99L);
        return t;
    }

    private static AmongSessie start(GameTestHelper helper, List<ServerPlayer> spelers, CompoundTag opties, java.util.function.Consumer<Balans> balans) {
        AmongSessie.TEST_BALANS = balans;
        try {
            Sessie s = Sessies.startOp(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), AmongSlice.SPEL, spelers, opties);
            return s instanceof AmongSessie a ? a : null;
        } finally {
            AmongSessie.TEST_BALANS = null;
        }
    }

    private static void naar(AmongSessie s, ServerPlayer p, double x, double z) {
        Vec3 pos = s.arena().wereld(new Vec3(x, 2.0, z));
        p.teleportTo(s.level(), pos.x, pos.y, pos.z, Set.of(), 0f, 0f, true);
        s.ronde.zetPositie(s.idx(p), x, z, 0f);
    }

    private static void naarKnoop(AmongSessie s, ServerPlayer p, int knoop) {
        Schip.Knoop k = s.ronde.schip.knopen.get(knoop);
        naar(s, p, k.x(), k.z());
    }

    @GuhTest(template = SCHIP, batch = BATCH, timeoutTicks = 600)
    public static void rondeAlsMikaOpHetSchip(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        p.getInventory().setItem(3, new ItemStack(Items.DIAMOND, 7));
        AmongSessie s = start(helper, List.of(p), opties("mika", false), b -> {
            b.vergeetKans = 0;
            b.bespreekTijd = 60;
            b.stemTijd = 120;
            b.uitslagTijd = 20;
            b.saboteerGemiddeld = 10_000_000;
        });
        helper.assertTrue(s != null && Sessies.van(p) == s, "the round started");
        Ronde r = s.ronde;
        helper.assertTrue(r.deelnemers.size() == 9 && r.d(0).mika() && s.idx(p) == 0 && !s.lastig, "nine participants, the player is the Mika");
        helper.assertTrue(level.getEntitiesOfClass(AmongGuhEntity.class, s.arena().doos()).size() == 8, "eight guh NPCs on the ship");
        helper.assertTrue(p.getInventory().getItem(0).is(AmongSlice.KUSSEN.get()) && p.getInventory().getItem(1).is(AmongSlice.SABOTEERKAART.get())
                && p.getInventory().getItem(8).is(AmongSlice.STEMBRIEFJE.get()) && !p.getInventory().hasAnyMatching(i -> i.is(Items.DIAMOND)),
                "the Mika's things; the own inventory is in the safe");
        ItemStack pak = p.getItemBySlot(EquipmentSlot.CHEST);
        helper.assertTrue(pak.is(Items.LEATHER_CHESTPLATE) && pak.get(DataComponents.DYED_COLOR) != null
                && pak.get(DataComponents.DYED_COLOR).rgb() == r.d(0).kleur.rgb, "a space suit in the own colour");
        helper.assertTrue(p.position().distanceTo(s.arena().wereld(new Vec3(r.d(0).x, 2, r.d(0).z))) < 0.5, "on the own seat at the table");
        CompoundTag hud = s.hudTag(0);
        helper.assertTrue(hud.getBooleanOr("Mika", false) && hud.getListOrEmpty("Taken").size() == r.balans.takenPerGuh && hud.getIntOr("Totaal", 0) > 0
                && hud.contains("DuwAfkoel"), "the HUD: role, fake tasks, the bar, the cooldown");
        helper.runAtTickTime(4, () -> {
            for (Deelnemer d : r.deelnemers) {
                if (d.npc) {
                    AmongGuhEntity guh = s.guh(d.idx);
                    helper.assertTrue(guh != null && guh.kleur() == d.kleur && guh.deelnemer == d.idx
                            && guh.position().distanceTo(s.arena().wereld(new Vec3(d.x, 2, d.z))) < 0.3, "guh " + d.idx + " stands where the round says");
                }
            }
            // the pillow: whoever stands in front of the Mika
            Deelnemer doel = r.d(4);
            naar(s, p, doel.x, doel.z - 1.5);
            helper.assertTrue(!s.duw(p, -1) && doel.wakker, "the cooldown");
            r.d(0).duwAfkoel = 0;
            helper.assertTrue(s.duw(p, -1) && !doel.wakker && s.guh(4).slaapt(), "pushed asleep: the guh lies down");
            // right-click on the sleeper = report (here the Mika reports its own victim)
            s.klikGuh(p, s.guh(4));
            helper.assertTrue(r.fase == Ronde.Fase.VERGADERING, "reported: a meeting");
            helper.assertTrue(p.position().distanceTo(s.arena().wereld(new Vec3(r.d(0).x, 2, r.d(0).z))) < 0.5, "back on the seat");
            Schip.Plek bed = r.schip.bedden.get(0);
            helper.assertTrue(s.guh(4).position().distanceTo(s.arena().wereld(new Vec3(bed.x(), 2.5625, bed.z()))) < 0.2, "the sleeper is carried to a bed");
            CompoundTag v = s.vergaderTag(0, true, false);
            helper.assertTrue(v.getListOrEmpty("Deelnemers").size() == 9 && v.getListOrEmpty("Uitspraken").size() == 1 && v.getListOrEmpty("Kamers").size() == 9
                    && v.getIntOr("Stap", -1) == 0 && v.getBooleanOr("Wakker", false), "the meeting screen's data");
            helper.assertTrue(s.zeg(p, Uitspraak.Soort.WAAR_IK.ordinal(), -1, r.schip.zoneVan("navigatie")) && !s.stem(p, 2), "talking, no voting yet");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(s.isGestopt(), "the round is over and everybody is out");
            helper.assertTrue(r.winnaar == Deelnemer.Rol.CREW && r.vorigeVergadering != null && r.vorigeVergadering.weg == 0, "the witnesses voted the Mika out");
            helper.assertTrue(p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "the droomguh has its own game mode back");
            helper.assertTrue(p.getInventory().getItem(3).is(Items.DIAMOND) && p.getInventory().getItem(3).getCount() == 7 && !Kluis.heeft(p)
                    && !p.getInventory().hasAnyMatching(i -> i.is(AmongSlice.KUSSEN.get())), "the own inventory is back, no game item is left");
            helper.assertTrue(AmongBeloning.cijfer(p, AmongBeloning.RONDES) == 1 && AmongBeloning.cijfer(p, AmongBeloning.BETRAPT) == 1
                    && AmongBeloning.cijfer(p, AmongBeloning.GEDUWD) == 1 && AmongBeloning.cijfer(p, AmongBeloning.WINST_MIKA) == 0, "the numbers");
            helper.assertTrue(Muntjes.vandaag(p, AmongBeloning.POT) == AmongBeloning.VERLIES, "a lost round pays " + AmongBeloning.VERLIES);
            helper.assertTrue(level.getEntitiesOfClass(AmongGuhEntity.class, s.arena().doos()).isEmpty(), "the ship is cleaned");
            PxTest.klaar(helper, p);
        });
    }

    @GuhTest(template = SCHIP, batch = BATCH, timeoutTicks = 400)
    public static void crewOpHetSchip(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        AmongSessie s = start(helper, List.of(p), opties("crew", false), b -> {
            b.duwAfkoelStart = 10_000_000;       // the NPC Mika keeps still: this test moves everything itself
            b.saboteerGemiddeld = 10_000_000;
            b.spelerTaakTijd = 10;
            b.herstelTijd = 10;
            b.deurTijd = 30;
            b.lichtReageerKans = 0;
            b.bespreekTijd = 20;
            b.stemTijd = 60;
            b.uitslagTijd = 10;
        });
        helper.assertTrue(s != null, "the round started");
        Ronde r = s.ronde;
        Schip schip = r.schip;
        int mika = 8;
        helper.assertTrue(!r.d(0).mika() && r.d(mika).mika() && r.d(mika).npc && p.getInventory().getItem(0).isEmpty()
                && p.getInventory().getItem(8).is(AmongSlice.STEMBRIEFJE.get()), "crew: no pillow");
        TaakStand taak = r.d(0).taken.get(0);
        Schip.Paneel paneel = schip.panelen.get(taak.paneel());
        BlockPos paneelPos = s.arena().wereld(paneel.bx(), paneel.by(), paneel.bz());
        helper.assertTrue(level.getBlockState(paneelPos).is(AmongSlice.TAAKPANEEL.get()), "the panel block is there");
        helper.assertTrue(s.gebruikBlok(p, paneelPos) && r.d(0).werkPaneel == -1, "a click from the table: too far, nothing starts");
        naarKnoop(s, p, paneel.knoop());
        helper.assertTrue(s.gebruikBlok(p, paneelPos) && r.d(0).werkPaneel == paneel.idx(), "at the panel the task starts");
        helper.assertTrue(!s.taakKlaar(p, paneel.idx(), new CompoundTag()) && taak.stap == 0, "'done' at once is refused");
        helper.assertTrue(!s.gebruikBlok(p, s.arena().wereld(5, 1, 5)), "a floor block is no panel");
        helper.runAtTickTime(3, () -> s.gebruikBlok(p, paneelPos));
        helper.runAtTickTime(14, () -> {
            helper.assertTrue(s.taakKlaar(p, paneel.idx(), new CompoundTag()) && taak.stap == 1 && r.d(0).takenKlaar + r.stappenKlaar() >= 1,
                    "after the panel's time the step is done");
            // a vent is not for the crew
            Schip.Luik luik = schip.luiken.get(0);
            helper.assertTrue(level.getBlockState(s.arena().wereld(luik.x(), luik.y(), luik.z())).is(AmongSlice.VENTILATIELUIK.get())
                    && s.gebruikBlok(p, s.arena().wereld(luik.x(), luik.y(), luik.z())), "the vent block (only a Mika fits)");
            // lights out: the crew player is blinded until somebody fixes it in Elektra
            r.saboteerAfkoel = 0;
            helper.assertTrue(r.saboteer(mika, Ronde.Sabotage.LICHT, -1) == Ronde.Antwoord.OK, "the Mika puts the lights out");
            s.verwerkNu();
        });
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(p.hasEffect(MobEffects.BLINDNESS) && s.hudTag(0).getIntOr("Sabotage", 0) == Ronde.Sabotage.LICHT.ordinal(), "blind in the dark");
            naarKnoop(s, p, schip.lichtPaneel.knoop());
            BlockPos licht = s.arena().wereld(schip.lichtPaneel.bx(), schip.lichtPaneel.by(), schip.lichtPaneel.bz());
            helper.assertTrue(s.gebruikBlok(p, licht) && !s.taakKlaar(p, schip.lichtPaneel.idx(), new CompoundTag()) && r.donker(), "repairing takes a moment");
            s.gebruikBlok(p, licht);
        });
        helper.runAtTickTime(52, () -> {
            helper.assertTrue(s.taakKlaar(p, schip.lichtPaneel.idx(), new CompoundTag()) && !r.donker() && !p.hasEffect(MobEffects.BLINDNESS), "the lights are on again");
            // doors: real blocks in the doorways of the room, gone again after a while
            int ziekenboeg = schip.zoneVan("ziekenboeg");
            Schip.Deur deur = schip.deurenVan(ziekenboeg).get(0);
            r.saboteerAfkoel = 0;
            helper.assertTrue(r.saboteer(mika, Ronde.Sabotage.DEUREN, ziekenboeg) == Ronde.Antwoord.OK, "doors shut");
            s.verwerkNu();
            helper.assertTrue(level.getBlockState(s.arena().wereld(deur.x0(), 2, deur.z0())).is(Blocks.PINK_WOOL)
                    && level.getBlockState(s.arena().wereld(deur.x1(), 4, deur.z1())).is(Blocks.PINK_WOOL), "pillow doors in the doorway");
        });
        helper.runAtTickTime(90, () -> {
            Schip.Deur deur = schip.deurenVan(schip.zoneVan("ziekenboeg")).get(0);
            helper.assertTrue(level.getBlockState(s.arena().wereld(deur.x0(), 2, deur.z0())).isAir() && r.sabotage == Ronde.Sabotage.GEEN, "open again");
            // the button: once per round, with a cooldown
            BlockPos knop = s.arena().wereld(schip.knopX, schip.knopY, schip.knopZ);
            naarKnoop(s, p, schip.knopKnoop);
            helper.assertTrue(level.getBlockState(knop).is(AmongSlice.NOODKNOP.get()), "the button block");
            r.knopAfkoel = 100;
            helper.assertTrue(s.gebruikBlok(p, knop) && r.fase == Ronde.Fase.SPEL, "the button has a cooldown");
            r.knopAfkoel = 0;
            helper.assertTrue(s.gebruikBlok(p, knop) && r.fase == Ronde.Fase.VERGADERING && r.d(0).knopGebruikt, "a meeting by the button");
        });
        helper.runAtTickTime(115, () -> helper.assertTrue(s.stem(p, -1) && !s.stem(p, -1), "skip, once"));
        helper.runAtTickTime(200, () -> {
            helper.assertTrue(r.fase == Ronde.Fase.SPEL && r.vorigeVergadering != null, "the meeting is over, the round goes on");
            // the NPC Mika pushes the player asleep: a droomguh in spectator mode, a sleeping guh where the player stood
            r.d(mika).duwAfkoel = 0;
            r.d(mika).x = r.d(0).x + 1;
            r.d(mika).z = r.d(0).z;
            r.d(mika).zone = r.d(0).zone;
            helper.assertTrue(r.duw(mika, 0) == Ronde.Antwoord.OK, "pushed");
            s.verwerkNu();
            r.d(0).lichaam = false;      // (nobody finds this sleeper: the test goes on without a meeting)
            helper.assertTrue(p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR && s.isDroomguh(p) && s.droomguhs().contains(p)
                    && s.guh(0) != null && s.guh(0).slaapt() && p.isAlive() && p.getHealth() == p.getMaxHealth(), "a droomguh; nobody is hurt");
            helper.assertTrue(!s.hudTag(0).getBooleanOr("Wakker", true), "the HUD knows");
            // a crash now would leave the player a spectator: the game mode of before is in the player's own data for the next login
            helper.assertTrue(nl.juiced.guhs.quest.GuhQuests.saved(p).getIntOr(AmongSessie.MODUS, -1) == GameType.SURVIVAL.getId(), "the old game mode is saved");
            // a droomguh still finishes tasks
            TaakStand volgende = null;
            for (TaakStand t : r.d(0).taken) {
                if (!t.klaar()) {
                    volgende = t;
                    break;
                }
            }
            helper.assertTrue(volgende != null, "a task left");
            Schip.Paneel q = schip.panelen.get(volgende.paneel());
            naarKnoop(s, p, q.knoop());
            helper.assertTrue(s.gebruikBlok(p, s.arena().wereld(q.bx(), q.by(), q.bz())) && r.d(0).werkPaneel == q.idx(), "the droomguh starts a task");
        });
        helper.runAtTickTime(215, () -> {
            int stappen = r.stappenKlaar();
            int paneel2 = r.d(0).werkPaneel;
            helper.assertTrue(paneel2 >= 0 && s.taakKlaar(p, paneel2, new CompoundTag()) && r.stappenKlaar() == stappen + 1, "and it counts for the crew");
            // everybody finishes: the crew wins, the player is paid for a win and the own tasks
            for (Deelnemer d : r.deelnemers) {
                r.devTakenKlaar(d.idx);
            }
            s.verwerkNu();
            helper.assertTrue(r.winnaar == Deelnemer.Rol.CREW && r.einde == Ronde.Einde.TAKEN && s.afgelopen(), "all tasks done: the crew wins");
            int verwacht = AmongBeloning.bedrag(true, r.balans.takenPerGuh, false);
            helper.assertTrue(Muntjes.vandaag(p, AmongBeloning.POT) == verwacht && AmongBeloning.cijfer(p, AmongBeloning.WINST_CREW) == 1
                    && AmongBeloning.cijfer(p, AmongBeloning.TAKEN) == r.balans.takenPerGuh, "paid " + verwacht + " muntjes");
            PxTest.klaar(helper, p);
            helper.assertTrue(p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && s.isGestopt()
                    && !nl.juiced.guhs.quest.GuhQuests.saved(p).contains(AmongSessie.MODUS), "out of the game: the own game mode is back");
            // and the crash case: a leftover mark at login
            p.setGameMode(GameType.SPECTATOR);
            nl.juiced.guhs.quest.GuhQuests.saved(p).putInt(AmongSessie.MODUS, GameType.SURVIVAL.getId());
            AmongSessie.herstelModus(p);
            helper.assertTrue(p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && !nl.juiced.guhs.quest.GuhQuests.saved(p).contains(AmongSessie.MODUS),
                    "a login with a leftover droomguh mark: back to the own game mode");
            helper.succeed();
        });
    }

    @GuhTest(template = SCHIP, batch = BATCH, timeoutTicks = 200)
    public static void wachtrijMetTweeSpelers(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer a = PxTest.speler(helper), b = PxTest.speler(helper);
        AmongWachtrij.leeg();
        var oud = AmongWachtrij.starter;
        Sessie[] gestart = new Sessie[1];
        AmongWachtrij.starter = (spelers, lastig) -> {
            CompoundTag opties = new CompoundTag();
            opties.putBoolean("Lastig", lastig);
            gestart[0] = start(helper, spelers, opties, bal -> bal.duwAfkoelStart = 10_000_000);
            return gestart[0];
        };
        try {
            helper.assertTrue(AmongWachtrij.magSpelen(a) && AmongWachtrij.erbij(a) && AmongWachtrij.erbij(b) && AmongWachtrij.grootte() == 2
                    && a.getUUID().equals(AmongWachtrij.leider()), "two players in the queue, the first is the leader");
            CompoundTag stand = AmongWachtrij.stand(helper.getLevel().getServer(), b, true);
            helper.assertTrue(stand.getListOrEmpty("Spelers").size() == 2 && !stand.getBooleanOr("Leider", true) && stand.getIntOr("Deelnemers", 0) == 9,
                    "what the screen shows");
            AmongWachtrij.actie(b, AmongPayloads.WACHTRIJ_NIVEAU);
            helper.assertTrue(!AmongWachtrij.lastig(), "only the leader picks the difficulty");
            AmongWachtrij.actie(b, AmongPayloads.WACHTRIJ_KLAAR);
            AmongWachtrij.actie(a, AmongPayloads.WACHTRIJ_NIVEAU);
            helper.assertTrue(AmongWachtrij.lastig() && !AmongWachtrij.stand(helper.getLevel().getServer(), b, false).getBooleanOr("Klaar", true),
                    "Lastig; a change of rules asks everybody to be ready again");
            AmongPayloads.opActie(a, new AmongPayloads.Actie(AmongPayloads.WACHTRIJ_KLAAR, 0, 0));
            helper.assertTrue(gestart[0] == null && Sessies.van(a) == null, "not everybody is ready: no round yet");
            AmongPayloads.opActie(b, new AmongPayloads.Actie(AmongPayloads.WACHTRIJ_KLAAR, 0, 0));
            helper.assertTrue(gestart[0] instanceof AmongSessie && Sessies.van(a) == gestart[0] && Sessies.van(b) == gestart[0] && AmongWachtrij.grootte() == 0,
                    "everybody ready: one round for the two of them, the queue is free again");
            AmongSessie s = (AmongSessie) gestart[0];
            helper.assertTrue(s.lastig && s.ronde.deelnemers.size() == 10 && s.ronde.wakker(Deelnemer.Rol.MIKA) == 2 && s.idx(a) == 0 && s.idx(b) == 1
                    && helper.getLevel().getEntitiesOfClass(AmongGuhEntity.class, s.arena().doos()).size() == 8, "Lastig: ten participants, two Mikas, eight guh NPCs");
            helper.assertTrue(!AmongWachtrij.erbij(a), "who plays cannot queue");
            // one of the two leaves: the round goes on for the other
            Sessies.verlaat(b, nl.juiced.guhs.feature.guhpixel.Vertrek.VERLATEN);
            helper.assertTrue(!s.isGestopt() && s.ronde.d(1).weg && Sessies.van(b) == null && Sessies.van(a) == s, "b left; a plays on");
            PxTest.klaar(helper, a, b);
            helper.assertTrue(s.isGestopt(), "the last player left: the round stops");
            // alone = at once
            PxTest.gebied(helper);
            ServerPlayer c = PxTest.speler(helper);
            gestart[0] = null;
            AmongWachtrij.erbij(c);
            AmongWachtrij.actie(c, AmongPayloads.WACHTRIJ_KLAAR);
            helper.assertTrue(gestart[0] != null && ((AmongSessie) gestart[0]).ronde.deelnemers.size() == 9, "alone and ready: the round starts at once");
            PxTest.klaar(helper, c);
        } finally {
            AmongWachtrij.starter = oud;
            AmongWachtrij.leeg();
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // rewards, numbers, titles
    // =====================================================================================================================
    @GuhTest(template = KLEIN, batch = BATCH)
    public static void beloningMetDagpot(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper), q = PxTest.speler(helper);
        helper.assertTrue(AmongBeloning.bedrag(false, 0, false) == 25 && AmongBeloning.bedrag(true, 0, false) == 45 && AmongBeloning.bedrag(true, 7, false) == 59
                && AmongBeloning.bedrag(false, 3, true) == 47 && AmongBeloning.bedrag(true, 7, true) == 89, "the pay table");
        Titels.Titel sus = null, onterecht = null;
        for (Titels.Titel t : GuhpixelTitels.ALLE) {
            if (t.id().equals("among_sus")) {
                sus = t;
            } else if (t.id().equals("among_onterecht")) {
                onterecht = t;
            }
        }
        helper.assertTrue(sus != null && onterecht != null && !sus.behaald().test(p) && !onterecht.behaald().test(p), "the titles exist and are not earned yet");
        int saldo = Muntjes.saldo(p);
        helper.assertTrue(AmongBeloning.rondeKlaar(p, false, true, false, 7, 0, true) == 59 && Muntjes.saldo(p) == saldo + 59, "a win as crew with seven tasks");
        helper.assertTrue(onterecht.behaald().test(p) && !sus.behaald().test(p) && !onterecht.behaald().test(q), "wrongly voted out once: that title, for this player only");
        helper.assertTrue(AmongBeloning.rondeKlaar(p, true, true, true, 0, 4, false) == 68, "a win as Mika on Lastig (tasks do not count for a Mika)");
        helper.assertTrue(AmongBeloning.rondeKlaar(p, true, false, false, 0, 1, true) == 25 && AmongBeloning.rondeKlaar(p, false, false, false, 2, 0, true) == 29,
                "two lost rounds");
        helper.assertTrue(AmongBeloning.rondeKlaar(p, false, true, false, 7, 0, false) == AmongBeloning.DAG_MAX - 181
                && AmongBeloning.rondeKlaar(p, false, true, false, 7, 0, false) == 0 && Muntjes.vandaag(p, AmongBeloning.POT) == AmongBeloning.DAG_MAX,
                "the daily pot is full at " + AmongBeloning.DAG_MAX);
        helper.assertTrue(AmongBeloning.rondeKlaar(q, false, false, false, 0, 0, false) == 25, "another player has an own pot");
        PxTest.spoel(24.5);
        helper.assertTrue(AmongBeloning.rondeKlaar(p, false, false, false, 0, 0, false) == 25, "a new real day: paid again");
        helper.assertTrue(AmongBeloning.cijfer(p, AmongBeloning.RONDES) == 7 && AmongBeloning.cijfer(p, AmongBeloning.WINST_CREW) == 3
                && AmongBeloning.cijfer(p, AmongBeloning.WINST_MIKA) == 1 && AmongBeloning.cijfer(p, AmongBeloning.ONTERECHT) == 2
                && AmongBeloning.cijfer(p, AmongBeloning.BETRAPT) == 1 && AmongBeloning.cijfer(p, AmongBeloning.WEGGESTEMD) == 3
                && AmongBeloning.cijfer(p, AmongBeloning.GEDUWD) == 5 && AmongBeloning.cijfer(p, AmongBeloning.LASTIG_WINST) == 1
                && AmongBeloning.cijfer(q, AmongBeloning.RONDES) == 1, "the personal numbers");
        helper.assertTrue(sus.behaald().test(p), "voted out three times: Sus");
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }

    // =====================================================================================================================
    // the balance: headless NPC rounds
    // =====================================================================================================================
    @GuhTest(template = KLEIN, batch = BATCH, timeoutTicks = 400)
    public static void simulatieWinkansen(GameTestHelper helper) {
        Schip schip = Schip.standaard();
        Simulatie.Uitkomst normaal = Simulatie.draai(schip, Balans.normaal(), 250, 1);
        Simulatie.Uitkomst lastig = Simulatie.draai(schip, Balans.lastig(), 250, 2);
        LOGGER.info("[px_among sim] {}", normaal.tekst("Normaal"));
        LOGGER.info("[px_among sim] {}", lastig.tekst("Lastig"));
        helper.assertTrue(normaal.onbeslist() == 0 && lastig.onbeslist() == 0, "every round ends with a winner");
        helper.assertTrue(normaal.mikaDeel() >= 0.35 && normaal.mikaDeel() <= 0.65, "Normaal: the Mika wins 35-65%: " + normaal.tekst("Normaal"));
        helper.assertTrue(lastig.mikaDeel() >= 0.40 && lastig.mikaDeel() <= 0.80, "Lastig: the Mikas win 40-80%: " + lastig.tekst("Lastig"));
        helper.assertTrue(normaal.gemTicks() >= 20 * 60 * 6 && normaal.gemTicks() <= 20 * 60 * 15 && normaal.langste() <= 20 * 60 * 25,
                "a round takes a believable while: " + normaal.tekst("Normaal"));
        helper.assertTrue(normaal.doorTaken() > 0 && normaal.doorStemmen() > 0 && normaal.doorOvermacht() > 0, "every ending happens");
        helper.assertTrue(normaal.gemVergaderingen() >= 2 && normaal.gemDuwen() >= 3, "meetings and pushes happen");
        helper.succeed();
    }
}
