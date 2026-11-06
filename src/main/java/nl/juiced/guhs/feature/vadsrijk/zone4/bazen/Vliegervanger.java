package nl.juiced.guhs.feature.vadsrijk.zone4.bazen;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4Vfx;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.mob.Z4Wezen;

/**
 * Kootje de Vliegervanger (lv 56, end boss of the small dungeon "Het Wrak van de Vliegervanger"): a Mika who steals the
 * Knuffeltij plushies and flies off with them under his war kite (ZONE4_PLAN Z4-3).
 * <ul>
 *   <li><b>Fase 1</b> (100-60 %): <i>Knuffelnet</i> = a red strip (0.9 s), the net: 15 % + Plakkerig; <i>Kaasbom</i> = 2 red
 *       circles (1.2 s): 14 %. At 80 % he whistles 2 Vliegerknechten.</li>
 *   <li><b>Fase 2</b> (60-25 %): <i>Opstijgen!</i> He hangs under his war kite 9 blocks above the deck: IMMUNE. Three winches
 *       (lieren) on the deck hold the kite: right-click a winch 3x to wind it in (purple marker). While he hangs: plushie sacks
 *       (red circles, 12 %) and a wind gust (purple strip, a push). All 3 winches in = the kite crashes: 6 s stunned, +50 %.</li>
 *   <li><b>Fase 3</b> (25-0 %): <i>Knuffelregen</i>: his sack tears, stolen plushies fall: a yellow circle (3 s) where one lands:
 *       enough players in it (2, solo 1) = caught (max 3, counted for the story); too few = 15 % on everyone.</li>
 * </ul>
 * Beaten he flees in a smoke puff (a Mika, not cheered up). Moment "wrak_knuffels:&lt;n&gt;" for the story.
 */
public class Vliegervanger extends Z4Baas {
    private static final EntityDataAccessor<Boolean> VLIEGT = SynchedEntityData.defineId(Vliegervanger.class, EntityDataSerializers.BOOLEAN);

    private enum Actie {
        GEEN, NET, BOM, VLIEG, VAL, REGEN
    }

    private Actie actie = Actie.GEEN;
    private int actieT, netCd = 40, bomCd = 90, zakCd = 60, windCd = 140, regenCd = 60, verdoofdT;
    private boolean hulpGeroepen, neergehaald;
    private int gered, geprobeerd;
    @Nullable
    private Vec3 netNaar, windNaar;
    private final List<UUID> lieren = new ArrayList<>();
    private final Map<UUID, Integer> slagen = new HashMap<>();
    /** Gametests: winches wound in this fight. */
    public int lierenBinnen;

