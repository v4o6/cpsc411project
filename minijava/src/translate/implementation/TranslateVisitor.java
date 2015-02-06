package translate.implementation;

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
import ast.Statement;
import ast.This;
import ast.Times;
import ast.VarDecl;
import ast.While;
import ir.frame.Frame;
import ir.frame.x86_64.X86_64Frame;
import ir.temp.Label;
import ir.temp.Temp;
import ir.tree.IR;
import ir.tree.IRExp;
import ir.tree.IRStm;
import ir.tree.TEMP;
import ir.tree.BINOP.Op;
import ir.tree.CJUMP.RelOp;
import translate.DataFragment;
import translate.Fragments;
import translate.ProcFragment;
import translate.Translator;
import typechecker.implementation.ClassInfo;
import typechecker.implementation.MethodInfo;
import util.List;
import util.Lookup;
import visitor.Visitor;


/**
 * This visitor builds up a collection of IRTree code fragments for the body
 * of methods in a minijava program.
 * <p>
 * Methods that visit statements and expression return a TRExp, other methods 
 * just return null, but they may add Fragments to the collection by means
 * of a side effect.
 * 
 * @author kdvolder
 */
public class TranslateVisitor implements Visitor<TRExp> {

	/**
	 * We build up a list of Fragment (pieces of stuff to be converted into
	 * assembly) here.
	 */
	private Fragments frags;

	/**
	 * We use this factory to create Frame's, without making our code dependent
	 * on the target architecture.
	 */
	private Frame frameFactory;
	private Lookup<ClassInfo> table;
	private Frame frame;
	private ClassInfo currentClass;
	private OOEnv currentEnv;

	public TranslateVisitor(Lookup<ClassInfo> table, Frame frameFactory) {
		this.frags = new Fragments(frameFactory);
		this.frameFactory = frameFactory;
		this.table = table;
	}

	public Fragments getResult() {
//		System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
//		System.out.println(currentEnv);
//		System.out.println();
		return frags;
	};
	
	/////// Helpers //////////////////////////////////////////////

	/**
	 * Create a frame with a given number of formals.
	 */
	private Frame newFrame(Label name, int formals) {
		return frameFactory.newFrame(name, formals);
	}

	// Retrieve a variable location from the environment
	private IRExp varLookup(IRExp fp, String name) {
		// lookup local variables and parameters
		if (currentEnv.lookupSymbol(fp, name) != null)
			return currentEnv.lookupSymbol(fp, name);

		// lookup fields against the implicit parameter this
		return currentEnv.lookupFields(name, currentEnv.lookupSymbol(fp, "this"));
	}
	
	// Retrieve an expression for an array subscript for a given array and index
	private IRExp arrayLookup(IRExp base, IRExp index) {
		IRExp len = arrayLength(base);
		Label t = Label.gen();
		Label f = Label.gen();
		return IR.ESEQ(IR.SEQ(IR.CJUMP(RelOp.LT, index, len, t, f),
							  IR.LABEL(f),
							  IR.EXP(IR.CALL(Translator.L_ERROR, IR.CONST(1))),
							  IR.LABEL(t)),
					   IR.MEM(IR.PLUS(base, IR.MUL(index, X86_64Frame.WORD_SIZE))));
	}
	
	// Retrieve an expression for an array's length
	private IRExp arrayLength(IRExp array) {
		// Presently no check for null array
		return IR.MEM(IR.MINUS(array, X86_64Frame.WORD_SIZE));
	}
	
	// Generate a null pointer check on the provided pointer
	private IRStm nullPointerCheck(TEMP ptr) {
		Label t = Label.gen();
		Label f = Label.gen();
		return IR.SEQ(IR.CJUMP(RelOp.EQ, ptr, IR.NULL, t, f),
					  IR.LABEL(t),
					  IR.EXP(IR.CALL(Translator.L_ERROR, IR.CONST(2))),
					  IR.LABEL(f));
	}
	
	////// Visitor ///////////////////////////////////////////////

	@Override
	public <T extends AST> TRExp visit(NodeList<T> ns) {
		for (int i = 0; i < ns.size(); i++) {
			ns.elementAt(i).accept(this);
		}
		return null;
	}
	
	@Override
	public TRExp visit(Program n) {
		currentEnv = OOEnv.theEmpty();

		frame = newFrame(Label.get("dummy"), 0);
		// VMTs must be built before translating method bodies so that methods can refer to instances of other classes
// Current implementation ignores Main Classes as mere wrappers for main methods, which is not very robust.
//		List<DataFragment> vmts = List.list(buildTable(n.mainClass.name));
		List<DataFragment> vmts = List.empty();
		for (int i = 0; i < n.classes.size(); i++) {
			vmts.add(buildTable(n.classes.elementAt(i).name));
		}
		n.mainClass.accept(this);
		n.classes.accept(this);
		// For the interpreter; VMTs must come at the end of the fragment list so that the NAME labelled method entries refer to existing procedures
		for (DataFragment vtable : vmts)
			frags.add(vtable);

		return null;
	}

