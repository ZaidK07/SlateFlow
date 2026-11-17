package com.zaid01.slateflow.reader

import io.noties.prism4j.GrammarLocator
import io.noties.prism4j.Prism4j
import java.util.regex.Pattern

internal class SlateGrammarLocator : GrammarLocator {
    private val grammars = mutableMapOf<String, Prism4j.Grammar>()

    override fun languages(): Set<String> = setOf(
        "kotlin", "java", "python", "javascript", "json", "bash", "cpp", "xml", "sql", "yaml"
    )

    override fun grammar(prism4j: Prism4j, language: String): Prism4j.Grammar? {
        val lang = language.lowercase().trim()
        grammars[lang]?.let { return it }

        val created: Prism4j.Grammar? = when (lang) {
            "kt", "kotlin" -> createKotlin()
            "java" -> createJava()
            "py", "python" -> createPython()
            "js", "javascript", "ts", "typescript" -> createJavaScript()
            "json" -> createJson()
            "sh", "bash", "zsh", "shell" -> createBash()
            "c", "cpp", "c++", "h", "hpp" -> createCpp()
            "xml", "html", "svg" -> createXml()
            "sql" -> createSql()
            "yaml", "yml" -> createYaml()
            else -> null
        }
        if (created != null) {
            grammars[lang] = created
        }
        return created
    }

    private fun p(regex: String, flags: Int = 0): Prism4j.Pattern =
        Prism4j.pattern(Pattern.compile(regex, flags))

    private fun createKotlin(): Prism4j.Grammar {
        val comment = Prism4j.token("comment", p("//.*"), p("/\\*[\\s\\S]*?\\*/"))
        val string = Prism4j.token("string", p("\"\"\"[\\s\\S]*?\"\"\""), p("\"([^\"\\\\]|\\\\.)*\""), p("'([^'\\\\]|\\\\.)*'"))
        val number = Prism4j.token("number", p("\\b0[xX][\\da-fA-F_]+[uUlL]?\\b"), p("\\b\\d+[\\d_]*(\\.[\\d_]+)?([eE][+-]?[\\d_]+)?[fF]?\\b"))
        val boolean = Prism4j.token("boolean", p("\\b(true|false)\\b"))
        val keyword = Prism4j.token("keyword", p("\\b(as|as\\?|break|class|continue|do|else|for|fun|if|in|!in|interface|is|!is|null|object|package|return|super|this|throw|try|typealias|typeof|val|var|when|while|by|catch|constructor|delegate|dynamic|field|file|finally|get|import|init|param|property|receiver|set|setparam|where|actual|abstract|annotation|companion|const|crossinline|data|enum|expect|external|final|infix|inline|inner|internal|lateinit|noinline|open|operator|out|override|private|protected|public|reified|sealed|suspend|tailrec|vararg)\\b"))
        val function = Prism4j.token("function", p("\\b[a-zA-Z_]\\w*(?=\\s*\\()"))
        val operator = Prism4j.token("operator", p("[-+*/%=!&|?:<>]+"))
        val punctuation = Prism4j.token("punctuation", p("[{}()\\[\\],.;]"))
        return Prism4j.grammar("kotlin", comment, string, number, boolean, keyword, function, operator, punctuation)
    }

    private fun createJava(): Prism4j.Grammar {
        val comment = Prism4j.token("comment", p("//.*"), p("/\\*[\\s\\S]*?\\*/"))
        val string = Prism4j.token("string", p("\"([^\"\\\\]|\\\\.)*\""), p("'([^'\\\\]|\\\\.)*'"))
        val number = Prism4j.token("number", p("\\b0[xX][\\da-fA-F_]+[lL]?\\b"), p("\\b\\d+[\\d_]*(\\.[\\d_]+)?([eE][+-]?[\\d_]+)?[fFdDlL]?\\b"))
        val boolean = Prism4j.token("boolean", p("\\b(true|false)\\b"))
        val keyword = Prism4j.token("keyword", p("\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|record|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|var|void|volatile|while|yield)\\b"))
        val function = Prism4j.token("function", p("\\b[a-zA-Z_]\\w*(?=\\s*\\()"))
        val operator = Prism4j.token("operator", p("[-+*/%=!&|?:<>]+"))
        val punctuation = Prism4j.token("punctuation", p("[{}()\\[\\],.;]"))
        return Prism4j.grammar("java", comment, string, number, boolean, keyword, function, operator, punctuation)
    }

