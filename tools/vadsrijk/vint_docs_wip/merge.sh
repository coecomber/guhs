#!/usr/bin/env bash
# usage: merge.sh <slice>
SP="C:/Users/Juiced/AppData/Local/Temp/claude/9315d5c7bc494c38/4d6f12c1-daf4-4a5c-9db4-207807b4e444/scratchpad"
cd /c/Users/Juiced/Documents/MineScapeStory/guhs_vadsrijk/v-int
git merge --no-ff 110-u-$1 -m "110-v-int: merge 110-u-$1

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" > /tmp/merge.log 2>&1
for f in $(git diff --name-only --diff-filter=U); do
  case "$f" in
    src/main/resources/assets/guhs/lang/*.json) python $SP/langunie.py "$f" && git add "$f";;
    */VadsrijkFeature.java|*/client/VadsrijkClient.java) python $SP/beide.py "$f" && git add "$f" && echo "BOTH-SIDES: $f";;
    tools/features/__init__.py) python $SP/beide.py "$f" && python $SP/vadsfix.py && git add "$f";;
  esac
done
python $SP/vadsfix.py
echo "REMAINING:"; R=$(git diff --name-only --diff-filter=U); echo "$R"
if [ -z "$(git diff --name-only --diff-filter=U)" ] && [ -f .git/MERGE_HEAD -o -f "$(git rev-parse --git-dir)/MERGE_HEAD" ]; then git add -A tools/features/__init__.py; git commit -q --no-edit && echo COMMITTED; fi
git log --oneline -1
[ -z "$R" ]
if [ -z "$R" ] && ! git diff --quiet -- tools/features/__init__.py; then git commit -q -m "110-v-int: FEATURES order (vadsrijk_t_vfx last) after merging 110-u-$1

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- tools/features/__init__.py && echo "FEATURES-ORDER COMMITTED"; fi
[ -z "$R" ]
