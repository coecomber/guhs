package nl.juiced.guhs.feature.bio.bouwdal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.GuhTime;

/**
 * biomes3 bouw-dal: one weebhuisje and its trip to Japan. The trip is a state of the HOUSE (kept in {@link WeebHuizen},
 * world data): several players share it.
 * <pre>
 * THUIS    both at home, the balloon on its mooring, the loket sign says open, no note on the door
 *   | a player talks to one of them (and has no present waiting and no double in the hand)
 * GESPREK  the scene (five lines, about 12 seconds); everybody who talks to them now is written down as well
 * VERTREK  they hop out of the door to the balloon, step in, it rises and is gone (about 25 seconds)
 * WEG      nobody home for one game day: the note hangs on the door, the sign says closed. Nothing to claim.
 *   | the day is over AND the house is loaded again (nothing happens, and nothing is computed, while nobody is near)
 * THUIS    "Tadaima!"; everybody who was written down has a present waiting, handed over when they next talk to
 *          either of them, however much later that is (so a player who was offline at the return misses nothing)
 * </pre>
 * Everything is a function of two clocks read through {@link #dag} and {@link #spel}, so a restart, an unloaded chunk or a
 * crash in the middle of a scene only means the next step happens when the house is looked at again. While the house is
 * loaded {@link #zorg} keeps it right every few seconds, the way the lobby does for its characters: exactly one of each
 * of the two at home (a missing one is made again, a double removed, a strayed one put back), one balloon, the right
 * sign and note; while they are away, none of them.
 * <p>
 * Where things are is read from the building itself the first time ({@link #meet}): the loket sign, the note, the door,
 * the mooring, and where the two stand. So a turned copy of the house works the same.
 */
public final class WeebHuis {
    public enum Staat {
        THUIS, GESPREK, VERTREK, WEG
    }

    /** One game day away; and never longer than this many game ticks, also when the day clock stands still. */
    public static final long REIS = GuhTime.DAY, REIS_MAX_SPEL = 2 * GuhTime.DAY;
    /** The scene: when its five lines are said (ticks), and when they start walking. */
    static final int[] REGEL_OP = {0, 50, 110, 160, 205};
    static final int GESPREK_TICKS = 250;
    /** Walking speed to the balloon (blocks a tick), and how long the whole departure may take. */
    static final double LOOP = 0.16;
    static final int VERTREK_MAX = 900;
    static final int VARIANTEN = 5, TERUG_VARIANTEN = 5, VERTREK_VARIANTEN = 3;
    /** Who says lines 1-4 of each scene (E = Evivads, N = Nielsvads); line 5 they shout together. */
    static final String[] SPREKERS = {"ENEN", "ENNE", "ENEN", "NENE", "ENEN"};
    /** Who says the two lines of each homecoming. */
    static final String[] TERUG_SPREKERS = {"NE", "EN", "EN", "NE", "EN"};

    public final ResourceKey<Level> dim;
    public final BoundingBox box;
    Staat staat = Staat.THUIS;
    /** Game time at which the current state began. */
    long sinds;
    /** WEG: they are back when the day clock reaches this, or the game clock reaches terugSpel. */
    long terugDag, terugSpel;
    int variant = -1, stap, reizen, terugVariant;
    /** Who sent them off on the trip that is running. */
    final Set<UUID> zwaaiers = new LinkedHashSet<>();
    /** Presents waiting per player. */
    final Map<UUID, Integer> tegoed = new HashMap<>();
    // read from the building (meet)
    boolean gemeten;
    @Nullable
    BlockPos deur, briefje, bord, steiger;
    Vec3 thuisE, thuisN;
    float yawE, yawN;
    /** (tests) added to both clocks of this house. */
    transient long klokVooruit;
    private transient long volgendeZorg;

    public WeebHuis(ResourceKey<Level> dim, BoundingBox box) {
        this.dim = dim;
        this.box = box;
        Vec3 midden = new Vec3((box.minX() + box.maxX() + 1) / 2.0, box.minY() + 1, (box.minZ() + box.maxZ() + 1) / 2.0);
        this.thuisE = midden;
        this.thuisN = midden.add(1.5, 0, 0);
    }

    public Staat staat() {
        return staat;
    }

    public int reizen() {
        return reizen;
    }

    public boolean zwaaide(UUID speler) {
        return zwaaiers.contains(speler);
    }

