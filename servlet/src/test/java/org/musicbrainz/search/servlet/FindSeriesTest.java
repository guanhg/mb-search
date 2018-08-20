package org.musicbrainz.search.servlet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Before;
import org.junit.Test;
import org.musicbrainz.mmd2.*;
import org.musicbrainz.search.LuceneVersion;
import org.musicbrainz.search.MbDocument;
import org.musicbrainz.search.analysis.MusicbrainzSimilarity;
import org.musicbrainz.search.index.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Assumes an index has been built stored and in the data folder, I've picked a fairly obscure bside so hopefully
 * will not get added to another release
 */
public class FindSeriesTest {

    private AbstractSearchServer ss;
    private AbstractDismaxSearchServer sd;


    @Before
    public void setUp() throws Exception {
        ObjectFactory of = new ObjectFactory();
        RAMDirectory ramDir = new RAMDirectory();
        Analyzer analyzer = DatabaseIndex.getAnalyzer(SeriesIndexField.class);
        IndexWriterConfig writerConfig = new IndexWriterConfig(LuceneVersion.LUCENE_VERSION, analyzer);
        writerConfig.setSimilarity(new MusicbrainzSimilarity());
        IndexWriter writer = new IndexWriter(ramDir, writerConfig);

        {
            MbDocument doc = new MbDocument();

            Series series = of.createSeries();
            doc.addField(SeriesIndexField.SERIES_ID, "ff571ff4-04cb-4b9c-8a1c-354c330f863c");
            series.setId("ff571ff4-04cb-4b9c-8a1c-354c330f863c");

            doc.addField(SeriesIndexField.SERIES, "Now thats what I call music !");
            series.setName("Now thats what I call music !");

            doc.addField(SeriesIndexField.ALIAS, "UK Top 40 Series");
            AliasList aliasList = of.createAliasList();
            Alias alias = of.createAlias();
            aliasList.getAlias().add(alias);
            alias.setContent("UK Top 40 Series");
            series.setAliasList(aliasList);

            doc.addField(SeriesIndexField.TYPE, "Various");
            series.setType("Various");

            doc.addField(LabelIndexField.TAG, "desert");
            TagList tagList = of.createTagList();
            Tag tag = of.createTag();
            tag.setName("desert");
            tag.setCount(BigInteger.valueOf(22));
            tagList.getTag().add(tag);
            series.setTagList(tagList);

            doc.addField(SeriesIndexField.SERIES_STORE, MMDSerializer.serialize(series));
            writer.addDocument(doc.getLuceneDocument());
        }


        {
            MbDocument doc = new MbDocument();
            Series series = of.createSeries();
            doc.addField(MetaIndexField.META, MetaIndexField.META_VALUE);
            doc.addNumericField(MetaIndexField.LAST_UPDATED, new Date().getTime());
            doc.addField(LabelIndexField.LABEL_STORE, MMDSerializer.serialize(series));
            writer.addDocument(doc.getLuceneDocument());
        }

        writer.close();
        SearcherManager searcherManager = new SearcherManager(ramDir, new MusicBrainzSearcherFactory(ResourceType.SERIES));
        ss = new SeriesSearch(searcherManager);
        sd = new SeriesDismaxSearch(ss);
    }

    @Test
    public void testFindSeriesById() throws Exception {
        Results res = ss.search("sid:\"ff571ff4-04cb-4b9c-8a1c-354c330f863c\"", 0, 10);
        assertEquals(1, res.getTotalHits());
        Result result = res.results.get(0);
        MbDocument doc = result.getDoc();
        assertEquals("ff571ff4-04cb-4b9c-8a1c-354c330f863c", doc.get(SeriesIndexField.SERIES_ID));
        assertEquals("Now thats what I call music !", doc.get(SeriesIndexField.SERIES));
    }

    @Test
    public void testFindSeriesByName() throws Exception {
        Results res = ss.search("series:\"Now thats what I call music !\"", 0, 10);
        assertEquals(1, res.getTotalHits());
        Result result = res.results.get(0);
        MbDocument doc = result.getDoc();
        assertEquals("ff571ff4-04cb-4b9c-8a1c-354c330f863c", doc.get(SeriesIndexField.SERIES_ID));
        assertEquals("Now thats what I call music !", doc.get(SeriesIndexField.SERIES));
    }


    @Test
    public void testFindSeriesByDismax1() throws Exception {
        Results res = sd.search("Now thats what I call music !", 0, 10);
        assertEquals(1, res.getTotalHits());
        Result result = res.results.get(0);
        MbDocument doc = result.getDoc();
        assertEquals("ff571ff4-04cb-4b9c-8a1c-354c330f863c", doc.get(SeriesIndexField.SERIES_ID));
        assertEquals("Now thats what I call music !", doc.get(SeriesIndexField.SERIES));
    }


