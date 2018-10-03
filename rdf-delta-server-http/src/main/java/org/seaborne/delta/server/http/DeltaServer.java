/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seaborne.delta.server.http;

import java.net.BindException;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.atlas.logging.FmtLog;
import org.seaborne.delta.Delta;
import org.seaborne.delta.link.DeltaLink;
import org.seaborne.delta.server.local.DeltaLinkLocal;
import org.seaborne.delta.server.local.LocalServer;
import org.seaborne.delta.server.local.LocalServerConfig;
import org.seaborne.delta.server.local.LocalServers;

/**
 * Delta server.
 * This is the {@link PatchLogServer} and all additional servers like ZooKeeper.
 */

public class DeltaServer {
    private final PatchLogServer patchLogServer ;

    public static DeltaServer create(LocalServerConfig localServerConfig) {
        // XXX Create link.
        int port = -1;
        DeltaLink dLink = null;

        if ( true ) throw new NotImplemented();
        return create(port, dLink);
    }

    /** Create a {@code PatchLogServer} for a file-provider using the {@code base} area. */
    public static DeltaServer server(int port, String base) {
        LocalServer server = LocalServers.createFile(base);
        DeltaLink link = DeltaLinkLocal.connect(server);
        return DeltaServer.create(port, link);
    }

    /**
     * Create a patch log server that uses the given local {@link DeltaLink} for its
     * state.
     */
    public static DeltaServer create(int port, DeltaLink engine) {
        PatchLogServer pls = new PatchLogServer(null, port, engine);
        return new DeltaServer(pls);
    }

    /**
     * Create a patch log server that uses the given a Jetty configuation file and a
     * {@link DeltaLink} for its state.
     */
    public static DeltaServer create(String jettyConfig, DeltaLink engine) {
        PatchLogServer pls = new PatchLogServer(jettyConfig, -1, engine);
        return new DeltaServer(pls);
    }

    public static Builder create() { return new Builder(); }

    private DeltaServer(PatchLogServer patchLogServer) {
        this.patchLogServer = patchLogServer;
    }

    public int getPort() {
        return patchLogServer.getPort();
    }

    public DeltaServer start() throws BindException {
        FmtLog.info(Delta.DELTA_LOG, "Server start: port=%d", getPort());
        patchLogServer.start();
        return this;
    }

    public void stop() {
        patchLogServer.stop();
    }

    public void join() {
        patchLogServer.join();
    }

    // XXX Improve so that a DeltaServerConfig is build internally.
    public static class Builder {
        private Integer port = null;
        private String base;
        private LocalServer localServer;
        private DeltaServerConfig config;

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder config(DeltaServerConfig config) {
            this.config = config;
            return this;
        }

        public DeltaServer build() {
            if ( port != null && port < 0 )
                throw new IllegalArgumentException("Port number is negative: "+port);
            if ( config.serverPort == null || config.serverPort < 0 )
                throw new IllegalArgumentException("Bad port number: "+config.serverPort);

            PatchLogServer patchLogServer = null;
            DeltaServer deltaServer = new DeltaServer(patchLogServer);
            return deltaServer;
        }
    }
}
