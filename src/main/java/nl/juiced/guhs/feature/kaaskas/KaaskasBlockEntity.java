package nl.juiced.guhs.feature.kaaskas;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.techsaus.SausMachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.SausTank;
import nl.juiced.guhs.feature.vadskracht.Sauzen;

/**
 * The works of a {@link KaaskasBlock}: one kaassaus tank ({@link KaaskasGetallen#TANK} mB). While it has vadskracht and sauce,
 * every {@link KaaskasGetallen#DUW_TICKS} ticks it gives one growing crop in the kas a push ({@link CropBlock#growCrops}, like
 * a bit of bonemeal) for {@link KaaskasGetallen#SAUS_PER_DUW} mB. The kas is the box of {@link #gebied}: any crop block counts
 * (kaasknabbelplantjes, pluisbolletjes, roze graan, wheat...). Harvesting is somebody else's job: an Oogster, the guh chore
 * Kassier ({@link KassierKlus}) or your own paws.
 */
public class KaaskasBlockEntity extends SausMachineBlockEntity {
    private final SausTank saus;
    private int tik;
    private List<BlockPos> gewassen = List.of();
    private long gescand = -KaaskasGetallen.SCAN_TICKS;

    public KaaskasBlockEntity(BlockPos pos, BlockState state) {
        super(KaaskasFeature.KAASKAS_BE.get(), pos, state, KaaskasGetallen.VRAAG, 0);
        saus = maakTank(KaaskasGetallen.TANK, Sauzen.kaassaus());
    }

    public SausTank saus() {
        return saus;
    }

    /** Every spot of the kas around this kern (the kern itself left out). */
    public static List<BlockPos> gebied(BlockPos kern) {
        List<BlockPos> uit = new ArrayList<>();
        int r = KaaskasGetallen.STRAAL;
        for (BlockPos p : BlockPos.betweenClosed(kern.offset(-r, -KaaskasGetallen.ONDER, -r), kern.offset(r, KaaskasGetallen.BOVEN, r))) {
            if (!p.equals(kern)) {
                uit.add(p.immutable());
            }
        }
        return uit;
    }

    /** The crop blocks in the kas right now (looked up again every {@link KaaskasGetallen#SCAN_TICKS} ticks). */
    public List<BlockPos> gewassen(Level level) {
        long nu = level.getGameTime();
        if (nu - gescand >= KaaskasGetallen.SCAN_TICKS || nu < gescand) {
            gescand = nu;
            List<BlockPos> lijst = new ArrayList<>();
            for (BlockPos p : gebied(worldPosition)) {
                if (level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof CropBlock) {
                    lijst.add(p);
                }
            }
            gewassen = lijst;
        }
        return gewassen;
    }

    /** The ripe crops in the kas (for the Kassier). */
    public List<BlockPos> rijp(Level level) {
        List<BlockPos> uit = new ArrayList<>();
        for (BlockPos p : gewassen(level)) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s)) {
                uit.add(p);
            }
        }
        return uit;
    }

    @Override
    protected boolean isInvoer(int vak) {
        return false;
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return false;
    }

    @Override
    protected boolean kanWerken() {
        return saus.inhoud() >= KaaskasGetallen.SAUS_PER_DUW;
    }

    @Override
    protected boolean isVol() {
        return false;
    }

    @Override
    protected void werk() {
        if (!(level instanceof ServerLevel server) || ++tik < KaaskasGetallen.DUW_TICKS) {
            return;
        }
        tik = 0;
        duw(server);
    }

    /** One push: a random growing crop of the kas grows a bit (returns false when nothing in the kas can grow). */
    public boolean duw(ServerLevel server) {
        List<BlockPos> groeit = new ArrayList<>();
        for (BlockPos p : gewassen(server)) {
            BlockState s = server.getBlockState(p);
            if (s.getBlock() instanceof CropBlock crop && !crop.isMaxAge(s)) {
                groeit.add(p);
            }
        }
        if (groeit.isEmpty() || saus.inhoud() < KaaskasGetallen.SAUS_PER_DUW) {
            return false;
        }
        BlockPos p = groeit.get(server.getRandom().nextInt(groeit.size()));
        BlockState s = server.getBlockState(p);
        ((CropBlock) s.getBlock()).growCrops(server, p, s);
        saus.tap(KaaskasGetallen.SAUS_PER_DUW, false);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.25, 0.2, 0.25, 0.0);
        return true;
    }

    @Nullable
    @Override
    protected Component wachtOp() {
        return saus.inhoud() < KaaskasGetallen.SAUS_PER_DUW ? Component.translatable("gui.guhs.kaaskas.wacht_saus") : null;
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        super.opslaan(uit);
        uit.putInt("Tik", tik);
    }

    @Override
    protected void laden(ValueInput in) {
        super.laden(in);
        tik = in.getIntOr("Tik", 0);
    }
}
