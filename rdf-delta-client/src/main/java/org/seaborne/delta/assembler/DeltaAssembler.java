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

package org.seaborne.delta.assembler;

import static org.apache.jena.sparql.util.graph.GraphUtils.exactlyOneProperty ;
import static org.apache.jena.sparql.util.graph.GraphUtils.getResourceValue ;
import static org.apache.jena.sparql.util.graph.GraphUtils.getStringValue ;
import static org.apache.jena.sparql.util.graph.GraphUtils.multiValueString ;
import static org.seaborne.delta.assembler.VocabDelta.pDataset ;
import static org.seaborne.delta.assembler.VocabDelta.pDeltaChanges ;
import static org.seaborne.delta.assembler.VocabDelta.pDeltaInit1 ;
import static org.seaborne.delta.assembler.VocabDelta.pDeltaInit2 ;
import static org.seaborne.delta.assembler.VocabDelta.pPollForChanges ;

import java.io.InputStream ;
import java.util.HashSet ;
import java.util.List ;
import java.util.Set ;
import java.util.concurrent.Executor ;
import java.util.concurrent.Executors ;

import org.apache.jena.assembler.Assembler ;
import org.apache.jena.assembler.Mode ;
import org.apache.jena.assembler.assemblers.AssemblerBase ;
import org.apache.jena.assembler.exceptions.AssemblerException ;
import org.apache.jena.atlas.io.IO ;
import org.apache.jena.atlas.lib.Lib ;
import org.apache.jena.atlas.logging.FmtLog ;
import org.apache.jena.query.Dataset ;
import org.apache.jena.query.DatasetFactory ;
import org.apache.jena.rdf.model.RDFNode ;
import org.apache.jena.rdf.model.Resource ;
import org.apache.jena.rdf.model.Statement ;
import org.apache.jena.sparql.core.DatasetGraph ;
import org.apache.jena.sparql.util.FmtUtils ;
import org.apache.jena.sparql.util.NodeUtils ;
import org.apache.jena.util.iterator.ExtendedIterator ;
import org.seaborne.delta.DP ;
import org.seaborne.delta.Delta ;
import org.seaborne.delta.DeltaOps ;
import org.seaborne.delta.base.DatasetGraphChanges ;
import org.seaborne.delta.client.DeltaClient ;
import org.seaborne.delta.client.DeltaConnection ;
import org.seaborne.patch.RDFChanges ;
import org.seaborne.patch.RDFChangesN ;

public class DeltaAssembler extends AssemblerBase implements Assembler {
    //static private Logger log = LoggerFactory.getLogger(DeltaAssembler.class) ;
    
    /* 
     */
    
    @Override
    public Object open(Assembler a, Resource root, Mode mode) {
        List<String> xList = multiValueString(root, pDeltaChanges) ;
        if ( xList.size() == 0 )
            throw new AssemblerException(root, "No destination for changes given") ;
        // check for duplicates.
        Set<String> xs = new HashSet<>(xList) ;
        if ( xList.size() != xs.size() )
            FmtLog.warn(Delta.DELTA_LOG, "Duplicate destinations for changes") ;  
        
        RDFChanges streamChanges = null ;
        for ( String dest : xs ) {
            FmtLog.info(Delta.DELTA_LOG, "Destination: '%s'", dest) ;
            RDFChanges sc = DeltaConnection.destination(dest+DP.EP_Patch) ;
            streamChanges = RDFChangesN.multi(streamChanges, sc) ;
        }
        
        if ( ! root.hasProperty(pDataset) )
            throw new AssemblerException(root, "No dataset to be wrapped for delta processing") ;
        if ( ! exactlyOneProperty(root, pDataset) )
            throw new AssemblerException(root, "Multiple datasets referenced for one delta processing wrapper") ;
        
        Resource dsr = getResourceValue(root, pDataset) ;
        // Check exists.
        
        DatasetGraph dsgSub = ((Dataset)Assembler.general.open(dsr)).asDatasetGraph() ;
        
        ExtendedIterator<Statement> sIter = root.listProperties(pDeltaInit1).andThen(root.listProperties(pDeltaInit2)) ;
        while(sIter.hasNext()) {
            RDFNode rn = sIter.next().getObject() ;
            if ( ! NodeUtils.isSimpleString(rn.asNode()) )
                throw new AssemblerException(root, "Not a string literal for initialization changes: "+FmtUtils.stringForNode(rn.asNode())) ;
            String str = rn.asLiteral().getString() ;
            FmtLog.info(Delta.DELTA_LOG, "Delta: initialize: %s",str) ;
            InputStream in = openChangesSrc(str) ;
            DeltaOps.play(dsgSub, in); 
        }
        
        // And someday tap into services to add a "sync before operation" step.
        if ( root.hasProperty(pPollForChanges) ) {
            if ( ! exactlyOneProperty(root, pPollForChanges) )
                throw new AssemblerException(root, "Multiple places to poll for chnages") ;
            String source = getStringValue(root, pPollForChanges) ;
            forkUpdateFetcher(source, dsgSub) ;
        }
        
        DatasetGraph dsg = new DatasetGraphChanges(dsgSub, streamChanges) ;
        Dataset ds = DatasetFactory.wrap(dsg) ;
        return ds ;
    }

    private InputStream openChangesSrc(String x) {
        // May change to cope with remote source
        return IO.openFile(x) ;
    }

    private static void forkUpdateFetcher(String source, DatasetGraph dsg) {
        DeltaClient client = DeltaClient.create("foo",  source, dsg) ;
        Runnable r = ()->{
            while(true) {
                try { client.sync(); }
                catch (Exception ex) { 
                    Delta.DELTA_LOG.warn("Failed to sync with the change server: "+ex.getMessage()) ;
                    // Additonal delay
                    Lib.sleep(3*1000);
                }
                Lib.sleep(2*1000);
            }
        } ;
        Executor executor = Executors.newSingleThreadExecutor() ;
        executor.execute(r); 
    }
}