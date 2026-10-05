package nl.juiced.guhs.feature.klusjes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.guhpolder.MolentjeBlockEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeBlockEntity;
import nl.juiced.guhs.feature.huisje.HuisjeOpslag;
import nl.juiced.guhs.storage.BankContents;

/**
 * The huisje's stock for chores that need something (a bottle for kaasmelk, knabbelvoer, graan and ingredients for
 * baking, a snack for a hurt guh): taken from the chests around the huisje (the same ones {@link HuisjeOpslag} fills:
 * within 4 blocks) and from the Bank Guh of the home base. Output never goes through here: that is
 * {@link HuisjeOpslag#lever}.
 */
public final class Voorraad {
    /**
     * 1.1.0: the block's item handler (NeoForge 26.1 has {@code Capabilities.Item.BLOCK}, a ResourceHandler; wrapped in the old
     * slot-style IItemHandler so the chest code stays as it was). Null: not a container.
     */
    @SuppressWarnings("removal")
    @Nullable
    static IItemHandler handler(ServerLevel level, BlockPos pos) {
        var resources = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        return resources == null ? null : IItemHandler.of(resources);
    }

    private Voorraad() {
    }

    /** The containers around the huisje (nearest to the door first), not the Bank Guh (nor a guh-molentje). */
    public static List<BlockPos> kisten(ServerLevel level, Huisje h) {
        List<BlockPos> blokken = h.blokken();
        BlockPos deur = h.deur();
        List<BlockPos> uit = new ArrayList<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE,
                maxZ = Integer.MIN_VALUE;
        for (BlockPos b : blokken) {
            minX = Math.min(minX, b.getX());
            minY = Math.min(minY, b.getY());
            minZ = Math.min(minZ, b.getZ());
            maxX = Math.max(maxX, b.getX());
            maxY = Math.max(maxY, b.getY());
            maxZ = Math.max(maxZ, b.getZ());
        }
        for (int cx = (minX - 4) >> 4; cx <= (maxX + 4) >> 4; cx++) {
            for (int cz = (minZ - 4) >> 4; cz <= (maxZ + 4) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (be instanceof BankGuhBlockEntity || be instanceof HuisjeBlockEntity || be instanceof MolentjeBlockEntity || blokken.contains(p)
                            || p.getX() < minX - 4 || p.getX() > maxX + 4 || p.getY() < minY - 4 || p.getY() > maxY + 4
                            || p.getZ() < minZ - 4 || p.getZ() > maxZ + 4
                            || Voorraad.handler(level, p) == null) {
                        continue;
                    }
                    uit.add(p);
                }
            }
        }
        uit.sort(Comparator.comparingDouble(p -> p.distSqr(deur)));
        return uit;
    }

    /** Is there anywhere to put things (a chest or a Bank Guh)? Without it, things would only pile up at the door. */
    public static boolean heeftOpslag(ServerLevel level, Huisje h) {
        return HuisjeOpslag.heeftBankGuh(level, h) || !kisten(level, h).isEmpty();
    }

    /**
     * Would this stack fit? bbq2: a Bank Guh only has room up to its cap (256 of one kind, unless it is upgraded), so the
     * part the Bank Guhs of the home base do not take must fit in the chests; never a loaned thing in a bank.
     */
    public static boolean past(ServerLevel level, Huisje h, ItemStack stack) {
        long inBank = HuisjeOpslag.bankRuimte(level, h, stack);
        if (inBank >= stack.getCount()) {
            return true;
        }
        ItemStack rest = stack.copyWithCount(stack.getCount() - (int) inBank);
        for (BlockPos p : kisten(level, h)) {
            IItemHandler handler = Voorraad.handler(level, p);
            if (handler != null) {
                rest = ItemHandlerHelper.insertItemStacked(handler, rest, true);
                if (rest.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** How many items matching this the huisje has (chests + Bank Guh). */
    public static long tel(ServerLevel level, Huisje h, Predicate<ItemStack> wat) {
        long n = 0;
        for (BlockPos p : kisten(level, h)) {
            IItemHandler handler = Voorraad.handler(level, p);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack s = handler.getStackInSlot(i);
                    if (!s.isEmpty() && wat.test(s)) {
                        n += s.getCount();
                    }
                }
            }
        }
        BankGuhBlockEntity bank = HuisjeOpslag.bankGuh(level, h);
        if (bank != null) {
            for (BankContents.Entry e : bank.getStorage().snapshot().entries()) {
                if (wat.test(e.item())) {
                    n += e.count();
                }
            }
        }
        return n;
    }

    /** Takes up to max items of ONE kind matching this (chests first, then the Bank Guh); EMPTY when there is none. */
    public static ItemStack neem(ServerLevel level, Huisje h, Predicate<ItemStack> wat, int max) {
        for (BlockPos p : kisten(level, h)) {
            IItemHandler handler = Voorraad.handler(level, p);
            if (handler == null) {
                continue;
            }
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack s = handler.getStackInSlot(i);
                if (!s.isEmpty() && wat.test(s)) {
                    ItemStack uit = handler.extractItem(i, Math.min(max, s.getCount()), false);
                    if (!uit.isEmpty()) {
                        int nog = max - uit.getCount();
                        for (int j = i + 1; j < handler.getSlots() && nog > 0; j++) {
                            ItemStack t = handler.getStackInSlot(j);
                            if (ItemStack.isSameItemSameComponents(t, uit)) {
                                ItemStack meer = handler.extractItem(j, nog, false);
                                uit.grow(meer.getCount());
                                nog -= meer.getCount();
                            }
                        }
                        return uit;
                    }
                }
            }
        }
        BankGuhBlockEntity bank = HuisjeOpslag.bankGuh(level, h);
        if (bank != null) {
            for (BankContents.Entry e : bank.getStorage().snapshot().entries()) {
                if (wat.test(e.item())) {
                    ItemStack uit = bank.getStorage().extract(e.item(), Math.min(max, e.item().getMaxStackSize()));
                    if (!uit.isEmpty()) {
                        return uit;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /** Where a guh brings its spoils: the Bank Guh (it sorts), else the huisje's chest, else the door. */
    public static BlockPos brengPlek(ServerLevel level, Huisje h) {
        BankGuhBlockEntity bank = HuisjeOpslag.bankGuh(level, h);
        if (bank != null) {
            return bank.getBlockPos();
        }
        Container kist = HuisjeOpslag.kist(level, h);
        if (kist instanceof BlockEntity be) {
            return be.getBlockPos();
        }
        List<BlockPos> kisten = kisten(level, h);
        return kisten.isEmpty() ? h.deur() : kisten.get(0);
    }

    /** The chest itself (for sorting it into the Bank Guh), or null. */
    @Nullable
    public static BlockPos eersteKist(ServerLevel level, Huisje h) {
        List<BlockPos> kisten = kisten(level, h);
        return kisten.isEmpty() ? null : kisten.get(0);
    }

    public static Vec3 midden(BlockPos p) {
        return Vec3.atCenterOf(p);
    }
}
