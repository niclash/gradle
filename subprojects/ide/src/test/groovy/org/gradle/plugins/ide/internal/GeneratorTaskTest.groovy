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
package org.gradle.plugins.ide.internal

import org.gradle.api.Action
import org.gradle.plugins.ide.api.GeneratorTask
import org.gradle.plugins.ide.internal.generator.generator.Generator
import org.gradle.util.HelperUtil
import org.gradle.util.TemporaryFolder
import org.junit.Rule
import spock.lang.Specification

class GeneratorTaskTest extends Specification {
    @Rule public final TemporaryFolder tmpDir = new TemporaryFolder()
    final Generator<TestConfigurationObject> generator = Mock()
    final File inputFile = tmpDir.file('input')
    final File outputFile = tmpDir.file('output')
    final GeneratorTask<TestConfigurationObject> task = HelperUtil.createTask(GeneratorTask)

    def setup() {
        task.inputFile = inputFile
        task.outputFile = outputFile
        task.generator = generator
    }

    def usesOutputFileAsDefaultInputFile() {
        when:
        task.inputFile = null

        then:
        task.inputFile == task.outputFile

        when:
        task.inputFile = inputFile

        then:
        task.inputFile == inputFile
    }

    def mergesConfigurationWhenInputFileExists() {
        def configObject = new TestConfigurationObject()
        inputFile.text = 'config'

        when:
        task.generate()

        then:
        1 * generator.read(inputFile) >> configObject
        1 * generator.configure(configObject)
        1 * generator.write(configObject, outputFile)
        0 * _._
    }

    def generatesConfigurationWhenInputFileDoesNotExist() {
        def configObject = new TestConfigurationObject()

        when:
        task.generate()

        then:
        1 * generator.defaultInstance() >> configObject
        1 * generator.configure(configObject)
        1 * generator.write(configObject, outputFile)
        0 * _._
    }

    def executesActionBeforeConfiguringObject() {
        def configObject = new TestConfigurationObject()
        Action<TestConfigurationObject> action = Mock()
        task.beforeConfigured(action)

        when:
        task.generate()

        then:
        1 * generator.defaultInstance() >> configObject
        1 * action.execute(configObject)
        1 * generator.configure(configObject)
    }

    def executesActionAfterConfiguringObject() {
        def configObject = new TestConfigurationObject()
        Action<TestConfigurationObject> action = Mock()
        task.whenConfigured(action)

        when:
        task.generate()

        then:
        1 * generator.defaultInstance() >> configObject
        1 * generator.configure(configObject)
        1 * action.execute(configObject)
    }
}

class TestConfigurationObject {

}