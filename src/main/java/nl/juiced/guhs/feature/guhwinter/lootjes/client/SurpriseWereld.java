package nl.juiced.guhs.feature.guhwinter.lootjes.client;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import nl.juiced.guhs.feature.guhwinter.lootjes.Inhoud;

/**
 * A little world for tesselating a gift like the chunk renderer does: the gift's blocks at their world positions (so the
 * faces between two blocks of the gift are left out and the corners get ambient occlusion), air everywhere else; the light
 * and the biome tints come from the real level around it.
 */
final class SurpriseWereld implements BlockAndTintGetter {
    private static final BlockState LUCHT = Blocks.AIR.defaultBlockState();
    private final ClientLevel level;
    private final BlockPos oorsprong;
    private final Inhoud inhoud;

    SurpriseWereld(ClientLevel level, BlockPos oorsprong, Inhoud inhoud) {
        this.level = level;
        this.oorsprong = oorsprong;
        this.inhoud = inhoud;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        int x = pos.getX() - oorsprong.getX(), y = pos.getY() - oorsprong.getY(), z = pos.getZ() - oorsprong.getZ();
        if (x < 0 || y < 0 || z < 0 || x >= Inhoud.N || y >= Inhoud.N || z >= Inhoud.N) {
            return LUCHT;
        }
        return inhoud.get(x, y, z);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinY() {
        return level.getMinY();
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return level.cardinalLighting();
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver color) {
        return level.getBlockTint(pos, color);
    }
}
