# KhimairaCraft — v1 Readiness Audit

Audited: 2026-08-15, commit `69ae8e8` (all branches merged, no open PRs/issues).
Scope: all 173 Java files, all 42 resource/data JSON files, build config, git state.

**Verdict: not ready for v1.** The four implemented martial classes (Fighter, Barbarian,
Rogue, Ranger) are in genuinely good shape — registration, networking, persistence, and
event wiring are clean and thorough. But the repo ships several features that are broken
at runtime, two hollow classes, a large pool of do-nothing feats, and zero original art.
Below, in priority order.

---

## 1. Broken at runtime (fix or cut before launch)

These are confirmed bugs, verified against source:

1. **Phylactery XP withdrawal is dead for every classed player.**
   `VanillaXpSuppressionHandler.onXpChange` (`xp/VanillaXpSuppressionHandler.java:29-38`)
   cancels every positive `PlayerXpEvent.XpChange` for players with a class.
   `ArcanePhylacteryBlock.java:95` pays out stored XP via `giveExperiencePoints(...)`,
   which fires exactly that event → the payout is silently cancelled and the stored XP
   is lost. The glass-bottle path still works, which masks the bug in testing. The
   suppression handler needs a reentrancy guard (or the payout must bypass the event).

2. **The ores drop gold, the arcane table drops a vanilla enchanting table, the
   phylactery drops nothing.** `ModBlocks.java:22,34,38` use
   `Properties.ofFullCopy(...)`, which since 1.20.5 copies the **loot table key** of the
   source block. So `arcane_ore` uses `minecraft:blocks/gold_ore` and the hand-written
   loot tables in `data/khimairacraft/loot_table/blocks/` are never consulted;
   `arcane_enchanting_table` drops a vanilla enchanting table; and `arcane_phylactery`
   (fresh `Properties.of()`, no loot table file) drops nothing at all — breaking a
   tier-4 phylactery is total loss.

3. **The `khimairacraft:boss` tag never loads.**
   `data/khimairacraft/tags/entity_types/boss.json` uses the pre-1.21 plural folder;
   1.21 requires `tags/entity_type/`. Every other data folder in the repo already uses
   the singular convention — this one file was missed. Consequence:
   `SavingThrowSystem.java:31` and `MobXpRewardSystem.java:16` resolve an empty tag, so
   dragon/wither/warden get no boss save handling and no boss XP.

4. **The three "epic" enchantments do literally nothing and are unobtainable.**
   `sharpness_x.json`, `power_vi.json`, `protection_v.json` all have `"effects": {}`
   (zero gameplay effect), no vanilla enchantment-tag membership (not in any
   `in_enchanting_table`/loot/trade tag), and their key-holder class
   `enchanting/epic/ModEnchantments.java` is referenced by no other file. An entire
   advertised feature wired to nothing.

5. **The arcane enchanting table recipe collides with vanilla's.**
   `recipe/arcane_enchanting_table.json` (` B ` / `DOD` / `OOO`, book+diamond+obsidian)
   is shape-identical to the vanilla enchanting table recipe. Which one wins is
   registry-order luck. Needs a distinct pattern (e.g. swap in arcane dust).

6. **The phylactery tier recipes are scams.** `arcane_phylactery_tier2/3/4.json`
   consume a phylactery + 4 dusts and return a plain phylactery — tier is a blockstate
   property that a crafted item cannot carry. Real upgrading already happens in-world
   via `ArcanePhylacteryBlock.handleDustUpgrade`. Delete all three recipe files.

7. **The Human racial bonus feat can never be spent.**
   `SelectRacePayload.java:74` grants it and `SelectFeatPayload.java:90-95` can consume
   it server-side, but no code path ever constructs `FeatSelectionScreen(true)` — the
   level-up screen only prints static text "Racial Bonus Feat Available!"
   (`LevelUpScreen.java:466-469`) with no button. Verified: every `FeatSelectionScreen`
   construction in the repo passes `false` for the racial slot.

