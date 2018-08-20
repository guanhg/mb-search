package org.musicbrainz.search.servlet;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Before;
import org.musicbrainz.mmd2.Metadata;

import javax.xml.bind.JAXBContext;
import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Abstract Integration test for  searching against real indexes using latest search code in jetty
 *
 * Assumes that the web.xml is configured to use indexes compatible with the search code so when code changes
 * have been made that necessitate real indexes would need to do the following
 *
 * mvn package
 * Rebuild indexes
 * mvn integration-test to run tests, if test fail get an error but build doesn't currently fall over to
 * allow for the fact that in some environments it may be difficult to setup up integration tests
 *
 */
public class AbstractIntegration  {

    protected JAXBContext context;
    protected JAXBContext contextV1;
   
    @Before
    public void setUp() throws Exception {
        context     = JAXBContext.newInstance("org.musicbrainz.mmd2");
        contextV1   = JAXBContext.newInstance("com.jthink.brainz.mmd");
    }

    public Metadata doSearch(String searchUrl) throws Exception {

        StopWatch clock = new StopWatch();
        clock.start();

        BufferedInputStream bis;
        URL url = new URL(searchUrl);
        HttpURLConnection uc = (HttpURLConnection)url.openConnection();
        int responseCode = uc.getResponseCode();
        assertEquals(responseCode,HttpURLConnection.HTTP_OK);

        if("gzip".equals(uc.getContentEncoding())) {
            bis = new BufferedInputStream(new GZIPInputStream(uc.getInputStream()));
        }
        else {
            bis = new BufferedInputStream(uc.getInputStream());
        }

        Metadata metadata = (Metadata) context.createUnmarshaller().unmarshal(bis);

        clock.stop();
        System.out.println(getClass()+":"+clock.getTime()+" ms");
        assertTrue(clock.getTime() < 5000);

        return metadata;
    }

    public com.jthink.brainz.mmd.Metadata doSearchV1(String searchUrl) throws Exception {

        StopWatch clock = new StopWatch();
        clock.start();

        BufferedInputStream bis;
        URL url = new URL(searchUrl);
        HttpURLConnection uc = (HttpURLConnection)url.openConnection();
        int responseCode = uc.getResponseCode();
        assertEquals(responseCode,HttpURLConnection.HTTP_OK);

        if("gzip".equals(uc.getContentEncoding())) {
            bis = new BufferedInputStream(new GZIPInputStream(uc.getInputStream()));
        }
        else {
            bis = new BufferedInputStream(uc.getInputStream());
        }

        com.jthink.brainz.mmd.Metadata metadata = (com.jthink.brainz.mmd.Metadata) contextV1.createUnmarshaller().unmarshal(bis);

        clock.stop();
        System.out.println(getClass()+":"+clock.getTime()+" ms");
        assertTrue(clock.getTime() < 5000);

        return metadata;
    }
}