    public Vliegervanger(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VLIEGT, false);
    }

    /** Client: he hangs under his war kite (the kite bone is drawn). */
    public boolean vliegt() {
        return entityData.get(VLIEGT);
    }

    public int knuffelsGered() {
        return gered;
    }

    @Override
    public String baasId() {
        return "vliegervanger";
    }

    @Override
    protected boolean isMika() {
        return true;
    }

    @Override
    public boolean eindbaas() {
        return true;
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"net", "gooi", "opstijg", "val", "fluit"};
    }

    @Override
    protected double hp(int level) {
        return 7600 + 280 * level;
    }

    @Override
    protected double klap(int level) {
        return 20 + 2.3 * level;
    }

    @Override
    public int xp() {
        return nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Xp.baas(mobLevel());
    }

    @Override
    public Element standaardElement() {
        return Element.VUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.6, 0.25};
    }

    @Override
    public boolean isNoGravity() {
        return vliegt() || super.isNoGravity();
    }

    @Override
    protected boolean bezig() {
        return actie != Actie.GEEN || verdoofdT > 0;
    }

    @Override
    protected boolean immuun() {
        return actie == Actie.VLIEG;
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z4bazen.vliegervanger.immuun";
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        return verdoofdT > 0 && neergehaald ? 1.5 : 1;
    }

    private void start(Actie a, int t) {
        actie = a;
        actieT = t;
        getNavigation().stop();
        rustT = t + 20;
    }

    // ---- phases -------------------------------------------------------------------------------------------------------------

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (fase == 2) {
            kaart("vadsrijk.z4bazen.vliegervanger.fase2", PAARS);
            spreek("vadsrijk.z4bazen.vliegervanger.zeg.op");
            opstijgen(sl);
        } else if (fase == 3) {
            if (actie == Actie.VLIEG) {
                neerhalen(sl);
            }
            kaart("vadsrijk.z4bazen.vliegervanger.fase3", GEEL);
            spreek("vadsrijk.z4bazen.vliegervanger.zeg.regen");
        }
    }

    private void opstijgen(ServerLevel sl) {
        start(Actie.VLIEG, 99999);
        entityData.set(VLIEGT, true);
        setNoGravity(true);
        speel("opstijg");
        sl.playSound(null, blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 2f, 0.7f);
        lieren.clear();
        slagen.clear();
        for (int i = 0; i < 3; i++) {
            if (Z4BazenRegistry.LIER.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Z3Doel d) {
                net.minecraft.core.BlockPos o = Z4BazenPlekken.lier(i).subtract(Z4BazenPlekken.WRAK_CENTRUM);
                d.koppel(this, i, 1000, zMidden().add(o.getX(), o.getY(), o.getZ()));
                d.zetRaakbaar(false);   // a winch is wound by hand (right-click), not hit
                sl.addFreshEntity(d);
                lieren.add(d.getUUID());
            }
        }
        zeg("vadsrijk.z4bazen.vliegervanger.lieren", ChatFormatting.LIGHT_PURPLE);
    }

    private void neerhalen(ServerLevel sl) {
        entityData.set(VLIEGT, false);
        setNoGravity(false);
        neergehaald = true;
        for (UUID u : lieren) {
            if (sl.getEntity(u) instanceof Z3Doel d) {
                d.discard();
            }
        }
        lieren.clear();
        actie = Actie.GEEN;
        verdoofdT = 120;
        speel("val");
        zSpring(zMidden(), 16, 1);
        zeg("vadsrijk.z4bazen.vliegervanger.neer", ChatFormatting.GOLD);
        sl.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 3f, 0.6f);
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 40, 2, 1, 2, 0.05);
        Statussen.geef(this, Status.VERDOOFD, 120, 1, null);
        rustT = 130;
    }

    @Override
    public void doelGebruikt(Z3Doel d, ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl) || actie != Actie.VLIEG || !lieren.contains(d.getUUID())) {
            return;
        }
        int n = slagen.merge(d.getUUID(), 1, Integer::sum);
        d.zetStand(Math.min(3, n));
        d.speel("pomp");
        sl.playSound(null, d.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1f, 0.8f + n * 0.15f);
        if (n == 3) {
            lierenBinnen++;
            Z4Vfx.cirkel(sl, d.position(), 1.2, 20, ROZE);
            long klaar = slagen.values().stream().filter(v -> v >= 3).count();
            zeg("vadsrijk.z4bazen.vliegervanger.lier", ChatFormatting.LIGHT_PURPLE, klaar, lieren.size());
            if (klaar >= lieren.size()) {
                neerhalen(sl);
            }
        } else if (n < 3) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("vadsrijk.z4bazen.vliegervanger.draai", n, 3)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // ---- the moves -----------------------------------------------------------------------------------------------------------

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (l.isEmpty() || actie != Actie.GEEN || verdoofdT > 0) {
            return;
        }
        ServerPlayer d = iemandOpAfstand() ? afstandsDoel() : zDoel();
        if (d == null) {
            d = l.get(getRandom().nextInt(l.size()));
        }
        if (fase() >= 3 && regenCd <= 0 && geprobeerd < 6) {
            ServerPlayer p = l.get(getRandom().nextInt(l.size()));
            Vec3 v = voetVan(p.position().add(getRandom().nextInt(5) - 2, 0, getRandom().nextInt(5) - 2));
            start(Actie.REGEN, tg(60));
            Z4Vfx.cast(this, "knuffelregen", tg(60));
            regenCd = 180;
            geprobeerd++;
            zeg("vadsrijk.z4bazen.vliegervanger.knuffel", ChatFormatting.YELLOW);
            sl.sendParticles(new DustParticleOptions(0xB4B0BC, 2.0f), v.x, v.y + 8, v.z, 20, 0.4, 0.4, 0.4, 0);
            stapel(v, 3, 60, 0.3, 0.15, 2, "knuffelregen", ok -> {
                actie = Actie.GEEN;
                if (ok && gered < 3) {
                    gered++;
                    Z4Vfx.feest(sl, v.add(0, 1, 0), 1.2);
                    zeg("vadsrijk.z4bazen.vliegervanger.gevangen", ChatFormatting.LIGHT_PURPLE, gered, 3);
                }
            });
            laatsteAanval = "knuffelregen";
            return;
        }
        if (bomCd <= 0 && getRandom().nextBoolean()) {
            for (int i = 0; i < Math.min(2, l.size() + 1); i++) {
                ServerPlayer p = l.get(getRandom().nextInt(l.size()));
                inslag(voetVan(p), 2.5, 24, 0.14, "kaasbom", null, new DustParticleOptions(0xFFD86A, 1.8f));
            }
            start(Actie.BOM, tg(24));
            bomCd = 140;
            speel("gooi");
            laatsteAanval = "kaasbom";
            return;
        }
        if (netCd <= 0) {
            Vec3 van = position();
            Vec3 r = d.position().subtract(van).multiply(1, 0, 1);
            r = r.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : r.normalize();
            netNaar = van.add(r.scale(Math.max(9, d.position().distanceTo(van) + 3)));
            Z4Vfx.lijn(sl, van, netNaar, 2.2, tg(18), ROOD);
            start(Actie.NET, tg(18));
            Z4Vfx.windup(this, tg(18));   // T-VFX one-liner (reflection)
            netCd = fase() >= 3 ? 70 : 100;
            speel("net");
            laatsteAanval = "knuffelnet";
            return;
        }
        rustT = 10;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        netCd--;
        bomCd--;
        regenCd--;
        zakCd--;
        windCd--;
        if (verdoofdT > 0) {
            verdoofdT--;
            if (tickCount % 10 == 0) {
                sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() + 0.3, getZ(), 4, 0.3, 0.1, 0.3, 0.02);
            }
        }
        if (!hulpGeroepen && vads() < maxVads() * 0.8) {
            hulpGeroepen = true;
            roepKnechten(sl, adds(2));
        }
        if (actie == Actie.VLIEG) {
            vliegTick(sl, l);
            return;
        }
        if (actie == Actie.GEEN || --actieT > 0) {
            return;
        }
        if (actie == Actie.NET && netNaar != null) {
            Vec3 van = position();
            Z4Vfx.wind(sl, van, netNaar);
            sl.playSound(null, blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.HOSTILE, 2f, 0.6f);
            for (ServerPlayer p : l) {
                if (Zeeslurf.inBaan(p.position(), van, netNaar, 2.2) && Math.abs(p.getY() - getY()) < 3) {
                    mech(p, 0.15, "knuffelnet");
                    Statussen.geef(p, Status.PLAKKERIG, 60, 1, this);
                }
            }
        }
        if (actie != Actie.REGEN) {
            actie = Actie.GEEN;
        }
    }

    /** Fase 2: he hangs under the kite above the deck; sacks and gusts; the winches bring him down. */
    private void vliegTick(ServerLevel sl, List<ServerPlayer> l) {
        Vec3 boven = zMidden().add(Math.cos(tickCount * 0.02) * 4, 9, Math.sin(tickCount * 0.02) * 4);
        Vec3 v = boven.subtract(position());
        setDeltaMovement(v.lengthSqr() > 0.04 ? v.normalize().scale(Math.min(0.3, v.length() * 0.2)) : Vec3.ZERO);
        if (tickCount % 4 == 0) {
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 3, getZ(), 1, 1.5, 0.3, 1.5, 0.01);
            for (UUID u : lieren) {   // a purple rope from every unwound winch to the kite
                if (sl.getEntity(u) instanceof Z3Doel d && slagen.getOrDefault(u, 0) < 3) {
                    Vec3 a = d.position().add(0, 1, 0), b = position().add(0, 3, 0);
                    for (double s = 0; s <= 1; s += 0.1) {
                        Vec3 p = a.lerp(b, s);
                        sl.sendParticles(new DustParticleOptions(PAARS, 0.8f), p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
        if (l.isEmpty()) {
            return;
        }
        if (zakCd <= 0) {
            zakCd = 60;
            ServerPlayer p = l.get(getRandom().nextInt(l.size()));
            inslag(voetVan(p), 2.5, 26, 0.12, "knuffelzak", null, new DustParticleOptions(0xB4B0BC, 2f));
            speel("gooi");
        }
        if (windCd <= 0) {
            windCd = 160;
            ServerPlayer p = l.get(getRandom().nextInt(l.size()));
            Vec3 van = new Vec3(getX(), p.getY(), getZ());
            Vec3 r = p.position().subtract(van).multiply(1, 0, 1);
            r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
            windNaar = van.add(r.scale(14));
            Vec3 naar = windNaar, rr = r;
            Z4Vfx.lijn(sl, van, naar, 3, tg(20), PAARS);
            Z4Vfx.cast(this, "windvlaag", tg(20));
            later(tg(20), () -> {
                Z4Vfx.wind(sl, van, naar);
                for (ServerPlayer q : strijders()) {
                    if (Zeeslurf.inBaan(q.position(), van, naar, 3)) {
                        q.push(rr.x * 1.2, 0.3, rr.z * 1.2);
                        q.hurtMarked = true;
                        mech(q, 0.06, "windvlaag");
                    }
                }
            });
        }
    }

    private void roepKnechten(ServerLevel sl, int n) {
        spreek("vadsrijk.z4bazen.vliegervanger.zeg.fluit");
        speel("fluit");
        for (int i = 0; i < n; i++) {
            if (Z4WezensRegistry.VLIEGERKNECHT.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Z4Wezen w) {
                double a = getRandom().nextDouble() * Math.PI * 2;
                w.snapTo(getX() + Math.cos(a) * 3, getY() + 0.2, getZ() + Math.sin(a) * 3, getRandom().nextFloat() * 360, 0);
                w.stelIn(Math.max(50, mobLevel() - 1), null, false, EnumSet.noneOf(Affix.class));
                sl.addFreshEntity(w);
                ServerPlayer d = zDoel();
                if (d != null && !d.isCreative()) {
                    w.setTarget(d);
                }
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl) {
            for (UUID u : lieren) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    d.discard();
                }
            }
        }
        super.remove(reason);
    }

    @Override
    protected void opPlof() {
        super.opPlof();   // the Mika smoke
        spreek("vadsrijk.z4bazen.vliegervanger.zeg.weg");
        entityData.set(VLIEGT, false);
    }
}
