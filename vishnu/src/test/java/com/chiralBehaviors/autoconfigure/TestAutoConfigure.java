/** (C) Copyright 2014 Chiral Behaviors, All Rights Reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package com.chiralBehaviors.autoconfigure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.chiralBehaviors.autoconfigure.configuration.JmxConfiguration;
import com.chiralBehaviors.autoconfigure.configuration.ServiceCollection;
import com.chiralBehaviors.autoconfigure.configuration.SingletonService;
import com.chiralBehaviors.autoconfigure.configuration.Template;
import com.chiralBehaviors.autoconfigure.configuration.UniqueDirectory;
import com.hellblazer.slp.ServiceListener;
import com.hellblazer.slp.ServiceReference;
import com.hellblazer.slp.ServiceScope;
import com.hellblazer.slp.ServiceURL;
import com.hellblazer.slp.local.config.LocalScopeConfiguration;
import com.hellblazer.utils.Condition;
import com.hellblazer.utils.TemporaryDirectory;
import com.hellblazer.utils.Utils;

/**
 * @author hhildebrand
 * 
 */
public class TestAutoConfigure {

    @Mock
    private ServiceScope     discovery;
    private String           interfaceName;
    private JmxConfiguration jmxConfig = new JmxConfiguration();

    @Before
    public void setup() throws SocketException {
        MockitoAnnotations.initMocks(this);
        interfaceName = NetworkInterface.getByIndex(1).getName();
    }

