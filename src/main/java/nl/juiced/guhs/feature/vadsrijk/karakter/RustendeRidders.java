package nl.juiced.guhs.feature.vadsrijk.karakter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * T-KARAKTER: the Rustende Ridders-zaal, the hall of fame of retired Eénvads knights. The list is server-wide SavedData
 * {@code guhs:vr_rustende_ridders} (with the overworld); the hall is a small building in ZWAMZOET, west of the plaza, on the
 * open lawn between the Kruidenier and the houses north of the harbour promenade ({@link #ANKER}, entrance to the south onto the
 * promenade). It is a STRUCTURE PIECE built by code (not a map tile), once, as soon as its chunk is loaded ({@link #tick}), and only
 * when the lawn is still free (else it logs and waits for {@code /guhs vadsrijk karakter zaal bouw hier}). The statues (armour
 * stands with the knight's own head, pink armour, a name plate) are rebuilt from the list after every retirement.
 */
public final class RustendeRidders extends SavedData {
    public static final SavedDataType<RustendeRidders> TYPE = GuhSavedData.tagType("vr_rustende_ridders", RustendeRidders::new,
            RustendeRidders::load, RustendeRidders::save);
    /** The floor centre of the hall: the floor is at y 63 (flush with the lawn), 9 wide (x) and 11 deep (z), door to the south. */
    public static final BlockPos ANKER = new BlockPos(-27, 63, 71);
    /** Version of the building: a higher number rebuilds it once (walls, floor, statues). */
    public static final int BOUW_VERSIE = 1;
    public static final String TAG = "vr_rustende_ridder";
    static final int HALF_X = 4, HALF_Z = 5, HOOGTE = 5;

    public record Ridder(UUID speler, int slot, String naam, String spelerNaam, int level, String klasse, int klasseLevel, String gebied,
            String waarom, long tijd) {
    }

    final List<Ridder> ridders = new ArrayList<>();
    int gebouwd;
    @Nullable
    BlockPos plek;
    boolean vies = true;

    public static RustendeRidders get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Ridder> ridders() {
        return List.copyOf(ridders);
    }

    /** Where the hall stands (its own spot when built "hier", else {@link #ANKER}). */
    public BlockPos plek() {
        return plek == null ? ANKER : plek;
    }

    public static void voegToe(@Nullable MinecraftServer s, Ridder r) {
        if (s == null) {
            return;
        }
        RustendeRidders d = get(s);
        d.ridders.add(r);
        d.vies = true;
        d.setDirty();
        ServerLevel sl = Vadsrijk.level(s);
        if (sl != null) {
            d.beelden(sl);
        }
    }

    // ---- saving -------------------------------------------------------------------------------------------------------------

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (Ridder r : ridders) {
            CompoundTag c = new CompoundTag();
            c.store("Speler", UUIDUtil.CODEC, r.speler());
            c.putInt("Slot", r.slot());
            c.putString("Naam", r.naam());
            c.putString("SpelerNaam", r.spelerNaam());
            c.putInt("Level", r.level());
            c.putString("Klasse", r.klasse());
            c.putInt("KlasseLevel", r.klasseLevel());
            c.putString("Gebied", r.gebied());
            c.putString("Waarom", r.waarom());
            c.putLong("Tijd", r.tijd());
            l.add(c);
        }
        t.put("Ridders", l);
        t.putInt("Gebouwd", gebouwd);
        if (plek != null) {
            t.putLong("Plek", plek.asLong());
        }
        return t;
    }

    private static RustendeRidders load(CompoundTag t) {
        RustendeRidders d = new RustendeRidders();
        ListTag l = t.getListOrEmpty("Ridders");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompoundOrEmpty(i);
            UUID id = c.read("Speler", UUIDUtil.CODEC).orElse(null);
            if (id != null) {
                d.ridders.add(new Ridder(id, c.getIntOr("Slot", 0), c.getStringOr("Naam", "?"), c.getStringOr("SpelerNaam", "?"), c.getIntOr("Level", 1),
                        c.getStringOr("Klasse", Klassen.RIDDER), c.getIntOr("KlasseLevel", 1), c.getStringOr("Gebied", ""), c.getStringOr("Waarom", ""),
                        c.getLongOr("Tijd", 0)));
            }
        }
        d.gebouwd = t.getIntOr("Gebouwd", 0);
        d.plek = t.getLong("Plek").map(BlockPos::of).orElse(null);
        return d;
    }

    // ---- the building -------------------------------------------------------------------------------------------------------

    /** Every 5 s: build the hall once its chunk is loaded; refresh the statues when the list changed. */
    static void tick(MinecraftServer s) {
        ServerLevel sl = Vadsrijk.level(s);
        if (sl == null) {
            return;
        }
        RustendeRidders d = get(s);
        BlockPos a = d.plek();
        if (!sl.isLoaded(a) || !sl.isLoaded(a.offset(HALF_X + 1, 0, HALF_Z + 4)) || !sl.isLoaded(a.offset(-HALF_X - 1, 0, -HALF_Z - 1))) {
            return;
        }
        if (d.gebouwd < BOUW_VERSIE) {
            if (d.gebouwd == 0 && !vrij(sl, a)) {
                if (d.gebouwd != -1) {
                    com.mojang.logging.LogUtils.getLogger().warn("[T-KARAKTER] Rustende Ridders-zaal: the lawn at {} is not free (map changed?); not built. "
                            + "Use /guhs vadsrijk karakter zaal bouw hier", a);
                }
                d.gebouwd = -1;   // (warn once; a dev build "hier" sets it)
                d.setDirty();
                return;
            }
            if (d.gebouwd >= 0) {
                bouw(sl, a);
                d.gebouwd = BOUW_VERSIE;
                d.vies = true;
                d.setDirty();
            }
        }
        if (d.vies && d.gebouwd > 0) {
            d.beelden(sl);
        }
    }

    /** Is the lawn still free: floor level solid at y, air above for the whole footprint (+ the path)? */
    static boolean vrij(ServerLevel sl, BlockPos a) {
        for (int x = -HALF_X; x <= HALF_X; x++) {
            for (int z = -HALF_Z; z <= HALF_Z + 3; z++) {
                BlockPos b = a.offset(x, 0, z);
                if (sl.getBlockState(b).isAir()) {
                    return false;
                }
                for (int y = 1; y <= HOOGTE + 1; y++) {
                    BlockState s = sl.getBlockState(b.above(y));
                    if (!s.isAir() && !s.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** (dev) Build the hall here (floor = the block under your feet) and remember the spot. */
    public static void bouwHier(ServerLevel sl, BlockPos voeten) {
        RustendeRidders d = get(sl.getServer());
        d.plek = voeten.below();
        bouw(sl, d.plek);
        d.gebouwd = BOUW_VERSIE;
        d.vies = true;
        d.setDirty();
        d.beelden(sl);
    }

    /** (dev) Build at the configured spot now, even when not free. */
    public static void bouwAnker(ServerLevel sl) {
        RustendeRidders d = get(sl.getServer());
        d.plek = null;
        bouw(sl, ANKER);
        d.gebouwd = BOUW_VERSIE;
        d.vies = true;
        d.setDirty();
        d.beelden(sl);
    }

    private static Block blok(String id, Block anders) {
        Block b = BuiltInRegistries.BLOCK.getValue(Guhs.id(id));
        return b == Blocks.AIR ? anders : b;
    }

    /**
     * The hall: a pink brick hall (9 x 11, walls 5 high) with cherry corner posts, stained-glass windows, a cherry stair roof, a
     * heart-tile floor flush with the lawn, a 3-wide open entrance to the south and a clinker path to the promenade. Pedestals
     * along the walls (statues on them), lanterns, a name sign over the entrance inside.
     */
    static void bouw(ServerLevel sl, BlockPos a) {
        Block muur = blok("pastelbaksteen_roze", Blocks.PINK_TERRACOTTA);
        Block vloer = blok("tegel_hartjes", Blocks.SMOOTH_QUARTZ);
        Block raam = blok("glasinlood_guh", Blocks.PINK_STAINED_GLASS);
        Block pad = blok("knuffelklinkers", Blocks.BRICKS);
        Block sokkel = Blocks.QUARTZ_PILLAR;
        BlockState post = Blocks.STRIPPED_CHERRY_LOG.defaultBlockState();
        int x0 = -HALF_X, x1 = HALF_X, z0 = -HALF_Z, z1 = HALF_Z;
        // clear + floor + foundation
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                for (int y = 1; y <= HOOGTE + 7; y++) {
                    zet(sl, a.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                if (x >= x0 && x <= x1 && z >= z0 && z <= z1) {
                    zet(sl, a.offset(x, 0, z), vloer.defaultBlockState());
                    for (int y = -1; y >= -3; y--) {   // (no hole under the floor)
                        if (sl.getBlockState(a.offset(x, y, z)).isAir()) {
                            zet(sl, a.offset(x, y, z), Blocks.DIRT.defaultBlockState());
                        }
                    }
                }
            }
        }
        // walls: cherry corner posts, stained glass at eye height, a 3-wide 3-high open entrance in the south wall
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean zij = x == x0 || x == x1, achter = z == z0, voor = z == z1;
                if (!zij && !achter && !voor) {
                    continue;
                }
                boolean hoek = zij && (achter || voor);
                boolean deur = voor && Math.abs(x) <= 1;
                boolean ramen = zij ? Math.abs(z) == 2 : Math.abs(x) == (voor ? 3 : 2);
                for (int y = 1; y <= HOOGTE; y++) {
                    BlockPos b = a.offset(x, y, z);
                    if (deur && y <= 3) {
                        continue;
                    }
                    zet(sl, b, hoek ? post : !deur && ramen && (y == 2 || y == 3) ? raam.defaultBlockState() : muur.defaultBlockState());
                }
            }
        }
        // ceiling (cherry planks with froglights) + a cherry stair gable roof (ridge north-south) with a slab ridge
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean licht = x == 0 && (z == -3 || z == 0 || z == 3);
                zet(sl, a.offset(x, HOOGTE + 1, z), licht ? Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState() : Blocks.CHERRY_PLANKS.defaultBlockState());
            }
        }
        BlockState trapO = Blocks.CHERRY_STAIRS.defaultBlockState().setValue(StairBlock.FACING, net.minecraft.core.Direction.EAST)
                .setValue(StairBlock.HALF, Half.BOTTOM);
        BlockState trapW = Blocks.CHERRY_STAIRS.defaultBlockState().setValue(StairBlock.FACING, net.minecraft.core.Direction.WEST)
                .setValue(StairBlock.HALF, Half.BOTTOM);
        for (int z = z0 - 1; z <= z1 + 1; z++) {
            for (int i = 0; i <= HALF_X; i++) {
                int y = HOOGTE + 1 + i;
                zet(sl, a.offset(x0 - 1 + i, y, z), trapO);
                zet(sl, a.offset(x1 + 1 - i, y, z), trapW);
                for (int yy = HOOGTE + 2; yy < y; yy++) {   // (planks under the slope: a closed attic)
                    zet(sl, a.offset(x0 - 1 + i, yy, z), Blocks.CHERRY_PLANKS.defaultBlockState());
                    zet(sl, a.offset(x1 + 1 - i, yy, z), Blocks.CHERRY_PLANKS.defaultBlockState());
                }
            }
            for (int yy = HOOGTE + 2; yy <= HOOGTE + 1 + HALF_X; yy++) {
                zet(sl, a.offset(0, yy, z), Blocks.CHERRY_PLANKS.defaultBlockState());
            }
            zet(sl, a.offset(0, HOOGTE + 2 + HALF_X, z), Blocks.CHERRY_SLAB.defaultBlockState());
        }
        // pedestals (the statues stand on them) along the walls, never in the 5-wide aisle; hanging lanterns
        for (BlockPos s : sokkels(a)) {
            zet(sl, s, sokkel.defaultBlockState());
        }
        BlockState hangt = Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true);
        for (int z : new int[] {-2, 2}) {
            zet(sl, a.offset(-2, HOOGTE, z), hangt);
            zet(sl, a.offset(2, HOOGTE, z), hangt);
        }
        // the path to the promenade (3 wide, flush with the lawn), lanterns beside the entrance outside
        for (int z = z1 + 1; z <= z1 + 3; z++) {
            for (int x = -1; x <= 1; x++) {
                zet(sl, a.offset(x, 0, z), pad.defaultBlockState());
            }
        }
        zet(sl, a.offset(-2, 1, z1 + 1), Blocks.LANTERN.defaultBlockState());
        zet(sl, a.offset(2, 1, z1 + 1), Blocks.LANTERN.defaultBlockState());
        // the name above the entrance (outside) and inside over the door
        ruimTekst(sl, a);
        tekst(sl, a.getX() + 0.5, a.getY() + 4.6, a.getZ() + z1 + 1.3,
                Component.translatable("vadsrijk.karakter.zaal.naam").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), 1.1f);
        tekst(sl, a.getX() + 0.5, a.getY() + 4.5, a.getZ() + z1 - 0.6,
                Component.translatable("vadsrijk.karakter.zaal.onder").withStyle(ChatFormatting.LIGHT_PURPLE), 0.8f);
    }

    /** The pedestal blocks (the statue stands on top): 4 along each long wall + 3 along the back wall. */
    static List<BlockPos> sokkels(BlockPos a) {
        List<BlockPos> l = new ArrayList<>();
        for (int z : new int[] {-3, -1, 1, 3}) {
            l.add(a.offset(-HALF_X + 1, 1, z));
            l.add(a.offset(HALF_X - 1, 1, z));
        }
        for (int x : new int[] {-2, 0, 2}) {
            l.add(a.offset(x, 1, -HALF_Z + 1));
        }
        return l;
    }

    private static void zet(ServerLevel sl, BlockPos b, BlockState s) {
        sl.setBlock(b, nl.juiced.guhs.feature.vadsrijk.verbind.Verbind.vorm(sl, b, s), Block.UPDATE_CLIENTS);   // U-HEKKEN: walls/panes connected
    }

    private static void ruimTekst(ServerLevel sl, BlockPos a) {
        AABB box = new AABB(a).inflate(HALF_X + 3, HOOGTE + 6, HALF_Z + 4);
        for (Entity e : sl.getEntitiesOfClass(Display.TextDisplay.class, box, e -> e.entityTags().contains(TAG + "_bord"))) {
            e.discard();
        }
    }

    private static void tekst(ServerLevel sl, double x, double y, double z, Component c, float schaal) {
        Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return;
        }
        CompoundTag tag = nl.juiced.guhs.storage.Nbt.saveWithoutId(d);
        tag.put("text", ComponentSerialization.CODEC.encodeStart(sl.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), c)
                .getOrThrow());
        tag.putInt("background", 0x40000000);
        tag.putString("billboard", "center");
        tag.putBoolean("shadow", true);
        CompoundTag tr = new CompoundTag();
        tr.put("translation", floats(0, 0, 0));
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("scale", floats(schaal, schaal, schaal));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("transformation", tr);
        nl.juiced.guhs.storage.Nbt.load(d, tag);
        d.snapTo(x, y, z, 0, 0);
        d.addTag(TAG + "_bord");
        sl.addFreshEntity(d);
    }

    private static ListTag floats(float... f) {
        ListTag l = new ListTag();
        for (float v : f) {
            l.add(net.minecraft.nbt.FloatTag.valueOf(v));
        }
        return l;
    }

    // ---- the statues --------------------------------------------------------------------------------------------------------

    /** Rebuilds the statues from the list (newest first, as many as there are pedestals). */
    void beelden(ServerLevel sl) {
        BlockPos a = plek();
        if (!sl.isLoaded(a) || gebouwd <= 0) {
            vies = true;
            return;
        }
        AABB box = new AABB(a).inflate(HALF_X + 2, HOOGTE + 2, HALF_Z + 2);
        for (Entity e : sl.getEntitiesOfClass(Entity.class, box, e -> e.entityTags().contains(TAG))) {
            e.discard();
        }
        List<BlockPos> plekken = sokkels(a);
        List<Ridder> l = new ArrayList<>(ridders);
        java.util.Collections.reverse(l);
        for (int i = 0; i < Math.min(plekken.size(), l.size()); i++) {
            BlockPos s = plekken.get(i);
            Ridder r = l.get(i);
            // face the aisle: west wall faces east, east wall faces west, back wall faces south
            float yaw = s.getX() < a.getX() ? -90f : s.getX() > a.getX() ? 90f : 0f;
            beeld(sl, s.above(), yaw, r);
        }
        vies = false;
    }

    private static void beeld(ServerLevel sl, BlockPos voet, float yaw, Ridder r) {
        ArmorStand st = new ArmorStand(EntityType.ARMOR_STAND, sl);
        CompoundTag t = nl.juiced.guhs.storage.Nbt.saveWithoutId(st);
        t.putBoolean("ShowArms", true);
        t.putBoolean("NoBasePlate", true);
        t.putBoolean("Invulnerable", true);
        t.putBoolean("NoGravity", true);
        t.putInt("DisabledSlots", 4144959);
        nl.juiced.guhs.storage.Nbt.load(st, t);
        st.snapTo(voet.getX() + 0.5, voet.getY(), voet.getZ() + 0.5, yaw, 0);
        st.setYHeadRot(yaw);
        st.setYBodyRot(yaw);
        ItemStack hoofd = new ItemStack(Items.PLAYER_HEAD);
        hoofd.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(r.speler()));
        st.setItemSlot(EquipmentSlot.HEAD, hoofd);
        st.setItemSlot(EquipmentSlot.CHEST, roze(Items.LEATHER_CHESTPLATE));
        st.setItemSlot(EquipmentSlot.LEGS, roze(Items.LEATHER_LEGGINGS));
        st.setItemSlot(EquipmentSlot.FEET, roze(Items.LEATHER_BOOTS));
        st.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        Component klasse = Component.translatable(Klassen.ofRidder(r.klasse()).naamKey());
        st.setCustomName(Component.literal(Modus.PENSIOEN_BADGE + " ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(r.naam()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                .append(Component.literal("  ").append(Component.translatable("vadsrijk.karakter.zaal.plaat", r.level(), klasse, r.spelerNaam()))
                        .withStyle(ChatFormatting.GRAY)));
        st.setCustomNameVisible(true);
        st.addTag(TAG);
        sl.addFreshEntity(st);
    }

    private static ItemStack roze(net.minecraft.world.item.Item item) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFF8CC8));
        return s;
    }
}
