package nl.juiced.guhs.feature.kleding.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingFeature;
import nl.juiced.guhs.registry.ModItems;

/**
 * Client side of the kleding feature (2.9): the confetti particle and the big "you unlocked it!" pop of the piece on
 * your screen (like a totem). The new wardrobe itself is client.screen.GuhWardrobeScreen.
 */
public final class KledingClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(KledingClient::particles);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(KledingFeature.CONFETTI.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Confetti(level, x, y, z, dx, dy, dz, sprites));
    }

    /** guhs:kleding_ontgrendeld: the piece pops up big in the middle of your screen. */
    public static void ontgrendeld(String id) {
        GuhClothes c = GuhClothes.byId(id);
        Minecraft mc = Minecraft.getInstance();
        if (c != null && mc.gameRenderer != null) {
            mc.gameRenderer.displayItemActivation(new ItemStack(ModItems.clothingItem(c)));
        }
    }

    /** A little paper snipper in guh colours: pops up, flutters down turning, and fades. */
    static class Confetti extends SingleQuadParticle {
        private final float spin;

        Confetti(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites.get(level.getRandom()));
            lifetime = 30 + random.nextInt(30);
            quadSize = 0.05f + random.nextFloat() * 0.05f;
            gravity = 0.35f;
            friction = 0.9f;
            xd = (random.nextDouble() - 0.5) * 0.35;
            yd = 0.15 + random.nextDouble() * 0.2;
            zd = (random.nextDouble() - 0.5) * 0.35;
            spin = (random.nextFloat() - 0.5f) * 0.6f;
            roll = random.nextFloat() * 6.28f;
        }

        @Override
        public void tick() {
            super.tick();
            oRoll = roll;
            if (!onGround) {
                roll += spin;
                xd += Math.sin(age * 0.4 + spin * 10) * 0.004;       // (fluttering)
            }
            if (age > lifetime - 10) {
                alpha = (lifetime - age) / 10f;
            }
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private KledingClient() {
    }
}
