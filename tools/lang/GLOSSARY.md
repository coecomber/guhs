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
| Bankguh / Bank Guh | Bank Guh | storage, 256 of each kind (endless with the Bottomless Nibble Belly, section 24); *het buikje* = *the tummy* |
| Guhrad / Guhdraad | Guh Wheel / Guh Wire | they make and carry *vadskracht* = *chonk power* (section 24), no longer redstone |
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

## 24. Guh Technology, the Barbecuether buildings, The Lord of the Nibble Ring, Super Guhrio (bbq2)

Chunks `c40` - `c62`. Everything above still holds: njeg = **nyeg**, vads = **chonk**, vahoeg = **wahoog**, knabbel =
**nibble**, guhtje in a name = **guhling**, drankje = **Potion**, kooltje = **Ember**, beeldje = **Statuette**, snoet =
**snoot**, buikje = **tummy**, American spelling (*cozy, neighbor, mustache, apartment, campground, range hood*).

**The one fixed line.** Guhdalf on the bridge (`scene.guhs.ringh3_brug.you_1` .. `you_4`, the advancement and the FTB quest
of the same name) says **"YOU.. SHALL.. NOT.. VADS!"** in BOTH languages, word for word: this one "VADS" is never "CHONK".

**Parodies keep their quote.** The Nibble Ring story is *The Lord of the Rings*, Super Guhrio is *Super Mario*: where the
Dutch bends a famous line, take the famous ENGLISH line and bend it the same way (table "famous lines" below). Names of the
cast keep their guh/Mika pun and follow the English names of the originals (Rivendel -> Rivendell, Merrie -> Merry).

**Nobody gets hurt.** A Mika, the Barbecuerog, the Eye, the Nine, a Flomp Mika or a falling ember only *shove* or *send you
back* to your rest fire / flag: never "kill", "die", "damage", "attack", "lives". The Eye *looks you back*, the Nine *sniff*.

**How they talk.**
- **Smikagol** hisses and says "we" for "I": stretch the s at the END of a word as the Dutch does (*vissss* -> *fishesss*,
  *oogjesss* -> *eyesss*, *Sssst!* -> *Shhhh!*), calls the ring AND the player *vadsje* = **chonkie** (*ons vadsje* = *our
  chonkie*, *Deze kant op, vadsje!* = *This way, chonkie!*). Never "precious".
- **Sam-guh** calls the player *baas* = **boss** (as the guhs on the postcards do) and Guhdalf *meneer Guhdalf* = *Mr. Guhdalf*.
- **Marktmeester-Mika** (a cat-like Mika) says *Mjauw* = **Meow**. **Pad-guh** says *Njam-njeg!* = *Nom-nyeg!*
- **Guhdalf**'s spells are dog Latin and stay: *Vadsus opendus! Knabbelum portalis!* -> *Chonkus opendus! Nibblum portalis!*
- Counted time in screens: *tellen* = *seconds* (as in section 23).

### vadskracht, bank, tech

| Dutch | English | note |
|---|---|---|
| Guh-technologie | Guh Technology | FTB chapter, advancement tab, Tales heading |
| vadskracht | chonk power | lowercase in prose; replaces the old *guhkracht* = *guh power* everywhere; no unit: "%s chonk power" |
| Knuffelkracht | Cuddle Power | advancement title |
| opstelling | setup | everything on one stretch of Guh Wire; *Opstelling 1* = *Setup 1* (practice hall) |
| guhmachine(s) / guh-machine | guh machine(s) | lowercase in prose; the block *Guhmachine* (machine_deel) = *Guh Machine* |
| Telt niet mee | Doesn't count | hover line of a source over the limit |
| Te zwaar / Te groot / staat uit | Too heavy / Too big / is off | hover lines of a setup |
| een machine met een snoet / snoetje | a machine with a snoot / little snoot | every guh machine has a face |
| Proefbron / Proefbatterij | Test Source / Test Battery | dev blocks |
| Proefmachientje / Groot proefmachientje | Little Test Machine / Big Test Machine | dev blocks |
| Knabbelbatterij | Nibble Battery | |
| Knuffelgenerator | Cuddle Generator | knuffel = the act here, so Cuddle |
| Disco-dynamo | Disco Dynamo | no hyphen |
| Blubkacheltje | Blub Stove | "little blub stove" in prose where the Dutch is cute about it |
| Gloeisterkern | Glowstar Core | *Een ster in een kooitje* = *A Star in a Little Cage* |
| draaitafel / bakje | turntable / little tray | parts of the dynamo and the stove |
| Knabbelbuis / Knabbelbuizen | Nibble Tube / Nibble Tubes | as *hamster tubes*, *Tube Workbench* |
| Richtingstuk | Direction Piece | |
| Filterstuk | Filter Piece | |
| Opzuiger | Slurper | *Slurp!* stays |
| Voorraadmeter | Stock Meter | |
| Snuffelsensor | Sniff Sensor | not the Nibble Sensor (Knabbelsensor) |
| Guhklok / Guhteller | Guh Clock / Guh Counter | |
| Rollebollen / Kieskeurig guhtje / De guh ziet alles | Roly-Poly / Picky Little Guh / The Guh Sees All | advancement titles |
| Sauspomp / Sausslang / Sausvat | Sauce Pump / Sauce Hose / Sauce Vat | |
| Grillkoolpers | Grill Coal Press | |
| Brouwautomaat | Auto Brewer | |
| Frituurautomaat | Auto Fryer | |
| Saus uit de grond / Tapje, njeg? | Sauce from the Ground / A Little Tap, Nyeg? | advancement titles |
| Roeren is voor vroeger / Vanzelf vahoeg | Stirring Is So Yesterday / Wahoog All by Itself | advancement titles |
| Onder druk wordt alles grillkool | Under Pressure Everything Turns to Grill Coal | |
| Oogster | Harvester | |
| Knabbelaar | Nibbler | the block breaker |
| Neerzetter | Placer | |
| Knutselmachine | Tinker Machine | *knutselen* (of this machine) = *to tinker*: *Het knutselt vanzelf* = *It Tinkers by Itself* |
| Tekentafel | Drawing Table | |
| Bouwtekening / Lege bouwtekening / Bouwtekening: %s | Blueprint / Empty Blueprint / Blueprint: %s | also *Bouwtekening: Mika-brug* = *Blueprint: Mika Bridge* |
| Plantagebak | Plantation Box | bak = Box (as *Chonk Seed Box*); *Een bos in een bak* = *A Forest in a Box* |
| Vadsmolen | Chonk Mill | |
| Stepstation | Scooter Station | step = kick scooter |
| Haltepaaltje / halte | Stop Post / stop | *aflever-halte* = *drop-off stop* |
| Bezorgguhtje | Delivery Guhling | creature, variant, Guhdex page; *Bezorgguhtje-fluitje* = *Delivery Guhling Whistle* |
| Ophalen / Afleveren | Pick Up / Drop Off | what a stop does |
| Rugzak / Haltes / Naar huis! / Ander station / Leegmaken | Backpack / Stops / Go Home! / Other Station / Clear | Scooter Station screen |
| Tuut tuut! / Bezorgd, njeg! / Op je wenken bediend | Toot Toot! / Delivered, Nyeg! / At Your Beck and Call | advancement titles |
| Hapluikje / luikje | Nom Hatch / hatch | hap = nom: *Hap!* = *Nom!*, *het luikje hapt* = *the hatch noms* |
| Banksleutel | Bank Key | |
| Bodemloos Knabbelmaagje | Bottomless Nibble Belly | the bank upgrade; *Bodemloos buikje* = *Bottomless Tummy* |
| Vol is vol / opgevoerd | Full Is Full / upgraded | bank: 256 of each kind, "%s/%s" |
| Machines bijvullen & leeghalen | Refill & Empty Machines | chore name, in the style of *Tidy Up & Sort* |
| Plantage: planten & hakken | Plantation: Plant & Chop | chore name |
| voordoen / voorgedaan | show it once / shown | what a guh learns to refill |
| klus-area / klusgebied | chore area | one word for both |
| scheutjes | sprouts | as *Peanut Sprouts* |
| Collega's met een snoet / Houthakkertje guh | Colleagues with a Snoot / Little Lumberjack Guh | advancements, diary firsts |
| Hap, opgeruimd! / Pompoenenplukker / Machinist guh | Nom, All Tidy! / Pumpkin Picker / Machinist Guh | |
| Oude Guhrad-centrale | Old Guh Wheel Power Plant | structure; "the power plant" in prose |
| Uitvinder-guh | Inventor Guh | |
| oefenhal | practice hall | *De oefenhal draait weer* = *The Practice Hall Is Running Again* |
| Project 1: Nooit meer zelf bakken | Project 1: Never Bake Yourself Again | FTB sections |
| Project 2: Alles vanzelf in de bank | Project 2: Everything into the Bank by Itself | |
| Project 3: Saus uit de kraan | Project 3: Sauce on Tap | |
| Project 4: Een fabriek die doorwerkt | Project 4: A Factory That Keeps Going | |
| De Grote Knabbelmachine | The Great Nibble Machine | *Stukje Grote Knabbelmachine* = *Piece of the Great Nibble Machine* |
| Knabbelmachine-beeldje | Nibble Machine Statuette | |
| Knabbelmachinist | Nibble Machinist | title |
| Perfecte knabbel | Perfect Nibble | |
| Receptkaart / receptkaarten | Recipe Card / recipe cards | *Receptkaart: saus en slangen* = *Recipe Card: Sauce and Hoses* |
| Receptkaart: knabbelende machines / het Bezorgguhtje / Plantagebak | Recipe Card: Nibbling Machines / The Delivery Guhling / Plantation Box | |
| Oefenknabbel / Propje papier | Practice Nibble / Paper Wad | practice hall items |
| Ome Knabbel | Uncle Nibble | a sign in the practice hall |
| Wordt vervolgd in het Guheinde... | To Be Continued in the Guh End... | the locked FTB slot |
| Bestelguh / Guhterminal / draadloze vadskracht | Order Guh / Guh Terminal / wireless chonk power | named in that slot only |

### gebouwen (the Guhbarbecuether buildings)

