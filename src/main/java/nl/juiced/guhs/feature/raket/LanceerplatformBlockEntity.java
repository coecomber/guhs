package nl.juiced.guhs.feature.raket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The state of one Lanceerplatform: how many stages are done (0..4), how much of the current stage is in (per demand, in
 * the order of {@link RaketStappen.Stap#eisen}), whether the rocket went up once, who built it and the crew's ids.
 */
public class LanceerplatformBlockEntity extends BlockEntity {
    private int klaar;
    private int[] binnen = new int[4];
    private boolean gelanceerd;
    @Nullable
    private UUID bouwer;
    private final List<UUID> bemanning = new ArrayList<>();

    public LanceerplatformBlockEntity(BlockPos pos, BlockState state) {
        super(RaketFeature.LANCEERPLATFORM_BE.get(), pos, state);
    }

    /** How many stages are done (0..4). */
    public int klaar() {
        return klaar;
    }

    /** The stage being built now (1..4), or 5 when all four are done. */
    public int bezig() {
        return klaar + 1;
    }

    public int binnen(int eis) {
        return eis < binnen.length ? binnen[eis] : 0;
    }

    void zetBinnen(int eis, int n) {
        if (eis >= binnen.length) {
            binnen = java.util.Arrays.copyOf(binnen, eis + 1);
        }
        binnen[eis] = n;
        setChanged();
    }

    void stapKlaar() {
        klaar = Math.min(RaketStappen.STAPPEN.size(), klaar + 1);
        binnen = new int[4];
        setChanged();
    }

    public boolean gelanceerd() {
        return gelanceerd;
    }

    void zetGelanceerd(boolean g) {
        gelanceerd = g;
        setChanged();
    }

    @Nullable
    public UUID bouwer() {
        return bouwer;
    }

    void zetBouwer(UUID id) {
        if (bouwer == null) {
            bouwer = id;
            setChanged();
        }
    }

    public List<UUID> bemanning() {
        return bemanning;
    }

    void zetBemanning(List<UUID> ids) {
        bemanning.clear();
        bemanning.addAll(ids);
        setChanged();
    }

    /** Tests and /guhs raket reset: back to an empty platform. */
    void reset() {
        klaar = 0;
        binnen = new int[4];
        gelanceerd = false;
        bemanning.clear();
        setChanged();
    }

    /** Tests and /guhs raket stap: jump to {@code n} stages done. */
    void zetKlaar(int n) {
        klaar = Math.max(0, Math.min(RaketStappen.STAPPEN.size(), n));
        binnen = new int[4];
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        klaar = tag.getIntOr("Klaar", 0);
        for (int i = 0; i < binnen.length; i++) {
            binnen[i] = tag.getIntOr("Binnen" + i, 0);
        }
        gelanceerd = tag.getBooleanOr("Gelanceerd", false);
        bouwer = tag.read("Bouwer", UUIDUtil.CODEC).orElse(null);
        bemanning.clear();
        for (int i = 0; i < RaketStappen.BEMANNING; i++) {
            tag.read("Bemanning" + i, UUIDUtil.CODEC).ifPresent(bemanning::add);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putInt("Klaar", klaar);
        for (int i = 0; i < binnen.length; i++) {
            tag.putInt("Binnen" + i, binnen[i]);
        }
        tag.putBoolean("Gelanceerd", gelanceerd);
        if (bouwer != null) {
            tag.store("Bouwer", UUIDUtil.CODEC, bouwer);
        }
        for (int i = 0; i < bemanning.size(); i++) {
            tag.store("Bemanning" + i, UUIDUtil.CODEC, bemanning.get(i));
        }
    }
}
