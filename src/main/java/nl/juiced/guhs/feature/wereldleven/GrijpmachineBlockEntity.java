package nl.juiced.guhs.feature.wereldleven;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/** The plushies inside a grijpmachine (their spots on the floor of the glass case), synced to the clients for the renderer. */
public class GrijpmachineBlockEntity extends BlockEntity {
    /** A plushie on the floor of the case: its id, and x/z in 0..1 (x left to right, z back to front, seen from the front). */
    public record Prijs(String knuffel, float x, float z) {
    }

    private final List<Prijs> prijzen = new ArrayList<>();

    public GrijpmachineBlockEntity(BlockPos pos, BlockState state) {
        super(WereldlevenFeature.GRIJPMACHINE_BE.get(), pos, state);
    }

    /** The upper half's block entity: just there, empty (the lower half keeps the plushies and is drawn). */
    public boolean isBoven() {
        BlockState state = getBlockState();
        return state.hasProperty(GrijpmachineBlock.HALF) && state.getValue(GrijpmachineBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    public List<Prijs> prijzen() {
        return prijzen;
    }

    /** Fills the case when it's (nearly) empty. */
    public void vul(RandomSource random) {
        while (prijzen.size() < Grijpmachine.PRIJZEN) {
            prijzen.add(Grijpmachine.nieuwePrijs(random, prijzen));
        }
    }

    /** A machine fresh from a structure (or just placed) fills its case right away. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && !isBoven() && prijzen.size() < Grijpmachine.PRIJZEN) {
            vul(level.getRandom());
            setChanged();
        }
    }

    public void veranderd() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public CompoundTag prijzenTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Prijs p : prijzen) {
            CompoundTag t = new CompoundTag();
            t.putString("K", p.knuffel());
            t.putFloat("X", p.x());
            t.putFloat("Z", p.z());
            list.add(t);
        }
        tag.put("Prijzen", list);
        return tag;
    }

    public static List<Prijs> leesPrijzen(CompoundTag tag) {
        List<Prijs> out = new ArrayList<>();
        for (Tag t : tag.getListOrEmpty("Prijzen")) {
            CompoundTag c = (CompoundTag) t;
            if (WereldlevenFeature.knuffel(c.getStringOr("K", "")) != null) {
                out.add(new Prijs(c.getStringOr("K", ""), c.getFloatOr("X", 0.0F), c.getFloatOr("Z", 0.0F)));
            }
        }
        return out;
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.merge(prijzenTag());
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        prijzen.clear();
        prijzen.addAll(leesPrijzen(tag));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return prijzenTag();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
