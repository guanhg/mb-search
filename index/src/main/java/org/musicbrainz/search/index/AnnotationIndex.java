package org.musicbrainz.search.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.musicbrainz.search.MbDocument;
import org.musicbrainz.search.type.AnnotationType;

import java.io.IOException;
import java.sql.*;
import java.util.EnumMap;

public class AnnotationIndex extends DatabaseIndex {

    public static final String INDEX_NAME = "annotation";

    // Private class for storing specific information about each annotation type
    private class AnnotationTypeInfo {
        public String entityDbName;

        public AnnotationTypeInfo(String entityDbName) {
            this.entityDbName = entityDbName;
        }
    }
    protected EnumMap<AnnotationType, AnnotationTypeInfo> annotationTypeInfos;
    
    public AnnotationIndex(Connection dbConnection) {
        super(dbConnection);
    }

    public AnnotationIndex(){
    }

    public String getName() {
        return AnnotationIndex.INDEX_NAME;
    }

    public Analyzer getAnalyzer()
    {
        return super.getAnalyzer(AnnotationIndexField.class);
    }

	@Override
	public IndexField getIdentifierField() {
		return AnnotationIndexField.ID;
	}
    
    public int getMaxId() throws SQLException {
        Statement st = this.dbConnection.createStatement();
        ResultSet rs = st.executeQuery("SELECT MAX(id) FROM annotation");
        rs.next();
        return rs.getInt(1);
    }

    @Override
    public void init(IndexWriter indexWriter, boolean isUpdater) throws SQLException {

        // Register all information for each annotation type
        annotationTypeInfos = new EnumMap<AnnotationType, AnnotationTypeInfo>(AnnotationType.class);
        annotationTypeInfos.put(AnnotationType.ARTIST, new AnnotationTypeInfo("artist"));
        annotationTypeInfos.put(AnnotationType.LABEL, new AnnotationTypeInfo("label"));
        annotationTypeInfos.put(AnnotationType.RECORDING, new AnnotationTypeInfo("recording"));
        annotationTypeInfos.put(AnnotationType.RELEASE, new AnnotationTypeInfo("release"));
        annotationTypeInfos.put(AnnotationType.RELEASE_GROUP, new AnnotationTypeInfo("release_group"));
        annotationTypeInfos.put(AnnotationType.WORK, new AnnotationTypeInfo("work"));
        annotationTypeInfos.put(AnnotationType.INSTRUMENT, new AnnotationTypeInfo("instrument"));
        annotationTypeInfos.put(AnnotationType.SERIES, new AnnotationTypeInfo("series"));
        annotationTypeInfos.put(AnnotationType.AREA, new AnnotationTypeInfo("area"));
        annotationTypeInfos.put(AnnotationType.PLACE, new AnnotationTypeInfo("place"));
        annotationTypeInfos.put(AnnotationType.EVENT, new AnnotationTypeInfo("event"));
        
        // Build prepared statement for each annotation type
        for (AnnotationType type : annotationTypeInfos.keySet()) {
            AnnotationTypeInfo info = annotationTypeInfos.get(type);
            addPreparedStatement(type.getName(), 
                "SELECT a.id, e.gid, a.text, e.name, e.comment " +
                " FROM annotation a " +
                "  INNER JOIN " + info.entityDbName + "_annotation ea ON a.id=ea.annotation " +
                "  INNER JOIN (SELECT DISTINCT ea2." + info.entityDbName + " as id, max(created) as created_date " +
                "               FROM annotation a2 " +
                "                INNER JOIN " + info.entityDbName + "_annotation ea2 on a2.id=ea2.annotation GROUP BY ea2." + info.entityDbName +
                "              ) AS last_ann ON ea." + info.entityDbName + "=last_ann.id AND a.created=last_ann.created_date "  +
                "  INNER JOIN " + info.entityDbName + " e ON ea." + info.entityDbName + "=e.id " +
                " WHERE a.id BETWEEN ? and ? AND length(a.text) > 0"
            );
        }

    }

    public int getNoOfRows(int maxId) throws SQLException {
        Statement st = dbConnection.createStatement();
        ResultSet rs = st.executeQuery("SELECT count(*) FROM annotation WHERE id<="+maxId);
        rs.next();
        return rs.getInt(1);
    }

    public void indexData(IndexWriter indexWriter, int min, int max)
	throws SQLException, IOException {
        
        for (AnnotationType type : annotationTypeInfos.keySet()) {
            indexAnnotation(indexWriter, type, min, max);
        }
    }

    protected void indexAnnotation(IndexWriter indexWriter, AnnotationType type, int min, int max)
    throws SQLException, IOException {
        PreparedStatement st = getPreparedStatement(type.getName());
        st.setInt(1, min);
        st.setInt(2, max);
        ResultSet rs = st.executeQuery();
        while (rs.next()) {
            indexWriter.addDocument(documentFromResultSet(rs, type));
        }
        rs.close();
    }
    
    public Document documentFromResultSet(ResultSet rs, AnnotationType type) throws SQLException {
        MbDocument doc = new MbDocument();
        doc.addField(AnnotationIndexField.ID, rs.getString("id"));
        doc.addField(AnnotationIndexField.ENTITY, rs.getString("gid"));
        doc.addField(AnnotationIndexField.NAME, rs.getString("name"));
        doc.addField(AnnotationIndexField.TYPE, type.getName());
        doc.addNonEmptyField(AnnotationIndexField.TEXT, rs.getString("text"));
        return doc.getLuceneDocument();
    }
        
}
