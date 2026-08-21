# Deployment

The lobby server runs in Pterodactyl. Nothing is built at install time — CI
publishes the runnable fat jar (internal plugins included) to the private
reposilite repository, and the egg's install script only downloads it, so
"Reinstall server" takes seconds instead of a ~9 minute build.

## Pipeline

1. `.github/workflows/publish-server.yml` runs on every push to `master` and
   via manual `workflow_dispatch` (for plugin-only updates without a lobby
   commit). It builds with `-PinternalPlugins=true --refresh-dependencies` —
   the refresh forces Gradle to re-check `+`/SNAPSHOT versions instead of
   trusting its 24h dynamic-version cache, so a manual run always picks up
   freshly published plugins.
2. The workflow publishes the shadow jar to
   `https://reposilite.slne.dev/private` under
   `dev.slne.minestom.lobby:surf-minestom-lobby-server`.
3. The egg install script downloads `latest` (or the version pinned in the
   `LOBBY_VERSION` egg variable, e.g. `1.2.0`) with a read token.

## Required setup

- Reposilite: a `private` repository, plus two access tokens — one with write
  access for CI, one read-only for the egg.
- GitHub Actions secrets (repo is public — secrets are not exposed to fork PRs):
  - `GH_PACKAGES_READ_TOKEN`: PAT with `read:packages` for the private
    internal-plugin repositories (see docs/internal-plugins.md).
  - `REPOSILITE_PRIVATE_USER` / `REPOSILITE_PRIVATE_TOKEN`: the CI write token.
- Self-hosted runner with bash and git; JDK 25 comes from `setup-java`.
- Pterodactyl egg variables: `MAVEN_REPO_USER` / `MAVEN_REPO_TOKEN` (the read
  token) and `LOBBY_VERSION` (default `latest`).

## Versioning

Currently every master push publishes `1.0.0-SNAPSHOT`; reposilite keeps
timestamped snapshot files and `latest` resolves to the newest one. Once real
versions are published (e.g. `1.2.0`), `latest` follows the most recently
published version, and `LOBBY_VERSION` can pin a server to a specific one.
