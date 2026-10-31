package nl.juiced.guhs.feature.frituur;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.techsaus.Frituur;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.8.1: the frituur line. The frying pan fries on: {@link FrituurRecepten} holds what it makes. Some of that you can always
 * fry (double and triple fried, patat from a potato); the snackbar classics are learned first ({@link SnackRecepten}, the
 * Snackbar-guh teaches them). De Vetste Hap is the summit: {@link VetsteHapItem}. Resources: tools/features/frituur.py.
 */
public final class FrituurFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredItem<Item> DUBBEL = snack("dubbel_gefrituurde_kaasknabbels", 7, 0.8f);
    public static final DeferredItem<Item> DRIEDUBBEL = snack("driedubbel_gefrituurde_kaasknabbels", 8, 0.9f);
    public static final DeferredItem<Item> KAASKROKET = snack("kaaskroket", 7, 0.8f);
    public static final DeferredItem<Item> FRIKANDEL = snack("frikandel_guhspeciaal", 8, 0.8f);
    public static final DeferredItem<Item> BITTERGUHLLEN = snack("bitterguhllen", 6, 0.8f);
    public static final DeferredItem<Item> KAASSOUFFLGUH = snack("kaassoufflguh", 6, 0.8f);
    public static final DeferredItem<Item> PATATJE_KAASSAUS = snack("patatje_kaassaus", 6, 0.7f);
    public static final DeferredItem<Item> PATATJE_VADS = snack("patatje_vads", 7, 0.7f);
    public static final DeferredItem<Item> PATATJE_OORLOG = snack("patatje_oorlog", 7, 0.7f);
    public static final DeferredItem<Item> PATATJE_GUHSPECIAAL = snack("patatje_guhspeciaal", 8, 0.7f);
    public static final DeferredItem<Item> VETSTE_HAP = ITEMS.registerItem("vetste_hap", VetsteHapItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).food(
                    new FoodProperties.Builder().nutrition(20).saturationModifier(1.0f).alwaysEdible().build(),
                    Consumables.defaultFood().consumeSeconds(3.2f).build()));

    /** A food with its lore line (item.guhs.&lt;naam&gt;.lore). */
    private static DeferredItem<Item> snack(String naam, int voeding, float verzadiging) {
        return ITEMS.registerItem(naam, p -> new Item(p) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                        TooltipFlag flag) {
                tooltip.accept(Component.translatable("item.guhs." + naam + ".lore").withStyle(ChatFormatting.GRAY));
            }
        }, () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(voeding).saturationModifier(verzadiging).build()));
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        // the Frituurautomaat only fries what needs no recipe (a machine has no player who knows one)
        Frituur.zet(ModItems.GEFRITUURDE_KAASKNABBELS, DUBBEL);
        Frituur.zet(DUBBEL, DRIEDUBBEL);
        Frituur.zet(() -> Items.POTATO, PATATJE_KAASSAUS);
        SnackRecepten.register();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> FrituurCommando.register(event));
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<Item> item : java.util.List.of(DUBBEL, DRIEDUBBEL, PATATJE_KAASSAUS, PATATJE_VADS, PATATJE_OORLOG, PATATJE_GUHSPECIAAL,
                KAASKROKET, FRIKANDEL, BITTERGUHLLEN, KAASSOUFFLGUH, VETSTE_HAP)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private FrituurFeature() {
    }
}
