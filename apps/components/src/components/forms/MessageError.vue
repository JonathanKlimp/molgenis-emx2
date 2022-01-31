<template>
  <div class="alert alert-danger" role="alert">
    <slot />
  </div>
</template>

<script>
export default {
  name: "MessageError"
}
</script>

<docs>
```
<MessageError>Something bad</MessageError>
```
</docs>
