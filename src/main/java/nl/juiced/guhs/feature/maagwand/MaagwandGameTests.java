package nl.juiced.guhs.feature.maagwand;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/** 1.8.1: the maagwand: the table, the BOERP (nothing lost), training food, and the bonuses that can be seen on the server. */
public class MaagwandGameTests {
    private static final String BATCH = "maagwand", KAMER = "vadskracht_test_kamer";
    /** Food of 1.8.1 made by other branches: the table may name them before they exist. */
    private static final java.util.Set<String> NIEUW = java.util.Set.of("patatje_kaassaus", "patatje_vads", "patatje_oorlog",
            "patatje_guhspeciaal", "dubbel_gefrituurde_kaasknabbels", "driedubbel_gefrituurde_kaasknabbels", "kaaskroket",
            "frikandel_guhspeciaal", "bitterguhllen", "kaassoufflguh", "vetste_hap");

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        GuhQuests.saved(p).remove(Maagwand.KEY);
        return p;
    }

    private static void eet(ServerPlayer p, ItemStack stack) {
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(p, stack, 0, ItemStack.EMPTY));
    }

    /** Every id of the table is a real item, apart from the new food of the other 1.8.1 branches. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void tabelKlopt(GameTestHelper helper) {
        for (Maagwand.Stap s : Maagwand.STAPPEN) {
            for (var lijst : java.util.List.of(s.trainvoer(), s.opent())) {
                for (String id : lijst) {
                    helper.assertTrue(Maagwand.item(id).isPresent() || NIEUW.contains(id), "unknown food in the maagwand table: " + id);
                }
            }
        }
        helper.assertTrue(Maagwand.nodigeLaag(ModItems.KAAS_KNABBELS.get()) == Maagwand.MIN, "kaasknabbels may always");
        helper.assertTrue(Maagwand.nodigeLaag(ModItems.GEFRITUURDE_KAASKNABBELS.get()) == 2, "fried: layer 2");
        helper.succeed();
    }

    /** Too vadsig: at the end of the bite the stomach protests, the item stays and the player waits a little. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void boerpItemBlijft(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ItemStack vads = new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 3);
        helper.assertFalse(Maagwand.magEten(p, vads), "layer 1 may not");
        p.getFoodData().setFoodLevel(10);
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Tick(p, vads, MaagwandEten.STOP_BIJ + 5));
        helper.assertFalse(p.getCooldowns().isOnCooldown(vads), "not while still chewing");
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Tick(p, vads, MaagwandEten.STOP_BIJ));
        helper.assertTrue(p.getCooldowns().isOnCooldown(vads), "BOERP and a breather");
        helper.assertTrue(vads.getCount() == 3 && p.getFoodData().getFoodLevel() == 10, "nothing lost, nothing eaten");
        Maagwand.zet(p, 2, 0);
        helper.assertTrue(Maagwand.magEten(p, vads), "layer 2 may");
        p.setGameMode(GameType.CREATIVE);
        helper.assertTrue(Maagwand.magEten(p, new ItemStack(ModItems.KAAS_KNABBELS.get())), "creative always may");
        GuhQuests.saved(p).remove(Maagwand.KEY);
        helper.succeed();
    }

    /** All kinds of training food fill the same count; other food doesn't; the count starts over per layer. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void trainvoerTelt(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Item knabbels = ModItems.KAAS_KNABBELS.get();
        Item sate = Maagwand.item("kaasknabbelsate").orElseThrow();
        eet(p, new ItemStack(knabbels));
        eet(p, new ItemStack(sate));
        helper.assertTrue(Maagwand.teller(p) == 2, "two kinds, one count");
        eet(p, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get()));
        helper.assertTrue(Maagwand.teller(p) == 2, "not training food of this layer");
        Maagwand.zet(p, Maagwand.MIN, 99);
        eet(p, new ItemStack(knabbels));
        helper.assertTrue(Maagwand.laag(p) == 2 && Maagwand.teller(p) == 0, "100 eaten: Knabbelmaag");
        eet(p, new ItemStack(knabbels));
        helper.assertTrue(Maagwand.teller(p) == 0, "kaasknabbels no longer count");
        Maagwand.zet(p, Maagwand.MAX, 0);
        eet(p, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get()));
        helper.assertTrue(Maagwand.laag(p) == Maagwand.MAX, "the top stays the top");
        GuhQuests.saved(p).remove(Maagwand.KEY);
        helper.succeed();
    }

    /** The bonuses: faster eating from layer 2, no hunger cramps from layer 4. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void bonussen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ItemStack hap = new ItemStack(ModItems.KAAS_KNABBELS.get());
        var start = new LivingEntityUseItemEvent.Start(p, hap, net.minecraft.world.InteractionHand.MAIN_HAND, 32);
        NeoForge.EVENT_BUS.post(start);
        helper.assertTrue(start.getDuration() == 32, "layer 1 eats like everyone");
        Maagwand.zet(p, 2, 0);
        start = new LivingEntityUseItemEvent.Start(p, hap, net.minecraft.world.InteractionHand.MAIN_HAND, 32);
        NeoForge.EVENT_BUS.post(start);
        helper.assertTrue(start.getDuration() == 24, "layer 2 eats faster");
        p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100));
        helper.assertTrue(p.hasEffect(MobEffects.HUNGER), "cramps at layer 2");
        p.removeAllEffects();
        Maagwand.zet(p, 4, 0);
        p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100));
        helper.assertFalse(p.hasEffect(MobEffects.HUNGER), "no cramps at layer 4");
        GuhQuests.saved(p).remove(Maagwand.KEY);
        helper.succeed();
    }
}
