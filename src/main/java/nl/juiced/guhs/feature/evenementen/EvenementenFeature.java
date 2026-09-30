package nl.juiced.guhs.feature.evenementen;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.NpcRole;

/**
 * Guh events in the Guhmension (evenementen): now and then (about every 2-3 Minecraft days of your time there) something
 * happens around you outdoors, with a chat announcement and a boss bar with the time left: a {@link Kaasregen}, the
 * {@link Vadsparade} (with the parade outfit, only from the parade) or at night a {@link Sterrenregen} (starry guhs to
 * tame). See {@link Evenementen} for the scheduler, the cleaning up and the op command /guhs evenement.
 */
public final class EvenementenFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** Only from the kaasregen: tames a wild guh at once. */
    public static final DeferredItem<GoudenKaasknabbelItem> GOUDEN_KAASKNABBEL = ITEMS.registerItem("gouden_kaasknabbel", GoudenKaasknabbelItem::new,
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE)
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(1.2f).build()));
    /** Left behind by a falling star. */
    public static final DeferredItem<SterrenstofItem> STERRENSTOF = ITEMS.registerItem("sterrenstof", SterrenstofItem::new,
            new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));

    /** The guhs of the Vadsparade (never saved). */
    public static final DeferredHolder<EntityType<?>, EntityType<ParadeGuhEntity>> PARADE_GUH = ENTITY_TYPES.register("parade_guh",
            () -> EntityType.Builder.of(ParadeGuhEntity::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f)
                    .passengerAttachments(new Vec3(0, 0.6, -0.3)).clientTrackingRange(10).updateInterval(1).noSave().noSummon()
                    .build(Guhs.id("parade_guh").toString()));
    /** A knabbel of the kaasregen (never saved). */
    public static final DeferredHolder<EntityType<?>, EntityType<VallendeKnabbelEntity>> VALLENDE_KNABBEL = ENTITY_TYPES.register("vallende_knabbel",
            () -> EntityType.Builder.<VallendeKnabbelEntity>of(VallendeKnabbelEntity::new, MobCategory.MISC).sized(0.4f, 0.3f)
                    .clientTrackingRange(6).updateInterval(10).noSave().noSummon().fireImmune().build(Guhs.id("vallende_knabbel").toString()));
    /** A falling star of the sterrenregen (never saved). */
    public static final DeferredHolder<EntityType<?>, EntityType<VallendeSterEntity>> VALLENDE_STER = ENTITY_TYPES.register("vallende_ster",
            () -> EntityType.Builder.<VallendeSterEntity>of(VallendeSterEntity::new, MobCategory.MISC).sized(0.5f, 0.5f)
                    .clientTrackingRange(10).updateInterval(10).noSave().noSummon().fireImmune().build(Guhs.id("vallende_ster").toString()));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(PARADE_GUH.get(), GuhEntity.createAttributes().build()));
        NeoForge.EVENT_BUS.addListener(Evenementen::tick);
        NeoForge.EVENT_BUS.addListener(Evenementen::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Evenementen::onInteract);
        NeoForge.EVENT_BUS.addListener(Evenementen::onLogout);
        NeoForge.EVENT_BUS.addListener(Evenementen::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(Evenementen::onDeath);
        NeoForge.EVENT_BUS.addListener(Evenementen::onJoinLevel);
        NeoForge.EVENT_BUS.addListener(Evenementen::onServerStopping);
        NeoForge.EVENT_BUS.addListener(Evenementen::onServerStopped);
        NeoForge.EVENT_BUS.addListener(Evenementen::registerCommands);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GOUDEN_KAASKNABBEL.get()));
        output.accept(new ItemStack(STERRENSTOF.get()));
        // (the parade outfit is in the tab already, with all the other guh clothes)
    }

    @Nullable
    public static NpcRole role() {
        return null;
    }

    private EvenementenFeature() {
    }
}
