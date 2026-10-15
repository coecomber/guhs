package nl.juiced.guhs.feature.techmachine;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.tuintjes.TuinBlock;
import nl.juiced.guhs.feature.tuintjes.TuinPlant;
import nl.juiced.guhs.feature.vadswoud.KnabbelbessenstruikBlock;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;

/**
 * What the Oogster knows about plants: is this ripe, what does cutting it give, and what stands there afterwards. Nothing
 * is changed by {@link #bekijk}: it gives a {@link Pluk} (the harvest and the new block) that {@link #doe} carries out, so
 * a machine can first see whether the harvest fits. Whatever it cuts grows again without anybody planting:
 * <ul>
 *   <li>crops ({@code CropBlock}: wheat, carrots, potatoes, beetroot, kaasknabbelplantjes, crops of other mods), nether
 *       wart and cocoa: ripe -> young again, one seed of the harvest (when there is one) goes back into the ground;</li>
 *   <li>the guhtuintjes (bloempot, moestuinbak): the plant stays and starts again;</li>
 *   <li>sweet berries, knabbelbessen, glow berries: the berries only;</li>
 *   <li>pumpkins and melons that hang on a stem: the fruit (the stem stays and grows the next one);</li>
 *   <li>sugar cane, cactus and bamboo: everything above the bottom piece, one piece per cut, from the top down (the cut
 *       may be higher than the spot that was asked about: {@link Pluk#pos}).</li>
 * </ul>
 * Public: the chore slice may use it for its harvest chores too.
 */
public final class Oogst {
    /** How far up a stalk (sugar cane, cactus, bamboo) is followed to find its top. */
    private static final int STENGEL = 24;

    /** One cut: the spot and the block as it was, what stands there afterwards, and what the cut gives. */
    public record Pluk(BlockPos pos, BlockState oud, BlockState nieuw, List<ItemStack> oogst) {
    }

    /** Is the plant here ripe for the Oogster? (Cheap: no loot is rolled.) */
    public static boolean isRijp(ServerLevel level, BlockPos pos) {
        return soort(level, pos, level.getBlockState(pos)) != 0;
    }

    /** 0 = nothing to cut; otherwise the kind of plant (the cases of {@link #bekijk}). */
    private static int soort(ServerLevel level, BlockPos pos, BlockState s) {
        Block b = s.getBlock();
        if (b instanceof CropBlock crop) {
            return crop.isMaxAge(s) ? 1 : 0;
        }
        if (b instanceof NetherWartBlock) {
            return s.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE ? 2 : 0;
        }
        if (b instanceof CocoaBlock) {
            return s.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE ? 3 : 0;
        }
        if (TuinBlock.isTuin(s)) {
            return TuinBlock.rijp(s) ? 4 : 0;
        }
        if (b instanceof SweetBerryBushBlock) {
            return s.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE ? 5 : 0;
        }
        if (b instanceof KnabbelbessenstruikBlock) {
            return s.getValue(KnabbelbessenstruikBlock.AGE) >= KnabbelbessenstruikBlock.MAX_AGE ? 6 : 0;
        }
        if (s.hasProperty(CaveVines.BERRIES) && (s.is(Blocks.CAVE_VINES) || s.is(Blocks.CAVE_VINES_PLANT))) {
            return s.getValue(CaveVines.BERRIES) ? 7 : 0;
        }
        if (s.is(Blocks.PUMPKIN) || s.is(Blocks.MELON)) {
            return aanStengel(level, pos) ? 8 : 0;
        }
        if (s.is(Blocks.SUGAR_CANE) || s.is(Blocks.CACTUS) || s.is(Blocks.BAMBOO)) {
            // a piece that stands on a piece of the same plant: the stalk has grown (its top is what is cut, see bekijk)
            return level.getBlockState(pos.below()).is(b) ? 9 : 0;
        }
        return 0;
    }

    /**
     * Did this pumpkin or melon grow here: does a stem next to it hold it? (A pumpkin somebody put down as a lantern-to-be
     * or as a wall is not a crop.)
     */
    private static boolean aanStengel(ServerLevel level, BlockPos pos) {
        for (Direction kant : Direction.Plane.HORIZONTAL) {
            BlockState buur = level.getBlockState(pos.relative(kant));
            if (buur.getBlock() instanceof AttachedStemBlock && buur.getValue(AttachedStemBlock.FACING) == kant.getOpposite()) {
                return true;
            }
        }
        return false;
    }

