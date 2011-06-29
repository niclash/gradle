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
package org.gradle

import org.gradle.StartParameter.ShowStacktrace
import org.gradle.api.GradleException
import org.gradle.api.LocationAwareException
import org.gradle.api.logging.LogLevel
import org.gradle.execution.TaskSelectionException
import org.gradle.initialization.BuildClientMetaData
import org.gradle.logging.StyledTextOutputFactory
import org.gradle.logging.internal.TestStyledTextOutput
import spock.lang.Specification
import org.gradle.groovy.scripts.ScriptSource

class BuildExceptionReporterTest extends Specification {
    final TestStyledTextOutput output = new TestStyledTextOutput()
    final StyledTextOutputFactory factory = Mock()
    final BuildClientMetaData clientMetaData = Mock()
    final StartParameter startParameter = new StartParameter()
    final BuildExceptionReporter reporter = new BuildExceptionReporter(factory, startParameter, clientMetaData)

    def setup() {
        factory.create(BuildExceptionReporter.class, LogLevel.ERROR) >> output
        clientMetaData.describeCommand(!null, !null) >> { args -> args[0].append("[gradle ${args[1].join(' ')}]")}
    }

    def doesNothingWheBuildIsSuccessful() {
        expect:
        reporter.buildFinished(result(null))
        output.value == ''
    }

    def reportsInternalFailure() {
        final RuntimeException exception = new RuntimeException("<message>");

        expect:
        reporter.buildFinished(result(exception))
        output.value == '''
{failure}FAILURE: {normal}{failure}Build aborted because of an internal error.{normal}

* What went wrong:
Build aborted because of an unexpected internal error. Please file an issue at: http://www.gradle.org.

* Try:
Run with {userinput}--debug{normal} option to get additional debug info.

* Exception is:
java.lang.RuntimeException: <message>
{stacktrace}
'''
    }

    def reportsBuildFailure() {
        GradleException exception = new GradleException("<message>");

        expect:
        reporter.buildFinished(result(exception))
        output.value == '''
{failure}FAILURE: {normal}{failure}Build failed with an exception.{normal}

* What went wrong:
<message>

* Try:
Run with {userinput}--stacktrace{normal} option to get the stack trace. Run with {userinput}--info{normal} or {userinput}--debug{normal} option to get more log output.
'''
    }

    def reportsBuildFailureWhenFailureHasNoMessage() {
        GradleException exception = new GradleException();

        expect:
        reporter.buildFinished(result(exception))
        output.value == '''
{failure}FAILURE: {normal}{failure}Build failed with an exception.{normal}

* What went wrong:
org.gradle.api.GradleException (no error message)

* Try:
Run with {userinput}--stacktrace{normal} option to get the stack trace. Run with {userinput}--info{normal} or {userinput}--debug{normal} option to get more log output.
'''
    }

    def reportsLocationAwareException() {
        Throwable exception = exception("<location>", "<message>", new RuntimeException("<cause>"));

        expect:
        reporter.buildFinished(result(exception))
        output.value == '''
{failure}FAILURE: {normal}{failure}Build failed with an exception.{normal}

* Where:
<location>

* What went wrong:
<message>
Cause: <cause>

* Try:
Run with {userinput}--stacktrace{normal} option to get the stack trace. Run with {userinput}--info{normal} or {userinput}--debug{normal} option to get more log output.
'''
    }

    def reportsLocationAwareExceptionWithMultipleCauses() {
        Throwable exception = exception("<location>", "<message>", new RuntimeException("<outer>"), new RuntimeException("<cause>"));

        expect:
        reporter.buildFinished(result(exception))
        output.value == '''
{failure}FAILURE: {normal}{failure}Build failed with an exception.{normal}

* Where:
<location>

* What went wrong:
<message>
Cause: <outer>
Cause: <cause>

* Try:
Run with {userinput}--stacktrace{normal} option to get the stack trace. Run with {userinput}--info{normal} or {userinput}--debug{normal} option to get more log output.
'''
    }

