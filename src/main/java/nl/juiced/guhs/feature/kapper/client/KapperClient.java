package nl.juiced.guhs.feature.kapper.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.kapper.KapperFeature;
import nl.juiced.guhs.feature.kapper.KapperPayloads;
import nl.juiced.guhs.feature.kapper.Kapsel;
import nl.juiced.guhs.item.GuhClothingItem;

/**
 * Client side of the kapper: Kapper Krulletje's own model (a big pink curly hairdo with a comb in it, scissors in his
 * paw that snip now and then), the customers (the guh renderer: their hair is drawn by the clothes layer), the
 * particles (haarplukje, krulglitter), the tooltip of the kapsel items, and the two screens.
 */
public final class KapperClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(KapperClient::renderers);
        modBus.addListener(KapperClient::particles);
        NeoForge.EVENT_BUS.addListener(KapperClient::tooltip);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.KAPPERGUH, Guhs.id("geo/entity/guh_npc_kapperguh.geo.json"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.KAPPERGUH, (npc, state, bot) -> {
            // snip snip: every few seconds the scissors open and close a few times
            float t = (float) state.getAnimationTick();
            float phase = t % 70f;
            float open = phase < 16f ? (float) Math.abs(Math.sin(phase * 0.6f)) * 0.45f : 0f;
            bot.apply("kapper_schaar_blad_a").ifPresent(b -> b.setRotZ(open));
            bot.apply("kapper_schaar_blad_b").ifPresent(b -> b.setRotZ(-open));
            bot.apply("kapper_krullen").ifPresent(b -> b.setRotY((float) Math.sin(t * 0.05f) * 0.03f));
        });
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(KapperFeature.KAPPER_KLANT.get(), GuhRenderer::new);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(KapperFeature.HAARPLUKJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Plukje(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(KapperFeature.KRULGLITTER.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Glitter(level, x, y, z, sprites));
    }

    /** The kapsel items: permanent hair, not clothes. */
    private static void tooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof GuhClothingItem c && Kapsel.van(c.getClothes()) != null) {
            event.getToolTip().removeIf(line -> line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && t.getKey().equals("item.guhs.guh_clothes.lore"));
            event.getToolTip().add(Component.translatable("item.guhs.kapsel.lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** guhs:kapper_open: Krulletje's talking screen. */
    public static void open(KapperPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new KapperScreen(payload.npcId(), payload.data()));
    }

    /** guhs:kapper_knip: open, update or close the knip screen. */
    public static void knip(KapperPayloads.Knip payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.data().getBoolean("Sluit")) {
            if (mc.screen instanceof KnipScreen) {
                mc.setScreen(null);
            }
            return;
        }
        if (mc.screen instanceof KnipScreen screen && screen.npcId() == payload.npcId()) {
            screen.update(payload.data());
        } else if (payload.data().getBoolean("Open")) {
            mc.setScreen(new KnipScreen(payload.npcId(), payload.data()));
        }
    }

    /** A little tuft of cut hair: twirls down and lies still a moment. */
    static class Plukje extends TextureSheetParticle {
        private final float spin;

        Plukje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz);
            pickSprite(sprites);
            lifetime = 30 + random.nextInt(25);
            quadSize = 0.06f + random.nextFloat() * 0.05f;
            gravity = 0.25f;
            xd = (random.nextDouble() - 0.5) * 0.08;
            yd = 0.05 + random.nextDouble() * 0.05;
            zd = (random.nextDouble() - 0.5) * 0.08;
            spin = (random.nextFloat() - 0.5f) * 0.4f;
            roll = random.nextFloat() * 6.28f;
        }

        @Override
        public void tick() {
            super.tick();
            oRoll = roll;
            if (!onGround) {
                roll += spin;
                xd *= 0.9;
                zd *= 0.9;
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A glittery curl: floats up, twinkles and fades. */
    static class Glitter extends TextureSheetParticle {
        private final SpriteSet sprites;

        Glitter(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            this.sprites = sprites;
            setSpriteFromAge(sprites);
            lifetime = 20 + random.nextInt(20);
            quadSize = 0.07f + random.nextFloat() * 0.06f;
            gravity = -0.02f;
            xd = (random.nextDouble() - 0.5) * 0.03;
            yd = 0.02 + random.nextDouble() * 0.02;
            zd = (random.nextDouble() - 0.5) * 0.03;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            alpha = 1f - (float) age / lifetime * 0.8f;
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

    private KapperClient() {
    }
}
