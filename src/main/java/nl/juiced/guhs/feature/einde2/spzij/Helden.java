package nl.juiced.guhs.feature.einde2.spzij;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 "Sponsguh", slice sp_zij: Zeemeerman and Mosselguh ({@value SpZijLijnen#ZEEMEERMAN}, four steps).
 * <ol start="0">
 *   <li>visit the two old heroes in the rest home (walk up to Zeemeerman, or click one of them): scene {@code helden};</li>
 *   <li>the belt is in the invisible boatmobile on the car park ({@code onzichtbare_boot}): you bump into something you
 *       cannot see, and the belt lies at {@code riem} (a thing of ONE player): walk over it;</li>
 *   <li>the belt back to Zeemeerman: he puts it on and finds a "heroic" errand: fetch the lunch at De Krokante Knabbel, in the
 *       invisible boatmobile;</li>
 *   <li>drive (a joke of pure particles: an outline of a boat round you, a toot now and then, a little speed) to the front
 *       door of De Krokante Knabbel ({@code mobiel_start}; then the variant {@code 3_b}) and back to the heroes; reward the
 *       heldencape.</li>
 * </ol>
 * The residents stay where they are (scenes hide them with {@code verbergEcht}); this slice owns their clicks.
 */
public final class Helden {
    static final String T = SpZij.P + "helden.";
    static final String RIEM = "riem";
    static final double PAK = 1.7;
    static final String BAND = "einde2_sp_zij_helden_riem", DANK = "einde2_sp_zij_helden_dank";

    private static final Map<UUID, Long> BONK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TOET = new ConcurrentHashMap<>();

    private Helden() {
    }

    static Verhaallijn ln() {
        return SpZijLijnen.ZEEMEERMAN_LIJN;
    }

    // =====================================================================================================================
    // every 5 ticks while the player is in town
    // =====================================================================================================================

    static void tick(ServerPlayer p, ServerLevel level, long nu) {
        int stap = ln().stap(p);
        if (stap != SpZijLijnen.Z_RIEM) {
            SpZijEigen.weg(level, p, RIEM);
        }
        if (Cutscenes.bezig(p) || !SpZij.open(p)) {
            return;
        }
        Vec3 zeemeerman = SpZij.voeten(level, "zeemeerman");
        if (stap == SpZijLijnen.Z_LUNCH) {
            bootmobiel(p, level, nu);
        }
        if (Praat.bezig(p) || zeemeerman == null) {
            return;
        }
        switch (stap) {
            case SpZijLijnen.Z_BEZOEK -> {
                if (SpZij.bij(p.position(), zeemeerman, 3.0)) {
                    bezoek(p, level);
                }
            }
            case SpZijLijnen.Z_RIEM -> riem(p, level, nu);
            case SpZijLijnen.Z_RIEM_TERUG -> {
                if (SpZij.bij(p.position(), zeemeerman, 3.0)) {
                    riemTerug(p, level);
                }
            }
            case SpZijLijnen.Z_LUNCH -> {
                Vec3 deur = SpZij.voeten(level, "mobiel_start");
                if (!ln().vlag(p, SpZijLijnen.LUNCH) && SpZij.bij(p.position(), deur, 3.5)) {
                    lunchOpgehaald(p);
                } else if (ln().vlag(p, SpZijLijnen.LUNCH) && SpZij.bij(p.position(), zeemeerman, 3.5)) {
                    lunchTerug(p, level);
                }
            }
            default -> {
            }
        }
    }

    /** Step 0: the visit (scene helden). */
    static void bezoek(ServerPlayer p, ServerLevel level) {
        BlockPos anker = SpZij.plek(level, "zeemeerman");
        if (anker == null) {
            return;
        }
        if (!Cutscenes.speel(p, SpZijScenes.HELDEN, anker, Rotation.NONE, s -> ln().verder(s, SpZijLijnen.Z_BEZOEK)) && !Cutscenes.bezig(p)) {
            ln().verder(p, SpZijLijnen.Z_BEZOEK);
        }
    }

    /** Step 1: the car park: the bump into the invisible boat, the belt (a thing of this player only). */
    static void riem(ServerPlayer p, ServerLevel level, long nu) {
        Vec3 plek = SpZij.voeten(level, "riem");
        if (plek == null) {
            return;
        }
        if (p.position().distanceTo(plek) > 48) {
            SpZijEigen.weg(level, p, RIEM);
            return;
        }
        SpZijEigen.ding(level, p, RIEM, "minecraft:lead", plek.add(0, 0.45, 0), 1.0f);
        level.sendParticles(p, ParticleTypes.END_ROD, true, true, plek.x, plek.y + 0.8, plek.z, 2, 0.2, 0.2, 0.2, 0.01);
        if (Boomkoepel.inZone(level, "onzichtbare_boot", p.position())) {
            Long laatst = BONK.get(p.getUUID());
            if (laatst == null || nu - laatst > 200) {
                BONK.put(p.getUUID(), nu);
                SpZij.geluid(p, SoundEvents.WOOD_HIT, 1f, 0.6f);
                SpZij.merk(p, T + "bonk");
                Vec3 m = Boomkoepel.zoneMidden(level, "onzichtbare_boot");
                if (m != null) {
                    omtrek(level, p, m, 0f);
                }
            }
        }
        if (SpZij.bij(p.position(), plek, PAK)) {
            pakRiem(p, level);
        }
    }

    /** The belt picked up (true: now). */
    static boolean pakRiem(ServerPlayer p, @Nullable ServerLevel level) {
        if (ln().stap(p) != SpZijLijnen.Z_RIEM) {
            return false;
        }
        if (level != null) {
            SpZijEigen.weg(level, p, RIEM);
        }
        SpZij.geluid(p, SoundEvents.ITEM_PICKUP, 1f, 0.9f);
        SpZij.geluid(p, SoundEvents.NOTE_BLOCK_PLING.value(), 0.9f, 1.6f);
        SpZij.titel(p, T + "riem.titel", T + "riem.onder");
        return ln().verder(p, SpZijLijnen.Z_RIEM);
    }

    /** Step 2: the belt back; he finds a heroic errand. */
    static void riemTerug(ServerPlayer p, ServerLevel level) {
        if (!ln().verder(p, SpZijLijnen.Z_RIEM_TERUG)) {
            return;
        }
        SponsguhWezen z = held(level, Cast.ZEEMEERMAN_GUH), m = held(level, Cast.MOSSELGUH);
        if (z != null) {
            z.zetAnimatie("held");
            z.getLookControl().setLookAt(p);
            Praat.scene(p, BAND, List.of(new Praat.Regel(z, "", T + "band.1"), new Praat.Regel(m != null ? m : z, "", T + "band.2"),
                    new Praat.Regel(z, "", T + "band.3"), new Praat.Regel(z, "", T + "band.4")));
        } else {
            SpZij.stem(p, "entity.guhs.zeemeerman_guh", T + "band.3");
            SpZij.stem(p, "entity.guhs.zeemeerman_guh", T + "band.4");
        }
    }

    /** Step 3: at the front door of De Krokante Knabbel. */
    static void lunchOpgehaald(ServerPlayer p) {
        if (ln().stap(p) != SpZijLijnen.Z_LUNCH || ln().vlag(p, SpZijLijnen.LUNCH)) {
            return;
        }
        ln().vlag(p, SpZijLijnen.LUNCH, true);
        nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
        SpZij.geluid(p, E2SponsguhFeature.BESTELBEL.get(), 1f, 1f);
        SpZij.titel(p, T + "lunch.titel", T + "lunch.onder");
        Vec3 thuis = SpZij.voeten(p.level(), "zeemeerman");
        if (thuis != null) {
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("➜ ").withStyle(net.minecraft.ChatFormatting.GOLD)
                    .append(net.minecraft.network.chat.Component.translatable(T + "lunch.terug",
                            nl.juiced.guhs.feature.einde2.verhaal.Richting.van(p.getEyePosition(), thuis)).withStyle(net.minecraft.ChatFormatting.YELLOW)));
        }
    }

    /** Step 3, back at the rest home: the lunch, the thanks, the heldencape. */
    static void lunchTerug(ServerPlayer p, ServerLevel level) {
        if (!ln().vlag(p, SpZijLijnen.LUNCH) || !ln().verder(p, SpZijLijnen.Z_LUNCH)) {
            return;
        }
        ln().vlag(p, SpZijLijnen.LUNCH, false);
        SponsguhWezen z = held(level, Cast.ZEEMEERMAN_GUH), m = held(level, Cast.MOSSELGUH);
        if (z != null) {
            z.zetAnimatie("juich");
            Praat.scene(p, DANK, List.of(new Praat.Regel(z, "", T + "dank.1"), new Praat.Regel(m != null ? m : z, "", T + "dank.2"),
                    new Praat.Regel(z, "", T + "dank.3")));
        } else {
            SpZij.stem(p, "entity.guhs.zeemeerman_guh", T + "dank.3");
        }
        if (SpZij.eenmalig(ln(), p, "heldencape", new ItemStack(ModItems.clothingItem(GuhClothes.SPONSGUH_HELDENCAPE)))) {
            SpZij.titel(p, T + "klaar.titel", T + "klaar.onder");
            SpZij.geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
        }
    }

    /** The invisible boatmobile round the player: its outline in white specks, a little speed, a toot now and then. */
    static void bootmobiel(ServerPlayer p, ServerLevel level, long nu) {
        Vec3 thuis = SpZij.voeten(level, "zeemeerman");
        if (thuis == null || p.position().distanceTo(thuis) > 96 || !p.onGround() && p.getDeltaMovement().y < -0.6) {
            return;
        }
        omtrek(level, p, p.position(), p.getYRot());
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 0, false, false, false));
        Long laatst = TOET.get(p.getUUID());
        if (laatst == null || nu - laatst > 200) {
            TOET.put(p.getUUID(), nu);
            SpZij.geluid(p, E2SponsguhFeature.MOBIEL_TOET.get(), 0.8f, 1.3f);
            SpZij.balk(p, T + "toet");
        }
    }

    /** The outline of a boat (5 long, 3 wide) round a spot, facing this way, in white specks, for this player only. */
    static void omtrek(ServerLevel level, ServerPlayer p, Vec3 midden, float yaw) {
        double r = Math.toRadians(yaw);
        double vx = -Math.sin(r), vz = Math.cos(r), zx = vz, zz = -vx;
        DustParticleOptions stip = new DustParticleOptions(0xF4F8FF, 0.9f);
        for (double a = -2.5; a <= 2.5; a += 0.5) {
            for (int kant : new int[] {-1, 1}) {
                double breed = Math.abs(a) > 1.8 ? 0.9 : 1.5;
                double x = midden.x + vx * a + zx * breed * kant, z = midden.z + vz * a + zz * breed * kant;
                level.sendParticles(p, stip, true, false, x, midden.y + 0.3, z, 1, 0, 0, 0, 0);
            }
        }
        level.sendParticles(p, stip, true, false, midden.x + vx * 2.9, midden.y + 0.3, midden.z + vz * 2.9, 1, 0, 0, 0, 0);
    }

    /** The resident hero of this kind near his spot (null: not there). */
    @Nullable
    static SponsguhWezen held(ServerLevel level, Cast cast) {
        Vec3 plek = SpZij.voeten(level, cast == Cast.MOSSELGUH ? "mosselguh" : "zeemeerman");
        if (plek == null) {
            return null;
        }
        SponsguhWezen best = null;
        double bestD = Double.MAX_VALUE;
        for (SponsguhWezen w : level.getEntitiesOfClass(SponsguhWezen.class, new AABB(plek, plek).inflate(10), w -> w.cast() == cast && w.isAlive())) {
            double d = w.distanceToSqr(plek);
            if (d < bestD) {
                bestD = d;
                best = w;
            }
        }
        return best;
    }

    // =====================================================================================================================
    // clicks
    // =====================================================================================================================

    /** {@code SponsguhWezen.BIJ_KLIK}: Zeemeerman and Mosselguh (the residents of the rest home). */
    static boolean klik(SponsguhWezen w, ServerPlayer p) {
        boolean zm = w.cast() == Cast.ZEEMEERMAN_GUH, mg = w.cast() == Cast.MOSSELGUH;
        if (!zm && !mg || !Boomkoepel.isOnze(w, p, zm ? "zeemeerman" : "mosselguh")) {
            return false;
        }
        w.getLookControl().setLookAt(p);
        if (!SpZij.open(p)) {
            w.zetAnimatie("held");
            GuhQuests.say(p, w, T + "later." + (zm ? "zeemeerman" : "mosselguh"));
            return true;
        }
        ServerLevel level = p.level();
        int stap = ln().stap(p);
        switch (stap) {
            case SpZijLijnen.Z_BEZOEK -> bezoek(p, level);
            case SpZijLijnen.Z_RIEM -> {
                w.zetAnimatie("praat");
                GuhQuests.say(p, w, T + "klik.riem." + (zm ? "zeemeerman" : "mosselguh"));
            }
            case SpZijLijnen.Z_RIEM_TERUG -> riemTerug(p, level);
            case SpZijLijnen.Z_LUNCH -> {
                if (ln().vlag(p, SpZijLijnen.LUNCH)) {
                    lunchTerug(p, level);
                } else {
                    w.zetAnimatie("praat");
                    GuhQuests.say(p, w, T + "klik.lunch." + (zm ? "zeemeerman" : "mosselguh"));
                }
            }
            default -> {
                w.zetAnimatie(p.getRandom().nextBoolean() ? "held" : "lach");
                GuhQuests.say(p, w, T + "na." + (zm ? "zeemeerman." : "mosselguh.") + (1 + Math.floorMod(p.level().getGameTime() / 40, 2)));
            }
        }
        return true;
    }

    static void vergeet(UUID id) {
        BONK.remove(id);
        TOET.remove(id);
    }

    /** (for the clicks of Entity typing) */
    static boolean isHeld(Entity e) {
        return e instanceof SponsguhWezen w && (w.cast() == Cast.ZEEMEERMAN_GUH || w.cast() == Cast.MOSSELGUH);
    }
}
