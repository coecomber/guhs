package nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenRegistry;

/**
 * Takelmeester Taleggio (Kaaszeppelinwerf boss 1, lv 38, Natuur: rope and wood).
 * <ul>
 *   <li>Platform cut: red circles = that platform drops in 3 s (40 % + knocked down); pink circles = the safe rigging to jump to.</li>
 *   <li>Knife throws at whoever stands at range, a rope-swing leap at a player (red circle).</li>
 *   <li>Fase 2 (50 %): he hangs high on his ropes and swings around: ranged and magic hit him; melee pulls him down with one of
 *       the 3 rope levers (pink circle) when he swings over it: he falls and is stunned 5 s (+50 % damage taken). He keeps throwing
 *       knives and cutting platforms from up there. Vahoeg: he cuts two platforms at a time and the levers need him closer.</li>
 * </ul>
 */
public class Taleggio extends Z3Baas {
    public static final String H_TOUWEN = "z3_taleggio_touwen";
    private static final EntityDataAccessor<Boolean> HANGT = SynchedEntityData.defineId(Taleggio.class, EntityDataSerializers.BOOLEAN);
    /** The 3 rope levers (offsets from the centre). */
    public static final int[][] TOUWEN = {{7, 0, 0}, {-4, 0, 6}, {-4, 0, -6}};
    private int snijCd = 60, mesCd = 40, sprongCd = 120, valT;
    private double hoek;
    private boolean opgezet;
    /** Gametests: times pulled down. */
    public int neergetrokken;

    public Taleggio(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(HANGT, false);
    }

    /** Fase 2: hanging on the ropes (server logic). */
    private boolean hangFase;

    public boolean hangt() {
        return hangFase;
    }

    /** Client: hanging right now (not fallen). */
    public boolean hangtZichtbaar() {
        return entityData.get(HANGT);
    }

    @Override
    public String baasId() {
        return "taleggio";
    }

    @Override
    public String kerker() {
        return "werf";
    }

