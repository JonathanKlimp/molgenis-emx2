package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndividualsResultSetsItem {

  private String id;
  private OntologyTerm sex;
  private OntologyTerm ethnicity;
  private OntologyTerm geographicOrigin;
  private Diseases[] diseases;
  private Measures[] measures;

  public void setId(String id) {
    this.id = id;
  }

  public void setSex(OntologyTerm sex) {
    this.sex = sex;
  }

  public void setEthnicity(OntologyTerm ethnicity) {
    this.ethnicity = ethnicity;
  }

  public void setGeographicOrigin(OntologyTerm geographicOrigin) {
    this.geographicOrigin = geographicOrigin;
  }

  public void setDiseases(Diseases[] diseases) {
    this.diseases = diseases;
  }

  public void setMeasures(Measures[] measures) {
    this.measures = measures;
  }
}
