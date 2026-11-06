package nl.juiced.guhs.feature.vadsrijk.imker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenLoot;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;

/**
 * T-IMKER: De Mika-imker ("Mika de Imker"), the SECRET boss of zone 3 (lv 47, Natuur), in his raatzaal under the Raattros
 * of Honingtak. A spy of the Keizer in a far too big beekeeper's suit: he turns the bee queen grey with "rijpe kaas-honing"
 * so all of Honingtak's honey goes grey.
 * <ul>
 *   <li><b>The room:</b> the Koninginnekorf (the queen's hive, {@link #korf()}) stands north of the middle; right-click it =
 *       turn its opening 60 degrees. Three Rookpotjes ({@link #potten()}) stand round the edge; right-click one = a smoke cloud
 *       for 8 s (then it needs to cool down).</li>
 *   <li><b>Phase 1 "Kwaliteitscontrole" (100-66 %):</b> grey honey lobs (red circle, then a sticky puddle that slows and
 *       stings a little, 20 s), a cheese-smoke puff in a line in front of him, and an angry swarm that chases one player:
 *       lead it through a smoke cloud and it falls asleep.</li>
 *   <li><b>Phase 2 "De grauwe koningin" (66-33 %):</b> he jumps onto one of three perches and pours grey honey into the hive
 *       (a grey meter; he can't be hurt while pouring). Every 6 s the queen's bees shoot out of the hive's opening along a red
 *       lane. Turn the hive so its opening points at his perch: the stream stings HIM (stunned 5 s, he takes extra damage, the
 *       meter drops). A full meter = the queen turns grey: a big hit on everyone, and the meter starts again at 40 %.</li>
 *   <li><b>Phase 3 "De grote rookpot" (33-0 %):</b> he comes down and chases again, two swarms at once, more lobs, and the big
 *       cheese-smoke bomb: a yellow circle at the hive (the queen's wings shelter you): everybody in it shares the hit; too few
 *       inside = everyone takes a hit.</li>
 *   <li>Defeat: he flees in smoke ("Dit komt in mijn rapport!"); the queen turns gold again ({@link ImkerVerhaal#na}).</li>
 * </ul>
 * Offsets are from the room's middle (the arena centre); the ladder from the hatch comes down at the south edge.
 */
public class MikaImker extends Z3Baas {
    private static final EntityDataAccessor<Boolean> GIET = SynchedEntityData.defineId(MikaImker.class, EntityDataSerializers.BOOLEAN);

    /** The hive (north of the middle) and the three smoke pots (round the edge, clear of the ladder at the south edge). */
    public static final int[] KORF = {0, 0, -2};
    public static final int[][] POTTEN = {{-7, 0, -3}, {7, 0, -3}, {4, 0, 6}};
    /** The perches in phase 2: from the hive in these opening directions (k * 60 degrees), this far. */
    public static final int[] STOKKEN = {0, 2, 4};
    public static final double STOK_AFSTAND = 7;
    public static final double LAAN = 12, LAAN_BREED = 3;
    public static final int ROOK_TICKS = 160, ROOK_CD = 300, ROOK_CD_SOLO = 200;
    public static final double ROOK_STRAAL = 3.5, PLAS_STRAAL = 2.2, ZWERM_RAAK = 1.6;
    public static final int PLAS_TICKS = 400, ZWERM_TICKS = 240, MAX_PLASSEN = 6;
    public static final int GRAUW_VOL = 600;   // ticks of pouring until the queen turns grey (30 s)
    public static final int KORF_VUUR = 120, KORF_WAARSCHUW = 30, STEEK_TICKS = 100;

    public static final int HONING = 0xE0B040, GRAUW = 0x8A8A96, ROOK = 0xD8D8D0;

    /** A smoke cloud (from a pot). */
    public record Rook(Vec3 plek, long tot) {
    }

    /** A sticky grey honey puddle. */
    public record Plas(Vec3 plek, long tot) {
    }

    /** An angry swarm chasing one player. */
    public static final class Zwerm {
        public Vec3 plek;
        public UUID doel;
        public int leven;
        final Map<UUID, Long> gestoken = new HashMap<>();

