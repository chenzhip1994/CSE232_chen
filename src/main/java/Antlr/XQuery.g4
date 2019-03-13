grammar XQuery;
//import XPath;

ap
	: doc '/' rp                  # ApChildren
	| doc '//' rp                 # ApAll
	;

doc
	: 'doc' '(' StringConstant ')'     #ApDoc
	;

rp
	: NAME                          # TagName     /* 一定出现在最后，属于递归结束的环节* ok */
	| '.'                          # Current     /* ok */
	| '..'                         # Parent        /*ok*/
	| '*'                          # AllChildren     /*ok*/
	| 'text()'                     # Txt              /*ok*/
	| '@' NAME                     # Attribute               /*ok*/
	| '(' rp ')'                   # RpwithP           /*ok*/
	| rp '/' rp                    # RpChildren        /*ok*/
	| rp '//' rp                   # RpAll             /*ok*/
	| rp '[' filter ']'            # RpFilter           /*ok*/
	| rp ',' rp                    # TwoRp             /*ok*/
	;

filter
	: rp                           # FltRp
	| rp '=' rp                    # FltEqual
	| rp 'eq' rp                   # FltEqual
	| rp '==' rp                   # FltIs
	| rp 'is' rp                   # FltIs
	| '(' filter ')'               # FltwithP
	| filter 'and' filter          # FltAnd
	| filter 'or' filter           # FltOr
	| 'not' filter                 # FltNot
	;


DOC: 'doc' ;
//TXT: 'text()';
NAME: [a-zA-Z0-9_-]+;


// XQuery
xq
    : Variable                                                                 # XqVariable
    | StringConstant                                                           # XqConstant
    | ap                                                                       # XqAp
    | '(' xq ')'                                                               # XqwithP
    | xq '/' rp                                                                # XqRp
    | xq '//' rp                                                               # XqAll
    | xq ',' xq                                                                # XqTwoXq
    | '<' NAME '>' '{' xq '}' '</' NAME '>'                                    # XqTag
    | letClause xq                                                             # XqLet
    | forClause letClause? whereClause? returnClause                           # XqFLWR
    | 'join' '(' xq ',' xq ',' tList ',' tList ')'                           # XqJoin
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
// Ignore White Space
WhiteSpace: [ \n\t\r]+ -> skip;
tList : '[' NAME (',' NAME)* ']' ;