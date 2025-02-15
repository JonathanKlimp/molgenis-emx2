package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.ColumnType.REFBACK;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;
import static org.molgenis.emx2.sql.SqlColumnRefArrayExecutor.createRefArrayConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefArrayExecutor.removeRefArrayConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefBackExecutor.createRefBackColumnConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefBackExecutor.removeRefBackConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefExecutor.createRefConstraints;
import static org.molgenis.emx2.sql.SqlTypeUtils.getPsqlType;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Table;
import org.molgenis.emx2.*;

public class SqlColumnExecutor {
  private SqlColumnExecutor() {
    // hide
  }

  public static void executeSetRequired(DSLContext jooq, Column column) {
    boolean isRequired = column.isRequired();
    if (column.isRefback()) {
      isRequired = false;
    } else if (column.isReference()) {
      isRequired = column.getReferences().stream().allMatch(Reference::isRequired) && isRequired;
    } else
    // if has default, we will first update all 'null' to default
    if (column.isRequired() && column.getDefaultValue() != null) {
      jooq.update(column.getJooqTable())
          .set(column.getJooqField(), column.getDefaultValue())
          .where(column.getJooqField().isNull())
          .execute();
    }
    // in case of FILE we have to add all parts
    if (column.isFile()) {
      for (Field<?> f : column.getJooqFileFields()) {
        executeSetRequired(jooq, column.getJooqTable(), f, column.isRequired());
      }
    }
    // simply add set nullability
    else {
      if (column.isReference()) {
        for (Reference ref : column.getReferences()) {
          executeSetRequired(jooq, column.getJooqTable(), ref.getJooqField(), isRequired);
        }
      } else {
        executeSetRequired(jooq, column.getJooqTable(), column.getJooqField(), isRequired);
      }
    }
  }

  private static void executeSetRequired(
      DSLContext jooq, Table<?> table, Field<?> field, boolean required) {
    if (required) {
      jooq.alterTable(table).alterColumn(field).setNotNull().execute();
    } else {
      jooq.alterTable(table).alterColumn(field).dropNotNull().execute();
    }
  }

  public static String getJoinTableName(Column column) {
    return column.getTable().getTableName() + "-" + column.getName();
  }

  static void executeAlterName(DSLContext jooq, Column oldColumn, Column newColumn) {
    // asumes validated before
    if (!oldColumn.getName().equals(newColumn.getName())) {
      if (newColumn.isFile()) {
        for (String suffix : new String[] {"", "_extension", "_size", "_contents", "_mimetype"}) {
          jooq.execute(
              "ALTER TABLE {0} RENAME COLUMN {1} TO {2}",
              newColumn.getJooqTable(),
              field(name(oldColumn.getName() + suffix)),
              field(name(newColumn.getName() + suffix)));
        }
      } else {
        jooq.execute(
            "ALTER TABLE {0} RENAME COLUMN {1} TO {2}",
            newColumn.getJooqTable(),
            field(name(oldColumn.getName())),
            field(name(newColumn.getName())));
      }
    }
  }

  static void executeAlterType(DSLContext jooq, Column oldColumn, Column newColumn) {
    Table<?> table = newColumn.getTable().getJooqTable();

    if (oldColumn.getColumnType().getBaseType().equals(newColumn.getColumnType().getBaseType())) {
      return; // nothing to do
    }

    // catch cases we do not support
    if (oldColumn.isFile() && !newColumn.isFile() || !oldColumn.isFile() && newColumn.isFile()) {
      throw new MolgenisException(
          "Alter type for column '"
              + newColumn.getName()
              + "' failed: Cannot convert from or to binary");
    }

    // pre changes
    if (oldColumn.isRefArray()) {
      // if ref_array drop the index
      jooq.execute(
          "DROP INDEX {0}",
          name(oldColumn.getSchemaName(), table.getName() + "/" + oldColumn.getName()));
    }

    // change the type
    if (newColumn.isHeading() && !oldColumn.isHeading()) {
      // if changed to heading, drop
      executeRemoveColumn(jooq, oldColumn);
    } else if (oldColumn.isHeading() && !newColumn.isHeading()) {
      executeCreateColumn(jooq, newColumn);
    } else if (!newColumn.isHeading()) {
      alterField(
          jooq,
          table,
          oldColumn.getName(),
          oldColumn.getJooqField().getDataType(),
          newColumn.getJooqField().getDataType(),
          getPsqlType(newColumn));
    }

    // post changes
    if (newColumn.isRefArray()) {
      executeCreateRefArrayIndex(jooq, table, newColumn.getJooqField());
    }

    SqlTableMetadataExecutor.updateSearchIndexTriggerFunction(
        jooq, newColumn.getTable(), newColumn.getTableName());
  }

