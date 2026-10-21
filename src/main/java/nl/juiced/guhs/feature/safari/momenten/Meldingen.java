package nl.juiced.guhs.feature.safari.momenten;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The message budget of the park's moments (test round 3, design ronde 43): park-wide at most one chat line per
 * {@link #BUDGET} ticks (2.5 real minutes). Every moment is offered {@link MomentSchema#VOORUIT} ticks before it begins;
 * what comes up in between waits and goes out together in one line ("Straks in het park: voedertijd bij de pinguïns
 * (08:30), de pinguïnparade (09:00) en ..."). A waiting notice that is out of date
 * (its moment began) is dropped, so loading a world never sends a burst of old moments: only what is still to come.
 * Pure logic on a tick counter (the level's game time), tested in {@code SafariBelevingGameTests}.
 */
public final class Meldingen {
    public static final int BUDGET = 3000;
    /** A summary names at most this many moments (the rest: "en nog N"). */
    static final int MAX_IN_ZIN = 3;

    /** A waiting notice: what is coming (a short part of the line), until when it is worth saying. */
    record Melding(String sleutel, Component kort, long tot) {
    }

    private final List<Melding> wacht = new ArrayList<>();
    private final Set<String> gezien = new HashSet<>();
    private long vrijOp = Long.MIN_VALUE;

    /** Offers a notice (once per key); it waits for the budget and is dropped at {@code tot} (game time). */
    public boolean bied(String sleutel, Component kort, long tot) {
        if (!gezien.add(sleutel)) {
            return false;
        }
        if (gezien.size() > 1024) {
            gezien.clear();
            gezien.add(sleutel);
        }
        wacht.add(new Melding(sleutel, kort, tot));
        return true;
    }

    /** The line to send now (null: nothing, or the budget is not free yet). */
    @Nullable
    public Component volgende(long nu) {
        wacht.removeIf(m -> m.tot() <= nu);
        if (wacht.isEmpty() || nu < vrijOp) {
            return null;
        }
        vrijOp = nu + BUDGET;
        wacht.sort((a, b) -> Long.compare(a.tot(), b.tot()));
        MutableComponent lijst = Component.empty();
        int n = Math.min(MAX_IN_ZIN, wacht.size());
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                lijst.append(Component.translatable(i == n - 1 && wacht.size() <= MAX_IN_ZIN ? "gui.guhs.safari.melding.en" : "gui.guhs.safari.melding.komma"));
            }
            lijst.append(wacht.get(i).kort());
        }
        if (wacht.size() > MAX_IN_ZIN) {
            lijst.append(Component.translatable("gui.guhs.safari.melding.meer", wacht.size() - MAX_IN_ZIN));
        }
        wacht.clear();
        return Component.translatable("gui.guhs.safari.melding.samen", lijst).withStyle(ChatFormatting.YELLOW);
    }

    /** How many notices wait (tests). */
    int wachtend() {
        return wacht.size();
    }
}
