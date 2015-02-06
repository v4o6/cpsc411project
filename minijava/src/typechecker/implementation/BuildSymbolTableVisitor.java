package typechecker.implementation;

import ast.AST;
import ast.And;
import ast.ArrayAssign;
import ast.ArrayLength;
import ast.ArrayLookup;
import ast.Assign;
import ast.Block;
import ast.BooleanLiteral;
import ast.BooleanType;
import ast.Call;
import ast.ClassDecl;
import ast.Identifier;
import ast.If;
import ast.IntArrayType;
import ast.IntegerLiteral;
import ast.IntegerType;
import ast.LessThan;
import ast.MainClass;
import ast.MethodDecl;
import ast.Minus;
import ast.NewArray;
import ast.NewObject;
import ast.NodeList;
import ast.Not;
import ast.ObjectType;
import ast.Plus;
import ast.Print;
import ast.Program;
import ast.This;
import ast.Times;
import ast.VarDecl;
import ast.While;
import typechecker.ErrorReport;
import util.ImpTable;
import util.ImpTable.DuplicateException;
import visitor.Visitor;

/**
 * This visitor implements Phase 1 of the TypeChecker. It constructs the symbol table.
 * 
 * @author norm
 */
public class BuildSymbolTableVisitor implements Visitor<ImpTable<ClassInfo>> {
	
	private final ImpTable<ClassInfo> table;
	private final ErrorReport errors;
	private ClassInfo currentClass;
	private MethodInfo currentMethod;
	
	public BuildSymbolTableVisitor(ImpTable<ClassInfo> table, ErrorReport errors) {
		this.table = table;
		this.errors = errors;
	}

	/////////////////// Phase 1 ///////////////////////////////////////////////////////

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
		currentClass = table.lookup(n.name);

		currentMethod = new MethodInfo(currentClass.classDecl.name, null);
		def(currentMethod.symbols, "this", new ObjectType(currentClass.classDecl.name, currentClass));
		def(currentClass.methods, "main", currentMethod);

		return null;
	}

	@Override
	public ImpTable<ClassInfo> visit(ClassDecl n) {
		currentClass = table.lookup(n.name);
		if (n.superName != null) {
			if (table.lookup(n.superName) == null)
				errors.undefinedClass(n.superName);
			else {
				currentClass.superClass = table.lookup(n.superName);
				inheritFields(currentClass.superClass);
			}
		}
		n.vars.accept(this);
		n.methods.accept(this);

		return null;
	}

	private void inheritFields(ClassInfo superClass) {
		ClassDecl superDecl = superClass.classDecl;
		if (superDecl.superName != null)
			inheritFields(superClass.superClass);
		superDecl.vars.accept(this);
	}
	
	@Override
	public ImpTable<ClassInfo> visit(VarDecl n) {
		switch (n.kind) {
			case FIELD:
				def(currentClass.fields, n.name, n.type);
				break;
			case FORMAL:
				currentMethod.formals.add(n.type);
				def(currentMethod.symbols, n.name, n.type);
				break;
			case LOCAL:
				def(currentMethod.symbols, n.name, n.type);
				break;
		}
		return null;
	}

	@Override
	public ImpTable<ClassInfo> visit(MethodDecl n) {
		currentMethod = new MethodInfo(currentClass.classDecl.name, n.returnType);
		def(currentMethod.symbols, "this", new ObjectType(currentClass.classDecl.name, currentClass));
		n.formals.accept(this);
		n.vars.accept(this);
		def(currentClass.methods, n.name, currentMethod);
		return null;
	}

	@Override
	public ImpTable<ClassInfo> visit(IntArrayType n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(BooleanType n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(IntegerType n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(ObjectType n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(Block n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(If n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(While n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(Print n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(Assign n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(ArrayAssign n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(And n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(LessThan n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(Plus n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(Minus n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(Times n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(ArrayLookup n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(ArrayLength n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(Call n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(IntegerLiteral n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(BooleanLiteral n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(Identifier n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(This n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(NewArray n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(NewObject n) {
		throw new Error("Not implemented");
	}

	@Override
	public ImpTable<ClassInfo> visit(Not n) {
		throw new Error("Not implemented");
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
