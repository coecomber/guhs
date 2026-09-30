package nl.juiced.guhs.quest;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaBaasEntity;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.network.MaagPayloads;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhWorldData;
import nl.juiced.guhs.world.MaagManager;

/**
 * The quests.
 * <p>
 * <b>De ontvoerde guh</b> (unlocks your own guh stomach):
 * <ol>
 *     <li>Moeder Vadsig (in her very rare shrine): "Mika has kidnapped my little Guhbert!" - gives the Mika track compass.</li>
 *     <li>The Mika camp: the Mika-baas plays rock-paper-scissors-VADS; win 3 times in a row ("NJEG... IK BEN GEVADST!")
 *     and Guhbert is free (he follows you from then on).</li>
 *     <li>It's Guhbert's birthday, but his cake is gone: the cake crumbs point to a guh picnic, where you find it.</li>
 *     <li>Bring the cake and 3 guh balloons to Moeder Vadsig: party! You get the Guh-buikfluitje; your stomach exists.</li>
 * </ol>
 * The Tandarts-guh in the mouth makes stomachs bigger; the Maagenzym-guh in every stomach holds its settings.
 * The Slee-guh's quest is in {@link SledQuest}.
 */
public final class GuhQuests {
    public static final ResourceKey<Structure> GUH_PICNIC = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guh_picnic"));
    public static final int BALLOONS_WANTED = 3;
    public static final int RPS_WINS_NEEDED = 3;

    public enum Rps {
        STEEN, PAPIER, SCHAAR, VADS;

        /** What Mika picks against you: always the one that beats you (there's nothing that beats VADS). */
        public Rps counter() {
            return switch (this) {
                case STEEN -> PAPIER;
                case PAPIER -> SCHAAR;
                case SCHAAR -> STEEN;
                case VADS -> STEEN;
            };
        }
    }

    /** "<Name> text" in the chat, only for this player. */
    public static void say(ServerPlayer player, Entity speaker, String key, Object... args) {
        MutableComponent line = Component.literal("<").append(speaker.getDisplayName()).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(key, args).withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(line);
    }

