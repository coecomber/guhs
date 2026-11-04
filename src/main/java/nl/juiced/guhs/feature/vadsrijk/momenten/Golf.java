package nl.juiced.guhs.feature.vadsrijk.momenten;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.wereld.Littekens;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * The recolour wave at {@code zone1_klaar} (VOORSTEL_EPISCHER_ZONE1 idea 30): the grey dragon scars no longer heal at once. When
 * the zone-1 story ends ({@link NpcApi#ZONE1_KLAAR}, during the cliffhanger scene), the client is told to keep its scars grey
 * ({@link MomentenPayloads.GolfKomt}); right after the cliffhanger a non-click scene plays: a pink wave rolls from the Zwamtempel
 * over the whole zone in 20 s ({@link GolfBaan}), the camera looks towards the temple, sees it come, turns and follows it away,
 * the scars recolour by distance as it passes (client tint, per player), cheered-up creatures cheer, and a title card ends it.
 * Per player: only this player's client recolours.
 */
public final class Golf {
    /** Ticks after the flag before the wave starts: the cliffhanger scene (260 ticks) has to end first. */
    static final int WACHT = 275;
    /** The zone edge as seen from the temple (the far scar ends in the south-west are ~1420 away). */
    static final double R_MAX = 1450;
    private static final Map<UUID, Long> GEPLAND = new HashMap<>();

    private Golf() {
    }

    static void init() {
        NpcApi.ZONE1_KLAAR.add(Golf::plan);
    }

    /** The zone is done for this player: keep the scars grey on the client, and start the wave after the cliffhanger. */
    public static void plan(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new MomentenPayloads.GolfKomt(WACHT + 200));
        GEPLAND.put(p.getUUID(), p.level().getGameTime() + WACHT);
    }

    static void tick(MinecraftServer server) {
        if (GEPLAND.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Long>> it = GEPLAND.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                it.remove();
            } else if (p.level().getGameTime() >= e.getValue()) {
                it.remove();
                if (Vadsrijk.in(p)) {
                    speel(p);
                }
            }
        }
    }

    static void vergeet(UUID id) {
        GEPLAND.remove(id);
    }

    /** The temple the wave starts from (the Zwamtempel's front hall). */
    public static Vec3 tempel() {
        BlockPos b = WereldPlekken.plek("zwamtempel_voorhal");
        return b == null ? new Vec3(330, 152, -398) : Vec3.atCenterOf(b);
    }

    /** Plays the wave for this player now (scene + client wave). Also the dev command. */
    public static void speel(ServerPlayer p) {
        Vec3 t = tempel();
        Vec3 oog = p.getEyePosition();
        double dx = oog.x - t.x, dz = oog.z - t.z, d = Math.sqrt(dx * dx + dz * dz);
        GolfBaan baan = new GolfBaan(t.x, t.z, d, R_MAX);
        RijkScenes.speel(p, scene(p, t));
        PacketDistributor.sendToPlayer(p, new MomentenPayloads.GolfStart(baan.tx(), baan.tz(), baan.afstand(), baan.rMax()));
        if (!Littekens.klaar(p)) {
            Littekens.zetKlaar(p, true);   // (dev command / a missed flag: the wave always ends healed)
        }
    }

    /**
     * The camera: high beside the player looking at the ground towards the temple, sees the wave come over the land, turns via
     * the side and follows it away. Camera points stay well above whatever is built or grown around them (the order temple on its
     * cap, the mushroom caps) and above the player; look targets are on the ground.
     */
    static RijkScene scene(ServerPlayer p, Vec3 tempel) {
        Vec3 oog = p.getEyePosition();
        Vec3 u = new Vec3(oog.x - tempel.x, 0, oog.z - tempel.z);
        u = u.lengthSqr() < 1 ? new Vec3(0, 0, 1) : u.normalize();
        Vec3 zij = new Vec3(-u.z, 0, u.x);
        ServerLevel sl = (ServerLevel) p.level();
        Vec3 o = new Vec3(oog.x, 0, oog.z);
        return RijkScene.maak("momenten:golf", GolfBaan.SCENE)
                .camera(0, hoog(sl, o.add(u.scale(30)).add(zij.scale(20)), oog.y, 30), grond(sl, o.subtract(u.scale(160))))
                .camera(GolfBaan.A, hoog(sl, o.add(u.scale(24)).add(zij.scale(16)), oog.y, 26), grond(sl, o.subtract(u.scale(120))))
                .camera(140, hoog(sl, o.add(u.scale(10)).add(zij.scale(22)), oog.y, 20), grond(sl, o.subtract(u.scale(35))))
                .camera(170, hoog(sl, o.add(zij.scale(24)), oog.y, 20), grond(sl, o.add(zij.scale(90))))
                .camera(GolfBaan.B, hoog(sl, o.subtract(u.scale(6)).add(zij.scale(14)), oog.y, 22), grond(sl, o.add(u.scale(90))))
                .camera(300, hoog(sl, o.add(u.scale(50)).add(zij.scale(6)), oog.y, 34), grond(sl, o.add(u.scale(200))))
                .camera(GolfBaan.DUUR, hoog(sl, o.add(u.scale(80)), oog.y, 48), grond(sl, o.add(u.scale(290))))
                .camera(GolfBaan.SCENE, hoog(sl, o.add(u.scale(84)), oog.y, 50), grond(sl, o.add(u.scale(290))))
                .titel("vadsrijk.momenten.golf.titel", "vadsrijk.momenten.golf.onder", GolfBaan.DUUR - 15, GolfBaan.SCENE - GolfBaan.DUUR + 10, 0xFF8FC8)
                .bouw();
    }

    /** The surface height (64 where the chunk isn't loaded). */
    private static int hoogte(ServerLevel sl, double x, double z) {
        BlockPos b = BlockPos.containing(x, 64, z);
        return sl.hasChunkAt(b) ? sl.getHeight(Heightmap.Types.MOTION_BLOCKING, b.getX(), b.getZ()) : 64;
    }

    /** A camera point: {@code extra} above the highest thing within 8 blocks, and at least 0.6 x extra above the player's eyes. */
    private static Vec3 hoog(ServerLevel sl, Vec3 v, double oogY, double extra) {
        int h = 0;
        for (int dx = -8; dx <= 8; dx += 8) {
            for (int dz = -8; dz <= 8; dz += 8) {
                h = Math.max(h, hoogte(sl, v.x + dx, v.z + dz));
            }
        }
        return new Vec3(v.x, Math.max(h + extra, oogY + extra * 0.6), v.z);
    }

    /** A look target on the ground (a mushroom cap or tower there doesn't pull the view up into the sky). */
    private static Vec3 grond(ServerLevel sl, Vec3 v) {
        return new Vec3(v.x, Math.min(hoogte(sl, v.x, v.z), 96) + 2, v.z);
    }
}
