package nl.juiced.guhs.feature.vadsrijk.karakter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.vadsrijk.karakter.KarakterPayloads;

/** T-KARAKTER, client: the character screen and refreshing the names when the badges change. No key (command/NPC/entrance). */
public final class KarakterClient {
    private KarakterClient() {
    }

    public static void init(IEventBus modBus) {
        KarakterPayloads.schermOntvanger = s -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof KarakterSchermClient k) {
                k.update(s.data(), java.util.List.of());
            } else {
                mc.setScreen(new KarakterSchermClient(s.data()));
            }
        });
        KarakterPayloads.badgesOntvangen = () -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                for (Player p : mc.level.players()) {
                    p.refreshDisplayName();
                }
            }
        });
    }
}
