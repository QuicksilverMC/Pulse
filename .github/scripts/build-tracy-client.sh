#!/usr/bin/env bash
set -euo pipefail

version="${1:?usage: build-tracy-client.sh <tracy-version> <output-dir> [cmake args...]}"
out="${2:?usage: build-tracy-client.sh <tracy-version> <output-dir> [cmake args...]}"
src="$(mktemp -d)/tracy"

git clone --depth 1 --branch "v${version}" https://github.com/wolfpld/tracy "${src}"

cmake -S "${src}" -B "${src}/build" \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=ON \
    -DTRACY_ENABLE=ON \
    -DTRACY_ON_DEMAND=ON \
    -DTRACY_MANUAL_LIFETIME=ON \
    -DTRACY_NO_CRASH_HANDLER=ON \
    -DTRACY_NO_SAMPLING=ON \
    -DCMAKE_CXX_FLAGS=-DTRACY_DELAYED_INIT \
    -DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded \
    "${@:3}"
cmake --build "${src}/build" --config Release --parallel

case "$(uname -s)" in
    Darwin)      name=libTracyClient.dylib ;;
    Linux)       name=libTracyClient.so ;;
    MINGW*|MSYS*|CYGWIN*) name=TracyClient.dll ;;
    *) echo "unsupported platform: $(uname -s)" >&2; exit 1 ;;
esac

built="$(find "${src}/build" -type f \( -name 'TracyClient.dll' -o -name 'libTracyClient.so*' -o -name 'libTracyClient.dylib*' \) -print -quit)"
if [ -z "${built}" ]; then
    echo "no TracyClient library was produced" >&2
    exit 1
fi

mkdir -p "${out}"
cp "${built}" "${out}/${name}"
cp "${src}/LICENSE" "${out}/LICENSE-tracy"
echo "${version}" > "${out}/VERSION"
ls -l "${out}"
