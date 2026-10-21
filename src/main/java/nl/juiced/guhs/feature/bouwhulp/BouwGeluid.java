package nl.juiced.guhs.feature.bouwhulp;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * The Bouwhulp's own sounds (F12; the OGGs and sounds.json: tools/features/bouwhulp_geluid.py): a ghost appearing, blocks
 * plonking in, the keet's crane, Pof!, the done-fanfare, the vormstaf/kwast swish, the Ontwerpbureau's paper, the helpers'
 * gnawing and digging, and the bouwguhs' whistling.
 */
public final class BouwGeluid {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SPOOK = sound("bouwhulp.spook");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLONK = sound("bouwhulp.plonk");
    public static final DeferredHolder<SoundEvent, SoundEvent> KRAAN = sound("bouwhulp.kraan");
    public static final DeferredHolder<SoundEvent, SoundEvent> POF = sound("bouwhulp.pof");
    public static final DeferredHolder<SoundEvent, SoundEvent> FEEST = sound("bouwhulp.feest");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZWIEP = sound("bouwhulp.zwiep");
    public static final DeferredHolder<SoundEvent, SoundEvent> PAPIER = sound("bouwhulp.papier");
    public static final DeferredHolder<SoundEvent, SoundEvent> KNAAG = sound("bouwhulp.knaag");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAAF = sound("bouwhulp.graaf");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLUIT = sound("bouwhulp.fluit");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }

    private BouwGeluid() {
    }
}
