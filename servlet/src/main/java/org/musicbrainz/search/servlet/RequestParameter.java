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

package org.musicbrainz.search.servlet;

public enum RequestParameter {
	
    QUERY ("query"),
    TYPE ("type"),
    FORMAT ("fmt"),
    LIMIT ("limit"),
    MAX ("max"),
    OFFSET ("offset"),
    COUNT ("count"),
    VERSION ("version"),
    DISMAX("dismax"),
    EXPLAIN("explain"),
    PRETTY("pretty"),
    // For admin only
    INIT ("init"),
    RELOAD_INDEXES ("reload"),
    RATE("rate"),
    GC("gc"),
    ;
    
    private String name;

    RequestParameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static RequestParameter getValue(String value) {
        for ( RequestParameter candidateEnum : RequestParameter.values() ) {
            if(candidateEnum.getName().equals(value)) return candidateEnum;
        }
        return null;
    }
}
