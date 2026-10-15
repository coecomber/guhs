package nl.juiced.guhs.feature.einde2.audio;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.network.ModNetworking;
import org.slf4j.Logger;

/**
 * einde2 phase B (CONTRACT_E2_B 3, 6), owner: slice audio. The music and the sounds of the Guhvatar story: every name of
 * 6.1 as {@code guhs:einde2_audio.<naam>} ({@link E2AudioFeature#SOUNDS}), the epic moments under a cutscene, the stingers.
 * <p>
 * A moment is played by the player's client as music (the music slider), fades in, loops while the player waits at a click
 * of the cutscene and fades out on {@link #stopMoment}; the background music of the area is silent meanwhile. A moment
 * nobody stops ends by itself after {@link #MOMENT_MAX_TICKS}. {@link #BPM} is fixed: the rhythm games beat on it from tick
 * 0 of the sound (the generator puts the first beat on the first sample).
 */
public final class E2Audio {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** The beats per minute of the tunes the rhythm games follow (names without the {@code einde2_audio.} prefix). */
    public static final Map<String, Integer> BPM = Map.of("deuntje.dansfeest", 100, "deuntje.waaierdans", 90, "deuntje.ritmeduel", 80);
    /** The default fade of a moment, in and out (ticks). */
    public static final int FADE = 40;
    /** A moment that nobody stops fades out after this long (6 minutes; the client keeps the same limit). */
    public static final int MOMENT_MAX_TICKS = 6 * 60 * 20;
    /** Stingers at a spot: heard by the players within this many blocks. */
    public static final double STINGER_BEREIK = 32;

    /** Server: the moment each player hears now (full name, {@code moment.golf}). */
    private static final Map<UUID, String> MOMENT = new ConcurrentHashMap<>();
    private static final Set<String> GEMELD = ConcurrentHashMap.newKeySet();

    private E2Audio() {
    }

    /** The sound of a name of 6.1 (without the {@code einde2_audio.} prefix); an unknown name: a vanilla chime (logged once). */
    public static Supplier<SoundEvent> geluid(String naam) {
        Holder<SoundEvent> h = holder(naam);
        return h == null ? () -> SoundEvents.NOTE_BLOCK_CHIME.value() : h::value;
    }

    /** The registered holder of a name of 6.1 (null: no such name). */
    @Nullable
    public static Holder<SoundEvent> holder(String naam) {
        Holder<SoundEvent> h = E2AudioFeature.SOUNDS_PER_NAAM.get(naam);
        if (h == null && GEMELD.add(naam)) {
            LOGGER.warn("Guhs einde2 audio: no sound named {} (CONTRACT_E2_B 6.1)", naam);
        }
        return h;
    }

    /** An epic moment for this player (the part after {@code moment.}): replaces the background music, fades in, loops. */
    public static void moment(ServerPlayer p, String id) {
        String naam = id.startsWith("moment.") ? id : "moment." + id;
        if (holder(naam) == null) {
            return;
        }
        MOMENT.put(p.getUUID(), naam);
        ModNetworking.sendTo(p, new E2AudioPayloads.Moment(naam, FADE));
    }

    /** (Dev command) Any long piece of 6.1 ({@code muziek.*} or {@code moment.*}) as a moment. */
    static void speelLang(ServerPlayer p, String naam) {
        MOMENT.put(p.getUUID(), naam);
        ModNetworking.sendTo(p, new E2AudioPayloads.Moment(naam, FADE));
    }

    /** Ends this player's moment, fading out over this many ticks. */
    public static void stopMoment(ServerPlayer p, int fadeTicks) {
        MOMENT.remove(p.getUUID());
        ModNetworking.sendTo(p, new E2AudioPayloads.Moment("", Math.max(0, fadeTicks)));
    }

    /** The moment this player hears now (full name), or null. */
    @Nullable
    public static String moment(ServerPlayer p) {
        return MOMENT.get(p.getUUID());
    }

    /** A stinger (a name of 6.1) for this player only. */
    public static void stinger(ServerPlayer p, String naam) {
        Holder<SoundEvent> h = holder(naam);
        if (h == null) {
            return;
        }
        p.connection.send(new ClientboundSoundPacket(h, AudioNamen.categorie(naam), p.getX(), p.getY(), p.getZ(), 1.0F, 1.0F, p.getRandom().nextLong()));
    }

    /** A stinger at a spot: every player within 32 blocks hears it. */
    public static void stinger(ServerLevel level, Vec3 plek, String naam) {
        Holder<SoundEvent> h = holder(naam);
        if (h == null) {
            return;
        }
        // a variable-range event carries volume * 16 blocks: 2.0 = the 32 blocks of the contract
        level.playSound(null, plek.x, plek.y, plek.z, h, AudioNamen.categorie(naam), (float) (STINGER_BEREIK / 16.0), 1.0F);
    }

    /** (Logout) forget the player's moment. */
    static void vergeet(UUID speler) {
        MOMENT.remove(speler);
    }
}
