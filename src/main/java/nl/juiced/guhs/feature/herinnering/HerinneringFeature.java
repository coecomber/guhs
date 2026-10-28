package nl.juiced.guhs.feature.herinnering;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DromenFeature;

/**
 * 1.8.0 Guhdromenland, the Herinneringseiland (dromen-ontwerp/BOUWPLAN_180.md part E; design ontwerp-guhs180-dromen.md,
 * chapter 3 of Snoes' story; revamped after the playtest of test build 2): three little memories of Snoes, from long before he
 * had a place. The island itself is a template of part A's framework (data/guhs/structure/dromen/eiland_herinnering.nbt +
 * data/guhs/dromen/eiland/herinnering.json, both from tools/features/herinnering.py); this class adds:
 * <ul>
 *   <li>{@link HerinneringBellen}: the three herinneringsbelletjes, each with its retro film ({@link HerinneringScenes});</li>
 *   <li>the little memory-Snoeses (guh_npc with the tag {@value #TAG}): clicking them only gives a memory line.</li>
 * </ul>
 */
public final class HerinneringFeature {
    /** The tag of the memory's people (tools/features/herinnering.py TAG). */
    public static final String TAG = "guhs_herinnering";

    public static void register() {
        HerinneringScenes.laad();
        NeoForge.EVENT_BUS.addListener(HerinneringFeature::onTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, HerinneringFeature::onKlikBlok);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, HerinneringFeature::onKlikEntity);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, HerinneringFeature::onKlikEntitySpecifiek);
    }

    /** Is this player dreaming on (or right next to) the Herinneringseiland? */
    static boolean hier(ServerPlayer p) {
        return DromenFeature.isDroom(p.level()) && DroomEilanden.HERINNERING.equals(DroomEilanden.dichtstbij(p.position()));
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 10 == 5 && !p.isSpectator() && hier(p)) {
            HerinneringBellen.aanraken(p);
        }
    }

    private static void onKlikBlok(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer p && hier(p) && HerinneringBellen.klik(p, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void onKlikEntity(PlayerInteractEvent.EntityInteract event) {
        if (herinnering(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void onKlikEntitySpecifiek(PlayerInteractEvent.EntityInteractSpecific event) {
        if (herinnering(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** A click on one of the memory's people: a memory line instead of their usual chat. True: it was one. */
    static boolean herinnering(net.minecraft.world.entity.player.Player speler, Entity doel) {
        if (!(doel instanceof GuhNpcEntity npc) || !doel.entityTags().contains(TAG)) {
            return false;
        }
        if (speler instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.herinnering.persoon_snoes").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        }
        return true;
    }

    private HerinneringFeature() {
    }
}
