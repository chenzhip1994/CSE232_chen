grammar XQuery;
import XPath;

// XQuery
xq
    : Variable                                                                 # XqVariable
    | StringConstant                                                                    # XqConstant
    | ap                                                                       # XqAp
    | '(' xq ')'                                                               # XqwithP
    | xq '/' rp                                                                # XqRp
    | xq '//' rp                                                               # XqAll
    | xq ',' xq                                                                # XqTwoXq
    | '<' NAME '>' '{' xq '}' '</' NAME '>'                                    # XqTag
    | 'join' '(' xq ',' xq ',' tagList ',' tagList ')'                         # XqJoin
    | letClause xq                                                             # XqLet
    | forClause letClause? whereClause? returnClause                           # XqFLWR
    ;

// For Clause
forClause
    : 'for' Variable 'in' xq (',' Variable 'in' xq)*
    ;

// Let Clause
letClause
    : 'let' Variable ':=' xq (',' Variable ':=' xq)*
    ;

// Where Clause
whereClause
    : 'where' cond
    ;

// Return Clause
returnClause
    : 'return' xq
    ;

// Tag List
tagList
    : '[' (NAME (',' NAME)*)? ']'
    ;

// Condition
cond
    : xq ('=' | 'eq') xq                                                       # CondValueEqual
    | xq ('==' | 'is') xq                                                      # CondIdentityEqual
    | 'empty' '(' xq ')'                                                       # CondEmpty
    | 'some' Variable 'in' xq (',' Variable 'in' xq)* 'satisfies' cond         # CondSome
    | '(' cond ')'                                                             # CondWithP
    | cond 'and' cond                                                          # CondAnd
    | cond 'or' cond                                                           # CondOr
    | 'not' cond                                                               # CondNot
    ;

// Variable Name
StringConstant: '"' (~'"')* '"';
Variable: '$' NAME;