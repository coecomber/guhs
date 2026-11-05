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
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensRegistry;

/**
 * Opzichter Gorgonzola (De Buikgangen, the Ketelkamer; lv 59, Vuur): a fat Kootje of the Ringvinger in a boiler suit with a huge
 * wrench, who turns the warm-air valves of Moeder Wolkwal shut ("warme lucht = vads = drijven: geen warme lucht, geen walvis").
 * <ul>
 *   <li><b>De kleppen:</b> four valves (purple: right-click). Every ~25 s he turns one shut (a purple line from him to it, 1.5 s);
 *       every closed valve gives him 15 % less damage taken and the cold creeps in (6 % on everyone every 3 s while any is shut).
 *       Right-click a closed valve 3 times to turn it open again.</li>
 *   <li>Moersleutelzwaai (red circle around him), Kaasgasbom (red circle on someone + Plakkerig), Buikschuiver (red strip: he slides
 *       through and shoves).</li>
 *   <li>60 %: he whistles two Mika-saboteurs in. 25 %: ALL valves shut at once; he can't drop below 15 % until at least three are
 *       open again.</li>
 * </ul>
 */
public class Gorgonzola extends Z5Baas {
    public static final int KLEPPEN = 4, KLIKS = 3;
    public static final double KLEP_AFSTAND = 9;
    private final List<UUID> kleppen = new ArrayList<>();
    private final int[] kliks = new int[KLEPPEN];
    private final boolean[] dicht = new boolean[KLEPPEN];
    private int klepCd = 400, zwaaiCd = 70, bomCd = 110, schuifCd = 180, schuifT = -1;
    private Vec3 van = Vec3.ZERO, naar = Vec3.ZERO;
    private final Set<UUID> geschoven = new HashSet<>();

    public Gorgonzola(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public String baasId() {
        return "gorgonzola";
    }

    @Override
    protected boolean isMika() {
        return true;
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"aanval", "hef", "klap", "spuug"};
    }

