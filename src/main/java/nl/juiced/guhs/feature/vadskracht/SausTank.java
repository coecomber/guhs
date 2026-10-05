package nl.juiced.guhs.feature.vadskracht;

import java.util.function.Predicate;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * One fluid tank for a block entity (a Sausvat, a machine that takes sauce): {@code mb} mB of ONE fluid at a time, only
 * fluids that {@code mag} allows, and {@code veranderd} runs whenever the content changed (after a committed transaction):
 * call {@code setChanged()} and sync there. It IS the handler to register for {@code Capabilities.Fluid.BLOCK}.
 * <pre>
 * private final SausTank tank = new SausTank(4 * Sauzen.EMMER, Sauzen::isTechniek, this::sync);
 * // saveAdditional: tank.opslaan(uit, "Tank");   loadAdditional: tank.laden(in, "Tank");
 * // capabilities:   event.registerBlockEntity(Capabilities.Fluid.BLOCK, MY_BE.get(), (be, kant) -> be.tank());
 * </pre>
 * Saved with the block entity, so a block entity that sends its saved data to the client ({@code getUpdateTag}) has the
 * amount there too ({@link #inhoud}, {@link #vulling}).
 */
public class SausTank extends FluidStacksResourceHandler {
    private final Predicate<FluidResource> mag;
    private final Runnable veranderd;

    public SausTank(int mb, Predicate<FluidResource> mag, Runnable veranderd) {
        super(1, mb);
        this.mag = mag;
        this.veranderd = veranderd;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return mag.test(resource);
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        veranderd.run();
    }

    /** What is in it (EMPTY when empty). */
    public FluidResource saus() {
        return getResource(0);
    }

    /** How much is in it, in mB. */
    public int inhoud() {
        return getAmountAsInt(0);
    }

    public int max() {
        return capacity;
    }

    public int ruimte() {
        return capacity - inhoud();
    }

    public boolean isLeeg() {
        return inhoud() <= 0;
    }

    /** 0..1. */
    public float vulling() {
        return capacity <= 0 ? 0 : inhoud() / (float) capacity;
    }

    /** Pours up to mb in (its own transaction); returns what went in. */
    public int vul(FluidResource wat, int mb, boolean simuleer) {
        return Sauzen.stop(this, wat, mb, simuleer);
    }

    /** Taps up to mb of what is in it (its own transaction); returns what came out. */
    public int tap(int mb, boolean simuleer) {
        return isLeeg() ? 0 : Sauzen.neem(this, saus(), mb, simuleer);
    }

    /** Sets the content directly (commands, tests, the work of the machine itself). */
    public void zet(FluidResource wat, int mb) {
        try (Transaction tx = Kisten.open()) {
            if (!isLeeg()) {
                extract(0, saus(), inhoud(), tx);
            }
            if (!wat.isEmpty() && mb > 0) {
                insert(0, wat, Math.min(mb, capacity), tx);
            }
            tx.commit();
        }
    }

    public void opslaan(ValueOutput uit, String naam) {
        serialize(uit.child(naam));
    }

    public void laden(ValueInput in, String naam) {
        deserialize(in.childOrEmpty(naam));
    }
}
