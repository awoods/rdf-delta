/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package org.seaborne.delta.server.local;

import java.util.ArrayList;
import java.util.List;

import org.seaborne.delta.Delta;
import org.seaborne.delta.server.local.filestore.FileStore;
import org.seaborne.delta.server.local.patchstores.file.PatchStoreProviderFile;
import org.seaborne.delta.server.local.patchstores.mem.PatchStoreProviderMem;
import org.seaborne.delta.server.local.patchstores.zk.PatchStoreProviderZk;
import org.seaborne.delta.server.system.DeltaSystem;
import org.slf4j.Logger;

public class DPS {

    public static Logger LOG = Delta.DELTA_LOG;
    public static Logger HTTP_LOG = Delta.DELTA_HTTP_LOG;

    private static volatile boolean initialized = false;

    public static String PatchStoreFileProvider = "PatchStore/File";
    public static String PatchStoreMemProvider  = "PatchStore/Mem";
    public static String PatchStoreZkProvider  = "PatchStore/Zk";

    // Short names.
    public static String pspFile = "file";
    public static String pspMem  = "mem";
    public static String pspZk   = "zk";


    public static void init() {
        if ( initialized )
            return;
        synchronized(DPS.class) {
            if ( initialized )
                return;
            initialized = true;
            initPatchStoreProviders();
        }
    }

    /**
     * For testing. This code knows where all the global state is
     * and reset the system to the default after init() called.
     * There default PatchStoreProvider is retained.
     */
    public static void resetSystem() {
        DeltaSystem.init();
        DPS.init();
        LocalServer.releaseAll();
        FileStore.resetTracked();
        // PatchStoreMgr.reset clears the patch store providers.
        PatchStoreMgr.reset();
        initPatchStoreProviders();
    }

    // Things to do once.
    private static void initPatchStoreProviders() {
        // Find PatchStoreProviders.
        List<PatchStoreProvider> providers = new ArrayList<>();

        // Hard code the discovery for now.
        providers.add(new PatchStoreProviderFile());
        providers.add(new PatchStoreProviderMem());
        providers.add(new PatchStoreProviderZk());

        providers.forEach(psp->{
            LOG.debug("Provider: "+psp.getProviderName());
            PatchStoreMgr.register(psp);
        });
        // Still need to set the server-wide default PatchStore.
//        PatchStoreMgr.setDftPatchStoreName(DPS.PatchStoreZkProvider);
//        PatchStoreMgr.setDftPatchStoreName(DPS.PatchStoreFileProvider);
    }
}
