package nl.juiced.guhs.feature.guhpixel.blok;

import java.util.Locale;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import nl.juiced.guhs.feature.guhpixel.Toegang;

/**
 * The Guhpixel portal block ({@code guhs:guhpixel_portaal}, unbreakable, walk-through): {@code soort=in} in the giant CRT
 * monitor of the Guh-internetcafé (walking through it the first time unlocks Guhpixel for that player), {@code soort=uit}
 * as the exit portal on the lobby plaza ("Terug naar huis"). Only players use it: guhs, pets and items never go through.
 */
public class PortaalBlock extends Block implements Portal {
    public static final MapCodec<PortaalBlock> CODEC = simpleCodec(PortaalBlock::new);

    public enum Soort implements StringRepresentable {
        IN, UIT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Soort> SOORT = EnumProperty.create("soort", Soort.class);
    private static final DustParticleOptions ROZE = new DustParticleOptions(0xFF7AC8, 1.0f), GROEN = new DustParticleOptions(0x7AFFB0, 1.0f);

    public PortaalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SOORT, Soort.IN));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOORT);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity instanceof ServerPlayer && entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return 10;
    }

    @Nullable
    @Override
    public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        if (!(entity instanceof ServerPlayer p)) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(SOORT)) {
            return null;
        }
        if (state.getValue(SOORT) == Soort.IN) {
            Toegang.ontgrendel(p);
            return Toegang.lobbyTransitie(p, true);
        }
        return Toegang.huisTransitie(p);
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.NONE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        DustParticleOptions kleur = state.getValue(SOORT) == Soort.IN ? ROZE : GROEN;
        for (int i = 0; i < 2; i++) {
            level.addParticle(kleur, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0.03, 0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }
}
