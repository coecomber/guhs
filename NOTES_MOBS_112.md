# 1.1.2 part 2: no pile-up of wild Guhs mobs (changelog lines)

- Wild animals, critters and Mikas no longer pile up in the world (the same bug as the guhs in 1.1.1; on the official
  server 2,901 zeemeeuwtjes, 2,495 pluisvinkjes, 2,219 guh bees and 1,054 Mikas were loaded). The bird top-up around
  players only counted 64 blocks and every new flock was saved with its chunk, so birds kept being added around the
  places you play.
- A wild one that a spawner brings while you play (the natural spawner, a mob spawner block, the birds' top-up, a
  ladybird coming to your tuintje) now comes and goes: it isn't saved when its chunk unloads and it despawns when every
  player is more than 128 blocks away (fish: 64), like a vanilla monster or bat. New ones keep coming around you.
- The animals the world is made with stay, just like vanilla cows. Everything you keep stays exactly as before: tame,
  named, leashed, from a bucket, bred, placed by a structure, Big Mika and the other bosses, the muisjes in the buildings.
- The bird top-up also waits when there are 40 birds within 128 blocks.
- A tidy-up every 30 s (like the guh one): above 200 wild ones of a kind in a dimension (+100 per player there), the ones
  more than 64 blocks from every player go, farthest first. It only takes come-and-go animals, wild birds and wild Mikas/
  flutter critters (also the ones old worlds saved), never an animal of the world itself or one you keep.
- Kinds: pluisvinkje, kaasmeesje, guh-uiltje, zeemeeuwtje, guhxolotl, eendje, knabbelvlindertje, glimguhtje,
  lieveheersbeestje, egeltje, konijntje, eekhoorntje, Sjokkel, guh bee, guh slime, guh fish, kaasmot, kikkerguh, Mika,
  Nether-Mika, Mika-larfje, Moerasheks-Mika, Vonk-Mika, Knekel-Mika, Rookguh, Poepschilly, Schilly. (Guhs keep their
  1.1.1 rule; farm animals, the IJscoguh, Mieuwguh and game/quest characters are unchanged.)
- Gametests: WildeDierenGameTests (natural/spawner vs world/structure/bred spawns, keeping by name/leash/tame/bucket/
  persistence/boss, despawn distance, tidy-up).

Code: world/WildeDieren.java (tag guhs_kom_en_ga, FinalizeSpawnEvent, MobDespawnEvent, tidy-up), mixin/EntitySaveMixin.java
(Entity#shouldBeSaved false for a come-and-go animal), VogelSpawns (wide count + tag), WaterdiertjesEvents (lured ladybird).
