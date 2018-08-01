package sqlparser

import kotlin.test.Test
import kotlin.test.assertEquals
import sqlparser.Ast.*
import sqlparser.Ast.Statement.*
import sqlparser.Ast.Expression.*
import sqlparser.Ast.Expression.Literal.*

class SelectTest {

    @Test
    fun testCreateTable1() {
        val statement = "Select 1 as foo, bar, 1 * 3;"
        val expected = SelectStmt(
                SelectClause(listOf(
                        NamedExpression("foo", IntLiteral(1)),
                        NamedExpression(null, Reference(Identifier(null, "bar"))),
                        NamedExpression(null, FunctionCall("*", listOf(
                                IntLiteral(1),
                                IntLiteral(3)
                        ), infix = true))
                ))
        )
        assertEquals(expected, parseStatement(statement, true))
    }

    @Test
    fun testCreateTable1ToString() {
        val statement = "Select 1 foo, bar, 1 * 3;"
        val expected = "SELECT 1 AS foo, bar, 1 * 3;"

        assertEquals(expected, SqlPrinter.from(parseStatement(statement, true)))
    }


}