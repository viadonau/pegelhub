#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: worktree-clean.sh [options] [task-name]

Remove a task worktree and its local branch.

Options:
  --prefix <name>   Branch prefix. Default: wt
  --force           Force removal of the worktree and branch
  -h, --help        Show this help

Examples:
  ./scripts/worktree-clean.sh fix-login-timeout
  ./scripts/worktree-clean.sh --prefix feat export-csv
  ./scripts/worktree-clean.sh --force chore-http-cleanup
EOF
}

die() {
  printf 'Error: %s\n' "$1" >&2
  exit 1
}

slugify() {
  printf '%s' "$1" \
    | tr '[:upper:]' '[:lower:]' \
    | sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//; s/-{2,}/-/g'
}

find_branch_worktree() {
  local repo_root="$1"
  local branch_name="$2"
  local current_worktree=""
  local current_branch=""

  while IFS= read -r line; do
    case "$line" in
      worktree\ *)
        current_worktree="${line#worktree }"
        current_branch=""
        ;;
      branch\ refs/heads/*)
        current_branch="${line#branch refs/heads/}"
        if [[ "$current_branch" == "$branch_name" ]]; then
          printf '%s\n' "$current_worktree"
          return 0
        fi
        ;;
      '')
        current_worktree=""
        current_branch=""
        ;;
    esac
  done < <(git -C "$repo_root" worktree list --porcelain)

  return 1
}

BRANCH_PREFIX="wt"
FORCE=0
TASK_NAME=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --prefix)
      [[ $# -ge 2 ]] || die "--prefix requires a value"
      BRANCH_PREFIX="$2"
      shift 2
      ;;
    --force)
      FORCE=1
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

BRANCH_EXISTS=0
if git -C "$REPO_ROOT" show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
  BRANCH_EXISTS=1
fi

REGISTERED_WORKTREE="$(find_branch_worktree "$REPO_ROOT" "$BRANCH_NAME" || true)"
WORKTREE_EXISTS=0
if [[ -e "$WORKTREE_DIR" ]]; then
  WORKTREE_EXISTS=1
fi

if [[ "$BRANCH_EXISTS" -eq 0 && "$WORKTREE_EXISTS" -eq 0 && -z "$REGISTERED_WORKTREE" ]]; then
  die "nothing found for task '$TASK_NAME' (branch '$BRANCH_NAME', worktree '$WORKTREE_DIR')"
fi

if [[ -n "$REGISTERED_WORKTREE" && "$REGISTERED_WORKTREE" != "$WORKTREE_DIR" ]]; then
  die "branch '$BRANCH_NAME' is attached to unexpected worktree '$REGISTERED_WORKTREE'"
fi

if [[ "$REGISTERED_WORKTREE" == "$REPO_ROOT" ]]; then
  die "refusing to remove the main repository worktree"
fi

if [[ -n "$REGISTERED_WORKTREE" ]]; then
  printf 'Removing worktree %s\n' "$REGISTERED_WORKTREE"
  if [[ "$FORCE" -eq 1 ]]; then
    git -C "$REPO_ROOT" worktree remove --force "$REGISTERED_WORKTREE"
  else
    git -C "$REPO_ROOT" worktree remove "$REGISTERED_WORKTREE"
  fi
elif [[ "$WORKTREE_EXISTS" -eq 1 ]]; then
  die "path '$WORKTREE_DIR' exists but is not a registered git worktree"
fi

if [[ "$BRANCH_EXISTS" -eq 1 ]]; then
  printf 'Removing branch %s\n' "$BRANCH_NAME"
  if [[ "$FORCE" -eq 1 ]]; then
    git -C "$REPO_ROOT" branch -D "$BRANCH_NAME"
  else
    git -C "$REPO_ROOT" branch -d "$BRANCH_NAME"
  fi
fi

printf '\nCleaned up:\n'
printf '  Worktree: %s\n' "$WORKTREE_DIR"
printf '  Branch:   %s\n' "$BRANCH_NAME"
