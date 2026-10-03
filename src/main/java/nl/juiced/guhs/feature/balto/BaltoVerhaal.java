package nl.juiced.guhs.feature.balto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.baltoslee.BaltoSleeFeature;
import nl.juiced.guhs.feature.baltoslee.SleeTocht;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.EntitySpawnReason;
/**
 * The questline "Baltoguh en Nomguh" (DESIGN_30 §2, CONTRACT_30 §6.1), per player in {@code GuhQuests.saved(p)}:
 * <ol start="0">
 *   <li>{@link #NIEUW}: talk to Baltoguh at the old boat on the frozen bay (Steele-Mika mocks him) → {@link #ONTMOET};</li>
 *   <li>{@link #ONTMOET}: visit Rosy and the sick guh babies in the ziekenhuisje → {@link #BIJ_ROSY};</li>
 *   <li>{@link #BIJ_ROSY}: Boris' advice on the boat (follow the red markers...) → {@link #KLAAR_VOOR_TOCHT};</li>
 *   <li>{@link #KLAAR_VOOR_TOCHT}: tell Baltoguh "op naar de berghut!" → the medicine trek starts
 *       ({@link SleeTocht#startMedicijn}, balto-slee drives the sled) → {@link #HEEN};</li>
 *   <li>{@link #HEEN}: at the berghut (moment BERGHUT) you take the medicijnkist → {@link #TERUG};</li>
 *   <li>{@link #TERUG}: at the dieptepunt the storm is too strong and Baltoguh is lost: the white wolf-guh, Boris' words,
 *       the howl, the storm clears ({@link SleeTocht#stormKlaartOp}); back in time (AANKOMST) Steele-Mika tries to take the
 *       credit and everybody giggles → {@link #AANGEKOMEN} (TE_LAAT / GESTOPT: "Njeg, nog een keer!" → back to
 *       {@link #KLAAR_VOOR_TOCHT});</li>
 *   <li>{@link #AANGEKOMEN}: bring the kist to Rosy: she's better! The rewards ({@link #beloon}) → {@link #KLAAR};</li>
 *   <li>{@link #KLAAR}: the last quest "Maar heel misschien... een Baltoguh wel" is done; Baltoguh may come home with you
 *       once ({@link VerhaalGuhs#tem}).</li>
 * </ol>
 * The scenes are {@link Praat} scenes with a sleutel {@code balto_*}; their answers come back in {@link #luisteraars}.
 */
public final class BaltoVerhaal {
    public static final String STAP = "guhs_balto_stap";
    /** The title flag "Held van Nomguh" (the tab list shows it). */
    public static final String HELD = "guhs_balto_held";
    /** Talked to Muk (1) and Luk (2). */
    public static final String MUKLUK = "guhs_balto_mukluk";

    public static final int NIEUW = 0, ONTMOET = 1, BIJ_ROSY = 2, KLAAR_VOOR_TOCHT = 3, HEEN = 4, TERUG = 5, AANGEKOMEN = 6, KLAAR = 7;

    /** How far the speakers of a scene may be (else the page shows no portrait). */
    static final double SPREKERS = 48;
    /** The white wolf-guh stays this long (ticks) at most. */
    static final int WOLF_TICKS = 20 * 45;
    static final String WOLF_VOOR = "guhs_balto_voor", WOLF_TOT = "guhs_balto_tot";

    // =================================================================================================================
    // the player's step
    // =================================================================================================================

    public static int stap(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(STAP, 0);
    }

    public static void zet(ServerPlayer p, int stap) {
        GuhQuests.saved(p).putInt(STAP, stap);
    }

    public static boolean isHeld(ServerPlayer p) {
        return GuhQuests.saved(p).getBooleanOr(HELD, false);
    }

    /** (Tests / ops) forget everything of this questline (Baltoguh's release and taming too). */
    public static void wis(ServerPlayer p) {
        CompoundTag s = GuhQuests.saved(p);
        s.remove(STAP);
        s.remove(HELD);
        s.remove(MUKLUK);
        VerhaalGuhs.vergeet(p, VerhaalGuh.BALTOGUH);
        nl.juiced.guhs.feature.titels.Titels.ververs(p);
    }

