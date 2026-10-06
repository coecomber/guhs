package nl.juiced.guhs.feature.ringh3;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Rustpunten;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * bbq2 (ring-h3): what the mine does with a player who is in it ({@link #tick}, every tick; most of it five times a second):
 * <ul>
 *   <li><b>the story</b>, step by step and only for a player whose own story is there ({@link Ring#aanZet}): arriving on the
 *       west forecourt shows the narrator card (0 -> 1); walking into the well room plays the scene with the bucket (3 -> 4);
 *       walking into the great hall wakes that player's Barbecuerog ({@link Achtervolging}); reaching the east bank plays the
 *       bridge scene (5 -> 6). The riddles in between are {@link Raadsels}, the last step is Araguh outside ({@link Rollen});</li>
 *   <li><b>the doors</b> ({@link Deuren}) and the bridge that is broken for one player ({@link Brug});</li>
 *   <li><b>falling</b>: whoever drops into the chasm is caught long before the coals and put back at their rest fire
 *       ({@link #terug}); inside the mine a fall never hurts (the damage is cancelled).</li>
 * </ul>
 * Saying the guh word in the chat is the gate's riddle ({@link Raadsels#chat}).
 */
public final class MijnEvents {
    private MijnEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            tick(p);
        }
    }

    /** One tick of a player (public: the game tests tick their mock players themselves). */
    public static void tick(ServerPlayer p) {
        Mijn m = Mijn.van(p);
        if (m == null) {
            if (p.tickCount % 20 == 0) {
                Achtervolging.stop(p);
                Brug.vergeet(p.getUUID());
            }
            return;
        }
        BlockPos lokaal = m.lokaal(p.blockPosition());
        if (Plekken.KLOOF.binnen(lokaal) && !p.isSpectator() && !p.getAbilities().flying) {
            p.resetFallDistance();
            Ring.behaald(p, "ring_h3_gevallen");
            terug(p, m, Ring.GEVALLEN, null);
            return;
        }
        if (p.tickCount % 5 != 0) {
            return;
        }
        Deuren.tick(p, m, lokaal);
        Brug.tick(p, m, lokaal);
        Verhaallijn lijn = RingH3Feature.LIJN;
        if (!lijn.aanDeBeurt(p) || p.isSpectator() || Cutscenes.bezig(p)) {
            return;
        }
        switch (lijn.stap(p)) {
            case 0 -> {
                if (Plekken.PLEIN.binnen(lokaal)) {
                    lijn.begin(p);
                    Verteller.toon(p, RingH3Feature.KAART, speler -> lijn.verder(speler, 0));
                }
            }
            case 3 -> {
                if (Plekken.PUTKAMER.binnen(lokaal)) {
                    Cutscenes.speel(p, Scenes.EMMER, m.wereld(Plekken.PUT), m.draai(), speler -> {
                        if (lijn.verder(speler, 3)) {
                            Ring.behaald(speler, "ring_h3_emmer");
                        }
                    });
                }
            }
            case 5 -> {
                if (Plekken.OOSTOEVER.binnen(lokaal)) {
                    Achtervolging.stop(p);
                    Brug.heel(p);
                    Cutscenes.speel(p, Scenes.BRUG, m.wereld(Plekken.BRUG_ANKER), m.draai(), MijnEvents::naDeBrug);
                } else if (Plekken.ZUILENHAL.binnen(lokaal)) {
                    Achtervolging.start(p, m);
                }
            }
            default -> {
            }
        }
    }

    /** The bridge scene was watched to the end: the step, the bridge that stays broken for this player for a minute. */
    static void naDeBrug(ServerPlayer p) {
        if (!RingH3Feature.LIJN.verder(p, 5)) {
            return;
        }
        Mijn m = Mijn.van(p);
        if (m != null) {
            Brug.breek(p, m, Brug.KAPOT_TICKS);
        }
        Ring.behaald(p, "ring_h3_brug");
        p.sendSystemMessage(Component.translatable("quest.guhs.ringh3.na_brug").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /**
     * Fallen or caught: poof, back at the last rest fire (with the story's own line for that reason), nothing lost, nobody hurt.
     * A player whose rest point isn't in this mine (a visitor whose story is done, somebody who walked along) is put at the mouth
     * of Gimguh's passage instead.
     */
    static void terug(ServerPlayer p, @Nullable Mijn m, String reden, @Nullable Vec3 van) {
        Rustpunten.Punt rust = Ring.rustpunt(p);
        boolean hier = m != null && rust != null && rust.dim() == p.level().dimension() && m.binnen(BlockPos.containing(rust.plek()));
        if (hier || m == null) {
            Ring.terugNaarRustpunt(p, reden, van);
            return;
        }
        if (!Duwtje.mag(p)) {
            return;
        }
        Duwtje.terug(p, p.level().dimension(), m.midden(Plekken.HAL_INGANG), m.yaw(180f));
        p.sendSystemMessage(Component.translatable("quest.guhs.ring.terug." + reden).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer p = event.getPlayer();
        String tekst = event.getRawText();
        if (Raadsels.zegtWoord(tekst)) {
            p.level().getServer().execute(() -> Raadsels.chat(p, tekst));
        }
    }

    /** Inside the mine a fall never hurts (the chasm, the stairs, a shove off the path). */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getSource().is(DamageTypeTags.IS_FALL) && Mijn.van(p) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Achtervolging.stop(event.getEntity().getUUID());
        Brug.vergeet(event.getEntity().getUUID());
        Mijn.vergeet(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        Achtervolging.wisAlles();
        Brug.wisAlles();
        Mijn.vergeetAlles();
    }
}
