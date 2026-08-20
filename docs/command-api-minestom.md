# Minestom CommandAPI (Kotlin port) — compatibility and usage guide

This document describes `dev.slne.minestom.lobby.api.command.commandapi` and its subpackages: a
Kotlin-native command builder API, substantially translated from
[CommandAPI 12.0.0](https://github.com/CommandAPI/CommandAPI) (MIT license, Copyright (c) 2020–2022
Jorel Ali; the full text ships at `META-INF/LICENSES/CommandAPI-LICENSE.txt` in the API artifact),
compiled onto a Mojang Brigadier `CommandDispatcher` that owns parsing, dispatch and completion. It
targets pinned Minestom `2026.07.22-26.2` and Brigadier `1.3.10`.

The API lives entirely under `dev.slne.minestom.lobby.api.command.commandapi` and its subpackages
(`argument`, `dsl`, `executor`, `suggestion`, `exception`). It depends on no Bukkit, Paper, NMS or
`dev.jorel.commandapi` types. Brigadier is part of the public surface: every argument exposes the
`com.mojang.brigadier.arguments.ArgumentType` that reads it, and a failing parse raises Brigadier's
own `CommandSyntaxException`.

## 0. How a command is dispatched

Minestom's own registry never holds a CommandAPI command. Registration puts the command into this
API's Brigadier dispatcher and claims its names in `MinestomCommandOwnership`; `CommandManagerMixin`
then makes `CommandManager` answer for both registries, so every caller — chat, the console, and
Minestom itself — reaches a CommandAPI command through the manager it already uses:

| Hook                         | Method                             | Behaviour                                                                                                                                                                                                                             |
|------------------------------|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `CommandManagerMixin`        | `commandExists`                    | Reports an owned name as taken, so registering a Minestom command that shadows one is rejected.                                                                                                                                       |
| `CommandManagerMixin`        | `execute`                          | Dispatches an owned line through Brigadier and reports a rejection the way vanilla does. A foreign line is parsed by Minestom as before. Injected after `PlayerCommandEvent`, which therefore still sees and may rewrite the command. |
| `CommandManagerMixin`        | `createDeclareCommandsPacket`      | Merges this API's commands into the tree Minestom built for its own (`DeclareCommandsMerger`).                                                                                                                                        |
| `MinestomSuggestionListener` | incoming `ClientTabCompletePacket` | Cancels an owned request and answers it from `CommandDispatcher.getCompletionSuggestions`, keeping Brigadier's own replacement range. Tab completion never reaches the manager, so it stays a packet listener.                        |

The mixin calls into `CommandAPIHook`, which answers as if no CommandAPI existed while no platform
is installed. The manager's unknown command callback — which Minestom leaves unset — is filled in
with vanilla's report, so a line no registry resolves is answered rather than silently dropped.

The merged tree is protocol data only: each argument node carries the vanilla parser id, properties
and suggestion type for its kind (`NodeDeclarations`), so a client validates and completes it
exactly as it would a vanilla command, while the server parses with the argument's own Brigadier
type. A node the sender may not use is left out of the merge entirely.

## 1. Quick start

### Fluent builder

```kotlin
import dev.slne.minestom.lobby.api.command.commandapi.CommandAPICommand
import dev.slne.minestom.lobby.api.command.commandapi.dsl.greedyStringArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.replaceSuggestionsAsync
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutorSuspend

CommandAPICommand("message")
    .withPermission("lobby.command.message")
    .playerArgument("target") {
        replaceSuggestionsAsync { info -> playerDirectory.search(info.currentArg) }
    }
    .greedyStringArgument("message")
    .playerExecutorSuspend { player, args ->
        val target: Player by args
        val message: String by args
        target.sendMessage(message)
    }
    .register()
```

`register()` compiles the command definition and installs it in the dispatcher immediately; there is
no separate "build" step.

### DSL entry point (auto-registering)

```kotlin
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandAPICommand

commandAPICommand("spawn") {
    executesPlayer { player, _ -> player.teleport(spawnPosition) }
}
```

`executesPlayer` here is the `CommandExecutable` **member** (see §4) — it needs no `dsl` import at
all, since it is already visible on the `CommandAPICommand` receiver inside the block.

`commandAPICommand(name) { ... }` and `commandTree(name) { ... }` build the definition and call
`.register()` on it before returning (see
`dev.slne.minestom.lobby.api.command.commandapi.dsl.CommandDsl`).
`subcommand(name) { ... }` builds a `CommandAPICommand` **without** registering it — it is meant to
be attached via `withSubcommand`/`withSubcommands` on a parent that will itself be registered.

### `CommandTree`

`CommandTree` implements `CommandExecutable`, so an executor attached directly to the tree runs when
the command is invoked with no arguments. Branch executors are attached to a node reached through
`then(...)` or an argument-scoped DSL block, and a tree may carry both:

```kotlin
import dev.slne.minestom.lobby.api.command.commandapi.CommandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.literalArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.integerArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor

CommandTree("gamemode")
    .executes { sender, _ -> showMode(sender) }
    .literalArgument("survival") {
        anyExecutor { sender, _ -> setMode(sender, GameMode.SURVIVAL) }
    }
    .integerArgument("level", min = 0, max = 3) {
        anyExecutor { sender, args -> setPermissionLevel(sender, args.get<Int>("level")) }
    }
    .register()
```

