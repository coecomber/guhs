package nl.juiced.guhs.feature.techbron;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.vadskracht.BatterijBlock;
import nl.juiced.guhs.feature.vadskracht.BatterijBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;

/**
 * De Knabbelbatterij: the vadskracht battery ({@link BatterijBlock}, {@link VadsGetallen#BATTERIJ} VK: about half an hour
 * of one Guhrad) that KEEPS ITS CHARGE when it is broken: the charge travels on the item as the data component
 * {@code guhs:techbron_lading} (the loot table copies it from the block entity, placing the item puts it back), so a full
 * battery can be carried to another setup. An empty one has no component and stacks as usual.
 */
public class KnabbelbatterijBlock extends BatterijBlock {
    public static final MapCodec<KnabbelbatterijBlock> CODEC = simpleCodec(KnabbelbatterijBlock::new);

    public KnabbelbatterijBlock(Properties p) {
        super(p, VadsGetallen.BATTERIJ, () -> TechbronFeature.KNABBELBATTERIJ_BE.get());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Kern(pos, state);
    }

    /** The battery's content, which goes onto the item and comes back from it. */
    public static class Kern extends BatterijBlockEntity {
        public Kern(BlockPos pos, BlockState state) {
            super(TechbronFeature.KNABBELBATTERIJ_BE.get(), pos, state);
        }

        @Override
        protected void applyImplicitComponents(DataComponentGetter input) {
            super.applyImplicitComponents(input);
            Long lading = input.get(TechbronFeature.LADING.get());
            if (lading != null) {
                zetInhoud(lading);
            }
        }

        @Override
        protected void collectImplicitComponents(DataComponentMap.Builder components) {
            super.collectImplicitComponents(components);
            if (vadsInhoud() > 0) {
                components.set(TechbronFeature.LADING.get(), vadsInhoud());
            }
        }

        @Override
        public void removeComponentsFromTag(ValueOutput tag) {
            super.removeComponentsFromTag(tag);
            tag.discard("Inhoud");
        }
    }
}
