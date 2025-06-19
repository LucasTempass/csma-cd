import java.util.*;

public class Host {
	private final static int MAX_COLISOES = 16;

	private Deque<Double> pacotes;
	private final double posicaoBarramento;
	private int colisoes;

	public Host(double posicaoBarramento, double taxaDePacotes, double duracao) {
		this.posicaoBarramento = posicaoBarramento;
		this.pacotes = new ArrayDeque<>(gerarPacotes(taxaDePacotes, duracao));
		this.colisoes = 0;
	}

	public void onColisao(double larguraDeBanda) {
		colisoes++;

		if (colisoes > MAX_COLISOES) {
			removerPacote();
			return;
		}

		double tempoBackoff = pacotes.peek() + getTempoBackoffExponencial(larguraDeBanda, colisoes);
		List<Double> pacotesAtualizados = new ArrayList<>();
		for (double tempo : pacotes) {
			pacotesAtualizados.add(Math.max(tempoBackoff, tempo));
		}
		pacotes = new ArrayDeque<>(pacotesAtualizados);
	}

	public void onSucesso() {
		removerPacote();
	}

	public List<Double> gerarPacotes(double taxaDePacotes, double duracao) {
		List<Double> pacotes = new ArrayList<>();
		double tempoAtual = 0;
		while (tempoAtual <= duracao) {
			tempoAtual += getValorAleatorioConformeTaxa(taxaDePacotes);
			pacotes.add(tempoAtual);
		}
		Collections.sort(pacotes);
		return pacotes;
	}

	private double getValorAleatorioConformeTaxa(double taxa) {
		return -Math.log(1 - (1 - Math.random())) / taxa;
	}

	private double getTempoBackoffExponencial(double larguraDeBanda, int colisoes) {
		double quantidadeSlots = Math.pow(2, colisoes);
		// intervalo de [0, N[
		double slot = Math.random() * quantidadeSlots;
		// tamanho mínimo do frame de 512 bits (64 bytes)
		return slot * 512 / larguraDeBanda;
	}

	public void removerPacote() {
		pacotes.poll();
		colisoes = 0;
	}

	public double getPosicaoBarramento() {
		return posicaoBarramento;
	}

	public Deque<Double> getPacotes() {
		return pacotes;
	}

	public void setPacotes(Deque<Double> pacotes) {
		this.pacotes = pacotes;
	}

	public int getColisoes() {
		return colisoes;
	}

}
