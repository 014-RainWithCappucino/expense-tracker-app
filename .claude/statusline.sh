#!/usr/bin/env bash
# Statusline: shows which milestone is active and how far the project has got.
# Reads docs/milestones.md so there is one source of truth, not two.
set -u

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
file="$root/docs/milestones.md"
[ -f "$file" ] && [ -r "$file" ] || { printf 'no docs/milestones.md'; exit 0; }

dim=$'\033[2m'; bold=$'\033[1m'; lime=$'\033[38;5;191m'; grey=$'\033[38;5;245m'; off=$'\033[0m'

# Top-level milestones only (no leading indent); sub-steps are indented.
done_n=0 total=0 active="" active_sub="" next=""
while IFS= read -r line; do
  case "$line" in
    "- ["*)
      total=$((total + 1))
      label="${line:6}"   # strip exactly "- [x] "
      case "$line" in
        "- [x]"*) done_n=$((done_n + 1)) ;;
        "- [>]"*) active="$label" ;;
        "- [ ]"*) [ -n "$active" ] && [ -z "$next" ] && next="${label%% —*}" ;;
      esac
      ;;
    "  - [>]"*)
      [ -n "$active" ] && [ -z "$active_sub" ] && active_sub="${line:8}"
      ;;
  esac
done < "$file"

[ "$total" -gt 0 ] || { printf 'milestones.md has no entries'; exit 0; }

if [ -z "$active" ]; then
  printf '%s%s%s all %d milestones done' "$lime" "✔" "$off" "$total"
else
  id="${active%% —*}"
  title="${active#*— }"
  printf '%s%s%s %s%s%s' "$bold$lime" "$id" "$off" "$grey" "${title:0:44}" "$off"
  [ -n "$active_sub" ] && printf ' %s▸ %s%s' "$dim" "${active_sub:0:52}" "$off"
fi

printf ' %s· %d/%d%s' "$dim" "$done_n" "$total" "$off"
[ -n "$next" ] && printf ' %s· next %s%s' "$dim" "$next" "$off"
