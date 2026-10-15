package nl.juiced.guhs.feature.einde2.zij.client;

import java.util.List;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhKeys;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.zij.KrachtPayloads;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 zij, client side: the ONE bending key (Options &gt; Controls &gt; Guhs, default H): a tap uses the chosen power,
 * holding it opens the element wheel ({@link KrachtWiel}) of what you may use here; the icon of the chosen power next to
 * the hotbar with its cooldown; and walking on water (the lasting water power: the server says until when, the client
 * keeps the player on the surface; sneak to sink).
 */
public final class E2ZijClient {
    public static final KeyMapping KRACHT = new KeyMapping("key.guhs.einde2_kracht", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, GuhKeys.CATEGORY);
    /** Holding the key this many ticks opens the wheel. */
    static final int HOUD = 6;
    /** The colour of each element (frame of the icon, the wheel). */
    static final int[] KLEUR = {0xFFF2E6B8, 0xFF5AB4F0, 0xFF6FA84A, 0xFFF07032};

    @Nullable
    static String gekozen;
    private static int houd = -1;

    private E2ZijClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(KRACHT));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAbove(VanillaGuiLayers.HOTBAR, Guhs.id("einde2_zij_kracht"), E2ZijClient::hud));
        NeoForge.EVENT_BUS.addListener(E2ZijClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(E2ZijClient::onPlayerTick);
    }

    /** The powers the server says the player may use now. */
    static List<KrachtPayloads.Regel> krachten() {
        return KrachtPayloads.CLIENT.krachten;
    }

    /** The chosen power (the first one when nothing was chosen, or the choice is not possible here). */
    @Nullable
    static KrachtPayloads.Regel keuze() {
        List<KrachtPayloads.Regel> k = krachten();
        if (k.isEmpty()) {
            return null;
        }
        for (KrachtPayloads.Regel r : k) {
            if (r.id().equals(gekozen)) {
                return r;
            }
        }
        return k.get(0);
    }

    /** Ticks left of a power's cooldown, counted on from what the server sent. */
    static int rest(KrachtPayloads.Regel r) {
        Minecraft mc = Minecraft.getInstance();
        long verstreken = mc.level == null ? 0 : mc.level.getGameTime() - KrachtPayloads.CLIENT.ontvangen;
        return (int) Math.max(0, r.rest() - verstreken);
    }

    static ItemStack icoon(KrachtPayloads.Regel r) {
        Identifier id = Identifier.tryParse(r.icoon());
        return id == null || !BuiltInRegistries.ITEM.containsKey(id) ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(id));
    }

    /** The chapter slices that may name their story powers {@code gui.guhs.einde2_<slice>.kracht.<id>} (h2_water does). */
    private static final String[] HOOFDSTUKKEN = {"h1_lucht", "h2_water", "h3_aarde", "h4_vuur"};

    /**
     * A power's name: {@code gui.guhs.einde2_kracht.<id>}, else a chapter's {@code gui.guhs.einde2_<slice>.kracht.<id>},
     * else the name of its icon.
     */
    static Component naam(KrachtPayloads.Regel r) {
        String key = "gui.guhs.einde2_kracht." + r.id();
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        for (String h : HOOFDSTUKKEN) {
            String k = "gui.guhs.einde2_" + h + ".kracht." + r.id();
            if (I18n.exists(k)) {
                return Component.translatable(k);
            }
        }
        return icoon(r).getHoverName();
    }

    static void kies(String id) {
        gekozen = id;
    }

    static void gebruik(KrachtPayloads.Regel r) {
        ClientPacketDistributor.sendToServer(new KrachtPayloads.Gebruik(r.id()));
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            houd = -1;
            return;
        }
        boolean vrij = mc.screen == null && !Cutscenes.bezig(mc.player);
        if (!vrij) {
            while (KRACHT.consumeClick()) {
                // (a press while a screen is open means nothing here)
            }
            if (!(mc.screen instanceof KrachtWiel)) {
                houd = -1;
            }
            return;
        }
        while (KRACHT.consumeClick()) {
            if (houd < 0) {
                houd = 0;
            }
        }
        if (KRACHT.isDown()) {
            if (houd >= 0 && ++houd == HOUD && !krachten().isEmpty()) {
                mc.setScreen(new KrachtWiel());
                houd = -1;
            }
            return;
        }
        if (houd >= 0) {
            houd = -1;
            KrachtPayloads.Regel r = keuze();
            if (r == null) {
                mc.player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_zij.kracht.geen"));
            } else if (rest(r) > 0) {
                mc.player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_zij.kracht.wacht", naam(r)));
            } else {
                gebruik(r);
            }
        }
    }

    /** Walking on water: before the player moves, keep them on the surface (and let them jump from it). */
    private static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer p) || p != Minecraft.getInstance().player || p.isSpectator() || p.isShiftKeyDown()
                || p.isPassenger() || p.getAbilities().flying) {
            return;
        }
        long tot = KrachtPayloads.CLIENT.waterTot;
        if (tot <= 0 || p.level().getGameTime() > tot) {
            return;
        }
        BlockPos onder = BlockPos.containing(p.getX(), p.getY() - 0.05, p.getZ());
        boolean voetenNat = p.level().getFluidState(BlockPos.containing(p.getX(), p.getY() + 0.15, p.getZ())).is(FluidTags.WATER);
        boolean waterOnder = p.level().getFluidState(onder).is(FluidTags.WATER);
        Vec3 v = p.getDeltaMovement();
        if (voetenNat && !p.isUnderWater()) {
            p.setDeltaMovement(v.x, Math.max(v.y, 0.18), v.z);
        } else if (waterOnder && !voetenNat && v.y <= 0.0) {
            double top = onder.getY() + 1.0;
            p.setDeltaMovement(v.x, 0.0, v.z);
            if (p.getY() < top) {
                p.setPos(p.getX(), top, p.getZ());
            }
            p.setOnGround(true);
            p.resetFallDistance();
            if (p.tickCount % 4 == 0 && v.horizontalDistanceSqr() > 0.002) {
                p.level().addParticle(ParticleTypes.SPLASH, p.getX(), top + 0.05, p.getZ(), 0, 0.05, 0);
            }
        }
    }

    /** The chosen power next to the hotbar (on the side of the off hand's opposite), with its cooldown. */
    private static void hud(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator() || Cutscenes.bezig(mc.player)) {
            return;
        }
        KrachtPayloads.Regel r = keuze();
        if (r == null) {
            return;
        }
        int x = g.guiWidth() / 2 + 91 + 8, y = g.guiHeight() - 23;
        int kleur = KLEUR[Math.floorMod(r.element(), KLEUR.length)];
        g.fill(x - 1, y - 1, x + 23, y + 23, kleur);
        g.fill(x, y, x + 22, y + 22, 0xE0201418);
        g.item(icoon(r), x + 3, y + 3);
        int rest = rest(r);
        if (rest > 0 && r.afkoel() > 0) {
            int h = Math.max(1, Math.round(22f * rest / r.afkoel()));
            g.fill(x, y + 22 - h, x + 22, y + 22, 0xA0000000);
            g.centeredText(mc.font, String.valueOf((rest + 19) / 20), x + 11, y + 7, 0xFFFFFFFF);
        }
        long water = KrachtPayloads.CLIENT.waterTot - (mc.level == null ? 0 : mc.level.getGameTime());
        if (water > 0) {
            int w = (int) Math.round(22.0 * water / nl.juiced.guhs.feature.einde2.zij.ZijKrachten.WATERLOOP_TICKS);
            g.fill(x, y + 24, x + Math.min(22, w), y + 26, KLEUR[Einde2Ids.Element.WATER.ordinal()]);
        }
    }
}
