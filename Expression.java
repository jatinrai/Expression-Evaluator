package app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import structures.Stack;

public class Expression {

	public static String delims = " \t*+-/()[]";
			
    /**
     * Populates the vars list with simple variables, and arrays lists with arrays
     * in the expression. For every variable (simple or array), a SINGLE instance is created 
     * and stored, even if it appears more than once in the expression.
     * At this time, values for all variables and all array items are set to
     * zero - they will be loaded from a file in the loadVariableValues method.
     * 
     
     */
    public static void 
    makeVariableLists(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
    	StringTokenizer tokenizer = new StringTokenizer(expr.replaceAll("[ \\t]", ""), delims);
        int lastIndex = -1;
        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            lastIndex = expr.indexOf(token, lastIndex + 1);
            int checkIndex = lastIndex + token.length();
            if (checkIndex < expr.length() && expr.charAt(checkIndex) == '[') {
                arrays.add(new Array(token));
            } else {
                Variable var = new Variable(token);
                try {
                    var.value = Integer.parseInt(token);
                } catch (NumberFormatException ignored) {
                }
                vars.add(var);
            }
        }
    }
    
    /**
     * Loads values for variables and arrays in the expression
     * 
     
     */
    public static void 
    loadVariableValues(Scanner sc, ArrayList<Variable> vars, ArrayList<Array> arrays) 
    throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String tok = st.nextToken();
            Variable var = new Variable(tok);
            Array arr = new Array(tok);
            int vari = vars.indexOf(var);
            int arri = arrays.indexOf(arr);
            if (vari == -1 && arri == -1) {
            	continue;
            }
            int num = Integer.parseInt(st.nextToken());
            if (numTokens == 2) { // scalar symbol
                vars.get(vari).value = num;
            } else { // array symbol
            	arr = arrays.get(arri);
            	arr.values = new int[num];
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)");
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    arr.values[index] = val;              
                }
            }
        }
    }
    
    /**
     * Evaluates the expression.
     * 
    
     */
    public static float 
    evaluate(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
    	if (expr.contains("[")) {
            return evaluate(normalizeArray(expr, vars, arrays), vars, arrays);
        } else {
            return solveExpression(expr, vars);
        }
    }
    
    private static float solveExpression(String expr, ArrayList<Variable> vars) {

        expr = expr.replaceAll("[ \\t]", "");
        String operators = "()/*+-";

        Stack<Float> operandsStack = new Stack<>();
        Stack<Character> operatorsStack = new Stack<>();

        int len = expr.length();
        int lastIndex = -1;
        for (int i = 0; i < len + 1; i++) {

            if (i == len) {
                if (expr.charAt(i - 1) != ')') {
                    String varName = expr.substring(lastIndex + 1);
                    int vari = vars.indexOf(new Variable(varName));
                    if (vari >= 0) {
                        Variable var = vars.get(vari);
                        operandsStack.push((float) var.value);
                    } else {
                        operandsStack.push(Float.parseFloat(varName));
                    }
                }
                break;
            }

            char kar = expr.charAt(i);
            if (operators.contains(String.valueOf(kar))) {

                String varName = expr.substring(lastIndex + 1, i);
                if (!varName.equals("")) {
                    int vari = vars.indexOf(new Variable(varName));
                    if (vari >= 0) {
                        Variable var = vars.get(vari);
                        operandsStack.push((float) var.value);
                    } else {
                        operandsStack.push(Float.parseFloat(varName));
                    }
                }

                if (kar == '(') {
                    operatorsStack.push(kar);
                } else if (kar == ')') {
                    while (operatorsStack.peek() != '(') {
                        process(operandsStack, operatorsStack);
                    }
                    operatorsStack.pop();
                } else {
                    while (!operatorsStack.isEmpty() &&
                            getPrecedence(operatorsStack.peek()) >= getPrecedence(kar)) {
                        process(operandsStack, operatorsStack);
                    }
                    operatorsStack.push(kar);
                }

                lastIndex = i;
            }
        }

        while (!operatorsStack.isEmpty()) {
            process(operandsStack, operatorsStack);
        }
        return operandsStack.peek();
    }

    private static void process(Stack<Float> operands, Stack<Character> operators) {
        float rhs = operands.pop();
        char operator = operators.pop();
        float lhs = operands.pop();

        float result = getResult(lhs, operator, rhs);
        operands.push(result);
    }

    private static int getPrecedence(char c) {
        switch (c) {
            case '/':
            case '*':
                return 5;
            case '+':
            case '-':
                return 4;
            default:
                return 0;
        }
    }

    private static float getResult(float lhs, char operator, float rhs) {
        switch (operator) {
            case '/':
                return lhs / rhs;
            case '*':
                return lhs * rhs;
            case '+':
                return lhs + rhs;
            case '-':
                return lhs - rhs;
            default:
                return 0;
        }
    }

    private static String normalizeArray(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
        expr = expr.replaceAll("[ \\t]", "");
        int length = expr.length();

        int lastOperatorIndex = -1;
        for (int i = 0; i < length; i++) {
            char kar = expr.charAt(i);
            if (kar == '[') {
                String name = expr.substring(lastOperatorIndex + 1, i);
                int arri = arrays.indexOf(new Array(name));

                int j = i + 1;
                int openingCount = 1;
                int closingIndex = 0;
                while (openingCount > 0) {
                    char test = expr.charAt(j);
                    if (test == '[') openingCount++;
                    else if (test == ']') openingCount--;
                    if (openingCount == 0) {
                        closingIndex = j;
                        break;
                    }
                    j++;
                }

                String subExpr = expr.substring(i + 1, closingIndex);
                int index = (int) evaluate(subExpr, vars, arrays);
                String value = String.valueOf(arrays.get(arri).values[index]);
                return expr.replace(name + "[" + subExpr + "]", value);
            }
            if (delims.contains(String.valueOf(kar))) {
                lastOperatorIndex = i;
            }
        }
        return expr;
    }
    
}
