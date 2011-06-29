/*
 * Copyright 2007-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.artifacts.ivyservice;

import org.apache.ivy.plugins.resolver.AbstractResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.IvyArtifactRepository;
import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer;
import org.gradle.api.artifacts.maven.GroovyMavenDeployer;
import org.gradle.api.artifacts.maven.MavenResolver;
import org.gradle.api.internal.artifacts.publish.maven.MavenPomMetaInfoProvider;
import org.gradle.api.internal.file.FileResolver;

import java.io.File;

/**
 * @author Hans Dockter
 */
public interface ResolverFactory {
    DependencyResolver createResolver(Object userDescription);

    FileSystemResolver createFlatDirResolver(String name, File... roots);

    AbstractResolver createMavenRepoResolver(String name, String root, String... jarRepoUrls);

    AbstractResolver createMavenLocalResolver(String name);

    GroovyMavenDeployer createMavenDeployer(String name, MavenPomMetaInfoProvider pomMetaInfoProvider, ConfigurationContainer configurationContainer,
                                           Conf2ScopeMappingContainer scopeMapping, FileResolver fileResolver);

    MavenResolver createMavenInstaller(String name, MavenPomMetaInfoProvider pomMetaInfoProvider, ConfigurationContainer configurationContainer,
                                       Conf2ScopeMappingContainer scopeMapping, FileResolver fileResolver);

    IvyArtifactRepository createIvyRepository(FileResolver resolver);
}
