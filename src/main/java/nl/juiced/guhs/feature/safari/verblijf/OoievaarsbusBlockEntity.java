package nl.juiced.guhs.feature.safari.verblijf;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Op Safari! step 2 (S5): the Ooievaarsbus: 9 slots that friends' storks fill ({@link Ooievaar}); only its owner opens it. */
public class OoievaarsbusBlockEntity extends BlockEntity {
    private final SimpleContainer inhoud = new SimpleContainer(9) {
        @Override
        public void setChanged() {
            super.setChanged();
            OoievaarsbusBlockEntity.this.setChanged();
        }
    };
    @Nullable
    private UUID eigenaar;
    private String eigenaarNaam = "";

    public OoievaarsbusBlockEntity(BlockPos pos, BlockState state) {
        super(VerblijfFeature.BUS_BE.get(), pos, state);
    }

    public SimpleContainer inhoud() {
        return inhoud;
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    public String eigenaarNaam() {
        return eigenaarNaam;
    }

    public void zetEigenaar(UUID id, String naam) {
        eigenaar = id;
        eigenaarNaam = naam;
        setChanged();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            Containers.dropContents(level, pos, inhoud);
            if (eigenaar != null && level.getServer() != null) {
                Ooievaar.Bussen.get(level.getServer()).weg(eigenaar, level.dimension(), pos);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        eigenaar = in.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
        eigenaarNaam = in.getStringOr("EigenaarNaam", "");
        NonNullList<ItemStack> l = NonNullList.withSize(9, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in.childOrEmpty("Inhoud"), l);
        for (int i = 0; i < 9; i++) {
            inhoud.setItem(i, l.get(i));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.storeNullable("Eigenaar", UUIDUtil.CODEC, eigenaar);
        out.putString("EigenaarNaam", eigenaarNaam);
        NonNullList<ItemStack> l = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) {
            l.set(i, inhoud.getItem(i));
        }
        ContainerHelper.saveAllItems(out.child("Inhoud"), l);
    }
}
