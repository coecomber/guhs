# Guhs 1.0.0 (MC 1.21.1) -> 1.1.0 (MC 26.1.2) migration notes

Reference for every agent of the port (plan: `port26/PORT_PLAN.md`). Every signature below was checked against the real
sources: `port26/src/mc` (Minecraft 26.1.2 + NeoForge patches, from `build/moddev/artifacts/minecraft-patched-26.1.2.112-sources.jar`),
`port26/src/nf` (NeoForge 26.1.2.112 sources, also copied to `build/moddev/artifacts/neoforge-26.1.2.112-sources.jar`),
`port26/src/gl` (GeckoLib 5.5.2 sources) and, for "old", `port26/src/old` (1.21.1 sources). **Grep those folders instead of
guessing**, e.g. `grep -rn "void tick(" port26/src/mc/net/minecraft/world/entity/Entity.java`.

Owners append patterns they establish (phase 2/2b) at the end of the matching section, prefixed with their role.

## 0. Tooling (read first)

| What | How |
|---|---|
| Build files | Minecraft 26.1.2, NeoForge 26.1.2.112, Java 25 toolchain (foojay downloads it to `~/.gradle/jdks/eclipse_adoptium-25-*`; Gradle itself runs on Prism's JDK 21), ModDevGradle 2.0.148, no Parchment (26.1 ships Mojang names incl. parameter names), GeckoLib `com.geckolib:geckolib-neoforge-26.1.2:5.5.2`, mixins `JAVA_25`, `data` run uses `clientData()`. |
| Compile | `bash C:/Users/Juiced/Documents/MineScapeStory/port26/gradle_slot.sh compileJava` from your worktree root. **Set JAVA_HOME** for it: `JAVA_HOME="$APPDATA/PrismLauncher/java/java-runtime-delta" bash .../gradle_slot.sh compileJava` (the system JAVA_HOME is JDK 14). |
| Full error list | build.gradle has `-Xmaxerrs 100000` and (while porting) `options.fork = true` + the JDK 25 `javac` as fork executable: Gradle's in-process compile listener crashes with an NPE (`ConstantsTreeVisitor ... typeElement is null`) on the broken tree and cuts the list at ~600 errors. Remove those lines (`options.incremental = false`, `options.fork = true`, `forkOptions.executable`) once the tree compiles. **Never add `-XDshould-stop.*`**: it makes javac stop after the "enter" phase and hides ~90 % of the errors. |
| Faster check without Gradle | once per worktree: `gradle_slot.sh writeCompileClasspath` (writes `build/compile-classpath.txt`), then `bash port26/scripts/javac_all.sh <out.log>` (runs JDK 25 javac on the whole tree, ~10 s). |
| Errors per package | `python port26/scripts/split_errors.py <log> <out_dir> [summary.md]` -> one file per package + slice table (slice map = PORT_PLAN section 6). Filter your own packages from it. |
| Error counts are lower bounds | javac does not report errors inside code whose types are already broken (e.g. a particle class that extends the removed `TextureSheetParticle`, or a renderer whose superclass generics are wrong). Expect new errors to appear when you fix the "header" errors of a class. |

## 1. What the phase-1 scripts already did (do not redo)

Scripts in `port26/scripts/` (`pass1.py`, `fix_imports.py`, `pass2.py`, `pass3.py`, `pass4.py`, `pass5.py`), statistics in `port26/scripts/pass1_stats.txt`.
Commits on `mc26`: `mc26: build setup`, `mc26: mechanical pass`, `mc26: compile with forked command-line javac while porting`, `mc26: mechanical pass 2`, `mc26: mechanical pass 3 (ValueInput/ValueOutput override signatures)`.

| Old | New (done everywhere) |
|---|---|
| `ResourceLocation` | `net.minecraft.resources.Identifier` (same static factories: `fromNamespaceAndPath`, `withDefaultNamespace`, `parse`, `tryParse`, `tryBuild`; `Guhs.id()` unchanged) |
| `level.isClientSide` (field) | `level.isClientSide()` (field is private) |
| `level.random` (protected in Level now) | `level.getRandom()` (only where javac complained, i.e. outside Level subclasses) |
| `MobSpawnType.X` / `SPAWN_EGG` | `EntitySpawnReason.X` / `SPAWN_ITEM_USE` (`net.minecraft.world.entity.EntitySpawnReason`) |
| `DimensionTransition` | `TeleportTransition` (type name only - constructor args see 4.4) |
| `GuiGraphics` | `GuiGraphicsExtractor` + method renames on the graphics parameter: `drawString`->`text`, `drawCenteredString`->`centeredText`, `renderItem`->`item`, `renderTooltip`->`setTooltipForNextFrame`, `renderComponentTooltip`->`setComponentTooltipForNextFrame`, `renderOutline`->`outline`, `pose().pushPose/popPose`->`pushMatrix/popMatrix`, `pose().translate(x,y,z)`/`scale(x,y,z)` -> 2-arg; overrides `render(GuiGraphics..)`->`extractRenderState`, `renderBackground`->`extractBackground`, `renderLabels`->`extractLabels`, `renderWidget`->`extractWidgetRenderState` |
| `@GameTest` / `@GameTestHolder` / `@PrefixGameTestTemplate` | `@GuhTest` (new `nl.juiced.guhs.gametest.GuhTest`, same attributes + defaults), holder annotations removed; `mixin/GameTestRegistryMixin` deleted (and removed from `guhs.mixins.json`) |
| `helper.assertTrue/assertFalse(cond, "msg")` | **unchanged** - NeoForge 26.1.2 patches `GameTestHelper#assertTrue/assertFalse(boolean, String)` back in (they wrap `Component.literal`). No wrapping needed. |
| `software.bernie.geckolib.*` | `com.geckolib.*` with the 5.x package of each class (e.g. `PlayState` -> `com.geckolib.animation.object.PlayState`, `GeoBone`/`BakedGeoModel` -> `com.geckolib.cache.model`, `AnimatableManager` -> `com.geckolib.animatable.manager`, `GeoRenderer` -> `com.geckolib.renderer.base`, built-in layers -> `com.geckolib.renderer.layer.builtin`); `AnimationState` -> `AnimationTest`; `new AnimationController<>(this, name, ticks, handler)` -> `new AnimationController<>(name, ticks, handler)`. `com.geckolib.util.Color` does **not** exist any more (see 4.6). |
| `tag.getInt/Long/Float/Double/Boolean/String/Byte/Short(k)` (CompoundTag and ListTag) | `tag.getIntOr(k, 0)`, `getLongOr(k, 0L)`, `getFloatOr(k, 0.0F)`, `getDoubleOr(k, 0.0)`, `getBooleanOr(k, false)`, `getStringOr(k, "")`, ... (same defaults as 1.21.1 returned for missing keys) |
| `tag.getCompound(k)` / `tag.getList(k, Tag.TAG_X)` | `tag.getCompoundOrEmpty(k)` / `tag.getListOrEmpty(k)` (the element type argument is gone) |
| `tag.getIntArray/getLongArray/getByteArray(k)` | `tag.getIntArray(k).orElse(new int[0])` etc. |
| `tag.getUUID(k)` / `hasUUID(k)` / `putUUID(k, v)` | `tag.read(k, UUIDUtil.CODEC).orElseThrow()` / `.read(k, UUIDUtil.CODEC).isPresent()` / `tag.store(k, UUIDUtil.CODEC, v)` (same int-array format as 1.21.1, old saves load) |
| `NbtUtils.loadUUID/createUUID` | `UUIDUtil.uuidFromIntArray(((IntArrayTag) t).getAsIntArray())` / `new IntArrayTag(UUIDUtil.uuidToIntArray(u))` |
| `tag.put(k, NbtUtils.writeBlockPos(p))` / `NbtUtils.readBlockPos(tag, k)` | `tag.store(k, BlockPos.CODEC, p)` / `tag.read(k, BlockPos.CODEC)` (Optional, as before) |
| `tag.getAllKeys()` / `tag.contains(k, Tag.TAG_X)` | `tag.keySet()` / `tag.contains(k)` (type check dropped) |
| `InteractionResultHolder<ItemStack>` | `InteractionResult`; `.success/.sidedSuccess(stack, ..)` -> `InteractionResult.SUCCESS.heldItemTransformedTo(stack)`, `.consume(stack)` -> `CONSUME.heldItemTransformedTo(stack)`, `.pass/.fail` -> `PASS/FAIL` |
| `InteractionResult.sidedSuccess(client)` | `InteractionResult.SUCCESS` (swings on the client; use `SUCCESS_SERVER` if only the server should swing) |
| `ItemInteractionResult.X` | `InteractionResult`: `SUCCESS`, `CONSUME`, `FAIL`; `PASS_TO_DEFAULT_BLOCK_INTERACTION` -> `TRY_WITH_EMPTY_HAND`; `SKIP_DEFAULT_BLOCK_INTERACTION` -> `PASS` |
| moved classes (import fixer) | `TriState` -> `net.minecraft.util.TriState`; `RenderType` -> `net.minecraft.client.renderer.rendertype.RenderType`; `UseAnim` -> `ItemUseAnimation`; `GameRules` -> `net.minecraft.world.level.gamerules.GameRules`; `ArmorMaterial(s)` -> `world.item.equipment`; `FogRenderer` -> `client.renderer.fog`; `Villager*` -> `world.entity.npc.villager`; `AbstractMinecart/Minecart` -> `vehicle.minecart`; `Util`/`BlockUtil` -> `net.minecraft.util`; `PlayerSkin` -> `world.entity.player`; `PlayerModel` -> `client.model.player`; `VillagerTrades` -> `world.item.trading`; `Bee/Pig/AbstractSchoolingFish` -> `animal.bee/pig/fish`; `Snowball/ThrowableItemProjectile` -> `projectile.throwableitemprojectile`; `Fireball` -> `projectile.hurtingprojectile`; `LightTexture.FULL_BRIGHT/pack/block/sky` -> `net.minecraft.util.LightCoordsUtil`; `SnowyDirtBlock` -> `SnowyBlock`; `FarmBlock` -> `FarmlandBlock`; `FungusBlock` -> `NetherFungusBlock`; `RelativeMovement` -> `Relative`; `ClientboundSetCarriedItemPacket` -> `ClientboundSetHeldSlotPacket`; `PlayerRenderer` -> `AvatarRenderer`; `ToastComponent` -> `ToastManager` |
| `serverPlayer.server` (private) | `serverPlayer.level().getServer()` |
| `entity.getServer()` (removed from Entity) | `entity.level().getServer()` (nullable on the client, like before) |
| `serverPlayer.serverLevel()` | `serverPlayer.level()` (returns `ServerLevel` for a ServerPlayer) |
| `entity.moveTo(...)` (all overloads) | `entity.snapTo(...)` |
| `resourceKey.location()` | `resourceKey.identifier()` |
| `registryAccess().registryOrThrow(k)` | `registryAccess().lookupOrThrow(k)` (returns `Registry<T>`; see 4.3 for its getters) |
| `entity.getTags()` | `entity.entityTags()` |
| `level.getMinBuildHeight()` / `getMaxBuildHeight()` | `level.getMinY()` / `level.getMaxY() + 1` (`getMaxY()` is the highest *valid* y, the old value was exclusive) |
| `EntityType.create(level)` | `create(level, EntitySpawnReason.TRIGGERED)` (26.1.2 ignores the reason in `create`) |
| `EntityType.Builder...build("guhs:x")` / `build(Guhs.id("x").toString())` | `build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("x")))` |
| `chunkPos.x` / `.z` | `chunkPos.x()` / `.z()` (ChunkPos is a record) |
| `inventory.items` / `inventory.selected` | `inventory.getNonEquipmentItems()` / `getSelectedSlot()` / `setSelectedSlot(i)` |
| `player.displayClientMessage(msg, true/false)` | `player.sendOverlayMessage(msg)` / `player.sendSystemMessage(msg)` |
| `profile.getName()/getId()` (GameProfile is a record) | `profile.name()` / `profile.id()` |
| `DirectionProperty` | `EnumProperty<Direction>` (`HorizontalDirectionalBlock.FACING` is `EnumProperty<Direction>`) |
| `RenderType.entityCutoutNoCull/entityTranslucent/eyes/energySwirl/debugQuads(...)` | `RenderTypes.entityCutout/entityTranslucent/eyes/energySwirl/debugQuads(...)` (`net.minecraft.client.renderer.rendertype.RenderTypes`). **Name shift:** 26.1 `entityCutout` = old `entityCutoutNoCull`; old culling `entityCutout` = `entityCutoutCull`. |
| `PacketDistributor.sendToServer(p)` | `ClientPacketDistributor.sendToServer(p)` (`net.neoforged.neoforge.client.network`) |
| `.requires(s -> s.hasPermission(2))` | `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))` (1 MODERATORS, 2 GAMEMASTERS, 3 ADMINS, 4 OWNERS) |
| `GameRules.RULE_*` + `getBoolean/getInt(rule)` + `getRule(r).set(v, s)` | `GameRules.<NEW_NAME>` + `get(rule)` + `set(rule, v, server)`; names: DAYLIGHT->ADVANCE_TIME, WEATHER_CYCLE->ADVANCE_WEATHER, DOMOBSPAWNING->SPAWN_MOBS, DOBLOCKDROPS->BLOCK_DROPS, KEEPINVENTORY->KEEP_INVENTORY, MOBGRIEFING->MOB_GRIEFING, DO_PATROL_SPAWNING->SPAWN_PATROLS, DO_TRADER_SPAWNING->SPAWN_WANDERING_TRADERS, ANNOUNCE_ADVANCEMENTS->SHOW_ADVANCEMENT_MESSAGES, SENDCOMMANDFEEDBACK->SEND_COMMAND_FEEDBACK. `RULE_DOFIRETICK` is gone -> `FIRE_SPREAD_RADIUS_AROUND_PLAYER` (int, 0 = no fire spread, -1 = everywhere) |
| `MobEffects.MOVEMENT_SPEED/MOVEMENT_SLOWDOWN/DIG_SPEED/JUMP/CONFUSION/DAMAGE_RESISTANCE` | `SPEED/SLOWNESS/HASTE/JUMP_BOOST/NAUSEA/RESISTANCE` (also DAMAGE_BOOST->STRENGTH, HEAL->INSTANT_HEALTH, HARM->INSTANT_DAMAGE, DIG_SLOWDOWN->MINING_FATIGUE) |
| `BlockEvent.BreakEvent` | `net.neoforged.neoforge.event.level.block.BreakBlockEvent` (same getters: `getPlayer()`, `getPos()`, `getState()`, `getLevel()`, cancellable) |
| `ITEMS.registerSimpleItem(name, props)`, `registerItem(name, f, props)`, `BLOCKS.registerBlock/registerSimpleBlock(name, [f,] props)`, `registerSimpleBlockItem(.., props)` | last argument became `() -> props` (the helpers take `Supplier<Properties>` or `UnaryOperator<Properties>`) |
| Entity `addAdditionalSaveData(CompoundTag)` / `readAdditionalSaveData(CompoundTag)`, BE `saveAdditional(CompoundTag, Provider)` / `loadAdditional(CompoundTag, Provider)` | signatures switched to `ValueOutput` / `ValueInput` (bodies mostly compile because the getters/putters match - see 4.5); `t.contains(k)` -> `t.keySet().contains(k)` in load bodies |

## 2. Build / bootstrap

* `gradle.properties`: `mod_version=1.1.0`, `minecraft_version=26.1.2`, `minecraft_version_range=[26.1.2]`, `neo_version=26.1.2.112`,
  `neo_version_range=[26.1.2.71,)`, `geckolib_version=5.5.2`, `geckolib_version_range=[5.5.2,)`, dev deps (JEI 29.34.0.90, Jade 26.1.10,
  JourneyMap 26.1.2-6.0.9, AppleSkin 3.0.9+mc26.1, Mouse Tweaks 26.1-2.31, Architectury 20.1.16, FTB Library/Teams/Filter/Quests 26.1.2.9/.4/.2/.8).
  `loader_version_range` and the Parchment properties are gone.
* `neoforge.mods.toml` (template): no `modLoader`/`loaderVersion` lines (MDK-26.1.2 style); dependency ranges from the properties.
* `guhs.mixins.json`: `JAVA_25` (sponge-mixin 0.17.3 knows JAVA_25), `GameTestRegistryMixin` removed.
* `pack.mcmeta`: none in `src/main/resources` (NeoForge generates it). Formats for 26.1.2 (for D's generators if they write one): data **101.1**, resource **84**.
* Access transformers: still supported (`META-INF/accesstransformer.cfg` is picked up automatically). Needed for e.g. `FireBlock#setFlammable` (see 4.3).

## 3. GameTests (owner B) - plan for the registrar

The annotation-based system (`@GameTest`, `GameTestRegistry`, `@GameTestHolder`) is gone in 26.1. Tests are registry entries:

```java
// net.neoforged.neoforge.event.RegisterGameTestsEvent  (mod bus; fired only when gametests are enabled)
Holder<TestEnvironmentDefinition<?>> registerEnvironment(Identifier name, TestEnvironmentDefinition<?>... definitions); // AllOf(...)
void registerTest(Identifier name, GameTestInstance test);
void registerTest(Identifier name, Function<TestData<Holder<TestEnvironmentDefinition<?>>>, GameTestInstance> factory, TestData<Holder<TestEnvironmentDefinition<?>>> data);

// net.minecraft.gametest.framework.FunctionGameTestInstance
public FunctionGameTestInstance(ResourceKey<Consumer<GameTestHelper>> function, TestData<Holder<TestEnvironmentDefinition<?>>> info)
// the function must be registered in the static registry Registries.TEST_FUNCTION (ResourceKey<Registry<Consumer<GameTestHelper>>>)

public record TestData<E>(E environment, Identifier structure, int maxTicks, int setupTicks, boolean required, Rotation rotation,
                          boolean manualOnly, int maxAttempts, int requiredSuccesses, boolean skyAccess, int padding)
```

Registrar design (`gametest/GuhsGameTests.java`, B):

1. **Find the tests** with FML's scan data (no class list to maintain):
   `ModList.get().getModFileById(Guhs.MODID).getFile().getScanResult().getAnnotatedBy(GuhTest.class, ElementType.METHOD)`
   (`ModFileScanData.AnnotationData`: `clazz()` (ASM Type), `memberName()` e.g. `"tamingGivesThousandHealth(Lnet/minecraft/gametest/framework/GameTestHelper;)V"`,
   `annotationData()`). Then `Class.forName(clazz.getClassName())`, find the `public static void name(GameTestHelper)` method and read
   the real `@GuhTest` via reflection (it has RUNTIME retention).
2. **Filter** with `GametestFilter.matches(ENTRIES, simpleClassName, batch)` (the `-Pgt=` / `-Dguhs.gametests` property is unchanged in
   build.gradle). `GametestFilter.allowed(Method)` already reads `@GuhTest` after the pass.
3. **Register the function** in a `DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Guhs.MODID)`
   (id e.g. `guhs:<classname_lower>/<method_lower>`), body `helper -> method.invoke(null, helper)` (unwrap `InvocationTargetException`
   so `GameTestAssertException` reaches the framework). Registry ids must be `[a-z0-9_./-]`: lower-case the names.
   The test-function registry is static -> the scan has to run when the DeferredRegister fires (mod bus `RegisterEvent`), before
   `RegisterGameTestsEvent`.
4. **Environments = old batches**: `registerEnvironment(Guhs.id("batch/" + batch), new TestEnvironmentDefinition.AllOf(List.of()))` once per
   distinct batch (`"defaultBatch"` -> `guhs:batch/default`). Tests with the same environment are batched together like before.
5. **Test instance**: `registerTest(id, new FunctionGameTestInstance(fnKey, new TestData<>(env, Guhs.id(template), timeoutTicks, (int) setupTicks,
   required, Rotation.values()[rotationSteps & 3], manualOnly, attempts, requiredSuccesses, skyAccess, 0)))`.
   Structure ids stay `guhs:<template>` (`data/guhs/structure/<template>.nbt`, `@PrefixGameTestTemplate(false)` semantics).
6. `/test` command ids become `guhs:<class>/<method>` (lower case). `runGameTestServer` runs every registered test (NeoForge
   `GameTestHooks.isGametestEnabled()`), `neoforge.enabledGameTestNamespaces` is still passed by build.gradle.
7. Helper API changes to expect in test bodies (verified in `GameTestHelper`): `getBlockEntity(pos, Class<T>)` (class argument now
   required), `spawn(type, pos)` still there, `makeMockPlayer(GameType)` and `makeMockServerPlayerInLevel()` still there,
   `fail(String)` still there, `assertValueEqual(v, expected, String)` still there, `startSequence()`, `runAfterDelay`, `succeed*`,
   `onEachTick` unchanged. `assertEntityProperty/assertBlockProperty` take `Component`.

**B: the registrar is done** (`gametest/GuhsGameTests.java`, called from the `Guhs` constructor):
* Write tests exactly as in 1.0.0: `@GuhTest(template = "x", timeoutTicks = .., batch = "..", required = ..)` on a
  `public static void name(GameTestHelper helper)` in any class of the mod (a `throws Exception` is fine). No holder
  annotation, no class list: FML's scan data finds every `@GuhTest` method.
* Only registered when NeoForge has gametests on (`GameTestHooks.isGametestEnabled()`: dev runs, `runGameTestServer`); a normal
  game registers nothing.
* **Test ids keep the 1.0.0 names**: `guhs:<class>.<method>` in lower case, e.g. `/test run guhs:guhgametests.portalroundtriptoguhmension`
  (not `class/method` as planned above). The test function ids are the same ids in `Registries.TEST_FUNCTION`.
* Batches -> environments `guhs:batch/<batch>` (`defaultBatch` -> `guhs:batch/default`); templates `"empty"` -> `guhs:empty`
  (a template with a namespace is used as is).
* `-Pgt=KnusGameTests,grond` works as before (class simple name or batch prefix, `GametestFilter`); the log says
  `Guhs gametests: N test methods found (-Dguhs.gametests filter: M skipped)`.
* Assertion failures reach the framework unchanged (the `InvocationTargetException` is unwrapped).
* `gametest/PortGameTests` tests the registrar itself, the 1.0.0 saved data move, the day clock, owners and entity NBT (batch `port`).

Test body fixes seen in B's test files (script `port26/scripts/b_fixes.py <files>` does the mechanical ones, idempotent):

| 1.21.1 | 26.1.2 |
|---|---|
| `(X) helper.getBlockEntity(p)` | `helper.getBlockEntity(p, X.class)` |
| `entity.saveWithoutId(tag)` / `entity.load(tag)` (CompoundTag) | `nl.juiced.guhs.storage.Nbt.saveWithoutId(entity, tag)` / `Nbt.load(entity, tag)` |
| `player.startRiding(e, true)` | `player.startRiding(e, true, true)` (`startRiding(e)` unchanged) |
| `mob.interact(player, hand)` | `mob.interact(player, hand, mob.position())` |
| `mika.doHurtTarget(target)` | `mika.doHurtTarget(helper.getLevel(), target)` |
| `transition.pos()` (TeleportTransition) | `transition.position()` |
| `new ChunkPos(blockPos)` / `new ChunkPos(long)` / `c.toLong()` | `ChunkPos.containing(blockPos)` / `ChunkPos.unpack(l)` / `c.pack()` |
| `registry.getHolder(key)` / `getHolderOrThrow(key)` / `holders()` | `registry.get(key)` / `getOrThrow(key)` / `listElements()` |
| `level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)`, `r.getResultItem(access)`, `r.getIngredients()` | `level.recipeAccess().getRecipes()` (filter `instanceof ShapedRecipe/ShapelessRecipe`), `r.assemble(CraftingInput.EMPTY)` (those two ignore the input), `r.placementInfo().ingredients()` |
| `JukeboxSong.fromStack(registries, stack)` | `JukeboxSong.fromStack(stack)` |
| Villagers: `setType(t).setProfession(p)`, `getProfession() == X`, `VillagerProfession.LIBRARIAN` (a value) | `data.withType(access, ModVillagers.GUH.getKey()).withProfession(access, key)`, `data.profession().is(key)`, `data.type().is(ModVillagers.GUH.getKey())`; `VillagerProfession.X` are `ResourceKey`s; classes live in `world.entity.npc.villager` |
| `net.minecraft.world.entity.monster.Husk` | `net.minecraft.world.entity.monster.zombie.Husk` |
| `StringBuilder.append(cond ? null : x)` "ambiguous" | `append((Object) (cond ? null : x))` |

## 4. API cheat sheet (area by area, PORT_PLAN section 4)

### 4.3 Registration and items

**Registration helpers** (`DeferredRegister.Items` / `.Blocks`, NeoForge 26.1.2):
```java
<I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func, Supplier<Item.Properties> props)
<I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func, UnaryOperator<Item.Properties> props)
<I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func)
DeferredItem<Item> registerSimpleItem(String name, Supplier<Item.Properties> | UnaryOperator<Item.Properties>)
DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block, Supplier<Item.Properties> | UnaryOperator<..>)
DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block[, props])
<B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> func, Supplier<..> | UnaryOperator<..>)
DeferredBlock<Block> registerSimpleBlock(String name, Supplier<..> | UnaryOperator<..>)
```
These helpers call `Properties#setId(ResourceKey)` for you. **Every `Item`/`Block` built outside them needs the id set before the
constructor runs** or it crashes at startup ("Item id not set"):
`ITEMS.register(name, id -> new X(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))))` (same for
`BlockBehaviour.Properties#setId(ResourceKey.create(Registries.BLOCK, id))`). Prefer converting such calls to `registerItem`/`registerBlock`.
`BlockItem`s default to the item description prefix; for "block style" names use `Item.Properties#useBlockDescriptionPrefix()`.

**Block entity types**: `BlockEntityType.Builder` is gone ->
`new BlockEntityType<>(MyBe::new, block1, block2)` / `new BlockEntityType<>(factory, Set<Block>)` (also `(factory, boolean onlyOpCanSetNbt, Block...)`).

**Block override signatures** (`BlockBehaviour`, 26.1.2):
```java
protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
                                 BlockPos neighbourPos, BlockState neighbourState, RandomSource random)
protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston)
protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston)
protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston)   // replaces onRemove(...)
protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise)
protected void tick / randomTick(BlockState, ServerLevel, BlockPos, RandomSource)          // unchanged
protected void spawnAfterBreak(BlockState, ServerLevel, BlockPos, ItemStack tool, boolean dropExperience)
protected void onExplosionHit(BlockState, ServerLevel, BlockPos, Explosion, BiConsumer<ItemStack, BlockPos> onHit)
```
Old `onRemove(state, level, pos, newState, moved)` did two jobs: dropping BE contents -> `BlockEntity#preRemoveSideEffects(BlockPos, BlockState)`
(override in the block entity, vanilla calls `Containers.dropContents` there); neighbour updates -> `affectNeighborsAfterRemoval`.
`ScheduledTickAccess ticks` replaces `level.scheduleTick(...)` inside `updateShape`: `ticks.scheduleTick(pos, fluidOrBlock, delay)`.

**Fire**: `FireBlock#setFlammable(Block, int, int)` is private -> add an access transformer line
`public net.minecraft.world.level.block.FireBlock setFlammable(Lnet/minecraft/world/level/block/Block;II)V` in
`src/main/resources/META-INF/accesstransformer.cfg` (owner D for the file, A requests it), or override
`IBlockExtension#getFlammability/getFireSpreadSpeed(BlockState, BlockGetter, BlockPos, Direction)` in our own blocks.

**Items** (`Item`, 26.1.2):
```java
InteractionResult use(Level level, Player player, InteractionHand hand)             // was InteractionResultHolder<ItemStack>
InteractionResult useOn(UseOnContext context)
InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand)
void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot)   // was (stack, Level, Entity, int slot, boolean selected); server only now
void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) // was List<Component>
ItemUseAnimation getUseAnimation(ItemStack stack)
int getUseDuration(ItemStack stack, LivingEntity user)
boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime)   // returns boolean now
ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity)
void hurtEnemy(ItemStack, LivingEntity mob, LivingEntity attacker)  // returns void now; postHurtEnemy too
```
`Item.Properties` builder (all return `Item.Properties`): `stacksTo`, `durability`, `rarity`, `fireResistant()`, `food(FoodProperties[, Consumable])`,
`craftRemainder(Item)`, `useCooldown(float s)`, `enchantable(int)`, `repairable(Item|TagKey<Item>)`, `equippable(EquipmentSlot)`,
`sword/pickaxe/axe/shovel/hoe(ToolMaterial, float attackDamageBaseline, float attackSpeedBaseline)`,
`humanoidArmor(ArmorMaterial, ArmorType)`, `wolfArmor(ArmorMaterial)`, `horseArmor(ArmorMaterial)`, `spawnEgg(EntityType<?>)`,
`component(DataComponentType<T>, T)`, `delayedComponent(type, provider -> value)`, `attributes(ItemAttributeModifiers)`, `setId`,
`useBlockDescriptionPrefix()/useItemDescriptionPrefix()/overrideDescription(String)`.

**Tools/armour (not 1:1, PORT_PLAN 7.6)**: `SwordItem/PickaxeItem/DiggerItem/ArmorItem/Tier/Tiers/SimpleTier/AnimalArmorItem/ElytraItem/Equipable` are gone.
```java
public record ToolMaterial(TagKey<Block> incorrectBlocksForDrops, int durability, float speed, float attackDamageBonus, int enchantmentValue, TagKey<Item> repairItems)
// ToolMaterial.WOOD/STONE/COPPER/IRON/DIAMOND/GOLD/NETHERITE
public record ArmorMaterial(int durability, Map<ArmorType, Integer> defense, int enchantmentValue, Holder<SoundEvent> equipSound,
                            float toughness, float knockbackResistance, TagKey<Item> repairIngredient, ResourceKey<EquipmentAsset> assetId)
// ArmorType.HELMET/CHESTPLATE/LEGGINGS/BOOTS/BODY; ArmorMaterials.IRON/DIAMOND/NETHERITE...; not a registry any more (no Holder<ArmorMaterial>)
// EquipmentAssets.createId(name) uses the minecraft namespace - for ours: ResourceKey.create(EquipmentAssets.ROOT_ID, Guhs.id("vads"))
Equippable.builder(EquipmentSlot).setEquipSound(..).setAsset(ResourceKey<EquipmentAsset>).setAllowedEntities(EntityType...).setDamageOnHurt(..).build()
```
Before/after:
```java
// 1.21.1
new PickaxeItem(Tiers.IRON, new Item.Properties().attributes(PickaxeItem.createAttributes(Tiers.IRON, 1, -2.8f)))
// 26.1.2
ITEMS.registerItem("kaashouweel", Item::new, p -> p.pickaxe(ToolMaterial.IRON, 1.0F, -2.8F))
// armour: new ArmorItem(material, ArmorItem.Type.HELMET, props)  ->  Item::new with p -> p.humanoidArmor(MAT, ArmorType.HELMET)
```
A custom item class may still extend `Item` and add behaviour; the tool/armour behaviour comes from the components the properties add
(`TOOL`, `WEAPON`, `EQUIPPABLE`, `ATTRIBUTE_MODIFIERS`). Armour textures move to equipment assets
(`assets/guhs/equipment/<asset>.json` + `textures/entity/equipment/humanoid[_leggings]/<asset>.png`, owner D).
Guh body armour (`GuhArmorItem extends AnimalArmorItem`): make it a plain `Item` with `p.component(DataComponents.EQUIPPABLE,
Equippable.builder(EquipmentSlot.BODY).setAllowedEntities(ModEntities.GUH.get())...)` (or keep the old "right-click the guh" logic)
plus `attributes(...)` for the armour value; the GeckoLib layer keeps reading the tier from the item.

**Food**: `FoodProperties` is `record (int nutrition, float saturation, boolean canAlwaysEat)`; builder has `nutrition`,
`saturationModifier`, `alwaysEdible()` only. Eating speed, sound, effects and "converts to" live in `Consumable`:
```java
// 1.21.1:  new FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).fast().effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100), 1f).usingConvertsTo(Items.BOWL).build()
// 26.1.2:
p.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build(),
       Consumables.defaultFood().consumeSeconds(0.8F)            // fast() == 0.8 s (default 1.6 s)
           .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 100), 1.0F)).build())
 .usingConvertsTo(Items.BOWL)
// drinks: Consumables.defaultDrink(); Consumable.builder().animation(ItemUseAnimation.DRINK).sound(holder)...
```

**Spawn eggs**: `DeferredSpawnEggItem` is gone. `ITEMS.registerItem(name, SpawnEggItem::new, p -> p.delayedComponent(DataComponents.ENTITY_DATA,
ctx -> TypedEntityData.of(TYPE.get(), new CompoundTag())))` (`delayedComponent` because entity types register after items; `Item.Properties#spawnEgg(type)`
does the same eagerly). Spawn eggs have **no tint colours** in 26.1: they need a texture / item model (D) -> behaviour change.

**InteractionResult** (sealed interface): `SUCCESS` (client swing), `SUCCESS_SERVER`, `CONSUME` (no swing), `FAIL`, `PASS`,
`TRY_WITH_EMPTY_HAND`; `InteractionResult.Success#heldItemTransformedTo(ItemStack)` replaces returning a new stack; `consumesAction()`.

**Registries**: `Registry#getValue(ResourceKey|Identifier)` (nullable / default), `getOptional(..)`, `getValueOrThrow(key)`,
`get(ResourceKey)` -> `Optional<Holder.Reference<T>>` (that is why `registry.get(key)` now "cannot be converted to Structure/Biome/Item":
use `getValue(key)` or `getOrThrow(key).value()`). `BuiltInRegistries.ITEM.get(id)` -> `getValue(id)`.

**Game rules / permissions / commands**: see section 1. Player permission checks: `player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)`
(`net.minecraft.server.permissions.Permissions`), same on `CommandSourceStack#permissions()`.

**Villager trades** (`VillagerTradesEvent`, `BasicItemListing`, `ItemListing` gone): 26.1 trades are the datapack registry `villager_trade` -> JSON (D + owner of the feature). `MerchantOffer`/`ItemCost` for our own NPC shops still exist.

**Chunk tickets**: `TicketType` is a non-generic record now; use `serverLevel.getChunkSource().addTicketWithRadius(TicketType.X, chunkPos, radius)` or
`serverLevel.setChunkForced(x, z, true)`.

**A (core-1) patterns, established in `registry/` + `item/` + `block/` (commits on `mc26-a`):**

* **Tools** (`ModItems`, `ModArmorMaterials`): tier = `ToolMaterial` record; `SwordItem`/`PickaxeItem` -> plain `Item::new` with
  `p -> p.sword(MAT, 3.0f, -2.4f)` / `p.pickaxe(MAT, 1.0f, -2.8f)` (same numbers as the old `createAttributes(tier, dmg, speed)`).
  `AxeItem`/`ShovelItem`/`HoeItem` still exist (right-click actions): `new AxeItem(MAT, 5.0f, -3.0f, props)`.
  A custom digger (our Paxel) = `Item` + `props.tool(MAT, TagKey<Block> minesEfficiently, dmg, speed, 0.0f)`; its `canPerformAction(ItemInstance, ItemAbility)`
  (first parameter is `ItemInstance` now). A sword subclass with extra behaviour (Mika-mepper) = `extends Item` + `.sword(..)` in the properties.
  **Sword sweep** is `stack.is(ItemTags.SWORDS)` now (NeoForge default) - our swords must be in `#minecraft:swords` (they are).
  `ToolMaterial`/`ArmorMaterial` need a `TagKey<Item>` for repairs; we keep repair-by-ingot by calling `.repairable(ModItems.X.get())` after
  `.sword/.humanoidArmor` (later `repairable` wins), so no tag file is needed.
* **Armour**: `ArmorMaterial` is a record (no registry, no `Holder`): `new ArmorMaterial(durabilityMultiplier, Map<ArmorType,Integer>, enchant, equipSound,
  toughness, knockbackRes, repairTag, ResourceKey<EquipmentAsset>)` with `ResourceKey.create(EquipmentAssets.ROOT_ID, Guhs.id("vahoege_vads"))`;
  items: `Item::new, () -> props.humanoidArmor(MAT, ArmorType.HELMET)` (sets durability/attributes/enchantable/EQUIPPABLE/repairable).
  Unbreakable: `.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)` (was `new Unbreakable(true)`).
  **Guh body armour** (`GuhArmorItem`, was `AnimalArmorItem`): plain `Item` subclass (keeps `getTier()`), properties
  `attributes(ArmorMaterials.X.createAttributes(ArmorType.BODY))` + `delayedComponent(EQUIPPABLE, ctx -> Equippable.builder(EquipmentSlot.BODY)
  .setAllowedEntities(HolderSet.direct(ModEntities.GUH.get().builtInRegistryHolder())).setDamageOnHurt(false).build())` (see `ModItems#guhArmor`).
  `delayedComponent` is the way to reference entity types/holders from item properties (entity types register after items).
* **Spawn eggs**: `ModItems.spawnEgg("x_spawn_egg", ModEntities.X)` or, for a feature's own register, `ModItems.spawnEgg(ITEMS, "x_spawn_egg", X_TYPE)`
  (returns `DeferredItem<SpawnEggItem>`; puts `TypedEntityData.of(type, new CompoundTag())` into `ENTITY_DATA` via `delayedComponent`).
  Replace `DeferredItem<DeferredSpawnEggItem> X = ITEMS.registerItem(n, p -> new DeferredSpawnEggItem(TYPE, c1, c2, p))` by it (colours are dropped, see Behaviour changes).
* **Food**: `fast()` -> `food(fp, ModItems.FAST_FOOD)` (`Consumables.defaultFood().consumeSeconds(0.8f).build()`); `usingConvertsTo(Items.BOWL)` moved from the
  FoodProperties builder to `Item.Properties#usingConvertsTo`. Drinks: `food(fp, Consumables.defaultDrink().build())`. `HoneyBottleItem` is gone: honey-like =
  `food(fp.alwaysEdible(), Consumables.HONEY_BOTTLE).usingConvertsTo(Items.GLASS_BOTTLE)`. `ItemNameBlockItem` is gone: `new BlockItem(block, props)` with
  `props.useItemDescriptionPrefix()` (keeps the `item.guhs.x` name). Registration helpers need `() -> new Item.Properties()...` (never a bare `new Item.Properties()`).
* **Item overrides**: `appendHoverText(stack, ctx, TooltipDisplay display, Consumer<Component> tooltip, flag)` + `tooltip.accept(..)` -> script
  `python port26/scripts/a_hovertext.py <dir>`. `inventoryTick(ItemStack, ServerLevel, Entity, @Nullable EquipmentSlot slot)` is **server only**; old `isSelected`
  == `slot == EquipmentSlot.MAINHAND`, the old int slot index is gone -> script `python port26/scripts/a_inventorytick.py <dir>` (prints what it cannot fix).
  `getEatingSound/getDrinkingSound` are gone: sounds + crumbs come from a `CONSUMABLE` component (see `GuhClothingItem.properties`: a Consumable is only
  used for its sound/particles when `use`/`finishUsingItem` stay overridden). `player.getCooldowns().addCooldown(ItemStack, ticks)` (was `(Item, ticks)`).
  `getCraftingRemainingItem/hasCraftingRemainingItem` -> `ItemStackTemplate getCraftingRemainder(ItemInstance)` (or `props.craftRemainder(item)`).
  `BuiltInRegistries.ITEM.get(id)` -> `getValue(id)`; `lookup.getHolder(key)` -> `lookup.get(key)` (Optional<Holder.Reference>).
  `Item#onCraftedBy(stack, player)` (no level). Recipes: `recipe.assemble(input)`, `resultContainer.setRecipeUsed(serverPlayer, holder)`.
* **Block properties**: `noCollission()` -> `noCollision()`; `hasPostProcess(pred)` -> `postProcess((state, level, pos) -> pos)`; `WaterlilyBlock` -> `LilyPadBlock`;
  `LeavesBlock(float leafParticleChance, props)` is abstract (`codec()` + `spawnFallingLeavesParticle(level, pos, random)`; cherry-style = chance 0.1).
  `DustParticleOptions(int rgb, float scale)` (was `Vector3f`): `new Vector3f(1f, 0.6f, 0.85f)` -> `0xFF99D9` (script `python port26/scripts/a_dust.py <dir>`).
* **Block overrides** - script `python port26/scripts/a_blocks.py <dir>`: `updateShape` (new order + `ScheduledTickAccess ticks`, `LevelReader level`:
  cast to `LevelAccessor` only via `instanceof` if a helper needs it), `entityInside(+ InsideBlockEffectApplier, boolean)`, `getCloneItemStack(+ boolean includeData)`,
  `neighborChanged(.., @Nullable Orientation, boolean)` (old `fromPos` is gone - printed), `fallOn(.., double)`, `propagatesSkylightDown(BlockState)`,
  `RenderShape.ENTITYBLOCK_ANIMATED` -> `INVISIBLE` (the BER still draws). **`onRemove` is gone** (script only reports it):
  - drops / cleanup that needs the block entity -> `@Override public void preRemoveSideEffects(BlockPos pos, BlockState state)` in the **block entity**
    (server, whenever the block really changes, before the BE is removed; e.g. `GuhWheelBlockEntity`, `SleeRailBlockEntity`);
  - neighbour/multiblock cleanup without BE -> `protected void affectNeighborsAfterRemoval(BlockState, ServerLevel, BlockPos, boolean movedByPiston)` on the
    block (e.g. `GuhWheelPartBlock`, `SleeRailPartBlock`, `GuhWireBlock`). The new state is not passed: it is only called when the block really changed.
    Careful: it only runs for `setBlock` with `Block.UPDATE_NEIGHBORS` (flag 1, e.g. 3) or pistons.
* **Block entities**: `BlockEntityType.Builder.of(F::new, blocks).build(null)` -> `new BlockEntityType<>(F::new, blocks...)`; `onlyOpCanSetNbt()` override ->
  `new BlockEntityType<>(F::new, true, block)`. Components: `applyImplicitComponents(DataComponentGetter)`, `removeComponentsFromTag(ValueOutput out)` -> `out.discard(k)`.
* **Villagers** (`ModVillagers`): `new VillagerType()`; `new VillagerProfession(Component name, held, acquirable, ImmutableSet<Item>, ImmutableSet<Block>, SoundEvent,
  Int2ObjectMap<ResourceKey<TradeSet>>)` (our name keeps the 1.21.1 key `entity.minecraft.villager.guhs.<name>`). `VillagerData` holds holders:
  `data.profession()/type()/level()`, `withProfession(registryAccess, KEY)`, `withType(..)`; compare with `ModVillagers.is(villager, ModVillagers.VADSSMID)`,
  `ModVillagers.isGuhVillager(villager)`, `holder.is(VillagerProfession.NONE)`. `VillagerType.byBiome(holder)` returns a `ResourceKey<VillagerType>`.
  Trades: `VillagerTradesEvent`, `BasicItemListing`, `VillagerTrades.ItemListing` are gone. Our professions keep their trade lists in code
  (`ModVillagers.trades(profession)`, record `ModVillagers.Trade` with `offer()` -> `MerchantOffer`); `mixin/VillagerMixin` (TAIL of `Villager#updateTrades`) adds two
  random ones per level like 1.21.1. `ModVillagers.kleermakerAanbod()` returns `List<Trade>` now: `offers.add(trade.offer())` (no random/entity needed).
  Own NPC shops: `new MerchantOffer(new ItemCost(item, count), resultStack, maxUses, xp, 0.05f)` is unchanged.
* **Misc**: creative painting stacks: `stack.set(DataComponents.PAINTING_VARIANT, holder)`. Server profile lookup: `server.services().nameToIdCache().get(name)`
  -> `Optional<NameAndId>` (`.id()`, `.name()`). `level.getGameRules()` exists only on `ServerLevel` (cast). `SoundEvents.GENERIC_DRINK` etc. are Holders: `.value()` for
  the `SoundEvent` overloads. `WalkAnimationState.update(speed, factor, positionScale)` (vanilla passes `isBaby() ? 3 : 1`). `Direction.getNearest(double..)` -> `getApproximateNearest`.
  Fire: A chose the **access transformer** for `FireBlock#setFlammable` (keeps the 23 call sites 1:1; D creates `META-INF/accesstransformer.cfg`, then regenerate
  the patched Minecraft jar in your worktree: `gradle_slot.sh createMinecraftArtifacts` / `writeCompileClasspath`).

### 4.4 Entities and AI

```java
// Entity
public abstract boolean hurtServer(ServerLevel level, DamageSource source, float damage);   // override this instead of hurt(...)
public boolean hurtClient(DamageSource source);
public final boolean hurtOrSimulate(DamageSource source, float damage);                      // call this where old code called entity.hurt(..)
public void kill(ServerLevel level);
public @Nullable ItemEntity spawnAtLocation(ServerLevel level, ItemStack|ItemLike [, float|Vec3 offset]);
public boolean startRiding(Entity vehicle, boolean force, boolean sendEventAndTriggers);
public boolean teleportTo(ServerLevel level, double x, double y, double z, Set<Relative> relatives, float yRot, float xRot, boolean resetCamera);
public @Nullable Entity teleport(TeleportTransition transition);
public InteractionResult interact(Player player, InteractionHand hand, Vec3 location);
protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction);
public void snapTo(double x, double y, double z [, float yRot, float xRot]);  // was moveTo
public Set<String> entityTags();                                               // was getTags
// Mob
public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data);
protected void customServerAiStep(ServerLevel level);
protected InteractionResult mobInteract(Player player, InteractionHand hand);   // unchanged
// Player
public InteractionResult interactOn(Entity entity, InteractionHand hand, Vec3 location);
// Level
public void playSound(@Nullable Entity except, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch);
public void playSound(@Nullable Entity except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch);
```
* `SoundEvents.X` is a `Holder.Reference<SoundEvent>` for many sounds -> pass `SoundEvents.X` to the Holder overloads or `.value()`.
* `ServerPlayer#playNotifySound` is gone: `sp.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, sp.getX(), sp.getY(), sp.getZ(), vol, pitch, sp.getRandom().nextLong()))`.
* `TeleportTransition(ServerLevel newLevel, Vec3 pos, Vec3 speed, float yRot, float xRot, [Set<Relative> relatives,] PostTeleportTransition post)`;
  posts: `TeleportTransition.DO_NOTHING`, `PLAY_PORTAL_SOUND`, `PLACE_PORTAL_TICKET` (compose with `.then(..)`).
* **Owners** (`TamableAnimal`, `OwnableEntity`): `getOwnerUUID()` is gone. `@Nullable EntityReference<LivingEntity> getOwnerReference()`,
  `setOwnerReference(..)`, `setOwner(LivingEntity)`, `getOwner()`, `isOwnedBy(LivingEntity)`, `tame(Player)`.
  Replacement: `EntityReference<LivingEntity> r = guh.getOwnerReference(); UUID owner = r == null ? null : r.getUUID();`.
  B adds `public @Nullable UUID getOwnerUUID()` to `GuhEntity` (and any other of our TamableAnimal bases that need it) so the ~70 call sites keep compiling (see REQUESTS).
  Old saves: vanilla reads the old `"Owner"` int-array with `EntityReference.readWithOldOwnerConversion`.
* `SynchedEntityData`, goals, `MoveControl`: mostly unchanged; `Goal#canUse` rules as before.
* **Time (not 1:1)**: `level.getDayTime()` / `setDayTime()` / `isDay()` / `isNight()` / `getTimeOfDay(pt)` / `getMoonPhase()` are gone.
  ```java
  long t = level.getOverworldClockTime();          // == old getDayTime() of the overworld (total ticks, % 24000 = time of day)
  long d = level.getDefaultClockTime();            // clock of this dimension type (dimension_type "default_clock")
  boolean day = level.isBrightOutside();           // was isDay();  isDarkOutside() was isNight()
  float sunDeg = level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, pos);   // degrees; was getTimeOfDay(pt)*360
  MoonPhase m = level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos);   // .index()
  // setting time (server):
  ServerClockManager clocks = serverLevel.clockManager();  // server.clockManager()
  Holder<WorldClock> overworld = serverLevel.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
  clocks.setTotalTicks(overworld, ticks); clocks.addTicks(overworld, n); clocks.setPaused(overworld, true); clocks.setRate(overworld, r);
  ```
* Attributes: `Attributes.X` are `Holder<Attribute>` (unchanged since 1.21); `MOVEMENT_SPEED` is still `Attributes.MOVEMENT_SPEED` - the
  missing `MOVEMENT_SPEED` symbol errors were `MobEffects` (done).

**B: entity patterns established in `entity/` (all compile against 26.1.2; copy them):**

| 1.21.1 | 26.1.2 |
|---|---|
| `guh.getOwnerUUID()` / `setOwnerUUID(uuid)` on **GuhEntity** | unchanged: `GuhEntity` keeps both (they wrap `getOwnerReference()` / `setOwnerReference(..)`) |
| `pet.getOwnerUUID()` on any other `TamableAnimal` / `OwnableEntity` | `nl.juiced.guhs.entity.Owners.uuid(pet)`; `Owners.isOwner(pet, player.getUUID())`; `pet.setOwnerReference(Owners.ref(uuid))`; copy an owner: `baby.setOwnerReference(this.getOwnerReference())` |
| `boolean hurt(DamageSource, float)` override | `boolean hurtServer(ServerLevel level, DamageSource source, float amount)` (server only; `hurt` is final). Plain `Entity` subclasses **must** implement it (`return false` = 1.21.1's default) |
| calling `target.hurt(src, dmg)` | `target.hurtServer(serverLevel, src, dmg)` on the server, or `target.hurtOrSimulate(src, dmg)` |
| `isInvulnerableTo(DamageSource)` override | LivingEntity: `isInvulnerableTo(ServerLevel level, DamageSource source)`; plain Entity: test `isInvulnerableToBase(source)` |
| `doHurtTarget(Entity)` | `doHurtTarget(ServerLevel level, Entity target)` |
| `customServerAiStep()` | `customServerAiStep(ServerLevel level)` |
| `dropEquipment()`, `spawnAtLocation(stack)` | `dropEquipment(ServerLevel level)`; `spawnAtLocation(ServerLevel level, ItemStack or ItemLike)` |
| `interact(Player, InteractionHand)` (Entity) | `interact(Player player, InteractionHand hand, Vec3 location)`; `mobInteract(Player, hand)` unchanged |
| `canBeCollidedWith()` | `canBeCollidedWith(@Nullable Entity other)` |
| `causeFallDamage(float, float, DamageSource)` | `causeFallDamage(double fallDistance, float multiplier, DamageSource source)` |
| `lerpTo(x, y, z, yRot, xRot, steps)` override (ignore server positions) | gone; override `getInterpolation()` and return an `InterpolationHandler` whose `interpolateTo(Vec3, yRot, xRot)` decides (see `GuhSleeEntity`: 0 steps, sets pos/rot directly = 1.21.1's plain `Entity#lerpTo`) |
| `new NearestAttackableTargetGoal<>(mob, Cls, 10, true, false, target -> ..)` | the selector gets the level: `(target, serverLevel) -> ..` (`TargetingConditions.Selector`) |
| `walkAnimation.update(speed, factor)` | `walkAnimation.update(speed, factor, 1f)` (position scale) |
| `Saddleable` (`isSaddleable/equipSaddle/isSaddled`) | interface gone (the saddle is an equipment slot now). `GuhEntity` keeps `isSaddleable()`, `equipSaddle(stack, source)`, `isSaddled()` (own synced flag, same "Saddle" NBT) **without `@Override`**, and takes a vanilla saddle itself in `mobInteract` (what SaddleItem did). Subclasses of GuhEntity (Parade/Kapper/Race guhs) keep their `@Override` (it overrides GuhEntity's method now) |
| `isBodyArmorItem(stack)` override | gone; `GuhEntity#isBodyArmorItem` stays as a plain method; `setBodyArmorItem` / `getBodyArmorItem` unchanged |
| `shouldDespawnInPeaceful()` override | gone: peaceful is per entity type, `EntityType.Builder#notInPeaceful()` (A: ModEntities). Keep the method without `@Override` if a test calls it |
| `Merchant` implementers | must implement `boolean stillValid(Player)` (1.21.1's MerchantMenu used `getTradingPlayer() == player`) |
| `new ServerBossEvent(name, color, overlay)` | `new ServerBossEvent(Mth.createInsecureUUID(this.random), name, color, overlay)` |
| Bee anger `setRemainingPersistentAngerTime(int)` | `setPersistentAngerEndTime(long)` (-1 = not angry) |
| `new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(x))` | `new ItemParticleOption(ParticleTypes.ITEM, x)` (an `Item`) |
| `new DustParticleOptions(new Vector3f(r, g, b), size)` | `new DustParticleOptions(0xRRGGBB, size)` (e.g. `(1f, 0.55f, 0.8f)` -> `0xFF8CCC`) |
| `SoundEvents.X` passed as a `SoundEvent` (`playSound(null, pos, SoundEvents.SHIELD_BLOCK, ..)`) | many are `Holder.Reference<SoundEvent>` now: add `.value()` |
| `level.isDay()` / `isNight()` | `level.isBrightOutside()` / `isDarkOutside()` (identical code) |
| `flyingNav.setCanPassDoors(true)` | gone from the navigation (passing doors is the node evaluator's default: true) |

**EntitySpawnReason** (1.21.2): `MobSpawnType` -> `EntitySpawnReason` (same constants, `SPAWN_EGG` -> `SPAWN_ITEM_USE`).
`finalizeSpawn(ServerLevelAccessor, DifficultyInstance, EntitySpawnReason, @Nullable SpawnGroupData)`; spawn rules
`checkXSpawnRules(EntityType<T>, ServerLevelAccessor|LevelAccessor, EntitySpawnReason, BlockPos, RandomSource)`;
`type.create(level, EntitySpawnReason.X)` (26.1.2 ignores the reason there);
`EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.LOAD, e -> {..})` (1.21.1 had no reason argument; LOAD is what it used).

**Teleports (B)**:
```java
// 1.21.1 ServerPlayer#teleportTo(ServerLevel, x, y, z, yaw, pitch)  (left the vehicle, reset the camera, changed dimension if needed)
player.teleportTo(level, x, y, z, java.util.Set.of(), yaw, pitch, true);   // true = reset camera; same or other dimension
// entity.changeDimension(transition)             -> entity.teleport(transition)   (the entity in the new level, or null)
// TeleportTransition.PostDimensionTransition     -> PostTeleportTransition;  t.postDimensionTransition() -> t.postTeleportTransition();  t.pos() -> t.position()
// new TeleportTransition(level, pos, speed, yRot, xRot, false, post)   (the boolean was missingRespawnBlock)
//                                                 -> new TeleportTransition(level, pos, speed, yRot, xRot, post)
// serverLevel.getSharedSpawnPos()                -> serverLevel.getRespawnData().pos()   (LevelData.RespawnData(GlobalPos, yaw, pitch))
```
26.1's `ServerPlayer#teleport(TeleportTransition)` also wakes a sleeping player.

**Day / time (B): use `nl.juiced.guhs.world.GuhTime`** (PORT_PLAN 7.2). In 1.21.1 every dimension read the overworld's day time
(DerivedLevelData), so the overworld clock is the exact replacement, in every dimension, on both sides:

| 1.21.1 | 26.1.2 |
|---|---|
| `level.getDayTime()` | `GuhTime.dayTime(level)` (= `level.getOverworldClockTime()`) |
| `level.getDayTime() % 24000` / `Math.floorMod(level.getDayTime(), 24000L)` | `GuhTime.timeOfDay(level)` |
| `Math.floorDiv(level.getDayTime(), 24000L)` | `GuhTime.day(level)` |
| `serverLevel.setDayTime(t)` (tests, commands) | `GuhTime.setDayTime(serverLevel, t)` (`clockManager().setTotalTicks(overworld clock, t)`; all dimensions, like 1.21.1) |
| `level.getMoonPhase()` | `GuhTime.moonPhase(level)` (same formula) |
| `level.getTimeOfDay(pt)` | `GuhTime.celestialAngle(level)` (same formula; 26.1 has no `fixed_time` value any more) |
| `level.isDay()` / `isNight()` | `level.isBrightOutside()` / `isDarkOutside()` |

The dimension types of the Guhmension etc. should keep following the overworld clock (D: `"default_clock": "minecraft:overworld"`
where a dimension has day and night).

**Ticket types (B)**: `TicketType.create(name, comparator)` is gone; ticket types are registry entries: see `world/BouwCheck.TICKET_TYPES`
(`new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION)`), then
`chunkSource.addTicketWithRadius(type, chunkPos, radius)` / `removeTicketWithRadius(..)` (no value argument).

**Structures (B)**: `structure.generate(holder, level.dimension(), registryAccess, generator, biomeSource, randomState, templates, seed, chunkPos, refs, heightAccessor, biomePredicate)`
(holder + dimension first); `getShuffledJigsawBlocks(..)` returns `StructureTemplate.JigsawBlockInfo` (`.name()`, `.info().pos()`, `.pool()`, `.target()`);
`new Beardifier(List<Rigid>, List<JigsawJunction>, BoundingBox affectedBox)` (a null box makes it return 0 everywhere: use the pieces' box
`inflatedBy(24)` like vanilla). `level.structureManager().getStructureAt(pos, structure)` needs a `Structure`: `registry.getValue(key)`
(was `registry.get(key)`).


### 4.5 NBT / persistence / networking

**ValueInput / ValueOutput** (`net.minecraft.world.level.storage`; NeoForge adds `ValueInputExtension` / `ValueOutputExtension`):
```java
// ValueOutput
<T> void store(String name, Codec<T> codec, T value);   <T> void storeNullable(String name, Codec<T> codec, @Nullable T value);
void putBoolean/putByte/putShort/putInt/putLong/putFloat/putDouble/putString(String, v);  void putIntArray(String, int[]);
ValueOutput child(String name);  ValueOutput.ValueOutputList childrenList(String name) /* addChild() */;  <T> TypedOutputList<T> list(String, Codec<T>) /* add(v) */;
void discard(String name);  boolean isEmpty();
default void store(CompoundTag tag)            // NeoForge: copies all entries of an old-style tag into this output
default void putChild(String key, ValueIOSerializable child)
// ValueInput
<T> Optional<T> read(String name, Codec<T> codec);
Optional<ValueInput> child(String name);  ValueInput childOrEmpty(String name);
Optional<ValueInputList> childrenList(String name);  ValueInputList childrenListOrEmpty(String name);   // Iterable<ValueInput>, stream()
<T> Optional<TypedInputList<T>> list(String, Codec<T>);  <T> TypedInputList<T> listOrEmpty(String, Codec<T>);
boolean getBooleanOr(String, boolean); byte getByteOr(..); int getShortOr(..); int getIntOr(..); long getLongOr(..); float getFloatOr(..); double getDoubleOr(..);
Optional<Integer> getInt(String); Optional<Long> getLong(String); Optional<String> getString(String); String getStringOr(String, String); Optional<int[]> getIntArray(String);
default Set<String> keySet()                   // NeoForge
default void readChild(String key, ValueIOSerializable object)
```
Signatures: Entity `protected abstract void addAdditionalSaveData(ValueOutput)` / `readAdditionalSaveData(ValueInput)`;
BlockEntity `protected void saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)` (no `HolderLookup.Provider` parameter -
codecs get the registries from the output/input's ops automatically). `getUpdateTag(HolderLookup.Provider)` and
`saveWithoutMetadata(Provider)` / `saveCustomOnly(Provider)` still return `CompoundTag`.

Rewrite table for the bodies (after pass5 the signatures are done):

| 1.21.1 (CompoundTag) | 26.1.2 |
|---|---|
| `tag.putX(k, v)` / `tag.getXOr(k, d)` | same names on ValueOutput/ValueInput |
| `tag.contains(k)` | `in.keySet().contains(k)` (done in load bodies) or better `in.getInt(k).isPresent()` / `in.read(k, codec).isPresent()` |
| `tag.put(k, subTag)` | `out.store(k, CompoundTag.CODEC, subTag)` or build with `ValueOutput sub = out.child(k)` |
| `tag.getCompoundOrEmpty(k)` | `in.childOrEmpty(k)` (a ValueInput) - or `in.read(k, CompoundTag.CODEC).orElseGet(CompoundTag::new)` to keep old helper code |
| `ListTag` of compounds | `ValueOutput.ValueOutputList l = out.childrenList(k); ValueOutput e = l.addChild();` / `for (ValueInput e : in.childrenListOrEmpty(k))` |
| `ListTag` of strings/ints | `out.list(k, Codec.STRING).add(s)` / `in.listOrEmpty(k, Codec.STRING).stream()` |
| `stack.save(registries)` / `ItemStack.parseOptional(registries, t)` | `out.store(k, ItemStack.CODEC, stack)` (non-empty) or `ItemStack.OPTIONAL_CODEC`; `in.read(k, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY)` |
| `ContainerHelper.saveAllItems(tag, items, registries)` / `loadAllItems(tag, items, registries)` | `ContainerHelper.saveAllItems(out, items)` / `loadAllItems(in, items)` |
| whole old helper `void save(CompoundTag)` you want to keep | `CompoundTag t = new CompoundTag(); oldSave(t); out.store(t);` and `oldLoad(in.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new))` (the NeoForge `keySet()` uses the same trick). Careful: codecs that need registries (ItemStack) must not be written into a bare CompoundTag without `RegistryOps` - use `tag.store(k, ItemStack.CODEC, registryAccess.createSerializationContext(NbtOps.INSTANCE), stack)`. |

Entity copy helpers: `entity.saveWithoutId(ValueOutput)`, `load(ValueInput)`; to get/put a CompoundTag use
`TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess())` -> `.buildResult()` and
`TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag)`.

**Persistent data** (`entity.getPersistentData()`): still a `CompoundTag` (only the Optional getters changed, done).

**SavedData** (1.21.5 + NeoForge 26.1):
```java
public record SavedDataType<T extends SavedData>(Identifier id, Factory<T> factory, Factory<Codec<T>> codecFactory, @Nullable DataFixTypes dataFixType)
new SavedDataType<>(Identifier id, Supplier<T> constructor, Codec<T> codec)          // most of ours
new SavedDataType<>(Identifier id, Factory<T> constructor, Factory<Codec<T>> codec)  // level-dependent (Factory.create(@Nullable ServerLevel))
T data = serverLevel.getDataStorage().computeIfAbsent(TYPE);   // SavedDataStorage#computeIfAbsent(SavedDataType) / get(TYPE) / set(TYPE, data)
```
No more `save(CompoundTag, Provider)` - the codec does it. Quickest port of an existing tag-based class:
`Codec<T> CODEC = CompoundTag.CODEC.xmap(T::load, t -> t.save(new CompoundTag()))` (drop the `Provider` parameter; for item stacks inside, see above).
**File location changed**: the file is `<dataFolder>/<namespace>/<path>.dat` (`Identifier#resolveAgainst`). Old 1.21.1 saves have
`data/guhs_piep_nesten.dat` etc.; 26.1's own file fixer only moves vanilla files. To keep old worlds' data: use
`Identifier.withDefaultNamespace("guhs_piep_nesten")`? -> `data/minecraft/guhs_piep_nesten.dat` - still not the old file.
Owner B: add a one-time migration (on `ServerStartedEvent`, if `<world>/data/guhs_*.dat` exists and the new file does not, move it)
and verify with a copy of a 1.0.0 world (never the user's run/ worlds). Also check where 26.1 keeps per-dimension data
(`dimensions/<ns>/<dim>/data/`) - vanilla's `DimensionStorageFileFix` moves the vanilla files only.

**Networking**: `CustomPacketPayload`, `StreamCodec`, `PayloadRegistrar#playToClient/playToServer/playBidirectional(type, codec, handler)` unchanged;
client->server: `ClientPacketDistributor.sendToServer(payload)` (done); server->client: `PacketDistributor.sendToPlayer(ServerPlayer, payload)`,
`sendToPlayersTrackingEntity(AndSelf)`, `sendToAllPlayers`, `sendToPlayersInDimension`, `sendToPlayersNear` unchanged. Client-side
handlers can be registered separately with `RegisterClientPayloadHandlersEvent` (register the payload with `playToClient(type, codec)`
without a handler on the common side) - the old single-registration style still works.

**B: saved data and entity tags: use core's helpers in `storage/` (owner A)**: `nl.juiced.guhs.storage.GuhSavedData`
(`tagType("path", T::new, T::load, t -> t.save(new CompoundTag()))` + `GuhSavedData.get(serverLevel, TYPE, "guhs_old_name")`,
which moves the 1.0.0 file once) and `nl.juiced.guhs.storage.Nbt` (`saveWithoutId(entity[, tag])`, `load(entity, tag)`,
`toTag(valueInput)`, `saveStack/parseStack`). B's classes use them (GuhWorldData `guhs:world`, Scorebord `guhs:scoreborden`,
Reisguh `guhs:reisguhs`); their old `load(tag, provider)` / `save(tag, provider)` get `null` as provider (it was never used).
`PortGameTests#portOudeSavedDataVerhuist` checks the move.

Bridging a 1.21.1 helper that writes/reads a CompoundTag inside `addAdditionalSaveData(ValueOutput)` / `readAdditionalSaveData(ValueInput)`:
```java
CompoundTag t = new CompoundTag(); emotes.save(t); out.store(t);     // NeoForge ValueOutput#store(CompoundTag): keys at the top level, like 1.0.0
emotes.load(Nbt.toTag(in));                                          // the whole input as a tag
out.store("Verstop", CompoundTag.CODEC, verstop.save());            // was tag.put("Verstop", sub)
in.read("Verstop", CompoundTag.CODEC).orElseGet(CompoundTag::new);   // was tag.getCompoundOrEmpty("Verstop")
ContainerHelper.saveAllItems(out.child("Backpack"), items);          // was tag.put("Backpack", saveAllItems(new CompoundTag(), items, regs)); same layout
ContainerHelper.loadAllItems(in.childOrEmpty("Backpack"), items);
```
* A `ListTag` element: `t.getAsString()` -> `t.asString().orElse("")`.
* Text in entity NBT (text displays, custom names) is a **component in NBT** now, not a JSON string:
  `ComponentSerialization.CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), text).getOrThrow()` and
  `tag.put("text", encoded)` (see `quest/Scorebord#show`). `Component.Serializer.toJson` is gone.
* `NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag)` -> `NbtUtils.readBlockState(BuiltInRegistries.BLOCK, tag)`.
* `CompoundTag#contains(key, TAG_COMPOUND)` for a sub-compound -> `tag.getCompound(key).isPresent()`.

**A (core-1): old-style tags <-> ValueInput/ValueOutput** - `nl.juiced.guhs.storage.Nbt` (registry context always from the entity/level):
`Nbt.saveWithoutId(entity)` (was `entity.saveWithoutId(new CompoundTag())`), `Nbt.saveWithoutId(entity, tag)`, `Nbt.save(entity, tag)` (with id),
`Nbt.load(entity, tag)`, `Nbt.input(registries, tag)` (a `ValueInput` over a tag, e.g. `EntityType.create(Nbt.input(level.registryAccess(), tag), level, EntitySpawnReason.LOAD)`,
`EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.LOAD, e -> {..})`), `Nbt.write(registries, out -> ...)` -> CompoundTag, `Nbt.toTag(valueInput)`,
`Nbt.saveStack(registries, stack)` / `Nbt.parseStack(registries, tag)` (1.21.1 `stack.save(registries)` / `ItemStack.parseOptional(registries, tag)`, same format).
In BE/entity save/load bodies: a nested CompoundTag -> `out.store("K", CompoundTag.CODEC, tag)` / `in.read("K", CompoundTag.CODEC).orElse(null)`; a codec-backed value ->
`out.store("K", CODEC, v)` / `in.read("K", CODEC).ifPresent(..)` (no `registries.createSerializationContext(..)` needed - the ops carry the registries).
`ItemStack.SINGLE_ITEM_CODEC` is gone: `ItemStack.CODEC` reads the old `{id, components}` (count defaults to 1).

**A (core-1): SavedData** - `nl.juiced.guhs.storage.GuhSavedData`:
```java
public static final SavedDataType<Scorebord> TYPE = GuhSavedData.tagType("scorebord", Scorebord::new, Scorebord::load, s -> s.save(new CompoundTag()));
Scorebord data = GuhSavedData.get(server.overworld(), TYPE, "guhs_scorebord");   // 1.21.1: getDataStorage().computeIfAbsent(new SavedData.Factory<>(..), "guhs_scorebord")
```
(`load(CompoundTag)` / `save(CompoundTag)` lose the `HolderLookup.Provider` parameter; item stacks inside via `Nbt.saveStack/parseStack` with
`server.registryAccess()` or a real codec with `GuhSavedData.type(path, ctor, codec)`). `GuhSavedData.get` first moves the 1.0.0 file
(`<world>/data/<legacy>.dat` for the overworld, `<dim>/data/<legacy>.dat` otherwise) to the 26.1 place `<world>/dimensions/<ns>/<dim>/data/guhs/<path>.dat`
if that does not exist yet - this is the migration L asked B for; B only has to use it with the old file name. The file content (`{"data": ..., DataVersion}`)
is unchanged. Vanilla's own file fixer does not touch mod files (checked `DimensionStorageFileFix`).

### 4.6 Rendering, GUI, client

**GUI (1.21.6 + 26.1)** - `GuiGraphicsExtractor` (`net.minecraft.client.gui`):
```java
void text(Font, String|Component|FormattedCharSequence, int x, int y, int argb [, boolean dropShadow]);   // default dropShadow = true
void centeredText(Font, String|Component|FormattedCharSequence, int x, int y, int argb);
void textWithWordWrap(Font, FormattedText, int x, int y, int width, int argb [, boolean shadow]);
void fill(int x0, int y0, int x1, int y1, int argb);  void fillGradient(..., int argbTop, int argbBottom);  void outline(x, y, w, h, argb);
void horizontalLine(x0, x1, y, argb); void verticalLine(x, y0, y1, argb);
void blit(RenderPipeline pipeline, Identifier tex, int x, int y, float u, float v, int w, int h, int texW, int texH [, int argb]);  // pipeline = RenderPipelines.GUI_TEXTURED
void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h [, float alpha | int argb]);
void item(ItemStack, int x, int y); void itemDecorations(Font, ItemStack, int x, int y); void fakeItem(..);
void setTooltipForNextFrame(Font, Component|List<..>, int x, int y); void setComponentTooltipForNextFrame(Font, List<Component>, int x, int y);
Matrix3x2fStack pose();   // 2D: pushMatrix()/popMatrix()/translate(x, y)/scale(x, y)/rotate(radians); no z - use nextStratum() for layering
void enableScissor(x0, y0, x1, y1); void disableScissor(); void nextStratum();
```
* **Colours need alpha** everywhere: `0xRRGGBB` is fully transparent now (the old font renderer turned alpha 0 into opaque; `fill`
  was already ARGB). All hex literals written directly in `text/centeredText` calls already had `0xFF..`; **check named constants**
  (`private static final int TEXT = 0x...`) and computed colours: use `ARGB.opaque(rgb)` / `0xFF000000 | rgb` (`net.minecraft.util.ARGB`).
* `RenderSystem.setShaderColor/enableBlend/setShaderTexture/depthMask` are gone: tint via the `argb` argument of `blit/blitSprite/text`;
  blending is part of the `RenderPipeline` (`RenderPipelines.GUI_TEXTURED` blends).
* Screen overrides: `extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a)` (was render), `extractBackground(g, mx, my, a)`
  (framework calls it before `extractRenderState` - **never call it yourself**; old `renderBackground` calls inside `render` must go,
  "blur once per frame" crash otherwise). Container screens: no abstract `renderBg` any more - override
  `extractBackground(g, mx, my, a)`, call `super.extractBackground(..)` first, then draw the panel (vanilla `ContainerScreen` does this);
  `extractLabels(g, mx, my)`. Widgets: `extractWidgetRenderState(g, mx, my, a)`; buttons: `extractContents`.
* Input: `boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)` (`event.x()/y()/button()`), `mouseReleased(MouseButtonEvent)`,
  `mouseDragged(MouseButtonEvent, double dx, double dy)`, `mouseScrolled(double x, double y, double scrollX, double scrollY)`,
  `keyPressed(KeyEvent event)` (`event.key()/scancode()/modifiers()`, `event.isEscape()`), `charTyped(CharacterEvent event)` (`codepoint()`).
* `KeyMapping(String name, int keysym, KeyMapping.Category category)`; categories are `new KeyMapping.Category(Guhs.id("guhs"))`
  (register it with `RegisterKeyMappingsEvent#registerCategory(category)` next to `register(key)`; vanilla ones `KeyMapping.Category.MISC/GAMEPLAY/...`). NeoForge ctor with `IKeyConflictContext` still exists.
* HUD: `RegisterGuiLayersEvent#registerAbove/registerBelow/registerAboveAll(Identifier id, GuiLayer layer)`,
  `GuiLayer.render(GuiGraphicsExtractor g, DeltaTracker delta)`.
* Entity in GUI: `InventoryScreen.extractEntityInInventoryFollowsMouse(...)` (picture-in-picture under the hood); custom 3D in GUI ->
  `RegisterPictureInPictureRenderersEvent`.

**Vanilla entity renderers** (`EntityRenderer<T extends Entity, S extends EntityRenderState>`):
```java
public abstract S createRenderState();
public void extractRenderState(T entity, S state, float partialTicks);   // copy what you need from the entity into your state class
public void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera);  // was render(entity, yaw, pt, pose, buffers, light)
protected float shadowRadius;  // field still there
```
Submissions instead of buffers: `collector.submitModel(model, state, poseStack, renderType, light, overlay, ...)`, `submitModelPart`, `submitItem`,
`submitBlockModel`, `submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {...})` (for our hand-drawn quads),
`submitNameTag`, `submitShadow`. Render types: `RenderTypes.entityCutout(tex)` (no cull), `entityCutoutCull`, `entityTranslucent`, `eyes`, ...
Registration unchanged: `EntityRenderersEvent.RegisterRenderers#registerEntityRenderer(EntityType<? extends T>, EntityRendererProvider<T>)`.

**BlockEntityRenderers** (`BlockEntityRenderer<T, S extends BlockEntityRenderState>`):
```java
S createRenderState();
default void extractRenderState(T be, S state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) // call BlockEntityRenderState.extractBase / super first
void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera);
```
`registerBlockEntityRenderer(BlockEntityType<? extends T>, BlockEntityRendererProvider<T, S>)`.

**Block render layers**: `ItemBlockRenderTypes` and `RenderType.cutout()/translucent()` for blocks are gone - 26.1 picks the chunk layer from
the texture's alpha (behaviour change, PORT_PLAN 7.9).

**Particles** (`net.minecraft.client.particle`):
```java
public abstract class SingleQuadParticle extends Particle {   // replaces TextureSheetParticle
    protected float quadSize, rCol, gCol, bCol, alpha, roll, oRoll; protected TextureAtlasSprite sprite;
    public SingleQuadParticle(ClientLevel level, double x, double y, double z, [double xa, double ya, double za,] TextureAtlasSprite sprite);
    protected abstract SingleQuadParticle.Layer getLayer();   // Layer.OPAQUE / TRANSLUCENT / OPAQUE_TERRAIN / ... (was getRenderType() PARTICLE_SHEET_*)
    public void setSpriteFromAge(SpriteSet sprites);
}
// Particle keeps: x,y,z,xd,yd,zd, age, lifetime, gravity, friction, hasPhysics, random (protected final RandomSource), tick(), remove()
// ParticleProvider<T>: @Nullable Particle createParticle(T options, ClientLevel level, double x, double y, double z, double xa, double ya, double za, RandomSource random);
```
Port: `extends TextureSheetParticle` -> `extends SingleQuadParticle`, pass `sprites.get(random)` (old `pickSprite(sprites)`) to the
constructor, replace `getRenderType()` by `getLayer()`. `noCollission()` is gone - `hasPhysics = false`.

**Custom skies / weather (not 1:1, PORT_PLAN 7.1/7.3)**: `DimensionSpecialEffects` and `RegisterDimensionSpecialEffectsEvent` are gone.
NeoForge 26.1.2 replacement:
```java
// mod bus, client: net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent
event.registerSkyboxRenderer(Guhs.id("guhmension"), new CustomSkyboxRenderer() {
    public boolean renderSky(LevelRenderState levelState, SkyRenderState skyState, Matrix4fc modelView, Runnable setupFog) { ...; return true; } });
event.registerWeatherEffectRenderer(id, CustomWeatherEffectRenderer)   // renderSnowAndRain(LevelRenderState, WeatherRenderState, MultiBufferSource, Vec3 cam) / tickRain(ClientLevel, int, Camera)
event.registerCloudRenderer(id, CustomCloudsRenderer)
```
They are selected per dimension/biome by the environment attributes `neoforge:custom_skybox`, `neoforge:custom_weather_effects`,
`neoforge:custom_clouds` (value = the id above) in the `attributes` of the dimension_type or biome JSON (D writes the JSON).
Colours/fog go to vanilla attributes (`minecraft:visual/sky_color`, `visual/fog_color`, `visual/fog_start_distance`, `visual/cloud_color`,
`visual/water_fog_color`, `audio/background_music`, ...; see `EnvironmentAttributes`). Guhpolder snow: instead of the `LevelRenderer` mixin,
`neoforge:custom_weather_effects` on the polder biome with a renderer that returns `true` (= no vanilla snow) is probably the clean replacement.

**Fog**: `ViewportEvent.RenderFog` (`getEnvironment()`, `getType()`, `setNearPlaneDistance/setFarPlaneDistance`, `getFogData()`) and
`ViewportEvent.ComputeFogColor` (`setRed/Green/Blue`) still exist.

**RenderLevelStageEvent**: now one subclass per stage: `RenderLevelStageEvent.AfterSky`, `AfterOpaqueBlocks`, `AfterOpaqueFeatures`,
`AfterTranslucentBlocks`, `AfterTranslucentFeatures`, `AfterTranslucentParticles`, `AfterWeather`, `AfterLevel` (listen to the class);
`getPoseStack()`, `getModelViewMatrix()`, `getLevelRenderState()`. Extraction hook: `ExtractLevelRenderStateEvent`.

**GeckoLib 5.5.2** (`com.geckolib`):
```java
public class GeoEntityRenderer<T extends Entity & GeoAnimatable, R extends EntityRenderState & GeoRenderState> extends EntityRenderer<T, R>
    GeoEntityRenderer(EntityRendererProvider.Context ctx, EntityType<? extends T> type)   // defaulted model from the entity id
    GeoEntityRenderer(EntityRendererProvider.Context ctx, GeoModel<T> model)
    withRenderLayer(GeoRenderLayer<T, Void, R>), withScale(float[, float])
// GeoRenderer hooks (default methods):
void addRenderData(T animatable, @Nullable O related, R renderState, float partialTick)   // copy per-entity data into the state: renderState.addGeckolibData(TICKET, value)
void adjustRenderPose(RenderPassInfo<R> info)                                           // pose-stack changes (was preRender scaling etc.)
void adjustModelBonesForRender(RenderPassInfo<R> info, BoneSnapshots snapshots)         // hide/scale/move bones: snapshots.ifPresent("hat", s -> s.skipRender(true))
void scaleModelForRender(RenderPassInfo<R> info, float widthScale, float heightScale)
int getRenderColor(T animatable, @Nullable O related, float partialTick)                // ARGB int (Color class is gone): 0x88FFFFFF
@Nullable RenderType getRenderType(R renderState, Identifier texture)
// RenderPassInfo: renderState(), poseStack(), model(), getGeckolibData(ticket), addBoneUpdater((info, snapshots) -> ...), addPerBoneRender(bone, ...)
// Data tickets: static final DataTicket<Float> SIZE = DataTicket.create("guhs_size", Float.class);  state.getGeckolibData(SIZE) / getOrDefaultGeckolibData
// BoneSnapshot: setScale/ setTranslation/ setRotation/ setRotX.. / skipRender(bool) / skipChildrenRender(bool)   (was GeoBone#setHidden/setScaleX/..)
// Models
abstract Identifier getModelResource(GeoRenderState renderState);   abstract Identifier getTextureResource(GeoRenderState renderState);
abstract Identifier getAnimationResource(T animatable);
// DefaultedEntityGeoModel(Identifier assetSubpath): model  guhs:entity/<path>  -> assets/guhs/geckolib/models/entity/<path>.geo.json
//                                                   anim   guhs:entity/<path>  -> assets/guhs/geckolib/animations/entity/<path>.animation.json
//                                                   tex    guhs:textures/entity/<path>.png
// Hand-written GeoModels must return the bare id (Guhs.id("entity/guh")), NOT "geo/entity/guh.geo.json".
// Animation
AnimationController(String name, int transitionTicks, AnimationStateHandler<T> handler)       // (done by the pass)
PlayState handle(AnimationTest<T> test): test.animatable(), test.controller(), test.isMoving(), test.setAndContinue(anim), test.setAnimation(anim),
    test.isCurrentAnimation(anim), test.getData(ticket), test.renderState()   // getAnimationTick() -> test.renderState().getAnimatableAge()
controller.triggerableAnim(name, raw); animatable.triggerAnim(controller, name)  // unchanged
GeoRenderLayer<T, O, R>: addRenderData(..), preRender(RenderPassInfo<R>, SubmitNodeCollector), submitRenderTask(RenderPassInfo<R>, SubmitNodeCollector)
BlockAndItemGeoLayer(EntityRendererProvider.Context ctx, GeoRenderer<T,O,R> renderer): abstract addRenderData(...) filling CONTENTS with RenderData.item(bone, ctx, ItemStackRenderState)
GeoBlockRenderer<T extends BlockEntity & GeoAnimatable, R extends BlockEntityRenderState & GeoRenderState>(BlockEntityRendererProvider.Context ctx, BlockEntityType<? extends T> type | GeoModel<T>)
```
**Rule for per-entity visuals**: the renderer no longer sees the entity while drawing. Everything a renderer used to read from the entity
(size, clothes, hat, hair, emote, instrument, colour, sleeping...) must be copied in `addRenderData` into data tickets, and the bone
changes happen in `adjustModelBonesForRender` / bone updaters from those tickets.

**Items with special rendering** (`ItemProperties`, `BEWLR`, `ModelResourceLocation`, `BakedModel`, `ItemRenderer` gone): client item
definitions `assets/guhs/items/<id>.json` (`minecraft:model`, `select`, `range_dispatch`, `condition`, `minecraft:special`); GeckoLib items
use `geckolib:geckolib` special model. Custom numeric/select properties: `RegisterRangeSelectItemModelPropertyEvent` /
`RegisterSelectItemModelPropertyEvent` / `RegisterConditionalItemModelPropertyEvent` (NeoForge client events).

### 4.6b R (render): how to port a renderer / screen / GeoBone code (reference code on branch `mc26-r`)

Reference files (all compile against 26.1.2 + GeckoLib 5.5.2): `client/GuhRenderer.java` + `client/GuhRenderFrame.java` (GeckoLib entity,
layers, bone updaters, hooks for features), `client/SittingGuhRenderers.java` (NPC models per kind, block GeoBlockRenderer),
`client/GuhSleeRenderer.java` (non-living GeoEntity + drawing other entities), `client/GuhSpawnerRenderer.java` / `GuhWheelRenderer.java` /
`SleeRailRenderer.java` (block-entity renderers), `client/GuhVillagerFeaturesLayer.java` (vanilla RenderLayer), `client/particle/GuhBlaadjeParticle.java`,
`client/screen/*` (screens), `client/GuhmensionSky.java` + `client/SkyDraw.java` + `feature/guheinde/client/GuheindeSky.java` (skies),
`mixin/client/LevelRendererMixin.java` (Guhpolder snow), `client/KaasSausClient.java` (fluid looks), `client/GuhsClient.java` (registrations).

**0. Build prerequisite (REQUESTS: R -> L).** GeckoLib 5 adds `GeoRenderState` to `EntityRenderState`/`BlockEntityRenderState` by mixin
+ interface injection. MDG must be told (`neoForge { interfaceInjectionData { from(file('gradle/geckolib_interface_injections.json')) } }`,
json = `META-INF/interface_injections.json` from the GeckoLib jar), then `gradle_slot.sh createMinecraftArtifacts` + `writeCompileClasspath`
in your worktree (and point `build/compile-classpath.txt` at your own worktree's `build/moddev/artifacts/minecraft-patched-*.jar`).
Without it every `state.addGeckolibData(..)` and `GeoEntityRenderer<T, LivingEntityRenderState>` fails to compile.

#### A. GeckoLib 5 entity renderer (was `GeoEntityRenderer<T>`)

```java
public class XRenderer extends GeoEntityRenderer<XEntity, LivingEntityRenderState> {   // EntityRenderState for non-living entities
    static final DataTicket<Float> WOBBLE = DataTicket.create("guhs_x_wobble", Float.class);       // one static ticket per value
    public XRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(Guhs.id("x")));   // bare id: geckolib/models/entity/x.geo.json, textures/entity/x.png
        this.shadowRadius = 0.5f;
        withRenderLayer(new MyLayer(this));                        // was addRenderLayer
    }
    @Override public void addRenderData(XEntity e, @Nullable Void v, LivingEntityRenderState s, float pt) {   // EXTRACT: entity is here
        s.addGeckolibData(WOBBLE, e.getWobble(pt));                // copy everything the render needs; never keep the entity
    }
    @Override public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {   // SUBMIT
        DefaultAnimations.hardcodedHeadRotation(info, bones, "head");     // was DefaultedEntityGeoModel(id, true) ("turnsHead")
        float w = info.getOrDefaultGeckolibData(WOBBLE, 0f);
        bones.ifPresent("tail", b -> b.setRotY(w));
    }
    @Override protected float getShadowRadius(LivingEntityRenderState s) { return 0.5f * s.scale * s.ageScale; }  // was set in render()
    @Override public int getRenderColor(XEntity e, @Nullable Void v, float pt) { return 0x88FFFFFF; }          // ARGB int, no Color class
    @Override public @Nullable RenderType getRenderType(LivingEntityRenderState s, Identifier tex) { return RenderTypes.entityTranslucent(tex); }
}
```
| GeckoLib 4 | GeckoLib 5 |
|---|---|
| `getTextureResource(T)` / `getModelResource(T)` in the GeoModel | `(GeoRenderState state)`: put what you need in a ticket first (`GeoModel#addAdditionalStateData(animatable, related, state)` or the renderer's `addRenderData`), read it back (`SittingGuhRenderers.NpcRenderer`: kind -> model/texture). `getAnimationResource(T)` still gets the animatable. |
| `setCustomAnimations(T, id, state)` + `getBone(..)` | renderer `adjustModelBonesForRender(info, bones)` (runs after the animations) or `info.addBoneUpdater((info, bones) -> ..)` |
| `preRender(..)` changing bone visibility | same place (`adjustModelBonesForRender`); snapshots are fresh every frame, nothing to "put back" |
| `render(entity, yaw, pt, pose, buffers, light)` extra code | before: `addRenderData`; after: override `submit(state, pose, collector, camera)` and call super first (`GuhSleeRenderer`) |
| `applyRotations(T, pose, age, yaw, pt, nativeScale)` | `applyRotations(RenderPassInfo<R>, PoseStack, float nativeScale)`; the yaw comes from ticket `DataTickets.ENTITY_BODY_YAW` (set it in `addRenderData` for non-living entities) |
| `scaleModelForRender(w, h, pose, T, model, reRender, pt, light, overlay)` | `scaleModelForRender(RenderPassInfo<R>, float w, float h)` -> call super with the new factors |
| `getMotionAnimThreshold(T)` | unchanged |
| `reRender(model, pose, buffers, T, type, buffer, ...)` (a layer drawing the model again) | `renderer.submitRenderTasks(info, collector.order(1), renderType)` (same bones) or `GuhRenderer.submitPass(info, collector, type, light, colour, boneName -> visible)` (other bones visible, e.g. clothes) |
| `BlockAndItemGeoLayer#getStackForBone/renderStackForBone` | a `GeoRenderLayer` with `addRenderData` (make an `ItemStackRenderState`: `GuhRenderer.itemState(stack, ctx, entity)`) + `addPerBoneRender(info, consumer)` -> `consumer.accept(bone, (pass, bone, collector) -> { pose tweaks; item.submit(pass.poseStack(), collector, pass.packedLight(), OverlayTexture.NO_OVERLAY, pass.renderState().outlineColor); })` (`GuhRenderer.PaperLayer`) |
| `GeoRenderLayer#render(pose, T, model, type, buffers, buffer, pt, light, overlay)` | `GeoRenderLayer<T, Void, R>`: `addRenderData(..)` (extract) + `submitRenderTask(RenderPassInfo<R> info, SubmitNodeCollector c)`; pose = `info.poseStack()` (model pose, already rotated+scaled), `info.getPreRenderMatrixPose()` = entity origin |
| `GeoBlockRenderer<T>(GeoModel)` | `GeoBlockRenderer<T, BlockEntityRenderState>(BlockEntityRendererProvider.Context, GeoModel)` |
| `new GeoEntityRenderer<>(ctx, model)` inline | `new GeoEntityRenderer<XEntity, LivingEntityRenderState>(ctx, model)` (type args needed) |

**GeoBone -> BoneSnapshot** (`bones.ifPresent("name", b -> ..)`, `bones.get(geoBone)`):

| GeckoLib 4 (`GeoBone`) | GeckoLib 5 (`BoneSnapshot`) |
|---|---|
| `setHidden(true)` (hid the bone *and* its children) | `skipRender(true).skipChildrenRender(true)` |
| `setHidden(h); setChildrenHidden(false)` | `skipRender(h)` (children decide for themselves) |
| `setPosX/Y/Z(v)` | `setTranslateX/Y/Z(v)` (same pixel units) |
| `setRotX/Y/Z(r)` (radians) | `setRotX/Y/Z(r)`: **relative to the model's own (base) rotation**; for bones without a base rotation this is the same number. Old `x + bone.getInitialSnapshot().getRotZ()` -> `x`; `bone.getRotZ()` = this frame's animated value (relative) |
| `setScaleX/Y/Z` | same names |
| `getName()`, `getChildBones()`, `getPivotX()` | `bone.name()`, `bone.children()`, `bone.pivotX()` on the `GeoBone` (`snapshot.getBone()`) |
| `model.topLevelBones()` loop | `info.model().boneLookup().get().values()` (all bones) |

**Guh features (GuhRenderer hooks).** Features never touch `GuhRenderer`; they register a hook in their client init:
```java
GuhRenderer.hook((guh, partialTick, frame) -> {          // extract time, guh available
    if (guh.getVariant() != GuhVariant.PINGUH) return;
    frame.texture(PinguhRender.texture(guh), 20);          // fur texture, highest priority wins (Pinguh 20, story variants 10)
    float glij = PinguhRender.glij(guh, partialTick);      // compute now ...
    frame.pose(pose -> pose.translate(0, -0.11f * glij, 0));                                   // after the body rotation
    frame.bones(bones -> bones.ifPresent("leg_front_left", b -> b.setRotX(Mth.lerp(glij, b.getRotX(), 1.3f))));   // ... use later
    frame.glow(GLOW_TEXTURE);                                                                 // extra full-bright pass
    frame.pass(GuhClothes.PYJAMA_PAKJE.texture(), 0xFFFFFFFF, GuhClothes.PYJAMA_PAKJE::shows);  // model again, only these bones
    ItemStackRenderState item = GuhRenderer.itemState(stack, ItemDisplayContext.GROUND, guh);
    frame.layerExtra((pose, collector, light) -> { pose.translate(0, 1.5, 0); item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0); });
});
```
Porting table for the 1.0.0 hook APIs (owners: guhpolder S2, verhaal + knus S3, users of `GuhRenderHooks.laag` S1/S2/S3/S5):
`PinguhRender.texture/pose/animate` -> one hook (`frame.texture(.., 20)`, `frame.pose(..)`, `frame.bones(..)`);
`VariantUiterlijk.Uiterlijk#texture/glow/botten/extra` -> `frame.texture(.., 10)`, `frame.glow(..)`, `frame.bones(..)`, `frame.extra(..)`
(entity space, unscaled, as before) - simplest: `VariantUiterlijk` registers ONE `GuhRenderer.hook` that looks up the variant's `Uiterlijk`
and the `Uiterlijk` methods get a `GuhRenderFrame` parameter; `GuhRenderHooks.Laag` -> `GuhRenderer.Hook` (`laag(..)` can simply call
`GuhRenderer.hook`), re-render with hidden bones -> `frame.pass(..)`, item renders -> `frame.layerExtra(..)` (same pose as the old layers:
entity origin, not rotated, scaled with baby size/squish). `GuhRenderer.slaapt(guh)` / `slaap(texture)` are unchanged.
`SittingGuhRenderers.NPC_ANIMATORS`: `(npc, state, bot) -> { float t = (float) state.getAnimationTick() * k; bot.apply("x").ifPresent(b -> b.setRotZ(..)); }`
becomes `(npc, tick) -> { float t = (float) tick * k; return bones -> bones.ifPresent("x", b -> b.setRotZ(..)); }` (compute npc data outside the returned lambda).
NPC model/animation ids in `NPC_MODELEN`/`NPC_ANIMATIES` are bare ids now: `Guhs.id("entity/guh_npc_x")`.
Subclasses of `GuhRenderer` (race ghost): override `getRenderType(LivingEntityRenderState, Identifier)` and `int getRenderColor(GuhEntity, Void, float)`
(e.g. `ARGB.colorFromFloat(0.6f, 1f, 0.82f, 0.25f)`); the variant etc. is in `GuhRenderer.frame(state)`.

**Drawing another entity from a renderer** (sled pullers, guh in the wheel, spawner): at extract time
`EntityRenderState s = dispatcher.extractEntity(entity, pt); s.lightCoords = myState.lightCoords; s.shadowPieces.clear();` (was `setRenderShadow(false)`),
at submit time `dispatcher.submit(s, camera, x, y, z, poseStack, collector)`. `context.getEntityRenderDispatcher()` (entity) / `context.entityRenderer()` (BER).

#### B. Vanilla entity renderers / layers
`EntityRenderer<T, S extends EntityRenderState>`: `S createRenderState()`, `extractRenderState(T, S, float pt)` (call super), `submit(S, PoseStack,
SubmitNodeCollector, CameraRenderState)`. Model parts: `collector.submitModelPart(part, pose, renderType, light, overlay, null)`; whole models
`collector.submitModel(model, state, pose, renderType, light, overlay, outline, null)`; hand-made quads `collector.submitCustomGeometry(pose, renderType,
(poseEntry, vertexConsumer) -> { .. })` (vertex code unchanged). Layers: `RenderLayer<S, M>` with `submit(PoseStack, SubmitNodeCollector, int light, S state,
float yRot, float xRot)`; `LivingEntityRenderer.getOverlayCoords(state, 0f)`. Villager example: `GuhVillagerFeaturesLayer` (`state.villagerData.type().value()`).
Texture override of a vanilla renderer: `getTextureLocation(SlimeRenderState)` (the state, not the entity).

#### C. Block-entity renderers (`GuhSpawnerRenderer`, `GuhWheelRenderer`, `SleeRailRenderer`)
```java
public class XRenderer implements BlockEntityRenderer<XBlockEntity, XRenderer.State> {
    public static class State extends BlockEntityRenderState { float spin; }
    public State createRenderState() { return new State(); }
    public void extractRenderState(XBlockEntity be, State s, float pt, Vec3 cam, @Nullable ModelFeatureRenderer.CrumblingOverlay crumble) {
        BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, crumble);   // pos, type, lightCoords
        s.spin = be.getSpin(pt);                                               // anything that needs the level: HERE (e.g. LevelRenderer.getLightCoords)
    }
    public void submit(State s, PoseStack pose, SubmitNodeCollector c, CameraRenderState camera) { .. }
    public boolean shouldRenderOffScreen() { .. }                              // no parameter any more; getRenderBoundingBox(T) unchanged
}
```
Extra block models (was `ModelEvent.RegisterAdditional` + `ModelResourceLocation` + `BakedModel`): `StandaloneModelKey<BlockStateModelPart> KEY =
new StandaloneModelKey<>(() -> "guhs:block/x")`, register in `ModelEvent.RegisterStandalone`: `event.register(KEY, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/x")))`,
draw: `c.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(Minecraft.getInstance().getModelManager().getStandaloneModel(KEY)), BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0)`.
`LevelRenderer.getLightColor(level, pos)` -> `LevelRenderer.getLightCoords(level, pos)`.

#### D. Screens / HUD (`client/screen/*`)
Script: `python port26/scripts/r_gui_input.py <files>` (idempotent) converts input overrides and super calls (`mouseClicked(MouseButtonEvent, boolean)`,
`mouseReleased(MouseButtonEvent)`, `mouseDragged(MouseButtonEvent, dx, dy)`, `keyPressed(KeyEvent)`, `charTyped(CharacterEvent)` with the old local
variables recreated), `Screen.hasShiftDown()` -> `Minecraft.getInstance().hasShiftDown()`, `renderEntityInInventoryFollowsMouse` ->
`extractEntityInInventoryFollowsMouse`, container `this.imageWidth/Height = ..` -> `super(menu, inv, title, w, h)` (fields are final),
`renderBg(g, pt, mx, my)` -> `extractBackground(g, mx, my, pt)` + `super.extractBackground(..)` first. By hand:
* `renderTooltip(g, mx, my)` in a container screen: delete it, `AbstractContainerScreen#extractRenderState` already calls `extractTooltip`; to suppress the
  slot tooltips override `extractTooltip` (`GuhWardrobeScreen`).
* enter key: `event.isConfirmation()`; escape `event.isEscape()`; `editBox.keyPressed(event)`; press a button from code: `button.onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0))`.
* Entity preview with custom angles (was `InventoryScreen.renderEntityInInventory(g, x, y, scale, translation, pose, camera, entity)`):
  `EntityRenderState s = minecraft.getEntityRenderDispatcher().extractEntity(e, 1f); s.shadowPieces.clear(); s.outlineColor = 0;
  g.entity(s, scale, translation, pose, camera, x0, y0, x1, y1)` - drawn picture-in-picture, centred in the rectangle (`GuhWardrobeScreen#renderPop`).
* Colours: every `text/centeredText/fill` colour must be ARGB (`0xFFrrggbb`); all R screens already were (checked: no 6-digit literals, no
  `getColor()` results). `(alpha << 24) | 0xRRGGBB` computed colours are fine. Old `renderBackground` inside `render` must go (framework calls
  `extractBackground`); a screen without blur overrides `extractBackground` without calling super (`SledPanelScreen`).
* `BuiltInRegistries.ITEM.get(id)` -> `getValue(id)`; `item.getDescription()` -> `Component.translatable(item.getDescriptionId())`.
* HUD layers: `RegisterGuiLayersEvent#registerAbove/Below/AboveAll(id, GuiLayer)`; `GuiLayer` is still `render(GuiGraphicsExtractor, DeltaTracker)`.
  **Trap:** the phase-1 pass renamed every `render(GuiGraphicsExtractor ..)` method to `extractRenderState`, also in plain HUD helper classes, so
  method references like `SleeHud::render` no longer resolve -> use `SleeHud::extractRenderState` (or rename the method back).
* Key mappings: `new KeyMapping(name, InputConstants.Type.KEYSYM, key, GuhKeys.CATEGORY)` - use the shared `GuhKeys.CATEGORY` (registered once in
  `GuhKeys.register`; its label is lang key `key.category.guhs.guhs`).

#### E. Particles (`GuhBlaadjeParticle`)
`TextureSheetParticle` -> `SingleQuadParticle` (sprite in the constructor: `sprites.get(random)`), `getRenderType()` -> `SingleQuadParticle.Layer getLayer()`
(`Layer.TRANSLUCENT` = old PARTICLE_SHEET_TRANSLUCENT, `Layer.OPAQUE` = PARTICLE_SHEET_OPAQUE). Providers get a `RandomSource` as 9th argument:
`event.registerSpriteSet(TYPE, sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new P(level, x, y, z, sprites.get(random)))`.
`CherryParticle` -> `FallingLeavesParticle(level, x, y, z, sprite, 0.25F, 2.0F, false, true, 1.0F, 0.0F)`. `pickSprite(sprites)` in a constructor ->
pass `sprites.get(random)` to super, `setSpriteFromAge(sprites)` unchanged.

#### F. Skies, weather, fluids, block tints
* Sky: `CustomSkyboxRenderer#renderSky(LevelRenderState, SkyRenderState, Matrix4fc modelView, Runnable setupFog)` registered in
  `RegisterCustomEnvironmentEffectRendererEvent#registerSkyboxRenderer(id, r)` (`GuhmensionSky.register` registers guhs:guhmension and guhs:guheinde),
  selected by the dimension type/biome attribute `"neoforge:custom_skybox": "guhs:<id>"` (and `"skybox"` must not be `none`, else no sky pass runs).
  No immediate mode: `SkyDraw.drawNow(label, pipeline, meshData, texture, colourModulator, modelView)` / `SkyDraw.draw(.., GpuBuffer ..)`
  (RenderPass + RenderPipeline like vanilla `SkyRenderer`); own pipelines must be registered in `RegisterRenderPipelinesEvent` (`SkyDraw.registerPipelines`).
  Sky colour/angles/star brightness come from `SkyRenderState` (environment attributes); `new SkyRenderer(textureManager, atlasManager)` gives
  vanilla's `renderSkyDisc(colour)` / `renderSunriseAndSunset(pose, sunAngle, colour)`. Barbecuether (S7): its old `SkyType.NONE` + fog is pure
  data now (`"skybox": "none"` + fog attributes); no renderer needed.
* Guhpolder snow: `@ModifyReturnValue` on the private `WeatherEffectRenderer#getPrecipitationAt(Level, BlockPos)` (SNOW -> NONE in polder biomes).
* Fluids: textures/tint moved from `IClientFluidTypeExtensions` to `RegisterFluidModelsEvent#register(new FluidModel.Unbaked(new Material(still),
  new Material(flow), overlayOrNull, FluidTintSources.constant(argb) | null), stillFluid, flowingFluid)`; fog colour stays in the extension as
  `modifyFogColor(.., Vector4f fluidFogColor)` (mutate it).
* Block tints: `RegisterColorHandlersEvent.BlockTintSources#register(List.of(BlockTintSource), blocks)`; `BlockTintSource#color(BlockState)` returns ARGB
  and `relevantProperties()` lists the properties that change the colour (else no re-tint on state change).
* Item properties (compass needle etc.): `ItemProperties.register` is gone -> client item definition JSON (D), e.g. `range_dispatch` with
  `"property": "minecraft:compass", "target": "lodestone"`.

### 4.7 Mixins

| Mixin | 26.1.2 target |
|---|---|
| `FlowingFluidMixin` (`canSpreadTo`, gone) | `FlowingFluid#canMaybePassThrough(BlockGetter level, BlockPos sourcePos, BlockState sourceState, Direction direction, BlockPos testPos, BlockState testState, FluidState testFluidState)` (private, used by both downward `spread` and sideways `getSpread`) -> `@Inject(method = "canMaybePassThrough", at = @At("HEAD"), cancellable = true)` returning `false` |
| `ServerLevelMixin` (`tickPrecipitation(BlockPos)`) | still `public void tickPrecipitation(BlockPos pos)` in `ServerLevel` - unchanged |
| `client.LevelRendererMixin` (`renderSnowAndRain`, gone) | `WeatherEffectRenderer#getPrecipitationAt(Level, BlockPos)` (private, used by `extractRenderState` and `tickRainParticles`) or the NeoForge `custom_weather_effects` attribute (4.6) |
| `GameTestRegistryMixin` | deleted (filter lives in the registrar) |

* A: `FlowingFluidMixin` retargeted to `canMaybePassThrough(BlockGetter, BlockPos, BlockState, Direction, BlockPos, BlockState, FluidState)` (no `Fluid` parameter);
  `ServerLevelMixin` unchanged; new `VillagerMixin` (`Villager#updateTrades(ServerLevel)`, TAIL) for the guh trades - needs `"VillagerMixin"` in `guhs.mixins.json` (requested).

### 4.8 Data / resources (owner D; code owners only need the ids)

* GeckoLib assets: `assets/guhs/geckolib/models/<entity|block|item>/*.geo.json`, `assets/guhs/geckolib/animations/...` (bare ids, see 4.6).
* Equipment assets for armour, client item definitions for every item, recipes with plain-string ingredients, `random_patch` removed,
  biome/dimension `attributes`, `villager_trade` registry - see PORT_PLAN 4.8.

**D (data/tools): how the resources are made for 26.1.2** (branch `mc26-d`)

* The generators still build everything in their old shapes; **`tools/mc26.py`** turns the tree into 26.1.2 formats and is
  the last step of `python tools/make_resources.py` (run alone: `python tools/mc26.py`; it only rewrites files still in an old
  shape, a second run writes nothing). A full `make_resources.py` run reproduces the committed tree byte for byte (except
  gzip timestamps of the .nbt files and one ogg: do not commit those).
* The generators need the **1.21.1 vanilla resources jar** for their recoloured vanilla textures (keeps the textures identical
  to 1.0.0): copy `guhs/build/moddev/artifacts/neoforge-21.1.251-client-extra-aka-minecraft-resources.jar` into
  `<worktree>/build/moddev/artifacts/` once (`mc26.py` itself also finds it in `../guhs/build/...`).
* **`tools/check_datapack26.py`** loads `src/main/resources/data` into a plain 26.1.2 dedicated server (the server jar from the
  Gradle cache, JDK 25 from `~/.gradle/jdks`) and prints every file the game refuses; `--gen` also generates chunks in the four
  Guhs dimensions. Ids only our Java code knows (blocks, items, entities, sounds, particles, feature/structure/placement/
  pool-element/processor types) are replaced by vanilla stand-ins first, so what it reports are real format errors. It is a
  stop-gap until `runGameTestServer` runs; `tools/check_assets.py` does the same for the client assets (items/ definitions,
  models, textures incl. the minecraft: ones against the 26.1.2 client jar, equipment, GeckoLib folders).
* Ids code owners need:
  * item model properties: guh compasses = vanilla `minecraft:compass` (target `lodestone`), Guhvis-hengel = vanilla
    `minecraft:fishing_rod/cast`, motknabbel = vanilla `minecraft:block_state` (`kleur`) -> **delete** the old
    `ItemProperties.register` calls (GuhsClient, KaasmoerasClient, VissenClient). Two need code: the guhxolotl emmertje uses the
    select property **`guhs:guhxolotl_kleur`** (register with `RegisterSelectItemModelPropertyEvent`, value =
    `GuhxolotlEmmertje.kleur(stack).getSerializedName()`: roze/mint/choco/wit/goud), and the Mewtwo tankonderdeel uses
    `minecraft:custom_model_data` **floats[0]** 1..4 (`new CustomModelData(List.of((float) n), List.of(), List.of(), List.of())`).
  * items with NeoForge `separate_transforms` models (guhhuisje_klein/medium/groot, guh_glijbaantje, guh_schommel, guh_wip,
    guh_schaatsen) are `minecraft:display_context` selects now - no code needed.
  * spawn eggs: every `*_spawn_egg` has its own texture `guhs:item/<name>` baked from the 1.21.1 template + old colours; the
    items only need `SpawnEggItem` + the entity type component.
  * equipment assets: `guhs:vahoege_vads`, `guhs:duikhelm`, `guhs:knabbelkroon` (humanoid + humanoid_leggings) -> use
    `ResourceKey.create(EquipmentAssets.ROOT_ID, Guhs.id("duikhelm"))` etc. in the armour materials / Equippable.
  * skies: dimension types carry `neoforge:custom_skybox` = `guhs:guhmension` / `guhs:guheinde` (register those ids with
    `RegisterCustomEnvironmentEffectRendererEvent`); barbecuether/guhmaag have `skybox: none`. No `custom_weather_effects`
    is set anywhere yet (Guhpolder snow: tell D the renderer id if you want it on the guhpolder biome).
  * `META-INF/accesstransformer.cfg`: `FireBlock#setFlammable` is public (A's request); regenerate the patched jar
    (`gradle_slot.sh createMinecraftArtifacts`) in your worktree after merging.
  * Lang: `key.category.guhs.guhs` (26.1 key category label) exists, and every block with an item also has an
    `item.guhs.<name>` name (26.1 BlockItems made without `useBlockDescriptionPrefix()` look that one up).
  * GeckoLib ids in Java must be bare: `Guhs.id("entity/guh_npc_x")`, not `Guhs.id("geo/entity/guh_npc_x.geo.json")` -
    GeckoLib 5's legacy-path fallback only strips a leading `geckolib/`/`models/` and the `.geo.json` suffix, so
    `geo/entity/...` ids end up as "missing model" at runtime.
  * Entity NBT used by advancement predicates (keep these keys in the save data): guh `Saddle` (byte), guh `Variant`
    (string), Mika `Boss` (byte).
* Structure templates (`data/guhs/structure/*.nbt`, also the gametest rooms) keep **DataVersion 3955 (1.21.1)** on purpose:
  `make_structures.py` writes 1.21.1 block names/block entity data, and 26.1.2's DataFixer upgrades every older template
  when it is loaded (e.g. `minecraft:chain` -> `minecraft:iron_chain`, sign texts, item components).

## Behaviour changes

(append, prefix with your role)

* L: `RULE_DOFIRETICK` no longer exists. `Kaasfrituursaus` now treats "fire does not spread" as `GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER == 0`
  (26.1 replacement of doFireTick=false); the dev AutoCheck sets that rule to 0 instead of doFireTick=false.
* L: `CompoundTag.contains(key, Tag.TAG_X)` became `contains(key)` (26.1 has no typed contains); a key holding a different tag type now
  counts as present and the typed getter then returns the default.
* L: `EntityType.create(level)` calls pass `EntitySpawnReason.TRIGGERED`; 26.1.2 ignores that argument in `create`, so no gameplay effect.
* L: Spawn eggs lose their two tint colours in 26.1 (vanilla eggs are plain textures now); D/A need textures or tinted item models.
* L (to verify by B): SavedData files move from `data/<name>.dat` to `data/<namespace>/<path>.dat`; without a migration step, data
  of 1.0.0 worlds (bank, highscores, nests, band, huisjes, ...) would not be found after the update.
* B: A guh gets its saddle by right-clicking it with a vanilla saddle, now handled in `GuhEntity#mobInteract` (26.1 has no
  `Saddleable`; vanilla's saddle is an equipment slot now). Same conditions (tamed, rideable size, alive, not saddled yet), same
  sound, the saddle is used up; the "Saddle" NBT flag and the drop on death are unchanged. The saddle is not in the vanilla SADDLE
  slot, so vanilla's saddle logic (`#can_equip_saddle`, shears) does not apply; Guhs draws its own saddle bone as before.
* B: `/test` ids are `guhs:<class>.<method>` (the same names as 1.0.0); batches are test environments `guhs:batch/<batch>`.
* B: Guh sled (`GuhSleeEntity`): "ignore server positions while on a rail" moved from `lerpTo` to a custom interpolation
  handler; without a rail it sets position/rotation at once like 1.21.1's `Entity#lerpTo` (no smoothing, as before).
* B: `/guhs bouwcheck` uses a registered ticket type `guhs:bouwcheck` (loading + simulation, no timeout) instead of an
  unregistered `TicketType.create`.
* B: Players teleported by Guhs (guhmaag, Reisguh, verstoppertje, kasteelpoort) are woken up first if they sleep (26.1 `ServerPlayer#teleportTo`).
* B: `QuestGuhEntity` / `MikaBaasEntity` / `GuhNpcEntity` invulnerability is only asked on the server now (`isInvulnerableTo(ServerLevel, ..)`);
  the client may show a hurt flash that 1.21.1 did not (cosmetic).
* B: `GuhTime.moonPhase/celestialAngle` ignore the old `fixed_time` value of fixed-time dimensions (26.1 dimension types only
  have `has_fixed_time`); nothing in Guhs asks for them in a fixed-time dimension.
* B: SavedData files get new ids (`guhs:world`, `guhs:scoreborden`, `guhs:reisguhs`, ...): the 1.0.0 file is moved to
  `<world>/dimensions/minecraft/overworld/data/guhs/<path>.dat` the first time it is needed (a 1.1.0 world can't go back to 1.0.0 anyway).
* A: Spawn eggs are `SpawnEggItem` with the type in `ENTITY_DATA` (no `DeferredSpawnEggItem`); tint colours are gone (needs D textures).
* A: Guh villager trades are no longer data driven by NeoForge's `VillagerTradesEvent` (removed); the same lists live in `ModVillagers` and are added by
  `VillagerMixin` after vanilla's trade update (two random per level, like 1.21.1). Datapacks can not change them (1.0.0 could only via the event either).
* A: Guh armour is a plain `Item` with body-slot attribute modifiers + an `EQUIPPABLE` component limited to guhs (was `AnimalArmorItem`, `BodyType.CANINE`);
  armour value/toughness are the same (from the vanilla material); the guh still only gets it by our right-click / the wardrobe.
* A: Vads tools/armour: `ToolMaterial`/`ArmorMaterial` records; stats identical. Anvil repair with the ingot is set per item (`repairable(ingot)`).
  Paxel/axe right-click and mining as before; old `DiggerItem` extra durability damage on hit is irrelevant (unbreakable).
* A: Kaashoning (was `HoneyBottleItem`) uses vanilla honey's 26.1 consumable (2 s drink, clears poison, bottle back; always drinkable as before).
* A: Clothing unlock ("eat" a clothing item, 1.5 s): sound/crumbs come from a `CONSUMABLE` component (same leather sound; timing of the crumbs is vanilla's).
* A: `PickedUpGuhItem` "waar is mijn guh" pocket check: 26.1 `inventoryTick` has no slot index; the check runs every 100 ticks for every picked-up guh
  (was spread over slots with `gameTime + slot`).
* A: Block removal side effects (`onRemove` gone): Guh wheel (drops the guh + removes parts) and sled rail anchor (removes parts) moved to the block entity's
  `preRemoveSideEffects` (same triggers as before); wheel parts, rail parts and guh wire use `affectNeighborsAfterRemoval`, which only runs for block changes with
  neighbour updates (flag 1) or pistons: removing a part with `setBlock(.., 2)` no longer breaks the rest of the multiblock.
* A: Guh blossom leaves use 26.1 `LeavesBlock` falling-leaf logic (chance 0.1 per animate tick, same as the old 1 in 10).
* A: `FlowingFluidMixin` now also stops the fluid's slope search at protected buildings (same outcome: no flow into them).
* R: Guhmension sky: sun, pink moon and pink stars now follow vanilla 26.1's separate sun/moon/star angle attributes (by default the same
  day cycle as before, moon opposite the sun); the sky disc and sunrise glow are vanilla's drawn with the attribute colours (sky/fog colours must
  be set in the dimension/biome `attributes`, D). The old "skip the sky when foggy" check is gone (vanilla itself skips the sky under lava/powder snow).
  Moon size 26, full moon, 1800 pink/lilac/white stars with the same seed and the same twinkle - look should match.
* R: Guheinde sky: same six-sided purple swirl (uv 0..8, drift 0.0004/tick, colour 0xFF6A3A64); needs `"skybox": "end"` +
  `"neoforge:custom_skybox": "guhs:guheinde"` and fog colour (0.24, 0.12, 0.2) as attribute (D).
* R: Guhpolder snow: same result (no vanilla snow columns in polder biomes); the mixin now targets `WeatherEffectRenderer#getPrecipitationAt`
  with require = 1 (a broken target now fails loudly instead of silently drawing both kinds of snow).
* R: Guh compasses (9 items): the needle is a client item definition (`minecraft:compass` / `lodestone` target, D) instead of code; same behaviour.
* R: Guh key category lang key is now `key.category.guhs.guhs` (was `key.categories.guhs`; D adds it to en_us/nl_nl, text "Guhs").
* R: Ghost guh: alpha 0x88 as before, but when the guh is also invisible-to-you the (lower) invisibility alpha wins instead of 0x88.
* R: Guh render hooks (features): extra model passes/items are submitted after the clothes like before; hook items drawn with `layerExtra` get the
  1.0.0 layer pose (entity origin, not rotated, baby size/squish scale). Visual check in phase 6.
* R: Guh wheel stand/ring are drawn with the cutout block sheet (was `RenderType.cutout()`); same look.
* R: Kaas saus / maagzuur: fluid textures/tint via `FluidModel` (same textures, maagzuur tint 0xFFB8E03A over water textures); the render layer
  is picked from texture alpha now (PORT_PLAN 7.9).
* R: dev AutoCheck: the check world's game rules are set right after joining (26.1 `LevelSettings` has no game rules); "mist off" can no longer
  cancel the fog event, it only pushes the terrain fog out to the render distance.
* D: **Biome/dimension looks** are environment attributes now (1.21.11). Converted 1:1: fog/sky/water fog colours, ambient
  particles, ambient loop/mood/additions sounds, music. Music follows 1.21.1's rules for non-vanilla dimensions exactly:
  every biome/dimension has `background_music` = biome music (or the game music) + creative music (1.21.1 played the
  creative music when flying in creative outside the nether); none of our biomes played the under-water music. Dimension
  looks come from the 1.21.1 effects: Guhmension = overworld sky/lighting/clouds (cloud height 192.33 = the old 192 + the
  0.33 the old cloud renderer added) + `neoforge:custom_skybox guhs:guhmension`; Guheinde = end sky/lighting, fog
  #3d1f33 + `neoforge:custom_skybox guhs:guheinde`; Guhmaag (old `effects: the_nether`) and Barbecuether = nether lighting,
  no sky, nether fog distances (10/96, 1.21.1's "foggy" dimensions), fog colour per biome.
* D: **Dimension type flags** became attributes: `ultrawarm` -> water evaporates + fast lava + lava dripstone particle,
  `bed_works: false` -> exploding bed, `has_raids: false` -> `can_start_raid false`, `natural` -> nether portals spawn
  zombified piglins (Guhmension). `fixed_time` -> `has_fixed_time` + `sky_light_level` 4 for the midnight dims (15 - the
  old skyDarken 11) and no day timeline (`#minecraft:universal`); the Guhmension keeps the overworld clock
  (`default_clock minecraft:overworld`, `#minecraft:in_overworld` timelines) like 1.21.1 shared the overworld day time.
* D: **random_patch** (24 flower/grass/fire/plant patches): the configured feature is now the inner feature, and the placed
  feature got `count(tries)` + `random_offset(trapezoid +-xz, +-y)` + the old inner filters. Same distribution as
  random_patch's `rand(n+1) - rand(n+1)` offsets, same number of tries.
* D: **noise settings**: the router's `initial_density_without_jaggedness` became `preliminary_surface_level` =
  `find_top_surface(initial - 0.390625, upper = min_y + height, lower = min_y, cell_height = size_vertical * 4)`, the same
  scan 1.21.1's NoiseChunk did (only difference: "nothing found" is `min_y` instead of Integer.MAX_VALUE).
* D: **Block render layers**: `render_type` is gone; models that were `translucent` got `force_translucent` textures (same
  look). `cutout`/`cutout_mipped` models are left to 26.1's texture-alpha detection: a cutout texture with half-transparent
  pixels now renders translucent (small look difference possible, check in the client).
* D: **Glowing faces** (glimtegel, knuffelbad glijgoot): NeoForge's `neoforge_data` block_light/sky_light 15 -> `light_emission` 15.
* D: **Spawn eggs** keep their 1.21.1 look: the two tint colours are baked into one texture per egg (1.21.1's
  template_spawn_egg textures multiplied like the old item tint).
* D: **Huge mushroom features** (reuze_guhpaddenstoel) need a ground predicate in 26.1: vanilla's
  `#minecraft:huge_red_mushroom_can_place_on` (= 1.21.1's dirt + mushroom_grow_block).
* D: **Villager trades** stay in code (A: ModVillagers + mixin/VillagerMixin, 2 random offers per level as in 1.21.1); no
  `trade_set`/`villager_trade` data is shipped.
* D: **GeckoLib 5** refuses animation files without animations in a dev environment: the five "empty" ones (sled, sled dog,
  surfboard, swim ring, rubber duck) got one empty looping animation `animation.guhs.leeg` (nothing plays it).
* D: FTB Quests SNBT needs no changes for FTB Quests 26.1.2.8 (same task/reward types, `ftbquests:custom_icon` +
  `ftbquests:icon` component still exist); verify the quest book in the dev client.
* S3: Guhvis-hengel "cast" look: the code-side `ItemProperties.register(.., "cast")` is gone; the client item definition (D) uses vanilla's
  `minecraft:fishing_rod/cast` condition (same rule: line out + rod in the fishing hand; NeoForge checks `FISHING_ROD_CAST`, our rod is a `FishingRodItem`).
* S3: Hula dance "stand still": 26.1 `ClientInput` has no settable impulses; while you dance the player gets a plain `ClientInput` (never moves,
  no jump/sneak/sprint) and its `KeyboardInput` back afterwards (checked every input tick). The dance keys themselves still come from `InputEvent.Key`.
* S3: Guhs asleep inside a huisje: 1.0.0 cancelled `RenderLivingEvent.Pre` (never fired for the GeckoLib guh renderer); now a NeoForge render state
  modifier on `GuhRenderer` makes them invisible + no name/shadow/fire/outline (the guh is also invisible on the server, as before).
* S3: `onRemove` is gone: speeltoestel (wip/schommel/glijbaan) and huisje clean up their parts/riders/registration in their block entity's
  `preRemoveSideEffects` (same triggers); the invisible parts (`SpeelDeelBlock`, `HuisjeDeelBlock`) and the tiki radiootje use
  `affectNeighborsAfterRemoval`, which only runs for block changes with neighbour updates (flag 1) or pistons (breaking, explosions: yes).
* S3: SavedData ids: `guhs:knus` (season offset, was `guhs_knus.dat`) and `guhs:huisjes` (was `guhs_huisjes.dat`); the 1.0.0 files are moved once by `GuhSavedData.get`.
* S3: Theehuis tea is drunk through a `Consumable` (`Consumables.defaultDrink()`: drink animation + generic drink sound, 1.6 s, no crumbs) instead of the
  removed `getDrinkingSound/getEatingSound` overrides; Mika-meerval / Gouden guhvis effects are `ApplyStatusEffectsConsumeEffect`s (chance 1, same effects).
* S3: Doolhof knabbel sparkles for the player (`sendParticles(player, .., longDistance, ..)`): 26.1 splits the old flag into overrideLimiter + alwaysShow; both set (1.21.1's "force").
* S3: Knabbelbal client glide (was `lerpTo` + `lerpTarget*`): an `InterpolationHandler` that only keeps the target, the ball glides there itself in 3 steps like before;
  zitje ignores server positions (empty handler); surf board: your own board ignores them, others jump there at once (as 1.21.1's plain `Entity#lerpTo`).
* S3: Huisje dome and surf waves are still drawn immediately (buffer source + `endBatch`) at `RenderLevelStageEvent.AfterTranslucentBlocks`
  (was `Stage.AFTER_TRANSLUCENT_BLOCKS`); camera from `getLevelRenderState().cameraRenderState.pos`, partial tick from `Minecraft#getDeltaTracker()`.
* S3: Huisje/wip/schommel extra models are NeoForge standalone models (`ModelEvent.RegisterStandalone`), drawn with `submitBlockModel` + cutout sheet
  (was `renderModel` with `RenderType.cutout()` and the crumbling overlay; now `NO_OVERLAY`, like R's guh wheel).
* S3: BlockItems registered with `registerItem(.., p -> new BlockItem(..), ..)` (huisjes, speelgoed, theetafel/theepotje, tiki blocks) get
  `useBlockDescriptionPrefix()` so their names stay `block.guhs.<id>` (26.1 BlockItems default to `item.guhs.<id>`). The huisje lore moved from
  `Block#appendHoverText` (gone) to `HuisjeFeature.HuisjeItem`.
* S3: `VariantUiterlijk.Uiterlijk#botten/extra` and `GuhRenderHooks.laag` run at extract time and hand bone moves / extra submits to the `GuhRenderFrame`
  (see 4.6b); story-variant textures have priority 10 (the Pinguh's 20 wins, as in 1.0.0 where PinguhRender was asked first).
* S1: 13 S1 spawn eggs (pluisegeltje, guh_konijntje, pluiseekhoorntje, shuckle, pieppiepmuisje, poepschilly, schilly, boze_kaasknabbel,
  boze_oppernabbel, pluisvinkje, kaasmeesje, guh_uiltje, zeemeeuwtje) are `ModItems.spawnEgg` eggs without tint colours (textures requested from D).
* S1: Piep/landdiertjes "waar is mijn guh" pocket check (`PiepDierItem#inventoryTick`) runs every 100 game ticks for every carried critter
  (was spread over the slots with `gameTime + slot`; 26.1 has no slot index), like A's `PickedUpGuhItem`.
* S1: Item "crumb" particles (`ItemParticleOption`) are made from the item only (26.1 takes an `Item`/`ItemStackTemplate`); stack components
  no longer change the particle look (none of the S1 items relied on that).
* S1: Voerhuisje: forgetting a removed voerhuisje moved from `onRemove` to `affectNeighborsAfterRemoval` (only on removals with neighbour
  updates; the list is only a spawn hint).
* S1: Leenhouweel (loaner pickaxe) is an `Item` with `pickaxe(IRON, 1, -2.8)` + `UNBREAKABLE`; "not enchantable" is now
  `isPrimaryItemFor`/`supportsEnchantment` = false (no table offers, no books on the anvil) instead of `isEnchantable`. Kaashouweel is an
  `Item` with its own `ToolMaterial` (same numbers as the old SimpleTier) and `repairable(kaasbrok)`.
* S1: Guh wiggle while Poepschilly is inside: the clock is copied at extract time (GeckoLib `CompileEntityRenderStateEvent`) and the pose is
  moved in `GeoEntityPreRenderEvent` (same place in the pose stack as 1.0.0's `GeoRenderEvent.Entity.Pre`).
* S1: Muisje/maatje on a player's shoulder: drawn from `RenderPlayerEvent.Post` by extracting the copy's render state and submitting it
  with the player's pose (no shadow, as before; its name tag follows the vanilla rules of the copy).
* S1: Pyjamas by the campfire (kamperen) and the klusje icon over a working guh (klusjes) are `GuhRenderer.hook`s now (`frame.pass` /
  `frame.layerExtra`) instead of `GuhRenderHooks.laag` layers; same bones, textures and pose.
* S1: Kaasknabbel nests: SavedData `guhs:piep_nesten` (1.0.0 file `guhs_piep_nesten.dat` moved once by `GuhSavedData.get`).
* S2: Vadswoud mist: 26.1 has no fog shapes, so the mist can no longer be cylindrical; it is set on the atmospheric (terrain)
  fog distances of `ViewportEvent.RenderFog` and measured like all 26.1 environmental fog (looking down from high up in a
  reuzenguhboom is a little hazier than in 1.0.0). Same distances (36..112 blocks), same fade in/out; the sky fog is untouched.
* S2: Herinnering (the star of a guh in the wolkjes) held in the main hand: its sparkles come from a client tick for every
  player the client sees (1.0.0: the item's client-side `inventoryTick`, which 26.1 no longer calls). Same chance and spot.
* S2: Guh-molentje hoppers: the item capability is NeoForge 26.1's transactional `ResourceHandler<ItemResource>` (was
  `IItemHandler`); same rules (graan in from the top/sides, never from below; only meel out at the bottom), aborted
  transfers are rolled back. The graan/meel still drop when it is broken (now from `MolentjeBlockEntity#preRemoveSideEffects`).
* S2: Knotwilg and vadshout leaves: 26.1 leaves drop falling-leaf particles by default; ours use chance 0 (as in 1.0.0).
  The vadshout leaves keep their own mint-leaf particle.
* S2: Knuffelhart: 1.0.0 passed a grey colour for the still sleeping heart, but its model has no tint index, so it never
  showed; the port simply draws the model (same look).
* S2: Knabbelkatapult notification sounds (countdown, fort done, run done) are sent as a `ClientboundSoundPacket` to the
  player (26.1 removed `ServerPlayer#playNotifySound`, which sent the same packet).
* S4: Kaasmelkdrankje (beroepen) clears **all** effects before giving Regeneration (NeoForge 26.1 has no effect cures;
  1.21.1 removed the milk-curable ones, which were practically all). Its drinking sound now comes from a `CONSUMABLE`
  component (`Consumables.defaultDrink()`, same sound and 1.6 s).
* S4: Bakkerij pastries: the small effect and the 0.8 s eating time of the fast ones come from the `CONSUMABLE` component
  (`ApplyStatusEffectsConsumeEffect`, chance 1 like before); nutrition/saturation unchanged.
* S4: Duikhelm is a plain `Item` with `humanoidArmor(DUIK, HELMET)` (2 armour, 220 durability, enchantability 12, turtle
  equip sound, repaired with a parel); its worn texture is the equipment asset `guhs:duikhelm` (D). Water breathing /
  night vision logic unchanged.
* S4: Mieuwguh spawn egg has no tint colours any more (was FAB2D2/4682DE; needs an item texture, D).
* S4: Sneeuwslee owner is synced as an entity reference (26.1 has no OPTIONAL_UUID serializer) - same UUID, no visible change.
  The Baltoslee sled route is synced with our own data serializer `guhs:baltoslee_compound_tag` (vanilla's COMPOUND_TAG is gone).
* S4: Sled teams (Baltoslee sled, sneeuwslee): dogs, Baltoguh and Steele-Mika are extracted with the sled and submitted
  after it (26.1 submit pipeline); they get the sled's light like before and no shadow of their own. The sledehondjes' ear/tail
  clock is the level's game time (GeckoLib 4 used its own render clock; same speed).
* S4: Sneeuwslee steering reads the movement keys directly (26.1's move vector is normalised; 1.21.1's impulses were plain
  +-1), so steering diagonally is as strong as before.
* S4: Server position updates of the sjoelschijf, the Baltoslee sled (ignored) and the sneeuwslee (own 10-step glide) go
  through a custom `InterpolationHandler` (26.1 replacement of `lerpTo`); same behaviour.
* S4: Bakkerij customer bubble: the pastry is drawn with the item model's particle sprite (was the baked model's particle
  icon; same texture).
* S4: Zielsguh heart next to the name uses `RenderNameTagEvent.CanRender` (26.1 split the event); only added when the guh
  has a name tag text at all.
* S4: Knabbeldief-Mika / bakkerij customers / Mieuwguh invulnerability is asked on the server only (`isInvulnerableTo(ServerLevel, ..)`,
  `hurtServer`); the client may show a hurt flash (cosmetic), like B's NPC note.
* S5: Band (`guhs_band.dat`) and Guhkamer (`guhs_guhkamer.dat`) saved data are `GuhSavedData.tagType("band"/"guhkamer", ..)`: the 1.0.0 file
  is moved to `<world>/dimensions/minecraft/overworld/data/guhs/band.dat` / `guhkamer.dat` the first time it is read (content unchanged).
* S5: Spawn eggs of the farm animals (guhschaapje, knabbelkippetje, guhkoe) and the waterdiertjes (guhxolotl, guh_eendje,
  knabbelvlindertje, glimguhtje, lieveheersbeestje) are plain `SpawnEggItem`s (`ModItems.spawnEgg`); their look comes from D's baked
  textures instead of the two tint colours.
* S5: Kaasmelk, kokosnoot and kokosmelk: sounds/animation/effect moved from item overrides and `FoodProperties.effect` to a `Consumable`
  (kaasmelk: drink, generic drink sound, bottle back via `usingConvertsTo`; kokosnoot: eaten with the drink slurp; kokosmelk: drink,
  honey sound, 1.6 s, Regeneration 5 s at 100 %). Same sounds, times and effect as 1.0.0; vanilla's consume particles/timing apply.
* S5: Guhxolotl "dry" check: `isInWaterRainOrBubble()` is gone -> `isInWaterOrRain()` (a bubble column is water, so it still counts).
* S5: Zwembandje steering reads the raw left/right keys (26.1 `ClientInput` has no `leftImpulse`; 1.0.0 was the same +-1, sneaking can't
  happen on the ring).
* S5: Emote "VAHOEG!" particle (4:1 wide): 26.1 particle quads are square, so it is extracted as four squares side by side with the same
  corners/uv; 26.1 culls particles by their centre point, so it disappears when its centre leaves the screen (1.0.0 used a widened box).
* S5: Knuffelbad shine (freshly washed guh): the energy-swirl model pass is a `GuhRenderer` hook pass on the bones the guh's own pass
  shows, worked out by bone name (saddle, armour tier, variant bones under clothes, like `GuhRenderer#applyVisibility`); bones that another
  feature's hook hides would still shimmer (1.0.0 re-rendered exactly the visible bones). R may add a "same bones as the main pass" pass.
* S5: Farm animals, guhxolotl and eendje renderers: baby size via `scaleModelForRender` (same 0.6 / 0.55 / 0.5), baby shadow halved like
  1.21.1's entity render dispatcher did, head turning via `DefaultAnimations.hardcodedHeadRotation` (was `DefaultedEntityGeoModel(id, true)`).
* S5: Badeendje: "picked up in my own game" hides it through `shouldRender` (was an early return in `render`); minimum block light 7 is set
  on the render state; the glow of the glowing kinds is the eyes render type (full bright) on the same bones.
* S5: Guhdex "Mijn guhs" guh preview (`band/client/GuhPop`) and the creche screen draw entities picture-in-picture (centred in their
  box; the old 8 % lower centre is kept through the translation).
* S5: Guhxolotl emmertje icon: item property `guhs:kleur` (ordinal / 4, model overrides) -> select property `guhs:guhxolotl_kleur`
  (roze/mint/choco/wit/goud) for D's client item definition.
* S5: Palm fronds (`PalmBlad`, a `LeavesBlock`): 26.1 leaves have a falling-leaf particle chance; set to 0 with no particle, as in 1.0.0.
* S6: Guhvleugels (guheinde) are a plain item with vanilla's elytra components (`GLIDER`, `EQUIPPABLE` chest with equipment asset
  `guhs:guhvleugels`, `repairable(mika_vet)`, durability 540, no damage on hurt); `ElytraItem`/`ElytraLayer` are gone, so the wings on your
  back are drawn by vanilla's `WingsLayer` from the equipment asset (D: `assets/guhs/equipment/guhvleugels.json` with a `wings` layer). Same
  look and gliding; like vanilla elytra they now also show on armour stands and other humanoids that wear them (1.0.0: players only).
* S6: Knabbelkroon is a plain item with `humanoidArmor(KROON_MATERIAL, HELMET)` (same numbers: 4 armour, 3 toughness, 0.1 knockback
  resistance, durability 11 x 45, enchantability 25, gold equip sound), repaired with the vahoege vads ingot; worn look = equipment asset
  `guhs:knabbelkroon` (D). The aura (saturation, hearts, Mika's) is unchanged (inventoryTick was already server-side logic).
* S6: Knabbelkristal is no longer an `EndCrystal` subclass (26.1's `EndCrystal#hurtServer` is final and always explodes): its own entity
  with the end crystal's spin/beam/show-bottom (same save keys `beam_target`/`ShowBottom`), drawn with vanilla 26.1's end crystal model.
  Smashing, the beam and the fight hooks are unchanged; vanilla code that looks for `EndCrystal` (dragon respawn, none in the Guheinde) no
  longer sees it.
* S6: Pluizenboom leaves (knuffeldal) extend 26.1's abstract `LeavesBlock` with falling-leaf chance 0 (1.0.0's leaves had no falling-leaf
  particles; the pluisjes still drift down from `animateTick`). Pluisgras/Guhpaddenstoel extend `VegetationBlock` (1.21.1's `BushBlock`).
* S6: Guh gieter (tuintjes): `setNoRepair()` is gone -> `isCombineRepairable` returns false (no grid repair), and without an
  `ENCHANTABLE` component it can't be enchanted (1.0.0 overrode `isEnchantable` to false): same behaviour.
* S6: Warme chocovet / snert (elftocht): the Speed II boost is an `ApplyStatusEffectsConsumeEffect` of the drink's `Consumable`
  (0.8 s drink = old `fast()`, honey-drink sound, no crumbs) instead of a food effect; stille knabbel (gatenkaas) likewise. Same result.
* S6: Skating (elftocht): the skates on your feet (`SchaatsLaag`) and the skater's lean/sway are prepared at render-state extraction
  (NeoForge `registerAvatarEntityModifier`) and applied in `RenderLivingEvent.Pre/Post` / the player layer; same numbers as 1.0.0.
  The skates are drawn under the legs of player renderers only (mannequins not), like 1.0.0.
* S6: Guheinde fight saved data is `guhs:guheinde` (the 1.0.0 file `guhs_guheinde.dat` of the Guheinde is moved once by `GuhSavedData`).
* S6: NPC animators (Schaatsmeester, Stempelguh, Kapper Krulletje, Burgemeester, Cocotje) moved to the 1.1.0 `(npc, tick) -> BoneMove`
  form; the Stempelguh's stamping arm now sets its rotation relative to the model's base rotation (GeckoLib 5 snapshots), which is the
  old "initial + plof" value.
* S7: Luchtballon lifts (`DATA_LIFT`) and the race guh's scripted ride (`DATA_RIT`) are synced as SNBT strings (`EntityDataSerializers.STRING`
  + `TagParser.parseCompoundFully`): 26.1 has no `COMPOUND_TAG` entity data serializer. Same data, no gameplay change. The golf ball's owner is
  synced as `OPTIONAL_LIVING_ENTITY_REFERENCE` (no `OPTIONAL_UUID` any more; same UUID).
* S7: Fog (Sneeuwstorm blizzard, kaasmoeras mist, Barbecuether Rookdelta/Asdal smoke, kaasfrituursaus): 26.1 fires one `ViewportEvent.RenderFog`
  (no FOG_TERRAIN/FOG_SKY mode, no fog shape, not cancellable); the same near/far values now go into the environmental fog. The old sphere
  (blizzard) / cylinder (mist) shape is gone and the sky fog is left alone -> the look may differ slightly (phase 6 check). In the sauce the
  fluid extension sets environmental/sky/cloud end like vanilla lava.
* S7: Barbecuether sky: `BarbecuetherSky` (DimensionSpecialEffects, SkyType.NONE, foggy everywhere) is deleted; the look is D's dimension type
  (`skybox: none`, nether-like `fog_start/end_distance` 10/96, fast_lava, water_evaporates, bed rule "explodes"). The kaasfrituursaus is fast
  (tick delay 10, slope 4, drop-off 1, faster push) where `minecraft:gameplay/fast_lava` is true (was `DimensionType#ultraWarm`).
* S7: Rookguh: 26.1 has no `FlyingMob`; `RookguhEntity extends Mob` with 1.21.1 FlyingMob's travel/no fall damage/no climbing copied in. The
  walk animation value is only computed on the client now (vanilla 26.1 aiStep).
* S7: Verkoolde mikakop: wearable through the item's `equippableUnswappable(HEAD)` (like 26.1 skulls; 1.21.1 `Equipable` on the block is gone).
  The wall variant gets its name through `overrideDescription("block.guhs.verkoolde_mikakop")`.
* S7: Guhbrouwketel: the leftover grillspiespoeder drop on removal moved to `GuhbrouwketelBlockEntity#preRemoveSideEffects` (no `onRemove`).
* S7: Sneeuwguhspar needles (`SparNaalden`) are a 26.1 `LeavesBlock` with falling-leaf chance 0 and no particle (1.21.1 plain leaves had none).
  Grill plants and moerasgras extend `VegetationBlock` (26.1's `BushBlock` is a new bonemealable bush). Saté/worst zwammetjes use the vanilla
  `SUPPORTS_CRIMSON_FUNGUS` / `SUPPORTS_WARPED_FUNGUS` tags (same blocks as 1.21.1's nylium/mycelium/soul soil/dirt rule).
* S7: Moerasheks-Mika, Vonk-Mika, Knekel-Mika and the Aangebrande Mika are `notInPeaceful()` types (1.21.1 Monsters despawned in peaceful); the
  Aangebrande Mika's own `checkDespawn` asks `getType().isAllowedInPeaceful()`.
* S7: Baltoguh sniffing pose and the Asguh's glowing cheeks are `GuhRenderer.hook`s (glow = `frame.glow`, eyes render type as before); the
  Baltoguh no longer goes through `VariantUiterlijk`. Nether-Mika's held ingot: a GeoRenderLayer added in `AddLayers` (per-bone item on "head").
* S7: Race ghost: colour as ARGB int (same values); its 1.0.0 `shadowRadius = 0.2f` was overwritten by GuhRenderer every frame, so it keeps the
  normal guh shadow (unchanged look).
* S7: Knabbelbaken beam: `BeaconRenderer.submitBeaconBeam` with the old fixed radii 0.2/0.25 (no 26.1 distance scaling), same colour/height.
* S7: Vadsverdrijvend drankje: the `effect` burst is a white `SpellParticleOption` (EFFECT takes a colour in 26.1; white = the 1.21.1 look); item
  particles use the stack's `ItemStackTemplate`.
* S7: Race ghosts SavedData: `guhs:race_geesten` via `GuhSavedData` (1.0.0 file `guhs_race_geesten.dat` is moved once).
* S7: Kaasmoeras test "rumbling tummy": FoodData has no exhaustion getter any more; the test reads `foodExhaustionLevel` from the player's save data.
* L: Riding: 26.1 `Entity#startRiding` refuses vehicles whose type is `noSave()`; `mixin/EntityRidingMixin` lets guhs entity types
  pass that check, so the race guh, kart, guh seat, sleds, surf board and zwembandje stay rideable and are still never saved (as 1.0.0).
* L: TemptGoal reads the new `minecraft:tempt_range` attribute; every guhs living entity gets it with the old fixed 10 blocks
  (`ModEntities#addTemptRange`, only where the entity's own attribute set has none).
* L: `Monster` peaceful despawn is per type now: `notInPeaceful()` on Mika and Nether-Mika (and the slice Mikas), as 1.0.0.
* L: Kaasfrituursaus sets you on fire again: 26.1 split lava's `lavaHurt` (damage only) and `lavaIgnite`; the sauce calls both.
* L: Structure templates: the DataFixer does not touch NBT of modded entities, so the generators now write `minecraft:scale` /
  `minecraft:max_health` (were `minecraft:generic.*`, which 26.1 ignores: giant hamster-house guhs, big Mikas etc. were normal size).
  33 .nbt regenerated (still DataVersion 3955; vanilla parts are fixed on load).
* L: NeoForge 26.1 loads every `data/*/loot_modifiers/*.json` itself; `data/neoforge/loot_modifiers/global_loot_modifiers.json` is gone
  (the file no longer parses). Guh fish in the Guhmension fishing loot unchanged.
* L: Katapult advancements/quests (`katapult_gevonden`, `katapult_drie_sterren`, `katapult_alle_sterren`) used the block ids
  `katapult_werper/knabbelkist/mika` as icons, which have no item - those advancements never loaded (also in 1.0.0). Icons are now
  `katapult_pluisballen`, `katapultster`, `katapult_mikaplank`; the advancements and the quests linked to them work.
* L: Reisguh at a new portal is put on the Reisguh list at once (was: on its first tick). In 26.1 a still-loading chunk's entities are
  invisible, so a second portal check (building + arriving) would otherwise place a second Reisguh.
* L: Barbecuether sate/worst sprouts: the "plants make room" check uses `VegetationBlock` (1.21.1 `BushBlock`; 26.1's BushBlock is only
  the new vanilla bush), so bone meal grows the giant skewer/sausage again.
* L: Guhvleugels equipment asset `guhs:guhvleugels` (wings layer) generated by `tools/mc26.py` (S6's request to D).
* L (gametests only): the registrar keeps the 1.0.0 test world: `GameTestInfoMixin` puts the test origin one block below the template
  (1.21.1 counted from the structure block), batch environments set `random_tick_speed 0` + no fire spread (1.21.1's test server rules)
  and start at day time 3000 (`data/guhs/function/gametest/batch_start.mcfunction`), `GuhMockPlayer` replaces
  `makeMockServerPlayerInLevel` (follows `setGameMode`, starts on the test origin instead of 0,0,0), `MockConnectionMixin` drops modded
  payloads to mock players (NeoForge 26.1 throws). 26.1 runs tests in id order, so a few tests that were disturbed by their new
  neighbours got their own batch (meppen searches stay inside the own test area).
* R (phase 6, visual check): Barbecuether fog: 1.0.0's "foggy everywhere" (isFoggyAt, start 5 % / end half of the render distance, max 192)
  is now applied in `BarbecuetherClient#fog` from the render distance (the dimension type's nether-like 10..96 alone was thinner at 8
  chunks); Rookdelta 40 / Asdal 64 on top as before. Same look as 1.0.0 (compared side by side).
* R (phase 6): glow textures (guh glows, Guhtwo floating glow, badeendjes, vogels) use `GuhRenderTypes.eyes` = 1.21.1's additive eyes
  (26.1's `RenderTypes.eyes` blends by alpha and drew black texels) -> same look as 1.0.0.
* R (phase 6): 3 block models (theetafel/feestbuffettafel gedekt, elftocht vuurkorf aan) got explicit uv for faces above y 16 (26.1 refuses
  automatic UVs outside the texture); the few texels on those faces come from the top row of the texture instead of outside the sprite.
* R (phase 6): Guhs creative tab skips stacks that a feature adds a second time (26.1 throws); the tab shows each item once like 1.0.0.
* R (phase 6): NOT 1:1 yet - FTB Quests 26.1 reads only JSON5 (see REQUESTS: A/D must switch the installer and generator); until then the
  quest book in 1.1.0 has no Guhs chapters.
* R (phase 6): GUI entity previews/items are lit a little brighter and hover tooltips follow 26.1's style (vanilla 26.1 GUI lighting); night
  sky of the Guhmension is a shade more purple near the zenith (26.1 sky gradient); both vanilla-side, not changed.

### 4.6c R phase 6 lessons (for anyone touching client code)
* `ViewportEvent.RenderFog#getType()` is `FogType.ATMOSPHERIC` in air; `NONE` never arrives (FogRenderer maps it). Check ATMOSPHERIC.
* `Screen#minecraft` is final and set in the constructor: `minecraft != null` no longer means "initialised". A screen that rebuilds on a
  data update before its first `init` must check `width > 0` (else the widgets of that early build stay next to the real ones).
* Renderers are constructed during the resource reload before item components are bound: never `new ItemStack(..)` in a renderer/layer
  constructor or field (NPE "Components not bound yet" -> the whole reload fails and the client hangs on a white window).
* `RenderTypes.eyes` is alpha-blended in 26.1; for 1.21.1-style additive glow use `GuhRenderTypes.eyes`.
* The integrated client handles packets before `Minecraft#execute` tasks of the same tick.
