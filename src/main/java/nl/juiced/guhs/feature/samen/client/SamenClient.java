package nl.juiced.guhs.feature.samen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.samen.SamenBeloning;
import nl.juiced.guhs.feature.samen.SamenFeature;
import nl.juiced.guhs.feature.samen.SamenPayloads;

/**
 * Client side of samen (2.10): the zielsguh's sparkling pink heart next to its name (and now and then a sparkling heart
 * floating up from it), the big bff-knuffel heart above a guh and its player, the particles, and the synced copy of your
 * unlocked hartjes emotes (for the emote picker).
 */
public final class SamenClient {
    /** The pinks the zielsguh heart twinkles through (and a white glint now and then). */
    private static final int[] ROZE = {0xFF5FB4, 0xFF8AC8, 0xFFB3DA, 0xFF7ABF, 0xFFE3F1};

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterParticleProvidersEvent event) -> {
            event.registerSpriteSet(SamenFeature.ZIELSHARTJE.get(),
                    sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Hart(level, x, y, z, dx, dy, dz, sprites, sprites.get(random), false));
            event.registerSpriteSet(SamenFeature.BFF_HART.get(),
                    sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Hart(level, x, y, z, dx, dy, dz, sprites, sprites.get(random), true));
        });
        NeoForge.EVENT_BUS.addListener(SamenClient::naam);
        NeoForge.EVENT_BUS.addListener(SamenClient::tick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> SamenBeloning.Client.zet(0));
        SamenPayloads.bffOntvanger = p -> Minecraft.getInstance().execute(() -> bff(p.guh(), p.speler()));
    }

    /** Is this a zielsguh (level 3) that is out and about? */
    public static boolean isZielsguh(Entity e) {
        return e instanceof GuhEntity && BandVlaggen.heeft(e, BandVlaggen.ZIELSGUH) && !BandVlaggen.heeft(e, BandVlaggen.HUISJE_BINNEN);
    }

    /** The heart next to the name: its colour twinkles through soft pinks, with a sparkle now and then. */
    public static Component hart(long tijd, int id) {
        int fase = (int) ((tijd + id * 7L) / 5 % ROZE.length);
        boolean glinster = (tijd + id * 13L) % 60 < 6;
        MutableComponent hart = Component.literal("❤").withStyle(s -> s.withColor(TextColor.fromRgb(ROZE[fase])).withBold(true));
        return glinster ? Component.literal("✦").withStyle(s -> s.withColor(TextColor.fromRgb(0xFFF6FB))).append(hart) : hart;
    }

    private static void naam(RenderNameTagEvent.CanRender event) {
        if (isZielsguh(event.getEntity()) && Minecraft.getInstance().level != null && event.getContent() != null) {
            long tijd = Minecraft.getInstance().level.getGameTime();
            event.setContent(event.getContent().copy().append(" ").append(hart(tijd, event.getEntity().getId())));
        }
    }

    /** Now and then a sparkling heart floats up from a zielsguh close by. */
    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || mc.player == null) {
            return;
        }
        long t = level.getGameTime();
        for (Entity e : level.entitiesForRendering()) {
            if ((t + e.getId()) % 20 != 0 || !isZielsguh(e) || e.distanceToSqr(mc.player) > 48 * 48 || e.isInvisible()
                    || level.getRandom().nextInt(3) != 0) {
                continue;
            }
            level.addParticle(SamenFeature.ZIELSHARTJE.get(), e.getX() + (level.getRandom().nextDouble() - 0.5) * e.getBbWidth(),
                    e.getY() + e.getBbHeight() + 0.35, e.getZ() + (level.getRandom().nextDouble() - 0.5) * e.getBbWidth(), 0, 0.02, 0);
        }
    }

    /** The bff-knuffel: a big heart above the guh and its player, a ring of little hearts round them. */
    static void bff(int guhId, int spelerId) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity guh = level.getEntity(guhId), speler = level.getEntity(spelerId);
        if (guh == null || speler == null) {
            return;
        }
        double x = (guh.getX() + speler.getX()) / 2, z = (guh.getZ() + speler.getZ()) / 2;
        double y = Math.max(guh.getY() + guh.getBbHeight(), speler.getY() + speler.getBbHeight()) + 0.6;
        level.addParticle(SamenFeature.BFF_HART.get(), x, y, z, 0, 0.004, 0);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16;
            level.addParticle(BandFeature.HARTJE.get(), x + Math.cos(a) * 0.9, y - 0.8, z + Math.sin(a) * 0.9, Math.cos(a) * 0.04, 0.05,
                    Math.sin(a) * 0.04);
        }
    }

    /** The zielsguh's sparkling heart (small, twinkling, floats up and fades) or the big bff heart (grows, beats, lingers). */
    static class Hart extends SingleQuadParticle {
        private final boolean groot;
        private final SpriteSet sprites;
        private final float basis;

        Hart(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, TextureAtlasSprite sprite, boolean groot) {
            super(level, x, y, z, sprite);
            this.groot = groot;
            this.sprites = sprites;
            this.lifetime = groot ? 70 : 30 + random.nextInt(12);
            this.basis = groot ? 0.75f : 0.1f + random.nextFloat() * 0.04f;
            this.quadSize = groot ? 0.05f : basis;
            this.gravity = 0;
            this.hasPhysics = false;
            this.xd = dx;
            this.yd = dy == 0 ? 0.02 : dy;
            this.zd = dz;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            setSpriteFromAge(sprites);
            if (groot) {
                float groei = Mth.clamp(age / 8f, 0f, 1f);
                float klop = 1f + 0.12f * (float) Math.max(0, Math.sin(age * 0.45));   // a heartbeat
                quadSize = basis * groei * klop;
                alpha = 1f - Mth.clamp((age - 55) / 15f, 0f, 1f);
                yd = 0.004;
            } else {
                xd = Math.sin((age + x * 7) * 0.35) * 0.006;
                alpha = (1f - Mth.clamp((age - 20) / 14f, 0f, 1f)) * (0.75f + 0.25f * (float) Math.sin(age * 0.9));
            }
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private SamenClient() {
    }
}
