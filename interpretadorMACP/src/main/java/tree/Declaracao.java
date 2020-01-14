package tree;

import java.util.List;

import model.Token;

public abstract class Declaracao {
public interface Visitor<R> {
public R visitBlocoDeclaracao(Bloco declaracao);
public R visitExpressaoDeclaracao(Expressao declaracao);
public R visitPrintDeclaracao(Print declaracao);
public R visitSeDeclaracao(Se declaracao);
public R visitLerDeclaracao(Ler declaracao);
public R visitVarDeclaracao(Var declaracao);
public R visitEnquantoDeclaracao(Enquanto declaracao);
  }
public static class Bloco extends Declaracao {
    public Bloco(List<Declaracao> declaracoes) {
      this.declaracoes = declaracoes;
    }

    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlocoDeclaracao(this);
    }

    public final List<Declaracao> declaracoes;
  }
public static class Expressao extends Declaracao {
    public Expressao(tree.Expressao expressao) {
      this.expressao = expressao;
    }

    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressaoDeclaracao(this);
    }

    public final tree.Expressao expressao;
  }
public static class Print extends Declaracao {
    public Print(tree.Expressao expressao) {
      this.expressao = expressao;
    }

    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintDeclaracao(this);
    }

    public final tree.Expressao expressao;
  }
public static class Se extends Declaracao {
    public Se(tree.Expressao condicao, Bloco entaoBloco, Bloco senaoBloco) {
      this.condicao = condicao;
      this.entaoBloco = entaoBloco;
      this.senaoBloco = senaoBloco;
    }

    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitSeDeclaracao(this);
    }

    public final tree.Expressao condicao;
    public final Bloco entaoBloco;
    public final Bloco senaoBloco;
  }
public static class Ler extends Declaracao {
    public Ler(tree.Expressao expressao) {
      this.expressao = expressao;
    }

    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitLerDeclaracao(this);
    }

    public final tree.Expressao expressao;
  }
public static class Var extends Declaracao {
    public Var(Token nome, tree.Expressao expressao) {
      this.nome = nome;
      this.expressao = expressao;
    }

    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarDeclaracao(this);
    }

    public final Token nome;
    public final tree.Expressao expressao;
  }
public static class Enquanto extends Declaracao {
    public Enquanto(tree.Expressao condicao, Bloco corpo) {
      this.condicao = condicao;
      this.corpo = corpo;
    }

    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitEnquantoDeclaracao(this);
    }

    public final tree.Expressao condicao;
    public final Bloco corpo;
  }

  public abstract <R> R accept(Visitor<R> visitor);
}
