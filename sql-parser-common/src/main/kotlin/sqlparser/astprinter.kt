package sqlparser

open class SqlPrinter {

    companion object {
        fun from(ast: Ast): String {
            return SqlPrinter().render(ast)
        }
    }

    fun render(node: Ast): String {
        return when(node) {
            is Ast.File -> render(node)
            is Ast.Statement.CreateSchema -> render(node)
            is Ast.Statement.CreateTable -> render(node)
            is Ast.ColumnDefinition -> render(node)
            is Ast.Identifier -> render(node)
        }
    }

    open fun render(node: Ast.File): String {
        return node.statements.joinToString("\n\n") { render(it) }
    }

    open fun render(node: Ast.Statement.CreateSchema): String {
        return "CREATE SCHEMA ${render(node.schemaName)};"
    }

    open fun render(node: Ast.Statement.CreateTable): String {
        return "CREATE TABLE ${render(node.tableName)} (\n" +
              node.columns.map { render(it) }.joinToString(",\n  ", "  ", "\n") +
                ");"
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