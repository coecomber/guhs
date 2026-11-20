package nl.juiced.guhs.feature.erfenis;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.erfenis.vorm.DecoWezens;
import nl.juiced.guhs.feature.slaapzorg.SlaapBlokken;
import nl.juiced.guhs.feature.slaapzorg.Slapen;

/**
 * 1.10.10: the Muziekdoosje on the Muziektent (every level, L1..L10) plays a dance tune in De Erfenis. Right-click it (the
 * village's owner) and the "Guhdansje" plays at the box for 30 seconds (one positional sound, {@link #DANSJE}); every guh
 * within {@link #BEREIK} blocks dances ({@link nl.juiced.guhs.feature.emotes.GuhEmotes#startMuziek}: the Muziektent's show
 * guhs without AI too), the village's other show animals hop along, and notes pop out of the box. When the song is over
 * they all go back to normal.
 * <p>
 * The state lives in the block ({@link SlaapBlokken.Speeldoos#SPEELT}: {@link #STAPPEN} steps of {@link #STAP} ticks, each a
 * scheduled block tick), so a second click does nothing while it plays ("speelt al"), and it survives a chunk unload (the
 * scheduled tick is saved with the chunk; the dancers are collected again every step, nothing is kept per guh). Outside De
 * Erfenis the same block stays the slaapzorg lullaby box (Droomwinkel): the guhs fall asleep, they don't dance.
 */
