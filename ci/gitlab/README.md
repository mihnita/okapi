# Overview

By adding a `.gitlab-ci.yml` file to the root directory of the source
repository and configuring the GitLab project to use
[a Runner](https://docs.gitlab.com/ee/ci/runners/README.html) you are
activating [GitLab's continuous integration service](https://about.gitlab.com/product/continuous-integration),
which in its turn will give you an ability to automatically trigger
your CI [pipeline](https://docs.gitlab.com/ee/ci/pipelines.html) for
each push to the repository. For more general information please refer
to [the getting started guide](https://docs.gitlab.com/ee/ci/quick_start/README.html).


# Bitbucket integration

GitLab CI/CD can be used with GitHub or any other Git server. Instead
of moving the entire project to GitLab, we will connect our Butbucket
repository to get the benefits of GitLab CI/CD. That will set up
repository mirroring and create a lightweight project where issues,
merge requests, wiki, and snippets disabled (these features can be
re-enabled later).

Below are the steps required to be taken.

1. In GitLab create a **CI/CD for external repo**, select **Repo by URL**
and create the project. GitLab will import the repository and enable
Pull Mirroring.

2. In GitLab create a [Personal Access Token](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
with api scope. This will be used to authenticate requests from the web
hook that will be created in Bitbucket to notify GitLab of new commits.

3. In Bitbucket from **Settings > Webhooks** create a new web hook to
notify GitLab of new commits.

    The web hook URL should be set to the GitLab API to trigger pull
    mirroring, using the Personal Access Token, which has been just
    generated for the authentication. The web hook Trigger should be set
    to ‘Repository Push’.

    ```
    https://gitlab.com/api/v4/projects/<CI_PROJECT_ID>/mirror/pull?private_token=<PERSONAL_ACCESS_TOKEN>
    ```

    `PERSONAL_ACCESS_TOKEN`: the generated personal access token

    `CI_PROJECT_ID`: the GitLab project ID, it can be found on the
     landing page of the project

4. In Bitbucket create an **App Password** from Bitbucket **Settings >
App Passwords** to authenticate the build status script setting commit
build statuses in Bitbucket. Repository write permissions are required.

5. In GitLab from **Settings > CI/CD > Variables** add variables to
allow communication with Bitbucket via the Bitbucket API.

    `BITBUCKET_ACCESS_TOKEN`: the Bitbucket app password created above

    `BITBUCKET_USERNAME`: the username of the Bitbucket account

    `BITBUCKET_NAMESPACE`: set this if your GitLab and Bitbucket
    namespaces differ

    `BITBUCKET_REPOSITORY`: set this if your GitLab and Bitbucket
    project names differ

The required `build-status.sh` script can be found under `ci/gitlab` path.

GitLab should now be configured to mirror changes from Bitbucket,
run CI/CD pipelines configured in `.gitlab-ci.yml` and push the status
to Bitbucket.

For extra details please refer to [the original GitLab guide](https://docs.gitlab.com/ee/ci/ci_cd_for_external_repos/bitbucket_integration.html) .

# Triggering the Okapi project pipeline from the XLIFF Toolkit project pipeline

When you get done with setting up the Okapi project, make sure a trigger
token is available for sharing with the XLIFF Toolkit project.

You can add a new trigger by going to the project’s **Settings > CI/CD**
under **Triggers**. The **Add trigger** button will create a new token
which you can then use to trigger a rerun of this particular project’s
pipeline. For more information please refer to the corresponding section
of GitLab's documentation
[here](https://docs.gitlab.com/ee/ci/triggers/#adding-a-new-trigger).

# Sonatype integration

The following secret variables have to be declared under
**Settings > CI/CD > Variables**:

`MAVEN_REPO_USER`: sonatype user

`MAVEN_REPO_PASS`: sonatype user's password

`OPENSSL_ENC_KEY`: the OpenSSL key for decoding the code signing key

`OPENSSL_ENC_IV`: the OpenSSL initialisation vector for decoding the code signing key

`GPG_PASSPHRASE`: the pass-phrase for the code signing key

# Docker image creation & publishing

See https://gitlab.com/okapiframework/okapi/container_registry

But the "TLDR" flow is:
```
docker login registry.gitlab.com
# use okapiframework.robot with the a Personal Access Token for login

docker build -t registry.gitlab.com/okapiframework/okapi:latest .
docker push registry.gitlab.com/okapiframework/okapi:latest
```