  static void alterField(
      DSLContext jooq,
      Table<?> table,
      String columnName,
      DataType<?> oldType,
      DataType<?> newType,
      String postgresType) {

    // change the raw type
    if (!newType.equals(oldType)) {
      if (newType.isArray() && !oldType.isArray()) {
        jooq.execute(
            "ALTER TABLE {0} ALTER COLUMN {1} TYPE {2} USING array[{1}::{3}]",
            table,
            name(columnName),
            keyword(postgresType),
            keyword(postgresType.replace("[]", ""))); // non-array type needed
      } else {
        jooq.execute(
            "ALTER TABLE {0} ALTER COLUMN {1} TYPE {2} USING {1}::{2}",
            table, name(columnName), keyword(postgresType));
      }
    }
  }

  static void reapplyRefbackContraints(Column oldColumn, Column newColumn) {
    if ((oldColumn.isRef() || oldColumn.isRefArray())
        && (newColumn.isRef() || newColumn.isRefArray())) {
      for (Column check : oldColumn.getRefTable().getNonInheritedColumns()) {
        if (check.isRefback() && oldColumn.getName().equals(check.getRefBack())) {
          check.getTable().dropColumn(check.getName());
          check.getTable().add(check);
        }
      }
    }
  }

  static void executeRemoveRefback(Column oldColumn, Column newColumn) {
    if ((oldColumn.isRef() || oldColumn.isRefArray())
        && !(newColumn.isRef() || newColumn.isRefArray())) {
      for (Column check : oldColumn.getRefTable().getColumns()) {
        if (check.isRefback() && oldColumn.getName().equals(check.getRefBack())) {
          check.getTable().dropColumn(check.getName());
        }
      }
    }
  }

  static void executeCreateColumn(DSLContext jooq, Column column) {
    String current = column.getName(); // for composite ref errors
    try {
      // create the column
      if (column.isReference()) {
        if (column.isOntology()) {
          createOntologyTable(column);
        }
        for (Reference ref : column.getReferences()) {
          current = ref.getName();
          // check if reference name already exists, composite ref may reuse columns
          // either other column, or a part of a reference
          if (!ref.isOverlapping()) {
            jooq.alterTable(column.getJooqTable()).addColumn(ref.getJooqField()).execute();
            if (column.isRefArray()) {
              executeCreateRefArrayIndex(jooq, column.getJooqTable(), ref.getJooqField());
            }
          }
        }

        // we only have hard not null in case of primary key
        if (column.isPrimaryKey()) {
          executeSetRequired(jooq, column);
        }
      } else if (column.isFile()) {
        for (Field<?> f : column.getJooqFileFields()) {
          jooq.alterTable(column.getJooqTable()).addColumn(f).execute();
        }
      } else if (!column.isHeading()) {
        jooq.alterTable(column.getJooqTable()).addColumn(column.getJooqField()).execute();
        executeSetDefaultValue(jooq, column);

        // we only have hard not null in case of primary key
        if (column.isPrimaryKey()) {
          executeSetRequired(jooq, column);
        }
      }

      saveColumnMetadata(jooq, column);

      // update the metaData with added column and put back schema info
      var tableMetadata =
          MetadataUtils.loadTable(jooq, column.getSchemaName(), column.getTableName());
      tableMetadata.setSchema(column.getSchema());

      // central constraints
      SqlTableMetadataExecutor.updateSearchIndexTriggerFunction(
          jooq, tableMetadata, column.getTableName());
    } catch (Exception e) {
      if (e.getMessage() != null && e.getMessage().contains("null values")) {
        throw new MolgenisException(
            "Create column '"
                + column.getTableName()
                + "."
                + current
                + "' failed:"
                + e.getMessage()
                + ". You might want to set nullable=TRUE or add a default value to update existing rows.");
      } else {
        throw new MolgenisException(
            "Create column '" + column.getTableName() + "." + current + "' failed", e);
      }
    }
  }

  public static void createOntologyTable(Column column) {
    SchemaMetadata schema = column.getSchema();
    if (column.getRefTableName() == null) {
      throw new MolgenisException(
          String.format(
              "Create of column '%s.%s' failed: refTable must be defined for columns of type '%s'",
              column.getTableName(), column.getName(), column.getColumnType()));
    }
    // check table doesn't exist
    SchemaMetadata refSchema = schema;
    if (column.getRefSchema() != null) {
      if (schema.getDatabase().getSchema(column.getRefSchema()) == null) {
        throw new MolgenisException(
            "refSchema '" + column.getRefSchema() + "' does not exist or permission denied");
      }
      refSchema = schema.getDatabase().getSchema(column.getRefSchema()).getMetadata();
    }
    if (refSchema.getTableMetadata(column.getRefTableName()) == null) {
      TableMetadata tm =
          new TableMetadata(column.getRefTableName())
              .setDescription(column.getDescription())
              .setTableType(TableType.ONTOLOGIES)
              .add(
                  column("order").setType(INT).setDescription("Order within the code system"),
                  column("name")
                      .setPkey()
                      .setRequired(true)
                      .setDescription("User friendly name for this code"),
                  column("codesystem")
                      // .setKey(2) //todo: ideally, combination of code+codesystem has a unique
                      // check
                      .setRequired(false)
                      .setDescription(
                          "Abbreviation of the code system/ontology this ontology term belongs to"),
                  column("code")
                      // .setKey(2)
                      .setRequired(false)
                      .setDescription(
                          "Identifier used for this code within this code system/ontology"),
                  column("parent")
                      .setType(REF)
                      .setRefTable(column.getRefTableName())
                      .setDescription("Parent in case this code exists in a hierarchy"),
                  column("ontologyTermURI")
                      // .setKey(3) //todo: ideally, has a unique check
                      .setRequired(false)
                      .setDescription("Reference to structured definition of this term"),
                  column("definition").setType(TEXT).setDescription("Definition of the term"),
                  column("children")
                      .setType(REFBACK)
                      .setRefTable(column.getRefTableName())
                      .setRefBack("parent"));

      // create the table
      refSchema.create(tm);
    }
  }

