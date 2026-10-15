package nl.juiced.guhs.feature.guhriow3;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.guhrio.GuhrioStuk;

/**
 * bbq2 (guhrio-w3): the pieces world 3 adds to Super Guhrio (blocks that implement {@link GuhrioStuk}: the engine finds
 * them in a lane by itself):
 * <ul>
 *     <li>{@link BaasPlek}: where the Grote Nether-Mika stands while somebody is in the duel arena.</li>
 *     <li>{@link HendelBlok}: the lever of the duel. Walk into it and the far half of the bridge drops.</li>
 *     <li>{@link ParkeerPaal}: a hitching post for Guhshi. He sneezes from Vuurpepers, so he waits here while you do a
 *     Vuurpeper puzzle on foot (riding him, a mouse button is his tongue, not a knabbel).</li>
 *     <li>{@link Peperstruik}: a Vuurpeper bush: walk through it and you have the Vuurpeper, as often as you like (a
 *     puzzle that needs knabbels can't be lost by losing your power-up).</li>
 * </ul>
 * Their models and textures: tools/features/guhrio_w3.py.
 */
public final class GuhrioW3Blocks {
    private GuhrioW3Blocks() {
    }

    /** The spot of the boss (invisible; only somebody holding the block sees it). */
    public static class BaasPlek extends GuhrioBlocks.WezenPlek {
        public static final MapCodec<BaasPlek> CODEC = simpleCodec(BaasPlek::new);

        public BaasPlek(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected Entity maak(ServerLevel level, GuhrioSpel.Actief actief, GuhrioSpel.Stuk stuk) {
            GroteNetherMikaEntity mika = GuhrioW3Feature.GROTE_NETHER_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
            if (mika != null) {
                BlockPos pos = stuk.pos();
                mika.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
                mika.zetBaan(actief, actief.level.banen().get(stuk.baan()), pos);
            }
            return mika;
        }
    }

    /** The boss of the level this player is in, or null. */
    @Nullable
    static GroteNetherMikaEntity baas(GuhrioSpel.Sessie sessie) {
        for (Entity e : GuhrioSpel.wezens(sessie.actief)) {
            if (e instanceof GroteNetherMikaEntity mika && mika.isAlive()) {
                return mika;
            }
        }
        return null;
    }

    /**
     * The lever of the duel: walk into it. {@link #GETROKKEN} is its look; the Grote Nether-Mika sets it (the fight is the
     * same for everybody in the arena) and puts it back up for the next fight.
     */
    public static class HendelBlok extends Block implements GuhrioStuk {
        public static final MapCodec<HendelBlok> CODEC = simpleCodec(HendelBlok::new);
        public static final BooleanProperty GETROKKEN = BooleanProperty.create("getrokken");
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);

        public HendelBlok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(GETROKKEN, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(GETROKKEN);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        public void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            GroteNetherMikaEntity mika = baas(sessie);
            if (mika != null) {
                mika.hendel(player);
            }
        }
    }

    /** Guhshi's hitching post: whoever rides him through it goes on on foot (he waits at his spots again). */
    public static class ParkeerPaal extends Block implements GuhrioStuk {
        public static final MapCodec<ParkeerPaal> CODEC = simpleCodec(ParkeerPaal::new);
        private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

        public ParkeerPaal(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        public void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            if (!sessie.guhshi) {
                return;
            }
            GuhrioSpel.zetGuhshi(player, sessie, false);
            player.sendOverlayMessage(Component.translatable("gui.guhs.guhriow3.parkeer").withStyle(ChatFormatting.GREEN));
            player.level().playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 0.9f, 1.0f);
        }
    }

    /** A Vuurpeper bush: the Vuurpeper for whoever walks through it and has none. */
    public static class Peperstruik extends Block implements GuhrioStuk {
        public static final MapCodec<Peperstruik> CODEC = simpleCodec(Peperstruik::new);
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

        public Peperstruik(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        public void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            if (sessie.kracht == GuhrioSpel.Kracht.VUUR) {
                return;
            }
            GuhrioSpel.zetKracht(player, sessie, GuhrioSpel.Kracht.VUUR);
            ServerLevel level = player.level();
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.9f);
            level.sendParticles(player, ParticleTypes.FLAME, false, false, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
            if (sessie.guhshi) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.guhriow3.peper_guhshi").withStyle(ChatFormatting.YELLOW));
            }
        }
    }
}
