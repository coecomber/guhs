package nl.juiced.guhs.feature.park;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.ModDimensions;

/**
 * 1.4.2 (Het Knabbelpark): the parkwachter's questline "Een dagje in het Knabbelpark" (per player, at every park, any
 * number of players at once: all progress is in the {@link #LIJN Verhaallijn}).
 * <ol start="0">
 *   <li>talk to the parkwachter-guh in his hut;</li>
 *   <li>read the three plaques at the statues ({@link #lees}: each plaque is a flag; the third one reached moves on);</li>
 *   <li>feed the three Hertguhtjes in the hertenkooi a kaasknabbel each ({@link #voer}: hearts, a happy hop; a flag per
 *       Hertguhtje);</li>
 *   <li>help the kooi-verzorger: she needs {@link #HOOI} wheat for the ruif;</li>
 *   <li>back to the parkwachter: the reward, once per player: your own Hertguhtje (a new tamed guh, the only way to get
 *       one) and a little statue of your own guh ({@link ParkBlokken.EigenBeeld}).</li>
 * </ol>
 * Texts: tools/features/park.py (quest.guhs.park.*, gui.guhs.park.*).
 */
public final class Park {
    public static final int START = 0, PLAQUETTES_STAP = 1, VOEREN = 2, KLUSJE = 3, TERUG = 4, KLAAR = 5;
    public static final int PLAQUETTES = 3, HERTEN = 3, HOOI = 6;
    private static final String Q = "quest.guhs.park.";

    public static final Verhaallijn LIJN = Verhaallijn.maak("knabbelpark", "knabbelpark").stappen(KLAAR).icoon("guhs:park_eigen_beeld")
            .nodig((p, stap) -> switch (stap) {
                case PLAQUETTES_STAP -> List.of(Verhaallijn.nodig("guhs:park_plaquette", "gui.guhs.park.nodig.plaquettes", gelezen(p), PLAQUETTES));
                case VOEREN -> List.of(Verhaallijn.nodig("guhs:kaas_knabbels", "gui.guhs.park.nodig.herten", gevoerd(p), HERTEN));
                case KLUSJE -> List.of(Verhaallijn.nodig("minecraft:wheat", GuhQuests.count(p, Items.WHEAT), HOOI));
                default -> List.of();
            })
            .beloningen(p -> {
                boolean klaar = Park.LIJN.klaar(p);
                return List.of(Verhaallijn.beloning("guhs:park_eigen_beeld", klaar),
                        Verhaallijn.beloning("guhs:kaas_knabbels", "gui.guhs.park.beloning.hertguhtje", klaar));
            })
            .doel((p, stap) -> Doel.structuur(ModDimensions.GUHMENSION, ParkFeature.STRUCTUUR, Component.translatable("structure.guhs.knabbelpark")))
            .registreer();

    private Park() {
    }

    // --- the plaques -----------------------------------------------------------------------------------------------------

    /** A plaque is read: its text on screen; at the plaques step it counts. */
    public static void lees(ServerPlayer p, int nr) {
        Praat.scene(p, "park_plaquette", List.of(new Praat.Regel(null, "gui.guhs.park.plaquette.naam", "gui.guhs.park.plaquette." + nr)));
        p.level().playSound(null, p.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.3f, 1.4f);
        GuhAdvancements.grant(p, "park_plaquette_" + nr);
        if (LIJN.stap(p) != PLAQUETTES_STAP || LIJN.vlag(p, "plaquette_" + nr)) {
            return;
        }
        LIJN.vlag(p, "plaquette_" + nr, true);
        if (gelezen(p) >= PLAQUETTES && LIJN.verder(p, PLAQUETTES_STAP)) {
            GuhQuests.hint(p, Q + "hint.herten");
        } else {
            p.sendOverlayMessage(Component.translatable("gui.guhs.park.gelezen", gelezen(p), PLAQUETTES).withStyle(ChatFormatting.GOLD));
        }
    }

    public static int gelezen(ServerPlayer p) {
        int n = 0;
        for (int nr = 1; nr <= PLAQUETTES; nr++) {
            n += LIJN.vlag(p, "plaquette_" + nr) ? 1 : 0;
        }
        return n;
    }

    // --- the Hertguhtjes ---------------------------------------------------------------------------------------------------

