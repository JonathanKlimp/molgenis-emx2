package org.molgenis.emx2.examples;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;

public class PetStoreExample {

  public static final String CATEGORY = "Category";
  public static final String TAG = "Tag";
  public static final String NAME = "name";
  public static final String PET = "Pet";
  public static final String ORDER = "Order";
  public static final String USER = "User";
  public static final String CATEGORY_COLUMN = "category";
  public static final String STATUS = "status";
  public static final String WEIGHT = "weight";
  public static final String ORDER_ID = "orderId";
  public static final String QUANTITY = "quantity";
  public static final String PRICE = "price";
  public static final String COMPLETE = "complete";
  public static final String EMAIL = "email";

  private PetStoreExample() {
    // hide public constructor
  }

  public static void create(SchemaMetadata schema) {

    schema.create(table(CATEGORY).add(column(NAME).setPkey()));

    schema.create(table(TAG).add(column(NAME).setPkey()));

    schema.create(
        table(PET)
            .add(column(NAME).setDescription("the name").setPkey())
            .add(column(CATEGORY_COLUMN).setType(REF).setRefTable(CATEGORY).setRequired(true))
            .add(column("photoUrls").setType(STRING_ARRAY))
            .add(
                column("details")
                    .setType(CONSTANT)
                    .setDescription("<h1>Details:</h1>")) // add a layout element
            .add(column(STATUS)) // todo enum: available, pending, sold
            .add(column("tags").setType(REF_ARRAY).setRefTable(TAG))
            .add(column(WEIGHT).setType(DECIMAL).setRequired(true))
            .setDescription("My pet store example table"));

    schema.create(
        table(ORDER)
            .add(column(ORDER_ID).setPkey())
            .add(column("pet").setType(REF).setRefTable(PET))
            .add(
                column(QUANTITY)
                    .setType(INT)
                    .setValidIf("if(value<1)'Must be larger than 1'")) // todo: validation >=1
            .add(
                column(PRICE)
                    .setType(DECIMAL)
                    .setValidIf("if(value<1.0)'Must be larger than 1.0'")) // todo: validation >=1
            .add(column(COMPLETE).setType(BOOL)) // todo: default false
            .add(column(STATUS))); // todo enum: placed, approved, delivered

    // refBack
    schema
        .getTableMetadata(PET)
        .add(column("orders").setType(REFBACK).setRefTable(ORDER).setRefBack("pet"));

    schema.create(
        table(USER)
            .add(column("username").setPkey())
            .add(column("firstName"))
            .add(column("lastName"))
            .add(
                column(EMAIL)
                    .setValidIf(
                        "if(!/^\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*(\\.\\w{2,3})+$/.test(value)) 'Should be valid email address'")) // todo: validation email
            .add(column("password")) // todo: password type
            .add(column("phone")) // todo: validation phone
            .add(column("userStatus").setType(INT))
            .add(column("pets").setType(REF_ARRAY).setRefTable(PET)));
  }

  public static void populate(Schema schema) {
    final String shopviewer = "shopviewer";
    final String shopmanager = "shopmanager";

    // initual user
    schema.addMember(shopmanager, "Manager");
    schema.getDatabase().setUserPassword(shopmanager, shopmanager);

    schema.addMember(shopviewer, "Viewer");
    schema.getDatabase().setUserPassword(shopviewer, shopviewer);

    schema.getTable(CATEGORY).insert(new Row().set(NAME, "cat"), new Row().set(NAME, "dog"));
    schema.getTable(TAG).insert(new Row().set(NAME, "red"), new Row().set(NAME, "green"));

    schema
        .getTable(PET)
        .insert(
            new Row()
                .set(CATEGORY_COLUMN, "cat")
                .set("name", "pooky")
                .set(STATUS, "available")
                .set(WEIGHT, 9.4),
            new Row()
                .set(CATEGORY_COLUMN, "dog")
                .set("name", "spike")
                .set(STATUS, "sold")
                .set("tags", "red,green")
                .set(WEIGHT, 15.7));

    schema
        .getTable(ORDER)
        .insert(
            new Row()
                .set(ORDER_ID, "1")
                .set("pet", "pooky")
                .set(QUANTITY, 1)
                .set(PRICE, 9.99)
                .set(COMPLETE, true)
                .set(STATUS, "delivered"),
            new Row()
                .set(ORDER_ID, "2")
                .set("pet", "spike")
                .set(PRICE, 14.99)
                .set(QUANTITY, 7)
                .set(COMPLETE, false)
                .set(STATUS, "approved"));

    schema.getTable(USER).insert(new Row().set("username", "bofke").set("pets", "spike,pooky"));
  }
}
