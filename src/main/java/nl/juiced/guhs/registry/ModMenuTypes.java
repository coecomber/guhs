package nl.juiced.guhs.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.menu.BankGuhMenu;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Guhs.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<BankGuhMenu>> BANK_GUH = MENUS.register("bank_guh",
            () -> IMenuTypeExtension.create(BankGuhMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<nl.juiced.guhs.menu.GuhWardrobeMenu>> GUH_WARDROBE = MENUS.register("guh_wardrobe",
            () -> IMenuTypeExtension.create(nl.juiced.guhs.menu.GuhWardrobeMenu::new));

    private ModMenuTypes() {
    }
}
