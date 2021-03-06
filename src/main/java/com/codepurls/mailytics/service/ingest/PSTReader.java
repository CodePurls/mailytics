package com.codepurls.mailytics.service.ingest;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codepurls.mailytics.data.pst.MSPSTFolder;
import com.codepurls.mailytics.data.pst.MSPSTMail;
import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTMessage;

public class PSTReader implements MailReader {
  public static final Logger LOG = LoggerFactory.getLogger(PSTReader.class);
  private PSTFile pstFile;

  public void visit(MailReaderContext context, File file, MailVisitor visitor) {
    try {
      pstFile = new PSTFile(file);
//      boolean passwordProtected = pstFile.getMessageStore().isPasswordProtected();
//      if (passwordProtected) {
//        LOG.warn("File {} is password protected, parser will not work");
//      }
      PSTFolder rootFolder = pstFile.getRootFolder();
      walk(null, rootFolder, visitor);
    } catch (PSTException | IOException e) {
      throw new MailReaderException(format("Error opening %s", file), e);
    }
  }

  public void visit(MailReaderContext context, String file, MailVisitor visitor) {
    visit(context, new File(file), visitor);
  }

  private void walk(MSPSTFolder parent, PSTFolder f, MailVisitor visitor) {
    MSPSTFolder folder = new MSPSTFolder(f, parent);
    visitor.onNewFolder(folder);
    try {
      if (f.getContentCount() > 0) {
        PSTMessage email = (PSTMessage) f.getNextChild();
        while (email != null) {
          visitor.onNewMail(new MSPSTMail(folder, email));
          email = (PSTMessage) f.getNextChild();
        }
      }
      if (f.hasSubfolders()) {
        f.getSubFolders().forEach((cf) -> {
          try {
            walk(folder, cf, visitor);
          } catch (Exception e) {
            visitor.onError(e, folder, null);
          }
        });
      }

    } catch (PSTException | IOException e) {
      visitor.onError(e, folder, null);
    }
  }

  public void close() throws IOException {
    pstFile.getFileHandle().close();
  }

}
