package org.musicbrainz.search.analysis;

import com.ibm.icu.text.Normalizer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;
import org.musicbrainz.search.LuceneVersion;

import java.io.IOException;
import java.io.StringReader;

/**
 * Compare Filters
 */
public class CompareNormalizationFiltersTest {

    @Test
    public void testTokenization() throws IOException
    {
        StringBuffer sb = new StringBuffer();


        for(char i=0;i<100;i++)
        {
            Character c = new Character(i);
            if(!Character.isWhitespace(c)) {
                sb.append(new Character(i).toString() + ' ');
            }
        }
        System.out.println(sb.toString());
        Tokenizer tokenizer = new WhitespaceTokenizer(LuceneVersion.LUCENE_VERSION,new StringReader(sb.toString()));
        tokenizer.reset();
        while(tokenizer.incrementToken())
        {
        }
    }

    @Test
    public void testFilters() throws IOException
    {
        StringBuffer sb = new StringBuffer();


        for(char i=0;i<65535;i++)
        {
            Character c = new Character(i);
            if(!Character.isWhitespace(c)) {
                sb.append(new Character(i).toString() + ' ');
            }
        }
        Tokenizer tokenizer0 = new WhitespaceTokenizer(LuceneVersion.LUCENE_VERSION,new StringReader(sb.toString()));
        Tokenizer tokenizer1 = new WhitespaceTokenizer(LuceneVersion.LUCENE_VERSION,new StringReader(sb.toString()));
        Tokenizer tokenizer2 = new WhitespaceTokenizer(LuceneVersion.LUCENE_VERSION,new StringReader(sb.toString()));
        Tokenizer tokenizer3 = new WhitespaceTokenizer(LuceneVersion.LUCENE_VERSION,new StringReader(sb.toString()));
        Tokenizer tokenizer4 = new WhitespaceTokenizer(LuceneVersion.LUCENE_VERSION,new StringReader(sb.toString()));

        TokenStream result1 = new AccentFilter(tokenizer1);
        TokenStream result2 = new ASCIIFoldingFilter(tokenizer2);
        Token t0;
        Token t;
        Token t2;
        Token t3;
        Token t4;

        
        int changedByAccent =0;
        int changedByASCII  =0;
        int changedByNFKC    =0;
        int changedByASCIIAndNFKC    =0;

        System.out.println("Chars that are changed by one filter different to other filter");
        System.out.println("input:existingfilter:newfilter");

        CharTermAttribute term  = tokenizer0.addAttribute(CharTermAttribute.class);
        CharTermAttribute term1 = result1.addAttribute(CharTermAttribute.class);
        CharTermAttribute term2 = result2.addAttribute(CharTermAttribute.class);

        tokenizer0.reset();
        result1.reset();
        result2.reset();

        while(tokenizer0.incrementToken())
        {
            result1.incrementToken();
            result2.incrementToken();


            if(!new String(term1.buffer(),0,term1.length()).equals(new String(term.buffer(), 0, term.length())))
            {
                changedByAccent ++;
            }
            if(!new String(term2.buffer(),0,term2.length()).equals(new String(term.buffer(), 0, term.length())))
                        {
                changedByASCII ++;
            }


        }
        System.out.println("Accent      Filter changed "+ changedByAccent + " chars");
        System.out.println("ASCII       Filter changed "+ changedByASCII + " chars");
        System.out.println("ICU         Filter changed "+ changedByNFKC   + " chars");
        System.out.println("ASCIIICU    Filter changed "+ changedByASCIIAndNFKC   + " chars");
    }

    private void printAsHexAndValue(String term)
    {
        System.out.print("0x" + Integer.toHexString(Character.valueOf(term.charAt(0)))+ " " + term + ":");
    }
}
