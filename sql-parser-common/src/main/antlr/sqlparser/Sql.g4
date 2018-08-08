grammar Sql;

@header {
 package sqlparser;
}


tokens { STRING_LITERAL }

file
  : stmt (';' stmt)* ';'? EOF
  ;

singleStmt
  : stmt ';'? EOF
  ;

singleExpression
  : expression EOF
  ;

stmt
  : createSchemaStmt
  | createTableStmt
  | selectStmt
  ;

createSchemaStmt
  : CREATE (DATABASE | SCHEMA) ifNotExists? simpleIdentifier createSchemaStmtAuthorizationClause?
  | CREATE (DATABASE | SCHEMA) ifNotExists? createSchemaStmtAuthorizationClause
  ;

createSchemaStmtAuthorizationClause
  : AUTHORIZATION simpleIdentifier
  ;

createTableStmt
  : CREATE temporary? EXTERNAL? TABLE qualifiedIdentifier createTableStmtColumnList
  ;

selectStmt
  : selectOrUnion
  ;

createTableStmtColumnList
  : OP_OPEN_BRACKET createTableStmtColumnSpec (OP_COMMA createTableStmtColumnSpec)* OP_CLOSE_BRACKET
  ;

createTableStmtColumnSpec
  : simpleIdentifier dataType createTableStmtColumnSpecExtra*
  ;

createTableStmtColumnSpecExtra
  : NOT NULL
  | NULL
  | COMMENT STRING_LITERAL
  | CONSTRAINT simpleIdentifier
  | PRIMARY KEY
  ;

dataType
  : simpleIdentifier OP_OPEN_BRACKET POSITIVE_INT_LITERAL OP_CLOSE_BRACKET
  | simpleIdentifier OP_OPEN_BRACKET POSITIVE_INT_LITERAL OP_COMMA POSITIVE_INT_LITERAL OP_CLOSE_BRACKET
  | ARRAY OP_LT dataType OP_GT
  | STRUCT OP_LT structMember (OP_COMMA structMember)* OP_GT
  | INTERVAL intervalUnits
  | INTERVAL intervalUnits TO intervalUnits
  | simpleIdentifier
  ;

structMember
  : simpleIdentifier OP_COLON dataType
  ;

intervalUnits
  : YEAR
  | MONTH
  | DAY
  | HOUR
  | MINUTE
  | SECOND
  ;

temporary
  : TEMP
  | TEMPORARY
  ;

withClause
  : WITH withClauseItem (OP_COMMA withClauseItem)*
  ;

withClauseItem
  : simpleIdentifier AS OP_OPEN_BRACKET selectOrUnion OP_CLOSE_BRACKET
  ;

selectOrUnion
  : selectOrUnion UNION ALL? selectClause
  | selectClause
  ;

selectClause
  : withClause?
  SELECT DISTINCT? namedExpression (OP_COMMA namedExpression)*
  fromClause?
  whereClause?
  groupByClause?
  havingClause?
  orderByClause?
  limitClause?
  ;

fromClause
  : FROM fromItem (OP_COMMA fromItem)*
  ;

fromItem
  : dataSource (AS? simpleIdentifier)?
  | fromItem (LEFT | RIGHT | FULL)? OUTER JOIN fromItem ON expression
  | fromItem (LEFT | RIGHT | FULL) JOIN fromItem ON expression
  | fromItem INNER? JOIN fromItem ON expression
  | fromItem CROSS JOIN fromItem
  | tableFunction
  ;

tableFunction
  : functionCall AS? simpleIdentifier tableFunctionColumnAliases
  ;

tableFunctionColumnAliases
  : OP_OPEN_BRACKET simpleIdentifier (OP_COMMA simpleIdentifier)* OP_CLOSE_BRACKET
  ;

whereClause
  : WHERE expression
  ;

groupByClause
  : GROUP BY expression (OP_COMMA expression)*
  ;

havingClause
  : HAVING expression
  ;

orderByClause
  : ORDER BY orderByExpression (OP_COMMA orderByExpression)*
  ;

orderByExpression
  : expression ( ASC | DESC )?
  ;

limitClause
  : LIMIT POSITIVE_INT_LITERAL
  ;

dataSource
  : qualifiedIdentifier
  | OP_OPEN_BRACKET selectOrUnion OP_CLOSE_BRACKET
  ;

namedExpression
  : expression
  | expression AS? simpleIdentifier
  ;

ifNotExists
  : IF NOT EXISTS
  ;

