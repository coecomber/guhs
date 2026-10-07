package nl.juiced.guhs.feature.einde2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * einde2: the hooks the later phases hang their story on. Phase A ships them as working no-ops (nothing is locked,
 * nobody has learnt an element, the workshop door stays shut, nothing is hidden, no story is finished, nobody knows the
 * recipe); phase B (story, quests) and phase C (tech) only set or add to these fields, they never need to change the code
 * that asks.
 * <p>
 * The portal lock is a LIST of separate conditions behind one question ({@link #magNaarGuheinde}): every condition is its
 * own entry of {@link #PORTAAL_SLOTEN} with its own message, added by whoever owns it; nothing else decides about that
 * portal, and whoever wants to say what is missing asks {@link #portaalRedenen}.
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

    /** Every lock that refuses this entity, in the order the locks were added (empty: it may go through). */
    public static List<Component> portaalRedenen(ServerLevel level, Entity entity) {
        List<Component> redenen = new ArrayList<>();
        for (BiFunction<ServerLevel, Entity, Component> slot : PORTAAL_SLOTEN) {
            Component reden = slot.apply(level, entity);
            if (reden != null) {
                redenen.add(reden);
            }
        }
        return redenen;
    }

    /** The one question about the portal into the Guheinde: may this entity go through? */
    public static boolean magNaarGuheinde(ServerLevel level, Entity entity) {
        return portaalGeweigerd(level, entity) == null;
    }

    /** hook fase B: has this player learnt this element? Side-place residents ask it (Einde2Ids.ELEMENT_VAN). Default: nobody. */
    public static BiPredicate<ServerPlayer, Einde2Ids.Element> ELEMENT_GELEERD = (p, e) -> false;
    /** hook fase B / fase C: may this player walk through the Werktuigguh's door (block guhs:werkplaatsdeur)? Default: nobody. */
    public static Predicate<Player> WERKPLAATS_OPEN = p -> false;
    /** hook fase B: structures hidden for a player (veil, compass, map). Default: nothing is hidden. */
    public static BiPredicate<ServerPlayer, String> VERBORGEN = (p, structuur) -> false;

    /** hook fase B: the stories of einde2 as the story engine will know them: ONE group, fixed ids. */
    public static final String VERHAAL_GROEP = "guheinde";
    /** The big stories (they count for "alle verhalen gevolgd"), in playing order. */
    public static final List<String> GROTE_VERHALEN = List.of("guhvatar_lucht", "guhvatar_water", "guhvatar_aarde", "guhvatar_vuur",
            "sponsguh_recept", "sponsguh_kroon");
    /** The small ones (side quests and tech): optional, they never count for a lock. */
    public static final List<String> KLEINE_VERHALEN = List.of("omashguh", "westelijke_luchttempel", "vuurtempel_roku", "kyoshi_dorp",
            "bibliotheek_wan_shi_guh", "leeuwschildpad", "leegtewalvis", "mika_kok", "vetbootje", "sandy_boomkoepel", "vliegende_guhlander",
            "zeemeerman", "werktuigguh", "vredesballon");
    /** hook fase B: has this player finished this story (per player)? Default: nobody. Nothing in phase A keeps "story done" flags of its own. */
    public static BiPredicate<ServerPlayer, String> VERHAAL_KLAAR = (p, id) -> false;

    /** How many of the {@link #GROTE_VERHALEN} this player has finished. */
    public static int groteVerhalenKlaar(ServerPlayer p) {
        int n = 0;
        for (String id : GROTE_VERHALEN) {
            if (VERHAAL_KLAAR.test(p, id)) {
                n++;
            }
        }
        return n;
    }

    /** hook fase B: does this player know the secret Knabbelburger recipe (the krokante_grill bakes only then)? Default: nobody. */
    public static Predicate<Player> KNABBELBURGER_RECEPT = p -> false;
    /** hook fase B: called at the end of GuheindeGevecht.startFight (the Opper-Mika's entrance scene, id "opper_mika_opkomst"). Default: nothing. */
    public static final List<Consumer<ServerLevel>> GEVECHT_BEGINT = new CopyOnWriteArrayList<>();

    private Einde2Haken() {
    }
}
