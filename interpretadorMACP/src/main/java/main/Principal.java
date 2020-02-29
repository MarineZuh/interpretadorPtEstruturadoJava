package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import interpreter.Interpretador;
import interpreter.JavaConversorTeste;
import model.Token;
import model.TokenType;
import parser.Parser;
import parser.RuntimeError;
import scanner.Scanner;
import tree.Declaracao;
import tree.Expressao;
import util.ImpressoraAST;

public class Principal {
	static boolean hadError = false;
	static boolean hadRuntimeError = false;
	static final InputStreamReader input = new InputStreamReader(System.in);
	static final BufferedReader reader = new BufferedReader(input);
	private static final Interpretador interpreter = new Interpretador(reader);

	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			System.out.println("Usage: Principal [script]");
			System.exit(64);
		} else if (args.length == 1) {
			System.out.println("Usage: Principal [file]");
			runFile(args[0]);
		} else {
			System.out.println("Usage: Principal [prompt]");
			runPrompt();
		}
	}
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));
		// Indicate an error in the exit code.
		if (hadError)
			System.exit(65);
		if (hadRuntimeError)
			System.exit(70);
	}
	private static void runPrompt() throws IOException {

		for (;;) {
			System.out.print("> ");
			run(reader.readLine());
			hadError = false;
		}
	}
	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();

		// For now, just print the tokens.
		// for (Token token : tokens) {
		// System.out.println(token);
		// }
		Parser parser = new Parser(tokens);
		Declaracao.Programa programa = parser.parse();
		// Stop if there was a syntax error.
		if (hadError)
			return;
//		System.out.println(declaracoes.size());
//		new ImpressoraAST().print(declaracoes);// imprime arvore
		JavaConversorTeste t = new JavaConversorTeste();
		System.out.println(t.converter(programa));
		interpreter.interpret(programa);

	}
	public static void error(int line, String message) {
		report(line, "", message);
	}

	private static void report(int line, String where, String message) {
		System.err
				.println("[linha " + line + "] Erro" + where + ": " + message);
		hadError = true;
	}
	public static void error(Token token, String message) {
		if (token.type == TokenType.EOF) {
			report(token.line, " no fim", message);
		} else {
			report(token.line, " em '" + token.lexeme + "'", message);
		}
	}
	public static void runtimeError(RuntimeError error) {
		System.err.println(
				error.getMessage() + "\n[linha " + error.token.line + "]");
		hadRuntimeError = true;
	}
}
