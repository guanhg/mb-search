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

import java.util.LinkedList;
import java.util.List;

/**
 * Store the results of a search
 */
public class Results implements Comparable<Results>{

    private float maxScore=0.0f;
    private int offset;
    private int totalHits;
    private ResourceType resourceType;

    public List<Result> results;

    public Results() {
        results = new LinkedList<Result>();
    }

    public int compareTo(Results results)
    {
        //If I don't explicity check for Nan behaviour is inconsistent test fails under maven, possibly a fix in java version
        //As we are only ever comparing 6 numbers not a big deal
        if(Float.isNaN(getMaxScore()))
        {
            if(Float.isNaN(results.getMaxScore()))
            {
                return 0;
            }
            else
            {
                return -1;
            }
        }
        if(Float.isNaN(results.getMaxScore()))
        {
            return 1;
        }
        int result =  (getMaxScore() < results.getMaxScore() ? -1 : (getMaxScore() == results.getMaxScore() ? 0 : 1));
        return result;

    }

    public float getMaxScore()
    {
        return maxScore;
    }

    public void setMaxScore(float maxScore)
    {
        this.maxScore = maxScore;
    }

    public int getOffset()
    {
        return offset;
    }

    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    public int getTotalHits()
    {
        return totalHits;
    }

    public void setTotalHits(int totalHits)
    {
        this.totalHits = totalHits;
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
