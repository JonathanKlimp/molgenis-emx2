package org.molgenis.emx2.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFile;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestColumnTypeIsFile {

  private static final String SCHEMA_NAME = "TestColumnTypeIsFile";
  static Database database;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(SCHEMA_NAME);
    new PetStoreLoader().load(schema, false);

    schema
        .getTable("User")
        .insert(
            row(
                "username",
                "bob",
                "picture",
                new BinaryFileWrapper(
                    "text/plain", "test.txt", "test".getBytes(Charset.defaultCharset()))));
  }

  @Test
  public void testExcelShouldNotIncludeFileColumns() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    Path excelFile = tmp.resolve("test.xlsx");
    MolgenisIO.toExcelFile(excelFile, schema);
    TableStore store = new TableStoreForXlsxFile(excelFile);
    assertFalse(store.readTable("User").iterator().next().getColumnNames().contains("picture"));
  }

  @Test
  public void testCsvShouldNotIncludeFileColumns() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    Path csvFile = tmp.resolve("User.csv");
    MolgenisIO.toExcelFile(csvFile, schema.getTable("User"));
    TableStore store = new TableStoreForCsvFile(csvFile);
    assertFalse(store.readTable("User").iterator().next().getColumnNames().contains("picture"));
  }

  @Test
  public void testCanExportImportFilesInZip() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    Path zipFile = tmp.resolve("test.zip");
    MolgenisIO.toZipFile(zipFile, schema);

    schema = database.dropCreateSchema(SCHEMA_NAME);
    MolgenisIO.fromZipFile(zipFile, schema, true);

    Table userTable = schema.getTable("User");
    assertEquals(
        "test",
        new String(
            userTable
                .query()
                .select(s("picture", s("contents"), s("mimetype"), s("extension")))
                .retrieveRows()
                .get(0)
                .getBinary("picture_contents")));
  }

  @Test
  public void testCanExportImportFilesInDirectory() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    Path directory = tmp.resolve("test");
    Files.createDirectories(directory);

    MolgenisIO.toDirectory(directory, schema);

    schema = database.dropCreateSchema(SCHEMA_NAME);
    MolgenisIO.fromDirectory(directory, schema, true);

    Table userTable = schema.getTable("User");
    Row result =
        userTable
            .query()
            .select(s("picture", s("contents"), s("mimetype"), s("extension")))
            .retrieveRows()
            .get(0);
    assertEquals("test", new String(result.getBinary("picture_contents")));
    assertEquals("txt", result.getString("picture_extension"));
    assertEquals("text/plain", result.getString("picture_mimetype"));
  }
}
