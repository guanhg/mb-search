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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.similarities.Similarity;
import org.musicbrainz.mmd2.*;
import org.musicbrainz.search.MbDocument;
import org.musicbrainz.search.analysis.MusicbrainzSimilarity;
import org.musicbrainz.search.helper.AliasHelper;
import org.musicbrainz.search.helper.TagHelper;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;

public class LabelIndex extends DatabaseIndex {

    public static final String INDEX_NAME = "label";

    private static final String DELETED_LABEL_MBID = "f43e252d-9ebf-4e8e-bba8-36d080756cc1";

    public LabelIndex(Connection dbConnection) {
        super(dbConnection);
    }

    public LabelIndex() {
    }


    public String getName() {
        return LabelIndex.INDEX_NAME;
    }

    public Analyzer getAnalyzer() {
        return DatabaseIndex.getAnalyzer(LabelIndexField.class);
    }

	@Override
	public IndexField getIdentifierField() {
		return LabelIndexField.ID;
	}
    
    public int getMaxId() throws SQLException {
        Statement st = dbConnection.createStatement();
        ResultSet rs = st.executeQuery("SELECT MAX(id) FROM label");
        rs.next();
        return rs.getInt(1);
    }

    public int getNoOfRows(int maxId) throws SQLException {
    	PreparedStatement st = dbConnection.prepareStatement(
    		"SELECT count(*) FROM label WHERE id <= ? AND gid <> ?::uuid");
    	st.setInt(1, maxId);
    	st.setString(2, DELETED_LABEL_MBID);
    	ResultSet rs = st.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    @Override
    public Similarity getSimilarity()
    {
        return new MusicbrainzSimilarity();
    }

    @Override
    public void init(IndexWriter indexWriter, boolean isUpdater) throws SQLException {

        addPreparedStatement("TAGS", TagHelper.constructTagQuery("label_tag", "label"));
        addPreparedStatement("ALIASES", AliasHelper.constructAliasQuery("label"));
        addPreparedStatement("LABELS",
                "SELECT label.id, label.gid, label.name as name," +
                "  label_type.name as type, label.begin_date_year, label.begin_date_month, label.begin_date_day, " +
                "  label.end_date_year, label.end_date_month, label.end_date_day, label.ended," +
                "  label.comment, label_code, lower(i.code) as country, " +
                "  a1.gid as area_gid, a1.name as area_name " +
                " FROM label " +
                "  LEFT JOIN label_type ON label.type = label_type.id " +
                "  LEFT JOIN iso_3166_1 i on label.area=i.area" +
                "  LEFT JOIN area a1 on label.area = a1.id" +
                " WHERE label.id BETWEEN ? AND ?");

        addPreparedStatement("IPICODES",
                "SELECT ipi, label " +
                        " FROM label_ipi  " +
                        " WHERE label between ? AND ?");

    }

    private  Map<Integer, List<String>> loadIpiCodes(int min, int max) throws SQLException, IOException {
        Map<Integer, List<String>> ipiCodes = new HashMap<Integer, List<String>>();
        PreparedStatement st = getPreparedStatement("IPICODES");
        st.setInt(1, min);
        st.setInt(2, max);
        ResultSet rs = st.executeQuery();
        while (rs.next()) {
            int artistId = rs.getInt("label");

            List<String> list;
            if (!ipiCodes.containsKey(artistId)) {
                list = new LinkedList<String>();
                ipiCodes.put(artistId, list);
            } else {
                list = ipiCodes.get(artistId);
            }
            list.add(rs.getString("ipi"));
        }
        rs.close();
        return ipiCodes;
    }



    public void indexData(IndexWriter indexWriter, int min, int max) throws SQLException, IOException {

        Map<Integer,List<Tag>> tags = TagHelper.loadTags(min, max, getPreparedStatement("TAGS"), "label");
        Map<Integer, List<String>> ipiCodes = loadIpiCodes(min,max);
        Map<Integer, Set<Alias>> aliases = AliasHelper.completeFromDbResults(min, max, getPreparedStatement("ALIASES"));

        // Get labels
        PreparedStatement st = getPreparedStatement("LABELS");
        st.setInt(1, min);
        st.setInt(2, max);
        ResultSet rs = st.executeQuery();
        while (rs.next()) {
            if(rs.getString("gid").equals(DELETED_LABEL_MBID))
            {
                continue;
            }
            indexWriter.addDocument(documentFromResultSet(rs, tags, ipiCodes, aliases));
        }
        rs.close();
    }

    public Document documentFromResultSet(ResultSet rs,
                                          Map<Integer,List<Tag>> tags,
                                          Map<Integer, List<String>> ipiCodes,
                                          Map<Integer, Set<Alias>> aliases) throws SQLException {

        MbDocument doc = new MbDocument();

        ObjectFactory of = new ObjectFactory();
        Label label = of.createLabel();

        int labelId = rs.getInt("id");
        doc.addField(LabelIndexField.ID, labelId);

        String labelGuid = rs.getString("gid");
        doc.addField(LabelIndexField.LABEL_ID, labelGuid);
        label.setId(labelGuid);

        String name=rs.getString("name");
        doc.addField(LabelIndexField.LABEL,name );
        label.setName(name);
        doc.addField(LabelIndexField.SORTNAME, name);
        label.setSortName(name);


        //Accented artist
        doc.addField(LabelIndexField.LABEL_ACCENT, name );


        String type = rs.getString("type");
        doc.addFieldOrUnknown(LabelIndexField.TYPE, type);
        if (!Strings.isNullOrEmpty(type)) {
            label.setType(type);
        }

        String comment = rs.getString("comment");
        doc.addFieldOrNoValue(LabelIndexField.COMMENT, comment);
        if (!Strings.isNullOrEmpty(comment)) {
            label.setDisambiguation(comment);
        }

        String country = rs.getString("country");
        doc.addFieldOrUnknown(LabelIndexField.COUNTRY, country);
        if (!Strings.isNullOrEmpty(country)) {
            label.setCountry(country.toUpperCase(Locale.US));
        }

        String areaId = rs.getString("area_gid");
        if(areaId!=null) {
            DefAreaElementInner area = of.createDefAreaElementInner();
            area.setId(areaId);
            String areaName = rs.getString("area_name");
            area.setName(areaName);
            doc.addFieldOrNoValue(ArtistIndexField.AREA, areaName);
            area.setSortName(areaName);
            label.setArea(area);
        }
        else {
            doc.addField(ArtistIndexField.AREA, Index.NO_VALUE);
        }

        boolean ended = rs.getBoolean("ended");
        doc.addFieldOrUnknown(LabelIndexField.ENDED, Boolean.toString(ended));

        String begin = Utils.formatDate(rs.getInt("begin_date_year"), rs.getInt("begin_date_month"), rs.getInt("begin_date_day"));
        doc.addNonEmptyField(LabelIndexField.BEGIN, begin);

        String end = Utils.formatDate(rs.getInt("end_date_year"), rs.getInt("end_date_month"), rs.getInt("end_date_day"));
        doc.addNonEmptyField(LabelIndexField.END, end);

        LifeSpan lifespan = of.createLifeSpan();
        label.setLifeSpan(lifespan);
        if(!Strings.isNullOrEmpty(begin)) {
            lifespan.setBegin(begin);
        }
        if(!Strings.isNullOrEmpty(end)) {
            lifespan.setEnd(end);
        }
        lifespan.setEnded(Boolean.toString(ended));


        int labelcode = rs.getInt("label_code");
        if (labelcode > 0) {
            doc.addField(LabelIndexField.CODE, labelcode);
            label.setLabelCode(BigInteger.valueOf(labelcode));
        }
        else {
            doc.addField(LabelIndexField.CODE,Index.NO_VALUE);
        }

        if (aliases.containsKey(labelId))
        {
            label.setAliasList(AliasHelper.addAliasesToDocAndConstructAliasList(of, doc, aliases, labelId, LabelIndexField.ALIAS));
        }

        if (tags.containsKey(labelId))
        {
            label.setTagList(TagHelper.addTagsToDocAndConstructTagList(of, doc, tags, labelId, LabelIndexField.TAG));
        }

        if (ipiCodes.containsKey(labelId)) {
            IpiList ipiList = of.createIpiList();
            for (String ipiCode : ipiCodes.get(labelId)) {
                doc.addField(LabelIndexField.IPI, ipiCode);
                ipiList.getIpi().add(ipiCode);
            }
            label.setIpiList(ipiList);
        }

        LabelBoostDoc.boost(labelGuid, doc);

        String store = MMDSerializer.serialize(label);
        doc.addField(LabelIndexField.LABEL_STORE, store);



        return doc.getLuceneDocument();
    }

}
