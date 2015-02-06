package codegen.x86_64;

import static analysis.implementation.SpillColor.spilledTEMP;
import static codegen.patterns.IRPat.CALL;
import static codegen.patterns.IRPat.CJUMP;
import static codegen.patterns.IRPat.CMOVE;
import static codegen.patterns.IRPat.CONST;
import static codegen.patterns.IRPat.EXP;
import static codegen.patterns.IRPat.JUMP;
import static codegen.patterns.IRPat.LABEL;
import static codegen.patterns.IRPat.MEM;
import static codegen.patterns.IRPat.MINUS;
import static codegen.patterns.IRPat.MOVE;
import static codegen.patterns.IRPat.MUL;
import static codegen.patterns.IRPat.NAME;
import static codegen.patterns.IRPat.PLUS;
import static codegen.patterns.IRPat.TEMP;
import static codegen.patterns.IRPat.XOR;
import static ir.frame.x86_64.X86_64Frame.RAX;
import static ir.frame.x86_64.X86_64Frame.RDX;
import static ir.frame.x86_64.X86_64Frame.RV;
import static ir.frame.x86_64.X86_64Frame.arguments;
import static ir.frame.x86_64.X86_64Frame.callerSave;
import static ir.frame.x86_64.X86_64Frame.special;
import static util.List.list;
import util.IndentingWriter;
import util.List;
import ir.frame.Frame;
import ir.temp.Label;
import ir.temp.Temp;
import ir.tree.IR;
import ir.tree.IRExp;
import ir.tree.IRStm;
import ir.tree.CJUMP.RelOp;
import codegen.assem.A_LABEL;
import codegen.assem.A_MOVE;
import codegen.assem.A_OPER;
import codegen.assem.Instr;
import codegen.muncher.MunchRule;
import codegen.muncher.Muncher;
import codegen.muncher.MuncherRules;
import codegen.patterns.Matched;
import codegen.patterns.Pat;
import codegen.patterns.Wildcard;
import analysis.implementation.SpillColor;

/**
 * This Muncher implements the munching rules for a subset
 * of X86 instruction set.
 * 
 * @author kdvolder
 */
public class X86_64Muncher extends Muncher {

	private static final List<Temp> noTemps = List.empty();

	private static MuncherRules<IRStm, Void> sm = new MuncherRules<IRStm, Void>();
	private static MuncherRules<IRExp, Temp> em = new MuncherRules<IRExp, Temp>();
	private static MuncherRules<IRExp, Void> dm = new MuncherRules<IRExp, Void>();
	
	public X86_64Muncher(Frame frame) {
		super(frame, sm, em, dm);
	}

	public X86_64Muncher(Frame frame, boolean beVerbose) {
		super(frame, sm, em, beVerbose);
	}

	//////////// The munching rules ///////////////////////////////

