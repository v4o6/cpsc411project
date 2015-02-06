package translate.implementation;

import ir.temp.Label;
import ir.tree.IR;
import ir.tree.IRExp;
import ir.tree.IRStm;
import ir.tree.CJUMP.RelOp;

/**
 * A TRExp wrapper around an expression (that returns a result).
 */
public class Ex extends TRExp {

	private final IRExp exp;

	public Ex(IRExp exp) {
		this.exp = exp;
	}

	@Override
	public
	IRStm unCx(Label ifTrue, Label ifFalse) {
		// Optimizations as suggested in the book (pg. 142)
		if (exp.isCONST(0))
			return IR.JUMP(ifFalse);
		if (exp.isCONST(1))
			return IR.JUMP(ifTrue);

		return IR.CJUMP(RelOp.NE, exp, IR.CONST(0), ifTrue, ifFalse);
	}

	@Override
	public
	IRStm unCx(IRExp dst, IRExp src) {
		// Optimizations as suggested in the book (pg. 142)
		if (exp.isCONST(0))
			return IR.NOP;
		if (exp.isCONST(1))
			return IR.MOVE(dst, src);
		
		return IR.CMOVE(RelOp.NE, exp, IR.CONST(0), dst, src);
	}

	@Override
	public
	IRExp unEx() {
		return exp;
	}

	@Override
	public
	IRStm unNx() {
		return IR.EXP(exp);
	}

}
