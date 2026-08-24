# Deployment

The lobby server runs in Pterodactyl. Nothing is built at install time — CI
publishes the runnable fat jar (internal plugins included) to the private
reposilite repository, and the egg's install script only downloads it, so
"Reinstall server" takes seconds instead of a ~9 minute build.

## Pipeline

1. `.github/workflows/publish-server.yml` runs on every push to `master` and
   via `workflow_dispatch` (for plugin-only updates without a lobby commit —
   either triggered by the update check below or manually). It builds with
   `-PinternalPlugins=true --refresh-dependencies` — the refresh forces Gradle
   to re-check `+`/SNAPSHOT versions instead of trusting its 24h
   dynamic-version cache, so a dispatched run always picks up freshly
   published plugins.
2. The workflow publishes two variants:
   - **internal** (anticheat etc. shaded in) to
     `https://reposilite.slne.dev/private` under
     `dev.slne.minestom.lobby:surf-minestom-lobby-server`;
   - **public** (no internal plugins) as asset
     `surf-minestom-lobby-server.jar` on the rolling `latest` GitHub
     prerelease of this repository (real versions will use `v1.2.0`-style
     tags later).
3. The egg install script downloads `latest` (or the version pinned in the
   `LOBBY_VERSION` egg variable, e.g. `1.2.0`). With `INTERNAL_PLUGINS=true`
   it pulls the internal jar from reposilite using a read token; with `false`
   it pulls the public jar from GitHub releases without credentials.

## Plugin update check

Most of what the jar bundles is versioned `+`, so a plugin release changes the
build output without any commit here. `.github/workflows/check-plugin-updates.yml`
closes that gap: once a day (03:17 UTC, plus `workflow_dispatch`) it runs
`.github/scripts/bundled-plugin-versions.sh`, which asks reposilite (public,
releases and private) and GitHub Packages for the `maven-metadata.xml` of every
`+` coordinate — the catalog libraries (25 today) plus the
`minestom-relocations` Gradle plugin marker — and prints the available versions
per module.

That listing is diffed against `.github/bundled-plugins.lock`. If it differs,
the workflow dispatches `publish-server.yml` and commits the new lock; if not,
it does nothing. Recording whole version *sets* instead of "the newest version"
means the check never has to reimplement Gradle's `+` resolution — the tradeoff
is that a backported or deleted version also causes one extra publish.

Notes:

- The first run finds no lock file, records the baseline and does *not*
  publish. Deleting the lock file resets it the same way.
- The probe needs credentials for every private repository a bundled plugin
  lives in: `GH_PACKAGES_READ_TOKEN` for GitHub Packages plus
  `REPOSILITE_PRIVATE_USER`/`REPOSILITE_PRIVATE_TOKEN` for reposilite's
  `private` repository. A missing one fails the step instead of silently
  dropping the module — a skipped module would hide an update.
- `master` is covered by the `Default` ruleset (merge queue + pull request
  required), and the default `GITHUB_TOKEN` cannot be a ruleset bypass actor.
  The lock is therefore pushed with an app installation token (see below).
- That token has to be handed to `actions/checkout` via `token:` — it is not
  enough to put it in the push URL. Checkout stores its credential as
  `http.https://github.com/.extraheader`, which git sends for *every*
  `https://github.com/` URL and which wins over credentials in the URL, so a
  push URL carrying the app token still authenticates as `github-actions[bot]`
  and gets rejected by the ruleset. Hence the token is minted before checkout,
  unconditionally, and `git push origin` just works. Because the token is now
  minted on every run, missing or expired `LOCK_PUSH_APP_*` secrets fail the
  very first step instead of only the runs that found an update.
- The recording step only runs on `master`. Since the app token bypasses the
  ruleset, a `workflow_dispatch` from a feature branch would otherwise push
  that branch's commits to `master` — the probe, the diff and the publish
  dispatch are branch-safe, the push is not.
