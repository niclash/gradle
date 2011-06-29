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

package org.gradle.api.plugins

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.bundling.Ear
import org.gradle.util.HelperUtil
import org.junit.Before
import org.junit.Test
import static org.gradle.util.Matchers.dependsOn
import static org.hamcrest.Matchers.*
import static org.junit.Assert.*
import static org.gradle.util.TextUtil.toPlatformLineSeparators

/**
 * @author David Gileadi
 */
class EarPluginTest {
    private Project project
    private EarPlugin earPlugin
    private static final String TEST_APP_XML = toPlatformLineSeparators('<?xml version="1.0" encoding="UTF-8"?>\n' +
        '<application xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://java.sun.com/xml/ns/javaee" xmlns:application="http://java.sun.com/xml/ns/javaee/application_5.xsd" xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/application_5.xsd" version="5">\n' +
        '  <display-name>Application</display-name>\n' +
        '  <module>\n' +
        '    <web>\n' +
        '      <web-uri>Web.war</web-uri>\n' +
        '      <context-root>/</context-root>\n' +
        '    </web>\n' +
        '  </module>\n' +
        '  <module>\n' +
        '    <ejb>jrules-bres-session-wl100-6.7.3.jar</ejb>\n' +
        '  </module>\n' +
        '</application>')
    

    @Before
    public void setUp() {
        project = HelperUtil.createRootProject()
        earPlugin = new EarPlugin()
    }

    @Test public void appliesBasePluginAndAddsConvention() {
        earPlugin.apply(project)
        
        assertTrue(project.getPlugins().hasPlugin(BasePlugin));
        assertThat(project.convention.plugins.ear, instanceOf(EarPluginConvention))
    }
    
    @Test public void createsConfigurations() {
        earPlugin.apply(project)

        def configuration = project.configurations.getByName(EarPlugin.DEPLOY_CONFIGURATION_NAME)
        assertFalse(configuration.visible)
        assertFalse(configuration.transitive)

        configuration = project.configurations.getByName(EarPlugin.EARLIB_CONFIGURATION_NAME)
        assertFalse(configuration.visible)
        assertTrue(configuration.transitive)
    }

    @Test public void addsTasks() {
        earPlugin.apply(project)

        def task = project.tasks[EarPlugin.EAR_TASK_NAME]
        assertThat(task, instanceOf(Ear))
        assertThat(task.destinationDir, equalTo(project.libsDir))

        task = project.tasks[BasePlugin.ASSEMBLE_TASK_NAME]
        assertThat(task, dependsOn(EarPlugin.EAR_TASK_NAME))
    }

    @Test public void addsTasksToJavaProject() {
        project.plugins.apply(JavaPlugin.class)
        earPlugin.apply(project)

        def task = project.tasks[EarPlugin.EAR_TASK_NAME]
        assertThat(task, instanceOf(Ear))
        assertThat(task, dependsOn(JavaPlugin.CLASSES_TASK_NAME))
        assertThat(task.destinationDir, equalTo(project.libsDir))

        task = project.tasks[BasePlugin.ASSEMBLE_TASK_NAME]
        assertThat(task, dependsOn(JavaPlugin.JAR_TASK_NAME, EarPlugin.EAR_TASK_NAME))
    }

    @Test public void dependsOnEarlibConfig() {
        earPlugin.apply(project)

        Project childProject = HelperUtil.createChildProject(project, 'child')
        JavaPlugin javaPlugin = new JavaPlugin()
        javaPlugin.apply(childProject)

        project.dependencies {
            earlib project(path: childProject.path, configuration: 'archives')
        }

        def task = project.tasks[EarPlugin.EAR_TASK_NAME]
        assertThat(task.taskDependencies.getDependencies(task)*.path as Set, hasItem(':child:jar'))
    }

    @Test public void appliesMappingsToArchiveTasks() {
        earPlugin.apply(project)

        def task = project.task(type: Ear, 'customEar')
        assertThat(task.destinationDir, equalTo(project.libsDir))

        assertThat(project.tasks[BasePlugin.ASSEMBLE_TASK_NAME], dependsOn(EarPlugin.EAR_TASK_NAME, 'customEar'))
    }

