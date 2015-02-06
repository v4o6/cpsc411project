package test.parser;

import java.io.File;

import org.junit.Test;

import parser.Parser;
import ast.Program;
import util.SampleCode;


/**
 * The tests in this class correspond more or less to the work in Chapter 3.
 * <p>
 * These tests try to call your parser to parse Expression programs, but they do 
 * not check the AST produced by the parser. As such you should be able to get
 * these tests to pass without inserting any semantic actions into your
 * parser specification file.
 * <p>
 * The tests in this file are written in an order that can be followed if
 * you want to develop your parser incrementally, staring with the tests at the
 * top of the file, and working your way down.
 * 
 * @author kdvolder
 */
public class Test3Parse {
	
	/**
	 * All testing is supposed to go through calling this method to one of the
	 * accept methods, to see whether some input is accepted by the parser. The subclass 
	 * Test4Parse refines these tests by overriding this accept method to also verify the 
	 * parse tree structure.
	 */
	protected void accept(String input) throws Exception {
		System.out.println("parsing string: "+ input);
		Program p = Parser.parse(input);
		System.out.println("Parse tree:");
		System.out.println(p.dump());
	}
	
	protected void accept(File file) throws Exception {
		System.out.println("parsing file: "+file);
		Program p = Parser.parse(file);
		System.out.println("Parse tree:");
		System.out.println(p.dump());
	}
	
	//////////////////////////////////////////////////////////////////////////
	// First let's ensure we can parse the "simplest possible" program:

	void acceptExpression(String exp) throws Exception {
		accept("class Test {\n" +
					"\tpublic static void main(String[] a) {\n" +
						"\t\tSystem.out.println(" + exp + ");\n\t}\n}");
	}
	
	@Test
	public void testSmallest() throws Exception {
		// The smallest program has one print statement with the smallest expression.
		acceptExpression("1");
	}
	
	//////////////////////////////////////////////////////////////////////////
	// Next: let's work on making the expression parsing complete.
	
	// Note: some of the created test programs here are not correct. They use undeclared variables
	// and may have type errors... but they should pass by the parser. The parser doesn't
	// check for these kinds of errors.
		
	@Test
	public void testIdentifier() throws Exception {
		acceptExpression("x");
		acceptExpression("y");
		acceptExpression("xy123");
		acceptExpression("x_y_123");
		acceptExpression("x_y_123");
	}
	
	@Test
	public void testThis() throws Exception {
		acceptExpression("this");
	}
	
	@Test
	public void testNot() throws Exception {
		acceptExpression("!x");
		acceptExpression("!!!!!!x");
	}

	@Test
	public void testParens() throws Exception {
		acceptExpression("(1)");
		acceptExpression("((((((1))))))");
	}

	@Test
	public void testMult() throws Exception {
		acceptExpression("10*9");
		acceptExpression("10*9*8");
		acceptExpression("foo*len");
		acceptExpression("10*9*8*7*x*y*foo");
	}
	
	@Test
	public void testAdd() throws Exception {
		acceptExpression("10+9");
		acceptExpression("10-9");
		acceptExpression("10+9+8");
		acceptExpression("10-9-8");
		acceptExpression("len+len");
		acceptExpression("len-len");
		acceptExpression("foo+foo");
		acceptExpression("foo+(foo)");
		acceptExpression("10+9+x*len-foo+array");
		acceptExpression("(a-b)*(a+b)");
	}

	@Test
	public void testComp() throws Exception {
		acceptExpression("10<9");
		acceptExpression("10+a*3<9-4+2");
		acceptExpression("len<1");
		acceptExpression("i<foo");
		acceptExpression("10<9");
		acceptExpression("10+a*3<9-4+2");
		acceptExpression("len<1");
		acceptExpression("i<foo");
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	// Now let's work on making statement parsing complete.
	
	void acceptStatement(String statement) throws Exception {
		accept("class Test {\n" +
					"\tpublic static void main(String[] a) {\n" +
						"\t\tSystem.out.println(new Test1().func(0));\n" +
					"\t}\n" +
				"}\n" +
				"class Test1 {\n" +
					"\tint a;\n" +
					"\tbool b;\n" +
					"\tpublic int func(int c) {\n" +
						"\t\t" + statement + "\n" +
						"\t\treturn c;\n" +
					"\t}\n" +
				"}");
	}
	
	@Test public void testAssign() throws Exception {
		acceptStatement("a = a + 1;");
		acceptStatement("a = a+1;");
		acceptStatement("a = 1;");
		acceptStatement("b = true;");
	}
	
	////////////////////////////////////////////////////////////////////////////////
	// Additional tests for features in the Functions language
	@Test public void goodFunctionDeclaration() throws Exception {
		accept(progWithFunction("int x() { return 1; }", "x()"));
		accept(progWithFunction("boolean x() { a = 1; return a < 2; }", "x()"));
		accept(progWithFunction("int x(int x) { int c; c = 1; return x+c; }", "x(1)"));
		accept(progWithFunction("boolean x(int x) { return !(x < 1); }", "x(1)"));
		accept(progWithFunction("int x(int x, int y) { return x + y; }", "x(2, 3)"));
		accept(progWithFunction("boolean x(int x, int y) { return true && x < y; }", "x(2, 3)"));
	}

	/**
	 * Generate a test program with a function declaration in it.
	 */
	private String progWithFunction(String func, String call) {
		return	"class Test {\n" +
					"\tpublic static void main(String[] a) {\n" +
						"\t\tSystem.out.println(new Test2()." + call + ");\n" +
					"\t}\n" +
				"}\n" +
				"class Test2 {\n" +
					"\tint a;\n" +
					"\tbool b;\n" +
					"\tpublic " + func + "\n" +
				"}";
	}

	/////////////////////////////////////////////////////////////////////////////////
	// Finally, check whether the parser accepts all the sample code.
	@Test 
	public void testParseSampleCode() throws Exception {
		File[] files = SampleCode.sampleFiles("java");
		for (File file : files) {
			accept(file);
		}
	}	
}
