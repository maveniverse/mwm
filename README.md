# Maveniverse MWM

Maveniverse "Maven Workspace Manager" (MWM) is a Maven core extension, targeting **Maven 3.10+ (including Maven 4)** and Java 8+.
It is designed to help user cope with "workspaces". It supports git worktrees also, builds upon [Nisse JGit](https://github.com/maveniverse/nisse) capabilities,
but does not force users to explicitly use Nisse extension.

If MWM detects any advanced usage like "chained" LRM, it will stand aside and not interfere.

The idea: as workspace is dynamically detected, MWM will "follow" your work, and in case of branch change, it will
**automatically switch to new workspace**. This implies that a `mvn install` upon a branch change is needed, otherwise
in case you reduce your build (like using `mvn -f ...` or `-rf :...`) your build will not find the dependencies.

## Use cases

The gist of MWM is to allow branched development without the hassle. Imagine working on all 4 Maven
active branches (master, 4.0, 3.10 and 3.9, as of today). Today, when you switch a branch 
(or build in other directory in case of git worktree checkout), you must ensure local repository 
contains locally built artifacts from current branch. Basically, a common "reflex" today is to 
build full reactor AFTER branch (or directory) switch `mvn install`. But with MWM, this becomes not needed:

* on branch switch, the "workspace" changes (automatically) as well
* still, you must ensure that workspace is populated: on first use, you must perform a full reactor install
* from this moment, you can freely "jump" from branch to branch and never get compile errors (for example, in one branch you added a method to interface, and have compilation failure on another branch, due lack of it).

Sharing workspaces across multiple projects:

* MWM knows the git URL (host, owner and repository name) and the current branch
* besides workspace ID, MWM also mints a workspace "discriminator", that by default ignores remote and repository name
* so discriminator is a "$host+$owner+$branch" function, in example of Maven, it is `github.com-apache-master`
* setting `ws-key=DISCRIMINATOR` makes MWM use the discriminator (instead of the ID) for workspace directories

This results in similar setup as ASF Maven CI has: across multiple (apache) projects, IF you use 
consistently same branch name (ie feature spanning across multiple checkouts), you share 
same workspace, providing you same features as above. Hence, similar as above, but spanning across multiple projects/checkouts.

Future step: MWM (is not yet) could be `localPrefix` provider for split local repository use cases as explained in 
[Local Repository Use Cases](https://maven.apache.org/resolver/local-repository.html#Use_Cases). This would make 
the "switch" happen automatically, while today, users have to proactively provide `localPrefix` themselves.

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

The workspace ID is used to derive workspace output directory, which is by default `$MAVEN_LOCAL_REPOSITORY/installed/$workspaceId`,
or `.mvn/target/installed` within the checkout, if it is a git worktree.

MWM _assumes user performs regular repository hygiene of deleting local repository_. Or even better, as this core
extension is 3.10+, user may use Mimir as well in combination.

To use it, add the following to your project or user-wide (3.10+ feature) `extensions.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
    <extension>
        <groupId>eu.maveniverse.maven.mwm</groupId>
        <artifactId>extension</artifactId>
        <version>${mwm.version}</version>
    </extension>
</extensions>
```

To try it out, one can use `mvn mwm:detect` goal (full coordinates of mojo is `eu.maveniverse.maven.plugins:mwm-plugin3:detect`;
the `mwm` prefix works only if `eu.maveniverse.maven.plugins` is added to `pluginGroups` in your `settings.xml`).

Requirements:
* Java: 8+
* Maven: 3.10+
