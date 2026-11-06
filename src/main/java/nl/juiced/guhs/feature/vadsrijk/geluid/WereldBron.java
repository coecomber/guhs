package nl.juiced.guhs.feature.vadsrijk.geluid;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.tags.FluidTags;
import nl.juiced.guhs.feature.bio.blokkenwolk.Waterval;

/**
 * U-AUDIO2 (V4 "wereldgeluiden, SUBTIEL"): the world point sounds of Het Vadsrijk. What makes a sound is found by a light
 * client-side block scan around the player ({@link #soortOp}) plus a few fixed points from the world lists (the Kandijkom gondola
 * stations and the zone-3 lifts: {@link #vastePunten}). The client ({@code geluid.client.WereldGeluiden}) plays them soft, short
 * range and sparse, with a budget.
 * <ul>
 *   <li>{@code fontein}: a small fall of water (1-3 blocks: fountains, spouts, weirs). The BIG falls (4+) already have the soft
 *       rush of {@code bio.blokkenwolk.WatervalEffecten} in every dimension, so they are left alone (no doubling).</li>
 *   <li>{@code bijen}: beehives and bee nests; {@code smidse}: anvils and smithing tables (a few soft hammer taps);
 *       {@code windgong}: bells and the Klaterdal wind chime blocks; {@code kampvuur}: lit campfires (a soft extra crackle);
 *       {@code kraak}: gondola stations and lifts (a wooden creak); {@code markt}: three or more NPC guhs close together
 *       (a murmur of guh babble; found from the entities, not the blocks).</li>
 *   <li>Caves: footsteps get a short, soft echo underground (no event of its own: the step sound itself, later and lower).</li>
 * </ul>
 */
public final class WereldBron {
    public static final List<String> SOORTEN = List.of("fontein", "bijen", "smidse", "markt", "windgong", "kampvuur", "kraak");

    private WereldBron() {
    }

    public static String id(String soort) {
        return "vr.wereld." + soort;
    }

    /** What sound the block at pos makes (null = none). Water: only the FOOT of a small fall counts. */
    @Nullable
    public static String soortOp(BlockGetter level, BlockPos pos) {
        BlockState b = level.getBlockState(pos);
        if (b.is(Blocks.BEEHIVE) || b.is(Blocks.BEE_NEST)) {
            return "bijen";
        }
        if (b.is(BlockTags.ANVIL) || b.is(Blocks.SMITHING_TABLE)) {
            return "smidse";
        }
        if (b.is(Blocks.BELL)) {
            return "windgong";
        }
        if (b.getBlock() instanceof CampfireBlock && b.getValue(CampfireBlock.LIT)) {
            return "kampvuur";
        }
        FluidState f = b.getFluidState();
        if (!f.isEmpty() && f.is(FluidTags.WATER) && valt(f) && !valt(level.getFluidState(pos.below()))) {
            int h = 1;
            while (h < Waterval.GROOT && valt(level.getFluidState(pos.above(h)))) {
                h++;
            }
            return h < Waterval.GROOT ? "fontein" : null;
        }
        if (!b.isAir() && WINDGONG.computeIfAbsent(b.getBlock(), k -> BuiltInRegistries.BLOCK.getKey(k).getPath().contains("windgong"))) {
            return "windgong";   // U-GROOTTE (FPS): the name test once per block type, not per scanned block (700 per tick)
        }
        return null;
    }

    private static final java.util.Map<net.minecraft.world.level.block.Block, Boolean> WINDGONG = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean valt(FluidState f) {
        return !f.isEmpty() && f.is(FluidTags.WATER) && !f.isSource() && f.hasProperty(FlowingFluid.FALLING) && f.getValue(FlowingFluid.FALLING);
    }

    /** A fixed point source from the world lists. */
    public record Punt(BlockPos pos, String soort) {
    }

    private static List<Punt> vast;

    /** The fixed points (gondola stations, lift stops). Built once; empty if a world list is missing. */
    public static List<Punt> vastePunten() {
        if (vast == null) {
            List<Punt> l = new ArrayList<>();
            try {
                l.add(new Punt(nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.GONDEL_DAL, "kraak"));
                l.add(new Punt(nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.GONDEL_BERG, "kraak"));
                for (var lift : nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.LIFTEN) {
                    l.add(new Punt(lift.boven(), "kraak"));
                    l.add(new Punt(lift.onder(), "kraak"));
                }
            } catch (Throwable t) {
                // (a regenerated world list without these fields: no fixed creaks, the rest still works)
            }
            vast = List.copyOf(l);
        }
        return vast;
    }
}
