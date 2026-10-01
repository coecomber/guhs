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
        KnusfeestEvenement feest = KnusfeestEvenement.maak(helper.getLevel(), p);
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
        helper.assertTrue(pluis > n * 0.25 && pluis < n * 0.45, "about 35%: " + pluis + " of " + n);
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
}
