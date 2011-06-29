/*
 * Copyright 2009 the original author or authors.
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

import org.gradle.api.artifacts.*;
import org.gradle.api.internal.artifacts.IvyService;
import org.gradle.api.specs.Spec;

import java.io.File;
import java.util.Set;

public class ErrorHandlingIvyService implements IvyService {
    private final IvyService ivyService;

    public ErrorHandlingIvyService(IvyService ivyService) {
        this.ivyService = ivyService;
    }

    public IvyService getIvyService() {
        return ivyService;
    }

    public void publish(Configuration configuration, File descriptorDestination) {
        try {
            ivyService.publish(configuration, descriptorDestination);
        } catch (Throwable e) {
            throw new PublishException(String.format("Could not publish %s.", configuration), e);
        }
    }

    public ResolvedConfiguration resolve(final Configuration configuration) {
        final ResolvedConfiguration resolvedConfiguration;
        try {
            resolvedConfiguration = ivyService.resolve(configuration);
        } catch (final Throwable e) {
            return new BrokenResolvedConfiguration(e, configuration);
        }
        return new ErrorHandlingResolvedConfiguration(resolvedConfiguration, configuration);
    }

    private static ResolveException wrapException(Throwable e, Configuration configuration) {
        if (e instanceof ResolveException) {
            return (ResolveException) e;
        }
        return new ResolveException(configuration, e);
    }

    private static class ErrorHandlingResolvedConfiguration implements ResolvedConfiguration {
        private final ResolvedConfiguration resolvedConfiguration;
        private final Configuration configuration;

        public ErrorHandlingResolvedConfiguration(ResolvedConfiguration resolvedConfiguration,
                                                  Configuration configuration) {
            this.resolvedConfiguration = resolvedConfiguration;
            this.configuration = configuration;
        }

        public boolean hasError() {
            return resolvedConfiguration.hasError();
        }

        public void rethrowFailure() throws ResolveException {
            try {
                resolvedConfiguration.rethrowFailure();
            } catch (Throwable e) {
                throw wrapException(e, configuration);
            }
        }

        public Set<File> getFiles(Spec<Dependency> dependencySpec) throws ResolveException {
            try {
                return resolvedConfiguration.getFiles(dependencySpec);
            } catch (Throwable e) {
                throw wrapException(e, configuration);
            }
        }

        public Set<ResolvedDependency> getFirstLevelModuleDependencies() throws ResolveException {
            try {
                return resolvedConfiguration.getFirstLevelModuleDependencies();
            } catch (Throwable e) {
                throw wrapException(e, configuration);
            }
        }

        public Set<ResolvedDependency> getFirstLevelModuleDependencies(Spec<Dependency> dependencySpec) throws ResolveException {
            try {
                return resolvedConfiguration.getFirstLevelModuleDependencies(dependencySpec);
            } catch (Throwable e) {
                throw wrapException(e, configuration);
            }
        }

        public Set<ResolvedArtifact> getResolvedArtifacts() throws ResolveException {
            try {
                return resolvedConfiguration.getResolvedArtifacts();
            } catch (Throwable e) {
                throw wrapException(e, configuration);
            }
        }
    }

    private static class BrokenResolvedConfiguration implements ResolvedConfiguration {
        private final Throwable e;
        private final Configuration configuration;

        public BrokenResolvedConfiguration(Throwable e, Configuration configuration) {
            this.e = e;
            this.configuration = configuration;
        }

        public boolean hasError() {
            return true;
        }

        public void rethrowFailure() throws ResolveException {
            throw wrapException(e, configuration);
        }

        public Set<File> getFiles(Spec<Dependency> dependencySpec) throws ResolveException {
            throw wrapException(e, configuration);
        }

        public Set<ResolvedDependency> getFirstLevelModuleDependencies() throws ResolveException {
            throw wrapException(e, configuration);
        }

        public Set<ResolvedDependency> getFirstLevelModuleDependencies(Spec<Dependency> dependencySpec) throws ResolveException {
            throw wrapException(e, configuration);
        }

        public Set<ResolvedArtifact> getResolvedArtifacts() throws ResolveException {
            throw wrapException(e, configuration);
        }
    }
}
