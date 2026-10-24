package nl.juiced.guhs.feature.safari.camera;

import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * Op Safari! step 2, slice S1: the safari camera, the photos and the Safari tab of the Guhdex (design 1.7/1.8).
 * <ul>
 *   <li>{@link SafariCameraItem} (hold = zoom with a photo frame, let go = click), judged by {@link Fotograaf}: stars,
 *   coins, the day's {@link Missies};</li>
 *   <li>{@link SafariFotoItem}: a printed dex photo;</li>
 *   <li>the client side (camera.client): the frame, the flash and the photo card, and the Guhdex tab SafariDexTab.</li>
 * </ul>
 * The guh numbers of the Guhdex (F3b) live in quest.GuhNummers.
 */
public final class CameraFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredItem<SafariCameraItem> CAMERA = ITEMS.registerItem("safaricamera", SafariCameraItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SafariFotoItem> FOTO = ITEMS.registerItem("safari_foto", SafariFotoItem::new,
            () -> new Item.Properties().stacksTo(16));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CameraFeature::onDimensie);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CameraFeature::onLogin);
    }

    /** 1.7.0: whoever comes into the safari park without a camera gets a new one (lost, sold, left in a chest...). */
    private static void onDimensie(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo().equals(nl.juiced.guhs.feature.safari.Safari.DIM) && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) {
            geefCamera(p);
        }
    }

    private static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p && p.level().dimension().equals(nl.juiced.guhs.feature.safari.Safari.DIM)) {
            geefCamera(p);
        }
    }

    static void geefCamera(net.minecraft.server.level.ServerPlayer p) {
        if (p.isSpectator() || p.getInventory().hasAnyMatching(s -> s.getItem() instanceof SafariCameraItem)) {
            return;
        }
        ItemStack camera = new ItemStack(CAMERA.get());
        if (!p.getInventory().add(camera)) {
            p.drop(camera, false);
        }
        p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.safari.camera.nieuw"));
    }

    public static void payloads(net.neoforged.neoforge.network.registration.PayloadRegistrar registrar) {
        CameraPayloads.payloads(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(CAMERA.get()));
    }

    private CameraFeature() {
    }
}
