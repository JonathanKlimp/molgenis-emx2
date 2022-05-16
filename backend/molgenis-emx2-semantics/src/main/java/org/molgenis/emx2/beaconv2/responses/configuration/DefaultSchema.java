package org.molgenis.emx2.beaconv2.responses.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DefaultSchema {

  String id;
  String name;
  String referenceToSchemaDefinition;
  String schemaVersion;

  public DefaultSchema(String entryTypeSingular, String entryTypePlural) {
    this.id = "ga4gh-beacon-" + entryTypeSingular + "-v2.0.0-draft.4";
    this.name = "Default schema for " + entryTypePlural;
    this.referenceToSchemaDefinition = "./" + entryTypePlural + "/defaultSchema.json";
    this.schemaVersion = "v2.0.0-draft.4";
  }
}
