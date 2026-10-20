package nl.juiced.guhs.feature.safari.quests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.SafariFeature;
import nl.juiced.guhs.feature.safari.SafariOpslag;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;

/**
 * De te vahe olifant (design 1.7): "Je kunt niet te vads zijn... maar hij is wel te VAH." Dikkie lives in his own stal
 * near the savanne drinkpoel (built by the park generator: a gate one block wide, quests.json) and no longer fits through
 * it. Click him: a diet ({@link #DIEET}: one each, given from the main hand), then the olifantenloopband next to the stal
 * ({@link #RONDES} clicks, a little pause between them). Reward: the olifantenloopband and the photo frame with the slim
 * elephant, and 150 safarimunten. Quest {@link #QUEST}: "" -> "dieet" -> "loopband" -> "klaar"; what he ate is in
 * {@link #GEGETEN} (comma list), the laps in {@link #RONDEN}.
 */
public final class VaheOlifant {
    public static final String QUEST = "vahe_olifant", GEGETEN = "vahe_olifant_dieet", RONDEN = "vahe_olifant_ren";
    public static final String TAG = "guhs_safari_dikkie";
    public static final int MUNTEN = 150, RONDES = 5;
    /** The diet: item and its lang word. */
    static final Map<String, Item> DIEET = new java.util.LinkedHashMap<>(Map.of());
    private static final Map<UUID, Long> PAUZE = new ConcurrentHashMap<>();

    static {
        DIEET.put("appel", Items.APPLE);
        DIEET.put("wortel", Items.CARROT);
        DIEET.put("meloen", Items.MELON_SLICE);
    }

    /** A right click on an entity: true when it was Dikkie. */
    static boolean klik(ServerPlayer p, Entity e) {
        if (!e.entityTags().contains(TAG)) {
            return false;
        }
        praat(p, p.getMainHandItem());
        return true;
    }

