package nl.juiced.guhs.feature.vadsrijk.stadsmuziek;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;

/**
 * 1.10 fix round 2 (I-AUDIO): town music and town ambience in Het Vadsrijk. The user asked for sweet little tunes in the
 * towns (this overrides design ronde 9's "music only at bosses and scenes"; the wilderness stays ambient only):
 * <ul>
 *   <li>Zwamzoet, Knuffelkamp and De Haven (zone 1) and Kandijkom (zone 2, with its dock) each have two tunes
 *       ({@code guhs:vr.stad.muziek.<stad>_<n>}, synthesized by tools/features/vadsrijk_stadsmuziek_geluid.py);</li>
 *   <li>an ambience bed per town ({@code vr.stad.bed.*}, quiet, no wind: lapping water, birdsong + chimes, a warm hearth + sleigh bells) and now-and-then
 *       sounds ({@code vr.stad.sfeer.<stad>} / {@code vr.stad.sfeer.nacht}: birds, guh giggles, the market chatter, wind
 *       chimes, little bells, sleigh bells, crackling fire baskets), made of existing Guhs and vanilla sounds in sounds.json.</li>
 * </ul>
 * This class is common (registration + the area to town mapping, gametested); the player is {@code client.StadsMuziekSpeler}.
 */
public final class StadsMuziek {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    /** The towns, in a fixed order. */
    public static final List<String> STEDEN = List.of("zwamzoet", "knuffelkamp", "haven", "kandijkom");
    /** Tunes per town (at least two each, played one after the other). */
    public static final Map<String, List<DeferredHolder<SoundEvent, SoundEvent>>> MUZIEK = new LinkedHashMap<>();
    /** The ambience bed of each town (a seamless 20 s loop). */
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> BED = new LinkedHashMap<>();
    /** Now-and-then sounds by day per town. */
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> SFEER = new LinkedHashMap<>();
    /** Now-and-then sounds at night (all towns: an owl, wind chimes, a sleepy giggle). */
    public static final DeferredHolder<SoundEvent, SoundEvent> NACHT = geluid("vr.stad.sfeer.nacht");

    static {
        DeferredHolder<SoundEvent, SoundEvent> briesje = geluid("vr.stad.bed.briesje");
        DeferredHolder<SoundEvent, SoundEvent> golfjes = geluid("vr.stad.bed.golfjes");
        DeferredHolder<SoundEvent, SoundEvent> haardvuur = geluid("vr.stad.bed.haardvuur");
        for (String stad : STEDEN) {
            MUZIEK.put(stad, List.of(geluid("vr.stad.muziek." + stad + "_1"), geluid("vr.stad.muziek." + stad + "_2")));
            SFEER.put(stad, geluid("vr.stad.sfeer." + stad));
            BED.put(stad, switch (stad) {
                case "haven" -> golfjes;
                case "kandijkom" -> haardvuur;
                default -> briesje;
            });
        }
    }

    private StadsMuziek() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Guhs.id(id)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }

    /** The town of a kern area id ("zwamzoet", "haven", "knuffelkamp", "z2_kandijkom", "z2_kandijkom_dok"), or null (wilderness). */
    @Nullable
    public static String stadVan(@Nullable String gebied) {
        if (gebied == null) {
            return null;
        }
        return switch (gebied) {
            case "zwamzoet", "knuffelkamp", "haven" -> gebied;
            case "z2_kandijkom", "z2_kandijkom_dok" -> "kandijkom";
            default -> null;
        };
    }

    /** The town at block x/z (via the kern areas), or null. */
    @Nullable
    public static String stadOp(double x, double z) {
        Gebieden.Gebied g = Gebieden.op(x, z);
        return stadVan(g == null ? null : g.id());
    }

    /** z3-systemen: the town at x/y/z (zone-3 towns are layers: Honingtak sits on a branch above the ground). */
    @Nullable
    public static String stadOp(double x, double y, double z) {
        Gebieden.Gebied g = Gebieden.op(x, y, z);
        return stadVan(g == null ? null : g.id());
    }

    /** The tunes of a town (empty outside the towns). */
    public static List<DeferredHolder<SoundEvent, SoundEvent>> muziekVan(@Nullable String stad) {
        return stad == null ? List.of() : MUZIEK.getOrDefault(stad, List.of());
    }
}