expression
  : literal
  | OP_OPEN_BRACKET expression OP_CLOSE_BRACKET
  | OP_MINUS expression
  | NOT expression
  | expression IS expression
  | expression IS NOT expression
  | expression LIKE expression
  | expression NOT LIKE expression
  | expression ( OP_MULT | OP_DIV | OP_MOD ) expression
  | expression ( OP_PLUS | OP_MINUS ) expression
  | expression ( OP_GT | OP_GTE | OP_LT | OP_LTE | OP_EQ | OP_NEQ ) expression
  | expression ( AND | OR ) expression
  | expression BETWEEN expression AND expression
  | expression NOT? IN OP_OPEN_BRACKET expression (OP_COMMA expression)* OP_CLOSE_BRACKET
  | qualifiedIdentifier
  | functionCall
  | caseStatement
  | cast
  | ARRAY OP_OPEN_SQUARE (expression (OP_COMMA expression)*)? OP_CLOSE_SQUARE
  | selectOrUnion
  ;

cast
  : (CAST | TRY_CAST) OP_OPEN_BRACKET expression AS dataType OP_CLOSE_BRACKET
  ;

caseStatement
  : CASE expression? caseStatementMatch* caseStatementElse? END
  ;

caseStatementMatch
  : WHEN expression THEN expression
  ;

caseStatementElse
  : ELSE expression
  ;

functionCall
  : simpleIdentifier OP_OPEN_BRACKET (DISTINCT? expression (OP_COMMA expression)*)? OP_CLOSE_BRACKET windowSpec?
  ;

windowSpec
  : OVER OP_OPEN_BRACKET windowSpecPartition? orderByClause? OP_CLOSE_BRACKET
  ;

windowSpecPartition
  : PARTITION BY expression
  ;

qualifiedIdentifier
  : simpleIdentifier (OP_DOT simpleIdentifier)?
  ;

simpleIdentifier
  : IDENTIFIER
  | keyword
  | OP_MULT
  ;

literal
  : STRING_LITERAL
  | POSITIVE_INT_LITERAL
  | POSITIVE_FLOAT_LITERAL
  | DATE STRING_LITERAL
  | INTERVAL STRING_LITERAL intervalUnits?
  | TRUE
  | FALSE
  | NULL
  ;

keyword
  : ADD
  | ALL
  | ALTER
  | ANALYZE
  | ARRAY
  | AS
  | ASC
  | AUTHORIZATION
  | BEGIN
  | BETWEEN
  | BY
  | CASCADE
  | CAST
  | CASE
  | CLUSTER
  | COMMENT
  | CONSTRAINT
  | CREATE
  | CROSS
  | DATABASE
  | DATE
  | DAY
  | DEFAULT
  | DELIMITED
  | DESC
  | DISTKEY
  | DISTINCT
  | DISTRIBUTE
  | DROP
  | ELSE
  | END
  | ESCAPED
  | EXISTS
  | EXTERNAL
  | FALSE
  | FIELDS
  | FIRST
  | FORMAT
  | FULL
  | FUNCTION
  | FROM
  | GRANT
  | GROUP
  | GROUPING
  | HAVING
  | HOUR
  | IF
  | INNER
  | INSERT
  | INTERVAL
  | INTO
  | IN
  | IS
  | INPUTFORMAT
  | JOIN
  | KEY
  | LAST
  | LATERAL
  | LEFT
  | LIKE
  | LIMIT
  | LINES
  | LOCATION
  | MACRO
  | MINUTE
  | MONTH
  | NOT
  | NULL
  | NULLS
  | ON
  | ORDER
  | OUTER
  | OUTPUTFORMAT
  | OVER
  | OVERWRITE
  | OWNER
  | PARTITION
  | PARTITIONED
  | PRECEDING
  | PRIMARY
  | RENAME
  | RESTRICT
  | RIGHT
  | ROW
  | ROWS
  | SCHEMA
  | SECOND
  | SERDE
  | SERDEPROPERTIES
  | SELECT
  | SEMI
  | SET
  | SETS
  | SORT
  | SORTKEY
  | STORED
  | STRING
  | STRUCT
  | TABLE
  | TBLPROPERTIES
  | TEMP
  | TEMPORARY
  | TERMINATED
  | TRY_CAST
  | THEN
  | TRUE
  | TRUNCATE
  | UNBOUNDED
  | UNION
  | WITH
  | WHEN
  | WHERE
  | VIEW
  | YEAR
  ;


