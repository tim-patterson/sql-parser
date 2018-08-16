package sqlparser

import kotlin.test.Test
import kotlin.test.assertEquals
import sqlparser.Ast.*
import sqlparser.Ast.Statement.*
import sqlparser.Ast.Expression.*
import sqlparser.Ast.Expression.Literal.*
import sqlparser.Ast.SelectOrUnion.*

class WhereTest {

    @Test
    fun testWhere() {
        val statement = "Select 1 From foo where a + b = 2;"
        val expected = SelectStmt(
                SelectClause(
                        listOf(
                            NamedExpression(null, IntLiteral(1))
                        ),
                        fromClause = FromClause(DataSource.Table(Identifier(listOf(), "foo"), null)),
                        predicate = FunctionCall("=", listOf(
                                FunctionCall("+", listOf(
                                        Reference(Identifier(listOf(), "a")),
                                        Reference(Identifier(listOf(), "b"))
                                ), infix = true),
                                IntLiteral(2)
                        ), infix = true)
                )
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testWhereToString() {
        val statement = "Select 1 From foo where a + b = 2;"
        val expected = """SELECT
            |  1
            |FROM
            |  foo
            |WHERE
            |  a + b = 2;""".trimMargin()

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }
}