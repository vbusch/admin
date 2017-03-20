/*
 * Copyright 2016 Red Hat Inc.
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

package enmasse.controller;

import enmasse.controller.address.AddressManagerFactory;
import enmasse.controller.address.AddressManagerFactoryImpl;
import enmasse.controller.common.OpenShift;
import enmasse.controller.common.OpenShiftHelper;
import enmasse.controller.flavor.FlavorController;
import enmasse.controller.flavor.FlavorManager;
import enmasse.controller.instance.InstanceManagerImpl;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

import java.io.IOException;

public class Controller extends AbstractVerticle {
    private final AMQPServer server;
    private final HTTPServer restServer;
    private final AddressManagerFactory addressManagerFactory;
    private final InstanceManagerImpl instanceManager;
    private final FlavorController flavorController;


    public Controller(ControllerOptions options) throws IOException {
        OpenShiftClient controllerClient = new DefaultOpenShiftClient(new ConfigBuilder()
                .withMasterUrl(options.openshiftUrl())
                .withOauthToken(options.openshiftToken())
                .withNamespace(options.openshiftNamespace())
                .build());

        OpenShift openShift = new OpenShiftHelper(InstanceId.withIdAndNamespace(options.openshiftNamespace(), options.openshiftNamespace()), controllerClient);
        String templateName = options.useTLS() ? "tls-enmasse-instance-infra" : "enmasse-instance-infra";

        FlavorManager flavorManager = new FlavorManager();
        this.instanceManager = new InstanceManagerImpl(openShift, templateName, options.isMultiinstance());
        if (!options.isMultiinstance() && !openShift.hasService("messaging")) {
            instanceManager.create(new Instance.Builder(openShift.getInstanceId()).build());
        }

        this.addressManagerFactory = new AddressManagerFactoryImpl(openShift, instanceManager, flavorManager);
        this.server = new AMQPServer(openShift.getInstanceId(), addressManagerFactory, flavorManager, options.port());
        this.restServer = new HTTPServer(openShift.getInstanceId(), addressManagerFactory, instanceManager, flavorManager);
        this.flavorController = new FlavorController(controllerClient, flavorManager);
    }

    @Override
    public void start() {
        vertx.deployVerticle(flavorController);
        vertx.deployVerticle(server);
        vertx.deployVerticle(restServer, new DeploymentOptions().setWorker(true));
    }

    public static void main(String args[]) {
        try {
            Vertx vertx = Vertx.vertx();
            vertx.deployVerticle(new Controller(ControllerOptions.fromEnv(System.getenv())));
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Error starting address controller: " + e.getMessage());
            System.exit(1);
        }
    }
}