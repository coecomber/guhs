package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * T-OPSLAG: all virtual storage of one player, as ONE NeoForge attachment on the player ({@link OpslagRegistry#DATA}). It is
 * saved in player.dat together with the live inventory and kern's {@code SpelerData}, so moving an item between the inventory
 * and a bank/knapzak/materiaaltas is atomic: a crash keeps both sides or neither.
 * <ul>
 *   <li><b>Knapzak</b> (per character): {@link #knapzakken}, keyed by the character key ({@link Opslag#KARAKTER}; "1" until
 *   t-karakter's slots exist). Up to {@link Knapzak#MAX_TABS} tabs of {@link Knapzak#VAKKEN} slots, plus how they were unlocked.</li>
 *   <li><b>Bank</b> (account-wide): {@link #bank}, up to {@link Bank#MAX_TABS} tabs of {@link Bank#VAKKEN}; {@link #bankGekocht}.</li>
 *   <li><b>Materiaaltas</b> (account-wide): {@link #materialen}, one entry per kind (item + components, count 1) with a long count.</li>
 * </ul>
 * The containers are LIVE objects (an open menu edits them directly: no copy, so nothing can be duplicated between "the menu"
 * and "the save"). Items that cannot be read back (a removed mod) stay as raw tags in {@link #onleesbaar} and are written back
 * unchanged: nothing is ever thrown away.
 */
public final class OpslagData implements ValueIOSerializable {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Codec<List<CompoundTag>> TAGS = CompoundTag.CODEC.listOf();

    /** One character's knapzak: its tabs and what unlocked them. */
    public static final class Zak {
        public final List<Vak> tabs = new ArrayList<>();
        /** Extra unlock sources given through {@link Knapzak#ontgrendel} (quest/achievement ids). */
        public final Set<String> bronnen = new LinkedHashSet<>();
        /** Bought with Vadsmunten (T-OPSLAG: one tab; kept as "at least one" for old saves). */
        public boolean gekocht;
        /** U-OPSLAGVIP: how many tabs were bought (price curve OpslagBalans); read through {@link #gekochtAantal}. */
        public int gekochtN;

        /** U-OPSLAGVIP: tabs bought with coins (an old save's {@code gekocht} counts as 1). */
        public int gekochtAantal() {
            return Math.max(gekochtN, gekocht ? 1 : 0);
        }

        public Vak tab(int i) {
            while (tabs.size() <= i) {
                tabs.add(new Vak(Knapzak.VAKKEN));
            }
            return tabs.get(i);
        }
    }

    /** One material kind: the item (count 1) and how many. */
    public static final class Materiaal {
        public final ItemStack soort;
        public long aantal;

        public Materiaal(ItemStack soort, long aantal) {
            this.soort = soort.copyWithCount(1);
            this.aantal = aantal;
        }
    }

    public final Map<String, Zak> knapzakken = new LinkedHashMap<>();
    public final List<Vak> bank = new ArrayList<>();
    public int bankGekocht;
    /** U-OPSLAGVIP: the "WORD GUH VIP++" joke was shown (once per account, on the first try to buy an expansion). */
    public boolean vipGrapGezien;
    public final List<Materiaal> materialen = new ArrayList<>();
    /** The player switched off "materials go into the bag on pickup" (default: on). */
    public boolean autoUit;
    /** Unreadable item tags per place ("knapzak/<key>/<tab>", "bank/<tab>", "tas"), kept as they were. */
    public final Map<String, List<CompoundTag>> onleesbaar = new LinkedHashMap<>();
    /** Bumped on every change of the materiaaltas (the open screen re-syncs when it differs). */
    public transient int tasVersie;
    @Nullable
    private transient HolderLookup.Provider lookup;

    public Zak zak(String karakter) {
        return knapzakken.computeIfAbsent(karakter, k -> new Zak());
    }

    public Vak bankTab(int i) {
        while (bank.size() <= i) {
            bank.add(new Vak(Bank.VAKKEN));
        }
        return bank.get(i);
    }

    // ---- saving ------------------------------------------------------------------------------------------------------------

    @Nullable
    private HolderLookup.Provider lookup() {
        if (lookup == null && ServerLifecycleHooks.getCurrentServer() != null) {
            lookup = ServerLifecycleHooks.getCurrentServer().registryAccess();
        }
        return lookup;
    }

    @Override
    public void serialize(ValueOutput out) {
        HolderLookup.Provider l = lookup();
        if (l == null) {
            LOG.error("T-OPSLAG: no registry access while saving: storage NOT written this time");
            return;
        }
        RegistryOps<Tag> ops = l.createSerializationContext(NbtOps.INSTANCE);
        CompoundTag zakken = new CompoundTag();
        knapzakken.forEach((k, z) -> {
            CompoundTag t = new CompoundTag();
            for (int i = 0; i < z.tabs.size(); i++) {
                t.put("tab" + i, schrijf(ops, z.tabs.get(i), "knapzak/" + k + "/" + i));
            }
            t.putInt("tabs", z.tabs.size());
            t.putString("bronnen", String.join(",", z.bronnen));
            t.putBoolean("gekocht", z.gekocht);
            t.putInt("gekocht_n", z.gekochtAantal());   // U-OPSLAGVIP
            zakken.put(k, t);
        });
        out.store("knapzakken", CompoundTag.CODEC, zakken);
        CompoundTag b = new CompoundTag();
        for (int i = 0; i < bank.size(); i++) {
            b.put("tab" + i, schrijf(ops, bank.get(i), "bank/" + i));
        }
        b.putInt("tabs", bank.size());
        out.store("bank", CompoundTag.CODEC, b);
        out.putInt("bank_gekocht", bankGekocht);
        out.putBoolean("auto_uit", autoUit);
        out.putBoolean("vip_grap", vipGrapGezien);   // U-OPSLAGVIP
        List<CompoundTag> tas = new ArrayList<>();
        for (Materiaal m : materialen) {
            if (m.aantal <= 0) {
                continue;
            }
            Optional<Tag> t = ItemStack.CODEC.encodeStart(ops, m.soort).result();
            if (t.isPresent() && t.get() instanceof CompoundTag c) {
                CompoundTag e = new CompoundTag();
                e.put("item", c);
                e.putLong("n", m.aantal);
                tas.add(e);
            } else {
                LOG.error("T-OPSLAG: could not write material {} x{}", m.soort, m.aantal);
            }
        }
        tas.addAll(onleesbaar.getOrDefault("tas", List.of()));
        out.store("tas", TAGS, tas);
    }

    private net.minecraft.nbt.ListTag schrijf(RegistryOps<Tag> ops, Vak v, String plek) {
        List<CompoundTag> l = new ArrayList<>();
        for (int s = 0; s < v.getContainerSize(); s++) {
            ItemStack st = v.getItem(s);
            if (st.isEmpty()) {
                continue;
            }
            Optional<Tag> t = ItemStackWithSlot.CODEC.encodeStart(ops, new ItemStackWithSlot(s, st)).result();
            if (t.isPresent() && t.get() instanceof CompoundTag c) {
                l.add(c);
            } else {
                LOG.error("T-OPSLAG: could not write {} in {}", st, plek);
            }
        }
        l.addAll(onleesbaar.getOrDefault(plek, List.of()));
        net.minecraft.nbt.ListTag lt = new net.minecraft.nbt.ListTag();
        lt.addAll(l);
        return lt;
    }

    @Override
    public void deserialize(ValueInput in) {
        lookup = in.lookup();
        RegistryOps<Tag> ops = lookup.createSerializationContext(NbtOps.INSTANCE);
        knapzakken.clear();
        bank.clear();
        materialen.clear();
        onleesbaar.clear();
        CompoundTag zakken = in.read("knapzakken", CompoundTag.CODEC).orElse(new CompoundTag());
        for (String k : zakken.keySet()) {
            CompoundTag t = zakken.getCompoundOrEmpty(k);
            Zak z = zak(k);
            int n = t.getIntOr("tabs", 0);
            for (int i = 0; i < n; i++) {
                lees(ops, t, "tab" + i, z.tab(i), "knapzak/" + k + "/" + i);
            }
            String br = t.getStringOr("bronnen", "");
            if (!br.isEmpty()) {
                z.bronnen.addAll(List.of(br.split(",")));
            }
            z.gekocht = t.getBooleanOr("gekocht", false);
            z.gekochtN = t.getIntOr("gekocht_n", z.gekocht ? 1 : 0);   // U-OPSLAGVIP
        }
        CompoundTag b = in.read("bank", CompoundTag.CODEC).orElse(new CompoundTag());
        int n = b.getIntOr("tabs", 0);
        for (int i = 0; i < n; i++) {
            lees(ops, b, "tab" + i, bankTab(i), "bank/" + i);
        }
        bankGekocht = in.getIntOr("bank_gekocht", 0);
        autoUit = in.getBooleanOr("auto_uit", false);
        vipGrapGezien = in.getBooleanOr("vip_grap", false);   // U-OPSLAGVIP
        for (CompoundTag e : in.read("tas", TAGS).orElse(List.of())) {
            Optional<ItemStack> s = e.getCompound("item").flatMap(c -> ItemStack.CODEC.parse(ops, c).result());
            long aantal = e.getLongOr("n", 0);
            if (s.isPresent() && !s.get().isEmpty() && aantal > 0) {
                materialen.add(new Materiaal(s.get(), aantal));
            } else if (aantal > 0) {
                onleesbaar.computeIfAbsent("tas", x -> new ArrayList<>()).add(e);
            }
        }
        tasVersie++;
    }

    private void lees(RegistryOps<Tag> ops, CompoundTag ouder, String sleutel, Vak v, String plek) {
        List<CompoundTag> l = new ArrayList<>();
        ouder.getList(sleutel).ifPresent(lt -> lt.forEach(t -> {
            if (t instanceof CompoundTag c) {
                l.add(c);
            }
        }));
        for (CompoundTag c : l) {
            Optional<ItemStackWithSlot> s = ItemStackWithSlot.CODEC.parse(ops, c).result();
            if (s.isPresent() && !s.get().stack().isEmpty() && s.get().slot() >= 0 && s.get().slot() < v.getContainerSize()
                    && v.getItem(s.get().slot()).isEmpty()) {
                v.setItem(s.get().slot(), s.get().stack());
            } else {
                onleesbaar.computeIfAbsent(plek, x -> new ArrayList<>()).add(c);
            }
        }
    }
}
