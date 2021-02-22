fun main() {
    val interpreter = Interpreter()
    println(interpreter.run("""
        (define make-account (lambda (initial-balance)
            (let ((balance initial-balance))
                (lambda (amount)
                    (set! balance (- balance amount))))))
        (define zimri (make-account 20))
        (zimri 4)
        (zimri 5)
    """.trimIndent()))
}