package nl.juiced.guhs.feature.sterrenveld;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.feature.dromen.DromenFeature;

/**
 * 1.8.0 Guhdromenland, the island "sterren": het Sterrenkussenveld (BOUWPLAN_180.md part E, ontwerp r03). The island itself
 * is a template (tools/features/dromen_sterren.py: data/guhs/structure/dromen/eiland_sterren.nbt + its json), stamped by
 * {@link DroomEilanden}. Here:
 * <ul>
 *   <li>the stuiterkussens ({@link Stuiterkussen}, 8 colours): the spiral of giant pillows you bounce up;</li>
 *   <li>{@link VallendeSter}: little stars keep falling over the island while somebody is near; catch one = droomzand;</li>
 *   <li>the big falling star of Snoes on top of the spiral (json key "ster"): get close = the story piece
 *       {@link DroomVoortgang.Stukje#STER} (his wish: "a little place of my own");</li>
 *   <li>creatures of the island (Blubs, the kussengevecht-guhs) that tumble off are put back on the path.</li>
 * </ul>
 */
public final class SterrenveldFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final List<String> KLEUREN = List.of("boter", "vanille", "roze", "hemelsblauw", "lavendel", "perzik", "aqua", "wolkje");
    public static final List<DeferredBlock<Stuiterkussen>> STUITERKUSSENS = new ArrayList<>();

    static {
        for (String k : KLEUREN) {
            String id = "stuiterkussen_" + k;
            DeferredBlock<Stuiterkussen> b = BLOCKS.registerBlock(id, Stuiterkussen::new,
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f).sound(SoundType.WOOL));
            ITEMS.registerItem(id, p -> new BlockItem(b.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("block.guhs." + id + ".lore").withStyle(ChatFormatting.GRAY));
                }
            }, () -> new Item.Properties().useBlockDescriptionPrefix());
            STUITERKUSSENS.add(b);
        }
    }

    /** What a falling star looks like in each phase (only for its renderer; not in the creative tab). */
    public static final DeferredItem<Item> LOOK_VALT = ITEMS.registerSimpleItem("droomsterretje");
    public static final DeferredItem<Item> LOOK_GLOED = ITEMS.registerSimpleItem("droomsterretje_gloed");
    public static final DeferredItem<Item> LOOK_VAAG = ITEMS.registerSimpleItem("droomsterretje_vaag");

    public static final DeferredHolder<EntityType<?>, EntityType<VallendeSter>> VALLENDE_STER = ENTITY_TYPES.register("droomsterretje",
            () -> EntityType.Builder.<VallendeSter>of(VallendeSter::new, MobCategory.MISC).sized(0.7f, 0.7f).clientTrackingRange(8)
                    .updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("droomsterretje"))));

    /** How many little stars may be about at once, how often a new one may fall, how far around the island they fall. */
    static final int MAX_STERREN = 6, VAL_KANS = 6, STRAAL = 30;
    /** How close you have to come to the big star's middle, and how near the island somebody must be for stars to fall. */
    static final double STER_BEREIK = 4.5, BIJ_EILAND = 90;
    private static final Map<UUID, Long> AL_GEMELD = new HashMap<>();
    @Nullable
    private static Vec3 sterRel;

    private SterrenveldFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        NeoForge.EVENT_BUS.addListener(SterrenveldFeature::tick);
    }

    public static void creative(Consumer<ItemStack> output) {
        STUITERKUSSENS.forEach(b -> output.accept(new ItemStack(b.get())));
    }

    /** The big star's middle relative to the island's centre (json key "ster"), or a spot high above the middle. */
    public static synchronized Vec3 sterRel() {
        if (sterRel == null) {
            sterRel = new Vec3(1.5, 41.5, -2);
            try (InputStream in = SterrenveldFeature.class.getResourceAsStream("/data/guhs/dromen/eiland/sterren.json")) {
                if (in != null) {
                    JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (o.has("ster")) {
                        JsonArray a = o.getAsJsonArray("ster");
                        sterRel = new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
                    }
                }
            } catch (Exception e) {
                com.mojang.logging.LogUtils.getLogger().error("Sterrenkussenveld: sterren.json unreadable: {}", e.toString());
            }
        }
        return sterRel;
    }

    /** The big star in the world. */
    public static Vec3 grooteSter() {
        return DroomEilanden.wereld(DroomEilanden.STERREN, sterRel());
    }

    private static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 10 != 3) {
            return;
        }
        ServerLevel level = event.getServer().getLevel(DromenFeature.DIM);
        if (level == null || level.players().isEmpty()) {
            return;
        }
        BlockPos m = DroomEilanden.midden(DroomEilanden.STERREN);
        Vec3 midden = Vec3.atBottomCenterOf(m);
        boolean iemand = false;
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceTo(midden) < BIJ_EILAND) {
                iemand = true;
                grooteSterCheck(p);
            }
        }
        if (!iemand || DroomEilanden.gebouwd(level, DroomEilanden.STERREN) < 0) {
            return;
        }
        AABB eiland = new AABB(midden, midden).inflate(STRAAL + 40, 80, STRAAL + 40);
        if (level.getRandom().nextInt(VAL_KANS) == 0
                && level.getEntities(VALLENDE_STER.get(), eiland, e -> true).size() < MAX_STERREN) {
            double a = level.getRandom().nextDouble() * Math.PI * 2, r = Math.sqrt(level.getRandom().nextDouble()) * STRAAL;
            VallendeSter.nieuw(level, midden.add(Math.cos(a) * r, 26 + level.getRandom().nextInt(10), Math.sin(a) * r));
        }
        if (event.getServer().getTickCount() % 40 == 3) {
            terugOpHetEiland(level, eiland);
        }
    }

    /** Close to the big star: the story piece "ster" (once), with a title and a shower of sparkles. */
    static void grooteSterCheck(ServerPlayer p) {
        Vec3 ster = grooteSter();
        if (p.position().add(0, 1, 0).distanceTo(ster) > STER_BEREIK) {
            return;
        }
        long nu = p.level().getGameTime();
        Long eerder = AL_GEMELD.get(p.getUUID());
        if (eerder != null && nu - eerder < 200) {
            return;
        }
        AL_GEMELD.put(p.getUUID(), nu);
        ServerLevel level = (ServerLevel) p.level();
        level.sendParticles(ParticleTypes.END_ROD, ster.x, ster.y, ster.z, 40, 2.5, 2.5, 1.0, 0.1);
        if (DroomVoortgang.geef(p, DroomVoortgang.Stukje.STER)) {
            level.playSound(null, ster.x, ster.y, ster.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.2f);
            level.playSound(null, ster.x, ster.y, ster.z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
            ItemStack zand = new ItemStack(DroomblokkenFeature.DROOMZAND.get(), 3);
            if (!p.getInventory().add(zand)) {
                p.drop(zand, false);
            }
            title(p, Component.translatable("gui.guhs.sterren.grote_ster.titel").withStyle(ChatFormatting.GOLD),
                    Component.translatable("gui.guhs.sterren.grote_ster.sub"));
            p.sendSystemMessage(Component.translatable("gui.guhs.sterren.grote_ster.chat").withStyle(ChatFormatting.YELLOW));
        } else {
            level.playSound(null, ster.x, ster.y, ster.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.6f);
            p.sendOverlayMessage(Component.translatable("gui.guhs.sterren.grote_ster.al").withStyle(ChatFormatting.YELLOW));
        }
    }

    private static void title(ServerPlayer p, Component titel, Component sub) {
        if (p instanceof FakePlayer || p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(titel));
    }

    /** A Blub or a kussengevecht-guh that tumbled off the island floats back onto the path (the sea is no place for them). */
    static void terugOpHetEiland(ServerLevel level, AABB eiland) {
        DroomEilanden.Opzet o = DroomEilanden.opzet(DroomEilanden.STERREN);
        Vec3 terug = DroomEilanden.wereld(DroomEilanden.STERREN, o.aankomst().plek());
        AABB onder = new AABB(eiland.minX, level.getMinY(), eiland.minZ, eiland.maxX, DroomEilanden.Y - 30, eiland.maxZ);
        for (Entity e : level.getEntities((Entity) null, onder, e -> e instanceof LivingEntity && !(e instanceof ServerPlayer))) {
            e.teleportTo(terug.x, terug.y, terug.z);
            e.setDeltaMovement(Vec3.ZERO);
            e.fallDistance = 0;
            level.sendParticles(ParticleTypes.BUBBLE_POP, terug.x, terug.y + 0.5, terug.z, 8, 0.3, 0.3, 0.3, 0.02);
        }
    }
}
