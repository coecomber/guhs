package nl.juiced.guhs.feature.huisje;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.Features;

/**
 * Where the chores' output goes (2.10): a Bank Guh within the home base sorts it (never loaned items), else a container
 * touching the huisje (within 2 blocks of any of its blocks), else a container within 4 blocks, else it pops out at the
 * door.
 */
public final class HuisjeOpslag {
    private HuisjeOpslag() {
    }

    /** Delivers chore output; returns what is left (normally empty: the rest pops out at the door). */
    public static ItemStack lever(ServerLevel level, Huisje h, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack rest = stack.copy();
        if (!Features.isLoaned(rest)) {
            BankGuhBlockEntity bank = bankGuh(level, h);
            if (bank != null) {
                bank.getStorage().insert(rest);
                return ItemStack.EMPTY;
            }
        }
        for (BlockPos pos : containers(level, h, 4)) {
            var resources = level.getCapability(Capabilities.Item.BLOCK, pos, null);   // (1.1.0: the transfer API, wrapped like 1.0.0)
            IItemHandler handler = resources == null ? null : IItemHandler.of(resources);
            if (handler != null) {
                rest = ItemHandlerHelper.insertItemStacked(handler, rest, false);
                if (rest.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        BlockPos d = h.deur();
        ItemEntity item = new ItemEntity(level, d.getX() + 0.5, d.getY() + 0.3, d.getZ() + 0.5, rest);
        item.setDeltaMovement(h.facing().getStepX() * 0.1, 0.15, h.facing().getStepZ() * 0.1);
        level.addFreshEntity(item);
        return ItemStack.EMPTY;
    }

    /** The huisje's chest: the nearest container touching it, else within 4 blocks, or null. */
    @Nullable
    public static Container kist(ServerLevel level, Huisje h) {
        for (BlockPos pos : containers(level, h, 4)) {
            if (level.getBlockEntity(pos) instanceof Container c) {
                return c;
            }
        }
        return null;
    }

    public static boolean heeftBankGuh(ServerLevel level, Huisje h) {
        return bankGuh(level, h) != null;
    }

    /** The nearest Bank Guh in the home base, or null. */
    @Nullable
    public static BankGuhBlockEntity bankGuh(ServerLevel level, Huisje h) {
        BankGuhBlockEntity best = null;
        double bestD = Double.MAX_VALUE;
        Vec3 m = h.midden();
        for (BlockEntity be : blockEntities(level, h.gebied())) {
            if (be instanceof BankGuhBlockEntity bank && h.inGebied(be.getBlockPos())) {
                double d = be.getBlockPos().distToCenterSqr(m);
                if (d < bestD) {
                    best = bank;
                    bestD = d;
                }
            }
        }
        return best;
    }

    /**
     * Containers (block entities with an item handler, not the Bank Guh) around the huisje: first the ones touching it
     * (within 2 blocks of any of its blocks), then the rest within max blocks; nearest to the door first.
     */
    static List<BlockPos> containers(ServerLevel level, Huisje h, int max) {
        List<BlockPos> blokken = h.blokken();
        AABB box = new AABB(blokken.get(0));
        for (BlockPos b : blokken) {
            box = box.minmax(new AABB(b));
        }
        List<BlockPos> dichtbij = new ArrayList<>(), verder = new ArrayList<>();
        for (BlockEntity be : blockEntities(level, box.inflate(max))) {
            BlockPos p = be.getBlockPos();
            if (be instanceof BankGuhBlockEntity || be instanceof HuisjeBlockEntity || blokken.contains(p)
                    || level.getCapability(Capabilities.Item.BLOCK, p, null) == null) {
                continue;
            }
            int afstand = afstand(blokken, p);
            if (afstand <= 2) {
                dichtbij.add(p);
            } else if (afstand <= max) {
                verder.add(p);
            }
        }
        BlockPos deur = h.deur();
        Comparator<BlockPos> opDeur = Comparator.comparingDouble(p -> p.distSqr(deur));
        dichtbij.sort(opDeur);
        verder.sort(opDeur);
        dichtbij.addAll(verder);
        return dichtbij;
    }

    /** Chebyshev distance from p to the nearest block of the huisje. */
    private static int afstand(List<BlockPos> blokken, BlockPos p) {
        int best = Integer.MAX_VALUE;
        for (BlockPos b : blokken) {
            int d = Math.max(Math.abs(b.getX() - p.getX()), Math.max(Math.abs(b.getY() - p.getY()), Math.abs(b.getZ() - p.getZ())));
            best = Math.min(best, d);
        }
        return best;
    }

    /** The loaded block entities in the chunks of this box, inside the box. */
    static List<BlockEntity> blockEntities(ServerLevel level, AABB box) {
        List<BlockEntity> out = new ArrayList<>();
        int x0 = ((int) Math.floor(box.minX)) >> 4, x1 = ((int) Math.floor(box.maxX)) >> 4;
        int z0 = ((int) Math.floor(box.minZ)) >> 4, z1 = ((int) Math.floor(box.maxZ)) >> 4;
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (box.contains(Vec3.atCenterOf(be.getBlockPos()))) {
                        out.add(be);
                    }
                }
            }
        }
        return out;
    }
}
