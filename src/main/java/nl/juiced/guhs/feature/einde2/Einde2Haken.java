package nl.juiced.guhs.feature.einde2;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * einde2: the hooks the later phases hang their story on. Phase A ships them as working no-ops (nothing is locked,
 * nobody has learnt an element, the workshop door stays shut, nothing is hidden); phase B (story, quests) and phase C
 * (tech) only set or add to these fields, they never need to change the code that asks.
 */
public final class Einde2Haken {
    /**
     * hook fase B: the portal lock. A non-null Component = refused with that message. Asked only on the way INTO the
     * Guheinde (GuheindePortaalBlock.entityInside), never on the way back.
     */
    public static final List<BiFunction<ServerLevel, Entity, Component>> PORTAAL_SLOTEN = new CopyOnWriteArrayList<>();

    /** The message of the first lock that refuses this entity (null: it may go through). */
    @Nullable
    public static Component portaalGeweigerd(ServerLevel level, Entity entity) {
        for (BiFunction<ServerLevel, Entity, Component> slot : PORTAAL_SLOTEN) {
            Component reden = slot.apply(level, entity);
            if (reden != null) {
                return reden;
            }
        }
        return null;
    }

    /** hook fase B: has this player learnt this element? Side-place residents ask it (Einde2Ids.ELEMENT_VAN). Default: nobody. */
    public static BiPredicate<ServerPlayer, Einde2Ids.Element> ELEMENT_GELEERD = (p, e) -> false;
    /** hook fase B / fase C: may this player walk through the Werktuigguh's door (block guhs:werkplaatsdeur)? Default: nobody. */
    public static Predicate<Player> WERKPLAATS_OPEN = p -> false;
    /** hook fase B: structures hidden for a player (veil, compass, map). Default: nothing is hidden. */
    public static BiPredicate<ServerPlayer, String> VERBORGEN = (p, structuur) -> false;

    private Einde2Haken() {
    }
}
