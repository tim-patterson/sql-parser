package sqlparser

import kotlin.test.Test
import kotlin.test.assertEquals


class LiteralTest {

    @Test
    fun testIdentifier() {
        val expression = "\"Hello world\""
        val expected = Ast.Expression.Reference(Ast.Identifier(listOf(),"hello world"))

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testQualifiedIdentifier() {
        val expression = "foo.\"bar\".\"Hello world\""
        val expected = Ast.Expression.Reference(Ast.Identifier(listOf("foo", "bar"),"hello world"))

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testDoubleQuotedHiveStringLiteral() {
        val expression = "\"Hello world\""
        val expected = Ast.Expression.Literal.StringLiteral("Hello world")

        assertEquals(expected, parseExpression(expression, true, Dialect.HIVE))
    }


    @Test
    fun testStringLiteral() {
        val expression = "'Hello world'"
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
        val expected = Ast.Expression.Literal.NullLiteral()

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
        val expression = "date \'2018-01-01\'"
        val expected = Ast.Expression.Literal.DateLiteral("2018-01-01")

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testDateLiteralToString() {
        val expression = "date \'2018-01-01\'"
        val expected = "DATE '2018-01-01'"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testIntervalLiteral() {
        val expression = "interval '7' day"
        val expected = Ast.Expression.Literal.IntervalLiteral("7 DAY")

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testIntervalLiteralToString() {
        val expression = "interval '7' day"
        val expected = "INTERVAL '7 DAY'"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testSqlEscapedString() {
        val expression = "'a''b'"
        val expected = "'a\\'b'"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

}