    // =================================================================================================================
    // Baltoguh's story copy (VerhaalGuhs.opKlik)
    // =================================================================================================================

    public static InteractionResult klikBalto(GuhEntity kopie, ServerPlayer p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
        kopie.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 0.8f);
        kopie.getLookControl().setLookAt(p);
        switch (stap(p)) {
            case NIEUW -> ontmoet(p, kopie);
            case ONTMOET -> {
                GuhQuests.say(p, kopie, "gui.guhs.balto.balto.naar_rosy");
                GuhQuests.hint(p, "gui.guhs.balto.hint.rosy");
            }
            case BIJ_ROSY -> {
                GuhQuests.say(p, kopie, "gui.guhs.balto.balto.naar_boris");
                GuhQuests.hint(p, "gui.guhs.balto.hint.boris");
            }
            case KLAAR_VOOR_TOCHT -> startScene(p, kopie);
            case HEEN, TERUG -> {
                if (SleeTocht.bezig(p)) {
                    GuhQuests.say(p, kopie, "gui.guhs.balto.balto.onderweg");
                } else {
                    // the ride stopped without a word (a log-out...): just try again
                    zet(p, KLAAR_VOOR_TOCHT);
                    neemKist(p);
                    startScene(p, kopie);
                }
            }
            case AANGEKOMEN -> {
                GuhQuests.say(p, kopie, "gui.guhs.balto.balto.naar_rosy_kist");
                GuhQuests.hint(p, "gui.guhs.balto.hint.kist");
            }
            default -> {
                if (VerhaalGuhs.magTemmen(p, VerhaalGuh.BALTOGUH)) {
                    Praat.scene(p, "balto_tem", List.of(new Praat.Regel(kopie, "", "gui.guhs.balto.scene.tem.1")),
                            new Praat.Optie(1, "gui.guhs.balto.optie.tem_ja"), new Praat.Optie(2, "gui.guhs.balto.optie.tem_nee"));
                } else if (VerhaalGuhs.heeftGetemd(p, VerhaalGuh.BALTOGUH)) {
                    GuhQuests.say(p, kopie, "gui.guhs.balto.balto.na_temmen." + p.getRandom().nextInt(3));
                } else {
                    GuhQuests.say(p, kopie, "gui.guhs.balto.balto.klaar." + p.getRandom().nextInt(3));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    static void ontmoet(ServerPlayer p, @Nullable Entity balto) {
        Entity steele = npc(p, GuhNpcEntity.Kind.STEELE_MIKA);
        Praat.scene(p, "balto_ontmoet", List.of(
                verteller("gui.guhs.balto.scene.ontmoet.1"),
                regel(balto, "entity.guhs.guh.baltoguh", "gui.guhs.balto.scene.ontmoet.2"),
                regel(steele, "entity.guhs.guh_npc.steele_mika", "gui.guhs.balto.scene.ontmoet.3"),
                regel(balto, "entity.guhs.guh.baltoguh", "gui.guhs.balto.scene.ontmoet.4"),
                regel(balto, "entity.guhs.guh.baltoguh", "gui.guhs.balto.scene.ontmoet.5")),
                new Praat.Optie(1, "gui.guhs.balto.optie.ontmoet"));
    }

    static void startScene(ServerPlayer p, @Nullable Entity balto) {
        Praat.scene(p, "balto_start", List.of(regel(balto, "entity.guhs.guh.baltoguh", "gui.guhs.balto.scene.start.1")),
                new Praat.Optie(1, "gui.guhs.balto.optie.start"), new Praat.Optie(2, "gui.guhs.balto.optie.nog_niet"));
    }

    // =================================================================================================================
    // the answers of the scenes
    // =================================================================================================================

    static void luisteraars() {
        Praat.luister("balto_ontmoet", (p, spreker, optie) -> {
            if (stap(p) == NIEUW) {
                zet(p, ONTMOET);
                grant(p, "balto_ontmoet");
                GuhQuests.hint(p, "gui.guhs.balto.hint.rosy");
            }
        });
        Praat.luister("balto_rosy", (p, spreker, optie) -> {
            if (stap(p) == ONTMOET) {
                zet(p, BIJ_ROSY);
                grant(p, "balto_rosy");
                GuhQuests.hint(p, "gui.guhs.balto.hint.boris");
            }
        });
        Praat.luister("balto_boris", (p, spreker, optie) -> {
            if (stap(p) == BIJ_ROSY) {
                zet(p, KLAAR_VOOR_TOCHT);
                grant(p, "balto_boris");
                GuhQuests.hint(p, "gui.guhs.balto.hint.start");
            }
        });
        Praat.luister("balto_start", (p, spreker, optie) -> {
            if (optie == 1 && stap(p) == KLAAR_VOOR_TOCHT) {
                start(p);
            }
        });
        Praat.luister("balto_berghut", (p, spreker, optie) -> kistGepakt(p));
        Praat.luister("balto_wolf", (p, spreker, optie) -> huil(p));
        Praat.luister("balto_aankomst", (p, spreker, optie) -> {
            if (optie < 0 && stap(p) == AANGEKOMEN) {
                GuhQuests.hint(p, "gui.guhs.balto.hint.kist");
            }
        });
        Praat.luister("balto_feest", (p, spreker, optie) -> beloon(p, spreker));
        Praat.luister("balto_tem", (p, spreker, optie) -> {
            if (optie == 1) {
                tem(p);
            }
        });
    }

    /** "Op naar de berghut!": balto-slee starts the medicine ride (false: busy, or no Nomguh near). */
    static boolean start(ServerPlayer p) {
        GuhEntity balto = baltoKopie(p);
        if (!SleeTocht.startMedicijn(p, balto)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.balto.slee_niet").withStyle(ChatFormatting.AQUA));
            return false;
        }
        if (stap(p) < HEEN) {
            zet(p, HEEN);
        }
        return true;
    }

    // =================================================================================================================
    // the medicine trek (balto-slee's SleeTocht tells what happens)
    // =================================================================================================================

    public static void opMoment(ServerPlayer p, SleeTocht.Moment m) {
        int stap = stap(p);
        if (stap < KLAAR_VOOR_TOCHT) {
            return;   // (merge 3.0) not on balto's trek yet (an op proef ride / a test ride): SleeTocht goes on by itself
        }
        switch (m) {
            case START -> {
                if (stap == KLAAR_VOOR_TOCHT) {
                    zet(p, HEEN);
                }
                if (stap(p) == HEEN) {
                    p.sendSystemMessage(Component.translatable("gui.guhs.balto.moment.start").withStyle(ChatFormatting.AQUA));
                    grant(p, "balto_tocht");
                }
            }
            case BERGHUT -> {
                if (stap == HEEN || stap == TERUG) {
                    Praat.scene(p, "balto_berghut", List.of(verteller("gui.guhs.balto.scene.berghut.1"),
                                    regel(null, "entity.guhs.guh.baltoguh", "gui.guhs.balto.scene.berghut.2")),
                            new Praat.Optie(1, "gui.guhs.balto.optie.berghut"));
                } else {
                    SleeTocht.verder(p);
                }
            }
            case DIEPTEPUNT -> {
                if (stap == TERUG) {
                    wolfMoment(p);
                } else {
                    SleeTocht.verder(p);
                }
            }
            case AANKOMST -> {
                if (stap == TERUG) {
                    aankomst(p);
                }
            }
            case TE_LAAT, GESTOPT -> {
                if (stap == HEEN || stap == TERUG) {
                    zet(p, KLAAR_VOOR_TOCHT);
                    neemKist(p);
                    p.sendSystemMessage(Component.translatable(m == SleeTocht.Moment.TE_LAAT ? "gui.guhs.balto.moment.te_laat"
                            : "gui.guhs.balto.moment.gestopt").withStyle(ChatFormatting.LIGHT_PURPLE));
                    GuhQuests.hint(p, "gui.guhs.balto.hint.start");
                }
            }
        }
    }

    /** At the berghut: the medicine chest into your pockets, and back (the scene was read, or its answer). */
    static void kistGepakt(ServerPlayer p) {
        if (stap(p) == HEEN) {
            zet(p, TERUG);
            if (GuhQuests.count(p, BaltoFeature.MEDICIJNKIST_ITEM.get()) == 0) {
                Minigames.give(p, new ItemStack(BaltoFeature.MEDICIJNKIST_ITEM.get()));
            }
            grant(p, "balto_berghut");
            p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1f, 1.2f);
        }
        SleeTocht.verder(p);
    }

    /** The dieptepunt: the white wolf-guh appears a few blocks ahead, and the scene with Boris' words. */
    static void wolfMoment(ServerPlayer p) {
        GuhNpcEntity wolf = spawnWolf(p);
        GuhDex.zie(p, GuhVariant.WITTE_WOLFGUH);
        Praat.scene(p, "balto_wolf", List.of(
                        verteller("gui.guhs.balto.scene.wolf.1"),
                        regel(null, "entity.guhs.guh.baltoguh", "gui.guhs.balto.scene.wolf.2"),
                        regel(wolf, "entity.guhs.guh_npc.witte_wolfguh", "gui.guhs.balto.scene.wolf.3"),
                        regel(null, "gui.guhs.balto.boris_gedachten", "gui.guhs.balto.scene.wolf.4"),
                        regel(null, "gui.guhs.balto.boris_gedachten", "gui.guhs.balto.scene.wolf.5"),
                        regel(null, "entity.guhs.guh.baltoguh", "gui.guhs.balto.scene.wolf.6")),
                new Praat.Optie(1, "gui.guhs.balto.optie.huil"));
    }

    /** The white wolf-guh (a glowing NPC for this player, gone after the howl or after {@link #WOLF_TICKS}). */
    @Nullable
    static GuhNpcEntity spawnWolf(ServerPlayer p) {
        ServerLevel level = p.level();
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 at = p.position().add(look.scale(6));
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(at.x), (int) Math.floor(at.z));
        GuhNpcEntity wolf = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        if (wolf == null) {
            return null;
        }
        wolf.setKind(GuhNpcEntity.Kind.WITTE_WOLFGUH);
        wolf.snapTo(at.x, Math.abs(y - p.getY()) < 6 ? y : p.getY(), at.z, (float) Math.toDegrees(Math.atan2(look.x, -look.z)), 0f);
        wolf.roleData.store(WOLF_VOOR, UUIDUtil.CODEC, p.getUUID());
        wolf.roleData.putLong(WOLF_TOT, level.getGameTime() + WOLF_TICKS);
        level.addFreshEntity(wolf);
        level.sendParticles(BaltoFeature.WOLFGLANS.get(), wolf.getX(), wolf.getY() + 1, wolf.getZ(), 40, 0.6, 0.8, 0.6, 0.02);
        level.playSound(null, wolf.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5f, 0.7f);
        return wolf;
    }

    /** "Huil mee!": Baltoguh (and the white wolf-guh) howl, the storm clears, the ride goes on. */
    static void huil(ServerPlayer p) {
        ServerLevel level = p.level();
        level.playSound(null, p.blockPosition(), BaltoFeature.HUIL.get(), SoundSource.NEUTRAL, 1.4f, 1.0f);
        for (GuhNpcEntity wolf : level.getEntitiesOfClass(GuhNpcEntity.class, p.getBoundingBox().inflate(40),
                n -> n.getKind() == GuhNpcEntity.Kind.WITTE_WOLFGUH && n.roleData.read(WOLF_VOOR, UUIDUtil.CODEC).isPresent() && p.getUUID().equals(n.roleData.read(WOLF_VOOR, UUIDUtil.CODEC).orElseThrow()))) {
            level.playSound(null, wolf.blockPosition(), BaltoFeature.HUIL.get(), SoundSource.NEUTRAL, 1.0f, 1.25f);
            level.sendParticles(BaltoFeature.WOLFGLANS.get(), wolf.getX(), wolf.getY() + 1.2, wolf.getZ(), 60, 0.8, 1.2, 0.8, 0.04);
            wolf.roleData.putLong(WOLF_TOT, Math.min(wolf.roleData.getLongOr(WOLF_TOT, 0L), level.getGameTime() + 60));   // she fades away
        }
        level.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 2.2, p.getZ(), 6, 0.8, 0.3, 0.8, 1);
        if (stap(p) == TERUG) {
            grant(p, "balto_wolf");
            p.sendOverlayMessage(Component.translatable("gui.guhs.balto.moment.storm_klaart_op").withStyle(ChatFormatting.AQUA));
        }
        SleeTocht.stormKlaartOp(p);
        SleeTocht.verder(p);
    }

