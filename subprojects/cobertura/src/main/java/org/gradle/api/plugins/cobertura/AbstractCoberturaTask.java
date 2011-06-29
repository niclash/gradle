package org.qi4j.gradle.cobertura;

import java.io.File;
import java.util.Set;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.internal.ConventionTask;

/**
 * Provides some shared functionality for other Cobertura classes
 *
 * @author Phil Messenger
 */
public abstract class AbstractCoberturaTask extends ConventionTask
{

    protected String COBERTURA_INSTRUMENT_CLASS = "net.sourceforge.cobertura.instrument.Main";
    protected String COBERTURA_REPORT_CLASS = "net.sourceforge.cobertura.reporting.Main";

    public AbstractCoberturaTask()
    {
    }

    /**
     * Build the classpath required to execute the Cobertura command line utilities
     *
     * @return A fully built classpath with platform-dependent separators.
     */
    protected String getCoberturaClasspath()
    {
        Set<File> files = getProject().getConfigurations().getByName( "coberturaTool" ).getFiles();
        StringBuilder result = new StringBuilder();
        for( File file : files )
        {
            result.append( file.getAbsolutePath() );
            result.append( File.separator );
        }
        return result.toString();
    }
}