- Unlike `GITHUB_TOKEN`, an app token *does* trigger workflows, so
  `publish-server.yml` ignores pushes that only touch the lock file. Without
  that `paths-ignore` the lock commit would publish a second time on top of the
  dispatched run — and even bootstrap runs would publish.
- The publish is dispatched before the lock is committed, so a failed push
  costs a duplicate publish tomorrow rather than a missed update. A failed
  *publish* is not retried automatically — the lock already moved on, so rerun
  `publish-server.yml` by hand.
- LuckPerms and spark are deliberately not polled: both are pinned by commit
  (submodule / vendored source), so a push already triggers the publish.
- GitHub disables `schedule` triggers on public repositories after ~60 days
  without repository activity. If updates stop appearing, check whether the
  workflow was disabled rather than assuming nothing was released.

## Required setup

- Reposilite: a `private` repository, plus two access tokens — one with write
  access for CI, one read-only for the egg.
- GitHub Actions secrets (repo is public — secrets are not exposed to fork PRs):
  Both jobs run on the `production` environment, so environment-scoped secrets
  work; repository-level ones do too.

  - `GH_PACKAGES_READ_TOKEN`: PAT with `read:packages` for the private
    internal-plugin repositories (see docs/internal-plugins.md). The update
    check needs it too — an expired token fails that workflow with a message
    naming the secret.
  - `REPOSILITE_PRIVATE_USER` / `REPOSILITE_PRIVATE_TOKEN`: the CI write token.
  - `LOCK_PUSH_APP_ID` / `LOCK_PUSH_APP_PRIVATE_KEY`: the app that may push the
    lock file to `master` (see below).
- Self-hosted runner with bash, git and curl; JDK 25 comes from `setup-java`.
- A GitHub App for the lock file push, because the `Default` ruleset blocks
  direct pushes to `master` and `GITHUB_TOKEN` cannot bypass it:
  - the app only needs repository permission `Contents: Read and write`, and
    must be installed on this repository;
  - add it to the bypass list of the `Default` ruleset (Settings → Rules →
    Rulesets → Default). It only shows up in the picker once installed.

  A bot account added to the team that already has bypass access works too —
  then swap the app-token step for that account's PAT. Adding the
  `github-actions` app itself is not possible; its token is not branch-scoped,
  so GitHub does not allow it as a bypass actor.
- Pterodactyl egg variables: `INTERNAL_PLUGINS` (default `true`),
  `MAVEN_REPO_USER` / `MAVEN_REPO_TOKEN` (the read token, only needed with
  internal plugins) and `LOBBY_VERSION` (default `latest`).

## Build identity and the update check

`:surf-minestom-lobby-server:generateBuildInfo` bakes
`surf-minestom-lobby-build.properties` into the jar: the project version, the
commit, the branch, the commit timestamp and — only in CI — `GITHUB_RUN_NUMBER`
as the build number. Locally the build number is empty, which is what marks a
jar as a development build. Nothing is read from the wall clock, so a rebuild
without a new commit does not invalidate the shadow jar.

`/version` (aliases `ver`, `about`, permission
`minestom.lobby.command.version`) prints that identity plus how many builds the
server is behind, and the same two lines are logged once after startup.

The distance is read from the GitHub Actions API: the successful
`publish-server.yml` runs on `master`, counting those whose run number is higher
than ours. Counting *successful runs* rather than subtracting run numbers is
what makes the number a build count — a failed run consumes a run number
without publishing a jar. It also covers the plugin-only republishes above,
which a commit-distance check (what Paper does) would report as "up to date"
even though the jar is stale.

The check is unauthenticated (60 requests/hour per IP), cached for 15 minutes,
and never fails a command or the startup — a timeout or an HTTP error is
reported as a failed check. Only the newest 100 published builds are inspected;
past that the count is reported as a lower bound.

## Versioning

Currently every master push publishes `1.0.0-SNAPSHOT`; reposilite keeps
timestamped snapshot files and `latest` resolves to the newest one. Once real
versions are published (e.g. `1.2.0`), `latest` follows the most recently
published version, and `LOBBY_VERSION` can pin a server to a specific one.
