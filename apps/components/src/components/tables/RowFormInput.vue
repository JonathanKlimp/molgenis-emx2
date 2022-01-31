<template>
  <div>
    <InputHeading
      v-if="columnType === 'HEADING'"
      v-bind="$props"
      v-on="$listeners"
    />
    <InputString
      v-else-if="columnType === 'STRING'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputText
      v-else-if="columnType === 'TEXT'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputInt
      v-else-if="columnType === 'INT'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputDecimal
      v-else-if="columnType === 'DECIMAL'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputBoolean
      v-else-if="columnType === 'BOOL'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputOntology
      v-else-if="columnType === 'ONTOLOGY' || columnType === 'ONTOLOGY_ARRAY'"
      v-bind="$props"
      v-model="input"
      :table="table"
      :list="columnType === 'ONTOLOGY_ARRAY'"
      v-on="$listeners"
    />
    <InputRefSelect
      v-else-if="columnType === 'REF'"
      v-bind="$props"
      v-model="input"
      :filter="filter"
      :table="table"
      v-on="$listeners"
    />
    <InputDate
      v-else-if="columnType === 'DATE'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputDateTime
      v-else-if="columnType === 'DATETIME'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputRef
      :list="true"
      :multiple-columns="true"
      :max-num="35"
      v-else-if="
        columnType === 'REF_ARRAY' ||
        columnType === 'MREF' ||
        refBackType === 'REF_ARRAY'
      "
      v-bind="$props"
      v-model="input"
      :table="table"
      :graphqlURL="graphqlURL"
      v-on="$listeners"
    />
    <inputRefback
      v-else-if="refBackType == 'REF'"
      v-bind="$props"
      :table="table"
    />
    <InputString
      v-else-if="columnType === 'STRING_ARRAY'"
      :list="true"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputText
      v-else-if="columnType === 'TEXT_ARRAY'"
      :list="true"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputFile
      v-else-if="columnType === 'FILE'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <div v-else>UNSUPPORTED TYPE '{{ columnType }}'</div>
  </div>
</template>

<script>
import BaseInput from "../forms/BaseInput.vue";
import InputString from "../forms/InputString.vue";
import InputInt from "../forms/InputInt.vue";
import InputDecimal from "../forms/InputDecimal.vue";
import InputBoolean from "../forms/InputBoolean.vue";
import InputDate from "../forms/InputDate.vue";
import InputDateTime from "../forms/InputDateTime.vue";
import InputFile from "../forms/InputFile.vue";
import InputText from "../forms/InputText.vue";
import InputHeading from "../forms/InputHeading.vue";
import InputRef from "../forms/InputRef.vue";
import InputRefback from "../forms/InputRefback.vue";
import InputRefSelect from "../forms/InputRefSelect.vue";

export default {
  name: "RowFormInput",
  extends: BaseInput,
  props: {
    /** enable editing of label and description*/
    editMeta: Boolean,
    schema: String,
    columnType: String,
    description: String,
    filter: Object,
    table: String,
    refLabel: String,
    refBack: String,
    refBackType: String,
    pkey: Object,
    graphqlURL: {
      default: "graphql",
      type: String,
    },
  },
  data() {
    return {
      input: null,
    };
  },
  components: {
    InputString,
    InputInt,
    InputDecimal,
    InputBoolean,
    InputRefSelect, 
    InputDate,
    InputDateTime,
    InputFile,
    InputText,
    InputHeading,
    // InputOntology,
    // InputRef,
    InputRefback,
  },
  created() {
    this.input = this.value;
  },
  watch: {
    value() {
      this.input = this.value;
    },
    input() {
      this.$emit("input", this.input);
    },
  },
};
</script>

