package test.translate;

import ir.frame.Frame;
import ir.frame.x86_64.X86_64Frame;
import ir.interp.Interp;
import ir.interp.InterpMode;

import java.io.File;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import util.SampleCode;
import translate.Fragments;
import translate.Translator;
import typechecker.TypeCheckerException;
import util.Utils;


/**
 * Test the minijava translation phase that takes a (type-checked) program and turns
 * the bodies of all the methods in the program into IRtrees.
 * <p>
 * This test suite uses the IR interpreter to simulate the execution of the
 * resulting IR. This gives us some confidence that our translation works correctly :-)
 * 
 * @author kdvolder
 */
public class TestTranslate {

	public static final Frame architecture = X86_64Frame.factory;

	/**
	 * To make it easy to run all of these tests with the either 
	 * linearized ir code, basic blocks or trace scheduled code
	 * We determine the simulation mode via this method.
	 * <p>
	 * Simply creating a subclass and overriding this method will create
	 * a test suite that runs all the same tests in a different simulation 
	 * mode.
	 * 
	 * @return
	 */
	protected InterpMode getSimulationMode() {
		// return null;
		return InterpMode.LINEARIZED_IR;
	}

	/**
	 * Print out all the generated IR?
	 * <p>
	 * If false, only the result of simulating the IR execution 
	 * will be printed.
	 */
	protected boolean dumpIR() {
		return true;
	}

	@Test
	public void simpleProgram() throws Exception {
		test("1\n",
			 progWithPrint("1")
		);
	}
	
	@Test 
	public void printNumber() throws Exception {
		test("10\n",
			 progWithPrint("10")
		);
	}
	
	@Test
	public void printSum() throws Exception {
		test("30\n",
			 progWithPrint("10 + 20")
		);
	}
	
	@Test
	public void expressions() throws Exception {
		test("1711\n",
			 progWithExp("int",
					 	 "\t\tint tmp;\n" +
					 	 "\t\tint tmp2;\n" +
					 	 "\t\tx = 14;\n" +
					 	 "\t\ty = 17;\n" +
					 	 "\t\ttmp = y * 99;\n" +
					 	 "\t\ttmp2 = x * 2;\n",
					 	 "tmp + tmp2")
		);
	}
	
	@Test
	public void not() throws Exception {
		test("1\n",
			 progWithExp("boolean",
					 	 "\t\tboolean z;\n" +
					 	 "\t\tx = 10;\n" +
					 	 "\t\ty = 20;\n" +
					 	 "\t\tz = !(y < x);\n",
					 	 "z")
		);
	}
	
	@Test
	public void not2() throws Exception {
		test("1\n",
			 progWithExp("boolean",
					 	 "\t\tboolean flag;\n" +
					 	 "\t\tboolean flag2;\n" +
			 			 "\t\tx = 10;\n" +
					 	 "\t\ty = 20;\n" +
					 	 "\t\tflag = y < x;\n" +
					 	 "\t\tflag2 = !flag;\n",
					 	 "flag2")
		);
	}
	
	@Test
	public void complexBranches() throws Exception {
		//Trying to create a program that has many choices... so many traces.
		//We want to work the trace schedule to a point where it actually 
		//gets driven into some of the rarer cases.
		test( "1\n7\n",
		"class Main {\n" +
		"  public static void main(String[] args) {\n" +
		"      System.out.println(new Test().doit());\n" +
		"  }\n" +
		"}\n" +
		"class Test {\n" +
		"   int count;\n"+
		"   public boolean cond() {\n" +
		"      count = count + 1;\n"+
		"      return true;\n"+
		"   }\n"+
		"   public int doit() {\n" +
		"     boolean a;\n"+
		"     a = this.cond() && this.cond() && this.cond();\n"+
		"     if (a && this.cond() && this.cond() && this.cond() && this.cond())\n" +
		"        System.out.println(1);" +
		"     else\n"+
		"        System.out.println(0);" +
		"     return count;\n" +
		"   }\n"+
		"}");
	}

