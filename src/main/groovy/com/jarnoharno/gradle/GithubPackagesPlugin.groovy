package com.jarnoharno.gradle

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.GradleScriptException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.yaml.snakeyaml.Yaml

class GithubPackagesExtension {

    String repository
    String username
    String accessToken
    Project project

    GithubPackagesExtension(Project project) {
        this.project = project
    }

    @Lazy String resolvedUsername = {
        project.findProperty('githubPackages.username') as String
                ?: username ?: hubConfig['user'] as String
    } ()

    @Lazy String resolvedAccessToken = {
        project.findProperty('githubPackages.accessToken') as String
                ?: accessToken ?: hubConfig['oauth_token'] as String
    } ()

    @Lazy String resolvedRepository = {
        project.findProperty('githubPackages.repository') as String
                ?: repository ?: throwUndefinedException('repository')
    } ()

    @Lazy String resolvedUrl = { repositoryUrl(resolvedRepository) } ()

    @Lazy hubConfig = {
        def userHome = System.getProperty("user.home")
        def hubFile = new File("$userHome/.config/hub")
        try {
            hubFile.withInputStream { input ->
                def yaml = new Yaml().load(input)
                return yaml['github.com'][0]
            }
        } catch (Throwable e) {
            throw new GradleScriptException("Unable to read ${hubFile}", e)
        }
    } ()

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
        project.ext.githubPackagesConfiguration = configuration
        project.ext.githubPackages = { String repository = null, String username = null, String accessToken = null ->
            def resolvedUrl = repository ? GithubPackagesExtension.repositoryUrl(repository) : configuration.resolvedUrl
            def resolvedUsername = username ?: configuration.resolvedUsername
            def resolvedAccessToken = accessToken ?: configuration.resolvedAccessToken
            return new Action<MavenArtifactRepository>() {
                void execute(MavenArtifactRepository repo) {
                    repo.url = project.uri(resolvedUrl)
                    repo.credentials {
                        delegate.username = resolvedUsername
                        delegate.password = resolvedAccessToken
                    }
                }
            }
        }
    }
}
