package scanner;

import static modelos.TiposToken.ASTERISCO;
import static modelos.TiposToken.ATE;
import static modelos.TiposToken.ATRIBUICAO;
import static modelos.TiposToken.BARRA;
import static modelos.TiposToken.CADEIA;
import static modelos.TiposToken.CARACTERE;
import static modelos.TiposToken.CASO;
import static modelos.TiposToken.DE;
import static modelos.TiposToken.DIFERENTE;
import static modelos.TiposToken.DIR_CHAVES;
import static modelos.TiposToken.DIR_COLCHETE;
import static modelos.TiposToken.DIR_PARENTESES;
import static modelos.TiposToken.DOIS_PONTOS;
import static modelos.TiposToken.E;
import static modelos.TiposToken.ENQUANTO;
import static modelos.TiposToken.ENTAO;
import static modelos.TiposToken.EOF;
import static modelos.TiposToken.ESCREVER;
import static modelos.TiposToken.ESQ_CHAVES;
import static modelos.TiposToken.ESQ_COLCHETE;
import static modelos.TiposToken.ESQ_PARENTESES;
import static modelos.TiposToken.FACA;
import static modelos.TiposToken.FALSO;
import static modelos.TiposToken.FIM;
import static modelos.TiposToken.IDENTIFICADOR;
import static modelos.TiposToken.IGUAL;
import static modelos.TiposToken.INICIO;
import static modelos.TiposToken.INTEIRO;
import static modelos.TiposToken.INTERVALO;
import static modelos.TiposToken.LER;
import static modelos.TiposToken.MAIOR_IQUAL;
import static modelos.TiposToken.MAIOR_QUE;
import static modelos.TiposToken.MAIS;
import static modelos.TiposToken.MENOR_IGUAL;
import static modelos.TiposToken.MENOR_QUE;
import static modelos.TiposToken.MENOS;
import static modelos.TiposToken.NAO;
import static modelos.TiposToken.OU;
import static modelos.TiposToken.PARA;
import static modelos.TiposToken.PASSO;
import static modelos.TiposToken.PONTO;
import static modelos.TiposToken.PONTO_VIRGULA;
import static modelos.TiposToken.REAL;
import static modelos.TiposToken.REPITA;
import static modelos.TiposToken.SE;
import static modelos.TiposToken.SENAO;
import static modelos.TiposToken.TIPO_CADEIA;
import static modelos.TiposToken.TIPO_CARACTERE;
import static modelos.TiposToken.TIPO_INTEIRO;
import static modelos.TiposToken.TIPO_LOGICO;
import static modelos.TiposToken.TIPO_MODULO;
import static modelos.TiposToken.TIPO_REAL;
import static modelos.TiposToken.TIPO_VETOR;
import static modelos.TiposToken.VARIAVEIS;
import static modelos.TiposToken.VERDADEIRO;
import static modelos.TiposToken.VIRGULA;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import evento.EventoInterpretador;
import evento.GerenciadorEventos;
import modelos.TiposToken;
import modelos.Token;
import modelos.excecao.ParserError;

/**
 * Análise Léxica
 * 
 * @author Kerlyson
 *
 */
