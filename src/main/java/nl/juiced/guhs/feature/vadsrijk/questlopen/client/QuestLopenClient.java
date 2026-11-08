package nl.juiced.guhs.feature.vadsrijk.questlopen.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.questlopen.QlNet;

/**
 * U-QUESTLOPEN, client: receives the state ({@link QlNet.Stand}) and opens / refreshes / closes the {@link QuestLopenScherm}.
 * A "sluit" only closes OUR screen (a talking screen that is already open stays).
 */
public final class QuestLopenClient {
    @Nullable
    static QlNet.Stand laatste;

    private QuestLopenClient() {
    }

    public static void init(IEventBus modBus) {
        QlNet.ontvanger = s -> Minecraft.getInstance().execute(() -> ontvang(s));
    }

    static void ontvang(QlNet.Stand s) {
        Minecraft mc = Minecraft.getInstance();
        if (s.meta().getBooleanOr("sluit", false)) {
            if (mc.screen instanceof QuestLopenScherm) {
                mc.setScreen(null);
            }
            return;
        }
        laatste = s;
        if (mc.screen instanceof QuestLopenScherm q) {
            q.nieuw();
        } else if (s.meta().getBooleanOr("open", false) && (mc.screen == null || mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) {
            mc.setScreen(new QuestLopenScherm());
        }
    }

    static void doe(String wat, int arg) {
        ClientPacketDistributor.sendToServer(new QlNet.Actie(wat, arg));
    }
}
