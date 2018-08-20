/* Copyright (c) 2009 Lukas Lalinsky
 * Copyright (c) 2009 Aurelien Mino
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the MusicBrainz project nor the names of the
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.musicbrainz.search.index;

import org.apache.commons.lang.time.StopWatch;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoLockFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.musicbrainz.search.LuceneVersion;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class IndexBuilder
{

    private static final int MAX_THREADS_FOR_CONCURRENT_OPTIMIZATION = 1;

    public static void main(String[] args) throws SQLException, IOException, InterruptedException
    {

        IndexOptions options = new IndexOptions();
        CmdLineParser parser = new CmdLineParser(options);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println("Couldn't parse command line parameters");
            parser.printUsage(System.out);
            System.exit(1);
        }


        if(options.isCheckFileLimit())
        {
            OpenFileLimitCheck.checkOpenFileLimit();
            System.exit(0);
        }
        // On request, print command line usage
        if (options.isHelp()) {
            parser.printUsage(System.out);
            System.exit(1);
        }
        
        if (options.isTest()) { System.out.println("Running in test mode."); }

        // At least one index should have been selected 
        ArrayList<String> selectedIndexes = options.selectedIndexes();
        if (selectedIndexes.size() == 0 
              || (selectedIndexes.size() == 1 && selectedIndexes.contains(""))) { 
            System.out.println("No indexes selected. Exiting.");
            System.exit(1);
        }

        Connection mainDbConn = null;

        System.out.println("Index Builder Started:"+ Utils.formatCurrentTimeForOutput());


        // Check that FreeDB is not the only index requested for build
        if (options.selectedIndexes().size() > 1 || !options.buildIndex("freedb")) {

            // Try loading PostgreSql driver
            try {
                Class.forName("org.postgresql.Driver");
            }
            catch (ClassNotFoundException e) {
                System.err.println("Couldn't load org.postgresql.Driver");
                System.exit(1);
            }

            // Connect to main database
            mainDbConn = options.getMainDatabaseConnection();
        }
    

        // MusicBrainz data indexing do the largest indexes first then can run optimizer whilst start building
        // the indexes on subsequent tables
        DatabaseIndex[] indexes = {
                new RecordingIndex(mainDbConn),
                new ReleaseIndex(mainDbConn),
                new WorkIndex(mainDbConn),
                new ArtistIndex(mainDbConn),
                new ReleaseGroupIndex(mainDbConn),
                new LabelIndex(mainDbConn),
                new AreaIndex(mainDbConn),
                new PlaceIndex(mainDbConn),
                new EventIndex(mainDbConn),
                new AnnotationIndex(mainDbConn),
                new UrlIndex(mainDbConn),
                new InstrumentIndex(mainDbConn),
                new SeriesIndex(mainDbConn),
                new EditorIndex(mainDbConn),
                new TagIndex(mainDbConn),
                new CDStubIndex(mainDbConn), //Note different db
        };

        List<String> indexesToBeBuilt = new ArrayList<String>();
        for (DatabaseIndex index : indexes) {

            // Check if this index should be built
            if (options.buildIndex(index.getName())) {
                indexesToBeBuilt.add(index.getName());
            }
        }

        // Extract current replication information, using one database index
        ReplicationInformation initialReplicationInformation=null;
        if(indexesToBeBuilt.size()>0) {
            initialReplicationInformation = indexes[0].readReplicationInformationFromDatabase();
        }
        // Create temporary tables used by multiple indexes
        CommonTables commonTables = new CommonTables(mainDbConn, indexesToBeBuilt);
        commonTables.createTemporaryTables(false);

        ExecutorService es = Executors.newFixedThreadPool(MAX_THREADS_FOR_CONCURRENT_OPTIMIZATION);
        CompletionService<Boolean> cs = new ExecutorCompletionService<Boolean>(es);
        for (DatabaseIndex index : indexes) {

            // Check if this index should be built
            if (!options.buildIndex(index.getName())) {
                System.out.println("Skipping index: " + index.getName());
                continue;
            }

            IndexWriter indexWriter = createIndexWriter(index,options);
            int maxId = buildDatabaseIndex(indexWriter, index, options, initialReplicationInformation);
            cs.submit(new IndexWriterOptimizerAndClose(maxId,indexWriter, index, options));
        }

        // FreeDB data indexing
        if(options.buildIndex("freedb")) {

            File dumpFile = new File(options.getFreeDBDump());
            //If they have set freedbdump file 
            if (options.getFreeDBDump() != null && options.getFreeDBDump().length()!=0)  {
                if( dumpFile.isFile()) {
                    buildFreeDBIndex(dumpFile, options);
                } else {
                    System.out.println("  Can't build FreeDB index: invalid file "+options.getFreeDBDump());
                }
            }
        }

        //Wait for each index to be optimized and closed before exiting from Index Build
        System.out.println("Waiting for any indexes to finish optimizing:"+ Utils.formatCurrentTimeForOutput());
        for (int i =0;i<indexesToBeBuilt.size();i++) {
            Future<Boolean> result = cs.take();
            try
            {
                if(!result.get())
                {
                    System.out.println("Optimize Failed");
                }
            }
            catch(ExecutionException ee)
            {
                System.out.println("Optimize Failed with unexpected exception");
            }
        }
        es.shutdown();
        if(mainDbConn!=null)
        {
            mainDbConn.close();
        }
        System.out.println("Index Builder Finished:"+ Utils.formatCurrentTimeForOutput());
    }


    /**
     * Initialize IndexWriter for populating index
     *
     * All addDocuments request are put on a queue to allow another query to be made to database without waiting
     * for all added documents to be analysed, queue is serviced by available processor no of threads.
     * If the max query outperforms the lucene analysis then analysis will switch to main thread because
     * the pool queue size cannot be larger than the max number of documents returned from one query.
     * Will get best results on multicpu systems accessing database on another system.
     *
     * @param index
     * @param options
     * @return
     * @throws IOException
     * @throws SQLException
     */
    private static IndexWriter createIndexWriter(DatabaseIndex index, IndexOptions options) throws IOException, SQLException
    {
        IndexWriter indexWriter;
        String path = options.getIndexesDir() + index.getFilename();

        FSDirectory fsDir = FSDirectory.open(new File(path), NoLockFactory.getNoLockFactory() );

        IndexWriterConfig config = new IndexWriterConfig(LuceneVersion.LUCENE_VERSION, index.getAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        config.setMaxBufferedDocs(options.getMaxBufferedDocs());

        if(index.getSimilarity()!=null)
        {
            config.setSimilarity(index.getSimilarity());
        }
        indexWriter = new ThreadedIndexWriter(
                                                fsDir,
                                                config,
                                                Runtime.getRuntime().availableProcessors(),
                                                options.getDatabaseChunkSize()
                                                );

        return indexWriter;
    }



    /**
     * Build an index from database
     * 
     * @param options
     * @param initialReplicationInformation 
     * @throws IOException 
     * @throws SQLException 
     */
    private static int buildDatabaseIndex(IndexWriter indexWriter, DatabaseIndex index, IndexOptions options, ReplicationInformation initialReplicationInformation) throws IOException, SQLException
    {
        try
        {
            StopWatch clock = new StopWatch();
            clock.start();
            System.out.println(index.getName()+":Started at "+ Utils.formatCurrentTimeForOutput());
            index.init(indexWriter, false);
            index.addMetaInformation(indexWriter, initialReplicationInformation);
            int maxId = index.getMaxId();
            if(maxId > 0) {

                if (options.isTest() && options.getTestIndexSize() < maxId)
                    maxId = options.getTestIndexSize();
                int j = 0;
                while (j <= maxId) {
                    int k = Math.min(j + options.getDatabaseChunkSize() - 1, maxId);
                    System.out.print(index.getName()+":Indexing " + j + "..." + k + " / " + maxId + " (" + (100*k/maxId) + "%)\r");
                    index.indexData(indexWriter, j, k);
                    j += options.getDatabaseChunkSize();
                }
            }
            index.destroy();
            clock.stop();
            System.out.println("\n"+index.getName()+":Finished:" + Utils.formatClock(clock));

            return maxId;
        }
        finally
        {

        }
    }

    /**
     * Build a FreeDB index from a FreeDB dump
     * 
     * @param dumpFile FreeDB dump file
     * @param options
     * @throws IOException 
     */
    private static void buildFreeDBIndex(File dumpFile, IndexOptions options) throws IOException
    {
        FreeDBIndex index = new FreeDBIndex();
        index.setDumpFile(dumpFile);

        StopWatch clock = new StopWatch();
        clock.start();
        System.out.println(index.getName()+":Started at "+ Utils.formatCurrentTimeForOutput());

        IndexWriterConfig config = new IndexWriterConfig(LuceneVersion.LUCENE_VERSION, index.getAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        config.setMaxBufferedDocs(options.getMaxBufferedDocs());

        IndexWriter indexWriter;
        String path = options.getIndexesDir() + index.getFilename();
        System.out.println("Building index: " + path);
        indexWriter = new IndexWriter(FSDirectory.open(new File(path)), config);
        //indexWriter.setMergeFactor(options.getMergeFactor());

        index.addMetaInformation(indexWriter);
        index.indexData(indexWriter);
        indexWriter.close();
        System.out.println(index.getName()+":Finished:" + Utils.formatClock(clock));

    }

    /*
     * Optimize the index in and close writer once index has been optimized
     *
     *
     * We run this as a future task so we can be optimizing the last index whilst the next index is being built.
     *
     */
   static class IndexWriterOptimizerAndClose implements Callable<Boolean>
    {
        private int             maxId;
        private IndexWriter     indexWriter;
        private DatabaseIndex   index;
        private IndexOptions    options;

        /**
         *
         * @param maxId
         * @param indexWriter
         * @param index
         * @param options
         */
        public IndexWriterOptimizerAndClose(int maxId, IndexWriter indexWriter, DatabaseIndex index, IndexOptions options)
        {
            this.maxId=maxId;
            this.indexWriter= indexWriter;
            this.index=index;
            this.options=options;
        }

        public Boolean call()
        {
            IndexReader reader=null;
            try
            {
                StopWatch clock = new StopWatch();
                clock.start();
                String path = options.getIndexesDir() + index.getFilename();
                System.out.println(index.getName()+":Started forceMerge at "+Utils.formatCurrentTimeForOutput());
                indexWriter.forceMerge(1);
                indexWriter.close();
                clock.stop();
                // For debugging to check sql is not creating too few/many rows
                if(true) {
                    int dbRows = index.getNoOfRows(maxId);
                    reader = DirectoryReader.open(FSDirectory.open(new File(path)));
                    System.out.println(index.getName()+":"+dbRows+" db rows:"+(reader.maxDoc() - 1)+" lucene docs");
                }
                System.out.println(index.getName()+":Finished forceMerge:" + Utils.formatClock(clock));
                return true;
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
                try
                {
                    indexWriter.close();
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                }
                return false;
            }
            catch(SQLException sqle)
            {
                sqle.printStackTrace();
                try
                {
                    indexWriter.close();
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                }
                return false;
            }
        }
    }
}
