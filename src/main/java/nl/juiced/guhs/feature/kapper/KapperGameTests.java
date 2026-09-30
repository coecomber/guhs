package nl.juiced.guhs.feature.kapper;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
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
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * GameTests of Knip &amp; Vads: the rewards (+1), hair on your own tamed guh (kapsels, dyes, the scissors), a whole
 * kappersshow on a small test salon (kapper_test_salon: Krulletje, the showstoel, a second chair further away, a
 * haarwasbak), the feest round for the Knusfeest, the shop, the role, the tags and the customers that never stay behind.
 * The show is ticked by hand (KappersShow.tick), so a whole show fits in one test tick.
 */
public class KapperGameTests {
    private static final String EMPTY = "empty", SALON = "kapper_test_salon";

    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id(name.contains("/") ? name : "quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static GuhNpcEntity krulletje(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(1),
                n -> n.getKind() == GuhNpcEntity.Kind.KAPPERGUH);
        helper.assertTrue(npcs.size() == 1, "there is one Kapper Krulletje: " + npcs.size());
        return npcs.get(0);
    }

    private static void tickUntil(GameTestHelper helper, GuhNpcEntity npc, KappersShow show, KappersShow.Fase fase) {
        for (int i = 0; i < 2000 && show.fase() != fase; i++) {
            show.tick(npc);
        }
        helper.assertTrue(show.fase() == fase, "reached " + fase + " (still " + show.fase() + ")");
    }

    /** Wash, cut and dye exactly as the picture says, then the föhn. */
    private static void perfect(GameTestHelper helper, GuhNpcEntity npc, ServerPlayer p, KappersShow show) {
        tickUntil(helper, npc, show, KappersShow.Fase.KLANT);
        Kapsel wens = show.wens();
        Haarverf verf = show.wensVerf();
        for (int i = 0; i < KappersShow.WASSEN; i++) {
            KappersShow.action(npc, p, KappersShow.WAS);
        }
        KappersShow.action(npc, p, KappersShow.KNIP + wens.ordinal());
        KappersShow.action(npc, p, KappersShow.VERF + (verf == null ? KappersShow.NATUREL : verf.ordinal()));
        KapperKlantEntity klant = show.klant(helper.getLevel());
        helper.assertTrue(klant != null && klant.getClothes(GuhClothes.Slot.HAAR) == wens.kleding, "the customer has its new hairstyle at once");
        helper.assertTrue(verf == null ? klant.getHaarkleur() == -1 : klant.getHaarkleur() >= 0, "and its colour");
        KappersShow.action(npc, p, KappersShow.FOHN);
        helper.assertTrue(show.fase() == KappersShow.Fase.TUSSEN, "the föhn finishes the customer: " + show.fase());
        helper.assertTrue(klant.weg > 0, "the customer leaves happily");
    }

    // --- rewards: every rule +1 ----------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void kapperBeloningenZijnEenMeer(GameTestHelper helper) {
        helper.assertTrue(KappersShow.munten(0) == 0, "nothing for no points");
        for (int score = 1; score <= 600; score++) {
            int basis = Math.min(15, score / 20);
            helper.assertTrue(KappersShow.munten(score) == basis + 1, "krulmunten for " + score + ": " + KappersShow.munten(score));
        }
        helper.assertTrue(KappersShow.FIRST_MUNTEN == 3 + 1, "the first show: 3 + 1");
        helper.assertTrue(KappersShow.punten(true, true, 20, 0) == 25 && KappersShow.punten(true, true, 0, 3) == 21
                && KappersShow.punten(true, true, 0, 9) == 25, "perfect: 15 + seconds/2 + 2 per combo (max 5)");
        helper.assertTrue(KappersShow.punten(true, false, 20, 4) == 8 && KappersShow.punten(false, true, 20, 4) == 4
                && KappersShow.punten(false, false, 30, 2) == 1, "half right, or a thank you");
        helper.assertTrue(KappersShow.geduld(0, false) == 600 && KappersShow.geduld(20, false) == 320 && KappersShow.geduld(3, true) == 460,
                "less patience for every next customer");
        helper.succeed();
    }

    // --- your own tamed guh --------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void kapperKapselsEnVervenOpEigenGuh(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 1, 1));
        ServerPlayer other = player(helper, new BlockPos(2, 1, 1));
        try {
            GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 3));
            guh.tame(p);
            // a dye without a hairstyle: njeg
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KapperFeature.HAARVERF.get(Haarverf.MINT).get()));
            guh.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(guh.getHaarkleur() == -1 && count(p, KapperFeature.HAARVERF.get(Haarverf.MINT).get()) == 1, "no dye on bald guhs");
            // a hairstyle: permanent, the item is used up, nothing comes back
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.clothingItem(GuhClothes.KAPSEL_KRULLEN)));
            guh.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(guh.getClothes(GuhClothes.Slot.HAAR) == GuhClothes.KAPSEL_KRULLEN && p.getMainHandItem().isEmpty(), "krullen!");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.clothingItem(GuhClothes.KAPSEL_KNOTJES)));
            guh.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(guh.getClothes(GuhClothes.Slot.HAAR) == GuhClothes.KAPSEL_KNOTJES && p.getMainHandItem().isEmpty()
                    && count(p, ModItems.clothingItem(GuhClothes.KAPSEL_KRULLEN)) == 0, "the old hair is cut off, not given back");
            helper.assertTrue(guh.takeOffClothes().isEmpty() && guh.getClothes(GuhClothes.Slot.HAAR) == GuhClothes.KAPSEL_KNOTJES,
                    "undressing keeps the hair");
            // dye it
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KapperFeature.HAARVERF.get(Haarverf.MINT).get()));
            guh.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(guh.getHaarkleur() == Haarverf.MINT.rgb && KapperHaar.verfVan(guh) == Haarverf.MINT && p.getMainHandItem().isEmpty(), "mint hair");
            // rainbow hair changes colour by itself
            KapperHaar.verf(guh, Haarverf.REGENBOOG);
            helper.assertTrue(KapperHaar.verfVan(guh) == Haarverf.REGENBOOG && guh.getHaarkleur() >= 0, "rainbow hair");
            helper.assertTrue(KapperHaar.regenboog(0) != KapperHaar.regenboog(40), "the rainbow runs through the colours");
            // someone else's guh: njeg
            other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.clothingItem(GuhClothes.KAPSEL_HANENKAM)));
            guh.mobInteract(other, InteractionHand.MAIN_HAND);
            helper.assertTrue(guh.getClothes(GuhClothes.Slot.HAAR) == GuhClothes.KAPSEL_KNOTJES && !other.getMainHandItem().isEmpty(), "only the owner");
            // the scissors: shift + right-click cuts it all off
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KapperFeature.KAPPERSSCHAAR.get()));
            p.setShiftKeyDown(true);
            guh.mobInteract(p, InteractionHand.MAIN_HAND);
            p.setShiftKeyDown(false);
            helper.assertTrue(guh.getClothes(GuhClothes.Slot.HAAR) == null && guh.getHaarkleur() == -1 && KapperHaar.verfVan(guh) == null
                    && !p.getMainHandItem().isEmpty(), "bald and natural again, the scissors stay");
            // the Knus tab and the advancements
            helper.assertTrue(KnusVoortgang.heeft(p, KapperVoortgang.KAPSELS, "kapsel_krullen") && KnusVoortgang.heeft(p, KapperVoortgang.KAPSELS, "kapsel_knotjes")
                    && KnusVoortgang.heeft(p, KapperVoortgang.KAPSELS, "haarverf_mint"), "the collection");
            helper.assertTrue(KnusVoortgang.teller(p, KapperVoortgang.EIGEN) == 3, "three times to the kapper: " + KnusVoortgang.teller(p, KapperVoortgang.EIGEN));
            helper.assertTrue(advancement(p, "kapper_eigen_guh") && advancement(p, "kapper_haarverf") && advancement(p, "knuffeldal/kapper_eerste_kapsel"),
                    "the advancements");
            // saved and loaded
            guh.wear(GuhClothes.KAPSEL_MATJE);
            KapperHaar.verf(guh, Haarverf.PERZIK);
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            guh.saveWithoutId(tag);
            GuhEntity copy = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            copy.load(tag);
            helper.assertTrue(copy.getClothes(GuhClothes.Slot.HAAR) == GuhClothes.KAPSEL_MATJE && copy.getHaarkleur() == Haarverf.PERZIK.rgb
                    && KapperHaar.verfVan(copy) == Haarverf.PERZIK, "saved");
        } finally {
            leave(helper, p, other);
        }
        helper.succeed();
    }

    // --- a whole kappersshow -------------------------------------------------------------------------------------------------

    @GuhTest(template = SALON, timeoutTicks = 200)
    public static void kapperShowScoortEnBeloont(GameTestHelper helper) {
        GuhNpcEntity npc = krulletje(helper);
        ServerPlayer p = player(helper, new BlockPos(6, 2, 9));
        try {
            KappersShow show = KappersShow.of(npc);
            // talking gives the scissors (once)
            KappersShow.talk(npc, p);
            KappersShow.talk(npc, p);
            helper.assertTrue(count(p, KapperFeature.KAPPERSSCHAAR.get()) == 1 && Features.isLoaned(new ItemStack(KapperFeature.KAPPERSSCHAAR.get())),
                    "Krulletje lends one pair of scissors");
            KappersShow.action(npc, p, KappersShow.START);
            helper.assertTrue(show.isRunning() && KappersShow.isPlaying(p) && Minigames.KAPPER.equals(Minigames.playing(p)), "the show is on");
            // the showstoel is the chair nearest to Krulletje
            List<BlockPos> stoelen = new ArrayList<>();
            BlockPos.betweenClosedStream(helper.getBounds()).filter(pos -> helper.getLevel().getBlockState(pos).is(KapperFeature.KAPPERSSTOEL.get()))
                    .forEach(pos -> stoelen.add(pos.immutable()));
            helper.assertTrue(stoelen.size() == 2 && show.stoel() != null, "two chairs, one found: " + stoelen);
            BlockPos near = stoelen.stream().min((a, b) -> Double.compare(a.distToCenterSqr(npc.position()), b.distToCenterSqr(npc.position()))).orElseThrow();
            helper.assertTrue(show.stoel().equals(near), "the showstoel is the nearest chair");
            // no hunger, no damage
            p.hurt(helper.getLevel().damageSources().fall(), 5f);
            helper.assertTrue(p.getHealth() == p.getMaxHealth(), "no damage in the salon");
            // customer 1: cut before washing doesn't work; then perfect
            tickUntil(helper, npc, show, KappersShow.Fase.KLANT);
            KapperKlantEntity klant = show.klant(helper.getLevel());
            helper.assertTrue(klant != null && klant.isInSittingPose() && klant.blockPosition().equals(show.stoel()), "the customer sits in the chair");
            Kapsel wens = show.wens();
            KappersShow.action(npc, p, KappersShow.KNIP + wens.ordinal());
            helper.assertTrue(klant.getClothes(GuhClothes.Slot.HAAR) != wens.kleding || show.gewassen() == 0, "first wash");
            KappersShow.action(npc, p, KappersShow.FOHN);
            helper.assertTrue(show.fase() == KappersShow.Fase.KLANT, "no föhn before cutting");
            perfect(helper, npc, p, show);
            int na1 = show.score();
            helper.assertTrue(na1 >= 15 && show.combo() == 1, "a perfect customer: " + na1);
            helper.assertTrue(KnusVoortgang.teller(p, KapperVoortgang.KLANTEN) == 1 && KnusVoortgang.teller(p, KapperVoortgang.PERFECT) == 1
                    && advancement(p, "kapper_eerste_klant") && advancement(p, "knuffeldal/kapper_eerste_kapsel"), "counted");
            helper.assertTrue(KnusVoortgang.heeft(p, KapperVoortgang.KAPSELS, wens.id()), "in the collection");
            // customer 2: perfect again (combo)
            perfect(helper, npc, p, show);
            helper.assertTrue(show.combo() == 2 && show.score() > na1 + 15, "combo 2: " + show.score());
            // customer 3: the wrong hairstyle
            tickUntil(helper, npc, show, KappersShow.Fase.KLANT);
            Kapsel fout = Kapsel.values()[(show.wens().ordinal() + 1) % Kapsel.values().length];
            int voor = show.score();
            for (int i = 0; i < KappersShow.WASSEN; i++) {
                KappersShow.action(npc, p, KappersShow.WAS);
            }
            KappersShow.action(npc, p, KappersShow.KNIP + fout.ordinal());
            KappersShow.action(npc, p, KappersShow.FOHN);
            helper.assertTrue(show.combo() == 0 && show.score() - voor <= 4, "no combo for a wrong hairstyle");
            // customer 4: too slow
            tickUntil(helper, npc, show, KappersShow.Fase.KLANT);
            int voor4 = show.score();
            helper.assertTrue(!show.fotoZichtbaar() || show.nr() >= KappersShow.FOTO_WEG_VANAF, "from customer 4 the picture goes away (after a while)");
            tickUntil(helper, npc, show, KappersShow.Fase.TUSSEN);
            helper.assertTrue(show.score() == voor4 && show.nr() == 4, "too slow: no points");
            // stop: krulmunten, the record, the welcome present
            int score = show.score();
            KappersShow.action(npc, p, KappersShow.STOP);
            helper.assertTrue(!show.isRunning() && !KappersShow.isPlaying(p), "stopped");
            helper.assertTrue(count(p, KapperFeature.KRULMUNT.get()) == KappersShow.munten(score) + KappersShow.FIRST_MUNTEN,
                    "krulmunten: " + count(p, KapperFeature.KRULMUNT.get()) + " for " + score);
            helper.assertTrue(KappersShow.best(p) == score && Highscores.remember(p, KappersShow.BOARD, score, false) == false, "the record");
            helper.assertTrue(KnusVoortgang.teller(p, KapperVoortgang.RECORD) == score && advancement(p, "kapper_eerste_show"), "the Knus record");
            helper.assertTrue(GuhQuests.saved(p).getBooleanOr("guhs_kapper_first", false), "the first show is remembered");
        } finally {
            leave(helper, p);
        }
        helper.succeedWhen(() -> helper.assertTrue(KappersShow.leftovers(helper.getLevel(), helper.getBounds().inflate(2)).isEmpty()
                && helper.getLevel().getEntitiesOfClass(KapperKlantEntity.class, helper.getBounds().inflate(2)).isEmpty(), "every customer went home"));
    }

    /** The Burgemeester asked for feestkapsels: the feest round gives the feestkapselset (and Knusfeest.gemaakt). */
    @GuhTest(template = SALON, timeoutTicks = 200)
    public static void kapperFeestkapselsVoorHetKnusfeest(GameTestHelper helper) {
        GuhNpcEntity npc = krulletje(helper);
        ServerPlayer p = player(helper, new BlockPos(6, 2, 9));
        try {
            KappersShow show = KappersShow.of(npc);
            KappersShow.action(npc, p, KappersShow.FEEST);
            helper.assertTrue(!show.isRunning(), "no feest round when the Burgemeester didn't ask");
            Knusfeest.nieuweRonde(p, 0, EnumSet.of(Feesttaak.FEESTKAPSELS));
            helper.assertTrue(Knusfeest.open(p, Feesttaak.FEESTKAPSELS), "asked");
            KappersShow.action(npc, p, KappersShow.FEEST);
            helper.assertTrue(show.isRunning() && show.aantal() == KappersShow.FEEST_KLANTEN, "the feest round: 6 guests");
            for (int i = 0; i < KappersShow.FEEST_KLANTEN; i++) {
                perfect(helper, npc, p, show);
                helper.assertTrue(show.wensVerf() != null, "feest hair is always coloured");
            }
            tickUntil(helper, npc, show, KappersShow.Fase.IDLE);
            helper.assertTrue(show.score() >= 0 && count(p, KapperFeature.FEESTKAPSELSET.get()) == 1, "the feestkapselset");
            helper.assertTrue(new ItemStack(KapperFeature.FEESTKAPSELSET.get()).is(KnusTags.FEESTKAPSELS), "in the tag guhs:knus/feestkapsels");
            helper.assertTrue(Knusfeest.stap(p, Feesttaak.FEESTKAPSELS) == Knusfeest.Stap.GEMAAKT && advancement(p, "knusfeest_feestkapsels_gemaakt"), "made");
        } finally {
            Knusfeest.vergeet(p);
            leave(helper, p);
        }
        helper.succeed();
    }

    // --- shop, role, tags, bones, customers --------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void kapperWinkelRolEnTags(GameTestHelper helper) {
        helper.assertTrue(Features.role(GuhNpcEntity.Kind.KAPPERGUH) == KapperFeature.role() && KapperFeature.role() != Binnenkort.ROLE, "Krulletje's own role");
        var offers = KapperFeature.shop();
        helper.assertTrue(offers.size() == Kapsel.values().length + Haarverf.values().length + 3, "the shop: " + offers.size());
        for (MerchantOffer o : offers) {
            helper.assertTrue(o.getBaseCostA().is(KapperFeature.KRULMUNT.get()) && o.getCostB().isEmpty(), "paid with krulmunten");
        }
        for (Kapsel k : Kapsel.values()) {
            helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(ModItems.clothingItem(k.kleding))), "sells " + k);
        }
        helper.assertTrue(new ItemStack(KapperFeature.KRULMUNT.get()).is(KnusTags.GRIJPTICKETS), "a krulmunt is a grijpticket");
        helper.assertTrue(Features.isLoaned(new ItemStack(KapperFeature.KAPPERSSCHAAR.get())), "the scissors are lent");
        // the HAAR slot: every hairstyle there, with its hat-proof top and the Pluisguh's tuft stepping aside
        for (Kapsel k : Kapsel.values()) {
            helper.assertTrue(k.kleding.slot == GuhClothes.Slot.HAAR && k.kleding.shows("outfit_haar_" + k.stijl() + "_kruin")
                    && k.kleding.shows("pluis_kuif") && !GuhClothes.Slot.kleding().contains(k.kleding.slot), "hair bones of " + k);
        }
        helper.assertTrue(GuhClothes.KAPPERSCAPE.slot == GuhClothes.Slot.BODY, "the cape is clothing");
        helper.assertTrue(KapperVoortgang.KAPSEL_LIJST.size() == 16 && KapperVoortgang.KAPSEL_LIJST.contains("kapsel_pluisbol")
                && KapperVoortgang.KAPSEL_LIJST.contains("haarverf_regenboog"), "16 collection entries (the item ids)");
        helper.succeed();
    }

    /** A customer without a show goes home by itself; customers can't be hurt. */
    @GuhTest(template = EMPTY, timeoutTicks = 120)
    public static void kapperKlantZonderShowGaatNaarHuis(GameTestHelper helper) {
        KapperKlantEntity klant = helper.spawn(KapperFeature.KAPPER_KLANT.get(), new BlockPos(2, 1, 2));
        klant.hurt(helper.getLevel().damageSources().generic(), 5f);
        helper.assertTrue(klant.getHealth() == klant.getMaxHealth() && !klant.isFood(new ItemStack(ModItems.KAAS_KNABBELS.get())), "lief and untouchable");
        helper.succeedWhen(() -> helper.assertTrue(klant.isRemoved(), "went home"));
    }
}
