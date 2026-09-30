package nl.juiced.guhs.feature.huisje;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * A Guhhuisje (2.10): a little house shaped like a guh HEAD (the fluffy ears are the roof, the windows are its eyes, the
 * snoet is the door), in three sizes ({@link HuisjeMaat}). This is the controller block (front row, middle); the rest of
 * the head is invisible {@link HuisjeDeelBlock}s and {@link HuisjeBlockEntity}'s renderer draws the whole head.
 * Right-click (owner): the huisje screen; with a picked-up guh or a maatje item: it moves right in.
 * 3.0 (timmerguh): everyone else may LOOK at the screen (read-only: grey buttons and "Dit is het huisje van X"), but only
 * the owner (or an op) changes it, moves guhs in or out, or breaks it (fundament: {@link Huisjes#magBewerken}).
 */
public class HuisjeBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private final HuisjeMaat maat;
    private final MapCodec<HuisjeBlock> codec;

    public HuisjeBlock(HuisjeMaat maat, Properties properties) {
        super(properties);
        this.maat = maat;
        this.codec = simpleCodec(p -> new HuisjeBlock(maat, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public HuisjeMaat maat() {
        return maat;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        for (BlockPos p : Huisje.blokken(context.getClickedPos(), facing, maat)) {
            if (!level.isInWorldBounds(p) || !level.getBlockState(p).canBeReplaced(context)) {
                return null;   // not enough room for the whole guh head
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        zetDelen(level, pos, state);
        if (level instanceof ServerLevel sl && placer instanceof ServerPlayer player) {
            Huisje h = Huisjes.registreer(sl, pos, state.getValue(FACING), maat, player.getUUID());
            player.sendSystemMessage(Component.translatable("gui.guhs.huisje.gebouwd", h.naam()).withStyle(ChatFormatting.LIGHT_PURPLE));
            GidsFeature.grant(player, "lieve_vadsjes/huisje_gebouwd");
            if (maat == HuisjeMaat.GROOT) {
                GidsFeature.grant(player, "lieve_vadsjes/huisje_groot");
            }
        }
    }

    private void zetDelen(Level level, BlockPos pos, BlockState state) {
        BlockState deel = HuisjeFeature.DEEL.get().defaultBlockState();
        for (BlockPos p : Huisje.blokken(pos, state.getValue(FACING), maat)) {
            if (!p.equals(pos)) {
                level.setBlock(p, HuisjeDeelBlock.voor(deel, pos, p), 3);
            }
        }
    }

    /** Builds a huisje (the controller and all its parts) and registers it for this owner (tests, AutoCheck). */
    public static Huisje bouw(ServerLevel level, BlockPos pos, Direction facing, HuisjeMaat maat, UUID eigenaar) {
        HuisjeBlock blok = HuisjeFeature.blok(maat);
        BlockState state = blok.defaultBlockState().setValue(FACING, facing);
        level.setBlock(pos, state, 3);
        blok.zetDelen(level, pos, state);
        return Huisjes.registreer(level, pos, facing, maat, eigenaar);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel sl) {
            Huisjes.verwijder(sl, pos);
            for (BlockPos p : Huisje.blokken(pos, state.getValue(FACING), maat)) {
                if (!p.equals(pos) && level.getBlockState(p).getBlock() instanceof HuisjeDeelBlock) {
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** 3.0: only the owner (or an op) can break a huisje: for anyone else it doesn't even crack (BreakEvent is cancelled too). */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof HuisjeBlockEntity be && !be.magBreken(player) ? 0f
                : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;   // (the whole head is drawn big by HuisjeRenderer)
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HuisjeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, HuisjeFeature.HUISJE_BE.get(), HuisjeBlockEntity::serverTick);
    }

    // --- using it ----------------------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        Huisje h = kijk(sl, pos, state, sp);
        if (h != null) {
            HuisjePayloads.open(sp, h);   // (3.0: for others read-only, the screen greys its buttons: data "MagBewerken")
        }
        return InteractionResult.CONSUME;
    }

    /** With a picked-up guh or a maatje in your hand: it moves right in (out of the door). */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        boolean guh = stack.is(ModItems.PICKED_UP_GUH.get());
        if (!guh && !(stack.getItem() instanceof PiepDierItem)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        Huisje h = eigen(sl, pos, state, sp);
        if (h == null) {
            return InteractionResult.CONSUME;
        }
        if (h.isVol()) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.huisje.vol", h.naam()).withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.CONSUME;
        }
        BlockPos d = h.deur();
        Entity e;
        if (guh) {
            e = PickedUpGuhItem.release(sl, PickedUpGuhItem.guhData(stack), d.getX() + 0.5, d.getY(), d.getZ() + 0.5, h.facing().toYRot());
        } else {
            e = PiepDierItem.zetNeer(stack, sl, Vec3.atBottomCenterOf(d), h.facing().toYRot(), ((PiepDierItem) stack.getItem()).type());
        }
        if (e == null) {
            return InteractionResult.CONSUME;
        }
        stack.consume(1, sp);
        if (!Huisjes.trekIn(h, e)) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.huisje.niet_jouw_guh").withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    /** The huisje, when this player may use it (the owner; an unregistered huisje is claimed); else a message and null. */
    @Nullable
    private Huisje eigen(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        Huisje h = kijk(level, pos, state, player);
        if (h != null && !Huisjes.magBewerken(player, h)) {
            player.sendOverlayMessage(Huisjes.vanWie(h).copy().withStyle(ChatFormatting.GRAY));   // (3.0: "Dit is het huisje van X")
            return null;
        }
        return h;
    }

    /**
     * 3.0 (timmerguh): the huisje to LOOK at, for anyone (an unregistered huisje is claimed by the first one to use it). For
     * someone who may not change it, the "Dit is het huisje van X" line shows too; the screen is read-only for them.
     */
    @Nullable
    private Huisje kijk(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        Huisje h = Huisjes.van(level, pos);
        if (h == null) {
            h = Huisjes.registreer(level, pos, state.getValue(FACING), maat, player.getUUID());
        }
        return h;
    }

    /** (tests, 3.0) what a right-click without an item opens for this player: the huisje (read-only for others), or null. */
    @Nullable
    public static Huisje bekijk(ServerLevel level, BlockPos anyPart, ServerPlayer player) {
        Huisje h = Huisjes.van(level, anyPart);
        if (h == null) {
            return null;
        }
        BlockState state = level.getBlockState(h.pos());
        return state.getBlock() instanceof HuisjeBlock b ? b.kijk(level, h.pos(), state, player) : null;
    }

    /** (tests, 3.0) what a right-click with a picked-up guh or maatje allows: the huisje for its owner, else null (refused). */
    @Nullable
    public static Huisje bewerk(ServerLevel level, BlockPos anyPart, ServerPlayer player) {
        Huisje h = Huisjes.van(level, anyPart);
        if (h == null) {
            return null;
        }
        BlockState state = level.getBlockState(h.pos());
        return state.getBlock() instanceof HuisjeBlock b ? b.eigen(level, h.pos(), state, player) : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("block.guhs.guhhuisje.lore", maat.plekken()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("block.guhs." + maat.blokId() + ".lore").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** (tests) whether a placed band guh / maatje can move in here right now. */
    static boolean kanErbij(Huisje h, Entity e) {
        return Huisjes.kanBewoner(e) && h.eigenaar().equals(Band.eigenaar(e)) && (!h.isVol() || h.bewoners().contains(Band.id(e)));
    }

    static boolean isMaatje(Entity e) {
        return e instanceof TamableAnimal && !(e instanceof nl.juiced.guhs.entity.GuhEntity);
    }
}
