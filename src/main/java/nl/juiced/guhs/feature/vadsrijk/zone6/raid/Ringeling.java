package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
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
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx;
import nl.juiced.guhs.feature.vadsrijk.vfx.Deeltje;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vfx;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vorm;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;

/**
 * Floor 4, the roof: Hertogin Ringeling, de Ringvinger (a Mika duchess with the Keizer's rings on all ten toes).
 * <ul>
 *   <li><b>Fase 1 "Tien Tenen"</b> (100-60 %): she kicks rings at players: a PINK catch circle in a red blast circle. Whoever stands
 *       in the pink circle and JUMPS in the last half second catches the ring: it flies back (3 % of her Vads, one ring less,
 *       its effect gone). Not caught = a blast with the ring's effect (Vuur burns, IJs freezes, Lichtdief steals energy, Duw
 *       pushes, Wissel swaps two players). Teentrap: a stomp round her (get out). 1 ring per kick, 2 from 5 players, 3 from 9.</li>
 *   <li><b>Fase 2 "De Lege Lens"</b> (60-25 %): the Lens sucks the light away: dark for everyone far from a burning beacon, a pulse
 *       every 6 s. m() unlit sterrenbakens stand round the roof; star shards fall: right-click a shard to carry it, then right-click
 *       an unlit beacon. All lit = the light is back: she is stunned 10 s and takes x1.5 for 15 s. She can't go below 25 % before.</li>
 *   <li><b>Fase 3 "Ringdans"</b> (25-0 %): she rises; giant rings sweep the roof: LOW ring = jump (be in the air), HIGH ring = duck
 *       (sneak). After three rings the Lens cracks: she falls, stunned 12 s, x2 damage. Then again.</li>
 *   <li><b>The end</b> (user decision): she is CHEERED UP, not chased away: the starlight comes back, the stars above the desert
 *       relight, and she turns against the Keizer (Z6Raid.klaar -> Z6RaidHaken -> u-z6verhaal).</li>
 * </ul>
 */
public class Ringeling extends Z6RaidBaas {
    public static final String[] RINGEN = {"vuur", "ijs", "lichtdief", "duw", "wissel"};
    private static final EntityDataAccessor<Integer> RINGEN_OVER = SynchedEntityData.defineId(Ringeling.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ZWEEFT = SynchedEntityData.defineId(Ringeling.class, EntityDataSerializers.BOOLEAN);

    /** A kicked ring in flight. */
    private record Schop(Vec3 plek, int soort, int duur, int[] t, Set<UUID> vangers) {
    }

    private final List<Schop> schoppen = new ArrayList<>();
    private final List<UUID> bakens = new ArrayList<>(), scherven = new ArrayList<>();
    private final Map<UUID, UUID> drager = new HashMap<>();   // shard -> player
    private int schopCd = 80, trapCd = 200, scherfCd, pulsCd, lichtT, ringCd = 100, ringen3, valT;
    private boolean donker;
    @Nullable
    private String ringNu;
    private int ringT = -1;
    /** Gametests. */
    public int gevangen, ontploft, bakensAan, gevallen;

    public Ringeling(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(RINGEN_OVER, 10);
        b.define(ZWEEFT, false);
    }

    public int ringenOver() {
        return entityData.get(RINGEN_OVER);
    }

    public boolean zweeft() {
        return entityData.get(ZWEEFT);
    }

    @Override
    public String baasId() {
        return "ringeling";
    }

    @Override
    public int verdieping() {
        return 4;
    }

    @Override
    protected double basisHp() {
        return 40000;
    }

    @Override
    protected double basisKlap() {
        return 200;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.6, 0.25};
    }

    @Override
    protected int enrageTicks() {
        return 20 * 60 * 10;
    }