| Dutch | English | note |
|---|---|---|
| frituursauszee / sauszee | frying sauce sea / sauce sea | the Barbecuether's lava sea of Cheese Frying Sauce |
| Sausloper | Sauce Strider | vanilla Strider nod |
| Sausblubje / blubje | Sauce Blubby / blubby | plural *Sauce Blubbies*; *Blubje to go* = *Blubby to Go* |
| Blubroom | Blub Cream | "blub cream" in prose |
| Sausblubje in een potje | Sauce Blubby in a Jar | a creature in a jar, not "Potted" |
| Pindasaus aan een stok | Peanut Sauce on a Stick | |
| Stuiterdrankje / Stuiterblub | Bouncy Potion / Bouncy Blub | Guh Potion and its effect |
| Sausloper-stal | Sauce Strider Stable | |
| Verzorger-guh | Caretaker Guh | not the Stable Hand Guh of the Mika Stable |
| sausbak / proefrit / proefrondje | sauce basin / test ride / test lap | |
| Sausloper-vriend / Sausracer | Sauce Strider Friend / Sauce Racer | |
| Lange poten, klein hartje / Kom maar, langpoot! | Long Legs, Little Heart / Come On, Longlegs! | |
| Vriendschap gaat door de maag / Een knuffel te veel / Boing! | Friendship Goes Through the Tummy / One Cuddle Too Many / Boing! | |
| Rookguh-vuurtoren | Smoke Guh Lighthouse | |
| Torenwachter-guh | Lighthouse Keeper Guh | |
| Pepertuin | Pepper Garden | *Pluktuin* = *Picking Garden* |
| Peperteler-guh | Pepper Grower Guh | |
| Vuurtorenlamp / Seinlantaarn / Lampkooltje | Lighthouse Lamp / Signal Lantern / Lamp Ember | |
| Verdwaalde Rookguh | Lost Smoke Guh | |
| Kweekbak / Peperplant / Peperzaadjes | Grow Box / Pepper Plant / Pepper Seeds | |
| Njegpeper / Vahoegpeper / Snoeppeper | Nyeg Pepper / Wahoog Pepper / Candy Pepper | green / red / pink |
| Pepervuurdrankje / Peperzoetdrankje / peperdrankje | Pepperfire Potion / Peppersweet Potion / pepper potion | |
| Peperadem | Pepper Breath | effect |
| Wachtersjas van de vuurtoren / Peperslinger | Lighthouse Keeper's Coat / Pepper Garland | outfit pieces |
| Licht in de rook / Peperbrouwer / Lampaansteker | A Light in the Smoke / Pepper Brewer / Lamplighter | |
| Heet! Heet! Heet! / Ahoi, njeg! / Fiet-fiew! | Hot! Hot! Hot! / Ahoy, Nyeg! / Fweet-fwee! | |
| Mika-woonblokken | Mika Apartments | American: never "flats" |
| flat / westflat / oostflat / hoge flat | apartment block / west block / east block / tall block | *daktuin* = *roof garden*, *galerij* = *walkway* |
| Mika-stal | Mika Stable | |
| Mika-brugpaleis | Mika Bridge Palace | *Door de bek van de Mika* = *Through the Mika's Mouth* |
| Mika-oma | Granny Mika | as *Granny Nibbles* |
| Mopper-Mika / mopperaars | Grumpy Mika / grumps | |
| Brom-Mika / Zeur-Mika / Snurk-Mika | Grumble Mika / Whiny Mika / Snore Mika | no hyphens, as every Mika |
| Buur-Mika / Barbecue-Mika / Zonnebad-Mika | Neighbor Mika / Barbecue Mika / Sunbathing Mika | |
| Worstzwijntje | Sausage Piglet | creature, variant |
| Knorretje | Oinky | the runaway piglet; *Knorretje (in je armen)* = *Oinky (in Your Arms)* |
| Knir / Snuffel / Mosterdje / Truffel | Snort / Snuffles / Mustard / Truffle | name signs in the stable |
| Knor! / Zwijntjesfluisteraar | Oink! / Piglet Whisperer | |
| Stalknecht-guh | Stable Hand Guh | |
| Tolwachter-Mika | Toll Keeper Mika | *HALT! Tol!* = *HALT! Toll!*, *Tolvrij!* = *Toll-Free!* |
| tolbel / tolhuis / tolbrug | toll bell / toll house / toll bridge | *DONG!* stays |
| Raadselkoning / Mika-kwaliteit | Riddle King / Mika Quality | |
| Oma's worstsoep / Soep van oma | Granny's Sausage Soup / Granny's Soup | |
| Breiwerk van Mika-oma / Breimandje van Mika-oma | Granny Mika's Knitting / Granny Mika's Knitting Basket | |
| Gebreide Mika-muts | Knitted Mika Cap | muts = cap |
| Zak zwijnenvoer | Sack of Piglet Feed | |
| Worstzwijntje in een mandje | Sausage Piglet in a Basket | |
| Brugplank van de Tolwachter / Mika-brugplanken / Touwleuning | Toll Keeper's Bridge Plank / Mika Bridge Planks / Rope Railing | |
| Wachter-guh | Guard Guh | at the Skewer Keep; not the Gatekeeper |
| Ere-wachter / wachterspak | Honorary Guard / guard outfit | |
| Wachtershelm / Wachtersmantel | Guard's Helmet / Guard's Cloak | |
| Knuffelmaker-guh | Plushie Maker Guh | |
| Knuffelguh / Knuffel-Mika / Knuffel-Rookguh | Plush Guh / Plush Mika / Plush Smoke Guh | the three plushie BLOCKS; in prose they are "plushies" |
| Knuffelpatroon | Plushie Pattern | |
| De wacht bij de Spiesburcht / De gestolen knuffels | The Watch at the Skewer Keep / The Stolen Plushies | storylines |
| Brugvuurkorf / brugvuren | Bridge Brazier / bridge fires | *Fwoesj!* = *Fwoosh!* |
| Mikakruid | Mika Weed | onkruid pun; *wieden* = *to weed* |
| Zielig lantaarntje | Sad Little Lantern | |
| Aansteekspies van de Wachter-guh / Lantaarnrecept van de Wachter-guh | Guard Guh's Lighting Skewer / Guard Guh's Lantern Recipe | |
| Op kousenvoetjes / Knuffels horen bij guhs | On Tiptoe / Plushies Belong with Guhs | |
| wachthokje / naaihoek | sentry box / sewing corner | |
| Zoutkristal | Salt Crystal | |
| Zoutkristalmijn | Salt Crystal Mine | |
| Zoutkristalader / Zoutkristalerts / Zoutkristalblok / Zoutkristalletjes | Salt Crystal Vein / Salt Crystal Ore / Salt Crystal Block / Little Salt Crystals | *kristalader* = *crystal vein* |
| Zoutkristalhouweel | Salt Crystal Pickaxe | |
| Puin op het spoor | Rubble on the Track | *karrenspoor* = *cart track* |
| Mijnwerker-guh | Mineworker Guh | not the Miner Guh (Mijnguh) of the Cheese Mine |
| Fossiel-opgraving | Fossil Dig | |
| Archeoloog-guh | Archaeologist Guh | |
| Bottenzand / Skeletrek | Bone Sand / Skeleton Rack | |
| Guhkwastje | Guh Dusting Brush | not the Guh Brush (Guhborstel) of the farm; *kwasten* = *to dust* |
| Tyrannoguhrus Njex / Njex | Tyrannoguhrus Nyex / Nyex | *Njex!* = *Nyex!* |
| Tyrannoguhrus-beeldje | Tyrannoguhrus Statuette | |
| Tyrannoguhrus-schedel / -ruggengraat / -ribben / -pootjes / -staartje | Tyrannoguhrus Skull / Spine / Ribs / Legs / Tail | |
| Guhceratops | Guhceratops | stays |
| Een korreltje zout / Zout op de boterham / Njex op de kast | A Grain of Salt / Salt on Your Sandwich / Nyex on the Shelf | |
| Botten in de as / Een berg van zout / De vondst van de eeuw | Bones in the Ash / A Mountain of Salt / The Find of the Century | |
| Grillcamping | Grill Campground | not the Snuggledale Campsite (Kampeerplekje) |
| De Gloeiende Guh | The Glowing Guh | the campground's name |
| Kampbaas-guh | Camp Boss Guh | |
| Houthakker-guh | Lumberjack Guh | not the Woodcutter Guh of the Palewood; *Tjak! Tjak!* = *Chop! Chop!* |
| Nether-Mika-ruilmarkt / ruilmarkt | Nether Mika Barter Market / barter market | piglin-barter nod |
| Marktmeester-Mika / Marktmeester | Market Master Mika / Market Master | says *Mjauw* = *Meow* |
| Kraam-Mika | Stall Mika | |
| Saté-Mika / Vads-Mika / Kolen-Mika | Satay Mika / Chonk Mika / Coal Mika | the three stalls |
| de Waag / Weegschaal van de Waag | the Weigh House / Weigh House Scale | |
| Stapel vads / nepvads | Stack of Chonk / fake chonk | *De nepvads* = *The Fake Chonk*; *NEP!* = *FAKE!* |
| Keurstempel van de Marktmeester | Market Master's Seal of Approval | |
| afdingen / Meester-afdinger | to haggle / Master Haggler | *bod* = *offer* |
| koopje van de dag / extraatje | deal of the day / little extra | |
| Smikkel | Smikkel | the Market Master's nephew, stays |
| tentdoek (Rood/Blauw/Geel/Groen/Crèmekleurig) | Tent Canvas (Red/Blue/Yellow/Green/Cream) | *Red Tent Canvas* |
| tentdoek (schuin) / tentdoekplaat | Tent Canvas (Sloped) / Tent Canvas Slab | *Red Tent Canvas (Sloped)*, *Red Tent Canvas Slab* |
| Tentzak / Tentharing / Kampeerplekbordje | Tent Bag / Tent Stake / Campsite Sign | |
| Hakblok / Groot kampvuur / Bos brandhout / Roosterstok | Chopping Block / Big Campfire / Bundle of Firewood / Roasting Stick | *goudbruin* = *golden brown* |
| Kampeerhoedje / Padvindersdasje / Kampeerrugzak / kampeerpakje | Camping Hat / Scout Neckerchief / Camping Backpack / camping outfit | |
| Kampeerguh / Kampregels | Camper Guh / Camp Rules | |
| Kamperen bij De Gloeiende Guh / De nepvads van de ruilmarkt | Camping at The Glowing Guh / The Fake Chonk of the Barter Market | storylines |

### verhaal (the story engine)

| Dutch | English | note |
|---|---|---|
| Mijn verhaal | My Story | Super Compass entry; in prose a tale you follow is a "story", the tab stays *Tales* |
| Doel op het scherm | Objective on Screen | *aan / uit* = *on / off* |
| Reiskaart | Journey Map | *De reis van de Knabbelring* = *The Journey of the Nibble Ring* |
| Je bent hier / Dit moet je nu doen | You are here / What to do now | on the Journey Map |
| vertelkaart | story card | the narrator's card before a chapter |
| filmpje | scene | a cutscene: *Kijk het filmpje* = *Watch the scene* |
| Opnieuw bekijken | Watch Again | heading |
| Hoofdstuk %s | Chapter %s | |
| Guhdalfs sluier / sluier / rookmuur | Guhdalf's Veil / veil / wall of smoke | the smoke wall around a chapter you have not reached |
| Guhdalf vindt dat je hier nog niet aan toe bent, njeg | Guhdalf thinks you're not ready for this yet, nyeg | |
| Je volgt dit verhaal / vanzelf / vastzetten | You're following this story / automatically / pin | |
| Dit hoort bij het verhaal / bij het gebouw | This is part of the story / of the building | protected ground |
| rustpunt / Rustpunt | rest stop / Rest Stop | structure name *Rest Stop* |
| Rustvuurtje | Rest Fire | where a shove sends you back to |

### Knabbelring (The Lord of the Nibble Ring)