    @Test
    public void testConfigurationProcessing() throws Exception {
        try (TemporaryDirectory tempDirectory = new TemporaryDirectory(
                                                                       "config-processing",
                                                                       "dir")) {
            Utils.copyDirectory(new File("src/test/resources/configurations"),
                                tempDirectory.directory);
            String serviceHost = "example.com";
            int servicePort = 1;
            int serviceCollection1Port = 2;
            int serviceCollection2Port = 2;
            String serviceFormat = "service:test:tcp://%s:%s";
            String hostVariable = "host";
            String portVariable = "port";
            String serviceCollectionVariable = "serviceCollection";
            String serviceVariable = "service";
            String serviceCollectionConfig = String.format("%s:%s,%s:%s,",
                                                           serviceHost,
                                                           serviceCollection1Port,
                                                           serviceHost,
                                                           serviceCollection2Port);
            String serviceConfig = String.format("%s:%s", serviceHost,
                                                 servicePort);

            Map<String, String> serviceProperties = new HashMap<String, String>();
            SingletonService service = new SingletonService();
            service.service = "service:testService:tcp";
            service.variable = serviceVariable;
            List<SingletonService> serviceDefinitions = new ArrayList<>();
            serviceDefinitions.add(service);
            ServiceCollection serviceCollection = new ServiceCollection();
            serviceCollection.service = "service:testServiceCollection:tcp";
            serviceCollection.cardinality = 2;
            serviceCollection.variable = serviceCollectionVariable;
            List<UniqueDirectory> uniqueDirectories = new ArrayList<>();
            List<ServiceCollection> serviceCollectionDefinitions = new ArrayList<>();
            serviceCollectionDefinitions.add(serviceCollection);
            List<Template> templates = new ArrayList<>();
            for (File config : tempDirectory.directory.listFiles()) {
                Template template = new Template();
                template.name = String.format("%s.properties",
                                              Utils.getNameWithoutExtension(config));
                template.templateGroup = config.getAbsolutePath();
                template.generated = new File(tempDirectory.directory,
                                              template.name);
                templates.add(template);
            }
            List<String> additionalPorts = new ArrayList<>();
            Map<String, String> substitutions = new HashMap<>();
            substitutions.put("a", "A");
            substitutions.put("b", "B");
            final AtomicBoolean completed = new AtomicBoolean();
            final AtomicBoolean succeeded = new AtomicBoolean();
            final AtomicReference<List<File>> transformedConfigurations = new AtomicReference<List<File>>();
            ServiceReference serviceRef = mock(ServiceReference.class);
            ServiceURL serviceUrl = mock(ServiceURL.class);
            when(serviceRef.getUrl()).thenReturn(serviceUrl);
            when(serviceUrl.getHost()).thenReturn(serviceHost);
            when(serviceUrl.getPort()).thenReturn(servicePort);

            Map<String, String> serviceProps1 = new HashMap<>();
            serviceProps1.put(ServiceScope.SERVICE_REGISTRATION,
                              UUID.randomUUID().toString());
            ServiceReference serviceCollection1Ref = mock(ServiceReference.class);
            ServiceURL serviceCollection1Url = mock(ServiceURL.class);
            when(serviceCollection1Url.getHost()).thenReturn(serviceHost);
            when(serviceCollection1Url.getPort()).thenReturn(serviceCollection1Port);
            when(serviceCollection1Ref.getUrl()).thenReturn(serviceCollection1Url);
            when(serviceCollection1Ref.getProperties()).thenReturn(serviceProps1);

            Map<String, String> serviceProps2 = new HashMap<>();
            serviceProps2.put(ServiceScope.SERVICE_REGISTRATION,
                              UUID.randomUUID().toString());
            ServiceReference serviceCollection2Ref = mock(ServiceReference.class);
            ServiceURL serviceCollection2Url = mock(ServiceURL.class);
            when(serviceCollection2Url.getHost()).thenReturn(serviceHost);
            when(serviceCollection2Url.getPort()).thenReturn(serviceCollection2Port);
            when(serviceCollection2Ref.getUrl()).thenReturn(serviceCollection2Url);
            when(serviceCollection2Ref.getProperties()).thenReturn(serviceProps2);

            AutoConfigure autoConfigure = new AutoConfigure(
                                                            serviceFormat,
                                                            interfaceName,
                                                            0,
                                                            false,
                                                            serviceProperties,
                                                            discovery,
                                                            serviceDefinitions,
                                                            serviceCollectionDefinitions,
                                                            templates,
                                                            substitutions,
                                                            uniqueDirectories,
                                                            additionalPorts,
                                                            null, null, true,
                                                            jmxConfig, null);
            AutoConfigureService configuredService = new AutoConfigureService(
                                                                              autoConfigure) {

                @Override
                public void fail(Map<String, File> configurations)
                                                                  throws Exception {
                    succeeded.set(false);
                    completed.set(true);
                }

                @Override
                public void succeed(Map<String, File> configurations)
                                                                     throws Exception {
                    List<File> configs = new ArrayList<>();
                    File generated = configurations.get("configuration1.properties");
                    configs.add(generated);
                    generated = configurations.get("configuration2.properties");
                    configs.add(generated);
                    transformedConfigurations.set(configs);
                    succeeded.set(true);
                    completed.set(true);
                }
            };
            configuredService.start(100, TimeUnit.MILLISECONDS);
            autoConfigure.discover(serviceRef, service);
            autoConfigure.discover(serviceCollection1Ref, serviceCollection);
            autoConfigure.discover(serviceCollection2Ref, serviceCollection);
            assertTrue("configuration did not complete",
                       Utils.waitForCondition(1000, new Condition() {
                           @Override
                           public boolean isTrue() {
                               return completed.get();
                           }
                       }));
            assertTrue("configuration not successful", succeeded.get());
            assertNotNull(transformedConfigurations.get());
            assertEquals(2, transformedConfigurations.get().size());
            Properties properties1 = new Properties();
            File configuration1 = transformedConfigurations.get().get(0);
            assertNotNull("Configuration 1 was not generated", configuration1);
            assertTrue("configuration 1 does not exist",
                       configuration1.exists());
            FileInputStream is = new FileInputStream(configuration1);
            properties1.load(is);
            is.close();
            Properties properties2 = new Properties();
            File configuration2 = transformedConfigurations.get().get(1);
            assertNotNull("Configuration 2 was not generated", configuration2);
            assertTrue("configuration 2 does not exist",
                       configuration2.exists());
            is = new FileInputStream(configuration2);
            properties2.load(is);
            is.close();
            InetSocketAddress bound = autoConfigure.getBound();
            assertEquals(bound.getHostName(), properties1.get(hostVariable));
            assertEquals(String.valueOf(bound.getPort()),
                         properties1.get(portVariable));
            assertEquals("B", properties1.get("property.b"));
            assertEquals(serviceCollectionConfig,
                         properties2.get(serviceCollectionVariable));
            assertEquals(serviceConfig, properties2.get(serviceVariable));
            assertEquals("A", properties2.get("property.a"));
        }
    }

