package nl.juiced.guhs.feature.vadsrijk.kern;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Checkpoints (ronde 12): right-click a campfire in the dimension. An unlit one lights up (with a little show) and becomes your
 * checkpoint; a lit one makes it your checkpoint again. You wake there after fainting. No teleport between campfires.
 */
public final class Kampvuren {
    private Kampvuren() {
    }

    static void opKlik(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !Vadsrijk.in(p) || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        BlockPos pos = e.getPos();
        BlockState s = sl.getBlockState(pos);
        if (!(s.getBlock() instanceof CampfireBlock) || !s.hasProperty(CampfireBlock.LIT)) {
            return;
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        steekAan(p, pos);
    }

    public static void steekAan(ServerPlayer p, BlockPos pos) {
        ServerLevel sl = (ServerLevel) p.level();
        BlockState s = sl.getBlockState(pos);
        boolean nieuw = s.hasProperty(CampfireBlock.LIT) && !s.getValue(CampfireBlock.LIT);
        if (nieuw) {
            sl.setBlock(pos, s.setValue(CampfireBlock.LIT, true), 3);
            sl.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f, 1.2f);
        }
        SpelerData d = Spelers.data(p);
        BlockPos plek = vrijePlek(sl, pos);
        boolean anders = !plek.equals(d.checkpoint);
        d.checkpoint = plek;
        d.vads = Spelers.maxVads(p);
        d.energie = Spelers.maxEnergie(p);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 25, 0.8, 0.5, 0.8, 0.02);
        sl.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 3, 0.4, 0.2, 0.4, 0);
        if (anders || nieuw) {
            sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.4f);
            p.sendOverlayMessage(Component.translatable("vadsrijk.kern.kampvuur.checkpoint").withStyle(ChatFormatting.GOLD));
            Tips.toon(p, "vadsrijk.kern.tip.kampvuur");
            Haken.KAMPVUUR.forEach(h -> h.accept(p, pos));
        } else {
            p.sendOverlayMessage(Component.translatable("vadsrijk.kern.kampvuur.rust").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        Spelers.sync(p);
    }

    /** A free standing spot next to the campfire (south first). */
    static BlockPos vrijePlek(ServerLevel sl, BlockPos vuur) {
        for (BlockPos b : new BlockPos[] {vuur.south(2), vuur.north(2), vuur.east(2), vuur.west(2), vuur.south()}) {
            if (sl.getBlockState(b).getCollisionShape(sl, b).isEmpty() && sl.getBlockState(b.above()).getCollisionShape(sl, b.above()).isEmpty()
                    && !sl.getBlockState(b.below()).getCollisionShape(sl, b.below()).isEmpty()) {
                return b;
            }
        }
        return vuur.above();
    }
}
