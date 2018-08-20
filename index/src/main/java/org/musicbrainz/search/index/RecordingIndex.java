/*
 * MusicBrainz Search Server
 * Copyright (C) 2009  Lukas Lalinsky

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

import com.google.common.base.Strings;
import org.apache.commons.lang.time.StopWatch;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.similarities.Similarity;
import org.musicbrainz.mmd2.*;
import org.musicbrainz.search.MbDocument;
import org.musicbrainz.search.analysis.RecordingSimilarity;
import org.musicbrainz.search.helper.*;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;

public class RecordingIndex extends DatabaseIndex {

    private static final String VARIOUS_ARTISTS_GUID = "89ad4ac3-39f7-470e-963a-56509c546377";
    private static final String VARIOUS_ARTISTS_NAME = "Various Artists";

    private static final int VARIOUS_ARTIST_CREDIT_ID = 1;

    public static final String INDEX_NAME = "recording";

    private StopWatch trackClock = new StopWatch();
    private StopWatch isrcClock = new StopWatch();
    private StopWatch artistClock = new StopWatch();
    private StopWatch trackArtistClock = new StopWatch();
    private StopWatch releaseClock = new StopWatch();
    private StopWatch recordingClock = new StopWatch();
    private StopWatch buildClock = new StopWatch();
    private StopWatch storeClock = new StopWatch();


    private final static int QUANTIZED_DURATION = 2000;

    public RecordingIndex(Connection dbConnection) {
        super(dbConnection);
        trackClock.start();
        isrcClock.start();
        artistClock.start();
        trackArtistClock.start();
        releaseClock.start();
        recordingClock.start();
        buildClock.start();
        storeClock.start();
        trackClock.suspend();
        isrcClock.suspend();
        artistClock.suspend();
        releaseClock.suspend();
        recordingClock.suspend();
        trackArtistClock.suspend();
        buildClock.suspend();
        storeClock.suspend();
    }

    public RecordingIndex() {
    }

    public String getName() {
        return RecordingIndex.INDEX_NAME;
    }

    public Analyzer getAnalyzer() {
        return DatabaseIndex.getAnalyzer(RecordingIndexField.class);
    }

    @Override
    public IndexField getIdentifierField() {
        return RecordingIndexField.ID;
    }

    public int getMaxId() throws SQLException {
        Statement st = dbConnection.createStatement();
        ResultSet rs = st.executeQuery("SELECT MAX(id) FROM recording");
        rs.next();
        return rs.getInt(1);
    }

    public int getNoOfRows(int maxId) throws SQLException {
        Statement st = dbConnection.createStatement();
        ResultSet rs = st.executeQuery("SELECT count(*) FROM recording WHERE id<=" + maxId);
        rs.next();
        return rs.getInt(1);
    }

    String releases;
    String releaseArtistCredits;
    String releaseEvents;
    String releaseSecondaryTypes;

    @Override
    public Similarity getSimilarity() {
        return new RecordingSimilarity();
    }

    public void init(IndexWriter indexWriter, boolean isUpdater) throws SQLException {

        if (!isUpdater) {
            addPreparedStatement("TRACKS",
                    "SELECT id, gid, track_name, length as duration, recording, track_position, track_number, track_count, " +
                            "  release_id, medium_position, format " +
                            " FROM tmp_track " +
                            " WHERE recording between ? AND ?");
        } else {
            addPreparedStatement("TRACKS",
                    "SELECT t.id, t.gid, t.name as track_name, t.length as duration, t.recording, t.position as track_position, t.number as track_number, m.track_count, " +
                            "  m.release as release_id, m.position as medium_position,mf.name as format " +
                            " FROM track t " +
                            "  INNER JOIN medium m ON t.medium=m.id " +
                            "  LEFT JOIN  medium_format mf ON m.format=mf.id " +
                            " WHERE t.recording BETWEEN ? AND ?"
            );
        }

        addPreparedStatement("TAGS", TagHelper.constructTagQuery("recording_tag", "recording"));

        addPreparedStatement("ISRCS",
                "SELECT recording as recordingId, isrc " +
                        " FROM isrc " +
                        " WHERE recording BETWEEN ? AND ?  " +
                        " ORDER BY recording, id");

        addPreparedStatement("ARTISTCREDITS",
                "SELECT r.id as recordingId, " +
                        "  a.artist_credit, " +
                        "  a.pos, " +
                        "  a.joinphrase, " +
                        "  a.artistId,  " +
                        "  a.comment, " +
                        "  a.artistName, " +
                        "  a.artistCreditName, " +
                        "  a.artistSortName " +
                        " FROM recording AS r " +
                        "  INNER JOIN tmp_artistcredit a ON r.artist_credit=a.artist_credit " +
                        " WHERE r.id BETWEEN ? AND ?  " +
                        " ORDER BY r.id, a.pos");

        addPreparedStatement("ARTISTCREDITALIASES",
                "SELECT r.id as recordingId," +
                        " a.artist_credit, " +
                        " a.pos, " +
                        " aa.name," +
                        " aa.sort_name," +
                        " aa.primary_for_locale," +
                        " aa.locale," +
                        " aa.begin_date_year," +
                        " aa.begin_date_month," +
                        " aa.begin_date_day," +
                        " aa.end_date_year," +
                        " aa.end_date_month," +
                        " aa.end_date_day," +
                        " att.name as type" +
                        " FROM recording AS r " +
                        "  INNER JOIN tmp_artistcredit a ON r.artist_credit=a.artist_credit " +
                        "  INNER JOIN artist_alias aa ON a.id=aa.artist" +
                        "  LEFT  JOIN artist_alias_type att on (aa.type=att.id)" +
                        " WHERE r.id BETWEEN ? AND ?  " +
                        " AND a.artistId!='" + ArtistIndex.VARIOUS_ARTIST_MBID +"'" +
                        " AND a.artistId!='" + ArtistIndex.UNKNOWN_ARTIST_MBID  +"'" +
                        " ORDER BY r.id, a.pos, aa.name");


        addPreparedStatement("TRACKARTISTCREDITS",
                "SELECT t.id as id, " +
                        "  a.artist_credit, " +
                        "  a.pos, " +
                        "  a.joinphrase, " +
                        "  a.artistId,  " +
                        "  a.comment, " +
                        "  a.artistName, " +
                        "  a.artistCreditName, " +
                        "  a.artistSortName " +
                        " FROM track AS t " +
                        "  INNER JOIN tmp_artistcredit a ON t.artist_credit=a.artist_credit " +
                        " WHERE t.recording BETWEEN ? AND ?  " +
                        " ORDER BY t.recording, a.pos");

        addPreparedStatement("TRACKARTISTCREDITALIASES",
                "SELECT r.id as recordingId," +
                        " a.artist_credit, " +
                        " a.pos, " +
                        " aa.name," +
                        " aa.sort_name," +
                        " aa.primary_for_locale," +
                        " aa.locale," +
                        " aa.begin_date_year," +
                        " aa.begin_date_month," +
                        " aa.begin_date_day," +
                        " aa.end_date_year," +
                        " aa.end_date_month," +
                        " aa.end_date_day," +
                        " att.name as type" +
                        " FROM track AS r " +
                        "  INNER JOIN tmp_artistcredit a ON r.artist_credit=a.artist_credit " +
                        "  INNER JOIN artist_alias aa ON a.id=aa.artist" +
                        "  LEFT  JOIN artist_alias_type att on (aa.type=att.id)" +
                        " WHERE r.recording BETWEEN ? AND ?  " +
                        " AND a.artistId!='" + ArtistIndex.VARIOUS_ARTIST_MBID +"'" +
                        " AND a.artistId!='" + ArtistIndex.UNKNOWN_ARTIST_MBID  +"'" +
                        " ORDER BY r.id, a.pos, aa.name");

        releaseArtistCredits =
                "SELECT r.id as releaseKey, " +
                        "  a.artist_credit, " +
                        "  a.pos, " +
                        "  a.joinphrase, " +
                        "  a.artistId,  " +
                        "  a.comment, " +
                        "  a.artistName, " +
                        "  a.artistCreditName, " +
                        "  a.artistSortName " +
                        " FROM release AS r " +
                        "  INNER JOIN tmp_artistcredit a ON r.artist_credit=a.artist_credit " +
                        " WHERE a.artistId!='" + ArtistIndex.VARIOUS_ARTIST_MBID +"'" +
                        " AND r.id in ";

        releases =
                "SELECT " +
                        "  id as releaseKey, gid as releaseid, name as releasename, type, " +
                        "  status, tracks,artist_credit, rg_gid " +
                        " FROM tmp_release r1 " +
                        " WHERE r1.id in ";

        releaseEvents =
                " SELECT release, country, " +
                        "   date_year, date_month, date_day, name, gid"+
                        " FROM tmp_release_event r " +
                        " WHERE r.release in ";

        releaseSecondaryTypes =
                "SELECT rg.name as type, r.id as releaseKey" +
                        " FROM tmp_release r " +
                        " INNER JOIN release_group_secondary_type_join  rgj " +
                        " ON r.rg_id=rgj.release_group " +
                        " INNER JOIN release_group_secondary_type rg  " +
                        " ON rgj.secondary_type = rg.id " +
                        " WHERE r.id in ";

        addPreparedStatement("RECORDINGS",
                "SELECT re.id as recordingId, re.gid as trackid, re.length as duration, re.name as trackname, re.comment, re.video " +
                        " FROM recording re " +
                        " WHERE re.id BETWEEN ? AND ?");
    }

    public void destroy() throws SQLException {

        super.destroy();
        System.out.println(this.getName() + ":Isrcs Queries " + Utils.formatClock(isrcClock));
        System.out.println(this.getName() + ":Track Queries " + Utils.formatClock(trackClock));
        System.out.println(this.getName() + ":Artists Queries " + Utils.formatClock(artistClock));
        System.out.println(this.getName() + ":Track Artists Queries " + Utils.formatClock(trackArtistClock));
        System.out.println(this.getName() + ":Releases Queries " + Utils.formatClock(releaseClock));
        System.out.println(this.getName() + ":Recording Queries " + Utils.formatClock(recordingClock));
        System.out.println(this.getName() + ":Build Index " + Utils.formatClock(buildClock));
        System.out.println(this.getName() + ":Build Store " + Utils.formatClock(storeClock));

    }

    /**
     * Get tag information
     *
     * @param min min recording id
     * @param max max recording id
     * @return A map of matches
     * @throws SQLException
     * @throws IOException
     */
    private Map<Integer, List<Tag>> loadTags(int min, int max) throws SQLException, IOException {

        PreparedStatement st = getPreparedStatement("TAGS");
        st.setInt(1, min);
        st.setInt(2, max);
        ResultSet rs = st.executeQuery();
        Map<Integer, List<Tag>> tags = TagHelper.completeTagsFromDbResults(rs, "recording");
        rs.close();
        return tags;
    }

    /**
     * Get ISRC Information for the recordings
     *
     * @param min min recording id
     * @param max max recording id
     * @return map of matches
     * @throws SQLException
     * @throws IOException
     */
    private Map<Integer, List<String>> loadISRCs(int min, int max) throws SQLException, IOException {

        //ISRC
        isrcClock.resume();
        Map<Integer, List<String>> isrcWrapper = new HashMap<Integer, List<String>>();
        PreparedStatement st = getPreparedStatement("ISRCS");
        st.setInt(1, min);
        st.setInt(2, max);
        ResultSet rs = st.executeQuery();
        while (rs.next()) {
            int recordingId = rs.getInt("recordingId");
            List<String> list;
            if (!isrcWrapper.containsKey(recordingId)) {
                list = new LinkedList<String>();
                isrcWrapper.put(recordingId, list);
            } else {
                list = isrcWrapper.get(recordingId);
            }
            String isrc = new String(rs.getString("isrc"));
            list.add(isrc);
        }
        rs.close();
        isrcClock.suspend();
        return isrcWrapper;
    }

    /**
     * Get Recording Artist Credit
     *
     * @param min min recording id
     * @param max max recording id
     * @return A map of matches
     * @throws SQLException if sql problem
     * @throws IOException  if io exception
     */
    private Map<Integer, ArtistCreditWrapper> loadArtists(int min, int max) throws SQLException, IOException {

        //Artists
        artistClock.resume();
        PreparedStatement st = getPreparedStatement("ARTISTCREDITS");
        st.setInt(1, min);
        st.setInt(2, max);
        ResultSet rs = st.executeQuery();
        Map<Integer, ArtistCreditWrapper> artistCredits
                = ArtistCreditHelper.completeArtistCreditFromDbResults(rs, "recordingId", "artist_Credit", "artistId", "artistName", "artistSortName", "comment", "joinphrase", "artistCreditName");
        rs.close();
        artistClock.suspend();
        return artistCredits;
    }

    private Map<Integer, ArtistCreditWrapper> updateArtistCreditWithAliases(
            Map<Integer, ArtistCreditWrapper> artistCredits,
            int min,
            int max)
            throws SQLException, IOException {

        //Artist Credit Aliases
        PreparedStatement st = getPreparedStatement("ARTISTCREDITALIASES");
        st.setInt(1, min);
        st.setInt(2, max);
        ResultSet rs = st.executeQuery();
        return ArtistCreditHelper.updateArtistCreditWithAliases(artistCredits,"recordingId", rs);
    }

    private Map<Integer, ArtistCreditWrapper> updateTrackArtistCreditWithAliases(
            Map<Integer, ArtistCreditWrapper> artistCredits,
            int min,
            int max)
            throws SQLException, IOException {

        //Artist Credit Aliases
        PreparedStatement st = getPreparedStatement("TRACKARTISTCREDITALIASES");
        st.setInt(1, min);
        st.setInt(2, max);
        ResultSet rs = st.executeQuery();
        return ArtistCreditHelper.updateArtistCreditWithAliases(artistCredits,"recordingId", rs);
    }

    /**
     * Get Track Artist Credit
     *
     * @param min min recording id
     * @param max max recording id
     * @return A map of matches
     * @throws SQLException if sql problem
     * @throws IOException  if io exception
     */
    private Map<Integer, ArtistCreditWrapper> loadTrackArtists(int min, int max) throws SQLException, IOException {

        //Artists
        trackArtistClock.resume();
        PreparedStatement st = getPreparedStatement("TRACKARTISTCREDITS");
        st.setInt(1, min);
        st.setInt(2, max);
        ResultSet rs = st.executeQuery();
        Map<Integer, ArtistCreditWrapper> artistCredits
                = ArtistCreditHelper.completeArtistCreditFromDbResults
                (rs,
                        "id",
                        "artist_Credit",
                        "artistId",
                        "artistName",
                        "artistSortName",
                        "comment",
                        "joinphrase",
                        "artistCreditName"
                );
        rs.close();
        trackArtistClock.suspend();
        return artistCredits;
    }

    /**
     * Get Release Artist Credit
     *
     * @param min min recording id
     * @param max max recording id
     * @return A map of matches
     * @throws SQLException if sql problem
     * @throws IOException  if io exception
     */
    private Map<Integer, ArtistCreditWrapper> loadReleaseArtists(Map<Integer, Release> releases,int min, int max) throws SQLException, IOException {

        //Add release artists
        PreparedStatement stmt = createReleaseArtistCreditsStatement(releases.size());
        int count = 1;
        for (Integer key : releases.keySet()) {
            stmt.setInt(count, key);
            count++;
        }

        ResultSet rs = stmt.executeQuery();
        Map<Integer, ArtistCreditWrapper> releaseArtistCredits
                = ArtistCreditHelper.completeArtistCreditFromDbResults
                (rs,
                        "releaseKey",
                        "artist_Credit",
                        "artistId",
                        "artistName",
                        "artistSortName",
                        "comment",
                        "joinphrase",
                        "artistCreditName"
                );
        rs.close();
        return  releaseArtistCredits;
    }

    /**
     * Get track  information for recordings
     * <p/>
     * One recording can be linked to by multiple tracks
     *
     * @param min
     * @param max
     * @return A map of matches
     * @throws SQLException
     * @throws IOException
     */
    private Map<Integer, List<TrackWrapper>> loadTracks(int min, int max) throws SQLException, IOException {

        //Tracks and Release Info
        trackClock.resume();
        Map<Integer, List<TrackWrapper>> tracks = new HashMap<Integer, List<TrackWrapper>>();
        PreparedStatement st = getPreparedStatement("TRACKS");
        st.setInt(1, min);
        st.setInt(2, max);
        ResultSet rs = st.executeQuery();
        while (rs.next()) {
            int recordingId = rs.getInt("recording");
            List<TrackWrapper> list;
            if (!tracks.containsKey(recordingId)) {
                list = new LinkedList<TrackWrapper>();
                tracks.put(recordingId, list);
            } else {
                list = tracks.get(recordingId);
            }
            TrackWrapper tw = new TrackWrapper();
            tw.setTrackId(rs.getInt("id"));
            tw.setTrackGuid(rs.getString("gid"));
            tw.setReleaseId(rs.getInt("release_id"));
            tw.setTrackCount(rs.getInt("track_count"));
            tw.setTrackPosition(rs.getInt("track_position"));
            tw.setTrackName(rs.getString("track_name"));
            tw.setMediumPosition(rs.getInt("medium_position"));
            tw.setMediumFormat(rs.getString("format"));
            tw.setDuration(rs.getInt("duration"));
            tw.setTrackNumber(rs.getString("track_number"));
            list.add(tw);
        }
        rs.close();
        trackClock.suspend();
        return tracks;
    }

    /**
     * Create the release statement
     *
     * @param noOfElements
     * @return
     * @throws SQLException
     */
    private PreparedStatement createReleaseStatement(int noOfElements) throws SQLException {
        StringBuilder inClause = new StringBuilder();
        boolean firstValue = true;
        for (int i = 0; i < noOfElements; i++) {
            if (firstValue) {
                firstValue = false;
            } else {
                inClause.append(',');
            }
            inClause.append('?');
        }
        PreparedStatement stmt = dbConnection.prepareStatement(
                releases + "(" + inClause.toString() + ')');
        return stmt;

    }

    private PreparedStatement createReleaseEventStatement(int noOfElements) throws SQLException {
        StringBuilder inClause = new StringBuilder();
        boolean firstValue = true;
        for (int i = 0; i < noOfElements; i++) {
            if (firstValue) {
                firstValue = false;
            } else {
                inClause.append(',');
            }
            inClause.append('?');
        }
        PreparedStatement stmt = dbConnection.prepareStatement(
                releaseEvents + "(" + inClause.toString() + ')');
        return stmt;

    }

    /**
     * Create the release secondary types statement
     *
     * @param noOfElements
     * @return
     * @throws SQLException
     */
    private PreparedStatement createReleaseSecondaryTypesStatement(int noOfElements) throws SQLException {
        StringBuilder inClause = new StringBuilder();
        boolean firstValue = true;
        for (int i = 0; i < noOfElements; i++) {
            if (firstValue) {
                firstValue = false;
            } else {
                inClause.append(',');
            }
            inClause.append('?');
        }
        PreparedStatement stmt = dbConnection.prepareStatement(
                releaseSecondaryTypes + "(" + inClause.toString() + ')');
        return stmt;

    }

    /**
     * Create the release artist credits statement
     *
     * @param noOfElements
     * @return
     * @throws SQLException
     */
    private PreparedStatement createReleaseArtistCreditsStatement(int noOfElements) throws SQLException {
        StringBuilder inClause = new StringBuilder();
        boolean firstValue = true;
        for (int i = 0; i < noOfElements; i++) {
            if (firstValue) {
                firstValue = false;
            } else {
                inClause.append(',');
            }
            inClause.append('?');
        }
        PreparedStatement stmt = dbConnection.prepareStatement(
                releaseArtistCredits + "(" + inClause.toString() + ')');
        return stmt;

    }


    /**
     * Get release information for recordings
     *
     * @param tracks
     * @return A map of matches
     * @throws SQLException
     * @throws IOException
     */
    private Map<Integer, Release> loadReleases
    (Map<Integer, List<TrackWrapper>> tracks) throws SQLException, IOException {


        Map<Integer, Release> releases = new HashMap<Integer, Release>();
        ObjectFactory of = new ObjectFactory();

        try {
            releaseClock.resume();
        } catch (IllegalStateException e) {
            System.out.println("Warning: IllegalStateException during StopWatch.resume");
        }

        // Add all the releaseKeys to a set to prevent duplicates
        Set<Integer> releaseKeys = new HashSet<Integer>();
        for (List<TrackWrapper> recording : tracks.values()) {
            for (TrackWrapper track : recording) {
                releaseKeys.add(track.getReleaseId());
            }
        }

        if (releaseKeys.isEmpty()) {
            return releases;
        }

        PreparedStatement stmt = createReleaseStatement(releaseKeys.size());
        int count = 1;
        for (Integer key : releaseKeys) {
            stmt.setInt(count, key);
            count++;
        }

        Release release;
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            int releaseKey = rs.getInt("releaseKey");
            if (!releases.containsKey(releaseKey)) {
                release = of.createRelease();
                releases.put(releaseKey, release);
            } else {
                release = releases.get(releaseKey);
            }

            MediumList ml = of.createMediumList();
            ReleaseGroup rg = of.createReleaseGroup();
            release.setId(rs.getString("releaseId"));
            release.setTitle(rs.getString("releasename"));
            PrimaryType pt = new PrimaryType();
            pt.setContent(rs.getString("type"));
            rg.setPrimaryType(pt);
            rg.setId(rs.getString("rg_gid"));
            release.setReleaseGroup(rg);
            Status status = new Status();
            status.setContent(rs.getString("status"));
            release.setStatus(status);
            ml.setTrackCount(BigInteger.valueOf(rs.getInt("tracks")));
            release.setReleaseGroup(rg);
            release.setMediumList(ml);
            //Manually create various artist artists they are not retrieved from the database
            //for performance reasons
            if (rs.getInt("artist_credit") == VARIOUS_ARTIST_CREDIT_ID) {
                ArtistCredit ac = createVariousArtistsCredit();
                release.setArtistCredit(ac);
            }
        }
        rs.close();


        //Add ReleaseEvents for each Release
        stmt = createReleaseEventStatement(releaseKeys.size());
        count = 1;
        for (Integer key : releaseKeys) {
            stmt.setInt(count, key);
            count++;
        }
        rs = stmt.executeQuery();
        while (rs.next()) {
            int releaseKey = rs.getInt("release");
            release = releases.get(releaseKey);
            if(release!=null) {
                if (release.getReleaseEventList() == null) {
                    release.setReleaseEventList(of.createReleaseEventList());
                }
                ReleaseEvent re = of.createReleaseEvent();
                re.setDate(Strings.emptyToNull(Utils.formatDate(rs.getInt("date_year"), rs.getInt("date_month"), rs.getInt("date_day"))));
                String iso_code=rs.getString("country");
                String gid       = rs.getString("gid");
                String name      = rs.getString("name");
                String sort_name = name;
                if(iso_code!=null) {
                    Iso31661CodeList isoList = of.createIso31661CodeList();
                    isoList.getIso31661Code().add(iso_code);
                    DefAreaElementInner area = of.createDefAreaElementInner();
                    area.setIso31661CodeList(isoList);
                    re.setArea(area);
                    area.setId(gid);
                    area.setName(name);
                    area.setSortName(sort_name);
                }
                release.getReleaseEventList().getReleaseEvent().add(re);
            }
        }
        //Add secondary types of the releasegroup that each release is part of
        stmt = createReleaseSecondaryTypesStatement(releaseKeys.size());
        count = 1;
        for (Integer key : releaseKeys) {
            stmt.setInt(count, key);
            count++;
        }
        rs = stmt.executeQuery();
        while (rs.next()) {
            int releaseKey = rs.getInt("releaseKey");
            release = releases.get(releaseKey);
            ReleaseGroup rg = release.getReleaseGroup();
            if (rg.getSecondaryTypeList() == null) {
                rg.setSecondaryTypeList(of.createSecondaryTypeList());
            }
            SecondaryType st = new SecondaryType();
            st.setContent(rs.getString("type"));
            rg.getSecondaryTypeList().getSecondaryType().add(st);
        }

        try {
            releaseClock.suspend();
        } catch (IllegalStateException e) {
            System.out.println("Warning: IllegalStateException during StopWatch.resume");
        }
        return releases;
    }


    public void indexData(IndexWriter indexWriter, int min, int max) throws SQLException, IOException {

        Map<Integer, List<Tag>>             tags                = loadTags(min, max);
        Map<Integer, List<String>>          isrcs               = loadISRCs(min, max);
        Map<Integer, ArtistCreditWrapper>   artistCredits       = updateArtistCreditWithAliases(loadArtists(min, max), min, max);
        Map<Integer, ArtistCreditWrapper>   trackArtistCredits  = updateTrackArtistCreditWithAliases(loadTrackArtists(min, max), min, max);
        Map<Integer, List<TrackWrapper>>    tracks              = loadTracks(min, max);
        Map<Integer, Release>               releases            = loadReleases(tracks);
        Map<Integer, ArtistCreditWrapper>   releaseArtists      = loadReleaseArtists(releases, min, max);

        PreparedStatement st = getPreparedStatement("RECORDINGS");
        st.setInt(1, min);
        st.setInt(2, max);
        recordingClock.resume();
        ResultSet rs = st.executeQuery();
        recordingClock.suspend();
        while (rs.next()) {
            indexWriter.addDocument(documentFromResultSet(rs, tags, isrcs, artistCredits, trackArtistCredits, tracks, releases, releaseArtists));
        }
        rs.close();

    }

    public Document documentFromResultSet(ResultSet rs,
                                          Map<Integer, List<Tag>> tags,
                                          Map<Integer, List<String>> isrcs,
                                          Map<Integer, ArtistCreditWrapper> artistCredits,
                                          Map<Integer, ArtistCreditWrapper> trackArtistCredits,
                                          Map<Integer, List<TrackWrapper>> tracks,
                                          Map<Integer, Release> releases,
                                          Map<Integer, ArtistCreditWrapper>   releaseArtists) throws SQLException {

        buildClock.resume();
        Set<Integer> durations = new HashSet<Integer>();
        Set<Integer> qdurs = new HashSet<Integer>();

        Set<String> trackNames = new HashSet<String>();

        int id = rs.getInt("recordingId");

        MbDocument doc = new MbDocument();
        ObjectFactory of = new ObjectFactory();
        Recording recording = of.createRecording();

        doc.addField(RecordingIndexField.ID, id);

        String guid = rs.getString("trackid");
        doc.addField(RecordingIndexField.RECORDING_ID, guid);
        recording.setId(guid);

        String recordingName = rs.getString("trackname");
        //Just add an accent version for recording name not track names
        doc.addField(RecordingIndexField.RECORDING_ACCENT, recordingName);
        recording.setTitle(recordingName);

        trackNames.add(recordingName.toLowerCase(Locale.UK));
        int recordingDuration = rs.getInt("duration");
        if (recordingDuration > 0) {
            durations.add(recordingDuration);
            recording.setLength(BigInteger.valueOf(recordingDuration));
        }

        String comment = rs.getString("comment");
        doc.addFieldOrNoValue(RecordingIndexField.COMMENT, comment);
        if (!Strings.isNullOrEmpty(comment)) {
            recording.setDisambiguation(comment);
        }

        boolean video = rs.getBoolean("video");
        if(video) {
            doc.addField(RecordingIndexField.VIDEO, Boolean.toString(video));
            recording.setVideo("true");
        }

        if (isrcs.containsKey(id)) {
            IsrcList isrcList = of.createIsrcList();
            for (String nextIsrc : isrcs.get(id)) {
                doc.addField(RecordingIndexField.ISRC, nextIsrc);
                Isrc isrc = of.createIsrc();
                isrc.setId(nextIsrc);
                isrcList.getIsrc().add(isrc);
            }
            recording.setIsrcList(isrcList);
        } else {
            doc.addFieldOrNoValue(RecordingIndexField.ISRC, null);
        }

        //Recording Artist Credit
        ArtistCreditWrapper ac = artistCredits.get(id);
        if (ac != null) {
            ArtistCreditHelper.buildIndexFieldsOnlyFromArtistCredit
                    (doc,
                            ac.getArtistCredit(),
                            RecordingIndexField.ARTIST,
                            RecordingIndexField.ARTIST_NAMECREDIT,
                            RecordingIndexField.ARTIST_ID,
                            RecordingIndexField.ARTIST_NAME);
            recording.setArtistCredit(ac.getArtistCredit());
        } else {
            System.out.println("\nNo artist credit found for recording:" + rs.getString("trackid"));
        }

        if (tracks.containsKey(id)) {

            ReleaseList releaseList = of.createReleaseList();
            recording.setReleaseList(releaseList);

            // For each track that uses recording
            for (TrackWrapper trackWrapper : tracks.get(id)) {
                //Get the release details for this track
                Release origRelease = releases.get(trackWrapper.getReleaseId());


                if (origRelease != null) {

                    //Get Artist Credit for Track
                    ArtistCreditWrapper taw = trackArtistCredits.get(trackWrapper.getTrackId());

                    //This release instance will be shared by all recordings that have a track on the release so we need
                    //to copy details so we can append track specific details
                    Release release = of.createRelease();
                    release.setId(origRelease.getId());
                    release.setTitle(origRelease.getTitle());
                    MediumList ml = of.createMediumList();
                    release.setReleaseGroup(origRelease.getReleaseGroup());
                    release.setStatus(origRelease.getStatus());
                    //This will be a va release
                    if(origRelease.getArtistCredit()!=null)
                    {
                        release.setArtistCredit(origRelease.getArtistCredit());
                    }
                    //Only add if release artist credit is different to track artist, or if that not set
                    //because same as recording artist only set if different to recording artist
                    else
                    {
                        ArtistCreditWrapper racWrapper = releaseArtists.get(trackWrapper.getReleaseId());
                        if(racWrapper!=null)
                        {
                            if(taw!=null)
                            {
                                if(taw.getArtistCreditId()!=racWrapper.getArtistCreditId())
                                {
                                    release.setArtistCredit(racWrapper.getArtistCredit());
                                }
                            }
                            else if(ac!=null)
                            {
                                if(ac.getArtistCreditId()!=racWrapper.getArtistCreditId())
                                {
                                    release.setArtistCredit(racWrapper.getArtistCredit());
                                }
                            }
                        }
                    }

                    ml.setTrackCount(origRelease.getMediumList().getTrackCount());
                    release.setMediumList(ml);
                    release.setReleaseEventList(origRelease.getReleaseEventList());
                    releaseList.getRelease().add(release);

                    ReleaseGroup rg = release.getReleaseGroup();
                    String trackGuid = trackWrapper.getTrackGuid();
                    doc.addNonEmptyField(RecordingIndexField.TRACK_ID, trackGuid);
                    String primaryType = "";
                    if (rg.getPrimaryType() != null)
                    {
                        primaryType = rg.getPrimaryType().getContent();
                    }
                    doc.addFieldOrUnknown(RecordingIndexField.RELEASEGROUP_ID, rg.getId());
                    doc.addFieldOrUnknown(RecordingIndexField.RELEASE_PRIMARY_TYPE, primaryType);
                    if (
                            (rg.getSecondaryTypeList() != null) &&
                                    (rg.getSecondaryTypeList().getSecondaryType() != null)
                            ) {
                        List<String> secondaryTypeStringList = new ArrayList<String>();
                        for (SecondaryType secondaryType : rg.getSecondaryTypeList().getSecondaryType()) {
                            String st = "";
                            if (secondaryType != null)
                            {
                                st = secondaryType.getContent();
                            }
                            doc.addField(RecordingIndexField.RELEASE_SECONDARY_TYPE, st);
                            secondaryTypeStringList.add(st);
                        }

                        String type = ReleaseGroupHelper.calculateOldTypeFromPrimaryType(primaryType, secondaryTypeStringList);
                        doc.addFieldOrNoValue(RecordingIndexField.RELEASE_TYPE, type);
                        rg.setType(type);
                    } else {
                        String pt = "";
                        if (release.getReleaseGroup().getPrimaryType() != null)
                        {
                            pt = release.getReleaseGroup().getPrimaryType().getContent();
                        }
                        doc.addFieldOrNoValue(RecordingIndexField.RELEASE_TYPE, pt);
                        rg.setType(pt);
                    }


                    doc.addNumericField(RecordingIndexField.NUM_TRACKS, trackWrapper.getTrackCount());
                    doc.addNumericField(RecordingIndexField.TRACKNUM, trackWrapper.getTrackPosition());
                    doc.addFieldOrNoValue(RecordingIndexField.NUMBER, trackWrapper.getTrackNumber());
                    DefTrackData track = of.createDefTrackData();
                    track.setId(trackGuid);
                    track.setTitle(trackWrapper.getTrackName());
                    track.setLength(BigInteger.valueOf(trackWrapper.getDuration()));
                    track.setNumber(trackWrapper.getTrackNumber());

                    Medium medium = of.createMedium();
                    medium.setPosition(BigInteger.valueOf(trackWrapper.getMediumPosition()));
                    Format format = new Format();
                    format.setContent(trackWrapper.getMediumFormat());
                    medium.setFormat(format);

                    Medium.TrackList tl  = of.createMediumTrackList();
                    tl.setCount(BigInteger.valueOf(trackWrapper.getTrackCount()));
                    tl.setOffset(BigInteger.valueOf(trackWrapper.getTrackPosition()  - 1));

                    release.getMediumList().getMedium().add(medium);
                    medium.setTrackList(tl);
                    tl.getDefTrack().add(track);
                    String st = "";
                    if (release.getStatus() != null)
                    {
                        st = release.getStatus().getContent();
                    }
                    doc.addFieldOrNoValue(RecordingIndexField.RELEASE_STATUS, st);

                    if (
                            (release.getReleaseEventList() != null) &&
                                    (release.getReleaseEventList().getReleaseEvent().size()>0)
                            ) {
                        for (ReleaseEvent re : release.getReleaseEventList().getReleaseEvent()) {
                            doc.addNonEmptyField(RecordingIndexField.RELEASE_DATE, re.getDate());
                            if(re.getArea()!=null) {
                                if(re.getArea().getIso31661CodeList()!=null) {
                                    doc.addNonEmptyField(RecordingIndexField.COUNTRY, re.getArea().getIso31661CodeList().getIso31661Code().get(0));
                                }
                            }
                        }
                        Collections.sort(release.getReleaseEventList().getReleaseEvent(), new ReleaseEventComparator());
                        ReleaseEvent firstReleaseEvent = release.getReleaseEventList().getReleaseEvent().get(0);
                        if (!Strings.isNullOrEmpty(firstReleaseEvent.getDate())) {
                            release.setDate(firstReleaseEvent.getDate());
                        }
                        if(firstReleaseEvent.getArea()!=null) {
                            if(firstReleaseEvent.getArea().getIso31661CodeList()!=null)
                            {
                                release.setCountry(firstReleaseEvent.getArea().getIso31661CodeList().getIso31661Code().get(0));
                            }
                        }

                    } else {
                        doc.addFieldOrNoValue(RecordingIndexField.RELEASE_DATE, null);
                        doc.addFieldOrNoValue(RecordingIndexField.COUNTRY, null);
                    }


                    doc.addField(RecordingIndexField.RELEASE_ID, release.getId());
                    doc.addField(RecordingIndexField.RELEASE, release.getTitle());
                    doc.addNumericField(RecordingIndexField.NUM_TRACKS_RELEASE, release.getMediumList().getTrackCount().intValue());



                    trackNames.add(trackWrapper.getTrackName().toLowerCase(Locale.UK));
                    doc.addField(RecordingIndexField.POSITION, String.valueOf(trackWrapper.getMediumPosition()));
                    doc.addFieldOrNoValue(RecordingIndexField.FORMAT, trackWrapper.getMediumFormat());

                    //If different to the Artist Credit for the recording
                    if (taw != null &&
                            (
                                    (ac == null) ||
                                            (taw.getArtistCreditId() != ac.getArtistCreditId())
                            )
                            ) {
                        ArtistCreditHelper.buildIndexFieldsOnlyFromArtistCredit
                                (doc,
                                        taw.getArtistCredit(),
                                        RecordingIndexField.ARTIST,
                                        RecordingIndexField.ARTIST_NAMECREDIT,
                                        RecordingIndexField.ARTIST_ID,
                                        RecordingIndexField.ARTIST_NAME);
                        track.setArtistCredit(taw.getArtistCredit());
                    }
                }
            }
        } else {
            doc.addFieldOrNoValue(RecordingIndexField.RELEASE_TYPE, "standalone");
        }

        if (tags.containsKey(id))
        {
            recording.setTagList(TagHelper.addTagsToDocAndConstructTagList(of, doc, tags, id, RecordingIndexField.TAG));
        }

        //If we have no recording length in the recording itself or the track length then we add this value so
        //they can search for recordings/tracks with no length
        if (durations.size() == 0) {
            doc.addField(RecordingIndexField.DURATION, Index.NO_VALUE);
            doc.addField(RecordingIndexField.QUANTIZED_DURATION, Index.NO_VALUE);
        } else {
            for (Integer dur : durations) {
                doc.addNumericField(RecordingIndexField.DURATION, dur);
                qdurs.add(dur / QUANTIZED_DURATION);
            }

            for (Integer qdur : qdurs) {
                doc.addNumericField(RecordingIndexField.QUANTIZED_DURATION, qdur);
            }

        }

        //Allow searching of all unique recording/track names
        for (String next : trackNames) {
            doc.addNonEmptyField(RecordingIndexField.RECORDING, next);
        }

        buildClock.suspend();
        storeClock.resume();
        doc.addField(RecordingIndexField.RECORDING_STORE, MMDSerializer.serialize(recording));
        storeClock.suspend();
        return doc.getLuceneDocument();
    }

    /**
     * Create various artist credits
     *
     * @return
     */
    private ArtistCredit createVariousArtistsCredit()
    {
        ObjectFactory of = new ObjectFactory();
        Artist       artist   = of.createArtist();
        artist.setId(VARIOUS_ARTISTS_GUID);
        artist.setName(VARIOUS_ARTISTS_NAME);
        artist.setSortName(VARIOUS_ARTISTS_NAME);
        NameCredit   naCredit = of.createNameCredit();
        naCredit.setArtist(artist);
        ArtistCredit vaCredit = of.createArtistCredit();
        vaCredit.getNameCredit().add(naCredit);
        return vaCredit;
    }

}
