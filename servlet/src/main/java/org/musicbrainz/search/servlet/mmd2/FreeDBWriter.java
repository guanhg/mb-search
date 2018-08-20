/* Copyright (c) 2009 Paul Taylor
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
import org.musicbrainz.search.index.FreeDBIndexField;
import org.musicbrainz.search.servlet.Result;
import org.musicbrainz.search.servlet.Results;

import java.io.IOException;
import java.math.BigInteger;


public class FreeDBWriter extends ResultsWriter {

    /**
     *
     * @param metadata
     * @param results
     * @throws IOException
     */
    public void write(Metadata metadata, Results results) throws IOException {

        ObjectFactory of = new ObjectFactory();
        FreedbDiscList freeDBList = of.createFreedbDiscList();

        for (Result result : results.results) {
            MbDocument doc = result.getDoc();
            FreedbDisc freeDB = of.createFreedbDisc();

            freeDB.setArtist(doc.get(FreeDBIndexField.ARTIST));
            freeDB.setTitle(doc.get(FreeDBIndexField.TITLE));
            freeDB.setId(doc.get(FreeDBIndexField.DISCID));
            freeDB.setCategory(doc.get(FreeDBIndexField.CATEGORY));
            freeDB.setYear(doc.get(FreeDBIndexField.YEAR));

            result.setNormalizedScore(results.getMaxScore());
            freeDB.setScore(result.getNormalizedScore());
            Cdstub.TrackList trackList = of.createCdstubTrackList();
            trackList.setCount(new BigInteger(doc.get(FreeDBIndexField.TRACKS)));
            freeDB.setTrackList(trackList);
            freeDBList.getFreedbDisc().add(freeDB);

        }
        freeDBList.setCount(BigInteger.valueOf(results.getTotalHits()));
        freeDBList.setOffset(BigInteger.valueOf(results.getOffset()));
        metadata.setFreedbDiscList(freeDBList);
    }

    /**
     * Overridden because freedb doesn't currently write last date to index
     */
    @Override
    public Metadata write(Results results) throws IOException {
        ObjectFactory of = new ObjectFactory();
        Metadata metadata = of.createMetadata();
        write(metadata, results);
        return metadata;
    }

}