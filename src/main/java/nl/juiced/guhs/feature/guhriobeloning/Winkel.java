package nl.juiced.guhs.feature.guhriobeloning;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * Pad-guh's shop: what he sells for the level coins of Super Guhrio (the pocket of {@code GuhrioKasteel}: a coin of a
 * level counts the first time you ever take it), and his tip. Three outfits and the three building blocks; the prices are
 * also in the Dutch texts of tools/features/guhrio_beloning.py (PRIJZEN there; a game test compares them).
 * <p>
 * The coins of a level are finite, so every flagpole also pays a little tip ({@link #fooi}): one coin for every
 * {@link #FOOI_PER} coins on your panel of that run. That way the building blocks stay for sale for who keeps playing.
 */
public final class Winkel {
    /** One thing in the shop: what you get for how many coins. */
    public enum Waar {
        RODE_PET("rode_pet", 30, true, () -> kleding(GuhClothes.GUHRIOBELONING_RODE_PET)),
        GROENE_PET("groene_pet", 30, true, () -> kleding(GuhClothes.GUHRIOBELONING_GROENE_PET)),
        SCHILD("schild", 40, true, () -> kleding(GuhClothes.GUHRIOBELONING_SCHILD)),
        VRAAGBLOK("vraagblok", 20, false, () -> new ItemStack(GuhrioBeloningFeature.VRAAGBLOK_ITEM.get())),
        VLAGGENMAST("vlaggenmast", 10, false, () -> new ItemStack(GuhrioBeloningFeature.VLAGGENMAST_ITEM.get(), 4)),
        PIJP("pijp", 25, false, () -> new ItemStack(GuhrioBeloningFeature.PIJP_ITEM.get(), 2));

        public final String id;
        public final int prijs;
        /** An outfit (the first shelf) or a building block (the second). */
        public final boolean outfit;
        private final Supplier<ItemStack> wat;

        Waar(String id, int prijs, boolean outfit, Supplier<ItemStack> wat) {
            this.id = id;
            this.prijs = prijs;
            this.outfit = outfit;
            this.wat = wat;
        }

        /** What you get (a fresh stack). */
        public ItemStack stack() {
            return wat.get();
        }

        /** The text of its button, with the price ("Rode pet met snor (30 munten)"). */
        public String knop() {
            return "gui.guhs.guhriobeloning.waar." + id;
        }

        /** The outfits, or the building blocks. */
        public static List<Waar> plank(boolean outfits) {
            return java.util.Arrays.stream(values()).filter(w -> w.outfit == outfits).toList();
        }
    }

    /** Pad-guh's tip at a flagpole: one pocket coin for every this many coins on the panel of that run. */
    public static final int FOOI_PER = 5;
    /** Questline flags: bought anything, bought a building block. */
    public static final String GEKOCHT = "gekocht", BLOK_GEKOCHT = "blok_gekocht";

    private Winkel() {
    }

    private static ItemStack kleding(GuhClothes c) {
        return new ItemStack(ModItems.clothingItem(c));
    }

    /**
     * Buys one thing: the coins leave the pocket and the thing goes into the inventory (or at your feet). False (and
     * nothing happens) when the pocket is too light.
     */
    public static boolean koop(ServerPlayer p, Waar waar) {
        if (!GuhrioKasteel.betaal(p, waar.prijs)) {
            return false;
        }
        Minigames.give(p, waar.stack());
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.5f);
        p.level().sendParticles(ParticleTypes.WAX_ON, p.getX(), p.getY() + 1.2, p.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
        GuhrioBeloningFeature.LIJN.vlag(p, GEKOCHT, true);
        if (!waar.outfit) {
            GuhrioBeloningFeature.LIJN.vlag(p, BLOK_GEKOCHT, true);
        }
        GuhAdvancements.grant(p, "guhrio_beloning_gekocht");
        return true;
    }

    /** How many coins this player is short for it (0: can pay). */
    public static int tekort(ServerPlayer p, Waar waar) {
        return Math.max(0, waar.prijs - GuhrioSpel.munten(p));
    }

    /** (GuhrioSpel.BIJ_KLAAR) the tip at the flagpole of a castle level. */
    static void fooi(ServerPlayer p, GuhrioSpel.Sessie sessie, int ticks, boolean record) {
        if (!GuhrioKasteel.LEVELS.contains(sessie.level().level().id())) {
            return;
        }
        int fooi = sessie.munten / FOOI_PER;
        if (fooi <= 0) {
            return;
        }
        geef(p, fooi);
        p.sendSystemMessage(Component.translatable("gui.guhs.guhriobeloning.fooi", fooi, GuhrioSpel.munten(p)).withStyle(ChatFormatting.YELLOW));
    }

    /** Puts coins into the pocket (the tip; dev commands). */
    public static void geef(ServerPlayer p, int munten) {
        GuhrioSpel.spaar(p).putInt("Munten", GuhrioSpel.munten(p) + Math.max(0, munten));
    }
}
