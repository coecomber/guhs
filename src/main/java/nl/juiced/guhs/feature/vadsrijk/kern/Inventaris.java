package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.compat.ExtraVakken;

/**
 * Het Vadsrijk: "eigen kist per wereld" (ronde 2). Inside the dimension the player has their Vadsrijk inventory; their normal
 * inventory waits in {@link SpelerData#opgeborgen}, and the other way round outside. Both live in the player attachment, so
 * they are saved in the same player.dat as the live inventory: a crash can never keep one and lose the other.
 * <p>Rules (never lose an item):
 * <ul>
 *   <li>the cursor item and the 2x2 crafting grid go back into the inventory first;</li>
 *   <li>every slot (36 + armour + offhand + body + saddle) is stored as a raw tag; a slot is only cleared once its tag is stored;</li>
 *   <li>a stash that is unexpectedly not empty is never overwritten: it moves to {@link SpelerData#gevonden};</li>
 *   <li>an item that can't be read back or doesn't fit goes to {@link SpelerData#gevonden} (as a tag, marked with its world),
 *   which {@code /guhs vadsrijk kist} hands out in the right world.</li>
 * </ul>
 * Kern calls {@link #naarRijk} / {@link #naarThuis} from its reconciler only (every dimension change, login and every second).
 */
public final class Inventaris {
    public static final String WERELD = "vr_wereld", RIJK = "rijk", THUIS = "thuis", EXTRA = "vr_extra", EXTRA_I = "vr_extra_i";
    /** The slot number accessory items get in their tag (outside the vanilla inventory: they go back by EXTRA + EXTRA_I). */
    static final int EXTRA_SLOT = 250;   // (the codec stores Slot as an unsigned byte)

    private Inventaris() {
    }

    /** The normal inventory into the stash, the Vadsrijk inventory out. No-op when already inside. */
    public static boolean naarRijk(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        if (d.binnen) {
            return false;
        }
        wissel(p, d, d.opgeborgen, d.rijkInventaris, THUIS, RIJK, true);
        d.binnen = true;
        return true;
    }

    /** The Vadsrijk inventory into its stash, the normal inventory back. No-op when already outside. */
    public static boolean naarThuis(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        if (!d.binnen) {
            return false;
        }
        wissel(p, d, d.rijkInventaris, d.opgeborgen, RIJK, THUIS, false);
        d.binnen = false;
        return true;
    }

