package nl.juiced.guhs.feature.doolhof;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;

/**
 * Het Guhdoolhof: the maze plans (always solvable, knabbels, fake ones), the anchor maths, the rewards (+1), the role,
 * shop and clothing sources, the protection and the lanterns, and the real building: its numbers match DoolhofVeld,
 * a whole game (growing the maze, finding every knabbel, out through the exit), a Mika that pinches without hurting,
 * a fake knabbel's penalty and the exit that sends you back without all knabbels.
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class DoolhofGameTests {
    private static final String EMPTY = "empty";
    private static final String GEBOUW = "guhdoolhof";

    private static GuhNpcEntity vadskronkel(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(1),
                n -> n.getKind() == GuhNpcEntity.Kind.DOOLHOFGUH);
        helper.assertTrue(npcs.size() == 1, "there is one Meneer Vadskronkel: " + npcs.size());
        return npcs.get(0);
    }

    private static ServerPlayer speler(GameTestHelper helper, GuhNpcEntity npc) {
        @SuppressWarnings("removal")
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.moveTo(npc.getX(), npc.getY(), npc.getZ() + 2);
        for (Niveau n : Niveau.values()) {
            GuhQuests.saved(p).remove(DoolhofGame.BEST_KEY + n.id());
        }
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer p) {
        DoolhofGame.stopFor(p);
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static int tel(ServerPlayer p, net.minecraft.world.item.Item item) {
        return GuhQuests.count(p, item);
    }

    // --- pure logic -------------------------------------------------------------------------------------------------------

    /** Every maze, on every level, with many seeds: all cells reachable, the right number of knabbels (and fakes only on lastig in dead ends). */
    @GameTest(template = EMPTY)
    public static void doolhofKaartenZijnAltijdOplosbaar(GameTestHelper helper) {
        for (Niveau n : Niveau.values()) {
            for (long seed = 0; seed < 40; seed++) {
                DoolhofKaart k = new DoolhofKaart(n, seed * 7919L + 3);
                helper.assertTrue(k.alles(), n + "/" + seed + ": every cell can be reached");
                helper.assertTrue(k.knabbels.size() == DoolhofKaart.aantalKnabbels(n), n + ": " + k.knabbels.size() + " knabbels");
                helper.assertTrue(k.nep.size() == DoolhofKaart.aantalNep(n), n + ": " + k.nep.size() + " fake knabbels");
                helper.assertTrue(k.n == DoolhofKaart.grootte(n) && k.off == (DoolhofKaart.N - k.n) / 2, n + ": the size");
                helper.assertTrue(k.actief[k.startX][k.startZ] && k.actief[k.uitX][k.uitZ], "start and exit are in the maze");
                helper.assertTrue(k.startZ == k.off + k.n - 1 && k.uitZ == k.off, "start south, exit north");
                for (int[] c : k.knabbels) {
                    helper.assertTrue(k.actief[c[0]][c[1]] && !DoolhofKaart.toren(c[0], c[1]) && k.afstand[c[0]][c[1]] >= 3,
                            "a knabbel lies in the maze, not at the start");
                }
                for (int[] c : k.nep) {
                    helper.assertTrue(k.buren(c[0], c[1]) == 1, "fake knabbels lie in dead ends");
                }
                for (int z = 0; z < k.off; z++) {
                    helper.assertTrue(k.gang[DoolhofKaart.MIDDEN][z], "the corridor leads from the exit to the gate");
                }
            }
        }
        // the exit corridor is open towards the gate, the rest of the fenced-off part is hedge
        DoolhofKaart k = new DoolhofKaart(Niveau.MAKKELIJK, 5);
        int bx = DoolhofVeld.P * DoolhofKaart.MIDDEN + 1;
        helper.assertTrue(!DoolhofVeld.dicht(k, bx, DoolhofVeld.P), "the corridor's wall line is open");
        helper.assertTrue(DoolhofVeld.dicht(k, 1, 1), "the fenced-off corner is hedge");
        helper.assertTrue(DoolhofVeld.dicht(k, 3, 3), "posts are always hedge");
        helper.succeed();
    }

    /** The anchor turns template spots into world spots and back, for all four ways a structure can be turned. */
    @GameTest(template = EMPTY)
    public static void doolhofAnkerDraaitMee(GameTestHelper helper) {
        BlockPos pos = new BlockPos(100, 64, -40);
        for (Direction f : Direction.Plane.HORIZONTAL) {
            Anker a = Anker.van(pos, f, 36, 3, 84);
            helper.assertTrue(a.blok(36, 3, 84).equals(pos), "the anchor itself");
            helper.assertTrue(a.richting(Direction.NORTH) == f, "north turns to " + f);
            BlockPos b = a.blok(40, 5, 70);
            Vec3 midden = a.punt(40.5, 5, 70.5);
            helper.assertTrue(Vec3.atBottomCenterOf(b).distanceTo(midden) < 1e-6, f + ": block and spot agree");
            Vec3 terug = a.lokaal(midden);
            helper.assertTrue(terug.distanceTo(new Vec3(40.5, 5, 70.5)) < 1e-6, f + ": and back: " + terug);
            Vec3 v = a.vector(0, -1);
            helper.assertTrue(Math.abs(v.x - f.getStepX()) < 1e-6 && Math.abs(v.z - f.getStepZ()) < 1e-6, f + ": the north vector");
            float yaw = a.yaw(180f);
            helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - f.toYRot())) < 1e-3, f + ": looking north = looking " + f);
            Anker saved = Anker.load(a.save(), 36, 3, 84);
            helper.assertTrue(saved.equals(a), "saved and loaded");
        }
        helper.assertTrue(Anker.rotatie(Direction.EAST) == Rotation.CLOCKWISE_90, "east = clockwise");
        helper.succeed();
    }

    /** Coins: the level's base, a speed bonus, lastig +50 %, and +1 like every reward. */
    @GameTest(template = EMPTY)
    public static void doolhofBeloningPlusEen(GameTestHelper helper) {
        int traag = 20 * 60 * 10;
        helper.assertTrue(DoolhofGame.munten(Niveau.MAKKELIJK, traag) == 2 + 1, "makkelijk, slow: 2 +1");
        helper.assertTrue(DoolhofGame.munten(Niveau.MEDIUM, DoolhofGame.PAR[1]) == 3 + 1 + 1, "medium on par: 3 + 1, +1");
        helper.assertTrue(DoolhofGame.munten(Niveau.LASTIG, 20) == (int) Math.ceil((3 + 2) * 1.5) + 1, "lastig, very fast: ceil(5 x 1.5) +1");
        helper.assertTrue(DoolhofGame.munten(Niveau.LASTIG, traag) == Niveau.LASTIG.munten(3) + 1, "lastig, slow");
        helper.assertTrue(DoolhofGame.board(Niveau.MEDIUM).equals("doolhof_medium") && DoolhofGame.board(Niveau.LASTIG).equals("doolhof_lastig"),
                "the Highscores boards");
        helper.succeed();
    }

    /** Meneer Vadskronkel's role and shop, the one source of the explorer's outfit, the loaned knabbel. */
    @GameTest(template = EMPTY)
    public static void doolhofRolWinkelEnKleding(GameTestHelper helper) {
        helper.assertTrue(Features.role(GuhNpcEntity.Kind.DOOLHOFGUH) instanceof DoolhofRole, "Meneer Vadskronkel has his role");
        var offers = new DoolhofRole().offers(null);
        for (GuhClothes c : List.of(GuhClothes.DOOLHOF_HOEDJE, GuhClothes.DOOLHOF_KOMPAS, GuhClothes.DOOLHOF_RUGZAKJE)) {
            boolean verkocht = false;
            for (MerchantOffer o : offers) {
                if (o.getResult().is(ModItems.clothingItem(c)) && o.getBaseCostA().is(DoolhofFeature.DOOLHOFKNABBEL.get())) {
                    verkocht = true;
                }
            }
            helper.assertTrue(verkocht, c + " is sold for doolhofknabbels");
            helper.assertTrue("doolhof".equals(KledingBronnen.bron(c)), c + " comes from the doolhof (only)");
            helper.assertTrue(KledingBronnen.prijs(c) != null, c + " has a price in the Guhdex");
        }
        helper.assertTrue(GuhClothes.DOOLHOF_RUGZAKJE.slot == GuhClothes.Slot.BACK && GuhClothes.DOOLHOF_KOMPAS.slot == GuhClothes.Slot.NECK
                && GuhClothes.DOOLHOF_HOEDJE.slot == GuhClothes.Slot.HEAD, "the slots");
        helper.assertTrue(Features.isLoaned(new ItemStack(DoolhofFeature.GESTOLEN_KNABBEL.get())), "the gestolen knabbel is loaned");
        helper.assertTrue(!Features.isLoaned(new ItemStack(DoolhofFeature.DOOLHOFKNABBEL.get())), "the coin is yours");
        helper.succeed();
    }

    /** Nobody breaks the maze (only creative); the lanterns know when it's night. */
    @GameTest(template = EMPTY)
    public static void doolhofBeschermdEnLantaarns(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(DoolhofProtection.denied(p, true), "a survival player can't break the maze");
        helper.assertTrue(!DoolhofProtection.denied(p, false), "outside it: fine");
        p.setGameMode(GameType.CREATIVE);
        helper.assertTrue(!DoolhofProtection.denied(p, true), "creative may");
        ServerLevel level = helper.getLevel();
        long oud = level.getDayTime();
        level.setDayTime(6000);
        boolean dag = DoolhofBlocks.Lantaarn.donker(level);
        level.setDayTime(18000);
        boolean nacht = DoolhofBlocks.Lantaarn.donker(level);
        level.setDayTime(oud);
        helper.assertTrue(!dag && nacht, "the lanterns are on at night only");
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    // --- the real building ------------------------------------------------------------------------------------------------

    /** The building: the anchor under Meneer Vadskronkel, the border, posts, tower and exit where DoolhofVeld expects them. */
    @GameTest(template = GEBOUW, timeoutTicks = 100, batch = "doolhof_gebouw")
    public static void doolhofGebouwKlopt(GameTestHelper helper) {
        GuhNpcEntity npc = vadskronkel(helper);
        Anker a = DoolhofVeld.anker(npc);
        helper.assertTrue(a != null, "the anchor is found under Meneer Vadskronkel");
        ServerLevel level = helper.getLevel();
        int F = DoolhofVeld.F, FX = DoolhofVeld.FX, FZ = DoolhofVeld.FZ, G = DoolhofVeld.G;
        helper.assertTrue(level.getBlockState(a.blok(FX, G + 2, FZ + 5)).is(DoolhofFeature.HEG.get())
                || level.getBlockState(a.blok(FX, G + 2, FZ + 5)).is(DoolhofFeature.HEG_GEZICHT.get()), "the west border is hedge");
        for (int i = 3; i < F - 1; i += 3) {
            helper.assertTrue(level.getBlockState(a.blok(FX + i, G + 3, FZ + 3)).is(DoolhofFeature.HEG.get()) || DoolhofVeld.toren(i, 3),
                    "a post at " + i + ": " + level.getBlockState(a.blok(FX + i, G + 3, FZ + 3)) + " anchor " + a);
        }
        int gate = FX + DoolhofVeld.P * DoolhofKaart.MIDDEN + 1;
        helper.assertTrue(level.getBlockState(a.blok(gate, G + 1, FZ)).isAir() && level.getBlockState(a.blok(gate + 1, G + 2, FZ)).isAir(),
                "the exit gate is open");
        helper.assertTrue(level.getBlockState(a.blok(gate, G + 1, FZ + F - 1)).is(DoolhofFeature.HEG.get()), "the entrance is closed");
        helper.assertTrue(level.getBlockState(a.blok(FX + DoolhofVeld.TOREN_A, G + 2, FZ + DoolhofVeld.TOREN_A + 3)).is(Blocks.PINK_WOOL),
                "the lookout guh stands on the tower cells");
        Vec3 uit = a.punt(gate + 1.0, G + 1, FZ - 2.0);
        helper.assertTrue(DoolhofVeld.bijUitgang(a, uit), "in front of the gate is the exit");
        helper.assertTrue(!DoolhofVeld.bijUitgang(a, DoolhofVeld.cel(a, 9, 18, G + 1)), "the start isn't");
        int[] c = DoolhofVeld.celVan(a, DoolhofVeld.cel(a, 4, 7, G + 1));
        helper.assertTrue(c != null && c[0] == 4 && c[1] == 7, "a cell's middle is in that cell");
        helper.succeed();
    }

    /** A whole makkelijk game: the maze grows, 8 knabbels and a Mika, find them all, out of the exit: coins, record, board. */
    @GameTest(template = GEBOUW, timeoutTicks = 400, batch = "doolhof_spel")
    public static void doolhofHeelSpel(GameTestHelper helper) {
        GuhNpcEntity npc = vadskronkel(helper);
        ServerPlayer p = speler(helper, npc);
        ServerLevel level = helper.getLevel();
        DoolhofGame game = DoolhofGame.start(npc, p, Niveau.MAKKELIJK);
        helper.assertTrue(game != null && DoolhofGame.isPlaying(p) && "doolhof".equals(Minigames.playing(p)), "the game started");
        helper.assertTrue(DoolhofGame.start(npc, p, Niveau.MEDIUM) == null, "one game at a time");
        game.meteen(level, p);
        helper.assertTrue(game.fase() == DoolhofGame.Fase.SPELEN, "playing after the growing and the countdown");
        Anker a = game.anker;
        // the hedges follow the plan: every maze cell is a path now, the fenced-off corner hedge
        for (int[] c : game.kaart.cellen()) {
            BlockPos m = BlockPos.containing(DoolhofVeld.cel(a, c[0], c[1], DoolhofVeld.G + 1).add(-0.5, 0, -0.5));
            helper.assertTrue(level.getBlockState(m).isAir(), "maze cell " + c[0] + "," + c[1] + " is open");
        }
        helper.assertTrue(level.getBlockState(a.blok(DoolhofVeld.FX + 1, DoolhofVeld.G + 1, DoolhofVeld.FZ + 1)).is(DoolhofFeature.HEG.get()),
                "the fenced-off part of the field is hedge");
        helper.assertTrue(p.position().distanceTo(game.start()) < 1.5, "the player stands at the start");
        long mikas = level.getEntitiesOfClass(DoolhofMikaEntity.class, DoolhofVeld.veld(a)).size();
        helper.assertTrue(mikas == 1, "one Mika on makkelijk: " + mikas);
        helper.assertTrue(game.knabbels.size() == 8, "8 knabbels in the maze: " + game.knabbels.size());
        // pick them all up
        for (Map.Entry<java.util.UUID, Boolean> e : List.copyOf(game.knabbels.entrySet())) {
            ItemEntity item = (ItemEntity) level.getEntity(e.getKey());
            helper.assertTrue(item != null && item.isNoGravity(), "a floating knabbel");
            game.knabbels.remove(e.getKey());
            game.pak(level, p, item, e.getValue());
        }
        helper.assertTrue(game.inZak() == 8 && tel(p, DoolhofFeature.GESTOLEN_KNABBEL.get()) == 8, "8 knabbels in the bag");
        // out through the exit
        Vec3 uit = a.punt(DoolhofVeld.FX + DoolhofVeld.P * DoolhofKaart.MIDDEN + 2.0, DoolhofVeld.G + 1, DoolhofVeld.FZ - 2.0);
        p.moveTo(uit.x, uit.y, uit.z);
        helper.succeedWhen(() -> {
            helper.assertTrue(!DoolhofGame.isPlaying(p), "the game ended at the exit");
            int munten = tel(p, DoolhofFeature.DOOLHOFKNABBEL.get());
            helper.assertTrue(munten >= DoolhofGame.munten(Niveau.MAKKELIJK, 20 * 60 * 30) && munten <= DoolhofGame.munten(Niveau.MAKKELIJK, 1),
                    "doolhofknabbels: " + munten);
            helper.assertTrue(tel(p, DoolhofFeature.GESTOLEN_KNABBEL.get()) == 0, "the gestolen knabbels went back");
            helper.assertTrue(DoolhofGame.best(p, Niveau.MAKKELIJK) >= 0, "a record");
            helper.assertTrue(Scorebord.top(level.getServer(), "doolhof_makkelijk").stream().anyMatch(s -> s.name().equals(p.getGameProfile().getName())),
                    "on the board");
            helper.assertTrue(level.getEntitiesOfClass(DoolhofMikaEntity.class, DoolhofVeld.veld(a), m -> m.isAlive()).isEmpty(), "the Mika's went poof");
            weg(helper, p);
        });
    }

    /**
     * 2.10: the knabbels float sparkling above the hedges (you see them from afar, you pick one up by walking under it), and
     * none ever gets lost: a knabbel whose entity went away is hidden again within a second (self-heal), a lost one that turns
     * up again is taken back. On makkelijk the bar counts how many are still hidden.
     */
    @GameTest(template = GEBOUW, timeoutTicks = 300, batch = "doolhof_heel")
    public static void doolhofKnabbelsZwevenEnRakenNooitZoek(GameTestHelper helper) {
        GuhNpcEntity npc = vadskronkel(helper);
        ServerPlayer p = speler(helper, npc);
        ServerLevel level = helper.getLevel();
        DoolhofGame game = DoolhofGame.start(npc, p, Niveau.MAKKELIJK);
        helper.assertTrue(game != null, "started");
        game.meteen(level, p);
        Anker a = game.anker;
        helper.assertTrue(game.verstopt() == 8, "8 knabbels hidden (the makkelijk counter): " + game.verstopt());
        for (java.util.UUID id : game.knabbels.keySet()) {
            ItemEntity item = (ItemEntity) level.getEntity(id);
            double h = DoolhofVeld.hoogte(a, item.getY());
            helper.assertTrue(item.isNoGravity() && h > DoolhofVeld.HEG_TOT + 1 && Math.abs(h - DoolhofGame.ZWEEF_Y) < 0.01,
                    "a knabbel floats just above the hedges: " + h);
        }
        // a knabbel whose entity is gone (its chunk went away, it got removed...) is hidden again
        java.util.UUID weg = game.knabbels.keySet().iterator().next();
        level.getEntity(weg).discard();
        helper.assertTrue(game.herstel(level) == 1 && game.verstopt() == 8 && !game.knabbels.containsKey(weg), "one hidden again: still 8");
        // a knabbel nobody keeps track of any more (it turned up again) is taken back, not doubled
        java.util.UUID los = game.knabbels.keySet().iterator().next();
        game.knabbels.remove(los);
        helper.assertTrue(game.herstel(level) == 0 && game.verstopt() == 8 && game.knabbels.containsKey(los), "the lost one is back in the game");
        // and two at once
        var twee = List.copyOf(game.knabbels.keySet()).subList(0, 2);
        for (java.util.UUID id : twee) {
            level.getEntity(id).discard();
        }
        helper.assertTrue(game.herstel(level) == 2 && game.verstopt() == 8, "two hidden again");
        long knabbelsInHetVeld = level.getEntitiesOfClass(ItemEntity.class, DoolhofVeld.veld(a).inflate(4),
                e -> e.getTags().contains(DoolhofGame.TAG) && e.isAlive()).size();
        helper.assertTrue(knabbelsInHetVeld == 8, "exactly 8 knabbels in the field, none extra: " + knabbelsInHetVeld);
        // the game heals by itself while you play (every second)
        java.util.UUID nogEen = game.knabbels.keySet().iterator().next();
        level.getEntity(nogEen).discard();
        helper.runAtTickTime(helper.getTick() + DoolhofGame.HERSTEL_TICKS + 2, () -> {
            helper.assertTrue(game.verstopt() == 8 && !game.knabbels.containsKey(nogEen), "hidden again by itself: " + game.verstopt());
            // walk under a floating knabbel: it's yours
            java.util.UUID doel = game.knabbels.keySet().iterator().next();
            ItemEntity item = (ItemEntity) level.getEntity(doel);
            int[] cel = DoolhofVeld.celVan(a, item.position());
            Vec3 onder = DoolhofVeld.cel(a, cel[0], cel[1], DoolhofVeld.G + 1);
            p.moveTo(onder.x, onder.y, onder.z);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(game.inZak() + game.gepikt >= 1, "picked up by walking under it");
            helper.assertTrue(game.inZak() + game.verstopt() == 8, "in the bag + still hidden = 8: " + game.inZak() + " + " + game.verstopt());
            weg(helper, p);
        });
    }

    /** A Heg-Mika touches you: one knabbel goes back into the maze, it runs off, and you're not hurt (neither is it). */
    @GameTest(template = GEBOUW, timeoutTicks = 200, batch = "doolhof_mika")
    public static void doolhofMikaPiktZonderPijn(GameTestHelper helper) {
        GuhNpcEntity npc = vadskronkel(helper);
        ServerPlayer p = speler(helper, npc);
        ServerLevel level = helper.getLevel();
        DoolhofGame game = DoolhofGame.start(npc, p, Niveau.MEDIUM);
        helper.assertTrue(game != null, "started");
        game.meteen(level, p);
        List<DoolhofMikaEntity> mikas = level.getEntitiesOfClass(DoolhofMikaEntity.class, DoolhofVeld.veld(game.anker));
        helper.assertTrue(mikas.size() == 2, "two Mika's on medium: " + mikas.size());
        DoolhofMikaEntity mika = mikas.get(0);
        // two knabbels in the bag
        for (int i = 0; i < 2; i++) {
            var e = game.knabbels.entrySet().iterator().next();
            ItemEntity item = (ItemEntity) level.getEntity(e.getKey());
            game.knabbels.remove(e.getKey());
            game.pak(level, p, item, false);
        }
        int inMaze = game.knabbels.size();
        float hp = p.getHealth();
        game.gepikt(mika, p);
        helper.assertTrue(game.inZak() == 1 && tel(p, DoolhofFeature.GESTOLEN_KNABBEL.get()) == 1, "one knabbel pinched");
        helper.assertTrue(game.knabbels.size() == inMaze + 1, "and hidden again in the maze");
        helper.assertTrue(!mika.kanPikken(), "the Mika runs off (it can't pinch again at once)");
        helper.assertTrue(p.getHealth() == hp, "nobody got hurt");
        helper.assertTrue(!mika.doHurtTarget(p), "a Mika never attacks");
        mika.hurt(level.damageSources().playerAttack(p), 10f);
        helper.assertTrue(mika.isAlive() && mika.getHealth() == mika.getMaxHealth(), "and it can't be hurt either");
        // with an empty bag it only sticks out its tongue
        game.inZak = 0;
        p.getInventory().clearContent();
        int nu = game.knabbels.size();
        game.gepikt(mika, p);
        helper.assertTrue(game.knabbels.size() == nu && game.inZak() == 0, "nothing to pinch");
        weg(helper, p);
        helper.succeedWhen(() -> helper.assertTrue(level.getEntitiesOfClass(DoolhofMikaEntity.class, DoolhofVeld.veld(game.anker), Entity::isAlive).isEmpty(),
                "the Mika's go when the game stops"));
    }

    /** Lastig: 16 knabbels, 3 Mika's, fake knabbels cost 5 seconds; at the exit without all knabbels you're sent back in. */
    @GameTest(template = GEBOUW, timeoutTicks = 200, batch = "doolhof_lastig")
    public static void doolhofLastigNepEnUitgang(GameTestHelper helper) {
        GuhNpcEntity npc = vadskronkel(helper);
        ServerPlayer p = speler(helper, npc);
        ServerLevel level = helper.getLevel();
        DoolhofGame game = DoolhofGame.start(npc, p, Niveau.LASTIG);
        helper.assertTrue(game != null, "started");
        game.meteen(level, p);
        long nep = game.knabbels.values().stream().filter(b -> b).count();
        helper.assertTrue(game.knabbels.size() == 16 + nep && nep == DoolhofKaart.aantalNep(Niveau.LASTIG), "16 real + " + nep + " fake knabbels");
        helper.assertTrue(level.getEntitiesOfClass(DoolhofMikaEntity.class, DoolhofVeld.veld(game.anker)).size() == 3, "three Mika's");
        var e = game.knabbels.entrySet().stream().filter(Map.Entry::getValue).findFirst().orElseThrow();
        ItemEntity item = (ItemEntity) level.getEntity(e.getKey());
        game.knabbels.remove(e.getKey());
        int voor = game.tijd();
        game.pak(level, p, item, true);
        helper.assertTrue(game.tijd() - voor == DoolhofGame.NEP_STRAF_TICKS && game.inZak() == 0, "a fake knabbel: +5 seconds, nothing in the bag");
        Vec3 uit = game.anker.punt(DoolhofVeld.FX + DoolhofVeld.P * DoolhofKaart.MIDDEN + 2.0, DoolhofVeld.G + 1, DoolhofVeld.FZ - 2.0);
        p.moveTo(uit.x, uit.y, uit.z);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(DoolhofGame.isPlaying(p), "without all knabbels the game goes on");
            helper.assertTrue(!DoolhofVeld.bijUitgang(game.anker, p.position()), "and you're back in the maze");
            weg(helper, p);
            helper.succeed();
        });
    }
}
