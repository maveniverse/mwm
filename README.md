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

Note: MWM writes its files into `.mvn/target/` directory of the checkout (workspace state, and in case of
git worktree, the installed artifacts as well). Add `.mvn/target/` to your `.gitignore`, just like `target/`.

## Configuration

MWM is configured with a Java properties file named `mwm.properties`, looked up in this order (the first file
found is used as whole, files are not merged):

1. `.mvn/target/mwm.properties` in the checkout
2. in case of git worktree, `.mvn/target/mwm.properties` in the main checkout
3. `~/.m2/mwm.properties`

| Key                      | Default             | Meaning                                                                                                     |
|--------------------------|---------------------|-------------------------------------------------------------------------------------------------------------|
| `build-cache-scope`      | `USER`              | Where downloaded artifacts are cached; see scopes below.                                                    |
| `build-output-scope`     | `AUTO`              | Where built artifacts are installed; see scopes below.                                                      |
| `ws-key`                 | `ID`                | Which one names the workspace directories: `ID` or `DISCRIMINATOR` (allows sharing across projects).        |
| `discriminator-elements` | `HOST,OWNER,BRANCH` | Comma separated elements of discriminator, out of `REMOTE`, `HOST`, `OWNER`, `REPOSITORY`, `BRANCH`.        |
| `worktree-joined`        | `true`              | In a git worktree, use the main checkout workspace as well (for artifacts not built in worktree).           |
| `link-enforced`          | `true`              | Fail the build if a linked workspace cannot be detected; if `false`, it is skipped with a warning.          |
| `cached-dir`             | `cached`            | Directory name used for cached artifacts in `PROJECT` and `USER_SCOPED` scopes.                             |
| `installed-dir`          | `installed`         | Directory name used for installed artifacts in `PROJECT` and `USER_SCOPED` scopes.                          |

Scopes:

| Scope         | Location                                                                                    |
|---------------|---------------------------------------------------------------------------------------------|
| `AUTO`        | Cache: `USER`. Output: `PROJECT` in a git worktree, otherwise `USER_SCOPED`.                |
| `PROJECT`     | `.mvn/target/cached` or `.mvn/target/installed` in the checkout.                            |
| `USER_SCOPED` | `$MAVEN_LOCAL_REPOSITORY/cached/$workspace` or `$MAVEN_LOCAL_REPOSITORY/installed/$workspace` |
| `USER`        | `$MAVEN_LOCAL_REPOSITORY`, as Maven does by default.                                        |

When both scopes resolve to `USER` (`AUTO` included), MWM stays inactive. Workspace ID parts that are not safe for directory names
are altered: for example, branch `feature/x` becomes `feature_x-<hash>`.

MWM also stays inactive if:
* it is disabled with `-Dmwm.disabled=true`
* split local repository is enabled (`aether.lrm.enhanced.split`)
* chained local repository is used

## Goals

The `mwm-plugin3` Maven Plugin (coordinates `eu.maveniverse.maven.plugins:mwm-plugin3`) has these goals (the `mwm`
prefix works only if `eu.maveniverse.maven.plugins` is added to `pluginGroups` in your `settings.xml`):

* `mvn mwm:detect`: shows the detected workspace, its directories and linked workspaces.
* `mvn mwm:link -Dmwm.other=../other-project`: links the workspace of other checkout to current workspace, so
  artifacts installed by other checkout are visible to current one.
* `mvn mwm:unlink -Dmwm.other=../other-project`: removes such a link.

Links are stored in `.mvn/target/$workspaceId.properties`, so they belong to the workspace, hence to the current
branch: a link made on `main` branch is not present on other branches.

## Requirements

* Java: 8+
* Maven: 3.10+
