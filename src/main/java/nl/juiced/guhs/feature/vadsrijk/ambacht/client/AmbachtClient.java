package nl.juiced.guhs.feature.vadsrijk.ambacht.client;

import javax.annotation.Nullable;

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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
import nl.juiced.guhs.feature.vadsrijk.ambacht.GrondstofBlok;
import nl.juiced.guhs.feature.vadsrijk.ambacht.NodeSoort;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPayloads;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtXp;
import nl.juiced.guhs.feature.vadsrijk.ambacht.client.ClientPlekken.Plek;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;

/**
 * The Ambachten on the client: the book key {@link #BOEK} (default Y, Vadsrijk category; taken were R V G Z C U K J X N P B),
 * the screens, clicking a gathering spot (right-click: the {@link Minigame}; sneak + right-click: harvest without it), the soft
 * glitter of full spots in the craft colour (1.10.4: dimmer and rarer when your level is too low; rare spots golden), your bees
 * flying to their flowers, and the small HUD (food buff, rest, what you look at). 1.10.4: the spots are real shared blocks
 * ({@link GrondstofBlok}), clicked through the normal block target; only the hive stands are still drawn ({@link PlekTekenaar}).
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
        } else if (s.equals(nl.juiced.guhs.feature.vadsrijk.ambacht.Imkeren.WISSEL_SCHERM)) {
            mc.setScreen(new KorfWisselScherm(d, items));   // "Korf verplaatsen": your zone is full, take another hive away?
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

    /** True: the crosshair is on a gathering node block. */
    private static boolean opNode() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.hitResult instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK
                && mc.level.getBlockState(bh.getBlockPos()).getBlock() instanceof GrondstofBlok;
    }

    /** The spot you look at: the node block under the crosshair (1.10.4), else a drawn hive stand. */
    @Nullable
    static Plek doel(float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (opNode() && mc.hitResult instanceof BlockHitResult bh) {
            Plek p = ClientPlekken.opBlok(bh.getBlockPos());
            if (p != null) {
                return p;
            }
        }
        return ClientPlekken.bekeken(pt);
    }

    /** Right-click on a spot: the minigame (or a tap in it). */
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
        Plek p = doel(1f);
        if (p == null) {
            if (opNode()) {   // (a node the last sync did not mention yet: nothing to do, nothing else may happen)
                e.setCanceled(true);
                e.setSwingHand(false);
            }
            return;
        }
        e.setCanceled(true);
        e.setSwingHand(true);
        if (p.echt() && p.leeg()) {
            mc.gui.setOverlayMessage(Component.translatable("vadsrijk.ambacht.oogst.leeg"), false);
            return;
        }
        if (!p.lvOk()) {
            mc.gui.setOverlayMessage(Component.translatable("vadsrijk.ambacht.oogst.te_laag", Component.translatable(p.ambacht().naamKey()),
                    p.nodig()), false);
            return;
        }
        boolean korfZonderOogst = p.soort() == NodeSoort.KORF && (!p.eigen() || p.raten() <= 0);
        if (mc.player.isShiftKeyDown() || korfZonderOogst) {
            Minigame.stuur(p.id(), 0, true);
        } else {
            Minigame.start(p.id(), p.soort());   // (1.10.4: by spot kind, coral dives too)
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
            if (b.distSqr(pl.blockPosition()) > GLINSTER * GLINSTER) {
                continue;
            }
            if (p.echt() && !p.leeg()) {
                glinster(mc, p, t);
            }
            if (b.distSqr(pl.blockPosition()) > 40 * 40) {
                continue;
            }
            if (p.zeldzaam() && t % 4 == 0) {
                double y0 = p.soort() == NodeSoort.ERTS ? 1.0 : 0.3;
                mc.level.addParticle(ParticleTypes.END_ROD, true, false, b.getX() + R.nextDouble(), b.getY() + y0 + R.nextDouble() * 0.8, b.getZ() + R.nextDouble(),
                        0, 0.02, 0);
            }
            if (p.soort() == NodeSoort.VIS && !p.leeg() && t % 10 == 0) {
                mc.level.addParticle(ParticleTypes.BUBBLE_POP, b.getX() + 0.2 + R.nextDouble() * 0.6, b.getY() + 0.95, b.getZ() + 0.2 + R.nextDouble() * 0.6, 0, 0.02, 0);
            }
            if (p.soort() == NodeSoort.DUIK && !p.leeg() && t % 8 == 0) {
                mc.level.addParticle(ParticleTypes.BUBBLE, b.getX() + 0.3 + R.nextDouble() * 0.4, b.getY() + 0.5, b.getZ() + 0.3 + R.nextDouble() * 0.4, 0, 0.08, 0);
            }
            if (p.soort() == NodeSoort.KORF && p.eigen() && !p.bloemen().isEmpty() && t % 6 == 0) {
                // a bee flying from the hive to a flower (or back with nectar)
                BlockPos bl = p.bloemen().get(R.nextInt(p.bloemen().size()));
                boolean heen = R.nextBoolean();
                double x0 = heen ? b.getX() + 0.5 : bl.getX() + 0.5, y0 = heen ? b.getY() + 0.6 : bl.getY() + 0.5, z0 = heen ? b.getZ() + 0.5 : bl.getZ() + 0.5;
                double x1 = heen ? bl.getX() + 0.5 : b.getX() + 0.5, y1 = heen ? bl.getY() + 0.5 : b.getY() + 0.6, z1 = heen ? bl.getZ() + 0.5 : b.getZ() + 0.5;
                mc.level.addParticle(new DustParticleOptions(heen ? 0xFFD030 : 0xFFF090, 0.7f), x0, y0, z0, (x1 - x0) / 14, (y1 - y0) / 14 + 0.02, (z1 - z0) / 14);
            }
        }
    }

    /** How far the glitter of a full spot shows (blocks). */
    static final int GLINSTER = 48;

    /**
     * 1.10.4: the soft glitter of a full spot in its craft's colour ({@link Ambacht#kleur}, a little lighter): a mote every few
     * ticks drifting up; when your level is too low for it, rarer, smaller and greyer; a rare spot adds gold.
     */
    private static void glinster(Minecraft mc, Plek p, long t) {
        BlockPos b = p.pos();
        int elke = p.lvOk() ? 6 : 30;
        if ((t + (b.hashCode() & 63)) % elke != 0) {
            return;
        }
        int k = licht(p.ambacht().kleur & 0xFFFFFF, 0.3);
        if (!p.lvOk()) {
            k = grijs(k);
        }
        double y = switch (p.soort()) {
            case ERTS -> b.getY() + 1.05 + R.nextDouble() * 0.3;   // (above the vein in the floor)
            case VIS -> b.getY() + 0.95 + R.nextDouble() * 0.3;
            default -> b.getY() + 0.2 + R.nextDouble() * 0.9;
        };
        mc.level.addParticle(new DustParticleOptions(k, p.lvOk() ? 1.1f : 0.6f), true, false, b.getX() + 0.1 + R.nextDouble() * 0.8, y,
                b.getZ() + 0.1 + R.nextDouble() * 0.8, 0, 0.02, 0);
        if (p.zeldzaam()) {
            mc.level.addParticle(new DustParticleOptions(0xFFD84A, 1.3f), true, false, b.getX() + R.nextDouble(), y + 0.2, b.getZ() + R.nextDouble(),
                    0, 0.03, 0);
        }
    }

    private static int licht(int rgb, double f) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        return (int) (r + (255 - r) * f) << 16 | (int) (g + (255 - g) * f) << 8 | (int) (b + (255 - b) * f);
    }

    private static int grijs(int rgb) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, m = (r + g + b) / 3;
        return (r + m) / 2 << 16 | (g + m) / 2 << 8 | (b + m) / 2;
    }

    /** The HUD: the minigame bar, what you look at, the food buff and your rest. */
    private static void hud(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Vadsrijk.in(mc.player) || mc.options.hideGui) {
            return;
        }
        Minigame.teken(g, mc.font);
        if (!Minigame.bezig() && mc.screen == null) {
            Plek p = doel(dt.getGameTimeDeltaPartialTick(false));
            if (p != null && p.echt() && p.leeg()) {
                g.centeredText(mc.font, Component.translatable("vadsrijk.ambacht.hud.leeg", Component.translatable(p.soort().naamKey())), g.guiWidth() / 2,
                        g.guiHeight() / 2 + 14, 0xFFB8B8C8);
            } else if (p != null) {
                Component c = p.soort() == NodeSoort.KORF
                        ? Component.translatable(p.eigen() ? "vadsrijk.ambacht.hud.korf" : "vadsrijk.ambacht.hud.korf_vrij", p.raten(),
                                Component.translatable("item.guhs.vr_amb_" + p.honing()))
                        : Component.translatable(p.lvOk() ? "vadsrijk.ambacht.hud.plek" : "vadsrijk.ambacht.hud.te_laag", Component.translatable(p.soort().naamKey()),
                                p.tier(), p.nodig());
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
