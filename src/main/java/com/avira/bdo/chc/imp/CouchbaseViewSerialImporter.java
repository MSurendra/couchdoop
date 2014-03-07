package com.avira.bdo.chc.imp;

import com.avira.bdo.chc.ArgsException;
import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CouchbaseViewSerialImporter {

  private static final String PAGE_FILE_BASENAME = "part";

  private static final Logger LOGGER = LoggerFactory.getLogger(CouchbaseViewSerialImporter.class);

  public void start(String[] args)
      throws ArgsException {
    Configuration conf = new Configuration();
    ImportViewArgs iva;
    try {
      iva = new ImportViewArgs(conf, args);
    } catch (ArgsException e) {
      System.exit(1);
      return;
    }

    // Connect to couchbase and get the view.
    CouchbaseClient couchbaseClient;
    try {
      couchbaseClient = connectToCouchbase(iva.getUrls(), iva.getBucket(), iva.getPassword());
    } catch (IOException e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
      return;
    }
    View view = couchbaseClient.getView(iva.getDesignDocumentName(), iva.getViewName());

    int pageNo = 0;
    for (String viewKey : iva.getViewKeys()) {
      LOGGER.info("___________________________________");
      LOGGER.info("Importing documents for view key " + viewKey + ".");

      Query query = new Query();
      query.setKey(viewKey);
      query.setIncludeDocs(true);

      Paginator pages = couchbaseClient.paginatedQuery(view, query, iva.getDocumentsPerPage());
      ViewResponse response;
      PageFileWriter writer = null;

      try {
        while (pages.hasNext()) {
          LOGGER.info("Writing page " + pageNo + "...");

          // Get page rows.
          response = pages.next();

          // Prepare the object which writes the page to a file.
          writer = new PageFileWriter(conf, iva.getOutput(), PAGE_FILE_BASENAME, pageNo);

          // Iterate on each row.
          for (ViewRow row : response) {
            String key = row.getId();
            String doc = row.getDocument().toString();

            LOGGER.debug("Writing document with ID " + row.getId() + "...");
            writer.write(key, doc);
          }

          // Prepare for the next page.
          writer.close();
          pageNo++;
        }
      } catch (IOException e) {
        LOGGER.error(ExceptionUtils.getStackTrace(e));
      } finally {
        if (writer != null) {
          try {
            writer.close();
          } catch (IOException e) {
            LOGGER.error(ExceptionUtils.getStackTrace(e));
          }
        }
      }
    }

    LOGGER.info("Disconnecting from Couchbase...");
    couchbaseClient.shutdown();
  }

  protected CouchbaseClient connectToCouchbase(List<URI> couchbaseUrls, String couchbaseBucket, String couchbasePassword)
      throws IOException {
    CouchbaseClient couchbaseClient;

    LOGGER.info("Connecting to Couchbase...");
    try {
      couchbaseClient = new CouchbaseClient(couchbaseUrls, couchbaseBucket, couchbasePassword);
      LOGGER.info("Connected to Couchbase.");
    } catch (IOException e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
      throw e;
    }

    return couchbaseClient;
  }
}
