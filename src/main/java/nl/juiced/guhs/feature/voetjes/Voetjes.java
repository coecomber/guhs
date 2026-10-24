package nl.juiced.guhs.feature.voetjes;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForgeMod;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.registry.ModFluids;

/**
 * 1.7.0 (ronde 26): the lopers of the FEET slot. A guh wearing kaassauslopers walks on top of every kaassaus (the
 * Guhmension's kaas saus and the Barbecuether's hot kaasfrituursaus), one wearing lavalopers on real lava too: like a
 * strider it stands on a source block, floats up out of flowing sauce, never burns there and follows you across.
 * GuhEntity asks {@link #kanStaanOp} / {@link #heeftLopers} / {@link #vuurvast}; {@link VoetjesFeature} does the rest
 * (floating, pathfinding, the kleermaker's upgrade).
 */
public final class Voetjes {
    /** What a guh with lopers stands on: the top half of a fluid source block (the strider's shape). */
    public static final VoxelShape VLOEISTOFVLOER = Block.column(16.0, 0.0, 8.0);

    /** Which lopers a guh wears. */
    public enum Soort {
        GEEN, KAAS, LAVA
    }

    public static Soort soort(GuhEntity guh) {
        GuhClothes voeten = guh.getClothes(GuhClothes.Slot.FEET);
        if (voeten == GuhClothes.LAVALOPERS) {
            return Soort.LAVA;
        }
        return voeten == GuhClothes.KAASSAUSLOPERS ? Soort.KAAS : Soort.GEEN;
    }

    public static boolean heeftLopers(GuhEntity guh) {
        return soort(guh) != Soort.GEEN;
    }

    /** Every kaassaus variant: kaas saus and the hot kaasfrituursaus (source or flowing). */
    public static boolean isKaassaus(FluidState fluid) {
        if (fluid.isEmpty()) {
            return false;
        }
        var type = fluid.getFluidType();
        return type == ModFluids.KAAS_SAUS_TYPE.get() || type == BarbecuetherFeature.KAASFRITUURSAUS_TYPE.get();
    }

    public static boolean isLava(FluidState fluid) {
        return !fluid.isEmpty() && (fluid.is(FluidTags.LAVA) || fluid.getFluidType() == NeoForgeMod.LAVA_TYPE.value());
    }

    /** Can lopers of this kind walk on this fluid? */
    public static boolean loopbaar(Soort soort, FluidState fluid) {
        return switch (soort) {
            case GEEN -> false;
            case KAAS -> isKaassaus(fluid);
            case LAVA -> isKaassaus(fluid) || isLava(fluid);
        };
    }

    /** GuhEntity#canStandOnFluid. */
    public static boolean kanStaanOp(GuhEntity guh, FluidState fluid) {
        return loopbaar(soort(guh), fluid);
    }

    /**
     * GuhEntity#fireImmune: on (or in) a sauce its lopers walk on, nothing there burns it (no fire, no lava damage). Real
     * lava touching a guh in kaassauslopers still burns: those are only made for cheese.
     */
    public static boolean vuurvast(GuhEntity guh) {
        Soort soort = soort(guh);
        if (soort == Soort.GEEN) {
            return false;
        }
        Level level = guh.level();
        AABB box = guh.getBoundingBox().inflate(0.05).expandTowards(0, -0.6, 0);
        boolean op = false;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            FluidState f = level.getFluidState(p);
            if (f.isEmpty()) {
                continue;
            }
            if (loopbaar(soort, f)) {
                op = true;
            } else if (isLava(f)) {
                return false;
            }
        }
        return op;
    }

    /** Like the strider: standing on the sauce it is on the ground; sunk into it (flowing sauce) it bobs up. */
    public static void drijf(GuhEntity guh) {
        Soort soort = soort(guh);
        BlockPos pos = guh.blockPosition();
        if (!loopbaar(soort, guh.level().getFluidState(pos))) {
            return;
        }
        CollisionContext context = CollisionContext.of(guh);
        if (context.isAbove(VLOEISTOFVLOER, pos, true) && !loopbaar(soort, guh.level().getFluidState(pos.above()))) {
            guh.setOnGround(true);
        } else {
            guh.setDeltaMovement(guh.getDeltaMovement().scale(0.5).add(0.0, 0.05, 0.0));
        }
        guh.resetFallDistance();
    }

    /**
     * The pathfinding costs: with lopers a path may cross lava-type fluid (the kaasfrituursaus is one too). Kaassauslopers
     * only do that in the Barbecuether, where all lava is kaasfrituursaus (elsewhere a path can't tell them apart).
     */
    public static void zetPaden(GuhEntity guh) {
        Soort soort = soort(guh);
        boolean over = soort == Soort.LAVA || soort == Soort.KAAS && guh.level().dimension() == BarbecuetherFeature.BARBECUETHER;
        for (PathType type : new PathType[]{PathType.LAVA, PathType.FIRE_IN_NEIGHBOR, PathType.FIRE}) {
            float wil = over ? 0.0f : type.getMalus();
            if (guh.getPathfindingMalus(type) != wil) {
                guh.setPathfindingMalus(type, wil);
            }
        }
    }

    private Voetjes() {
    }
}
