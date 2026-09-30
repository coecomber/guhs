package nl.juiced.guhs.feature.mewtwo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandEvents;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.VariantGedragen;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the kloon-eiland (slice mewtwo): the notes / tank / meal steps of the questline, the Guhtwo once per
 * player, knabbel telekinesis (range 8), x2 hearts (capped at twice the normal day cap, never down), Mieuwguh only after the
 * questline, the variant's behaviour and the copy, the template (every quest spot once). Templates: mewtwo_test_lab (14 x 14
 * lab floor: the kloontank at helper (4, 2, 4), the knabbelschaal at (10, 2, 10), note spot 3 at (10, 2, 3), parts crate 2
 * at (11, 2, 3)), mewtwo_test_wei (20 x 20 grass).
 */
public class MewtwoGameTests {
    private static final String LAB = "mewtwo_test_lab", WEI = "mewtwo_test_wei", BATCH = "mewtwo";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        MewtwoVoortgang.wis(p);
        VerhaalGuhs.vergeet(p, VerhaalGuh.MEWTWO);
        Praat.vergeet(p);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            MewtwoVoortgang.wis(p);
            VerhaalGuhs.vergeet(p, VerhaalGuh.MEWTWO);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static int tel(ServerPlayer p, java.util.function.Predicate<ItemStack> wat) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (!s.isEmpty() && wat.test(s)) {
                n += s.getCount();
            }
        }
        return n;
    }

    static boolean heeftKleding(ServerPlayer p, GuhClothes c) {
        return tel(p, s -> s.is(ModItems.clothingItem(c))) > 0;
    }

    static void mewsWeg(GameTestHelper helper) {
        for (MewEntity m : helper.getLevel().getEntitiesOfClass(MewEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(64))) {
            m.discard();
        }
    }

    // =================================================================================================================
    // the questline
    // =================================================================================================================

    /** The notes (each once, the note spot block too), the professor's memory (the petje), the parts and the tank (the pakje, Mieuwguh). */
    @GuhTest(template = LAB, batch = BATCH, timeoutTicks = 200)
    public static void mewtwoNotitiesEnTank(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(7, 1, 7));
        ServerLevel level = helper.getLevel();
        // a note spot found before talking to the professor: the note is yours (once)
        BlockPos spot = helper.absolutePos(new BlockPos(10, 2, 3));
        helper.assertTrue(level.getBlockState(spot).is(MewtwoFeature.NOTITIEPLEK.get()) && level.getBlockState(spot).getValue(MewtwoBlokken.NUMMER) == 3,
                "note spot 3 in the template");
        level.getBlockState(spot).useWithoutItem(level, p, new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(spot), net.minecraft.core.Direction.UP, spot, false));
        helper.assertTrue(MewtwoVoortgang.heeftNotitie(p, 3) && tel(p, s -> LabnotitieItem.nummer(s) == 3) == 1, "note 3 found by clicking its spot");
        MewtwoVerhaal.vindNotitie(p, spot, 3);
        helper.assertTrue(tel(p, s -> s.getItem() instanceof LabnotitieItem) == 1, "the same note never twice");
        helper.assertTrue(Praat.lopend(p) != null && Praat.lopend(p).equals(MewtwoVerhaal.SCENE), "reading the note opens its page");
        // the professor asks; the crates are still closed
        MewtwoVerhaal.vindOnderdeel(p, spot, 2);
        helper.assertTrue(tel(p, s -> s.getItem() instanceof TankonderdeelItem) == 0, "no parts before the professor asks for them");
        MewtwoVerhaal.begin(p);
        helper.assertTrue(MewtwoVoortgang.stap(p) == MewtwoVoortgang.NOTITIES, "looking for the notes");
        MewtwoVerhaal.notitiesKlaar(p, null);
        helper.assertTrue(MewtwoVoortgang.stap(p) == MewtwoVoortgang.NOTITIES, "not with one note");
        for (int n = 1; n <= MewtwoFeature.NOTITIES; n++) {
            MewtwoVerhaal.vindNotitie(p, spot, n);
        }
        helper.assertTrue(MewtwoVoortgang.aantalNotities(p) == 6 && tel(p, s -> s.getItem() instanceof LabnotitieItem) == 6, "all six notes");
        MewtwoVerhaal.notitiesKlaar(p, null);
        helper.assertTrue(MewtwoVoortgang.stap(p) == MewtwoVoortgang.ONDERDELEN && heeftKleding(p, GuhClothes.MEWTWO_TRAINERPETJE),
                "the professor remembers: the trainerpetje, now the parts");
        // the tank: clicking without parts does nothing; the parts from the crates; each crate once
        BlockPos tank = helper.absolutePos(new BlockPos(4, 2, 4));
        MewtwoVerhaal.klikTank(p, tank);
        helper.assertTrue(MewtwoVoortgang.aantalIngebouwd(p) == 0, "nothing to build in yet");
        MewtwoVerhaal.vindOnderdeel(p, spot, 1);
        MewtwoVerhaal.vindOnderdeel(p, spot, 1);
        helper.assertTrue(tel(p, s -> TankonderdeelItem.soort(s) == 1) == 1, "part 1 once");
        // (a click on a glass part of the tank = a click on the tank)
        BlockPos glas = helper.absolutePos(new BlockPos(3, 3, 5));
        helper.assertTrue(MewtwoBlokken.Tankwand.tank(level, glas) != null && MewtwoBlokken.Tankwand.tank(level, glas).equals(tank), "the glass knows its tank");
        MewtwoVerhaal.klikTank(p, MewtwoBlokken.Tankwand.tank(level, glas));
        helper.assertTrue(MewtwoVoortgang.isIngebouwd(p, 1) && MewtwoVoortgang.stap(p) == MewtwoVoortgang.ONDERDELEN
                && tel(p, s -> s.getItem() instanceof TankonderdeelItem) == 0, "part 1 built in, three to go");
        for (int n = 2; n <= MewtwoFeature.ONDERDELEN; n++) {
            MewtwoVerhaal.vindOnderdeel(p, spot, n);
        }
        mewsWeg(helper);
        MewtwoVerhaal.klikTank(p, tank);
        helper.assertTrue(MewtwoVoortgang.stap(p) == MewtwoVoortgang.MAALTIJD && MewtwoVoortgang.tankHeel(p)
                && heeftKleding(p, GuhClothes.MEWTWO_TRAINERPAKJE), "the tank bubbles again: the pakje, on to the meal");
        helper.assertTrue(!level.getEntitiesOfClass(MewEntity.class, new AABB(tank).inflate(24)).isEmpty(), "Mieuwguh came to look");
        mewsWeg(helper);
        weg(helper, p);
        helper.succeed();
    }

    /** The big meal: a double portion (knabbels + snacks, in bits), then the Guhtwo is tameable once; not for another player. */
    @GuhTest(template = LAB, batch = BATCH, timeoutTicks = 200)
    public static void mewtwoMaaltijdEnEenKeerTemmen(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(8, 1, 10)), b = speler(helper, new BlockPos(12, 1, 12));
        ServerLevel level = helper.getLevel();
        BlockPos schaal = helper.absolutePos(new BlockPos(10, 2, 10));
        MewtwoVerhaal.klikSchaal(p, schaal);
        helper.assertTrue(MewtwoVoortgang.knabbels(p) == 0, "the bowl isn't for you yet");
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.MAALTIJD);
        p.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 20));
        MewtwoVerhaal.klikSchaal(p, schaal);
        helper.assertTrue(MewtwoVoortgang.knabbels(p) == 20 && MewtwoVoortgang.stap(p) == MewtwoVoortgang.MAALTIJD, "20 of the 32 in the bowl");
        p.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 30));
        p.getInventory().add(new ItemStack(ModItems.GUH_CUPCAKE.get(), 5));
        mewsWeg(helper);
        GuhEntity kopie = VerhaalGuhs.maakKopie(level, VerhaalGuh.MEWTWO, helper.absolutePos(new BlockPos(6, 1, 10)));
        MewtwoVerhaal.klikSchaal(p, schaal);
        helper.assertTrue(MewtwoVoortgang.stap(p) == MewtwoVoortgang.KLAAR && VerhaalGuhs.isVrij(p, VerhaalGuh.MEWTWO), "the meal: released");
        helper.assertTrue(tel(p, s -> s.is(ModItems.KAAS_KNABBELS.get())) == 18 + 16 && tel(p, s -> s.is(ModItems.GUH_CUPCAKE.get())) == 3,
                "exactly the double portion was taken (32 knabbels, 2 snacks; 16 knabbels back as a reward)");
        helper.assertTrue(heeftKleding(p, GuhClothes.MEWTWO_STAARTJE) && heeftKleding(p, GuhClothes.MEW_BALLONNETJE), "the staartje and the ballonnetje");
        // b hasn't done the story: the copy stays sulky for b, and taming isn't allowed
        helper.assertTrue(!VerhaalGuhs.magTemmen(b, VerhaalGuh.MEWTWO) && MewtwoVerhaal.tem(b, kopie) == null, "not for b");
        MewtwoVerhaal.klikKopie(kopie, p, InteractionHand.MAIN_HAND);
        helper.assertTrue(MewtwoVerhaal.TEM.equals(Praat.lopend(p)), "the copy asks p whether it may come along");
        GuhEntity eigen = MewtwoVerhaal.tem(p, kopie);
        helper.assertTrue(eigen != null && eigen.isOwnedBy(p) && eigen.getVariant() == GuhVariant.MEWTWO && Band.isBandGuh(eigen)
                && !VerhaalGuhs.isKopie(eigen), "p's own Guhtwo");
        helper.assertTrue(MewtwoVerhaal.tem(p, kopie) == null && VerhaalGuhs.heeftGetemd(p, VerhaalGuh.MEWTWO), "only once");
        helper.assertTrue(!kopie.isTame() && kopie.isAlive(), "the copy stays for the next visitor");
        eigen.discard();
        kopie.discard();
        mewsWeg(helper);
        weg(helper, p, b);
        helper.succeed();
    }

    // =================================================================================================================
    // the Guhtwo
    // =================================================================================================================

    /** Knabbel telekinesis: an item 7 blocks away floats to it, one 10.5 blocks away stays; not for the story copy. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 200)
    public static void mewtwoTelekineseAchtBlokken(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(18, 1, 18));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 10));
        guh.setVariant(GuhVariant.MEWTWO);
        guh.tame(p);
        guh.setOrderedToSit(true);
        ItemEntity dichtbij = new ItemEntity(helper.getLevel(), helper.absoluteVec(new Vec3(10.5, 2.1, 10.5)).x, helper.absoluteVec(new Vec3(10.5, 2.1, 10.5)).y,
                helper.absoluteVec(new Vec3(10.5, 2.1, 10.5)).z, new ItemStack(Items.PAPER, 3));
        Vec3 ver0 = helper.absoluteVec(new Vec3(14.5, 2.1, 10.5));
        ItemEntity ver = new ItemEntity(helper.getLevel(), ver0.x, ver0.y, ver0.z, new ItemStack(Items.APPLE));
        for (ItemEntity i : List.of(dichtbij, ver)) {
            i.setNoPickUpDelay();
            i.setDeltaMovement(Vec3.ZERO);
            helper.getLevel().addFreshEntity(i);
        }
        double d0 = dichtbij.distanceTo(guh);
        helper.assertTrue(d0 < MewtwoGedrag.TELEKINESE && ver.distanceTo(guh) > MewtwoGedrag.TELEKINESE, "one in range, one not: " + d0 + " / " + ver.distanceTo(guh));
        GuhEntity kopie = VerhaalGuhs.maakKopie(helper.getLevel(), VerhaalGuh.MEWTWO, helper.absolutePos(new BlockPos(3, 1, 3)));
        helper.assertTrue(!MewtwoGedrag.magTelekinese(kopie) && MewtwoGedrag.magTelekinese(guh), "the copy doesn't pull things, a tamed one does");
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(dichtbij.isAlive() && dichtbij.distanceTo(guh) < 2.6, "the paper floated to it: " + dichtbij.distanceTo(guh));
            helper.assertTrue(ver.position().subtract(ver0).horizontalDistance() < 0.6, "the apple 10.5 blocks away stayed: " + ver.position().subtract(ver0).horizontalDistance());
            // switched off with its guh-menu button: nothing moves
            VariantGedragen.van(GuhVariant.MEWTWO).speciaal(guh, p);
            helper.assertTrue(!MewtwoGedrag.magTelekinese(guh), "telekinesis off");
            VariantGedragen.van(GuhVariant.MEWTWO).speciaal(guh, p);
            helper.assertTrue(MewtwoGedrag.magTelekinese(guh), "and on again");
            guh.discard();
            kopie.discard();
            dichtbij.discard();
            ver.discard();
            weg(helper, p);
            helper.succeed();
        });
    }

    /** x2: every snack gives twice the VOEREN hearts, and the day cap doubles too; hearts never go down; the x2 moment. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 200)
    public static void mewtwoX2Hartjes(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(10, 2, 10));
        GuhEntity gewoon = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 2, 4));
        GuhEntity mewtwo = helper.spawn(ModEntities.GUH.get(), new BlockPos(14, 1, 14));
        gewoon.tame(p);
        mewtwo.tame(p);
        mewtwo.setVariant(GuhVariant.MEWTWO);
        helper.assertTrue(VariantGedragen.voerFactor(mewtwo) == 2 && VariantGedragen.voerFactor(gewoon) == 1, "voerFactor 2");
        Map<Integer, Integer> voeren = new ConcurrentHashMap<>();
        Band.opHartjes((g, eigenaar, erbij, reden) -> {
            if (reden == Reden.VOEREN && (g == gewoon || g == mewtwo)) {
                voeren.merge(g.getId(), erbij, Integer::sum);
            }
        });
        BandEvents.voer(gewoon, p, new ItemStack(ModItems.GUH_CUPCAKE.get()));
        BandEvents.voer(mewtwo, p, new ItemStack(ModItems.GUH_CUPCAKE.get()));
        int h1 = voeren.getOrDefault(gewoon.getId(), 0), h2 = voeren.getOrDefault(mewtwo.getId(), 0);
        helper.assertTrue(h1 == Reden.VOEREN.standaard() && h2 == 2 * h1, "one snack: x2 hearts (" + h1 + " / " + h2 + ")");
        int voor = Band.hartjes(mewtwo);
        for (int i = 0; i < 60; i++) {
            BandEvents.voer(gewoon, p, new ItemStack(ModItems.GUH_CUPCAKE.get()));
            BandEvents.voer(mewtwo, p, new ItemStack(ModItems.GUH_CUPCAKE.get()));
            int nu = Band.hartjes(mewtwo);
            helper.assertTrue(nu >= voor, "hearts never go down");
            voor = nu;
        }
        h1 = voeren.getOrDefault(gewoon.getId(), 0);
        h2 = voeren.getOrDefault(mewtwo.getId(), 0);
        helper.assertTrue(h1 == Reden.VOEREN.dagMax() && h2 == 2 * Reden.VOEREN.dagMax(), "capped: twice the normal day cap (" + h1 + " / " + h2 + ")");
        gewoon.discard();
        mewtwo.discard();
        weg(helper, p);
        helper.succeed();
    }

    /** Mieuwguh floats round the island only for players who reached the meal of the story; she leaves when nobody like that is near. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 200)
    public static void mewtwoMewAlleenNaHetVerhaal(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(10, 2, 10));
        BlockPos midden = helper.absolutePos(new BlockPos(10, 6, 10));
        mewsWeg(helper);
        MewSpawner.TEST_EILANDEN.add(midden);
        try {
            helper.assertTrue(MewSpawner.eiland(helper.getLevel(), p.blockPosition()) != null, "p is on the (test) island");
            MewtwoVoortgang.zetStap(p, MewtwoVoortgang.NOTITIES);
            helper.assertTrue(!MewSpawner.magMew(p) && MewSpawner.kijk(p) == null, "no Mieuwguh while you look for the notes");
            MewtwoVoortgang.zetStap(p, MewtwoVoortgang.ONDERDELEN);
            helper.assertTrue(MewSpawner.kijk(p) == null, "nor while the tank is broken");
            MewtwoVoortgang.zetStap(p, MewtwoVoortgang.KLAAR);
            MewEntity mew = MewSpawner.kijk(p);
            helper.assertTrue(mew != null && mew.isWild() && mew.thuis().equals(midden), "after the story: Mieuwguh near the island");
            helper.assertTrue(MewSpawner.kijk(p) == null, "just one");
            helper.assertTrue(MewSpawner.welkom(mew), "she stays while you're there");
            helper.assertTrue(!mew.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(p), 5f) && mew.isAlive(), "never hurt");
            helper.assertTrue(GuhDex.isCreaturePage(GuhVariant.MEW), "her Guhdex page");
            MewtwoVoortgang.zetStap(p, MewtwoVoortgang.NOTITIES);
            helper.assertTrue(!MewSpawner.welkom(mew), "nobody who knows her: she floats off");
            mew.discard();
        } finally {
            MewSpawner.TEST_EILANDEN.remove(midden);
        }
        weg(helper, p);
        helper.succeed();
    }

    /** The story copy: sulky before the meal, never tamed by knabbels; the variant's behaviour and outfits are registered. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void mewtwoKopieEnGedrag(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(10, 2, 10));
        GuhEntity kopie = VerhaalGuhs.maakKopie(helper.getLevel(), VerhaalGuh.MEWTWO, helper.absolutePos(new BlockPos(5, 1, 5)));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
        for (int i = 0; i < 12; i++) {
            kopie.mobInteract(p, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(!kopie.isTame() && p.getMainHandItem().getCount() == 16 && Praat.lopend(p) == null, "sulky, never tamed, no question yet");
        helper.assertTrue(VariantGedragen.van(GuhVariant.MEWTWO) instanceof MewtwoGedrag g && "gui.guhs.mewtwo.telekinese".equals(g.speciaalKnop()),
                "the Guhtwo's behaviour and its guh-menu button");
        for (GuhClothes c : List.of(GuhClothes.MEWTWO_TRAINERPETJE, GuhClothes.MEWTWO_TRAINERPAKJE, GuhClothes.MEWTWO_STAARTJE, GuhClothes.MEW_BALLONNETJE)) {
            helper.assertTrue(MewtwoFeature.BRON.equals(KledingBronnen.bron(c)), "outfit source of " + c);
        }
        helper.assertTrue(GuhDex.ENTRIES.contains(GuhVariant.MEW) && GuhDex.ENTRIES.contains(GuhVariant.KNABBELKLOON) && GuhDex.TAMEABLE.contains(GuhVariant.MEWTWO),
                "Guhdex pages: Mieuwguh, the professor, the Guhtwo (tameable)");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(nl.juiced.guhs.feature.knus.GuhHooks.heeft(kopie, nl.juiced.guhs.feature.verhaal.VerhaalVlaggen.ZWEEFT), "it floats (the flag)");
            kopie.discard();
            weg(helper, p);
            helper.succeed();
        });
    }

    /** The kloon_eiland template: 6 note spots (1..6), 4 parts crates (1..4), the tank (+26 glass parts), the bowl, 5-8 shuckle plekjes. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void mewtwoEilandTemplate(GameTestHelper helper) {
        StructureTemplate t = helper.getLevel().getStructureManager().get(Guhs.id("kloon_eiland")).orElse(null);
        helper.assertTrue(t != null && t.getSize().getX() == 80 && t.getSize().getZ() == 80, "the template exists (80 x 80)");
        StructurePlaceSettings s = new StructurePlaceSettings();
        java.util.Set<Integer> notities = new java.util.HashSet<>(), kisten = new java.util.HashSet<>();
        var n = t.filterBlocks(BlockPos.ZERO, s, MewtwoFeature.NOTITIEPLEK.get());
        n.forEach(i -> notities.add(i.state().getValue(MewtwoBlokken.NUMMER)));
        var k = t.filterBlocks(BlockPos.ZERO, s, MewtwoFeature.ONDERDELENKIST.get());
        k.forEach(i -> kisten.add(i.state().getValue(MewtwoBlokken.SOORT)));
        helper.assertTrue(n.size() == 6 && notities.size() == 6 && k.size() == 4 && kisten.size() == 4, "notes " + notities + ", crates " + kisten);
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, s, MewtwoFeature.KLOONTANK.get()).size() == 1
                && t.filterBlocks(BlockPos.ZERO, s, MewtwoFeature.TANKWAND.get()).size() == 26, "the tank and its glass");
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, s, MewtwoFeature.KNABBELSCHAAL.get()).size() == 1, "the knabbelschaal");
        int plekjes = t.filterBlocks(BlockPos.ZERO, s, nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature.SHUCKLE_PLEKJE.get()).size();
        helper.assertTrue(plekjes >= 5 && plekjes <= 8, "shuckle plekjes on the coast: " + plekjes);
        helper.succeed();
    }
}