    @Test
    public void testConfigurationProcessingFromResources() throws Exception {
        try (TemporaryDirectory tempDirectory = new TemporaryDirectory(
                                                                       "config-processing",
                                                                       "dir")) {
            Utils.copyDirectory(new File("src/test/resources/configurations"),
                                tempDirectory.directory);
            String serviceHost = "example.com";
            int servicePort = 1;
            int serviceCollection1Port = 2;
            int serviceCollection2Port = 2;
            String serviceFormat = "service:test:tcp://%s:%s";
            String hostVariable = "host";
            String portVariable = "port";
            String serviceCollectionVariable = "serviceCollection";
            String serviceVariable = "service";
            String serviceCollectionConfig = String.format("%s:%s,%s:%s,",
                                                           serviceHost,
                                                           serviceCollection1Port,
                                                           serviceHost,
                                                           serviceCollection2Port);
            String serviceConfig = String.format("%s:%s", serviceHost,
                                                 servicePort);

            Map<String, String> serviceProperties = new HashMap<String, String>();
            List<UniqueDirectory> uniqueDirectories = new ArrayList<>();

            SingletonService service = new SingletonService();
            service.service = "service:testService:tcp";
            service.variable = serviceVariable;
            List<SingletonService> serviceDefinitions = new ArrayList<>();
            serviceDefinitions.add(service);

            ServiceCollection serviceCollection = new ServiceCollection();
            serviceCollection.service = "service:testServiceCollection:tcp";
            serviceCollection.cardinality = 2;
            serviceCollection.variable = serviceCollectionVariable;
            List<ServiceCollection> serviceCollectionDefinitions = new ArrayList<>();
            serviceCollectionDefinitions.add(serviceCollection);

            List<Template> templates = new ArrayList<>();
            Template template1 = new Template();
            template1.name = "configuration1.properties";
            template1.templateGroup = "configurations/configuration1.stg";
            template1.generated = new File(tempDirectory.directory,
                                           template1.name);
            templates.add(template1);

            Template template2 = new Template();
            template2.name = "configuration2.properties";
            template2.templateGroup = "configurations/configuration2.stg";
            template2.generated = new File(tempDirectory.directory,
                                           template2.name);
            templates.add(template2);

            List<String> additionalPorts = new ArrayList<>();

            Map<String, String> substitutions = new HashMap<>();
            substitutions.put("a", "A");
            substitutions.put("b", "B");

            final AtomicBoolean completed = new AtomicBoolean();
            final AtomicBoolean succeeded = new AtomicBoolean();
            final AtomicReference<List<File>> transformedConfigurations = new AtomicReference<List<File>>();
            ServiceReference serviceRef = mock(ServiceReference.class);
            ServiceURL serviceUrl = mock(ServiceURL.class);
            when(serviceRef.getUrl()).thenReturn(serviceUrl);
            when(serviceUrl.getHost()).thenReturn(serviceHost);
            when(serviceUrl.getPort()).thenReturn(servicePort);

            Map<String, String> serviceProps1 = new HashMap<>();
            serviceProps1.put(ServiceScope.SERVICE_REGISTRATION,
                              UUID.randomUUID().toString());
            ServiceReference serviceCollection1Ref = mock(ServiceReference.class);
            ServiceURL serviceCollection1Url = mock(ServiceURL.class);
            when(serviceCollection1Url.getHost()).thenReturn(serviceHost);
            when(serviceCollection1Url.getPort()).thenReturn(serviceCollection1Port);
            when(serviceCollection1Ref.getUrl()).thenReturn(serviceCollection1Url);
            when(serviceCollection1Ref.getProperties()).thenReturn(serviceProps1);

            Map<String, String> serviceProps2 = new HashMap<>();
            serviceProps2.put(ServiceScope.SERVICE_REGISTRATION,
                              UUID.randomUUID().toString());
            ServiceReference serviceCollection2Ref = mock(ServiceReference.class);
            ServiceURL serviceCollection2Url = mock(ServiceURL.class);
            when(serviceCollection2Url.getHost()).thenReturn(serviceHost);
            when(serviceCollection2Url.getPort()).thenReturn(serviceCollection2Port);
            when(serviceCollection2Ref.getUrl()).thenReturn(serviceCollection2Url);
            when(serviceCollection2Ref.getProperties()).thenReturn(serviceProps2);

            AutoConfigure autoConfigure = new AutoConfigure(
                                                            serviceFormat,
                                                            interfaceName,
                                                            0,
                                                            false,
                                                            serviceProperties,
                                                            discovery,
                                                            serviceDefinitions,
                                                            serviceCollectionDefinitions,
                                                            templates,
                                                            substitutions,
                                                            uniqueDirectories,
                                                            additionalPorts,
                                                            null,
                                                            null,
                                                            true,
                                                            jmxConfig,
                                                            new LocalScopeConfiguration());
            AutoConfigureService configuredService = new AutoConfigureService(
                                                                              autoConfigure) {

                @Override
                public void fail(Map<String, File> configurations)
                                                                  throws Exception {
                    succeeded.set(false);
                    completed.set(true);
                }

                @Override
                public void succeed(Map<String, File> configurations)
                                                                     throws Exception {
                    List<File> configs = new ArrayList<>();
                    File generated = configurations.get("configuration1.properties");
                    configs.add(generated);
                    generated = configurations.get("configuration2.properties");
                    configs.add(generated);
                    transformedConfigurations.set(configs);
                    succeeded.set(true);
                    completed.set(true);
                }
            };
            configuredService.start(100, TimeUnit.MILLISECONDS);
            autoConfigure.discover(serviceRef, service);
            autoConfigure.discover(serviceCollection1Ref, serviceCollection);
            autoConfigure.discover(serviceCollection2Ref, serviceCollection);
            assertTrue("configuration did not complete",
                       Utils.waitForCondition(1000, new Condition() {
                           @Override
                           public boolean isTrue() {
                               return completed.get();
                           }
                       }));
            assertTrue("configuration not successful", succeeded.get());
            assertNotNull(transformedConfigurations.get());
            assertEquals(2, transformedConfigurations.get().size());
            Properties properties1 = new Properties();
            File configuration1 = transformedConfigurations.get().get(0);
            assertNotNull("Configuration 1 was not generated", configuration1);
            assertTrue("configuration 1 does not exist",
                       configuration1.exists());
            FileInputStream is = new FileInputStream(configuration1);
            properties1.load(is);
            is.close();
            Properties properties2 = new Properties();
            File configuration2 = transformedConfigurations.get().get(1);
            assertNotNull("Configuration 2 was not generated", configuration2);
            assertTrue("configuration 2 does not exist",
                       configuration2.exists());
            is = new FileInputStream(configuration2);
            properties2.load(is);
            is.close();
            InetSocketAddress bound = autoConfigure.getBound();
            assertEquals(bound.getHostName(), properties1.get(hostVariable));
            assertEquals(String.valueOf(bound.getPort()),
                         properties1.get(portVariable));
            assertEquals("B", properties1.get("property.b"));
            assertEquals(serviceCollectionConfig,
                         properties2.get(serviceCollectionVariable));
            assertEquals(serviceConfig, properties2.get(serviceVariable));
            assertEquals("A", properties2.get("property.a"));
        }
    }

