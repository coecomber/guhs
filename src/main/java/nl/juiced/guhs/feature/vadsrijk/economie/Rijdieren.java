package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;

/**
 * Mounts with stats (design 9: "rijdieren met stats"; G-MOUNTS: the mount system). The collection is kept per player in
 * {@code SpelerData.extra("economie")}: {@code rijdieren} = {soort id: {level, xp, kleuren ("roze,mint"), kleur}}, {@code actief} =
 * the chosen one, {@code starter} = the starter colour picked in the zone-3 quest.
 * <ul>
 *   <li>the mount key (default X, {@link #toets}) calls the chosen mount and seats you on it (a poof + a pop-in); again: it hops
 *   home. The rijfluitje still works but is optional now;</li>
 *   <li>the mount screen (default N, {@link Stal#overzicht}) shows all mounts (silhouettes for the ones you lack) and picks the
 *   active mount and its colour; at the stal ({@link Stal#open}) you can also buy;</li>
 *   <li>it poofs away when you get hit (then a short "geschrokken" cooldown), at a boss arena, when you get off, faint, leave the
 *   Vadsrijk, log out, and after a restart (never saved in the world). Riding in towns is fine;</li>
 *   <li>riding gives the mount xp: one per block ({@link #geefXp}); level 1..{@link RijdierSoort#MAX_LEVEL}.</li>
 * </ul>
 * APIs: {@link #geefEerste} (npc, the Wolkenschaapje return quest), {@link #kiesStarter} (zone 3 main quest: choose 1 of 3 starters),
 * {@link #geef} / {@link #geefKleur} (rewards), {@link #STARTER_GEKOZEN} (quest progress hook).
 */
public final class Rijdieren {
    /** Ticks between summoning and sending home (anti-spam). */
    public static final int CD_NORMAAL = 20;
    /** Ticks you can't call a mount after it ran off from a hit. */
    public static final int CD_GESCHROKKEN = 100;

    /** HUD states (see {@link EcoPayloads.RijdierHud}). */
    public static final int HUD_GEEN = 0, HUD_KLAAR = 1, HUD_RIJDT = 2, HUD_WACHT = 3, HUD_GESCHROKKEN = 4, HUD_ARENA = 5;

    /** Zone 3 quest hook: called once when a player picked their starter colour. */
    public static final List<BiConsumer<ServerPlayer, PluimKleur>> STARTER_GEKOZEN = new CopyOnWriteArrayList<>();

    /** Server: the mount each player has out right now (entity id). */
    private static final Map<UUID, UUID> UIT = new ConcurrentHashMap<>();
    /** Server: cooldown per player: {until (game time), max ticks, geschrokken?}. */
    private static final Map<UUID, long[]> CD = new ConcurrentHashMap<>();
    /** Server: the last HUD state sent, to only send changes. */
    private static final Map<UUID, String> HUD = new ConcurrentHashMap<>();

    private Rijdieren() {
    }

    private static CompoundTag eco(ServerPlayer p) {
        return Spelers.data(p).extra("economie");
    }

    private static CompoundTag tag(ServerPlayer p) {
        CompoundTag e = eco(p);
        if (!e.contains("rijdieren")) {
            e.put("rijdieren", new CompoundTag());
        }
        return e.getCompoundOrEmpty("rijdieren");
    }

    private static CompoundTag mount(ServerPlayer p, RijdierSoort s) {
        return tag(p).getCompoundOrEmpty(s.id());
    }

    public static boolean heeft(ServerPlayer p, RijdierSoort s) {
        return tag(p).contains(s.id());
    }

    public static int level(ServerPlayer p, RijdierSoort s) {
        return mount(p, s).getIntOr("level", 1);
    }

    public static long xp(ServerPlayer p, RijdierSoort s) {
        return mount(p, s).getLongOr("xp", 0);
    }

    // ---- colours (the Pluimguh) -----------------------------------------------------------------------------------------------

    public static boolean heeftKleur(ServerPlayer p, PluimKleur k) {
        if (!heeft(p, RijdierSoort.PLUIMGUH)) {
            return false;
        }
        for (String s : mount(p, RijdierSoort.PLUIMGUH).getStringOr("kleuren", "").split(",")) {
            if (s.equals(k.id())) {
                return true;
            }
        }
        return false;
    }

