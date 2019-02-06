grammar XPath;

doc
	: DOC '(' '"' fname '"' ')'       #ApDoc
	;

fname
	: NAME ('.' NAME)?
	;

ap
	: doc '/' rp                  # ApChildren
	| doc '//' rp                 # ApAll
	;

rp
	: NAME                          # TagName     /* 一定出现在最后，属于递归结束的环节* ok */
	| '.'                          # Current     /* ok */
	| '..'                         # Parent        /*ok*/
	| '*'                          # AllChildren     /*ok*/
	| TXT                          # Txt              /*ok*/
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
TXT: 'text()';
NAME: [a-zA-Z0-9_-]+;
WhiteSpace : [\r\t]+ -> skip;