    @Test
    public void testNoServicesRequired() {
        String serviceFormat = "service:test:tcp://%s:%s";
        Map<String, String> serviceProperties = new HashMap<String, String>();
        List<SingletonService> serviceDefinitions = new ArrayList<>();
        List<ServiceCollection> serviceCollectionDefinitions = new ArrayList<>();
        List<Template> templates = new ArrayList<>();
        List<UniqueDirectory> uniqueDirectories = new ArrayList<>();
        Map<String, String> substitutions = new HashMap<>();
        List<String> additionalPorts = new ArrayList<>();
        AutoConfigure autoConfigure = new AutoConfigure(
                                                        serviceFormat,
                                                        interfaceName,
                                                        0,
                                                        false,
                                                        serviceProperties,
                                                        discovery,
                                                        serviceDefinitions,
                                                        serviceCollectionDefinitions,
                                                        templates,
                                                        substitutions,
                                                        uniqueDirectories,
                                                        additionalPorts, null,
                                                        null, true, jmxConfig,
                                                        null);
        final AtomicBoolean succeeded = new AtomicBoolean();
        final AtomicBoolean completed = new AtomicBoolean();
        AutoConfigureService configuredService = new AutoConfigureService(
                                                                          autoConfigure) {

            @Override
            public void fail(Map<String, File> configurations) throws Exception {
                succeeded.set(false);
                completed.set(true);
            }

            @Override
            public void succeed(Map<String, File> configurations)
                                                                 throws Exception {
                succeeded.set(true);
                completed.set(true);
            }
        };
        configuredService.start(10, TimeUnit.MILLISECONDS);
        assertTrue("configuration did not complete",
                   Utils.waitForCondition(1000, new Condition() {
                       @Override
                       public boolean isTrue() {
                           return completed.get();
                       }
                   }));
        assertTrue("configuration did not succeed", succeeded.get());
    }

