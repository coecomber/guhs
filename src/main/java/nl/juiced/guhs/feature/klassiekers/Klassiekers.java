package nl.juiced.guhs.feature.klassiekers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.Scorebord;

/**
 * The difficulty levels of the five klassiekers (2.9, De Grote Guhspelen): beauty, meppen, golf, smul and vissen. What
 * they share lives here: how a screen button carries a level ({@link #metNiveau} / {@link #niveau} / {@link #actie}), the
 * personal records per level (saved keys via {@link Niveau#board}), the one floating scoreboard with all three levels
 * ({@link #bord}), and the advancements of the tab "De Grote Guhspelen" (lastig done per game, the Klassiekers-kampioen).
 * <p>
 * Each game keeps its own knobs (time, speed, Mika's, wind...) in its own package; the game as it was before 2.9 is
 * {@link Niveau#MEDIUM}, and a plain action (without a level) still means medium.
 */
public final class Klassiekers {
    /** The five games, in the order of the Guhdex (also the advancement ids: klassiekers_&lt;spel&gt;_lastig). */
    public static final List<String> SPELLEN = List.of("beauty", "meppen", "golf", "smul", "vissen");
    /** A screen action with a level: action + STAP * (level + 1). A plain action (below STAP) is medium. */
    public static final int STAP = 1000;
    /** Advancements (tab grote_guhspelen). */
    public static final String ADV_MAKKELIJK = "grote_guhspelen/klassiekers_makkelijk", ADV_MEDIUM = "grote_guhspelen/klassiekers_medium",
            ADV_KAMPIOEN = "grote_guhspelen/klassiekers_kampioen";

    /** The action a screen sends for this button on this level. */
    public static int metNiveau(int action, Niveau niveau) {
        return action + STAP * (niveau.ordinal() + 1);
    }

    /** The level of an action from a screen (medium when it has none: the old screens and the tests). */
    public static Niveau niveau(int action) {
        return action < STAP ? Niveau.MEDIUM : Niveau.of(action / STAP - 1);
    }

    /** The action itself, without its level. */
    public static int actie(int action) {
        return action < 0 ? action : action % STAP;
    }

    /** The saved key of a personal record on this level (medium keeps the old key, so old records stay medium). */
    public static String sleutel(String base, Niveau niveau) {
        return niveau.board(base);
    }

    /** Puts the personal best of every level in a screen's data: Best_makkelijk, Best_medium, Best_lastig. */
    public static void records(CompoundTag data, ToIntFunction<Niveau> best) {
        for (Niveau n : Niveau.values()) {
            data.putInt("Best_" + n.id(), best.applyAsInt(n));
        }
    }

    /**
     * One floating scoreboard with the top 3 of all three levels (base = the medium board, the existing one): a title,
     * and per level a heading ("Makkelijk", or "Makkelijk: " + extra) and its top 3.
     */
    public static Component bord(MinecraftServer server, Component title, String base, IntFunction<String> format) {
        return bord(server, title, base, null, format);
    }

    /** As {@link #bord(MinecraftServer, Component, String, IntFunction)}, with a line after each level name (e.g. "9 holes, par 26"). */
    public static Component bord(MinecraftServer server, Component title, String base, @javax.annotation.Nullable IntFunction<Component> extra,
                                 IntFunction<String> format) {
        List<String> boards = new ArrayList<>();
        List<Component> headings = new ArrayList<>();
        for (Niveau n : Niveau.values()) {
            boards.add(n.board(base));
            // (a child with its own colour: Scorebord.text colours the heading itself pink)
            MutableComponent heading = Component.empty().append(Component.literal("~ ").append(n.naam()).append(" ~").withStyle(kleur(n), ChatFormatting.BOLD));
            if (extra != null) {
                heading.append(Component.literal(" ").append(extra.apply(n.ordinal())).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            headings.add(heading);
        }
        return Scorebord.text(server, title, boards, headings, format);
    }

    /** The colour of a level: green, yellow, red. */
    public static ChatFormatting kleur(Niveau niveau) {
        return switch (niveau) {
            case MAKKELIJK -> ChatFormatting.GREEN;
            case MEDIUM -> ChatFormatting.YELLOW;
            case LASTIG -> ChatFormatting.RED;
        };
    }

    /** "Makkelijk" / "Medium" / "Lastig" in its colour (for the chat). */
    public static MutableComponent naam(Niveau niveau) {
        return niveau.naam().copy().withStyle(kleur(niveau));
    }

    /**
     * A game of one of the klassiekers was played to the end for real (not just standing there): the advancements of the
     * tab "De Grote Guhspelen". Makkelijk / medium / lastig each have one; lastig per game, and all five lastig =
     * Klassiekers-kampioen.
     */
    public static void gespeeld(ServerPlayer player, String spel, Niveau niveau) {
        switch (niveau) {
            case MAKKELIJK -> grant(player, ADV_MAKKELIJK);
            case MEDIUM -> grant(player, ADV_MEDIUM);
            case LASTIG -> {
                grant(player, lastigAdvancement(spel));
                if (SPELLEN.stream().allMatch(s -> done(player, lastigAdvancement(s)))) {
                    grant(player, ADV_KAMPIOEN);
                }
            }
        }
    }

    public static String lastigAdvancement(String spel) {
        return "grote_guhspelen/klassiekers_" + spel.toLowerCase(Locale.ROOT) + "_lastig";
    }

    /** Grants an advancement of the mod (every criterion of it) by its path, e.g. grote_guhspelen/klassiekers_golf_lastig. */
    public static void grant(ServerPlayer player, String path) {
        AdvancementHolder holder = player.server.getAdvancements().get(Guhs.id(path));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            for (String criterion : holder.value().criteria().keySet()) {
                player.getAdvancements().award(holder, criterion);
            }
        }
    }

    public static boolean done(ServerPlayer player, String path) {
        AdvancementHolder holder = player.server.getAdvancements().get(Guhs.id(path));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private Klassiekers() {
    }
}
