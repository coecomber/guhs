package nl.juiced.guhs.feature.baltoslee.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.baltoslee.BaltoSleeFeature;
import nl.juiced.guhs.feature.baltoslee.SleeEntity;
import nl.juiced.guhs.feature.baltoslee.SleeRijden;
import nl.juiced.guhs.feature.baltoslee.SneeuwsleeEntity;

/**
 * 3.0 (Guhverhalen), slice baltoslee, client side: the sled renderers (with their teams of guh-sledehondjes), the sled panel
 * ({@link SleeHud}), the ride's storm, sounds and sparkles ({@link SleeEffecten}), the keys (W/S/A/D while riding), Baltoguh's
 * nose particle.
 */
public final class BaltoSleeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(BaltoSleeFeature.SLEE.get(), SleeRenderer::new);
            event.registerEntityRenderer(BaltoSleeFeature.SNEEUWSLEE_ENTITY.get(), SneeuwsleeRenderer::new);
            event.registerEntityRenderer(BaltoSleeFeature.SLEDEHONDJE.get(), SledehondjeRenderer::new);
        });
        modBus.addListener((RegisterParticleProvidersEvent event) -> event.registerSpriteSet(BaltoSleeFeature.SNUFFEL.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) -> new Snuffel(level, x, y, z, dx, dy, dz, sprites)));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(Guhs.id("baltoslee_hud"), SleeHud::render));
        SleeEntity.lokaal = () -> Minecraft.getInstance().player;
        SleeEntity.invoer = BaltoSleeClient::toetsen;
        SleeEntity.clientTick = SleeEffecten::tick;
        SleeEntity.clientEvent = SleeEffecten::event;
        SneeuwsleeEntity.invoer = BaltoSleeClient::toetsen;
        SneeuwsleeEntity.clientTick = SleeEffecten::eigenTick;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> SleeEffecten.clientTick());
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeCameraAngles event) -> SleeEffecten.camera(event));
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> lore(event));
    }

    /** The coin and the deco get a little line of text (item.guhs.&lt;id&gt;.lore). */
    private static final java.util.Set<String> LORE = java.util.Set.of("sledebelletje", "baltoslee_sneeuwguh", "baltoslee_minislee",
            "baltoslee_sledebellen", "baltoslee_beker", "baltoslee_hondenmand", "baltoslee_lantaarnpaal");

    private static void lore(ItemTooltipEvent event) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (Guhs.MODID.equals(id.getNamespace()) && LORE.contains(id.getPath())) {
            event.getToolTip().add(Math.min(1, event.getToolTip().size()), net.minecraft.network.chat.Component
                    .translatable("item.guhs." + id.getPath() + ".lore").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    /** W/S and A/D of the local player (nothing while a screen is open). */
    private static SleeRijden.Invoer toetsen() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || Minecraft.getInstance().screen != null) {
            return SleeRijden.Invoer.NIKS;
        }
        return new SleeRijden.Invoer(p.input.forwardImpulse, -p.input.leftImpulse);
    }

    /** Baltoguh's nose: a little glowing golden-blue sparkle on the snow, twinkling, slowly rising. */
    static final class Snuffel extends TextureSheetParticle {
        private final float basis;

        Snuffel(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz);
            pickSprite(sprites);
            lifetime = 22 + random.nextInt(14);
            basis = 0.07f + random.nextFloat() * 0.05f;
            quadSize = basis;
            gravity = 0f;
            xd = dx;
            yd = dy + 0.004;
            zd = dz;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            float t = (float) age / lifetime;
            quadSize = basis * (0.35f + Mth.sin(t * Mth.PI) * (0.85f + 0.15f * Mth.sin(age * 1.1f)));
            alpha = Math.min(1f, (1 - t) * 1.6f);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return LightCoordsUtil.FULL_BRIGHT;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private BaltoSleeClient() {
    }
}
