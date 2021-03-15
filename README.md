**About Okapi Framework**

The **Okapi Framework** is a set of interface specifications, format definitions, components and applications that provides a cross-platform environment to build interoperable tools for the different steps of the translation and localization process.

The goal of the **Okapi Framework** is to allow tools developers and localizers to build new localization processes or enhance existing ones to best meet their needs, while preserving a level of compatibility and interoperability. It also provides them with a way to share (and re-use) components across different solutions.

Okapi code is developed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0).

**Useful links**

 * **[Latest release of the distributions](https://bintray.com/okapi/Distribution)**
 * [Latest snapshot of the distributions (nightly build)](https://gitlab.com/okapiframework/okapi/-/jobs/artifacts/dev/browse/deployment/maven/done?job=verification:jdk8)
 * [The main Okapi Framework Web site](http://okapiframework.org/wiki/index.php?title=Main_Page)
 * [Users group](https://groups.google.com/forum/#!forum/okapi-users)
 * [A word about using open standards](http://okapiframework.org/wiki/index.php?title=Open_Standards)
 * [How to Contribute](https://bitbucket.org/okapiframework/okapi/wiki/How%20to%20Contribute.md)
 * [Contributor License Agreement](https://bitbucket.org/okapiframework/okapi/wiki/ContributorLicenseAgreement.md)
 * [Consultancy](https://bitbucket.org/okapiframework/okapi/wiki/Consultancy.md)

**Maven repositories**

Okapi is available in Maven Central, so unless you want to use the snapshot version
there is no reason to do anything in you `pom.xml` other than add dependencies.

But just in case, here it is:

 * [Okapi artifacts releases](https://search.maven.org/search?q=net.sf.okapi): https://search.maven.org/search?q=net.sf.okapi
 * [Okapi artifacts snapshots](https://oss.sonatype.org/content/repositories/snapshots/): https://oss.sonatype.org/content/repositories/snapshots/

**Build status on GitLab**

Pipeline | Status (`master`) | Status (`dev`) 
-------- | --------------- | ------------
Okapi | [![pipeline status](https://gitlab.com/okapiframework/okapi/badges/master/pipeline.svg)](https://gitlab.com/okapiframework/okapi/commits/master) | [![pipeline status](https://gitlab.com/okapiframework/okapi/badges/dev/pipeline.svg)](https://gitlab.com/okapiframework/okapi/commits/dev)
XLIFF Toolkit | [![pipeline status](https://gitlab.com/okapiframework/xliff-toolkit/badges/master/pipeline.svg)](https://gitlab.com/okapiframework/xliff-toolkit/commits/master) | [![pipeline status](https://gitlab.com/okapiframework/xliff-toolkit/badges/dev/pipeline.svg)](https://gitlab.com/okapiframework/xliff-toolkit/commits/dev)
Okapi Integration Tests | [![pipeline status](https://gitlab.com/okapiframework/okapi-integration-tests/badges/master/pipeline.svg)](https://gitlab.com/okapiframework/okapi-integration-tests/commits/master) | **N/A** (`master` only)
Longhorn | [![pipeline status](https://gitlab.com/okapiframework/longhorn/badges/master/pipeline.svg)](https://gitlab.com/okapiframework/longhorn/commits/master) | [![pipeline status](https://gitlab.com/okapiframework/longhorn/badges/dev/pipeline.svg)](https://gitlab.com/okapiframework/longhorn/commits/dev)
Longhorn JS Client | [![pipeline status](https://gitlab.com/okapiframework/longhorn-js-client/badges/master/pipeline.svg)](https://gitlab.com/okapiframework/longhorn-js-client/commits/master) | [![pipeline status](https://gitlab.com/okapiframework/longhorn-js-client/badges/dev/pipeline.svg)](https://gitlab.com/okapiframework/longhorn-js-client/commits/dev)
OmegaT Plugin | [![pipeline status](https://gitlab.com/okapiframework/omegat-plugin/badges/master/pipeline.svg)](https://gitlab.com/okapiframework/omegat-plugin/commits/master) | [![pipeline status](https://gitlab.com/okapiframework/omegat-plugin/badges/dev/pipeline.svg)](https://gitlab.com/okapiframework/omegat-plugin/commits/dev)
Olifant | [![pipeline status](https://gitlab.com/okapiframework/olifant/badges/master/pipeline.svg)](https://gitlab.com/okapiframework/olifant/commits/master) | [![pipeline status](https://gitlab.com/okapiframework/olifant/badges/dev/pipeline.svg)](https://gitlab.com/okapiframework/olifant/commits/dev)



**Example of tools and applications using Okapi**

 * [Ratel](http://okapiframework.org/wiki/index.php?title=Ratel) - an editor for [SRX (segmentation rules)](http://okapiframework.org/wiki/index.php?title=SRX).
 * [Pangolin](https://github.com/davidmason/Pangolin) - an on-line editor for SRX files.
 * [Rainbow](http://okapiframework.org/wiki/index.php?title=Rainbow) - a localization toolbox that makes use of standards like [XLIFF](http://okapiframework.org/wiki/index.php?title=Open_Standards#XLIFF) and [TMX](http://okapiframework.org/wiki/index.php?title=Open_Standards#TMX), supports a wide range of [formats](http://okapiframework.org/wiki/index.php?title=Filters) and offers many [features](http://okapiframework.org/wiki/index.php?title=Steps).
 * [CheckMate](http://okapiframework.org/wiki/index.php?title=CheckMate) - an application to perform various quality checks on bilingual translated documents.
 * [Tikal](http://okapiframework.org/wiki/index.php?title=Tikal) - a command-line tool for extracting/merging XLIFF files and do many other tasks.
 * [Filters plugin for OmegaT](http://okapiframework.org/wiki/index.php?title=Okapi_Filters_Plugin_for_OmegaT) - an [OmegaT](http://www.omegat.org/) plugin to use Okapi filters.
 * [Longhorn](http://okapiframework.org/wiki/index.php?title=Longhorn) - a batch processing server.
 * [iL10Nz](http://www.myl10n.net/il10nz/) - an online localization system
 * [Okapi-ant](https://github.com/tingley/okapi-ant) - several Ant tasks to process files with Okapi components.
 * See some [screenshots](http://okapiframework.org/wiki/index.php?title=Screenshots)
