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
package org.gradle.api.internal

import org.gradle.api.Action
import org.gradle.api.artifacts.maven.XmlProvider
import org.gradle.util.TextUtil
import spock.lang.Specification

class XmlTransformerTest extends Specification {
    final XmlTransformer transformer = new XmlTransformer()

    def "returns original string when no actions are provided"() {
        expect:
        looksLike '<root/>', transformer.transform('<root/>')
    }

    def "action can access XML as StringBuilder"() {
        Action<XmlProvider> action = Mock()
        transformer.addAction(action)

        when:
        def result = transformer.transform('<root/>')

        then:
        action.execute(_) >> { XmlProvider provider ->
            def builder = provider.asString()
            builder.insert(builder.indexOf("root"), 'some-')
        }
        looksLike '<some-root/>', result
    }

    def "action can access XML as Node"() {
        Action<XmlProvider> action = Mock()
        transformer.addAction(action)

        when:
        def result = transformer.transform('<root/>')

        then:
        action.execute(_) >> { XmlProvider provider ->
            provider.asNode().appendNode('child1')
        }
        looksLike '<root>\n  <child1/>\n</root>\n', result
    }

    def "action can access XML as DOM element"() {
        Action<XmlProvider> action = Mock()
        transformer.addAction(action)

        when:
        def result = transformer.transform('<root/>')

        then:
        action.execute(_) >> { XmlProvider provider ->
            def document = provider.asElement().ownerDocument
            provider.asElement().appendChild(document.createElement('child1'))
        }
        looksLike '<root>\n  <child1/>\n</root>\n', result
    }

    def "can transform String to a Writer"() {
        Action<XmlProvider> action = Mock()
        transformer.addAction(action)
        StringWriter writer = new StringWriter()

        when:
        transformer.transform('<root/>', writer)

        then:
        action.execute(_) >> { XmlProvider provider ->
            provider.asNode().appendNode('child1')
        }
        looksLike '<root>\n  <child1/>\n</root>\n', writer.toString()
    }

    def "can transform Node to a Writer"() {
        Action<XmlProvider> action = Mock()
        transformer.addAction(action)
        StringWriter writer = new StringWriter()
        Node node = new XmlParser().parseText('<root/>')

        when:
        transformer.transform(node, writer)

        then:
        action.execute(_) >> { XmlProvider provider ->
            provider.asNode().appendNode('child1')
        }
        looksLike '<root>\n  <child1/>\n</root>\n', writer.toString()
    }

    def "can use a closure as an action"() {
        transformer.addAction { provider ->
            provider.asNode().appendNode('child1')
        }
        StringWriter writer = new StringWriter()

        when:
        transformer.transform('<root/>', writer)

        then:
        looksLike '<root>\n  <child1/>\n</root>\n', writer.toString()
    }

    def "can chain actions"() {
        Action<XmlProvider> stringAction = Mock()
        Action<XmlProvider> nodeAction = Mock()
        Action<XmlProvider> elementAction = Mock()
        Action<XmlProvider> stringAction2 = Mock()
        transformer.addAction(stringAction)
        transformer.addAction(elementAction)
        transformer.addAction(nodeAction)
        transformer.addAction(stringAction2)

        when:
        def result = transformer.transform('<root/>')

        then:
        stringAction.execute(_) >> { XmlProvider provider ->
            def builder = provider.asString()
            builder.insert(builder.indexOf("root"), 'some-')
        }
        nodeAction.execute(_) >> { XmlProvider provider ->
            provider.asNode().appendNode('child2')
        }
        elementAction.execute(_) >> { XmlProvider provider ->
            def document = provider.asElement().ownerDocument
            provider.asElement().appendChild(document.createElement('child1'))
        }
        stringAction2.execute(_) >> { XmlProvider provider ->
            provider.asString().append('<!-- end -->')
        }

        looksLike '<some-root>\n  <child1/>\n  <child2/>\n</some-root>\n<!-- end -->', result
    }

    def "can chain node actions"() {
        Action<XmlProvider> nodeAction = Mock()
        Action<XmlProvider> nodeAction2 = Mock()
        transformer.addAction(nodeAction)
        transformer.addAction(nodeAction2)

        when:
        def result = transformer.transform('<root/>')

        then:
        nodeAction.execute(_) >> { XmlProvider provider ->
            provider.asNode().appendNode('child1')
        }
        nodeAction2.execute(_) >> { XmlProvider provider ->
            provider.asNode().appendNode('child2')
        }
        looksLike '<root>\n  <child1/>\n  <child2/>\n</root>\n', result
    }

    def "indentation correct when writing out Node"() {
        transformer.indentation = "\t"
        transformer.addAction { XmlProvider provider -> provider.asNode().children()[0].appendNode("grandchild") }

        when:
        def result = transformer.transform("<root>\n  <child/>\n</root>\n")

        then:
        looksLike "<root>\n\t<child>\n\t\t<grandchild/>\n\t</child>\n</root>\n", result
    }

    def "allows adding DOCTYPE along with nodes"() {
        transformer.addAction { it.asNode().appendNode('someChild') }
        transformer.addAction {
            def s = it.asString()
            s.insert(s.indexOf("?>") + 2, '\n<!DOCTYPE application PUBLIC "-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN" "http://java.sun.com/dtd/application_1_3.dtd">')
        }

        when:
        def result = transformer.transform("<root></root>")

        then:
        result == TextUtil.toPlatformLineSeparators("<?xml version=\"1.0\"?>\n<!DOCTYPE application PUBLIC \"-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN\" \"http://java.sun.com/dtd/application_1_3.dtd\">\n<root>\n  <someChild/>\n</root>\n")
    }

    def "indentation correct when writing out DOM element (only) if indenting with spaces"() {
        transformer.indentation = expected
        transformer.addAction { XmlProvider provider ->
            def document = provider.asElement().ownerDocument
            document.getElementsByTagName("child").item(0).appendChild(document.createElement("grandchild"))
        }

        when:
        def result = transformer.transform("<root>\n  <child/>\n</root>\n")

        then:
        looksLike("<root>\n$actual<child>\n$actual$actual<grandchild/>\n$actual</child>\n</root>\n", result)

        where:
        expected | actual
        "    "   | "    "
        "\t"     | "  " // tabs not supported, two spaces used instead
    }

    private void looksLike(String expected, String actual) {
        assert actual == TextUtil.toPlatformLineSeparators(addXmlDeclaration(expected))
    }

    private String addXmlDeclaration(String value) {
        "<?xml version=\"1.0\"?>\n" + value
    }
}