Input that matches no branch is rejected rather than falling through to the root executor:
`/gamemode nonsense` reports `Unknown or incomplete command. See below for error`, and trailing data
after a complete syntax (`/gamemode survival extra`) reports `Incorrect argument for command`. Both
are sent in vanilla's format — the red message followed by a grey context line ending in `<--[HERE]`
— and neither runs any executor. See [§9](#9-syntax-errors).

### Delegated (child) arguments

Every argument builder exists on three receiver families: `CommandAPICommand`, `CommandTree`, and
any `Argument<*>` (for building a child chain). All three accept the same `nodeName`, type-specific
parameters, an `optional` flag, and a trailing configuration block:

```kotlin
import dev.slne.minestom.lobby.api.command.commandapi.argument.StringArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.integerArgument

StringArgument("scoreboard")
    .integerArgument("value", min = 0) { /* configure the child Argument<Int> */ }
```

## 2. Registration and lifecycle

- `commandAPICommand`/`commandTree` register immediately; `subcommand` does not (see above).
- `CommandAPI.register(...)` requires an installed platform; calling it before the server has
  started throws `IllegalStateException("The CommandAPI platform has not been installed; register
  commands after server startup")` (`CommandAPI.kt`).
- On the server, `MinestomCommandAPIService` installs the backend (`MinestomCommandAPIPlatform`) as
  one of `ServerLifecycle`'s ordered services, **before** the service that registers the lobby's own
  commands and before chat services start (`ServerLifecycle.orderedLobbyServices`: `... , players, commandApi, commands, chat,
  chatFormat`). Plugin/lobby command registration always happens after the CommandAPI backend is
  live.
- `CommandAPICommand.register(namespace: String? = null)` and `CommandTree.register(namespace)`
  accept an optional namespace; when given, the command is additionally registered under
  `namespace:name` (and `namespace:alias` for every alias). Namespaces must match
  `[a-z0-9_.-]+`.
- `CommandAPI.unregister(name)` detaches a previously registered command and returns whether
  anything was removed; it is safe to call for a name that isn't registered by this platform
  instance.
- Registering a name or alias that collides with an already-registered command — this platform's, or
  one registered directly with Minestom — throws `IllegalStateException("Command name '...' is
  already registered")` rather than shadowing it.
- **An optional argument cannot precede a required argument within one registered path.**
  Whichever order arguments are chained in (fluent, `CommandTree`, or nested `Argument<*>`),
  registration walks every executable path and throws
  `CommandValidationException("Required arguments cannot follow optional arguments")` if a later
  argument on that path is required while an earlier one is optional.

## 3. Permissions and requirements

Permissions (`withPermission`) and requirements (`withRequirement { sender -> Boolean }`) can be
attached at the command, subcommand, and argument level; they accumulate down the executed path. A
sender must satisfy **all** accumulated permissions and **all** accumulated requirements for a
syntax to run (`MinestomConditions.canUse`):

- `net.minestom.server.command.ConsoleSender` always passes every permission check — console
  bypasses permissions entirely, but requirements still apply.
- `dev.slne.minestom.lobby.api.player.LobbyPlayer` senders are checked with
  `sender.hasPermission(permission)` for every accumulated permission.
- Any other `CommandSender` implementation only passes if the accumulated permission set is empty
  for that path.

```kotlin
CommandAPICommand("kick")
    .withPermission("lobby.command.kick")
    .withRequirement { sender -> sender !is Player || sender.gameMode != GameMode.SPECTATOR }
    .playerArgument("target")
    .executesPlayer { sender, args -> /* ... */ }
    .register()
```

Accumulated conditions become the `requires` predicate of the node they were declared on
(`AccumulatedConditions`/`MinestomConditions.canUse`), which Brigadier consults for dispatch,
completion, and the tree it sends the client alike. A sender who could not execute a syntax
therefore never sees it: no suggestions for it, and no node for it in the command tree.

## 4. Executors

`executes`, `executesPlayer`, and `executesConsole` are Unit-returning members of
`CommandExecutable<SELF>`, so they exist on `CommandAPICommand`, `CommandTree`, and `Argument<T>`.
On `CommandAPICommand` and `CommandTree` they bind the no-argument invocation of the command itself.
Every other executor shape is a
`dsl.ExecutorDsl` extension function with its own name, because overloading `executes` for both
`Unit` and `Int` return types on a SAM-typed parameter compiles but silently binds a bare lambda to
the `Unit` overload (discarding a returned result code), and plain function-type overloads don't
compile at all (`@JvmName` is illegal on interface default methods). The full set:

| Family                             | Names                                                                                                                               |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Unit, plain args                   | `executes` / `executesPlayer` / `executesConsole` (members) — DSL equivalents: `anyExecutor` / `playerExecutor` / `consoleExecutor` |
| Unit, with `ExecutionInfo`         | `anyExecutionInfo` / `playerExecutionInfo` / `consoleExecutionInfo`                                                                 |
| `Int` result, plain args           | `anyResultingExecutor` / `playerResultingExecutor` / `consoleResultingExecutor`                                                     |
| `Int` result, with `ExecutionInfo` | `anyResultingExecutionInfo` / `playerResultingExecutionInfo` / `consoleResultingExecutionInfo`                                      |
| Suspend, plain args                | `anyExecutorSuspend` / `playerExecutorSuspend` / `consoleExecutorSuspend`                                                           |
| Suspend, with `ExecutionInfo`      | `anyExecutionInfoExecutorSuspend` / `playerExecutionInfoExecutorSuspend` / `consoleExecutionInfoExecutorSuspend`                    |

There is deliberately no `executes` overload that returns a result, and no suspend variant that
returns an `Int` result code. `ExecutionInfo<S>` carries `sender: S`, `args: CommandArguments`, and
the raw `input: String`.

A resulting executor's `Int` is the dispatch result: `CommandAPI.execute(sender, input)` returns it
directly. A Unit executor counts as `1`, and an execution that is turned away — no permission for
the path, or no executor for this sender type — counts as `0`. `execute` throws Brigadier's
`CommandSyntaxException` when the input does not parse; `CommandAPIHook` catches that and reports it
(see [§9](#9-syntax-errors)), so a caller that must not throw should go through `CommandManager` or
handle the exception itself.

Reading parsed arguments inside an executor uses `CommandArguments`
(`dev.slne.minestom.lobby.api.command.commandapi.executor.CommandArguments`):

```kotlin
.anyExecutor { sender, args ->
    val amount: Int = args.get("amount")           // throws if absent/wrong type
    val note: String? = args.getOptional("note")   // null if absent
    val target: Player by args                     // delegated read by property name
    val raw: String? = args.getRaw("amount")        // the exact typed-in token
}
```

`CommandArguments.get`/`getOptional` are `inline reified` — another reason this API is Kotlin-first
(see §8).

### Literals hold no argument name

`LiteralArgument` takes part in parsing and in the client-facing command tree, but it occupies no
slot in `CommandArguments`. `args.getRaw("<literal>")` is `null`, `"<literal>" in args` is `false`,
and positional access skips it, so in

```kotlin
CommandAPICommand("inspect")
    .withArguments(LiteralArgument("action", "run"), StringArgument("value"))
```

`args[0]` is the `value` argument. Every other argument kind carries a value,
`MultiLiteralArgument` included — use it when the chosen literal has to be readable.

Because a literal holds no argument name, a literal may share its name with a real argument on the
same path:

```kotlin
commandTree("send") {
    literalArgument("server") {
        stringArgument("server") { anyExecutor { _, args -> args.get<String>("server") } }
    }
}
```

A literal becomes a Brigadier literal node rather than an argument node, so the two never compete
for the same name and nothing is renamed. Two value-carrying arguments sharing a name on one path
remain a registration error.

### Suspend executors and coroutines

Each `*ExecutorSuspend`/`*ExecutionInfoExecutorSuspend` function accepts an optional `scope`
parameter (a `CoroutineScope` or a `() -> CoroutineScope`) that **defaults to
`dev.slne.minestom.lobby.api.coroutine.minestomAsyncScope`** — a scope built on
`Dispatchers.Default` with its own `SupervisorJob`, so one executor's failure never cancels another
(`MinestomScopeProvider`/`ExecutorDsl.kt`). The Brigadier command itself always runs synchronously
on whatever thread invoked it; a `Suspending` executor definition is *launched* into its scope
(`launchCommandExecutor`/`SuspendExecutors.kt`) and returns control immediately — it never blocks
the dispatch call, and the suspend body never runs on Minestom's packet/tick thread unless the
caller explicitly supplies such a scope.

Failures thrown from inside a suspend executor are handled uniformly (`handleCommandFailure`): a
`CommandSyntaxException` (or a `WrapperCommandSyntaxException`
wrapping one) sends its component to the sender; any other exception is logged and the sender
receives a fixed generic failure message; `CancellationException` is always rethrown rather than
swallowed.

There is no fixed backend timeout on suspend executor or suggestion work. If a provider needs a
deadline, wrap its own body in `kotlinx.coroutines.withTimeout`/`withTimeoutOrNull` — the backend
does not impose one for you.

## 5. Suggestions

`Argument<T>` exposes four suggestion-mode setters: `replaceSuggestions`, `includeSuggestions`,
`replaceSafeSuggestions`, `includeSafeSuggestions`. "Replace" discards the argument's built-in
completions; "include" merges the provider's entries with the built-ins. The `*Safe` variants take a
`SafeSuggestions<T>` of typed values (rendered through the argument's own `stringify`); the non-safe
variants take an `ArgumentSuggestions` of raw strings (optionally with tooltips).

Both `ArgumentSuggestions` and `SafeSuggestions<T>` are `suspend fun interface`s
(`dev.slne.minestom.lobby.api.command.commandapi.suggestion`) — every provider is invoked fresh, per
tab-completion request, as a genuine suspend call. There is no `CompletableFuture`-based suggestion
type and no cache/refresh wrapper anywhere in this API: upstream CommandAPI's future-backed
"refreshing" suggestion factories are represented here simply as suspend lambdas that run again on
every keystroke.

```kotlin
import dev.slne.minestom.lobby.api.command.commandapi.dsl.replaceSuggestionsAsync
import dev.slne.minestom.lobby.api.command.commandapi.dsl.replaceSafeSuggestionsAsync

PlayerArgument("target")
    .replaceSuggestionsAsync { info -> playerDirectory.search(info.currentArg) }

IntegerArgument("amount")
    .replaceSafeSuggestionsAsync { info -> presetAmounts(info.previousArgs) }
```

### Suggestions with tooltips

A suggestion entry can carry a tooltip `Component`, shown by the client alongside the completion
text. Raw-string providers carry it as `StringTooltip(suggestion, tooltip)`; the safe/typed
providers carry it as `Tooltip(suggestion, tooltip)` (rendered to text through the argument's own
`stringify`, same as the non-tooltip safe variants). Every suggestion-mode combination (`replace`/
`include`, sync/async, raw-string/safe-typed) has a `WithTooltips`/`WithTooltipsAsync`
counterpart in `dsl.SuggestionDsl` — none of these twelve names collide with an `Argument<T>`
member (unlike the four plain-provider overloads in the next section), so a bare trailing lambda
works directly with no explicit function type needed:

```kotlin
import dev.slne.minestom.lobby.api.command.commandapi.dsl.replaceSuggestionsWithTooltipsAsync
import dev.slne.minestom.lobby.api.command.commandapi.suggestion.StringTooltip
import net.kyori.adventure.text.Component

StringArgument("mode")
    .replaceSuggestionsWithTooltipsAsync { info ->
        listOf(
            StringTooltip("build", Component.text("Switch to the building toolset")),
            StringTooltip("play", Component.text("Switch to play mode")),
        )
    }
```

The sync forms (`replaceSuggestionsWithTooltips`/`includeSuggestionsWithTooltips`) accept either a
`vararg StringTooltip` or the same `(SuggestionInfo) -> Collection<StringTooltip>` provider shape,
and `includeSuggestionsWithTooltipsAsync` mirrors the example above but merges with the argument's
built-ins instead of replacing them. The safe/typed family —
`replaceSafeSuggestionsWithTooltips`/`includeSafeSuggestionsWithTooltips` and their `*Async` forms —
follow the same shape with `Tooltip<T>` in place of `StringTooltip`, for suggesting typed values
(not just strings) with a tooltip attached.

### The four overloads that need an explicitly typed argument

`replaceSuggestions`, `includeSuggestions`, `replaceSafeSuggestions`, and `includeSafeSuggestions`
each have a `dsl.SuggestionDsl` extension taking a plain (non-suspend)
`(SuggestionInfo) -> Collection<String>` (or `Collection<T>` for the safe variants) — but they share
their name with an `Argument<T>` **member** of the same arity, and because
`ArgumentSuggestions`/`SafeSuggestions<T>` are `fun interface`s, a bare trailing lambda at the call
site resolves to the member, never the DSL overload, regardless of whether the DSL extension is
imported into scope. Because the member's single abstract method returns `List<StringTooltip>`
(or `List<Tooltip<T>>` for the safe variants), a lambda written for the simpler DSL shape fails to
compile against the member with a return-type mismatch — it does not silently pick either overload
and run:

```kotlin
// error: Return type mismatch: expected 'List<StringTooltip>', actual 'List<String>'.
// The bare lambda resolves to the Argument.replaceSuggestions(ArgumentSuggestions) member, whose
// suggest() must return List<StringTooltip>, not List<String> — this does not compile.
StringArgument("mode").replaceSuggestions { info -> listOf("build", "play") }
```

Import the `dsl` overload explicitly and give the lambda its function type up front to select it
instead of the member:

```kotlin
import dev.slne.minestom.lobby.api.command.commandapi.dsl.replaceSuggestions

val provider: (SuggestionInfo) -> Collection<String> = { info -> listOf("build", "play") }
StringArgument("mode").replaceSuggestions(provider)
```

The `*Async` (`replaceSuggestionsAsync`, `includeSuggestionsAsync`, `replaceSafeSuggestionsAsync`,
`includeSafeSuggestionsAsync`) and vararg-value overloads have no same-arity member to collide with,
so they take a bare lambda (or values) directly — the collision is specific to the plain,
non-suspend `(SuggestionInfo) -> Collection<...>` provider shape.

`SuggestionsBranch` (`dev.slne.minestom.lobby.api.command.commandapi.suggestion.SuggestionsBranch`)
lets a provider depend on which of several prior branches matched, for commands whose suggestion set
differs by an earlier literal/argument choice; `replaceSuggestions(branch)` /
`includeSuggestions(branch)` adapt a `SuggestionsBranch` into an `ArgumentSuggestions`.

### Suggestion-mode support is not uniform

`MinestomCommandCompiler.validateSuggestionModes` rejects a non-built-in suggestion mode at
**registration time** (throwing `CommandValidationException`) for exactly these argument kinds:

| Argument kind                                                                         | Rejected modes                                                            | Why                                                                                                                                                                                                                                                                                                                             |
|---------------------------------------------------------------------------------------|---------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `LiteralArgument`                                                                     | any non-built-in                                                          | A literal is a literal node, not an argument node, and a literal node has no suggestion type to point at the server. `MultiLiteralArgument` and `EnumArgument` are argument nodes and **do** accept every mode.                                                                                                                 |
| `CommandArgument`                                                                     | any non-built-in                                                          | It keeps the client's own redirect node for a nested command, whose completions come from the tree that node redirects to.                                                                                                                                                                                                      |
| `PositionArgument`, `Position2DArgument`, `BlockPositionArgument`, `RotationArgument` | any non-built-in                                                          | One value of these spans several space-separated tokens, and a client splits a completion request on spaces, so a provider attached to one would be asked to complete a fragment of its own value. `AngleArgument` and `AxisArgument` are single-token and are **not** restricted, despite sitting in the same argument family. |
| `EntityTypeArgument`                                                                  | `Include`/`IncludeSafe` only (`Replace`/`ReplaceSafe` and built-ins work) | It advertises the client's own `SUMMONABLE_ENTITIES` completion set, which the server cannot enumerate to merge extra entries into: neither `EntityType`, `RegistryData.EntityEntry`, nor the pinned `data:26.2-rv3` `entity_type.json` exposes the summonable-entity predicate.                                                |

Every other argument kind accepts all four suggestion modes.

### Where completions come from

A client only asks the server to complete a node whose declaration says `minecraft:ask_server`;
otherwise it fills the node in itself, from the vanilla parser the node was declared with. So
`NodeDeclarations` marks a node `ask_server` in exactly two cases: it carries a provider, or its
kind is one the client could not complete on its own. Everything else — a boolean, a number, a game
mode, a team color, a biome or an enchantment — is left to the client, which already has the data
and needs no round trip.

The kinds answered by the server under `BuiltIns`, and what their Brigadier type offers:

- `SoundArgument`, `PotionEffectArgument`, `BiomeArgument`, `EnchantmentArgument`,
  `ResourceArgument<T>` — every key currently registered in the relevant registry, re-read per
  request so a late registration is offered.
- `InstanceArgument` — every live instance's UUID, plus each instance's dimension name where that
  name is unique among currently registered instances (an ambiguous name would not resolve, so it is
  not offered).
- `PlayerArgument`/`PlayersArgument`/`EntityArgument`/`EntitiesArgument` — the online usernames,
  then the selectors the argument would accept: `@e` only for the entity kinds, and only the
  single-target selectors (`@p`/`@r`/`@s`) when the argument accepts one target.

  | Builder | Online usernames | Selectors |
    |---|---|---|
  | `playerArgument` | yes | `@p` `@r` `@s` |
  | `playersArgument` | yes | `@p` `@r` `@a` `@s` |
  | `entityArgument` | yes | `@p` `@r` `@s` |
  | `entitiesArgument` | yes | `@p` `@r` `@a` `@e` `@s` |

- `MultiLiteralArgument`, `EnumArgument` — their fixed set of accepted spellings. These are one
  argument node declared as a plain string, not one literal node per spelling, so the client has
  nothing to complete them from. (`GameModeArgument` and `TeamColorArgument` also match a fixed set,
  but their nodes name the vanilla `gamemode`/`team_color` parser, which the client completes from
  the same values — so those stay client-side.)

A `CustomArgument` or `ListArgument` follows whatever its base or element kind does, and a list
completes the element being typed rather than the whole value: the entries replace the text after
the last delimiter.

Entries are matched against what has already been typed, case-insensitively, before being sent — for
a provider, unless it declares `SuggestionFilter.NONE` (`ArgumentSuggestions.unfiltered`). Parsing a
fixed set stays case-sensitive either way, matching vanilla; only the completion offer is lenient,
since that just saves typing.

### Cancellation and timing

A completion request is resolved through `CommandDispatcher.getCompletionSuggestions`, and a
provider that suspends is run in its own coroutine rather than on the netty thread. There is no
fixed backend timeout on suggestion resolution (see §4); a slow provider simply delays that
keystroke's response.

Typing a trailing space after a completed argument triggers a fresh request for whichever argument
comes next — completion is not limited to the argument currently being typed. Brigadier only offers
nodes the sender passes the `requires` check for, so completion is gated by the same accumulated
permissions/requirements as dispatch (see §3).

## 6. Argument reference

The following table lists every delivered argument: its DSL builder name (identical across the
`CommandAPICommand`, `CommandTree`, and `Argument<*>` receiver families), the Kotlin/Minestom type
returned to executors and suggestion providers, and the Brigadier `ArgumentType` that reads it
(`Argument.rawType`). A type named `com.mojang.brigadier.arguments.*` is Brigadier's own, delegated
to unchanged so that bounds and failure messages match vanilla exactly; every other name is a parser
in `...commandapi.argument.parser`, ported from the vanilla argument of the same name. What the
*client* is told a node is — which is a separate concern from how the server reads it — is listed
under [§0](#0-how-a-command-is-dispatched) and derived by `NodeDeclarations`.

### Primitives and text

| Builder                | Class                  | Result type                              | Brigadier type                                |
|------------------------|------------------------|------------------------------------------|-----------------------------------------------|
| `booleanArgument`      | `BooleanArgument`      | `Boolean`                                | `BoolArgumentType.bool()`                     |
| `integerArgument`      | `IntegerArgument`      | `Int`                                    | `IntegerArgumentType.integer(min, max)`       |
| `longArgument`         | `LongArgument`         | `Long`                                   | `LongArgumentType.longArg(min, max)`          |
| `floatArgument`        | `FloatArgument`        | `Float`                                  | `FloatArgumentType.floatArg(min, max)`        |
| `doubleArgument`       | `DoubleArgument`       | `Double`                                 | `DoubleArgumentType.doubleArg(min, max)`      |
| `stringArgument`       | `StringArgument`       | `String` (single word)                   | `StringArgumentType.word()`                   |
| `textArgument`         | `TextArgument`         | `String` (quoted phrase)                 | `StringArgumentType.string()`                 |
| `greedyStringArgument` | `GreedyStringArgument` | `String` (rest of input)                 | `StringArgumentType.greedyString()`           |
| `literalArgument`      | `LiteralArgument`      | none - see [§4](#4-executors)            | none; becomes a Brigadier literal node        |
| `multiLiteralArgument` | `MultiLiteralArgument` | `String` (one of the fixed literals)     | `FixedSetParser`                              |
| `commandArgument`      | `CommandArgument`      | `String` (the trailing sub-command text) | `StringArgumentType.greedyString()`           |
| `uuidArgument`         | `UUIDArgument`         | `java.util.UUID`                         | `UuidParser`                                  |
| `integerRangeArgument` | `IntegerRangeArgument` | `net.minestom.server.utils.Range.Int`    | `IntegerRangeParser`                          |
| `floatRangeArgument`   | `FloatRangeArgument`   | `net.minestom.server.utils.Range.Float`  | `FloatRangeParser`                            |
| `enumArgument`         | `EnumArgument<E>`      | `E : Enum<E>`                            | `FixedSetParser` over the formatted spellings |

`FixedSetParser` matches case-sensitively: each accepted spelling is the canonical form the argument
stringifies back to, so accepting other casings would let two spellings mean the same value. This is
what vanilla does — `/gamemode SURVIVAL` is a syntax error there too.

Constructor bounds default to the full range of the underlying numeric type (e.g. `IntegerArgument`
's `min`/`max` default to `Int.MIN_VALUE`/`Int.MAX_VALUE`); construction only rejects `min > max` or
(for `Float`/`Double`) a NaN bound — an infinite bound is accepted and not specially validated.

### Entity and player selectors

| Builder              | Class                | Result type                             | Brigadier type                                   |
|----------------------|----------------------|-----------------------------------------|--------------------------------------------------|
| `playerArgument`     | `PlayerArgument`     | `net.minestom.server.entity.Player`     | `EntitySelectorParser`, one result, players only |
| `playersArgument`    | `PlayersArgument`    | `List<Player>`                          | `EntitySelectorParser`, any number, players only |
| `entityArgument`     | `EntityArgument`     | `net.minestom.server.entity.Entity`     | `EntitySelectorParser`, one result               |
| `entitiesArgument`   | `EntitiesArgument`   | `List<Entity>`                          | `EntitySelectorParser`, any number               |
| `entityTypeArgument` | `EntityTypeArgument` | `net.minestom.server.entity.EntityType` | `RegistryParser` over `EntityType.fromKey`       |

`EntitySelectorParser` is a port of vanilla's selector grammar: `@a`, `@e`, `@p`, `@r` and `@s`
with their implied limits and player-only defaults, an optional bracketed `key=value` option list,
or a bare player name or UUID. A selector whose limit exceeds what the argument accepts is rejected
at **parse** time rather than truncated, as is a selector that could include a non-player entity on
a players-only argument — so `@a` on `playerArgument` fails with "Expected a single player" and
`@e` on `playersArgument` with "Only players can be targeted".

A selector that matches nothing is a parse failure too: "No entity matched the selector" for the
single-target kinds, "No entities matched the selector" for the multi-target kinds unless they were
built with `allowEmpty = true`. A bare UUID counts as targeting any entity, so `playerArgument`
rejects one; use the player's name.

Three option groups have their grammar consumed — so a malformed value is still reported at the
right place, and later options in the same bracket list still parse — but are then rejected as
unsupported, because Minestom models nothing to resolve them against: `scores`, `advancements` and
`predicate`. `nbt` is rejected outright, since reading it would need an SNBT parser this port does
not have.

### Position, rotation, and movement

| Builder                 | Class                   | Result type                               | Brigadier type                                      |
|-------------------------|-------------------------|-------------------------------------------|-----------------------------------------------------|
| `locationArgument`      | `PositionArgument`      | `net.minestom.server.coordinate.Vec` (3D) | `PositionArgumentType`, resolved against the sender |
| `location2DArgument`    | `Position2DArgument`    | `Vec` (X/Z only)                          | `PositionArgumentType`, two-dimensional             |
| `blockPositionArgument` | `BlockPositionArgument` | `Vec` (block-aligned)                     | `PositionArgumentType`, block-aligned               |
| `rotationArgument`      | `RotationArgument`      | `Rotation(yaw: Float, pitch: Float)`      | `RotationArgumentType`                              |
| `angleArgument`         | `AngleArgument`         | `Float`, wrapped into `[-180, 180)`       | `AngleParser`                                       |
| `axisArgument`          | `AxisArgument`          | `Set<Axis>` (`X`/`Y`/`Z`)                 | `AxisParser`                                        |

The three position kinds read vanilla's coordinate grammar: absolute values, `~` offsets from the
sender's position, and `^` local coordinates relative to the sender's facing (all three components
must agree on whether they are local). A block-aligned position rejects a decimal in an absolute
component, the way vanilla does, but accepts one in a `~` offset.

`rotationArgument` and `angleArgument` read `~` as an offset from the sender's own yaw and pitch. A
sender with no position — the console, for instance — counts as being at the origin facing yaw `0`,
so a relative coordinate resolves against that rather than failing.

### Native Minecraft values

| Builder                    | Class                      | Result type                             | Brigadier type                                              |
|----------------------------|----------------------------|-----------------------------------------|-------------------------------------------------------------|
| `resourceLocationArgument` | `ResourceLocationArgument` | `net.kyori.adventure.key.Key`           | `ResourceLocationParser`                                    |
| `timeArgument`             | `TimeArgument`             | `java.time.Duration`                    | `TimeParser` (vanilla's `50d`/`25s`/`75t`/bare-tick syntax) |
| `teamColorArgument`        | `TeamColorArgument`        | `net.minestom.server.color.TeamColor`   | `FixedSetParser`                                            |
| `particleArgument`         | `ParticleArgument`         | `net.minestom.server.particle.Particle` | `ParticleParser`                                            |
| `gameModeArgument`         | `GameModeArgument`         | `net.minestom.server.entity.GameMode`   | `FixedSetParser`                                            |

`ResourceLocationParser` defaults an unqualified path to the `minecraft` namespace and accepts only
the characters a key may contain, so `Stone` (uppercase) is rejected as an invalid identifier rather
than read as a value.

`timeArgument` counts in ticks: `d` is 24000 ticks, `s` is 20, `t` and a bare number are 1. `2d` is
therefore 40 real minutes, not two days.

`ParticleArgument` accepts only a bare particle key (e.g. `minecraft:flame`). A key naming a
particle that carries options (`dust`, `block`, `vibration`, and similar) is rejected as
**unsupported** rather than resolved to a default-valued instance, because this port has no syntax
for particle options; an unrecognized key is rejected as unknown.

### Registry-backed resources and instances

| Builder                | Class                  | Result type                               | Brigadier type                                                    |
|------------------------|------------------------|-------------------------------------------|-------------------------------------------------------------------|
| `soundArgument`        | `SoundArgument`        | `net.minestom.server.sound.SoundEvent`    | `RegistryParser` over `SoundEvent.fromKey`                        |
| `potionEffectArgument` | `PotionEffectArgument` | `net.minestom.server.potion.PotionEffect` | `RegistryParser` over `PotionEffect.fromKey`                      |
| `biomeArgument`        | `BiomeArgument`        | `RegistryKey<Biome>`                      | `RegistryParser` over the biome registry                          |
| `enchantmentArgument`  | `EnchantmentArgument`  | `RegistryKey<Enchantment>`                | `RegistryParser` over the enchantment registry                    |
| `resourceArgument`     | `ResourceArgument<T>`  | `RegistryKey<T>`                          | `RegistryParser` over a caller-supplied `Registry<T>`             |
| `instanceArgument`     | `InstanceArgument`     | `net.minestom.server.instance.Instance`   | `InstanceParser`; by UUID or by a currently-unique dimension name |

The client is told these are the vanilla `resource_location` (sound, potion effect) or `resource`
(biome, enchantment, generic) nodes, so it validates and completes them from its own registries;
`instanceArgument` has no vanilla counterpart and is declared as a plain word.

`SoundArgument` resolves only builtin/vanilla sounds: pinned `SoundEvent.fromKey` is documented to
"never return a custom/resource pack sound", so a resource-pack-only sound key will not parse
through this argument.

The wire identifier for the biome registry is **`minecraft:worldgen/biome`, not
`minecraft:biome`**: pinned `BuiltinRegistries.BIOME` is `RegistryKey.unsafeOf("worldgen/biome")`,
and the deprecated `WORLDGEN_BIOME` constant is only an alias for that same key — there is no
`minecraft:biome` registry to reference. The `minecraft:` prefix in both this and the
`enchantmentArgument` row above is not decorative: `RegistryKey.unsafeOf(String)` parses its raw
value through Adventure's `Key.key(String)`, which defaults the namespace to `minecraft` whenever
the input carries none, and `RegistryKey.name()` always renders through `Key.asString()`
(`namespace + ':' + value`) — so the identifier actually placed on the wire always carries an
explicit namespace, never the bare `"worldgen/biome"`/`"enchantment"` value the source passes in.

## 7. Custom and composite arguments

`CustomArgument<T, B>` wraps a base `Argument<B>` and reinterprets its parsed value:

```kotlin
import dev.slne.minestom.lobby.api.command.commandapi.argument.CustomArgument
import dev.slne.minestom.lobby.api.command.commandapi.CommandAPI

val userArgument = CustomArgument(StringArgument("user")) { info ->
    users[info.currentInput] ?: CommandAPI.failWithString("Unknown user ${info.currentInput}")
}
```

`info` (`CustomArgumentInfo<B>`) carries the `sender`, the exact `currentInput` text the base
consumed, and the base argument's already-converted `baseValue`. The parser never sees the
`StringReader` — it composes on top of an already-parsed value, so it cannot leave the cursor
somewhere the dispatcher does not expect.

Throwing this API's `CommandSyntaxException` (directly, or via
`CommandAPI.failWithString`/`failWithMessage`) fails that syntax with the given message, exactly
like a parse failure, with the cursor reset to where the value started so the client underlines the
value itself. A styled component survives that translation and is shown as written. Brigadier's own
`com.mojang.brigadier.exceptions.CommandSyntaxException` may be thrown instead and passes through
untouched, cursor included.

Two extension helpers cover the common cases without writing a `CustomArgument` by hand:

```kotlin
import dev.slne.minestom.lobby.api.command.commandapi.argument.map
import dev.slne.minestom.lobby.api.command.commandapi.argument.filter

IntegerArgument("amount").map { value -> value * 2 }
IntegerArgument("positive").filter { value -> value > 0 }
```

`ListArgument<T>` parses a delimiter-separated sequence of a single element `Argument<T>`:

```kotlin
import dev.slne.minestom.lobby.api.command.commandapi.argument.ListArgument

ListArgument("ids", UUIDArgument("id"), delimiter = ',')
```

A list reads the rest of the command line and splits it itself, so it must be the last argument on
its path. The element argument cannot itself consume remaining input (a greedy element is rejected
at construction with `IllegalArgumentException`), and the delimiter must be a visible,
non-whitespace character. With `allowEmpty = false` (the default), an empty element between two
delimiters — or a leading/trailing empty element — fails to parse.

## 8. Unsupported and out of scope

This port intentionally does not implement:

- **Arguments whose value is SNBT or a block/item state.** `ComponentArgument`, `NBTArgument`,
  `NBTCompoundArgument`, `ItemStackArgument` and `BlockStateArgument` are not part of this API:
  each needs a full SNBT or state-property parser to read its value, and there is no use for one in
  this project. Their vanilla wire parsers (`COMPONENT`, `NBT_TAG`, `NBT_COMPOUND_TAG`,
  `ITEM_STACK`, `BLOCK_STATE`) are therefore unreachable too.
- **Bukkit/Paper senders or types.** The API is built entirely on Minestom's own
  `net.minestom.server.command.CommandSender`/`Player`/`ConsoleSender`; there is no Bukkit
  `CommandSender`, Paper `Audience`-based sender, or NMS interop of any kind.
- **Offline player profiles.** `PlayerArgument`/`PlayersArgument`/`EntityArgument`/
  `EntitiesArgument` resolve only against entities Minestom currently has instantiated
  (`EntityFinder`); there is no argument that looks up a Mojang profile or an offline player by
  name/UUID.
- **Signed Bukkit chat.** Minestom has no equivalent of Bukkit's signed-chat message argument;
  vanilla's `MESSAGE` parser type (`ArgumentParserType.MESSAGE`) has no argument in this port.
- **Predicates, functions, loot tables, advancements, and scoreboard objects**, because pinned
  Minestom exposes no stable server-side model to bind an argument to. Concretely, this port ships
  no argument for these `ArgumentParserType` wire parsers: `BLOCK_PREDICATE`, `ITEM_PREDICATE`,
  `FUNCTION`, `LOOT_TABLE`, `LOOT_PREDICATE`, `LOOT_MODIFIER`, `OBJECTIVE`,
  `OBJECTIVE_CRITERIA`, `SCOREBOARD_SLOT`, `SCORE_HOLDER`, `TEAM`, `ITEM_SLOT`, `ITEM_SLOTS`,
  `GAME_PROFILE`, `ENTITY_ANCHOR`, `DIMENSION`, `NBT_PATH`, `STYLE`, `HEX_COLOR`,
  `RESOURCE_OR_TAG`, `RESOURCE_OR_TAG_KEY`, `RESOURCE_KEY`, `RESOURCE_SELECTOR`,
  `TEMPLATE_MIRROR`, `TEMPLATE_ROTATION`, `HEIGHTMAP`, and `DIALOG`.
- **Brigadier forks and redirects.** A command is declared through this API's builders, not by
  handing nodes to the dispatcher, so there is no node-forking or generic `redirect()`
  construction. `CommandArgument` declares the client's own redirect node for a nested command, and
  that is not something plugin code can attach to an arbitrary node. Brigadier's argument types and
  exceptions *are* part of the public surface (see [§7](#7-custom-and-composite-arguments)); the
  dispatcher itself is not.
- **Java interop as a design goal.** The public surface is Kotlin-first: `CommandArguments.get`/
  `getOptional` are `inline reified` generic functions, every argument builder is a Kotlin extension
  function with default parameters, and executors/suggestion providers are expressed as Kotlin
  function types (including `suspend` function types) — none of which have a clean, idiomatic Java
  call shape. There is no separate Java-compatible surface.

## 9. Syntax errors

Leftover input is always an error, so `/difficulty nonsense` and `/difficulty hard extra` are both
rejected rather than running the closest executor and discarding the rest. When the line cannot be
parsed, no executor runs and the sender receives vanilla's two-line failure:

```
Incorrect argument for command: Unknown value 'nonsense'
...fficulty nonsense<--[HERE]
```

The first line is red: the heading, then the parser's own message where there is one. The second is
grey, click-to-suggest the original command, shows at most the 10 characters preceding the error
(prefixed with `...` when more precede it), renders the unconsumed remainder red and underlined, and
ends with `<--[HERE]` in red italics.

Which heading appears follows vanilla: input whose command label was never resolved is
`command.unknown.command` ("Unknown or incomplete command. See below for error"); a failure after at
least one accepted token is `command.unknown.argument` ("Incorrect argument for command").

These are sent as translatable components carrying the vanilla keys, so a player's client renders
them in its own language. Senders without a client have no translator of their own, so
`CommandAPITranslations.register()` installs English fallbacks into the `GlobalTranslator` and
console output is rendered through it.

A failure raised by command *code* is not a syntax error: the input parsed, so the sender is sent
the executor's own message (or a generic failure, with the cause logged) and the command reports no
result rather than the line being underlined (`handleCommandExecutorFailure`).

## 10. Update provenance

- Public API translated or adapted from upstream CommandAPI 12.0.0 carries an MIT attribution header
  in its source file; the API artifact ships the corresponding license text at
  `META-INF/LICENSES/CommandAPI-LICENSE.txt`, and a provenance note at
  `META-INF/commandapi-port/NOTICE.md` naming the exact upstream revision
  (`740a093a7d111863067eb3f94a3041e69ade02fa`) and stating that only parsed-argument and
  syntax/validation-failure concepts were retained — no Bukkit, Paper, NMS, or CommandAPI runtime
  code is included.
- Compiled and verified against pinned Minestom `net.minestom:minestom:2026.07.22-26.2` and
  Brigadier `com.mojang:brigadier:1.3.10`.
- The parsers in `...commandapi.argument.parser` are ports of the vanilla argument of the same name,
  read from the decompiled sources under
  `surf-canvas/.gradle/caches/paperweight/upstreams/server-work/canvas`. Where a value has no
  vanilla counterpart (`InstanceArgument`) or no vanilla-expressible option syntax
  (`ParticleArgument`), the divergence is called out in [§6](#6-argument-reference).
- Known divergences from vanilla, beyond those already noted: a repeated invertable selector option
  (for example `type=!a,type=!a`) is rejected at the repeated value's start rather than the key's,
  so the client underlines one token less than vanilla would.
