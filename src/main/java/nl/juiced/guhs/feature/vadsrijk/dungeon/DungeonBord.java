package nl.juiced.guhs.feature.vadsrijk.dungeon;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeurData.Deur;

/**
 * X-DUNGEON (round X, the user: "Graag ook bij dungeon-entrance-deuren een stuk tekst met naam van dungeon, dit zorgt er ook meteen
 * voor dat ze meer opvallen"): a name board over every dungeon door, two text displays: the name big ("De Zwamgrot", the theme's
 * colour, bold) and a smaller line under it ("Dungeon · Grot · lv 11-14"; a sealed second entrance says where the door is).
 * <ul>
 *   <li><b>Where</b>: just outside the door (1 block out of the door plane, towards the stand spot), right over the door's top;
 *       when that spot is in rock it goes down/out until both lines fit in open cells. A hatch: over its stand spot.</li>
 *   <li><b>Robust</b>: made by the game with the door ({@link DungeonDeur#tick}, every 2 s while the chunk is loaded); one per key
 *       through U-DUBBEL ({@link Dubbel#merk}/{@link Dubbel#zoek}); a copy a chunk brings back from disk is always refused
 *       ({@link #opJoin}), so an old board (an older text, a door that moved) never stays; a board not on its spot any more is
 *       replaced. Text displays have no hitbox (not killable by a player); a /kill is undone within 2 s.</li>
 *   <li><b>Standing out</b>: full-bright text with a dark background and a shadow, visible from far (view range x2); an invisible
 *       light block (level 11) in the board's open cell lights the door at night; a few soft glow sparks at the door face while a
 *       player is within 24 blocks.</li>
 * </ul>
 */
public final class DungeonBord {
    /** Entity tag of every board (also how a disk copy is recognised). */
    public static final String TAG = "guhs_vr_dungeonbord";
    /** Scale of the name line and of the small line. */
    static final float SCHAAL_NAAM = 1.7f, SCHAAL_ONDER = 0.95f;
    /** Height of the small line's block (with background) in blocks, so the name sits right on top. */
    static final double HOOG_ONDER = 0.32;
    /** Sparks only when a player is this close. */
    static final double VONK_AFSTAND = 24;