    /** What cutting the plant here would give (the loot is rolled now); null when nothing here is ripe. */
    @Nullable
    public static Pluk bekijk(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        RandomSource rnd = level.getRandom();
        switch (soort(level, pos, s)) {
            case 1 -> {
                List<ItemStack> oogst = drops(level, pos, s);
                neemEen(oogst, s.getCloneItemStack(level, pos, false).getItem());
                return new Pluk(pos, s, ((CropBlock) s.getBlock()).getStateForAge(0), oogst);
            }
            case 2 -> {
                List<ItemStack> oogst = drops(level, pos, s);
                neemEen(oogst, Items.NETHER_WART);
                return new Pluk(pos, s, s.setValue(NetherWartBlock.AGE, 0), oogst);
            }
            case 3 -> {
                List<ItemStack> oogst = drops(level, pos, s);
                neemEen(oogst, Items.COCOA_BEANS);
                return new Pluk(pos, s, s.setValue(CocoaBlock.AGE, 0), oogst);
            }
            case 4 -> {
                // (the same harvest as picking it by hand: TuinBlock.oogst)
                TuinPlant plant = s.getValue(TuinBlock.PLANT);
                List<ItemStack> oogst = new ArrayList<>();
                oogst.add(new ItemStack(plant.oogst(), 2 + rnd.nextInt(2)));
                if (rnd.nextFloat() < 0.3f) {
                    oogst.add(new ItemStack(plant.zaadje()));
                }
                return new Pluk(pos, s, s.setValue(TuinBlock.GROEI, 0).setValue(TuinBlock.GEWATERD, false), oogst);
            }
            case 5 -> {
                return new Pluk(pos, s, s.setValue(SweetBerryBushBlock.AGE, 1), lijst(new ItemStack(Items.SWEET_BERRIES, 2 + rnd.nextInt(2))));
            }
            case 6 -> {
                return new Pluk(pos, s, s.setValue(KnabbelbessenstruikBlock.AGE, 1),
                        lijst(new ItemStack(VadswoudFeature.KNABBELBESSEN.get(), 2 + rnd.nextInt(2))));
            }
            case 7 -> {
                return new Pluk(pos, s, s.setValue(CaveVines.BERRIES, false), lijst(new ItemStack(Items.GLOW_BERRIES)));
            }
            case 8 -> {
                return new Pluk(pos, s, Blocks.AIR.defaultBlockState(), drops(level, pos, s));
            }
            case 9 -> {
                // the top piece of the stalk (it may stand higher than the spot that was looked at)
                BlockPos top = pos;
                for (int i = 0; i < STENGEL && level.getBlockState(top.above()).is(s.getBlock()); i++) {
                    top = top.above();
                }
                BlockState bovenste = level.getBlockState(top);
                return new Pluk(top, bovenste, Blocks.AIR.defaultBlockState(), drops(level, top, bovenste));
            }
            default -> {
                return null;
            }
        }
    }

    /** Carries the cut out (when the plant is still as it was): the new block, a rustle and some bits of plant. */
    public static boolean doe(ServerLevel level, Pluk pluk) {
        if (level.getBlockState(pluk.pos()) != pluk.oud()) {
            return false;
        }
        level.setBlock(pluk.pos(), pluk.nieuw(), Block.UPDATE_ALL);
        level.playSound(null, pluk.pos(), SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.8f, 1.1f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, pluk.oud()), pluk.pos().getX() + 0.5, pluk.pos().getY() + 0.4,
                pluk.pos().getZ() + 0.5, 10, 0.25, 0.2, 0.25, 0.05);
        return true;
    }

    private static List<ItemStack> drops(ServerLevel level, BlockPos pos, BlockState s) {
        List<ItemStack> uit = new ArrayList<>();
        for (ItemStack d : Block.getDrops(s, level, pos, null)) {
            if (!d.isEmpty()) {
                uit.add(d.copy());
            }
        }
        return uit;
    }

    private static List<ItemStack> lijst(ItemStack stack) {
        List<ItemStack> uit = new ArrayList<>();
        uit.add(stack);
        return uit;
    }

    /** Takes one of this item out of the harvest (the seed that goes back into the ground); false when there is none. */
    private static boolean neemEen(List<ItemStack> oogst, Item zaad) {
        for (int i = 0; i < oogst.size(); i++) {
            ItemStack d = oogst.get(i);
            if (zaad != Items.AIR && d.is(zaad)) {
                d.shrink(1);
                if (d.isEmpty()) {
                    oogst.remove(i);
                }
                return true;
            }
        }
        return false;
    }

    private Oogst() {
    }
}
