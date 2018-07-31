package sqlparser

import sqlparser.Ast.*

abstract class AstListener {
    open fun enterFile(node: File) {}
    open fun exitFile(node: File) {}

    open fun enterStatement(node: Statement) {}
    open fun exitStatement(node: Statement) {}

    open fun enterCreateSchemaStatement(node: Statement.CreateSchema) {}
    open fun exitCreateSchemaStatement(node: Statement.CreateSchema) {}

    open fun enterIdentifier(node: Identifier) {}
    open fun exitIdentifier(node: Identifier) {}

    open fun enterColumnDefinition(node: ColumnDefinition) {}
    open fun exitColumnDefinition(node: ColumnDefinition) {}

    open fun enterCreateTableStatement(node: Statement.CreateTable) {}
    open fun exitCreateTableStatement(node: Statement.CreateTable) {}
}



class AstWalker(val listener: AstListener) {
    fun walk(node: Ast) {
        when(node) {
            is File -> walk(node)
            is Statement -> walk(node)
            is Identifier -> walk(node)
            is ColumnDefinition -> walk(node)
        }
    }

    fun walk(node: File) {
        listener.enterFile(node)
        node.statements.forEach(::walk)
        listener.exitFile(node)
    }

    fun walk(node: Statement) {
        when(node) {
            is Statement.CreateSchema -> walk(node)
            is Statement.CreateTable -> walk(node)
        }
    }

    fun walk(node: Statement.CreateSchema) {
        listener.enterStatement(node)
        listener.enterCreateSchemaStatement(node)
        walk(node.schemaName)
        listener.exitCreateSchemaStatement(node)
        listener.exitStatement(node)
    }

    fun walk(node: Statement.CreateTable) {
        listener.enterStatement(node)
        listener.enterCreateTableStatement(node)
        walk(node.tableName)
        node.columns.forEach { walk(it) }
        listener.exitCreateTableStatement(node)
        listener.exitStatement(node)
    }

    fun walk(node: Identifier) {
        listener.enterIdentifier(node)
        listener.exitIdentifier(node)
    }

    fun walk(node: ColumnDefinition) {
        listener.enterColumnDefinition(node)
        walk(node.columnName)
        listener.exitColumnDefinition(node)
    }
}