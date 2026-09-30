package nl.juiced.guhs.feature.balto.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.balto.BaltoFeature;
import nl.juiced.guhs.feature.guhpolder.GuhpolderFeature;
import nl.juiced.guhs.feature.verhaal.VerhaalFeature;

/**
 * 3.0, client only: the blizzard look of the Sneeuwguhtoendra and the medicine ride (balto; balto-slee drives it with source
 * "tocht"): dense, gusty guh-snow (the Guhpolder's own guh-sneeuw flakes, with now and then a tiny pink guh head, here
 * blown sideways by the wind) and a white fog that closes in. Several sources can ask at once: the strongest wins
 * (CONTRACT_30 §4.10). The own source "toendra": in the tundra it always snows a little; when it rains elsewhere it storms
 * here (thunder: a real blizzard). Everything eases in and out (never a jump).
 */
public final class Sneeuwstorm {
    /** What one source asks (all 0..1; wind: -1..1 per axis, 1 = a strong wind). */
    public record Wens(float dichtheid, float windX, float windZ, float zicht) {
    }

    private static final Map<String, Wens> BRONNEN = new ConcurrentHashMap<>();
    /** Flakes per tick at full density (around the camera, within {@link #STRAAL}). */
    public static final float PER_TICK = 11f;
    public static final int STRAAL = 14;
    /** How far you still see in the thickest storm (blocks), and how fast the look follows (per tick). */
    public static final float ZICHT_MIN = 14f, VOLG = 0.025f;

    // the eased look now (and a tick ago for the fog)
    private static float dichtheid, windX, windZ, zicht, oudZicht;
    private static float rest;
    private static int windGeluid;

    /** Source bron wants a storm: dichtheid, wind and zicht (sight: 1 = almost no sight) all 0..1. */
    public static void zet(String bron, float dichtheid, float windX, float windZ, float zicht) {
        BRONNEN.put(bron, new Wens(Mth.clamp(dichtheid, 0, 1), Mth.clamp(windX, -1, 1), Mth.clamp(windZ, -1, 1), Mth.clamp(zicht, 0, 1)));
    }

    /** Source bron doesn't want a storm any more. */
    public static void uit(String bron) {
        BRONNEN.remove(bron);
    }

    /** The strongest wish now (or none: calm). */
    public static Wens sterkste() {
        Wens beste = new Wens(0, 0, 0, 0);
        for (Wens w : BRONNEN.values()) {
            if (w.dichtheid() + w.zicht() > beste.dichtheid() + beste.zicht()) {
                beste = w;
            }
        }
        return beste;
    }

    /** (the look now, for other client code and tests) */
    public static float dichtheid() {
        return dichtheid;
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(Sneeuwstorm::onFog);
        NeoForge.EVENT_BUS.addListener(Sneeuwstorm::onFogColour);
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        oudZicht = zicht;
        if (level == null || mc.player == null) {
            BRONNEN.clear();
            dichtheid = zicht = windX = windZ = 0;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        toendra(level, mc.player.blockPosition());
        Wens w = sterkste();
        dichtheid = Mth.approach(dichtheid, w.dichtheid(), VOLG);
        zicht = Mth.approach(zicht, w.zicht(), VOLG * 0.6f);
        windX = Mth.approach(windX, w.windX(), VOLG);
        windZ = Mth.approach(windZ, w.windZ(), VOLG);
        if (dichtheid > 0.01f) {
            vlokken(mc, level);
        } else {
            rest = 0;
        }
    }

    /** The tundra's own weather: always a few flakes, a storm when it rains (a blizzard in a thunderstorm). */
    private static void toendra(ClientLevel level, BlockPos pos) {
        if (!level.getBiome(pos).is(VerhaalFeature.SNEEUWGUHTOENDRA)) {
            uit("toendra");
            return;
        }
        float regen = level.getRainLevel(1f), donder = level.getThunderLevel(1f);
        float d = 0.12f + regen * 0.5f + donder * 0.38f;
        // the wind blows from the north-west, it turns slowly
        double t = level.getGameTime() / 1200.0;
        float kracht = 0.25f + regen * 0.45f + donder * 0.3f;
        zet("toendra", d, (float) (Math.cos(t) * 0.4 + 0.6) * kracht, (float) (Math.sin(t * 0.8) * 0.3 + 0.55) * kracht,
                regen * 0.45f + donder * 0.45f);
    }

    /** Guh-sneeuw flakes blown by the (gusty) wind around the camera. */
    private static void vlokken(Minecraft mc, ClientLevel level) {
        Camera cam = mc.gameRenderer.getMainCamera();
        if (!cam.isInitialized()) {
            return;
        }
        Vec3 c = cam.getPosition();
        RandomSource r = level.getRandom();
        long tijd = level.getGameTime();
        float vlaag = 1f + 0.55f * Mth.sin(tijd * 0.045f) + 0.3f * Mth.sin(tijd * 0.17f + 1.3f);     // gusts
        double wx = windX * 0.42 * vlaag, wz = windZ * 0.42 * vlaag;
        float n = rest + PER_TICK * dichtheid;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        while (n >= 1f) {
            n -= 1f;
            // (upwind a bit more: the flakes blow into view)
            double x = c.x + (r.nextDouble() * 2 - 1) * STRAAL - wx * 12;
            double z = c.z + (r.nextDouble() * 2 - 1) * STRAAL - wz * 12;
            double y = c.y - 3 + r.nextDouble() * 12;
            p.set(x, y, z);
            if (y < level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ())) {
                continue;       // under a roof or in the ground
            }
            boolean guhkop = r.nextInt(14) == 0;
            level.addParticle(GuhpolderFeature.GUH_SNEEUW.get(), x, y, z, wx * (0.8 + r.nextDouble() * 0.4), guhkop ? 1 : 0,
                    wz * (0.8 + r.nextDouble() * 0.4));
        }
        rest = n;
        // a soft gust of wind now and then
        if (--windGeluid <= 0 && dichtheid > 0.35f) {
            windGeluid = 60 + r.nextInt(80);
            mc.getSoundManager().play(new net.minecraft.client.resources.sounds.SimpleSoundInstance(BaltoFeature.WIND.get(), SoundSource.WEATHER,
                    0.25f + dichtheid * 0.55f, 0.85f + r.nextFloat() * 0.3f, r, c.x + wx * 20, c.y + 2, c.z + wz * 20));
        }
    }

    private static void onFog(ViewportEvent.RenderFog event) {
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE) {
            return;
        }
        float m = Mth.lerp((float) event.getPartialTick(), oudZicht, zicht);
        if (m <= 0.001f) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float doel = Mth.lerp(m, far, ZICHT_MIN);
        if (doel >= far) {
            return;
        }
        m = m * m * (3 - 2 * m);
        event.setFarPlaneDistance(Mth.lerp(m, far, doel));
        event.setNearPlaneDistance(Mth.lerp(m, event.getNearPlaneDistance(), 0f));
        event.setFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);
        event.setCanceled(true);
    }

    /** The fog turns snow-white in the storm. */
    private static void onFogColour(ViewportEvent.ComputeFogColor event) {
        float m = zicht;
        if (m <= 0.001f) {
            return;
        }
        m = Math.min(1f, m * 1.4f);
        event.setRed(Mth.lerp(m, event.getRed(), 0.90f));
        event.setGreen(Mth.lerp(m, event.getGreen(), 0.93f));
        event.setBlue(Mth.lerp(m, event.getBlue(), 0.98f));
    }

    private Sneeuwstorm() {
    }
}
