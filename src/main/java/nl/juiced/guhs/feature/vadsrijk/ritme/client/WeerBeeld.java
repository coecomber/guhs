package nl.juiced.guhs.feature.vadsrijk.ritme.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.juiced.guhs.feature.vadsrijk.ritme.Rooster;
import nl.juiced.guhs.feature.vadsrijk.ritme.Steden;
import nl.juiced.guhs.feature.vadsrijk.ritme.WeerKlok;
import nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.SfeerKlok;
import nl.juiced.guhs.world.GuhTime;

/**
 * U-LEVEN-RITME client: the weather with charm (from {@link WeerKlok}, the same on every client) and the night life particles.
 * Gentle on purpose and on a budget: a capped number of particles per tick, scaled by the slider ({@link RitmeConfig#sterkte}).
 * <ul>
 *   <li>zone 2 snow shower, zone 3 blossom rain, zone 4 sweet rain (soft rain sound), all three followed by a rainbow (drawn by the
 *   atmosphere's sky layer through {@code SfeerClient.U_REGENBOOG});</li>
 *   <li>zone 4 the lampion-glow evening haze (warm fog + rising glow motes), zone 5 drifting cloud wisps (always a little, a parade
 *   during its moment), zone 6 warm sand swirls;</li>
 *   <li>morning mist everywhere (closer, whiter fog around sunrise on mist days);</li>
 *   <li>at night in the towns: fireflies.</li>
 * </ul>
 */
final class WeerBeeld {
    private static final RandomSource R = RandomSource.create();
    private static final DustParticleOptions ZAND = new DustParticleOptions(0xE8C27A, 1.3f), ZAND2 = new DustParticleOptions(0xD9A55E, 0.9f),
            GLOED = new DustParticleOptions(0xFFB070, 0.8f), GLOED2 = new DustParticleOptions(0xFF8AB8, 0.7f);
    /** Forced moments (dev): what -> game time it started. */
    private static final Map<String, Long> FORCE = new HashMap<>();
    static volatile Boolean nachtForceer;
    /** Smoothed values at the camera (for the fog). */
    private static float mist, gloed, regen;
    private static long volgendRegengeluid;
    private static float wervelHoek;

    private WeerBeeld() {
    }

    static void forceer(ClientLevel l, String wat) {
        switch (wat) {
            case "nacht" -> nachtForceer = Boolean.TRUE;
            case "dag" -> nachtForceer = Boolean.FALSE;
            case "auto" -> nachtForceer = null;
            case "weer:stop" -> FORCE.clear();
            default -> {
                if (wat.startsWith("weer:")) {
                    FORCE.put(wat.substring(5), l.getGameTime());
                }
            }
        }
    }

    static void wis() {
        FORCE.clear();
        nachtForceer = null;
        mist = gloed = regen = 0;
    }

    private static float force(ClientLevel l, String wat, long start, long duur, long ramp) {
        Long t = FORCE.get(wat);
        return t == null ? 0f : SfeerKlok.venster(l.getGameTime() - t, start, duur, ramp);
    }

    static float bui(ClientLevel l, int zone) {
        return Math.max(WeerKlok.bui(GuhTime.day(l), GuhTime.timeOfDay(l), zone), force(l, "bui", 0, WeerKlok.BUI_DUUR, 100));
    }

    static float regenboog(ClientLevel l, int zone) {
        if (zone < 2 || zone > 4) {
            return 0f;
        }
        return Math.max(WeerKlok.regenboog(GuhTime.day(l), GuhTime.timeOfDay(l), zone),
                Math.max(force(l, "bui", WeerKlok.BUI_DUUR - 200, WeerKlok.BOOG_DUUR, 200), force(l, "regenboog", 0, WeerKlok.BOOG_DUUR, 100)));
    }

    static float gloed(ClientLevel l, int zone) {
        return Math.max(WeerKlok.gloed(GuhTime.timeOfDay(l), zone), zone == 4 ? force(l, "gloed", 0, 2400, 200) : 0f);
    }

    static float mist(ClientLevel l) {
        return Math.max(WeerKlok.mist(GuhTime.day(l), GuhTime.timeOfDay(l)), force(l, "mist", 0, 2400, 200));
    }

