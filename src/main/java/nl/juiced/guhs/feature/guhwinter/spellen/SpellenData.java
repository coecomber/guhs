package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Everything the Guhwinter games remember (with the overworld, file guhs:guhwinter_spellen): the top 10 of the
 * Pepernotenrace and of the schaatsrondje (one line per player, their best), and per player their own bests and what they
 * already got once (the first run's bonus, the first lap, the hidden pepernoten they found, the first oliebollen).
 */
public final class SpellenData extends SavedData {
    public static final String RACE = "race", SCHAATS = "schaats", OLIEBOLLEN = "oliebollen";
    public static final int PLAATSEN = 10;

    /** One line of a board. */
    public record Regel(UUID speler, String naam, int score) {
    }

    private final Map<String, List<Regel>> borden = new HashMap<>();
    private final Map<UUID, CompoundTag> spelers = new HashMap<>();

    static final SavedDataType<SpellenData> TYPE = GuhSavedData.tagType("guhwinter_spellen", SpellenData::new, SpellenData::laad, SpellenData::bewaar);

    public static SpellenData get(MinecraftServer server) {
        return GuhSavedData.get(server.overworld(), TYPE, "guhs_guhwinter_spellen");
    }

    public static boolean lagerIsBeter(String bord) {
        return SCHAATS.equals(bord);
    }

    // --- boards ------------------------------------------------------------------------------------------------------------

    public List<Regel> top(String bord) {
        return List.copyOf(borden.getOrDefault(bord, List.of()));
    }

    /** This player's best on a board (also outside the top 10), or -1. */
    public int beste(UUID speler, String bord) {
        CompoundTag t = spelers.get(speler);
        return t == null ? -1 : t.getCompoundOrEmpty("Beste").getIntOr(bord, -1);
    }

    /** What handing in a score did. */
    public record Inzending(int plaats, boolean nieuwBeste, boolean eersteKeer) {
    }

    /** Hands in a score: their own best and the top 10 get it when it's better. */
    public Inzending zet(ServerPlayer player, String bord, int score) {
        boolean lager = lagerIsBeter(bord);
        UUID id = player.getUUID();
        int oud = beste(id, bord);
        boolean eerste = oud < 0;
        boolean beter = eerste || (lager ? score < oud : score > oud);
        if (beter) {
            CompoundTag t = speler(id);
            CompoundTag b = t.getCompoundOrEmpty("Beste");
            b.putInt(bord, score);
            t.put("Beste", b);
        }
        List<Regel> lijst = borden.computeIfAbsent(bord, k -> new ArrayList<>());
        Regel eigen = lijst.stream().filter(r -> r.speler().equals(id)).findFirst().orElse(null);
        if (eigen == null || (lager ? score < eigen.score() : score > eigen.score())) {
            lijst.remove(eigen);
            lijst.add(new Regel(id, player.getGameProfile().name(), score));
            lijst.sort((a, c) -> lager ? Integer.compare(a.score(), c.score()) : Integer.compare(c.score(), a.score()));
            while (lijst.size() > PLAATSEN) {
                lijst.remove(lijst.size() - 1);
            }
        }
        setDirty();
        int plaats = 0;
        for (int i = 0; i < lijst.size(); i++) {
            if (lijst.get(i).speler().equals(id) && lijst.get(i).score() == score) {
                plaats = i + 1;
            }
        }
        return new Inzending(plaats, beter && !eerste, eerste);
    }

    /** (Dev) empties a board. */
    public void wis(String bord) {
        borden.remove(bord);
        setDirty();
    }

    // --- per player --------------------------------------------------------------------------------------------------------

    private CompoundTag speler(UUID id) {
        return spelers.computeIfAbsent(id, k -> new CompoundTag());
    }

    public boolean vlag(UUID id, String naam) {
        CompoundTag t = spelers.get(id);
        return t != null && t.getBooleanOr(naam, false);
    }

    public void zetVlag(UUID id, String naam, boolean aan) {
        speler(id).putBoolean(naam, aan);
        setDirty();
    }

    /** Has this player found the hidden pepernoten with this key? */
    public boolean gevonden(UUID id, String sleutel) {
        CompoundTag t = spelers.get(id);
        if (t == null) {
            return false;
        }
        for (Tag s : t.getListOrEmpty("Gevonden")) {
            if (s instanceof StringTag st && st.value().equals(sleutel)) {
                return true;
            }
        }
        return false;
    }

    public void vind(UUID id, String sleutel) {
        CompoundTag t = speler(id);
        ListTag l = t.getListOrEmpty("Gevonden");
        l.add(StringTag.valueOf(sleutel));
        t.put("Gevonden", l);
        setDirty();
    }

    public int aantalGevonden(UUID id) {
        CompoundTag t = spelers.get(id);
        return t == null ? 0 : t.getListOrEmpty("Gevonden").size();
    }

    /** (Dev) forgets everything of one player. */
    public void vergeet(UUID id) {
        spelers.remove(id);
        setDirty();
    }

    // --- saving ------------------------------------------------------------------------------------------------------------

    private CompoundTag bewaar() {
        CompoundTag tag = new CompoundTag();
        CompoundTag b = new CompoundTag();
        borden.forEach((bord, lijst) -> {
            ListTag l = new ListTag();
            for (Regel r : lijst) {
                CompoundTag t = new CompoundTag();
                t.store("Speler", UUIDUtil.CODEC, r.speler());
                t.putString("Naam", r.naam());
                t.putInt("Score", r.score());
                l.add(t);
            }
            b.put(bord, l);
        });
        tag.put("Borden", b);
        CompoundTag s = new CompoundTag();
        spelers.forEach((id, t) -> s.put(id.toString(), t.copy()));
        tag.put("Spelers", s);
        return tag;
    }

    private static SpellenData laad(CompoundTag tag) {
        SpellenData d = new SpellenData();
        CompoundTag b = tag.getCompoundOrEmpty("Borden");
        for (String bord : b.keySet()) {
            List<Regel> lijst = new ArrayList<>();
            for (Tag t : b.getListOrEmpty(bord)) {
                if (t instanceof CompoundTag c) {
                    c.read("Speler", UUIDUtil.CODEC).ifPresent(id -> lijst.add(new Regel(id, c.getStringOr("Naam", "?"), c.getIntOr("Score", 0))));
                }
            }
            d.borden.put(bord, lijst);
        }
        CompoundTag s = tag.getCompoundOrEmpty("Spelers");
        for (String k : s.keySet()) {
            try {
                d.spelers.put(UUID.fromString(k), s.getCompoundOrEmpty(k).copy());
            } catch (IllegalArgumentException ignored) {
                // (not a player id)
            }
        }
        return d;
    }
}
