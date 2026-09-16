#!/usr/bin/env sh
# Read-only local resource report.
set -eu

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -W)
    if command -v pwsh.exe >/dev/null 2>&1; then
      exec pwsh.exe -NoProfile -File "$script_dir/system-info.ps1"
    fi
    exec powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$script_dir/system-info.ps1"
    ;;
esac

first_line() {
  if command -v "$1" >/dev/null 2>&1; then
    "$@" 2>&1 | sed -n '1p'
  else
    printf 'unavailable\n'
  fi
}

os=$(uname -srm)
if [ -r /proc/cpuinfo ]; then
  cpu=$(sed -n 's/^model name[[:space:]]*:[[:space:]]*//p' /proc/cpuinfo | sed -n '1p')
  total_kb=$(awk '/^MemTotal:/ {print $2}' /proc/meminfo)
  available_kb=$(awk '/^MemAvailable:/ {print $2}' /proc/meminfo)
elif [ "$(uname -s)" = Darwin ]; then
  cpu=$(sysctl -n machdep.cpu.brand_string)
  total_kb=$(($(sysctl -n hw.memsize) / 1024))
  page_size=$(pagesize)
  available_pages=$(vm_stat | awk -F: '/Pages free:|Pages inactive:/ {gsub(/[^0-9]/,"",$2); sum += $2} END {print sum+0}')
  available_kb=$((available_pages * page_size / 1024))
else
  cpu=unavailable
  total_kb=0
  available_kb=0
fi
free_kb=$(df -Pk . | awk 'NR==2 {print $4}')
printf 'Operating system: %s\n' "$os"
printf 'CPU: %s\n' "$cpu"
awk -v kb="$total_kb" 'BEGIN {printf "Total RAM: %.1f GiB\n", kb/1048576}'
awk -v kb="$available_kb" 'BEGIN {printf "Available RAM: %.1f GiB\n", kb/1048576}'
awk -v kb="$free_kb" 'BEGIN {printf "Free disk space (current filesystem): %.1f GiB\n", kb/1048576}'
printf 'Java version: %s\n' "$(first_line java -version)"
printf 'Node version: %s\n' "$(first_line node --version)"
printf 'Docker version: %s\n' "$(first_line docker --version)"
printf 'Git version: %s\n' "$(first_line git --version)"
