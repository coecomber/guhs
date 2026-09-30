package nl.juiced.guhs.feature.hemel.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.hemel.HemelFeature;
import nl.juiced.guhs.feature.hemel.KnuffelhartBlockEntity;

/**
 * Client side of the Hemelkapelletje: the Knuffelhart's renderer (the beating heart), its music and heartbeat near you,
 * the sparkle particle, the wolkenhoeder's own model (its little cloud bobs), the revive screen, and whether the heart
 * beats for you ({@link #klopt}, from guhs:hemel_status).
 */
public final class HemelClient {
    /** Does the Knuffelhart beat for this player (the wolkenhoeder's questline is done)? */
    private static volatile boolean klopt;
    private static HemelMuziek muziek;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(HemelFeature.KNUFFELHART_BE.get(), KnuffelhartRenderer::new));
        modBus.addListener((ModelEvent.RegisterAdditional event) -> {
            event.register(KnuffelhartRenderer.HART);
            event.register(KnuffelhartRenderer.GLOED);
        });
        modBus.addListener((RegisterParticleProvidersEvent event) -> event.registerSpriteSet(HemelFeature.STERRETJE.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) -> new Sterretje(level, x, y, z, dx, dy, dz, sprites)));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            klopt = false;
            muziek = null;
        });
        // the wolkenhoeder: its own model (tools/features/hemel_npc.py); its little cloud bobs, the halo turns slowly
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.WOLKENHOEDER, Guhs.id("geo/entity/guh_npc_wolkenhoeder.geo.json"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.WOLKENHOEDER, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.07f;
            // (absolute values: these bones have no animation of their own, nothing resets them)
            bot.apply("hoeder_wolkje").ifPresent(b -> b.setPosY((float) Math.sin(t) * 0.35f));
            bot.apply("hoeder_aureool").ifPresent(b -> {
                b.setPosY((float) Math.sin(t + 1.1f) * 0.25f);
                b.setRotY((t * 0.3f) % ((float) Math.PI * 2f));
            });
        });
    }

    public static boolean klopt() {
        return klopt;
    }

    /** guhs:hemel_status */
    public static void status(boolean ja) {
        klopt = ja;
    }

    /** guhs:hemel_open: the revive screen (opened, or updated when it is already open). */
    public static void open(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof HemelScherm scherm) {
            scherm.update(data);
        } else {
            mc.setScreen(new HemelScherm(data));
        }
    }

    /** (KnuffelhartBlockEntity, client tick) the soft music around the heart, and its heartbeat when it beats for you. */
    public static void tickHart(KnuffelhartBlockEntity hart) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || hart.getLevel() == null) {
            return;
        }
        Vec3 c = Vec3.atCenterOf(hart.getBlockPos());
        double d = p.position().distanceTo(c);
        if (d < HemelMuziek.BEGIN && (muziek == null || muziek.isStopped())) {
            muziek = new HemelMuziek(hart);
            mc.getSoundManager().play(muziek);
        }
        if (klopt && d < 12 && hart.tijd % KnuffelhartRenderer.SLAG == 0) {
            hart.getLevel().playLocalSound(c.x, c.y, c.z, HemelFeature.HARTKLOP.get(), SoundSource.BLOCKS, 0.35f, 1f, false);
        }
    }

    /** A tiny four-pointed twinkle: floats a little, twinkles, fades. */
    static class Sterretje extends TextureSheetParticle {
        private final SpriteSet sprites;

        Sterretje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z);
            this.sprites = sprites;
            this.lifetime = 18 + random.nextInt(14);
            this.quadSize = 0.06f + random.nextFloat() * 0.05f;
            this.gravity = 0;
            this.hasPhysics = false;
            this.xd = dx * 0.5;
            this.yd = dy == 0 ? 0.01 : dy * 0.5;
            this.zd = dz * 0.5;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            setSpriteFromAge(sprites);
            xd *= 0.92;
            yd = yd * 0.92 + 0.001;
            zd *= 0.92;
            alpha = (1f - Mth.clamp((age - lifetime * 0.6f) / (lifetime * 0.4f), 0f, 1f)) * (0.7f + 0.3f * (float) Math.sin(age * 1.1));
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private HemelClient() {
    }
}
