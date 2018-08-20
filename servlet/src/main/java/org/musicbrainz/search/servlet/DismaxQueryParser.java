/*
 Copyright (c) 2012 Paul Taylor
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

package org.musicbrainz.search.servlet;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.musicbrainz.search.LuceneVersion;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DismaxQueryParser {

    public static String IMPOSSIBLE_FIELD_NAME = "\uFFFC\uFFFC\uFFFC";
    protected DisjunctionQueryParser dqp;

    protected DismaxQueryParser() {
    }

    public DismaxQueryParser(org.apache.lucene.analysis.Analyzer analyzer) {
        dqp = new DisjunctionQueryParser(IMPOSSIBLE_FIELD_NAME, analyzer);
    }

    /**
     * Create query consists of disjunction queries for each term fields combo, and then
     * a phrase search for each field as long as the original query is more than one term
     *
     * @param query
     * @return
     *
     */
    public Query parse(String query) throws org.apache.lucene.queryparser.classic.ParseException {

        Query term = dqp.parse(DismaxQueryParser.IMPOSSIBLE_FIELD_NAME + ":(" + query + ")");
        Query phrase = dqp.parse(DismaxQueryParser.IMPOSSIBLE_FIELD_NAME + ":\"" + query + "\"");
        return buildTopQuery(term, phrase);
    }

    /**
     * If a phrase query was built then we create a boolean query that requires something to match in
     * the term query, under normal circumstances if nothing matches the term query nothing will match the phrase
     * query
     *
     * @param term
     * @param phrase
     * @return
     */
    protected Query buildTopQuery(Query term, Query phrase) {
        if (phrase instanceof DisjunctionMaxQuery) {
            BooleanQuery bq = new BooleanQuery(true);
            bq.add(term, BooleanClause.Occur.MUST);
            bq.add(phrase, BooleanClause.Occur.SHOULD);
            return bq;
        } else {
            return term;
        }
    }


    public void addAlias(String field, DismaxAlias dismaxAlias) {
        dqp.addAlias(field, dismaxAlias);
    }

    static class DisjunctionQueryParser extends QueryParser {

        //Only make search terms that are this length fuzzy searchable and only match to terms that are also this length
        protected static final int MIN_FIELD_LENGTH_TO_MAKE_FUZZY = 4;
        protected static final float FUZZY_SIMILARITY = 0.5f;

        //Reduce boost of wildcard/fuzzy matches compared to exact matches
        protected static final float WILDCARD_BOOST_REDUCER = 0.8f;

        //Reduce phrase query scores otherwise there is too much difference between a document that matches on
        //phrase and one that doesn't quite.
        protected static final float PHRASE_BOOST_REDUCER = 0.2f;


        public DisjunctionQueryParser(String defaultField, org.apache.lucene.analysis.Analyzer analyzer) {
            super(LuceneVersion.LUCENE_VERSION, defaultField, analyzer);
        }

        protected Map<String, DismaxAlias> aliases = new HashMap<String, DismaxAlias>(3);

        //Field to DismaxAlias
        public void addAlias(String field, DismaxAlias dismaxAlias) {
            aliases.put(field, dismaxAlias);
        }

        // Rewrite Method used by Prefix Search and Fuzzy Search, use idf of the original term
        MultiTermQuery.RewriteMethod prefixRewrite = new PrefixTermRewrite(200);
        MultiTermQuery.RewriteMethod fuzzyRewrite = new FuzzyTermRewrite(200);


        protected boolean checkQuery(DisjunctionMaxQuery q, Query querySub, boolean quoted, DismaxAlias a, String f) {
            if (querySub != null) {
                //if query was quoted but doesn't generate a phrase query we reject it
                if ((!quoted) || (querySub instanceof PhraseQuery)) {
                    //Reduce phrase because will have matched both parts giving far too much score differential
                    if (quoted) {
                        querySub.setBoost(PHRASE_BOOST_REDUCER);
                    } else {
                        querySub.setBoost(a.getFields().get(f).getBoost());
                    }
                    q.add(querySub);
                    return true;
                }
            }
            return false;
        }

        @Override
        //TODO FIXME was using a FLOAT similarity value of 0.5 but now changed to integral
        protected Query getFuzzyQuery(String field, String termStr, float minSimilarity) {
            Term t = new Term(field, termStr);
            FuzzyQuery fq = new FuzzyQuery(t,  2, MIN_FIELD_LENGTH_TO_MAKE_FUZZY);
            fq.setRewriteMethod(fuzzyRewrite);
            return fq;
        }

        @Override
        protected Query getFieldQuery(String field, String queryText, boolean quoted)
                throws ParseException
        {
            return getFieldQuery(field, queryText, quoted, MIN_FIELD_LENGTH_TO_MAKE_FUZZY);
        }

        protected Query getFieldQuery(String field, String queryText, boolean quoted, int fuzzyFieldLength)
                throws ParseException
        {
            //If field is an alias
            if (aliases.containsKey(field)) {

                DismaxAlias a = aliases.get(field);
                DisjunctionMaxQuery q = new DisjunctionMaxQuery(a.getTie());
                boolean ok = false;

                for (String f : a.getFields().keySet()) {

                    //if query can be created for this field and text
                    Query querySub;
                    Query queryWildcard = null;
                    Query queryFuzzy = null;

                    DismaxAlias.AliasField af = a.getFields().get(f);
                    if (!quoted && queryText.length() >= fuzzyFieldLength) {
                        querySub = getFieldQuery(f, queryText, quoted);
                        if (querySub instanceof TermQuery) {

                            if (af.isFuzzy()) {
                                Term t = ((TermQuery) querySub).getTerm();
                                queryWildcard = newPrefixQuery(new Term(t.field(), t.text()));
                                queryFuzzy = getFuzzyQuery(t.field(), t.text(), FUZZY_SIMILARITY);
                                queryFuzzy.setBoost(af.getBoost() * WILDCARD_BOOST_REDUCER);
                                q.add(queryFuzzy);
                                queryWildcard.setBoost(af.getBoost() * WILDCARD_BOOST_REDUCER);
                                q.add(queryWildcard);
                            }
                        }
                    } else {
                        querySub = getFieldQuery(f, queryText, quoted);
                    }

                    if (checkQuery(q, querySub, quoted, a, f) && ok == false) {
                        ok = true;
                    }
                }
                //Something has been added to disjunction query
                return ok ? q : null;

            } else {
                //usual Field
                try {
                    return super.getFieldQuery(field, queryText, quoted);
                } catch (Exception e) {
                    return null;
                }
            }
        }

        /**
         * Builds a new PrefixQuery instance
         * @param prefix Prefix term
         * @return new PrefixQuery instance
         */
        protected Query newPrefixQuery(Term prefix){
            PrefixQuery query = new PrefixQuery(prefix);
            query.setRewriteMethod(prefixRewrite);
            return query;
        }
    }

    /**
     * Fuzzy matches are rewritten to a DisjunctionMaxQuery instead of the more usual BooleanQuery so that
     * if search term matches multiple fields we just take the best field rather summing all matches like a boolean
     * query. The 0.1 for tiebreaker is to favour documents that contain all words rather than the same word in multiple
     * fields.
     *
     * We use a constant score otherwise fuzzy matches could get higher scores then an exact match
     *
     * @param <Q>
     */
    public static class FuzzyTermRewrite<Q extends DisjunctionMaxQuery> extends TopTermsRewrite<Query> {

        public FuzzyTermRewrite(int size) {
            super(size);
        }

        @Override
        protected int getMaxSize() {
            return BooleanQuery.getMaxClauseCount();
        }

        @Override
        protected DisjunctionMaxQuery getTopLevelQuery() {
            return new DisjunctionMaxQuery(0.1f);
        }

        @Override
        protected void addClause(Query topLevel, Term term, int docCount, float boost, TermContext states) {
            final Query tq = new ConstantScoreQuery(new TermQuery(term, states));
            tq.setBoost(boost);
            ((DisjunctionMaxQuery)topLevel).add(tq);
        }
    }

    /**
     *
     * Prefix matches are rewritten to a DisjunctionMaxQuery instead of the more usual BooleanQuery so that
     * if search term matches multiple fields we just take the best field rather summing all matches like a boolean
     * query. The 0.1 for tiebreaker is to favour documents that contain all words rather than the same word in multiple
     * fields.
     *
     * We set the idf the same as an exact match so that a wildcard match to a term which happens to be rarer than
     * the exact term we were searching for does not get an unfairly high idf.
     *
     */
    public static class PrefixTermRewrite extends MultiTermQuery.RewriteMethod {

        private TFIDFSimilarity     similarity;
        private FuzzyTermRewrite    rewrite;

        public PrefixTermRewrite(int size) {
            this.rewrite    = new FuzzyTermRewrite(size);
            this.similarity = new DefaultSimilarity();
        }

        protected float getQueryBoost(final IndexReader reader, final MultiTermQuery query)
                throws IOException {
            float idf = 1f;
            float df;
            PrefixQuery fq = (PrefixQuery) query;
            df = reader.docFreq(fq.getPrefix());
            if(df>=1)
            {
                //Same as idf value for search term, 0.5 acts as length norm
                idf = (float)Math.pow(similarity.idf((int) df, reader.numDocs()),2) * 0.5f;
            }
            return idf;
        }


        @Override
        public Query rewrite(final IndexReader reader, final MultiTermQuery query) throws IOException {
            DisjunctionMaxQuery  dmq = (DisjunctionMaxQuery)rewrite.rewrite(reader, query);
            float idfBoost = getQueryBoost(reader, query);
            Iterator<Query> iterator = dmq.iterator();
            while(iterator.hasNext())
            {
                Query next = iterator.next();
                next.setBoost(next.getBoost() * idfBoost);
            }
            return dmq;
        }
    }


}