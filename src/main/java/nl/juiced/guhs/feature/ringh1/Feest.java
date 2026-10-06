package nl.juiced.guhs.feature.ringh1;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.barbecuether.Grillguh;
import nl.juiced.guhs.feature.ring.Cast;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingBeloning;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.ModDimensions;

/**
 * bbq2 (ring-h1): chapter 1 of the Knabbelring, "Een langverwacht knabbelfeest", as a per-player questline
 * ({@link RingH1Feature#LIJN}, six steps). It plays at any camp of Guhdalf: on the feestwei of a Knabbelgouw or next to an
 * old big barbecueput ({@link Gouw}). Nothing in the world is used up: every click only moves the clicking player's own
 * story, so any number of players do it at the same camp, at the same time.
 * <ol start="0">
 *   <li>Talk to Guhdalf (after the Grillguh's barbecue burns): the narrator card, then the scene of his arrival.</li>
 *   <li>Three chores for the party: fire a rocket from the crate on his cart, lay the party table, invite Sam-guh.</li>
 *   <li>Talk to Guhdalf: the scene of the farewell party, where he hands over the Knabbelring.</li>
 *   <li>Ask Sam-guh along: from now on he is the player's own companion ({@link Sam#roep}).</li>
 *   <li>Pack the provisions with him: the three crates at the cart (worst, kaas, knabbels).</li>
 *   <li>Walk to the grill portal together. That finishes the chapter, which opens the portal lock of ring-kern.</li>
 * </ol>
 * Afterwards Guhdalf says where the trip goes; when the whole story is done he and the party table give the daily treat
 * ({@link RingBeloning#feest}), and the fireworks crate keeps working for everybody, always.
 */
public final class Feest {
    /** The chores of step 1 and the provisions of step 4 (flags of the questline). */
    public static final String VUURWERK = "vuurwerk", TAFEL = "tafel", SAM = "sam";
    public static final List<String> KLUSJES = List.of(VUURWERK, TAFEL, SAM);
    public static final List<String> PROVIAND = List.of("worst", "kaas", "knabbels");
    /** Saved: the camp where this player met Guhdalf (block position + dimension), and the frame of its pit. */
    static final String THUIS = "guhs_ringh1_thuis", THUIS_DIM = "guhs_ringh1_thuis_dim", PORTAAL = "guhs_ringh1_portaal";
    /** How near the portal (or the frame of the pit) counts as "there", and how near Sam-guh has to be. */
    public static final double PORTAAL_BEREIK = 4.5, SAM_BEREIK = 16;
    /** A rocket per player per this many ticks; it bursts this high above the crate (and above whatever is over it). */
    public static final int VUURWERK_WACHT = 40, VUURWERK_HOOG = 14;
    private static final String VUURWERK_TIJD = "guhs_ringh1_vuurwerk";
    private static final int[] KLEUREN = {0xF2577A, 0xFFD23F, 0x6CC24A, 0x4AA3F2, 0xB36CF2, 0xFF8A3D, 0xFFFFFF};

    private static Verhaallijn lijn() {
        return RingH1Feature.LIJN;
    }

    // =====================================================================================================================
    // the questline's own answers (Guhdex, compass)
    // =====================================================================================================================

