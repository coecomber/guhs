package nl.juiced.guhs.feature.disco;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Guhdisco (2.4): a disco club in the Guhmension with a light-up dance floor, where the DJ-guh plays Simon says
 * ({@link DiscoGame}) for discomunten and sells the disco outfit (glitter suit, afro, star glasses, and since 2.9 the
 * little headphones) that you can only get there. 2.9: real songs ({@link DiscoLiedje}, {@link DiscoMuziek}; the song is
 * the level) and the game follows their beat. Resources: tools/features/disco.py, the songs: tools/remix/.
 */
public final class DiscoFeature {
    /** The DJ-guh: talks (the screen), runs the game and keeps the shop. */
    public static final NpcRole DJ_GUH = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            DiscoGame.talk(npc, player);
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            DiscoGame.of(npc).tick(npc);
        }

        @Override
        public MerchantOffers offers(GuhNpcEntity npc) {
            return shop();
        }
    };

    /** Prices of the disco outfit, in discomunten (2.9: the little headphones, an accessory like the afro). */
    public static final int PRICE_BRIL = 4, PRICE_AFRO = 6, PRICE_PAK = 10, PRICE_KOPTELEFOONTJE = 6;

    public static void register(IEventBus modBus) {
        DiscoBlocks.BLOCKS.register(modBus);
        DiscoBlocks.ITEMS.register(modBus);
        DiscoMuziek.SOUNDS.register(modBus);
        // 2.9: the headphones come only from the DJ-guh (the older disco pieces are registered by the kleding slice)
        nl.juiced.guhs.feature.kleding.KledingBronnen.bron(GuhClothes.DISCO_KOPTELEFOONTJE, "disco", PRICE_KOPTELEFOONTJE + " discomunten");
        NeoForge.EVENT_BUS.register(DiscoProtection.class);
        nl.juiced.guhs.feature.Protected.add(DiscoProtection::protectedAt);
        NeoForge.EVENT_BUS.addListener(DiscoGame::onDamage);
        NeoForge.EVENT_BUS.addListener(DiscoGame::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(DiscoGame::onLogout);
        NeoForge.EVENT_BUS.addListener(DiscoGame::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(DiscoGame::onServerStopped);
    }

    public static void payloads(PayloadRegistrar registrar) {
        DiscoPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(DiscoBlocks.DISCOMUNT.get()));
        DiscoBlocks.TEGELS.values().forEach(b -> output.accept(new ItemStack(b.get())));
        output.accept(new ItemStack(DiscoBlocks.DANSVLOER_ITEM.get()));
        output.accept(new ItemStack(DiscoBlocks.DISCOBAL_ITEM.get()));
        output.accept(new ItemStack(DiscoBlocks.MILKSHAKE_ITEM.get()));
    }

    @Nullable
    public static NpcRole role() {
        return DJ_GUH;
    }

    /** The DJ-guh's shop: the disco outfit (only here), plus a real shake and some dance floor for at home. */
    public static MerchantOffers shop() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(PRICE_BRIL, new ItemStack(ModItems.clothingItem(GuhClothes.DISCO_BRIL))));
        offers.add(offer(PRICE_AFRO, new ItemStack(ModItems.clothingItem(GuhClothes.DISCO_AFRO))));
        offers.add(offer(PRICE_PAK, new ItemStack(ModItems.clothingItem(GuhClothes.DISCO_GLITTERPAK))));
        offers.add(offer(PRICE_KOPTELEFOONTJE, new ItemStack(ModItems.clothingItem(GuhClothes.DISCO_KOPTELEFOONTJE))));
        offers.add(offer(1, new ItemStack(ModItems.KAASKNABBEL_MILKSHAKE.get())));
        offers.add(offer(1, new ItemStack(DiscoBlocks.DANSVLOER_ITEM.get(), 8)));
        offers.add(offer(2, new ItemStack(DiscoBlocks.DISCOBAL_ITEM.get(), 4)));
        return offers;
    }

    private static MerchantOffer offer(int munten, ItemStack result) {
        Item coin = DiscoBlocks.DISCOMUNT.get();
        return new MerchantOffer(new ItemCost(coin, munten), result, Integer.MAX_VALUE, 0, 0);
    }

    private DiscoFeature() {
    }
}
