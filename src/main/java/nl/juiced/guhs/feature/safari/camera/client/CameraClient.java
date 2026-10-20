package nl.juiced.guhs.feature.safari.camera.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.safari.camera.CameraFeature;
import nl.juiced.guhs.feature.safari.camera.CameraPayloads;
import nl.juiced.guhs.feature.safari.camera.Foto;
import nl.juiced.guhs.feature.safari.camera.Lokaal;

/**
 * The camera on the client (S1): the viewfinder instead of the spyglass's round scope (the camera's scope texture), the
 * white flash when a photo is taken, and a little photo card at the top of the screen with the stars, the caption and
 * what it paid ("njeg, niks op de foto" when nothing was in the picture).
 */
public final class CameraClient {
    static final Identifier ZOEKER = Guhs.id("textures/misc/safari_zoeker.png");
    private static final int FLITS = 8, KAART = 90;
    private static long flitsTot = -1, kaartTot = -1;
    private static CameraPayloads.Uitslag laatste;

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterClientExtensionsEvent event) -> event.registerItem(new IClientItemExtensions() {
            @Override
            public Identifier getScopeOverlayTexture(ItemStack stack) {
                return ZOEKER;
            }
        }, CameraFeature.CAMERA.get()));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(Guhs.id("safari_camera"), CameraClient::teken));
        CameraPayloads.ontvanger = CameraClient::uitslag;
        Lokaal.bron = () -> Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getUUID();
        Foto.Teksten.bestaat = I18n::exists;
    }

    private static void uitslag(CameraPayloads.Uitslag u) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long nu = mc.level.getGameTime();
        flitsTot = nu + FLITS;
        kaartTot = nu + KAART;
        laatste = u;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.SPYGLASS_STOP_USING, 2.0f, 0.7f));
        if (!u.foto().isEmpty() && u.foto().getIntOr("Sterren", 1) > u.vorige()) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0f + 0.2f * u.foto().getIntOr("Sterren", 1), 0.6f));
        }
        SafariDexTab.vernieuwd();
    }

    private static void teken(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || laatste == null) {
            return;
        }
        long nu = mc.level.getGameTime();
        float pt = delta.getGameTimeDeltaPartialTick(false);
        if (nu < flitsTot) {
            float a = (flitsTot - nu - pt) / FLITS;
            int alpha = (int) (Math.max(0, Math.min(1, a)) * 220);
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), (alpha << 24) | 0xFFFFFF);
        }
        if (nu >= kaartTot || mc.options.hideGui) {
            return;
        }
        Font font = mc.font;
        CompoundTag f = laatste.foto();
        int w = 220, h = f.isEmpty() ? 22 : 46 + (laatste.munten() > 0 ? 10 : 0) + (laatste.missies() > 0 ? 10 : 0);
        int x = (g.guiWidth() - w) / 2, y = 8;
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFFF7B6CB);
        g.fill(x, y, x + w, y + h, 0xF0FFF8FB);
        if (f.isEmpty()) {
            g.centeredText(font, Component.translatable("gui.guhs.safari.foto.niks"), x + w / 2, y + 7, 0xFF7A2848);
            return;
        }
        int sterren = f.getIntOr("Sterren", 1);
        Component kop = Component.literal("★".repeat(sterren) + "☆".repeat(3 - sterren) + "  ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("entity.guhs.safari." + f.getStringOr("Soort", "")).withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_PURPLE));
        g.centeredText(font, kop, x + w / 2, y + 5, 0xFFFFFFFF);
        int ty = y + 17;
        for (var regel : font.split(Foto.onderschrift(f), w - 12)) {
            if (ty > y + 36) {
                break;
            }
            g.text(font, regel, x + (w - font.width(regel)) / 2, ty, 0xFF5A3A4A, false);
            ty += 10;
        }
        ty = y + 38;
        if (sterren > laatste.vorige()) {
            Component nieuw = Component.translatable(laatste.vorige() == 0 ? "gui.guhs.safari.foto.nieuw" : "gui.guhs.safari.foto.beter");
            g.centeredText(font, nieuw, x + w / 2, ty - 1, 0xFF2A9A58);
        }
        if (laatste.munten() > 0) {
            ty += 10;
            g.centeredText(font, Component.translatable("gui.guhs.safari.foto.munten", laatste.munten()), x + w / 2, ty, 0xFFD89A10);
        }
        if (laatste.missies() > 0) {
            ty += 10;
            g.centeredText(font, Component.translatable("gui.guhs.safari.foto.missie", laatste.missies()), x + w / 2, ty, 0xFFC0407A);
        }
    }

    private CameraClient() {
    }
}
