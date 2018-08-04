package sqlparser

import kotlin.test.Test
import kotlin.test.assertEquals
import sqlparser.Ast.*
import sqlparser.Ast.Statement.*
import sqlparser.Ast.Expression.*
import sqlparser.Ast.Expression.Literal.*
import sqlparser.Ast.SelectOrUnion.*

class JoinTest {

    @Test
    fun testInferredJoin1() {
        val statement = "Select 1 From foo, bar;"
        val expected = SelectStmt(
                SelectClause(
                        listOf(
                            NamedExpression(null, IntLiteral(1))
                        ),
                        fromClause = FromClause(
                                DataSource.Join(
                                        DataSource.Table(Identifier(null, "foo"), null),
                                        DataSource.Table(Identifier(null, "bar"), null),
                                        joinType = JoinType.CROSS
                                )
                        )
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testInferredJoin1ToString() {
        val statement = "Select 1 From foo, bar;"
        val expected = """SELECT
            |  1
            |FROM
            |  foo
            |CROSS JOIN
            |  bar;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }

    @Test
    fun testInnerJoin1() {
        val statement = "Select 1 From foo join bar on foo.id = bar.id;"
        val expected = SelectStmt(
                SelectClause(
                        listOf(
                                NamedExpression(null, IntLiteral(1))
                        ),
                        fromClause = FromClause(
                                DataSource.Join(
                                        DataSource.Table(Identifier(null, "foo"), null),
                                        DataSource.Table(Identifier(null, "bar"), null),
                                        joinType = JoinType.INNER,
                                        onExpression = FunctionCall("=", listOf(
                                                Reference(Identifier("foo", "id")),
                                                Reference(Identifier("bar", "id"))
                                        ), infix = true)
                                )
                        )
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testInnerJoin1ToString() {
        val statement = "Select 1 From foo join bar on foo.id = bar.id;"
        val expected = """SELECT
            |  1
            |FROM
            |  foo
            |INNER JOIN
            |  bar
            |ON foo.id = bar.id;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }

    @Test
    fun testChainedJoin1() {
        val statement = "Select 1 From foo join bar on foo.id = bar.id join baz on bar.id = baz.id;"
        val expected = SelectStmt(
                SelectClause(
                        listOf(
                                NamedExpression(null, IntLiteral(1))
                        ),
                        fromClause = FromClause(
                                DataSource.Join(
                                        DataSource.Join(
                                                DataSource.Table(Identifier(null, "foo"), null),
                                                DataSource.Table(Identifier(null, "bar"), null),
                                                joinType = JoinType.INNER,
                                                onExpression = FunctionCall("=", listOf(
                                                        Reference(Identifier("foo", "id")),
                                                        Reference(Identifier("bar", "id"))
                                                ), infix = true)
                                        ),
                                        DataSource.Table(Identifier(null, "baz"), null),
                                        joinType = JoinType.INNER,
                                        onExpression = FunctionCall("=", listOf(
                                                Reference(Identifier("bar", "id")),
                                                Reference(Identifier("baz", "id"))
                                        ), infix = true)
                                )
                        )
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testChainedJoin1ToString() {
        val statement = "Select 1 From foo join bar on foo.id = bar.id join baz on bar.id = baz.id;"
        val expected = """SELECT
            |  1
            |FROM
            |  foo
            |INNER JOIN
            |  bar
            |ON foo.id = bar.id
            |INNER JOIN
            |  baz
            |ON bar.id = baz.id;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }
}