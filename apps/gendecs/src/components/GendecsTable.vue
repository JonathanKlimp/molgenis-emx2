<template>
  <div class="table-responsive">
    <table class="table table-bordered" :class="{ 'table-hover': tableHover }" id="gendecs-table">
      <thead>
      <tr>
        <th scope="col" style="width: 1px" v-if="hasColheader">
          <slot name="colheader" />
        </th>
        <th v-for="col in columns" :key="col" scope="col">
          {{ col }}
        </th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="row in rows" :key="JSON.stringify(row)">
        <td
            v-for="col in columns"
            :key="col"
            @click="onRowClick(row)"
            style="cursor: pointer"
        >
          <ul v-if="Array.isArray(row[col])" class="list-unstyled">
            <li v-for="(item, index3) in row[col]" :key="index3">
              <div v-if="col === 'Diseases'">
                <a v-if="item.includes('omim')" :href="item" target="_blank">link to OMIM</a>
                <a v-if="item.includes('orpha')" :href="item" target="_blank">link to ORPHA</a>
              </div>

              <a v-if="col === 'ClinVar' && item !== ''"
                    :href="'https://www.ncbi.nlm.nih.gov/clinvar/?term=' + item + '[alleleid]' + item" target="_blank">link to ClinVar</a>
              <p v-if="col ==='ClinVar' && item === ''">No ClinVar match</p>

              <p v-else-if="col !== 'ClinVar' && col !== 'Diseases'"> {{item}}</p>
            </li>
          </ul>
          <span v-else-if="row[col]">{{ flattenObject(row[col]) }}</span>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  props: {
    columns: Array,
    rows: Array,
    selectColumn: String,
    defaultValue: Array,
  },
  data: function () {
    return {
      selectedItems: [],
    };
  },
  watch: {
    selectedItems() {
      this.$emit("input", this.selectedItems);
    },
  },
  created() {
    if (this.defaultValue instanceof Array) {
      this.selectedItems = this.defaultValue;
    } else {
      this.selectedItems.push(this.defaultValue);
    }
  },
  mounted() {
    this.sortTable();
  },
  computed: {
    tableHover() {
      return this.selectColumn || (this.$listeners && this.$listeners.click);
    },
    hasColheader() {
      return (
          this.selectColumn ||
          !!this.$slots["colheader"] ||
          !!this.$scopedSlots["colheader"] ||
          !!this.$slots["rowheader"] ||
          !!this.$scopedSlots["rowheader"]
      );
    },
  },
  methods: {
    flattenObject(object) {
      let result = "";
      if (typeof object === "object") {
        Object.keys(object).forEach((key) => {
          if (object[key] === null) {
            //nothing
          } else if (typeof object[key] === "object") {
            result += this.flattenObject(object[key]);
          } else {
            result += " " + object[key];
          }
        });
      } else {
        result = object;
      }
      return result.replace(/^\./, "");
    },
    isSelected(row) {
      return (
          this.selectedItems != null &&
          this.selectedItems.includes(row[this.selectColumn])
      );
    },
    onRowClick(row) {
      if (this.selectColumn) {
        if (this.isSelected(row)) {
          this.selectedItems = this.selectedItems.filter(
              (item) => item !== row[this.selectColumn]
          );
          this.$emit("deselect", row[this.selectColumn]);
        } else {
          this.selectedItems.push(row[this.selectColumn]);
          this.$emit("select", row[this.selectColumn]);
        }
      } else {
        this.$emit("click", row);
      }
    },
    sortTable() {
      let table, rows, switching, i, x, y, shouldSwitch;
      table = document.getElementById("gendecs-table");
      switching = true;
      while (switching) {
        switching = false;
        rows = table.rows;
        for (i = 1; i < (rows.length - 1); i++) {
          shouldSwitch = false;

          x = rows[i].getElementsByTagName("ul")[0];
          y = rows[i + 1].getElementsByTagName("ul")[0];

          let xLength = x.getElementsByTagName("p").length;
          let yLength = y.getElementsByTagName("p").length;

          // Check if the two rows should switch place
          if (xLength < yLength) {
            shouldSwitch = true;
            break;
          }
        }
        if (shouldSwitch) {
          /* If a switch has been marked, make the switch
          and mark that a switch has been done: */
          rows[i].parentNode.insertBefore(rows[i + 1], rows[i]);
          switching = true;
        }
      }
    }
  },
};
</script>

<style scoped>
th,
td {
  text-align: left;
}
#gendecs-table {
  /*width: 75%;*/
  margin: 0 auto;
}
</style>