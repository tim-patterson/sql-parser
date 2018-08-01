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
            is Statement.SelectStmt -> render(node)
            is ColumnDefinition -> render(node)
            is Identifier -> render(node)
            is Expression -> render(node)
            is NamedExpression -> render(node)
            is OrderExpression -> render(node)
            is SelectClause -> render(node)
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
              node.columns.joinToString(",\n  ", "  ", "\n") { render(it) } +
                ");"
    }

    protected open fun render(node: Statement.SelectStmt): String {
        return "${render(node.selectClause)};"
    }

    protected open fun render(node: SelectClause): String {
        return "SELECT " + node.selectExpressions.joinToString{ render(it) }
    }

    protected open fun render(node: NamedExpression): String {
        return if (node.name != null) {
            "${render(node.expression)} AS ${node.name}"
        } else {
            render(node.expression)
        }
    }

    protected open fun render(node: OrderExpression): String {
        val ascDesc = if(node.asc) "ASC" else "DESC"
        return "${render(node.expression)} $ascDesc"
    }

    protected open fun render(node: Expression): String {
        return when(node) {
            is Expression.Literal -> render(node)
            is Expression.Reference -> render(node)
            is Expression.FunctionCall -> render(node)
        }
    }

    protected open fun render(node: Expression.Reference): String {
        return render(node.identifier)
    }

    protected open fun render(node: Expression.FunctionCall): String {
        val rawArgs = node.args

        return if(node.infix) {
            // For each of the args if its another infix function then we need to wrap in
            // brackets
            val args = rawArgs.map {
                if (it is Expression.FunctionCall && it.infix) {
                    "(${render(it)})"
                } else {
                    render(it)
                }
            }

            if (args.size == 1) {
                // special case for unitary minus
                "${node.functionName} ${args[0]}"
            } else {
                "${args[0]} ${node.functionName} ${args[1]}"
            }

        } else {
            val args = rawArgs.map(::render)
            "${node.functionName}(${args.joinToString()})"
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