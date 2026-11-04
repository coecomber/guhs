package nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;

/**
 * Bombardier Brokkel (Kaaszeppelinwerf boss 2, lv 39, Vuur).
 * <ul>
 *   <li>Kaaswielbommen: red lanes across the hall, then burning cheese wheels roll down them (30 % + Brandend).</li>
 *   <li>Bomb lobs at whoever stands at range.</li>
 *   <li>4 armour plates: with all 4 he takes only 40 % damage. Two deck cannons stand in the hall: right-click = fire where you
 *       look; a hit knocks a plate off (+4 % of his Vads, stunned 1.5 s). The cannon reloads faster when you're alone. Every lost
 *       plate makes him faster. Vahoeg: a ring of bombs around him each time a plate falls.</li>
 * </ul>
 */
public class Brokkel extends Z3Baas {
    private static final EntityDataAccessor<Integer> PLATEN = SynchedEntityData.defineId(Brokkel.class, EntityDataSerializers.INT);
    public static final int[][] KANONNEN = nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenPlekken.BROKKEL_KANONNEN;
    private final List<Z3Doel> kanonnen = new ArrayList<>();
    private final long[] herlaad = new long[2];
    private int wielCd = 50, bomCd = 30;
    /** Gametests: cannon shots that hit. */
    public int voltreffers;

