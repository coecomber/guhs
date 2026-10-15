package nl.juiced.guhs.feature.knuffeldal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.evenementen.Evenement;
import nl.juiced.guhs.feature.evenementen.Evenementen;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.feature.knus.Seizoen;
import nl.juiced.guhs.feature.onderwater.GuhbubbelStructure;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Game tests of the Knuffeldal (2.8): Cocotje's dialogue (the right answer and the others), the Grote Knusfeest (the
 * Burgemeester's round, the six tasks, the feast with its rewards, then the seasonal feasts), the Kruimel-Mika (steals,
 * never hurts or gets hurt, lured away with a treat), guhs coming to the feestbuffet, the Pluisguh, the residents, the
 * seasonal activities, the town's templates and protection, the blocks, and the Knuffeldal's share of the Guhmension
 * with one town per dal (worked out from the dimension's own biome source and noise, so it runs on the GameTest server).
 */
public class KnuffeldalGameTests {
    private static final String EMPTY = "empty";
    /** (its floor is at relative y 1: a test's structure sits one block above its structure block) */
    private static final String PLEIN = "knuffeldal_test_plein";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("guhs");

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
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

    private static GuhNpcEntity npc(GameTestHelper helper, BlockPos at, GuhNpcEntity.Kind kind) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), at);
        npc.setKind(kind);
        return npc;
    }

    private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id(name.contains("/") ? name : "quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    // =================================================================================================================
    // Cocotje
    // =================================================================================================================

    @GuhTest(template = PLEIN)
    public static void knuffeldalCocotjeGoedAntwoord(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 7));
        try {
            // the exact words (the user's own) in nl_nl; 1.2.0: en_us has a real English translation of them
            Map<String, String> woorden = Map.of("quest.guhs.cocotje.vraag", "WEET JIJ WAAR ZE ZIJN??????",
                    "quest.guhs.cocotje.optie.1", "Ik ga gelijk zoeken!", "quest.guhs.cocotje.optie.2", "Ik zie ze aan je hangen Cocotje",
                    "quest.guhs.cocotje.optie.3", "Wie is ze?", "quest.guhs.cocotje.optie.4", "omda je vahoeg beh",
                    "quest.guhs.cocotje.antwoord.1", "Njeg succes. Ik vads het nog wel als je iets weet",
                    "quest.guhs.cocotje.antwoord.2", "OHJA ZE HANGEN AAN ME VEH", "quest.guhs.cocotje.antwoord.3", "hmmm da wik dus ook ekkes nie eigi...",
                    "quest.guhs.cocotje.antwoord.4", "njeg.");
            for (var e : woorden.entrySet()) {
                String got = nl.juiced.guhs.taal.NlTekst.get(e.getKey());
                helper.assertTrue(e.getValue().equals(got), e.getKey() + " = '" + got + "'");
                String en = Component.translatable(e.getKey()).getString();
                helper.assertTrue(!en.isBlank() && !en.equals(e.getKey()), e.getKey() + " has no English");
            }
            GuhNpcEntity cocotje = npc(helper, new BlockPos(7, 2, 9), GuhNpcEntity.Kind.COCOTJE);
            var role = Features.role(GuhNpcEntity.Kind.COCOTJE);
            helper.assertTrue(role instanceof Cocotje, "Cocotje's role");
            role.talk(cocotje, p);
            helper.assertTrue(Cocotje.state(p) == Cocotje.GEVRAAGD, "she asks WEET JIJ WAAR ZE ZIJN");
            Cocotje.kies(cocotje, p, Cocotje.ZOEKEN);
            helper.assertTrue(Cocotje.state(p) == Cocotje.NIETS, "'Ik ga gelijk zoeken!': next time she asks again");
            Cocotje.kies(cocotje, p, Cocotje.HANGEN);
            helper.assertTrue(Cocotje.state(p) == Cocotje.NIETS, "no answer when she didn't ask");
            role.talk(cocotje, p);
            Cocotje.kies(cocotje, p, Cocotje.WIE);
            helper.assertTrue(Cocotje.state(p) == Cocotje.NIETS, "'Wie is ze?': asked again next time");
            role.talk(cocotje, p);
            Cocotje.kies(cocotje, p, Cocotje.HANGEN);
            helper.assertTrue(Cocotje.state(p) == Cocotje.OHJA, "'Ik zie ze aan je hangen Cocotje': OHJA ZE HANGEN AAN ME VEH");
            Cocotje.kies(cocotje, p, Cocotje.ZOEKEN);
            helper.assertTrue(Cocotje.state(p) == Cocotje.OHJA, "now only 'omda je vahoeg beh' fits");
            helper.assertTrue(count(p, ModItems.KAAS_KNABBELS.get()) == 0, "no knabbel yet");
            Cocotje.kies(cocotje, p, Cocotje.VAHOEG);
            helper.assertTrue(Cocotje.state(p) == Cocotje.KLAAR, "njeg.");
            helper.assertTrue(count(p, ModItems.KAAS_KNABBELS.get()) == 1, "one kaasknabbel");
            helper.assertTrue(KnusVoortgang.teller(p, KnuffeldalVoortgang.COCOTJE) == 1 && KnusVoortgang.heeft(p, KnuffeldalVoortgang.VRIENDJES_BOEK, "cocotje")
                    && advancement(p, "knuffeldal_cocotje"), "Cocotje in the Knus tab");
            role.talk(cocotje, p);
            Cocotje.kies(cocotje, p, Cocotje.VAHOEG);
            helper.assertTrue(Cocotje.state(p) == Cocotje.KLAAR && count(p, ModItems.KAAS_KNABBELS.get()) == 1, "only once; now she just says something cute");
            helper.assertTrue(GuhVariant.ofCharacter(GuhNpcEntity.Kind.COCOTJE) == GuhVariant.COCOTJE, "her Guhdex page");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the Grote Knusfeest
    // =================================================================================================================

    @GuhTest(template = PLEIN, timeoutTicks = 200, batch = "knuffeldal_feest")
    public static void knuffeldalGroteKnusfeestEnSeizoensfeest(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 6));
        var grens = helper.getBounds();
        Feestbuffet.TEST_GRENZEN.add(grens);   // (only this test's two tables, not the neighbours')
        try {
            Knusfeest.vergeet(p);
            GuhNpcEntity burgemeester = npc(helper, new BlockPos(7, 2, 8), GuhNpcEntity.Kind.BURGEMEESTERGUH);
            var role = Features.role(GuhNpcEntity.Kind.BURGEMEESTERGUH);
            role.talk(burgemeester, p);
            helper.assertTrue(Knusfeest.rondeBezig(p) && Knusfeest.ronde(p) == 0 && Knusfeest.taken(p).size() == 6, "the Grote Knusfeest: six tasks");
            helper.assertTrue(count(p, KnuffeldalFeature.KNUSFEESTLIJSTJE.get()) == 1, "the knusfeestlijstje");
            helper.assertTrue(advancement(p, "knuffeldal_burgemeester") && KnusVoortgang.teller(p, KnuffeldalVoortgang.KNUSFEEST) == 1, "met the Burgemeester");
            for (Feesttaak t : Feesttaak.values()) {
                Knusfeest.gemaakt(p, t);                        // (what the features do when their feest-activity is done)
                Burgemeester.gebracht(p, t);                    // (the item itself is the other slices' business: tag-less here)
            }
            helper.assertTrue(Knusfeest.alleGebracht(p) && KnusVoortgang.teller(p, KnuffeldalVoortgang.TAAKJES) == 6, "all six brought");
            role.talk(burgemeester, p);
            Evenement feest = Evenementen.eventOf(p);
            helper.assertTrue(feest instanceof KnusfeestEvenement, "the feast begins: " + feest);
            KnusfeestEvenement knusfeest = (KnusfeestEvenement) feest;
            helper.assertTrue(knusfeest.tafels().size() == 2, "the feast is at the feestbuffet: " + knusfeest.tafels());
            for (BlockPos t : knusfeest.tafels()) {
                helper.assertTrue(helper.getLevel().getBlockState(t).getValue(KnuffeldalBlocks.Feestbuffettafel.GEDEKT), "the tables are laid");
            }
            ItemStack knabbels = new ItemStack(ModItems.KAAS_KNABBELS.get(), 3);
            helper.assertTrue(Feestbuffet.opTafel(p, helper.getLevel(), knusfeest.tafels().get(0), knabbels) && knabbels.getCount() == 2,
                    "kaasknabbels go on the buffet");
            feest.end(true);
            for (BlockPos t : knusfeest.tafels()) {
                helper.assertTrue(!helper.getLevel().getBlockState(t).getValue(KnuffeldalBlocks.Feestbuffettafel.GEDEKT), "cleared again");
            }
            helper.assertTrue(Knusfeest.isKlaar(p) && !Knusfeest.rondeBezig(p), "the Grote Knusfeest is done");
            helper.assertTrue(count(p, KnuffeldalFeature.KNUS_OORKONDE.get().asItem()) == 1
                    && count(p, ModItems.clothingItem(GuhClothes.BURGEMEESTERSSJERP)) == 1, "the knus_oorkonde and the burgemeesterssjerp");
            helper.assertTrue(Burgemeester.isKnuffelburgemeester(p) && advancement(p, "knuffeldal_finale")
                    && advancement(p, "knuffeldal/knuffelburgemeester"), "Knuffelburgemeester!");
            // the same season: no new round
            role.talk(burgemeester, p);
            helper.assertTrue(!Knusfeest.rondeBezig(p), "the seasonal feasts start next season");
            // next season: 2-3 tasks again
            Seizoen.zet(helper.getLevel().getServer(), Seizoen.huidig(helper.getLevel()).volgende());
            role.talk(burgemeester, p);
            int n = Knusfeest.taken(p).size();
            helper.assertTrue(Knusfeest.rondeBezig(p) && n >= 2 && n <= 3 && Knusfeest.ronde(p) == Seizoen.nummer(helper.getLevel()) + 1,
                    "a seasonal round: " + Knusfeest.taken(p));
            for (Feesttaak t : Knusfeest.taken(p)) {
                Knusfeest.gemaakt(p, t);
                Burgemeester.gebracht(p, t);
            }
            Feestbuffet.gevierd(p, false);
            helper.assertTrue(!Knusfeest.rondeBezig(p) && KnusVoortgang.teller(p, KnuffeldalVoortgang.SEIZOENSFEESTEN) == 1, "the seasonal feast");
            role.talk(burgemeester, p);
            helper.assertTrue(!Knusfeest.rondeBezig(p), "once per season");
        } finally {
            Feestbuffet.TEST_GRENZEN.remove(grens);
            Knusfeest.vergeet(p);
            GuhQuests.saved(p).remove(Feestbuffet.TITEL);
            leave(helper, p);
        }
        helper.succeed();
    }

    /** During a feast every participant's tamed guhs come to the buffet (from far away with a poof) and eat there. */
    // (1.1.0: own batch - 26.1 runs tests in id order; its neighbours in the shared batch disturbed it)
    @GuhTest(template = PLEIN, timeoutTicks = 400, batch = "knuffeldal_buffet")
    public static void knuffeldalGuhsKomenNaarHetBuffet(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 7));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(13, 2, 13));
        guh.setPersistenceRequired();
        guh.tame(p);
        var grens = helper.getBounds();
        Feestbuffet.TEST_GRENZEN.add(grens);   // (only this test's tables, not the neighbours')
        KnusfeestEvenement feest = KnusfeestEvenement.maak(helper.getLevel(), p);
        Feestbuffet.TEST_GRENZEN.remove(grens);
        feest.autoJoin = false;
        Evenementen.begin(feest, p);
        helper.succeedWhen(() -> {
            BlockPos t = feest.tafels().get(0);
            boolean near = feest.tafels().stream().anyMatch(tb -> guh.position().distanceTo(Vec3.atBottomCenterOf(tb)) < 3.2);
            helper.assertTrue(near && GuhHooks.isBezig(guh), "the guh comes to the buffet: " + guh.position() + " tables " + t);
            feest.end(false);
            helper.assertTrue(!helper.getLevel().getBlockState(t).getValue(KnuffeldalBlocks.Feestbuffettafel.GEDEKT), "cleared");
            leave(helper, p);
        });
    }

    // =================================================================================================================
    // the Kruimel-Mika
    // =================================================================================================================

    @GuhTest(template = PLEIN, timeoutTicks = 300)
    public static void knuffeldalKruimelMikaSteeltEnWordtWeggelokt(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 7));
        Knusfeest.vergeet(p);
        Knusfeest.nieuweRonde(p, 0, EnumSet.of(Feesttaak.FEESTTAART));
        Knusfeest.gemaakt(p, Feesttaak.FEESTTAART);
        KruimelMikaEntity mika = KruimelMikaEntity.steel(p, Feesttaak.FEESTTAART, new ItemStack(Items.CAKE));
        helper.assertTrue(mika != null && mika.buit().is(Items.CAKE), "it has the (stand-in) feesttaart");
        helper.assertTrue(Knusfeest.stap(p, Feesttaak.FEESTTAART) == Knusfeest.Stap.GESTOLEN && Knusfeest.open(p, Feesttaak.FEESTTAART),
                "stolen, still open");
        float health = mika.getHealth();
        boolean hurt = mika.hurtOrSimulate(p.damageSources().playerAttack(p), 6f);
        helper.assertTrue(!hurt && mika.getHealth() == health && p.getHealth() == p.getMaxHealth(), "no fighting: nobody gets hurt");
        helper.assertTrue(KruimelMikaEntity.lekkernij(p) == false, "no treat in hand");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 2));
        helper.assertTrue(KruimelMikaEntity.lekkernij(p), "kaasknabbels are a treat");
        mika.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(count(p, Items.CAKE) == 1 && count(p, ModItems.KAAS_KNABBELS.get()) == 1, "the treat for the cake");
        helper.assertTrue(Knusfeest.stap(p, Feesttaak.FEESTTAART) == Knusfeest.Stap.TERUGGEVONDEN, "found back");
        helper.assertTrue(KnusVoortgang.teller(p, KnuffeldalVoortgang.KRUIMEL_MIKAS) == 1 && advancement(p, "knuffeldal_kruimel_mika")
                && advancement(p, "knuffeldal/kruimel_mika_verjaagd"), "it counts");
        helper.assertTrue(mika.toestand() == KruimelMikaEntity.Toestand.WEG, "it runs off giggling");
        helper.succeedWhen(() -> {
            helper.assertTrue(mika.isRemoved(), "and is gone (poof)");
            Knusfeest.vergeet(p);
            leave(helper, p);
        });
    }

    /** A stolen item that stays gone too long: the Burgemeester found it back himself (nobody gets stuck). */
    @GuhTest(template = PLEIN)
    public static void knuffeldalGestolenKomtAltijdTerug(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 7));
        try {
            Knusfeest.vergeet(p);
            Knusfeest.nieuweRonde(p, 0, EnumSet.of(Feesttaak.THEESERVIES));
            Knusfeest.zet(p, Feesttaak.THEESERVIES, Knusfeest.Stap.GESTOLEN);
            helper.assertTrue(Burgemeester.lever(p) == 0, "just stolen: not yet");
            GuhQuests.saved(p).getCompoundOrEmpty(Knusfeest.KEY).getCompoundOrEmpty("GestolenOp")
                    .putLong(Feesttaak.THEESERVIES.id(), helper.getLevel().getGameTime() - KruimelMikaEntity.TERUG_NA - 1);
            helper.assertTrue(Burgemeester.lever(p) == 1 && Knusfeest.gebracht(p, Feesttaak.THEESERVIES), "after a while it counts as brought");
        } finally {
            Knusfeest.vergeet(p);
            leave(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the Pluisguh and the residents
    // =================================================================================================================

    @GuhTest(template = EMPTY)
    public static void knuffeldalPluisguhAlleenInHetDal(GameTestHelper helper) {
        int pluis = 0, n = 400;
        for (int i = 0; i < n; i++) {
            GuhEntity guh = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            if (KnuffeldalEvents.decide(guh, true)) {
                pluis++;
                helper.assertTrue(guh.getVariant() == GuhVariant.PLUISGUH, "a Pluisguh");
            }
            helper.assertTrue(!KnuffeldalEvents.decide(guh, true), "decided only once");
        }
        // (1.3.1 lowered the chance from 35% to 23%, KnuffeldalEvents.PLUISGUH_CHANCE: 92 of 400 on average, 8 either way)
        helper.assertTrue(pluis > n * 0.14 && pluis < n * 0.32, "about 23%: " + pluis + " of " + n);
        GuhEntity outside = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(!KnuffeldalEvents.decide(outside, false) && outside.getVariant() == GuhVariant.NORMAL, "never outside the Knuffeldal");
        GuhEntity bewoner = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        GuhHooks.maakBewoner(bewoner, BlockPos.ZERO);
        helper.assertTrue(!KnuffeldalEvents.decide(bewoner, true), "residents keep their looks");
        helper.assertTrue(!GuhVariant.PLUISGUH.isCharacter() && nl.juiced.guhs.quest.GuhDex.TAMEABLE.contains(GuhVariant.PLUISGUH),
                "a real variant you can tame");
        helper.succeed();
    }

    @GuhTest(template = PLEIN, timeoutTicks = 400)
    public static void knuffeldalBewonersBlijvenThuisEnWillenNietMee(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 7));
        BlockPos home = helper.absolutePos(new BlockPos(3, 2, 3));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 2, 3));
        GuhHooks.maakBewoner(guh, home);
        guh.getPersistentData().putString(GuhHooks.BEWONER_NAAM, "pluisje");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
        for (int i = 0; i < 5; i++) {
            guh.mobInteract(p, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(!guh.isTame() && count(p, ModItems.KAAS_KNABBELS.get()) == 0, "it eats the knabbels but never goes with you: it lives here");
        helper.assertTrue(KnusVoortgang.heeft(p, KnuffeldalVoortgang.VRIENDJES_BOEK, "pluisje")
                && KnusVoortgang.teller(p, KnuffeldalVoortgang.VRIENDJES) == 1, "a new friend in the Knus tab");
        // (the test player doesn't move: out of the way, or the guh bumps into it on the straight path home)
        BlockPos aside = helper.absolutePos(new BlockPos(12, 2, 2));
        p.teleportTo(aside.getX() + 0.5, aside.getY(), aside.getZ() + 0.5);
        BlockPos far = helper.absolutePos(new BlockPos(13, 2, 13));
        guh.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
        helper.succeedWhen(() -> {
            helper.assertTrue(guh.blockPosition().distSqr(home) < 36, "it walks back home: " + guh.blockPosition() + " home " + home);
            leave(helper, p);
        });
    }

    // =================================================================================================================
    // the seasons
    // =================================================================================================================

    @GuhTest(template = PLEIN, batch = "knuffeldal_seizoen")
    public static void knuffeldalSeizoensactiviteiten(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ServerPlayer p = player(helper, new BlockPos(7, 2, 7));
        try {
            BlockPos bak = helper.absolutePos(new BlockPos(2, 2, 12));
            // spring: three flowers make a bloesemkransje
            Seizoen.zet(server, Seizoen.LENTE);
            ItemStack flowers = new ItemStack(Items.POPPY, 2);
            helper.assertTrue(Seizoensactiviteiten.bloembak(p, helper.getLevel(), bak, flowers) && flowers.getCount() == 2, "two is not enough");
            flowers = new ItemStack(Items.POPPY, 3);
            helper.assertTrue(Seizoensactiviteiten.bloembak(p, helper.getLevel(), bak, flowers) && flowers.isEmpty()
                    && count(p, ModItems.clothingItem(GuhClothes.BLOESEMKRANSJE)) == 1, "a bloesemkransje");
            helper.assertTrue(!Seizoensactiviteiten.bloembak(p, helper.getLevel(), bak, new ItemStack(Items.WHEAT, 3)), "wheat is for the summer");
            // summer: wheat makes a zonnehoedje
            Seizoen.zet(server, Seizoen.ZOMER);
            helper.assertTrue(Seizoensactiviteiten.bloembak(p, helper.getLevel(), bak, new ItemStack(Items.WHEAT, 3))
                    && count(p, ModItems.clothingItem(GuhClothes.ZONNEHOEDJE)) == 1, "a zonnehoedje");
            // the flower box and the leaf pile follow the season (random ticks)
            BlockState box = helper.getBlockState(new BlockPos(2, 2, 12));
            box.randomTick(helper.getLevel(), bak, helper.getLevel().getRandom());
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 12)).getValue(KnuffeldalBlocks.SEIZOEN) == Seizoen.ZOMER, "the flower box shows summer");
            // autumn: the leaf piles fill up; jumping in counts; leaves on the box make leaf piles
            Seizoen.zet(server, Seizoen.HERFST);
            BlockPos hoop = helper.absolutePos(new BlockPos(12, 2, 12));
            helper.getBlockState(new BlockPos(12, 2, 12)).randomTick(helper.getLevel(), hoop, helper.getLevel().getRandom());
            helper.assertTrue(helper.getBlockState(new BlockPos(12, 2, 12)).getValue(KnuffeldalBlocks.Bladerhoopje.VOL), "a big leaf pile in autumn");
            GuhEntity mijnGuh = helper.spawn(ModEntities.GUH.get(), new BlockPos(11, 2, 11));
            mijnGuh.tame(p);
            Seizoensactiviteiten.gesprongen(helper.getLevel(), hoop, p);
            helper.assertTrue(KnusVoortgang.teller(p, KnuffeldalVoortgang.BLADERHOOPJES) == 1
                    && KnusVoortgang.heeft(p, KnuffeldalVoortgang.PLAKBOEK, "herfst_hoopje")
                    && KnusVoortgang.heeft(p, KnuffeldalVoortgang.PLAKBOEK, "herfst_guh"), "jumped in, with your guh");
            helper.assertTrue(Seizoensactiviteiten.bloembak(p, helper.getLevel(), bak, new ItemStack(Items.OAK_LEAVES, 4))
                    && count(p, KnuffeldalFeature.BLADERHOOPJE.get().asItem()) == 2, "leaves make leaf piles");
            // winter: wool makes a sjaaltje, snowballs a sneeuwguhkopje; a snow guh on two snow blocks
            Seizoen.zet(server, Seizoen.WINTER);
            helper.assertTrue(Seizoensactiviteiten.bloembak(p, helper.getLevel(), bak, new ItemStack(Items.WHITE_WOOL, 3))
                    && count(p, ModItems.clothingItem(GuhClothes.KNUS_SJAALTJE)) == 1, "a knus sjaaltje");
            ItemStack balls = new ItemStack(Items.SNOWBALL, 3);
            helper.assertTrue(Seizoensactiviteiten.bloembak(p, helper.getLevel(), bak, balls) && count(p, KnuffeldalFeature.SNEEUWGUHKOPJE.get()) == 1,
                    "a sneeuwguhkopje");
            helper.setBlock(new BlockPos(5, 2, 5), Blocks.SNOW_BLOCK);
            helper.setBlock(new BlockPos(5, 3, 5), Blocks.SNOW_BLOCK);
            BlockPos top = helper.absolutePos(new BlockPos(5, 3, 5));
            ItemStack kopje = new ItemStack(KnuffeldalFeature.SNEEUWGUHKOPJE.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, kopje);
            kopje.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(top).add(0, 0.5, 0), Direction.UP, top, false)));
            helper.assertTrue(helper.getBlockState(new BlockPos(5, 2, 5)).is(KnuffeldalFeature.SNEEUWPOPGUH.get())
                    && helper.getBlockState(new BlockPos(5, 3, 5)).is(KnuffeldalFeature.SNEEUWPOPGUH.get()), "a sneeuwpopguh stands there");
            helper.assertTrue(KnusVoortgang.heeft(p, KnuffeldalVoortgang.PLAKBOEK, "winter_sneeuwpop"), "in the plakboek");
            // your tamed guh wearing the sjaaltje in winter
            mijnGuh.wear(GuhClothes.KNUS_SJAALTJE);
            Seizoensactiviteiten.guhTick(mijnGuh);
            helper.assertTrue(KnusVoortgang.heeft(p, KnuffeldalVoortgang.PLAKBOEK, "winter_guh"), "a guh with a sjaaltje");
            helper.assertTrue(KnusVoortgang.teller(p, "seizoenen.winter") == 2 && KnusVoortgang.teller(p, "seizoenen.herfst") == 2
                    && KnusVoortgang.teller(p, KnuffeldalVoortgang.SEIZOEN_ALLE) == 2, "two seasons complete");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the town: templates and protection; the blocks
    // =================================================================================================================

    @GuhTest(template = EMPTY)
    public static void knuffeldalStadjeEnBescherming(GameTestHelper helper) {
        var templates = helper.getLevel().getStructureManager();
        var plein = templates.get(Guhs.id("knuffeldal_stadje/plein"));
        helper.assertTrue(plein.isPresent() && plein.get().getSize().equals(new net.minecraft.core.Vec3i(49, 24, 49)), "the plein: 49 x 24 x 49");
        var jigsaws = plein.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                Blocks.JIGSAW, true);
        Map<String, BlockPos> named = new HashMap<>();
        for (var j : jigsaws) {
            named.put(j.nbt().getStringOr("name", "") + (j.nbt().getStringOr("name", "").equals("guhs:plein_hoek") ? j.pos().toShortString() : ""), j.pos());
        }
        helper.assertTrue(new BlockPos(24, 4, 24).equals(named.get("guhs:knuffeldal_midden")), "the anchor in the middle: " + named);
        helper.assertTrue(new BlockPos(24, 4, 0).equals(named.get("guhs:plein_bakkerij")) && new BlockPos(48, 4, 24).equals(named.get("guhs:plein_theehuis"))
                && new BlockPos(24, 4, 48).equals(named.get("guhs:plein_kapper")) && new BlockPos(0, 4, 24).equals(named.get("guhs:plein_creche"))
                && new BlockPos(38, 4, 10).equals(named.get("guhs:plein_grijpmachine")), "the five slots");
        helper.assertTrue(jigsaws.size() == 10, "ten jigsaws: " + jigsaws.size());
        for (String hoek : List.of("noordoost", "noordwest", "zuidoost", "zuidwest")) {
            var t = templates.get(Guhs.id("knuffeldal_stadje/hoek_" + hoek));
            helper.assertTrue(t.isPresent() && t.get().getSize().equals(new net.minecraft.core.Vec3i(40, 30, 31)), "corner " + hoek);
            // the town's one free street jigsaw (for later versions) sits at the east end of hoek_noordoost's street
            var vrij = t.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                    Blocks.JIGSAW, true).stream().filter(j -> j.nbt().getStringOr("name", "").equals("guhs:knuffeldal_straat_vrij")).toList();
            if (hoek.equals("noordoost")) {
                helper.assertTrue(vrij.size() == 1 && vrij.get(0).pos().equals(new BlockPos(39, 4, 23))
                        && vrij.get(0).nbt().getStringOr("pool", "").equals("guhs:knuffeldal_stadje/vrij")
                        && vrij.get(0).state().getValue(net.minecraft.world.level.block.JigsawBlock.ORIENTATION)
                        == net.minecraft.core.FrontAndTop.EAST_UP, "the free street jigsaw: " + vrij);
            } else {
                helper.assertTrue(vrij.isEmpty(), "only one free street jigsaw, not in " + hoek);
            }
        }
        // its pool exists and holds the 2.9 street piece (the Beroepenstraat, feature.beroepen)
        var vrijPool = helper.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL).getValue(Guhs.id("knuffeldal_stadje/vrij"));
        helper.assertTrue(vrijPool != null && vrijPool.size() == 1
                && vrijPool.getShuffledTemplates(net.minecraft.util.RandomSource.create(1)).get(0).toString().contains("beroepenstraat"),
                "the free street pool holds the Beroepenstraat");
        // a campfire within 20 blocks of the anchor (Opa Guh's, and the evening campfire of the day rhythm)
        var fires = plein.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                Blocks.CAMPFIRE, true);
        helper.assertTrue(fires.stream().anyMatch(f -> f.pos().distSqr(new BlockPos(24, 4, 24)) <= 400), "a campfire near the anchor");
        // protection: no breaking or building for survival players, creative may
        BlockPos stone = new BlockPos(2, 1, 2);
        helper.setBlock(stone, Blocks.STONE);
        BlockPos abs = helper.absolutePos(stone);
        PleinSlot.testStadje(new BoundingBox(abs.getX() - 2, abs.getY() - 1, abs.getZ() - 2, abs.getX() + 2, abs.getY() + 2, abs.getZ() + 2));
        ServerPlayer p = player(helper, new BlockPos(1, 1, 1));
        try {
            helper.assertTrue(KnuffeldalProtection.inStadje(helper.getLevel(), abs), "in the (test) town");
            helper.assertTrue(!p.gameMode.destroyBlock(abs) && helper.getBlockState(stone).is(Blocks.STONE), "survival: can't break the town");
            p.setGameMode(GameType.CREATIVE);
            helper.assertTrue(p.gameMode.destroyBlock(abs), "creative: may");
        } finally {
            PleinSlot.testWissen();
            leave(helper, p);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void knuffeldalBlokkenEnRecepten(GameTestHelper helper) {
        BlockPos face = new BlockPos(2, 1, 2);
        helper.setBlock(face, KnuffeldalFeature.KNUFFELSTEEN_GEZICHT.get().defaultBlockState().setValue(KnuffeldalBlocks.Gezicht.STEMMING, 0));
        ServerPlayer p = player(helper, new BlockPos(1, 1, 1));
        try {
            BlockPos abs = helper.absolutePos(face);
            helper.getBlockState(face).useWithoutItem(helper.getLevel(), p, new BlockHitResult(Vec3.atCenterOf(abs), Direction.NORTH, abs, false));
            helper.assertTrue(helper.getBlockState(face).getValue(KnuffeldalBlocks.Gezicht.STEMMING) == 1, "another face");
            var recipes = helper.getLevel().getServer().getRecipeManager();
            for (String r : List.of("knuffelsteen", "knuffelsteen_trap", "knuffelsteen_plaat", "knuffelsteen_muur", "knuffelsteen_gezicht", "pluisdak",
                    "pluisdak_trap", "pluisdak_plaat", "knuffelklinkers", "seizoensbloembak", "seizoensslinger", "feestbuffettafel", "sneeuwguhkopje",
                    "bladerhoopje")) {
                helper.assertTrue(recipes.byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, Guhs.id(r))).isPresent(), "a recipe for " + r);
            }
            helper.assertTrue(KnuffeldalFeature.KNUFFELGRAS.get().defaultBlockState().is(net.minecraft.tags.BlockTags.DIRT), "knuffelgras is dirt for plants");
            helper.assertTrue(KnuffeldalFeature.PLUIZENBOOM_BLADEREN.get().defaultBlockState().is(net.minecraft.tags.BlockTags.LEAVES), "leaves");
            var features = helper.getLevel().registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
            helper.assertTrue(features.containsKey(KnuffeldalFeature.PLUIZENBOOM.identifier()) && features.containsKey(KnuffeldalFeature.REUZE_GUHPADDENSTOEL.identifier()),
                    "the tree and the huge mushroom");
            helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).containsKey(KnuffeldalFeature.KNUFFELDAL), "the biome");
            var structure = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(KnuffeldalFeature.STADJE);
            helper.assertTrue(structure instanceof KnuffeldalStadjeStructure s && s.keepClear() > 60 && s.voorrang() == 800, "the town: " + structure);
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // worldgen: the Knuffeldal's share of the Guhmension, one town per dal
    // =================================================================================================================

    /**
     * Samples the Guhmension's surface biomes with and without the Knuffeldal (the dimension JSON and its noise settings,
     * three seeds): the Knuffeldal is small but findable, and every other surface biome keeps most of its share. Then the
     * dalen (connected Knuffeldal samples) and the town spots: never two towns in one dal, and most (big) dalen have one.
     */
    @GuhTest(template = EMPTY, timeoutTicks = 2400)
    public static void knuffeldalDeelEnEenStadjePerDal(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var access = server.registryAccess();
        JsonObject source;
        try (var reader = server.getResourceManager().getResource(Guhs.id("dimension/guhmension.json")).orElseThrow().openAsReader()) {
            source = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator").getAsJsonObject("biome_source");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        JsonObject without = source.deepCopy();
        JsonArray kept = new JsonArray();
        for (JsonElement e : without.getAsJsonArray("biomes")) {
            if (!e.getAsJsonObject().get("biome").getAsString().equals("guhs:knuffeldal")) {
                kept.add(e);
            }
        }
        without.add("biomes", kept);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        BiomeSource with = BiomeSource.CODEC.parse(ops, source).getOrThrow();
        BiomeSource before = BiomeSource.CODEC.parse(ops, without).getOrThrow();
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        KnuffeldalStadjeStructure town = (KnuffeldalStadjeStructure) access.lookupOrThrow(Registries.STRUCTURE).getValue(KnuffeldalFeature.STADJE);
        Map<String, Integer> now = new HashMap<>(), then = new HashMap<>();
        int samples = 0, dalen = 0, dalenMetStadje = 0, groteDalen = 0, groteMetStadje = 0, stadjes = 0, kruimels = 0, middel = 0;
        final int step = 32, half = 3200, n = 2 * half / step;
        StringBuilder report = new StringBuilder();
        for (long seed : new long[]{1L, 20280101L, -778899L}) {
            RandomState random = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed);
            Climate.Sampler sampler = random.sampler();
            boolean[][] dal = new boolean[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int x = -half + i * step, z = -half + j * step;
                    int qx = QuartPos.fromBlock(x), qy = QuartPos.fromBlock(100), qz = QuartPos.fromBlock(z);
                    String b = with.getNoiseBiome(qx, qy, qz, sampler).unwrapKey().orElseThrow().identifier().getPath();
                    now.merge(b, 1, Integer::sum);
                    then.merge(before.getNoiseBiome(qx, qy, qz, sampler).unwrapKey().orElseThrow().identifier().getPath(), 1, Integer::sum);
                    dal[i][j] = b.equals("knuffeldal");
                    samples++;
                }
            }
            // the towns of this area
            var noise = random.getOrCreateNoise(town.knuffelNoise());
            int size = town.cellChunks(), cells = half / (size * 16);
            List<GuhbubbelStructure.Peak> found = new ArrayList<>();
            for (int cx = -cells; cx < cells; cx++) {
                for (int cz = -cells; cz < cells; cz++) {
                    if (town.plek(seed, noise, cx, cz)) {
                        GuhbubbelStructure.Peak peak = GuhbubbelStructure.peak(seed, noise, size, cx, cz);
                        String b = with.getNoiseBiome(QuartPos.fromBlock(peak.x()), QuartPos.fromBlock(100), QuartPos.fromBlock(peak.z()), sampler)
                                .unwrapKey().orElseThrow().identifier().getPath();
                        if (b.equals("knuffeldal")) {
                            found.add(peak);
                        }
                    }
                }
            }
            stadjes += found.size();
            // the dalen: connected Knuffeldal samples; the towns in each
            int[][] label = new int[n][n];
            int labels = 0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (!dal[i][j] || label[i][j] != 0) {
                        continue;
                    }
                    labels++;
                    int area = 0;
                    ArrayDeque<int[]> todo = new ArrayDeque<>();
                    todo.add(new int[]{i, j});
                    label[i][j] = labels;
                    while (!todo.isEmpty()) {
                        int[] c = todo.poll();
                        area++;
                        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                            int a = c[0] + d[0], bb = c[1] + d[1];
                            if (a >= 0 && bb >= 0 && a < n && bb < n && dal[a][bb] && label[a][bb] == 0) {
                                label[a][bb] = labels;
                                todo.add(new int[]{a, bb});
                            }
                        }
                    }
                    int inIt = 0;
                    StringBuilder which = new StringBuilder();
                    for (GuhbubbelStructure.Peak peak : found) {
                        int a = Math.floorDiv(peak.x() + half, step), bb = Math.floorDiv(peak.z() + half, step);
                        boolean hit = false;
                        for (int da = -1; da <= 1 && !hit; da++) {
                            for (int db = -1; db <= 1 && !hit; db++) {
                                int aa = a + da, bbb = bb + db;
                                hit = aa >= 0 && bbb >= 0 && aa < n && bbb < n && label[aa][bbb] == labels;
                            }
                        }
                        if (hit) {
                            inIt++;
                            which.append(String.format(" (%d, %d: %.3f)", peak.x(), peak.z(), peak.value()));
                        }
                    }
                    helper.assertTrue(inIt <= 1, "never two towns in one dal (seed " + seed + ", " + inIt + " in a dal of " + area + " samples:" + which + ")");
                    dalen++;
                    if (area <= 3) {
                        kruimels++;
                    } else if (area * step * step < 30000) {
                        middel++;
                    }
                    if (inIt == 1) {
                        dalenMetStadje++;
                    }
                    if (area * step * step >= 30000) {
                        groteDalen++;
                        if (inIt == 1) {
                            groteMetStadje++;
                        }
                    }
                }
            }
            report.append(String.format("seed %d: %d towns; ", seed, found.size()));
        }
        for (String b : new java.util.TreeSet<>(then.keySet())) {
            report.append(String.format("%s %.1f%% -> %.1f%%; ", b, 100.0 * then.get(b) / samples, 100.0 * now.getOrDefault(b, 0) / samples));
        }
        double share = now.getOrDefault("knuffeldal", 0) / (double) samples;
        report.append(String.format("knuffeldal %.2f%%, dalen %d (with a town %d; crumbs <= 3 samples %d, small %d), big dalen %d (with a town %d), towns %d",
                100 * share, dalen, dalenMetStadje, kruimels, middel, groteDalen, groteMetStadje, stadjes));
        LOGGER.info("Knuffeldal: {}", report);
        helper.assertTrue(share >= 0.012 && share <= 0.06, "the Knuffeldal's share: " + report);
        for (var e : then.entrySet()) {
            double was = e.getValue() / (double) samples, is = now.getOrDefault(e.getKey(), 0) / (double) samples;
            if (was >= 0.01) {
                helper.assertTrue(is >= 0.75 * was, e.getKey() + " keeps most of its share: " + report);
            }
        }
        helper.assertTrue(stadjes >= 3, "towns: " + report);
        helper.assertTrue(groteDalen == 0 || groteMetStadje >= 0.6 * groteDalen, "most big dalen have their town: " + report);
        helper.succeed();
    }

    // =================================================================================================================
    // 1.2.7: for any number of players, forever
    // =================================================================================================================
    private static final String FIX = "knuffeldal_fix127";

    private static ItemStack item(String id) {
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Guhs.id(id)));
    }

    /**
     * A feest-item that got lost is asked again by the Burgemeester (so it can be made anew), the feesttaart is never
     * food for the buffet, the tea table or a Kruimel-Mika, and a feest-item that was ready before he asked still grants
     * its "made" quest advancement. Two players, each with their own list.
     */
    @GuhTest(template = PLEIN, batch = FIX)
    public static void knuffeldalKwijtFeestItemWordtOpnieuwGevraagd(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 7));
        ServerPlayer q = player(helper, new BlockPos(8, 2, 7));
        try {
            for (ServerPlayer s : List.of(p, q)) {
                Knusfeest.vergeet(s);
                Knusfeest.nieuweRonde(s, 0, EnumSet.of(Feesttaak.FEESTTAART, Feesttaak.FEESTBLOEMEN));
            }
            ItemStack taart = item("feesttaart");
            helper.assertTrue(!Feestbuffet.buffetEten(taart) && !nl.juiced.guhs.feature.theehuis.Theekransje.isGebak(taart)
                    && !KruimelMikaEntity.isLekkernij(taart), "the feesttaart is never eaten: not on the buffet, the tea table or by a Mika");
            helper.assertTrue(Feestbuffet.buffetEten(item("knabbelbroodje")) && KruimelMikaEntity.isLekkernij(item("knabbelbroodje"))
                    && KruimelMikaEntity.isLekkernij(new ItemStack(ModItems.KAAS_KNABBELS.get())), "the other treats still are");
            // p made the cake and lost it
            Knusfeest.gemaakt(p, Feesttaak.FEESTTAART);
            helper.assertTrue(Knusfeest.nodig(p, Feesttaak.FEESTTAART), "made but not in the pockets: needed again");
            p.getInventory().add(taart.copy());
            helper.assertTrue(!Knusfeest.nodig(p, Feesttaak.FEESTTAART), "in the pockets: not needed");
            p.getInventory().clearContent();
            helper.assertTrue(Burgemeester.lever(p) == 0 && Knusfeest.stap(p, Feesttaak.FEESTTAART) == Knusfeest.Stap.GEVRAAGD,
                    "the Burgemeester asks the lost cake again");
            // q is not touched by that, and hands in its own
            Knusfeest.gemaakt(q, Feesttaak.FEESTTAART);
            q.getInventory().add(taart.copy());
            helper.assertTrue(Burgemeester.lever(q) == 1 && Knusfeest.gebracht(q, Feesttaak.FEESTTAART) && !Knusfeest.gebracht(p, Feesttaak.FEESTTAART),
                    "q delivers its own cake");
            // p bakes a new one and delivers
            Knusfeest.gemaakt(p, Feesttaak.FEESTTAART);
            p.getInventory().add(taart.copy());
            helper.assertTrue(Burgemeester.lever(p) == 1 && Knusfeest.gebracht(p, Feesttaak.FEESTTAART), "p delivers the new cake");
            // a found-back item that is lost again: the same
            Knusfeest.zet(q, Feesttaak.FEESTBLOEMEN, Knusfeest.Stap.TERUGGEVONDEN);
            helper.assertTrue(Burgemeester.lever(q) == 0 && Knusfeest.stap(q, Feesttaak.FEESTBLOEMEN) == Knusfeest.Stap.GEVRAAGD, "asked again");
            // a feestboeket that was ready before (never "made" in this round): handed in, and the quest counts it as made
            helper.assertTrue(!advancement(q, "knusfeest_feestbloemen_gemaakt"), "not made yet");
            q.getInventory().add(item("feestboeket"));
            helper.assertTrue(Burgemeester.lever(q) == 1 && Knusfeest.alleGebracht(q), "the ready-made boeket is handed in");
            helper.assertTrue(advancement(q, "knusfeest_feestbloemen_gemaakt"), "and its 'made' quest is granted too");
        } finally {
            Knusfeest.vergeet(p);
            Knusfeest.vergeet(q);
            leave(helper, p, q);
        }
        helper.succeed();
    }

    /** A resident is never tamed (whatever the way, by whoever), can't be hurt or leashed; a tamed one is no resident any more. */
    @GuhTest(template = PLEIN, batch = FIX)
    public static void knuffeldalBewonersZijnVanIedereen(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 7));
        ServerPlayer q = player(helper, new BlockPos(8, 2, 7));
        try {
            GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 2, 3));
            GuhHooks.maakBewoner(guh, guh.blockPosition());
            guh.getPersistentData().putString(GuhHooks.BEWONER_NAAM, "pluisje");
            Bewoners.opJoin(guh);   // (what KnuffeldalEvents.onJoin does when it comes into the world)
            helper.assertTrue(guh.isInvulnerable() && !guh.canBeLeashed(), "a resident can't be hurt or leashed");
            helper.assertTrue(!guh.hurtServer(helper.getLevel(), q.damageSources().playerAttack(q), 50f) && guh.isAlive(), "a survival player can't hurt it");
            helper.assertTrue(!Evenementen.wild(guh), "the kaasregen and the golden knabbel leave it alone");
            for (ServerPlayer s : List.of(p, q)) {
                helper.assertTrue(!Evenementen.tameNow(guh, s) && !guh.isTame(), "taming is refused, for every player");
                helper.assertTrue(net.neoforged.neoforge.event.EventHooks.onAnimalTame(guh, s), "the tame event is cancelled");
            }
            Evenementen.makeHappy(guh, 200);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 30));
            for (int i = 0; i < 30; i++) {
                guh.mobInteract(p, InteractionHand.MAIN_HAND);
            }
            helper.assertTrue(!guh.isTame() && GuhHooks.isBewoner(guh), "thirty knabbels on a happy resident: still the town's");
            // a wild guh is still tameable and leashable
            GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 2, 3));
            helper.assertTrue(wild.canBeLeashed() && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(wild, p), "a wild guh is as before");
            // tamed before 1.2.7: it stays the player's guh, and is no resident any more
            GuhEntity mee = helper.spawn(ModEntities.GUH.get(), new BlockPos(7, 2, 3));
            GuhHooks.maakBewoner(mee, mee.blockPosition());
            mee.getPersistentData().putString(GuhHooks.BEWONER_NAAM, "dikkie");
            mee.tame(q);
            Bewoners.opJoin(mee);
            helper.assertTrue(mee.isTame() && mee.isOwnedBy(q) && !GuhHooks.isBewoner(mee) && GuhHooks.thuis(mee) == null && !mee.isInvulnerable(),
                    "the tamed one is q's guh, not a resident");
        } finally {
            leave(helper, p, q);
        }
        helper.succeed();
    }

    /** A town that misses a resident gets it back at its home spot (once), made like the town template makes it. */
    @GuhTest(template = PLEIN, batch = FIX)
    public static void knuffeldalVerdwenenBewonerKomtTerug(GameTestHelper helper) {
        var level = helper.getLevel();
        // the real town templates hold the residents (the houses in the four corners, Timmertje on the bouwplaats)
        java.util.Set<String> namen = new java.util.TreeSet<>();
        Bewoners.Plek dikkie = null;
        for (String t : List.of("plein", "hoek_noordoost", "hoek_noordwest", "hoek_zuidoost", "hoek_zuidwest", "bouwplaats", "beroepenstraat")) {
            for (Bewoners.Plek plek : Bewoners.template(level, Guhs.id("knuffeldal_stadje/" + t))) {
                namen.add(plek.naam());
                if (plek.naam().equals("dikkie")) {
                    dikkie = plek;
                }
            }
        }
        helper.assertTrue(namen.containsAll(KnuffeldalVoortgang.BEWONERS), "all six residents are read from the town's templates: " + namen);
        helper.assertTrue(dikkie != null, "Dikkie's template entry");
        // Dikkie's spot in this test "town"
        BlockPos thuis = helper.absolutePos(new BlockPos(4, 2, 4));
        net.minecraft.world.phys.AABB zoek = new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(0, 0, 0))).inflate(1)
                .minmax(new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(15, 6, 15))));
        List<Bewoners.Plek> plekken = List.of(new Bewoners.Plek("dikkie", Vec3.atBottomCenterOf(thuis), dikkie.nbt()));
        java.util.function.Supplier<List<GuhEntity>> dikkies = () -> level.getEntitiesOfClass(GuhEntity.class, zoek,
                g -> g.isAlive() && GuhHooks.isBewoner(g) && "dikkie".equals(g.getPersistentData().getStringOr(GuhHooks.BEWONER_NAAM, "")));
        // somebody tamed Dikkie away before the fix: that one is no resident any more
        GuhEntity oud = helper.spawn(ModEntities.GUH.get(), new BlockPos(8, 2, 8));
        GuhHooks.maakBewoner(oud, thuis);
        oud.getPersistentData().putString(GuhHooks.BEWONER_NAAM, "dikkie");
        ServerPlayer p = player(helper, new BlockPos(7, 2, 7));
        try {
            oud.tame(p);
            Bewoners.opJoin(oud);
            helper.assertTrue(dikkies.get().isEmpty(), "the town misses Dikkie");
            helper.assertTrue(Bewoners.herstel(level, plekken, zoek) == 1, "a new Dikkie comes");
            List<GuhEntity> nu = dikkies.get();
            helper.assertTrue(nu.size() == 1, "exactly one: " + nu.size());
            GuhEntity nieuw = nu.get(0);
            helper.assertTrue(!nieuw.isTame() && nieuw.isInvulnerable() && thuis.equals(GuhHooks.thuis(nieuw)) && nieuw.hasCustomName(),
                    "a real resident: at home, named, protected (home " + GuhHooks.thuis(nieuw) + ")");
            helper.assertTrue(Math.abs(nieuw.getGuhScale() - 1.35f) < 0.01f, "as big as the template's Dikkie: " + nieuw.getGuhScale());
            helper.assertTrue(Bewoners.herstel(level, plekken, zoek) == 0 && dikkies.get().size() == 1, "not twice");
            // every player can make friends with the new one
            ServerPlayer q = player(helper, new BlockPos(6, 2, 6));
            try {
                for (ServerPlayer s : List.of(p, q)) {
                    s.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    nieuw.mobInteract(s, InteractionHand.MAIN_HAND);
                    helper.assertTrue(KnusVoortgang.heeft(s, KnuffeldalVoortgang.VRIENDJES_BOEK, "dikkie"), "a friend for every player");
                }
            } finally {
                leave(helper, q);
            }
            // a double one (the old one turned up again): the re-created extra leaves
            GuhEntity dubbel = Bewoners.maak(level, plekken.get(0));
            helper.assertTrue(dubbel != null && dikkies.get().size() == 2, "two for a moment");
            helper.assertTrue(Bewoners.herstel(level, plekken, zoek) == 0 && dikkies.get().size() == 1, "one again");
            dikkies.get().forEach(Entity::discard);
            oud.discard();
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    /** The feast can't start while the player is in another event: the Burgemeester says so, and it starts the next time. */
    @GuhTest(template = PLEIN, batch = FIX)
    public static void knuffeldalFeestWachtOpAnderEvenement(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 7));
        GuhNpcEntity burgemeester = npc(helper, new BlockPos(7, 2, 5), GuhNpcEntity.Kind.BURGEMEESTERGUH);
        Evenement regen = null;
        try {
            Knusfeest.vergeet(p);
            Knusfeest.nieuweRonde(p, 0, EnumSet.of(Feesttaak.FEESTBLOEMEN));
            Burgemeester.gebracht(p, Feesttaak.FEESTBLOEMEN);
            regen = Evenementen.start(nl.juiced.guhs.feature.evenementen.EvenementType.KAASREGEN, p);
            helper.assertTrue(regen != null && Evenementen.eventOf(p) == regen, "in a kaasregen");
            Burgemeester.feest(burgemeester, p);
            helper.assertTrue(Evenementen.eventOf(p) == regen && Knusfeest.rondeBezig(p), "no feast yet (and nothing lost): he says to come back");
            regen.end(false);
            regen = null;
            helper.assertTrue(Evenementen.eventOf(p) == null, "the kaasregen is over");
            Burgemeester.feest(burgemeester, p);
            helper.assertTrue(Evenementen.eventOf(p) instanceof KnusfeestEvenement || !Knusfeest.rondeBezig(p), "now the feast starts (or its rewards come)");
            Evenement feest = Evenementen.eventOf(p);
            if (feest != null) {
                feest.end(false);
            }
        } finally {
            if (regen != null) {
                regen.end(false);
            }
            Knusfeest.vergeet(p);
            leave(helper, p);
        }
        helper.succeed();
    }

    /** A sneeuwguhkopje and Bob's dakpan work for a survival player inside a protected town (nothing is put back). */
    @GuhTest(template = PLEIN, batch = FIX)
    public static void knuffeldalEigenWerkInBeschermdStadje(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos onder = new BlockPos(11, 2, 11), boven = onder.above(), plek = new BlockPos(11, 2, 13);
        helper.setBlock(onder, Blocks.SNOW_BLOCK);
        helper.setBlock(boven, Blocks.SNOW_BLOCK);
        helper.setBlock(plek, nl.juiced.guhs.feature.beroepen.BeroepenFeature.DAKPLEK.get());
        BlockPos a = helper.absolutePos(new BlockPos(9, 0, 9)), b = helper.absolutePos(new BlockPos(14, 8, 14));
        PleinSlot.testStadje(BoundingBox.fromCorners(a, b));
        ServerPlayer p = player(helper, new BlockPos(10, 2, 10));
        try {
            helper.assertTrue(KnuffeldalProtection.inStadje(level, helper.absolutePos(onder)), "in the protected (test) town");
            // still protected: a dirt block isn't placed
            BlockPos naast = helper.absolutePos(new BlockPos(12, 1, 12));
            level.setBlock(naast.above(), Blocks.AIR.defaultBlockState(), 3);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 2));
            p.gameMode.useItemOn(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(naast).add(0, 0.5, 0), net.minecraft.core.Direction.UP, naast, false));
            helper.assertTrue(!level.getBlockState(naast.above()).is(Blocks.DIRT) && p.getMainHandItem().getCount() == 2,
                    "no building in the town: " + level.getBlockState(naast.above()) + " x" + p.getMainHandItem().getCount());
            // the sneeuwpopguh
            BlockPos top = helper.absolutePos(boven);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KnuffeldalFeature.SNEEUWGUHKOPJE.get(), 2));
            p.gameMode.useItemOn(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(top).add(0, 0.5, 0), net.minecraft.core.Direction.UP, top, false));
            helper.assertTrue(helper.getBlockState(onder).is(KnuffeldalFeature.SNEEUWPOPGUH.get()) && helper.getBlockState(boven).is(KnuffeldalFeature.SNEEUWPOPGUH.get()),
                    "the sneeuwpopguh stands (and stays): " + helper.getBlockState(onder));
            helper.assertTrue(p.getMainHandItem().getCount() == 1, "one kopje used");
            // Bob's dakpan (a block item) on his ghost tile
            BlockPos tegel = helper.absolutePos(plek);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(nl.juiced.guhs.feature.beroepen.BeroepenFeature.DAKPAN_ITEM.get(), 2));
            p.gameMode.useItemOn(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(tegel).add(0, 0.5, 0), net.minecraft.core.Direction.UP, tegel, false));
            helper.assertTrue(helper.getBlockState(plek).is(nl.juiced.guhs.feature.beroepen.BeroepenFeature.DAKPAN.get()) && p.getMainHandItem().getCount() == 1,
                    "the dakpan lies on the ghost tile (and stays): " + helper.getBlockState(plek));
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }
}
