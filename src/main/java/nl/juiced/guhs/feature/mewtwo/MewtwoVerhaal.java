package nl.juiced.guhs.feature.mewtwo;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * The questline of the kloon-eiland (per player, {@link MewtwoVoortgang}):
 * <ol>
 *   <li>Professor Knabbelkloon tells the story in bits and asks for his 6 lab notes ({@link Knabbelkloon}); every note spot
 *       gives its note once ({@link #vindNotitie}), reading it shows its bit of the story ({@link #leesNotitie});</li>
 *   <li>with all 6 he remembers (the trainerpetje) and asks for the 4 tank parts: each crate gives its part once
 *       ({@link #vindOnderdeel}), a click on the kloontank builds in what you carry ({@link #klikTank});</li>
 *   <li>the tank bubbles again and Mieuwguh appears, giggling ({@link #tankGerepareerd}; the trainerpakje);</li>
 *   <li>the big knabbel meal: a double portion (32 kaas knabbels + 2 snacks) in the grote knabbelschaal on the arena
 *       ({@link #klikSchaal}); Guhtwo and Mieuwguh eat together and become best friends ({@link #maaltijd}; the staartje and
 *       the ballonnetje); {@code VerhaalGuhs.geefVrij(MEWTWO)};</li>
 *   <li>a click on the Guhtwo in the arena: "May I come with you?" -> your own Guhtwo, once ({@link #klikKopie}).</li>
 * </ol>
 * Visible advancements verhalen/mewtwo_*, hidden quest/mewtwo_* for FTB ({@link #adv}).
 */
public final class MewtwoVerhaal {
    public static final String INTRO = "mewtwo_intro", SCENE = "mewtwo_scene", TEM = "mewtwo_tem";

    /** Both advancements of a step: the hidden quest/&lt;name&gt; (FTB) and the visible verhalen/&lt;name&gt; (when it exists). */
    public static void adv(ServerPlayer p, String name) {
        GuhAdvancements.grant(p, name);
        GidsFeature.grant(p, "verhalen/" + name);
    }

    static void luisteraars() {
        Praat.luister(INTRO, (p, spreker, optie) -> {
            if (optie == 1 && MewtwoVoortgang.stap(p) == MewtwoVoortgang.NIEUW) {
                begin(p);
                Praat.sluit(p);
            } else if (optie == 2) {
                if (spreker != null) {
                    GuhQuests.say(p, spreker, "gui.guhs.mewtwo.prof.later");
                }
                Praat.sluit(p);
            }
        });
        Praat.luister(SCENE, (p, spreker, optie) -> {
        });
        Praat.luister(TEM, (p, spreker, optie) -> {
            if (optie == 1) {
                tem(p, spreker instanceof GuhEntity kopie ? kopie : null);
                Praat.sluit(p);
            } else if (optie == 2) {
                if (spreker != null) {
                    GuhQuests.say(p, spreker, "gui.guhs.mewtwo.tem.later");
                }
                Praat.sluit(p);
            }
        });
    }

    /** "I'll look for them!": step NOTITIES. */
    public static void begin(ServerPlayer p) {
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.NOTITIES);
        adv(p, "mewtwo_welkom");
        p.sendSystemMessage(Component.translatable("gui.guhs.mewtwo.prof.zoek", MewtwoVoortgang.aantalNotities(p),
                MewtwoFeature.NOTITIES - MewtwoVoortgang.aantalNotities(p), Component.translatable(plekNotitie(p))).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =================================================================================================================
    // the lab notes
    // =================================================================================================================

    /** A note spot was clicked: its note, once per player (and its text); a second time: read it again. */
    public static void vindNotitie(ServerPlayer p, BlockPos spot, int n) {
        ServerLevel level = p.level();
        if (MewtwoVoortgang.vondNotitie(p, n)) {
            Minigames.give(p, LabnotitieItem.maak(n));
            level.playSound(null, spot, MewtwoFeature.NOTITIE.get(), SoundSource.PLAYERS, 1f, 1.1f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spot.getX() + 0.5, spot.getY() + 0.4, spot.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
            p.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.notitie.gevonden", n, MewtwoVoortgang.aantalNotities(p))
                    .withStyle(ChatFormatting.GOLD));
            leesNotitie(p, n);
            if (MewtwoVoortgang.aantalNotities(p) == MewtwoFeature.NOTITIES) {
                p.sendSystemMessage(Component.translatable("gui.guhs.mewtwo.notitie.alle").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        } else {
            p.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.notitie.al").withStyle(ChatFormatting.GRAY));
            if (!heeftItem(p, LabnotitieItem.class, n)) {
                Minigames.give(p, LabnotitieItem.maak(n));      // (lost it? here's the note again)
            }
            leesNotitie(p, n);
        }
    }

    /** Shows the text of note n (a page without a speaker). */
    public static void leesNotitie(ServerPlayer p, int n) {
        Praat.scene(p, SCENE, List.of(new Praat.Regel(null, "item.guhs.mewtwo_labnotitie", "gui.guhs.mewtwo.notitie." + n)));
    }

    private static boolean heeftItem(ServerPlayer p, Class<?> soort, int n) {
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (soort.isInstance(s.getItem()) && (s.getItem() instanceof LabnotitieItem ? LabnotitieItem.nummer(s) : TankonderdeelItem.soort(s)) == n) {
                return true;
            }
        }
        return false;
    }

    /** The first note this player still misses (1..6; 1 when they have them all). */
    public static int eersteNotitie(ServerPlayer p) {
        for (int n = 1; n <= MewtwoFeature.NOTITIES; n++) {
            if (!MewtwoVoortgang.heeftNotitie(p, n)) {
                return n;
            }
        }
        return 1;
    }

    /** Where the first note this player still misses lies (lang key). */
    public static String plekNotitie(ServerPlayer p) {
        return "gui.guhs.mewtwo.plek.notitie." + eersteNotitie(p);
    }

    /** The first part this player hasn't built in and doesn't carry (1..4). */
    public static int eersteOnderdeel(ServerPlayer p) {
        for (int n = 1; n <= MewtwoFeature.ONDERDELEN; n++) {
            if (!MewtwoVoortgang.isIngebouwd(p, n) && !heeftItem(p, TankonderdeelItem.class, n)) {
                return n;
            }
        }
        return 1;
    }

    /** All six notes: the professor remembers (the scene), the trainerpetje, step ONDERDELEN. */
    public static void notitiesKlaar(ServerPlayer p, @Nullable Entity prof) {
        if (MewtwoVoortgang.stap(p) != MewtwoVoortgang.NOTITIES || MewtwoVoortgang.aantalNotities(p) < MewtwoFeature.NOTITIES) {
            return;
        }
        // bbq2 (oude-scenes): the first time, what he remembers plays as a camera scene; the rest comes after it
        if (nl.juiced.guhs.feature.oudescenes.OudeScenes.speel(p, nl.juiced.guhs.feature.oudescenes.OudeScenes.MEWTWO,
                prof != null ? prof.blockPosition() : p.blockPosition(), s -> notitiesKlaar(s, prof))) {
            return;
        }
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.ONDERDELEN);
        adv(p, "mewtwo_notities");
        geef(p, GuhClothes.MEWTWO_TRAINERPETJE);
        Minigames.give(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 8));
        feestje(p, prof);
        List<Praat.Regel> pag = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            pag.add(new Praat.Regel(prof, "gui.guhs.mewtwo.naam.prof", "gui.guhs.mewtwo.prof.notities." + i));
        }
        Praat.scene(p, SCENE, pag);
    }

    // =================================================================================================================
    // the tank parts and the kloontank
    // =================================================================================================================

    /** A parts crate was clicked: its part, once (only while the professor asks for them). */
    public static void vindOnderdeel(ServerPlayer p, BlockPos spot, int n) {
        int stap = MewtwoVoortgang.stap(p);
        if (stap < MewtwoVoortgang.ONDERDELEN) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.kist.nog_niet").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (stap > MewtwoVoortgang.ONDERDELEN) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.kist.klaar").withStyle(ChatFormatting.GRAY));
            return;
        }
        ServerLevel level = p.level();
        boolean nieuw = MewtwoVoortgang.vondOnderdeel(p, n);
        if (!nieuw && (MewtwoVoortgang.isIngebouwd(p, n) || heeftItem(p, TankonderdeelItem.class, n))) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.kist.al").withStyle(ChatFormatting.GRAY));
            return;
        }
        ItemStack deel = TankonderdeelItem.maak(n);
        Minigames.give(p, deel);
        level.playSound(null, spot, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.8f, 1.2f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spot.getX() + 0.5, spot.getY() + 0.8, spot.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
        p.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.kist.gevonden", deel.getHoverName()).withStyle(ChatFormatting.GOLD));
    }

    /** The kloontank was clicked: build in the parts you carry; with all four it's repaired. */
    public static void klikTank(ServerPlayer p, BlockPos tank) {
        int stap = MewtwoVoortgang.stap(p);
        if (stap >= MewtwoVoortgang.MAALTIJD) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.tank.heel").withStyle(ChatFormatting.LIGHT_PURPLE));
            p.level().playSound(null, tank, MewtwoFeature.TANK_BORREL.get(), SoundSource.BLOCKS, 0.8f, 1.1f);
            return;
        }
        if (stap < MewtwoVoortgang.ONDERDELEN) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.tank.kapot").withStyle(ChatFormatting.GRAY));
            return;
        }
        ServerLevel level = p.level();
        boolean iets = false;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.getItem() instanceof TankonderdeelItem) {
                int n = TankonderdeelItem.soort(s);
                if (n >= 1 && n <= MewtwoFeature.ONDERDELEN && !MewtwoVoortgang.isIngebouwd(p, n)) {
                    Component naam = s.getHoverName();
                    s.shrink(1);
                    MewtwoVoortgang.vondOnderdeel(p, n);
                    MewtwoVoortgang.bouwIn(p, n);
                    iets = true;
                    level.playSound(null, tank, MewtwoFeature.TANK_KLIK.get(), SoundSource.BLOCKS, 1f, 1f + n * 0.1f);
                    level.sendParticles(MewtwoFeature.GLOED.get(), tank.getX() + 0.5, tank.getY() + 1.5, tank.getZ() + 0.5, 12, 0.8, 0.8, 0.8, 0.02);
                    p.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.tank.ingebouwd", naam,
                            MewtwoFeature.ONDERDELEN - MewtwoVoortgang.aantalIngebouwd(p)).withStyle(ChatFormatting.GOLD));
                }
            }
        }
        if (MewtwoVoortgang.aantalIngebouwd(p) >= MewtwoFeature.ONDERDELEN) {
            tankGerepareerd(p, tank);
        } else if (!iets) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.tank.mist", MewtwoFeature.ONDERDELEN - MewtwoVoortgang.aantalIngebouwd(p))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** The last part clicks in: the tank bubbles, Mieuwguh appears giggling; the trainerpakje; step MAALTIJD. */
    public static void tankGerepareerd(ServerPlayer p, BlockPos tank) {
        if (MewtwoVoortgang.stap(p) != MewtwoVoortgang.ONDERDELEN) {
            return;
        }
        ServerLevel level = p.level();
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.MAALTIJD);
        adv(p, "mewtwo_tank");
        level.playSound(null, tank, MewtwoFeature.TANK_HEEL.get(), SoundSource.BLOCKS, 1f, 1.2f);
        level.playSound(null, tank, MewtwoFeature.TANK_BORREL.get(), SoundSource.BLOCKS, 1f, 1f);
        level.sendParticles(MewtwoFeature.BUBBEL.get(), tank.getX() + 0.5, tank.getY() + 1.5, tank.getZ() + 0.5, 40, 0.7, 1.0, 0.7, 0.05);
        // Mieuwguh comes to look what all that bubbling is
        MewEntity mew = null;
        List<MewEntity> er = level.getEntitiesOfClass(MewEntity.class, new AABB(tank).inflate(24));
        if (!er.isEmpty()) {
            mew = er.get(0);
            mew.giechel();
        } else {
            BlockPos thuis = MewSpawner.eiland(level, tank);
            mew = MewSpawner.spawn(level, tank.above(4), thuis != null ? thuis : tank.above(10), true);
        }
        adv(p, "mewtwo_mew");
        geef(p, GuhClothes.MEWTWO_TRAINERPAKJE);
        Minigames.give(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 8));
        Entity prof = professor(level, tank);
        Praat.scene(p, SCENE, List.of(
                new Praat.Regel(null, "gui.guhs.mewtwo.naam.verteller", "gui.guhs.mewtwo.tank.scene.1"),
                new Praat.Regel(mew, "gui.guhs.mewtwo.naam.mew", "gui.guhs.mewtwo.tank.scene.2"),
                new Praat.Regel(prof, "gui.guhs.mewtwo.naam.prof", "gui.guhs.mewtwo.tank.scene.3"),
                new Praat.Regel(prof, "gui.guhs.mewtwo.naam.prof", "gui.guhs.mewtwo.tank.scene.4"),
                new Praat.Regel(prof, "gui.guhs.mewtwo.naam.prof", "gui.guhs.mewtwo.tank.scene.5")));
    }

    // =================================================================================================================
    // the big knabbel meal
    // =================================================================================================================

    /** The grote knabbelschaal was clicked: in go kaas knabbels and snacks, until the double portion is there. */
    public static void klikSchaal(ServerPlayer p, BlockPos schaal) {
        int stap = MewtwoVoortgang.stap(p);
        if (stap < MewtwoVoortgang.MAALTIJD) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.schaal.nog_niet").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (stap > MewtwoVoortgang.MAALTIJD) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.schaal.klaar").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        int knabbels = MewtwoVoortgang.knabbels(p), snacks = MewtwoVoortgang.snacks(p);
        boolean iets = false;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (knabbels < MewtwoFeature.PORTIE_KNABBELS && s.is(ModItems.KAAS_KNABBELS.get())) {
                int n = Math.min(s.getCount(), MewtwoFeature.PORTIE_KNABBELS - knabbels);
                s.shrink(n);
                knabbels += n;
                iets = true;
            } else if (snacks < MewtwoFeature.PORTIE_SNACKS && s.is(BandFeature.SNACKS)) {
                int n = Math.min(s.getCount(), MewtwoFeature.PORTIE_SNACKS - snacks);
                s.shrink(n);
                snacks += n;
                iets = true;
            }
        }
        MewtwoVoortgang.zetSchaal(p, knabbels, snacks);
        ServerLevel level = p.level();
        if (iets) {
            level.playSound(null, schaal, SoundEvents.GENERIC_EAT.value(), SoundSource.BLOCKS, 0.6f, 1.3f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, schaal.getX() + 0.5, schaal.getY() + 0.9, schaal.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.0);
        }
        if (knabbels >= MewtwoFeature.PORTIE_KNABBELS && snacks >= MewtwoFeature.PORTIE_SNACKS) {
            maaltijd(p, schaal);
        } else {
            p.sendOverlayMessage(Component.translatable(iets ? "gui.guhs.mewtwo.schaal.erin" : "gui.guhs.mewtwo.schaal.meer",
                    iets ? knabbels : MewtwoFeature.PORTIE_KNABBELS - knabbels, iets ? MewtwoFeature.PORTIE_KNABBELS : MewtwoFeature.PORTIE_SNACKS - snacks,
                    snacks, MewtwoFeature.PORTIE_SNACKS).withStyle(ChatFormatting.GOLD));
        }
    }

    /** The double portion is there: Guhtwo and Mieuwguh eat together (x2!), best friends; released; the last two outfits. */
    public static void maaltijd(ServerPlayer p, BlockPos schaal) {
        if (MewtwoVoortgang.stap(p) != MewtwoVoortgang.MAALTIJD) {
            return;
        }
        ServerLevel level = p.level();
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.KLAAR);
        VerhaalGuhs.geefVrij(p, VerhaalGuh.MEWTWO);
        adv(p, "mewtwo_maaltijd");
        geef(p, GuhClothes.MEWTWO_STAARTJE);
        geef(p, GuhClothes.MEW_BALLONNETJE);
        Minigames.give(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
        GuhEntity kopie = kopie(level, schaal, 40);
        MewEntity mew = null;
        List<MewEntity> er = level.getEntitiesOfClass(MewEntity.class, new AABB(schaal).inflate(64));
        if (!er.isEmpty()) {
            mew = er.get(0);
        } else {
            BlockPos thuis = MewSpawner.eiland(level, schaal);
            mew = MewSpawner.spawn(level, schaal.above(3), thuis != null ? thuis : schaal, true);
        }
        if (mew != null) {
            mew.snapTo(schaal.getX() + 0.5, schaal.getY() + 2.5, schaal.getZ() + 0.5);
            mew.giechel();
        }
        if (kopie != null) {
            kopie.getNavigation().moveTo(schaal.getX() + 0.5, schaal.getY(), schaal.getZ() + 1.5, 1.0);
            level.sendParticles(MewtwoFeature.X2.get(), kopie.getX(), kopie.getY() + kopie.getBbHeight() + 0.9, kopie.getZ(), 1, 0, 0, 0, 0);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(kopie, new MewtwoPayloads.X2(kopie.getId()));
        }
        level.playSound(null, schaal, MewtwoFeature.X2_SMUL.get(), SoundSource.NEUTRAL, 1f, 1.2f);
        level.sendParticles(BandFeature.HARTJE.get(), schaal.getX() + 0.5, schaal.getY() + 1.4, schaal.getZ() + 0.5, 24, 1.2, 0.6, 1.2, 0.03);
        level.sendParticles(MewtwoFeature.GLOED.get(), schaal.getX() + 0.5, schaal.getY() + 1.4, schaal.getZ() + 0.5, 30, 1.5, 0.8, 1.5, 0.05);
        Praat.scene(p, SCENE, List.of(
                new Praat.Regel(kopie, "gui.guhs.mewtwo.naam.mewtwo", "gui.guhs.mewtwo.maal.scene.1"),
                new Praat.Regel(mew, "gui.guhs.mewtwo.naam.mew", "gui.guhs.mewtwo.maal.scene.2"),
                new Praat.Regel(null, "gui.guhs.mewtwo.naam.verteller", "gui.guhs.mewtwo.maal.scene.3"),
                new Praat.Regel(kopie, "gui.guhs.mewtwo.naam.mewtwo", "gui.guhs.mewtwo.maal.scene.4"),
                new Praat.Regel(kopie, "gui.guhs.mewtwo.naam.mewtwo", "gui.guhs.mewtwo.maal.scene.5")));
        p.sendSystemMessage(Component.translatable("gui.guhs.mewtwo.maal.cadeau").withStyle(ChatFormatting.LIGHT_PURPLE));
        p.sendSystemMessage(Component.translatable("gui.guhs.mewtwo.klaar").withStyle(ChatFormatting.GOLD));
    }

    // =================================================================================================================
    // the Guhtwo (story copy) in the arena
    // =================================================================================================================

    /** VerhaalGuhs.Klik of the Guhtwo copy: its mood follows your questline; after the meal: "may I come with you?". */
    static InteractionResult klikKopie(GuhEntity kopie, ServerPlayer p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
        int stap = MewtwoVoortgang.stap(p);
        if (stap < MewtwoVoortgang.MAALTIJD) {
            GuhQuests.say(p, kopie, "gui.guhs.mewtwo.kopie.mokt");
        } else if (stap == MewtwoVoortgang.MAALTIJD) {
            GuhQuests.say(p, kopie, "gui.guhs.mewtwo.kopie.honger");
        } else if (VerhaalGuhs.magTemmen(p, VerhaalGuh.MEWTWO)) {
            Praat.open(p, kopie, TEM, "gui.guhs.mewtwo.tem.vraag", new Object[0], new Praat.Optie(1, "gui.guhs.mewtwo.tem.ja"),
                    new Praat.Optie(2, "gui.guhs.mewtwo.tem.nee"));
        } else if (VerhaalGuhs.heeftGetemd(p, VerhaalGuh.MEWTWO)) {
            GuhQuests.say(p, kopie, "gui.guhs.mewtwo.kopie.al_getemd");
        } else {
            GuhQuests.say(p, kopie, "gui.guhs.mewtwo.kopie.getemd_hier");
        }
        kopie.playSound(nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), 1f, 0.85f);
        return InteractionResult.SUCCESS;
    }

    /** "Yes, come with me!": your own Guhtwo (once), next to the copy. */
    @Nullable
    public static GuhEntity tem(ServerPlayer p, @Nullable GuhEntity kopie) {
        Vec3 waar = kopie != null ? kopie.position().add(1.5, 0, 0) : p.position().add(p.getLookAngle().multiply(2, 0, 2));
        GuhEntity guh = VerhaalGuhs.tem(p, VerhaalGuh.MEWTWO, waar);
        if (guh != null) {
            p.sendSystemMessage(Component.translatable("gui.guhs.mewtwo.tem.gelukt").withStyle(ChatFormatting.GOLD));
            ServerLevel level = p.level();
            level.sendParticles(MewtwoFeature.GLOED.get(), guh.getX(), guh.getY() + 0.8, guh.getZ(), 30, 0.6, 0.6, 0.6, 0.05);
            for (MewEntity mew : level.getEntitiesOfClass(MewEntity.class, guh.getBoundingBox().inflate(32))) {
                mew.giechel();
            }
        }
        return guh;
    }

    // =================================================================================================================
    // helpers
    // =================================================================================================================

    /** A clothing piece as its item (the player unlocks it by using it). */
    static void geef(ServerPlayer p, GuhClothes c) {
        Minigames.give(p, new ItemStack(ModItems.clothingItem(c)));
    }

    static void feestje(ServerPlayer p, @Nullable Entity bij) {
        ServerLevel level = p.level();
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
        Entity e = bij != null ? bij : p;
        level.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + 1.4, e.getZ(), 8, 0.4, 0.3, 0.4, 0.05);
    }

    /** Professor Knabbelkloon near this spot (for the scenes' portraits), or null. */
    @Nullable
    static Entity professor(ServerLevel level, BlockPos bij) {
        List<GuhNpcEntity> er = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(bij).inflate(48),
                n -> n.getKind() == GuhNpcEntity.Kind.KNABBELKLOON);
        return er.isEmpty() ? null : er.get(0);
    }

    /** The Guhtwo story copy near this spot, or null. */
    @Nullable
    static GuhEntity kopie(ServerLevel level, BlockPos bij, int straal) {
        List<GuhEntity> er = level.getEntitiesOfClass(GuhEntity.class, new AABB(bij).inflate(straal), g -> VerhaalGuhs.kopieVan(g) == VerhaalGuh.MEWTWO);
        return er.isEmpty() ? null : er.get(0);
    }

    private MewtwoVerhaal() {
    }
}