    /** Back in Nomguh in time: Steele-Mika wants the credit, everybody giggles; bring the kist to Rosy. */
    static void aankomst(ServerPlayer p) {
        zet(p, AANGEKOMEN);
        grant(p, "balto_aankomst");
        ServerLevel level = p.level();
        Entity steele = npc(p, GuhNpcEntity.Kind.STEELE_MIKA);
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, p.getBoundingBox().inflate(24))) {
            level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + 1, guh.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
            guh.playSound(ModSounds.GUH_HAPPY.get(), 0.7f, 1.1f + guh.getRandom().nextFloat() * 0.3f);
        }
        level.playSound(null, p.blockPosition(), BaltoFeature.BELLETJES.get(), SoundSource.NEUTRAL, 1f, 1f);
        Praat.scene(p, "balto_aankomst", List.of(
                verteller("gui.guhs.balto.scene.aankomst.1"),
                regel(steele, "entity.guhs.guh_npc.steele_mika", "gui.guhs.balto.scene.aankomst.2"),
                verteller("gui.guhs.balto.scene.aankomst.3"),
                regel(baltoKopie(p), "entity.guhs.guh.baltoguh", "gui.guhs.balto.scene.aankomst.4")));
    }

    /** At Rosy's bed with the kist (step AANGEKOMEN): the healing scene; its answer gives the rewards. */
    static void feest(ServerPlayer p, GuhNpcEntity rosy) {
        Praat.scene(p, "balto_feest", List.of(
                        verteller("gui.guhs.balto.scene.feest.1"),
                        regel(rosy, "entity.guhs.guh_npc.rosy", "gui.guhs.balto.scene.feest.2"),
                        regel(baltoKopie(p), "entity.guhs.guh.baltoguh", "gui.guhs.balto.scene.feest.3"),
                        verteller("gui.guhs.balto.scene.feest.4"),
                        regel(npc(p, GuhNpcEntity.Kind.BORIS), "entity.guhs.guh_npc.boris", "gui.guhs.balto.scene.feest.5")),
                new Praat.Optie(1, "gui.guhs.balto.optie.feest"));
    }

    /**
     * The end of the questline: the kist goes to Rosy; Baltoguh may come home with you (once), your own sneeuwslee, the
     * Baltoguh-beeldje, the title "Held van Nomguh", the four outfits, the advancements, a party. Only once (false when not
     * at {@link #AANGEKOMEN}).
     */
    public static boolean beloon(ServerPlayer p, @Nullable Entity bij) {
        if (stap(p) != AANGEKOMEN) {
            return false;
        }
        zet(p, KLAAR);
        neemKist(p);
        VerhaalGuhs.geefVrij(p, VerhaalGuh.BALTOGUH);
        Minigames.give(p, new ItemStack(BaltoSleeFeature.SNEEUWSLEE.get()));
        Minigames.give(p, new ItemStack(BaltoFeature.BALTOGUH_BEELDJE_ITEM.get()));
        for (var c : BaltoFeature.KLEDING) {
            Minigames.give(p, new ItemStack(ModItems.clothingItem(c)));
        }
        GuhQuests.saved(p).putBoolean(HELD, true);
        nl.juiced.guhs.feature.titels.Titels.ververs(p);
        grant(p, "balto_held");
        ServerLevel level = p.level();
        Vec3 at = bij != null ? bij.position() : p.position();
        level.sendParticles(ParticleTypes.HEART, at.x, at.y + 1.2, at.z, 16, 0.8, 0.6, 0.8, 0.05);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 30, 0.8, 0.8, 0.8, 0.1);
        level.sendParticles(BaltoFeature.WOLFGLANS.get(), p.getX(), p.getY() + 1.5, p.getZ(), 40, 1.2, 1.2, 1.2, 0.03);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        level.playSound(null, p.blockPosition(), BaltoFeature.HUIL.get(), SoundSource.NEUTRAL, 0.8f, 1.1f);
        p.sendSystemMessage(Component.translatable("gui.guhs.balto.held", p.getDisplayName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        GuhQuests.hint(p, "gui.guhs.balto.hint.tem");
        return true;
    }

    /** Baltoguh comes home with you (once per player; the story copy stays in Nomguh for the others). */
    @Nullable
    static GuhEntity tem(ServerPlayer p) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        GuhEntity guh = VerhaalGuhs.tem(p, VerhaalGuh.BALTOGUH, p.position().add(look.scale(1.6)));
        if (guh != null) {
            p.level().playSound(null, guh.blockPosition(), BaltoFeature.HUIL.get(), SoundSource.NEUTRAL, 0.6f, 1.3f);
            p.sendSystemMessage(Component.translatable("gui.guhs.balto.getemd").withStyle(ChatFormatting.AQUA));
        }
        return guh;
    }

    // =================================================================================================================
    // helpers
    // =================================================================================================================

    /** Takes the medicijnkist(en) out of the pockets (a new ride gets a new one). */
    static void neemKist(ServerPlayer p) {
        int n = GuhQuests.count(p, BaltoFeature.MEDICIJNKIST_ITEM.get());
        if (n > 0) {
            GuhQuests.take(p, BaltoFeature.MEDICIJNKIST_ITEM.get(), n);
        }
    }

    /** The hidden quest/&lt;name&gt; (FTB) and the shown verhalen/&lt;name&gt;. */
    static void grant(ServerPlayer p, String name) {
        GuhAdvancements.grant(p, name);
        GidsFeature.grant(p, "verhalen/" + name);
    }

    static Praat.Regel verteller(String key) {
        return new Praat.Regel(null, "gui.guhs.balto.verteller", key);
    }

    static Praat.Regel regel(@Nullable Entity spreker, String naamKey, String key) {
        return new Praat.Regel(spreker, naamKey, key);
    }

    /** The nearest NPC of this kind (within {@link #SPREKERS}), or null. */
    @Nullable
    static GuhNpcEntity npc(ServerPlayer p, GuhNpcEntity.Kind kind) {
        return p.level().getEntitiesOfClass(GuhNpcEntity.class, p.getBoundingBox().inflate(SPREKERS), n -> n.getKind() == kind).stream()
                .min(Comparator.comparingDouble(n -> n.distanceToSqr(p))).orElse(null);
    }

    /** Baltoguh's nearest story copy (within {@link #SPREKERS}), or null. */
    @Nullable
    static GuhEntity baltoKopie(ServerPlayer p) {
        List<GuhEntity> list = new ArrayList<>(p.level().getEntitiesOfClass(GuhEntity.class, p.getBoundingBox().inflate(SPREKERS),
                g -> VerhaalGuhs.kopieVan(g) == VerhaalGuh.BALTOGUH));
        return list.stream().min(Comparator.comparingDouble(g -> g.distanceToSqr(p))).orElse(null);
    }

    /** A step's name for the op command and the tests. */
    static String naam(int stap) {
        return switch (stap) {
            case NIEUW -> "nieuw";
            case ONTMOET -> "ontmoet";
            case BIJ_ROSY -> "bij_rosy";
            case KLAAR_VOOR_TOCHT -> "klaar_voor_tocht";
            case HEEN -> "heen";
            case TERUG -> "terug";
            case AANGEKOMEN -> "aangekomen";
            default -> "klaar";
        };
    }

    static BlockPos voor(ServerPlayer p, double afstand) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        return BlockPos.containing(p.position().add(look.scale(afstand)));
    }

    private BaltoVerhaal() {
    }
}
