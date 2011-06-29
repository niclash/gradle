package org.qi4j.gradle.cobertura;

import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.util.GUtil;

/**
 * @author Phil Messenger
 */
public class CoberturaPlugin
    implements Plugin<Project>
{
    /**
     * Configure task dependencies on the building of child projects
     *
     * @param project The project that the Plugin is applied to.
     *
     * @return The dependencies that this plugin has.
     */
    private List<String> getProjectDependencies( Project project )
    {
        List<String> dependencies = new ArrayList<String>();
        dependencies.add( JavaPlugin.COMPILE_JAVA_TASK_NAME );
        return dependencies;
    }

    @Override
    public void apply( Project project )
    {
        CoberturaInstrumentTask instrumentTask = (CoberturaInstrumentTask) project.createTask( GUtil.map( "type", CoberturaInstrumentTask.class, Task.TASK_DEPENDS_ON, getProjectDependencies( project ) ), "cbInstrument" );
        CoberturaReportTask reportTask = (CoberturaReportTask) project.createTask( GUtil.map( "type", CoberturaReportTask.class, Task.TASK_DEPENDS_ON, getProjectDependencies( project ) ), "cbReport" );
        ConfigurationContainer configurations = project.getConfigurations();
        configurations.add( "coberturaTool" );

        // We need to modify the JavaPlugin's sourceSets.
        Convention convention = project.getConvention();
        JavaPluginConvention javaPlugin = convention.getPlugin( JavaPluginConvention.class );
        SourceSetContainer sourceSets = javaPlugin.getSourceSets();
        SourceSet main = sourceSets.getByName( SourceSet.MAIN_SOURCE_SET_NAME );
        SourceSet test = sourceSets.getByName( SourceSet.TEST_SOURCE_SET_NAME );
        Configuration testCompileConf = configurations.getByName( JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME );
        Configuration testRuntimeConf = configurations.getByName( JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME );
        test.setCompileClasspath( project.files( main.getClasses(), testCompileConf ) );
        test.setRuntimeClasspath( project.files( test.getClasses(), main.getClasses(), testRuntimeConf ) );
    }
}
