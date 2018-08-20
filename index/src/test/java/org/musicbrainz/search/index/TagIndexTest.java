package org.musicbrainz.search.index;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

import java.sql.Statement;

public class TagIndexTest extends AbstractIndexTest {


    private void createIndex(RAMDirectory ramDir) throws Exception {
        IndexWriter writer = createIndexWriter(ramDir,TagIndexField.class);
        TagIndex ti = new TagIndex(conn);
        CommonTables ct = new CommonTables(conn, ti.getName());
        ct.createTemporaryTables(false);

        ti.init(writer, false);
        ti.addMetaInformation(writer);
        ti.indexData(writer, 0, Integer.MAX_VALUE);
        ti.destroy();
        writer.close();

    }

    private void addTagOne() throws Exception {

        Statement stmt = conn.createStatement();
        stmt.addBatch("INSERT INTO tag (id, name, ref_count) VALUES (1, 'rock', 1);");
        stmt.executeBatch();
        stmt.close();
        conn.close();
    }

    @Test
    public void testIndexTag() throws Exception {

        addTagOne();

        /* SEARCH-43
        RAMDirectory ramDir = new RAMDirectory();
        createIndex(ramDir);
        IndexReader ir = DirectoryReader.open(ramDir);
        assertEquals(2, ir.numDocs());
        {
            Document doc = ir.document(1);
            assertEquals(1, doc.getFields(TagIndexField.TAG.getName()).length);
            assertEquals("rock", doc.getField(TagIndexField.TAG.getName()).stringValue());
            ir.close();
        }
        */
    }


}