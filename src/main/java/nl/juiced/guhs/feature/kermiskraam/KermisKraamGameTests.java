package nl.juiced.guhs.feature.kermiskraam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/** 1.8.1: the kermis stalls (the guh behind the counter, the prices, a round at a game stall). */
public class KermisKraamGameTests {
    private static final String BATCH = "kermiskraam", KAMER = "vadskracht_test_kamer";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        GuhQuests.saved(p).remove(KraamSpel.PERFECT);
        KraamSpel.vergeet(p);
        return p;
    }

    static int tel(Inventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        return n;
    }

    /** A counter gets exactly one stall guh, behind it; churros cost 15 and 18 Kaas Knabbels. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void kraamGuhEnPrijzen(GameTestHelper helper) {
        BlockPos blok = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(blok, KermisKraamFeature.KRAMEN.get(Kraam.CHURROS).get().defaultBlockState());
        GuhNpcEntity guh = KermisKraam.zorgVoorGuh(helper.getLevel(), blok);
        helper.assertTrue(guh != null, "a stall guh");
        helper.assertTrue(guh == KermisKraam.zorgVoorGuh(helper.getLevel(), blok), "and only one");
        helper.assertTrue(guh.blockPosition().equals(blok.south()), "behind the counter (the customer stands on the north side)");
        var offers = guh.getOffers();
        helper.assertTrue(offers.size() == 2, "two kinds of churros");
        MerchantOffer eerste = offers.get(0), tweede = offers.get(1);
        helper.assertTrue(eerste.getBaseCostA().is(ModItems.KAAS_KNABBELS.get()) && eerste.getBaseCostA().getCount() == 15
                && eerste.getResult().is(KermisKraamFeature.CHURROS.get()), "churros: 15 Kaas Knabbels");
        helper.assertTrue(tweede.getBaseCostA().getCount() == 18 && tweede.getResult().is(KermisKraamFeature.CHURROS_GUHTELLA.get()),
                "churros met guhtella: 18 Kaas Knabbels");
        guh.discard();
        helper.succeed();
    }

    /** What a score is worth. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void prijzen(GameTestHelper helper) {
        helper.assertTrue(KraamSpel.prijs(Kraam.SCHIETEN, 10).bonnen() == 2, "the top score: 2 bonnen");
        helper.assertTrue(KraamSpel.prijs(Kraam.SCHIETEN, 5).bonnen() == 1, "half: 1 bon");
        helper.assertTrue(KraamSpel.prijs(Kraam.SCHIETEN, 4).bonnen() == 0 && KraamSpel.prijs(Kraam.SCHIETEN, 4).knabbels() == 1, "less: a troostknabbel");
        helper.assertTrue(KraamSpel.prijs(Kraam.VISSEN, -1).knabbels() == 0, "gave up: nothing");
        helper.succeed();
    }

    /** A round: paid once; too fast or a wrong seed pays nothing; a second round has to wait; every stall waits on its own. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void ronde(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        BlockPos blok = p.blockPosition();
        helper.assertTrue(KraamSpel.start(p, Kraam.VISSEN, blok) == KraamSpel.Start.OK, "a round starts");
        helper.assertTrue(KraamSpel.start(p, Kraam.VISSEN, blok) == KraamSpel.Start.WACHT, "not right after one");
        long seed = KraamSpel.seed(p);
        helper.assertTrue(KraamSpel.klaar(p, seed + 1, 3) == null, "a wrong seed pays nothing");
        helper.assertTrue(KraamSpel.klaar(p, seed, 3) == null && tel(p.getInventory(), ModItems.KERMISBON.get()) == 0, "too fast pays nothing");
        KraamSpel.vergeet(p);
        KraamSpel.start(p, Kraam.VISSEN, blok);
        seed = KraamSpel.seed(p);
        KraamSpel.spoel(p, KraamSpel.KORTST);
        helper.assertTrue(KraamSpel.klaar(p, seed, 99) != null && tel(p.getInventory(), ModItems.KERMISBON.get()) == 2, "the top score, clamped: 2 bonnen");
        helper.assertTrue(KraamSpel.klaar(p, seed, 3) == null && tel(p.getInventory(), ModItems.KERMISBON.get()) == 2, "paid only once");
        helper.assertTrue(KraamSpel.start(p, Kraam.BLIKKEN, blok) == KraamSpel.Start.OK, "another stall has its own waiting time");
        KraamSpel.vergeet(p);
        helper.succeed();
    }

    /** All five kermis foods exist and can be eaten. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void eten(GameTestHelper helper) {
        for (Kraam k : Kraam.values()) {
            for (Kraam.Waar w : k.waren) {
                ItemStack s = new ItemStack(BuiltInRegistries.ITEM.getValue(Guhs.id(w.item())));
                helper.assertTrue(s.has(DataComponents.FOOD), w.item() + " is food");
            }
        }
        helper.succeed();
    }
}
