package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.einde2.Einde2Plaatshouder;

/**
 * einde2, slice sponsguh_wezens: the base class of the whole cast of the Sponsguh story (every character is its own
 * entity type on this one class): lief, can't be hurt, looks at players, keeps to its Honk.
 * Skeleton stub (a stand-in that does nothing, it only remembers its animation name): the sponsguh_wezens slice makes the
 * real class under this name (a PathfinderMob + GeoEntity with a synced animation).
 * <p>
 * hook fase B: every dialogue and scene plays an animation with {@link #zetAnimatie} and answers a click through
 * {@link #BIJ_KLIK}.
 */
public class SponsguhWezen extends Einde2Plaatshouder {
    /** Asked in mobInteract, in the order they were added: the first that answers true took the click. */
    public static final List<BiFunction<SponsguhWezen, ServerPlayer, Boolean>> BIJ_KLIK = new CopyOnWriteArrayList<>();

    private String animatie = "idle";

    public SponsguhWezen(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** Plays this animation of the model ("idle", "loop", "praat", "lach", "schrik", "juich", or one of the character's own). */
    public void zetAnimatie(String naam) {
        animatie = naam;
    }

    public String animatie() {
        return animatie;
    }
}
