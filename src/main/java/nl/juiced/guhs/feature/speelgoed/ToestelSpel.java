package nl.juiced.guhs.feature.speelgoed;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A guh goes to a glijbaantje, wip or schommel and gets on (a {@link ZitjeEntity}); from then on the seat runs the ride
 * (laps on the glijbaantje, a while on the wip/schommel) and hands out the hearts at the end. This task is done as soon
 * as the guh sits.
 */
class ToestelSpel extends SpeelTaak {
    private final BlockPos pos;
    private int plek;
    private int wachten;

    ToestelSpel(Mob mob, ServerLevel level, BlockPos pos, int plek) {
        super(mob, level);
        this.pos = pos.immutable();
        this.plek = plek;
    }

    BlockPos pos() {
        return pos;
    }

    @Override
    public int maxTicks() {
        return 500;
    }

    @Override
    protected boolean speel() {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ToestelBlock t)) {
            return false;
        }
        if (ToestelBlock.zitje(level, pos, plek) != null) {       // someone got there first: another seat, or wait a bit
            int vrij = t.vrijePlek(level, pos);
            if (vrij >= 0) {
                plek = vrij;
            } else if (++wachten > 100) {
                return false;
            } else {
                mob.getLookControl().setLookAt(Vec3.atCenterOf(pos));
                return true;
            }
        }
        Vec3 instap = t.instap(pos, state, plek);
        if (loopNaar(instap, 1.1, 1.3)) {
            int duur;
            int rondjes = 1;
            if (t instanceof GlijbaanBlock) {
                rondjes = 1 + level.getRandom().nextInt(3);
                duur = 0;
            } else {
                duur = 240 + level.getRandom().nextInt(240);
            }
            ZitjeEntity.zet(level, pos, plek, mob, duur, rondjes);
            return false;
        }
        return !vast();
    }
}