| Dutch | English | note |
|---|---|---|
| In de ban van de Knabbelring | The Lord of the Nibble Ring | the Dutch is the Dutch title of *The Lord of the Rings*: FTB chapter, advancement tab, Tales heading, clothing source |
| De Knabbelring / de ring | The Nibble Ring / the ring | item *The Nibble Ring*; "the Nibble Ring" in prose |
| Ringdrager / ringdrager / ringdragertje | Ring-Bearer / ring-bearer / little ring-bearer | title *Ring-Bearer* |
| Het Reisgenootschap van de Knabbelring / het Reisgenootschap | The Fellowship of the Nibble Ring / the Fellowship | *reisgenoten*, *het gezelschap* = *the companions*, *the company* |
| Guhdalf | Guhdalf | stays; *Guhdalf de Grijze / de Witte* = *Guhdalf the Grey / the White* (the one British "Grey": it is his name) |
| Sam-guh | Sam-guh | stays, with the hyphen |
| Araguh / Leguhlas / Gimguh / Boromika / Pippguh | Araguh / Leguhlas / Gimguh / Boromika / Pippguh | stay |
| Merrie | Merry | |
| Guhrond / Guhladriel | Guhrond / Guhladriel | stay; *Vrouwe Guhladriel* = *Lady Guhladriel* |
| Smikagol | Smikagol | stays; *Smikagol (krokant)* = *Smikagol (Crispy)* |
| Guhstapper | Guhstrider | Araguh's nickname (Stapper = Strider) |
| Gimguh, zoon van Gluhin | Gimguh, son of Gluhin | |
| Durguh | Durguh | stays; *Durguh, Heer van Knabbelmoria* = *Durguh, Lord of Nibblemoria* |
| Sausron | Sauceron | |
| Het Oog van Sausron / het Oog | The Eye of Sauceron / the Eye | *Beeldje van het Oog van Sausron* = *Eye of Sauceron Statuette* |
| Sausuman | Sauceruman | *Sausuman van de Vele Sauzen* = *Sauceruman of Many Sauces* |
| Barbecuerog | Barbecuerog | stays; *Braadworstzweep* = *bratwurst whip* |
| Knekel-Mika-ruiter / de Negen | Skelly Mika Rider / the Nine | Knekel-Mika = Skelly Mika |
| Uruk-Mika | Uruk-Mika | stays |
| dwerg-guh / boomguhs / Gouwguh | dwarf guh / tree guhs / Shire Guh | *dwergendeur* = *dwarf door* |
| Rookguhje | Smoke Guhling | the three caged ones on Mount Fry |
| Knabbelgouw | Nibble Shire | "the Nibble Shire" in prose; *de Gouw* = *the Shire* |
| Knabbel-eind | Nibble End | Bag End |
| heuvelholletje / moestuin / Hovenier | hill hole / vegetable patch / Gardener | |
| Bakkerguh Knabbelings | Baker Guh Nibbins | Baggins |
| Tante Lobelia Guhzak | Aunt Lobelia Guhsackville | Sackville-Baggins |
| Visser Guhpkuil | Fisher Guhpool | |
| Rozie Katoenguh | Rosie Cottonguh | |
| Een langverwacht knabbelfeest | A Long-Expected Nibble Party | chapter 1 |
| afscheidsfeest / Guhdalfs vuurwerkkist / vuurpijl | farewell party / Guhdalf's Fireworks Crate / rocket | *Fwoesh!* = *Fwoosh!* |
| Feesttafel van de Knabbelgouw / Proviandkrat / Heuvelschoorsteentje | Party Table of the Nibble Shire / Provisions Crate / Little Hill Chimney | *proviand* = *provisions* |
| tweede ontbijt / derde ontbijt / elfuurtje / tussendoorknabbel | second breakfast / third breakfast / elevenses / in-between nibble | |
| grillportaal | grill portal | the Barbecuether portal of the big Barbecue Pit |
| Guhvendel | Guhvendell | Rivendell |
| Het Laatste Knusse Huis | The Last Cozy House | Last Homely House |
| De Raad van Guhrond / raadskring / raadsbel | The Council of Guhrond / council ring / council bell | chapter 2 |
| Knabsil / de scherven van Knabsil | Nibsil / the shards of Nibsil | Narsil |
| De Mijnen van Knabbelmoria / Knabbelmoria | The Mines of Nibblemoria / Nibblemoria | chapter 3; knabbel = nibble here too |
| Brug van Knabbel-dûm / Knabbel-dûm | Bridge of Nibble-dûm / Nibble-dûm | Khazad-dûm; keep the û |
| De Poort van Durguh / Diepe Poort / Zuilenhal / Hal van de Hefbomen | The Gate of Durguh / Deep Gate / Hall of Pillars / Hall of Levers | |
| Zeg njeg en treed binnen | Say Nyeg and Enter | "Speak friend and enter"; *Sesam, open u!* = *Open sesame!* |
| Brokkelpad / Brokkelsteen | Crumble Path / Crumble Stone | |
| de Kloof van Knabbelmoria | the Chasm of Nibblemoria | |
| Dwergen-hefboom / hefboom | Dwarf Lever / lever | |
| Runensteen van Knabbelmoria / rune / tombe | Runestone of Nibblemoria / rune / tomb | |
| Trommels in de diepte / Doem... doem... | Drums in the deep / Doom... doom... | |
| De emmer van Pippguh / Dwaas van een Pippguh | Pippguh's Bucket / Fool of a Pippguh | "Fool of a Took" |
| Aan de worst geregen / Even de diepte in / Eronderdoor | Strung on the Sausage / A Quick Trip into the Deep / Underneath | advancement titles |
| De Spiegel van Guhladriel / Spiegel van Guhladriel | The Mirror of Guhladriel / Mirror of Guhladriel | chapter 4 / the block |
| Guhlórien / Caras Guhladhon | Guhlórien / Caras Guhladhon | stay; *boomstad* = *tree city*, *het gouden woud* = *the golden wood* |
| de Grote Spies | the Great Skewer | the mallorn of the tree city |
| gastenvlonder | guest platform | |
| de Guhduin / de Arguhnath / de Guhkoningen | the Guhduin / the Arguhnath / the Guh Kings | *de Pilaren van de Guhkoningen* = *the Pillars of the Guh Kings* |
| de Sausval van Rauguhs | the Sauce Falls of Rauguhs | |
| Elfenbootje / elfenogen | Elven Boat / elf eyes | |
| De gaven van Guhladriel / Guhladriels zegen | The Gifts of Guhladriel / Guhladriel's blessing | |
| Lichtflesje | Light Phial | |
| Elfenmanteltje | Little Elven Cloak | the story item ("be a rock"); the outfit is *Elfenmantel met blaadjesspeld* = *Elven Cloak with Leaf Pin* |
| Elfentouw / Elfentouwhaak | Elven Rope / Elven Rope Hook | |
| De Zwarte Roosterpoort | The Black Grill Gate | chapter 5; rooster = grill, as *Grill Iron* |
| Roosterwachter / Poortrooster / Roosterpoortje | Grill Guard / Gate Grate / Little Grill Door | |
| Blik van het Oog | Gaze of the Eye | |
| het Asveld / de Kale Vlakte / de Schaduwlaan | the Ash Field / the Bare Plain / Shadow Lane | in Ash Vale (Asdal) |
| het Wachthek / de Slakkenhut / de Holte | the Guard Fence / the Slag Hut / the Hollow | |
| de schedelpaal / de uitkijkrug / sluipweg | the skull post / the lookout ridge / sneak path | |
| Kamp van de Reisgenoten / Basiskamp | Camp of the Fellowship / Base Camp | |
| Ik ben een rots, njeg / Achterom | I'm a Rock, Nyeg / Round the Back | |
| De Frituurberg | Mount Fry | chapter 6 and structure; Mount Doom |
| Frituurspleet | the Crack of Fry | Crack of Doom; scene title *The Crack of Fry* |
| Kronkelpad / Eerste, Tweede, Derde Richel / Bakrandje | the Winding Path / First, Second, Third Ledge / the Frying Edge | |
| Kooislot van de Frituurberg / Vallend kooltje | Cage Lock of Mount Fry / Falling Ember | |
| afzuigkap | range hood | *Geen afzuigkap meer* = *No More Range Hood*; signs *AFZUIGKAP 1* = *RANGE HOOD 1* |
| Stukje gefrituurde Knabbelring | Piece of the Fried Nibble Ring | |
| Toren van Sausuman | Tower of Sauceruman | |
| De Ringenbakker | The Ring Baker | his machine |
| Mika-rad | Mika Wheel | the wheel an Uruk-Mika trudges in (a Mika does not run) |
| Sputterpijp / Deegkneder / Sauskraan / Kaaskast | Sputter Pipe / Dough Kneader / Sauce Tap / Cheese Cupboard | |
| Mokhoek / mokken / Ik mok | Sulking Corner / to sulk / I'm Sulking | |
| Pannantír | Pannantír | stays (palantír + pan) |
| Voorraad van Sausuman | Sauceruman's Supplies | |
| Uienring / Ringdeeg | Onion Ring / Ring Dough | |
| Kannetje hete frituursaus / Ui uit de Kaaskast | Jug of Hot Frying Sauce / Onion from the Cheese Cupboard | |
| Stoofpotje van Sam-guh / stoofpotje | Sam-guh's Stew / stew | |
| Feestknabbel | Party Nibble | |
| Smikagols vissenbotje | Smikagol's Fish Bone | |
| lekkere vissss | tasty fishesss | |
| Guhdalfs punthoed met baard | Guhdalf's Pointy Hat with Beard | outfit |
| Harige hobbitvoetjes / De ring aan een kettinkje | Hairy Hobbit Feet / The Ring on a Chain | outfit |
| Een eigen Sam-guh / Een eigen Guhshi | A Sam-guh of Your Own / A Guhshi of Your Own | advancements |
| Mewtwo-guh (in the mirror wink) | Guhtwo | the Dutch line means Guhtwo (section 8) |

**Famous lines (the Nibble Ring).**

| Dutch | English | note |
|---|---|---|
| YOU.. SHALL.. NOT.. VADS! | YOU.. SHALL.. NOT.. VADS! | identical, see the top of this section |
| Een tovenaar komt nooit te laat. Hij komt precies wanneer de knabbels klaar zijn | A wizard is never late. He arrives precisely when the nibbles are ready | title: *A Wizard Is Never Late* |
| Bewaar hem geheim. Bewaar hem heel. En wat je ook doet: NIET opeten. | Keep it secret. Keep it whole. And whatever you do: do NOT eat it. | |
| Kook ze, stamp ze, stop ze in een stoofpotje | Boil 'em, mash 'em, stick 'em in a stew | *Aardappelen!* = *Po-ta-toes!* |
| Verder van huis dan ooit | Farther from Home than Ever | |
| Ik ga niet zonder jou / Ik ga mee! | I'm Not Going Without You / I'm Coming Too! | |
| Ik neem de ring wel mee! Al weet ik de weg niet. | I will take the ring! Though I do not know the way. | |
| Men wandelt niet zomaar naar de Frituurberg | One does not simply walk to Mount Fry | |
| Pootjes vegen, njeg | Wipe your paws, nyeg | the line; on the three SIGNS it is *Wipe paws, nyeg* (a sign line is 90 pixels wide) |
| dingen die nog gebakken moeten worden | things that have yet to be baked | the mirror; title *Wat nog gebakken moet worden* = *What Has Yet to Be Baked* |
| Niet duister, maar ROND en VADS als de dageraad! | Not dark, but ROUND and CHONK as the dawn! | |
| Ik heb alleen maar een beetje trek | I'm only a tiny bit hungry | |
| Tot hier, en geen kruimel verder / Vaarwel, Guhlórien | This Far, and Not a Crumb Further / Farewell, Guhlórien | |
| Wij zweren het op het vadsje / Ons vadsje | We Swears It on the Chonkie / Our Chonkie | |
| Ruik jij ook knabbel? / Wie het licht heeft | Do You Smell Nibble Too? / Whoever Holds the Light | |
| Eén knabbel om ze allemaal te delen | One Nibble to Share Them All | the story's motto (FTB subtitle) |
| Eén ring om ze allemaal op te eten / Eén uienring om ze allemaal op te eten | One Ring to Eat Them All / One Onion Ring to Eat Them All | |
| Ik kan de ring niet dragen... maar wel jou, njeg! / Maar wel jou | I can't carry the ring... but I can carry you, nyeg! / But I Can Carry You | |
| Het prikt in onze oogjesss | It Stings Our Little Eyesss | |
| Daar staat hij dan / Lang leve de koning | There It Stands / Long Live the King | |
| Niet omdoen! / De Negen te snel af / Even uitpuffen | Don't Put It On! / Outrunning the Nine / Catching Your Breath | |
| Iedereen wil een hapje / Er is altijd wel iets te vieren | Everyone Wants a Bite / There's Always Something to Celebrate | |

### Super Guhrio

