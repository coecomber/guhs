package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensRegistry;

/**
 * Kootje Zilverstaart (the spy finale of "Verraad in de stad", in the Wolkwaldroom; lv 60, Vuur): the Mika of the Ringvinger who
 * hid in the top of the Wolkwacht. A slim Mika with a silver tail tip, a long hatpin and a stolen Wolkwacht cloak.
 * <ul>
 *   <li><b>Fase 1:</b> Hoedenspeldsprong (red strip: he lunges through it, 30 %), Rookbom (grey circle on someone: 15 % + can't
 *       see; then he blinks behind whoever stands furthest away), hatpin stabs up close.</li>
 *   <li><b>Fase 2 (70 %) - De Vermommingen:</b> smoke! Five Wolkwacht knights stand in a ring; one of them is him (look for the
 *       SILVER glint at the tail). Hit the real one: unmasked, the decoys pop and he takes +50 % for 8 s. Hit a decoy ("Een
 *       lokpop!"): it pops and you take 10 %. Not found within 25 s: he jumps out laughing (25 % on everyone). Again every ~50 s.</li>
 *   <li><b>Fase 3 (35 %) - De ontsnapping:</b> he runs for the blowhole and opens three grey-gas valves to cloud the whale: 6 % on
 *       everyone every 3 s while one is open, and he can't drop below 10 % until all three are shut (right-click each 3 times).
 *       He keeps throwing hatpins along red lanes.</li>
 * </ul>
 * Only spawns when the story asks ({@link Z5Bazen#start}); beaten = {@code Z5Finale.spionVerslagen(p)} for every helper.
 */
public class Zilverstaart extends Z5Baas {
    private static final EntityDataAccessor<Boolean> VERMOMD = SynchedEntityData.defineId(Zilverstaart.class, EntityDataSerializers.BOOLEAN);
    public static final int LOKPOPPEN = 4, VENTIELEN = 3, KLIKS = 3, ZOEK_TICKS = 20 * 25;
    private int sprongCd = 80, rookCd = 160, vermomCd = 30, vermomT = -1, ontmaskerdT = 0, sprongT = -1, speldCd = 60;
    private Vec3 van = Vec3.ZERO, naar = Vec3.ZERO;
    private final Set<UUID> gesprongen = new HashSet<>();
    private final List<UUID> lokpoppen = new ArrayList<>();
    private final List<UUID> ventielen = new ArrayList<>();
    private final int[] kliks = new int[VENTIELEN];
    private final boolean[] dicht = new boolean[VENTIELEN];
    private boolean ontsnapping;
    /** Gametests. */
    public int lokpopMis, gevonden;

