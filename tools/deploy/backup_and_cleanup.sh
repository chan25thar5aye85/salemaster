#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# backup_and_cleanup.sh
#
#   1. Backs up app/src to tools/deploy/backups/<timestamp>/
#   2. Runs apply_new_files.sh
#   3. Runs patch_existing.py
#   4. Prints a diff summary (files added / modified)
#   5. Cleans up stray root-level junk files (os, re, sys, the odd class-file-named
#      markdown, etc.) — DRY-RUN unless --clean is passed.
#
# Usage:
#   ./tools/deploy/backup_and_cleanup.sh                # backup + apply + diff
#   ./tools/deploy/backup_and_cleanup.sh --clean        # also delete junk
#   ./tools/deploy/backup_and_cleanup.sh --no-apply     # backup only
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

CLEAN=0
APPLY=1
for arg in "$@"; do
    case "$arg" in
        --clean)    CLEAN=1 ;;
        --no-apply) APPLY=0 ;;
    esac
done

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

TS="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="tools/deploy/backups/$TS"
mkdir -p "$BACKUP_DIR"

echo "═══ BACKUP ═══"
echo "→ $BACKUP_DIR"
tar -C app -cf - src \
    | tar -C "$BACKUP_DIR" -xf -
echo "✅ Backup complete"
echo

if [[ $APPLY -eq 0 ]]; then
    echo "Skipping apply (--no-apply)."
    exit 0
fi

echo "═══ APPLY NEW FILES ═══"
./tools/deploy/apply_new_files.sh
echo

echo "═══ PATCH EXISTING FILES ═══"
python3 ./tools/deploy/patch_existing.py
echo

echo "═══ DIFF SUMMARY ═══"
if command -v git >/dev/null 2>&1 && git -C "$REPO_ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git -C "$REPO_ROOT" status --porcelain app/src \
        | awk '{
            status=$1; path=$2
            if (status == "??") print "  NEW     " path
            else if (status ~ /M/) print "  MODIFY  " path
            else print "  " status "  " path
        }'
else
    echo "  (not a git repo — compare manually against $BACKUP_DIR)"
fi
echo

# ── Junk cleanup (dry-run unless --clean) ──
JUNK=(
    "os"
    "re"
    "sys"
    "com.akari.retailer.features.expense.presentation.ExpenseAnalyticsScreen"
)

echo "═══ JUNK FILES AT REPO ROOT ═══"
found_junk=0
for f in "${JUNK[@]}"; do
    if [[ -e "$REPO_ROOT/$f" ]]; then
        found_junk=1
        if [[ $CLEAN -eq 1 ]]; then
            rm -f "$REPO_ROOT/$f"
            echo "  🗑  removed $f"
        else
            echo "  ⚠  $f  (would remove — pass --clean)"
        fi
    fi
done
[[ $found_junk -eq 0 ]] && echo "  none"

echo
echo "═══ DONE ═══"
echo "Backup: $BACKUP_DIR"
if [[ $CLEAN -eq 0 && $found_junk -eq 1 ]]; then
    echo "Run again with --clean to delete the junk files above."
fi
echo
echo "Next:"
echo "  ./gradlew :app:assembleDebug"
