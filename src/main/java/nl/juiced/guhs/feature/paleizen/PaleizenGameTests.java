package nl.juiced.guhs.feature.paleizen;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.boerderij.BoerderijDier;
import nl.juiced.guhs.feature.boerderij.BoerderijFeature;
import nl.juiced.guhs.feature.boerderij.GuhVoerbakBlock;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.spiesburcht.BurchtStructure;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Herstel;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GegarandeerdPlacement;

/**
 * bbq2 (paleizen): the three Mika palaces and their questlines. The test server has no Guhbarbecuether, so a palace is a
 * try-out copy without its blocks ({@link PaleisProef}): the tests put the few blocks and characters they need at the spots
 * of the template. The tests with a copy run alone in their own batch (a copy of another test nearby would be found too).
 * Template paleizen_test_kamer: 36 x 12 x 36, a floor of houtskoolsteen stenen (helper y 1; things stand at helper y 2).
 */
public class PaleizenGameTests {
    private static final String KAMER = "paleizen_test_kamer";

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        naar(helper, p, at);
        return p;
    }

    private static void naar(GameTestHelper helper, ServerPlayer p, BlockPos at) {
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static AABB kamer(GameTestHelper helper) {
        return new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(36, 12, 36).inflate(2);
    }

    /** Everything a test left in its room (not the players). */
    private static void ruimOp(GameTestHelper helper) {
        for (Entity e : helper.getLevel().getEntitiesOfClass(Entity.class, kamer(helper), e -> !(e instanceof ServerPlayer))) {
            e.discard();
        }
    }

    private static int tel(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    /** All the player has of this item goes into their main hand (the stack the click handlers get). */
    private static ItemStack inHand(ServerPlayer p, Item item) {
        int n = tel(p, item);
        GuhQuests.take(p, item, n);
        p.setItemInHand(InteractionHand.MAIN_HAND, n > 0 ? new ItemStack(item, n) : ItemStack.EMPTY);
        return p.getMainHandItem();
    }

    /** An empty main hand: what was in it goes to the back of the inventory (a given item lands in the hand of an empty inventory). */
    private static void legeHand(ServerPlayer p) {
        ItemStack was = p.getMainHandItem().copyAndClear();
        if (!was.isEmpty()) {
            p.getInventory().setItem(27, was);
        }
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, BlockPos at) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        BlockPos abs = helper.absolutePos(at);
        npc.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(npc);
        return npc;
    }

    // =================================================================================================================
    // what is registered
    // =================================================================================================================

    /**
     * The three questlines, their characters' roles, the structures with their templates and guaranteed sets, the
     * Superkompas entries, the Guhdex page, the source of the hat, the sniffing loot and the riddles' answers.
     */
    @GuhTest(template = "empty", batch = "paleizen")
    public static void paleizenRegistraties(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(OmaQuest.LIJN.stappen() == 4 && StalQuest.LIJN.stappen() == 4 && TolQuest.LIJN.stappen() == 5, "the steps of the three questlines");
        for (String id : List.of("mika_oma", "stalknecht", "tolwachter")) {
            helper.assertTrue(Verhaallijnen.van(id) != null && "barbecue".equals(Verhaallijnen.van(id).groep()), "the questline " + id + " is in the group barbecue");
        }
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.MIKA_OMA);
        helper.assertTrue(NpcRollen.van(npc) == OmaQuest.ROL, "Mika-oma's role");
        npc.setKind(GuhNpcEntity.Kind.STALKNECHTGUH);
        helper.assertTrue(NpcRollen.van(npc) == StalQuest.ROL, "the Stalknecht-guh's role");
        npc.setKind(GuhNpcEntity.Kind.TOLWACHTER_MIKA);
        helper.assertTrue(NpcRollen.van(npc) == TolQuest.ROL, "the Tolwachter-Mika's role");
        npc.discard();
        // the structures, their templates and their two sets (a random spread and the one guaranteed copy)
        var manager = level.getStructureManager();
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        int sector = 1;
        for (String naam : PaleizenFeature.STRUCTUREN) {
            var structuur = Kopieen.structuur(level, naam);
            helper.assertTrue(structuur != null, "the structure guhs:" + naam);
            if (naam.equals(PaleisPlekken.STAL)) {
                helper.assertTrue(manager.get(Guhs.id(naam)).isPresent(), "the template of the stal");
            } else {
                BurchtStructure burcht = (BurchtStructure) structuur;
                helper.assertTrue(manager.get(burcht.tile(0, 0)).isPresent(), "the first tile of " + naam);
                helper.assertTrue(burcht.anchor().equals(naam.equals(PaleisPlekken.BRUGPALEIS) ? PaleisPlekken.Brug.ANKER : PaleisPlekken.Woon.ANKER),
                        "the anchor of " + naam + " is the builder's: " + burcht.anchor());
            }
            helper.assertTrue(sets.getValue(ResourceKey.create(Registries.STRUCTURE_SET, Guhs.id(naam))) != null, "the set of " + naam);
            StructureSet zeker = sets.getValue(ResourceKey.create(Registries.STRUCTURE_SET, Guhs.id(naam + "_gegarandeerd")));
            helper.assertTrue(zeker != null && zeker.placement() instanceof GegarandeerdPlacement g && g.sector() == sector && g.alleenNieuw(),
                    "the guaranteed copy of " + naam + " in sector " + sector + ", in new terrain only");
            sector++;
            String n = naam;
            helper.assertTrue(SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.id().equals("barbecue") && c.structures().contains(n)),
                    naam + " is in the Superkompas tab Barbecue");
        }
        helper.assertTrue(GuhDex.isCreaturePage(GuhVariant.WORSTZWIJNTJE), "the Guhdex page of the Worstzwijntje");
        helper.assertTrue("paleizen".equals(KledingBronnen.bron(GuhClothes.PALEIZEN_MIKAMUTS)), "the source of the Mika hat");
        // the sniffing loot always gives something
        var table = level.getServer().reloadableRegistries().getLootTable(WorstzwijntjeEntity.SNUFFEL_LOOT);
        WorstzwijntjeEntity z = PaleizenFeature.WORSTZWIJNTJE.get().create(level, EntitySpawnReason.TRIGGERED);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .withParameter(LootContextParams.THIS_ENTITY, z).create(LootContextParamSets.GIFT);
        for (int i = 0; i < 20; i++) {
            List<ItemStack> vondst = table.getRandomItems(params);
            helper.assertTrue(vondst.size() == 1 && !vondst.get(0).isEmpty() && vondst.get(0).getCount() <= 4, "a modest find: " + vondst);
        }
        z.discard();
        helper.assertTrue(TolQuest.GOED.length == 6, "six riddles");
        for (int goed : TolQuest.GOED) {
            helper.assertTrue(goed >= 0 && goed < 3, "an answer a, b or c");
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the Worstzwijntje
    // =================================================================================================================

    /**
     * A farm animal like the Guhboerderij's: two kinds of care make it content and it sniffs something up once; a basket
     * lets a young one out; the stable's own animals can't be fed, bred or hurt; it never has an attack.
     */
    @GuhTest(template = KAMER, batch = "paleizen")
    public static void paleizenWorstzwijntje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(3, 2, 3));
        try {
            WorstzwijntjeEntity z = PaleizenFeature.WORSTZWIJNTJE.get().create(level, EntitySpawnReason.TRIGGERED);
            BlockPos at = helper.absolutePos(new BlockPos(6, 2, 6));
            z.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
            level.addFreshEntity(z);
            ItemStack voer = new ItemStack(BoerderijFeature.KNABBELVOER.get());
            helper.assertTrue(!z.isStal() && !z.isOntsnapt() && z.fireImmune() && z.isFood(voer) && z.shouldBeSaved(), "a farm animal by default");
            helper.assertTrue(z.getAttribute(Attributes.ATTACK_DAMAGE) == null, "it has no attack at all");
            z.vergeetZorg();
            z.verzorg(BoerderijDier.Zorg.AAIEN, p);
            helper.assertTrue(!z.isBlij() && !z.productGegeven(), "one kind of care is not enough");
            z.verzorg(BoerderijDier.Zorg.BORSTELEN, p);
            List<ItemEntity> vondst = level.getEntitiesOfClass(ItemEntity.class, z.getBoundingBox().inflate(3));
            helper.assertTrue(z.isBlij() && z.productGegeven() && vondst.size() == 1, "content: it sniffed something up (" + vondst.size() + ")");
            vondst.forEach(Entity::discard);
            z.verzorg(BoerderijDier.Zorg.VOEREN, p);
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, z.getBoundingBox().inflate(3)).isEmpty(), "once a day");
            // the basket
            WorstzwijntjeEntity jong = MandjeItem.laatLos(level, helper.absolutePos(new BlockPos(9, 2, 9)), 0f);
            helper.assertTrue(jong != null && jong.isBaby() && jong.isPersistenceRequired() && !jong.isStal(), "a young one of your own hops out of the basket");
            helper.assertTrue(MandjeItem.laatLos(level, helper.absolutePos(new BlockPos(9, 1, 9)), 0f) == null, "not inside a block");
            helper.assertTrue(z.getBreedOffspring(level, jong) instanceof WorstzwijntjeEntity kind && !kind.isStal(), "its young are farm animals too");
            // one of the stable's own
            WorstzwijntjeEntity stal = StalQuest.stalzwijntje(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(14, 2, 6))), Rotation.NONE, 2);
            WorstzwijntjeEntity big = StalQuest.stalzwijntje(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(18, 2, 6))), Rotation.NONE, 4);
            level.addFreshEntity(stal);
            level.addFreshEntity(big);
            helper.assertTrue(stal.isStal() && stal.stalNr() == 2 && !stal.isBaby() && big.isBaby(), "number 2 is grown, number 4 the piglet");
            helper.assertTrue(!stal.isFood(voer) && !stal.canFallInLove() && !stal.canBeLeashed() && stal.hasHome() && stal.isPersistenceRequired()
                    && stal.isInvulnerableTo(level, level.damageSources().generic()), "the stable's own: not fed, bred, led away or hurt");
            helper.assertTrue(!z.isInvulnerableTo(level, level.damageSources().generic()), "(a farm animal is an ordinary animal)");
        } finally {
            ruimOp(helper);
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // Mika-oma
    // =================================================================================================================

    /**
     * "Soep van Mika-oma" for two players at the same characters: three bowls of soup, each neighbour takes one per player,
     * the knitting comes out of the basket (again when lost) and the basket stays, the hat comes once, and afterwards every
     * third barter with a Nether-Mika gives the ingot back.
     */
    @GuhTest(template = KAMER, batch = "paleizen")
    public static void paleizenOmaQuest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(20, 2, 22)), b = speler(helper, new BlockPos(22, 2, 22));
        try {
            GuhNpcEntity oma = npc(helper, GuhNpcEntity.Kind.MIKA_OMA, new BlockPos(20, 2, 20));
            MopperMikaEntity[] buren = new MopperMikaEntity[4];
            for (int nr = 0; nr < 4; nr++) {
                buren[nr] = PaleizenFeature.MOPPER_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
                buren[nr].setNr(nr);
                BlockPos at = helper.absolutePos(new BlockPos(24 + nr * 2, 2, 20));
                buren[nr].snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
                level.addFreshEntity(buren[nr]);
            }
            helper.assertTrue(buren[0].isMopperaar() && !buren[3].isMopperaar() && buren[2].isInvulnerableTo(level, level.damageSources().generic())
                    && buren[1].getAttribute(Attributes.ATTACK_DAMAGE) == null, "three grumpy neighbours and one who just lives there; harmless and unharmable");
            BlockPos mand = helper.absolutePos(new BlockPos(30, 2, 26));
            level.setBlockAndUpdate(mand, PaleizenFeature.BREIWERK.get().defaultBlockState());
            Item soep = PaleizenFeature.OMASOEP.get(), breiwerk = PaleizenFeature.BREIWERKJE.get();
            Item muts = ModItems.clothingItem(GuhClothes.PALEIZEN_MIKAMUTS);
            // before the quest: a neighbour wants nothing from a stranger, the basket is just a basket
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(soep));
            OmaQuest.klik(a, buren[0], InteractionHand.MAIN_HAND);
            OmaQuest.breiwerk(a, mand);
            helper.assertTrue(tel(a, soep) == 1 && !OmaQuest.LIJN.vlag(a, "soep_0") && tel(a, breiwerk) == 0, "nothing happens before Mika-oma asked");
            a.getInventory().clearContent();
            // step 0: she asks, a says yes
            OmaQuest.ROL.talk(oma, a);
            helper.assertTrue(OmaQuest.LIJN.stap(a) == 0 && OmaQuest.LIJN.begonnen(a), "she asks first");
            OmaQuest.ROL.antwoord(oma, a, 2);
            helper.assertTrue(OmaQuest.LIJN.stap(a) == 0, "'who is grumpy?' is only an explanation");
            OmaQuest.ROL.antwoord(oma, a, 1);
            helper.assertTrue(OmaQuest.LIJN.stap(a) == 1 && tel(a, soep) == 3 && OmaQuest.LIJN.stap(b) == 0, "a has three bowls of soup, b nothing yet");
            helper.assertTrue(OmaQuest.moppertTegen(a, 0) && !OmaQuest.moppertTegen(b, 0) && !OmaQuest.moppertTegen(a, 3), "the grumpy three grumble at a");
            // step 1: an empty hand gets a grumble, soup a slurp; once per neighbour
            legeHand(a);
            OmaQuest.klik(a, buren[0], InteractionHand.MAIN_HAND);
            helper.assertTrue(OmaQuest.soepOver(a) == 3, "no soup, no peace");
            inHand(a, soep);
            OmaQuest.klik(a, buren[0], InteractionHand.MAIN_HAND);
            OmaQuest.klik(a, buren[0], InteractionHand.MAIN_HAND);
            helper.assertTrue(OmaQuest.soepOver(a) == 2 && a.getMainHandItem().getCount() == 2 && !OmaQuest.moppertTegen(a, 0), "Brom-Mika took one bowl, not two");
            OmaQuest.klik(a, buren[3], InteractionHand.MAIN_HAND);
            helper.assertTrue(a.getMainHandItem().getCount() == 2, "the fourth neighbour only chats");
            // a ate a bowl: Mika-oma has more
            a.getMainHandItem().shrink(1);
            a.getInventory().add(a.getMainHandItem().copyAndClear());
            OmaQuest.ROL.talk(oma, a);
            helper.assertTrue(OmaQuest.LIJN.stap(a) == 1 && tel(a, soep) == 2, "a missing bowl comes again");
            inHand(a, soep);
            OmaQuest.klik(a, buren[1], InteractionHand.MAIN_HAND);
            helper.assertTrue("1".equals(OmaQuest.LIJN.sleutel(a)), "not done yet");
            OmaQuest.klik(a, buren[2], InteractionHand.MAIN_HAND);
            helper.assertTrue(OmaQuest.soepOver(a) == 0 && a.getMainHandItem().isEmpty() && "1_klaar".equals(OmaQuest.LIJN.sleutel(a)) && OmaQuest.soepOver(b) == 3,
                    "all three had a's soup: go and tell her (b's round is b's own)");
            // step 2: the knitting
            OmaQuest.ROL.talk(oma, a);
            helper.assertTrue(OmaQuest.LIJN.stap(a) == 2, "she misses her knitting");
            OmaQuest.breiwerk(a, mand);
            helper.assertTrue(OmaQuest.LIJN.stap(a) == 3 && tel(a, breiwerk) == 1 && level.getBlockState(mand).is(PaleizenFeature.BREIWERK.get()),
                    "a has the knitting, the basket stays for the next player");
            OmaQuest.breiwerk(a, mand);
            helper.assertTrue(tel(a, breiwerk) == 1, "not twice");
            GuhQuests.take(a, breiwerk, 1);
            OmaQuest.ROL.talk(oma, a);
            helper.assertTrue(OmaQuest.LIJN.stap(a) == 3 && tel(a, muts) == 0, "without the knitting she waits");
            OmaQuest.breiwerk(a, mand);
            helper.assertTrue(tel(a, breiwerk) == 1, "lost on the way: the basket gives it again");
            // step 3: back to her
            OmaQuest.ROL.talk(oma, a);
            helper.assertTrue(OmaQuest.LIJN.klaar(a) && tel(a, breiwerk) == 0 && tel(a, muts) == 1, "done: the knitted Mika hat");
            OmaQuest.ROL.talk(oma, a);
            OmaQuest.breiwerk(a, mand);
            helper.assertTrue(tel(a, muts) == 1 && tel(a, breiwerk) == 0, "the reward comes once, the basket has nothing more for a");
            // b does the whole round at the same characters
            OmaQuest.ROL.talk(oma, b);
            OmaQuest.ROL.antwoord(oma, b, 1);
            for (int nr = 0; nr < 3; nr++) {
                inHand(b, soep);
                OmaQuest.klik(b, buren[nr], InteractionHand.MAIN_HAND);
                b.getInventory().add(b.getMainHandItem().copyAndClear());
            }
            OmaQuest.ROL.talk(oma, b);
            OmaQuest.breiwerk(b, mand);
            OmaQuest.ROL.talk(oma, b);
            helper.assertTrue(OmaQuest.LIJN.klaar(b) && tel(b, muts) == 1, "b too: nothing was used up");
            // the discount: every third barter of a friend of Mika-oma is free
            ServerPlayer c = speler(helper, new BlockPos(18, 2, 22));
            try {
                Item staaf = ModItems.VAHOEGE_VADS_INGOT.get();
                MikaEntity mika = ModEntities.NETHER_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
                ItemStack aanbod = new ItemStack(staaf);
                OmaQuest.ruil(a, mika, aanbod);
                OmaQuest.ruil(a, mika, aanbod);
                helper.assertTrue(tel(a, staaf) == 0, "the first two barters cost an ingot as always");
                OmaQuest.ruil(a, mika, new ItemStack(Items.GOLD_INGOT));
                OmaQuest.ruil(a, oma, aanbod);
                helper.assertTrue(tel(a, staaf) == 0, "only an ingot held out to a Nether-Mika counts");
                OmaQuest.ruil(a, mika, aanbod);
                helper.assertTrue(tel(a, staaf) == 1, "the third one comes back");
                for (int i = 0; i < 6; i++) {
                    OmaQuest.ruil(c, mika, aanbod);
                }
                helper.assertTrue(tel(c, staaf) == 0 && !OmaQuest.geruild(c), "no discount for somebody who never helped her");
                mika.discard();
            } finally {
                weg(helper, c);
            }
        } finally {
            level.setBlockAndUpdate(helper.absolutePos(new BlockPos(30, 2, 26)), Blocks.AIR.defaultBlockState());
            ruimOp(helper);
            weg(helper, a, b);
        }
        helper.succeed();
    }

    /** A copy of the Mika-woonblokken: the template's spots in the world, and nobody breaks it. */
    @GuhTest(template = KAMER, batch = "paleizen_woon")
    public static void paleizenWoonblokkenKopie(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(3, 2, 3));
        try {
            // (template 10, 12, 30 is the room's corner at floor height: the west block's ground floor lies in the room)
            StructureStart start = PaleisProef.bouw(level, PaleisPlekken.WOONBLOKKEN, new BlockPos(10, 12, 30), helper.absolutePos(new BlockPos(0, 1, 0)), false);
            BlockPos brom = helper.absolutePos(new BlockPos(1, 2, 6));
            helper.assertTrue(brom.equals(PaleisPlekken.wereld(start, PaleisPlekken.Woon.MOPPER_0)), "Brom-Mika's spot: " + PaleisPlekken.wereld(start, PaleisPlekken.Woon.MOPPER_0));
            helper.assertTrue(PaleisPlekken.kopie(level, PaleisPlekken.WOONBLOKKEN, brom) == start && PaleisPlekken.is(level, PaleisPlekken.WOONBLOKKEN, brom, PaleisPlekken.Woon.MOPPER_0)
                    && !PaleisPlekken.is(level, PaleisPlekken.WOONBLOKKEN, brom.above(), PaleisPlekken.Woon.MOPPER_0), "the copy is found, a spot is one block");
            helper.assertTrue(PaleisPlekken.WOONBLOKKEN.equals(Bescherming.structuurBij(level, brom)) && !Bescherming.mag(p, brom), "the building is protected");
            // Brom-Mika comes by himself (seen missing twice), with his number and name
            Bezetting.controleer(level, brom);
            Bezetting.bevestigAlles(level);
            Bezetting.controleer(level, brom);
            List<MopperMikaEntity> er = level.getEntitiesOfClass(MopperMikaEntity.class, new AABB(brom).inflate(3));
            helper.assertTrue(er.size() == 1 && er.get(0).getNr() == 0 && er.get(0).blockPosition().equals(brom) && Bezetting.isBezetting(er.get(0)),
                    "Brom-Mika lives here: " + er);
        } finally {
            Kopieen.testWissen(level);
            for (Entity e : level.getEntitiesOfClass(Entity.class, kamer(helper).inflate(80), e -> e instanceof MopperMikaEntity || e instanceof GuhNpcEntity)) {
                e.discard();
            }
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the Stalknecht-guh
    // =================================================================================================================

    /**
     * "De onrustige Worstzwijntjes" at a copy of the Mika-stal: its inhabitants come by themselves; a player pets three
     * animals calm (an empty hand, three strokes each), fills the stable's voerbak (it empties again), finds the runaway
     * that is theirs alone (it bolts from stomping, not from sneaking) and gets two baskets; a second player's round is
     * untouched by all of it.
     */
    @GuhTest(template = KAMER, batch = "paleizen_stal", timeoutTicks = 200)
    public static void paleizenStalQuest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(20, 2, 16)), b = speler(helper, new BlockPos(22, 2, 16));
        try {
            // (template 5, 4, 5 is the room's corner at standing height: every spot of the questline lies in the room)
            StructureStart start = PaleisProef.bouw(level, PaleisPlekken.STAL, new BlockPos(5, 4, 5), helper.absolutePos(new BlockPos(0, 2, 0)), false);
            BlockPos midden = helper.absolutePos(new BlockPos(16, 2, 16));
            BlockPos bak = PaleisPlekken.wereld(start, PaleisPlekken.Stal.VOERBAK);
            helper.assertTrue(helper.absolutePos(new BlockPos(2, 2, 7)).equals(bak) && PaleisPlekken.kopie(level, PaleisPlekken.STAL, midden) == start, "template -> world: " + bak);
            // the inhabitants: seen missing twice, then they come
            Bezetting.controleer(level, midden);
            Bezetting.bevestigAlles(level);
            int erbij = Bezetting.controleer(level, midden);
            List<WorstzwijntjeEntity> dieren = level.getEntitiesOfClass(WorstzwijntjeEntity.class, kamer(helper), WorstzwijntjeEntity::isStal);
            List<GuhNpcEntity> knechten = level.getEntitiesOfClass(GuhNpcEntity.class, kamer(helper), n -> n.getKind() == GuhNpcEntity.Kind.STALKNECHTGUH);
            helper.assertTrue(erbij == 6 && dieren.size() == 5 && knechten.size() == 1, "the Stalknecht-guh and five Worstzwijntjes come: " + erbij);
            WorstzwijntjeEntity[] dier = new WorstzwijntjeEntity[5];
            for (WorstzwijntjeEntity z : dieren) {
                dier[z.stalNr()] = z;
                helper.assertTrue(z.blockPosition().equals(PaleisPlekken.wereld(start, PaleisPlekken.Stal.ZWIJNTJES[z.stalNr()])), "number " + z.stalNr() + " in its box");
            }
            helper.assertTrue(dier[4].isBaby() && !dier[0].isBaby(), "the piglet");
            GuhNpcEntity knecht = knechten.get(0);
            helper.assertTrue(knecht.blockPosition().equals(PaleisPlekken.wereld(start, PaleisPlekken.Stal.STALKNECHTGUH)) && NpcRollen.van(knecht) == StalQuest.ROL, "the knecht at the door");
            level.setBlockAndUpdate(bak, BoerderijFeature.GUH_VOERBAK.get().defaultBlockState());
            BlockPos andereBak = helper.absolutePos(new BlockPos(20, 2, 30));
            level.setBlockAndUpdate(andereBak, BoerderijFeature.GUH_VOERBAK.get().defaultBlockState());
            Item voer = PaleizenFeature.ZWIJNENVOER.get(), gevangen = PaleizenFeature.GEVANGEN_ZWIJNTJE.get(), mandje = PaleizenFeature.MANDJE.get();
            // step 0
            StalQuest.ROL.talk(knecht, a);
            StalQuest.ROL.antwoord(knecht, a, 2);
            helper.assertTrue(StalQuest.LIJN.stap(a) == 0, "he explains first");
            StalQuest.ROL.antwoord(knecht, a, 1);
            helper.assertTrue(StalQuest.LIJN.stap(a) == 1 && StalQuest.LIJN.stap(b) == 0, "a helps");
            // step 1: three strokes each, with an empty hand
            a.snapTo(dier[0].getX() + 1.5, dier[0].getY(), dier[0].getZ() + 1.5);
            helper.assertTrue(StalQuest.onrustig(dier[0]), "restless while a is near and has not calmed it");
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            StalQuest.aai(a, dier[0]);
            helper.assertTrue(StalQuest.LIJN.teller(a, "aai_0") == 0, "a stick in your hand is no stroke");
            a.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            StalQuest.aai(a, dier[0]);
            StalQuest.aai(a, dier[0]);
            helper.assertTrue(StalQuest.aantalKalm(a) == 0 && StalQuest.onrustig(dier[0]), "two strokes: not yet");
            StalQuest.aai(a, dier[0]);
            StalQuest.aai(a, dier[0]);
            helper.assertTrue(StalQuest.aantalKalm(a) == 1 && !StalQuest.onrustig(dier[0]) && StalQuest.LIJN.teller(a, "aai_0") == 3, "three: calm, and it counts once");
            StalQuest.ROL.talk(knecht, a);
            helper.assertTrue(StalQuest.LIJN.stap(a) == 1 && tel(a, voer) == 0, "one calm animal is not three");
            for (int nr : new int[]{1, 4}) {
                for (int i = 0; i < StalQuest.AAIEN; i++) {
                    StalQuest.aai(a, dier[nr]);
                }
            }
            helper.assertTrue(StalQuest.aantalKalm(a) == 3 && "1_klaar".equals(StalQuest.LIJN.sleutel(a)) && StalQuest.aantalKalm(b) == 0, "three calm (the piglet counts too): back to him");
            StalQuest.ROL.talk(knecht, a);
            helper.assertTrue(StalQuest.LIJN.stap(a) == 2 && tel(a, voer) == 1, "a sack of feed");
            // step 2: the sack goes into the stable's own voerbak only
            ItemStack zak = inHand(a, voer);
            helper.assertTrue(StalQuest.vulVoerbak(a, andereBak, zak) && !StalQuest.gevoerd(a) && zak.getCount() == 1
                    && GuhVoerbakBlock.voer(level.getBlockState(andereBak)) == 0, "another voerbak: nothing happens");
            helper.assertTrue(!StalQuest.vulVoerbak(a, bak.above(), zak), "not a voerbak: not our click");
            helper.assertTrue(StalQuest.vulVoerbak(a, bak, zak) && StalQuest.gevoerd(a) && a.getMainHandItem().isEmpty()
                    && GuhVoerbakBlock.voer(level.getBlockState(bak)) == GuhVoerbakBlock.MAX && Herstel.wacht(level, bak), "the stable's voerbak is full");
            Herstel.verschuif(level, StalQuest.VOERBAK_TICKS);
            Herstel.verwerk(level);
            helper.assertTrue(GuhVoerbakBlock.voer(level.getBlockState(bak)) == 0 && "2_klaar".equals(StalQuest.LIJN.sleutel(a)), "and empty again for the next player");
            a.getInventory().clearContent();
            StalQuest.ROL.talk(knecht, a);
            helper.assertTrue(StalQuest.LIJN.stap(a) == 3 && StalQuest.zoekt(a) && !StalQuest.zoekt(b), "Knorretje has run away (from a)");
            // step 3: the runaway is a's alone
            helper.assertTrue(StalQuest.tick(b, start) == null, "b is not looking for one");
            WorstzwijntjeEntity k = StalQuest.tick(a, start);
            helper.assertTrue(k != null && k.isOntsnapt() && a.getUUID().equals(k.ontsnaptVan()) && !k.shouldBeSaved() && StalQuest.knorretje(a) == k
                    && StalQuest.tick(a, start) == null, "one runaway for a, never saved");
            Vec3 eerste = PaleisPlekken.voet(start, PaleisPlekken.Stal.SCHUILPLEKKEN[k.schuilplek()]);
            helper.assertTrue(k.position().distanceTo(eerste) < 0.1, "it hides at a hiding spot");
            StalQuest.pak(b, k);
            helper.assertTrue(k.isAlive() && tel(b, gevangen) == 0, "b can't take a's runaway");
            a.snapTo(k.getX() + 2.0, k.getY(), k.getZ());
            a.setShiftKeyDown(false);
            int plek = k.schuilplek();
            k.ontsnaptTick();
            helper.assertTrue(k.gevlucht() == 1 && k.schuilplek() != plek, "a stomps up: it bolts to the next spot");
            for (int i = 0; i < WorstzwijntjeEntity.VLUCHT_TICKS + 5; i++) {
                k.ontsnaptTick();
            }
            Vec3 tweede = PaleisPlekken.voet(start, PaleisPlekken.Stal.SCHUILPLEKKEN[k.schuilplek()]);
            helper.assertTrue(k.isAlive() && k.position().distanceTo(tweede) < 1.6, "and gets there: " + k.position() + " / " + tweede);
            a.snapTo(k.getX() + 2.0, k.getY(), k.getZ());
            a.setShiftKeyDown(true);
            k.ontsnaptTick();
            helper.assertTrue(k.gevlucht() == 1, "sneaking does not startle it");
            StalQuest.pak(a, k);
            helper.assertTrue(!k.isAlive() && tel(a, gevangen) == 1 && "3_gevangen".equals(StalQuest.LIJN.sleutel(a)) && !StalQuest.zoekt(a)
                    && StalQuest.tick(a, start) == null, "a has Knorretje in their arms");
            a.setShiftKeyDown(false);
            // lost it (it wriggled free): it turns up again
            GuhQuests.take(a, gevangen, 1);
            WorstzwijntjeEntity weer = StalQuest.tick(a, start);
            helper.assertTrue(weer != null, "a lost runaway is back at the stable");
            StalQuest.ROL.talk(knecht, a);
            helper.assertTrue(StalQuest.LIJN.stap(a) == 3 && tel(a, mandje) == 0, "without Knorretje no reward");
            StalQuest.pak(a, weer);
            StalQuest.ROL.talk(knecht, a);
            helper.assertTrue(StalQuest.LIJN.klaar(a) && tel(a, gevangen) == 0 && tel(a, mandje) == 2, "done: two Worstzwijntjes in a basket");
            StalQuest.ROL.talk(knecht, a);
            helper.assertTrue(tel(a, mandje) == 2, "once");
            // b starts now: the same animals are restless for b, calm for a
            StalQuest.ROL.talk(knecht, b);
            StalQuest.ROL.antwoord(knecht, b, 1);
            b.snapTo(dier[0].getX() + 1.5, dier[0].getY(), dier[0].getZ() + 1.5);
            a.snapTo(dier[0].getX() - 1.5, dier[0].getY(), dier[0].getZ() + 1.5);
            helper.assertTrue(StalQuest.LIJN.stap(b) == 1 && StalQuest.onrustig(dier[0]) && StalQuest.aantalKalm(b) == 0 && dier[0].isAlive(), "nothing was used up for b");
        } finally {
            Kopieen.testWissen(level);
            level.setBlockAndUpdate(helper.absolutePos(new BlockPos(2, 2, 7)), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(helper.absolutePos(new BlockPos(20, 2, 30)), Blocks.AIR.defaultBlockState());
            ruimOp(helper);
            weg(helper, a, b);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the Tolwachter-Mika
    // =================================================================================================================

    /**
     * "De tolbrug" at a copy of the Mika-brugpaleis: the gate shoves back whoever has not paid (and nobody else), toll or
     * three riddles open it per player, five planks lay the five rows (everyone who laid one has mended it; the rows fall
     * out again), the tolbel only counts on its step, and the building set comes once (the card again when lost).
     */
    @GuhTest(template = KAMER, batch = "paleizen_tol", timeoutTicks = 200)
    public static void paleizenTolQuest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(3, 2, 6)), b = speler(helper, new BlockPos(4, 2, 6)), c = speler(helper, new BlockPos(5, 2, 6));
        try {
            // (template 29, 28, 5 is the room's corner at floor height: the deck is the floor; the gate and the gap lie in the room)
            StructureStart start = PaleisProef.bouw(level, PaleisPlekken.BRUGPALEIS, new BlockPos(29, 28, 5), helper.absolutePos(new BlockPos(0, 1, 0)), false);
            BlockPos midden = helper.absolutePos(new BlockPos(8, 2, 10));
            Bezetting.controleer(level, midden);
            Bezetting.bevestigAlles(level);
            Bezetting.controleer(level, midden);
            List<GuhNpcEntity> wachters = level.getEntitiesOfClass(GuhNpcEntity.class, kamer(helper), n -> n.getKind() == GuhNpcEntity.Kind.TOLWACHTER_MIKA);
            helper.assertTrue(wachters.size() == 1 && wachters.get(0).blockPosition().equals(helper.absolutePos(new BlockPos(6, 2, 8)))
                    && NpcRollen.van(wachters.get(0)) == TolQuest.ROL, "the Tolwachter-Mika sits beside the mouth: " + wachters);
            GuhNpcEntity wachter = wachters.get(0);
            Item knabbels = ModItems.KAAS_KNABBELS.get(), plank = PaleizenFeature.LOSSE_PLANK.get();
            // the gate: a shove back towards the mouth for a (survival, not paid); never for creative players
            BlockPos inPoort = new BlockPos(12, 2, 10);
            Vec3 buiten = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 10)));
            naar(helper, a, inPoort);
            float levens = a.getHealth();
            helper.assertTrue(TolQuest.poort(a, start) && a.getDeltaMovement().x < -0.3 && a.getHealth() == levens, "shoved back west, unhurt: " + a.getDeltaMovement());
            boolean teruggezet = false;
            for (int i = 0; i < 40 && !teruggezet; i++) {
                naar(helper, a, inPoort);
                TolQuest.poort(a, start);
                teruggezet = a.position().distanceTo(buiten) < 1.0;
            }
            helper.assertTrue(teruggezet && a.getHealth() == levens, "whoever keeps pushing is put back in front of the mouth, unhurt");
            naar(helper, a, new BlockPos(4, 2, 10));
            helper.assertTrue(!TolQuest.poort(a, start), "in front of the mouth nothing happens");
            b.setGameMode(GameType.CREATIVE);
            naar(helper, b, inPoort);
            helper.assertTrue(!TolQuest.poort(b, start), "a creative player walks through");
            b.setGameMode(GameType.SURVIVAL);
            // step 0 and 1 for a: toll
            TolQuest.ROL.talk(wachter, a);
            helper.assertTrue(TolQuest.LIJN.stap(a) == 1 && !TolQuest.magDoor(a), "he wants toll or riddles");
            TolQuest.ROL.antwoord(wachter, a, 1);
            helper.assertTrue(TolQuest.LIJN.stap(a) == 1, "no knabbels, no passage");
            a.getInventory().add(new ItemStack(knabbels, 10));
            TolQuest.ROL.antwoord(wachter, a, 1);
            helper.assertTrue(TolQuest.LIJN.stap(a) == 2 && tel(a, knabbels) == 2 && tel(a, plank) == TolQuest.RIJEN && TolQuest.magDoor(a), "eight knabbels: through, with five planks");
            naar(helper, a, inPoort);
            helper.assertTrue(!TolQuest.poort(a, start) && TolQuest.poort(b, start), "the gate is open for a, not for b");
            // step 1 for b: riddles; a wrong answer costs nothing
            TolQuest.ROL.talk(wachter, b);
            TolQuest.ROL.antwoord(wachter, b, 2);
            int eerste = TolQuest.LIJN.teller(b, "raadsel");
            TolQuest.ROL.antwoord(wachter, b, 10 + (TolQuest.GOED[eerste] + 1) % 3);
            helper.assertTrue(TolQuest.LIJN.stap(b) == 1 && TolQuest.LIJN.teller(b, "goed") == 0 && TolQuest.LIJN.teller(b, "raadsel") == eerste + 1,
                    "wrong: another riddle, nothing lost");
            for (int i = 0; i < TolQuest.RAADSELS_NODIG; i++) {
                helper.assertTrue(TolQuest.LIJN.stap(b) == 1, "not through yet after " + i);
                TolQuest.ROL.antwoord(wachter, b, 10 + TolQuest.GOED[TolQuest.LIJN.teller(b, "raadsel") % TolQuest.GOED.length]);
            }
            helper.assertTrue(TolQuest.LIJN.stap(b) == 2 && tel(b, plank) == TolQuest.RIJEN && !TolQuest.poort(b, start), "three right: b is through too");
            // c pays as well and lays nothing
            TolQuest.ROL.talk(wachter, c);
            c.getInventory().add(new ItemStack(knabbels, 8));
            TolQuest.ROL.antwoord(wachter, c, 1);
            // step 2: the gap. Far away nothing is laid; near it a plank lays a whole row, from the tolhuis side on
            BoundingBox gat = PaleisPlekken.wereld(start, PaleisPlekken.Brug.GAT);
            BlockPos rij0 = helper.absolutePos(new BlockPos(29, 1, 8));
            BlockState vloer = level.getBlockState(rij0);
            helper.assertTrue(gat.minX() == rij0.getX() && gat.minY() == rij0.getY() && gat.minZ() == rij0.getZ() && TolQuest.openRijen(level, start) == 5, "the gap: five rows");
            naar(helper, a, new BlockPos(5, 2, 10));
            ItemStack planken = inHand(a, plank);
            helper.assertTrue(TolQuest.legPlank(a, planken) && planken.getCount() == 5 && TolQuest.openRijen(level, start) == 5, "too far from the gap");
            helper.assertTrue(!TolQuest.legPlank(a, new ItemStack(Items.OAK_PLANKS)), "ordinary planks are not our click");
            naar(helper, a, new BlockPos(27, 2, 10));
            naar(helper, b, new BlockPos(26, 2, 10));
            naar(helper, c, new BlockPos(26, 2, 12));
            TolQuest.legPlank(a, planken);
            boolean rijLigt = true;
            for (int z = 8; z <= 12; z++) {
                rijLigt &= level.getBlockState(helper.absolutePos(new BlockPos(29, 1, z))).is(PaleizenFeature.BRUGPLANK.get());
            }
            helper.assertTrue(rijLigt && planken.getCount() == 4 && TolQuest.openRijen(level, start) == 4 && TolQuest.LIJN.teller(a, "planken") == 1
                    && !level.getBlockState(helper.absolutePos(new BlockPos(30, 1, 10))).is(PaleizenFeature.BRUGPLANK.get()), "one plank: the first row of five blocks");
            TolQuest.legPlank(a, planken);
            TolQuest.legPlank(a, planken);
            ItemStack vanB = inHand(b, plank);
            TolQuest.legPlank(b, vanB);
            helper.assertTrue(TolQuest.openRijen(level, start) == 1 && TolQuest.LIJN.stap(a) == 2 && TolQuest.LIJN.stap(b) == 2, "four rows: not whole yet");
            TolQuest.legPlank(b, vanB);
            helper.assertTrue(TolQuest.openRijen(level, start) == 0 && TolQuest.LIJN.stap(a) == 3 && TolQuest.LIJN.stap(b) == 3 && TolQuest.LIJN.stap(c) == 2,
                    "whole: a and b mended it together, c laid nothing");
            helper.assertTrue(tel(a, plank) == 0 && tel(b, plank) == 0, "the planks that were left go back");
            ItemStack vanC = inHand(c, plank);
            TolQuest.legPlank(c, vanC);
            helper.assertTrue(vanC.getCount() == 5 && TolQuest.LIJN.stap(c) == 2, "c has to wait until it falls apart again");
            Herstel.verschuif(level, TolQuest.HERSTEL_TICKS);
            Herstel.verwerk(level);
            helper.assertTrue(TolQuest.openRijen(level, start) == 5 && level.getBlockState(rij0) == vloer, "after a minute the gap is back, as it was");
            for (int i = 0; i < 5; i++) {
                TolQuest.legPlank(c, vanC);
            }
            helper.assertTrue(TolQuest.LIJN.stap(c) == 3 && TolQuest.LIJN.stap(a) == 3, "c mends it for themselves");
            Herstel.verschuif(level, TolQuest.HERSTEL_TICKS);
            Herstel.verwerk(level);
            // step 3: the bell (a second copy: template 70, 28, 5 at the room's corner)
            Kopieen.testWissen(level);
            StructureStart ver = PaleisProef.bouw(level, PaleisPlekken.BRUGPALEIS, new BlockPos(70, 28, 5), helper.absolutePos(new BlockPos(0, 1, 0)), false);
            BlockPos bel = PaleisPlekken.wereld(ver, PaleisPlekken.Brug.BEL);
            helper.assertTrue(helper.absolutePos(new BlockPos(12, 6, 10)).equals(bel), "the tolbel: " + bel);
            level.setBlock(bel.below(), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(bel, Blocks.BELL.defaultBlockState(), 2);
            BlockPos andereBel = helper.absolutePos(new BlockPos(20, 3, 20));
            level.setBlock(andereBel.below(), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(andereBel, Blocks.BELL.defaultBlockState(), 2);
            ServerPlayer d = speler(helper, new BlockPos(6, 2, 6));
            try {
                helper.assertTrue(!TolQuest.bel(d, bel) && TolQuest.LIJN.stap(d) == 0, "a stranger only hears a dong");
                helper.assertTrue(!TolQuest.bel(a, andereBel) && !TolQuest.bel(a, bel.below()) && TolQuest.LIJN.stap(a) == 3, "another bell is just a bell");
                helper.assertTrue(TolQuest.bel(a, bel) && TolQuest.LIJN.stap(a) == 4 && !TolQuest.bel(a, bel), "DONG: back to the Tolwachter");
            } finally {
                weg(helper, d);
            }
            // step 4: the reward, once; the card again when it is lost
            Item kaart = PaleizenFeature.RECEPT_BRUG.get();
            TolQuest.ROL.talk(wachter, b);
            helper.assertTrue(TolQuest.LIJN.stap(b) == 3 && tel(b, kaart) == 0, "b has not rung the bell");
            TolQuest.ROL.talk(wachter, a);
            helper.assertTrue(TolQuest.LIJN.klaar(a) && tel(a, kaart) == 1 && tel(a, PaleizenFeature.BRUGPLANK.get().asItem()) == 16
                    && tel(a, PaleizenFeature.BRUGLEUNING.get().asItem()) == 8, "done: the bridge building set");
            TolQuest.ROL.talk(wachter, a);
            helper.assertTrue(tel(a, kaart) == 1 && tel(a, PaleizenFeature.BRUGPLANK.get().asItem()) == 16, "once");
            GuhQuests.take(a, kaart, 1);
            TolQuest.ROL.talk(wachter, a);
            helper.assertTrue(tel(a, kaart) == 1, "a lost card comes again");
        } finally {
            Kopieen.testWissen(level);
            for (BlockPos p : List.of(new BlockPos(12, 6, 10), new BlockPos(12, 5, 10), new BlockPos(20, 3, 20), new BlockPos(20, 2, 20))) {
                level.setBlock(helper.absolutePos(p), Blocks.AIR.defaultBlockState(), 2);
            }
            ruimOp(helper);
            weg(helper, a, b, c);
        }
        helper.succeed();
    }
}
