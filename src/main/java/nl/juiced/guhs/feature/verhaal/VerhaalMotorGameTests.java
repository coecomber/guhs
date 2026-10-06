package nl.juiced.guhs.feature.verhaal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.barbecuether.GrillPortalBlock;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.gids.VerhalenVoortgang;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhWorldData;
import nl.juiced.guhs.world.ModDimensions;

/**
 * bbq2, the verhaal engine (server side), on the demo story ({@link VerhaalDemo}): a questline's steps are per player,
 * forward only, grant their advancements and tell their listeners; which line is followed; the lock of a cutscene and of a
 * narrator card (no damage, held in place, no shoves; {@code daarna} once, never on a replay or a break-off); the scene
 * script's maths; Guhdalfs sluier (shove, put back, hidden, protected, open); Duwtje and rest points; the next-goal
 * pointer and the Superkompas entry "Mijn verhaal"; the portal lock; a registered title and the Guhkenner rule; the texts.
 * Mock players are not ticked by the server and their client receives nothing: the tests tick the lock themselves, and a
 * scene "plays" for {@link Vast#MOCK_TICKS} ticks.
 */
public class VerhaalMotorGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "verhaalmotor";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Vast.breekAf(p, false);
            Sluiers.vergeet(p.getUUID());
            VerhaalSync.vergeet(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static String key(Component c) {
        return c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : "";
    }

    private static void tik(ServerPlayer p) {
        Vast.onTick(new PlayerTickEvent.Post(p));
    }

    /** Steps: per player, forward only, an advancement per step passed, listeners, flags, counters, once-only. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhaalMotorStappenPerSpeler(GameTestHelper helper) {
        Verhaallijn l = VerhaalDemo.lijn();
        helper.assertTrue(l != null && Verhaallijnen.van("demo") == l && l.stappen() == 3, "the demo line is registered (dev)");
        ServerPlayer a = speler(helper), b = speler(helper);
        List<String> gehoord = new ArrayList<>();
        Verhaallijn.StapLuisteraar luister = (p, oud, nieuw) -> {
            if (p == a) {
                gehoord.add(oud + ">" + nieuw);
            }
        };
        l.opStap(luister);
        helper.assertTrue(l.stap(a) == 0 && !l.begonnen(a) && !l.klaar(a), "a fresh player: step 0, not started");
        helper.assertTrue(l.begin(a) && l.begonnen(a) && !l.begin(a) && l.stap(a) == 0, "begin: started without a step, once");
        helper.assertTrue(!l.verder(a, 1) && l.stap(a) == 0, "verder only from the step the player is at");
        helper.assertTrue(l.zet(a, 2) && l.stap(a) == 2, "zet 2");
        helper.assertTrue(GidsFeature.heeft(a, "quest/demo_stap_1") && GidsFeature.heeft(a, "quest/demo_stap_2")
                && !GidsFeature.heeft(a, "quest/demo_stap_3"), "an advancement for every step passed");
        helper.assertTrue(!l.zet(a, 1) && !l.zet(a, 2) && l.stap(a) == 2, "forward only");
        helper.assertTrue(l.verder(a, 2) && l.klaar(a) && l.stap(a) == 3 && GidsFeature.heeft(a, "quest/demo_stap_3"), "verder to the end");
        helper.assertTrue(!l.zet(a, 9) && l.stap(a) == 3, "never past the end");
        helper.assertTrue(gehoord.equals(List.of("0>2", "2>3")), "the listener heard " + gehoord);
        helper.assertTrue(l.stap(b) == 0 && !l.begonnen(b) && !GidsFeature.heeft(b, "quest/demo_stap_1"), "the other player has their own progress");
        // flags, counters, once-only
        helper.assertTrue(!l.vlag(a, "x"), "no flag yet");
        l.vlag(a, "x", true);
        l.teller(a, "n", 4);
        helper.assertTrue(l.vlag(a, "x") && l.teller(a, "n") == 4 && !l.vlag(b, "x") && l.teller(b, "n") == 0, "flags and counters per player");
        l.vlag(a, "x", false);
        helper.assertTrue(!l.vlag(a, "x"), "flag off");
        helper.assertTrue(l.eenmalig(a, "cadeau") && !l.eenmalig(a, "cadeau") && l.eenmalig(b, "cadeau"), "once per player");
        CompoundTag saved = GuhQuests.saved(a);
        helper.assertTrue(saved.getIntOr("guhs_demo_stap", -1) == 3 && saved.getIntOr("guhs_demo_t_n", -1) == 4, "saved with the player (survives death)");
        l.wis(a);
        helper.assertTrue(l.stap(a) == 0 && !l.begonnen(a) && l.teller(a, "n") == 0 && l.eenmalig(a, "cadeau"), "wis forgets everything");
        weg(helper, a, b);
        helper.succeed();
    }

    /** The Guhdex state of a registered line, its text variants, what is followed, and what goes to the client. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhaalMotorGevolgdEnStand(GameTestHelper helper) {
        Verhaallijn l = VerhaalDemo.lijn(), vervolg = VerhaalDemo.vervolg();
        ServerPlayer p = speler(helper);
        helper.assertTrue(VerhalenVoortgang.ids().indexOf("demo") >= 0 && VerhalenVoortgang.ids().indexOf("demo") < VerhalenVoortgang.ids().indexOf("timmerguh")
                && VerhalenVoortgang.STAPPEN.get("demo") == 3 && VerhalenVoortgang.sleutels("demo").contains("2_knabbel"), "registered lines are questlines of the tab, first");
        VerhaalStand v = VerhalenVoortgang.van(p, "demo");
        helper.assertTrue(v != null && v.status() == VerhaalStand.Status.NIET_BEGONNEN && v.stap() == 0 && v.stappen() == 3, "not started");
        helper.assertTrue(Verhaallijnen.gevolgd(p) == null && Doelen.van(p) == null, "nothing followed before anything started");
        l.begin(p);
        helper.assertTrue(VerhalenVoortgang.van(p, "demo").status() == VerhaalStand.Status.BEZIG && Verhaallijnen.gevolgd(p) == l, "started: followed");
        helper.assertTrue(!vervolg.aanDeBeurt(p) && l.aanDeBeurt(p), "the second line waits for the first (na)");
        l.zet(p, 2);
        helper.assertTrue(l.sleutel(p).equals("2") && l.stand(p).nodig().size() == 1 && !l.stand(p).nodig().get(0).genoeg(), "step 2: needs a knabbel");
        p.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 3));
        v = l.stand(p);
        helper.assertTrue(l.sleutel(p).equals("2_knabbel") && v.nodig().get(0).genoeg()
                && key(v.nu()).equals("gui.guhs.verhalen.demo.nu.2_knabbel"), "the text variant of a step");
        CompoundTag stand = VerhaalSync.stand(p);
        helper.assertTrue(stand.getCompoundOrEmpty("Lijnen").getCompoundOrEmpty("demo").getIntOr("S", -1) == 2
                && stand.getCompoundOrEmpty("Lijnen").getCompoundOrEmpty("demo").getStringOr("K", "").equals("2_knabbel")
                && stand.getStringOr("Volg", "").equals("demo"), "the client gets step, variant and the followed line");
        helper.assertTrue(stand.getListOrEmpty("Verborgen").toString().contains(VerhaalDemo.PLEK), "and what the sluier hides");
        l.zet(p, 3);
        helper.assertTrue(l.sleutel(p).equals("klaar") && l.stand(p).klaar() && l.stand(p).beloningen().stream().allMatch(VerhaalStand.Beloning::binnen), "done");
        helper.assertTrue(vervolg.aanDeBeurt(p) && Verhaallijnen.gevolgd(p) == vervolg, "the next line opens and is followed by itself");
        helper.assertTrue(!VerhaalSync.stand(p).getListOrEmpty("Verborgen").toString().contains(VerhaalDemo.PLEK), "the sluier opened");
        // picking a line in the Guhdex
        ServerPlayer q = speler(helper);
        l.begin(q);
        VerhaalSync.kies(q, "demo_vervolg");
        helper.assertTrue(Verhaallijnen.gevolgd(q) == vervolg && VerhaalSync.gekozen(q).equals("demo_vervolg"), "a picked line is followed");
        VerhaalSync.kies(q, "bestaat_niet");
        helper.assertTrue(Verhaallijnen.gevolgd(q) == l && VerhaalSync.gekozen(q).isEmpty(), "an unknown pick: by itself again");
        weg(helper, p, q);
        helper.succeed();
    }

    /** A cutscene: the lock (no damage, held, not shoved), daarna once at the end, seen; a replay never runs daarna. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public static void verhaalMotorCutsceneSlot(GameTestHelper helper) {
        Cutscene scene = VerhaalDemo.scene();
        ServerPlayer p = speler(helper);
        ServerLevel level = helper.getLevel();
        int[] daarna = {0};
        BlockPos anker = p.blockPosition();
        Vec3 plek = p.position();
        helper.assertTrue(!Cutscenes.bezig(p) && !Cutscenes.gezien(p, scene.id()) && Duwtje.mag(p), "nothing plays");
        helper.assertTrue(Cutscenes.speel(p, scene, anker, Rotation.CLOCKWISE_90, s -> daarna[0]++), "the scene starts");
        helper.assertTrue(Cutscenes.bezig(p) && !Cutscenes.speel(p, scene, anker, Rotation.NONE, s -> daarna[0] += 10)
                && !Verteller.toon(p, "demo", s -> daarna[0] += 100), "one thing at a time");
        helper.assertTrue(!Duwtje.mag(p), "nobody shoves a player who is watching");
        float leven = p.getHealth();
        ServerPlayer ander = speler(helper);
        // (a player whose client never said "loaded" can't be hurt at all: both are loaded, so the lock is what protects p)
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        ander.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        ander.hurtServer(level, level.damageSources().generic(), 6f);
        helper.assertTrue(ander.getHealth() < leven, "(a player who isn't watching does get hurt: the check below means something)");
        weg(helper, ander);
        p.hurtServer(level, level.damageSources().generic(), 6f);
        p.hurtServer(level, level.damageSources().lava(), 6f);
        helper.assertTrue(p.getHealth() == leven, "no damage during a cutscene");
        Duwtje.duw(p, new Vec3(1, 0, 0), 1.0);
        helper.assertTrue(p.getDeltaMovement().lengthSqr() < 1e-6, "no shove during a cutscene");
        p.snapTo(plek.x + 6, plek.y + 3, plek.z);
        p.fallDistance = 12;
        tik(p);
        helper.assertTrue(p.position().distanceTo(plek) < 0.05 && p.fallDistance == 0, "held on the spot, no fall");
        helper.assertTrue(daarna[0] == 0 && !Cutscenes.gezien(p, scene.id()), "not over yet");
        Vast.klaar(p, VerhaalPayloads.SCENE, scene.id());
        helper.assertTrue(Cutscenes.bezig(p), "a client can't end a scene early (not skippable)");
        helper.onEachTick(() -> tik(p));
        helper.runAfterDelay(Vast.MOCK_TICKS + 2, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && daarna[0] == 1 && Cutscenes.gezien(p, scene.id()), "over: daarna ran once, seen (" + daarna[0] + ")");
            helper.assertTrue(!Duwtje.mag(p), "still looked after right after the scene");
            p.hurtServer(level, level.damageSources().generic(), 6f);
            helper.assertTrue(p.getHealth() == leven, "no damage in the after-care");
            CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty("guhs_scene_" + scene.id());
            helper.assertTrue(BlockPos.of(t.getLongOr("Pos", 0L)).equals(anker) && t.getIntOr("Draai", -1) == Rotation.CLOCKWISE_90.ordinal(),
                    "the anchor of the first viewing is remembered");
            Cutscenes.herbekijk(p, scene.id());
            helper.assertTrue(Cutscenes.bezig(p), "the replay plays (near its anchor)");
        });
        helper.runAfterDelay(2L * Vast.MOCK_TICKS + 6, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && daarna[0] == 1, "a replay never runs daarna");
            // far away: the picture book
            p.snapTo(plek.x + 300, plek.y, plek.z);
            Cutscenes.herbekijk(p, scene.id());
            Vast.Slot slot = Vast.van(p);
            helper.assertTrue(slot != null && slot.soort.equals(VerhaalPayloads.BOEK), "far from the anchor it is a picture book");
        });
        helper.runAfterDelay(3L * Vast.MOCK_TICKS + 10 + Vast.NAZORG, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && daarna[0] == 1 && Duwtje.mag(p), "over, and the after-care ended");
            weg(helper, p);
            helper.succeed();
        });
    }

    /** A scene that is broken off (death, logout, another dimension) never runs daarna and isn't "seen": it replays. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhaalMotorCutsceneAfgebroken(GameTestHelper helper) {
        Cutscene scene = VerhaalDemo.scene();
        ServerPlayer p = speler(helper);
        int[] daarna = {0};
        helper.assertTrue(Cutscenes.speel(p, scene, p.blockPosition(), Rotation.NONE, s -> daarna[0]++) && Cutscenes.bezig(p), "playing");
        Vast.onDeath(new LivingDeathEvent(p, helper.getLevel().damageSources().genericKill()));
        helper.assertTrue(!Cutscenes.bezig(p) && daarna[0] == 0 && !Cutscenes.gezien(p, scene.id()), "broken off: no daarna, not seen");
        Cutscenes.herbekijk(p, scene.id());
        helper.assertTrue(!Cutscenes.bezig(p), "a scene that was never seen has no replay");
        helper.assertTrue(Cutscenes.speel(p, scene, p.blockPosition(), Rotation.NONE, s -> daarna[0]++), "it plays again next time");
        Vast.breekAf(p, false);
        weg(helper, p);
        helper.succeed();
    }

    /** A narrator card: the same lock, daarna when it was read, read again without daarna. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhaalMotorVertelkaart(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        int[] daarna = {0};
        helper.assertTrue(Verteller.van("demo") != null && Verteller.van("demo").regels() == 3 && "demo".equals(Verteller.van("demo").lijn()), "the card is registered");
        helper.assertTrue(!Verteller.toon(p, "bestaat_niet", s -> daarna[0]++) && !Cutscenes.bezig(p), "an unknown card: nothing");
        Verteller.herbekijk(p, "demo");
        helper.assertTrue(!Cutscenes.bezig(p), "an unread card can't be read again");
        helper.assertTrue(Verteller.toon(p, "demo", s -> daarna[0]++) && Cutscenes.bezig(p) && !Verteller.gezien(p, "demo") && !Duwtje.mag(p), "the card shows, the player is locked");
        helper.onEachTick(() -> tik(p));
        helper.runAfterDelay(Vast.MOCK_TICKS + 2, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && daarna[0] == 1 && Verteller.gezien(p, "demo"), "read: daarna once");
            Verteller.herbekijk(p, "demo");
            helper.assertTrue(Cutscenes.bezig(p), "reading it again");
        });
        helper.runAfterDelay(2L * Vast.MOCK_TICKS + 6, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && daarna[0] == 1, "reading again never runs daarna");
            helper.assertTrue(VerhaalSync.stand(p).getListOrEmpty("Kaarten").toString().contains("demo"), "the client knows the card was read");
            weg(helper, p);
            helper.succeed();
        });
    }

    /** The scene script: where actors are, which way they face, the camera path, cuts, fades, subtitles, the anchor's rotation. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhaalMotorSceneScript(GameTestHelper helper) {
        Cutscene s = VerhaalDemo.scene();
        helper.assertTrue(Cutscene.van("verhaal_demo") == s && s.duur() == 190 && s.heeftSpeler() && "demo".equals(s.lijn()) && "demo".equals(s.kaart()),
                "the demo scene is registered");
        helper.assertTrue(s.plek("guh", 0).equals(new Vec3(0.5, 0, 6.5)) && s.plek("guh", 500).equals(new Vec3(0.5, 0, 2.5)), "an actor starts and ends where the script says");
        helper.assertTrue(Math.abs(s.plek("guh", 50).z - 4.5) < 1e-6 && s.looptNu("guh", 50) && !s.looptNu("guh", 90), "halfway its walk");
        helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(s.yaw("guh", 50) - 180f)) < 0.01f, "it faces where it walks (north)");
        helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(s.yaw("reisguh", 10) - 270f)) < 0.01f, "an actor keeps its yaw until it looks");
        float kijkt = s.yaw("reisguh", 70);
        helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(kijkt - Cutscene.yawVan(new Vec3(3, 0, -1)))) < 0.01f, "then it looks at the point");
        Vec3[] c0 = s.cameraOp(0), c30 = s.cameraOp(30), c105 = s.cameraOp(105), c106 = s.cameraOp(106);
        helper.assertTrue(c0[0].equals(new Vec3(6.0, 3.2, -2.0)) && c30[0].distanceTo(c0[0]) > 0.2 && c30[0].distanceTo(new Vec3(5.0, 2.4, 1.0)) > 0.2, "the camera glides");
        helper.assertTrue(c105[0].distanceTo(new Vec3(4.0, 1.8, 5.5)) < 1e-6 && c106[0].equals(new Vec3(3.4, 1.7, -0.6)), "and jumps at a cut");
        helper.assertTrue(c106[1].distanceTo(s.plek("guh", 106).add(0, 1, 0)) < 1e-6, "a following camera looks at its actor");
        helper.assertTrue(s.zwartOp(100) == 0 && s.zwartOp(186) == 1f && s.zinOp(20) != null && s.zinOp(20).key().equals("begin") && s.zinOp(75) == null, "fades and subtitles");
        // the anchor: a scene position turns exactly like a template block
        BlockPos anker = new BlockPos(100, 64, -30);
        for (Rotation r : Rotation.values()) {
            BlockPos blok = StructureTemplate.transform(new BlockPos(2, 0, 5), Mirror.NONE, r, BlockPos.ZERO).offset(anker);
            Vec3 midden = Cutscene.wereld(anker, r, new Vec3(2.5, 0, 5.5));
            helper.assertTrue(BlockPos.containing(midden).equals(blok) && Math.abs(midden.x - blok.getX() - 0.5) < 1e-6 && Math.abs(midden.z - blok.getZ() - 0.5) < 1e-6,
                    "rotation " + r + ": the middle of a template block stays the middle of that block");
            Vec3 zuid = Cutscene.wereld(anker, r, new Vec3(0.5, 0, 1.5)).subtract(Cutscene.wereld(anker, r, new Vec3(0.5, 0, 0.5)));
            helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(Cutscene.yawVan(zuid) - Cutscene.wereldYaw(r, 0f))) < 0.01f, "rotation " + r + ": a yaw turns along");
        }
        helper.succeed();
    }

    /** Guhdalfs sluier: a shove at the edge, put back from deeper in, hidden, protected; open for whoever's story is there. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhaalMotorSluier(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper), klaar = speler(helper), toeschouwer = speler(helper);
        Verhaallijn l = VerhaalDemo.lijn();
        l.zet(klaar, 3);
        toeschouwer.setGameMode(GameType.SPECTATOR);
        BlockPos o = p.blockPosition();
        Sluiers.wisPlekken(level, VerhaalDemo.PLEK);
        Sluiers.Zone zone = Sluiers.zetPlek(level, VerhaalDemo.PLEK, new BoundingBox(o.getX() + 3, o.getY() - 2, o.getZ() - 2, o.getX() + 8, o.getY() + 6, o.getZ() + 2));
        try {
            helper.assertTrue(zone.x0() == o.getX() + 1 && zone.x1() == o.getX() + 10 && zone.z0() == o.getZ() - 4 && zone.y0() == o.getY() - 4
                    && zone.y1() == o.getY() + 8, "the wall stands rand blocks around the box");
            helper.assertTrue(Sluiers.magBinnen(p, o.east(5).above(20)) && Sluiers.magBinnen(p, o.east(5).below(9)) && Sluiers.zone(level, o.east(5).above(20)) == null,
                    "above and below the box is free (a mine under a plain doesn't close the plain)");
            helper.assertTrue(Sluiers.isVerborgen(p, VerhaalDemo.PLEK) && !Sluiers.open(p, VerhaalDemo.PLEK) && !Sluiers.isVerborgen(klaar, VerhaalDemo.PLEK)
                    && Sluiers.open(p, "geen_sluier"), "hidden until the story is there");
            BlockPos binnen = o.east(5);
            helper.assertTrue(!Sluiers.magBinnen(p, binnen) && Sluiers.magBinnen(klaar, binnen) && Sluiers.magBinnen(p, o) && Sluiers.zone(level, binnen) == zone,
                    "magBinnen per player");
            // outside: nothing happens, the spot is remembered
            Vec3 buiten = p.position();
            Sluiers.houdBuiten(p);
            helper.assertTrue(p.position().equals(buiten) && p.getDeltaMovement().lengthSqr() < 1e-6, "outside the wall nothing happens");
            // a step in: a gentle shove back out
            p.snapTo(zone.x0() + 0.6, buiten.y, buiten.z);
            float leven = p.getHealth();
            Sluiers.houdBuiten(p);
            helper.assertTrue(p.getDeltaMovement().x < -0.3 && p.hurtMarked && p.getHealth() == leven, "one step in: shoved back out, no damage");
            // deep in (ran, rode or was dropped in): put back where they last stood outside
            p.setDeltaMovement(Vec3.ZERO);
            p.snapTo(zone.x0() + 5.5, buiten.y, buiten.z);
            p.fallDistance = 9;
            Sluiers.houdBuiten(p);
            helper.assertTrue(p.position().distanceTo(buiten) < 0.01 && p.fallDistance == 0 && p.getHealth() == leven, "deep in: back where they stood, unharmed");
            // whoever is there in the story, and spectators, walk through
            klaar.snapTo(zone.x0() + 5.5, buiten.y, buiten.z);
            toeschouwer.snapTo(zone.x0() + 5.5, buiten.y, buiten.z);
            Vec3 daar = klaar.position();
            Sluiers.houdBuiten(klaar);
            Sluiers.houdBuiten(toeschouwer);
            helper.assertTrue(klaar.position().equals(daar) && toeschouwer.position().equals(daar), "open for a player whose story is there; spectators pass");
            // nobody breaks or places there
            // (bbq2 ring-kern, CONTRACT_130 13.9: the sluier's box IS the protected box of feature.wereld.Bescherming)
            BreakBlockEvent breek = new BreakBlockEvent(level, binnen.below(), Blocks.STONE.defaultBlockState(), klaar);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(breek);
            BreakBlockEvent ernaast = new BreakBlockEvent(level, o.below(), Blocks.STONE.defaultBlockState(), klaar);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(ernaast);
            helper.assertTrue(breek.isCanceled() && !ernaast.isCanceled(), "protected inside, also for players who may enter; free outside");
            helper.assertTrue(nl.juiced.guhs.feature.wereld.Bescherming.beschermd(level, new BlockPos(zone.x0(), zone.y0(), zone.z0()))
                    && nl.juiced.guhs.feature.wereld.Bescherming.beschermd(level, new BlockPos(zone.x1(), zone.y1(), zone.z1()))
                    && !nl.juiced.guhs.feature.wereld.Bescherming.beschermd(level, new BlockPos(zone.x0() - 1, zone.y0(), zone.z0())),
                    "the protected box is exactly the box of smoke");
            // the goal pointer and the compass never give a hidden structure away
            Doel doel = Doel.structuur(level.dimension(), VerhaalDemo.PLEK, Component.literal("x"));
            helper.assertTrue(Doelen.zoek(p, doel) == null, "a hidden structure is never a goal");
            // it opens when the story gets there
            l.zet(p, 3);
            p.snapTo(zone.x0() + 5.5, buiten.y, buiten.z);
            Sluiers.houdBuiten(p);
            helper.assertTrue(p.position().x == zone.x0() + 5.5 && !Sluiers.isVerborgen(p, VerhaalDemo.PLEK) && Sluiers.magBinnen(p, binnen), "the sluier opened for this player");
        } finally {
            Sluiers.wisPlekken(level, VerhaalDemo.PLEK);
        }
        helper.assertTrue(Sluiers.zone(level, o.east(5)) == null, "the wall is gone");
        weg(helper, p, klaar, toeschouwer);
        helper.succeed();
    }

    /** Duwtje: a shove is knockback only, never for spectators; terug and the rest points put a player back unharmed. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhaalMotorDuwtjeEnRustpunt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper), toeschouwer = speler(helper);
        toeschouwer.setGameMode(GameType.SPECTATOR);
        helper.assertTrue(Duwtje.mag(p) && !Duwtje.mag(toeschouwer), "who may be shoved");
        float leven = p.getHealth();
        Duwtje.duw(p, new Vec3(0, 0, -4), 0.9);
        Vec3 v = p.getDeltaMovement();
        helper.assertTrue(Math.abs(v.z + 0.9) < 1e-6 && Math.abs(v.x) < 1e-6 && v.y > 0.2 && v.y <= 0.45 && p.hurtMarked && p.getHealth() == leven, "a shove: knockback only (" + v + ")");
        Duwtje.duw(toeschouwer, new Vec3(1, 0, 0), 1.0);
        helper.assertTrue(toeschouwer.getDeltaMovement().lengthSqr() < 1e-6, "a spectator is not shoved");
        Praat.doeAlsOf(p, "verhaalmotor_test", null);
        helper.assertTrue(Praat.bezig(p) && !Duwtje.mag(p), "nobody shoves a player who is talking");
        Praat.vergeet(p);
        helper.assertTrue(Duwtje.mag(p), "talking is over");
        // rest points
        helper.assertTrue(Rustpunten.van(p, "test") == null && !Rustpunten.terug(p, "test"), "no rest point yet");
        Vec3 rust = p.position().add(2, 0, 1);
        p.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
        Rustpunten.zet(p, "test", level.dimension(), rust, 90f);
        p.snapTo(rust.x + 40, rust.y + 10, rust.z);
        p.fallDistance = 20;
        p.setDeltaMovement(new Vec3(0, -2, 0));
        helper.assertTrue(Rustpunten.terug(p, "test"), "back to the rest point");
        helper.assertTrue(p.position().distanceTo(rust) < 0.01 && p.fallDistance == 0 && p.getDeltaMovement().lengthSqr() < 1e-6 && p.getHealth() == leven
                && GuhQuests.count(p, ModItems.KAAS_KNABBELS.get()) == 5 && Math.abs(net.minecraft.util.Mth.wrapDegrees(p.getYRot() - 90f)) < 0.01f,
                "put back: no fall, no damage, nothing lost, facing the saved way");
        helper.assertTrue(Rustpunten.van(p, "test").dim() == level.dimension() && Rustpunten.van(p, "ander") == null, "a rest point per kind");
        Rustpunten.wis(p, "test");
        helper.assertTrue(Rustpunten.van(p, "test") == null, "forgotten");
        weg(helper, p, toeschouwer);
        helper.succeed();
    }

    /** The next-goal pointer and the Superkompas: "Mijn verhaal" points where the followed story goes; voegToe adds a place. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhaalMotorDoelEnKompas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        Verhaallijn l = VerhaalDemo.lijn();
        BlockPos anker = p.blockPosition().offset(7, 0, 3);
        GuhQuests.saved(p).putLong(VerhaalDemo.ANKER, anker.asLong());
        GuhQuests.saved(p).putString(VerhaalDemo.DIM, level.dimension().identifier().toString());
        helper.assertTrue(Doelen.van(p) == null && Doelen.wijs(p) == null, "no story followed: no goal");
        l.begin(p);
        Doel d = Doelen.van(p);
        helper.assertTrue(d != null && anker.equals(d.plek()) && d.dim() == level.dimension() && anker.equals(Doelen.zoek(p, d)) && anker.equals(Doelen.wijs(p)),
                "the goal of the followed line");
        l.zet(p, 2);
        helper.assertTrue(anker.south(12).equals(Doelen.wijs(p)), "the goal moves with the step");
        // the compass
        ItemStack kompas = new ItemStack(ModItems.SUPERKOMPAS.get());
        helper.assertTrue(SuperkompasItem.allowed(SuperkompasItem.DOEL) && SuperkompasItem.categoryOf(SuperkompasItem.DOEL) == -1, "Mijn verhaal is a choice, in no tab");
        SuperkompasItem.choose(kompas, SuperkompasItem.DOEL);
        SuperkompasItem.volgVerhaal(kompas, level, p, true);
        LodestoneTracker tracker = kompas.get(DataComponents.LODESTONE_TRACKER);
        helper.assertTrue(tracker != null && tracker.target().isPresent() && tracker.target().get().pos().equals(anker.south(12))
                && tracker.target().get().dimension() == level.dimension(), "the compass points at the story's goal");
        // the goal in another dimension: the portal last used here, or nothing
        GuhQuests.saved(p).putString(VerhaalDemo.DIM, ModDimensions.GUHMENSION.identifier().toString());
        helper.assertTrue(Doelen.van(p) != null && Doelen.zoek(p, Doelen.van(p)) == null && Doelen.wijs(p) == null && Doelen.portaal(p) == null,
                "a goal elsewhere and no portal known: nothing to point at");
        SuperkompasItem.volgVerhaal(kompas, level, p, true);
        helper.assertTrue(!kompas.has(DataComponents.LODESTONE_TRACKER), "the compass spins");
        Doelen.onAankomst(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(p, ModDimensions.GUHMENSION, level.dimension()));
        helper.assertTrue(p.blockPosition().equals(Doelen.portaal(p)) && p.blockPosition().equals(Doelen.wijs(p)), "it points at the portal the player came through");
        l.zet(p, 3);
        VerhaalDemo.vervolg().zet(p, 1);
        helper.assertTrue(Doelen.van(p) == null && Doelen.wijs(p) == null, "everything done: no goal");
        // a slice adds its structure to a tab (and the tab is as it was afterwards)
        int tab = -1;
        for (int i = 0; i < SuperkompasItem.CATEGORIES.size(); i++) {
            if (SuperkompasItem.CATEGORIES.get(i).id().equals("einde")) {
                tab = i;
            }
        }
        SuperkompasItem.Category oud = SuperkompasItem.CATEGORIES.get(tab);
        try {
            helper.assertTrue(!oud.structures().contains("spiesburcht"), "not there yet");
            SuperkompasItem.voegToe("einde", "spiesburcht");
            SuperkompasItem.voegToe("einde", "spiesburcht");
            List<String> nu = SuperkompasItem.CATEGORIES.get(tab).structures();
            helper.assertTrue(nu.size() == oud.structures().size() + 1 && nu.get(nu.size() - 1).equals("spiesburcht") && nu.subList(0, nu.size() - 1).equals(oud.structures()),
                    "voegToe adds it once, after what was there");
        } finally {
            SuperkompasItem.CATEGORIES.set(tab, oud);
        }
        boolean gooit = false;
        try {
            SuperkompasItem.voegToe("bestaat_niet", "spiesburcht");
        } catch (IllegalArgumentException e) {
            gooit = true;
        }
        helper.assertTrue(gooit, "an unknown tab is a mistake");
        weg(helper, p);
        helper.succeed();
    }

    /** The portal lock: only asked from the Guhmensie, the first lock that says no wins; none registered = open. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhaalMotorPortaalslot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper), q = speler(helper);
        // (bbq2 ring-kern: the Knabbelring's own lock is registered for everybody; these two have finished chapter 1)
        Verhaallijn h1 = Verhaallijnen.van("ring_h1");
        if (h1 != null) {
            h1.zet(p, h1.stappen());
            h1.zet(q, h1.stappen());
        }
        java.util.function.BiFunction<ServerLevel, Entity, Component> slot = (lvl, e) -> e == p ? Component.literal("nee") : null;
        helper.assertTrue(GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, p) == null, "no lock: open");
        GrillPortalBlock.SLOTEN.add(slot);
        try {
            Component nee = GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, p);
            helper.assertTrue(nee != null && nee.getString().equals("nee"), "refused in the Guhmensie");
            helper.assertTrue(GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, q) == null, "another player may go");
            helper.assertTrue(GrillPortalBlock.slot(nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.BARBECUETHER, level, p) == null
                    && GrillPortalBlock.slot(level, p) == null && !GrillPortalBlock.geweigerd(level, p), "the way back is never asked");
        } finally {
            GrillPortalBlock.SLOTEN.remove(slot);
        }
        weg(helper, p, q);
        helper.succeed();
    }

    /** A registered title works like the built-in ones; the Guhkenner keeps the title of a Guhdex that was full before. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhaalMotorTitelEnKenner(GameTestHelper helper) {
        ServerPlayer p = speler(helper), oud = speler(helper);
        Titels.Titel t = new Titels.Titel("verhaalmotor_test", "gui.guhs.titels.naam.guhkenner", ChatFormatting.GOLD, "minecraft:spyglass", s -> s == p);
        Titels.registreer(t);
        try {
            helper.assertTrue(Titels.van("verhaalmotor_test") == t && Titels.ALLE.get(Titels.ALLE.size() - 1) == t, "registered, after the built-in ones");
            helper.assertTrue(Titels.behaald(p).contains(t) && !Titels.behaald(oud).contains(t) && Titels.kies(p, "verhaalmotor_test") && Titels.actief(p) == t, "earned and chosen");
            boolean dubbel = false;
            try {
                Titels.registreer(t);
            } catch (IllegalStateException e) {
                dubbel = true;
            }
            helper.assertTrue(dubbel, "a title id only once");
        } finally {
            Titels.ALLE.remove(t);
        }
        // the Guhkenner
        GuhWorldData data = GuhWorldData.get(helper.getLevel().getServer());
        helper.assertTrue(!GuhDex.kenner(p), "an empty Guhdex");
        GuhDex.onthoudKenner(p);
        helper.assertTrue(!GuhQuests.saved(p).getBooleanOr(GuhDex.KENNER_OUD, false) && GuhQuests.saved(p).getBooleanOr(GuhDex.KENNER_GEKEKEN, false),
                "looked at once (at login): no");
        for (GuhVariant v : GuhDex.TELLEND) {
            if (!GuhDex.NIEUW_BBQ2.contains(v.name())) {
                data.player(oud.getUUID()).seen.add(v);
                data.player(p.getUUID()).seen.add(v);
            }
        }
        GuhQuests.saved(oud).remove(GuhDex.KENNER_GEKEKEN);   // (as if this is oud's first login with the update, with a full Guhdex)
        GuhDex.onthoudKenner(oud);
        GuhDex.onthoudKenner(p);
        helper.assertTrue(GuhQuests.saved(oud).getBooleanOr(GuhDex.KENNER_OUD, false) && !GuhQuests.saved(p).getBooleanOr(GuhDex.KENNER_OUD, false),
                "full before the new pages at the first login: kept for good; a later login changes nothing");
        data.player(oud.getUUID()).seen.remove(GuhVariant.NORMAL);
        helper.assertTrue(GuhDex.kenner(oud), "the old Guhkenner stays one");
        data.player(oud.getUUID()).seen.clear();
        data.player(p.getUUID()).seen.clear();
        weg(helper, p, oud);
        helper.succeed();
    }

    /** Every text and data file of the engine and of the demo exists. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhaalMotorTekstenBestaan(GameTestHelper helper) {
        Language lang = Language.getInstance();
        for (String k : List.of("gui.guhs.verhalen.kop.knabbelring", "gui.guhs.verhalen.kop.guhrio", "gui.guhs.verhalen.kop.techniek", "gui.guhs.verhalen.kop.barbecue",
                "gui.guhs.verhalen.kop.demo", "gui.guhs.verhalen.kop.herbekijk", "gui.guhs.verhalen.doelregel.aan", "gui.guhs.verhalen.doelregel.uit",
                "gui.guhs.verhalen.doelregel.tooltip", "gui.guhs.verhalen.volg", "gui.guhs.verhalen.volg.gekozen", "gui.guhs.verhalen.volg.vanzelf",
                "gui.guhs.verhalen.volg.tooltip", "gui.guhs.verhalen.volgt", "gui.guhs.verhalen.geheim", "gui.guhs.verhalen.geheim.tooltip",
                "gui.guhs.verhalen.reiskaart", "gui.guhs.verhalen.reiskaart.tooltip", "gui.guhs.verhalen.kaart.hier", "gui.guhs.verhalen.kaart.nu",
                "gui.guhs.verhalen.kaart.klaar", "gui.guhs.verhalen.herbekijk", "gui.guhs.verhalen.herbekijk.scene", "gui.guhs.verhalen.herbekijk.kaart",
                "gui.guhs.verhaal.verder", "structure.guhs." + SuperkompasItem.DOEL, "structure.guhs." + SuperkompasItem.DOEL + ".tooltip",
                "gui.guhs.verhaal.kompas.geen", "gui.guhs.verhaal.kompas.afstand", "gui.guhs.verhaal.kompas.portaal", "gui.guhs.verhaal.kompas.elders",
                "gui.guhs.verhaal.kompas.zoek", Sluiers.BERICHT, Sluiers.BESCHERMD, "guhs.configuration.objectiveLine", "guhs.configuration.objectiveLine.tooltip")) {
            helper.assertTrue(lang.has(k), "text " + k);
        }
        for (Verhaallijn l : Verhaallijnen.alle()) {
            String base = "gui.guhs.verhalen." + l.id();
            helper.assertTrue(lang.has(base + ".naam") && lang.has(base + ".uitleg") && lang.has("gui.guhs.verhalen.kop." + l.groep()), "name and heading of " + l.id());
            for (int i = 0; i < l.stappen(); i++) {
                helper.assertTrue(lang.has(VerhaalStand.stapKey(l.id(), i)), l.id() + " step " + i);
                helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(nl.juiced.guhs.Guhs.id("quest/" + l.id() + "_stap_" + (i + 1))) != null,
                        "advancement quest/" + l.id() + "_stap_" + (i + 1));
            }
            for (String s : VerhalenVoortgang.sleutels(l.id())) {
                helper.assertTrue(lang.has(base + ".nu." + s) && lang.has(base + ".waar." + s), l.id() + " " + s);
            }
        }
        for (String id : Verteller.ids()) {
            Verteller.Kaart k = Verteller.van(id);
            helper.assertTrue(lang.has(k.titelKey()), "card title " + id);
            for (int i = 0; i < k.regels(); i++) {
                helper.assertTrue(lang.has(k.regelKey(i)), "card " + id + " line " + i);
            }
        }
        for (Cutscene s : Cutscene.alle()) {
            helper.assertTrue(lang.has(s.titelKey()), "scene title " + s.id());
            for (Cutscene.Zeg z : s.zinnen()) {
                helper.assertTrue(lang.has(s.tekstKey(z.key())), "scene " + s.id() + " line " + z.key());
            }
        }
        for (Reiskaart k : Reiskaarten.alle()) {
            helper.assertTrue(lang.has(k.naamKey()), "travel map " + k.id());
            for (Halte h : k.haltes()) {
                helper.assertTrue(Verhaallijnen.van(h.lijn()) != null && h.kaartX() >= 0 && h.kaartX() < 256 && h.kaartY() >= 0 && h.kaartY() < 160, "halte " + h.lijn());
            }
        }
        helper.assertTrue(helper.getLevel().getServer().getResourceManager().getResource(nl.juiced.guhs.Guhs.id("kaart/verborgen.json")).isPresent(),
                "the live map's hide list is in the datapack");
        helper.succeed();
    }
}
