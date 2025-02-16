package org.molgenis.emx2.sql;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Operator.LIKE;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

public class TestInherits {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testExtends() {

    Schema s = db.dropCreateSchema(TestInherits.class.getSimpleName());

    Table person = s.create(table("Person"));

    // test if fails if no primary key
    try {
      s.create(table("Employee").setInherit(person.getName()));
      fail("Should fail because does not have pkey");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    try {
      s.create(table("Employee").setInherit("fake_table"));
      fail("Should fail");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // set pkey and a property
    person.getMetadata().add(column("fullName").setPkey());
    person.getMetadata().add(column("birthDate").setType(DATE));

    // create first extended table
    Table employee =
        s.create(table("Employee").setInherit(person.getName()).add(column("salary").setType(INT)));

    Table manager =
        s.create(
            table("Manager")
                .setInherit("Employee")
                .add(column("directs").setType(REF_ARRAY).setRefTable("Employee")));

    Table ceo = s.create(table("CEO").setInherit("Manager"));

    // try to add column that already exists in parent
    try {
      employee.getMetadata().add(column("birthDate").setType(DATE));
      fail("should fail: cannot add column to subclass that already exists in superclass");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // try to extend twice
    try {
      manager.getMetadata().setInherit("Student");
      fail("should fail: cannot extend another table");
    } catch (MolgenisException e) {
      System.out.println("Errored correctly:\n" + e);
    }

    // create another extended table
    s.create(
        table("Student").setInherit(person.getName()).add(column("averageGrade").setType(INT)));

    // test insert, retrieve
    Table studentTable = s.getTable("Student");
    studentTable.insert(new Row().setString("fullName", "Donald Duck").setInt("averageGrade", 10));

    Table employeeTable = s.getTable("Employee");
    employeeTable.insert(
        new Row()
            .setString("fullName", "Katrien Duck")
            .setInt("salary", 100)
            .setDate("birthDate", LocalDate.of(2000, 12, 01)));

    Table ceoTable = s.getTable("CEO"); // we use CEO to make it more difficult
    Row managerRow =
        new Row()
            .setString("fullName", "Dagobert Duck")
            .setInt("salary", 1000000)
            .setDate("birthDate", LocalDate.of(2000, 12, 01))
            .setStringArray("directs", "Katrien Duck");
    ceoTable.insert(managerRow);

    Table personTable = s.getTable("Person");
    assertEquals(3, personTable.retrieveRows().size());
    assertEquals(1, studentTable.retrieveRows().size());
    assertEquals(2, employeeTable.retrieveRows().size());
    assertEquals(1, ceoTable.retrieveRows().size());

    // retrieve
    assertEquals(
        (Integer) 1000000,
        employeeTable
            .query()
            .select(s("salary"))
            .where(f("fullName", EQUALS, "Dagobert Duck"))
            .retrieveRows()
            .get(0)
            .getInteger("salary"));

    // TODO test RLS

    // test search
    assertEquals(1, personTable.search("Dagobert").retrieveRows().size());
    assertEquals(1, employeeTable.search("Dagobert").retrieveRows().size());

    // update
    managerRow.setDate("birthDate", LocalDate.of(1900, 12, 01));
    ceoTable.update(managerRow);
    assertEquals(LocalDate.of(1900, 12, 01), ceoTable.retrieveRows().get(0).getDate("birthDate"));

    // test graph query
    // simple
    String result = ceoTable.select(s("fullName"), s("salary")).retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Dagobert"));
    // nested relation
    result =
        ceoTable.select(s("fullName"), s("salary"), s("directs", s("fullName"))).retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Katrien"));
    // filtering (erroroneous)
    result =
        ceoTable
            .select(s("fullName"), s("salary"), s("directs", s("fullName")))
            .where(f("directs", f("fullName", LIKE, "Pietje")))
            .retrieveJSON();
    System.out.println(result);
    assertFalse(result.contains("Katrien"));
    // filtering (correct)
    result =
        ceoTable
            .select(s("fullName"), s("salary"), s("directs", s("fullName")))
            .where(f("directs", f("fullName", LIKE, "Katrien")))
            .retrieveJSON();
    System.out.println(result);
    assertTrue(result.contains("Katrien"));

    // filtering on mg_tableclass
    assertEquals(
        1,
        personTable
            .query()
            .where(f(MG_TABLECLASS, EQUALS, s.getName() + ".Employee"))
            .retrieveRows()
            .size());

    // delete
    ceoTable.delete(managerRow);
    assertEquals(2, personTable.retrieveRows().size());
    assertEquals(1, studentTable.retrieveRows().size());
    assertEquals(1, employeeTable.retrieveRows().size());
    assertEquals(0, ceoTable.retrieveRows().size());

    // test multi add
    personTable.insert(
        row(MG_TABLECLASS, "Manager", "fullName", "popeye"),
        row(MG_TABLECLASS, "Employee", "fullName", "goofy"));
    assertEquals(4, personTable.retrieveRows().size());
    assertEquals(1, studentTable.retrieveRows().size());
    assertEquals(3, employeeTable.retrieveRows().size());
    assertEquals(1, manager.retrieveRows().size());
    assertEquals(0, ceoTable.retrieveRows().size());

    try {
      personTable.insert(row(MG_TABLECLASS, "Wrong", "fullName", "popeye"));
      fail("should error");
    } catch (Exception e) {
      System.out.println("Errored correctly: " + e.getMessage());
    }

    try {
      personTable.insert(row(MG_TABLECLASS, "Blaat.Wrong", "fullName", "popeye"));
      fail("should error");
    } catch (Exception e) {
      System.out.println("Errored correctly: " + e.getMessage());
    }
  }
}
