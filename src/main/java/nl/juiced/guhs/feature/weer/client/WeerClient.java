package nl.juiced.guhs.feature.weer.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.weer.WeerFeature;
import nl.juiced.guhs.feature.weer.WeerPayloads;
import nl.juiced.guhs.feature.weer.WeerZones;
import nl.juiced.guhs.feature.weer.Weertje;

/**
 * 1.8.0, client: what you see of a Weer vadser's zone (WeerZones.Client holds the synced zones).
 * <ul>
 *   <li>rain, snow and thunder: the vanilla weather, but only in the zone. The client's own rain/thunder level is raised
 *       (or, in a sunny zone, lowered) towards the zone you stand in, softly at its edge, and the mixin
 *       {@code mixin.client.WeerNeerslagMixin} asks {@link #neerslag} which columns rain or snow (cold biomes snow, as in
 *       vanilla; the Guhmension's biomes, which never rain by themselves, rain here too);</li>
 *   <li>the guh-weertjes: their own falling particles around the camera ({@link WeerDeeltje}), the rainbow (an arc of
 *       coloured dust, opposite the sun), falling stars at night, and the pink fog of the knusmist.</li>
 * </ul>
 * The world's real weather is remembered while a zone changes it (the server may change it meanwhile: then that counts).
 */
public final class WeerClient {
    /** Particles per tick at full strength (around the camera, within {@link #STRAAL}). */
    private static final int STRAAL = 16;
    private static final float VOLG = 0.02f;
    /** The rainbow's colours, from the outside in. */
    private static final int[] REGENBOOG = {0xFF5A6E, 0xFFA04A, 0xFFE15A, 0x7EE07A, 0x6AB8FF, 0x8A7CFF, 0xC88CFF};

    // the zone at the camera (eased)
    @Nullable
    private static Weertje weertje;
    private static float sterkte, mist, oudMist;
    // the world's own weather while a zone holds the client's levels
    private static boolean houdVast;
    private static float echtRegen, echtDonder, gezetRegen, gezetDonder;
    private static float rest;
    private static final List<Ster> STERREN = new ArrayList<>();

    private record Ster(Vec3[] plek, Vec3 snelheid, int[] leeftijd) {
    }

