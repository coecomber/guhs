package nl.juiced.guhs.feature.bio.wereld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.wereld.DalBlokken;
import nl.juiced.guhs.feature.bio.wereld.DalWeer;

/**
 * biomes3 wereld, the Klaterdal: its day rhythm in the air, client side only and cheap. The biome file already lets a few
 * petals fall all the time; this adds the weather: when a gust of wind passes (about every two minutes, the same moment
 * for everybody: it follows the world's clock) or while it rains, many more petals come down around the player, drifting
 * the way the wind blows, and a gust starts with the wind chime. Outside the Klaterdal it does nothing (one biome lookup
 * per second). The rhythm itself is {@link DalWeer} (plain functions, tested on the server).
 */
public final class DalSfeer {
    private static boolean inDal;
    private static int tik;
    private static long laatsteVlaag = -1;

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(DalSfeer::onTick);
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) {
            inDal = false;
            return;
        }
        if (tik++ % 20 == 0) {
            inDal = Bio.in(level, mc.player.blockPosition(), Bio.KLATERDAL);
        }
        if (!inDal) {
            return;
        }
        long tijd = level.getGameTime();
        int aantal = DalWeer.blaadjes(tijd, level.isRaining());
        if (aantal <= 0) {
            return;
        }
        RandomSource rand = level.getRandom();
        long vlaag = Math.floorDiv(tijd, DalWeer.VLAAG_OM);
        if (vlaag != laatsteVlaag && DalWeer.vlaag(tijd) > 0.05) {
            laatsteVlaag = vlaag;
            level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), DalBlokken.WINDGONG.get(), SoundSource.AMBIENT, 0.55f,
                    0.95f + rand.nextFloat() * 0.1f, false);
        }
        // the wind turns slowly through the day; every gust blows one way
        double hoek = (vlaag * 2.399) % (Math.PI * 2);
        double wx = Math.cos(hoek) * 0.06, wz = Math.sin(hoek) * 0.06;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < aantal; i++) {
            double x = mc.player.getX() + (rand.nextDouble() - 0.5) * 28 - wx * 60, z = mc.player.getZ() + (rand.nextDouble() - 0.5) * 28 - wz * 60;
            int grond = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            double y = Math.max(grond + 1.5, mc.player.getY() + 2 + rand.nextDouble() * 9);
            if (!level.getBlockState(p.set(Mth.floor(x), Mth.floor(y), Mth.floor(z))).isAir()) {
                continue;
            }
            level.addParticle(ParticleTypes.CHERRY_LEAVES, x, y, z, wx, -0.02, wz);
        }
    }

    private DalSfeer() {
    }
}
