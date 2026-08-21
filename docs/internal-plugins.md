# Internal plugins

Internal/private plugins (e.g. anticheat) are published to private GitHub Packages
repositories and are an **optional** part of the build: without access they are
simply excluded and the build works normally.

## Enabling

1. Set the credentials in `~/.gradle/gradle.properties`:

   ```properties
   gpr.user=<github username>
   gpr.key=<github token with read:packages>
   ```

   In CI, `GITHUB_ACTOR`/`GITHUB_TOKEN` environment variables work as a fallback.

2. Build with `-PinternalPlugins=true` (or put `internalPlugins=true` in
   `~/.gradle/gradle.properties`).

## How it works

Everything lives in `surf-minestom-lobby-server/build.gradle.kts`, gated on the
`internalPlugins` property. It has to live there: Gradle resolves a configuration
with the repositories of the *resolving* project, so a repository declared in a
helper module would never be consulted for the server's `runtimeClasspath`.

- The private GitHub Packages repositories are declared as `exclusiveContent`
  with credentials, filtered to the internal groups — so only internal plugins
  are looked up on GitHub and the public repositories are never asked for them.
- The plugins are `runtimeOnly(...) { isTransitive = false }`, so they land on
  the runtime classpath and are **shaded** by `shadowJar` — unlike the public
  plugins, which gremlin downloads at runtime (gremlin has no credentials at
  runtime, hence shading).
- Discovery at runtime is unchanged: shaded plugins sit on the same system
  classloader as gremlin-downloaded ones and are found via `ServiceLoader`.

## Adding a plugin

1. Add a `forRepository` block for its GitHub repository (if new) and make sure
   its group is covered by the `filter` in the `exclusiveContent` block.
2. Add the coordinates to `gradle/libs.versions.toml` and a
   `runtimeOnly(...) { isTransitive = false }` line in the `internalPlugins`
   dependencies block. Private-only transitive dependencies must be listed
   explicitly.
