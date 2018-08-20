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

package org.musicbrainz.search.servlet;

import org.musicbrainz.search.MbDocument;

public class Result implements Comparable<Result>{

	private MbDocument   doc;
	private float        score;
    private int          normalizedScore;
    private ResourceType resourceType;


    public Result() {
    }

	public MbDocument getDoc() {
		return doc;
	}

	public float getScore() {
		return score;
	}

    public void setNormalizedScore(float maxScore)
    {
        normalizedScore = (((int)(((float)(getScore() / maxScore)) * 100)));
    }

    public int compareTo(Result result)
    {
        return  (getNormalizedScore() < result.getNormalizedScore() ? -1 : (getNormalizedScore() == result.getNormalizedScore() ? 0 : 1));

    }

    public void setDoc(MbDocument doc)
    {
        this.doc = doc;
    }

    public void setScore(float score)
    {
        this.score = score;
    }

    public int getNormalizedScore()
    {
        return normalizedScore;
    }


    public ResourceType getResourceType()
    {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType)
    {
        this.resourceType = resourceType;
    }
}