    /**
     * A player's own guh data that survives dying (NeoForge copies the "PlayerPersisted" part on respawn). Older saves
     * kept some of it at the top level: that moves in the first time.
     */
    public static net.minecraft.nbt.CompoundTag saved(net.minecraft.world.entity.player.Player player) {
        net.minecraft.nbt.CompoundTag root = player.getPersistentData();
        if (!root.contains("PlayerPersisted", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            root.put("PlayerPersisted", new net.minecraft.nbt.CompoundTag());
        }
        net.minecraft.nbt.CompoundTag saved = root.getCompound("PlayerPersisted");
        for (String key : java.util.List.copyOf(root.getAllKeys())) {
            if (key.startsWith("guhs_verstop_best_") || key.equals("guhs_kermis_first_lap") || key.equals("guhs_guhvriend")) {
                saved.put(key, root.get(key));
                root.remove(key);
            }
        }
        return saved;
    }

    public static void talkTo(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.9f);
        switch (npc.getKind()) {
            case MOEDER_VADSIG -> vadsig(npc, player);
            case TANDARTS -> dentist(npc, player);
            case MAAGENZYM -> enzyme(npc, player);
            case SLEE_GUH -> SledQuest.talk(npc, player);
            case KERMIS_GUH -> say(player, npc, "quest.guhs.kermis.hello");
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Moeder Vadsig
    // ------------------------------------------------------------------------------------------------------------

    private static void vadsig(GuhNpcEntity npc, ServerPlayer player) {
        GuhWorldData data = GuhWorldData.get(player.server);
        GuhWorldData.PlayerData p = data.player(player.getUUID());
        // however you got them: the cake and the balloons in your pockets means party time
        if (p.maagQuest < 4 && count(player, ModItems.VERLOREN_GUH_TAART.get()) > 0) {
            p.maagQuest = Math.max(p.maagQuest, 3);
        }
        switch (p.maagQuest) {
            case 0 -> {
                say(player, npc, "quest.guhs.vadsig.start");
                give(player, ModItems.MIKA_SPOORKOMPAS.get());
                p.maagQuest = 1;
                data.setDirty();
                hint(player, "quest.guhs.next.camp");
                GuhAdvancements.grant(player, "vadsig_met");
            }
            case 1 -> {
                say(player, npc, "quest.guhs.vadsig.camp");
                giveIfMissing(player, ModItems.MIKA_SPOORKOMPAS.get());
                hint(player, "quest.guhs.next.camp");
            }
            case 2 -> {
                say(player, npc, "quest.guhs.vadsig.cake");
                giveIfMissing(player, ModItems.TAARTKRUIMELS.get());
                hint(player, "quest.guhs.next.picnic");
                say(player, npc, "quest.guhs.vadsig.need", Component.translatable("quest.guhs.missing"),
                        count(player, ModItems.GUH_BALLON.get()), BALLOONS_WANTED);
            }
            case 3 -> {
                boolean cake = count(player, ModItems.VERLOREN_GUH_TAART.get()) > 0;
                int balloons = count(player, ModItems.GUH_BALLON.get());
                if (cake && balloons >= BALLOONS_WANTED) {
                    take(player, ModItems.VERLOREN_GUH_TAART.get(), 1);
                    take(player, ModItems.GUH_BALLON.get(), BALLOONS_WANTED);
                    party(npc, player);
                    p.maagQuest = 4;
                    data.setDirty();
                    say(player, npc, "quest.guhs.vadsig.party");
                    give(player, ModItems.GUH_BUIKFLUITJE.get());
                    player.sendSystemMessage(Component.translatable("quest.guhs.vadsig.unlocked").withStyle(ChatFormatting.GOLD));
                    GuhAdvancements.grant(player, "maag_unlocked");
                    ServerLevel maagLevel = MaagManager.level(player.server);
                    if (maagLevel != null) {
                        MaagManager.ensureMaag(maagLevel, player);
                        swallow(npc, player); // NJEG... HAP!
                    }
                } else {
                    say(player, npc, "quest.guhs.vadsig.need", cake ? Component.translatable("quest.guhs.have") : Component.translatable("quest.guhs.missing"),
                            balloons, BALLOONS_WANTED);
                    hint(player, cake ? "quest.guhs.next.balloons" : "quest.guhs.next.picnic");
                }
            }
            default -> say(player, npc, "quest.guhs.vadsig.done");
        }
    }

    /** Balloons, confetti and fireworks around Moeder Vadsig. */
    private static void party(GuhNpcEntity npc, ServerPlayer player) {
        ServerLevel level = (ServerLevel) npc.level();
        level.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 3, npc.getZ(), 20, 1.5, 1, 1.5, 0.1);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, npc.getX(), npc.getY() + 2, npc.getZ(), 120, 2, 2, 2, 0.4);
        level.playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1f);
        for (int i = 0; i < 5; i++) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            rocket.set(net.minecraft.core.component.DataComponents.FIREWORKS, new Fireworks(1, List.of(new FireworkExplosion(
                    FireworkExplosion.Shape.values()[i % FireworkExplosion.Shape.values().length],
                    it.unimi.dsi.fastutil.ints.IntList.of(0xFF7FB6, 0xFFD27A, 0xFFFFFF), it.unimi.dsi.fastutil.ints.IntList.of(0xE8506E), true, true))));
            level.addFreshEntity(new FireworkRocketEntity(level, npc.getX() + (i - 2) * 1.5, npc.getY() + 1, npc.getZ() + (i % 2 == 0 ? 2 : -2), rocket));
        }
        // Guhbert gets a party hat for his birthday
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(24),
                g -> g.isOwnedBy(player) && g.hasCustomName() && "Guhbert".equals(g.getCustomName().getString()))) {
            guh.wear(GuhClothes.PARTY_HAT);
            guh.triggerAnim("action", "happy");
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Rock-paper-scissors-VADS with the Mika-baas
    // ------------------------------------------------------------------------------------------------------------

    public static void openRps(MikaBaasEntity mika, ServerPlayer player) {
        GuhWorldData.PlayerData p = GuhWorldData.get(player.server).player(player.getUUID());
        say(player, mika, p.maagQuest == 1 ? "quest.guhs.mika.challenge" : "quest.guhs.mika.play");
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new MaagPayloads.RpsState(mika.getId(), p.rpsStreak, -1, false, true, p.vadsRevealed));
    }

    public static void playRps(ServerPlayer player, MikaBaasEntity mika, Rps choice) {
        GuhWorldData data = GuhWorldData.get(player.server);
        GuhWorldData.PlayerData p = data.player(player.getUUID());
        if (choice == Rps.VADS && !p.vadsRevealed) {
            return; // (not a move you know about yet)
        }
        boolean won = choice == Rps.VADS;
        Rps mikaChoice = choice.counter();
        if (won) {
            p.rpsStreak++;
            player.sendSystemMessage(Component.literal("<").append(mika.getDisplayName()).append("> ").withStyle(ChatFormatting.DARK_RED)
                    .append(Component.translatable("quest.guhs.mika.vadsed").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
            mika.playSound(nl.juiced.guhs.registry.ModSounds.MIKA_HURT.get(), 1.2f, 0.8f);
        } else {
            p.rpsStreak = 0;
            say(player, mika, "quest.guhs.mika.won", Component.translatable("gui.guhs.rps." + mikaChoice.name().toLowerCase(java.util.Locale.ROOT)));
            if (!p.vadsRevealed) { // after the first loss Mika brags a bit too much... and VADS appears
                p.vadsRevealed = true;
                say(player, mika, "quest.guhs.mika.vads_reveal");
            }
            mika.playSound(nl.juiced.guhs.registry.ModSounds.MIKA_AMBIENT.get(), 1f, 1.2f);
        }
        boolean freed = false;
        if (p.rpsStreak >= RPS_WINS_NEEDED) {
            p.rpsStreak = 0;
            nl.juiced.guhs.feature.guheinde.GuheindeEvents.onRpsBeaten(player);
            if (p.maagQuest == 1) {
                p.maagQuest = 2;
                freed = true;
                freeGuhbert(player, mika);
            } else {
                say(player, mika, "quest.guhs.mika.again");
            }
        }
        data.setDirty();
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new MaagPayloads.RpsState(mika.getId(), p.rpsStreak, mikaChoice.ordinal(), won, !freed, p.vadsRevealed));
    }

    /** Players being swallowed by Moeder Vadsig: ticks left until they arrive in their stomach. */
    private static final java.util.Map<java.util.UUID, Integer> SWALLOWING = new java.util.HashMap<>();
    public static final int BELLY_LINES = 8;

    /** The first time: Moeder Vadsig swallows you whole. It goes dark, a big gulp, and you are in her belly. */
    private static void swallow(GuhNpcEntity npc, ServerPlayer player) {
        say(player, npc, "quest.guhs.vadsig.swallow");
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 60, 0, false, false));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 50, 0, false, false));
        npc.level().playSound(null, player.blockPosition(), nl.juiced.guhs.registry.ModSounds.GUH_EAT.get(), SoundSource.NEUTRAL, 2f, 0.5f);
        npc.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 1.5f, 0.6f);
        SWALLOWING.put(player.getUUID(), 40);
    }

    /** "Volgende stap: ..." in gold, so you always know where to go next. */
    public static void hint(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable("quest.guhs.next", Component.translatable(key)).withStyle(ChatFormatting.GOLD));
    }

    /** Guhbert (a little baby guh) is free and follows you from now on. */
    private static void freeGuhbert(ServerPlayer player, MikaBaasEntity mika) {
        ServerLevel level = (ServerLevel) mika.level();
        GuhEntity guhbert = ModEntities.GUH.get().create(level);
        if (guhbert != null) {
            guhbert.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
            guhbert.setGuhScale(0.6f);
            guhbert.setVariant(GuhVariant.NORMAL);
            guhbert.setPersonality(GuhPersonality.CUDDLY);
            guhbert.setBaby(true);
            guhbert.tame(player);
            guhbert.setCustomName(Component.literal("Guhbert"));
            guhbert.setPersistenceRequired();
            level.addFreshEntity(guhbert);
            level.sendParticles(ParticleTypes.HEART, guhbert.getX(), guhbert.getY() + 0.8, guhbert.getZ(), 8, 0.4, 0.3, 0.4, 0.1);
            say(player, guhbert, "quest.guhs.guhbert.free");
            hint(player, "quest.guhs.next.back_to_vadsig");
            GuhAdvancements.grant(player, "guhbert_free");
        }
        // the caged Guhbert of the camp is the one that got out
        level.getEntitiesOfClass(GuhEntity.class, mika.getBoundingBox().inflate(32), g -> g.getTags().contains("guhs_caged_guhbert"))
                .forEach(g -> g.discard());
        giveIfMissing(player, ModItems.TAARTKRUIMELS.get());
    }

    /** While looking for the lost cake: walking into a guh picnic finds it. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Integer swallowing = SWALLOWING.get(player.getUUID());
        if (swallowing != null) {
            if (swallowing <= 0) {
                SWALLOWING.remove(player.getUUID());
                MaagManager.goToOwnMaag(player);
                player.sendSystemMessage(Component.translatable("quest.guhs.swallowed").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                SWALLOWING.put(player.getUUID(), swallowing - 1);
            }
        }
        if (player.tickCount % 40 != 0) {
            return;
        }
        // now and then Moeder Vadsig talks to you from above: you are in her belly, after all
        if (MaagManager.isGuhmaag(player.level()) && player.getRandom().nextInt(90) == 0) {
            player.sendSystemMessage(Component.literal("<").append(Component.translatable("entity.guhs.guh_npc.moeder_vadsig")).append("> ")
                    .withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.translatable("quest.guhs.vadsig.belly." + player.getRandom().nextInt(BELLY_LINES))
                            .withStyle(ChatFormatting.ITALIC)));
        }
        GuhWorldData data = GuhWorldData.get(player.server);
        GuhWorldData.PlayerData p = data.player(player.getUUID());
        if (p.maagQuest == 2) {
            ServerLevel level = player.serverLevel();
            var picnic = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(GUH_PICNIC);
            if (picnic != null && level.structureManager().getStructureWithPieceAt(player.blockPosition(), picnic).isValid()) {
                p.maagQuest = 3;
                data.setDirty();
                give(player, ModItems.VERLOREN_GUH_TAART.get());
                player.sendSystemMessage(Component.translatable("quest.guhs.cake.found").withStyle(ChatFormatting.GOLD));
                hint(player, "quest.guhs.next.balloons");
                GuhAdvancements.grant(player, "cake_found");
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
            }
        }
        GuhDex.onPlayerTick(player, data);
        GuhDex.findCreatures(player);
        GuhDex.seeCreatures(player);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Tandarts-guh: bigger stomachs
    // ------------------------------------------------------------------------------------------------------------

    /** Something the dentist wants: an item and how many. */
    public record Need(java.util.function.Supplier<? extends net.minecraft.world.level.ItemLike> item, int count) {
        Item get() {
            return item.get().asItem();
        }
    }

    /**
     * One chapter of making your stomach bigger (48 -> 64 -> 80 -> 96 -> 112 -> 128): a little story inside Moeder
     * Vadsig's belly (quest.guhs.dentist.story.<id> / done.<id>), what the dentist needs, and special conditions.
     */
    public record Upgrade(String id, List<Need> needs, boolean bigMika, boolean goldenGuh, boolean fullGuhdex) {
    }

    public static final Upgrade[] UPGRADES = {
            new Upgrade("krampjes", List.of(new Need(ModItems.KAAS_KNABBELS, 64), new Need(ModItems.VAHOEGE_VADS_INGOT, 16),
                    new Need(ModItems.GUH_KRISTAL, 8)), false, false, false),
            new Upgrade("mika", List.of(new Need(ModItems.GUH_KRISTAL, 16)), true, false, false),
            new Upgrade("goud", List.of(new Need(ModItems.GUH_KRISTAL, 32)), false, true, false),
            new Upgrade("zacht", List.of(new Need(ModItems.GUH_SLIMEBALL, 32), new Need(ModItems.KAASHONING, 8)), false, false, false),
            new Upgrade("bloesem", List.of(new Need(nl.juiced.guhs.registry.ModBlocks.GUHBLOESEM_SAPLING, 16),
                    new Need(ModItems.GUH_KRISTAL, 48)), false, false, true),
    };

    private static void dentist(GuhNpcEntity npc, ServerPlayer player) {
        GuhWorldData data = GuhWorldData.get(player.server);
        GuhWorldData.Maag maag = data.maagOf(player.getUUID());
        if (maag == null) {
            say(player, npc, "quest.guhs.dentist.no_maag");
            return;
        }
        int lvl = MaagManager.sizeLevel(maag.size);
        if (lvl >= UPGRADES.length) {
            say(player, npc, "quest.guhs.dentist.max", maag.size);
            return;
        }
        Upgrade up = UPGRADES[lvl];
        GuhWorldData.PlayerData p = data.player(player.getUUID());
        boolean golden = !up.goldenGuh() || !player.level().getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(10),
                g -> g.isOwnedBy(player) && g.getVariant() == GuhVariant.GOLDEN).isEmpty();
        boolean mika = !up.bigMika() || p.beatBigMika;
        boolean dex = !up.fullGuhdex() || GuhDex.vol(p.seen);   // (2.10.1: bonus pages like the Rookguh don't count)
        boolean ok = golden && mika && dex && up.needs().stream().allMatch(n -> count(player, n.get()) >= n.count());
        if (!ok) {
            say(player, npc, "quest.guhs.dentist.story." + up.id());
            say(player, npc, "quest.guhs.dentist.job", maag.size, MaagManager.SIZES[lvl + 1]);
            for (Need n : up.needs()) {
                int have = count(player, n.get());
                player.sendSystemMessage(Component.literal("  - ").append(Component.translatable("quest.guhs.need.item", Math.min(have, n.count()), n.count(),
                        n.get().getDescription())).withStyle(have >= n.count() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
            if (up.bigMika()) {
                player.sendSystemMessage(Component.translatable("quest.guhs.need.big_mika").withStyle(mika ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
            if (up.goldenGuh()) {
                player.sendSystemMessage(Component.translatable("quest.guhs.need.golden").withStyle(golden ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
            if (up.fullGuhdex()) {
                player.sendSystemMessage(Component.translatable("quest.guhs.need.guhdex", Math.min(GuhDex.geteld(p.seen), GuhDex.TELLEND.size()), GuhDex.TELLEND.size())
                        .withStyle(dex ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
            return;
        }
        up.needs().forEach(n -> take(player, n.get(), n.count()));
        ServerLevel maagLevel = MaagManager.level(player.server);
        if (maagLevel != null && MaagManager.grow(maagLevel, maag)) {
            say(player, npc, "quest.guhs.dentist.done." + up.id(), maag.size);
            nl.juiced.guhs.quest.GuhAdvancements.grant(player, "maag_" + maag.size);
            ((ServerLevel) npc.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + 1.5, npc.getZ(), 20, 0.6, 0.6, 0.6, 0.1);
            npc.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.2f);
        }
    }

    private static Component need(String key, int have, int wanted) {
        return Component.literal("  - ").append(Component.translatable(key, have, wanted))
                .withStyle(have >= wanted ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Maagenzym-guh: stomach settings (only its owner)
    // ------------------------------------------------------------------------------------------------------------

    private static void enzyme(GuhNpcEntity npc, ServerPlayer player) {
        GuhWorldData data = GuhWorldData.get(player.server);
        if (npc.getMaagOwner() == null || !npc.getMaagOwner().equals(player.getUUID())) {
            GuhWorldData.Maag maag = npc.getMaagOwner() == null ? null : data.maagOf(npc.getMaagOwner());
            say(player, npc, "quest.guhs.enzyme.not_yours", maag == null ? "?" : maag.ownerName);
            return;
        }
        GuhWorldData.Maag maag = data.maagOf(player.getUUID());
        if (maag != null) {
            nl.juiced.guhs.network.ModNetworking.sendTo(player, MaagPayloads.MaagSettings.of(maag));
        }
    }

    public static void onBigMikaKilled(LivingDeathEvent event) {
        if (event.getEntity() instanceof MikaEntity mika && mika.isBoss()
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            GuhWorldData data = GuhWorldData.get(player.server);
            data.player(player.getUUID()).beatBigMika = true;
            data.setDirty();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // inventory helpers
    // ------------------------------------------------------------------------------------------------------------

    public static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    public static void take(ServerPlayer player, Item item, int amount) {
        for (ItemStack stack : player.getInventory().items) {
            if (amount <= 0) {
                return;
            }
            if (stack.is(item)) {
                int t = Math.min(amount, stack.getCount());
                stack.shrink(t);
                amount -= t;
            }
        }
    }

    public static void give(ServerPlayer player, Item item) {
        ItemStack stack = new ItemStack(item);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static void giveIfMissing(ServerPlayer player, Item item) {
        if (count(player, item) == 0) {
            give(player, item);
        }
    }

    public static BlockPos blockPos(Entity e) {
        return e.blockPosition();
    }

    private GuhQuests() {
    }
}
