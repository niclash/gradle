package org.qi4j.gradle.cobertura;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.slf4j.Logger;

/**
 * This task instruments code using Cobertura.
 *
 * There are two conventions here:
 *
 * classesDir - the directory of class files to instrument. This defaults to
 * the output directory for the Java task.
 *
 * outputDir - the directory to output instrumented classes to. This
 * defaults to {BUILD-DIR}/instrumented-classes
 *
 * @author Phil Messenger
 */
public class CoberturaInstrumentTask extends AbstractCoberturaTask
{

    private File classesDir;

    private String outputDir;

    public File getClassesDir()
    {
        if( classesDir == null )
        {
            return (File) getProject().getConvention().getProperty( "classesDir" );
        }
        else
        {
            return classesDir;
        }
    }

    public void setClassesDir( File classesDir )
    {
        this.classesDir = classesDir;
    }

    public String getOutputDir()
    {
        return getProject().getBuildDir().getName() + File.separator + "instrumented-classes";
    }

    public CoberturaInstrumentTask()
    {
        doFirst( new Action<Task>()
        {
            @Override
            public void execute( Task task )
            {
                try
                {
                    doInstrumentation( task.getProject().getProjectDir() );
                }
                catch( Exception e )
                {
                    throw new GradleException( "Cobertura instrumentation failed.", e );
                }
            }
        } );
    }

    private void doInstrumentation( File projectRoot )
    {
        Logger logger = getProject().getLogger();

        String classPath = getCoberturaClasspath();

        String javaExecutable = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";

        StringBuilder commandLine = new StringBuilder();

        commandLine.append( javaExecutable );
        commandLine.append( " -cp " );
        commandLine.append( classPath );
        commandLine.append( " " );

        commandLine.append( COBERTURA_INSTRUMENT_CLASS );

        commandLine.append( " --baseDir " ).append( getClassesDir() );

        commandLine.append( " --destination " ).append( new File( projectRoot, getOutputDir() ) );

        logger.debug( "going to execute cobertura: {}", commandLine );

        logger.info( "Classes directory: {}", getClassesDir() );

        logger.info( "Output directory: {}", getOutputDir() );

        try
        {
            Process p = Runtime.getRuntime().exec( commandLine.toString() );

            BufferedReader stdInput = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
            BufferedReader stdError = new BufferedReader( new InputStreamReader( p.getErrorStream() ) );

            String s;

            while( ( s = stdInput.readLine() ) != null )
            {
                logger.info( s );
            }

            // read any errors from the attempted command
            while( ( s = stdError.readLine() ) != null )
            {
                logger.error( s );
            }

            int result = p.waitFor();

            logger.info( "Cobertura exit status: {}", result );

            if( result > 0 )
            {
                throw new GradleException( "Coberturba instrumentation task did not execute sucessfully" );
            }
        }
        catch( IOException e )
        {
            throw new GradleException( "Error invoking JVM to instrument code", e );
        }
        catch( InterruptedException e )
        {
            throw new GradleException( "Coberturba instrumentation task execution interrupted", e );
        }
    }
}
