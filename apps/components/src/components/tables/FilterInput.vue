<template>
  <div style="min-width: 10em">
    <InputString
      :list="true"
      :or="true"
      v-if="
        columnType.startsWith('STRING') ||
        columnType.startsWith('TEXT') ||
        columnType.startsWith('UUID')
      "
      :value="conditions"
      @input="$emit('update:conditions', $event)"
    />
    <InputRangeInt
      :list="true"
      v-else-if="columnType.startsWith('INT')"
      :value="conditions"
      @input="$emit('update:conditions', $event)"
    />
    <InputRangeDecimal
      :list="true"
      v-else-if="columnType.startsWith('DECIMAL')"
      :value="conditions"
      @input="$emit('update:conditions', $event)"
    />
    <InputRangeDate
      :list="true"
      v-else-if="columnType.startsWith('DATE')"
      :value="conditions"
      @input="$emit('update:conditions', $event)"
    />
    <InputCheckbox
      :list="true"
      v-else-if="columnType.startsWith('BOOL')"
      :options="['true', 'false']"
      :value="conditions"
      @input="$emit('update:conditions', $event)"
    />
    <InputOntology
      :list="true"
      v-else-if="columnType.startsWith('ONTOLOGY')"
      :table="refTable"
      :value="conditions"
      @input="$emit('update:conditions', $event)"
      :limit="7"
      :show-expanded="true"
      :graphqlURL="graphqlURL"
    />
    <InputRef
      :list="true"
      v-else-if="columnType.startsWith('REF')"
      :table="refTable"
      :value="conditions"
      @input="$emit('update:conditions', $event)"
      :limit="7"
      :graphqlURL="graphqlURL"
    />
    <div v-else>ERROR {{ column }}</div>
  </div>
</template>

<script>
import InputCheckbox from "../forms/InputCheckbox.vue";
import InputString from "../forms/InputString.vue";
import InputRangeInt from "../forms/InputRangeInt.vue";
import InputRangeDecimal from "../forms/InputRangeDecimal.vue";
import InputRangeDate from "../forms/InputRangeDate.vue";
import InputRef from "../forms/InputRef.vue";
import InputOntology from "../forms/InputOntology.vue";

export default {
  components: {
    InputCheckbox,
    InputString,
    InputRangeInt,
    InputRangeDecimal,
    InputRangeDate,
    InputRef,
    InputOntology,
  },
  props: {
    columnType: String,
    refTable: String,
    conditions: Array,
    graphqlURL: String,
  },
};
</script>
