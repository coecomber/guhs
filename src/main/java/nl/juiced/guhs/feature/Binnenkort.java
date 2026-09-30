package nl.juiced.guhs.feature;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The placeholder role of a 2.8 guh character whose feature isn't built yet (the phase-1 stubs use it): it says
 * "binnenkort..." (lang quest.guhs.knus.binnenkort) and nothing else. The owner replaces it with its own role.
 */
public final class Binnenkort implements NpcRole {
    public static final NpcRole ROLE = new Binnenkort();

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        GuhQuests.say(player, npc, "quest.guhs.knus.binnenkort");
    }

    private Binnenkort() {
    }
}