    @Override
    protected boolean isMika() {
        return true;
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"schop", "trap", "zweef", "val", "huil", "blij"};
    }

    @Override
    public boolean isNoGravity() {
        return zweeft() || super.isNoGravity();
    }

    @Override
    protected double vadsVloer() {
        return fase() == 2 && bakensAan < bakens.size() ? 0.2505 : 0;
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z6raid.ringeling.lens";
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        if (valT > 0) {
            return 2.0;
        }
        return lichtT > 0 ? 1.5 : 1.0;
    }

    @Override
    protected boolean bezig() {
        return super.bezig() || zweeft();
    }

    public boolean donker() {
        return donker;
    }

    public List<Z3Doel> bakens() {
        return doelen(bakens);
    }

    public List<Z3Doel> scherven() {
        return doelen(scherven);
    }

    private List<Z3Doel> doelen(List<UUID> ids) {
        List<Z3Doel> l = new ArrayList<>();
        if (level() instanceof ServerLevel sl) {
            for (UUID u : ids) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    l.add(d);
                }
            }
        }
        return l;
    }

    // ---- phases ----------------------------------------------------------------------------------------------------------------

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (fase == 2) {
            lens(sl);
        } else if (fase == 3) {
            einddonker(sl);
            kaartAlle("vadsrijk.z6raid.ringeling.fase3", GEEL);
            zegGroep("vadsrijk.z6raid.ringeling.zeg.dans");
            stijg(sl);
        }
    }

    /** Fase 2: dark, beacons round the roof, shards fall. */
    public void lens(ServerLevel sl) {
        donker = true;
        bakensAan = 0;
        kaartAlle("vadsrijk.z6raid.ringeling.fase2", PAARS);
        zegGroep("vadsrijk.z6raid.ringeling.zeg.lens");
        int m = m();
        for (int i = 0; i < m; i++) {
            if (Z6RaidRegistry.STERRENBAKEN.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Z3Doel d) {
                double a = i * Math.PI * 2 / m + Math.PI / 4;
                Vec3 v = zMidden().add(Math.cos(a) * 17, 0, Math.sin(a) * 17);
                d.koppel(this, i, 1000, v);
                d.zetRaakbaar(false);
                d.zetStand(0);
                sl.addFreshEntity(d);
                bakens.add(d.getUUID());
            }
        }
        scherfCd = 20;
        pulsCd = 120;
    }

    private void einddonker(ServerLevel sl) {
        donker = false;
        for (Z3Doel d : scherven()) {
            d.discard();
        }
        scherven.clear();
        drager.clear();
        for (ServerPlayer p : strijders()) {
            p.removeEffect(MobEffects.DARKNESS);
        }
    }

    private void scherf(ServerLevel sl) {
        if (scherven.size() >= m() + 1) {
            return;
        }
        if (Z6RaidRegistry.STERRENSCHERF.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Z3Doel d) {
            double a = getRandom().nextDouble() * Math.PI * 2, r = 4 + getRandom().nextDouble() * 12;
            Vec3 v = zMidden().add(Math.cos(a) * r, 0.3, Math.sin(a) * r);
            d.koppel(this, scherven.size(), 1000, v);
            d.zetRaakbaar(false);
            sl.addFreshEntity(d);
            scherven.add(d.getUUID());
            Vfx.straal(sl, zMidden().add(0, 12, 0), v.add(0, 0.5, 0), 0.3, STERWIT, 10);
            Vfx.deeltjes(sl, Deeltje.STER, v.add(0, 0.5, 0), 10, STERWIT, 0.4, 0.05);
        }
    }

    /** Right-click: a shard = pick it up; an unlit beacon while carrying = light it. */
    @Override
    public void doelGebruikt(Z3Doel d, ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (scherven.contains(d.getUUID())) {
            if (drager.containsValue(p.getUUID())) {
                meld(p, "vadsrijk.z6raid.ringeling.al_scherf", ChatFormatting.GRAY);
                return;
            }
            drager.put(d.getUUID(), p.getUUID());
            meld(p, "vadsrijk.z6raid.ringeling.scherf_op", ChatFormatting.AQUA);
            sl.playSound(null, d.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1f, 1.6f);
        } else if (bakens.contains(d.getUUID()) && d.stand() == 0) {
            UUID scherf = null;
            for (Map.Entry<UUID, UUID> e : drager.entrySet()) {
                if (e.getValue().equals(p.getUUID())) {
                    scherf = e.getKey();
                }
            }
            if (scherf == null) {
                meld(p, "vadsrijk.z6raid.ringeling.geen_scherf", ChatFormatting.GRAY);
                return;
            }
            drager.remove(scherf);
            scherven.remove(scherf);
            if (sl.getEntity(scherf) instanceof Z3Doel s) {
                s.discard();
            }
            steekAan(sl, d);
        }
    }

    public void steekAan(ServerLevel sl, Z3Doel baken) {
        baken.zetStand(1);
        bakensAan++;
        Vfx.zuil(sl, baken.position(), 10, STERGOUD, 9999);
        Vfx.deeltjes(sl, Deeltje.STER, baken.position().add(0, 1.5, 0), 30, STERGOUD, 0.8, 0.1);
        sl.playSound(null, baken.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2f, 1.3f);
        zeg("vadsrijk.z6raid.ringeling.baken", ChatFormatting.GOLD, bakensAan, bakens.size());
        if (bakensAan >= bakens.size()) {
            licht(sl);
        }
    }

    private void licht(ServerLevel sl) {
        einddonker(sl);
        lichtT = 20 * 15;
        Statussen.geef(this, Status.VERDOOFD, 200, 1, null);
        anim("huil");
        Vfx.flits(sl, zMidden().add(0, 8, 0), 12, 0xFFF4C0);
        kaartAlle("vadsrijk.z6raid.ringeling.licht", GEEL);
        zegGroep("vadsrijk.z6raid.ringeling.zeg.licht");
    }

    private void lensTick(ServerLevel sl, List<ServerPlayer> l) {
        // carried shards float above their carrier
        for (Map.Entry<UUID, UUID> e : new ArrayList<>(drager.entrySet())) {
            if (sl.getEntity(e.getKey()) instanceof Z3Doel s && sl.getEntity(e.getValue()) instanceof ServerPlayer p && p.isAlive()) {
                s.zetPlek(p.position().add(0, 2.3, 0));
                for (Z3Doel b : bakens()) {   // walking into an unlit beacon with a shard also lights it
                    if (b.stand() == 0 && p.distanceToSqr(b) < 2.5 * 2.5) {
                        drager.remove(e.getKey());
                        scherven.remove(e.getKey());
                        s.discard();
                        steekAan(sl, b);
                        break;
                    }
                }
            } else {
                drager.remove(e.getKey());
            }
        }
        if (!donker) {
            return;
        }
        if (--scherfCd <= 0) {
            scherfCd = 20 * 6;
            scherf(sl);
        }
        if (tickCount % 40 == 0) {
            for (ServerPlayer p : l) {
                if (!bijLicht(p)) {
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false));
                } else {
                    p.removeEffect(MobEffects.DARKNESS);
                }
            }
        }
        if (--pulsCd <= 0) {
            pulsCd = 120;
            Vfx.golf(sl, zMidden().add(0, 10, 0), 6, 0x6A3AA0, 14);
            for (ServerPlayer p : l) {
                if (!bijLicht(p)) {
                    mech(p, 0.08, "lege_lens");
                    Vfx.straal(sl, p.position().add(0, 1, 0), zMidden().add(0, 10, 0), 0.3, 0x8A4AD0, 10);
                }
            }
        }
    }

    public boolean bijLicht(ServerPlayer p) {
        for (Z3Doel b : bakens()) {
            if (b.stand() == 1 && p.distanceToSqr(b) < 5 * 5) {
                return true;
            }
        }
        return false;
    }

    // ---- fase 1: the rings --------------------------------------------------------------------------------------------------------

    public int perSchop() {
        return Math.max(1, (m() + 1) / 2);
    }

    public void schop(ServerLevel sl, List<ServerPlayer> l) {
        anim("schop");
        int k = Math.min(perSchop(), ringenOver());
        for (int i = 0; i < k; i++) {
            ServerPlayer p = l.get(getRandom().nextInt(l.size()));
            Vec3 v = voetVan(p);
            int soort = getRandom().nextInt(RINGEN.length);
            int duur = tg(40);
            int groep = Vfx.nieuweGroep();
            Vfx.decal(sl, Vorm.cirkel(3.6), v, 0, duur, ROOD, groep);
            Vfx.decal(sl, Vorm.cirkel(1.6), v, 0, duur, ROZE, groep);
            schoppen.add(new Schop(v, soort, duur, new int[1], new HashSet<>()));
            meld(p, "vadsrijk.z6raid.ringeling.ring_komt", ChatFormatting.LIGHT_PURPLE,
                    net.minecraft.network.chat.Component.translatable("vadsrijk.z6raid.ring." + RINGEN[soort]));
        }
        zegGroep("vadsrijk.z6raid.ringeling.zeg.schop");
    }

    private void schoppenTick(ServerLevel sl) {
        for (var it = schoppen.iterator(); it.hasNext(); ) {
            Schop s = it.next();
            int t = ++s.t()[0];
            Vec3 boog = position().add(0, getBbHeight(), 0).lerp(s.plek(), Math.min(1, t / (double) s.duur())).add(0, 3 * Math.sin(Math.PI * t / s.duur()), 0);
            if (t % 3 == 0) {
                Vfx.deeltjes(sl, Deeltje.GLOED, boog, 2, STERGOUD, 0.1, 0);
            }
            if (t >= s.duur() - 10) {   // the catch window: in the pink circle AND in the air (a jump)
                for (ServerPlayer p : strijders()) {
                    if (!p.onGround() && p.position().multiply(1, 0, 1).distanceTo(s.plek().multiply(1, 0, 1)) <= 1.9 && Math.abs(p.getY() - s.plek().y) < 3) {
                        s.vangers().add(p.getUUID());
                    }
                }
            }
            if (t < s.duur()) {
                continue;
            }
            it.remove();
            if (!s.vangers().isEmpty()) {
                gevangen++;
                entityData.set(RINGEN_OVER, Math.max(0, ringenOver() - 1));
                Vfx.straal(sl, s.plek().add(0, 1, 0), position().add(0, 1, 0), 0.6, STERGOUD, 10);
                Vfx.flits(sl, position().add(0, 1.2, 0), 2, STERGOUD);
                double n = maxVads() * 0.03;
                super.krijgSchade(n, null);
                sl.playSound(null, blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 1f, 1.8f);
                for (UUID u : s.vangers()) {
                    if (sl.getEntity(u) instanceof ServerPlayer p) {
                        meld(p, "vadsrijk.z6raid.ringeling.gevangen", ChatFormatting.GOLD);
                        bijdragen().schade(u, n);
                    }
                }
                if (ringenOver() == 0) {
                    kaartAlle("vadsrijk.z6raid.ringeling.geen_ringen", GEEL);
                }
                continue;
            }
            ontploft++;
            Vfx.golf(sl, s.plek(), 3.8, ROOD, 10);
            List<ServerPlayer> geraakt = mechRond(s.plek(), 3.6, 0.25, "ring_" + RINGEN[s.soort()]);
            for (ServerPlayer p : geraakt) {
                switch (s.soort()) {
                    case 0 -> Statussen.geef(p, Status.BRANDEND, 80, Math.max(0.4, mobLevel() * 0.5), this);
                    case 1 -> Statussen.geef(p, nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas.bevroren(), 60, 1, this);
                    case 2 -> Spelers.data(p).energie = Math.max(0, Spelers.data(p).energie - Spelers.maxEnergie(p) * 0.5);
                    case 3 -> duw(p, s.plek(), 1.6);
                    default -> {
                    }
                }
            }
            if (s.soort() == 4) {
                List<ServerPlayer> l = strijders();
                if (l.size() >= 2) {
                    ServerPlayer a = l.get(getRandom().nextInt(l.size())), b = l.get((l.indexOf(a) + 1) % l.size());
                    Vec3 pa = a.position(), pb = b.position();
                    a.teleportTo(pb.x, pb.y, pb.z);
                    b.teleportTo(pa.x, pa.y, pa.z);
                }
            }
        }
    }

    private void teentrap(ServerLevel sl) {
        int w = tg(35);
        Vec3 c = position();
        Telegraaf.cirkel(sl, c, 6.5, w, ROOD);
        BaasVfx.windup(this, w);
        anim("trap");
        later(w, () -> {
            Vfx.golf(sl, c, 7, STERGOUD, 10);
            for (ServerPlayer p : mechRond(c, 6.5, 0.3, "teentrap")) {
                duw(p, c, 1.2);
            }
        });
    }

    // ---- fase 3: the ring dance -------------------------------------------------------------------------------------------------

    private void stijg(ServerLevel sl) {
        entityData.set(ZWEEFT, true);
        setNoGravity(true);
        anim("zweef");
        ringCd = 60;
        ringen3 = 0;
    }

    private void dansTick(ServerLevel sl) {
        if (valT > 0) {
            if (--valT == 0) {
                stijg(sl);
            }
            return;
        }
        if (!zweeft()) {
            return;
        }
        Vec3 boven = zMidden().add(0, 5, 0);
        Vec3 v = boven.subtract(position());
        setDeltaMovement(v.lengthSqr() > 0.04 ? v.normalize().scale(Math.min(0.25, v.length() * 0.2)) : Vec3.ZERO);
        if (ringT >= 0) {
            if (++ringT >= tg(40)) {
                boolean laag = "laag".equals(ringNu);
                Vfx.golf(sl, zMidden().add(0, laag ? 0.4 : 2.0, 0), 22, STERGOUD, 14);
                for (ServerPlayer p : strijders()) {
                    boolean goed = laag ? !p.onGround() : p.isShiftKeyDown();
                    if (!goed) {
                        mech(p, 0.3, "reuzenring");
                    }
                }
                ringT = -1;
                ringen3++;
                if (ringen3 >= 3) {
                    val(sl);
                }
            }
            return;
        }
        if (--ringCd <= 0) {
            ringCd = 20 * 4;
            ringNu = getRandom().nextBoolean() ? "laag" : "hoog";
            ringT = 0;
            Vfx.cast(this, "vadsrijk.vfx.aanval.ring_" + ringNu, tg(40), false);
            kaartAlle("vadsrijk.z6raid.ringeling.ring_" + ringNu, "laag".equals(ringNu) ? GEEL : PAARS);
            Vfx.decal(sl, Vorm.cirkel(21), zMidden(), 0, tg(40), "laag".equals(ringNu) ? GEEL : PAARS, Vfx.nieuweGroep());
        }
    }

    /** The Lens cracks: she falls, stunned and x2. */
    public void val(ServerLevel sl) {
        gevallen++;
        ringen3 = 0;
        entityData.set(ZWEEFT, false);
        setNoGravity(false);
        valT = 20 * 12;
        Statussen.geef(this, Status.VERDOOFD, 240, 1, null);
        anim("val");
        Vfx.schud(sl, zMidden(), 30, 0.8f, 20);
        Vfx.deeltjes(sl, Deeltje.KRISTAL, zMidden().add(0, 10, 0), 50, 0xB080FF, 3, 0.2);
        sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3f, 0.5f);
        kaartAlle("vadsrijk.z6raid.ringeling.val", GEEL);
    }

    // ---- the rotation --------------------------------------------------------------------------------------------------------------

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        rustT = 20;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        schoppenTick(sl);
        lensTick(sl, l);
        if (lichtT > 0) {
            lichtT--;
        }
        if (fase() >= 3) {
            dansTick(sl);
        }
        if (gietTick() || l.isEmpty() || inIntro() || zweeft() || valT > 0) {
            return;
        }
        if (--schopCd <= 0 && ringenOver() > 0) {
            schopCd = fase() >= 2 ? 20 * 9 : 20 * 7;
            schop(sl, l);
        } else if (--trapCd <= 0) {
            trapCd = 20 * 14;
            teentrap(sl);
        }
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        if (!was && level() instanceof ServerLevel sl) {
            einddonker(sl);
            entityData.set(ZWEEFT, false);
            setNoGravity(false);
            anim("blij");
            zegGroep("vadsrijk.z6raid.ringeling.zeg.einde1");
            Vfx.zuil(sl, position(), 20, 0xFFF4C0, 120);
        }
        super.verslagen(laatste);
        if (!was && geplofd()) {
            zegGroep("vadsrijk.z6raid.ringeling.zeg.einde2");
            zegGroep("vadsrijk.z6raid.ringeling.zeg.einde3");
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl) {
            for (Z3Doel d : bakens()) {
                d.discard();
            }
            for (Z3Doel d : scherven()) {
                d.discard();
            }
            for (ServerPlayer p : sl.players()) {
                if (donker && p.distanceToSqr(this) < 40 * 40) {
                    p.removeEffect(MobEffects.DARKNESS);
                }
            }
        }
        super.remove(reason);
    }
}
