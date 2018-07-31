package sqlparser

import sqlparser.Ast.*
import sqlparser.Ast.Expression.Literal.*

open class SqlPrinter {

    companion object {
        fun from(ast: Ast): String {
            return SqlPrinter().render(ast)
        }
    }

    fun render(node: Ast): String {
        return when(node) {
            is File -> render(node)
            is Statement.CreateSchema -> render(node)
            is Statement.CreateTable -> render(node)
            is ColumnDefinition -> render(node)
            is Identifier -> render(node)
            is Expression -> render(node)
        }
    }

    protected open fun render(node: File): String {
        return node.statements.joinToString("\n\n") { render(it) }
    }

    protected open fun render(node: Statement.CreateSchema): String {
        return "CREATE SCHEMA ${render(node.schemaName)};"
    }

    protected open fun render(node: Statement.CreateTable): String {
        return "CREATE TABLE ${render(node.tableName)} (\n" +
              node.columns.map { render(it) }.joinToString(",\n  ", "  ", "\n") +
                ");"
    }

    protected open fun render(node: Expression): String {
        return when(node) {
            is Expression.Literal -> render(node)
        }
    }

    protected open fun render(node: Expression.Literal): String {
        return when(node) {
            is DateLiteral -> render(node)
            is FloatLiteral -> render(node)
            is IntLiteral -> render(node)
            is NullLiteral -> render(node)
            is BooleanLiteral -> render(node)
            is StringLiteral -> render(node)
        }
    }

    protected open fun render(node: DateLiteral): String {
        return "DATE '${node.value}'"
    }

    protected open fun render(node: FloatLiteral): String {
        return node.value.toString()
    }

    protected open fun render(node: IntLiteral): String {
        return node.value.toString()
    }

    protected open fun render(node: NullLiteral): String {
        return "NULL"
    }

    protected open fun render(node: BooleanLiteral): String {
        return if (node.value) {
            "TRUE"
        } else {
            "FALSE"
        }
    }

    protected open fun render(node: StringLiteral): String {
        return "\'${node.value.replace("'", "\\'")}\'"
    }


    open fun render(node: Ast.ColumnDefinition): String {
        return "${render(node.columnName)} ${node.type}"
    }

    open fun render(node: Ast.Identifier): String {
        return if (node.qualifier != null) {
            "${node.qualifier}.${node.identifier}"
        } else {
            node.identifier
        }
    }
}