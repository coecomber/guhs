package nl.juiced.guhs.feature.vadsrijk.zone3.bazen.hart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Vast;

/**
 * De Wortelwurger (Hart van de Boom boss 1, lv 49, Natuur): a knot of grey roots around a rotten core.
 * <ul>
 *   <li>The heartbeat: every 4 s the hall goes dark for 1.5 s; you see only your light and the pink veins. Only during the beat
 *       are his 4 feeding roots open (they glow pink): cut them. Each living root gives him +20 % damage reduction and heals him.</li>
 *   <li>Root grabs: red strips from the wall to a player; whoever is caught hangs in a root (teammates sneak next to you, solo
 *       click/shift yourself loose; else 30 % when it lets go).</li>
 *   <li>A slam around himself, spore lobs at whoever stands at range. Fase 3 (33 %): the heart beats faster. Vahoeg: cut roots grow
 *       back once after 30 s.</li>
 * </ul>
 */
public class Wortelwurger extends Z3Baas {
    private static final EntityDataAccessor<Boolean> KLOP = SynchedEntityData.defineId(Wortelwurger.class, EntityDataSerializers.BOOLEAN);
    private final List<Z3Doel> wortels = new ArrayList<>();
    private final Map<Integer, Integer> teruggroei = new HashMap<>();
    private int klopT = 80, klopDuur, grijpCd = 80, slamCd = 120, sporeCd = 60;
    /** Gametests: roots cut, players grabbed. */
    public int gekapt, gegrepen;

    public Wortelwurger(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(KLOP, false);
    }

    public boolean klopt() {
        return entityData.get(KLOP);
    }

    @Override
    public String baasId() {
        return "wortelwurger";
    }

