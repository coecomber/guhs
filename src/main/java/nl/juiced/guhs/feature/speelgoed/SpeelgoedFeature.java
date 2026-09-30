package nl.juiced.guhs.feature.speelgoed;

import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.huisje.Speelgoed;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * Het speelgoed (2.10 "Lieve vadsjes van elkaar", slice speelgoed): four toys in guh style that guhs play with when a
 * player is near ({@link SpeelGoal}) and that the residents of a Guhhuisje sometimes use at random (the four
 * {@link Speeltjes} registered with the huisje's {@link Speelgoed}):
 * <ul>
 *   <li>the <b>Knabbelbal</b> ({@link KnabbelbalEntity}): a fluffy pink ball with guh ears and a kaasknabbel inside; guhs
 *       push it around until the knabbel pops out (and eat it), players kick it;</li>
 *   <li>the <b>Guh-glijbaantje</b> ({@link GlijbaanBlock}): climb the klimrek, slide down "wieee", again!;</li>
 *   <li>the <b>Pluizige tunnel</b> ({@link TunnelBlock}): connectable pink fur tunnel pieces; guhs run through and hide
 *       (verstoppertje with the muisje, or with you: knock on the tunnel);</li>
 *   <li>the <b>Guh-wip</b> and <b>Guh-schommel</b> ({@link WipBlock}, {@link SchommelBlock}): guhs sit on them
 *       ({@link ZitjeEntity}) and you can push them (right-click).</li>
 * </ul>
 * Playing gives SPEELGOED hearts, the SPEELTJE moment, the dagboek stat SPEELTJES and "eerste speeltje" ({@link Spelen}).
 * tools/features/speelgoed.py makes the models, textures, recipes, sounds, texts, advancements and FTB quests.
 */
public final class SpeelgoedFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Guhs.MODID);

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.2f, 4f).sound(SoundType.WOOL).noOcclusion()
                .pushReaction(PushReaction.BLOCK).ignitedByLava();
    }

    public static final DeferredBlock<GlijbaanBlock> GLIJBAANTJE = BLOCKS.register("guh_glijbaantje", () -> new GlijbaanBlock(props()));
    public static final DeferredBlock<WipBlock> WIP = BLOCKS.register("guh_wip", () -> new WipBlock(props()));
    public static final DeferredBlock<SchommelBlock> SCHOMMEL = BLOCKS.register("guh_schommel", () -> new SchommelBlock(props()));
    public static final DeferredBlock<TunnelBlock> TUNNEL = BLOCKS.register("pluizige_tunnel", () -> new TunnelBlock(props().strength(0.6f, 2f)));
    /** The invisible parts of the glijbaantje, the wip and the schommel (no item: breaking one breaks the toy). */
    public static final DeferredBlock<SpeelDeelBlock> DEEL = BLOCKS.registerBlock("speelgoed_deel", SpeelDeelBlock::new, props().noLootTable());

    public static final DeferredItem<KnabbelbalItem> KNABBELBAL_ITEM = ITEMS.registerItem("knabbelbal", KnabbelbalItem::new,
            new Item.Properties().stacksTo(16));

    static {
        ITEMS.registerItem("guh_glijbaantje", p -> new SpeelgoedBlockItem(GLIJBAANTJE.get(), p), new Item.Properties());
        ITEMS.registerItem("guh_wip", p -> new SpeelgoedBlockItem(WIP.get(), p), new Item.Properties());
        ITEMS.registerItem("guh_schommel", p -> new SpeelgoedBlockItem(SCHOMMEL.get(), p), new Item.Properties());
        ITEMS.registerItem("pluizige_tunnel", p -> new SpeelgoedBlockItem(TUNNEL.get(), p), new Item.Properties());
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ToestelBlockEntity>> TOESTEL_BE = BLOCK_ENTITIES.register("speelgoed_toestel",
            () -> BlockEntityType.Builder.of(ToestelBlockEntity::new, GLIJBAANTJE.get(), WIP.get(), SCHOMMEL.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<KnabbelbalEntity>> KNABBELBAL = ENTITIES.register("knabbelbal",
            () -> EntityType.Builder.<KnabbelbalEntity>of(KnabbelbalEntity::new, MobCategory.MISC)
                    .sized(KnabbelbalEntity.SIZE, KnabbelbalEntity.SIZE).clientTrackingRange(8).updateInterval(1)
                    .build(Guhs.id("knabbelbal").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ZitjeEntity>> ZITJE = ENTITIES.register("speelgoed_zitje",
            () -> EntityType.Builder.<ZitjeEntity>of(ZitjeEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.2f).clientTrackingRange(10).updateInterval(20).noSummon()
                    .build(Guhs.id("speelgoed_zitje").toString()));

    /** Every toy block that guhs can find (glijbaantje, wip, schommel and every tunnel piece). */
    public static final DeferredHolder<PoiType, PoiType> POI = POI_TYPES.register("speelgoed", () -> new PoiType(ImmutableSet.<BlockState>builder()
            .addAll(GLIJBAANTJE.get().getStateDefinition().getPossibleStates()).addAll(WIP.get().getStateDefinition().getPossibleStates())
            .addAll(SCHOMMEL.get().getStateDefinition().getPossibleStates()).addAll(TUNNEL.get().getStateDefinition().getPossibleStates())
            .build(), 0, 1));

    public static final DeferredHolder<SoundEvent, SoundEvent> WIEEE = geluid("speelgoed.wieee");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUW = geluid("speelgoed.duw");
    public static final DeferredHolder<SoundEvent, SoundEvent> BAL = geluid("speelgoed.bal");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLOP = geluid("speelgoed.plop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEVONDEN = geluid("speelgoed.gevonden");
    public static final DeferredHolder<SoundEvent, SoundEvent> KLIM = geluid("speelgoed.klim");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Guhs.id(id)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        POI_TYPES.register(modBus);
        Speeltjes.registreer();
        GuhHooks.doelen((guh, goals) -> goals.addGoal(4, new SpeelGoal(guh)));
        GuhHooks.tick(Spelen::tick);
        NeoForge.EVENT_BUS.register(Spelen.class);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KNABBELBAL_ITEM.get()));
        output.accept(new ItemStack(GLIJBAANTJE.get()));
        output.accept(new ItemStack(TUNNEL.get()));
        output.accept(new ItemStack(WIP.get()));
        output.accept(new ItemStack(SCHOMMEL.get()));
    }

    /** A toy block item with its lore line. */
    static final class SpeelgoedBlockItem extends BlockItem {
        SpeelgoedBlockItem(net.minecraft.world.level.block.Block block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip,
                                    net.minecraft.world.item.TooltipFlag flag) {
            String key = getBlock().getDescriptionId();
            tooltip.add(net.minecraft.network.chat.Component.translatable(key + ".lore").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            tooltip.add(net.minecraft.network.chat.Component.translatable(key + ".uitleg").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    private SpeelgoedFeature() {
    }
}
