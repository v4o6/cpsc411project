package typechecker.implementation;


import java.util.ArrayList;
import java.util.List;

import ast.AST;
import ast.Assign;
import ast.BooleanType;
import ast.Conditional;
import ast.Expression;
import ast.IdentifierExp;
import ast.IntegerLiteral;
import ast.IntegerType;
import ast.LessThan;
import ast.Minus;
import ast.NodeList;
import ast.Not;
import ast.Plus;
import ast.Print;
import ast.Program;
import ast.Times;
import ast.Type;
import ast.UnknownType;

import typechecker.ErrorReport;
import util.ImpTable;
import visitor.Visitor;

/**
 * This class implements Phase 2 of the Type Checker. This phase
 * assumes that we have already constructed the program's symbol table in
 * Phase1.
 * <p>
 * Phase 2 checks for the use of undefined identifiers and type errors.
 * <p>
 * Visitors may return a Type as a result. Generally, only visiting 
 * an expression or a type actually returns a type.
 * <p>
 * Visiting other nodes just returns null.
 * 
 * @author kdvolder
 */
public class TypeCheckVisitor implements Visitor<Type> {

	/**
	 * The place to send error messages to.
	 */
	private ErrorReport errors;

	/**
	 * The symbol table from Phase 1. 
	 */
	private ImpTable<Type> variables;


	public TypeCheckVisitor(ImpTable<Type> variables, ErrorReport errors) {
		this.variables = variables;
		this.errors = errors;
	}

	//// Helpers /////////////////////

	/**
	 * Check whether the type of a particular expression is as expected.
	 */
	private void check(Expression exp, Type expected) {
		Type actual = exp.accept(this);
		if (!assignableFrom(expected, actual))
			errors.typeError(exp, expected, actual);
	}

	/**
	 * Check whether two types in an expression are the same
	 */
	private void check(Expression exp, Type t1, Type t2) {
		if (!t1.equals(t2))
			errors.typeError(exp, t1, t2);
	}	

	private boolean assignableFrom(Type varType, Type valueType) {
		return varType.equals(valueType); 
	}

	///////// Visitor implementation //////////////////////////////////////

	@Override
	public <T extends AST> Type visit(NodeList<T> ns) {
		for (int i = 0; i < ns.size(); i++) {
			ns.elementAt(i).accept(this);
		}
		return null;
	}

	@Override
	public Type visit(Program n) {
		//		variables = applyInheritance(variables);
		n.statements.accept(this);
		n.print.accept(this);
		return null;
	}

	@Override
	public Type visit(BooleanType n) {
		return n;
	}

	@Override
	public Type visit(IntegerType n) {
		return n;
	}

	@Override
	public Type visit(UnknownType n) {
		return n;
	}

	/**
	 * Can't use check, because print allows either Integer or Boolean types
	 */
	@Override
	public Type visit(Print n) {
		Type actual = n.exp.accept(this);
		if (!assignableFrom(new IntegerType(), actual) && !assignableFrom(new BooleanType(), actual)) {
			List<Type> l = new ArrayList<Type>();
			l.add(new IntegerType());
			l.add(new BooleanType());
			errors.typeError(n.exp, l, actual);
		}
		return null;
	}

	@Override
	public Type visit(Assign n) {
		Type expressionType = n.value.accept(this);
		variables.set(n.name, expressionType);
		return null; 
	}

	@Override
	public Type visit(Conditional n) {
		check(n.e1, new BooleanType());
		Type t2 = n.e2.accept(this);
		Type t3 = n.e3.accept(this);
		check(n.e3, t2, t3);
		return t2;
	}

	@Override
	public Type visit(LessThan n) {
		check(n.e1, new IntegerType());
		check(n.e2, new IntegerType());
		n.setType(new BooleanType());
		return n.getType();
	}

	@Override
	public Type visit(Plus n) {
		check(n.e1, new IntegerType());
		check(n.e2, new IntegerType());
		n.setType(new IntegerType());
		return n.getType();
	}

	@Override
	public Type visit(Minus n) {
		check(n.e1, new IntegerType());
		check(n.e2, new IntegerType());
		n.setType(new IntegerType());
		return n.getType();
	}

	@Override
	public Type visit(Times n) {
		check(n.e1, new IntegerType());
		check(n.e2, new IntegerType());
		n.setType(new IntegerType());
		return n.getType();
	}

	@Override
	public Type visit(IntegerLiteral n) {
		n.setType(new IntegerType());
		return n.getType();
	}

	@Override
	public Type visit(IdentifierExp n) {
		Type type = variables.lookup(n.name);
		if (type == null) 
			type = new UnknownType();
		return type;
	}

	@Override
	public Type visit(Not n) {
		check(n.e, new BooleanType());
		n.setType(new BooleanType());
		return n.getType(); 
	}

}
