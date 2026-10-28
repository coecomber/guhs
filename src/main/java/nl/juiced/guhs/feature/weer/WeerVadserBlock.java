package nl.juiced.guhs.feature.weer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;

/**
 * De Weer vadser ({@code guhs:weer_vadser}, 1.8.0): a guh machine that makes its own weather in a circle of
 * {@link WeerZones#STRAAL} blocks around it. Right-click: its menu (choose a weather; the guh-weertjes you have not unlocked yet
 * are locked, with a hint where their weerpotje is). Right-click with a weerpotje: unlocked at once; with the Reisbureau's
 * "Regenboogje in een potje": the regenboogweer is unlocked and the souvenir stays yours. Needs {@link WeerFeature#KRACHT}
 * vadskracht while it makes a weather. Everything else: {@link WeerVadserBlockEntity}.
 */
public class WeerVadserBlock extends MachineBlock {
    public static final MapCodec<WeerVadserBlock> CODEC = simpleCodec(WeerVadserBlock::new);

    public WeerVadserBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeerVadserBlockEntity(pos, state);
    }

    /** The Reisbureau's rare Wolkenweide souvenir, "Regenboogje in een potje". */
    public static boolean isRegenboogje(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(Guhs.id("reisbureau_zeldzaam_wolkenweide"));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof WeerPotjeItem potje) {
            if (player instanceof ServerPlayer sp && WeerUnlocks.ontgrendel(sp, potje.weertje())) {
                stack.consume(1, sp);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof WolkenantenneBlock antenne) {
            if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof WeerVadserBlockEntity be) {
                be.antenneErin(sp, antenne.niveau, stack);
            }
            return InteractionResult.SUCCESS;
        }
        if (isRegenboogje(stack)) {
            if (player instanceof ServerPlayer sp) {
                WeerUnlocks.ontgrendel(sp, Weertje.REGENBOOG);     // (the souvenir stays: it is a decoration too)
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof WeerVadserBlockEntity be) {
            be.open(sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        WeerZones.laatLos(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
