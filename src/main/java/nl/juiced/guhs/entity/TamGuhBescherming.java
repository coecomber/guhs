package nl.juiced.guhs.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * 1.2.1: tamed guhs and players leave each other alone. A player (or their arrow, snowball...) can't hurt someone
 * else's tamed guh, and a tamed guh never hurts a player (its owner included). Your own guh you can still hit.
 */
public final class TamGuhBescherming {
    public static void onAttack(AttackEntityEvent event) {
        if (beschermd(event.getTarget(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onDamage(LivingIncomingDamageEvent event) {
        Entity bron = event.getSource().getEntity();
        if (bron instanceof Player speler && beschermd(event.getEntity(), speler)) {
            event.setCanceled(true);
        } else if (event.getEntity() instanceof Player && bron instanceof GuhEntity guh && guh.isTame()) {
            event.setCanceled(true);
        }
    }

    /** Is this a tamed guh that belongs to someone else than this player? */
    static boolean beschermd(Entity doel, Player speler) {
        return doel instanceof GuhEntity guh && guh.isTame() && !guh.isOwnedBy(speler);
    }

    private TamGuhBescherming() {
    }
}
