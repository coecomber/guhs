package nl.juiced.guhs.feature.vissen;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.NpcRole;

/**
 * The Guhvis-wedstrijd (fishing contest) at the guhvis pond (guhvis_vijver): the Visguh lends you a rod, you fish for
 * three minutes in her pond and every special fish (kaasvis, vadsbaars, guhpuffer, njegforel, Mika-meerval, Gouden
 * Guhvis) gives points by its weight. Points become visbonnen, visbonnen buy the angler outfit for your guh.
 * See {@link VisWedstrijd} for the game, {@link VisguhRole} for the Visguh herself.
 */
public final class VissenFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** The currency: earned in contests, spent at the Visguh's stall. */
    public static final DeferredItem<Item> VISBON = ITEMS.registerSimpleItem("visbon", () -> new Item.Properties());
    /** The loaned contest rod (never kept). */
    public static final DeferredItem<GuhvisHengel> GUHVIS_HENGEL = ITEMS.registerItem("guhvis_hengel", GuhvisHengel::new,
            () -> new Item.Properties().stacksTo(1));
    /** The special fish, one item per species. */
    public static final Map<VisSoort, DeferredItem<VisItem>> VISSEN = new EnumMap<>(VisSoort.class);

    static {
        for (VisSoort soort : VisSoort.values()) {
            VISSEN.put(soort, ITEMS.registerItem(soort.id(), props -> new VisItem(soort, props),
                    () -> new Item.Properties().stacksTo(soort == VisSoort.GOUDEN_GUHVIS ? 16 : 64)));
        }
    }

    /** The angler outfit (only sold by the Visguh). */
    public static final GuhClothes[] OUTFIT = {GuhClothes.VISSERSHOEDJE, GuhClothes.VISVEST, GuhClothes.VIS_AAN_DE_HAAK};

    private static final VisguhRole ROLE = new VisguhRole();

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(VisWedstrijd::onFished);
        NeoForge.EVENT_BUS.addListener(VisWedstrijd::onDamage);
        NeoForge.EVENT_BUS.addListener(VisWedstrijd::onToss);
        NeoForge.EVENT_BUS.addListener(VisWedstrijd::onLogout);
        NeoForge.EVENT_BUS.addListener(VisWedstrijd::onContainerClose);
        NeoForge.EVENT_BUS.addListener(VisWedstrijd::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(VisWedstrijd::onDeath);
        NeoForge.EVENT_BUS.addListener(VisWedstrijd::onDrops);
        NeoForge.EVENT_BUS.addListener(VisWedstrijd::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(VisWedstrijd::onInteractEntity);
        NeoForge.EVENT_BUS.addListener(VisWedstrijd::onInteractEntityAt);
        NeoForge.EVENT_BUS.addListener(VisWedstrijd::onUseBlock);
        NeoForge.EVENT_BUS.register(VissenProtection.class);
        nl.juiced.guhs.feature.Protected.add(VissenProtection::protectedAt);
    }

    public static void payloads(PayloadRegistrar registrar) {
        VissenPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(VISBON.get()));
        for (VisSoort soort : VisSoort.values()) {
            output.accept(new ItemStack(VISSEN.get(soort).get()));
        }
        // (the loaned rod stays out of the tab; the outfit is in it already, with all the other guh clothes)
    }

    public static Item vis(VisSoort soort) {
        return VISSEN.get(soort).get();
    }

    @Nullable
    public static NpcRole role() {
        return ROLE;
    }

    private VissenFeature() {
    }
}
