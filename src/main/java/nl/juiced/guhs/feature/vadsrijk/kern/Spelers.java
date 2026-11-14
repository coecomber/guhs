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
        return klasse(p).titelKey(klasseLevel(p));
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

    /**
     * The GENERIC level (g-klassen): Vads, Verdediging, area/quest gates, armour and jewellery, the party HUD. For the class
     * level use {@link #level(Player, String)} or {@link #klasseLevel}.
     */
    public static int level(Player p) {
        return data(p).level();
    }

    /** The level of one class (main stat, skill points, weapons, job advancement). */
    public static int level(Player p, String klasse) {
        return data(p).level(klasse);
    }

    /** The level of the active class. */
    public static int klasseLevel(Player p) {
        return data(p).klasseLevel();
    }

    // ---- class weapons (g-klassen: the class is the weapon in your hand) -------------------------------------------------

    /** The class a weapon base type belongs to: its own {@code klasse}, else by kind (melee = ridder, ranged = jager, magic = magier). */
    @Nullable
    public static String wapenKlasse(@Nullable BasisType t) {
        if (t == null || t.slot() != Slot.WAPEN) {
            return null;
        }
        if (t.klasse() != null && Klassen.van(t.klasse()) != null) {
            return t.klasse();
        }
        return switch (Aanvalstempo.schadeSoort(t)) {
            case AFSTAND -> Klassen.JAGER;
            case MAGIE -> Klassen.MAGIER;
            default -> Klassen.RIDDER;
        };
    }

    /** The class weapon in the main hand, requirement met or not (null = no class weapon: only the punch). */
    @Nullable
    public static Uitrusting wapenInHand(Player p) {
        Uitrusting u = Rollen.van(p.getMainHandItem());
        return u != null && u.basisType() != null && u.basisType().slot() == Slot.WAPEN ? u : null;
    }

    /** Weapon requirement: the CLASS level of the weapon's class. */
    public static boolean magWapen(Player p, Uitrusting u) {
        String k = wapenKlasse(u.basisType());
        return k == null || u.level() <= level(p, k);
    }

    /** The class weapon in the main hand whose requirement you meet (any class), or null. */
    @Nullable
    public static Uitrusting wapen(Player p) {
        Uitrusting u = wapenInHand(p);
        return u != null && magWapen(p, u) ? u : null;
    }

    /** The class of the weapon in your hand that you may use, or null (then: no skills, the HUD class line is grey). */
    @Nullable
    public static String actieveWapenKlasse(Player p) {
        Uitrusting u = wapen(p);
        return u == null ? null : wapenKlasse(u.basisType());
    }

    /** Called on a class switch (player, old class, new class), after the cooldowns are set. */
    public static final List<KlasseWissel> KLASSE_GEWISSELD = new java.util.concurrent.CopyOnWriteArrayList<>();

    @FunctionalInterface
    public interface KlasseWissel {
        void accept(ServerPlayer p, String oud, String nieuw);
    }

    private static final java.util.Map<java.util.UUID, Long> MELDING = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Every attack (basic melee hit, ranged/magic shot, skill) asks this first. With a class weapon whose requirement you
     * don't meet: a message and false (the attack doesn't happen). With a weapon of another class: the active class switches
     * ({@link #wisselKlasse}). Without a class weapon: true (the weak punch).
     */
    public static boolean voorAanval(ServerPlayer p) {
        Uitrusting u = wapenInHand(p);
        if (u == null) {
            return true;
        }
        String k = wapenKlasse(u.basisType());
        if (!magWapen(p, u)) {
            long nu = p.level().getGameTime();
            if (nu > MELDING.getOrDefault(p.getUUID(), 0L)) {
                MELDING.put(p.getUUID(), nu + 40);
                p.sendOverlayMessage(Component.translatable("vadsrijk.kern.wapen.vereist", Component.translatable(Klassen.ofRidder(k).naamKey()), u.level(),
                        level(p, k)).withStyle(ChatFormatting.RED));
                p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.4f, 0.6f);
            }
            return false;
        }
        if (k != null && !k.equals(data(p).klasse)) {
            wisselKlasse(p, k);
        }
        return true;
    }

    /**
     * Switches the active class: your Vads keep their percentage, the new class's R/V/G start on FULL cooldown and its ult
     * meter at 0 (the Knuffelsprong keeps its own cooldown).
     */
    public static void wisselKlasse(ServerPlayer p, String nieuw) {
        SpelerData d = data(p);
        Klasse k = Klassen.van(nieuw);
        if (k == null || nieuw.equals(d.klasse)) {
            return;
        }
        String oud = d.klasse;
        double deel = vads(p) / maxVads(p);
        d.klasse = nieuw;
        d.vads = Math.max(1, maxVads(p) * deel);
        d.energie = Math.min(Math.max(0, d.energie), maxEnergie(p));
        d.ultLading = 0;
        long nu = p.level().getGameTime();
        for (var v : k.vaardigheden()) {
            if (!v.ult()) {
                d.klaarOp.put(v.id(), nu + Vaardigheden.cooldown(p, v));
            }
        }
        p.sendOverlayMessage(Component.translatable("vadsrijk.kern.klasse.actief", Component.translatable(titelKey(p))).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
        KLASSE_GEWISSELD.forEach(h -> h.accept(p, oud, nieuw));
        sync(p);
    }

    /**
     * The weapon's damage per second for skills: its rating ({@link Rollen#wapenSchade}) x the class factor of its kind
     * ({@link Aanvalstempo}). Skills multiply this; a basic hit is {@link #klapSchade}.
     */
    public static double wapenSchade(Player p) {
        Uitrusting u = wapen(p);
        return u == null ? VUIST + 0.5 * level(p) : Rollen.wapenSchade(u) * Aanvalstempo.klasseFactor(u.basisType());
    }

    /** One basic attack: {@link #wapenSchade} x the weapon's base interval in seconds (slow weapons hit big). */
    public static double klapSchade(Player p) {
        Uitrusting u = wapen(p);
        return wapenSchade(p) * Aanvalstempo.seconden(u == null ? null : u.basisType());
    }

    public static Element wapenElement(Player p) {
        Uitrusting u = wapen(p);
        return u == null ? Element.GEEN : u.element();
    }

    /**
     * Final stat changes after gear/tree/extras (klassen: the balans multipliers per class, the Knuffelridder's Vads per class
     * level). Called before the statuses (Duizelig etc.).
     */
    public static final List<java.util.function.BiConsumer<ServerPlayer, Stats>> STATS_NA = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * Everything that counts now (g-klassen): the active class's growth, with Vads and Verdediging from the GENERIC level and
     * the rest (main stats) from the CLASS level; the weapon (class level requirement), armour and jewellery (generic level
     * requirement), the tree of the active class, statuses.
     */
    public static Stats stats(Player p) {
        SpelerData d = data(p);
        Klasse k = klasse(p);
        int klv = d.level(k.id());
        int lvl = d.level();
        Stats s = k.basis(klv).kopie();
        Stats gen = k.basis(lvl);
        s.zet(Stat.VADS, gen.get(Stat.VADS));
        s.zet(Stat.VERDEDIGING, gen.get(Stat.VERDEDIGING));
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
                if (u != null && u.basisType() != null && slot.past(u.basisType().slot()) && u.level() <= lvl) {   // T-ITEMS: past (RING2)
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
        if (p instanceof ServerPlayer sp) {
            for (var f : STATS_NA) {
                f.accept(sp, s);
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
        return klasse(p).maxEnergie(klasseLevel(p));
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

    /** BELEVING hook (fix round 1): xp factors per player (the Vrolijke reeks gives +10 %), all multiplied. */
    public static final List<java.util.function.ToDoubleFunction<ServerPlayer>> XP_FACTOR = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** U-HUIZEN: extra xp on top of a gain (after the factors), e.g. rested xp from the own house; each returns what it adds. */
    public static final List<java.util.function.ToLongBiFunction<ServerPlayer, Long>> XP_EXTRA = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * g-klassen: while a defeat hands out xp ({@code VadsrijkMob.verslagen}), the damage per player per class on that mob: the
     * class that dealt the most damage gets the class xp. Null outside a defeat.
     */
    @Nullable
    private static java.util.Map<java.util.UUID, java.util.Map<String, Double>> klasseBijdrage;

    /** Runs {@code wat} (the xp/loot hand-out of a defeat) with the per-class damage of that mob known to {@link #geefXp}. */
    public static void metKlasseBijdrage(java.util.Map<java.util.UUID, java.util.Map<String, Double>> bijdrage, Runnable wat) {
        var oud = klasseBijdrage;
        klasseBijdrage = bijdrage;
        try {
            wat.run();
        } finally {
            klasseBijdrage = oud;
        }
    }

    /** The class that gets this player's class xp now: the class that hit the defeated mob the hardest, else the active class. */
    public static String xpKlasse(ServerPlayer p) {
        var m = klasseBijdrage == null ? null : klasseBijdrage.get(p.getUUID());
        if (m != null && !m.isEmpty()) {
            String beste = null;
            double b = -1;
            for (var e : m.entrySet()) {
                if (e.getValue() > b && Klassen.van(e.getKey()) != null) {
                    b = e.getValue();
                    beste = e.getKey();
                }
            }
            if (beste != null) {
                return beste;
            }
        }
        return data(p).klasse;
    }

    /** 1101-balans: class catch-up, +15 % class xp per level behind your highest class, at most x3 (generic xp unchanged). */
    public static final double INHAAL_PER_LEVEL = 0.15, INHAAL_MAX = 3.0;

    /** The catch-up factor for a class {@code achter} levels behind the highest class (pure, tests). */
    public static double inhaalFactor(int hoogste, int deze) {
        return Math.min(INHAAL_MAX, 1 + INHAAL_PER_LEVEL * Math.max(0, hoogste - deze));
    }

    /** The catch-up factor of {@code klasse} for this player (1 for the highest class). */
    public static double inhaalFactor(SpelerData d, String klasse) {
        int hoogste = 1;
        for (var k : Klassen.alle()) {
            hoogste = Math.max(hoogste, d.level(k.id()));
        }
        return inhaalFactor(hoogste, d.level(klasse));
    }

    /** Xp (g-klassen): ALL of it to the generic level AND to the class of {@link #xpKlasse}. */
    public static void geefXp(ServerPlayer p, long n, String bron) {
        geefXp(p, n, bron, xpKlasse(p));
    }

    /** Xp to the generic level and to {@code klasse} (null = only generic). */
    public static void geefXp(ServerPlayer p, long n, String bron, @Nullable String klasse) {
        if (n <= 0) {
            return;
        }
        double factor = 1;
        for (java.util.function.ToDoubleFunction<ServerPlayer> f : XP_FACTOR) {
            factor *= f.applyAsDouble(p);
        }
        n = Math.round(n * factor);
        long basisN = n;
        for (java.util.function.ToLongBiFunction<ServerPlayer, Long> f : XP_EXTRA) {   // U-HUIZEN
            n += f.applyAsLong(p, basisN);
        }
        SpelerData d = data(p);
        int gVoor = d.level();
        long nKlasse = klasse != null && Klassen.van(klasse) != null
                ? nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.knip(p, d, Math.round(n * inhaalFactor(d, klasse)), klasse) : n;   // X-ZONE1: soft cap / bank; 1101-balans: catch-up
        n = nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.knip(p, d, n, null);   // X-ZONE1
        d.generiekXp = Math.min(Levels.totaalVoorGeneriek(Levels.MAX_GENERIEK), d.generiekXp + n);
        int gNa = d.level();
        int kVoor = 0, kNa = 0;
        if (klasse != null && Klassen.van(klasse) != null) {
            kVoor = d.level(klasse);
            d.xp.put(klasse, Math.min(Levels.totaalVoor(Levels.MAX), d.xp(klasse) + nKlasse));   // X-ZONE1: nKlasse (was n)
            kNa = d.level(klasse);
        }
        levelOp(p, gNa > gVoor ? gNa : 0, klasse, kNa > kVoor ? kNa : 0);
        sync(p);
    }

    /** Xp from a defeat: the player and every party member within 48 blocks in the dimension get it in full. */
    public static void geefXpGedeeld(ServerPlayer p, long n, String bron) {
        List<ServerPlayer> leden = Partijen.ledenInBuurt(p, 48);
        for (ServerPlayer l : leden) {
            geefXp(l, n, bron);
        }
    }

    /** Dev/tests: sets the generic level AND the active class's level. */
    public static void zetLevel(ServerPlayer p, int level) {
        SpelerData d = data(p);
        level = Math.max(1, Math.min(Levels.MAX, level));
        int gVoor = d.level(), kVoor = d.klasseLevel();
        d.generiekXp = Levels.totaalVoorGeneriek(level);
        d.xp.put(d.klasse, Levels.totaalVoor(level));
        d.vads = maxVads(p);
        levelOp(p, d.level() > gVoor ? d.level() : 0, d.klasse, d.klasseLevel() > kVoor ? d.klasseLevel() : 0);
        sync(p);
    }

    /** Dev/tests: only the generic level. */
    public static void zetGeneriek(ServerPlayer p, int level) {
        SpelerData d = data(p);
        int voor = d.level();
        d.generiekXp = Levels.totaalVoorGeneriek(Math.max(1, Math.min(Levels.MAX_GENERIEK, level)));
        levelOp(p, d.level() > voor ? d.level() : 0, null, 0);
        sync(p);
    }

    /** Dev/tests: only one class's level. */
    public static void zetKlasseLevel(ServerPlayer p, String klasse, int level) {
        SpelerData d = data(p);
        int voor = d.level(klasse);
        d.xp.put(klasse, Levels.totaalVoor(Math.max(1, Math.min(Levels.MAX, level))));
        levelOp(p, 0, klasse, d.level(klasse) > voor ? d.level(klasse) : 0);
        sync(p);
    }

    /**
     * Level-up effects. {@code generiek} / {@code klasseLevel}: the new level, 0 = that one didn't go up. Hook
     * {@link Haken#LEVEL_OP} with "generiek:N" and/or "&lt;klasse&gt;:N".
     */
    private static void levelOp(ServerPlayer p, int generiek, @Nullable String klasse, int klasseLevel) {
        if (generiek <= 0 && klasseLevel <= 0) {
            return;
        }
        SpelerData d = data(p);
        d.vads = maxVads(p);
        d.energie = maxEnergie(p);
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), generiek > 0 ? 60 : 25, 0.6, 1.0, 0.6, 0.3);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() + 2, p.getZ(), 40, 1.2, 0.6, 1.2, 0.05);
            sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, generiek > 0 ? 1.0f : 1.3f);
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.8f);
        }
        Component naam = klasse == null ? Component.empty() : Component.translatable(Klassen.ofRidder(klasse).naamKey());
        Component onder = generiek > 0 && klasseLevel > 0 ? Component.translatable("vadsrijk.kern.levelop.beide", generiek, naam, klasseLevel)
                : generiek > 0 ? Component.translatable("vadsrijk.kern.levelop.generiek", generiek)
                : Component.translatable("vadsrijk.kern.levelop.onder", naam, klasseLevel);
        nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes.titel(p, Component.translatable("vadsrijk.kern.levelop.titel").withStyle(ChatFormatting.GOLD),
                onder, 50, 0xFFD24A);
        Tips.toon(p, "vadsrijk.kern.tip.levelop");
        if (generiek > 0) {
            Haken.LEVEL_OP.forEach(h -> h.accept(p, "generiek:" + generiek));
        }
        if (klasseLevel > 0 && klasse != null) {
            Haken.LEVEL_OP.forEach(h -> h.accept(p, klasse + ":" + klasseLevel));
        }
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
