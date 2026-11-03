package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.erfenis.ErfenisMomenten;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * 1.9 De Erfenis, the Sherlock story (erfenis-voorstellen/sherlock.md + rounds 8, 13, 16 of ontwerp-guhs190-erfenis.md).
 * <ul>
 *   <li>story: {@link Sherlock} (API + flow), {@link Zaken}/{@link Zaak} (the eleven cases), {@link Voortgang} in {@link SherlockData}</li>
 *   <li>world: {@link Sporen} (clues + vergrootglas), {@link Cast} (the characters), {@link Gesprek} + {@link Regels} (dialogues),
 *   {@link Guhrrit} (also for the tutorial), {@link Rechtbank} + {@link SherlockScenes} (the finale)</li>
 *   <li>client: client/SherlockClient, client/BureauScherm (Speurboekje + speurbord); network {@link SherlockPayloads}</li>
 * </ul>
 * Resources: tools/features/erfenis_sherlock.py. Commands: /guhs erfenis sherlock ... ({@link SherlockCommando}).
 */
public final class SherlockFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredItem<Item> VERGROOTGLAS = ITEMS.registerItem("sherlock_vergrootglas", Vergrootglas::new, () -> new Item.Properties());

    private SherlockFeature() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        SherlockScenes.laad();
        NeoForge.EVENT_BUS.addListener(SherlockCommando::register);
        NeoForge.EVENT_BUS.addListener(Sherlock::tick);
        NeoForge.EVENT_BUS.addListener(Cast::tick);
        NeoForge.EVENT_BUS.addListener(Rechtbank::tick);
        NeoForge.EVENT_BUS.addListener(Cast::opLaden);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Sherlock::opInteractie);
        // the dimension cancels every click on a block: the glass still works when you look at the ground
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerInteractEvent.RightClickBlock e) -> {
            if (e.getHand() == InteractionHand.MAIN_HAND && e.getEntity() instanceof ServerPlayer p && e.getItemStack().getItem() instanceof Vergrootglas) {
                Sherlock.speur(p);
            }
        });
        ErfenisMomenten.luister(Sherlock::opMoment);
        Praat.luister(Sherlock.GETUIGE, Sherlock::opGetuigeAntwoord);
        Praat.luister(Sherlock.VADSON, Sherlock::opVadsonAntwoord);
        Praat.luister(Sherlock.TERUG_NAAR_BORD, (p, spreker, optie) -> {
            if (optie < 0) {
                Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
                Sherlock.stuurBord(p, v.actief == null ? null : Zaken.van(v.actief));
            }
        });
        Praat.luister(Gesprek.VERHAAL, Rechtbank::opVerhaalAntwoord);
        Praat.luister(Rechtbank.BEZWAAR, Rechtbank::opBewijs);
        Praat.luister(Rechtbank.BEZWAAR_FOUT, (p, spreker, optie) -> {
            if (optie < 0) {
                Rechtbank.kies(p);
            }
        });
        NpcRollen.zet(Kind.SHERLOCK_GUH, Sherlock.rol("sherlock"));
        NpcRollen.zet(Kind.DOKTER_VADSON, Sherlock.rol("vadson"));
        NpcRollen.zet(Kind.MAKELAAR_GUHRRIT, Sherlock.rol("guhrrit"));
        NpcRollen.zet(Kind.GUHRRIT_VAKANTIE, Sherlock.rol("guhrrit"));
        NpcRollen.zet(Kind.MORRIS_MIKA, Sherlock.rol("morris"));
        NpcRollen.zet(Kind.PIM_MIKA, Sherlock.rol("pim"));
        NpcRollen.zet(Kind.OEHOE, Sherlock.rol("oehoe"));
        NpcRollen.zet(Kind.KNABBELHAMER, Sherlock.rol("knabbelhamer"));
        // the titles (feature/titels): earned with the case
        ChatFormatting[] kleuren = {ChatFormatting.GOLD, ChatFormatting.GRAY, ChatFormatting.DARK_AQUA, ChatFormatting.GREEN, ChatFormatting.YELLOW,
                ChatFormatting.LIGHT_PURPLE, ChatFormatting.AQUA};
        String[] iconen = {"minecraft:egg", "minecraft:cobweb", "minecraft:book", "minecraft:oak_fence", "minecraft:glow_berries", "minecraft:feather",
                "guhs:sherlock_vergrootglas"};
        int i = 0;
        for (Zaak z : Zaken.ALLE) {
            String titel = Zaken.TITELS.get(z.id());
            if (titel == null) {
                continue;
            }
            String zaak = z.id();
            Titels.registreer(new Titels.Titel(titel, "gui.guhs.titels.naam." + titel, kleuren[i], iconen[i],
                    p -> {
                        Voortgang v = SherlockData.als(p.level().getServer(), p.getUUID());
                        return v != null && v.isOpgelost(zaak);
                    }));
            i++;
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
        SherlockPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(VERGROOTGLAS.get()));
    }
}
