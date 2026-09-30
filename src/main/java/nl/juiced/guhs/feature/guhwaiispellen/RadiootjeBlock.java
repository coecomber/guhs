package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.Guhs;

/**
 * Het Tiki-radiootje: a little bamboo radio with a shell for a loudspeaker. Right-click it and it plays the hula songs of
 * Guhwai'i one after the other (Aloha, Njeg - Guhla-Hula Rock - Vahoeg Hula Hop - off), for everybody close by.
 */
public class RadiootjeBlock extends TikiBlock {
    /** Which song each radio plays (-1: off). Only in memory: after a restart they are all off. */
    private static final Map<GlobalPos, Integer> SPEELT = new ConcurrentHashMap<>();
    public static final double HOREN = 24;

    public RadiootjeBlock(Properties properties) {
        super(properties, new double[][]{{3, 0, 5, 13, 9, 12}, {11, 9, 8, 12, 14, 9}}, false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            GlobalPos key = GlobalPos.of(server.dimension(), pos.immutable());
            int nu = SPEELT.getOrDefault(key, -1);
            stop(server, pos, nu);
            int volgende = nu + 1 >= HulaLiedje.values().length ? -1 : nu + 1;
            if (volgende < 0) {
                SPEELT.remove(key);
                player.sendOverlayMessage(Component.translatable("block.guhs.tiki_radiootje.uit").withStyle(ChatFormatting.GRAY));
            } else {
                SPEELT.put(key, volgende);
                HulaLiedje l = HulaLiedje.of(volgende);
                for (ServerPlayer p : server.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(HOREN))) {
                    if (p.connection != null) {
                        p.connection.send(new ClientboundSoundPacket(GuhwaiiSpellenBlocks.lied(l), SoundSource.RECORDS, pos.getX() + 0.5,
                                pos.getY() + 0.5, pos.getZ() + 0.5, 1.4f, 1.0f, server.getRandom().nextLong()));
                    }
                }
                player.sendOverlayMessage(Component.translatable("block.guhs.tiki_radiootje.speelt", l.naam()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void stop(ServerLevel server, BlockPos pos, int liedje) {
        if (liedje < 0) {
            return;
        }
        HulaLiedje l = HulaLiedje.of(liedje);
        for (ServerPlayer p : server.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(HOREN + 16))) {
            if (p.connection != null) {
                p.connection.send(new ClientboundStopSoundPacket(Guhs.id(l.geluid()), SoundSource.RECORDS));
            }
        }
    }

    /** 1.1.0 (onRemove is gone): the radio is gone, its song stops (block changes with neighbour updates, i.e. breaking). */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel server, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, server, pos, movedByPiston);
        Integer nu = SPEELT.remove(GlobalPos.of(server.dimension(), pos.immutable()));
        if (nu != null) {
            stop(server, pos, nu);
        }
    }

    /** Little notes float up from a playing radio (the client can't know it plays: a note now and then always). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, random.nextInt(24) / 24.0, 0, 0);
        }
    }

    static void vergeet() {
        SPEELT.clear();
    }
}
