import java.util.*;

public class Host {
	private final static int MAX_COLISOES = 16;

	private final Queue<Pacote> pacotes;
	private final double posicaoBarramento;
	private int colisoes;

	public Host(double posicaoBarramento, double taxaDePacotes, double duracao) {
		this.posicaoBarramento = posicaoBarramento;
		this.pacotes = new LinkedList<>(gerarPacotes(taxaDePacotes, duracao));
		this.colisoes = 0;
	}

	public void onColisao(double larguraDeBanda) {
		colisoes++;

		if (colisoes > MAX_COLISOES) {
			removerPacote();
			return;
		}

		Pacote pacote = pacotes.peek();

		if (pacote == null) return;

		double tempoBackoff = pacote.getTempo() + getTempoBackoffExponencial(larguraDeBanda, colisoes);

		// atrasa envio dos pacotes previstos, imitando um comportamento de buffer
		for (Pacote p : pacotes) {
			if (tempoBackoff < p.getTempo()) break;
			p.setTempo(tempoBackoff);
		}
	}

	public void onSucesso() {
		removerPacote();
	}

	public List<Pacote> gerarPacotes(double taxaDePacotes, double duracao) {
		List<Pacote> pacotes = new ArrayList<>();
		double tempoAtual = 0;

		while (tempoAtual <= duracao) {
			tempoAtual += getValorAleatorioConformeTaxa(taxaDePacotes);
			pacotes.add(new Pacote(tempoAtual));
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

	public Queue<Pacote> getPacotes() {
		return pacotes;
	}

	public int getColisoes() {
		return colisoes;
	}

}
