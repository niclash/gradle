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

package org.gradle.integtests;

import org.gradle.integtests.fixtures.ExecutionFailure;
import org.gradle.integtests.fixtures.internal.AbstractIntegrationTest;
import org.gradle.util.TestFile;
import org.junit.Test;

import java.io.IOException;

public class SettingsScriptErrorIntegrationTest extends AbstractIntegrationTest {
    @Test
    public void reportsSettingsScriptEvaluationFailsWithRuntimeException() throws IOException {
        TestFile buildFile = testFile("some build.gradle");
        TestFile settingsFile = testFile("some settings.gradle");
        settingsFile.writelns("", "", "throw new RuntimeException('<failure message>')");

        ExecutionFailure failure = usingBuildFile(buildFile).usingSettingsFile(settingsFile).withTasks("do-stuff")
                .runWithFailure();

        failure.assertHasFileName(String.format("Settings file '%s'", settingsFile));
        failure.assertHasLineNumber(3);
        failure.assertHasDescription("A problem occurred evaluating settings 'reportsSettingsScriptEvaluationFailsWithRuntimeException");
        failure.assertHasCause("<failure message>");
    }
}
