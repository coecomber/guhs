package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * De Erfenis (POC), the server side: going there and back, the actions of the overview, the builder's clock, the
 * protection of the dimension. All checks happen here against the base of the plot the player STANDS on; the client is
 * never trusted (only the owner changes a base, everyone else gets a read-only overview).
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
        BlockPos a = Plot.aankomst(b.cx, b.cz);
        p.teleport(new TeleportTransition(level, Vec3.atBottomCenterOf(a), Vec3.ZERO, 180f, 20f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        level.playSound(null, a, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
        p.sendSystemMessage(Component.translatable(eigen(p, b) ? "erfenis.welkom.eigen" : "erfenis.welkom.ander", b.naam));
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

    static boolean eigen(ServerPlayer p, Base b) {
        return !b.npc && p.getUUID().equals(b.eigenaar);
    }

    // =====================================================================================================================
    // the builder's clock
    // =====================================================================================================================

    /** Finishes the builder's job if its time has come (or "snel" is on); stamps it when somebody is there. True: something changed. */
    static boolean bijwerken(MinecraftServer server, Base b, boolean meld) {
        ErfenisData data = ErfenisData.get(server);
        long nu = System.currentTimeMillis();
        if (b.bouw == null || !(data.snel || nu >= b.bouw.eind())) {
            return false;
        }
        Gebouw g = b.afronden(data.snel ? Math.max(nu, b.bouw.eind()) : nu);
        data.setDirty();
        if (g != null) {
            if (bezocht(server, b)) {
                stempelGebouw(server, b, g);
            } else {
                b.moetStempelen = true;
            }
            if (meld) {
                for (ServerPlayer p : kijkers(server, b)) {
                    melding(p, "erfenis.ok.klaar", true, g.type.naamKey(), Integer.toString(g.level));
                }
            }
        }
        return true;
    }

    /** Every second: finished builds, and bases somebody walked onto that are behind. */
    static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 7 || level(server) == null) {
            return;
        }
        ErfenisData data = ErfenisData.get(server);
        for (Base b : new ArrayList<>(data.alleBases())) {
            if (bijwerken(server, b, true)) {
                syncKijkers(server, b);
            }
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!in(p)) {
                continue;
            }
            Base b = onder(p);
            if (b != null && b.moetStempelen) {
                stempel(server, b);
            }
            if (p.getY() < Plot.Y - 24) {   // fell off the island: back on the path
                Base thuis = b != null ? b : data.van(p.getUUID());
                if (thuis != null) {
                    p.teleport(new TeleportTransition((ServerLevel) p.level(), Vec3.atBottomCenterOf(Plot.aankomst(thuis.cx, thuis.cz)), Vec3.ZERO,
                            180f, 20f, TeleportTransition.DO_NOTHING));
                    p.resetFallDistance();
                } else {
                    terug(p);
                }
            }
        }
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

    /** Sends this base to the player (open = the overview opens; read-only unless the player owns it). */
    public static void sync(ServerPlayer p, Base b, boolean open) {
        PacketDistributor.sendToPlayer(p, new ErfenisPayloads.Sync(b.save(), System.currentTimeMillis(), eigen(p, b), open,
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
                } else {
                    PacketDistributor.sendToPlayer(p, new ErfenisPayloads.Vraag());
                }
                return;
            }
            case ErfenisPayloads.Actie.GA -> {
                gaNaar(p, data.vanSpeler(p.getUUID(), p.getGameProfile().name(), System.currentTimeMillis()));
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
        bijwerken(server, b, false);
        long nu = System.currentTimeMillis();
        long duur = data.snel ? 0 : -1;
        Base.Fout f = null;
        switch (a.soort()) {
            case ErfenisPayloads.Actie.PLAATS -> {
                GebouwType t = GebouwType.van(a.gebouw());
                if (t == null) {
                    return;
                }
                f = b.plaats(t, a.x(), a.z(), a.rot(), nu, duur);
                if (f == null) {
                    Gebouw g = b.gebouwen.get(b.gebouwen.size() - 1);
                    stempelGebouw(server, b, g);
                    geluid(p, SoundEvents.WOOD_PLACE);
                }
            }
            case ErfenisPayloads.Actie.LIJN -> f = lijn(server, p, b, a, nu);
            case ErfenisPayloads.Actie.VERPLAATS -> {
                Gebouw g = b.gebouw(a.id());
                if (g == null) {
                    return;
                }
                int ox = g.x, oz = g.z;
                f = b.verplaats(a.id(), a.x(), a.z());
                if (f == null) {
                    ServerLevel level = level(server);
                    if (!b.moetStempelen) {
                        Stempel.wis(level, Plot.oorsprong(b.cx, b.cz), ox, oz, g.type.maat);
                    }
                    stempelGebouw(server, b, g);
                    geluid(p, SoundEvents.WOOD_PLACE);
                }
            }
            case ErfenisPayloads.Actie.DRAAI -> {
                f = b.draai(a.id());
                if (f == null) {
                    stempelGebouw(server, b, b.gebouw(a.id()));
                }
            }
            case ErfenisPayloads.Actie.VERBETER -> {
                f = b.verbeter(a.id(), nu, duur);
                if (f == null) {
                    stempelGebouw(server, b, b.gebouw(a.id()));
                    geluid(p, SoundEvents.ANVIL_USE);
                }
            }
            case ErfenisPayloads.Actie.OPHALEN -> {
                Object r = b.ophalen(a.id(), nu);
                if (r instanceof Base.Fout fout) {
                    f = fout;
                } else {
                    melding(p, "erfenis.ok.opgehaald", true, String.valueOf(r));
                    geluid(p, SoundEvents.EXPERIENCE_ORB_PICKUP);
                }
            }
            default -> {
                return;
            }
        }
        if (f != null) {
            fout(p, f);
        }
        data.setDirty();
        if (data.snel) {
            bijwerken(server, b, true);
        }
        syncKijkers(server, b);
    }

    /** Walls in a straight line from (x, z) to (x2, z2) (the longer axis wins): as many as the limit and Bouwspul allow. */
    @Nullable
    private static Base.Fout lijn(MinecraftServer server, ServerPlayer p, Base b, ErfenisPayloads.Actie a, long nu) {
        GebouwType t = GebouwType.van(a.gebouw());
        if (t == null || t.maat != 1) {
            return null;
        }
        int dx = a.x2() - a.x(), dz = a.z2() - a.z();
        boolean langsX = Math.abs(dx) >= Math.abs(dz);
        int n = Math.min(Plot.MAAT, langsX ? Math.abs(dx) : Math.abs(dz));
        int stap = (langsX ? dx : dz) >= 0 ? 1 : -1;
        int gezet = 0;
        Base.Fout laatste = null;
        for (int i = 0; i <= n; i++) {
            int x = langsX ? a.x() + i * stap : a.x(), z = langsX ? a.z() : a.z() + i * stap;
            if (!b.vrij(x, z, 1, -1)) {
                continue;   // (already a wall there, or something else: skipped)
            }
            Base.Fout f = b.plaats(t, x, z, 0, nu, -1);
            if (f != null) {
                laatste = f;
                break;
            }
            stempelGebouw(server, b, b.gebouwen.get(b.gebouwen.size() - 1));
            gezet++;
        }
        if (gezet > 0) {
            geluid(p, SoundEvents.STONE_PLACE);
        }
        return laatste;
    }

    private static void geluid(ServerPlayer p, net.minecraft.sounds.SoundEvent s) {
        p.level().playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, 0.6f, 1.1f);
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
        if (in(event.getEntity())) {
            event.setCanceled(true);
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