    /** The colour the player rides (of the soort; only the Pluimguh has colours). */
    public static PluimKleur kleur(ServerPlayer p, RijdierSoort s) {
        if (!s.kleuren()) {
            return PluimKleur.ROZE;
        }
        PluimKleur k = PluimKleur.vanOfNull(mount(p, s).getStringOr("kleur", ""));
        if (k != null && heeftKleur(p, k)) {
            return k;
        }
        for (PluimKleur x : PluimKleur.values()) {
            if (heeftKleur(p, x)) {
                return x;
            }
        }
        return PluimKleur.ROZE;
    }

    /** Unlocks a colour of the Pluimguh (and the Pluimguh itself when needed). False when already owned. */
    public static boolean geefKleur(ServerPlayer p, PluimKleur k) {
        if (heeftKleur(p, k)) {
            return false;
        }
        boolean nieuw = !heeft(p, RijdierSoort.PLUIMGUH);
        if (nieuw) {
            maak(p, RijdierSoort.PLUIMGUH);
        }
        CompoundTag m = mount(p, RijdierSoort.PLUIMGUH);
        String oud = m.getStringOr("kleuren", "");
        m.putString("kleuren", oud.isEmpty() ? k.id() : oud + "," + k.id());
        if (nieuw) {
            m.putString("kleur", k.id());
        }
        tag(p).put(RijdierSoort.PLUIMGUH.id(), m);
        if (nieuw && eco(p).getStringOr("actief", "").isEmpty()) {
            eco(p).putString("actief", RijdierSoort.PLUIMGUH.id());
        }
        EcoUtil.geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.3f);
        EcoUtil.chat(p, "vadsrijk.economie.rijdier.nieuw_kleur", Component.translatable(k.naamKey()).withStyle(ChatFormatting.GOLD),
                Component.translatable(RijdierSoort.PLUIMGUH.naamKey()));
        return true;
    }

    // ---- choosing -------------------------------------------------------------------------------------------------------------

    @Nullable
    public static RijdierSoort actief(ServerPlayer p) {
        RijdierSoort s = RijdierSoort.van(eco(p).getStringOr("actief", ""));
        if (s != null && heeft(p, s)) {
            return s;
        }
        for (RijdierSoort x : RijdierSoort.values()) {
            if (heeft(p, x)) {
                return x;
            }
        }
        return null;
    }

    public static void kies(ServerPlayer p, RijdierSoort s) {
        if (heeft(p, s)) {
            eco(p).putString("actief", s.id());
        }
    }

    /** Picks the active mount and (Pluimguh) its colour. False when not owned. */
    public static boolean kies(ServerPlayer p, RijdierSoort s, @Nullable PluimKleur k) {
        if (!heeft(p, s) || (s.kleuren() && k != null && !heeftKleur(p, k))) {
            return false;
        }
        eco(p).putString("actief", s.id());
        if (s.kleuren() && k != null) {
            CompoundTag m = mount(p, s);
            m.putString("kleur", k.id());
            tag(p).put(s.id(), m);
        }
        RijdierEntity r = uit(p);
        if (r != null && (r.soort() != s || r.kleur() != kleur(p, s))) {
            poef(r);   // (the old one hops home; the key calls the new one)
        }
        return true;
    }

    // ---- getting mounts ---------------------------------------------------------------------------------------------------------

    /**
     * API (npc slice, the return quest of Stalmeester Hoefje, "Een eigen Wolkenschaapje", only after the zone-3 starter; and dev):
     * the Wolkenschaapje (I-MOUNTS: it replaced the Hoedhupper). False when the player already had it (e.g. bought at the stal). (The rijfluitje is optional now.)
     */
    public static boolean geefEerste(ServerPlayer p) {
        boolean nieuw = geef(p, RijdierSoort.WOLKENSCHAAPJE);
        if (nieuw) {
            Tips.toon(p, "vadsrijk.economie.tip.rijdier");
        }
        return nieuw;
    }

    /**
     * The reward of the Wolkenschaapje return quest (npc role "rijdier"): the Wolkenschaapje, or, when the player already bought it at the stal, its
     * price back. Always true (so the quest never says "later").
     */
    public static boolean questBeloning(ServerPlayer p) {
        if (geefEerste(p)) {
            return true;
        }
        long prijs = RijdierSoort.WOLKENSCHAAPJE.prijs;
        Spelers.geefMunten(p, prijs);
        EcoUtil.chat(p, "vadsrijk.economie.rijdier.al_schaapje", Component.literal(Spelers.munten(prijs)).withStyle(ChatFormatting.GOLD));
        return true;
    }

    private static void maak(ServerPlayer p, RijdierSoort s) {
        CompoundTag m = new CompoundTag();
        m.putInt("level", 1);
        m.putLong("xp", 0);
        tag(p).put(s.id(), m);
    }

    /** Unlocks a mount (level 1; the Pluimguh in pink). False when already owned. */
    public static boolean geef(ServerPlayer p, RijdierSoort s) {
        if (s.kleuren()) {
            return !heeft(p, s) && geefKleur(p, PluimKleur.ROZE);
        }
        if (heeft(p, s)) {
            return false;
        }
        maak(p, s);
        if (eco(p).getStringOr("actief", "").isEmpty()) {
            kies(p, s);
        }
        EcoUtil.geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.3f);
        EcoUtil.chat(p, "vadsrijk.economie.rijdier.nieuw", Component.translatable(s.naamKey()).withStyle(ChatFormatting.GOLD));
        if (!magRijden(p)) {   // 1.10.2
            EcoUtil.chat(p, "vadsrijk.economie.rijdier.leer", Component.translatable(s.naamKey()));
        }
        return true;
    }

    /**
     * API for quests and rewards: unlocks a mount by its id ({@link RijdierSoort#id()}: "knuffelhert", "guhwolf", "wolkenschaapje",
     * ...; "pluimguh" = the pink one; "pluimguh:<kleur>" = that colour). False when unknown or already owned.
     */
    public static boolean geef(ServerPlayer p, String id) {
        if (id.startsWith("pluimguh:")) {
            PluimKleur k = PluimKleur.vanOfNull(id.substring(9));
            return k != null && geefKleur(p, k);
        }
        RijdierSoort s = RijdierSoort.van(id);
        return s != null && geef(p, s);
    }

    /** True when the player owns any mount (the return quests of the Knuffelhert / Guhwolf appear from then on). */
    public static boolean heeftEen(ServerPlayer p) {
        return actief(p) != null;
    }

    /**
     * 1.10.2 (user decision): mounts can be USED (summoned with the key / the rijfluitje, ridden) only after the zone-3 Pluimstal quest,
     * where the starter Pluimguh is chosen. Mounts from earlier (drops, the return quests) stay in the collection. Ops/creative always.
     */
    public static boolean magRijden(ServerPlayer p) {
        return heeftStarter(p) || nl.juiced.guhs.feature.vadsrijk.npc.Questlog.klaar(p, "z3_hq03_pluimstal") || p.isCreative()
                || p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    /** True when the player already picked a starter (zone 3 quest). */
    public static boolean heeftStarter(ServerPlayer p) {
        return !eco(p).getStringOr("starter", "").isEmpty();
    }

    /**
     * API (zone 3 main quest): opens the choice screen with the 3 starter Pluimguhs (pink, mint, lavender); the player picks one for
     * free ({@link #geefStarter}, then {@link #STARTER_GEKOZEN} fires). False (and nothing opens) when a starter was already picked.
     * Closing the screen without picking is fine: call it again.
     */
    public static boolean kiesStarter(ServerPlayer p) {
        if (heeftStarter(p)) {
            return false;
        }
        Stal.starterScherm(p);
        return true;
    }

    /** The free starter. False when a starter was already picked or the colour is no starter. */
    public static boolean geefStarter(ServerPlayer p, PluimKleur k) {
        if (heeftStarter(p) || !k.starter) {
            return false;
        }
        eco(p).putString("starter", k.id());
        geefKleur(p, k);
        kies(p, RijdierSoort.PLUIMGUH, k);
        Tips.toon(p, "vadsrijk.economie.tip.starter");
        for (BiConsumer<ServerPlayer, PluimKleur> h : STARTER_GEKOZEN) {
            h.accept(p, k);
        }
        return true;
    }

    /** Dev: everything (all mounts, all colours). */
    public static void alles(ServerPlayer p) {
        for (RijdierSoort s : RijdierSoort.values()) {
            if (!s.kleuren()) {
                geef(p, s);
            }
        }
        for (PluimKleur k : PluimKleur.values()) {
            geefKleur(p, k);
        }
    }

    /** Dev: forget the whole collection (and the starter choice). */
    public static void reset(ServerPlayer p) {
        vergeet(p);
        eco(p).remove("rijdieren");
        eco(p).remove("actief");
        eco(p).remove("starter");
        CD.remove(p.getUUID());
        HUD.remove(p.getUUID());
    }

    /** Riding xp; levels up (message + sparkles; title Rijdiermeester at the max). Returns the new level. */
    public static int geefXp(ServerPlayer p, RijdierSoort s, long n) {
        if (!heeft(p, s) || n <= 0) {
            return heeft(p, s) ? level(p, s) : 0;
        }
        CompoundTag m = mount(p, s);
        int level = m.getIntOr("level", 1);
        long xp = m.getLongOr("xp", 0) + n;
        boolean op = false;
        while (level < RijdierSoort.MAX_LEVEL && xp >= RijdierSoort.xpNodig(level)) {
            xp -= RijdierSoort.xpNodig(level);
            level++;
            op = true;
        }
        if (level >= RijdierSoort.MAX_LEVEL) {
            xp = 0;
        }
        m.putInt("level", level);
        m.putLong("xp", xp);
        tag(p).put(s.id(), m);
        if (op) {
            EcoUtil.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.5f);
            EcoUtil.zeg(p, "vadsrijk.economie.rijdier.level", Component.translatable(s.naamKey()), level);
            if (p.getVehicle() instanceof RijdierEntity r && r.soort() == s) {
                r.zetLevel(level);
                p.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, r.getX(), r.getY() + 1, r.getZ(), 20, 0.6, 0.5, 0.6, 0.1);
            }
            if (level >= RijdierSoort.MAX_LEVEL) {
                Cosmetica.geefTitel(p, Cosmetica.RIJDIERMEESTER);
            }
        }
        return level;
    }

    // ---- calling ---------------------------------------------------------------------------------------------------------------

    @Nullable
    static RijdierEntity uit(ServerPlayer p) {
        UUID id = UIT.get(p.getUUID());
        if (id == null) {
            return null;
        }
        Entity e = p.level().getEntity(id);
        return e instanceof RijdierEntity r && r.isAlive() ? r : null;
    }

    /** Ticks of cooldown left (0 = ready). */
    public static int cooldown(ServerPlayer p) {
        long[] c = CD.get(p.getUUID());
        return c == null ? 0 : (int) Math.max(0, c[0] - p.level().getGameTime());
    }

    private static void zetCd(ServerPlayer p, int ticks, boolean geschrokken) {
        CD.put(p.getUUID(), new long[] {p.level().getGameTime() + ticks, ticks, geschrokken ? 1 : 0});
    }

    /** The mount key (and the rijfluitje): rides off on your chosen mount, or sends it home. */
    public static void toets(ServerPlayer p) {
        if (!Vadsrijk.in(p)) {
            EcoUtil.zeg(p, "vadsrijk.economie.rijdier.alleen_rijk");
            return;
        }
        RijdierEntity oud = uit(p);
        if (oud != null) {
            poef(oud);
            zetCd(p, CD_NORMAAL, false);
            return;
        }
        int cd = cooldown(p);
        if (cd > 0) {
            long[] c = CD.get(p.getUUID());
            EcoUtil.zeg(p, c != null && c[2] == 1 ? "vadsrijk.economie.rijdier.nog_geschrokken" : "vadsrijk.economie.rijdier.wacht", (cd + 19) / 20);
            return;
        }
        RijdierSoort s = actief(p);
        if (s != null && !magRijden(p)) {   // 1.10.2: riding is learnt at the Pluimstal in zone 3; the mount waits in the stable
            EcoUtil.zeg(p, "vadsrijk.economie.rijdier.leer", Component.translatable(s.naamKey()));
            return;
        }
        RijdierSoort plek = nl.juiced.guhs.feature.vadsrijk.zone4.boot.Boot.kiesSoort(p, s);   // T-Z4BOOT: on water the boat, on land a land mount
        if (plek != s) {
            if (plek == null) {
                return;   // (Boot said why)
            }
            s = plek;
        }
        if (s == null) {
            EcoUtil.zeg(p, "vadsrijk.economie.rijdier.geen");
            return;
        }
        if (inArenaVerbod(p.getX(), p.getY(), p.getZ())) {
            EcoUtil.zeg(p, "vadsrijk.economie.rijdier.arena_niet");
            return;
        }
        if (p.isPassenger() || p.isSleeping() || p.isSpectator()) {
            return;
        }
        if (roep(p, s) != null) {
            zetCd(p, CD_NORMAAL, false);
        }
    }

    /** The rijfluitje (same as the key). */
    public static void fluit(ServerPlayer p) {
        toets(p);
    }

    /** Summons the mount (in its chosen colour) with a poof and seats the player on it. */
    @Nullable
    public static RijdierEntity roep(ServerPlayer p, RijdierSoort s) {
        ServerLevel sl = p.level();
        RijdierEntity r = EcoSpul.type(s).create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (r == null) {
            return null;
        }
        r.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        if (s.water() && !nl.juiced.guhs.feature.vadsrijk.zone4.boot.Boot.plaats(p, r)) {   // T-Z4BOOT: the boat goes on the water
            return null;
        }
        r.zetEigenaar(p.getUUID(), level(p, s));   // (+ its speed/jump for that level, synced: the client steers)
        r.zetKleur(kleur(p, s));
        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(r, "rijdier:" + p.getUUID());   // U-DUBBEL: one mount per player
        r.zetUiterlijk(nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.uiterlijk(p, s));   // T-RIJDIER: coat, pattern, tack
        sl.addFreshEntity(r);
        UIT.put(p.getUUID(), r.getUUID());
        p.startRiding(r);
        sl.sendParticles(ParticleTypes.CLOUD, r.getX(), r.getY() + 0.5, r.getZ(), 18, 0.6, 0.4, 0.6, 0.04);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, r.getX(), r.getY() + 0.8, r.getZ(), 30, 0.6, 0.6, 0.6, 0.05);
        sl.sendParticles(ParticleTypes.HEART, r.getX(), r.getY() + 1.6, r.getZ(), 3, 0.4, 0.2, 0.4, 0);
        EcoUtil.geluid(p, SoundEvents.NOTE_BLOCK_FLUTE.value(), 0.9f, 1.6f);
        EcoUtil.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
        if (!s.glijdt()) {   // V-INT: the flying carpet has its own first-ride tip (Tapijt), no "Spatie = een grote hup"
            Tips.toon(p, "vadsrijk.economie.tip.rijden");
        }
        return r;
    }

    /** The mount hops away with a pink poof. */
    public static void poef(RijdierEntity r) {
        if (r.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, r.getX(), r.getY() + 0.6, r.getZ(), 16, 0.5, 0.4, 0.5, 0.03);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, r.getX(), r.getY() + 0.8, r.getZ(), 16, 0.5, 0.4, 0.5, 0.03);
            sl.playSound(null, r.blockPosition(), SoundEvents.SLIME_JUMP_SMALL, net.minecraft.sounds.SoundSource.NEUTRAL, 0.8f, 1.5f);
        }
        if (r.eigenaar() != null) {
            UIT.remove(r.eigenaar(), r.getUUID());
        }
        r.ejectPassengers();
        r.discard();
    }

    /** The rider got hit: the mount runs off, and needs a moment before it dares to come back. */
    static void geraakt(ServerPlayer p) {
        if (p.getVehicle() instanceof RijdierEntity r) {
            if (r.soort().stevig() && r.vangSchrikOp()) {   // T-RIJDIER: the Zwamzwijntje shrugs off the first hit
                EcoUtil.zeg(p, "vadsrijk.rijdier.stevig");
                return;
            }
            poef(r);
            zetCd(p, CD_GESCHROKKEN, true);
            EcoUtil.zeg(p, "vadsrijk.economie.rijdier.geschrokken");
        }
    }

    /**
     * Why a ridden mount must go now (checked by the mount every few ticks): "rijk" (outside the Vadsrijk), "arena" (in a boss
     * arena), or null (keep riding; towns are fine).
     */
    @Nullable
    public static String afReden(boolean inRijk, double x, double y, double z) {
        if (!inRijk) {
            return "rijk";
        }
        return inArenaVerbod(x, y, z) ? "arena" : null;
    }

    /**
     * (z3-bazen) Spots inside a boss arena where riding is allowed anyway, e.g. the Kruinroofvogel's chase phase (mount up and
     * follow him to his other perch). Empty = the normal rule: no mounts in any arena.
     */
    public static final List<java.util.function.Predicate<net.minecraft.world.phys.Vec3>> RIJDEN_IN_ARENA = new CopyOnWriteArrayList<>();

    /** In a boss arena where no exception ({@link #RIJDEN_IN_ARENA}) allows riding right now. */
    public static boolean inArenaVerbod(double x, double y, double z) {
        if (Arenas.op(x, y, z) == null) {
            return false;
        }
        net.minecraft.world.phys.Vec3 v = new net.minecraft.world.phys.Vec3(x, y, z);
        for (java.util.function.Predicate<net.minecraft.world.phys.Vec3> f : RIJDEN_IN_ARENA) {
            if (f.test(v)) {
                return false;
            }
        }
        return true;
    }

    /** The rider entered a boss arena: off you get. */
    static void arena(ServerPlayer p, RijdierEntity r) {
        poef(r);
        EcoUtil.zeg(p, "vadsrijk.economie.rijdier.arena");
    }

    static void vergeet(ServerPlayer p) {
        RijdierEntity r = uit(p);
        if (r != null) {
            poef(r);
        }
        UIT.remove(p.getUUID());
    }

    static void uitgelogd(ServerPlayer p) {
        vergeet(p);
        CD.remove(p.getUUID());
        HUD.remove(p.getUUID());
    }

    // ---- HUD --------------------------------------------------------------------------------------------------------------------

    /** The HUD state of this player: {state, cooldown ticks left, cooldown max}. */
    static int[] hudStaat(ServerPlayer p) {
        RijdierSoort s = actief(p);
        if (s == null) {
            return new int[] {HUD_GEEN, 0, 0};
        }
        if (p.getVehicle() instanceof RijdierEntity) {
            return new int[] {HUD_RIJDT, 0, 0};
        }
        int cd = cooldown(p);
        long[] c = CD.get(p.getUUID());
        if (cd > 0 && c != null) {
            return new int[] {c[2] == 1 ? HUD_GESCHROKKEN : HUD_WACHT, cd, (int) c[1]};
        }
        if (inArenaVerbod(p.getX(), p.getY(), p.getZ())) {
            return new int[] {HUD_ARENA, 0, 0};
        }
        return new int[] {HUD_KLAAR, 0, 0};
    }

    /** Server, every few ticks in the Vadsrijk: sends the HUD icon's state when it changed. */
    static void stuurHud(ServerPlayer p) {
        RijdierSoort s = actief(p);
        int[] h = hudStaat(p);
        String soort = s == null ? "" : s.id(), kleur = s == null ? "" : kleur(p, s).id();
        String sleutel = soort + "|" + kleur + "|" + h[0] + "|" + h[2];
        if (sleutel.equals(HUD.get(p.getUUID()))) {
            return;
        }
        HUD.put(p.getUUID(), sleutel);
        EcoPayloads.stuurHud(p, new EcoPayloads.RijdierHud(soort, kleur, h[0], h[1], h[2]));
    }

    /** (tests) the stack the stal would show for a mount. */
    static ItemStack icoon(ServerPlayer p, RijdierSoort s) {
        return EcoSpul.icoon(s, kleur(p, s));
    }
}
