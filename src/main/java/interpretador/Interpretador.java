package interpretador;

import static modelos.TiposToken.ASTERISCO;
import static modelos.TiposToken.BARRA;
import static modelos.TiposToken.MAIS;
import static modelos.TiposToken.MENOS;
import static modelos.TiposToken.OU;

import java.util.List;
import java.util.Map;

import debug.GerenciadorEventos;
import debug.TiposEvento;
import main.Principal;
import modelos.LeitorEntradaConsole;
import modelos.RuntimeError;
import modelos.TiposToken;
import modelos.Token;
import modelos.VariavelVetor;
import tree.Declaracao;
import tree.Declaracao.Bloco;
import tree.Declaracao.ChamadaModulo;
import tree.Declaracao.Enquanto;
import tree.Declaracao.Escreva;
import tree.Declaracao.Ler;
import tree.Declaracao.Modulo;
import tree.Declaracao.Para;
import tree.Declaracao.Programa;
import tree.Declaracao.Repita;
import tree.Declaracao.Se;
import tree.Declaracao.Var;
import tree.Declaracao.VarDeclaracoes;
import tree.Declaracao.VariavelArray;
import tree.Expressao;
import tree.Expressao.Atribuicao;
import tree.Expressao.AtribuicaoArray;
import tree.Expressao.Binario;
import tree.Expressao.ExpParentizada;
import tree.Expressao.Grupo;
import tree.Expressao.Literal;
import tree.Expressao.Logico;
import tree.Expressao.Unario;
import tree.Expressao.Variavel;

/**
 * Interpreta e gera a saída do pseudocodigo representado no Declaração.Programa
 * passado
 * 
 * @author Kerlyson
 *
 */
public class Interpretador implements Expressao.Visitor<Object>, Declaracao.Visitor<Void> {

    private Ambiente environment = new Ambiente();
    private GerenciadorEventos gerenciadorEventos;
    private Thread thread; // thread para executar o processo de interpretação
    private boolean parada, terminada; // FLAGS com o estado da execução da thread
    private LeitorEntradaConsole entradaConsole = new LeitorEntradaConsole(this);

    public Interpretador(GerenciadorEventos ge) {
	this.gerenciadorEventos = ge;
    }

    public Map<String, Object> getAmbienteSnapshot() {
	return this.environment.criarSnapshot();
    }

    public void interpretar(Declaracao.Programa programa) {
	this.parada = false;
	this.terminada = false;
	thread = new Thread(new Runnable() {
	    @Override
	    public void run() {
		long startTime = System.nanoTime();
		try {
		    visitProgramaDeclaracao(programa);
		} catch (RuntimeError error) {
		    gerenciadorEventos.notificar(TiposEvento.ERRO_RUNTIME, error);

		} catch (StackOverflowError e) {
		    e.printStackTrace();
		}
		long elapsedTime = System.nanoTime() - startTime;
		gerenciadorEventos.notificar(TiposEvento.INTERPRETACAO_CONCLUIDA, elapsedTime);
	    }
	});
	thread.start();

    }

    // THREAD CONTROLE

    public void suspender() {
	this.parada = true;
	try {
	    synchronized (thread) {

		thread.wait();

	    }
	} catch (InterruptedException e) {
	    thread.interrupt();
	    e.printStackTrace();
	}

    }

    public void resumir() {
	this.parada = false;
	synchronized (thread) {
	    thread.notify();
	}
    }

    public void terminar() {
	this.terminada = true;
	this.parada = true;
	synchronized (thread) {
	    thread.stop();

	}
    }

    public boolean isRodando() {
	return this.parada || this.terminada;
    }

    public boolean isPausada() {
	return this.parada;
    }

    public boolean isTerminada() {
	return this.terminada;
    }

    // AUXILIARES:

    private void execute(Declaracao declaracao) {
	gerenciadorEventos.notificar(TiposEvento.NODE_DEBUG, declaracao);
	declaracao.accept(this);
    }

    private Object evaluate(Expressao expressao) {
	gerenciadorEventos.notificar(TiposEvento.NODE_DEBUG, expressao);
	return expressao.accept(this);
    }

    private String stringify(Object object) {
	if (object == null)
	    return "nulo";

	// Java add ".0" em intergers convertidos para doubles.
	if (object instanceof Double) {
	    String text = object.toString();
	    if (text.endsWith(".0")) {
		text = text.substring(0, text.length() - 2);
	    }
	    return text;
	}

	if (object instanceof Boolean) {
	    if ((boolean) object) {
		return "verdadeiro";
	    } else {
		return "falso";
	    }
	}

	return object.toString();
    }

