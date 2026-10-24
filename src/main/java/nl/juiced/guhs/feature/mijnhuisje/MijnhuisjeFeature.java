package nl.juiced.guhs.feature.mijnhuisje;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.7.0 "Onder de Wol" (ronde 37-40, design ontwerp-guhs170-mijnen.md): the Mijn-guhhuisje ({@link MijnhuisjeBlock},
 * {@link MijnhuisjeBlockEntity}: up to four guhs mine out an area under it), its menu ({@link MijnhuisjeMenu}), the walk to
 * the work ({@link MijnGoal}), the Rugzakzadel ({@link Rugzak}) and the quest "Mijnguh-hulpje" ({@link MijnHulpje}).
 * Resources: tools/features/mijnrook.py (standalone: {@code python tools/features/mijnrook.py}).
 */
public final class MijnhuisjeFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Guhs.MODID);

    /** Natural terrain a Mijn-guhhuisje may mine (wool, the layer stones, ores, dirt, sand, kaasknabbel blocks, gatenkaas...). */
    public static final TagKey<Block> MIJNBAAR = TagKey.create(Registries.BLOCK, Guhs.id("mijnhuisje_mijnbaar"));

    public static final DeferredBlock<MijnhuisjeBlock> BLOK = BLOCKS.registerBlock("mijn_guhhuisje", MijnhuisjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f).sound(SoundType.WOOL).noOcclusion()
                    .lightLevel(s -> 7).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<Item> ITEM = ITEMS.registerItem("mijn_guhhuisje",
            p -> new nl.juiced.guhs.feature.balto.BaltoBlocks.LoreBlock(BLOK.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MijnhuisjeBlockEntity>> BE = BLOCK_ENTITIES.register("mijn_guhhuisje",
            () -> new BlockEntityType<>(MijnhuisjeBlockEntity::new, BLOK.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<MijnhuisjeMenu>> MENU = MENUS.register("mijn_guhhuisje",
            () -> IMenuTypeExtension.create(MijnhuisjeMenu::client));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        GuhHooks.doelen((guh, goals) -> goals.addGoal(3, new MijnGoal(guh)));
        nl.juiced.guhs.feature.kleding.KledingBronnen.bron(GuhClothes.RUGZAKZADEL, "crafting");
        // only the owner (or an op) breaks a Mijn-guhhuisje
        NeoForge.EVENT_BUS.addListener((BreakBlockEvent event) -> {
            if (event.getState().is(BLOK.get()) && event.getLevel().getBlockEntity(event.getPos()) instanceof MijnhuisjeBlockEntity be
                    && !be.mag(event.getPlayer())) {
                event.setCanceled(true);
                event.getPlayer().sendOverlayMessage(Component.translatable("gui.guhs.mijnhuisje.van", be.eigenaarNaam()).withStyle(ChatFormatting.GRAY));
            }
        });
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(ITEM.get()));
        output.accept(new ItemStack(ModItems.clothingItem(GuhClothes.RUGZAKZADEL)));
    }

    private MijnhuisjeFeature() {
    }
}