    @Test
    public void testFindSeriesByDefault() throws Exception {

        {
            Results res = ss.search("\"Now thats what I call music !\"", 0, 10);
            assertEquals(1, res.getTotalHits());
            Result result = res.results.get(0);
            MbDocument doc = result.getDoc();
            assertEquals("ff571ff4-04cb-4b9c-8a1c-354c330f863c", doc.get(SeriesIndexField.SERIES_ID));
            assertEquals("Now thats what I call music !", doc.get(SeriesIndexField.SERIES));
        }

    }

    @Test
    public void testFindSeriesByType() throws Exception {
        Results res = ss.search("type:\"Various\"", 0, 10);
        assertEquals(1, res.getTotalHits());
        Result result = res.results.get(0);
        MbDocument doc = result.getDoc();

        //(This will always come first because searcher sots by score and then docno, and this doc added first)
        assertEquals("ff571ff4-04cb-4b9c-8a1c-354c330f863c", doc.get(SeriesIndexField.SERIES_ID));
        assertEquals("Now thats what I call music !", doc.get(SeriesIndexField.SERIES));
    }

    @Test
    public void testFindSeriesByTag() throws Exception {
        Results res = ss.search("tag:desert", 0, 10);
        assertEquals(1, res.getTotalHits());
        Result result = res.results.get(0);
        MbDocument doc = result.getDoc();
        assertEquals("ff571ff4-04cb-4b9c-8a1c-354c330f863c", doc.get(SeriesIndexField.SERIES_ID));
        assertEquals("Now thats what I call music !", doc.get(SeriesIndexField.SERIES));
    }

    /**
     * @throws Exception exception
     */
    @Test
    public void testOutputJson() throws Exception {

        Results res = ss.search("series:\"Now thats what I call music !\"", 0, 10);
        org.musicbrainz.search.servlet.mmd2.ResultsWriter writer = ss.getMmd2Writer();
        StringWriter sw = new StringWriter();
        PrintWriter pr = new PrintWriter(sw);
        writer.write(pr, res, SearchServerServlet.RESPONSE_JSON);
        pr.close();

        String output = sw.toString();
        System.out.println("Json is" + output);

        assertTrue(output.contains("id\":\"ff571ff4-04cb-4b9c-8a1c-354c330f863c\""));
        assertTrue(output.contains("\"series-list\""));
        assertTrue(output.contains("\"count\":1"));
        assertTrue(output.contains("\"offset\":0,"));
        assertTrue(output.contains("\"type\":\"Various\""));
        assertTrue(output.contains("name\":\"Now thats what I call music !\""));
    }

    /**
     * @throws Exception exception
     */
    @Test
    public void testOutputJsonNew() throws Exception {

        Results res = ss.search("series:\"Now thats what I call music !\"", 0, 10);
        org.musicbrainz.search.servlet.mmd2.ResultsWriter writer = ss.getMmd2Writer();
        StringWriter sw = new StringWriter();
        PrintWriter pr = new PrintWriter(sw);
        writer.write(pr, res, SearchServerServlet.RESPONSE_JSON_NEW);
        pr.close();

        String output = sw.toString();
        System.out.println("Json New is" + output);
        assertTrue(output.contains("series"));
        assertTrue(output.contains("id\":\"ff571ff4-04cb-4b9c-8a1c-354c330f863c\""));
        assertTrue(output.contains("\"series\""));
        assertTrue(output.contains("\"type\":\"Various\""));
        assertTrue(output.contains("name\":\"Now thats what I call music !\""));
        assertTrue(output.contains("\"aliases\":[{\"name\":\"UK Top 40 Series\",\"locale\":null,\"type\":null,\"primary\":null,\"begin-date\":null,\"end-date\":null}]"));
        assertTrue(output.contains("\"count\":1"));
        assertTrue(output.contains("\"offset\":0,"));
        assertTrue(output.contains("\"tags\":[{\"count\":22,\"name\":\"desert\""));

    }

    /**
     * @throws Exception exception
     */
    @Test
    public void testOutputJsonNewIdent() throws Exception {

        Results res = ss.search("series:\"Now thats what I call music !\"", 0, 10);
        org.musicbrainz.search.servlet.mmd2.ResultsWriter writer = ss.getMmd2Writer();
        StringWriter sw = new StringWriter();
        PrintWriter pr = new PrintWriter(sw);
        writer.write(pr, res, SearchServerServlet.RESPONSE_JSON_NEW, true);
        pr.close();

        String output = sw.toString();
        System.out.println("Json New Ident is" + output);
        assertTrue(output.contains("\"offset\" : 0"));

    }
}