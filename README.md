# Maveniverse MWM

Maveninverse "Maven Workspace Manager" (MWM) is Maven core extension, targeting **exclusively Maven 3.10+** and Java 8+.
It is designed to help user cope with "workspaces". It supports git worktrees also.

If MWM detects any advanced usage like "chained" LRM, it will stand aside and not interfere.

In simplest case, MWM will inspect the checkout (only `git` supported for now) and using Nisse Core (if Nisse 
extension is present, it will reuse its results) will try to figure out the workspace.

Note: MWM will look for "git remotes" in order as: `upstream`, `origin`. Hence, it expects that users when using
forks, name the upstream as "upstream" and the fork as "origin".

Currently, workspace ID is in form of `$remoteName - $host - $owner - $repoName - $branchName`.

Example workspace IDs:
* `upstream-github.com-apache-maven-maven-3.10.x` when using forked repository against https://github.com/apache/maven/tree/maven-3.10.x branch (Note: locally this is a git worktree, in fact).
* `origin-github.com-maveniverse-mwm-main` when using forked repository against https://github.com/maveniverse/mwm/tree/main branch (no fork).

Workspace ID is used to derive workspace output directory, which is by default: `$MAVEN_LOCAL_REPOSITORY/.mwm/$workspaceId`.

MWM _assumes user performs regular repository hygiene of deleting local repository_. Or even better, as this core
extension is 3.10+, user may use Mimir as well in combination.

* Requirements:
* Java: 8+
* Maven: 3.10+
