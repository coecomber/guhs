package nl.juiced.guhs.feature.techbron;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.sausdieren.SausdierenFeature;
import nl.juiced.guhs.feature.spiesburcht.AangebrandeMikaEntity;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.feature.vadskracht.BatterijBlock;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.feature.vadskracht.GuhradKracht;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.TestMachineBlock;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadsNet;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.NlTekst;

/**
 * Game tests of the vadskracht sources (bbq2 tech-bronnen, batch "techbron"): guhs come to the Knuffelgenerator by
 * themselves and lie on it (who comes and who does not, at most eight, a cuddled guh runs harder in a Guhrad), the
 * Disco-dynamo (no disc no dance, the looping turntable, four dancers, the rare disc, every disc its own show), the
 * Blubkacheltje (the jar goes in and comes out as it was, knabbels by hand and by pipe, hungry = cold), the
 * Gloeisterkern, how many of each kind count per net and what the readout says, the Knabbelbatterij that keeps its
 * charge, the Guhrad per variant, the recipes and tags, and the flag "Aangebrande Mika verslagen".
 * Template techbron_test_kamer: 15 x 6 x 15 with a stone floor (things stand at helper y 2). The tests in which guhs walk
 * have a batch of their own: a source calls every tamed guh within 8 blocks, also the ones of the test next door.
 */
public class TechbronGameTests {
    private static final String BATCH = "techbron", KAMER = "techbron_test_kamer";

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    static BlockPos p(int x, int z) {
        return new BlockPos(x, 2, z);
    }

    static ServerPlayer speler(GameTestHelper helper, int x, int z) {
        ServerPlayer speler = GuhMockPlayer.of(helper);
        speler.setGameMode(GameType.SURVIVAL);
        speler.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(p(x, z));
        speler.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return speler;
    }

