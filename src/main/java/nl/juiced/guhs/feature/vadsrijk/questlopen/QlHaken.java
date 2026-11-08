package nl.juiced.guhs.feature.vadsrijk.questlopen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.minecraft.server.level.ServerPlayer;

/**
 * U-QUESTLOPEN: "is a story scene of this player still running?" Each zone adds its own (the scene runners keep their state
 * package-private): the Questlopen screen waits until all are quiet before it teleports on or opens again. Kern's RijkScenes and
 * the talking screen are asked directly by {@link QuestLoper#rustig}.
 */
public final class QlHaken {
    /** true = this player is in a scene of that zone right now. */
    public static final List<Predicate<ServerPlayer>> SCENE = new CopyOnWriteArrayList<>();

    private QlHaken() {
    }
}
