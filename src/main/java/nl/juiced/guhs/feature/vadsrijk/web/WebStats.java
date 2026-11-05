package nl.juiced.guhs.feature.vadsrijk.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * (S-SITE) The public numbers per player for guhs.nl/vadsrijk ({@code data/guhs/vadsrijk_web.dat} with the overworld), so the
 * highscores also hold players who are offline without reading every player file. One {@link CompoundTag} per player UUID with
 * ONLY the allowlisted fields of {@link WebExport#snapshot}: name, levels, class levels, cheered count, bosses, book %, pets/mounts
 * counts, title, first-level-50 time and the last-seen DAY. Never an IP, a position, coins, chat or trades.
 */
public final class WebStats extends SavedData {
    static final SavedDataType<WebStats> TYPE = GuhSavedData.tagType("vadsrijk_web", WebStats::new, WebStats::load, WebStats::save);

    final Map<UUID, CompoundTag> spelers = new LinkedHashMap<>();

    public static WebStats get(MinecraftServer server) {
        return GuhSavedData.get(server.overworld(), TYPE, "guhs_vadsrijk_web");
    }

    /** Merge a fresh snapshot (keeps "eerste_50" and the boss counts once set). */
    public void zet(UUID id, CompoundTag nieuw) {
        CompoundTag oud = spelers.get(id);
        if (oud != null) {
            long eerste = oud.getLongOr("eerste_50", 0L);
            if (eerste > 0) {
                nieuw.putLong("eerste_50", eerste);
            }
            CompoundTag bazen = oud.getCompoundOrEmpty("bazen").copy();
            nieuw.getCompoundOrEmpty("bazen").keySet().forEach(k -> bazen.putInt(k, Math.max(bazen.getIntOr(k, 0),
                    nieuw.getCompoundOrEmpty("bazen").getIntOr(k, 0))));
            nieuw.put("bazen", bazen);
        }
        spelers.put(id, nieuw);
        setDirty();
    }

    /** A boss of this arena was beaten with this player's help (>= 5 % contribution, kern's rule). */
    public void baas(UUID id, String naam, String arena) {
        CompoundTag t = spelers.computeIfAbsent(id, k -> {
            CompoundTag n = new CompoundTag();
            n.putString("naam", naam);
            return n;
        });
        CompoundTag b = t.getCompoundOrEmpty("bazen");
        b.putInt(arena, b.getIntOr(arena, 0) + 1);
        t.put("bazen", b);
        setDirty();
    }

    private static WebStats load(CompoundTag t) {
        WebStats w = new WebStats();
        for (String k : t.keySet()) {
            try {
                w.spelers.put(UUID.fromString(k), t.getCompoundOrEmpty(k));
            } catch (IllegalArgumentException ignored) {
                // not a uuid
            }
        }
        return w;
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        spelers.forEach((k, v) -> t.put(k.toString(), v));
        return t;
    }
}
