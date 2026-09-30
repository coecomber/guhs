package nl.juiced.guhs.feature.beroepen;

import java.util.List;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Binnenkort;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.beroepen.BeroepenVoortgang.Beroep;
import nl.juiced.guhs.feature.boerderij.BoerderijFeature;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the beroepen: the four one-time jobs from start to reward (and no reward twice), one helper at a time,
 * the Knabbeldief never does anything but giggle and run, the street piece and Bob's village layout, the sources of the
 * clothing, the roles and the loaned items. (Templates beroepen_test_*: a floor at helper y = 1.)
 */
public class BeroepenGameTests {
    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, int x, int z) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        BeroepenVoortgang.wis(p);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, int x, int z) {
        GuhNpcEntity n = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(x, 2, z));
        n.setKind(kind);
        return n;
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

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id(name.contains("/") ? name : "quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static void beloond(GameTestHelper helper, ServerPlayer p, Beroep beroep) {
        helper.assertTrue(BeroepenVoortgang.klaar(p, beroep), beroep + ": done");
        for (GuhClothes c : beroep.kleding()) {
            helper.assertTrue(count(p, ModItems.clothingItem(c)) == 1, beroep + ": exactly one " + c + ", not " + count(p, ModItems.clothingItem(c)));
        }
        helper.assertTrue(advancement(p, beroep.advancement()) && advancement(p, "grote_guhspelen/" + beroep.advancement()), beroep + ": advancements");
    }

    // --- Blusguh -------------------------------------------------------------------------------------------------------

    @GuhTest(template = "beroepen_test_brandweer", batch = "beroepen_brandweer", timeoutTicks = 100)
    public static void beroepenBrandweerBlussenEnGuhtjeRedden(GameTestHelper helper) {
        ServerPlayer p = player(helper, 2, 12);
        ServerPlayer q = player(helper, 3, 12);
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.BRANDWEERGUH, 2, 2);
        BlockPos pit1 = helper.absolutePos(new BlockPos(5, 2, 5)), pit2 = helper.absolutePos(new BlockPos(9, 2, 5));
        try {
            Brandweer.ROLE.talk(npc, p);
            helper.assertTrue(BeroepenVoortgang.stap(p, Beroep.BRANDWEER) == 1, "step 1: blow them out");
            helper.assertTrue(helper.getLevel().getBlockState(pit1).getValue(MarshmallowvuurBlock.VUUR) == 3
                    && helper.getLevel().getBlockState(pit2).getValue(MarshmallowvuurBlock.VUUR) == 3, "both fires flared up");
            helper.assertTrue(count(p, BeroepenFeature.GUH_BRANDSLANG.get()) == 1, "the loaned hose");
            helper.assertTrue(Features.isLoaned(new ItemStack(BeroepenFeature.GUH_BRANDSLANG.get())), "the hose is loaned");
            // someone else has to wait
            Brandweer.ROLE.talk(npc, q);
            helper.assertTrue(BeroepenVoortgang.stap(q, Beroep.BRANDWEER) == 0 && count(q, BeroepenFeature.GUH_BRANDSLANG.get()) == 0, "one helper at a time");
            // the hose really sprays the fire you look at: stand before pit 1 and spray
            BlockPos voor = helper.absolutePos(new BlockPos(5, 2, 8));
            p.snapTo(voor.getX() + 0.5, voor.getY(), voor.getZ() + 0.5);
            p.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pit1));
            for (int t = 0; t <= BrandslangItem.BLUS_TICKS * 3; t++) {
                BrandslangItem.spuit(p, t);
            }
            helper.assertTrue(helper.getLevel().getBlockState(pit1).getValue(MarshmallowvuurBlock.VUUR) == 0, "sprayed out: "
                    + helper.getLevel().getBlockState(pit1));
            Brandweer.controleer(npc);
            helper.assertTrue(BeroepenVoortgang.stap(p, Beroep.BRANDWEER) == 1 && Brandweer.brandend(npc) == 1, "one still burns");
            while (MarshmallowvuurBlock.blus(helper.getLevel(), pit2, p)) {
                // (pssst)
            }
            Brandweer.controleer(npc);
            helper.assertTrue(BeroepenVoortgang.stap(p, Beroep.BRANDWEER) == 2, "all out: the guhtje in the tree");
            GuhEntity g = Brandweer.guhtje(npc);
            helper.assertTrue(g != null && Brandweer.isBoomguhtje(g) && g.blockPosition().equals(helper.absolutePos(new BlockPos(11, 5, 12))),
                    "the guhtje sits on the branch: " + (g == null ? null : g.blockPosition()));
            helper.assertTrue(!Brandweer.red(q, g), "only the helper can get it down");
            helper.assertTrue(Brandweer.red(p, g), "rescued");
            beloond(helper, p, Beroep.BRANDWEER);
            helper.assertTrue(count(p, BeroepenFeature.GUH_BRANDSLANG.get()) == 0, "Blusguh took his hose back");
            helper.assertTrue(g.distanceTo(npc) < 3, "the guhtje is down by Blusguh");
            Brandweer.ROLE.talk(npc, p);
            helper.assertTrue(count(p, ModItems.clothingItem(GuhClothes.FIREFIGHTER_HELMET)) == 1
                    && helper.getLevel().getBlockState(pit1).getValue(MarshmallowvuurBlock.VUUR) == 0, "one-time: afterwards only thanks");
        } finally {
            leave(helper, p, q);
        }
        helper.succeed();
    }

    // --- Vahoegsma -----------------------------------------------------------------------------------------------------

    @GuhTest(template = "beroepen_test_politie", batch = "beroepen_politie", timeoutTicks = 100)
    public static void beroepenPolitieVolgHetSpoor(GameTestHelper helper) {
        ServerPlayer p = player(helper, 4, 2);
        ServerPlayer q = player(helper, 5, 2);
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.POLITIEGUH, 4, 4);
        try {
            Politie.ROLE.talk(npc, p);
            helper.assertTrue(BeroepenVoortgang.stap(p, Beroep.POLITIE) == 1, "step 1: follow the trail");
            BlockPos buit = Politie.buit(npc);
            helper.assertTrue(helper.absolutePos(new BlockPos(20, 2, 20)).equals(buit)
                    && helper.getLevel().getBlockState(buit).is(BeroepenFeature.KNABBELBUIT.get()), "the sack in the hiding place: " + buit);
            List<BlockPos> spoor = Politie.spoor(npc);
            helper.assertTrue(spoor.size() >= 10, "a trail of paw prints: " + spoor.size());
            for (BlockPos s : spoor) {
                helper.assertTrue(helper.getLevel().getBlockState(s).is(BeroepenFeature.POOTAFDRUK.get()), "a print at " + s);
            }
            int wall = helper.absolutePos(new BlockPos(0, 2, 11)).getZ();
            BlockPos past = spoor.stream().filter(s -> s.getZ() == wall).findFirst().orElse(null);
            helper.assertTrue(past == null || helper.relativePos(past).getX() >= 19, "the trail goes round the wall, not through it");
            KnabbeldiefMikaEntity mika = Politie.mika(npc);
            helper.assertTrue(mika != null && mika.distanceToSqr(Vec3.atCenterOf(buit)) < 4, "the Knabbeldief sits by the sack");
            // it never does anything but giggle and run
            float health = p.getHealth();
            helper.assertTrue(!mika.doHurtTarget(helper.getLevel(), p) && p.getHealth() == health, "a Mika never hurts");
            helper.assertTrue(!mika.hurtServer(helper.getLevel(), p.damageSources().playerAttack(p), 5f), "and can't be hurt");
            mika.schrik(p);
            helper.assertTrue(mika.isGevlucht(), "caught: it runs");
            // someone else can't take the sack; the detective can
            helper.assertTrue(!Politie.gevonden(helper.getLevel(), buit, q), "not your case");
            helper.assertTrue(Politie.gevonden(helper.getLevel(), buit, p), "found");
            helper.assertTrue(BeroepenVoortgang.stap(p, Beroep.POLITIE) == 2, "step 2: tell Vahoegsma");
            helper.assertTrue(helper.getLevel().getBlockState(buit).is(BeroepenFeature.VERSTOPPLEK.get()), "the hiding place is free again");
            for (BlockPos s : spoor) {
                helper.assertTrue(!helper.getLevel().getBlockState(s).is(BeroepenFeature.POOTAFDRUK.get()), "swept up: " + s);
            }
            Politie.ROLE.talk(npc, p);
            beloond(helper, p, Beroep.POLITIE);
            Politie.ROLE.talk(npc, p);
            helper.assertTrue(count(p, ModItems.clothingItem(GuhClothes.POLICE_CAP)) == 1 && Politie.buit(npc) == null, "one-time");
            // the next detective gets a new case
            Politie.ROLE.talk(npc, q);
            helper.assertTrue(BeroepenVoortgang.stap(q, Beroep.POLITIE) == 1 && Politie.buit(npc) != null, "a new case for the next one");
            Politie.stop(npc);
            helper.assertTrue(helper.getLevel().getBlockState(buit).is(BeroepenFeature.VERSTOPPLEK.get()), "stopping cleans up");
        } finally {
            leave(helper, p, q);
        }
        helper.succeed();
    }

    // --- Snotneus-guh --------------------------------------------------------------------------------------------------

    @GuhTest(template = "beroepen_test_apotheek", batch = "beroepen_apotheek", timeoutTicks = 100)
    public static void beroepenApotheekSnotjeWordtBeter(GameTestHelper helper) {
        ServerPlayer p = player(helper, 4, 4);
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.APOTHEKERGUH, 2, 2);
        GuhEntity snotje = helper.spawn(ModEntities.GUH.get(), new BlockPos(9, 2, 9));
        snotje.setNoAi(true);
        snotje.getPersistentData().putBoolean(Apotheek.SNOTJE, false);
        BlockPos plant = helper.absolutePos(new BlockPos(6, 2, 6)), ketel = helper.absolutePos(new BlockPos(3, 2, 8));
        try {
            Apotheek.ROLE.talk(npc, p);
            helper.assertTrue(BeroepenVoortgang.stap(p, Beroep.APOTHEEK) == 1 && snotje.getPersistentData().getBooleanOr(Apotheek.SNOTJE, false),
                    "step 1: Snotje is snotterig");
            helper.assertTrue(count(p, BoerderijFeature.KAASMELK.get()) == 1, "a bottle of kaasmelk from the fridge");
            helper.assertTrue(!MengketelBlock.meng(helper.getLevel(), ketel, p), "no drankje without snotkruidjes");
            for (int i = 0; i < Apotheek.KRUIDJES; i++) {
                helper.assertTrue(SnotkruidBlock.pluk(helper.getLevel(), plant, p), "picked " + i);
                helper.assertTrue(!SnotkruidBlock.pluk(helper.getLevel(), plant, p), "it has to grow back first");
                helper.getLevel().setBlock(plant, helper.getLevel().getBlockState(plant).setValue(SnotkruidBlock.AGE, SnotkruidBlock.MAX), 3);
            }
            helper.assertTrue(count(p, BeroepenFeature.SNOTKRUIDJE.get()) == Apotheek.KRUIDJES, "three snotkruidjes");
            helper.assertTrue(MengketelBlock.meng(helper.getLevel(), ketel, p), "blub blub");
            helper.assertTrue(count(p, BeroepenFeature.KAASMELKDRANKJE.get()) == 1 && count(p, BeroepenFeature.SNOTKRUIDJE.get()) == 0
                    && count(p, BoerderijFeature.KAASMELK.get()) == 0, "one kaasmelkdrankje, the herbs and the kaasmelk are in it");
            ItemStack drankje = p.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(BeroepenFeature.KAASMELKDRANKJE.get())).findFirst().orElseThrow();
            helper.assertTrue(!Apotheek.snotje(p, snotje, ItemStack.EMPTY), "an empty hand: he only sniffs");
            helper.assertTrue(Apotheek.snotje(p, snotje, drankje), "he drinks it");
            helper.assertTrue(!snotje.getPersistentData().getBooleanOr(Apotheek.SNOTJE, false) && BeroepenVoortgang.stap(p, Beroep.APOTHEEK) == 2,
                    "Snotje is better, step 2");
            helper.assertTrue(count(p, Items.GLASS_BOTTLE) == 1 && count(p, BeroepenFeature.KAASMELKDRANKJE.get()) == 0, "the bottle stays");
            Apotheek.ROLE.talk(npc, p);
            beloond(helper, p, Beroep.APOTHEEK);
            Apotheek.ROLE.talk(npc, p);
            helper.assertTrue(count(p, ModItems.clothingItem(GuhClothes.STETHOSCOPE)) == 1, "one-time");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    // --- Bob -----------------------------------------------------------------------------------------------------------

    @GuhTest(template = "beroepen_test_bouw", batch = "beroepen_bouw", timeoutTicks = 100)
    public static void beroepenBouwDeVlagInTop(GameTestHelper helper) {
        ServerPlayer p = player(helper, 5, 7);
        ServerPlayer q = player(helper, 5, 9);
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.BOUWVAKKERGUH, 2, 2);
        try {
            Bouw.ROLE.talk(npc, p);
            helper.assertTrue(BeroepenVoortgang.stap(p, Beroep.BOUW) == 1 && Bouw.plekken(npc).size() == 8, "step 1: bring planks and lunch");
            p.getInventory().add(new ItemStack(Items.OAK_PLANKS, 10));
            p.getInventory().add(new ItemStack(Items.BIRCH_PLANKS, 6));
            p.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 7));
            Bouw.ROLE.talk(npc, p);
            helper.assertTrue(BeroepenVoortgang.stap(p, Beroep.BOUW) == 1 && count(p, Items.OAK_PLANKS) == 10, "one knabbel short: nothing taken");
            p.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 3));
            Bouw.ROLE.talk(npc, p);
            helper.assertTrue(BeroepenVoortgang.stap(p, Beroep.BOUW) == 2, "step 2: the roof");
            helper.assertTrue(count(p, Items.OAK_PLANKS) + count(p, Items.BIRCH_PLANKS) == 0 && count(p, ModItems.KAAS_KNABBELS.get()) == 2,
                    "16 planks and 8 knabbels taken");
            helper.assertTrue(count(p, BeroepenFeature.DAKPAN_ITEM.get()) == 8, "a dakpan per ghost tile");
            helper.assertTrue(Features.isLoaned(new ItemStack(BeroepenFeature.DAKPAN_ITEM.get())), "the dakpannen are loaned");
            ItemStack pannen = p.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(BeroepenFeature.DAKPAN_ITEM.get())).findFirst().orElseThrow();
            helper.assertTrue(!DakpanItem.leg(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)), p, pannen), "only on a ghost tile");
            List<BlockPos> plekken = Bouw.plekken(npc);
            for (int i = 0; i < plekken.size(); i++) {
                helper.assertTrue(DakpanItem.leg(helper.getLevel(), plekken.get(i), p, pannen), "laid " + i);
                helper.assertTrue(BeroepenVoortgang.klaar(p, Beroep.BOUW) == (i == plekken.size() - 1), "done only with the last one (" + i + ")");
            }
            beloond(helper, p, Beroep.BOUW);
            BlockPos vlag = Bouw.vlagPlek(npc);
            helper.assertTrue(vlag != null && helper.getLevel().getBlockState(vlag).is(Blocks.ORANGE_BANNER), "de vlag in top");
            helper.assertTrue(count(p, BeroepenFeature.DAKPAN_ITEM.get()) == 0, "no tiles left over");
            // the next builder: the Mika's "borrowed" all the tiles in the night
            Bouw.ROLE.talk(npc, q);
            helper.assertTrue(Bouw.open(npc) == 8 && BeroepenVoortgang.stap(q, Beroep.BOUW) == 1, "the roof is open again for the next one");
            helper.assertTrue(!helper.getLevel().getBlockState(vlag).is(Blocks.ORANGE_BANNER), "the flag came down");
            Bouw.ROLE.talk(npc, p);
            helper.assertTrue(count(p, ModItems.clothingItem(GuhClothes.BUILDER_HELMET)) == 1, "one-time");
        } finally {
            leave(helper, p, q);
        }
        helper.succeed();
    }

    // --- all four ------------------------------------------------------------------------------------------------------

    @GuhTest(template = "beroepen_test_brandweer", batch = "beroepen_alle")
    public static void beroepenAlleVierEnDeBronnen(GameTestHelper helper) {
        ServerPlayer p = player(helper, 2, 2);
        try {
            for (Beroep b : Beroep.values()) {
                helper.assertTrue(BeroepenVoortgang.rondAf(p, b, null), "done " + b);
                helper.assertTrue(!BeroepenVoortgang.rondAf(p, b, null), "never twice " + b);
            }
            helper.assertTrue(advancement(p, "beroepen_alle") && advancement(p, "grote_guhspelen/beroepen_alle"), "Guh van alle markten");
            for (Beroep b : Beroep.values()) {
                for (GuhClothes c : b.kleding()) {
                    helper.assertTrue(count(p, ModItems.clothingItem(c)) == 1, "one " + c);
                    helper.assertTrue(b.bron().equals(KledingBronnen.bron(c)), c + " comes from " + b.bron() + ", not " + KledingBronnen.bron(c));
                }
            }
            for (GuhNpcEntity.Kind k : List.of(GuhNpcEntity.Kind.BRANDWEERGUH, GuhNpcEntity.Kind.POLITIEGUH, GuhNpcEntity.Kind.APOTHEKERGUH,
                    GuhNpcEntity.Kind.BOUWVAKKERGUH)) {
                helper.assertTrue(Features.role(k) != null && Features.role(k) != Binnenkort.ROLE && Features.role(k) == BeroepenFeature.role(k),
                        "a real role for " + k);
            }
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    // --- the buildings -------------------------------------------------------------------------------------------------

    private static int blocks(StructureTemplate t, Block block) {
        return t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block, true).size();
    }

    @GuhTest(template = "beroepen_test_bouw", batch = "beroepen_gebouwen")
    public static void beroepenDeStraatEnHetHalveHuisje(GameTestHelper helper) {
        var templates = helper.getLevel().getStructureManager();
        var straat = templates.get(Guhs.id("knuffeldal_stadje/beroepenstraat"));
        helper.assertTrue(straat.isPresent() && straat.get().getSize().equals(new net.minecraft.core.Vec3i(23, 44, 133)), "the Beroepenstraat");
        StructureTemplate s = straat.get();
        var jigsaws = s.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW, true);
        helper.assertTrue(jigsaws.size() == 1 && jigsaws.get(0).pos().equals(new BlockPos(0, 4, 40))
                && jigsaws.get(0).nbt().getStringOr("name", "").equals("guhs:knuffeldal_straat_vrij")
                && jigsaws.get(0).state().getValue(JigsawBlock.ORIENTATION) == FrontAndTop.WEST_UP, "its jigsaw on the west side: " + jigsaws);
        helper.assertTrue(blocks(s, BeroepenFeature.MARSHMALLOWVUUR.get()) == 5 && blocks(s, BeroepenFeature.GUHTJEPLEK.get()) == 1
                && blocks(s, BeroepenFeature.KLUISPLEK.get()) == 1 && blocks(s, BeroepenFeature.VERSTOPPLEK.get()) == 6
                && blocks(s, BeroepenFeature.MENGKETEL.get()) == 1 && blocks(s, BeroepenFeature.SNOTKRUID.get()) >= 12, "the jobs' things are there");
        var pool = helper.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL).getValue(Guhs.id("knuffeldal_stadje/vrij"));
        helper.assertTrue(pool != null && pool.size() == 1
                && pool.getShuffledTemplates(net.minecraft.util.RandomSource.create(1)).get(0).toString().contains("beroepenstraat"),
                "the town's free street pool holds the Beroepenstraat");
        var dorp = templates.get(Guhs.id("guh_village/layout_c"));
        helper.assertTrue(dorp.isPresent() && blocks(dorp.get(), BeroepenFeature.DAKPLEK.get()) == 16, "Bob's roof: 16 ghost tiles");
        var dorpen = helper.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL).getValue(Guhs.id("guh_village/start"));
        helper.assertTrue(dorpen != null && dorpen.size() == 3, "three village layouts");
        helper.succeed();
    }
}
