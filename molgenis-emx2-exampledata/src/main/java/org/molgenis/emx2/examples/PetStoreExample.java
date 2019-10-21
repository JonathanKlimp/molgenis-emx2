package org.molgenis.emx2.examples;

import org.molgenis.emx2.*;

import static org.molgenis.emx2.ColumnType.BOOL;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.STRING_ARRAY;

public class PetStoreExample {

  public static final String CATEGORY = "Category";
  public static final String TAG = "Tag";
  public static final String NAME = "name";

  private PetStoreExample() {
    // hide public constructor
  }

  public static void create(SchemaMetadata schema) {

    TableMetadata categoryTable = schema.createTableIfNotExists(CATEGORY);
    categoryTable.addColumn(NAME).setPrimaryKey(true);

    TableMetadata tagTable = schema.createTableIfNotExists(TAG);
    tagTable.addColumn(NAME).setPrimaryKey(true);

    TableMetadata petTable = schema.createTableIfNotExists("Pet");
    petTable.addColumn(NAME).setPrimaryKey(true);
    petTable.addRef("category", CATEGORY).setNullable(true);
    petTable.addColumn("photoUrls", STRING_ARRAY);
    petTable.addColumn("status"); // todo enum: available, pending, sold
    petTable.addRefArray("tags", TAG);

    TableMetadata userTable = schema.createTableIfNotExists("User");
    userTable.addColumn("username").setPrimaryKey(true);
    userTable.addColumn("firstName");
    userTable.addColumn("lastName");
    userTable.addColumn("email"); // todo: validation email
    userTable.addColumn("password"); // todo: password type
    userTable.addColumn("phone"); // todo: validation phone
    userTable.addColumn("userStatus", INT);

    TableMetadata orderTable = schema.createTableIfNotExists("Order");
    orderTable.addColumn("orderId").primaryKey();
    orderTable.addRef("petId", "Pet", NAME);
    orderTable.addColumn("quantity", INT); // todo: validation >=1
    orderTable.addColumn("complete", BOOL); // todo: default false
    orderTable.addColumn("status"); // todo enum: placed, approved, delivered
  }

  public static void populate(Schema schema) {

    schema
        .getTable(CATEGORY)
        .insert(new Row().set(NAME, "aCategory"), new Row().set(NAME, "bCategory"));

    schema.getTable(TAG).insert(new Row().set(NAME, "aTag"), new Row().set(NAME, "bTag"));
  }
}
