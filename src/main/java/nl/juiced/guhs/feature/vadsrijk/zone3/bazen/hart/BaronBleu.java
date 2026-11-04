package nl.juiced.guhs.feature.vadsrijk.zone3.bazen.hart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Blokken;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Figurant;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Vast;

/**
 * Baron Bleu en de Schimmelkolos, the last boss of Het Hart van de Boom (lv 50, Water/Slijm + mould). Graaf Piepvel's
 * lieutenant (de Knokkel): since the Keizer snatched the count, Bleu wants to be the new Pink by bringing Him the egg.
 * <ul>
 *   <li>Fase 1: he stands on his walking mould kettle. Mould fields creep over the floor (Gesporen + Plakkerig + a small tick);
 *       a LIT lantern post burns every field within 6 blocks away: fight in the light. The kettle sprays a post out now and then
 *       (purple line): right-click to relight it. Rapier throws at whoever stands at range.</li>
 *   <li>Fase 2 (66 %): he jumps off and fights himself: rapier lunges (red strip), and the Rijpingskamer: a purple circle on a
 *       player; whoever is still in it gets locked up (teammates sneak next to the cage, solo click/shift loose).</li>
 *   <li>Fase 3 (33 %): the Kolos reaches for the egg: a yellow stack circle at the egg every 12 s. Stand in it together to block
 *       the arm (the hit is shared; solo one player can block it but takes a big hit). Miss it: the Baron heals 8 % and everyone
 *       takes 25 %.</li>
 *   <li>Enrage: the whole hall moulds. Vahoeg: the mould creeps up the walls too (extra fields along the edge).</li>
 *   <li>Defeat: the Kolos collapses; Bleu flees in his escape zeppelin through a hole in the roots.</li>
 * </ul>
 */
public class BaronBleu extends Z3Baas {
    public static final String H_PALEN = "z3_bleu_palen";

    private record Veld(Vec3 plek, double[] straal, int[] t) {
    }

    private final List<Veld> velden = new ArrayList<>();
    /** The walking mould kettle: hittable (melee reaches its legs), every hit goes to the Baron. */
    @Nullable
    private Z3Doel kolos;
    @Nullable
    private Z3Figurant ei;
    private boolean opKolos = true, opgezet;
    private int veldCd = 60, sprayCd = 300, degenCd = 50, kamerCd = 160, grijpCd = 100, lungeT = -1;
    private Vec3 van = Vec3.ZERO, naar = Vec3.ZERO;
    /** Gametests: fields burned by the light, egg grabs blocked / missed. */
    public int verbrand, geblokt, gemist;

    public BaronBleu(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public String baasId() {
        return "bleu";
    }

    @Override
    public String kerker() {
        return "hart";
    }

    @Override
    public boolean eindbaas() {
        return true;
    }

    @Override
    protected boolean isMika() {
        return true;
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"slag", "worp", "sprong", "draai"};
    }

    @Override
    protected double hp(int level) {
        return 9000 + 340 * level;
    }

    @Override
    protected double klap(int level) {
        return 22 + 2.5 * level;
    }

    @Override
    public int xp() {
        return nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Xp.baas(Math.min(50, mobLevel()));
    }

