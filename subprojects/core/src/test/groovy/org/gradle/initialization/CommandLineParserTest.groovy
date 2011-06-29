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
package org.gradle.initialization

import org.gradle.CommandLineArgumentException
import spock.lang.Specification

class CommandLineParserTest extends Specification {
    private final CommandLineParser parser = new CommandLineParser()

    def parsesEmptyCommandLine() {
        parser.option('a')
        parser.option('long-value')

        expect:
        def result = parser.parse([])
        !result.hasOption('a')
        !result.hasOption('long-value')
        result.extraArguments == []
    }

    def parsesShortOption() {
        parser.option('a')
        parser.option('b')

        expect:
        def result = parser.parse(['-a'])
        result.hasOption('a')
        !result.hasOption('b')
    }

    def canUseDoubleDashesForShortOptions() {
        parser.option('a')

        expect:
        def result = parser.parse(['--a'])
        result.hasOption('a')
    }

    def parsesShortOptionWithArgument() {
        parser.option('a').hasArgument()

        expect:
        def result = parser.parse(['-a', 'arg'])
        result.hasOption('a')
        result.option('a').value == 'arg'
        result.option('a').values == ['arg']
    }

    def parsesShortOptionWithAttachedArgument() {
        parser.option('a').hasArgument()

        expect:
        def result = parser.parse(['-aarg'])
        result.hasOption('a')
        result.option('a').value == 'arg'
        result.option('a').values == ['arg']
    }

    def attachedArgumentTakesPrecedenceOverCombinedOption() {
        parser.option('a').hasArgument()
        parser.option('b')

        expect:
        def result = parser.parse(['-ab'])
        result.hasOption('a')
        result.option('a').value == 'b'
        !result.hasOption('b')
    }

    def parsesShortOptionWithEqualArgument() {
        parser.option('a').hasArgument()

        expect:
        def result = parser.parse(['-a=arg'])
        result.hasOption('a')
        result.option('a').value == 'arg'
        result.option('a').values == ['arg']
    }

    def parsesShortOptionWithEqualsCharacterInAttachedArgument() {
        parser.option('a').hasArgument()

        expect:
        def result = parser.parse(['-avalue=arg'])
        result.hasOption('a')
        result.option('a').value == 'value=arg'
        result.option('a').values == ['value=arg']
    }

    def parsesShortOptionWithDashCharacterInAttachedArgument() {
        parser.option('a').hasArgument()

        expect:
        def result = parser.parse(['-avalue-arg'])
        result.hasOption('a')
        result.option('a').value == 'value-arg'
        result.option('a').values == ['value-arg']
    }

    def parsesCombinedShortOptions() {
        parser.option('a')
        parser.option('b')

        expect:
        def result = parser.parse(['-ab'])
        result.hasOption('a')
        result.hasOption('b')
    }

    def parsesLongOption() {
        parser.option('long-option-a')
        parser.option('long-option-b')

        expect:
        def result = parser.parse(['--long-option-a'])
        result.hasOption('long-option-a')
        !result.hasOption('long-option-b')
    }

    def canUseSingleDashForLongOptions() {
        parser.option('long')
        parser.option('other').hasArgument()

        expect:
        def result = parser.parse(['-long', '-other', 'arg'])
        result.hasOption('long')
        result.hasOption('other')
        result.option('other').value == 'arg'
    }

    def parsesLongOptionWithArgument() {
        parser.option('long-option-a').hasArgument()
        parser.option('long-option-b')

        expect:
        def result = parser.parse(['--long-option-a', 'arg'])
        result.hasOption('long-option-a')
        result.option('long-option-a').value == 'arg'
        result.option('long-option-a').values == ['arg']
    }

    def parsesLongOptionWithEqualsArgument() {
        parser.option('long-option-a').hasArgument()

        expect:
        def result = parser.parse(['--long-option-a=arg'])
        result.hasOption('long-option-a')
        result.option('long-option-a').value == 'arg'
        result.option('long-option-a').values == ['arg']
    }

    def parsesMultipleOptions() {
        parser.option('a').hasArgument()
        parser.option('long-option')

        expect:
        def result = parser.parse(['--long-option', '-a', 'arg'])
        result.hasOption('long-option')
        result.hasOption('a')
        result.option('a').value == 'arg'
    }

    def parsesOptionWithMultipleAliases() {
        parser.option('a', 'b', 'long-option-a')

        expect:
        def longOptionResult = parser.parse(['--long-option-a'])
        longOptionResult.hasOption('a')
        longOptionResult.hasOption('b')
        longOptionResult.hasOption('long-option-a')
        longOptionResult.option('a') == longOptionResult.option('long-option-a')
        longOptionResult.option('a') == longOptionResult.option('b')

        def shortOptionResult = parser.parse(['-a'])
        shortOptionResult.hasOption('a')
        shortOptionResult.hasOption('b')
        shortOptionResult.hasOption('long-option-a')
    }