	private DataFragment buildTable(String name) {
		currentClass = table.lookup(name);
		List<IRExp> classTable = generateVMT(currentClass);
		DataFragment result = currentEnv.insertTable(frame, name, classTable);
		currentEnv.exitClass();
		return result;
	}
	
	private List<IRExp> generateVMT(ClassInfo classDef) {
		List<IRExp> values = List.empty();
		String className = classDef.classDecl.name;

		// The first entry of a VMT points to a class's super class
		if (classDef.superClass != null)
			values.add(IR.NAME(Label.get(classDef.superClass.classDecl.name)));
		else
			values.add(IR.NULL);

		// Insert references to each method, including inherited ones.
		for (Entry<String, MethodInfo> method : classDef.methods) {
			values.add(IR.NAME(Label.get(method.getValue().methodClass + "." + method.getKey())));
			currentEnv.insertMethod(className + "." + method.getKey());
		}			
		
		return values;
	}
	
	@Override
	public TRExp visit(MainClass n) {
		currentClass = table.lookup(n.name);

		// first field is a pointer to the class's vtable
		currentEnv.insertField(n.name);
		// Process main statement as a procedure
		frame = newFrame(Translator.L_MAIN, 0);
//		currentEnv.insertFormal(frame, "this");
		TRExp statement = n.statement.accept(this);
		IRStm body = frame.procEntryExit1(statement.unNx());
		frags.add(new ProcFragment(frame, body));
		currentEnv.exitMethod();

		currentEnv.exitClass();
		return null;
	}

	@Override
	public TRExp visit(ClassDecl n) {
		currentClass = table.lookup(n.name);

		// first field is a pointer to the class's vtable
		currentEnv.insertField(n.name);
		if (n.superName != null) 
			inheritFields(currentClass.superClass);
		n.vars.accept(this);
		n.methods.accept(this);

		currentEnv.exitClass();
		return null;
	}

	private void inheritFields(ClassInfo superClass) {
		ClassDecl superDecl = superClass.classDecl;
		if (superDecl.superName != null)
			inheritFields(superClass.superClass);
		superDecl.vars.accept(this);
	}
	
	@Override
	public TRExp visit(VarDecl n) {
		switch (n.kind) {
		case FIELD:
			currentEnv.insertField(n.name);
			break;
		case FORMAL:
			currentEnv.insertFormal(frame, n.name);
			break;
		case LOCAL:
			currentEnv.insertLocal(frame, n.name, false);
			break;
		}
		return null;
	}

	@Override
	public TRExp visit(MethodDecl n) {
		Label label = Label.get(currentClass.classDecl.name + "." + n.name);
		frame = newFrame(label, n.formals.size() + 1);
		currentEnv.insertFormal(frame, "this");
		n.formals.accept(this);
		n.vars.accept(this);
		
		TRExp statements = getStatements(n.statements);
		TRExp returnVal = n.returnExp.accept(this);
		IRStm body = IR.SEQ(
				statements.unNx(),
				IR.MOVE(frame.RV(), returnVal.unEx())
		);
		body = frame.procEntryExit1(body);
		frags.add(new ProcFragment(frame, body));
		
		currentEnv.exitMethod();
		return null;
	}
	
	public TRExp getStatements(NodeList<Statement> ns) {
		IRStm result = IR.NOP;
		for (int i = 0; i < ns.size(); i++) {
			AST nextStm = ns.elementAt(i);
			result = IR.SEQ(result, nextStm.accept(this).unNx());
		}
		return new Nx(result);
	}

	@Override
	public TRExp visit(IntArrayType n) {
		throw new Error("Not implemented");
	}

	@Override
	public TRExp visit(BooleanType n) {
		throw new Error("Not implemented");
	}

	@Override
	public TRExp visit(IntegerType n) {
		throw new Error("Not implemented");
	}

	@Override
	public TRExp visit(ObjectType n) {
		throw new Error("Not implemented");
	}

	@Override
	public TRExp visit(Block n) {
		return getStatements(n.statements);
	}

	@Override
	public TRExp visit(If n) {
		Label thn = Label.gen();
		Label els = Label.gen();
		Label end = Label.gen();
		TRExp tst = n.tst.accept(this);
		IRStm cond = IR.SEQ(tst.unCx(thn, els),
							IR.LABEL(thn),
							n.thn.accept(this).unNx(),
							IR.JUMP(end),
							IR.LABEL(els),
							n.els.accept(this).unNx(),
							IR.LABEL(end));
		return new Nx(cond);
	}

	@Override
	public TRExp visit(While n) {
		Label tst = Label.gen();
		Label bdy = Label.gen();
		Label end = Label.gen();
		IRStm loop = IR.SEQ(IR.LABEL(tst),
							IR.CJUMP(RelOp.NE, IR.FALSE, n.tst.accept(this).unEx(), bdy, end),
							IR.LABEL(bdy),
							n.body.accept(this).unNx(),
							IR.JUMP(tst),
							IR.LABEL(end));
		return new Nx(loop);
	}

