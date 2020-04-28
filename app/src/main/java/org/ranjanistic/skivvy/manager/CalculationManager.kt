package org.ranjanistic.skivvy.manager

import org.ranjanistic.skivvy.R
import org.ranjanistic.skivvy.Skivvy
import kotlin.math.*

@ExperimentalStdlibApi
class CalculationManager(var skivvy: Skivvy) {

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
                        arrayOfExpression[fin + 1] = ""
                        var lk = fin + 2
                        while (lk < arrayOfExpression.size) {
                            if (lk == fin + 2) {
                                arrayOfExpression[lk - 2] += arrayOfExpression[lk]
                            } else {
                                arrayOfExpression[lk - 2] = arrayOfExpression[lk]
                            }
                            ++lk
                        }
                        arrayOfExpression[arrayOfExpression.size - 1] = ""
                        arrayOfExpression[arrayOfExpression.size - 2] = ""
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
                            arrayOfExpression[arrayOfExpression.size - 1] = ""
                            arrayOfExpression[arrayOfExpression.size - 2] = ""
                        } else return null
                    } else return null
                }
                arrayOfExpression[fin] = operateFuncWithConstant(arrayOfExpression[fin]!!)
                if (arrayOfExpression[fin]!!.contains(skivvy.textPattern)) {
                    when {
                        arrayOfExpression[fin]!!.contains("E-",false) -> {
                            arrayOfExpression[fin] = "0"
                        }
                        arrayOfExpression[fin]!!.contains("E",false) -> {
                            arrayOfExpression[fin] = skivvy.getString(R.string.infinity)
                        }
                        else -> return null
                    }
                } else if(!arrayOfExpression[fin]!!.contains(skivvy.numberPattern)){
                    return null
                }
            }
            ++fin
        }
        return arrayOfExpression
    }

    fun operateFuncWithConstant(func: String):String?{
        val tempp = func.replace(skivvy.textPattern,"|")
        val numBefore = tempp.substringBefore("|")
        return if(numBefore.contains(skivvy.numberPattern)){
            this.functionOperate(func.replace(skivvy.numberPattern,"").replace(".","")
                    + tempp.substringAfterLast("|"))?.toFloat()?.let {
                this.operate(numBefore.toFloat(),'*',
                    it
                ).toString()
            }
        } else {
            this.functionOperate(func)
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
            localExp = localExp.replace(skivvy.numberPattern, "")
        }
        val validCharsOfExpression = arrayOf(".")
        val operatorsFunctionsNumbers =
            arrayOf(skivvy.operators, skivvy.mathFunctions, validCharsOfExpression)
        var kkk = 0
        while (kkk < operatorsFunctionsNumbers.size) {
            var kk = 0
            while (kk < operatorsFunctionsNumbers[kkk].size) {
                localExp = localExp.replace(operatorsFunctionsNumbers[kkk][kk], "")
                ++kk
            }
            ++kkk
        }
        return localExp == ""
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
        return when {
            arrayOfExpression.contentDeepToString().contains("point")->
                "Invalid expression"
            arrayOfExpression.contentDeepToString().contains("NaN")->
                "Undefined result in my logic"
            else-> formatToProperValue(arrayOfExpression[0].toString())     //final result stored at index = 0
        }
    }

    fun operate(operand1: Float, operator: Char, operand2: Float): Float? {
        return when (operator) {
            '/' -> operand1 / operand2
            '*' -> operand1 * operand2
            '+' -> operand1 + operand2
            '-' -> operand1 - operand2
            'p' -> (operand1 / 100) * operand2
            'm' -> operand1 % operand2
            '^' -> operand1.toDouble().pow(operand2.toDouble()).toFloat()
            else -> null
        }
    }

    fun functionOperate(func: String): String? {
        return when {
            func.contains("sin") -> sin(
                func.replace(skivvy.textPattern, "").toFloat() * (PI / 180)
            ).toString()
            func.contains("cos") -> cos(
                func.replace(skivvy.textPattern, "").toFloat() * (PI / 180)
            ).toString()
            func.contains("tan") -> tan(
                func.replace(skivvy.textPattern, "").toFloat() * (PI / 180)
            ).toString()
            func.contains("cot") -> (1 / tan(
                func.replace(skivvy.textPattern, "").toFloat() * (PI / 180)
            )).toString()
            func.contains("sec") -> (1 / cos(
                func.replace(skivvy.textPattern, "").toFloat() * (PI / 180)
            )).toString()
            func.contains("cosec") -> (1 / sin(
                func.replace(skivvy.textPattern, "").toFloat() * (PI / 180)
            )).toString()
            func.contains("log") -> {
                log(func.replace(skivvy.textPattern, "").toFloat(), 10F).toString()
            }
            func.contains("ln") -> {
                ln1p(func.replace(skivvy.textPattern, "").toFloat()).toString()
            }
            func.contains("sqrt") -> {
                (func.replace(skivvy.textPattern, "").toFloat().pow(0.5F)).toString()
            }
            func.contains("cbrt") -> {
                (func.replace(skivvy.textPattern, "").toDouble().pow(1 / 3.toDouble())).toString()
            }
            func.contains("exp") -> {
                (exp(func.replace(skivvy.textPattern, "").toFloat())).toString()
            }
            else -> "Invalid expression"
        }
    }

    private fun isFloat(value: String): Boolean {
        return value.toFloat() - value.toFloat().toInt() != 0F
    }

    fun formatToProperValue(value: String): String {
        return if (isFloat(value)) {
            value.toFloat().toString()
        } else value.toFloat().toInt().toString()
    }
}