8. **`SyncPlayerDataPayload` NPE risk.** `SyncPlayerDataPayload.java:19` uses
   `buf.readNbt()` (nullable); a null tag flows into `DnDPlayerData.load(null)` on the
   client. Add a null guard.

---

## 2. Advertised but missing — decide: build, gate, or cut

1. **`mods.toml` promises "an endgame Tarrasque awaits." There is no Tarrasque.**
   No entity is registered anywhere in the mod (no `Registries.ENTITY_TYPE` usage at
   all). Remove the claim or it's a day-one broken promise.

2. **"New Spell Level Unlocked!" is shown to every leveling Wizard/Cleric**
   (`LevelUpScreen.java:314`, duplicated in layout at `:660-664`) — but there is no
   spell system anywhere: no slots, no casting, no spell registry. Cheapest
   trust-preserving fix in this document: delete the message.

3. **Wizard and Cleric are hollow classes.** Fully selectable, but: 3 abilities each
   (nothing new after level 7 of 20), no passives class, no combat handler, no
   level-up feature grants — versus Fighter's 10 abilities + passives + events.
   Either cut them from `ClassSelectionScreen.SELECTABLE_CLASSES` for v1 or accept
   shipping them visibly unfinished.

4. **Armor Class is display-only.** The AC formula exists in exactly one place — the
   client-side character sheet (`CharacterSheetScreen.java:136-137`). No combat code
   reads `getRacialAcBonus`/`getFeatAcBonus`/`getFeatShieldBonus`/
   `getNaturalArmorBonus`. Every AC-granting feat (Dodge, Mobility, Draconic
   Resilience, Mage Armor, etc.) is cosmetic. Related: there is no attack-roll/BAB
   combat mechanism at all — BAB exists only as a feat prerequisite — so ~19 feats
   promising to-hit effects (Power Attack included) cannot function.

5. **Prestige classes are 100% unreachable.**
   `PrestigeClass.meetsRequirements` (`classes/PrestigeClass.java:57-61`) is a stub
   returning `false` with zero callers, and `setPrestigeClass` has no packet, screen,
   or command that calls it. Yet prestige UI paths render in `LevelUpScreen`,
   `CharacterSheetScreen`, and `DnDHudOverlay`. Hide the UI for v1.

6. **Cleric turning economy doesn't exist.** `extraTurningCharges` and
   `turningLevelBonus` are written and serialized but never read; the ~20 "spend a
   turning attempt" feats in `CompleteDivineFeats.java` are all inert.

---

## 3. The feat pool is ~69% inert

413 feats are registered; roughly **128 have any gameplay effect**. ~285 are flag-only
(`<id>_unlocked` set, nothing reads it) — the content files openly say so
("Most feats are flag only — their active combat mechanics arrive in later passes":
`CompleteWarriorFeats.java:20`, `TomeOfBattleFeats.java:14`, `CompleteChampionFeats.java:17`,
`CompleteScoundrelFeats.java:16`). Feat slots are permanent, so a player can burn 20
levels of picks on nothing.

`FeatSourceConfig` defaults **all 11 sourcebooks on** (and its `isEnabled()` returns
`true` on any exception — `FeatSourceConfig.java:17,22-30`). The one-line v1 mitigation:
default the non-PHB sources to `false` and ship the ~150-feat PHB pool, most of which
is implemented.

Also inert despite being selectable:
- **Crippling Strike**, a Rogue-10 capstone option (`RogueSpecialAbilityScreen.java:32-33`):
  its stack count is incremented (`SelectRogueSpecialAbilityPayload.java:63`) and never
  read. The other five options all work.
- **Dwarf `poison_resistance`** and **Skeleton Warrior `bone_armor`** trait keys are
  declared in `RaceRegistry.java:14,47` but have no implementation in
  `RacialTraitHandler` (and bone_armor grants AC, which nothing reads anyway).
