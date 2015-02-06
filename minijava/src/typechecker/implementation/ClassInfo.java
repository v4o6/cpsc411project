package typechecker.implementation;

import java.util.Map.Entry;

import util.DefaultIndentable;
import util.ImpTable;
import util.IndentingWriter;
import ast.ClassDecl;
import ast.Type;

public class ClassInfo extends DefaultIndentable{
	
	public final ClassDecl classDecl;
	public ClassInfo superClass;
	public ImpTable<Type> fields;
	public ImpTable<MethodInfo> methods;
	
	public ClassInfo(ClassDecl classDecl, ClassInfo superClass) {
		this.classDecl = classDecl;
		this.superClass = superClass;
		this.fields = new ImpTable<Type>();
		this.methods = new ImpTable<MethodInfo>();
	}

	public ClassInfo(ClassDecl classDecl) {
		this.classDecl = classDecl;
		this.superClass = null;
		this.fields = new ImpTable<Type>();
		this.methods = new ImpTable<MethodInfo>();
	}
		
	@Override
	public void dump(IndentingWriter out) {
		out.println("class {");
		out.indent();
		if (superClass != null)
			out.println("super = " + superClass.classDecl.name);
		out.println("fields {");
		out.indent();
		for (Entry<String, Type> entry : fields) {
			out.print(entry.getKey()+" = ");
			out.println(entry.getValue());
		}
		out.outdent();
		out.println("}");
		out.println("methods {");
		out.indent();
		for (Entry<String, MethodInfo> entry : methods) {
			out.print(entry.getKey()+" = ");
			out.println(entry.getValue());
		}
		out.outdent();
		out.println("}");
		out.outdent();
		out.print("}");
	}
	
}
