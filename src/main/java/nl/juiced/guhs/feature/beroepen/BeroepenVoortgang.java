package nl.juiced.guhs.feature.beroepen;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * Per player: how far each beroep is (saved in GuhQuests.saved(player)["guhs_beroepen"], survives dying). Each beroep
 * has its own compound {Stap: int, Klaar: bool}; step 0 = not started. {@link #rondAf} gives the two clothing pieces of the
 * beroep exactly once and the advancements (quest/beroepen_&lt;id&gt; for FTB, grote_guhspelen/beroepen_&lt;id&gt; shown;
 * all four: beroepen_alle).
 */
public final class BeroepenVoortgang {
    public static final String KEY = "guhs_beroepen";

    public enum Beroep {
        BRANDWEER(GuhClothes.FIREFIGHTER_HELMET, GuhClothes.FIREFIGHTER_JACKET),
        POLITIE(GuhClothes.POLICE_CAP, GuhClothes.POLICE_UNIFORM),
        APOTHEEK(GuhClothes.DOCTOR_COAT, GuhClothes.STETHOSCOPE),
        BOUW(GuhClothes.BUILDER_HELMET, GuhClothes.SAFETY_VEST);

        private final List<GuhClothes> kleding;

        Beroep(GuhClothes a, GuhClothes b) {
            this.kleding = List.of(a, b);
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The two pieces this job gives (their one source). */
        public List<GuhClothes> kleding() {
            return kleding;
        }

        /** The KledingBronnen source id (CONTRACT_29 §5.5). */
        public String bron() {
            return "beroep_" + id();
        }

        public String advancement() {
            return "beroepen_" + id();
        }
    }

    public static CompoundTag data(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (!saved.contains(KEY)) {
            saved.put(KEY, new CompoundTag());
        }
        return saved.getCompoundOrEmpty(KEY);
    }

    private static CompoundTag van(ServerPlayer player, Beroep beroep) {
        CompoundTag all = data(player);
        if (!all.contains(beroep.id())) {
            all.put(beroep.id(), new CompoundTag());
        }
        return all.getCompoundOrEmpty(beroep.id());
    }

    public static int stap(ServerPlayer player, Beroep beroep) {
        return van(player, beroep).getIntOr("Stap", 0);
    }

    public static void zet(ServerPlayer player, Beroep beroep, int stap) {
        van(player, beroep).putInt("Stap", stap);
    }

    public static boolean klaar(ServerPlayer player, Beroep beroep) {
        return van(player, beroep).getBooleanOr("Klaar", false);
    }

    /** How many of the four jobs this player has done. */
    public static int aantal(ServerPlayer player) {
        int n = 0;
        for (Beroep b : Beroep.values()) {
            if (klaar(player, b)) {
                n++;
            }
        }
        return n;
    }

    /**
     * The job is done: the two clothing pieces (as items, once ever), the advancements, a little party. Returns false (and
     * gives nothing) when it was already done before.
     */
    public static boolean rondAf(ServerPlayer player, Beroep beroep, @Nullable Entity npc) {
        CompoundTag d = van(player, beroep);
        if (d.getBooleanOr("Klaar", false)) {
            return false;
        }
        d.putBoolean("Klaar", true);
        d.putInt("Stap", 0);
        for (GuhClothes c : beroep.kleding()) {
            Minigames.give(player, new ItemStack(ModItems.clothingItem(c)));
        }
        GuhAdvancements.grant(player, beroep.advancement());
        toon(player, beroep.advancement());
        if (aantal(player) == Beroep.values().length) {
            GuhAdvancements.grant(player, "beroepen_alle");
            toon(player, "beroepen_alle");
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.beroepen.geleerd", Component.translatable("gui.guhs.beroepen.naam." + beroep.id()))
                .withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
        if (npc != null) {
            player.level().sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.6, npc.getZ(), 8, 0.4, 0.3, 0.4, 0.05);
        }
        player.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.6, 0.6, 0.6, 0.1);
        return true;
    }

    /** Grants the shown advancement grote_guhspelen/&lt;name&gt;. */
    static void toon(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id("grote_guhspelen/" + name));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    /** (Tests) forget everything. */
    public static void wis(ServerPlayer player) {
        GuhQuests.saved(player).remove(KEY);
    }

    private BeroepenVoortgang() {
    }
}
