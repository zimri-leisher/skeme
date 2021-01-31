fun main() {
    val interpreter = Interpreter()
    println(interpreter.run("""
        (define get-second (lambda (lst) (if (> 2 (length lst)) #f (car (cdr lst)))))
        (let ((l '(1 2 3))) (get-second l))
    """.trimIndent()))
}