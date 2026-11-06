package nl.juiced.guhs.feature.vadsrijk.bazen.mika;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenBaas;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenEffecten;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenMuziek;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenPlekken;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.ProefKlodder;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * De Mika-spion, the secret boss (ronde 7): an evil Mika disguised as "Akim de Kaasguh" at the Zwamzoet market.
 * The npc slice runs the clue quest and calls {@link #ontmasker(ServerPlayer)} (or sets {@link #MAG_ONTMASKEREN} so a
 * right-click on Akim does it). Then: the reveal scene (the disguise falls off, he leaps up the mushroom caps), and a rooftop
 * fight over 3 caps (one per phase; the guhs of Zwamzoet shoot you after him with a spore catapult): smoke-bomb blinks,
 * ontvadsings-poeder (grey circles: Gesporen), grey kloddertjes, and at 15 % he tries to float away on a balloon: pop it in
 * {@link SpionBallon#ONTSNAP_TICKS} or he escapes (and is back at the market in disguise 2 min later).
 * Popped: he falls, is caught in a knuffelnet (no gore), personal chests on the cap, and a cliffhanger about the other Mika's.
 * He comes back in disguise {@link BazenPlekken#MIKA_TERUG_SECONDEN} later so others can do it too.
 */
public class MikaSpion extends BazenBaas {
    public static final int VERMOMD = 0, VLUCHT = 1, VECHT = 2, BALLON = 3;
    private static final EntityDataAccessor<Integer> TOESTAND = SynchedEntityData.defineId(MikaSpion.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ONTHULD = SynchedEntityData.defineId(MikaSpion.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle"), WALK = RawAnimation.begin().thenLoop("animation.guh.walk");

    /** npc slice: when true, right-clicking Akim unmasks him (default: never; the quest calls ontmasker itself). */
    public static Predicate<ServerPlayer> MAG_ONTMASKEREN = p -> false;

    // the one spy (shared world)
    @Nullable
    private static UUID huidig;
    private static long terugOp;

    private int hoed, sceneT, blinkT = 160, poederT = 100, klodderT = 300, leegT, lokT = 200, kaasT = 260;
    /** Cheese wheels rolling over the cap: start x, z, direction x, z, length, tick it starts rolling. */
    private final List<double[]> kaas = new ArrayList<>();
    private final List<UUID> lokken = new ArrayList<>();
    private final Set<UUID> kijkers = new HashSet<>();
    private final List<double[]> poeder = new ArrayList<>();   // x, y, z, tick
    @Nullable
    private UUID ballon;
    private boolean vangen;
    private Vec3 vluchtVan = Vec3.ZERO;

    public MikaSpion(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(TOESTAND, VERMOMD);
        b.define(ONTHULD, false);
    }

    /** Kern's loose boss (no Arena): bar, HP scaling and heal contribution for players within 48 while he fights. */
    @Override
    public int losseStraal() {
        int t = toestand();
        return t == VECHT || t == BALLON ? 48 : 0;
    }

    public int toestand() {
        return entityData.get(TOESTAND);
    }

    public boolean onthuld() {
        return entityData.get(ONTHULD);
    }

    @Override
    public String baasId() {
        return "mika_spion";
    }

    @Override
    protected double basisVads(int level) {
        return 2400 + 150 * level;
    }

    @Override
    protected double basisSchade(int level) {
        return 10 + 2 * level;
    }

    @Override
    public int xp() {
        return 900 + 80 * mobLevel();
    }

    @Override
    public Element standaardElement() {
        return Element.GEEN;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.7, 0.4, 0.15};
    }

    @Override
    public boolean stil() {
        return super.stil() || toestand() != VECHT;
    }

    @Override
    protected Vec3 kistMidden() {
        return hoedTop((ServerLevel) level(), hoed);
    }

    // ---- the caps -----------------------------------------------------------------------------------------------------------

    /** The top of cap i (heightmap; the planned height when the city isn't there yet). */
    public static Vec3 hoedTop(ServerLevel sl, int i) {
        int[] h = BazenPlekken.MIKA_HOEDEN[Math.max(0, Math.min(BazenPlekken.MIKA_HOEDEN.length - 1, i))];
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, h[0], h[1]);
        if (y < h[2] - 25) {
            y = h[2];
        }
        return new Vec3(h[0] + 0.5, y, h[1] + 0.5);
    }

    private Arena arenaVan(ServerLevel sl, int i) {
        return new Arena("bazen:mika_spion", BlockPos.containing(hoedTop(sl, i)), BazenPlekken.MIKA_HOED_STRAAL + 6, BazenPlekken.MIKA_KRAAM, 0,
                BazenPlekken.MIKA_KRAAM, titelKey());
    }

    List<ServerPlayer> vechters(ServerLevel sl) {
        Vec3 c = hoedTop(sl, hoed);
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.position().distanceToSqr(c) < 48 * 48) {
                l.add(p);
            }
        }
        return l;
    }

    private void zeg(ServerLevel sl, String key, ChatFormatting k) {
        for (ServerPlayer p : vechters(sl)) {
            p.sendSystemMessage(Component.translatable(key).withStyle(k));
        }
    }

    // ---- disguised ----------------------------------------------------------------------------------------------------------

    @Override
    public void naamplaatje() {
        if (level().isClientSide()) {
            return;
        }
        if (toestand() == VERMOMD) {
            setCustomName(Component.translatable("vadsrijk.bazen.mika_vermomd").withStyle(ChatFormatting.LIGHT_PURPLE));
            setCustomNameVisible(true);
            return;
        }
        super.naamplaatje();
        if (toestand() == VLUCHT) {
            setCustomNameVisible(false);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (toestand() != VERMOMD || !(player instanceof ServerPlayer p) || hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }
        if (MAG_ONTMASKEREN.test(p)) {
            begin(p);
        } else {
            p.sendSystemMessage(Component.translatable("vadsrijk.bazen.mika_vermomd").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable("vadsrijk.bazen.mika.praat." + getRandom().nextInt(5)).withStyle(ChatFormatting.WHITE)));
            lookAt(p, 30, 30);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (toestand() == VERMOMD) {
            if (source.getEntity() instanceof ServerPlayer p) {
                p.sendOverlayMessage(Component.translatable("vadsrijk.bazen.mika.au").withStyle(ChatFormatting.GRAY));
            }
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    /** The spy in this world (spawning him disguised at the market stall when needed). */
    @Nullable
    public static MikaSpion zoek(ServerLevel sl, boolean maak) {
        if (huidig != null && sl.getEntity(huidig) instanceof MikaSpion m && !m.isRemoved() && !m.geplofd()) {
            return m;
        }
        Vec3 k = Vec3.atBottomCenterOf(BazenPlekken.MIKA_KRAAM);
        for (MikaSpion m : sl.getEntitiesOfClass(MikaSpion.class, new net.minecraft.world.phys.AABB(BlockPos.containing(k)).inflate(200, 160, 200),
                x -> !x.geplofd())) {
            huidig = m.getUUID();
            return nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(m, "baas:mika_spion");   // U-DUBBEL
        }
        if (!maak || !nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.klaar(sl, BazenPlekken.MIKA_KRAAM)) {   // U-DUBBEL (was isLoaded)
            return null;
        }
        MikaSpion m = BazenRegistry.MIKA_SPION.get().create(sl, EntitySpawnReason.SPAWNER);
        if (m == null) {
            return null;
        }
        m.stelIn(15, Element.GEEN, false, Set.of());
        m.zetOntvadst(false);
        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(m, "baas:mika_spion");   // U-DUBBEL
        m.snapTo(k.x, k.y, k.z, 180, 0);
        sl.addFreshEntity(m);
        huidig = m.getUUID();
        return m;
    }

    /** The npc slice's entry point: the reveal scene + the rooftop fight. False when he can't be found right now. */
    public static boolean ontmasker(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return false;
        }
        MikaSpion m = zoek(sl, true);
        if (m == null || m.toestand() != VERMOMD) {
            return false;
        }
        m.begin(p);
        return true;
    }

    /** Every 20 ticks: put Akim back at his stall when he's gone and his time-out is over. */
    public static void beheer(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || sl.getGameTime() % 20 != 0 || sl.getGameTime() < terugOp || !sl.isLoaded(BazenPlekken.MIKA_KRAAM)) {
            return;
        }
        if (sl.players().stream().noneMatch(p -> p.position().distanceToSqr(Vec3.atCenterOf(BazenPlekken.MIKA_KRAAM)) < 160 * 160)) {
            return;
        }
        zoek(sl, true);
    }

    // ---- the reveal ---------------------------------------------------------------------------------------------------------

    private void begin(ServerPlayer ontmaskeraar) {
        ServerLevel sl = (ServerLevel) level();
        entityData.set(TOESTAND, VLUCHT);
        sceneT = 0;
        hoed = 0;
        vluchtVan = position();
        kijkers.clear();
        bijdragen.wis();
        Vec3 doel = hoedTop(sl, 0);
        Vec3 oog = position().add(0, 1, 0);
        Vec3 voor = Vec3.directionFromRotation(0, getYRot()).multiply(1, 0, 1).normalize();
        RijkScene s = RijkScene.maak("mika_spion", 160)
                .camera(0, oog.add(voor.scale(3.2)).add(0, 0.3, 0), oog)
                .camera(40, oog.add(voor.scale(2.6)).add(0, 0.2, 0), oog)
                .cameraVolgt(70, oog.add(voor.scale(8)).add(0, 4, 0), getId())
                .cameraVolgt(120, vluchtVan.add(doel).scale(0.5).add(voor.scale(14)).add(0, 12, 0), getId())
                .cameraVolgt(160, doel.add(voor.scale(12)).add(0, 6, 0), getId())
                .titel(titelKey(), ondertitelKey(), 100, 55, 0x8A2BE2)
                .bouw();
        for (ServerPlayer p : sl.players()) {
            if (p == ontmaskeraar || p.position().distanceToSqr(position()) < 24 * 24) {
                kijkers.add(p.getUUID());
                RijkScenes.speel(p, s);
                BazenMuziek.start(p, baasId());
            }
        }
        lookAt(ontmaskeraar, 180, 180);
        naamplaatje();
    }

    private void vluchtTick(ServerLevel sl) {
        sceneT++;
        Vec3 doel = hoedTop(sl, 0);
        if (sceneT == 30) {
            entityData.set(ONTHULD, true);
            BazenEffecten.poef(sl, position());
            sl.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_WOLOLO, SoundSource.HOSTILE, 1.5f, 1.2f);
            for (UUID id : kijkers) {
                if (sl.getEntity(id) instanceof ServerPlayer p) {
                    p.sendSystemMessage(Component.translatable("vadsrijk.bazen.mika_spion").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                            .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                            .append(Component.translatable("vadsrijk.bazen.mika.ontmaskerd").withStyle(ChatFormatting.WHITE)));
                }
            }
            naamplaatje();
        }
        if (sceneT >= 50 && sceneT <= 110) {
            double t = (sceneT - 50) / 60.0;
            Vec3 p = vluchtVan.lerp(doel, t).add(0, Math.sin(t * Math.PI) * 18, 0);
            setNoGravity(true);
            setPos(p.x, p.y, p.z);
            setDeltaMovement(Vec3.ZERO);
            if (sceneT % 4 == 0) {
                sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 4, 0.2, 0.2, 0.2, 0.01);
            }
        }
        if (sceneT == 111) {
            setNoGravity(false);
            BazenEffecten.poef(sl, position());
        }
        if (sceneT >= 160) {
            entityData.set(TOESTAND, VECHT);
            zetArena(arenaVan(sl, 0));
            for (UUID id : kijkers) {
                if (sl.getEntity(id) instanceof ServerPlayer p) {
                    katapult(sl, p, doel);
                    Tips.toon(p, "vadsrijk.bazen.tip.mika_spion");
                }
            }
            naamplaatje();
        }
    }

    /** The guhs' spore catapult: onto the edge of the cap. */
    private void katapult(ServerLevel sl, ServerPlayer p, Vec3 hoedMidden) {
        Vec3 r = p.position().subtract(hoedMidden).multiply(1, 0, 1);
        r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
        Vec3 plek = hoedMidden.add(r.scale(BazenPlekken.MIKA_HOED_STRAAL - 2));
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(plek.x), (int) Math.floor(plek.z));
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 1, 0.5, 0.02);
        p.teleportTo(sl, plek.x, Math.max(y, (int) hoedMidden.y) + 0.1, plek.z, Set.of(), p.getYRot(), p.getXRot(), true);
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 1, 0.5, 0.02);
        sl.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1f, 1.2f);
    }

    // ---- the fight ----------------------------------------------------------------------------------------------------------

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        sl.playSound(null, blockPosition(), SoundEvents.EVOKER_CELEBRATE, SoundSource.HOSTILE, 1.5f, 1.0f);
        if (fase == 2 || fase == 3) {
            naarHoed(sl, fase - 1);
        } else if (fase == 4) {
            ballonOp(sl);
        }
    }

    private void naarHoed(ServerLevel sl, int i) {
        Vec3 oud = hoedTop(sl, hoed);
        List<ServerPlayer> mee = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.position().distanceToSqr(oud) < 60 * 60 || bijdragen.spelers().contains(p.getUUID())) {
                mee.add(p);
            }
        }
        BazenEffecten.poef(sl, position());
        hoed = i;
        Vec3 n = hoedTop(sl, i);
        snapTo(n.x, n.y + 0.2, n.z, getYRot(), 0);
        BazenEffecten.poef(sl, position());
        zetArena(arenaVan(sl, i));
        for (ServerPlayer p : mee) {
            p.sendSystemMessage(Component.translatable("vadsrijk.bazen.mika.kap").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        BazenRegistry.later(sl.getServer(), 60, () -> {
            for (ServerPlayer p : mee) {
                if (!p.isRemoved() && p.level() == sl) {
                    katapult(sl, p, n);
                }
            }
        });
    }

    private void ballonOp(ServerLevel sl) {
        SpionBallon b = BazenRegistry.SPION_BALLON.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (b == null) {
            return;
        }
        b.stelIn(mobLevel(), Element.GEEN, false, Set.of());
        b.koppel(this, maxVads() * 0.06);
        b.snapTo(getX(), getY() + 3.0, getZ(), 0, 0);
        sl.addFreshEntity(b);
        ballon = b.getUUID();
        entityData.set(TOESTAND, BALLON);
        setNoGravity(true);
        sl.playSound(null, blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.HOSTILE, 1.5f, 0.6f);
        for (ServerPlayer p : vechters(sl)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.bazen.mika.ballon").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    /** The balloon popped: he falls and gets caught. */
    void ballonKapot(@Nullable LivingEntity wie) {
        ballon = null;
        setNoGravity(false);
        entityData.set(TOESTAND, VECHT);
        if (level() instanceof ServerLevel sl) {
            BazenRegistry.later(sl.getServer(), 30, () -> vang(wie));
        }
    }

    void vang(@Nullable LivingEntity wie) {
        if (geplofd() || isRemoved()) {
            return;
        }
        vangen = true;
        super.krijgSchade(vads() + 1, wie);
        vangen = false;
    }

    /** The balloon got away (or everybody left): back to the market in disguise later. */
    public void ontsnapt() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        for (ServerPlayer p : vechters(sl)) {
            p.sendSystemMessage(Component.translatable("vadsrijk.bazen.mika.ontsnapt").withStyle(ChatFormatting.GRAY));
        }
        stopBalk(sl, true);
        BazenEffecten.poef(sl, position());
        terugOp = sl.getGameTime() + 20 * 120;
        huidig = null;
        discard();
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        if (vangen || toestand() == VECHT) {
            super.krijgSchade(n, bron);
        }
    }

    @Override
    public boolean isPickable() {
        return toestand() != VLUCHT && super.isPickable();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (geplofd()) {
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);   // caught in the net: no happy hopping for a villain
            return;
        }
        switch (toestand()) {
            case VERMOMD -> {
                setDeltaMovement(0, getDeltaMovement().y, 0);
                if (tickCount % 60 == 0) {
                    Player p = sl.getNearestPlayer(this, 6);
                    if (p != null) {
                        lookAt(p, 30, 30);
                    }
                }
            }
            case VLUCHT -> vluchtTick(sl);
            case VECHT -> vechtTick(sl);
            case BALLON -> {
                if (ballon != null && sl.getEntity(ballon) instanceof SpionBallon b && !b.isRemoved()) {
                    setPos(b.getX(), b.getY() - 3.0, b.getZ());
                    setDeltaMovement(Vec3.ZERO);
                } else if (ballon != null) {
                    ballonKapot(null);
                }
            }
            default -> {
            }
        }
    }

    private void vechtTick(ServerLevel sl) {
        Vec3 c = hoedTop(sl, hoed);
        List<ServerPlayer> l = vechters(sl);
        if (l.isEmpty()) {
            if (++leegT > 1200) {
                ontsnapt();
            }
            return;
        }
        leegT = 0;
        if (getY() < c.y - 6) {   // fell off his cap: smoke bomb back up
            BazenEffecten.poef(sl, position());
            snapTo(c.x, c.y + 0.2, c.z, getYRot(), 0);
            BazenEffecten.poef(sl, position());
        }
        if (--blinkT <= 0) {
            blinkT = 150 + getRandom().nextInt(60);
            blink(sl, c);
        }
        lokEnKaas(sl, c, l);
        if (--poederT <= 0) {
            poederT = 100 + getRandom().nextInt(30);
            ServerPlayer ver = afstandsSpeler();   // (fix round 1: who keeps away gets the powder first)
            ServerPlayer p = ver != null ? ver : l.get(getRandom().nextInt(l.size()));
            Vec3 v = new Vec3(p.getX(), Math.floor(p.getY()), p.getZ());
            Telegraaf.cirkel(sl, v, 3, 26, Telegraaf.GRIJS);
            poeder.add(new double[] {v.x, v.y, v.z, sl.getGameTime() + 26});
            swing(InteractionHand.MAIN_HAND);
            sl.playSound(null, blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 1f, 0.6f);
        }
        for (Iterator<double[]> it = poeder.iterator(); it.hasNext(); ) {
            double[] d = it.next();
            if (sl.getGameTime() < d[3]) {
                continue;
            }
            it.remove();
            sl.sendParticles(ParticleTypes.ASH, d[0], d[1] + 0.5, d[2], 80, 1.5, 0.6, 1.5, 0.02);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, d[0], d[1] + 0.3, d[2], 20, 1.2, 0.3, 1.2, 0.01);
            for (ServerPlayer p : l) {
                double dx = p.getX() - d[0], dz = p.getZ() - d[2];
                if (dx * dx + dz * dz <= 9 && Math.abs(p.getY() - d[1]) < 3) {
                    Schade.doe(new Schade.Klap(this, p, basisSchade(mobLevel()), Element.GEEN, Schade.Soort.MAGIE, false, "bazen_ontvadsingspoeder"));
                    Statussen.geef(p, Status.GESPOREN, 120, mobLevel() * 0.6, this);
                }
            }
        }
        if (--klodderT <= 0) {
            klodderT = 400;
            int levend = sl.getEntitiesOfClass(ProefKlodder.class, getBoundingBox().inflate(30), m -> m.klein() && !m.geplofd()).size();
            for (int i = 0; i < 3 && levend + i < 6; i++) {
                ProefKlodder k = KernRegistry.PROEFKLODDERTJE.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
                if (k == null) {
                    break;
                }
                double a = Math.PI * 2 * i / 3;
                k.snapTo(getX(), getY() + 1, getZ(), 0, 0);
                k.stelIn(Math.max(1, mobLevel() - 2), Element.WATER, false, Set.of());
                k.setDeltaMovement(Math.cos(a) * 0.5, 0.5, Math.sin(a) * 0.5);
                sl.addFreshEntity(k);
            }
            sl.playSound(null, blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1f, 1.2f);
        }
    }

    // ---- fix round 1: decoy Mika's and the cheese-wheel avalanche --------------------------------------------------------------

    private void lokEnKaas(ServerLevel sl, Vec3 c, List<ServerPlayer> l) {
        long nu = sl.getGameTime();
        lokken.removeIf(id -> !(sl.getEntity(id) instanceof LokMika lm) || lm.isRemoved());
        if (!lokken.isEmpty() && tickCount % 3 == 0) {   // the real one's moustache glitters
            Vec3 snor = position().add(getLookAngle().multiply(0.45, 0, 0.45)).add(0, getBbHeight() * 0.62, 0);
            sl.sendParticles(ParticleTypes.WAX_ON, snor.x, snor.y, snor.z, 1, 0.12, 0.04, 0.12, 0);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, snor.x, snor.y, snor.z, 1, 0.1, 0.03, 0.1, 0);
        }
        if (fase() >= 2 && lokken.isEmpty() && --lokT <= 0) {
            lokT = 600 + getRandom().nextInt(200);
            lokMikas(sl, c);
        }
        if (--kaasT <= 0) {
            kaasT = (fase() >= 3 ? 300 : 420) + getRandom().nextInt(100);
            kaasLawine(sl, c);
        }
        for (java.util.Iterator<double[]> it = kaas.iterator(); it.hasNext(); ) {
            double[] k = it.next();
            double t = nu - k[5];
            if (t < 0) {
                continue;
            }
            double f = t / 22.0;
            if (f > 1) {
                it.remove();
                continue;
            }
            double x = k[0] + k[2] * k[4] * f, z = k[1] + k[3] * k[4] * f, y = c.y + 0.9;
            var stof = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.HONEYCOMB_BLOCK.defaultBlockState());
            for (int i = 0; i < 10; i++) {   // a rolling wheel: a ring of cheese crumbs standing upright along the lane
                double a = Math.PI * 2 * i / 10 + t * 0.6;
                sl.sendParticles(stof, x + k[2] * Math.cos(a) * 0.9, y + Math.sin(a) * 0.9, z + k[3] * Math.cos(a) * 0.9, 1, 0, 0, 0, 0);
            }
            if (t % 4 == 0) {
                sl.playSound(null, BlockPos.containing(x, y, z), SoundEvents.MINECART_RIDING, SoundSource.HOSTILE, 0.5f, 1.4f);
            }
            for (ServerPlayer p : l) {
                double dx = p.getX() - x, dz = p.getZ() - z;
                if (dx * dx + dz * dz < 1.7 * 1.7 && Math.abs(p.getY() - c.y) < 3 && p.invulnerableTime <= 0) {
                    Schade.doe(new Schade.Klap(this, p, basisSchade(mobLevel()) * 1.3, Element.GEEN, Schade.Soort.MELEE, false, "bazen_kaaswiel"));
                    p.setDeltaMovement(k[2] * 0.9, 0.5, k[3] * 0.9);
                    p.hurtMarked = true;
                    p.invulnerableTime = 12;
                }
            }
        }
    }

    /** Three decoys poof up around the cap, he poofs to a new spot among them: find the one with the glittering moustache. */
    private void lokMikas(ServerLevel sl, Vec3 c) {
        for (int i = 0; i < 3; i++) {
            LokMika lm = BazenRegistry.LOK_MIKA.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (lm == null) {
                return;
            }
            lm.stelIn(mobLevel(), Element.GEEN, false, Set.of());
            lm.snapTo(getX(), getY(), getZ(), getYRot(), 0);
            sl.addFreshEntity(lm);
            lm.koppel(this);
            lokken.add(lm.getUUID());
            Vec3 voor = position();
            blinkVan(lm, sl, c);
            BazenEffecten.poef(sl, voor);
        }
        blink(sl, c);
        zeg(sl, "vadsrijk.bazen.mika.lok", ChatFormatting.LIGHT_PURPLE);
    }

    private void blinkVan(LokMika lm, ServerLevel sl, Vec3 c) {
        double a = getRandom().nextDouble() * Math.PI * 2, d = 2 + getRandom().nextDouble() * (BazenPlekken.MIKA_HOED_STRAAL - 3);
        int x = (int) Math.floor(c.x + Math.cos(a) * d), z = (int) Math.floor(c.z + Math.sin(a) * d);
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        lm.snapTo(x + 0.5, Math.abs(y - c.y) <= 4 ? y : c.y + 0.2, z + 0.5, getYRot(), 0);
        BazenEffecten.poef(sl, lm.position());
    }

    /** The cheese-wheel avalanche: red lanes over the cap (gaps between them are safe), then the wheels roll through. */
    private void kaasLawine(ServerLevel sl, Vec3 c) {
        double a = getRandom().nextInt(4) * Math.PI / 2 + Math.PI / 4 * getRandom().nextInt(2);
        double dx = Math.cos(a), dz = Math.sin(a), nx = -dz, nz = dx;
        double r = BazenPlekken.MIKA_HOED_STRAAL + 1;
        int banen = fase() >= 3 ? 3 : 2;
        double start = getRandom().nextBoolean() ? -r * 0.55 : -r * 0.2;
        long op = sl.getGameTime() + 36;
        for (int i = 0; i < banen; i++) {
            double o = start + i * 5.5;
            if (Math.abs(o) > r) {
                continue;
            }
            Vec3 van = new Vec3(c.x - dx * r + nx * o, c.y, c.z - dz * r + nz * o);
            Vec3 naar = new Vec3(c.x + dx * r + nx * o, c.y, c.z + dz * r + nz * o);
            Telegraaf.lijn(sl, van, naar, 3.2, 36, Telegraaf.ROOD);
            kaas.add(new double[] {van.x, van.z, dx, dz, 2 * r, op + i * 4});
        }
        swing(InteractionHand.MAIN_HAND);
        sl.playSound(null, blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.2f, 0.7f);
        zeg(sl, "vadsrijk.bazen.mika.kaas", ChatFormatting.GOLD);
    }

    private void blink(ServerLevel sl, Vec3 c) {
        for (int tries = 0; tries < 10; tries++) {
            double a = getRandom().nextDouble() * Math.PI * 2, d = getRandom().nextDouble() * (BazenPlekken.MIKA_HOED_STRAAL - 2);
            int x = (int) Math.floor(c.x + Math.cos(a) * d), z = (int) Math.floor(c.z + Math.sin(a) * d);
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - c.y) <= 4 || tries == 9) {
                BazenEffecten.poef(sl, position());
                snapTo(x + 0.5, Math.abs(y - c.y) <= 4 ? y : c.y + 0.2, z + 0.5, getYRot(), 0);
                BazenEffecten.poef(sl, position());
                return;
            }
        }
    }

    /** Clears the boss bar (kern's loose-boss bar, contract 13) and optionally the music for everyone near the cap. */
    private void stopBalk(ServerLevel sl, boolean muziek) {
        for (ServerPlayer p : vechters(sl)) {
            KernPayloads.stuur(p, new KernPayloads.BaasBalk(-1, Component.empty(), 0, 0, 0));
            if (muziek) {
                BazenMuziek.stop(p);
            }
        }
    }

    @Override
    protected void opPlof() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        setNoGravity(false);
        sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.5, getZ(), 12, 0.8, 0.5, 0.8, 0);
        for (int i = 0; i < 24; i++) {   // the knuffelnet
            double a = Math.PI * 2 * i / 24;
            sl.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(a) * 1.2, getY() + 0.2 + (i % 3) * 0.6, getZ() + Math.sin(a) * 1.2, 1, 0, 0, 0, 0);
        }
        sl.playSound(null, blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.HOSTILE, 2f, 0.8f);
        sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 1.5f, 0.7f);
        List<ServerPlayer> l = vechters(sl);
        for (ServerPlayer p : l) {
            p.sendSystemMessage(Component.translatable("vadsrijk.bazen.mika.gevangen").withStyle(ChatFormatting.GREEN));
            p.sendSystemMessage(Component.translatable("vadsrijk.bazen.mika_spion").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable("vadsrijk.bazen.mika.cliff").withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC)));
        }
        BazenRegistry.later(sl.getServer(), 60, () -> {
            for (ServerPlayer p : l) {
                RijkScenes.titel(p, Component.translatable("vadsrijk.bazen.mika.cliff_titel").withStyle(ChatFormatting.BOLD),
                        Component.translatable("vadsrijk.bazen.mika.cliff_onder"), 80, 0x8A2BE2);
            }
        });
        stopBalk(sl, false);
        terugOp = sl.getGameTime() + 20L * BazenPlekken.MIKA_TERUG_SECONDEN;
        huidig = null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("beweeg", 3, state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    /** Not saved: after a restart Akim simply stands at his stall again ({@link #beheer}). */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /** Dev/test: start the fight right away (no scene). */
    public void vechtNu(ServerLevel sl) {
        entityData.set(TOESTAND, VECHT);
        entityData.set(ONTHULD, true);
        zetArena(arenaVan(sl, hoed));
        naamplaatje();
    }
}