	@Override
	public TRExp visit(Print n) {
		TRExp arg = n.exp.accept(this);
		return new Ex(IR.CALL(Translator.L_PRINT,
							  arg.unEx()));
	}

	@Override
	public TRExp visit(Assign n) {
		IRExp lValue = varLookup(frame.FP(), n.name.name);
		TRExp rValue = n.value.accept(this);
		return new Nx(IR.MOVE(lValue, rValue.unEx()));
	}

	@Override
	public TRExp visit(ArrayAssign n) {
		IRExp lValue = varLookup(frame.FP(), n.name.name);
		TRExp index = n.index.accept(this);
		TRExp rValue = n.value.accept(this);
		return new Nx(IR.MOVE(arrayLookup(lValue, index.unEx()), rValue.unEx()));
	}

	@Override
	public TRExp visit(And n) {
		TRExp left = n.e1.accept(this);
		TRExp right = n.e2.accept(this);
		Label t = Label.gen();
		Label f = Label.gen();
		TEMP r = IR.TEMP(new Temp());
		return new Ex(IR.ESEQ(IR.SEQ(IR.MOVE(r, IR.FALSE),
									 left.unCx(t, f),
									 IR.LABEL(t),
									 right.unCx(r, IR.TRUE),
									 IR.LABEL(f)),
							   r));
	}

	@Override
	public TRExp visit(LessThan n) {
		TRExp left = n.e1.accept(this);
		TRExp right = n.e2.accept(this);
		return new RelCx(RelOp.LT, left.unEx(), right.unEx());
	}

	@Override
	public TRExp visit(Plus n) {
		TRExp left = n.e1.accept(this);
		TRExp right = n.e2.accept(this);
		return new Ex(IR.PLUS(left.unEx(), right.unEx()));
	}

	@Override
	public TRExp visit(Minus n) {
		TRExp left = n.e1.accept(this);
		TRExp right = n.e2.accept(this);
		return new Ex(IR.MINUS(left.unEx(), right.unEx()));
	}

	@Override
	public TRExp visit(Times n) {
		TRExp left = n.e1.accept(this);
		TRExp right = n.e2.accept(this);
		return new Ex(IR.MUL(left.unEx(), right.unEx()));
	}

	@Override
	public TRExp visit(ArrayLookup n) {
		TRExp base = n.array.accept(this);
		TRExp index = n.index.accept(this);
		return new Ex(arrayLookup(base.unEx(), index.unEx()));
	}

	@Override
	public TRExp visit(ArrayLength n) {
		TRExp array = n.array.accept(this);
		return new Ex(arrayLength(array.unEx()));
	}

	@Override
	public TRExp visit(Call n) {
		TRExp obj = n.receiver.accept(this);
		TEMP r = IR.TEMP(new Temp());

		List<IRExp> args = List.list((IRExp) r);
		for (int i = 0; i < n.rands.size(); i++) {
			TRExp arg = n.rands.elementAt(i).accept(this);
			args.add(arg.unEx());
		}		
		
		String objclass = ((ObjectType) n.receiver.getType()).name;
		IRExp offset = currentEnv.lookupMethods(objclass + "." + n.name);
		return new Ex(IR.ESEQ(IR.SEQ(IR.MOVE(r, obj.unEx()),
									 nullPointerCheck(r)),
							  IR.CALL(IR.MEM(IR.PLUS(IR.MEM(r), offset)), args)));
	}

	@Override
	public TRExp visit(IntegerLiteral n) {
		return new Ex(IR.CONST(n.value));
	}

	@Override
	public TRExp visit(BooleanLiteral n) {
		if (n.value)
			return new Ex(IR.TRUE);
		return new Ex(IR.FALSE);
	}

	@Override
	public TRExp visit(Identifier n) {
		return new Ex(varLookup(frame.FP(), n.name));
	}

	@Override
	public TRExp visit(This n) {
		return new Ex(varLookup(frame.FP(), "this"));
	}

	@Override
	public TRExp visit(NewArray n) {
		TRExp size = n.size.accept(this);
		return new Ex(IR.CALL(Translator.L_NEW_ARRAY, size.unEx()));
	}

	@Override
	public TRExp visit(NewObject n) {
		ClassInfo objectClass = table.lookup(n.typeName);
		TEMP r = IR.TEMP(new Temp());
		IRStm alloc = IR.MOVE(r,
							  IR.CALL(Translator.L_NEW_OBJECT,
									  IR.CONST((objectClass.fields.size() + 1) * X86_64Frame.WORD_SIZE)));
		return new Ex(IR.ESEQ(IR.SEQ(alloc,
									 IR.MOVE(IR.MEM(r),
											 currentEnv.lookupTables(n.typeName))),
							  r));
	}

	@Override
	public TRExp visit(Not n) {
		TRExp e = n.e.accept(this);
		return new Ex(IR.BINOP(Op.XOR, IR.TRUE, e.unEx()));
	}

}
