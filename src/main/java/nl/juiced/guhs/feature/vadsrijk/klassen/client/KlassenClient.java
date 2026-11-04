package nl.juiced.guhs.feature.vadsrijk.klassen.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;
import nl.juiced.guhs.feature.vadsrijk.klassen.Basisaanval;
import nl.juiced.guhs.feature.vadsrijk.klassen.KlassenPayloads;
import org.lwjgl.glfw.GLFW;

/**
 * Het Vadsrijk, slice "klassen", the client: the skill tree key (K, category Vadsrijk, only in the dimension), the ranged
 * basic attack (in the dimension a left click with a bow/crossbow/catapult/staff/wand/book doesn't hit or break: holding
 * the button sends {@link KlassenPayloads.Schiet} and the server keeps the rhythm), and the screens.
 */
public final class KlassenClient {
    public static final KeyMapping BOOM = new KeyMapping("key.guhs.vr_kl_boom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KernClient.CATEGORIE);
    private static int vuurTik;

    private KlassenClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(BOOM));
        KlassenPayloads.staatOntvanger = s -> open(s.tag());
        SkillInfo.init();   // fix round 2 (g-ui): HUD skill slot tooltips while the inventory is open
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (e.isAttack() && mc.player != null && Vadsrijk.in(mc.player) && Basisaanval.isAfstandswapen(mc.player.getMainHandItem())) {
                e.setSwingHand(false);
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            boolean binnen = mc.player != null && Vadsrijk.in(mc.player);
            while (BOOM.consumeClick()) {
                if (binnen && mc.screen == null) {
                    ClientPacketDistributor.sendToServer(new KlassenPayloads.Vraag());
                }
            }
            if (binnen && mc.screen == null && mc.options.keyAttack.isDown() && Basisaanval.isAfstandswapen(mc.player.getMainHandItem())) {
                if (vuurTik-- <= 0) {
                    ClientPacketDistributor.sendToServer(new KlassenPayloads.Schiet());
                    vuurTik = 2;
                }
                // The attack-ready indicator: kern sets the attack speed attribute to the weapon's interval, so the vanilla
                // crosshair cooldown runs at the same pace as the server's shots; restart it when a shot goes out (prediction).
                if (mc.player.getAttackStrengthScale(0f) >= 1f) {
                    mc.player.resetAttackStrengthTicker();
                }
            } else {
                vuurTik = 0;
            }
        });
    }

    private static void open(CompoundTag t) {
        Minecraft mc = Minecraft.getInstance();
        String modus = t.getStringOr("modus", "boom");
        if (modus.equals("kies")) {
            if (mc.screen instanceof KeuzeScherm k) {
                k.zet(t);
            } else {
                mc.setScreen(new KeuzeScherm(t));
            }
        } else if (mc.screen instanceof BoomScherm b) {
            b.zet(t);
        } else {
            mc.setScreen(new BoomScherm(t));
        }
    }
}
