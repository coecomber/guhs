package nl.juiced.guhs.feature.snuffel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Good deeds: what a dog does for the island's residents (bring back what somebody lost...). A deed has an id; its name
 * is the lang key {@code gui.guhs.snuffel.daad.<id>}. Each deed counts once per player ({@link SnuffelData}, key
 * {@code Daden}), teaches a scent when it has one, and makes the companion's tree grow one step with the growth scene
 * ({@link Boom#groei}; a fifth deed finds the tree full-grown and simply counts).
 */
public final class Daden {
    private static final List<BiConsumer<ServerPlayer, String>> LUISTERAARS = new CopyOnWriteArrayList<>();

    private Daden() {
    }

    public static List<String> van(ServerPlayer p) {
        return SnuffelData.lijst(p, "Daden");
    }

    public static boolean heeft(ServerPlayer p, String daad) {
        return SnuffelData.bevat(p, "Daden", daad);
    }

    public static int aantal(ServerPlayer p) {
        return van(p).size();
    }

    /**
     * The player did this good deed: it is written down, its scent is learned ({@code geur} may be null), the tree grows
     * a step and the growth scene plays; {@code daarna} as in {@link Boom#groei}. False (and nothing happens) when the
     * player had done it before.
     */
    public static boolean geef(ServerPlayer p, String daad, @Nullable String geur, @Nullable Consumer<ServerPlayer> daarna) {
        if (!SnuffelData.voegToe(p, "Daden", daad)) {
            return false;
        }
        p.sendSystemMessage(Component.translatable("gui.guhs.snuffel.daad_gedaan", Component.translatable("gui.guhs.snuffel.daad." + daad)
                .withStyle(ChatFormatting.WHITE), aantal(p)).withStyle(ChatFormatting.GREEN));
        if (geur != null) {
            Geuren.leer(p, geur);
        }
        for (BiConsumer<ServerPlayer, String> l : LUISTERAARS) {
            l.accept(p, daad);
        }
        if (!Boom.groei(p, daarna)) {
            Stand.stuur(p);   // (the tree is full-grown: only the list of deeds changed)
        }
        return true;
    }

    public static boolean geef(ServerPlayer p, String daad, @Nullable String geur) {
        return geef(p, daad, geur, null);
    }

    /** Hears every good deed (before the tree grows). */
    public static void opDaad(BiConsumer<ServerPlayer, String> l) {
        LUISTERAARS.add(l);
    }

    /** (Dev, tests) undoes a deed (the tree stays as it is). */
    public static boolean vergeet(ServerPlayer p, String daad) {
        boolean weg = SnuffelData.haalWeg(p, "Daden", daad);
        if (weg) {
            Stand.stuur(p);
        }
        return weg;
    }
}
