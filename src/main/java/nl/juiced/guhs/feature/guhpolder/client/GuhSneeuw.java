package nl.juiced.guhs.feature.guhpolder.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.guhpolder.GuhpolderFeature;

/**
 * 2.10.1: guh-sneeuw, the Guhpolder's own snowfall. Where vanilla would draw its busy snow curtain (left out there by
 * mixin.client.LevelRendererMixin), a few soft flakes drift down around the camera instead: far fewer than vanilla,
 * swaying slowly in a light wind, each one fading in and melting away when it lands, and now and then a tiny pink guh head
 * (round ears, two dots for eyes) tumbling gently among them. Only while it rains/snows, only in the Guhpolder, only
 * under the open sky. The particles respect the particle setting (via level.addParticle).
 */
public final class GuhSneeuw {
    /** Flakes appear within this many blocks (horizontally) of the camera. */
    public static final int STRAAL = 16;
    /** New flakes per tick in full snowfall (vanilla fancy snow shows several hundred at once; this keeps ~1/5 of that). */
    public static final float PER_TICK = 2.0f;
    /** One in this many is a guh head. */
    public static final int GUHKOP_EEN_OP = 10;
    /** Sprite index of the guh head in particles/guh_sneeuw.json (0..3 are flakes). */
    static final int GUHKOP = 4;

    private static float rest;

    public static void init(IEventBus modBus) {
        modBus.addListener(GuhSneeuw::particles);
        NeoForge.EVENT_BUS.addListener(GuhSneeuw::tick);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        // (dy carries what it is: 1 = a guh head, 0 = a flake; dx/dz are the wind)
        event.registerSpriteSet(GuhpolderFeature.GUH_SNEEUW.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                new Vlok(level, x, y, z, dx, dy > 0.5, dz, sprites));
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            return;
        }
        float regen = level.getRainLevel(1f);
        if (regen <= 0.01f) {
            rest = 0;
            return;
        }
        Camera cam = mc.gameRenderer.getMainCamera();
        if (!cam.isInitialized()) {
            return;
        }
        Vec3 c = cam.position();
        // is there any polder near at all? (cheap: the camera's own spot and four around it)
        if (!polderBij(level, c)) {
            rest = 0;
            return;
        }
        RandomSource r = level.getRandom();
        // a soft wind that slowly turns
        double t = level.getGameTime() / 900.0;
        double windX = Math.sin(t) * 0.012 + 0.004, windZ = Math.cos(t * 0.7) * 0.009;
        float n = rest + PER_TICK * regen * (level.isThundering() ? 1.4f : 1f);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        while (n >= 1f) {
            n -= 1f;
            double x = c.x + (r.nextDouble() * 2 - 1) * STRAAL;
            double z = c.z + (r.nextDouble() * 2 - 1) * STRAAL;
            double y = c.y - 2 + r.nextDouble() * 16;
            p.set(x, y, z);
            if (y < level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ())) {
                continue;       // under a roof or in the ground
            }
            Holder<Biome> biome = level.getBiome(p);
            if (!biome.is(GuhpolderFeature.GUHPOLDER) || biome.value().getPrecipitationAt(p, level.getSeaLevel()) != Biome.Precipitation.SNOW) {
                continue;
            }
            boolean guhkop = r.nextInt(GUHKOP_EEN_OP) == 0;
            level.addParticle(GuhpolderFeature.GUH_SNEEUW.get(), x, y, z, windX, guhkop ? 1 : 0, windZ);
        }
        rest = n;
    }

    private static boolean polderBij(ClientLevel level, Vec3 c) {
        for (int[] d : new int[][]{{0, 0}, {STRAAL, 0}, {-STRAAL, 0}, {0, STRAAL}, {0, -STRAAL}}) {
            if (level.getBiome(BlockPos.containing(c.x + d[0], c.y, c.z + d[1])).is(GuhpolderFeature.GUHPOLDER)) {
                return true;
            }
        }
        return false;
    }

    /** One guh-sneeuw flake (or guh head): drifts down swaying, fades in, melts away where it lands. */
    static class Vlok extends SingleQuadParticle {
        private static final int INFADEN = 20, UITFADEN = 30, SMELTEN = 30;
        private final boolean guhkop;
        private final double windX, windZ, val, zwaai;
        private final float fase, freq, draai;
        private int geland = -1;

        Vlok(ClientLevel level, double x, double y, double z, double windX, boolean guhkop, double windZ, SpriteSet sprites) {
            super(level, x, y, z, sprites.first());
            this.guhkop = guhkop;
            this.windX = windX;
            this.windZ = windZ;
            setSprite(sprites.get(guhkop ? GUHKOP : random.nextInt(GUHKOP), GUHKOP));
            lifetime = 260 + random.nextInt(100);
            quadSize = guhkop ? 0.10f + random.nextFloat() * 0.03f : 0.05f + random.nextFloat() * 0.045f;
            val = guhkop ? 0.026 + random.nextDouble() * 0.008 : 0.032 + random.nextDouble() * 0.024;
            zwaai = guhkop ? 0.012 + random.nextDouble() * 0.008 : 0.014 + random.nextDouble() * 0.016;
            fase = random.nextFloat() * Mth.TWO_PI;
            freq = 0.035f + random.nextFloat() * 0.035f;
            draai = guhkop ? 0f : (random.nextFloat() - 0.5f) * 0.05f;
            roll = oRoll = guhkop ? 0f : random.nextFloat() * Mth.TWO_PI;
            gravity = 0f;
            friction = 1f;
            hasPhysics = true;
            alpha = 0f;
            xd = windX;
            yd = -val;
            zd = windZ;
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            oRoll = roll;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            if (geland >= 0) {          // landed: melt away where it lies
                geland++;
                alpha = Math.max(0f, 0.9f * (1f - (float) geland / SMELTEN));
                if (geland >= SMELTEN) {
                    remove();
                }
                return;
            }
            float t = age * freq + fase;
            xd = windX + Mth.sin(t) * zwaai;
            zd = windZ + Mth.cos(t * 0.8f) * zwaai * 0.7;
            yd = -val;
            move(xd, yd, zd);
            if (guhkop) {
                roll = Mth.sin(t * 0.9f) * 0.3f;        // a gentle wobble, face up
            } else {
                roll += draai;
            }
            if (onGround || !level.getFluidState(BlockPos.containing(x, y, z)).isEmpty()) {
                geland = 0;
            }
            float in = Math.min(1f, (float) age / INFADEN);
            float uit = Math.min(1f, (float) (lifetime - age) / UITFADEN);
            alpha = 0.9f * Math.min(in, uit);
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private GuhSneeuw() {
    }
}