    @Test public void appliesMappingsToArchiveTasksForJavaProject() {
        project.plugins.apply(JavaPlugin.class)
        earPlugin.apply(project)

        def task = project.task(type: Ear, 'customEar') {
            earModel = new EarPluginConvention(null)
        }
        assertThat(task.destinationDir, equalTo(project.libsDir))

        assertThat(project.tasks[BasePlugin.ASSEMBLE_TASK_NAME], dependsOn(JavaPlugin.JAR_TASK_NAME, EarPlugin.EAR_TASK_NAME, 'customEar'))
        assertThat(task, dependsOn(hasItems(JavaPlugin.CLASSES_TASK_NAME)))
    }

    @Test public void addsDefaultEarToArchiveConfiguration() {
        earPlugin.apply(project)

        Configuration archiveConfiguration = project.getConfigurations().getByName(Dependency.ARCHIVES_CONFIGURATION);
        assertThat(archiveConfiguration.getAllArtifacts().size(), equalTo(1));
        assertThat(archiveConfiguration.getAllArtifacts().iterator().next().getType(), equalTo("ear"));
    }

    @Test public void supportsAppDir() {
        project.file("src/main/application/META-INF").mkdirs()
        project.file("src/main/application/META-INF/test.txt").createNewFile()
        project.file("src/main/application/test2.txt").createNewFile()

        earPlugin.apply(project)

        execute project.tasks[EarPlugin.EAR_TASK_NAME]

        inEar "test2.txt"
        inEar "META-INF/test.txt"
    }

    @Test public void supportsRenamedAppDir() {
        project.file("src/main/myapp").mkdirs()
        project.file("src/main/myapp/test.txt").createNewFile()

        earPlugin.apply(project)
        project.convention.plugins.ear.appDirName = "src/main/myapp"

        execute project.tasks[EarPlugin.EAR_TASK_NAME]
        inEar "test.txt"
    }

    @Test public void supportsRenamingLibDir() {
        Project childProject = HelperUtil.createChildProject(project, 'child')
        childProject.file("src/main/resources").mkdirs()
        childProject.file("src/main/resources/test.txt").createNewFile()
        JavaPlugin javaPlugin = new JavaPlugin()
        javaPlugin.apply(childProject)

        execute childProject.tasks[BasePlugin.ASSEMBLE_TASK_NAME]

        earPlugin.apply(project)
        project.convention.plugins.ear.libDirName = "APP-INF/lib"
        project.dependencies {
            earlib project(path: childProject.path, configuration: 'archives')
        }

        execute project.tasks[EarPlugin.EAR_TASK_NAME]
        
        inEar "APP-INF/lib/child.jar"
    }

    @Test public void supportsGeneratingDeploymentDescriptor() {
        earPlugin.apply(project)
        execute project.tasks[EarPlugin.EAR_TASK_NAME]

        inEar "META-INF/application.xml"
    }

    @Test public void avoidsOverwritingDeploymentDescriptor() {
        project.file("src/main/application/META-INF").mkdirs()
        project.file("src/main/application/META-INF/application.xml").text = TEST_APP_XML

        earPlugin.apply(project)
        execute project.tasks[EarPlugin.EAR_TASK_NAME]

        assert inEar("META-INF/application.xml").text == TEST_APP_XML
    }

    @Test public void supportsRenamingDeploymentDescriptor() {
        earPlugin.apply(project)
        project.convention.plugins.ear.deploymentDescriptor {
            fileName = "myapp.xml"
        }
        execute project.tasks[EarPlugin.EAR_TASK_NAME]

        inEar "META-INF/myapp.xml"
    }

    @Test public void avoidsOverwritingRenamedDeploymentDescriptor() {
        project.file("src/main/application/META-INF").mkdirs()
        project.file("src/main/application/META-INF/myapp.xml").text = TEST_APP_XML

        earPlugin.apply(project)
        project.convention.plugins.ear.deploymentDescriptor {
            fileName = "myapp.xml"
        }
        execute project.tasks[EarPlugin.EAR_TASK_NAME]
        assert inEar("META-INF/myapp.xml").text == TEST_APP_XML
    }

    private void execute(Task task) {
        for (Task dep : task.taskDependencies.getDependencies(task)) {
            for (Action action : dep.actions) {
                action.execute(dep)
            }
        }
        for (Action action : task.actions) {
            action.execute(task)
        }
    }
    
    File inEar(path) {
        def ear = project.zipTree("build/libs/${project.name}.ear")
        assert !ear.empty
        ear.matching { include path }.singleFile
    }
}
