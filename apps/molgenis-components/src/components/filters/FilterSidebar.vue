<template>
  <div class="sidebar-container">
    <FilterContainer
      v-for="(filter, index) in visibleFilters"
      :key="filter.name"
      :title="filter.name"
      :conditions="filter.conditions"
    >
      <FilterInput
        :id="'filter-' + filter.name"
        :conditions="filters[index].conditions"
        @updateConditions="handleUpdateFilter(index, $event)"
        :columnType="filter.columnType"
        :tableName="filter.tableName"
        :graphqlURL="filter.graphqlURL"
      />
    </FilterContainer>
  </div>
</template>

<style scoped>
.sidebar-container {
  min-width: 16rem;
}
</style>

<script>
import FilterContainer from "./FilterContainer.vue";
import FilterInput from "./FilterInput.vue";

export default {
  name: "FilterSidebar",
  components: {
    FilterInput,
    FilterContainer,
  },
  props: {
    filters: Array,
  },
  computed: {
    visibleFilters() {
      return this.filters.filter(
        (column) => column.showFilter && column.columnType !== "HEADING"
      );
    },
  },
  methods: {
    handleUpdateFilter(index, event) {
      let newFilters = [...this.filters];
      newFilters[index].conditions = event;
      this.$emit("updateFilters", newFilters);
    },
  },
};
</script>

<docs>
<template>
  <demo-item>
    <div class="row">
      <div class="col-4">
        <FilterSidebar :filters="filters" @updateFilters="onUpdate"/>
      </div>
      <div class="col-8">
        <FilterWells :filters="filters" @updateFilters="onUpdate"/>
        <pre>{{ filters }}</pre>
      </div>
    </div>
  </demo-item>
</template>
<script>
  export default {
    data: function () {
      return {
        filters: [
          {
            name: "orderId",
            pkey: true,
            columnType: "STRING",
            showFilter: true,
            conditions: ["test123"]
          },
          {
            name: "pets",
            columnType: "REF",
            showFilter: true,
            expanded: true,
            conditions: [],
            tableName: "Pet",
            graphqlURL: "/pet store/graphql"
          },
          {
            name: "quantity",
            columnType: "INT",
            showFilter: true,
            conditions: []
          },
          {
            name: "price",
            columnType: "DECIMAL",
            showFilter: true,
            conditions: []
          },
          {
            name: "complete",
            columnType: "BOOL",
            showFilter: true,
            conditions: []
          },
          {
            name: "status",
            columnType: "STRING",
            showFilter: true,
            conditions: []
          },
          {
            name: "birthday",
            columnType: "DATE",
            showFilter: true,
            conditions: []
          },
          {
            name: "tags",
            columnType: "ONTOLOGY_ARRAY",
            showFilter: true,
            conditions: [],
            tableName: "Tag",
            graphqlURL: "/pet store/graphql"
          },
        ],
      };
    },
    methods: {
      showAlert() {
        alert("clicked");
      },
      onUpdate(update) {
        this.filters = update
      }
    },
  };
</script>
</docs>
