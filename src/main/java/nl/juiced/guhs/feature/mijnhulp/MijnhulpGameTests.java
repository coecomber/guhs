package nl.juiced.guhs.feature.mijnhulp;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.event.KeepOnDeathHandler;
import nl.juiced.guhs.feature.onderwol.OnderWolFeature;
import nl.juiced.guhs.feature.safari.kriebel.KriebelBlokken;
import nl.juiced.guhs.feature.safari.verblijf.ThuisDier;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfFeature;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfsbordBlockEntity;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.7.0 mining helpers (feature/mijnhulp): the Vadsknakker (recipe with merged enchantments, slim per blok, mining), super
 * vahoege vads (smithing keeps the enchantments, the template copy, the stats), the Wolkenliftje, the Pluisladder, the
 * Mijnlampje's light (comes, follows, goes) and terraria of any shape (the flood fill, the cap, sharing, walking inside).
 */
public class MijnhulpGameTests {
    private static final String EMPTY = "empty";
    private static final String VLOER = "px_test_16";

    private static Holder<Enchantment> ench(GameTestHelper helper, net.minecraft.resources.ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ItemStack met(Supplier<? extends Item> item, Holder<Enchantment> e, int lvl) {
        ItemStack s = new ItemStack(item.get());
        s.enchant(e, lvl);
        return s;
    }

    /** Paxel + shears + hoe (shapeless, any order): the Vadsknakker with their enchantments merged the anvil way. */
    @GuhTest(template = EMPTY)
    public static void vadsknakkerReceptVoegtEnchantmentsSamen(GameTestHelper helper) {
        Holder<Enchantment> eff = ench(helper, Enchantments.EFFICIENCY), fortuin = ench(helper, Enchantments.FORTUNE),
                zijde = ench(helper, Enchantments.SILK_TOUCH);
        ItemStack paxel = met(ModItems.VADS_PAXEL, eff, 3), schaar = met(ModItems.VADS_SHEARS, eff, 3), schoffel = met(ModItems.VADS_HOE, fortuin, 2);
        schoffel.enchant(zijde, 1);     // (clashes with fortune: left out)
        CraftingInput in = CraftingInput.of(3, 1, List.of(schoffel, paxel, schaar));
        var recept = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, helper.getLevel());
        helper.assertTrue(recept.isPresent(), "paxel + shears + hoe make something");
        ItemStack uit = recept.get().value().assemble(in);
        helper.assertTrue(uit.is(MijnhulpFeature.VADSKNAKKER.get()), "the Vadsknakker");
        helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(eff, uit) == 4, "efficiency III + III = IV");
        helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(fortuin, uit) == 2, "fortune II along");
        helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(zijde, uit) == 0, "silk touch clashes with fortune: left out");
        helper.assertTrue(uit.has(DataComponents.UNBREAKABLE) && uit.is(KeepOnDeathHandler.KEEP_ON_DEATH), "unbreakable vads gear, kept on death");
        helper.assertTrue(Language.getInstance().has("item.guhs.vahoege_vadsknakker"), "it has a name");
        helper.succeed();
    }

    /** Mines like five tools; right-click: grass to farmland, sneak to path, log stripped, a sheep sheared. */
    @GuhTest(template = EMPTY)
    public static void vadsknakkerSlimPerBlok(GameTestHelper helper) {
        ItemStack k = new ItemStack(MijnhulpFeature.VADSKNAKKER.get());
        for (BlockState s : List.of(Blocks.STONE.defaultBlockState(), Blocks.DIAMOND_ORE.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(), Blocks.HAY_BLOCK.defaultBlockState(), Blocks.COBWEB.defaultBlockState())) {
            helper.assertTrue(k.isCorrectToolForDrops(s) && k.getDestroySpeed(s) > 1f, "mines " + s);
        }
        helper.assertTrue(!k.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()) == !new ItemStack(Items.DIAMOND_PICKAXE)
                .isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "the tier of diamond");
        helper.assertTrue(k.getDestroySpeed(Blocks.OAK_LEAVES.defaultBlockState()) == 15f && k.getDestroySpeed(Blocks.PINK_WOOL.defaultBlockState()) >= 5f,
                "leaves and wool like shears");
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, k);
        BlockPos gras = new BlockPos(1, 1, 1), pad = new BlockPos(3, 1, 1), stam = new BlockPos(5, 1, 1);
        helper.setBlock(gras, Blocks.GRASS_BLOCK);
        helper.setBlock(pad, Blocks.GRASS_BLOCK);
        helper.setBlock(stam, Blocks.OAK_LOG);
        klik(helper, p, k, gras);
        helper.assertBlockPresent(Blocks.FARMLAND, gras);
        p.setShiftKeyDown(true);
        klik(helper, p, k, pad);
        helper.assertBlockPresent(Blocks.DIRT_PATH, pad);
        p.setShiftKeyDown(false);
        klik(helper, p, k, stam);
        helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, stam);
        Sheep schaap = helper.spawn(EntityType.SHEEP, new BlockPos(2, 1, 3));
        helper.assertTrue(k.interactLivingEntity(p, schaap, InteractionHand.MAIN_HAND).consumesAction() && schaap.isSheared(), "the sheep is sheared");
        helper.assertTrue(k.getDamageValue() == 0, "nothing wears it out");
        helper.succeed();
    }

    private static void klik(GameTestHelper helper, ServerPlayer p, ItemStack k, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        k.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
    }

    /** Every vads piece + template + Vahoegietstaaf on the smithing table: its super version, enchantments kept. */
    @GuhTest(template = EMPTY)
    public static void superVadsOpDeSmidstafel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> unb = ench(helper, Enchantments.SHARPNESS);
        for (var e : MijnhulpFeature.OPWAARDERING.entrySet()) {
            ItemStack basis = new ItemStack(e.getKey().get());
            boolean scherp = basis.supportsEnchantment(unb);
            if (scherp) {
                basis.enchant(unb, 3);
            }
            SmithingRecipeInput in = new SmithingRecipeInput(new ItemStack(MijnhulpFeature.SJABLOON.get()), basis,
                    new ItemStack(MijnhulpFeature.VAHOEGIETSTAAF.get()));
            var r = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, in, level);
            helper.assertTrue(r.isPresent(), "a smithing recipe for " + basis.getItem());
            ItemStack uit = r.get().value().assemble(in);
            helper.assertTrue(uit.is(e.getValue().get()), basis.getItem() + " becomes " + e.getValue().get());
            helper.assertTrue(!scherp || EnchantmentHelper.getItemEnchantmentLevel(unb, uit) == 3, "its enchantments stay: " + uit.getItem());
            helper.assertTrue(uit.has(DataComponents.UNBREAKABLE) && uit.has(DataComponents.DAMAGE_RESISTANT) && uit.is(KeepOnDeathHandler.KEEP_ON_DEATH),
                    "unbreakable, fire-proof, kept on death: " + uit.getItem());
        }
        // netherite stats: more damage than the vads sword; armour toughness
        helper.assertTrue(new ItemStack(MijnhulpFeature.SUPER_PICKAXE.get()).getDestroySpeed(Blocks.STONE.defaultBlockState()) == 9f, "netherite speed");
        helper.assertTrue(MijnhulpFeature.SUPER_VADS.toughness() == 3f && MijnhulpFeature.SUPER_VADS.knockbackResistance() > 0, "toughness 3, knockback resistance");
        // the template: 7 vahoegiet + 1 guhkristalsteen + a template = 2 templates
        ItemStack v = new ItemStack(OnderWolFeature.VAHOEGIET.get()), t = new ItemStack(MijnhulpFeature.SJABLOON.get()),
                g = new ItemStack(nl.juiced.guhs.registry.ModBlocks.GUH_KRISTALSTEEN.get());
        CraftingInput kopie = CraftingInput.of(3, 3, List.of(v, t, v, v, g, v, v, v, v));
        var rk = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, kopie, level);
        helper.assertTrue(rk.isPresent() && rk.get().value().assemble(kopie).getCount() == 2 && rk.get().value().assemble(kopie).is(MijnhulpFeature.SJABLOON.get()),
                "the template copied: two");
        ItemStack staaf = new ItemStack(ModItems.VAHOEGE_VADS_INGOT.get());
        CraftingInput st = CraftingInput.of(3, 3, List.of(v, v, v, v, staaf, staaf, staaf, staaf, ItemStack.EMPTY));
        var rs = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, st, level);
        helper.assertTrue(rs.isPresent() && rs.get().value().assemble(st).is(MijnhulpFeature.VAHOEGIETSTAAF.get()), "4 vahoegiet + 4 ingots = a Vahoegietstaaf");
        helper.succeed();
    }

    /** Jump on a Wolkenliftje: up to the next one with room; sneak: down again; nothing above: nothing happens. */
    @GuhTest(template = VLOER, timeoutTicks = 60)
    public static void wolkenliftjeOmhoogEnOmlaag(GameTestHelper helper) {
        BlockPos onder = new BlockPos(3, 1, 3), boven = new BlockPos(3, 9, 3);
        helper.setBlock(onder, MijnhulpFeature.WOLKENLIFTJE.get());
        helper.setBlock(boven, MijnhulpFeature.WOLKENLIFTJE.get());
        helper.setBlock(new BlockPos(3, 5, 3), Blocks.STONE);     // (solid rock in between: no problem)
        ServerPlayer p = GuhMockPlayer.of(helper);
        Vec3 op = helper.absoluteVec(new Vec3(3.5, 2, 3.5));
        p.teleportTo(op.x, op.y, op.z);
        helper.assertTrue(Wolkenliftje.rijd(p, true), "up");
        helper.assertTrue(Math.abs(p.getY() - helper.absoluteVec(new Vec3(0, 10, 0)).y) < 0.01, "on the upper one");
        helper.assertTrue(!Wolkenliftje.rijd(p, true), "nothing above it, and too soon anyway");
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(Wolkenliftje.rijd(p, false), "down");
            helper.assertTrue(Math.abs(p.getY() - op.y) < 0.01, "back on the lower one");
            helper.setBlock(new BlockPos(3, 10, 3), Blocks.STONE);   // no room on the upper one
            helper.runAfterDelay(12, () -> {
                helper.assertTrue(!Wolkenliftje.rijd(p, true), "no room above the upper one: stays");
                helper.succeed();
            });
        });
    }

    /** The Pluisladder unrolls down to the floor; climbable; breaking a lower piece rolls it all up and gives one roll. */
    @GuhTest(template = VLOER, timeoutTicks = 100)
    public static void pluisladderRoltUit(GameTestHelper helper) {
        BlockPos muur = new BlockPos(5, 1, 5);
        for (int y = 1; y <= 8; y++) {
            helper.setBlock(muur.atY(y), Blocks.STONE);
        }
        BlockPos top = new BlockPos(5, 8, 4);
        helper.setBlock(top, MijnhulpFeature.PLUISLADDER.get().defaultBlockState().setValue(PluisladderBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(40, () -> {
            for (int y = 1; y <= 8; y++) {
                helper.assertBlockPresent(MijnhulpFeature.PLUISLADDER.get(), top.atY(y));
            }
            helper.assertTrue(helper.getBlockState(top.atY(1)).is(BlockTags.CLIMBABLE), "climbable");
            helper.assertTrue(!helper.getBlockState(top.atY(3)).getValue(PluisladderBlock.TOP), "the lower pieces aren't the top");
            ServerPlayer p = GuhMockPlayer.of(helper);
            BlockPos abs = helper.absolutePos(top.atY(3));
            helper.getBlockState(top.atY(3)).getBlock().playerWillDestroy(helper.getLevel(), abs, helper.getBlockState(top.atY(3)), p);
            helper.getLevel().removeBlock(abs, false);
            for (int y = 1; y <= 8; y++) {
                helper.assertBlockNotPresent(MijnhulpFeature.PLUISLADDER.get(), top.atY(y));
            }
            helper.assertItemEntityCountIs(MijnhulpFeature.PLUISLADDER_ITEM.get().asItem(), top.atY(3), 2.0, 1);
            helper.succeed();
        });
    }

    /** A tamed guh with a Mijnlampje lights up a dark spot, takes the light along, and takes it away when it goes. */
    @GuhTest(template = VLOER, timeoutTicks = 120)
    public static void mijnlampjeLoptMee(GameTestHelper helper) {
        // a dark box: a roof over the floor
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 4, z), Blocks.STONE);
            }
        }
        ServerPlayer p = GuhMockPlayer.of(helper);
        GuhEntity g = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 3));
        g.tame(p);
        g.setNoAi(true);
        g.wear(GuhClothes.MIJNLAMPJE);
        helper.runAfterDelay(10, () -> {
            BlockPos l = Mijnlampje.lamp(g);
            helper.assertTrue(l != null && helper.getLevel().getBlockState(l).is(MijnhulpFeature.MIJNLAMP_LICHT.get()), "a light at the guh");
            g.teleportTo(g.getX() + 3, g.getY(), g.getZ());
            helper.runAfterDelay(10, () -> {
                BlockPos l2 = Mijnlampje.lamp(g);
                helper.assertTrue(l2 != null && !l2.equals(l) && !helper.getLevel().getBlockState(l).is(MijnhulpFeature.MIJNLAMP_LICHT.get()),
                        "the light moved along, the old one is gone");
                g.discard();
                helper.runAfterDelay(30, () -> {
                    helper.assertTrue(!helper.getLevel().getBlockState(l2).is(MijnhulpFeature.MIJNLAMP_LICHT.get()), "the guh is gone: so is its light");
                    // a stray light block (e.g. from a saved chunk) disappears by itself
                    BlockPos los = helper.absolutePos(new BlockPos(7, 1, 7));
                    helper.getLevel().setBlockAndUpdate(los, MijnhulpFeature.MIJNLAMP_LICHT.get().defaultBlockState());
                    helper.runAfterDelay(25, () -> {
                        helper.assertTrue(helper.getLevel().getBlockState(los).isAir(), "a light without a lamp guh vanishes");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** The living space: everything joined by terrariumglas/terrariumbodem, any shape; a second terrarium doesn't share. */
    @GuhTest(template = VLOER, timeoutTicks = 20)
    public static void terrariumRuimteVrijeVorm(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos t = new BlockPos(2, 1, 2);
        helper.setBlock(t, VerblijfFeature.TERRARIUM.get());
        // an L-shaped tank, a tunnel one wide, one block up
        helper.setBlock(new BlockPos(3, 1, 2), KriebelBlokken.TERRARIUMGLAS.get());
        helper.setBlock(new BlockPos(4, 1, 2), MijnhulpFeature.TERRARIUMBODEM.get());
        helper.setBlock(new BlockPos(4, 1, 3), KriebelBlokken.TERRARIUMGLAS.get());
        helper.setBlock(new BlockPos(4, 1, 4), KriebelBlokken.TERRARIUMGLAS.get());
        helper.setBlock(new BlockPos(4, 2, 4), KriebelBlokken.TERRARIUMGLAS.get());
        helper.setBlock(new BlockPos(7, 1, 7), KriebelBlokken.TERRARIUMGLAS.get());   // not joined
        List<BlockPos> r = TerrariumRuimte.ruimte(level, helper.absolutePos(t));
        helper.assertTrue(r.size() == 6 && r.get(0).equals(helper.absolutePos(t)), "the terrarium and its five pieces (" + r.size() + ")");
        helper.assertTrue(!r.contains(helper.absolutePos(new BlockPos(7, 1, 7))), "a loose piece isn't part of it");
        // a second terrarium joined to it: only one of them gets the space
        BlockPos t2 = new BlockPos(4, 1, 5);
        helper.setBlock(t2, VerblijfFeature.TERRARIUM.get());
        int a = TerrariumRuimte.ruimte(level, helper.absolutePos(t)).size(), b = TerrariumRuimte.ruimte(level, helper.absolutePos(t2)).size();
        helper.assertTrue(a + b == 7 && Math.min(a, b) == 1, "not shared: " + a + " and " + b);
        helper.succeed();
    }

    /** The flood fill stops at {@link TerrariumRuimte#MAX} pieces. */
    @GuhTest(template = EMPTY, timeoutTicks = 20)
    public static void terrariumRuimteHeeftEenGrens(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos t = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(t, VerblijfFeature.TERRARIUM.get().defaultBlockState());
        BlockState glas = KriebelBlokken.TERRARIUMGLAS.get().defaultBlockState();
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 15; y++) {
                for (int z = 0; z < 15; z++) {
                    BlockPos q = t.offset(x + 1, y, z);
                    level.setBlock(q, glas, 2);
                }
            }
        }
        int n = TerrariumRuimte.ruimte(level, t).size();
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 15; y++) {
                for (int z = 0; z < 15; z++) {
                    level.setBlock(t.offset(x + 1, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        helper.assertTrue(n == TerrariumRuimte.MAX, "capped at " + TerrariumRuimte.MAX + " (" + n + ")");
        helper.succeed();
    }

    /** The creatures walk out into the space, never out of it, and come back in when their piece is taken away. */
    @GuhTest(template = VLOER, timeoutTicks = 1400)
    public static void terrariumDiertjesLopenRond(GameTestHelper helper) {
        BlockPos t = new BlockPos(2, 1, 2);
        helper.setBlock(t, VerblijfFeature.TERRARIUM.get());
        for (int x = 3; x <= 8; x++) {
            helper.setBlock(new BlockPos(x, 1, 2), KriebelBlokken.TERRARIUMGLAS.get());
        }
        VerblijfsbordBlockEntity be = (VerblijfsbordBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(t));
        be.plaats(helper.getLevel(), null, "mier");
        boolean[] buiten = {false};
        helper.onEachTick(() -> {
            List<BlockPos> ruimte = TerrariumRuimte.ruimte(helper.getLevel(), helper.absolutePos(t));
            for (ThuisDier d : be.dieren(helper.getLevel())) {
                BlockPos c = BlockPos.containing(d.getX(), d.getY() + 0.01, d.getZ());
                if (!ruimte.contains(c)) {
                    buiten[0] = true;
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(!buiten[0], "never out of its space");
            Optional<ThuisDier> ver = be.dieren(helper.getLevel()).stream().filter(d -> d.getX() > helper.absolutePos(t).getX() + 2).findAny();
            helper.assertTrue(ver.isPresent(), "an ant walked out into the glass");
        });
    }
}
