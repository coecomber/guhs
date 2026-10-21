package nl.juiced.guhs.feature.bouwhulp.kern;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One building action that can be undone (design 2.1: "de laatste 5 acties"): every block it changed, what stood there
 * before and whether the new block was paid for. The blocks that were in the way are KEPT in the action (not handed out)
 * while it can still be undone, so undoing puts them back without anything being doubled; when the action falls off the
 * end of the list (the sixth action) its kept blocks go to the player ({@link #bewaard}).
 */
public final class BouwActie {
    /** One changed block. {@code betaald}: the new block cost items (not in creative, not a free half). */
    public record Wijziging(BlockPos pos, BlockState oud, BlockState nieuw, boolean betaald) {
    }

    final UUID id;
    final UUID speler;
    final UUID spook;
    final List<Wijziging> wijzigingen = new ArrayList<>();

    BouwActie(UUID id, UUID speler, UUID spook) {
        this.id = id;
        this.speler = speler;
        this.spook = spook;
    }

    public UUID speler() {
        return speler;
    }

    public UUID spook() {
        return spook;
    }

    public List<Wijziging> wijzigingen() {
        return wijzigingen;
    }

    void voegToe(BlockPos pos, BlockState oud, BlockState nieuw, boolean betaald) {
        wijzigingen.add(new Wijziging(pos.immutable(), oud, nieuw, betaald));
    }

    /** The items of the blocks that were in the way (kept while the action can be undone). */
    public Map<Item, Integer> bewaard() {
        Map<Item, Integer> uit = new LinkedHashMap<>();
        for (Wijziging w : wijzigingen) {
            Materiaal.Kost k = Materiaal.kost(w.oud());
            if (k != null && k.aantal() > 0) {
                uit.merge(k.item(), k.aantal(), Integer::sum);
            }
        }
        return uit;
    }

    static List<ItemStack> stapels(Map<Item, Integer> items) {
        List<ItemStack> uit = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : items.entrySet()) {
            int n = e.getValue();
            while (n > 0) {
                int k = Math.min(n, e.getKey().getDefaultMaxStackSize());
                uit.add(new ItemStack(e.getKey(), k));
                n -= k;
            }
        }
        return uit;
    }

    // --- saving (a palette of states; per change: pos, old index, new index, paid) ---------------------------------------

    CompoundTag opslaan() {
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, id);
        t.store("Speler", UUIDUtil.CODEC, speler);
        t.store("Spook", UUIDUtil.CODEC, spook);
        Map<BlockState, Integer> index = new LinkedHashMap<>();
        long[] plekken = new long[wijzigingen.size()];
        int[] staten = new int[wijzigingen.size() * 3];
        for (int i = 0; i < wijzigingen.size(); i++) {
            Wijziging w = wijzigingen.get(i);
            plekken[i] = w.pos().asLong();
            staten[i * 3] = index.computeIfAbsent(w.oud(), s -> index.size());
            staten[i * 3 + 1] = index.computeIfAbsent(w.nieuw(), s -> index.size());
            staten[i * 3 + 2] = w.betaald() ? 1 : 0;
        }
        ListTag palet = new ListTag();
        index.keySet().forEach(s -> palet.add(NbtUtils.writeBlockState(s)));
        t.put("Palet", palet);
        t.put("Plekken", new LongArrayTag(plekken));
        t.put("Staten", new IntArrayTag(staten));
        return t;
    }

    static BouwActie laad(CompoundTag t) {
        BouwActie a = new BouwActie(t.read("Id", UUIDUtil.CODEC).orElse(UUID.randomUUID()), t.read("Speler", UUIDUtil.CODEC).orElse(new UUID(0, 0)),
                t.read("Spook", UUIDUtil.CODEC).orElse(new UUID(0, 0)));
        ListTag palet = t.getListOrEmpty("Palet");
        List<BlockState> staten = new ArrayList<>();
        for (int i = 0; i < palet.size(); i++) {
            staten.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK, palet.getCompoundOrEmpty(i)));
        }
        long[] plekken = t.getLongArray("Plekken").orElse(new long[0]);
        int[] s = t.getIntArray("Staten").orElse(new int[0]);
        for (int i = 0; i < plekken.length && i * 3 + 2 < s.length; i++) {
            if (s[i * 3] < staten.size() && s[i * 3 + 1] < staten.size()) {
                a.voegToe(BlockPos.of(plekken[i]), staten.get(s[i * 3]), staten.get(s[i * 3 + 1]), s[i * 3 + 2] == 1);
            }
        }
        return a;
    }
}
