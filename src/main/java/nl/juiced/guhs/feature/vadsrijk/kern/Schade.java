package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;

/**
 * The damage pipeline of the dimension (CONTRACT_VADSRIJK 4). Everything that hurts goes through {@link #doe}:
 * <ol>
 *   <li>no PvP, no friendly fire, guhs are never hurt;</li>
 *   <li>attacker: weapon/skill base x main stat (Kracht for melee, Behendigheid for ranged, Wijsheid for magic: +2 % per point)
 *   x Vahoeg, crit (Kritiek% chance, x1.5 + Kritiekschade%), element ({@link Element#factor}, + Elementschade% when it has one);</li>
 *   <li>{@link #VOOR_KLAP} modifiers (blocks, reflects, boss shields);</li>
 *   <li>defender (players): Verdediging, level-relative ({@link #verdedigingFactor}: def / (def + 25 + 15 x attacker level), at
 *   most 60 % off), Elementweerstand%, then Knuffelschild absorbs. Hits of normal mobs (not bosses) first get
 *   {@link Balans#mobSchade()} (config guhs-vadsrijk-balans.toml);</li>
 *   <li>apply: players lose {@link SpelerData#vads} (0 = {@link Flauwvallen}), mobs {@link VadsrijkMob#krijgSchade}; lifesteal;
 *   a floating number; {@link #NA_KLAP}.</li>
 * </ol>
 * {@link Soort#WAAR} skips steps 2 and 4 (true damage: DoTs, falls).
 */
public final class Schade {
    public enum Soort {
        MELEE, AFSTAND, MAGIE, WAAR
    }

    /** One hit: who, on whom, base damage (weapon x skill factor, or the mob's hit), element, kind, can it crit, which skill. */
    public record Klap(@Nullable LivingEntity bron, LivingEntity doel, double basis, Element element, Soort soort, boolean kanKritiek, String skillId,
            boolean zekerKritiek) {
        /** The usual hit (crit only by chance). */
        public Klap(@Nullable LivingEntity bron, LivingEntity doel, double basis, Element element, Soort soort, boolean kanKritiek, String skillId) {
            this(bron, doel, basis, element, soort, kanKritiek, skillId, false);
        }
    }

    /** What came out (after everything). */
    public record Uitkomst(Klap klap, double schade, boolean kritiek, double elementFactor) {
    }

    /** A modifier between attacker and defender: return the new damage (e.g. a block returns 20 %). */
    @FunctionalInterface
    public interface Aanpasser {
        double pas(Klap k, double schade);
    }

    public static final List<Aanpasser> VOOR_KLAP = new CopyOnWriteArrayList<>();
    public static final List<Consumer<Uitkomst>> NA_KLAP = new CopyOnWriteArrayList<>();
    /** (healer, target, amount) after a heal. */
    public static final List<HealLuisteraar> NA_HEAL = new CopyOnWriteArrayList<>();

    @FunctionalInterface
    public interface HealLuisteraar {
        void heal(@Nullable LivingEntity bron, LivingEntity doel, double n);
    }

    private Schade() {
    }

    /** Pure maths of step 2 (also used by tests): base x main stat x crit x element. */
    public static double aanval(double basis, Stats s, Soort soort, boolean kritiek, Element element, @Nullable Element doelElement, boolean ontvadst) {
        double hoofd = switch (soort) {
            case MELEE -> s.get(Stat.KRACHT);
            case AFSTAND -> s.get(Stat.BEHENDIGHEID);
            case MAGIE -> s.get(Stat.WIJSHEID);
            case WAAR -> 0;
        };
        double n = basis * (1 + 0.02 * hoofd);
        if (kritiek) {
            n *= 1.5 + s.get(Stat.KRITIEKSCHADE) / 100.0;
        }
        double ef = Element.factor(element, doelElement, ontvadst);
        n *= ef;
        if (element != Element.GEEN) {
            n *= 1 + s.get(Stat.ELEMENTSCHADE) / 100.0;
        }
        return n;
    }

    /** At most this much of a hit is taken off by Verdediging (balans 2026-11). */
    public static final double MAX_VERDEDIGING = 0.60;

    /**
     * The share of a hit Verdediging takes off (0..{@link #MAX_VERDEDIGING}), level-relative (balans 2026-11):
     * def / (def + 25 + 15 x attacker level). 50 def takes 56 % off a level-1 hit (50 / 90), but only 22 % off a level-10 hit.
     */
    public static double verdedigingFactor(double def, int aanvallerLevel) {
        def = Math.max(0, def);
        return Math.min(MAX_VERDEDIGING, def / (def + 25.0 + 15.0 * Math.max(1, aanvallerLevel)));
    }

    /** Pure maths of step 4 (attacker level 1: tests, old callers). */
    public static double verdediging(double n, Stats s, Element element) {
        return verdediging(n, s, element, 1);
    }

