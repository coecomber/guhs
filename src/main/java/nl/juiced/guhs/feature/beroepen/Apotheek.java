package nl.juiced.guhs.feature.beroepen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.beroepen.BeroepenVoortgang.Beroep;
import nl.juiced.guhs.feature.boerderij.BoerderijFeature;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Dokter Snotneus-guh (APOTHEKERGUH), behind the balie of the Apotheekje. The job (once per player):
 * <ol>
 *   <li>Talk to him: Snotje (a little guhtje by the bed in the ziekenhoekje) is all snotterig - hatsjoe! You get a bottle
 *   of kaasmelk from his fridge (if you have none) (step 1).</li>
 *   <li>Pick {@link #KRUIDJES} snotkruidjes in the kruidentuin behind the apotheek ({@link SnotkruidBlock}), and mix them
 *   with the kaasmelk in the {@link MengketelBlock}: a kaasmelkdrankje.</li>
 *   <li>Give the drankje to Snotje (right-click him with it): one last big HATSJOE... VAHOEG! He's better (step 2).</li>
 *   <li>Tell the doctor: the doctor's coat and the stethoscope.</li>
 * </ol>
 * Snotje is a guh with the flag {@link #SNOTJE} in its NeoForge data (1 = snotterig, 0 = better); while snotterig he
 * sneezes now and then. Starting the job makes him snotterig again ("hij is ook zo gauw verkouden").
 */
public final class Apotheek implements NpcRole {
    public static final Apotheek ROLE = new Apotheek();
    public static final Beroep BEROEP = Beroep.APOTHEEK;
    public static final String SNOTJE = "guhs_beroepen_snotterig";
    public static final int KRUIDJES = 3;
    public static final int BEREIK = 20;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.05f);
        if (BeroepenVoortgang.klaar(player, BEROEP)) {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.apotheek.bedankt" + player.getRandom().nextInt(3));
            return;
        }
        int stap = BeroepenVoortgang.stap(player, BEROEP);
        if (stap == 0) {
            BeroepenVoortgang.zet(player, BEROEP, 1);
            for (GuhEntity s : snotjes(npc)) {
                s.getPersistentData().putBoolean(SNOTJE, true);
            }
            GuhQuests.say(player, npc, "quest.guhs.beroepen.apotheek.start", KRUIDJES);
            if (GuhQuests.count(player, BoerderijFeature.KAASMELK.get()) == 0 && !heeftKaasmelk(player)) {
                Minigames.give(player, new ItemStack(BoerderijFeature.KAASMELK.get()));
                GuhQuests.say(player, npc, "quest.guhs.beroepen.apotheek.kaasmelk");
            }
        } else if (stap == 1) {
            int kruidjes = GuhQuests.count(player, BeroepenFeature.SNOTKRUIDJE.get());
            if (GuhQuests.count(player, BeroepenFeature.KAASMELKDRANKJE.get()) > 0) {
                GuhQuests.say(player, npc, "quest.guhs.beroepen.apotheek.geef");
            } else if (kruidjes >= KRUIDJES && heeftKaasmelk(player)) {
                GuhQuests.say(player, npc, "quest.guhs.beroepen.apotheek.ketel");
            } else if (kruidjes >= KRUIDJES) {
                Minigames.give(player, new ItemStack(BoerderijFeature.KAASMELK.get()));
                GuhQuests.say(player, npc, "quest.guhs.beroepen.apotheek.kaasmelk");
            } else {
                GuhQuests.say(player, npc, "quest.guhs.beroepen.apotheek.kruidjes", kruidjes, KRUIDJES);
            }
        } else {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.apotheek.klaar");
            BeroepenVoortgang.rondAf(player, BEROEP, npc);
        }
    }

    static boolean heeftKaasmelk(ServerPlayer player) {
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (s.is(KnusTags.KAASMELK)) {
                return true;
            }
        }
        return false;
    }

    /** The Snotjes near the doctor (usually one). */
    public static List<GuhEntity> snotjes(GuhNpcEntity npc) {
        return npc.level().getEntitiesOfClass(GuhEntity.class, new AABB(npc.blockPosition()).inflate(BEREIK),
                g -> g.getPersistentData().contains(SNOTJE));
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (npc.tickCount % 100 == 37) {
            for (GuhEntity s : snotjes(npc)) {
                if (s.getPersistentData().getBooleanOr(SNOTJE, false) && npc.getRandom().nextInt(3) > 0) {
                    nies(s, 0.8f);
                }
            }
        }
    }

    /** Hatsjoe! */
    public static void nies(GuhEntity snotje, float volume) {
        if (snotje.level() instanceof ServerLevel level) {
            level.playSound(null, snotje, BeroepenFeature.HATSJOE.get(), SoundSource.NEUTRAL, volume, 1.1f + level.getRandom().nextFloat() * 0.2f);
            var look = snotje.getLookAngle();
            level.sendParticles(ParticleTypes.SNEEZE, snotje.getX() + look.x * 0.4, snotje.getEyeY() - 0.1, snotje.getZ() + look.z * 0.4, 6,
                    0.1, 0.05, 0.1, 0.02);
        }
    }

    /**
     * The player right-clicks Snotje with `stack` in hand: with a kaasmelkdrankje he drinks it and is better (step 2 of
     * the job, if it's theirs); without one he just sniffs. Returns true when he drank.
     */
    public static boolean snotje(ServerPlayer player, GuhEntity snotje, ItemStack stack) {
        ServerLevel level = player.level();
        if (!stack.is(BeroepenFeature.KAASMELKDRANKJE.get())) {
            if (snotje.getPersistentData().getBooleanOr(SNOTJE, false)) {
                nies(snotje, 1.0f);
                player.sendOverlayMessage(Component.translatable("gui.guhs.beroepen.snotje.snif").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                level.sendParticles(ParticleTypes.HEART, snotje.getX(), snotje.getY() + 0.8, snotje.getZ(), 3, 0.2, 0.2, 0.2, 0.02);
                player.sendOverlayMessage(Component.translatable("gui.guhs.beroepen.snotje.blij").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return false;
        }
        if (!snotje.getPersistentData().getBooleanOr(SNOTJE, false)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.beroepen.snotje.al_beter").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            Minigames.give(player, new ItemStack(Items.GLASS_BOTTLE));
        }
        snotje.getPersistentData().putBoolean(SNOTJE, false);
        nies(snotje, 1.3f);
        level.playSound(null, snotje, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.7f);
        level.sendParticles(ParticleTypes.HEART, snotje.getX(), snotje.getY() + 0.8, snotje.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, snotje.getX(), snotje.getY() + 0.5, snotje.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        if (BeroepenVoortgang.stap(player, BEROEP) == 1) {
            BeroepenVoortgang.zet(player, BEROEP, 2);
            player.sendSystemMessage(Component.translatable("gui.guhs.beroepen.snotje.beter").withStyle(ChatFormatting.GOLD));
        } else {
            player.sendOverlayMessage(Component.translatable("gui.guhs.beroepen.snotje.vahoeg").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }
}