	static { //Done only once, at class loading time.

		// Pattern "variables" (used by the rules below)

		final Pat<IRExp>        _e_ = Pat.any();
		final Pat<IRExp>        _l_ = Pat.any();
		final Pat<IRExp>        _r_ = Pat.any();

		final Pat<List<IRExp>> _es_ = Pat.any();

		final Pat<Label>       _lab_ = Pat.any();
		final Pat<Label>       _thn_ = Pat.any();
		final Pat<Label>       _els_ = Pat.any();

		final Pat<RelOp>     _relOp_ = Pat.any();

		final Pat<Temp>          _t_ = Pat.any();
		final Pat<SpillColor>    _sc_ = Pat.any();

		final Pat<Integer>       _i_ = Pat.any();
		final Pat<Integer>  _offset_ = Pat.any();
		final Pat<Integer>   _scale_ = new Wildcard<Integer>() {
			@Override
			public void match(Integer toMatch, Matched matched) throws Failed {
				int value = (Integer)toMatch;
				if (value==1 || value==2 || value==4 || value==8) 
					super.match(toMatch, matched);
				else 
					fail();
			}

			public void dump(IndentingWriter out) {
				out.print("1|2|4|8");
			}
		};

		// tiles

		dm.add(new MunchRule<IRExp, Void>( CONST(_i_) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				m.emit(A_QUAD(c.get(_i_)));
				return null;
			}
		});
		dm.add(new MunchRule<IRExp, Void>( NAME(_lab_) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				m.emit(A_QUAD(c.get(_lab_)));
				return null;
			}
		});

		sm.add(new MunchRule<IRStm, Void>( LABEL(_lab_) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				m.emit(A_LABEL(c.get(_lab_)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(CONST(_offset_), _l_, MUL(CONST(_scale_), _r_))), CONST(_i_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp base = m.munch(c.get(_l_));
				Temp index = m.munch(c.get(_r_));
				m.emit(new A_OPER("movq    $" + c.get(_i_) + ", " + c.get(_offset_) + "(`s0, `s1, " + c.get(_scale_) + ")", noTemps, list(base, index)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(CONST(_offset_), _l_, _r_)), CONST(_i_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp base = m.munch(c.get(_l_));
				Temp index = m.munch(c.get(_r_));
				m.emit(new A_OPER("movq    $" + c.get(_i_) + ", " + c.get(_offset_) + "(`s0, `s1)", noTemps, list(base, index)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(CONST(_offset_), _e_)), CONST(_i_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp base = m.munch(c.get(_e_));
				m.emit(new A_OPER("movq    $" + c.get(_i_) + ", " + c.get(_offset_) + "(`s0)", noTemps, list(base)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(_l_, MUL(CONST(_scale_), _r_))), CONST(_i_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp base = m.munch(c.get(_l_));
				Temp index = m.munch(c.get(_r_));
				m.emit(new A_OPER("movq    $" + c.get(_i_) + ", (`s0, `s1, " + c.get(_scale_) + ")", noTemps, list(base, index)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(_l_, _r_)), CONST(_i_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp base = m.munch(c.get(_l_));
				Temp index = m.munch(c.get(_r_));
				m.emit(new A_OPER("movq    $" + c.get(_i_) + ", (`s0, `s1)", noTemps, list(base, index)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(_e_), CONST(_i_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp base = m.munch(c.get(_e_));
				m.emit(new A_OPER("movq    $" + c.get(_i_) + ", (`s0)", noTemps, list(base)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(CONST(_offset_), _l_, MUL(CONST(_scale_), _r_))), NAME(_lab_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp base = m.munch(c.get(_l_));
				Temp index = m.munch(c.get(_r_));
				m.emit(new A_OPER("movq    $" + c.get(_lab_) + ", " + c.get(_offset_) + "(`s0, `s1, " + c.get(_scale_) + ")", noTemps, list(base, index)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(CONST(_offset_), _l_, _r_)), NAME(_lab_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp base = m.munch(c.get(_l_));
				Temp index = m.munch(c.get(_r_));
				m.emit(new A_OPER("movq    $" + c.get(_lab_) + ", " + c.get(_offset_) + "(`s0, `s1)", noTemps, list(base, index)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(CONST(_offset_), _e_)), NAME(_lab_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp base = m.munch(c.get(_e_));
				m.emit(new A_OPER("movq    $" + c.get(_lab_) + ", " + c.get(_offset_) + "(`s0)", noTemps, list(base)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(_l_, MUL(CONST(_scale_), _r_))), NAME(_lab_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp base = m.munch(c.get(_l_));
				Temp index = m.munch(c.get(_r_));
				m.emit(new A_OPER("movq    $" + c.get(_lab_) + ", (`s0, `s1, " + c.get(_scale_) + ")", noTemps, list(base, index)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(_l_, _r_)), NAME(_lab_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp base = m.munch(c.get(_l_));
				Temp index = m.munch(c.get(_r_));
				m.emit(new A_OPER("movq    $" + c.get(_lab_) + ", (`s0, `s1)", noTemps, list(base, index)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(_e_), NAME(_lab_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp base = m.munch(c.get(_e_));
				m.emit(new A_OPER("movq    $" + c.get(_lab_) + ", (`s0)", noTemps, list(base)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(CONST(_offset_), _l_, MUL(CONST(_scale_), _r_))), _e_) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp val = m.munch(c.get(_e_));
				Temp base = m.munch(c.get(_l_));
				Temp index = m.munch(c.get(_r_));
				m.emit(new A_OPER("movq    `s0, " + c.get(_offset_) + "(`s1, `s2, " + c.get(_scale_) + ")", noTemps, list(val, base, index)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(CONST(_offset_), _l_, _r_)), _e_) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp val = m.munch(c.get(_e_));
				Temp base = m.munch(c.get(_l_));
				Temp index = m.munch(c.get(_r_));
				m.emit(new A_OPER("movq    `s0, " + c.get(_offset_) + "(`s1, `s2)", noTemps, list(val, base, index)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(CONST(_offset_), _l_)), _r_) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp val = m.munch(c.get(_r_));
				Temp base = m.munch(c.get(_l_));
				m.emit(new A_OPER("movq    `s0, " + c.get(_offset_) + "(`s1)", noTemps, list(val, base)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(_l_, MUL(CONST(_scale_), _r_))), _e_) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp val = m.munch(c.get(_e_));
				Temp base = m.munch(c.get(_l_));
				Temp index = m.munch(c.get(_r_));
				m.emit(new A_OPER("movq    `s0, (`s1, `s2, " + c.get(_scale_) + ")", noTemps, list(val, base, index)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(PLUS(_l_, _r_)), _e_) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp val = m.munch(c.get(_e_));
				Temp base = m.munch(c.get(_l_));
				Temp index = m.munch(c.get(_r_));
				m.emit(new A_OPER("movq    `s0, (`s1, `s2)", noTemps, list(val, base, index)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(MEM(_l_), _r_) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp val = m.munch(c.get(_r_));
				Temp base = m.munch(c.get(_l_));
				m.emit(new A_OPER("movq    `s0, (`s1)", noTemps, list(val, base)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(TEMP(_t_), CONST(_i_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				m.emit(A_MOV( c.get(_t_), c.get(_i_) ));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(TEMP(_t_), NAME(_lab_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				m.emit(A_MOV( c.get(_t_), c.get(_lab_) ));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( MOVE(TEMP(_t_), _e_) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				m.emit(A_MOV( c.get(_t_), m.munch(c.get(_e_)) ));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( JUMP(NAME(_lab_)) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {	
				m.emit(A_JMP(c.get(_lab_)));
				return null;
			}
		});		
		sm.add(new MunchRule<IRStm, Void>( JUMP(_e_) ) {
			protected Void trigger(Muncher m, Matched children) {
				// Minijava shouldn't need to emit indirect jumps.
				// (assuming there's a rule to match JUMP(NAME(*))
				throw new Error("Not implemented");
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CMOVE( _relOp_, MEM(PLUS(CONST(_offset_), _l_)), CONST(_i_), TEMP(_t_), _e_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				m.emit( new A_OPER("cmpq    $" + c.get(_i_) + ", " + c.get(_offset_) + "(`s0)", noTemps, list(l)));
				m.emit( A_CMOV(c.get(_relOp_), c.get(_t_), m.munch(c.get(_e_))) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CMOVE( _relOp_, CONST(_i_), MEM(PLUS(CONST(_offset_), _r_)), TEMP(_t_), _e_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    $" + c.get(_i_) + ", " + c.get(_offset_) + "(`s0)", noTemps, list(r)));
				m.emit( A_CMOV(invertRelOp(c.get(_relOp_)), c.get(_t_), m.munch(c.get(_e_))) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CMOVE( _relOp_, MEM(_l_), CONST(_i_), TEMP(_t_), _e_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				m.emit( new A_OPER("cmpq    $" + c.get(_i_) + ", (`s0)", noTemps, list(l)));
				m.emit( A_CMOV(c.get(_relOp_), c.get(_t_), m.munch(c.get(_e_))) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CMOVE( _relOp_, CONST(_i_), MEM(_r_), TEMP(_t_), _e_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    $" + c.get(_i_) + ", (`s0)", noTemps, list(r)));
				m.emit( A_CMOV(invertRelOp(c.get(_relOp_)), c.get(_t_), m.munch(c.get(_e_))) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CMOVE( _relOp_, MEM(PLUS(CONST(_offset_), _l_)), _r_, TEMP(_t_), _e_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    `s1, " + c.get(_offset_) + "(`s0)", noTemps, list(l, r)));
				m.emit( A_CMOV(c.get(_relOp_), c.get(_t_), m.munch(c.get(_e_))) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CMOVE( _relOp_, _l_, MEM(PLUS(CONST(_offset_), _r_)), TEMP(_t_), _e_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    " + c.get(_offset_) + "(`s1), `s0", noTemps, list(l, r)));
				m.emit( A_CMOV(c.get(_relOp_), c.get(_t_), m.munch(c.get(_e_))) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CMOVE( _relOp_, MEM(_l_), _r_, TEMP(_t_), _e_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    `s1, (`s0)", noTemps, list(l, r)));
				m.emit( A_CMOV(c.get(_relOp_), c.get(_t_), m.munch(c.get(_e_))) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CMOVE( _relOp_, _l_, MEM(_r_), TEMP(_t_), _e_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    (`s1), `s0", noTemps, list(l, r)));
				m.emit( A_CMOV(c.get(_relOp_), c.get(_t_), m.munch(c.get(_e_))) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CMOVE( _relOp_, _l_, CONST(_i_), TEMP(_t_), _e_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				m.emit( new A_OPER("cmpq    $" + c.get(_i_) + ", `s0", noTemps, list(l)));
				m.emit( A_CMOV(c.get(_relOp_), c.get(_t_), m.munch(c.get(_e_))) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CMOVE( _relOp_, CONST(_i_), _r_, TEMP(_t_), _e_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    $" + c.get(_i_) + ", `s0", noTemps, list(r)));
				m.emit( A_CMOV(invertRelOp(c.get(_relOp_)), c.get(_t_), m.munch(c.get(_e_))) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CMOVE( _relOp_, _l_, _r_, TEMP(_t_), _e_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				m.emit( A_CMP(m.munch(c.get(_l_)), m.munch(c.get(_r_)))    );
				m.emit( A_CMOV(c.get(_relOp_), c.get(_t_), m.munch(c.get(_e_))) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CJUMP( _relOp_, MEM(PLUS(CONST(_offset_), _l_)), CONST(_i_), _thn_, _els_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				m.emit( new A_OPER("cmpq    $" + c.get(_i_) + ", " + c.get(_offset_) + "(`s0)", noTemps, list(l)));
				m.emit( A_CJUMP(c.get(_relOp_), c.get(_thn_), c.get(_els_)) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CJUMP( _relOp_, CONST(_i_), MEM(PLUS(CONST(_offset_), _r_)), _thn_, _els_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    $" + c.get(_i_) + ", " + c.get(_offset_) + "(`s0)", noTemps, list(r)));
				m.emit( A_CJUMP(invertRelOp(c.get(_relOp_)), c.get(_thn_), c.get(_els_)) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CJUMP( _relOp_, MEM(_l_), CONST(_i_), _thn_, _els_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				m.emit( new A_OPER("cmpq    $" + c.get(_i_) + ", (`s0)", noTemps, list(l)));
				m.emit( A_CJUMP(c.get(_relOp_), c.get(_thn_), c.get(_els_)) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CJUMP( _relOp_, CONST(_i_), MEM(_r_), _thn_, _els_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    $" + c.get(_i_) + ", (`s0)", noTemps, list(r)));
				m.emit( A_CJUMP(invertRelOp(c.get(_relOp_)), c.get(_thn_), c.get(_els_)) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CJUMP( _relOp_, MEM(PLUS(CONST(_offset_), _l_)), _r_, _thn_, _els_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    `s1, " + c.get(_offset_) + "(`s0)", noTemps, list(l, r)));
				m.emit( A_CJUMP(c.get(_relOp_), c.get(_thn_), c.get(_els_)) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CJUMP( _relOp_, _l_, MEM(PLUS(CONST(_offset_), _r_)), _thn_, _els_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    " + c.get(_offset_) + "(`s1), `s0", noTemps, list(l, r)));
				m.emit( A_CJUMP(c.get(_relOp_), c.get(_thn_), c.get(_els_)) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CJUMP( _relOp_, MEM(_l_), _r_, _thn_, _els_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    `s1, (`s0)", noTemps, list(l, r)));
				m.emit( A_CJUMP(c.get(_relOp_), c.get(_thn_), c.get(_els_)) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CJUMP( _relOp_, _l_, MEM(_r_), _thn_, _els_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    (`s1), `s0", noTemps, list(l, r)));
				m.emit( A_CJUMP(c.get(_relOp_), c.get(_thn_), c.get(_els_)) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CJUMP( _relOp_, _l_, CONST(_i_), _thn_, _els_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp l = m.munch(c.get(_l_));
				m.emit( new A_OPER("cmpq    $" + c.get(_i_) + ", `s0", noTemps, list(l)));
				m.emit( A_CJUMP(c.get(_relOp_), c.get(_thn_), c.get(_els_)) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( CJUMP( _relOp_, CONST(_i_), _r_, _thn_, _els_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Temp r = m.munch(c.get(_r_));
				m.emit( new A_OPER("cmpq    $" + c.get(_i_) + ", `s0", noTemps, list(r)));
				m.emit( A_CJUMP(invertRelOp(c.get(_relOp_)), c.get(_thn_), c.get(_els_)) );
				return null;
			}
		});		
		sm.add(new MunchRule<IRStm, Void>( CJUMP( _relOp_, _l_, _r_, _thn_, _els_ ) ) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				m.emit( A_CMP(m.munch(c.get(_l_)), m.munch(c.get(_r_)))    );
				m.emit( A_CJUMP(c.get(_relOp_), c.get(_thn_), c.get(_els_)) );
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>( EXP(_e_) ) {
			@Override
			protected Void trigger(Muncher m, Matched children) {
				IRExp exp = children.get(_e_);
				m.munch(exp);
				return null;
			}
		});

		em.add(new MunchRule<IRExp, Temp>( PLUS(CONST(_i_), _l_, _r_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp dst = new Temp();
				Temp l = m.munch(c.get(_l_));
				Temp r = m.munch(c.get(_r_));
				m.emit(new A_OPER("leaq    " + c.get(_i_) + "(`s0, `s1), `d0", list(dst), list(l, r)));
				return dst;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( PLUS(CONST(_i_), _e_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				int i = c.get(_i_);
				switch (i) {
				case 0:
					return  m.munch(c.get(_e_));
				case 1:
					Temp inc = new Temp();
					m.emit(A_MOV(inc, m.munch(c.get(_e_))));
					m.emit(new A_OPER("incq    `d0", list(inc), list(inc)));
					return inc;
				default:
					Temp sum = new Temp();
					m.emit(A_MOV(sum, m.munch(c.get(_e_))));
					m.emit(new A_OPER("addq    $" + i + ", `d0", list(sum), list(sum)));
					return sum;
				}
			}
		});
		em.add(new MunchRule<IRExp, Temp>( PLUS(_l_, _r_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp sum = new Temp();
				m.emit(A_MOV(sum, m.munch(c.get(_l_))));
				m.emit(A_ADD(sum, m.munch(c.get(_r_))));
				return sum;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( MINUS(CONST(_i_), _e_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				int i = c.get(_i_);
				switch (i) {
				case 0:
					return  m.munch(c.get(_e_));
				case 1:
					Temp dec = new Temp();
					m.emit(A_MOV(dec, m.munch(c.get(_e_))));
					m.emit(new A_OPER("decq    `d0", list(dec), list(dec)));
					return dec;
				default:
					Temp dif = new Temp();
					m.emit(A_MOV(dif, m.munch(c.get(_e_))));
					m.emit(new A_OPER("subq    $" + i + ", `d0", list(dif), list(dif)));
					return dif;
				}
			}
		});
		em.add(new MunchRule<IRExp, Temp>( MINUS(_l_, _r_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp dif = new Temp();
				m.emit(A_MOV(dif, m.munch(c.get(_l_))));
				m.emit(A_SUB(dif, m.munch(c.get(_r_))));
				return dif;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( XOR(CONST(_i_), _e_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp xor = new Temp();
				m.emit(A_MOV(xor, m.munch(c.get(_e_))));
				m.emit(new A_OPER("xorq    $" + c.get(_i_) + ", `d0", list(xor), list(xor)));
				return xor;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( XOR(_l_, _r_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp xor = new Temp();
				m.emit(A_MOV(xor, m.munch(c.get(_l_))));
				m.emit(A_XOR(xor, m.munch(c.get(_r_))));
				return xor;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( MUL(CONST(_i_), _l_, _r_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp src = new Temp();
				m.emit(A_MOV(src, m.munch(c.get(_l_))));
				Temp dst = new Temp();
				m.emit(A_MOV(dst, m.munch(c.get(_r_))));
				m.emit(new A_OPER("imulq    $" + c.get(_i_) + ", `s0, `d0", list(src, dst), list(dst)));
				return dst;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( MUL(CONST(_i_), _e_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp prd = new Temp();
				int i = c.get(_i_);
				switch (i) {
				case 0:
					m.emit(A_XOR(prd, prd));
					return prd;
				case 1:
					return m.munch(c.get(_e_));
				default:
					m.emit(A_MOV(prd, m.munch(c.get(_e_))));
					Temp tmp;
					switch(i) {
					case 2:
						m.emit(new A_OPER("salq    $1, `d0", list(prd), list(prd)));
						break;
					case 3:
						m.emit(new A_OPER("leaq    (`s0, `s0, 2), `d0", list(prd), list(prd)));
						break;
					case 4:
						m.emit(new A_OPER("salq    $2, `d0", list(prd), list(prd)));
						break;
					case 5:
						m.emit(new A_OPER("leaq    (`s0, `s0, 4), `d0", list(prd), list(prd)));
						break;
					case 6:
						m.emit(new A_OPER("salq    $1, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 2), `d0", list(prd), list(prd)));
						break;
					case 7:
						tmp = new Temp();
						m.emit(new A_OPER("leaq    (`s0, `s0, 4), `d0", list(tmp), list(prd)));
						m.emit(new A_OPER("leaq    (`s1, `s0, 2), `d0", list(prd), list(prd, tmp)));
						break;
					case 8:
						m.emit(new A_OPER("salq    $3, `d0", list(prd), list(prd)));
						break;
					case 9:
						m.emit(new A_OPER("leaq    (`s0, `s0, 8), `d0", list(prd), list(prd)));
						break;
					case 10:
						m.emit(new A_OPER("salq    $1, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 4), `d0", list(prd), list(prd)));
						break;
					case 11:
						tmp = new Temp();
						m.emit(new A_OPER("leaq    (`s0, `s0, 4), `d0", list(tmp), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s1, 2), `d0", list(prd), list(prd, tmp)));
						break;
					case 12:
						m.emit(new A_OPER("salq    $2, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 2), `d0", list(prd), list(prd)));
						break;
					case 13:
						tmp = new Temp();
						m.emit(new A_OPER("leaq    (`s0, `s0, 8), `d0", list(tmp), list(prd)));
						m.emit(new A_OPER("leaq    (`s1, `s0, 4), `d0", list(prd), list(prd, tmp)));
						break;
					case 16:
						m.emit(new A_OPER("salq    $4, `d0", list(prd), list(prd)));
						break;
					case 17:
						tmp = new Temp();
						m.emit(new A_OPER("leaq    (`s0, `s0, 8), `d0", list(tmp), list(prd)));
						m.emit(new A_OPER("leaq    (`s1, `s0, 8), `d0", list(prd), list(prd, tmp)));
						break;
					case 18:
						m.emit(new A_OPER("salq    $1, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 8), `d0", list(prd), list(prd)));
						break;
					case 19:
						tmp = new Temp();
						m.emit(new A_OPER("leaq    (`s0, `s0, 8), `d0", list(tmp), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s1, 2), `d0", list(prd), list(prd, tmp)));
						break;
					case 20:
						m.emit(new A_OPER("salq    $2, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 4), `d0", list(prd), list(prd)));
						break;
					case 21:
						tmp = new Temp();
						m.emit(new A_OPER("leaq    (`s0, `s0, 4), `d0", list(tmp), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s1, 4), `d0", list(prd), list(prd, tmp)));
						break;
					case 24:
						m.emit(new A_OPER("salq    $3, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 2), `d0", list(prd), list(prd)));
						break;
					case 32:
						m.emit(new A_OPER("salq    $5, `d0", list(prd), list(prd)));
						break;
					case 36:
						m.emit(new A_OPER("salq    $2, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 8), `d0", list(prd), list(prd)));
						break;
					case 37:
						tmp = new Temp();
						m.emit(new A_OPER("leaq    (`s0, `s0, 8), `d0", list(tmp), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s1, 4), `d0", list(prd), list(prd, tmp)));
						break;
					case 40:
						m.emit(new A_OPER("salq    $3, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 4), `d0", list(prd), list(prd)));
						break;
					case 48:
						m.emit(new A_OPER("salq    $4, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 2), `d0", list(prd), list(prd)));
						break;
					case 64:
						m.emit(new A_OPER("salq    $6, `d0", list(prd), list(prd)));
						break;
					case 72:
						m.emit(new A_OPER("salq    $3, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 8), `d0", list(prd), list(prd)));
						break;
					case 73:
						tmp = new Temp();
						m.emit(new A_OPER("leaq    (`s0, `s0, 8), `d0", list(tmp), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s1, 8), `d0", list(prd), list(prd, tmp)));
						break;
					case 80:
						m.emit(new A_OPER("salq    $4, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 4), `d0", list(prd), list(prd)));
						break;
					case 96:
						m.emit(new A_OPER("salq    $5, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 2), `d0", list(prd), list(prd)));
						break;
					case 128:
						m.emit(new A_OPER("salq    $6, `d0", list(prd), list(prd)));
						break;
					case 144:
						m.emit(new A_OPER("salq    $4, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 8), `d0", list(prd), list(prd)));
						break;
					case 160:
						m.emit(new A_OPER("salq    $5, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 4), `d0", list(prd), list(prd)));
						break;
					case 192:
						m.emit(new A_OPER("salq    $6, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 2), `d0", list(prd), list(prd)));
					case 256:
						m.emit(new A_OPER("salq    $7, `d0", list(prd), list(prd)));
						break;
					case 288:
						m.emit(new A_OPER("salq    $5, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 8), `d0", list(prd), list(prd)));
						break;
					case 320:
						m.emit(new A_OPER("salq    $6, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 4), `d0", list(prd), list(prd)));
					case 384:
						m.emit(new A_OPER("salq    $7, `d0", list(prd), list(prd)));
						m.emit(new A_OPER("leaq    (`s0, `s0, 2), `d0", list(prd), list(prd)));
					case 512:
						m.emit(new A_OPER("salq    $8, `d0", list(prd), list(prd)));
						break;
					default:
						m.emit(new A_OPER("imulq    $" + c.get(_i_) + ", `d0", list(prd), list(prd)));
					}
					return prd;
				}
			}
		});
		em.add(new MunchRule<IRExp, Temp>( MUL(_l_, _r_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp res = new Temp();
				m.emit(A_MOV(res, m.munch(c.get(_l_))));
				m.emit(A_IMUL(res, m.munch(c.get(_r_))));
				return res;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( TEMP(_t_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				return c.get(_t_);
			}
		});						
		em.add(new MunchRule<IRExp, Temp>( CONST(_i_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp t = new Temp();
				m.emit(A_MOV(t, c.get(_i_)) );
				return t;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( NAME(_lab_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp t = new Temp();
				m.emit(A_MOV(t, c.get(_lab_)) );
				return t;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( MEM(PLUS(CONST(_offset_), _l_, MUL(CONST(_scale_), _r_))) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp base = new Temp();
				m.emit(A_MOV(base, m.munch(c.get(_l_))));
				Temp index = new Temp();
				m.emit(A_MOV(index, m.munch(c.get(_r_))));
				Temp dst = new Temp();
				m.emit(new A_OPER("movq    " + c.get(_offset_) + "(`s0, `s1, " + c.get(_scale_) + "), `d0", list(dst), list(base, index)));
				return dst;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( MEM(PLUS(CONST(_offset_), _l_, _r_)) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp base = new Temp();
				m.emit(A_MOV(base, m.munch(c.get(_l_))));
				Temp index = new Temp();
				m.emit(A_MOV(index, m.munch(c.get(_r_))));
				Temp dst = new Temp();
				m.emit(new A_OPER("movq    " + c.get(_offset_) + "(`s0, `s1), `d0", list(dst), list(base, index)));
				return dst;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( MEM(PLUS(CONST(_offset_), _e_)) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp base = new Temp();
				m.emit(A_MOV(base, m.munch(c.get(_e_))));
				Temp dst = new Temp();
				m.emit(new A_OPER("movq    " + c.get(_offset_) + "(`s0), `d0", list(dst), list(base)));
				return dst;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( MEM(PLUS(_l_, MUL(CONST(_scale_), _r_))) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp base = new Temp();
				m.emit(A_MOV(base, m.munch(c.get(_l_))));
				Temp index = new Temp();
				m.emit(A_MOV(index, m.munch(c.get(_r_))));
				Temp dst = new Temp();
				m.emit(new A_OPER("movq    (`s0, `s1, " + c.get(_scale_) + "), `d0", list(dst), list(base, index)));
				return dst;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( MEM(PLUS(_l_, _r_)) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp base = new Temp();
				m.emit(A_MOV(base, m.munch(c.get(_l_))));
				Temp index = new Temp();
				m.emit(A_MOV(index, m.munch(c.get(_r_))));
				Temp dst = new Temp();
				m.emit(new A_OPER("movq    (`s0, `s1), `d0", list(dst), list(base, index)));
				return dst;
			}
		});
		em.add(new MunchRule<IRExp, Temp>( MEM(_e_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Temp base = new Temp();
				m.emit(A_MOV(base, m.munch(c.get(_e_))));
				Temp dst = new Temp();
				m.emit(new A_OPER("movq    (`s0), `d0", list(dst), list(base)));
				return dst;
			}
		});	
		em.add(new MunchRule<IRExp, Temp>( CALL(NAME(_lab_), _es_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Frame frame = m.getFrame();
				Label name = c.get(_lab_);
				List<IRExp> args = c.get(_es_);
				for (int i = args.size()-1; i >= 0; i--) {
					IRExp outArg = frame.getOutArg(i).exp(frame.FP());
					m.munch( IR.MOVE(outArg, args.get(i)) );
				}
				m.emit(A_CALL(name, args.size()));
				return RV;
			}

		});
		em.add(new MunchRule<IRExp, Temp>( CALL(_e_, _es_) ) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				Frame frame = m.getFrame();
				Temp ptr = m.munch(c.get(_e_));
				List<IRExp> args = c.get(_es_);
				for (int i = args.size()-1; i >= 0; i--) {
					IRExp outArg = frame.getOutArg(i).exp(frame.FP());
					m.munch( IR.MOVE(outArg, args.get(i)) );
				}
				m.emit(A_CALL(ptr, args.size()));
				return RV;
			}
		});
		
		//////// For matching spilled Temps /////

		em.add(new MunchRule<IRExp, Temp>(spilledTEMP(_sc_)) {
			@Override
			protected Temp trigger(Muncher m, Matched c) {
				SpillColor color = c.get(_sc_);
				return m.munch(color.getLocation());
			}
		});

		sm.add(new MunchRule<IRStm, Void>(MOVE(spilledTEMP(_sc_), _e_)) {
			@Override
			protected Void trigger(Muncher m, Matched c) {
				m.munch(IR.MOVE(c.get(_sc_).getLocation(), c.get(_e_)));
				return null;
			}
		});
		sm.add(new MunchRule<IRStm, Void>(CMOVE(_relOp_, _l_, _r_, spilledTEMP(_sc_), _e_)) {
			/** Due to limitations in both the munching infrastructure and the X86_64 simulator/interpreters' ability 
			 * to handle the combinatorial explosion of 1) comparison codes, 2) source values/addressing modes, and 
			 * 3) destination addressing modes, we're presently unable to express a cmov targetting a location in memory.
			 * 
			 * Although the consequences of this with regards to the conversion of normal CMOVE statements in the IR 
			 * is only some slightly less efficient code and unnecessary extra temp usage (unnecessarily storing 
			 * intermediate results or immediate values, etc.), we are unable to use this approach regarding CMOVEs 
			 * with spilled temps precisely because spills indicate that we cannot generate any more unnecessary temporaries.
			 * 
			 * The present solution therefore performs a hackish conversion of the CMOVE into a CJUMP, conditionally going 
			 * to a MOVE instruction (for which all of the addressing modes have been implemented by the muncher) before
			 * jumping back.
			 */
			@Override
			protected Void trigger(Muncher m, Matched c) {
				Label move = Label.gen();
				Label done = Label.gen();
				m.munch(IR.CJUMP(c.get(_relOp_), c.get(_l_), c.get(_r_), move, done));
				m.emit(A_LABEL(move));
				m.munch(IR.MOVE(c.get(_sc_).getLocation(), c.get(_e_)));
				m.emit(A_LABEL(done));
				return null;
			}
		});
		
	}

	///////// Helper methods to generate X86 assembly instructions //////////////////////////////////////

	private static Instr A_QUAD(int i) {
		return new A_OPER(".quad    " + i, noTemps, noTemps);
  	}
	private static Instr A_QUAD(Label l) {
		return new A_OPER(".quad    " + l, noTemps, noTemps);
  	}
	private static Instr A_LABEL(Label name) {
		return new A_LABEL(name+":", name);
	}
	private static Instr A_ADD(Temp dst, Temp src) {
		return new A_OPER("addq    `s0, `d0", 
				list(dst),
				list(src,dst));
	}
	private static Instr A_SUB(Temp dst, Temp src) {
		return new A_OPER("subq    `s0, `d0", 
				list(dst),
				list(src,dst));
	}
	private static Instr A_XOR(Temp dst, Temp src) {
		return new A_OPER("xorq    `s0, `d0", 
				list(dst),
				list(src,dst));
	}
	private static Instr A_IMUL(Temp dst, Temp src) {
		return new A_OPER("imulq   `s0, `d0", 
				list(dst),
				list(src,dst));
	}
	private static Instr A_IDIV(Temp dst, Temp src) {
		return new A_OPER("movq    `d0, %rax\n" +
				"   cqto\n" +
				"   idivq   `s0\n" +
				"   movq    %rax, `d0", 
				list(dst, RAX, RDX),
				list(src,dst));
	}
	private static Instr A_MOV(Temp d, Temp s) {
		return new A_MOVE("movq    `s0, `d0", d, s);
	}
	private static Instr A_MOV(Temp t, int value) {
		if (value == 0) 
			return new A_OPER("xorq    `d0, `d0", list(t), noTemps);
		else
			return new A_OPER("movq    $"+value+", `d0", list(t), noTemps);
	}
	private static Instr A_MOV(Temp d, Label l) {
		return new A_OPER("leaq    " + l + "(%rip), `d0", list(d), noTemps);
	}
	private static Instr A_JMP(Label target) {
		return new A_OPER("jmp     `j0", noTemps, noTemps, List.list(target));
	}
	private static Instr A_CMP(Temp l, Temp r) {
		return new A_OPER("cmpq    `s1, `s0", noTemps, list(l, r));
	}
	private static Instr A_CMOV(RelOp relOp, Temp d, Temp s) {
		return new A_OPER("cmov" + getRelOpCode(relOp) + "    `s0, `d0", list(d), list(s, d));
	}
	private static Instr A_CJUMP(RelOp relOp, Label thn, Label els) {
		return new A_OPER("j" + getRelOpCode(relOp) + "     `j0", noTemps, noTemps, list(thn, els));
	}
	private static Instr A_CALL(Label fun, int nargs) {
		List<Temp> args = List.empty();
		for (int i = 0; i < Math.min(arguments.size(), nargs); ++i) {
			args.add(arguments.get(i));
		}
		return new A_OPER("call    "+fun, callerSave.append(arguments), special.append(args)); 
	}
	private static Instr A_CALL(Temp t, int nargs) {
		List<Temp> args = List.empty();
		for (int i = 0; i < Math.min(arguments.size(), nargs); ++i) {
			args.add(arguments.get(i));
		}
		return new A_OPER("call    * `s0", callerSave.append(arguments), list(t).append(special.append(args))); 
	}

	private static RelOp invertRelOp(RelOp op) {
		switch (op) {
		case EQ:
			return RelOp.EQ;
		case NE:
			return RelOp.NE;
		case GE:
			return RelOp.LT;
		case LT:
			return RelOp.GE;
		case LE:
			return RelOp.GT;
		case GT:
			return RelOp.LE;
		case ULT:
			return RelOp.UGE;
		case UGT:
			return RelOp.ULE;
		case ULE:
			return RelOp.UGT;
		case UGE:
			return RelOp.ULT;
		default:
			throw new Error("Missing case?");
		}
	}
	private static String getRelOpCode(RelOp relOp) {
		switch (relOp) {
		case EQ:
			return "e ";
		case NE:
			return "ne";
		case GE:
			return "ge";
		case LT:
			return "l ";
		case LE:
			return "le";
		case GT:
			return "g";
		case ULT:
			return "b";
		case UGT:
			return "a";
		case ULE:
			return "be";
		case UGE:
			return "ae";
		default:
			throw new Error("Missing case?");
		}
	}
	
	
	public static void dumpRules() {
		System.out.println("StmMunchers: "+sm);
		System.out.println("ExpMunchers: "+em);
	}
}