    private static void wissel(ServerPlayer p, SpelerData d, List<CompoundTag> opslag, List<CompoundTag> uitHaal, String huidigeWereld,
            String nieuweWereld, boolean naarRijk) {
        handenLeeg(p);
        if (!opslag.isEmpty()) {   // never overwrite: whatever is there goes to the found items (of its own world)
            for (CompoundTag t : opslag) {
                d.gevonden.add(merk(t, huidigeWereld));
            }
            opslag.clear();
        }
        Inventory inv = p.getInventory();
        RegistryOps<Tag> ops = ops(p);
        if (naarRijk) {
            d.opgeborgenSlot = inv.getSelectedSlot();
        } else {
            d.rijkSlot = inv.getSelectedSlot();
        }
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack s = inv.getItem(slot);
            if (s.isEmpty()) {
                continue;
            }
            Optional<Tag> t = ItemStackWithSlot.CODEC.encodeStart(ops, new ItemStackWithSlot(slot, s.copy())).result();
            if (t.isPresent() && t.get() instanceof CompoundTag c) {
                opslag.add(c);
                inv.setItem(slot, ItemStack.EMPTY);
            }
            // (a stack that cannot be written stays where it is: carried into the other world rather than lost)
        }
        // accessory slots of Curios / Accessories (soft compat): same rule, marked with their source and index
        for (ExtraVakken.Bron bron : ExtraVakken.ALLE) {
            List<ExtraVakken.Vak> vakken = bron.vakken(p);
            for (int i = 0; i < vakken.size(); i++) {
                ExtraVakken.Vak v = vakken.get(i);
                ItemStack s = v.get();
                if (s == null || s.isEmpty()) {
                    continue;
                }
                Optional<Tag> t = ItemStackWithSlot.CODEC.encodeStart(ops, new ItemStackWithSlot(EXTRA_SLOT, s.copy())).result();
                if (t.isPresent() && t.get() instanceof CompoundTag c) {
                    c.putString(EXTRA, bron.id());
                    c.putInt(EXTRA_I, i);
                    try {
                        v.set(ItemStack.EMPTY);
                        opslag.add(c);
                    } catch (RuntimeException e) {
                        // (the slot refused: the item stays equipped, nothing is duplicated)
                    }
                }
            }
        }
        List<CompoundTag> terug = new ArrayList<>(uitHaal);
        uitHaal.clear();
        laad(p, d, terug, nieuweWereld);
        inv.setSelectedSlot(Math.max(0, Math.min(8, naarRijk ? d.rijkSlot : d.opgeborgenSlot)));
        inv.setChanged();
        p.inventoryMenu.broadcastFullState();
        if (p.containerMenu != p.inventoryMenu) {
            p.containerMenu.broadcastFullState();
        }
    }

    /** Puts these tags into the inventory (their own slot, else anywhere, else into the found items). */
    static void laad(ServerPlayer p, SpelerData d, List<CompoundTag> tags, String wereld) {
        Inventory inv = p.getInventory();
        RegistryOps<Tag> ops = ops(p);
        List<ItemStack> later = new ArrayList<>();
        java.util.Map<String, List<ExtraVakken.Vak>> extra = new java.util.HashMap<>();
        for (CompoundTag t : tags) {
            Optional<ItemStackWithSlot> s = ItemStackWithSlot.CODEC.parse(ops, t).result();
            if (s.isEmpty() || s.get().stack().isEmpty()) {
                d.gevonden.add(merk(t, wereld));
                continue;
            }
            String bronId = t.getStringOr(EXTRA, "");
            if (!bronId.isEmpty()) {
                ExtraVakken.Bron bron = ExtraVakken.van(bronId);
                List<ExtraVakken.Vak> vakken = bron == null ? List.of() : extra.computeIfAbsent(bronId, k -> bron.vakken(p));
                int i = t.getIntOr(EXTRA_I, -1);
                boolean gezet = false;
                if (i >= 0 && i < vakken.size()) {
                    try {
                        ItemStack nu = vakken.get(i).get();
                        if (nu == null || nu.isEmpty()) {
                            vakken.get(i).set(s.get().stack());
                            gezet = true;
                        }
                    } catch (RuntimeException e) {
                        gezet = false;
                    }
                }
                if (!gezet) {
                    later.add(s.get().stack());
                }
                continue;
            }
            int slot = s.get().slot();
            if (slot >= 0 && slot < inv.getContainerSize() && inv.getItem(slot).isEmpty()) {
                inv.setItem(slot, s.get().stack());
            } else {
                later.add(s.get().stack());
            }
        }
        for (ItemStack s : later) {
            inv.add(s);
            if (!s.isEmpty()) {
                bewaar(p, d, s, wereld);
            }
        }
    }

    /** Hands out the found items of the world the player is in now (as many as fit). Returns how many stacks came out. */
    public static int haalGevonden(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        String wereld = d.binnen ? RIJK : THUIS;
        RegistryOps<Tag> ops = ops(p);
        int n = 0;
        List<ItemStack> rest = new ArrayList<>();
        Iterator<CompoundTag> it = d.gevonden.iterator();
        while (it.hasNext()) {
            CompoundTag t = it.next();
            if (!wereld.equals(t.getStringOr(WERELD, THUIS))) {
                continue;
            }
            Optional<ItemStackWithSlot> s = ItemStackWithSlot.CODEC.parse(ops, t).result();
            if (s.isEmpty()) {
                continue;   // (still unreadable: it stays)
            }
            ItemStack st = s.get().stack().copy();
            int voor = st.getCount();
            p.getInventory().add(st);
            if (st.getCount() == voor) {
                break;   // full
            }
            it.remove();
            n++;
            if (!st.isEmpty()) {
                rest.add(st);
                break;
            }
        }
        for (ItemStack s : rest) {
            bewaar(p, d, s, wereld);
        }
        return n;
    }

    public static int gevondenAantal(ServerPlayer p, boolean rijk) {
        String w = rijk ? RIJK : THUIS;
        return (int) Spelers.data(p).gevonden.stream().filter(t -> w.equals(t.getStringOr(WERELD, THUIS))).count();
    }

    /**
     * Public (integration 1.10.0, for economie): keeps a stack that did not fit in the player's "found items" of the world the
     * player is in now (handed out later by {@link #haalGevonden}).
     */
    public static void bewaar(ServerPlayer p, ItemStack s) {
        if (!s.isEmpty()) {
            SpelerData d = Spelers.data(p);
            bewaar(p, d, s.copy(), d.binnen ? RIJK : THUIS);
        }
    }

    /**
     * (fix round 3) An item for the NORMAL inventory (post from the rest of the mod, e.g. the makelaar's postcards) while the
     * player may be in the Vadsrijk: inside it waits with the stored normal inventory and comes out on the way home (any free
     * slot, else the found items); outside it goes straight into the inventory.
     */
    public static void legThuis(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) {
            return;
        }
        SpelerData d = Spelers.data(p);
        if (!d.binnen) {
            if (!p.getInventory().add(s) && !s.isEmpty()) {
                p.drop(s, false);
            }
            return;
        }
        ItemStackWithSlot.CODEC.encodeStart(ops(p), new ItemStackWithSlot(THUIS_POST_SLOT, s.copy())).result().ifPresent(t -> {
            if (t instanceof CompoundTag c) {
                d.opgeborgen.add(c);   // (slot out of range: laad puts it anywhere)
            }
        });
    }

    static final int THUIS_POST_SLOT = 251;

    private static void bewaar(ServerPlayer p, SpelerData d, ItemStack s, String wereld) {
        ItemStackWithSlot.CODEC.encodeStart(ops(p), new ItemStackWithSlot(0, s)).result().ifPresent(t -> {
            if (t instanceof CompoundTag c) {
                d.gevonden.add(merk(c, wereld));
            }
        });
    }

    private static CompoundTag merk(CompoundTag t, String wereld) {
        CompoundTag c = t.copy();
        if (!c.contains(WERELD)) {
            c.putString(WERELD, wereld);
        }
        return c;
    }

    /** The cursor item and the 2x2 crafting grid back into the inventory (closing whatever menu is open). */
    private static void handenLeeg(ServerPlayer p) {
        // Y-SCHEIDING: nothing the swap empties may be DROPPED (a full inventory made placeItemBackInInventory / a closing menu drop
        // the cursor or grid item on the OTHER world's ground): scheiding.Vangnet turns those drops into "gevonden" of their own world
        nl.juiced.guhs.feature.vadsrijk.scheiding.Vangnet.tijdens(p, () -> handenLeeg0(p));
    }

    private static void handenLeeg0(ServerPlayer p) {
        if (p.containerMenu != p.inventoryMenu) {
            p.closeContainer();
        }
        ItemStack cursor = p.inventoryMenu.getCarried();
        if (!cursor.isEmpty()) {
            p.inventoryMenu.setCarried(ItemStack.EMPTY);
            p.getInventory().placeItemBackInInventory(cursor);
        }
        var grid = p.inventoryMenu.getCraftSlots();
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack s = grid.removeItemNoUpdate(i);
            if (!s.isEmpty()) {
                p.getInventory().placeItemBackInInventory(s);
            }
        }
    }

    private static RegistryOps<Tag> ops(ServerPlayer p) {
        return p.registryAccess().createSerializationContext(NbtOps.INSTANCE);
    }
}
