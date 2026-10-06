package nl.juiced.guhs.feature.guhpixel.kantoor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A Prikklok: the (at most four) Bureautjes that belong to it, in the order they were connected. */
public class PrikklokBlockEntity extends BlockEntity {
    private final List<BlockPos> bureaus = new ArrayList<>();

    public PrikklokBlockEntity(BlockPos pos, BlockState state) {
        super(KantoorSlice.PRIKKLOK_BE.get(), pos, state);
    }

    /** The desks of this clock (a copy). */
    public List<BlockPos> bureaus() {
        return new ArrayList<>(bureaus);
    }

    public boolean vol() {
        return bureaus.size() >= Kantoor.MAX_BUREAUS;
    }

    public boolean voegToe(BlockPos pos) {
        if (vol() || bureaus.contains(pos)) {
            return false;
        }
        bureaus.add(pos.immutable());
        setChanged();
        return true;
    }

    public void verwijder(BlockPos pos) {
        if (bureaus.remove(pos)) {
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        bureaus.clear();
        for (long l : in.read("Bureaus", com.mojang.serialization.Codec.LONG.listOf()).orElse(List.of())) {
            if (bureaus.size() < Kantoor.MAX_BUREAUS) {
                bureaus.add(BlockPos.of(l));
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("Bureaus", com.mojang.serialization.Codec.LONG.listOf(), bureaus.stream().map(BlockPos::asLong).toList());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            Kantoor.klokWeg(server, pos, this);
        }
    }
}
