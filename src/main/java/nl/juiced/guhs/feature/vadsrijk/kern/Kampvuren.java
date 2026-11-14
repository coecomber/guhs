package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Checkpoints (ronde 12): right-click a campfire in the dimension. An unlit one lights up (with a little show) and becomes your
 * checkpoint; a lit one makes it your checkpoint again. No teleport between campfires. 1.10.2: after fainting you wake at the
 * NEAREST campfire you ever lit/used ({@link #dichtstbij}), not just the last one you clicked.
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

    /** A campfire block counts within this many blocks of a remembered spot (the book snaps spots to its own campfire list). */
    static final int ZOEK = 3;

    /**
     * 1.10.2: the wake-up spot at the campfire nearest to {@code van} among the ones this player has lit/used (the Verkenningsboek's
     * memory, plus the checkpoint), in this dimension. Skipped: campfires in a zone that is closed for this player (the zone gate)
     * and spots where the campfire is gone. Null = none (the caller keeps the old rule: the checkpoint, else the area's respawn).
     */
    @Nullable
    public static BlockPos dichtstbij(ServerPlayer p, Vec3 van) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return null;
        }
        List<BlockPos> kandidaten = new ArrayList<>(nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.vuren(p));
        BlockPos cp = Spelers.data(p).checkpoint;
        if (cp != null) {
            kandidaten.add(cp);
        }
        kandidaten.sort(Comparator.comparingDouble(b -> b.distToCenterSqr(van)));
        for (BlockPos b : kandidaten) {
            if (nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.dicht(p,
                    nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.zoneOp(b.getX() + 0.5, b.getY(), b.getZ() + 0.5))) {
                continue;
            }
            if (b.equals(cp)) {
                return cp;   // (the checkpoint is already a free spot next to the campfire you clicked)
            }
            BlockPos vuur = zoekVuur(sl, b);
            if (vuur != null) {
                BlockPos plek = vrijePlek(sl, vuur);
                return cp != null && cp.distSqr(vuur) <= 9 ? cp : plek;
            }
        }
        return null;
    }

    /** The campfire block at or within {@link #ZOEK} blocks of this remembered spot (the closest), or null. */
    @Nullable
    static BlockPos zoekVuur(ServerLevel sl, BlockPos b) {
        if (sl.getBlockState(b).getBlock() instanceof CampfireBlock) {
            return b;
        }
        BlockPos beste = null;
        double d = Double.MAX_VALUE;
        for (BlockPos x : BlockPos.betweenClosed(b.offset(-ZOEK, -ZOEK, -ZOEK), b.offset(ZOEK, ZOEK, ZOEK))) {
            if (sl.getBlockState(x).getBlock() instanceof CampfireBlock && x.distSqr(b) < d) {
                d = x.distSqr(b);
                beste = x.immutable();
            }
        }
        return beste;
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
