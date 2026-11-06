package nl.juiced.guhs.feature.vadsrijk.economie.client;

import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.GeoEntityRenderer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
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
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
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
            for (RijdierSoort s : RijdierSoort.values()) {
                if (s.water()) {
                    continue;   // T-Z4BOOT: the Zeilbootje has its own renderer (zone4.boot.client.BootClient)
                }
                if (s.glijdt()) {   // U-TAPIJT: the flying carpet (banking, bob) on top of the normal mount renderer
                    e.registerEntityRenderer(EcoSpul.type(s), nl.juiced.guhs.feature.vadsrijk.tapijt.client.TapijtRenderer::new);
                    continue;
                }
                e.registerEntityRenderer(EcoSpul.type(s), c -> new RijdierRenderer(c, s));
            }
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.HOTBAR, Guhs.id("vr_economie_munten"), EconomieClient::tekenMunten));
        RijdierClient.init(modBus);
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
            case EcoPayloads.STAL -> new RijdierScherm(d, items);
            case EcoPayloads.STARTER -> new StarterScherm(d, items);
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

    private static final DataTicket<Integer> KLEUR = DataTicket.create("guhs_vr_rijdier_kleur", Integer.class);
    private static final DataTicket<Float> GROEI = DataTicket.create("guhs_vr_rijdier_groei", Float.class);
    /** T-RIJDIER: the look string (coat|pattern|tack) for the coat texture. */
    private static final DataTicket<String> LOOK = DataTicket.create("guhs_vr_rijdier_look", String.class);

    /**
     * A mount: GeckoLib model by id (geckolib/models/entity/vr_eco_<soort>.geo.json, generated by tools/features/vadsrijk_economie.py
     * and vadsrijk_rijdieren.py; swap the Pluimguh's look there). The Pluimguh has a texture per colour
     * (textures/entity/vr_eco_pluimguh_<kleur>.png). A freshly called mount pops in (grows with a little bounce, 7 ticks); a
     * silhouette preview (the mount screen) is drawn dark.
     */
    public static class RijdierRenderer extends GeoEntityRenderer<RijdierEntity, LivingEntityRenderState> {
        private final RijdierSoort soort;

        public RijdierRenderer(EntityRendererProvider.Context context, RijdierSoort soort) {
            super(context, new DefaultedEntityGeoModel<RijdierEntity>(Guhs.id("vr_eco_" + soort.id())) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    if (!soort.kleuren()) {   // T-RIJDIER: the coat colour (vacht)
                        return nl.juiced.guhs.feature.vadsrijk.rijdier.client.UiterlijkLaag.textuur(soort, state.getOrDefaultGeckolibData(LOOK, ""));
                    }
                    RijdierSoort.PluimKleur[] k = RijdierSoort.PluimKleur.values();
                    int i = state.getOrDefaultGeckolibData(KLEUR, 0);
                    return Guhs.id("textures/entity/vr_eco_" + soort.id() + "_" + k[Math.floorMod(i, k.length)].id() + ".png");
                }
            });
            this.soort = soort;
            this.shadowRadius = soort == RijdierSoort.PLUIMGUH ? 0.9f : 0.6f;
            withRenderLayer(new nl.juiced.guhs.feature.vadsrijk.rijdier.client.UiterlijkLaag(this, soort));   // T-RIJDIER: pattern + tack
        }

        @Override
        public void addRenderData(RijdierEntity r, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(KLEUR, r.kleur().ordinal());
            state.addGeckolibData(LOOK, r.uiterlijk());   // T-RIJDIER
            float t = r.voorbeeld ? 99 : r.tickCount + partialTick;
            // pop-in: 0.2 -> 1.12 -> 1.0
            float g = t >= 9 ? 1f : t < 6 ? 0.2f + 0.92f * (t / 6f) : 1.12f - 0.12f * ((t - 6) / 3f);
            state.addGeckolibData(GROEI, g);
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float w, float h) {
            float g = info.getOrDefaultGeckolibData(GROEI, 1f);
            super.scaleModelForRender(info, w * g, h * g);
        }

        @Override
        public int getRenderColor(RijdierEntity r, @Nullable Void related, float partialTick) {
            return r.silhouet ? 0xFF3A2C48 : super.getRenderColor(r, related, partialTick);
        }
    }
}
