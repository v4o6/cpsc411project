package translate.implementation;

import ast.Program;
import translate.Fragments;
import typechecker.TypeChecked;
import typechecker.implementation.ClassInfo;
import util.ImpTable;
import ir.frame.Frame;

public class TranslateImplementation {

	private Frame frameFactory;
	private Program program;
	private ImpTable<ClassInfo> table;

	public TranslateImplementation(Frame frameFactory, TypeChecked typechecked) {
		this.frameFactory = frameFactory;
		this.program = typechecked.getProgram();
		this.table = typechecked.getTable();
	}

	public Fragments translate() {
		TranslateVisitor vis = new TranslateVisitor(table, frameFactory);
		program.accept(vis);
		return vis.getResult();
	}

}