    private DungeonBord() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DungeonBord::opJoin);
    }

    /** A board a chunk brings back from disk is never kept: {@link #zorg} makes a fresh one (current text and spot). */
    static void opJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide() && e.loadedFromDisk() && e.getEntity().entityTags().contains(TAG)) {
            e.setCanceled(true);
        }
    }

    /** The colour of the name per theme. */
    static int kleur(String thema) {
        return switch (thema) {
            case "zwam" -> 0xFF7A8C;
            case "kristal" -> 0xC9A8FF;
            case "wortel" -> 0xA8E07A;
            case "koraal" -> 0xFF9E8A;
            case "wolk" -> 0xFFE7A0;
            case "zout" -> 0xFFC6D0;
            default -> 0xFFD7F0;
        };
    }

    /** The two lines of a door's board: the name and the small line. */
    public static List<Component> regels(Deur d) {
        Dungeon k = Dungeons.van(d.dungeon());
        Component naam = k != null ? k.naam() : Component.literal(d.dungeon());
        MutableComponent groot = naam.copy().withStyle(s -> s.withBold(true).withColor(kleur(d.thema())));
        Component onder;
        if (d.zegel()) {
            onder = Component.translatable("vadsrijk.dungeon.zegel.bord." + d.dungeon()).withStyle(ChatFormatting.GRAY);
        } else if (k != null) {
            onder = Component.translatable("vadsrijk.dungeon.bord.onder", k.type().label(), k.minLv(), k.maxLv()).withStyle(ChatFormatting.WHITE);
        } else {
            onder = Component.translatable("vadsrijk.dungeon.bord.onder.kort").withStyle(ChatFormatting.WHITE);
        }
        return List.of(groot, onder);
    }

    /** The key of one board line (U-DUBBEL). */
    static String sleutel(Deur d, int nr, int regel) {
        return "dungeonbord:" + d.dungeon() + ":" + nr + ":" + regel;
    }

    /** The horizontal unit vector from the door's middle out to its stand spot (a hatch: none). */
    static Vec3 uit(Deur d) {
        double mx = 0, mz = 0;
        for (int[] c : d.cellen()) {
            mx += c[0] + 0.5;
            mz += c[2] + 0.5;
        }
        int n = Math.max(1, d.cellen().size());
        mx /= n;
        mz /= n;
        double dx = d.voor().getX() + 0.5 - mx, dz = d.voor().getZ() + 0.5 - mz;
        double l = Math.hypot(dx, dz);
        return l < 1e-3 ? Vec3.ZERO : new Vec3(dx / l, 0, dz / l);
    }

    /** The middle of the door's cells (exact, x/z block centres). */
    static Vec3 midden(Deur d) {
        double x = 0, y = 0, z = 0;
        for (int[] c : d.cellen()) {
            x += c[0] + 0.5;
            y += c[1];
            z += c[2] + 0.5;
        }
        int n = Math.max(1, d.cellen().size());
        return new Vec3(x / n, y / n, z / n);
    }

    static int topY(Deur d) {
        int t = Integer.MIN_VALUE;
        for (int[] c : d.cellen()) {
            t = Math.max(t, c[1]);
        }
        return t;
    }

    static int bodemY(Deur d) {
        int t = Integer.MAX_VALUE;
        for (int[] c : d.cellen()) {
            t = Math.min(t, c[1]);
        }
        return t;
    }

    /**
     * Where the board goes (the bottom of the small line): over the door's top, 1 block out; when that is not open, further out (2, 3
     * blocks), then lower (down to 1 above the door's bottom). A hatch: 2.2 over its stand spot. Null: nowhere open.
     */
    @Nullable
    public static Vec3 plek(ServerLevel sl, Deur d) {
        if (d.luik()) {
            Vec3 v = new Vec3(d.voor().getX() + 0.5, d.voor().getY() + 2.2, d.voor().getZ() + 0.5);
            return vrij(sl, v) ? v : null;
        }
        Vec3 m = midden(d), u = uit(d);
        int top = topY(d) + 1, bodem = bodemY(d) + 1;
        for (int y = top; y >= bodem; y--) {
            for (double ver : new double[] {1.0, 2.0, 3.0}) {
                Vec3 v = new Vec3(m.x + u.x * ver, y + 0.05, m.z + u.z * ver);
                if (vrij(sl, v)) {
                    return v;
                }
            }
        }
        return null;
    }

    /** Both lines fit: the cell of the small line and the one above it are open (no collision). */
    static boolean vrij(ServerLevel sl, Vec3 v) {
        BlockPos a = BlockPos.containing(v);
        return open(sl.getBlockState(a), sl, a) && open(sl.getBlockState(a.above()), sl, a.above());
    }

    private static boolean open(BlockState s, ServerLevel sl, BlockPos p) {
        return s.getCollisionShape(sl, p).isEmpty() && !s.is(DungeonDeur.BLOK.get());
    }

    /**
     * Every 2 s per door whose chunk is loaded: its two board lines are there (one each, on their spot), the light block in the
     * board's cell, and (a player near) a few sparks. {@code nr} = the door's index in {@link DungeonDeur#deuren()}.
     */
    public static void zorg(ServerLevel sl, Deur d, int nr) {
        zorg(sl, d, nr, true);
    }

    /** The same; {@code wacht} false (gametests): also when the chunk's entities are not entity-ticking. */
    public static void zorg(ServerLevel sl, Deur d, int nr, boolean wacht) {
        BlockPos m = DungeonDeur.midden(d);
        if (wacht && !Dubbel.klaar(sl, m)) {
            return;   // (the chunk's entities are not back yet: looking now would make a second board)
        }
        Vec3 v = plek(sl, d);
        if (v == null) {
            return;
        }
        List<Component> r = regels(d);
        double[] hoog = {HOOG_ONDER, 0};
        float[] schaal = {SCHAAL_NAAM, SCHAAL_ONDER};
        for (int i = 0; i < 2; i++) {
            Vec3 p = v.add(0, hoog[i], 0);
            String k = sleutel(d, nr, i);
            Entity e = Dubbel.zoek(sl, k);
            if (e != null && e.position().distanceToSqr(p) < 0.04) {
                continue;
            }
            if (e != null) {
                e.discard();   // (the door moved or the spot changed)
            }
            maak(sl, p, r.get(i), schaal[i], k);
        }
        // the light: an invisible light block in the board's open cell (only in air, never in water)
        BlockPos lp = BlockPos.containing(v);
        BlockState ls = sl.getBlockState(lp);
        if (ls.isAir()) {
            sl.setBlock(lp, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 11), 2);
        }
        vonken(sl, d, v);
    }

    /** Soft glow sparks over the door face (or the hatch) while a player is near. */
    static void vonken(ServerLevel sl, Deur d, Vec3 bord) {
        Vec3 m = midden(d);
        if (sl.getNearestPlayer(m.x, m.y, m.z, VONK_AFSTAND, false) == null) {
            return;
        }
        if (d.luik()) {
            sl.sendParticles(ParticleTypes.GLOW, m.x, m.y + 1.2, m.z, 5, 1.6, 0.2, 1.6, 0.0);
            return;
        }
        Vec3 u = uit(d);
        double h = (topY(d) - bodemY(d) + 1) / 2.0;
        sl.sendParticles(ParticleTypes.GLOW, m.x + u.x * 0.65, bodemY(d) + h, m.z + u.z * 0.65, 4, Math.abs(u.z) * 1.0 + 0.1, h * 0.8,
                Math.abs(u.x) * 1.0 + 0.1, 0.0);
    }

    /** One board line: a text display, full bright, dark background, shadow, turning with the viewer around the vertical axis. */
    static Display.TextDisplay maak(ServerLevel sl, Vec3 p, Component tekst, float schaal, String sleutel) {
        Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        CompoundTag tag = nl.juiced.guhs.storage.Nbt.saveWithoutId(d);
        tag.put("text", ComponentSerialization.CODEC.encodeStart(sl.registryAccess().createSerializationContext(NbtOps.INSTANCE), tekst).getOrThrow());
        tag.putInt("background", 0x99201028);
        tag.putString("billboard", "vertical");
        tag.putBoolean("shadow", true);
        tag.putFloat("view_range", 2f);
        tag.putInt("line_width", 400);
        CompoundTag licht = new CompoundTag();
        licht.putInt("block", 15);
        licht.putInt("sky", 15);
        tag.put("brightness", licht);
        CompoundTag tr = new CompoundTag();
        tr.put("translation", floats(0, 0, 0));
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("scale", floats(schaal, schaal, schaal));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("transformation", tr);
        nl.juiced.guhs.storage.Nbt.load(d, tag);
        d.snapTo(p.x, p.y, p.z, 0, 0);
        d.addTag(TAG);
        Dubbel.merk(d, sleutel);
        sl.addFreshEntity(d);
        return d;
    }

    private static ListTag floats(float... f) {
        ListTag l = new ListTag();
        for (float v : f) {
            l.add(FloatTag.valueOf(v));
        }
        return l;
    }

    /** The boards near a door (gametests, dev). */
    public static List<Display.TextDisplay> borden(ServerLevel sl, Deur d) {
        BlockPos m = DungeonDeur.midden(d);
        return new ArrayList<>(sl.getEntitiesOfClass(Display.TextDisplay.class, new net.minecraft.world.phys.AABB(m).inflate(8),
                e -> e.entityTags().contains(TAG) && !e.isRemoved()));
    }
}
