package org.musicbrainz.search.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;
import org.musicbrainz.search.type.AnnotationType;

import java.sql.Statement;

import static org.junit.Assert.*;

public class AnnotationIndexTest extends AbstractIndexTest {



    private void createIndex(RAMDirectory ramDir) throws Exception {
        IndexWriter writer = createIndexWriter(ramDir,AnnotationIndexField.class);
        AnnotationIndex ai = new AnnotationIndex(conn);
        CommonTables ct = new CommonTables(conn, ai.getName());
        ct.createTemporaryTables(false);
        ai.init(writer, false);
        ai.addMetaInformation(writer);
        ai.indexData(writer, 0, Integer.MAX_VALUE);
        ai.destroy();
        writer.close();
    }


    /**
     * @throws Exception exception
     */
    private void addReleaseAnnotation() throws Exception {

        Statement stmt = conn.createStatement();

        stmt.addBatch("INSERT INTO release (id, gid, name, artist_credit, release_group) " +
                "  VALUES (491240, 'c3b8dbc9-c1ff-4743-9015-8d762819134e', 'Crocodiles (bonus disc)', 1, 491240)");
        
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
        	"  VALUES (1, 1, 'release annotation', 'change', now())");
        stmt.addBatch("INSERT INTO release_annotation (release, annotation) VALUES (491240, 1)");

