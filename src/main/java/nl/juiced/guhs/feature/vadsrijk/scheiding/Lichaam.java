package nl.juiced.guhs.feature.vadsrijk.scheiding;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * Y-SCHEIDING: "eigen lijf per wereld", the body next to kern's "eigen kist per wereld" ({@code kern.Inventaris}).
 * <p>What swaps (channel 5 and 6 of SCHEIDING_RAPPORT.md):
 * <ul>
 *   <li><b>vanilla xp</b> (level, progress, total): the Vadsrijk has its own pool (normally 0: Vadsrijk mobs give no orbs), so
 *   survival levels can't be spent in the Vadsrijk (FTB Essentials {@code /anvil}) and nothing gained inside comes home;</li>
 *   <li><b>health, hunger, saturation, absorption, fire and frost</b>: inside, kern keeps health and food full; without this a
 *   trip in and out was a free full heal and meal. Home values wait and come back exactly;</li>
 *   <li><b>potion effects</b>: survival effects (Strength, Speed, Night Vision...) wait with their remaining time and come back on
 *   the way home; whatever the Vadsrijk gave (Darkness, Slow Falling, Water Breathing...) is gone when you leave.</li>
 * </ul>
 * State lives in {@code SpelerData.extra("scheiding")} (the player attachment, saved in the same player.dat as the live body and
 * copied on death), with its own side flag {@link #KANT}: {@link #verzoen} only acts when that flag differs from kern's
 * {@code SpelerData.binnen}, so calling it twice, at login, or after a crash never swaps twice. A player who was inside when this
 * update arrived: their current xp/effects are their home ones (nothing was swapped before), so they are stashed as home.
 */
public final class Lichaam {
    static final String SLICE = "scheiding", KANT = "lijf_binnen", THUIS = "lijf_thuis", RIJK_XP = "lijf_rijk_xp";

    private Lichaam() {
    }

    /** Swaps the body when kern's side (the inventory side) differs from the body's side. Returns whether it swapped. */
    public static boolean verzoen(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        CompoundTag t = d.extra(SLICE);
        boolean kant = t.getBooleanOr(KANT, false);
        if (d.binnen == kant) {
            return false;
        }
        if (d.binnen) {
            naarRijk(p, t);
        } else {
            naarThuis(p, t);
        }
        t.putBoolean(KANT, d.binnen);
        return true;
    }

    private static void naarRijk(ServerPlayer p, CompoundTag t) {
        if (t.contains(THUIS)) {   // never overwrite a home body that is still waiting (it would be lost): keep the oldest
            leegLijf(p);
        } else {
            t.put(THUIS, vang(p, true));
            leegLijf(p);
        }
        CompoundTag xp = t.getCompoundOrEmpty(RIJK_XP);
        zetXp(p, xp.getIntOr("lvl", 0), xp.getFloatOr("prog", 0f), xp.getIntOr("tot", 0));
        t.remove(RIJK_XP);
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
    }

    private static void naarThuis(ServerPlayer p, CompoundTag t) {
        t.put(RIJK_XP, vang(p, false));
        leegLijf(p);
        CompoundTag thuis = t.getCompoundOrEmpty(THUIS);
        t.remove(THUIS);
        if (!thuis.isEmpty()) {
            zet(p, thuis);
        } else {
            zetXp(p, 0, 0f, 0);
        }
    }

    /** The body as a tag (all of it, or only the xp for the Vadsrijk side). */
    static CompoundTag vang(ServerPlayer p, boolean alles) {
        CompoundTag c = new CompoundTag();
        c.putInt("lvl", p.experienceLevel);
        c.putFloat("prog", p.experienceProgress);
        c.putInt("tot", p.totalExperience);
        if (!alles) {
            return c;
        }
        c.putFloat("hp", p.getHealth());
        c.putInt("food", p.getFoodData().getFoodLevel());
        c.putFloat("sat", p.getFoodData().getSaturationLevel());
        c.putFloat("abs", p.getAbsorptionAmount());
        c.putInt("vuur", Math.max(0, p.getRemainingFireTicks()));
        c.putInt("vries", p.getTicksFrozen());
        RegistryOps<Tag> ops = p.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ListTag eff = new ListTag();
        for (MobEffectInstance e : p.getActiveEffects()) {
            MobEffectInstance.CODEC.encodeStart(ops, e).result().ifPresent(eff::add);
        }
        c.put("eff", eff);
        return c;
    }

    /** Puts a captured home body back. */
    static void zet(ServerPlayer p, CompoundTag c) {
        zetXp(p, c.getIntOr("lvl", 0), c.getFloatOr("prog", 0f), c.getIntOr("tot", 0));
        RegistryOps<Tag> ops = p.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        List<MobEffectInstance> eff = new ArrayList<>();
        for (Tag e : c.getListOrEmpty("eff")) {
            MobEffectInstance.CODEC.parse(ops, e).result().ifPresent(eff::add);
        }
        for (MobEffectInstance e : eff) {
            p.addEffect(e);   // (effects first: Absorption / Health Boost raise the caps the amounts below need)
        }
        p.setHealth(Math.max(1f, Math.min(p.getMaxHealth(), c.getFloatOr("hp", p.getMaxHealth()))));
        p.getFoodData().setFoodLevel(Math.max(0, Math.min(20, c.getIntOr("food", 20))));
        p.getFoodData().setSaturation(Math.max(0f, c.getFloatOr("sat", 5f)));
        p.setAbsorptionAmount(Math.max(0f, c.getFloatOr("abs", 0f)));
        p.setRemainingFireTicks(c.getIntOr("vuur", 0));
        p.setTicksFrozen(c.getIntOr("vries", 0));
    }

    /** No effects, no absorption, no fire, no frost. */
    private static void leegLijf(ServerPlayer p) {
        p.removeAllEffects();
        p.setAbsorptionAmount(0f);
        p.clearFire();
        p.setTicksFrozen(0);
    }

    private static void zetXp(ServerPlayer p, int lvl, float prog, int tot) {
        p.experienceLevel = Math.max(0, lvl);
        p.experienceProgress = Math.max(0f, Math.min(1f, prog));
        p.totalExperience = Math.max(0, tot);
        p.setExperienceLevels(p.experienceLevel);   // (marks the xp bar for a resend)
    }

    /** Test/notes: is a home body waiting? */
    public static boolean thuisWacht(ServerPlayer p) {
        return Spelers.data(p).extra(SLICE).contains(THUIS);
    }
}
