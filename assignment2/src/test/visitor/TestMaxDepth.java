package test.visitor;

import java.io.File;

import org.junit.Test;

import parser.Parser;
import ast.Program;
import test.SampleCode;


public class TestMaxDepth {

	protected void accept(String input) throws Exception {
		System.out.println("parsing string: " + input);
		Program p = Parser.parse(input);
		System.out.println("Max Depth: " + p.maxDepth());
	}
	
	protected void accept(File file) throws Exception {
		System.out.println("parsing file: " + file);
		Program p = Parser.parse(file);
		System.out.println("Max Depth: " + p.maxDepth());
	}


	@Test
	public void testSmallest() throws Exception {
		// The smallest program has one print statement with the smallest expression.
		accept( "print 1");
	}

	
	void acceptExpression(String exp) throws Exception {
		accept( "print " + exp);
	}
	
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
		acceptExpression("foo*length");
		acceptExpression("10*9*8*7*x*y*foo");
	}
	
	@Test
	public void testAdd() throws Exception {
		acceptExpression("10+9");
		acceptExpression("10-9");
		acceptExpression("10+9+8");
		acceptExpression("10-9-8");
		acceptExpression("length+length");
		acceptExpression("length-length");
		acceptExpression("foo+foo");
		acceptExpression("foo+(foo)");
		acceptExpression("10+9+x*length-foo+array");
		acceptExpression("(a-b)*(a+b)");
	}

	@Test
	public void testComp() throws Exception {
		acceptExpression("10<9");
		acceptExpression("10+a*3<9-4+2");
		acceptExpression("length<1");
		acceptExpression("i<foo");
		acceptExpression("10<9");
		acceptExpression("10+a*3<9-4+2");
		acceptExpression("length<1");
		acceptExpression("i<foo");
	}
	
	@Test
	public void testConditional() throws Exception {
		acceptExpression("10<9?x:y");
		acceptExpression("10+a*3<9-4+2 ? 3 + 4 : 5 * 7");
		acceptExpression("1 ? 2 ? 3 ? 4 : 5 : 6 : 7");
		acceptExpression("1 ? 2 ? 3 : 4 ? 5 : 6 : 7 ? 8 : 9");
	}

	
	void acceptStatement(String statement) throws Exception {
		accept( statement + "\n" + "print 1");
	}
	
	@Test public void testAssign() throws Exception {
		acceptStatement("numbers = numbers + 1;");
		acceptStatement("foo = foo+1;");
	}
	

	@Test 
	public void testParseSampleCode() throws Exception {
		File[] files = SampleCode.sampleFiles();
		for (File file : files) {
			accept(file);
		}
	}
	
}
