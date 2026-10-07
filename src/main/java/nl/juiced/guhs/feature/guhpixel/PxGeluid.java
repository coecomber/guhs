package nl.juiced.guhs.feature.guhpixel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Small helpers for what one player hears and sees: a sound only for them, a title in the middle of the screen. */
public final class PxGeluid {
    /** A sound only this player hears (mock players in tests have no real connection: nothing happens). */
    public static void speel(ServerPlayer p, SoundEvent sound, SoundSource bron, float volume, float pitch) {
        if (p.connection != null) {
            p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), bron, p.getX(), p.getY(), p.getZ(),
                    volume, pitch, p.getRandom().nextLong()));
        }
    }

    /** A title with a subtitle that stays this many ticks. */
    public static void titel(ServerPlayer p, Component titel, Component onder, int ticks) {
        if (p.connection != null) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(6, ticks, 14));
            p.connection.send(new ClientboundSetSubtitleTextPacket(onder));
            p.connection.send(new ClientboundSetTitleTextPacket(titel));
        }
    }

    private PxGeluid() {
    }
}
