package nl.juiced.guhs.feature.boerderij;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.tuintjes.TuinBlock;
import nl.juiced.guhs.feature.tuintjes.TuinPlant;
import nl.juiced.guhs.feature.tuintjes.TuintjesFeature;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
/**
 * Game tests of the Guhboerderij: care makes the animals content and they give their products (pluiswol, a knabbelei in
 * the nest, kaasmelk with a bottle), the voerbak feeds them by itself, Boerin Hooibaal's chores and shop, the Knus
 * counters and milestones, the tags, the Guhdex pages and the protection. (Template boerderij_test_wei: a grass floor at
 * helper y = 1.)
 */
public class BoerderijGameTests {
    private static final String WEI = "boerderij_test_wei";

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
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

    private static <T extends BoerderijDier> T dier(GameTestHelper helper, net.minecraft.world.entity.EntityType<T> type, int x, int z) {
        T dier = helper.spawn(type, new BlockPos(x, 2, z));
        dier.vergeetZorg();
        return dier;
    }

    // --- care, content, products ---------------------------------------------------------------------------------------

    @GuhTest(template = WEI, timeoutTicks = 100)
    public static void boerderijSchaapjeWordtBlijEnGeeftPluiswol(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        GuhschaapjeEntity schaap = dier(helper, BoerderijFeature.GUHSCHAAPJE.get(), 6, 6);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        schaap.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(schaap.heeftZorg(BoerderijDier.Zorg.AAIEN) && !schaap.isBlij(), "petted, not content yet");
        schaap.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(KnusVoortgang.teller(p, BoerderijVoortgang.AAIEN) == 1, "petting twice on a day counts once");
        ItemStack borstel = new ItemStack(BoerderijFeature.GUHBORSTEL.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, borstel);
        schaap.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(schaap.isBlij() && schaap.productGegeven(), "brushed too: content, and the wool came off");
        helper.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).getDamageValue() == 1, "the brush wears a little");
        helper.assertTrue(KnusVoortgang.teller(p, BoerderijVoortgang.BLIJ) == 1 && advancement(p, "boerderij_verzorgd"), "the Knus tab: a content animal");
        helper.assertTrue(KnusVoortgang.teller(p, BoerderijVoortgang.PRODUCTEN) >= 2, "the wool counts as products");
        // one product a day: feeding it now gives nothing more
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BoerderijFeature.KNABBELVOER.get(), 3));
        schaap.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(schaap.heeftZorg(BoerderijDier.Zorg.VOEREN) && p.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 2, "fed (one)");
        helper.succeedWhen(() -> {
            List<ItemEntity> wol = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(schaap.blockPosition()).inflate(4),
                    e -> e.getItem().is(BoerderijFeature.PLUISWOL.get()));
            helper.assertTrue(wol.size() == 1 && wol.get(0).getItem().getCount() >= 2, "one tuft of pluiswol on the ground: " + wol.size());
            leave(helper, p);
        });
    }

    @GuhTest(template = WEI)
    public static void boerderijKippetjeLegtInHetNestje(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        BlockPos nest = new BlockPos(3, 2, 3);
        helper.setBlock(nest, BoerderijFeature.KIPPENNESTJE.get());
        KnabbelkippetjeEntity kip = dier(helper, BoerderijFeature.KNABBELKIPPETJE.get(), 7, 7);
        try {
            kip.verzorg(BoerderijDier.Zorg.AAIEN, p);
            kip.verzorg(BoerderijDier.Zorg.VOEREN, p);
            helper.assertTrue(kip.isBlij() && kip.productGegeven(), "content: it laid");
            helper.assertTrue(helper.getBlockState(nest).getValue(KippennestjeBlock.EIEREN) == 1, "the egg is in the nest");
            int n = KippennestjeBlock.neem(helper.getLevel(), helper.absolutePos(nest), p);
            helper.assertTrue(n == 1 && count(p, BoerderijFeature.KNABBELEI.get()) == 1, "taken out of the nest");
            helper.assertTrue(helper.getBlockState(nest).getValue(KippennestjeBlock.EIEREN) == 0, "the nest is empty");
            helper.assertTrue(KnusVoortgang.teller(p, BoerderijVoortgang.PRODUCTEN) == 1, "the egg counts when you take it");
            // a full nest: the egg drops on the ground
            helper.setBlock(nest, BoerderijFeature.KIPPENNESTJE.get().defaultBlockState().setValue(KippennestjeBlock.EIEREN, KippennestjeBlock.MAX));
            helper.assertTrue(!KippennestjeBlock.leg(helper.getLevel(), helper.absolutePos(nest)), "a full nest takes no more");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    @GuhTest(template = WEI)
    public static void boerderijGuhkoeGeeftKaasmelk(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        GuhkoeEntity koe = dier(helper, BoerderijFeature.GUHKOE.get(), 6, 6);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
            helper.assertTrue(!koe.melk(p, InteractionHand.MAIN_HAND), "not content: no kaasmelk");
            koe.verzorg(BoerderijDier.Zorg.AAIEN, p);
            koe.verzorg(BoerderijDier.Zorg.BORSTELEN, p);
            helper.assertTrue(koe.isBlij() && !koe.productGegeven(), "content; the kaasmelk waits for a bottle");
            helper.assertTrue(koe.melk(p, InteractionHand.MAIN_HAND), "milked");
            helper.assertTrue(count(p, BoerderijFeature.KAASMELK.get()) == 1 && count(p, Items.GLASS_BOTTLE) == 1, "a bottle of kaasmelk");
            helper.assertTrue(!koe.melk(p, InteractionHand.MAIN_HAND), "once a day");
            ItemStack melk = new ItemStack(BoerderijFeature.KAASMELK.get());
            helper.assertTrue(melk.getItem().getCraftingRemainder(melk) != null && melk.getItem().getCraftingRemainder(melk).create().is(Items.GLASS_BOTTLE), "the bottle stays (recipes)");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    /** Milked yesterday and cared for yesterday: a new day resets both (set through the saved data, the world day stays). */
    @GuhTest(template = WEI)
    public static void boerderijGuhkoeNieuweDag(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        GuhkoeEntity koe = dier(helper, BoerderijFeature.GUHKOE.get(), 6, 6);
        try {
            net.minecraft.nbt.CompoundTag tag = nl.juiced.guhs.storage.Nbt.write(helper.getLevel().registryAccess(), koe::addAdditionalSaveData);
            tag.putLong("ZorgDag", nl.juiced.guhs.feature.knus.Seizoen.dag(helper.getLevel()) - 1);
            tag.putBoolean("ProductGegeven", true);
            tag.putInt("Zorg", 3);
            koe.readAdditionalSaveData(nl.juiced.guhs.storage.Nbt.input(helper.getLevel().registryAccess(), tag));
            helper.assertTrue(koe.isBlij() && koe.productGegeven(), "yesterday: content and milked");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
            helper.assertTrue(!koe.melk(p, InteractionHand.MAIN_HAND), "a new day: yesterday's care doesn't count");
            helper.assertTrue(!koe.isBlij() && !koe.productGegeven() && koe.zorg() == 0,
                    "niet blij (care was reset), not al gemolken");
            koe.verzorg(BoerderijDier.Zorg.AAIEN, p);
            koe.verzorg(BoerderijDier.Zorg.VOEREN, p);
            helper.assertTrue(koe.melk(p, InteractionHand.MAIN_HAND) && count(p, BoerderijFeature.KAASMELK.get()) == 1,
                    "2 of 3 care today: kaasmelk again");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    @GuhTest(template = WEI)
    public static void boerderijAlleProductenEnMijlpalen(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        try {
            for (BoerderijVoortgang.Product product : BoerderijVoortgang.Product.values()) {
                BoerderijVoortgang.product(p, product, 1);
            }
            helper.assertTrue(KnusVoortgang.teller(p, BoerderijVoortgang.SOORTEN) == 3, "all three products");
            helper.assertTrue(advancement(p, "boerderij_alle_producten") && advancement(p, "knuffeldal/boerderij_alle_producten"),
                    "the advancements for all three");
            helper.assertTrue(KnusVoortgang.mijlpalen("boerderij").size() >= 4, "at least four milestones");
            var m = KnusVoortgang.mijlpaal("boerderij_alle_producten");
            helper.assertTrue(m != null && KnusVoortgang.bereikt(p, m) && KnusVoortgang.claim(p, m.id()), "claimed");
            helper.assertTrue(count(p, ModItems.clothingItem(nl.juiced.guhs.entity.GuhClothes.BOERDERIJ_ZAKDOEK)) == 1, "the neckerchief");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    /** A hungry animal walks to a filled voerbak and eats from it by itself. */
    @GuhTest(template = WEI, timeoutTicks = 500)
    public static void boerderijVoerbakVoertVanzelf(GameTestHelper helper) {
        BlockPos bak = new BlockPos(2, 2, 2);
        helper.setBlock(bak, BoerderijFeature.GUH_VOERBAK.get().defaultBlockState().setValue(GuhVoerbakBlock.VOER, 2));
        GuhkoeEntity koe = dier(helper, BoerderijFeature.GUHKOE.get(), 8, 8);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(koe.heeftZorg(BoerderijDier.Zorg.VOEREN), "the koe ate from the voerbak");
                    helper.assertTrue(helper.getBlockState(bak).getValue(GuhVoerbakBlock.VOER) == 1, "one portion less");
                })
                .thenIdle(20)                                  // (the goal ends cleanly: it ticks once more after eating)
                .thenExecute(() -> helper.assertTrue(koe.isAlive() && helper.getBlockState(bak).getValue(GuhVoerbakBlock.VOER) == 1,
                        "fed once a day: it doesn't eat more"))
                .thenSucceed();
    }

    @GuhTest(template = WEI)
    public static void boerderijVoerbakVullen(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        BlockPos bak = new BlockPos(3, 2, 3);
        helper.setBlock(bak, BoerderijFeature.GUH_VOERBAK.get());
        try {
            Hooibaal.zetKlus(p, Hooibaal.Klus.VOERBAK);
            GuhVoerbakBlock.vul(helper.getLevel(), helper.absolutePos(bak), p);
            GuhVoerbakBlock.vul(helper.getLevel(), helper.absolutePos(bak), p);
            helper.assertTrue(helper.getBlockState(bak).getValue(GuhVoerbakBlock.VOER) == 2, "two portions in");
            helper.assertTrue(Hooibaal.rondAf(p) && Hooibaal.klaarVandaag(p), "the voerbak chore is done");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    // --- Boerin Hooibaal ------------------------------------------------------------------------------------------------

    @GuhTest(template = WEI)
    public static void boerderijHooibaalKlusjes(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        try {
            // a doing chore: pet three animals
            Hooibaal.zetKlus(p, Hooibaal.Klus.AAIEN);
            helper.assertTrue(!Hooibaal.rondAf(p), "nothing done yet");
            for (int i = 0; i < 3; i++) {
                GuhschaapjeEntity s = dier(helper, BoerderijFeature.GUHSCHAAPJE.get(), 3 + 2 * i, 6);
                s.verzorg(BoerderijDier.Zorg.AAIEN, p);
            }
            helper.assertTrue(Hooibaal.rondAf(p) && Hooibaal.klaarVandaag(p) && Hooibaal.vandaag(p) == null, "three animals petted: done");
            helper.assertTrue(count(p, ModItems.KAAS_KNABBELS.get()) == Hooibaal.BELONING_KNABBELS
                    && count(p, BoerderijFeature.KNABBELVOER.get()) == Hooibaal.BELONING_VOER, "the reward");
            helper.assertTrue(KnusVoortgang.teller(p, BoerderijVoortgang.KLUSJES) == 1 && advancement(p, "boerderij_klusje"), "counted");
            // a bringing chore: four pluiswol (taken when you come back)
            p.getInventory().clearContent();
            Hooibaal.zetKlus(p, Hooibaal.Klus.WOL);
            p.getInventory().add(new ItemStack(BoerderijFeature.PLUISWOL.get(), 3));
            helper.assertTrue(!Hooibaal.rondAf(p) && count(p, BoerderijFeature.PLUISWOL.get()) == 3, "three is not enough (and nothing is taken)");
            p.getInventory().add(new ItemStack(BoerderijFeature.PLUISWOL.get(), 2));
            helper.assertTrue(Hooibaal.rondAf(p) && count(p, BoerderijFeature.PLUISWOL.get()) == 1, "four taken, one left");
            // kaasmelk: the bottles come back
            p.getInventory().clearContent();
            Hooibaal.zetKlus(p, Hooibaal.Klus.MELK);
            p.getInventory().add(new ItemStack(BoerderijFeature.KAASMELK.get(), 2));
            helper.assertTrue(Hooibaal.rondAf(p) && count(p, Items.GLASS_BOTTLE) == 2, "the empty bottles come back");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    @GuhTest(template = WEI)
    public static void boerderijHooibaalPraatEnVerkoopt(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.BOERINNEGUH);
        BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
        npc.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        helper.getLevel().addFreshEntity(npc);
        try {
            var role = Features.role(GuhNpcEntity.Kind.BOERINNEGUH);
            helper.assertTrue(role instanceof Hooibaal, "Boerin Hooibaal has her own role: " + role);
            role.talk(npc, p);
            helper.assertTrue(BoerderijVoortgang.data(p).getBooleanOr("Ontmoet", false) && Hooibaal.vandaag(p) != null, "met her; today's chore");
            helper.assertTrue(advancement(p, "boerderij_hooibaal"), "the quest advancement");
            var offers = role.offers(npc);
            helper.assertTrue(offers != null && offers.stream().anyMatch(o -> o.getResult().is(BoerderijFeature.GUHBORSTEL.get()))
                    && offers.stream().anyMatch(o -> o.getResult().is(nl.juiced.guhs.feature.tuintjes.TuintjesFeature.GUH_GIETER.get())), "her shop");
        } finally {
            npc.discard();
            leave(helper, p);
        }
        helper.succeed();
    }

    // --- tags, pages, protection -----------------------------------------------------------------------------------------

    @GuhTest(template = WEI)
    public static void boerderijTagsPaginasEnBescherming(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(BoerderijFeature.PLUISWOL.get()).is(KnusTags.PLUISWOL), "pluiswol in #knus/pluiswol");
        helper.assertTrue(new ItemStack(BoerderijFeature.KNABBELEI.get()).is(KnusTags.KNABBELEI), "knabbelei in #knus/knabbelei");
        helper.assertTrue(new ItemStack(BoerderijFeature.KAASMELK.get()).is(KnusTags.KAASMELK), "kaasmelk in #knus/kaasmelk");
        for (GuhVariant page : List.of(GuhVariant.GUHSCHAAPJE, GuhVariant.KNABBELKIPPETJE, GuhVariant.GUHKOE, GuhVariant.BOERINNEGUH)) {
            helper.assertTrue(GuhDex.ENTRIES.contains(page), "a Guhdex page: " + page);
        }
        var types = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE;
        helper.assertTrue(types.getValue(Guhs.id("guhschaapje")) == BoerderijFeature.GUHSCHAAPJE.get()
                && types.getValue(Guhs.id("guhkoe")) == BoerderijFeature.GUHKOE.get(), "the page id is the entity id");
        // the passive animals never attack
        GuhkoeEntity koe = dier(helper, BoerderijFeature.GUHKOE.get(), 6, 6);
        helper.assertTrue(koe.getTarget() == null && !(koe instanceof net.minecraft.world.entity.monster.Enemy), "a lief animal");
        // protection
        BlockPos a = helper.absolutePos(new BlockPos(2, 1, 2));
        BoundingBox box = new BoundingBox(a.getX(), a.getY(), a.getZ(), a.getX() + 3, a.getY() + 3, a.getZ() + 3);
        BoerderijProtection.TEST_AREAS.add(box);
        try {
            helper.assertTrue(BoerderijProtection.inBoerderij(helper.getLevel(), a.offset(1, 1, 1)) && Protected.at(helper.getLevel(), a.offset(1, 1, 1)),
                    "the farm is protected");
            helper.assertTrue(!BoerderijProtection.inBoerderij(helper.getLevel(), a.offset(8, 0, 8)), "outside it isn't");
        } finally {
            BoerderijProtection.TEST_AREAS.remove(box);
        }
        var templates = helper.getLevel().getStructureManager();
        var farm = templates.get(Guhs.id("guhboerderij"));
        helper.assertTrue(farm.isPresent() && farm.get().getSize().getX() == 64 && farm.get().getSize().getZ() == 64, "the farm template");
        helper.succeed();
    }

    /** Right-clicks the top of a block (helper pos) with this in the main hand, the way the real game does it. */
    private static ItemStack klik(GameTestHelper helper, ServerPlayer p, BlockPos pos, ItemStack stack) {
        BlockPos abs = helper.absolutePos(pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.gameMode.useItemOn(p, helper.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false));
        return p.getMainHandItem();
    }

    /**
     * The protection for real (through the event handlers, the way a survival player does it): no breaking, no building,
     * no bone meal, no buckets; but planting seeds, harvesting and filling a voerbak still work. Creative may change it.
     */
    @GuhTest(template = WEI)
    public static void boerderijBeschermingVoorEchteSpelers(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos grond = new BlockPos(4, 1, 4), gras = new BlockPos(5, 1, 5);
        BlockPos bak = new BlockPos(6, 2, 6), voerbak = new BlockPos(7, 2, 3), pot = new BlockPos(3, 2, 7);
        helper.setBlock(bak, TuintjesFeature.GUH_MOESTUINBAK.get());
        helper.setBlock(voerbak, BoerderijFeature.GUH_VOERBAK.get());
        helper.setBlock(pot, TuintjesFeature.GUH_BLOEMPOT.get().defaultBlockState().setValue(TuinBlock.PLANT, TuinPlant.THEEKRUID)
                .setValue(TuinBlock.GROEI, TuinBlock.RIJP));
        BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(1, 0, 1)), helper.absolutePos(new BlockPos(10, 5, 10)));
        BoerderijProtection.TEST_AREAS.add(box);
        ServerPlayer p = player(helper);
        try {
            // survival: no breaking
            BlockPos grondAbs = helper.absolutePos(grond);
            helper.assertTrue(!p.gameMode.destroyBlock(grondAbs) && helper.getBlockState(grond).is(Blocks.GRASS_BLOCK), "survival: can't break the farm");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, grondAbs, level.getBlockState(grondAbs), p)).isCanceled(),
                    "the BreakEvent is cancelled");
            // no building
            var place = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, grondAbs.above()), Blocks.DIRT.defaultBlockState(), p);
            helper.assertTrue(NeoForge.EVENT_BUS.post(place).isCanceled(), "the EntityPlaceEvent is cancelled");
            ItemStack dirt = klik(helper, p, grond, new ItemStack(Items.DIRT, 3));
            helper.assertTrue(helper.getBlockState(grond.above()).isAir() && dirt.getCount() == 3, "no blocks placed on the farm");
            // no bone meal
            ItemStack beendermeel = klik(helper, p, gras, new ItemStack(Items.BONE_MEAL, 4));
            helper.assertTrue(beendermeel.getCount() == 4, "no bone meal on the farm");
            // no buckets (emptied where you look)
            BlockPos boven = helper.absolutePos(new BlockPos(8, 3, 8));
            p.snapTo(boven.getX() + 0.5, boven.getY(), boven.getZ() + 0.5, 0f, 90f);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            p.gameMode.useItem(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND);
            helper.assertTrue(p.getMainHandItem().is(Items.WATER_BUCKET) && helper.getBlockState(new BlockPos(8, 2, 8)).isAir(),
                    "no water poured on the farm");
            // but the farm still works: plant seeds, fill the voerbak, harvest
            ItemStack zaadjes = klik(helper, p, bak, new ItemStack(TuintjesFeature.KNABBELZAADJES.get(), 3));
            helper.assertTrue(helper.getBlockState(bak).getValue(TuinBlock.PLANT) == TuinPlant.KNABBELPLANTJE && zaadjes.getCount() == 2,
                    "seeds planted in the moestuinbak");
            ItemStack voer = klik(helper, p, voerbak, new ItemStack(BoerderijFeature.KNABBELVOER.get(), 2));
            helper.assertTrue(helper.getBlockState(voerbak).getValue(GuhVoerbakBlock.VOER) == 1 && voer.getCount() == 1, "the voerbak filled");
            klik(helper, p, pot, ItemStack.EMPTY);
            helper.assertTrue(helper.getBlockState(pot).getValue(TuinBlock.GROEI) == 0 && count(p, TuintjesFeature.THEEKRUID.get()) >= 2,
                    "harvested the theekruid");
            // creative may change it
            p.setGameMode(GameType.CREATIVE);
            helper.assertTrue(p.gameMode.destroyBlock(grondAbs) && helper.getBlockState(grond).isAir(), "creative can change the farm");
        } finally {
            BoerderijProtection.TEST_AREAS.remove(box);
            leave(helper, p);
        }
        helper.succeed();
    }
}