// Key words
ADD: A D D;
AND: A N D;
ALL: A L L;
ALTER: A L T E R;
ANALYZE: A N A L Y Z E;
ARRAY: A R R A Y;
AS: A S;
ASC: A S C;
AUTHORIZATION: A U T H O R I Z A T I O N;
BEGIN: B E G I N;
BETWEEN: B E T W E E N;
BY: B Y;
CASCADE: C A S C A D E;
CASE: C A S E;
CAST: C A S T;
CLUSTER: C L U S T E R;
COMMENT: C O M M E N T;
CONSTRAINT: C O N S T R A I N T;
CREATE: C R E A T E;
CROSS: C R O S S;
DATABASE: D A T A B A S E;
DATE: D A T E;
DAY: D A Y;
DEFAULT: D E F A U L T;
DELIMITED: D E L I M I T E D;
DESC: D E S C;
DISTKEY: D I S T K E Y;
DISTINCT: D I S T I N C T;
DISTRIBUTE: D I S T R I B U T E;
DROP: D R O P;
ELSE: E L S E;
END: E N D;
ESCAPED: E S C A P E D;
EXISTS: E X I S T S;
EXTERNAL: E X T E R N A L;
FALSE: F A L S E;
FIELDS: F I E L D S;
FIRST: F I R S T;
FORMAT: F O R M A T;
FROM: F R O M;
FULL: F U L L;
FUNCTION: F U N C T I O N;
GRANT: G R A N T;
GROUPING: G R O U P I N G;
GROUP: G R O U P;
HAVING: H A V I N G;
HOUR: H O U R;
IF: I F;
INNER: I N N E R;
INSERT: I N S E R T;
INTERVAL: I N T E R V A L;
INTO: I N T O;
IN: I N;
IS: I S;
INPUTFORMAT: I N P U T F O R M A T;
JOIN: J O I N;
KEY: K E Y;
LAST: L A S T;
LATERAL: L A T E R A L;
LEFT: L E F T;
LIKE: L I K E;
LIMIT: L I M I T;
LINES: L I N E S;
LOCATION: L O C A T I O N;
MACRO: M A C R O;
MINUTE: M I N U T E;
MONTH: M O N T H;
NOT: N O T;
NULL: N U L L;
NULLS: N U L L S;
ON: O N;
ORDER: O R D E R;
OR: O R;
OUTER: O U T E R;
OUTPUTFORMAT: O U T P U T F O R M A T;
OVER: O V E R;
OVERWRITE: O V E R W R I T E;
OWNER: O W N E R;
PARTITION: P A R T I T I O N;
PARTITIONED: P A R T I T I O N E D;
PRECEDING: P R E C E D I N G;
PRIMARY: P R I M A R Y;
RENAME: R E N A M E;
RESTRICT: R E S T R I C T;
RIGHT: R I G H T;
ROWS: R O W S;
ROW: R O W;
SCHEMA: S C H E M A;
SECOND: S E C O N D;
SERDEPROPERTIES: S E R D E P R O P E R T I E S;
SERDE: S E R D E;
SELECT: S E L E C T;
SEMI: S E M I;
SETS: S E T S;
SET: S E T;
SORTKEY: S O R T K E Y;
SORT: S O R T;
STORED: S T O R E D;
STRING: S T R I N G;
STRUCT: S T R U C T;
TABLE: T A B L E;
TBLPROPERTIES: T B L P R O P E R T I E S;
TEMP: T E M P;
TEMPORARY: T E M P O R A R Y;
TERMINATED: T E R M I N A T E D;
THEN: T H E N;
TO: T O;
TRUE: T R U E;
TRUNCATE: T R U N C A T E;
TRY_CAST: T R Y '_' C A S T;
UNBOUNDED: U N B O U N D E D;
UNION: U N I O N;
WITH: W I T H;
WHEN: W H E N;
WHERE: W H E R E;
VIEW: V I E W;
YEAR: Y E A R;


OP_PLUS: '+';
OP_MINUS: '-';
OP_MULT: '*';
OP_DIV: '/';
OP_GT: '>';
OP_GTE: '>=';
OP_LT: '<';
OP_LTE: '<=';
OP_EQ: '=' | '==';
OP_NEQ: '!=' | '<>';
OP_NS_EQ: '<=>';
OP_DOT: '.';
OP_COMMA: ',';
OP_CONCAT: '||';
OP_OPEN_BRACKET: '(';
OP_CLOSE_BRACKET: ')';
OP_OPEN_SQUARE: '[';
OP_CLOSE_SQUARE: ']';
OP_COLON: ':';
OP_MOD: '%';

IDENTIFIER
 : [a-zA-Z_] [a-zA-Z_0-9]*
 ;

// Literals
POSITIVE_INT_LITERAL
 : DIGIT+
 ;

POSITIVE_FLOAT_LITERAL
 : DIGIT+ ( '.' DIGIT* )?
 ;

// These 3 rules aren't used directly by the parser, but rely on the code to change the
// token type to IDENTIFIER or STRING_LITERAL where appropriate
BACKTICKED_LIT
 : '`' ( ~'`' )* '`'
 ;

SINGLE_QUOTED_LIT
 : '\'' (('\\' .) | ~('\\' | '\''))* '\''
 ;

DOUBLE_QUOTED_LIT
 : '"' (('\\' .) | ~('\\' | '"'))* '"'
 ;

// Whitespace
SPACES
 : [ \u000B\t\r\n] -> channel(HIDDEN)
 ;

SINGLE_LINE_COMMENT
 : '--' ~[\r\n]* -> channel(2)
 ;

MULTI_LINE_COMMENT
 : '/*' .*? '*/' -> channel(2)
 ;

// Fragments
fragment DIGIT : [0-9];

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];