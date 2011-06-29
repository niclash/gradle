/*
 * Copyright 2007 the original author or authors.
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

package org.gradle.util

import org.apache.ivy.Ivy
import org.apache.tools.ant.Main
import org.codehaus.groovy.runtime.InvokerHelper
import spock.lang.Specification

/**
 * @author Hans Dockter
 */
class GradleVersionTest extends Specification {
    final GradleVersion version = GradleVersion.current()

    def currentVersionHasNonNullVersionAndBuildTime() {
        expect:
        version.version
        version.buildTime
    }

    def equalsAndHashCode() {
        expect:
        Matchers.strictlyEquals(GradleVersion.version('0.9'), GradleVersion.version('0.9'))
        GradleVersion.version('0.9') != GradleVersion.version('1.0')
    }

    def canConstructPatchVersion() {
        when:
        def version = GradleVersion.version('1.0-milestone-2')
        def patch = GradleVersion.version('1.0-milestone-2a')
        def nextPatch = GradleVersion.version('1.0-milestone-2b')
        def nextMilestone = GradleVersion.version('1.0-milestone-3')

        then:
        version < patch
        patch < nextPatch
        nextPatch < nextMilestone

        patch > version
        nextPatch > patch
        nextMilestone > nextPatch
    }

    def canConstructSnapshotVersion() {
        expect:
        GradleVersion.version('0.9-20101220110000+1100').snapshot
        GradleVersion.version('0.9-20101220110000-0800').snapshot
        !GradleVersion.version('0.9-rc-1').snapshot
    }

    def canCompareMajorVersions() {
        expect:
        GradleVersion.version(a) > GradleVersion.version(b)
        GradleVersion.version(b) < GradleVersion.version(a)
        GradleVersion.version(a) == GradleVersion.version(a)
        GradleVersion.version(b) == GradleVersion.version(b)

        where:
        a | b
        '0.9' | '0.8'
        '1.0' | '0.10'
        '10.0' | '2.1'
        '2.5' | '2.4'
    }

    def canComparePointVersions() {
        expect:
        GradleVersion.version(a) > GradleVersion.version(b)
        GradleVersion.version(b) < GradleVersion.version(a)
        GradleVersion.version(a) == GradleVersion.version(a)
        GradleVersion.version(b) == GradleVersion.version(b)

        where:
        a | b
        '0.9.2' | '0.9.1'
        '0.10.1' | '0.9.2'
    }

    def canComparePointVersionAndMajorVersions() {
        expect:
        GradleVersion.version(a) > GradleVersion.version(b)
        GradleVersion.version(b) < GradleVersion.version(a)
        GradleVersion.version(a) == GradleVersion.version(a)
        GradleVersion.version(b) == GradleVersion.version(b)

        where:
        a | b
        '0.9.1' | '0.9'
        '0.10' | '0.9.1'
    }

    def canComparePreviewsMilestonesAndRCVersions() {
        expect:
        GradleVersion.version(a) > GradleVersion.version(b)
        GradleVersion.version(b) < GradleVersion.version(a)
        GradleVersion.version(a) == GradleVersion.version(a)
        GradleVersion.version(b) == GradleVersion.version(b)

        where:
        a | b
        '1.0-milestone-2' | '1.0-milestone-1'
        '1.0-preview-2' | '1.0-preview-1'
        '1.0-rc-2' | '1.0-rc-1'
        '1.0-preview-1' | '1.0-milestone-7'
        '1.0-rc-7' | '1.0-rc-1'
        '1.0' | '1.0-rc-7'
    }

    def canCompareSnapshotVersions() {
        expect:
        GradleVersion.version(a) > GradleVersion.version(b)
        GradleVersion.version(b) < GradleVersion.version(a)
        GradleVersion.version(a) == GradleVersion.version(a)
        GradleVersion.version(b) == GradleVersion.version(b)

        where:
        a | b
        '0.9-20101220110000+1100' | '0.9-20101220100000+1100'
        '0.9-20101220110000+1000' | '0.9-20101220100000+1100'
        '0.9-20101220110000-0100' | '0.9-20101220100000+0000'
        '0.9' | '0.9-20101220100000+1000'
    }

    def defaultValuesForGradleVersion() {
        expect:
        version.version != null
        version.buildTime != null
    }

    def prettyPrint() {
        String expectedText = """
------------------------------------------------------------
Gradle $version.version
------------------------------------------------------------

Gradle build time: $version.buildTime
Groovy: $InvokerHelper.version
Ant: $Main.antVersion
Ivy: ${Ivy.ivyVersion}
JVM: ${Jvm.current()}
OS: ${OperatingSystem.current()}
"""
        expect:
        version.prettyPrint() == expectedText
    }
}