    public Zilverstaart(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VERMOMD, false);
    }

    public boolean vermomd() {
        return entityData.get(VERMOMD);
    }

    @Override
    public String baasId() {
        return "zilverstaart";
    }

    @Override
    protected boolean isMika() {
        return true;
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"aanval", "spuug", "speciaal", "klap"};
    }

    @Override
    protected double hp(int level) {
        return 12000 + 400 * level;
    }

    @Override
    protected double klap(int level) {
        return 24 + 2.6 * level;
    }

    @Override
    public Element standaardElement() {
        return Element.VUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.7, 0.35};
    }

    @Override
    protected double jaagSnelheid() {
        return 1.25;
    }

    @Override
    protected boolean bezig() {
        return vermomd() || sprongT >= 0;
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        return ontmaskerdT > 0 ? 1.5 : 1;
    }

    @Override
    protected double vadsVloer() {
        return ontsnapping && dichtAantal() < VENTIELEN ? 0.10 : 0;
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z5wezens.zs.ventielen_eerst";
    }

    public int dichtAantal() {
        int n = 0;
        for (boolean b : dicht) {
            if (b) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected void opFase(int fase) {
        kaart("vadsrijk.z5wezens.zs.fase" + fase, fase == 3 ? ROOD : PAARS);
        if (fase == 2) {
            vermomCd = 20;
        }
        if (fase == 3 && level() instanceof ServerLevel sl) {
            ontsnap(sl);
        }
    }

    // ---- fase 2: the disguises ---------------------------------------------------------------------------------------------------

    /** Starts the disguise round now (dev/tests too). */
    public void vermom(ServerLevel sl) {
        rook(sl, position(), 1.2);
        entityData.set(VERMOMD, true);
        getNavigation().stop();
        int echt = getRandom().nextInt(LOKPOPPEN + 1);
        double start = getRandom().nextDouble() * Math.PI * 2;
        ruimLokpoppen(sl);
        for (int i = 0; i <= LOKPOPPEN; i++) {
            Vec3 v = voetVan(ring(zMidden(), 7, start + i * Math.PI * 2 / (LOKPOPPEN + 1)));
            if (i == echt) {
                teleportTo(v.x, v.y, v.z);
                continue;
            }
            Z3Doel d = Z5WezensRegistry.LOKPOP.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (d != null) {
                d.koppel(this, 100 + i, 50, v);
                d.kijk((float) Math.toDegrees(Math.atan2(-(zMidden().x - v.x), zMidden().z - v.z)));
                sl.addFreshEntity(d);
                lokpoppen.add(d.getUUID());
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, v.x, v.y + 1, v.z, 15, 0.4, 0.6, 0.4, 0.02);
            }
        }
        vermomT = ZOEK_TICKS;
        zeg("vadsrijk.z5wezens.zs.welke", ChatFormatting.LIGHT_PURPLE);
        praat("zs.vermom");
    }

    private void ruimLokpoppen(ServerLevel sl) {
        for (UUID u : lokpoppen) {
            if (sl.getEntity(u) instanceof Z3Doel d) {
                sl.sendParticles(ParticleTypes.POOF, d.getX(), d.getY() + 1, d.getZ(), 15, 0.3, 0.6, 0.3, 0.04);
                d.discard();
            }
        }
        lokpoppen.clear();
    }

    private void ontmasker(ServerLevel sl, boolean gevondenDoorSpeler) {
        entityData.set(VERMOMD, false);
        vermomT = -1;
        ruimLokpoppen(sl);
        rook(sl, position(), 1.2);
        speel("speciaal");
        if (gevondenDoorSpeler) {
            gevonden++;
            ontmaskerdT = 160;
            zeg("vadsrijk.z5wezens.zs.gevonden", ChatFormatting.GOLD);
            praat("zs.betrapt");
        } else {
            praat("zs.haha");
            for (ServerPlayer p : strijders()) {
                mech(p, 0.25, "verrassing");
            }
        }
        vermomCd = 1000;
        rustT = 30;
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        if (vermomd() && bron instanceof ServerPlayer && level() instanceof ServerLevel sl) {
            ontmasker(sl, true);
        }
        super.krijgSchade(n, bron);
    }

    @Override
    public void doelGeraakt(Z3Doel d, double n, @Nullable LivingEntity bron) {
        if (!(level() instanceof ServerLevel sl) || d.nummer < 100) {
            return;
        }
        lokpoppen.remove(d.getUUID());
        sl.sendParticles(ParticleTypes.POOF, d.getX(), d.getY() + 1, d.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        sl.sendParticles(new DustParticleOptions(0xFFD62A, 1.2f), d.getX(), d.getY() + 1.2, d.getZ(), 20, 0.4, 0.5, 0.4, 0);
        sl.playSound(null, d.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 1.4f, 1.4f);
        d.discard();
        if (bron instanceof ServerPlayer p) {
            lokpopMis++;
            Getallen.tekst(sl, d.getX(), d.getY() + 2.2, d.getZ(), Component.translatable("vadsrijk.z5wezens.zs.lokpop"), 0.9f);
            mech(p, 0.10, "lokpop");
        }
    }

    // ---- fase 3: the escape ------------------------------------------------------------------------------------------------------

    private Vec3 ventielPlek(int i) {
        return voetVan(zMidden().add(-11, 0, (i - 1) * 6));
    }

    /** The escape: three gas valves by the blowhole side (dev/tests too). */
    public void ontsnap(ServerLevel sl) {
        if (vermomd()) {
            ontmasker(sl, false);
        }
        ontsnapping = true;
        praat("zs.ontsnap");
        for (UUID u : ventielen) {
            if (sl.getEntity(u) != null) {
                sl.getEntity(u).discard();
            }
        }
        ventielen.clear();
        for (int i = 0; i < VENTIELEN; i++) {
            dicht[i] = false;
            kliks[i] = 0;
            Z3Doel d = Z5WezensRegistry.KLEP.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (d != null) {
                d.koppel(this, i, 100, ventielPlek(i));
                d.zetRaakbaar(false);
                d.zetStand(2);   // 2 = grey gas pouring out
                sl.addFreshEntity(d);
                ventielen.add(d.getUUID());
            }
        }
        getNavigation().moveTo(zMidden().x - 13, zMidden().y, zMidden().z, 1.3);
    }

    @Override
    public void doelGebruikt(Z3Doel d, ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl) || d.nummer < 0 || d.nummer >= VENTIELEN || dicht[d.nummer]) {
            return;
        }
        int i = d.nummer;
        kliks[i]++;
        sl.playSound(null, d.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1f, 0.8f + 0.2f * kliks[i]);
        if (kliks[i] >= KLIKS) {
            dicht[i] = true;
            d.zetStand(0);
            sl.sendParticles(new DustParticleOptions(0xFF8AC8, 1.4f), d.getX(), d.getY() + 1.4, d.getZ(), 15, 0.4, 0.6, 0.4, 0);
            zeg("vadsrijk.z5wezens.zs.ventiel_dicht", ChatFormatting.LIGHT_PURPLE, dichtAantal(), VENTIELEN);
            if (dichtAantal() >= VENTIELEN) {
                praat("zs.route_weg");
            }
        } else {
            p.sendOverlayMessage(Component.translatable("vadsrijk.z5wezens.gz.klep_draai", kliks[i], KLIKS).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // ---- the fight ---------------------------------------------------------------------------------------------------------------------

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (fase() == 2 && vermomCd <= 0 && !ontsnapping && level() instanceof ServerLevel) {
            vermom(sl);
            rustT = 20;
            laatsteAanval = "vermom";
            return;
        }
        if (ontsnapping && speldCd <= 0) {
            ServerPlayer v = l.get(getRandom().nextInt(l.size()));
            Vec3 a = position(), r = voetVan(v).subtract(a).multiply(1, 0, 1);
            r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
            rol(a, a.add(r.scale(20)), 1.6, 24, 14, 0.2, "hoedenspeld", ParticleTypes.CRIT, null);
            speel("spuug");
            speldCd = 50;
            rustT = 25;
            laatsteAanval = "speld";
            return;
        }
        if (sprongCd <= 0) {
            ServerPlayer v = iemandOpAfstand() ? afstandsDoel() : zDoel();
            if (v != null) {
                van = position();
                Vec3 r = voetVan(v).subtract(van).multiply(1, 0, 1);
                r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
                naar = voetVan(van.add(r.scale(Math.max(8, voetVan(v).distanceTo(van) + 3))));
                Telegraaf.lijn(sl, van, naar, 2, tg(18), ROOD);
                sprongT = tg(18) + 8;
                gesprongen.clear();
                sprongCd = 140;
                rustT = sprongT + 10;
                laatsteAanval = "sprong";
                return;
            }
        }
        if (rookCd <= 0) {
            ServerPlayer v = l.get(getRandom().nextInt(l.size()));
            Vec3 c = voetVan(v);
            Telegraaf.cirkel(sl, c, 4, tg(22), Telegraaf.GRIJS);
            later(tg(22), () -> {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.5, c.z, 60, 2, 0.6, 2, 0.02);
                for (ServerPlayer p : mechRond(c, 4, 0.15, "rookbom")) {
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
                }
                ServerPlayer ver = afstandsDoel();
                if (ver != null) {
                    Vec3 achter = voetVan(ver.position().subtract(ver.getLookAngle().multiply(1, 0, 1).normalize().scale(2)));
                    rook(sl, position(), 1);
                    teleportTo(achter.x, achter.y, achter.z);
                    rook(sl, position(), 1);
                }
            });
            rookCd = 220;
            rustT = 30;
            laatsteAanval = "rook";
            return;
        }
        rustT = 10;
    }

    @Override
    protected void bijt(ServerLevel sl, ServerPlayer d) {
        speel("aanval");
        zRaak(d, 1.0, "speldsteek");
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        sprongCd--;
        rookCd--;
        vermomCd--;
        speldCd--;
        if (ontmaskerdT > 0) {
            ontmaskerdT--;
        }
        if (vermomd()) {
            setDeltaMovement(Vec3.ZERO);
            if (tickCount % 25 == 0) {   // the silver glint at the tail tip: the tell
                Vec3 staart = position().subtract(voor().scale(0.7)).add(0, 0.5, 0);
                sl.sendParticles(ParticleTypes.END_ROD, staart.x, staart.y, staart.z, 2, 0.05, 0.05, 0.05, 0.005);
            }
            if (--vermomT <= 0) {
                ontmasker(sl, false);
            }
            return;
        }
        if (sprongT >= 0) {
            sprongT--;
            if (sprongT < 8) {
                Vec3 v = van.lerp(naar, 1 - sprongT / 8.0);
                setPos(v.x, v.y, v.z);
                setDeltaMovement(Vec3.ZERO);
                for (ServerPlayer p : l) {
                    if (p.distanceToSqr(this) < 2.2 * 2.2 && gesprongen.add(p.getUUID())) {
                        mech(p, 0.3, "speldsprong");
                    }
                }
            }
        }
        if (ontsnapping && dichtAantal() < VENTIELEN && tickCount % 60 == 0) {
            for (ServerPlayer p : l) {
                mech(p, 0.06, "grauwgas");
            }
            for (int i = 0; i < VENTIELEN; i++) {
                if (!dicht[i]) {
                    Vec3 k = ventielPlek(i);
                    sl.sendParticles(new DustParticleOptions(0x7A7A86, 2.2f), k.x, k.y + 1.5, k.z, 30, 0.6, 1, 0.6, 0.02);
                }
            }
        }
    }

    private Vec3 voor() {
        double a = Math.toRadians(getYRot());
        return new Vec3(-Math.sin(a), 0, Math.cos(a));
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl) {
            ruimLokpoppen(sl);
            for (UUID u : ventielen) {
                if (sl.getEntity(u) != null) {
                    sl.getEntity(u).discard();
                }
            }
        }
        super.remove(reason);
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        if (!was && level() instanceof ServerLevel sl) {
            ruimLokpoppen(sl);
            entityData.set(VERMOMD, false);
        }
        super.verslagen(laatste);
        if (!was && geplofd()) {
            praat("zs.weg");
        }
    }

    @Override
    protected void naVerslagen(ServerPlayer p) {
        Z5Bazen.spionVerslagen(p);
    }
}