    def parsesCommandLineWhenOptionAppearsMultipleTimes() {
        parser.option('a', 'b', 'long-option-a')

        expect:
        def result = parser.parse(['--long-option-a', '-a', '-a', '-b'])
        result.hasOption('a')
        result.hasOption('b')
        result.hasOption('long-option-a')
    }

    def parsesOptionWithMultipleArguments() {
        parser.option('a', 'long').hasArguments()

        expect:
        def result = parser.parse(['-a', 'arg1', '--long', 'arg2', '-aarg3', '--long=arg4'])
        result.hasOption('a')
        result.hasOption('long')
        result.option('a').values == ['arg1', 'arg2', 'arg3', 'arg4']
    }

    def parsesCommandLineWithSubcommand() {
        parser.option('a')

        expect:
        def singleArgResult = parser.parse(['a'])
        singleArgResult.extraArguments == ['a']
        !singleArgResult.hasOption('a')

        def multipleArgsResult = parser.parse(['a', 'b'])
        multipleArgsResult.extraArguments == ['a', 'b']
        !multipleArgsResult.hasOption('a')
    }

    def parsesCommandLineWithOptionsAndSubcommand() {
        parser.option('a')

        expect:
        def optionBeforeSubcommandResult = parser.parse(['-a', 'a'])
        optionBeforeSubcommandResult.extraArguments == ['a']
        optionBeforeSubcommandResult.hasOption('a')

        def optionAfterSubcommandResult = parser.parse(['a', '-a'])
        optionAfterSubcommandResult.extraArguments == ['a', '-a']
        !optionAfterSubcommandResult.hasOption('a')
    }

    def parsesCommandLineWithOptionsAndSubcommandWhenMixedOptionsAllowed() {
        parser.option('a')
        parser.allowMixedSubcommandsAndOptions()

        expect:
        def optionBeforeSubcommandResult = parser.parse(['-a', 'a'])
        optionBeforeSubcommandResult.extraArguments == ['a']
        optionBeforeSubcommandResult.hasOption('a')

        def optionAfterSubcommandResult = parser.parse(['a', '-a'])
        optionAfterSubcommandResult.extraArguments == ['a']
        optionAfterSubcommandResult.hasOption('a')
    }

    def parsesCommandLineWithSubcommandThatHasOptions() {
        when:
        def result = parser.parse(['a', '--option', 'b'])

        then:
        result.extraArguments == ['a', '--option', 'b']

        when:
        parser.allowMixedSubcommandsAndOptions()
        result = parser.parse(['a', '--option', 'b'])

        then:
        result.extraArguments == ['a', '--option', 'b']
    }

    def canMapOptionToSubcommand() {
        parser.option('a').mapsToSubcommand('subcmd')

        expect:
        def result = parser.parse(['-a', '--option', 'b'])
        result.extraArguments == ['subcmd', '--option', 'b']
        result.hasOption('a')
    }

    def canCombineSubcommandShortOptionWithOtherShortOptions() {
        parser.option('a').mapsToSubcommand('subcmd')
        parser.option('b')

        when:
        def result = parser.parse(['-abc', '--option', 'b'])

        then:
        result.extraArguments == ['subcmd', '-b', '-c', '--option', 'b']
        result.hasOption('a')
        !result.hasOption('b')

        when:
        result = parser.parse(['-bac', '--option', 'b'])

        then:
        result.extraArguments == ['subcmd', '-c', '--option', 'b']
        result.hasOption('a')
        result.hasOption('b')

        when:
        parser.allowMixedSubcommandsAndOptions()
        result = parser.parse(['-abc', '--option', 'b'])

        then:
        result.extraArguments == ['subcmd', '-c', '--option', 'b']
        result.hasOption('a')
        result.hasOption('b')

        when:
        result = parser.parse(['-bac', '--option', 'b'])

        then:
        result.extraArguments == ['subcmd', '-c', '--option', 'b']
        result.hasOption('a')
        result.hasOption('b')
    }

    def singleDashIsNotConsideredAnOption() {
        expect:
        def result = parser.parse(['-'])
        result.extraArguments == ['-']
    }

    def doubleDashMarksEndOfOptions() {
        parser.option('a')

        expect:
        def result = parser.parse(['--', '-a'])
        result.extraArguments == ['-a']
        !result.hasOption('a')
    }

    def valuesEmptyWhenOptionIsNotPresentInCommandLine() {
        parser.option('a').hasArgument()

        expect:
        def result = parser.parse([])
        result.option('a').values == []
    }

    def formatsUsageMessage() {
        parser.option('a', 'long-option').hasDescription('this is option a')
        parser.option('b')
        parser.option('another-long-option').hasDescription('this is a long option')
        parser.option('z', 'y', 'last-option', 'end-option').hasDescription('this is the last option')
        parser.option('B')
        def outstr = new ByteArrayOutputStream()

        expect:
        parser.printUsage(outstr)
        outstr.toString().readLines() == [
                '-a, --long-option                    this is option a',
                '--another-long-option                this is a long option',
                '-B',
                '-b',
                '-y, -z, --end-option, --last-option  this is the last option'
        ]
    }

