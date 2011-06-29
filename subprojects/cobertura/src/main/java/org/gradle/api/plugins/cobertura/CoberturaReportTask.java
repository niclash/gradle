package org.qi4j.gradle.cobertura;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;

/**
 * This task generates Cobertura reports. It has two overridable settings
 *
 * coberturaData - the cobertura datafile to use when generating reports. Defaults to {PROJECT DIR}/cobertura.ser
 * reportDirectory - the directory to emit reports to. Defaults to {BUILD DIR}/reports/cobertura
 *
 * @author phil
 */
public class CoberturaReportTask extends AbstractCoberturaTask
{
    private File coberturaData;

    private File reportDirectory;

    private List<File> srcDirs;

    public void setCoberturaData( File coberturaData )
    {
        this.coberturaData = coberturaData;
    }

    @SuppressWarnings( "unchecked" )
    public List<File> getSrcDirs()
    {
        if( srcDirs == null )
        {
            return (List<File>) getProject().getConvention().getProperty( "srcDirs" );
        }
        else
        {
            return srcDirs;
        }
    }

    public void setSrcDirs( List<File> srcDirs )
    {
        this.srcDirs = srcDirs;
    }

    public File getCoberturaData()
    {
        if( coberturaData == null )
        {
            return getProject().file( "cobertura.ser" );
        }
        else
        {
            return coberturaData;
        }
    }

    public void setCoberturaOutput( File coberturaOutput )
    {
        this.coberturaData = coberturaOutput;
    }

    public File getReportDirectory()
    {
        if( reportDirectory == null )
        {
            return new File( getProject().getBuildDir(), "reports/cobertura" );
        }
        else
        {
            return reportDirectory;
        }
    }

    public void setReportDirectory( File reportDirectory )
    {
        this.reportDirectory = reportDirectory;
    }

    public CoberturaReportTask()
    {
        doFirst( new Action<Task>()
        {
            @Override
            public void execute( Task task )
            {
                try
                {
                    doReport();
                }
                catch( Exception e )
                {
                    throw new GradleException( "Failed Cobertura report.", e );
                }
            }
        } );
    }

    private void doReport()
    {
        Logger logger = getProject().getLogger();

        String[] classPath = getCoberturaClasspath();

        String javaExecutable = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";

        StringBuilder commandLine = new StringBuilder();

        commandLine.append( javaExecutable );
        commandLine.append( " -cp " );
        commandLine.append( classPath[ 0 ] );
        commandLine.append( ":" );
        commandLine.append( classPath[ 1 ] );
        commandLine.append( " " );

        commandLine.append( COBERTURA_REPORT_CLASS );

        commandLine.append( " --datafile " ).append( getCoberturaData() );
        commandLine.append( " --destination " ).append( getReportDirectory() );

        for( File f : getSrcDirs() )
        {
            commandLine.append( " " ).append( f );
        }

        try
        {
            Process p = Runtime.getRuntime().exec( commandLine.toString() );

            logger.debug( "Cobertura report executing: {}", commandLine.toString() );

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

            if( result > 0 )
            {
                throw new GradleException( "Coberturba report task did not execute sucessfully" );
            }
        }
        catch( IOException e )
        {
            throw new GradleException( "Error invoking JVM to instrument code", e );
        }
        catch( InterruptedException e )
        {
            throw new GradleException( "Coberturba instrumentation task execution " );
        }
    }
}