| Dutch | English | note |
|---|---|---|
| Super Guhrio / Guhrio / Luiguh | Super Guhrio / Guhrio / Luiguh | stay |
| Guhmba / Guhshi | Guhmba / Guhshi | stay; plural *Guhmbas* |
| Kasteel van de Grote Nether-Mika | Big Nether Mika's Castle | "Bowser's Castle" |
| Grote Nether-Mika | Big Nether Mika | as *Big Mika* |
| Prinses Perzikguh | Princess Peachguh | |
| Pad-guh / Pad-guh's kraam | Toad-guh / Toad-guh's Stall | |
| Schild-Mika / Schild-Mika-schild | Shell Mika / Shell Mika Shell | Koopa |
| Plof-Mika | Flomp Mika | Thwomp; PLOF! = FLOMP! |
| Hapbloem | Chomp Flower | Piranha Plant |
| Draaiende grillspies / Naaf van een grillspies | Spinning Grill Skewer / Grill Skewer Hub | fire bar |
| Superknabbel / Gegooide knabbel | Super Nibble / Thrown Nibble | |
| Vuurpeper / Vuurpeperstruik | Fire Pepper / Fire Pepper Bush | |
| Grote vadsmunt / vadsmunten | Big Chonk Coin / chonk coins | *(al gehad)* = *(Already Collected)*; *Achttien keer vads* = *Eighteen Times Chonk* |
| Guhrio-munt / munten | Guhrio Coin / coins | what Toad-guh's stall takes |
| Ei van Guhshi / Een gespikkeld ei | Guhshi's Egg / A Speckled Egg | |
| Vraagtekenblok / Onzichtbaar vraagtekenblok | Question Block / Hidden Question Block | |
| Knabbel-vraagtekenblok | Nibble Question Block | the one for at home |
| Uitroeptekenschakelaar / Schakelblok / klokschakelaar | Exclamation Switch / Switch Block / timer switch | |
| Valblok / Platform | Falling Block / Platform | |
| Groene pijp / Groene pijp (onderstuk) | Green Pipe / Green Pipe (Base) | |
| Warppijp / Warppijp (onderstuk) | Warp Pipe / Warp Pipe (Base) | |
| Groene reispijp | Green Travel Pipe | the one for at home |
| Levelpoort / levelhal | Level Gate / level hall | |
| Guhrio-vlaggetje / Guhrio-vlaggenmast / Guhrio-startvlag | Guhrio Flag / Guhrio Flagpole / Guhrio Start Flag | the flag is the checkpoint a shove sends you back to |
| Kasteelvlaggenmast | Castle Flagpole | the one for at home |
| Guhrio-blok / Guhrio-steen / Guhrio-grond / Guhrio-deur / Guhrio-siersteen | Guhrio Block / Guhrio Brick / Guhrio Ground / Guhrio Door / Guhrio Trim Brick | |
| ...-plekje (Guhmba-, Hapbloem-, Platform-, Plof-Mika-, Schild-Mika-, Valblok-) | ... Spot (*Guhmba Spot*, *Chomp Flower Spot*, ...) | builder marker blocks; *Wachtplekje van Guhshi* = *Guhshi's Waiting Spot*, *Plekje van de Grote Nether-Mika* = *Big Nether Mika's Spot* |
| Guhrio-tip / Guhrio-geheimpje | Guhrio Tip / Guhrio Secret | |
| Geschilderd(e) gras / aarde / heg / wolk / lucht | Painted Grass / Dirt / Hedge / Cloud / Sky | the painted scenery of world 1 |
| Geschilderde heuvel / heuvel met oogjes / Geschilderd tuinplantje | Painted Hill / Painted Hill with Eyes / Painted Garden Plant | |
| Geschilderd loof in de verte / wolkje in de verte / wolkje met een snoet | Painted Distant Leaves / Painted Distant Cloud / Painted Cloud with a Snoot | |
| Wereld 1: de binnentuin / Wereld 2: de kelders / Wereld 3: de burcht | World 1: The Courtyard / World 2: The Cellars / World 3: The Keep | burcht = Keep, as *Skewer Keep* |
| Level 1-1: de binnentuin / Level 1-2: de heggentuin | Level 1-1: The Courtyard / Level 1-2: The Hedge Garden | |
| Level 2-1: De buizenkelder / Level 2-2: Het nest van Guhshi | Level 2-1: The Pipe Cellar / Level 2-2: Guhshi's Nest | |
| Level 3-1: De Grillgang / Level 3-2: De Sauskelder | Level 3-1: The Grill Corridor / Level 3-2: The Sauce Cellar | |
| Het hele kasteel in één keer | The Whole Castle in One Go | high score row |
| het mollenhol / de muntenkas | the Mole Hole / the Coin Greenhouse | the secrets of world 1 |
| Keldergrond / Keldersteen | Cellar Ground / Cellar Brick | |
| Eierslot / Broednest van Guhshi / Broedplekje | Egg Lock / Guhshi's Hatching Nest / Hatching Spot | |
| Roosterbrug / Brughendel | Grate Bridge / Bridge Lever | |
| Guhshi-parkeerpaal | Guhshi Hitching Post | |
| Gloeiend kooltje (the duel) | Glowing Ember | the existing name |
| Taartkarretje van Prinses Perzikguh | Princess Peachguh's Cake Cart | |
| voorplein / torenkamer / Peperkamer / schatkamer | forecourt / tower room / Pepper Room / treasure room | |
| Highscorebord van het kasteel | Castle High Score Board | |
| Rode Guhrio-pet met snor / Groene Luiguh-pet | Red Guhrio Cap with Mustache / Green Luiguh Cap | |
| Kroontje van Prinses Perzikguh / Gouden vadspet | Princess Peachguh's Little Crown / Golden Chonk Cap | |
| Loodguhter | Plumbguh | loodgieter pun |
| Vlaggenmastenverzamelaar / Super vahoege vadsverzamelaar | Flagpole Collector / Super Wahoog Chonk Collector | |
| Njeg! Mama! / Binnendoor / Kelder-vads / Krak... njeg! | Nyeg! Mama! / Shortcut / Cellar Chonk / Crack... nyeg! | |
| Guhshi kruipt uit zijn ei / Een stukje taart / Taart voor iedereen | Guhshi Hatches from His Egg / A Piece of Cake / Cake for Everyone | |

**Famous lines (Super Guhrio).**

| Dutch | English | note |
|---|---|---|
| Bedankt! Maar de prinses is in een ander kasteeldeel, njeg | Thank you! But the princess is in another part of the castle, nyeg | "...in another castle"; title *In een ander kasteeldeel* = *In Another Part of the Castle* |
| Het is-a mij, Guhrio! | It's-a Me, Guhrio! | |
| Hij had gewoon trek in taart | He Just Wanted Some Cake | |
| Geheim gevonden, njeg! | Secret found, nyeg! | |
| Een held! Njeg! / Guhshi is van jou! | A Hero! Nyeg! / Guhshi Is Yours! | |

### Added during the review of c40 - c62 (cross-chunk rules and names)

**Floors.** Dutch *begane grond* = *ground floor*; *eerste / tweede / derde verdieping* = *one floor up / two floors up /
three floors up* (never "first / second / third floor": American and British counting differ by one, and the player has to
find the right stairs). *een trap omhoog* = *one staircase up*.

**A quoted button is the button.** Where a text quotes a screen label, use the label's own words: the Filter Piece's
*alleen deze / alles behalve / laat liggen* = *only these / all except / leave behind* (screen: *Only these*, *All except*,
*Leave behind:*), a Stop Post's *Ophalen / Afleveren* = *Pick Up / Drop Off*.

**The quest book ticks.** *afvinken* = *to tick off* (as in the older chapters), and the closing lines of a "How Do You
Get Here?" quest are the standard ones: *Nothing is locked: every quest ticks itself off as soon as you've done it, even
if you did it earlier. The headings show what logically comes after what. Tick this off and get chonky to work!*

**Steps of a story** (`gui.guhs.verhalen.<story>.stap.N`) are sentences: only the first word and names get a capital
(*The lookout ridge*, *Up the rope*, but *The Ash Field*, *Shadow Lane*). Quest and advancement TITLES stay Title Case.

**Dutch that stays in en_us on purpose.** Guhdalf's *YOU.. SHALL.. NOT.. VADS!*; the word **VRIJ** on the campsite board of
the Grill Campground (it is painted into the texture, green, and *BEZET* in red: the English says *a green VRIJ (vacant)
sign*, never "a VACANT sign"); the dev command `/guhs verhaal demo`; the names *Smikkel*, *Guhdalf*, *Smikagol*...

| Dutch | English | note |
|---|---|---|
| baasje (Smikagol to the player) | master | *Volg ons, vadsje... eh, baasje* = *Follow us, chonkie... er, master*; a guh's note still says *my human* |
| Opa Njeg / Tante Vads / Oma Vahoeg / Neef Guh | Grandpa Nyeg / Auntie Chonk / Granny Wahoog / Cousin Guh | the runners of the Old Guh Wheel Power Plant (with *Uncle Nibble*) |
| Prof. dr. Guh | Prof. Dr. Guh | sign at the Fossil Dig |
| wachtkamer (Knabbelmoria) / westpoort / oostpoort / valhek | guard room / West Gate / East Gate / portcullis | the *Wachtkamer* of the toll bridge is a *Waiting Room* |
| steiger | dock (boats, Sauce Striders) / scaffold (under the toll bridge) / little jetty (the castle) | |
| gang (under the Black Grill Gate) | tunnel | *de gang* of Nibblemoria is *the corridor* |
| wei / box / gangpad / voerbak (Mika-stal) | paddock / pen / aisle / feeding trough | |
| receptie / houtschuur / kampeerplekjes | reception / woodshed / campsites | Grill Campground |
| afdakje | little roof | Fossil Dig, Ash Field |
| kijkbedden / brouwhoek | display beds / brewing corner | Pepper Garden |
| knabbelbak (vuurtoren) | Nibble Trough | |
| pollen Mikakruid | clumps of Mika Weed | |
| brok(ken) puin / hakken (puin, zout) | chunk(s) of rubble / to chip | *Hak maar raak!* = *Chip away!* |
| marktbewijs | market permit | what you haggle over |
| kaasvijver | cheese pond | Super Guhrio world 1 |
| ?-blok | ?-block | in the duel's texts; the block itself is the *Question Block* |
| Floep! (the lighthouse lamp) | Fwoop! | *floept aan* = *fwoops on*; the catapult's *Floep!* is still *Floop!* |
| Hmpf | Hmph | |
| %s uur (a duration) | %s h | never "%s hours" (1 hours) |

## 25. The Guh Path, the old stories' scenes, Sniff Island (verhalenpad)

Chunks `c63` - `c67`. Everything above still holds: njeg = **nyeg**, vads = **chonk**, vahoeg = **wahoog**, knabbel =
**nibble**, pootjes = **pawsies** (a sign says *paws*), American spelling (*harbor, gray, color, favorite, neighbor*).

**Sniff Island is our own story.** It was inspired by the premise of an old dog adventure game and by nothing more: every
name here is ours, and the English must be ours too. Use the names of the tables below and never a term you remember from a
dog game. In particular a *Snuffelmeester* is a **Sniff Maestro** (never "Sniff Master"), the *geneesbloem* is the
**healing flower** (never a "legendary flower") and the *maatje* is a **buddy**, a **forest sprite**.

**Tales and stories.** Titles, the tab and the chapters are *Tales* (*Tales of the Guhmension*, the *Tales* tab); in a
sentence a tale you follow is a *story*: *de grote verhalen* = *the big stories*, *Verhalen gevolgd: %s van %s* = *Stories
followed: %s of %s*, *volg deze verhalen* = *follow these stories* (as *My Story* and *You're following this story* in
section 24).

**Nobody gets hurt, nobody fails.** A dog that swims too far is *washed back ashore*, a dog behind the roadblock is *put
back*. *Zakken bestaat niet* = *There's no such thing as failing*.

**How they talk.**
- The dogs of the island say *njeg* like everybody else. *Woef!* = *Woof!*, *Kef!* = *Yip!*, *Snuf snuf* = *Sniff sniff*.
  A puppy's sad *Piep...* = *Whimper...* (an excited *Piep!* may be *Yip!*; never *Squeak*: that is the mouse of Squeak!).
- **Kapitein Zoutsnoet** calls the player *landrot* = **landlubber**; *Ahoi* = *Ahoy*.
- **Oma Wolletje** calls everybody *lieverd* = **dearie**. The residents call the player *pup* = **pup**.
- **Meester Truffelneus** is a schoolmaster: short, strict, kind. *Zoek!* = *Seek!* His motto *Neus omlaag, staart omhoog*
  = *Nose down, tail up*.
