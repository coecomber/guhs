package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Inventaris;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/** Small shared helpers: messages, item <-> tag, inventory space. */
public final class EcoUtil {
    /** The main inventory (hotbar + bag): the slots trade, market and selling work with. */
    public static final int TAS = 36;

    private EcoUtil() {
    }

    public static void zeg(ServerPlayer p, String key, Object... args) {
        p.sendOverlayMessage(Component.translatable(key, args).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public static void chat(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public static void geluid(ServerPlayer p, SoundEvent s, float volume, float pitch) {
        p.level().playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, volume, pitch);
    }

    /** An item stack as a tag (null when it can't be written). */
    @Nullable
    public static CompoundTag tag(HolderLookup.Provider reg, ItemStack s) {
        Optional<Tag> t = ItemStack.CODEC.encodeStart(reg.createSerializationContext(NbtOps.INSTANCE), s).result();
        return t.isPresent() && t.get() instanceof CompoundTag c ? c : null;
    }

    /** A tag back to an item stack (EMPTY when unreadable). */
    public static ItemStack stack(HolderLookup.Provider reg, CompoundTag t) {
        return ItemStack.CODEC.parse(reg.createSerializationContext(NbtOps.INSTANCE), t).result().orElse(ItemStack.EMPTY);
    }

    public static int vrij(Inventory inv) {
        int n = 0;
        for (int i = 0; i < TAS; i++) {
            if (inv.getItem(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** Puts a stack in the first empty bag slot; false (nothing changed) when there is none. */
    public static boolean inLegeSlot(Inventory inv, ItemStack s) {
        for (int i = 0; i < TAS; i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, s);
                return true;
            }
        }
        return false;
    }

    /**
     * Gives the stack; whatever doesn't fit goes to kern's found items of the world the player is in now (handed out by
     * {@code /guhs vadsrijk kist}). Adapter: kern has no public "bewaar" (NOTITIES_ECONOMIE), so this writes the same tag kern writes.
     */
    public static void geefOfBewaar(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) {
            return;
        }
        p.getInventory().add(s);
        if (s.isEmpty()) {
            return;
        }
        Inventaris.bewaar(p, s);   // kern (integration 1.10.0; was an own copy of the encoding)
        zeg(p, "vadsrijk.economie.tas_vol_kist");
    }

    @Nullable
    public static Uitrusting uitrusting(ItemStack s) {
        return Rollen.van(s);
    }

    /** Is this stack something a player may trade / put on the market (not gebonden, not empty)? */
    public static boolean verhandelbaar(ItemStack s) {
        if (s.isEmpty()) {
            return false;
        }
        Uitrusting u = Rollen.van(s);
        return u == null || !u.gebonden();
    }

    /** Counts this item in the bag. */
    public static int tel(Inventory inv, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, n + nl.juiced.guhs.feature.vadsrijk.opslag.Opslag.tasVoor(inv, item));   // T-OPSLAG: + the materiaaltas
    }

    /** Takes n of this item (only call after {@link #tel} said there are enough). */
    public static void neem(Inventory inv, net.minecraft.world.item.Item item, int n) {
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int k = Math.min(n, s.getCount());
                s.shrink(k);
                n -= k;
            }
        }
        inv.setChanged();
        nl.juiced.guhs.feature.vadsrijk.opslag.Opslag.neemRestUitTas(inv, item, n);   // T-OPSLAG: the rest from the materiaaltas
    }
}
