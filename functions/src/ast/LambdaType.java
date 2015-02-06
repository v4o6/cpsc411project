package ast;

import java.util.ArrayList;

import visitor.Visitor;

public class LambdaType extends Type {

	public Type returnType;
	public ArrayList<Type> parameterTypes;
	
	public LambdaType() {
	}
	
	public LambdaType(Type returnType, ArrayList<Type> parameterTypes) {
		this.returnType = returnType;
		this.parameterTypes = parameterTypes;
	}	
	
	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}
	
	@Override
	public boolean equals(Object other) {
//		if (this.getClass()==other.getClass()) {
//			LambdaType o = (LambdaType) other;
//			if (!returnType.equals(o.returnType)) 
//				return false;
//			for (int i = 0; i < parameterTypes.size(); i++) {
//				if (!parameterTypes.get(i).equals(o.parameterTypes.get(i)))
//					return false;
//			}
//			return true;
//		}
//		return false;
		return this.getClass()==other.getClass();
	}

}