    @Override
    public Element standaardElement() {
        return Element.WATER;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.66, 0.33};
    }

    @Override
    protected double jaagSnelheid() {
        return opKolos ? 0 : 1.15;
    }

    @Override
    protected void bijt(ServerLevel sl, ServerPlayer d) {
        if (!opKolos) {
            speel("slag");
            raak(d, 1.1, "degen");
        }
    }

    @Override
    protected void enragePuls(ServerLevel sl, List<ServerPlayer> l) {
        super.enragePuls(sl, l);
        for (ServerPlayer p : l) {
            Statussen.geef(p, Status.GESPOREN, 60, 2, this);
        }
    }

    @Override
    public void intro(ServerPlayer p, boolean eersteKeer) {
        super.intro(p, eersteKeer);
        p.sendSystemMessage(Component.translatable(titelKey()).withStyle(ChatFormatting.GOLD).append(": ")
                .append(Component.translatable("vadsrijk.z3bazen.bb.intro").withStyle(ChatFormatting.WHITE)));
    }

    @Nullable
    public Z3Doel kolos() {
        return kolos;
    }

    public Vec3 eiPlek() {
        return punt(Z3BazenPlekken.BLEU_EI);
    }

    public List<BlockPos> palen() {
        List<BlockPos> l = new ArrayList<>();
        for (int[] o : Z3BazenPlekken.BLEU_PALEN) {
            l.add(blok(o));
        }
        return l;
    }

    public int velden() {
        return velden.size();
    }

    private void zetPaal(ServerLevel sl, BlockPos b, boolean aan) {
        var s = sl.getBlockState(b);
        if (s.isAir() || s.canBeReplaced() || s.getBlock() instanceof Z3Blokken.Lantaarnpaal) {
            Herstel.zet(sl, H_PALEN, b, Z3BazenRegistry.LANTAARNPAAL.get().defaultBlockState().setValue(Z3Blokken.Lantaarnpaal.LIT, aan));
        }
    }

    @Override
    public boolean paalAan(ServerLevel sl, BlockPos pos, ServerPlayer p) {
        if (!palen().contains(pos)) {
            return false;
        }
        zetPaal(sl, pos, true);
        sl.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f, 1.4f);
        return true;
    }

    @Override
    public void doelGeraakt(Z3Doel d, double n, @Nullable LivingEntity bron) {
        if (d == kolos) {
            d.zetVads(d.maxVads());
            krijgSchade(n, bron);
        }
    }

    /** A mould field (gametests: also from outside). */
    public void veld(Vec3 v) {
        velden.add(new Veld(new Vec3(v.x, zMidden().y, v.z), new double[] {2.5}, new int[] {0}));
    }

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (fase == 2) {
            kaart("vadsrijk.z3bazen.bb.fase2", ROOD);
            opKolos = false;
            setNoGravity(false);
            Vec3 doel = zDoel() != null ? voetVan(zDoel()) : zMidden();
            zSpring(doel, 18, 4);
            speel("sprong");
            spreek("vadsrijk.z3bazen.bb.zelf");
        } else if (fase == 3) {
            kaart("vadsrijk.z3bazen.bb.fase3", GEEL);
            spreek("vadsrijk.z3bazen.bb.ei");
            grijpCd = 40;
        }
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (fase() >= 3 && grijpCd <= 0) {
            grijpCd = 240;
            Vec3 e = eiPlek();
            zeg("vadsrijk.z3bazen.bb.grijparm", ChatFormatting.YELLOW);
            stapel(e, 3, 60, 0.5, 0.25, 2, "grijparm", ok -> {
                if (ok) {
                    geblokt++;
                    zeg("vadsrijk.z3bazen.bb.geblokt", ChatFormatting.GREEN);
                } else {
                    gemist++;
                    heal(maxVads() * 0.08);
                    zeg("vadsrijk.z3bazen.bb.gegrepen", ChatFormatting.RED);
                }
                sl.playSound(null, e.x, e.y, e.z, SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 2f, 0.5f);
            });
            rustT = 40;
            laatsteAanval = "grijparm";
            return;
        }
        if (!opKolos && kamerCd <= 0) {
            ServerPlayer v = l.get(getRandom().nextInt(l.size()));
            Vec3 plek = voetVan(v);
            Telegraaf.cirkel(sl, plek, 1.8, tg(30), PAARS);
            later(tg(30), () -> {
                for (ServerPlayer q : inCirkel(plek, 1.8)) {
                    Vec3 vast = q.position();
                    Z3Vast.pak(this, q, "kamer", () -> vast, 10, 120, 0.04, () -> mech(q, 0.3, "rijping"));
                    sl.sendParticles(new DustParticleOptions(0x7A8AB0, 2f), vast.x, vast.y + 1, vast.z, 40, 0.6, 1, 0.6, 0);
                }
            });
            zeg("vadsrijk.z3bazen.bb.kamer", ChatFormatting.LIGHT_PURPLE);
            kamerCd = 300;
            rustT = 30;
            laatsteAanval = "kamer";
            return;
        }
        if (degenCd <= 0) {
            ServerPlayer v = afstandsDoel();
            if (v != null) {
                if (opKolos) {
                    speel("worp");
                    inslag(voetVan(v), 2.5, 25, 0.25, "degenworp", null, ParticleTypes.CRIT);
                } else {
                    van = position();
                    Vec3 r = voetVan(v).subtract(van).multiply(1, 0, 1);
                    r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
                    naar = voetVan(van.add(r.scale(Math.max(7, voetVan(v).distanceTo(van) + 2))));
                    Telegraaf.lijn(sl, van, naar, 2.4, tg(22), ROOD);
                    lungeT = tg(22) + 8;
                    speel("slag");
                }
                degenCd = 90;
                rustT = 30;
                laatsteAanval = "degen";
                return;
            }
        }
        rustT = 10;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        veldCd--;
        sprayCd--;
        degenCd--;
        kamerCd--;
        grijpCd--;
        if (!opgezet) {
            opgezet = true;
            Herstel.terug(sl, H_PALEN);
            for (BlockPos b : palen()) {
                zetPaal(sl, b, true);
            }
            Vec3 m = zMidden();
            kolos = Z3BazenRegistry.KOLOS.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (kolos != null) {
                kolos.koppel(this, 0, 1.0e9, m.add(0, 0, 4));
                kolos.kijk(180);
                sl.addFreshEntity(kolos);
            }
            ei = Z3Figurant.maak(sl, eiPlek(), 0, "vr_z3_baas_ei", "textures/vadsrijk/z3bazen/ei.png", "vr_z3_baas_ei", "idle", 1.5f);
        }
        // the kolos walks slowly after the target, the baron rides on top in fase 1; in fase 3 it goes to the egg
        if (kolos != null && !kolos.isRemoved()) {
            Vec3 doel;
            if (fase() >= 3) {
                Vec3 e = eiPlek(), m = zMidden();
                Vec3 r = m.subtract(e).multiply(1, 0, 1);
                doel = e.add(r.lengthSqr() < 0.01 ? new Vec3(0, 0, 4) : r.normalize().scale(4.5));
            } else {
                ServerPlayer d = zDoel();
                doel = d != null ? voetVan(d) : zMidden();
            }
            Vec3 k = kolos.position();
            Vec3 d = doel.subtract(k).multiply(1, 0, 1);
            if (d.length() > 4) {
                kolos.zetPlek(k.add(d.normalize().scale(0.05)));
                kolos.kijk((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
            } else if (tickCount % 60 == 0) {
                kolos.speel(fase() >= 3 ? "grijp" : "pomp");
            }
            if (opKolos) {
                Vec3 top = kolos.position().add(0, 4.9, 0);
                setNoGravity(true);
                setPos(top.x, top.y, top.z);
                setDeltaMovement(Vec3.ZERO);
                getNavigation().stop();
            }
        }
        // mould fields: new ones, growing, ticking, burned by the light
        if (veldCd <= 0) {
            veldCd = opKolos ? 110 : 160;
            ServerPlayer p = l.get(getRandom().nextInt(l.size()));
            veld(voetVan(p));
            if (vahoeg()) {
                veld(ring(zMidden(), zStraal() - 2, getRandom().nextDouble() * Math.PI * 2));
                veld(ring(zMidden(), zStraal() - 2, getRandom().nextDouble() * Math.PI * 2));
            }
            sl.playSound(null, blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 2f, 0.5f);
        }
        List<BlockPos> lit = palen().stream().filter(b -> Z3Blokken.Lantaarnpaal.aan(sl, b)).toList();
        for (Iterator<Veld> it = velden.iterator(); it.hasNext(); ) {
            Veld v = it.next();
            v.t()[0]++;
            v.straal()[0] = Math.min(4.5, v.straal()[0] + 0.01);
            boolean weg = v.t()[0] > 20 * 30;
            for (BlockPos b : lit) {
                if (Vec3.atBottomCenterOf(b).distanceTo(v.plek()) < 6 + v.straal()[0] * 0.5) {
                    weg = true;
                    verbrand++;
                    sl.sendParticles(ParticleTypes.FLAME, v.plek().x, v.plek().y + 0.3, v.plek().z, 20, v.straal()[0] * 0.4, 0.1, v.straal()[0] * 0.4, 0.02);
                }
            }
            if (weg) {
                it.remove();
                continue;
            }
            if (v.t()[0] % 5 == 0) {
                int n = (int) (v.straal()[0] * 5);
                for (int i = 0; i < n; i++) {
                    double a = getRandom().nextDouble() * Math.PI * 2, r = Math.sqrt(getRandom().nextDouble()) * v.straal()[0];
                    sl.sendParticles(new DustParticleOptions(0x6E7E98, 1.6f), v.plek().x + Math.cos(a) * r, v.plek().y + 0.15, v.plek().z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                }
            }
            if (v.t()[0] % 20 == 0) {
                for (ServerPlayer p : inCirkel(v.plek(), v.straal()[0])) {
                    mech(p, 0.04, "schimmelveld");
                    Statussen.geef(p, Status.GESPOREN, 40, 2, this);
                    Statussen.geef(p, Status.PLAKKERIG, 40, 0.4, this);
                }
            }
        }
        // the kolos sprays a lit post out
        if (sprayCd <= 0 && !lit.isEmpty() && kolos != null) {
            sprayCd = 400;
            BlockPos b = lit.get(getRandom().nextInt(lit.size()));
            Telegraaf.lijn(sl, kolos.position(), Vec3.atBottomCenterOf(b), 1.5, tg(40), PAARS);
            zeg("vadsrijk.z3bazen.bb.spray", ChatFormatting.LIGHT_PURPLE);
            later(tg(40), () -> {
                zetPaal(sl, b, false);
                sl.sendParticles(ParticleTypes.SMOKE, b.getX() + 0.5, b.getY() + 1.2, b.getZ() + 0.5, 20, 0.2, 0.3, 0.2, 0.02);
            });
        }
        // the rapier lunge
        if (lungeT >= 0) {
            lungeT--;
            if (lungeT < 8) {
                Vec3 v = van.lerp(naar, 1 - lungeT / 8.0);
                setPos(v.x, v.y, v.z);
                for (ServerPlayer p : l) {
                    if (p.distanceToSqr(this) < 2 * 2 && lungeT == 4) {
                        mech(p, 0.3, "uitval");
                    }
                }
            }
        }
    }

    @Override
    protected void opPlof() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        setNoGravity(true);
        rook(sl, position(), 1);
        spreek("vadsrijk.z3bazen.bb.vlucht");
        if (kolos != null) {
            pluis(sl, kolos.position().add(0, 2.5, 0), 4, 5);
            sl.playSound(null, kolos.blockPosition(), SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 2f, 0.5f);
            kolos.discard();
        }
        velden.clear();
    }

    @Override
    protected int plofDuur() {
        return 100;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && geplofd() && level() instanceof ServerLevel sl) {
            setDeltaMovement(Vec3.ZERO);
            setPos(getX(), getY() + 0.15, getZ() - 0.05);   // up through the hole in the roots, in his escape zeppelin
            if (tickCount % 2 == 0) {
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() - 0.2, getZ(), 1, 0.2, 0.1, 0.2, 0.01);
            }
        }
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd() && level() instanceof ServerLevel sl) {
            Z3Vast.losAlles(getUUID(), sl.getServer());
            for (BlockPos b : palen()) {
                zetPaal(sl, b, true);
            }
            Z3Figurant e = ei;
            nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry.later(sl.getServer(), 20 * 30, () -> {
                Herstel.terug(sl, H_PALEN);
                if (e != null) {
                    e.discard();
                }
            });
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.DISCARDED && level() instanceof ServerLevel sl && !geplofd()) {
            Herstel.terug(sl, H_PALEN);
            if (ei != null) {
                ei.discard();
            }
        }
        if (kolos != null) {
            kolos.discard();
        }
        super.remove(reason);
    }
}