    def showsDeprecationWarning() {
        def parser = new CommandLineParser()
        parser.option("foo").hasDescription("usless option, just for testing").deprecated("deprecated. Please use --bar instead.")
        parser.option("x").hasDescription("I'm not deprecated")

        when:
        parser.deprecationPrinter = new ByteArrayOutputStream()
        parser.parse(["-x"])

        then:
        parser.deprecationPrinter.toString() == ''

        when:
        parser.deprecationPrinter = new ByteArrayOutputStream()
        parser.parse(["--foo"])

        then:
        parser.deprecationPrinter.toString().contains("deprecated. Please use --bar instead.")
    }

    def parseFailsWhenCommandLineContainsUnknownShortOption() {
        when:
        parser.parse(['-a'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'Unknown command-line option \'-a\'.'
    }

    def parseFailsWhenCommandLineContainsUnknownShortOptionWithDoubleDashes() {
        when:
        parser.parse(['--a'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'Unknown command-line option \'--a\'.'
    }

    def parseFailsWhenCommandLineContainsUnknownShortOptionWithEqualsArgument() {
        when:
        parser.parse(['-a=arg'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'Unknown command-line option \'-a\'.'
    }

    def parseFailsWhenCommandLineContainsUnknownShortOptionWithAttachedArgument() {
        when:
        parser.parse(['-aarg'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'Unknown command-line option \'-a\'.'
    }

    def parseFailsWhenCommandLineContainsUnknownLongOption() {
        when:
        parser.parse(['--unknown'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'Unknown command-line option \'--unknown\'.'
    }

    def parseFailsWhenCommandLineContainsUnknownLongOptionWithSingleDashes() {
        when:
        parser.parse(['-unknown'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'Unknown command-line option \'-u\'.'
    }

    def parseFailsWhenCommandLineContainsUnknownLongOptionWithEqualsArgument() {
        when:
        parser.parse(['--unknown=arg'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'Unknown command-line option \'--unknown\'.'
    }

    def parseFailsWhenCommandLineContainsLongOptionWithAttachedArgument() {
        parser.option("long").hasArgument()

        when:
        parser.parse(['--longvalue'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'Unknown command-line option \'--longvalue\'.'
    }

    def parseFailsWhenCommandLineContainsDashAndEquals() {
        parser.option("long").hasArgument()

        when:
        parser.parse(['-='])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'Unknown command-line option \'-=\'.'
    }

    def getOptionFailsForUnknownOption() {
        def result = parser.parse(['other'])

        when:
        result.option('unknown')

        then:
        def e = thrown(IllegalArgumentException)
        e.message == 'Option \'unknown\' not defined.'

        when:
        result.hasOption('unknown')

        then:
        e = thrown(IllegalArgumentException)
        e.message == 'Option \'unknown\' not defined.'
    }

    def parseFailsWhenSingleValueOptionHasMultipleArguments() {
        parser.option('a').hasArgument()

        when:
        parser.parse(['-a=arg1', '-a', 'arg2'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'Multiple arguments were provided for command-line option \'-a\'.'
    }

    def parseFailsWhenArgumentIsMissing() {
        parser.option('a').hasArgument()

        when:
        parser.parse(['-a'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'No argument was provided for command-line option \'-a\'.'
    }

    def parseFailsWhenArgumentIsMissingFromEqualsForm() {
        parser.option('a').hasArgument()

        when:
        parser.parse(['-a='])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'An empty argument was provided for command-line option \'-a\'.'
    }

    def parseFailsWhenEmptyArgumentIsProvided() {
        parser.option('a').hasArgument()

        when:
        parser.parse(['-a', ''])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'An empty argument was provided for command-line option \'-a\'.'
    }

    def parseFailsWhenArgumentIsMissingAndAnotherOptionFollows() {
        parser.option('a').hasArgument()

        when:
        parser.parse(['-a', '-b'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'No argument was provided for command-line option \'-a\'.'
    }

    def parseFailsWhenArgumentIsMissingAndOptionsAreCombined() {
        parser.option('a')
        parser.option('b').hasArgument()

        when:
        parser.parse(['-ab'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'No argument was provided for command-line option \'-b\'.'
    }

    def parseFailsWhenAttachedArgumentIsProvidedForOptionWhichDoesNotTakeAnArgument() {
        parser.option('a')

        when:
        parser.parse(['-aarg'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'Unknown command-line option \'-r\'.'
    }

    def parseFailsWhenEqualsArgumentIsProvidedForOptionWhichDoesNotTakeAnArgument() {
        parser.option('a')

        when:
        parser.parse(['-a=arg'])

        then:
        def e = thrown(CommandLineArgumentException)
        e.message == 'Command-line option \'-a\' does not take an argument.'
    }
}
