package nl.juiced.guhs.feature.droomweer;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.dromen.DroomEilanden;

/**
 * 1.8.0 (playtest 2): the droomweer and its droomkistje.
 * <ul>
 *   <li>The droomweer is the Weer vadser's ninth guh-weertje ({@code Weertje.DROOM}): on the server WeerEffecten (floaty,
 *       dream stars, sleepy guhs), on the client {@code client/DroomweerClient} (Guhdromenland's sky over the normal one,
 *       the plein's dream music) and WeerClient (twinkles).</li>
 *   <li>Its weerpotje comes from the droomkistje ({@link DroomKistje}) at the top of the Sterrenkussenveld, put next to the
 *       big star by {@link #tick} (once every few seconds while someone dreams near that island).</li>
 * </ul>
 */
public final class DroomweerFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<DroomKistjeBlock> DROOMKISTJE = BLOCKS.registerBlock("droomkistje", DroomKistjeBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).mapColor(MapColor.COLOR_PURPLE).strength(2.5f).sound(SoundType.WOOD)
                    .noOcclusion().lightLevel(s -> 6));
    public static final DeferredItem<BlockItem> DROOMKISTJE_ITEM = ITEMS.registerItem("droomkistje", p -> new BlockItem(DROOMKISTJE.get(), p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("block.guhs.droomkistje.lore").withStyle(ChatFormatting.GRAY));
        }
    }, () -> new Item.Properties().useBlockDescriptionPrefix());

    /** How often the kistje is checked for (ticks), and how close a dreamer must be to the Sterrenkussenveld. */
    static final int ELKE = 100;
    static final double BIJ_EILAND = 90;

    private DroomweerFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(DroomweerFeature::tick);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(DROOMKISTJE_ITEM.get()));
    }

    private static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % ELKE != 17) {
            return;
        }
        ServerLevel level = event.getServer().getLevel(DromenFeature.DIM);
        if (level == null || level.players().isEmpty()) {
            return;
        }
        Vec3 midden = Vec3.atBottomCenterOf(DroomEilanden.midden(DroomEilanden.STERREN));
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceTo(midden) < BIJ_EILAND) {
                DroomKistje.zetNeer(level);
                return;
            }
        }
    }
}
