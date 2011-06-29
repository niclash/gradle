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


package org.gradle.integtests

import org.gradle.integtests.fixtures.ArtifactBuilder
import org.gradle.integtests.fixtures.ExecutionResult
import org.gradle.integtests.fixtures.HttpServer
import org.gradle.util.TestFile
import org.junit.Test
import static org.hamcrest.Matchers.*
import static org.junit.Assert.*
import org.gradle.integtests.fixtures.internal.AbstractIntegrationTest
import org.junit.Rule

public class ExternalScriptExecutionIntegrationTest extends AbstractIntegrationTest {
    @Rule
    public final HttpServer server = new HttpServer()

    @Test
    public void executesExternalScriptAgainstAProjectWithCorrectEnvironment() {
        createExternalJar()
        createBuildSrc()

        def implClassName = 'com.google.common.collect.Multimap'
        TestFile externalScript = testFile('external.gradle')
        externalScript << """
buildscript {
    dependencies { classpath files('repo/test-1.3.jar') }
}
new org.gradle.test.BuildClass()
new BuildSrcClass()
println 'quiet message'
logging.captureStandardOutput(LogLevel.ERROR)
println 'error message'
assert project != null
assert "${externalScript.absolutePath.replace("\\", "\\\\")}" == buildscript.sourceFile as String
assert "${externalScript.toURI()}" == buildscript.sourceURI as String
assert buildscript.classLoader == getClass().classLoader.parent
assert buildscript.classLoader == Thread.currentThread().contextClassLoader
assert gradle.scriptClassLoader == buildscript.classLoader.parent
assert project.buildscript.classLoader != buildscript.classLoader
Gradle.class.classLoader.loadClass('${implClassName}')
try {
    buildscript.classLoader.loadClass('${implClassName}')
    assert false: 'should fail'
} catch (ClassNotFoundException e) {
    // expected
}

task doStuff
someProp = 'value'
"""
        testFile('build.gradle') << '''
apply { from 'external.gradle' }
assert 'value' == someProp
'''

        ExecutionResult result = inTestDirectory().withTasks('doStuff').run()
        assertThat(result.output, containsString('quiet message'))
        assertThat(result.output, not(containsString('error message')))
        assertThat(result.error, containsString('error message'))
        assertThat(result.error, not(containsString('quiet message')))
    }

    @Test
    public void canExecuteExternalScriptAgainstAnArbitraryObject() {
        createBuildSrc()

        testFile('external.gradle') << '''
println 'quiet message'
captureStandardOutput(LogLevel.ERROR)
println 'error message'
new BuildSrcClass()
assert 'doStuff' == name
assert buildscript.classLoader == getClass().classLoader.parent
assert buildscript.classLoader == Thread.currentThread().contextClassLoader
assert project.gradle.scriptClassLoader == buildscript.classLoader.parent
assert project.buildscript.classLoader != buildscript.classLoader
someProp = 'value'
'''
        testFile('build.gradle') << '''
task doStuff
apply {
    to doStuff
    from 'external.gradle'
}
assert 'value' == doStuff.someProp
'''

        ExecutionResult result = inTestDirectory().withTasks('doStuff').run()
        assertThat(result.output, containsString('quiet message'))
        assertThat(result.output, not(containsString('error message')))
        assertThat(result.error, containsString('error message'))
        assertThat(result.error, not(containsString('quiet message')))
    }

    @Test
    public void canExecuteExternalScriptFromSettingsScript() {
        testFile('settings.gradle') << ''' apply { from 'other.gradle' } '''
        testFile('other.gradle') << ''' include 'child' '''
        testFile('build.gradle') << ''' assert ['child'] == subprojects*.name '''

        inTestDirectory().withTaskList().run()
    }

    @Test
    public void canExecuteExternalScriptFromInitScript() {
        TestFile initScript = testFile('init.gradle') << ''' apply { from 'other.gradle' } '''
        testFile('other.gradle') << '''
addListener(new ListenerImpl())
class ListenerImpl extends BuildAdapter {
    public void projectsEvaluated(Gradle gradle) {
        gradle.rootProject.task('doStuff')
    }
}
'''
        inTestDirectory().usingInitScript(initScript).withTasks('doStuff').run()
    }

    @Test
    public void canExecuteExternalScriptFromExternalScript() {
        testFile('build.gradle') << ''' apply { from 'other1.gradle' } '''
        testFile('other1.gradle') << ''' apply { from 'other2.gradle' } '''
        testFile('other2.gradle') << ''' task doStuff '''

        inTestDirectory().withTasks('doStuff').run()
    }

    @Test
    public void canFetchScriptViaHttp() {
        TestFile script = testFile('external.gradle')

        server.expectGet('/external.gradle', script)
        server.start()

        script << """
            task doStuff
            assert buildscript.sourceFile == null
            assert "http://localhost:$server.port/external.gradle" == buildscript.sourceURI as String
"""

        testFile('build.gradle') << """
            apply from: 'http://localhost:$server.port/external.gradle'
            defaultTasks 'doStuff'
"""

        inTestDirectory().run()
    }

    @Test
    public void cachesScriptClassForAGivenScript() {
        testFile('settings.gradle') << 'include \'a\', \'b\''
        testFile('external.gradle') << 'appliedScript = this'
        testFile('build.gradle') << '''
allprojects {
   apply from: "$rootDir/external.gradle"
}
subprojects {
    assert appliedScript.class == rootProject.appliedScript.class
}
task doStuff
'''
        inTestDirectory().withTasks('doStuff').run()
    }

    private TestFile createBuildSrc() {
        return testFile('buildSrc/src/main/java/BuildSrcClass.java') << '''
            public class BuildSrcClass { }
'''
    }

    private def createExternalJar() {
        ArtifactBuilder builder = artifactBuilder();
        builder.sourceFile('org/gradle/test/BuildClass.java') << '''
            package org.gradle.test;
            public class BuildClass { }
'''
        builder.buildJar(testFile("repo/test-1.3.jar"))
    }
}
