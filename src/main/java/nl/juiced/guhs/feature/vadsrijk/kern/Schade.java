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
 *   <li>defender (players): Verdediging (n x 100 / (100 + 2 x def)), Elementweerstand%, then Knuffelschild absorbs;</li>
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
    public record Klap(@Nullable LivingEntity bron, LivingEntity doel, double basis, Element element, Soort soort, boolean kanKritiek, String skillId) {
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

    /** Pure maths of step 4. */
    public static double verdediging(double n, Stats s, Element element) {
        n = n * 100.0 / (100.0 + 2.0 * Math.max(0, s.get(Stat.VERDEDIGING)));
        if (element != Element.GEEN) {
            n *= 1 - Math.min(75, s.get(Stat.ELEMENTWEERSTAND)) / 100.0;
        }
        return n;
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
            krit = k.kanKritiek() && bron != null && bron.getRandom().nextDouble() * 100 < s.get(Stat.KRITIEK);
            n = aanval(n, s, k.soort(), krit, k.element(), doelEl, ontvadst);
            if (bron != null && Statussen.heeft(bron, Status.VAHOEG)) {
                n *= 1 + Math.max(0.1, Statussen.sterkte(bron, Status.VAHOEG));
            }
        }
        for (Aanpasser a : VOOR_KLAP) {
            n = a.pas(k, n);
        }
        if (doel instanceof ServerPlayer dp && k.soort() != Soort.WAAR) {
            n = verdediging(n, Spelers.stats(dp), k.element());
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

    /** A left click with the weapon in hand on a Vadsrijk mob (called from {@link VadsrijkMob#hurtServer}). */
    public static boolean basisaanval(ServerPlayer p, LivingEntity doel) {
        if (Statussen.heeft(p, Status.VERDOOFD)) {
            return false;
        }
        double kracht = p.getAttackStrengthScale(0.5f);
        double basis = Spelers.wapenSchade(p) * (0.25 + 0.75 * kracht * kracht);
        Uitkomst u = doe(new Klap(p, doel, basis, Spelers.wapenElement(p), Schade.Soort.MELEE, kracht > 0.9, "basis"));
        Spelers.laadUlt(p, u.schade());
        return u.schade() > 0;
    }

    static Component getal(double n, boolean krit) {
        return Component.literal((krit ? "✨" : "") + (int) Math.ceil(n)).withStyle(krit ? ChatFormatting.GOLD : ChatFormatting.WHITE);
    }

    static boolean zelfdeKant(LivingEntity a, LivingEntity b) {
        return a instanceof ServerPlayer pa && b instanceof ServerPlayer pb && (pa == pb || Partijen.zelfdeParty(pa, pb));
    }
}