    @Test
    public void testServiceRegistration() throws Exception {
        String serviceFormat = "service:test:tcp://%s:%s";
        Map<String, String> serviceProperties = new HashMap<String, String>();
        SingletonService service = new SingletonService();
        service.service = "service:testService:tcp";
        List<SingletonService> serviceDefinitions = new ArrayList<>();
        serviceDefinitions.add(service);
        ServiceCollection serviceCollection = new ServiceCollection();
        serviceCollection.service = "service:testServiceCollection:tcp";
        serviceCollection.cardinality = 1;
        List<ServiceCollection> serviceCollectionDefinitions = new ArrayList<>();
        serviceCollectionDefinitions.add(serviceCollection);
        List<Template> templates = new ArrayList<>();
        List<UniqueDirectory> uniqueDirectories = new ArrayList<>();
        Map<String, String> substitutions = new HashMap<>();
        List<String> additionalPorts = new ArrayList<>();

        final AtomicBoolean completed = new AtomicBoolean();
        final AtomicBoolean succeeded = new AtomicBoolean();

        ServiceReference serviceRef = mock(ServiceReference.class);
        ServiceURL serviceUrl = mock(ServiceURL.class);
        when(serviceRef.getUrl()).thenReturn(serviceUrl);
        when(serviceUrl.getHost()).thenReturn("example.com");
        when(serviceUrl.getPort()).thenReturn(1);

        ServiceReference serviceCollectionRef = mock(ServiceReference.class);
        ServiceURL serviceCollectionUrl = mock(ServiceURL.class);
        when(serviceCollectionUrl.getHost()).thenReturn("example.com");
        when(serviceCollectionUrl.getPort()).thenReturn(2);
        when(serviceCollectionRef.getUrl()).thenReturn(serviceCollectionUrl);

        AutoConfigure autoConfigure = new AutoConfigure(
                                                        serviceFormat,
                                                        interfaceName,
                                                        0,
                                                        false,
                                                        serviceProperties,
                                                        discovery,
                                                        serviceDefinitions,
                                                        serviceCollectionDefinitions,
                                                        templates,
                                                        substitutions,
                                                        uniqueDirectories,
                                                        additionalPorts, null,
                                                        null, true, jmxConfig,
                                                        null);
        AutoConfigureService configuredService = new AutoConfigureService(
                                                                          autoConfigure) {

            @Override
            public void fail(Map<String, File> configurations) throws Exception {
                succeeded.set(false);
                completed.set(true);
            }

            @Override
            public void succeed(Map<String, File> configurations)
                                                                 throws Exception {
                succeeded.set(true);
                completed.set(true);
            }
        };
        configuredService.start(100, TimeUnit.MILLISECONDS);
        autoConfigure.discover(serviceRef, service);
        autoConfigure.discover(serviceCollectionRef, serviceCollection);
        assertTrue("configuration did not complete",
                   Utils.waitForCondition(1000, new Condition() {
                       @Override
                       public boolean isTrue() {
                           return completed.get();
                       }
                   }));
        assertTrue("configuration not successful", succeeded.get());

        verify(discovery).addServiceListener(isA(ServiceListener.class),
                                             eq(AutoConfigure.constructFilter(service.service,
                                                                              service.properties)));
        verify(discovery).addServiceListener(isA(ServiceListener.class),
                                             eq(AutoConfigure.constructFilter(serviceCollection.service,
                                                                              serviceCollection.properties)));
    }
}
