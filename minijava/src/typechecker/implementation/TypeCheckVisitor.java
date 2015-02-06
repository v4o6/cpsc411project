package typechecker.implementation;


import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
import ast.Expression;
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
import ast.Type;
import ast.VarDecl;
import ast.While;
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
	private ImpTable<ClassInfo> classes;
	private ClassInfo currentClass;
	private MethodInfo currentMethod;

	public TypeCheckVisitor(ImpTable<ClassInfo> classes, ErrorReport errors) {
		this.classes = classes;
		this.errors = errors;
	}

	///////// Visitor implementation //////////////////////////////////////

	@Override
	public <T extends AST> Type visit(NodeList<T> ns) {
		for (int i = 0; i < ns.size(); i++)
			ns.elementAt(i).accept(this);
		return null;
	}

	@Override
	public Type visit(Program n) {
		n.mainClass.accept(this);
		n.classes.accept(this);
		return null;
	}

	@Override
	public Type visit(MainClass n) {
		currentClass = classes.lookup(n.name);
		currentMethod = currentClass.methods.lookup("main");
		n.statement.accept(this);
		return null;
	}

	@Override
	public Type visit(ClassDecl n) {
		currentClass = classes.lookup(n.name);
		if (currentClass.superClass != null) {
			ImpTable<MethodInfo> completeMethods = new ImpTable<MethodInfo>();
			inheritMethods(currentClass, completeMethods);
			currentClass.methods = completeMethods;
		}		
		// typecheck method bodies
		n.methods.accept(this);

		return null;
	}

	private void inheritMethods(ClassInfo classinfo, ImpTable<MethodInfo> methodtable) {
		if (classinfo.superClass != null)
			inheritMethods(classinfo.superClass, methodtable);
		for (Entry<String, MethodInfo> method : classinfo.methods) {
			// override parent entries if they exist
			methodtable.set(method.getKey(), method.getValue());
		}
	}
	
	@Override
	public Type visit(VarDecl n) {
		if (n.type instanceof ObjectType) {
			String className = ((ObjectType) n.type).name;
			if (classes.lookup(className) == null)
				errors.undefinedClass(className);
		}
		return null;
	}

	@Override
	public Type visit(MethodDecl n) {
		currentMethod = currentClass.methods.lookup(n.name);
		n.statements.accept(this);
		check(n.returnExp, n.returnType);
		return null;
	}

	@Override
	public Type visit(IntArrayType n) {
		return n;
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
	public Type visit(ObjectType n) {
		if (n.classInfo == null)
			n.classInfo = classes.lookup(n.name);
		return n;
	}

	@Override
	public Type visit(Block n) {
		n.statements.accept(this);
		return null;
	}

	@Override
	public Type visit(If n) {
		check(n.tst, new BooleanType());
		n.thn.accept(this);
		n.els.accept(this);
		return null;
	}

	@Override
	public Type visit(While n) {
		check(n.tst, new BooleanType());
		n.body.accept(this);
		return null;
	}

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
		if (n.name.equals("this"))
			errors.invalidLvalue(n.name);
		Type expected = n.name.accept(this);
		check(n.value, expected);
		return null;
	}

	@Override
	public Type visit(ArrayAssign n) {
		check(n.name, new IntArrayType());
		check(n.index, new IntegerType());
		check(n.value, new IntegerType());
		return null;
	}

	@Override
	public Type visit(And n) {
		check(n.e1, new BooleanType());
		check(n.e2, new BooleanType());
		n.setType(new BooleanType());
		return n.getType();
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
	public Type visit(ArrayLookup n) {
		check(n.array, new IntArrayType());
		check(n.index, new IntegerType());
		n.setType(new IntegerType());
		return n.getType();
	}

	@Override
	public Type visit(ArrayLength n) {
		check(n.array, new IntArrayType());
		n.setType(new IntegerType());
		return n.getType();
	}

	@Override
	public Type visit(Call n) {
		// verify receiver type
		if (!(n.receiver.accept(this) instanceof ObjectType)) {
			errors.typeError(n, new ObjectType(null), n.receiver.getType());
			return null;
		}
		ObjectType receiverClass = (ObjectType) n.receiver.getType();

		// verify method
		MethodInfo method = classes.lookup(receiverClass.name).methods.lookup(n.name);
		if (method == null) {
			errors.undefinedMethod(receiverClass.name, n.name);
			return null;
		}
		ArrayList<Type> formalTypes = method.formals;

		// verify each argument
		for (int i = 0; i < n.rands.size(); i++) {
			check(n.rands.elementAt(i), formalTypes.get(i));
		}
		
		n.setType(method.returntype.accept(this));
		return n.getType();
	}

	@Override
	public Type visit(IntegerLiteral n) {
		n.setType(new IntegerType());
		return n.getType();
	}

	@Override
	public Type visit(BooleanLiteral n) {
		n.setType(new BooleanType());
		return n.getType();
	}

	@Override
	public Type visit(Identifier n) {
		Type type = lookupVariable(n.name);
		if (type == null) {
			errors.undefinedId(n.name);
			return null;
		}
		n.setType(type.accept(this));
		return n.getType();
	}

	@Override
	public Type visit(This n) {
		n.setType(new ObjectType(currentClass.classDecl.name, currentClass));
		return n.getType();
	}

	@Override
	public Type visit(NewArray n) {
		check(n.size, new IntegerType());
		n.setType(new IntArrayType());
		return n.getType();
	}

	@Override
	public Type visit(NewObject n) {
		// verify object type
		ClassInfo objectClass = classes.lookup(n.typeName);
		if (objectClass == null) {
			errors.undefinedClass(n.typeName);
			return null;
		}

		n.setType(new ObjectType(n.typeName, objectClass));
		return n.getType();
	}

	@Override
	public Type visit(Not n) {
		check(n.e, new BooleanType());
		n.setType(new BooleanType());
		return n.getType();
	}

	///////////////////// Helpers ///////////////////////////////////////////////

	/**
	 * Check whether the type of a particular expression is as expected.
	 */
	private void check(Expression exp, Type expected) {
		Type actual = exp.accept(this);
		if (!assignableFrom(expected, actual))
			errors.typeError(exp, expected, actual);
	}

//	/**
//	 * Check whether two types in an expression are the same
//	 */
//	private void check(Expression exp, Type t1, Type t2) {
//		if (!t1.equals(t2))
//			errors.typeError(exp, t1, t2);
//	}	

	private boolean assignableFrom(Type varType, Type valueType) {
		if (valueType != null && varType.equals(valueType))
			return true;
		else if (!(varType instanceof ObjectType && valueType instanceof ObjectType))
			return false;
		
		String varClass = ((ObjectType) varType).name;
		ClassInfo superClass = ((ObjectType) valueType).classInfo.superClass;
		
		while (superClass != null) {
			if (superClass.classDecl.name.equals(varClass))
				return true;
			superClass = superClass.superClass;
		}
		return false;
	}

	private Type lookupVariable(String id) {
		if (currentMethod.symbols.lookup(id) != null)
			return currentMethod.symbols.lookup(id);
		else if (currentClass.fields.lookup(id) != null)
			return currentClass.fields.lookup(id);
		errors.undefinedId(id);
		return null;
	}
	
}
