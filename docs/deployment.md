# Deployment

The lobby server runs in Pterodactyl. Nothing is built at install time — CI
publishes the runnable server jar, and the egg's install script only downloads
it, so "Reinstall server" takes seconds.

## Server layout

`surf-minestom-lobby-server` is a surf Minestom server module
(`dev.slne.surf.api.gradle.minestom-server`), set up like surf-gecko and
surf-rift:

- The jar only contains the lobby's own code and the surf api modules. Minestom
  and every other third-party library are listed in `dependencies.txt` and
  downloaded into `libraries/` by the surf bootstrap agent on the first start.
- The surf api provides the command API, signed chat, LuckPerms, spark, the NPC
  library, player visibility, the console and the plugin loader. They are
  installed in `LobbyServerApplication` through `surfMinestomServer { }`; the
  lobby's own services (database, uploads, code of conduct, world, players) run
  as the `surf-minestom-lobby` feature inside it.
- The lobby's mixins are bootstrapped by `LobbyAgent`, which the surf bootstrap
  calls through `Surf-Delegate-Agent-Classes` once the libraries are on the
  class path. It also calls the LuckPerms agent.

## Plugins

Plugins (surf-core, surf-chat, surf-lobby, ...) are no longer bundled into the
jar. They are surf Minestom plugins (`dev.slne.surf.api.gradle.minestom`) and
are dropped into the server's `plugins/` directory, from where `withPlugins()`
loads them after every other feature — so the lobby world already exists when a
plugin is loaded. Data directories are created as `plugins/<plugin-id>`.

Plugins that the lobby's own code uses would have to be declared in
`surfMinestomServerApi { pluginDependencies { } }`; the lobby currently uses
none.

## Pipeline

`.github/workflows/publish-server.yml` runs on every push to `master` and via
`workflow_dispatch`. It builds `:surf-minestom-lobby-server` with
`--refresh-dependencies`, so `+` versions (the surf api and its Gradle plugin)
are re-checked instead of trusting Gradle's 24h dynamic-version cache, and then:

- publishes the server jar to `https://reposilite.slne.dev/private` under
  `dev.slne.minestom.lobby:surf-minestom-lobby-server`;
- tags the commit with `v<projectVersion>` and attaches the same jar as
  `surf-minestom-lobby-server.jar` to the GitHub release of that tag.

`projectVersion` in `gradle.properties` has to be bumped before another release
can be published; a tag that already points to another commit fails the run.

## Required setup

- Reposilite: a `private` repository, plus two access tokens — one with write
  access for CI, one read-only for the egg.
- GitHub Actions secrets, on the `production` environment or the repository:
  `REPOSILITE_PRIVATE_USER` / `REPOSILITE_PRIVATE_TOKEN` (the CI write token).
- Self-hosted runner with bash and git; JDK 25 comes from `setup-java`.
- Pterodactyl egg variables: `MAVEN_REPO_USER` / `MAVEN_REPO_TOKEN` (the read
  token) and `LOBBY_VERSION` (default `latest`). The egg also has to provide the
  plugins in `plugins/`.

## Build identity and the update check

`:surf-minestom-lobby-server:generateBuildInfo` bakes
`surf-minestom-lobby-build.properties` into the jar: the project version, the
commit, the branch, the commit timestamp and — only in CI — `GITHUB_RUN_NUMBER`
as the build number. Locally the build number is empty, which is what marks a
jar as a development build. Nothing is read from the wall clock, so a rebuild
without a new commit does not invalidate the shadow jar.

`/version` (aliases `ver`, `about`, permission
`minestom.lobby.command.version`) prints that identity and compares the version
with the newest GitHub release; the same is logged once after startup.

The check is unauthenticated (60 requests/hour per IP), cached for 15 minutes,
and never fails a command or the startup — a timeout or an HTTP error is
reported as a failed check.
