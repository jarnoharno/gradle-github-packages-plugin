package com.jarnoharno.gradle

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.GradleScriptException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.yaml.snakeyaml.Yaml

import java.util.function.Function

class GithubPackagesExtension {

    String name = 'GithubPackages'
    String repository
    String username
    String accessToken
    Project project

    GithubPackagesExtension(Project project) {
        this.project = project
    }

    @Lazy String resolvedName = {
        project.findProperty('githubPackages.name') as String
                ?: name
    } ()

    @Lazy String resolvedUsername = {
        project.findProperty('githubPackages.username') as String
                ?: username
                ?: project.findProperty('gpr.user') as String
                ?: System.getenv("GITHUB_ACTOR")
                ?: ghConfig['user'] as String
                ?: hubConfig['user'] as String
    } ()

    @Lazy String resolvedAccessToken = {
        project.findProperty('githubPackages.accessToken') as String
                ?: accessToken
                ?: project.findProperty('gpr.key') as String
                ?: System.getenv("GITHUB_TOKEN")
                ?: ghConfig['oauth_token'] as String
                ?: hubConfig['oauth_token'] as String
    } ()

    @Lazy String resolvedRepository = {
        project.findProperty('githubPackages.repository') as String
                ?: repository
                ?: throwUndefinedException('repository')
    } ()

    @Lazy String resolvedUrl = {
        repositoryUrl(resolvedRepository)
    } ()

    @Lazy hubConfig = {
        yamlConfig(".config/hub") {
            it['github.com'][0]
        }
    } ()

    @Lazy ghConfig = {
        yamlConfig(".config/gh/hosts.yml") {
            it['github.com']
        }
    } ()

    static def yamlConfig(String userPath, Function<Object, Object> extractor) {
        def userHome = System.getProperty("user.home")
        def file = new File("$userHome/$userPath")
        try {
            file.withInputStream { input ->
                def yaml = new Yaml().load(input)
                return extractor.apply(yaml)
            }
        } catch (Throwable e) {
            throw new GradleScriptException("Unable to read ${file}", e)
        }
    }

    static def throwUndefinedException(String variable) {
        throw new GradleException("${variable} is undefined")
    }

    static def repositoryUrl(String repository) {
        return "https://maven.pkg.github.com/${repository}"
    }
}

class GithubPackagesPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def configuration = project.extensions.create('githubPackages', GithubPackagesExtension, project)
        def githubPackagesUrl = { String repository = null ->
            repository ? GithubPackagesExtension.repositoryUrl(repository) : configuration.resolvedUrl
        }
        def githubPackagesCredentials = { String username = null, String accessToken = null ->
            def resolvedUsername = username ?: configuration.resolvedUsername
            def resolvedAccessToken = accessToken ?: configuration.resolvedAccessToken
            return new Action<? super PasswordCredentials>() {

                @Override
                void execute(Object credentials) {
                    credentials.username = resolvedUsername
                    credentials.password = resolvedAccessToken
                }
            }
        }
        def githubPackages = { String repository = null, String username = null, String accessToken = null ->
            def resolvedName = configuration.resolvedName
            def resolvedUrl = githubPackagesUrl(repository)
            return new Action<? super MavenArtifactRepository>() {

                @Override
                void execute(Object repo) {
                    repo.name = resolvedName
                    repo.url = project.uri(resolvedUrl)
                    repo.credentials githubPackagesCredentials(username, accessToken)
                }
            }
        }
        project.ext.githubPackagesConfiguration = configuration
        project.ext.githubPackages = githubPackages
        project.ext.githubPackagesUrl = githubPackagesUrl
        project.ext.githubPackagesCredentials = githubPackagesCredentials
    }
}