    /** Is this one of the Hertguhtjes of a hertenkooi (synced flag: the client knows it too)? */
    public static boolean kooiHert(GuhEntity guh) {
        return GuhHooks.heeft(guh, ParkFeature.KOOIHERT);
    }

    /** (GuhHooks.klik) a kaasknabbel for a Hertguhtje of the kooi: it eats it (hearts, a hop); never tames it. */
    static InteractionResult klik(GuhEntity guh, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kooiHert(guh) && stack.is(ModItems.KAAS_KNABBELS.get())) {
            if (player instanceof ServerPlayer sp) {
                voer(guh, sp, stack);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ParkFeature.EIGEN_BEELD_ITEM.get()) && guh.isTame() && guh.isOwnedBy(player)) {
            if (player instanceof ServerPlayer sp) {
                poseer(guh, sp, stack);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    static void voer(GuhEntity guh, ServerPlayer p, ItemStack stack) {
        ServerLevel level = p.level();
        if (!p.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, guh, ModSounds.GUH_EAT.get(), SoundSource.NEUTRAL, 1f, 1.3f);
        level.playSound(null, guh, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.8f, 1.4f);
        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + 0.9, guh.getZ(), 6, 0.35, 0.3, 0.35, 0.05);
        if (guh.onGround()) {
            guh.setDeltaMovement(guh.getDeltaMovement().add(0, 0.42, 0));   // (a happy hop)
            guh.hurtMarked = true;
        }
        GuhAdvancements.grant(p, "park_hert_gevoerd");
        if (LIJN.stap(p) != VOEREN) {
            return;
        }
        String vlag = "hert_" + hertNummer(guh);
        if (LIJN.vlag(p, vlag)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.park.al_gevoerd").withStyle(ChatFormatting.GOLD));
            return;
        }
        LIJN.vlag(p, vlag, true);
        if (gevoerd(p) >= HERTEN && LIJN.verder(p, VOEREN)) {
            GuhQuests.hint(p, Q + "hint.verzorger");
        } else {
            p.sendOverlayMessage(Component.translatable("gui.guhs.park.gevoerd", gevoerd(p), HERTEN).withStyle(ChatFormatting.GOLD));
        }
    }

    /** Which of the three it is (its Bezetting id "park_hert_&lt;n&gt;@..."; 0 for any other Hertguhtje). */
    static int hertNummer(GuhEntity guh) {
        String tag = guh.getPersistentData().getStringOr(Bezetting.TAG, "");
        for (int n = 1; n <= HERTEN; n++) {
            if (tag.startsWith(ParkFeature.HERT_ID + n)) {
                return n;
            }
        }
        return 0;
    }

    public static int gevoerd(ServerPlayer p) {
        int n = 0;
        for (int nr = 1; nr <= HERTEN; nr++) {
            n += LIJN.vlag(p, "hert_" + nr) ? 1 : 0;
        }
        return n;
    }

    /** A Hertguhtje for the kooi (Bezetting makes it when one is missing). */
    static GuhEntity kooiHert(ServerLevel level, Vec3 plek) {
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guh == null) {
            return null;
        }
        guh.setVariant(GuhVariant.HERTGUHTJE);
        guh.setBehavior(GuhEntity.Behavior.PASSIVE);
        guh.setGuhScale(0.85f);
        GuhHooks.zet(guh, ParkFeature.KOOIHERT, true);
        guh.snapTo(plek.x, plek.y, plek.z, level.getRandom().nextFloat() * 360f, 0f);
        return guh;
    }

    // --- the litter bin, the own statue ----------------------------------------------------------------------------------

    static void weggegooid(ServerPlayer p) {
        GuhAdvancements.grant(p, "park_prullenbak");
    }

