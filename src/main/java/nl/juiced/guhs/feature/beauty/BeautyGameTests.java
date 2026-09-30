package nl.juiced.guhs.feature.beauty;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.item.GuhClothingItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Tests of the Guh Beauty Vads-wedstrijd: the jury, a whole show without any items of your own, borrowing your own guh
 * (and getting it back, also after a crash), one show at a time, the shop, and the real theatre template.
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class BeautyGameTests {
    private static final String EMPTY = "empty";
    private static final BlockPos MODEL = new BlockPos(1, 1, 1), END = new BlockPos(1, 1, 4), NPC = new BlockPos(3, 1, 1);

    /** A mini theatre in the empty room: the two stage markers and a Showguh. */
    private static GuhNpcEntity miniTheatre(GameTestHelper helper) {
        helper.setBlock(MODEL, BeautyFeature.PLEK.get().defaultBlockState().setValue(BeautyBlocks.Plek.SPOT, BeautyBlocks.Spot.MODEL));
        helper.setBlock(END, BeautyFeature.PLEK.get().defaultBlockState().setValue(BeautyBlocks.Plek.SPOT, BeautyBlocks.Spot.EINDE));
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), NPC);
        npc.setKind(GuhNpcEntity.Kind.SHOWGUH);
        return npc;
    }

    private static ServerPlayer player(GameTestHelper helper, GuhNpcEntity npc) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(npc.getX() + 1, npc.getY(), npc.getZ());
        return player;
    }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        return GuhQuests.count(player, item);
    }

    private static boolean hasClothes(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() instanceof GuhClothingItem) {
                return true;
            }
        }
        return false;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean advancement(ServerPlayer player, String name) {
        var holder = player.server.getAdvancements().get(Guhs.id("quest/" + name));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    @GameTest(template = EMPTY)
    public static void beautyJuryLovesTheTheme(GameTestHelper helper) {
        RandomSource random = RandomSource.create(7);
        var winter = BeautyJury.judge(ShowTheme.WINTER, List.of(GuhClothes.SANTA_HAT, GuhClothes.CHRISTMAS_SWEATER, GuhClothes.WINTER_SCARF),
                false, random);
        helper.assertTrue(winter.scores()[0] == 10, "a perfect winter outfit: Juf Vadsma gives 10, not " + winter.scores()[0]);
        helper.assertTrue(winter.scores()[1] == 8, "3 pieces of one set: Meneer Glitterguh gives 8, not " + winter.scores()[1]);
        var spooky = BeautyJury.judge(ShowTheme.WINTER, List.of(GuhClothes.PUMPKIN_HEAD, GuhClothes.GHOST_SHEET, GuhClothes.EYEPATCH,
                GuhClothes.STETHOSCOPE), false, random);
        helper.assertTrue(spooky.scores()[0] == 1 && spooky.offTheme() == 4, "a spooky outfit on a winter show: 1 point for the theme");
        helper.assertTrue(spooky.scores()[1] == 8, "complete, a spooky set, but a mess for winter: 9 + 1 - 2 = 8, not " + spooky.scores()[1]);
        helper.assertTrue(winter.total() > spooky.total() || winter.scores()[2] < spooky.scores()[2] - 5, "the theme wins");
        var naked = BeautyJury.judge(ShowTheme.GALA, List.of(), false, random);
        helper.assertTrue(naked.scores()[0] == 1 && naked.scores()[1] == 1 && naked.rosettes() == 0, "a naked model gets nothing");
        var showster = BeautyJury.judge(ShowTheme.GALA, ShowTheme.SHOWSTER, true, RandomSource.create(1));
        var plain = BeautyJury.judge(ShowTheme.GALA, ShowTheme.SHOWSTER, false, RandomSource.create(1));
        helper.assertTrue(showster.scores()[2] == Math.min(10, plain.scores()[2] + 1), "Oma Knabbel loves your own guh");
        helper.assertTrue(showster.rosettes() >= 3, "the Showster set on a gala is worth a lot of rosettes: " + showster.total());
        for (ShowTheme theme : ShowTheme.values()) {
            helper.assertTrue(theme.maxFit() >= 6, "every theme can be dressed well: " + theme.id() + " " + theme.maxFit());
        }
        for (GuhClothes c : ShowTheme.loaners()) {
            helper.assertTrue(java.util.Arrays.stream(ShowTheme.values()).anyMatch(t -> t.fit(c) > 0), "every loaner fits some theme: " + c.id());
            helper.assertTrue(c.slot != GuhClothes.Slot.BACK, "no backpacks on the catwalk");
        }
        helper.assertTrue(ShowTheme.SHOWSTER.stream().noneMatch(ShowTheme::isLoaner), "the Showster set is not in the loaner wardrobe");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void beautyJurySaysWhy(GameTestHelper helper) {
        RandomSource random = RandomSource.create(3);
        Component model = Component.literal("Vadsy");
        List<GuhClothes> scarf = List.of(GuhClothes.WINTER_SCARF);
        var v = BeautyJury.judge(ShowTheme.WINTER, scarf, false, random);
        helper.assertTrue(key(BeautyJury.reason(0, v, ShowTheme.WINTER, scarf, model, random)).equals("quest.guhs.beauty.reason.vadsma.perfect"),
                "Juf Vadsma: that scarf fits Winter perfectly");
        helper.assertTrue(key(BeautyJury.reason(1, v, ShowTheme.WINTER, scarf, model, random)).equals("quest.guhs.beauty.reason.glitterguh.missing.head"),
                "Meneer Glitterguh misses the hat");
        List<GuhClothes> spooky = List.of(GuhClothes.PUMPKIN_HEAD);
        var s = BeautyJury.judge(ShowTheme.WINTER, spooky, false, random);
        helper.assertTrue(key(BeautyJury.reason(0, s, ShowTheme.WINTER, spooky, model, random)).equals("quest.guhs.beauty.reason.vadsma.off"),
                "a pumpkin at a winter show: njeg");
        List<GuhClothes> chef = List.of(GuhClothes.CHEF_HAT, GuhClothes.SUNGLASSES, GuhClothes.STETHOSCOPE, GuhClothes.CHEF_JACKET);
        var c = BeautyJury.judge(ShowTheme.WERK, chef, false, random);
        helper.assertTrue(key(BeautyJury.reason(1, c, ShowTheme.WERK, chef, model, random)).equals("quest.guhs.beauty.reason.glitterguh.set"),
                "the chef set belongs together");
        var showster = BeautyJury.judge(ShowTheme.GALA, ShowTheme.SHOWSTER, false, random);
        helper.assertTrue(key(BeautyJury.reason(2, showster, ShowTheme.GALA, ShowTheme.SHOWSTER, model, random)).equals("quest.guhs.beauty.reason.knabbel.showster"),
                "Oma Knabbel melts for the Showster set");
        var naked = BeautyJury.judge(ShowTheme.GALA, List.of(), false, random);
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(key(BeautyJury.reason(i, naked, ShowTheme.GALA, List.of(), model, random)).endsWith(".naked"), "a naked model: the naked lines");
        }
        helper.succeed();
    }

    private static String key(Component c) {
        return c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : "";
    }

    @GameTest(template = EMPTY)
    public static void beautyWholeShowWithoutOwnItems(GameTestHelper helper) {
        GuhNpcEntity npc = miniTheatre(helper);
        ServerPlayer player = player(helper, npc);
        helper.assertTrue(player.getInventory().isEmpty(), "the player brings nothing");
        BeautyShow.action(npc, player, BeautyShow.START, 0);
        BeautyShow show = BeautyShow.at(npc);
        helper.assertTrue(show != null && BeautyShow.of(player) == show && BeautyShow.isPerforming(player), "the show is on");
        GuhEntity model = show.model();
        helper.assertTrue(model != null && model.getTags().contains(BeautyShow.MODEL_TAG) && model.isNoAi() && model.isInvulnerable(),
                "a model guh is provided");
        helper.assertTrue(model.blockPosition().equals(helper.absolutePos(MODEL)), "on the stage");
        player.getFoodData().setFoodLevel(2);
        player.hurt(helper.getLevel().damageSources().fall(), 5f);
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "performers can't get hurt");
        for (int round = 0; round < BeautyShow.ROUNDS; round++) {
            helper.assertTrue(show.phase() == BeautyShow.Phase.COUNTDOWN && show.round() == round, "round " + round + " starts with a countdown");
            helper.assertTrue(show.worn().isEmpty(), "and a naked model");
            BeautyShow.action(npc, player, BeautyShow.DRESS, GuhClothes.SANTA_HAT.ordinal());
            helper.assertTrue(show.worn().isEmpty(), "no dressing during the countdown");
            show.skip(npc);
            helper.assertTrue(show.isDressing(), "dressing time");
            // dress for the theme: the best loaner in every slot
            for (GuhClothes.Slot slot : ShowTheme.SLOTS) {
                ShowTheme.loaners().stream().filter(c -> c.slot == slot).max(java.util.Comparator.comparingInt(show.theme()::fit))
                        .ifPresent(c -> BeautyShow.action(npc, player, BeautyShow.DRESS, c.ordinal()));
            }
            BeautyShow.action(npc, player, BeautyShow.DRESS, GuhClothes.SHOWSTER_TIARA.ordinal());
            BeautyShow.action(npc, player, BeautyShow.DRESS, GuhClothes.GUH_BACKPACK.ordinal());
            helper.assertTrue(!show.worn().contains(GuhClothes.SHOWSTER_TIARA) && model.getClothes(GuhClothes.Slot.BACK) == null,
                    "only the loaner wardrobe");
            helper.assertTrue(show.worn().size() == 4 && !hasClothes(player), "the model wears 4 pieces, the player has none of them");
            BeautyShow.action(npc, player, BeautyShow.READY, 0);
            helper.assertTrue(show.phase() == BeautyShow.Phase.WALK, "onto the catwalk");
            show.skip(npc);
            helper.assertTrue(show.phase() == BeautyShow.Phase.JURY && model.blockPosition().equals(helper.absolutePos(END)),
                    "the model walked to the end: " + model.blockPosition());
            helper.assertTrue(show.lastVerdict() != null && show.lastVerdict().scores()[0] >= 9, "Juf Vadsma likes it: "
                    + java.util.Arrays.toString(show.lastVerdict().scores()));
            show.skip(npc);
        }
        helper.assertTrue(BeautyShow.at(npc) == null && !BeautyShow.isPerforming(player), "the show is over");
        helper.assertTrue(model.isRemoved(), "the model went home");
        helper.assertTrue(!hasClothes(player), "no loaner clothes went home with the player");
        int rosettes = count(player, BeautyFeature.SHOWROZET.get());
        helper.assertTrue(rosettes == show.rosettes() + BeautyShow.FIRST_ROSETTES, "rosettes for every round + the bonus + 4 for the first show: " + rosettes);
        helper.assertTrue(show.rosettes() >= 3 * 2 + 1, "a well dressed show earns plenty: " + show.rosettes());
        helper.assertTrue(BeautyShow.best(player) == show.total() && show.total() > 40, "the record is saved: " + show.total());
        helper.assertTrue(count(player, nl.juiced.guhs.registry.ModItems.GUH_BALLON.get()) == 3, "the first-show present");
        helper.assertTrue(advancement(player, "beauty_first"), "the first-show advancement");
        helper.assertTrue(GuhQuests.saved(player).getInt("guhs_beauty_shows") == 1, "one show played");
        helper.assertTrue(nl.juiced.guhs.quest.Scorebord.top(helper.getLevel().getServer(), BeautyShow.SCOREBORD).stream()
                .anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == show.total()), "the show is on the world's top 3");
        BeautyShow.showScores(npc);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, npc.getBoundingBox().inflate(4),
                d -> d.getTags().contains(nl.juiced.guhs.quest.Scorebord.TAG)).isEmpty(), "the top 3 floats over the Showguh");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void beautyNakedShowEarnsNothing(GameTestHelper helper) {
        GuhNpcEntity npc = miniTheatre(helper);
        ServerPlayer player = player(helper, npc);
        GuhQuests.saved(player).putBoolean("guhs_beauty_first", true);     // not the first show: no present
        BeautyShow.action(npc, player, BeautyShow.START, 0);
        BeautyShow show = BeautyShow.at(npc);
        helper.assertTrue(show != null, "the show is on");
        for (int i = 0; i < 4 * BeautyShow.ROUNDS && BeautyShow.at(npc) == show; i++) {
            show.skip(npc);                                   // AFK: nothing is ever put on
        }
        helper.assertTrue(BeautyShow.at(npc) == null && !BeautyShow.isPerforming(player), "the show is over");
        helper.assertTrue(show.rosettes() == 0 && count(player, BeautyFeature.SHOWROZET.get()) == 0,
                "three naked walks earn no rosettes, not even the finish bonus: " + show.rosettes());
        helper.assertTrue(GuhQuests.saved(player).getInt("guhs_beauty_shows") == 1, "but it counts as a show");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void beautyOwnGuhComesBackInItsOwnClothes(GameTestHelper helper) {
        GuhNpcEntity npc = miniTheatre(helper);
        ServerPlayer player = player(helper, npc);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 3));
        guh.tame(player);
        guh.wear(GuhClothes.RAIN_HAT);
        guh.wear(GuhClothes.GUH_BACKPACK);          // (2.9: clothes are unlocks, the backpack is just worn)
        guh.getBackpack().setItem(0, new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(BeautyShow.ownGuh(player) == guh, "the player's own guh is found");
        BeautyShow.action(npc, player, BeautyShow.START_OWN, 0);
        BeautyShow show = BeautyShow.at(npc);
        helper.assertTrue(show != null && show.withOwnGuh() && show.model() == guh, "the own guh walks the show");
        helper.assertTrue(!guh.isOwnedBy(player) && guh.isNoAi() && guh.isInvulnerable(), "borrowed: nobody can undress or pick it up");
        helper.assertTrue(guh.blockPosition().equals(helper.absolutePos(MODEL)) && guh.getClothes(GuhClothes.Slot.HEAD) == null,
                "on the stage, freshly undressed");
        helper.assertTrue(show.allowed(GuhClothes.RAIN_HAT), "its own rain hat is in the wardrobe for this show");
        show.skip(npc);
        BeautyShow.action(npc, player, BeautyShow.DRESS, GuhClothes.SANTA_HAT.ordinal());
        BeautyShow.action(npc, player, BeautyShow.DRESS, GuhClothes.KERMIS_JASJE.ordinal());
        helper.assertTrue(guh.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.SANTA_HAT, "dressed in a loaner");
        // a crash right now: the guh is saved with its loan note, and cleans itself up when it's loaded again
        CompoundTag saved = new CompoundTag();
        guh.save(saved);
        BeautyShow.action(npc, player, BeautyShow.QUIT, 0);
        helper.assertTrue(BeautyShow.at(npc) == null && !BeautyShow.isPerforming(player), "stopping ends the show");
        helper.assertTrue(guh.isAlive() && guh.isOwnedBy(player) && !guh.isNoAi() && !guh.isInvulnerable(), "the guh is the player's again");
        helper.assertTrue(guh.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.RAIN_HAT && guh.getClothes(GuhClothes.Slot.BODY) == null,
                "in its own clothes again");
        helper.assertTrue(guh.hasBackpack() && guh.getBackpack().getItem(0).getCount() == 3, "and its backpack is untouched");
        helper.assertTrue(!guh.getPersistentData().contains(BeautyShow.LOAN) && !hasClothes(player), "no loan left, nothing in the inventory");
        guh.discard();
        GuhEntity restored = (GuhEntity) net.minecraft.world.entity.EntityType.create(saved, helper.getLevel()).orElseThrow();
        restored.setUUID(java.util.UUID.randomUUID());
        helper.assertTrue(restored.getPersistentData().contains(BeautyShow.LOAN) && !restored.isOwnedBy(player), "saved while borrowed");
        helper.getLevel().addFreshEntity(restored);
        helper.assertTrue(restored.isOwnedBy(player) && restored.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.RAIN_HAT
                && !restored.isNoAi() && !restored.getPersistentData().contains(BeautyShow.LOAN), "loaded again: given back");
        restored.discard();
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void beautyOneShowAtATime(GameTestHelper helper) {
        GuhNpcEntity npc = miniTheatre(helper);
        ServerPlayer a = player(helper, npc), b = player(helper, npc);
        BeautyShow.action(npc, a, BeautyShow.START, 0);
        BeautyShow show = BeautyShow.at(npc);
        BeautyShow.action(npc, b, BeautyShow.START, 0);
        helper.assertTrue(BeautyShow.at(npc) == show && !BeautyShow.isPerforming(b), "the second player waits (and watches)");
        show.skip(npc);
        BeautyShow.action(npc, b, BeautyShow.DRESS, GuhClothes.PARTY_HAT.ordinal());
        helper.assertTrue(show.worn().isEmpty(), "only the performer dresses the model");
        GuhEntity model = show.model();
        net.minecraft.world.InteractionResult result = b.interactOn(model, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(result.consumesAction() && !model.isOrderedToSit(), "others can't touch the model");
        BeautyShow.leave(a);                                  // the performer logs out
        helper.assertTrue(BeautyShow.at(npc) == null && model.isRemoved(), "the show ends and the model goes home");
        BeautyShow.action(npc, b, BeautyShow.START, 0);
        helper.assertTrue(BeautyShow.at(npc) != null && BeautyShow.isPerforming(b), "now it's b's turn");
        BeautyShow.at(npc).end(false);
        leave(helper, a, b);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void beautyShopSellsTheShowsterSet(GameTestHelper helper) {
        GuhNpcEntity npc = miniTheatre(helper);
        var offers = npc.getOffers();
        helper.assertTrue(offers.size() == 3, "three offers: " + offers.size());
        List<GuhClothes> sold = new java.util.ArrayList<>();
        for (var offer : offers) {
            helper.assertTrue(offer.getBaseCostA().is(BeautyFeature.SHOWROZET.get()), "paid with showrozetten");
            if (offer.getResult().getItem() instanceof GuhClothingItem item) {
                sold.add(item.getClothes());
            }
        }
        helper.assertTrue(sold.containsAll(ShowTheme.SHOWSTER), "the whole Showster set is for sale");
        helper.assertTrue(offers.get(2).getBaseCostA().getCount() == BeautyFeature.PRICE_TIARA, "the tiara costs the most");
        helper.succeed();
    }

    @GameTest(template = "guh_beauty_theater", timeoutTicks = 400)
    public static void beautyTheatreHasItAll(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(120);
        List<GuhNpcEntity> hosts = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, area, n -> n.getKind() == GuhNpcEntity.Kind.SHOWGUH);
        helper.assertTrue(hosts.size() == 1, "one Showguh on the stage: " + hosts.size());
        GuhNpcEntity npc = hosts.get(0);
        List<GuhEntity> jury = helper.getLevel().getEntitiesOfClass(GuhEntity.class, area, g -> g.getTags().contains(BeautyShow.JURY_TAG));
        helper.assertTrue(jury.size() == 3 && jury.stream().allMatch(g -> g.isInvulnerable() && g.isNoAi() && g.isOrderedToSit()),
                "three sitting jury guhs");
        var spots = BeautyShow.spots(npc);
        helper.assertTrue(spots != null && spots[0].distSqr(spots[1]) > 30 * 30, "the stage and the end of a long catwalk are marked");
        helper.assertTrue(!helper.getLevel().getBlockState(spots[0].below()).isAir() && !helper.getLevel().getBlockState(spots[1].below()).isAir(),
                "the model stands on the stage");
        ServerPlayer player = player(helper, npc);
        BeautyShow show = BeautyShow.start(npc, player, false);
        helper.assertTrue(show != null && show.model() != null, "a show starts in the real theatre");
        show.skip(npc);
        BeautyShow.action(npc, player, BeautyShow.DRESS, GuhClothes.ROYAL_CROWN.ordinal());
        BeautyShow.action(npc, player, BeautyShow.READY, 0);
        show.skip(npc);
        helper.assertTrue(show.phase() == BeautyShow.Phase.JURY && show.model().blockPosition().equals(spots[1]), "the model reached the jury");
        for (int i = 0; i <= BeautyShow.POSE_TICKS + 2 * BeautyShow.JURY_STEP; i++) {
            show.tick(npc);
        }
        helper.assertTrue(show.scoreCards() == 3, "the three jury guhs hold up their score cards: " + show.scoreCards());
        show.end(false);
        helper.assertTrue(show.scoreCards() == 0, "the cards are gone after the show");
        leave(helper, player);
        helper.succeed();
    }

    // --- 2.9: makkelijk / medium / lastig -----------------------------------------------------------------------------------

    /** Makkelijk: a mild jury and a whole minute; lastig: a strict jury and 30 seconds. Medium is the jury as it was. */
    @GameTest(template = EMPTY)
    public static void beautyJuryAndDressTimePerLevel(GameTestHelper helper) {
        var m = nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK;
        var n = nl.juiced.guhs.feature.spelen.Niveau.MEDIUM;
        var l = nl.juiced.guhs.feature.spelen.Niveau.LASTIG;
        helper.assertTrue(BeautyShow.kleedTijd(m) == 20 * 60 && BeautyShow.kleedTijd(n) == BeautyShow.DRESS_TICKS && BeautyShow.kleedTijd(l) == 20 * 30,
                "a minute, 45 seconds, 30 seconds");
        List<GuhClothes> mixed = List.of(GuhClothes.SANTA_HAT, GuhClothes.CHRISTMAS_SWEATER, GuhClothes.EYEPATCH);   // one piece off-theme
        var easy = BeautyJury.judge(ShowTheme.WINTER, mixed, false, RandomSource.create(3), m);
        var normal = BeautyJury.judge(ShowTheme.WINTER, mixed, false, RandomSource.create(3), n);
        var hard = BeautyJury.judge(ShowTheme.WINTER, mixed, false, RandomSource.create(3), l);
        var old = BeautyJury.judge(ShowTheme.WINTER, mixed, false, RandomSource.create(3));
        helper.assertTrue(java.util.Arrays.equals(old.scores(), normal.scores()), "medium = the old jury");
        helper.assertTrue(easy.total() > normal.total() && normal.total() > hard.total(), "mild, fair, strict: " + easy.total() + " "
                + normal.total() + " " + hard.total());
        helper.assertTrue(hard.scores()[0] == Math.max(1, normal.scores()[0] - 1), "Juf Vadsma takes a second point for the eyepatch");
        var naked = BeautyJury.judge(ShowTheme.GALA, List.of(), false, RandomSource.create(3), m);
        helper.assertTrue(naked.rosettes() == 0 && naked.scores()[0] == 1, "a naked model gets nothing, not even from a mild jury");
        helper.succeed();
    }

    /** A whole show on lastig: its own record and board, half as many rosettes more, and the lastig advancement. */
    @GameTest(template = EMPTY)
    public static void beautyLastigShowHasItsOwnBoard(GameTestHelper helper) {
        GuhNpcEntity npc = miniTheatre(helper);
        ServerPlayer player = player(helper, npc);
        var lastig = nl.juiced.guhs.feature.spelen.Niveau.LASTIG;
        BeautyShow.action(npc, player, nl.juiced.guhs.feature.klassiekers.Klassiekers.metNiveau(BeautyShow.START, lastig), 0);
        BeautyShow show = BeautyShow.at(npc);
        helper.assertTrue(show != null && show.niveau() == lastig, "a show on lastig");
        for (int round = 0; round < BeautyShow.ROUNDS; round++) {
            show.skip(npc);
            helper.assertTrue(show.isDressing(), "dressing time");
            for (GuhClothes.Slot slot : ShowTheme.SLOTS) {
                ShowTheme.loaners().stream().filter(c -> c.slot == slot).max(java.util.Comparator.comparingInt(show.theme()::fit))
                        .ifPresent(c -> BeautyShow.action(npc, player, BeautyShow.DRESS, c.ordinal()));
            }
            BeautyShow.action(npc, player, BeautyShow.READY, 0);
            show.skip(npc);
            show.skip(npc);
        }
        helper.assertTrue(BeautyShow.at(npc) == null, "the show is over");
        int rosettes = count(player, BeautyFeature.SHOWROZET.get());
        helper.assertTrue(rosettes == show.rosettes() + BeautyShow.FIRST_ROSETTES, "the rosettes: " + rosettes);
        helper.assertTrue(show.rosettes() >= 3 * lastig.munten(2) + lastig.munten(2), "lastig: at least 3 a round and 3 at the end: " + show.rosettes());
        helper.assertTrue(BeautyShow.best(player, lastig) == show.total() && BeautyShow.best(player) == 0, "the lastig record, medium untouched");
        helper.assertTrue(nl.juiced.guhs.quest.Scorebord.top(helper.getLevel().getServer(), "beauty_show_lastig").stream()
                .anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == show.total()), "on the lastig board");
        helper.assertTrue(nl.juiced.guhs.feature.klassiekers.Klassiekers.done(player, "grote_guhspelen/klassiekers_beauty_lastig"), "the lastig advancement");
        leave(helper, player);
        helper.succeed();
    }
}
