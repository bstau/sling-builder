package org.apache.sling.jcr.contentloader.internal;

import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.contentloader.ContentImportListener;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.*;
import java.io.*;
import java.util.UUID;

public class DefaultContentImporterTest {

    private final String FILE_PATH = "/Users/Petr/Development/sling/bundles/jcr/contentloader/src/test/resources/reader/node-content.jcr.xml";
    private DefaultContentImporter contentImporter;

    private Session session;
    private Node parentNode;

    @Before
    public void setup() throws RepositoryException {
        final SlingRepository repo = RepositoryProvider.instance().getRepository();
        session = repo.loginAdministrative(null);
        parentNode = session.getRootNode().addNode(getClass().getSimpleName()).addNode(uniqueId());

        contentImporter = new DefaultContentImporter();
    }

    @Test
    public void test() throws IOException, RepositoryException {
        ImportOptions options = U.createImportOptions(false, false, false, false, false);
        File jcrXml = new File(FILE_PATH);
        FileInputStream is = new FileInputStream(jcrXml);
        ContentImportListener listener = new CustomContentImportListener();

        contentImporter.importContent(parentNode, "node-content.jcr.xml", is, options, listener);
    }

    @After
    public void shutdown() throws RepositoryException {
        contentImporter = null;
        if(session != null) {
            session.save();
            session.logout();
            session = null;
        }
    }

    private final String uniqueId() {
        return getClass().getSimpleName() + UUID.randomUUID();
    }
}
