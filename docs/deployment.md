# Deployment

The lobby server runs in Pterodactyl. Nothing is built at install time. CI
publishes one runnable server jar, and the egg's install script downloads it
together with the plugins of the server, so "Reinstall server" takes seconds
instead of a full build. The egg lives in
[`pterodactyl/egg-minestom-lobby.json`](../pterodactyl/egg-minestom-lobby.json);
the install script it embeds is
[`pterodactyl/install.sh`](../pterodactyl/install.sh). Keep both identical.

## Pipeline

1. `.github/workflows/publish-server.yml` runs on every push to `master` and
   via `workflow_dispatch` (for manual republishes). Pushes that only touch
   `.github/surf-api.lock` are ignored. It builds with `--refresh-dependencies`
   — the refresh forces Gradle to re-check `+`/SNAPSHOT versions instead of
   trusting its 24h dynamic-version cache, so a run always picks up a freshly
   published surf-api. There is no submodule or vendor step: LuckPerms and
   spark come with the surf-api feature modules.
2. The workflow publishes the same jar,
   `surf-minestom-lobby-server/build/libs/surf-minestom-lobby-server-<version>.jar`
   (`<version>` is `projectVersion` from `gradle.properties`), twice:
   - to `https://reposilite.slne.dev/private` as the `lobbyServer` publication
     `dev.slne.minestom.lobby:surf-minestom-lobby-server`;
   - as asset `surf-minestom-lobby-server.jar` of the GitHub release
     `v<version>`, which is marked as the latest release.
3. The egg install script downloads the release asset: `latest`, or the version
   pinned in the `LOBBY_VERSION` egg variable (`2.0.0` and `v2.0.0` both work).
   That download needs no credentials.

The build resolves surf-api from reposilite only. A lobby that needs a newer
surf-api (2.0.0 needs 3.50.0) can only be published after that surf-api
release: release surf-api first, then merge the lobby. If the lobby was merged
earlier, rerun this workflow via `workflow_dispatch` once surf-api is out.

The `surf-minestom-lobby-api` module no longer exists and is not published.
Its command API lives in surf-api (`dev.slne.surf.api.minestom.command`).

## Jar layout

The server is built with the surf-api Gradle plugin
`dev.slne.surf.api.gradle.minestom-server`. There is a single jar; plugins are
not part of the build. The jar is a thin bootstrap:

- The jar contains the lobby's own code and the surf-api bootstrap. The lobby
  requires surf-api 3.50.0 or newer (`SurfMinestomServer.exit()` for `/stop`,
  `exitFlushingLogs` for startup failures; the console is the surf-api default
  and surf-api flushes the logs).
- The bootstrap agent downloads everything else (surf-api, Minestom, log4j,
  Guice, Polar, komapper, the JDBC drivers, ...) into `libraries/` on first
  start (`downloadSurfModules = true`). The directory can be changed with
  `-Dsurf.minestom.libraries=<dir>`. The first start needs network access; keep
  `libraries/` persistent to avoid downloading again.
- Mixins come from surf-api, including the compatibility fix for
  `/miniplaceholders` on Minestom 26.3 (surf-api 3.50.0 and newer apply it when
  MiniPlaceholders is on the class path). The lobby has no mixins of its own.
- LuckPerms and spark are surf-api feature modules (`withLuckPerms()`,
  `withSpark()`). Their data directories stay `plugins/luckperms` and
  `plugins/spark`.

The surf plugin also writes `-sources.jar` and `-javadoc.jar` next to the
server jar. The `jar` task uses the classifier `thin`; the shadow jar has no
classifier, which is why the workflow picks it by exact name.

## Plugins

Plugins are installed by the egg, not by the build. Every surf plugin
publishes a fat jar, `<artifact>-<version>-all.jar`, that bundles its libraries
but neither surf-api nor Minestom; those come from the server. The server loads
every jar in `plugins/` that carries a `minestom-plugin.json`. A plugin that
fails to load or enable aborts the startup (`withPlugins { requireAll = true }`),
so a broken plugin is noticed instead of silently missing.

The plugins of a server are the egg variable `PLUGINS`. It is editable per
server in the panel and holds a list separated by commas, spaces or line
breaks. Pterodactyl stores the value per server when the server is created, so
a later change of the egg default only reaches new servers. An entry is one
of:

| Entry                    | Meaning                                                           |
|--------------------------|-------------------------------------------------------------------|
| `group:artifact`         | The newest release. Replaced by a newer one on every (re)install. |
| `group:artifact:version` | Exactly this version. Never updated.                              |

The default lists the 22 public surf plugins of the former bundled build, for
example `dev.slne.surf.chat:surf-chat-minestom`. The internal plugins
`dev.slne.minestom:surf-minestom-anticheat` and
`dev.slne.surf.bb:surf-ban-bypass-minestom` are not in it: they are not migrated
yet and need credentials (see [internal-plugins.md](internal-plugins.md)).

