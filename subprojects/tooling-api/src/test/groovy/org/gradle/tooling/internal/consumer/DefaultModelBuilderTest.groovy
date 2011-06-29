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
package org.gradle.tooling.internal.consumer

import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.ResultHandler
import org.gradle.tooling.internal.protocol.ProjectVersion3
import org.gradle.tooling.internal.protocol.ResultHandlerVersion1
import org.gradle.tooling.model.Project
import org.gradle.util.ConcurrentSpecification

class DefaultModelBuilderTest extends ConcurrentSpecification {
    final AsyncConnection protocolConnection = Mock()
    final ProtocolToModelAdapter adapter = Mock()
    final ConnectionParameters parameters = Mock()
    final DefaultModelBuilder<Project> builder = new DefaultModelBuilder<Project>(Project, ProjectVersion3, protocolConnection, adapter, parameters)

    def getModelDelegatesToProtocolConnectionToFetchModel() {
        ResultHandler<Project> handler = Mock()
        ResultHandlerVersion1<ProjectVersion3> adaptedHandler
        ProjectVersion3 result = Mock()
        Project adaptedResult = Mock()

        when:
        builder.get(handler)

        then:
        1 * protocolConnection.getModel(ProjectVersion3, !null, !null) >> {args ->
            def params = args[1]
            assert params.standardOutput == null
            assert params.standardError == null
            assert params.progressListener != null
            adaptedHandler = args[2]
        }

        when:
        adaptedHandler.onComplete(result)

        then:
        1 * adapter.adapt(Project.class, result) >> adaptedResult
        1 * handler.onComplete(adaptedResult)
        0 * _._
    }

    def getModelWrapsFailureToFetchModel() {
        ResultHandler<Project> handler = Mock()
        ResultHandlerVersion1<ProjectVersion3> adaptedHandler
        RuntimeException failure = new RuntimeException()
        GradleConnectionException wrappedFailure

        when:
        builder.get(handler)

        then:
        1 * protocolConnection.getModel(!null, !null, !null) >> {args -> adaptedHandler = args[2]}

        when:
        adaptedHandler.onFailure(failure)

        then:
        1 * handler.onFailure(!null) >> {args -> wrappedFailure = args[0] }
        _ * protocolConnection.displayName >> '[connection]'
        wrappedFailure.message == 'Could not fetch model of type \'Project\' using [connection].'
        wrappedFailure.cause.is(failure)
        0 * _._
    }

    def getModelBlocksUntilResultReceivedFromProtocolConnection() {
        def supplyResult = waitsForAsyncCallback()
        ProjectVersion3 result = Mock()
        Project adaptedResult = Mock()
        _ * adapter.adapt(Project.class, result) >> adaptedResult

        when:
        def model
        supplyResult.start {
            model = builder.get()
        }

        then:
        model == adaptedResult
        1 * protocolConnection.getModel(!null, !null, !null) >> { args ->
            def handler = args[2]
            supplyResult.callbackLater {
                handler.onComplete(result)
            }
        }
    }

    def getModelBlocksUntilFailureReceivedFromProtocolConnectionAndRethrowsFailure() {
        def supplyResult = waitsForAsyncCallback()
        RuntimeException failure = new RuntimeException()

        when:
        def model
        supplyResult.start {
            model = builder.get()
        }

        then:
        GradleConnectionException e = thrown()
        e.cause.is(failure)
        1 * protocolConnection.getModel(!null, !null, !null) >> { args ->
            def handler = args[2]
            supplyResult.callbackLater {
                handler.onFailure(failure)
            }
        }
    }
}


