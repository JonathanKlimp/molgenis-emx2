<template>
  <InputRangeDecimal :id="id" :value="condition" @input="onUpdateCondition">
    <template v-slot:append>
      <button v-if="condition" @click="$emit('clearCondition')" class="btn btn-outline-primary" type="button">
        <i class="fas fa-fw fa-times"></i>
      </button>
      <button v-if="showAddButton && condition" @click="$emit('addCondition')" class="btn btn-outline-primary" type="button">
        <i class="fas fa-fw fa-plus"></i>
      </button>
    </template>
  </InputRangeDecimal>
</template>

<script>
import InputRangeDecimal from "../forms/InputRangeDecimal.vue";

export default {
  name: "DecimalFilter",
  components: { InputRangeDecimal },
  props: {
    id: {
      type: String,
      required: true,
    },
    name: {
      type: String,
      required: false,
      default: function () {
        return this.id;
      },
    },
    condition: {
      type: Array,
      required: false,
    },
    showAddButton: {
      type: Boolean,
      required: false,
      default: () => false
    }
  },
  methods: {
    onUpdateCondition($event) {
      this.$emit("updateCondition", $event);
    },
  },
};
</script>
