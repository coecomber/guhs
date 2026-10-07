# Guhs 1.2.0 - Dutch -> English glossary

This glossary is **binding** for every translator. If a word is in here, use exactly this English form,
in every key, quest, sign, book and dialogue. If a word is not in here, build it from the parts that are
(knabbel = nibble, vads = chonk, knuffel = snuggle, ...), and keep it consistent with what's already here.

---

## 1. STYLE

**Tone.** Cute, cozy, silly, kid-friendly. Same energy as the Dutch: short sentences, lots of
exclamations, guh-words sprinkled in. Never sarcastic or mean. Mikas are grumpy, never scary.

**How guhs talk.** Guhs are happy little chonks. They say *Guh!*, *Nyeg!* and *WAHOOG!* a lot, use
diminutives ("little", "-ie", "-y": *tummy, pawsies, ears*), and love words like *cozy, snuggly, chonky*.
Keep the guh-word where the Dutch has one; don't add three more. Mikas say *NYEG!* angrily and laugh
*Nyeh-heh-heh!*.

**Spelling.** American English: *color, favorite, behavior, cozy, gray, center, armor, theater,
neighbor, practice*. (Minecraft's own en_us also says *Armor*.) Ukulele, not ukelele.

**Capitalization.**
- Item, block, entity, biome, structure, effect and enchant names: **Title Case**
  (*Wahoog Chonk Pickaxe*, *Bucket of Cheese Sauce*, *Potted Cheese Flower*). Small words stay lowercase
  inside a name (*of, the, in, a, and, on*) unless first.
- Character names: as in the tables (*Mayor Chonkworth*, *Grandpa Guh*).
- In running text, generic nouns stay lowercase: *a guh, two cheese nibbles, your guhbelly*;
  proper places/characters stay capitalized: *Snuggledale, the Guh End, Overlord Mika*.
- Shouts stay in caps exactly where the Dutch shouts: *VAHOEG!* -> *WAHOOG!*, *NJEG!* -> *NYEG!*.

**Placeholders and codes - never touch.** Keep `%s`, `%1$s`, `%2$s`... (you may reorder positional ones
if English word order needs it, but every placeholder must still be there exactly once). Keep `§x`/`&x`
color codes, `&r`, `\\&` (escaped ampersand in FTB Quests), `\n`, `<3`, `♥`, `★`, `♪`, `✔`, `⚠`, arrows,
`{...}`, JSON escapes and `[links]` exactly as-is. Keep keybind letters (W/A/S/D, 1-8, A-K) as they are.

**Punctuation.** Use straight quotes `'` `"` like the Dutch source does. Ellipsis = three dots `...`.
Dashes: the source uses ` - ` (space hyphen space); keep that, no em dashes. Exclamation marks: match the
Dutch count (`!!!` stays `!!!`).

**Puns.** Translate the *joke*, not the words. If the Dutch puns on a Dutch idiom, swap in an English
idiom with the same feel (*"Boer zoekt Guh"* -> *"Farmer Wants a Guh"*). If there is no good English pun,
write something cute and guh-flavored instead; never leave a pun-shaped hole or a literal translation that
makes no sense. Pop-culture parodies keep their parody (Balto, Mewtwo, Lilo & Stitch, Elfstedentocht,
Sherlock, Bob the Builder, Finding Nemo seagulls, Saturday Night Fever...).

**Length.** Keep names short enough for the UI: an item name should not be much longer than the Dutch.
**Signs:** max ~15 characters per line (each sign line is its own key) - rephrase to fit, never let a
line overflow. Scoreboard/HUD/button strings: keep within ~120% of the Dutch length.

**Plurals & possessives.** *Mika* -> plural *Mikas* (no apostrophe; Dutch *Mika's* = plural).
Possessive *Mika's* only where it really is "of Mika" (*Mika's Biome*, *Mika's Grease*).
*guh* -> *guhs*. *Guhdex* has no plural.

**Diminutives.** Dutch *-tje/-je* (guhtje, babyguhtje, muisje) -> *little ...*, *-ie*, or *-ling*:
*guhtje* = "little guh" in prose, *guhling* in names, songs and titles. Don't stack them
("little guhling" is too much).

**Spawn eggs, buckets, borrowed items.** Follow vanilla English: *Guh Spawn Egg*, *Mika Spawn Egg*;
*Bucket of Cheese Sauce*, *Bucket of Guhfish*; "(geleend)" -> "(Borrowed)"; "X in pot" -> *Potted X*.
Stairs/slabs/walls/fences/gates/doors/trapdoors: *Chonkwood Stairs*, *Cheese Rind Brick Slab*, etc.

**Things that NEVER change:** guh, guhs, Guh! (sound), Guhdex, Guhmension (Dutch: Guhmensie), Mika (the
name; titles around it are translated), GeckoLib/mod names, Minecraft vanilla names (use the official
en_us name: Nether, End, Overworld, Ender Dragon, Sniffer...), Experiment 626, Ohana, Aloha.

---

## 2. Core guh words, slang & sounds

| Dutch | English | note |
|---|---|---|
| guh / guhs | guh / guhs | never translated |
| Guh! / Guh guh! / Guhhh... | Guh! / Guh guh! / Guhhh... | the guh sound, unchanged |
| guhtje | little guh / guhling | see Style: diminutives |
| babyguhtje / babyguh | baby guh / guhling | *Babyguhtjes terugbrengen* = *Bringing Back Baby Guhs* |
| njeg / Njeg! / NJEG! | nyeg / Nyeg! / NYEG! | happy guh word; ANGRY when a Mika says it |
| Njeg-njeg / njeh-heh-heh | Nyeg-nyeg / nyeh-heh-heh | Mika laugh = *Nyeh-heh-heh!* |
| vahoeg / VAHOEG! | wahoog / WAHOOG! | the happiest word ("wahoo" + guh) |
| vahoege (adj.) | wahoog | *vahoege vads* = *wahoog chonk*; never "wahooge" |
| supervahoeg | super wahoog | |
| vahoegste | most wahoog / wahoogest | pick what reads best; *Vahoegste van de Zeskamp* = *Wahoogest of the Hexathlon* |
| Vahoegheid | Wahoogness | *Drankje van Vahoegheid* = *Potion of Wahoogness* |
| onvahoeg (effect) | Unwahoog | |
| vads (noun/interj.) | chonk / Chonk! | "lazy and round"; *Vads!* = *Chonk!*; *Slaap lekker vads* = *Sleep chonky-tight* |
| vadsig | chonky | the best compliment for a guh |
| vadsige / vadsiger / vadsigst | chonky / chonkier / chonkiest | |
| vadsigheid | chonkiness | *Vadsigheid-scanner* = *Chonkiness Scanner* |
| vadsen / rondvadsen | to chonk / chonk around | menu *Rondvadsen: %s* = *Chonk around: %s* |
| gevadst / OPPER-GEVADST! | chonked / OVER-CHONKED! | Overlord Mika's defeat cry |
| vadsjes (lieve vadsjes) | chonkies | only in the heart-level phrases |
| lieve vadsige guhs | sweet chonky guhs | the mod's tagline |
| knabbel / knabbels | nibble / nibbles | everywhere, also in compounds |
| kaasknabbel(s) | cheese nibble(s) | generic, lowercase in prose |
| Kaas Knabbels (item) | Cheese Nibbles | the original item name |
| knabbelen | to nibble | |
| knuffel (hug) / knuffelen | cuddle / hug / to cuddle | the act |
| knuffel (plush toy) | plushie | *Guhknuffel* = *Guh Plushie* |
| Knuffel- (in place/proper names) | Snuggle- | *Knuffeldal* = *Snuggledale* |
| knus / Knus (tab, superkompas) | cozy / Cozy | |
| gezellig (effect Gezellig) | snug / Snug | |
| pluis / pluisje / pluizig | fluff / fluffy / fluffy | |
| zielsguh | soulguh | soulmate pun |
| guhwoord (geheim guhwoord) | guh word (secret guh word) | the secret word is NYEG (starts with an N, keep that!) |
| Blub / blub-njeg | Blub / blub-nyeg | water critter sound |
| Piep piep! | Squeak squeak! | |
| Mjam! / Nom nom | Nom! / Nom nom | |
| Hatsjoe! / Hatsjoe-njeg! | Achoo! / Achoo-nyeg! | |
| Gak! (Boris the goose) | Honk! | |
| Auuuhoe! | Awooo! | Baltoguh howl |
| PLOF! / Plons! | FLOMP! / Splash! | |
| Hup hup! | Hup hup! / Go go! | *Hup, Baltoguh!* = *Go, Baltoguh!* |
| Doei! / Doei-doei! | Bye-bye! | |
| Tingeling! | Ding-a-ling! | |
| Hihihi! | Hee hee hee! | |
| Snuf snuf / Snuffel! | Sniff sniff / Sniff! | |
| Oei! | Oopsie! | |
| Wiee! / WIEEEEE! | Wheee! / WHEEEEE! | |
| Brrr | Brrr | |
| Tokkel tokkel | Strum strum | ukulele |
| Kwaak / Kwak | Ribbit / Quack | frog / duck |
| Tjiep | Tweet | |
| Oehoe | Hoo-hoo | |
| Ssst / SSST | Shh / SHHH | |
| zieli / gewoon zieli | sadsie / just sadsie | Piep chapter catch-word for pitiful |
| beef / beef bijgelegd | beef / beef squashed | Shelly drama, keep "beef" |
| besties / bestie-moment | besties / bestie moment | |
| bff 5evr <3 | bff 5evr <3 | keep exactly |

## 3. Heart levels, friendship & guh menu

| Dutch | English | note |
|---|---|---|
| op weg naar lieve vadsjes... | on the way to sweet chonkies... | level 0 |
| lieve vadsjes van elkaar | sweet chonkies together | level 1 |
| mega lieve vadsjes van elkaar | mega sweet chonkies together | level 2 |
| zielsguh bff 5evr <3 | soulguh bff 5evr <3 | level 3 (max) |
| Lieve vadsjes (FTB chapter) | Sweet Chonkies | |
| hartjes | hearts | |
| guhmenu | guh menu | |
| Kledingkast | Wardrobe | |
| Kleertjes / Kleding | Clothes | *Kleertjes uit* = *Clothes Off* |
| pakje (minigame outfit set) | outfit | *het Mika-jagerpakje* = *the Mika Hunter outfit* |
| Rugzak & harnas | Backpack & Harness | |
| Aantrekken! | Dress Up! | |
| Dobbel! | Roll the Dice! | |
| Karakter | Personality | |
| Speels / Lui / Vadsig / Verlegen | Playful / Lazy / Chonky / Shy | |
| Dapper / Kletskous / Knuffelig / Nieuwsgierig | Brave / Chatterbox / Cuddly / Curious | |
| Gedrag: Passief (vluchten) / Passief / Neutraal / Agressief | Behavior: Passive (Flee) / Passive / Neutral / Aggressive | |
| Aanvalsstraal | Attack Radius | |
| Guhgeluidjes | Guh Noises | |
| Zitten / Opstaan | Sit / Stand Up | |
| Zwaartekracht | Gravity | |
| Dagboekje | Diary | |
| Wist-je-datjes | Did-You-Knows | |
| Eerste keren | Firsts | |
| Favorietjes / favorietje | Faves / fave | |
| Lievelings- (hapje/plek/knuffel/liedje/speeltje/emote/kleur/kunstje) | Favorite (Snack/Spot/Plushie/Song/Toy/Emote/Color/Trick) | |
| Beste guh-vriendje / vriendjes | Best Guh Buddy / buddies | |
| Klusjes | Chores | |
| Guhhuisje | Guh House | the guh-head house; *Villa Vahoeg* = *Villa Wahoog* |
| Bewoners | Residents | |
| Speeltje / speelgoed | Toy / toys | |
| Logeren / logeerguhs | Sleepover / sleepover guhs | |
| Guhkamer | Guh Room | guest room in your Guhbelly |
| Guhbel | Guh Bell | |
| Mijn guhs | My Guhs | Guhdex tab |
| Emotes / Lievelingsemote | Emotes / Favorite Emote | |
| Op slot | Locked | |
| Ontgrendeld | Unlocked | |
| Rechtsklik(ken) | Right-click | |
| aaien / aaitje / Je aait %s! | pet / a pet / You pet %s! | 1.2.0: a short tap on your own guh is only petting |
| Welke taal wil je voor Guhs? / Automatisch | Which language for Guhs? / Automatic | 1.2.0 first-join question; language button label *Lang: %s* |

## 4. Emotes

| Dutch | English | note |
|---|---|---|
| Zwaaien | Wave | |
| Dansen | Dance | |
| Slapen | Sleep | |
| VAHOEG-sprong | WAHOOG Jump | |
| Rollen | Roll | |
| Smakken | Munch | |
| Verlegen | Shy | |
| Gapen | Yawn | |
| Zingen | Sing | |
| Knuffelen | Cuddle | |
| Hartjes | Hearts | |
| Knuffeldansje | Snuggle Dance | |
| Bff-knuffel | BFF Hug | |
| Verdrietje | Sniffles | |
| Ukelele | Ukulele | 626-guh only |

## 5. Guhdex & UI terms

| Dutch | English | note |
|---|---|---|
| Guhdex | Guhdex | never translated |
| Zeldzaamheid | Rarity | |
| Gewoon / Ongewoon / Zeldzaam / Heel zeldzaam | Common / Uncommon / Rare / Very Rare | |
| Legendarisch / Uniek | Legendary / Unique | |
| Vaak / Zielig / Stiekem | Common / Pitiful / Sneaky | other rarity words |
| Getemd / Nog niet gezien | Tamed / Not seen yet | |
| Beloningen / Ophalen / Opgehaald | Rewards / Claim / Claimed | |
| Mijlpalen / Verzamelingen | Milestones / Collections | |
| Nog niet ontdekt | Not discovered yet | |
| Highscores / Scorebord / Serverrecord / Wereldrecord | High Scores / Scoreboard / Server Record / World Record | |
| makkelijk / medium / lastig / moeilijk | easy / medium / hard / hard | difficulty levels |
| snelste ronde | fastest lap | |
| Superkompas / Guhmensie-superkompas | Super Compass / Guhmension Super Compass | |
| Avontuur / Wonderen / Wonen / Ondergrond / Einde | Adventure / Wonders / Homes / Underground / End | Super Compass categories |
| Klassiekers | Classics | minigame group |
| De Grote Guhspelen | The Great Guh Games | minigame group |
| Knuffeldal-spelletjes | Snuggledale Games | |
| Guhverhalen / Verhalen | Guh Tales / Tales | stories tab |
| Eerdere avonturen | Earlier Adventures | |
| Bezocht / Nog niet bezocht | Visited / Not visited yet | |
| Kaasregen / Vadsparade / Sterrenregen | Cheese Shower / Chonk Parade / Star Shower | world events |
| Seizoenen: Lente/Zomer/Herfst/Winter | Seasons: Spring/Summer/Fall/Winter | American "Fall" |
| Ochtend / Dag / Middagdutje / Avond / Nacht | Morning / Day / Nap Time / Evening / Night | |
| Bovenwereld | Overworld | vanilla name |
| Maaginstellingen | Belly Settings | |
| Openbaar / Privé / Whitelist | Public / Private / Whitelist | |
| Guhs-instellingen / Clientinstellingen | Guhs Settings / Client Settings | |
| Taal: Automatisch / NL / EN | Language: Auto / NL / EN | the new switch |

## 6. Dimensions & biomes

| Dutch | English | note |
|---|---|---|
| Guhmensie | Guhmension | never translated |
| Guhmaag (dimension/biome) | Guhbelly | your pocket dimension inside a giant guh |
| guhmaag (your own) | guhbelly | lowercase in prose |
| Het Guheinde | The Guh End | endgame dimension |
| De Guhbarbecuether | The Guhbarbecuether | already an English pun, keep |
| Barbecuether | Barbecuether | |
| Guhvelden | Guh Fields | |
| Knabbelkruimels | Nibble Crumbs | |
| Roze pluisjes | Pink Puffs | |
| Kaasvlakte | Cheese Flats | |
| Guhweides | Guh Meadows | |
| Guhpieken | Guh Peaks | |
| Vadskliffen | Chonk Cliffs | |
| Mika's bioom | Mika's Biome | |
| Guhzee / Diepe Guhzee | Guh Sea / Deep Guh Sea | |
| Guhkristalmijn | Guh Crystal Mine | |
| Gatenkaasgrotten | Holey Cheese Caves | holey/holy pun |
| Kaasmoeras | Cheese Swamp | |
| Vadswoud | Chonkwood Forest | wood itself = Chonkwood |
| Houtskoolvlakte | Charcoal Flats | |
| Satébos | Satay Grove | |
| Worstenwoud | Sausage Woods | |
| Asdal | Ash Vale | |
| Rookdelta | Smoke Delta | |
| Knuffeldal | Snuggledale | |
| Guhpolder | Guh Polder | keep "polder" (Dutch-landscape nod for the Elfstedentocht parody) |
| Sneeuwguhtoendra | Snowguh Tundra | |
| Guhwai'i | Guhwai'i | Hawaii pun works in English |
| Nomguhpieken | Nomguh Peaks | |

## 7. Structures & places

| Dutch | English | note |
|---|---|---|
| Guhportaal / kaasknabbelportaal | Guh Portal / cheese nibble portal | |
| Guhgrotten / Uitdagende guhgrotten | Guh Caves / Challenging Guh Caves | |
| hamsterbuizen | hamster tubes | |
| Huis van Boze Mika | Evil Mika's House | |
| Vadsig-heiligdom | Chonky Shrine | Mother Chonky lives here |
| Mika-kamp | Mika Camp | |
| Guh-picknick | Guh Picnic | |
| Sleehut | Sled Hut | |
| Verstopguhhuis | Hide-and-Guh House | |
| Guhkermis / kermis | Guh Fair / fair | |
| Guhkasteel | Guh Castle | |
| Guhland | Guhland | theme park, keep |
| Guhramide | Guhramid | |
| Guhgypter (adv.) | Guhgyptian | "Walk like a Guhgyptian" |
| Guhstandbeeld | Guh Statue | |
| (Grote) Kaasfontein | (Grand) Cheese Fountain | |
| Guhdorp / guhdorpelingen | Guh Village / guh villagers | |
| Hamsterhuis / Groot hamsterhuis / Hamsterstad | Hamster House / Big Hamster House / Hamster Town | |
| Guh Beauty Theater | Guh Beauty Theater | |
| Guhracebaan | Guh Racetrack | |
| Mika-mephal | Whack-a-Mika Hall | |
| Guhdisco | Guh Disco | |
| Guhgolfbaan | Guh Mini Golf | |
| Vadsig eetfestijn | Chonky Food Fest | |
| Guhvisvijver | Guhfish Pond | |
| Zwevende guh-eilandjes | Floating Guh Islets | |
| Kaasmijn | Cheese Mine | |
| Guhbibliotheek / bieb | Guh Library / library | |
| Guhbubbel | The Guh Bubble | underwater dome |
| Knabbelkelder | Nibble Cellar | portal to the Guh End |
| Mika-vesting | Mika Fortress | |
| Terugpoort | Return Gate | |
| Stille Voorraadkelder | Silent Pantry | |
| Verlaten kaasmijnschacht | Old Cheese Mineshaft | (shortened: fits the Super Compass button) |
| Paalhut van de Moerasheks | Swamp Witch's Stilt Hut | |
| Boomhutdorp | Treehouse Village | |
| Barbecueput | Barbecue Pit | |
| Spiesburcht | Skewer Keep | |
| Mika-grillpaleis | Mika Grill Palace | |
| Knuffeldal(-stadje) | Snuggledale (town) | |
| Knabbelbakkerij | Nibble Bakery | |
| Knuffelcreche | Snuggle Daycare | |
| Knabbelthee-huisje / theehuis | Nibble Tea House / tea house | |
| Knip & Vads | Snip & Chonk | hair salon |
| Beroepenstraat | Career Street | |
| brandweerkazerne / ziekenhuisje | fire station / little hospital | |
| Guhboerderij | Guh Farm | |
| Guhtuintjes | Guh Gardens | |
| Guh-Sterrenwacht | Guh Observatory | |
| Ballonfestival | Balloon Festival | |
| Kampeerplekje(s) | Campsite(s) | |
| Knuffelbad | Snuggle Pool | |
| Het Sjoelhuisje | The Shuffle House | |
| Het Guhdoolhof | The Guh Maze | |
| De Knabbelkatapult | The Nibble Catapult | |
| De Knabbelspelen | The Nibble Games | |
| Kaasknabbel-nest | Cheese Nibble Nest | |
| De Elf-Guhjestocht | The Eleven Guhtowns Tour | Elfstedentocht nod |
| Het Guh-Circuit / Pitpaleis | The Guh Circuit / Pit Palace | |
| Nomguh | Nomguh | Nome + nom, keep |
| berghut | mountain hut | Balto story |
| Het kloon-eiland | Clone Island | |
| Het Hemelkapelletje | The Cloud Chapel | |
| Het paalhuisje van Lilo en Nani | Lilo and Nani's Stilt House | |
| guh-asiel | guh shelter | |
| De neergestorte capsule | The Crashed Capsule | |
| Het surfstrand van Guhwai'i | Guhwai'i Surf Beach | |

**Eleven Guhtowns** (Elfstedentocht villages; keep the Frisian-town parody, swap only Dutch slang parts):
Guhwarden -> **Guhwarden** · Snuh -> **Snuh** · IJlguh -> **Ijlguh** · Knabbelsloten -> **Nibblesloten** ·
Vadsvoren -> **Chonkvoren** · Guhdeloopen -> **Guhdeloopen** · Vadskum -> **Chonkum** ·
Knabbelsward -> **Nibblesward** · Guhlingen -> **Guhlingen** · Franeguh -> **Franeguh** · Dokguh -> **Dokguh**.
"It giet oan!" (Frisian) stays as-is: the famous Elfstedentocht phrase.

## 8. Characters & NPCs

| Dutch | English | note |
|---|---|---|
| Mika / Mika's | Mika / Mikas | name kept; plural no apostrophe |
| Grote Mika | Big Mika | |
| Boze Mika | Evil Mika | |
| Mika-baas | Mika Boss | |
| Opper-Mika | Overlord Mika | Guh End boss |
| Nether-Mika | Nether Mika | |
| Moerasheks-Mika | Swamp Witch Mika | |
| Kruimel-Mika | Crumb Mika | |
| Vonk-Mika | Spark Mika | |
| Knekel-Mika | Skelly Mika | |
| Aangebrande Mika | Burnt Mika | |
| Heg-Mika | Hedge Mika | |
| Steele-Mika | Steele Mika | Balto parody, keep "Steele" |
| Voorraadmika | Pantry Mika | |
| Mika-larfje | Mika Grub | |
| Knabbeldief | Nibble Thief | |
| Mika-pikker | Mika Snatcher | |
| Moeder Vadsig | Mother Chonky | |
| Guhbert | Guhbert | her baby, keep |
| Oerguh | Firstguh | the very first guh |
| Hongerige Guh | Hungry Guh | |
| Tandarts-guh | Dentist Guh | |
| Maagenzym-guh | Enzyme Guh | |
| Slee-guh | Sled Guh | |
| Kermis-guh | Fair Guh | |
| Verstopguhtje | Hidey Guh | |
| Tipguh | Hint Guh | |
| Reisguh | Travel Guh | |
| Poortwachter | Gatekeeper | |
| Koningguh / Zijne Vadsigheid | King Guh / His Chonkiness | |
| Showguh | Show Guh | |
| Juf Vadsma / Meneer Glitterguh / Oma Knabbel | Miss Chonksley / Mr. Glitterguh / Granny Nibbles | beauty jury |
| Raceguh | Race Guh | |
| Mepguh | Whack Guh | |
| DJ-guh | DJ Guh | |
| Golfguh | Golf Guh | |
| Smulguh | Munch Guh | |
| Visguh | Fishing Guh | |
| Mijnguh | Miner Guh | |
| Bibliothecaris | The Librarian | |
| Zeemeerguh | Merguh | NPC and variant |
| Boswachterguh | Ranger Guh | |
| Knabbelplukker | Nibble Picker | |
| Grillguh | Grill Guh | |
| Paradeguh / Tamboerguh | Parade Guh / Drummer Guh | |
| Burgemeester Vadsema | Mayor Chonkworth | |
| Knuffelburgemeester (title) | Snuggle Mayor | |
| Cocotje | Coco | always losing things |
| Opa Guh | Grandpa Guh | |
| Pluisje / Knabbeltje / Dikkie | Fluffy / Nibbles / Chubbs | Snuggledale neighbors |
| Mollie / Sproetje / Bolletje | Molly / Freckles / Dumpling | Snuggledale neighbors |
| Bakker Korstje | Baker Crusty | |
| Klantje / Feestklantje | Customer / Party Customer | |
| Juf Knuffel | Miss Snuggles | daycare |
| Mevrouw Theelepel | Mrs. Teaspoon | |
| Kapper Krulletje | Curly the Barber | |
| Boerin Hooibaal | Farmer Haybale | |
| Professor Sterretje | Professor Twinkle | |
| Kapitein Wolkje | Captain Cloudy | |
| Badmeester Bubbel | Lifeguard Bubbles | |
| IJscoguh Tingeling | Jingles the Ice Cream Guh | |
| Opoe Njegschuif | Granny Nyegshuffle | shuffleboard |
| Meneer Vadskronkel | Mr. Chonkwiggle | maze |
| Kapitein Floepguh | Captain Floopguh | catapult |
| Juf Vahoegsakee | Coach Wahoogaroo | Nibble Games host |
| Schaatsmeester Guhglij | Skate Master Guhglide | |
| Stempelguh | Stamp Guh | |
| Coach Vahoegvroem | Coach Wahoog-Vroom | |
| Brandweercommandant Blusguh | Fire Chief Splashguh | |
| Inspecteur Vahoegsma | Inspector Wahoogsby | |
| Dokter Snotneus-guh | Doctor Sniffleguh | |
| Bob de Guhbouwer | Bob the Guhbuilder | "Can we fix it?" nod |
| Klimguhtje / Snotje | Climby / Sniffles | career-street helpers |
| Brandweer-/Politie-/Apothekers-/Bouwguh | Firefighter / Police / Pharmacist / Builder Guh | learned careers |
| Timmerguh | Carpenter Guh | |
| Grillguhs geheime recept | Grill Guh's Secret Recipe | |
| Baltoguh | Baltoguh | |
| Boris (de gans) | Boris (the goose) | |
| Muk / Luk | Muk / Luk | |
| Rosy | Rosy | |
| Zuster Knuffel | Nurse Snuggles | |
| De witte wolf-guh | The White Wolf Guh | |
| Verteller | Narrator | |
| Held van Nomguh | Hero of Nomguh | title |
| Vriend van Guhtwo | Friend of Guhtwo | title (1.2.6) |
| Ohana-guh | Ohana Guh | title (1.2.6) |
| Wolkenvriend | Cloud Friend | title (1.2.6) |
| Huisjesbouwer | House Builder | title (1.2.6) |
| Opper-vadser | Over-Chonker | title (1.2.6) |
| Guhkenner | Guh Expert | title (1.2.6) |
| Titels / Geen titel | Titles / No title | Guhdex tab (1.2.6) |
| Guhtwo | Guhtwo | |
| Mieuwguh | Mewguh | |
| Professor Knabbelkloon | Professor Nibbleclone | |
| De wolkenhoeder | The Cloud Shepherd | |
| 626-guh | 626-guh | |
| Lilo-guh / Nani-guh | Lilo-guh / Nani-guh | |
| Tikiguh | Tiki Guh | |
| Vadstemmer | Chonk Tamer | villager profession |
| Guhkleermaker / kleermaker | Guh Tailor / tailor | villager profession |
| Vadssmid | Chonksmith | villager profession |
| Hamsterbouwer | Hamster Builder | villager profession |
| Mika-jager | Mika Hunter | villager profession |
| Knabbelboer | Nibble Farmer | villager profession |

## 9. Guh variants

| Dutch | English | note |
|---|---|---|
| Regenboogguh | Rainbow Guh | |
| Sterrenhemelguh | Starry Guh | |
| Spookguh | Ghost Guh | |
| Teckelguh | Dachshund Guh | |
| Mummieguh | Mummy Guh | |
| Muntguh | Mint Guh | |
| Chocoguh | Choco Guh | |
| Sneeuwguh | Snow Guh | |
| Truiguh | Sweater Guh | |
| Regenguh | Rain Guh | |
| Feestguh | Party Guh | |
| Chef-guh | Chef Guh | |
| Brontosaurusguh | Brontosaurus Guh | "Guh-saurus!" |
| Gouden Guh | Golden Guh | |
| brococolief | broccocutie | unique, lowercase on purpose |
| Enderguh | Enderguh | |
| Vahoege Enderguh / Hongerige Enderguh | Wahoog Enderguh / Hungry Enderguh | |
| Koningguh | King Guh | |
| Wolkguh | Cloud Guh | |
| Magere guh | Skinny Guh | |
| Kaasmoerasguh | Cheese Swamp Guh | |
| Asguh | Ash Guh | |
| Rookguh | Smoke Guh | |
| Pluisguh | Fluffguh | |
| Pinguh | Pinguh | |
| Glitterguh | Glitter Guh | |
| Kikkerguh | Frogguh | |
| Sterrenguh | Star Guh | |
| Sneeuwpopguh | Snowman Guh | the block you build |

## 10. Critters

| Dutch | English | note |
|---|---|---|
| diertjes / Diertjes van de Guhmensie | critters / Critters of the Guhmension | |
| Pieppiepmuisje(s) / muisje | Squeaksqueak Mouse (Mice) / mousie | |
| Schilly | Shelly | turtle, tiny head, big bestie |
| Poepschilly | Poopshelly | the butt-cleaner of the guh sea |
| Boze Kaasknabbel | Angry Cheese Nibble | |
| Boze Oppernabbel | Angry Overnibble | Opper -> Over |
| Roze Guh Koek | Pink Guh Cookie | |
| Guhvis | Guhfish | |
| Guhbij | Guh Bee | |
| Guhslijm | Guh Slime | |
| Kaasmot | Cheese Moth | |
| Vadswaker | Chonk Warden | Warden parody |
| Pluisvinkje | Fluff Finch | |
| Kaasmeesje | Cheese Chickadee | do NOT use "tit" |
| Guh-uiltje | Guh Owlet | |
| Zeemeeuwtje | Seagull | says "Mine! Mine!" (Nemo nod) |
| Guhxolotl | Guhxolotl | |
| Guh-eendje | Guh Duckling | |
| Knabbelvlindertje | Nibble Butterfly | |
| Glimguhtje | Glowguh | firefly |
| Lieveheersbeestje | Ladybug | |
| Pluisegeltje | Fluffhog | |
| Guh-konijntje | Guh Bunny | |
| Pluiseekhoorntje | Fluff Squirrel | |
| Sjokkel | Shuckly | Shuckle nod |
| Guhschaapje | Guh Lamb | |
| Knabbelkippetje | Nibble Chick | |
| Guhkoe | Guh Cow | |
| Babyguhtje (daycare) | Baby Guh | |
| Guh-sledehondje | Guh Sled Pup | |

## 11. Effects

| Dutch | English | note |
|---|---|---|
| Stil | Hushed | |
| Onvahoeg | Unwahoog | |
| Gezellig | Snug | |
| Uitgerust | Well-Rested | |
| Blosjes | Blushies | |
| Zweverig | Floaty | |
| Besties | Besties | |
| Fris van binnen | Fresh Inside | |
| Lief kijken | Puppy Eyes | |

## 12. Core items & blocks

| Dutch | English | note |
|---|---|---|
| Blok kaasknabbels | Block of Cheese Nibbles | |
| Kaasknabbelsteen / -diepsteen / -aarde / -keien | Cheese Nibble Stone / Deepslate / Dirt / Cobblestone | |
| Gefrituurde kaasknabbels | Fried Cheese Nibbles | |
| Mika's vet | Mika's Grease | used for frying |
| Mika-vetbal | Mika Grease Ball | |
| Guh-koekenpan | Guh Frying Pan | |
| Frituurvads | Fry Chonk | quest title |
| Kaassaus / Borrelende kaassaus | Cheese Sauce / Bubbling Cheese Sauce | |
| Vahoege vads | Wahoog Chonk | the pink ore material |
| Samengeperste super vahoege vads | Compressed Super Wahoog Chonk | |
| Vahoege-vadsstaaf | Wahoog Chonk Ingot | |
| Vahoege-vads(zwaard/houweel/bijl/schep/schoffel/paxel/schaar) | Wahoog Chonk (Sword/Pickaxe/Axe/Shovel/Hoe/Paxel/Shears) | |
| Vahoege-vads(helm/borstplaat/beenstukken/laarzen) | Wahoog Chonk (Helmet/Chestplate/Leggings/Boots) | |
| guhpantser (IJzeren/Diamanten/Netherieten) | Guh Armor (Iron/Diamond/Netherite) | |
| Bankguh / Bank Guh | Bank Guh | infinite storage; *het buikje* = *the tummy* |
| Guhrad / Guhdraad | Guh Wheel / Guh Wire | redstone |
| Guhspawner | Guh Spawner | |
| Knabbelbak | Nibble Trough | |
| Guhnaaitafel | Guh Sewing Table | |
| Vadsaambeeld | Chonk Anvil | |
| Buizenwerkbank | Tube Workbench | |
| Mikatrofee / Opper-Mikatrofee | Mika Trophy / Overlord Mika Trophy | |
| Mika-mepper | Mika Whacker | |
| Guhrugzak | Guh Backpack | |
| Guhgrotkompas / Uitdagingskompas / Heiligdomkompas | Guh Cave Compass / Challenge Compass / Shrine Compass | |
| Mika-spoorkompas | Mika Tracker Compass | |
| Guh-buikfluitje | Guh Belly Whistle | |
| Verloren guh-taart / Taartkruimels | Lost Guh Cake / Cake Crumbs | |
| Guh-ballon | Guh Balloon | |
| Guhkristal(-blok/-cluster/-lamp/-steen) | Guh Crystal (Block/Cluster/Lamp/Stone) | |
| Guh-kristalverrekijker | Guh Crystal Spyglass | |
| Guhslee / Sleerail / Sleeglijder | Guh Sled / Sled Rail / Sled Runner | |
| Vahoege sleebouwersboek | Wahoog Sled Builder's Book | |
| Vadsdrop / Guhkurkentrekker / Vahoegschans | Chonk Drop / Guh Corkscrew / Wahoog Ramp | sled rails |
| Maagwand / Maagbodem / Maagzuur / Maagportaal | Belly Wall / Belly Floor / Belly Acid / Belly Portal | |
| Tong / Tand / Verteerde kaasknabbels | Tongue / Tooth / Digested Cheese Nibbles | |
| Guhlelie | Guh Lily Pad | |
| Kaashoning / Knabbelkorf | Cheese Honey / Nibble Hive | |
| Vadszak / Guhkussen | Chonkbag / Guh Cushion | beanbag pun |
| Vadszetel / Vadskast | Chonk Couch / Chonk Cabinet | |
| Guhstoel / Guhtafel | Guh Chair / Guh Table | |
| Lampgion | Languhtern | lampion pun |
| Guhvlaggetjes | Guh Bunting | |
| Kaasbloem / Guhoortjes / Roze guhbloem / Knabbelroos | Cheese Flower / Guh Ears / Pink Guh Flower / Nibble Rose | |
| Guhbloesem(-stam/-boompje) / Vadsplanken | Guh Blossom (Log/Sapling) / Chonk Planks | |
| Guhtaart / Guhcupcake / guhcaron | Guh Cake / Guh Cupcake / Guhcaron | |
| Kaasfondguh | Cheese Fondguh | |
| Vahoege kaasknabbelshake | Wahoog Cheese Nibble Shake | |
| Kaasknabbelzaadjes / Kaasknabbelplant | Cheese Nibble Seeds / Cheese Nibble Plant | |
| Vadszaadbak | Chonk Seed Box | |
| Roze guhgras | Pink Guh Grass | |
| Eenrichtingsvadsglas | One-Way Chonk Glass | |
| Guhlock-pet / Vergroot-vadsglas | Guhlock Cap / Magnifying Chonk Glass | Sherlock nod |
| Koninklijke guhtroon / Gouden guhmedaillon | Royal Guh Throne / Golden Guh Medallion | |
| Wolkenlift / Wolkensuikerspin | Cloud Lift / Cloud Cotton Candy | |
| Kaasader (Diepe/Gouden/Uitgemijnde) | Cheese Vein (Deep/Golden/Mined-Out) | |
| Kaaskluis / Kaasbrok / Goudkaas | Cheese Vault / Cheese Chunk / Gold Cheese | |
| Gouden kaasknabbel | Golden Cheese Nibble | |
| Sterrenstof | Stardust | |
| Kaaskoraal / Parelmoer / Reuzenschelp / Parel | Cheese Coral / Mother-of-Pearl / Giant Clam / Pearl | |
| Kaaskorst(stenen) | Cheese Rind (Bricks) | |
| Mika-steen | Mika Stone | |
| Knabbelportaalframe / Knabbelsokkel / Knabbelslot / Knabbelpoort | Nibble Portal Frame / Nibble Pedestal / Nibble Lock / Nibble Gate | |
| Guheindeportaal | Guh End Portal | |
| Enderguh-ei | Enderguh Egg | |
| Mika-traan | Mika Tear | |
| Oog van Vadsig | Eye of Chonky | Eye of Ender nod |
| Knabbelkristal / Knabbelkroon | Nibble Crystal / Nibble Crown | |
| Guhvleugels | Guh Wings | |
| Gatenkaas(stenen) / Belegen kaas | Holey Cheese (Bricks) / Aged Cheese | |
| Kaasstalactiet / Gloeiend kaasmos | Cheese Stalactite / Glowing Cheese Moss | |
| Kaaskorrel / Kaaskorrelerts | Cheese Crumb / Cheese Crumb Ore | |
| Knabbelsensor / Knabbelschreeuwer / Stille knabbel | Nibble Sensor / Nibble Shrieker / Silent Nibble | sculk parodies |
| Kaasmodder / Kaasriet / Modderig kaasgras | Cheese Mud / Cheese Reeds / Muddy Cheese Grass | |
| Motknabbel / Moeraskaas | Moth Nibble / Swamp Cheese | |
| Vadsverdrijvend drankje | Chonk-Be-Gone Potion | |
| Vadshout(-stam/-planken/...) | Chonkwood (Log/Planks/...) | |
| Guhgezichtje in de schors | Guh Face in the Bark | |
| Vadsmos / Guhnestje / Vadstouw | Chonk Moss / Guh Nest / Chonk Rope | |
| Knabbelbessen(struik) / Knabbelbessentaartje | Nibbleberries (Bush) / Nibbleberry Tart | |
| Houtskoolsteen / Roosterijzer / Gloeikool | Charcoal Stone / Grill Iron / Glowcoal | |
| Satéstam / Worststam / Satévlees | Satay Stem / Sausage Stem / Satay Meat | |
| Pindasaus- / Mosterd-nylium | Peanut Sauce / Mustard Nylium | |
| Uienlicht / Rookgat / Smeulkooltjes | Onionlight / Smoke Vent / Smoldering Embers | |
| Verkoold guhbot / Verkoolde mikakop | Charred Guh Bone / Charred Mika Head | |
| Grillkool / Kaasfrituursaus / Aanmaakblokje | Grill Coal / Cheese Frying Sauce / Fire Starter Cube | |
| Kaasknabbelsaté / Guhbraadworst | Cheese Nibble Satay / Guh Bratwurst | |
| Guhbrouwketel / Knabbelbaken | Guh Brewing Cauldron / Nibble Beacon | |
| Grillspies / Gloeister / Gloeiend kooltje | Grill Skewer / Glowstar / Glowing Ember | |
| Rookloop- / Sluipknabbel- / Guhsprongdrankje | Smokewalk / Sneaky Nibble / Guh Jump Potion | |
| Knuffelgras / Pluisgras | Snuggle Grass / Fluff Grass | |
| Pluizenboom | Fluffy Tree | |
| Knuffelsteen / Pluisdak / Knuffelklinkers | Snugglestone / Fluff Thatch / Snuggle Cobbles | |
| Guhpaddenstoeltje | Guh Mushroom | |
| Feestbuffettafel / Knus-oorkonde | Party Buffet Table / Cozy Certificate | |
| Seizoensbloembak / Seizoensslinger / Bladerhoopje | Season Planter / Season Garland / Leaf Pile | |
| Knusfeestlijstje | Cozyfest List | |
| Knabbeloven | Nibble Oven | |
| Guh-wiegje / Speelkleed | Guh Cradle / Play Mat | |
| Guh-telescoop / Sterrenlantaarn / Wensster | Guh Telescope / Star Lantern / Wishing Star | |
| Guh-luchtballon / Mini-luchtballon | Guh Hot-Air Balloon / Mini Hot-Air Balloon | |
| Guh-slaapzak | Guh Sleeping Bag | |
| Guh-wastobbe / Guhshampoo / Guh-föhn | Guh Washtub / Guh Shampoo / Guh Hair Dryer | |
| Badeendje / Zwembandje | Rubber Ducky / Swim Ring | |
| Guh-xylofoon / Grijpmachine | Guh Xylophone / Claw Machine | |
| Liedjesboekje | Songbook | |
| Marshmallowknabbel | Marshmallow Nibble | |
| Kaasijsje | Cheese Ice Cream | |
| Pluiswol / Knabbelei / Kaasmelk / Knabbelvoer / Guhborstel | Fluff Wool / Nibble Egg / Cheese Milk / Nibble Feed / Guh Brush | |
| Guhgieter / Knabbelgraan / Knabbelmeel / Theekruid | Guh Watering Can / Nibble Grain / Nibble Flour / Tea Herb | |
| Guh-molentje | Guh Windmill | |
| Guhhuisje (klein/medium/groot) | Guh House (Small/Medium/Large) | |
| Knabbelbal / Guh-glijbaantje / Guh-wip / Guh-schommel / Pluizige tunnel | Nibble Ball / Guh Slide / Guh Seesaw / Guh Swing / Fluffy Tunnel | |
| Vadsigheid-scanner | Chonkiness Scanner | |
| Onberekenbaar vahoeg | Incalculably Wahoog | poster/painting |
| Kokosnoot / Kokosmelk / Guh-palm | Coconut / Coconut Milk / Guh Palm | |
| Guh-palm(stam/-planken/-trap/-plaat/-hek/-poort/-deur/-luik/-bord), Gestripte guh-palmstam | Guh Palm (Log/Planks/Stairs/Slab/Fence/Fence Gate/Door/Trapdoor/Sign), Stripped Guh Palm Log | the palm wood set (1.2.8) |
| Schilly-eitjes | Shelly Eggs | |
| Baltoguh-beeldje / Medicijnkist | Baltoguh Statuette / Medicine Chest | |
| Sneeuwguhspar | Snowguh Spruce | |
| Sneeuwslee / Sledebelletje | Snow Sled / Sleigh Bell | |
| Kloontank / Labnotitie / Tankonderdeel | Clone Tank / Lab Note / Tank Part | |
| Het Knuffelhart / Herinnering | The Snuggleheart / Memory | |
| Guhmuziekplaat | Guh Music Disc | |
| Wilde-guhweerder | Wild Guh Shoo-Sign | 1.2.0; "shoo-sign" in running text; its sign says *hier woont al een vadsje* = *a chonky lives here already* |

## 13. Food & bakery

| Dutch | English | note |
|---|---|---|
| Knabbelbroodje | Nibble Bun | |
| Kaaskrakeling | Cheese Pretzel | |
| Vadsvlaai | Chonk Pie | |
| Guhcroissant | Guhcroissant | |
| Knabbelkoekje | Nibble Cookie | |
| Kaasbolletje | Cheese Roll | |
| Pluismuffin | Fluff Muffin | |
| Theetaartje | Tea Tart | |
| Knabbeltompouce | Nibble Tompouce | Dutch pastry nod |
| Vadsdonut | Chonk Donut | |
| Guhwafel | Guh Waffle | |
| Sterrenkoekje | Star Cookie | |
| Feesttaart | Party Cake | |
| Knabbelthee / Kaasmelkthee / Theekruidthee / Guhbloementhee | Nibble Tea / Cheese Milk Tea / Herb Tea / Guh Flower Tea | |
| Warme chocovet | Hot Chocochonk | Elfstedentocht hot cocoa |
| Kommetje snert | Bowl of Pea Soup | |
| Poffertjes | Poffertjes (mini pancakes) | keep the Dutch nod |
| Vadsig gebakken guhvis | Chonky Fried Guhfish | |

## 14. Coins, tickets & currencies

| Dutch | English | note |
|---|---|---|
| munt / -munt | coin / Coin | |
| bon / -bon | ticket | *Kermisbon* = *Fair Ticket*; *Visbon* = *Fishing Ticket* |
| Boekenbon | Book Token | |
| Mepmunt / Discomunt / Smulmunt | Whack Coin / Disco Coin / Munch Coin | |
| Bakmunt / Speenmunt / Krulmunt | Bake Coin / Pacifier Coin / Curl Coin | |
| Ballonmunt / Eendjesmunt / Schelpjesmunt | Balloon Coin / Ducky Coin / Seashell Coin | |
| Showrozet / Raceprijsje | Show Rosette / Race Prize | |
| Katapultster / Spelenlintje / Elfstempel | Catapult Star / Games Ribbon / Town Stamp | |
| Sjoelschijfje / Circuitbeker / Sledesprintbeker | Shuffle Puck / Circuit Cup / Sled Sprint Cup | |
| Stempelkaart | Stamp Card | |
| Elf-Guhjeskruisje | Eleven Guhtowns Cross | the tour medal |

## 15. Minigames

| Dutch | English | note |
|---|---|---|
| Vads-wedstrijd | Chonk Pageant | at the Guh Beauty Theater |
| Miss Vadsig(-sjerp) | Miss Chonky (Sash) | |
| Guhrace / Huur-renguh / Recordgeest | Guh Race / Rental Racing Guh / Record Ghost | |
| Mika meppen | Whack-a-Mika | |
| Guhdisco songs: Vadsige Tango / Mika-Mambo / Njeg-Njeg Boogie | Chonky Tango / Mika Mambo / Nyeg-Nyeg Boogie | |
| Ze hangen aan me vet/veh | They Hang On My Chonk | also the jukebox song: *Guh - They Hang On My Chonk* |
| Guhkoorts op zaterdagavond | Saturday Night Guh Fever | |
| Guhgolf (9 holes) | Guh Golf (9 Holes) | |
| Vadsig eetfestijn | Chonky Food Fest | |
| Guhvis-wedstrijd | Guhfish Contest | |
| Kaasvis / Vadsbaars / Guhpuffer / Roze njegforel / Mika-meerval / Gouden Guhvis | Cheesefish / Chonky Bass / Guhpuffer / Pink Nyeg Trout / Mika Catfish / Golden Guhfish | |
| Verstopguh / verstoppertje | Hide-and-Guh / hide-and-seek | |
| Knabbels bakken | Nibble Baking | |
| Kappersshow | Salon Show | |
| Glijbanen: Roze Trechter / Glimtunnel / Grote Plons | Slides: Pink Funnel / Glow Tunnel / Big Splash | |
| Guh-sjoelen / sjoelbak | Guh Shuffleboard / shuffleboard | Dutch sjoelen nod |
| Guhdoolhof | Guh Maze | |
| Pluisballen / Mika-fort | Fluffballs / Mika Fort | catapult |
| De Grote Zeskamp | The Great Hexathlon | |
| Knabbelhappen | Nibble Bobbing | |
| Zaklopen | Sack Race | |
| Mika-blikgooien | Mika Can Toss | |
| Eierlopen met knabbelei | Egg-and-Spoon Race | |
| Spijkerpoepen | Nail Plopping | the Dutch party game, kid-cheeky |
| Guhguhtje prik | Pin the Tail on the Guh | |
| Elf-Guhjestocht | Eleven Guhtowns Tour | |
| Regenboogbaan / Vadsbaan / Kaasbergbaan | Rainbow Road / Chonk Track / Cheese Mountain Track | Guh Circuit |
| Vadslooping / Rolknabbel | Chonk Loop / Rolling Nibble | |
| Nomguh-sledesprint / medicijntocht | Nomguh Sled Sprint / medicine run | |
| Surfen / Hula / Lilo-guh's surfschool | Surfing / Hula / Lilo-guh's Surf School | |
| Knabbeldraai / njeg-grab / Guh-hop | Nibble Spin / nyeg-grab / Guh Hop | surf tricks |
| Aloha, Njeg / Guhla-Hula Rock / Vahoeg Hula Hop | Aloha, Nyeg / Guhla-Hula Rock / Wahoog Hula Hop | hula songs |
| Guhkwis | Guh Quiz | library |

## 16. Stories (Guh Tales) & questlines

| Dutch | English | note |
|---|---|---|
| De ontvoerde guh | The Guhnapping | Mother Chonky's story |
| De kapotte slee | The Broken Sled | |
| Het Grote Knusfeest / Knusfeest / seizoensfeest | The Great Cozyfest / Cozyfest / season party | |
| feesttaakjes | party chores | |
| Het Guheinde (story) | The Guh End | |
| De Grillguh helpt | The Grill Guh Helps | |
| Samen een huisje bouwen | Building a House Together | |
| Baltoguh en de medicijnkist | Baltoguh and the Medicine Chest | Balto parody |
| Het kloon-eiland | Clone Island | Mewtwo parody |
| Het Knuffelhart | The Snuggleheart | revive story |
| in de wolkjes | up in the clouds | where lost guhs go (gentle, never "dead") |
| Ohana op Guhwai'i | Ohana on Guhwai'i | Lilo & Stitch parody; "Ohana means family" |
| Beroep: brandweer / politie / apotheek / bouw | Career: Firefighter / Police / Pharmacy / Builder | |
| De Knabbeldief-zaak | The Case of the Nibble Thief | |
| De Grote Mika-oorlog | The Great Mika War | |
| steen-papier-schaar-VADS | rock-paper-scissors-CHONK | the VADS gesture = the CHONK gesture |
| Ridder van de Koningguh / van het Guheinde | Knight of the King Guh / of the Guh End | |
| De allereerste kaasknabbel / Het geheim van de kaasknabbel | The Very First Cheese Nibble / The Secret of the Cheese Nibble | |
| VAHOEG! Het guhwoordenboek | WAHOOG! The Guh Dictionary | library book |
| Het Geheime Guhboek | The Secret Guh Book | |

## 17. FTB Quests chapters

| Dutch | English | note |
|---|---|---|
| Guhs (chapter group) | Guhs | |
| Guhs & basis | Guhs & Basics | keep `\\&` escape |
| De Guhmensie | The Guhmension | |
| Minigames & bijzondere plekken | Minigames & Special Places | |
| Onderwater | Underwater | |
| De Guhmaag | The Guhbelly | |
| Het Guheinde | The Guh End | |
| De Guhbarbecuether | The Guhbarbecuether | |
| Grotten, moeras & woud | Caves, Swamp & Woods | |
| Knuffeldal | Snuggledale | |
| Piep! | Squeak! | |
| Lieve vadsjes van elkaar | Sweet Chonkies Together | |
| Guhverhalen | Guh Tales | |
| Diertjes van de Guhmensie | Critters of the Guhmension | |
| Hoe kom je hier? | How Do You Get Here? | first quest of every chapter |

## 18. Snuggledale collections & songs

| Dutch | English | note |
|---|---|---|
| Knuffelvriendjes / Seizoensplakboek | Snuggle Friends / Season Scrapbook | |
| Receptenboek / Theesoorten / Kapselcollectie | Recipe Book / Tea Collection / Hairstyle Collection | |
| Kapsel: Krullen/Kuifje/Knotjes/Strikjes/Pluisbol/Vlechtjes/Hanenkam/Matje | Hairstyle: Curls/Quiff/Buns/Bows/Puffball/Braids/Mohawk/Mullet | |
| Haarverf (roze/mint/citroen/lavendel/hemelsblauw/perzik/zilver/regenboog) | Hair Dye (Pink/Mint/Lemon/Lavender/Sky Blue/Peach/Silver/Rainbow) | |
| Tuinboek / Sterrenatlas / Ballonstempelkaart / Verhalenbundel | Garden Book / Star Atlas / Balloon Stamp Card / Story Collection | |
| Badeendjes / Knuffelkast / Liedjesboek / Kaasijsjes / Piepboek | Rubber Duckies / Plushie Shelf / Songbook / Cheese Ice Creams / Squeak Book | |
| De Grote Knabbel / De Kleine Vads | The Big Nibble / The Little Chonk | constellations (Big/Little Dipper nod) |
| De Vahoege Buik / De Vluchtende Mika / De Kaasschaaf | The Wahoog Belly / The Fleeing Mika / The Cheese Slicer | constellations |
| De VAHOEGE Toonladder | The WAHOOG Scale | |
| Vader Guh | Are You Sleeping, Brother Guh? | Frère Jacques |
| Altijd is Guhtje vads | Twinkle Twinkle Little Guh | same melody |
| Alle guhtjes zwemmen in de kaassaus | All the Guhlings Swim in Cheese Sauce | |
| In de knabbelmaneschijn | By the Light of the Nibble Moon | |
| Ode aan de Knabbel | Ode to the Nibble | Ode to Joy |
| Slaap, guhtje, slaap | Hush, Little Guhling | lullaby |
| Daar is het maantje / Knabbeltje onder je kussen / Wolkje, wolkje | There's the Little Moon / A Nibble Under Your Pillow / Little Cloud, Little Cloud | lullabies |

## 19. Outfits & clothing words

| Dutch | English | note |
|---|---|---|
| hoed / muts / pet / petje | hat / cap / cap / little cap | |
| jas / jasje / vest / trui / sjaal / strik | coat / jacket / vest / sweater / scarf / bow | |
| Guhzonnebril / Hartjesbril / Guhmonocle | Guh Sunglasses / Heart Glasses / Guh Monocle | |
| Sinterklaasmijter / Pietenmuts | Saint Nicholas Miter / Helper's Cap | |
| Koningsdagkroontje / Oranje shirt | King's Day Crown / Orange Shirt | Dutch holiday nod |
| Roze guhonesie / Guhkoksbuis / Guhzuidwester | Pink Guh Onesie / Guh Chef Jacket / Guh Sou'wester | |
| Jockeypetje / Mika-jagershoed / Mepvest met zakjes | Jockey Cap / Mika Hunter Hat / Whacking Vest with Pockets | |
| Glittervadspak / Vahoege afropruik | Glitter Chonk Suit / Wahoog Afro Wig | |
| Ruitjesvadstrui / Vadsige golfpet | Argyle Chonk Sweater / Chonky Golf Cap | |
| Mijnguhhelm / Boekenwurmvest / Geleerdenbaret | Miner Guh Helmet / Bookworm Vest / Scholar's Beret | |
| Hermelijnen koningsmantel / Vahoege guhkoningskroon | Ermine Royal Cape / Wahoog Guh King Crown | |
| Gouden aureooltje / Wolkenvleugeltjes | Golden Halo / Cloud Wings | |
| Rood sjaaltje van Baltoguh / Wolfsoortjes | Baltoguh's Red Scarf / Wolf Ears | |
| Guhtwo-staartje + nekbuisje / Trainerpetje | Guhtwo Tail + Neck Tube / Trainer Cap | |
| 626-oren met antennes / Hula-rokje / Bloemenkrans | 626 Ears with Antennae / Hula Skirt / Flower Lei | |
| Hartjesspeldje / Knuffeltruitje / Zielskroontje / Gouden hartjes-halsbandje | Heart Pin / Snuggle Sweater / Soulguh Crown / Golden Heart Collar | soulguh rewards |
| Oorstrikjes | Ear Bows | |

---

## 20. Quick word-builder (for compounds not listed)

| Dutch part | English part |
|---|---|
| guh- | Guh / guh- (*Guh Sled*, *Guhfish*: join only when it's a creature/proper name, otherwise two words) |
| knabbel- | Nibble |
| kaas- | Cheese |
| vads- / vadsig | Chonk / Chonky |
| vahoeg(e)- | Wahoog |
| knuffel- | Snuggle (places/names), Cuddle (actions), Plushie (toys) |
| knus- | Cozy |
| pluis- | Fluff / Fluffy |
| Mika- | Mika |
| opper- | Over- / Overlord |
| maag- | Belly |
| wolk(en)- / wolkje | Cloud / little cloud |
| sterren- | Star |
| roze | pink |
| zielsguh- | soulguh |
| -guhtje | -guhling / little guh |

## 21. Added during review (cross-chunk names)

| Dutch | English | note |
|---|---|---|
| Smulschaal (geleend) | Munch Bowl (Borrowed) | "munch bowl" in running text |
| Feestslingers | Party Garlands | "party garlands" / "garlands" in running text |
| Kaasmelkdrankje | Cheese Milk Potion | |
| Guh-belletje | Guh Jingle Bell | not the same item as Guh Bell |
| Leenkledingkast | Loaner Wardrobe | |
| Bouwboekje van de Timmerguh | Carpenter Guh's Building Booklet | |
| Showster(-) | Showstar | clothing line, shop |
| Guh-kwismeester | Guh Quizmaster | |
| Knuffeldekentje | Snuggle Blankie | |
| Rompertje | Baby Romper | |
| Guhvoerbak | Guh Feeding Trough | |
| Routelantaarn | Trail Lantern | |
| Mieuwguh-ballonnetje | Little Mewguh Balloon | |
| Feestkapselset | Party Hairdo Kit | |
| De Vallende Knabbel | The Falling Nibble | constellation |
| Pluk (verhaal van Opa Guh) | Pip | |
| Plaatje (bakvorm) | Disc | bakery shape |
| TATUU (sirene) | Nee-naw | fire siren sound |
| Dames en heren-guhs | Ladies and Guhtlemen | |

## 22. The Palewood (1.2.8)

| Dutch | English | note |
|---|---|---|
| Bleekwoud | Palewood | the biome: "the Palewood" in running text |
| Bleekhout(-stam/-planken/-bord/...) | Pale Guhwood (Log/Planks/Sign/...) | |
| Slapend guhgezichtje in de schors | Sleepy Guh Face in the Bark | |
| Bleekmos / Bleekmostapijt / Bleek hangmos | Palewood Moss / Palewood Moss Carpet / Hanging Palewood Moss | not vanilla's "Pale Moss" |
| Krakend Guhhartje / Verzuurd Guhhartje | Creaking Guh Heart / Soured Guh Heart | verzuurd = soured; "zure hartjes" = sour hearts |
| Kraakguh / Kraak-Mika | Creakguh / Creak Mika | |
| Krak! / Krak... krak... | Creak! / Creak... creak... | |
| houten knuffel | wooden hug | |
| Kaashars / Blok kaashars | Cheese Resin / Block of Cheese Resin | |
| Harssteen / Harsstenen (trap/plaat/muur) / Gebeitelde harsstenen | Cheese Resin Brick / Cheese Resin Bricks (Brick Stairs/Slab/Wall) / Chiseled Cheese Resin Bricks | |
| Oogbloempje (dicht/open) | Guh Eyeblossom (Closed/Open) | "guh eyeblossom" in running text |
| Bleke open plek / De Bleke Open Plek | Pale Clearing / The Pale Clearing | |
| Houthakkershutje / Houthakkerguh | Woodcutter's Hut / Woodcutter Guh | |

## 22b. Inside the Guh House

| Dutch | English | Note |
|---|---|---|
| Naar binnen | Go inside | the button in the Guh House screen |
| Kijk eens binnen | Have a Look Inside | the quest and the advancement |
| guhbedje / bedje | Guh Bed / little bed | one per resident |
| logeerbedje | guest bed | the pink bed the player sleeps in |
| prikbord | notice board | who does which chore |
| instoppen | tuck in | once per guh per night |
| briefje | note | on the bed of a guh that is away |
| deurmat | doormat | |
| snoetdeurtje | snoot door | as in the other Guh House texts |
| het baasje | my human | on a guh's note |
| Huisjesraampje | Little House Window | the painted window |

## 23. Guhpixel (lobby, minigames, things for home, Reisbureau, Guh-parkour)

### kern

| Dutch | English | note |
|---|---|---|
| Guhpixel | Guhpixel | never translated |
| Guhpixel-muntjes / muntje / muntjes | Guhpixel Coins / coin / coins | not an item |
| Guh-internetcafé "De Trage Verbinding" | Guh Internet Café "The Slow Connection" | |
| Beheerder-guh / Welkomstguh / Verkoper-guh | Admin Guh / Welcome Guh / Shopkeeper Guh | |
| Netwerkkabeltje / Guhpixel-poort / Guhpixel-portaal | Network Cable / Guhpixel Gate / Guhpixel Portal | |
| Guhpixel-winkel | Guhpixel Shop | |
| [GUH] [VADS] [VADS+] [MVG] [MVG++] | [GUH] [CHONK] [CHONK+] [MVG] [MVG++] | the rank prefixes |
| Guhpixel & uitjes (Guhdex tab) | Guhpixel & Outings | |
| grapspelletjes | joke games | |
| aandenken | keepsake | |
| Skyblok / Bedwars / Vadsnite / Vadsbus | Skyblok / Bedwars / Chonknite / Chonk Bus | |
| Guhmon-gevecht / Gymleider Dutjes | Guhmon Battle / Gym Leader Naps | |
| Boer zoekt Guh / Presentatrice Guhvon / Boer Guhrrit | Farmer Wants a Guh / Host Guhvon / Farmer Guhrrit | |
| Among Guhs / De Vadsvaarder / Kapitein-guh / droomguh | Among Guhs / The Chonkfarer / Captain Guh / dream guh | |
| Guhkade / Flappy Guh / Mika-Pong | Guhcade / Flappy Guh / Mika Pong | |
| Guhkantoor / Prikklok / Bureautje / loonstrookje / kwartaalrapport | Guh Office / Time Clock / Little Desk / payslip / quarterly report | |
| Guhbioscoop / Bioscoopdoek / Bioscoopstoeltje / film | Guh Cinema / Cinema Screen / Cinema Seat / movie | |
| Reisbureau "De Vadsvakantie" / Reisagent-guh / Reisbalie / Reisstempel / reispas / ansichtkaart | Travel Agency "The Chonk Vacation" / Travel Agent Guh / Travel Desk / Travel Stamp / travel pass / postcard | |
| Vadsen bij huize Lingsesdijk 86 / Huize Lingsesdijk 86 | Chonking at Lingsesdijk 86 / The House at Lingsesdijk 86 | keep "Lingsesdijk 86" exactly |
| Guh-parkour / Startpaaltje / Finishpaaltje | Guh Parkour / Start Post / Finish Post | |

### lobby

| Dutch | English | note |
|---|---|---|
| Lobbyguh | Lobby Guh | the six little guhs with gamer names that "chat" |
| Slapende surfer | Sleeping Surfer | a guh asleep behind a computer in the café |
| Oude beige computer | Old Beige Computer | decoration block |
| Gouden lobbyknabbel / gouden knabbel | Golden Lobby Nibble / golden nibble | ten hidden in the lobby |
| Lobby-parkour | lobby parkour | for players; not the Guh Parkour for guhs |
| Parkour-startplaat / -tussenpunt / -finishplaat | Parkour Start Plate / Checkpoint / Finish Plate | |
| tussenpunt | checkpoint | |
| AFK-hoek | AFK Corner | "afk (slaap)" = "afk (sleeping)" |
| De winkel / Guhpixel-winkel | The Shop / Guhpixel Shop | |
| kraampje | stall | the game stalls in the lobby |
| Knabbelspeurder | Nibble Sleuth | title |
| Meest Vadsige Guh | Most Valuable Guh | what MVG stands for (Dutch: Meest Vadsige Guh); no title since 1.3.1 |
| Spelers online: n (en 47 guhs) | Players online: n (and 47 guhs) | |
| TERUG NAAR HUIS | BACK HOME | the exit door |
| BINNENKORT, njeg | COMING SOON, Nyeg | the empty stall |
| xX_Vadsje_Xx / Knabbel2009 / NjegMaster / SlaapKopGuh / GuhGamer_NL / KaasKoning77 | xX_Chonky_Xx / Nibble2009 / NyegMaster / SleepyHeadGuh / GuhGamer_NL / CheeseKing77 | gamer names of the lobby guhs |
| Heb je hem al uit en weer aan gezet, njeg? | Have you tried turning it off and on again, Nyeg? | the Admin Guh |
| daklopers | roof runners | |

### grap1

| Dutch | English | note |
|---|---|---|
| Skyblok-guh / Bedwars-guh / Vadsnite-guh | Skyblok Guh / Bedwars Guh / Chonknite Guh | the three game guhs in the lobby |
| spelguh | game guh | a lobby NPC that starts a game |
| grapspelletje / Grapspelletjes (FTB section) | joke game / Joke Games | |
| hét eiland | THE island | Skyblok; the Dutch stress accent becomes capitals |
| handleiding (Stap 1 van 4.812) | manual (Step 1 of 4,812) | English thousands separator is a comma: 4,812 and 2,406 |
| cobblestone generator | cobblestone generator | kept in both languages (the players' own word) |
| SKYBLOK UITGESPEELD! | SKYBLOK COMPLETED! | |
| aftiteling / Regie / Met dank aan | credits / Director / Special thanks to | |
| decorguhs | set guhs | the guhs who painted the sky |
| ALLEEN PERSONEEL / ZON (40 watt) | STAFF ONLY / SUN (40 watts) | floating labels in the Skyblok box |
| Eilandje-in-een-fles | Island in a Bottle | Skyblok keepsake (decoration block) |
| Team Rood / Blauw / Groen / Geel | Team Red / Blue / Green / Yellow | |
| Teamguh | Team Guh | the stand-in entity |
| Teamslaapmuts Rood (...) | Team Nightcap Red (...) | Bedwars keepsake; slaapmuts = nightcap |
| VERDEDIG JE BED! / BED VERDEDIGD! | DEFEND YOUR BED! / BED DEFENDED! | |
| bedden vernield / dutjes | beds destroyed / naps | end screen |
| bruggen (met wol) | to bridge (with wool) | |
| KUSSEN-GENERATOR | PILLOW GENERATOR | |
| Sloopwars | Breakwars | "Dit is Bedwars, geen Sloopwars" = "This is Bedwars, not Breakwars" |
| luik | hatch | in the floor of the Chonk Bus |
| Nog wakker: %s | Still awake: %s | the counter |
| slaapwolk | sleep cloud | the pink "storm" of Chonknite |
| #1 VADSOVERWINNING / Vadsoverwinning | #1 CHONK ROYALE / Chonk Royale | parody of "#1 Victory Royale" |
| GELIJKSPEL / Iedereen slaapt. Gelijkspel, njeg. | DRAW / Everyone is asleep. It's a draw, Nyeg. | |
| Parachuterugzakje | Parachute Pack | Chonknite keepsake (outfit, back) |
| slaapplekje | sleeping spot | the beds on the Chonknite island |
| Aandenken van Bedwars (Guhpixel) | Keepsake from Bedwars (Guhpixel) | clothing source |

### grap2

| Dutch | English | note |
|---|---|---|
| Guhmon-gevecht / Guhmon | Guhmon Battle / Guhmon | Cobblemon parody; "Guhmon" never translated |
| Gymleider Dutjes / de Dutjesgym | Gym Leader Naps / the Nap Gym | core glossary |
| Snurkel | Snorkel | the gym leader's guh |
| Leenguh | Loaner Guh | the gym's own guh for players without tamed guhs |
| SLAAP (balk) | SLEEP (bar) | instead of HP; caps as in the Dutch |
| Vadsen / Njeg / Knabbel eten / Dutje (zetten) | Chonk / Nyeg / Eat Nibble / Nap (moves) | Title Case, like move names |
| "Het is super vadsig!" / "Het is niet erg effectief..." | "It's super chonky!" / "It's not very effective..." | battle lines |
| vol buikje | full tummy | the after-effect of Eat Nibble |
| Badgedoosje / gymbadge | Badge Case / gym badge | |
| Dutjesbadge / Njegbadge / Knabbelbadge | Nap Badge / Nyeg Badge / Nibble Badge | |
| Guhmon-trainerspet | Guhmon Trainer Cap | outfit; not the Mewtwo "Trainerpetje" |
| "dicht wegens dutje" | "closed for a nap" | the other five gyms |
| Boer zoekt Guh | Farmer Wants a Guh | core glossary |
| Presentatrice Guhvon / Boer Guhrrit | Host Guhvon / Farmer Guhrrit | core glossary |
| Tukkie / Dommelien / Snurkbert | Snoozie / Dozeline / Snorebert | the three candidates |
| logeerweek | stay-over week | |
| instoppen / ingestopt | tuck in / tucked in | |
| slaapvrienden | nap friends | |
| aftiteling | credits | |
| Brievenbus / Ingelijste brief | Mailbox / Framed Letter | |
| Strohoed van Boer Guhrrit / Overall van Boer Guhrrit | Farmer Guhrrit's Straw Hat / Farmer Guhrrit's Overalls | outfit; not the old Straw Hat / Overalls |
| kijkbuisguhtjes | dear viewers | Guhvon's greeting |
| Guhpixel TV | Guhpixel TV | |

### among

| Dutch | English | note |
|---|---|---|
| Among Guhs / De Vadsvaarder | Among Guhs / The Chonkfarer | the game and its ship (kern glossary) |
| Kapitein-guh / Logboek-guh | Captain Guh / Logbook Guh | the queue; the personal numbers |
| Ruimteguh / ruimtepakje | Space Guh / space suit | the guh NPCs of a round |
| crew / Mika / Mika's | crew / Mika / Mikas | roles; never "impostor" |
| in slaap duwen / slaper / droomguh | to push asleep / sleeper / dream guh | nobody is hurt: never "kill", "dead", "body" |
| Mika-kussentje / Saboteerkaart / Stembriefje | Mika Pillow / Sabotage Map / Voting Slip | the game items |
| Taakjes-paneel / Noodknop / Ventilatieluik (luik) | Task Panel / Emergency Button / Vent Hatch (vent) | the ship blocks |
| vergadering / uitspraak / stemmen / overslaan / wegstemmen | meeting / statement / to vote / to skip / to vote out | |
| wachtrij / klaar / leider | queue / ready / leader | |
| Normaal / Lastig | Normal / Hard | difficulty |
| Licht uit / Knabbelalarm / Deuren dicht | Lights Out / Nibble Alarm / Doors Shut | the three sabotages |
| de Slaapzaal / de Kantine / de Navigatie / de Reactor | the Dorm / the Cafeteria / Navigation / the Reactor | room names, with their article inside sentences |
| de Ziekenboeg / Elektra / de Machinekamer | the Sick Bay / Electrical / the Engine Room | |
| de Voorraadkamer / de Schildkamer / de gang | the Storage Room / the Shield Room / the corridor | |
| Worstjes knopen / Pasje door de lezer / Kruimelbak legen | Tying Sausages / Card Through the Reader / Emptying the Crumb Tray | task names |
| Pindasaus tanken / Knabbels sorteren / Dromen downloaden | Fuelling Peanut Sauce / Sorting Nibbles / Downloading Dreams | |
| Wegen ("Resultaat: vads") / Schakelaars goedzetten | Weighing ("Result: chonk") / Setting the Switches | |
| Rood, Blauw, Groen, Geel, Roze, Oranje, Paars, Wit, Bruin, Mint | Red, Blue, Green, Yellow, Pink, Orange, Purple, White, Brown, Mint | a guh NPC is called by its color |
| sus | sus | stays "sus" |
| tellen (10 tellen) | seconds | |
| Sus | Sus | title (the only Among Guhs title since 1.3.1) |
| Onterecht weggestemd | Wrongly Voted Out | a number on the stats board |
| oefenrondje | practice round | the one-time parody round at Captain Guh (joke game "among") |
| SUS-stickerbord | SUS Sticker Board | the keepsake of the practice round |
| (kleur) ruimtepakje / luchttank | (color) Space Suit / air tank | shop clothes: "Red Space Suit" |
| Plantjeshoedje / Eihoedje / Wc-rolhoedje | Little Plant Hat / Fried Egg Hat / Toilet Roll Hat | shop hats |
| Kaaspunthoedje / Sus-briefje / Knabbelhoedje | Cheese Wedge Hat / Sus Note / Nibble Hat | |
| hendel / streep / bak / schakelaar / lampje / briefje | lever / line / bin / switch / little lamp / note | words of the task panels |
| kaasknabbel / knabbel / kruimel (the three bins) | Cheese / Nibble / Crumb | short labels on the bins |
| noodvergadering / agendapunt / besluit | emergency meeting / (agenda) item / decision | the Emergency Button at home |
| gluren (uit het luik) | to peek out (of the vent) | the Vent Hatch at home |
| cijferbord / logboek | stats board / logbook | the Logbook Guh's screen |

### guhkade

| Dutch | English | note |
|---|---|---|
| Guhkade | Guhcade | "arcade" + guh |
| Guhkade-kast / speelkast | Guhcade Cabinet / arcade cabinet | block names: *Guhcade Cabinet: Flappy Guh*, *Guhcade Cabinet: Mika Pong* |
| Flappy Guh | Flappy Guh | never translated |
| Mika-Pong | Mika Pong | no hyphen in English; on the cabinet's screen *MIKA PONG* |
| kaasknabbelpilaren | cheese nibble pillars | |
| fladderen | flap | the one button of Flappy Guh |
| batje | paddle | |
| potje | game | *een potje doen* = *play a game* |
| top 5 van deze kast | this cabinet's top 5 | |
| plafond (van een guh) | ceiling | the best score a kind of guh can ever get |
| sip kijken | look glum | the beaten guh; never "cry" |
| extra oefenen | practice extra | |
| NJEG! / VAHOEG! / TIJD! (op het scherm) | NYEG! / WAHOOG! / TIME! | capitals only: the cabinet's pixel font knows A-Z, digits and - ! ? . : + |

### kantoor

| Dutch | English | note |
|---|---|---|
| Guhkantoor | Guh Office | |
| Guhkantoor-set | Guh Office Set | shop offer: a Time Clock and one Little Desk |
| Guhkantoor B.V. (Besloten Vadsschap) | Guh Office Ltd. (Limited Chonkability) | the company name on every paper |
| Prikklok | Time Clock | |
| Bureautje | Little Desk | *bureau* in the screen = *desk* |
| loonstrookje | payslip | |
| kwartaalrapport | quarterly report | *boekjaar* = *fiscal year* |
| Oorkonde Werknemer van de maand | Employee of the Month Certificate | *oorkonde* = *certificate* |
| Werknemer van de maand | Employee of the Month | Title Case, also in running text |
| inklokken / uitklokken | clock in / clock out | buttons: *Aan het werk* = *Put to Work*, *Naar huis* = *Go Home* |
| dienst | shift | 8 real hours |
| postvakje | pigeonhole | holds 3 payslips per guh |
| bruto / netto / ingehouden / toeslag | gross / net / withheld / bonus | payslip lines |
| de baas / de directie | the boss / management | |
| Vadstoeslag | Chonk bonus | |
| Vakbond Vads Verenigd | Union Chonks United | |

### bioscoop

| Dutch | English | note |
|---|---|---|
| Guhbioscoop | Guh Cinema | core glossary; "theater" for the room (American) |
| film | movie | American; "Skyblok: De Film" = "Skyblok: The Movie" |
| Guhbioscoop-projector | Guh Cinema Projector | block |
| Bioscoopdoek | Cinema Screen | block; "a piece of Cinema Screen" |
| Bioscoopstoeltje | Cinema Seat | block |
| Popcornmachine | Popcorn Machine | block |
| Bakje popcorn | Tub of Popcorn | item |
| Guhbioscoop-set | Guh Cinema Set | shop offer |
| Filmavond / Filmkenner | Movie Night / Movie Buff | FTB quests |
| UITGESPEELD! | COMPLETED! | on the screen in the Skyblok movie |
| #1 VADSOVERWINNING | #1 CHONK ROYALE | the Chonknite win banner |
| slaapwolk | sleep cloud | Chonknite |
| Slaap ze allemaal! | Gotta nap 'em all! | Guhmon tagline |
| Vadsen / Njeg / Knabbel eten / Dutje (Guhmon moves) | Chonk / Nyeg / Eat Nibble / Nap | "Guh used Chonk!" |
| Het is super vadsig! | It's super chonky! | |
| logeerweek | sleepover week | Farmer Wants a Guh |
| Noodvergadering | Emergency meeting | Among Guhs |
| Rood is sus | Red is sus | never translated further |
| ventilatieluik | vent | |
| EINDE | THE END | credits |

### reisbureau

| Dutch | English | note |
|---|---|---|
| Reisbureau "De Vadsvakantie" | Travel Agency "The Chonk Vacation" | structure; on signs: Travel / Agency: The / Chonk Vacation |
| Reisagent-guh | Travel Agent Guh | NPC |
| Reisbalie / balie | Travel Desk / desk | block; the counter in the structure is the same block |
| Reisstempel | Travel Stamp | item, used in the Travel Desk recipe |
| reispas / stempel | travel pass / stamp | per player, not an item |
| ansichtkaart | postcard | item names: *Postcard: <trip>* |
| baas (a guh about its owner) | boss | postcards: *Hi boss!* |
| Veel vadsjes van | Lots of chonks from | the postcard's closing line |
| 1 knabbel (postage stamp) | 1 nibble | |
| Proefreisje om de hoek | Trial Trip Around the Corner | the questline's five-minute trip |
| Gouden koffertje / Koffertje | Golden Suitcase / Little Suitcase | deco blocks |
| Vadsen bij huize Lingsesdijk 86 | Chonking at House Lingsesdijk 86 | the user's inside joke: "Lingsesdijk 86" is never translated |
| Schilderij "Huize Lingsesdijk 86" | Painting "House Lingsesdijk 86" | wall block |
| Dagje Kaasmarkt | Day at the Cheese Market | |
| Middagdutje in het Vadswoud | Afternoon Nap in Chonkwood Forest | |
| Kinderboerderij Knuffeldal | Snuggledale Petting Farm | |
| Strandmiddag op Guhwai'i | Beach Afternoon on Guhwai'i | |
| Wellness in de Barbecuether | Wellness in the Barbecuether | |
| Pretpark de Efteguh | Efteguh Theme Park | Efteling pun, keep "Efteguh" |
| Holle Bolle Guh | Hungry Bulgy Guh | "Papier hier, njeg!" = "Paper here, nyeg!" |
| Guhkenhof | Guhkenhof | Keukenhof pun, keep |
| Wintersport in Nomguh | Winter Sports in Nomguh | |
| Stedentrip Guhrijs / Guhrijs | City Trip to Guhris / Guhris | Paris pun |
| Eiffelknabbeltoren(tje) | (Little) Eiffel Nibble Tower | |
| Camping De Vadsige Tent | Camp Chonky Tent | |
| Guhnetië | Guhnice | Venice pun |
| Reis naar de Kaasmaan / Kaasmaan | Trip to the Cheese Moon / Cheese Moon | |
| Wereldreis in 80 dutjes | Around the World in 80 Naps | |
| Cruise over de Guhzee | Cruise on the Guh Sea | |
| Thuisblijfvakantie "Balkonië" / Balkonië | Staycation "Balconia" / Balconia | |
| Thuis is het ook vads | Home Is Chonk Too | painting title |
| glimguhtjes-lantaarn | Glowguh Lantern | |
| Op vakantie | On vacation | American spelling: vacation, not holiday |

### parkour

| Dutch | English | note |
|---|---|---|
| Guh-parkour / parkour | Guh Parkour / parkour | the course for guhs; not the lobby parkour for players |
| Startpaaltje / Finishpaaltje | Start Post / Finish Post | core glossary |
| Guh-horde | Guh Hurdle | |
| Guh-springplank | Guh Springboard | |
| Kruiptunnel | Crawl Tunnel | not the Fluffy Tunnel (Pluizige tunnel) |
| Slalompaaltjes | Slalom Poles | |
| Evenwichtsbalk | Balance Beam | |
| Knabbeltafeltje | Nibble Table | |
| Parkour-scorebord / scorebord | Parkour Scoreboard / scoreboard | |
| stuk (van een route) / hindernis | piece (of a route) / obstacle | |
| route uitzetten / stukken aanklikken | laying out a route / click pieces | |
| rondje / rondetijd / record | lap / lap time / record | |
| op de route zetten / van de route halen | put on the route / take off the route | |
| bobbel | bump | the guh inside the Crawl Tunnel |
| verplichte pitstop | mandatory pit stop | |
| Hup! / Boing! / Oeps! | Hup! / Boing! / Oops! | |
| vadsje | little chonk | |

## 24. einde2 (the outer islands of the Guheinde: biomes, creatures, the big places, the Sponsguh cast)

### kern

| Dutch | English | note |
|---|---|---|
| Gatenkaasheuvels | Holey Cheese Hills | biome |
| Korstwoestijn | Rind Desert | biome |
| Kruimelwolken | Crumb Clouds | biome |
| Schimmelkaasvlakte | Blue Cheese Plains | biome |
| Kristalkaasklippen | Crystal Cheese Cliffs | biome |
| Knabbelkoorbos | Nibble Chorus Forest | biome |
| buiteneilanden | outer islands | the islands of the Guh End far from the main island |
| kristalkaas | Crystal Cheese |  |
| knabbelkoor | Nibble Chorus | chorus plant parody |
| knabbelkoorvrucht | Nibble Chorus Fruit |  |
| gepofte | Popped | gepofte knabbelkoorvrucht = Popped Nibble Chorus Fruit |
| lichtstaafje | Light Rod | end rod parody |
| kruimelrots | Crumb Rock |  |
| kruimelzand | Crumb Sand |  |
| kruimelwolk | Crumb Cloud |  |
| schimmelkaas | Blue Cheese |  |
| klipkaas | Cliff Cheese |  |
| Korstguh | Rind Guh |  |
| Duinguh | Dune Guh |  |
| Kruimelguh | Crumb Guh |  |
| Schimmelguh | Blue Cheese Guh |  |
| Kristalguh | Crystal Guh |  |
| Koorguh | Chorus Guh |  |
| Ender-Mika | Ender Mika |  |
| Kaasmijt | Cheese Mite |  |
| Schuilguh | Hidey Guh |  |
| Schuildoos | Hidey Box |  |
| doosschil | Box Shell |  |
| Flitskruimel | Flash Crumb |  |
| hik | Hiccups | effect |
| Kruimelkwal | Crumb Jelly |  |
| Leegtewalvisguh | Void Whale Guh |  |
| vleermuis-lemurguh | Bat-Lemur Guh |  |
| otter-pinguïnguh | Otter-Penguin Guh |  |
| das-molguh | Badger-Mole Guh |  |
| schildpad-eendguh | Turtle-Duck Guh |  |
| Appa-guh | Appa Guh |  |
| Momo-guh | Momo Guh |  |
| Appa-fluitje | Appa Whistle |  |
| vetbootje | Little Grease Boat |  |
| vetschip | Grease Ship |  |
| heteluchtballon | Hot Air Balloon |  |
| Zuidelijke Luchttempel | Southern Air Temple |  |
| Noordelijke Waterstam | Northern Water Tribe |  |
| Ba Sing Guh | Ba Sing Guh |  |
| Vuurnatie-hoofdstad | Fire Nation Capital |  |
| Omashguh | Omashguh |  |
| Westelijke Luchttempel | Western Air Temple |  |
| Vuurtempel van Roku-guh | Fire Temple of Roku Guh |  |
| Kyoshi-dorp | Kyoshi Village |  |
| Bibliotheek van Wan Shi Guh | Wan Shi Guh's Library |  |
| Leeuwschildpad-eiland | Lion Turtle Island |  |
| Leegtehaven | Void Harbor |  |
| Guhvleugel-ringenrace | Guh Wing Ring Race |  |
| Guhvatar | Guhvatar |  |
| Mika-vesting | Mika Fortress | already in the glossary |
| Sponsguh Kaasbroek | Spongeguh Cheesepants | the Sponsguh story (proposals: the well-known English names of the parodied show, guh-ified the same way) |
| Patguh Ster | Patguh Star |  |
| Meneer Krabguh | Mr. Krabguh |  |
| Parel | Pearl |  |
| Sandy-guh | Sandy Guh |  |
| Gerrit | Gary | the snail |
| Octo Tentakel | Squidmika Tentacles | Octo-Mika = Squidmika |
| Octoliam | Squilliam |  |
| Plankmika | Plankmika |  |
| Karen | Karen |  |
| juf Pufguh | Mrs. Puffguh |  |
| koning Neptuguh | King Neptuguh |  |
| prinses Mindy-guh | Princess Mindy Guh |  |
| Dennis-Mika | Dennis Mika |  |
| de Cycloop | the Cyclops |  |
| David Hasselguh | David Hasselguh |  |
| Zeemeerman | Mermaid Guh |  |
| Mosselguh | Barnacle Guh |  |
| De Vliegende Guhlander | The Flying Guhchman |  |
| Bikinibodem | Bikini Bottom | structure |
| Schelpenstad | Shell City | structure |
| De Krokante Knabbel | The Krusty Nibble |  |
| De Vetemmer | The Grease Bucket |  |
| De Ruige Kroeg | The Thug Tug |  |
| de loopgraaf | the Trench |  |
| Bellenkom | Bubble Bowl |  |
| Kwallenvelden | Jelly Fields |  |
| Guhfy Guhber | Guhfy Guhber |  |
| Knabbelburger | Nibble Patty |  |
| Knabbelburgermobiel | Patty Wagon |  |
| kwallenjam | Jelly Jam |  |
| spooklantaarn | Ghost Lantern |  |
| bellenblaas | Bubble Blower |  |
| zeepbel | Soap Bubble |  |
| bubbelstroom | Bubble Stream |  |
| bloemwolk | Flower Cloud |  |
| leegtemist | void mist |  |
| besteltafel | Order Table |  |
| grill | Grill |  |
| vruchthuis | Fruit House |  |
| heldencape | Hero Cape |  |

### biomes

| Dutch | English | note |
|---|---|---|
| streek / de zes streken | region / The Six Regions | the six biomes of the outer islands; FTB section title |
| Korstgras | Rind Grass | korst = rind, as in Cheese Rind (kaaskorst) |
| Dorre korststruik | Dry Rind Bush | |
| Gebarsten korst | Cracked Rind | |
| Koorkorst | Chorus Rind | the purple ground of the Nibble Chorus Forest |
| Kruimelrots(stenen) | Crumb Rock (Bricks) | "Crumb Rock Brick Stairs / Slab / Wall" |
| Kruimelzand / Laagje kruimelzand | Crumb Sand / Crumb Sand Layer | |
| Kruimelwolk / Dichte kruimelwolk | Crumb Cloud / Dense Crumb Cloud | |
| Schimmelkaas(stenen) | Blue Cheese (Bricks) | |
| Schimmelpaddenstoel / Schimmelhoed / Schimmelsteel | Blue Cheese Mushroom / Blue Cheese Mushroom Cap / Blue Cheese Mushroom Stem | "schimmel" is never "mold" in a name: it is the blue of blue cheese |
| Klipkaas(stenen) | Cliff Cheese (Bricks) | |
| Kristalkaaserts / Kristalkaaskristal / Blok kristalkaas | Crystal Cheese Ore / Crystal Cheese Cluster / Block of Crystal Cheese | |
| Kristalkaasbrokje | Crystal Cheese Nugget | nine make one Crystal Cheese |
| Knabbelkoorplant / -bloem / -blok / -pilaar | Nibble Chorus Plant / Flower / Block / Pillar | "Nibble Chorus Stairs", "Nibble Chorus Slab" |
| Knabbelkoorvrucht / Gepofte knabbelkoorvrucht | Nibble Chorus Fruit / Popped Nibble Chorus Fruit | |
| Lichtstaafje | Light Rod | |
| Floep! | Floop! | the sound of the fruit's flash (as Captain Floopguh) |
| Zes smaken kaas | Six Flavors of Cheese | advancement |
| Kaas met een randje goud | Cheese with a Golden Edge | advancement, quest |

### biomeguhs

| Dutch | English | note |
|---|---|---|
| Guhs van de buiteneilanden | Guhs of the Outer Islands | FTB section |
| kaasgat | cheese hole | the round holes of the Holey Cheese Hills; a Rind Guh naps in one |
| korst (van de Korstguh) | rind | its shield of baked rind |
| koorkraagje | choir collar | the Chorus Guh's white collar (koor = choir and chorus) |
| blauwaderkaas | blue-veined cheese | the Blue Cheese Guh's fur |
| kaaskristal(len) | cheese crystal(s) | what grows on the Crystal Guh |
| kristalkaasbrokje | Crystal Cheese Nugget | item of the biomes slice: use its name there if it differs |
| Schimmelguh-gloed | Blue Cheese Guh Glow | the invisible light block `schimmelguh_licht` |
| Disco-dynamo | Disco Dynamo | bbq2's machine: use bbq2's English name if it differs |
| Vriendje van ver | A Friend From Afar | advancement |
| Balletje balletje | Roly-Poly | advancement |
| Ik zie, ik zie... | I Spy... | advancement |
| Het Guhkoor | The Guh Choir | advancement |
| Zes streken, zes vriendjes | Six Lands, Six Friends | advancement and quest |
| Kaas met een korstje | Cheese With a Rind | quest |
| Alleen de oortjes | Just the Ears | quest |
| Lichter dan een kruimel | Lighter Than a Crumb | quest |
| Een lampje op pootjes | A Lamp on Little Legs | quest |
| Een neus voor glimmers | A Nose for Shinies | quest |
| Eerste stem: njeg | First Voice: Nyeg | quest |

### mikas

| Dutch | English | note |
|---|---|---|
| Ender-Mika | Ender Mika | enderman parody; plural *Ender Mikas* |
| Kaasmijt | Cheese Mite | endermite parody; plural *Cheese Mites* |
| Schuilguh | Hidey Guh | shulker parody; plural *Hidey Guhs* |
| Schuildoos | Hidey Box | shulker box parody; plural *Hidey Boxes* |
| doosschil | Box Shell | shulker shell parody; lowercase in prose: *a box shell* |
| Flitskruimel | Flash Crumb | ender pearl parody (a material); lowercase in prose |
| kaasdoos / kaasdoosje | cheese box / little cheese box | the round wooden box a Hidey Guh lives in |
| flitsen / flitst weg | to flit / flits away | how an Ender Mika teleports; never "teleports" in flavor text |
| jatten / jat | to swipe / swipes | what the Ender Mika does with your nibble |
| hik / de hik | Hiccups / the hiccups | effect name *Hiccups*; the sound *Hik!* = *Hic!* |
| kusje | smooch | *een kusje van de Schuilguh* = *a smooch from the Hidey Guh* |
| kietelen / Kiele kiele! | to tickle / Tickle tickle! | |
| Toe! | Shut! | the Hidey Guh's lid snapping shut |
| Plop! / Smak! / Krak! | Pop! / Smooch! / Crack! | |
| buiteneilanden | outer islands | (shared, see kern.md) |

### zwevers

| Dutch | English | note |
|---|---|---|
| Kruimelkwal | Crumb Jelly | (in kern.md too) the jellyfish of cheese crumbs |
| kruimelkwalpot | Crumb Jelly Jar | block kruimelkwal_pot: a caught Crumb Jelly, a lamp |
| Leegtewalvisguh | Void Whale Guh | (in kern.md too) |
| Rug van de Leegtewalvisguh | Void Whale Guh's Back | entity leegtewalvisguh_rug: a technical part, players never read it |
| leegteparel | Void Pearl | the whale's thank-you |
| Vleermuis-lemurguh | Bat-Lemur Guh | lucht |
| Otter-pinguïnguh | Otter-Penguin Guh | water |
| Das-molguh | Badger-Mole Guh | aarde |
| Schildpad-eendguh | Turtle-Duck Guh | vuur |
| elementdieren | element animals | the four hybrid animals of the Guhvatar story |
| Zwevers & elementdieren | Floaters & Element Animals | FTB section |
| BOING | BOING | the bounce on a Crumb Jelly |
| kwak-njeg | quack-nyeg | the Turtle-Duck Guh's happy sound |
| luchttempels / de Waterstam / de paleisvijver | the air temples / the Water Tribe / the palace pond | the place names of kern.md |
| de oude kapitein (in de havenkroeg) | the old captain (in the harbor pub) | a character of the Void Harbor, a later phase |
| aardestuurders | earthbenders | the Guhvatar story's word for who steers earth |

### appa

| Dutch | English | note |
|---|---|---|
| Appa-guh | Appa Guh | the sky bison of the Guhvatar story; "he" |
| Momo-guh | Momo Guh | the winged lemur; "he" |
| Appa-fluitje | Appa Whistle | item |
| jip jip | yip yip | the take-off call, as in the parodied show |
| zadelmand | saddle basket | where the guests ride |
| zadelkist | saddle chest | the 27-slot chest on his back |
| luchtbizon | sky bison | |
| mensje | human | a player, as elsewhere in the mod |
| aangevadst (komen) | come chonking along | vads = chonk |
| lenen (een knabbel) | borrow (a nibble) | Momo never takes one for good |

### vetbootje

| Dutch | English | note |
|---|---|---|
| vetbootje | Little Grease Boat | the player's own small flying ship (contract 3.5); lowercase in running text: "your little grease boat" |
| Vetfluitje | Grease Whistle | calls the boat |
| Werfboekje | Shipyard Booklet | opens the customise screen |
| De Scheepswerf | The Shipyard | the screen now, the yard of the Void Harbor later |
| Leenbootje | Loaner Boat | a boat that belongs to nobody (the boating exam of phase B) |
| Ruim van %s | Hold of %s | the chest's title; a ship's hold |
| Heteluchtballon | Hot Air Balloon | contract 3.5 |
| Mika-snoet (zeil) | Mika Face | sail picture |
| Knabbelzak (zeil) | Nibble Bag | sail picture: the bag of Cheese Nibbles |
| Vetstrepen (zeil) | Grease Stripes | sail picture |
| Vahoeg-ster (zeil) | Wahoog Star | sail picture |
| Kruimels (zeil) | Crumbs | sail picture |
| Pootjes thuis, njeg! | Pawsies off, nyeg! | "hands off" |
| Glibber, glibber! | Slip, slide! | the boat gliding in |
| De Vette Knabbel | The Greasy Nibble | made-up boat name |
| Glibberguh | Slipperyguh | made-up boat name |
| Mika's Nachtmerrie | Mika's Nightmare | made-up boat name |
| De Dobberende Vadsbaal | The Bobbing Chonkball | made-up boat name |
| Frituurpiraat | Fryer Pirate | made-up boat name |
| Njegvaart | Nyeg Cruise | made-up boat name |
| SS Kaaskorst | SS Cheese Rind | made-up boat name (kaaskorst = Cheese Rind) |
| De Slome Guh | The Slowpoke Guh | made-up boat name |
| Vadsig door de Lucht | Chonky in the Sky | made-up boat name |
| Knabbelkruiser | Nibble Cruiser | made-up boat name |
| Het Vette Hapje | The Greasy Bite | made-up boat name |
| Kruimeldief | Crumb Thief | made-up boat name |

### vesting

| Dutch | English | note |
|---|---|---|
| Mika-vesting | Mika Fortress | already in the glossary; the modular one keeps the name |
| burcht | keep | the main tower of a fortress |
| wachtlokaal | guard room | ground floor of the keep |
| vesting-kapitein / de kapitein | fortress captain / the captain | character of a later phase; signs are signed "-the captain" |
| de spijtige Mika-kok / de kok | the Sorry Mika Cook / the cook | character of a later phase |
| cellenblok / "de logeervleugel" | cell block / "the guest wing" | the Mikas' own name for it is the joke |
| kerkertoren / "de logeerkamer" | dungeon tower / "the guest room" | |
| wachttoren / uitkijktoren | guard tower / lookout tower | |
| troonzaal / schatkamer | throne room / treasury | |
| knabbeldepot / buitkamer | nibble depot / loot room | |
| vetstokerij | grease works | |
| steiger / havenbord | pier / harbor board | American spelling |
| vetschip / de sloep / de vetblaas | grease ship / the sloop / the grease bubble | the three ships |
| kajuit | cabin | |
| vestingguh | fortress guh | a skinny guh freed from a fortress cell |
| magere guh | skinny guh | as in the glossary |
| duwpop / duwoefening | shove dummy / shove drill | Mikas only shove |
| knabbelkar | nibble cart | |
| knabbelsoep (zonder knabbel) | nibble soup (minus nibble) | |

### deco

| Dutch | English | note |
|---|---|---|
| luchtbizon | sky bison | Appa's kind; "Luchtbizon-stal" = Sky Bison Stable |
| meditatiepaal | Meditation Pole |  |
| luchtbal | airball | the air nomads' game; "Luchtbal-veld" = Airball Court |
| Guhatso | Guhatso | monk Gyatso as a guh (the name DESIGN uses) |
| geestenoase | Spirit Oasis |  |
| Zhao-Mika | Zhao Mika | the admiral; proper nouns with -Mika / -guh lose the hyphen |
| ijsvissen | ice fishing |  |
| Waterstam | Water Tribe | as in Noordelijke Waterstam = Northern Water Tribe |
| kaasbol | cheese ball | the ball of ice and cheese the old Guhvatar slept in ("De jongen in de kaasbol" = The Boy in the Cheese Ball) |
| das-molguh-hol / -grot | Badger-Mole Den / Badger-Mole Guh Burrow | the sign is short of room |
| Aardrommel | Earth Rumble |  |
| De Blinde Bandiet | The Blind Bandit |  |
| De Kei-guh | The Boulder Guh |  |
| koolkraam | cabbage cart |  |
| MIJN KOLEN! | MY CABBAGES! | the cabbage merchant's cry, always in capitals |
| kool | Cabbage | the block `einde2_deco_kool` |
| geplet | squashed |  |
| Geheime tunnel! Door de bergen! | Secret tunnel! Through the mountains! | the nomads' song; on the sign "Thru mountains!" (16 characters) |
| Sintelstrand | Ember Beach | Ember Island's beach |
| Vuurheer | Fire Lord |  |
| Zonnekrijger | Sun Warrior |  |
| eeuwige vlam | Eternal Flame | the block `einde2_deco_eeuwige_vlam` |
| draakguh | Dragon Guh |  |
| zonnesteen | Sun Stone |  |
| Oom Miroh | Uncle Miroh | Iroh as a Mika (DESIGN) |
| windgong | Wind Chime | the block `einde2_deco_windgong` |
| witte lotustegel | White Lotus Tile | the block `einde2_deco_lotustegel` |
| wachtpost | guard post |  |
| Vetschip Njegus | G.S. Nyegus | G.S. = Grease Ship, as S.S. |
| karavaan | caravan |  |
| Appa-afdruk | Appa Footprint |  |
| jip jip | yip yip | what Appa answers to |
| boemerang | boomerang |  |
| Sokka-guh, Aang-guh | Sokka Guh, Aang Guh |  |

### bouw_lucht

| Dutch | English | note |
|---|---|---|
| Zuidelijke Luchttempel | Southern Air Temple | contract 3.5 |
| Hal van de Guhvatars | Hall of the Guhvatars | the statue hall |
| de Werktuigguh | the Mechanist Guh | the Mechanist of the show; on a sign "- Mechanist Guh" |
| Werkplaats / Werkplaatsdeur | Workshop / Workshop Door | the door's sign: "Nog niet af, njeg!" = "Not finished yet, nyeg!" |
| Vredesballon | Peace Balloon | the tier-5 end project; never called the last or the final one |
| Bouwplaats | Building Site | |
| Guhatso | Guhatso | monk Gyatso; stays as it is |
| Taart van Guhatso / taart | Guhatso's Pie / pie | the fruit pies he throws; NOT cake (guh_taart = Guh Cake is another block) |
| Luchtnomade / luchtnomaden | Air Nomad / Air Nomads | Luchtnomadentegel = Air Nomad Tile |
| Waterstam / Aarderijk / Vuurnatie | Water Tribe / Earth Kingdom / Fire Nation | on the statue signs |
| Pijltegel | Arrow Tile | |
| Tandwielblok | Gear Block | |
| Blauwdruk van de Werktuigguh | Mechanist Guh's Blueprint | |
| Luchtbalveld / luchtbal | Airball Court / airball | |
| Luchtscooter(-baan) | Air Scooter (Track) | |
| Zweefringen | Glide Rings | |
| Rustpunt | Rest Point | |
| Tempelbel / Windgong / Windfluiten | Temple Bell / Wind Gong / Wind Flutes | |
| Stal van Appa | Appa's Stable | |
| Monnikentoren / windtoren / grote toren | the Monks' Tower / the Wind Tower / the Great Tower | only in code comments and the wiki text so far |
| Wan-guh, Szeto-guh, Yangchen-guh, Kuruk-guh, Kyoshi-guh, Roku-guh, Aang-guh | Wan Guh, Szeto Guh, Yangchen Guh, Kuruk Guh, Kyoshi Guh, Roku Guh, Aang Guh | earlier Guhvatars; "<name>-guh" = "<Name> Guh" as Appa-guh = Appa Guh |
| Plons-guh / Kluit-guh | Splash Guh / Clod Guh | two made-up early Guhvatars (water, earth) |
| kaasbol | cheese ball | "Ging even vadsen in een kaasbol" = "Went for a chonk in a cheese ball" |

### bouw_water

The Noordelijke Waterstam parodies the Northern Water Tribe of Avatar: The Last Airbender. Names follow the well-known English
ones of the show, guh-ified the way the Dutch is (the kit's section 3.5 has the names shared with the other slices).

| Dutch | English | note |
|---|---|---|
| Noordelijke Waterstam | Northern Water Tribe | the structure (contract 3.5) |
| Waterstam-ijsstenen | Water Tribe Ice Bricks | "Water Tribe Ice Brick Stairs", "Water Tribe Ice Brick Slab" |
| IJslantaarn | Ice Lantern | |
| Waterstamteken | Water Tribe Emblem | block |
| Vuurnatieteken | Fire Nation Emblem | block |
| Vuurnatie | Fire Nation | the Mikas of this story |
| Meester Pakguh | Master Pakguh | Pakku; "alleen guhs met een snor" = "only guhs with a mustache" |
| hoofdman Arnook-guh | Chief Arnook Guh | |
| prinses Yue-guh / Yue | Princess Yue Guh / Yue | |
| Katara-guh / Sokka-guh | Katara Guh / Sokka Guh | |
| admiraal Zhao-Mika / Zhao | Admiral Zhao Mika / Zhao | "Zhao de Onoverwinbare" = "Zhao the Invincible" (his own words) |
| Yugoda-guh | Yugoda Guh | the healer |
| Tui en La | Tui and La | the moon fish and the ocean fish of the Spirit Oasis; unchanged |
| Geestenoase | Spirit Oasis | |
| Golfkansel | Wave Pulpit | the platform above the gate where the great wave is made |
| Golfbassin | Wave Pool | on the training square |
| Glijkansel / Otterslee | (the start of the) Otter Sled | sign: "Otter Sled" |
| Otterhoek | Otter Corner | |
| otter(-pinguïnguh) | otter (Otter-Penguin Guh) | contract 3.5 |
| Landingsijs | Landing Ice | |
| Kamp Guhvatar | Camp Guhvatar | |
| Appa / Momo | Appa / Momo | |
| Gondel / Gondelhalte / Gondelwerf / Gondelverhuur | Gondola / Gondola Stop / Gondola Yard / Gondola Rental | |
| watersturen | waterbending | "Zelf duwen met watersturen" = "Push it yourself with waterbending" |
| Genezershut | Healing Hut | |
| Wapenkamer | Armory | American spelling |
| Krijgsraad / Raadzaal | War Council / council hall | |
| Maankamer | Moon Room | |
| Maanwachters | Moon Watchers | |
| Snorrenschool / Plaksnorren | Mustache School / Stick-On Mustaches | |
| Zeepruimen | Sea Prunes | as in the show |
| Zweepdoelen | Whip Targets | water whip |
| De Dampende Guh | The Steaming Guh | tea house |
| Brr-en-breakfast | Brr and Breakfast | pun on bed and breakfast; keep the pun |
| Bakker Brrr | Baker Brrr | |
| Badmeester | Lifeguard | |
| Havenmeester | Harbor Master | American spelling |
| VNS Schroeiplek | FNS Scorch Mark | Zhao's flagship ("Vuurnatieschip" = "Fire Nation Ship") |
| Wachtlokaal / Hut van Zhao / Brug | Guard Room / Zhao's Cabin / Bridge | on the ship |
| Hier vadsen alleen Mika's | Only Mikas chonk here | vadsen = to chonk |
| zwarte sneeuw | black snow | soot of the fleet: the warning sign of the show |

### bouw_aarde

| Dutch | English | note |
|---|---|---|
| Ba Sing Guh | Ba Sing Guh | never translated (the contract's name) |
| Oud Ba Sing Guh | Old Ba Sing Guh | the city under the city |
| Er is geen knabbeltekort in Ba Sing Guh | There is no nibble shortage in Ba Sing Guh | the running line of the city (the show's "There is no war in Ba Sing Se"); on signs: "There is no / nibble shortage." |
| Stadssteen van Ba Sing Guh | Ba Sing Guh City Stone | the big wall stone |
| Aardemuntsteen | Earth Coin Stone | aardemunt = Earth Coin (the emblem: round, a square bite out of it) |
| Kool van de koolman / de koolman | Cabbage Man's Cabbage / the Cabbage Man | "MIJN KOLEN!" = "MY CABBAGES!" (the show's line) |
| koolkraam | cabbage stall | |
| Monorailbalk | Monorail Beam | |
| Catacombenkristal | Catacomb Crystal | |
| Theekist | Tea Crate | |
| onderring / middenring / bovenring | Lower Ring / Middle Ring / Upper Ring | the show's names |
| buitenmuur / buitenpoort | Outer Wall / outer gate | station "Buitenmuur" = "Outer Wall" |
| paspoortloket / de loketguh | passport desk / the desk guh | |
| De Jasmijn Draak | The Jasmine Dragon | the show's name |
| Oom Miroh | Uncle Miroh | Miroh = Mika + Iroh; stays |
| Aardrommel | Earth Rumble | the show's name |
| De Blinde Bandiet / De Kei-guh | The Blind Bandit / The Boulder Guh | the show's names |
| blinddoekbaan | blindfold course | |
| Meer Laogai | Lake Laogai | the show's name |
| Botenverhuur | Boat Rental | the shed that is the way down to the base |
| Dai Li(-Mika) | Dai Li (Mika) | plural "Dai Li Mikas" |
| Joo Dee-guh | Joo Dee Guh | |
| Aardekoning-guh | Earth King Guh | |
| beer Bosco | Bosco the bear | "Gewoon een beer." = "Just a bear." |
| Long Feng-Mika, Cultuurminister | Long Feng Mika, Minister of Culture | |
| De Koningsput | The King's Well | the shaft from the palace garden to the crystal chamber |
| kristalcatacomben / kristalkamer / kristalcel | crystal catacombs / crystal chamber / crystal cell | |
| gasthuis | guest house | |
| luchtbizon(-guh) | sky bison (guh) | as the Appa slice |
| das-molguh | Badger-Mole Guh | contract 3.5 |
| aardesturen | earthbending | as the show |
| kn (op het menubord) | nb | "2 kn" = two knabbels -> "2 nb" (nibbles) |

### bouw_vuur

| Dutch | English | note |
|---|---|---|
| Vuurnatie | Fire Nation | as the show |
| Vuurheer Mikai | Fire Lord Mikai | Ozai as a Mika; never translated |
| Zuka / Oom Miroh / Azula-Mika / Ty Lee-Mika | Zuka / Uncle Miroh / Azula Mika / Ty Lee Mika | the names of DESIGN 5 |
| Lauwe lava | Lukewarm Lava | the block: looks like lava, harms nobody |
| Lauwe vlam | Lukewarm Flame | |
| Vuurnatie-embleem | Fire Nation Emblem | |
| Portret van Vuurheer Mikai | Portrait of Fire Lord Mikai | |
| Zonnekrijgersteen | Sun Warrior Stone | Zonnekrijgers = Sun Warriors |
| Witte Lotus-tegel | White Lotus Tile | Orde van de Witte Lotus = Order of the White Lotus |
| Kraterpoort | Crater Gate | the tunnel through the crater wall |
| Havenpoort | Harbor Gate | American spelling |
| Het Kokend Rotsblok | The Boiling Rock | as the show |
| De Koelcel | The Cooler | as the show |
| Sintelstrand | Ember Island | the show's name for the place; "Ember Beach" where only the beach is meant |
| De jongen in de kaasbol | The Boy in the Cheese Ball | the play (the show: The Boy in the Iceberg); kaasbol = Cheese Ball |
| Dag van de Zwarte Zon | Day of Black Sun | as the show |
| Drakendans | Dancing Dragon | as the show |
| Leeuwschildpad-guh | Lion Turtle Guh | |
| vuurvlokken | fire flakes | as the show |
| duwstok | shoving stick | Mikas only shove |
| Heet Hapje | Hot Bite | the bakery |
| De Lauwe Plons | The Lukewarm Splash | the bathhouse |
| VNS Aangebrand | FNS Scorched | the moored ship (VNS = Vuurnatie-schip) |
| havikpost | messenger hawk mail | |

### bouw_zij1

| Dutch | English | note |
|---|---|---|
| Omashguh | Omashguh | the mountain town (Omashu); untranslated |
| Westelijke Luchttempel | Western Air Temple | |
| Vuurtempel van Roku-guh | Fire Temple of Roku Guh | |
| koning Bumi-guh | King Bumi Guh | |
| Flopsie | Flopsie | the king's far too big pet |
| postgoot | Mail Chute | the block of the slides; "postglijbaan" = mail slide |
| postkantoor / postguh / postbak | Post Office / the mail guh / Mail Bin | |
| de kurkentrekker | the corkscrew | the spiral tower on the post square |
| proef | Trial | Proef 1-3: the trial rooms of the king |
| kristalsnoep | Crystal Candy | rock candy; the reward ring is phase B |
| das-molguh | Badger-Mole Guh | as in the shared glossary |
| luchtnon-guh | the air nun guh | |
| windgang / windfluit | wind corridor / Wind Flute | "Windfluit 3" = "Wind Flute 3" |
| orgelpijp | organ pipe | the fifth, vertical wind corridor |
| luchtbizon-guh / bizonstal / hok | sky bison guh / Bison Stable / Pen | |
| Vuurwijze | Fire Sage | |
| vuurdeur | fire door | |
| heiligdom | sanctum | |
| Guhvatar Roku-guh | Guhvatar Roku Guh | |
| eeuwige vlam | Eternal Flame | |
| vuurvlokken | Fire Flakes | the snack |
| Halvemaaneiland | Crescent Island | |
| zonnewende | solstice | |
| Stiltezaal / Eetzaal | Quiet Hall / Dining Hall | |
| Grote plekken van de buiteneilanden | Great Places of the Outer Islands | FTB section title |

### bouw_zij2

| Dutch | English | note |
|---|---|---|
| Kyoshi-dorp | Kyoshi Village | contract 3.5 |
| Bibliotheek van Wan Shi Guh | Wan Shi Guh's Library | contract 3.5 |
| Leeuwschildpad-eiland / Leeuwschildpad-guh | Lion Turtle Island / Lion Turtle Guh | contract 3.5 |
| Leegtehaven | Void Harbor | contract 3.5; American spelling |
| Guhvleugel-ringenrace | Guh Wing Ring Race | contract 3.5 |
| Ringenrace-ring | Ring Race Ring | the block of the hoops |
| Haven, dorp en bibliotheek | Harbor, Village and Library | FTB section title |
| Kyoshi-guh | Kyoshi Guh | the statue; "Kyoshi-krijgsterguhs" = Kyoshi Warrior Guhs |
| dojo / waaierdans / schminktafel | Dojo / fan dance / Makeup Table | |
| dorpsoudste | village elder | |
| Unaguh | Unaguh | the Unagi of the pond; not translated |
| uilenzaal / kennisbalie | Owl Hall / Knowledge Desk | |
| Zaal 1-5: Knabbelkunde, Vadskunst, Njegologie, Mika-zaken, Kaaskaarten | Hall 1-5: Nibble Studies, Art of Chonking, Nyegology, Mika Matters, Cheese Maps | vads = chonk, njeg = nyeg |
| planetarium / Het Wiel | planetarium / The Wheel | |
| prof. Zei-guh | Prof. Zei Guh | |
| steiger / veerpier / scheepswerf / helling / loods | Jetty / Ferry Pier / Shipyard / slip / The Shed | |
| De Vadsige Walvis | The Chonky Whale | the pub of the Void Harbor |
| De Vadsige Golf | The Chonky Wave | the ferry |
| De Spijtige Spatel | The Sorry Spatula | the kitchen that waits for the Mika cook |
| De Vette Hap / De Gladde Aal | The Greasy Bite / The Slippery Eel | the two moored Grease Ships |
| Vuurtoren Guhoortje | Guh Ear Lighthouse | |
| prikbord | notice board | |
| Flitskraam / Vet & Honing / Bijna Vers / Teruggevonden | Flash Stall / Grease & Honey / Almost Fresh / Lost & Found | the four market stalls |
| Starttoren / Landingsbaan / Landingskussen | Start Tower / Landing Strip / Landing Cushion | |
| Kruimelkraam | Crumb Stall | |
| Erepodium | Winners' Podium | |

### sponsguh_wezens

The English follows the well-known English names of the parodied show, guh-ified the same way as the Dutch (build contract 3.5).

| Dutch | English | note |
|---|---|---|
| Sponsguh Kaasbroek | Spongeguh Cheesepants | the hero of the story; "Sponsguh" alone = Spongeguh |
| Patguh Ster | Patguh Star |  |
| Meneer Krabguh | Mr. Krabguh |  |
| Parel | Pearl | his daughter |
| Sandy-guh | Sandy Guh |  |
| Gerrit | Gary | the snail; he says njeg = nyeg |
| Octo Tentakel | Squidmika Tentacles | Octo-Mika = Squidmika |
| Octoliam | Squilliam | the rival |
| Plankmika | Plankmika |  |
| Karen | Karen | his computer wife (a block) |
| Juf Pufguh | Mrs. Puffguh |  |
| Koning Neptuguh | King Neptuguh |  |
| Prinses Mindy-guh | Princess Mindy Guh |  |
| Dennis-Mika | Dennis Mika |  |
| De Cycloop | The Cyclops | "the Cyclops" in running text |
| David Hasselguh | David Hasselguh |  |
| Loopgraafmonster | Trench Monster | de loopgraaf = the Trench |
| Bikinibodemguh | Bikini Bottom Guh | a citizen |
| De Vliegende Guhlander | The Flying Guhchman | the ghost captain and his ship |
| Zeemeerman | Mermaid Guh |  |
| Mosselguh | Barnacle Guh |  |
| Knabbelburgermobiel | Patty Wagon |  |
| Knabbelburger | Nibble Patty |  |
| kwallenjam | Jelly Jam |  |
| Krokante grill | Krusty Grill | De Krokante Knabbel = The Krusty Nibble |
| Besteltafel | Order Table |  |
| Vruchthuis (-wand, -dak, -raam, -deur) | Fruit House (Wall, Roof, Window, Door) |  |
| Bellenblaas | Bubble Blower |  |
| Spooklantaarn | Ghost Lantern |  |
| Vierkante broek met stropdas | Square Pants with Tie | clothes |
| Krokante Knabbel-petje | Krusty Nibble Cap | clothes |
| Patguh's bloemetjesbroek | Patguh's Flower Trunks | clothes |
| Sandy's luchthelm | Sandy's Air Helmet | clothes |
| Plankmika-antennes | Plankmika Antennas | clothes |
| Guhfy Guhber-ijshoedje | Guhfy Guhber Ice Cream Hat | clothes |
| Heldencape | Hero Cape | clothes |
| maatje (Sponsguh) | buddy | "Sponsguh als maatje" = Spongeguh as your buddy |
| knabbelkluis | nibble safe |  |
| eetsok | dining sock | the Flying Guhchman's dining sock |
| onzichtbare bootmobiel | invisible boatmobile |  |

Lines that follow a well-known English line of the show:

| Dutch | English |
|---|---|
| Ik ben er klaar voor! | I'm ready! |
| Is knabbelsaus een instrument? | Is nibble sauce an instrument? |
| Hallo, ik hou van knabbels! | Hello, I like nibbles! |
| MIJN POOT! | MY LEG! |
| KWAAAD! | EVIL! |

### bouw_bikinibodem

| Dutch | English | note |
|---|---|---|
| Bikinibodem onder de leegte | Bikini Bottom Under the Void | the structure name; Bikinibodem = Bikini Bottom (kern.md) |
| Gloeikoraal | Glow Coral | the street lighting of the town |
| Zelfportret van Octo Tentakel | Self-Portrait of Squidmika Tentacles | |
| Klarinet op standaard | Clarinet on a Stand | |
| Bewakingscamera | Security Camera | |
| Archiefkast | Filing Cabinet | Plan A to Plan Z |
| Bellenplein | Bubble Square | the plein with the lift |
| Hoofdstraat / Bellenlaan / Schelpstraat | Main Street / Bubble Lane / Conch Street | Schelpstraat follows the show (Conch Street) |
| Vaarschool | Boating School | juf Pufguh = Mrs. Puffguh |
| Rusthuis De Luie Schelp | The Lazy Shell Rest Home | the show: Shady Shoals |
| Zeemeergrot | Mermalair | follows the show |
| Rotsbodem | Rock Bottom | the bus that never comes |
| Aanlegplaats | Mooring | of De Vliegende Guhlander (The Flying Guhchman) |
| Tot bubbels! | See you in the bubbles! | the goodbye of the town |
| "Ik ben er klaar voor!" | "I'm ready!" | the well-known line |
| "CHOCOLA?!" | "CHOCOLATE?!" | the well-known line |
| "MIJN POOT!" | "MY LEG!" | (sponsguh_wezens uses the same) |

### bouw_schelpenstad

Names of build contract 3.5 are used as they stand (Schelpenstad = Shell City, De Ruige Kroeg = The Thug Tug, de loopgraaf =
the Trench). New here:

| Dutch | English | note |
|---|---|---|
| Leegtezee | Void Sea | the void beyond the beach of Shell City |
| Schelpenstad Souvenirs & Zo | Shell City Gifts & Stuff | the Cyclops's shop (the film: "Marine Gifts & Sundries") |
| schelpsteen | Shell Stone | building block |
| droogtelamp | Drying Lamp | "heat lamp" of the film; Drying fits the dried souvenirs |
| gedroogd souvenir | Dried Souvenir |  |
| wiebeloogjes | googly eyes |  |
| kroonkussen | Crown Cushion |  |
| stopcontact | Power Socket |  |
| sproeier | Sprinkler |  |
| neonbord | Neon Sign | Neonbord van De Ruige Kroeg = Thug Tug Neon Sign |
| ansichtkaartenrek | Postcard Rack |  |
| De Lange Njeg | The Long Nyeg | the lighthouse |
| Guhwatch | Guhwatch | the lifeguard tower (Baywatch) |
| De Zoute Snoet | The Salty Snout | beach bar |
| Kaartjes & Kruimels | Cards & Crumbs | postcard kiosk |
| De Brullende Mika's | The Roaring Mikas | the band on the bar's poster |
| baby'tje | baby | "Wie hier een bel blaast is een baby'tje" = "Whoever blows a bubble here is a baby" (the film's bubble-blowing baby) |
| Groetjes uit Schelpenstad | Greetings from Shell City | advancement, postcards |
