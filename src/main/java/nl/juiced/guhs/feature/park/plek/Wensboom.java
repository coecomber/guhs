package nl.juiced.guhs.feature.park.plek;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * 1.4.2 (park-plekjes b): the wensboom (plekje wensboom). Its trunk is {@link Stam} (guhs:park_wensstam, blossom bark with
 * ribbons); its branches are horizontal logs, and under them hang the wishes.
 * <ul>
 *   <li>Right-click the trunk with a renamed paper (a name given on an anvil = your wish): the paper is used up and the wish
 *       hangs on a little cherry hanging sign under a branch ({@link #hang}): the wish on the front, "Wens van" and your name
 *       on the back. Everybody sees it; it is waxed (nobody can edit it) and protected with the park.</li>
 *   <li>Safe text: only the plain characters of the name (no formatting codes, no control or direction characters, spaces
 *       folded), at most {@value #MAX_TEKENS} characters, wrapped over the sign's four lines; it is put on the sign as a
 *       literal (never parsed).</li>
 *   <li>At most {@value #PER_SPELER} wishes per player per tree: a new one takes the place of your oldest. A free spot first,
 *       then the spot of an example wish of the template, then the oldest wish of the tree.</li>
 *   <li>Empty hand: how many wishes hang there; sneaking with an empty hand: take your own wishes down again.</li>
 * </ul>
 * Saved per dimension ({@link Wensen}, SavedData guhs:parkb_wensen): per tree (the lowest north-west block of its trunk)
 * which wish hangs where, whose, since when. An op can clear a tree: /guhs parkb wensen wis.
 */
public final class Wensboom {
    public static final int PER_SPELER = 2, MAX_TEKENS = 40, REGEL = 11;
    /** How far from the trunk wishes may hang (blocks: sideways, up). */
    public static final int STRAAL = 6, HOOG = 8;

    private Wensboom() {
    }

    // =====================================================================================================================
    /** The trunk of the wish tree. */
    public static class Stam extends Block {
        public static final com.mojang.serialization.MapCodec<Stam> CODEC = simpleCodec(Stam::new);

        public Stam(Properties properties) {
            super(properties);
        }

        @Override
        protected com.mojang.serialization.MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            if (!stack.is(Items.PAPER)) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
                hang(server, pos, sp, stack);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
                if (sp.isSecondaryUseActive()) {
                    haalWeg(server, pos, sp);
                } else {
                    sp.sendOverlayMessage(Component.translatable("gui.guhs.parkb.wens.info", tel(server, pos)).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =====================================================================================================================
    /** The tree of this trunk block: the lowest, north-west-most trunk block (its key in {@link Wensen}). */
    public static BlockPos boom(Level level, BlockPos stam) {
        BlockPos p = stam;
        while (level.getBlockState(p.below()).getBlock() instanceof Stam) {
            p = p.below();
        }
        BlockPos best = p;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos q = p.offset(dx, 0, dz);
                if (level.getBlockState(q).getBlock() instanceof Stam
                        && (q.getX() < best.getX() || (q.getX() == best.getX() && q.getZ() < best.getZ()))) {
                    best = q;
                }
            }
        }
        return best.immutable();
    }

    /** Where wishes can hang: under a branch (a horizontal log) near the tree, the spot free or a hanging sign. */
    public static List<BlockPos> plekjes(Level level, BlockPos boom) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(boom.offset(-STRAAL, 1, -STRAAL), boom.offset(STRAAL + 1, HOOG, STRAAL + 1))) {
            BlockState boven = level.getBlockState(p.above());
            if (!boven.hasProperty(BlockStateProperties.AXIS) || boven.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y) {
                continue;
            }
            BlockState hier = level.getBlockState(p);
            if (hier.isAir() || hier.getBlock() instanceof CeilingHangingSignBlock) {
                out.add(p.immutable());
            }
        }
        out.sort(Comparator.comparingInt((BlockPos q) -> q.getY()).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        return out;
    }

    /** How many wishes hang in this tree (the players' and the examples). */
    public static int tel(ServerLevel level, BlockPos stam) {
        int n = 0;
        for (BlockPos p : plekjes(level, boom(level, stam))) {
            n += level.getBlockState(p).getBlock() instanceof CeilingHangingSignBlock ? 1 : 0;
        }
        return n;
    }

    /** The safe text of a wish: plain characters, spaces folded, at most {@link #MAX_TEKENS}; "" when nothing is left. */
    public static String schoon(String naam) {
        StringBuilder sb = new StringBuilder();
        boolean spatie = false;
        for (int i = 0; i < naam.length(); ) {
            int c = naam.codePointAt(i);
            i += Character.charCount(c);
            if (c == '§') {                          // (a formatting code: skip it and its letter)
                if (i < naam.length()) {
                    i += Character.charCount(naam.codePointAt(i));
                }
                continue;
            }
            int type = Character.getType(c);
            if (Character.isISOControl(c) || type == Character.FORMAT || type == Character.PRIVATE_USE || type == Character.SURROGATE
                    || type == Character.UNASSIGNED) {
                continue;
            }
            if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                spatie = sb.length() > 0;
                continue;
            }
            if (spatie) {
                sb.append(' ');
                spatie = false;
            }
            sb.appendCodePoint(c);
            if (sb.codePointCount(0, sb.length()) >= MAX_TEKENS) {
                break;
            }
        }
        return sb.toString().trim();
    }

    /** The wish over four lines of at most {@link #REGEL} characters (words kept whole where they fit). */
    public static List<String> regels(String tekst) {
        List<String> out = new ArrayList<>();
        StringBuilder regel = new StringBuilder();
        for (String woord : tekst.split(" ")) {
            while (woord.codePointCount(0, woord.length()) > REGEL) {      // (a long word: cut it)
                if (regel.length() > 0) {
                    out.add(regel.toString());
                    regel.setLength(0);
                }
                int knip = woord.offsetByCodePoints(0, REGEL);
                out.add(woord.substring(0, knip));
                woord = woord.substring(knip);
            }
            if (woord.isEmpty()) {
                continue;
            }
            int lengte = regel.codePointCount(0, regel.length());
            if (lengte > 0 && lengte + 1 + woord.codePointCount(0, woord.length()) > REGEL) {
                out.add(regel.toString());
                regel.setLength(0);
            }
            if (regel.length() > 0) {
                regel.append(' ');
            }
            regel.append(woord);
        }
        if (regel.length() > 0) {
            out.add(regel.toString());
        }
        if (out.size() > SignText.LINES) {
            List<String> kort = new ArrayList<>(out.subList(0, SignText.LINES));
            String laatste = kort.get(SignText.LINES - 1);
            kort.set(SignText.LINES - 1, (laatste.codePointCount(0, laatste.length()) >= REGEL
                    ? laatste.substring(0, laatste.offsetByCodePoints(0, REGEL - 1)) : laatste) + "…");
            return kort;
        }
        return out;
    }

    /** A renamed paper is hung in the tree of this trunk. */
    public static void hang(ServerLevel level, BlockPos stam, ServerPlayer p, ItemStack papier) {
        Component naam = papier.get(DataComponents.CUSTOM_NAME);
        String tekst = naam == null ? "" : schoon(naam.getString());
        if (tekst.isEmpty()) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.parkb.wens.geen_naam").withStyle(ChatFormatting.GOLD));
            return;
        }
        BlockPos boom = boom(level, stam);
        Wensen data = Wensen.get(level);
        List<BlockPos> plekjes = plekjes(level, boom);
        List<Wens> wensen = data.van(boom, level);
        BlockPos plek = null;
        boolean vervangen = false;
        List<Wens> mijn = wensen.stream().filter(w -> w.speler().equals(p.getUUID())).sorted(Comparator.comparingLong(Wens::tijd)).toList();
        if (mijn.size() >= PER_SPELER) {
            plek = mijn.get(0).plek();
            vervangen = true;
        }
        if (plek == null) {
            for (BlockPos q : plekjes) {
                if (level.getBlockState(q).isAir()) {
                    plek = q;
                    break;
                }
            }
        }
        if (plek == null) {
            for (BlockPos q : plekjes) {
                if (wensen.stream().noneMatch(w -> w.plek().equals(q))) {
                    plek = q;           // (an example wish of the template)
                    break;
                }
            }
        }
        if (plek == null && !wensen.isEmpty()) {
            plek = wensen.stream().min(Comparator.comparingLong(Wens::tijd)).orElseThrow().plek();
        }
        if (plek == null) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.parkb.wens.vol").withStyle(ChatFormatting.GOLD));
            return;
        }
        if (!zetBordje(level, plek, tekst, p.getGameProfile().name())) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.parkb.wens.vol").withStyle(ChatFormatting.GOLD));
            return;
        }
        BlockPos waar = plek;
        data.zet(boom, new Wens(waar, p.getUUID(), p.getGameProfile().name(), tekst, level.getGameTime()));
        papier.consume(1, p);
        level.playSound(null, waar, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1.1f);
        level.playSound(null, waar, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.6f, 1.3f);
        level.sendParticles(ParticleTypes.HEART, waar.getX() + 0.5, waar.getY() + 0.3, waar.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, waar.getX() + 0.5, waar.getY() + 0.5, waar.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
        if (vervangen) {
            p.sendSystemMessage(Component.translatable("gui.guhs.parkb.wens.vervangen", PER_SPELER).withStyle(ChatFormatting.GRAY));
        }
        p.sendOverlayMessage(Component.translatable("gui.guhs.parkb.wens.gehangen", tekst).withStyle(ChatFormatting.LIGHT_PURPLE));
        GuhAdvancements.grant(p, "parkb_wens");
    }

    /** The little hanging sign with this wish at this spot (turned along the branch above it). False when it can't hang there. */
    static boolean zetBordje(ServerLevel level, BlockPos plek, String tekst, String van) {
        BlockState boven = level.getBlockState(plek.above());
        int draai = boven.hasProperty(BlockStateProperties.AXIS) && boven.getValue(BlockStateProperties.AXIS) == Direction.Axis.Z ? 4 : 0;
        BlockState bordje = Blocks.CHERRY_HANGING_SIGN.defaultBlockState().setValue(CeilingHangingSignBlock.ROTATION, draai)
                .setValue(CeilingHangingSignBlock.ATTACHED, false);
        if (!bordje.canSurvive(level, plek)) {
            return false;
        }
        level.setBlock(plek, bordje, Block.UPDATE_ALL);
        if (!(level.getBlockEntity(plek) instanceof SignBlockEntity sign)) {
            return false;
        }
        List<String> regels = regels(tekst);
        Component[] voor = new Component[SignText.LINES];
        for (int i = 0; i < SignText.LINES; i++) {
            voor[i] = Component.literal(i < regels.size() ? regels.get(i) : "");
        }
        Component[] achter = {Component.translatable("gui.guhs.parkb.wens.van"), Component.literal(van), Component.empty(), Component.empty()};
        sign.setText(new SignText(voor, voor, DyeColor.BLACK, false), true);
        sign.setText(new SignText(achter, achter, DyeColor.GRAY, false), false);
        sign.setWaxed(true);
        sign.setChanged();
        level.sendBlockUpdated(plek, bordje, bordje, Block.UPDATE_ALL);
        return true;
    }

    /** Sneak + empty hand on the trunk: your own wishes in this tree come down again. */
    public static int haalWeg(ServerLevel level, BlockPos stam, ServerPlayer p) {
        BlockPos boom = boom(level, stam);
        int n = Wensen.get(level).haalWeg(level, boom, w -> w.speler().equals(p.getUUID()));
        p.sendOverlayMessage(Component.translatable("gui.guhs.parkb.wens.weg").withStyle(ChatFormatting.GRAY));
        return n;
    }

    // =====================================================================================================================
    /** One wish: where it hangs, whose, the text, since when (game time). */
    public record Wens(BlockPos plek, UUID speler, String naam, String tekst, long tijd) {
    }

    /** The wishes of every wish tree of a dimension. */
    public static class Wensen extends SavedData {
        public static final SavedDataType<Wensen> TYPE = GuhSavedData.tagType("parkb_wensen", Wensen::new, Wensen::load, Wensen::save);
        private final Map<Long, List<Wens>> bomen = new HashMap<>();

        public static Wensen get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        /** The wishes of this tree that still hang (a sign that is gone takes its wish with it). */
        public List<Wens> van(BlockPos boom, Level level) {
            List<Wens> lijst = bomen.getOrDefault(boom.asLong(), List.of());
            List<Wens> nog = new ArrayList<>();
            for (Wens w : lijst) {
                if (!level.isLoaded(w.plek()) || level.getBlockState(w.plek()).getBlock() instanceof CeilingHangingSignBlock) {
                    nog.add(w);
                }
            }
            if (nog.size() != lijst.size()) {
                bomen.put(boom.asLong(), nog);
                setDirty();
            }
            return List.copyOf(nog);
        }

        void zet(BlockPos boom, Wens wens) {
            List<Wens> lijst = new ArrayList<>(bomen.getOrDefault(boom.asLong(), List.of()));
            lijst.removeIf(w -> w.plek().equals(wens.plek()));
            lijst.add(wens);
            bomen.put(boom.asLong(), lijst);
            setDirty();
        }

        /** Takes these wishes of the tree down (their signs too); how many. */
        public int haalWeg(ServerLevel level, BlockPos boom, java.util.function.Predicate<Wens> welke) {
            List<Wens> lijst = new ArrayList<>(bomen.getOrDefault(boom.asLong(), List.of()));
            int n = 0;
            for (Wens w : List.copyOf(lijst)) {
                if (welke.test(w)) {
                    if (level.getBlockState(w.plek()).getBlock() instanceof CeilingHangingSignBlock) {
                        level.setBlock(w.plek(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                    lijst.remove(w);
                    n++;
                }
            }
            if (n > 0) {
                bomen.put(boom.asLong(), lijst);
                setDirty();
            }
            return n;
        }

        /** (tests, commands) every tree with wishes. */
        public List<BlockPos> bomen() {
            return bomen.keySet().stream().map(BlockPos::of).toList();
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            ListTag lijst = new ListTag();
            bomen.forEach((boom, wensen) -> {
                for (Wens w : wensen) {
                    CompoundTag t = new CompoundTag();
                    t.putLong("Boom", boom);
                    t.putLong("Plek", w.plek().asLong());
                    t.store("Speler", UUIDUtil.CODEC, w.speler());
                    t.putString("Naam", w.naam());
                    t.putString("Tekst", w.tekst());
                    t.putLong("Tijd", w.tijd());
                    lijst.add(t);
                }
            });
            tag.put("Wensen", lijst);
            return tag;
        }

        private static Wensen load(CompoundTag tag) {
            Wensen d = new Wensen();
            for (Tag t : tag.getListOrEmpty("Wensen")) {
                if (t instanceof CompoundTag c) {
                    UUID speler = c.read("Speler", UUIDUtil.CODEC).orElse(null);
                    if (speler == null) {
                        continue;
                    }
                    d.bomen.computeIfAbsent(c.getLongOr("Boom", 0L), k -> new ArrayList<>()).add(new Wens(BlockPos.of(c.getLongOr("Plek", 0L)),
                            speler, c.getStringOr("Naam", ""), schoon(c.getStringOr("Tekst", "")), c.getLongOr("Tijd", 0L)));
                }
            }
            return d;
        }
    }

    /** (tests) the wish on this sign's front, as it is written (the lines joined with spaces). */
    @Nullable
    public static String bordTekst(Level level, BlockPos plek) {
        if (!(level.getBlockEntity(plek) instanceof SignBlockEntity sign)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Component c : sign.getFrontText().getMessages(false)) {
            if (!c.getString().isEmpty()) {
                sb.append(sb.length() > 0 ? " " : "").append(c.getString());
            }
        }
        return sb.toString();
    }
}
