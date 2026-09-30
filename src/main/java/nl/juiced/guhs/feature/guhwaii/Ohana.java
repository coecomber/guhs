package nl.juiced.guhs.feature.guhwaii;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The ohana questline (per player: {@code GuhQuests.saved(p)["guhs_guhwaii_stap"]}, + the gifts and the rommeltjes):
 * <ol start="0">
 *   <li>{@link #NIEUW}: Lilo-guh and Nani-guh in their stilt house: "Er viel gisteravond een ster op de heuvel!"</li>
 *   <li>{@link #CAPSULE}: find the crashed capsule on the island's hill (the vadsigheid-scanner stands in it);</li>
 *   <li>{@link #TERUG}: back to Lilo: the little blue alien guh with four arms is in the asiel... she adopts him! 626-guh
 *       "helps" in the house and makes a big mess (only mess, nothing broken);</li>
 *   <li>{@link #OPRUIMEN}: help Nani-guh clean up {@link #ROMMEL_NODIG} rommeltjes (click them);</li>
 *   <li>{@link #LIEF}: help 626-guh be good: give him three lovely things (a kokosnoot, a roze hibiscus, kaasknabbels);</li>
 *   <li>{@link #OHANA}: back to Lilo: "Ohana betekent familie. Familie betekent dat niemand wordt achtergelaten... of vergeten.
 *       Njeg." The rewards: the poster, the ukelele, the four outfit pieces, and 626-guh released for you;</li>
 *   <li>{@link #KLAAR}: give 626-guh a kaasknabbel and he comes home with you (once per player, {@link VerhaalGuhs#tem}).</li>
 * </ol>
 * Everything in the chat is Dutch, cosy and lief: 626-guh only ever makes mess, never hurts anybody.
 */
public final class Ohana {
    public static final String STAP = "guhs_guhwaii_stap", GAVEN = "guhs_guhwaii_gaven", ROMMEL = "guhs_guhwaii_rommel";
    public static final int NIEUW = 0, CAPSULE = 1, TERUG = 2, OPRUIMEN = 3, LIEF = 4, OHANA = 5, KLAAR = 6;
    public static final int ROMMEL_NODIG = 5;
    public static final int GAVE_KOKOS = 1, GAVE_BLOEM = 2, GAVE_KNABBELS = 4, ALLE_GAVEN = 7;
    /** How many kaasknabbels 626 wants (one bag of the stack). */
    public static final int KNABBELS = 8;
    /** How far from Nani-guh the rommeltjes lie. */
    public static final int ROMMEL_STRAAL = 9;
    public static final String SCENE = "guhwaii_ohana";

    /** Lilo-guh (the default role of LILO_GUH; guhwaiispellen gives the Lilo's at the surf beach their own plek role). */
    public static final NpcRole LILO = Ohana::lilo;
    /** Nani-guh: her big sister (tidy, a bit stern, very lief). */
    public static final NpcRole NANI = Ohana::nani;

    private Ohana() {
    }

    // =================================================================================================================
    // progress
    // =================================================================================================================

    public static int stap(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(STAP, 0);
    }

    public static void zet(ServerPlayer p, int stap) {
        GuhQuests.saved(p).putInt(STAP, stap);
    }

    public static int gaven(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(GAVEN, 0);
    }

    public static int rommel(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(ROMMEL, 0);
    }

    /** (tests / ops) start over. */
    public static void vergeet(ServerPlayer p) {
        CompoundTag s = GuhQuests.saved(p);
        s.remove(STAP);
        s.remove(GAVEN);
        s.remove(ROMMEL);
        VerhaalGuhs.vergeet(p, VerhaalGuh.STITCH626);
    }

    // =================================================================================================================
    // Lilo-guh
    // =================================================================================================================

    static void lilo(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel level = p.level();
        npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.35f);
        Entity nani = dichtste(level, npc, GuhNpcEntity.Kind.NANI_GUH);
        Entity stitch = kopie(level, npc.position());
        switch (stap(p)) {
            case NIEUW -> {
                Praat.scene(p, SCENE, List.of(
                        regel(npc, "", "gui.guhs.guhwaii.lilo.intro.1"),
                        regel(nani, "entity.guhs.guh_npc.nani_guh", "gui.guhs.guhwaii.lilo.intro.2"),
                        regel(npc, "", "gui.guhs.guhwaii.lilo.intro.3"),
                        regel(npc, "", "gui.guhs.guhwaii.lilo.intro.4")));
                zet(p, CAPSULE);
                GuhwaiiFeature.advancement(p, "aloha");
                GuhQuests.hint(p, "gui.guhs.guhwaii.hint.capsule");
            }
            case CAPSULE -> GuhQuests.say(p, npc, "gui.guhs.guhwaii.lilo.capsule");
            case TERUG -> {
                Praat.scene(p, SCENE, List.of(
                        regel(npc, "", "gui.guhs.guhwaii.lilo.adoptie.1"),
                        regel(npc, "", "gui.guhs.guhwaii.lilo.adoptie.2"),
                        regel(nani, "entity.guhs.guh_npc.nani_guh", "gui.guhs.guhwaii.lilo.adoptie.3"),
                        regel(npc, "", "gui.guhs.guhwaii.lilo.adoptie.4"),
                        regel(stitch, "entity.guhs.guh.stitch626", "gui.guhs.guhwaii.lilo.adoptie.5"),
                        regel(nani, "entity.guhs.guh_npc.nani_guh", "gui.guhs.guhwaii.lilo.adoptie.6")));
                zet(p, OPRUIMEN);
                GuhQuests.saved(p).putInt(ROMMEL, 0);
                GuhwaiiFeature.advancement(p, "adoptie");
                strooiRommel(level, nani != null ? nani.blockPosition() : npc.blockPosition());
                if (stitch instanceof GuhEntity s) {
                    blij(s, Emote.ROLLEN);
                }
                GuhQuests.hint(p, "gui.guhs.guhwaii.hint.opruimen");
            }
            case OPRUIMEN -> GuhQuests.say(p, npc, "gui.guhs.guhwaii.lilo.opruimen", rommel(p), ROMMEL_NODIG);
            case LIEF -> {
                GuhQuests.say(p, npc, "gui.guhs.guhwaii.lilo.lief");
                lijstje(p);
            }
            case OHANA -> ohana(p, npc, nani, stitch);
            default -> {
                String[] tips = {"gui.guhs.guhwaii.lilo.klaar.1", "gui.guhs.guhwaii.lilo.klaar.2", "gui.guhs.guhwaii.lilo.klaar.3",
                        "gui.guhs.guhwaii.lilo.klaar.4"};
                String tip = tips[p.getRandom().nextInt(tips.length)];
                if (tip.endsWith(".4") && !nl.juiced.guhs.feature.guhwaiispellen.Surfplek.open(p)) {
                    tip = tips[0];
                }
                GuhQuests.say(p, npc, tip);
                if (!VerhaalGuhs.heeftGetemd(p, VerhaalGuh.STITCH626)) {
                    GuhQuests.hint(p, "gui.guhs.guhwaii.hint.temmen");
                }
            }
        }
    }

    /** The big moment: Lilo, Nani and 626 together, the quote, and the rewards. */
    static void ohana(ServerPlayer p, GuhNpcEntity lilo, @Nullable Entity nani, @Nullable Entity stitch) {
        Praat.scene(p, SCENE, List.of(
                regel(lilo, "", "gui.guhs.guhwaii.ohana.1"),
                regel(stitch, "entity.guhs.guh.stitch626", "gui.guhs.guhwaii.ohana.2"),
                regel(lilo, "", "gui.guhs.guhwaii.ohana.3"),
                regel(lilo, "", "gui.guhs.guhwaii.ohana.citaat"),
                regel(nani, "entity.guhs.guh_npc.nani_guh", "gui.guhs.guhwaii.ohana.4"),
                regel(stitch, "entity.guhs.guh.stitch626", "gui.guhs.guhwaii.ohana.5")));
        GuhQuests.say(p, lilo, "gui.guhs.guhwaii.ohana.citaat");
        beloning(p);
        if (stitch instanceof GuhEntity s) {
            blij(s, Emote.KNUFFELEN);
        }
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.HEART, lilo.getX(), lilo.getY() + 1.2, lilo.getZ(), 12, 1.2, 0.6, 1.2, 0.05);
        level.sendParticles(ParticleTypes.FIREWORK, lilo.getX(), lilo.getY() + 2.5, lilo.getZ(), 30, 1.5, 0.8, 1.5, 0.08);
        level.playSound(null, lilo.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.3f);
    }

    /** The rewards of the ohana: 626-guh released, the poster, the ukelele, the four outfit pieces, kaasknabbels. */
    static void beloning(ServerPlayer p) {
        zet(p, KLAAR);
        VerhaalGuhs.geefVrij(p, VerhaalGuh.STITCH626);
        Minigames.give(p, new ItemStack(GuhwaiiFeature.VADSIGHEID_POSTER.get()));
        Minigames.give(p, new ItemStack(GuhwaiiFeature.UKELELE.get()));
        for (GuhClothes c : List.of(GuhClothes.GUHWAII_HULAROKJE, GuhClothes.GUHWAII_BLOEMENKRANS, GuhClothes.GUHWAII_STITCHOREN,
                GuhClothes.GUHWAII_SURFPLANKJE)) {
            Minigames.give(p, new ItemStack(ModItems.clothingItem(c)));
        }
        Minigames.give(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
        GuhwaiiFeature.advancement(p, "ohana");
        GuhQuests.hint(p, "gui.guhs.guhwaii.hint.temmen");
    }

    // =================================================================================================================
    // Nani-guh
    // =================================================================================================================

    static void nani(GuhNpcEntity npc, ServerPlayer p) {
        npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.1f);
        switch (stap(p)) {
            case NIEUW, CAPSULE, TERUG -> GuhQuests.say(p, npc, "gui.guhs.guhwaii.nani.begin");
            case OPRUIMEN -> {
                int nodig = ROMMEL_NODIG - rommel(p);
                int liggen = strooiRommel(p.level(), npc.blockPosition());
                GuhQuests.say(p, npc, "gui.guhs.guhwaii.nani.opruimen", nodig);
                if (liggen == 0) {
                    GuhQuests.hint(p, "gui.guhs.guhwaii.hint.opruimen");
                }
            }
            case LIEF -> GuhQuests.say(p, npc, "gui.guhs.guhwaii.nani.lief");
            case OHANA -> GuhQuests.say(p, npc, "gui.guhs.guhwaii.nani.ohana");
            default -> GuhQuests.say(p, npc, p.getRandom().nextBoolean() ? "gui.guhs.guhwaii.nani.klaar.1" : "gui.guhs.guhwaii.nani.klaar.2");
        }
    }

    // =================================================================================================================
    // the capsule, the rommeltjes, the gifts
    // =================================================================================================================

    /** (the player tick) the player stands in 626's capsule. */
    public static void inCapsule(ServerPlayer p) {
        if (stap(p) == CAPSULE) {
            zet(p, TERUG);
            GuhQuests.hint(p, "gui.guhs.guhwaii.hint.capsule_gevonden");
            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1f, 0.8f);
        }
        GuhwaiiFeature.advancement(p, "capsule");
    }

    /**
     * Scatters rommeltjes around this spot (Nani-guh's kitchen and veranda) until {@link #ROMMEL_NODIG} lie there; returns
     * how many lie there now.
     */
    public static int strooiRommel(ServerLevel level, BlockPos rond) {
        List<BlockPos> al = new ArrayList<>();
        for (BlockPos q : BlockPos.betweenClosed(rond.offset(-ROMMEL_STRAAL, -3, -ROMMEL_STRAAL), rond.offset(ROMMEL_STRAAL, 3, ROMMEL_STRAAL))) {
            if (level.getBlockState(q).is(GuhwaiiFeature.ROMMELTJE.get())) {
                al.add(q.immutable());
            }
        }
        int tries = 0;
        while (al.size() < ROMMEL_NODIG && tries++ < 200) {
            BlockPos q = rond.offset(level.getRandom().nextInt(ROMMEL_STRAAL * 2 + 1) - ROMMEL_STRAAL, level.getRandom().nextInt(3) - 1,
                    level.getRandom().nextInt(ROMMEL_STRAAL * 2 + 1) - ROMMEL_STRAAL);
            BlockState st = GuhwaiiFeature.ROMMELTJE.get().defaultBlockState().setValue(GuhwaiiBlokken.SOORT, level.getRandom().nextInt(4));
            if (level.isEmptyBlock(q) && level.isEmptyBlock(q.above()) && st.canSurvive(level, q) && level.getFluidState(q).isEmpty()
                    && al.stream().noneMatch(a -> a.distManhattan(q) < 2) && !level.getBlockState(q.below()).is(GuhwaiiFeature.ROMMELTJE.get())) {
                level.setBlock(q, st, Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.POOF, q.getX() + 0.5, q.getY() + 0.3, q.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.01);
                al.add(q);
            }
        }
        return al.size();
    }

    /** A click on a rommeltje: it's cleaned up (poof) and counted. */
    public static void opgeruimd(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        if (!level.getBlockState(pos).is(GuhwaiiFeature.ROMMELTJE.get())) {
            return;
        }
        level.removeBlock(pos, false);
        telOpgeruimd(p, pos);
    }

    /** A rommeltje was cleaned up by p (clicked or broken). */
    public static void telOpgeruimd(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 6, 0.25, 0.15, 0.25, 0.02);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.02);
        level.playSound(null, pos, SoundEvents.BRUSH_SAND_COMPLETED, SoundSource.BLOCKS, 0.8f, 1.2f);
        if (stap(p) != OPRUIMEN) {
            return;
        }
        int n = rommel(p) + 1;
        GuhQuests.saved(p).putInt(ROMMEL, n);
        Entity nani = dichtste(level, p, GuhNpcEntity.Kind.NANI_GUH);
        if (n >= ROMMEL_NODIG) {
            zet(p, LIEF);
            GuhwaiiFeature.advancement(p, "opgeruimd");
            if (nani != null) {
                GuhQuests.say(p, nani, "gui.guhs.guhwaii.nani.opgeruimd");
            }
            lijstje(p);
        } else if (nani != null) {
            GuhQuests.say(p, nani, "gui.guhs.guhwaii.nani.nog", ROMMEL_NODIG - n);
        } else {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.guhwaii.rommel.teller", n, ROMMEL_NODIG));
        }
    }

    /** What 626-guh still wants (the three lovely things). */
    static void lijstje(ServerPlayer p) {
        int g = gaven(p);
        p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.guhwaii.lijstje",
                vinkje(g, GAVE_KOKOS), vinkje(g, GAVE_BLOEM), vinkje(g, GAVE_KNABBELS), KNABBELS));
    }

    private static String vinkje(int gaven, int gave) {
        return (gaven & gave) != 0 ? "✔" : "✿";
    }

    /** A click on the story copy of 626-guh (any hand item): a word, a gift during LIEF, and after the ohana: taming. */
    static InteractionResult klik626(GuhEntity kopie, ServerPlayer p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = p.getItemInHand(hand);
        int stap = stap(p);
        switch (stap) {
            case LIEF -> {
                int gave = stack.is(GuhwaiiFeature.KOKOSNOOT_ITEM.get()) ? GAVE_KOKOS : stack.is(GuhwaiiFeature.ROZE_HIBISCUS.get().asItem()) ? GAVE_BLOEM
                        : stack.is(ModItems.KAAS_KNABBELS.get()) && stack.getCount() >= KNABBELS ? GAVE_KNABBELS : 0;
                if (stack.is(ModItems.KAAS_KNABBELS.get()) && gave == 0) {
                    GuhQuests.say(p, kopie, "gui.guhs.guhwaii.626.meer_knabbels", KNABBELS);
                    return InteractionResult.SUCCESS;
                }
                return geef(kopie, p, stack, gave);
            }
            case KLAAR -> {
                if (stack.is(ModItems.KAAS_KNABBELS.get()) && VerhaalGuhs.magTemmen(p, VerhaalGuh.STITCH626)) {
                    Vec3 waar = kopie.position().add(p.position().subtract(kopie.position()).normalize().scale(1.2));
                    GuhEntity eigen = VerhaalGuhs.tem(p, VerhaalGuh.STITCH626, waar);
                    if (eigen != null) {
                        if (!p.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                        eigen.setCustomName(null);
                        GuhQuests.say(p, eigen, "gui.guhs.guhwaii.626.getemd");
                        blij(eigen, Emote.VAHOEG);
                        blij(kopie, Emote.ZWAAIEN);
                        return InteractionResult.SUCCESS;
                    }
                }
                GuhQuests.say(p, kopie, VerhaalGuhs.heeftGetemd(p, VerhaalGuh.STITCH626) ? "gui.guhs.guhwaii.626.al_thuis"
                        : "gui.guhs.guhwaii.626.knabbel");
                return InteractionResult.SUCCESS;
            }
            default -> {
                String key = switch (stap) {
                    case NIEUW, CAPSULE -> "gui.guhs.guhwaii.626.vreemd";
                    case TERUG -> "gui.guhs.guhwaii.626.asiel";
                    case OPRUIMEN -> "gui.guhs.guhwaii.626.rommel";
                    default -> "gui.guhs.guhwaii.626.ohana";
                };
                kopie.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.5f);
                GuhQuests.say(p, kopie, key);
                return InteractionResult.SUCCESS;
            }
        }
    }

    /** A gift for 626-guh during LIEF. */
    static InteractionResult geef(GuhEntity kopie, ServerPlayer p, ItemStack stack, int gave) {
        int gaven = gaven(p);
        if (gave == 0) {
            GuhQuests.say(p, kopie, "gui.guhs.guhwaii.626.wat_lief");
            lijstje(p);
            return InteractionResult.SUCCESS;
        }
        if ((gaven & gave) != 0) {
            GuhQuests.say(p, kopie, "gui.guhs.guhwaii.626.al_gehad");
            return InteractionResult.SUCCESS;
        }
        if (!p.getAbilities().instabuild) {
            stack.shrink(gave == GAVE_KNABBELS ? KNABBELS : 1);
        }
        gaven |= gave;
        GuhQuests.saved(p).putInt(GAVEN, gaven);
        String key = gave == GAVE_KOKOS ? "gui.guhs.guhwaii.626.gave.kokos" : gave == GAVE_BLOEM ? "gui.guhs.guhwaii.626.gave.bloem"
                : "gui.guhs.guhwaii.626.gave.knabbels";
        GuhQuests.say(p, kopie, key);
        kopie.playSound(ModSounds.GUH_EAT.get(), 1f, 1.3f);
        p.level().sendParticles(ParticleTypes.HEART, kopie.getX(), kopie.getY() + 1.0, kopie.getZ(), 5, 0.4, 0.3, 0.4, 0.02);
        blij(kopie, gave == GAVE_BLOEM ? Emote.VERLEGEN : Emote.SMAKKEN);
        if (gaven == ALLE_GAVEN) {
            zet(p, OHANA);
            GuhwaiiFeature.advancement(p, "lief");
            GuhQuests.say(p, kopie, "gui.guhs.guhwaii.626.lief_klaar");
            GuhQuests.hint(p, "gui.guhs.guhwaii.hint.ohana");
        } else {
            lijstje(p);
        }
        return InteractionResult.SUCCESS;
    }

    /** (the scenes) nothing to do when they're read: the step already moved when they opened. */
    static void luisteraars() {
        Praat.luister(SCENE, (p, spreker, optie) -> {
            if (optie < 0) {
                Entity stitch = kopie(p.level(), p.position());
                if (stitch instanceof GuhEntity s && stap(p) >= OPRUIMEN) {
                    blij(s, Emote.VAHOEG);
                }
            }
        });
    }

    // =================================================================================================================
    // helpers
    // =================================================================================================================

    private static Praat.Regel regel(@Nullable Entity spreker, String naamKey, String tekst, Object... args) {
        return new Praat.Regel(spreker, spreker == null && naamKey.isEmpty() ? "entity.guhs.guh_npc.lilo_guh" : naamKey, tekst, args);
    }

    /** The nearest NPC of that kind within 32 blocks. */
    @Nullable
    static GuhNpcEntity dichtste(ServerLevel level, Entity bij, GuhNpcEntity.Kind kind) {
        return level.getEntitiesOfClass(GuhNpcEntity.class, bij.getBoundingBox().inflate(32), n -> n.getKind() == kind).stream()
                .min(Comparator.comparingDouble(n -> n.distanceToSqr(bij))).orElse(null);
    }

    /** The nearest story copy of 626-guh within 32 blocks. */
    @Nullable
    static GuhEntity kopie(ServerLevel level, Vec3 bij) {
        return level.getEntitiesOfClass(GuhEntity.class, new AABB(bij, bij).inflate(32), g -> VerhaalGuhs.kopieVan(g) == VerhaalGuh.STITCH626).stream()
                .min(Comparator.comparingDouble(g -> g.position().distanceToSqr(bij))).orElse(null);
    }

    static void blij(GuhEntity guh, Emote emote) {
        if (GuhEmotes.canStart(guh)) {
            guh.emotes.start(emote, false, GuhEmotes.Source.SELF);
        }
    }
}
