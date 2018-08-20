package org.musicbrainz.search.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.NumericUtils;
import org.junit.Before;
import org.musicbrainz.search.LuceneVersion;

import java.io.IOException;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

public abstract class AbstractIndexTest {

    /**
     * Holds connection to be used for the duration of an individual test
     */
    protected Connection conn;

    protected IndexWriter createIndexWriter(RAMDirectory ramDir, Class indexFieldClass) throws Exception
    {
        Analyzer analyzer = DatabaseIndex.getAnalyzer(indexFieldClass);
        IndexWriterConfig config = new IndexWriterConfig(LuceneVersion.LUCENE_VERSION,analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        return new IndexWriter(ramDir, config);
    }

    /**
     * Create private db in memory and setup connection
     *
     * This ensures that the database is only accessible from the connection that is
     * setup. This allows multiple tests to run concurrently without encountering race conditions and also
     * allow code that create temporary tables to work correctly in tests.
     *
     * @return
     * @throws Exception
     */
    protected void createConnection() throws Exception {
        Class.forName("org.h2.Driver");
        conn =  DriverManager.getConnection("jdbc:h2:mem:;MODE=PostgreSQL");
    }

    /** Check first term of given field, terms are listed lexigrahically
     *  Use when field is indexed. but not stored
     *
     * @param ir
     * @param field
     * @param value
     * @throws java.io.IOException
     */
    protected void checkTerm(IndexReader ir, IndexField field, String value) throws IOException {

        Fields fields = MultiFields.getFields(ir);
        Terms terms = fields.terms(field.getName());
        TermsEnum termsEnum = terms.iterator(null);
        termsEnum.next();
        assertEquals(value,termsEnum.term().utf8ToString());
    }

    protected void checkTerm(IndexReader ir, IndexField field, int value) throws IOException {

        Fields fields = MultiFields.getFields(ir);
        Terms terms = fields.terms(field.getName());
        TermsEnum termsEnum = terms.iterator(null);
        termsEnum.next();
        assertEquals(value, NumericUtils.prefixCodedToInt(termsEnum.term()));
    }

    protected void checkTerm(IndexReader ir, IndexField field, float value) throws IOException {

        Fields fields = MultiFields.getFields(ir);
        Terms terms = fields.terms(field.getName());
        TermsEnum termsEnum = terms.iterator(null);
        termsEnum.next();
        assertEquals(value, NumericUtils.sortableIntToFloat(NumericUtils.prefixCodedToInt(termsEnum.term())),0);
    }

    /** Check nth term of given field, terms are listed lexigraphically
     *  Use when field is indexed. but not stored
     *
     * @param ir
     * @param field the field
     * @param value the value of the term
     * @param index the index in the terms of the term you want to check
     * @throws IOException
     */
    protected void checkTermX(IndexReader ir, IndexField field, String value,int index) throws IOException {

        Fields fields = MultiFields.getFields(ir);
        Terms terms = fields.terms(field.getName());
        TermsEnum termsEnum = terms.iterator(null);
        int count=0;
        while(termsEnum.next()!=null)
        {
            if(count==index)
            {
                break;
            }
            count++;
        }
        assertEquals(value,termsEnum.term().utf8ToString());
    }

    @Before
    public void setup() throws Exception {
        try {
            createConnection();
            //Drop tables, if they exist
            try {
                Statement stmt = conn.createStatement();
                stmt.addBatch("DROP TABLE area");
                stmt.addBatch("DROP TABLE area_type");
                stmt.addBatch("DROP TABLE area_gid_redirect");
                stmt.addBatch("DROP TABLE iso_3166_1");
                stmt.addBatch("DROP TABLE iso_3166_2");
                stmt.addBatch("DROP TABLE iso_3166_3");
                stmt.addBatch("DROP TABLE area_alias_type");
                stmt.addBatch("DROP TABLE area_alias");
                stmt.addBatch("DROP TABLE tag");
                stmt.addBatch("DROP TABLE l_area_area");
                stmt.addBatch("DROP TABLE area_tag");

                stmt.addBatch("DROP TABLE artist");
                stmt.addBatch("DROP TABLE artist_alias");
                stmt.addBatch("DROP TABLE artist_alias_type");
                stmt.addBatch("DROP TABLE artist_type");
                stmt.addBatch("DROP TABLE artist_credit");
                stmt.addBatch("DROP TABLE artist_credit_name");
                stmt.addBatch("DROP TABLE gender");
                stmt.addBatch("DROP TABLE artist_tag");
                stmt.addBatch("DROP TABLE artist_ipi");
                stmt.addBatch("DROP TABLE artist_isni");
                stmt.addBatch("DROP TABLE editor");

                stmt.addBatch("DROP TABLE label");
                stmt.addBatch("DROP TABLE label_alias");
                stmt.addBatch("DROP TABLE label_alias_type");
                stmt.addBatch("DROP TABLE label_type");
                stmt.addBatch("DROP TABLE label_tag");
                stmt.addBatch("DROP TABLE label_ipi");
                stmt.addBatch("DROP TABLE label_isni");

                stmt.addBatch("DROP TABLE release");
                stmt.addBatch("DROP TABLE release_meta");
                stmt.addBatch("DROP TABLE release_status");
                stmt.addBatch("DROP TABLE release_packaging");
                stmt.addBatch("DROP TABLE release_label");
                stmt.addBatch("DROP TABLE medium");
                stmt.addBatch("DROP TABLE medium_format");
                stmt.addBatch("DROP TABLE medium_cdtoc");
                stmt.addBatch("DROP TABLE tracklist");
                stmt.addBatch("DROP TABLE language");
                stmt.addBatch("DROP TABLE script");
                stmt.addBatch("DROP TABLE release_tag");
                stmt.addBatch("DROP TABLE release_country");
                stmt.addBatch("DROP TABLE release_unknown_country");

                stmt.addBatch("DROP TABLE release_group");
                stmt.addBatch("DROP TABLE release_group_primary_type");
                stmt.addBatch("DROP TABLE release_group_secondary_type_join");
                stmt.addBatch("DROP TABLE release_group_secondary_type");

                stmt.addBatch("DROP TABLE release_group_tag");

                stmt.addBatch("DROP TABLE track");
                stmt.addBatch("DROP TABLE recording");
                stmt.addBatch("DROP TABLE recording_tag");
                stmt.addBatch("DROP TABLE recording_puid");
                stmt.addBatch("DROP TABLE puid");
                stmt.addBatch("DROP TABLE isrc");

                stmt.addBatch("DROP TABLE annotation");
                stmt.addBatch("DROP TABLE artist_annotation");
                stmt.addBatch("DROP TABLE label_annotation");
                stmt.addBatch("DROP TABLE recording_annotation");
                stmt.addBatch("DROP TABLE release_annotation");
                stmt.addBatch("DROP TABLE release_group_annotation");
                stmt.addBatch("DROP TABLE work_annotation");
                stmt.addBatch("DROP TABLE series_annotation");
                stmt.addBatch("DROP TABLE area_annotation");
                stmt.addBatch("DROP TABLE instrument_annotation");

                stmt.addBatch("DROP TABLE release_raw");
                stmt.addBatch("DROP TABLE cdtoc_raw");
                stmt.addBatch("DROP TABLE track_raw");

                stmt.addBatch("DROP TABLE url");
                stmt.addBatch("DROP TABLE l_area_url");
                stmt.addBatch("DROP TABLE l_artist_url");
                stmt.addBatch("DROP TABLE l_place_url");
                stmt.addBatch("DROP TABLE l_recording_url");
                stmt.addBatch("DROP TABLE l_release_url");
                stmt.addBatch("DROP TABLE l_release_group_url");
                stmt.addBatch("DROP TABLE l_url_url");
                stmt.addBatch("DROP TABLE l_work_url");

                stmt.addBatch("DROP TABLE work");
                stmt.addBatch("DROP TABLE iswc");
                stmt.addBatch("DROP TABLE work_alias");
                stmt.addBatch("DROP TABLE work_alias_type");
                stmt.addBatch("DROP TABLE work_type");
                stmt.addBatch("DROP TABLE work_tag");
                stmt.addBatch("DROP TABLE l_artist_work");
                stmt.addBatch("DROP TABLE l_recording_work");
                stmt.addBatch("DROP TABLE link");
                stmt.addBatch("DROP TABLE link_type");
                stmt.addBatch("DROP TABLE link_attribute");
                stmt.addBatch("DROP TABLE link_attribute_type");

                stmt.addBatch("DROP TABLE place_tag");
                stmt.addBatch("DROP TABLE place");
                stmt.addBatch("DROP TABLE place_gid_redirect");
                stmt.addBatch("DROP TABLE place_type");
                stmt.addBatch("DROP TABLE place_alias");
                stmt.addBatch("DROP TABLE place_alias_type");
                stmt.addBatch("DROP TABLE place_annotation");
                stmt.addBatch("DROP TABLE l_area_place");
                stmt.addBatch("DROP TABLE l_artist_place");
                stmt.addBatch("DROP TABLE l_label_place");
                stmt.addBatch("DROP TABLE l_place_place");
                stmt.addBatch("DROP TABLE l_place_recording");
                stmt.addBatch("DROP TABLE l_place_release");
                stmt.addBatch("DROP TABLE l_place_release_group");
                stmt.addBatch("DROP TABLE l_place_url");
                stmt.addBatch("DROP TABLE l_place_work");
                stmt.addBatch("DROP TABLE l_place_tag");

                stmt.addBatch("DROP TABLE event_tag");
                stmt.addBatch("DROP TABLE event");
                stmt.addBatch("DROP TABLE event_gid_redirect");
                stmt.addBatch("DROP TABLE event_type");
                stmt.addBatch("DROP TABLE event_alias");
                stmt.addBatch("DROP TABLE event_alias_type");
                stmt.addBatch("DROP TABLE event_annotation");
                stmt.addBatch("DROP TABLE l_area_event");
                stmt.addBatch("DROP TABLE l_artist_event");
                stmt.addBatch("DROP TABLE l_label_event");
                stmt.addBatch("DROP TABLE l_event_event");
                stmt.addBatch("DROP TABLE l_event_place");
                stmt.addBatch("DROP TABLE l_event_recording");
                stmt.addBatch("DROP TABLE l_event_release");
                stmt.addBatch("DROP TABLE l_event_release_group");
                stmt.addBatch("DROP TABLE l_event_url");
                stmt.addBatch("DROP TABLE l_event_work");
                stmt.addBatch("DROP TABLE l_event_tag");

                stmt.addBatch("DROP TABLE instrument_type");
                stmt.addBatch("DROP TABLE instrument");
                stmt.addBatch("DROP TABLE instrument_alias_type");
                stmt.addBatch("DROP TABLE instrument_alias");
                stmt.addBatch("DROP TABLE instrument_tag");

                stmt.addBatch("DROP TABLE series_type");
                stmt.addBatch("DROP TABLE series");
                stmt.addBatch("DROP TABLE series_alias_type");
                stmt.addBatch("DROP TABLE series_alias");
                stmt.addBatch("DROP TABLE series_ordering_type");
                stmt.addBatch("DROP TABLE series_deletion");
                stmt.addBatch("DROP TABLE series_tag");

                stmt.addBatch("DROP TABLE replication_control");
                stmt.addBatch("DROP TABLE dbmirror_pending");
                stmt.addBatch("DROP TABLE dbmirror_pendingdata");

                stmt.executeBatch();
                stmt.close();
            }
            catch (BatchUpdateException bue) {
                //Ignore errors, because will just be that tables does not exist
                if(bue.getMessage()!=null)
                {
                    System.err.println(bue.getMessage());
                }
            }

            //Create tables and data for Artist Index
            Statement stmt = conn.createStatement();

            setupCommonTables(stmt);
            setupArtistTables(stmt);
            setupLabelTables(stmt);
            setupReleaseTables(stmt);
            setupReleaseGroupTables(stmt);
            setupRecordingTables(stmt);
            setupAnnotationTables(stmt);
            setupCDStubTables(stmt);
            setupWorkTables(stmt);
            setupPlaceTables(stmt);
            setupEventTables(stmt);
            setupUrlTables(stmt);
            setupInstrumentTables(stmt);
            setupSeriesTables(stmt);
            setupEditorTables(stmt);
            setupReplicationTables(stmt);
            
            insertReferenceData(stmt);
            insertReplicationInfo(stmt);
            
            stmt.executeBatch();
            stmt.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    protected void setupCommonTables(Statement stmt) throws Exception {

        stmt.addBatch("CREATE TABLE area_type (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL);");


        stmt.addBatch("CREATE TABLE area (id          INTEGER PRIMARY KEY," +
                "                   gid               uuid," +
                "                   name              VARCHAR ," +
                "                   comment           VARCHAR ," +
                "                   type              INTEGER," +
                "                   edits_pending     INTEGER NOT NULL DEFAULT 0," +
                "                   last_updated      TIMESTAMP," +
                "                   begin_date_year   SMALLINT," +
                "                   begin_date_month  SMALLINT," +
                "                   begin_date_day    SMALLINT," +
                "                   end_date_year     SMALLINT," +
                "                   end_date_month    SMALLINT," +
                "                   end_date_day      SMALLINT," +
                "                   ended             BOOLEAN NOT NULL DEFAULT FALSE" +
                "                   );");

        stmt.addBatch("CREATE TABLE area_gid_redirect (" +
                "    gid UUID NOT NULL PRIMARY KEY," +
                "    new_id INTEGER NOT NULL," +
                "    created TIMESTAMP" +
                ");");

        stmt.addBatch("CREATE TABLE iso_3166_1 (area      INTEGER NOT NULL," +
                "                         code      CHAR(2) PRIMARY KEY);" +
                "");

        stmt.addBatch("CREATE TABLE iso_3166_2 (area      INTEGER NOT NULL," +
                "                         code      VARCHAR(10) PRIMARY KEY);");

        stmt.addBatch("CREATE TABLE iso_3166_3 (area      INTEGER NOT NULL," +
                "                         code      CHAR(4) PRIMARY KEY);");


        stmt.addBatch("CREATE TABLE area_alias_type (id SERIAL PRIMARY KEY, name TEXT NOT NULL);");


        stmt.addBatch("CREATE TABLE area_alias (id            SERIAL PRIMARY KEY," +
                "                         area                INTEGER NOT NULL," +
                "                         name                VARCHAR NOT NULL," +
                "                         locale              TEXT," +
                "                         edits_pending       INTEGER NOT NULL DEFAULT 0 ," +
                "                         last_updated        TIMESTAMP," +
                "                         type                INTEGER," +
                "                         sort_name           VARCHAR NOT NULL," +
                "                         begin_date_year     SMALLINT," +
                "                         begin_date_month    SMALLINT," +
                "                         begin_date_day      SMALLINT," +
                "                         end_date_year       SMALLINT," +
                "                         end_date_month      SMALLINT," +
                "                         end_date_day        SMALLINT," +
                "                         primary_for_locale  BOOLEAN NOT NULL DEFAULT false"+
                ");");

        stmt.addBatch("CREATE TABLE l_area_area" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL DEFAULT 0," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE tag (" +
                "  id serial NOT NULL," +
                "  name character varying(255) NOT NULL," +
                "  ref_count integer NOT NULL DEFAULT 0" +
                ")");

        stmt.addBatch("CREATE TABLE area_tag (" +
                "  area integer NOT NULL," +
                "  tag integer NOT NULL," +
                "  count integer NOT NULL," +
                "  last_updated timestamp" +
                ")");


    }

    protected void setupArtistTables(Statement stmt) throws Exception {

        stmt.addBatch("CREATE TABLE artist (" +
                "  id serial NOT NULL," +
                "  gid uuid NOT NULL," +
                "  name character varying(255)," +
                "  sort_name character varying(255)," +
                "  begin_date_year integer," +
                "  begin_date_month integer," +
                "  begin_date_day integer," +
                "  end_date_year integer," +
                "  end_date_month integer," +
                "  end_date_day integer," +
                "  type integer," +
                "  area integer," +
                "  begin_area integer," +
                "  end_area integer," +
                "  gender integer," +
                "  comment character varying(255)," +
                "  last_updated timestamp," +
                "  edits_pending integer DEFAULT 0," +
                "  ended boolean" +
                ")");

        stmt.addBatch("CREATE TABLE artist_credit (" +
                "  id serial NOT NULL," +
                "  name character varying(255)," +
                "  artist_count integer NOT NULL," +
                "  ref_count integer DEFAULT 0," +
                "  created timestamp" +
                ")");

        stmt.addBatch("CREATE TABLE artist_credit_name (" +
                "  artist_credit integer NOT NULL," +
                "  position integer NOT NULL," +
                "  artist integer not null," +
                "  name character varying(255)," +
                "  join_phrase character varying(32)" +
                ")");

        stmt.addBatch("CREATE TABLE artist_alias (" +
                "  id serial NOT NULL," +
                "  artist integer NOT NULL," +
                "  name character varying(255)," +
                "  locale text," +
                "  last_updated timestamp," +
                "  edits_pending integer NOT NULL DEFAULT 0," +
                "  type                INTEGER," +
                "  sort_name           character varying(255)," +
                "  begin_date_year     SMALLINT," +
                "  begin_date_month    SMALLINT," +
                "  begin_date_day      SMALLINT," +
                "  end_date_year       SMALLINT," +
                "  end_date_month      SMALLINT," +
                "  end_date_day        SMALLINT," +
                "  primary_for_locale  BOOLEAN"   +
                ")");
        stmt.addBatch("CREATE TABLE artist_alias_type (" +
                " id SERIAL,"  +
                " name TEXT" +
                ")");

        stmt.addBatch("CREATE TABLE artist_type (" +
                "  id serial NOT NULL," +
                "  name character varying(255) NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE gender (" +
                "  id serial NOT NULL," +
                "  name character varying(255) NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE artist_tag" +
                "(" +
                "  artist integer NOT NULL," +
                "  tag integer NOT NULL," +
                "  count integer NOT NULL," +
                "  last_updated timestamp," +
                ")");


        stmt.addBatch("CREATE TABLE artist_ipi" +
                   "(" +
                   " artist              INTEGER," +
                   " ipi                 VARCHAR(11)," +
                   " edits_pending       INTEGER," +
                   " created             TIMESTAMP" +
                   " )"
                   );

        stmt.addBatch("CREATE TABLE artist_isni" +
                        "(" +
                        " artist              INTEGER," +
                        " isni                CHAR(16)," +
                        " edits_pending       INTEGER," +
                        " created             TIMESTAMP" +
                        " )"
        );

    }

    protected void setupLabelTables(Statement stmt) throws Exception {
        stmt.addBatch("CREATE TABLE label (" +
                "  id serial NOT NULL," +
                "  gid uuid NOT NULL," +
                "  name character varying(255)," +
                "  begin_date_year integer," +
                "  begin_date_month integer," +
                "  begin_date_day integer," +
                "  end_date_year integer," +
                "  end_date_month integer," +
                "  end_date_day integer," +
                "  label_code integer," +
                "  type integer," +
                "  area integer," +
                "  comment character varying(255)," +
                "  last_updated timestamp," +
                "  edits_pending integer DEFAULT 0," +
                "  ended boolean" +
                ")");

        stmt.addBatch("CREATE TABLE label_alias (" +
                "  id serial NOT NULL," +
                "  label integer NOT NULL," +
                "  name character varying(255)," +
                "  locale text," +
                "  last_updated timestamp," +
                "  edits_pending integer NOT NULL DEFAULT 0," +
                "  type                INTEGER," +
                "  sort_name           character varying(255)," +
                "  begin_date_year     SMALLINT," +
                "  begin_date_month    SMALLINT," +
                "  begin_date_day      SMALLINT," +
                "  end_date_year       SMALLINT," +
                "  end_date_month      SMALLINT," +
                "  end_date_day        SMALLINT," +
                "  primary_for_locale  BOOLEAN"   +
                ")");

        stmt.addBatch("CREATE TABLE label_alias_type (" +
                " id SERIAL,"  +
                " name TEXT" +
                ")");

        stmt.addBatch("CREATE TABLE label_type (" +
                "  id serial NOT NULL," +
                "  name character varying(255) NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE label_tag (" +
                "  label integer NOT NULL," +
                "  tag integer NOT NULL," +
                "  count integer NOT NULL," +
                "  last_updated timestamp" +
                ")");

        stmt.addBatch("CREATE TABLE label_ipi" +
                "(" +
                " label              INTEGER," +
                " ipi                 VARCHAR(11)," +
                " edits_pending       INTEGER," +
                " created             TIMESTAMP" +
                " )"
        );

        stmt.addBatch("CREATE TABLE label_isni" +
                        "(" +
                        " artist              INTEGER," +
                        " isni                CHAR(16)," +
                        " edits_pending       INTEGER," +
                        " created             TIMESTAMP" +
                        " )"
        );

    }

    protected void setupReleaseTables(Statement stmt) throws Exception {

        stmt.addBatch("CREATE TABLE release (" +
                "  id serial NOT NULL," +
                "  gid uuid NOT NULL," +
                "  name character varying(255)," +
                "  artist_credit integer NOT NULL," +
                "  release_group integer NOT NULL," +
                "  status integer," +
                "  packaging integer," +
                "  language integer," +
                "  script integer," +
                "  barcode character varying(255)," +
                "  comment character varying(255)," +
                "  quality integer," +
                "  last_updated timestamp," +
                "  edits_pending integer DEFAULT 0" +
                ")");

        stmt.addBatch("CREATE TABLE release_status (" +
                "  id serial NOT NULL," +
                "  name character varying(255) NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE release_packaging (" +
                "  id serial NOT NULL," +
                "  name character varying(255) NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE release_label (" +
                "  id serial NOT NULL," +
                "  release integer NOT NULL," +
                "  label integer," +
                "  catalog_number character varying(255)," +
                "  last_updated timestamp" +
                ")");

        stmt.addBatch("CREATE TABLE medium (" +
                "  id serial NOT NULL," +
                "  track_count integer NOT NULL," +
                "  release integer NOT NULL," +
                "  position integer NOT NULL," +
                "  format integer," +
                "  name character varying(255)," +
                "  last_updated timestamp," +
                "  edits_pending integer DEFAULT 0" +
                ")");

        stmt.addBatch("CREATE TABLE medium_format (" +
                "  id serial NOT NULL," +
                "  name character varying(255) NOT NULL," +
                "  year integer" +
                ")");

        stmt.addBatch("CREATE TABLE release_meta (" +
                "  id integer NOT NULL," +
                "  date_added timestamp," +
                "  info_url character varying(255)," +
                "  amazon_asin character varying(10)," +
                "  amazon_store character varying(20)" +
                ")");

        stmt.addBatch("CREATE TABLE medium_cdtoc (" +
                "  id serial NOT NULL," +
                "  medium integer NOT NULL," +
                "  cdtoc integer NOT NULL," +
                "  last_updated timestamp," +
                "  edits_pending integer NOT NULL DEFAULT 0" +
                ")");

        stmt.addBatch("CREATE TABLE tracklist (" +
                "  id serial NOT NULL," +
                "  track_count integer NOT NULL DEFAULT 0," +
                "  last_updated timestamp" +
                ")");
        
        stmt.addBatch("CREATE TABLE language (" +
                "  id serial NOT NULL," +
                "  iso_code_3  character(3)," +
                "  iso_code_2t character(3) NOT NULL," +
                "  iso_code_2b character(3) NOT NULL," +
                "  iso_code_1 character(2)," +
                "  name character varying(100) NOT NULL," +
                "  frequency integer NOT NULL DEFAULT 0" +
                ")");

        stmt.addBatch("CREATE TABLE script (" +
                "  id serial NOT NULL," +
                "  iso_code character(4) NOT NULL," +
                "  iso_number character(3) NOT NULL," +
                "  name character varying(100) NOT NULL," +
                "  frequency integer NOT NULL DEFAULT 0" +
                ")");

        stmt.addBatch("CREATE TABLE release_tag" +
                "(" +
                "  release integer NOT NULL," +
                "  tag integer NOT NULL," +
                "  count integer NOT NULL," +
                "  last_updated timestamp" +
                ")");

        stmt.addBatch("CREATE TABLE release_country" +
                "(" +
                "  release integer NOT NULL," +
                "  country integer NOT NULL," +
                "  date_year smallint," +
                "  date_month smallint," +
                "  date_day smallint" +
                ")");

        stmt.addBatch("CREATE TABLE release_unknown_country" +
                "(" +
                "  release integer NOT NULL," +
                "  date_year smallint," +
                "  date_month smallint," +
                "  date_day smallint" +
                ")");
    }

    protected void setupReleaseGroupTables(Statement stmt) throws Exception {

        stmt.addBatch("CREATE TABLE release_group (" +
                "  id serial NOT NULL," +
                "  gid uuid NOT NULL," +
                "  name character varying(255)," +
                "  artist_credit integer NOT NULL," +
                "  type integer," +
                "  comment character varying(255)," +
                "  last_updated timestamp," +
                "  edits_pending integer NOT NULL DEFAULT 0" +
                ")");

        stmt.addBatch("CREATE TABLE release_group_primary_type (" +
                "  id serial NOT NULL," +
                "  name character varying(255) NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE release_group_secondary_type_join (" +
                "  release_group INTEGER NOT NULL," +
                "  secondary_type INTEGER NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE release_group_secondary_type (" +
                "  id serial NOT NULL," +
                "  name character varying(255) NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE release_group_tag" +
                "(" +
                "  release_group integer NOT NULL," +
                "  tag integer NOT NULL," +
                "  count integer NOT NULL," +
                "  last_updated timestamp" +
                ")");
    }

    protected void setupRecordingTables(Statement stmt) throws Exception {

       stmt.addBatch("CREATE TABLE track (" +
                "  id serial," +
                "  gid uuid," +
                "  recording integer NOT NULL," +
                "  medium integer NOT NULL," +
                "  position integer NOT NULL," +
                "  number text, " +
                "  name character varying(255)," +
                "  artist_credit integer NOT NULL," +
                "  length integer," +
                "  last_updated timestamp," +
                "  edits_pending integer NOT NULL DEFAULT 0," +
                "  is_data_track BOOLEAN NOT NULL DEFAULT FALSE" +
               ")");

        stmt.addBatch("CREATE TABLE recording (" +
                "  id serial NOT NULL," +
                "  gid uuid NOT NULL," +
                "  name character varying(255)," +
                "  artist_credit integer NOT NULL," +
                "  length integer," +
                "  comment character varying(255)," +
                "  last_updated timestamp," +
                "  edits_pending integer NOT NULL DEFAULT 0," +
                "  video BOOLEAN NOT NULL DEFAULT FALSE" +
                ")");

        stmt.addBatch("CREATE TABLE recording_tag" +
                "(" +
                "  recording integer NOT NULL," +
                "  tag integer NOT NULL," +
                "  count integer NOT NULL," +
                "  last_updated timestamp" +
                ")");

        stmt.addBatch("CREATE TABLE isrc" +
                "(" +
                "  id serial NOT NULL," +
                "  recording integer NOT NULL," +
                "  isrc character(12) NOT NULL," +
                "  source smallint," +
                "  created timestamp," +
                "  edits_pending integer NOT NULL DEFAULT 0" +
                ")");

        stmt.addBatch("CREATE TABLE recording_puid" +
                "(" +
                "  id serial NOT NULL," +
                "  puid integer NOT NULL," +
                "  recording integer NOT NULL," +
                "  created timestamp," +
                "  edits_pending integer NOT NULL DEFAULT 0," +
                ")");

        stmt.addBatch("CREATE TABLE puid" +
                "(" +
                "  id serial NOT NULL," +
                "  puid character(36) NOT NULL," +
                ")");
    }

    protected void setupAnnotationTables(Statement stmt) throws Exception {
        
        stmt.addBatch(
                "CREATE TABLE annotation (" +
                "  id serial NOT NULL, " +
                "  editor integer NOT NULL," +
                "  text text," +
                "  changelog character varying(255), " +
                "  created timestamp" +
                ")" );

        stmt.addBatch("CREATE TABLE artist_annotation (" +
                "  artist integer NOT NULL," +
                "  annotation integer NOT NULL" +
                ")");
        
        stmt.addBatch("CREATE TABLE label_annotation (" +
                "  label integer NOT NULL," +
                "  annotation integer NOT NULL" +
                ")");
        
        stmt.addBatch("CREATE TABLE recording_annotation (" +
                "  recording integer NOT NULL," +
                "  annotation integer NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE release_annotation (" +
                "  release integer NOT NULL," +
                "  annotation integer NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE release_group_annotation (" +
                "  release_group integer NOT NULL," +
                "  annotation integer NOT NULL" +
                ")");
        
        stmt.addBatch("CREATE TABLE work_annotation (" +
                "  work integer NOT NULL," +
                "  annotation integer NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE series_annotation" +
                "(" +
                "    series              INTEGER NOT NULL," +
                "    annotation          INTEGER NOT NULL," +
                ")");

        stmt.addBatch("CREATE TABLE area_annotation" +
                "(" +
                "    area                INTEGER NOT NULL," +
                "    annotation          INTEGER NOT NULL," +
                ")");

        stmt.addBatch("CREATE TABLE instrument_annotation" +
                "(" +
                "    instrument          INTEGER NOT NULL," +
                "    annotation          INTEGER NOT NULL," +
                ")");
    }

    protected void setupCDStubTables(Statement stmt) throws Exception {
        
        stmt.addBatch("CREATE TABLE release_raw (" +
                "  id serial NOT NULL," +
                "  title character varying(255) NOT NULL," +
                "  artist character varying(255)," +
                "  added timestamp DEFAULT now()," +
                "  last_modified timestamp DEFAULT now()," +
                "  lookup_count integer DEFAULT 0," +
                "  modify_count integer DEFAULT 0," +
                "  source integer DEFAULT 0," +
                "  barcode character varying(255)," +
                "  comment character varying(255)" +
                ")");
        
        stmt.addBatch("CREATE TABLE track_raw (" +
                "  id serial NOT NULL," +
                "  release integer NOT NULL," +
                "  title character varying(255) NOT NULL," +
                "  artist character varying(255)," +
                "  sequence integer NOT NULL" +
                ")");
        
        stmt.addBatch("CREATE TABLE cdtoc_raw (" +
                "  id serial NOT NULL," +
                "  release integer NOT NULL," +
                "  discid character(28) NOT NULL," +
                "  track_count integer NOT NULL," +
                "  leadout_offset integer NOT NULL" +
                // "  track_offset integer[] NOT NULL"  // Not needed for our purposes (and h2 doesn't support array)
                ")");            
    }

    protected void setupWorkTables(Statement stmt) throws Exception {
        
        stmt.addBatch("CREATE TABLE work (" +
                "  id serial NOT NULL," +
                "  gid uuid NOT NULL," +
                "  name character varying(255)," +
                "  artist_credit integer NOT NULL," +
                "  type integer," +
                "  comment character varying(255)," +
                "  last_updated timestamp," +
                "  edits_pending integer NOT NULL DEFAULT 0" +
                ")");

        stmt.addBatch("CREATE TABLE iswc (" +
                "id SERIAL NOT NULL," +
                "work INTEGER NOT NULL," +
                "iswc CHARACTER(15)," +
                "source SMALLINT," +
                "edits_pending INTEGER NOT NULL DEFAULT 0," +
                "created TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE work_language (" +
                "work INTEGER NOT NULL," +
                "language INTEGER NOT NULL," +
                "edits_pending INTEGER NOT NULL DEFAULT 0," +
                "created TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE work_alias (" +
                "  id serial NOT NULL," +
                "  work integer NOT NULL," +
                "  name character varying(255)," +
                "  locale text," +
                "  sort_name character varying(255)," +
                "  type                INTEGER," +
                "  begin_date_year     SMALLINT," +
                "  begin_date_month    SMALLINT," +
                "  begin_date_day      SMALLINT," +
                "  end_date_year       SMALLINT," +
                "  end_date_month      SMALLINT," +
                "  end_date_day        SMALLINT," +
                "  primary_for_locale  BOOLEAN,"  +
                "  last_updated timestamp," +
                "  edits_pending integer NOT NULL DEFAULT 0" +
                ")");

        stmt.addBatch("CREATE TABLE work_alias_type (" +
                " id SERIAL,"  +
                " name TEXT" +
                ")");

        stmt.addBatch("CREATE TABLE work_type (" +
                "  id serial NOT NULL," +
                "  name character varying(255) NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE work_tag (" +
                "  work integer NOT NULL," +
                "  tag integer NOT NULL," +
                "  count integer NOT NULL," +
                "  last_updated timestamp" +
                ")");

        stmt.addBatch("CREATE TABLE l_artist_work" +
                "(" +
                "  id serial NOT NULL," +
                "  link integer NOT NULL," +
                "  entity0 integer NOT NULL," +
                "  entity1 integer NOT NULL," +
                "  edits_pending integer," +
                "  last_updated timestamp" +
                ")");

        stmt.addBatch("CREATE TABLE l_recording_work" +
                "(" +
                "  id serial NOT NULL," +
                "  link integer NOT NULL," +
                "  entity0 integer NOT NULL," +
                "  entity1 integer NOT NULL," +
                "  edits_pending integer," +
                "  last_updated timestamp" +
                ")");

        stmt.addBatch("CREATE TABLE link" +
                "(" +
                "  id serial NOT NULL," +
                "  link_type integer NOT NULL," +
                "  begin_date_year smallint," +
                "  begin_date_month smallint," +
                "  begin_date_day smallint," +
                "  end_date_year smallint," +
                "  end_date_month smallint," +
                "  end_date_day smallint," +
                "  attribute_count integer," +
                "  created timestamp" +
                ")");

        stmt.addBatch("CREATE TABLE link_type" +
                "(" +
                "  id serial ," +
                "  parent integer," +
                "  child_order integer," +
                "  gid uuid," +
                "  entity_type0 character varying(50)," +
                "  entity_type1 character varying(50)," +
                "  name character varying(50)," +
                "  description text," +
                "  link_phrase character varying(50)," +
                "  reverse_link_phrase character varying(50)," +
                "  short_link_phrase character varying(50)," +
                "  priority integer," +
                "  last_updated timestamp" +
                ")");

        stmt.addBatch("CREATE TABLE link_attribute" +
                "(" +
                "  link integer NOT NULL," +
                "  attribute_type integer NOT NULL," +
                "  created timestamp" +
                ")");

        stmt.addBatch("CREATE TABLE link_attribute_type" +
                "( " +
                "  id serial NOT NULL," +
                "  parent integer," +
                "  root integer NOT NULL," +
                "  child_order integer NOT NULL DEFAULT 0," +
                "  gid uuid NOT NULL," +
                "  name character varying(255) NOT NULL," +
                "  description text," +
                "  last_updated timestamp" +
                ")");
    }



    protected void setupPlaceTables(Statement stmt) throws Exception {

        PlaceIndex.isUsingH2Db=true;

        stmt.addBatch("create domain point as array check (array_length(value) = 2)");
        stmt.addBatch("CREATE TABLE place (" +
                "    id                  SERIAL," +
                "    gid                 uuid," +
                "    name                VARCHAR," +
                "    type                INTEGER," +
                "    address             VARCHAR  ," +
                "    area                INTEGER," +
                "    coordinates         POINT," +
                "    comment             VARCHAR(255)," +
                "    edits_pending       INTEGER  ," +
                "    last_updated        TIMESTAMP," +
                "    begin_date_year     SMALLINT," +
                "    begin_date_month    SMALLINT," +
                "    begin_date_day      SMALLINT," +
                "    end_date_year       SMALLINT," +
                "    end_date_month      SMALLINT," +
                "    end_date_day        SMALLINT," +
                "    ended               BOOLEAN " +
                ")");

        stmt.addBatch("CREATE TABLE place_gid_redirect" +
                "(" +
                "    gid                 UUID NOT NULL," +
                "    new_id              INTEGER NOT NULL," +
                "    created             TIMESTAMP" +
                ")");



        stmt.addBatch("CREATE TABLE place_type (" +
                "    id                  SERIAL," +
                "    name                VARCHAR(255) NOT NULL" +
                ")");


        stmt.addBatch("CREATE TABLE place_alias" +
                "(" +
                "    id                  SERIAL," +
                "    place               INTEGER NOT NULL," +
                "    name                VARCHAR NOT NULL," +
                "    locale              VARCHAR," +
                "    edits_pending       INTEGER ," +
                "    last_updated        TIMESTAMP," +
                "    type                INTEGER," +
                "    sort_name           VARCHAR ," +
                "    begin_date_year     SMALLINT," +
                "    begin_date_month    SMALLINT," +
                "    begin_date_day      SMALLINT," +
                "    end_date_year       SMALLINT," +
                "    end_date_month      SMALLINT," +
                "    end_date_day        SMALLINT," +
                "    primary_for_locale  BOOLEAN ," +
                "    ended               BOOLEAN " +
                ")");

        stmt.addBatch("CREATE TABLE place_alias_type (" +
                "    id SERIAL," +
                "    name TEXT NOT NULL" +
                ")");

        stmt.addBatch("CREATE TABLE place_annotation" +
                "(" +
                "    place               INTEGER NOT NULL," +
                "    annotation          INTEGER NOT NULL," +
                ")");

        stmt.addBatch("CREATE TABLE l_area_place" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE l_artist_place" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE l_label_place" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE l_place_place" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE l_place_recording" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE l_place_release" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE l_place_release_group" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL," +
                "    last_updated        TIMESTAMP" +
                ")");



        stmt.addBatch("CREATE TABLE l_place_work" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE place_tag" +
                "(" +
                "    place               INTEGER NOT NULL," +
                "    tag                 INTEGER NOT NULL," +
                "    count               INTEGER NOT NULL," +
                "    last_updated        TIMESTAMP" +
                ")");

    }

    protected void setupEventTables(Statement stmt) throws Exception {
        stmt.addBatch("CREATE TABLE event (" +
                "    id                  SERIAL," +
                "    gid                 uuid," +
                "    name                VARCHAR," +
                "    begin_date_year     SMALLINT," +
                "    begin_date_month    SMALLINT," +
                "    begin_date_day      SMALLINT," +
                "    end_date_year       SMALLINT," +
                "    end_date_month      SMALLINT," +
                "    end_date_day        SMALLINT," +
                "    ended               BOOLEAN, " +
                "    time                TIME," +
                "    type                INTEGER," +
                "    cancelled           BOOLEAN," +
                "    setlist             TEXT," +
                "    comment             VARCHAR(255)," +
                "    edits_pending       INTEGER," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE event_gid_redirect" +
                "(" +
                "    gid                 UUID NOT NULL," +
                "    new_id              INTEGER NOT NULL," +
                "    created             TIMESTAMP" +
                ")");



        stmt.addBatch("CREATE TABLE event_type (" +
                "    id                  SERIAL," +
                "    name                VARCHAR(255) NOT NULL," +
                "    parent              INTEGER, " +
                "    child_order         INTEGER," +
                "    description         TEXT" +
                ")");

        stmt.addBatch("CREATE TABLE event_alias" +
                "(" +
                "    id                  SERIAL," +
                "    event               INTEGER NOT NULL," +
                "    name                VARCHAR NOT NULL," +
                "    locale              VARCHAR," +
                "    edits_pending       INTEGER ," +
                "    last_updated        TIMESTAMP," +
                "    type                INTEGER," +
                "    sort_name           VARCHAR ," +
                "    begin_date_year     SMALLINT," +
                "    begin_date_month    SMALLINT," +
                "    begin_date_day      SMALLINT," +
                "    end_date_year       SMALLINT," +
                "    end_date_month      SMALLINT," +
                "    end_date_day        SMALLINT," +
                "    primary_for_locale  BOOLEAN ," +
                "    ended               BOOLEAN " +
                ")");
        stmt.addBatch("CREATE TABLE event_alias_type (" +
                "    id SERIAL," +
                "    name TEXT NOT NULL," +
                "    parent INTEGER, " +
                "    child_order         INTEGER NOT NULL DEFAULT 0," +
                "    description         TEXT" +
                ")");

        stmt.addBatch("CREATE TABLE event_annotation" +
                "(" +
                "    event               INTEGER NOT NULL," +
                "    annotation          INTEGER NOT NULL," +
                ")");

        stmt.addBatch("CREATE TABLE l_area_event" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE l_artist_event" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE l_label_event" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE l_event_event" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER," +
                "    last_updated        TIMESTAMP" +
                ")");


        stmt.addBatch("CREATE TABLE l_event_place" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER ," +
                "    last_updated        TIMESTAMP" +
                ")");
        
        stmt.addBatch("CREATE TABLE l_event_recording" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER ," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE l_event_release" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE l_event_release_group" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER," +
                "    last_updated        TIMESTAMP" +
                ")");



        stmt.addBatch("CREATE TABLE l_event_work" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE event_tag" +
                "(" +
                "    event               INTEGER NOT NULL," +
                "    tag                 INTEGER NOT NULL," +
                "    count               INTEGER NOT NULL," +
                "    last_updated        TIMESTAMP" +
                ")");

    }

    protected void setupSeriesTables(Statement stmt) throws Exception {

        stmt.addBatch("CREATE TABLE series" +
                "(\n" +
                "    id                  SERIAL,\n" +
                "    gid                 UUID ,\n" +
                "    name                VARCHAR,\n" +
                "    comment             VARCHAR(255) ,\n" +
                "    type                INTEGER,\n" +
                "    ordering_attribute  INTEGER,\n" +
                "    ordering_type       INTEGER,\n" +
                "    edits_pending       INTEGER,\n" +
                "    last_updated        TIMESTAMP\n" +
                ")");

        stmt.addBatch("CREATE TABLE series_type" +
                "(\n" +
                "    id                  SERIAL,\n" +
                "    name                VARCHAR(255) ,\n" +
                "    entity_type         VARCHAR(50),\n" +
                "    parent              INTEGER,\n" +
                "    child_order         INTEGER,\n" +
                "    description         TEXT\n" +
                ")");


        stmt.addBatch("CREATE TABLE series_ordering_type" +
                "(\n" +
                "    id                  SERIAL,\n" +
                "    name                VARCHAR(255),\n" +
                "    parent              INTEGER,\n" +
                "    child_order         INTEGER,\n" +
                "    description         TEXT\n" +
                ")");

        stmt.addBatch("CREATE TABLE series_deletion\n" +
                "(\n" +
                "    gid                 UUID,\n" +
                "    last_known_name     VARCHAR,\n" +
                "    last_known_comment  TEXT,\n" +
                "    deleted_at          timestamp \n" +
                ")");


        stmt.addBatch("CREATE TABLE series_alias_type\n" +
                "(\n" +
                "    id                  SERIAL, \n" +
                "    name                TEXT ,\n" +
                "    parent              INTEGER,\n" +
                "    child_order         INTEGER,\n" +
                "    description         TEXT\n" +
                ")");

        stmt.addBatch("CREATE TABLE series_alias\n" +
                "(\n" +
                "    id                  SERIAL,\n" +
                "    series              INTEGER,\n" +
                "    name                VARCHAR,\n" +
                "    locale              TEXT,\n" +
                "    edits_pending       INTEGER,\n" +
                "    last_updated        TIMESTAMP,\n" +
                "    type                INTEGER,\n" +
                "    sort_name           VARCHAR ,\n" +
                "    begin_date_year     SMALLINT,\n" +
                "    begin_date_month    SMALLINT,\n" +
                "    begin_date_day      SMALLINT,\n" +
                "    end_date_year       SMALLINT,\n" +
                "    end_date_month      SMALLINT,\n" +
                "    end_date_day        SMALLINT,\n" +
                "    primary_for_locale  BOOLEAN DEFAULT FALSE,\n" +
                "    ended               BOOLEAN \n" +
                ")");

        stmt.addBatch("CREATE TABLE series_tag (" +
                "  series integer NOT NULL," +
                "  tag integer NOT NULL," +
                "  count integer NOT NULL," +
                "  last_updated timestamp" +
                ")");


    }

    protected void setupInstrumentTables(Statement stmt) throws Exception {
        stmt.addBatch("CREATE TABLE instrument_type (\n" +
                "    id                  SERIAL, -- PK\n" +
                "    name                VARCHAR(255) NOT NULL,\n" +
                "    parent              INTEGER, \n" +
                "    child_order         INTEGER NOT NULL DEFAULT 0,\n" +
                "    description         TEXT\n" +
                ");");

        stmt.addBatch("CREATE TABLE instrument (\n" +
                "    id                  SERIAL, -- PK\n" +
                "    gid                 uuid NOT NULL,\n" +
                "    name                VARCHAR NOT NULL,\n" +
                "    type                INTEGER, -- references instrument_type.id\n" +
                "    edits_pending       INTEGER NOT NULL DEFAULT 0 ,\n" +
                "    last_updated        TIMESTAMP ,\n" +
                "    comment             VARCHAR(255) NOT NULL DEFAULT '',\n" +
                "    description         TEXT NOT NULL DEFAULT ''\n" +
                ");");


        stmt.addBatch("CREATE TABLE instrument_alias_type (\n" +
                "    id SERIAL, -- PK,\n" +
                "    name TEXT NOT NULL,\n" +
                "    parent              INTEGER, \n" +
                "    child_order         INTEGER NOT NULL DEFAULT 0,\n" +
                "    description         TEXT\n" +
                ");");

        stmt.addBatch("CREATE TABLE instrument_alias (\n" +
                "    id                  SERIAL, \n" +
                "    instrument          INTEGER NOT NULL, \n" +
                "    name                VARCHAR NOT NULL,\n" +
                "    locale              TEXT,\n" +
                "    edits_pending       INTEGER ,\n" +
                "    last_updated        TIMESTAMP  ,\n" +
                "    type                INTEGER, \n" +
                "    sort_name           VARCHAR ,\n" +
                "    begin_date_year     SMALLINT,\n" +
                "    begin_date_month    SMALLINT,\n" +
                "    begin_date_day      SMALLINT,\n" +
                "    end_date_year       SMALLINT,\n" +
                "    end_date_month      SMALLINT,\n" +
                "    end_date_day        SMALLINT,\n" +
                "    primary_for_locale  BOOLEAN,\n" +
                "    ended               BOOLEAN  \n" +
                ");");

        stmt.addBatch("CREATE TABLE instrument_tag (" +
                "  instrument integer NOT NULL," +
                "  tag integer NOT NULL," +
                "  count integer NOT NULL," +
                "  last_updated timestamp" +
                ")");


    }

    protected void setupEditorTables(Statement stmt) throws Exception {
        stmt.addBatch("CREATE TABLE editor\n" +
                "(\n" +
                "    id                  SERIAL,\n" +
                "    name                VARCHAR(64) NOT NULL,\n" +
                "    password            VARCHAR(64),\n" +
                "    privs               INTEGER ,\n" +
                "    email               VARCHAR(64) ,\n" +
                "    website             VARCHAR(255) ,\n" +
                "    bio                 TEXT,\n" +
                "    member_since        TIMESTAMP,\n" +
                "    email_confirm_date  TIMESTAMP,\n" +
                "    last_login_date     TIMESTAMP,\n" +
                "    edits_accepted      INTEGER DEFAULT 0,\n" +
                "    edits_rejected      INTEGER DEFAULT 0,\n" +
                "    auto_edits_accepted INTEGER DEFAULT 0,\n" +
                "    edits_failed        INTEGER DEFAULT 0,\n" +
                "    last_updated        TIMESTAMP,\n" +
                "    birth_date          DATE,\n" +
                "    gender              INTEGER,\n" +
                "    area                INTEGER\n" +
                ");\n");

    }

    protected void setupUrlTables(Statement stmt) throws Exception {

        stmt.addBatch("CREATE TABLE url" +
                "(" +
                "    id                INTEGER," +
                "    gid               UUID," +
                "    url               TEXT," +
                "    last_updated      TIMESTAMP" +
                ")");


        stmt.addBatch("CREATE TABLE l_area_url" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL DEFAULT 0," +
                "    last_updated        TIMESTAMP" +
                ");");

        stmt.addBatch("CREATE TABLE l_artist_url" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL DEFAULT 0," +
                "    last_updated        TIMESTAMP" +
                ");");

        stmt.addBatch("CREATE TABLE l_label_url" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL DEFAULT 0," +
                "    last_updated        TIMESTAMP" +
                ");");

        stmt.addBatch("CREATE TABLE l_place_url" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL ," +
                "    last_updated        TIMESTAMP" +
                ")");

        stmt.addBatch("CREATE TABLE l_release_url" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL DEFAULT 0," +
                "    last_updated        TIMESTAMP" +
                ");");

        stmt.addBatch("CREATE TABLE l_recording_url" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL DEFAULT 0," +
                "    last_updated        TIMESTAMP" +
                ");");

        stmt.addBatch("CREATE TABLE l_release_group_url" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL DEFAULT 0," +
                "    last_updated        TIMESTAMP" +
                ");");

        stmt.addBatch("CREATE TABLE l_url_url" +
                "(" +
                "    id                  SERIAL," +
                "    link                INTEGER NOT NULL," +
                "    entity0             INTEGER NOT NULL," +
                "    entity1             INTEGER NOT NULL," +
                "    edits_pending       INTEGER NOT NULL DEFAULT 0," +
                "    last_updated        TIMESTAMP" +
                ");");
    }


    protected void setupReplicationTables(Statement stmt) throws Exception {
        
        stmt.addBatch("CREATE TABLE replication_control (" +
                "  id serial NOT NULL," +
                "  current_schema_sequence integer NOT NULL," +
                "  current_replication_sequence integer, " +
                "  last_replication_date timestamp " +
                ")");

        stmt.addBatch("CREATE TABLE dbmirror_pending (" +
                "  seqid serial NOT NULL," +
                "  tablename integer NOT NULL," +
                "  op character(1) ," +
                "  xid integer NOT NULL DEFAULT 0" +
                ")");

        stmt.addBatch("CREATE TABLE dbmirror_pendingdata (" +
                "  seqid serial NOT NULL," +
                "  iskey boolean NOT NULL," +                
                "  data character varying" +
                ")");

    }


    protected void insertReferenceData(Statement stmt) throws Exception {

        stmt.addBatch("INSERT INTO gender (id, name) VALUES " + 
                "(1, 'Male'), " +
                "(2, 'Female') "
        );

        stmt.addBatch("INSERT INTO artist_type (id, name) VALUES " + 
                "(1, 'Person'), " +
                "(2, 'Group') "
        );

        stmt.addBatch("INSERT INTO label_type (id, name) VALUES " + 
                "(1, 'Distributor'), " +
                "(2, 'Holding'), " +
                "(3, 'Production'), " +
                "(4, 'Original Production'), " +
                "(5, 'Bootleg Production'), " +
                "(6, 'Reissue Production'), " +
                "(7, 'Publisher') "
        );

        stmt.addBatch("INSERT INTO release_status (id, name) VALUES " + 
                "(1, 'Official'), " +
                "(2, 'Promotion'), " +
                "(3, 'Bootleg'), " +
                "(4, 'Pseudo-Release') "
        );

        stmt.addBatch("INSERT INTO release_packaging (id, name) VALUES " + 
                "(1, 'Jewel Case'), " +
                "(2, 'Slim Jewel Case'), " +
                "(3, 'Digipak'), " +
                "(4, 'Paper Sleeve'), " +
                "(5, 'Other') "
        );

        stmt.addBatch("INSERT INTO release_group_primary_type (id, name) VALUES " +
                "(1, 'Album'), " +
                "(2, 'Single'), " +
                "(3, 'EP'), " +
                "(4, 'Other'), " +
                "(5, 'Audiobook') "
        );

        stmt.addBatch("INSERT INTO release_group_secondary_type (id, name) VALUES " +
                "(1, 'Compilation'), " +
                "(2, 'Interview'), " +
                "(3, 'Live'), " +
                "(4, 'Remix'), " +
                "(5, 'Soundtrack'), " +
                "(6, 'Spokenword') "

        );

        stmt.addBatch("INSERT INTO medium_format (id, name, year) VALUES " + 
                "(1, 'CD', 1982), " +
                "(2, 'DVD', 1995), " +
                "(3, 'SACD', 1999), " +
                "(4, 'DualDisc', 2004), " +
                "(5, 'LaserDisc', 1978), " +
                "(6, 'MiniDisc', 1992), " +
                "(7, 'Vinyl', 1895), " +
                "(8, 'Cassette', 1964), " +
                "(9, 'Cartridge', 1962), " +
                "(10, 'Reel-to-reel', 1935), " +
                "(11, 'DAT', 1976), " +
                "(12, 'Digital Media', NULL), " +
                "(13, 'Other', NULL), " +
                "(14, 'Wax Cylinder', 1877), " +
                "(15, 'Piano Roll', 1883), " +
                "(16, 'DCC', 1992) "
        );

        stmt.addBatch("INSERT INTO artist_alias_type (id, name) VALUES (1, 'Artist name')");
        stmt.addBatch("INSERT INTO artist_alias_type (id, name) VALUES (2, 'Legal name')");
        stmt.addBatch("INSERT INTO artist_alias_type (id, name) VALUES (3, 'Search hint')");

        stmt.addBatch("INSERT INTO label_alias_type (id, name) VALUES (1, 'Search hint')");

        stmt.addBatch("INSERT INTO work_alias_type (id, name) VALUES (1, 'Search hint')");

        stmt.addBatch("INSERT INTO area_alias_type (id, name) VALUES (1, 'AliasType')");

    }
    
    protected void insertReplicationInfo(Statement stmt) throws Exception {

    	stmt.addBatch("INSERT INTO replication_control (id, current_schema_sequence, current_replication_sequence) " +
        		" VALUES (1, 12, 42459)");
    }


}
