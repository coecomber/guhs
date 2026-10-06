package nl.juiced.guhs.feature.techbron;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * bbq2 (tech-bronnen): the sources of vadskracht.
 * <ul>
 *   <li><b>Guhrad</b> (exists; {@code block.GuhWheel*}): what a guh gives per variant is data ({@code GuhradKracht}); this
 *       slice adds how each story guh does its rounds ({@link GuhradStijl}) and the happy guh that comes from the cushion.</li>
 *   <li><b>Knuffelgenerator</b> ({@link KnuffelgeneratorBlock}): a pink cushion of 2 x 2; 3 VK per tamed guh that lies on it,
 *       at most 8; they come by themselves ({@link GuhTrek}).</li>
 *   <li><b>Disco-dynamo</b> ({@link DiscoDynamoBlock}): a dance floor of 3 x 3 with a looping turntable; 5 VK per dancing guh,
 *       at most 4; every disc its own light show ({@link LichtShow}); a rare disc gives a little more.</li>
 *   <li><b>Blubkacheltje</b> ({@link BlubkacheltjeBlock}): a Sausblubje in a jar, 6 VK, a knabbel now and then.</li>
 *   <li><b>Gloeisterkern</b> ({@link GloeisterkernBlock}): 200 VK for ever, costs one gloeister.</li>
 *   <li><b>Knabbelbatterij</b> ({@link KnabbelbatterijBlock}): 18,000 VK, keeps its charge when broken.</li>
 * </ul>
 * How many of a kind count per net and the "telt niet mee" line are the vadskracht foundation's ({@code BronSoort},
 * {@code VadsUitlezing}); a source that does not count shows its sleeping face ({@link BronBlockEntity}).
 * Also here: the per-player flag "Aangebrande Mika verslagen" ({@link AangebrandeMika}).
 * Resources: tools/features/tech_bronnen.py.
 */
