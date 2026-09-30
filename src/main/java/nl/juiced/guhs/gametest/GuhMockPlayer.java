package nl.juiced.guhs.gametest;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;

/**
 * 1.1.0 (MC 26.1): the gametests' mock player. Vanilla's {@code GameTestHelper#makeMockServerPlayerInLevel} player
 * always reports CREATIVE (as in 1.21.1), even after a test calls {@code setGameMode(SURVIVAL)}; 26.1's
 * {@code Mob#setTarget} ignores creative players, so tests with a "survival" mock that must be chased failed. This mock
 * is the same player, but once a test sets a game mode it reports that mode. It also starts on the test's origin (see
 * {@link #of}).
 */
public final class GuhMockPlayer extends ServerPlayer {
    private boolean modeSet;

    private GuhMockPlayer(GameTestHelper helper, CommonListenerCookie cookie) {
        super(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
    }

    /** Replaces {@code nl.juiced.guhs.gametest.GuhMockPlayer.of(helper)}. */
    public static ServerPlayer of(GameTestHelper helper) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        GuhMockPlayer player = new GuhMockPlayer(helper, cookie);
        net.minecraft.network.Connection connection = new net.minecraft.network.Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        // 1.21.1 players started at the (always loaded) world spawn next to the tests; a 26.1 player starts at 0,0,0 in
        // unloaded chunks far from the (randomly placed) tests, so the mock starts on the test's origin instead.
        net.minecraft.core.BlockPos at = helper.absolutePos(net.minecraft.core.BlockPos.ZERO);
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    @Override
    public GameType gameMode() {
        return this.modeSet ? super.gameMode() : GameType.CREATIVE;
    }

    @Override
    public boolean setGameMode(GameType mode) {
        boolean changed = super.setGameMode(mode);
        this.modeSet = true;
        return changed;
    }
}
