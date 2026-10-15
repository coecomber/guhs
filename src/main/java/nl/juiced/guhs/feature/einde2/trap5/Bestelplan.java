package nl.juiced.guhs.feature.einde2.trap5;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import nl.juiced.guhs.feature.techmachine.Bouwtekening;
import nl.juiced.guhs.feature.techmachine.KnutselmachineBlockEntity;
import nl.juiced.guhs.storage.BankStorage;

/**
 * What a Bestelguh can make and how: the Knutselmachines within {@link T5Getallen#BALIE_BEREIK} blocks of a Bestelbalie,
 * each with its Bouwtekening ({@link #recepten}), and the plan of one order ({@link #maak}): a list of {@link Taak}s in the
 * order they must run, intermediate products first (DESIGN 6: "ook tussenproducten in meerdere stappen"). An intermediate
 * product the bank already holds is taken from the bank; what is not in the bank and has a machine is made first; what is
 * neither makes the plan fail with that item. No stock rules: one order, once.
 */
public final class Bestelplan {
    /** One step of an order: this machine crafts {@code keer} times (each craft gives {@code resultaat}). */
    public record Taak(BlockPos machine, ItemStack resultaat, int keer) {
        public static final Codec<Taak> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("machine").forGetter(Taak::machine),
                ItemStack.CODEC.fieldOf("resultaat").forGetter(Taak::resultaat),
                Codec.INT.fieldOf("keer").forGetter(Taak::keer)).apply(i, Taak::new));
    }

    /** A machine and what it makes. */
    public record Recept(BlockPos machine, Bouwtekening tekening) {
    }

    /** The plan, or why there is none: {@code mist} = the item that is not in the bank and has no machine (or too many crafts). */
    public record Uitkomst(List<Taak> taken, @Nullable ItemStack mist, boolean teVeel) {
        public boolean gelukt() {
            return mist == null && !teVeel && !taken.isEmpty();
        }
    }

    private Bestelplan() {
    }

    /** Every Knutselmachine with a drawing within range of this spot (nearest first). */
    public static List<Recept> recepten(ServerLevel level, BlockPos balie) {
        List<Recept> uit = new ArrayList<>();
        int r = T5Getallen.BALIE_BEREIK;
        ChunkPos min = ChunkPos.containing(balie.offset(-r, 0, -r)), max = ChunkPos.containing(balie.offset(r, 0, r));
        for (int cx = min.x(); cx <= max.x(); cx++) {
            for (int cz = min.z(); cz <= max.z(); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof KnutselmachineBlockEntity km && !be.isRemoved() && km.tekening() != null
                            && be.getBlockPos().distManhattan(balie) <= r * 3 && Math.abs(be.getBlockPos().getX() - balie.getX()) <= r
                            && Math.abs(be.getBlockPos().getY() - balie.getY()) <= r && Math.abs(be.getBlockPos().getZ() - balie.getZ()) <= r) {
                        uit.add(new Recept(be.getBlockPos(), km.tekening()));
                    }
                }
            }
        }
        uit.sort((a, b) -> Double.compare(a.machine().distSqr(balie), b.machine().distSqr(balie)));
        return uit;
    }

    /** The first machine that makes this item (item + components), or null. */
    @Nullable
    public static Recept receptVoor(List<Recept> recepten, ItemStack wat) {
        for (Recept r : recepten) {
            if (ItemStack.isSameItemSameComponents(r.tekening().resultaat(), wat)) {
                return r;
            }
        }
        return null;
    }

    /** What these machines can make, one stack per different result (the drawing's own count). */
    public static List<ItemStack> kan(List<Recept> recepten) {
        List<ItemStack> uit = new ArrayList<>();
        for (Recept r : recepten) {
            ItemStack res = r.tekening().resultaat();
            if (uit.stream().noneMatch(s -> ItemStack.isSameItemSameComponents(s, res))) {
                uit.add(res.copy());
            }
        }
        return uit;
    }

    /** The plan to MAKE {@code aantal} of {@code wat} (the target itself is always made, never taken from the bank). */
    public static Uitkomst maak(List<Recept> recepten, BankStorage bank, ItemStack wat, int aantal) {
        Planner p = new Planner(recepten, bank);
        boolean ok = p.maak(wat, Math.max(1, aantal), 0, new HashSet<>());
        return new Uitkomst(List.copyOf(p.taken), ok ? null : p.mist, p.teVeel);
    }

    private static final class Planner {
        private final List<Recept> recepten;
        private final BankStorage bank;
        private final List<Taak> taken = new ArrayList<>();
        private final List<ItemStack> gereserveerd = new ArrayList<>();
        private int keerTotaal;
        @Nullable
        private ItemStack mist;
        private boolean teVeel;

        Planner(List<Recept> recepten, BankStorage bank) {
            this.recepten = recepten;
            this.bank = bank;
        }

        private long gereserveerd(ItemStack wat) {
            for (ItemStack s : gereserveerd) {
                if (ItemStack.isSameItemSameComponents(s, wat)) {
                    return s.getCount();
                }
            }
            return 0;
        }

        private void reserveer(ItemStack wat, long n) {
            if (n <= 0) {
                return;
            }
            for (ItemStack s : gereserveerd) {
                if (ItemStack.isSameItemSameComponents(s, wat)) {
                    s.setCount((int) Math.min(Integer.MAX_VALUE, s.getCount() + n));
                    return;
                }
            }
            gereserveerd.add(wat.copyWithCount((int) Math.min(Integer.MAX_VALUE, n)));
        }

        boolean maak(ItemStack wat, int aantal, int diepte, Set<String> bezig) {
            Recept r = receptVoor(recepten, wat);
            String sleutel = wat.getItem().toString();
            if (r == null || diepte > 8 || bezig.contains(sleutel)) {
                mist = wat.copyWithCount(aantal);
                return false;
            }
            int per = Math.max(1, r.tekening().resultaat().getCount());
            int keer = (aantal + per - 1) / per;
            keerTotaal += keer;
            if (keerTotaal > T5Getallen.BALIE_MAX_KEER) {
                teVeel = true;
                return false;
            }
            Set<String> verder = new HashSet<>(bezig);
            verder.add(sleutel);
            for (ItemStack ing : r.tekening().ingredienten()) {
                long nodig = (long) ing.getCount() * keer;
                long heeft = Math.max(0, bank.count(ing) - gereserveerd(ing));
                long neem = Math.min(heeft, nodig);
                reserveer(ing, neem);
                long rest = nodig - neem;
                if (rest > 0 && !maak(ing, (int) Math.min(Integer.MAX_VALUE, rest), diepte + 1, verder)) {
                    return false;
                }
            }
            taken.add(new Taak(r.machine(), r.tekening().resultaat().copy(), keer));
            return true;
        }
    }
}
