package nl.juiced.guhs.feature.vadsrijk.bazen.opperzwam;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenBaas;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenPlekken;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * De Opperzwam, boss of the Zwamtempel (ronde 7), lv 18-20, respawn 5 min.
 * <ul>
 *   <li>Fase 1: roots (pink circles under everyone, then they burst: Plakkerig) and spore clouds that fill the hall except one
 *       pink safe spot (outside it: Gesporen + damage).</li>
 *   <li>Fase 2 (66 %): the temple goes dark (Darkness + client fog, he only shows his glowing eyes) and he hunts with pounces.
 *       Light the 4 Lichtzwammen (right-click): then he is blinded (5 s stunned, takes a big hit). While dark he takes 25 %
 *       damage.</li>
 *   <li>Fase 3 (33 %): the hall "tilts" (a rocking camera scene), he grows gigantic and the ceiling breaks open to the sky.
 *       Every changed block is remembered ({@link Herstel}) and comes back 10 s after the fight, when he is reset/respawned,
 *       and at every server start: the world never stays changed.</li>
 * </ul>
 */
public class Opperzwam extends BazenBaas {
    private static final EntityDataAccessor<Boolean> DONKER = SynchedEntityData.defineId(Opperzwam.class, EntityDataSerializers.BOOLEAN);
    public static final String H_PLAFOND = "oz_plafond", H_LICHT = "oz_licht";
    public static final double REUS = 2.1;

    private enum Doet {
        RUST, WORTELS, SPOREN, MIKT, SPRINGT, STAMP, KANTEL
    }

    private Doet doet = Doet.RUST;
    private int timer = 60, kantelT;
    @Nullable
    private Vec3 spawnPlek;
    private final List<Vec3> plekken = new ArrayList<>();
    private Vec3 veilig = Vec3.ZERO, landing = Vec3.ZERO;
    private boolean opgeruimd;
    private int zet;

