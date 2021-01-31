fun main() {
    val interpreter = Interpreter(
        """
        (define get-second (lambda (lst) (if (> 2 (length lst)) #f (car (cdr lst)))))
        (let ((l '(1 2 3))) (get-second l))
    """.trimIndent()
    )
    println(interpreter.run())
}

fun repl() {
    print(">>")
    var line = readLine()!!
    while (line != ",q") {
        println(Interpreter(line).run())
        print(">>")
        line = readLine()!!
    }
}