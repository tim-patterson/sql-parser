package sqlparser

import kotlin.test.Test
import kotlin.test.assertEquals


class LiteralTest {

    @Test
    fun testStringLiteral() {
        val expression = "\"Hello world\""
        val expected = Ast.Expression.Literal.StringLiteral("Hello world")

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testStringLiteralToString() {
        val expression = "'Hello world'"

        assertEquals(expression, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testTrueLiteral() {
        val expression = "TrUe"
        val expected = Ast.Expression.Literal.BooleanLiteral(true)

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testTrueLiteralToString() {
        val expression = "TrUe"
        val expected = "TRUE"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testFalseLiteral() {
        val expression = "falsE"
        val expected = Ast.Expression.Literal.BooleanLiteral(false)

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testFalseLiteralToString() {
        val expression = "falsE"
        val expected = "FALSE"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testNullLiteral() {
        val expression = "nuLL"
        val expected = Ast.Expression.Literal.NullLiteral

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testNullLiteralToString() {
        val expression = "nuLL"
        val expected = "NULL"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testIntLiteral() {
        val expression = "1234"
        val expected = Ast.Expression.Literal.IntLiteral(1234)

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testIntLiteralToString() {
        val expression = "1234"

        assertEquals(expression, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testFloatLiteral() {
        val expression = "1234.5678"
        val expected = Ast.Expression.Literal.FloatLiteral(1234.5678)

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testFloatLiteralToString() {
        val expression = "1234.5678"

        assertEquals(expression, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testDateLiteral() {
        val expression = "date \"2018-01-01\""
        val expected = Ast.Expression.Literal.DateLiteral("2018-01-01")

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testDateLiteralToString() {
        val expression = "date \"2018-01-01\""
        val expected = "DATE '2018-01-01'"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }
}