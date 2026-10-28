package nl.juiced.guhs.feature.zingendeweide;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;

/**
 * Badmeester Bubbels, the guh of the bath hut at the Bubbelsap-poeltjes: he hands every player one warm towel a day
 * ({@link WeideBlokken.Handdoekje}: fresh and happy) and tells a joke now and then.
 */
public final class BadGuh implements NpcRole {
    private static final String DAG = "guhs_weide_handdoek_dag";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        nl.juiced.guhs.feature.droomsfeer.DroomGeluid.BADMEESTER.speel(npc);   // (droomsfeer: his own voice, inspiration/Dromen audio)
        ServerLevel level = (ServerLevel) npc.level();
        long dag = Math.floorDiv(level.getDefaultClockTime(), 24000L);
        String sleutel = DAG + "_" + player.getUUID();
        if (npc.roleData.getLongOr(sleutel, Long.MIN_VALUE) == dag) {
            player.sendSystemMessage(praat(npc, level.getRandom().nextBoolean() ? 3 : 4));
            return;
        }
        npc.roleData.putLong(sleutel, dag);
        player.sendSystemMessage(praat(npc, level.getRandom().nextBoolean() ? 1 : 2));
        ItemStack doek = new ItemStack(ZingendeWeideFeature.HANDDOEKJE.get());
        if (!player.getInventory().add(doek)) {
            player.drop(doek, false);
        }
        level.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.2, npc.getZ(), 3, 0.3, 0.2, 0.3, 0);
    }

    private static Component praat(GuhNpcEntity npc, int n) {
        return Component.literal("<").append(npc.getDisplayName()).append("> ").append(Component.translatable("gui.guhs.weide.badguh." + n));
    }
}
