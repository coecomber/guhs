package nl.juiced.guhs.feature.bibliotheek;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhFurnitureBlock;
import nl.juiced.guhs.feature.NpcRole;

/**
 * De guhbibliotheek: a very rare, grand library in the Guhmension. The Bibliothecaris lends you guh lore books (one free
 * per guh day), asks quiz questions about them for boekenbonnen and sells the books, the librarian's outfit and the guh
 * armchair; behind a secret bookcase lies the Secret Guh Book. See {@link Bibliothecaris}, {@link Guhboek}; the structure
 * and all resources come from tools/features/bibliotheek.py.
 */
public final class BibliotheekFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<GeheimeKastBlock> GEHEIME_KAST = BLOCKS.registerBlock("bieb_geheime_kast", GeheimeKastBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF).noOcclusion());
    /** The stand in the secret room: can't be broken or crafted (it would hand out the secret book anywhere). */
    public static final DeferredBlock<BoekaltaarBlock> BOEKALTAAR = BLOCKS.registerBlock("bieb_boekaltaar", BoekaltaarBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(-1f, 3600000f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 7));
    /** A guh-shaped reading armchair: sit in it (right-click with an empty hand). */
    public static final DeferredBlock<GuhFurnitureBlock> GUHFAUTEUIL = BLOCKS.registerBlock("bieb_guhfauteuil",
            p -> new GuhFurnitureBlock(p, 0.5, new double[]{1, 0, 1, 15, 8, 15}, new double[]{1, 3, 12, 15, 22, 16},
                    new double[]{0, 3, 1, 3, 12, 12}, new double[]{13, 3, 1, 16, 12, 12}),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_WOOL).strength(0.8f).noOcclusion());

    public static final DeferredItem<Item> BOEKENBON = ITEMS.registerItem("boekenbon", p -> new Item(p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.guhs.boekenbon.lore").withStyle(ChatFormatting.GRAY));
        }
    }, new Item.Properties());
    public static final DeferredItem<BlockItem> GEHEIME_KAST_ITEM = ITEMS.registerSimpleBlockItem(GEHEIME_KAST);
    public static final DeferredItem<BlockItem> BOEKALTAAR_ITEM = ITEMS.registerSimpleBlockItem(BOEKALTAAR);
    public static final DeferredItem<BlockItem> GUHFAUTEUIL_ITEM = ITEMS.registerSimpleBlockItem(GUHFAUTEUIL);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.register(BibliotheekProtection.class);
        nl.juiced.guhs.feature.Protected.add(BibliotheekProtection::inLibrary);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> BibliotheekProtection.TEST_AREAS.clear());
        NeoForge.EVENT_BUS.addListener(BibliotheekFeature::onPlayerTick);
    }

    public static void payloads(PayloadRegistrar registrar) {
        BibliotheekPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BOEKENBON.get()));
        output.accept(new ItemStack(GUHFAUTEUIL_ITEM.get()));
        output.accept(new ItemStack(GEHEIME_KAST_ITEM.get()));
        output.accept(new ItemStack(BOEKALTAAR_ITEM.get()));
        for (Guhboek book : Guhboek.values()) {
            output.accept(book.stack());
        }
    }

    @Nullable
    public static NpcRole role() {
        return Bibliothecaris.INSTANCE;
    }

    /** Every second: guh books in the inventory go into the player's collection. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 7) {
            Bibliothecaris.collect(player);
        }
    }

    private BibliotheekFeature() {
    }
}
