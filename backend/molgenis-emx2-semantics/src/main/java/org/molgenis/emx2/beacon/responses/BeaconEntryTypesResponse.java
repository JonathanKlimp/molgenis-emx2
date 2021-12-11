package org.molgenis.emx2.beacon.responses;

import org.molgenis.emx2.beacon.common.BeaconInformationalResponseMeta;
import org.molgenis.emx2.beacon.common.EntryType;

import java.util.Map;

// https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/beaconEntryTypesResponse.json
public class BeaconEntryTypesResponse {

  // Information about the response that could be relevant for the Beacon client in order to
  // interpret the results.
  BeaconInformationalResponseMeta meta;

  // This is a dictionary of the entry types implemented in this Beacon instance.
  // https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/configuration/entryTypesSchema.json
  Map<String, EntryType> entryTypes;
}
