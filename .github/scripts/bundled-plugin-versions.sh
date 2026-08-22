#!/usr/bin/env bash
# Prints the versions currently available for every dynamically versioned ("+")
# dependency the lobby server bundles, one line per module:
#
#     group:artifact version version ...
#
# The output is a fingerprint of the upstream state: it changes as soon as a
# bundled plugin gains (or loses) a version, so no reimplementation of Gradle's
# "+" resolution is needed. .github/workflows/check-plugin-updates.yml diffs it
# against .github/bundled-plugins.lock to decide whether to rebuild the jar.
#
# Reading the internal plugins needs GH_PACKAGES_READ_TOKEN (a token with
# read:packages), see docs/internal-plugins.md.
set -euo pipefail

repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)

# Mirrors the repositories in surf-minestom-lobby-server/build.gradle.kts.
reposilite_repos=(
    https://reposilite.slne.dev/public
    https://reposilite.slne.dev/releases
)
anticheat_repo=https://maven.pkg.github.com/SLNE-Development/surf-minestom-anticheat

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

# Marker artifacts of `id("...") version "+"` Gradle plugins. Not bundled
# themselves, but they define the jar's relocations.
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

    case $group in
    dev.slne.minestom*)
        [ -n "${GH_PACKAGES_READ_TOKEN:-}" ] ||
            die "GH_PACKAGES_READ_TOKEN is not set, so $module cannot be read from GitHub Packages (see docs/internal-plugins.md)"
        versions=$(
            fetch "$anticheat_repo/$path" \
                -u "${GITHUB_ACTOR:-x}:$GH_PACKAGES_READ_TOKEN" |
                versions_from_metadata
        ) || die "cannot read $module from GitHub Packages - is GH_PACKAGES_READ_TOKEN still valid and does it grant read:packages?"
        ;;
    *)
        for repo in "${reposilite_repos[@]}"; do
            versions+=$(fetch "$repo/$path" | versions_from_metadata)$'\n'
        done
        ;;
    esac

    versions=$(printf '%s\n' "$versions" | sed '/^$/d' | LC_ALL=C sort -u | tr '\n' ' ')
    [ -n "$versions" ] || die "$module has no versions listed"

    echo "$module ${versions% }"
    count=$((count + 1))
done <<<"$modules"

echo "probed $count modules" >&2
