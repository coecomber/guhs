package nl.juiced.guhs.feature.techsaus;

import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * What a hose sees of a machine's tanks: sauce only goes IN (a machine that uses sauce: nobody drinks it back out) or only
 * comes OUT (the pump). The machine itself, and a player with a bucket, work on the tanks directly.
 */
public final class Eenrichting extends DelegatingResourceHandler<FluidResource> {
    private final boolean in, uit;

    public Eenrichting(ResourceHandler<FluidResource> tanks, boolean in, boolean uit) {
        super(tanks);
        this.in = in;
        this.uit = uit;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return in && super.isValid(index, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return in ? super.insert(index, resource, amount, transaction) : 0;
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        return in ? super.insert(resource, amount, transaction) : 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return uit ? super.extract(index, resource, amount, transaction) : 0;
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        return uit ? super.extract(resource, amount, transaction) : 0;
    }
}
