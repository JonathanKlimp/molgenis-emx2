<template>
  <div v-if="variable" class="mt-2">
    <harmonization-definition
      v-for="repeatedVariable in repeats"
      :key="repeatedVariable.name"
      :variable="repeatedVariable"
    />
  </div>
  <div class="mt-2" v-else>
    <Spinner />
    Fetching data..
  </div>
</template>

<script>
import { Spinner } from "@mswertz/emx2-styleguide";
import HarmonizationDefinition from "../components/HarmonizationDefinition.vue";
import { mapActions } from "vuex";

export default {
  name: "ResourceHarmonizationDetails",
  components: { Spinner, HarmonizationDefinition },
  props: {
    name: String,
    network: String,
    version: String,
    sourceCohort: String,
    variable: Object,
  },
  data() {
    return {
      fromVariables: null,
    };
  },
  computed: {
    repeats() {
      const cohortMapping = !this.variable.mappings
        ? undefined
        : this.variable.mappings.find(
            (mapping) =>
              mapping.fromTable.dataDictionary.resource.pid ===
              this.sourceCohort
          );
      let repeats = [
        {
          ...this.variable,
          cohortMapping,
        },
      ];
      if (this.variable.repeats) {
        repeats = repeats.concat(
          this.variable.repeats.map((repeat) => {
            if (repeat.mappings) {
              repeat.cohortMapping = repeat.mappings.find(
                (mapping) =>
                  mapping.fromTable.dataDictionary.resource.pid ===
                  this.sourceCohort
              );
            }
            return repeat;
          })
        );
      }
      return repeats;
    },
  },
  methods: {
    ...mapActions(["fetchSchema"]),
  },
};
</script>

<style></style>
