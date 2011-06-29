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
package org.gradle.plugins.ide.eclipse

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.AbstractSpockTaskTest
import org.gradle.plugins.ide.eclipse.model.BuildCommand
import org.gradle.plugins.ide.eclipse.model.EclipseProject

/**
 * @author Hans Dockter
 */
class GenerateEclipseProjectTest extends AbstractSpockTaskTest {
    GenerateEclipseProject eclipseProject

    ConventionTask getTask() {
        return eclipseProject
    }

    def setup() {
        eclipseProject = createTask(GenerateEclipseProject.class);
        eclipseProject.projectModel = new EclipseProject()
    }

    def natures_shouldAdd() {
        when:
        eclipseProject.natures 'nature1'
        eclipseProject.natures 'nature2'

        then:
        eclipseProject.natures == ['nature1', 'nature2']
    }

    def buildCommands_shouldAdd() {
        when:
        eclipseProject.buildCommand 'command1', key1: 'value1'
        eclipseProject.buildCommand 'command2'

        then:
        eclipseProject.buildCommands as List == [new BuildCommand('command1', [key1: 'value1']), new BuildCommand('command2')]
    }
}
