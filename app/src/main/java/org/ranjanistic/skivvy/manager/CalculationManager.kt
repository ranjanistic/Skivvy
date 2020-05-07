package org.ranjanistic.skivvy.manager

import org.ranjanistic.skivvy.R
import org.ranjanistic.skivvy.Skivvy
import kotlin.math.*

@ExperimentalStdlibApi
class CalculationManager(var skivvy: Skivvy) {
    private val nothing = ""
    fun totalOperatorsInExpression(expression: String): Int {
        var expIndex = 0
        var totalOps = 0
        while (expIndex < expression.length) {
            var opIndex = 0
            while (opIndex < skivvy.operators.size) {
                if (expression[expIndex].toString() == skivvy.operators[opIndex]) {
                    ++totalOps              //counting total
                }
                ++opIndex
            }
            ++expIndex
        }
        return totalOps
    }

    /**
     *  The following block stores the position of operators in the given expression
     *  in  a new array (of Integers), irrespective of repetition of operators.
     *  @param expression: the expression string
     *  @return the array of positions of operators in given string
     */
    fun positionsOfOperatorsInExpression(expression: String): Array<Int?> {
        var expIndex = 0
        val expOperatorPos = arrayOfNulls<Int>(totalOperatorsInExpression(expression))
        var expOpIndex = 0
        while (expIndex < expression.length) {
            var opIndex = 0
            while (opIndex < skivvy.operators.size) {
                if (expression[expIndex].toString() == skivvy.operators[opIndex]) {
                    expOperatorPos[expOpIndex] = expIndex         //saving operator positions
                    ++expOpIndex
                }
                ++opIndex
            }
            ++expIndex
        }
        return expOperatorPos
    }

    /**
     * The following block extracts values from given expression, char by char, and stores them
     * in an array of Strings, by grouping digits in form of numbers at the same index as string,
     * and skivvy.operators in the expression at a separate index if array of Strings.
     *  For ex - Let the given expression be :   1234/556*89+4-23
     *  Starting from index = 0, the following block will store digits till '/'  at index =0 of empty array of Strings, then
     *  will store '/' itself at index =  1 of empty array of Strings. Then proceeds to store 5, 5  and 6
     *  at the same index = 2 of e.a. of strings. And stores the next operator '*' at index = 3, and so on.
     *  Thus a distinction between operands and operators is created and stored in a new array (of strings).
     *  @param expression: takes expression as a string
     *  @param sizeOfArray: takes size of segmented array to be formed (2*total operators+1)
     *  @return the segmented array of expression
     */
    fun segmentizeExpression(expression: String, sizeOfArray: Int): Array<String?>? {
        val arrayOfExpression = arrayOfNulls<String>(sizeOfArray)
        val expOperatorPos = positionsOfOperatorsInExpression(expression)
        var expArrayIndex = 0
        var positionInExpression = expArrayIndex
        var positionInOperatorPos = positionInExpression
        while (positionInOperatorPos < expOperatorPos.size && positionInExpression < expression.length) {
            while (positionInExpression < expOperatorPos[positionInOperatorPos]!!) {
                if (arrayOfExpression[expArrayIndex] == null) {
                    arrayOfExpression[expArrayIndex] = expression[positionInExpression].toString()
                } else {
                    arrayOfExpression[expArrayIndex] += expression[positionInExpression].toString()
                }
                ++positionInExpression
            }
            ++expArrayIndex
            if (positionInExpression == expOperatorPos[positionInOperatorPos]) {
                if (arrayOfExpression[expArrayIndex] == null) {
                    arrayOfExpression[expArrayIndex] = expression[positionInExpression].toString()
                } else {
                    arrayOfExpression[expArrayIndex] += expression[positionInExpression].toString()
                }
                ++expArrayIndex
            }
            ++positionInExpression
            ++positionInOperatorPos
            if (positionInOperatorPos >= expOperatorPos.size) {
                while (positionInExpression < expression.length) {
                    if (arrayOfExpression[expArrayIndex] == null) {
                        arrayOfExpression[expArrayIndex] =
                            expression[positionInExpression].toString()
                    } else {
                        arrayOfExpression[expArrayIndex] += expression[positionInExpression].toString()
                    }
                    ++positionInExpression
                }
            }
        }
        //if operator comes first, place zero at null index
        if (arrayOfExpression[arrayOfExpression.size - 1] == null)
            return null
        if (arrayOfExpression[0] == null) {
            arrayOfExpression[0] = "0"
        }
        return arrayOfExpression
    }

