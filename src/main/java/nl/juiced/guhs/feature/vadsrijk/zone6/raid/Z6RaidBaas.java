package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.vfx.BaasAnims;
import nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx;
import nl.juiced.guhs.feature.vadsrijk.vfx.Deeltje;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vfx;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;

/**
 * U-Z6RAID: the base of the four raid bosses (and the two companion Sterrenbeelden and the Spiegelvizier's images/clones): the
 * whole zone-3 engine ({@link Z3Baas}: rotation, fraction-of-max-Vads mechanics capped at 60 %, immune/floor while a mechanic waits,
 * per-tick tasks, enrage) with the raid rules:
 * <ul>
 *   <li><b>Scaling</b> from {@link Z6Schaal} with the group size at the start ({@link #n()}), not with who stands in the arena;</li>
 *   <li><b>Who fights</b>: only members of the raid group in this floor's arena ({@link Z6Raid#doetMee}); everyone else is ignored
 *       (and can't hit it). Gametests set {@link #testN} and use {@link #testFilter} instead;</li>
 *   <li><b>Interrupts</b> ({@link #giet}): a purple cast bar with a break meter; enough damage during the cast ({@code breek} x max
 *       Vads, e.g. 4 %) stops it and stuns the boss 2 s; otherwise the follow-up runs;</li>
 *   <li>texts under {@code vadsrijk.z6raid.<id>}, music ids per boss + phase ({@link Z6RaidMuziek}), cheer-up at the end
 *       (confetti, stars; never a death), chests from {@link Z6RaidLoot}.</li>
 * </ul>
 */
public abstract class Z6RaidBaas extends Z3Baas {
    public static final int LEVEL = 70;
    public static final int STERGOUD = 0xFFD86A, STERWIT = 0xE8F0FF, ZAND = 0xF2B48C, SPIEGEL = 0xBFEAFF;
    /** Gametests: a fixed group size (0 = the raid's). */
    public int testN;
    private int geschaaldN = -1;
    // the running interruptible cast
    @Nullable
    private Runnable gietKlaar, gietBreuk;
    private int gietT, gietDuur;
    private double gietSchade, gietNodig;
    /** Gametests: interrupts that worked. */
    public int onderbroken;

    protected Z6RaidBaas(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /** The floor (1-4). */
    public abstract int verdieping();

    /** Base HP at lv 70 for ONE player-equivalent (x {@link Z6Schaal#factor} for the group). */
    protected abstract double basisHp();

    /** Base hit at lv 70 (x {@link Z6Schaal#schadeFactor}). */
    protected abstract double basisKlap();

    /** Is this the floor's real boss (chests, the raid's progress)? The companions/images/clones say false. */
    public boolean echteBaas() {
        return true;
    }

    public int n() {
        return testN > 0 ? Z6Schaal.n(testN) : Z6Raid.n();
    }

    /** Mechanic count for this group (ceil(n/2), at least 2). */
    public int m() {
        return Z6Schaal.mech(n());
    }

    @Override
    public String titelKey() {
        return "vadsrijk.z6raid." + baasId();
    }

    @Override
    protected double hp(int level) {
        return basisHp() * (1 + 0.02 * (level - LEVEL));
    }

    @Override
    protected double klap(int level) {
        return basisKlap() * (1 + 0.02 * (level - LEVEL)) * Z6Schaal.schadeFactor(n());
    }

    @Override
    public int xp() {
        return echteBaas() ? nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Xp.baas(mobLevel()) : 0;
    }

    @Override
    public Element standaardElement() {
        return Element.ROZE;
    }

    /** HP scales with the group size at the START (kern's arena count is ignored). */
    @Override
    public void schaal(int spelers) {
        int nu = n();
        if (nu == geschaaldN || geplofd()) {
            return;
        }
        geschaaldN = nu;
        zetMaxVads(vadsOp(mobLevel()) * Z6Schaal.factor(nu), true);
    }

    @Override
    public int losseStraal() {
        return 0;
    }

    @Override
    public boolean zDoetMee(Player p) {
        if (p.isSpectator() || !p.isAlive()) {
            return false;
        }
        if (testN > 0) {
            Vec3 m = zMidden();
            double dx = p.getX() - m.x, dz = p.getZ() - m.z;
            return dx * dx + dz * dz <= 24 * 24 && Math.abs(p.getY() - m.y) < 10;
        }
        return p instanceof ServerPlayer sp && Z6Raid.doetMee(sp, verdieping());
    }

    @Override
    public boolean inGevecht(Player p) {
        return zDoetMee(p);
    }

    @Override
    public boolean magIntro(ServerPlayer p) {
        return testN > 0 || Z6Raid.inGroep(p) || p.isCreative();
    }

    @Override
    public void intro(ServerPlayer p, boolean eersteKeer) {
        super.intro(p, eersteKeer);
        Tips.toon(p, "vadsrijk.z6raid.tip." + baasId());
    }

    @Override
    protected int enrageTicks() {
        return 20 * 60 * 8;
    }

    /** The enrage time in seconds (the raid HUD counts down to it). */
    public int enrageSeconden() {
        return enrageTicks() / 20;
    }

    /** Extra bars in the raid HUD: {lang key, fraction 0..1, done?} (the Sterrenbeelden show all three). */
    public List<Object[]> hudExtra() {
        return List.of();
    }

    @Override
    protected void enrage(net.minecraft.server.level.ServerLevel sl) {
        kaart("vadsrijk.z6raid.enrage", ROOD);
    }

    @Nullable
    @Override
    public net.minecraft.resources.Identifier muziek() {
        return echteBaas() ? Z6RaidMuziek.id(baasId(), fase()) : null;
    }

    @Override
    protected LootTabel.Resultaat kistInhoud(ServerPlayer p, boolean eersteKill) {
        return echteBaas() ? Z6RaidLoot.baasKist(baasId(), p, getRandom()) : new LootTabel.Resultaat(List.of(), 0);
    }

    @Override
    protected void beloon(ServerLevel sl, Set<ServerPlayer> helpers) {
        if (echteBaas()) {
            super.beloon(sl, helpers);
        }
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd() && echteBaas() && level() instanceof ServerLevel sl) {
            Z6Raid.baasVerslagen(sl, this);
        }
    }