    public Brokkel(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PLATEN, 4);
    }

    public int platen() {
        return entityData.get(PLATEN);
    }

    @Override
    public String baasId() {
        return "brokkel";
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
        return new String[] {"worp", "slag", "draai"};
    }

    @Override
    protected double hp(int level) {
        return 5000 + 220 * level;
    }

    @Override
    protected double klap(int level) {
        return 16 + 2.0 * level;
    }

    @Override
    public int xp() {
        return 3300 + 125 * mobLevel();
    }

    @Override
    public Element standaardElement() {
        return Element.VUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.6, 0.3};
    }

    @Override
    protected double jaagSnelheid() {
        return 1.0 + 0.08 * (4 - platen());
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        return 1 - 0.15 * platen();
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z3bazen.br.pantser";
    }

    public List<Z3Doel> kanonnen() {
        return kanonnen;
    }

    @Override
    protected void opFase(int fase) {
        kaart("vadsrijk.z3bazen.br.fase" + fase, ROOD);
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        int sneller = 4 - platen();
        if (wielCd <= 0) {
            int banen = 2 + (fase() >= 2 ? 1 : 0) + (fase() >= 3 ? 1 : 0);
            Vec3 m = zMidden();
            double r = zStraal() - 1;
            boolean langsX = getRandom().nextBoolean();
            for (int i = 0; i < banen; i++) {
                ServerPlayer p = l.get(getRandom().nextInt(l.size()));
                double off = i == 0 ? (langsX ? p.getZ() - m.z : p.getX() - m.x) : (getRandom().nextDouble() * 2 - 1) * (r - 2);
                Vec3 a = langsX ? new Vec3(m.x - r, m.y, m.z + off) : new Vec3(m.x + off, m.y, m.z - r);
                Vec3 b = langsX ? new Vec3(m.x + r, m.y, m.z + off) : new Vec3(m.x + off, m.y, m.z + r);
                if (getRandom().nextBoolean()) {
                    Vec3 t = a;
                    a = b;
                    b = t;
                }
                rol(a, b, 3, 36, 26, 0.3, "kaaswiel", ParticleTypes.FLAME, Status.BRANDEND);
            }
            speel("worp");
            zeg("vadsrijk.z3bazen.br.wielen", ChatFormatting.RED);
            wielCd = 170 - sneller * 20;
            rustT = 40;
            laatsteAanval = "wielen";
            return;
        }
        if (bomCd <= 0) {
            ServerPlayer v = iemandOpAfstand() ? afstandsDoel() : l.get(getRandom().nextInt(l.size()));
            if (v != null) {
                speel("worp");
                inslag(voetVan(v), 3, 30, 0.3, "bom", Status.BRANDEND, ParticleTypes.LAVA);
                bomCd = 70 - sneller * 8;
                rustT = 20 - sneller * 2;
                laatsteAanval = "bom";
                return;
            }
        }
        rustT = 10;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        wielCd--;
        bomCd--;
        if (kanonnen.isEmpty() || kanonnen.stream().anyMatch(k -> k.isRemoved())) {
            kanonnen.forEach(k -> k.discard());
            kanonnen.clear();
            for (int i = 0; i < KANONNEN.length; i++) {
                Z3Doel k = Z3BazenRegistry.KANON.get().create(sl, EntitySpawnReason.EVENT);
                if (k != null) {
                    Vec3 v = punt(KANONNEN[i]);
                    k.koppel(this, i, 1000, v);
                    k.zetRaakbaar(false);
                    k.kijk(180);
                    sl.addFreshEntity(k);
                    kanonnen.add(k);
                }
            }
        }
        if (platen() > 0 && tickCount % 10 == 0) {
            sl.sendParticles(new DustParticleOptions(0xC8B060, 1.2f), getX(), getY() + 1, getZ(), 3, 0.6, 0.4, 0.6, 0);
        }
    }

    /** A player fires a deck cannon where they look. */
    @Override
    public void doelGebruikt(Z3Doel kanon, ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        int i = Math.max(0, Math.min(1, kanon.nummer));
        long nu = sl.getGameTime();
        if (nu < herlaad[i]) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.z3bazen.br.laden", (herlaad[i] - nu + 19) / 20).withStyle(ChatFormatting.GOLD));
            return;
        }
        Tips.toon(p, "vadsrijk.z3bazen.tip.kanon");
        herlaad[i] = nu + (strijders().size() <= 1 ? 80 : 160);
        Vec3 r = p.getLookAngle().multiply(1, 0, 1);
        r = r.lengthSqr() < 0.01 ? new Vec3(0, 0, -1) : r.normalize();
        kanon.kijk((float) Math.toDegrees(Math.atan2(-r.x, r.z)));
        kanon.speel("schiet");
        Vec3 a = kanon.position().add(0, 1, 0);
        sl.playSound(null, kanon.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.5f, 1.2f);
        boolean raak = false;
        for (double s = 1; s < 32; s += 0.8) {
            Vec3 v = a.add(r.scale(s));
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.YELLOW_CONCRETE.defaultBlockState()), v.x, v.y, v.z, 2, 0.1, 0.1, 0.1, 0);
            if (v.distanceToSqr(getX(), getY() + 1, getZ()) < 2.4 * 2.4) {
                raak = true;
                break;
            }
        }
        if (raak) {
            voltreffers++;
            plaatEraf(sl, p);
        }
    }

    public void plaatEraf(ServerLevel sl, @Nullable ServerPlayer p) {
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 3, 0.5, 0.5, 0.5, 0);
        if (platen() > 0) {
            entityData.set(PLATEN, platen() - 1);
            zeg("vadsrijk.z3bazen.br.plaat", ChatFormatting.GREEN, platen());
            sl.playSound(null, blockPosition(), SoundEvents.ANVIL_BREAK, SoundSource.HOSTILE, 1.5f, 1.2f);
            if (vahoeg()) {
                for (int k = 0; k < 6; k++) {
                    inslag(ring(position(), 5, k * Math.PI / 3), 2.2, 30, 0.25, "ringbom", Status.BRANDEND, ParticleTypes.LAVA);
                }
            }
        }
        Statussen.geef(this, Status.VERDOOFD, 30, 1, null);
        krijgSchade(maxVads() * 0.04 / Math.max(0.4, schadeFactor(null)), p);
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd()) {
            spreek("vadsrijk.z3bazen.br.vlucht");
            kanonnen.forEach(k -> k.discard());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        kanonnen.forEach(k -> k.discard());
        super.remove(reason);
    }
}
