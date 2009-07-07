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
package org.gradle.integtests

import org.gradle.integtests.DistributionIntegrationTestRunner
import org.gradle.integtests.GradleDistribution
import org.gradle.integtests.GradleExecuter
import org.gradle.integtests.TestFile
import org.junit.Test
import org.junit.runner.RunWith

@RunWith (DistributionIntegrationTestRunner.class)
public class SamplesJavaOnlyIfIntegrationTest {
    // Injected by test runner
    private GradleDistribution dist;
    private GradleExecuter executer;


    /**
     * runs a build 3 times.
     * execute clean dists
     * check worked correctly
     *
     * remove test results
     * execute dists
     * check didn't re-run tests
     *
     * remove class file
     * execute dists
     * check that it re-ran tests 
     */
    @Test public void testOptimizedBuild() {
        File javaprojectDir = new File(dist.samplesDir, 'java/onlyif')
        // Build and test projects
        executer.inDirectory(javaprojectDir).withTasks('clean', 'dists').run()

        // Check tests have run
        assertExists(javaprojectDir, 'build/test-results/TEST-org.gradle.PersonTest.xml')
        assertExists(javaprojectDir, 'build/test-results/TESTS-TestSuites.xml')

        // Check jar exists
        assertExists(javaprojectDir, "build/libs/onlyif-1.0.jar")

        // remove test results
        removeFile(javaprojectDir, 'build/test-results/TEST-org.gradle.PersonTest.xml')
        removeFile(javaprojectDir, 'build/test-results/TESTS-TestSuites.xml')

        executer.inDirectory(javaprojectDir).withTasks('dists').run()

        // assert that tests did not run
        // (since neither compile nor compileTests should have done anything)
        assertDoesNotExist(javaprojectDir, 'build/test-results/TEST-org.gradle.PersonTest.xml')
        assertDoesNotExist(javaprojectDir, 'build/test-results/TESTS-TestSuites.xml')

        // remove a compiled class file
        removeFile(javaprojectDir, 'build/classes/org/gradle/Person.class')

        executer.inDirectory(javaprojectDir).withTasks('dists').run()

        // Check tests have run
        assertExists(javaprojectDir, 'build/test-results/TEST-org.gradle.PersonTest.xml')
        assertExists(javaprojectDir, 'build/test-results/TESTS-TestSuites.xml')
    }

    @Test public void testNoOptCommandLine() {
        File javaprojectDir = new File(dist.samplesDir, 'java/onlyif')
        // Build and test projects
        executer.inDirectory(javaprojectDir).withTasks('clean', 'dists').run()

        // Check tests have run
        assertExists(javaprojectDir, 'build/test-results/TEST-org.gradle.PersonTest.xml')
        assertExists(javaprojectDir, 'build/test-results/TESTS-TestSuites.xml')

        // remove test results
        removeFile(javaprojectDir, 'build/test-results/TEST-org.gradle.PersonTest.xml')
        removeFile(javaprojectDir, 'build/test-results/TESTS-TestSuites.xml')

        executer.inDirectory(javaprojectDir).withTasks('dists').withArguments('-o').run()

        // Check tests have run
        assertExists(javaprojectDir, 'build/test-results/TEST-org.gradle.PersonTest.xml')
        assertExists(javaprojectDir, 'build/test-results/TESTS-TestSuites.xml')

    }


    private static void assertExists(File baseDir, String path) {
        new TestFile(baseDir).file(path).assertExists()
    }

    private static void assertDoesNotExist(File baseDir, String path) {
        new TestFile(baseDir).file(path).assertDoesNotExist()
    }

    private static void removeFile(File baseDir, String path) {
        new TestFile(baseDir).file(path).asFile().delete();
    }
}