    /** Cheered up, never "dead": stars, confetti and hearts, a happy chime. */
    @Override
    protected void opPlof() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 v = position().add(0, getBbHeight() / 2, 0);
        double r = Math.max(1.2, getBbWidth());
        Vfx.deeltjes(sl, Deeltje.STER, v, (int) (24 * r), STERGOUD, r * 0.7, 0.12);
        Vfx.deeltjes(sl, Deeltje.CONFETTI, v, (int) (18 * r), 0xFF8AD0, r * 0.7, 0.15);
        Vfx.deeltjes(sl, Deeltje.HART, v.add(0, r * 0.5, 0), 6, 0xFF6AB0, r * 0.4, 0.05);
        Vfx.flits(sl, v, r * 1.4, 0xFFE8B0);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2f, 1.3f);
        sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.5f);
    }

    // ---- interruptible casts ------------------------------------------------------------------------------------------------

    /**
     * Starts an interruptible cast ("&lt;baas&gt; laadt: &lt;naam&gt;", purple with a break meter): {@code breek} x max Vads of damage
     * during the cast stops it (2 s stun, {@code bijBreuk}); else {@code klaar} runs at the end.
     */
    public void giet(String aanvalId, int ticks, double breek, Runnable klaar, @Nullable Runnable bijBreuk) {
        gietT = 0;
        gietDuur = Math.max(10, ticks);
        gietSchade = 0;
        gietNodig = Math.max(1, maxVads() * breek);
        gietKlaar = klaar;
        gietBreuk = bijBreuk;
        Vfx.cast(this, "vadsrijk.vfx.aanval." + aanvalId, gietDuur, true);
        BaasVfx.windup(this, gietDuur);
        getNavigation().stop();
    }

    public boolean giet() {
        return gietKlaar != null;
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        double voor = vads();
        super.krijgSchade(n, bron);
        double echt = voor - vads();
        if (gietKlaar != null && echt > 0) {
            gietSchade += echt;
            Vfx.castBreuk(this, (float) Math.min(1, gietSchade / gietNodig));
            if (gietSchade >= gietNodig) {
                Runnable b = gietBreuk;
                gietKlaar = null;
                gietBreuk = null;
                onderbroken++;
                Vfx.castStop(this, true);
                nl.juiced.guhs.feature.vadsrijk.kern.Statussen.geef(this, nl.juiced.guhs.feature.vadsrijk.kern.Status.VERDOOFD, 40, 1, null);
                if (level() instanceof ServerLevel sl) {
                    Vfx.deeltjes(sl, Deeltje.STER, position().add(0, getBbHeight() + 0.3, 0), 12, 0xFFF0A0, 0.6, 0.05);
                }
                zeg("vadsrijk.z6raid.onderbroken", ChatFormatting.LIGHT_PURPLE);
                if (b != null) {
                    b.run();
                }
            }
        }
    }

    /** Called from {@link #vecht} by subclasses first: runs the cast timer. True while a cast runs (do nothing else). */
    protected boolean gietTick() {
        if (gietKlaar == null) {
            return false;
        }
        if (++gietT >= gietDuur) {
            Runnable k = gietKlaar;
            gietKlaar = null;
            gietBreuk = null;
            Vfx.castStop(this, false);
            k.run();
            return false;
        }
        return true;
    }

    @Override
    protected boolean bezig() {
        return giet();
    }

    /** A line for the whole group (the boss "speaks"). */
    public void zegGroep(String key, Object... args) {
        spreek(key, args);
    }

    /** A title card for everyone in the fight. */
    public void kaartAlle(String key, int kleur) {
        kaart(key, kleur);
    }

    /** The overlay text for one player. */
    public static void meld(ServerPlayer p, String key, ChatFormatting kleur, Object... args) {
        p.sendOverlayMessage(Component.translatable(key, args).withStyle(kleur));
    }

    // ---- GeckoLib: beweeg + actie + T-VFX's "vfx" (Z3Baas leaves the vfx controller out) ---------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("idle"), walk = RawAnimation.begin().thenLoop("walk");
        controllers.add(new AnimationController<>("beweeg", 4, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<Z6RaidBaas> actie = new AnimationController<>("actie", 2, state -> PlayState.STOP);
        for (String a : actieAnimaties()) {
            actie.triggerableAnim(a, RawAnimation.begin().thenPlay(a));
        }
        controllers.add(actie);
        BaasAnims.controllers(controllers);
    }

    /** Plays one of this boss's own trigger animations (public: the raid and gametests). */
    public void anim(String a) {
        speel(a);
    }
}
