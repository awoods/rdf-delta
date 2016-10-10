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

package org.seaborne.patch;

import java.io.InputStream ;
import java.io.OutputStream ;

import org.apache.jena.sparql.core.DatasetGraph ;
import org.apache.jena.system.JenaSystem ;
import org.seaborne.patch.changes.RDFChangesApply ;
import org.seaborne.patch.changes.RDFChangesWriter ;
import org.seaborne.patch.system.DatasetGraphChanges ;
import org.seaborne.patch.system.InitPatch ;
import org.seaborne.riot.tio.TokenWriter ;
import org.seaborne.riot.tio.TupleIO ;
import org.seaborne.riot.tio.TupleReader ;
import org.seaborne.riot.tio.impl.TokenWriterText ;

public class RDFPatchOps {
    public static String namespace = "http://jena.apache.org/rdf-patch/" ;
    
    /** Apply changes from a text format input stream to a {@link DatasetGraph} */ 
    public static void applyChange(DatasetGraph dsg, InputStream input) {
        TupleReader tr = TupleIO.createTupleReaderText(input) ;
        applyChange(dsg, tr);
    }

    /** Apply changes from a {@link TupleReader} stream to a {@link DatasetGraph} */ 
    public static void applyChange(DatasetGraph dsg, TupleReader tupleReader) {
        PatchReader pr = new PatchReader(tupleReader) ;
        RDFChanges changes = new RDFChangesApply(dsg) ;
        pr.apply(changes);
    }

    /** Create a {@link DatasetGraph} that sends changes to a {@link RDFChanges} stream */ 
    public static DatasetGraph changes(DatasetGraph dsgBase, RDFChanges changes) {
        return new DatasetGraphChanges(dsgBase, changes) ;
    }
    
    /** Create a {@link DatasetGraph} that writes changes to an{@link OutputStream} in text format.
     *  The caller is responsible for closing the {@link OutputStream}.
     */ 
    public static DatasetGraph changesAsText(DatasetGraph dsgBase, OutputStream out) {
        TokenWriter tokenWriter = new TokenWriterText(out) ;
        RDFChanges changeLog = new RDFChangesWriter(tokenWriter) ;
        return changes(dsgBase, changeLog) ;
    }
    
    /** This is automatically called by the Jena subsystem startup cycle.
     * See {@link InitPatch} and {@code META_INF/services/org.apache.jena.system.JenaSubsystemLifecycle}
     */
    public static void init( ) {}
    
    private static Object initLock = new Object() ;
    private static volatile boolean initialized = false ;
    
    private static void init$() {
        if ( initialized )
            return ;
        synchronized(initLock) {
            if ( initialized ) {
                JenaSystem.logLifecycle("Patch.init - return") ;
                return ;
            }
            initialized = true ;
            JenaSystem.logLifecycle("Patch.init - start") ;
            // -- Nothing here at the moment -- 
            JenaSystem.logLifecycle("Patch.init - finish") ;
        }
    }
}
