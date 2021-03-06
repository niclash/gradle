/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.api.internal.project;

import org.gradle.api.AntBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Module;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.internal.ClassGenerator;
import org.gradle.api.internal.Factory;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.artifacts.ArtifactPublicationServices;
import org.gradle.api.internal.artifacts.DefaultModule;
import org.gradle.api.internal.artifacts.DependencyManagementServices;
import org.gradle.api.internal.artifacts.DependencyResolutionServices;
import org.gradle.api.internal.artifacts.configurations.DependencyMetaDataProvider;
import org.gradle.api.internal.artifacts.dsl.DefaultArtifactHandler;
import org.gradle.api.internal.artifacts.dsl.PublishArtifactFactory;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.file.*;
import org.gradle.api.internal.initialization.DefaultScriptHandlerFactory;
import org.gradle.api.internal.initialization.ScriptClassLoaderProvider;
import org.gradle.api.internal.initialization.ScriptHandlerFactory;
import org.gradle.api.internal.initialization.ScriptHandlerInternal;
import org.gradle.api.internal.plugins.DefaultConvention;
import org.gradle.api.internal.plugins.DefaultProjectsPluginContainer;
import org.gradle.api.internal.plugins.PluginRegistry;
import org.gradle.api.internal.project.ant.AntLoggingAdapter;
import org.gradle.api.internal.project.taskfactory.ITaskFactory;
import org.gradle.api.internal.tasks.DefaultTaskContainerFactory;
import org.gradle.api.internal.tasks.TaskContainerInternal;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.logging.LoggingManagerInternal;

import java.io.File;

/**
 * Contains the services for a given project.
 */
public class ProjectInternalServiceRegistry extends DefaultServiceRegistry implements ServiceRegistryFactory {
    private final ProjectInternal project;

    public ProjectInternalServiceRegistry(ServiceRegistry parent, final ProjectInternal project) {
        super(parent);
        this.project = project;
    }

    protected PluginRegistry createPluginRegistry(PluginRegistry parentRegistry) {
        return parentRegistry.createChild(get(ScriptClassLoaderProvider.class).getClassLoader());
    }

    protected FileResolver createFileResolver() {
        return new BaseDirConverter(project.getProjectDir());
    }

    protected LoggingManagerInternal createLoggingManager() {
        return getFactory(LoggingManagerInternal.class).create();
    }

    protected FileOperations createFileOperations() {
        return new DefaultFileOperations(get(FileResolver.class), project.getTasks(), get(TemporaryFileProvider.class));
    }

    protected TemporaryFileProvider createTemporaryFileProvider() {
        return new DefaultTemporaryFileProvider(new FileSource() {
            public File get() {
                return new File(project.getBuildDir(), "tmp");
            }
        });
    }

    protected Factory<AntBuilder> createAntBuilderFactory() {
        return new DefaultAntBuilderFactory(new AntLoggingAdapter(), project);
    }

    protected PluginContainer createPluginContainer() {
        return new DefaultProjectsPluginContainer(get(PluginRegistry.class), project);
    }

    protected Factory<TaskContainerInternal> createTaskContainerInternal() {
        return new DefaultTaskContainerFactory(get(ClassGenerator.class), get(ITaskFactory.class), project);
    }

    protected Convention createConvention() {
        return new DefaultConvention();
    }

    protected Factory<ArtifactPublicationServices> createRepositoryHandlerFactory() {
        return get(DependencyResolutionServices.class).getPublishServicesFactory();
    }

    protected RepositoryHandler createRepositoryHandler() {
        return get(DependencyResolutionServices.class).getResolveRepositoryHandler();
    }

    protected ConfigurationContainer createConfigurationContainer() {
        return get(DependencyResolutionServices.class).getConfigurationContainer();
    }

    protected DependencyResolutionServices createDependencyResolutionServices() {
        return newDependencyResolutionServices();
    }

    private DependencyResolutionServices newDependencyResolutionServices() {
        return get(DependencyManagementServices.class).create(get(FileResolver.class), get(DependencyMetaDataProvider.class), get(ProjectFinder.class), project);
    }

    protected ArtifactHandler createArtifactHandler() {
        return new DefaultArtifactHandler(get(ConfigurationContainer.class), get(PublishArtifactFactory.class));
    }

    protected ProjectFinder createProjectFinder() {
        return new ProjectFinder() {
            public Project getProject(String path) {
                return project.project(path);
            }
        };
    }

    protected DependencyHandler createDependencyHandler() {
        return get(DependencyResolutionServices.class).getDependencyHandler();
    }

    protected ScriptHandlerInternal createScriptHandler() {
        ScriptHandlerFactory factory = new DefaultScriptHandlerFactory(
                get(DependencyManagementServices.class),
                get(FileResolver.class),
                get(DependencyMetaDataProvider.class));
        ClassLoader parentClassLoader;
        if (project.getParent() != null) {
            parentClassLoader = project.getParent().getBuildscript().getClassLoader();
        } else {
            parentClassLoader = project.getGradle().getScriptClassLoader();
        }
        return factory.create(project.getBuildScriptSource(), parentClassLoader, project);
    }

    protected DependencyMetaDataProvider createDependencyMetaDataProvider() {
        return new DependencyMetaDataProvider() {

            public File getGradleUserHomeDir() {
                return project.getGradle().getGradleUserHomeDir();
            }

            public Module getModule() {
                return new DefaultModule(project.getGroup().toString(), project.getName(), project.getVersion().toString(), project.getStatus().toString());
            }
        };
    }

    public ServiceRegistryFactory createFor(Object domainObject) {
        if (domainObject instanceof TaskInternal) {
            return new TaskInternalServiceRegistry(this, project, (TaskInternal)domainObject);
        }
        throw new UnsupportedOperationException();
    }

}
