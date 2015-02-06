package typechecker.implementation;

import ast.Program;
import typechecker.ErrorReport;
import typechecker.TypeChecked;
import typechecker.TypeCheckerException;
import util.ImpTable;


public class TypeCheckerImplementation extends TypeChecked {
	
	/**
	 * The AST of the program we are type checking.
	 */
	private Program program;

	/**
	 * The place to which error messages get sent.
	 */
	private ErrorReport errors = new ErrorReport();

	/**
	 * The symbol table computed by phase 1:
	 */
	private ImpTable<ClassInfo> classes;

	public TypeCheckerImplementation(Program program) {
		this.program = program;
	}

	public TypeChecked typeCheck() throws TypeCheckerException {
		//Phase 1:
		classes = buildTable();
		//Phase 2:
		program.accept(new TypeCheckVisitor(classes, errors));
		//Throw an exception if there were errors:
		errors.close();
		// If there was no exception:
		return this;
	}

	/**
	 * This is really an internal helper method, which should not be public.
	 * It has only been made public to allow us to test Phase 1 of the typechecker
	 * in isolation. In normal operation (not unit testing) this method should 
	 * not be called by code outside the type checker.
	 * @throws TypeCheckerException 
	 */
	public ImpTable<ClassInfo> buildTable() throws TypeCheckerException {
		classes = program.accept(new InitSymbolTableVisitor(errors));
		errors.close();
		classes = program.accept(new BuildSymbolTableVisitor(classes, errors));
		errors.close();
		return classes;
	}

	public ImpTable<ClassInfo> typeCheckPhaseTwo() throws TypeCheckerException {
		program.accept(new TypeCheckVisitor(classes, errors));
		errors.close();
		return classes;
	}
	public Program getProgram() {
		return program;
	}

	public ImpTable<ClassInfo> getTable() {
		return classes;
	}

}
