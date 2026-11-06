package nl.juiced.guhs.feature.vadsrijk.klassen.spec;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.klassen.Advancement;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;

/**
 * U-SPECS: the spec rules.
 * <ul>
 *   <li>{@link #spec} / {@link #heeft}: which spec a player has in a class, and "spec X at advancement step &gt;= n" for the
 *   effects (works on server AND client data: it only reads the synced tree nodes of the ACTIVE class).</li>
 *   <li>{@link #open}: the first choice is open (the step-1 ceremony asked for it, or a migrated save): free, anywhere.</li>
 *   <li>{@link #kies}: the first choice (with the step-1 ceremony when that step isn't done yet).</li>
 *   <li>{@link #wissel}: switch to the other spec: at a class NPC (the desk session), at most once per 10 minutes, never in a
 *   fight, a boss arena or a dungeon. Only the tree nodes that no longer fit (the old spec branch) are refunded.</li>
 * </ul>
 * Per-player data in {@code SpelerData.extra("klassen")}: {@code spec_open_<klasse>} (bool), {@code spec_wissel} (millis of the
 * last switch), {@code spec_v} (migration version).
 */
public final class Specs {
    /** 10 minutes real time between two spec switches. */
    public static final long WISSEL_MS = 10L * 60L * 1000L;
    /** "In a fight": you hit or got hit within the last 10 s. */
    static final int GEVECHT_TICKS = 200;
    static final Map<UUID, Long> GEVECHT = new ConcurrentHashMap<>();
    /** Tests: pretend the player stands at a class NPC. */
    public static final Set<UUID> TEST_BALIE = ConcurrentHashMap.newKeySet();

    private Specs() {
    }

    /** The spec of this player in a class (null = none yet). Server or client data alike. */
    @Nullable
    public static Spec spec(Player p, String klasse) {
        return Spec.uit(klasse, Spelers.data(p).boom.getOrDefault(klasse, Set.of()));
    }

    /** The spec of the active class. */
    @Nullable
    public static Spec spec(Player p) {
        return spec(p, Spelers.klasse(p).id());
    }

    /** Is the ACTIVE class of this player spec {@code s}, at advancement step {@code stap} or higher? */
    public static boolean heeft(Player p, Spec s, int stap) {
        if (!Spelers.klasse(p).id().equals(s.klasse) || !Spelers.heeftKnoop(p, s.knoop())) {
            return false;
        }
        return stap <= 0 || Advancement.stap(p, s.klasse) >= stap;
    }

    /** May this player make the first spec choice of this class now (asked by the step-1 ceremony or a migration)? */
    public static boolean open(Player p, String klasse) {
        return spec(p, klasse) == null && Spelers.data(p).extra("klassen").getBooleanOr("spec_open_" + klasse, false);
    }

    public static void zetOpen(SpelerData d, String klasse, boolean aan) {
        if (aan) {
            d.extra("klassen").putBoolean("spec_open_" + klasse, true);
        } else {
            d.extra("klassen").remove("spec_open_" + klasse);
        }
    }

    /** The first choice of a spec. Returns null when it worked, else the reason (lang suffix). Does the step-1 ceremony. */
    @Nullable
    public static String kies(ServerPlayer p, Spec s) {
        if (spec(p, s.klasse) != null) {
            return "al";
        }
        if (!open(p, s.klasse)) {
            return "nog_niet";
        }
        SpelerData d = Spelers.data(p);
        Boom.knopen(d, s.klasse).add(s.knoop());
        zetOpen(d, s.klasse, false);
        if (Advancement.stap(p, s.klasse) == 0) {
            Advancement.voltooi(p, s.klasse, 1);   // the ceremony of step 1 with the spec's title
        } else {
            Spelers.sync(p);
            Advancement.ceremonie(p, s.klasse, Advancement.stap(p, s.klasse));   // (a migrated save: the title of its step)
        }
        return null;
    }

    /** Why a switch to the other spec can't happen now (lang suffix), or null. */
    @Nullable
    public static String waaromGeenWissel(ServerPlayer p, String klasse, boolean aanBalie, long nuMs) {
        Spec nu = spec(p, klasse);
        if (nu == null) {
            return "geen";
        }
        if (!aanBalie && !TEST_BALIE.contains(p.getUUID())) {
            return "balie";
        }
        if (inGevecht(p)) {
            return "gevecht";
        }
        if (Arenas.op(p.getX(), p.getY(), p.getZ()) != null) {
            return "arena";
        }
        if (wisselOver(p, nuMs) > 0) {
            return "wacht";
        }
        return null;
    }

