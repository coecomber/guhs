package nl.juiced.guhs.feature.speelgoed;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.registry.ModSounds;

/**
 * What the wip and the schommel share: a {@link ToestelBlockEntity} that swings, guhs on the seats keep it going,
 * right-click = give it a push ("Duw!"), sneak + right-click = sit on it yourself.
 */
public abstract class SchommelToestel extends ToestelBlock implements ToestelBlockEntity.Schommelend {
    private static final String DUW_TOT = "guhs_speelgoed_duw_tot";

    protected SchommelToestel(Properties properties) {
        super(properties);
    }

    /** How much a push adds (radians). */
    protected abstract float duwKracht();

    /** How much the guhs on it swing it by themselves (radians), by the number of riders. */
    protected abstract float basis(int rijders);

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ToestelBlockEntity(pos, state);
    }

    public static float hoek(net.minecraft.world.level.Level level, BlockPos pos, float tijd) {
        return level.getBlockEntity(pos) instanceof ToestelBlockEntity be ? be.hoek(tijd) : 0;
    }

    @Override
    protected void rijdt(ServerLevel level, BlockPos pos, BlockState state, ZitjeEntity zitje, LivingEntity rijder) {
        if (!(level.getBlockEntity(pos) instanceof ToestelBlockEntity be)) {
            return;
        }
        if (rijder instanceof net.minecraft.world.entity.player.Player) {
            be.minstens(basis(bezet(level, pos)) * 0.6f);   // (a player swings a little by themselves; a push goes higher)
        }
        if (rijder instanceof GuhEntity g) {
            be.minstens(basis(bezet(level, pos)));
            if (level.getRandom().nextInt(90) == 0) {
                level.playSound(null, rijder.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.8f, g.getVoicePitch() * 1.1f);
            }
            if (level.getRandom().nextInt(25) == 0) {
                Vec3 p = rijder.position();
                level.sendParticles(nl.juiced.guhs.feature.band.BandFeature.HARTJE.get(), p.x, p.y + rijder.getBbHeight() + 0.2, p.z, 1, 0.2, 0.1, 0.2, 0);
            }
            if (level.getRandom().nextInt(160) == 0 && be.amplitude(level.getGameTime()) > maxHoek() * 0.4f) {
                level.playSound(null, rijder.blockPosition(), SpeelgoedFeature.WIEEE.get(), SoundSource.NEUTRAL, 0.8f, g.getVoicePitch());
            }
        }
    }

    @Override
    protected InteractionResult gebruik(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (player.isShiftKeyDown()) {
            int plek = vrijePlek(level, pos);
            if (plek < 0) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.speelgoed.vol").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (ZitjeEntity.zet(level, pos, plek, player, 0, 1) != null) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.speelgoed.zit").withStyle(ChatFormatting.LIGHT_PURPLE));
                Spelen.spelerSpeelt(player, speeltje());
            }
            return InteractionResult.CONSUME;
        }
        duw(level, pos, player);
        return InteractionResult.CONSUME;
    }

    /** A push by a player: more swing, and the guhs on it love it (hearts from their owner). */
    public boolean duw(ServerLevel level, BlockPos pos, ServerPlayer player) {
        long nu = level.getGameTime();
        if (player.getPersistentData().getLongOr(DUW_TOT, 0L) > nu || !(level.getBlockEntity(pos) instanceof ToestelBlockEntity be)) {
            return false;
        }
        player.getPersistentData().putLong(DUW_TOT, nu + 8);
        be.duw(duwKracht());
        level.playSound(null, pos, SpeelgoedFeature.DUW.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.3f);
        boolean guh = false;
        for (int i = 0; i < plekken(); i++) {
            ZitjeEntity z = zitje(level, pos, i);
            if (z != null && z.rijder() instanceof GuhEntity g) {
                z.duwer(player);
                guh = true;
                Spelen.geduwd(player, g);
            }
        }
        player.sendOverlayMessage(Component.translatable(guh ? "gui.guhs.speelgoed.duw.guh" : "gui.guhs.speelgoed.duw").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }
}
