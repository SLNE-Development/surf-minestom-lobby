#!/bin/bash
# Installs the lobby server jar and the plugins listed in PLUGINS instead of building anything
# (a full build takes ~9 minutes). Runs in the install container of the egg
# (ghcr.io/parkervcp/yolks:debian); the embedded copy in egg-minestom-lobby.json is identical.
#
# LOBBY_VERSION=latest   -> jar of the newest GitHub release
# LOBBY_VERSION=2.0.0    -> jar of release v2.0.0 ("v2.0.0" works as well)
#
# PLUGINS                -> list separated by commas, spaces or newlines. Entries:
#   group:artifact          newest release of the plugin; replaced on every (re)install
#   group:artifact:version  pinned version; never updated
#
# <artifact>-<version>-all.jar is downloaded and stored as plugins/<artifact>-<version>.jar. There
# is no fallback to the thin jar, it lacks the libraries of the plugin. Jars that are not listed
# stay untouched, except other jars of the same plugin id (minestom-plugin.json).
#
# A -SNAPSHOT version (pinned, or the newest one of a plugin without any release) is resolved
# through the version-level maven-metadata.xml to the timestamped build of the all jar and stored
# as plugins/<artifact>-<build>.jar, for example surf-minestom-anticheat-1.0.1-20261017.120000-3.jar.
# A newer build replaces the old jar, the same build is skipped.
#
# Plugin repositories, tried in order. The last two are skipped while their token is empty.
#   https://reposilite.slne.dev/releases
#   https://reposilite.slne.dev/public
#   https://reposilite.slne.dev/private                   MAVEN_REPO_USER / MAVEN_REPO_TOKEN
#   https://maven.pkg.github.com/SLNE-Development/surf-minestom-anticheat
#                                                         GITHUB_PACKAGES_USER / GITHUB_PACKAGES_TOKEN
# A repository that answers 404 or any other status without the jar is recorded and the next one
# is tried. The plugin fails when no repository serves it.
#
# Server Files: /mnt/server
set -euo pipefail

apt update
apt install -y curl unzip jq

SERVER_DIR="${SERVER_DIR:-/mnt/server}"
cd "$SERVER_DIR"

JAR="${SERVER_JARFILE:-lobby.jar}"
VERSION="${LOBBY_VERSION:-latest}"
GITHUB_REPO="SLNE-Development/surf-minestom-lobby"
PLUGIN_DIR="plugins"

WORK=$(mktemp -d)
cleanup() {
    rm -rf "$WORK" "$JAR.tmp"
    rm -f "$PLUGIN_DIR"/.*.part
}
trap cleanup EXIT

# ---------------------------------------------------------------------------------------------
# Lobby jar
# ---------------------------------------------------------------------------------------------

# Accept both "2.0.0" and "v2.0.0".
VERSION="${VERSION//[[:space:]]/}"
if [[ -z "$VERSION" || "$VERSION" == "latest" ]]; then
    # GitHub's /releases/latest endpoint redirects to the release marked as latest.
    echo "Downloading the lobby jar of the latest GitHub release"
    URL="https://github.com/$GITHUB_REPO/releases/latest/download/surf-minestom-lobby-server.jar"
else
    TAG="v${VERSION#v}"
    echo "Downloading the lobby jar of GitHub release $TAG"
    URL="https://github.com/$GITHUB_REPO/releases/download/$TAG/surf-minestom-lobby-server.jar"
fi

curl -fsSL --retry 3 --retry-delay 2 --connect-timeout 15 --max-time 600 -o "$JAR.tmp" "$URL"

[[ -s "$JAR.tmp" ]] || {
    echo "Downloaded jar is empty"
    exit 1
}

mv "$JAR.tmp" "$JAR"
echo "Installed $JAR"

# ---------------------------------------------------------------------------------------------
# Plugins
# ---------------------------------------------------------------------------------------------

REPO_NAMES=(releases public private github-anticheat)
REPO_URLS=(
    "https://reposilite.slne.dev/releases"
    "https://reposilite.slne.dev/public"
    "https://reposilite.slne.dev/private"
    "https://maven.pkg.github.com/SLNE-Development/surf-minestom-anticheat"
)
REPO_USERS=("" "" "${MAVEN_REPO_USER:-pterodactyl}" "${GITHUB_PACKAGES_USER:-token}")
REPO_TOKENS=("" "" "${MAVEN_REPO_TOKEN:-}" "${GITHUB_PACKAGES_TOKEN:-}")
REPO_SECURED=(0 0 1 1)