    @Override
    public String kerker() {
        return "hart";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"grijp", "klop"};
    }

    @Override
    protected double hp(int level) {
        return 7600 + 300 * level;
    }

    @Override
    protected double klap(int level) {
        return 20 + 2.3 * level;
    }

    @Override
    public int xp() {
        return nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Xp.baas(Math.min(50, mobLevel()));
    }

    @Override
    public Element standaardElement() {
        return Element.NATUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.66, 0.33};
    }

    @Override
    protected double jaagSnelheid() {
        return 0.8;
    }

    public List<Z3Doel> wortels() {
        return wortels;
    }

    public int levend() {
        return (int) wortels.stream().filter(w -> !w.isRemoved() && w.vads() > 0.01).count();
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        return 1 - 0.2 * levend();
    }

    @Override
    protected void opFase(int fase) {
        kaart("vadsrijk.z3bazen.ww.fase" + fase, fase == 3 ? ROOD : PAARS);
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (grijpCd <= 0) {
            Vec3 m = zMidden();
            int n = Math.min(l.size(), 1 + (fase() >= 2 ? 1 : 0) + (l.size() > 2 ? 1 : 0));
            for (int i = 0; i < n; i++) {
                ServerPlayer p = i == 0 && iemandOpAfstand() ? afstandsDoel() : l.get((i + getRandom().nextInt(l.size())) % l.size());
                if (p == null) {
                    continue;
                }
                Vec3 doel = voetVan(p);
                Vec3 r = doel.subtract(m).multiply(1, 0, 1);
                r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
                Vec3 muur = m.add(r.scale(zStraal() - 0.5));
                Vec3 eind = doel.subtract(r.scale(3));
                Telegraaf.lijn(sl, muur, eind, 2.4, tg(30), ROOD);
                nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.windup(this, tg(30));   // T-VFX: wind-up animation (held until the hit / auto-release)
                later(tg(30), () -> {
                    for (ServerPlayer q : strijders()) {
                        if (inStrook(q.position(), muur, eind, 1.3) && !Z3Vast.vast(q)) {
                            gegrepen++;
                            Vec3 plek = q.position();
                            Z3Vast.pak(this, q, "wortel", () -> plek, 8, 100, 0.04, () -> mech(q, 0.3, "wurg"));
                            sl.sendParticles(new DustParticleOptions(0x6A6A7A, 2f), q.getX(), q.getY() + 1, q.getZ(), 30, 0.5, 0.8, 0.5, 0);
                        }
                    }
                    sl.playSound(null, muur.x, muur.y, muur.z, SoundEvents.ROOTS_BREAK, SoundSource.HOSTILE, 2f, 0.5f);
                });
            }
            speel("grijp");
            zeg("vadsrijk.z3bazen.ww.grijp", ChatFormatting.RED);
            grijpCd = 160;
            rustT = 50;
            laatsteAanval = "grijp";
            return;
        }
        if (slamCd <= 0 && zDoel() != null && zDoel().distanceTo(this) < 7) {
            inslag(position(), 5, 26, 0.35, "slam", Status.PLAKKERIG, ParticleTypes.CAMPFIRE_COSY_SMOKE);
            slamCd = 140;
            rustT = 40;
            laatsteAanval = "slam";
            return;
        }
        if (sporeCd <= 0) {
            ServerPlayer v = afstandsDoel();
            if (v != null) {
                inslag(voetVan(v), 3, 30, 0.25, "sporen", Status.GESPOREN, new DustParticleOptions(0x8A9AB0, 2f));
                sporeCd = 90;
                rustT = 20;
                laatsteAanval = "sporen";
                return;
            }
        }
        rustT = 10;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        grijpCd--;
        slamCd--;
        sporeCd--;
        if (wortels.isEmpty()) {
            for (int i = 0; i < Z3BazenPlekken.WURGER_WORTELS.length; i++) {
                Z3Doel w = Z3BazenRegistry.VOEDINGSWORTEL.get().create(sl, EntitySpawnReason.EVENT);
                if (w != null) {
                    w.koppel(this, i, hp(Math.min(50, mobLevel())) * 0.05, punt(Z3BazenPlekken.WURGER_WORTELS[i]));
                    w.zetRaakbaar(false);
                    sl.addFreshEntity(w);
                    wortels.add(w);
                }
            }
        }
        // the heartbeat
        if (--klopT <= 0) {
            klopT = fase() >= 3 ? 60 : 80;
            klopDuur = 30;
            entityData.set(KLOP, true);
            speel("klop");
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 4f, 0.6f);
            for (ServerPlayer p : l) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 34, 0, false, false));
            }
            for (Z3Doel w : wortels) {
                if (w.vads() > 0.01) {
                    w.zetRaakbaar(true);
                    w.speel("klop");
                }
            }
        }
        if (klopDuur > 0 && --klopDuur == 0) {
            entityData.set(KLOP, false);
            wortels.forEach(w -> w.zetRaakbaar(false));
        }
        if (klopDuur > 0 && tickCount % 3 == 0) {
            for (Z3Doel w : wortels) {
                if (w.vads() > 0.01) {
                    sl.sendParticles(new DustParticleOptions(ROZE, 1.8f), w.getX(), w.getY() + 1.2, w.getZ(), 6, 0.4, 0.8, 0.4, 0);
                }
            }
        }
        // living roots feed him
        if (tickCount % 20 == 0 && levend() > 0) {
            heal(maxVads() * 0.003 * levend());
        }
        // Vahoeg: roots grow back once
        teruggroei.replaceAll((i, t) -> t - 1);
        for (Map.Entry<Integer, Integer> e : new ArrayList<>(teruggroei.entrySet())) {
            if (e.getValue() == 0 && e.getKey() < wortels.size()) {
                Z3Doel w = wortels.get(e.getKey());
                w.zetVads(w.maxVads());
                w.setInvisible(false);
                zeg("vadsrijk.z3bazen.ww.groei", ChatFormatting.GOLD);
            }
        }
    }

    @Override
    public void doelKapot(Z3Doel w, @Nullable LivingEntity bron) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        gekapt++;
        w.setInvisible(true);
        pluis(sl, w.position().add(0, 1, 0), 1.2, 2);
        zeg("vadsrijk.z3bazen.ww.gekapt", ChatFormatting.GREEN, 4 - levend());
        if (vahoeg() && !teruggroei.containsKey(w.nummer)) {
            teruggroei.put(w.nummer, 600);
        }
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd() && level() instanceof ServerLevel sl) {
            entityData.set(KLOP, false);
            Z3Vast.losAlles(getUUID(), sl.getServer());
            wortels.forEach(w -> w.discard());
            zeg("vadsrijk.z3bazen.ww.verslagen", ChatFormatting.GOLD);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        wortels.forEach(w -> w.discard());
        super.remove(reason);
    }
}
