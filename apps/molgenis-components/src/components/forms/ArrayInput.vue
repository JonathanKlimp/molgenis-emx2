<template>
  <FormGroup
    :id="id + '-0'"
    :label="label"
    :required="required"
    description="description"
    :errorMessage="errorMessage"
  >
    <div v-for="(value, index) in values" :key="index">
      <component
        :is="inputType"
        :id="id + '-' + index"
        v-model="values[index]"
        :showAddButton="index === values.length"
      >
        <template v-slot:append>
          <button
            v-if="values.length > 1"
            @click="clearInput(index)"
            class="btn btn-outline-primary"
            type="button"
          >
            <i class="fas fa-fw fa-times"></i>
          </button>
          <button
            @click="addItem(index)"
            class="btn btn-outline-primary"
            type="button"
          >
            <i class="fas fa-fw fa-plus"></i>
          </button>
        </template>
      </component>
    </div>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import InputString from "./InputString.vue";

export default {
  name: "ArrayInput",
  extends: BaseInput,
  data() {
    return { values: this.value || [null] };
  },
  props: {
    columnType: {
      type: String,
      required: true,
    },
  },
  computed: {
    inputType() {
      return {
        STRING_ARRAY: InputString,
      }[this.columnType];
    },
  },
  methods: {
    addItem(index) {
      this.values.splice(index + 1, 0, null);
      this.$emit("input", this.values);
    },
    clearInput(index) {
      if (this.values.length > 1) {
        this.values.splice(index, 1);
      }
      this.$emit("input", this.values);
    },
  },
};
</script>

<docs>
<template>
  <demo-item>
    <div>
      <ArrayInput
          id="array-string-1"
          columnType="STRING_ARRAY"
          v-model="value"
      ></ArrayInput>
    </div>
    <div>
      {{ value }}
    </div>
  </demo-item>
</template>
<script>
  export default {
    methods: {
      alert(text) {
        alert(text);
      },
    },
    data() {
      return {
        value: ["String array value"],
      };
    },
  };
</script>
</docs>