	@Test
	public void emptyBranches() throws Exception {
		//This test is good to see if the BasicBlocks / TraceScheduler deal well
		//with "empty" basic blocks (they do *not* => inefficient jumps)
		//Challenge problem: inspect the code after TraceScheduling and try to fix
		//the compiler somehow to produce more optimal code.
		test("9999\n",
			 progWithExp("int",
					 	 "\t\tboolean flag;" +
					 	 "\t\tflag = 1 < 2;\n",
					 	 "9999")
		);
	}

	//////////////// Sample code //////////////////////////////////
	@Test
	public void testSampleCode() throws Exception {
		File[] files = SampleCode.sampleFiles("java");
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (!optionalSample(f))
				test(f);
		}
	}
	@Test // @Ignore // Don't run this unless you are implementing inheritance support!
	public void testOptionalSampleCode() throws Exception {
		File[] files = SampleCode.sampleFiles("java");
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (optionalSample(f))
				test(f);
		}
	}
	
	protected Fragments test(File program) throws TypeCheckerException, Exception {
		System.out.println("Translating: "+program);
		String expected = Utils.getExpected(program);
		
		return test(expected, program);
	}	

	protected Fragments test(String expected, File program)
			throws TypeCheckerException, Exception {
		Fragments translated = Translator.translate(architecture, program);
		if (dumpIR()) {
			System.out.println("VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV");
			System.out.println(translated);
			System.out.println();
		}
			
		if (getSimulationMode()!=null) {
			System.out.println("Simulating IR code:");
			Interp interp = new Interp(translated, getSimulationMode());
			String result = interp.run();
			System.out.println(result);
			Assert.assertEquals(expected, result);
		}
		System.out.println("=================================");
		return translated;
	}
	
	private boolean optionalSample(File f) {
		if (f.getName().equals("TreeVisitor.java"))
			return true;
		if (f.getName().equals("Inheritance.java"))
			return true;
		return false;
	}
	
	protected Fragments test(String expected, String program) throws Exception {
		System.out.println("Translating program: ");
		System.out.println(program);
		Fragments translated = Translator.translate(architecture, program);
		if (dumpIR()) {
			System.out.println("VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV");
			System.out.println(translated);
			System.out.println();
		}
		if (getSimulationMode()!=null) {
			System.out.println("Simulating IR code:");
			Interp interp = new Interp(translated, getSimulationMode());
			String result = interp.run();
			System.out.print(result);
			Assert.assertEquals(expected, result);
		}
		System.out.println("=================================");
		return translated;
	}

	
	////////// Various code snippets and templates to be used in our tests //////////////

	private int testnum = 0;
	
	/**
	 * Generate a test program with a print in it.
	 */
	private String progWithPrint(String exp) {
		return
		"class Test" + testnum++ + " {\n" +
			"\tpublic static void main(String[] a) {\n" +
				"\t\tSystem.out.println(" + exp + ");\n" +
			"\t}\n" +
		"}";
	}
	
	/**
	 * Generate a test program with an expression in it.
	 */
	private String progWithExp(String type, String statements, String exp) {
		return 
		"class Test" + testnum++ + " {\n" +
			"\tpublic static void main(String[] a) {\n" +
				"\t\tSystem.out.println(new Test1().func());\n" +
			"\t}\n" +
		"}\n" +
		"class Test1 {\n" +
			"\tint x;\n" +
			"\tint y;\n" +
			"\tpublic int y() { return y; }\n" +
//			"\tpublic int xy() {\n" + statements + "\t\treturn x*y;\n\t}\n" +
//			"\tpublic int setx(int a, int b, int c) { x = a - b * c; return x; }\n" +
			"\tpublic " + type + " func() {\n" +
				statements +
				"\t\treturn " + exp + ";\n" +
			"\t}\n" +
		"}";
	}
	
}
