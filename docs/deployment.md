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
`.github/scripts/bundled-plugin-versions.sh`, which asks reposilite and GitHub
Packages for the `maven-metadata.xml` of every `+` coordinate — the catalog
libraries (24 today) plus the `minestom-relocations` Gradle plugin marker — and
prints the available versions per module.

That listing is diffed against `.github/bundled-plugins.lock`. If it differs,
the workflow dispatches `publish-server.yml` and commits the new lock; if not,
it does nothing. Recording whole version *sets* instead of "the newest version"
means the check never has to reimplement Gradle's `+` resolution — the tradeoff
is that a backported or deleted version also causes one extra publish.

Notes:

- The first run finds no lock file, records the baseline and does *not*
  publish. Deleting the lock file resets it the same way.
- The lock is pushed with the default `GITHUB_TOKEN`, whose pushes do not
  trigger workflows — that is what keeps the commit from setting off the
  push-triggered publish on top of the dispatched one. Giving `actions/checkout`
  a PAT would publish twice.
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
  - `GH_PACKAGES_READ_TOKEN`: PAT with `read:packages` for the private
    internal-plugin repositories (see docs/internal-plugins.md). The update
    check needs it too — an expired token fails that workflow with a message
    naming the secret. It has to be readable **at repository level**: unlike
    the publish job, the check job declares no `environment`, so a secret
    scoped to `production` alone would arrive empty. Adding
    `environment: production` to the check job works too, but then any
    protection rule on that environment stalls the scheduled run.
  - `REPOSILITE_PRIVATE_USER` / `REPOSILITE_PRIVATE_TOKEN`: the CI write token.
- Self-hosted runner with bash, git and curl; JDK 25 comes from `setup-java`.
- The runner's git must be able to commit non-interactively: with
  `commit.gpgsign=true` the lock file commit dies on an invisible pinentry
  prompt, so set `commit.gpgsign false` for the account the runner uses.
- `master` must accept pushes from `github-actions` for the lock file commit.
- Pterodactyl egg variables: `INTERNAL_PLUGINS` (default `true`),
  `MAVEN_REPO_USER` / `MAVEN_REPO_TOKEN` (the read token, only needed with
  internal plugins) and `LOBBY_VERSION` (default `latest`).

## Versioning

Currently every master push publishes `1.0.0-SNAPSHOT`; reposilite keeps
timestamped snapshot files and `latest` resolves to the newest one. Once real
versions are published (e.g. `1.2.0`), `latest` follows the most recently
published version, and `LOBBY_VERSION` can pin a server to a specific one.
