package nl.juiced.guhs.feature.safari.lobby;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.gametest.GuhTest;

/** Op Safari! step 2 (S2): the giftshop's buy (price, cap, requirement) and the aaihoek's daily cap. */
public class SafariLobbyGameTests {
    private static final String BATCH = "safari";

    /** A buy takes the price once delivered, never when too expensive, never past the cap, never when a requirement says no. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void winkelKoop(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID id = UUID.randomUUID();
        AtomicInteger geleverd = new AtomicInteger();
        SafariWinkel.aanbod("test_ding").groep("voer").icoon(() -> new ItemStack(Items.CARROT)).prijs(30).max(2)
                .lever((p, a) -> {
                    geleverd.incrementAndGet();
                    return true;
                }).registreer();
        SafariWinkel.aanbod("test_nee").groep("voer").prijs(1)
                .eis((s, u) -> Component.literal("nog niet")).lever((p, a) -> true).registreer();
        try {
            helper.assertTrue(SafariWinkel.koop(server, id, null, "test_ding") == SafariWinkel.Uitkomst.TE_DUUR, "0 coins: too expensive");
            SafariMunten.geef(server, id, 100, "test");
            helper.assertTrue(SafariWinkel.koop(server, id, null, "test_ding") == SafariWinkel.Uitkomst.OK, "100 coins: bought");
            helper.assertTrue(SafariMunten.saldo(server, id) == 70 && geleverd.get() == 1, "30 paid, delivered once");
            helper.assertTrue(SafariWinkel.koop(server, id, null, "test_ding") == SafariWinkel.Uitkomst.OK, "a second one");
            helper.assertTrue(SafariWinkel.koop(server, id, null, "test_ding") == SafariWinkel.Uitkomst.MAX, "max 2");
            helper.assertTrue(SafariMunten.saldo(server, id) == 40 && SafariWinkel.gekocht(server, id, "test_ding") == 2, "40 left, 2 bought");
            helper.assertTrue(SafariWinkel.koop(server, id, null, "test_nee") == SafariWinkel.Uitkomst.EIS, "a requirement blocks the buy");
            helper.assertTrue(SafariWinkel.koop(server, id, null, "bestaat_niet") == SafariWinkel.Uitkomst.ONBEKEND, "unknown offer");
            helper.assertTrue(SafariMunten.saldo(server, id) == 40, "nothing charged for refused buys");
        } finally {
            SafariWinkel.vergeet("test_ding");
            SafariWinkel.vergeet("test_nee");
            SafariSpeler.wis(server, id);
        }
        helper.succeed();
    }

    /** Four chores a day pay 15 each (+1 for the right food); a fifth pays nothing; the same chore twice pays once. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void aaihoekDagMax(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID id = UUID.randomUUID();
        try {
            helper.assertTrue(Aaihoek.klusGedaan(server, id, "voer_geitje", true) == Aaihoek.LOON + 1, "right food: 15 + 1");
            helper.assertTrue(Aaihoek.klusGedaan(server, id, "voer_geitje", true) == -1, "the same chore again today: nothing");
            helper.assertTrue(Aaihoek.klusGedaan(server, id, "borstel_geitje", false) == Aaihoek.LOON, "brushing: 15");
            helper.assertTrue(Aaihoek.klusGedaan(server, id, "borstel_alpaca", false) == Aaihoek.LOON, "third chore: 15");
            helper.assertTrue(Aaihoek.klusGedaan(server, id, "alpaca_wandeling", false) == Aaihoek.LOON, "fourth chore: 15");
            helper.assertTrue(Aaihoek.klusGedaan(server, id, "borstel_cavia", false) == 0, "fifth chore: the day's cap is reached");
            helper.assertTrue(SafariMunten.saldo(server, id) == Aaihoek.LOON * Aaihoek.PER_DAG + 1, "61 coins in total");
            // today's favourite is always one of the animal's two foods, and stable within a day
            for (String soort : Aaihoek.DIEREN) {
                String f = Aaihoek.favoriet(soort, 12345);
                helper.assertTrue(Aaihoek.VOER.get(soort).contains(f) && f.equals(Aaihoek.favoriet(soort, 12345)), "favourite of " + soort);
            }
        } finally {
            SafariSpeler.wis(server, id);
        }
        helper.succeed();
    }

    /** lobby.json is in the jar: every character has a spot, the eight experts have a zone and a counter. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void lobbyPlekken(GameTestHelper helper) {
        LobbyPlekken l = LobbyPlekken.get();
        helper.assertTrue(l.npcs.size() == 13, "13 characters, got " + l.npcs.size());
        helper.assertTrue(l.npcs.stream().filter(n -> n.zone() != null && n.balie() != null).count() == 8, "8 experts with a zone and counter");
        helper.assertTrue(l.pennen.size() == 4 && l.alpacaLus.size() >= 4, "4 pens and the alpaca's loop");
        helper.assertTrue(l.lodge.getXsize() > 4 && l.npc("hoofdranger") != null, "the lodge box and the hoofdranger");
        helper.succeed();
    }
}
