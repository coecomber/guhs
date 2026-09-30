package nl.juiced.guhs.feature.samen;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.band.Vriendjes;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.huisje.Speelgoed;
import nl.juiced.guhs.feature.huisje.Speeltje;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Guh-vriendschappen (2.10, samen). Tamed guhs of the same owner that spend time together become friends
 * ({@link Vriendjes#VRIENDJES} points) and then besties ({@link Vriendjes#BESTIES}); points only ever go up:
 * <ul>
 *   <li>+{@link #PER_SECONDE} for every second two guhs are within {@link #SAMEN} blocks of each other;</li>
 *   <li>+{@link #KNUFFEL_PUNTEN} for a cuddle together, +{@link #SPEEL_PUNTEN} for playing on the wip / schommel together,
 *       +{@link #SLAAP_PUNTEN} for a night side by side in the same Guhhuisje.</li>
 * </ul>
 * Friends visibly do things together ({@link VriendjesGoal}): they walk together, cuddle (both KNUFFELEN, hearts between
 * them), play together on the wip and the schommel (the speelgoed's toys, {@link Speelgoed}) and sleep together in their
 * huisje (little hearts at the window). Each becomes a VRIENDJE moment on the band bus; the first friend is an "eerste
 * keer" in the dagboek, and the Guhdex shows the friends and the best-friend duo.
 */
public final class SamenVriendjes {
    public static final double SAMEN = 6;
    public static final int PER_SECONDE = 1, KNUFFEL_PUNTEN = 60, SPEEL_PUNTEN = 120, SLAAP_PUNTEN = 240;
    /** Friends only go off together while their owner (if around) is within this distance (else they follow the owner). */
    public static final double BAAS_BEREIK = 20;
    /** How long a guh waits between two things with a friend (ticks). */
    public static final int RUST = 20 * 45;
    /** Persistent data: game time until the next activity with a friend; the day of the last night together. */
    static final String RUST_TOT = "guhs_samen_vriend_rust", SLAAP_DAG = "guhs_samen_slaap_dag";
    /** The toy the friends play on together (speelgoed's wip &amp; schommel). */
    public static final String WIP = "wip_schommel";

    /** (Tests) a stand-in for the speelgoed's wip &amp; schommel; guhs that start something with a friend at once (and what). */
    static volatile Speeltje TEST_WIP;
    static final Map<UUID, Wat> TEST_NU = new ConcurrentHashMap<>();

    /** A friend asked this guh to play (guh UUID -> what to do). Picked up by the friend's own goal. */
    private static final Map<UUID, Uitnodiging> UITNODIGINGEN = new ConcurrentHashMap<>();

    enum Wat { LOPEN, KNUFFELEN, SPELEN }

    record Uitnodiging(UUID van, Wat wat, @Nullable KlusTaak taak, long tot) {
    }

    private SamenVriendjes() {
    }

    // =====================================================================================================================
    // the points
    // =====================================================================================================================

    /** Every second: +1 with each own guh close by (each pair counted once: from the guh with the smaller UUID). */
    static void tick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 20 != 0 || Huisjes.isBinnen(guh) || !(guh.level() instanceof ServerLevel level)) {
            return;
        }
        for (GuhEntity ander : level.getEntitiesOfClass(GuhEntity.class, guh.getBoundingBox().inflate(SAMEN),
                g -> g != guh && Band.isBandGuh(g) && guh.getOwnerUUID().equals(g.getOwnerUUID()) && !Huisjes.isBinnen(g))) {
            if (guh.getUUID().compareTo(ander.getUUID()) < 0 && guh.distanceTo(ander) <= SAMEN) {
                Vriendjes.samen(guh, ander, PER_SECONDE);
            }
        }
    }

    /** Vriendjes.opNieuw: two guhs became friends (or besties): the moment, the dagboek, a message to the owner. */
    static void nieuw(MinecraftServer s, UUID a, UUID b, boolean besties) {
        Mob ga = Band.zoekGeladen(s, a), gb = Band.zoekGeladen(s, b);
        Mob een = ga != null ? ga : gb;
        ServerPlayer owner = een == null ? null : Band.eigenaarOnline(een);
        for (Mob[] paar : new Mob[][]{{ga, gb}, {gb, ga}}) {
            Mob guh = paar[0], ander = paar[1];
            if (guh == null) {
                continue;
            }
            UUID anderId = guh == ga ? b : a;
            String naam = ander != null ? ander.getName().getString() : "?";
            Band.moment(guh, owner, Moment.VRIENDJE, anderId.toString());
            Band.geefHartjes(guh, owner, Reden.VRIENDJE.dagMax(), Reden.VRIENDJE);
            if (!besties && Dagboek.eersteKeer(guh, owner, "eerste_vriendje")) {
                Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.vriendje", naam);
            } else if (besties) {
                Dagboek.eersteKeer(guh, owner, "samen_bestie");
                Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.bestie", naam);
            }
            if (guh.level() instanceof ServerLevel level) {
                level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 8, 0.4, 0.2, 0.4, 0.03);
            }
        }
        if (owner != null && ga != null && gb != null) {
            owner.sendSystemMessage(Component.translatable(besties ? "gui.guhs.samen.besties" : "gui.guhs.samen.vriendjes",
                    ga.getDisplayName(), gb.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            GidsFeature.grant(owner, besties ? "lieve_vadsjes/samen_besties" : "lieve_vadsjes/samen_vriendjes");
            owner.level().playSound(null, ga.blockPosition(), BandFeature.HARTJES_GELUID.get(), SoundSource.NEUTRAL, 1f, 1.2f);
        }
    }

    // =====================================================================================================================
    // sleeping together in the huisje
    // =====================================================================================================================

    /** The band bus: SLAAP (a resident went inside for the night): friends already asleep in the same huisje. */
    static void moment(Mob mob, @Nullable ServerPlayer speler, Moment m, String waarde) {
        if (m != Moment.SLAAP || !(mob.level() instanceof ServerLevel level) || !Band.isBandGuh(mob)) {
            return;
        }
        Huisje h = Huisjes.thuisVan(mob);
        if (h == null) {
            return;
        }
        long dag = Band.dag(level.getServer());
        UUID id = Band.id(mob);
        for (UUID ander : h.bewoners()) {
            if (ander.equals(id) || !Vriendjes.vrienden(level.getServer(), id, ander)) {
                continue;
            }
            net.minecraft.world.entity.Entity e = Huisjes.zoekBewoner(level, h, ander);
            if (!(e instanceof Mob am) || !Huisjes.isBinnen(am)) {
                continue;
            }
            samenGeslapen(level, h, mob, am, dag);
        }
    }

    /** Two friends side by side in their huisje tonight (once per night per guh): points, hearts at the window. */
    static boolean samenGeslapen(ServerLevel level, Huisje h, Mob a, Mob b, long dag) {
        if (a.getPersistentData().getLongOr(SLAAP_DAG, 0L) == dag + 1) {
            return false;
        }
        a.getPersistentData().putLong(SLAAP_DAG, dag + 1);
        Vriendjes.samen(a, b, SLAAP_PUNTEN);
        for (boolean links : new boolean[]{true, false}) {
            Vec3 raam = h.raam(links);
            level.sendParticles(BandFeature.HARTJE.get(), raam.x, raam.y, raam.z, 3, 0.15, 0.1, 0.15, 0.01);
        }
        Band.moment(a, null, Moment.VRIENDJE, Band.id(b).toString());
        if (a.getRandom().nextInt(3) == 0) {
            Dagboek.wistJeDat(a, "gui.guhs.wistjedat.samen.samen_slapen", b.getName().getString());
        }
        return true;
    }

    // =====================================================================================================================
    // doing things together
    // =====================================================================================================================

    /** A friend of this guh that's loaded, close by (16) and free, or null (besties first). */
    @Nullable
    static GuhEntity vriendInDeBuurt(GuhEntity guh) {
        MinecraftServer s = guh.level().getServer();
        if (s == null) {
            return null;
        }
        List<UUID> vrienden = new ArrayList<>(Vriendjes.vriendenVan(s, Band.id(guh)));
        UUID bestie = Vriendjes.bestie(s, Band.id(guh));
        if (bestie != null) {
            vrienden.remove(bestie);
            vrienden.add(0, bestie);
        }
        for (UUID id : vrienden) {
            if (guh.level() instanceof ServerLevel level && level.getEntity(id) instanceof GuhEntity v && v.isAlive() && v.distanceTo(guh) <= 16
                    && vrij(v)) {
                return v;
            }
        }
        return null;
    }

    static boolean vrij(GuhEntity guh) {
        return Band.isBandGuh(guh) && !guh.isOrderedToSit() && guh.mayWander() && !guh.isPassenger() && !guh.isVehicle() && !guh.isLeashed()
                && !Huisjes.isBinnen(guh) && !GuhHooks.isBezig(guh) && guh.emotes.current() != Emote.SLAPEN && !guh.isBaby();
    }

    /** Invites a friend: it comes along with its own goal (a toy task of its own, if any). */
    static void nodigUit(GuhEntity van, GuhEntity vriend, Wat wat, @Nullable KlusTaak taak) {
        UITNODIGINGEN.put(vriend.getUUID(), new Uitnodiging(van.getUUID(), wat, taak, van.level().getGameTime() + 100));
    }

    /**
     * Priority 4 (before following the owner, while the owner is within {@link #BAAS_BEREIK}): now and then a guh does something with a friend close by
     * (walking together, a cuddle, the wip / schommel); an invited friend joins in.
     */
    public static final class VriendjesGoal extends Goal {
        private final GuhEntity guh;
        @Nullable
        private GuhEntity vriend;
        private Wat wat = Wat.LOPEN;
        @Nullable
        private KlusTaak taak;
        private int tijd;
        private boolean uitgenodigd;
        private boolean geknuffeld;

        public VriendjesGoal(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (guh.level().isClientSide() || !vrij(guh) || !baasDichtbij(guh)) {
                return false;
            }
            Uitnodiging u = UITNODIGINGEN.remove(guh.getUUID());
            if (u != null && u.tot() >= guh.level().getGameTime() && guh.level() instanceof ServerLevel level
                    && level.getEntity(u.van()) instanceof GuhEntity van && van.isAlive()) {
                vriend = van;
                wat = u.wat();
                taak = u.taak();
                uitgenodigd = true;
                return true;
            }
            Wat test = TEST_NU.remove(guh.getUUID());
            if (test == null && ((guh.tickCount + guh.getId()) % 40 != 0 || guh.getPersistentData().getLongOr(RUST_TOT, 0L) > guh.level().getGameTime()
                    || guh.getRandom().nextInt(3) != 0)) {
                return false;
            }
            GuhEntity v = vriendInDeBuurt(guh);
            if (v == null) {
                return false;
            }
            vriend = v;
            uitgenodigd = false;
            taak = null;
            wat = kies(v, test);
            return true;
        }

        /** What to do: the wip / schommel when there is one close by (for both), else a cuddle or a walk. */
        private Wat kies(GuhEntity v, @Nullable Wat test) {
            Speeltje wip = TEST_WIP != null ? TEST_WIP : Speelgoed.van(WIP);
            if (wip != null && guh.level() instanceof ServerLevel level && (test == null ? guh.getRandom().nextInt(2) == 0 : test == Wat.SPELEN)) {
                KlusTaak mijn = wip.zoek(level, guh, guh.blockPosition(), 16);
                if (mijn != null) {
                    KlusTaak zijn = wip.zoek(level, v, guh.blockPosition(), 16);
                    if (zijn != null) {
                        taak = mijn;
                        nodigUit(guh, v, Wat.SPELEN, zijn);
                        return Wat.SPELEN;
                    }
                    mijn.stop();
                }
            }
            Wat w = test != null && test != Wat.SPELEN ? test : guh.getRandom().nextInt(5) < 2 ? Wat.KNUFFELEN : Wat.LOPEN;
            nodigUit(guh, v, w, null);
            return w;
        }

        @Override
        public boolean canContinueToUse() {
            return vriend != null && vriend.isAlive() && tijd < max() && !guh.isOrderedToSit() && !guh.isPassenger() && !guh.isLeashed()
                    && !Huisjes.isBinnen(guh) && vriend.level() == guh.level() && vriend.distanceTo(guh) < 24 && baasDichtbij(guh);
        }

        /** The owner (when online, in this world) is still close enough: else following the owner goes first again. */
        private static boolean baasDichtbij(GuhEntity guh) {
            ServerPlayer owner = Band.eigenaarOnline(guh);
            return owner == null || owner.level() != guh.level() || owner.distanceTo(guh) < BAAS_BEREIK;
        }

        private int max() {
            return switch (wat) {
                case LOPEN -> 20 * 12;
                case KNUFFELEN -> 20 * 8;
                case SPELEN -> taak != null ? taak.maxTicks() : 20 * 20;
            };
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            tijd = 0;
            geknuffeld = false;
            GuhHooks.bezig(guh, max());
        }

        @Override
        public void stop() {
            if (taak != null) {
                taak.stop();
            }
            if (wat == Wat.SPELEN && tijd > 40 && vriend != null && !uitgenodigd) {
                Vriendjes.samen(guh, vriend, SPEEL_PUNTEN);
                Band.moment(guh, Band.eigenaarOnline(guh), Moment.VRIENDJE, Band.id(vriend).toString());
                Band.geefHartjes(guh, Band.eigenaarOnline(guh), Reden.VRIENDJE.standaard(), Reden.VRIENDJE);
            }
            taak = null;
            vriend = null;
            guh.getNavigation().stop();
            guh.getPersistentData().putLong(RUST_TOT, guh.level().getGameTime() + RUST + guh.getRandom().nextInt(RUST));
            GuhHooks.bezig(guh, 0);
        }

        @Override
        public void tick() {
            tijd++;
            GuhEntity v = vriend;
            if (v == null) {
                return;
            }
            switch (wat) {
                case SPELEN -> {
                    if (taak == null || !taak.tick()) {
                        tijd = max();
                    }
                }
                case LOPEN -> loop(v);
                case KNUFFELEN -> knuffel(v);
            }
        }

        /** Walking together: the invited one follows the other at its side; the other strolls on. */
        private void loop(GuhEntity v) {
            if (uitgenodigd) {
                Vec3 kant = v.position().add(new Vec3(-Math.cos(Math.toRadians(v.yBodyRot)), 0, -Math.sin(Math.toRadians(v.yBodyRot))).scale(1.2));
                if (guh.position().distanceTo(kant) > 1.2 && tijd % 5 == 0) {
                    guh.getNavigation().moveTo(kant.x, kant.y, kant.z, 1.05);
                }
            } else if (guh.getNavigation().isDone()) {
                Vec3 heen = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPos(guh, 8, 3);
                if (heen != null) {
                    guh.getNavigation().moveTo(heen.x, heen.y, heen.z, 0.8);
                }
            }
            if (tijd % 60 == 30 && guh.level() instanceof ServerLevel level && guh.getRandom().nextInt(2) == 0) {
                level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.1, guh.getZ(), 1, 0.1, 0.05, 0.1, 0.01);
            }
            guh.getLookControl().setLookAt(v, 20f, 20f);
        }

        /** A cuddle: walk up to the friend, both hug (KNUFFELEN), hearts between them. */
        private void knuffel(GuhEntity v) {
            guh.getLookControl().setLookAt(v, 30f, 30f);
            if (geknuffeld) {
                return;
            }
            if (guh.distanceTo(v) > 1.1 + guh.getBbWidth() * 0.5 + v.getBbWidth() * 0.5) {
                if (tijd % 10 == 1) {
                    guh.getNavigation().moveTo(v, 1.0);
                }
                return;
            }
            guh.getNavigation().stop();
            geknuffeld = true;
            knuffelSamen(guh, v, !uitgenodigd);
        }
    }

    /** Two friends hug (both KNUFFELEN, facing each other, hearts between them); the one who asked counts the points. */
    static void knuffelSamen(GuhEntity guh, GuhEntity v, boolean tellen) {
        SamenSpel.kijk(guh, v.position());
        guh.emotes.start(Emote.KNUFFELEN, false, GuhEmotes.Source.SELF);
        if (!(guh.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 midden = guh.position().add(v.position()).scale(0.5);
        level.sendParticles(BandFeature.HARTJE.get(), midden.x, midden.y + Math.max(guh.getBbHeight(), v.getBbHeight()) + 0.2, midden.z, 5,
                0.25, 0.15, 0.25, 0.02);
        if (guh.isTame() && guh.areSoundsEnabled()) {
            level.playSound(null, guh.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.5f, guh.getVoicePitch() * 1.25f);
        }
        if (tellen) {
            Vriendjes.samen(guh, v, KNUFFEL_PUNTEN);
            ServerPlayer owner = Band.eigenaarOnline(guh);
            Band.moment(guh, owner, Moment.VRIENDJE, Band.id(v).toString());
            Band.moment(v, owner, Moment.VRIENDJE, Band.id(guh).toString());
            Band.geefHartjes(guh, owner, Reden.VRIENDJE.standaard(), Reden.VRIENDJE);
            Band.geefHartjes(v, owner, Reden.VRIENDJE.standaard(), Reden.VRIENDJE);
            if (owner != null && owner.level() == guh.level() && owner.distanceTo(guh) < 24) {
                nl.juiced.guhs.quest.GuhAdvancements.grant(owner, "samen_vriendjes_knuffel");
            }
        }
        level.sendParticles(ParticleTypes.HEART, midden.x, midden.y + 0.8, midden.z, 1, 0, 0, 0, 0);
    }

    /** (Tests) forget the invitations. */
    static void wis() {
        UITNODIGINGEN.clear();
        TEST_NU.clear();
        TEST_WIP = null;
    }

    /** (Tests) is there an invitation waiting for this guh? */
    static boolean uitgenodigd(GuhEntity guh) {
        return UITNODIGINGEN.containsKey(guh.getUUID());
    }
}
