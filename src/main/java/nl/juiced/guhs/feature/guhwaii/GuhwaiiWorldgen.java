package nl.juiced.guhs.feature.guhwaii;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;
import nl.juiced.guhs.feature.onderwater.KaaskoraalBlock;
import nl.juiced.guhs.feature.onderwater.OnderwaterFeature;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * The worldgen features of Guhwai'i (placed by the biome, see tools/features/guhwaii.py):
 * <ul>
 *   <li>{@link Palm}: a guh-palm: a slender, gently curving trunk with a guh face in it (looking out), a crown of long
 *       drooping fronds and a few coconuts (green to brown) hanging under them;</li>
 *   <li>{@link Rif}: a little reef mound in the shallow lagoon: kaaskoraal blocks and warm corals, kaaskoraal trees on top
 *       (you can breathe near them: snorkelling!), coral fans, sea pickles and seagrass around it, the guhvisjes swim there;</li>
 *   <li>{@link Nestje}: Schilly-eitjes in the warm beach sand close to the water.</li>
 * </ul>
 * All of them stay out of buildings ({@link BouwRuimte#inBuilding}).
 */
public final class GuhwaiiWorldgen {
    private GuhwaiiWorldgen() {
    }

    static boolean vrij(BlockState s) {
        return s.isAir() || s.canBeReplaced() && s.getFluidState().isEmpty() || s.is(BlockTags.FLOWERS) || s.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    // =================================================================================================================
    // the guh-palm
    // =================================================================================================================

    public static class Palm extends Feature<NoneFeatureConfiguration> {
        public Palm(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos o = context.origin();
            BlockPos grond = new BlockPos(o.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ()) - 1, o.getZ());
            return boom(level, context.random(), grond);
        }
    }

    /**
     * A guh-palm on {@code grond} (sand or grass): 5-8 trunk blocks leaning a little (one or two blocks over its height),
     * the guh face 2-3 blocks up looking out, a crown of 5-8 fronds (3-4 long, drooping at the end) and 2-4 coconuts. Returns
     * false (nothing built) when there is no room.
     */
    public static boolean boom(WorldGenLevel level, RandomSource random, BlockPos grond) {
        if (!GuhwaiiBlokken.eilandgrond(level.getBlockState(grond)) || BouwRuimte.inBuilding(level, grond.above())
                || !level.getFluidState(grond.above()).isEmpty()) {
            return false;
        }
        int hoog = 5 + random.nextInt(4);
        Direction scheef = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int schuif = random.nextInt(3);                           // 0-2 blocks of lean over its height
        List<BlockPos> stam = new ArrayList<>();
        int vorig = 0;
        for (int i = 1; i <= hoog; i++) {
            int opzij = (int) Math.floor((double) i * i / (hoog * hoog) * schuif + 0.001);
            if (opzij != vorig) {   // an elbow: the trunk stays face-connected
                stam.add(grond.above(i).relative(scheef, vorig));
            }
            stam.add(grond.above(i).relative(scheef, opzij));
            vorig = opzij;
        }
        BlockPos top = stam.get(stam.size() - 1);
        // the crown: fronds from just above the top
        BlockPos kruin = top.above();
        Set<BlockPos> blad = new HashSet<>();
        blad.add(kruin);
        int[][] richtingen = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        List<int[]> gekozen = new ArrayList<>(List.of(richtingen));
        java.util.Collections.shuffle(gekozen, new java.util.Random(random.nextLong()));
        int fronds = 5 + random.nextInt(4);
        for (int k = 0; k < fronds; k++) {
            int[] r = gekozen.get(k);
            boolean schuin = r[0] != 0 && r[1] != 0;
            int lang = schuin ? 2 : 3 + random.nextInt(2);
            // out along the top, the last one a step lower, and the tip hanging down (every leaf touches the next by a face)
            BlockPos vorige = kruin;
            for (int s = 1; s <= lang; s++) {
                BlockPos p = kruin.offset(r[0] * s, s < lang ? 0 : -1, r[1] * s);
                verbind(blad, vorige, p);
                vorige = p;
            }
            verbind(blad, vorige, vorige.below());
        }
        blad.add(kruin.above());
        // room?
        for (BlockPos p : stam) {
            if (!vrij(level.getBlockState(p)) || BouwRuimte.inBuilding(level, p)) {
                return false;
            }
        }
        for (BlockPos p : blad) {
            if (!vrij(level.getBlockState(p)) && !level.getBlockState(p).is(GuhwaiiFeature.PALM_BLAD.get())) {
                return false;
            }
        }
        // the trunk, with the guh face 2-3 up (facing out, away from the lean if it leans)
        int gezicht = 1 + random.nextInt(2);
        Direction kijk = schuif > 0 ? scheef : Direction.Plane.HORIZONTAL.getRandomDirection(random);
        for (int i = 0; i < stam.size(); i++) {
            BlockState s = i == gezicht ? GuhwaiiFeature.PALM_GEZICHT.get().defaultBlockState().setValue(GuhwaiiBlokken.PalmGezicht.FACING, kijk)
                    : GuhwaiiFeature.PALM_STAM.get().defaultBlockState();
            level.setBlock(stam.get(i), s, Block.UPDATE_CLIENTS);
        }
        // leaves with their distance to the wood (they decay once the trunk is chopped)
        Map<BlockPos, Integer> afstand = new HashMap<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        afstand.put(top, 0);
        todo.add(top);
        while (!todo.isEmpty()) {
            BlockPos p = todo.poll();
            for (Direction d : Direction.values()) {
                BlockPos q = p.relative(d);
                if (blad.contains(q) && !afstand.containsKey(q)) {
                    afstand.put(q, afstand.get(p) + 1);
                    todo.add(q);
                }
            }
        }
        // (a frond tip too far from the wood would decay: it isn't grown at all)
        blad.removeIf(p -> afstand.getOrDefault(p, LeavesBlock.DECAY_DISTANCE) >= LeavesBlock.DECAY_DISTANCE);
        BlockState bladState = GuhwaiiFeature.PALM_BLAD.get().defaultBlockState();
        for (BlockPos p : blad) {
            level.setBlock(p, bladState.setValue(LeavesBlock.DISTANCE, Math.max(1, afstand.get(p))), Block.UPDATE_CLIENTS);
        }
        // coconuts under the fronds next to the top
        List<BlockPos> plekken = new ArrayList<>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos p = top.relative(d);
            if (blad.contains(p.above()) && level.isEmptyBlock(p)) {
                plekken.add(p);
            }
        }
        java.util.Collections.shuffle(plekken, new java.util.Random(random.nextLong()));
        int noten = Math.min(plekken.size(), 2 + random.nextInt(3));
        for (int i = 0; i < noten; i++) {
            level.setBlock(plekken.get(i), GuhwaiiFeature.KOKOSNOOT.get().defaultBlockState().setValue(GuhwaiiBlokken.HANGEND, true)
                    .setValue(GuhwaiiBlokken.RIJP, random.nextInt(3)), Block.UPDATE_CLIENTS);
        }
        // now and then a fallen ripe coconut in the sand at its foot
        if (random.nextInt(3) == 0) {
            BlockPos voet = grond.above().relative(Direction.Plane.HORIZONTAL.getRandomDirection(random), 1 + random.nextInt(2));
            voet = new BlockPos(voet.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, voet.getX(), voet.getZ()), voet.getZ());
            if (level.isEmptyBlock(voet) && level.getBlockState(voet.below()).isFaceSturdy(level, voet.below(), Direction.UP)
                    && level.getFluidState(voet).isEmpty()) {
                level.setBlock(voet, GuhwaiiFeature.KOKOSNOOT.get().defaultBlockState().setValue(GuhwaiiBlokken.HANGEND, false)
                        .setValue(GuhwaiiBlokken.RIJP, 2), Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }

    /** Adds the leaves from a to b (both included) with face-adjacent steps: x first, then z, then y. */
    static void verbind(Set<BlockPos> blad, BlockPos a, BlockPos b) {
        int x = a.getX(), y = a.getY(), z = a.getZ();
        blad.add(a);
        while (x != b.getX() || y != b.getY() || z != b.getZ()) {
            if (x != b.getX()) {
                x += Integer.signum(b.getX() - x);
            } else if (z != b.getZ()) {
                z += Integer.signum(b.getZ() - z);
            } else {
                y += Integer.signum(b.getY() - y);
            }
            blad.add(new BlockPos(x, y, z));
        }
    }

    // =================================================================================================================
    // the reef
    // =================================================================================================================

    public static class Rif extends Feature<NoneFeatureConfiguration> {
        private static final Block[] KORAAL = {Blocks.BRAIN_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK, Blocks.TUBE_CORAL_BLOCK,
                Blocks.HORN_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK};
        private static final Block[] WAAIER = {Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.TUBE_CORAL_FAN, Blocks.HORN_CORAL_FAN,
                Blocks.FIRE_CORAL_FAN};
        private static final Block[] WANDWAAIER = {Blocks.BRAIN_CORAL_WALL_FAN, Blocks.BUBBLE_CORAL_WALL_FAN, Blocks.TUBE_CORAL_WALL_FAN,
                Blocks.HORN_CORAL_WALL_FAN, Blocks.FIRE_CORAL_WALL_FAN};
        private static final Block[] STRUIK = {Blocks.BRAIN_CORAL, Blocks.BUBBLE_CORAL, Blocks.TUBE_CORAL, Blocks.HORN_CORAL, Blocks.FIRE_CORAL};

        public Rif(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos o = context.origin();
            int vloerY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, o.getX(), o.getZ()) - 1;
            BlockPos vloer = new BlockPos(o.getX(), vloerY, o.getZ());
            int diep = 0;
            while (diep < 16 && level.getFluidState(vloer.above(diep + 1)).is(Fluids.WATER)) {
                diep++;
            }
            if (diep < 2 || diep > 14 || BouwRuimte.inBuilding(level, vloer.above())) {
                return false;
            }
            int oppervlak = vloerY + diep;               // the top water block
            int r = 2 + random.nextInt(3);
            int kleur = random.nextInt(KORAAL.length);
            List<BlockPos> toppen = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz) + random.nextDouble() * 0.8;
                    if (d > r + 0.3) {
                        continue;
                    }
                    int x = o.getX() + dx, z = o.getZ() + dz;
                    int bodem = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                    int bult = (int) Math.round((r - d) * (0.8 + random.nextDouble() * 0.6));
                    bult = Math.min(bult, Math.max(0, oppervlak - 1 - bodem - 1));   // (one water block stays over it at least)
                    int y = bodem;
                    for (int k = 1; k <= bult; k++) {
                        BlockPos p = new BlockPos(x, bodem + k, z);
                        if (!level.getFluidState(p).is(Fluids.WATER) || !level.getBlockState(p).is(Blocks.WATER)) {
                            break;
                        }
                        Block b = random.nextInt(3) == 0 ? OnderwaterFeature.KAASKORAALBLOK.get()
                                : KORAAL[random.nextInt(5) < 3 ? kleur : random.nextInt(KORAAL.length)];
                        level.setBlock(p, b.defaultBlockState(), Block.UPDATE_CLIENTS);
                        y = bodem + k;
                    }
                    toppen.add(new BlockPos(x, y + 1, z));
                }
            }
            // on top: kaaskoraal trees (breathe!), coral bushes and fans, sea pickles; seagrass around the foot; fans on the sides
            for (BlockPos p : toppen) {
                if (!level.getBlockState(p).is(Blocks.WATER) || !level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                    continue;
                }
                int kies = random.nextInt(10);
                BlockState s;
                if (kies < 2) {
                    s = OnderwaterFeature.KAASKORAAL.get().defaultBlockState().setValue(KaaskoraalBlock.WATERLOGGED, true);
                } else if (kies < 4) {
                    s = WAAIER[random.nextInt(WAAIER.length)].defaultBlockState();
                } else if (kies < 6) {
                    s = STRUIK[random.nextInt(STRUIK.length)].defaultBlockState();
                } else if (kies < 7) {
                    s = Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 1 + random.nextInt(4));
                } else if (kies < 9 && level.getBlockState(p.above()).is(Blocks.WATER)) {
                    s = Blocks.SEAGRASS.defaultBlockState();
                } else {
                    continue;
                }
                if (s.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    s = s.setValue(BlockStateProperties.WATERLOGGED, true);
                }
                if (s.canSurvive(level, p)) {
                    level.setBlock(p, s, Block.UPDATE_CLIENTS);
                }
            }
            for (int i = 0; i < 6; i++) {
                BlockPos p = o.offset(random.nextInt(2 * r + 1) - r, 0, random.nextInt(2 * r + 1) - r);
                p = new BlockPos(p.getX(), vloerY + 1 + random.nextInt(Math.max(1, Math.min(3, diep - 1))), p.getZ());
                Direction kant = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos muur = p.relative(kant.getOpposite());
                if (level.getBlockState(p).is(Blocks.WATER) && level.getBlockState(muur).is(BlockTags.CORAL_BLOCKS)) {
                    BlockState fan = WANDWAAIER[random.nextInt(WANDWAAIER.length)].defaultBlockState().setValue(BaseCoralWallFanBlock.FACING, kant)
                            .setValue(BlockStateProperties.WATERLOGGED, true);
                    if (fan.canSurvive(level, p)) {
                        level.setBlock(p, fan, Block.UPDATE_CLIENTS);
                    }
                }
            }
            return true;
        }
    }

    // =================================================================================================================
    // Schilly-eitjes on the beach
    // =================================================================================================================

    public static class Nestje extends Feature<NoneFeatureConfiguration> {
        public Nestje(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos o = context.origin();
            BlockPos zand = new BlockPos(o.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ()) - 1, o.getZ());
            if (!level.getBlockState(zand).is(BlockTags.SAND) || zand.getY() < 62 || zand.getY() > 68 || BouwRuimte.inBuilding(level, zand.above())) {
                return false;
            }
            boolean water = false;
            for (int dx = -8; dx <= 8 && !water; dx += 2) {
                for (int dz = -8; dz <= 8 && !water; dz += 2) {
                    water = level.getFluidState(new BlockPos(zand.getX() + dx, 62, zand.getZ() + dz)).is(Fluids.WATER);
                }
            }
            if (!water) {
                return false;
            }
            int n = 0;
            for (int i = 0; i < 4 && n < 3; i++) {
                int x = zand.getX() + random.nextInt(5) - 2, z = zand.getZ() + random.nextInt(5) - 2;
                BlockPos p = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z);
                if (level.isEmptyBlock(p) && level.getBlockState(p.below()).is(BlockTags.SAND) && level.getFluidState(p).isEmpty()) {
                    level.setBlock(p, GuhwaiiFeature.SCHILLY_EITJES.get().defaultBlockState().setValue(GuhwaiiBlokken.EITJES, 1 + random.nextInt(4))
                            .setValue(GuhwaiiBlokken.RIJP, random.nextInt(2)), Block.UPDATE_CLIENTS);
                    n++;
                }
            }
            return n > 0;
        }
    }
}
