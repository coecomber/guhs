package nl.juiced.guhs.feature.kaasmoeras;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The motknabbel: a glowing lump of kaasmot-cheese that a kikkerguh spits out after snapping up a kaasmot (the
 * froglight of the kaasmoeras). It keeps the colour of the kikkerguh: roze, mint or geel (the block state {@link #KLEUR};
 * its item carries the colour in its block_state component, so it places and drops in the same colour).
 */
public class MotknabbelBlock extends Block {
    public static final MapCodec<MotknabbelBlock> CODEC = simpleCodec(MotknabbelBlock::new);
    public static final EnumProperty<Kleur> KLEUR = EnumProperty.create("kleur", Kleur.class);

    public enum Kleur implements StringRepresentable {
        ROZE, MINT, GEEL;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Kleur byIndex(int i) {
            return values()[Math.floorMod(i, values().length)];
        }
    }

    public MotknabbelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KLEUR, Kleur.ROZE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KLEUR);
    }

    /** A motknabbel item of this colour. */
    public static ItemStack stack(Kleur kleur, int count) {
        ItemStack stack = new ItemStack(KaasmoerasFeature.MOTKNABBEL_ITEM.get(), count);
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(KLEUR, kleur));
        return stack;
    }

    /** The colour of a motknabbel item (roze when it has none). */
    public static Kleur kleur(ItemStack stack) {
        Kleur k = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(KLEUR);
        return k == null ? Kleur.ROZE : k;
    }

    /** Pick block: the motknabbel of this colour. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return stack(state.getValue(KLEUR), 1);
    }

    /** The item: named after its colour ("Roze motknabbel"). */
    public static class Item extends BlockItem {
        public Item(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatable("block.guhs.motknabbel." + kleur(stack).getSerializedName());
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("block.guhs.motknabbel.lore").withStyle(ChatFormatting.GRAY));
        }
    }
}
