# Internal plugins

Internal (private) plugins such as anticheat and ban bypass are published to
private repositories: reposilite's `private` repository or GitHub Packages.
They are not part of the build. The egg installs them into `plugins/` like every
other plugin, see [deployment.md](deployment.md#plugins); only the credentials
and the list entries differ.

> **Both private plugins must be migrated before they load.**
> `surf-ban-bypass-minestom` and `surf-minestom-anticheat` still use the removed
> lobby plugin API (`MinestomPlugin`/`MinestomPluginEntrypoint`). The server only
> loads plugins that implement
> `dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin` and are built with the
> Gradle plugin `dev.slne.surf.api.gradle.minestom`, which also writes the
> `minestom-plugin.json` the install script requires. Until they are migrated,
> the install fails for them (no `minestom-plugin.json` in the jar) or the server
> does not load them.

## Credentials

Set these egg variables to make the install script use the private repositories.
Each repository is skipped while its token is empty.

| Variable                | Repository                                                              |
|-------------------------|-------------------------------------------------------------------------|
| `MAVEN_REPO_USER`       | `https://reposilite.slne.dev/private` (default user `pterodactyl`)      |
| `MAVEN_REPO_TOKEN`      | `https://reposilite.slne.dev/private`                                   |
| `GITHUB_PACKAGES_USER`  | `https://maven.pkg.github.com/SLNE-Development/surf-minestom-anticheat` |
| `GITHUB_PACKAGES_TOKEN` | `https://maven.pkg.github.com/SLNE-Development/surf-minestom-anticheat` |

- The reposilite token only needs read access to `private`.
- The GitHub token needs `read:packages` and access to the anticheat
  repository. Without a user, the install script sends a placeholder name,
  which GitHub accepts together with a token.
- The tokens are never printed by the install script, and the committed egg
  (`pterodactyl/egg-minestom-lobby.json`) keeps empty defaults. Set the values in
  the panel only.

## List entries

The internal plugins are not part of the egg's default `PLUGINS` list: they are
not migrated yet and the install fails for them without credentials. Once a
plugin is migrated and released, add its entry to `PLUGINS` of the servers that
have the credentials set. Ready to paste:

```
dev.slne.minestom:surf-minestom-anticheat
dev.slne.surf.bb:surf-ban-bypass-minestom
```

Add `:<version>` to pin one. An entry that no repository can serve fails the
install, so a server without credentials must not list them.

`surf-minestom-anticheat` only publishes `-SNAPSHOT` versions to GitHub Packages.
The install script supports this: without a release it selects the highest
snapshot and downloads the newest timestamped build of the fat jar (see
[deployment.md](deployment.md#plugins)). Every reinstall moves the server to the
newest build.

## Adding a plugin

1. Make sure the plugin is built as a `SurfMinestomPlugin` (see above) and
   publishes a fat jar `<artifact>-<version>-all.jar` that bundles its
   libraries, but not surf-api or Minestom.
2. Add `group:artifact` to `PLUGINS` of every server that needs it. Changing
   the egg default only affects servers created afterwards: Pterodactyl stores
   each variable per server when the server is created. No change to the lobby
   build is needed.
3. If the plugin lives in a repository the install script does not know yet,
   extend the `REPO_*` arrays in `pterodactyl/install.sh` and keep
   the copy in `pterodactyl/egg-minestom-lobby.json` identical.
