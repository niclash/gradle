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
package org.gradle.api.internal.file.collections;

import groovy.lang.Closure;
import static org.gradle.api.file.FileVisitorUtil.*;
import static org.gradle.api.tasks.AntBuilderAwareUtil.*;
import org.gradle.util.TestFile;
import org.gradle.util.HelperUtil;
import org.gradle.util.TemporaryFolder;
import static org.gradle.util.WrapUtil.*;
import static org.hamcrest.Matchers.*;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

public class MapFileTreeTest {
    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();
    private TestFile rootDir = tmpDir.getDir();
    private final MapFileTree tree = new MapFileTree(rootDir);

    @Test
    public void isEmptyByDefault() {
        List<String> emptyList = toList();
        assertVisits(tree, emptyList, emptyList);
        assertSetContainsForAllTypes(tree, emptyList);
    }
    
    @Test
    public void canAddAnElementUsingAClosureToGeneratedContent() {
        Closure closure = HelperUtil.toClosure("{it.write('content'.getBytes())}");
        tree.add("path/file.txt", closure);

        assertVisits(tree, toList("path/file.txt"), toList("path"));
        assertSetContainsForAllTypes(tree, toList("path/file.txt"));

        rootDir.file("path").assertIsDir();
        rootDir.file("path/file.txt").assertContents(equalTo("content"));
    }

    @Test
    public void canAddMultipleElementsInDifferentDirs() {
        Closure closure = HelperUtil.toClosure("{it.write('content'.getBytes())}");
        tree.add("path/file.txt", closure);
        tree.add("file.txt", closure);
        tree.add("path/subdir/file.txt", closure);

        assertVisits(tree, toList("path/file.txt", "file.txt", "path/subdir/file.txt"), toList("path", "path/subdir"));
        assertSetContainsForAllTypes(tree, toList("path/file.txt", "file.txt", "path/subdir/file.txt"));
    }

    @Test
    public void canStopVisitingElements() {
        Closure closure = HelperUtil.toClosure("{it.write('content'.getBytes())}");
        tree.add("path/file.txt", closure);
        tree.add("file.txt", closure);
        assertCanStopVisiting(tree);
    }
}
