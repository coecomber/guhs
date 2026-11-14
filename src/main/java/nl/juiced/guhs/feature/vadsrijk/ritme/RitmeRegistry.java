package nl.juiced.guhs.feature.vadsrijk.ritme;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;

/**
 * U-LEVEN-RITME (Guhs 1.10.0, round U, FEEDBACK_RONDE4 V7 "levendigheid"): the day/night rhythm of the towns ({@link Rooster}:
 * shops close, the night bell; {@link Nachtleven}: sleepers in the beds, owls; {@link Nachtmarkt}), the weather timetable per zone
 * ({@link WeerKlok}, drawn by {@code client.WeerBeeld}), reactions to your emotes + the knuffelmeter ({@link Knuffelmeter}) and the
 * town newspaper with chores ({@link Pluizenblaadje}). Commands {@link RitmeCommando} ({@code /guhs vadsrijk ritme ...}).
 * See NOTITIES_U_LEVEN_RITME.md / SPIEK_U_LEVEN_RITME.md.
 */
public final class RitmeRegistry {
    private RitmeRegistry() {
    }

    public static void register(IEventBus modBus) {
        Knuffelmeter.titels();
        Nachtmarkt.init();
        Pluizenblaadje.init();
        // chores first (citizens without quest roles only), then a sleeping shopkeeper (opens its shop); both never block quests
        QuestMotor.KLIK_VOOR.add(Pluizenblaadje::klikVoor);
        QuestMotor.KLIK_VOOR.add(Rooster::klikVoor);
        RijkGuh.STIL = Rooster::slaapt;   // U-LEVEN-RITME hook: a sleeping guh doesn't wave, idle-emote or stroll
        nl.juiced.guhs.feature.vadsrijk.hud.Emotes.NA_EMOTE.add(Knuffelmeter::opEmote);   // U-LEVEN-RITME hook in U-HUD's emotes
        nl.juiced.guhs.feature.vadsrijk.menu.MenuServer.KARAKTER_EXTRA.add(Knuffelmeter::karakter);
        nl.juiced.guhs.world.TikWacht.post("RitmeRegistry:41", e -> {
            Rooster.tick(e.getServer());
            Nachtleven.tick(e.getServer());
            Nachtleven.houSlapend(e.getServer());
            Nachtmarkt.tick(e.getServer());
            Knuffelmeter.tick(e.getServer());
            Pluizenblaadje.tick(e.getServer());
            Pluizenblaadje.tickBorden(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (PlayerInteractEvent.EntityInteract e) -> {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer p) || !e.getTarget().entityTags().contains(Ritme.TAG)) {
                return;
            }
            Entity t = e.getTarget();
            boolean onsDing = e.getHand() != InteractionHand.MAIN_HAND || switch (t) {
                case Interaction i -> Rooster.belKlik(p, i) || Pluizenblaadje.klik(p, i);
                default -> Nachtmarkt.klik(p, t) || Nachtleven.klik(p, t);
            };
            if (onsDing) {
                e.setCancellationResult(InteractionResult.SUCCESS);
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.loadedFromDisk() && e.getEntity().entityTags().contains(Ritme.TAG)) {
                e.setCanceled(true);   // (a sign, bell, sleeper, owl or lampion left behind by an unloaded chunk: the owner makes it again)
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> wis());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> wis());
        NeoForge.EVENT_BUS.addListener(RitmeCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
        RitmePayloads.register(registrar);
    }

    static void wis() {
        Rooster.wis();
        Rooster.forceer = null;
        Bedden.wis();
        Nachtleven.wis();
        Nachtmarkt.wis();
        Knuffelmeter.wis();
        Pluizenblaadje.wis();
        Pluizenblaadje.dagVerschuiving = 0;
    }
}
