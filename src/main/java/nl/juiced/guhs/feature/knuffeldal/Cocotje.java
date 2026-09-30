package nl.juiced.guhs.feature.knuffeldal;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Cocotje, in her little guh house in the Knuffeldal town. She's always looking for something...
 * <p>
 * She asks "WEET JIJ WAAR ZE ZIJN??????" and you pick an answer (the talking screen, guhs:knuffeldal_open/_action):
 * <ol>
 *   <li>"Ik ga gelijk zoeken!" - "Njeg succes. Ik vads het nog wel als je iets weet" (next time she asks again).</li>
 *   <li>"Ik zie ze aan je hangen Cocotje" - "OHJA ZE HANGEN AAN ME VEH", and the only thing left to say:
 *   "omda je vahoeg beh" - "njeg." You get a kaasknabbel and Cocotje in the Knus tab (once). After that she only says
 *   something cute.</li>
 *   <li>"Wie is ze?" - "hmmm da wik dus ook ekkes nie eigi..." (next time she asks again).</li>
 * </ol>
 * Her state per player is in GuhQuests.saved ({@value #KEY}): 0 nothing, 1 asked, 2 "OHJA...", 3 done.
 */
public final class Cocotje implements NpcRole {
    public static final String KEY = "guhs_knuffeldal_cocotje";
    public static final int NIETS = 0, GEVRAAGD = 1, OHJA = 2, KLAAR = 3;
    /** The answers (option ids of the screen). */
    public static final int ZOEKEN = 1, HANGEN = 2, WIE = 3, VAHOEG = 4;
    public static final int LIEVE_ZINNEN = 4;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.25f);
        KnuffeldalEvents.vriendje(player, "cocotje");
        int state = state(player);
        if (state == KLAAR) {
            GuhQuests.say(player, npc, "quest.guhs.cocotje.lief." + npc.getRandom().nextInt(LIEVE_ZINNEN));
            return;
        }
        if (state == OHJA) {
            // she's still waiting for your answer
            GuhQuests.say(player, npc, "quest.guhs.cocotje.antwoord." + HANGEN);
            scherm(npc, player, "quest.guhs.cocotje.antwoord." + HANGEN, VAHOEG);
            return;
        }
        setState(player, GEVRAAGD);
        GuhQuests.say(player, npc, "quest.guhs.cocotje.vraag");
        scherm(npc, player, "quest.guhs.cocotje.vraag", ZOEKEN, HANGEN, WIE);
    }

    /** The player picked an answer. */
    static void kies(GuhNpcEntity npc, ServerPlayer player, int answer) {   // (3.0: was antwoord, now NpcRole.antwoord's name)
        int state = state(player);
        if (state == GEVRAAGD && (answer == ZOEKEN || answer == WIE || answer == HANGEN)) {
            zeg(player, answer);
            GuhQuests.say(player, npc, "quest.guhs.cocotje.antwoord." + answer);
            if (answer == HANGEN) {
                setState(player, OHJA);
                npc.level().playSound(null, npc, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.3f);
                scherm(npc, player, "quest.guhs.cocotje.antwoord." + HANGEN, VAHOEG);
            } else {
                setState(player, NIETS);    // (next time she asks again)
                sluit(npc, player, "quest.guhs.cocotje.antwoord." + answer);
            }
        } else if (state == OHJA && answer == VAHOEG) {
            zeg(player, answer);
            GuhQuests.say(player, npc, "quest.guhs.cocotje.antwoord." + VAHOEG);
            setState(player, KLAAR);
            Minigames.give(player, new ItemStack(ModItems.KAAS_KNABBELS.get()));
            KnusVoortgang.hoogste(player, KnuffeldalVoortgang.COCOTJE, 1);
            GuhAdvancements.grant(player, "knuffeldal_cocotje");
            KnuffeldalAdvancements.toon(player, "cocotje");
            npc.level().playSound(null, npc, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.1f);
            if (npc.level() instanceof net.minecraft.server.level.ServerLevel server) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, npc.getX(), npc.getY() + 1.6, npc.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
            }
            sluit(npc, player, "quest.guhs.cocotje.antwoord." + VAHOEG);
        }
    }

    /** Your answer, in the chat: "<you> Ik zie ze aan je hangen Cocotje". */
    private static void zeg(ServerPlayer player, int answer) {
        player.sendSystemMessage(Component.literal("<").append(player.getDisplayName()).append("> ").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable("quest.guhs.cocotje.optie." + answer).withStyle(ChatFormatting.WHITE)));
    }

    private static void scherm(GuhNpcEntity npc, ServerPlayer player, String tekst, int... opties) {
        CompoundTag data = new CompoundTag();
        data.putString("Scherm", "cocotje");
        data.putString("Tekst", tekst);
        ListTag list = new ListTag();
        for (int o : opties) {
            CompoundTag opt = new CompoundTag();
            opt.putInt("Id", o);
            opt.putString("Tekst", "quest.guhs.cocotje.optie." + o);
            list.add(opt);
        }
        data.put("Opties", list);
        ModNetworking.sendTo(player, new KnuffeldalPayloads.Open(npc.getId(), data));
    }

    /** Shows her last words, with only a "Doei!" button. */
    private static void sluit(GuhNpcEntity npc, ServerPlayer player, String tekst) {
        CompoundTag data = new CompoundTag();
        data.putString("Scherm", "cocotje");
        data.putString("Tekst", tekst);
        data.putBoolean("Klaar", true);
        ModNetworking.sendTo(player, new KnuffeldalPayloads.Open(npc.getId(), data));
    }

    public static int state(ServerPlayer player) {
        return GuhQuests.saved(player).getIntOr(KEY, 0);
    }

    static void setState(ServerPlayer player, int state) {
        GuhQuests.saved(player).putInt(KEY, state);
    }
}
