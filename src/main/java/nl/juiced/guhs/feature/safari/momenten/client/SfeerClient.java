package nl.juiced.guhs.feature.safari.momenten.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.balto.client.Sneeuwstorm;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.momenten.Momenten;
import nl.juiced.guhs.feature.safari.rit.RitData;
import nl.juiced.guhs.feature.safari.rit.TruckEntity;

/**
 * The weather and atmosphere of the park per zone (S3, design 1.10), client only:
 * <ul>
 *   <li>IJskust and Oerijstijd: snow flakes (the Sneeuwstorm source "safari"), a bit of a white haze in the valley;</li>
 *   <li>Bamboebergen: mist (the same source, haze only);</li>
 *   <li>Regenwoud: now and then a tropical shower (rain, drips, the sound);</li>
 *   <li>Savanne: heat shimmer around noon (little rising specks);</li>
 *   <li>the warm springs steam, the elephant fountain on the village square sprays;</li>
 *   <li>northern lights above the IJskust at night ({@link Momenten#noorderlicht}): curtains of green, teal and violet;</li>
 *   <li>the lantern trucks of a night safari glow.</li>
 * </ul>
 */
public final class SfeerClient {
    private static final DustParticleOptions[] NOORDERLICHT = {new DustParticleOptions(0x5CFF9A, 3.5f), new DustParticleOptions(0x3FE0D0, 3.5f),
            new DustParticleOptions(0xB070FF, 3.0f), new DustParticleOptions(0x7DFFB8, 2.5f)};
    private static final DustParticleOptions LAMPION = new DustParticleOptions(0xFFC060, 1.2f);
    private static int regenGeluid;

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Player p = mc.player;
        if (level == null || p == null || !Safari.echt(level)) {
            Sneeuwstorm.uit("safari");
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        RandomSource r = level.getRandom();
        int zi = ParkData.get().zone(p.getBlockX(), p.getBlockZ());
        String zone = zi >= 0 ? ParkData.get().zoneNaam(zi) : "";
        int tijd = Momenten.tijd(level);
        long t = level.getGameTime();
        switch (zone) {
            case "ijskust" -> Sneeuwstorm.zet("safari", 0.16f, 0.25f, 0.15f, 0.02f);
            case "oertijd" -> Sneeuwstorm.zet("safari", 0.28f, 0.35f, 0.2f, 0.12f);
            case "bamboe" -> Sneeuwstorm.zet("safari", 0f, 0f, 0f, 0.42f);
            default -> Sneeuwstorm.uit("safari");
        }
        if (zone.equals("regenwoud") && Math.floorMod(t / 2400, 3) == 0) {
            bui(mc, level, p, r);
        }
        if (zone.equals("savanne") && tijd > 4000 && tijd < 9000 && r.nextInt(2) == 0) {
            double x = p.getX() + (r.nextDouble() - 0.5) * 24, z = p.getZ() + (r.nextDouble() - 0.5) * 24;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
            level.addParticle(ParticleTypes.WHITE_ASH, x, y + 0.3 + r.nextDouble(), z, 0, 0.03, 0);
        }
        RitData d = RitData.get();
        for (Vec3 b : d.bronnen) {
            if (b.distanceToSqr(p.position()) < 48 * 48 && r.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.CLOUD, b.x + (r.nextDouble() - 0.5) * 2.5, b.y + 0.2, b.z + (r.nextDouble() - 0.5) * 2.5,
                        (r.nextDouble() - 0.5) * 0.01, 0.04 + r.nextDouble() * 0.03, (r.nextDouble() - 0.5) * 0.01);
            }
        }
        if (d.fontein != null && d.fontein.distanceToSqr(p.position()) < 40 * 40) {
            for (int i = 0; i < 3; i++) {
                double hoek = r.nextDouble() * Mth.TWO_PI;
                level.addParticle(ParticleTypes.SPLASH, d.fontein.x, d.fontein.y, d.fontein.z,
                        Math.cos(hoek) * 0.12, 0.35 + r.nextDouble() * 0.15, Math.sin(hoek) * 0.12);
            }
        }
        if (Momenten.noorderlicht(level, p.blockPosition())) {
            noorderlicht(level, p, r, t);
        }
        if (Momenten.nachtsafari(level)) {
            for (TruckEntity truck : level.getEntitiesOfClass(TruckEntity.class, p.getBoundingBox().inflate(64), TruckEntity::nacht)) {
                if (r.nextInt(2) == 0) {
                    Vec3 hoek = new Vec3((r.nextBoolean() ? 1.2 : -1.2), 2.4, (r.nextBoolean() ? 1.6 : -1.6)).yRot(-truck.getYRot() * Mth.DEG_TO_RAD);
                    level.addParticle(LAMPION, truck.getX() + hoek.x, truck.getY() + hoek.y, truck.getZ() + hoek.z, 0, 0, 0);
                }
            }
        }
    }

    /** A tropical shower: rain around you (not under a roof), drips from the leaves, the sound of it. */
    private static void bui(Minecraft mc, ClientLevel level, Player p, RandomSource r) {
        BlockPos.MutableBlockPos b = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 14; i++) {
            double x = p.getX() + (r.nextDouble() - 0.5) * 20, z = p.getZ() + (r.nextDouble() - 0.5) * 20;
            b.set(x, p.getY(), z);
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, b.getX(), b.getZ());
            double y = Math.max(top, p.getY()) + 2 + r.nextDouble() * 6;
            level.addParticle(top > p.getY() + 2 ? ParticleTypes.DRIPPING_WATER : ParticleTypes.FALLING_WATER, x, y, z, 0, -0.5, 0);
        }
        if (--regenGeluid <= 0) {
            regenGeluid = 40 + r.nextInt(20);
            mc.getSoundManager().play(new net.minecraft.client.resources.sounds.SimpleSoundInstance(SoundEvents.WEATHER_RAIN, SoundSource.WEATHER,
                    0.25f, 0.9f + r.nextFloat() * 0.2f, r, p.getX(), p.getY() + 3, p.getZ()));
        }
    }

    /** Curtains of light high up north of you, slowly waving. */
    private static void noorderlicht(ClientLevel level, Player p, RandomSource r, long t) {
        float sterkte = 1f;
        int tijd = Momenten.tijd(level);
        if (tijd < 14000) {
            sterkte = (tijd - 13000) / 1000f;
        } else if (tijd > 22000) {
            sterkte = (23000 - tijd) / 1000f;
        }
        int n = Mth.ceil(10 * Mth.clamp(sterkte, 0, 1));
        for (int i = 0; i < n; i++) {
            int band = r.nextInt(3);
            double x = (r.nextDouble() - 0.5) * 160;
            double golf = Mth.sin((float) (x * 0.035 + t * 0.01 + band * 2.1)) * 14;
            double z = -45 - band * 14 + golf;
            double y = 48 + band * 6 + r.nextDouble() * 14;
            // (always visible: far above you, beyond the normal 32-block particle range)
            level.addAlwaysVisibleParticle(NOORDERLICHT[(band + r.nextInt(2)) % NOORDERLICHT.length], p.getX() + x, p.getY() + y, p.getZ() + z, 0, 0.01, 0);
        }
    }

    private SfeerClient() {
    }
}