    public Opperzwam(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DONKER, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32f));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public String baasId() {
        return "opperzwam";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"stamp", "brul"};
    }

    @Override
    protected double basisVads(int level) {
        return 4000 + 250 * level;
    }

    @Override
    protected double basisSchade(int level) {
        return 12 + 1.8 * level;
    }

    @Override
    public int xp() {
        return 1500 + 100 * mobLevel();
    }

    @Override
    public Element standaardElement() {
        return Element.NATUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.66, 0.33};
    }

    public boolean donker() {
        return entityData.get(DONKER);
    }

    public Vec3 midden() {
        if (arena != null) {
            return Vec3.atBottomCenterOf(arena.centrum());
        }
        return spawnPlek != null ? spawnPlek : position();
    }

    double straal() {
        return arena != null ? arena.straal() : BazenPlekken.OZ_STRAAL;
    }

    /** The 4 Lichtzwammen of the hall (centre + offsets). */
    public List<BlockPos> lichtPlekken() {
        BlockPos c = BlockPos.containing(midden());
        List<BlockPos> l = new ArrayList<>();
        for (int[] o : BazenPlekken.OZ_LICHT_OFFSETS) {
            l.add(c.offset(o[0], o[1], o[2]));
        }
        return l;
    }

    List<ServerPlayer> spelers(ServerLevel sl) {
        Vec3 m = midden();
        double r = straal() + 3;
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.position().distanceToSqr(m.x, p.getY(), m.z) <= r * r && Math.abs(p.getY() - m.y) < 24) {
                l.add(p);
            }
        }
        return l;
    }

    private void zeg(ServerLevel sl, String key, ChatFormatting kleur, Object... args) {
        for (ServerPlayer p : spelers(sl)) {
            p.sendOverlayMessage(Component.translatable(key, args).withStyle(kleur));
        }
    }

    @Override
    protected void signatuur() {
        speel("brul");
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.5f, 1.5f);
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 3, getZ(), 120, 3, 2, 3, 0.02);
        }
    }

    // ---- phases ------------------------------------------------------------------------------------------------------------

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        speel("brul");
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2f, 1.3f);
        if (fase == 2) {
            wortelvelden(sl, 5);
            gaDonker(sl);
        } else if (fase == 3) {
            if (donker()) {
                licht(sl, false);
            }
            kantel(sl);
        }
    }

    private void gaDonker(ServerLevel sl) {
        entityData.set(DONKER, true);
        for (BlockPos p : lichtPlekken()) {
            Herstel.zet(sl, H_LICHT, p, BazenRegistry.LICHTZWAM.get().defaultBlockState());
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, 12, 0.3, 0.4, 0.3, 0.01);
        }
        doet = Doet.RUST;
        timer = 40;
        zeg(sl, "vadsrijk.bazen.oz.donker", ChatFormatting.LIGHT_PURPLE);
        naamplaatje();
    }

    /** A Lichtzwam was lit (LichtzwamBlok listener). */
    public void aangestoken(ServerLevel sl, BlockPos pos) {
        if (!donker() || !lichtPlekken().contains(pos)) {
            return;
        }
        int n = 0;
        for (BlockPos p : lichtPlekken()) {
            n += LichtzwamBlok.aan(sl, p) ? 1 : 0;
        }
        if (n >= 4) {
            licht(sl, true);
        } else {
            zeg(sl, "vadsrijk.bazen.oz.licht", ChatFormatting.YELLOW, n);
        }
    }

    private void licht(ServerLevel sl, boolean verdiend) {
        entityData.set(DONKER, false);
        for (BlockPos p : lichtPlekken()) {
            LichtzwamBlok.zet(sl, p, true);
        }
        for (ServerPlayer p : spelers(sl)) {
            p.removeEffect(MobEffects.DARKNESS);
        }
        naamplaatje();
        if (verdiend) {
            zeg(sl, "vadsrijk.bazen.oz.licht_klaar", ChatFormatting.GOLD);
            Statussen.geef(this, Status.VERDOOFD, 100, 1, null);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 80, 1.5, 2, 1.5, 0.3);
            sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2f, 1.4f);
            super.krijgSchade(maxVads() * 0.08, null);
        }
    }

    private void kantel(ServerLevel sl) {
        doet = Doet.KANTEL;
        kantelT = 0;
        timer = 80;
        zeg(sl, "vadsrijk.bazen.oz.kantel", ChatFormatting.LIGHT_PURPLE);
        Vec3 m = midden();
        Vec3 kijk = m.add(0, 6, 0);
        RijkScene.Builder s = RijkScene.maak("oz_kantel", 80);
        for (int t = 0; t <= 80; t += 10) {
            double zwaai = (t / 10 % 2 == 0 ? 1 : -1) * (3 - t / 40.0);
            s.camera(t, m.add(10 + zwaai, 3 + t / 12.0, 10 - zwaai), kijk.add(0, t / 10.0, 0));
        }
        RijkScene sc = s.bouw();
        for (ServerPlayer p : spelers(sl)) {
            RijkScenes.speel(p, sc);
        }
    }

    /** One step of the ceiling breaking open (ring by ring from the centre). */
    private void plafond(ServerLevel sl, int ring) {
        BlockPos c = BlockPos.containing(midden());
        int r = BazenPlekken.OZ_PLAFOND_STRAAL;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int d = (int) Math.round(Math.sqrt(x * x + z * z));
                if (d != ring) {
                    continue;
                }
                for (int y = BazenPlekken.OZ_PLAFOND_ONDER; y <= BazenPlekken.OZ_PLAFOND_BOVEN; y++) {
                    BlockPos p = c.offset(x, y, z);
                    BlockState s = sl.getBlockState(p);
                    if (s.isAir() || s.getDestroySpeed(sl, p) < 0) {
                        continue;
                    }
                    Herstel.zet(sl, H_PLAFOND, p, Blocks.AIR.defaultBlockState());
                    if (sl.getRandom().nextInt(3) == 0) {
                        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.1);
                    }
                }
            }
        }
    }

    /** Everything back: ceiling, lights, size. */
    public static void herstel(ServerLevel sl) {
        Herstel.terug(sl, H_PLAFOND);
        Herstel.terug(sl, H_LICHT);
        Herstel.terug(sl, H_VLOER);
        Herstel.terug(sl, H_TOREN);
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        if (doet == Doet.KANTEL) {
            return;
        }
        super.krijgSchade((donker() ? n * 0.25 : n) * capFactor(bron), bron);
    }

    @Override
    public void naamplaatje() {
        super.naamplaatje();
        if (!level().isClientSide() && donker()) {
            setCustomNameVisible(false);
        }
    }

    // ---- fix round 1: Sporenlast (yellow, soak together), spread markers (purple), root fields, the root tower -----------------

    public static final String H_VLOER = "oz_vloer", H_TOREN = "oz_toren";
    /** Sporenlast: the marked player, ticks left. Spread: the marked players, ticks left. */
    @Nullable
    private java.util.UUID last;
    private int lastT = -1, spreidT = -1;
    private final List<java.util.UUID> spreid = new ArrayList<>();
    private int torenT = -1;

    /** Yellow: a heavy spore load lands on one player; whoever stands with them shares the hit (solo works, but hurts). */
    private void sporenlast(ServerLevel sl, ServerPlayer p) {
        last = p.getUUID();
        lastT = 70;
        zeg(sl, "vadsrijk.bazen.oz.last", ChatFormatting.YELLOW, p.getDisplayName());
        sl.playSound(null, p.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.5f, 1.4f);
    }

    /** Purple: every fighter gets a spore marker that bursts in 3.5 s; stand apart (each other marker nearby adds a hit). */
    private void spreidSporen(ServerLevel sl) {
        spreid.clear();
        for (ServerPlayer p : vechters()) {
            spreid.add(p.getUUID());
        }
        spreidT = 70;
        zeg(sl, "vadsrijk.bazen.oz.spreid", ChatFormatting.DARK_PURPLE);
        sl.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.5f, 0.8f);
    }

    private void markeringen(ServerLevel sl) {
        if (lastT >= 0 && last != null && sl.getEntity(last) instanceof ServerPlayer p) {
            if (lastT % 8 == 0) {
                Telegraaf.cirkel(sl, new Vec3(p.getX(), Math.floor(p.getY() + 0.01), p.getZ()), 3, 9, Telegraaf.GEEL);
            }
            if (lastT-- == 0) {
                List<ServerPlayer> samen = new ArrayList<>();
                for (ServerPlayer q : vechters()) {
                    if (q.distanceToSqr(p) <= 3.2 * 3.2) {
                        samen.add(q);
                    }
                }
                double elk = basisSchade(mobLevel()) * Math.max(1.0, 3.2 / Math.max(1, samen.size()));
                for (ServerPlayer q : samen) {
                    Schade.doe(new Schade.Klap(this, q, elk, element(), Schade.Soort.MAGIE, false, "bazen_sporenlast"));
                }
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1, p.getZ(), 80, 1.5, 1, 1.5, 0.05);
                sl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, p.getX(), p.getY() + 2, p.getZ(), 40, 1.5, 0.5, 1.5, 0.01);
                sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 1.4f);
                last = null;
            }
        } else {
            lastT = -1;
        }
        if (spreidT >= 0) {
            List<ServerPlayer> l = new ArrayList<>();
            for (java.util.UUID id : spreid) {
                if (sl.getEntity(id) instanceof ServerPlayer p && p.isAlive()) {
                    l.add(p);
                }
            }
            if (spreidT % 8 == 0) {
                for (ServerPlayer p : l) {
                    Telegraaf.cirkel(sl, new Vec3(p.getX(), Math.floor(p.getY() + 0.01), p.getZ()), 3, 9, Telegraaf.PAARS);
                }
            }
            if (spreidT-- == 0) {
                for (ServerPlayer p : l) {
                    int buren = 0;
                    for (ServerPlayer q : l) {
                        if (q != p && q.distanceToSqr(p) <= 3.2 * 3.2) {
                            buren++;
                        }
                    }
                    Schade.doe(new Schade.Klap(this, p, basisSchade(mobLevel()) * (0.6 + 1.4 * buren), element(), Schade.Soort.MAGIE, false, "bazen_spreidsporen"));
                    sl.sendParticles(ParticleTypes.WITCH, p.getX(), p.getY() + 1, p.getZ(), 40, 1.2, 0.8, 1.2, 0.05);
                }
                spreid.clear();
            }
        }
    }

    /** Root fields: patches of the hall floor crack open into roots (Plakkerig while you stand on them). Restored after the fight. */
    private void wortelvelden(ServerLevel sl, int aantal) {
        Vec3 m = midden();
        BlockPos c = BlockPos.containing(m).below();
        for (int i = 0; i < aantal; i++) {
            double a = getRandom().nextDouble() * Math.PI * 2, d = 5 + getRandom().nextDouble() * (straal() - 8);
            BlockPos v = c.offset((int) Math.round(Math.cos(a) * d), 0, (int) Math.round(Math.sin(a) * d));
            for (BlockPos p : BlockPos.betweenClosed(v.offset(-2, 0, -2), v.offset(2, 0, 2))) {
                if (p.distSqr(v) <= 5 && sl.getBlockState(p).isSolidRender() && sl.getBlockState(p.above()).isAir()) {
                    Herstel.zet(sl, H_VLOER, p, Blocks.ROOTED_DIRT.defaultBlockState());
                    if (getRandom().nextInt(3) == 0) {
                        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), p.getX() + 0.5, p.getY() + 1.1,
                                p.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.05);
                    }
                }
            }
        }
        sl.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2f, 0.4f);
        zeg(sl, "vadsrijk.bazen.oz.vloer", ChatFormatting.DARK_GREEN);
    }

    /** Where the root tower stands: 4 blocks from the centre (so its top is right at his cap once he is gigantic). */
    public BlockPos torenVoet() {
        return BlockPos.containing(midden()).offset(4, 0, 0);
    }

    /** The tower is 7 high: a stem, a 3x3 platform on top, and root steps (1 block each) climbing round it one ring further out. */
    public static final int TOREN_HOOGTE = 7;
    private static final int[][] TREDEN = {{2, -1}, {2, 0}, {2, 1}, {2, 2}, {1, 2}, {0, 2}, {-1, 2}};

    private void torenStap(ServerLevel sl, int h) {
        BlockPos v = torenVoet();
        if (h < TOREN_HOOGTE) {
            Herstel.zet(sl, H_TOREN, v.above(h), Blocks.MUSHROOM_STEM.defaultBlockState());
            BlockPos trede = v.offset(TREDEN[h][0], h, TREDEN[h][1]);
            if (sl.getBlockState(trede).canBeReplaced()) {
                Herstel.zet(sl, H_TOREN, trede, Blocks.ROOTED_DIRT.defaultBlockState());
            }
            if (h == TOREN_HOOGTE - 1) {
                for (BlockPos p : BlockPos.betweenClosed(v.offset(-1, h, -1), v.offset(1, h, 1))) {
                    if (sl.getBlockState(p).canBeReplaced()) {
                        Herstel.zet(sl, H_TOREN, p.immutable(), Blocks.MUSHROOM_STEM.defaultBlockState());
                    }
                }
            }
        }
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), v.getX() + 0.5, v.getY() + h + 0.5, v.getZ() + 0.5,
                12, 1, 0.4, 1, 0.1);
        sl.playSound(null, v.above(h), SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1.5f, 0.6f);
    }

    /** Fase 3: his stem is tough (ground hits x0.5), his cap is the weak spot (hits from the root tower's top x2). */
    private double capFactor(@Nullable LivingEntity bron) {
        if (fase() < 3 || bron == null) {
            return 1;
        }
        return bron.getY() >= getY() + TOREN_HOOGTE - 1.5 ? 2.0 : 0.5;
    }

    // ---- the fight ---------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (spawnPlek == null) {
            spawnPlek = position();
            if (!opgeruimd) {   // a fresh spawn: whatever an earlier fight broke is repaired first
                opgeruimd = true;
                herstel(sl);
            }
        }
        if (geplofd()) {
            return;
        }
        markeringen(sl);
        if (torenT >= 0 && tickCount % 6 == 0) {   // the root tower grows step by step
            torenStap(sl, torenT++);
            if (torenT >= TOREN_HOOGTE) {
                torenT = -1;
                zeg(sl, "vadsrijk.bazen.oz.toren", ChatFormatting.GOLD);
            }
        }
        if (tickCount % 10 == 0 && Herstel.heeft(sl, H_VLOER)) {
            for (ServerPlayer p : vechters()) {
                if (sl.getBlockState(p.blockPosition().below()).is(Blocks.ROOTED_DIRT) && p.getY() < midden().y + 1) {
                    Statussen.geef(p, Status.PLAKKERIG, 30, 0.4, this);
                }
            }
        }
        if (donker() && tickCount % 40 == 0) {
            for (ServerPlayer p : spelers(sl)) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
            }
        }
        if (doet == Doet.KANTEL) {
            kantelStap(sl);
            return;
        }
        if (stil()) {
            return;
        }
        if (inSprong()) {
            return;
        }
        LivingEntity t = doel();
        timer--;
        if (t != null && doet == Doet.RUST) {
            double d = distanceTo(t);
            if (d > (donker() ? 2 : 3.5) && fase() < 3) {   // (fase 3: rooted in the middle, next to his root tower)
                getNavigation().moveTo(t, donker() ? 1.15 : 0.7);
            } else if (tickCount % 30 == 0 && d < 3.5 * getScale()) {
                aanval(t);
                speel("stamp");
            }
        }
        switch (doet) {
            case RUST -> {
                if (timer > 0 || t == null) {
                    break;
                }
                int keuze = getRandom().nextInt(donker() ? 1 : fase() >= 3 ? 3 : 2);
                zet++;
                ServerPlayer ver = afstandsSpeler();
                if (!donker() && ver != null && zet % 3 == 0) {   // standing at range: a spore ball comes your way
                    lob(ver, 3, 26, 1.4, "bazen_sporenbal", Status.GESPOREN, 80, ParticleTypes.SPORE_BLOSSOM_AIR, Telegraaf.ROOD);
                    timer = 30;
                } else if (!donker() && zet % 5 == 2 && volgendDoel() != null) {
                    sporenlast(sl, volgendDoel());
                    timer = 45;
                } else if (!donker() && fase() >= 2 && zet % 5 == 4) {
                    spreidSporen(sl);
                    timer = 45;
                } else if (donker()) {
                    doet = Doet.MIKT;
                    timer = 25;
                    landing = voetenVan(t);
                    Telegraaf.cirkel(sl, landing, 3, 41, Telegraaf.ROOD);
                } else if (keuze == 0) {
                    wortels(sl);
                } else if (keuze == 1) {
                    sporen(sl);
                } else {
                    stamp(sl);
                }
            }
            case WORTELS -> {
                if (timer <= 0) {
                    for (Vec3 v : plekken) {
                        sl.sendParticles(ParticleTypes.COMPOSTER, v.x, v.y + 0.5, v.z, 30, 1.2, 0.8, 1.2, 0.1);
                        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), v.x, v.y + 0.3, v.z, 30, 1, 0.5, 1, 0.1);
                    }
                    sl.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
                    raak(sl, plekken, 2.6, 1.4, Status.PLAKKERIG, 60);
                    doet = Doet.RUST;
                    timer = 50;
                }
            }
            case SPOREN -> {
                if (timer % 5 == 0) {
                    Vec3 m = midden();
                    double r = straal();
                    for (int i = 0; i < 70; i++) {
                        double a = getRandom().nextDouble() * Math.PI * 2, d = Math.sqrt(getRandom().nextDouble()) * r;
                        Vec3 v = new Vec3(m.x + Math.cos(a) * d, m.y + 0.3 + getRandom().nextDouble() * 2.5, m.z + Math.sin(a) * d);
                        if (v.distanceTo(new Vec3(veilig.x, v.y, veilig.z)) > 4.2) {
                            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, v.x, v.y, v.z, 1, 0.2, 0.2, 0.2, 0);
                            if (i % 7 == 0) {
                                sl.sendParticles(ParticleTypes.CRIMSON_SPORE, v.x, v.y, v.z, 2, 0.4, 0.3, 0.4, 0);
                            }
                        }
                    }
                }
                if (timer <= 0) {
                    for (ServerPlayer p : spelers(sl)) {
                        double dx = p.getX() - veilig.x, dz = p.getZ() - veilig.z;
                        if (dx * dx + dz * dz > 4.5 * 4.5) {
                            Schade.doe(new Schade.Klap(this, p, basisSchade(mobLevel()) * 1.2, element(), Schade.Soort.MAGIE, false, "bazen_sporen"));
                            Statussen.geef(p, Status.GESPOREN, 120, mobLevel() * 0.8, this);
                        }
                    }
                    doet = Doet.RUST;
                    timer = 50;
                }
            }
            case MIKT -> {
                if (timer <= 0) {
                    doet = Doet.SPRINGT;
                    springNaar(landing, 16, 4);
                    sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 2f, 0.6f);
                }
            }
            case SPRINGT -> {
                doet = Doet.RUST;
                timer = 30;
            }
            case STAMP -> {
                if (timer <= 0) {
                    sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.6f);
                    for (Vec3 v : plekken) {
                        sl.sendParticles(ParticleTypes.EXPLOSION, v.x, v.y + 0.5, v.z, 3, 1.5, 0.3, 1.5, 0);
                    }
                    raak(sl, plekken, 4.5, 1.8, null, 0);
                    doet = Doet.RUST;
                    timer = 45;
                }
            }
            default -> {
            }
        }
    }

    @Override
    protected void geland() {
        if (level() instanceof ServerLevel sl) {
            raak(sl, List.of(position()), 3.2, 1.6, null, 0);
            speel("stamp");
        }
        doet = Doet.RUST;
        timer = 60;
    }

    private void kantelStap(ServerLevel sl) {
        kantelT++;
        if (kantelT == 1) {   // fase 3: back to the middle; the floor cracks, the root tower starts to grow
            Vec3 m = midden();
            getNavigation().stop();
            snapTo(m.x, m.y, m.z, getYRot(), 0);
            wortelvelden(sl, 8);
            torenT = 0;
        }
        setDeltaMovement(0, getDeltaMovement().y, 0);
        var schaal = getAttribute(Attributes.SCALE);
        if (schaal != null && kantelT <= 40) {
            schaal.setBaseValue(1 + (REUS - 1) * kantelT / 40.0);
        }
        if (kantelT % 4 == 0) {
            plafond(sl, kantelT / 4 - 3);
            sl.playSound(null, blockPosition(), SoundEvents.DEEPSLATE_BREAK, SoundSource.HOSTILE, 2f, 0.5f + sl.getRandom().nextFloat() * 0.3f);
        }
        if (kantelT % 10 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, midden().x, midden().y + 14, midden().z, 30, 4, 4, 4, 0.05);
        }
        if (kantelT >= 80) {
            doet = Doet.RUST;
            timer = 30;
        }
    }

    private void wortels(ServerLevel sl) {
        doet = Doet.WORTELS;
        timer = 32;
        plekken.clear();
        for (ServerPlayer p : spelers(sl)) {
            Vec3 v = new Vec3(p.getX(), Math.floor(p.getY()), p.getZ());
            plekken.add(v);
            Telegraaf.cirkel(sl, v, 2.5, timer, Telegraaf.ROZE);
        }
        speel("stamp");
    }

    private void sporen(ServerLevel sl) {
        doet = Doet.SPOREN;
        timer = 70;
        Vec3 m = midden();
        int q = getRandom().nextInt(4);
        double o = straal() * 0.5;
        veilig = m.add(q % 2 == 0 ? -o : o, 0, q < 2 ? -o : o);
        Telegraaf.cirkel(sl, veilig, 4, timer, Telegraaf.ROZE);
        sl.sendParticles(ParticleTypes.END_ROD, veilig.x, veilig.y + 1, veilig.z, 30, 1.5, 1, 1.5, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.HOSTILE, 2f, 0.5f);
        speel("brul");
        zeg(sl, "vadsrijk.bazen.oz.sporen", ChatFormatting.GREEN);
    }

    private void stamp(ServerLevel sl) {
        doet = Doet.STAMP;
        timer = 35;
        plekken.clear();
        List<ServerPlayer> l = spelers(sl);
        for (int i = 0; i < 3 && !l.isEmpty(); i++) {
            ServerPlayer p = l.get(getRandom().nextInt(l.size()));
            Vec3 v = new Vec3(p.getX() + getRandom().nextGaussian() * 2, Math.floor(p.getY()), p.getZ() + getRandom().nextGaussian() * 2);
            plekken.add(v);
            Telegraaf.cirkel(sl, v, 4.5, timer, Telegraaf.ROOD);
        }
        speel("stamp");
    }

    private void raak(ServerLevel sl, List<Vec3> waar, double r, double factor, @Nullable Status st, int stTicks) {
        for (ServerPlayer p : vechters()) {
            for (Vec3 v : waar) {
                double dx = p.getX() - v.x, dz = p.getZ() - v.z;
                if (dx * dx + dz * dz <= r * r && Math.abs(p.getY() - v.y) < 3) {
                    Schade.doe(new Schade.Klap(this, p, basisSchade(mobLevel()) * factor, element(), Schade.Soort.MELEE, false, "bazen_wortel"));
                    if (st != null) {
                        Statussen.geef(p, st, stTicks, 0.4, this);
                    }
                    p.setDeltaMovement(p.getDeltaMovement().x, 0.6, p.getDeltaMovement().z);
                    p.hurtMarked = true;
                    break;
                }
            }
        }
    }

    @Override
    protected void opPlof() {
        super.opPlof();
        if (level() instanceof ServerLevel sl) {
            entityData.set(DONKER, false);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 3, getZ(), 300, 4, 3, 4, 0.1);
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 3, getZ(), 200, 6, 3, 6, 0.02);
            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 2f, 0.5f);
            zeg(sl, "vadsrijk.bazen.oz.verslagen", ChatFormatting.LIGHT_PURPLE);
            BazenRegistry.later(sl.getServer(), 200, () -> herstel(sl));
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl && reason != RemovalReason.UNLOADED_TO_CHUNK && reason != RemovalReason.UNLOADED_WITH_PLAYER
                && reason != RemovalReason.CHANGED_DIMENSION && !geplofd()) {
            herstel(sl);   // dev reset / killed by a command mid-fight
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(DONKER, false);
        var schaal = getAttribute(Attributes.SCALE);
        if (schaal != null) {
            schaal.setBaseValue(1);
        }
    }
}
