# Maveniverse MWM

Maveninverse "Maven Workspace Manager" (MWM) is Maven core extension, targeting **exclusively Maven 3.10+** and Java 8+.
It is designed to help user cope with "workspaces". It supports git worktrees also.

If MWM detects any advanced usage like "chained" LRM, it will stand aside and not interfere.

The idea: as workspace is dynamically detected, MWM will "follow" your work, and in case of branch change, it will
**automatically switch to new workspace**. This implies that a `mvn install` upon a branch change is needed, otherwise
if you reduce you build (like using `mvn -f ...` or `-rf :...`) your build will not find the dependencies.

## Use cases

The gist of MWM is to allow branched development without the hassle. Imagine working on Maven all 4 
active branches (master, 4.0, 3.10 and 3.9, as of today). Today, when you switch a branch 
(or build in other directory in case of git worktree checkout), you must ensure local repository 
contains locally built artifacts from current branch. Basically, a common "reflex" today is to 
build full reactor AFTER branch (or directory) switch `mvn install`. But with MWM, this becomes not needed:

* on branch switch, the "workspace" changes (automatically) as well
* still, you must ensure that workspace is populated, (on first use) you do must perform full reactor install
* from this moment, you can freely "jump" from branch to branch and never get compile errors (for example, in one branch you added a method to interface, and have compilation failure on another branch, due lack of it).

Future steps: share workspaces across multiple projects. Here is an:

* given MWM knows the git URL (host+owner+repositoer name) and the current branch
* ignore the repository name in minting WS ID
* identify the branch only
* so WS ID becomes "$owner+branch" function, in example of Maven, it could be `apache-master`

This would result in similar setup as ASF Maven CI has: across multiple (apache) projects, IF you use 
consistently same branch name (ie feature spanning across multiple checkouts), you COULD share 
same workspace, providing you same features as above. Hence, similar as above, but spanning across multiple projects/checkouts.

Another example: MWM (is not yet) could be `localPrefix` provider for split local repository use cases as explained in 
[Local Repository Use Cases](https://maven.apache.org/resolver/local-repository.html#Use_Cases). This would make 
the "switch" happen automatically, while today, user has to proactively provide `localPrefix` by himself.

## Using it

When using this extension, best is to start with _empty local repository_. Your local repository will turn into "cache only"
and the workspace will be used as build output directory (where project is "installed"). This, combined with Mimir
makes full experience.

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

To use it, add the following to your project or user-wide `extensions.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
    <extension>
        <groupId>eu.maveniverse.maven.mwm</groupId>
        <artifactId>extension3</artifactId>
        <version>${last-mvm-release-version}</version>
    </extension>
</extensions>
```

To try it out, one can use `mvn mwn:status` goal (full coordinates of mojo is `eu.maveniverse.maven.plugins:mwm-plugin3:status`).

* Requirements:
* Java: 8+
* Maven: 3.10+
