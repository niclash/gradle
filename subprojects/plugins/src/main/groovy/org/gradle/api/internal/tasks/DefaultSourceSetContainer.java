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
package org.gradle.api.internal.tasks;

import org.gradle.api.internal.AutoCreateDomainObjectContainer;
import org.gradle.api.internal.ClassGenerator;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public class DefaultSourceSetContainer extends AutoCreateDomainObjectContainer<SourceSet> implements SourceSetContainer {
    private final FileResolver fileResolver;
    private final TaskResolver taskResolver;
    private final ClassGenerator generator;

    public DefaultSourceSetContainer(FileResolver fileResolver, TaskResolver taskResolver, ClassGenerator classGenerator) {
        super(SourceSet.class, classGenerator);
        this.fileResolver = fileResolver;
        this.taskResolver = taskResolver;
        this.generator = classGenerator;
    }

    @Override
    protected SourceSet create(String name) {
        DefaultSourceSet sourceSet = generator.newInstance(DefaultSourceSet.class, name, fileResolver, taskResolver);
        sourceSet.setClasses(generator.newInstance(DefaultSourceSetOutput.class, sourceSet.getDisplayName(), fileResolver, taskResolver));

        return sourceSet;
    }
}