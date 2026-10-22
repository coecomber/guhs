package nl.juiced.guhs.feature.safari.quests.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.quests.SafariQuests;

/**
 * Client side of slice S4: the pootafdrukken light up while you look through a spyglass (or S6's verrekijker,
 * {@link SafariQuests#VERREKIJKER}): every few ticks the prints in front of you (within 32 blocks, in your view) get a
 * few glowing particles, so a trail to a hider shows up from afar. Test round 4: also the quest tracker
 * ({@link QuestTrackerHud}).
 */
public final class SafariQuestsClient {
    private static final int BEREIK = 32;

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        // test round 4: the quest tracker (QuestTrackerHud; top left since test round 5)
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) ->
                event.registerAboveAll(nl.juiced.guhs.Guhs.id("safari_quest_tracker"), QuestTrackerHud::extractRenderState));
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        ClientLevel level = mc.level;
        if (p == null || level == null || mc.isPaused() || !Safari.echt(level) || p.tickCount % 6 != 0
                || !(p.isScoping() || SafariQuests.VERREKIJKER.test(p))) {
            return;
        }
        Vec3 oog = p.getEyePosition();
        Vec3 kijk = p.getLookAngle();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int px = p.blockPosition().getX(), py = p.blockPosition().getY(), pz = p.blockPosition().getZ();
        for (int dx = -BEREIK; dx <= BEREIK; dx++) {
            for (int dz = -BEREIK; dz <= BEREIK; dz++) {
                double ax = dx + 0.5, az = dz + 0.5;
                double afstand = Math.sqrt(ax * ax + az * az);
                if (afstand > BEREIK || afstand < 2 || (ax * kijk.x + az * kijk.z) / afstand < 0.85) {
                    continue;                      // only in front of you, in a narrow cone (you are looking through a lens)
                }
                for (int dy = -10; dy <= 10; dy++) {
                    m.set(px + dx, py + dy, pz + dz);
                    if (level.getBlockState(m).is(SafariQuests.POOTAFDRUK.get())) {
                        Vec3 c = Vec3.atBottomCenterOf(m).add(0, 0.05, 0);
                        if (c.distanceTo(oog) < BEREIK + 2) {
                            level.addParticle(ParticleTypes.GLOW, c.x + (level.getRandom().nextDouble() - 0.5) * 0.6, c.y + 0.1,
                                    c.z + (level.getRandom().nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
                        }
                    }
                }
            }
        }
    }

    private SafariQuestsClient() {
    }
}
