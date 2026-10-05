package nl.juiced.guhs.feature.huisje;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/** The registered toy kinds ({@link Speeltje}). */
public final class Speelgoed {
    private static final List<Speeltje> ALLE = new CopyOnWriteArrayList<>();

    private Speelgoed() {
    }

    public static void registreer(Speeltje s) {
        for (int i = 0; i < ALLE.size(); i++) {
            if (ALLE.get(i).id().equals(s.id())) {
                ALLE.set(i, s);
                return;
            }
        }
        ALLE.add(s);
    }

    public static List<Speeltje> alle() {
        return List.copyOf(ALLE);
    }

    @Nullable
    public static Speeltje van(String id) {
        for (Speeltje s : ALLE) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** 1.2.8: every toy line of the overview (the kinds in registration order), counted like {@link Speeltje#tel}. */
    public static List<Speeltje.Telling> tel(ServerLevel level, BlockPos rond, int bereik, java.util.function.Predicate<BlockPos> binnen) {
        List<Speeltje.Telling> uit = new ArrayList<>();
        for (Speeltje s : ALLE) {
            uit.addAll(s.tel(level, rond, bereik, binnen));
        }
        return uit;
    }

    /** A random toy nearby to play with (the kinds in random order, the first that has one), or null. */
    @Nullable
    public static KlusTaak willekeurig(ServerLevel level, Mob wie, BlockPos rond, int bereik) {
        List<Speeltje> soorten = new ArrayList<>(ALLE);
        Collections.shuffle(soorten, new java.util.Random(wie.getRandom().nextLong()));
        for (Speeltje s : soorten) {
            KlusTaak t = s.zoek(level, wie, rond, bereik);
            if (t != null) {
                return t;
            }
        }
        return null;
    }
}
