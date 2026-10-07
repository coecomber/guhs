package nl.juiced.guhs.feature.oudescenes;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * bbq2 (oude-scenes): one of the six scenes of the older stories: its script, where it stands (the structure, the jigsaw
 * piece and the template block it is anchored on), what this slice's client adds to it, and how to tell that a player is
 * already past its moment in the story (they get it in the Guhdex to watch, nothing more).
 */
public record OudeScene(String naam, Cutscene scene, String structuur, @Nullable String stuk, BlockPos anker, Effecten effecten,
                        Predicate<ServerPlayer> voorbij) {
    /** The scene's id (oudescenes_&lt;naam&gt;): what the verhaal engine and the Guhdex know it by. */
    public String id() {
        return scene.id();
    }
}
