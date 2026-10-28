package nl.juiced.guhs.feature.guhpixel.gezel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;
import nl.juiced.guhs.feature.guhpixel.GuhpixelFeature;
import nl.juiced.guhs.feature.guhpixel.LobbyNpcs;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * Guhpixel slice "gezel" (1.8.0): the Gezelschapshoek of the lobby, three games by three NPCs on new lobby anchors.
 * <ul>
 *   <li>{@link KussenSessie} Kussengevecht (Kussenbaas-guh at {@link LobbyPlek#SPEL_KUSSEN}): an arena game, alone or with
 *   the players standing near the Kussenbaas, guh-bots fill up;</li>
 *   <li>Wie is het (Spelletjesguh at {@link LobbyPlek#SPEL_WIEISHET}) and Guh op een rij (Rijtjesguh at
 *   {@link LobbyPlek#SPEL_OPEENRIJ}): board games on a screen ({@link BordSpel}), against the guh-bot or another player.</li>
 * </ul>
 * Resources: tools/features/guhpixel_gezel.py; tests: PxGezelGameTests (batch px_gezel).
 */
public final class GezelSlice {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredItem<Item> KUSSEN = ITEMS.registerItem("kussengevecht_kussen", GuhpixelFeature.LoreItem::new,
            () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SCHIJFJE = ITEMS.registerItem("opeenrij_schijfje", GuhpixelFeature.LoreItem::new,
            () -> new Item.Properties());
    public static final DeferredItem<Item> KAARTJE = ITEMS.registerItem("wieishet_kaartje", GuhpixelFeature.LoreItem::new,
            () -> new Item.Properties());

    static final int SPELEN = 0, UITLEG = 1;
    /** How near the Kussenbaas other players must stand to join a round. */
    static final double SAMEN = 6.0;

    private static final NpcRole KUSSENBAAS = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            Praat.open(player, npc, null, "quest.guhs.gezel.kussenbaas.hallo", new Object[]{},
                    new Praat.Optie(SPELEN, "quest.guhs.gezel.kussenbaas.optie.spelen"), new Praat.Optie(UITLEG, "quest.guhs.gezel.kussenbaas.optie.uitleg"));
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
            if (optie == SPELEN) {
                Praat.sluit(player);
                List<ServerPlayer> samen = new ArrayList<>();
                samen.add(player);
                for (ServerPlayer p : player.level().getServer().getPlayerList().getPlayers()) {
                    if (p != player && p.level() == npc.level() && p.distanceTo(npc) <= SAMEN && Sessies.van(p) == null && samen.size() < KussenSessie.SPEL.maxSpelers()) {
                        samen.add(p);
                    }
                }
                Sessies.start(KussenSessie.SPEL, samen, new CompoundTag());
            } else if (optie == UITLEG) {
                Praat.open(player, npc, null, "quest.guhs.gezel.kussenbaas.uitleg", new Object[]{(int) SAMEN},
                        new Praat.Optie(SPELEN, "quest.guhs.gezel.kussenbaas.optie.spelen"));
            }
        }
    };

    private static NpcRole bordRol(String spel) {
        return (npc, player) -> BordSpel.open(player, spel);
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        Sessies.registreer(KussenSessie.SPEL);
        NpcRollen.zet(GuhNpcEntity.Kind.KUSSENBAAS_GUH, KUSSENBAAS);
        NpcRollen.zet(GuhNpcEntity.Kind.WIEISHET_GUH, bordRol(BordSpel.WIEISHET));
        NpcRollen.zet(GuhNpcEntity.Kind.OPEENRIJ_GUH, bordRol(BordSpel.OPEENRIJ));
        LobbyNpcs.registreer(LobbyPlek.SPEL_KUSSEN, GuhNpcEntity.Kind.KUSSENBAAS_GUH, null, server -> LobbyNpcs.spelersRegel(KussenSessie.ID, 4));
        LobbyNpcs.registreer(LobbyPlek.SPEL_WIEISHET, GuhNpcEntity.Kind.WIEISHET_GUH, null,
                server -> Component.translatable("gui.guhs.gezel.npc.wieishet").withStyle(ChatFormatting.GRAY));
        LobbyNpcs.registreer(LobbyPlek.SPEL_OPEENRIJ, GuhNpcEntity.Kind.OPEENRIJ_GUH, null,
                server -> Component.translatable("gui.guhs.gezel.npc.opeenrij").withStyle(ChatFormatting.GRAY));

        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, GezelSlice::opAanval);
        NeoForge.EVENT_BUS.addListener(GezelSlice::opSchade);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> BordSpel.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            BordSpel.VAN.clear();
            BordSpel.WACHT.clear();
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                BordSpel.stop(p, true);
            }
        });
    }

    /** A pillow hit in the Kussengevecht: handled here, before Guhpixel's rules cancel every attack. */
    static void opAanval(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && Sessies.van(p) instanceof KussenSessie sessie) {
            sessie.klap(p, event.getTarget());
            event.setCanceled(true);
        }
    }

    /** The guh-bots never get hurt (not even by the fall into the cushion pit). */
    static void opSchade(LivingIncomingDamageEvent event) {
        if (event.getEntity().entityTags().contains(KussenSessie.BOT_TAG) && Guhpixel.in(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
        GezelPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KUSSEN.get()));
    }

    /** (Tests) the game this player is in. */
    static Sessie sessie(ServerPlayer p) {
        return Sessies.van(p);
    }

    private GezelSlice() {
    }
}
