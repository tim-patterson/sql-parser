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
        val expected = "1 * 2 + 3 / 4"

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

    @Test
    fun testIn() {
        val expression = "foobar in(1,2,3)"
        val expected = FunctionCall("IN", listOf(
                Reference(Ast.Identifier(null, "foobar")),
                IntLiteral(1),
                IntLiteral(2),
                IntLiteral(3)
        ), infix = true)

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testInToString() {
        val expression = "foobar in(1,2,3)"
        val expected = "foobar IN (1, 2, 3)"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testNotIn() {
        val expression = "1+ 2 not in(1,2,3)"
        val expected = FunctionCall("NOT IN", listOf(
                FunctionCall("+", listOf(IntLiteral(1), IntLiteral(2)), true),
                IntLiteral(1),
                IntLiteral(2),
                IntLiteral(3)
        ), infix = true)

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testNotInToString() {
        val expression = "1+ 2 not in(1,2,3)"
        val expected = "1 + 2 NOT IN (1, 2, 3)"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testAndOr() {
        val expression = "1 = 3 and 2 < 4"
        val expected = FunctionCall("AND", listOf(
                FunctionCall("=", listOf(IntLiteral(1), IntLiteral(3)), true),
                FunctionCall("<", listOf(IntLiteral(2), IntLiteral(4)), true)
        ), infix = true)

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testAndOrString() {
        val expression = "1 = 3 and 2 < 4"
        val expected = "1 = 3 AND 2 < 4"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testArrayConstructor() {
        val expression = "array[1,2,3]"
        val expected = FunctionCall("ARRAY", listOf(
                IntLiteral(1), IntLiteral(2), IntLiteral(3)
        ))

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testArrayConstructorString() {
        val expression = "array[1,2,3]"
        val expected = "ARRAY[1, 2, 3]"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testCase1() {
        val expression = "case when a then 1 when b then 2 else 3 end"
        val expected = Case(null, listOf(
                Reference(Ast.Identifier(null, "a")) to IntLiteral(1),
                Reference(Ast.Identifier(null, "b")) to IntLiteral(2)
        ), IntLiteral(3))

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testCase1String() {
        val expression = "case when a then 1 when b then 2 else 3 end"
        val expected = """CASE
            |  WHEN a THEN 1
            |  WHEN b THEN 2
            |  ELSE 3
            |END
        """.trimMargin()

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testCase2() {
        val expression = "case foo when a then 1 when b then 2 else 3 end"
        val expected = Case(Reference(Ast.Identifier(null, "foo")), listOf(
                Reference(Ast.Identifier(null, "a")) to IntLiteral(1),
                Reference(Ast.Identifier(null, "b")) to IntLiteral(2)
        ), IntLiteral(3))

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testCase2String() {
        val expression = "case foo when a then 1 when b then 2 else 3 end"
        val expected = """CASE foo
            |  WHEN a THEN 1
            |  WHEN b THEN 2
            |  ELSE 3
            |END
        """.trimMargin()

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

    @Test
    fun testCast() {
        val expression = "cast('2018-01-01' as date)"
        val expected = Cast(Literal.StringLiteral("2018-01-01"), "DATE")

        assertEquals(expected, parseExpression(expression, true))
    }

    @Test
    fun testCastToString() {
        val expression = "cast('2018-01-01' as date)"
        val expected = "CAST('2018-01-01' AS DATE)"

        assertEquals(expected, SqlPrinter.from(parseExpression(expression, true)))
    }

}