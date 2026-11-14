package nl.juiced.guhs.feature.vadsrijk.opslag;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;

/**
 * T-OPSLAG (Guhs 1.10.0, night round T): virtual storage + HUD additions. VOORSTEL_ROADMAP §6.1, §6.2 and decision R5.
 * <ul>
 *   <li>{@link Knapzak}: extra inventory tabs per character (key I);</li>
 *   <li>{@link Materiaaltas}: the automatic, unlimited material bag (account-wide);</li>
 *   <li>{@link Bank}: the account-wide vault at a banker in every hub town ({@code Bank.bankier(npcId)});</li>
 *   <li>{@link HudBron} + {@link Gietbalk}: buff/debuff row, target frame, cast bars (client: {@code opslag.client}).</li>
 * </ul>
 * All storage is one player attachment ({@link OpslagData}). Commands: {@link OpslagCommando} ({@code /guhs vadsrijk opslag ...}).
 */
public final class OpslagRegistry {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Guhs.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Guhs.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<OpslagData>> DATA = ATTACHMENTS.register("vr_t_opslag",
            () -> AttachmentType.serializable(OpslagData::new).copyOnDeath().build());
    public static final DeferredHolder<MenuType<?>, MenuType<OpslagMenu>> OPSLAG_MENU = MENUS.register("vr_t_opslag",
            () -> IMenuTypeExtension.create(OpslagMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MateriaalMenu>> TAS_MENU = MENUS.register("vr_t_opslag_tas",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new MateriaalMenu(id, inv, null, null)));

    private OpslagRegistry() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        MENUS.register(modBus);
        // the first materials (every slice adds its own: Materiaaltas.registreer / regel / the item tag guhs:vr_materiaal)
        Materiaaltas.regel(s -> s.is(EcoSpul.VERBETERSTEENTJE.get()) || s.is(EcoSpul.HERROLKNABBEL.get()) || s.is(EcoSpul.PLUISDRAAD.get()));
        Materiaaltas.regel(s -> s.getItem() instanceof Kristallen.KristalItem);
        Bank.init();
        AmbachtTas.koppel();   // T-INT: t-ambacht's MateriaalTas.BRON -> this materiaaltas
        HudBron.init();
        Gietbalk.BRONNEN.add(p -> {   // kern's Losknuffelen (U): its 3 s channel gets a cast bar
            long b = nl.juiced.guhs.feature.vadsrijk.kern.veilig.Losknuffelen.kanaalBegin(p.getUUID());
            return b < 0 ? null : new Gietbalk.Kanaal("vadsrijk.opslag.giet.losknuffelen", b,
                    nl.juiced.guhs.feature.vadsrijk.kern.veilig.Losknuffelen.KANAAL_TICKS, 0xFF9AD5);
        });
        NeoForge.EVENT_BUS.addListener(Materiaaltas::opraap);
        nl.juiced.guhs.world.TikWacht.post("OpslagRegistry:59", e -> HudBron.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> HudBron.vergeet(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(OpslagCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
        OpslagPayloads.register(registrar);
    }
}