    def reportsLocationAwareExceptionWhenCauseHasNoMessage() {
        Throwable exception = exception("<location>", "<message>", new RuntimeException());

        expect:
        reporter.buildFinished(result(exception))
        output.value == '''
{failure}FAILURE: {normal}{failure}Build failed with an exception.{normal}

* Where:
<location>

* What went wrong:
<message>
Cause: java.lang.RuntimeException (no error message)

* Try:
Run with {userinput}--stacktrace{normal} option to get the stack trace. Run with {userinput}--info{normal} or {userinput}--debug{normal} option to get more log output.
'''
    }

    def showsStacktraceOfCauseOfLocationAwareException() {
        startParameter.showStacktrace = ShowStacktrace.ALWAYS

        Throwable exception = exception("<location>", "<message>", new GradleException('<failure>'))

        expect:
        reporter.buildFinished(result(exception))
        output.value == '''
{failure}FAILURE: {normal}{failure}Build failed with an exception.{normal}

* Where:
<location>

* What went wrong:
<message>
Cause: <failure>

* Try:
Run with {userinput}--info{normal} or {userinput}--debug{normal} option to get more log output.

* Exception is:
org.gradle.api.GradleException: <failure>
{stacktrace}
'''
    }

    def reportsTaskSelectionException() {
        Throwable exception = new TaskSelectionException("<message>");

        expect:
        reporter.buildFinished(result(exception))
        output.value == '''
{failure}FAILURE: {normal}{failure}Could not determine which tasks to execute.{normal}

* What went wrong:
<message>

* Try:
Run {userinput}[gradle tasks]{normal} to get a list of available tasks.
'''
    }

    def reportsBuildFailureWhenShowStacktraceEnabled() {
        startParameter.showStacktrace = ShowStacktrace.ALWAYS

        GradleException exception = new GradleException('<message>')

        expect:
        reporter.buildFinished(result(exception))
        output.value == '''
{failure}FAILURE: {normal}{failure}Build failed with an exception.{normal}

* What went wrong:
<message>

* Try:
Run with {userinput}--info{normal} or {userinput}--debug{normal} option to get more log output.

* Exception is:
org.gradle.api.GradleException: <message>
{stacktrace}
'''
    }

    def reportsBuildFailureWhenShowFullStacktraceEnabled() {
        startParameter.showStacktrace = ShowStacktrace.ALWAYS_FULL

        GradleException exception = new GradleException('<message>')

        expect:
        reporter.buildFinished(result(exception))
        output.value == '''
{failure}FAILURE: {normal}{failure}Build failed with an exception.{normal}

* What went wrong:
<message>

* Try:
Run with {userinput}--info{normal} or {userinput}--debug{normal} option to get more log output.

* Exception is:
org.gradle.api.GradleException: <message>
{stacktrace}
'''
    }

    def reportsBuildFailureWhenDebugLoggingEnabled() {
        startParameter.logLevel = LogLevel.DEBUG

        GradleException exception = new GradleException('<message>')

        expect:
        reporter.buildFinished(result(exception))
        output.value == '''
{failure}FAILURE: {normal}{failure}Build failed with an exception.{normal}

* What went wrong:
<message>

* Exception is:
org.gradle.api.GradleException: <message>
{stacktrace}
'''
    }

    def result(Throwable failure) {
        BuildResult result = Mock()
        result.failure >> failure
        result
    }

    def exception(final String location, final String message, final Throwable... causes) {
        TestException exception = Mock()
        exception.location >> location
        exception.originalMessage >> message
        exception.reportableCauses >> (causes as List)
        exception.cause >> causes[0]
        exception
    }
}

public abstract class TestException extends LocationAwareException {
    TestException(Throwable cause, ScriptSource source, Integer lineNumber) {
        super(cause, source, lineNumber)
    }
}
