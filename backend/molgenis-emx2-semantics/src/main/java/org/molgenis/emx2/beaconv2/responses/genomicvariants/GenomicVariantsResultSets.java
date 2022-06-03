package org.molgenis.emx2.beaconv2.responses.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GenomicVariantsResultSets {

  String id;
  String type;
  String setType;
  Boolean exists;
  Integer resultsCount;
  GenomicVariantsResultSetsItem[] results;

  // see
  // https://github.com/ga4gh-beacon/beacon-v2-Models/blob/94bd059442c386c8306b08b34ec7db547d6df13d/BEACON-V2-Model/genomicVariations/examples/genomicVariant-MID-example.json
  public GenomicVariantsResultSets(
      String id, int resultsCount, GenomicVariantsResultSetsItem[] results) {
    this.id = id;
    this.type = "dataset";
    this.setType = "genomicVariant";
    this.exists = true;
    this.resultsCount = results.length;
    this.results = results;
  }
}
