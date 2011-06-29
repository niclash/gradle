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

package org.gradle.util

import spock.lang.Specification

class TextUtilTest extends Specification {
    private static String sep = "separator"
    private static String platformSep = TextUtil.platformLineSeparator

    def convertLineSeparators() {
        expect:
        TextUtil.convertLineSeparators(original, sep) == converted

        where:
        original                          | converted
        "one\rtwo\nthree\r\nfour\n\rfive" | "one${sep}two${sep}three${sep}four${sep}${sep}five"
    }

    def toPlatformLineSeparators() {
        expect:
        TextUtil.toPlatformLineSeparators(original) == converted

        where:
        original | converted
        "one\rtwo\nthree\r\nfour\n\rfive" | "one${platformSep}two${platformSep}three${platformSep}four${platformSep}${platformSep}five"
    }
}
