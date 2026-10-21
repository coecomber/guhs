package nl.juiced.guhs.feature.safari.rit;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * The glass-bottom boat (round 3, J2; round 4: the boat LINE). A route with jetties ({@link RitData.Bootroute#haltes}) is
 * the one line up the river from the hoofdsteiger: {@link RitSchema#boten} boats run it and stop
 * {@link RitSchema#BOOT_LIJN_WACHT} ticks at every jetty (hop on, hop off, like the truck); on the way to a jetty the
 * skipper tells a little thing about what lives there. A route without jetties is the old calm loop with one stop at its
 * jetty. Get in and out only while it stands at a jetty. Model: geckolib safari_rit/glasboot (tools/features/safari_rit.py).
 */
public class BootEntity extends SchemaRit {
    public static final int PLAATSEN = 6, GUH_PLAATSEN = 2, VERHALEN = 3;
    /** The timetable number k = route * PER_ROUTE + boat number on that route. */
    public static final int PER_ROUTE = 100;
    private static final EntityDataAccessor<Boolean> DATA_STIL = SynchedEntityData.defineId(BootEntity.class, EntityDataSerializers.BOOLEAN);
    /** Seats (model pixels): the benches along both sides. */
    private static final double[][] ZIT = {{-17, 5, -24}, {17, 5, -24}, {-17, 5, -8}, {17, 5, -8}, {-17, 5, 8}, {17, 5, 8}, {-17, 5, 24}, {17, 5, 24}};

    private int vorigDeel = -1;
    private int vorigeHalte = -2;

    public BootEntity(EntityType<? extends BootEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STIL, true);
    }

    public boolean staatStil() {
        return entityData.get(DATA_STIL);
    }

    @Override
    protected void plaats(long t, boolean snap) {
        RitData d = RitData.get();
        int route = k() / PER_ROUTE;
        if (route >= d.boten.size()) {
            return;
        }
        RitData.Bootroute r = d.boten.get(route);
        double l = r.pad().lengte();
        if (!r.haltes().isEmpty()) {
            lijn(r, t, snap);
            return;
        }
        RitSchema.Stand s = RitSchema.boot(t, l);
        zet(r.pad().op(s.afstand()), r.pad().richting(s.afstand(), getYRot()), snap);
        if (level().isClientSide()) {
            return;
        }
        if (s.staatStil() != staatStil()) {
            entityData.set(DATA_STIL, s.staatStil());
            if (s.staatStil() && !snap) {
                vertel(Component.translatable("gui.guhs.safari.boot.terug"));
            }
        }
        // the skipper's three little stories, a quarter, half and three quarters along the way
        int deel = s.staatStil() ? -1 : (int) (s.afstand() / Math.max(1, l) * (VERHALEN + 1));
        if (deel != vorigDeel && deel >= 1 && deel <= VERHALEN && !snap) {
            vertel(Component.translatable("gui.guhs.safari.boot." + r.zone() + "." + (deel - 1)));
        }
        vorigDeel = deel;
    }

    /** The boat line: stop at every jetty; arriving, the skipper names it; leaving, he tells about the next one. */
    private void lijn(RitData.Bootroute r, long t, boolean snap) {
        RitSchema.Stand s = RitSchema.bootLijn(t, k() % PER_ROUTE, n(), r.pad().lengte(), r.halteAfstanden());
        zet(r.pad().op(s.afstand()), r.pad().richting(s.afstand(), getYRot()), snap);
        if (level().isClientSide()) {
            return;
        }
        if (s.staatStil() != staatStil()) {
            entityData.set(DATA_STIL, s.staatStil());
        }
        if (snap) {
            vorigeHalte = s.halte();
            return;
        }
        if (s.halte() >= 0 && s.halte() != vorigeHalte) {
            RitData.Halte h = r.haltes().get(s.halte());
            vertel(s.halte() == 0 ? Component.translatable("gui.guhs.safari.boot.terug")
                    : Component.translatable("gui.guhs.safari.boot.halte", Component.translatable("gui.guhs.safari.zone." + h.zone())));
        } else if (s.halte() < 0 && vorigeHalte >= 0) {
            RitData.Halte volgende = r.haltes().get((vorigeHalte + 1) % r.haltes().size());
            if (!"entree".equals(volgende.zone())) {
                int verhaal = Math.floorMod(vorigeHalte + k(), VERHALEN);
                vertel(Component.translatable("gui.guhs.safari.boot." + volgende.zone() + "." + verhaal));
            }
        }
        vorigeHalte = s.halte();
    }

    private void vertel(Component tekst) {
        Component regel = Component.translatable("gui.guhs.safari.boot.schipper", tekst).withStyle(ChatFormatting.AQUA);
        for (Entity e : getPassengers()) {
            if (e instanceof ServerPlayer p) {
                p.sendSystemMessage(regel);
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer p) {
            if (!staatStil()) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.safari.boot.onderweg"));
            } else if (spelers() >= PLAATSEN) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.safari.rit.vol"));
            } else if (p.startRiding(this)) {
                neemGuhsMee(p, GUH_PLAATSEN);
                p.sendOverlayMessage(Component.translatable("gui.guhs.safari.boot.instappen"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (passenger instanceof GuhEntity) {
            return guhs() < GUH_PLAATSEN;
        }
        return passenger instanceof Player && spelers() < PLAATSEN;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int i = Math.max(0, getPassengers().indexOf(passenger));
        double[] z = ZIT[Math.min(i, ZIT.length - 1)];
        return stoel(z[0], z[1] - 4, z[2]);
    }

    private static final RawAnimation DOBBER = RawAnimation.begin().thenLoop("animation.glasboot.dobber");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("dobber", 10, state -> state.setAndContinue(DOBBER)));
    }
}
