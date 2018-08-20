/*
 * MusicBrainz Search Server
 * Copyright (C) 2009  Aurelien Mino

 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.musicbrainz.search.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.NumericUtils;
import org.musicbrainz.search.MbDocument;
import org.musicbrainz.search.analysis.MusicbrainzAnalyzer;

import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * An abstract Index specialized in indexing information from a Database
 */
public abstract class DatabaseIndex implements Index {

    /* This is appended to the getName() method of each index to create the index folder  */
    private static final String INDEX_SUFFIX = "_index";

    protected HashMap<String, PreparedStatement> preparedStatements;
    protected Connection dbConnection;
    
    public String getFilename() {
        return getName() + INDEX_SUFFIX;
    }

    protected DatabaseIndex(Connection dbConnection) {
        this.preparedStatements = new HashMap<String, PreparedStatement>();
        this.dbConnection = dbConnection;
    }

    protected DatabaseIndex() {

    }

    public static Analyzer getAnalyzer(Class indexFieldClass) {
        Map<String,Analyzer> fieldAnalyzers = new HashMap<String, Analyzer>();
        for(Object o : EnumSet.allOf(indexFieldClass)) {
            IndexField indexField = (IndexField) o;
            Analyzer analyzer = indexField.getAnalyzer();
            if (analyzer != null) {
                fieldAnalyzers.put(indexField.getName(), analyzer);
            }
        }
        PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(new MusicbrainzAnalyzer(), fieldAnalyzers);

        return wrapper;
    }

	
	public ReplicationInformation readReplicationInformationFromIndex(IndexReader reader) throws IOException {
		
		ReplicationInformation info = new ReplicationInformation();
		IndexSearcher searcher = new IndexSearcher(reader);
		
		Term term = new Term(MetaIndexField.META.getName(), MetaIndexField.META_VALUE);
		TermQuery query = new TermQuery(term);
		TopDocs hits = searcher.search(query, 10);
		
		if (hits.scoreDocs.length == 0) {
		    throw new IllegalArgumentException("No matches in the index for the given Term.");
		} else if (hits.scoreDocs.length > 1) {
		    throw new IllegalArgumentException("Given Term matches more than 1 document in the index.");
		} else {
		    int docId = hits.scoreDocs[0].doc;
		
		    MbDocument doc = new MbDocument(searcher.doc(docId));
		    info.replicationSequence = Integer.parseInt(doc.get(MetaIndexField.REPLICATION_SEQUENCE));
		    info.schemaSequence = Integer.parseInt(doc.get(MetaIndexField.SCHEMA_SEQUENCE));
		    String tmpStr = doc.get(MetaIndexField.LAST_CHANGE_SEQUENCE);
		    info.changeSequence = (tmpStr != null && !tmpStr.isEmpty()) ? Integer.parseInt(tmpStr) : null;
		    
		}
		return info; 
	}    
    
	public ReplicationInformation readReplicationInformationFromDatabase() throws IOException {
		
		ReplicationInformation info = new ReplicationInformation();
		Statement st;
		try {
			st = dbConnection.createStatement();
	        ResultSet rs = st.executeQuery(
	        		"SELECT current_schema_sequence, current_replication_sequence " +
	        		"FROM replication_control");
	        rs.next();
	        
	        info.schemaSequence = rs.getInt("current_schema_sequence");
	        info.replicationSequence = rs.getInt("current_replication_sequence");
	        
			// Check if dbmirror tables exist to get last change sequence
			DatabaseMetaData meta = dbConnection.getMetaData();
			rs = meta.getTables(null, null, "dbmirror_pending", new String[] {"TABLE"});
			if (rs.first()) {
				rs = st.executeQuery("SELECT MAX(seqid) FROM dbmirror_pending");
				rs.first();
				info.changeSequence = rs.getInt(0);
			} else {
				info.changeSequence = null;
			}
	        
		} catch (SQLException e) {
			System.err.println(getName()+": Unable to get replication information");
		}
		
		return info;
	}
	
	@Override
	public void addMetaInformation(IndexWriter indexWriter) throws IOException {

        ReplicationInformation info = readReplicationInformationFromDatabase();
		addMetaInformation(indexWriter, info);
	}
	
	public void addMetaInformation(IndexWriter indexWriter, ReplicationInformation info) throws IOException {
		
    	MbDocument doc = new MbDocument();
    	doc.addField(MetaIndexField.META, MetaIndexField.META_VALUE);
        doc.addNumericField(MetaIndexField.LAST_UPDATED, new Date().getTime());
        doc.addField(MetaIndexField.SCHEMA_SEQUENCE, info.schemaSequence);
        doc.addField(MetaIndexField.REPLICATION_SEQUENCE, info.replicationSequence);
        if (info.changeSequence != null) {
        	doc.addField(MetaIndexField.LAST_CHANGE_SEQUENCE, info.changeSequence);
        }
        indexWriter.addDocument(doc.getLuceneDocument());       

	}

	public void updateMetaInformation(IndexWriter indexWriter, ReplicationInformation info) throws IOException {
		
		// Remove the old one
		Term term = new Term(MetaIndexField.META.getName(), MetaIndexField.META_VALUE);
		TermQuery query = new TermQuery(term);
		indexWriter.deleteDocuments(query);
		
		// And the new one
		addMetaInformation(indexWriter, info);
	}
    
    public PreparedStatement addPreparedStatement(String identifier, String SQL) throws SQLException {
        PreparedStatement st = dbConnection.prepareStatement(SQL);
        preparedStatements.put(identifier, st);
        return st;
    }

    public PreparedStatement getPreparedStatement(String identifier) {
        return preparedStatements.get(identifier);
    }

    public Connection getDbConnection() {
        return dbConnection;
    }

    /**
     * Initialize the indexer, usually this includes creation of prepared statements
     * and any temporary tables or indexes that re reuired.
     *
     * @param indexWriter
     * @param isUpdater
     * @throws SQLException
     */
    public void init(IndexWriter indexWriter, boolean isUpdater) throws SQLException {
    }
    
    public void destroy() throws SQLException {
        for (PreparedStatement st : preparedStatements.values() ) {
            st.close();
        }
    }

    public abstract int getNoOfRows(int maxId) throws SQLException ;
    
    /**
     * Returns the max id
     *
     * @return
     */
    public abstract int getMaxId() throws SQLException;

    /**
     * Index data on a range ids defined by min and max
     * @param indexWriter
     * @param min
     * @param max
     * @return
     */
    public abstract void indexData(IndexWriter indexWriter, int min, int max) throws SQLException, IOException;

    public abstract IndexField getIdentifierField();

    public Similarity getSimilarity()
    {
        return null;
    }
    
}

