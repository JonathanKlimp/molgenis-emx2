<template>
  <InputRef
    :id="id"
    :value="condition"
    @input="onUpdateCondition"
    :tableName="tableName"
    :graphqlURL="graphqlURL"
  />
</template>

<script>
import InputRef from "../forms/InputRef.vue";

export default {
  name: "RefFilter",
  components: { InputRef },
  props: {
    id: {
      type: String,
      required: true,
    },
    condition: {
      type: Object,
      required: false,
    },
    graphqlURL: {
      required: false,
      type: String,
    },
    tableName: {
      type: String,
      required: true,
    },
  },
  methods: {
    onUpdateCondition(newValue) {
      if (newValue === null) {
        this.$emit("clearCondition", newValue);
      } else {
        this.$emit("updateCondition", newValue);
      }
    },
  },
};
</script>
