package org.musicbrainz.search.servlet;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.musicbrainz.search.index.DatabaseIndex;
import org.musicbrainz.search.index.EditorIndexField;
import org.musicbrainz.search.servlet.mmd2.EditorWriter;

import java.io.IOException;
import java.util.ArrayList;

public class EditorSearch extends AbstractSearchServer {

  protected void setupDefaultFields() {
    defaultFields = new ArrayList<String>();
    defaultFields.add(EditorIndexField.EDITOR.getName());
    defaultFields.add(EditorIndexField.BIO.getName());
  }

  public EditorSearch() throws Exception {
    resultsWriter = new EditorWriter();
    setupDefaultFields();
    analyzer = DatabaseIndex.getAnalyzer(EditorIndexField.class);
  }

  public EditorSearch(SearcherManager searcherManager) throws Exception {
    this();
    this.searcherManager = searcherManager;
    setLastServerUpdatedDate();
    resultsWriter.setLastServerUpdatedDate(this.getServerLastUpdatedDate());
  }

  @Override
  public QueryParser getParser() {
    return new EditorQueryParser(defaultFields.toArray(new String[0]), analyzer);
  }

  @Override
  protected String printExplainHeader(Document doc) throws IOException, ParseException {
    return doc.get(EditorIndexField.EDITOR.getName()) + ':' + doc.get(EditorIndexField.EDITOR.getName()) + '\n';
  }

    /**
     *
     * @param searcher
     * @param topDocs
     * @param offset
     * @return
     * @throws java.io.IOException
     */
    protected Results processResults(IndexSearcher searcher, TopDocs topDocs, int offset) throws IOException
    {
        Results results = super.processResults(searcher, topDocs, offset);
        results.setResourceType(ResourceType.EDITOR);
        return results;
    }
}
