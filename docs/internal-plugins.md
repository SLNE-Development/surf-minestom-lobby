# Internal plugins

Internal/private plugins (e.g. anticheat, ban bypass) are published to private
repositories — GitHub Packages or reposilite's `private` repository — and are an
**optional** part of the build: without access they are simply excluded and the
build works normally.

## Enabling

1. Set the credentials in `~/.gradle/gradle.properties`:

   ```properties
   gpr.user=<github username>
   gpr.key=<github token with read:packages>
   ```

   In CI, `GITHUB_ACTOR`/`GITHUB_TOKEN` environment variables work as a fallback.
   Plugins hosted on reposilite instead need `REPOSILITE_USER`/`REPOSILITE_TOKEN`
   in the environment.

2. Build with `-PinternalPlugins=true` (or put `internalPlugins=true` in
   `~/.gradle/gradle.properties`).

## How it works

Everything lives in `surf-minestom-lobby-server/build.gradle.kts`, gated on the
`internalPlugins` property. It has to live there: Gradle resolves a configuration
with the repositories of the *resolving* project, so a repository declared in a
helper module would never be consulted for the server's `runtimeClasspath`.

- Every private repository is declared as its own `exclusiveContent` block with
  credentials, filtered to the coordinates it hosts — so only internal plugins are
  looked up there and the public repositories are never asked for them. One block
  per repository is mandatory: `forRepository` takes a single repository, and a
  second one in the same block would end up a regular repository that is
  *excluded* from the filtered content.
- The plugins are `runtimeOnly(...) { isTransitive = false }`, so they land on
  the runtime classpath and are **shaded** by `shadowJar` — unlike the public
  plugins, which gremlin downloads at runtime (gremlin has no credentials at
  runtime, hence shading).
- Discovery at runtime is unchanged: shaded plugins sit on the same system
  classloader as gremlin-downloaded ones and are found via `ServiceLoader`.

## Adding a plugin

1. Add an `exclusiveContent` block for its repository (if new) and make sure its
   coordinates are covered by that block's `filter`.
2. Add the coordinates to `gradle/libs.versions.toml` and a
   `runtimeOnly(...) { isTransitive = false }` line in the `internalPlugins`
   dependencies block. Private-only transitive dependencies must be listed
   explicitly.
