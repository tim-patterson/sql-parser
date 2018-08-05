package sqlparser

import kotlin.test.Test
import kotlin.test.assertEquals
import sqlparser.Ast.*
import sqlparser.Ast.Statement.*
import sqlparser.Ast.Expression.*
import sqlparser.Ast.Expression.Literal.*
import sqlparser.Ast.SelectOrUnion.*

class OrderByLimitTest {

    @Test
    fun testOrderBy() {
        val statement = "Select * From foo order by a desc, b;"
        val expected = SelectStmt(
                SelectClause(
                        listOf(
                                NamedExpression(null, Reference(Identifier(null, "*")))
                        ),
                        fromClause = FromClause(DataSource.Table(Identifier(null, "foo"), null)),
                        orderByExpressions = listOf(
                                OrderExpression(Reference(Identifier(null, "a")), asc = false),
                                OrderExpression(Reference(Identifier(null, "b")), asc = true)
                        )
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testOrderByToString() {
        val statement = "Select * From foo order by a desc, b;"
        val expected = """SELECT
            |  *
            |FROM
            |  foo
            |ORDER BY
            |  a DESC,
            |  b ASC;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }

    @Test
    fun testLimit() {
        val statement = "Select * From foo limit 100;"
        val expected = SelectStmt(
                SelectClause(
                        listOf(
                                NamedExpression(null, Reference(Identifier(null, "*")))
                        ),
                        fromClause = FromClause(DataSource.Table(Identifier(null, "foo"), null)),
                        limit = 100
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testLimitToString() {
        val statement = "Select * From foo limit 100;"
        val expected = """SELECT
            |  *
            |FROM
            |  foo
            |LIMIT 100;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }
}