# Runs curl against repository $1. The credentials are passed as a curl config on stdin so that
# they never show up in a process list; curl drops them when a redirect leaves the host.
repo_curl() {
    local index=$1 user token
    shift

    if [[ -n "${REPO_TOKENS[index]}" ]]; then
        user=${REPO_USERS[index]}
        token=${REPO_TOKENS[index]}
        user=${user//\\/\\\\}
        user=${user//\"/\\\"}
        token=${token//\\/\\\\}
        token=${token//\"/\\\"}
        curl "$@" -K - <<<"user = \"$user:$token\""
    else
        curl "$@"
    fi
}

# Downloads $2 with the credentials of repository $1 into $3 and prints the HTTP status ("000"
# when the request itself failed). The output file is removed unless the status is 200.
http_get() {
    local index=$1 url=$2 out=$3 status

    status=$(repo_curl "$index" -sSL --retry 3 --retry-delay 2 --connect-timeout 15 --max-time 600 \
        -o "$out" -w '%{http_code}' "$url") || status=000

    if [[ "$status" != "200" ]]; then
        rm -f "$out"
    fi

    printf '%s' "$status"
}

# Prints the newest version of an artifact-level maven-metadata.xml: <release>, otherwise the
# highest version without -SNAPSHOT, and only if there is none the highest -SNAPSHOT version.
# (sort -V ranks 1.0.0-SNAPSHOT above 1.0.0, so the two groups are sorted separately.)
latest_from_metadata() {
    local meta version versions

    meta=$(tr -d '\r\n' <"$1")
    version=$(sed -n 's:.*<release>\([^<]*\)</release>.*:\1:p' <<<"$meta")

    if [[ -z "$version" ]]; then
        versions=$(grep -oE '<version>[^<]+</version>' <<<"$meta" | sed -E 's:</?version>::g' || true)
        version=$(grep -v -- '-SNAPSHOT$' <<<"$versions" | sort -V | tail -n 1 || true)

        if [[ -z "$version" ]]; then
            version=$(grep -- '-SNAPSHOT$' <<<"$versions" | sort -V | tail -n 1 || true)
        fi
    fi

    printf '%s' "$version"
}

# Prints the <value> (for example 1.0.1-20261017.120000-3) of the jar with the classifier "all" in
# a version-level maven-metadata.xml of a -SNAPSHOT version, or nothing if there is none.
snapshot_value() {
    local meta

    meta=$(tr -d '\r\n' <"$1")
    sed 's:</snapshotVersion>:&\n:g' <<<"$meta" |
        grep -F '<classifier>all</classifier>' |
        grep -F '<extension>jar</extension>' |
        sed -n 's:.*<value>\([^<]*\)</value>.*:\1:p' |
        head -n 1 || true
}

# Prints the id from the minestom-plugin.json of a jar, or nothing if there is none.
plugin_id() {
    unzip -p "$1" minestom-plugin.json 2>/dev/null | jq -r '.id // empty' 2>/dev/null || true
}

# Deletes every jar in plugins/ except $2 whose plugin id is $1, whatever its file name is.
# Fails as soon as a jar cannot be deleted.
remove_same_id() {
    local id=$1 keep=$2 other

    for other in "$PLUGIN_DIR"/*.jar; do
        [[ -f "$other" && "$other" != "$keep" ]] || continue

        if [[ "$(plugin_id "$other")" == "$id" ]]; then
            echo "  removing ${other##*/} (replaced by ${keep##*/})"
            rm -f "$other" || return 1
        fi
    done
}

# Writes the failure reason of entry $1 to $WORK/tried, where the caller picks it up.
fail_plugin() {
    printf '%s\n' "$1: $2" >"$WORK/tried"
}

# Installs one list entry. Writes the reason to $WORK/tried when it fails. The function is called
# as the condition of an if, which disables errexit inside it, so every command that matters
# is checked explicitly.
install_plugin() {
    local entry=$1
    local group artifact pinned=""
    IFS=: read -r group artifact pinned <<<"$entry"

    local base_path="${group//./\/}/$artifact"
    local tried=() index name url version file_version part target status id

    for index in "${!REPO_NAMES[@]}"; do
        name=${REPO_NAMES[index]}

        if [[ "${REPO_SECURED[index]}" == "1" && -z "${REPO_TOKENS[index]}" ]]; then
            tried+=("$name: skipped, no token set")
            continue
        fi

        url="${REPO_URLS[index]}/$base_path"
        version=$pinned

        if [[ -z "$version" ]]; then
            status=$(http_get "$index" "$url/maven-metadata.xml" "$WORK/metadata.xml")

            if [[ "$status" != "200" ]]; then
                tried+=("$name: HTTP $status (maven-metadata.xml)")
                continue
            fi

            version=$(latest_from_metadata "$WORK/metadata.xml")

            if [[ -z "$version" ]]; then
                tried+=("$name: no version listed")
                continue
            fi
        fi

        if [[ ! "$version" =~ ^[A-Za-z0-9._+-]+$ ]]; then
            tried+=("$name: unusable version '$version'")
            continue
        fi

        # Gradle uploads a snapshot as timestamped files; the version directory lists them. A newer
        # build replaces the older one because the build is part of the file name.
        file_version=$version

        if [[ "$version" == *-SNAPSHOT ]]; then
            status=$(http_get "$index" "$url/$version/maven-metadata.xml" "$WORK/snapshot.xml")

            if [[ "$status" != "200" ]]; then
                tried+=("$name: HTTP $status ($version/maven-metadata.xml)")
                continue
            fi

            file_version=$(snapshot_value "$WORK/snapshot.xml")

            if [[ ! "$file_version" =~ ^[A-Za-z0-9._+-]+$ ]]; then
                tried+=("$name: no -all jar listed for $version")
                continue
            fi
        fi

        target="$PLUGIN_DIR/$artifact-$file_version.jar"

        # An installed jar without a readable id counts as damaged and is downloaded again.
        if [[ -f "$target" ]]; then
            id=$(plugin_id "$target")

            if [[ -n "$id" ]]; then
                echo "  $artifact-$file_version.jar is already installed"

                remove_same_id "$id" "$target" || {
                    fail_plugin "$entry" "could not remove older jars with the id $id"
                    return 1
                }

                return 0
            fi
        fi

        # Every surf plugin publishes a fat jar (-all) that bundles its libraries. A thin jar of the
        # same version is not a valid install.
        part="$PLUGIN_DIR/.$artifact-$file_version.jar.part"
        status=$(http_get "$index" "$url/$version/$artifact-$file_version-all.jar" "$part")

        if [[ "$status" != "200" ]]; then
            tried+=("$name: HTTP $status")
            continue
        fi

        if [[ ! -s "$part" ]]; then
            rm -f "$part"
            tried+=("$name: empty response")
            continue
        fi

        id=$(plugin_id "$part")

        if [[ -z "$id" ]]; then
            rm -f "$part"
            fail_plugin "$entry" "the jar from $name has no minestom-plugin.json with an id"
            return 1
        fi

        mv -f "$part" "$target" || {
            rm -f "$part"
            fail_plugin "$entry" "could not move the jar from $name to $target"
            return 1
        }

        echo "  installed $artifact-$file_version.jar (id $id, from $name)"

        # Other jars of the same plugin are replaced, whatever their file name is.
        remove_same_id "$id" "$target" || {
            fail_plugin "$entry" "installed $target but could not remove older jars with the id $id"
            return 1
        }

        return 0
    done

    local summary="" item
    for item in "${tried[@]}"; do
        summary+="${summary:+; }$item"
    done

    fail_plugin "$entry" "no repository serves the -all jar; $summary"
    return 1
}

mkdir -p "$PLUGIN_DIR"

# Commas, tabs and line breaks separate entries like spaces do.
PLUGIN_ENTRIES=()
read -r -a PLUGIN_ENTRIES <<<"$(printf '%s' "${PLUGINS:-}" | tr ',\t\r\n' '    ')" || true

if [[ ${#PLUGIN_ENTRIES[@]} -eq 0 ]]; then
    echo "PLUGINS is empty, no plugins installed"
    exit 0
fi

echo "Installing ${#PLUGIN_ENTRIES[@]} plugins into $PLUGIN_DIR/"

FAILURES=()
for entry in "${PLUGIN_ENTRIES[@]}"; do
    if [[ ! "$entry" =~ ^[A-Za-z0-9._-]+:[A-Za-z0-9._-]+(:[A-Za-z0-9._+-]+)?$ ]]; then
        FAILURES+=("$entry: expected group:artifact or group:artifact:version")
        continue
    fi

    echo "$entry"
    rm -f "$WORK/tried"

    if ! install_plugin "$entry"; then
        FAILURES+=("$(cat "$WORK/tried" 2>/dev/null || printf '%s: failed' "$entry")")
    fi
done

if [[ ${#FAILURES[@]} -gt 0 ]]; then
    echo >&2
    echo "Could not install ${#FAILURES[@]} of ${#PLUGIN_ENTRIES[@]} plugins:" >&2
    printf '  %s\n' "${FAILURES[@]}" >&2
    exit 1
fi

echo "Installed ${#PLUGIN_ENTRIES[@]} plugins"
