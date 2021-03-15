# Okapi Integration Tests #

All long running tests for Okapi should go here. Normally round trip tests or any tests that process full files.

**WARNING**

The `logback-test.xml` configuration file in `src\test\resources\` reduces the log level of `net.sf.okapi.common.pipelinedriver` to `WARN`.<br>
Without it the continuous integration build fails. The log limit is 4MB, and we can't change it (controlled by GitLab)<br>
Please don't change the level, and don't remove the file. If you do it in your local copy please don't commit it.

**Example Test File Directory Structure:**


```
#!

xmlstream
   normal.xml
   pcdata_tests
      okf_xmlstream@pcdata.fprm
      okf_html@pcdata.secondary.fprm
      pcdata.xml
```

Each subdirectory is assumed to contain a custom filter config and possible secondary config. Following the Naming convention is important for the test code to auto-detect the config files. Files in the root directory (and sub-directories) are processed with the default parameters of the filter. Then each sub-directory is processed using the custom config files. Also be mindful that the tests include all the file extensions for your test files.

Bilingual file's locales are auto-detected.

These conventions allow large numbers of files and configurations to be tested with standard code. Any files that are added to "integration-tests-common" will be automatically tested by other integration tests that assume the above conventions (for example integration-tests-tkit)

Test files that fail should be given a "_FAIL" suffix. This will mark them so they are not included in the tests and warn developers that these files have issues. Some integration tests are marked with "@Ignore(...)". This may not mean there is a problem, only that comparison of the different files found differences. In some cases the latest code may be correct. Please look at these tests carefully and if possible suggest a better way to compare the results.

Info on how to update the golden files (from Jim):

1.	Build the integration tests in Eclipse (Right Clisk -> Maven -> Update Project). Be sure to select the option to force update of snaphots.
2.	Run the tkit integration tests (net.sf.okapi.tkit.integration) to generate the updated XLIFF.
3.	Navigate to "../okapi-integration-tests/target/test-classes/.." and select the new xliff files. Be sure to also copy over the ones in sub-directories.
4.	Copy the xliff files to the corresponding directories under "../okapi-integration-tests/src/test/resources/XLIFF_M28_05_12_2015"
5.	Do a refresh in eclipse and re-run the XLIFF compare you should see everything pass.