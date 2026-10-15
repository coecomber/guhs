package nl.juiced.guhs.feature.einde2.trap5;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * The four Elementmachines of tier 5 (DESIGN 6), each a guh machine on vadskracht with a face, each doing what its element
 * does, gently (nothing gets hurt, nothing is lost):
 * <ul>
 *   <li><b>Luchtblazer</b>: blows items, guhs and players away from its snoet, up to {@link T5Getallen#BLAZER_BEREIK}
 *       blocks; placed while looking down it blows UP: a fan lift (and nobody falls hard off it).</li>
 *   <li><b>Waterdrager</b>: waters the farmland around it, fills cauldrons next to it and empty buckets in it (or in your
 *       hand).</li>
 *   <li><b>Korstduwer</b>: a piston on vadskracht: at a redstone pulse (or a click) it shoves the row of blocks in front of
 *       its snoet one block on (at most {@link T5Getallen#DUWER_MAX}; never containers, machines, unbreakable blocks or
 *       anything in a protected building or somebody else's huisje).</li>
 *   <li><b>Vuurkorf</b>: warms the ground around it: items that lie in its glow are baked (as a furnace would), snow and ice
 *       melt.</li>
 * </ul>
 */
public final class Elementmachines {
    private Elementmachines() {
    }

    // =====================================================================================================================
    // Luchtblazer
    // =====================================================================================================================

    public static class LuchtblazerBlock extends MachineBlock {
        public static final MapCodec<LuchtblazerBlock> CODEC = simpleCodec(LuchtblazerBlock::new);
        public static final BooleanProperty OMHOOG = BooleanProperty.create("omhoog");

        public LuchtblazerBlock(Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(OMHOOG, false));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(OMHOOG);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null ? null : state.setValue(OMHOOG, context.getPlayer() != null && context.getPlayer().getXRot() > 50f);
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Luchtblazer(pos, state);
        }
    }

    public static class Luchtblazer extends MachineBlockEntity {
        boolean oefen;

        public Luchtblazer(BlockPos pos, BlockState state) {
            super(E2Trap5Feature.LUCHTBLAZER_BE.get(), pos, state, T5Getallen.LUCHTBLAZER, 1);
        }

        Direction richting() {
            BlockState s = getBlockState();
            return s.hasProperty(LuchtblazerBlock.OMHOOG) && s.getValue(LuchtblazerBlock.OMHOOG) ? Direction.UP : s.getValue(MachineBlock.FACING);
        }

        /** How far the wind reaches: up to the first solid block. */
        int bereik(Level level) {
            Direction d = richting();
            for (int i = 1; i <= T5Getallen.BLAZER_BEREIK; i++) {
                BlockPos p = worldPosition.relative(d, i);
                if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                    return i - 1;
                }
            }
            return T5Getallen.BLAZER_BEREIK;
        }

        @Override
        protected boolean kanWerken() {
            return true;
        }

        @Override
        protected boolean isVol() {
            return false;
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
        protected void werk() {
            if (!(level instanceof ServerLevel server) || Math.floorMod(server.getGameTime(), 2L) != 0) {
                return;
            }
            Direction d = richting();
            int n = bereik(server);
            if (n <= 0) {
                return;
            }
            BlockPos eind = worldPosition.relative(d, n);
            AABB wind = new AABB(Vec3.atLowerCornerOf(worldPosition.relative(d)), Vec3.atLowerCornerOf(eind).add(1, 1, 1)).inflate(0.15);
            boolean iets = false;
            for (Entity e : server.getEntities((Entity) null, wind, e -> (e instanceof ItemEntity || e instanceof LivingEntity) && !e.isSpectator())) {
                if (e instanceof Player pl && pl.getAbilities().flying) {
                    continue;
                }
                Vec3 v = e.getDeltaMovement();
                if (d == Direction.UP) {
                    e.setDeltaMovement(v.x * 0.9, Math.min(0.45, Math.max(v.y, 0) + 0.12), v.z * 0.9);
                    e.resetFallDistance();
                } else {
                    Vec3 duw = new Vec3(d.getStepX(), 0, d.getStepZ()).scale(0.08);
                    Vec3 nieuw = v.add(duw);
                    double h = Math.sqrt(nieuw.x * nieuw.x + nieuw.z * nieuw.z);
                    if (h > 0.6) {
                        nieuw = new Vec3(nieuw.x / h * 0.6, nieuw.y, nieuw.z / h * 0.6);
                    }
                    e.setDeltaMovement(nieuw);
                }
                e.hurtMarked = true;
                iets = true;
            }
            if (iets && !oefen) {
                E2Trap5.meld(T5Machine.speler(server, eigenaar()), E2Trap5.P_LUCHT);
            }
        }

        @Override
        protected void clientTick() {
            if (level == null || !bezig() || level.getRandom().nextInt(2) != 0) {
                return;
            }
            Direction d = richting();
            int n = bereik(level);
            double t = 0.6 + level.getRandom().nextDouble() * Math.max(0.5, n);
            Vec3 p = Vec3.atCenterOf(worldPosition).add(d.getStepX() * t, d.getStepY() * t, d.getStepZ() * t)
                    .add((level.getRandom().nextDouble() - 0.5) * 0.6, (level.getRandom().nextDouble() - 0.5) * 0.6, (level.getRandom().nextDouble() - 0.5) * 0.6);
            level.addParticle(ParticleTypes.CLOUD, p.x, p.y, p.z, d.getStepX() * 0.15, d.getStepY() * 0.15, d.getStepZ() * 0.15);
        }

        @Override
        protected void opslaan(ValueOutput uit) {
            uit.putBoolean("Oefen", oefen);
        }

        @Override
        protected void laden(ValueInput in) {
            oefen = in.getBooleanOr("Oefen", false);
        }
    }

    // =====================================================================================================================
    // Waterdrager
    // =====================================================================================================================

    public static class WaterdragerBlock extends MachineBlock {
        public static final MapCodec<WaterdragerBlock> CODEC = simpleCodec(WaterdragerBlock::new);

        public WaterdragerBlock(Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Waterdrager(pos, state);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            if (!stack.is(Items.BUCKET)) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof Waterdrager w) {
                if (!w.heeftKracht()) {
                    speler.sendOverlayMessage(Component.translatable("block.guhs.waterdrager.slaapt"));
                } else {
                    stack.consume(1, speler);
                    speler.getInventory().placeItemBackInInventory(new ItemStack(Items.WATER_BUCKET));
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8f, 1.1f);
                    if (!w.oefen) {
                        E2Trap5.meld(speler, E2Trap5.P_WATER);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class Waterdrager extends MachineBlockEntity {
        public static final int IN = 0, UIT = 1;
        boolean oefen;
        private int tel;

        public Waterdrager(BlockPos pos, BlockState state) {
            super(E2Trap5Feature.WATERDRAGER_BE.get(), pos, state, T5Getallen.WATERDRAGER, 2);
        }

        @Override
        protected boolean isInvoer(int vak) {
            return vak == IN;
        }

        @Override
        protected boolean isUitvoer(int vak) {
            return vak == UIT;
        }

        @Override
        protected boolean past(int vak, net.neoforged.neoforge.transfer.item.ItemResource wat) {
            return vak != IN || wat.toStack().is(Items.BUCKET);
        }

        @Override
        protected boolean kanWerken() {
            return true;
        }

        @Override
        protected boolean isVol() {
            return vakken().getAmountAsInt(IN) > 0 && vakken().getAmountAsInt(UIT) > 0;
        }

        @Override
        protected void werk() {
            if (!(level instanceof ServerLevel server) || ++tel < 40) {
                return;
            }
            tel = 0;
            boolean iets = false;
            int r = T5Getallen.DRAGER_BEREIK;
            // the farmland around it
            for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-r, -2, -r), worldPosition.offset(r, 0, r))) {
                BlockState s = server.getBlockState(p);
                if (s.getBlock() instanceof FarmlandBlock && s.getValue(FarmlandBlock.MOISTURE) < FarmlandBlock.MAX_MOISTURE
                        && Bescherming.magWijzigen(server, p, eigenaar())) {
                    server.setBlock(p, s.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
                    server.sendParticles(ParticleTypes.SPLASH, p.getX() + 0.5, p.getY() + 1.05, p.getZ() + 0.5, 3, 0.3, 0, 0.3, 0);
                    iets = true;
                }
            }
            // cauldrons next to it
            for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
                BlockPos p = worldPosition.relative(d);
                BlockState s = server.getBlockState(p);
                if (!Bescherming.magWijzigen(server, p, eigenaar())) {
                    continue;
                }
                if (s.is(Blocks.CAULDRON)) {
                    server.setBlockAndUpdate(p, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1));
                    iets = true;
                } else if (s.is(Blocks.WATER_CAULDRON) && s.getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL) {
                    server.setBlockAndUpdate(p, s.setValue(LayeredCauldronBlock.LEVEL, s.getValue(LayeredCauldronBlock.LEVEL) + 1));
                    iets = true;
                }
            }
            // an empty bucket in it: filled
            if (vakken().getAmountAsInt(IN) > 0 && vakken().getAmountAsInt(UIT) == 0) {
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    if (vakken().extract(IN, vakken().getResource(IN), 1, tx) == 1
                            && vakken().insert(UIT, net.neoforged.neoforge.transfer.item.ItemResource.of(new ItemStack(Items.WATER_BUCKET)), 1, tx) == 1) {
                        tx.commit();
                        iets = true;
                    }
                }
            }
            if (iets) {
                server.playSound(null, worldPosition, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.4f);
                if (!oefen) {
                    E2Trap5.meld(T5Machine.speler(server, eigenaar()), E2Trap5.P_WATER);
                }
            }
        }

        @Override
        protected void clientTick() {
            if (level != null && bezig() && level.getRandom().nextInt(6) == 0) {
                level.addParticle(ParticleTypes.DRIPPING_WATER, worldPosition.getX() + 0.2 + level.getRandom().nextDouble() * 0.6, worldPosition.getY() - 0.05,
                        worldPosition.getZ() + 0.2 + level.getRandom().nextDouble() * 0.6, 0, 0, 0);
            }
        }

        @Override
        protected void opslaan(ValueOutput uit) {
            uit.putBoolean("Oefen", oefen);
        }

        @Override
        protected void laden(ValueInput in) {
            oefen = in.getBooleanOr("Oefen", false);
        }
    }

    // =====================================================================================================================
    // Korstduwer
    // =====================================================================================================================

    public static class KorstduwerBlock extends MachineBlock {
        public static final MapCodec<KorstduwerBlock> CODEC = simpleCodec(KorstduwerBlock::new);

        public KorstduwerBlock(Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Korstduwer(pos, state);
        }

        @Override
        protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
            super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
            if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof Korstduwer k) {
                boolean stroom = level.hasNeighborSignal(pos);
                if (stroom && !k.stroom) {
                    k.duwen = true;
                }
                k.stroom = stroom;
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof Korstduwer k) {
                if (!k.heeftKracht()) {
                    speler.sendOverlayMessage(Component.translatable("block.guhs.korstduwer.slaapt"));
                } else {
                    k.duwen = true;
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class Korstduwer extends MachineBlockEntity {
        boolean oefen;
        boolean duwen, stroom, vast;
        private int rust;

        public Korstduwer(BlockPos pos, BlockState state) {
            super(E2Trap5Feature.KORSTDUWER_BE.get(), pos, state, T5Getallen.KORSTDUWER, 1);
        }

        @Override
        protected boolean kanWerken() {
            if (rust > 0) {
                rust--;
            }
            return duwen && rust <= 0;
        }

        @Override
        protected boolean isVol() {
            return vast;
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
        protected void werk() {
            duwen = false;
            rust = T5Getallen.DUWER_TIK;
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            vast = !duw(server);
            if (!vast) {
                server.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.6f, 1.1f);
                if (!oefen) {
                    E2Trap5.meld(T5Machine.speler(server, eigenaar()), E2Trap5.P_AARDE);
                }
            } else {
                server.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.4f, 0.6f);
            }
        }

        /** Shoves the row in front one block on; false: nothing to shove, or something in the row may not move. */
        private boolean duw(ServerLevel server) {
            Direction d = getBlockState().getValue(MachineBlock.FACING);
            List<BlockPos> rij = new ArrayList<>();
            BlockPos p = worldPosition.relative(d);
            while (!server.getBlockState(p).isAir() && !server.getBlockState(p).canBeReplaced()) {
                BlockState s = server.getBlockState(p);
                if (rij.size() >= T5Getallen.DUWER_MAX || s.hasBlockEntity() || s.getDestroySpeed(server, p) < 0
                        || s.getPistonPushReaction() == PushReaction.BLOCK || s.getPistonPushReaction() == PushReaction.DESTROY
                        || !Bescherming.magWijzigen(server, p, eigenaar())) {
                    return false;
                }
                rij.add(p);
                p = p.relative(d);
            }
            if (rij.isEmpty() || server.isOutsideBuildHeight(p) || !Bescherming.magWijzigen(server, p, eigenaar())) {
                return false;
            }
            for (int i = rij.size() - 1; i >= 0; i--) {
                BlockPos van = rij.get(i);
                BlockState s = server.getBlockState(van);
                server.setBlock(van.relative(d), s, Block.UPDATE_ALL);
                server.setBlock(van, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            server.sendParticles(ParticleTypes.POOF, rij.get(0).getX() + 0.5, rij.get(0).getY() + 0.5, rij.get(0).getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
            return true;
        }

        @Override
        public void vadsRegels(Consumer<Component> regels) {
            if (vast) {
                regels.accept(Component.translatable("block.guhs.korstduwer.vast", T5Getallen.DUWER_MAX).withStyle(ChatFormatting.GOLD));
            }
        }

        @Override
        protected void opslaan(ValueOutput uit) {
            uit.putBoolean("Oefen", oefen);
            uit.putBoolean("Stroom", stroom);
        }

        @Override
        protected void laden(ValueInput in) {
            oefen = in.getBooleanOr("Oefen", false);
            stroom = in.getBooleanOr("Stroom", false);
        }
    }

    // =====================================================================================================================
    // Vuurkorf
    // =====================================================================================================================

    public static class VuurkorfBlock extends MachineBlock {
        public static final MapCodec<VuurkorfBlock> CODEC = simpleCodec(VuurkorfBlock::new);

        public VuurkorfBlock(Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Vuurkorf(pos, state);
        }
    }

    public static class Vuurkorf extends MachineBlockEntity {
        /** The persistent data key on an item entity: how long it has lain in the glow (ticks). */
        public static final String GLOED = "guhs_einde2_trap5_machines_gloed";
        boolean oefen;
        private int tel;

        public Vuurkorf(BlockPos pos, BlockState state) {
            super(E2Trap5Feature.VUURKORF_BE.get(), pos, state, T5Getallen.VUURKORF, 1);
        }

        @Override
        protected boolean kanWerken() {
            return true;
        }

        @Override
        protected boolean isVol() {
            return false;
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
        protected void werk() {
            if (!(level instanceof ServerLevel server) || ++tel < 20) {
                return;
            }
            tel = 0;
            boolean iets = false;
            int r = T5Getallen.KORF_BEREIK;
            for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition).inflate(r, 1, r))) {
                int gloed = item.getPersistentData().getIntOr(GLOED, 0) + 20;
                if (gloed < T5Getallen.KORF_BAK) {
                    item.getPersistentData().putInt(GLOED, gloed);
                    continue;
                }
                item.getPersistentData().remove(GLOED);
                ItemStack stack = item.getItem();
                SingleRecipeInput in = new SingleRecipeInput(stack.copyWithCount(1));
                ItemStack uit = server.recipeAccess().getRecipeFor(RecipeType.SMELTING, in, server).map(rec -> rec.value().assemble(in)).orElse(ItemStack.EMPTY);
                if (!uit.isEmpty()) {
                    item.setItem(uit.copyWithCount(Math.min(uit.getMaxStackSize(), uit.getCount() * stack.getCount())));
                    server.sendParticles(ParticleTypes.FLAME, item.getX(), item.getY() + 0.3, item.getZ(), 6, 0.15, 0.15, 0.15, 0.01);
                    iets = true;
                }
            }
            for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-r, -1, -r), worldPosition.offset(r, 1, r))) {
                BlockState s = server.getBlockState(p);
                if ((s.is(Blocks.SNOW) || s.is(Blocks.ICE) || s.is(Blocks.FROSTED_ICE)) && Bescherming.magWijzigen(server, p, eigenaar())) {
                    server.setBlockAndUpdate(p, s.is(Blocks.SNOW) ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState());
                    iets = true;
                }
            }
            if (iets && !oefen) {
                E2Trap5.meld(T5Machine.speler(server, eigenaar()), E2Trap5.P_VUUR);
            }
        }

        @Override
        protected void clientTick() {
            if (level != null && bezig() && level.getRandom().nextInt(3) == 0) {
                level.addParticle(level.getRandom().nextInt(4) == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, worldPosition.getX() + 0.3
                        + level.getRandom().nextDouble() * 0.4, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.3 + level.getRandom().nextDouble() * 0.4, 0, 0.02, 0);
            }
        }

        @Override
        protected void opslaan(ValueOutput uit) {
            uit.putBoolean("Oefen", oefen);
        }

        @Override
        protected void laden(ValueInput in) {
            oefen = in.getBooleanOr("Oefen", false);
        }
    }
}
