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

package org.gradle.api.tasks;

import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.internal.artifacts.IvyService;
import org.gradle.util.HelperUtil;
import static org.gradle.util.WrapUtil.toList;
import static org.hamcrest.Matchers.*;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * @author Hans Dockter
 */
@RunWith(JMock.class)
public class UploadTest extends AbstractTaskTest {
    private Upload upload;

    private JUnit4Mockery context = new JUnit4Mockery();
    private RepositoryHandler repositoriesMock;
    private IvyService ivyServiceMock;
    private DependencyResolver repositoryDummy;
    private Configuration configurationMock;

    @Before public void setUp() {
        super.setUp();
        upload = createTask(Upload.class);
        repositoriesMock = context.mock(RepositoryHandler.class);
        repositoryDummy = context.mock(DependencyResolver.class);
        ivyServiceMock = context.mock(IvyService.class);

        context.checking(new Expectations(){{
            allowing(repositoriesMock).getResolvers();
            will(returnValue(toList(repositoryDummy)));
        }});
        configurationMock = context.mock(Configuration.class);
    }

    public AbstractTask getTask() {
        return upload;
    }

    @Test public void testUpload() {
        assertThat(upload.isUploadDescriptor(), equalTo(false));
        assertNull(upload.getDescriptorDestination());
        assertNotNull(upload.getRepositories());
    }

    @Test public void testUploading() {
        final File descriptorDestination = new File("somePath");
        upload.setUploadDescriptor(true);
        upload.setDescriptorDestination(descriptorDestination);
        upload.setConfiguration(configurationMock);
        upload.setIvyService(ivyServiceMock);
        context.checking(new Expectations() {{
            one(ivyServiceMock).publish(configurationMock, descriptorDestination);
        }});
        upload.upload();
    }

    @Test public void testUploadingWithUploadDescriptorFalseAndDestinationSet() {
        upload.setUploadDescriptor(false);
        upload.setDescriptorDestination(new File("somePath"));
        upload.setConfiguration(configurationMock);
        upload.setIvyService(ivyServiceMock);
        context.checking(new Expectations() {{
            one(ivyServiceMock).publish(configurationMock, null);
        }});
        upload.upload();
    }

    @Test public void testRepositories() {
        upload.setRepositories(repositoriesMock);

        context.checking(new Expectations(){{
            one(repositoriesMock).mavenCentral();
        }});

        upload.repositories(HelperUtil.toClosure("{ mavenCentral() }"));
    }

    @Test public void testDeclaresConfigurationArtifactsAsInputFiles() {
        assertThat(upload.getArtifacts(), nullValue());

        upload.setConfiguration(configurationMock);

        final FileCollection files = context.mock(FileCollection.class);
        context.checking(new Expectations(){{
            one(configurationMock).getAllArtifactFiles();
            will(returnValue(files));
        }});

        assertThat(upload.getArtifacts(), sameInstance(files));
    }
}
