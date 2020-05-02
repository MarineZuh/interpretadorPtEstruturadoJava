package conversores;

import java.util.HashMap;
import java.util.Map;

import model.TokenType;
import model.VariavelVetor;
import tree.Declaracao.Programa;

public abstract class Conversor {
	
	protected Escritor escritor;
	protected Programa programa;
	private Map<String, VariavelVetor> mapaVariaveisVetor; // salva as variaveis do tipo vetor
	
	public Conversor(Programa programa) {
		this.escritor = new Escritor();
		this.programa = programa;
		this.mapaVariaveisVetor = new HashMap<String, VariavelVetor>();
	}
	
	
	
	protected void addVariavelVetor(String nome, VariavelVetor variavel) {
		this.mapaVariaveisVetor.put(nome, variavel);
	}
	
	protected VariavelVetor getVariavelVetor(String nome) {
		if(this.mapaVariaveisVetor.containsKey(nome)) 
			return this.mapaVariaveisVetor.get(nome);
		return null;
	}
	
	/**
	 * Converte o pseudocodigo para a linguagem da Classe
	 * @return - String com o programa convertido
	 */
	public abstract String converter();
	
	/**
	 * Retorna o tipo equivalente na linguagem alvo
	 * @param tipo - tipo de dado do pseudoCodigo (cadeia, inteiro, real, ...)
	 * @return - String com o tipo equivalente na linguagem alvo
	 */
	protected abstract String tipoVariavel(TokenType tipo);
	
	/**
	 * Retorna o operador lógico equivalente na linguagem alvo
	 * @param op - Operação logica em pseudoCodigo (e, ou, nao)
	 * @return - String com o operador lógico equivalente na linguagem alvo
	 */
	protected abstract String getOperadorLogico(TokenType op);
}
