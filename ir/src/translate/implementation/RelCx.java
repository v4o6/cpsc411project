package translate.implementation;

import static ir.tree.IR.CJUMP;
import static ir.tree.IR.CMOVE;
import ir.temp.Label;
import ir.tree.CJUMP.RelOp;
import ir.tree.IRExp;
import ir.tree.IRStm;

public class RelCx extends Cx {

	private RelOp relop;
	private IRExp left;
	private IRExp right;
	
	public RelCx(RelOp relop, IRExp left, IRExp right) {
		this.relop = relop;
		this.left = left;
		this.right = right;
	}
	
	@Override
	public IRStm unCx(Label ifTrue, Label ifFalse) {
		return CJUMP(relop, left, right, ifTrue, ifFalse);
	}

	@Override
	public IRStm unCx(IRExp dst, IRExp src) {
		return CMOVE(relop, left, right, dst, src);
	}

}
