package nl.juiced.guhs.feature.snuffel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * A sniffing exam: a list of scent sources that the player has to find, in any order, without a clock (failing does not
 * exist: an exam simply lasts until everything is found). An exam is registered once ({@link #registreer}, in a feature's
 * {@code register}) with the ids of its sources and what happens when a player passes; its sources can only be smelled
 * by a player who is taking that exam (the exam takes the {@link Geurbronnen#voorwaarde condition} of those ids).
 * <p>
 * {@link #start} begins it for a player: the sources are fresh again for that player's nose, the screen shows
 * "Examen: 0 / n". The progress is kept per player ({@link SnuffelData}, key {@code Examen}) and survives a logout; the
 * name of the exam on the screen is the lang key {@code gui.guhs.snuffel.examen.<id>}.
 */
public final class Examen {
    public record Def(String id, List<String> bronnen, Consumer<ServerPlayer> geslaagd) {
    }

    /** An exam a player is taking, and how many of its sources they found. */
    public record Loop(Def examen, int gevonden) {
    }

    private static final Map<String, Def> ALLE = new ConcurrentHashMap<>();

    static {
        Geurbronnen.opVondst((p, bron) -> gevonden(p, bron.id()));
    }

    private Examen() {
    }

    static void init() {
        // (loads the class: its listener for finds must be there before the first exam is registered)
    }

    /** Registers an exam (a second registration of the same id replaces the first). */
    public static void registreer(String id, List<String> bronnen, Consumer<ServerPlayer> geslaagd) {
        Def def = new Def(id, List.copyOf(bronnen), geslaagd);
        ALLE.put(id, def);
        for (String bron : def.bronnen()) {
            Geurbronnen.voorwaarde(bron, p -> {
                Loop l = bezig(p);
                return l != null && l.examen().id().equals(id);
            });
        }
    }

    @Nullable
    public static Def van(String id) {
        return ALLE.get(id);
    }

    /** The exam this player is taking (null: none). */
    @Nullable
    public static Loop bezig(ServerPlayer p) {
        if (!SnuffelData.heeft(p)) {
            return null;
        }
        CompoundTag t = SnuffelData.van(p).getCompound("Examen").orElse(null);
        Def def = t == null ? null : ALLE.get(t.getStringOr("Id", ""));
        if (def == null) {
            return null;
        }
        int n = 0;
        for (String bron : def.bronnen()) {
            if (Geurbronnen.gevonden(p, bron)) {
                n++;
            }
        }
        return new Loop(def, n);
    }

    /** Did this player pass this exam? */
    public static boolean gehaald(ServerPlayer p, String id) {
        return SnuffelData.bevat(p, "Examens", id);
    }

    /** The exam begins for this player (an exam that was running is replaced). False: unknown exam. */
    public static boolean start(ServerPlayer p, String id) {
        Def def = ALLE.get(id);
        if (def == null) {
            return false;
        }
        for (String bron : def.bronnen()) {
            Geurbronnen.vergeet(p, bron);
        }
        CompoundTag t = new CompoundTag();
        t.putString("Id", id);
        SnuffelData.van(p).put("Examen", t);
        p.sendSystemMessage(Component.translatable("gui.guhs.snuffel.examen.start", Component.translatable("gui.guhs.snuffel.examen." + id),
                def.bronnen().size()).withStyle(ChatFormatting.GOLD));
        Stand.stuur(p);
        return true;
    }

    /** Stops the exam without a result (dev, or the story takes it back). */
    public static void stop(ServerPlayer p) {
        if (SnuffelData.heeft(p)) {
            SnuffelData.van(p).remove("Examen");
            Stand.stuur(p);
        }
    }

    private static void gevonden(ServerPlayer p, String bronId) {
        Loop l = bezig(p);
        if (l == null || !l.examen().bronnen().contains(bronId)) {
            return;
        }
        int totaal = l.examen().bronnen().size();
        if (l.gevonden() < totaal) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.snuffel.examen.voortgang", l.gevonden(), totaal).withStyle(ChatFormatting.GOLD));
            Stand.stuur(p);
            return;
        }
        SnuffelData.van(p).remove("Examen");
        SnuffelData.voegToe(p, "Examens", l.examen().id());
        p.sendSystemMessage(Component.translatable("gui.guhs.snuffel.examen.geslaagd", Component.translatable("gui.guhs.snuffel.examen." + l.examen().id()))
                .withStyle(ChatFormatting.GOLD));
        p.level().playSound(null, p.blockPosition(), SnuffelFeature.GELEERD_GELUID.get(), SoundSource.PLAYERS, 1f, 1.3f);
        Stand.stuur(p);
        l.examen().geslaagd().accept(p);
    }
}
