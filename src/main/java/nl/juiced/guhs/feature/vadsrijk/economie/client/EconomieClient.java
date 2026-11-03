package nl.juiced.guhs.feature.vadsrijk.economie.client;

import java.util.List;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Het Vadsrijk, slice "economie": the client side. The screens (one payload, see {@link EcoPayloads}), the mount renderers
 * (GeckoLib) and the coin pop-up on the HUD (when the wallet changes: the coins with a guh face + the difference, 3 s).
 */
public final class EconomieClient {
    private static long munten = -1, verschil;
    private static long popTot;

    private EconomieClient() {
    }

    public static void init(IEventBus modBus) {
        EcoPayloads.schermOntvanger = EconomieClient::opScherm;
        EcoPayloads.muntenOntvanger = m -> {
            if (munten >= 0 && m.brons() != munten) {
                verschil = m.brons() - munten;
                popTot = System.currentTimeMillis() + 3000;
            }
            munten = m.brons();
        };
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(EcoSpul.HOEDHUPPER.get(), c -> new RijdierRenderer(c, "vr_eco_hoedhupper"));
            e.registerEntityRenderer(EcoSpul.KLODDERROS.get(), c -> new RijdierRenderer(c, "vr_eco_klodderros"));
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.HOTBAR, Guhs.id("vr_economie_munten"), EconomieClient::tekenMunten));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            munten = -1;
            popTot = 0;
        });
    }

    private static void opScherm(EcoPayloads.Scherm s) {
        Minecraft mc = Minecraft.getInstance();
        Screen nu = mc.screen;
        if (s.scherm().equals(EcoPayloads.DICHT)) {
            if (nu instanceof EcoScherm) {
                mc.setScreen(null);
            }
            return;
        }
        if (nu instanceof EcoScherm e && e.scherm.equals(s.scherm())
                && (!s.scherm().equals(EcoPayloads.WINKEL) || e.data.getStringOr("winkel", "").equals(s.data().getStringOr("winkel", "")))) {
            e.update(s.data(), s.items());
            return;
        }
        mc.setScreen(maak(s.scherm(), s.data(), s.items()));
    }

    private static Screen maak(String scherm, CompoundTag d, List<ItemStack> items) {
        return switch (scherm) {
            case EcoPayloads.SMID -> new SmidScherm(d, items);
            case EcoPayloads.MARKT -> new MarktScherm(d, items);
            case EcoPayloads.HANDEL -> new HandelScherm(d, items);
            case EcoPayloads.STAL -> new StalScherm(d, items);
            default -> new WinkelScherm(d, items);
        };
    }

    /** The coin pop-up above the hotbar, right side: the coin icons + "+12b" for 3 seconds after the wallet changed. */
    private static void tekenMunten(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Vadsrijk.in(mc.player) || mc.options.hideGui || munten < 0) {
            return;
        }
        long over = popTot - System.currentTimeMillis();
        if (over <= 0) {
            return;
        }
        Font f = mc.font;
        float a = Math.min(1f, over / 500f);
        int alpha = (int) (a * 255) << 24;
        int x = g.guiWidth() / 2 + 98, y = g.guiHeight() - 40;
        String d = (verschil >= 0 ? "+" : "-") + nl.juiced.guhs.feature.vadsrijk.kern.Spelers.munten(Math.abs(verschil));
        int bw = 22 + f.width(d);
        g.fill(x - 2, y - 3, x + bw, y + 13, (int) (a * 0xA0) << 24 | 0x3A1030);
        ItemStack munt = new ItemStack(Math.abs(verschil) >= 10000 ? EcoSpul.MUNT_GOUD.get() : Math.abs(verschil) >= 100 ? EcoSpul.MUNT_ZILVER.get()
                : EcoSpul.MUNT_BRONS.get());
        float bob = (float) Math.sin(System.currentTimeMillis() / 120.0) * 1.5f;
        g.pose().pushMatrix();
        g.pose().translate(x, y - 2 + bob);
        g.item(munt, 0, 0);
        g.pose().popMatrix();
        g.text(f, d, x + 19, y + 2, alpha | (verschil >= 0 ? 0xFFE070 : 0xFF8FB0), true);
    }

    /** A mount: GeckoLib model + texture by id (geckolib/models/entity/<id>.geo.json, textures/entity/<id>.png). */
    public static class RijdierRenderer extends GeoEntityRenderer<RijdierEntity, LivingEntityRenderState> {
        public RijdierRenderer(EntityRendererProvider.Context context, String id) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id(id)));
            this.shadowRadius = 0.6f;
        }
    }
}