    static List<VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        Verhaallijn l = lijn();
        if (stap == 1) {
            return List.of(Verhaallijn.nodig("minecraft:firework_rocket", "gui.guhs.ringh1.nodig.vuurwerk", l.vlag(p, VUURWERK) ? 1 : 0, 1),
                    Verhaallijn.nodig("minecraft:cake", "gui.guhs.ringh1.nodig.tafel", l.vlag(p, TAFEL) ? 1 : 0, 1),
                    Verhaallijn.nodig("guhs:ring_stoofpotje", "gui.guhs.ringh1.nodig.sam", l.vlag(p, SAM) ? 1 : 0, 1));
        }
        if (stap == 4) {
            return List.of(Verhaallijn.nodig("guhs:guhbraadworst", "gui.guhs.ringh1.nodig.worst", l.vlag(p, PROVIAND.get(0)) ? 1 : 0, 1),
                    Verhaallijn.nodig("guhs:belegen_kaas_tegels", "gui.guhs.ringh1.nodig.kaas", l.vlag(p, PROVIAND.get(1)) ? 1 : 0, 1),
                    Verhaallijn.nodig("guhs:kaas_knabbels", "gui.guhs.ringh1.nodig.knabbels", l.vlag(p, PROVIAND.get(2)) ? 1 : 0, 1));
        }
        return List.of();
    }

    static List<VerhaalStand.Beloning> beloningen(ServerPlayer p) {
        boolean klaar = lijn().klaar(p);
        return List.of(Verhaallijn.beloning("guhs:knabbelring", lijn().stap(p) >= 3),
                Verhaallijn.beloning("guhs:ring_stoofpotje", "gui.guhs.ringh1.beloning.sam", lijn().stap(p) >= 4),
                Verhaallijn.beloning("guhs:grillkool", "gui.guhs.ringh1.beloning.portaal", klaar));
    }

    /** Where to go: the nearest Guhdalf until the player met him, then his camp, at the end the portal of that pit. */
    @Nullable
    static Doel doel(ServerPlayer p, int stap) {
        CompoundTag saved = GuhQuests.saved(p);
        Identifier dimId = Identifier.tryParse(saved.getStringOr(THUIS_DIM, ""));
        if (dimId != null && saved.contains(THUIS)) {   // (they met Guhdalf: his camp is where their chapter plays)
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimId);
            if (stap >= 5 && saved.contains(PORTAAL)) {
                return Doel.plek(dim, BlockPos.of(saved.getLongOr(PORTAAL, 0L)), Component.translatable("gui.guhs.ringh1.doel.portaal"));
            }
            return Doel.plek(dim, BlockPos.of(saved.getLongOr(THUIS, 0L)), Component.translatable("gui.guhs.ringh1.doel.guhdalf"));
        }
        BlockPos daar = Gouw.dichtstbij(p);
        if (daar != null) {
            return Doel.plek(ModDimensions.GUHMENSION, daar, Component.translatable("gui.guhs.ringh1.doel.guhdalf"));
        }
        return Ring.doel(Gouw.STRUCTUUR);
    }

    /** May this player start the story: the Grillguh's quest is done (they lit a grill portal)? */
    public static boolean magBeginnen(ServerPlayer p) {
        return Grillguh.step(p) >= Grillguh.DONE;
    }

    /** (once a second per player) the story starts by itself once the Grillguh's barbecue burns: "zoek Guhdalf". */
    static void begin(ServerPlayer p) {
        if (!lijn().begonnen(p) && lijn().stap(p) == 0 && magBeginnen(p)) {
            lijn().begin(p);
        }
    }

    // =====================================================================================================================
    // Guhdalf
    // =====================================================================================================================

    /** Guhdalf at a camp (NpcRollen plek {@link Gouw#ROL}): the same Guhdalf, every player their own story. */
    static final class GuhdalfRol extends QuestRol {
        GuhdalfRol() {
            super(RingH1Feature.LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.75f);
            Feest.praat(npc, p, stap);
        }
    }

    static void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
        Verhaallijn l = lijn();
        if (l.klaar(p)) {
            if (Ring.klaar(p)) {
                RingBeloning.feest(p, npc);   // the daily party: one treat a day
            } else {
                if (Ring.kreeg(p) && !Ring.heeft(p) && Ring.geef(p)) {
                    GuhQuests.say(p, npc, "quest.guhs.ringh1.guhdalf.ring_kwijt");
                    return;
                }
                GuhQuests.say(p, npc, "quest.guhs.ring.cast.guhdalf." + Cast.REIS + "." + p.getRandom().nextInt(Cast.REGELS));
                Ring.vertelDoel(p, npc);
            }
            return;
        }
        if (stap >= 3 && !Ring.heeft(p) && Ring.geef(p)) {
            GuhQuests.say(p, npc, "quest.guhs.ringh1.guhdalf.ring_kwijt");
            return;
        }
        switch (stap) {
            case 0 -> {
                if (!magBeginnen(p)) {
                    GuhQuests.say(p, npc, "quest.guhs.ringh1.guhdalf.grillguh");
                    return;
                }
                l.begin(p);
                onthoudThuis(p, npc.blockPosition());
                GuhQuests.say(p, npc, "quest.guhs.ringh1.guhdalf.hallo");
                Gouw.Plek kamp = Gouw.kampBij(p.level(), npc.blockPosition());
                BlockPos anker = npc.blockPosition();
                // the card, then the scene, then the step: a player who logs out in between simply sees it again
                if (!Verteller.toon(p, RingH1Feature.KAART, a -> speelAankomst(a, anker, kamp))) {
                    speelAankomst(p, anker, kamp);
                }
            }
            case 1 -> {
                if (klusjesKlaar(p)) {
                    return;
                }
                MutableComponent nog = Component.empty();
                boolean eerste = true;
                for (String klus : KLUSJES) {
                    if (!l.vlag(p, klus)) {
                        if (!eerste) {
                            nog.append(", ");
                        }
                        nog.append(Component.translatable("quest.guhs.ringh1.guhdalf.klus." + klus));
                        eerste = false;
                    }
                }
                GuhQuests.say(p, npc, "quest.guhs.ringh1.guhdalf.klusjes", nog);
            }
            case 2 -> {
                GuhQuests.say(p, npc, "quest.guhs.ringh1.guhdalf.klaar_voor_feest");
                Gouw.Plek kamp = Gouw.kampBij(p.level(), npc.blockPosition());
                Cutscenes.speel(p, RingH1Feature.FEEST, npc.blockPosition(), kamp.draai(), a -> {
                    Ring.geef(a);
                    Ring.behaald(a, "ring_h1_feest");
                    lijn().verder(a, 2);
                });
            }
            case 3 -> GuhQuests.say(p, npc, "quest.guhs.ringh1.guhdalf.na_feest");
            case 4 -> GuhQuests.say(p, npc, "quest.guhs.ringh1.guhdalf.proviand");
            default -> GuhQuests.say(p, npc, "quest.guhs.ringh1.guhdalf.portaal");
        }
    }

    private static void speelAankomst(ServerPlayer p, BlockPos anker, Gouw.Plek kamp) {
        Cutscenes.speel(p, RingH1Feature.AANKOMST, anker, kamp.draai(), a -> lijn().verder(a, 0));
    }

    /** This camp is where the player's chapter plays: the compass points here, and to its pit's portal at the end. */
    private static void onthoudThuis(ServerPlayer p, BlockPos guhdalf) {
        CompoundTag saved = GuhQuests.saved(p);
        saved.putLong(THUIS, guhdalf.asLong());
        saved.putString(THUIS_DIM, p.level().dimension().identifier().toString());
        BlockPos frame = Gouw.frame(p.level(), guhdalf);
        if (frame != null) {
            saved.putLong(PORTAAL, frame.asLong());
        } else {
            saved.remove(PORTAAL);
        }
    }

    // =====================================================================================================================
    // the chores
    // =====================================================================================================================

    /** All three chores done: on to the party (true when the step moved, or had moved already). */
    static boolean klusjesKlaar(ServerPlayer p) {
        Verhaallijn l = lijn();
        if (l.stap(p) != 1) {
            return l.stap(p) > 1;
        }
        for (String klus : KLUSJES) {
            if (!l.vlag(p, klus)) {
                return false;
            }
        }
        if (l.verder(p, 1)) {
            p.sendSystemMessage(Component.translatable("quest.guhs.ringh1.klusjes_klaar").withStyle(ChatFormatting.GOLD));
        }
        return true;
    }

    /**
     * A click on Guhdalf's fireworks crate: a rocket, for everybody, always (one per two seconds per player). For a player at
     * the chores it is the first chore. The rocket never comes near anybody: it bursts high above the crate and above
     * whatever stands over it, and when somebody is up there after all, it is only sparks.
     */
    public static void vuurwerk(ServerPlayer p, BlockPos kist) {
        ServerLevel level = p.level();
        long nu = level.getGameTime();
        if (nu - p.getPersistentData().getLongOr(VUURWERK_TIJD, -1000L) < VUURWERK_WACHT) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.ringh1.vuurwerk.wacht").withStyle(ChatFormatting.GRAY));
            return;
        }
        p.getPersistentData().putLong(VUURWERK_TIJD, nu);
        steekAf(level, kist);
        Verhaallijn l = lijn();
        if (Ring.aanZet(p, l, 1) && !l.vlag(p, VUURWERK)) {
            l.vlag(p, VUURWERK, true);
            Ring.behaald(p, "ring_h1_vuurwerk");
            p.sendSystemMessage(Component.translatable("quest.guhs.ringh1.vuurwerk.klus").withStyle(ChatFormatting.LIGHT_PURPLE));
            klusjesKlaar(p);
        } else {
            p.sendOverlayMessage(Component.translatable("quest.guhs.ringh1.vuurwerk.los").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** One rocket from this crate: a trail of sparks up, a coloured burst high above it. Returns where it bursts. */
    public static Vec3 steekAf(ServerLevel level, BlockPos kist) {
        int top = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING, kist.getX(), kist.getZ()), kist.getY() + 1);
        Vec3 knal = new Vec3(kist.getX() + 0.5, top + VUURWERK_HOOG, kist.getZ() + 0.5);
        level.playSound(null, kist, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.2f, 1.0f);
        for (int i = 1; i <= 8; i++) {
            level.sendParticles(ParticleTypes.FIREWORK, knal.x, kist.getY() + 1 + (knal.y - kist.getY() - 1) * i / 8.0, knal.z, 3, 0.05, 0.3, 0.05, 0.01);
        }
        // (a real rocket hurts whoever is within a few blocks of its burst: only when nobody is up there)
        boolean vrij = level.getEntitiesOfClass(Player.class, new AABB(knal, knal).inflate(8), e -> true).isEmpty();
        if (vrij) {
            var random = level.getRandom();
            FireworkExplosion.Shape vorm = FireworkExplosion.Shape.values()[random.nextInt(FireworkExplosion.Shape.values().length)];
            if (vorm == FireworkExplosion.Shape.CREEPER) {
                vorm = FireworkExplosion.Shape.STAR;
            }
            IntList kleuren = IntList.of(KLEUREN[random.nextInt(KLEUREN.length)], KLEUREN[random.nextInt(KLEUREN.length)]);
            ItemStack pijl = new ItemStack(Items.FIREWORK_ROCKET);
            pijl.set(DataComponents.FIREWORKS, new Fireworks(0, List.of(new FireworkExplosion(vorm, kleuren, IntList.of(KLEUREN[random.nextInt(KLEUREN.length)]),
                    random.nextBoolean(), true))));
            level.addFreshEntity(new FireworkRocketEntity(level, knal.x, knal.y - 2, knal.z, pijl));
        } else {
            level.sendParticles(ParticleTypes.FIREWORK, knal.x, knal.y, knal.z, 60, 0.4, 0.4, 0.4, 0.18);
            level.playSound(null, knal.x, knal.y, knal.z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.BLOCKS, 1.5f, 1.0f);
        }
        return knal;
    }

    /** A click on the party table: the second chore; afterwards the daily treat of the Gouw. */
    public static void tafel(ServerPlayer p, BlockPos tafel) {
        Verhaallijn l = lijn();
        ServerLevel level = p.level();
        if (Ring.aanZet(p, l, 1) && !l.vlag(p, TAFEL)) {
            l.vlag(p, TAFEL, true);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, tafel.getX() + 0.5, tafel.getY() + 1.3, tafel.getZ() + 0.5, 10, 0.4, 0.2, 0.4, 0);
            level.playSound(null, tafel, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 1.2f);
            p.sendSystemMessage(Component.translatable("quest.guhs.ringh1.tafel.klus").withStyle(ChatFormatting.LIGHT_PURPLE));
            klusjesKlaar(p);
        } else if (l.klaar(p)) {
            RingBeloning.feest(p, null);
        } else {
            p.sendOverlayMessage(Component.translatable(l.stap(p) < 1 ? "quest.guhs.ringh1.tafel.kijk" : "quest.guhs.ringh1.tafel.straks").withStyle(ChatFormatting.GRAY));
        }
    }

    // =====================================================================================================================
    // Sam-guh
    // =====================================================================================================================

    /** (Sam.BIJ_KLIK) a click on a Sam-guh who waits at home: the third chore, and later the moment he joins. */
    static InteractionResult klikSam(GuhEntity sam, ServerPlayer p, InteractionHand hand) {
        if (Sam.isSam(sam) || !Gouw.isSamThuis(sam)) {
            return InteractionResult.PASS;
        }
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
        sam.playSound(ModSounds.GUH_AMBIENT.get(), 1f, sam.getVoicePitch());
        Verhaallijn l = lijn();
        int stap = l.stap(p);
        if (!l.aanDeBeurt(p) || stap == 0) {
            GuhQuests.say(p, sam, "quest.guhs.ringh1.sam.onbekend");
        } else if (stap == 1) {
            if (l.vlag(p, SAM)) {
                GuhQuests.say(p, sam, "quest.guhs.ringh1.sam.komt_al");
            } else {
                l.vlag(p, SAM, true);
                GuhQuests.say(p, sam, "quest.guhs.ringh1.sam.uitnodiging");
                klusjesKlaar(p);
            }
        } else if (stap == 2) {
            GuhQuests.say(p, sam, "quest.guhs.ringh1.sam.eerst_feest");
        } else if (stap == 3) {
            GuhQuests.say(p, sam, "quest.guhs.ringh1.sam.mee1");
            GuhQuests.say(p, sam, "quest.guhs.ringh1.sam.mee2");
            Sam.roep(p);
            l.verder(p, 3);
        } else {
            GuhQuests.say(p, sam, "quest.guhs.ringh1.sam.komt_al");
        }
        return InteractionResult.SUCCESS;
    }

    /** A click on a provisions crate (soort 0..2): at step 4 Sam-guh packs it, once per kind; all three: on to the portal. */
    public static void proviand(ServerPlayer p, BlockPos krat, int soort) {
        Verhaallijn l = lijn();
        if (!Ring.aanZet(p, l, 4)) {
            p.sendOverlayMessage(Component.translatable(l.aanDeBeurt(p) && l.stap(p) == 3 ? "quest.guhs.ringh1.proviand.zonder_sam" : "quest.guhs.ringh1.proviand.kijk")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        String naam = PROVIAND.get(Math.floorMod(soort, PROVIAND.size()));
        if (l.vlag(p, naam)) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.ringh1.proviand.al").withStyle(ChatFormatting.GRAY));
            return;
        }
        GuhEntity sam = samErbij(p);
        l.vlag(p, naam, true);
        ServerLevel level = p.level();
        level.playSound(null, krat, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 1f, 1f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, krat.getX() + 0.5, krat.getY() + 1.1, krat.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0);
        zeg(p, sam, "quest.guhs.ringh1.sam.proviand." + PROVIAND.indexOf(naam));
        for (String s : PROVIAND) {
            if (!l.vlag(p, s)) {
                return;
            }
        }
        Ring.behaald(p, "ring_h1_proviand");
        if (l.verder(p, 4)) {
            p.sendSystemMessage(Component.translatable("quest.guhs.ringh1.proviand.klaar").withStyle(ChatFormatting.GOLD));
        }
    }

    /** The player's own Sam-guh, next to them (he hops over when he was far; null: he can't be here, e.g. another world). */
    @Nullable
    private static GuhEntity samErbij(ServerPlayer p) {
        GuhEntity sam = Sam.van(p);
        if (sam == null) {
            Sam.roep(p);
            sam = Sam.van(p);
        }
        if (sam != null && sam.distanceToSqr(p) > SAM_BEREIK * SAM_BEREIK) {
            Sam.kom(p);
        }
        return sam;
    }

    private static void zeg(ServerPlayer p, @Nullable Entity spreker, String key) {
        if (spreker != null) {
            GuhQuests.say(p, spreker, key);
        } else {
            p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // =====================================================================================================================
    // the walk to the portal
    // =====================================================================================================================

    /** (once a second per player) at step 5: standing at the grill portal with Sam-guh finishes the chapter. */
    static void portaal(ServerPlayer p) {
        Verhaallijn l = lijn();
        if (!Ring.aanZet(p, l, 5) || Cutscenes.bezig(p) || !bijPortaal(p)) {
            return;
        }
        GuhEntity sam = samErbij(p);
        zeg(p, sam, "quest.guhs.ringh1.portaal.sam1");
        zeg(p, sam, "quest.guhs.ringh1.portaal.sam2");
        Ring.behaald(p, "ring_h1_klaar");
        l.verder(p, 5);
    }

    /** Is this player at a grill portal: a burning one within reach, or the frame of the pit where their story plays? */
    static boolean bijPortaal(ServerPlayer p) {
        ServerLevel level = p.level();
        CompoundTag saved = GuhQuests.saved(p);
        if (saved.contains(PORTAAL) && level.dimension().identifier().toString().equals(saved.getStringOr(THUIS_DIM, ""))) {
            BlockPos frame = BlockPos.of(saved.getLongOr(PORTAAL, 0L));
            if (frame.distToCenterSqr(p.position()) <= PORTAAL_BEREIK * PORTAAL_BEREIK) {
                return true;
            }
        }
        BlockPos hier = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(hier.offset(-3, -2, -3), hier.offset(3, 3, 3))) {
            if (level.getBlockState(pos).is(BarbecuetherFeature.BARBECUETHER_PORTAAL.get())) {
                return true;
            }
        }
        // (a pit the player did not start at: its frame counts too)
        BlockPos frame = Gouw.frame(level, hier);
        return frame != null && frame.distToCenterSqr(p.position()) <= PORTAAL_BEREIK * PORTAAL_BEREIK;
    }

    // =====================================================================================================================
    // the residents
    // =====================================================================================================================

    public static final int BEWONER_ZINNEN = 8;

    /** (GuhHooks.klik) a click on a resident of the Knabbelgouw: a word about life in the Gouw; never fed, tamed or dressed. */
    static InteractionResult klikBewoner(GuhEntity guh, Player player, InteractionHand hand) {
        int nr = Gouw.bewoner(guh);
        if (nr < 0) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer p && hand == InteractionHand.MAIN_HAND) {
            long nu = p.level().getGameTime();
            if (nu - p.getPersistentData().getLongOr("guhs_ringh1_praat", 0L) > 20) {
                p.getPersistentData().putLong("guhs_ringh1_praat", nu);
                GuhQuests.say(p, guh, "quest.guhs.ringh1.bewoner." + Math.floorMod(nr * 2 + (int) (nu / 600), BEWONER_ZINNEN));
                guh.playSound(ModSounds.GUH_AMBIENT.get(), 1f, guh.getVoicePitch());
                Verhaallijn l = lijn();
                l.vlag(p, "bewoner_" + nr, true);
                List<Integer> gesproken = new ArrayList<>();
                for (int i = 0; i < Gouw.BEWONERS.size(); i++) {
                    if (l.vlag(p, "bewoner_" + i)) {
                        gesproken.add(i);
                    }
                }
                if (gesproken.size() >= Gouw.BEWONERS.size()) {
                    Ring.behaald(p, "ring_h1_bewoners");
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    private Feest() {
    }
}
