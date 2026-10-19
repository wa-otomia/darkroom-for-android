#!/bin/sh
# Reproducible OpenSpec CLI for maintainers; product builds do not depend on it.
set -eu
# A registered private Store has a separate real spec root.
project_cli=$(command -v pi-project 2>/dev/null || true)
if [ -z "$project_cli" ] && [ -x "$HOME/.pi-env/bin/pi-project" ]; then
  project_cli="$HOME/.pi-env/bin/pi-project"
fi
if [ -n "$project_cli" ] && command -v node >/dev/null 2>&1; then
  project_context=$("$project_cli" context --json 2>/dev/null || true)
  spec_root=$(printf '%s' "$project_context" | node -e '
    let input = "";
    process.stdin.on("data", data => input += data);
    process.stdin.on("end", () => {
      try {
        const context = JSON.parse(input);
        if (context.specMode === "store" && typeof context.specRoot === "string") {
          process.stdout.write(context.specRoot);
        }
      } catch {}
    });
  ')
  if [ -n "$spec_root" ]; then
    if [ ! -d "$spec_root/openspec" ]; then
      echo 'Registered OpenSpec Store is unavailable.' >&2
      exit 1
    fi
    cd "$spec_root"
  fi
fi
export OPENSPEC_TELEMETRY=0
# Reuse the installed pinned CLI without fetching npm metadata.
native_cli=$(command -v pi-openspec 2>/dev/null || true)
if [ -z "$native_cli" ] && [ -x "$HOME/.pi-env/bin/pi-openspec" ]; then
  native_cli="$HOME/.pi-env/bin/pi-openspec"
fi
if [ -n "$native_cli" ] && [ "$("$native_cli" --version)" = '1.13.1' ]; then
  exec "$native_cli" "$@"
fi
if ! command -v npx >/dev/null 2>&1; then
  echo 'OpenSpec requires Node.js >= 20.19 and npm (npx).' >&2
  exit 1
fi
exec npx --yes --package=@fission-ai/openspec@1.13.1 openspec "$@"
