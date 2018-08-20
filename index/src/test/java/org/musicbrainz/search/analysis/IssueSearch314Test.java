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

package org.musicbrainz.search.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;
import org.musicbrainz.search.LuceneVersion;

import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * Better handling of diacritics in analyser AND tokenizer
 *
 */
public class IssueSearch314Test
{

    @Test
    public void testDiacriticsConvertedInAnalyzerAndTokenizer1() throws Exception
    {

        //Show token is kept intact
        {
            Tokenizer tokenizer = new MusicbrainzTokenizer(LuceneVersion.LUCENE_VERSION, new StringReader("bär"));
            assertTrue(tokenizer.incrementToken());
            CharTermAttribute term = tokenizer.addAttribute(CharTermAttribute.class);
            TypeAttribute type = tokenizer.addAttribute(TypeAttribute.class);
            OffsetAttribute offset = tokenizer.addAttribute(OffsetAttribute.class);
            assertEquals("<ALPHANUM>", type.type());
            assertEquals("bär", new String(term.buffer(), 0, term.length()));
            assertEquals(0, offset.startOffset());
            assertEquals(3, offset.endOffset());
            assertFalse(tokenizer.incrementToken());
        }


        Analyzer analyzer = new MusicbrainzAnalyzer();
        RAMDirectory dir = new RAMDirectory();
        IndexWriterConfig writerConfig = new IndexWriterConfig(LuceneVersion.LUCENE_VERSION, analyzer);
        IndexWriter writer = new IndexWriter(dir, writerConfig);
        Document doc = new Document();
        doc.add(new Field("name", "bär", TextField.TYPE_STORED));
        writer.addDocument(doc);
        writer.close();

        //Show how it has been converted
        IndexReader ir = DirectoryReader.open(dir);
        Fields fields = MultiFields.getFields(ir);
        Terms terms = fields.terms("name");
        TermsEnum termsEnum = terms.iterator(null);
        termsEnum.next();
        assertEquals(1, termsEnum.docFreq());
        assertEquals("bar", termsEnum.term().utf8ToString());
        assertNull(termsEnum.next());

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(dir));
        {
            Query q = new QueryParser(LuceneVersion.LUCENE_VERSION, "name", analyzer).parse("name:\"bar\"");
            assertEquals(1, searcher.search(q, 10).totalHits);
        }

        searcher = new IndexSearcher(DirectoryReader.open(dir));
        {
            Query q = new QueryParser(LuceneVersion.LUCENE_VERSION, "name", analyzer).parse("name:\"bär\"");
            assertEquals(1, searcher.search(q, 10).totalHits);
        }

        searcher = new IndexSearcher(DirectoryReader.open(dir));
        {
            Query q = new QueryParser(LuceneVersion.LUCENE_VERSION, "name", analyzer).parse("name:\"bär\"");
            assertEquals(1, searcher.search(q, 10).totalHits);
        }
    }

    @Test
    public void testDiacriticsConvertedInAnalyzerAndTokenizer2() throws Exception {

        //Show token is kept intact
        {
            Tokenizer tokenizer = new MusicbrainzTokenizer(LuceneVersion.LUCENE_VERSION, new StringReader("bär"));
            assertTrue(tokenizer.incrementToken());
            CharTermAttribute term = tokenizer.addAttribute(CharTermAttribute.class);
            TypeAttribute type = tokenizer.addAttribute(TypeAttribute.class);
            OffsetAttribute offset = tokenizer.addAttribute(OffsetAttribute.class);
            assertEquals("<ALPHANUMANDPUNCTUATION>", type.type());
            assertEquals("bär", new String(term.buffer(), 0, term.length()));
            assertEquals(0, offset.startOffset());
            assertEquals(4, offset.endOffset());
            assertFalse(tokenizer.incrementToken());
        }

        Analyzer analyzer = new MusicbrainzAnalyzer();
        RAMDirectory dir = new RAMDirectory();
        IndexWriterConfig writerConfig = new IndexWriterConfig(LuceneVersion.LUCENE_VERSION, analyzer);
        IndexWriter writer = new IndexWriter(dir, writerConfig);
        Document doc = new Document();
        doc.add(new Field("name", "bär", TextField.TYPE_STORED));
        writer.addDocument(doc);
        writer.close();

        //Show how it has been converted
        IndexReader ir = DirectoryReader.open(dir);
        Fields fields = MultiFields.getFields(ir);
        Terms terms = fields.terms("name");
        TermsEnum termsEnum = terms.iterator(null);
        termsEnum.next();
        assertEquals(1, termsEnum.docFreq());
        assertEquals("bar", termsEnum.term().utf8ToString());
        assertNull(termsEnum.next());

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(dir));
        {
            Query q = new QueryParser(LuceneVersion.LUCENE_VERSION, "name", analyzer).parse("name:\"bar\"");
            assertEquals(1, searcher.search(q, 10).totalHits);
        }

        searcher = new IndexSearcher(DirectoryReader.open(dir));
        {
            Query q = new QueryParser(LuceneVersion.LUCENE_VERSION, "name", analyzer).parse("name:\"bär\"");
            assertEquals(1, searcher.search(q, 10).totalHits);
        }

        searcher = new IndexSearcher(DirectoryReader.open(dir));
        {
            Query q = new QueryParser(LuceneVersion.LUCENE_VERSION, "name", analyzer).parse("name:\"bär\"");
            assertEquals(1, searcher.search(q, 10).totalHits);
        }
    }

    @Test
    public void testDiacriticsConvertedInAnalyzerAndTokenizer3() throws Exception {

        //Show token is kept intact
        {
            Tokenizer tokenizer = new MusicbrainzTokenizer(LuceneVersion.LUCENE_VERSION, new StringReader("bar"));
            assertTrue(tokenizer.incrementToken());
            CharTermAttribute term = tokenizer.addAttribute(CharTermAttribute.class);
            TypeAttribute type = tokenizer.addAttribute(TypeAttribute.class);
            OffsetAttribute offset = tokenizer.addAttribute(OffsetAttribute.class);
            assertEquals("<ALPHANUM>", type.type());
            assertEquals("bar", new String(term.buffer(), 0, term.length()));
            assertEquals(0, offset.startOffset());
            assertEquals(3, offset.endOffset());
            assertFalse(tokenizer.incrementToken());
        }

        Analyzer analyzer = new MusicbrainzAnalyzer();
        RAMDirectory dir = new RAMDirectory();
        IndexWriterConfig writerConfig = new IndexWriterConfig(LuceneVersion.LUCENE_VERSION,analyzer);
        IndexWriter writer = new IndexWriter(dir, writerConfig);
        Document doc = new Document();
        doc.add(new Field("name", "bar", TextField.TYPE_STORED));

        writer.addDocument(doc);
        writer.close();

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(dir));
        {
            Query q = new QueryParser(LuceneVersion.LUCENE_VERSION, "name", analyzer).parse("name:\"bar\"");
            assertEquals(1, searcher.search(q,10).totalHits);
        }

        searcher = new IndexSearcher(DirectoryReader.open(dir));
        {
            Query q = new QueryParser(LuceneVersion.LUCENE_VERSION, "name", analyzer).parse("name:\"bär\"");
            assertEquals(1, searcher.search(q,10).totalHits);
        }

        searcher = new IndexSearcher(DirectoryReader.open(dir));
        {
            Query q = new QueryParser(LuceneVersion.LUCENE_VERSION, "name", analyzer).parse("name:\"bär\"");
            assertEquals(1, searcher.search(q,10).totalHits);
        }
    }
}