- Write-only, NBT-persisted fields: `eldritchLoreCasterBonus`, `devotionHealingBonusWis`,
  `smiteDamageBonus`, `extraTurningCharges`, `turningLevelBonus` — all written on
  level-up/grant, zero readers.

---

## 4. Missing art and player onboarding

- **Zero original textures in the repo** (`assets/khimairacraft` has no `textures/`
  directory). Existing models borrow vanilla textures — passable placeholder, except
  `deepslate_arcane_ore` uses the plain `minecraft:block/deepslate` texture and is
  literally invisible against deepslate, and `arcane_ore` uses the transparent
  `amethyst_cluster` texture on a full cube.
- **11 of 15 registered items have no model at all** and render as magenta/black
  checker cubes: all 10 arcane dusts (the mod's core progression currency and the bulk
  of the creative tab) plus `arcane_enchanting_table`, which also has **no blockstate
  and no block model** — the signature block is an untextured cube in-world.
- **No advancements directory at all** → none of the 15 recipes ever appear in the
  recipe book or pop an unlock toast. The entire dust chain
  (copper→…→nether star) must be learned outside the game.
- Creative tab icon is a vanilla diamond sword placeholder.
- All UI text is hardcoded `Component.literal` English — 122 literal calls across 44
  files vs 3 translatable ones. Fine if English-only is intentional; unlocalizable
  otherwise.

---

## 5. Dead code to delete

| What | Why |
|---|---|
| `client/LevelUpPlaceholderScreen.java` | Renders "Level Up Menu - Coming Soon"; zero references, superseded by real `LevelUpScreen` |
| `enchanting/ArcaneEnchantingHandler.java` | Zero references anywhere |
| `ability/AbilityBarState.java` | Zero references |
| `network/OpenAbilityBarPayload` | Registered `playToServer` with a no-op handler, never sent; a free unauthenticated packet path |
| `recipe/arcane_phylactery_tier2/3/4.json` | See §1.6 |
| `FEAT_AUDIT_EXPORT.txt` (repo root, 53 KB) | Generated working artifact; will rot out of sync with `feat/content/*`; also reproduces verbatim WotC feat text in one scrapeable file under an all-rights-reserved repo |
| `META-INF/accesstransformer.cfg` + its `mods.toml` declaration | Empty (one comment line); dead config |
| `RageSystem.isFatigued`, `BarbarianPassives.getIndomitableWillBonus`, `BarbarianPassives.hasTrapSense`, `PrestigeClass.unifiesPools` | Zero callers |

---

## 6. Performance and robustness (pre-launch hardening)

- **Full-player-NBT resync once per second per player**: `CombatEventHandler.java:61`
  and `StaminaSystem.java:100` push the entire ~82-key data attachment to sync a single
  regen integer. Wants a small delta payload before any multiplayer launch.
- **Unthrottled per-tick work**: `FeatEffectHandler.onPlayerTick` (`:116-155`, ~10
  attribute reconciliations + light-level lookups every tick), `RacialTraitHandler`
  sunlight check computes biome temperature *before* its throttle check (`:62-76` —
  move the modulo first), `FighterPassives.java:64-71` unthrottled (contrast
  `StaminaSystem.java:87` which does it right). `CombatEventHandler.java:69-88` does a
  20-block entity scan per player every 10 ticks just to expire cobwebs — store web
  positions instead.
- **Leak**: `GuildManager.guildHallTeleportCooldowns` (`GuildManager.java:29`) is never
  cleaned on logout nor reset on server start (unlike `PartyManager.reset()`). All
  other systems clean up on logout correctly.
- **Consistency**: `AbilityCooldownManager.java:10` and `RangerCombatHandler.java:76`
  use plain `HashMap` where every sibling system uses `ConcurrentHashMap`.
- **Wrong namespace**: `RageSystem.java:30` attribute modifier id uses `"dndmods"`
  instead of `khimairacraft`.