public final class TechbronFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Guhs.MODID);

    /** Discs that make every dancer of a Disco-dynamo give a little more (the mod's own disc). */
    public static final TagKey<Item> ZELDZAME_PLAAT = TagKey.create(Registries.ITEM, Guhs.id("techbron/zeldzame_plaat"));
    /** What fills a Blubkacheltje: a Sausblubje in a jar ({@code guhs:sausblubje_potje} of the sausdieren slice). */
    public static final TagKey<Item> BLUBJE_IN_POT = TagKey.create(Registries.ITEM, Guhs.id("techbron/blubje_in_pot"));
    /** What the Sausblubje in a Blubkacheltje eats: kaasknabbels, and (optional entry) {@code #guhs:sausdieren/sausblubje_voer}. */
    public static final TagKey<Item> BLUBVOER = TagKey.create(Registries.ITEM, Guhs.id("techbron/blubvoer"));

    // --- the fixed ids of CONTRACT_130 7 ---
    public static final DeferredBlock<KnuffelgeneratorBlock> KNUFFELGENERATOR = BLOCKS.registerBlock("knuffelgenerator", KnuffelgeneratorBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> KNUFFELGENERATOR_ITEM = ITEMS.registerSimpleBlockItem(KNUFFELGENERATOR);
    public static final DeferredBlock<DiscoDynamoBlock> DISCO_DYNAMO = BLOCKS.registerBlock("disco_dynamo", DiscoDynamoBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.0f, 6f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(DiscoDynamoBlock::licht).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> DISCO_DYNAMO_ITEM = ITEMS.registerSimpleBlockItem(DISCO_DYNAMO);
    public static final DeferredBlock<BlubkacheltjeBlock> BLUBKACHELTJE = BLOCKS.registerBlock("blubkacheltje", BlubkacheltjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2.0f, 6f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(BlubkacheltjeBlock::licht).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> BLUBKACHELTJE_ITEM = ITEMS.registerSimpleBlockItem(BLUBKACHELTJE);
    public static final DeferredBlock<GloeisterkernBlock> GLOEISTERKERN = BLOCKS.registerBlock("gloeisterkern", GloeisterkernBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(3.0f, 9f).sound(SoundType.AMETHYST).noOcclusion()
                    .lightLevel(GloeisterkernBlock::licht).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> GLOEISTERKERN_ITEM = ITEMS.registerSimpleBlockItem(GLOEISTERKERN);
    public static final DeferredBlock<KnabbelbatterijBlock> KNABBELBATTERIJ = BLOCKS.registerBlock("knabbelbatterij", KnabbelbatterijBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2.0f, 6f).sound(SoundType.METAL)
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> KNABBELBATTERIJ_ITEM = ITEMS.registerSimpleBlockItem(KNABBELBATTERIJ);

    // --- the part blocks (no items): the rest of the cushion and of the dance floor ---
    public static final DeferredBlock<LaagDeelBlock> KUSSEN_DEEL = BLOCKS.registerBlock("techbron_kussen_deel",
            p -> new LaagDeelBlock(p, KnuffelgeneratorBlock.HOOGTE, false),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f).sound(SoundType.WOOL).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<LaagDeelBlock> VLOER_DEEL = BLOCKS.registerBlock("techbron_vloer_deel",
            p -> new LaagDeelBlock(p, DiscoDynamoBlock.VLOER, true),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.0f, 6f).sound(SoundType.METAL).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.BLOCK));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KnuffelgeneratorBlockEntity>> KNUFFELGENERATOR_BE = BLOCK_ENTITIES.register(
            "knuffelgenerator", () -> new BlockEntityType<>(KnuffelgeneratorBlockEntity::new, KNUFFELGENERATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiscoDynamoBlockEntity>> DISCO_DYNAMO_BE = BLOCK_ENTITIES.register(
            "disco_dynamo", () -> new BlockEntityType<>(DiscoDynamoBlockEntity::new, DISCO_DYNAMO.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlubkacheltjeBlockEntity>> BLUBKACHELTJE_BE = BLOCK_ENTITIES.register(
            "blubkacheltje", () -> new BlockEntityType<>(BlubkacheltjeBlockEntity::new, BLUBKACHELTJE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GloeisterkernBlock.Kern>> GLOEISTERKERN_BE = BLOCK_ENTITIES.register(
            "gloeisterkern", () -> new BlockEntityType<>(GloeisterkernBlock.Kern::new, GLOEISTERKERN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KnabbelbatterijBlock.Kern>> KNABBELBATTERIJ_BE = BLOCK_ENTITIES.register(
            "knabbelbatterij", () -> new BlockEntityType<>(KnabbelbatterijBlock.Kern::new, KNABBELBATTERIJ.get()));

    /** {@code guhs:techbron_lading}: the charge (VK) of a Knabbelbatterij item; absent on an empty one. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LADING = COMPONENTS.registerComponentType("techbron_lading",
            b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener(TechbronFeature::capabilities);
        GuhTrek.register();
        NeoForge.EVENT_BUS.register(AangebrandeMika.class);
        NeoForge.EVENT_BUS.addListener(TechbronCommands::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KNUFFELGENERATOR_ITEM.get()));
        output.accept(new ItemStack(DISCO_DYNAMO_ITEM.get()));
        output.accept(new ItemStack(BLUBKACHELTJE_ITEM.get()));
        output.accept(new ItemStack(GLOEISTERKERN_ITEM.get()));
        output.accept(new ItemStack(KNABBELBATTERIJ_ITEM.get()));
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        VadskrachtFeature.knoopCapability(event, KNUFFELGENERATOR_BE.get());
        VadskrachtFeature.knoopCapability(event, DISCO_DYNAMO_BE.get());
        VadskrachtFeature.knoopCapability(event, BLUBKACHELTJE_BE.get());
        VadskrachtFeature.knoopCapability(event, GLOEISTERKERN_BE.get());
        VadskrachtFeature.knoopCapability(event, KNABBELBATTERIJ_BE.get());
        VadskrachtFeature.deelCapabilities(event, KUSSEN_DEEL.get(), VLOER_DEEL.get());
        // Knabbelbuizen and hoppers feed the Sausblubje
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLUBKACHELTJE_BE.get(), BlubkacheltjeBlockEntity::handler);
    }

    /** Gives a stack to a player (inventory, else at their feet). */
    static void geef(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (player instanceof ServerPlayer sp) {
            Minigames.give(sp, stack);
        } else {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    /** A Knabbelbatterij item that holds this much (0: an empty one). */
    public static ItemStack batterij(long lading) {
        ItemStack stack = new ItemStack(KNABBELBATTERIJ_ITEM.get());
        if (lading > 0) {
            stack.set(LADING.get(), Math.min(lading, nl.juiced.guhs.feature.vadskracht.VadsGetallen.BATTERIJ));
        }
        return stack;
    }

    private TechbronFeature() {
    }
}
