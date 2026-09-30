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

### 4.7 Mixins

| Mixin | 26.1.2 target |
|---|---|
| `FlowingFluidMixin` (`canSpreadTo`, gone) | `FlowingFluid#canMaybePassThrough(BlockGetter level, BlockPos sourcePos, BlockState sourceState, Direction direction, BlockPos testPos, BlockState testState, FluidState testFluidState)` (private, used by both downward `spread` and sideways `getSpread`) -> `@Inject(method = "canMaybePassThrough", at = @At("HEAD"), cancellable = true)` returning `false` |
| `ServerLevelMixin` (`tickPrecipitation(BlockPos)`) | still `public void tickPrecipitation(BlockPos pos)` in `ServerLevel` - unchanged |
| `client.LevelRendererMixin` (`renderSnowAndRain`, gone) | `WeatherEffectRenderer#getPrecipitationAt(Level, BlockPos)` (private, used by `extractRenderState` and `tickRainParticles`) or the NeoForge `custom_weather_effects` attribute (4.6) |
| `GameTestRegistryMixin` | deleted (filter lives in the registrar) |

### 4.8 Data / resources (owner D; code owners only need the ids)

* GeckoLib assets: `assets/guhs/geckolib/models/<entity|block|item>/*.geo.json`, `assets/guhs/geckolib/animations/...` (bare ids, see 4.6).
* Equipment assets for armour, client item definitions for every item, recipes with plain-string ingredients, `random_patch` removed,
  biome/dimension `attributes`, `villager_trade` registry - see PORT_PLAN 4.8.

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
