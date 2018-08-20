/*
 Copyright (c) 2010 Paul Taylor
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
  2. Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
  3. Neither the name of the MusicBrainz project nor the names of the
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.musicbrainz.search.helper;

import org.musicbrainz.mmd2.ObjectFactory;
import org.musicbrainz.mmd2.Tag;
import org.musicbrainz.mmd2.TagList;
import org.musicbrainz.search.MbDocument;
import org.musicbrainz.search.index.IndexField;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared for retrieving tags for an entity, all entities use the same pattern
 */
public class TagHelper {

    public static String constructTagQuery(String tagTableName, String entityColName)
    {
        return "SELECT t1."
                + entityColName
                + ", t2.name as tag, t1.count as count " +
                " FROM " +
                tagTableName +
                " t1" +
                "  INNER JOIN tag t2 ON tag=id " +
                " WHERE t1." +
                entityColName +
                " between ? AND ?";
    }

    /**
     * Load Tags
     *
     * @param min
     * @param max
     * @return
     * @throws SQLException
     * @throws java.io.IOException
     */
    public static Map<Integer, List<Tag>> loadTags(int min, int max, PreparedStatement st, String entityKey) throws SQLException, IOException
    {
        st.setInt(1, min);
        st.setInt(2, max);
        ResultSet rs = st.executeQuery();
        Map<Integer, List<Tag>> tags = completeTagsFromDbResults(rs, entityKey);
        rs.close();
        return tags;
    }

    /**
     *
     * @param rs
     * @param entityKey
     * @return
     * @throws SQLException
     */
    public static Map<Integer,List<Tag>> completeTagsFromDbResults(ResultSet rs,
                                                                  String entityKey) throws SQLException {
        Map<Integer, List<Tag>> tags = new HashMap<Integer, List<Tag>>();
        ObjectFactory of = new ObjectFactory();
        List<Tag> tagList;
        while (rs.next()) {
            int entityId = rs.getInt(entityKey);
            if (!tags.containsKey(entityId)) {
                tagList = new ArrayList<Tag>();
                tags.put(entityId,tagList );
            } else {
                tagList = tags.get(entityId);
            }

            Tag tag = of.createTag();
            tag.setName(rs.getString("tag"));
            tag.setCount(BigInteger.valueOf(rs.getInt("count")));
            tagList.add(tag);
        }
        return tags;
    }

    /**
     * Add tags to search field and return tag list for adding to store object
     *
     * @param of
     * @param doc
     * @param tags
     * @param entityId
     * @param aliasIndexField
     * @return
     */
    public static TagList addTagsToDocAndConstructTagList(ObjectFactory of, MbDocument doc, Map<Integer, List<Tag>> tags, int entityId, IndexField aliasIndexField)
    {
        TagList tagList = of.createTagList();
        for (Tag nextTag : tags.get(entityId)) {
            Tag tag = of.createTag();
            doc.addField(aliasIndexField, nextTag.getName());
            tag.setName(nextTag.getName());
            tag.setCount(new BigInteger(nextTag.getCount().toString()));
            tagList.getTag().add(tag);
        }
        return tagList;
    }
}
