package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.feature.vadsrijk.kern.compat.ExtraVakken;

/**
 * The builder's survival things while they build in creative (design R5: "inventaris wisselen bij het betreden en verlaten,
 * niets meenemen naar buiten"). Entering your own plot stashes everything: the inventory (all slots: 36 + armour + offhand
 * + body + saddle), the Curios/Accessories slots, the effects, health, hunger, XP and the game mode; the creative inventory
 * starts empty. Leaving the plot, logging out, travelling to another dimension, dying, the deadline, the plaza being
 * stamped: everything comes back exactly, and whatever the builder had in creative is gone.
 * <p><b>Crash-safe:</b> the stash is written into the player's own save data ({@code getPersistentData()}, saved in the same
 * player.dat as the live inventory), BEFORE anything is cleared. So the file always holds either the survival inventory
 * without a stash, or the creative inventory plus the stash: a crash at any moment can't lose or double anything. At the
 * next login a left-over stash is put back at once. {@link LootjesData} keeps a backup copy (SavedData) for an op.
 */
public final class Bouwkist {
    static final String SLEUTEL = "guhs_guhwinter_bouwkist";
    private static final String EXTRA = "lt_extra", EXTRA_I = "lt_extra_i", ONLEESBAAR = "guhs_guhwinter_onleesbaar";
    private static final int EXTRA_SLOT = 250;
    /** While we switch the game mode ourselves (the game-mode guard lets that through). */
    static boolean bezig;

    private Bouwkist() {
    }

    /** Is this player building in creative right now (has a stash)? */
    public static boolean bouwt(ServerPlayer p) {
        return p.getPersistentData().contains(SLEUTEL);
    }

    /** The plot this player builds on (-1: not building). */
    public static int veld(ServerPlayer p) {
        return p.getPersistentData().getCompoundOrEmpty(SLEUTEL).getIntOr("Veld", -1);
    }

    /** Stash everything and switch to creative (empty) for this plot. No-op when already building. */
    public static void begin(ServerPlayer p, int veld) {
        if (bouwt(p)) {
            return;
        }
        handenLeeg(p, true);
        CompoundTag k = pak(p);
        k.putInt("Veld", veld);
        p.getPersistentData().put(SLEUTEL, k);   // first the stash...
        LootjesData.get(p.level().getServer()).backup(p.getUUID(), k);
        leeg(p);                                   // ...then the clearing
        bezig = true;
        try {
            p.setGameMode(GameType.CREATIVE);
        } finally {
            bezig = false;
        }
        sync(p);
    }

    /** Everything back as it was (creative things gone). False when the player wasn't building. */
    public static boolean herstel(ServerPlayer p) {
        Optional<CompoundTag> k = p.getPersistentData().getCompound(SLEUTEL);
        if (k.isEmpty()) {
            return false;
        }
        handenLeeg(p, false);
        bezig = true;
        try {
            zetTerug(p, k.get());
        } finally {
            bezig = false;
        }
        p.getPersistentData().remove(SLEUTEL);
        LootjesData.get(p.level().getServer()).backup(p.getUUID(), null);
        sync(p);
        return true;
    }

    // --- the stash itself (also used by the gametest) ----------------------------------------------------------------------

