#!/usr/bin/env bash
# Prints the versions currently available for every dynamically versioned ("+")
# surf-api dependency of the lobby server, one line per module:
#
#     group:artifact version version ...
#
# The output is a fingerprint of the upstream state: it changes as soon as surf-api
# gains (or loses) a version, so no reimplementation of Gradle's "+" resolution is
# needed. .github/workflows/check-surf-api-updates.yml diffs it against
# .github/surf-api.lock to decide whether to rebuild the jar. Plugins are not
# covered: the egg installs them into plugins/, see docs/deployment.md.
set -euo pipefail

repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)

# Mirrors the Maven repositories of the server build (surf-minestom-lobby-server/build.gradle.kts).
repos=(
    https://reposilite.slne.dev/public
    https://reposilite.slne.dev/releases
)

die() {
    echo "error: $*" >&2
    exit 1
}

# Fetches a URL and prints the body, failing with the status code on anything
# but 200 - a silently skipped module would hide an update.
fetch() {
    local url=$1
    shift
    local response status
    response=$(curl -sS --retry 3 --retry-delay 2 --max-time 30 \
        -w $'\n%{http_code}' "$@" "$url") || die "request to $url failed"
    status=${response##*$'\n'}
    [ "$status" = 200 ] || die "$url returned HTTP $status"
    printf '%s' "${response%$'\n'*}"
}

versions_from_metadata() {
    # `|| true` so metadata without any version reaches the check below instead
    # of aborting on grep's exit code; a failed fetch still fails the pipeline.
    { grep -oE '<version>[^<]+</version>' || true; } | sed -E 's|</?version>||g'
}

catalog_modules=$(
    grep -E 'version *= *"\+"' "$repo_root/gradle/libs.versions.toml" |
        grep -oE 'module *= *"[^"]+"' |
        sed -E 's/.*"(.*)"/\1/'
) || true
[ -n "$catalog_modules" ] ||
    die "no '+' versioned libraries in gradle/libs.versions.toml - did the catalog format change?"

# Marker artifacts of `id("...") version "+"` Gradle plugins. Not part of the jar
# themselves, but the surf-api server plugin defines how the jar is built (shading,
# library download, mixin setup), so a new release changes the output.
plugin_modules=$(
    grep -hoE 'id\("[^"]+"\) version "\+"' \
        "$repo_root"/*.gradle.kts "$repo_root"/*/*.gradle.kts |
        sed -E 's/id\("(.*)"\) version .*/\1:\1.gradle.plugin/'
) || true
[ -n "$plugin_modules" ] ||
    die "no '+' versioned Gradle plugins in the build scripts - did they change?"

modules=$(printf '%s\n%s\n' "$catalog_modules" "$plugin_modules" | LC_ALL=C sort -u)
count=0

while IFS= read -r module; do
    group=${module%%:*}
    path="${group//.//}/${module#*:}/maven-metadata.xml"
    versions=

    for repo in "${repos[@]}"; do
        versions+=$(fetch "$repo/$path" | versions_from_metadata)$'\n'
    done

    versions=$(printf '%s\n' "$versions" | sed '/^$/d' | LC_ALL=C sort -u | tr '\n' ' ')
    [ -n "$versions" ] || die "$module has no versions listed"

    echo "$module ${versions% }"
    count=$((count + 1))
done <<<"$modules"

echo "probed $count modules" >&2
