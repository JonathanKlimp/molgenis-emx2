package org.molgenis.emx2.beaconv2.endpoints.runs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RunsResultSetsItem {

  private String id;
  private String biosampleId;
  private String individualId;
  private String runDate;
  private OntologyTerm librarySource;
  private String librarySelection;
  private String libraryStrategy;
  private String libraryLayout;
  private String platform;
  private OntologyTerm platformModel;

  public void setId(String id) {
    this.id = id;
  }

  public void setBiosampleId(String biosampleId) {
    this.biosampleId = biosampleId;
  }

  public void setIndividualId(String individualId) {
    this.individualId = individualId;
  }

  public void setRunDate(String runDate) {
    this.runDate = runDate;
  }

  public void setLibrarySource(OntologyTerm librarySource) {
    this.librarySource = librarySource;
  }

  public void setLibrarySelection(String librarySelection) {
    this.librarySelection = librarySelection;
  }

  public void setLibraryStrategy(String libraryStrategy) {
    this.libraryStrategy = libraryStrategy;
  }

  public void setLibraryLayout(String libraryLayout) {
    this.libraryLayout = libraryLayout;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public void setPlatformModel(OntologyTerm platformModel) {
    this.platformModel = platformModel;
  }
}
