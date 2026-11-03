package nl.juiced.guhs.feature.vadsrijk.bazen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * Boss music hooks (design ronde 6/9: music only at bosses and cutscenes). No audio exists yet: the sound events are registered
 * ({@code guhs:vr.bazen.muziek.<baas>}) but nothing plays until {@link #AAN} is true and a sounds.json entry + .ogg exist.
 * Integration: add the files, flip AAN.
 */
public final class BazenMuziek {
    public static boolean AAN = false;
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> KONING_KLODDER = muziek("koning_klodder");
    public static final DeferredHolder<SoundEvent, SoundEvent> OPPERZWAM = muziek("opperzwam");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIKA_SPION = muziek("mika_spion");
    private static final Map<UUID, String> SPEELT = new HashMap<>();

    private BazenMuziek() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> muziek(String baas) {
        String id = "vr.bazen.muziek." + baas;
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Guhs.id(id)));
    }

    public static DeferredHolder<SoundEvent, SoundEvent> van(String baas) {
        return switch (baas) {
            case "opperzwam" -> OPPERZWAM;
            case "mika_spion" -> MIKA_SPION;
            default -> KONING_KLODDER;
        };
    }

    /** The fight starts for this player (idempotent). */
    public static void start(ServerPlayer p, String baas) {
        if (!AAN || baas.equals(SPEELT.get(p.getUUID()))) {
            return;
        }
        SPEELT.put(p.getUUID(), baas);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(van(baas), SoundSource.RECORDS, p.getX(), p.getY(), p.getZ(),
                1f, 1f, p.getRandom().nextLong()));
    }

    public static void stop(ServerPlayer p) {
        String b = SPEELT.remove(p.getUUID());
        if (AAN && b != null) {
            p.connection.send(new ClientboundStopSoundPacket(Guhs.id("vr.bazen.muziek." + b), SoundSource.RECORDS));
        }
    }
}