    fun evaluateFunctionsInExpressionArray(arrayOfExpression: Array<String?>): Array<String?>? {
        var fin = 0
        while (fin < arrayOfExpression.size) {
            if (arrayOfExpression[fin]!!.contains(skivvy.textPattern)) {
                if (!arrayOfExpression[fin]!!.contains(skivvy.numberPattern)) {
                    if (arrayOfExpression[fin + 1]!! == "+") {
                        arrayOfExpression[fin + 1] = nothing
                        var lk = fin + 2
                        while (lk < arrayOfExpression.size) {
                            if (lk == fin + 2) {
                                arrayOfExpression[lk - 2] += arrayOfExpression[lk]
                            } else {
                                arrayOfExpression[lk - 2] = arrayOfExpression[lk]
                            }
                            ++lk
                        }
                        arrayOfExpression[arrayOfExpression.size - 1] = nothing
                        arrayOfExpression[arrayOfExpression.size - 2] = nothing
                    } else if (arrayOfExpression[fin + 1]!! == "-") {
                        if (arrayOfExpression[fin + 2]!!.contains(skivvy.numberPattern)) {
                            arrayOfExpression[fin + 2] =
                                (0 - arrayOfExpression[fin + 2]!!.toFloat()).toString()
                            var lk = fin + 2
                            while (lk < arrayOfExpression.size) {
                                if (lk == fin + 2) {
                                    arrayOfExpression[lk - 2] += arrayOfExpression[lk]
                                } else {
                                    arrayOfExpression[lk - 2] = arrayOfExpression[lk]
                                }
                                ++lk
                            }
                            arrayOfExpression[arrayOfExpression.size - 1] = nothing
                            arrayOfExpression[arrayOfExpression.size - 2] = nothing
                        } else return null
                    } else return null
                }
                arrayOfExpression[fin] = operateFuncWithConstant(arrayOfExpression[fin]!!)
                arrayOfExpression[fin] = handleExponentialTerm(arrayOfExpression[fin]!!)
                if (arrayOfExpression[fin]!! == nothing || !arrayOfExpression[fin]!!.contains(skivvy.numberPattern))
                    return null
            }
            ++fin
        }
        return arrayOfExpression
    }

    fun handleExponentialTerm(value:String):String{
        return if(value.contains(skivvy.textPattern)) {
            when {
                value.contains("E-", false) -> {
                    "0"
                }
                value.contains("E", false) -> {
                    skivvy.getString(R.string.infinity)
                }
                else -> nothing
            }
        } else value
    }

    //To operate function with a constant in beginning (to be multiplied)
    fun operateFuncWithConstant(func: String):String?{
        val tempp = func.replace(skivvy.textPattern,"|")
        val numBefore = tempp.substringBefore("|")
        val numAfter = tempp.substringAfterLast("|")
        return if(numBefore.contains(skivvy.numberPattern) && numAfter.contains(skivvy.numberPattern)){
            this.functionOperate(
                func.replace(skivvy.numberPattern,nothing).replace(".",nothing)
                    + tempp.substringAfterLast("|"))?.toFloat()?.let {
                this.operate(numBefore.toFloat(),'*',
                    it
                ).toString()
            }
        } else if(numAfter.contains(skivvy.numberPattern)){
            this.functionOperate(func)
        } else{
          nothing  
        }
    }
    /**
     * Checks if expression doesn't have any illegal characters,
     * and returns true if operatable.
     */
    fun isExpressionOperatable(expression: String): Boolean {
        var localExp = expression
        if (!localExp.contains(skivvy.numberPattern)) {
            return false
        } else {
            localExp = localExp.replace(skivvy.numberPattern, nothing)
        }
        val validCharsOfExpression = arrayOf(".")
        val operatorsFunctionsNumbers =
            arrayOf(skivvy.operators, skivvy.mathFunctions, validCharsOfExpression)
        var kkk = 0
        while (kkk < operatorsFunctionsNumbers.size) {
            var kk = 0
            while (kk < operatorsFunctionsNumbers[kkk].size) {
                localExp = localExp.replace(operatorsFunctionsNumbers[kkk][kk], nothing)
                ++kk
            }
            ++kkk
        }
        return localExp == nothing
    }

    fun isExpressionArrayOnlyNumbersAndOperators(arrayOfExpression: Array<String?>): Boolean {
        var fci = 0
        while (fci < arrayOfExpression.size) {
            if (arrayOfExpression[fci] != null) {
                if (arrayOfExpression[fci]!!.contains(skivvy.textPattern) &&
                    arrayOfExpression[fci]!!.length > 1         //for symbols which are letters
                ) {
                    return false
                }
            } else {
                return false
            }
            ++fci
        }
        return true
    }

