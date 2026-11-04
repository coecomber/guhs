package nl.juiced.guhs.feature.vadsrijk.zone2.wereld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.juiced.guhs.feature.balto.BaltoFeature;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldRegistry;
import nl.juiced.guhs.world.GuhTime;

/**
 * Ambient of zone 2 (vanilla + Balto's wind sound, local on the client): wind on the slopes and in the ravine, creaking ice on the
 * lake, crystal chimes from the needles, a cosy town (chimes, crackling fires) in Kandijkom; drifting snowflakes outside, glints
 * in the Spiegelburcht, campfire crackle near the zone-2 checkpoints.
 */
final class Z2Sfeer {
    private static final RandomSource R = RandomSource.create();

    private Z2Sfeer() {
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        LocalPlayer p = mc.player;
        if (l == null || p == null || !Vadsrijk.in(l) || mc.isPaused() || !Z2WereldRegistry.inZone2(p.getX(), p.getZ())) {
            return;
        }
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getZ());
        String id = g == null ? "zee" : g.id();
        int maaiveld = l.getHeight(Heightmap.Types.WORLD_SURFACE, p.getBlockX(), p.getBlockZ());
        boolean binnen = p.getY() < maaiveld - 4;
        long t = GuhTime.timeOfDay(l);
        boolean nacht = t > 13000 && t < 23000;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        // particles
        if (!binnen && !id.equals("z2_spiegelburcht")) {
            int n = id.equals("z2_kandijkom") || id.equals("z2_kandijkom_dok") ? 1 : 3;
            for (int i = 0; i < n; i++) {
                l.addParticle(ParticleTypes.SNOWFLAKE, x + R.nextGaussian() * 10, y + 5 + R.nextDouble() * 6, z + R.nextGaussian() * 10,
                        0.04, -0.03, 0.02);
            }
        }
        if (id.equals("z2_spiegelburcht") && R.nextInt(3) == 0) {
            l.addParticle(ParticleTypes.END_ROD, x + R.nextGaussian() * 6, y + 1 + R.nextDouble() * 4, z + R.nextGaussian() * 6, 0, 0.005, 0);
        }
        if (nacht && !binnen && R.nextInt(5) == 0 && id.equals("z2_galmhellingen")) {
            l.addParticle(ParticleTypes.GLOW, x + R.nextGaussian() * 8, y + 1 + R.nextDouble() * 3, z + R.nextGaussian() * 8, 0, 0.01, 0);
        }
        // (I-AUDIO: not in Kandijkom, its sounds come from stadsmuziek.client.StadsMuziekSpeler)
        if (R.nextInt(60) != 0 || nl.juiced.guhs.feature.vadsrijk.stadsmuziek.StadsMuziek.stadVan(id) != null) {
            return;
        }
        SoundEvent s;
        float pitch = 0.9f + R.nextFloat() * 0.3f, vol = 0.35f;
        switch (id) {
            case "z2_kandijkom", "z2_kandijkom_dok" -> {
                s = R.nextBoolean() ? SoundEvents.NOTE_BLOCK_CHIME.value() : SoundEvents.CAMPFIRE_CRACKLE;
                pitch = 1.2f + R.nextFloat() * 0.5f;
                vol = 0.25f;
            }
            case "z2_galmhellingen" -> {
                s = R.nextInt(3) == 0 ? BaltoFeature.WIND.get() : SoundEvents.AMETHYST_BLOCK_CHIME;
                pitch = 0.8f + R.nextFloat() * 0.8f;
            }
            case "z2_knuffelmeer" -> {
                s = R.nextBoolean() ? SoundEvents.GLASS_HIT : (R.nextBoolean() ? SoundEvents.POWDER_SNOW_STEP : BaltoFeature.WIND.get());
                pitch = 0.45f + R.nextFloat() * 0.2f;
                vol = 0.45f;
            }
            case "z2_splinterkloof" -> {
                s = R.nextBoolean() ? BaltoFeature.WIND.get() : SoundEvents.AMETHYST_CLUSTER_STEP;
                pitch = 0.6f + R.nextFloat() * 0.3f;
                vol = 0.45f;
            }
            case "z2_spiegelburcht" -> {
                s = R.nextBoolean() ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_BLOCK_CHIME;
                pitch = 0.5f + R.nextFloat() * 0.3f;
            }
            default -> {
                s = BaltoFeature.WIND.get();
                pitch = 0.9f;
            }
        }
        if (binnen && s == BaltoFeature.WIND.get()) {
            vol *= 0.4f;
        }
        l.playLocalSound(x + R.nextGaussian() * 8, y + 2, z + R.nextGaussian() * 8, s, SoundSource.AMBIENT, vol, pitch, false);
        for (BlockPos k : Z2WereldPlekken.KAMPVUREN) {
            if (k.distToCenterSqr(x, y, z) < 64) {
                l.playLocalSound(k.getX() + 0.5, k.getY() + 0.5, k.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.AMBIENT, 0.6f, 1f, false);
            }
        }
    }
}
