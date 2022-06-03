package org.molgenis.emx2.beaconv2.responses.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Position {

  String assemblyId;
  String refseqId;
  Long[] start;
  Long[] end;

  public Position() {
    super();
  }

  public Position(String assemblyId, String refseqId, Long[] start, Long[] end) {
    this.assemblyId = assemblyId;
    this.refseqId = refseqId;
    this.start = start;
    this.end = end;
  }
}