    /** Seconds until the next spec switch is allowed (0 = now). */
    public static long wisselOver(Player p, long nuMs) {
        long laatst = Spelers.data(p).extra("klassen").getLongOr("spec_wissel", 0L);
        return laatst <= 0 ? 0 : Math.max(0, (laatst + WISSEL_MS - nuMs + 999) / 1000);
    }

    /** Switch to the other spec. Returns null when it worked, else the reason. */
    @Nullable
    public static String wissel(ServerPlayer p, String klasse, boolean aanBalie, long nuMs) {
        String nee = waaromGeenWissel(p, klasse, aanBalie, nuMs);
        if (nee != null) {
            return nee;
        }
        Spec oud = spec(p, klasse);
        SpelerData d = Spelers.data(p);
        Set<String> heeft = Boom.knopen(d, klasse);
        heeft.remove(oud.knoop());
        heeft.add(oud.ander().knoop());
        snoei(Klassen.van(klasse), heeft);
        d.extra("klassen").putLong("spec_wissel", nuMs);
        Spelers.sync(p);
        return null;
    }

    /** Removes every bought node whose requirements are no longer met (repeat until stable): the old spec branch. */
    static void snoei(@Nullable Klasse k, Set<String> heeft) {
        if (k == null) {
            return;
        }
        boolean weg = true;
        while (weg) {
            weg = false;
            for (BoomKnoop b : k.boom()) {
                if (b.rij() < 0 || !heeft.contains(b.id())) {
                    continue;
                }
                for (String v : b.vereist()) {
                    boolean een = false;
                    for (String alt : v.split("\\|")) {
                        een |= heeft.contains(alt);
                    }
                    if (!een) {
                        heeft.remove(b.id());
                        weg = true;
                        break;
                    }
                }
            }
        }
    }

    static void gevochten(ServerPlayer p) {
        GEVECHT.put(p.getUUID(), p.level().getGameTime());
    }

    public static boolean inGevecht(ServerPlayer p) {
        Long t = GEVECHT.get(p.getUUID());
        return t != null && p.level().getGameTime() - t < GEVECHT_TICKS;
    }

    /** Dev/tests. */
    public static void zetGevecht(ServerPlayer p, boolean aan) {
        if (aan) {
            gevochten(p);
        } else {
            GEVECHT.remove(p.getUUID());
        }
    }

    // ---- migration -----------------------------------------------------------------------------------------------------------

    /** The version of the save layout (1 = the old lv 10/20/30/50 model, 2 = specs). */
    public static final int VERSIE = 2;

    /**
     * Old model -> new model, once per save (see NOTITIES_U_SPECS.md, "Migratie"). Per class:
     * <ul>
     *   <li>old steps (adv1 lv 10, adv2 lv 20, adv3 lv 30, adv4 lv 50) -> new steps (1 at lv 17, 2 at lv 35, 3 at lv 58):
     *   old 2+ = new 1, old 3+ = new 2 (nobody had the new step 3 yet); old 1 only = new 1 when the class is lv 17+, else
     *   nothing (its quest comes at 17);</li>
     *   <li>new step 1+ without a spec: the spec choice is open (free, anywhere: the K screen shows the button);</li>
     *   <li>the tree was re-laid: every bought node is refunded once (all points back, the advancements stay).</li>
     * </ul>
     * Returns true when something changed.
     */
    public static boolean migreer(SpelerData d) {
        CompoundTag x = d.extra("klassen");
        if (x.getIntOr("spec_v", 1) >= VERSIE) {
            return false;
        }
        x.putInt("spec_v", VERSIE);
        boolean iets = false;
        for (Klasse k : Klassen.alle()) {
            Set<String> heeft = Boom.knopen(d, k.id());
            String pre = Spec.prefix(k.id());
            int oud = 0;
            for (int s = 4; s >= 1; s--) {
                if (heeft.contains(pre + "_adv" + s)) {
                    oud = s;
                    break;
                }
            }
            int nieuw = oud >= 3 ? 2 : oud >= 2 ? 1 : oud == 1 && d.level(k.id()) >= Advancement.nodigLevel(0) ? 1 : 0;
            int voor = heeft.size();
            heeft.removeIf(id -> {
                BoomKnoop b = Boom.knoop(k, id);
                return b == null || b.rij() >= 0 || id.startsWith(pre + "_adv");
            });
            for (int s = 1; s <= nieuw; s++) {
                heeft.add(pre + "_adv" + s);
            }
            if (nieuw >= 1 && Spec.uit(k.id(), heeft) == null) {
                zetOpen(d, k.id(), true);
            }
            iets |= oud > 0 || voor != heeft.size();
        }
        return iets;
    }
}
