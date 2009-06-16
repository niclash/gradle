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
package org.gradle.api.internal.artifacts.dependencies;

import org.gradle.api.artifacts.SelfResolvingDependency;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.FileCollection;

import java.io.File;
import java.util.Set;

public class DefaultSelfResolvingDependency extends AbstractDependency implements SelfResolvingDependency {
    private final FileCollection source;

    public DefaultSelfResolvingDependency(FileCollection source) {
        super(DEFAULT_CONFIGURATION);
        this.source = source;
    }

    public FileCollection getSource() {
        return source;
    }
    
    public boolean contentEquals(Dependency dependency) {
        if (!(dependency instanceof DefaultSelfResolvingDependency)) {
            return false;
        }
        DefaultSelfResolvingDependency selfResolvingDependency = (DefaultSelfResolvingDependency) dependency;
        return source.equals(selfResolvingDependency.source);
    }

    public Dependency copy() {
        return new DefaultSelfResolvingDependency(source);
    }

    public String getGroup() {
        return null;
    }

    public String getName() {
        return "unspecified";
    }

    public String getVersion() {
        return null;
    }

    public Set<File> resolve() {
        return source.getFiles();
    }
}