    @Override
    protected boolean isMika() {
        return true;
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"slag", "worp", "sprong"};
    }

    @Override
    protected double hp(int level) {
        return 5400 + 230 * level;
    }

    @Override
    protected double klap(int level) {
        return 16 + 2.0 * level;
    }

    @Override
    public int xp() {
        return 3200 + 120 * mobLevel();
    }

    @Override
    public Element standaardElement() {
        return Element.NATUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.5};
    }

    @Override
    protected void bijt(ServerLevel sl, ServerPlayer d) {
        if (!hangt() || valT > 0) {
            super.bijt(sl, d);
        }
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        return valT > 0 ? 1.5 : 1;
    }

    @Override
    protected void opFase(int fase) {
        if (fase == 2 && level() instanceof ServerLevel sl) {
            kaart("vadsrijk.z3bazen.ta.fase2", PAARS);
            hangFase = true;
            entityData.set(HANGT, true);
            setNoGravity(true);
            zetTouwen(sl);
            zeg("vadsrijk.z3bazen.ta.hangt", ChatFormatting.LIGHT_PURPLE);
        }
    }

    public List<BlockPos> touwen() {
        List<BlockPos> l = new ArrayList<>();
        for (int[] o : TOUWEN) {
            l.add(blok(o));
        }
        return l;
    }

    private void zetTouwen(ServerLevel sl) {
        for (BlockPos b : touwen()) {
            if (sl.getBlockState(b).isAir() || sl.getBlockState(b).canBeReplaced()) {
                Herstel.zet(sl, H_TOUWEN, b, Z3BazenRegistry.HENDEL.get().defaultBlockState());
            }
        }
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        int n = vahoeg() ? 2 : 1;
        if (snijCd <= 0) {
            snij(sl, l, n + (fase() >= 2 ? 1 : 0));
            snijCd = hangt() ? 140 : 180;
            laatsteAanval = "snij";
            rustT = 50;
            return;
        }
        if (mesCd <= 0) {
            ServerPlayer v = iemandOpAfstand() || hangt() ? afstandsDoel() : zDoel();
            if (v != null) {
                speel("worp");
                inslag(voetVan(v), 2.5, 25, 0.25, "mes", null, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()));
                mesCd = hangt() ? 40 : 70;
                laatsteAanval = "mes";
                rustT = 20;
                return;
            }
        }
        if (!hangt() && sprongCd <= 0) {
            ServerPlayer v = afstandsDoel();
            if (v != null) {
                Vec3 doel = voetVan(v);
                Telegraaf.cirkel(sl, doel, 3, tg(22), ROOD);
                speel("sprong");
                later(tg(22), () -> zSpring(doel, 14, 5));
                sprongCd = 160;
                laatsteAanval = "sprong";
                rustT = 50;
                return;
            }
        }
        rustT = 10;
    }

    /** Red = this platform falls in 3 s; pink = the safe rigging. */
    private void snij(ServerLevel sl, List<ServerPlayer> l, int n) {
        Vec3 m = zMidden();
        List<Vec3> rood = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ServerPlayer p = l.get(getRandom().nextInt(l.size()));
            Vec3 v = i == 0 ? voetVan(p) : ring(m, 4 + getRandom().nextDouble() * 6, getRandom().nextDouble() * Math.PI * 2);
            rood.add(new Vec3(v.x, m.y, v.z));
        }
        for (int i = 0; i < 2; i++) {   // the safe rigging: away from every red circle
            Vec3 best = m;
            double bd = -1;
            for (int k = 0; k < 10; k++) {
                Vec3 c = ring(m, 3 + getRandom().nextDouble() * (zStraal() - 5), getRandom().nextDouble() * Math.PI * 2);
                double d = rood.stream().mapToDouble(r -> r.distanceTo(c)).min().orElse(99);
                if (d > bd) {
                    bd = d;
                    best = c;
                }
            }
            Telegraaf.cirkel(sl, best, 1.6, tg(60), ROZE);
        }
        for (Vec3 v : rood) {
            inslag(v, 3.5, 60, 0.4, "platform", Status.PLAKKERIG, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_PLANKS.defaultBlockState()));
        }
        speel("slag");
        sl.playSound(null, blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.HOSTILE, 2f, 0.6f);
        zeg("vadsrijk.z3bazen.ta.snij", ChatFormatting.RED);
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        snijCd--;
        mesCd--;
        sprongCd--;
        if (!opgezet) {
            opgezet = true;
            Herstel.terug(sl, H_TOUWEN);
        }
        if (hangt()) {
            if (valT > 0) {
                if (--valT == 0) {
                    zeg("vadsrijk.z3bazen.ta.klimt", ChatFormatting.GOLD);
                    setNoGravity(true);
                    entityData.set(HANGT, true);
                }
                return;
            }
            // swinging around high above the floor
            hoek += 0.035;
            Vec3 m = zMidden();
            Vec3 v = new Vec3(m.x + Math.cos(hoek) * 6.5, m.y + 7 + Math.sin(hoek * 2) * 0.8, m.z + Math.sin(hoek) * 6.5);
            setPos(v.x, v.y, v.z);
            setDeltaMovement(Vec3.ZERO);
            getNavigation().stop();
            if (tickCount % 20 == 0) {
                for (BlockPos b : touwen()) {
                    Telegraaf.cirkel(sl, Vec3.atBottomCenterOf(b), 1.2, 20, ROZE);
                }
            }
        }
    }

    /** A rope lever was pulled: if he swings close above it, he falls. */
    public boolean touw(ServerLevel sl, BlockPos b, ServerPlayer p) {
        if (!hangt() || valT > 0 || !zDoetMee(p)) {
            return false;
        }
        double dx = getX() - (b.getX() + 0.5), dz = getZ() - (b.getZ() + 0.5);
        double max = vahoeg() ? 3.5 : 5;
        if (dx * dx + dz * dz > max * max) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("vadsrijk.z3bazen.ta.te_ver").withStyle(ChatFormatting.GOLD));
            return false;
        }
        neergetrokken++;
        valT = 100;
        entityData.set(HANGT, false);
        setNoGravity(false);
        Statussen.geef(this, Status.VERDOOFD, 100, 1, null);
        setPos(getX(), zMidden().y + 0.2, getZ());
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 30, 1, 0.3, 1, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1f, 0.8f);
        zeg("vadsrijk.z3bazen.ta.gevallen", ChatFormatting.GREEN);
        return true;
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd() && level() instanceof ServerLevel sl) {
            hangFase = false;
            entityData.set(HANGT, false);
            setNoGravity(false);
            spreek("vadsrijk.z3bazen.ta.vlucht");
            Herstel.terug(sl, H_TOUWEN);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.DISCARDED && level() instanceof ServerLevel sl && !geplofd()) {
            Herstel.terug(sl, H_TOUWEN);
        }
        super.remove(reason);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("idle"), walk = RawAnimation.begin().thenLoop("walk"), hang = RawAnimation.begin().thenLoop("hang");
        controllers.add(new AnimationController<Taleggio>("beweeg", 4,
                state -> state.setAndContinue(state.animatable().hangtZichtbaar() ? hang : state.isMoving() ? walk : idle)));
        AnimationController<Taleggio> a = new AnimationController<>("actie", 2, state -> PlayState.STOP);
        for (String n : actieAnimaties()) {
            a.triggerableAnim(n, RawAnimation.begin().thenPlay(n));
        }
        controllers.add(a);
    }
}
