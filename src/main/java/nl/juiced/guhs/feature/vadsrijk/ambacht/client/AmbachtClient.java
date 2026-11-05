package nl.juiced.guhs.feature.vadsrijk.ambacht.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPayloads;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtXp;
import nl.juiced.guhs.feature.vadsrijk.ambacht.client.ClientPlekken.Plek;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;

/**
 * The Ambachten on the client: the book key {@link #BOEK} (default Y, Vadsrijk category; taken were R V G Z C U K J X N P B),
 * the screens, drawing your own spots ({@link PlekTekenaar}), clicking them (right-click: the {@link Minigame}; sneak + right-click:
 * harvest without it), the glitter of rare spots and your bees flying to their flowers, and the small HUD (food buff, rest, what
 * you look at).
 */
public final class AmbachtClient {
    public static final KeyMapping BOEK = new KeyMapping("key.guhs.vr_ambachten", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, KernClient.CATEGORIE);
    public static final String TAB = "ambachten";
    private static final RandomSource R = RandomSource.create();

    private AmbachtClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(BOEK));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.CROSSHAIR, Guhs.id("vr_ambacht_hud"), AmbachtClient::hud));
        AmbachtPayloads.syncOntvanger = s -> Minecraft.getInstance().execute(() -> ClientPlekken.zet(s.data()));
        AmbachtPayloads.schermOntvanger = s -> Minecraft.getInstance().execute(() -> scherm(s.data(), s.items()));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterTranslucentBlocks e) -> PlekTekenaar.teken(e));
        NeoForge.EVENT_BUS.addListener(AmbachtClient::klik);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            ClientPlekken.wis();
            Minigame.stop();
            PlekTekenaar.vergeet();
        });
        nl.juiced.guhs.feature.vadsrijk.client.CollectieTabs.registreer(TAB, 40, () -> Component.translatable("vadsrijk.ambacht.titel"), AmbachtClient::open);
    }

    public static void open() {
        CompoundTag a = new CompoundTag();
        a.putString("tab", "overzicht");
        ClientPacketDistributor.sendToServer(new AmbachtPayloads.Doe("boek", a));
    }

    private static void scherm(CompoundTag d, java.util.List<net.minecraft.world.item.ItemStack> items) {
        Minecraft mc = Minecraft.getInstance();
        String s = d.getStringOr("scherm", "boek");
        if (s.equals("werkplaats")) {
            if (mc.screen instanceof WerkplaatsScherm w) {
                w.update(d, items);
            } else {
                mc.setScreen(new WerkplaatsScherm(d, items));
            }
        } else if (mc.screen instanceof AmbachtScherm b) {
            b.update(d, items);
        } else {
            mc.setScreen(new AmbachtScherm(d, items));
        }
    }

    private static boolean hier() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && Vadsrijk.in(mc.player) && mc.screen == null;
    }

    /** Right-click on one of your spots: the minigame (or a tap in it). */
    private static void klik(InputEvent.InteractionKeyMappingTriggered e) {
        if (!hier() || !e.isUseItem()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (Minigame.bezig()) {
            if (Minigame.tik()) {
                e.setCanceled(true);
                e.setSwingHand(false);
            }
            return;
        }
        Plek p = ClientPlekken.bekeken(1f);
        if (p == null) {
            return;
        }
        e.setCanceled(true);
        e.setSwingHand(true);
        if (!p.lvOk()) {
            mc.gui.setOverlayMessage(Component.translatable("vadsrijk.ambacht.oogst.te_laag", Component.translatable(p.ambacht().naamKey()),
                    AmbachtXp.tierLevel(p.tier())), false);
            return;
        }
        boolean korfZonderOogst = p.ambacht() == Ambacht.IMKEREN && (!p.eigen() || p.raten() <= 0);
        if (mc.player.isShiftKeyDown() || korfZonderOogst) {
            Minigame.stuur(p.id(), 0, true);
        } else {
            Minigame.start(p.id(), p.ambacht());
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer pl = mc.player;
        if (pl == null || mc.level == null || !Vadsrijk.in(pl)) {
            Minigame.stop();
            return;
        }
        while (BOEK.consumeClick()) {
            if (mc.screen == null) {
                open();
            }
        }
        Minigame.tick();
        long t = mc.level.getGameTime();
        for (Plek p : ClientPlekken.PLEKKEN) {
            BlockPos b = p.pos();
            if (b.distSqr(pl.blockPosition()) > 40 * 40) {
                continue;
            }
            if (p.zeldzaam() && t % 4 == 0) {
                mc.level.addParticle(ParticleTypes.END_ROD, b.getX() + R.nextDouble(), b.getY() + 0.3 + R.nextDouble() * 0.8, b.getZ() + R.nextDouble(), 0, 0.02, 0);
            }
            if (p.ambacht() == Ambacht.VISSEN && !p.leeg() && t % 10 == 0) {
                mc.level.addParticle(ParticleTypes.BUBBLE_POP, b.getX() + 0.2 + R.nextDouble() * 0.6, b.getY() + 0.95, b.getZ() + 0.2 + R.nextDouble() * 0.6, 0, 0.02, 0);
            }
            if (p.ambacht() == Ambacht.IMKEREN && p.eigen() && !p.bloemen().isEmpty() && t % 6 == 0) {
                // a bee flying from the hive to a flower (or back with nectar)
                BlockPos bl = p.bloemen().get(R.nextInt(p.bloemen().size()));
                boolean heen = R.nextBoolean();
                double x0 = heen ? b.getX() + 0.5 : bl.getX() + 0.5, y0 = heen ? b.getY() + 0.6 : bl.getY() + 0.5, z0 = heen ? b.getZ() + 0.5 : bl.getZ() + 0.5;
                double x1 = heen ? bl.getX() + 0.5 : b.getX() + 0.5, y1 = heen ? bl.getY() + 0.5 : b.getY() + 0.6, z1 = heen ? bl.getZ() + 0.5 : b.getZ() + 0.5;
                mc.level.addParticle(new DustParticleOptions(heen ? 0xFFD030 : 0xFFF090, 0.7f), x0, y0, z0, (x1 - x0) / 14, (y1 - y0) / 14 + 0.02, (z1 - z0) / 14);
            }
        }
    }

    /** The HUD: the minigame bar, what you look at, the food buff and your rest. */
    private static void hud(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Vadsrijk.in(mc.player) || mc.options.hideGui) {
            return;
        }
        Minigame.teken(g, mc.font);
        if (!Minigame.bezig() && mc.screen == null) {
            Plek p = ClientPlekken.bekeken(dt.getGameTimeDeltaPartialTick(false));
            if (p != null) {
                Component c = p.ambacht() == Ambacht.IMKEREN
                        ? Component.translatable(p.eigen() ? "vadsrijk.ambacht.hud.korf" : "vadsrijk.ambacht.hud.korf_vrij", p.raten(),
                                Component.translatable("item.guhs.vr_amb_" + p.honing()))
                        : Component.translatable(p.lvOk() ? "vadsrijk.ambacht.hud.plek" : "vadsrijk.ambacht.hud.te_laag", Component.translatable(p.ambacht().naamKey()),
                                p.tier(), AmbachtXp.tierLevel(p.tier()));
                g.centeredText(mc.font, c, g.guiWidth() / 2, g.guiHeight() / 2 + 14, p.lvOk() ? 0xFFFFE8F4 : 0xFFFF9090);
                if (p.zeldzaam()) {
                    g.centeredText(mc.font, Component.translatable("vadsrijk.ambacht.hud.zeldzaam"), g.guiWidth() / 2, g.guiHeight() / 2 + 25, 0xFFFFD84A);
                }
            }
        }
        int y = g.guiHeight() - 62;
        long sinds = (System.currentTimeMillis() - ClientPlekken.ontvangen) / 1000;
        if (!ClientPlekken.buff.isEmpty()) {
            long over = Math.max(0, ClientPlekken.buffOver - sinds);
            g.text(mc.font, Component.translatable("vadsrijk.ambacht.hud.buff", Component.translatable("item.guhs.vr_amb_eten_" + ClientPlekken.buff), over / 60, over % 60),
                    4, y, 0xFFFFC8A0, true);
            y -= 10;
        }
        if (ClientPlekken.gezellig > 0) {
            long over = Math.max(0, ClientPlekken.gezellig - sinds);
            g.text(mc.font, Component.translatable("vadsrijk.ambacht.hud.gezellig", over / 60), 4, y, 0xFFFF9AD8, true);
            y -= 10;
        }
        if (ClientPlekken.uitgerust > 0 && !ClientPlekken.PLEKKEN.isEmpty()) {
            g.text(mc.font, Component.translatable("vadsrijk.ambacht.hud.uitgerust", ClientPlekken.uitgerust / 60), 4, y, 0xFFA0E0FF, true);
        }
    }
}
