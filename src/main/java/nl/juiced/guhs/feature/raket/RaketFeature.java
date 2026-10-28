package nl.juiced.guhs.feature.raket;

import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.8.0 Vadskracht (the second theme, design: MineScapeStory/ontwerp-guhs180-vadskracht.md V4-V6): the Kaas-ruimtevaart.
 * <ul>
 *   <li>{@link LanceerplatformBlock} (guhs:lanceerplatform): one block that shows a ghost rocket; you hand in four stages
 *       ({@link RaketStappen}: romp, motor, brandstof, bemanning) and per stage the rocket builds itself above the platform
 *       ({@link RaketBouw}). With all four: the launch ({@link RaketScene}) and off to the Kaasmaan.</li>
 *   <li>The crew: three tame guhs in an {@link GuhClothes#ASTRONAUTENPAKJE} (a new piece, with the
 *       {@link GuhClothes#ASTRONAUTENHELM}; crafted, source "raket").</li>
 *   <li>{@link KaasMaan} (dimension guhs:kaasmaan): a small cheese moon with low gravity, a moon base (built once) with
 *       astronaut Kaasje (trades kaasmaankristallen for souvenirs), craters with kaasmaankristal ore, a return capsule
 *       ({@link TerugkeercapsuleBlock}) and a flag to plant (guhs:maanvlaggetje).</li>
 * </ul>
 * Resources: tools/features/raket.py. Test commands: {@link RaketCommando} (/guhs raket ...).
 */
public final class RaketFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    /** The source of the astronaut clothes (gui.guhs.kledingbron.raket). */
    public static final String BRON = "raket";

    private static <B extends Block> DeferredBlock<B> blok(String id, Function<BlockBehaviour.Properties, B> maak,
                                                           java.util.function.Supplier<BlockBehaviour.Properties> props) {
        DeferredBlock<B> b = BLOCKS.registerBlock(id, maak, props);
        ITEMS.registerItem(id, p -> new BlockItem(b.get(), p) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                tooltip.accept(Component.translatable("block.guhs." + id + ".lore").withStyle(ChatFormatting.GRAY));
            }
        }, () -> new Item.Properties().useBlockDescriptionPrefix());
        return b;
    }

    public static final DeferredBlock<LanceerplatformBlock> LANCEERPLATFORM = blok("lanceerplatform", LanceerplatformBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PINK).strength(4f).sound(SoundType.METAL));
    public static final DeferredBlock<TerugkeercapsuleBlock> TERUGKEERCAPSULE = blok("terugkeercapsule", TerugkeercapsuleBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.SNOW).strength(-1f, 3600000f).noOcclusion()
                    .lightLevel(s -> 7));
    public static final DeferredBlock<Block> KAASMAANKRISTAL_ERTS = blok("kaasmaankristal_erts", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.GOLD).strength(2f).lightLevel(s -> 5)
                    .sound(SoundType.AMETHYST));
    public static final DeferredBlock<MaanvlaggetjeBlock> MAANVLAGGETJE = blok("maanvlaggetje", MaanvlaggetjeBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_BANNER).mapColor(MapColor.COLOR_PINK).noCollision().strength(0.5f)
                    .sound(SoundType.WOOL));

    public static final DeferredItem<Item> KAASMAANKRISTAL = ITEMS.registerItem("kaasmaankristal", p -> new Item(p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.kaasmaankristal.lore").withStyle(ChatFormatting.GRAY));
        }
    }, () -> new Item.Properties());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LanceerplatformBlockEntity>> LANCEERPLATFORM_BE = BLOCK_ENTITIES.register(
            "lanceerplatform", () -> new BlockEntityType<>(LanceerplatformBlockEntity::new, LANCEERPLATFORM.get()));

    private RaketFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        RaketScene.init();
        KledingBronnen.bron(GuhClothes.ASTRONAUTENPAKJE, BRON, KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.raket"));
        KledingBronnen.bron(GuhClothes.ASTRONAUTENHELM, BRON, KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.raket"));
        NeoForge.EVENT_BUS.addListener(KaasMaan::tick);
        NeoForge.EVENT_BUS.addListener(KaasMaan::opKlik);
        NeoForge.EVENT_BUS.addListener(KaasMaan::opNeerzetten);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> RaketCommando.register(event));
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(LANCEERPLATFORM.get()));
        output.accept(new ItemStack(MAANVLAGGETJE.get()));
        output.accept(new ItemStack(KAASMAANKRISTAL_ERTS.get()));
        output.accept(new ItemStack(KAASMAANKRISTAL.get()));
        output.accept(new ItemStack(ModItems.clothingItem(GuhClothes.ASTRONAUTENPAKJE)));
        output.accept(new ItemStack(ModItems.clothingItem(GuhClothes.ASTRONAUTENHELM)));
    }
}
