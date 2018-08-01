package sqlparser

import org.antlr.v4.kotlinruntime.ast.Point
import org.antlr.v4.kotlinruntime.ast.Position


sealed class Ast {
    internal abstract val sourcePosition: SP

    data class File(val statements: List<Statement>, override val sourcePosition: SP = SP()): Ast()

    sealed class Statement: Ast() {
        data class CreateSchema(val schemaName: Identifier, val ifNotExists: Boolean = false, override val sourcePosition: SP = SP()): Statement()
        data class CreateTable(val tableName: Identifier, val columns: List<ColumnDefinition>, override val sourcePosition: SP = SP()): Statement()
        // Why have a selectStmt and and selectClause you may ask, the selectStmt is simply a wrapper that denotes
        // the statement is at the top level
        data class SelectStmt(val selectClause: SelectClause, override val sourcePosition: SP = SP()): Statement()
    }

    data class Identifier(val qualifier: String?, val identifier: String, override val sourcePosition: SP = SP()): Ast()
    data class ColumnDefinition(val columnName: Identifier, val type: String, override val sourcePosition: SP = SP()): Ast()
    sealed class Expression: Ast() {
        sealed class Literal: Expression() {
            data class DateLiteral(val value: String, override val sourcePosition: SP = SP()): Literal()
            data class StringLiteral(val value: String, override val sourcePosition: SP = SP()): Literal()
            data class IntLiteral(val value: Long, override val sourcePosition: SP = SP()): Literal()
            data class FloatLiteral(val value: Double, override val sourcePosition: SP = SP()): Literal()
            data class BooleanLiteral(val value: Boolean, override val sourcePosition: SP = SP()): Literal()
            data class  NullLiteral(override val sourcePosition: SP = SP()): Literal()
        }
        data class FunctionCall(val functionName: String, val args: List<Expression>, val infix: Boolean=false, override val sourcePosition: SP = SP()): Expression()
        data class Reference(val identifier: Ast.Identifier, override val sourcePosition: SP = SP()): Expression()
    }

    data class NamedExpression(val name: String?, val expression: Expression, override val sourcePosition: SP = SP()): Ast()
    data class OrderExpression(val expression: Expression, val asc: Boolean = true, override val sourcePosition: SP = SP()): Ast()
    data class SelectClause(val selectExpressions: List<NamedExpression>, override val sourcePosition: SP = SP()): Ast()
}

// Wrapper class to stop Ast objects using Source position for equality,
// This makes unit testing etc so much easier as well as comparing subtrees etc
class SourcePosition(val pos: Position? = null) {
    override fun equals(other: Any?) =  other is SourcePosition
    override fun hashCode() = 0
    /**
     * Method to return the enclosing position of two positions
     */
    operator fun plus(other: SourcePosition): SourcePosition {
        return when{
            pos == null -> other
            other.pos == null -> this
            else -> {
                SourcePosition(Position(min(pos.start, other.pos.start), max(pos.end, other.pos.end)))
            }
        }
    }
}
private typealias SP = SourcePosition

private fun min(point1: Point, point2: Point) =
        if (point1.isBefore(point2)) point1 else point2

private fun max(point1: Point, point2: Point) =
        if (point1.isBefore(point2)) point2 else point1