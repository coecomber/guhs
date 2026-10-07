package nl.juiced.guhs.feature.torenpeper;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.spiesburcht.Brouwsel;
import nl.juiced.guhs.feature.spiesburcht.GuhbrouwketelBlockEntity;
import nl.juiced.guhs.feature.spiesburcht.RookguhEntity;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.feature.techbezorg.TechbezorgFeature;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of bbq2 (toren-peper). Template: torenpeper_test_kamer (21 x 12 x 21, a bare floor of houtskoolsteen; things on
 * the floor stand at helper y 2).
 * <ul>
 *   <li>the pepper plant: its kind follows the ground, it grows on its own clock (faster under glass), a ripe one is picked
 *       with a click and grows on, its loot;</li>
 *   <li>the kweekbakken: every player an own plant per kind, planted with a seed, ripe after its time, picked once; two
 *       players in the same three troughs;</li>
 *   <li>the Peperteler-guh's whole questline for one player while a second one's does not move; the two drinks are brewed in
 *       a real Guhbrouwketel and do what they say; a raw Vahoegpeper;</li>
 *   <li>the lamp burns only while a player who lit it is near; the Torenwachter-guh's whole questline; the lost Rookguhs
 *       are a player's own, follow the seinlantaarn, come home at the burning lamp and leave without their guide;</li>
 *   <li>both templates hold what the Java side expects where it expects it; structures, sets, Superkompas.</li>
 * </ul>
 * The lamp tests have batches of their own: a lamp burns for ANY player nearby who lit one, also a neighbouring test's.
 */
