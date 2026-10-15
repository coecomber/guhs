package nl.juiced.guhs.feature.bank.client;

import net.neoforged.bus.api.IEventBus;

/**
 * Client side of bbq2 (bank). Nothing to set up: the Hapluikje is an ordinary block model (its three faces are block
 * states), the tooltips live in the item classes, the Bank Guh's screen is {@code client/screen/BankGuhScreen} (registered
 * in GuhsClient) and an upgraded bank sparkles from {@code BankGuhBlock#animateTick}.
 */
public final class BankClient {
    public static void init(IEventBus modBus) {
    }

    private BankClient() {
    }
}
