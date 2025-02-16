<template>
  <span>
    <span v-if="inplace && !focus && !errorMessage" @click="toggleFocus">
      <span v-if="list && value">{{ value.join(", ") }}</span>
      <span v-else> {{ value ? value : "&zwnj;&zwnj;" }}</span>
    </span>
    <FormGroup v-else v-bind="$props" v-on="$listeners">
      <InputAppend
        v-for="(item, idx) in valueArray"
        :key="idx"
        v-bind="$props"
        :showClear="false"
        @add="addRow"
        :showPlus="showPlus(idx)"
        :showMinus="showMinus(idx)"
      >
        <input
          v-focus="inplace && !list"
          type="number"
          step="1"
          :value="item"
          :class="{ 'form-control': true, 'is-invalid': errorMessage }"
          :aria-describedby="id + 'Help'"
          :placeholder="placeholder"
          :readonly="readonly"
          @keypress="keyhandler"
          @input="emitValue($event, idx)"
          @blur="toggleFocus"
        />
      </InputAppend>
    </FormGroup>
    <IconAction
      v-if="inplace && !focus"
      class="hoverIcon"
      icon="pencil-alt"
      @click="toggleFocus"
    />
  </span>
</template>

<script>
import BaseInput from "./_baseInput.vue";
import InputAppend from "./_inputAppend";
import IconAction from "./IconAction";
import { isNumericKey } from "./utils/InputUtils";

export default {
  extends: BaseInput,
  components: {
    InputAppend,
    FormGroup: () => import("./_formGroup"), //because it uses itself in nested form
    IconAction,
  },
  props: {
    parser: {
      default() {
        return parseInt;
      },
    },
  },
  methods: {
    keyhandler(event) {
      if (!isNumericKey(event)) event.preventDefault();
    },
  },
};
</script>

<style scoped>
.is-invalid {
  background-image: none;
  padding-right: 0.75rem;
}

span:hover .hoverIcon {
  visibility: visible;
}
</style>

<docs>
Example integer input
```
<template>
  <div>
    <InputInt v-model="value" label="My int input label" description="Some help needed?"/>
    You typed: {{ JSON.stringify(value) }}
    You typed with type: {{ typeof value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: null
      };
    }
  };
</script>
```
Example with default value
```
<template>
  <div>
    <InputInt
        v-model="value"
        label="My int input label"
        description="Some help needed?"
    />
    <br/>
    You typed: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: 3
      };
    }
  };
</script>
```
Example readonly
```
<InputString label="test" :readonly="true" value="can't change me" description="Should not be able to edit this"/>
```
Example list of integers
```
<template>
  <div>
    <InputInt v-model="value" :list="true" label="test"
                 description="should be able to manage a list of values"/>
    <br/>
    You typed: {{ JSON.stringify(value) }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: [1, 2, 3]
      };
    },
  };
</script>
```
Example in place
```
<template>
  <div>
    In place some
    <InputInt label="test" v-model="value" :inplace="true" description="Should be able to edit in place"/>
    text.<br/>
    value: {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {value: null}
    }
  }
</script>
```
Example list in place
```
<template>
  <div>
    In place some
    <InputInt label="test" :list="true" v-model="value" :inplace="true"
                 description="Should be able to edit in place"/>
    text.<br/>
    value: {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {value: [1, 2]}
    }
  }
</script>
```
</docs>