public final class ErfenisMuziek {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    /** The dance tune (tools/features/erfenis_muziek_geluid.py: 128 BPM, 16 bars, 30 seconds). */
    public static final DeferredHolder<SoundEvent, SoundEvent> DANSJE = SOUNDS.register("muziekdoosje.dansje",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("muziekdoosje.dansje")));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    /**
     * 1.10.10: the dance tune as a music disc (jukebox song {@code guhs:dansje}: the same sound event {@link #DANSJE}, 31 s).
     * Found now and then in the chests of the Ballonfestival, the guh picnic and the Grotdisco (tools/features/erfenis_muziek.py). Every guh
     * near a playing jukebox dances anyway (GuhEmotes), so the tune does at home what it does on the Muziektent.
     */
    public static final DeferredItem<Item> MUZIEKPLAAT = ITEMS.registerItem("music_disc_dansje", p -> new Item(p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.music_disc_dansje.lore").withStyle(ChatFormatting.GRAY));
        }
    }, () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, Guhs.id("dansje"))));

    /** One step (ticks): notes from the box, the dancers collected again. */
    public static final int STAP = 50;
    /** Steps of a song (fits in {@link SlaapBlokken.Speeldoos#SPEELT}, 0..12): 12 x 50 ticks = the tune's 30 seconds. */
    public static final int STAPPEN = 12;
    public static final int DUUR = STAP * STAPPEN;
    /** How far the guhs dance along (the Muziektent itself is 9x9 around the box, so all of it). */
    public static final double BEREIK = 15;
    /** The volume: 1.25 carries it about 20 blocks, a bit past the dancers. */
    static final float VOLUME = 1.25f;

    /** Boxes started as a dance box outside De Erfenis (the game tests); per level. */
    private static final Map<ServerLevel, Set<BlockPos>> GEFORCEERD = Collections.synchronizedMap(new WeakHashMap<>());

    private ErfenisMuziek() {
    }

    static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
        ITEMS.register(modBus);
    }

    /** Does a Muziekdoosje at this spot play the dance tune (in De Erfenis, or started as one)? */
    public static boolean dansHier(Level level, BlockPos pos) {
        if (level.dimension() == Plot.DIM) {
            return true;
        }
        Set<BlockPos> s = level instanceof ServerLevel sl ? GEFORCEERD.get(sl) : null;
        return s != null && s.contains(pos);
    }

    /** (the game tests) Plays the dance tune here whatever the dimension; ends with the song. */
    public static void forceer(ServerLevel level, BlockPos pos) {
        GEFORCEERD.computeIfAbsent(level, l -> new HashSet<>()).add(pos.immutable());
    }

    // ================================================================================================= the click

    /**
     * A right-click on a Muziekdoosje in De Erfenis (Erfenis.opKlikBlok; the dimension cancels every block click itself). Only
     * the village's owner winds it up (like the Detectivebureau and the Marktplein); a second click while it plays says so.
     */
    static void klik(ServerPlayer p, ServerLevel level, BlockPos pos, BlockState state, boolean mag) {
        if (!(state.getBlock() instanceof SlaapBlokken.Speeldoos doos)) {
            return;
        }
        if (!mag) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.muziekdoosje.niet_eigen"));
        } else if (doos.speelt(level, pos, state)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.muziekdoosje.speelt_al"));
        } else {
            doos.speel(level, pos, state);
            p.sendOverlayMessage(Component.translatable("gui.guhs.muziekdoosje.dansen"));
        }
    }

    // ================================================================================================= the song

    /** The song starts (Speeldoos.speel): the tune at the box, a burst of notes, the dancers. Returns how many guhs dance. */
    public static int start(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, DANSJE.get(), SoundSource.BLOCKS, VOLUME, 1f);
        noten(level, pos, 4);
        return dansers(level, pos, 0);
    }

    /** Step {@code stap} (1..{@link #STAPPEN}-1) of a playing song: notes, and everyone around (again) dancing. */
    public static void stap(ServerLevel level, BlockPos pos, int stap) {
        noten(level, pos, 3);
        dansers(level, pos, stap);
    }

    /** The song is over: every dancer around goes back to normal. */
    public static void einde(ServerLevel level, BlockPos pos) {
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, gebied(pos), g -> g.emotes.dansOpMuziek())) {
            guh.emotes.stopMuziek();
        }
        Set<BlockPos> s = GEFORCEERD.get(level);
        if (s != null) {
            s.remove(pos);
        }
    }

    private static AABB gebied(BlockPos pos) {
        return new AABB(pos).inflate(BEREIK, 6, BEREIK);
    }

    /**
     * Every guh within {@link #BEREIK} dances until a bit after the next step (so it never stops between two steps, and
     * stops by itself if the box is gone). Two dances (the vads wiggle and the twirl with little hops), so a crowd doesn't
     * move as one; halfway they swap. A sleeping guh sleeps on. The show animals of the village (not the guhs, not the NPCs)
     * hop along now and then. One box query per step, nothing per tick.
     */
    static int dansers(ServerLevel level, BlockPos pos, int stap) {
        Vec3 midden = Vec3.atCenterOf(pos);
        double r2 = BEREIK * BEREIK;
        int n = 0;
        for (Entity e : level.getEntities((Entity) null, gebied(pos), x -> x.isAlive() && x.distanceToSqr(midden) <= r2)) {
            if (e instanceof GuhEntity guh) {
                if (Slapen.slaapt(guh)) {
                    continue;
                }
                boolean wissel = stap >= STAPPEN / 2;
                Emote dans = ((guh.getId() & 1) == 0) != wissel ? Emote.DANSEN : Emote.KNUFFELDANSJE;
                if (guh.emotes.startMuziek(dans, STAP + 15)) {
                    n++;
                }
            } else if (e instanceof Mob m && !(e instanceof GuhNpcEntity) && DecoWezens.isDeco(e) && m.onGround()
                    && level.getRandom().nextInt(3) > 0) {
                m.push(0, 0.38, 0);   // a happy little hop on the beat
            }
        }
        return n;
    }

    /** A few music notes from the box (their colour is the x speed of a note particle). */
    static void noten(ServerLevel level, BlockPos pos, int aantal) {
        var r = level.getRandom();
        for (int i = 0; i < aantal; i++) {
            level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 0.8 + r.nextDouble() * 0.4,
                    pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, r.nextInt(25) / 24.0, 0, 0, 1.0);
        }
    }
}
