package nl.juiced.guhs.feature.band;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * 1.6.2: "Roep naar mij" for a guh that sits (as a picked-up guh) in the pockets or ender chest of a player who is offline.
 * We can't open an offline player's inventory, so the call waits ({@link #wacht}): as soon as that player logs in, the guh
 * comes out of their pockets; if its owner is online it appears next to them at once, else it waits as "post" here and
 * appears when the owner logs in. SavedData {@code guhs:roep_post} (with the overworld).
 */
public final class RoepPost extends SavedData {
    public static final SavedDataType<RoepPost> TYPE = GuhSavedData.tagType("roep_post", RoepPost::new, RoepPost::load, RoepPost::save);

    /** A call waiting for a holder (by name, lower case) to log in. */
    private record Wacht(String houder, UUID eigenaar, UUID id) {
    }

    /** A guh taken out of an offline holder's pockets, waiting for its owner. */
    private record Post(UUID eigenaar, CompoundTag data) {
    }

    private final List<Wacht> wachten = new ArrayList<>();
    private final List<Post> post = new ArrayList<>();

    static RoepPost get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The guh waits until this (offline) player logs in. */
    static void wacht(MinecraftServer s, String houder, UUID eigenaar, UUID id) {
        RoepPost p = get(s);
        String h = houder.toLowerCase(Locale.ROOT);
        p.wachten.removeIf(w -> w.id().equals(id));
        p.wachten.add(new Wacht(h, eigenaar, id));
        p.setDirty();
    }

    static boolean wachtOp(MinecraftServer s, UUID id) {
        return get(s).wachten.stream().anyMatch(w -> w.id().equals(id));
    }

    /** A player logs in: guhs called from their pockets go out; guhs waiting for them as post arrive. */
    static void login(ServerPlayer speler) {
        MinecraftServer s = speler.level().getServer();
        RoepPost p = get(s);
        String naam = speler.getGameProfile().name().toLowerCase(Locale.ROOT);
        for (Iterator<Wacht> it = p.wachten.iterator(); it.hasNext(); ) {
            Wacht w = it.next();
            if (!w.houder().equals(naam)) {
                continue;
            }
            it.remove();
            p.setDirty();
            CompoundTag data = UitItem.uitSpeler(speler, w.id());
            if (data == null) {
                continue;                                     // (not in their pockets any more: nothing to do)
            }
            ServerPlayer eigenaar = s.getPlayerList().getPlayer(w.eigenaar());
            if (eigenaar != null) {
                Roepen.verschijn(eigenaar, data);
            } else {
                p.post.add(new Post(w.eigenaar(), data));
            }
        }
        for (Iterator<Post> it = p.post.iterator(); it.hasNext(); ) {
            Post x = it.next();
            if (x.eigenaar().equals(speler.getUUID())) {
                it.remove();
                p.setDirty();
                Roepen.verschijn(speler, x.data());
            }
        }
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        ListTag w = new ListTag();
        for (Wacht x : wachten) {
            CompoundTag c = new CompoundTag();
            c.putString("Houder", x.houder());
            c.store("Eigenaar", UUIDUtil.CODEC, x.eigenaar());
            c.store("Id", UUIDUtil.CODEC, x.id());
            w.add(c);
        }
        t.put("Wachten", w);
        ListTag l = new ListTag();
        for (Post x : post) {
            CompoundTag c = new CompoundTag();
            c.store("Eigenaar", UUIDUtil.CODEC, x.eigenaar());
            c.put("Data", x.data());
            l.add(c);
        }
        t.put("Post", l);
        return t;
    }

    private static RoepPost load(CompoundTag t) {
        RoepPost p = new RoepPost();
        ListTag w = t.getListOrEmpty("Wachten");
        for (int i = 0; i < w.size(); i++) {
            CompoundTag c = w.getCompoundOrEmpty(i);
            UUID e = c.read("Eigenaar", UUIDUtil.CODEC).orElse(null), id = c.read("Id", UUIDUtil.CODEC).orElse(null);
            if (e != null && id != null) {
                p.wachten.add(new Wacht(c.getStringOr("Houder", ""), e, id));
            }
        }
        ListTag l = t.getListOrEmpty("Post");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompoundOrEmpty(i);
            UUID e = c.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
            if (e != null) {
                p.post.add(new Post(e, c.getCompoundOrEmpty("Data")));
            }
        }
        return p;
    }
}
