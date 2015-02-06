package translate.implementation;

import ir.frame.Access;
import ir.frame.Frame;
import ir.frame.x86_64.X86_64Frame;
import ir.temp.Label;
import ir.tree.IR;
import ir.tree.IRData;
import ir.tree.IRExp;
import translate.DataFragment;
import util.DefaultIndentable;
import util.FunTable;
import util.IndentingWriter;
import util.List;

public class OOEnv extends DefaultIndentable{

	private FunTable<IRData> vmtables;
	private FunTable<IRExp> fields;
	private FunTable<IRExp> methods;
	private FunTable<Access> symbols;
	private int fieldCount = 0;
	private int methodCount = 0;
	private int formalCount = 0;
	
	private OOEnv() {
		vmtables = FunTable.theEmpty();
		methods = FunTable.theEmpty();
		fields = FunTable.theEmpty();
		symbols = FunTable.theEmpty();
	}
	
	protected static OOEnv theEmpty() {
		return new OOEnv();
	}

	// Associates the label name with a global data fragment initialized to the given values table 
	protected DataFragment insertTable(Frame frame, String name, List<IRExp> table) {
		IRData data = IR.DATA(Label.get(name), table);
		vmtables = vmtables.insert(name, data);
		return new DataFragment(frame, data);
	}

	// Declares a new field for the current class and associates it with the next (field) offset
	protected void insertField(String name) {
		IRExp offset = IR.CONST(fieldCount++ * X86_64Frame.WORD_SIZE);
		fields = fields.insert(name, offset);
	}

	// Binds the given identifier to the next offset of a virtual method table
	protected void insertMethod(String name) {
		IRExp offset = IR.CONST(++methodCount * X86_64Frame.WORD_SIZE);
		methods = methods.insert(name, offset);
	}
	
	// Binds the parameter at formalCount of the current method to the specified name 
	protected void insertFormal(Frame frame, String name) {
		Access location = frame.getFormal(formalCount++);
		symbols = symbols.insert(name, location);
	}

	// Clear the environment for a new class
	protected void exitClass() {
		fields = FunTable.theEmpty();
		symbols = FunTable.theEmpty();
		fieldCount = methodCount = formalCount = 0;
	}
	
	// Allocates name as a new local in the given frame, defined within the current method 
	protected void insertLocal(Frame frame, String name, boolean escapes) {
		Access location = frame.allocLocal(escapes);
		symbols = symbols.insert(name, location);
	}

	// Clear the environment for a new method
	protected void exitMethod() {
		this.symbols = FunTable.theEmpty();
		formalCount = 0;
	}
	
	protected IRExp lookupTables(String name) {
		IRData global = vmtables.lookup(name);
		return (global != null) ?  IR.NAME(global.getLabel()) : null;
	}

	protected IRExp lookupMethods(String name) {
		return methods.lookup(name);
	}
	
	protected IRExp lookupFields(String name, IRExp thisExp) {
		IRExp offset = fields.lookup(name);
		if (offset != null)
			return IR.MEM(IR.PLUS(thisExp, offset));
		return null;
	}
	
	protected IRExp lookupSymbol(IRExp fp, String name) {
		Access location = symbols.lookup(name);
		return (location != null) ? location.exp(fp) : null;
	}

	@Override
	public void dump(IndentingWriter out) {
		out.println("environment {");
		out.indent();
		out.println("vmtables {");
		out.indent();
		for (util.FunTable.Entry<IRData> entry : vmtables) {
			out.print(entry.getId() + " = ");
			out.println(entry.getValue());
		}
		out.outdent();
		out.println("}");
		out.println("fields {");
		out.indent();
		for (util.FunTable.Entry<IRExp> entry : fields) {
			out.print(entry.getId() + " = ");
			out.println(entry.getValue());
		}
		out.outdent();
		out.println("}");
		out.println("methods {");
		out.indent();
		for (util.FunTable.Entry<IRExp> entry : methods) {
			out.print(entry.getId() + " = ");
			out.println(entry.getValue());
		}
		out.outdent();
		out.println("}");
		out.println("symbols {");
		out.indent();
		for (util.FunTable.Entry<Access> entry : symbols) {
			out.print(entry.getId() + " = ");
			out.println(entry.getValue());
		}
		out.outdent();
		out.println("}");
		out.outdent();
		out.print("}");
	}

}