    /** The player clicks Dikkie with this stack in the main hand (also the gametests). */
    static void praat(ServerPlayer p, ItemStack hand) {
        MinecraftServer server = p.level().getServer();
        String stand = SafariSpeler.quest(server, p.getUUID(), QUEST);
        switch (stand) {
            case "" -> {
                SafariSpeler.zetQuest(server, p.getUUID(), QUEST, "dieet");
                SafariQuests.zeg(p, "gui.guhs.safari.olifant.start");
            }
            case "dieet" -> {
                List<String> gegeten = gegeten(server, p.getUUID());
                for (Map.Entry<String, Item> e : DIEET.entrySet()) {
                    if (!gegeten.contains(e.getKey()) && hand.is(e.getValue())) {
                        if (!p.getAbilities().instabuild) {
                            hand.shrink(1);
                        }
                        gegeten.add(e.getKey());
                        SafariSpeler.zetQuest(server, p.getUUID(), GEGETEN, String.join(",", gegeten));
                        p.sendSystemMessage(Component.translatable("gui.guhs.safari.olifant.hap").withStyle(ChatFormatting.GREEN));
                        p.level().playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 1f, 0.6f);
                        break;
                    }
                }
                List<String> nog = new ArrayList<>();
                for (String k : DIEET.keySet()) {
                    if (!gegeten.contains(k)) {
                        nog.add(k);
                    }
                }
                if (nog.isEmpty()) {
                    SafariSpeler.zetQuest(server, p.getUUID(), QUEST, "loopband");
                    SafariQuests.zeg(p, "gui.guhs.safari.olifant.loopband");
                } else {
                    net.minecraft.network.chat.MutableComponent lijst = Component.empty();
                    for (int i = 0; i < nog.size(); i++) {
                        if (i > 0) {
                            lijst.append(", ");
                        }
                        lijst.append(Component.translatable("gui.guhs.safari.olifant." + nog.get(i)));
                    }
                    SafariQuests.zeg(p, "gui.guhs.safari.olifant.dieet", lijst);
                }
            }
            case "loopband" -> SafariQuests.zeg(p, "gui.guhs.safari.olifant.loopband");
            default -> SafariQuests.zeg(p, "gui.guhs.safari.olifant.na");
        }
    }

    /** A right click on a block in the park: true when it was the olifantenloopband at Dikkie's stal. */
    static boolean loopband(ServerPlayer p, BlockPos pos) {
        BlockPos band = QuestData.get().loopband;
        if (band == null || !band.equals(pos)) {
            return false;
        }
        ren(p, true);
        return true;
    }

    /** One lap on the treadmill (pauze: wait a little between laps; the gametests skip it). */
    static void ren(ServerPlayer p, boolean pauze) {
        MinecraftServer server = p.level().getServer();
        String stand = SafariSpeler.quest(server, p.getUUID(), QUEST);
        if (!stand.equals("loopband")) {
            if (stand.equals("klaar")) {
                SafariQuests.zeg(p, "gui.guhs.safari.olifant.na");
            } else {
                p.sendOverlayMessage(Component.translatable("block.guhs.olifantenloopband.lore").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        long nu = server.getTickCount();
        Long vorige = PAUZE.get(p.getUUID());
        if (pauze && vorige != null && nu - vorige < 30) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.safari.olifant.even").withStyle(ChatFormatting.GRAY));
            return;
        }
        PAUZE.put(p.getUUID(), nu);
        int n = rondes(server, p.getUUID()) + 1;
        SafariSpeler.zetQuest(server, p.getUUID(), RONDEN, String.valueOf(n));
        p.sendOverlayMessage(Component.translatable("gui.guhs.safari.olifant.ren", n, RONDES).withStyle(ChatFormatting.GOLD));
        BlockPos band = QuestData.get().loopband;
        if (band != null && p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, band.getX() + 0.5, band.getY() + 0.5, band.getZ() + 0.5, 6, 0.4, 0.2, 0.4, 0.02);
            level.playSound(null, band, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.6f, 0.8f);
        }
        if (n >= RONDES) {
            klaar(p);
        }
    }

    static void klaar(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        SafariSpeler.zetQuest(server, p.getUUID(), QUEST, "klaar");
        SafariQuests.zeg(p, "gui.guhs.safari.olifant.klaar", MUNTEN);
        SafariQuests.geef(p, new ItemStack(SafariQuests.OLIFANTENLOOPBAND_ITEM.get()));
        SafariQuests.geef(p, new ItemStack(SafariQuests.FOTOLIJSTJE_ITEM.get()));
        SafariMunten.verdienEens(p, "quest_" + QUEST, MUNTEN, "te vahe olifant");
        Beloningen.adv(p, "safari_vahe_olifant");
        if (QuestData.get().stal != null && p.level() instanceof ServerLevel level) {
            BlockPos s = QuestData.get().stal;
            level.sendParticles(ParticleTypes.HEART, s.getX() + 0.5, s.getY() + 2.5, s.getZ() + 0.5, 10, 1.0, 0.6, 1.0, 0.0);
        }
    }

    static List<String> gegeten(MinecraftServer server, UUID id) {
        String s = SafariSpeler.quest(server, id, GEGETEN);
        List<String> uit = new ArrayList<>();
        for (String k : s.split(",")) {
            if (!k.isBlank()) {
                uit.add(k.trim());
            }
        }
        return uit;
    }

    static int rondes(MinecraftServer server, UUID id) {
        try {
            return Integer.parseInt(SafariSpeler.quest(server, id, RONDEN));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Every few seconds: Dikkie stands in his stal (exactly one) when the stal is loaded and a player is near. */
    static void bewaak(MinecraftServer server) {
        ServerLevel level = Safari.level(server);
        BlockPos stal = QuestData.get().stal;
        if (level == null || stal == null || level.players().isEmpty() || !level.hasChunkAt(stal)
                || level.getNearestPlayer(stal.getX(), stal.getY(), stal.getZ(), 96, false) == null) {
            return;
        }
        List<Entity> er = level.getEntities((Entity) null, new AABB(stal).inflate(16), e -> e.entityTags().contains(TAG));
        for (int k = 1; k < er.size(); k++) {
            er.get(k).discard();
        }
        if (er.isEmpty()) {
            zet(level, stal);
        }
    }

    @Nullable
    static SafariDier zet(ServerLevel level, BlockPos stal) {
        SafariDier dier = SafariFeature.DIER.get().create(level, EntitySpawnReason.TRIGGERED);
        if (dier == null) {
            return null;
        }
        int zone = ParkData.get().zone(stal.getX(), stal.getZ());
        dier.zet("olifant", "man", stal, 2, zone, -1, SafariOpslag.get(level.getServer()).generatie());
        dier.snapTo(stal.getX() + 0.5, stal.getY(), stal.getZ() + 0.5, 180f, 0f);
        dier.setCustomName(Component.translatable("gui.guhs.safari.olifant.naam"));
        dier.setCustomNameVisible(true);
        dier.addTag(TAG);
        level.addFreshEntity(dier);
        return dier;
    }

    private VaheOlifant() {
    }
}