  static void validateColumn(Column c) {
    if (c.getName() == null) {
      throw new MolgenisException("Add column failed: Column name cannot be null");
    }
    if (c.getKey() > 0 && c.getTable().getKeyFields(c.getKey()).size() > 1 && !c.isRequired()) {
      throw new MolgenisException(
          "unique on column '"
              + c.getTableName()
              + "."
              + c.getName()
              + "' failed: When key spans multiple columns, none of the columns can be nullable");
    }
    if (c.isReference() && !c.isOntology() && c.getRefTable() == null) {
      throw new MolgenisException(
          String.format(
              "Add column '%s.%s' failed: 'refTable' required for columns of type REF, REF_ARRAY, REFBACK",
              c.getTableName(), c.getName()));
    }
    if (c.getRefLink() != null) {
      if (c.getTable().getColumn(c.getRefLink()) == null) {
        throw new MolgenisException(
            String.format(
                "Add column '%s.%s' failed: refLink '%s' column cannot be found",
                c.getTableName(), c.getName(), c.getRefLink()));
      }
      Column refLink = c.getTable().getColumn(c.getRefLink());
      if (!refLink.isRef() && !refLink.isRefArray()) {
        throw new MolgenisException(
            String.format(
                "Add column '%s.%s' failed: refLink %s is not a REF,REF_ARRAY",
                c.getTableName(), c.getName(), c.getRefLink()));
      }
    }
    // fix required
    if (c.getKey() == 1 && !c.isRequired()) {
      c.setRequired(true);
    }
  }

  private static void executeCreateRefArrayIndex(DSLContext jooq, Table<?> table, Field<?> field) {
    jooq.execute(
        "CREATE INDEX {0} ON {1} USING GIN( {2} )",
        name(table.getName() + "/" + field.getName()), table, field);
  }

  static void executeCreateRefConstraints(DSLContext jooq, Column column) {
    // set constraints
    if (column.isRef()) {
      createRefConstraints(jooq, column);
    } else if (column.isRefArray()) {
      createRefArrayConstraints(jooq, column);
    } else if (column.isRefback()) {
      createRefBackColumnConstraints(jooq, column);
    }
  }

  static void executeRemoveColumn(DSLContext jooq, Column column) {
    executeRemoveRefConstraints(jooq, column);
    if (column.isFile()) {
      for (Field<?> f : column.getJooqFileFields()) {
        jooq.alterTable(SqlTableMetadataExecutor.getJooqTable(column.getTable()))
            .dropColumnIfExists(f)
            .execute();
      }
    } else if (column.isReference()) {
      for (Reference ref : column.getReferences()) {
        // check if reference name already exists, composite ref may reuse columns
        // either other column, or a part of a reference
        if (!ref.isOverlapping()) {
          jooq.alterTable(column.getJooqTable()).dropColumn(ref.getJooqField()).execute();
        }
      }
    } else {
      jooq.alterTable(SqlTableMetadataExecutor.getJooqTable(column.getTable()))
          .dropColumnIfExists(field(name(column.getName())))
          .execute();
    }
    MetadataUtils.deleteColumn(jooq, column);
  }

  static void executeRemoveRefConstraints(DSLContext jooq, Column column) {
    if (column.isRef()) {
      SqlColumnRefExecutor.removeRefConstraints(jooq, column);
    } else if (column.isRefArray()) {
      removeRefArrayConstraints(jooq, column);
    } else if (column.isRefback()) {
      removeRefBackConstraints(jooq, column);
    }
  }

  public static void executeSetDefaultValue(DSLContext jooq, Column newColumn) {
    if (newColumn.getDefaultValue() != null) {
      jooq.alterTable(newColumn.getJooqTable())
          .alterColumn(newColumn.getJooqField())
          .defaultValue(newColumn.getDefaultValue())
          .execute();
    } else {
      // remove default
      jooq.alterTable(newColumn.getJooqTable())
          .alterColumn(newColumn.getJooqField())
          .dropDefault()
          .execute();
    }
  }
}
