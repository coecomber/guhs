package nl.juiced.guhs.registry;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * Sound events. Which .ogg files they play is defined in assets/guhs/sounds.json -
 * to replace a sound, just overwrite the .ogg file in assets/guhs/sounds/ (keep it mono and short).
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GUH_AMBIENT = register("entity.guh.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUH_HURT = register("entity.guh.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUH_DEATH = register("entity.guh.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUH_EAT = register("entity.guh.eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUH_HAPPY = register("entity.guh.happy");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIKA_AMBIENT = register("entity.mika.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIKA_HURT = register("entity.mika.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIKA_DEATH = register("entity.mika.death");

    /** Guh crystals singing (a high guh). */
    public static final DeferredHolder<SoundEvent, SoundEvent> KRISTAL_SING = register("block.guh_kristal.sing");
    /** Music discs (their jukebox_song is data: data/guhs/jukebox_song/). */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DISC_ZE_HANGEN = register("music_disc.ze_hangen");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    private ModSounds() {
    }
}
