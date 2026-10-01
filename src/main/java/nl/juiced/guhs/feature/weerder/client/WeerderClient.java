package nl.juiced.guhs.feature.weerder.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.weerder.WeerderPayloads;

/** Client side of the Wilde-guhweerder (1.2.0): its screen and its blue dome. */
public final class WeerderClient {
    public static void init(IEventBus modBus) {
        WeerderPayloads.opener = p -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof WeerderScreen s && s.pos().asLong() == p.data().getLongOr("Pos", 0L)) {
                s.update(p.data());
            } else {
                mc.setScreen(new WeerderScreen(p.data()));
            }
        });
        NeoForge.EVENT_BUS.addListener(WeerderKoepel::teken);
    }

    private WeerderClient() {
    }
}