    public int tegoed(UUID speler) {
        return tegoed.getOrDefault(speler, 0);
    }

    /** The day clock (the overworld's, shared by every dimension): one game day of it is a trip. */
    public long dag(ServerLevel level) {
        return GuhTime.dayTime(level) + klokVooruit;
    }

    /** The game clock: the timing of the scene and the departure. */
    public long spel(ServerLevel level) {
        return level.getGameTime() + klokVooruit;
    }

    /** (tests) moves this house's clocks forward. */
    public void klokVooruit(long ticks) {
        klokVooruit += ticks;
    }

    boolean actief() {
        return staat == Staat.GESPREK || staat == Staat.VERTREK;
    }

    AABB zoek() {
        return new AABB(box.minX() - 3, box.minY() - 2, box.minZ() - 3, box.maxX() + 4, box.maxY() + 60, box.maxZ() + 4);
    }

    Vec3 midden() {
        return new Vec3((box.minX() + box.maxX() + 1) / 2.0, (box.minY() + box.maxY()) / 2.0, (box.minZ() + box.maxZ() + 1) / 2.0);
    }

    /** Are the blocks and the entities of the whole house loaded? */
    public boolean geladen(ServerLevel level) {
        for (int x = (box.minX() - 3) >> 4; x <= (box.maxX() + 3) >> 4; x++) {
            for (int z = (box.minZ() - 3) >> 4; z <= (box.maxZ() + 3) >> 4; z++) {
                if (!level.hasChunk(x, z) || !level.areEntitiesLoaded(ChunkPos.pack(x, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    // --- reading the building ---------------------------------------------------------------------------------------------

    /** Finds the sign, the note, the door, the mooring and where the two stand. Once per house. */
    void meet(ServerLevel level) {
        if (gemeten) {
            return;
        }
        gemeten = true;
        Block bordBlok = BouwDalSlice.LOKETBORD.get(), briefjeBlok = BouwDalSlice.BRIEFJE.get();
        Block steigerBlok = Bio.blok("ballonsteiger", net.minecraft.world.level.block.Blocks.AIR);
        List<BlockPos> steigers = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState s = level.getBlockState(p);
            if (s.isAir()) {
                continue;
            }
            if (s.is(bordBlok)) {
                bord = p.immutable();
            } else if (s.is(briefjeBlok)) {
                briefje = p.immutable();
            } else if (s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER && deur == null) {
                deur = p.immutable();
            } else if (s.is(steigerBlok) && level.getBlockState(p.above()).isAir()) {
                steigers.add(p.immutable());
            }
        }
        if (!steigers.isEmpty()) {
            double mx = steigers.stream().mapToInt(BlockPos::getX).average().orElse(0), mz = steigers.stream().mapToInt(BlockPos::getZ).average().orElse(0);
            steigers.sort(Comparator.comparingDouble(p -> (p.getX() - mx) * (p.getX() - mx) + (p.getZ() - mz) * (p.getZ() - mz)));
            steiger = steigers.get(0);
        }
        GuhNpcEntity e = dichtste(level, GuhNpcEntity.Kind.WEEB_EVIVADS, midden()), n = dichtste(level, GuhNpcEntity.Kind.WEEB_NIELSVADS, midden());
        if (e != null) {
            thuisE = e.position();
            yawE = e.getYRot();
        }
        if (n != null) {
            thuisN = n.position();
            yawN = n.getYRot();
        }
    }

    @Nullable
    GuhNpcEntity dichtste(ServerLevel level, GuhNpcEntity.Kind kind, Vec3 bij) {
        List<GuhNpcEntity> lijst = level.getEntitiesOfClass(GuhNpcEntity.class, zoek(), x -> x.getKind() == kind && x.isAlive());
        lijst.sort(Comparator.comparingDouble(x -> x.distanceToSqr(bij)));
        return lijst.isEmpty() ? null : lijst.get(0);
    }

    // --- keeping the house right (the repair) ------------------------------------------------------------------------------

    /**
     * Makes the house what its state says. At home: exactly one Evivads and one Nielsvads on their spots and one balloon
     * on the mooring. Away: none of them. And the sign and the note. Returns how many things it had to put right.
     */
    public int zorg(ServerLevel level) {
        meet(level);
        int hersteld = 0;
        boolean thuis = staat == Staat.THUIS;
        if (staat == Staat.THUIS || staat == Staat.WEG) {
            hersteld += npc(level, GuhNpcEntity.Kind.WEEB_EVIVADS, thuisE, yawE, thuis);
            hersteld += npc(level, GuhNpcEntity.Kind.WEEB_NIELSVADS, thuisN, yawN, thuis);
            hersteld += ballon(level, thuis);
        }
        hersteld += blokken(level, staat != Staat.WEG);
        return hersteld;
    }

    private int npc(ServerLevel level, GuhNpcEntity.Kind kind, Vec3 pos, float yaw, boolean hoort) {
        List<GuhNpcEntity> hier = level.getEntitiesOfClass(GuhNpcEntity.class, zoek(), x -> x.getKind() == kind && x.isAlive());
        hier.sort(Comparator.comparingDouble(x -> x.distanceToSqr(pos)));
        int n = 0;
        for (int i = hoort ? 1 : 0; i < hier.size(); i++) {
            hier.get(i).stopRiding();
            hier.get(i).discard();
            n++;
        }
        if (!hoort) {
            return n;
        }
        if (hier.isEmpty()) {
            GuhNpcEntity nieuw = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
            if (nieuw == null) {
                return n;
            }
            nieuw.setKind(kind);
            nieuw.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
            nieuw.setYHeadRot(yaw);
            nieuw.setYBodyRot(yaw);
            nieuw.setPersistenceRequired();
            level.addFreshEntity(nieuw);
            return n + 1;
        }
        GuhNpcEntity e = hier.get(0);
        if (e.isPassenger() || e.distanceToSqr(pos) > 0.05) {
            e.stopRiding();
            e.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
            e.setYHeadRot(yaw);
            e.setYBodyRot(yaw);
            n++;
        }
        return n;
    }

    @Nullable
    Vec3 ballonPlek() {
        return steiger == null ? null : new Vec3(steiger.getX() + 0.5, steiger.getY() + 1, steiger.getZ() + 0.5);
    }

    private int ballon(ServerLevel level, boolean hoort) {
        Vec3 plek = ballonPlek();
        List<WeebBallonEntity> hier = level.getEntitiesOfClass(WeebBallonEntity.class, zoek(), x -> x.isAlive());
        int n = 0;
        WeebBallonEntity blijft = null;
        for (WeebBallonEntity b : hier) {
            if (hoort && plek != null && blijft == null && !b.stijgt()) {
                blijft = b;
            } else {
                b.wegMetAlles();
                n++;
            }
        }
        if (!hoort || plek == null) {
            return n;
        }
        if (blijft == null) {
            return n + (nieuweBallon(level, plek) == null ? 0 : 1);
        }
        if (blijft.position().distanceToSqr(plek) > 0.05) {
            blijft.setPos(plek);
            n++;
        }
        return n;
    }

    @Nullable
    private WeebBallonEntity nieuweBallon(ServerLevel level, Vec3 plek) {
        WeebBallonEntity b = BouwDalSlice.WEEB_BALLON.get().create(level, EntitySpawnReason.TRIGGERED);
        if (b == null) {
            return null;
        }
        b.snapTo(plek.x, plek.y, plek.z, yawE, 0f);
        level.addFreshEntity(b);
        return b;
    }

    /** The loket sign (open while they are home) and the note on the door (hangs while they are away). */
    private int blokken(ServerLevel level, boolean thuis) {
        int n = 0;
        n += stand(level, bord, BouwDalSlice.LOKETBORD.get(), thuis);
        n += stand(level, briefje, BouwDalSlice.BRIEFJE.get(), !thuis);
        return n;
    }

    private static int stand(ServerLevel level, @Nullable BlockPos pos, Block blok, boolean aan) {
        if (pos == null) {
            return 0;
        }
        BlockState s = level.getBlockState(pos);
        if (!s.is(blok) || s.getValue(KleinBlok.Stand.AAN) == aan) {
            return 0;
        }
        level.setBlock(pos, s.setValue(KleinBlok.Stand.AAN, aan), Block.UPDATE_ALL);
        return 1;
    }

    private void zetDeur(ServerLevel level, boolean open) {
        if (deur == null) {
            return;
        }
        BlockState s = level.getBlockState(deur);
        if (s.getBlock() instanceof DoorBlock d && s.getValue(DoorBlock.OPEN) != open) {
            d.setOpen(null, level, s, deur, open);
        }
    }

    // --- talking -------------------------------------------------------------------------------------------------------------

    static Component naam(char wie) {
        return Component.translatable("entity.guhs.guh_npc." + (wie == 'E' ? "weeb_evivads" : "weeb_nielsvads"));
    }

    static Component regel(Component naam, String key) {
        MutableComponent line = Component.literal("<").append(naam).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE);
        return line.append(Component.translatable(key).withStyle(ChatFormatting.WHITE));
    }

    static Component samen() {
        return Component.empty().append(naam('E')).append(" & ").append(naam('N'));
    }

    /** Says a line to everybody near the house. */
    void zegAllen(ServerLevel level, Component regel) {
        Vec3 m = midden();
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(m) < 28 * 28) {
                p.sendSystemMessage(regel);
            }
        }
    }

    /** A player right-clicked one of the two. */
    public void praat(ServerLevel level, GuhNpcEntity npc, ServerPlayer speler) {
        meet(level);
        GuhAdvancements.grant(speler, "weeb_ontmoet");
        char wie = npc.getKind() == GuhNpcEntity.Kind.WEEB_EVIVADS ? 'E' : 'N';
        UUID id = speler.getUUID();
        if (staat == Staat.GESPREK || staat == Staat.VERTREK) {
            if (zwaaiers.add(id)) {
                WeebHuizen.vuil(level);
            }
            speler.sendSystemMessage(regel(naam(wie), "quest.guhs.weeb.zwaai." + (wie == 'E' ? "e" : "n")));
            return;
        }
        if (staat != Staat.THUIS) {
            return;
        }
        if (tegoed(id) > 0) {
            cadeau(level, speler);
            return;
        }
        Cadeaus.Uitkomst ruil = Cadeaus.ruil(speler, speler.getMainHandItem(), level.getRandom());
        switch (ruil.soort()) {
            case GERUILD -> {
                speler.sendSystemMessage(regel(naam('N'), "quest.guhs.weeb.ruil.ja"));
                speler.sendSystemMessage(regel(naam('E'), "quest.guhs.weeb.ruil.hier"));
                klaar(speler);
                return;
            }
            case ENIGE -> {
                speler.sendSystemMessage(regel(naam(wie), "quest.guhs.weeb.ruil.enige"));
                return;
            }
            case COMPLEET -> {
                speler.sendSystemMessage(regel(naam(wie), "quest.guhs.weeb.ruil.compleet"));
                return;
            }
            default -> {
            }
        }
        begin(level, id);
    }

    /** The scene begins; this player (or whoever: a dev command) sends them off. Only from THUIS. */
    public boolean begin(ServerLevel level, UUID id) {
        if (staat != Staat.THUIS) {
            return false;
        }
        meet(level);
        staat = Staat.GESPREK;
        sinds = spel(level);
        stap = 0;
        int nieuw = level.getRandom().nextInt(VARIANTEN - 1);
        variant = reizen == 0 && variant < 0 ? 0 : (nieuw >= variant ? nieuw + 1 : nieuw);   // (the first ever: the cherry blossom; never twice running)
        zwaaiers.add(id);
        WeebHuizen.vuil(level);
        return true;
    }

    private static void klaar(ServerPlayer speler) {
        if ((Cadeaus.reeks(speler) & Cadeaus.REEKS_VOL) == Cadeaus.REEKS_VOL) {
            speler.sendSystemMessage(regel(samen(), "quest.guhs.weeb.compleet"));
        }
    }

    /** Hands a waiting present over, with a story about the trip. */
    private void cadeau(ServerLevel level, ServerPlayer speler) {
        UUID id = speler.getUUID();
        int over = tegoed(id) - 1;
        if (over > 0) {
            tegoed.put(id, over);
        } else {
            tegoed.remove(id);
        }
        WeebHuizen.vuil(level);
        int v = Math.floorMod(terugVariant + Cadeaus.aantal(speler), TERUG_VARIANTEN);
        String wie = TERUG_SPREKERS[v];
        speler.sendSystemMessage(regel(naam(wie.charAt(0)), "quest.guhs.weeb.terug." + (v + 1) + ".1"));
        speler.sendSystemMessage(regel(naam(wie.charAt(1)), "quest.guhs.weeb.terug." + (v + 1) + ".2"));
        GuhAdvancements.grant(speler, "weeb_eerste_reis");       // (also for who had logged off before the balloon left)
        Cadeaus.Cadeau c = Cadeaus.geef(speler, level.getRandom());
        speler.sendSystemMessage(Component.translatable("quest.guhs.weeb.cadeau", Cadeaus.stapel(c).getHoverName()).withStyle(ChatFormatting.GOLD));
        if (c.pool() == Cadeaus.Pool.REEKS) {
            klaar(speler);
        }
    }

    // --- the state machine -----------------------------------------------------------------------------------------------

    /** One step (only called while the house is loaded): every tick during the scene and the departure, else now and then. */
    public void tick(ServerLevel level) {
        long nu = spel(level);
        switch (staat) {
            case THUIS -> {
                if (nu >= volgendeZorg) {
                    volgendeZorg = nu + 100;
                    zorg(level);
                }
            }
            case GESPREK -> gesprek(level, nu - sinds);
            case VERTREK -> vertrek(level, nu - sinds);
            case WEG -> {
                if (dag(level) >= terugDag || nu >= terugSpel || dag(level) < terugDag - REIS - 200) {
                    thuiskomst(level);
                } else if (nu >= volgendeZorg) {
                    volgendeZorg = nu + 100;
                    zorg(level);
                }
            }
        }
    }

    private void gesprek(ServerLevel level, long t) {
        if (t > GESPREK_TICKS + VERTREK_MAX) {
            naarWeg(level);         // (the house was unloaded in the middle of it: they are simply gone)
            return;
        }
        String wie = SPREKERS[Math.floorMod(variant, VARIANTEN)];
        while (stap < REGEL_OP.length && t >= REGEL_OP[stap]) {
            String key = "quest.guhs.weeb.scene." + (Math.floorMod(variant, VARIANTEN) + 1) + "." + (stap + 1);
            if (stap < 4) {
                zegAllen(level, regel(naam(wie.charAt(stap)), key));
                huppel(level, wie.charAt(stap));
            } else {
                zegAllen(level, regel(samen(), "quest.guhs.weeb.samen"));
                huppel(level, 'E');
                huppel(level, 'N');
            }
            stap++;
            WeebHuizen.vuil(level);
        }
        if (t >= GESPREK_TICKS) {
            staat = Staat.VERTREK;
            sinds = spel(level);
            stap = 0;
            zetDeur(level, true);
            int v = Math.floorMod(reizen, VERTREK_VARIANTEN) + 1;
            zegAllen(level, regel(naam('N'), "quest.guhs.weeb.vertrek." + v + ".n"));
            WeebHuizen.vuil(level);
        }
    }

    /** A little jump of joy on the spot. */
    private void huppel(ServerLevel level, char wie) {
        GuhNpcEntity npc = dichtste(level, wie == 'E' ? GuhNpcEntity.Kind.WEEB_EVIVADS : GuhNpcEntity.Kind.WEEB_NIELSVADS, wie == 'E' ? thuisE : thuisN);
        if (npc != null) {
            npc.playSound(nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), 0.8f, wie == 'E' ? 1.35f : 1.05f);
        }
    }

    /** The way from a spot at home to the balloon: through the door. */
    List<Vec3> pad(Vec3 thuis) {
        List<Vec3> pad = new ArrayList<>();
        pad.add(thuis);
        if (deur != null) {
            Vec3 d = Vec3.atBottomCenterOf(deur);
            Vec3 a = d, b = d;
            for (Direction r : Direction.Plane.HORIZONTAL) {
                Vec3 voor = Vec3.atBottomCenterOf(deur.relative(r)), achter = Vec3.atBottomCenterOf(deur.relative(r.getOpposite()));
                if (box.isInside(deur.relative(r)) && voor.distanceToSqr(thuis) < achter.distanceToSqr(thuis) && voor.distanceToSqr(thuis) < a.distanceToSqr(thuis)) {
                    a = voor;
                    b = achter;
                }
            }
            pad.add(a);
            pad.add(d);
            pad.add(b);
        }
        Vec3 plek = ballonPlek();
        if (plek != null) {
            pad.add(plek.add(0, 0.3, 0));
        }
        return pad;
    }

    static double lengte(List<Vec3> pad) {
        double l = 0;
        for (int i = 1; i < pad.size(); i++) {
            l += pad.get(i).distanceTo(pad.get(i - 1));
        }
        return l;
    }

    /** Puts an NPC at this distance along the path, hopping; true when it is at the end. */
    private static boolean loop(GuhNpcEntity npc, List<Vec3> pad, double afstand) {
        double rest = Math.max(0, afstand);
        for (int i = 1; i < pad.size(); i++) {
            Vec3 a = pad.get(i - 1), b = pad.get(i);
            double l = a.distanceTo(b);
            if (rest <= l && l > 0) {
                Vec3 p = a.lerp(b, rest / l);
                float yaw = (float) (Mth.atan2(b.z - a.z, b.x - a.x) * Mth.RAD_TO_DEG) - 90f;
                double sprong = Math.abs(Math.sin(afstand * Math.PI / 1.1)) * 0.4;
                npc.snapTo(p.x, p.y + sprong, p.z, yaw, 0f);
                npc.setYHeadRot(yaw);
                npc.setYBodyRot(yaw);
                npc.setDeltaMovement(Vec3.ZERO);
                return false;
            }
            rest -= l;
        }
        Vec3 eind = pad.get(pad.size() - 1);
        npc.snapTo(eind.x, eind.y, eind.z, npc.getYRot(), 0f);
        return true;
    }

    private void vertrek(ServerLevel level, long t) {
        if (t > VERTREK_MAX) {
            naarWeg(level);
            return;
        }
        GuhNpcEntity e = dichtste(level, GuhNpcEntity.Kind.WEEB_EVIVADS, thuisE), n = dichtste(level, GuhNpcEntity.Kind.WEEB_NIELSVADS, thuisN);
        if (stap == 0) {
            // walking: Evivads first, Nielsvads a moment later
            boolean daarE = e == null || loop(e, pad(thuisE), LOOP * t);
            boolean daarN = n == null || loop(n, pad(thuisN), LOOP * (t - 18));
            if (daarE && daarN) {
                zetDeur(level, false);
                Vec3 plek = ballonPlek();
                WeebBallonEntity b = null;
                if (plek != null) {
                    List<WeebBallonEntity> hier = level.getEntitiesOfClass(WeebBallonEntity.class, zoek(), x -> x.isAlive() && !x.stijgt());
                    b = hier.isEmpty() ? nieuweBallon(level, plek) : hier.get(0);
                }
                if (b != null) {
                    if (e != null) {
                        e.startRiding(b, true, true);
                    }
                    if (n != null) {
                        n.startRiding(b, true, true);
                    }
                    b.stijgOp();
                }
                int v = Math.floorMod(reizen, VERTREK_VARIANTEN) + 1;
                zegAllen(level, regel(naam('E'), "quest.guhs.weeb.vertrek." + v + ".e"));
                stap = 1;
                sinds = spel(level);
                WeebHuizen.vuil(level);
            }
        } else if (t >= WeebBallonEntity.STIJG_TICKS + 5
                || (t > 20 && level.getEntitiesOfClass(WeebBallonEntity.class, zoek(), x -> x.isAlive() && x.stijgt()).isEmpty())) {
            naarWeg(level);
        }
    }

    /** They are gone: the house is empty for a game day. */
    private void naarWeg(ServerLevel level) {
        staat = Staat.WEG;
        sinds = spel(level);
        terugDag = dag(level) + REIS;
        terugSpel = sinds + REIS_MAX_SPEL;
        stap = 0;
        terugVariant = level.getRandom().nextInt(TERUG_VARIANTEN);
        zetDeur(level, false);
        zorg(level);
        for (UUID id : zwaaiers) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p != null) {
                GuhAdvancements.grant(p, "weeb_eerste_reis");
            }
        }
        WeebHuizen.vuil(level);
    }

    /** Tadaima! Everybody who sent them off has a present waiting. */
    private void thuiskomst(ServerLevel level) {
        staat = Staat.THUIS;
        sinds = spel(level);
        reizen++;
        for (UUID id : zwaaiers) {
            tegoed.merge(id, 1, Integer::sum);
        }
        zwaaiers.clear();
        volgendeZorg = sinds + 100;
        zorg(level);
        zegAllen(level, regel(samen(), "quest.guhs.weeb.tadaima"));
        WeebHuizen.vuil(level);
    }

    // --- saving ----------------------------------------------------------------------------------------------------------------

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Dim", dim.identifier().toString());
        t.putIntArray("Box", new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
        t.putInt("Staat", staat.ordinal());
        t.putLong("Sinds", sinds);
        t.putLong("TerugDag", terugDag);
        t.putLong("TerugSpel", terugSpel);
        t.putInt("Variant", variant);
        t.putInt("Stap", stap);
        t.putInt("Reizen", reizen);
        t.putInt("TerugVariant", terugVariant);
        ListTag z = new ListTag();
        for (UUID id : zwaaiers) {
            CompoundTag e = new CompoundTag();
            e.store("Id", UUIDUtil.CODEC, id);
            z.add(e);
        }
        t.put("Zwaaiers", z);
        ListTag g = new ListTag();
        for (Map.Entry<UUID, Integer> e : tegoed.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.store("Id", UUIDUtil.CODEC, e.getKey());
            c.putInt("Aantal", e.getValue());
            g.add(c);
        }
        t.put("Tegoed", g);
        t.putBoolean("Gemeten", gemeten);
        if (gemeten) {
            zetPos(t, "Deur", deur);
            zetPos(t, "Briefje", briefje);
            zetPos(t, "Bord", bord);
            zetPos(t, "Steiger", steiger);
            t.putDouble("EX", thuisE.x);
            t.putDouble("EY", thuisE.y);
            t.putDouble("EZ", thuisE.z);
            t.putFloat("EYaw", yawE);
            t.putDouble("NX", thuisN.x);
            t.putDouble("NY", thuisN.y);
            t.putDouble("NZ", thuisN.z);
            t.putFloat("NYaw", yawN);
        }
        return t;
    }

    private static void zetPos(CompoundTag t, String key, @Nullable BlockPos pos) {
        if (pos != null) {
            t.putLong(key, pos.asLong());
        }
    }

    @Nullable
    private static BlockPos leesPos(CompoundTag t, String key) {
        return t.contains(key) ? BlockPos.of(t.getLongOr(key, 0L)) : null;
    }

    @Nullable
    static WeebHuis load(CompoundTag t) {
        Identifier dim = Identifier.tryParse(t.getStringOr("Dim", ""));
        int[] b = t.getIntArray("Box").orElse(new int[0]);
        if (dim == null || b.length != 6) {
            return null;
        }
        WeebHuis h = new WeebHuis(ResourceKey.create(Registries.DIMENSION, dim), new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]));
        h.staat = Staat.values()[Mth.clamp(t.getIntOr("Staat", 0), 0, Staat.values().length - 1)];
        h.sinds = t.getLongOr("Sinds", 0L);
        h.terugDag = t.getLongOr("TerugDag", 0L);
        h.terugSpel = t.getLongOr("TerugSpel", 0L);
        h.variant = t.getIntOr("Variant", -1);
        h.stap = t.getIntOr("Stap", 0);
        h.reizen = t.getIntOr("Reizen", 0);
        h.terugVariant = t.getIntOr("TerugVariant", 0);
        ListTag z = t.getListOrEmpty("Zwaaiers");
        for (int i = 0; i < z.size(); i++) {
            z.getCompoundOrEmpty(i).read("Id", UUIDUtil.CODEC).ifPresent(h.zwaaiers::add);
        }
        ListTag g = t.getListOrEmpty("Tegoed");
        for (int i = 0; i < g.size(); i++) {
            CompoundTag c = g.getCompoundOrEmpty(i);
            c.read("Id", UUIDUtil.CODEC).ifPresent(id -> h.tegoed.put(id, c.getIntOr("Aantal", 1)));
        }
        h.gemeten = t.getBooleanOr("Gemeten", false);
        if (h.gemeten) {
            h.deur = leesPos(t, "Deur");
            h.briefje = leesPos(t, "Briefje");
            h.bord = leesPos(t, "Bord");
            h.steiger = leesPos(t, "Steiger");
            h.thuisE = new Vec3(t.getDoubleOr("EX", 0), t.getDoubleOr("EY", 0), t.getDoubleOr("EZ", 0));
            h.yawE = t.getFloatOr("EYaw", 0f);
            h.thuisN = new Vec3(t.getDoubleOr("NX", 0), t.getDoubleOr("NY", 0), t.getDoubleOr("NZ", 0));
            h.yawN = t.getFloatOr("NYaw", 0f);
        }
        return h;
    }
}
