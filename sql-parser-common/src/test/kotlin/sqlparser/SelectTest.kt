package sqlparser

import kotlin.test.Test
import kotlin.test.assertEquals
import sqlparser.Ast.*
import sqlparser.Ast.Statement.*
import sqlparser.Ast.Expression.*
import sqlparser.Ast.Expression.Literal.*
import sqlparser.Ast.SelectOrUnion.*

class SelectTest {

    @Test
    fun testSelectOnly1() {
        val statement = "Select 1 as foo, bar, 1 * 3;"
        val expected = SelectStmt(
                SelectClause(listOf(
                        NamedExpression("foo", IntLiteral(1)),
                        NamedExpression(null, Reference(Identifier(listOf(), "bar"))),
                        NamedExpression(null, FunctionCall("*", listOf(
                                IntLiteral(1),
                                IntLiteral(3)
                        ), infix = true))
                ))
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testSelectOnly1ToString() {
        val statement = "Select 1 foo, bar, 1 * 3;"
        val expected = """SELECT
            |  1 AS foo,
            |  bar,
            |  1 * 3;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }

    @Test
    fun testSelectOnly2() {
        val statement = "Select *, foo.*;"
        val expected = SelectStmt(
                SelectClause(listOf(
                        NamedExpression(null, Reference(Identifier(listOf(), "*"))),
                        NamedExpression(null, Reference(Identifier(listOf("foo"), "*")))
                ))
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testSelectOnly2ToString() {
        val statement = "Select *, foo.*;"
        val expected = """SELECT
            |  *,
            |  foo.*;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }

    @Test
    fun testSelectOnly3() {
        val statement = "Select distinct foobar, baz;"
        val expected = SelectStmt(
                SelectClause(listOf(
                        NamedExpression(null, Reference(Identifier(listOf(), "foobar"))),
                        NamedExpression(null, Reference(Identifier(listOf(), "baz")))
                ), distinct = true)
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testSelectOnly3ToString() {
        val statement = "Select distinct foobar, baz;"
        val expected = """SELECT DISTINCT
            |  foobar,
            |  baz;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }

    @Test
    fun testSelectFrom1() {
        val statement = "Select a,b from mydb.my_table;"
        val expected = SelectStmt(
                SelectClause(
                        listOf(
                            NamedExpression(null, Reference(Identifier(listOf(), "a"))),
                            NamedExpression(null, Reference(Identifier(listOf(), "b")))
                        ),
                        fromClause = FromClause(
                                DataSource.Table(Identifier(listOf("mydb"), "my_table"), null)
                        )
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testSelectFrom1ToString() {
        val statement = "Select a,b from mydb.my_table;"
        val expected = """SELECT
            |  a,
            |  b
            |FROM
            |  mydb.my_table;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }


    @Test
    fun testSelectFrom2() {
        val statement = "Select a,b from mydb.my_table as t;"
        val expected = SelectStmt(
                SelectClause(
                        listOf(
                                NamedExpression(null, Reference(Identifier(listOf(), "a"))),
                                NamedExpression(null, Reference(Identifier(listOf(), "b")))
                        ),
                        fromClause = FromClause(
                                DataSource.Table(Identifier(listOf("mydb"), "my_table"), Identifier(listOf(), "t"))
                        )
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testSelectFrom2ToString() {
        val statement = "Select a,b from mydb.my_table as t;"
        val expected = """SELECT
            |  a,
            |  b
            |FROM
            |  mydb.my_table t;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }

    @Test
    fun testSelectFrom3() {
        val statement = "Select a,b from (select * from foobar) as t;"
        val expected = SelectStmt(
                SelectClause(
                        listOf(
                                NamedExpression(null, Reference(Identifier(listOf(), "a"))),
                                NamedExpression(null, Reference(Identifier(listOf(), "b")))
                        ),
                        fromClause = FromClause(
                                DataSource.SubQuery(
                                        SelectClause(
                                                listOf(
                                                        NamedExpression(null, Reference(Identifier(listOf(), "*")))
                                                ),
                                                fromClause = FromClause(DataSource.Table(Identifier(listOf(), "foobar"),null))
                                        ),
                                        Identifier(listOf(), "t")
                                )
                        )
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testSelectFrom3ToString() {
        val statement = "Select a,b from (select * from foobar) as t;"
        val expected = """SELECT
            |  a,
            |  b
            |FROM
            |  (
            |    SELECT
            |      *
            |    FROM
            |      foobar
            |  ) t;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }


    @Test
    fun testSelectFromUnion1() {
        val statement = "Select a from table1 union all Select a from table2;"
        val expected = SelectStmt(
                Union(
                    SelectClause(
                            listOf(
                                    NamedExpression(null, Reference(Identifier(listOf(), "a")))
                            ),
                            fromClause = FromClause(
                                    DataSource.Table(Identifier(listOf(), "table1"), null)
                            )
                    ),
                    SelectClause(
                            listOf(
                                    NamedExpression(null, Reference(Identifier(listOf(), "a")))
                            ),
                            fromClause = FromClause(
                                    DataSource.Table(Identifier(listOf(), "table2"), null)
                            )
                    ),
                    true
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testSelectFromUnion1ToString() {
        val statement = "Select a from table1 union all Select a from table2;"
        val expected = """SELECT
            |  a
            |FROM
            |  table1
            |UNION ALL
            |SELECT
            |  a
            |FROM
            |  table2;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }

    @Test
    fun testSelectWithCte() {
        val statement = "with foo as (select 1 as a) Select a from foo;"

        val cte = DataSource.SubQuery(SelectClause(
                listOf(
                        NamedExpression("a", IntLiteral(1))
                )
        ), Identifier(listOf(), "foo"))

        val expected = SelectStmt(
                SelectClause(
                        listOf(
                                NamedExpression(null, Reference(Identifier(listOf(), "a")))
                        ),
                        fromClause = FromClause(
                                DataSource.Table(Identifier(listOf(), "foo"), null)
                        ),
                        ctes = listOf(cte)
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testSelectWithCteToString() {
        val statement = "with foo as (select 1 as a) Select a from foo;"
        val expected = """WITH foo AS (
            |  SELECT
            |    1 AS a
            |)
            |
            |SELECT
            |  a
            |FROM
            |  foo;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }

    @Test
    fun testSelectWith2CteToString() {
        val statement = "with foo as (select 1 as a), bar as (select a+ 1 as b) Select b from bar;"
        val expected = """WITH foo AS (
            |  SELECT
            |    1 AS a
            |),
            |bar AS (
            |  SELECT
            |    a + 1 AS b
            |)
            |
            |SELECT
            |  b
            |FROM
            |  bar;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }

    @Test
    fun testSelectWithTableGeneratingFunction() {
        val statement = "Select a from table_func(1) foo (a);"


        val expected = SelectStmt(
                SelectClause(
                        listOf(
                                NamedExpression(null, Reference(Identifier(listOf(), "a")))
                        ),
                        fromClause = FromClause(
                                DataSource.TableFunction(
                                        FunctionCall("table_func", listOf(IntLiteral(1))),
                                        Identifier(listOf(), "foo"),
                                        listOf(Identifier(listOf(), "a"))
                                )
                        )
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testSelectWithTableGeneratingFunctionToString() {
        val statement = "Select a from table_func(1) foo (a);"
        val expected = """SELECT
            |  a
            |FROM
            |  table_func(1) AS foo (a);""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }

}