/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.integtests.tooling.m4

import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.tooling.model.BuildableProject
import org.gradle.tooling.model.eclipse.EclipseProject
import spock.lang.Issue

class ToolingApiTaskExecutionFixesTest extends ToolingApiSpecification {

    @Issue("GRADLE-1529")
    //this is just one of the ways of fixing the problem. See the issue for details
    def "should not show not executable tasks"() {
        dist.testFile('build.gradle') << '''
task a
task b
'''
        when:
        def project = withConnection { connection -> connection.getModel(BuildableProject.class) }

        then:
        def tasks = project.tasks.collect { it.name }
        assert tasks == ['a', 'b'] : "temp tasks like 'cleanEclipse', 'eclipse', e.g. should not show on this list: " + tasks
    }

    @Issue("GRADLE-1529")
    //this is just one of the ways of fixing the problem. See the issue for details
    def "should hide not executable tasks when necessary for a multi module build"() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = '''
project(':api') {
    apply plugin: 'java'
    apply plugin: 'eclipse'
}
'''
        projectDir.file('settings.gradle').text = "include 'api', 'impl'"

        when:
        EclipseProject eclipseProject = withConnection { connection -> connection.getModel(EclipseProject.class) }

        then:
        def rootTasks = eclipseProject.tasks.collect { it.name }

        EclipseProject api = eclipseProject.children[1]
        def apiTasks = api.tasks.collect { it.name }

        EclipseProject impl = eclipseProject.children[0]
        def implTasks = impl.tasks.collect { it.name }

        ['eclipse', 'cleanEclipse', 'eclipseProject', 'cleanEclipseProject'].each {
            assert !rootTasks.contains(it)
            assert !implTasks.contains(it)

            assert apiTasks.contains(it)
        }
    }
}
