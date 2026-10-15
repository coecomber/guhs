package nl.juiced.guhs.feature.einde2.reizen;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * einde2 phase B/C, slice reizen (CONTRACT_E2_B 7.10): travelling in the Guheinde.
 * <ul>
 *   <li>{@link E2Reizen}: the story flights on Appa between the chapters (team chat, Zuka's ship chase with a short dodge
 *       part), the free flights of the whistle list ({@link Fluitlijst}), and the guest rule of co-op;</li>
 *   <li>{@link E2Team}: Team Guhvatar at the {@code team_*} markers of the four story structures (per player), with two lines
 *       of small talk per member per structure ({@link TeamRol});</li>
 *   <li>{@link Leegtewalvis}: the old captain's questline in the harbour pub (believe, wait in the void, the whale rises,
 *       bring proof, tame one of your own) and {@link Walvis}: your own whale (the horn, slow steering, the furnished
 *       back with chests that really keep items).</li>
 * </ul>
 * Resources: tools/features/einde2_reizen.py (+ einde2_reizen_modellen.py); the whale's furniture lives in the whale's own
 * model (einde2_zwevers_modellen.py).
 */
public final class E2ReizenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /** Zuka's ship in the chase between the chapters (only the player it hunts and their guests see it). */
    public static final DeferredHolder<EntityType<?>, EntityType<ZukaSchipEntity>> ZUKA_SCHIP = ENTITY_TYPES.register("einde2_reizen_zuka_schip",
            () -> EntityType.Builder.<ZukaSchipEntity>of(ZukaSchipEntity::new, MobCategory.MISC).sized(3.0f, 3.0f).noSummon().fireImmune()
                    .clientTrackingRange(16).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("einde2_reizen_zuka_schip"))));

    /** The old captain's horn: calls your own Leegtewalvisguh (sneak: furnish his back). */
    public static final DeferredItem<WalvishoornItem> WALVISHOORN = ITEMS.registerItem("einde2_reizen_walvishoorn", WalvishoornItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    // --- sounds (vanilla sounds, pitched, in sounds.json) ---------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> HOORN = sound("einde2_reizen.hoorn");
    public static final DeferredHolder<SoundEvent, SoundEvent> VUURBAL = sound("einde2_reizen.vuurbal");
    public static final DeferredHolder<SoundEvent, SoundEvent> VUURBAL_PLOF = sound("einde2_reizen.vuurbal_plof");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHIP = sound("einde2_reizen.schip");
    public static final DeferredHolder<SoundEvent, SoundEvent> PROOST = sound("einde2_reizen.proost");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        E2Reizen.register();
        E2Team.register();
        Leegtewalvis.register();
        Walvis.register();
        NeoForge.EVENT_BUS.register(E2ReizenEvents.class);
    }

    public static void payloads(PayloadRegistrar registrar) {
        E2ReizenPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(WALVISHOORN.get()));
    }

    private E2ReizenFeature() {
    }
}
