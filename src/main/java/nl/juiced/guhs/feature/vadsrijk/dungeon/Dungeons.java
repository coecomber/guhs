package nl.juiced.guhs.feature.vadsrijk.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.neoforged.bus.api.IEventBus;

/**
 * W-GROT: every dungeon in this build. Dungeon types register a source ({@link #bron}); the caves do that from
 * {@code grotkerker.GrotKerker.register}. Registered from {@code VadsrijkFeature} (the door block, the queue, the commands).
 */
public final class Dungeons {
    private static final List<Supplier<List<? extends Dungeon>>> BRONNEN = new ArrayList<>();
    /** Gametests: a dungeon standing in for the real one with the same id (the server tick skips the id). */
    public static final Map<String, Dungeon> TEST = new java.util.concurrent.ConcurrentHashMap<>();

    private Dungeons() {
    }

    public static void register(IEventBus modBus) {
        DungeonDeur.register(modBus);
        DungeonRij.register();
    }

    public static void bron(Supplier<List<? extends Dungeon>> b) {
        BRONNEN.add(b);
    }

    public static List<Dungeon> alle() {
        List<Dungeon> l = new ArrayList<>();
        for (Supplier<List<? extends Dungeon>> b : BRONNEN) {
            l.addAll(b.get());
        }
        return l;
    }

    @Nullable
    public static Dungeon van(String id) {
        Dungeon t = TEST.get(id);
        if (t != null) {
            return t;
        }
        for (Dungeon d : alle()) {
            if (d.id().equals(id)) {
                return d;
            }
        }
        return null;
    }
}
