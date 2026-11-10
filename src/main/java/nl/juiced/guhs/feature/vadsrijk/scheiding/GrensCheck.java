package nl.juiced.guhs.feature.vadsrijk.scheiding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.ftb.VrFtbRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.vitrine.Vitrine;
import nl.juiced.guhs.feature.vadsrijk.vitrine.VitrineBlockEntity;

/**
 * Y-INT (dev only, op): the REAL border check in a client run. The gametest server has no Vadsrijk dimension, so this walks a
 * player through the real dimension change and checks every channel as a normal player would hit it. Driven by the AutoCheck
 * script {@code tools/autocheck/vadsrijk_y_int.txt}; every check is one line "GRENSCHECK PASS|FAIL|SKIP ..." in the log, in chat
 * and in {@code <game dir>/grenscheck.txt}.
 * <ol>
 *   <li>{@code /guhs grenscheck start} (at home): the player counts as a non-op ({@link Scheiding#NOOIT_VRIJ}), survival; a marked
 *   diamond, xp level 17, Speed II, 13 health, 11 food; FTB Essentials {@code /sethome grenscheck}; work blocks work at home.</li>
 *   <li>{@code /guhs grenscheck in}: an emerald is dropped at the player's feet, then the real Vadsrijk entry.</li>
 *   <li>{@code /guhs grenscheck binnen}: swapped inventory / xp / effects / health; the emerald stayed home; an item entity can't be
 *   teleported across (both ways); {@code /ec /enderchest /home /back /spawn /tpa} and the work blocks refused; the FTB claim of the
 *   vitrine reward refused; the vitrine and a pronkstuk can't be placed; Night Vision is given inside.</li>
 *   <li>{@code /guhs grenscheck uit}: the real way home. {@code /guhs grenscheck thuis}: everything back exactly, no Night Vision;
 *   the FTB claim works; vitrine + pronkstuk placed in front of the player.</li>
 *   <li>{@code /guhs grenscheck rapport}: the totals.</li>
 * </ol>
 */
public final class GrensCheck {
    static final String DIAMANT = "Grenscheck-diamant", SMARAGD = "Grensdruppel";
    /** FTB Essentials 26.1.2.4: /enderchest, and the work blocks as /open <block> (there is no /ec and no bare /anvil). */
    static final List<String> WERK = List.of("enderchest", "open anvil", "open crafting", "open smithing", "open stonecutter");
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("guhs");
    private static final List<String> REGELS = new ArrayList<>();
    private static int pass, fail, skip;
    private static UUID druppel;
    private static BlockPos vitrinePlek;
    private static float hp = 13f;
    private static int eten = 11, level = 17;

    private GrensCheck() {
    }