    static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer speler : spelers) {
            helper.getLevel().removePlayerImmediately(speler, Entity.RemovalReason.DISCARDED);
        }
    }

    /** A tamed guh of this player that was told to stay ("Rondvadsen: uit"): it does not follow its owner. */
    static GuhEntity guh(GameTestHelper helper, ServerPlayer eigenaar, int x, int z) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), p(x, z));
        guh.tame(eigenaar);
        guh.setWandering(false);
        return guh;
    }

    /** A tamed guh that sits (was told to sit) on this spot. */
    static GuhEntity zittend(GameTestHelper helper, ServerPlayer eigenaar, Vec3 abs) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), p(1, 1));
        guh.snapTo(abs.x, abs.y, abs.z);
        guh.tame(eigenaar);
        guh.setOrderedToSit(true);
        guh.setInSittingPose(true);
        return guh;
    }

    @SuppressWarnings("unchecked")
    static <T> T be(GameTestHelper helper, BlockPos pos) {
        return (T) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    static VadsNet net(GameTestHelper helper, BlockPos pos) {
        return VadsKracht.net(helper.getLevel(), helper.absolutePos(pos));
    }

    /** The hover readout of this block as a Dutch player reads it. */
    static List<String> lees(GameTestHelper helper, BlockPos pos) {
        return VadsKracht.regels(helper.getLevel(), helper.absolutePos(pos)).stream().map(NlTekst::tekst).toList();
    }

    static void gelijk(GameTestHelper helper, Object verwacht, Object echt, String wat) {
        helper.assertTrue(verwacht.equals(echt), wat + ": expected " + verwacht + ", got " + echt);
    }

    static Snoet snoet(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(BronBlock.SNOET);
    }

    static BlockHitResult raak(GameTestHelper helper, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    }

    /** The player right-clicks this block with what they hold in their main hand. */
    static InteractionResult klik(GameTestHelper helper, ServerPlayer speler, BlockPos pos) {
        ItemStack hand = speler.getMainHandItem();
        BlockState state = helper.getBlockState(pos);
        InteractionResult r = hand.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND
                : state.useItemOn(hand, helper.getLevel(), speler, InteractionHand.MAIN_HAND, raak(helper, pos));
        if (r == InteractionResult.TRY_WITH_EMPTY_HAND) {
            r = state.useWithoutItem(helper.getLevel(), speler, raak(helper, pos));
        }
        return r;
    }

    static TestMachineBlock.Kern machine(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, VadskrachtFeature.TESTMACHINE.get());
        return be(helper, pos);
    }

    /** Every guh in and around the test room, for a failure message: where it is (helper coordinates) and what it does. */
    static String wie(GameTestHelper helper, net.minecraft.world.phys.AABB zone) {
        StringBuilder uit = new StringBuilder();
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        for (GuhEntity guh : helper.getLevel().getEntitiesOfClass(GuhEntity.class, helper.getBounds().inflate(12))) {
            uit.append(String.format(java.util.Locale.ROOT, " [%.1f %.1f %.1f%s%s%s%s%s %s]", guh.getX() - o.getX(), guh.getY() - o.getY(), guh.getZ() - o.getZ(),
                    guh.isTame() ? " tam" : " wild", guh.isOrderedToSit() ? " zit" : "", guh.mayWander() ? " volgt" : "",
                    GuhTrek.bron(guh) != null ? " geroepen" : "", zone.contains(guh.position()) ? " erop" : "", guh.emotes.current()));
        }
        return uit.toString();
    }

    static boolean ligt(GuhEntity guh, KnuffelgeneratorBlockEntity kussen) {
        return GuhTrek.doetMee(guh, Emote.SLAPEN) && kussen.zone().inflate(0.75, 0, 0.75).contains(guh.position());
    }

    /** How wide a guh is. */
    static double guhBreedte() {
        return ModEntities.GUH.get().getWidth();
    }

    // =====================================================================================================================
    // Knuffelgenerator
    // =====================================================================================================================

    /**
     * Tamed guhs within 8 blocks come and lie on the cushion by themselves and each gives 3 VK; a wild guh, a guh that
     * sits somewhere else and a guh that lives in a Guhhuisje do not. Lying there makes them blij. A part of the cushion
     * reads like the kern; breaking a part takes the whole cushion and sets the guhs free.
     */
    @GuhTest(template = KAMER, batch = BATCH + "_knuffel", timeoutTicks = 600)
    public static void techbronKnuffelgeneratorGuhsKomenVanzelf(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer speler = speler(helper, 1, 1);
        BlockPos kern = p(6, 6);
        helper.setBlock(kern, TechbronFeature.KNUFFELGENERATOR.get());   // facing north: the cushion is x 6..7, z 6..7
        for (BlockPos deel : List.of(p(7, 6), p(6, 7), p(7, 7))) {
            helper.assertBlockPresent(TechbronFeature.KUSSEN_DEEL.get(), deel);
        }
        KnuffelgeneratorBlockEntity kussen = be(helper, kern);
        helper.setBlock(p(8, 6), ModBlocks.GUH_WIRE.get());              // Guhdraad against a part, to a machine that asks 5
        TestMachineBlock.Kern machine = machine(helper, p(9, 6));
        gelijk(helper, 0, kussen.vadsAanbod(), "an empty cushion gives nothing");
        helper.assertTrue(lees(helper, kern).contains("Nog geen guh op het kussen: tamme guhs binnen " + TechbronGetallen.BEREIK
                + " blokken komen vanzelf liggen"), "readout of the empty cushion: " + lees(helper, kern));
        gelijk(helper, 8, kussen.plekken().size(), "eight spots");
        helper.assertTrue(kussen.plekken().stream().allMatch(plek -> kussen.zone(0).contains(plek)), "every spot lies on the cushion");
        List<Vec3> plekken = kussen.plekken();
        for (Vec3 a : plekken) {
            for (Vec3 b : plekken) {
                helper.assertTrue(a == b || Math.max(Math.abs(a.x - b.x), Math.abs(a.z - b.z)) > guhBreedte(), "guhs on their spots do not push each other");
            }
        }

        List<GuhEntity> tam = List.of(guh(helper, speler, 2, 2), guh(helper, speler, 12, 4), guh(helper, speler, 10, 12));
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), p(3, 10));
        GuhEntity zit = zittend(helper, speler, Vec3.atBottomCenterOf(helper.absolutePos(p(12, 12))));
        // who may come, asked directly (a resident cannot wait for the walk: this test has no huisje, and the huisjes set a guh
        // without one free again)
        BlockPos kernAbs = helper.absolutePos(kern);
        Vec3 midden = kussen.zone().getCenter();
        GuhEntity bewoner = guh(helper, speler, 3, 12);
        helper.assertTrue(GuhTrek.magMee(bewoner, kernAbs, kussen.zone(0), midden, Emote.SLAPEN), "a free tamed guh may come");
        bewoner.getPersistentData().putLong(nl.juiced.guhs.feature.huisje.Huisjes.THUIS, 0L);    // (what Huisjes.isBewoner reads)
        helper.assertTrue(nl.juiced.guhs.feature.huisje.Huisjes.isBewoner(bewoner), "the test's resident counts as one");
        helper.assertFalse(GuhTrek.magMee(bewoner, kernAbs, kussen.zone(0), midden, Emote.SLAPEN), "a guh that lives in a Guhhuisje does not come");
        bewoner.discard();
        helper.assertFalse(GuhTrek.magMee(wild, kernAbs, kussen.zone(0), midden, Emote.SLAPEN), "a wild guh does not come");
        helper.assertFalse(GuhTrek.magMee(zit, kernAbs, kussen.zone(0), midden, Emote.SLAPEN), "a guh that sits somewhere else does not come");

        helper.succeedWhen(() -> {
            gelijk(helper, 3, kussen.guhs(), "three guhs on the cushion;" + wie(helper, kussen.zone()));
            for (GuhEntity guh : tam) {
                helper.assertTrue(ligt(guh, kussen), "a tamed guh lies asleep on the cushion: " + guh.position() + " " + guh.emotes.current());
                helper.assertTrue(Band.isBlij(guh), "lying on the cushion makes a guh blij");
            }
            helper.assertFalse(kussen.zone().contains(wild.position()) && wild.emotes.current() == Emote.SLAPEN && GuhTrek.bron(wild) != null,
                    "a wild guh is not called");
            helper.assertTrue(GuhTrek.bron(wild) == null && GuhTrek.bron(zit) == null, "a wild guh and a guh that sits elsewhere are not called");
            helper.assertTrue(zit.blockPosition().equals(helper.absolutePos(p(12, 12))), "the sitting guh stayed where it sits");
            gelijk(helper, 3 * VadsGetallen.KNUFFEL_PER_GUH, kussen.vadsAanbod(), "aanbod");
            VadsNet net = net(helper, p(8, 6));
            gelijk(helper, 3 * VadsGetallen.KNUFFEL_PER_GUH, net.aanbod(), "the net's aanbod");
            helper.assertTrue(net.draait() && machine.heeftKracht(), "the machine runs on cuddles");
            gelijk(helper, Snoet.WERKT, snoet(helper, kern), "the face");
            List<String> regels = lees(helper, p(7, 7));                 // a part reads like the kern
            helper.assertTrue(regels.contains("Dit geeft 9 vadskracht") && regels.contains("3/8 guhs liggen te knuffelen, njeg!"),
                    "readout through a part: " + regels);
            // a guh that comes straight from the cushion runs extra hard in a Guhrad
            CompoundTag opgepakt = Nbt.saveWithoutId(tam.get(0));
            helper.assertTrue(GuhradKracht.isBlij(opgepakt, level.getGameTime()), "the picked-up guh is still blij");
            gelijk(helper, VadsGetallen.GUHRAD_BLIJ, GuhradKracht.van(opgepakt, GuhradKracht.isBlij(opgepakt, level.getGameTime())), "a cuddled guh in a Guhrad");
            // breaking a part takes the whole cushion with it and sets the guhs free
            level.destroyBlock(helper.absolutePos(p(7, 7)), true);
            for (BlockPos weg : List.of(kern, p(7, 6), p(6, 7), p(7, 7))) {
                helper.assertBlockPresent(Blocks.AIR, weg);
            }
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, helper.getBounds(), e -> e.getItem().is(TechbronFeature.KNUFFELGENERATOR_ITEM.get())).size() == 1,
                    "the cushion drops as one item");
            for (GuhEntity guh : tam) {
                helper.assertTrue(guh.emotes.current() == null && GuhTrek.bron(guh) == null, "the guhs are free again");
            }
            weg(helper, speler);
        });
    }

    /** More guhs than fit: eight count (24 VK), the face looks surprised; guhs that sit on the cushion count where they sit. */
    @GuhTest(template = KAMER, batch = BATCH + "_vol", timeoutTicks = 600)
    public static void techbronKnuffelgeneratorHooguitAchtGuhs(GameTestHelper helper) {
        ServerPlayer speler = speler(helper, 1, 1);
        BlockPos kern = p(6, 6);
        helper.setBlock(kern, TechbronFeature.KNUFFELGENERATOR.get());
        KnuffelgeneratorBlockEntity kussen = be(helper, kern);
        List<GuhEntity> guhs = new ArrayList<>();
        List<Vec3> plekken = kussen.plekken();
        for (int i = 0; i < 2; i++) {
            guhs.add(zittend(helper, speler, plekken.get(i)));           // two that were told to sit on the cushion
        }
        for (int i = 0; i < 8; i++) {
            guhs.add(guh(helper, speler, 3 + i, 3 + (i % 2) * 7));       // eight more around it: ten guhs for eight spots
        }
        helper.succeedWhen(() -> {
            gelijk(helper, VadsGetallen.KNUFFEL_MAX_GUHS, kussen.guhs(), "eight guhs count;" + wie(helper, kussen.zone()));
            gelijk(helper, VadsGetallen.KNUFFEL_MAX_GUHS * VadsGetallen.KNUFFEL_PER_GUH, kussen.vadsAanbod(), "aanbod of a full cushion");
            gelijk(helper, Snoet.VOL, snoet(helper, kern), "a full cushion looks surprised");
            helper.assertTrue(ligt(guhs.get(0), kussen) && ligt(guhs.get(1), kussen) && guhs.get(0).isOrderedToSit(), "the sitting guhs count where they sit");
            long geroepen = guhs.stream().filter(guh -> GuhTrek.bron(guh) != null).count();
            gelijk(helper, (long) VadsGetallen.KNUFFEL_MAX_GUHS, geroepen, "only eight are called, the other two are left alone");
            weg(helper, speler);
        });
    }

    /** A guh that follows its owner joins while the owner is near the cushion and leaves when they walk away. */
    @GuhTest(template = KAMER, batch = BATCH + "_volger", timeoutTicks = 600)
    public static void techbronVolgendeGuhGaatMeeMetZijnBaas(GameTestHelper helper) {
        ServerPlayer speler = speler(helper, 4, 6);
        BlockPos kern = p(6, 6);
        helper.setBlock(kern, TechbronFeature.KNUFFELGENERATOR.get());
        KnuffelgeneratorBlockEntity kussen = be(helper, kern);
        GuhEntity volger = guh(helper, speler, 3, 4);
        volger.setWandering(true);                                       // (the default of a tamed guh: it follows)
        volger.setTeleportEnabled(false);                                // (so it stays in the test room when its owner is far)
        GuhEntity blijver = guh(helper, speler, 10, 9);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ligt(volger, kussen) && ligt(blijver, kussen) && kussen.guhs() == 2, "both lie on the cushion: " + kussen.guhs()))
                .thenExecute(() -> {
                    // (straight up: further than EIGENAAR_BEREIK from the cushion, and the guh cannot walk after them out of the room)
                    BlockPos ver = helper.absolutePos(p(6, 6)).above(TechbronGetallen.EIGENAAR_BEREIK + 30);
                    speler.snapTo(ver.getX() + 0.5, ver.getY(), ver.getZ() + 0.5);
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(GuhTrek.bron(volger) == null && !GuhTrek.doetMee(volger, Emote.SLAPEN), "the follower was let go");
                    helper.assertTrue(ligt(blijver, kussen), "the guh that was told to stay keeps the cushion warm");
                    gelijk(helper, 1, kussen.guhs(), "one guh left");
                })
                .thenExecute(() -> {
                    volger.discard();
                    weg(helper, speler);
                })
                .thenSucceed();
    }

    // =====================================================================================================================
    // Disco-dynamo
    // =====================================================================================================================

    /**
     * No disc, no dance. A disc on the turntable (through a part of the floor) plays and plays again; four guhs dance for
     * 5 VK each (a fifth and a wild one do not); the mod's own disc gives 1 more per dancer; an empty hand takes the disc
     * off and the guhs are free.
     */
    @GuhTest(template = KAMER, batch = BATCH + "_disco", timeoutTicks = 800)
    public static void techbronDiscoDynamo(GameTestHelper helper) {
        ServerPlayer speler = speler(helper, 1, 1);
        BlockPos kern = p(7, 5);
        helper.setBlock(kern, TechbronFeature.DISCO_DYNAMO.get());       // facing north: the floor is x 6..8, z 5..7
        for (int x = 6; x <= 8; x++) {
            for (int z = 5; z <= 7; z++) {
                if (x != 7 || z != 5) {
                    helper.assertBlockPresent(TechbronFeature.VLOER_DEEL.get(), p(x, z));
                }
            }
        }
        DiscoDynamoBlockEntity disco = be(helper, kern);
        gelijk(helper, 4, disco.plekken().size(), "four spots");
        helper.assertTrue(disco.plekken().stream().allMatch(plek -> disco.zone().contains(plek)), "every spot is on the floor");
        List<GuhEntity> guhs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            guhs.add(guh(helper, speler, 3 + 2 * i, 11));
        }
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), p(3, 3));
        helper.startSequence()
                .thenExecuteAfter(50, () -> {
                    gelijk(helper, 0, disco.dansers(), "without a disc nobody dances");
                    gelijk(helper, 0, disco.vadsAanbod(), "and it gives nothing");
                    helper.assertTrue(guhs.stream().allMatch(guh -> GuhTrek.bron(guh) == null), "nobody is called without a disc");
                    gelijk(helper, Snoet.SLAAPT, snoet(helper, kern), "asleep");
                    gelijk(helper, 0, helper.getBlockState(kern).getLightEmission(), "dark");
                    helper.assertTrue(lees(helper, kern).contains("Geen plaat: leg een muziekplaat op de draaitafel"), "readout: " + lees(helper, kern));
                    // a disc, put on through a floor part
                    speler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MUSIC_DISC_CAT));
                    gelijk(helper, InteractionResult.SUCCESS, klik(helper, speler, p(8, 7)), "a disc on a floor part");
                    helper.assertTrue(disco.plaat().is(Items.MUSIC_DISC_CAT) && speler.getMainHandItem().isEmpty(), "the disc is on the turntable");
                    speler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                    klik(helper, speler, kern);
                    helper.assertTrue(disco.plaat().is(Items.MUSIC_DISC_CAT), "a stick is no disc and takes nothing off");
                    speler.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(disco.speelt(), "the song plays");
                    gelijk(helper, VadsGetallen.DISCO_MAX_GUHS, disco.dansers(), "four dancers");
                    gelijk(helper, VadsGetallen.DISCO_MAX_GUHS * VadsGetallen.DISCO_PER_GUH, disco.vadsAanbod(), "aanbod");
                    long dansen = guhs.stream().filter(guh -> GuhTrek.doetMee(guh, Emote.DANSEN) && disco.zone().inflate(0.75, 0, 0.75).contains(guh.position())).count();
                    gelijk(helper, (long) VadsGetallen.DISCO_MAX_GUHS, dansen, "four of the five dance on the floor");
                    helper.assertTrue(GuhTrek.bron(wild) == null, "a wild guh is not called");
                    gelijk(helper, Snoet.VOL, snoet(helper, kern), "a full floor looks surprised");
                    gelijk(helper, DiscoDynamoBlock.LICHT, helper.getBlockState(kern).getLightEmission(), "the floor gives light");
                    List<String> regels = lees(helper, kern);
                    helper.assertTrue(regels.contains("Dit geeft 20 vadskracht") && regels.contains("4/4 guhs dansen de vadskracht bij elkaar, vahoeg!")
                            && regels.stream().anyMatch(r -> r.startsWith("Draait: ")), "readout: " + regels);
                    helper.assertFalse(regels.stream().anyMatch(r -> r.startsWith("Zeldzame plaat")), "an ordinary disc has no bonus line");
                })
                .thenExecute(() -> {
                    // the mod's own (rare) disc: the old one comes back, every dancer gives one more
                    speler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MUSIC_DISC_ZE_HANGEN.get()));
                    klik(helper, speler, kern);
                    helper.assertTrue(disco.plaat().is(ModItems.MUSIC_DISC_ZE_HANGEN.get()) && disco.zeldzaam(), "the rare disc is on");
                    helper.assertTrue(speler.getInventory().contains(new ItemStack(Items.MUSIC_DISC_CAT)), "the old disc came back");
                    gelijk(helper, VadsGetallen.DISCO_PER_GUH + TechbronGetallen.DISCO_BONUS, disco.perDanser(), "per dancer with the rare disc");
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(disco.speelt(), "the new song plays");
                    gelijk(helper, VadsGetallen.DISCO_MAX_GUHS * (VadsGetallen.DISCO_PER_GUH + TechbronGetallen.DISCO_BONUS), disco.vadsAanbod(), "aanbod with the rare disc");
                    gelijk(helper, disco.vadsAanbod(), net(helper, kern).aanbod(), "the net knows");
                    helper.assertTrue(lees(helper, kern).contains("Zeldzame plaat: elke danser geeft 1 vadskracht extra"), "readout: " + lees(helper, kern));
                })
                .thenExecute(() -> {
                    // an empty hand takes the disc off: silence, and the guhs are free
                    speler.getInventory().clearContent();
                    klik(helper, speler, kern);
                    helper.assertTrue(disco.plaat().isEmpty() && speler.getInventory().contains(new ItemStack(ModItems.MUSIC_DISC_ZE_HANGEN.get())), "the disc is back in your pockets");
                    helper.assertFalse(disco.speelt(), "silence");
                    gelijk(helper, 0, disco.vadsAanbod(), "no disc, nothing");
                    helper.assertTrue(guhs.stream().allMatch(guh -> GuhTrek.bron(guh) == null && guh.emotes.current() == null), "the dancers are free");
                    weg(helper, speler);
                })
                .thenSucceed();
    }

    /** The song starts again when it is over: the turntable loops. (The mod's disc is the shortest: 25 seconds.) */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 800)
    public static void techbronDiscoDraaitafelBlijftDraaien(GameTestHelper helper) {
        BlockPos kern = p(7, 5);
        helper.setBlock(kern, TechbronFeature.DISCO_DYNAMO.get());
        DiscoDynamoBlockEntity disco = be(helper, kern);
        disco.zetPlaat(new ItemStack(ModItems.MUSIC_DISC_ZE_HANGEN.get()));
        int lengte = disco.liedje().orElseThrow().value().lengthInTicks();
        helper.assertTrue(lengte + 40 < 780, "the song fits in this test: " + lengte);
        helper.startSequence()
                .thenExecuteAfter(5, () -> helper.assertTrue(disco.speelt(), "it plays"))
                .thenExecuteAfter(lengte + 30, () -> helper.assertTrue(disco.speelt(), "the song was over, and it plays again"))
                .thenExecute(() -> {
                    // saved with the block: after a reload it is still on the turntable
                    CompoundTag tag = disco.saveWithoutMetadata(helper.getLevel().registryAccess());
                    DiscoDynamoBlockEntity kopie = new DiscoDynamoBlockEntity(disco.getBlockPos(), disco.getBlockState());
                    Nbt.loadBlockEntity(kopie, helper.getLevel().registryAccess(), tag);
                    helper.assertTrue(kopie.plaat().is(ModItems.MUSIC_DISC_ZE_HANGEN.get()), "the disc is saved");
                    // breaking the floor drops the disc
                    helper.getLevel().destroyBlock(helper.absolutePos(kern), true);
                    helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(),
                            e -> e.getItem().is(ModItems.MUSIC_DISC_ZE_HANGEN.get())).size() == 1, "the disc drops");
                    helper.assertBlockPresent(Blocks.AIR, p(8, 7));
                })
                .thenSucceed();
    }

    /** Every disc has its own light show: all vanilla discs and the mod's own, none the same; a strange disc always gets the same one. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void techbronElkePlaatEenEigenLichtshow(GameTestHelper helper) {
        List<Identifier> vast = LichtShow.vasteplaten();
        // every disc of vanilla and of the mod has a hand-picked show, and every entry of the table is a real disc
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            boolean plaat = JukeboxSong.fromStack(new ItemStack(item)).isPresent();
            if (plaat && (id.getNamespace().equals("minecraft") || id.getNamespace().equals(Guhs.MODID))) {
                helper.assertTrue(vast.contains(id), id + " has no light show of its own");
            }
        }
        for (Identifier id : vast) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id) && JukeboxSong.fromStack(new ItemStack(BuiltInRegistries.ITEM.getValue(id))).isPresent(),
                    id + " in the table is not a disc");
        }
        for (int i = 0; i < vast.size(); i++) {
            LichtShow a = LichtShow.van(vast.get(i));
            for (int j = i + 1; j < vast.size(); j++) {
                helper.assertFalse(a.zelfde(LichtShow.van(vast.get(j))), vast.get(i) + " and " + vast.get(j) + " have the same show");
            }
            // lamps are opaque, lit, and the show moves
            boolean beweegt = false, licht = false;
            for (int rij = 0; rij < LichtShow.RASTER; rij++) {
                for (int kolom = 0; kolom < LichtShow.RASTER; kolom++) {
                    int nu = a.kleur(kolom, rij, 0f);
                    helper.assertTrue((nu >>> 24) == 0xFF, "a lamp is opaque");
                    licht |= (nu & 0xFFFFFF) != 0;
                    for (int t = 1; t <= 4; t++) {
                        beweegt |= a.kleur(kolom, rij, t * a.tempo()) != nu;
                    }
                }
            }
            helper.assertTrue(beweegt && licht, vast.get(i) + ": the show gives light and moves");
        }
        gelijk(helper, LichtShow.Patroon.HART, LichtShow.van(new ItemStack(ModItems.MUSIC_DISC_ZE_HANGEN.get())).patroon(), "the mod's own disc beats like a heart");
        LichtShow vreemd = LichtShow.van(Identifier.parse("anderemod:music_disc_njeg"));
        helper.assertTrue(vreemd.zelfde(LichtShow.van(Identifier.parse("anderemod:music_disc_njeg"))) && vreemd.patroon() != LichtShow.Patroon.HART
                && vreemd.kleuren().length == 3, "a strange disc always gets the same show (never the heart)");
        helper.assertTrue(LichtShow.van(ItemStack.EMPTY) == LichtShow.UIT && (LichtShow.UIT.kleur(2, 3, 7f) & 0xFFFFFF) == 0, "no disc: dark");
        helper.succeed();
    }

    // =====================================================================================================================
    // Blubkacheltje
    // =====================================================================================================================

    /**
     * The jar goes in by hand (and only one), knabbels by hand and through the item capability (nothing else, and nothing
     * comes out); a fed blubje gives 6 VK and takes the next knabbel by itself; a hungry one gives nothing; sneak + an
     * empty hand gives the same jar back; breaking drops jar and knabbels.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void techbronBlubkacheltje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer speler = speler(helper, 1, 1);
        BlockPos a = p(5, 5), b = p(10, 5);
        helper.setBlock(a, TechbronFeature.BLUBKACHELTJE.get());
        helper.setBlock(b, TechbronFeature.BLUBKACHELTJE.get());
        helper.setBlock(p(6, 5), ModBlocks.GUH_WIRE.get());
        TestMachineBlock.Kern machine = machine(helper, p(7, 5));
        BlubkacheltjeBlockEntity ka = be(helper, a), kb = be(helper, b);
        ItemStack potje = new ItemStack(SausdierenFeature.SAUSBLUBJE_POTJE.get());
        potje.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Blubbert"));

        // A: knabbels first (the bakje holds 16), then the jar: it starts on a knabbel at once
        helper.assertTrue(lees(helper, a).contains("Het potje is leeg: stop er een Sausblubje in een potje in"), "readout of an empty stove: " + lees(helper, a));
        speler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 20));
        gelijk(helper, InteractionResult.SUCCESS, klik(helper, speler, a), "knabbels in the bakje");
        gelijk(helper, TechbronGetallen.BLUB_VOER_MAX, ka.voorraad(), "the bakje is full");
        gelijk(helper, 20 - TechbronGetallen.BLUB_VOER_MAX, speler.getMainHandItem().getCount(), "the rest stays in your hand");
        gelijk(helper, InteractionResult.CONSUME, klik(helper, speler, a), "a full bakje takes no more");
        gelijk(helper, 0, ka.vadsAanbod(), "knabbels without a blubje give nothing");
        speler.setItemInHand(InteractionHand.MAIN_HAND, potje.copy());
        gelijk(helper, InteractionResult.SUCCESS, klik(helper, speler, a), "the jar goes in");
        helper.assertTrue(ka.heeftBlubje() && ka.warm() && speler.getMainHandItem().isEmpty(), "the blubje is in and warm");
        gelijk(helper, TechbronGetallen.BLUB_VOER_MAX - 1, ka.voorraad(), "it took a knabbel at once");
        gelijk(helper, TechbronGetallen.BLUB_SECONDEN * 20, ka.warmTicks(), "one knabbel lasts");
        gelijk(helper, VadsGetallen.BLUBKACHELTJE, ka.vadsAanbod(), "aanbod");
        speler.setItemInHand(InteractionHand.MAIN_HAND, potje.copy());
        gelijk(helper, InteractionResult.CONSUME, klik(helper, speler, a), "a second jar does not fit");
        helper.assertTrue(speler.getMainHandItem().getCount() == 1, "and stays in your hand");
        speler.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        // B: the jar first: hungry. Then knabbels through the item capability (a Knabbelbuis, a hopper)
        kb.zetBlubje(potje.copy());
        helper.assertTrue(kb.heeftBlubje() && !kb.warm() && kb.vadsAanbod() == 0, "a blubje without a knabbel is cold");
        helper.assertTrue(lees(helper, b).contains("Het Sausblubje heeft trek: geef het een kaasknabbel, njeg!"), "readout of a hungry blubje: " + lees(helper, b));
        ResourceHandler<ItemResource> buis = Kisten.van(level, helper.absolutePos(b), Direction.UP);
        helper.assertTrue(buis != null, "the stove has an item capability");
        helper.assertTrue(Kisten.stop(buis, new ItemStack(Items.COBBLESTONE, 3)).getCount() == 3, "only knabbels go in");
        helper.assertTrue(Kisten.stop(buis, new ItemStack(ModItems.KAAS_KNABBELS.get(), 3)).isEmpty(), "three knabbels go in through a pipe");
        helper.assertTrue(Kisten.neem(buis, stack -> true, 64).isEmpty(), "nothing comes out through a pipe");

        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(kb.warm() && kb.voorraad() == 2, "the hungry blubje took a knabbel by itself: " + kb.voorraad());
                    gelijk(helper, Snoet.WERKT, snoet(helper, a), "A's face");
                    gelijk(helper, Snoet.WERKT, snoet(helper, b), "B's face");
                    gelijk(helper, BlubkacheltjeBlock.LICHT, helper.getBlockState(a).getLightEmission(), "a warm stove glows");
                    VadsNet net = net(helper, p(6, 5));
                    gelijk(helper, VadsGetallen.BLUBKACHELTJE, net.aanbod(), "the net's aanbod");
                    helper.assertTrue(net.draait() && machine.heeftKracht(), "6 VK runs a machine that asks 5");
                    helper.assertTrue(lees(helper, a).contains("Dit geeft 6 vadskracht")
                            && lees(helper, a).contains("Het Sausblubje blubt lekker warm (15/16 knabbels in het bakje)"), "readout: " + lees(helper, a));
                    // top A's bakje up; B's knabbel is nearly finished
                    ka.voer(new ItemStack(ModItems.KAAS_KNABBELS.get(), 4), true);
                    kb.zetWarm(1);
                })
                .thenExecuteAfter(3, () -> {
                    gelijk(helper, TechbronGetallen.BLUB_VOER_MAX, ka.voorraad(), "A's bakje is full again (one of the four fitted)");
                    gelijk(helper, Snoet.VOL, snoet(helper, a), "a full bakje looks surprised");
                    helper.assertTrue(kb.warm() && kb.voorraad() == 1, "B took the next knabbel");
                    ka.zetWarm(1);
                    kb.zetWarm(1);
                })
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(ka.warm() && ka.voorraad() == TechbronGetallen.BLUB_VOER_MAX - 1 && ka.warmTicks() > 1000, "A took the next knabbel: " + ka.voorraad());
                    gelijk(helper, Snoet.WERKT, snoet(helper, a), "A's face when there is room again");
                    helper.assertTrue(kb.warm() && kb.voorraad() == 0, "B's last knabbel");
                    kb.zetWarm(1);
                })
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(!kb.warm() && kb.vadsAanbod() == 0, "no knabbels left: cold");
                    gelijk(helper, Snoet.SLAAPT, snoet(helper, b), "a cold stove sleeps");
                    gelijk(helper, 0, helper.getBlockState(b).getLightEmission(), "and is dark");
                    // sneak + an empty hand: the same jar comes back
                    speler.setShiftKeyDown(true);
                    klik(helper, speler, a);
                    speler.setShiftKeyDown(false);
                    ItemStack terug = speler.getMainHandItem();
                    helper.assertTrue(ItemStack.isSameItemSameComponents(terug, potje), "exactly the jar that went in comes back: " + terug);
                    helper.assertTrue(!ka.heeftBlubje() && ka.vadsAanbod() == 0 && ka.voorraad() == TechbronGetallen.BLUB_VOER_MAX - 1, "A is empty, its knabbels stay");
                    // breaking: the knabbels of A, the jar of B
                    level.destroyBlock(helper.absolutePos(a), true);
                    level.destroyBlock(helper.absolutePos(b), true);
                    List<ItemEntity> gevallen = level.getEntitiesOfClass(ItemEntity.class, helper.getBounds());
                    int knabbels = gevallen.stream().filter(e -> e.getItem().is(ModItems.KAAS_KNABBELS.get())).mapToInt(e -> e.getItem().getCount()).sum();
                    gelijk(helper, TechbronGetallen.BLUB_VOER_MAX - 1, knabbels, "A's knabbels drop");
                    helper.assertTrue(gevallen.stream().anyMatch(e -> ItemStack.isSameItemSameComponents(e.getItem(), potje)), "B's jar drops as it was");
                    gelijk(helper, 2L, gevallen.stream().filter(e -> e.getItem().is(TechbronFeature.BLUBKACHELTJE_ITEM.get())).count(), "both stoves drop");
                    weg(helper, speler);
                })
                .thenSucceed();
    }

    // =====================================================================================================================
    // how many count per net, the Gloeisterkern
    // =====================================================================================================================

    /**
     * Three Knuffelgeneratoren, three Disco-dynamo's, five Blubkacheltjes and two Gloeisterkernen, each kind in a net of
     * its own: 2, 2, 4 and 1 count. The one too many sleeps and its readout says "telt niet mee".
     */
    @GuhTest(template = KAMER, batch = BATCH + "_soort", timeoutTicks = 400)
    public static void techbronHooguitZoveelPerSoort(GameTestHelper helper) {
        ServerPlayer speler = speler(helper, 0, 14);
        // cushions x 0..5, z 0..1 (side by side: one net), a sitting guh on each
        KnuffelgeneratorBlockEntity[] kussens = new KnuffelgeneratorBlockEntity[3];
        for (int i = 0; i < 3; i++) {
            helper.setBlock(p(2 * i, 0), TechbronFeature.KNUFFELGENERATOR.get());
            kussens[i] = be(helper, p(2 * i, 0));
            zittend(helper, speler, kussens[i].plekken().get(0));         // (the middle of the cushion)
        }
        // Gloeisterkernen x 8..9, z 0
        helper.setBlock(p(8, 0), TechbronFeature.GLOEISTERKERN.get());
        helper.setBlock(p(9, 0), TechbronFeature.GLOEISTERKERN.get());
        // stoves x 0..4, z 4, each with a fed blubje
        for (int i = 0; i < 5; i++) {
            helper.setBlock(p(i, 4), TechbronFeature.BLUBKACHELTJE.get());
            BlubkacheltjeBlockEntity kachel = be(helper, p(i, 4));
            kachel.voer(new ItemStack(ModItems.KAAS_KNABBELS.get(), 2), true);
            kachel.zetBlubje(new ItemStack(SausdierenFeature.SAUSBLUBJE_POTJE.get()));
        }
        // dance floors x 0..8, z 8..10 (kerns at z 8), a disc and a sitting guh on each
        DiscoDynamoBlockEntity[] discos = new DiscoDynamoBlockEntity[3];
        for (int i = 0; i < 3; i++) {
            helper.setBlock(p(1 + 3 * i, 8), TechbronFeature.DISCO_DYNAMO.get());
            discos[i] = be(helper, p(1 + 3 * i, 8));
            discos[i].zetPlaat(new ItemStack(Items.MUSIC_DISC_STAL));
            zittend(helper, speler, discos[i].plekken().get(2));
        }
        helper.succeedWhen(() -> {
            VadsNet k = net(helper, p(0, 0)), g = net(helper, p(8, 0)), b = net(helper, p(0, 4)), d = net(helper, p(1, 8));
            helper.assertTrue(k != g && g != b && b != d && k != d, "four nets");
            for (int i = 0; i < 3; i++) {
                gelijk(helper, 1, kussens[i].guhs(), "a guh on cushion " + i);
                gelijk(helper, 1, discos[i].dansers(), "a dancer on floor " + i);
            }
            gelijk(helper, 3, k.aantal(BronSoort.KNUFFELGENERATOR), "cushions");
            gelijk(helper, BronSoort.KNUFFELGENERATOR.max, k.telt(BronSoort.KNUFFELGENERATOR), "cushions that count");
            gelijk(helper, 2 * VadsGetallen.KNUFFEL_PER_GUH, k.aanbod(), "aanbod of three cushions");
            helper.assertTrue(kussens[0].teltMee() && kussens[1].teltMee() && !kussens[2].teltMee(), "the third cushion does not count");
            gelijk(helper, Snoet.SLAAPT, snoet(helper, p(4, 0)), "the one too many sleeps");
            gelijk(helper, Snoet.WERKT, snoet(helper, p(0, 0)), "the others do not");
            helper.assertTrue(lees(helper, p(5, 1)).contains("Telt niet mee: hooguit 2 Knuffelgeneratoren per opstelling"), "readout of the third cushion: " + lees(helper, p(5, 1)));

            gelijk(helper, 2, g.aantal(BronSoort.GLOEISTERKERN), "kernen");
            gelijk(helper, VadsGetallen.GLOEISTERKERN, g.aanbod(), "aanbod of two Gloeisterkernen");
            gelijk(helper, Snoet.WERKT, snoet(helper, p(8, 0)), "the first kern");
            gelijk(helper, Snoet.SLAAPT, snoet(helper, p(9, 0)), "the second kern sleeps");
            gelijk(helper, 15, helper.getBlockState(p(8, 0)).getLightEmission(), "the kern shines");
            helper.assertTrue(lees(helper, p(9, 0)).contains("Telt niet mee: hooguit 1 Gloeisterkern per opstelling")
                    && lees(helper, p(9, 0)).contains("Dit geeft 200 vadskracht"), "readout of the second kern: " + lees(helper, p(9, 0)));

            gelijk(helper, 5, b.aantal(BronSoort.BLUBKACHELTJE), "stoves");
            gelijk(helper, BronSoort.BLUBKACHELTJE.max * VadsGetallen.BLUBKACHELTJE, b.aanbod(), "aanbod of five stoves");
            gelijk(helper, Snoet.SLAAPT, snoet(helper, p(4, 4)), "the fifth stove sleeps");
            helper.assertTrue(lees(helper, p(4, 4)).contains("Telt niet mee: hooguit 4 Blubkacheltjes per opstelling"), "readout of the fifth stove: " + lees(helper, p(4, 4)));

            gelijk(helper, 3, d.aantal(BronSoort.DISCO_DYNAMO), "floors");
            gelijk(helper, BronSoort.DISCO_DYNAMO.max * VadsGetallen.DISCO_PER_GUH, d.aanbod(), "aanbod of three floors with a dancer each");
            helper.assertTrue(!discos[2].teltMee(), "the third floor does not count");
            helper.assertTrue(lees(helper, p(8, 10)).contains("Telt niet mee: hooguit 2 Disco-dynamo's per opstelling"), "readout of the third floor: " + lees(helper, p(8, 10)));
            weg(helper, speler);
        });
    }

    // =====================================================================================================================
    // Knabbelbatterij
    // =====================================================================================================================

    /** The charge goes onto the item when the battery is broken, and back into the block when the item is placed. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void techbronKnabbelbatterijHoudtZijnLading(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer speler = speler(helper, 1, 1);
        BlockPos plek = p(5, 5);
        helper.setBlock(plek, TechbronFeature.KNABBELBATTERIJ.get());
        KnabbelbatterijBlock.Kern batterij = be(helper, plek);
        gelijk(helper, VadsGetallen.BATTERIJ, batterij.vadsMax(), "what it holds");
        // empty: an ordinary item that stacks
        List<ItemStack> leeg = Block.getDrops(helper.getBlockState(plek), level, helper.absolutePos(plek), batterij);
        helper.assertTrue(leeg.size() == 1 && ItemStack.isSameItemSameComponents(leeg.get(0), new ItemStack(TechbronFeature.KNABBELBATTERIJ_ITEM.get())),
                "an empty battery drops as a plain item: " + leeg);
        // charged: broken, the charge is on the item
        batterij.zetInhoud(5000);
        level.destroyBlock(helper.absolutePos(plek), true);
        List<ItemEntity> gevallen = level.getEntitiesOfClass(ItemEntity.class, helper.getBounds(), e -> e.getItem().is(TechbronFeature.KNABBELBATTERIJ_ITEM.get()));
        helper.assertTrue(gevallen.size() == 1, "the battery drops");
        ItemStack item = gevallen.get(0).getItem().copy();
        gevallen.get(0).discard();
        gelijk(helper, 5000L, item.getOrDefault(TechbronFeature.LADING.get(), 0L), "the charge on the item");
        helper.assertTrue(ItemStack.isSameItemSameComponents(item, TechbronFeature.batterij(5000)), "TechbronFeature.batterij makes the same item");
        helper.assertTrue(TechbronFeature.batterij(0).get(TechbronFeature.LADING.get()) == null, "an empty one has no component");
        // placed by a player somewhere else: the charge is back in the block, the gauge shows it
        BlockPos nieuw = p(9, 9);
        speler.setItemInHand(InteractionHand.MAIN_HAND, item);
        InteractionResult r = item.useOn(new UseOnContext(speler, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(nieuw.below())).add(0, 0.5, 0), Direction.UP, helper.absolutePos(nieuw.below()), false)));
        helper.assertTrue(r.consumesAction(), "the battery was placed: " + r);
        helper.assertBlockPresent(TechbronFeature.KNABBELBATTERIJ.get(), nieuw);
        KnabbelbatterijBlock.Kern terug = be(helper, nieuw);
        gelijk(helper, 5000L, terug.vadsInhoud(), "the charge is back in the block");
        gelijk(helper, BatterijBlock.lading(5000, VadsGetallen.BATTERIJ), helper.getBlockState(nieuw).getValue(BatterijBlock.LADING), "the gauge");
        // and it works as a battery: a machine next to it runs on it, without any source
        TestMachineBlock.Kern machine = machine(helper, nieuw.east());
        VadsNet net = net(helper, nieuw);
        gelijk(helper, 5000L, net.buffer(), "the net's buffer");
        helper.assertTrue(net.draait(), "the net runs on the battery");
        helper.assertTrue(lees(helper, nieuw).contains("Opgeslagen: 5000/18000 vadskracht"), "readout: " + lees(helper, nieuw));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(machine.heeftKracht() && terug.vadsInhoud() < 5000, "the machine uses the charge that was carried here: " + terug.vadsInhoud());
            weg(helper, speler);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the Guhrad per variant
    // =====================================================================================================================

    static GuhWheelBlockEntity rad(GameTestHelper helper, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        BlockState state = ModBlocks.GUH_WHEEL.get().defaultBlockState();
        helper.setBlock(pos, state);
        BlockPos abs = helper.absolutePos(pos);
        state.getBlock().setPlacedBy(level, abs, level.getBlockState(abs), null, ItemStack.EMPTY);
        return (GuhWheelBlockEntity) level.getBlockEntity(abs);
    }

    static CompoundTag guhTag(GuhVariant variant) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Variant", variant.id());
        return tag;
    }

    /** Every story guh does its rounds its own way: the numbers (data), the style, and the line in the readout. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void techbronGuhradPerVariant(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer speler = speler(helper, 1, 13);
        gelijk(helper, GuhradStijl.GEWOON, GuhradStijl.van(guhTag(GuhVariant.NORMAL)), "an ordinary guh");
        gelijk(helper, GuhradStijl.HARD, GuhradStijl.van(GuhVariant.BALTOGUH.id()), "the Baltoguh");
        gelijk(helper, GuhradStijl.ZWEEFT, GuhradStijl.van(GuhVariant.MEWTWO.id()), "Guhtwo");
        gelijk(helper, GuhradStijl.DUBBEL, GuhradStijl.van(GuhVariant.STITCH626.id()), "the 626-guh");
        gelijk(helper, GuhradStijl.SJOUWT, GuhradStijl.van(GuhVariant.SAM_GUH.id()), "Sam-guh");
        gelijk(helper, GuhradStijl.FLADDERT, GuhradStijl.van(GuhVariant.GUHSHI.id()), "Guhshi");
        helper.assertTrue(GuhradStijl.ZWEEFT.zweeft && !GuhradStijl.HARD.zweeft && GuhradStijl.HARD.tempo > 1f && GuhradStijl.DUBBEL.tempo == 2f, "how they look");
        for (GuhVariant verhaal : List.of(GuhVariant.BALTOGUH, GuhVariant.MEWTWO, GuhVariant.STITCH626, GuhVariant.SAM_GUH, GuhVariant.GUHSHI)) {
            GuhradKracht.Kracht kracht = GuhradKracht.van(verhaal);
            helper.assertTrue(kracht.kracht() >= 20 && kracht.kracht() <= 25 && kracht.blij() == 25, verhaal + " gives 20-25: " + kracht);
            helper.assertTrue(GuhradStijl.van(verhaal.id()) != GuhradStijl.GEWOON && NlTekst.has("gui.guhs.techbron.guhrad." + GuhradStijl.van(verhaal.id()).id()),
                    verhaal + " has a style with a line");
        }
        gelijk(helper, 2 * VadsGetallen.GUHRAD, GuhradKracht.van(GuhVariant.STITCH626).kracht(), "the 626-guh counts double");

        GuhWheelBlockEntity guhtwo = rad(helper, p(2, 2)), gewoon = rad(helper, p(7, 2)), blij = rad(helper, p(12, 2));
        guhtwo.insert(guhTag(GuhVariant.MEWTWO));
        gewoon.insert(guhTag(GuhVariant.NORMAL));
        CompoundTag vrolijk = guhTag(GuhVariant.NORMAL);
        CompoundTag data = new CompoundTag();
        data.putLong(Band.BLIJ_TOT, level.getGameTime() + 6000);
        vrolijk.put("NeoForgeData", data);
        // through the block, as a player does it (the overlay message is said, the guh item is used up)
        speler.setItemInHand(InteractionHand.MAIN_HAND, PickedUpGuhItem.of(vrolijk));
        gelijk(helper, InteractionResult.SUCCESS, klik(helper, speler, p(12, 2)), "a picked-up guh goes into the wheel");
        helper.assertTrue(blij.hasGuh() && blij.isBlij() && speler.getMainHandItem().isEmpty(), "the happy guh runs");

        gelijk(helper, GuhradStijl.ZWEEFT, guhtwo.stijl(), "the wheel's style");
        List<String> regels = lees(helper, p(2, 2));
        helper.assertTrue(regels.contains("Dit geeft 25 vadskracht") && regels.contains("Guhtwo rent niet. Hij zweeft, en het rad draait vanzelf mee"), "Guhtwo's readout: " + regels);
        helper.assertFalse(guhtwo.kanBlijer() || regels.stream().anyMatch(r -> r.startsWith("Tip:")), "Guhtwo gives the same happy or not: no tip");
        regels = lees(helper, p(7, 2));
        helper.assertTrue(gewoon.kanBlijer() && regels.contains("Tip: een guh die net op een Knuffelgenerator lag is blij, en rent harder"), "an ordinary guh's readout has the tip: " + regels);
        regels = lees(helper, p(12, 2));
        helper.assertTrue(regels.contains("Dit geeft 15 vadskracht") && regels.contains("Een blij guhtje: het rent extra hard, njeg!")
                && regels.stream().noneMatch(r -> r.startsWith("Tip:")), "a happy guh's readout: " + regels);
        guhtwo.takeOut();
        helper.assertTrue(lees(helper, p(2, 2)).contains("Er rent geen guh in dit rad") && guhtwo.stijl() == GuhradStijl.GEWOON, "an empty wheel");
        weg(helper, speler);
        helper.succeed();
    }

    // =====================================================================================================================
    // recipes, tags
    // =====================================================================================================================

    /** The five recipes exist and make the right block; the tags hold what the blocks ask for; the readout works on every block. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void techbronReceptenEnTags(GameTestHelper helper) {
        var recepten = helper.getLevel().getServer().getRecipeManager();
        for (var blok : List.of(TechbronFeature.KNUFFELGENERATOR_ITEM, TechbronFeature.DISCO_DYNAMO_ITEM, TechbronFeature.BLUBKACHELTJE_ITEM,
                TechbronFeature.GLOEISTERKERN_ITEM, TechbronFeature.KNABBELBATTERIJ_ITEM)) {
            var recept = recepten.byKey(ResourceKey.create(Registries.RECIPE, blok.getId()));
            helper.assertTrue(recept.isPresent(), "a recipe for " + blok.getId());
        }
        helper.assertTrue(new ItemStack(SausdierenFeature.SAUSBLUBJE_POTJE.get()).is(TechbronFeature.BLUBJE_IN_POT), "the Sausblubje in a jar fills the stove");
        helper.assertTrue(new ItemStack(ModItems.KAAS_KNABBELS.get()).is(TechbronFeature.BLUBVOER), "kaasknabbels feed it");
        helper.assertTrue(new ItemStack(ModItems.MUSIC_DISC_ZE_HANGEN.get()).is(TechbronFeature.ZELDZAME_PLAAT)
                && !new ItemStack(Items.MUSIC_DISC_CAT).is(TechbronFeature.ZELDZAME_PLAAT), "only the mod's disc is rare");
        for (var blok : List.of(TechbronFeature.KNUFFELGENERATOR, TechbronFeature.DISCO_DYNAMO, TechbronFeature.BLUBKACHELTJE, TechbronFeature.GLOEISTERKERN,
                TechbronFeature.KNABBELBATTERIJ, TechbronFeature.KUSSEN_DEEL, TechbronFeature.VLOER_DEEL)) {
            helper.assertTrue(blok.get().defaultBlockState().is(VadsKracht.TOON), blok.getId() + " is in #guhs:vadskracht");
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // the flag "Aangebrande Mika verslagen"
    // =====================================================================================================================

    /** An Aangebrande Mika dies: everybody within 32 blocks has the flag, somebody further away has not. */
    @GuhTest(template = KAMER, batch = BATCH + "_mika", timeoutTicks = 200)
    public static void techbronAangebrandeMikaVerslagen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer dichtbij = speler(helper, 2, 2), ook = speler(helper, 12, 12), ver = speler(helper, 7, 7);
        BlockPos midden = helper.absolutePos(p(7, 7));
        ook.snapTo(midden.getX() + TechbronGetallen.MIKA_BEREIK - 2, midden.getY(), midden.getZ() + 0.5);
        ver.snapTo(midden.getX() + TechbronGetallen.MIKA_BEREIK + 6, midden.getY(), midden.getZ() + 0.5);
        for (ServerPlayer speler : List.of(dichtbij, ook, ver)) {
            helper.assertFalse(AangebrandeMika.verslagen(speler), "nobody has the flag yet");
        }
        AangebrandeMikaEntity mika = SpiesburchtFeature.AANGEBRANDE_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
        mika.snapTo(midden.getX() + 0.5, midden.getY(), midden.getZ() + 0.5);
        level.addFreshEntity(mika);
        mika.finishSpawning();
        helper.runAfterDelay(5, () -> {
            for (int i = 0; i < 3 && mika.isAlive(); i++) {
                mika.invulnerableTime = 0;
                mika.hurtServer(level, level.damageSources().playerAttack(dichtbij), 1000f);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(mika.isDeadOrDying(), "the Mika is down");
            helper.assertTrue(AangebrandeMika.verslagen(dichtbij), "the one who hit him has the flag");
            helper.assertTrue(AangebrandeMika.verslagen(ook), "somebody else within " + TechbronGetallen.MIKA_BEREIK + " blocks has it too");
            helper.assertFalse(AangebrandeMika.verslagen(ver), "somebody further away has not");
            helper.assertTrue(GuhQuests.saved(dichtbij).getBooleanOr(AangebrandeMika.SLEUTEL, false), "it is the key in GuhQuests.saved");
            level.getEntitiesOfClass(Entity.class, helper.getBounds().inflate(6), e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
            weg(helper, dichtbij, ook, ver);
        });
    }

    /** Players who beat him before the flag existed get it at login; having summoned him only counts the very first time. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void techbronAangebrandeMikaVanVroeger(GameTestHelper helper) {
        ServerPlayer nieuw = speler(helper, 0, 0), versloeg = speler(helper, 0, 0), riepOp = speler(helper, 0, 0);
        // a new player: nothing; summoning him later and logging in again gives nothing; beating him does
        helper.assertFalse(AangebrandeMika.vulAan(nieuw), "a new player has not beaten him");
        helper.assertTrue(GuhQuests.saved(nieuw).getBooleanOr(AangebrandeMika.GEKEKEN, false), "the first look is remembered");
        GidsFeature.grant(nieuw, AangebrandeMika.OPGEROEPEN);
        helper.assertTrue(GidsFeature.heeft(nieuw, AangebrandeMika.OPGEROEPEN), "the mock player has the summon advancement");
        helper.assertFalse(AangebrandeMika.vulAan(nieuw), "summoning him after the update is not beating him");
        GuhAdvancements.grant(nieuw, "aangebrande_mika_verslagen");
        helper.assertTrue(AangebrandeMika.vulAan(nieuw) && AangebrandeMika.verslagen(nieuw), "the hidden advancement of his death gives the flag at any login");
        // from before the update: the hidden advancement of his death, or (once) the advancement the orchestrator named
        GuhAdvancements.grant(versloeg, "aangebrande_mika_verslagen");
        helper.assertFalse(AangebrandeMika.verslagen(versloeg), "not before the login");
        helper.assertTrue(AangebrandeMika.vulAan(versloeg), "beaten before the update: the flag at login");
        helper.assertTrue(GuhQuests.saved(riepOp).getBooleanOr(AangebrandeMika.GEKEKEN, false), "the login of the mock player itself already had the first look");
        GuhQuests.saved(riepOp).remove(AangebrandeMika.GEKEKEN);         // a player from before the update: never looked at yet
        GidsFeature.grant(riepOp, AangebrandeMika.OPGEROEPEN);
        helper.assertTrue(AangebrandeMika.vulAan(riepOp), "guhs:barbecuether/aangebrande_mika from before the update: the flag at the first login");
        // the flag stays, and the helper is the key
        GuhQuests.saved(riepOp).remove(AangebrandeMika.SLEUTEL);
        helper.assertFalse(AangebrandeMika.vulAan(riepOp), "(only at the FIRST login: afterwards the summon advancement is not looked at again)");
        AangebrandeMika.zet(riepOp);
        helper.assertTrue(AangebrandeMika.verslagen(riepOp), "zet / verslagen");
        weg(helper, nieuw, versloeg, riepOp);
        helper.succeed();
    }
}
