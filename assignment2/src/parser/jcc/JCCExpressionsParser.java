/* Generated By:JavaCC: Do not edit this line. JCCExpressionsParser.java */
package parser.jcc;

import ast.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public class JCCExpressionsParser implements JCCExpressionsParserConstants {

  final public Program Program() throws ParseException {
        NodeList<Statement> ss = new NodeList<Statement>();
        Statement s;
        Expression e;
    label_1:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case IDENTIFIER:
        ;
        break;
      default:
        jj_la1[0] = jj_gen;
        break label_1;
      }
      s = Statement();
                  ss.add(s);
    }
    jj_consume_token(PRINT);
    e = Expression();
    jj_consume_token(0);
          {if (true) return new Program(ss, new Print(e));}
    throw new Error("Missing return statement in function");
  }

  final public Statement Statement() throws ParseException {
        Statement s = null;
    s = Assign();
          {if (true) return s;}
    throw new Error("Missing return statement in function");
  }

  final public Statement Assign() throws ParseException {
        IdentifierExp name;
        Expression value;
    name = Identifier();
    jj_consume_token(ASSIGN);
    value = Expression();
    jj_consume_token(SEMICOLON);
                 {if (true) return new Assign(name, value);}
    throw new Error("Missing return statement in function");
  }

  final public Expression Expression() throws ParseException {
        Expression e, e2, e3;
    e = CompExpression();
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case QUESTION:
      jj_consume_token(QUESTION);
      e2 = Expression();
      jj_consume_token(COLON);
      e3 = Expression();
                        e = new Conditional(e, e2, e3);
      break;
    default:
      jj_la1[1] = jj_gen;
      ;
    }
          {if (true) return e;}
    throw new Error("Missing return statement in function");
  }

// For parsing anything with priority same or higher than <
  final public Expression CompExpression() throws ParseException {
        Expression e, e2;
    e = AddExpression();
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case SMALLER:
      jj_consume_token(SMALLER);
      e2 = AddExpression();
                  e = new LessThan(e, e2);
      break;
    default:
      jj_la1[2] = jj_gen;
      ;
    }
          {if (true) return e;}
    throw new Error("Missing return statement in function");
  }

// For parsing anything with priority same or higher than +
  final public Expression AddExpression() throws ParseException {
        Expression e, e2;
        Token op;
    e = MultExpression();
    label_2:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case PLUS:
      case MINUS:
        ;
        break;
      default:
        jj_la1[3] = jj_gen;
        break label_2;
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case PLUS:
        op = jj_consume_token(PLUS);
        break;
      case MINUS:
        op = jj_consume_token(MINUS);
        break;
      default:
        jj_la1[4] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      e2 = MultExpression();
                          if (op.image.equals("+"))
                                e=new Plus(e, e2);
                          else
                                e=new Minus(e, e2);
    }
          {if (true) return e;}
    throw new Error("Missing return statement in function");
  }

// For parsing anything with priority same or higher than *
  final public Expression MultExpression() throws ParseException {
        Expression e, e2;
    e = NotExpression();
    label_3:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case MULT:
        ;
        break;
      default:
        jj_la1[5] = jj_gen;
        break label_3;
      }
      jj_consume_token(MULT);
      e2 = NotExpression();
                          e = new Times(e, e2);
    }
          {if (true) return e;}
    throw new Error("Missing return statement in function");
  }

// For parsing anything with priority same or higher than ! expressions:
  final public Expression NotExpression() throws ParseException {
        Expression e;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case NOT:
      jj_consume_token(NOT);
      e = NotExpression();
                  {if (true) return new Not(e);}
      break;
    case LPAREN:
    case INTEGER_LITERAL:
    case IDENTIFIER:
      e = PrimaryExpression();
                  {if (true) return e;}
      break;
    default:
      jj_la1[6] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

/* PrimaryExpression is the expression that has highest precedence.*/
  final public Expression PrimaryExpression() throws ParseException {
        Token t;
        IdentifierExp i;
        Expression e;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case INTEGER_LITERAL:
      t = jj_consume_token(INTEGER_LITERAL);
                                                          {if (true) return new IntegerLiteral(t.image);}
      break;
    case IDENTIFIER:
      i = Identifier();
                                                                  {if (true) return i;}
      break;
    case LPAREN:
      jj_consume_token(LPAREN);
      e = Expression();
      jj_consume_token(RPAREN);
                                                          {if (true) return e;}
      break;
    default:
      jj_la1[7] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public IdentifierExp Identifier() throws ParseException {
        Token i;
    i = jj_consume_token(IDENTIFIER);
          {if (true) return new IdentifierExp(i.image);}
    throw new Error("Missing return statement in function");
  }

  /** Generated Token Manager. */
  public JCCExpressionsParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  /** Current token. */
  public Token token;
  /** Next token. */
  public Token jj_nt;
  private int jj_ntk;
  private int jj_gen;
  final private int[] jj_la1 = new int[8];
  static private int[] jj_la1_0;
  static {
      jj_la1_init_0();
   }
   private static void jj_la1_init_0() {
      jj_la1_0 = new int[] {0x100000,0x800,0x10000,0x6000,0x6000,0x8000,0x1a0100,0x180100,};
   }

  /** Constructor with InputStream. */
  public JCCExpressionsParser(java.io.InputStream stream) {
     this(stream, null);
  }
  /** Constructor with InputStream and supplied encoding */
  public JCCExpressionsParser(java.io.InputStream stream, String encoding) {
    try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source = new JCCExpressionsParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 8; i++) jj_la1[i] = -1;
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream) {
     ReInit(stream, null);
  }
  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream, String encoding) {
    try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 8; i++) jj_la1[i] = -1;
  }

  /** Constructor. */
  public JCCExpressionsParser(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new JCCExpressionsParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 8; i++) jj_la1[i] = -1;
  }

  /** Reinitialise. */
  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 8; i++) jj_la1[i] = -1;
  }

  /** Constructor with generated Token Manager. */
  public JCCExpressionsParser(JCCExpressionsParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 8; i++) jj_la1[i] = -1;
  }

  /** Reinitialise. */
  public void ReInit(JCCExpressionsParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 8; i++) jj_la1[i] = -1;
  }

  private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }


/** Get the next Token. */
  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

/** Get the specific Token. */
  final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
  private int[] jj_expentry;
  private int jj_kind = -1;

  /** Generate ParseException. */
  public ParseException generateParseException() {
    jj_expentries.clear();
    boolean[] la1tokens = new boolean[23];
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 8; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 23; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.add(jj_expentry);
      }
    }
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = jj_expentries.get(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  /** Enable tracing. */
  final public void enable_tracing() {
  }

  /** Disable tracing. */
  final public void disable_tracing() {
  }

}
