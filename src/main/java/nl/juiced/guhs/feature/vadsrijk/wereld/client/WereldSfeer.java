package nl.juiced.guhs.feature.vadsrijk.wereld.client;

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
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.world.GuhTime;

/**
 * Ambient life per area (design ronde 9: "alleen ambient": birds, spores, drips, seagulls; music only at bosses and scenes).
 * Vanilla sounds only, played locally on the client, now and then, around the player; plus particles: the light beams of
 * the Sporenwoud, floating spores, drips in the caves, petals in the meadows and the city.
 */
final class WereldSfeer {
    private static final RandomSource R = RandomSource.create();

    private WereldSfeer() {
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        LocalPlayer p = mc.player;
        if (l == null || p == null || !Vadsrijk.in(l) || mc.isPaused()) {
            return;
        }
        lichtstralen(l, p);
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getZ());
        String id = g == null ? "zee" : g.id();
        int maaiveld = l.getHeight(Heightmap.Types.WORLD_SURFACE, p.getBlockX(), p.getBlockZ());
        boolean onder = p.getY() < maaiveld - 6;
        long t = GuhTime.timeOfDay(l);
        boolean nacht = t > 13000 && t < 23000;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        // particles every tick
        if (onder) {
            for (int i = 0; i < 2; i++) {
                l.addParticle(ParticleTypes.DRIPPING_WATER, x + R.nextGaussian() * 6, y + 3 + R.nextDouble() * 3, z + R.nextGaussian() * 6, 0, 0, 0);
            }
        } else if (id.equals("sporenwoud")) {
            for (int i = 0; i < 3; i++) {
                l.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, x + R.nextGaussian() * 9, y + R.nextDouble() * 6, z + R.nextGaussian() * 9, 0, 0, 0);
            }
        } else if (id.equals("klodderweide") || id.equals("zwamzoet") || id.equals("knuffelkamp")) {
            if (R.nextInt(3) == 0) {
                l.addParticle(ParticleTypes.CHERRY_LEAVES, x + R.nextGaussian() * 10, y + 6 + R.nextDouble() * 4, z + R.nextGaussian() * 10, 0, 0, 0);
            }
        } else if (id.equals("zwamtempel") && R.nextInt(2) == 0) {
            l.addParticle(ParticleTypes.WARPED_SPORE, x + R.nextGaussian() * 8, y + R.nextDouble() * 5, z + R.nextGaussian() * 8, 0, 0, 0);
        }
        if (nacht && !onder && R.nextInt(4) == 0 && (id.equals("sporenwoud") || id.equals("klodderweide"))) {
            l.addParticle(ParticleTypes.GLOW, x + R.nextGaussian() * 8, y + 1 + R.nextDouble() * 3, z + R.nextGaussian() * 8, 0, 0.01, 0);
        }
        // sounds now and then (I-AUDIO: not in the towns, their sweet sounds come from stadsmuziek.client.StadsMuziekSpeler)
        if (R.nextInt(70) != 0 || (!onder && nl.juiced.guhs.feature.vadsrijk.stadsmuziek.StadsMuziek.stadVan(id) != null)) {
            return;
        }
        SoundEvent s;
        float pitch = 0.9f + R.nextFloat() * 0.3f, vol = 0.35f;
        if (onder) {
            s = R.nextBoolean() ? SoundEvents.POINTED_DRIPSTONE_DRIP_WATER : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON;
            vol = 0.6f;
        } else {
            switch (id) {
                case "sporenwoud" -> {
                    s = R.nextInt(3) == 0 ? SoundEvents.AMETHYST_BLOCK_CHIME : (nacht ? SoundEvents.FROG_AMBIENT : SoundEvents.PARROT_AMBIENT);
                    pitch = 1.3f + R.nextFloat() * 0.4f;
                }
                case "klodderweide" -> {
                    s = R.nextInt(3) == 0 ? SoundEvents.SLIME_SQUISH_SMALL : (nacht ? SoundEvents.FROG_AMBIENT : SoundEvents.PARROT_AMBIENT);
                    pitch = 1.4f + R.nextFloat() * 0.4f;
                }
                case "druipgrotten" -> {
                    s = R.nextBoolean() ? SoundEvents.POINTED_DRIPSTONE_DRIP_WATER : SoundEvents.TUFF_STEP;
                }
                case "zwamtempel" -> {
                    s = R.nextBoolean() ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
                    pitch = 0.6f;
                }
                case "haven", "knuffelkamp" -> {
                    s = R.nextBoolean() ? SoundEvents.PARROT_AMBIENT : SoundEvents.BOAT_PADDLE_WATER;   // seagull-ish + water lapping
                    pitch = 1.7f + R.nextFloat() * 0.2f;
                }
                case "zwamzoet" -> {
                    s = R.nextBoolean() ? SoundEvents.PARROT_AMBIENT : SoundEvents.NOTE_BLOCK_CHIME.value();
                    pitch = 1.5f + R.nextFloat() * 0.3f;
                    vol = 0.2f;
                }
                default -> {
                    s = nacht ? SoundEvents.FROG_AMBIENT : SoundEvents.PARROT_AMBIENT;
                    pitch = 1.5f;
                }
            }
        }
        l.playLocalSound(x + R.nextGaussian() * 8, y + 2, z + R.nextGaussian() * 8, s, SoundSource.AMBIENT, vol, pitch, false);
        // the campfires crackle (they are only lit for whoever lit them; the sound is just cosy)
        for (BlockPos k : WereldPlekken.KAMPVUREN) {
            if (k.distToCenterSqr(x, y, z) < 64) {
                l.playLocalSound(k.getX() + 0.5, k.getY() + 0.5, k.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.AMBIENT, 0.6f, 1f, false);
            }
        }
    }

    /** The Sporenwoud's light beams: a slow column of glittering motes falling through the gap in the caps. */
    private static void lichtstralen(ClientLevel l, LocalPlayer p) {
        for (BlockPos b : WereldPlekken.LICHTSTRALEN) {
            double d2 = b.distToCenterSqr(p.getX(), p.getY(), p.getZ());
            if (d2 > 96 * 96) {
                continue;
            }
            int n = d2 < 32 * 32 ? 3 : 1;
            for (int i = 0; i < n; i++) {
                double h = R.nextDouble() * 38;
                l.addParticle(ParticleTypes.END_ROD, b.getX() + 0.5 + R.nextGaussian() * 1.6, b.getY() + h, b.getZ() + 0.5 + R.nextGaussian() * 1.6,
                        0, -0.03, 0);
            }
            if (R.nextInt(4) == 0) {
                l.addParticle(ParticleTypes.CHERRY_LEAVES, b.getX() + 0.5 + R.nextGaussian() * 2.5, b.getY() + 30, b.getZ() + 0.5 + R.nextGaussian() * 2.5,
                        0, 0, 0);
            }
        }
    }
}