    static void commando(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("grenscheck").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("stap", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    switch (StringArgumentType.getString(c, "stap")) {
                        case "start" -> start(p);
                        case "in" -> in(p);
                        case "binnen" -> binnen(p);
                        case "uit" -> VadsrijkCommando.terug(p);
                        case "thuis" -> thuis(p);
                        case "vitrine" -> vitrine(p);
                        case "rapport" -> rapport(p);
                        default -> p.sendSystemMessage(Component.literal("start | in | binnen | uit | thuis | vitrine | rapport"));
                    }
                    return 1;
                }))));
    }

    // ---- the steps ----------------------------------------------------------------------------------------------------------

    static void start(ServerPlayer p) {
        REGELS.clear();
        pass = fail = skip = 0;
        regel(p, "== start (" + p.level().dimension().identifier() + ")");
        if (!check(p, !Scheiding.binnen(p), "start at home (not inside)")) {
            return;
        }
        Scheiding.NOOIT_VRIJ.add(p.getUUID());
        p.setGameMode(GameType.SURVIVAL);
        ItemStack d = new ItemStack(Items.DIAMOND);
        d.set(DataComponents.CUSTOM_NAME, Component.literal(DIAMANT));
        p.getInventory().add(d);
        p.setExperienceLevels(level);
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 600, 1));
        p.removeEffect(MobEffects.NIGHT_VISION);
        p.setHealth(hp);
        p.getFoodData().setFoodLevel(eten);
        check(p, !Scheiding.vrijgesteld(p), "the player counts as a normal player (no op exemption) for this check");
        if (ftbEssentials(p.level().getServer())) {
            later(p, () -> {
                doe(p, "sethome grenscheck");
                for (String w : WERK) {
                    doe(p, w);
                    check(p, p.containerMenu != p.inventoryMenu, "at home /" + w + " opens (FTB Essentials, control)");
                    p.closeContainer();
                }
            });
        } else {
            skip(p, "FTB Essentials is not loaded: the command checks are skipped");
        }
        regel(p, "home body: diamond, xp " + level + ", Speed II, health " + hp + ", food " + eten);
    }

    static void in(ServerPlayer p) {
        ItemStack s = new ItemStack(Items.EMERALD);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(SMARAGD));
        ItemEntity it = p.drop(s, false);
        druppel = it == null ? null : it.getUUID();
        regel(p, "== in: emerald dropped at the border (" + (druppel != null) + "), real entry");
        VadsrijkCommando.naarBinnen(p, false);
    }

    static void binnen(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        regel(p, "== binnen (" + p.level().dimension().identifier() + ")");
        check(p, Vadsrijk.in(p) && Spelers.data(p).binnen, "inside the Vadsrijk, inventory swapped");
        check(p, telDiamant(p) == 0, "the home diamond is not in the Vadsrijk inventory");
        check(p, p.experienceLevel != level, "vanilla xp swapped (inside: level " + p.experienceLevel + ", home " + level + ")");
        check(p, !p.hasEffect(MobEffects.SPEED), "the home Speed effect does not exist inside");
        check(p, Lichaam.thuisWacht(p), "the home body (health/food/effects) is stored");
        ServerLevel thuis = server.overworld();
        check(p, telNaam(p, SMARAGD) == 0 && geenNaam(p.level(), SMARAGD), "the emerald dropped at the border is nowhere inside");
        // an item entity can't be sent across, either way
        ItemEntity a = new ItemEntity(thuis, 0.5, thuis.getMaxY() - 5, 0.5, new ItemStack(Items.GOLD_INGOT));
        thuis.addFreshEntity(a);
        Entity na = a.teleport(new TeleportTransition((ServerLevel) p.level(), p.position(), Vec3.ZERO, 0f, 0f, TeleportTransition.DO_NOTHING));
        check(p, na == null && a.level() == thuis, "an item entity can't be teleported home -> Vadsrijk");
        a.discard();
        ItemEntity b = new ItemEntity(p.level(), p.getX(), p.getY() + 1, p.getZ(), new ItemStack(Items.GOLD_INGOT));
        p.level().addFreshEntity(b);
        Entity nb = b.teleport(new TeleportTransition(thuis, new Vec3(0.5, thuis.getMaxY() - 5, 0.5), Vec3.ZERO, 0f, 0f, TeleportTransition.DO_NOTHING));
        check(p, nb == null && b.level() == p.level(), "an item entity can't be teleported Vadsrijk -> home");
        b.discard();
        // commands (FTB Essentials)
        if (ftbEssentials(server)) {
            if (server.getCommands().getDispatcher().getRoot().getChild("ec") == null) {
                regel(p, "note: /ec does not exist in FTB Essentials 26.1.2.4 (only /enderchest); it is on the refusal list anyway");
            }
            later(p, () -> {
                for (String c : WERK) {
                    doe(p, c);
                    check(p, p.containerMenu == p.inventoryMenu, "inside /" + c + " is refused (no menu)");
                    p.closeContainer();
                }
                for (String c : List.of("home grenscheck", "back", "spawn")) {
                    doe(p, c);
                    check(p, Vadsrijk.in(p), "inside /" + c + " is refused (still in the Vadsrijk)");
                }
            });
        } else {
            skip(p, "FTB Essentials is not loaded: /ec /home inside not checked");
        }
        // FTB Quests: claiming the vitrine reward inside is refused
        if (ModList.get().isLoaded("ftbquests")) {
            GrensCheckFtb.binnen(p);
        } else {
            skip(p, "FTB Quests is not loaded");
        }
        // placing the vitrine and a pronkstuk inside: refused
        BlockPos plek = plek(p);
        check(p, !plaats(p, plek, Vitrine.ITEM.get()), "the vitrine can't be placed inside (game mode " + p.gameMode.getGameModeForPlayer().getName() + ", " + plek.toShortString() + ")");
        check(p, !plaats(p, plek, VrFtbRegistry.ITEMS_PER_ID.get("vadsrijk_trofee_zone1").get()), "a pronkstuk (Knuffelzwamlampje) can't be placed inside");
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 600, 0));
        regel(p, "Night Vision given inside (must be gone at home)");
    }

    static void thuis(ServerPlayer p) {
        regel(p, "== thuis (" + p.level().dimension().identifier() + ")");
        check(p, !Vadsrijk.in(p) && !Spelers.data(p).binnen, "home again, inventory swapped back");
        check(p, telDiamant(p) == 1, "the home diamond is back");
        check(p, p.experienceLevel == level, "vanilla xp level back (" + p.experienceLevel + ")");
        check(p, p.hasEffect(MobEffects.SPEED), "the home Speed effect is back");
        check(p, !p.hasEffect(MobEffects.NIGHT_VISION), "the Night Vision from inside did not come out");
        check(p, Math.abs(p.getHealth() - hp) <= 1f, "health back (" + p.getHealth() + ", was " + hp + ")");
        check(p, Math.abs(p.getFoodData().getFoodLevel() - eten) <= 1, "food back (" + p.getFoodData().getFoodLevel() + ", was " + eten + ")");
        check(p, !Lichaam.thuisWacht(p), "no home body waiting any more");
        Entity dr = druppel == null ? null : ((ServerLevel) p.level()).getEntity(druppel);
        check(p, dr instanceof ItemEntity ie && !ie.isRemoved() && dr.level() == p.level(), "the emerald dropped at the border is still lying at home");
        if (dr != null) {
            dr.discard();
        }
        if (ModList.get().isLoaded("ftbquests")) {
            GrensCheckFtb.thuis(p);
        }
        if (ftbEssentials(p.level().getServer())) {
            later(p, () -> {
                for (String w : List.of("enderchest", "open anvil")) {
                    doe(p, w);
                    check(p, p.containerMenu != p.inventoryMenu, "at home /" + w + " works again");
                    p.closeContainer();
                }
            });
        }
        // vitrine + pronkstuk in front of the player
        BlockPos plek = plek(p);
        boolean v = plaats(p, plek, Vitrine.ITEM.get());
        vitrinePlek = v ? plek : null;
        check(p, v && p.level().getBlockEntity(plek) instanceof VitrineBlockEntity be && p.getUUID().equals(be.eigenaar()),
                "the vitrine is placed at home (owner = the player) at " + plek.toShortString());
        BlockPos naast = grond(p.level(), plek.relative(p.getDirection().getClockWise(), 2));
        check(p, plaats(p, naast, VrFtbRegistry.ITEMS_PER_ID.get("vadsrijk_trofee_zone1").get()), "a pronkstuk is placed at home at " + naast.toShortString());
        BlockPos naast2 = grond(p.level(), plek.relative(p.getDirection().getCounterClockWise(), 2));
        check(p, plaats(p, naast2, VrFtbRegistry.ITEMS_PER_ID.get("vadsrijk_trofee_rekruut").get()), "the Rekruutvaantje is placed at home");
    }

    /** A mount for the statue (Knuffelhert, if the player has none), the snapshot refreshed, and the camera on the vitrine. */
    static void vitrine(ServerPlayer p) {
        if (nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren.actief(p) == null) {
            nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren.geef(p, nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.KNUFFELHERT);
            nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren.kies(p, nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.KNUFFELHERT);
        }
        if (vitrinePlek != null) {
            Vec3 c = Vec3.atBottomCenterOf(vitrinePlek);
            Vec3 oog = c.add(Vec3.atLowerCornerOf(p.getDirection().getUnitVec3i()).scale(-2.6)).add(0, 0.4, 0);
            p.teleportTo((ServerLevel) p.level(), oog.x, oog.y, oog.z, java.util.Set.of(), p.getDirection().toYRot(), 12f, true);
            if (p.level().getBlockEntity(vitrinePlek) instanceof VitrineBlockEntity be) {
                regel(p, "vitrine snapshot: " + be.foto());
            }
        }
    }

    static void rapport(ServerPlayer p) {
        Scheiding.NOOIT_VRIJ.remove(p.getUUID());
        regel(p, "GRENSCHECK TOTAAL: " + pass + " PASS, " + fail + " FAIL, " + skip + " SKIP");
    }

    // ---- helpers -------------------------------------------------------------------------------------------------------------

    static boolean check(ServerPlayer p, boolean ok, String wat) {
        if (ok) {
            pass++;
        } else {
            fail++;
        }
        regel(p, (ok ? "PASS " : "FAIL ") + wat);
        return ok;
    }

    static void skip(ServerPlayer p, String wat) {
        skip++;
        regel(p, "SKIP " + wat);
    }

    static void regel(ServerPlayer p, String s) {
        String r = "GRENSCHECK " + s;
        LOG.info(r);
        REGELS.add(r);
        p.sendSystemMessage(Component.literal(s).withStyle(s.startsWith("FAIL") ? ChatFormatting.RED : s.startsWith("PASS") ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        try {
            Path f = p.level().getServer().getServerDirectory().resolve("grenscheck.txt");
            Files.writeString(f, r + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.warn("GRENSCHECK: cannot write grenscheck.txt: {}", e.toString());
        }
    }

    static boolean ftbEssentials(MinecraftServer server) {
        return server.getCommands().getDispatcher().getRoot().getChild("enderchest") != null;
    }

    /**
     * Runs after the current command: a command started from inside another command (our /guhs grenscheck) is only QUEUED by
     * vanilla until the outer one ends, so its effect can't be checked in the same call.
     */
    static void later(ServerPlayer p, Runnable r) {
        MinecraftServer server = p.level().getServer();
        server.schedule(new net.minecraft.server.TickTask(server.getTickCount() + 1, r));
    }

    static void doe(ServerPlayer p, String cmd) {
        p.level().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack(), cmd);
    }

    static int telDiamant(ServerPlayer p) {
        return telNaam(p, DIAMANT);
    }

    static int telNaam(ServerPlayer p, String naam) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && s.has(DataComponents.CUSTOM_NAME) && naam.equals(s.getHoverName().getString())) {
                n += s.getCount();
            }
        }
        return n;
    }

    static boolean geenNaam(Level l, String naam) {
        if (!(l instanceof ServerLevel sl)) {
            return true;
        }
        for (Entity e : sl.getAllEntities()) {
            if (e instanceof ItemEntity it && it.getItem().has(DataComponents.CUSTOM_NAME) && naam.equals(it.getItem().getHoverName().getString())) {
                return false;
            }
        }
        return true;
    }

    /** A free spot 3 blocks in front of the player, on the ground. */
    static BlockPos plek(ServerPlayer p) {
        return grond(p.level(), p.blockPosition().relative(p.getDirection(), 3));
    }

    /** The spot in this column (+-3) with a solid floor and room above (snow layers and grass count as room: they get replaced). */
    static BlockPos grond(Level l, BlockPos b) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos c = b.above(dy);
            if (l.getBlockState(c).canBeReplaced() && l.getBlockState(c.above()).canBeReplaced()
                    && l.getBlockState(c.below()).isFaceSturdy(l, c.below(), Direction.UP) && !l.getBlockState(c.below()).canBeReplaced()) {
                return c;
            }
        }
        return b;
    }

    /** Places the item the way a right-click on the ground does (the item in the main hand for that moment). */
    static boolean plaats(ServerPlayer p, BlockPos plek, Item item) {
        ItemStack oud = p.getItemInHand(InteractionHand.MAIN_HAND).copy();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(plek.below()).add(0, 0.5, 0), Direction.UP, plek.below(), false);
            p.getItemInHand(InteractionHand.MAIN_HAND).useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, hit));
        } finally {
            p.setItemInHand(InteractionHand.MAIN_HAND, oud);
        }
        Block b = item instanceof net.minecraft.world.item.BlockItem bi ? bi.getBlock() : null;
        return b != null && p.level().getBlockState(plek).is(b);
    }
}
