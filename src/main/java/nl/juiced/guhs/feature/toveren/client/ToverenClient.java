package nl.juiced.guhs.feature.toveren.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FlyTowardsPositionParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.feature.toveren.ToverenFeature;

/**
 * Client side of toveren: the Glinsteraltaar's floating book ({@link GlinsteraltaarRenderer}), the hearts that fly from
 * the Knuffelboekenkasten to the altar ({@link Hartje}) and Pluisval's fluff ({@link Pluisje}). The altar's screen is the
 * vanilla enchanting screen (its menu is an EnchantmentMenu).
 */
public final class ToverenClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(ToverenFeature.GLINSTERALTAAR_BE.get(), GlinsteraltaarRenderer::new));
        modBus.addListener(ToverenClient::particles);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ToverenClient::uitleg);
    }

    /**
     * Playtest: hovering over a book or an item with a guh enchantment says what it does (the .desc text of each guh
     * enchantment, under the item's other lines). Vanilla enchantments keep their vanilla tooltip.
     */
    private static void uitleg(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        net.minecraft.world.item.ItemStack stack = event.getItemStack();
        for (var component : java.util.List.of(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,
                net.minecraft.core.component.DataComponents.ENCHANTMENTS)) {
            net.minecraft.world.item.enchantment.ItemEnchantments spreuken = stack.get(component);
            if (spreuken == null) {
                continue;
            }
            for (var holder : spreuken.keySet()) {
                holder.unwrapKey().filter(k -> k.identifier().getNamespace().equals(nl.juiced.guhs.Guhs.MODID)).ifPresent(k -> event.getToolTip().add(
                        net.minecraft.network.chat.Component.literal("  ").append(holder.value().description()).append(": ")
                                .append(net.minecraft.network.chat.Component.translatable("enchantment.guhs." + k.identifier().getPath() + ".desc"))
                                .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC)));
            }
        }
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ToverenFeature.GLINSTERHARTJE.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Hartje(level, x, y, z, dx, dy, dz, sprites.get(random)));
        event.registerSpriteSet(ToverenFeature.PLUISPOEF.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Pluisje(level, x, y, z, dx, dy, dz, sprites.get(random)));
    }

    /** A little pink heart or sparkle that flies like vanilla's enchanting glyph, glowing, in its own colours. */
    static class Hartje extends FlyTowardsPositionParticle {
        Hartje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite) {
            super(level, x, y, z, dx, dy, dz, true, Particle.LifetimeAlpha.ALWAYS_OPAQUE, sprite);
            rCol = gCol = bCol = 1.0f;
            quadSize *= 1.6f;
        }
    }

    /** A pinch of fluff: puffs out, floats down slowly and fades. */
    static class Pluisje extends SingleQuadParticle {
        Pluisje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite);
            lifetime = 20 + random.nextInt(20);
            quadSize = 0.08f + random.nextFloat() * 0.08f;
            xd = dx + (random.nextDouble() - 0.5) * 0.12;
            yd = 0.04 + random.nextDouble() * 0.05;
            zd = dz + (random.nextDouble() - 0.5) * 0.12;
            gravity = 0.02f;
            friction = 0.88f;
        }

        @Override
        public void tick() {
            super.tick();
            alpha = Math.min(1f, (1f - age / (float) lifetime) * 2f);
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private ToverenClient() {
    }
}
