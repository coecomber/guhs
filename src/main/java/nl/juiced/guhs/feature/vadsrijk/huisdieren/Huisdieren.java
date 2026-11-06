package nl.juiced.guhs.feature.vadsrijk.huisdieren;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * G-PETS: the pets of the Vadsrijk. Every field/world boss unlocks a tiny chibi pet for a player on their FIRST kill (the kern
 * first-kill key {@code "baaskill:<boss type>"}; contributors of the kill, see {@link #naKill}). One pet can walk along with you
 * (or none): it follows, hops after you, teleports along when it falls behind, is never targetable or hurt, and is never saved in
 * the world (respawned from the player data). Only in the Vadsrijk: it poofs away when you leave and comes back when you return.
 * <p>
 * Storage: {@code SpelerData.extra("huisdieren")}: {@code bezit} = "id,id", {@code actief} = the chosen id ("" = none on purpose;
 * missing = never chose, then a new pet starts walking along by itself).
 * <p>
 * New bosses (zone 3+): one line, e.g. in your registry class:
 * {@code Huisdieren.registreer(new Huisdier("kruinroofvogel", "guhs:vr_z3_baas_kruinroofvogel", "vr_z3_baas_kruinroofvogel",
 * "textures/vadsrijk/z3bazen/kruinroofvogel.png", "vr_huisdier_kruinroofvogel", 0.25f, null))} + its texts
 * ({@code vadsrijk.huisdier.<id>.naam|tekst|bron}) + an animation file (idle, hop, sprong; see tools/features/vadsrijk_huisdieren.py).
 * Then the boss's first kill unlocks it, the screen shows it, and players who already beat that boss get it when they come in.
 */
public final class Huisdieren {
    private static final Map<String, Huisdier> ALLE = Collections.synchronizedMap(new LinkedHashMap<>());

    public static final Huisdier KLODDERREUS = registreer(new Huisdier("klodderreus", "guhs:vr_klodderreus", "vr_baas_kk_klodder",
            "textures/entity/vr_huisdier_klodderreus.png", "vr_huisdier_klodderreus", 0.5f, null));
    public static final Huisdier KONING_KLODDER = registreer(new Huisdier("koning_klodder", "guhs:vr_baas_koning_klodder", "vr_baas_koning_klodder",
            "textures/vadsrijk/bazen/koning_klodder.png", "vr_huisdier_koning_klodder", 0.3f, null));
    public static final Huisdier GALMWOLF = registreer(new Huisdier("galmwolf", "guhs:vr_z2_baas_galmwolf", "vr_z2_baas_galmwolf",
            "textures/vadsrijk/z2bazen/galmwolf.png", "vr_huisdier_galmwolf", 0.24f, "textures/vadsrijk/z2bazen/galmwolf_glowmask.png"));

    /** Fires once when a player gets a new pet (quests, achievements, tests). */
    public static final List<BiConsumer<ServerPlayer, Huisdier>> NIEUW = new CopyOnWriteArrayList<>();

    /** Ticks between a boss kill and the "Nieuw huisdier!" title card (the boss's own victory moment goes first). */
    public static final int MOMENT_WACHT = 70;

    /** Server: the pet walking with each player. */
    private static final Map<UUID, HuisdierEntity> UIT = new ConcurrentHashMap<>();

    private record Moment(UUID speler, String id, long op) {
    }

    /** Server: title cards still to show. */
    private static final List<Moment> MOMENTEN = new CopyOnWriteArrayList<>();

    private Huisdieren() {
    }

    // ---- the registry ------------------------------------------------------------------------------------------------------

    /** Registers a pet (common code: the client screen lists them too). Returns it. */
    public static Huisdier registreer(Huisdier h) {
        ALLE.put(h.id(), h);
        return h;
    }

    public static Collection<Huisdier> alle() {
        synchronized (ALLE) {
            return List.copyOf(ALLE.values());
        }
    }

    @Nullable
    public static Huisdier van(String id) {
        return id == null ? null : ALLE.get(id);
    }

    // ---- ownership -------------------------------------------------------------------------------------------------------------

    private static CompoundTag tag(ServerPlayer p) {
        return Spelers.data(p).extra("huisdieren");
    }

    public static List<String> bezit(ServerPlayer p) {
        List<String> l = new ArrayList<>();
        for (String s : tag(p).getStringOr("bezit", "").split(",")) {
            if (!s.isEmpty() && ALLE.containsKey(s)) {
                l.add(s);
            }
        }
        return l;
    }

    public static boolean heeft(ServerPlayer p, String id) {
        return bezit(p).contains(id);
    }

    public static int aantal(ServerPlayer p) {
        return bezit(p).size();
    }

    @Nullable
    public static Huisdier actief(ServerPlayer p) {
        Huisdier h = van(tag(p).getStringOr("actief", ""));
        return h != null && heeft(p, h.id()) ? h : null;
    }

    /** Picks the pet that walks along (null or "" = none). False when the player doesn't have it. */
    public static boolean kies(ServerPlayer p, @Nullable String id) {
        if (id == null || id.isEmpty()) {
            tag(p).putString("actief", "");
            return true;
        }
        if (!heeft(p, id)) {
            return false;
        }
        tag(p).putString("actief", id);
        return true;
    }

    /** Gives a pet with the unlock moment (chat now, title card + poof after {@code wacht} ticks). False when already owned. */
    public static boolean geef(ServerPlayer p, String id, int wacht) {
        Huisdier h = van(id);
        if (h == null || heeft(p, id)) {
            return false;
        }
        CompoundTag t = tag(p);
        String oud = t.getStringOr("bezit", "");
        t.putString("bezit", oud.isEmpty() ? id : oud + "," + id);
        if (!t.contains("actief")) {
            t.putString("actief", id);   // the very first pet walks along by itself
        }
        p.sendSystemMessage(Component.translatable("vadsrijk.huisdieren.nieuw.chat", Component.translatable(h.naamKey()).withStyle(ChatFormatting.GOLD),
                Component.keybind("key.guhs.vr_huisdieren").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.LIGHT_PURPLE));
        MOMENTEN.add(new Moment(p.getUUID(), id, p.level().getGameTime() + Math.max(0, wacht)));
        for (BiConsumer<ServerPlayer, Huisdier> l : NIEUW) {
            l.accept(p, h);
        }
        return true;
    }

    public static boolean geef(ServerPlayer p, String id) {
        return geef(p, id, 10);
    }

    /** Dev: every pet. */
    public static void alles(ServerPlayer p) {
        for (Huisdier h : alle()) {
            geef(p, h.id(), 10);
        }
    }

    /** Dev: forget every pet (and the choice). */
    public static void reset(ServerPlayer p) {
        weg(p, false);
        Spelers.data(p).extra.remove("huisdieren");
        MOMENTEN.removeIf(m -> m.speler().equals(p.getUUID()));
    }

    // ---- unlocking -----------------------------------------------------------------------------------------------------------

    /**
     * A boss (or any Vadsrijk mob) was beaten: every helper (the kill's contributors + the last hit, kern {@code Haken.VERSLAGEN})
     * gets the pet of that boss type, once. Returns how many players got a new pet.
     */
    public static int naKill(Collection<ServerPlayer> helpers, String baasType) {
        int n = 0;
        for (Huisdier h : alle()) {
            if (!h.baas().equals(baasType)) {
                continue;
            }
            for (ServerPlayer p : helpers) {
                if (geef(p, h.id(), MOMENT_WACHT)) {
                    n++;
                }
            }
        }
        return n;
    }

    static void verslagen(Haken.Verslagen v) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(v.mob().getType()).toString();
        naKill(v.helpers(), type);
    }

    /** Players who already beat a boss before its pet existed get it now (from the kern first-kill key). */
    public static void inhalen(ServerPlayer p) {
        for (Huisdier h : alle()) {
            if (!heeft(p, h.id()) && Spelers.data(p).gezien.contains(h.killSleutel())) {
                geef(p, h.id(), 40);
            }
        }
    }

    private static void toonMoment(ServerPlayer p, Huisdier h) {
        RijkScenes.titel(p, Component.translatable("vadsrijk.huisdieren.nieuw.titel").withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable(h.naamKey()), 60, 0xFF7FC0);
        poefBij(p.level(), p.position().add(0, 0.6, 0), 1.0);
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1.4f);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.6f);
    }

    /** A little pink poof (pets appearing, teleporting, unlocking). */
    static void poefBij(ServerLevel sl, Vec3 v, double grootte) {
        sl.sendParticles(ParticleTypes.CLOUD, v.x, v.y, v.z, (int) (10 * grootte), 0.3 * grootte, 0.25 * grootte, 0.3 * grootte, 0.02);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, v.x, v.y + 0.2, v.z, (int) (12 * grootte), 0.35 * grootte, 0.3 * grootte, 0.35 * grootte, 0.03);
        sl.sendParticles(ParticleTypes.HEART, v.x, v.y + 0.6, v.z, Math.max(1, (int) (2 * grootte)), 0.2, 0.1, 0.2, 0);
    }

    // ---- walking along -----------------------------------------------------------------------------------------------------------

    @Nullable
    public static HuisdierEntity uit(ServerPlayer p) {
        HuisdierEntity e = UIT.get(p.getUUID());
        return e != null && !e.isRemoved() ? e : null;
    }

    /** Sends the pet home (with a poof when {@code poef}). */
    static void weg(ServerPlayer p, boolean poef) {
        HuisdierEntity e = UIT.remove(p.getUUID());
        if (e != null && !e.isRemoved()) {
            if (poef && e.level() instanceof ServerLevel sl) {
                poefBij(sl, e.position().add(0, 0.2, 0), 0.6);
                sl.playSound(null, e.blockPosition(), SoundEvents.SLIME_JUMP_SMALL, SoundSource.NEUTRAL, 0.6f, 1.8f);
            }
            e.discard();
        }
    }

    /**
     * Server, every 10 ticks per player: makes sure the right pet walks along (or none). Spawns it next to you when missing (after a
     * restart, a chunk unload, a teleport far away), swaps it when you chose another, sends it home outside the Vadsrijk.
     */
    static void bijwerken(ServerPlayer p) {
        Huisdier h = Vadsrijk.in(p) && p.isAlive() && !p.isSpectator() ? actief(p) : null;
        HuisdierEntity e = uit(p);
        if (h == null) {
            if (e != null) {
                weg(p, Vadsrijk.in(p));
            }
            return;
        }
        if (e != null && e.level() == p.level() && e.soortId().equals(h.id())) {
            return;
        }
        weg(p, e != null && e.level() == p.level());
        roep(p, h);
    }

    /** N-EXTRA: the pet's nameplate, "&lt;pet&gt; van &lt;player&gt;" (lang {@code vadsrijk.huisdieren.naam_van}). */
    public static Component naamVan(Huisdier h, Component speler) {
        return Component.translatable("vadsrijk.huisdieren.naam_van", Component.translatable(h.naamKey()), speler);
    }

    /** Gametests (other packages): summon this pet for the player right here. */
    @Nullable
    public static HuisdierEntity roepVoorTest(ServerPlayer p, Huisdier h) {
        return roep(p, h);
    }

    @Nullable
    static HuisdierEntity roep(ServerPlayer p, Huisdier h) {
        ServerLevel sl = p.level();
        HuisdierEntity e = HuisdierenRegistry.HUISDIER.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (e == null) {
            return null;
        }
        Vec3 plek = HuisdierEntity.plekBij(p);
        e.snapTo(plek.x, plek.y, plek.z, p.getYRot(), 0);
        e.zet(h.id(), p.getUUID());
        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(e, "huisdier:" + p.getUUID());   // U-DUBBEL: one pet per player (a hidden old one is replaced)
        e.setCustomName(naamVan(h, p.getName()));   // N-EXTRA: "Mini Koning Klodder van Juiced" (shown when you look at it)
        sl.addFreshEntity(e);
        UIT.put(p.getUUID(), e);
        poefBij(sl, plek.add(0, 0.2, 0), 0.6);
        return e;
    }

    static void tick(MinecraftServer server) {
        long nu = server.getTickCount();
        if (nu % 10 == 0) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                bijwerken(p);
                if (nu % 200 == 0 && Vadsrijk.in(p)) {
                    inhalen(p);
                }
            }
        }
        for (Moment m : MOMENTEN) {
            ServerPlayer p = server.getPlayerList().getPlayer(m.speler());
            if (p == null) {
                MOMENTEN.remove(m);
            } else if (p.level().getGameTime() >= m.op()) {
                MOMENTEN.remove(m);
                Huisdier h = van(m.id());
                if (h != null) {
                    toonMoment(p, h);
                }
            }
        }
    }

    static void uitgelogd(ServerPlayer p) {
        weg(p, false);
    }

    /** (tests) title cards still waiting for this player. */
    static int wachtendeMomenten(ServerPlayer p) {
        return (int) MOMENTEN.stream().filter(m -> m.speler().equals(p.getUUID())).count();
    }

    // ---- the screen ----------------------------------------------------------------------------------------------------------------

    /** Opens (or refreshes) the pet screen. Only in the Vadsrijk. */
    public static void scherm(ServerPlayer p) {
        if (!Vadsrijk.in(p)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.huisdieren.alleen_rijk").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        inhalen(p);
        stuurScherm(p);
    }

    static void stuurScherm(ServerPlayer p) {
        CompoundTag d = new CompoundTag();
        d.putString("bezit", String.join(",", bezit(p)));
        Huisdier a = actief(p);
        d.putString("actief", a == null ? "" : a.id());
        nl.juiced.guhs.feature.vadsrijk.rijdier.Maatjes.schermData(p, d);   // T-RIJDIER: pet levels + Vahoegje's stage
        HuisdierPayloads.stuur(p, new HuisdierPayloads.Scherm(d));
    }

    static void doe(ServerPlayer p, String actie, String id) {
        switch (actie) {
            case "open" -> scherm(p);
            case "kies" -> {
                if (kies(p, id)) {
                    Huisdier h = van(id);
                    if (h != null) {
                        p.sendOverlayMessage(Component.translatable("vadsrijk.huisdieren.mee", Component.translatable(h.naamKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                    bijwerken(p);
                    stuurScherm(p);
                }
            }
            case "geen" -> {
                Huisdier oud = actief(p);
                kies(p, null);
                if (oud != null) {
                    p.sendOverlayMessage(Component.translatable("vadsrijk.huisdieren.thuis", Component.translatable(oud.naamKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                bijwerken(p);
                stuurScherm(p);
            }
            default -> {
            }
        }
    }
}
