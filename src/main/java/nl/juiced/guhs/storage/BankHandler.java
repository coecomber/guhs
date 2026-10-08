package nl.juiced.guhs.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * bbq2: the Bank Guh as an item capability (NeoForge 26.1: a transactional {@link ResourceHandler}), so Knabbelbuizen,
 * hoppers, the Hapluikje and chore guhs can reach its stomach ({@link BankStorage}).
 * <ul>
 * <li><b>In</b>: always, up to the cap per kind of item ({@link BankStorage#CAP}; no cap once upgraded). What does not fit
 * is simply not taken, so the one who brings it keeps it.</li>
 * <li><b>Out</b>: NEVER through the capability, upgraded or not (the user's decision B6): a hopper under the bank, a plain
 * Knabbelbuis piece or a pick-up Haltepaaltje gets nothing, so a store can never be emptied by accident. A bank can always
 * be looked into (a Voorraadmeter counts what is in it).</li>
 * <li><b>The one way out</b> is {@link #filterkant()}: the door of an UPGRADED bank for a Filterstuk, the piece on which
 * its owner says what may be taken and how much has to stay ({@code feature/techbuis/FilterBlockEntity}). It is the same
 * stomach, the same slots and the same undo log, only with a working {@code extract}; it is not a capability, so nothing
 * finds it by accident.</li>
 * </ul>
 * The stomach has no slots, so this shows one slot per kind of item plus one empty slot at the end (where a new kind goes
 * in). A kind keeps its slot number for as long as it is in the bank; a kind that ran out leaves an empty slot that the
 * next new kind takes, so somebody walking over the slots is never thrown off by a shift.
 * <p>
 * Changes inside a transaction are written in an undo log, so opening a (nested) transaction costs nothing however much
 * the bank holds, and an aborted one puts everything back exactly.
 */
public final class BankHandler implements ResourceHandler<ItemResource> {
    private final BankStorage storage;
    /** What may never go in (loaned things); tested on the resource. */
    private final Predicate<ItemResource> geweigerd;
    /** The slots: {@link ItemResource#EMPTY} is a free one. */
    private final List<ItemResource> vakken = new ArrayList<>();
    private final Map<ItemResource, Integer> vakVan = new HashMap<>();
    private long gezien = -1;
    private int vrij;
    private long vrijBij = -2;

    /** One undone step: this kind held this many before, and the screen's version was this. */
    private record Stap(ItemResource soort, long was, int versie) {
    }

    private final List<Stap> logboek = new ArrayList<>();
    private final SnapshotJournal<Integer> journaal = new SnapshotJournal<>() {
        @Override
        protected Integer createSnapshot() {
            return logboek.size();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            for (int i = logboek.size() - 1; i >= snapshot; i--) {
                Stap stap = logboek.remove(i);
                storage.undo(stap.soort(), stap.was(), stap.versie());
            }
        }

        @Override
        protected void onRootCommit(Integer originalState) {
            if (!isInTransaction()) {
                logboek.clear();
            }
        }
    };

    public BankHandler(BankStorage storage, Predicate<ItemResource> geweigerd) {
        this.storage = storage;
        this.geweigerd = geweigerd;
    }

    /** Brings the slots up to date with the stomach (after the screen, a chore guh or a rollback changed it). */
    private void bij() {
        if (gezien == storage.mutations()) {
            return;
        }
        gezien = storage.mutations();
        for (int i = 0; i < vakken.size(); i++) {
            ItemResource soort = vakken.get(i);
            if (!soort.isEmpty() && !storage.has(soort)) {
                vakken.set(i, ItemResource.EMPTY);
                vakVan.remove(soort);
            }
        }
        int plek = 0;
        for (ItemResource soort : storage.keys()) {
            if (vakVan.containsKey(soort)) {
                continue;
            }
            while (plek < vakken.size() && !vakken.get(plek).isEmpty()) {
                plek++;
            }
            if (plek < vakken.size()) {
                vakken.set(plek, soort);
            } else {
                vakken.add(soort);
            }
            vakVan.put(soort, plek);
        }
        while (!vakken.isEmpty() && vakken.get(vakken.size() - 1).isEmpty()) {
            vakken.remove(vakken.size() - 1);   // (free slots at the end only: nothing shifts)
        }
    }

    /** The slot a new kind of item would get: the first free one (maybe the extra one at the end). Kept until the stomach changes. */
    private int vrijVak() {
        if (vrijBij != gezien) {
            vrijBij = gezien;
            vrij = vakken.size();
            for (int i = 0; i < vakken.size(); i++) {
                if (vakken.get(i).isEmpty()) {
                    vrij = i;
                    break;
                }
            }
        }
        return vrij;
    }

    private void zet(ItemResource soort, long aantal, TransactionContext transaction) {
        journaal.updateSnapshots(transaction);
        logboek.add(new Stap(soort, storage.amount(soort), storage.version()));
        storage.set(soort, aantal);
    }

    @Override
    public int size() {
        bij();
        return vakken.size() + 1;
    }

    @Override
    public ItemResource getResource(int index) {
        bij();
        return index >= 0 && index < vakken.size() ? vakken.get(index) : ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        ItemResource soort = getResource(index);
        return soort.isEmpty() ? 0 : storage.amount(soort);
    }

    /** The cap; a slot from before the cap that holds more says what it holds (never less than its amount). */
    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return Math.max(storage.cap(), getAmountAsLong(index));
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return !resource.isEmpty() && !geweigerd.test(resource);
    }

    /** In: a kind that is in the bank only into its own slot, a new kind only into the first free slot. */
    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        bij();
        Integer eigen = vakVan.get(resource);
        if (index != (eigen != null ? eigen : vrijVak())) {
            return 0;
        }
        return insert(resource, amount, transaction);
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || geweigerd.test(resource)) {
            return 0;
        }
        int erin = (int) Math.min(amount, storage.room(resource));
        if (erin <= 0) {
            return 0;
        }
        zet(resource, storage.amount(resource) + erin, transaction);
        return erin;
    }

    /** Out: never through the capability (hoppers, plain tube pieces, pick-up poles); see {@link #filterkant()}. */
    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return 0;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return 0;
    }

    /**
     * The bank as a Filterstuk sees it: everything of this handler (the same slots, the same way in), plus the one way
     * out: taking works here, from an UPGRADED bank only. The same object every time.
     */
    public ResourceHandler<ItemResource> filterkant() {
        return filterkant;
    }

    private final ResourceHandler<ItemResource> filterkant = new ResourceHandler<>() {
        @Override
        public int size() {
            return BankHandler.this.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return BankHandler.this.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return BankHandler.this.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return BankHandler.this.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return BankHandler.this.isValid(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return BankHandler.this.insert(index, resource, amount, transaction);
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            return BankHandler.this.insert(resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return resource.equals(BankHandler.this.getResource(index)) ? neem(resource, amount, transaction) : 0;
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return neem(resource, amount, transaction);
        }
    };

    /** Takes up to this many of this kind out (inside the transaction): only an upgraded bank gives. */
    private int neem(ItemResource resource, int amount, TransactionContext transaction) {
        if (amount == 0 || !storage.isUpgraded()) {
            return 0;
        }
        long heeft = storage.amount(resource);
        int eruit = (int) Math.min(amount, heeft);
        if (eruit <= 0) {
            return 0;
        }
        zet(resource, heeft - eruit, transaction);
        return eruit;
    }
}
