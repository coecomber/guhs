package nl.juiced.guhs.world;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhVariant;

import net.minecraft.core.UUIDUtil;
/**
 * Everything the Guhs mod remembers per world (stored with the overworld): every player's guh stomach (maag), their
 * quest progress, where they came from before stepping into a stomach, and their Guhdex.
 */
public class GuhWorldData extends SavedData {
    private static final String NAME = "guhs_world";
    private static final net.minecraft.world.level.saveddata.SavedDataType<GuhWorldData> TYPE =
            nl.juiced.guhs.storage.GuhSavedData.tagType("world", GuhWorldData::new, t -> load(t, null), d -> d.save(new CompoundTag(), null));   // (registries never used)

    public enum Access { PUBLIC, PRIVATE, WHITELIST }

    /** One player's stomach: a plot in the guhmaag dimension. */
    public static class Maag {
        public final UUID owner;
        public String ownerName;
        public final int index;
        public int size = MaagManager.SIZES[0];
        public Access access = Access.PUBLIC;
        /** name -> may build/interact (true) or only visit (false). */
        public final Map<UUID, WhitelistEntry> whitelist = new LinkedHashMap<>();
        public boolean built;

        Maag(UUID owner, String ownerName, int index) {
            this.owner = owner;
            this.ownerName = ownerName;
            this.index = index;
        }

        public boolean mayVisit(UUID player) {
            return player.equals(owner) || access == Access.PUBLIC || (access == Access.WHITELIST && whitelist.containsKey(player));
        }

        public boolean mayBuild(UUID player) {
            WhitelistEntry entry = whitelist.get(player);
            return player.equals(owner) || (access == Access.WHITELIST && entry != null && entry.build);
        }
    }

    public static class WhitelistEntry {
        public final String name;
        public boolean build;

        public WhitelistEntry(String name, boolean build) {
            this.name = name;
            this.build = build;
        }
    }

    /** Quest progress and other per-player things. */
    public static class PlayerData {
        /** 0 = not started, 1 = find the Mika camp, 2 = find the lost cake, 3 = bring cake + balloons, 4 = done (stomach unlocked). */
        public int maagQuest;
        public int rpsStreak;
        /** Has the Mika-baas let slip that there is a fourth move (VADS)? Hidden the first time you play. */
        public boolean vadsRevealed;
        /** 0 = not started, 1 = collecting the 3 parts, 2 = done (sled unlocked). */
        public int sledQuest;
        public boolean beatBigMika;
        @Nullable
        public ResourceKey<Level> returnDimension;
        public Vec3 returnPos = Vec3.ZERO;
        public float returnYaw;
        public final Set<GuhVariant> seen = EnumSet.noneOf(GuhVariant.class);
        public final Set<GuhVariant> tamed = EnumSet.noneOf(GuhVariant.class);
        /** Guhdex rewards already claimed (milestone indices). */
        public final Set<Integer> rewards = new java.util.HashSet<>();

        public boolean maagUnlocked() {
            return maagQuest >= 4;
        }
    }

    private final Map<UUID, Maag> maags = new LinkedHashMap<>();
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private boolean lobbyBuilt;

    public static GuhWorldData get(MinecraftServer server) {
        return nl.juiced.guhs.storage.GuhSavedData.get(server.overworld(), TYPE, NAME);
    }

    public PlayerData player(UUID id) {
        return players.computeIfAbsent(id, k -> new PlayerData());
    }

    @Nullable
    public Maag maagOf(UUID owner) {
        return maags.get(owner);
    }

    public Maag createMaag(UUID owner, String name) {
        Maag maag = maags.get(owner);
        if (maag == null) {
            maag = new Maag(owner, name, maags.size());
            maags.put(owner, maag);
            setDirty();
        }
        return maag;
    }

    @Nullable
    public Maag maagByIndex(int index) {
        for (Maag maag : maags.values()) {
            if (maag.index == index) {
                return maag;
            }
        }
        return null;
    }

    public List<Maag> allMaags() {
        return new ArrayList<>(maags.values());
    }

    public boolean isLobbyBuilt() {
        return lobbyBuilt;
    }

    public void setLobbyBuilt() {
        lobbyBuilt = true;
        setDirty();
    }