    /** Pure maths of step 4: level-relative Verdediging, then Elementweerstand. */
    public static double verdediging(double n, Stats s, Element element, int aanvallerLevel) {
        n = n * (1 - verdedigingFactor(s.get(Stat.VERDEDIGING), aanvallerLevel));
        if (element != Element.GEEN) {
            n *= 1 - Math.min(75, s.get(Stat.ELEMENTWEERSTAND)) / 100.0;
        }
        return n;
    }

    /** The level the defender's Verdediging is measured against: a mob's level, else the defender's own level. */
    public static int aanvallerLevel(@Nullable LivingEntity bron, ServerPlayer doel) {
        if (bron instanceof VadsrijkMob m) {
            return m.mobLevel();
        }
        return Spelers.level(doel);
    }

    public static Uitkomst doe(Klap k) {
        LivingEntity doel = k.doel(), bron = k.bron();
        if (doel == null || doel.isRemoved() || !(doel.level() instanceof ServerLevel sl)) {
            return new Uitkomst(k, 0, false, 1);
        }
        if (doel instanceof nl.juiced.guhs.entity.GuhEntity || (bron instanceof ServerPlayer && doel instanceof ServerPlayer)
                || (bron instanceof VadsrijkMob && doel instanceof VadsrijkMob) || (doel instanceof VadsrijkMob m && m.geplofd())) {
            return new Uitkomst(k, 0, false, 1);
        }
        if (doel instanceof ServerPlayer dp && (dp.isCreative() || dp.isSpectator() || Flauwvallen.beschermd(dp) || nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes.bezig(dp))) {
            return new Uitkomst(k, 0, false, 1);
        }
        Element doelEl = doel instanceof VadsrijkMob m ? m.element() : Element.GEEN;
        boolean ontvadst = doel instanceof VadsrijkMob m && m.ontvadst();
        double n = k.basis();
        boolean krit = false;
        double ef = Element.factor(k.element(), doelEl, ontvadst);
        if (k.soort() != Soort.WAAR) {
            Stats s = bron instanceof ServerPlayer bp ? Spelers.stats(bp) : new Stats();
            krit = k.zekerKritiek() || (k.kanKritiek() && bron != null && bron.getRandom().nextDouble() * 100 < s.get(Stat.KRITIEK));
            n = aanval(n, s, k.soort(), krit, k.element(), doelEl, ontvadst);
            if (bron != null && Statussen.heeft(bron, Status.VAHOEG)) {
                n *= 1 + Math.max(0.1, Statussen.sterkte(bron, Status.VAHOEG));
            }
        }
        for (Aanpasser a : VOOR_KLAP) {
            n = a.pas(k, n);
        }
        if (doel instanceof ServerPlayer && bron instanceof VadsrijkMob && !(bron instanceof nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas)) {
            n *= Balans.mobSchade();   // (balans 2026-11) normal zone mobs + elites hit harder; bosses unchanged
        }
        if (doel instanceof ServerPlayer dp && k.soort() != Soort.WAAR) {
            n = verdediging(n, Spelers.stats(dp), k.element(), aanvallerLevel(bron, dp));
        }
        Statussen.Actief schild = Statussen.actief(doel, Status.KNUFFELSCHILD);
        if (schild != null && n > 0) {
            double weg = Math.min(n, schild.sterkte);
            schild.sterkte -= weg;
            n -= weg;
        }
        n = Math.max(0, n);
        if (doel instanceof ServerPlayer dp) {
            SpelerData d = Spelers.data(dp);
            d.vads = Math.max(0, Spelers.vads(dp) - n);
            if (n > 0) {
                dp.connection.send(new net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket(dp));
                sl.playSound(null, dp.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.7f, 1.1f);
            }
            Spelers.sync(dp);
            if (d.vads <= 0) {
                Flauwvallen.flauw(dp);
            }
        } else if (doel instanceof VadsrijkMob m) {
            m.krijgSchade(n, bron);
        } else if (n > 0) {
            doel.hurtServer(sl, sl.damageSources().generic(), (float) n);
        }
        if (bron instanceof ServerPlayer bp && n > 0) {
            double roof = Spelers.stats(bp).get(Stat.LEVENSROOF);
            if (roof > 0) {
                heal(bp, bp, n * roof / 100.0);
            }
            Getallen.toon(sl, doel, n, krit, k.element(), ef);
            if (doelEl != Element.GEEN || k.element() != Element.GEEN) {
                Tips.toon(bp, "vadsrijk.kern.tip.elementen");   // (the first hit with/on an element: the triangle + the icons)
            }
        } else if (doel instanceof ServerPlayer && n > 0) {
            Getallen.toon(sl, doel, n, false, Element.GEEN, 1);
        }
        Uitkomst u = new Uitkomst(k, n, krit, ef);
        NA_KLAP.forEach(c -> c.accept(u));
        return u;
    }

