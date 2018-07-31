package sqlparser

import kotlin.test.Test
import kotlin.test.assertEquals
import sqlparser.Ast.*
import sqlparser.Ast.Statement.*

class CreateSchemaTest {

    @Test
    fun testCreateDatabase1() {
        val statement = "CREATE SCHEMA myschema;"
        val expected = CreateSchema(
                Identifier(null, "myschema")
        )

        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testCreateDatabase1ToString() {
        val statement = "CREATE SCHEMA myschema;"

        assertEquals(statement, SqlPrinter.from(parseStatement(statement, true)))
    }

    @Test
    fun testCreateDatabase2() {
        val statement = "CREATE SCHEMA AUTHORIZATION joe;"
        val expected = CreateSchema(
                Identifier(null, "joe")
        )

        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testCreateDatabase3() {
        val statement = "CREATE SCHEMA IF NOT EXISTS test AUTHORIZATION joe;"
        val expected = CreateSchema(
                Identifier(null, "test"),
                ifNotExists = true
        )

        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testCreateDatabase4() {
        val statement = "CREATE DATAbase myschema;"
        val expected = CreateSchema(
                Identifier(null,"myschema")
        )

        assertEquals(expected, parseStatement(statement, true))
    }
}