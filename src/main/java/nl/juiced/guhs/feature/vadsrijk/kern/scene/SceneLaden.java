package nl.juiced.guhs.feature.vadsrijk.kern.scene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * W-SPEL (1.10.0): the world is there before a cutscene shot starts. The user: "de eerste keer dat we een cutscene zien van een
 * plek waar we nog niet zijn geweest moet de wereld nog renderen". The cutscene camera is a client-only entity, so the server only
 * ever sent the chunks around the PLAYER; a camera far away looked at nothing (or at a world that popped in).
 * <p>
 * Server half (this class), for every {@link RijkScene} through {@link RijkScenes#speel}: when a camera keyframe (or a point on the
 * camera path) lies outside what the player's normal chunk view covers, the chunks around the camera are force-loaded (a ticket of
 * our own) and the player's chunk tracking view is moved to the camera for the length of the scene: the server then sends exactly
 * those chunks to the client (vanilla's own bookkeeping, {@code ChunkMap#applyChunkTrackingView}). The view follows the camera
 * along its path; when the scene ends (or another one replaces it, or the player leaves/changes dimension) the normal view around
 * the player comes back. Scenes whose camera stays inside the normal view change nothing here.
 * <p>
 * Client half ({@code SceneSpeler}): before the first frame of a shot it waits (black "laden" screen, at most 3 s) until the chunks
 * where the camera looks are received and built, and after a far scene it waits the same way for the player's surroundings.
 */
public final class SceneLaden {
    /** How long the client may wait before a shot starts (SceneSpeler.MAX_LADEN); the server keeps the view this much longer. */
    public static final int MARGE = 60;
    /** The camera view: at most this many chunks around the camera (the normal view distance when smaller). */
    static final int MAX_STRAAL = 10;
    /** Recentre when the camera is this many chunks away from the current centre. */
    static final int HERCENTREER = 3;

    private static final DeferredRegister<TicketType> TICKETS = DeferredRegister.create(Registries.TICKET_TYPE, Guhs.MODID);
    /** Loads the chunks around a far cutscene camera; times out by itself (refreshed while the scene runs). */
    static final DeferredHolder<TicketType, TicketType> TICKET = TICKETS.register("vr_scene_camera",
            () -> new TicketType(80L, TicketType.FLAG_LOADING));

    private record Lopend(RijkScene scene, long start, ResourceKey<Level> dim, int straal, int normaal, ChunkPos[] centrum) {
    }

    private static final Map<UUID, Lopend> LOPEND = new ConcurrentHashMap<>();

    private SceneLaden() {
    }

    public static void register(IEventBus modBus) {
        TICKETS.register(modBus);
        nl.juiced.guhs.world.TikWacht.post("SceneLaden:69", e -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> LOPEND.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> LOPEND.remove(e.getEntity().getUUID()));
    }

    /** The player's normal view distance (what ChunkMap#getPlayerViewDistance gives). */
    static int normaal(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        int sv = server == null ? 10 : server.getPlayerList().getViewDistance();
        return Mth.clamp(p.requestedViewDistance(), 2, Math.max(2, sv));
    }

    /** Where the camera is at tick t (as SceneSpeler glides: smoothstep between keyframes). */
    static Vec3 camera(RijkScene s, double t) {
        List<RijkScene.Punt> p = s.punten();
        RijkScene.Punt a = p.get(0), b = p.get(0);
        for (RijkScene.Punt q : p) {
            if (q.t() <= t) {
                a = q;
                b = q;
            } else {
                b = q;
                break;
            }
        }
        double f = b.t() == a.t() ? 1 : Mth.clamp((t - a.t()) / (b.t() - a.t()), 0, 1);
        f = f * f * (3 - 2 * f);
        return a.pos().lerp(b.pos(), f);
    }

    /** Points on the camera path: every keyframe, and every ~24 blocks between two keyframes. */
    static List<Vec3> pad(RijkScene s) {
        List<Vec3> l = new ArrayList<>();
        List<RijkScene.Punt> p = s.punten();
        for (int i = 0; i < p.size(); i++) {
            l.add(p.get(i).pos());
            if (i + 1 < p.size()) {
                Vec3 a = p.get(i).pos(), b = p.get(i + 1).pos();
                int n = (int) Math.min(64, a.distanceTo(b) / 24);
                for (int k = 1; k < n; k++) {
                    l.add(a.lerp(b, k / (double) n));
                }
            }
        }
        return l;
    }

    static int afstand(ChunkPos a, ChunkPos b) {
        return Math.max(Math.abs(a.x() - b.x()), Math.abs(a.z() - b.z()));
    }

    /** Is any point of this scene's camera path outside what the player's normal view covers (with a margin)? */
    public static boolean ver(ServerPlayer p, RijkScene s) {
        if (s.punten().isEmpty()) {
            return false;
        }
        int vd = normaal(p);
        ChunkPos speler = p.chunkPosition();
        for (Vec3 v : pad(s)) {
            if (afstand(speler, ChunkPos.containing(net.minecraft.core.BlockPos.containing(v))) > Math.max(1, vd - 2)) {
                return true;
            }
        }
        return false;
    }

    /** RijkScenes#speel: start (or stop) moving this player's chunk view along the scene's camera. */
    static void begin(ServerPlayer p, RijkScene s) {
        if (!(p.level() instanceof ServerLevel sl) || p instanceof net.neoforged.neoforge.common.util.FakePlayer) {
            return;
        }
        if (!ver(p, s)) {
            stop(p);
            return;
        }
        int vd = normaal(p);
        Lopend l = new Lopend(s, sl.getGameTime(), sl.dimension(), Math.min(vd, MAX_STRAAL), vd, new ChunkPos[1]);
        LOPEND.put(p.getUUID(), l);
        volg(p, sl, l, 0);
    }

    /** The scene is over for this player now (RijkScenes#vergeet): the normal view comes back. */
    static void stop(ServerPlayer p) {
        Lopend l = LOPEND.remove(p.getUUID());
        if (l != null) {
            herstel(p, l);
        }
    }

    /** Is this player's view moved to a cutscene camera right now? (tests) */
    public static boolean actief(ServerPlayer p) {
        return LOPEND.containsKey(p.getUUID());
    }

    /** The centre of the moved view, or null. (tests) */
    @Nullable
    public static ChunkPos centrum(ServerPlayer p) {
        Lopend l = LOPEND.get(p.getUUID());
        return l == null ? null : l.centrum()[0];
    }

    private static void volg(ServerPlayer p, ServerLevel sl, Lopend l, long t) {
        // the server runs up to MARGE ticks ahead of a client that waited for its chunks: follow the camera a little behind
        Vec3 cam = camera(l.scene(), Math.max(0, t - MARGE / 2));
        ChunkPos doel = ChunkPos.containing(net.minecraft.core.BlockPos.containing(cam));
        ChunkPos nu = l.centrum()[0];
        if (nu == null || afstand(nu, doel) >= HERCENTREER) {
            l.centrum()[0] = doel;
            nu = doel;
            sl.getChunkSource().addTicketWithRadius(TICKET.get(), doel, l.straal() + 2);
        } else if (t % 40 == 0) {
            sl.getChunkSource().addTicketWithRadius(TICKET.get(), nu, l.straal() + 2);   // (refresh: the ticket times out by itself)
        }
        if (!(p.getChunkTrackingView() instanceof ChunkTrackingView.Positioned v && v.center().equals(nu) && v.viewDistance() == l.straal())) {
            sl.getChunkSource().chunkMap.applyChunkTrackingView(p, ChunkTrackingView.of(nu, l.straal()));
        }
    }

    private static void herstel(ServerPlayer p, Lopend l) {
        if (!(p.level() instanceof ServerLevel sl) || sl.dimension() != l.dim() || p.isRemoved()) {
            return;
        }
        int vd = normaal(p);
        if (!(p.getChunkTrackingView() instanceof ChunkTrackingView.Positioned v && v.center().equals(p.chunkPosition()) && v.viewDistance() == vd)) {
            sl.getChunkSource().chunkMap.applyChunkTrackingView(p, ChunkTrackingView.of(p.chunkPosition(), vd));
        }
    }

    static void tick(MinecraftServer server) {
        if (LOPEND.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Lopend>> it = LOPEND.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Lopend> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Lopend l = e.getValue();
            if (p == null || p.isRemoved() || !(p.level() instanceof ServerLevel sl) || sl.dimension() != l.dim()) {
                it.remove();
                continue;
            }
            long t = sl.getGameTime() - l.start();
            if (t > l.scene().duur() + MARGE) {
                it.remove();
                herstel(p, l);
                continue;
            }
            volg(p, sl, l, t);
        }
    }
}
