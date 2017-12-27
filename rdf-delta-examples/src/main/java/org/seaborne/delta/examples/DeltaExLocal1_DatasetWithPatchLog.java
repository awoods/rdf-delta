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

package org.seaborne.delta.examples;

import java.io.OutputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.system.Txn;
import org.seaborne.patch.RDFChanges;
import org.seaborne.patch.RDFPatchOps;

/** Set up a dataset and write out a log of changes as they happen. */
public class DeltaExLocal1_DatasetWithPatchLog {
    public static void main(String ...args) {
        // -- Base dataset 
        DatasetGraph dsgBase = DatasetGraphFactory.createTxnMem();

        // -- Destination for changes.
        // Text form of output.  
        OutputStream out = System.out;
        // Create an RDFChanges that writes to "out".
        RDFChanges changeLog = RDFPatchOps.textWriter(out);

        // Combined datasetgraph and changes. 
        DatasetGraph dsg = RDFPatchOps.changes(dsgBase, changeLog);

        // Wrap in the Dataset API
        Dataset ds = DatasetFactory.wrap(dsg);

        // --------
        // Do something. Read in data.ttl inside a transaction.
        // (data.ttl is in src/main/resources/)
        Txn.executeWrite(ds,
            ()->RDFDataMgr.read(dsg, "data.ttl")
            );
    }    
}
