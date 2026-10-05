package nl.juiced.guhs.feature.bleekwoud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/**
 * What hangs on and sits in a bleekhout tree (vanilla's two pale oak decorators are hard-wired to vanilla's blocks):
 * hanging moss under the leaves and the trunk with a patch of moss on the ground, and a guh heart in the trunk.
 */
public final class BleekwoudBoom {
    /** Bleek hangmos under leaves and logs, and (ground_probability) a patch of bleekmos with carpets around the foot. */
    public static class Hangmos extends TreeDecorator {
        public static final MapCodec<Hangmos> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.floatRange(0f, 1f).fieldOf("leaves_probability").forGetter(d -> d.leaves),
                Codec.floatRange(0f, 1f).fieldOf("trunk_probability").forGetter(d -> d.trunk),
                Codec.floatRange(0f, 1f).fieldOf("ground_probability").forGetter(d -> d.ground)).apply(i, Hangmos::new));
        private final float leaves, trunk, ground;

        public Hangmos(float leaves, float trunk, float ground) {
            this.leaves = leaves;
            this.trunk = trunk;
            this.ground = ground;
        }

        @Override
        protected TreeDecoratorType<?> type() {
            return BleekwoudFeature.HANGMOS_DECORATOR.get();
        }

        @Override
        public void place(Context context) {
            RandomSource random = context.random();
            WorldGenLevel level = context.level();
            if (context.logs().isEmpty()) {
                return;
            }
            BlockPos origin = Collections.min(context.logs(), Comparator.comparingInt(Vec3i::getY));
            if (random.nextFloat() < ground) {
                level.registryAccess().lookup(Registries.CONFIGURED_FEATURE).flatMap(r -> r.get(BleekwoudFeature.MOS_PLEK))
                        .ifPresent(f -> f.value().place(level, level.getLevel().getChunkSource().getGenerator(), random, origin.above()));
            }
            context.logs().forEach(pos -> {
                if (random.nextFloat() < trunk && context.isAir(pos.below())) {
                    hang(pos.below(), context);
                }
            });
            context.leaves().forEach(pos -> {
                if (random.nextFloat() < leaves && context.isAir(pos.below())) {
                    hang(pos.below(), context);
                }
            });
        }

        private static void hang(BlockPos pos, Context context) {
            BlockState moss = BleekwoudFeature.BLEEK_HANGMOS.get().defaultBlockState();
            while (context.isAir(pos.below()) && !(context.random().nextFloat() < 0.5f)) {
                context.setBlock(pos, moss.setValue(HangingMossBlock.TIP, false));
                pos = pos.below();
            }
            context.setBlock(pos, moss.setValue(HangingMossBlock.TIP, true));
        }
    }

    /**
     * A guh heart in the trunk (probability: how many trees get one; soured: how many of those hearts are soured). It takes
     * the place of a log that has a log above and below it, at least two up from the foot, and likes the spot best where
     * it is hidden by logs on every side.
     */
    public static class Hartje extends TreeDecorator {
        public static final MapCodec<Hartje> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.floatRange(0f, 1f).fieldOf("probability").forGetter(d -> d.probability),
                Codec.floatRange(0f, 1f).fieldOf("soured").forGetter(d -> d.soured)).apply(i, Hartje::new));
        private final float probability, soured;

        public Hartje(float probability, float soured) {
            this.probability = probability;
            this.soured = soured;
        }

        @Override
        protected TreeDecoratorType<?> type() {
            return BleekwoudFeature.HARTJE_DECORATOR.get();
        }

        @Override
        public void place(Context context) {
            RandomSource random = context.random();
            List<BlockPos> logs = context.logs();
            if (logs.isEmpty() || random.nextFloat() >= probability) {
                return;
            }
            int foot = logs.get(0).getY();
            List<BlockPos> spots = new ArrayList<>(logs);
            Util.shuffle(spots, random);
            BlockPos best = null;
            int bestSides = -1;
            for (BlockPos pos : spots) {
                if (pos.getY() < foot + 2 || !isLog(context, pos.above()) || !isLog(context, pos.below())) {
                    continue;
                }
                int sides = 0;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    if (isLog(context, pos.relative(dir))) {
                        sides++;
                    }
                }
                if (sides > bestSides) {
                    best = pos;
                    bestSides = sides;
                }
            }
            if (best != null) {
                GuhhartjeBlock block = (random.nextFloat() < soured ? BleekwoudFeature.VERZUURD_GUHHARTJE : BleekwoudFeature.KRAKEND_GUHHARTJE).get();
                context.setBlock(best, block.defaultBlockState().setValue(GuhhartjeBlock.STATE, CreakingHeartState.DORMANT)
                        .setValue(GuhhartjeBlock.NATURAL, true));
            }
        }

        private static boolean isLog(Context context, BlockPos pos) {
            return context.checkBlock(pos, state -> state.is(BleekwoudFeature.STAMMEN));
        }
    }

    private BleekwoudBoom() {
    }
}
