package nl.juiced.guhs.feature.vogels;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.gids.GidsFeature;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;

/**
 * The guh-uiltje: a round cocoa-pink owl whose ear tufts are two guh ears. By day it sleeps on its perch (eyes closed,
 * fluffed up; only wakes up when you're right next to it); at night it flies around with glowing eyes and calls
 * "Oehoe-njeg!". Its body stays still while it sits, but its head turns all the way round to keep an eye on you.
 */
public class GuhUiltjeEntity extends Vogeltje {
    /** How far its head may turn from its body (degrees). */
    public static final int KOP_DRAAI = 175;

    private int roepTijd = 200;

    public GuhUiltjeEntity(EntityType<? extends Vogeltje> type, Level level) {
        super(type, level);
    }

    @Override
    public String naam() {
        return "guh_uiltje";
    }

    @Override
    protected double vliegSnelheid() {
        return 0.28;
    }

    @Override
    public boolean lekker(ItemStack stack) {
        return stack.is(VogelTags.UILTJESHAPJES);
    }

    @Override
    protected SoundEvent roep() {
        return VogelsFeature.OEHOE.get();
    }

    /** Is it night here (the owl is awake)? */
    public boolean nacht() {
        return level().isNight();
    }

    public boolean slaapt() {
        return houding() == SLAAPT;
    }

    @Override
    public double schrikAfstand() {
        return slaapt() ? 1.5 : 3.0;
    }

    @Override
    protected boolean wilVliegen() {
        return nacht();
    }

    @Override
    protected void zitStap() {
        if (!nacht()) {
            if (!slaapt() && onGround()) {
                zetHouding(SLAAPT);
            }
            return;
        }
        if (slaapt()) {
            zetHouding(STAAT);
            zitTijd = Math.min(zitTijd, 200 + random.nextInt(400));
        }
        if (--roepTijd <= 0) {
            oehoe();
        }
    }

    @Override
    protected boolean vliegStap() {
        if (nacht() && --roepTijd <= 0) {
            oehoe();
        }
        return false;
    }

    /** "Oehoe-njeg!" (players around it hear it: an advancement at night). */
    public void oehoe() {
        roepTijd = 240 + random.nextInt(400);
        triggerAnim("actie", "roep");
        level().playSound(null, getX(), getY(), getZ(), VogelsFeature.OEHOE.get(), SoundSource.NEUTRAL, 1.0f, 0.95f + random.nextFloat() * 0.1f);
        if (nacht()) {
            for (ServerPlayer p : kijkers(16)) {
                GidsFeature.grant(p, "diertjes/vogels_oehoe");
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;           // it calls with oehoe(), only at night
    }

    @Override
    protected void gevoerd(ServerPlayer player, ItemStack stack) {
        if (slaapt()) {
            zetHouding(STAAT);
        }
        oehoe();
    }

    // --- the head that turns all the way round ----------------------------------------------------------------------------
    @Override
    public int getMaxHeadYRot() {
        return vliegt() ? 40 : KOP_DRAAI;
    }

    @Override
    public int getHeadRotSpeed() {
        return 6;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                if (vliegt()) {
                    super.clientTick();         // sitting: the body keeps still, only the head turns
                }
            }
        };
    }

    @Override
    protected String[] extraActies() {
        return new String[]{"roep"};
    }

    @Override
    protected RawAnimation beweging(AnimationTest<Vogeltje> state) {
        if (!vliegt() && slaapt()) {
            return anim("slaap", true);
        }
        return super.beweging(state);
    }
}
