package nl.juiced.guhs.feature.vadsrijk.zone3.wereld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;
import nl.juiced.guhs.world.GuhTime;

/**
 * Ambient of zone 3 (ZONE3_PLAN §1 sound; vanilla sounds, local on the client), per LAYER (Z3WereldPlekken.gebied by height):
 * buzzing bees, rustling leaves and falling petals in the crown; creaking wood and wind on the branches; creaks, sap drips and
 * an owl-ish hoot in the hollow trunk; leaves, sap drops and fireflies at night on the ground; and deep in the roots a slow
 * heartbeat (stronger near the heart under the trunk) with violet glints. Plus campfire crackle near the zone-3 checkpoints.
 * Music only at bosses and scenes (not here).
 */
final class Z3Sfeer {
    private static final RandomSource R = RandomSource.create();
    private static int hartslag;

    private Z3Sfeer() {
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        LocalPlayer p = mc.player;
        if (l == null || p == null || !Vadsrijk.in(l) || mc.isPaused() || !Z3WereldPlekken.inZone3(p.getX(), p.getZ())) {
            return;
        }
        double x = p.getX(), y = p.getY(), z = p.getZ();
        Z3WereldPlekken.Gebied3 g = Z3WereldPlekken.gebied(x, y, z);
        String id = g == null ? "z3_zee" : g.id();
        long t = GuhTime.timeOfDay(l);
        boolean nacht = t > 13000 && t < 23000;
        double rStam = Math.hypot(x - Z3WereldPlekken.STAM_X, z - Z3WereldPlekken.STAM_Z);
        // particles
        switch (id) {
            case "z3_kruinzee", "z3_honingtak" -> {
                if (R.nextInt(2) == 0) {
                    l.addParticle(ParticleTypes.CHERRY_LEAVES, x + R.nextGaussian() * 10, y + 6 + R.nextDouble() * 6, z + R.nextGaussian() * 10, 0, 0, 0);
                }
                if (nacht && R.nextInt(3) == 0) {
                    l.addParticle(ParticleTypes.FIREFLY, x + R.nextGaussian() * 12, y + 1 + R.nextDouble() * 5, z + R.nextGaussian() * 12, 0, 0, 0);
                }
            }
            case "z3_wortelvelden" -> {
                if ((nacht || rStam < 140) && R.nextInt(2) == 0) {
                    l.addParticle(ParticleTypes.FIREFLY, x + R.nextGaussian() * 12, y + 1 + R.nextDouble() * 4, z + R.nextGaussian() * 12, 0, 0, 0);
                }
                if (rStam < 140 && R.nextInt(6) == 0) {
                    l.addParticle(ParticleTypes.FALLING_HONEY, x + R.nextGaussian() * 10, y + 10, z + R.nextGaussian() * 10, 0, 0, 0);
                }
            }
            case "z3_schorsstad" -> {
                if (R.nextInt(4) == 0) {
                    l.addParticle(ParticleTypes.FALLING_DRIPSTONE_WATER, x + R.nextGaussian() * 10, y + 8, z + R.nextGaussian() * 10, 0, 0, 0);
                }
            }
            case "z3_wortelduister", "z3_hart" -> {
                if (R.nextInt(3) == 0) {
                    l.addParticle(ParticleTypes.GLOW, x + R.nextGaussian() * 8, y + R.nextDouble() * 4, z + R.nextGaussian() * 8, 0, 0.01, 0);
                }
            }
            default -> {
            }
        }
        // [p-wereld] chimney smoke and bee swarms near the player (the map's SCHOORSTENEN / BIJENKORVEN)
        for (net.minecraft.core.BlockPos c : Z3WereldPlekken.SCHOORSTENEN) {
            if (Math.abs(c.getX() - x) < 64 && Math.abs(c.getZ() - z) < 64 && Math.abs(c.getY() - y) < 48 && R.nextInt(5) == 0) {
                l.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.getX() + 0.5, c.getY() + 0.6, c.getZ() + 0.5, 0, 0.05, 0);
            }
        }
        for (net.minecraft.core.BlockPos c : Z3WereldPlekken.BIJENKORVEN) {
            if (Math.abs(c.getX() - x) < 24 && Math.abs(c.getZ() - z) < 24 && Math.abs(c.getY() - y) < 12 && R.nextInt(3) == 0) {
                l.addParticle(ParticleTypes.WAX_ON, c.getX() + 0.5 + R.nextGaussian(), c.getY() + 0.5 + R.nextDouble(),
                        c.getZ() + 0.5 + R.nextGaussian(), 0, 0, 0);
            }
        }
        // the heartbeat in the roots: every ~4 s, louder towards the heart
        if (y < 66 && rStam < 230 && ++hartslag >= 80) {
            hartslag = 0;
            float vol = (float) Math.max(0.15, 1.0 - rStam / 230.0);
            l.playLocalSound(x, y, z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, vol, 0.55f, false);
        }
        if (R.nextInt(60) != 0) {
            return;
        }
        SoundEvent s;
        float pitch = 0.9f + R.nextFloat() * 0.3f, vol = 0.35f;
        switch (id) {
            case "z3_honingtak" -> {
                s = R.nextBoolean() ? SoundEvents.BEEHIVE_WORK : SoundEvents.NOTE_BLOCK_CHIME.value();
                pitch = 1.1f + R.nextFloat() * 0.4f;
                vol = 0.25f;
            }
            case "z3_kruinzee" -> {
                s = R.nextInt(3) == 0 ? SoundEvents.BEEHIVE_WORK : SoundEvents.AZALEA_LEAVES_STEP;
                pitch = 0.9f + R.nextFloat() * 0.5f;
            }
            case "z3_takkenbanen", "z3_zeppelinwerf" -> {
                s = R.nextBoolean() ? SoundEvents.WOOD_STEP : SoundEvents.AZALEA_LEAVES_HIT;
                pitch = 0.45f + R.nextFloat() * 0.2f;
                vol = 0.45f;
            }
            case "z3_schorsstad", "z3_stamspiraal" -> {
                int k = R.nextInt(3);
                s = k == 0 ? SoundEvents.WOOD_STEP : k == 1 ? SoundEvents.POINTED_DRIPSTONE_DRIP_WATER : SoundEvents.PARROT_IMITATE_PHANTOM;
                pitch = k == 2 ? 0.5f : 0.4f + R.nextFloat() * 0.3f;
                vol = k == 2 ? 0.15f : 0.4f;
            }
            case "z3_wortelduister", "z3_hart" -> {
                s = R.nextBoolean() ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON;
                pitch = 0.5f + R.nextFloat() * 0.3f;
            }
            default -> {
                s = R.nextBoolean() ? SoundEvents.AZALEA_LEAVES_STEP : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
                pitch = 0.8f + R.nextFloat() * 0.3f;
            }
        }
        l.playLocalSound(x + R.nextGaussian() * 8, y + 2, z + R.nextGaussian() * 8, s, SoundSource.AMBIENT, vol, pitch, false);
        for (BlockPos k : Z3WereldPlekken.KAMPVUREN) {
            if (k.distToCenterSqr(x, y, z) < 64) {
                l.playLocalSound(k.getX() + 0.5, k.getY() + 0.5, k.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.AMBIENT, 0.6f, 1f, false);
            }
        }
    }
}