    /**
     * Considering having the new array of strings, the proper segmented
     * expression as
     * @param arrayOfExpression
     * with operators at every even position of the array (at odd indices),
     * the following block of code will evaluate the expression according to the BODMAS rule.
     * @return the final answer solved at index = 0 of the given array of expression.
     */
    fun expressionCalculation(arrayOfExpression: Array<String?>): String {
        var nullPosCount = 0
        var opIndex = 0
        while (opIndex < skivvy.operators.size) {
            var opPos = 1
            while (opPos < arrayOfExpression.size - nullPosCount) {
                if (arrayOfExpression[opPos] == skivvy.operators[opIndex]) {
                    if (arrayOfExpression[opPos] == "-") {
                        arrayOfExpression[opPos + 1] =
                            (0 - arrayOfExpression[opPos + 1]!!.toFloat()).toString()
                        arrayOfExpression[opPos] = "+"
                    }
                    try {
                        arrayOfExpression[opPos - 1] = this.operate(
                            arrayOfExpression[opPos - 1]!!.toFloat(),
                            arrayOfExpression[opPos]!!.toCharArray()[0],
                            arrayOfExpression[opPos + 1]!!.toFloat()
                        ).toString()
                    } catch (e: Exception) {
                        arrayOfExpression[opPos - 1] = "point"
                    }
                    var j = opPos
                    while (j + 2 < arrayOfExpression.size) {
                        arrayOfExpression[j] = arrayOfExpression[j + 2]
                        ++j
                    }
                    nullPosCount += 2
                    if (arrayOfExpression.size > 3 &&
                        arrayOfExpression[opPos] == skivvy.operators[opIndex]
                    ) {    //if replacing operator is same as the replaced one
                        opPos -= 2            //index two indices back so that it returns at same position again
                    }
                }
                opPos += 2        //next index of operator in array of expression
            }
            ++opIndex       //next operator
        }
        return returnValidResult(arrayOfExpression)
    }
    fun returnValidResult(result: Array<String?>):String{
        return when {
            result.contentDeepToString().contains("point")->
                skivvy.getString(R.string.invalid_expression)
            result.contentDeepToString().contains("NaN")->
                skivvy.getString(R.string.undefined_result)
            else-> formatToProperValue(result[0].toString())     //final result stored at index = 0
        }

    }

    /**
     * This performs mathematical operations between two operands with given operator
     * @param operand1 : The first operand
     * @param operand2: The next operand
     * @param operator: The mathematical operator according to which operation will be performed on [operand1] and [operand2]
     * @return : Returns solved operation result if valid [operator] is provided, else returns null.
     */
    fun operate(operand1: Float, operator: Char, operand2: Float): Float? {
        return when (operator) {
            '/' -> operand1.div(operand2)
            '*' -> operand1.times(operand2)
            '+' -> operand1.plus(operand2)
            '-' -> operand1.minus(operand2)
            'p' -> (operand1.div(100)).times(operand2)
            'm' -> operand1.rem(operand2)
            '^' -> operand1.toDouble().pow(operand2.toDouble()).toFloat()
            else -> null
        }
    }
    private fun functionOperate(func: String): String? {
        try {
            return when {
                func.contains("sin") -> sin(
                    func.replace(skivvy.textPattern, nothing).toFloat().toRadian(skivvy.getAngleUnit())
                ).toString()
                func.contains("cos") -> cos(
                    func.replace(skivvy.textPattern, nothing).toFloat().toRadian(skivvy.getAngleUnit())
                ).toString()
                func.contains("tan") -> tan(
                    func.replace(skivvy.textPattern, nothing).toFloat().toRadian(skivvy.getAngleUnit())
                ).toString()
                func.contains("cot") -> (1.div(tan(
                    func.replace(skivvy.textPattern, nothing).toFloat().toRadian(skivvy.getAngleUnit())
                ))).toString()
                func.contains("sec") -> (1.div(cos(
                    func.replace(skivvy.textPattern, nothing).toFloat().toRadian(skivvy.getAngleUnit())
                ))).toString()
                func.contains("cosec") -> (1.div(sin(
                    func.replace(skivvy.textPattern, nothing).toFloat().toRadian(skivvy.getAngleUnit())
                ))).toString()
                func.contains("log") -> {
                    log(func.replace(skivvy.textPattern, nothing).toFloat(), 10F).toString()
                }
                func.contains("ln") -> {
                    ln1p(func.replace(skivvy.textPattern, nothing).toFloat()).toString()
                }
                func.contains("sqrt") -> {
                    (func.replace(skivvy.textPattern, nothing).toFloat().pow(0.5F)).toString()
                }
                func.contains("cbrt") -> {
                    (func.replace(skivvy.textPattern, nothing).toDouble()
                        .pow(1 / 3.toDouble())).toString()
                }
                func.contains("exp") -> {
                    (exp(func.replace(skivvy.textPattern, nothing).toFloat())).toString()
                }
                func.contains("fact") -> {
                    factorialOf(func.replace(skivvy.textPattern, nothing).toInt()).toString()
                }
                else -> skivvy.getString(R.string.invalid_expression)
            }
        } catch(e:Exception){
            return skivvy.getString(R.string.invalid_expression)
        }
    }

    private fun factorialOf(num: Int): Long {
    var result = 1L
    var i = 1
    while (i<=num){
        result *= i
        ++i
    }
        return result
    }

    private fun isFloat(value: String): Boolean {
        return value.toFloat() - value.toFloat().toInt() != 0F
    }

    fun formatToProperValue(value: String): String {
        return if (isFloat(value)) {
            value.toFloat().toString()
        } else value.toFloat().toInt().toString()
    }
    private fun Number.toRadian(angle:String): Float {
        return when(angle){
            "rad"->this.toFloat()*1F
            else-> this.toFloat()*PI.div(180).toFloat()
        }
    }
}

