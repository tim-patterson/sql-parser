package sqlparser

import kotlin.test.Test
import kotlin.test.assertEquals
import sqlparser.Ast.*
import sqlparser.Ast.Statement.*

class CreateTableTest {

    @Test
    fun testCreateTable1() {
        val statement = """
            CREATE TABLE films (
                code        char(5) CONSTRAINT firstkey PRIMARY KEY,
                title       varchar(40) NOT NULL,
                did         integer NOT NULL,
                date_prod   date,
                kind        varchar(10),
                len         interval hour to minute
            );
        """.trimIndent()
        val expected = CreateTable(
                Identifier(null, "films"),
                listOf(
                        ColumnDefinition(Identifier(null, "code"), "CHAR(5)"),
                        ColumnDefinition(Identifier(null, "title"), "VARCHAR(40)"),
                        ColumnDefinition(Identifier(null, "did"), "INTEGER"),
                        ColumnDefinition(Identifier(null, "date_prod"), "DATE"),
                        ColumnDefinition(Identifier(null, "kind"), "VARCHAR(10)"),
                        ColumnDefinition(Identifier(null, "len"), "INTERVAL HOUR TO MINUTE")
                )
        )

        assertEquals(expected, parseStatement(statement, true))
    }


    @Test
    fun testCreateTable1ToString() {
        val statement = """
            CREATE TABLE films (
                code        char(5) CONSTRAINT firstkey PRIMARY KEY,
                title       varchar(40) NOT NULL,
                did         integer NOT NULL,
                date_prod   date,
                kind        varchar(10),
                len         interval hour to minute
            );
        """
        val expected = """
            CREATE TABLE films (
              code CHAR(5),
              title VARCHAR(40),
              did INTEGER,
              date_prod DATE,
              kind VARCHAR(10),
              len INTERVAL HOUR TO MINUTE
            );
        """.trimIndent()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }
}