public class TorenpeperGameTests {
    private static final String KAMER = "torenpeper_test_kamer", BATCH = "torenpeper";
    private static final Verhaallijn TOREN = TorenpeperFeature.VUURTOREN, TUIN = TorenpeperFeature.PEPERTUIN;

    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        zet(helper, p, at);
        return p;
    }

    private static void zet(GameTestHelper helper, Entity e, BlockPos at) {
        BlockPos abs = helper.absolutePos(at);
        e.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            for (VerdwaaldeRookguhEntity r : Vuurtoren.alleVan(helper.getLevel(), p.getUUID())) {
                r.discard();
            }
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void inHand(ServerPlayer p, Item item, int n) {
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, n));
    }

    private static InteractionResult klik(GameTestHelper helper, ServerPlayer p, BlockPos at) {
        BlockPos abs = helper.absolutePos(at);
        return helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), p, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, BlockPos at) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        zet(helper, npc, at);
        npc.setPersistenceRequired();
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    private static int liggend(GameTestHelper helper, Item item) {
        int n = 0;
        for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(24),
                e -> e.getItem().is(item))) {
            n += e.getItem().getCount();
            e.discard();
        }
        return n;
    }

    // =================================================================================================================
    // the pepper plant
    // =================================================================================================================

    /** The ground decides the pepper; the plant has a clock; glass above makes a greenhouse; picking, loot, losing its ground. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 700)
    public static void torenpeperPlantEnGrond(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PeperplantBlock plant = TorenpeperFeature.PEPERPLANT.get();
        BlockPos as = new BlockPos(3, 2, 3), kool = new BlockPos(5, 2, 3), nylium = new BlockPos(7, 2, 3), klok = new BlockPos(11, 2, 3);
        helper.setBlock(as, BarbecuetherFeature.AS_AARDE.get());
        helper.setBlock(kool, BarbecuetherFeature.GLOEIKOOL.get());
        helper.setBlock(nylium, BarbecuetherFeature.PINDASAUS_NYLIUM.get());
        helper.setBlock(klok, BarbecuetherFeature.AS_AARDE.get());
        helper.setBlock(klok.above(5), Blocks.GLASS);                   // (a greenhouse roof over the plant that is left to its clock)
        for (BlockPos p : List.of(as, kool, nylium, klok)) {
            helper.setBlock(p.above(), plant.defaultBlockState());     // (as a template or a replanting machine would: the default state)
        }
        helper.assertTrue(helper.getBlockState(as.above()).getValue(PeperplantBlock.SOORT) == PeperSoort.GROEN, "as-aarde: the green one");
        helper.assertTrue(helper.getBlockState(kool.above()).getValue(PeperplantBlock.SOORT) == PeperSoort.ROOD, "gloeikool: the red one");
        helper.assertTrue(helper.getBlockState(nylium.above()).getValue(PeperplantBlock.SOORT) == PeperSoort.ROZE, "pindasaus-nylium: the pink one");
        helper.assertTrue(!plant.defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(9, 2, 9))), "bare houtskoolsteen is no pepper ground");
        helper.assertTrue(plant.defaultBlockState().canSurvive(level, helper.absolutePos(as.above())), "it needs no light, only its ground");
        helper.assertTrue(level.getBlockTicks().hasScheduledTick(helper.absolutePos(as.above()), plant), "a placed plant has its clock");
        helper.assertTrue(!plant.isMaxAge(helper.getBlockState(as.above())) && plant.getMaxAge() == 3, "a crop of four stages");
        // a greenhouse: glass above, nothing solid in between
        helper.assertTrue(!PeperplantBlock.onderGlas(level, helper.absolutePos(as.above())), "no glass: outside");
        helper.setBlock(nylium.above(6), Blocks.GLASS);
        helper.setBlock(nylium.above(3), Blocks.STONE);
        helper.assertTrue(PeperplantBlock.onderGlas(level, helper.absolutePos(klok.above())), "glass above: a greenhouse");
        helper.assertTrue(!PeperplantBlock.onderGlas(level, helper.absolutePos(nylium.above())), "stone between the plant and the glass: no greenhouse");
        int kas = PeperplantBlock.groeitijd(level, helper.absolutePos(klok.above())), buiten = PeperplantBlock.groeitijd(level, helper.absolutePos(as.above()));
        helper.assertTrue(kas >= PeperplantBlock.GROEI_KAS && kas < buiten && buiten >= PeperplantBlock.GROEI_BUITEN, "faster under glass: " + kas + " / " + buiten);
        // growing, and picking a ripe plant with a click
        BlockPos rood = helper.absolutePos(kool.above());
        BlockState s = plant.groei(level, rood, level.getBlockState(rood), 1);
        helper.assertTrue(s.getValue(PeperplantBlock.AGE) == 1 && !PeperplantBlock.isRijp(s), "one stage");
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 5));
        try {
            klik(helper, p, kool.above());
            helper.assertTrue(helper.getBlockState(kool.above()).getValue(PeperplantBlock.AGE) == 1 && liggend(helper, TorenpeperFeature.VAHOEGPEPER.get()) == 0,
                    "an unripe plant gives nothing");
            s = plant.groei(level, rood, level.getBlockState(rood), 5);
            helper.assertTrue(PeperplantBlock.isRijp(s) && plant.isMaxAge(s) && s.getValue(PeperplantBlock.SOORT) == PeperSoort.ROOD, "ripe, and red");
            klik(helper, p, kool.above());
            int geplukt = liggend(helper, TorenpeperFeature.VAHOEGPEPER.get());
            helper.assertTrue(geplukt >= PeperplantBlock.PLUK_MIN && geplukt <= PeperplantBlock.PLUK_MAX, "picked: " + geplukt + " Vahoegpepers");
            helper.assertTrue(helper.getBlockState(kool.above()).getValue(PeperplantBlock.AGE) == 1 && liggend(helper, TorenpeperFeature.PEPERZAADJES.get()) == 0,
                    "the plant stays and starts over; picking gives no seeds");
            helper.assertTrue(level.getBlockTicks().hasScheduledTick(rood, plant), "and it grows on");
        } finally {
            weg(helper, p);
        }
        // the loot of a broken plant: unripe = its seed; ripe = seeds and the peppers of its kind
        BlockPos roze = helper.absolutePos(nylium.above());
        List<ItemStack> jong = Block.getDrops(level.getBlockState(roze), level, roze, null);
        helper.assertTrue(jong.size() == 1 && jong.get(0).is(TorenpeperFeature.PEPERZAADJES.get()) && jong.get(0).getCount() == 1, "an unripe plant: one seed");
        List<ItemStack> rijp = Block.getDrops(plant.groei(level, roze, level.getBlockState(roze), 3), level, roze, null);
        int zaad = rijp.stream().filter(i -> i.is(TorenpeperFeature.PEPERZAADJES.get())).mapToInt(ItemStack::getCount).sum();
        int pepers = rijp.stream().filter(i -> i.is(TorenpeperFeature.SNOEPPEPER.get())).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(zaad >= 1 && zaad <= 2 && pepers >= 2 && pepers <= 3 && rijp.stream().noneMatch(i -> i.is(TorenpeperFeature.NJEGPEPER.get())),
                "a ripe pink plant: " + zaad + " seeds, " + pepers + " Snoeppepers");
        // other ground under it: another pepper; no ground: no plant
        helper.setBlock(nylium, BarbecuetherFeature.GLOEIKOOL.get());
        helper.assertTrue(helper.getBlockState(nylium.above()).getValue(PeperplantBlock.SOORT) == PeperSoort.ROOD, "the kind follows the ground under it");
        helper.setBlock(as, Blocks.AIR);
        helper.assertTrue(helper.getBlockState(as.above()).isAir(), "without its ground the plant is gone");
        liggend(helper, TorenpeperFeature.PEPERZAADJES.get());
        // and the clock really ticks: under glass the first stage comes within a kas growth time
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(klok.above()).getValue(PeperplantBlock.AGE) >= 1, "the plant under glass grows by itself"));
    }

    // =================================================================================================================
    // the kweekbakken
    // =================================================================================================================

    /** Two players in the same three troughs: each an own plant per kind, planted with a seed, ripe after its time, picked once. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void torenpeperKweekbakkenPerSpeler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos[] bak = {new BlockPos(4, 2, 4), new BlockPos(6, 2, 4), new BlockPos(8, 2, 4)};
        for (PeperSoort s : PeperSoort.values()) {
            helper.setBlock(bak[s.ordinal()], TorenpeperFeature.KWEEKBAK.get().defaultBlockState().setValue(KweekbakBlock.SOORT, s));
        }
        ServerPlayer a = speler(helper, new BlockPos(5, 2, 6)), b = speler(helper, new BlockPos(7, 2, 6));
        Item zaad = TorenpeperFeature.PEPERZAADJES.get();
        try {
            helper.assertTrue(level.getBlockEntity(helper.absolutePos(bak[0])) instanceof KweekbakBlock.Bak, "a kweekbak has its block entity (for the renderer)");
            helper.assertTrue(helper.getBlockState(bak[1]).getValue(KweekbakBlock.GROEI) == 0, "the world's own state is always the empty trough");
            // not before the Peperteler-guh gave you seeds
            inHand(a, zaad, 3);
            klik(helper, a, bak[0]);
            helper.assertTrue(Kweek.groei(a, PeperSoort.GROEN) == 0 && GuhQuests.count(a, zaad) == 3, "not before the questline began");
            TUIN.zet(a, 1);
            TUIN.zet(b, 1);
            // planting takes a seed, and only A has a plant now
            klik(helper, a, bak[0]);
            helper.assertTrue(Kweek.groei(a, PeperSoort.GROEN) == 1 && GuhQuests.count(a, zaad) == 2, "planted: stage 1, one seed gone");
            helper.assertTrue(Kweek.groei(b, PeperSoort.GROEN) == 0 && Kweek.groei(a, PeperSoort.ROOD) == 0, "only A, only the green trough");
            klik(helper, a, bak[0]);
            helper.assertTrue(GuhQuests.count(a, zaad) == 2 && GuhQuests.count(a, TorenpeperFeature.NJEGPEPER.get()) == 0, "a growing plant: no second seed, no pepper yet");
            inHand(b, Items.STICK, 1);
            klik(helper, b, bak[0]);
            helper.assertTrue(Kweek.groei(b, PeperSoort.GROEN) == 0, "an empty trough and no seed in the hand: nothing");
            // the stages follow the time since planting
            long t = 20L, toen = Kweek.stempel(t);
            helper.assertTrue(Kweek.groei(0, t) == 0 && Kweek.groei(toen, t) == 1 && Kweek.groei(toen, t + Kweek.GROEI_TICKS) == 2
                    && Kweek.groei(toen, t + 3L * Kweek.GROEI_TICKS - 1) == 3 && Kweek.groei(toen, t + 3L * Kweek.GROEI_TICKS) == Kweek.RIJP
                    && Kweek.groei(toen, t + 99999) == Kweek.RIJP && Kweek.groei(Kweek.stempel(t - 5000), t) == Kweek.RIJP,
                    "four stages, " + Kweek.GROEI_TICKS + " ticks apiece (also in a world that has only just begun)");
            // ripe: two peppers of that kind and the seed back, once
            Kweek.plant(a, PeperSoort.GROEN, 3L * Kweek.GROEI_TICKS);
            helper.assertTrue(Kweek.groei(a, PeperSoort.GROEN) == Kweek.RIJP, "ripe after three growth times");
            klik(helper, a, bak[0]);
            helper.assertTrue(GuhQuests.count(a, TorenpeperFeature.NJEGPEPER.get()) == Kweek.PEPERS && GuhQuests.count(a, zaad) == 3, "picked: the peppers and the seed back");
            helper.assertTrue(Kweek.groei(a, PeperSoort.GROEN) == 0 && Pepertuin.geplukt(a, PeperSoort.GROEN) && Pepertuin.aantalGeplukt(a) == 1 && TUIN.stap(a) == 1,
                    "the trough is empty again, one of three kinds picked");
            // the other two (the seed in the off hand works too); the third kind finishes the step
            a.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            a.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(zaad, 2));
            klik(helper, a, bak[1]);
            klik(helper, a, bak[2]);
            helper.assertTrue(Kweek.groei(a, PeperSoort.ROOD) == 1 && Kweek.groei(a, PeperSoort.ROZE) == 1, "planted from the off hand");
            helper.assertTrue(Kweek.maakRijp(a) == 3, "(the op command)");
            klik(helper, a, bak[1]);
            klik(helper, a, bak[2]);
            helper.assertTrue(GuhQuests.count(a, TorenpeperFeature.VAHOEGPEPER.get()) == Kweek.PEPERS && GuhQuests.count(a, TorenpeperFeature.SNOEPPEPER.get()) == Kweek.PEPERS,
                    "each trough gives its own pepper");
            helper.assertTrue(TUIN.stap(a) == 2 && Pepertuin.aantalGeplukt(a) == 3, "all three kinds: step 2");
            helper.assertTrue(TUIN.stap(b) == 1 && Pepertuin.aantalGeplukt(b) == 0 && Kweek.groei(b, PeperSoort.ROOD) == 0, "B's troughs and story did not move");
            // saved with the player
            helper.assertTrue(GuhQuests.saved(a).getLongArray("guhs_torenpeper_kweek").map(l -> l.length).orElse(0) == 3, "kept in the player's saved data");
        } finally {
            weg(helper, a, b);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the Peperteler-guh, the peppers and the drinks
    // =================================================================================================================

    /** The whole questline at the Peperteler-guh for one player, with a real Guhbrouwketel; a second player's does not move. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void torenpeperPepertelerLijn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(4, 2, 9)), b = speler(helper, new BlockPos(4, 2, 11));
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.PEPERTELERGUH, new BlockPos(3, 2, 9));
        NpcRole rol = NpcRollen.van(npc);
        Item zaad = TorenpeperFeature.PEPERZAADJES.get();
        BlockPos ketelPos = new BlockPos(8, 2, 9);
        helper.setBlock(ketelPos, SpiesburchtFeature.GUHBROUWKETEL.get());
        try {
            helper.assertTrue(rol instanceof PepertelerRol, "the Peperteler-guh has his role");
            helper.assertTrue(TUIN.stap(a) == 0 && TUIN.stappen() == 4 && !TUIN.begonnen(a), "four steps, not begun");
            // 0: talk, say yes: three seeds
            rol.talk(npc, a);
            rol.antwoord(npc, a, -1);
            helper.assertTrue(TUIN.begonnen(a) && TUIN.stap(a) == 0 && GuhQuests.count(a, zaad) == 0, "closing the screen is not a yes");
            rol.antwoord(npc, a, 1);
            helper.assertTrue(TUIN.stap(a) == 1 && GuhQuests.count(a, zaad) == 3, "yes: step 1 and three seeds");
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, zaad) == 3, "no more seeds while you have what you need");
            // 1: seeds lost: he makes up what the troughs still need
            GuhQuests.take(a, zaad, 2);
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, zaad) == 3 && PepertelerRol.zaadjesNodig(a) == 3, "lost two: he gives two");
            Kweek.plant(a, PeperSoort.GROEN, 0);
            GuhQuests.take(a, zaad, 3);
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, zaad) == 2, "one is planted: only the two that are still needed");
            for (PeperSoort s : PeperSoort.values()) {
                Kweek.plant(a, s, 3L * Kweek.GROEI_TICKS);
                Kweek.klik(a, a.blockPosition(), s);
            }
            helper.assertTrue(TUIN.stap(a) == 2, "the three peppers: step 2");
            // 2: the brewing kit, once; brewing in a real ketel
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, SpiesburchtFeature.GRILLSPIESPOEDER.get()) == 1 && GuhQuests.count(a, ModItems.KAAS_SAUS_BUCKET.get()) == 1
                    && GuhQuests.count(a, Items.GLASS_BOTTLE) == 3, "the brewing kit");
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, SpiesburchtFeature.GRILLSPIESPOEDER.get()) == 1 && TUIN.stap(a) == 2, "the kit comes once, and no drink yet: still step 2");
            GuhbrouwketelBlockEntity ketel = (GuhbrouwketelBlockEntity) level.getBlockEntity(helper.absolutePos(ketelPos));
            for (ItemStack ingredient : List.of(new ItemStack(SpiesburchtFeature.GRILLSPIESPOEDER.get()), new ItemStack(ModItems.KAAS_SAUS_BUCKET.get()),
                    new ItemStack(TorenpeperFeature.VAHOEGPEPER.get()))) {
                GuhQuests.take(a, ingredient.getItem(), 1);
                a.setItemInHand(InteractionHand.OFF_HAND, ingredient);
                helper.assertTrue(ketel.use(a, InteractionHand.OFF_HAND), "the ketel takes " + ingredient);
            }
            helper.assertTrue(ketel.isBrewing(), "a Vahoegpeper in kaasbouillon bubbles");
            ketel.finishBrewing();
            helper.assertTrue(ketel.contents() == Brouwsel.PEPERVUUR, "and becomes Pepervuur");
            GuhQuests.take(a, Items.GLASS_BOTTLE, 1);
            a.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLASS_BOTTLE));
            ketel.use(a, InteractionHand.OFF_HAND);
            helper.assertTrue(GuhQuests.count(a, TorenpeperFeature.PEPERVUURDRANKJE.get()) == 1 && Pepertuin.heeftDrankje(a), "bottled: a Pepervuurdrankje");
            Pepertuin.gebrouwen(a);       // (what TorenpeperEvents does every two seconds)
            helper.assertTrue(TUIN.stap(a) == 3, "a first pepper drink: step 3");
            // 3: he tastes: seeds, the OTHER drink, the peperslinger; once
            rol.talk(npc, a);
            Item slinger = ModItems.clothingItem(GuhClothes.TORENPEPER_PEPERSLINGER);
            helper.assertTrue(TUIN.klaar(a) && GuhQuests.count(a, zaad) == 2 + 3 + PepertelerRol.BELONING_ZAADJES, "done, and seeds for home: " + GuhQuests.count(a, zaad));
            helper.assertTrue(GuhQuests.count(a, TorenpeperFeature.PEPERZOETDRANKJE.get()) == 1 && GuhQuests.count(a, TorenpeperFeature.PEPERVUURDRANKJE.get()) == 1
                    && GuhQuests.count(a, slinger) == 1, "the other drink (he does not take yours) and the peperslinger");
            helper.assertTrue(GidsFeature.heeft(a, "barbecuether/toren_peper_pepertuin"), "the advancement");
            helper.assertTrue("toren_peper".equals(KledingBronnen.bron(GuhClothes.TORENPEPER_PEPERSLINGER))
                    && "toren_peper".equals(KledingBronnen.bron(GuhClothes.TORENPEPER_WACHTERSJAS)), "the source of both clothes");
            // afterwards: explanation; seeds only for who has none, once a day
            rol.talk(npc, a);
            rol.antwoord(npc, a, 2);
            rol.antwoord(npc, a, 3);
            helper.assertTrue(GuhQuests.count(a, zaad) == 2 + 3 + PepertelerRol.BELONING_ZAADJES && GuhQuests.count(a, slinger) == 1, "nothing twice, no seeds while you have some");
            GuhQuests.take(a, zaad, GuhQuests.count(a, zaad));
            rol.antwoord(npc, a, 3);
            helper.assertTrue(GuhQuests.count(a, zaad) == PepertelerRol.DAG_ZAADJES, "out of seeds: two new ones");
            GuhQuests.take(a, zaad, GuhQuests.count(a, zaad));
            rol.antwoord(npc, a, 3);
            helper.assertTrue(GuhQuests.count(a, zaad) == 0, "but only once a day");
            helper.assertTrue(TUIN.stap(b) == 0 && !TUIN.begonnen(b) && GuhQuests.count(b, zaad) == 0, "B's story did not move");
            // B drinks a pepper drink at step 2 without holding one any more: that counts as brewed too
            TUIN.zet(b, 2);
            Pepertuin.gebrouwen(b);
            helper.assertTrue(TUIN.stap(b) == 2, "no drink: still step 2");
            new ItemStack(TorenpeperFeature.PEPERZOETDRANKJE.get()).finishUsingItem(level, b);
            helper.assertTrue(TUIN.stap(b) == 3, "drunk: step 3");
        } finally {
            weg(helper, a, b);
            npc.discard();
        }
        helper.succeed();
    }

    /**
     * A real right-click of this player on this block with one of this item out of their pockets (the interact event fires,
     * then the block). With the off hand, so nothing in the pockets is in the way; what is left in it goes back.
     */
    private static void klikMet(GameTestHelper helper, ServerPlayer p, BlockPos abs, Item item) {
        GuhQuests.take(p, item, 1);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item));
        p.gameMode.useItemOn(p, helper.getLevel(), p.getOffhandItem(), InteractionHand.OFF_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        ItemStack over = p.getOffhandItem().copyAndClear();
        if (!over.isEmpty()) {
            p.getInventory().add(over);
        }
    }

    /**
     * The Guhbrouwketel of the kas is ONE pan for everybody. Whoever stirs a pepper of their own into it has brewed their
     * first pepper drink, whoever fills the bottles; a player whose powder and sauce went into somebody else's brew gets them
     * again from the Peperteler-guh (once a day), and one who ran out of peppers gets a seed. And the questline is done again,
     * from the first talk on, by somebody who only arrives when the others are finished.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void torenpeperGedeeldeKetel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(4, 2, 9)), b = speler(helper, new BlockPos(4, 2, 11)), c = speler(helper, new BlockPos(4, 2, 13)),
                d = speler(helper, new BlockPos(4, 2, 15));
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.PEPERTELERGUH, new BlockPos(3, 2, 9));
        NpcRole rol = NpcRollen.van(npc);
        Item poeder = SpiesburchtFeature.GRILLSPIESPOEDER.get(), saus = ModItems.KAAS_SAUS_BUCKET.get(), fles = Items.GLASS_BOTTLE;
        Item rood = TorenpeperFeature.VAHOEGPEPER.get(), roze = TorenpeperFeature.SNOEPPEPER.get(), zaad = TorenpeperFeature.PEPERZAADJES.get();
        Item vuur = TorenpeperFeature.PEPERVUURDRANKJE.get(), zoet = TorenpeperFeature.PEPERZOETDRANKJE.get();
        helper.setBlock(new BlockPos(8, 2, 9), SpiesburchtFeature.GUHBROUWKETEL.get());
        BlockPos pan = helper.absolutePos(new BlockPos(8, 2, 9));
        try {
            GuhbrouwketelBlockEntity ketel = (GuhbrouwketelBlockEntity) level.getBlockEntity(pan);
            // a and b both at the brewing step, each with the kit and the peppers of their own kweekbakken
            for (ServerPlayer p : List.of(a, b)) {
                TUIN.zet(p, 2);
                rol.talk(npc, p);
                p.getInventory().add(new ItemStack(rood, Kweek.PEPERS));
                helper.assertTrue(GuhQuests.count(p, poeder) == 1 && GuhQuests.count(p, saus) == 1 && GuhQuests.count(p, fles) == 3, "the brewing kit");
            }
            // a stokes the fire and pours the sauce; b is quicker with the pepper
            klikMet(helper, a, pan, poeder);
            klikMet(helper, a, pan, saus);
            helper.assertTrue(ketel.fuel() == GuhbrouwketelBlockEntity.BREWS_PER_POWDER && ketel.portions() == GuhbrouwketelBlockEntity.PORTIONS && TUIN.stap(a) == 2,
                    "a made the pan ready: no step for that");
            klikMet(helper, a, pan, TorenpeperFeature.NJEGPEPER.get());
            helper.assertTrue(TUIN.stap(a) == 2 && !ketel.isBrewing(), "(a green pepper brews nothing and counts for nothing)");
            klikMet(helper, b, pan, rood);
            helper.assertTrue(ketel.isBrewing() && GuhQuests.count(b, rood) == Kweek.PEPERS - 1, "b's pepper went into the pan that a made ready");
            helper.assertTrue(TUIN.stap(b) == 3 && TUIN.stap(a) == 2, "the pepper was b's: the step is b's, not a's");
            klikMet(helper, a, pan, rood);
            helper.assertTrue(TUIN.stap(a) == 2 && GuhQuests.count(a, rood) == Kweek.PEPERS, "a pepper the pan refuses (it bubbles already) counts for nothing");
            // a has no powder and no sauce left: the Peperteler-guh gives both again, once a day
            helper.assertTrue(GuhQuests.count(a, poeder) == 0 && GuhQuests.count(a, saus) == 0, "a's kit is in b's brew");
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, poeder) == 1 && GuhQuests.count(a, saus) == 1 && GuhQuests.count(a, fles) == 3,
                    "powder and sauce again (a still had the bottles)");
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, poeder) == 1 && GuhQuests.count(a, saus) == 1, "not while a has them");
            GuhQuests.take(a, poeder, 1);
            GuhQuests.take(a, saus, 1);
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, poeder) == 0 && GuhQuests.count(a, saus) == 0, "and only once a day");
            TUIN.teller(a, "pakket_dag", 0);
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, poeder) == 1 && GuhQuests.count(a, saus) == 1, "(another day: again)");
            // b taps the whole brew
            ketel.finishBrewing();
            for (int i = 0; i < 3; i++) {
                klikMet(helper, b, pan, fles);
            }
            helper.assertTrue(GuhQuests.count(b, vuur) == 3 && ketel.portions() == 0, "b filled three bottles: the pan is empty");
            // a brews in the same pan (its fire still burns): the moment a's own pepper goes in, the step is a's
            klikMet(helper, a, pan, saus);
            klikMet(helper, a, pan, rood);
            helper.assertTrue(ketel.isBrewing() && TUIN.stap(a) == 3 && !Pepertuin.heeftDrankje(a), "a's own pepper bubbles: a brewed, before any bottle is filled");
            // c, who has nothing to do with it, taps all of a's brew: nothing changes for a
            ketel.finishBrewing();
            c.getInventory().add(new ItemStack(fles, 3));
            for (int i = 0; i < 3; i++) {
                klikMet(helper, c, pan, fles);
            }
            helper.assertTrue(GuhQuests.count(c, vuur) == 3 && ketel.portions() == 0 && !Pepertuin.heeftDrankje(a) && TUIN.stap(a) == 3 && TUIN.stap(c) == 0,
                    "somebody else took every bottle of a's brew: a keeps the step");
            Item slinger = ModItems.clothingItem(GuhClothes.TORENPEPER_PEPERSLINGER);
            rol.talk(npc, a);
            helper.assertTrue(TUIN.klaar(a) && GuhQuests.count(a, slinger) == 1 && GuhQuests.count(a, zaad) == PepertelerRol.BELONING_ZAADJES
                    && GuhQuests.count(a, vuur) == 1, "a is done all the same: seeds, a bottle from the Peperteler-guh, the peperslinger");
            // b finishes after a
            rol.talk(npc, b);
            helper.assertTrue(TUIN.klaar(b) && GuhQuests.count(b, slinger) == 1 && GuhQuests.count(b, zoet) == 1, "b is done after a, with the other drink");
            // c only begins now: seeds, the same three kweekbakken, the kit, the same pan
            c.getInventory().clearContent();
            rol.talk(npc, c);
            rol.antwoord(npc, c, 1);
            helper.assertTrue(TUIN.stap(c) == 1 && GuhQuests.count(c, zaad) == 3 && Kweek.groei(c, PeperSoort.ROOD) == 0, "a newcomer: three seeds, empty troughs of their own");
            for (PeperSoort soort : PeperSoort.values()) {
                Kweek.plant(c, soort, 3L * Kweek.GROEI_TICKS);
                Kweek.klik(c, c.blockPosition(), soort);
            }
            helper.assertTrue(TUIN.stap(c) == 2 && GuhQuests.count(c, roze) == Kweek.PEPERS, "the three peppers");
            rol.talk(npc, c);
            klikMet(helper, c, pan, poeder);
            klikMet(helper, c, pan, saus);
            klikMet(helper, c, pan, roze);
            helper.assertTrue(TUIN.stap(c) == 3 && ketel.isBrewing(), "the newcomer brews in the pan the others used");
            ketel.finishBrewing();
            klikMet(helper, c, pan, fles);
            rol.talk(npc, c);
            helper.assertTrue(TUIN.klaar(c) && GuhQuests.count(c, zoet) == 1 && GuhQuests.count(c, vuur) == 1 && GuhQuests.count(c, slinger) == 1,
                    "and is done, with both drinks: any number of players, one after the other");
            // d is at the brewing step without a pepper, a seed or a plant: a seed (the kweekbakken are d's own)
            TUIN.zet(d, 2);
            rol.talk(npc, d);
            helper.assertTrue(GuhQuests.count(d, poeder) == 1 && GuhQuests.count(d, zaad) == 0, "the kit first");
            rol.talk(npc, d);
            helper.assertTrue(GuhQuests.count(d, zaad) == 1 && GuhQuests.count(d, poeder) == 1, "no pepper, no seed, nothing growing: a seed");
            rol.talk(npc, d);
            helper.assertTrue(GuhQuests.count(d, zaad) == 1, "not a second one");
            GuhQuests.take(d, zaad, 1);
            Kweek.plant(d, PeperSoort.ROZE, 0);
            rol.talk(npc, d);
            helper.assertTrue(GuhQuests.count(d, zaad) == 0, "nor while a pepper that brews is growing");
        } finally {
            weg(helper, a, b, c, d);
            npc.discard();
        }
        helper.succeed();
    }

    /** The two drinks brew from their peppers and do what they say; the green pepper brews nothing; a raw Vahoegpeper makes you run. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void torenpeperPepersEnDrankjes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(Brouwsel.forIngredient(new ItemStack(TorenpeperFeature.VAHOEGPEPER.get())) == Brouwsel.PEPERVUUR
                && Brouwsel.forIngredient(new ItemStack(TorenpeperFeature.SNOEPPEPER.get())) == Brouwsel.PEPERZOET
                && Brouwsel.forIngredient(new ItemStack(TorenpeperFeature.NJEGPEPER.get())) == null, "red brews Pepervuur, pink Peperzoet, green nothing");
        helper.assertTrue(Brouwsel.PEPERVUUR.drankje().is(TorenpeperFeature.PEPERVUURDRANKJE.get()) && Brouwsel.PEPERZOET.drankje().is(TorenpeperFeature.PEPERZOETDRANKJE.get())
                && Brouwsel.PEPERVUUR.heeftDrankje() && Brouwsel.PEPERZOET.heeftDrankje(), "both Brouwsels have their drankje");
        for (PeperSoort s : PeperSoort.values()) {
            helper.assertTrue(s.peper() instanceof PeperItems.Peper peper && peper.soort == s, "the pepper of " + s);
        }
        ServerPlayer p = speler(helper, new BlockPos(10, 2, 10));
        try {
            float hp = p.getHealth();
            p.setTicksFrozen(100);
            new ItemStack(TorenpeperFeature.PEPERVUURDRANKJE.get()).finishUsingItem(level, p);
            helper.assertTrue(p.hasEffect(MobEffects.HASTE) && p.hasEffect(TorenpeperFeature.PEPERADEM), "Pepervuur: haste and Peperadem");
            TorenpeperFeature.PEPERADEM.get().applyEffectTick(level, p, 0);
            helper.assertTrue(p.getTicksFrozen() == 0 && !p.isOnFire() && p.getHealth() == hp, "Peperadem: never cold, nothing burns, no harm");
            ItemStack rest = new ItemStack(TorenpeperFeature.PEPERZOETDRANKJE.get()).finishUsingItem(level, p);
            helper.assertTrue(p.hasEffect(MobEffects.REGENERATION) && p.hasEffect(MobEffects.ABSORPTION), "Peperzoet: regeneration and absorption");
            helper.assertTrue(rest.is(Items.GLASS_BOTTLE), "the bottle stays");
            p.removeAllEffects();
            new ItemStack(TorenpeperFeature.VAHOEGPEPER.get()).finishUsingItem(level, p);
            helper.assertTrue(p.hasEffect(MobEffects.SPEED) && p.getHealth() == hp && !p.isOnFire(), "a raw Vahoegpeper: a sprint, no harm");
            helper.assertTrue(GidsFeature.heeft(p, "barbecuether/toren_peper_heet"), "and its advancement");
            p.removeAllEffects();
            new ItemStack(TorenpeperFeature.NJEGPEPER.get()).finishUsingItem(level, p);
            helper.assertTrue(!p.hasEffect(MobEffects.SPEED), "the green one is mild");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the lamp
    // =================================================================================================================

    /** The lamp is lit by the questline's click and burns only while somebody who lit it is near; nothing is saved in the block. */
    @GuhTest(template = KAMER, batch = "torenpeper_lamp", timeoutTicks = 400)
    public static void torenpeperLampBrandtVoorWieHemAanstak(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos lamp = new BlockPos(10, 4, 10);
        helper.setBlock(lamp.below(2), Blocks.STONE);
        helper.setBlock(lamp.below(), Blocks.STONE);
        helper.setBlock(lamp, TorenpeperFeature.VUURTORENLAMP.get());
        BlockPos abs = helper.absolutePos(lamp);
        ServerPlayer a = speler(helper, new BlockPos(9, 2, 10)), b = speler(helper, new BlockPos(11, 2, 10));
        Item kooltje = TorenpeperFeature.LAMPKOOLTJE.get();
        Runnable opruimen = () -> weg(helper, a, b);
        try {
            helper.assertTrue(level.getBlockEntity(abs) instanceof VuurtorenlampBlock.Lamp && !Vuurtoren.brandt(level, abs), "a lamp, dark");
            klik(helper, a, lamp);
            helper.assertTrue(TOREN.stap(a) == 0 && !Vuurtoren.brandt(level, abs), "a click before the questline: nothing");
            TOREN.zet(a, 2);
            klik(helper, a, lamp);
            helper.assertTrue(TOREN.stap(a) == 2 && !Vuurtoren.brandt(level, abs), "step 2 without a lampkooltje: nothing");
            inHand(a, kooltje, 1);
            klik(helper, a, lamp);
            helper.assertTrue(TOREN.stap(a) == 3 && Vuurtoren.brandt(level, abs) && GuhQuests.count(a, kooltje) == 0, "with the lampkooltje: lit, step 3, the coal is used");
            helper.assertTrue(Vuurtoren.heeftAangestoken(a) && !Vuurtoren.heeftAangestoken(b) && Vuurtoren.moetBranden(level, abs), "it burns because of A");
            helper.assertTrue(abs.equals(Vuurtoren.lampBij(level, a.blockPosition(), 32)), "the lamp is known as a ticking lamp");
            helper.assertTrue(level.getBlockState(abs).getLightEmission(level, abs) == 15, "and it gives light");
        } catch (RuntimeException | Error e) {
            opruimen.run();
            throw e;
        }
        // A walks away: only B (who never lit one) is left, and the lamp goes out; A comes back: on again
        int[] fase = {0};
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertTrue(Vuurtoren.brandt(level, abs), "still burning with A next to it");
                a.snapTo(a.getX() + Vuurtoren.BEREIK + 40, a.getY(), a.getZ());
                helper.assertTrue(!Vuurtoren.moetBranden(level, abs), "A is far away");
                fase[0] = 1;
            } catch (RuntimeException | Error e) {
                opruimen.run();
                throw e;
            }
        });
        helper.onEachTick(() -> {
            if (fase[0] == 1 && !Vuurtoren.brandt(level, abs)) {
                // dark again for B, who clicks in vain
                klik(helper, b, lamp);
                try {
                    helper.assertTrue(TOREN.stap(b) == 0 && !Vuurtoren.brandt(level, abs), "B can't light it without the questline");
                } catch (RuntimeException | Error e) {
                    opruimen.run();
                    throw e;
                }
                a.snapTo(a.getX() - Vuurtoren.BEREIK - 40, a.getY(), a.getZ());
                fase[0] = 2;
            } else if (fase[0] == 2 && Vuurtoren.brandt(level, abs)) {
                fase[0] = 3;
                opruimen.run();
                helper.succeed();
            }
        });
    }

    /** The Torenwachter-guh's whole questline for one player; a second one's does not move; the rewards once. */
    @GuhTest(template = KAMER, batch = "torenpeper_wachter")
    public static void torenpeperTorenwachterLijn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(4, 2, 9)), b = speler(helper, new BlockPos(4, 2, 11));
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.TORENWACHTERGUH, new BlockPos(3, 2, 9));
        NpcRole rol = NpcRollen.van(npc);
        BlockPos lamp = new BlockPos(10, 3, 10);
        helper.setBlock(lamp.below(), Blocks.STONE);
        helper.setBlock(lamp, TorenpeperFeature.VUURTORENLAMP.get());
        Item gruis = BarbecuetherFeature.GLOEIKOOLGRUIS.get(), kooltje = TorenpeperFeature.LAMPKOOLTJE.get(), lantaarn = TorenpeperFeature.SEINLANTAARN.get();
        Item fluitje = TechbezorgFeature.BEZORGGUHTJE_FLUITJE.get(), jas = ModItems.clothingItem(GuhClothes.TORENPEPER_WACHTERSJAS);
        try {
            helper.assertTrue(rol instanceof TorenwachterRol, "the Torenwachter-guh has his role");
            helper.assertTrue(TOREN.stap(a) == 0 && TOREN.stappen() == 5, "five steps");
            rol.talk(npc, a);
            rol.antwoord(npc, a, -1);
            helper.assertTrue(TOREN.begonnen(a) && TOREN.stap(a) == 0, "closing the screen is not a yes");
            rol.antwoord(npc, a, 1);
            helper.assertTrue(TOREN.stap(a) == 1, "yes: step 1");
            // 1: four gloeikoolgruis for a lampkooltje and the seinlantaarn
            a.getInventory().add(new ItemStack(gruis, Vuurtoren.GRUIS_NODIG - 1));
            rol.talk(npc, a);
            helper.assertTrue(TOREN.stap(a) == 1 && GuhQuests.count(a, gruis) == Vuurtoren.GRUIS_NODIG - 1, "one short: nothing taken");
            a.getInventory().add(new ItemStack(gruis, 2));
            rol.talk(npc, a);
            helper.assertTrue(TOREN.stap(a) == 2 && GuhQuests.count(a, gruis) == 1 && GuhQuests.count(a, kooltje) == 1 && GuhQuests.count(a, lantaarn) == 1,
                    "four taken: the lampkooltje and the seinlantaarn");
            // 2: a lost lampkooltje comes back
            GuhQuests.take(a, kooltje, 1);
            rol.talk(npc, a);
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, kooltje) == 1, "lost: one new lampkooltje, not two");
            klik(helper, a, lamp);
            helper.assertTrue(TOREN.stap(a) == 3 && Vuurtoren.brandt(level, helper.absolutePos(lamp)), "the lamp is lit: step 3");
            // 3: a lost seinlantaarn comes back; three Rookguhs home
            GuhQuests.take(a, lantaarn, 1);
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, lantaarn) == 1 && TOREN.stap(a) == 3, "lost: a new seinlantaarn");
            VerdwaaldeRookguhEntity rookguh = TorenpeperFeature.VERDWAALDE_ROOKGUH.get().create(level, EntitySpawnReason.EVENT);
            for (int i = 1; i <= Vuurtoren.ROOKGUHS; i++) {
                Vuurtoren.thuisgekomen(a, rookguh);
                helper.assertTrue(Vuurtoren.thuis(a) == i && TOREN.stap(a) == (i < Vuurtoren.ROOKGUHS ? 3 : 4), "Rookguh " + i + " home");
            }
            Vuurtoren.thuisgekomen(a, rookguh);
            helper.assertTrue(Vuurtoren.thuis(a) == Vuurtoren.ROOKGUHS, "never more than three");
            // 4: the whistle and the coat, once
            rol.talk(npc, a);
            helper.assertTrue(TOREN.klaar(a) && GuhQuests.count(a, fluitje) == 1 && GuhQuests.count(a, jas) == 1, "done: the Bezorgguhtje-fluitje and the keeper's coat");
            helper.assertTrue(GidsFeature.heeft(a, "barbecuether/toren_peper_vuurtoren"), "the advancement");
            rol.talk(npc, a);
            rol.antwoord(npc, a, 2);
            rol.antwoord(npc, a, 3);
            helper.assertTrue(GuhQuests.count(a, fluitje) == 1 && GuhQuests.count(a, jas) == 1, "nothing twice, no whistle while you have one");
            GuhQuests.take(a, fluitje, 1);
            rol.antwoord(npc, a, 3);
            helper.assertTrue(GuhQuests.count(a, fluitje) == 1, "whistle lost: a new one");
            GuhQuests.take(a, fluitje, 1);
            rol.antwoord(npc, a, 3);
            helper.assertTrue(GuhQuests.count(a, fluitje) == 0, "but only once a day");
            helper.assertTrue(TOREN.stap(b) == 0 && !TOREN.begonnen(b) && GuhQuests.count(b, lantaarn) == 0, "B's story did not move");
            klik(helper, a, lamp);
            helper.assertTrue(TOREN.klaar(a), "clicking the lamp afterwards changes nothing");
            // B begins now that A is done: gloeikoolgruis, a lampkooltje of B's own for the lamp that already burns for A,
            // three lost Rookguhs of B's own, the same rewards
            BlockPos lampAbs = helper.absolutePos(lamp);
            rol.talk(npc, b);
            rol.antwoord(npc, b, 1);
            b.getInventory().add(new ItemStack(gruis, Vuurtoren.GRUIS_NODIG));
            rol.talk(npc, b);
            helper.assertTrue(TOREN.stap(b) == 2 && GuhQuests.count(b, kooltje) == 1 && GuhQuests.count(b, lantaarn) == 1, "B has a lampkooltje and a seinlantaarn too");
            klik(helper, b, lamp);
            helper.assertTrue(TOREN.stap(b) == 3 && GuhQuests.count(b, kooltje) == 0 && Vuurtoren.heeftAangestoken(b), "B lights the lamp for B, though it burns for A already");
            Vuurtoren.meld(level, lampAbs);
            for (int i = 0; i < 5; i++) {
                Vuurtoren.tik(a);
                Vuurtoren.tik(b);
            }
            List<VerdwaaldeRookguhEntity> vanB = Vuurtoren.van(level, b.getUUID(), lampAbs);
            helper.assertTrue(vanB.size() == Vuurtoren.ROOKGUHS && Vuurtoren.alleVan(level, a.getUUID()).isEmpty(),
                    "three lost Rookguhs for B, none for A (who brought theirs home long ago): " + vanB.size());
            for (int i = 1; i <= Vuurtoren.ROOKGUHS; i++) {
                Vuurtoren.thuisgekomen(b, vanB.get(i - 1));
                helper.assertTrue(Vuurtoren.thuis(b) == i && Vuurtoren.thuis(a) == Vuurtoren.ROOKGUHS, "B's Rookguh " + i + " home");
            }
            rol.talk(npc, b);
            helper.assertTrue(TOREN.klaar(b) && GuhQuests.count(b, fluitje) == 1 && GuhQuests.count(b, jas) == 1,
                    "the second player finishes the whole line after the first, with the same reward");
        } finally {
            weg(helper, a, b);
            npc.discard();
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the lost Rookguhs
    // =================================================================================================================

    /**
     * At step 3 a player gets three lost Rookguhs of their own and never a fourth; one follows its guide's seinlantaarn (not
     * somebody else's), sees the burning lamp, eats and floats home, which counts for its guide; without its guide it leaves.
     */
    @GuhTest(template = KAMER, batch = "torenpeper_rookguhs", timeoutTicks = 600)
    public static void torenpeperVerdwaaldeRookguhs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos lamp = new BlockPos(5, 6, 10);
        for (int y = 2; y < 6; y++) {
            helper.setBlock(new BlockPos(5, y, 10), Blocks.STONE);
        }
        helper.setBlock(lamp, TorenpeperFeature.VUURTORENLAMP.get());
        BlockPos abs = helper.absolutePos(lamp);
        ServerPlayer a = speler(helper, new BlockPos(17, 2, 4)), b = speler(helper, new BlockPos(17, 2, 16));
        TOREN.zet(a, 3);
        Vuurtoren.meld(level, abs);
        VerdwaaldeRookguhEntity[] r = new VerdwaaldeRookguhEntity[1];
        try {
            helper.assertTrue(Vuurtoren.van(level, a.getUUID(), abs).isEmpty(), "none yet");
            for (int i = 0; i < 5; i++) {
                Vuurtoren.tik(a);
                Vuurtoren.tik(b);
            }
            List<VerdwaaldeRookguhEntity> eigen = Vuurtoren.van(level, a.getUUID(), abs);
            helper.assertTrue(eigen.size() == Vuurtoren.ROOKGUHS, "three lost Rookguhs for A, never a fourth: " + eigen.size());
            helper.assertTrue(Vuurtoren.van(level, b.getUUID(), abs).isEmpty(), "none for B, who is not at that step");
            for (VerdwaaldeRookguhEntity e : eigen) {
                double d = Math.hypot(e.getX() - (abs.getX() + 0.5), e.getZ() - (abs.getZ() + 0.5));
                helper.assertTrue(e.gids().equals(a.getUUID()) && e.lamp().equals(abs) && d >= 6.5 && d <= Vuurtoren.START_MAX + 0.5, "a lost one starts away from the lamp: " + d);
                helper.assertTrue(e.getType() == TorenpeperFeature.VERDWAALDE_ROOKGUH.get() && e instanceof RookguhEntity && !e.isThuis(), "a small Rookguh, not home");
            }
            // keep one and put it right next to the burning lamp, B with a seinlantaarn beside it; it can't be hurt
            for (int i = 1; i < eigen.size(); i++) {
                eigen.get(i).discard();
            }
            r[0] = eigen.get(0);
            zet(helper, r[0], new BlockPos(9, 3, 10));
            zet(helper, b, new BlockPos(9, 2, 12));
            helper.assertTrue(!r[0].hurtServer(level, level.damageSources().playerAttack(a), 5f) && r[0].getHealth() == r[0].getMaxHealth(), "you don't hit a Rookguh");
            // the lamp must burn for it to come home: A lit it (step 3), so within a second it does
        } catch (RuntimeException | Error e) {
            weg(helper, a, b);
            throw e;
        }
        int[] fase = {0};
        int[] teller = {0};
        helper.runAfterDelay(590, () -> {
            VerdwaaldeRookguhEntity g = r[0];
            String waar = "fase " + fase[0] + ": alive " + g.isAlive() + ", volgt " + g.volgt() + ", eet " + g.eet() + ", thuis " + g.isThuis() + ", fed " + g.fed()
                    + ", to A " + g.distanceTo(a) + ", lamp " + Vuurtoren.brandt(level, abs) + ", stap " + TOREN.stap(a) + ", own " + Vuurtoren.alleVan(level, a.getUUID()).size();
            weg(helper, a, b);
            helper.assertTrue(false, "the lost Rookguh's story got stuck in " + waar);
        });
        helper.onEachTick(() -> {
            try {
                VerdwaaldeRookguhEntity g = r[0];
                switch (fase[0]) {
                    case 0 -> {
                        // B holds a seinlantaarn next to it: it is not B's, it does not follow; and nobody led it here, so the
                        // light next to it means nothing to it yet
                        inHand(b, TorenpeperFeature.SEINLANTAARN.get(), 1);
                        if (++teller[0] > 40) {
                            helper.assertTrue(Vuurtoren.brandt(level, abs), "the lamp burns (A lit it)");
                            helper.assertTrue(!g.volgt() && g.isAlive(), "it does not follow somebody else's lantern");
                            helper.assertTrue(!g.eet() && !g.isThuis() && !g.isGeleid() && g.fed() == 0, "a lost Rookguh never comes home by itself");
                            // now 10 blocks from A, who takes the lantern in hand
                            zet(helper, g, new BlockPos(17, 3, 14));
                            inHand(a, TorenpeperFeature.SEINLANTAARN.get(), 1);
                            fase[0] = 1;
                            teller[0] = 0;
                        }
                    }
                    case 1 -> {
                        // A holds the seinlantaarn: it comes
                        if (g.volgt() && g.distanceTo(a) < 6.0) {
                            helper.assertTrue(Vuurtoren.brandt(level, abs), "the lamp burns for A");
                            helper.assertTrue(Vuurtoren.thuis(a) == 0 && !g.eet(), "far from the lamp it is not home");
                            // A walks to the tower: the Rookguh is put where it would arrive (next to A, near the lamp)
                            zet(helper, a, new BlockPos(8, 2, 10));
                            zet(helper, g, new BlockPos(9, 3, 10));
                            fase[0] = 2;
                        }
                    }
                    case 2 -> {
                        if (g.isThuis()) {
                            helper.assertTrue(g.fed() >= RookguhEntity.NEEDED, "it ate its fill at the lamp");
                            fase[0] = 3;
                        }
                    }
                    case 3 -> {
                        if (Vuurtoren.thuis(a) == 1) {
                            helper.assertTrue(TOREN.stap(a) == 3, "one of three home");
                            // two more are made for A; without their guide they leave
                            Vuurtoren.tik(a);
                            Vuurtoren.tik(a);
                            Vuurtoren.tik(a);
                            List<VerdwaaldeRookguhEntity> nieuw = Vuurtoren.van(level, a.getUUID(), abs);
                            helper.assertTrue(nieuw.size() == 2, "two still to bring home: " + nieuw.size());
                            // (into the room, where the test world certainly ticks)
                            zet(helper, nieuw.get(0), new BlockPos(15, 4, 6));
                            zet(helper, nieuw.get(1), new BlockPos(15, 4, 15));
                            TOREN.zet(a, 4);
                            fase[0] = 4;
                        }
                    }
                    default -> {
                        if (Vuurtoren.alleVan(level, a.getUUID()).stream().allMatch(e -> e.isThuis() || !e.isAlive())) {
                            weg(helper, a, b);
                            helper.succeed();
                        }
                    }
                }
            } catch (RuntimeException | Error e) {
                weg(helper, a, b);
                throw e;
            }
        });
    }

    /**
     * A lost Rookguh that its guide never gets close to (a pocket of the cave, a ledge) can't keep a player at step 3 for
     * ever: when the guide has held the seinlantaarn nearby for a long time without it ever following, it hops to them.
     */
    @GuhTest(template = KAMER, batch = "torenpeper_zoek", timeoutTicks = 600)
    public static void torenpeperRookguhKomtNaLangZoeken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(19, 2, 19));
        TOREN.zet(a, 3);
        VerdwaaldeRookguhEntity r = TorenpeperFeature.VERDWAALDE_ROOKGUH.get().create(level, EntitySpawnReason.EVENT);
        zet(helper, r, new BlockPos(2, 3, 2));
        r.begin(a.getUUID(), helper.absolutePos(new BlockPos(2, 3, 2)).above(60));   // (its lamp is nowhere near: it can't come home in this test)
        level.addFreshEntity(r);
        inHand(a, TorenpeperFeature.SEINLANTAARN.get(), 1);
        int[] fase = {0};
        helper.runAfterDelay(60, () -> {
            try {
                helper.assertTrue(r.isAlive() && !r.volgt() && r.distanceTo(a) > VerdwaaldeRookguhEntity.VOLG_BEREIK,
                        "too far to see the lantern: it drifts where it is (" + r.distanceTo(a) + ")");
                r.zetGezocht(VerdwaaldeRookguhEntity.ZOEK_TICKS - 20);
                fase[0] = 1;
            } catch (RuntimeException | Error e) {
                weg(helper, a);
                throw e;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(fase[0] == 1 && r.isAlive() && r.distanceTo(a) < 8.0, "after a long search with the lantern it comes to its guide by itself");
            weg(helper, a);
        });
    }

    // =================================================================================================================
    // the buildings
    // =================================================================================================================

    /** Both templates hold what the Java side expects, where it expects it; structures, sets and Superkompas entries exist. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void torenpeperGebouwenBestaan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<StructureTemplate> toren = level.getStructureManager().get(Guhs.id(Vuurtoren.STRUCTUUR));
        Optional<StructureTemplate> tuin = level.getStructureManager().get(Guhs.id(Pepertuin.STRUCTUUR));
        helper.assertTrue(toren.isPresent() && tuin.isPresent(), "both templates");
        StructurePlaceSettings zo = new StructurePlaceSettings();
        // the lighthouse: one lamp, at LAMP, dark; a tower of at least 24 blocks above its ground
        List<StructureTemplate.StructureBlockInfo> lampen = toren.get().filterBlocks(BlockPos.ZERO, zo, TorenpeperFeature.VUURTORENLAMP.get());
        helper.assertTrue(lampen.size() == 1 && lampen.get(0).pos().equals(Vuurtoren.LAMP) && !lampen.get(0).state().getValue(VuurtorenlampBlock.LIT),
                "one dark lamp at " + Vuurtoren.LAMP);
        helper.assertTrue(toren.get().getSize().getY() >= Vuurtoren.G + 28 && Vuurtoren.LAMP.getY() >= Vuurtoren.G + 20, "a real tower: " + toren.get().getSize());
        helper.assertTrue(Vuurtoren.NPC.getY() == Vuurtoren.G + 1 && Vuurtoren.DEUR.getY() == Vuurtoren.G + 1 && Vuurtoren.BAK.getY() == Vuurtoren.LAMP.getY() - 1,
                "the keeper and the door on the ground, the gallery at the lamp");
        helper.assertTrue(Vuurtoren.LAMP.distSqr(new BlockPos(Vuurtoren.NPC.getX(), Vuurtoren.LAMP.getY(), Vuurtoren.NPC.getZ())) < Vuurtoren.THUIS_STRAAL * Vuurtoren.THUIS_STRAAL,
                "a Rookguh that follows you to the keeper is home");
        // the kas: three kweekbakken of the three kinds, the ketel, pepper plants on their own ground
        List<StructureTemplate.StructureBlockInfo> bakken = tuin.get().filterBlocks(BlockPos.ZERO, zo, TorenpeperFeature.KWEEKBAK.get());
        helper.assertTrue(bakken.size() == 3, "three kweekbakken: " + bakken.size());
        for (PeperSoort s : PeperSoort.values()) {
            helper.assertTrue(bakken.stream().anyMatch(i -> i.pos().equals(Pepertuin.BAKKEN.get(s.ordinal())) && i.state().getValue(KweekbakBlock.SOORT) == s
                    && i.state().getValue(KweekbakBlock.GROEI) == 0), "the kweekbak of " + s + " at " + Pepertuin.BAKKEN.get(s.ordinal()));
        }
        List<StructureTemplate.StructureBlockInfo> ketels = tuin.get().filterBlocks(BlockPos.ZERO, zo, SpiesburchtFeature.GUHBROUWKETEL.get());
        helper.assertTrue(ketels.size() == 1 && ketels.get(0).pos().equals(Pepertuin.KETEL), "the Guhbrouwketel at " + Pepertuin.KETEL);
        List<StructureTemplate.StructureBlockInfo> planten = tuin.get().filterBlocks(BlockPos.ZERO, zo, TorenpeperFeature.PEPERPLANT.get());
        for (PeperSoort s : PeperSoort.values()) {
            helper.assertTrue(planten.stream().filter(i -> i.state().getValue(PeperplantBlock.SOORT) == s && PeperplantBlock.isRijp(i.state())).count() >= 3,
                    "ripe " + s + " plants to pick");
        }
        // structures, sets, Superkompas
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (String structuur : List.of(Vuurtoren.STRUCTUUR, Pepertuin.STRUCTUUR)) {
            helper.assertTrue(Kopieen.structuur(level, structuur) != null, "the structure guhs:" + structuur);
            helper.assertTrue(sets.getValue(Guhs.id(structuur)) != null && sets.getValue(Guhs.id(structuur + "_gegarandeerd")) != null, "its set and its guaranteed copy");
            helper.assertTrue(SuperkompasItem.categoryOf(structuur) >= 0, structuur + " is in the Superkompas");
        }
        // the ground tags
        helper.assertTrue(PeperSoort.vanGrond(BarbecuetherFeature.GLOEIKOOL.get().defaultBlockState()) == PeperSoort.ROOD
                && PeperSoort.vanGrond(BarbecuetherFeature.PINDASAUS_NYLIUM.get().defaultBlockState()) == PeperSoort.ROZE
                && PeperSoort.vanGrond(BarbecuetherFeature.AS_AARDE.get().defaultBlockState()) == PeperSoort.GROEN
                && BarbecuetherFeature.AS_AARDE.get().defaultBlockState().is(TorenpeperFeature.PEPERGROND)
                && !Blocks.STONE.defaultBlockState().is(TorenpeperFeature.PEPERGROND), "the ground tags");
        helper.succeed();
    }
}
