package nl.juiced.guhs.feature.bibliotheek;

import java.util.List;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
/**
 * GameTests of the guh library: the books, the lecterns of the reading room (read right away, one copy per book per
 * player), the Bibliothecaris (first visit, the quiz, the shop), the secret bookcase, the book stand, the protection,
 * the archive loot, and the structure itself.
 */
public class BibliotheekGameTests {
    private static final String EMPTY = "empty";
    private static final String LIBRARY = "guhbibliotheek";
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private static GuhNpcEntity bibliothecaris(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), POS);
        npc.setKind(GuhNpcEntity.Kind.BIBLIOTHECARIS);
        return npc;
    }

    private static ServerPlayer player(GameTestHelper helper, Entity near) {
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.snapTo(near.getX() + 1, near.getY(), near.getZ());
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    private static int books(ServerPlayer player, Guhboek book) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (Guhboek.of(player.getInventory().getItem(i)) == book) {
                n += player.getInventory().getItem(i).getCount();
            }
        }
        return n;
    }

    private static boolean done(ServerPlayer player, String advancement) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id(advancement));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** A lectern with this guh book on it at POS, inside a (test) library. */
    private static BoundingBox lectern(GameTestHelper helper, Guhboek book) {
        helper.setBlock(POS, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.HAS_BOOK, true));
        if (helper.getLevel().getBlockEntity(helper.absolutePos(POS)) instanceof LecternBlockEntity lectern) {
            lectern.setBook(book.stack());
        }
        BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(4, 3, 4)));
        BibliotheekProtection.TEST_AREAS.add(box);
        return box;
    }

    private static boolean rightClick(ServerPlayer player, BlockPos pos) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit)).isCanceled();
    }

    @GuhTest(template = EMPTY)
    public static void guhBooksAreTranslatedWrittenBooks(GameTestHelper helper) {
        for (Guhboek book : Guhboek.values()) {
            ItemStack stack = book.stack();
            helper.assertTrue(stack.is(Items.WRITTEN_BOOK), "a real written book");
            helper.assertTrue(Guhboek.of(stack) == book, "it knows which book it is: " + book.id);
            var content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            helper.assertTrue(content != null && content.pages().size() == book.pages && content.resolved(), "pages of " + book.id);
            // 1.2.0: no author String (one language); the "by" line is translatable lore
            var lore = stack.get(DataComponents.LORE);
            helper.assertTrue(content.title().raw().length() <= 32 && content.author().isEmpty() && lore != null
                    && lore.lines().get(0).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents door
                    && door.getKey().equals("item.guhs.bieb_boek.door"), "title, and the author as lore");
            helper.assertTrue(stack.has(DataComponents.CUSTOM_NAME), "a translated name");
            helper.assertTrue(book.secret() == Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)), "only the secret one glitters");
        }
        helper.assertTrue(Guhboek.of(new ItemStack(Items.WRITTEN_BOOK)) == null && Guhboek.of(new ItemStack(Items.BOOK)) == null,
                "other books are no guh books");
        helper.assertTrue(Guhboek.questionCount() == Guhboek.values().length * Guhboek.QUESTIONS && Guhboek.bookOfQuestion(23) == Guhboek.GEHEIM && Guhboek.bookOfQuestion(25) == Guhboek.GUHEINDE, "two questions per book");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void firstVisitIsAWelcomeAndBooksCount(GameTestHelper helper) {
        GuhNpcEntity npc = bibliothecaris(helper);
        ServerPlayer player = player(helper, npc);
        helper.assertTrue(Bibliothecaris.count(player) == 0, "a new reader has no books");
        npc.interact(player, InteractionHand.MAIN_HAND, npc.position());
        helper.assertTrue(GuhQuests.saved(player).getBooleanOr(Bibliothecaris.WELCOMED, false), "welcomed");
        helper.assertTrue(Bibliothecaris.count(player) == 0, "no lending: the books are on the lecterns");
        // a guh book in the inventory goes into the collection, and losing it doesn't lose it from the collection
        player.getInventory().add(Guhboek.EERSTE_GUH.stack());
        Bibliothecaris.collect(player);
        helper.assertTrue(Bibliothecaris.has(player, Guhboek.EERSTE_GUH) && Bibliothecaris.count(player) == 1, "it is in the collection");
        helper.assertTrue(done(player, "quest/bieb_eerste_boek"), "advancement for the first book");
        player.getInventory().clearContent();
        Bibliothecaris.collect(player);
        helper.assertTrue(Bibliothecaris.has(player, Guhboek.EERSTE_GUH), "still collected");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void lecternBooksAreReadRightAwayAndTakenOnce(GameTestHelper helper) {
        BoundingBox box = lectern(helper, Guhboek.VAHOEG);
        try {
            BlockPos pos = helper.absolutePos(POS);
            ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5);
            helper.assertTrue(Leeszaal.bookAt(helper.getLevel(), pos) == Guhboek.VAHOEG, "the lectern holds a guh book");
            helper.assertTrue(rightClick(player, pos), "a right-click opens the book (no vanilla lectern menu)");
            helper.assertTrue((Leeszaal.read(player) & Guhboek.VAHOEG.bit()) != 0, "read right away, no waiting");
            helper.assertTrue(books(player, Guhboek.VAHOEG) == 0, "reading doesn't take it");
            // other books can be read at once too, and the quiz asks about what you have read
            helper.assertTrue(Bibliothecaris.screenData(player).contains("Question")
                    && Guhboek.bookOfQuestion(Bibliothecaris.screenData(player).getIntOr("Question", 0)) == Guhboek.VAHOEG, "a quiz question about the book you read");
            Leeszaal.take(player, pos);
            helper.assertTrue(books(player, Guhboek.VAHOEG) == 1 && Leeszaal.taken(player, Guhboek.VAHOEG), "one copy to take home");
            helper.assertTrue(Bibliothecaris.has(player, Guhboek.VAHOEG), "and it is in the collection");
            Leeszaal.take(player, pos);
            player.getInventory().clearContent();
            Leeszaal.take(player, pos);
            helper.assertTrue(books(player, Guhboek.VAHOEG) == 0, "only once per book, even when you lost it");
            var lectern = (LecternBlockEntity) helper.getLevel().getBlockEntity(pos);
            helper.assertTrue(Guhboek.of(lectern.getBook()) == Guhboek.VAHOEG, "the book stays on the lectern");
            player.snapTo(pos.getX() + 20, pos.getY(), pos.getZ());
            GuhQuests.saved(player).putInt(Leeszaal.TAKEN, 0);
            Leeszaal.take(player, pos);
            helper.assertTrue(books(player, Guhboek.VAHOEG) == 0, "not from across the room");
            leave(helper, player);
        } finally {
            BibliotheekProtection.TEST_AREAS.remove(box);
        }
        // outside a library a lectern is just a lectern
        helper.assertTrue(Leeszaal.bookAt(helper.getLevel(), helper.absolutePos(POS)) == null, "only library lecterns");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void quizGivesVouchersForRightAnswers(GameTestHelper helper) {
        GuhNpcEntity npc = bibliothecaris(helper);
        ServerPlayer player = player(helper, npc);
        helper.assertTrue(Bibliothecaris.screenData(player).getBooleanOr("QuizNone", false), "no questions without books");
        player.getInventory().add(Guhboek.EERSTE_GUH.stack());          // book 1: its two questions
        Bibliothecaris.collect(player);
        var data = Bibliothecaris.screenData(player);
        int question = data.getIntOr("Question", 0);
        helper.assertTrue(data.contains("Question") && Guhboek.bookOfQuestion(question) == Guhboek.EERSTE_GUH, "a question about your book");
        int[] order = Bibliothecaris.ORDERS[data.getIntOr("Order", 0)];
        int right = 0, wrong = 0;
        for (int i = 0; i < 3; i++) {
            if (order[i] == 0) {
                right = i;
            } else {
                wrong = i;
            }
        }
        Bibliothecaris.action(npc, player, Bibliothecaris.ANSWER + right);
        helper.assertTrue(GuhQuests.count(player, BibliotheekFeature.BOEKENBON.get()) == 1, "right: a boekenbon");
        helper.assertTrue(done(player, "quest/bieb_kwis") && Long.bitCount(Bibliothecaris.rightAnswers(player)) == 1, "quiz advancement");
        helper.assertTrue(Bibliothecaris.screenData(player).contains("QuizWait"), "a moment before the next question");
        GuhQuests.saved(player).putLong(Bibliothecaris.QUIZ_WAIT, 0);
        data = Bibliothecaris.screenData(player);
        helper.assertTrue(data.getIntOr("Question", 0) != question, "the next question is the other one");
        order = Bibliothecaris.ORDERS[data.getIntOr("Order", 0)];
        for (int i = 0; i < 3; i++) {
            if (order[i] != 0) {
                wrong = i;
            }
        }
        Bibliothecaris.action(npc, player, Bibliothecaris.ANSWER + wrong);
        helper.assertTrue(GuhQuests.count(player, BibliotheekFeature.BOEKENBON.get()) == 1, "wrong: nothing");
        helper.assertTrue(Bibliothecaris.screenData(player).getIntOr("QuizWait", 0) > 20, "and half a minute to read again");
        Bibliothecaris.action(npc, player, Bibliothecaris.ANSWER + right);
        helper.assertTrue(GuhQuests.count(player, BibliotheekFeature.BOEKENBON.get()) == 1, "no answering while waiting");
        // all questions of all books right: the quiz master
        GuhQuests.saved(player).putInt(Bibliothecaris.BOOKS, (1 << Guhboek.values().length) - 1);
        for (int n = 0; n < 60 && Long.bitCount(Bibliothecaris.rightAnswers(player)) < Guhboek.questionCount(); n++) {
            GuhQuests.saved(player).putLong(Bibliothecaris.QUIZ_WAIT, 0);
            var d = Bibliothecaris.screenData(player);
            int[] o = Bibliothecaris.ORDERS[d.getIntOr("Order", 0)];
            for (int i = 0; i < 3; i++) {
                if (o[i] == 0) {
                    Bibliothecaris.action(npc, player, Bibliothecaris.ANSWER + i);
                }
            }
        }
        helper.assertTrue(Long.bitCount(Bibliothecaris.rightAnswers(player)) == Guhboek.questionCount(), "every question answered");
        helper.assertTrue(GuhQuests.count(player, BibliotheekFeature.BOEKENBON.get()) == Guhboek.questionCount(), "one voucher per question, once");
        helper.assertTrue(done(player, "quest/bieb_kwismeester"), "the quiz master");
        GuhQuests.saved(player).putLong(Bibliothecaris.QUIZ_WAIT, 0);
        var d = Bibliothecaris.screenData(player);
        int[] o = Bibliothecaris.ORDERS[d.getIntOr("Order", 0)];
        for (int i = 0; i < 3; i++) {
            if (o[i] == 0) {
                Bibliothecaris.action(npc, player, Bibliothecaris.ANSWER + i);
            }
        }
        helper.assertTrue(GuhQuests.count(player, BibliotheekFeature.BOEKENBON.get()) == Guhboek.questionCount()
                && GuhQuests.count(player, ModItems.KAAS_KNABBELS.get()) == 2, "asked again: knabbels, no voucher");
        helper.assertTrue(Bibliothecaris.screenData(player).getIntOr("QuizWait", 0) > 30, "and a longer wait: no knabbel farm");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void shopSellsBooksAndTheOutfitForVouchers(GameTestHelper helper) {
        GuhNpcEntity npc = bibliothecaris(helper);
        var offers = npc.getOffers();
        helper.assertTrue(offers.size() == 4 + Guhboek.values().length - 1, "outfit (3), armchair and a spare copy of every book but the secret one: " + offers.size());
        boolean glasses = false;
        for (MerchantOffer offer : offers) {
            helper.assertTrue(offer.getCostA().is(BibliotheekFeature.BOEKENBON.get()), "paid with boekenbonnen");
            helper.assertTrue(Guhboek.of(offer.getResult()) != Guhboek.GEHEIM, "the secret book is not for sale");
            glasses |= offer.getResult().is(ModItems.clothingItem(GuhClothes.BIEB_LEESBRIL));
        }
        helper.assertTrue(glasses, "the scholar's glasses are for sale");
        helper.assertTrue(GuhClothes.BIEB_HOED.shows("outfit_bieb_hoed") && GuhClothes.BIEB_HOED.shows("outfit_bieb_steeltje")
                && GuhClothes.BIEB_HOED.slot == GuhClothes.Slot.HEAD, "the beret, with its little stalk");
        helper.assertTrue(GuhClothes.BIEB_LEESBRIL.slot == GuhClothes.Slot.EYES && GuhClothes.BIEB_VEST.slot == GuhClothes.Slot.BODY,
                "glasses, cardigan and beret: a whole scholar's set");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, timeoutTicks = 200)
    public static void secretBookcaseSwingsOpenAndShut(GameTestHelper helper) {
        BlockPos low = new BlockPos(2, 1, 2), high = low.above();
        helper.setBlock(low, BibliotheekFeature.GEHEIME_KAST.get().defaultBlockState());
        helper.setBlock(high, BibliotheekFeature.GEHEIME_KAST.get().defaultBlockState());
        var level = helper.getLevel();
        helper.assertTrue(helper.getBlockState(low).isCollisionShapeFullBlock(level, helper.absolutePos(low)), "closed: a full bookshelf");
        GeheimeKastBlock.setOpen(level, helper.absolutePos(low), true);
        helper.assertTrue(helper.getBlockState(low).getValue(GeheimeKastBlock.OPEN) && helper.getBlockState(high).getValue(GeheimeKastBlock.OPEN),
                "both halves of the door open");
        helper.assertTrue(!helper.getBlockState(low).isCollisionShapeFullBlock(level, helper.absolutePos(low)), "open: you can walk through");
        // someone in the doorway: it waits
        var guh = helper.spawn(ModEntities.GUH.get(), low);
        guh.setNoAi(true);
        helper.runAfterDelay(GeheimeKastBlock.OPEN_TICKS + 10, () -> {
            helper.assertTrue(helper.getBlockState(low).getValue(GeheimeKastBlock.OPEN), "doesn't close on a guh");
            helper.assertTrue(!helper.getBlockState(high).getValue(GeheimeKastBlock.OPEN), "the empty half did close");
            guh.discard();
        });
        helper.runAfterDelay(GeheimeKastBlock.OPEN_TICKS + 50, () -> {
            helper.assertTrue(!helper.getBlockState(low).getValue(GeheimeKastBlock.OPEN), "and then it closes");
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY)
    public static void bookStandGivesTheSecretBookOnce(GameTestHelper helper) {
        helper.setBlock(POS, BibliotheekFeature.BOEKALTAAR.get().defaultBlockState());
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        player.getInventory().clearContent();
        player.snapTo(helper.absolutePos(POS).getX() + 1.5, helper.absolutePos(POS).getY(), helper.absolutePos(POS).getZ() + 0.5);
        helper.assertTrue(Leeszaal.bookAt(helper.getLevel(), helper.absolutePos(POS)) == Guhboek.GEHEIM, "the secret book lies on the stand");
        Leeszaal.open(player, helper.absolutePos(POS), Guhboek.GEHEIM);
        helper.assertTrue((Leeszaal.read(player) & Guhboek.GEHEIM.bit()) != 0 && books(player, Guhboek.GEHEIM) == 0, "read it on the stand");
        Leeszaal.take(player, helper.absolutePos(POS));
        helper.assertTrue(books(player, Guhboek.GEHEIM) == 1 && Bibliothecaris.has(player, Guhboek.GEHEIM), "the secret book");
        helper.assertTrue(done(player, "quest/bieb_geheim"), "secret advancement");
        player.getInventory().clearContent();
        Leeszaal.take(player, helper.absolutePos(POS));
        helper.assertTrue(books(player, Guhboek.GEHEIM) == 0, "one copy per player, ever");
        helper.assertTrue(helper.getBlockState(POS).getDestroySpeed(helper.getLevel(), helper.absolutePos(POS)) < 0,
                "the stand can't be broken (and has no recipe)");
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Guhs.id("bieb_boekaltaar"))).isEmpty(), "no recipe");
        // all of them: the bookworm
        for (Guhboek book : Guhboek.values()) {
            player.getInventory().add(book.stack());
        }
        Bibliothecaris.collect(player);
        helper.assertTrue(Bibliothecaris.count(player) == Guhboek.values().length && done(player, "quest/bieb_alle_boeken") && done(player, "guhmension/bieb_boekenwurm"),
                "all books collected");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void libraryIsProtected(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(POS);
        helper.setBlock(POS, Blocks.BOOKSHELF);
        BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(4, 3, 4)));
        BibliotheekProtection.TEST_AREAS.add(box);
        try {
            ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
            player.setGameMode(GameType.SURVIVAL);
            BlockState state = helper.getLevel().getBlockState(pos);
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), pos, state, player)).isCanceled(),
                    "no breaking in the library");
            var place = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(helper.getLevel().dimension(), helper.getLevel(), pos.above()),
                    Blocks.BOOKSHELF.defaultBlockState(), player);
            helper.assertTrue(NeoForge.EVENT_BUS.post(place).isCanceled(), "no building in the library");
            helper.assertTrue(!BibliotheekProtection.inLibrary(helper.getLevel(), pos.offset(20, 0, 0)), "outside is fine");
            player.setGameMode(GameType.CREATIVE);
            helper.assertTrue(!NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), pos, state, player)).isCanceled(),
                    "creative players may change it");
            leave(helper, player);
        } finally {
            BibliotheekProtection.TEST_AREAS.remove(box);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void archiveChestsHaveGuhBooks(GameTestHelper helper) {
        LootTable table = helper.getLevel().getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/guhbibliotheek_archief")));
        helper.assertTrue(table != LootTable.EMPTY, "the archive loot table exists");
        int books = 0;
        for (int i = 0; i < 40; i++) {
            LootParams params = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(POS)))
                    .create(LootContextParamSets.CHEST);
            for (ItemStack stack : table.getRandomItems(params)) {
                Guhboek book = Guhboek.of(stack);
                if (book != null) {
                    books++;
                    helper.assertTrue(!book.secret(), "never the secret book");
                    var content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
                    helper.assertTrue(content != null && content.pages().size() == book.pages, "with all its pages: " + book.id);
                }
            }
        }
        helper.assertTrue(books > 5, "lore books in the archive: " + books);
        helper.succeed();
    }

    // --- the real thing ---------------------------------------------------------------------------------------------

    @GuhTest(template = LIBRARY, timeoutTicks = 200, batch = "guhbibliotheek")
    public static void theLibraryHasItsBibliothecarisAndSecretRoom(GameTestHelper helper) {
        AABB all = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(120);
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, all, n -> n.getKind() == GuhNpcEntity.Kind.BIBLIOTHECARIS);
        helper.assertTrue(npcs.size() == 1, "one Bibliothecaris behind the desk: " + npcs.size());
        GuhNpcEntity npc = npcs.get(0);
        helper.assertTrue(!helper.getLevel().getBlockState(npc.blockPosition().below()).isAir(), "sitting on something");
        // the secret door and the book stand
        BlockPos door = null, stand = null;
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(87, 20, 85))) {
            BlockState s = helper.getLevel().getBlockState(p);
            if (door == null && s.is(BibliotheekFeature.GEHEIME_KAST.get())) {
                door = p.immutable();
            }
            if (stand == null && s.is(BibliotheekFeature.BOEKALTAAR.get())) {
                stand = p.immutable();
            }
        }
        helper.assertTrue(door != null && stand != null, "a secret bookcase and a book stand");
        if (helper.getLevel().getBlockState(door.below()).is(BibliotheekFeature.GEHEIME_KAST.get())) {
            door = door.below();
        }
        GeheimeKastBlock.setOpen(helper.getLevel(), door, true);
        helper.assertTrue(helper.getLevel().getBlockState(door).getValue(GeheimeKastBlock.OPEN)
                && helper.getLevel().getBlockState(door.above()).getValue(GeheimeKastBlock.OPEN), "the secret door opens (two high)");
        // the reading room: a lectern for every guh book but the secret one
        java.util.Set<Guhboek> onLecterns = java.util.EnumSet.noneOf(Guhboek.class);
        BlockPos firstLectern = null;
        for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(87, 20, 85))) {
            if (helper.getLevel().getBlockEntity(p) instanceof LecternBlockEntity lectern && Guhboek.of(lectern.getBook()) != null) {
                helper.assertTrue(onLecterns.add(Guhboek.of(lectern.getBook())), "every book on its own lectern");
                if (firstLectern == null) {
                    firstLectern = p.immutable();
                }
            }
        }
        helper.assertTrue(onLecterns.size() == Guhboek.values().length - 2 && !onLecterns.contains(Guhboek.GEHEIM) && !onLecterns.contains(Guhboek.VOORRAADKELDER), "books on lecterns (not the secret one, not the Voorraadmika diary): " + onLecterns);
        var content = ((LecternBlockEntity) helper.getLevel().getBlockEntity(firstLectern)).getBook().get(DataComponents.WRITTEN_BOOK_CONTENT);
        helper.assertTrue(content != null && content.pages().size() == Guhboek.of(((LecternBlockEntity) helper.getLevel()
                .getBlockEntity(firstLectern)).getBook()).pages, "a whole book on the lectern");
        // a visit
        BibliotheekProtection.TEST_AREAS.add(BoundingBox.fromCorners(origin, origin.offset(87, 39, 85)));
        try {
            ServerPlayer player = player(helper, npc);
            Bibliothecaris.INSTANCE.talk(npc, player);
            helper.assertTrue(Bibliothecaris.count(player) == 0, "no lending at the desk");
            player.snapTo(firstLectern.getX() + 0.5, firstLectern.getY(), firstLectern.getZ() + 1.5);
            helper.assertTrue(Leeszaal.bookAt(helper.getLevel(), firstLectern) != null, "the lectern is a reading lectern");
            Leeszaal.take(player, firstLectern);
            player.snapTo(stand.getX() + 1.5, stand.getY(), stand.getZ() + 0.5);
            Leeszaal.take(player, stand);
            helper.assertTrue(Bibliothecaris.count(player) == 2, "a lectern book and the secret one");
            leave(helper, player);
        } finally {
            BibliotheekProtection.TEST_AREAS.removeIf(b -> b.minX() == origin.getX() && b.maxX() == origin.getX() + 87);
        }
        helper.succeed();
    }
}