- **UI**: two competing ability-assignment UIs with different protocols
  (`AbilityAssignmentScreen` via Alt+V, whole-hotbar sync vs `AbilityManagerScreen`
  via inventory button, per-slot) — pick one. `AbilityBarOverlay.java:35-42` cancels
  the vanilla hotbar then bails for classless players, leaving them with no hotbar
  at all while holding V.
- `EnchantingTableHandler.java:23` branch is unreachable (`ArcaneEnchantingTableBlock`
  extends `Block`, not `EnchantingTableBlock`), and `:13` is the only
  `@EventBusSubscriber` in the repo missing `modid`.
- Evasion tooltip lies: says "50% chance to dodge", implementation is flat Resistance
  II + Speed II (`ability/impl/Evasion.java:14,22-23`). Works; wrong description.

---

## 7. Project hygiene / metadata

- **No README, no LICENSE, no CI, no docs, no changelog.** `mods.toml` says
  "All Rights Reserved" (the MDK default — confirm it's deliberate) with no backing
  LICENSE file, no `logoFile`, no `issueTrackerURL`. A repo that already runs Copilot
  reviews has nothing verifying the build compiles on push — a minimal GitHub Actions
  `./gradlew build` workflow is cheap and would have caught several items above.
- **Version range too open**: `minecraft = "[1.21,1.22)"` lets the mod load on
  1.21.2+/1.21.4+ where recipe/data formats changed and every recipe would fail to
  parse. Tighten to `[1.21,1.21.2)`.
- **Version defined in three places** (`gradle.properties`, `build.gradle`,
  `mods.toml`) with no `processResources` expansion — they will drift.
- **Branding drift**: creative tab and keybind category say "D&D Mods",
  `pack.mcmeta` says "DnD Mods resources", everything else says "KhimairaCraft".
- `pack.mcmeta` `pack_format: 34` is the resource-pack number; the jar also ships
  data (format 48). Tolerated by NeoForge, technically wrong for half the pack.

---

## 8. What's actually solid

Credit where due — none of these need work:

- All 17 network payloads registered with correct directions; server-bound handlers
  validate sender and re-validate client-supplied ability ids.
- Every `@SubscribeEvent` class is properly registered; no orphaned handlers.
- All registries (items, blocks, block entities, tabs, attachments, menus, loot
  modifiers) wired to the mod bus; all three commands registered.
- No client-class leakage into common code paths that run on a dedicated server.
- Player data: `.copyOnDeath()`, full NBT round-trip (zero write-only keys in the
  serializer), synced on login/respawn/dimension change; legacy-migration read paths
  guarded. Guilds persist via `SavedData` with indexes rebuilt on load. Parties are
  deliberately session-scoped and documented as such.
- All 29 ability classes registered — no orphans; racial abilities correctly override
  the class-gate methods.
- Logout cleanup is thorough across combat/stamina/rage/rogue/ranger/fighter systems
  (guild teleport cooldowns being the one exception).
- Every JSON file parses; recipes use correct 1.21 format with resolvable item ids;
  worldgen features, biome modifiers (correct NeoForge path), damage type, and GLM
  wiring are all valid. Lang file covers 100% of registry-derived keys.
- No TODO/FIXME markers, no printlns, no commented-out blocks — the incompleteness is
  structural, not sloppy.

---

## Suggested v1 cut line

**Must fix (broken):** §1 items 1–8.
**Must decide (cut or gate):** Wizard/Cleric selectability, spell-level message,
Tarrasque claim, prestige UI, feat pool filtering to implemented sources.
**Must have (shippable product):** item/block models for dusts + enchanting table,
distinguishable ore textures, recipe advancements, README + LICENSE + CI.
**Should fix:** §6 performance/robustness list, §5 dead-code deletions.

Note: this audit is static analysis; the sandbox could not run `./gradlew build`
(NeoForge maven blocked by proxy). Run a local build + a smoke test on a dedicated
server as final verification.