public class Scanner {
  
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private static final Map<String, TiposToken> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("e", E);
    keywords.put("ou", OU);
    keywords.put("nao", NAO);
    keywords.put("verdadeiro", VERDADEIRO);
    keywords.put("falso", FALSO);
    keywords.put("se", SE);
    keywords.put("entao", ENTAO);
    keywords.put("caso", CASO);
    keywords.put("senao", SENAO);
    keywords.put("faca", FACA);
    keywords.put("enquanto", ENQUANTO);
    keywords.put("repita", REPITA);
    keywords.put("para", PARA);
    keywords.put("de", DE);
    keywords.put("ate", ATE);
    keywords.put("passo", PASSO);
    keywords.put("escrever", ESCREVER);
    keywords.put("ler", LER);
    keywords.put("variaveis", VARIAVEIS);
    keywords.put("inicio", INICIO);
    keywords.put("fim", FIM);
    keywords.put("inteiro", TIPO_INTEIRO);
    keywords.put("real", TIPO_REAL);
    keywords.put("logico", TIPO_LOGICO);
    keywords.put("cadeia", TIPO_CADEIA);
    keywords.put("caractere", TIPO_CARACTERE);
    keywords.put("..", INTERVALO);
    keywords.put("modulo", TIPO_MODULO);
    keywords.put("vetor", TIPO_VETOR);
  }

  private int comeco = 0;
  private int atual = 0;
  private int linha = 1;
  private GerenciadorEventos gerenciadorEventos;

  public Scanner(String source, GerenciadorEventos eventos) {
    this.gerenciadorEventos = eventos;
    this.source = source;
  }

  public List<Token> scanTokens() {
    while (!isFinal()) {
      // comeco do proximo lexeme
      comeco = atual;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, linha));
    return tokens;
  }

  private boolean isFinal() {
    return atual >= source.length();
  }

  private void scanToken() {
    char c = avancar();
    switch (c) {
      case '(':
        addToken(ESQ_PARENTESES);
        break;
      case ')':
        addToken(DIR_PARENTESES);
        break;
      case '[':
        addToken(ESQ_COLCHETE);
        break;
      case ']':
        addToken(DIR_COLCHETE);
        break;
      case '{':
        addToken(ESQ_CHAVES);
        break;
      case '}':
        addToken(DIR_CHAVES);
        break;
      case ',':
        addToken(VIRGULA);
        break;
      case '.':
        if (comparar('.')) {
          addToken(INTERVALO);
        } else {
          addToken(PONTO);
        }
        break;
      case '-':
        addToken(MENOS);
        break;
      case '+':
        addToken(MAIS);
        break;
      case ';':
        addToken(PONTO_VIRGULA);
        break;
      case '*':
        addToken(ASTERISCO);
        break;
      case ':':
        addToken(DOIS_PONTOS);
        break;

      case '=':
        addToken(IGUAL);
        break;
      case '<':
        if (comparar('=')) {
          addToken(MENOR_IGUAL);
        } else if (comparar('-')) {
          addToken(ATRIBUICAO);
        } else if (comparar('>')) {
          addToken(DIFERENTE);
        } else {
          addToken(MENOR_QUE);
        }

        break;
      case '>':
        addToken(comparar('=') ? MAIOR_IQUAL : MAIOR_QUE);
        break;
      // comentario:
      case '/':
        if (comparar('/')) {
          // comentario termina no fim da linha.
          while (checar() != '\n' && !isFinal())
            avancar();
        } else {
          addToken(BARRA);
        }
        break;
      // inuteis:
      case ' ':
      case '\r':
      case '\t':
        // ignora espaco em branco
        break;
      case '\n':
        linha++;
        break;
      case '"':
        cadeia();
        break;
      default:
        if (isNumerico(c)) {
          numero();
        } else if (isLetra(c)) {
          identificador();
        } else {
          this.gerenciadorEventos.notificar(EventoInterpretador.ERRO_PARSE,
              new ParserError(linha, "caractere '" + c + "' não identificado."));
        }
        break;
    }
  }

  private boolean isNumerico(char c) {
    return c >= '0' && c <= '9';
  }

  private void identificador() {
    while (isLetraOuNumero(checar()))
      avancar();
    String text = source.substring(comeco, atual);

    TiposToken type = keywords.get(text);
    if (type == null)
      type = IDENTIFICADOR;
    addToken(type);
  }

  private boolean isLetra(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
  }

  private boolean isLetraOuNumero(char c) {
    return isLetra(c) || isNumerico(c);
  }

  private void numero() {
    while (isNumerico(checar()))
      avancar();

    // Procura pela parte fracionada
    if (checar() == '.' && isNumerico(checkProximo())) {
      // consome "."
      avancar();

      while (isNumerico(checar()))
        avancar();
    }
    String valorString = source.substring(comeco, atual);
    if (valorString.contains(".")) {
      addToken(REAL, Double.parseDouble(valorString));
    } else {
      addToken(INTEIRO, Integer.parseInt(valorString));
    }
  }

  private char checkProximo() {
    if (atual + 1 >= source.length())
      return '\0';
    return source.charAt(atual + 1);
  }

  private void cadeia() {
    // " ou ' ??
    while (checar() != '"' && !isFinal()) {
      if (checar() == '\n')
        linha++; // suporte para cadeia multi-line
      avancar();
    }

    // Unterminated string.
    if (isFinal()) {
      this.gerenciadorEventos.notificar(EventoInterpretador.ERRO_PARSE,
          new ParserError(linha, "cadeia não determinada."));
      return;
    }

    // a ultima aspa .
    avancar();

    // remove as aspas.
    String value = source.substring(comeco + 1, atual - 1);
    if (value.length() == 1) {
      addToken(CARACTERE, value.charAt(0));
    } else {
      addToken(CADEIA, value);
    }
  }

  private char checar() {
    if (isFinal())
      return '\0';
    return source.charAt(atual);
  }

  private boolean comparar(char esperado) {
    if (checar() != esperado)
      return false;

    atual++;
    return true;
  }

  private char avancar() {
    atual++;
    return source.charAt(atual - 1);
  }

  private void addToken(TiposToken type) {
    addToken(type, null);
  }

  private void addToken(TiposToken type, Object literal) {
    String text = source.substring(comeco, atual);
    tokens.add(new Token(type, text, literal, linha));
  }
}
