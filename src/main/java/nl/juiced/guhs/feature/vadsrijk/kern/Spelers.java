package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;

/** The player API of Het Vadsrijk (CONTRACT_VADSRIJK 3): data, level, stats, xp, coins, HUD sync. Server side. */
public final class Spelers {
    /** Fist damage without a class weapon. */
    public static final double VUIST = 2;
    /**
     * Extra stat sources (slices: set bonuses, skill-tree bonuses, buffs...): each is asked in {@link #stats} and added on top.
     * Only called for server players; return an empty {@link Stats} when it doesn't apply.
     */
    public static final java.util.List<java.util.function.Function<net.minecraft.server.level.ServerPlayer, Stats>> EXTRA_STATS =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private Spelers() {
    }

    public static SpelerData data(Player p) {
        return p.getData(KernRegistry.SPELER);
    }

    /**
     * The title on the HUD instead of the class name (klassen: job advancement titles): the first non-null lang key wins.
     */
    public static final java.util.List<java.util.function.Function<ServerPlayer, String>> TITEL_BRON = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** The lang key of the title the HUD shows: {@link #TITEL_BRON} or the class name. */
    public static String titelKey(ServerPlayer p) {
        for (var f : TITEL_BRON) {
            String t = f.apply(p);
            if (t != null) {
                return t;
            }
        }
        return klasse(p).titelKey(level(p));
    }

    /**
     * After changing {@link SpelerData} (e.g. {@code extra(slice)}): nothing to do for saving (the attachment is written with the
     * player), but it re-sends the HUD. Kept so callers can mark "changed" in one place.
     */
    public static void opgeslagen(ServerPlayer p) {
        sync(p);
    }

    public static Klasse klasse(Player p) {
        return Klassen.ofRidder(data(p).klasse);
    }

    public static int level(Player p) {
        return data(p).level();
    }

    public static int level(Player p, String klasse) {
        return data(p).level(klasse);
    }

    /** The class weapon in the main hand (a piece of gear of a weapon base type this class accepts), or null. */
    @Nullable
    public static Uitrusting wapen(Player p) {
        Uitrusting u = Rollen.van(p.getMainHandItem());
        if (u == null) {
            return null;
        }
        BasisType t = u.basisType();
        if (t == null || t.slot() != Slot.WAPEN) {
            return null;
        }
        Klasse k = klasse(p);
        if (t.klasse() != null && !t.klasse().equals(k.id())) {
            return null;
        }
        return u.level() <= level(p) ? u : null;
    }

    public static double wapenSchade(Player p) {
        Uitrusting u = wapen(p);
        return u == null ? VUIST + 0.5 * level(p) : Rollen.wapenSchade(u);
    }

    public static Element wapenElement(Player p) {
        Uitrusting u = wapen(p);
        return u == null ? Element.GEEN : u.element();
    }

    /** Everything that counts now: class growth + gear (level requirement met) + tree + statuses. */
    public static Stats stats(Player p) {
        SpelerData d = data(p);
        Klasse k = klasse(p);
        int lvl = d.level(k.id());
        Stats s = k.basis(lvl).kopie();
        Uitrusting w = wapen(p);
        if (w != null) {
            s.plus(Rollen.stats(w));
        }
        for (EquipmentSlot es : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getItemBySlot(es);
            Uitrusting u = Rollen.van(st);
            if (u != null && u.basisType() != null && u.basisType().slot().vanilla == es && u.level() <= lvl) {
                s.plus(Rollen.stats(u));
            }
        }
        if (p instanceof ServerPlayer sp) {
            for (nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot slot : nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.SIERADEN) {
                Uitrusting u = Rollen.van(Sieraden.get(sp, slot));
                if (u != null && u.basisType() != null && u.basisType().slot() == slot && u.level() <= lvl) {
                    s.plus(Rollen.stats(u));
                }
            }
            for (java.util.function.Function<ServerPlayer, Stats> f : EXTRA_STATS) {
                Stats extra = f.apply(sp);
                if (extra != null) {
                    s.plus(extra);
                }
            }
        }
        Set<String> knopen = d.boom.getOrDefault(k.id(), Set.of());
        for (BoomKnoop b : k.boom()) {
            if (knopen.contains(b.id()) && b.bonus() != null) {
                s.plus(b.bonus());
            }
        }
        if (Statussen.heeft(p, Status.DUIZELIG)) {
            for (Stat st : Stat.values()) {
                if (st.hoofd() && st != Stat.VADS) {
                    s.zet(st, s.get(st) * 0.75);
                }
            }
        }
        if (Statussen.heeft(p, Status.SNELPOOTJES)) {
            s.plus(Stat.LOOPSNELHEID, 25);
        }
        if (Statussen.heeft(p, Status.PLAKKERIG)) {
            s.plus(Stat.LOOPSNELHEID, -35);
        }
        return s;
    }

    public static double maxVads(Player p) {
        return Math.max(1, stats(p).get(Stat.VADS));
    }

    /** The current Vads (fills up the first time). */
    public static double vads(Player p) {
        SpelerData d = data(p);
        double max = maxVads(p);
        if (d.vads < 0 || d.vads > max) {
            d.vads = max;
        }
        return d.vads;
    }

    public static double maxEnergie(Player p) {
        return klasse(p).maxEnergie(level(p));
    }

    public static double energie(Player p) {
        SpelerData d = data(p);
        double max = maxEnergie(p);
        if (d.energie < 0 || d.energie > max) {
            d.energie = max;
        }
        return d.energie;
    }

    /** Ult charge 0..100 from damage dealt (1 % per 2 % of your max Vads in damage). */
    public static void laadUlt(ServerPlayer p, double schade) {
        SpelerData d = data(p);
        if (schade > 0 && d.ultLading < 100) {
            d.ultLading = Math.min(100, d.ultLading + 50.0 * schade / maxVads(p));
        }
    }

    public static boolean heeftKnoop(Player p, String knoop) {
        return data(p).boom.getOrDefault(klasse(p).id(), Set.of()).contains(knoop);
    }

    public static int skillpunten(Player p, String klasse) {
        Klasse k = Klassen.van(klasse);
        int besteed = 0;
        if (k != null) {
            Set<String> knopen = data(p).boom.getOrDefault(klasse, Set.of());
            for (BoomKnoop b : k.boom()) {
                if (knopen.contains(b.id())) {
                    besteed += b.kosten();
                }
            }
        }
        return Levels.skillpunten(level(p, klasse)) - besteed;
    }

    // ---- xp -------------------------------------------------------------------------------------------------------------

    /** Xp for this player's current class only. */
    /** BELEVING hook (fix round 1): xp factors per player (the Vrolijke reeks gives +10 %), all multiplied. */
    public static final List<java.util.function.ToDoubleFunction<ServerPlayer>> XP_FACTOR = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void geefXp(ServerPlayer p, long n, String bron) {
        if (n <= 0) {
            return;
        }
        double factor = 1;
        for (java.util.function.ToDoubleFunction<ServerPlayer> f : XP_FACTOR) {
            factor *= f.applyAsDouble(p);
        }
        n = Math.round(n * factor);
        SpelerData d = data(p);
        String k = d.klasse;
        int voor = d.level(k);
        long max = Levels.totaalVoor(Levels.MAX);
        d.xp.put(k, Math.min(max, d.xp(k) + n));
        int na = d.level(k);
        if (na > voor) {
            levelOp(p, k, na);
        }
        sync(p);
    }

    /** Xp from a defeat: the player and every party member within 48 blocks in the dimension get it in full. */
    public static void geefXpGedeeld(ServerPlayer p, long n, String bron) {
        List<ServerPlayer> leden = Partijen.ledenInBuurt(p, 48);
        for (ServerPlayer l : leden) {
            geefXp(l, n, bron);
        }
    }

    public static void zetLevel(ServerPlayer p, int level) {
        SpelerData d = data(p);
        int voor = d.level();
        d.xp.put(d.klasse, Levels.totaalVoor(Math.max(1, Math.min(Levels.MAX, level))));
        d.vads = maxVads(p);
        if (d.level() > voor) {
            levelOp(p, d.klasse, d.level());
        }
        sync(p);
    }

    private static void levelOp(ServerPlayer p, String klasse, int level) {
        SpelerData d = data(p);
        d.vads = maxVads(p);
        d.energie = maxEnergie(p);
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1.0, 0.6, 0.3);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() + 2, p.getZ(), 40, 1.2, 0.6, 1.2, 0.05);
            sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.0f);
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.8f);
        }
        nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes.titel(p, Component.translatable("vadsrijk.kern.levelop.titel").withStyle(ChatFormatting.GOLD),
                Component.translatable("vadsrijk.kern.levelop.onder", Component.translatable(Klassen.ofRidder(klasse).naamKey()), level), 50, 0xFFD24A);
        Tips.toon(p, "vadsrijk.kern.tip.levelop");
        Haken.LEVEL_OP.forEach(h -> h.accept(p, klasse + ":" + level));
    }

    // ---- coins ----------------------------------------------------------------------------------------------------------

    public static void geefMunten(ServerPlayer p, long brons) {
        data(p).munten = Math.max(0, data(p).munten + brons);
        sync(p);
    }

    public static boolean neemMunten(ServerPlayer p, long brons) {
        SpelerData d = data(p);
        if (d.munten < brons) {
            return false;
        }
        d.munten -= brons;
        sync(p);
        return true;
    }

    /** "3g 12z 40b" style. */
    public static String munten(long brons) {
        long g = brons / 10000, z = brons / 100 % 100, b = brons % 100;
        return (g > 0 ? g + "g " : "") + (g > 0 || z > 0 ? z + "z " : "") + b + "b";
    }

    // ---- HUD ------------------------------------------------------------------------------------------------------------

    /** Sends the HUD values to the player (cheap; call after changes, kern also sends every 5 ticks). */
    public static void sync(ServerPlayer p) {
        if (Vadsrijk.in(p)) {
            KernPayloads.stuurHud(p);
        }
    }
}
