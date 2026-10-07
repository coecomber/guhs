package nl.juiced.guhs.feature.snuffel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.quest.GuhAdvancements;
import org.slf4j.Logger;

/**
 * The scents a dog can learn. A scent ({@link Geur}) has an id, a kind ({@link GeurSoort}: the colour of the meter), the
 * rank a dog needs to smell it and an item for its picture in the snuffelboekje; its name is the lang key
 * {@code gui.guhs.snuffel.geur.<id>} (optional line under it: {@code gui.guhs.snuffel.geur.<id>.tekst}).
 * <p>
 * Scents come from the island's data ({@code "geuren"} in eiland.json, registered when this class loads) and from code
 * ({@link #registreer}, in a feature's {@code register}). What a player has learned is kept per player ({@link SnuffelData},
 * key {@code Geuren}); the number of learned scents is the rank ({@link Rang}).
 */
public final class Geuren {
    private static final Logger LOG = LogUtils.getLogger();

    /** One scent. {@code rang}: 1..5, the lowest rank that smells it. {@code icoon}: an item id. */
    public record Geur(String id, GeurSoort soort, int rang, String icoon) {
        public Component naam() {
            return Component.translatable("gui.guhs.snuffel.geur." + id);
        }

        public String tekstKey() {
            return "gui.guhs.snuffel.geur." + id + ".tekst";
        }
    }

    private static final Map<String, Geur> ALLE = new LinkedHashMap<>();
    private static final List<BiConsumer<ServerPlayer, Geur>> LUISTERAARS = new CopyOnWriteArrayList<>();

    static {
        for (Eiland.GeurDef d : Eiland.opzet().geuren()) {
            GeurSoort soort = GeurSoort.vanId(d.soort());
            if (soort == null) {
                LOG.error("Snuffeleiland: scent {} has an unknown kind {}", d.id(), d.soort());
            } else {
                registreer(d.id(), soort, d.rang(), d.icoon());
            }
        }
    }

    private Geuren() {
    }

    /** Registers a scent (a second registration of the same id replaces the first: the island's data may be refined in code). */
    public static synchronized Geur registreer(String id, GeurSoort soort, int rang, String icoon) {
        Geur g = new Geur(id, soort, Math.max(1, Math.min(Rang.values().length, rang)), icoon);
        ALLE.put(id, g);
        return g;
    }

    @Nullable
    public static synchronized Geur van(@Nullable String id) {
        return id == null ? null : ALLE.get(id);
    }

    public static synchronized List<Geur> alle() {
        return List.copyOf(ALLE.values());
    }

    // --- per player ---------------------------------------------------------------------------------------------------------

    /** The ids of the scents this player has learned, in the order they were learned. */
    public static List<String> geleerd(ServerPlayer p) {
        return SnuffelData.lijst(p, "Geuren");
    }

    public static boolean kent(ServerPlayer p, String id) {
        return SnuffelData.bevat(p, "Geuren", id);
    }

    public static int aantal(ServerPlayer p) {
        return geleerd(p).size();
    }

    /** The scents this player has learned that still exist. */
    public static List<Geur> boekje(ServerPlayer p) {
        List<Geur> uit = new ArrayList<>();
        for (String id : geleerd(p)) {
            Geur g = van(id);
            if (g != null) {
                uit.add(g);
            }
        }
        return uit;
    }

    /** May this player's nose smell this scent (their rank is high enough)? */
    public static boolean ruikt(ServerPlayer p, Geur g) {
        return g.rang() <= Rang.van(p).nummer();
    }

    /**
     * The player learns this scent: it is written into the snuffelboekje, with a line on the screen and a sound. True the
     * first time only (an unknown scent, or one the player already knows: false and nothing happens).
     */
    public static boolean leer(ServerPlayer p, String id) {
        Geur g = van(id);
        if (g == null) {
            LOG.warn("Snuffeleiland: unknown scent {}", id);
            return false;
        }
        Rang voor = Rang.van(p);
        if (!SnuffelData.voegToe(p, "Geuren", id)) {
            return false;
        }
        int n = aantal(p);
        p.sendSystemMessage(Component.translatable("gui.guhs.snuffel.geur_geleerd", g.naam().copy().withColor(g.soort().kleur() & 0xFFFFFF),
                g.soort().naam(), n).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().playSound(null, p.blockPosition(), SnuffelFeature.GELEERD_GELUID.get(), SoundSource.PLAYERS, 0.8f, 1f);
        GuhAdvancements.grant(p, "snuffel_eerste_geur");
        Rang na = Rang.van(p);
        if (na != voor) {
            p.sendSystemMessage(Component.translatable("gui.guhs.snuffel.rang.omhoog", na.regel()).withStyle(ChatFormatting.GOLD));
        }
        Stand.stuur(p);
        for (BiConsumer<ServerPlayer, Geur> l : LUISTERAARS) {
            l.accept(p, g);
        }
        return true;
    }

    /** Hears every scent a player learns. */
    public static void opGeleerd(BiConsumer<ServerPlayer, Geur> l) {
        LUISTERAARS.add(l);
    }

    /** (Dev, tests) forgets a scent. */
    public static boolean vergeet(ServerPlayer p, String id) {
        boolean weg = SnuffelData.haalWeg(p, "Geuren", id);
        if (weg) {
            Stand.stuur(p);
        }
        return weg;
    }
}
