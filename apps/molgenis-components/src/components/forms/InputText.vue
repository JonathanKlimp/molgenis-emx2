<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage"
  >
    <ResizableTextarea>
      <textarea
        :id="id"
        :value="value"
        class="form-control"
        :class="{ 'is-invalid': errorMessage }"
        :aria-describedby="id + 'Help'"
        :placeholder="placeholder"
        :readonly="readonly"
        @input="$emit('input', $event.target.value)"
      />
    </ResizableTextarea>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import ResizableTextarea from "./ResizableTextarea.vue";

export default {
  extends: BaseInput,
  components: {
    FormGroup,
    ResizableTextarea,
  },
};
</script>

<docs>
<template>
  <div>
    <label>Empty input text</label>
    <demo-item>
      <InputText
          id="input-text"
          v-model="value"
          label="My text label"
          placeholder="type here your text"
          description="Some help needed?"
      />
      You typed: {{ value }}
    </demo-item>
    <label>Empty input text</label>
    <demo-item>
      <InputText
          id="input-text2"
          v-model="value2"
          label="My text label"
          placeholder="type here your text"
          description="This should have default value?"
      />
    </demo-item>
    <demo-item>
      <InputText
          id="input-text3"
          v-model="value2"
          label="My readonly text"
          readonly
      />
    </demo-item>
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: null,
        value2: "this is a default value"
      };
    }
  };
</script>
</docs>
