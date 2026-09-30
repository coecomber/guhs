package nl.juiced.guhs.feature.bakkerij;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Binnenkort;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Knabbelbakkerij: the recipes and tags, rewards (+1), baking at your own oven from ingredients, Korstje's order
 * game (customers walk to the counter, serving, combos, a wrong or late order, the feesttaart for the Knusfeest, the
 * end with bakmunten and the highscore), the customers' pastries that vanish, Korstje's role and shop, and the real
 * building (its markers, ovens, and a customer walking from the door to the counter).
 */
public class BakkerijGameTests {
    private static final String EMPTY = "empty";
    private static final String TEST = "bakkerij_test";
    private static final String GEBOUW = "knuffeldal_stadje/bakkerij";
    /** Each bakery test in a batch of its own: Korstje finds his bakery by its markers, and test bakeries stand close together. */
    private static final String BATCH = "bakkerij";

    private static GuhNpcEntity korstje(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(1),
                n -> n.getKind() == GuhNpcEntity.Kind.BAKKERGUH);
        helper.assertTrue(npcs.size() == 1, "there is one Bakker Korstje: " + npcs.size());
        GuhNpcEntity npc = npcs.get(0);
        onlyThisBakery(helper, npc);
        return npc;
    }

    /**
     * (merge 2.8) Korstje remembers only the bakery markers inside this test's own template: with the whole suite a
     * bakkerij_test of an earlier batch can still stand within his scan range, and then he'd find 5 spots and 5 ovens.
     */
    private static void onlyThisBakery(GameTestHelper helper, GuhNpcEntity npc) {
        net.minecraft.world.phys.AABB box = helper.getBounds();
        List<BlockPos> plekken = new java.util.ArrayList<>(), ingangen = new java.util.ArrayList<>(), ovens = new java.util.ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed((int) Math.floor(box.minX), (int) Math.floor(box.minY), (int) Math.floor(box.minZ),
                (int) Math.ceil(box.maxX) - 1, (int) Math.ceil(box.maxY) - 1, (int) Math.ceil(box.maxZ) - 1)) {
            var state = helper.getLevel().getBlockState(pos);
            if (state.is(BakkerijFeature.KLANTPLEK.get())) {
                plekken.add(pos.immutable());
            } else if (state.is(BakkerijFeature.INGANG.get())) {
                ingangen.add(pos.immutable());
            } else if (state.getBlock() instanceof KnabbelovenBlock) {
                ovens.add(pos.immutable());
            }
        }
        if (plekken.isEmpty() || ingangen.isEmpty() || ovens.isEmpty()) {
            return;     // (a template without a bakery: the game finds none, as it should)
        }
        plekken.sort(java.util.Comparator.comparingInt((BlockPos p) -> p.getX()).thenComparingInt(p -> p.getZ()));
        BlockPos ingang = ingangen.stream().min(java.util.Comparator.comparingDouble(p -> p.distSqr(npc.blockPosition()))).orElseThrow();
        npc.roleData.putLong("BakIngang", ingang.asLong());
        npc.roleData.put("BakPlekken", new net.minecraft.nbt.LongArrayTag(plekken.stream().mapToLong(BlockPos::asLong).toArray()));
        npc.roleData.put("BakOvens", new net.minecraft.nbt.LongArrayTag(ovens.stream().mapToLong(BlockPos::asLong).toArray()));
    }

    private static ServerPlayer player(GameTestHelper helper, Vec3 at) {
        @SuppressWarnings("removal")
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.snapTo(at.x, at.y, at.z);
        GuhQuests.saved(p).remove(BakkerijGame.PLAYED_KEY);
        GuhQuests.saved(p).remove(BakkerijGame.BEST_KEY);
        GuhQuests.saved(p).remove(KnusVoortgang.KEY);
        Knusfeest.vergeet(p);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            BakkerijGame.stopFor(p);
            Bakken.vergeet(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static BlockPos oven(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(helper.getBounds().minX, helper.getBounds().minY, helper.getBounds().minZ),
                BlockPos.containing(helper.getBounds().maxX, helper.getBounds().maxY, helper.getBounds().maxZ))) {
            if (helper.getLevel().getBlockState(pos).getBlock() instanceof KnabbelovenBlock) {
                return pos.immutable();
            }
        }
        throw new net.minecraft.gametest.framework.GameTestAssertException("no knabbeloven");
    }

    private static int count(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    /** Twelve different recipes (plus the feesttaart), all gebak; the feesttaart and the bakmunt in their Knus tags. */
    @GuhTest(template = EMPTY)
    public static void bakkerijReceptenEnTags(GameTestHelper helper) {
        Set<String> combos = new HashSet<>();
        for (Recept r : Recept.values()) {
            helper.assertTrue(combos.add(r.deeg + "/" + r.vorm + "/" + r.topping), "one combination per recipe: " + r);
            helper.assertTrue(Recept.van(r.deeg, r.vorm, r.topping) == r, "the combination finds its recipe: " + r);
            helper.assertTrue(new ItemStack(BakkerijFeature.bakje(r)).is(KnusTags.GEBAK), r + " is gebak");
        }
        helper.assertTrue(Recept.BOEK.size() == 12 && !Recept.BOEK.contains(Recept.FEESTTAART), "twelve in the receptenboek");
        helper.assertTrue(new ItemStack(BakkerijFeature.FEESTTAART.get()).is(KnusTags.FEESTTAART), "the feesttaart is the task item");
        helper.assertTrue(new ItemStack(BakkerijFeature.BAKMUNT.get()).is(KnusTags.GRIJPTICKETS), "a bakmunt pays for the grijpmachine");
        helper.assertTrue(new ItemStack(BakkerijFeature.bakje(Recept.KAASBOLLETJE)).is(KnusTags.LEKKERNIJ), "pastries lure Kruimel-Mika's");
        var boek = KnusVoortgang.verzameling(BakkerijVoortgang.RECEPTENBOEK);
        helper.assertTrue(boek != null && boek.items().size() == 12 && boek.onderdeel().equals("bakkerij"), "the receptenboek page");
        helper.assertTrue(KnusVoortgang.mijlpalen("bakkerij").size() >= 4, "at least four bakery milestones");
        // the oven's windows
        helper.assertTrue(Recept.Kwaliteit.bij(10) == Recept.Kwaliteit.RAUW && Recept.Kwaliteit.bij(60) == Recept.Kwaliteit.GOED
                && Recept.Kwaliteit.bij(75) == Recept.Kwaliteit.PERFECT && Recept.Kwaliteit.bij(85) == Recept.Kwaliteit.GOED
                && Recept.Kwaliteit.bij(95) == Recept.Kwaliteit.AANGEBRAND, "raw, good, perfect, good, burnt");
        helper.assertTrue(BakkerijGame.combo(0) == 1 && BakkerijGame.combo(3) == 2 && BakkerijGame.combo(6) == 3 && BakkerijGame.combo(20) == 4,
                "combo x2 from 3, x3 from 6, at most x4");
        helper.assertTrue(BakkerijGame.punten(Recept.VADSDONUT, Recept.Kwaliteit.PERFECT, 1f) == 25
                && BakkerijGame.punten(Recept.VADSDONUT, Recept.Kwaliteit.RAUW, 0f) == 5, "points per order");
        helper.succeed();
    }

    /** Every reward rule gives one more (like all minigames since 2.7): bakmunten per score and the first-game bonus. */
    @GuhTest(template = EMPTY)
    public static void bakkerijBeloningenEenMeer(GameTestHelper helper) {
        for (int score = 0; score <= 800; score++) {
            int old = score <= 0 ? 0 : Math.min(12, 1 + score / 40);
            helper.assertTrue(BakkerijGame.munten(score) == (score > 0 ? old + 1 : 0), "bakmunten for " + score + ": " + BakkerijGame.munten(score));
        }
        helper.assertTrue(BakkerijGame.FIRST_BONUS == 3 + 1, "first game bonus");
        helper.succeed();
    }

    /** Your own oven: takes the ingredients (vanilla ones work too), bakes with timing, and fills the receptenboek. */
    @GuhTest(template = TEST, timeoutTicks = 200, batch = BATCH + "_oven")
    public static void bakkerijEigenOven(GameTestHelper helper) {
        BlockPos oven = oven(helper);
        ServerPlayer p = player(helper, Vec3.atBottomCenterOf(oven.south()));
        helper.assertTrue("gui.guhs.bakkerij.te_weinig".equals(Bakken.start(p, oven, Recept.Deeg.ZOETDEEG, Recept.Vorm.BOLLETJE, Recept.Topping.GLAZUUR)),
                "no ingredients: no baking");
        helper.assertTrue("gui.guhs.bakkerij.geheim".equals(Bakken.start(p, oven, Recept.Deeg.ZOETDEEG, Recept.Vorm.TAARTJE, Recept.Topping.GLAZUUR)),
                "the feesttaart is Korstje's secret");
        p.getInventory().add(new ItemStack(Items.WHEAT, 2));
        p.getInventory().add(new ItemStack(Items.SUGAR, 3));
        p.getInventory().add(new ItemStack(Items.EGG, 2));
        p.getInventory().add(new ItemStack(Items.PINK_DYE, 1));
        helper.assertTrue(Bakken.start(p, oven, Recept.Deeg.ZOETDEEG, Recept.Vorm.BOLLETJE, Recept.Topping.GLAZUUR) == null, "a pluismuffin bakes");
        helper.assertTrue(count(p, Items.WHEAT) == 1 && count(p, Items.SUGAR) == 1 && count(p, Items.EGG) == 1 && count(p, Items.PINK_DYE) == 0,
                "wheat, 2 sugar, an egg and pink dye are used");
        helper.assertTrue(helper.getLevel().getBlockState(oven).getValue(KnabbelovenBlock.LIT), "the oven glows");
        helper.assertTrue("gui.guhs.bakkerij.al_bezig".equals(Bakken.start(p, oven, Recept.Deeg.KNABBELDEEG, Recept.Vorm.PLAATJE, Recept.Topping.SUIKER)),
                "one bake at a time");
        long start = helper.getLevel().getGameTime();
        int perfect = Recept.PLUISMUFFIN.bakTicks * 76 / 100;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(helper.getLevel().getGameTime() - start >= perfect, "baking..."))
                .thenExecute(() -> {
                    Bakken.Uit uit = Bakken.eruit(p, perfect);
                    helper.assertTrue(uit != null && uit.kwaliteit() == Recept.Kwaliteit.PERFECT && uit.aantal() == Bakken.PERFECT_AANTAL,
                            "taken out at the right moment: perfect, 3 of them: " + uit);
                    helper.assertTrue(count(p, BakkerijFeature.bakje(Recept.PLUISMUFFIN)) == 3, "three pluismuffins");
                    helper.assertTrue(KnusVoortgang.heeft(p, BakkerijVoortgang.RECEPTENBOEK, "pluismuffin")
                            && KnusVoortgang.teller(p, BakkerijVoortgang.ZELF_GEBAKKEN) == 3, "in the receptenboek, and counted");
                    ItemStack muffin = p.getInventory().getItem(p.getInventory().findSlotMatchingItem(new ItemStack(BakkerijFeature.bakje(Recept.PLUISMUFFIN))));
                    helper.assertTrue(!BakjeItem.isSpel(muffin) && muffin.getFoodProperties(p) != null, "your own pastry is food");
                })
                .thenExecute(() -> {
                    helper.assertTrue(Bakken.start(p, oven, Recept.Deeg.KNABBELDEEG, Recept.Vorm.STERRETJE, Recept.Topping.GLAZUUR) != null,
                            "no knabbels, no second bake");
                    leave(helper, p);
                })
                .thenSucceed();
    }

    /**
     * A forgotten bake burns by itself (the player tick listener): after bakTicks + VERGETEN it's burnt, you get nothing
     * but a message, the oven goes out, and you're free to bake again at any oven.
     */
    @GuhTest(template = TEST, timeoutTicks = 300, batch = BATCH + "_vergeten")
    public static void bakkerijVergetenBakBrandtAan(GameTestHelper helper) {
        BlockPos oven = oven(helper);
        ServerPlayer p = player(helper, Vec3.atBottomCenterOf(oven.south()));
        p.getInventory().add(new ItemStack(Items.WHEAT, 2));
        p.getInventory().add(new ItemStack(Items.SUGAR, 4));
        p.getInventory().add(new ItemStack(Items.EGG, 2));
        p.getInventory().add(new ItemStack(Items.PINK_DYE, 2));
        helper.assertTrue(Bakken.start(p, oven, Recept.Deeg.ZOETDEEG, Recept.Vorm.BOLLETJE, Recept.Topping.GLAZUUR) == null, "a pluismuffin bakes");
        long start = helper.getLevel().getGameTime();
        helper.startSequence()
                .thenWaitUntil(() -> {
                    // (a mock player has no connection that ticks it: post its tick event like the real server does)
                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
                    helper.assertTrue(!Bakken.bakt(p), "forgotten in the oven...");
                })
                .thenExecute(() -> {
                    long ticks = helper.getLevel().getGameTime() - start;
                    helper.assertTrue(ticks > Recept.PLUISMUFFIN.bakTicks + Bakken.VERGETEN, "it burns only after the bar and VERGETEN: " + ticks);
                    helper.assertTrue(count(p, BakkerijFeature.bakje(Recept.PLUISMUFFIN)) == 0, "burnt: no pluismuffins (njeg)");
                    helper.assertTrue(!KnusVoortgang.heeft(p, BakkerijVoortgang.RECEPTENBOEK, "pluismuffin"), "a burnt one isn't in the receptenboek");
                    helper.assertTrue(!Bakken.bakt(helper.getLevel(), oven), "nothing bakes in the oven any more");
                })
                .thenWaitUntil(() -> helper.assertTrue(!helper.getLevel().getBlockState(oven).getValue(KnabbelovenBlock.LIT), "the oven goes out"))
                .thenExecute(() -> {
                    helper.assertTrue(Bakken.start(p, oven, Recept.Deeg.ZOETDEEG, Recept.Vorm.BOLLETJE, Recept.Topping.GLAZUUR) == null,
                            "free again: the next bake goes in");
                    leave(helper, p);
                })
                .thenSucceed();
    }

    /** The order game: customers walk to the counter, serving scores with combos, a wrong or late order, the end. */
    @GuhTest(template = TEST, timeoutTicks = 600, batch = BATCH + "_spel")
    public static void bakkerijSpelServerenEnCombo(GameTestHelper helper) {
        GuhNpcEntity npc = korstje(helper);
        ServerPlayer p = player(helper, npc.position().add(0, 0, 1));
        ServerPlayer other = player(helper, npc.position().add(1, 0, 1));
        helper.assertTrue(BakkerijGame.start(npc, p), "the game starts");
        BakkerijGame game = BakkerijGame.of(npc);
        game.klanten = false;
        helper.assertTrue(BakkerijGame.isPlaying(p) && Minigames.BAKKERIJ.equals(Minigames.playing(p)), "playing the bakkerij game");
        helper.assertTrue(!BakkerijGame.start(npc, other) && !BakkerijGame.isPlaying(other), "one baker at a time");
        helper.assertTrue(game.winkel().plekken().size() == 3 && game.winkel().ovens().size() == 3, "the bakery is found: " + game.winkel());
        BlockPos oven = game.winkel().ovens().stream().min(java.util.Comparator.comparingDouble(o -> o.distSqr(npc.blockPosition()))).orElseThrow();
        helper.assertTrue(p.blockPosition().equals(oven.south()), "you're put in front of an oven: " + p.blockPosition());
        ServerLevel world = helper.getLevel();
        BakkerijKlant[] klant = new BakkerijKlant[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!game.counting(), "the countdown"))
                .thenExecute(() -> klant[0] = game.klant(world, Recept.KAASBOLLETJE))
                .thenWaitUntil(() -> helper.assertTrue(klant[0].wacht(), "the customer walks to the counter"))
                .thenExecute(() -> {
                    helper.assertTrue(game.winkel().plekken().stream().anyMatch(pl -> klant[0].position().distanceTo(Vec3.atBottomCenterOf(pl)) < 1.2),
                            "it waits on a spot at the counter");
                    helper.assertTrue(!BakkerijGame.serveer(p, klant[0]), "nothing to give: it tells you what it wants");
                    game.gebakken(p, Recept.KAASBOLLETJE, Recept.Kwaliteit.PERFECT);
                    helper.assertTrue(BakjeItem.isSpel(p.getMainHandItem()) && BakjeItem.recept(p.getMainHandItem()) == Recept.KAASBOLLETJE,
                            "the pastry is in your hand, for the customer");
                    helper.assertTrue(p.getMainHandItem().getItem().use(world, p, InteractionHand.MAIN_HAND).getResult() == net.minecraft.world.InteractionResult.FAIL,
                            "a customer's pastry can't be eaten");
                    helper.assertTrue(BakkerijGame.serveer(p, klant[0]), "served");
                    helper.assertTrue(game.score() >= 24 && game.score() <= 25 && game.streak() == 1, "perfect and quick: 25 points: " + game.score());
                    helper.assertTrue(!BakjeItem.isSpel(p.getMainHandItem()), "the pastry is given away");
                    helper.assertTrue(KnusVoortgang.teller(p, BakkerijVoortgang.BESTELLINGEN) == 1
                            && KnusVoortgang.heeft(p, BakkerijVoortgang.RECEPTENBOEK, "kaasbolletje"), "counted, and in the receptenboek");
                    klant[0] = game.klant(world, Recept.VADSDONUT);
                })
                .thenWaitUntil(() -> helper.assertTrue(klant[0].wacht(), "the next customer: " + klant[0].staat() + " at " + klant[0].position()))
                .thenExecute(() -> {
                    int before = game.score();
                    game.gebakken(p, Recept.GUHWAFEL, Recept.Kwaliteit.GOED);
                    helper.assertTrue(!BakkerijGame.serveer(p, klant[0]) && game.score() == before && game.streak() == 0,
                            "a wrong pastry: no points, the combo is gone");
                    game.gebakken(p, Recept.VADSDONUT, Recept.Kwaliteit.GOED);
                    helper.assertTrue(BakkerijGame.serveer(p, klant[0]) && game.streak() == 1, "the right one from your pockets");
                    helper.assertTrue(count(p, BakkerijFeature.bakje(Recept.GUHWAFEL)) == 1, "the wrong one is still yours (for another customer)");
                    klant[0] = game.klant(world, Recept.STERRENKOEKJE);
                })
                .thenWaitUntil(() -> helper.assertTrue(klant[0].wacht(), "a third customer: " + klant[0].debug() + " spots " + game.winkel().plekken() + " player " + p.blockPosition()))
                .thenExecute(() -> klant[0].geduldOver(3))
                .thenWaitUntil(() -> helper.assertTrue(!klant[0].bezet() && game.gemist == 1 && game.streak() == 0,
                        "waited too long: it goes home (sad, never angry), the combo is gone"))
                .thenExecute(() -> {
                    int score = game.score();
                    game.finish(npc, p);
                    helper.assertTrue(!BakkerijGame.isPlaying(p) && BakkerijGame.of(npc) == null, "the game is over");
                    helper.assertTrue(count(p, BakkerijFeature.BAKMUNT.get()) == BakkerijGame.munten(score) + BakkerijGame.FIRST_BONUS,
                            "bakmunten + the first-game bonus: " + count(p, BakkerijFeature.BAKMUNT.get()));
                    helper.assertTrue(count(p, BakkerijFeature.bakje(Recept.GUHWAFEL)) == 0, "the customers' pastries are gone");
                    helper.assertTrue(BakkerijGame.best(p) == score && GuhQuests.saved(p).getCompoundOrEmpty(Highscores.KEY).getIntOr(BakkerijGame.BOARD, 0) == score,
                            "the record, also on the Guhdex Highscores page");
                    helper.assertTrue(KnusVoortgang.teller(p, BakkerijVoortgang.HIGHSCORE) == score, "the Knus highscore milestone counter");
                })
                .thenWaitUntil(() -> helper.assertTrue(game.klanten(world).isEmpty(), "the customers go home"))
                .thenExecute(() -> leave(helper, p, other))
                .thenSucceed();
    }

    /** The Grote Knusfeest: with the task open a feestklant orders the feesttaart; serving it gives the real one. */
    @GuhTest(template = TEST, timeoutTicks = 400, batch = BATCH + "_feest")
    public static void bakkerijFeesttaartVoorHetKnusfeest(GameTestHelper helper) {
        GuhNpcEntity npc = korstje(helper);
        ServerPlayer p = player(helper, npc.position().add(0, 0, 1));
        Knusfeest.nieuweRonde(p, 0, EnumSet.of(Feesttaak.FEESTTAART));
        helper.assertTrue(Knusfeest.open(p, Feesttaak.FEESTTAART), "the Burgemeester asked for the feesttaart");
        helper.assertTrue(BakkerijGame.start(npc, p), "started");
        BakkerijGame game = BakkerijGame.of(npc);
        game.klanten = false;
        helper.assertTrue(game.feest(), "a feest game");
        ServerLevel world = helper.getLevel();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!game.counting(), "the countdown"))
                .thenExecute(() -> game.score = BakkerijGame.FEEST_MIN)
                .thenWaitUntil(() -> helper.assertTrue(game.feestKlantGekomen && game.klanten(world).stream().anyMatch(k -> k.recept() == Recept.FEESTTAART),
                        "with enough points the feestklant comes"))
                .thenWaitUntil(() -> helper.assertTrue(game.klanten(world).stream().anyMatch(k -> k.recept() == Recept.FEESTTAART && k.wacht()), "she waits"))
                .thenExecute(() -> {
                    BakkerijKlant feest = game.klanten(world).stream().filter(k -> k.recept() == Recept.FEESTTAART).findFirst().orElseThrow();
                    game.gebakken(p, Recept.FEESTTAART, Recept.Kwaliteit.GOED);
                    helper.assertTrue(BakkerijGame.serveer(p, feest), "the feesttaart is served");
                    helper.assertTrue(count(p, BakkerijFeature.FEESTTAART.get()) == 1, "Korstje packs a real feesttaart for you");
                    helper.assertTrue(Knusfeest.stap(p, Feesttaak.FEESTTAART) == Knusfeest.Stap.GEMAAKT, "the task is made");
                    helper.assertTrue(KnusVoortgang.teller(p, BakkerijVoortgang.FEESTTAART) == 1, "the milestone counts it");
                    ItemStack taart = p.getInventory().getItem(p.getInventory().findSlotMatchingItem(new ItemStack(BakkerijFeature.FEESTTAART.get())));
                    helper.assertTrue(!BakjeItem.isSpel(taart), "the real feesttaart stays after the game");
                    BakkerijGame.stopFor(p);
                    helper.assertTrue(count(p, BakkerijFeature.FEESTTAART.get()) == 1, "and it does");
                    Knusfeest.vergeet(p);
                    leave(helper, p);
                })
                .thenSucceed();
    }

    /** A customer's pastry outside the game vanishes (and a dropped one too). */
    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void bakkerijKlantgebakVerdwijnt(GameTestHelper helper) {
        ServerPlayer p = player(helper, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))));
        p.getInventory().add(BakjeItem.voorKlant(Recept.GUHWAFEL, Recept.Kwaliteit.GOED));
        p.getInventory().add(new ItemStack(BakkerijFeature.bakje(Recept.GUHWAFEL)));
        helper.startSequence()
                .thenWaitUntil(() -> {
                    p.getInventory().tick();                           // (a mock player's inventory isn't ticked by a connection)
                    helper.assertTrue(count(p, BakkerijFeature.bakje(Recept.GUHWAFEL)) == 1, "the game pastry is gone, your own stays");
                })
                .thenExecute(() -> leave(helper, p))
                .thenSucceed();
    }

    /** A customer from /summon (the autocheck): it waits where it stands with its order and patience, without a game. */
    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void bakkerijKlantVanSummonWacht(GameTestHelper helper) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", "guhs:bakkerij_klant");
        tag.putString("Recept", "vadsdonut");
        tag.putInt("Geduld", 600);
        tag.putInt("GeduldOver", 300);
        tag.putBoolean("Wacht", true);
        helper.setBlock(new BlockPos(1, 0, 1), net.minecraft.world.level.block.Blocks.STONE);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        Entity e = net.minecraft.world.entity.EntityType.loadEntityRecursive(tag, helper.getLevel(), en -> {
            en.moveTo(at.x, at.y, at.z, 0, 0);
            return en;
        });
        helper.assertTrue(e instanceof BakkerijKlant && helper.getLevel().addFreshEntity(e), "summoned");
        BakkerijKlant k = (BakkerijKlant) e;
        helper.assertTrue(k.recept() == Recept.VADSDONUT && k.wacht() && k.geduld() > 0.45f && k.geduld() <= 0.5f,
                "it wants a vadsdonut and is waiting, half its patience left: " + k.debug() + " " + k.geduld());
        long start = helper.getLevel().getGameTime();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(helper.getLevel().getGameTime() - start >= 40, "waiting..."))
                .thenExecute(() -> {
                    helper.assertTrue(k.isAlive() && k.wacht() && k.position().distanceTo(at) < 0.5, "it stays where it was summoned: " + k.debug());
                    helper.assertTrue(k.geduld() < 0.45f, "its patience goes down: " + k.geduld());
                    k.discard();
                })
                .thenSucceed();
    }

    /** Korstje's role (not the placeholder), his shop in bakmunten with the baker's outfit and an oven. */
    @GuhTest(template = EMPTY)
    public static void bakkerijKorstjeEnWinkel(GameTestHelper helper) {
        var role = Features.role(GuhNpcEntity.Kind.BAKKERGUH);
        helper.assertTrue(role instanceof BakkerijRole && role != Binnenkort.ROLE, "Bakker Korstje has his own role");
        var offers = role.offers(null);
        Set<Item> sold = new HashSet<>();
        for (MerchantOffer o : offers) {
            helper.assertTrue(o.getCostA().is(BakkerijFeature.BAKMUNT.get()), "paid with bakmunten");
            sold.add(o.getResult().getItem());
        }
        for (GuhClothes c : List.of(GuhClothes.BAKKERSMUTSJE, GuhClothes.BAKKERSSCHORTJE, GuhClothes.MEELSTRIKJE)) {
            helper.assertTrue(sold.contains(ModItems.clothingItem(c)), "sells " + c);
        }
        helper.assertTrue(sold.contains(BakkerijFeature.KNABBELOVEN_ITEM.get()), "sells a knabbeloven");
        helper.succeed();
    }

    /** The real building: Korstje, three customer spots, the door and three ovens; a customer walks from the door to the counter. */
    @GuhTest(template = GEBOUW, timeoutTicks = 800, batch = "bakkerij_gebouw")
    public static void bakkerijGebouwKlantLooptNaarDeToonbank(GameTestHelper helper) {
        GuhNpcEntity npc = korstje(helper);
        for (String key : List.of("BakIngang", "BakPlekken", "BakOvens")) {   // the real building: Korstje scans it himself
            npc.roleData.remove(key);
        }
        BakkerijGame.Winkel winkel = BakkerijGame.winkel(npc);
        helper.assertTrue(winkel != null && winkel.plekken().size() == 3 && winkel.ovens().size() == 3, "the bakery is complete: " + winkel);
        ServerPlayer p = player(helper, npc.position().add(0, 0, 1));
        helper.assertTrue(BakkerijGame.start(npc, p), "started");
        BakkerijGame game = BakkerijGame.of(npc);
        game.klanten = false;
        ServerLevel world = helper.getLevel();
        BakkerijKlant[] klanten = new BakkerijKlant[3];
        long[] binnen = new long[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!game.counting(), "the countdown"))
                .thenExecute(() -> {
                    for (int i = 0; i < 3; i++) {
                        klanten[i] = game.klant(world, Recept.BOEK.get(i));
                    }
                    helper.assertTrue(game.klant(world, Recept.GUHWAFEL) == null, "three spots: a fourth customer waits outside");
                    binnen[0] = world.getGameTime();
                })
                // (a customer that's stuck is teleported after VAST ticks: stop polling as soon as one is, so that fails below)
                .thenWaitUntil(() -> helper.assertTrue(java.util.Arrays.stream(klanten).allMatch(BakkerijKlant::wacht)
                        || java.util.Arrays.stream(klanten).anyMatch(k -> k.geholpen > 0), "the customers walk in"))
                .thenExecute(() -> {
                    long ticks = world.getGameTime() - binnen[0];
                    for (BakkerijKlant k : klanten) {
                        helper.assertTrue(k.geholpen == 0, "every customer really walks from the door (never helped along): " + k.debug());
                        helper.assertTrue(k.wacht() && winkel.plekken().stream().anyMatch(pl -> k.position().distanceTo(Vec3.atBottomCenterOf(pl)) < 1.2),
                                "every customer walks from the door to its spot: " + k.debug());
                    }
                    helper.assertTrue(ticks < BakkerijKlant.VAST, "all three at the counter before anyone would be helped along: " + ticks + " ticks");
                })
                .thenExecute(() -> {
                    game.gebakken(p, Recept.BOEK.get(0), Recept.Kwaliteit.GOED);
                    helper.assertTrue(BakkerijGame.serveer(p, klanten[0]), "served across the counter");
                    leave(helper, p);
                })
                .thenSucceed();
    }
}
