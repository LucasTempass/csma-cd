import java.math.BigDecimal;
import java.util.*;

import static java.lang.Math.max;
import static java.math.BigDecimal.valueOf;
import static java.math.MathContext.DECIMAL128;

public class Host {
	private final static int MAX_COLISOES = 16;

	private final Queue<Pacote> pacotes;
	private final double posicaoBarramento;
	private int colisoes;

	// métricas
	private final List<Pacote> pacotesTransmitidos;
	private final int quantidadePacotes;
	private int quantidadeFalhas;
	private int quantidadeColisoes;
	private final int id;

	public Host(double posicaoBarramento, double taxaDePacotes, double duracao, int id) {
		this.posicaoBarramento = posicaoBarramento;
		this.pacotes = new LinkedList<>(gerarPacotes(taxaDePacotes, duracao));
		this.quantidadePacotes = pacotes.size();
		this.colisoes = 0;
		this.pacotesTransmitidos = new ArrayList<>();
		this.id = id;
	}

	public void onColisao(double larguraDeBanda, BigDecimal tempo) {
		quantidadeColisoes++;

		colisoes++;

		if (colisoes > MAX_COLISOES) {
			quantidadeFalhas++;
			removerPacote();
			return;
		}

		Pacote pacote = pacotes.peek();

		if (pacote == null) return;

		BigDecimal tempoBackoffExponencial = getTempoBackoffExponencial(larguraDeBanda, colisoes);
		BigDecimal tempoBackoff = tempo.add(tempoBackoffExponencial);

		// atrasa envio dos pacotes previstos, imitando um comportamento de buffer
		for (Pacote p : pacotes) {
			if (tempoBackoff.compareTo(p.getTempo()) < 0) break;
			p.setTempo(tempoBackoff);
		}
	}

	public void onSucesso(BigDecimal tempoInicioTransmissao, BigDecimal tempoDeConclusaoTransmissao) {
		Pacote pacote = removerPacote();
		pacote.setTempoConclusao(tempoDeConclusaoTransmissao);
		pacote.setTempoInicio(tempoInicioTransmissao);
		pacotesTransmitidos.add(pacote);
	}

	public List<Pacote> gerarPacotes(double taxaDePacotes, double duracao) {
		List<Pacote> pacotes = new ArrayList<>();
		double tempoAtual = 0;
		BigDecimal tempoTransmissao = valueOf(Simulacao.BITS_POR_PACOTE).divide(valueOf(1e7), DECIMAL128);
		int id = 1;

		while (tempoAtual <= duracao) {
			tempoAtual += max(tempoTransmissao.doubleValue(), getValorAleatorioConformeTaxa(taxaDePacotes));
			pacotes.add(new Pacote(tempoAtual, this, id));
			id++;
		}

		Collections.sort(pacotes);

		return pacotes;
	}

	private double getValorAleatorioConformeTaxa(double taxa) {
		return -Math.log(1 - (1 - Math.random())) / taxa;
	}

	private BigDecimal getTempoBackoffExponencial(double larguraDeBanda, int colisoes) {
		double quantidadeSlots = Math.pow(2, colisoes);
		// intervalo de [0, N[
		double slot = Math.random() * quantidadeSlots;
		// tamanho mínimo do frame de 512 bits (64 bytes)
		return valueOf(slot * 512).divide(valueOf(larguraDeBanda), DECIMAL128);
	}

	private Pacote removerPacote() {
		colisoes = 0;
		return pacotes.poll();
	}

	public double getPosicaoBarramento() {
		return posicaoBarramento;
	}

	public Queue<Pacote> getPacotes() {
		return pacotes;
	}

	public int getQuantidadePacotes() {
		return quantidadePacotes;
	}

	public int getQuantidadeColisoes() {
		return quantidadeColisoes;
	}

	public int getPacotesPerdidos() {
		return quantidadeFalhas;
	}

	public double getTempoMedioDelay() {
		return pacotesTransmitidos.stream().mapToDouble(Pacote::getDelay).average().orElse(0);
	}

	public int getId() {
		return id;
	}

	public List<Pacote> getPacotesTransmitidos() {
		return pacotesTransmitidos;
	}

}