    /** Your own guh poses for the little statue in your hand: it takes the guh's look and name. */
    static void poseer(GuhEntity guh, ServerPlayer p, ItemStack stack) {
        Component naam = guh.hasCustomName() ? guh.getCustomName() : guh.getVariant().displayName();
        BeeldBlockEntity.schrijf(stack, guh.getVariant(), naam);
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.WAX_ON, guh.getX(), guh.getY() + 0.8, guh.getZ(), 14, 0.4, 0.4, 0.4, 0.4);
        level.playSound(null, guh, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.2f);
        p.sendOverlayMessage(Component.translatable("gui.guhs.park.poseert", naam).withStyle(ChatFormatting.GOLD));
    }

    // --- the parkwachter and the kooi-verzorger ---------------------------------------------------------------------------

    /** The parkwachter-guh: the start and the end of the questline. */
    static final class WachterRol extends QuestRol {
        private static final int HELPEN = 1, VERHAAL = 2, DANK = 3;

        WachterRol() {
            super(LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            switch (stap) {
                case START -> {
                    LIJN.begin(p);
                    scherm(p, npc, Q + "wachter.hallo", new Praat.Optie(HELPEN, Q + "optie.helpen"), new Praat.Optie(VERHAAL, Q + "optie.park"));
                }
                case PLAQUETTES_STAP -> {
                    zeg(p, npc, Q + "wachter.plaquettes", gelezen(p), PLAQUETTES);
                    hint(p, Q + "hint.plaquettes");
                }
                case VOEREN -> {
                    zeg(p, npc, Q + "wachter.herten");
                    hint(p, Q + "hint.herten");
                }
                case KLUSJE -> {
                    zeg(p, npc, Q + "wachter.verzorger");
                    hint(p, Q + "hint.verzorger");
                }
                case TERUG -> scherm(p, npc, Q + "wachter.klaar", new Praat.Optie(DANK, Q + "optie.dank"));
                default -> zeg(p, npc, Q + "wachter.later" + p.getRandom().nextInt(4));
            }
        }

        @Override
        protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
            if (stap == START && optie == VERHAAL) {
                scherm(p, npc, Q + "wachter.park", new Praat.Optie(HELPEN, Q + "optie.helpen"));
            } else if (stap == START && optie == HELPEN && verder(p, START)) {
                zeg(p, npc, Q + "wachter.start");
                hint(p, Q + "hint.plaquettes");
            } else if (stap == TERUG && optie == DANK && verder(p, TERUG)) {
                beloon(npc, p);
            }
        }

        /** Once per player: a Hertguhtje of your own and the little statue. */
        private void beloon(GuhNpcEntity npc, ServerPlayer p) {
            geefEenmalig(p, "eigen_beeld", new ItemStack(ParkFeature.EIGEN_BEELD_ITEM.get()));
            if (lijn().eenmalig(p, "hertguhtje")) {
                ServerLevel level = p.level();
                GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
                if (guh != null) {
                    guh.setVariant(GuhVariant.HERTGUHTJE);
                    Vec3 waar = p.position().add(p.getLookAngle().multiply(1.5, 0, 1.5));
                    guh.snapTo(waar.x, p.getY(), waar.z, p.getYRot() + 180f, 0f);
                    guh.setPersistenceRequired();
                    level.addFreshEntity(guh);
                    guh.tame(p);
                    level.broadcastEntityEvent(guh, (byte) 7);
                    level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + 0.8, guh.getZ(), 12, 0.6, 0.5, 0.6, 0.05);
                    level.playSound(null, guh, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.1f);
                }
            }
            zeg(p, npc, Q + "wachter.beloning");
            hint(p, Q + "hint.beeld");
            verborgen(p, "park_klaar");
        }
    }

    /** The kooi-verzorger-guh: the small chore (wheat for the ruif). */
    static final class VerzorgerRol extends QuestRol {
        VerzorgerRol() {
            super(LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            switch (stap) {
                case START, PLAQUETTES_STAP -> zeg(p, npc, Q + "verzorger.hallo");
                case VOEREN -> {
                    zeg(p, npc, Q + "verzorger.voeren", gevoerd(p), HERTEN);
                    hint(p, Q + "hint.herten");
                }
                case KLUSJE -> {
                    if (!LIJN.vlag(p, "klusje_uitleg") && GuhQuests.count(p, Items.WHEAT) < HOOI) {
                        LIJN.vlag(p, "klusje_uitleg", true);
                        zeg(p, npc, Q + "verzorger.klusje", HOOI);   // (the first time: what the chore is)
                        hint(p, Q + "hint.hooi");
                    } else if (lever(p, npc, KLUSJE, Items.WHEAT, HOOI, Q + "verzorger.tekort")) {
                        zeg(p, npc, Q + "verzorger.dank");
                        hint(p, Q + "hint.terug");
                        p.level().playSound(null, npc.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.2f);
                    }
                }
                case TERUG -> zeg(p, npc, Q + "verzorger.terug");
                default -> zeg(p, npc, Q + "verzorger.later" + p.getRandom().nextInt(3));
            }
        }
    }
}