> **Known blocker: trophy.** `surf-lobby-minestom` requires
> `surf-trophy-minestom`, which is in the default list. surf-trophy 1.3.6 is not
> migrated yet, so no jar with a `minestom-plugin.json` exists and the install
> fails at this entry until a `surf-trophy-minestom` release is published. The
> failure is intended; it is not hidden by removing the entry.

On every install the script walks the list in order:

1. It resolves the version, per repository. For `group:artifact` this is
   `<release>` of the `maven-metadata.xml`; without a `<release>` it is the
   highest version that is not a `-SNAPSHOT`, and only if there is none the
   highest `-SNAPSHOT` version.
2. For a `-SNAPSHOT` version (pinned or selected) it reads
   `<version>/maven-metadata.xml` and takes the `<snapshotVersion>` with the
   classifier `all` and the extension `jar`. Its `<value>`, for example
   `1.0.1-20261017.120000-3`, replaces the version in the file name. Gradle
   uploads snapshots as timestamped files, so this is the only way to find them.
3. It downloads `<artifact>-<version>-all.jar` from the first repository that
   serves it (below). There is no fallback to the thin jar `<artifact>-<version>.jar`:
   it lacks the libraries of the plugin, so only the fat jar is valid. Every
   status other than 200 (404 when the repository does not have it, but also 401,
   403, 5xx, or no answer at all, shown as `000`) is recorded as
   `<repo>: HTTP <status>` and the next repository is tried.
4. It requires a `minestom-plugin.json` in the jar and reads its `id`.
5. It stores the jar as `plugins/<artifact>-<version>.jar` (for a snapshot
   `plugins/<artifact>-<value>.jar`) and deletes every other jar in `plugins/` with
   the same plugin id, whatever its file name.

If the target file already exists and carries a readable plugin id, nothing is
downloaded (a jar without one counts as damaged and is downloaded again), but
step 5 still deletes other jars with the same id. A newer snapshot build has a different file
name, so it is downloaded and replaces the old build; the same build is skipped.
A pinned `-SNAPSHOT` version is therefore pinned to the snapshot line, not to one
build.

A listed plugin that cannot be installed fails the install. The message names the
plugin and the status of every repository that was tried, or the failed file
operation. All entries are attempted before the install fails, so one run reports
every problem.

Jars in `plugins/` that are not listed are left untouched; the script never
deletes a jar because its entry is missing. To exclude a plugin from a server,
remove its entry **and** delete `plugins/<artifact>-*.jar`. Plugins that another
listed plugin requires cannot be removed alone: `surf-core-minestom` and
`surf-redis-minestom` are required by `surf-chat-minestom`, for example, and the
server refuses to start when a required plugin is missing (`requireAll`). To
update, reinstall the server: pinned plugins stay, unpinned ones move to their
newest release.

### Repositories

Tried in this order; the first one that has the plugin wins.

| Repository                                                              | Credentials                                      |
|-------------------------------------------------------------------------|--------------------------------------------------|
| `https://reposilite.slne.dev/releases`                                  | none                                             |
| `https://reposilite.slne.dev/public`                                    | none                                             |
| `https://reposilite.slne.dev/private`                                   | `MAVEN_REPO_USER` / `MAVEN_REPO_TOKEN`           |
| `https://maven.pkg.github.com/SLNE-Development/surf-minestom-anticheat` | `GITHUB_PACKAGES_USER` / `GITHUB_PACKAGES_TOKEN` |

The last two are skipped while their token is empty. The tokens are never
printed. Private plugins and the credential variables are described in
[internal-plugins.md](internal-plugins.md).

The default list contains no private plugin. A server that adds one needs the
matching token, otherwise the install fails at that entry.

### Egg variables

| Variable                | Default               | Notes                                           |
|-------------------------|-----------------------|-------------------------------------------------|
| `SERVER_JARFILE`        | `lobby.jar`           | File name of the lobby jar.                     |
| `LOBBY_VERSION`         | `latest`              | `latest` or a version such as `2.0.0`.          |
| `PLUGINS`               | the 22 public plugins | Per server; format above.                       |
| `MAVEN_REPO_USER`       | `pterodactyl`         | Read token name for reposilite `private`.       |
| `MAVEN_REPO_TOKEN`      | empty                 | Read token secret for reposilite `private`.     |
| `GITHUB_PACKAGES_USER`  | empty                 | GitHub user for GitHub Packages.                |
| `GITHUB_PACKAGES_TOKEN` | empty                 | Token with `read:packages` for GitHub Packages. |

The egg in the repository has empty token defaults. Set the tokens in the
panel, never in the committed JSON.

## surf-api update check

