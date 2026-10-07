package nl.juiced.guhs.feature.techmachine;

import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.core.component.DataComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.HuisjeMaat;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineDeelBlock;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.taal.NlTekst;

/**
 * Game tests of the guh machines (bbq2 tech-machines, batch "techmachine"; the trees in their own batches because they
 * are big): the Oogster cuts and replants, the Knabbelaar gnaws what an iron pickaxe gets through and nothing else, the
 * Neerzetter places, the Vadsmolen grinds its data recipes, the Bouwtekening and the Tekentafel, the Knutselmachine
 * crafts from a drawing (the pool's caps, leftovers), the Plantagebak grows EVERY sapling of the game into a tree and
 * chops it clean, the screens' menus, and the hover lines (Dutch as a player reads it; names of vanilla things are in the
 * server's English here). Templates techmachine_test_kamer (9 x 6 x 9) and
 * techmachine_test_bos (17 x 30 x 17): a stone floor, things stand at helper y 2.
 */
public class TechmachineGameTests {
    private static final String BATCH = "techmachine", KAMER = "techmachine_test_kamer", BOS = "techmachine_test_bos";

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    static BlockPos p(int x, int z) {
        return new BlockPos(x, 2, z);
    }

    static void bron(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 5));
    }

    /** A machine with its snoet to the north (towards z - 1) and a source behind it. */
    @SuppressWarnings("unchecked")
    static <T extends TechBlockEntity> T machine(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        bron(helper, pos.south());
        return (T) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    static void gelijk(GameTestHelper helper, Object verwacht, Object echt, String wat) {
        helper.assertTrue(verwacht.equals(echt), wat + ": expected " + verwacht + ", got " + echt);
    }

    static int tel(TechBlockEntity machine, net.minecraft.world.item.Item item) {
        return (int) Kisten.tel(machine.vakken(), s -> s.is(item));
    }

    static List<String> lees(GameTestHelper helper, BlockPos pos) {
        return VadsKracht.regels(helper.getLevel(), helper.absolutePos(pos)).stream().map(NlTekst::tekst).toList();
    }

    static ResourceHandler<ItemResource> buis(GameTestHelper helper, BlockPos pos) {
        ResourceHandler<ItemResource> h = Kisten.van(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(h != null, "the machine has the item capability");
        return h;
    }

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    // =====================================================================================================================
    // Oogster
    // =====================================================================================================================

    /** Ripe plants in the field are cut and stand there young again, the harvest is in the machine; unripe ones are left. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techmachineOogsterMaaitEnPlant(GameTestHelper helper) {
        // the field in front of the Oogster (it stands at z 7, looking north): farmland at its own height, the crops one up
        BlockState akker = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7);
        for (int x = 3; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 2, 5), akker);
            helper.setBlock(new BlockPos(x, 3, 5), ((CropBlock) Blocks.WHEAT).getStateForAge(7));
        }
        helper.setBlock(new BlockPos(2, 2, 5), akker);
        helper.setBlock(new BlockPos(2, 3, 5), ((CropBlock) Blocks.WHEAT).getStateForAge(3));        // not ripe: stays
        helper.setBlock(new BlockPos(6, 2, 4), akker);
        helper.setBlock(new BlockPos(6, 3, 4), ((CropBlock) Blocks.CARROTS).getStateForAge(7));
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.SOUL_SAND);
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 3));
        helper.setBlock(new BlockPos(5, 2, 3), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(5, 3, 3), Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
        helper.setBlock(new BlockPos(4, 2, 3), Blocks.PUMPKIN);       // grown on the stem north of it
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        helper.setBlock(new BlockPos(4, 2, 2), Blocks.ATTACHED_PUMPKIN_STEM.defaultBlockState().setValue(AttachedStemBlock.FACING, Direction.SOUTH));
        helper.setBlock(new BlockPos(6, 2, 2), Blocks.PUMPKIN);       // put there by somebody: no stem, no crop
        helper.setBlock(new BlockPos(2, 2, 3), Blocks.SAND);
        for (int y = 3; y <= 5; y++) {
            helper.setBlock(new BlockPos(2, y, 3), Blocks.CACTUS);   // three high: the top two are cut, one after the other
        }
        helper.setBlock(new BlockPos(4, 2, 8), Blocks.PUMPKIN);       // behind the machine: not its field
        OogsterBlockEntity oogster = machine(helper, p(4, 6), TechmachineFeature.OOGSTER.get());
        helper.assertTrue(lees(helper, p(4, 6)).contains("Oogst het veld van 5 bij 5 voor zijn snoet"), "the hover line: " + lees(helper, p(4, 6)));
        helper.succeedWhen(() -> {
            for (int x = 3; x <= 5; x++) {
                BlockState s = helper.getBlockState(new BlockPos(x, 3, 5));
                helper.assertTrue(s.is(Blocks.WHEAT) && s.getValue(CropBlock.AGE) == 0, "the wheat at x " + x + " is cut and planted again: " + s);
            }
            gelijk(helper, 3, helper.getBlockState(new BlockPos(2, 3, 5)).getValue(CropBlock.AGE), "the unripe wheat is left alone");
            helper.assertTrue(helper.getBlockState(new BlockPos(6, 3, 4)).getValue(CropBlock.AGE) == 0, "the carrots are planted again");
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 3, 3)).getValue(NetherWartBlock.AGE) == 0, "the nether wart is planted again");
            gelijk(helper, 1, helper.getBlockState(new BlockPos(5, 3, 3)).getValue(SweetBerryBushBlock.AGE), "the berries are picked, the bush stays");
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 3)).isAir(), "the pumpkin is cut");
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 2)).getBlock() instanceof net.minecraft.world.level.block.StemBlock,
                    "its stem stays (and grows the next one): " + helper.getBlockState(new BlockPos(4, 2, 2)));
            helper.assertBlockPresent(Blocks.PUMPKIN, new BlockPos(6, 2, 2));   // (a pumpkin without a stem is not a crop)
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 3, 3)).is(Blocks.CACTUS) && helper.getBlockState(new BlockPos(2, 4, 3)).isAir(),
                    "the cactus is cut down to its bottom piece");
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 8)).is(Blocks.PUMPKIN), "what is behind the machine is not its field");
            helper.assertTrue(tel(oogster, Items.WHEAT) >= 3, "the wheat is in the machine: " + tel(oogster, Items.WHEAT));
            helper.assertTrue(tel(oogster, Items.CARROT) >= 1 && tel(oogster, Items.NETHER_WART) >= 1 && tel(oogster, Items.SWEET_BERRIES) >= 2,
                    "carrots, nether wart and berries are in the machine");
            gelijk(helper, 1, tel(oogster, Items.PUMPKIN), "one pumpkin");
            gelijk(helper, 2, tel(oogster, Items.CACTUS), "two pieces of cactus");
            helper.assertBlockProperty(p(4, 6), MachineBlock.SNOET, Snoet.WERKT);
            // pipes take the harvest out, nothing goes in
            ResourceHandler<ItemResource> buis = buis(helper, p(4, 6));
            gelijk(helper, 5, Kisten.stop(buis, new ItemStack(Items.DIRT, 5)).getCount(), "nothing can be put into an Oogster");
            helper.assertTrue(!Kisten.neem(buis, s -> s.is(Items.WHEAT), 64, true).isEmpty(), "the wheat can be taken out");
        });
    }

    /** A full Oogster cuts nothing (the plant stays ripe) and looks surprised; with room again it goes on. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void techmachineOogsterVol(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 5), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        helper.setBlock(new BlockPos(4, 3, 5), ((CropBlock) Blocks.WHEAT).getStateForAge(7));
        OogsterBlockEntity oogster = machine(helper, p(4, 6), TechmachineFeature.OOGSTER.get());
        for (int i = 0; i < 9; i++) {
            oogster.vakken().set(i, ItemResource.of(Items.DIRT), 64);
        }
        helper.runAfterDelay(80, () -> {
            gelijk(helper, 7, helper.getBlockState(new BlockPos(4, 3, 5)).getValue(CropBlock.AGE), "a full Oogster leaves the wheat");
            helper.assertBlockProperty(p(4, 6), MachineBlock.SNOET, Snoet.VOL);
            helper.assertTrue(lees(helper, p(4, 6)).contains("Vol! Haal eruit wat erin zit, dan gaat hij verder."), "the hover says it is full");
            oogster.vakken().set(0, ItemResource.EMPTY, 0);
            oogster.vakken().set(1, ItemResource.EMPTY, 0);
            helper.succeedWhen(() -> {
                gelijk(helper, 0, helper.getBlockState(new BlockPos(4, 3, 5)).getValue(CropBlock.AGE), "with room again the wheat is cut");
                helper.assertTrue(tel(oogster, Items.WHEAT) >= 1, "and the wheat is in the machine");
                helper.assertBlockProperty(p(4, 6), MachineBlock.SNOET, Snoet.WERKT);
            });
        });
    }

    // =====================================================================================================================
    // Knabbelaar
    // =====================================================================================================================

    /** What it eats and what it does not (the rule itself). */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void techmachineKnabbelaarLust(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plek = helper.absolutePos(p(4, 4));
        for (Block lekker : new Block[] {Blocks.STONE, Blocks.COBBLESTONE, Blocks.IRON_ORE, Blocks.DIAMOND_ORE, Blocks.OAK_LOG, Blocks.DIRT, Blocks.SAND,
                Blocks.GLASS, Blocks.OAK_LEAVES, Blocks.PUMPKIN}) {
            helper.assertTrue(KnabbelaarBlockEntity.magKnabbelen(level, plek, lekker.defaultBlockState()), "it eats " + lekker);
        }
        for (Block bah : new Block[] {Blocks.AIR, Blocks.WATER, Blocks.LAVA, Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.ANCIENT_DEBRIS, Blocks.CHEST,
                Blocks.BARREL, Blocks.FURNACE, Blocks.HOPPER, Blocks.SPAWNER, Blocks.REINFORCED_DEEPSLATE, Blocks.BUDDING_AMETHYST, Blocks.END_PORTAL_FRAME,
                ModBlocks.GUH_WIRE.get(), ModBlocks.GUH_WHEEL.get(), ModBlocks.BANK_GUH.get(), VadskrachtFeature.MACHINE_DEEL.get(),
                TechmachineFeature.KNABBELAAR.get(), TechmachineFeature.PLANTAGEBAK.get(), TechmachineFeature.PLANTAGEBAK_DEEL.get(),
                nl.juiced.guhs.feature.huisje.HuisjeFeature.DEEL.get(),
                nl.juiced.guhs.feature.guhoven.GuhovenFeature.GUH_OVEN.get()}) {
            helper.assertTrue(!KnabbelaarBlockEntity.magKnabbelen(level, plek, bah.defaultBlockState()), "it never eats " + bah);
        }
        gelijk(helper, 40, KnabbelaarBlockEntity.TIJD, "one block per two seconds");
        gelijk(helper, VadsGetallen.KNABBELAAR, new KnabbelaarBlockEntity(plek, TechmachineFeature.KNABBELAAR.get().defaultBlockState()).vadsVraag(),
                "it asks its vadskracht");
        helper.succeed();
    }

    /** It gnaws the block in front of it away in two seconds, block after block, and keeps the drops of an iron pickaxe. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techmachineKnabbelaarKnabbelt(GameTestHelper helper) {
        BlockPos doel = p(4, 5);
        helper.setBlock(doel, Blocks.STONE);
        helper.setBlock(doel.above(), Blocks.GRAVEL);          // falls into its mouth when the stone is gone
        helper.setBlock(doel.above(2), Blocks.GRAVEL);
        KnabbelaarBlockEntity knabbelaar = machine(helper, p(4, 6), TechmachineFeature.KNABBELAAR.get());
        helper.assertTrue(!knabbelaar.bezig(), "it starts asleep");
        helper.runAfterDelay(30, () -> {
            helper.assertBlockPresent(Blocks.STONE, doel);     // (40 ticks of work per block: not yet)
            helper.assertTrue(knabbelaar.heeftKracht() && knabbelaar.bezig() && knabbelaar.voortgang() > 0, "it is gnawing: " + knabbelaar.voortgang());
            helper.assertBlockProperty(p(4, 6), MachineBlock.SNOET, Snoet.WERKT);
            helper.succeedWhen(() -> {
                helper.assertTrue(helper.getBlockState(doel).isAir() && helper.getBlockState(doel.above()).isAir(), "the stone and both gravel are gone");
                gelijk(helper, 1, tel(knabbelaar, Items.COBBLESTONE), "stone gives cobblestone, as with a pickaxe");
                gelijk(helper, 2, tel(knabbelaar, Items.GRAVEL) + tel(knabbelaar, Items.FLINT), "two gravel (or flint)");
                helper.assertTrue(helper.getTick() >= 3 * KnabbelaarBlockEntity.TIJD, "three blocks take at least six seconds, not " + helper.getTick() + " ticks");
            });
        });
    }

    /** Containers, machines and blocks that are too hard stay; a full Knabbelaar stops with the block almost through. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techmachineKnabbelaarBlijftAf(GameTestHelper helper) {
        helper.setBlock(p(2, 5), Blocks.CHEST);
        KnabbelaarBlockEntity bijKist = machine(helper, p(2, 6), TechmachineFeature.KNABBELAAR.get());
        helper.setBlock(p(4, 5), Blocks.OBSIDIAN);
        KnabbelaarBlockEntity bijObsidiaan = machine(helper, p(4, 6), TechmachineFeature.KNABBELAAR.get());
        helper.setBlock(p(6, 5), Blocks.COBBLESTONE);
        KnabbelaarBlockEntity vol = machine(helper, p(6, 6), TechmachineFeature.KNABBELAAR.get());
        for (int i = 0; i < 9; i++) {
            vol.vakken().set(i, ItemResource.of(Items.DIRT), 64);
        }
        helper.runAfterDelay(100, () -> {
            helper.assertBlockPresent(Blocks.CHEST, p(2, 5));
            helper.assertBlockPresent(Blocks.OBSIDIAN, p(4, 5));
            helper.assertBlockPresent(Blocks.COBBLESTONE, p(6, 5));
            helper.assertTrue(!bijKist.bezig() && !bijObsidiaan.bezig(), "nothing to gnaw: they stand still");
            helper.assertTrue(lees(helper, p(2, 6)).contains("Chest lust hij niet"), "the hover says why: " + lees(helper, p(2, 6)));
            helper.assertBlockProperty(p(6, 6), MachineBlock.SNOET, Snoet.VOL);
            vol.vakken().set(3, ItemResource.EMPTY, 0);
            helper.succeedWhen(() -> {
                helper.assertTrue(helper.getBlockState(p(6, 5)).isAir(), "with room again it eats the cobblestone");
                gelijk(helper, 1, tel(vol, Items.COBBLESTONE), "and keeps it");
            });
        });
    }

    // =====================================================================================================================
    // somebody else's huisje
    // =====================================================================================================================

    /**
     * In the huisje area of another player the machines keep their snoet off everything (whoever placed them, or nobody);
     * the machine of the huisje's owner works there as anywhere. (Its own batch: the area covers the whole room and more.)
     */
    @GuhTest(template = KAMER, batch = "techmachine_huisje", timeoutTicks = 400)
    public static void techmachineAndermansHuisje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID buur = UUID.randomUUID(), vreemde = UUID.randomUUID();
        BlockPos huisje = helper.absolutePos(new BlockPos(4, 2, 1));
        HuisjeBlock.bouw(level, huisje, Direction.SOUTH, HuisjeMaat.KLEIN, buur);
        Runnable opruimen = () -> level.destroyBlock(huisje, false);
        try {
            // a stranger's Knabbelaar at a stone
            helper.setBlock(p(1, 5), Blocks.STONE);
            KnabbelaarBlockEntity knabbelaar = machine(helper, p(1, 6), TechmachineFeature.KNABBELAAR.get());
            knabbelaar.zetEigenaar(vreemde);
            // nobody's Neerzetter (put there by a command) with planks
            NeerzetterBlockEntity zetter = machine(helper, p(3, 6), TechmachineFeature.NEERZETTER.get());
            zetter.vakken().set(0, ItemResource.of(Items.OAK_PLANKS), 2);
            // a stranger's Oogster at ripe wheat
            helper.setBlock(new BlockPos(5, 2, 5), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
            helper.setBlock(new BlockPos(5, 3, 5), ((CropBlock) Blocks.WHEAT).getStateForAge(7));
            OogsterBlockEntity oogster = machine(helper, p(5, 6), TechmachineFeature.OOGSTER.get());
            oogster.zetEigenaar(vreemde);
            // the neighbour's own Knabbelaar at a stone
            helper.setBlock(p(7, 5), Blocks.STONE);
            KnabbelaarBlockEntity eigen = machine(helper, p(7, 6), TechmachineFeature.KNABBELAAR.get());
            eigen.zetEigenaar(buur);
            helper.runAfterDelay(120, () -> {
                try {
                    helper.assertBlockPresent(Blocks.STONE, p(1, 5));
                    helper.assertTrue(!knabbelaar.bezig() && knabbelaar.voortgang() == 0, "a stranger's Knabbelaar does not even start");
                    helper.assertTrue(lees(helper, p(1, 6)).contains("Hier mag hij niks veranderen: dit is beschermd of van iemand anders. Njeg."),
                            "the hover says why: " + lees(helper, p(1, 6)));
                    helper.assertTrue(helper.getBlockState(p(3, 5)).isAir(), "nobody's Neerzetter places nothing here");
                    gelijk(helper, 2, tel(zetter, Items.OAK_PLANKS), "and keeps its planks");
                    gelijk(helper, 7, helper.getBlockState(new BlockPos(5, 3, 5)).getValue(CropBlock.AGE), "a stranger's Oogster leaves the wheat");
                    gelijk(helper, 0, tel(oogster, Items.WHEAT), "and has nothing");
                    helper.assertTrue(helper.getBlockState(p(7, 5)).isAir(), "the owner's own Knabbelaar ate its stone");
                    gelijk(helper, 1, tel(eigen, Items.COBBLESTONE), "and keeps it");
                } finally {
                    opruimen.run();
                }
                // the huisje is gone: now the stranger's Knabbelaar eats (the answer is remembered for a second, then two of gnawing)
                helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(p(1, 5)).isAir(), "without the huisje the stone is eaten"));
            });
        } catch (RuntimeException e) {
            opruimen.run();
            throw e;
        }
    }

    // =====================================================================================================================
    // Neerzetter
    // =====================================================================================================================

    /** It puts its blocks down in front of it, one at a time, only where there is room; seeds only where they can grow. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techmachineNeerzetter(GameTestHelper helper) {
        NeerzetterBlockEntity zetter = machine(helper, p(2, 6), TechmachineFeature.NEERZETTER.get());
        ResourceHandler<ItemResource> buis = buis(helper, p(2, 6));
        gelijk(helper, 4, Kisten.stop(buis, new ItemStack(Items.STICK, 4)).getCount(), "a stick is no block: it does not go in");
        gelijk(helper, 0, Kisten.stop(buis, new ItemStack(Items.OAK_PLANKS, 3)).getCount(), "planks go in");
        helper.assertTrue(Kisten.neem(buis, s -> true, 64).isEmpty(), "a pipe takes nothing out of a Neerzetter");
        // seeds: one Neerzetter looks at farmland, one at stone
        helper.setBlock(new BlockPos(4, 1, 5), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        NeerzetterBlockEntity zaaier = machine(helper, p(4, 6), TechmachineFeature.NEERZETTER.get());
        zaaier.vakken().set(0, ItemResource.of(Items.WHEAT_SEEDS), 2);
        NeerzetterBlockEntity opSteen = machine(helper, p(6, 6), TechmachineFeature.NEERZETTER.get());
        opSteen.vakken().set(0, ItemResource.of(Items.WHEAT_SEEDS), 2);
        helper.runAfterDelay(70, () -> {
            helper.assertBlockPresent(Blocks.OAK_PLANKS, p(2, 5));
            gelijk(helper, 2, tel(zetter, Items.OAK_PLANKS), "one plank placed, the spot is taken now");
            helper.assertBlockProperty(p(2, 6), MachineBlock.SNOET, Snoet.VOL);
            helper.assertTrue(lees(helper, p(2, 6)).contains("Er staat al iets voor zijn snoet"), "the hover: " + lees(helper, p(2, 6)));
            helper.assertBlockPresent(Blocks.WHEAT, p(4, 5));
            gelijk(helper, 1, tel(zaaier, Items.WHEAT_SEEDS), "one seed sown on the farmland");
            helper.assertTrue(helper.getBlockState(p(6, 5)).isAir(), "seeds do not go on stone");
            gelijk(helper, 2, tel(opSteen, Items.WHEAT_SEEDS), "and stay in the machine");
            helper.assertTrue(lees(helper, p(6, 6)).contains("Dit blok kan hier niet staan"), "the hover: " + lees(helper, p(6, 6)));
            helper.setBlock(p(2, 5), Blocks.AIR);
            helper.succeedWhen(() -> {
                helper.assertBlockPresent(Blocks.OAK_PLANKS, p(2, 5));
                gelijk(helper, 1, tel(zetter, Items.OAK_PLANKS), "the spot was free again: the next plank");
            });
        });
    }

    // =====================================================================================================================
    // Vadsmolen
    // =====================================================================================================================

    /** Two blocks high; grinds graan and bones by its data recipes; refuses what it cannot grind; a full sack surprises it. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techmachineVadsmolen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        VadsmolenBlockEntity molen = machine(helper, p(2, 6), TechmachineFeature.VADSMOLEN.get());
        helper.assertBlockPresent(VadskrachtFeature.MACHINE_DEEL.get(), p(2, 6).above());
        gelijk(helper, helper.absolutePos(p(2, 6)), MachineDeelBlock.kern(helper.getBlockState(p(2, 6).above()), helper.absolutePos(p(2, 6).above())),
                "the top block belongs to the molen");
        helper.assertTrue(Maalrecepten.alle().size() >= 5, "the grinding recipes are loaded: " + Maalrecepten.alle().size());
        ResourceHandler<ItemResource> buis = buis(helper, p(2, 6));
        gelijk(helper, 3, Kisten.stop(buis, new ItemStack(Items.DIAMOND, 3)).getCount(), "it does not grind diamonds");
        gelijk(helper, 0, Kisten.stop(buis, new ItemStack(nl.juiced.guhs.feature.tuintjes.TuintjesFeature.KNABBELGRAAN.get(), 4)).getCount(), "graan goes in");
        VadsmolenBlockEntity botten = machine(helper, p(4, 6), TechmachineFeature.VADSMOLEN.get());
        botten.vakken().set(VadsmolenBlockEntity.IN, ItemResource.of(Items.BONE), 2);
        VadsmolenBlockEntity vol = machine(helper, p(6, 6), TechmachineFeature.VADSMOLEN.get());
        vol.vakken().set(VadsmolenBlockEntity.IN, ItemResource.of(Items.BONE), 2);
        vol.vakken().set(VadsmolenBlockEntity.UIT, ItemResource.of(Items.DIRT), 1);
        helper.runAfterDelay(60, () -> {
            helper.assertBlockProperty(p(6, 6), MachineBlock.SNOET, Snoet.VOL);
            gelijk(helper, 2, vol.vakken().getAmountAsInt(VadsmolenBlockEntity.IN), "a molen with a full sack grinds nothing");
        });
        helper.succeedWhen(() -> {
            gelijk(helper, 4, tel(molen, nl.juiced.guhs.feature.guhpolder.GuhpolderFeature.KNABBELMEEL.get()), "four graan are four meel");
            gelijk(helper, 0, molen.vakken().getAmountAsInt(VadsmolenBlockEntity.IN), "the graan is gone");
            gelijk(helper, 8, tel(botten, Items.BONE_MEAL), "two bones are eight bone meal");
            helper.assertTrue(helper.getTick() >= 4 * 20 && helper.getTick() > 60, "a second per graan: four take at least 80 ticks");
            gelijk(helper, 4, Kisten.neem(buis, s -> true, 64, true).getCount(), "a pipe takes the meel out");
            // breaking the molen takes its top block along
            level.destroyBlock(helper.absolutePos(p(4, 6)), false);
            helper.assertTrue(helper.getBlockState(p(4, 6).above()).isAir(), "the top block goes with the molen");
        });
    }

    // =====================================================================================================================
    // Bouwtekening + Tekentafel
    // =====================================================================================================================

    private static List<ItemStack> kistRooster() {
        List<ItemStack> rooster = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            rooster.add(i == 4 ? ItemStack.EMPTY : new ItemStack(Items.OAK_PLANKS, 5));
        }
        return rooster;
    }

    /** A drawing holds the grid, the result and the recipe; it survives saving, two equal ones stack, others can read it. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void techmachineBouwtekening(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack leeg = new ItemStack(TechmachineFeature.BOUWTEKENING.get());
        helper.assertTrue(Bouwtekeningen.isLeeg(leeg) && Bouwtekeningen.lees(leeg) == null, "a new sheet is empty");
        gelijk(helper, "Lege bouwtekening", NlTekst.tekst(leeg.getHoverName()), "its name");
        ItemStack stack = Bouwtekeningen.teken(level, kistRooster());
        Bouwtekening t = Bouwtekeningen.lees(stack);
        helper.assertTrue(t != null && !Bouwtekeningen.isLeeg(stack), "a drawing of a chest");
        helper.assertTrue(t.resultaat().is(Items.CHEST) && t.resultaat().getCount() == 1, "it makes one chest: " + t.resultaat());
        gelijk(helper, 9, t.rooster().size(), "nine cells");
        helper.assertTrue(t.vakje(4).isEmpty() && t.vakje(0).is(Items.OAK_PLANKS) && t.vakje(0).getCount() == 1, "the cells hold ONE of what lay there");
        List<ItemStack> nodig = t.ingredienten();
        helper.assertTrue(nodig.size() == 1 && nodig.get(0).is(Items.OAK_PLANKS) && nodig.get(0).getCount() == 8, "it takes eight planks: " + nodig);
        gelijk(helper, 8, t.nodig(new ItemStack(Items.OAK_PLANKS)), "nodig(planks)");
        gelijk(helper, 0, t.nodig(new ItemStack(Items.BIRCH_PLANKS)), "the drawing is exact: birch is not in it");
        helper.assertTrue(t.recept().isPresent() && t.recept().get().identifier().getPath().equals("chest"), "the recipe id is drawn too: " + t.recept());
        helper.assertTrue(Bouwtekeningen.recept(level, t) != null, "the recipe behind it is found");
        gelijk(helper, "Bouwtekening: Chest", NlTekst.tekst(stack.getHoverName()), "its name says what it makes");
        // saved and read back: the same drawing
        var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag tag = Bouwtekening.CODEC.encodeStart(ops, t).getOrThrow();
        Bouwtekening terug = Bouwtekening.CODEC.parse(ops, tag).getOrThrow();
        helper.assertTrue(terug.equals(t) && terug.hashCode() == t.hashCode(), "the drawing survives saving");
        helper.assertTrue(ItemStack.isSameItemSameComponents(stack, Bouwtekeningen.teken(level, kistRooster())), "two drawings of the same recipe stack");
        helper.assertTrue(!ItemStack.isSameItemSameComponents(stack, Bouwtekeningen.teken(level, List.of(new ItemStack(Items.OAK_LOG)))),
                "a drawing of something else does not");
        // nothing to draw
        helper.assertTrue(Bouwtekeningen.teken(level, List.of(new ItemStack(Items.DIRT), new ItemStack(Items.STICK))).isEmpty(), "no recipe: no drawing");
        helper.assertTrue(Bouwtekeningen.maak(level, List.of()) == null, "an empty grid: no drawing");
        helper.succeed();
    }

    /** At the table: a recipe on the grid + a sheet = a drawing; taking it costs one sheet and nothing of the grid. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void techmachineTekentafel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(p(4, 4), TechmachineFeature.TEKENTAFEL.get());
        BlockPos abs = helper.absolutePos(p(4, 4));
        ServerPlayer speler = speler(helper);
        helper.assertBlockProperty(p(4, 4), TekentafelBlock.SNOET, Snoet.SLAAPT);
        TekentafelMenu menu = new TekentafelMenu(1, speler.getInventory(), ContainerLevelAccess.create(level, abs), abs);
        speler.containerMenu = menu;
        helper.assertBlockProperty(p(4, 4), TekentafelBlock.SNOET, Snoet.WERKT);
        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                menu.getSlot(TekentafelMenu.ROOSTER + i).set(new ItemStack(Items.OAK_PLANKS, 2));
            }
        }
        helper.assertTrue(!menu.getSlot(TekentafelMenu.RESULTAAT).hasItem(), "a recipe but no sheet: nothing to take");
        helper.assertBlockProperty(p(4, 4), TekentafelBlock.SNOET, Snoet.VOL);   // "oh, that one I know!"
        helper.assertTrue(!menu.getSlot(TekentafelMenu.VEL).mayPlace(new ItemStack(Items.PAPER)), "only Bouwtekeningen go into the sheet slot");
        menu.getSlot(TekentafelMenu.VEL).set(new ItemStack(TechmachineFeature.BOUWTEKENING.get(), 3));
        Bouwtekening t = Bouwtekeningen.lees(menu.getSlot(TekentafelMenu.RESULTAAT).getItem());
        helper.assertTrue(t != null && t.resultaat().is(Items.CHEST), "the result is a drawing of a chest");
        // shift-click: every sheet becomes a drawing, the grid keeps its planks
        menu.quickMoveStack(speler, TekentafelMenu.RESULTAAT);
        menu.quickMoveStack(speler, TekentafelMenu.RESULTAAT);
        menu.quickMoveStack(speler, TekentafelMenu.RESULTAAT);
        gelijk(helper, 3, speler.getInventory().countItem(TechmachineFeature.BOUWTEKENING.get()), "three drawings in the pockets");
        int getekend = 0;
        for (int i = 0; i < speler.getInventory().getContainerSize(); i++) {
            Bouwtekening op = Bouwtekeningen.lees(speler.getInventory().getItem(i));
            if (op != null && op.equals(t)) {
                getekend += speler.getInventory().getItem(i).getCount();
            }
        }
        gelijk(helper, 3, getekend, "all three are drawings of the chest");
        helper.assertTrue(!menu.getSlot(TekentafelMenu.VEL).hasItem() && !menu.getSlot(TekentafelMenu.RESULTAAT).hasItem(), "the sheets are used up");
        gelijk(helper, 2, menu.getSlot(TekentafelMenu.ROOSTER).getItem().getCount(), "the planks on the grid are not used");
        // a drawn sheet can be drawn over
        menu.getSlot(TekentafelMenu.VEL).set(Bouwtekeningen.teken(level, List.of(new ItemStack(Items.OAK_LOG))));
        Bouwtekening over = Bouwtekeningen.lees(menu.getSlot(TekentafelMenu.RESULTAAT).getItem());
        helper.assertTrue(over != null && over.resultaat().is(Items.CHEST), "an old drawing is drawn over");
        // closing: everything on the table goes back, the table falls asleep
        menu.removed(speler);
        speler.containerMenu = speler.inventoryMenu;
        gelijk(helper, 16, speler.getInventory().countItem(Items.OAK_PLANKS), "the planks are back in the pockets");
        helper.assertBlockProperty(p(4, 4), TekentafelBlock.SNOET, Snoet.SLAAPT);
        weg(helper, speler);
        helper.succeed();
    }

    // =====================================================================================================================
    // Knutselmachine
    // =====================================================================================================================

    /** It crafts what its drawing shows from the pool; the pool takes only the ingredients, and never too many of one. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techmachineKnutselmachine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KnutselmachineBlockEntity machine = machine(helper, p(2, 6), TechmachineFeature.KNUTSELMACHINE.get());
        ResourceHandler<ItemResource> buis = buis(helper, p(2, 6));
        gelijk(helper, 8, Kisten.stop(buis, new ItemStack(Items.OAK_PLANKS, 8)).getCount(), "without a drawing it takes nothing");
        ItemStack tekening = Bouwtekeningen.teken(level, kistRooster());
        gelijk(helper, 1, Kisten.stop(buis, tekening).getCount(), "a pipe cannot put a drawing in");
        helper.assertTrue(machine.magErin(KnutselmachineBlockEntity.TEKENING, ItemResource.of(tekening)), "by hand a drawing goes into its slot");
        helper.assertTrue(!machine.magErin(KnutselmachineBlockEntity.TEKENING, ItemResource.of(Items.PAPER)), "paper does not");
        machine.vakken().set(KnutselmachineBlockEntity.TEKENING, ItemResource.of(tekening), 1);
        gelijk(helper, 5, Kisten.stop(buis, new ItemStack(Items.BIRCH_PLANKS, 5)).getCount(), "what is not on the drawing does not go in");
        gelijk(helper, 36, Kisten.stop(buis, new ItemStack(Items.OAK_PLANKS, 100)).getCount(), "of an ingredient at most a stack (8 crafts' worth) goes in");
        gelijk(helper, 64, machine.inVoorraad(new ItemStack(Items.OAK_PLANKS)), "64 planks in the pool");
        helper.assertTrue(lees(helper, p(2, 6)).contains("Knutselt 1× Chest"), "the hover says what it makes: " + lees(helper, p(2, 6)));
        helper.succeedWhen(() -> {
            gelijk(helper, 8, tel(machine, Items.CHEST), "64 planks are eight chests");
            gelijk(helper, 0, machine.inVoorraad(new ItemStack(Items.OAK_PLANKS)), "the planks are used up");
            helper.assertTrue(lees(helper, p(2, 6)).contains("Er ontbreekt nog 8× Oak Planks"), "the hover says what is missing: " + lees(helper, p(2, 6)));
            gelijk(helper, 8, Kisten.neem(buis, s -> s.is(Items.CHEST), 64, true).getCount(), "a pipe takes the chests out");
            helper.assertTrue(Kisten.neem(buis, s -> s.is(TechmachineFeature.BOUWTEKENING.get()), 1, true).isEmpty(), "but never the drawing");
            helper.assertTrue(helper.getTick() >= 8 * KnutselmachineBlockEntity.TIJD, "two seconds per craft");
        });
    }

    /** Leftovers of crafting (empty buckets) come out with the result; full out slots stop the machine without losing anything. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techmachineKnutselmachineRestjes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KnutselmachineBlockEntity taart = machine(helper, p(2, 6), TechmachineFeature.KNUTSELMACHINE.get());
        List<ItemStack> rooster = List.of(new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.MILK_BUCKET),
                new ItemStack(Items.SUGAR), new ItemStack(Items.EGG), new ItemStack(Items.SUGAR),
                new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT));
        ItemStack tekening = Bouwtekeningen.teken(level, rooster);
        Bouwtekening t = Bouwtekeningen.lees(tekening);
        helper.assertTrue(t != null && t.resultaat().is(Items.CAKE), "a drawing of a cake");
        gelijk(helper, 4, t.ingredienten().size(), "four different ingredients");
        taart.vakken().set(KnutselmachineBlockEntity.TEKENING, ItemResource.of(tekening), 1);
        ResourceHandler<ItemResource> buis = buis(helper, p(2, 6));
        // buckets do not stack: exactly what one craft needs goes in, the fourth stays outside
        int emmers = 0;
        for (int i = 0; i < 4; i++) {
            emmers += Kisten.stop(buis, new ItemStack(Items.MILK_BUCKET)).isEmpty() ? 1 : 0;
        }
        gelijk(helper, 3, emmers, "three milk buckets go in, not four");
        gelijk(helper, 0, Kisten.stop(buis, new ItemStack(Items.SUGAR, 4)).getCount(), "sugar");
        gelijk(helper, 0, Kisten.stop(buis, new ItemStack(Items.EGG, 2)).getCount(), "eggs");
        gelijk(helper, 0, Kisten.stop(buis, new ItemStack(Items.WHEAT, 6)).getCount(), "wheat");
        // a second one whose out slots are full
        KnutselmachineBlockEntity vol = machine(helper, p(5, 6), TechmachineFeature.KNUTSELMACHINE.get());
        vol.vakken().set(KnutselmachineBlockEntity.TEKENING, ItemResource.of(Bouwtekeningen.teken(level, kistRooster())), 1);
        vol.vakken().set(KnutselmachineBlockEntity.VOORRAAD, ItemResource.of(Items.OAK_PLANKS), 8);
        for (int i = KnutselmachineBlockEntity.UIT; i <= KnutselmachineBlockEntity.UIT_TOT; i++) {
            vol.vakken().set(i, ItemResource.of(Items.DIRT), 64);
        }
        helper.runAfterDelay(70, () -> {
            gelijk(helper, 1, tel(taart, Items.CAKE), "one cake (there was milk for one)");
            gelijk(helper, 3, tel(taart, Items.BUCKET), "the three empty buckets came out with it");
            gelijk(helper, 0, tel(taart, Items.MILK_BUCKET), "the milk is used");
            gelijk(helper, 2, tel(taart, Items.SUGAR), "sugar for one more is left");
            helper.assertTrue(lees(helper, p(2, 6)).contains("Er ontbreekt nog 3× Milk Bucket"), "the hover: " + lees(helper, p(2, 6)));
            helper.assertBlockProperty(p(5, 6), MachineBlock.SNOET, Snoet.VOL);
            gelijk(helper, 8, vol.inVoorraad(new ItemStack(Items.OAK_PLANKS)), "a full machine uses nothing");
            vol.vakken().set(KnutselmachineBlockEntity.UIT, ItemResource.EMPTY, 0);
            helper.succeedWhen(() -> {
                gelijk(helper, 1, tel(vol, Items.CHEST), "with room again the chest is made");
                gelijk(helper, 0, vol.inVoorraad(new ItemStack(Items.OAK_PLANKS)), "from the eight planks");
            });
        });
    }

    // =====================================================================================================================
    // the menu of the machines
    // =====================================================================================================================

    /** Shift-click puts blocks into a Neerzetter and takes the harvest out of an Oogster; out slots take nothing by hand. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void techmachineMenu(GameTestHelper helper) {
        ServerPlayer speler = speler(helper);
        NeerzetterBlockEntity zetter = machine(helper, p(2, 6), TechmachineFeature.NEERZETTER.get());
        helper.setBlock(p(2, 6).south(), Blocks.AIR);          // (no vadskracht: nothing moves by itself during this test)
        speler.getInventory().setItem(9, new ItemStack(Items.OAK_PLANKS, 10));
        speler.getInventory().setItem(10, new ItemStack(Items.STICK, 10));
        MachineMenu menu = (MachineMenu) zetter.createMenu(1, speler.getInventory(), speler);
        gelijk(helper, MachineSoort.NEERZETTER, menu.soort, "the menu of a Neerzetter");
        gelijk(helper, 9 + 36, menu.slots.size(), "nine slots and the inventory");
        menu.quickMoveStack(speler, 9);        // the planks (first inventory slot)
        menu.quickMoveStack(speler, 10);       // the sticks: no block, they only hop to the hotbar
        gelijk(helper, 10, tel(zetter, Items.OAK_PLANKS), "the planks are in the machine");
        gelijk(helper, 0, tel(zetter, Items.STICK), "the sticks are not");
        gelijk(helper, 10, speler.getInventory().countItem(Items.STICK), "they are still in the pockets");
        helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.STICK)) && menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)), "mayPlace");
        // an Oogster: out slots
        OogsterBlockEntity oogster = machine(helper, p(5, 6), TechmachineFeature.OOGSTER.get());
        helper.setBlock(p(5, 6).south(), Blocks.AIR);
        oogster.vakken().set(4, ItemResource.of(Items.WHEAT), 12);
        MachineMenu uit = (MachineMenu) oogster.createMenu(2, speler.getInventory(), speler);
        helper.assertTrue(!uit.getSlot(0).mayPlace(new ItemStack(Items.WHEAT)), "nothing goes into an out slot by hand");
        uit.quickMoveStack(speler, 4);
        gelijk(helper, 12, speler.getInventory().countItem(Items.WHEAT), "shift-click takes the harvest");
        gelijk(helper, 0, tel(oogster, Items.WHEAT), "out of the machine");
        helper.assertTrue(uit.stillValid(speler) == oogster.dichtbij(speler), "stillValid follows the distance");
        weg(helper, speler);
        helper.succeed();
    }

    // =====================================================================================================================
    // Plantagebak
    // =====================================================================================================================

    private static final BlockPos BAK = new BlockPos(8, 2, 7), PLANT = new BlockPos(8, 3, 8);

    private static PlantagebakBlockEntity bak(GameTestHelper helper) {
        helper.setBlock(BAK, TechmachineFeature.PLANTAGEBAK.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        bron(helper, BAK.north());
        return (PlantagebakBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BAK));
    }

    private static int telBlokken(GameTestHelper helper, java.util.function.Predicate<BlockState> wat) {
        int n = 0;
        for (BlockPos q : BlockPos.betweenClosed(new BlockPos(0, 3, 0), new BlockPos(16, 29, 16))) {
            if (wat.test(helper.getBlockState(q))) {
                n++;
            }
        }
        return n;
    }

    /** The bed of 3 x 3, an oak from sapling to tree to harvest, and what the chore slice asks. */
    @GuhTest(template = BOS, batch = "techmachine_bak", timeoutTicks = 400, skyAccess = true)
    public static void techmachinePlantagebakEik(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PlantagebakBlockEntity bak = bak(helper);
        // the bed: eight other blocks, each its own piece, all of them soil
        int delen = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = 0; dz <= 2; dz++) {
                BlockState s = helper.getBlockState(BAK.offset(dx, 0, dz));
                if (dx == 0 && dz == 0) {
                    continue;
                }
                helper.assertTrue(s.is(TechmachineFeature.PLANTAGEBAK_DEEL.get()), "a piece of the bed at " + dx + "," + dz + ": " + s);
                gelijk(helper, PlantagebakDeelBlock.Stuk.van(dx, dz - 1), s.getValue(PlantagebakDeelBlock.STUK), "which piece");
                delen++;
            }
        }
        gelijk(helper, 8, delen, "eight other blocks");
        gelijk(helper, helper.absolutePos(PLANT), bak.plantPlek(), "the plant spot is the middle of the bed, one up");
        helper.assertTrue(Plantagebakken.bij(level, helper.absolutePos(BAK.offset(1, 0, 2))) == bak, "a corner knows its bak");
        helper.assertTrue(Plantagebakken.rond(level, helper.absolutePos(BAK), 6).contains(bak), "rond finds it");
        helper.assertTrue(Blocks.POPPY.defaultBlockState().canSurvive(level, helper.absolutePos(BAK.above())), "anything may be planted on the bed");
        helper.assertTrue(PlantagebakBlockEntity.GROEITIJD <= 60 * 20, "the tree stands within a minute");
        // saplings in: by a pipe, by the chore call
        ResourceHandler<ItemResource> buis = buis(helper, BAK.offset(-1, 0, 1));
        gelijk(helper, 5, Kisten.stop(buis, new ItemStack(Items.STICK, 5)).getCount(), "a stick is no sapling");
        gelijk(helper, 0, Kisten.stop(buis, new ItemStack(Items.OAK_SAPLING, 2)).getCount(), "saplings go in through any block of the bed");
        ItemStack hand = new ItemStack(Items.OAK_SAPLING, 3);
        gelijk(helper, 3, bak.plant(hand), "plant() takes them");
        helper.assertTrue(hand.isEmpty(), "and the stack is used up");
        gelijk(helper, 0, bak.plant(new ItemStack(Items.BIRCH_SAPLING)), "one kind at a time");
        gelijk(helper, PlantagebakBlockEntity.Stand.LEEG, bak.stand(), "nothing planted yet");
        helper.runAfterDelay(25, () -> {
            helper.assertBlockPresent(Blocks.OAK_SAPLING, PLANT);
            gelijk(helper, PlantagebakBlockEntity.Stand.GROEIT, bak.stand(), "it planted one itself");
            gelijk(helper, 4, bak.voorraad().getCount(), "four saplings left");
            helper.assertTrue(bak.bezig() && bak.voortgang() > 0, "it grows");
            helper.assertBlockProperty(BAK, MachineBlock.SNOET, Snoet.WERKT);
            helper.assertTrue(lees(helper, BAK).stream().anyMatch(r -> r.startsWith("Hier groeit Oak Sapling: nog ")), "the hover: " + lees(helper, BAK));
            helper.assertTrue(bak.hak(null).isEmpty() && !bak.heeftBoom(), "no tree yet: nothing to chop");
            bak.voortgang = PlantagebakBlockEntity.GROEITIJD - 3;       // (the test does not wait 45 seconds)
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(helper.getBlockState(PLANT).is(BlockTags.LOGS), "the tree stands: " + helper.getBlockState(PLANT));
                helper.assertTrue(bak.heeftBoom() && bak.stand() == PlantagebakBlockEntity.Stand.BOOM, "the bak knows");
                helper.assertBlockProperty(BAK, MachineBlock.SNOET, Snoet.VOL);
                helper.assertTrue(helper.getBlockState(BAK.south()).is(TechmachineFeature.PLANTAGEBAK_DEEL.get()), "the bed under the trunk is still the bed");
                helper.assertTrue(lees(helper, BAK).stream().anyMatch(r -> r.startsWith("De boom staat!")), "the hover: " + lees(helper, BAK));
                List<BlockPos> stam = bak.stam();
                helper.assertTrue(stam.size() >= 4 && stam.get(0).equals(helper.absolutePos(PLANT)), "the trunk, from the bottom up: " + stam.size());
                int bladeren = telBlokken(helper, s -> s.is(BlockTags.LEAVES));
                helper.assertTrue(bladeren > 20, "an oak has leaves: " + bladeren);
                // a log somebody else put next to the tree is not the bak's
                helper.setBlock(PLANT.offset(3, 0, 0), Blocks.OAK_LOG);
                List<ItemStack> buit = bak.hak(null);
                int hout = buit.stream().filter(s -> s.is(Items.OAK_LOG)).mapToInt(ItemStack::getCount).sum();
                gelijk(helper, stam.size(), hout, "every log of the trunk is in the harvest");
                gelijk(helper, 0, telBlokken(helper, s -> s.is(BlockTags.LEAVES)), "the leaves are gone too");
                gelijk(helper, 1, telBlokken(helper, s -> s.is(BlockTags.LOGS)), "only the log that was not the tree's is left");
                int zaailingen = buit.stream().filter(s -> s.is(Items.OAK_SAPLING)).mapToInt(ItemStack::getCount).sum();
                helper.assertTrue(bak.voorraad().getCount() >= 4 && bak.voorraad().getCount() <= PlantagebakBlockEntity.RESERVE,
                        "the bak kept its saplings: " + bak.voorraad().getCount());
                helper.assertTrue(zaailingen == 0 || bak.voorraad().getCount() == PlantagebakBlockEntity.RESERVE,
                        "saplings that fell out went back into the bak first (" + zaailingen + " in the harvest, " + bak.voorraad().getCount() + " in the bak)");
                gelijk(helper, PlantagebakBlockEntity.Stand.LEEG, bak.stand(), "ready for the next tree");
                helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.OAK_SAPLING, PLANT));   // and it plants again by itself
            });
        });
    }

    /**
     * What a tree does to the ground is not the tree: four spruces planted by hand in a square on the bed grow into a big
     * one that turns the grass around the bak into podzol; chopping takes the whole tree and leaves the ground alone.
     */
    @GuhTest(template = BOS, batch = "techmachine_bak", timeoutTicks = 400, skyAccess = true)
    public static void techmachinePlantagebakLaatDeGrondLiggen(GameTestHelper helper) {
        PlantagebakBlockEntity bak = bak(helper);
        List<BlockPos> gras = new ArrayList<>();
        for (BlockPos q : BlockPos.betweenClosed(new BlockPos(3, 2, 3), new BlockPos(13, 2, 13))) {
            if (helper.getBlockState(q).isAir()) {
                helper.setBlock(q, Blocks.GRASS_BLOCK);
                gras.add(q.immutable());
            }
        }
        // (clockwise of north is east, behind is south: the square the bak itself would use)
        for (BlockPos q : List.of(PLANT, PLANT.east(), PLANT.south(), PLANT.east().south())) {
            helper.setBlock(q, Blocks.SPRUCE_SAPLING);
        }
        helper.runAfterDelay(10, () -> {
            gelijk(helper, PlantagebakBlockEntity.Stand.GROEIT, bak.stand(), "a sapling planted by hand counts");
            bak.voortgang = PlantagebakBlockEntity.GROEITIJD - 3;
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(bak.heeftBoom(), "the big spruce stands (wilNiet " + bak.wilNiet() + ")");
                helper.assertTrue(helper.getBlockState(PLANT.east()).is(BlockTags.LOGS) && helper.getBlockState(PLANT.east().south()).is(BlockTags.LOGS),
                        "a trunk of two by two");
                long podzol = gras.stream().filter(q -> helper.getBlockState(q).is(Blocks.PODZOL)).count();
                helper.assertTrue(podzol > 0, "the big spruce made podzol around the bak");
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = 0; dz <= 2; dz++) {
                        Block bed = helper.getBlockState(BAK.offset(dx, 0, dz)).getBlock();
                        helper.assertTrue(bed == TechmachineFeature.PLANTAGEBAK.get() || bed == TechmachineFeature.PLANTAGEBAK_DEEL.get(),
                                "the bed is still the bed at " + dx + "," + dz + ": " + bed);
                    }
                }
                List<ItemStack> buit = bak.hak(null);
                helper.assertTrue(buit.stream().anyMatch(s -> s.is(Items.SPRUCE_LOG)), "the harvest has the logs");
                helper.assertTrue(buit.stream().noneMatch(s -> s.is(Items.PODZOL) || s.is(Items.DIRT) || s.is(Items.GRASS_BLOCK)), "and no ground: " + buit);
                gelijk(helper, 0, telBlokken(helper, s -> s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES)), "the whole tree is gone");
                for (BlockPos q : gras) {
                    BlockState s = helper.getBlockState(q);
                    helper.assertTrue(s.is(Blocks.PODZOL) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT), "the ground at " + q + " is left alone: " + s);
                }
                helper.succeed();
            });
        });
    }

    /** Without vadskracht nothing is planted and nothing grows. */
    @GuhTest(template = BOS, batch = "techmachine_bak", timeoutTicks = 200, skyAccess = true)
    public static void techmachinePlantagebakZonderKracht(GameTestHelper helper) {
        PlantagebakBlockEntity bak = bak(helper);
        helper.setBlock(BAK.north(), Blocks.AIR);
        bak.plant(new ItemStack(Items.OAK_SAPLING, 2));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(helper.getBlockState(PLANT).isAir(), "no vadskracht: nothing planted");
            helper.assertBlockProperty(BAK, MachineBlock.SNOET, Snoet.SLAAPT);
            gelijk(helper, 2, bak.voorraad().getCount(), "the saplings wait in the bak");
            helper.succeed();
        });
    }

    /**
     * EVERY sapling of the game (vanilla, the mod's trees, the sate- and worstzwammetje, mushrooms, azalea) grows into a
     * tree on the bak and is chopped clean again. One after the other on the same bak, four in the slot (a kind that
     * only grows with four gets them).
     */
    @GuhTest(template = BOS, batch = "techmachine_soorten", timeoutTicks = 4000, skyAccess = true)
    public static void techmachinePlantagebakAlleSoorten(GameTestHelper helper) {
        PlantagebakBlockEntity bak = bak(helper);
        List<Block> soorten = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            if (Plantagebakken.isZaailing(b) && Plantagebakken.isZaailing(new ItemStack(b))) {
                soorten.add(b);
            }
        }
        helper.assertTrue(soorten.contains(Blocks.DARK_OAK_SAPLING) && soorten.contains(Blocks.CRIMSON_FUNGUS) && soorten.contains(Blocks.RED_MUSHROOM)
                && soorten.contains(nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.SATE_ZWAMMETJE.get())
                && soorten.contains(nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.WORST_ZWAMMETJE.get())
                && soorten.contains(ModBlocks.GUHBLOESEM_SAPLING.get()) && soorten.contains(nl.juiced.guhs.feature.vadswoud.VadswoudFeature.VADSHOUT_ZAAILING.get()),
                "the list has the vanilla ones, the zwammetjes and the mod's trees: " + soorten.size());
        int[] nr = {0};
        int[] fase = {0};        // 0 = put the saplings in, 1 = wait for the sapling, 2 = wait for the tree
        int[] wacht = {0};
        List<String> mislukt = new ArrayList<>();
        helper.onEachTick(() -> {
            if (nr[0] >= soorten.size()) {
                return;
            }
            Block soort = soorten.get(nr[0]);
            wacht[0]++;
            if (fase[0] == 0) {
                // a clean bak: nothing above the bed
                for (BlockPos q : BlockPos.betweenClosed(new BlockPos(0, 3, 0), new BlockPos(16, 29, 16))) {
                    if (!helper.getBlockState(q).isAir()) {
                        helper.setBlock(q, Blocks.AIR);
                    }
                }
                bak.vakken().set(0, ItemResource.of(soort), 4);
                fase[0] = 1;
                wacht[0] = 0;
            } else if (fase[0] == 1) {
                if (helper.getBlockState(PLANT).is(soort)) {
                    bak.voortgang = PlantagebakBlockEntity.GROEITIJD - 2;
                    fase[0] = 2;
                    wacht[0] = 0;
                } else if (wacht[0] > 40) {
                    mislukt.add(soort + " was not planted");
                    volgende(bak, nr, fase);
                }
            } else if (bak.heeftBoom()) {
                BlockState stam = helper.getBlockState(PLANT);
                int blokken = telBlokken(helper, s -> !s.isAir());
                int had = bak.voorraad().getCount();
                List<ItemStack> buit = bak.hak(null);
                int over = telBlokken(helper, s -> !s.isAir() && !s.hasBlockEntity());
                // (a huge mushroom gives only mushrooms, and those may all go back into the bak)
                boolean niks = buit.isEmpty() && bak.voorraad().getCount() <= had;
                if (stam.is(soort) || blokken < 4 || niks || over > 0 || bak.voorraad().getCount() > Math.max(had, PlantagebakBlockEntity.RESERVE)) {
                    mislukt.add(soort + ": trunk " + stam + ", " + blokken + " blocks, harvest " + buit.size() + ", left over " + over
                            + ", in the bak " + had + " -> " + bak.voorraad().getCount());
                }
                volgende(bak, nr, fase);
            } else if (wacht[0] > 60) {
                mislukt.add(soort + " did not grow (wilNiet " + bak.wilNiet() + ", at the spot: " + helper.getBlockState(PLANT) + ")");
                volgende(bak, nr, fase);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(nr[0] >= soorten.size(), "busy with kind " + nr[0] + " of " + soorten.size());
            helper.assertTrue(mislukt.isEmpty(), mislukt.size() + " of " + soorten.size() + " kinds failed: " + mislukt);
        });
    }

    private static void volgende(PlantagebakBlockEntity bak, int[] nr, int[] fase) {
        bak.vakken().set(0, ItemResource.EMPTY, 0);
        nr[0]++;
        fase[0] = 0;
    }

    /**
     * Who chops the BOTTOM log of the bak's tree by hand leaves a floating trunk. The bak does not plant a new sapling under
     * it (that one would never grow) and still knows its tree: an axe on the bak or a chore guh takes the rest down, and a
     * bee nest that came with the tree comes along as an item with its bees and its honey (it hung in the air before).
     */
    @GuhTest(template = BOS, batch = "techmachine_bak", timeoutTicks = 600, skyAccess = true)
    public static void techmachinePlantagebakOndersteStamEnBijennest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PlantagebakBlockEntity bak = bak(helper);
        // (beside the foot of the trunk: a tree only asks for free room around its trunk from one block up)
        BlockPos plantAbs = helper.absolutePos(PLANT), nestPlek = PLANT.north(), nestAbs = helper.absolutePos(nestPlek);
        gelijk(helper, 3, bak.plant(new ItemStack(Items.OAK_SAPLING, 3)), "three saplings in the bak");
        // the nest appears at the moment the tree grows (as a tree's own bee nest does), with honey and a bee at home
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockGrowFeatureEvent> nestje = e -> {
            if (e.getPos().equals(plantAbs) && level.getBlockState(nestAbs).isAir()) {
                level.setBlock(nestAbs, Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 3), Block.UPDATE_ALL);
                Bee bij = EntityType.BEE.create(level, EntitySpawnReason.TRIGGERED);
                bij.snapTo(nestAbs.getX() + 0.5, nestAbs.getY() + 1.5, nestAbs.getZ() + 0.5);
                ((BeehiveBlockEntity) level.getBlockEntity(nestAbs)).addOccupant(bij);
            }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.NORMAL, false,
                net.neoforged.neoforge.event.level.BlockGrowFeatureEvent.class, nestje);
        helper.runAfterDelay(25, () -> {
            helper.assertBlockPresent(Blocks.OAK_SAPLING, PLANT);
            bak.voortgang = PlantagebakBlockEntity.GROEITIJD - 3;
            helper.runAfterDelay(10, () -> {
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(nestje);
                helper.assertTrue(bak.heeftBoom() && helper.getBlockState(PLANT).is(BlockTags.LOGS), "the tree stands (wilNiet " + bak.wilNiet() + ")");
                helper.assertTrue(helper.getBlockState(nestPlek).is(Blocks.BEE_NEST), "with a bee nest against its trunk: " + helper.getBlockState(nestPlek));
                int stammen = bak.stam().size(), zaailingen = bak.voorraad().getCount();
                helper.assertTrue(stammen >= 4, "a trunk of at least four logs: " + stammen);
                // the bottom log is chopped by hand
                helper.setBlock(PLANT, Blocks.AIR);
                helper.runAfterDelay(3 * 20 + 5, () -> {
                    helper.assertTrue(helper.getBlockState(PLANT).isAir(), "no sapling is planted under the floating trunk: " + helper.getBlockState(PLANT));
                    gelijk(helper, zaailingen, bak.voorraad().getCount(), "the saplings stay in the bak");
                    helper.assertTrue(bak.heeftBoom() && bak.stand() == PlantagebakBlockEntity.Stand.BOOM, "the bak still knows its tree");
                    gelijk(helper, stammen - 1, bak.stam().size(), "the rest of the trunk");
                    helper.assertBlockProperty(BAK, MachineBlock.SNOET, Snoet.VOL);
                    // an axe on the bak (or a chore guh): the rest comes down, nest and all
                    List<ItemStack> buit = bak.hak(null);
                    gelijk(helper, stammen - 1, buit.stream().filter(s -> s.is(Items.OAK_LOG)).mapToInt(ItemStack::getCount).sum(), "every log that still stood");
                    gelijk(helper, 0, telBlokken(helper, s -> s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.is(Blocks.BEE_NEST)), "nothing of the tree is left, no floating nest");
                    ItemStack nest = buit.stream().filter(s -> s.is(Items.BEE_NEST)).findFirst().orElse(ItemStack.EMPTY);
                    helper.assertTrue(!nest.isEmpty() && nest.getCount() == 1, "the bee nest is in the harvest: " + buit);
                    Bees bijen = nest.get(DataComponents.BEES);
                    helper.assertTrue(bijen != null && bijen.bees().size() == 1, "with its bee in it: " + bijen);
                    BlockItemStateProperties staat = nest.get(DataComponents.BLOCK_STATE);
                    helper.assertTrue(staat != null && Integer.valueOf(3).equals(staat.get(BeehiveBlock.HONEY_LEVEL)), "and its honey: " + staat);
                    gelijk(helper, 0, level.getEntitiesOfClass(Bee.class, new net.minecraft.world.phys.AABB(nestAbs).inflate(8)).size(), "no angry bee came out");
                    gelijk(helper, PlantagebakBlockEntity.Stand.LEEG, bak.stand(), "ready for the next tree");
                    helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.OAK_SAPLING, PLANT));   // now it plants again
                });
            });
        });
    }

    /**
     * What a Neerzetter puts down belongs to whoever placed the Neerzetter (a machine without an owner is refused in every
     * huisje's home base and served by anybody's chore guhs); and a machine's items are not in what the clients get.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techmachineNeerzetterGeeftZijnEigenaarDoor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID baas = UUID.randomUUID();
        NeerzetterBlockEntity zetter = machine(helper, p(2, 6), TechmachineFeature.NEERZETTER.get());
        zetter.zetEigenaar(baas);
        zetter.vakken().set(0, ItemResource.of(TechmachineFeature.OOGSTER.get().asItem()), 1);
        zetter.vakken().set(1, ItemResource.of(Items.OAK_PLANKS), 7);
        // nobody's Neerzetter: what it places stays nobody's
        NeerzetterBlockEntity los = machine(helper, p(6, 6), TechmachineFeature.NEERZETTER.get());
        los.vakken().set(0, ItemResource.of(TechmachineFeature.KNABBELAAR.get().asItem()), 1);
        // the clients do not get the slots
        var naarClient = zetter.getUpdateTag(level.registryAccess());
        helper.assertTrue(zetter.saveWithoutMetadata(level.registryAccess()).contains("Vakken") && !naarClient.contains("Vakken"),
                "the saved data holds the items, the update for the clients does not: " + naarClient);
        helper.assertTrue(naarClient.contains("Bezig"), "the rest is still sent");
        NeerzetterBlockEntity kopie = new NeerzetterBlockEntity(zetter.getBlockPos(), zetter.getBlockState());
        kopie.loadWithComponents(nl.juiced.guhs.storage.Nbt.input(level.registryAccess(), naarClient));
        gelijk(helper, 0, tel(kopie, Items.OAK_PLANKS), "(a client loads that without the items)");
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(p(2, 5)).is(TechmachineFeature.OOGSTER.get()), "the Neerzetter placed the Oogster: " + helper.getBlockState(p(2, 5)));
            helper.assertTrue(level.getBlockEntity(helper.absolutePos(p(2, 5))) instanceof OogsterBlockEntity oogster && baas.equals(oogster.eigenaar()),
                    "and it belongs to the Neerzetter's owner");
            helper.assertTrue(level.getBlockEntity(helper.absolutePos(p(6, 5))) instanceof KnabbelaarBlockEntity knabbelaar && knabbelaar.eigenaar() == null,
                    "what nobody's Neerzetter places is nobody's");
        });
    }
}
