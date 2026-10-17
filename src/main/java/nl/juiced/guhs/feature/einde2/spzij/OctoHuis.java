package nl.juiced.guhs.feature.einde2.spzij;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.bouwbikinibodem.BikinibodemBlokken;
import nl.juiced.guhs.feature.einde2.bouwbikinibodem.E2BouwBikinibodemFeature;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * einde2 "Sponsguh", slice sp_zij (CONTRACT_E2_C 7.10): Octo Tentakel's moai house, atmosphere only (DESIGN 5b: no brass-band
 * quest). Octo himself is at work behind the till of De Krokante Knabbel (so he never stands twice in town): at home you hear
 * him. Click his clarinet on its stand: an off-key toot ({@code stinger.klarinet}) and one of his two lines from upstairs.
 * Click one of his 22 self-portraits: what you see in that one (four pictures). Stepping in: one line about the portraits
 * (at most once per five minutes).
 */
public final class OctoHuis {
    static final String T = SpZij.P + "octo.";
    static final int RUST = 20 * 300;

    private static final Map<UUID, Long> BINNEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> KLIK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> REGEL = new ConcurrentHashMap<>();

    private OctoHuis() {
    }

    /** Every 5 ticks in town: the first step into the house. */
    static void tick(ServerPlayer p, ServerLevel level, long nu) {
        if (!Boomkoepel.inZone(level, "octo_huis", p.position())) {
            return;
        }
        Long laatst = BINNEN.get(p.getUUID());
        if (laatst != null && nu - laatst < RUST) {
            return;
        }
        BINNEN.put(p.getUUID(), nu);
        SpZij.merk(p, T + "binnen");
    }

    /** A click on the clarinet or a self-portrait inside Bikinibodem. */
    static void opKlik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND
                || p.level().dimension() != GuheindeFeature.GUHEINDE) {
            return;
        }
        BlockState state = p.level().getBlockState(event.getPos());
        boolean klarinet = state.is(E2BouwBikinibodemFeature.KLARINET.get()), portret = state.is(E2BouwBikinibodemFeature.ZELFPORTRET.get());
        if (!klarinet && !portret || !inStad(p.level(), event.getPos())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        long nu = SpZij.nu(p);
        Long laatst = KLIK.get(p.getUUID());
        if (laatst != null && nu - laatst < 10) {
            return;
        }
        KLIK.put(p.getUUID(), nu);
        if (klarinet) {
            klarinet(p, event.getPos());
        } else {
            int soort = state.hasProperty(BikinibodemBlokken.Zelfportret.SOORT) ? state.getValue(BikinibodemBlokken.Zelfportret.SOORT) : 0;
            SpZij.merk(p, T + "portret." + soort);
        }
    }

    /** The clarinet: a toot (off key), notes, and Octo from upstairs (his two lines in turn). */
    static void klarinet(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        level.playSound(null, pos, E2SponsguhFeature.OCTO_KLARINET.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.9f, 0.6f + p.getRandom().nextFloat() * 0.5f);
        SpAudio.stinger(p, "stinger.klarinet");
        level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 0.5);
        int n = REGEL.merge(p.getUUID(), 1, Integer::sum);
        SpZij.stem(p, "entity.guhs.octo_mika", T + "stem." + (1 + (n - 1) % 2));
    }

    static boolean inStad(ServerLevel level, BlockPos pos) {
        if (!Plekken.bekend(level, SpZij.BB)) {
            return false;
        }
        BoundingBox doos = Plekken.doos(level, SpZij.BB);
        return doos != null && doos.isInside(pos);
    }

    static void vergeet(UUID id) {
        BINNEN.remove(id);
        KLIK.remove(id);
        REGEL.remove(id);
    }
}
