package nl.juiced.guhs.feature.droomwolkje;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomReis;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * 1.8.0 (uitbreiding U1): everyone's own droomwolkje in Guhdromenland, after Snoes' story. A little cloud island per player
 * on a grid far from the fixed islands ({@link #midden}: cell {@link #RASTER} blocks, the wolkje's own box {@link #HALF} blocks
 * around its middle, so two wolkjes never touch). There the dream's no-building rule does not hold for its owner and the
 * friends they allowed ({@link #magBouwen}); everyone else is a visitor (looks, no building, no chests). A new wolkje starts
 * with a little droomhuisje, a nestje and a maanlampje ({@link #bouwStart}); its steiger floats you back to the plein.
 * Kept in the dream level's saved data ({@link Stand}).
 */
public final class Wolkjes {
    /** The grid: wolkje number i sits in column i % KOLOMMEN, row i / KOLOMMEN, far away from the islands (they end near 250). */
    public static final int START = 1024, RASTER = 64, KOLOMMEN = 32, Y = DroomEilanden.Y;
    /** Half the size of a wolkje's own box (x and z): building is allowed within it, 16 blocks of sky stay between two boxes. */
    public static final int HALF = 24, ONDER = 40, BOVEN = 200;
    /** The start cloud's radius and its steiger and arrival (relative to the middle). */
    static final int STRAAL = 12;
    static final BlockPos STEIGER = new BlockPos(0, 1, STRAAL - 1), AANKOMST = new BlockPos(0, 1, 7);

    private Wolkjes() {
    }

    // =====================================================================================================================
    // the saved wolkjes
    // =====================================================================================================================

    /** One player's wolkje: its number on the grid, the friends who may build there, built yet. */
    public static final class Wolkje {
        final int nummer;
        final Set<UUID> vrienden = new HashSet<>();
        boolean gebouwd;

        Wolkje(int nummer) {
            this.nummer = nummer;
        }

        public int nummer() {
            return nummer;
        }

        public Set<UUID> vrienden() {
            return vrienden;
        }
    }

    public static final class Stand extends SavedData {
        public static final SavedDataType<Stand> TYPE = GuhSavedData.tagType("dromen_wolkjes", Stand::new, Stand::load, Stand::save);
        final Map<UUID, Wolkje> wolkjes = new HashMap<>();
        final Map<Integer, UUID> eigenaars = new HashMap<>();
        int volgende;
        /** Where the plein's wolkje brievenbusje stands (null: not placed yet). */
        @Nullable
        BlockPos bus;

        public Stand() {
        }

        private static Stand load(CompoundTag tag) {
            Stand s = new Stand();
            s.volgende = tag.getIntOr("Volgende", 0);
            CompoundTag w = tag.getCompoundOrEmpty("Wolkjes");
            for (String k : w.keySet()) {
                CompoundTag t = w.getCompoundOrEmpty(k);
                UUID id;
                try {
                    id = UUID.fromString(k);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                Wolkje wolkje = new Wolkje(t.getIntOr("Nummer", 0));
                wolkje.gebouwd = t.getBooleanOr("Gebouwd", false);
                for (var v : t.getListOrEmpty("Vrienden")) {
                    v.asString().ifPresent(str -> {
                        try {
                            wolkje.vrienden.add(UUID.fromString(str));
                        } catch (IllegalArgumentException ignored) {
                            // (a broken entry: skipped)
                        }
                    });
                }
                s.wolkjes.put(id, wolkje);
                s.eigenaars.put(wolkje.nummer, id);
                s.volgende = Math.max(s.volgende, wolkje.nummer + 1);
            }
            if (tag.contains("Bus")) {
                s.bus = BlockPos.of(tag.getLongOr("Bus", 0L));
            }
            return s;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Volgende", volgende);
            CompoundTag w = new CompoundTag();
            wolkjes.forEach((id, wolkje) -> {
                CompoundTag t = new CompoundTag();
                t.putInt("Nummer", wolkje.nummer);
                t.putBoolean("Gebouwd", wolkje.gebouwd);
                ListTag v = new ListTag();
                wolkje.vrienden.forEach(u -> v.add(StringTag.valueOf(u.toString())));
                t.put("Vrienden", v);
                w.put(id.toString(), t);
            });
            tag.put("Wolkjes", w);
            if (bus != null) {
                tag.putLong("Bus", bus.asLong());
            }
            return tag;
        }

        /** This player's wolkje (null: none yet). */
        @Nullable
        public Wolkje van(UUID eigenaar) {
            return wolkjes.get(eigenaar);
        }

        /** This player's wolkje, a new one on the next free grid spot when there is none: never two on one spot. */
        public Wolkje maak(UUID eigenaar) {
            Wolkje w = wolkjes.get(eigenaar);
            if (w == null) {
                while (eigenaars.containsKey(volgende)) {
                    volgende++;
                }
                w = new Wolkje(volgende++);
                wolkjes.put(eigenaar, w);
                eigenaars.put(w.nummer, eigenaar);
                setDirty();
            }
            return w;
        }

        /** Whose wolkje this block belongs to (null: nobody's, or outside every wolkje box). */
        @Nullable
        public UUID eigenaarVan(BlockPos pos) {
            int nummer = nummerBij(pos);
            return nummer < 0 ? null : eigenaars.get(nummer);
        }

        /** May this player build on this block? Only on their own wolkje or on one whose owner made them a friend. */
        public boolean mag(UUID speler, BlockPos pos) {
            UUID eigenaar = eigenaarVan(pos);
            if (eigenaar == null) {
                return false;
            }
            Wolkje w = wolkjes.get(eigenaar);
            return eigenaar.equals(speler) || (w != null && w.vrienden.contains(speler));
        }

        /** Forgets this player's wolkje (test command; its spot is not handed out again, so its blocks stay alone). */
        public void vergeet(UUID eigenaar) {
            Wolkje w = wolkjes.remove(eigenaar);
            if (w != null) {
                setDirty();
            }
        }
    }

    public static Stand stand(ServerLevel droom) {
        return droom.getDataStorage().computeIfAbsent(Stand.TYPE);
    }

    // =====================================================================================================================
    // where
    // =====================================================================================================================

    /** The middle (top surface block) of wolkje number n. */
    public static BlockPos midden(int nummer) {
        return new BlockPos(START + (nummer % KOLOMMEN) * RASTER, Y, START + (nummer / KOLOMMEN) * RASTER);
    }

    /** The wolkje number whose box this block is in, or -1. */
    public static int nummerBij(BlockPos pos) {
        if (pos.getY() < ONDER || pos.getY() > BOVEN) {
            return -1;
        }
        int kol = Math.floorDiv(pos.getX() - START + RASTER / 2, RASTER), rij = Math.floorDiv(pos.getZ() - START + RASTER / 2, RASTER);
        if (kol < 0 || kol >= KOLOMMEN || rij < 0) {
            return -1;
        }
        int nummer = rij * KOLOMMEN + kol;
        BlockPos m = midden(nummer);
        return Math.abs(pos.getX() - m.getX()) <= HALF && Math.abs(pos.getZ() - m.getZ()) <= HALF ? nummer : -1;
    }

    /** The world spot of a block relative to wolkje number n's middle (centre of the block). */
    static Vec3 wereld(int nummer, BlockPos rel) {
        return Vec3.atBottomCenterOf(midden(nummer).offset(rel));
    }

    // =====================================================================================================================
    // the rules (DroomRegels hooks)
    // =====================================================================================================================

    /** DroomRegels.MAG_HIER: building on your own (or a friend's) wolkje in the dream. */
    static boolean magBouwen(Player p, BlockPos pos) {
        if (!(p.level() instanceof ServerLevel level) || !DromenFeature.isDroom(level)) {
            return false;
        }
        return stand(level).mag(p.getUUID(), pos);
    }

    /** DroomRegels.PLEK: "wolkje:<owner>" while standing on a wolkje, else null. */
    @Nullable
    static String plek(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) {
            return null;
        }
        UUID eigenaar = stand(level).eigenaarVan(p.blockPosition().below());
        return eigenaar == null ? null : "wolkje:" + eigenaar;
    }

    /** DroomRegels.TERUG: fell off a wolkje: the Blub bubble puts you back on its arrival spot. */
    static boolean terug(ServerPlayer p, String plek) {
        if (!plek.startsWith("wolkje:") || !(p.level() instanceof ServerLevel level)) {
            return false;
        }
        UUID eigenaar;
        try {
            eigenaar = UUID.fromString(plek.substring("wolkje:".length()));
        } catch (IllegalArgumentException e) {
            return false;
        }
        Wolkje w = stand(level).van(eigenaar);
        if (w == null) {
            return false;
        }
        level.sendParticles(ParticleTypes.BUBBLE, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.8, 0.6, 0.1);
        level.playSound(null, p.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.PLAYERS, 1f, 1.4f);
        zetOp(p, level, w);
        p.sendOverlayMessage(Component.translatable("gui.guhs.dromen.regels.bubbel").withStyle(ChatFormatting.AQUA));
        return true;
    }

    // =====================================================================================================================
    // going there
    // =====================================================================================================================

    /** May this player have a wolkje? (After Snoes' story; or creative, for building and testing.) */
    public static boolean magEenWolkje(ServerPlayer p) {
        return DroomVoortgang.finaleKlaar(p) || p.getAbilities().instabuild;
    }

    /** Float this (dreaming) player to their own wolkje (made and built the first time). False when it can't (yet). */
    public static boolean naarEigen(ServerPlayer p) {
        if (!magEenWolkje(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.droomwolkje.nog_niet").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        return naar(p, p.getUUID());
    }

    /** Float this player to the wolkje of that owner (as a visitor when it isn't theirs). False when there is none. */
    public static boolean naar(ServerPlayer p, UUID eigenaar) {
        if (!DroomReis.inDroom(p) && !DroomReis.naarDroom(p, DroomEilanden.PLEIN)) {
            return false;
        }
        ServerLevel level = (ServerLevel) p.level();
        Stand s = stand(level);
        Wolkje w = eigenaar.equals(p.getUUID()) ? s.maak(eigenaar) : s.van(eigenaar);
        if (w == null) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.droomwolkje.geen_wolkje").withStyle(ChatFormatting.GRAY));
            return false;
        }
        zorg(level, s, w);
        level.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY() + 1, p.getZ(), 30, 0.6, 0.8, 0.6, 0.05);
        nl.juiced.guhs.feature.droomsfeer.DroomsfeerFeature.fade(p, nl.juiced.guhs.feature.droomsfeer.DroomsfeerFeature.Soort.ZWEEF, "wolkje");
        zetOp(p, level, w);
        level.playSound(null, p.blockPosition(), SoundEvents.BUBBLE_POP, SoundSource.PLAYERS, 1f, 1.1f);
        if (!eigenaar.equals(p.getUUID())) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.droomwolkje.bezoeker").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    private static void zetOp(ServerPlayer p, ServerLevel level, Wolkje w) {
        Vec3 plek = wereld(w.nummer, AANKOMST);
        p.stopRiding();
        p.teleportTo(level, plek.x, plek.y, plek.z, Set.of(), 180f, 0f, true);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        p.getPersistentData().putLong(DroomwolkjeFeature.AANGEKOMEN, level.getGameTime());
    }

    /** Builds the start cloud the first time (a wolkje that was built is never built over again: the owner's work stays). */
    static void zorg(ServerLevel level, Stand s, Wolkje w) {
        if (!w.gebouwd) {
            bouwStart(level, midden(w.nummer));
            w.gebouwd = true;
            s.setDirty();
        }
    }

    // =====================================================================================================================
    // the start cloud
    // =====================================================================================================================

    private static BlockState blok(String id, Block anders) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(anders).defaultBlockState();
    }

    /** A round cloud with a little droomhuisje (walls of cloud, bubble-glass windows, a fluffy roof), a nestje and a maanlampje inside, and a steiger. */
    static void bouwStart(ServerLevel level, BlockPos m) {
        BlockState top = blok("guhs:pluiswolblok", Blocks.PINK_WOOL), onder = blok("guhs:wolkenblok_roze", Blocks.PINK_WOOL),
                wit = blok("guhs:wolkenblok_wit", Blocks.WHITE_WOOL), muur = blok("guhs:wolkenblok_wit", Blocks.WHITE_WOOL),
                raam = blok("guhs:zeepbelglas", Blocks.GLASS), dak = blok("guhs:pluisdak", Blocks.PINK_WOOL),
                vloer = blok("guhs:droomzandblok", Blocks.WHITE_WOOL), nest = blok("guhs:guhnestje", Blocks.HAY_BLOCK),
                lamp = blok("guhs:maanlampje_maan", Blocks.LANTERN), bel = blok("guhs:zeepbel", Blocks.LIGHT_BLUE_STAINED_GLASS),
                bloem = blok("guhs:maanbloemetje", Blocks.ALLIUM);
        // the cloud
        for (int dx = -STRAAL; dx <= STRAAL; dx++) {
            for (int dz = -STRAAL; dz <= STRAAL; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > STRAAL + 0.3) {
                    continue;
                }
                int diepte = 1 + (int) Math.round((1 - d / (STRAAL + 0.5)) * 6);
                for (int dy = -diepte; dy <= 0; dy++) {
                    level.setBlock(m.offset(dx, dy, dz), dy == 0 ? top : dy == -diepte ? wit : onder, 2 | 16);
                }
            }
        }
        // the droomhuisje: 5x5, door to the south (towards the arrival)
        BlockPos h = m.offset(0, 0, -3);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(h.offset(dx, 0, dz), vloer, 2 | 16);
                boolean rand = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                for (int dy = 1; dy <= 3; dy++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (rand) {
                        boolean hoek = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                        s = !hoek && dy == 2 ? raam : muur;
                        if (dz == 2 && dx == 0 && dy <= 2) {
                            s = Blocks.AIR.defaultBlockState();   // the doorway
                        }
                    }
                    level.setBlock(h.offset(dx, dy, dz), s, 2 | 16);
                }
                level.setBlock(h.offset(dx, 4, dz), dak, 2 | 16);
            }
        }
        level.setBlock(h.offset(0, 5, 0), dak, 2 | 16);
        level.setBlock(h.offset(-1, 1, -1), nest, 2 | 16);
        level.setBlock(h.offset(1, 1, -1), lamp, 2 | 16);
        // some flowers and the steiger with its big bubble
        for (int[] b : new int[][]{{-6, 3}, {6, 2}, {-4, 8}, {5, 7}, {-8, -2}, {8, -4}}) {
            level.setBlock(m.offset(b[0], 1, b[1]), bloem, 2 | 16);
        }
        BlockPos s = m.offset(STEIGER).below();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            level.setBlock(s.relative(d), wit, 2 | 16);
        }
        level.setBlock(s, wit, 2 | 16);
        level.setBlock(s.above(4), bel, 2 | 16);
    }

    /** Is this player standing on the steiger of the wolkje they are on? */
    static boolean opSteiger(ServerPlayer p) {
        int nummer = nummerBij(p.blockPosition().below());
        if (nummer < 0) {
            return false;
        }
        Vec3 w = wereld(nummer, STEIGER);
        double dx = p.getX() - w.x, dz = p.getZ() - w.z;
        return dx * dx + dz * dz < 1.3 * 1.3 && Math.abs(p.getY() - w.y) < 2;
    }

    /** (the plein's wolkje brievenbusje faces the arrival spot) */
    static BlockState bus(Direction kijkt) {
        return DroomwolkjeFeature.BUS.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, kijkt);
    }
}