        Zwerm(Vec3 plek, UUID doel, int leven) {
            this.plek = plek;
            this.doel = doel;
            this.leven = leven;
        }
    }

    private final List<Z3Doel> potten = new ArrayList<>();
    @Nullable
    private Z3Doel korf;
    private final long[] potKlaar = new long[POTTEN.length];
    public final List<Rook> rook = new ArrayList<>();
    public final List<Plas> plassen = new ArrayList<>();
    public final List<Zwerm> zwermen = new ArrayList<>();
    /** The hive opening: 0..5 (k * 60 degrees; 0 = east). */
    private int richting = 3;
    private long gedraaid;
    /** Phase 2: which perch (index into STOKKEN), the grey meter (ticks poured), the next stream. */
    public int stok = -1;
    public int grauw;
    private int vuurT = KORF_VUUR;
    private int klodderCd = 40, pufCd = 90, zwermCd = 60, bomCd = 100;
    private int rustNaSteek;
    /** Gametests: how often the stream stung him, swarms put to sleep, big bombs. */
    public int gestoken, gesust, bommen;
    /** Gametests: no attacks of his own (the room, the swarms, the puddles and phase 2 still run). */
    public boolean testStil;

    public MikaImker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(GIET, false);
    }

    /** Pouring grey honey (phase 2, on a perch)? (synced: the "giet" pose) */
    public boolean giet() {
        return entityData.get(GIET);
    }

    private void zetGiet(boolean g) {
        entityData.set(GIET, g);
    }

    // ---- identity -------------------------------------------------------------------------------------------------------

    @Override
    public String baasId() {
        return "imker";
    }

    @Override
    public String titelKey() {
        return "vadsrijk.z3imker.titel";
    }

    @Override
    protected boolean isMika() {
        return true;
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"slag", "worp", "puf", "sprong", "giet"};
    }

    @Override
    protected double hp(int level) {
        return 7800 + 300 * level;
    }

    @Override
    protected double klap(int level) {
        return 19 + 2.2 * level;
    }

    @Override
    public int xp() {
        return nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Xp.baas(Math.min(50, mobLevel())) * 3 / 2;   // (a secret: a bit extra)
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
        return fase() >= 3 ? 1.15 : 1.0;
    }

    @Override
    public void intro(ServerPlayer p, boolean eersteKeer) {
        super.intro(p, eersteKeer);
        Tips.toon(p, "vadsrijk.z3imker.tip.rook");
        if (!eersteKeer) {
            spreek("vadsrijk.z3imker.zeg.weer");
        }
    }

    // ---- the room's objects ---------------------------------------------------------------------------------------------

    @Nullable
    public Z3Doel korf() {
        return korf;
    }

    public List<Z3Doel> potten() {
        return potten;
    }

    public int richting() {
        return richting;
    }

    /** The direction (x, z) of opening k. */
    public static Vec3 dir(int k) {
        double a = Math.toRadians(k * 60.0);
        return new Vec3(Math.cos(a), 0, Math.sin(a));
    }

    public Vec3 korfPlek() {
        return punt(KORF);
    }

    /** Where perch i is (phase 2). */
    public Vec3 stokPlek(int i) {
        Vec3 d = dir(STOKKEN[i]);
        return korfPlek().add(d.x * STOK_AFSTAND, 0, d.z * STOK_AFSTAND);
    }

    /** Turns the hive's opening to k (and its model). */
    public void zetRichting(int k) {
        richting = Math.floorMod(k, 6);
        if (korf != null) {
            Vec3 d = dir(richting);
            korf.kijk((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        }
    }

    private void zorgVoorSpullen(ServerLevel sl) {
        if (korf == null || korf.isRemoved()) {
            korf = ImkerRegistry.KORF.get().create(sl, EntitySpawnReason.EVENT);
            if (korf != null) {
                korf.koppel(this, 0, 1000, korfPlek());
                korf.zetRaakbaar(false);
                sl.addFreshEntity(korf);
                zetRichting(richting);
            }
        }
        if (potten.isEmpty() || potten.stream().anyMatch(Z3Doel::isRemoved)) {
            potten.forEach(Z3Doel::discard);
            potten.clear();
            for (int i = 0; i < POTTEN.length; i++) {
                Z3Doel d = ImkerRegistry.ROOKPOT.get().create(sl, EntitySpawnReason.EVENT);
                if (d != null) {
                    d.koppel(this, i, 1000, punt(POTTEN[i]));
                    d.zetRaakbaar(false);
                    sl.addFreshEntity(d);
                    potten.add(d);
                }
            }
        }
        if (korf != null) {
            korf.zetStand(grauw >= GRAUW_VOL * 2 / 3 ? 2 : grauw >= GRAUW_VOL / 3 ? 1 : 0);   // (the texture greys)
        }
    }

    @Override
    public void doelGebruikt(Z3Doel d, ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (d == korf) {
            if (sl.getGameTime() - gedraaid < 8) {
                return;
            }
            gedraaid = sl.getGameTime();
            zetRichting(richting + (p.isShiftKeyDown() ? 5 : 1));
            d.speel("klop");
            sl.playSound(null, d.blockPosition(), SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.2f, 1.0f);
            Tips.toon(p, "vadsrijk.z3imker.tip.korf");
            p.sendOverlayMessage(Component.translatable("vadsrijk.z3imker.korf.gedraaid").withStyle(ChatFormatting.GOLD));
            return;
        }
        int i = potten.indexOf(d);
        if (i < 0) {
            return;
        }
        long nu = sl.getGameTime();
        if (nu < potKlaar[i]) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.z3imker.pot.laden", (potKlaar[i] - nu + 19) / 20).withStyle(ChatFormatting.GRAY));
            return;
        }
        potKlaar[i] = nu + (strijders().size() <= 1 ? ROOK_CD_SOLO : ROOK_CD);
        rookWolk(sl, d.position());
        d.speel("pomp");
        p.sendOverlayMessage(Component.translatable("vadsrijk.z3imker.pot.rook").withStyle(ChatFormatting.WHITE));
    }

    /** A smoke cloud here for {@link #ROOK_TICKS}. */
    public void rookWolk(ServerLevel sl, Vec3 v) {
        rook.add(new Rook(v, sl.getGameTime() + ROOK_TICKS));
        sl.playSound(null, v.x, v.y, v.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.7f);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, v.x, v.y + 0.8, v.z, 20, 1.2, 0.5, 1.2, 0.01);
    }

    public boolean inRook(Vec3 v) {
        for (Rook r : rook) {
            if (r.plek().distanceToSqr(v.x, r.plek().y, v.z) < ROOK_STRAAL * ROOK_STRAAL && Math.abs(v.y - r.plek().y) < 3) {
                return true;
            }
        }
        return false;
    }

    // ---- the rotation -----------------------------------------------------------------------------------------------------

    @Override
    protected void opFase(int fase) {
        kaart("vadsrijk.z3imker.fase" + fase, fase == 2 ? GRAUW : HONING);
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (fase == 2) {
            spreek("vadsrijk.z3imker.zeg.fase2");
            grauw = 0;
            vuurT = KORF_VUUR;
            naarStok(sl, getRandom().nextInt(STOKKEN.length));
        } else if (fase == 3) {
            spreek("vadsrijk.z3imker.zeg.fase3");
            zetGiet(false);
            stok = -1;
            grauw = 0;
            zSpring(zMidden(), 20, 4);
            bomCd = 100;
            zwermCd = 40;
        }
    }

    /** Phase 2: jump onto perch i and start pouring. */
    public void naarStok(ServerLevel sl, int i) {
        stok = i;
        Vec3 s = stokPlek(i);
        speel("sprong");
        zSpring(s, 20, 5);
        zetGiet(true);
        zeg("vadsrijk.z3imker.giet", ChatFormatting.GRAY);
        for (ServerPlayer p : strijders()) {
            Tips.toon(p, "vadsrijk.z3imker.tip.korf");
        }
    }

    @Override
    protected boolean bezig() {
        return giet();
    }

    @Override
    protected boolean immuun() {
        return fase() == 2 && giet() && !Statussen.heeft(this, Status.VERDOOFD);
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z3imker.immuun";
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        return fase() == 2 && Statussen.heeft(this, Status.VERDOOFD) ? 1.25 : 1;
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (testStil) {
            rustT = 20;
            return;
        }
        if (fase() == 2) {
            if (klodderCd <= 0) {   // he keeps lobbing from his perch, slower
                klodder(sl, l);
                klodderCd = 110;
                rustT = 30;
                return;
            }
            rustT = 10;
            return;
        }
        boolean drie = fase() >= 3;
        if (drie && bomCd <= 0) {
            groteBom(sl);
            bomCd = 400;
            rustT = 50;
            return;
        }
        if (zwermCd <= 0 && zwermen.size() < (drie ? 2 : 1)) {
            ServerPlayer p = l.get(getRandom().nextInt(l.size()));
            zwerm(sl, p);
            zwermCd = drie ? 200 : 220;
            rustT = 25;
            return;
        }
        if (pufCd <= 0) {
            ServerPlayer p = zDoel() != null ? zDoel() : l.get(getRandom().nextInt(l.size()));
            puf(sl, p);
            pufCd = drie ? 140 : 160;
            rustT = 35;
            return;
        }
        if (klodderCd <= 0) {
            klodder(sl, l);
            klodderCd = drie ? 70 : 85;
            rustT = 25;
            return;
        }
        rustT = 10;
    }

    @Override
    protected void bijt(ServerLevel sl, ServerPlayer d) {
        if (!testStil) {
            speel("slag");
            super.bijt(sl, d);
        }
    }

    /** Grey honey lob: a red circle, then a sticky puddle stays there. */
    public void klodder(ServerLevel sl, List<ServerPlayer> l) {
        ServerPlayer v = iemandOpAfstand() ? afstandsDoel() : l.get(getRandom().nextInt(l.size()));
        if (v == null) {
            return;
        }
        speel("worp");
        Vec3 c = voetVan(v);
        inslag(c, 2.5, 30, 0.16, "klodder", Status.PLAKKERIG, new DustParticleOptions(GRAUW, 1.8f));
        later(tg(30), () -> {
            plassen.add(new Plas(c, sl.getGameTime() + PLAS_TICKS));
            while (plassen.size() > MAX_PLASSEN) {
                plassen.remove(0);
            }
        });
        laatsteAanval = "klodder";
    }

    /** A puff of cheese smoke in a strip in front of him (dodge sideways). */
    public void puf(ServerLevel sl, ServerPlayer p) {
        Vec3 a = position();
        Vec3 r = p.position().subtract(a).multiply(1, 0, 1);
        r = r.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : r.normalize();
        Vec3 b = a.add(r.scale(7));
        int w = tg(25);
        Telegraaf.lijn(sl, a, b, 3, w, ROOD);
        speel("puf");
        zeg("vadsrijk.z3imker.puf", ChatFormatting.RED);
        Vec3 rr = r;
        later(w, () -> {
            for (double s = 0.5; s < 7; s += 0.7) {
                Vec3 v = a.add(rr.scale(s));
                sl.sendParticles(new DustParticleOptions(GRAUW, 2.2f), v.x, v.y + 1, v.z, 6, 0.6, 0.4, 0.6, 0);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, v.x, v.y + 1, v.z, 2, 0.4, 0.3, 0.4, 0.01);
            }
            sl.playSound(null, a.x, a.y, a.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1f, 0.6f);
            for (ServerPlayer q : strijders()) {
                if (inStrook(q.position(), a, b, 3) && Math.abs(q.getY() - a.y) < 3) {
                    mech(q, 0.2, "puf");
                    Statussen.geef(q, Status.DUIZELIG, 50, 0.4, this);
                }
            }
        });
        laatsteAanval = "puf";
    }

    /** An angry swarm out of the hive after one player. */
    public void zwerm(ServerLevel sl, ServerPlayer p) {
        Vec3 d = dir(richting);
        Vec3 start = korfPlek().add(d.x, 1.2, d.z);
        zwermen.add(new Zwerm(start, p.getUUID(), ZWERM_TICKS));
        sl.playSound(null, start.x, start.y, start.z, SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 1.5f, 1.0f);
        zeg("vadsrijk.z3imker.zwerm", ChatFormatting.GOLD, p.getDisplayName());
        Tips.toon(p, "vadsrijk.z3imker.tip.rook");
        laatsteAanval = "zwerm";
    }

    /** Phase 3: the big cheese-smoke bomb: share it in the yellow circle at the hive. */
    public void groteBom(ServerLevel sl) {
        bommen++;
        speel("worp");
        zeg("vadsrijk.z3imker.bom", ChatFormatting.YELLOW);
        int nodig = Math.min(2, Math.max(1, strijders().size()));
        stapel(korfPlek(), 3.2, 70, 0.5, 0.4, nodig, "grote_bom", ok -> {
            Vec3 c = korfPlek();
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 1, c.z, 60, 5, 1, 5, 0.02);
            sl.sendParticles(new DustParticleOptions(GRAUW, 2.5f), c.x, c.y + 1, c.z, 80, 6, 1, 6, 0);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 0.8f);
            zeg(ok ? "vadsrijk.z3imker.bom.ok" : "vadsrijk.z3imker.bom.mis", ok ? ChatFormatting.GREEN : ChatFormatting.RED);
        });
        laatsteAanval = "bom";
    }

    // ---- phase 2: the hive's stream ---------------------------------------------------------------------------------------

    /** The hive shoots its bees out of its opening now (after the red lane): players in the lane are hit, he may be stung. */
    public void korfVuur(ServerLevel sl, int waarschuw) {
        Vec3 a = korfPlek().add(0, 0.2, 0);
        Vec3 d = dir(richting);
        Vec3 b = a.add(d.scale(LAAN));
        Telegraaf.lijn(sl, a, b, LAAN_BREED, waarschuw, ROOD);
        if (korf != null) {
            korf.speel("klop");
        }
        later(waarschuw, () -> {
            for (double s = 1; s < LAAN; s += 0.8) {
                Vec3 v = a.add(d.scale(s));
                sl.sendParticles(new DustParticleOptions(HONING, 1.4f), v.x, v.y + 1, v.z, 5, 0.6, 0.4, 0.6, 0);
                sl.sendParticles(new DustParticleOptions(0x2A2018, 1.0f), v.x, v.y + 1.1, v.z, 3, 0.6, 0.4, 0.6, 0);
            }
            sl.playSound(null, a.x, a.y, a.z, SoundEvents.BEE_STING, SoundSource.HOSTILE, 1.5f, 0.9f);
            for (ServerPlayer q : strijders()) {
                if (inStrook(q.position(), a, b, LAAN_BREED) && Math.abs(q.getY() - a.y) < 3) {
                    mech(q, 0.22, "korfstroom");
                    Statussen.geef(q, Status.PLAKKERIG, 40, 0.4, this);
                }
            }
            if (giet() && inStrook(position(), a, b, LAAN_BREED + 1)) {
                steek(sl);
            }
        });
    }

    /** The stream stung him: off the perch, stunned, the meter drops. */
    public void steek(ServerLevel sl) {
        gestoken++;
        Statussen.geef(this, Status.VERDOOFD, STEEK_TICKS, 1, null);
        grauw = Math.max(0, grauw - GRAUW_VOL * 35 / 100);
        spreek("vadsrijk.z3imker.zeg.au");
        zeg("vadsrijk.z3imker.gestoken", ChatFormatting.GREEN);
        sl.sendParticles(new DustParticleOptions(HONING, 2f), getX(), getY() + 1.4, getZ(), 40, 0.8, 0.8, 0.8, 0);
        sl.playSound(null, blockPosition(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.HOSTILE, 1.5f, 1.4f);
        zetGiet(false);
        rustNaSteek = 40;   // (after the stun: 2 s on the floor before he climbs a perch again)
        krijgSchade(maxVads() * 0.06 / schadeFactor(null), null);
    }

    // ---- the tick -----------------------------------------------------------------------------------------------------------

    @Override
    protected boolean vechtZonderStrijders() {
        return true;   // (the room's objects stand there as soon as he does; the timers wait on fighters below)
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        zorgVoorSpullen(sl);
        long nu = sl.getGameTime();
        rook.removeIf(r -> r.tot() <= nu);
        plassen.removeIf(p -> p.tot() <= nu);
        // smoke + puddles look
        if (tickCount % 5 == 0) {
            for (Rook r : rook) {
                Vec3 v = r.plek();
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, v.x, v.y + 0.5, v.z, 3, ROOK_STRAAL * 0.5, 0.4, ROOK_STRAAL * 0.5, 0.005);
                sl.sendParticles(new DustParticleOptions(ROOK, 2.4f), v.x, v.y + 1, v.z, 8, ROOK_STRAAL * 0.5, 0.6, ROOK_STRAAL * 0.5, 0);
            }
            for (Plas p : plassen) {
                Vec3 v = p.plek();
                sl.sendParticles(new DustParticleOptions(GRAUW, 1.5f), v.x, v.y + 0.1, v.z, 8, PLAS_STRAAL * 0.5, 0.02, PLAS_STRAAL * 0.5, 0);
                sl.sendParticles(ParticleTypes.DRIPPING_HONEY, v.x, v.y + 0.2, v.z, 1, PLAS_STRAAL * 0.4, 0.02, PLAS_STRAAL * 0.4, 0);
            }
        }
        if (l.isEmpty()) {
            return;
        }
        klodderCd--;
        pufCd--;
        zwermCd--;
        bomCd--;
        // puddles: sticky + a small sting every second
        if (tickCount % 20 == 0) {
            for (Plas p : plassen) {
                for (ServerPlayer q : inCirkel(p.plek(), PLAS_STRAAL)) {
                    Statussen.geef(q, Status.PLAKKERIG, 30, 0.45, this);
                    mech(q, 0.03, "plas");
                }
            }
        }
        zwermTick(sl);
        if (fase() == 2) {
            fase2Tick(sl);
        }
    }

    private void zwermTick(ServerLevel sl) {
        double snel = fase() >= 3 ? 0.2 : 0.16;
        long nu = sl.getGameTime();
        for (Iterator<Zwerm> it = zwermen.iterator(); it.hasNext(); ) {
            Zwerm z = it.next();
            z.leven--;
            ServerPlayer d = sl.getEntity(z.doel) instanceof ServerPlayer p && strijders().contains(p) ? p : null;
            if (d == null && !strijders().isEmpty()) {
                d = strijders().get(0);
                z.doel = d.getUUID();
            }
            if (d != null) {
                Vec3 naar = d.position().add(0, 1, 0).subtract(z.plek);
                if (naar.lengthSqr() > 0.01) {
                    z.plek = z.plek.add(naar.normalize().scale(Math.min(snel, naar.length())));
                }
            }
            if (tickCount % 2 == 0) {
                sl.sendParticles(new DustParticleOptions(0x2A2018, 0.9f), z.plek.x, z.plek.y, z.plek.z, 6, 0.5, 0.4, 0.5, 0);
                sl.sendParticles(new DustParticleOptions(HONING, 0.9f), z.plek.x, z.plek.y, z.plek.z, 6, 0.5, 0.4, 0.5, 0);
            }
            if (tickCount % 30 == 0) {
                sl.playSound(null, z.plek.x, z.plek.y, z.plek.z, SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 0.8f, 1.1f);
            }
            if (inRook(z.plek)) {
                gesust++;
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, z.plek.x, z.plek.y, z.plek.z, 10, 0.4, 0.4, 0.4, 0.01);
                sl.sendParticles(new DustParticleOptions(HONING, 1.2f), z.plek.x, z.plek.y - 0.5, z.plek.z, 16, 0.5, 0.2, 0.5, 0);
                sl.playSound(null, z.plek.x, z.plek.y, z.plek.z, SoundEvents.BEE_LOOP, SoundSource.HOSTILE, 1f, 0.6f);
                zeg("vadsrijk.z3imker.zwerm.suf", ChatFormatting.GREEN);
                it.remove();
                continue;
            }
            for (ServerPlayer q : strijders()) {
                if (q.position().add(0, 1, 0).distanceToSqr(z.plek) < ZWERM_RAAK * ZWERM_RAAK && nu >= z.gestoken.getOrDefault(q.getUUID(), 0L)
                        && !inRook(q.position())) {
                    z.gestoken.put(q.getUUID(), nu + 15);
                    mech(q, 0.1, "zwerm");
                }
            }
            if (z.leven <= 0) {
                sl.sendParticles(new DustParticleOptions(HONING, 1.0f), z.plek.x, z.plek.y, z.plek.z, 20, 0.6, 0.4, 0.6, 0.02);
                it.remove();
            }
        }
    }

    private void fase2Tick(ServerLevel sl) {
        if (Statussen.heeft(this, Status.VERDOOFD)) {
            return;
        }
        if (!giet()) {
            if (rustNaSteek > 0 && --rustNaSteek > 0) {
                return;
            }
            int nieuw = (stok + 1 + getRandom().nextInt(STOKKEN.length - 1)) % STOKKEN.length;   // another perch
            naarStok(sl, nieuw);
            vuurT = KORF_VUUR;
            return;
        }
        if (zInSprong()) {
            return;
        }
        getNavigation().stop();
        Vec3 k = korfPlek();
        getLookControl().setLookAt(k.x, k.y + 1, k.z);
        grauw++;
        if (tickCount % 40 == 0) {
            speel("giet");
        }
        if (tickCount % 4 == 0) {   // the pour: a grey thread from him to the hive
            Vec3 a = position().add(0, 1.2, 0), b = k.add(0, 1.4, 0);
            for (int i = 1; i < 8; i++) {
                Vec3 v = a.lerp(b, i / 8.0);
                sl.sendParticles(new DustParticleOptions(GRAUW, 1.1f), v.x, v.y, v.z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
        if (--vuurT <= 0) {
            vuurT = KORF_VUUR;
            korfVuur(sl, tg(KORF_WAARSCHUW));
        }
        if (grauw >= GRAUW_VOL) {
            grauw = GRAUW_VOL * 40 / 100;
            kaart("vadsrijk.z3imker.grauw", GRAUW);
            Vec3 c = korfPlek();
            sl.sendParticles(new DustParticleOptions(GRAUW, 2.6f), c.x, c.y + 1, c.z, 120, 7, 1.5, 7, 0);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.BEEHIVE_EXIT, SoundSource.HOSTILE, 2f, 0.5f);
            for (ServerPlayer q : strijders()) {
                mech(q, 0.35, "grauwe_koningin");
                Statussen.geef(q, Status.DUIZELIG, 60, 0.4, this);
            }
        }
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd() && level() instanceof ServerLevel sl) {
            spreek("vadsrijk.z3imker.zeg.vlucht");
            zetGiet(false);
            if (korf != null) {
                korf.zetStand(0);
                Vec3 c = korfPlek();
                sl.sendParticles(new DustParticleOptions(HONING, 2.2f), c.x, c.y + 1.2, c.z, 80, 1.2, 1.2, 1.2, 0.02);
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 2f, 1.3f);
            }
            ImkerVerhaal.verslagen(this, sl);
            zwermen.clear();
            plassen.clear();
            rook.clear();
            Z3Doel k = korf;
            List<Z3Doel> pp = new ArrayList<>(potten);
            nl.juiced.guhs.feature.vadsrijk.wereld.Later.na(20 * 12, () -> {
                if (k != null) {
                    k.discard();
                }
                pp.forEach(Z3Doel::discard);
            });
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!geplofd()) {
            if (korf != null) {
                korf.discard();
            }
            potten.forEach(Z3Doel::discard);
        }
        super.remove(reason);
    }

    @Override
    protected LootTabel.Resultaat kistInhoud(ServerPlayer p, boolean eersteKill) {
        LootTabel.Resultaat r = Z3BazenLoot.kist(baasId(), p, Math.min(50, mobLevel()), getRandom(), false, false);
        List<ItemStack> items = new ArrayList<>(r.items());
        if (eersteKill || getRandom().nextDouble() < 0.25) {
            items.add(new ItemStack(ImkerRegistry.TROFEE.get()));
        }
        if (eersteKill) {
            items.add(new ItemStack(net.minecraft.world.item.Items.HONEY_BOTTLE, 4));
            items.add(new ItemStack(net.minecraft.world.item.Items.HONEYCOMB, 8));
        }
        return new LootTabel.Resultaat(items, r.munten());
    }
}
