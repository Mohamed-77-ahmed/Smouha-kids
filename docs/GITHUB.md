# The GitHub repository and the pull-request flow

The project lives in a **private** GitHub repository named `Smouha-kids` (`https://github.com/<owner>/Smouha-kids`). The owner is the user's GitHub account; its name is a real name, so the full address is kept out of every committed file (the pre-push scan below looks for real names) and is given to Claude Code in the change request. The repository stays private until the doctor or the hospital agrees and a license is chosen (README "License").

Since batch 7 Claude Code may use GitHub, but only as described in CLAUDE.md section 2 "GitHub access". This page is the how-to for both the user and Claude Code.

## Status (2026-10-19)

Not set up yet. Two things are missing on this computer, and both are the user's to do:

1. **Git for Windows**: download it from https://git-scm.com/download/win, install with the default options, open PowerShell again and check `git --version`. Once per computer, the commit name and e-mail (GitHub's private "noreply" address keeps the real e-mail out of the history; GitHub > Settings > Emails):

   ```powershell
   git config --global user.name "<your name>"
   git config --global user.email "<the GitHub noreply address>"
   ```

2. **`kh.env`** in the project folder (a file, or a folder holding the file) with one line `GIT-HUB-PAT=<token>`: a fine-grained personal access token that can reach only `Smouha-kids`, with "Contents", "Pull requests" and "Issues" read and write and nothing else. `kh.env` is ignored by git and never leaves the computer.

Until then nothing has been committed or pushed, and no token has been read.

## How the token is used

- The value of `GIT-HUB-PAT` is read from `kh.env` by parsing the line (the key has hyphens, so the file is never "sourced"), only for the command that needs it, as an environment variable of that one command (for example `GH_TOKEN`) or a per-command auth header.
- It is never printed, echoed or logged, and never written into any file, commit, issue, pull request, release note, `.git/config`, remote URL, credential store, document or report. After every session `git config --get-regexp .` and `git remote -v` show no token.
- An auth or permission error stops the work and is reported; it is never retried in a loop.

## What goes into the repository

Ignored before anything is staged (`.gitignore`, each path checked with `git check-ignore`): `kh.env`, `*.env`, `keystore.properties`, `*.jks`, `*.keystore`, `local.properties`, every build folder, `.gradle`, `dist/` (the Drive folders: rebuilt by `scripts/build-dist.ps1`; the app file and the PDFs go to a draft GitHub release instead), `docs/evidence/` (except its `tools/`), device dumps, `dist/link.txt`, `scripts/scan-names.txt`, `docs/final-plan.md`, and the doctor's raw originals in the project root (`design/brand/` holds their exact copies and is the source of truth). `.gitattributes` keeps `gradlew` with LF line endings and `*.bat` / `*.cmd` with CRLF; `gradlew` is executable in the index. `git log --all -- kh.env` must show nothing; if it ever does, the token must be revoked.

Never committed: the APK and the PDFs.

## The pre-push scan (before every push)

Over the **full history** that the push would send, not only the current files. It reports counts only, never the matched text:

- the token value, and strings that look like GitHub tokens (`ghp_`, `gho_`, `github_pat_`);
- the passwords of `keystore.properties` (read by the scan in memory, never printed);
- keystore files and every excluded path above;
- the real names in text files and in pictures (`scripts/scan-names.ps1`, its list in the ignored `scripts/scan-names.txt`; pictures are looked at);
- any file over 10 MB.

Any hit: stop, unstage, report. If the local history fails the scan, it is not pushed: a fresh orphan `main` is made from the clean current tree and the old history stays only in a local branch.

## The flow

1. **Baseline (once):** the state before a change request is pushed to `main`. If GitHub created a README, `.gitignore` or license, they are merged keeping ours; any other conflict stops the work. Never a force push.
2. **Each change request:** a branch from `main` (for example `batch-7-koko-dawa`), small commits in Conventional Commits style (`feat:`, `fix:`, `docs:`, `test:`, `build:`, `chore:`), the pre-push scan, a push of the branch, and a pull request to `main` whose body says what changed, which tests actually ran with their results, what is UNVERIFIED, and links to the screenshots. **Claude Code never merges a pull request**: the user reviews and merges it on GitHub.
3. **Release:** a **draft** GitHub release (for example `v1.1.0`) targeting the branch, with short Arabic and English notes and the files `Koko-Dawa-<version>.apk`, `SHA256.txt`, `2_طريقة_التثبيت.pdf` and `3_طريقة_الاستخدام.pdf` attached. The user publishes it (or not); families still get the app through Google Drive (`dist/DRIVE.md`).
4. **Issues:** one per manual step, UNVERIFIED item and BLOCKED asset of `docs/FINAL_REPORT.md`, with the labels `manual`, `unverified`, `blocked` and `doctor-approval`; duplicates are checked first. No token, no real names, no personal data in any issue.

## Never

Force push; deleting or rewriting branches, tags, releases or the repository; changing the repository's settings, visibility, collaborators, secrets or webhooks; creating or editing GitHub Actions workflows; merging pull requests; touching any other repository; creating tokens or keys.

## Checks after a push

- [ ] The branch and the pull request exist; the release is a draft with its 4 files and the APK's size matches the local file.
- [ ] Open the repository address in a private window (not logged in): it must say "404 / not found", which proves it is private.
- [ ] No `keystore.properties`, `*.jks`, `kh.env`, `dist/`, `docs/evidence/batch*`, `docs/final-plan.md` in the file list.
- [ ] `git config --get-regexp .` and `git remote -v` show no token.
