<template>
  <div class="m-2 showcontainer row">
    <sectionFieldLabel
      class="col-4"
      :label="field.meta.name"
      :tooltip="field.description"
      :color="color"
    ></sectionFieldLabel>
    <div class="col-8">
      <component
        v-if="!isEmptyValue"
        :is="fieldTypeComponentName"
        :data="field.value"
        :metaData="field.meta"
        :color="color"
      />
      <div v-else>N/A</div>
    </div>
  </div>
</template>

<script>
import SectionFieldLabel from "./SectionFieldLabel.vue";
import SectionArrayValue from "./SectionArrayValue.vue";
import RefBackFieldValue from "./RefBackFieldValue.vue";
import RefArrayFieldValue from "./RefArrayFieldValue.vue";
import OntologyFieldValue from "./OntologyFieldValue.vue";
import RefFieldValue from "./RefFieldValue";
import TextFieldValue from "./TextFieldValue";
import {
  StringDisplay,
  FileDisplay,
  ObjectDisplay,
} from "@mswertz/emx2-styleguide";

export default {
  name: "SectionField",
  components: {
    SectionFieldLabel,
    SectionArrayValue,
    StringDisplay,
    TextFieldValue,
    FileDisplay,
    ObjectDisplay,
    RefBackFieldValue,
    RefArrayFieldValue,
    OntologyFieldValue,
    RefFieldValue,
  },
  props: {
    field: {
      type: Object,
      required: true,
    },
    color: {
      type: String,
      default: () => "primary",
    },
  },
  computed: {
    fieldTypeComponentName() {
      return {
        STRING: "StringDisplay",
        BOOL: "StringDisplay",
        INT: "StringDisplay",
        DECIMAL: "StringDisplay",
        FILE: "FileDisplay",
        DATE: "StringDisplay",
        UUID: "StringDisplay",
        DATETIME: "StringDisplay",
        HEADING: "StringDisplay",
        JSONB: "TextFieldValue",
        TEXT: "TextFieldValue",
        BOOL_ARRAY: "SectionArrayValue",
        UUID_ARRAY: "SectionArrayValue",
        STRING_ARRAY: "SectionArrayValue",
        TEXT_ARRAY: "SectionArrayValue",
        INT_ARRAY: "SectionArrayValue",
        DECIMAL_ARRAY: "SectionArrayValue",
        DATE_ARRAY: "SectionArrayValue",
        DATETIME_ARRAY: "SectionArrayValue",
        JSONB_ARRAY: "SectionArrayValue",
        ONTOLOGY_ARRAY: "SectionArrayValue",
        REF_ARRAY: "RefArrayFieldValue",
        REF: "RefFieldValue",
        REFBACK: "RefBackFieldValue",
        ONTOLOGY: "OntologyFieldValue",
      }[this.field.meta.columnType];
    },
    isEmptyValue() {
      return (
        this.field.value === undefined ||
        this.field.value === null ||
        (Array.isArray(this.field.value) && this.field.value.length === 0) ||
        (this.field.value &&
          Object.keys(this.field.value).length === 0 &&
          Object.getPrototypeOf(this.field.value) === Object.prototype)
      );
    },
  },
};
</script>
