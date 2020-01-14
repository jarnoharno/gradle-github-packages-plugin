# Gradle Github Packages Plugin

Use Github Packages repositories with credentials from your hub config file.

## Usage

```groovy
// Source repository for the Github Packages plugin.
buildscript {
    repositories {
        maven {
            url = uri('https://maven.pkg.github.com/jarnoharno/packages')
        }
    }
    dependencies {
        classpath 'com.jarnoharno:gradle-github-packages-plugin:1.0.0'
    }
}

plugins {
    id 'java'
    id 'maven-publish'
}

// Apply Github Packages plugin.
apply plugin: 'com.jarnoharno.github-packages'

githubPackages {

    // Github Packages repository name.
    repository = 'jarnoharno/example-packages'

    // Github username. By default this is extracted from hub config
    // file (~/.config/hub). Can also be overridden with
    // githubPackages.username gradle property.
    username

    // Github access token. By default this is extracted from hub config
    // file (~/.config/hub). Can also be overridden with
    // githubPackages.accessToken gradle property.
    accessToken
}

repositories {

    // Declares a Github Packages maven repository.
    maven githubPackages()

    // Declares a Github Packages maven repository overriding the global
    // configuration.
    maven githubPackages(

            // Get resolved configuration variables.
            githubPackagesConfiguration.resolvedRepository,
            githubPackagesConfiguration.resolvedUsername,
            githubPackagesConfiguration.resolvedAccessToken)

    // Declares a Github Packages maven repository using the resolved
    // configuration variables.
    maven {
        name = 'GithubPackages'
        url = uri(githubPackagesConfiguration.resolvedUrl)
        credentials {
            username = githubPackagesConfiguration.resolvedUsername
            password = githubPackagesConfiguration.resolvedAccessToken
        }
    }

    jcenter()
}

publishing {
    publications {
        maven(MavenPublication) {
            from components.java
        }
    }
    repositories {

        // Declare a Github Packages maven repository for publishing.
        maven githubPackages()
    }
}

dependencies {
    implementation 'com.jarnoharno:example-library:1.0-SNAPSHOT'
}
```