    @Override
    protected double hp(int level) {
        return 11000 + 380 * level;
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
        return new double[] {0.6, 0.25};
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

    public boolean dicht(int i) {
        return dicht[i];
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        return Math.max(0.4, 1 - 0.15 * dichtAantal());
    }

    @Override
    protected double vadsVloer() {
        return fase() >= 3 && dichtAantal() >= 2 ? 0.15 : 0;
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z5wezens.gz.kleppen_eerst";
    }

    @Override
    protected void opFase(int fase) {
        kaart("vadsrijk.z5wezens.gz.fase" + fase, fase == 3 ? ROOD : PAARS);
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (fase == 2) {
            praat("gz.fluit");
            for (int i = 0; i < adds(2); i++) {
                if (Z5WezensRegistry.MIKA_SABOTEUR.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof VadsrijkMob m) {
                    Vec3 v = ring(zMidden(), 6, i * Math.PI);
                    m.snapTo(v.x, v.y + 0.2, v.z, 0, 0);
                    m.stelIn(57, null, false, Set.of());
                    sl.addFreshEntity(m);
                    if (!strijders().isEmpty()) {
                        m.setTarget(strijders().get(0));
                    }
                }
            }
        }
        if (fase == 3) {
            praat("gz.alles_dicht");
            for (int i = 0; i < KLEPPEN; i++) {
                sluit(sl, i);
            }
        }
    }

    // ---- the valves -----------------------------------------------------------------------------------------------------------------

    private Vec3 klepPlek(int i) {
        return ring(zMidden(), KLEP_AFSTAND, Math.PI / 4 + i * Math.PI / 2).add(0, 0.1, 0);
    }

    @Nullable
    private Z3Doel klep(ServerLevel sl, int i) {
        return i < kleppen.size() && sl.getEntity(kleppen.get(i)) instanceof Z3Doel d && !d.isRemoved() ? d : null;
    }

    private void kleppenKlaar(ServerLevel sl) {
        if (kleppen.size() == KLEPPEN && klep(sl, 0) != null) {
            return;
        }
        for (UUID u : kleppen) {
            if (sl.getEntity(u) != null) {
                sl.getEntity(u).discard();
            }
        }
        kleppen.clear();
        for (int i = 0; i < KLEPPEN; i++) {
            Z3Doel d = Z5WezensRegistry.KLEP.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (d == null) {
                return;
            }
            d.koppel(this, i, 100, klepPlek(i));
            d.zetRaakbaar(false);
            d.zetStand(dicht[i] ? 1 : 0);
            d.kijk((float) Math.toDegrees(Math.atan2(-(zMidden().x - klepPlek(i).x), zMidden().z - klepPlek(i).z)));
            sl.addFreshEntity(d);
            kleppen.add(d.getUUID());
        }
    }

    /** Turns valve i shut (dev/tests too). */
    public void sluit(ServerLevel sl, int i) {
        dicht[i] = true;
        kliks[i] = 0;
        Z3Doel d = klep(sl, i);
        if (d != null) {
            d.zetStand(1);
            sl.sendParticles(new DustParticleOptions(0x8C8C98, 1.8f), d.getX(), d.getY() + 1, d.getZ(), 25, 0.4, 0.6, 0.4, 0);
        }
        sl.playSound(null, blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.HOSTILE, 1.5f, 0.6f);
    }

    @Override
    public void doelGebruikt(Z3Doel d, ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl) || d.nummer < 0 || d.nummer >= KLEPPEN) {
            return;
        }
        int i = d.nummer;
        if (!dicht[i]) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("vadsrijk.z5wezens.gz.klep_open").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        kliks[i]++;
        d.speel("pomp");
        sl.playSound(null, d.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1f, 0.8f + 0.2f * kliks[i]);
        if (kliks[i] >= KLIKS) {
            dicht[i] = false;
            kliks[i] = 0;
            d.zetStand(0);
            sl.sendParticles(ParticleTypes.CLOUD, d.getX(), d.getY() + 1.4, d.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
            sl.sendParticles(new DustParticleOptions(0xFF8AC8, 1.4f), d.getX(), d.getY() + 1.4, d.getZ(), 15, 0.4, 0.6, 0.4, 0);
            sl.playSound(null, d.blockPosition(), SoundEvents.BREEZE_IDLE_GROUND, SoundSource.BLOCKS, 1f, 1.4f);
            zeg("vadsrijk.z5wezens.gz.klep_weer_open", ChatFormatting.LIGHT_PURPLE, dichtAantal());
        } else {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("vadsrijk.z5wezens.gz.klep_draai", kliks[i], KLIKS)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // ---- the fight ---------------------------------------------------------------------------------------------------------------------

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (klepCd <= 0 && dichtAantal() < KLEPPEN) {
            List<Integer> open = new ArrayList<>();
            for (int i = 0; i < KLEPPEN; i++) {
                if (!dicht[i]) {
                    open.add(i);
                }
            }
            int i = open.get(getRandom().nextInt(open.size()));
            Telegraaf.lijn(sl, position(), klepPlek(i), 1.2, tg(30), PAARS);
            zeg("vadsrijk.z5wezens.gz.draait", ChatFormatting.LIGHT_PURPLE);
            speel("hef");
            later(tg(30), () -> sluit(sl, i));
            klepCd = 500;
            rustT = 40;
            laatsteAanval = "klep";
            return;
        }
        if (schuifCd <= 0) {
            ServerPlayer v = iemandOpAfstand() ? afstandsDoel() : zDoel();
            if (v != null) {
                van = position();
                Vec3 r = voetVan(v).subtract(van).multiply(1, 0, 1);
                r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
                naar = voetVan(van.add(r.scale(Math.max(8, voetVan(v).distanceTo(van) + 3))));
                Telegraaf.lijn(sl, van, naar, 3, tg(25), ROOD);
                schuifT = tg(25) + 10;
                geschoven.clear();
                schuifCd = 200;
                rustT = schuifT + 15;
                laatsteAanval = "schuif";
                return;
            }
        }
        if (bomCd <= 0) {
            ServerPlayer v = l.get(getRandom().nextInt(l.size()));
            speel("spuug");
            inslag(voetVan(v), 3, 30, 0.25, "kaasgasbom", Status.PLAKKERIG, ParticleTypes.LARGE_SMOKE);
            bomCd = fase() >= 2 ? 110 : 150;
            rustT = 25;
            laatsteAanval = "bom";
            return;
        }
        if (zwaaiCd <= 0 && zDoel() != null && zDoel().distanceTo(this) < 6) {
            speel("klap");
            inslag(position(), 4, 22, 0.3, "moersleutel", null, ParticleTypes.CRIT);
            zwaaiCd = 90;
            rustT = 30;
            laatsteAanval = "zwaai";
            return;
        }
        rustT = 10;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        kleppenKlaar(sl);
        klepCd--;
        zwaaiCd--;
        bomCd--;
        schuifCd--;
        if (schuifT >= 0) {
            schuifT--;
            if (schuifT < 10) {
                Vec3 v = van.lerp(naar, 1 - schuifT / 10.0);
                setPos(v.x, v.y, v.z);
                setDeltaMovement(Vec3.ZERO);
                for (ServerPlayer p : l) {
                    if (p.distanceToSqr(this) < 2.6 * 2.6 && geschoven.add(p.getUUID())) {
                        mech(p, 0.3, "buikschuiver");
                        duw(p, position(), 1.6);
                    }
                }
            }
        }
        if (dichtAantal() > 0 && tickCount % 60 == 0) {
            for (ServerPlayer p : l) {
                mech(p, 0.06, "kou");
                sl.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1, p.getZ(), 8, 0.4, 0.6, 0.4, 0.01);
            }
        }
        if (tickCount % 10 == 0) {
            for (int i = 0; i < KLEPPEN; i++) {
                Vec3 k = klepPlek(i);
                sl.sendParticles(dicht[i] ? new DustParticleOptions(0x8C8C98, 1.2f) : new DustParticleOptions(0xFFB07A, 1.0f), k.x, k.y + 2.2, k.z, 2, 0.15, 0.3, 0.15, 0);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl) {
            for (UUID u : kleppen) {
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
        super.verslagen(laatste);
        if (!was && geplofd()) {
            praat("gz.weg");
        }
    }
}
