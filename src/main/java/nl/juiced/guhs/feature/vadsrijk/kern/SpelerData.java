package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Everything Het Vadsrijk keeps per player: a NeoForge attachment on the player ({@link KernRegistry#SPELER}), saved in
 * player.dat together with the player's own inventory (so the inventory swap is atomic) and copied on death.
 * Read it with {@link Spelers#data}; change it, then {@link Spelers#sync} (HUD) if the HUD shows it.
 * <p>The two inventories are kept as raw item tags ({@link Inventaris}): an item that can't be read any more (a removed mod)
 * stays as a tag and is never thrown away.
 */
public final class SpelerData implements ValueIOSerializable {
    private static final Codec<List<CompoundTag>> TAGS = CompoundTag.CODEC.listOf();

    /** Is the Vadsrijk inventory the live one (= the normal inventory is in {@link #opgeborgen})? */
    public boolean binnen;
    /** The normal inventory while inside; the Vadsrijk inventory while outside; items the swap couldn't place. */
    public final List<CompoundTag> opgeborgen = new ArrayList<>(), rijkInventaris = new ArrayList<>(), gevonden = new ArrayList<>();
    public int opgeborgenSlot, rijkSlot;
    /** Where the player was before entering (dim id, x, y, z, yaw, pitch); null = their respawn point. */
    @Nullable
    public CompoundTag terug;

    /**
     * The ACTIVE class (g-klassen: you are always all 3 classes; this is the class of the weapon you last attacked with, see
     * {@code Spelers.voorAanval}). Without a class weapon in the hand the HUD shows it greyed.
     */
    public String klasse = "knuffelridder";
    /** Total xp per class (the class level is derived: {@link Levels#levelVan}). */
    public final Map<String, Long> xp = new HashMap<>();
    /** g-klassen: total generic xp (every xp gained also goes here; level: {@link Levels#generiekVan}). */
    public long generiekXp;
    /** Not saved: set when this data was migrated from a save without generic xp (for the log/tests). */
    public transient boolean gemigreerd;
    /** Runtime values; -1 = full (first time). */
    public double vads = -1, energie = -1, ultLading;
    /** Vadsmunten, in brons (100 brons = 1 zilver, 100 zilver = 1 goud). */
    public long munten;
    @Nullable
    public BlockPos checkpoint;
    /** Game time until which the player is Duizelig (after fainting). */
    public long duizeligTot;
    public int opgevrolijkt;
    public boolean introGezien;
    public final Set<String> tips = new LinkedHashSet<>(), gezien = new LinkedHashSet<>();
    /** Skill tree nodes per class. */
    public final Map<String, Set<String>> boom = new HashMap<>();
    /** Real time (millis) after which a free respec is possible again. */
    public long respecKlaar;
    /** Worn jewellery: slot name (RING/KETTING/OORBEL) -> raw item tag ({@link Sieraden}). */
    public final Map<String, CompoundTag> sieraden = new HashMap<>();
    /** Free storage per slice ("npc" = quests, "economie" = mounts/market...). */
    public final Map<String, CompoundTag> extra = new HashMap<>();

    /** Not saved: cooldowns (skill id -> game time it is ready), the last area. */
    public final transient Map<String, Long> klaarOp = new HashMap<>();
    /** Decoded {@link #sieraden} (filled by {@link Sieraden#get}). */
    public final transient Map<String, net.minecraft.world.item.ItemStack> sieradenCache = new HashMap<>();
    @Nullable
    public transient String gebied;

    public CompoundTag extra(String slice) {
        return extra.computeIfAbsent(slice, k -> new CompoundTag());
    }

    public long xp(String klasse) {
        return xp.getOrDefault(klasse, 0L);
    }

    public int level(String klasse) {
        return Levels.levelVan(xp(klasse));
    }

    /** The GENERIC level (g-klassen; before: the current class's level). */
    public int level() {
        return Levels.generiekVan(generiekXp);
    }

    /** The level of the active class. */
    public int klasseLevel() {
        return level(klasse);
    }

    /**
     * g-klassen save migration: a save from before the two levels only has xp per class. The generic xp becomes the HIGHEST
     * class xp (not the sum: someone who levelled two classes to 15 should not jump to generic 22), capped at the generic max.
     */
    public static long migreerGeneriek(Map<String, Long> perKlasse) {
        long m = 0;
        for (long v : perKlasse.values()) {
            m = Math.max(m, v);
        }
        return Math.min(m, Levels.totaalVoorGeneriek(Levels.MAX_GENERIEK));
    }

    @Override
    public void serialize(ValueOutput out) {
        out.putBoolean("binnen", binnen);
        out.store("opgeborgen", TAGS, List.copyOf(opgeborgen));
        out.store("rijk_inventaris", TAGS, List.copyOf(rijkInventaris));
        out.store("gevonden", TAGS, List.copyOf(gevonden));
        out.putInt("opgeborgen_slot", opgeborgenSlot);
        out.putInt("rijk_slot", rijkSlot);
        if (terug != null) {
            out.store("terug", CompoundTag.CODEC, terug);
        }
        out.putString("klasse", klasse);
        CompoundTag x = new CompoundTag();
        xp.forEach(x::putLong);
        out.store("xp", CompoundTag.CODEC, x);
        out.putLong("gxp", generiekXp);
        out.putDouble("vads", vads);
        out.putDouble("energie", energie);
        out.putDouble("ult", ultLading);
        out.putLong("munten", munten);
        if (checkpoint != null) {
            out.putLong("checkpoint", checkpoint.asLong());
        }
        out.putLong("duizelig_tot", duizeligTot);
        out.putInt("opgevrolijkt", opgevrolijkt);
        out.putBoolean("intro", introGezien);
        out.store("tips", Codec.STRING.listOf(), List.copyOf(tips));
        out.store("gezien", Codec.STRING.listOf(), List.copyOf(gezien));
        CompoundTag b = new CompoundTag();
        boom.forEach((k, v) -> b.putString(k, String.join(",", v)));
        out.store("boom", CompoundTag.CODEC, b);
        out.putLong("respec", respecKlaar);
        CompoundTag sr = new CompoundTag();
        sieraden.forEach(sr::put);
        out.store("sieraden", CompoundTag.CODEC, sr);
        CompoundTag e = new CompoundTag();
        extra.forEach(e::put);
        out.store("extra", CompoundTag.CODEC, e);
    }

    @Override
    public void deserialize(ValueInput in) {
        binnen = in.getBooleanOr("binnen", false);
        opgeborgen.clear();
        opgeborgen.addAll(in.read("opgeborgen", TAGS).orElse(List.of()));
        rijkInventaris.clear();
        rijkInventaris.addAll(in.read("rijk_inventaris", TAGS).orElse(List.of()));
        gevonden.clear();
        gevonden.addAll(in.read("gevonden", TAGS).orElse(List.of()));
        opgeborgenSlot = in.getIntOr("opgeborgen_slot", 0);
        rijkSlot = in.getIntOr("rijk_slot", 0);
        terug = in.read("terug", CompoundTag.CODEC).orElse(null);
        klasse = in.getStringOr("klasse", "knuffelridder");
        xp.clear();
        CompoundTag x = in.read("xp", CompoundTag.CODEC).orElse(new CompoundTag());
        for (String k : x.keySet()) {
            xp.put(k, x.getLongOr(k, 0L));
        }
        java.util.Optional<Long> g = in.getLong("gxp");
        gemigreerd = g.isEmpty();
        generiekXp = g.orElseGet(() -> migreerGeneriek(xp));
        vads = in.getDoubleOr("vads", -1);
        energie = in.getDoubleOr("energie", -1);
        ultLading = in.getDoubleOr("ult", 0);
        munten = in.getLongOr("munten", 0);
        checkpoint = in.getLong("checkpoint").map(BlockPos::of).orElse(null);
        duizeligTot = in.getLongOr("duizelig_tot", 0);
        opgevrolijkt = in.getIntOr("opgevrolijkt", 0);
        introGezien = in.getBooleanOr("intro", false);
        tips.clear();
        tips.addAll(in.read("tips", Codec.STRING.listOf()).orElse(List.of()));
        gezien.clear();
        gezien.addAll(in.read("gezien", Codec.STRING.listOf()).orElse(List.of()));
        boom.clear();
        CompoundTag b = in.read("boom", CompoundTag.CODEC).orElse(new CompoundTag());
        for (String k : b.keySet()) {
            String s = b.getStringOr(k, "");
            boom.put(k, s.isEmpty() ? new LinkedHashSet<>() : new LinkedHashSet<>(List.of(s.split(","))));
        }
        respecKlaar = in.getLongOr("respec", 0);
        sieraden.clear();
        sieradenCache.clear();
        CompoundTag sr = in.read("sieraden", CompoundTag.CODEC).orElse(new CompoundTag());
        for (String k : sr.keySet()) {
            sr.getCompound(k).ifPresent(t -> sieraden.put(k, t));
        }
        extra.clear();
        CompoundTag e = in.read("extra", CompoundTag.CODEC).orElse(new CompoundTag());
        for (String k : e.keySet()) {
            e.getCompound(k).ifPresent(t -> extra.put(k, t));
        }
    }
}
