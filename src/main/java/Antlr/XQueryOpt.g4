grammar XQueryOpt;

// XQuery Rewriter
xq
    : 'for' Variable 'in' path (',' Variable 'in' path)* 'where' cond 'return' returnClause   #XqFWR
    ;

path
    : ('doc' '(' StringConstant ')' | Variable) (sep NAME)*
	| ('doc' '(' StringConstant ')' | Variable) (sep NAME)* sep 'text()'
    ;

sep
	: '/' 
	| '//'
	;

// Return Clause
returnClause
    : Variable                                                              #XqreturnVar
    | returnClause  ',' returnClause                                        #XqTworeturn
    | '<' NAME '>' '{'returnClause'}' '</' NAME '>'                         #XqTagreturn
    | path                                                                  #Xqreturnpath
    ;

// Condition
cond
    : (Variable|StringConstant) ('=' | 'eq') (Variable|StringConstant)       # CondEqual
    | cond 'and' cond                                                        # CondAnd
    ;

// Variable Name
NAME: [a-zA-Z0-9_-]+;
StringConstant: '"' (~'"')* '"';
Variable: '$' NAME;
// Ignore White Space
WhiteSpace: [ \n\t\r]+ -> skip;
