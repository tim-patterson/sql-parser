package sqlparser

import kotlin.test.Test
import kotlin.test.assertEquals
import sqlparser.Ast.*
import sqlparser.Ast.Statement.*
import sqlparser.Ast.Expression.*
import sqlparser.Ast.Expression.Literal.*
import sqlparser.Ast.SelectOrUnion.*

class GroupByTest {

    @Test
    fun testGroupBy() {
        val statement = "Select a, count(*) From foo group by a;"
        val expected = SelectStmt(
                SelectClause(
                        listOf(
                                NamedExpression(null, Reference(Identifier(listOf(), "a"))),
                                NamedExpression(null, FunctionCall("count", listOf(Reference(Identifier(listOf(), "*")))))
                        ),
                        fromClause = FromClause(DataSource.Table(Identifier(listOf(), "foo"), null)),
                        groupByExpressions = listOf(
                                Reference(Identifier(listOf(), "a"))
                        )
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testGroupByToString() {
        val statement = "Select a, count(*) From foo group by a;"
        val expected = """SELECT
            |  a,
            |  count(*)
            |FROM
            |  foo
            |GROUP BY
            |  a;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }

    @Test
    fun testGroupByHaving() {
        val statement = "Select a, count(*) From foo group by a having count(*) > 1;"
        val expected = SelectStmt(
                SelectClause(
                        listOf(
                                NamedExpression(null, Reference(Identifier(listOf(), "a"))),
                                NamedExpression(null, FunctionCall("count", listOf(Reference(Identifier(listOf(), "*")))))
                        ),
                        fromClause = FromClause(DataSource.Table(Identifier(listOf(), "foo"), null)),
                        groupByExpressions = listOf(
                                Reference(Identifier(listOf(), "a"))
                        ),
                        havingPredicate = FunctionCall(">", listOf(
                                FunctionCall("count", listOf(Reference(Identifier(listOf(), "*")))),
                                IntLiteral(1)
                        ), infix = true)
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testGroupByHavingToString() {
        val statement = "Select a, count(*) From foo group by a having count(*) > 1;"
        val expected = """SELECT
            |  a,
            |  count(*)
            |FROM
            |  foo
            |GROUP BY
            |  a
            |HAVING
            |  count(*) > 1;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }
}