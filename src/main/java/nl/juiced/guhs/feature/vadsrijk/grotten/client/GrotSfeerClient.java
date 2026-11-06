package nl.juiced.guhs.feature.vadsrijk.grotten.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.grotten.GrotData;
import nl.juiced.guhs.feature.vadsrijk.grotten.Grotten;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * U-GROTTEN: the cave atmosphere on the client, inside a cave's box only. Floating particles in the air round you (spores in the
 * Zwamgrot, snow sparkles in the Kristalgrot, root dust + fireflies-ish wax sparkles in the Wortelgrot, drips + glow in the
 * Koraalgrot, little cloud puffs in the Wolkengrotten) and now and then a drip or a crystal ping somewhere on the ceiling (the
 * "echo" on top of t-geluid's cave bed, which already plays through {@code GeluidPlan.EXTRA}).
 */
public final class GrotSfeerClient {
    private static int volgendeDruppel = 100;

    private GrotSfeerClient() {
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        ClientLevel l = mc.level;
        if (p == null || l == null || mc.isPaused() || !Vadsrijk.in(l)) {
            return;
        }
        GrotData.Grot g = Grotten.op(p.getX(), p.getY(), p.getZ());
        if (g == null) {
            return;
        }
        RandomSource r = l.getRandom();
        for (int i = 0; i < 3; i++) {
            double x = p.getX() + (r.nextDouble() - 0.5) * 22, y = p.getY() + (r.nextDouble() - 0.3) * 8, z = p.getZ() + (r.nextDouble() - 0.5) * 22;
            if (!l.getBlockState(BlockPos.containing(x, y, z)).isAir()) {
                continue;
            }
            ParticleOptions soort = deeltje(g.zone(), r);
            double vy = switch (g.zone()) {
                case 2 -> -0.02;
                case 4 -> -0.04;
                case 5 -> 0.004;
                default -> 0.005;
            };
            l.addParticle(soort, x, y, z, (r.nextDouble() - 0.5) * 0.01, vy, (r.nextDouble() - 0.5) * 0.01);
        }
        if (--volgendeDruppel <= 0) {
            volgendeDruppel = 100 + r.nextInt(180);
            // a drip / ping somewhere above, 4-10 blocks away: the cave echo
            double a = r.nextDouble() * Math.PI * 2, d = 4 + r.nextDouble() * 6;
            double x = p.getX() + Math.cos(a) * d, z = p.getZ() + Math.sin(a) * d;
            SoundEvent s = switch (g.zone()) {
                case 2, 5 -> SoundEvents.AMETHYST_BLOCK_CHIME;
                default -> SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
            };
            l.playLocalSound(x, p.getY() + 3 + r.nextDouble() * 4, z, s, SoundSource.AMBIENT, 0.55f, 0.8f + r.nextFloat() * 0.4f, false);
            if (r.nextBoolean()) {
                // the echo: the same sound once more, softer and a bit later in pitch
                l.playLocalSound(x + 3, p.getY() + 5, z - 3, s, SoundSource.AMBIENT, 0.25f, 0.7f + r.nextFloat() * 0.2f, true);
            }
        }
    }

    private static ParticleOptions deeltje(int zone, RandomSource r) {
        return switch (zone) {
            case 1 -> r.nextInt(5) == 0 ? ParticleTypes.GLOW : ParticleTypes.SPORE_BLOSSOM_AIR;
            case 2 -> r.nextInt(4) == 0 ? ParticleTypes.END_ROD : ParticleTypes.SNOWFLAKE;
            case 3 -> r.nextInt(3) == 0 ? ParticleTypes.WAX_ON : ParticleTypes.MYCELIUM;
            case 4 -> r.nextInt(3) == 0 ? ParticleTypes.GLOW : ParticleTypes.DRIPPING_DRIPSTONE_WATER;
            default -> r.nextInt(4) == 0 ? ParticleTypes.END_ROD : ParticleTypes.WHITE_ASH;
        };
    }
}
