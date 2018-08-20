/* Copyright (c) 2009 Lukas Lalinsky
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the MusicBrainz project nor the names of the
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.musicbrainz.search.servlet.mmd2;

import org.musicbrainz.mmd2.*;
import org.musicbrainz.search.MbDocument;
import org.musicbrainz.search.helper.ArtistCreditHelper;
import org.musicbrainz.search.index.ReleaseGroupIndexField;
import org.musicbrainz.search.servlet.Result;
import org.musicbrainz.search.servlet.Results;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class ReleaseGroupWriter extends ResultsWriter {


    /**
     * @param metadata
     * @param results
     * @throws IOException
     */
    public void write(Metadata metadata, Results results) throws IOException {
        ObjectFactory of = new ObjectFactory();
        ReleaseGroupList releaseGroupList = of.createReleaseGroupList();

        for (Result result : results.results) {
            result.setNormalizedScore(results.getMaxScore());
        }
        write(releaseGroupList.getReleaseGroup(), results);

        releaseGroupList.setCount(BigInteger.valueOf(results.getTotalHits()));
        releaseGroupList.setOffset(BigInteger.valueOf(results.getOffset()));
        metadata.setReleaseGroupList(releaseGroupList);
    }

    /**
     * @param list
     * @param results
     * @throws IOException
     */
    public void write(List list, Results results) throws IOException {
        for (Result result : results.results) {
            write(list, result);
        }
    }

    /**
     * @param list
     * @param result
     * @throws IOException
     */
    public void write(List list, Result result) throws IOException {
        ObjectFactory of = new ObjectFactory();

        MbDocument doc = result.getDoc();
        ReleaseGroup releaseGroup = of.createReleaseGroup();
        releaseGroup.setId(doc.get(ReleaseGroupIndexField.RELEASEGROUP_ID));
        releaseGroup.setScore(result.getNormalizedScore());
        String name = doc.get(ReleaseGroupIndexField.RELEASEGROUP);
        if (name != null) {
            releaseGroup.setTitle(name);
        }

        String comment = doc.get(ReleaseGroupIndexField.COMMENT);
        if (isNotNoValue(comment)) {
            releaseGroup.setDisambiguation(comment);
        }

        String type = doc.get(ReleaseGroupIndexField.TYPE);
        if (isNotUnknown(type)) {
            releaseGroup.setType(type);
        }

        String primaryType = doc.get(ReleaseGroupIndexField.PRIMARY_TYPE);
        if (isNotUnknown(primaryType)) {
            PrimaryType pt = new PrimaryType();
            pt.setContent(primaryType);
            releaseGroup.setPrimaryType(pt);
        }

        String[] secondaryTypes = doc.getValues(ReleaseGroupIndexField.SECONDARY_TYPE);
        if (secondaryTypes.length > 0) {
            SecondaryTypeList stl = of.createSecondaryTypeList();
            for (int i = 0; i < secondaryTypes.length; i++) {
                SecondaryType st = new SecondaryType();
                st.setContent(secondaryTypes[i]);
                stl.getSecondaryType().add(st);
            }
            releaseGroup.setSecondaryTypeList(stl);
        }

        if (doc.get(ReleaseGroupIndexField.ARTIST_CREDIT) != null) {
            ArtistCredit ac = ArtistCreditHelper.unserialize(doc.get(ReleaseGroupIndexField.ARTIST_CREDIT));
            releaseGroup.setArtistCredit(ac);
        }

        String[] releaseIds = doc.getValues(ReleaseGroupIndexField.RELEASE_ID);
        String[] releaseNames = doc.getValues(ReleaseGroupIndexField.RELEASE);
        String[] releaseStatuses = doc.getValues(ReleaseGroupIndexField.RELEASESTATUS);

        ReleaseList releaseList = of.createReleaseList();
        releaseList.setCount(BigInteger.valueOf(releaseIds.length));
        for (int i = 0; i < releaseIds.length; i++) {
            Release release = of.createRelease();
            release.setId(releaseIds[i]);
            release.setTitle(releaseNames[i]);
            Status st = new Status();
            st.setContent(releaseStatuses[i]);
            release.setStatus(st);

            releaseList.getRelease().add(release);
        }
        releaseGroup.setReleaseList(releaseList);

        String[] tags = doc.getValues(ReleaseGroupIndexField.TAG);
        String[] tagCounts = doc.getValues(ReleaseGroupIndexField.TAGCOUNT);
        if (tags.length > 0) {
            TagList tagList = of.createTagList();
            for (int i = 0; i < tags.length; i++) {
                Tag tag = of.createTag();
                tag.setName(tags[i]);
                tag.setCount(new BigInteger(tagCounts[i]));
                tagList.getTag().add(tag);
            }
            releaseGroup.setTagList(tagList);
        }
        list.add(releaseGroup);
    }

    /**
     * Overridden to ensure all attributes are set for each alias
     *
     * @param metadata
     */
    @Override
    public void adjustForJson(Metadata metadata) {

        if (metadata.getReleaseGroupList().getReleaseGroup().size()>0) {
            for(ReleaseGroup releaseGroup:metadata.getReleaseGroupList().getReleaseGroup()) {
                if(releaseGroup.getArtistCredit()!=null) {
                    for (NameCredit nc :releaseGroup.getArtistCredit().getNameCredit())
                    {
                        if(nc.getArtist()!=null && nc.getArtist().getAliasList()!=null)
                        {
                            for (Alias alias : nc.getArtist().getAliasList().getAlias()) {
                                //On Xml output as primary, but in json they have changed to true/false
                                if (alias.getPrimary() != null) {
                                    alias.setPrimary("true");
                                }
                            }
                        }
                    }

                }
            }
        }

    }
}
