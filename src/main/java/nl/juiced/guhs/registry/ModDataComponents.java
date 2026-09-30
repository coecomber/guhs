package nl.juiced.guhs.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.storage.BankContents;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Guhs.MODID);

    /** Everything inside a Bank Guh; kept on the item when the block is broken (like a shulker box). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BankContents>> BANK_CONTENTS =
            COMPONENTS.registerComponentType("bank_contents",
                    b -> b.persistent(BankContents.CODEC).networkSynchronized(BankContents.STREAM_CODEC).cacheEncoding());

    /** The guh inside a picked-up guh item (its full entity data). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> GUH_DATA =
            COMPONENTS.registerComponentType("guh_data",
                    b -> b.persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC));

    private ModDataComponents() {
    }
}