- *papa* = **Papa** (a name: *I'm going after Papa!*), *je vader* = *your father*, *je broertje of zusje* = *your little
  brother or sister* (the game does not know which).

**The four colors of the scent meter are fixed** (the lessons, the exam, the hints and the quest book all use them):
*oranje: iets lekkers* = **orange: something tasty**, *blauw: een ding* = **blue: a thing**, *groen: een dier* = **green:
an animal**, *paars: iets vreemds* = **purple: something strange**. The four kinds stand after *Je ruikt %s* = *You
smell %s*, so they are lowercase and carry their own article.

**The rank line** is *%s (rang %s (laagste) van %s (hoogste))* = *%s (rank %s (lowest) of %s (highest))*, on a sign *(rang 2
van 5)* = *(rank 2 of 5)*: a player must always see which rank they have and how many there are.

**Steps of the story** (`gui.guhs.verhalen.snuffeleiland.stap.N`) are sentences (section 24): *Washed ashore*, *Good
deeds*, *The sniffing exam*. Scene, quest and advancement titles with the same Dutch are Title Case: *Washed Ashore*.

**Signs.** A sign line is its own key and shows 90 pixels (about 15 letters). One key can stand on several signs
(*Snuffeldorp*, *Snuffelschool*, *Truffelneus* do): keep a name alone on its line so every sign still reads well.

### Het Guhpad (c63)

| Dutch | English | note |
|---|---|---|
| Het Guhpad / het Guhpad | The Guh Path / the Guh Path | the big stories open the worlds; FTB chapter group, Guhdex path map |
| padkaart / halte | path map / stop | the map at the top of the Tales tab |
| Verhalen van de Guhmensie | Tales of the Guhmension | FTB chapter (was *Guhverhalen* = *Guh Tales*) |
| Verhalen van de Guhbarbecuether | Tales of the Guhbarbecuether | FTB chapter (holds the Nibble Ring and Super Guhrio) |
| Verhalen van het Guheinde | Tales of the Guh End | the Guheinde heading of the Tales tab (until 1.5.0 also the FTB chapter, was *Het Guheinde*) |
| Guheinde: algemeen / Guheinde: Guhvatar / Guheinde: Guhvatar extra / Guheinde: Sponsguh | Guheinde: general / Guheinde: Guhvatar / Guheinde: Guhvatar extras / Guheinde: Sponsguh | the four FTB chapters of the Guheinde (3. / 3.1 / 3.2 / 3.3, 1.5.0; the user's names, "Guheinde" kept in English) |
| Het echte Guheinde / het echte Guheinde | The Real Guh End / the real Guh End | the locked last chapter; mysterious, no spoilers |
| Verhalen gevolgd | Stories Followed | statistic and quest title; the counter is *Stories followed: %s of %s* |
| groot verhaal / de grote verhalen | big story / the big stories | the seven that count for the Guh Path |
| op slot / open | locked / open | *Op slot voor iedereen* = *Locked for everyone* |
| Slotje: eerst de Guhmensie / Slotje: eerst de Guhbarbecuether | Lock: The Guhmension First / Lock: The Guhbarbecuether First | the lock quests |
| Het Guhpad begint hier | The Guh Path Starts Here | first quest of *Tales of the Guhmension* |
| Guh-technologie, laag 6 / Guh-technologie: laag 6 / laag 6 | Guh Technology, tier 6 / Guh Technology: Tier 6 / tier 6 | the placeholders of the real Guh End |
| het knabbelfeest van Guhdalf | Guhdalf's nibble party | chapter 1 of the Nibble Ring (*A Long-Expected Nibble Party*) |
| De Aangebrande Mika verslaan | Beat the Burnt Mika | |
| Baltoguh en Nomguh / Guhtwo en het kloon-eiland | Baltoguh and Nomguh / Guhtwo and Clone Island | the big stories by their path names |
| Het Hemelkapelletje / Ohana op Guhwai'i | The Cloud Chapel / Ohana on Guhwai'i | |
| guhportaal / grillportaal / het portaal in de Knabbelkelder | guh portal / grill portal / the portal in the Nibble Cellar | |

### The old stories' scenes (c64)

The six scenes play in stories that have had English since 1.2.0: every name in them is an older name (sections 6 - 16;
the chunk's source file lists the ones its lines use).

| Dutch | English | note |
|---|---|---|
| De witte wolf-guh | The White Wolf Guh | scene title and speaker (Baltoguh's story) |
| Dag 45: de nacht van de knal | Day 45: The Night of the Bang | Guhtwo's story; the professor's notes say *Day 45. BOOM!* |
| Ohana, bij het kampvuurtje | Ohana, by the Campfire | the ohana line is word for word the English of `gui.guhs.guhwaii.ohana.citaat` |
| Het Knuffelhart klopt | The Snuggleheart Beats | *Bonk... bonk.* = *Thump... thump.* |
| Hij brandt weer! | It's Burning Again! | the line: *WAHOOG, it's burning again!* (as the advancement of that name) |
| Het dak zit erop | The Roof Is On | |
| Boris (in je hoofd) | Boris (in your head) | speaker |
| Het eerste bewonertje | The first little resident | speaker |
| AUUUHOE-NJEG! | AWOOO-NYEG! | the howl |
| Wie ben ik... en waar zijn mijn knabbels? | Who am I... and where are my nibbles? | Guhtwo |
| Dan is het begonnen, njeg | Then it has begun, nyeg | the stranger with the pointy hat (the speaker is shown as ???) |

### Sniff Island: places, things and words (c65 - c67)

| Dutch | English | note |
|---|---|---|
| Het Snuffeleiland / het Snuffeleiland | Sniff Island / Sniff Island | no article in English: *on Sniff Island*, *to Sniff Island*; dimension, biome, story |
| HET SNUFFEL EILAND | SNIFF ISLAND | the title on the Guhstation's little screen |
| Snuffeldorp | Sniffville | the village |
| Steigerhuisje / steigerhuisje | Dock Cottage / dock cottage | the structure in the Guhmension; *steiger* = *dock* (section 24) |
| De Natte Neus | The Wet Nose | the captain's boat |
| Guhstation | Guhstation | stays; *kastje* = *little box*, *spelkastje* = *game console* |
| Geheugenkaart / geheugenkaart | Memory Card / memory card | the item that brings you home |
| Snuffelboekje / snuffelboekje | Sniff Book / sniff book | |
| snuffelen / opsnuffelen / snuffelaar | to sniff / to sniff out / sniffer | |
| snuffeltoets / geurmeter | sniff key / scent meter | the meter *swings* (*slaat uit*); *Slaat hij helemaal uit?* = *Swinging all the way?* |
| geur / Geleerde geuren | scent / Scents learned | |
| aanvalsknop | attack button | you dig with it: *graven* = *to dig*, *Graaf!* = *Dig!* |
| Snuffelpup | Sniff Pup | rank 1 (*rang* = *rank*); lowercase *snuffelpup* (a way to call you) = *sniff pup* |
| Snuffelneus | Sniff Nose | rank 2; the doctor's *een echte snuffelneus* = *a real sniff nose* |
| Snuffelspeurder | Sniff Sleuth | rank 3 (as *Nibble Sleuth*) |
| Snuffelmeester | Sniff Maestro | rank 4; never "Sniff Master" |
| Opper-Snuffelmeester | Grand Sniff Maestro | rank 5 |
| goede daad / goede daden | good deed / good deeds | |
| maatje / Maatje | buddy / Buddy | the companion only you can see |
| bosgeestje / Het bosgeestje | forest sprite / The forest sprite | what a buddy is |
| Zweefzaadje / Mos-eikeltje / Zonnepluisje | Driftseed / Moss Acorn / Sunfluff | the three buddies |
| boompje / Boompje van je maatje | little tree / Your Buddy's Little Tree | |
| kiem / scheutje / struikje / jong boompje | sprout / shoot / little bush / young tree | the tree's four steps; *een kaal plekje* = *a bare patch* |
| stenenkrans | ring of stones | around the little tree |
| Bloesemtakje van je boompje / bloesemtakje | Blossom Twig from Your Little Tree / blossom twig | |
| snuffelles / snuffelschool / Snuffelschool | sniffing lessons / sniff school / Sniff School | |
| snuffelexamen / snuffeldiploma | sniffing exam / sniffing diploma | |
| snuffelkoorts | sniffle fever | what Little Wobble has |
| geneesbloem | healing flower | never "legendary flower" |
| lantaarnfeest / het lantaarnfeest | Lantern Feast / the Lantern Feast | *lampion* = *paper lantern* |
| wegversperring | roadblock | friendly |
| Eilandhond / Snuffelhond / Steigerhond | Island Dog / Sniff Dog / Dock Dog | entity names |
| het strand / strandpoortje / weipoortje | the beach / beach gate / meadow gate | |
| de wei (Snuffeleiland) | the meadow | not the *paddock* of the Mika Stable |
| het plein / het pleintje / de put | the square / the little square / the well | |
| havenkantoor | Harbor Office | *de haven* = *the harbor* |
| dokterspraktijk / praktijk (dokter) | doctor's office / office | the white house with the red cross |
| het schooltje / juttershut / vissershut | the little school / beachcomber's hut / fisherman's hut | |
| moestuin / kippenhok / bijeneik | vegetable patch / chicken coop / bee oak | *moestuin* as in section 24 |
| kluifje / fluitje / deegroller | chew bone / whistle / rolling pin | scents |
| dobber | bobber | *Dobber dobbert, visser vist* = *Bobber bobs, fisher fishes* |
| schoolbel / bol wol / gietertje | school bell / ball of wool / little watering can | wool smells of sheep |
| stuiterbal / reservepet / De sjaal van papa | bouncy ball / spare cap / Papa's scarf | |
| Ras / Vacht | Breed / Coat | the choice screen |
| Shiba / Jack russell / Teckel | Shiba / Jack Russell / Dachshund | |
| Corgi / Golden retriever / Mopshond | Corgi / Golden Retriever / Pug | |
| Speurhond | Sleuth Hound | the trainer's breed |
| Zwart-tan / Chocola-tan / Driekleur | Black and Tan / Chocolate and Tan / Tricolor | coats; the others are plain color words (*Crème* = *Cream*) |

### Sniff Island: who is who

| Dutch | English | note |
|---|---|---|
| Dokter Pleisterpoot | Doctor Plasterpaw | on his sign *Dokter* + *Pleisterpoot* = *Doctor* + *Plasterpaw* |
| Meester Truffelneus | Master Trufflenose | the sniffing teacher; alone on a sign *Truffelneus* = *Trufflenose* |
| Kapitein Zoutsnoet | Captain Saltsnout | on a sign *Kapt. Zoutsnoet* = *Capt. Saltsnout* |
| Jutje Kwispel | Wendy Wagtail | *Strandjutter* = *Beachcomber* |
| Papa Zwerfpoot | Papa Wanderpaw | *die ouwe Zwerfpoot* = *old Wanderpaw* |
| Bakker Kruimelsnuit | Baker Crumbsnout | |
| Visser Natneus | Fisher Wetnose | |
| Juf Blaffetje | Miss Barkley | |
| Oma Wolletje | Granny Woolly | |
| Tuinder Knolletje | Gardener Turnip | *knollen* = *turnips* |
| Kleine Kwijlebal | Little Droolball | the village puppy |
| Kleine Wiebel / Wiebel | Little Wobble / Wobble | your little brother or sister, at the dock |
| Buurvrouw Mandje | Mrs. Basket | the neighbor at the dock (a dog's basket) |

### Sniff Island: lines and titles that stand in more than one chunk

| Dutch | English | note |
|---|---|---|
| Hier mag je pas door als Snuffelneus | You May Only Pass Here as a Sniff Nose | quest title; the line itself: *You may only pass here as a Sniff Nose* |
| Neus omlaag, staart omhoog | Nose Down, Tail Up | quest title; the motto: *Nose down, tail up* |
| Het eiland loopt niet weg | The island isn't going anywhere | the captain |
| Nee ik wil even niet snuffelen, njeg | No, I'd rather not sniff right now, nyeg | the Guhstation's second button |
| Druk op start | Press start | |
| Opslaan en naar huis / Verder spelen | Save and go home / Keep playing | the memory card's menu |
| Ik ga papa achterna! / Hijs de zeilen! | I'm going after Papa! / Hoist the sails! | answer buttons |
| Wordt vervolgd, njeg! | To be continued, nyeg! | |
| Aangespoeld | Washed Ashore | scene and quest title; the step: *Washed ashore* |
| Wie doet dat toch? | Who Keeps Doing That? | scene and quest title |
| Een spoor van papa | A Trace of Papa | scene and quest title; the step: *A trace of Papa* |
| Welke hond ben jij? | Which Dog Are You? | quest title; the screen heading: *Which dog are you?* |
| Snuffelles / Goede daden / Het snuffelexamen | Sniffing Lessons / Good Deeds / The Sniffing Exam | quest titles; the steps: *Sniffing lessons*, *Good deeds*, *The sniffing exam* |
| Er rommelt iets | Something's rattling | step |
| Het lantaarnfeest / De overtocht | The Lantern Feast / The Crossing | scene titles |
| Naar het Snuffeleiland / Weer thuis / Naar huis | To Sniff Island / Home Again / Homeward | scene titles |
| Het boompje groeit | The Little Tree Grows | scene title |

### Added during the review of c63 - c67 (rules and words the translators settled)

**Something that is *kwijt* is never "lost".** The villagers have *mislaid* their things, a thing *is gone* or *is
missing*, somebody *can't find* it: *Ik ben mijn Guhstation kwijt* = *My Guhstation is gone*, *Ik ben de weg kwijt* = *I
can't find the way*, *Een hond raakt hem nooit kwijt* = *A dog never misplaces it*. A storm *settles down* (it does not
"die down"), *Jij leeft nog!* = *You're okay!*

**A fever is hot, not glowing.** *Wiebel gloeit* = *Wobble is burning up*, *gloeit als een kacheltje* = *is as hot as a
little stove*. The white wolf guh and the Sunfluff really *glow*.

**The Guhstation has a guh snoot** (*guh-snoet*, section 24), a dog has a *snout* (*snuit*) and *paws*; only the player's
own new *pootjes* are *pawsies*.

**A rank takes an article in a sentence**: *Je bent nu %s* = *You are now a %s*, *Je bent officieel Snuffelpup* = *You are
officially a Sniff Pup*. After a colon the rank line stands alone (*From now on you are officially: %s*).

**Master Trufflenose never contracts** (*I am*, *you will*, *that is*): it is how the schoolmaster sounds. His one fixed
exception is the line everybody uses, *There's no such thing as failing*.

**In a list a requirement keeps its tick-box form**: *Still needed: The Lord of the Nibble Ring, Super Guhrio and Beat the
Burnt Mika* (the same words as the lock quest's tick box).

**Small words.** *Plof* (the pup flomps to the floor) = *Flomp*; *Fwiet* = *Fweet*; *TINGELING* = *DING-A-LING*; *een tien
(met een griffel)* = *an A (with a gold star)*; *bolletjes* (the baker's) = *rolls*; *groeien als kool* = *growing like
weeds*; the pug's *beige* coat is *Fawn*; *voor de gezelligheid* = *for fun*; in running text the sauce is *cheese frying
sauce* (the item is *Cheese Frying Sauce*).

| Dutch | English | note |
|---|---|---|
| hartjesraam | heart window | the Cloud Chapel, the Snuggleheart's scene |
| prentenboek | picture book | 626-guh's, by the campfire |
| verkoold guhbeeld | charred guh statue | where the stranger with the pointy hat stands |
| Even pauze | Little Break | the memory card's menu |
| Kies je hond | Choose Your Dog | the choice screen's title |
| Alleen jij ziet het | Only You Can See It | quest title |
| Het hele dorp geholpen | The Whole Village Helped | quest title |
| Snuffelpup met diploma | Sniff Pup with a Diploma | quest title |
| Eeuwige pauze / Dorstige knollen | Endless Recess / Thirsty Turnips | quest titles |
| Een trui met één mouw / Beet of geen beet? | A Sweater with One Sleeve / To Bite or Not to Bite? | quest titles |
## 26. einde2 (the outer islands of the Guheinde: biomes, creatures, the big places, the Sponsguh cast)

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

## 27. einde2 phase B/C (the Guhvatar story, the Spirit World, travel, audio, tech tier 5) and the einde2 cast

The slices' own glossaries (`tools/lang/glossary_e2/<slice>.md`), folded in by the merge step (integratie_b). The cast and
Spirit World rows of phase A come first; they were not in section 26 yet. Where two slices named the same thing, it is
settled here, and the English chunks follow it:

| Dutch | English | settled |
|---|---|---|
| Leeuwschildpad-guh | Lion Turtle Guh | as bouw_vuur, bouw_zij2 and cast_schurk; never "Lion Turtle-guh" (h4_vuur's c95 changed) |
| Sjokkel | Shuckly | as section 24 (the Shuckle nod); never "Shuckle" (h3_aarde's c94 and h4_vuur's c95 changed) |
| Top der Voorgangers | Peak of the Predecessors | the place's own name (geestenwereld, c74); never "Summit" (h4_vuur's c95 changed) |
| Hei Bai-guh | Hei Bai Guh | "<name>-guh" = "<Name> Guh"; the lang already says Hei Bai Guh |
| Vuurkorf / Vadsontvanger | Fire Basket / Chonk Receiver | trap5_machines' names; trap5_ballon proposed the same |
| Luchtnon-guh | air nun guh | ("the" is the sentence's) |

### cast_team

| Dutch | English | note |
|---|---|---|
| Toph-guh | Toph Guh | contract 11.8 |
| Suki-guh | Suki Guh | contract 11.8 |
| Prins Zuka | Prince Zuka | contract 11.8 |
| Vuurheer Zuka | Fire Lord Zuka | contract 11.8 |
| Prins Zuka, verbannen | Banished Prince Zuka | contract 11.9 (the look ZUKA_BALLING) |
| De Blauwe Geest | The Blue Spirit | contract 11.9 (the look ZUKA_BLAUWE_GEEST), as the show |
| Yue-guh, de Maangeest | Yue Guh, the Moon Spirit | the look YUE_GUH_MAAN |
| MIJN EER! | MY HONOR! | Zuka's catchphrase (contract 11.8) |
| veerman-guh | Ferry Guh | contract 11.8 |
| scheepsbouwer-guh | Shipwright Guh | contract 11.8 |
| kroegbaas | Innkeeper | contract 11.8 |
| oude kapitein-guh | Old Captain Guh | contract 11.8 |
| racemeester-guh | Race Master Guh | contract 11.8 |
| vuurstuurderen | firebending | as aardestuurders = earthbenders |
| krijgster van Kyoshi | Kyoshi Warrior | |
| Noordelijke Waterstam | Northern Water Tribe | |
| knabbelsap | nibble juice | |
| Jip jip! | Yip yip! | the show's line |

### cast_volk

The masters, rulers and townsfolk of the Guhvatar story (CONTRACT_E2 11.5). Names that the bouw slices already fixed (now in
GLOSSARY.md) are used word for word; these are the new ones.
| Dutch | English | Notes |
|---|---|---|
| monnik Guhatso | Monk Guhatso | |
| Piandao-guh | Piandao Guh | |
| Guhvatar Roku-guh / Kyoshi-guh | Guhvatar Roku Guh / Guhvatar Kyoshi Guh | the spirits who give advice |
| Beeld van Roku-guh / Kyoshi-guh | Statue of Roku Guh / Statue of Kyoshi Guh | |
| Omroeper van de Aardrommel | Earth Rumble Announcer | |
| schuimbekkende fan-guh | Foaming Fan Guh | |
| Dorpsoudste van Kyoshi | Kyoshi Village Elder | |
| Kyoshi-krijgsterguh | Kyoshi Warrior Guh | |
| Waterstam-guh | Water Tribe Guh | |
| Poortwachter-guh | Gate Guard Guh | |
| Burger van Ba Sing Guh / Omashguh / de Vuurnatie | Citizen of Ba Sing Guh / Citizen of Omashguh / Fire Nation Citizen | |
| Dorpsguh van Kyoshi | Kyoshi Villager | |
| Gevangene | Prisoner | |
| Zonnekrijger-guh | Sun Warrior Guh | |
| Vuurwijze / Vuurwijze Shyu | Fire Sage / Fire Sage Shyu | |
| de juf / leerling | The Teacher / Student | |
| Toneelspeler (Aang-guh) ... | Ember Island Player (Aang Guh) ... | the troupe of Sintelstrand, one name per costume |
| De Blinde Bandiet | The Blind Bandit | as GLOSSARY.md |
| De Kei-guh voelt zich... verscheurd. | The Boulder Guh feels... conflicted. | the show's line |
| MIJN EER! | MY HONOR! | contract 11.8 (the stage Zuka) |

### cast_schurk

The names of contract 3.5 / 11.8 and of the bouw glossaries are used word for word; new here:

| Dutch | English | note |
|---|---|---|
| Feniks-koning Mikai | Phoenix King Mikai | Vuurheer Mikai with the power of the knabbelkomeet (the show's Phoenix King) |
| Vadsige Mikai | Chonky Mikai | Vuurheer Mikai after the finale: round, content, a nibble in his paws |
| de gevangenisdirecteur | the Warden | of Het Kokend Rotsblok = The Boiling Rock |
| de spijtige Mika-kok | the Sorry Mika Cook | as the vesting glossary |
| de vesting-kapitein | the Fortress Captain | as the vesting glossary |
| Vuurnatie-Mika | Fire Nation Mika | soldiers, guards, marines |
| duwstok | shoving stick | a soldier's spear: it only shoves |
| knabbelkomeet | nibble comet | Sozin's comet |
| vuurvlokken | fire flakes | the show's snack |
| Draakguh (Ran, Shaw) | Dragon Guh (Ran, Shaw) | Ran is red, Shaw is blue |
| Beer Bosco | Bosco the Bear | "Gewoon een beer." = "Just a bear." |
| Hij Die Tienduizend Dingen Weet | He Who Knows Ten Thousand Things | Wan Shi Guh's title |
| Vosguh | Fox Guh | the knowledge seekers (kenniszoeker = Knowledge Seeker) |
| Leeuwschildpad-guh | Lion Turtle Guh | |
| Luchtbizon-guh | Sky Bison Guh | |
| Maanvis Tui / Oceaanvis La | Tui the Moon Fish / La the Ocean Fish | |

### geestenwereld

| Dutch | English | note |
|---|---|---|
| Geestenwereld | Spirit World | the dimension (Avatar's Spirit World) |
| Meditatiecirkel | Meditation Circle | the arrival zone |
| Lantaarnboom | Lantern Tree | |
| lantaarngeest / lantaarngeesten | lantern spirit / lantern spirits | |
| Gefluisterwoud | Whispering Wood | |
| Hei Bai-guh | Hei Bai Guh | the forest spirit; stays |
| schaduwgeest | shadow spirit | |
| Koh's Grot / Koh de Gezichtenrover | Koh's Cave / Koh the Face Stealer | |
| Witte Lotus / Witte Lotus-ruines | White Lotus / White Lotus Ruins | |
| Hal van de Voorgangers | Hall of the Predecessors | |
| Top der Voorgangers | Peak of the Predecessors | |
| Hemelvijver | Sky Pond | Tui and La circle above it |
| Theepaviljoen / Theegeest | Tea Pavilion / Tea Spirit | |
| Fang's Hemelpad | Fang's Sky Path | Fang, Roku-guh's spirit dragon |
| Hemeltrap | Sky Stair | the spiral up to the Peak |
| gedenksteen / meditatiesteen | memorial stone / meditation stone | |
| geestengras / geestenaarde / geestenhout / schaduwhout | spirit grass / spirit soil / spirit wood / shadow wood | |
| gloeibladeren / schaduwbladeren | glow leaves / shadow leaves | |
| maansteen | moonstone | |
| nevelwater | mist water | falls up |
| lotusblad / lotuspuzzeltegel | lotus leaf / lotus puzzle tile | |
| geestenmasker | spirit mask | |
| geestpaddenstoel | spirit mushroom | |
| Raava en Vaatu? Nooit van gehoord. | Raava and Vaatu? Never heard of them. | the fresco sign (Legend of Korra wink) |
| (geest_bouw, phase B) | | |
| Trap der meesters | Stair of Masters | the road of five statues to the White Lotus |
| heiligdom / Lotusarchief | sanctuary / Lotus Archive | |
| de oude leden / de stichter | the old members / the founder | of the White Lotus |
| Gang der Gezichten | Passage of Faces | Koh's corridor |
| Geestenklok | Spirit Bell | |
| theetuin / roddelclub | tea garden / gossip club | |
| het mistige moeras | the Misty Swamp | |
| schrijntje / zwevend raadsel | little shrine / floating riddle | |
| Bumi-guh, de gekke koning | Bumi Guh, the Mad King | |
| Jeong Jeong-guh, de weggelopen meester | Jeong Jeong Guh, the Runaway Master | |
| Oom Miroh, de Grote Lotus | Uncle Miroh, the Grand Lotus | |
| Denk als een gekke oude koning | Think like a mad old king | Bumi's wing sign (DESIGN's line) |
| Het ijs onthoudt alles | The ice remembers all | Pakguh's wing sign |
| Vuur is een rivier, niet een fakkel | Fire is a river, not a torch | Jeong Jeong's wing sign (own words) |

### geestmodellen

The names of contract 3.5 / 11.8 and of the other einde2 glossaries are used word for word; new here:

| Dutch | English | note |
|---|---|---|
| Koh de Gezichtenrover | Koh the Face Stealer | the show's Koh the Face Stealer |
| Hei Bai-guh | Hei Bai Guh | the forest spirit of the show; the little panda and the angry giant are one name |
| Fang | Fang | Roku's dragon in the show; unchanged |
| Geest-koi | Spirit Koi | Tui and La as spirits of the Hemelvijver |
| Tui, de maangeest / La, de oceaangeest | Tui, the Moon Spirit / La, the Ocean Spirit | the two looks of the Spirit Koi |
| Libelkonijnguh | Dragonfly Bunny Guh | the show's dragonfly bunny spirits |
| Lantaarngeest | Lantern Spirit | a floating paper lantern with a face |
| Schaduwgeest | Shadow Spirit | dark spirit that only pushes |
| de Theegeest | the Tea Spirit | the spirit guh of the tea pavilion |
| de Bestelguh | the Order Guh | tech tier 5: the guh at the order counter (Bestelbalie) who runs the crafting orders |
| Geestenwereld | Spirit World | the dimension of the Guhvatar story |
| theepaviljoen | tea pavilion | |

Phase B (geest_modellen):

| Dutch | English | note |
|---|---|---|
| Kennisvosje / gouden kennisvosje | Knowledge Fox / golden Knowledge Fox | spirit fox with guh ears that leads to a secret |
| Mistreiger | Mist Heron | small white heron on the roofs of the White Lotus |
| Oude Reigerguh | Old Heron Guh | the huge crane spirit round the stone lotus |
| Wan-guh | Wan Guh | the hooded figure, "the first" |
| Geestreus | Spirit Giant | the far-away silhouettes of the geestentij |
| Kijkmasker | Watching Mask | a mask of Koh's cave that turns to you |
| Echo van ... | Echo of ... | the spirit echoes of the White Lotus masters (Echo of King Bumi Guh, Echo of Master Pakguh, Echo of Piandao Guh, Echo of Jeong Jeong Guh) |
| Droom van Oom Miroh | Dream of Uncle Miroh | Miroh still lives: his echo is a dream |
| Stichter / Lid van de Witte Lotus | Founder / Member of the White Lotus | |
| Guru Pathik-guh | Guru Pathik Guh | |
| Kuruk-guh, Yangchen-guh, Szeto-guh | Kuruk Guh, Yangchen Guh, Szeto Guh | earlier Guhvatars as spirits |
| ui-banaansap | onion-banana juice | Guru Pathik's drink |
| Gefluisterwoud / Hemelvijver / Hemelpad | Whispering Wood / Sky Pond / Sky Path | as used in the Guhdex pages (check against geest_bouw's glossary) |

### audio

| Dutch | English | note |
|---|---|---|
| Muziekboekje van het Guheinde / De Muziek van het Guheinde | Music Booklet of the Guheinde / The Music of the Guheinde | the credits book (a written book) |
| Miroh's wiegenlied | Miroh's lullaby | own song (moment.lu_ten); never "Leaves from the Vine" |
| Geestenklok | Spirit Bell | the bell of the Geestenwereld (sound geest.klok) |
| geestentij | spirit tide | sound geest.geestentij |
| Dag van de Zwarte Zon | Day of Black Sun | as the show |
| Guhvatar-staat | Guhvatar State | as "Avatar State" |
| luchtvloot | airship fleet | |

### reizen

| Dutch | English | note |
|---|---|---|
| De grote zingende | The Great Singing One | the whale questline (line `leegtewalvis`) and its first scene |
| Opa Walvis | Grandpa Whale | what the pub calls the old captain |
| Witte Knabbel | White Nibble | the captain's old quest (a Moby Dick wink) |
| knabbelsap | nibble juice | what the pub pours |
| Hoorn van de kapitein | The Captain's Horn | item `einde2_reizen_walvishoorn` |
| walviskist | whale chest | a chest on the back of an own whale |
| de rug van je walvis | your whale's back | the furniture screen |
| Zuka's schip | Zuka's Ship | entity `einde2_reizen_zuka_schip` |
| noodvisje | emergency fish | Sokka's |
| Bizonbert | Bisonbert | Sokka's name for a sky bison of his own |
| fluitlijstje | whistle list | Appa's list of places |
| gast / gastheer | guest / host | co-op: who travels along on someone's Appa |
| jonkie | youngster | the captain's word for the player |

### zij

| Dutch | English | note |
|---|---|---|
| vadskunst / de vier blijvende vadskunsten | chonk art / the four lasting chonk arts | the bending powers that stay after the story; one key |
| elementenwiel | Element Wheel | hold the key |
| Luchtsprong / Over water lopen / Rotszuil (bukken: rotsmuur) / Vuurschicht | Air Leap / Water Walk / Rock Pillar (sneak: Rock Wall) / Fire Bolt | the four lasting powers (fix4; the block: Gestuurde rots = Earthbent Rock) |
| luchtsturen / watersturen / aardesturen / vuursturen | airbending / waterbending / earthbending / firebending | as the show |
| Kristalsnoep-ring | Crystal Candy Ring | Omashguh's reward |
| koning Kristalsnoep | King Crystal Candy | a wrong answer of Bumi's riddle |
| De Kei-kolos / Het Kristalbeest | The Boulder Colossus / The Crystal Beast | Bumi's fake opponents |
| luchtnon-guh | air nun guh | (cast_volk) |
| Schub van de Leeuwschildpad | Scale of the Lion Turtle | catches you above the void |
| Spijtsoep | Sorry Soup | the Mika cook's recipe |
| de spijtige Mika-kok | the Sorry Mika Cook | (contract 11.8) |
| vesting-kapitein | Fortress Captain | (contract 11.8) |
| Kyoshi-schmink | Kyoshi Makeup | guh clothes (EYES) |
| Kraam van een Ender-Mika | Stall of an Ender Mika | the trade screen |
| Vestingbevrijder | Fortress Liberator | title |
| Ringenracer | Ring Racer | title |
| Knabbelberg | Nibble Mountain | the main island of the Guh End (the Opper-Mika's) |
| De Roze Stem / De Gouden Stem | The Pink Voice / The Golden Voice | the two voices of the poem |
| De drie proeven van Omashguh | The Three Trials of Omashguh | line |
| De stille tempel | The Silent Temple | line |
| Het vuur van Roku-guh | The Fire of Roku Guh | line |
| De waaiers van Kyoshi | The Fans of Kyoshi | line |
| Kennis voor kennis | Knowledge for Knowledge | line |
| De zegen van de Leeuwschildpad | The Blessing of the Lion Turtle | line |
| Een eigen vetbootje | A Little Grease Boat of Your Own | line |
| Zijpaden van de Guhvatar | Side Paths of the Guhvatar | FTB section |
| De Leegtehaven en de Mika-vesting | The Void Harbor and the Mika Fortress | FTB section |
| De dag dat de zon even ging vadsen | The Day the Sun Went for a Chonk | a book (the eclipse) |
| BUK / SPRING / DRAAI | DUCK / JUMP / TURN | the calls of the fan dance |

### h1_lucht

| Dutch | English | note |
|---|---|---|
| De Guhvatar: Lucht | The Guhvatar: Air | the questline guhvatar_lucht, also its FTB section and the clothes source |
| Hoofdstuk 1: Lucht | Chapter 1: Air | the narrator card |
| De reis van de Guhvatar | The Journey of the Guhvatar | the travel map of the whole story (Reiskaart "guhvatar") |
| kaasbol (de gloeiende) | (the glowing) cheese ball | as cast_team: "went for a chonk in a cheese ball" |
| Luchtscooter (speelgoed) | Air Scooter | the toy block for guhs (as the Air Scooter Track) |
| Luchtnomadengewaad | Air Nomad Robe | clothes GUHVATAR_LUCHTNOMADE |
| luchtsturen | airbending | as the show |
| Luchtbal (kracht, bal) | Air Ball | the story power and the ball of the airball match ("luchtbal" the game = airball) |
| Luchtsprong | Air Leap | the second story power |
| lemurguhtje | lemur guh | the Bat-Lemur Guh of the chase before it is Momo Guh |
| grillspiespoeder | grill skewer powder | the item is Grill Skewer Powder (c01) |
| steiger (van de tempel) | jetty | as bouw_zij2: steiger = Jetty |
| "Eindelijk, de Guhvatar" | "Finally, the Guhvatar" | Zuka's line at the jetty (a paraphrase of the show's "the Avatar") |
| "Je heet toch niet Korra?" | "Your name isn't Korra, is it?" | the Korra joke of DESIGN 5 |
| Water. Aarde. Vuur. Lucht. | Water. Earth. Fire. Air. | the opening film; its next two lines are the DESIGN's approved parody (vadsigheid = chonkiness, trek kreeg = got peckish) |

### h2_water

The names of contract 3.5 / 11.8 and of the other einde2 glossaries (bouw_water, cast_*, geestenwereld, geestmodellen, appa,
zwevers) are used word for word; new here:

| Dutch | English | note |
|---|---|---|
| De Noordelijke Waterstam (hoofdstuk 2) | The Northern Water Tribe (chapter 2) | the line guhvatar_water |
| De Blauwe Geest | The Blue Spirit | Zuka in his mask; he never speaks |
| Blauwe Geest-masker | Blue Spirit Mask | clothes (EYES) |
| Waterstam-parka | Water Tribe Parka | clothes (BODY) |
| Boemerang | Boomerang | Sokka Guh's; always comes back |
| Waterzweep / Watergolf / IJsbrug / Waterstroom | Water Whip / Water Wave / Ice Bridge / Water Current | the four story powers of chapter 2 |
| otter-surfen | otter surfing | the sled run |
| Glijkansel | Slide Pulpit | the top of the sled run (cf. Golfkansel = Wave Pulpit) |
| Slee-otter | Sled Otter | the otter-penguin guh you ride |
| Gondel | Gondola | |
| maanvis / maangeest | moon fish / Moon Spirit | Tui |
| Yue-guh, de maangeest | Yue Guh, the Moon Spirit | YUE_GUH_MAAN |
| Hemelvijver | Sky Pond | (geestenwereld glossary) |
| GEFELICIVADS! | CONGRACHONKULATIONS! | the banner of the surprise party (gefeliciteerd + vads) |
| JOOSTVADSJE EN SUUSVADSJE | JOOSTCHONKY AND SUUSCHONKY | the party's (wrong) guests of honour |
| Weer verkeerd afgevadst. | Wrong chonk-turn again. | Baltoguh's wink line |
| Iedereen | Everyone | a speaker in the party scene |
| plaksnorren | stick-on moustaches | (bouw_water glossary) |

### h3_aarde

| Dutch | English | note |
|---|---|---|
| Hoofdstuk 3: Ba Sing Guh | Chapter 3: Ba Sing Guh | the questline guhvatar_aarde |
| aardesturen | earthbending | as bouw_aarde and the show |
| De Blinde Bandiet / De Kei-guh / de Aardrommel | The Blind Bandit / The Boulder Guh / the Earth Rumble | as bouw_aarde |
| aardemunt | earth coin | the six stones of the blindfold course |
| oefenbaan / blinddoekbaan | training course / blindfold course | |
| Pai Sho (Guh Pai Sho) / lotus / tegel | Pai Sho (Guh Pai Sho) / lotus / tile | the simple guh version (12a.4) |
| Pai Sho-tafel | Pai Sho Table | reward block |
| Theeset van Oom Miroh | Uncle Miroh's Tea Set | reward block |
| Kraam van de koolman | Cabbage Man's Stall | reward block; "MIJN KOLEN!" = "MY CABBAGES!" |
| Aarderijk-hoed | Earth Kingdom Hat | guh clothes |
| Riem van De Blinde Bandiet | The Blind Bandit's Belt | guh clothes |
| Appa's verloren dagen | Appa's Lost Days | the scene (the show's episode title) |
| Een lied voor Lu Ten | A Song for Lu Ten | the scene; Miroh's own lullaby (own words, no line of the show's song) |
| De val van Ba Sing Guh | The Fall of Ba Sing Guh | the scene |
| Guhvatar-staat | Guhvatar State | the show's "Avatar State" |
| geestenwater | spirit water | |
| kristalcatacomben | crystal catacombs | as bouw_aarde |
| Er is geen knabbeltekort in Ba Sing Guh | There is no nibble shortage in Ba Sing Guh | as bouw_aarde |
| een guh van het huis | a guh of the house | the AI opponent at a Pai Sho table |
| jip jip | yip yip | as the appa slice |

### h4_vuur

| Dutch | English | note |
|---|---|---|
| Hoofdstuk 4: Vuur | Chapter 4: Fire | the questline guhvatar_vuur |
| De komeet van Mikai | Mikai's Comet | the questline guhvatar_finale (the show: Sozin's Comet) |
| knabbelkomeet | nibble comet | |
| Guhvatar-staat | Guhvatar State | the show: Avatar State |
| Top der Voorgangers | Peak of the Predecessors | in the Spirit World |
| Leeuwschildpad-guh | Lion Turtle Guh | as bouw_vuur and cast_schurk |
| luchtvloot / luchtschip | airship fleet / airship | |
| Vuurnatie-vermomming | Fire Nation Disguise | guh clothes and the player's look in chapter 4 |
| Zuka's litteken | Zuka's Scar | guh clothes |
| Guhvatar-standbeeld | Guhvatar Statue | the block guhvatar_beeld |
| Vuurhoofd | Hothead | Toph's nickname for Zuka (the show: Sparky) |
| Vuurheerin | Fire Lady | Azula's wish |
| Zonnekrijgers | Sun Warriors | as the show |
| luchtsprong / aardesprong | air jump / earth jump | the duel's own moves |
| Vuurstoot | Fire Blast | the entity einde2_h4_vuur_vuurbal |
| De Jasmijn Draak | the Jasmine Dragon | Miroh's tea house |
| Sjokkel | Shuckly | wink 1 (section 24's name) |
| 626-guh | 626-guh | wink 4, never translated |
| njeg | nyeg | the guh word, as everywhere |

### geest_spel

| Dutch | English | note |
|---|---|---|
| Pai Sho-tegel van Bumi-guh (Pakguh, Piandao-guh, Jeong Jeong-guh, Oom Miroh) | Bumi Guh's (Pakguh's, Piandao Guh's, Jeong Jeong Guh's, Uncle Miroh's) Pai Sho Tile | the five master tiles |
| Witte Lotus-tegel | White Lotus Tile | as bouw_vuur / deco |
| de Oude Reiger(guh) | the Old Heron | the giant crane spirit of the White Lotus |
| Lotusarchief | Lotus Archive | |
| "Uit het Lotusarchief" | "From the Lotus Archive" | the written book |
| Wen-Li-guh, Sao-guh, Mei-guh, Pao-guh | Wen-Li Guh, Sao Guh, Mei Guh, Pao Guh | the founder and three old members (invented) |
| De Vadsige Lotus | The Chonky Lotus | Pao-guh's Pai Sho opening |
| Kanna-guh | Kanna Guh | Pakku's lost love, guh-ified |
| de Valgeest / Tunnelgeest / Kluizenaarsgeest | the Falling Spirit / Tunnel Spirit / Hermit Spirit | Miroh's three tea guests |
| Knabbelkoor-eikel | Nibble Chorus Acorn | |
| Masker van Koh | Koh's Mask | cosmetic head item |
| Veer van de witte reiger / Gouden boekrol / Schubje van Koh / Maandruppel | White Heron Feather / Golden Scroll / Koh's Scale / Moon Drop | keepsakes of the rare sightings |
| De eerste knabbel | The First Nibble | Wan-guh's keepsake |
| Mediteerkussen | Meditation Cushion | the block for free visits |
| kennisvosje | knowledge fox | as geest_modellen |
| Guhvatar-gloed | Guhvatar glow | |
| chakra-pad / chakra-steen | Chakra Path / chakra stone | |
| maannacht / geestentij | moon night / spirit tide | |
| spiegelvijver | mirror pond | |
| Geestenklok | Spirit Bell | |
| het achtste eiland | the eighth island | |
| Wan-guh, de eerste | Wan Guh, the first | |
| Zeldzame verschijningen | Rare Sightings | |

### trap5_machines

| Dutch | English | note |
|---|---|---|
| Vadszender / Vadsontvanger | Chonk Transmitter / Chonk Receiver | one net in the rule; one jump only |
| Guhterminal | Guh Terminal | a block, never a tablet |
| Bestelbalie / Bestelguh | Order Counter / Order Guh | the guh with the apron; *bonnetje* = *receipt* |
| Knabbelpoort-buis | Nibble Gate Tube | *maatje* (its linked partner) = *buddy* |
| Thuispoort | Home Gate | *toelaten / weigeren* = *let in / keep out*; *lading* = *charge* |
| Enderguhrad | Enderguh Wheel | source, a Wahoog Enderguh flies in it |
| Komeetkern / scherf van de knabbelkomeet | Comet Core / shard of the nibble comet | the strongest source *so far* (never "the strongest") |
| Luchtblazer / Waterdrager / Korstduwer / Vuurkorf | Air Blower / Water Bearer / Crust Pusher / Fire Basket | the four element machines |
| Bouwplan van de Werktuigguh | Mechanist Guh's Blueprint | the recipe card of tier 5 (stays in the grid) |
| Diploma van de Werktuigguh | Mechanist Guh's Diploma | the keepsake at the end of the line |
| oefenopstelling | practice setup | the six bays in the workshop |
| De werkplaats gaat open | The Workshop Opens | the door scene |

### trap5_ballon

| Dutch | English | note |
|---|---|---|
| Vredesballon | Peace Balloon | bouw_lucht's name; the tier-5 big project, never "the last" or "final" |
| Vredesballon-beeldje | Peace Balloon Statuette | the reward block `guhs:vredesballon` |
| Vredesbrenger | Peacebringer | the title |
| de Werktuigguh | the Mechanist Guh | as bouw_lucht / cast_volk |
| de bouwplaats | the building site | the walled yard behind the workshop |
| het doek / de mand / de brander / de knabbelvoorraad / de versiering | the cloth / the basket / the burner / the nibble supply / the decorations | the five stages |
| tentdoek | tent canvas | campingmarkt's blocks ("Red Tent Canvas") |
| doosschil | box shell | mikas' item |
| Vuurkorf | Fire Basket | trap5_machines' element machine: trap5_machines' name |
| Vadsontvanger | Chonk Receiver | trap5_machines' receiver: trap5_machines' name |
| een zakje knabbels | a little bag of nibbles | the daily gift after the story |
| Wordt vervolgd... | To Be Continued... | the tier-6 picture at the end of Guh Technology (FTB_SLOT of tech_quests) |
| Trap 6 | Tier 6 | the open end |
| oortjes | little ears | the Mechanist's droop when he sees a balloon |

## 28. einde2 Sponsguh story (contract C: the slices sp_cast .. sp_zij, first merge merge_sp1)

The slices' own glossaries (`tools/lang/glossary_e2/sp_<slice>.md`), folded in by the merge step (merge_sp1). sp_cast has no
own file (its names are section 26's `sponsguh_wezens`). sp_d1_stad and sp_d2_weg follow with the next merge. Settled here:

| Dutch | English | settled |
|---|---|---|
| Werknemer van de Week | Employee of the Week | as "Werknemer van de maand" (section 23); d1_start's "Medewerker van de Week" now says it too (Dutch only, the English was the same) |
| De Verteller | The Narrator | one narrator in every slice; his French bits ("mes amis", "mon ami", "allez", "voilà", "vous voyez", "Et voilà") stay French in English |
| Zoete Knabbelzege | Sweet Nibble Victory | sp_audio and sp_d1_recept agree; never "Sweet Victory" |
| Ik ben een Guhfy Guhber | I'm a Guhfy Guhber | sp_audio and sp_d2_finale agree (DESIGN's own title) |
| rustpunt | rest point | the sneak (d1_recept) and the stand-still game (d2_heen) alike |

### sp_audio

| Dutch | English | note |
|---|---|---|
| Zoete Knabbelzege | Sweet Nibble Victory | own song title of part 1's finale (moment.zoete_zege); never "Sweet Victory" |
| Ik ben een Guhfy Guhber | I'm a Guhfy Guhber | own song title and refrain of part 2's finale (DESIGN's own); the verses differ from the film's song |
| Het lied van de loopgraaf | The Song of the Trench | the monsters' own sweet waltz (moment.monsters) |
| emmertjesmars | little bucket march | deuntje.emmers |
| de band speelt een beat | the band plays a beat | deuntje.trommel (the drum game, 112 BPM) |
| njeg / NJEG | nyeg / NYEG | as everywhere |

### sp_d1_start

| Dutch | English | note |
|---|---|---|
| bubbelbrief | Bubble Letter | the soap bubble with the job ad that starts part 1 |
| knabbelbakker | nibble cook | the job at the grill (fry cook) |
| De Verteller | The Narrator | the narrator with the French touch ("mon ami", "allez" stay French) |
| bonnetje | order ticket | over the grill |
| zeesla | sea lettuce | a part of the Nibble Patty |
| Werknemer van de Week | Employee of the Week | Sponsguh's pride (settled above) |

### sp_d1_recept

| Dutch | English | note |
|---|---|---|
| De Verteller | The Narrator | the narrator with a French touch; "mes amis" stays French in both languages |
| Professor Knabbelkloon | Professor Nibbleclone | wink 6 (the Knabbelkloon Kind of mewtwo) |
| Zoete Knabbelzege | Sweet Nibble Victory | the own title of the part-1 song (CONTRACT_E2_C 6.2) |
| Plan Z | Plan Z | |
| "Bedankt Karen, mijn computervrouw." | "Thank you Karen, my computer wife." | DESIGN's required line (plan_z) |
| schat (Karen tegen Plankmika) | honey | |
| rustpunt | rest point | the sneak: where you start again when Karen sees you |
| vetballetjes | grease balls | The Grease Bucket's menu |
| sluipheld | sneaking hero | |
| njeg | nyeg | as everywhere |

### sp_d2_heen

| Dutch | English | note |
|---|---|---|
| De Krokante Knabbel 2 | The Krusty Nibble 2 | Krabguh's second restaurant ("door de muur") |
| Werknemer van de Week | Employee of the Week | Spongeguh's 312 times in a row |
| Guhfy Guhber Knabbeltoren | Guhfy Guhber Nibble Tower | the towering sundae |
| zeg.. hoort hier geen poppetje bij? | Say.. isn't there supposed to be a little toy with this? | THE line of the user (CONTRACT_E2_C 5.3): verbatim in Dutch, this English fixed by the contract |
| kroonkussentje | crown cushion | the empty cushion Plankmika leaves on Krabguh's desk |
| koude straf | cold punishment | Neptuguh freezes Krabguh |
| voorlopig rijbewijs | provisional licence | Spongeguh's boating exam of part 1 |
| baby'tje | baby | "Wie was het baby'tje?" = "Who was the baby?" |
| het stilstaan-spel | the stand-still game | the minigame in The Thug Tug |
| rustpunt | rest point | a checkpoint of the stand-still game |
| sleuteltje | little key | the key of the Patty Wagon |
| mes amis / voilà | mes amis / voilà | the narrator's French touch: kept in English too |

### sp_d2_finale

| Dutch | English | note |
|---|---|---|
| bellenblazer | bubble blaster | the item einde2_sp_d2_finale_bellenblazer (the block bellenblaas = Bubble Blower) |
| emmerhelm | bucket helmet | Plankmika's mind-control buckets |
| Ik ben een Guhfy Guhber | I'm a Guhfy Guhber | the part-2 rock song (DESIGN's own title) |
| Guhfy Guhber (titel) | Guhfy Guhber | the title of part 2 |
| zeepbelzon | bubble sun | Neptuguh's deadline ("before the bubble sun sets"); no real timer |
| stoet | parade | the freed guhs walking with you |
| De Verteller | The Narrator | French touch: "Et voilà, njeg" stays "Et voilà, nyeg" |
| Hoofdstraat / Bellenlaan | High Street / Bubble Lane | the streets of Bikinibodem |
| stadspoort | town gate | |
| steiger | jetty | David Hasselguh's landing |
| manager | manager | Sponsguh's new job |

### sp_zij

| Dutch | English | note |
|---|---|---|
| Bikinibodem: zijdingen | Bikini Bottom: Side Quests | FTB section and clothes source |
| boomkoepel (van Sandy-guh) | tree dome | the show's word |
| luchtsluis | airlock | |
| luchthelm | air helmet | Sandy's Air Helmet (clothes) |
| karatemat | karate mat | |
| neuskegel / straalpijp / antenne | nose cone / exhaust pipe / antenna | the three rocket parts |
| de spookkapitein | the Ghost Captain | of The Flying Guhchman |
| aanlegplaats | mooring | (bouw_bikinibodem) |
| De Nacht van de Hakkende Hakker | The Night of the Chopping Chopper | the ghost captain's scary story (own words; it was onions) |
| spooklantaarn | Ghost Lantern | (kern) |
| onzichtbare bootmobiel | invisible boatmobile | |
| heldenriem | hero belt | Zeemeerman's belt |
| heldenlunch | heroes' lunch | |
| heldencape | Hero Cape | (kern) |
| jonge held | young hero | how the heroes call you |
| De Verteller | The Narrator | the narrator with the French touch ("mes amis", "vous voyez") |
