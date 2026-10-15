package nl.juiced.guhs.feature.bio.wereld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.wereld.MeerRitme;

/**
 * Client side of the Bloesemmeertje: the day rhythm of its falling petals (the rule: {@link MeerRitme}). The biome itself always lets a few petals fall
 * (its ambient particle, tools/features/bio_wereld_meer.py); this adds more around the camera when there is reason to:
 * <ul>
 *   <li>a breeze that comes and goes by itself (a gust about every 75 seconds);</li>
 *   <li>the morning and the evening breeze: a minute of extra petals when day turns to night and back;</li>
 *   <li>rain (about three times as many) and thunder (more still);</li>
 *   <li>at night, without rain, it is quiet: half as many.</li>
 * </ul>
 * Cheap: one biome lookup a second, and at most a handful of vanilla petal particles a tick, only while the camera is in
 * the biome. The server knows nothing of it.
 */
public final class MeerClient {
    /** The reach around the camera (blocks) and above it. */
    static final int BEREIK = 14, OMHOOG = 11;

    private static boolean inMeer, donker;
    private static int bries;
    private static ClientLevel wereld;

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(MeerClient::onTick);
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            inMeer = false;
            wereld = null;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        long tijd = level.getGameTime();
        BlockPos hier = mc.player.blockPosition();
        if (level != wereld || tijd % 20 == 0) {
            boolean was = inMeer && level == wereld;
            wereld = level;
            inMeer = Bio.in(level, hier, Bio.BLOESEMMEERTJE);
            boolean nu = level.isDarkOutside();
            if (inMeer && was && nu != donker) {
                bries = MeerRitme.BRIES;
            }
            donker = nu;
        }
        if (!inMeer) {
            bries = 0;
            return;
        }
        if (bries > 0) {
            bries--;
        }
        RandomSource r = level.getRandom();
        float s = MeerRitme.sterkte(level.getRainLevel(1f), level.getThunderLevel(1f), donker, bries, tijd);
        int n = (int) s + (r.nextFloat() < s - (int) s ? 1 : 0);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < n; i++) {
            double x = hier.getX() + 0.5 + (r.nextDouble() * 2 - 1) * BEREIK, z = hier.getZ() + 0.5 + (r.nextDouble() * 2 - 1) * BEREIK;
            double y = hier.getY() + 2 + r.nextDouble() * OMHOOG;
            if (level.getBlockState(p.set(x, y, z)).isAir()) {
                level.addParticle(ParticleTypes.CHERRY_LEAVES, x, y, z, 0, 0, 0);
            }
        }
    }

    private MeerClient() {
    }
}
