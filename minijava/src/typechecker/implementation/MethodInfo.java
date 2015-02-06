package typechecker.implementation;

import java.util.ArrayList;
import java.util.Map.Entry;

import util.DefaultIndentable;
import util.ImpTable;
import util.IndentingWriter;
import ast.Type;

public class MethodInfo extends DefaultIndentable {
	public final String methodClass;
	public final ArrayList<Type> formals; // ordered list for typechecking
	public final ImpTable<Type> symbols;
	public final Type returntype;
	
	public MethodInfo(String methodClass, Type returntype) {
		this.methodClass = methodClass;
		this.formals = new ArrayList<Type>();
		this.symbols = new ImpTable<Type>();
		this.returntype = returntype;
	}

	@Override
	public void dump(IndentingWriter out) {
		out.println("method {");
		out.indent();
		out.println("symbols {");
		out.indent();
		for (Entry<String, Type> entry : symbols) {
			out.print(entry.getKey()+" = ");
			out.println(entry.getValue());
		}
		out.outdent();
		out.println("}");
		out.println("returntype = " + returntype);
		out.outdent();
		out.print("}");
	}

}
