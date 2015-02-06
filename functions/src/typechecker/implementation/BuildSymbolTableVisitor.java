package typechecker.implementation;

import java.util.ArrayList;

import ast.AST;
import ast.Assign;
import ast.BooleanType;
import ast.Conditional;
import ast.ExpressionList;
import ast.FormalList;
import ast.FunctionCall;
import ast.FunctionDeclaration;
import ast.IdentifierExp;
import ast.IntegerLiteral;
import ast.IntegerType;
import ast.LambdaType;
import ast.LessThan;
import ast.Minus;
import ast.NodeList;
import ast.Not;
import ast.Parameter;
import ast.Plus;
import ast.Print;
import ast.Program;
import ast.Times;
import ast.Type;
import ast.UnknownType;
import typechecker.ErrorReport;
import util.ImpTable;
import util.ImpTable.DuplicateException;
import visitor.Visitor;

/**
 * This visitor implements Phase 1 of the TypeChecker. It constructs the symbol table.
 * 
 * @author norm
 */
public class BuildSymbolTableVisitor implements Visitor<ImpTable<Type>> {
	
	private final ImpTable<Type> variables = new ImpTable<Type>();
	private final ErrorReport errors;
	
	public BuildSymbolTableVisitor(ErrorReport errors) {
		this.errors = errors;
	}

	/////////////////// Phase 1 ///////////////////////////////////////////////////////
	// In our implementation, Phase 1 builds up a single symbol table containing all the
	// identifiers defined in an Expression program. 
	//
	// We also check for duplicate identifier definitions 

	@Override
	public ImpTable<Type> visit(Program n) {
		n.statements.accept(this);
		n.print.accept(this); // process all the "normal" classes.
		return variables;
	}
	
	@Override
	public <T extends AST> ImpTable<Type> visit(NodeList<T> ns) {
		for (int i = 0; i < ns.size(); i++)
			ns.elementAt(i).accept(this);
		return null;
	}

	@Override
	public ImpTable<Type> visit(Assign n) {
		n.value.accept(this);
		def(variables, n.name, new UnknownType());
		return null;
	}
	

	@Override
	public ImpTable<Type> visit(IdentifierExp n) {
		if (variables.lookup(n.name) == null)
			errors.undefinedId(n.name);
		return null;
	}
	
	@Override
	public ImpTable<Type> visit(BooleanType n) {
		return null;
	}

	@Override
	public ImpTable<Type> visit(IntegerType n) {
		return null;
	}

	@Override
	public ImpTable<Type> visit(Print n) {
		n.exp.accept(this);
		return null;
	}

	@Override
	public ImpTable<Type> visit(LessThan n) {
		n.e1.accept(this);
		n.e2.accept(this);
		return null;
	}

	@Override
	public ImpTable<Type> visit(Conditional n) {
		n.e1.accept(this);
		n.e2.accept(this);
		n.e3.accept(this);
		return null;
	}
	
	@Override
	public ImpTable<Type> visit(Plus n) {
		n.e1.accept(this);
		n.e2.accept(this);
		return null;
	}

	@Override
	public ImpTable<Type> visit(Minus n) {
		n.e1.accept(this);
		n.e2.accept(this);
		return null;
	}

	@Override
	public ImpTable<Type> visit(Times n) {
		n.e1.accept(this);
		n.e2.accept(this);
		return null;
	}

	@Override
	public ImpTable<Type> visit(IntegerLiteral n) {
		return null;
	}

	@Override
	public ImpTable<Type> visit(Not not) {
		not.e.accept(this);
		return null;
	}

	@Override
	public ImpTable<Type> visit(UnknownType n) {
		return null;
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// Added for project phase 2

	// The type of a FunctionDeclaration is a LambdaType associated with the return type and argument types of the function. 
	// FunctionDeclaration nodes construct their own symbol tables for internal scope.
	// We instantiate a new BuildSymbolTableVisitor to visit all the child nodes of the FunctionDeclaration
	// in place of the BuildSymbolTableVisitor which was passed to it.
	// The parent symbol table of the new symbol table is the symbol table belonging to the visiting BuildSymbolTableVisitor.
	@Override
	public ImpTable<Type> visit(FunctionDeclaration n) {
		
		ArrayList<Type> argTypes = new ArrayList<Type>();
		for (int i = 0; i < n.params.size(); i++) {
			argTypes.add(n.params.elementAt(i).type);
		}
		LambdaType functionType = new LambdaType(n.type, argTypes);
		def(variables, n.name, functionType);

		BuildSymbolTableVisitor functionVisitor = new BuildSymbolTableVisitor(errors);
//		functionVisitor.variables.setParent(variables);
		
		n.params.accept(functionVisitor);
		n.statements.accept(functionVisitor);
		n.value.accept(functionVisitor);
		n.variables = functionVisitor.variables;
		return null;
	}

	@Override
	public ImpTable<Type> visit(FormalList ns) {
		for (int i = 0; i < ns.size(); i++)
			ns.elementAt(i).accept(this);
		return null;
	}

	// Parameter nodes are only ever visited by the local BuildSymbolTableVisitor of a FunctionDeclaration,
	// not the BuildSymbolTableVisitor of the general Program
	@Override
	public ImpTable<Type> visit(Parameter n) {
		def(variables, n.name, n.type);
		return null;
	}

	@Override
	public ImpTable<Type> visit(FunctionCall n) {
		n.args.accept(this);
		if (variables.lookup(n.identifier) == null)
			errors.undefinedId(n.identifier);
		return null;
	}

	@Override
	public ImpTable<Type> visit(ExpressionList ns) {
		for (int i = 0; i < ns.size(); i++)
			ns.elementAt(i).accept(this);
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

	@Override
	public ImpTable<Type> visit(LambdaType n) {
		return null;
	}

}