package nl.juiced.guhs.feature.meppen;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.NpcRole;

/**
 * Mika meppen (whack-a-Mika) in the Mika-mephal, a rare fairground hall in the Guhmension. The Mepguh lends you a
 * Mika-mephamer; for 60 seconds Mikas pop up out of the 16 holes of the board, faster and faster. Whack them (combo
 * multiplier!), golden Mikas are worth a lot, but never whack the guh. Mepmunten (by score) buy the Mika-hunter outfit.
 * The game itself: {@link MepGame}; the hall is protected by {@link MepProtection}.
 */
public final class MeppenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    private static BlockBehaviour.Properties unbreakable(MapColor colour, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(colour).strength(-1f, 3600000f).noLootTable().sound(sound)
                .isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK);
    }

    /** A hole of the whack-a-Mika board. */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> MEP_GAT = BLOCKS.registerSimpleBlock("mika_mep_gat",
            () -> unbreakable(MapColor.COLOR_PINK, SoundType.WOOD));
    /** A head popped up out of a hole (only the game places these). */
    public static final DeferredBlock<MepBlocks.MepKop> MEP_KOP = BLOCKS.registerBlock("mika_mep_kop", MepBlocks.MepKop::new,
            () -> unbreakable(MapColor.COLOR_PINK, SoundType.WOOL).noOcclusion());
    public static final DeferredBlock<MepBlocks.MikaTrofee> TROFEE = BLOCKS.registerBlock("mika_mep_trofee", MepBlocks.MikaTrofee::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.8f).sound(SoundType.WOOL).noOcclusion());

    public static final DeferredItem<net.minecraft.world.item.BlockItem> MEP_GAT_ITEM = ITEMS.registerSimpleBlockItem(MEP_GAT);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> TROFEE_ITEM = ITEMS.registerSimpleBlockItem(TROFEE);
    /** The currency of the Mepguh's shop: earned by playing (more for a better score). */
    public static final DeferredItem<Item> MEPMUNT = ITEMS.registerSimpleItem("mepmunt", () -> new Item.Properties());
    /** The loaned mallet (taken back after the game). */
    public static final DeferredItem<MepHamerItem> MEP_HAMER = ITEMS.registerItem("mika_mep_hamer", MepHamerItem::new,
            () -> new Item.Properties().stacksTo(1));

    private static final MeppenRole ROLE = new MeppenRole();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.register(MepProtection.class);
        NeoForge.EVENT_BUS.addListener(MepGame::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(MepGame::onDamage);
        NeoForge.EVENT_BUS.addListener(MepGame::onToss);
        NeoForge.EVENT_BUS.addListener(MepGame::onDrops);
        NeoForge.EVENT_BUS.addListener(MepGame::onInteractEntity);
        NeoForge.EVENT_BUS.addListener(MepGame::onInteractEntityAt);
        NeoForge.EVENT_BUS.addListener(MepGame::onDeath);
        NeoForge.EVENT_BUS.addListener(MepGame::onLogout);
        NeoForge.EVENT_BUS.addListener(MepGame::onLogin);
        NeoForge.EVENT_BUS.addListener(MepGame::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(MepGame::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(MepGame::onServerStopped);
        nl.juiced.guhs.feature.Protected.add(MepProtection::protectedAt);
    }

    public static void payloads(PayloadRegistrar registrar) {
        MepPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(MEPMUNT.get()));
        output.accept(new ItemStack(TROFEE_ITEM.get()));
        output.accept(new ItemStack(MEP_GAT_ITEM.get()));
    }

    @Nullable
    public static NpcRole role() {
        return ROLE;
    }

    private MeppenFeature() {
    }
}
