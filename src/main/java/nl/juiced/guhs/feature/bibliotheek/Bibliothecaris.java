package nl.juiced.guhs.feature.bibliotheek;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The Bibliothecaris behind the desk of the guh library. Every guh book lies open on a lectern in the reading room (the
 * secret one on the stand in the secret room): read them right away, and take one copy of each home, once (see
 * {@link Leeszaal}). A right-click on him opens his screen:
 * <ul>
 *   <li>your collection: the book spines on a shelf (collected / read / not read yet);</li>
 *   <li>the Guhkwis: a question about one of the books you have read or collected, three answers. Right the first time: a
 *       boekenbon;</li>
 *   <li>his shop: spare copies of the books, the scholar's outfit (only sold here) and the guh armchair, for boekenbonnen.</li>
 * </ul>
 * Which books you have is saved with the player (GuhQuests.saved: it survives dying) and counts every guh book that was
 * ever in your inventory, from a lectern, the shop, the archive chests or the secret room.
 */
public final class Bibliothecaris implements NpcRole {
    public static final Bibliothecaris INSTANCE = new Bibliothecaris();

    /** Actions from the screen. */
    public static final int SHOP = 1, REFRESH = 2, ANSWER = 10;
    /** Waiting time for the next quiz question after a right answer (a new one / one answered right before) / a wrong one. */
    public static final long WAIT_RIGHT = 20 * 2, WAIT_AGAIN = 20 * 60, WAIT_WRONG = 20 * 30;
    /** The answers are shown in one of these orders; answer 0 is the right one. */
    public static final int[][] ORDERS = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};

    static final String BOOKS = "guhs_bieb_boeken", WELCOMED = "guhs_bieb_welkom",
            QUESTION = "guhs_bieb_vraag", ORDER = "guhs_bieb_volgorde", RIGHT = "guhs_bieb_goed", QUIZ_WAIT = "guhs_bieb_wacht";

    private Bibliothecaris() {
    }

    // --- the player's library card ----------------------------------------------------------------------------------

    public static int books(ServerPlayer player) {
        return GuhQuests.saved(player).getIntOr(BOOKS, 0);
    }

    public static boolean has(ServerPlayer player, Guhboek book) {
        return (books(player) & book.bit()) != 0;
    }

    public static int count(ServerPlayer player) {
        return Integer.bitCount(books(player) & ((1 << Guhboek.values().length) - 1));
    }

    /** The books the quiz may ask about: the ones you have collected or read on a lectern. */
    public static int known(ServerPlayer player) {
        return books(player) | Leeszaal.read(player);
    }

    public static long rightAnswers(ServerPlayer player) {
        return GuhQuests.saved(player).getLongOr(RIGHT, 0L);
    }

    static long now(ServerPlayer player) {
        return player.level().getServer().overworld().getGameTime();
    }

    /** Checks the inventory for guh books that aren't in the collection yet (every second, and after getting one). */
    public static void collect(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        int have = saved.getIntOr(BOOKS, 0);
        int found = have;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            Guhboek book = Guhboek.of(player.getInventory().getItem(i));
            if (book != null) {
                found |= book.bit();
            }
        }
        if (found == have) {
            return;
        }
        saved.putInt(BOOKS, found);
        int total = Guhboek.values().length;
        int count = Integer.bitCount(found);
        for (Guhboek book : Guhboek.values()) {
            if ((found & book.bit()) != 0 && (have & book.bit()) == 0) {
                player.sendSystemMessage(Component.translatable("quest.guhs.bieb.collected", book.title(), count, total)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.1f);
        GuhAdvancements.grant(player, "bieb_eerste_boek");
        if (count >= 6) {
            GuhAdvancements.grant(player, "bieb_zes_boeken");
        }
        if ((found & Guhboek.GEHEIM.bit()) != 0) {
            GuhAdvancements.grant(player, "bieb_geheim");
        }
        if (count >= total) {
            GuhAdvancements.grant(player, "bieb_alle_boeken");
            award(player, "guhmension/bieb_boekenwurm");
            player.sendSystemMessage(Component.translatable("quest.guhs.bieb.all_collected").withStyle(ChatFormatting.GOLD));
        }
    }

    /** Awards one of our visible advancements that the mod grants itself (their criterion is "done"). */
    static void award(ServerPlayer player, String path) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id(path));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // --- talking -----------------------------------------------------------------------------------------------------

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.6f, 1.2f);
        CompoundTag saved = GuhQuests.saved(player);
        collect(player);
        if (!saved.getBooleanOr(WELCOMED, false)) {
            saved.putBoolean(WELCOMED, true);
            GuhQuests.say(player, npc, "quest.guhs.bieb.hello_first");
        } else {
            GuhQuests.say(player, npc, "quest.guhs.bieb.hello");
        }
        open(npc, player);
    }

    /** Sends the screen (again). */
    static void open(GuhNpcEntity npc, ServerPlayer player) {
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new BibliotheekPayloads.Open(npc.getId(), screenData(player)));
    }

    /** What the screen shows: the collection, the books read and taken, the vouchers and the quiz question. */
    public static CompoundTag screenData(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        CompoundTag data = new CompoundTag();
        data.putInt("Books", books(player));
        data.putInt("Read", Leeszaal.read(player));
        data.putInt("Taken", saved.getIntOr(Leeszaal.TAKEN, 0));
        data.putInt("Bonnen", GuhQuests.count(player, BibliotheekFeature.BOEKENBON.get()));
        data.putInt("Right", Long.bitCount(rightAnswers(player)));
        long wait = saved.getLongOr(QUIZ_WAIT, 0L) - now(player);
        if (wait > 0) {
            data.putInt("QuizWait", (int) ((wait + 19) / 20));
        } else if (known(player) == 0) {
            data.putBoolean("QuizNone", true);
        } else {
            int question = currentQuestion(player);
            data.putInt("Question", question);
            data.putInt("Order", saved.getIntOr(ORDER, 0));
        }
        return data;
    }

    /** The question the player is being asked (a new one is picked if there is none). */
    public static int currentQuestion(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        int current = saved.getIntOr(QUESTION, 0) - 1;
        int known = known(player);
        if (current >= 0 && current < Guhboek.questionCount() && (known & Guhboek.bookOfQuestion(current).bit()) != 0) {
            return current;
        }
        List<Integer> asked = new ArrayList<>(), open = new ArrayList<>();
        long right = rightAnswers(player);
        for (int q = 0; q < Guhboek.questionCount(); q++) {
            if ((known & Guhboek.bookOfQuestion(q).bit()) != 0) {
                asked.add(q);
                if ((right & (1L << q)) == 0) {
                    open.add(q);
                }
            }
        }
        List<Integer> pool = open.isEmpty() ? asked : open;
        if (pool.isEmpty()) {
            return -1;
        }
        int question = pool.get(player.getRandom().nextInt(pool.size()));
        saved.putInt(QUESTION, question + 1);
        saved.putInt(ORDER, player.getRandom().nextInt(ORDERS.length));
        return question;
    }

    /** A button in the screen. */
    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.BIBLIOTHECARIS || player.distanceToSqr(npc) > 64) {
            return;
        }
        if (action == SHOP) {
            npc.openShop(player);
            return;
        }
        if (action == REFRESH) {                                   // (the screen's countdown ran out: the next question)
            open(npc, player);
            return;
        }
        if (action >= ANSWER && action < ANSWER + Guhboek.ANSWERS) {
            answer(npc, player, action - ANSWER);
        }
        open(npc, player);
    }

    /** The player clicked answer button `position` (0..2) for the current question. */
    static void answer(GuhNpcEntity npc, ServerPlayer player, int position) {
        CompoundTag saved = GuhQuests.saved(player);
        int question = saved.getIntOr(QUESTION, 0) - 1;
        if (question < 0 || saved.getLongOr(QUIZ_WAIT, 0L) > now(player)) {
            return;
        }
        int[] order = ORDERS[Math.floorMod(saved.getIntOr(ORDER, 0), ORDERS.length)];
        saved.putInt(QUESTION, 0);
        Guhboek book = Guhboek.bookOfQuestion(question);
        if (order[position] == 0) {
            long right = saved.getLongOr(RIGHT, 0L);
            if ((right & (1L << question)) == 0) {
                saved.putLong(RIGHT, right | (1L << question));
                give(player, new ItemStack(BibliotheekFeature.BOEKENBON.get()));
                GuhQuests.say(player, npc, "quest.guhs.bieb.right");
            } else {
                give(player, new ItemStack(ModItems.KAAS_KNABBELS.get(), 2));
                GuhQuests.say(player, npc, "quest.guhs.bieb.right_again");
            }
            player.level().playSound(null, npc.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.5f, 1.6f);
            GuhAdvancements.grant(player, "bieb_kwis");
            boolean again = (right & (1L << question)) != 0;
            if (Long.bitCount(saved.getLongOr(RIGHT, 0L)) >= Guhboek.questionCount() && (right & (1L << question)) == 0) {
                GuhAdvancements.grant(player, "bieb_kwismeester");
                GuhQuests.say(player, npc, "quest.guhs.bieb.kwismeester");
            }
            saved.putLong(QUIZ_WAIT, now(player) + (again ? WAIT_AGAIN : WAIT_RIGHT));   // (no knabbel farm)
        } else {
            GuhQuests.say(player, npc, "quest.guhs.bieb.wrong", Component.translatable(Guhboek.answerKey(question, 0)), book.title());
            player.level().playSound(null, npc.blockPosition(), ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.6f, 0.6f);
            saved.putLong(QUIZ_WAIT, now(player) + WAIT_WRONG);
        }
    }

    // --- the shop ------------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(clothes(3, GuhClothes.BIEB_LEESBRIL));
        offers.add(clothes(5, GuhClothes.BIEB_VEST));
        offers.add(clothes(8, GuhClothes.BIEB_HOED));
        offers.add(new MerchantOffer(new ItemCost(BibliotheekFeature.BOEKENBON.get(), 2), new ItemStack(BibliotheekFeature.GUHFAUTEUIL_ITEM.get()),
                Integer.MAX_VALUE, 0, 0));
        for (Guhboek book : Guhboek.values()) {
            if (!book.secret()) {
                offers.add(new MerchantOffer(new ItemCost(BibliotheekFeature.BOEKENBON.get(), 2), book.stack(), Integer.MAX_VALUE, 0, 0));
            }
        }
        return offers;
    }

    private static MerchantOffer clothes(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(BibliotheekFeature.BOEKENBON.get(), price), new ItemStack(ModItems.clothingItem(clothes)),
                Integer.MAX_VALUE, 0, 0);
    }
}
