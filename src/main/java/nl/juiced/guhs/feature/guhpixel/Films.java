package nl.juiced.guhs.feature.guhpixel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Which films a player may show in the Guhbioscoop: the interface between the games and the cinema. A film is unlocked by
 * a stored flag ({@link #ontgrendel}; {@link Grappen#voltooi} does it for the six joke games) or follows existing progress
 * ({@link #voorwaarde}: the three stories, registered by the bioscoop slice). A later film = one JSON, one {@link #IDS}
 * entry and one unlock rule.
 */
public final class Films {
    /** Every film, in the order of the projector's list. */
    public static final List<String> IDS = new ArrayList<>(List.of("skyblok", "bedwars", "vadsnite", "guhmon", "bzg", "among",
            "balto", "mewtwo", "stitch626"));
    private static final Map<String, Predicate<ServerPlayer>> VOORWAARDEN = new HashMap<>();
    private static final String DATA = "Films";

    /** True when new (the player is told "Nieuwe film in de Guhbioscoop"). */
    public static boolean ontgrendel(ServerPlayer p, String filmId) {
        CompoundTag films = PxData.sub(Muntjes.data(p), DATA);
        if (films.getBooleanOr(filmId, false)) {
            return false;
        }
        films.putBoolean(filmId, true);
        PxData.vuil(p.level().getServer());
        p.sendSystemMessage(Component.translatable("gui.guhs.guhpixel.film.nieuw").withStyle(ChatFormatting.GOLD));
        return true;
    }

    public static boolean heeft(ServerPlayer p, String filmId) {
        if (Muntjes.data(p).getCompoundOrEmpty(DATA).getBooleanOr(filmId, false)) {
            return true;
        }
        Predicate<ServerPlayer> v = VOORWAARDEN.get(filmId);
        return v != null && v.test(p);
    }

    /** A film that follows existing progress instead of a stored flag. */
    public static void voorwaarde(String filmId, Predicate<ServerPlayer> p) {
        VOORWAARDEN.put(filmId, p);
    }

    /** How many films this player has. */
    public static int aantal(ServerPlayer p) {
        int n = 0;
        for (String id : IDS) {
            if (heeft(p, id)) {
                n++;
            }
        }
        return n;
    }

    /** (Dev) forgets the stored flags. */
    public static void wis(ServerPlayer p) {
        Muntjes.data(p).remove(DATA);
        PxData.vuil(p.level().getServer());
    }

    private Films() {
    }
}
