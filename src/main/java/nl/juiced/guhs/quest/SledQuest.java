package nl.juiced.guhs.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhWorldData;

/**
 * <b>De kapotte slee</b>: the old Slee-guh in the sled hut up on the Guh Peaks lost the parts of his sled.
 * Bring him the sled runner (guh cave chests), the guh bell (guh village chests) and the pink ribbon (the guh
 * kleermaker) and you get your own sled plus the sled builder's book, which the rail recipes need.
 */
public final class SledQuest {
    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        GuhWorldData data = GuhWorldData.get(player.level().getServer());
        GuhWorldData.PlayerData p = data.player(player.getUUID());
        if (p.sledQuest >= 2) {
            GuhQuests.say(player, npc, "quest.guhs.sled.done");
            GuhQuests.giveIfMissing(player, ModItems.SLEEBOUWERSBOEK.get());
            return;
        }
        if (p.sledQuest == 0) {
            GuhQuests.say(player, npc, "quest.guhs.sled.start");
            p.sledQuest = 1;
            data.setDirty();
            return;
        }
        boolean runner = GuhQuests.count(player, ModItems.SLEEGLIJDER.get()) > 0;
        boolean bell = GuhQuests.count(player, ModItems.GUH_BELLETJE.get()) > 0;
        boolean ribbon = GuhQuests.count(player, ModItems.ROZE_LINT.get()) > 0;
        if (runner && bell && ribbon) {
            GuhQuests.take(player, ModItems.SLEEGLIJDER.get(), 1);
            GuhQuests.take(player, ModItems.GUH_BELLETJE.get(), 1);
            GuhQuests.take(player, ModItems.ROZE_LINT.get(), 1);
            p.sledQuest = 2;
            data.setDirty();
            GuhQuests.say(player, npc, "quest.guhs.sled.thanks");
            GuhQuests.give(player, ModItems.GUH_SLEE.get());
            GuhQuests.give(player, ModItems.SLEEBOUWERSBOEK.get());
            player.sendSystemMessage(Component.translatable("quest.guhs.sled.unlocked").withStyle(ChatFormatting.GOLD));
            GuhAdvancements.grant(player, "sled_repaired");
            ((ServerLevel) npc.level()).sendParticles(ParticleTypes.SNOWFLAKE, npc.getX(), npc.getY() + 1.5, npc.getZ(), 40, 0.8, 0.8, 0.8, 0.05);
            npc.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.3f);
            return;
        }
        GuhQuests.say(player, npc, "quest.guhs.sled.parts");
        player.sendSystemMessage(part("item.guhs.sleeglijder", runner));
        player.sendSystemMessage(part("item.guhs.guh_belletje", bell));
        player.sendSystemMessage(part("item.guhs.roze_lint", ribbon));
    }

    private static Component part(String key, boolean have) {
        return Component.literal(have ? "  ✔ " : "  - ").append(Component.translatable(key))
                .withStyle(have ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private SledQuest() {
    }
}