    private WeerClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(WeerClient::deeltjes);
        WeerPayloads.zonesOntvanger = p -> WeerZones.Client.zet(p.plekken(), p.weertjes());
        WeerPayloads.menuOpener = p -> Minecraft.getInstance().setScreen(new WeerScherm(p));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(WeerClient::onFog);
        NeoForge.EVENT_BUS.addListener(WeerClient::onFogKleur);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            WeerZones.Client.wis();
            houdVast = false;
            weertje = null;
            sterkte = mist = oudMist = 0;
            STERREN.clear();
        });
    }

    private static void deeltjes(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(WeerFeature.PLUISJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                new WeerDeeltje(level, x, y, z, dx, dy, dz, sprites, WeerDeeltje.Soort.PLUISJE));
        event.registerSpriteSet(WeerFeature.ZEEPBEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                new WeerDeeltje(level, x, y, z, dx, dy, dz, sprites, WeerDeeltje.Soort.ZEEPBEL));
        event.registerSpriteSet(WeerFeature.HARTJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                new WeerDeeltje(level, x, y, z, dx, dy, dz, sprites, WeerDeeltje.Soort.HARTJE));
        event.registerSpriteSet(WeerFeature.BLOESEM.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                new WeerDeeltje(level, x, y, z, dx, dy, dz, sprites, WeerDeeltje.Soort.BLOESEM));
        event.registerSpriteSet(WeerFeature.STER.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                new WeerDeeltje(level, x, y, z, dx, dy, dz, sprites, WeerDeeltje.Soort.STER));
    }

    // =====================================================================================================================
    // the precipitation of a column (the mixin on WeatherEffectRenderer.getPrecipitationAt)
    // =====================================================================================================================

    /** What falls on this column, or null: vanilla decides (no zone here and the levels are the world's own). */
    @Nullable
    public static Biome.Precipitation neerslag(Level level, BlockPos pos) {
        if (WeerZones.Client.zones().isEmpty() && !houdVast) {
            return null;
        }
        WeerZones.Zone z = WeerZones.zoneOp(level, pos.getX() + 0.5, pos.getZ() + 0.5);
        if (z != null) {
            Weertje w = z.weertje();
            if (!w.regent) {
                return Biome.Precipitation.NONE;
            }
            if (w == Weertje.REGENBOOG) {
                return Biome.Precipitation.RAIN;
            }
            Biome biome = level.getBiome(pos).value();
            return biome.coldEnoughToSnow(pos, level.getSeaLevel()) ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN;
        }
        // outside the zones: while we raised the levels for a zone nearby, it must not rain where it really doesn't
        return houdVast && echtRegen < 0.2f ? Biome.Precipitation.NONE : null;
    }

    // =====================================================================================================================
    // every tick
    // =====================================================================================================================

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        oudMist = mist;
        if (level == null || mc.player == null) {
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 c = cam.isInitialized() ? cam.position() : mc.player.position();
        WeerZones.Zone zone = WeerZones.zoneOp(level, c.x, c.z);
        float doel = zone == null ? 0f : zone.gewicht(c.x, c.z);
        if (zone != null) {
            weertje = zone.weertje();
        }
        sterkte = Mth.approach(sterkte, doel, VOLG);
        if (sterkte <= 0.001f && zone == null) {
            weertje = null;
        }
        mist = Mth.approach(mist, weertje == Weertje.KNUSMIST ? sterkte : 0f, VOLG * 0.6f);
        niveaus(level);
        if (weertje != null && sterkte > 0.01f) {
            effecten(mc, level, c, weertje, sterkte);
        }
        sterren(level);
    }

    /** The client's rain and thunder levels: towards the zone, or back to the world's own. */
    private static void niveaus(ClientLevel level) {
        boolean nodig = weertje != null && weertje != Weertje.NORMAAL && sterkte > 0.001f;
        if (!nodig) {
            if (houdVast) {
                level.setRainLevel(echtRegen);
                level.setThunderLevel(echtDonder);
                houdVast = false;
            }
            return;
        }
        if (!houdVast) {
            echtRegen = level.rainLevel;
            echtDonder = level.thunderLevel;
            houdVast = true;
        } else {
            // (the server changed the weather while we held it: that is what it really is now)
            if (Math.abs(level.rainLevel - gezetRegen) > 1e-4f) {
                echtRegen = level.rainLevel;
            }
            if (Math.abs(level.thunderLevel - gezetDonder) > 1e-4f) {
                echtDonder = level.thunderLevel;
            }
        }
        float s = sterkte * sterkte * (3 - 2 * sterkte);
        if (weertje.regent) {
            float hoeveel = weertje == Weertje.REGENBOOG ? 0.55f : 1f;
            gezetRegen = Math.max(echtRegen, s * hoeveel);
            gezetDonder = weertje == Weertje.ONWEER ? Math.max(echtDonder, s) : echtDonder * (1 - s);
        } else {
            gezetRegen = echtRegen * (1 - s);
            gezetDonder = echtDonder * (1 - s);
        }
        level.setRainLevel(gezetRegen);
        level.setThunderLevel(gezetDonder);
    }

    private static void effecten(Minecraft mc, ClientLevel level, Vec3 c, Weertje w, float s) {
        RandomSource r = level.getRandom();
        switch (w) {
            case PLUISJES -> val(level, c, r, WeerFeature.PLUISJE.get(), 6f * s);
            case BELLEN -> val(level, c, r, WeerFeature.ZEEPBEL.get(), 3.5f * s);
            case HARTJES -> val(level, c, r, WeerFeature.HARTJE.get(), 4f * s);
            case BLOESEM -> val(level, c, r, WeerFeature.BLOESEM.get(), 5f * s);
            case KNUSMIST -> val(level, c, r, WeerFeature.PLUISJE.get(), 0.8f * s);
            case KNABBEL -> {
                if (r.nextInt(12) == 0) {
                    level.addParticle(ParticleTypes.CLOUD, c.x + (r.nextDouble() * 2 - 1) * 12, c.y + 10 + r.nextDouble() * 4,
                            c.z + (r.nextDouble() * 2 - 1) * 12, 0, 0, 0);
                }
            }
            case REGENBOOG -> regenboog(level, c, r, s);
            case STERREN -> {
                if (isNacht(level) && r.nextFloat() < 0.07f * s) {
                    nieuweSter(c, r);
                }
            }
            default -> {
            }
        }
    }

    /** Particles falling around the camera (under the open sky only). */
    private static void val(ClientLevel level, Vec3 c, RandomSource r, net.minecraft.core.particles.SimpleParticleType type, float perTick) {
        float n = rest + perTick;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        while (n >= 1f) {
            n -= 1f;
            double x = c.x + (r.nextDouble() * 2 - 1) * STRAAL;
            double z = c.z + (r.nextDouble() * 2 - 1) * STRAAL;
            double y = c.y - 2 + r.nextDouble() * 14;
            p.set(x, y, z);
            if (y < level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ())) {
                continue;       // under a roof or in the ground
            }
            if (WeerZones.zoneOp(level, x, z) == null) {
                continue;       // outside the zone
            }
            level.addParticle(type, x, y, z, 0, 0, 0);
        }
        rest = n;
    }

    /** A big arc of coloured dust, opposite the sun, that moves with you (as a real rainbow does). */
    private static void regenboog(ClientLevel level, Vec3 c, RandomSource r, float s) {
        long tijd = level.getOverworldClockTime() % 24000L;
        double kant = tijd < 6000 || tijd > 18000 ? -1 : 1;       // morning: in the west; afternoon: in the east
        double afstand = 46, straal = 30;
        int aantal = (int) (34 * s);
        for (int i = 0; i < aantal; i++) {
            double hoek = r.nextDouble() * Math.PI;
            int band = r.nextInt(REGENBOOG.length);
            double rr = straal - band * 0.9;
            double y = c.y - 6 + Math.sin(hoek) * rr;
            double z = c.z + Math.cos(hoek) * rr;
            double x = c.x + kant * afstand;
            level.addAlwaysVisibleParticle(new DustParticleOptions(REGENBOOG[band], 3.6f), true, x, y, z, 0, 0, 0);
        }
    }

    private static boolean isNacht(ClientLevel level) {
        long t = level.getOverworldClockTime() % 24000L;
        return t > 12800 && t < 23200;
    }

    private static void nieuweSter(Vec3 c, RandomSource r) {
        double hoek = r.nextDouble() * Math.PI * 2;
        Vec3 start = new Vec3(c.x + Math.cos(hoek) * (12 + r.nextDouble() * 30), c.y + 28 + r.nextDouble() * 18,
                c.z + Math.sin(hoek) * (12 + r.nextDouble() * 30));
        double richting = r.nextDouble() * Math.PI * 2;
        Vec3 v = new Vec3(Math.cos(richting) * 0.9, -0.45 - r.nextDouble() * 0.25, Math.sin(richting) * 0.9);
        STERREN.add(new Ster(new Vec3[]{start}, v, new int[]{0}));
    }

    /** The falling stars: a shining head that moves, with a glittering tail. */
    private static void sterren(ClientLevel level) {
        for (Iterator<Ster> it = STERREN.iterator(); it.hasNext(); ) {
            Ster s = it.next();
            if (++s.leeftijd()[0] > 34) {
                it.remove();
                continue;
            }
            Vec3 p = s.plek()[0].add(s.snelheid());
            s.plek()[0] = p;
            level.addAlwaysVisibleParticle(WeerFeature.STER.get(), true, p.x, p.y, p.z, 0, 0, 0);
            for (int i = 0; i < 2; i++) {
                level.addAlwaysVisibleParticle(ParticleTypes.END_ROD, true, p.x - s.snelheid().x * i * 0.5, p.y - s.snelheid().y * i * 0.5,
                        p.z - s.snelheid().z * i * 0.5, (level.getRandom().nextDouble() - 0.5) * 0.02, -0.01, (level.getRandom().nextDouble() - 0.5) * 0.02);
            }
        }
    }

    // =====================================================================================================================
    // the knusmist
    // =====================================================================================================================

    private static void onFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.ATMOSPHERIC) {
            return;
        }
        float m = Mth.lerp((float) event.getPartialTick(), oudMist, mist);
        if (m <= 0.001f) {
            return;
        }
        m = m * m * (3 - 2 * m);
        float far = event.getFarPlaneDistance();
        float doel = Math.min(far, 28f);
        event.setFarPlaneDistance(Mth.lerp(m, far, doel));
        event.setNearPlaneDistance(Mth.lerp(m, event.getNearPlaneDistance(), 0f));
    }

    private static void onFogKleur(ViewportEvent.ComputeFogColor event) {
        float m = mist;
        if (m <= 0.001f) {
            return;
        }
        m = Math.min(1f, m * 1.3f);
        event.setRed(Mth.lerp(m, event.getRed(), 0.98f));
        event.setGreen(Mth.lerp(m, event.getGreen(), 0.82f));
        event.setBlue(Mth.lerp(m, event.getBlue(), 0.90f));
    }
}