    /** A snapshot of everything we put back later (nothing is changed). */
    public static CompoundTag pak(ServerPlayer p) {
        CompoundTag k = new CompoundTag();
        RegistryOps<Tag> ops = ops(p);
        Inventory inv = p.getInventory();
        ListTag items = new ListTag();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack s = inv.getItem(slot);
            if (!s.isEmpty()) {
                ItemStackWithSlot.CODEC.encodeStart(ops, new ItemStackWithSlot(slot, s.copy())).result().ifPresent(items::add);
            }
        }
        k.put("Items", items);
        ListTag extra = new ListTag();
        for (ExtraVakken.Bron bron : ExtraVakken.ALLE) {
            List<ExtraVakken.Vak> vakken = bron.vakken(p);
            for (int i = 0; i < vakken.size(); i++) {
                ItemStack s = vakken.get(i).get();
                if (s != null && !s.isEmpty()) {
                    int n = i;
                    ItemStackWithSlot.CODEC.encodeStart(ops, new ItemStackWithSlot(EXTRA_SLOT, s.copy())).result().ifPresent(t -> {
                        if (t instanceof CompoundTag c) {
                            c.putString(EXTRA, bron.id());
                            c.putInt(EXTRA_I, n);
                            extra.add(c);
                        }
                    });
                }
            }
        }
        k.put("Extra", extra);
        k.putInt("Sel", inv.getSelectedSlot());
        ListTag eff = new ListTag();
        for (MobEffectInstance e : p.getActiveEffects()) {
            MobEffectInstance.CODEC.encodeStart(ops, e).result().ifPresent(eff::add);
        }
        k.put("Effecten", eff);
        k.putFloat("Health", p.getHealth());
        k.putInt("Food", p.getFoodData().getFoodLevel());
        k.putFloat("Sat", p.getFoodData().getSaturationLevel());
        k.putInt("XpL", p.experienceLevel);
        k.putFloat("XpP", p.experienceProgress);
        k.putInt("XpT", p.totalExperience);
        k.putString("Mode", p.gameMode.getGameModeForPlayer().getName());
        k.putInt("Lucht", p.getAirSupply());
        return k;
    }

    /** Clears what the creative builder starts without: the inventory, the accessory slots, the effects. */
    public static void leeg(ServerPlayer p) {
        p.getInventory().clearContent();
        for (ExtraVakken.Bron bron : ExtraVakken.ALLE) {
            for (ExtraVakken.Vak v : bron.vakken(p)) {
                try {
                    ItemStack s = v.get();
                    if (s != null && !s.isEmpty()) {
                        v.set(ItemStack.EMPTY);
                    }
                } catch (RuntimeException ignored) {
                    // (a slot that refuses: nothing to do)
                }
            }
        }
        p.removeAllEffects();
    }

    /** Puts a snapshot back: first everything cleared (the creative things go), then the snapshot. */
    public static void zetTerug(ServerPlayer p, CompoundTag k) {
        leeg(p);
        RegistryOps<Tag> ops = ops(p);
        Inventory inv = p.getInventory();
        List<ItemStack> later = new ArrayList<>();
        List<CompoundTag> kapot = new ArrayList<>();
        for (Tag t : k.getListOrEmpty("Items")) {
            Optional<ItemStackWithSlot> s = ItemStackWithSlot.CODEC.parse(ops, t).result();
            if (s.isEmpty() || s.get().stack().isEmpty()) {
                if (t instanceof CompoundTag c) {
                    kapot.add(c);
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
        for (Tag t : k.getListOrEmpty("Extra")) {
            if (!(t instanceof CompoundTag c)) {
                continue;
            }
            Optional<ItemStackWithSlot> s = ItemStackWithSlot.CODEC.parse(ops, c).result();
            if (s.isEmpty() || s.get().stack().isEmpty()) {
                kapot.add(c);
                continue;
            }
            ExtraVakken.Bron bron = ExtraVakken.van(c.getStringOr(EXTRA, ""));
            List<ExtraVakken.Vak> vakken = bron == null ? List.of() : bron.vakken(p);
            int i = c.getIntOr(EXTRA_I, -1);
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
        }
        for (ItemStack s : later) {
            inv.add(s);
            if (!s.isEmpty()) {
                p.drop(s, false);   // (can't happen: the inventory was empty and has the same size; never lost though)
            }
        }
        if (!kapot.isEmpty()) {
            // an item that can't be read back (a mod gone?): its tag stays with the player instead of being lost
            ListTag l = p.getPersistentData().getListOrEmpty(ONLEESBAAR).copy();
            kapot.forEach(l::add);
            p.getPersistentData().put(ONLEESBAAR, l);
        }
        inv.setSelectedSlot(Math.max(0, Math.min(8, k.getIntOr("Sel", 0))));
        for (Tag t : k.getListOrEmpty("Effecten")) {
            MobEffectInstance.CODEC.parse(ops, t).result().ifPresent(p::addEffect);
        }
        p.setGameMode(GameType.byName(k.getStringOr("Mode", "survival"), GameType.SURVIVAL));
        float hp = k.getFloatOr("Health", p.getMaxHealth());
        p.setHealth(Math.max(1f, Math.min(p.getMaxHealth(), hp)));
        p.getFoodData().setFoodLevel(k.getIntOr("Food", 20));
        p.getFoodData().setSaturation(k.getFloatOr("Sat", 5f));
        p.experienceProgress = k.getFloatOr("XpP", 0f);
        p.totalExperience = k.getIntOr("XpT", 0);
        p.setExperienceLevels(k.getIntOr("XpL", 0));
        p.setAirSupply(k.getIntOr("Lucht", p.getMaxAirSupply()));
        p.clearFire();
        p.fallDistance = 0;
        inv.setChanged();
    }

    /** The cursor item and the 2x2 crafting grid: back into the inventory ({@code bewaren}), or gone (creative things). */
    private static void handenLeeg(ServerPlayer p, boolean bewaren) {
        if (p.containerMenu != p.inventoryMenu) {
            p.closeContainer();
        }
        ItemStack cursor = p.inventoryMenu.getCarried();
        if (!cursor.isEmpty()) {
            p.inventoryMenu.setCarried(ItemStack.EMPTY);
            if (bewaren) {
                p.getInventory().placeItemBackInInventory(cursor);
            }
        }
        var grid = p.inventoryMenu.getCraftSlots();
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack s = grid.removeItemNoUpdate(i);
            if (!s.isEmpty() && bewaren) {
                p.getInventory().placeItemBackInInventory(s);
            }
        }
    }

    private static void sync(ServerPlayer p) {
        p.inventoryMenu.broadcastFullState();
        if (p.containerMenu != p.inventoryMenu) {
            p.containerMenu.broadcastFullState();
        }
    }

    @Nullable
    static GameType modus(CompoundTag k) {
        return GameType.byName(k.getStringOr("Mode", "survival"), null);
    }

    private static RegistryOps<Tag> ops(ServerPlayer p) {
        return p.registryAccess().createSerializationContext(NbtOps.INSTANCE);
    }
}
