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

package org.seaborne.delta.server.local;

import java.util.Objects;

import org.seaborne.delta.DataSourceDescription;
import org.seaborne.delta.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * An item under the control of the server.
 * <p>
 * These are managed through the {@link DataRegistry}.
 */
public class DataSource {
    // Might eb able to replace with 2PatchLog".
    private static Logger LOG = LoggerFactory.getLogger(DataSource.class);
    private final DataSourceDescription dsDescription;
    private final PatchLog patchLog;

    /**
     * Attach to a {@link DataSource} file area and return a {@link DataSource} object.
     * The directory {@code dsPath} must exist.
     * The {@code DataSource} area is not formatted by the provider. 
     */
    public static DataSource connect(DataSourceDescription dsd, PatchStore patchStore) {
        Objects.requireNonNull(dsd, "Null DataSourceDescription");
        Objects.requireNonNull(patchStore, "No patch store");
        PatchLog patchLog = patchStore.connectLog(dsd);
        DataSource dataSource = new DataSource(dsd, patchLog);
        return dataSource;
    }

    public static DataSource create(DataSourceDescription dsd, PatchStore patchStore) {
        Objects.requireNonNull(dsd, "Null DataSourceDescription");
        Objects.requireNonNull(patchStore, "No patch store");
        PatchLog patchLog = patchStore.createLog(dsd);
        DataSource dataSource = new DataSource(dsd, patchLog);
        return dataSource;
    }

    private DataSource(DataSourceDescription dsd, PatchLog patchLog) {
        super();
        this.dsDescription = dsd;
        this.patchLog = patchLog;
    }

    public Id getId() {
        return dsDescription.getId();
    }

    public String getURI() {
        return dsDescription.getUri();
    }

    public String getName() {
        return dsDescription.getName();
    }

    public PatchLog getPatchLog() {
        return patchLog;
    }

    public PatchStore getPatchStore() {
        return patchLog.getPatchStore();
    }

    public DataSourceDescription getDescription() {
        return dsDescription;
    }
    
    public void release() {
        getPatchLog().release();
    }

    @Override
    public String toString() {
        return String.format("[DataSource:%s %s (%s)]", 
                             dsDescription.getName(), dsDescription.getId(),
                             patchLog.getPatchStore().getProvider().getShortName());
    }
}
