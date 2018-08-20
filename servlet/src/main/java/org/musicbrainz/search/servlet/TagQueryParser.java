package org.musicbrainz.search.servlet;

import org.apache.lucene.analysis.Analyzer;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.musicbrainz.search.LuceneVersion;

/**
 * Subclasses QueryParser to handle numeric fields that we might want wish to do range queries for and handle type
 * searches specified using integers.
 */
public class TagQueryParser extends QueryParser
{

    public TagQueryParser(String field, Analyzer a) {
        super(LuceneVersion.LUCENE_VERSION, field, a);
    }
}