    // --- saving -------------------------------------------------------------------------------------------------------

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag maagList = new ListTag();
        for (Maag maag : maags.values()) {
            CompoundTag m = new CompoundTag();
            m.store("Owner", UUIDUtil.CODEC, maag.owner);
            m.putString("Name", maag.ownerName);
            m.putInt("Index", maag.index);
            m.putInt("Size", maag.size);
            m.putString("Access", maag.access.name());
            m.putBoolean("Built", maag.built);
            ListTag wl = new ListTag();
            maag.whitelist.forEach((id, entry) -> {
                CompoundTag e = new CompoundTag();
                e.store("Id", UUIDUtil.CODEC, id);
                e.putString("Name", entry.name);
                e.putBoolean("Build", entry.build);
                wl.add(e);
            });
            m.put("Whitelist", wl);
            maagList.add(m);
        }
        tag.put("Maags", maagList);
        ListTag playerList = new ListTag();
        players.forEach((id, p) -> {
            CompoundTag c = new CompoundTag();
            c.store("Id", UUIDUtil.CODEC, id);
            c.putInt("MaagQuest", p.maagQuest);
            c.putInt("RpsStreak", p.rpsStreak);
            c.putBoolean("VadsRevealed", p.vadsRevealed);
            c.putInt("SledQuest", p.sledQuest);
            c.putBoolean("BeatBigMika", p.beatBigMika);
            if (p.returnDimension != null) {
                c.putString("ReturnDim", p.returnDimension.identifier().toString());
                c.putDouble("ReturnX", p.returnPos.x);
                c.putDouble("ReturnY", p.returnPos.y);
                c.putDouble("ReturnZ", p.returnPos.z);
                c.putFloat("ReturnYaw", p.returnYaw);
            }
            c.put("Seen", variants(p.seen));
            c.put("Tamed", variants(p.tamed));
            c.putIntArray("Rewards", p.rewards.stream().mapToInt(Integer::intValue).toArray());
            playerList.add(c);
        });
        tag.put("Players", playerList);
        tag.putBoolean("LobbyBuilt", lobbyBuilt);
        return tag;
    }

    private static ListTag variants(Set<GuhVariant> set) {
        ListTag list = new ListTag();
        set.forEach(v -> list.add(StringTag.valueOf(v.id())));
        return list;
    }

    private static void readVariants(ListTag list, Set<GuhVariant> into) {
        for (int i = 0; i < list.size(); i++) {
            GuhVariant v = GuhVariant.byId(list.getStringOr(i, ""));
            if (v != GuhVariant.NORMAL || "normal".equals(list.getStringOr(i, ""))) {
                into.add(v);
            }
        }
    }

    public static GuhWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        GuhWorldData data = new GuhWorldData();
        ListTag maagList = tag.getListOrEmpty("Maags");
        for (int i = 0; i < maagList.size(); i++) {
            CompoundTag m = maagList.getCompoundOrEmpty(i);
            Maag maag = new Maag(m.read("Owner", UUIDUtil.CODEC).orElseThrow(), m.getStringOr("Name", ""), m.getIntOr("Index", 0));
            maag.size = m.getIntOr("Size", 0);
            try {
                maag.access = Access.valueOf(m.getStringOr("Access", ""));
            } catch (IllegalArgumentException ignored) {
                maag.access = Access.PUBLIC;
            }
            maag.built = m.getBooleanOr("Built", false);
            ListTag wl = m.getListOrEmpty("Whitelist");
            for (int j = 0; j < wl.size(); j++) {
                CompoundTag e = wl.getCompoundOrEmpty(j);
                maag.whitelist.put(e.read("Id", UUIDUtil.CODEC).orElseThrow(), new WhitelistEntry(e.getStringOr("Name", ""), e.getBooleanOr("Build", false)));
            }
            data.maags.put(maag.owner, maag);
        }
        ListTag playerList = tag.getListOrEmpty("Players");
        for (int i = 0; i < playerList.size(); i++) {
            CompoundTag c = playerList.getCompoundOrEmpty(i);
            PlayerData p = new PlayerData();
            p.maagQuest = c.getIntOr("MaagQuest", 0);
            p.rpsStreak = c.getIntOr("RpsStreak", 0);
            p.vadsRevealed = c.getBooleanOr("VadsRevealed", false);
            p.sledQuest = c.getIntOr("SledQuest", 0);
            p.beatBigMika = c.getBooleanOr("BeatBigMika", false);
            if (c.contains("ReturnDim")) {
                p.returnDimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(c.getStringOr("ReturnDim", "")));
                p.returnPos = new Vec3(c.getDoubleOr("ReturnX", 0.0), c.getDoubleOr("ReturnY", 0.0), c.getDoubleOr("ReturnZ", 0.0));
                p.returnYaw = c.getFloatOr("ReturnYaw", 0.0F);
            }
            readVariants(c.getListOrEmpty("Seen"), p.seen);
            readVariants(c.getListOrEmpty("Tamed"), p.tamed);
            for (int r : c.getIntArray("Rewards").orElse(new int[0])) {
                p.rewards.add(r);
            }
            data.players.put(c.read("Id", UUIDUtil.CODEC).orElseThrow(), p);
        }
        data.lobbyBuilt = tag.getBooleanOr("LobbyBuilt", false);
        return data;
    }
}
