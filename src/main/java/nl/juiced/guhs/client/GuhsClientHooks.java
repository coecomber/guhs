package nl.juiced.guhs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import nl.juiced.guhs.client.screen.GuhDexScreen;
import nl.juiced.guhs.client.screen.MaagSettingsScreen;
import nl.juiced.guhs.client.screen.RpsScreen;
import nl.juiced.guhs.network.MaagPayloads;

/** What the client does with messages from the server (kept apart so a dedicated server never loads client code). */
public final class GuhsClientHooks {
    public static void openMaagSettings(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MaagSettingsScreen screen) {
            screen.update(data);
        } else {
            mc.setScreen(new MaagSettingsScreen(data));
        }
    }

    public static void rpsState(MaagPayloads.RpsState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof RpsScreen screen) {
            screen.update(state);
        } else if (state.open()) {
            mc.setScreen(new RpsScreen(state));
        }
    }

    public static void openGuhDex(MaagPayloads.GuhDexData data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof GuhDexScreen screen) {
            screen.update(data);
        } else {
            mc.setScreen(new GuhDexScreen(data));
        }
    }

    /** The Guhdex Highscores page: kept until the next update (the Guhdex screen shows it on its Highscores tab). */
    public static void highscores(MaagPayloads.HighscoresData data) {
        GuhDexScreen.highscores = data.rows();
    }

    public static void openVerstop(MaagPayloads.VerstopOpen data) {
        Minecraft.getInstance().setScreen(new nl.juiced.guhs.client.screen.VerstopScreen(data));
    }

    public static void openSuperkompas(net.minecraft.world.InteractionHand hand, @javax.annotation.Nullable String chosen) {
        Minecraft.getInstance().setScreen(new nl.juiced.guhs.client.screen.SuperkompasScreen(hand, chosen));
    }

    public static void openReis(MaagPayloads.ReisOpen data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof nl.juiced.guhs.client.screen.ReisguhScreen screen) {
            screen.update(data);
        } else {
            mc.setScreen(new nl.juiced.guhs.client.screen.ReisguhScreen(data));
        }
    }

    private GuhsClientHooks() {
    }
}
