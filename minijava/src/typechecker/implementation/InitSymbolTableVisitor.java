package typechecker.implementation;

import ast.AST;
import ast.ClassDecl;
import ast.MainClass;
import ast.NodeList;
import ast.Program;
import typechecker.ErrorReport;
import util.ImpTable;
import util.ImpTable.DuplicateException;
import visitor.DefaultVisitor;
import visitor.Visitor;

/**
 * Performs an initial scan over the AST to identify all the classes
 */
public class InitSymbolTableVisitor extends DefaultVisitor<ImpTable<ClassInfo>> implements
		Visitor<ImpTable<ClassInfo>> {
	
	private final ImpTable<ClassInfo> table = new ImpTable<ClassInfo>();
	private final ErrorReport errors;

	public InitSymbolTableVisitor(ErrorReport errors) {
		this.errors = errors;
	}

	@Override
	public <T extends AST> ImpTable<ClassInfo> visit(NodeList<T> ns) {
		for (int i = 0; i < ns.size(); i++)
			ns.elementAt(i).accept(this);
		return null;
	}

	@Override
	public ImpTable<ClassInfo> visit(Program n) {
		n.mainClass.accept(this);
		n.classes.accept(this);
		return table;
	}

	@Override
	public ImpTable<ClassInfo> visit(MainClass n) {
		def(table, n.name, new ClassInfo(n));
		return null;
	}

	@Override
	public ImpTable<ClassInfo> visit(ClassDecl n) {
		def(table, n.name, new ClassInfo(n));
		return null;
	}


	///////////////////// Helpers ///////////////////////////////////////////////
	/**
	 * Add an entry to a table, and check whether the name already existed.
	 * If the name already existed before, the new definition is ignored and
	 * an error is sent to the error report.
	 */
	private <V> void def(ImpTable<V> tab, String name, V value) {
		try {
			tab.put(name, value);
		} catch (DuplicateException e) {
			errors.duplicateDefinition(name);
		}
	}
	
}