    static boolean nacht(ClientLevel l) {
        Boolean f = nachtForceer;
        return f != null ? f : Rooster.nachtOp(GuhTime.timeOfDay(l));
    }

    /** The rainbow strength for the sky layer hook. */
    static double boogNu() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return 0;
        }
        int z = Steden.zone(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        return regenboog(mc.level, z) * Math.max(0.35f, RitmeConfig.sterkte() / 0.7f);
    }

    // ---- ticking ---------------------------------------------------------------------------------------------------------------

    static void tick(Minecraft mc) {
        ClientLevel l = mc.level;
        LocalPlayer p = mc.player;
        if (l == null || p == null) {
            return;
        }
        float k = RitmeConfig.sterkte();
        int zone = Steden.zone(p.getX(), p.getY(), p.getZ());
        float b = bui(l, zone), gl = gloed(l, zone), mi = mist(l);
        mist += (mi * k - mist) * 0.05f;
        gloed += (gl * k - gloed) * 0.05f;
        regen += ((zone == 4 ? b : 0f) * k - regen) * 0.05f;
        if (k <= 0.001f) {
            return;
        }
        switch (zone) {
            case 2 -> vallen(l, p, b * k, 14, "sneeuw");
            case 3 -> vallen(l, p, b * k, 10, "bloesem");
            case 4 -> {
                vallen(l, p, b * k, 16, "regen");
                if (b > 0.05f && l.getGameTime() >= volgendRegengeluid) {
                    volgendRegengeluid = l.getGameTime() + 18;
                    l.playLocalSound(p.getX(), p.getY() + 4, p.getZ(), SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.12f * b, 1.25f, false);
                }
                if (gl > 0.01f) {
                    gloedMotjes(l, p, gl * k);
                }
            }
            case 5 -> wolken(l, p, (0.3f + 0.7f * b) * k);
            case 6 -> zand(l, p, b * k);
            default -> {
            }
        }
        if (nacht(l) && RitmeConfig.vuurvliegjes() && Steden.op(p.getX(), p.getY(), p.getZ()) != null) {
            vuurvliegjes(l, p, k);
        }
    }

    /** Falling weather around the player: snowflakes, blossom petals or rain drops (only where the sky is open above). */
    private static void vallen(ClientLevel l, LocalPlayer p, float sterkte, int max, String soort) {
        int n = Math.round(max * sterkte);
        for (int i = 0; i < n; i++) {
            double x = p.getX() + R.nextGaussian() * 11, z = p.getZ() + R.nextGaussian() * 11;
            int top = l.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            double y = Math.max(p.getY() + 6 + R.nextDouble() * 7, top + 0.5);
            if (y - p.getY() > 24) {
                continue;   // (deep under cover: nothing falls here)
            }
            switch (soort) {
                case "sneeuw" -> l.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, R.nextGaussian() * 0.02, -0.06 - R.nextDouble() * 0.04, R.nextGaussian() * 0.02);
                case "bloesem" -> l.addParticle(R.nextInt(6) == 0 ? ParticleTypes.SPORE_BLOSSOM_AIR : ParticleTypes.CHERRY_LEAVES, x, y, z, 0, 0, 0);
                default -> l.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0, 0, 0);
            }
        }
    }

    private static void gloedMotjes(ClientLevel l, LocalPlayer p, float sterkte) {
        int n = Math.round(4 * sterkte);
        for (int i = 0; i < n; i++) {
            double x = p.getX() + R.nextGaussian() * 9, z = p.getZ() + R.nextGaussian() * 9, y = p.getY() + R.nextDouble() * 4;
            l.addParticle(R.nextBoolean() ? GLOED : GLOED2, x, y, z, 0, 0.02, 0);
            if (R.nextInt(14) == 0) {
                l.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.01, 0);
            }
        }
    }

    private static void wolken(ClientLevel l, LocalPlayer p, float sterkte) {
        if (R.nextFloat() > sterkte * 0.8f) {
            return;
        }
        double a = R.nextDouble() * Math.PI * 2, r = 10 + R.nextDouble() * 14;
        double x = p.getX() + Math.cos(a) * r, z = p.getZ() + Math.sin(a) * r, y = p.getY() - 4 + R.nextDouble() * 10;
        for (int i = 0; i < 4; i++) {
            l.addParticle(ParticleTypes.CLOUD, x + i * 0.6, y + R.nextGaussian() * 0.15, z + R.nextGaussian() * 0.3, 0.06, 0, 0.01);
        }
    }

    /** A warm sand swirl: a slowly wandering helix of sand-coloured dust near the player. */
    private static void zand(ClientLevel l, LocalPlayer p, float sterkte) {
        // a light dusty breeze always, the swirl during the moment
        if (R.nextFloat() < 0.25f * RitmeConfig.sterkte()) {
            l.addParticle(ZAND2, p.getX() + R.nextGaussian() * 10, p.getY() + R.nextDouble() * 2, p.getZ() + R.nextGaussian() * 10, 0.05, 0, 0);
        }
        if (sterkte <= 0.01f) {
            return;
        }
        wervelHoek += 0.35f;
        double t = l.getGameTime() * 0.01;
        double cx = p.getX() + Math.cos(t) * 7, cz = p.getZ() + Math.sin(t * 1.3) * 7;
        int top = l.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(cx), Mth.floor(cz));
        if (Math.abs(top - p.getY()) > 12) {
            return;
        }
        int n = Math.round(8 * sterkte);
        for (int i = 0; i < n; i++) {
            double h = R.nextDouble() * 3.5, hoek = wervelHoek + h * 2.2 + i * 0.8, r = 0.5 + h * 0.45;
            l.addParticle(i % 3 == 0 ? ZAND2 : ZAND, cx + Math.cos(hoek) * r, top + h, cz + Math.sin(hoek) * r, -Math.sin(hoek) * 0.1, 0.02, Math.cos(hoek) * 0.1);
        }
    }

    private static void vuurvliegjes(ClientLevel l, LocalPlayer p, float k) {
        if (R.nextFloat() > 0.6f * Math.max(0.3f, k)) {
            return;
        }
        double x = p.getX() + R.nextGaussian() * 10, z = p.getZ() + R.nextGaussian() * 10;
        BlockPos b = BlockPos.containing(x, p.getY() + 3, z);
        int top = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, b.getX(), b.getZ());
        double y = Math.abs(top - p.getY()) < 6 ? top + 0.4 + R.nextDouble() * 2.2 : p.getY() + 0.5 + R.nextDouble() * 2;
        l.addParticle(ParticleTypes.FIREFLY, x, y, z, 0, 0, 0);
    }

    // ---- fog -------------------------------------------------------------------------------------------------------------------

    static void kleur(ViewportEvent.ComputeFogColor e) {
        if (e.getCamera().getFluidInCamera() != FogType.ATMOSPHERIC && e.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        if (gloed > 0.005f) {
            float m = gloed * 0.45f;
            e.setRed(Mth.lerp(m, e.getRed(), 1.0f));
            e.setGreen(Mth.lerp(m, e.getGreen(), 0.62f));
            e.setBlue(Mth.lerp(m, e.getBlue(), 0.55f));
        }
        if (regen > 0.005f) {
            float m = regen * 0.3f;
            e.setRed(Mth.lerp(m, e.getRed(), 0.78f));
            e.setGreen(Mth.lerp(m, e.getGreen(), 0.74f));
            e.setBlue(Mth.lerp(m, e.getBlue(), 0.82f));
        }
        if (mist > 0.005f) {
            float m = mist * 0.6f;
            e.setRed(Mth.lerp(m, e.getRed(), 0.9f));
            e.setGreen(Mth.lerp(m, e.getGreen(), 0.9f));
            e.setBlue(Mth.lerp(m, e.getBlue(), 0.94f));
        }
    }

    static void afstand(ViewportEvent.RenderFog e) {
        if (e.getType() != FogType.ATMOSPHERIC) {
            return;
        }
        float f = 1f;
        f *= Mth.lerp(mist, 1f, 0.32f);
        f *= Mth.lerp(gloed, 1f, 0.75f);
        f *= Mth.lerp(regen, 1f, 0.8f);
        if (f < 0.999f) {
            e.setFarPlaneDistance(e.getFarPlaneDistance() * f);
            e.setNearPlaneDistance(e.getNearPlaneDistance() * f);
        }
    }
}
