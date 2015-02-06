package test.typechecker;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;

import parser.jcc.ParseException;
import ast.BooleanType;
import ast.IntegerType;
import ast.Type;
import util.SampleCode;
import typechecker.ErrorMessage;
import typechecker.TypeCheckerException;
import typechecker.implementation.TypeChecker;
import static parser.Parser.*;

/**
 * The difficulty in writing tests for this unit of work is that we should,
 * if at all possible try to not make the testing code be dependant on the
 * Expression type checker returning specific error messages.
 * <p>
 * To try to still have reasonably specific tests that specify relatively
 * precisely what type of error a specific program ought to raise we will:
 * <ul>
 *   <li>Provide you with a class ErrorReport that you should use to create
 *       error reports.
 *   <li>Tests will only inspect the first error in the report.
 *   <li>Tests will be written to avoid ambiguities into what is the "first"
 *       error as much as possible.
 * </ul>
 * 
 * @author kdvolder
 */
@SuppressWarnings("deprecation")
public class TypeCheckTest {

	//////////////////////////////////////////////////////////////////////////////////////////
	// Preliminary check....

	/**
	 * This test parses and typechecks all the book sample programs. These should
	 * type check without any errors.
	 * <p>
	 * By itself this is not a very good test. E.g. an implementation which does nothing
	 * at all will already pass the test!
	 */
	@Test
	public void testSampleCode() throws Exception {
		File[] sampleFiles = SampleCode.sampleFiles("java");
		for (int i = 0; i < sampleFiles.length; i++) {
			System.out.println("parsing: "+sampleFiles[i]);
			accept(sampleFiles[i]);
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////
	// We'll start with checking if the compiler detects duplicate
	// identifier definitions within the same scope.

	@Test
	public void duplicateId() throws Exception {
		// Duplicate variable name
		expect( ErrorMessage.duplicateDefinition("x"), progWithStatement(
			"int x;\n" +
			"boolean x;"));
		expect( ErrorMessage.duplicateDefinition("x"), progWithStatement(
			"int x;\n" +
			"int x;"));
	}

	@Test
	public void undefinedId() throws Exception {
		// Duplicate variable name
		expect(ErrorMessage.undefinedId("x"), progWithStatement(
			"int y;\n" +
			"y = x;"));
		expect(ErrorMessage.undefinedId("x"), progWithStatement(
			"System.out.println(x);"));
	}

	///////////////////////////////////////////////////////////////////////////////
	// Checking phase 2 

	// Statements
	@Test public void badPrint() throws Exception {
		accept(progWithStatement("System.out.println(1);"));
		accept(progWithStatement("System.out.println(1 < 2);"));
	}

	//
	// Expressions
	//

	@Test public void arithOps() throws Exception {
		String[] ops = { "+", "-", "*" };
		for (int i = 0; i < ops.length; i++) {
			String op = ops[i];
			accept( progWithExp("int", "i" +op+"10"));
			accept( progWithExp("int", "10"+op+"i"));

			expect( typeError("b", new IntegerType(), new BooleanType()),
					progWithExp("int", "b"+op+"10") );
			expect( typeError("b", new IntegerType(), new BooleanType()),
					progWithExp("int", "10"+op+"b") );
		}
	}

	@Test public void lessThan() throws Exception {
		String[] ops = { "<" };
		for (int i = 0; i < ops.length; ++i) {
			String op = ops[i]; 
			accept( progWithExp("boolean", "i" +op+"10"));
			accept( progWithExp("boolean", "10"+op+"i"));
			accept( progWithExp("boolean", "i+10" + op + "2*i"));

			expect( typeError("b", new IntegerType(), new BooleanType()),
					progWithExp("boolean", "b"+op+"10") );
			expect( typeError("b", new IntegerType(), new BooleanType()),
					progWithExp("boolean", "10"+op+"b") );
		}
	}

	@Test public void not() throws Exception {
		accept( progWithExp("boolean", "!(3 < 4)"));

		expect( typeError("i", new BooleanType(), new IntegerType()),
				progWithExp("boolean", "!i") );
	}

	//
	// Functions
	//

	@Test public void badFunctionDeclaration() throws Exception {
		expect( typeError("1 < 2", new IntegerType(), new BooleanType()),
				progWithFunction("x()", "int x() { return 1 < 2; }"));
		expect( typeError("1", new BooleanType(), new IntegerType()),
				progWithFunction("x()", "boolean x() { return 1; }"));
	}
	
	@Test public void goodFunctionDeclaration() throws Exception {
		accept(progWithFunction("x()", "int x() { return 1; }"));
		accept(progWithFunction("x()", "boolean x() { return 1 < 2; }"));
		accept(progWithFunction("x(2)", "int x(int x) { return x+1; }"));
		accept(progWithFunction("x(2)", "boolean x(int x) { return x < 1; }"));
		accept(progWithFunction("x(3, 4)", "int x(int x, int y) { return x + y; }"));
		accept(progWithFunction("x(3, 4)", "boolean x(int x, int y) { return x < y; }"));
	}
	
	///////////////////////// Helpers /////////////////////////////////////////////

	private ErrorMessage typeError(String exp, Type expected, Type actual) throws ParseException {
		return ErrorMessage.typeError(parseExp(exp), expected, actual);
	}

	private void accept(File file) throws TypeCheckerException, Exception {
		TypeChecker.parseAndCheck(file); 
	}

	private void accept(String string) throws TypeCheckerException, Exception {
		TypeChecker.parseAndCheck(string); 
	}

	/**
	 * Mostly what we want to do in this set of unit tests is see whether the checker
	 * produces the right kind of error reports. This is a helper method to do just that.
	 */
	private void expect(ErrorMessage expect, String input) throws Exception {
		try {
			TypeChecker.parseAndCheck(input);
			Assert.fail("A TypeCheckerException should have been raised but was not.");
		}
		catch (TypeCheckerException e) {
			Assert.assertEquals(expect, e.getFirstMessage());
		}
	}

	////////// Various code snippets and templates to be used in our tests //////////////

	/**
	 * Generate a test program with a statement in it.
	 */
	private String progWithStatement(String stm) {
		return
		"class Test {\n" +
			"\tpublic static void main(String[] a) {\n" +
				"\t\tSystem.out.println(new Test1().func(0));\n" +
			"\t}\n" +
		"}\n" +
		"class Test1 {\n" +
			"\tint a;\n" +
			"\tboolean b;\n" +
			"\tpublic int func(int c) {\n" +
				"\t\t" + stm + "\n" +
				"\t\treturn c;\n" +
			"\t}\n" +
		"}";
	}
	
	
	/**
	 * Generate a test program with an expression in it.
	 */
	private String progWithExp(String type, String exp) {
		return 
		"class Test {\n" +
			"\tpublic static void main(String[] a) {\n" +
				"\t\tSystem.out.println(new Test1().func());\n" +
			"\t}\n" +
		"}\n" +
		"class Test1 {\n" +
			"\tint i;\n" +
			"\tboolean b;\n" +
			"\tpublic int func() {\n" +
				"\t\t" + type + " x;\n" +
				"\t\ti = 5;\n" + 
				"\t\tb = 4 < 5;\n" +
				"\t\tx = " + exp + ";\n" +
				"\t\treturn 0;\n" +
			"\t}\n" +
		"}";
	}

	/**
	 * Generate a test program with a function declaration in it.
	 */
	private String progWithFunction(String call, String fnc) {
		return 
				"class Test {\n" +
				"\tpublic static void main(String[] a) {\n" +
					"\t\tSystem.out.println(new Test2()." + call + ");\n" +
				"\t}\n" +
			"}\n" +
			"class Test2 {\n" +
				"\tint a;\n" +
				"\tboolean b;\n" +
				"\tpublic " + fnc + "\n" +
			"}";
	}
	
}