surf-api is the only part of the build that is versioned `+`, so a surf-api
release changes the build output without any commit here.
`.github/workflows/check-surf-api-updates.yml` closes that gap: once a day
(03:17 UTC, plus `workflow_dispatch`) it runs
`.github/scripts/surf-api-versions.sh`, which asks reposilite (public and
releases) for the `maven-metadata.xml` of every `+` coordinate — the catalog
entry `dev.slne.surf.api:surf-api-minestom-server` and the
`dev.slne.surf.api.gradle.minestom-server` Gradle plugin marker — and prints the
available versions per module.

That listing is diffed against `.github/surf-api.lock`. If it differs, the
workflow records it in the lock, bumps the minor version in `gradle.properties`
and pushes both as one commit to `master`; that push starts
`publish-server.yml`. If the listing is unchanged, nothing happens. Recording
whole version *sets* instead of "the newest version" means the check never has
to reimplement Gradle's `+` resolution — the tradeoff is that a backported or
deleted version also causes one extra publish.

Plugin releases are not part of this check. Plugins are not in the jar, so a
new plugin release needs no new lobby build: a server picks it up on its next
reinstall.

Notes:

- The first run finds no lock file, records the baseline and does *not*
  publish. Deleting the lock file resets it the same way.
- `master` is covered by the `Default` ruleset (merge queue + pull request
  required), and the default `GITHUB_TOKEN` cannot be a ruleset bypass actor.
  The commit is therefore pushed with an app installation token (see below).
- That token has to be handed to `actions/checkout` via `token:` — it is not
  enough to put it in the push URL. Checkout stores its credential as
  `http.https://github.com/.extraheader`, which git sends for *every*
  `https://github.com/` URL and which wins over credentials in the URL, so a
  push URL carrying the app token still authenticates as `github-actions[bot]`
  and gets rejected by the ruleset. Hence the token is minted before checkout,
  unconditionally, and `git push origin` just works. Because the token is now
  minted on every run, missing or expired `LOCK_PUSH_APP_*` secrets fail the
  very first step instead of only the runs that found an update.
- The workflow always checks out and pushes `master`, so a `workflow_dispatch`
  from another branch does not push that branch.
- Unlike `GITHUB_TOKEN`, an app token *does* trigger workflows. An update commit
  changes `gradle.properties` and therefore publishes the new version. The
  bootstrap commit only touches the lock file, which `publish-server.yml`
  ignores via `paths-ignore` — without that, bootstrap runs would publish too.
- A failed publish is not retried automatically — the lock already moved on, so
  rerun `publish-server.yml` by hand.
- LuckPerms and spark are not polled on their own: they arrive with the surf-api
  feature modules, so a new surf-api release covers them.
- The check only sees `+` coordinates declared in `gradle/libs.versions.toml`
  and `id("...") version "+"` plugins in the build scripts. Keep a `+`
  `surf-api-minestom-server` entry in the catalog even though the Gradle plugin
  adds the dependency itself, otherwise surf-api releases are not detected.
- GitHub disables `schedule` triggers on public repositories after ~60 days
  without repository activity. If updates stop appearing, check whether the
  workflow was disabled rather than assuming nothing was released.

## Required setup

- Reposilite: a `private` repository with an access token that has write access
  for CI. For private plugins, a second, read-only token for the egg.
- GitHub Actions secrets (repo is public — secrets are not exposed to fork PRs).
  Both workflows run on the `production` environment, so environment-scoped
  secrets work; repository-level ones do too.

  - `REPOSILITE_PRIVATE_USER` / `REPOSILITE_PRIVATE_TOKEN`: the CI write token.
  - `LOCK_PUSH_APP_ID` / `LOCK_PUSH_APP_PRIVATE_KEY`: the app that may push the
    lock file and version bump to `master` (see below).
- Self-hosted runner with bash, git and curl; JDK 25 comes from `setup-java`.
- A GitHub App for the update commit, because the `Default` ruleset blocks
  direct pushes to `master` and `GITHUB_TOKEN` cannot bypass it:
  - the app only needs repository permission `Contents: Read and write`, and
    must be installed on this repository;
  - add it to the bypass list of the `Default` ruleset (Settings → Rules →
    Rulesets → Default). It only shows up in the picker once installed.

  A bot account added to the team that already has bypass access works too —
  then swap the app-token step for that account's PAT. Adding the
  `github-actions` app itself is not possible; its token is not branch-scoped,
  so GitHub does not allow it as a bypass actor.
- Pterodactyl egg: import `pterodactyl/egg-minestom-lobby.json`. The install
  container (`ghcr.io/parkervcp/yolks:debian`) installs `curl`, `unzip` and
  `jq` itself.

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

## Versioning

The release version is `projectVersion` in `gradle.properties` (`2.0.0` at the
time of writing). `publish-server.yml` requires a plain `major.minor.patch`
version, tags the commit `v<version>` and fails if that tag already points to
another commit, so the version has to be bumped before the next publish. The
surf-api update check bumps the minor version automatically. `latest` follows
the most recently published version, and `LOBBY_VERSION` can pin a server to a
specific one.