        stmt.executeBatch();
        stmt.close();
    }

     /**
     * @throws Exception exception
     */
    private void addReleaseGroupAnnotation() throws Exception {

        Statement stmt = conn.createStatement();

        stmt.addBatch("INSERT INTO release_group (id, gid, name, artist_credit)" +
                    "  VALUES (491240, 'efd2ace2-b3b9-305f-8a53-9803595c0e37', 'Crocodiles', 1)");
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
                    "  VALUES (1, 1, 'release group annotation', 'change', now())");
        stmt.addBatch("INSERT INTO release_group_annotation (release_group, annotation) VALUES (491240, 1)");

        stmt.executeBatch();
        stmt.close();
    }


     /**
     * @throws Exception exception
     */
    private void addArtistAnnotation() throws Exception {

        Statement stmt = conn.createStatement();

        stmt.addBatch("INSERT INTO artist (id, name, gid, sort_name)" +
                    "  VALUES (521316, 'Farming Incident', '4302e264-1cf0-4d1f-aca7-2a6f89e34b36', 'Farming Incident')");

        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
                    "  VALUES (1, 1, 'artist annotation', 'change', now())");
        stmt.addBatch("INSERT INTO artist_annotation (artist, annotation) VALUES (521316, 1)");
        
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
                    "  VALUES (2, 1, 'artist annotation newer', 'change', now()+1)");
        stmt.addBatch("INSERT INTO artist_annotation (artist, annotation) VALUES (521316, 2)");

        stmt.executeBatch();
        stmt.close();
    }

    /**
     * @throws Exception exception
     */
    private void addArtistEmptyAnnotation() throws Exception {

        Statement stmt = conn.createStatement();

        stmt.addBatch("INSERT INTO artist (id, name, gid, sort_name)" +
                "  VALUES (521316, 'Farming Incident', '4302e264-1cf0-4d1f-aca7-2a6f89e34b36', 'Farming Incident')");
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
                "  VALUES (1, 1, '', 'change', now())");
        stmt.addBatch("INSERT INTO artist_annotation (artist, annotation) VALUES (521316, 1)");

        stmt.executeBatch();
        stmt.close();
    }

    /**
     * @throws Exception exception
     */
    private void addArtistEmptyAnnotation2() throws Exception {

        Statement stmt = conn.createStatement();

        stmt.addBatch("INSERT INTO artist (id, name, gid, sort_name)" +
                "  VALUES (521316, 'Farming Incident', '4302e264-1cf0-4d1f-aca7-2a6f89e34b36', 'Farming Incident')");

        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
                "  VALUES (1, 1, null, 'change', now())");
        stmt.addBatch("INSERT INTO artist_annotation (artist, annotation) VALUES (521316, 1)");

        stmt.executeBatch();
        stmt.close();
    }
      /**
     * @throws Exception  exception
     */
    private void addLabelAnnotation() throws Exception {

        Statement stmt = conn.createStatement();

        stmt.addBatch("INSERT INTO label (id, gid, name) " +
                "  VALUES (1, 'a539bb1e-f2e1-4b45-9db8-8053841e7503', '4AD')");
        
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
        	"  VALUES (1, 1, 'label annotation', 'change', now())");
        stmt.addBatch("INSERT INTO label_annotation (label, annotation) VALUES (1, 1)");
        
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
        	"  VALUES (2, 1, 'label annotation newer', 'change', now()+1)");
        stmt.addBatch("INSERT INTO label_annotation (label, annotation) VALUES (1, 2)");

        stmt.executeBatch();
        stmt.close();
    }

      /**
     * @throws Exception exception
     */
    private void addRecordingAnnotation() throws Exception {

        Statement stmt = conn.createStatement();

        stmt.addBatch("INSERT INTO recording (id, gid, name, artist_credit)"
                       + "VALUES (1, '2f250ed2-6285-40f1-aa2a-14f1c05e9765', 'Do It Clean', 1)");
        
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
        	"  VALUES (1, 1, 'recording annotation', 'change', now())");
        stmt.addBatch("INSERT INTO recording_annotation (recording, annotation) VALUES (1, 1)");
        
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
        	"  VALUES (2, 1, 'recording annotation newer', 'change', now()+1)");
        stmt.addBatch("INSERT INTO recording_annotation (recording, annotation) VALUES (1, 2)");

        stmt.executeBatch();
        stmt.close();
    }

    /**
     * @throws Exception exception
     */
    private void addSeriesAnnotation() throws Exception {

        Statement stmt = conn.createStatement();

        stmt.addBatch("INSERT INTO series (id, gid, name)" +
                "  VALUES (2, 'efd2ace2-b3b9-305f-8a53-9803595c0e37', 'Crocodiles')");
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
                "  VALUES (1, 1, 'series annotation', 'change', now())");
        stmt.addBatch("INSERT INTO series_annotation (series, annotation) VALUES (2, 1)");
        stmt.executeBatch();
        stmt.close();
    }

    /**
     * Basic test for Release Group Annotation
     *
     * @throws Exception exception
     */
    @Test
    public void testSeriesIndexAnnotationFields() throws Exception {
        addSeriesAnnotation();
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);

        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(2, ir.numDocs());
        {
            Document doc = ir.document(1);
            assertEquals(1, doc.getFields(AnnotationIndexField.ENTITY.getName()).length);
            assertEquals("efd2ace2-b3b9-305f-8a53-9803595c0e37", doc.getField(AnnotationIndexField.ENTITY.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.NAME.getName()).length);
            assertEquals("Crocodiles", doc.getField(AnnotationIndexField.NAME.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TEXT.getName()).length);
            assertEquals("series annotation", doc.getField(AnnotationIndexField.TEXT.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TYPE.getName()).length);
            assertEquals("series", doc.getField(AnnotationIndexField.TYPE.getName()).stringValue());
        }
        ir.close();
    }

    /**
     * Basic test for Release Annotation
     *
     * @throws Exception exception
     */
    @Test
    public void testReleaseIndexAnnotationFields() throws Exception {
        addReleaseAnnotation();
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);
        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(2, ir.numDocs());
        {
            Document doc = ir.document(1);
            assertEquals(1, doc.getFields(AnnotationIndexField.ENTITY.getName()).length);
            assertEquals("c3b8dbc9-c1ff-4743-9015-8d762819134e", doc.getField(AnnotationIndexField.ENTITY.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.NAME.getName()).length);
            assertEquals("Crocodiles (bonus disc)", doc.getField(AnnotationIndexField.NAME.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TEXT.getName()).length);
            assertEquals("release annotation", doc.getField(AnnotationIndexField.TEXT.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TYPE.getName()).length);
            assertEquals("release", doc.getField(AnnotationIndexField.TYPE.getName()).stringValue());
        }
        ir.close();
    }


     /**
     * Basic test for Release Group Annotation
     *
     * @throws Exception exception
     */
     @Test
    public void testReleaseGroupIndexAnnotationFields() throws Exception {
        addReleaseGroupAnnotation();
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);

        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(2, ir.numDocs());
        {
            Document doc = ir.document(1);
            assertEquals(1, doc.getFields(AnnotationIndexField.ENTITY.getName()).length);
            assertEquals("efd2ace2-b3b9-305f-8a53-9803595c0e37", doc.getField(AnnotationIndexField.ENTITY.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.NAME.getName()).length);
            assertEquals("Crocodiles", doc.getField(AnnotationIndexField.NAME.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TEXT.getName()).length);
            assertEquals("release group annotation", doc.getField(AnnotationIndexField.TEXT.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TYPE.getName()).length);
            assertEquals("release-group", doc.getField(AnnotationIndexField.TYPE.getName()).stringValue());
        }
        ir.close();
    }

       /**
     * Basic test for Artist Annotation
     *
     * @throws Exception exception
     */
       @Test
    public void testArtistIndexAnnotationFields() throws Exception {
        addArtistAnnotation();
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);

        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(2, ir.numDocs());
        {
            Document doc = ir.document(1);
            assertEquals(1, doc.getFields(AnnotationIndexField.ENTITY.getName()).length);
            assertEquals("4302e264-1cf0-4d1f-aca7-2a6f89e34b36", doc.getField(AnnotationIndexField.ENTITY.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.NAME.getName()).length);
            assertEquals("Farming Incident", doc.getField(AnnotationIndexField.NAME.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TEXT.getName()).length);
            assertEquals("artist annotation newer", doc.getField(AnnotationIndexField.TEXT.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TYPE.getName()).length);
            assertEquals("artist", doc.getField(AnnotationIndexField.TYPE.getName()).stringValue());

            doc = ir.document(0);
            assertNotNull(doc);
            assertEquals("1", doc.getField(MetaIndexField.META.getName()).stringValue());
        }
        ir.close();
    }

    /*
     * Empty annotations should be ignored
     *
     * @throws Exception exception
     */
    @Test
    public void testEmptyArtistIndexAnnotationFields() throws Exception {
        addArtistEmptyAnnotation();
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);

        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(1, ir.numDocs());
        Document doc = ir.document(0);
        assertNotNull(doc);
        assertEquals("1", doc.getField(MetaIndexField.META.getName()).stringValue());
        ir.close();
    }

    /**
     * Null annotations should be ignored
     *
     * @throws Exception
     */
    @Test
    public void testEmptyArtistIndexAnnotationFields2() throws Exception {
        addArtistEmptyAnnotation2();
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);

        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(1, ir.numDocs());
        Document doc = ir.document(0);
        assertNotNull(doc);
        assertEquals("1", doc.getField(MetaIndexField.META.getName()).stringValue());
        ir.close();
    }

       /**
     * Basic test for Label Annotation
     *
     * @throws Exception exception
     */
       @Test
    public void testLabelIndexAnnotationFields() throws Exception {
        addLabelAnnotation();
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);

        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(2, ir.numDocs());
        {
            Document doc = ir.document(1);
            assertEquals(1, doc.getFields(AnnotationIndexField.ENTITY.getName()).length);
            assertEquals("a539bb1e-f2e1-4b45-9db8-8053841e7503", doc.getField(AnnotationIndexField.ENTITY.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.NAME.getName()).length);
            assertEquals("4AD", doc.getField(AnnotationIndexField.NAME.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TEXT.getName()).length);
            assertEquals("label annotation newer", doc.getField(AnnotationIndexField.TEXT.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TYPE.getName()).length);
            assertEquals("label", doc.getField(AnnotationIndexField.TYPE.getName()).stringValue());
        }
        ir.close();
    }

    /**
     * Basic test for Recording Annotation
     *
     * @throws Exception exception
     */
    @Test
    public void testRecordingIndexAnnotationFields() throws Exception {
        addRecordingAnnotation();
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);

        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(2, ir.numDocs());
        {
            Document doc = ir.document(1);
            assertEquals(1, doc.getFields(AnnotationIndexField.ENTITY.getName()).length);
            assertEquals("2f250ed2-6285-40f1-aa2a-14f1c05e9765", doc.getField(AnnotationIndexField.ENTITY.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.NAME.getName()).length);
            assertEquals("Do It Clean", doc.getField(AnnotationIndexField.NAME.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TEXT.getName()).length);
            assertEquals("recording annotation newer", doc.getField(AnnotationIndexField.TEXT.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TYPE.getName()).length);
            assertEquals("recording", doc.getField(AnnotationIndexField.TYPE.getName()).stringValue());
        }
        ir.close();
    }
    @Test
    public void testGetTypeByDbId () throws Exception {
        assertNull(AnnotationType.getByDbId(0));
        assertEquals(AnnotationType.ARTIST,AnnotationType.getByDbId(1));
    }


    /**
     * @throws Exception exception
     */
    private void addInstrumentAnnotation() throws Exception {

        Statement stmt = conn.createStatement();

        stmt.addBatch("INSERT INTO instrument (id, gid, name)" +
                "  VALUES (2, 'efd2ace2-b3b9-305f-8a53-9803595c0e37', 'Crocodiles')");
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
                "  VALUES (1, 1, 'instrument annotation', 'change', now())");
        stmt.addBatch("INSERT INTO instrument_annotation (instrument, annotation) VALUES (2, 1)");
        stmt.executeBatch();
        stmt.close();
    }

    /**
     * Basic test for Release Group Annotation
     *
     * @throws Exception exception
     */
    @Test
    public void testInstrumentIndexAnnotationFields() throws Exception {
        addInstrumentAnnotation();
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);

        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(2, ir.numDocs());
        {
            Document doc = ir.document(1);
            assertEquals(1, doc.getFields(AnnotationIndexField.ENTITY.getName()).length);
            assertEquals("efd2ace2-b3b9-305f-8a53-9803595c0e37", doc.getField(AnnotationIndexField.ENTITY.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.NAME.getName()).length);
            assertEquals("Crocodiles", doc.getField(AnnotationIndexField.NAME.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TEXT.getName()).length);
            assertEquals("instrument annotation", doc.getField(AnnotationIndexField.TEXT.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TYPE.getName()).length);
            assertEquals("instrument", doc.getField(AnnotationIndexField.TYPE.getName()).stringValue());
        }
        ir.close();
    }

    /**
     * @throws Exception exception
     */
    private void addAreaAnnotation() throws Exception {

        Statement stmt = conn.createStatement();

        stmt.addBatch("INSERT INTO area (id, gid, name)" +
                "  VALUES (2, 'efd2ace2-b3b9-305f-8a53-9803595c0e37', 'Crocodiles')");
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
                "  VALUES (1, 1, 'area annotation', 'change', now())");
        stmt.addBatch("INSERT INTO area_annotation (area, annotation) VALUES (2, 1)");
        stmt.executeBatch();
        stmt.close();
    }

    /**
     * Basic test for Release Group Annotation
     *
     * @throws Exception exception
     */
    @Test
    public void testAreaIndexAnnotationFields() throws Exception {
        addAreaAnnotation();
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);

        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(2, ir.numDocs());
        {
            Document doc = ir.document(1);
            assertEquals(1, doc.getFields(AnnotationIndexField.ENTITY.getName()).length);
            assertEquals("efd2ace2-b3b9-305f-8a53-9803595c0e37", doc.getField(AnnotationIndexField.ENTITY.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.NAME.getName()).length);
            assertEquals("Crocodiles", doc.getField(AnnotationIndexField.NAME.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TEXT.getName()).length);
            assertEquals("area annotation", doc.getField(AnnotationIndexField.TEXT.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TYPE.getName()).length);
            assertEquals("area", doc.getField(AnnotationIndexField.TYPE.getName()).stringValue());
        }
        ir.close();
    }

    /**
     * @throws Exception exception
     */
    private void addPlaceAnnotation() throws Exception {

        Statement stmt = conn.createStatement();

        stmt.addBatch("INSERT INTO place (id, gid, name)" +
                "  VALUES (2, 'efd2ace2-b3b9-305f-8a53-9803595c0e37', 'Crocodiles')");
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
                "  VALUES (1, 1, 'place annotation', 'change', now())");
        stmt.addBatch("INSERT INTO place_annotation (place, annotation) VALUES (2, 1)");
        stmt.executeBatch();
        stmt.close();
    }

    /**
     * Basic test for Release Group Annotation
     *
     * @throws Exception exception
     */
    @Test
    public void testPlaceIndexAnnotationFields() throws Exception {
        addPlaceAnnotation();
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);

        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(2, ir.numDocs());
        {
            Document doc = ir.document(1);
            assertEquals(1, doc.getFields(AnnotationIndexField.ENTITY.getName()).length);
            assertEquals("efd2ace2-b3b9-305f-8a53-9803595c0e37", doc.getField(AnnotationIndexField.ENTITY.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.NAME.getName()).length);
            assertEquals("Crocodiles", doc.getField(AnnotationIndexField.NAME.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TEXT.getName()).length);
            assertEquals("place annotation", doc.getField(AnnotationIndexField.TEXT.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TYPE.getName()).length);
            assertEquals("place", doc.getField(AnnotationIndexField.TYPE.getName()).stringValue());
        }
        ir.close();
    }

    /**
     * @throws Exception exception
     */
    private void addEventAnnotation() throws Exception {

        Statement stmt = conn.createStatement();

        stmt.addBatch("INSERT INTO event (id, gid, name)" +
                "  VALUES (2, 'efd2ace2-b3b9-305f-8a53-9803595c0e37', 'Crocodiles')");
        stmt.addBatch("INSERT INTO annotation (id, editor, text, changelog, created) " +
                "  VALUES (1, 1, 'event annotation', 'change', now())");
        stmt.addBatch("INSERT INTO event_annotation (event, annotation) VALUES (2, 1)");
        stmt.executeBatch();
        stmt.close();
    }

    /**
     * Basic test for Release Group Annotation
     *
     * @throws Exception exception
     */
    @Test
    public void testEventIndexAnnotationFields() throws Exception {
        addEventAnnotation();
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);

        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(2, ir.numDocs());
        {
            Document doc = ir.document(1);
            assertEquals(1, doc.getFields(AnnotationIndexField.ENTITY.getName()).length);
            assertEquals("efd2ace2-b3b9-305f-8a53-9803595c0e37", doc.getField(AnnotationIndexField.ENTITY.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.NAME.getName()).length);
            assertEquals("Crocodiles", doc.getField(AnnotationIndexField.NAME.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TEXT.getName()).length);
            assertEquals("event annotation", doc.getField(AnnotationIndexField.TEXT.getName()).stringValue());
            assertEquals(1, doc.getFields(AnnotationIndexField.TYPE.getName()).length);
            assertEquals("event", doc.getField(AnnotationIndexField.TYPE.getName()).stringValue());
        }
        ir.close();
    }
    
}