    private boolean isLogico(Object object) {
	if (object == null)
	    return false;
	if (object instanceof Boolean)
	    return (boolean) object;
	return true;
    }

    private boolean isIgual(Object a, Object b) {
	if (a == null && b == null)
	    return true;
	if (a == null)
	    return false;

	return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
	if (operand instanceof Double)
	    return;
	if (operand instanceof Integer)
	    return;
	throw new RuntimeError(operator, "Operador deve ser um número.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
	boolean leftDoubleInt = left instanceof Double || left instanceof Integer;
	boolean rightDoubleInt = right instanceof Double || right instanceof Integer;
	if (leftDoubleInt && rightDoubleInt)
	    return;

	throw new RuntimeError(operator, "Operadores devem ser números.");
    }

    private Object retornaValorNumericoTipoCorreto(TiposToken op, Object left, Object right) {
	if (left instanceof Integer && right instanceof Integer) {
	    switch (op) {
	    case MAIS:
		return (int) left + (int) right;
	    case MENOS:

		return (int) left - (int) right;
	    case ASTERISCO:

		return (int) left * (int) right;
	    case BARRA:

		return (int) left / (int) right;

	    }
	}
	double l = (left instanceof Integer) ? ((int) left) / 1.0 : (double) left;
	double r = (right instanceof Integer) ? ((int) right) / 1.0 : (double) right;
	switch (op) {
	case MAIS:
	    return l + r;
	case MENOS:

	    return l - r;
	case ASTERISCO:

	    return l * r;
	case BARRA:

	    return l / r;
	}
	return null;

    }

    private Double toDouble(Object valor) {
	if (valor instanceof Integer) {
	    return Double.valueOf((int) valor);
	}
	return (double) valor;
    }

    // TODO: analisar remoção da variavel enviroment
    // nao usada pois existe apenas o escopo global
    public void executeBlock(List<Declaracao> statements, Ambiente environment) {

//		Environment previous = this.environment; // uselles?
	try {
//			this.environment = environment;// uselles?

	    for (Declaracao statement : statements) {
		execute(statement);
	    }
	} finally {
//			this.environment = previous;uselles?
	}
    }

    // NODES:

    @Override
    public Object visitBinarioExpressao(Binario expressao) {
	Object esquerda = evaluate(expressao.esquerda);
	Object direita = evaluate(expressao.direita);

	switch (expressao.operador.type) {
	case MENOS:
	    checkNumberOperands(expressao.operador, esquerda, direita);
	    return retornaValorNumericoTipoCorreto(MENOS, esquerda, direita);
	case BARRA:
	    checkNumberOperands(expressao.operador, esquerda, direita);
	    return retornaValorNumericoTipoCorreto(BARRA, esquerda, direita);
	case ASTERISCO:
	    checkNumberOperands(expressao.operador, esquerda, direita);
	    return retornaValorNumericoTipoCorreto(ASTERISCO, esquerda, direita);
	case MAIS:
	    if (esquerda instanceof String && direita instanceof String) {
		return (String) esquerda + (String) direita;
	    }
	    if ((esquerda instanceof Double || esquerda instanceof Integer)
		    && (direita instanceof Double || direita instanceof Integer)) {
		return retornaValorNumericoTipoCorreto(MAIS, esquerda, direita);
	    }
	    throw new RuntimeError(
				   expressao.operador,
				   "Operadores devem ser apenas números ou apenas cadeia de caracteres.");
	case MAIOR_QUE:
	    checkNumberOperands(expressao.operador, esquerda, direita);
	    return toDouble(esquerda) > toDouble(direita);
	case MAIOR_IQUAL:
	    checkNumberOperands(expressao.operador, esquerda, direita);
	    return toDouble(esquerda) >= toDouble(direita);
	case MENOR_QUE:
	    checkNumberOperands(expressao.operador, esquerda, direita);
	    return toDouble(esquerda) < toDouble(direita);
	case MENOR_IGUAL:
	    checkNumberOperands(expressao.operador, esquerda, direita);

	    return toDouble(esquerda) <= toDouble(direita);
	case DIFERENTE:
	    return !isIgual(esquerda, direita);
	case IGUAL:
	    return isIgual(esquerda, direita);
	default:
	    break;
	}
	return null;
    }

    @Override
    public Object visitGrupoExpressao(Grupo expressao) {
	return evaluate(expressao.expressao);
    }

    @Override
    public Object visitLiteralExpressao(Literal expressao) {
	return expressao.valor;
    }

    @Override
    public Object visitUnarioExpressao(Unario expressao) {
	Object direita = evaluate(expressao.direira);

	switch (expressao.operador.type) {
	case NAO:
	    return !isLogico(direita);
	case MENOS:
	    checkNumberOperand(expressao.operador, direita);
	    if (direita instanceof Integer) {
		return -(int) direita;
	    }
	    if (direita instanceof Double) {
		return -(double) direita;
	    }
	    break;

	default:
	    break;
	}
	return null;
    }

    @Override
    public Void visitExpressaoDeclaracao(tree.Declaracao.Expressao declaracao) {
	evaluate(declaracao.expressao);
	return null;
    }

    @Override
    public Void visitEscrevaDeclaracao(Escreva declaracao) {
	StringBuilder output = new StringBuilder();

	for (tree.Expressao expressao : declaracao.expressoes) {
	    Object valor = evaluate(expressao);
	    if (valor instanceof VariavelVetor) {
		Object v[] = ((VariavelVetor) valor).getValores();
		output.append("[");
		for (int x = 0; x < v.length; x++) {
		    output.append(stringify(v[x]));
		    if (x < v.length - 1) {
			output.append(", ");
		    }
		}
		output.append("]");
		return null;
	    }
	    output.append(stringify(valor));
	}
	gerenciadorEventos.notificar(TiposEvento.ESCREVER_EVENTO, output.toString());
//		System.out.println(output.toString());// imprime acoes no terminal
	return null;
    }

    @Override
    public Void visitLerDeclaracao(Ler declaracao) {

	this.gerenciadorEventos.notificar(TiposEvento.LER_EVENTO, this.entradaConsole);

	this.suspender();// espera o valor ser setado para continuar

	String valor = this.entradaConsole.getValor();
	this.entradaConsole.reset();
	System.err.println("lido: " + valor);// imprime acoes no terminal

	Expressao atribuicao = declaracao.atribuicao;
	if (atribuicao instanceof Expressao.Atribuicao) {
	    Token nome = ((Expressao.Atribuicao) atribuicao).nome;
	    environment.setVariavelPorFuncaoLer(nome, valor, null);
	}
	if (atribuicao instanceof Expressao.AtribuicaoArray) {
	    Token nome = ((Expressao.AtribuicaoArray) atribuicao).nome;
	    Object index = evaluate(((Expressao.AtribuicaoArray) atribuicao).index);
	    environment.setVariavelPorFuncaoLer(nome, valor, index);
	}

	return null;
    }

    @Override
    public Void visitVarDeclaracao(Var declaracao) {
	environment.definirVariavel(declaracao.nome, declaracao.tipo);
	return null;
    }

    @Override
    public Object visitVariavelExpressao(Variavel expressao) {
	return environment.getValorVariavel(expressao.nome);
    }

    @Override
    public Object visitAtribuicaoExpressao(Atribuicao expressao) {
	Object value = evaluate(expressao.valor);
	if (value instanceof modelos.Modulo) {
	    throw new RuntimeError(expressao.nome, "Módulo não pode ser atribuido para variável");
	}
	environment.setValorVariavel(expressao.nome, value);
	return value;
    }

    @Override
    public Void visitBlocoDeclaracao(Bloco declaracao) {

	executeBlock(declaracao.declaracoes, null);
	return null;
    }

    @Override
    public Void visitSeDeclaracao(Se declaracao) {
	if (isLogico(evaluate(declaracao.condicao))) {
	    execute(declaracao.entaoBloco);
	} else if (declaracao.senaoBloco != null) {
	    execute(declaracao.senaoBloco);
	}
	return null;
    }

    @Override
    public Object visitLogicoExpressao(Logico expressao) {
	Object esquerda = evaluate(expressao.esquerda);

	if (expressao.operador.type == OU) {
	    if (isLogico(esquerda)) {
		return esquerda;
	    }
	} else {
	    if (!isLogico(esquerda))
		return esquerda;
	}
	return evaluate(expressao.direita);
    }

    @Override
    public Void visitEnquantoDeclaracao(Enquanto declaracao) {
	while (isLogico(evaluate(declaracao.condicao))) {
	    execute(declaracao.corpo);
	}
	return null;
    }

    @Override
    public Void visitParaDeclaracao(Para declaracao) {

	evaluate(declaracao.atribuicao);
	// GAMBIARRA - se o incremento for negativo
	// inverte o sinal da condição de <= para >=
	Expressao.Binario condicao = (Expressao.Binario) declaracao.condicao;
	Expressao.Atribuicao incremento = (Expressao.Atribuicao) declaracao.incremento;
	Expressao.Binario operacaoIncremento = (Expressao.Binario) incremento.valor;
	Expressao exValorIncremento = operacaoIncremento.direita;
	Object valorIncremento = evaluate(exValorIncremento);

	if (valorIncremento instanceof Integer) {

	    if ((int) valorIncremento < 0) {
		condicao = new Binario(
				       condicao.operador.line,
				       condicao.esquerda,
				       new Token(TiposToken.MAIOR_IQUAL, ">=", null, condicao.operador.line),
				       condicao.direita);

	    }
	} else if (valorIncremento instanceof Double) {
	    if ((double) valorIncremento < 0) {
		condicao = new Binario(
				       condicao.operador.line,
				       condicao.esquerda,
				       new Token(TiposToken.MAIOR_IQUAL, ">=", null, condicao.operador.line),
				       condicao.direita);
	    }
	}
	while (isLogico(evaluate(condicao))) {
	    execute(declaracao.facaBloco);
	    evaluate(declaracao.incremento);
	}
	return null;
    }

    @Override
    public Void visitProgramaDeclaracao(Programa declaracao) {

	for (Declaracao variaveis : declaracao.variaveis) {
	    execute(variaveis);
	}

	for (Declaracao modulo : declaracao.modulos) {
	    execute(modulo);
	}

	for (Declaracao corpo : declaracao.corpo) {
	    execute(corpo);
	}

	return null;
    }

    @Override
    public Void visitVariavelArrayDeclaracao(VariavelArray declaracao) {
	int intervaloI = (int) evaluate(declaracao.intervaloI);
	int intervaloF = (int) evaluate(declaracao.intervaloF);

	if (intervaloI > intervaloF) {
	    throw new RuntimeError(declaracao.nome, "Intervalo inicial não pode ser maior que o intervalo final");
	}
	if (declaracao.tipo.type == TiposToken.TIPO_MODULO) {
	    throw new RuntimeError(declaracao.nome, "vetor não pode ter o tipo modulo.");
	}
	environment
		   .definirVariavelVetor(
					 declaracao.nome,
					 new VariavelVetor(declaracao.tipo.type, intervaloI, intervaloF));
	return null;
    }

    @Override
    public Object visitAtribuicaoArrayExpressao(AtribuicaoArray expressao) {
	Object index = evaluate(expressao.index);
	Object valor = evaluate(expressao.valor);

	this.environment.setValorVariavelVetor(expressao.nome, index, valor);

	return null;
    }

    @Override
    public Object visitVariavelArrayExpressao(Expressao.VariavelArray expressao) {
	VariavelVetor variavel = (VariavelVetor) environment.getValorVariavel(expressao.nome);
	Object index = evaluate(expressao.index);
	if (index == null) {
	    throw new RuntimeError(expressao.nome, "Index informado não pode ser nulo.");
	}
	if (!(index instanceof Integer)) {
	    throw new RuntimeError(expressao.nome, "Index informado não pode ser resolvido.");
	}
	if ((int) index < 0 || (int) index > variavel.getIntervaloF() || (int) index < variavel.getIntervaloI()) {
	    throw new RuntimeError(expressao.nome, "Index informado não encontrado");
	}
	return variavel.getValorNoIndex((int) index);
    }

    @Override
    public Object visitExpParentizadaExpressao(ExpParentizada expressao) {
	return this.evaluate(expressao.grupo);
    }

    @Override
    public Void visitRepitaDeclaracao(Repita declaracao) {
	do {
	    execute(declaracao.corpo);
	} while (isLogico(evaluate(declaracao.condicao)));
	return null;
    }

    @Override
    public Void visitModuloDeclaracao(Modulo declaracao) {
	modelos.Modulo modulo = new modelos.Modulo(declaracao);
	this.environment.setValorVariavel(declaracao.nome, modulo);
	return null;
    }

    @Override
    public Void visitChamadaModuloDeclaracao(ChamadaModulo declaracao) {
	modelos.Modulo modulo = (modelos.Modulo) environment.getValorVariavel(declaracao.identificador);
	modulo.chamar(this, null);
	return null;
    }

    @Override
    public Void visitVarDeclaracoesDeclaracao(VarDeclaracoes declaracao) {
	for (Declaracao variavel : declaracao.variaveis) {
	    execute(variavel);
	}
	return null;
    }

}
