package nl.juiced.guhs.feature.beauty;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.item.component.TooltipDisplay;
/**
 * The Guh Beauty Vads-wedstrijd (2.4): a grand catwalk theatre in the Guhmension where the Showguh runs a beauty
 * contest for guhs ({@link BeautyShow}). Its own blocks (stage markers, the loaner wardrobe), the showrozet (the prize
 * currency) and the Showster outfit in the Showguh's shop (tiara, "Miss Vadsig" sash, glitter bow).
 */
public final class BeautyFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** The invisible markers of the stage (where the model stands) and the end of the catwalk. */
    public static final DeferredBlock<BeautyBlocks.Plek> PLEK = BLOCKS.registerBlock("beauty_plek", BeautyBlocks.Plek::new,
            () -> BlockBehaviour.Properties.of().noCollision().noLootTable().strength(-1f, 3600000f).noOcclusion().isValidSpawn((s, l, p, e) -> false));
    /** The loaner wardrobe: opens the dressing screen during your show. */
    public static final DeferredBlock<BeautyBlocks.Leenkast> LEENKAST = BLOCKS.registerBlock("beauty_leenkast", BeautyBlocks.Leenkast::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2f).sound(SoundType.CHERRY_WOOD));
    public static final DeferredItem<BlockItem> LEENKAST_ITEM = ITEMS.registerSimpleBlockItem(LEENKAST);
    /** Showrozetten: won on the catwalk, spent on the Showster outfit. */
    public static final DeferredItem<Rozet> SHOWROZET = ITEMS.registerItem("showrozet", Rozet::new, () -> new Item.Properties());

    /** Prices in the Showguh's shop (showrozetten). */
    public static final int PRICE_STRIK = 6, PRICE_SJERP = 10, PRICE_TIARA = 16;

    private static final NpcRole ROLE = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            BeautyShow.talk(npc, player);
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            if (npc.tickCount % 100 == 1) {
                BeautyShow.showScores(npc);
            }
            BeautyShow show = BeautyShow.at(npc);
            if (show != null) {
                show.tick(npc);
            }
        }

        @Override
        public MerchantOffers offers(GuhNpcEntity npc) {
            MerchantOffers offers = new MerchantOffers();
            offers.add(offer(PRICE_STRIK, GuhClothes.SHOWSTER_STRIK));
            offers.add(offer(PRICE_SJERP, GuhClothes.SHOWSTER_SJERP));
            offers.add(offer(PRICE_TIARA, GuhClothes.SHOWSTER_TIARA));
            return offers;
        }
    };

    private static MerchantOffer offer(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(SHOWROZET.get(), price), new ItemStack(ModItems.clothingItem(clothes)), Integer.MAX_VALUE, 0, 0);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.register(BeautyProtection.class);
        nl.juiced.guhs.feature.Protected.add(BeautyProtection::protectedAt);
    }

    public static void payloads(PayloadRegistrar registrar) {
        BeautyPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(SHOWROZET.get()));
        output.accept(new ItemStack(LEENKAST_ITEM.get()));
    }

    @Nullable
    public static NpcRole role() {
        return ROLE;
    }

    /** The showrozet, with a hint where it comes from. */
    public static class Rozet extends Item {
        public Rozet(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.showrozet.lore").withStyle(ChatFormatting.GRAY));
        }
    }

    private BeautyFeature() {
    }
}