    private fun createPython(): Prism4j.Grammar {
        val comment = Prism4j.token("comment", p("#.*"))
        val string = Prism4j.token("string", p("\"\"\"[\\s\\S]*?\"\"\""), p("'''[\\s\\S]*?'''"), p("[fFbBrRuU]?\"([^\"\\\\]|\\\\.)*\""), p("[fFbBrRuU]?'([^'\\\\]|\\\\.)*'"))
        val number = Prism4j.token("number", p("\\b0[xXbBoO][\\da-fA-F_]+\\b"), p("\\b\\d+[\\d_]*(\\.[\\d_]+)?([eE][+-]?[\\d_]+)?j?\\b"))
        val boolean = Prism4j.token("boolean", p("\\b(True|False|None)\\b"))
        val keyword = Prism4j.token("keyword", p("\\b(and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield|match|case)\\b"))
        val function = Prism4j.token("function", p("\\b[a-zA-Z_]\\w*(?=\\s*\\()"))
        val operator = Prism4j.token("operator", p("[-+*/%=!&|~^<>]+"))
        val punctuation = Prism4j.token("punctuation", p("[{}()\\[\\],.:;]"))
        return Prism4j.grammar("python", comment, string, number, boolean, keyword, function, operator, punctuation)
    }

    private fun createJavaScript(): Prism4j.Grammar {
        val comment = Prism4j.token("comment", p("//.*"), p("/\\*[\\s\\S]*?\\*/"))
        val string = Prism4j.token("string", p("`[\\s\\S]*?`"), p("\"([^\"\\\\]|\\\\.)*\""), p("'([^'\\\\]|\\\\.)*'"))
        val number = Prism4j.token("number", p("\\b0[xXbBoO][\\da-fA-F_]+n?\\b"), p("\\b\\d+[\\d_]*(\\.[\\d_]+)?([eE][+-]?[\\d_]+)?n?\\b"))
        val boolean = Prism4j.token("boolean", p("\\b(true|false|null|undefined)\\b"))
        val keyword = Prism4j.token("keyword", p("\\b(as|async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|finally|for|from|function|get|if|implements|import|in|instanceof|interface|let|new|of|package|private|protected|public|return|set|static|super|switch|this|throw|try|typeof|var|void|while|with|yield|type|namespace)\\b"))
        val function = Prism4j.token("function", p("\\b[a-zA-Z_]\\w*(?=\\s*\\()"))
        val operator = Prism4j.token("operator", p("[-+*/%=!&|?:<>^~]+"))
        val punctuation = Prism4j.token("punctuation", p("[{}()\\[\\],.;]"))
        return Prism4j.grammar("javascript", comment, string, number, boolean, keyword, function, operator, punctuation)
    }

    private fun createJson(): Prism4j.Grammar {
        val property = Prism4j.token("property", p("\"([^\"\\\\]|\\\\.)*\"(?=\\s*:)"))
        val string = Prism4j.token("string", p("\"([^\"\\\\]|\\\\.)*\""))
        val number = Prism4j.token("number", p("-?\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b"))
        val boolean = Prism4j.token("boolean", p("\\b(true|false|null)\\b"))
        val punctuation = Prism4j.token("punctuation", p("[{}()\\[\\],:]"))
        return Prism4j.grammar("json", property, string, number, boolean, punctuation)
    }

    private fun createBash(): Prism4j.Grammar {
        val comment = Prism4j.token("comment", p("#.*"))
        val string = Prism4j.token("string", p("\"([^\"\\\\]|\\\\.)*\""), p("'[^']*'"))
        val number = Prism4j.token("number", p("\\b\\d+\\b"))
        val keyword = Prism4j.token("keyword", p("\\b(if|then|else|elif|fi|case|esac|for|while|until|do|done|in|function|select|time)\\b"))
        val function = Prism4j.token("function", p("\\b(cd|echo|ls|cat|mkdir|rm|cp|mv|chmod|chown|grep|sed|awk|curl|wget|tar|git|export|source|alias)\\b"))
        val operator = Prism4j.token("operator", p("[|&;<>!]+"))
        val punctuation = Prism4j.token("punctuation", p("[{}()\\[\\],]"))
        return Prism4j.grammar("bash", comment, string, number, keyword, function, operator, punctuation)
    }

