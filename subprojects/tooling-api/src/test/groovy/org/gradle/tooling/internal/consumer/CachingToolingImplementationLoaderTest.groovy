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

import org.gradle.tooling.internal.protocol.ConnectionVersion4
import spock.lang.Specification

class CachingToolingImplementationLoaderTest extends Specification {
    final ToolingImplementationLoader target = Mock()
    final CachingToolingImplementationLoader loader = new CachingToolingImplementationLoader(target)

    def delegatesToTargetLoaderToCreateImplementation() {
        ConnectionVersion4 connectionImpl = Mock()
        final Distribution distribution = Mock()

        when:
        def impl = loader.create(distribution)

        then:
        impl == connectionImpl
        1 * target.create(distribution) >> connectionImpl
        _ * distribution.toolingImplementationClasspath >> ([new File('a.jar')] as Set)
        0 * _._
    }

    def reusesImplementationWithSameClasspath() {
        ConnectionVersion4 connectionImpl = Mock()
        final Distribution distribution = Mock()

        when:
        def impl = loader.create(distribution)
        def impl2 = loader.create(distribution)

        then:
        impl == connectionImpl
        impl2 == connectionImpl
        1 * target.create(distribution) >> connectionImpl
        _ * distribution.toolingImplementationClasspath >> ([new File('a.jar')] as Set)
        0 * _._
    }

    def createsNewImplementationWhenClasspathNotSeenBefore() {
        ConnectionVersion4 connectionImpl1 = Mock()
        ConnectionVersion4 connectionImpl2 = Mock()
        Distribution distribution1 = Mock()
        Distribution distribution2 = Mock()

        when:
        def impl = loader.create(distribution1)
        def impl2 = loader.create(distribution2)

        then:
        impl == connectionImpl1
        impl2 == connectionImpl2
        1 * target.create(distribution1) >> connectionImpl1
        1 * target.create(distribution2) >> connectionImpl2
        _ * distribution1.toolingImplementationClasspath >> ([new File('a.jar')] as Set)
        _ * distribution2.toolingImplementationClasspath >> ([new File('b.jar')] as Set)
        0 * _._
    }
}
