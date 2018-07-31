package sqlparser

import sqlparser.Ast.Expression.*
import sqlparser.Ast.Expression.Literal.IntLiteral

import kotlin.test.Test
import kotlin.test.assertEquals


class ExpressionTest {

    @Test
    fun testBasicMaths() {
        val expression = "1 * 2 + 3 / 4"
        val expected = FunctionCall("+", listOf(
                FunctionCall("*", listOf(IntLiteral(1), IntLiteral(2)), true),
                FunctionCall("/", listOf(IntLiteral(3), IntLiteral(4)), true)
        ), true)

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testBasicMathsToString() {
        val expression = "1 * 2 + 3 / 4"
        val expected = "(1 * 2) + (3 / 4)"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testBrackets() {
        val expression = "((1 + 2) * ((3 - 4)))"
        val expected = FunctionCall("*", listOf(
                FunctionCall("+", listOf(IntLiteral(1), IntLiteral(2)), true),
                FunctionCall("-", listOf(IntLiteral(3), IntLiteral(4)), true)
        ), true)

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testBracketsToString() {
        val expression = "((1 + 2) * ((3 - 4)))"
        val expected = "(1 + 2) * (3 - 4)"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testBasicFunction() {
        val expression = "fooBAR(1+2, abs(-5))"
        val expected = FunctionCall("foobar", listOf(
                FunctionCall("+", listOf(IntLiteral(1), IntLiteral(2)), true),
                FunctionCall("abs", listOf(
                        FunctionCall("-", listOf(IntLiteral(5)), true)
                ))
        ))

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testBasicFunctionToString() {
        val expression = "fooBAR(1+2, abs(-5))"
        val expected = "foobar(1 + 2, abs(- 5))"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testReference() {
        val expression = "sum(monEY)"
        val expected = FunctionCall("sum", listOf(
                Reference(Ast.Identifier(null, "money"))
        ))

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testReferenceToString() {
        val expression = "sum(monEY)"
        val expected = "sum(money)"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testQualifiedReference() {
        val expression = "sum(foobar.monEY)"
        val expected = FunctionCall("sum", listOf(
                Reference(Ast.Identifier("foobar", "money"))
        ))

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testQualifiedReferenceToString() {
        val expression = "sum(foobar.monEY)"
        val expected = "sum(foobar.money)"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }
}