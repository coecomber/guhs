package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.erfenis.kaart.KaartData;
import nl.juiced.guhs.feature.erfenis.kaart.KaartNieuws;

/**
 * De Erfenis, the server side: going there and back, the actions of the overview, the village's clock (production,
 * builders, residents: {@link #bijwerken}), the protection of the dimension, collecting by walking up to a building. All
 * checks happen here against the base of the plot the player STANDS on; the client is never trusted (only the owner
 * changes a base, everyone else gets a read-only overview).
 */
public final class Erfenis {
    private Erfenis() {
    }

    // =====================================================================================================================
    // where
    // =====================================================================================================================

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(Plot.DIM);
    }

    public static boolean in(@Nullable Entity e) {
        return e != null && e.level().dimension() == Plot.DIM;
    }

    /** The base of the plot this player stands on (in the dimension), or null. */
    @Nullable
    public static Base onder(ServerPlayer p) {
        if (!in(p)) {
            return null;
        }
        return ErfenisData.get(p.level().getServer()).opCel(Plot.cel(p.getBlockX()), Plot.cel(p.getBlockZ()));
    }

    /** Is somebody in this base's cell (so its chunks are loaded and a change can be stamped right away)? */
    public static boolean bezocht(MinecraftServer server, Base b) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (in(p) && Plot.cel(p.getBlockX()) == b.cx && Plot.cel(p.getBlockZ()) == b.cz) {
                return true;
            }
        }
        return false;
    }

    /** Puts the whole base into the world now (phase 2: after generating an NPC base, or before a visit). */
    public static void stempel(MinecraftServer server, Base b) {
        ServerLevel level = level(server);
        if (level != null) {
            Stempel.stempel(level, Plot.oorsprong(b.cx, b.cz), b);
            ErfenisData.get(server).setDirty();
        }
    }

    /** One building of a base into the world (or, with nobody there, remember to stamp on the next visit). */
    static void stempelGebouw(MinecraftServer server, Base b, Gebouw g) {
        ServerLevel level = level(server);
        if (level == null) {
            return;
        }
        if (b.moetStempelen) {
            stempel(server, b);
        } else {
            Stempel.gebouw(level, Plot.oorsprong(b.cx, b.cz), b, g);
        }
    }

    // =====================================================================================================================
    // there and back
    // =====================================================================================================================

    /** Takes a player to the plot of this base (their own or, phase 2, somebody else's); remembers the way back. */
    public static boolean gaNaar(ServerPlayer p, Base b) {
        MinecraftServer server = p.level().getServer();
        ServerLevel level = level(server);
        if (level == null) {
            return false;
        }
        ErfenisData data = ErfenisData.get(server);
        if (!in(p)) {
            data.zetTerug(p.getUUID(), new ErfenisData.Terug(p.level().dimension().identifier().toString(), p.getX(), p.getY(), p.getZ(),
                    p.getYRot(), p.getXRot()));
        }
        bijwerken(server, b, false);
        if (b.moetStempelen) {
            stempel(server, b);
        }
        BlockPos a = veiligeAankomst(level, b);
        p.teleport(new TeleportTransition(level, Vec3.atBottomCenterOf(a), Vec3.ZERO, 180f, 20f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        level.playSound(null, a, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
        p.sendSystemMessage(Component.translatable(eigen(p, b) ? "erfenis.welkom.eigen" : "erfenis.welkom.ander", b.naam));
        if (eigen(p, b) && !b.aangekomen) {
            b.aangekomen = true;
            data.setDirty();
            ErfenisMomenten.melding(server, p.getUUID(), "aangekomen");
        }
        return true;
    }

    /** Back to where the player was before the dimension (or their respawn point). */
    public static void terug(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        ErfenisData data = ErfenisData.get(server);
        ErfenisData.Terug t = data.terug(p.getUUID());
        TeleportTransition tr = null;
        if (t != null) {
            Identifier id = Identifier.tryParse(t.dim());
            ServerLevel level = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (level != null && level.dimension() != Plot.DIM) {
                tr = new TeleportTransition(level, new Vec3(t.x(), t.y(), t.z()), Vec3.ZERO, t.yaw(), t.pitch(), TeleportTransition.DO_NOTHING);
            }
        }
        if (tr == null) {
            tr = p.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        }
        data.zetTerug(p.getUUID(), null);
        p.teleport(tr);
        p.resetFallDistance();
    }

    /** This player's own base (made on first use in the next free cell; then the world map's news says so). */
    /**
     * May this player go to their own erfenis (overview key, /guhs erfenis)? Only after buying it with the makelaar's letter
     * ({@code koppeling.Makelaarsbrief}); creative players always (dev). A player who already stands in the dimension may.
     */
    public static boolean magErfenis(ServerPlayer p) {
        return p.isCreative() || in(p) || nl.juiced.guhs.feature.erfenis.koppeling.Makelaarsbrief.gekocht(p.level().getServer(), p.getUUID());
    }

    public static Base eigenBase(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        ErfenisData data = ErfenisData.get(server);
        boolean nieuw = data.van(p.getUUID()) == null;
        long nu = System.currentTimeMillis();
        Base b = data.vanSpeler(p.getUUID(), p.getGameProfile().name(), nu);
        if (nieuw) {
            KaartData.get(server).meld(new KaartNieuws("erfenis.kaart.nieuws.speler_erfenis", List.of(b.naam), nu));
        }
        return b;
    }

    static boolean eigen(ServerPlayer p, Base b) {
        return !b.npc && p.getUUID().equals(b.eigenaar);
    }

    // =====================================================================================================================
    // the village's clock
    // =====================================================================================================================

    /**
     * Catches the village up to now (production, finished jobs, residents) and acts on what happened: stamps finished
     * buildings and cleared plukjes when somebody is there (else on the next visit), fires the story moments, tells the
     * owner (toasts, when {@code meld}). True: something changed that the viewers should get.
     */
    public static boolean bijwerken(MinecraftServer server, Base b, boolean meld) {
        if (b.npc) {
            return false;
        }
        ErfenisData data = ErfenisData.get(server);
        long nu = System.currentTimeMillis();
        if (data.snel) {   // (dev: every job done now)
            b.bouwen.replaceAll(x -> x.eind() > nu ? new Base.Bouw(x.soort(), x.ref(), x.naar(), x.start(), nu) : x);
        }
        if (b.eigenaar != null) {
            b.vk = Math.max(0, ErfenisVadskracht.huidig().beschikbaar(server, b.eigenaar)) + data.devVk;
        }
        int oudeBewoners = b.bewoners;
        b.bijwerken(nu);
        boolean veranderd = !b.klaar.isEmpty() || !b.momenten.isEmpty() || b.bewoners != oudeBewoners;
        boolean bezocht = bezocht(server, b);
        ServerLevel level = level(server);
        for (Base.Bouw k : b.klaar) {
            if (level == null) {
                break;
            }
            Gebouw g = k.soort() == Base.Bouw.GEBOUW ? b.gebouw(k.ref()) : null;
            if (!bezocht) {
                b.moetStempelen = true;
            } else if (k.soort() == Base.Bouw.PLUKJE) {
                if (!b.moetStempelen) {
                    Stempel.plukje(level, Plot.oorsprong(b.cx, b.cz), b, k.ref());
                }
            } else if (g != null) {
                stempelGebouw(server, b, g);
            }
            if (g != null && g.type == GebouwType.HOEVE) {
                KaartData.get(server).meld(new KaartNieuws("erfenis.kaart.nieuws.speler_stadhuis", List.of(b.naam, Integer.toString(g.level)), nu));
            }
        }
        b.klaar.clear();
        meldKnus(b);
        if (b.eigenaar != null) {
            for (String m : List.copyOf(b.momenten)) {
                ErfenisMomenten.melding(server, b.eigenaar, m);
            }
            ServerPlayer eigenaar = server.getPlayerList().getPlayer(b.eigenaar);
            if (meld && eigenaar != null) {
                for (Base.Fout f : b.meldingen) {
                    melding(eigenaar, f.key(), true, f.args());
                }
            }
        }
        b.momenten.clear();
        b.meldingen.clear();
        if (veranderd) {
            data.setDirty();
        }
        return veranderd;
    }

    /** Fires {@code knusheid:<totaal>} when the Knusheid changed since the last time. */
    static void meldKnus(Base b) {
        int k = b.knusheid();
        if (k != b.knusGemeld) {
            b.knusGemeld = k;
            b.momenten.add("knusheid:" + k);
        }
        if (k > b.knusRecord) {   // deco slice: a milestone reached for the first time
            for (int m : Knusheid.MIJLPALEN) {
                if (b.knusRecord < m && k >= m) {
                    b.meldingen.add(new Base.Fout("erfenis.knus.mijlpaal." + m, Integer.toString(m)));
                }
            }
            b.knusRecord = k;
        }
    }

    /** Is a job finished or a resident due (so this base must be caught up now, even with nobody there)? */
    private static boolean teDoen(Base b, long nu) {
        for (Base.Bouw x : b.bouwen) {
            if (x.eind() <= nu) {
                return true;
            }
        }
        return b.volgendeIntrek > 0 && b.volgendeIntrek <= nu;
    }

    /** For the Bouwkeet mix when collecting. */
    private static final java.util.Random RNG = new java.util.Random();

    /** Players walking up to a building collect it (once every few seconds). */
    private static final Map<UUID, Long> LOOP_OPHALEN = new HashMap<>();

    /** Every second: bases with something due or with visitors, the logé stand-ins, walking up to collect, falling off. */
    static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (!openStraks.isEmpty()) {
            for (var it = openStraks.entrySet().iterator(); it.hasNext();) {
                var e = it.next();
                if (server.getTickCount() >= e.getValue()) {
                    it.remove();
                    ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                    if (p != null && in(p)) {
                        openOverzicht(p);
                    }
                }
            }
        }
        if (server.getTickCount() % 20 != 7 || level(server) == null) {
            return;
        }
        ErfenisData data = ErfenisData.get(server);
        long nu = System.currentTimeMillis();
        boolean opfrissen = server.getTickCount() % 600 == 7;   // (every 30 s the viewers get a fresh copy anyway)
        for (Base b : new ArrayList<>(data.alleBases())) {
            if (b.npc) {
                continue;
            }
            boolean bezocht = bezocht(server, b);
            boolean veranderd = (teDoen(b, nu) || data.snel || bezocht) && bijwerken(server, b, true);
            if (veranderd || bezocht && opfrissen) {
                syncKijkers(server, b);
            }
        }
        Guhpension.tick(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!in(p)) {
                continue;
            }
            Base b = onder(p);
            if (b != null && b.moetStempelen) {
                stempel(server, b);
            }
            if (b != null && eigen(p, b)) {
                loopOphalen(server, p, b);
            }
            if (p.getY() < Plot.Y - 24) {   // fell off the island: back in the middle
                Base thuis = b != null ? b : data.van(p.getUUID());
                if (thuis != null) {
                    p.teleport(new TeleportTransition((ServerLevel) p.level(), Vec3.atBottomCenterOf(veiligeAankomst((ServerLevel) p.level(), thuis)),
                            Vec3.ZERO, 180f, 20f, TeleportTransition.DO_NOTHING));
                    p.resetFallDistance();
                } else {
                    terug(p);
                }
            }
        }
    }

    /** Standing right next to (or in) a building with something ready collects it ("nice extra": no need to open the overview). */
    private static void loopOphalen(MinecraftServer server, ServerPlayer p, Base b) {
        long nu = System.currentTimeMillis();
        if (LOOP_OPHALEN.getOrDefault(p.getUUID(), 0L) > nu) {
            return;
        }
        BlockPos o = Plot.oorsprong(b.cx, b.cz);
        int tx = p.getBlockX() - o.getX(), tz = p.getBlockZ() - o.getZ();
        for (Gebouw g : b.gebouwen) {
            if (!g.type.maakt() || g.level <= 0) {
                continue;
            }
            if (tx >= g.x - 1 && tx <= g.x + g.b() && tz >= g.z - 1 && tz <= g.z + g.d()) {
                if (g.klaar() >= 1) {
                    Object r = b.ophalen(g.id, RNG);
                    if (r instanceof Map<?, ?> m) {
                        p.sendOverlayMessage(Component.translatable("erfenis.ok.opgehaald_lopen", ophaalTekst(m), Component.translatable(g.type.naamKey())));
                        geluid(p, SoundEvents.EXPERIENCE_ORB_PICKUP);
                        ErfenisData.get(server).setDirty();
                        syncKijkers(server, b);
                    } else if (r instanceof Base.Fout f && "erfenis.fout.vol".equals(f.key())) {
                        p.sendOverlayMessage(Component.translatable(f.key()));
                    }
                    LOOP_OPHALEN.put(p.getUUID(), nu + 4000);
                }
                return;
            }
        }
    }

    /** "12 Eieren, 6 Veren" for a collect message. */
    static Component ophaalTekst(Map<?, ?> m) {
        net.minecraft.network.chat.MutableComponent t = Component.empty();
        boolean eerste = true;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (!eerste) {
                t.append(", ");
            }
            eerste = false;
            t.append(e.getValue() + " ").append(Component.translatable(((Goed) e.getKey()).naamKey()));
        }
        return t;
    }

    /** The arrival spot, on top of whatever stands there now (a building may stand on it). */
    static BlockPos veiligeAankomst(ServerLevel level, Base b) {
        // 1.9 afwerking: never inside a building (the stamp may still be queued, so look at the tiles, not the heightmap):
        // the arrival tile, else the nearest free open tile without deco (paths are fine), rings outward
        BlockPos a = Plot.aankomst(b.cx, b.cz);
        BlockPos o = Plot.oorsprong(b.cx, b.cz);
        int tx0 = a.getX() - o.getX(), tz0 = a.getZ() - o.getZ();
        for (int ruim = 1; ruim >= 0; ruim--) {   // first a tile with free tiles all around it (not under a roof edge), then any
            for (int r = 0; r <= 40; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        int tx = tx0 + dx, tz = tz0 + dz;
                        if (b.vrij(tx - ruim, tz - ruim, 1 + 2 * ruim, 1 + 2 * ruim, -1) && b.decoOp(tx, tz) == null) {
                            return o.offset(tx, 1, tz);
                        }
                    }
                }
            }
        }
        BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, a);
        return top.getY() > a.getY() ? top : a;
    }

    /** The players standing in this base's cell. */
    static List<ServerPlayer> kijkers(MinecraftServer server, Base b) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (in(p) && Plot.cel(p.getBlockX()) == b.cx && Plot.cel(p.getBlockZ()) == b.cz) {
                l.add(p);
            }
        }
        return l;
    }

    public static void syncKijkers(MinecraftServer server, Base b) {
        for (ServerPlayer p : kijkers(server, b)) {
            sync(p, b, false);
        }
    }

    /**
     * Opens the overview of the plot this player stands on (read-only when it is not their own base). Phase 2: teleport with
     * {@link #gaNaar} first, then call this.
     */
    public static void openOverzicht(ServerPlayer p) {
        actie(p, ErfenisPayloads.Actie.simpel(ErfenisPayloads.Actie.OPEN));
    }

    /** Players whose overview opens in a moment (after a teleport: the client first has to arrive), with the server tick. */
    private static final Map<UUID, Integer> openStraks = new HashMap<>();

    /** {@link #openOverzicht} in about a second (use right after {@link #gaNaar} into the dimension). */
    public static void openOverzichtStraks(ServerPlayer p) {
        openStraks.put(p.getUUID(), p.level().getServer().getTickCount() + 25);
    }

    /** Sends this base to the player (open = the overview opens; read-only unless the player owns it). */
    public static void sync(ServerPlayer p, Base b, boolean open) {
        Afwerking.spiegel(p.level().getServer(), b);   // Sherlock's rewards the village did not know yet
        CompoundTag t = b.save(true);
        if (!b.npc && b.eigenaar != null) {   // Sherlock: a case waits in the letterbox (the magnifier bubble above the bureau)
            t.putBoolean("ZaakKlaar", nl.juiced.guhs.feature.erfenis.sherlock.Sherlock.zaakKlaar(p.level().getServer(), b.eigenaar));
            t.putInt("Decobonnen", nl.juiced.guhs.feature.erfenis.koppeling.Dorpsbeloningen.decobonnen(p.level().getServer(), b.eigenaar));
        }
        PacketDistributor.sendToPlayer(p, new ErfenisPayloads.Sync(t, System.currentTimeMillis(), eigen(p, b), open,
                ErfenisData.get(p.level().getServer()).snel));
    }

    static void melding(ServerPlayer p, String key, boolean goed, String... args) {
        PacketDistributor.sendToPlayer(p, new ErfenisPayloads.Melding(key, List.of(args), goed));
    }

    private static void fout(ServerPlayer p, Base.Fout f) {
        melding(p, f.key(), false, f.args());
    }

    // =====================================================================================================================
    // the actions of the overview
    // =====================================================================================================================

    static void actie(ServerPlayer p, ErfenisPayloads.Actie a) {
        MinecraftServer server = p.level().getServer();
        if (level(server) == null) {
            return;
        }
        ErfenisData data = ErfenisData.get(server);
        switch (a.soort()) {
            case ErfenisPayloads.Actie.OPEN -> {
                Base b = onder(p);
                if (b != null) {
                    bijwerken(server, b, false);
                    sync(p, b, true);
                } else if (in(p)) {
                    melding(p, "erfenis.fout.geen_plot", false);
                } else if (!magErfenis(p)) {
                    melding(p, "erfenis.cmd.nog_geen_erfenis", false);
                } else {
                    PacketDistributor.sendToPlayer(p, new ErfenisPayloads.Vraag());
                }
                return;
            }
            case ErfenisPayloads.Actie.GA -> {
                if (!magErfenis(p)) {
                    melding(p, "erfenis.cmd.nog_geen_erfenis", false);
                    return;
                }
                gaNaar(p, eigenBase(p));
                if (a.id() == ErfenisPayloads.Actie.GA_EN_OPEN) {
                    openOverzichtStraks(p);
                }
                return;
            }
            case ErfenisPayloads.Actie.TERUG -> {
                if (in(p)) {
                    terug(p);
                }
                return;
            }
            default -> {
            }
        }
        Base b = onder(p);
        if (b == null) {
            return;
        }
        if (!eigen(p, b)) {
            melding(p, "erfenis.fout.niet_eigen", false);
            sync(p, b, false);
            return;
        }
        bijwerken(server, b, true);
        long nu = System.currentTimeMillis();
        long duur = data.snel ? 0 : -1;
        Base.Fout f = acties(server, p, b, a, nu, duur);
        if (f != null) {
            fout(p, f);
        }
        meldKnus(b);
        bijwerken(server, b, true);
        data.setDirty();
        syncKijkers(server, b);
    }

    /** The owner's actions on their own base (after a catch-up). Returns what went wrong, or null. */
    @Nullable
    private static Base.Fout acties(MinecraftServer server, ServerPlayer p, Base b, ErfenisPayloads.Actie a, long nu, long duur) {
        ServerLevel level = level(server);
        BlockPos o = Plot.oorsprong(b.cx, b.cz);
        switch (a.soort()) {
            case ErfenisPayloads.Actie.PLAATS -> {
                GebouwType t = GebouwType.van(a.tekst());
                if (t == null) {
                    return null;
                }
                Base.Fout f = b.plaats(t, a.x(), a.z(), a.rot(), nu, duur);
                if (f == null) {
                    Gebouw nieuw = b.gebouwen.get(b.gebouwen.size() - 1);
                    stempelGebouw(server, b, nieuw);
                    DecoActies.rand(server, b, nieuw.x, nieuw.z, nieuw.b(), nieuw.d());
                    geluid(p, SoundEvents.WOOD_PLACE);
                }
                return f;
            }
            case ErfenisPayloads.Actie.VERPLAATS -> {
                Gebouw g = b.gebouw(a.id());
                if (g == null) {
                    return null;
                }
                int ox = g.x, oz = g.z;
                Base.Fout f = b.verplaats(a.id(), a.x(), a.z());
                if (f == null) {
                    Afwerking.voorVerhuizing(level, b, g, o.offset(ox, 0, oz), g.b(), g.d());   // Opa's old kelder
                    if (!b.moetStempelen) {
                        Stempel.wis(level, o, b, ox, oz, g.b(), g.d());
                    }
                    stempelGebouw(server, b, g);
                    DecoActies.rand(server, b, g.x, g.z, g.b(), g.d());
                    geluid(p, SoundEvents.WOOD_PLACE);
                }
                return f;
            }
            case ErfenisPayloads.Actie.DRAAI -> {
                Gebouw g = b.gebouw(a.id());
                if (g == null) {
                    return null;
                }
                int ow = g.b(), oh = g.d();
                Base.Fout f = b.draai(a.id());
                if (f == null) {
                    Afwerking.voorVerhuizing(level, b, g, o.offset(g.x, 0, g.z), ow, oh);   // Opa's old kelder
                    if (!b.moetStempelen) {
                        Stempel.wis(level, o, b, g.x, g.z, ow, oh);
                    }
                    stempelGebouw(server, b, g);
                    DecoActies.rand(server, b, g.x, g.z, Math.max(ow, g.b()), Math.max(oh, g.d()));
                }
                return f;
            }
            case ErfenisPayloads.Actie.VERBETER -> {
                Base.Fout f = b.verbeter(a.id(), nu, duur);
                if (f == null) {
                    stempelGebouw(server, b, b.gebouw(a.id()));
                    geluid(p, SoundEvents.ANVIL_USE);
                }
                return f;
            }
            case ErfenisPayloads.Actie.OPHALEN -> {
                Object r = b.ophalen(a.id(), RNG);
                if (r instanceof Base.Fout fout) {
                    return fout;
                }
                melding(p, "erfenis.ok.opgehaald", true, ophaalTekst((Map<?, ?>) r).getString());
                geluid(p, SoundEvents.EXPERIENCE_ORB_PICKUP);
                return null;
            }
            case ErfenisPayloads.Actie.OPHALEN_ALLES -> {
                Map<Goed, Integer> som = new java.util.LinkedHashMap<>();
                boolean vol = false;
                for (Gebouw g : b.gebouwen) {
                    if (g.type.maakt() && g.klaar() >= 1) {
                        Object r = b.ophalen(g.id, RNG);
                        if (r instanceof Map<?, ?> m) {
                            m.forEach((k, v) -> som.merge((Goed) k, (Integer) v, Integer::sum));
                        } else if (r instanceof Base.Fout fout && "erfenis.fout.vol".equals(fout.key())) {
                            vol = true;
                        }
                    }
                }
                if (som.isEmpty()) {
                    return new Base.Fout(vol ? "erfenis.fout.vol" : "erfenis.fout.niks");
                }
                melding(p, "erfenis.ok.opgehaald", true, ophaalTekst(som).getString());
                geluid(p, SoundEvents.EXPERIENCE_ORB_PICKUP);
                return vol ? new Base.Fout("erfenis.fout.vol") : null;
            }
            case ErfenisPayloads.Actie.OPRUIMEN -> {
                Base.Fout f = b.opruimen(a.x(), nu, duur);
                if (f == null) {
                    geluid(p, SoundEvents.AZALEA_LEAVES_BREAK);
                }
                return f;
            }
            case ErfenisPayloads.Actie.KAVEL -> {
                Base.Fout f = b.kavelKopen(a.x());
                if (f == null) {
                    if (!b.moetStempelen && bezocht(server, b)) {
                        Stempel.kavel(level, o, b, a.x());
                    }
                    melding(p, "erfenis.ok.kavel", true, Integer.toString(b.kavelsGekocht()));
                    geluid(p, SoundEvents.PLAYER_LEVELUP);
                }
                return f;
            }
            case ErfenisPayloads.Actie.VERF -> {
                Base.Fout f = b.verf(a.id(), a.x());
                if (f == null) {
                    for (Gebouw g : b.gebouwen) {
                        if (a.id() < 0 || g.id == a.id()) {
                            stempelGebouw(server, b, g);
                        }
                    }
                    geluid(p, SoundEvents.DYE_USE);
                }
                return f;
            }
            case ErfenisPayloads.Actie.KIES -> {
                return b.kies(a.id(), a.x());
            }
            case ErfenisPayloads.Actie.KLEUR -> {
                return b.kleur(a.id(), a.x());
            }
            case ErfenisPayloads.Actie.SELECTIE -> {
                List<String> l = new ArrayList<>();
                for (String s : a.tekst().split(",")) {
                    if (!s.isBlank()) {
                        l.add(s.trim());
                    }
                }
                return b.selectie(a.id(), l);
            }
            case ErfenisPayloads.Actie.NAAR_HUIS -> {
                return naarHuis(server, p, b, Goed.van(a.tekst()), a.x(), nu);
            }
            case ErfenisPayloads.Actie.LOKET -> {
                return loket(p, b, a.tekst(), a.x());
            }
            case ErfenisPayloads.Actie.BUREAU -> {
                Gebouw g = b.gebouw(a.id());
                if (g == null || g.type != GebouwType.DETECTIVEBUREAU || g.level <= 0) {
                    return new Base.Fout("erfenis.fout.weg");
                }
                nl.juiced.guhs.feature.erfenis.sherlock.Sherlock.openBureau(p);
                return null;
            }
            case ErfenisPayloads.Actie.DECO -> {
                return DecoActies.doe(server, p, b, a);
            }
            case ErfenisPayloads.Actie.MARKT -> {
                Gebouw g = b.gebouw(a.id());
                if (g == null || g.type != GebouwType.MARKTPLEIN || g.level <= 0) {
                    return new Base.Fout("erfenis.fout.weg");
                }
                nl.juiced.guhs.feature.erfenis.kaart.Bestellingen.open(p);
                return null;
            }
            case ErfenisPayloads.Actie.PENSION -> {
                Base.Fout f = null;
                UUID id = null;
                try {
                    id = a.tekst().isEmpty() ? null : UUID.fromString(a.tekst());
                } catch (IllegalArgumentException e) {
                    return null;
                }
                switch (a.x()) {
                    case ErfenisPayloads.Actie.PENSION_STUUR -> f = id == null ? null : Guhpension.stuur(p, b, id);
                    case ErfenisPayloads.Actie.PENSION_HAAL -> f = id == null ? null : Guhpension.haal(p, b, id);
                    case ErfenisPayloads.Actie.PENSION_HELPT -> f = id == null ? null : Guhpension.helpt(b, id, a.id());
                    default -> {
                    }
                }
                if (f == null && a.x() == ErfenisPayloads.Actie.PENSION_STUUR) {
                    melding(p, "erfenis.ok.loge", true);
                }
                Guhpension.open(p, b);
                return f;
            }
            default -> {
                return null;
            }
        }
    }

    /** "Naar huis sturen": the goods leave the storage for the Leverkist (with the caps per hour; wool exempt). */
    @Nullable
    private static Base.Fout naarHuis(MinecraftServer server, ServerPlayer p, Base b, @Nullable Goed g, int n, long nu) {
        if (g == null) {
            return new Base.Fout("erfenis.fout.weg");
        }
        if (g.naarHuis() && !"*".equals(g.thuisItem) && g != Goed.KAARS && Goed.item(g.thuisItem) == null) {
            return new Base.Fout("erfenis.fout.item_ontbreekt", g.naamKey());
        }
        Object r = b.naarHuis(g, n, nu);
        if (r instanceof Base.Fout f) {
            return f;
        }
        List<ItemStack> spul = new ArrayList<>();
        for (Map.Entry<?, ?> e : ((Map<?, ?>) r).entrySet()) {
            net.minecraft.world.item.Item item = Goed.item((String) e.getKey());
            if (item == null) {
                item = net.minecraft.world.item.Items.BRICKS;   // (a Bouwkeet block that went missing: never lose the count)
            }
            int over = (Integer) e.getValue();
            while (over > 0) {
                ItemStack st = new ItemStack(item);
                int neem = Math.min(over, st.getMaxStackSize());
                st.setCount(neem);
                spul.add(st);
                over -= neem;
            }
        }
        // the Leverkist's own ceiling (koppeling, balans 9.4) has the last word: what may not go yet stays in the village
        nl.juiced.guhs.feature.erfenis.koppeling.LeverkistPlafond.Beperkt mag =
                nl.juiced.guhs.feature.erfenis.koppeling.LeverkistPlafond.beperk(server, b.eigenaar, spul, nu);
        int terug = 0;
        for (ItemStack st : mag.blijft()) {
            terug += st.getCount();
        }
        if (terug > 0) {
            b.verander(g, terug);
            if (g.capPerUur > 0) {
                b.leverTegoed.merge(g, (double) terug, Double::sum);
            }
        }
        if (n - terug <= 0) {
            return new Base.Fout("erfenis.fout.leverkist_cap", "0", Integer.toString(Math.max(g.capPerUur,
                    nl.juiced.guhs.feature.erfenis.koppeling.LeverkistPlafond.PER_UUR.getOrDefault(
                            nl.juiced.guhs.feature.erfenis.koppeling.LeverkistPlafond.soort(spul.get(0)), 0))));
        }
        LeverkistPost.verstuur(server, b.eigenaar, mag.mee());
        melding(p, "erfenis.ok.naar_huis", true, Integer.toString(n - terug), g.naamKey());
        geluid(p, SoundEvents.BUNDLE_INSERT);
        return null;
    }

    /** Every main-world item any upgrade asks for (the Loket only takes these). */
    private static java.util.Set<String> loketItems;

    static java.util.Set<String> loketItems() {
        if (loketItems == null) {
            java.util.Set<String> s = new java.util.LinkedHashSet<>();
            for (GebouwType t : GebouwType.values()) {
                for (int l = 1; l <= t.topLevel(); l++) {
                    s.addAll(Prijzen.gebouw(t, l).hoofdwereld().keySet());
                }
            }
            loketItems = s;
        }
        return loketItems;
    }

    /** The Loket: hand in main-world items from your pockets (only what an upgrade asks for). */
    @Nullable
    private static Base.Fout loket(ServerPlayer p, Base b, String itemId, int n) {
        if (b.besteLevel(GebouwType.LOKET) <= 0) {
            return new Base.Fout("erfenis.fout.loket_nodig");
        }
        net.minecraft.world.item.Item item = Goed.item(itemId);
        if (item == null || !loketItems().contains(itemId) || n <= 0) {
            return new Base.Fout("erfenis.fout.loket_niet");
        }
        int over = n, gegeven = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && over > 0; i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(item)) {
                int neem = Math.min(over, st.getCount());
                st.shrink(neem);
                over -= neem;
                gegeven += neem;
            }
        }
        if (gegeven <= 0) {
            return new Base.Fout("erfenis.fout.loket_geen", item.getDescriptionId());
        }
        b.loket.merge(itemId, gegeven, Integer::sum);
        melding(p, "erfenis.ok.loket", true, Integer.toString(gegeven), item.getDescriptionId());
        geluid(p, SoundEvents.ITEM_PICKUP);
        return null;
    }

    private static void geluid(ServerPlayer p, net.minecraft.sounds.SoundEvent s) {
        p.level().playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, 0.6f, 1.1f);
    }

    private static void geluid(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> s) {
        geluid(p, s.value());
    }

    // =====================================================================================================================
    // the dimension is safe and can only be changed through the overview
    // =====================================================================================================================

    static void opBreken(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
        if (event.getLevel() instanceof Level l && l.dimension() == Plot.DIM) {
            event.setCanceled(true);
        }
    }

    static void opPlaatsen(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player && event.getLevel() instanceof Level l && l.dimension() == Plot.DIM) {
            event.setCanceled(true);
        }
    }

    static void opKlikBlok(PlayerInteractEvent.RightClickBlock event) {
        if (!in(event.getEntity())) {
            return;
        }
        event.setCanceled(true);
        // in the own village: the Detectivebureau opens Sherlock's Speurboekje, the Marktplein the orders
        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer p
                && !(event.getItemStack().getItem() instanceof nl.juiced.guhs.feature.erfenis.sherlock.Vergrootglas)) {
            Base b = onder(p);
            if (b == null || !eigen(p, b)) {
                return;
            }
            BlockPos o = Plot.oorsprong(b.cx, b.cz);
            Gebouw g = b.op(event.getPos().getX() - o.getX(), event.getPos().getZ() - o.getZ());
            if (g != null && g.level > 0 && g.type == GebouwType.DETECTIVEBUREAU) {
                nl.juiced.guhs.feature.erfenis.sherlock.Sherlock.openBureau(p);
            } else if (g != null && g.level > 0 && g.type == GebouwType.MARKTPLEIN) {
                nl.juiced.guhs.feature.erfenis.kaart.Bestellingen.open(p);
            }
        }
    }

    static void opSlaBlok(PlayerInteractEvent.LeftClickBlock event) {
        if (in(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    static void opItem(PlayerInteractEvent.RightClickItem event) {
        if (in(event.getEntity()) && event.getItemStack().getItem() instanceof BucketItem) {
            event.setCanceled(true);
        }
    }

    static void opSchade(LivingIncomingDamageEvent event) {
        if (in(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