    private fun createCpp(): Prism4j.Grammar {
        val comment = Prism4j.token("comment", p("//.*"), p("/\\*[\\s\\S]*?\\*/"))
        val string = Prism4j.token("string", p("\"([^\"\\\\]|\\\\.)*\""), p("'([^'\\\\]|\\\\.)*'"))
        val number = Prism4j.token("number", p("\\b0[xX][\\da-fA-F_]+[uUlL]?\\b"), p("\\b\\d+[\\d_]*(\\.[\\d_]+)?([eE][+-]?[\\d_]+)?[fFlL]?\\b"))
        val boolean = Prism4j.token("boolean", p("\\b(true|false|nullptr)\\b"))
        val keyword = Prism4j.token("keyword", p("\\b(auto|break|case|catch|class|const|constexpr|continue|default|delete|do|else|enum|explicit|export|extern|final|for|friend|goto|if|inline|mutable|namespace|new|noexcept|operator|override|private|protected|public|return|sizeof|static|static_assert|struct|switch|template|this|throw|try|typedef|typeid|typename|union|using|virtual|void|volatile|while)\\b"))
        val function = Prism4j.token("function", p("\\b[a-zA-Z_]\\w*(?=\\s*\\()"))
        val operator = Prism4j.token("operator", p("[-+*/%=!&|?:<>^~]+"))
        val punctuation = Prism4j.token("punctuation", p("[{}()\\[\\],.;]"))
        return Prism4j.grammar("cpp", comment, string, number, boolean, keyword, function, operator, punctuation)
    }

    private fun createXml(): Prism4j.Grammar {
        val comment = Prism4j.token("comment", p("<!--[\\s\\S]*?-->"))
        val tag = Prism4j.token("tag", p("</?[a-zA-Z][a-zA-Z0-9:-]*\\b"))
        val attrValue = Prism4j.token("attr-value", p("\"[^\"]*\""), p("'[^']*'"))
        val attrName = Prism4j.token("attr-name", p("[a-zA-Z0-9:-]+(?=\\s*=)"))
        val punctuation = Prism4j.token("punctuation", p("[/<>=\"]"))
        return Prism4j.grammar("xml", comment, tag, attrName, attrValue, punctuation)
    }

    private fun createSql(): Prism4j.Grammar {
        val comment = Prism4j.token("comment", p("--.*"), p("/\\*[\\s\\S]*?\\*/"))
        val string = Prism4j.token("string", p("'([^'\\\\]|\\\\.)*'"))
        val number = Prism4j.token("number", p("\\b\\d+(\\.\\d+)?\\b"))
        val boolean = Prism4j.token("boolean", p("\\b(TRUE|FALSE|NULL)\\b", Pattern.CASE_INSENSITIVE))
        val keyword = Prism4j.token("keyword", p("\\b(SELECT|FROM|WHERE|INSERT|INTO|UPDATE|DELETE|JOIN|LEFT|RIGHT|INNER|OUTER|ON|GROUP|BY|ORDER|HAVING|LIMIT|OFFSET|CREATE|TABLE|DROP|ALTER|ADD|INDEX|PRIMARY|KEY|FOREIGN|REFERENCES|NOT|AND|OR|AS|IN|EXISTS|BETWEEN|LIKE|IS|UNION|ALL|DISTINCT|CASE|WHEN|THEN|ELSE|END)\\b", Pattern.CASE_INSENSITIVE))
        val function = Prism4j.token("function", p("\\b(COUNT|SUM|AVG|MIN|MAX|COALESCE|NOW|DATE|SUBSTR|TRIM|LOWER|UPPER)(?=\\s*\\()", Pattern.CASE_INSENSITIVE))
        val operator = Prism4j.token("operator", p("[-+*/%=!<>]+"))
        val punctuation = Prism4j.token("punctuation", p("[()\\.,;]"))
        return Prism4j.grammar("sql", comment, string, number, boolean, keyword, function, operator, punctuation)
    }

    private fun createYaml(): Prism4j.Grammar {
        val comment = Prism4j.token("comment", p("#.*"))
        val property = Prism4j.token("property", p("^[\\t ]*[^\\s:]+:(?=\\s|$)", Pattern.MULTILINE))
        val string = Prism4j.token("string", p("\"([^\"\\\\]|\\\\.)*\""), p("'[^']*'"))
        val number = Prism4j.token("number", p("\\b\\d+(\\.\\d+)?\\b"))
        val boolean = Prism4j.token("boolean", p("\\b(true|false|yes|no|on|off|null)\\b", Pattern.CASE_INSENSITIVE))
        val punctuation = Prism4j.token("punctuation", p("[:,\\[\\]{}]"))
        return Prism4j.grammar("yaml", comment, property, string, number, boolean, punctuation)
    }
}
