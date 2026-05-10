#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: worktree-start.sh [options] [task-name]

Create a task branch and sibling worktree, then optionally open it in IntelliJ.

Options:
  --base <ref>      Base ref to branch from. Default: origin/main
  --prefix <name>   Branch prefix. Default: wt
  --no-idea         Do not try to open IntelliJ
  -h, --help        Show this help

Examples:
  ./scripts/worktree-start.sh fix-login-timeout
  ./scripts/worktree-start.sh --base origin/main --prefix feat export-csv
  ./scripts/worktree-start.sh --no-idea chore-http-cleanup
EOF
}

die() {
  printf 'Error: %s\n' "$1" >&2
  exit 1
}

is_wsl() {
  [[ -r /proc/version ]] && grep -qi microsoft /proc/version
}

find_idea_exe() {
  local jetbrains_dir="/mnt/c/Program Files/JetBrains"
  local candidate

  [[ -d "$jetbrains_dir" ]] || return 1

  while IFS= read -r candidate; do
    printf '%s\n' "$candidate"
    return 0
  done < <(find "$jetbrains_dir" -maxdepth 3 -name idea64.exe 2>/dev/null | sort -r)

  return 1
}

open_idea() {
  local worktree_dir="$1"
  local idea_exe

  if command -v idea >/dev/null 2>&1; then
    printf 'Opening IntelliJ via idea\n'
    idea "$worktree_dir"
    return 0
  fi

  if is_wsl && command -v wslpath >/dev/null 2>&1; then
    idea_exe="$(find_idea_exe || true)"
    if [[ -n "$idea_exe" ]]; then
      printf 'Opening IntelliJ via %s\n' "$idea_exe"
      "$idea_exe" "$(wslpath -w "$worktree_dir")"
      return 0
    fi
  fi

  printf "IntelliJ launcher 'idea' is not available; open this path manually.\n" >&2
  return 1
}

slugify() {
  printf '%s' "$1" \
    | tr '[:upper:]' '[:lower:]' \
    | sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//; s/-{2,}/-/g'
}

BASE_REF="origin/main"
BRANCH_PREFIX="wt"
OPEN_IDEA=1
TASK_NAME=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base)
      [[ $# -ge 2 ]] || die "--base requires a value"
      BASE_REF="$2"
      shift 2
      ;;
    --prefix)
      [[ $# -ge 2 ]] || die "--prefix requires a value"
      BRANCH_PREFIX="$2"
      shift 2
      ;;
    --no-idea)
      OPEN_IDEA=0
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    -*)
      die "unknown option: $1"
      ;;
    *)
      [[ -z "$TASK_NAME" ]] || die "task name was provided more than once"
      TASK_NAME="$1"
      shift
      ;;
  esac
done

if [[ -z "$TASK_NAME" ]]; then
  read -r -p "Short task name: " TASK_NAME
fi

[[ -n "$TASK_NAME" ]] || die "task name must not be empty"

TASK_SLUG="$(slugify "$TASK_NAME")"
[[ -n "$TASK_SLUG" ]] || die "task name did not contain usable characters"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
REPO_NAME="$(basename "$REPO_ROOT")"
PARENT_DIR="$(dirname "$REPO_ROOT")"
WORKTREE_ROOT="$PARENT_DIR/.worktrees"
WORKTREE_DIR="$WORKTREE_ROOT/$REPO_NAME-$TASK_SLUG"
BRANCH_NAME="$BRANCH_PREFIX/$TASK_SLUG"

git -C "$REPO_ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "not inside a git repository"
git -C "$REPO_ROOT" rev-parse --verify "$BASE_REF^{commit}" >/dev/null 2>&1 || die "base ref '$BASE_REF' does not exist locally"

if git -C "$REPO_ROOT" show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
  die "branch '$BRANCH_NAME' already exists"
fi

if [[ -e "$WORKTREE_DIR" ]]; then
  die "worktree path '$WORKTREE_DIR' already exists"
fi

mkdir -p "$WORKTREE_ROOT"

printf 'Creating branch %s from %s\n' "$BRANCH_NAME" "$BASE_REF"
git -C "$REPO_ROOT" worktree add -b "$BRANCH_NAME" "$WORKTREE_DIR" "$BASE_REF"

printf 'Worktree: %s\n' "$WORKTREE_DIR"
printf 'Branch:   %s\n' "$BRANCH_NAME"

if [[ "$OPEN_IDEA" -eq 1 ]]; then
  open_idea "$WORKTREE_DIR" || true
fi

printf '\nNext steps:\n'
printf '  cd %q\n' "$WORKTREE_DIR"
printf '  codex\n'
