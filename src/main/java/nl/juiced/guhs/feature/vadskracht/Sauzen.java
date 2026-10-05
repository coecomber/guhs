package nl.juiced.guhs.feature.vadskracht;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.registry.ModFluids;

/**
 * Fluids in and out of anything that holds fluid (a Sausvat, a pump, a machine's tank), through NeoForge 26.1's fluid
 * capability; amounts in mB (1000 = a bucket). Like {@link Kisten}: every method opens its own transaction and commits it
 * (unless {@code simuleer}). The fluids of the Guh-technologie are the tag {@link #TECHNIEK}: kaassaus, kaasfrituursaus,
 * water and milk (NeoForge's milk fluid, switched on by {@link VadskrachtFeature}).
 */
public final class Sauzen {
    /** Fluid tag {@code guhs:techniek_sauzen} (tools/features/vadskracht.py). */
    public static final TagKey<Fluid> TECHNIEK = TagKey.create(Registries.FLUID, Guhs.id("techniek_sauzen"));
    /** A bucket, in mB. */
    public static final int EMMER = 1000;

    /** The fluid handler of the block here, seen from this side; null when it holds no fluid. */
    @Nullable
    public static ResourceHandler<FluidResource> van(Level level, BlockPos pos, @Nullable Direction kant) {
        return level.isLoaded(pos) ? level.getCapability(Capabilities.Fluid.BLOCK, pos, kant) : null;
    }

    /** Puts up to mb of this fluid in; returns what was accepted. */
    public static int stop(ResourceHandler<FluidResource> in, FluidResource wat, int mb, boolean simuleer) {
        if (wat.isEmpty() || mb <= 0) {
            return 0;
        }
        try (Transaction tx = Kisten.open()) {
            int erin = in.insert(wat, mb, tx);
            if (!simuleer) {
                tx.commit();
            }
            return erin;
        }
    }

    /** Takes up to mb of this fluid out; returns what came out. */
    public static int neem(ResourceHandler<FluidResource> uit, FluidResource wat, int mb, boolean simuleer) {
        if (wat.isEmpty() || mb <= 0) {
            return 0;
        }
        try (Transaction tx = Kisten.open()) {
            int eruit = uit.extract(wat, mb, tx);
            if (!simuleer) {
                tx.commit();
            }
            return eruit;
        }
    }

    /** Moves up to mb from one handler to another (wat null: whatever fluid comes first); returns what moved. */
    public static int verplaats(ResourceHandler<FluidResource> van, ResourceHandler<FluidResource> naar, @Nullable FluidResource wat, int mb) {
        if (mb <= 0) {
            return 0;
        }
        try (Transaction tx = Kisten.open()) {
            int n;
            if (wat == null) {
                var eerste = ResourceHandlerUtil.moveFirst(van, naar, soort -> true, mb, tx);
                n = eerste == null ? 0 : eerste.amount();
            } else {
                n = ResourceHandlerUtil.move(van, naar, wat::equals, mb, tx);
            }
            tx.commit();
            return n;
        }
    }

    /** The fluid in the handler's first filled tank (EMPTY when there is none). */
    public static FluidResource eerste(ResourceHandler<FluidResource> h) {
        for (int i = 0; i < h.size(); i++) {
            if (!h.getResource(i).isEmpty() && h.getAmountAsLong(i) > 0) {
                return h.getResource(i);
            }
        }
        return FluidResource.EMPTY;
    }

    /** How many mB of this fluid the handler holds. */
    public static long tel(ResourceHandler<FluidResource> h, FluidResource wat) {
        long n = 0;
        for (int i = 0; i < h.size(); i++) {
            if (h.getResource(i).equals(wat)) {
                n += h.getAmountAsLong(i);
            }
        }
        return n;
    }

    /** Is this one of the fluids of the Guh-technologie? */
    public static boolean isTechniek(FluidResource wat) {
        return !wat.isEmpty() && wat.typeHolder().is(TECHNIEK);
    }

    public static FluidResource kaassaus() {
        return FluidResource.of(ModFluids.KAAS_SAUS.get());
    }

    public static FluidResource frituursaus() {
        return FluidResource.of(BarbecuetherFeature.KAASFRITUURSAUS.get());
    }

    public static FluidResource water() {
        return FluidResource.of(Fluids.WATER);
    }

    public static FluidResource melk() {
        return FluidResource.of(NeoForgeMod.MILK.get());
    }

    private Sauzen() {
    }
}
