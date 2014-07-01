package com.codepurls.mailytics.data.pst;

import com.codepurls.mailytics.data.core.AbstractFolder;
import com.pff.PSTFolder;

public final class MSPSTFolder extends AbstractFolder<PSTFolder, MSPSTFolder> {
  public MSPSTFolder(PSTFolder f, MSPSTFolder parent) {
    super(f, parent);
  }

  public String getName() {
    return folder.getDisplayName();
  }

}