    /** A heal (Heal-kracht of the healer counts). Players only get Vads; mobs heal too. */
    public static double heal(@Nullable LivingEntity bron, LivingEntity doel, double n) {
        if (bron instanceof ServerPlayer bp && bron != doel) {
            n *= 1 + Spelers.stats(bp).get(Stat.HEALKRACHT) / 100.0;
        }
        if (Statussen.heeft(doel, Status.GESPOREN)) {
            n *= 0.5;
        }
        if (doel instanceof ServerPlayer dp) {
            double max = Spelers.maxVads(dp);
            double voor = Spelers.vads(dp);
            Spelers.data(dp).vads = Math.min(max, voor + n);
            n = Spelers.data(dp).vads - voor;
            Spelers.sync(dp);
        } else if (doel instanceof VadsrijkMob m) {
            m.heal(n);
        }
        double h = n;
        NA_HEAL.forEach(l -> l.heal(bron, doel, h));
        return n;
    }

    /** Game time from which the next melee basic attack may land (server-side; the interval of kern.Aanvalstempo). */
    public static final java.util.Map<java.util.UUID, Long> VOLGENDE_KLAP = new java.util.concurrent.ConcurrentHashMap<>();
    /**
     * (h-melee) A melee click at most this many ticks before the weapon is ready is not lost: it is remembered and the hit
     * lands the moment the weapon is ready ({@link #wachtendeKlap}), like the ranged/magic buffer of klassen.Basisaanval.
     */
    public static final int BUFFER = 6;
    /** (h-melee) Client/server tick jitter: a click this many ticks "early" still counts as ready. */
    public static final int SPELING = 1;

    /** A remembered early melee click: the target and the tick it may land. */
    public record Wachtend(int doelId, long op) {
    }

    public static final java.util.Map<java.util.UUID, Wachtend> WACHTEND = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * A left click with the weapon in hand on a Vadsrijk mob (called from {@link VadsrijkMob#hurtServer}).
     * <p>h-melee (round 3): the server keeps its own rhythm ({@link #VOLGENDE_KLAP}, the weapon's interval) and no longer
     * also demands the vanilla attack-cooldown charge (&ge; 92 %): the vanilla ticker on the server is reset by EVERY click
     * (also misses and early clicks) and runs a tick or two apart from the client's crosshair, so a click on "ready" was
     * sometimes swallowed whole. Now: ready (with {@link #SPELING}) = a hit; up to {@link #BUFFER} ticks early = the hit
     * lands when ready; earlier = nothing (spam clicking still never beats the interval).
     */
    public static boolean basisaanval(ServerPlayer p, LivingEntity doel) {
        if (Statussen.heeft(p, Status.VERDOOFD)) {
            return false;
        }
        long nu = p.level().getGameTime();
        long klaar = VOLGENDE_KLAP.getOrDefault(p.getUUID(), 0L);
        if (nu + SPELING < klaar) {
            if (klaar - nu <= BUFFER) {
                WACHTEND.put(p.getUUID(), new Wachtend(doel.getId(), klaar));
            }
            return false;
        }
        WACHTEND.remove(p.getUUID());
        return klap(p, doel, nu);
    }

    /** The melee hit itself: sets the next ready tick, the damage pipeline, the ult charge. */
    private static boolean klap(ServerPlayer p, LivingEntity doel, long nu) {
        VOLGENDE_KLAP.put(p.getUUID(), nu + intervalTicks(p));
        double basis = Spelers.klapSchade(p);
        Uitkomst u = doe(new Klap(p, doel, basis, Spelers.wapenElement(p), Schade.Soort.MELEE, true, "basis"));
        Spelers.laadUlt(p, u.schade());
        return u.schade() > 0;
    }

    /** This player's melee interval now, in ticks (weapon + Aanvalssnelheid). */
    public static int intervalTicks(ServerPlayer p) {
        return Aanvalstempo.ticks(Aanvalstempo.seconden(p, Spelers.stats(p).get(Stat.AANVALSSNELHEID)));
    }

    /** Every tick per player in the dimension (KernEvents): lands a remembered early click once the weapon is ready. */
    public static void wachtendeKlap(ServerPlayer p) {
        Wachtend w = WACHTEND.get(p.getUUID());
        if (w == null) {
            return;
        }
        long nu = p.level().getGameTime();
        if (nu + SPELING < w.op()) {
            return;
        }
        WACHTEND.remove(p.getUUID());
        if (!(p.level() instanceof ServerLevel sl) || !p.isAlive() || p.isSpectator() || Statussen.heeft(p, Status.VERDOOFD)
                || !(sl.getEntity(w.doelId()) instanceof LivingEntity doel) || doel.isRemoved() || !doel.isAlive()
                || (doel instanceof VadsrijkMob m && m.geplofd()) || !p.isWithinEntityInteractionRange(doel, 1.0)
                || !Spelers.voorAanval(p)) {
            return;
        }
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        if (klap(p, doel, nu)) {
            sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    static Component getal(double n, boolean krit) {
        return Component.literal((krit ? "✨" : "") + (int) Math.ceil(n)).withStyle(krit ? ChatFormatting.GOLD : ChatFormatting.WHITE);
    }

    static boolean zelfdeKant(LivingEntity a, LivingEntity b) {
        return a instanceof ServerPlayer pa && b instanceof ServerPlayer pb && (pa == pb || Partijen.zelfdeParty(pa, pb));
    }
}
