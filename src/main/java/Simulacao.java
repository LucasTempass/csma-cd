import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.pow;
import static java.math.BigDecimal.valueOf;
import static java.math.MathContext.DECIMAL128;

public class Simulacao {

	private static final int COMPRIMENTO_BARRAMENTO = 100;
	private static final BigDecimal VELOCIDADE_DA_LUZ = new BigDecimal("3E8");
	private static final BigDecimal VELOCIDADE_DE_PROPAGACAO_DO_MEIO = VELOCIDADE_DA_LUZ.multiply(new BigDecimal("0.66"));
	private static final MathContext PRECISAO = DECIMAL128;
	// 64 bytes + 8 bits de Preâmbulo
	public static final int BITS_POR_PACOTE = 520;
	private static final double VAZAO = 1e7;

	// tempo que um símbolo demora a chegar até o host
	private static final BigDecimal TEMPO_PROPAGACAO = valueOf(COMPRIMENTO_BARRAMENTO).divide(VELOCIDADE_DE_PROPAGACAO_DO_MEIO, PRECISAO);
	// tempo necessário para transmitir o frame por completo
	private static final BigDecimal TEMPO_TRANSMISSAO = valueOf(Simulacao.BITS_POR_PACOTE).divide(valueOf(VAZAO), PRECISAO);
	// 8 bytes de interframe
	private static final BigDecimal TEMPO_INTERFRAME = valueOf(96).divide(valueOf(VAZAO), PRECISAO);
	// 4 bytes de jam
	private static final BigDecimal TEMPO_JAM = valueOf(32).divide(valueOf(VAZAO), PRECISAO);

	private static List<Host> gerarHosts(double taxaDePacotes, int duracao) {
		List<Host> hosts = new ArrayList<>();
		double distanciaEntreHosts = COMPRIMENTO_BARRAMENTO / pow(2, 0);
		for (int i = 0; i < 2; i++) {
			hosts.add(new Host(i * distanciaEntreHosts, taxaDePacotes, duracao, i + 1));
		}
		return hosts;
	}

	private static List<Host> simularCsmaCd(double taxaDePacotes, int duracao) {
		List<Host> hosts = gerarHosts(taxaDePacotes, duracao);

		while (true) {
			Host hostOrigem = getHostProximoPacote(hosts);

			// não há mais pacotes a serem transmitidos
			if (hostOrigem == null) break;

			Pacote proximoPacote = hostOrigem.getPacotes().peek();

			// não há mais pacotes a serem transmitidos
			if (proximoPacote == null) break;

			// adiciona o tempo de espera mínimo especificado para Ethernet
			BigDecimal tempoInicioTransmissao = proximoPacote.getTempo().add(TEMPO_INTERFRAME);
			BigDecimal tempoDeConclusaoTransmissao = tempoInicioTransmissao.add(TEMPO_PROPAGACAO).add(TEMPO_TRANSMISSAO);

			Host hostDestino = getOutroHost(hosts, hostOrigem);

			BigDecimal tempoColisao = validarColisao(hostDestino, tempoInicioTransmissao);

			if (tempoColisao == null) {
				hostOrigem.onSucesso(tempoInicioTransmissao, tempoDeConclusaoTransmissao);
				// limita sobreposição de pacotes do mesmo host
				BigDecimal tempoMinimoProximosPacotes = tempoInicioTransmissao.add(TEMPO_TRANSMISSAO);
				atualizarPacote(hostOrigem, tempoMinimoProximosPacotes);
			} else {
				// considera tempo de transmissão do JAM, pois é necessário ser concluído
				hostOrigem.onColisao(VAZAO, tempoColisao);
			}
		}

		return hosts;
	}

	private static Host getOutroHost(List<Host> hosts, Host hostProximoPacote) {
		return hosts.stream().filter(h -> h.getPosicaoBarramento() != hostProximoPacote.getPosicaoBarramento()).findFirst().orElseThrow();
	}

	private static BigDecimal validarColisao(Host host, BigDecimal tempoProximoPacote) {
		Pacote pacoteHost = host.getPacotes().peek();

		// não há possibilidade de colisão quando sem pacotes
		if (pacoteHost == null) return null;

		// tempo de chegada do primeiro símbolo de informação
		BigDecimal tempoDeteccao = tempoProximoPacote.add(TEMPO_PROPAGACAO);
		BigDecimal tempoConclusaoPacote = tempoDeteccao.add(TEMPO_TRANSMISSAO);

		BigDecimal tempoPacoteHost = pacoteHost.getTempo().add(TEMPO_INTERFRAME);

		// host vai ser capaz de identificar que meio está ocupado e vai atrasar envio, bufferizando pacotes
		if (isDetectavelAndHasInterseccao(tempoPacoteHost, tempoConclusaoPacote, tempoDeteccao)) {
			for (Pacote pacote : host.getPacotes()) {
				if (isDetectavelAndHasInterseccao(pacote.getTempo(), tempoConclusaoPacote, tempoDeteccao)) {
					pacote.setTempo(tempoConclusaoPacote);
				}
			}
		}

		// host não será capaz de detectar a portadora
		if (tempoPacoteHost.compareTo(tempoDeteccao) <= 0) {
			// considera tempo de transmissão do JAM, pois é necessário ser concluído
			host.onColisao(VAZAO, tempoDeteccao.add(TEMPO_JAM));
			return tempoPacoteHost.add(TEMPO_PROPAGACAO).add(TEMPO_JAM);
		}

		return null;
	}

	private static void atualizarPacote(Host hostProximoPacote, BigDecimal tempoMinimo) {
		var pacote = hostProximoPacote.getPacotes().peek();

		if (pacote == null) return;

		if (pacote.getTempo().compareTo(tempoMinimo) < 0) {
			pacote.setTempo(tempoMinimo);
		}
	}

	private static boolean isDetectavelAndHasInterseccao(BigDecimal tempoPacoteHost, BigDecimal tempoConclusaoProximoPacoteAoHost, BigDecimal tempoDeteccao) {
		boolean isDetectavel = tempoDeteccao.compareTo(tempoPacoteHost) < 0;

		if (!isDetectavel) return false;

		// pacote do host seria enviado antes da conclusão do envio do próximo pacote
		return tempoPacoteHost.compareTo(tempoConclusaoProximoPacoteAoHost) < 0;
	}

	private static Host getHostProximoPacote(List<Host> hosts) {
		Host hostProximoPacote = null;
		BigDecimal tempoProximoPacote = new BigDecimal(1000);
		for (Host host : hosts) {
			Pacote pacote = host.getPacotes().peek();
			if (pacote != null && pacote.getTempo().compareTo(tempoProximoPacote) < 0) {
				tempoProximoPacote = pacote.getTempo();
				hostProximoPacote = host;
			}
		}
		return hostProximoPacote;
	}

	public static void main(String[] args) throws InterruptedException {
		// 10 megabits por segundo
		int pacotesPorSegundo = 8000;
		int duracao = 1;
		List<Host> hosts = simularCsmaCd(pacotesPorSegundo, duracao);

		visualizarPacotes(hosts);

		System.out.println("\n\n\nResultados da simulação:\n");
		for (int i = 0; i < hosts.size(); i++) {
			Host host = hosts.get(i);
			int quantidadePacotes = host.getQuantidadePacotes();
			int quantidadeColisoes = host.getQuantidadeColisoes();
			System.out.printf("Host %d - Quantidade de pacotes: %d.\n", i, quantidadePacotes);
			System.out.printf("Host %d - Quantidade de colisões: %d.\n", i, quantidadeColisoes);
			System.out.printf("Host %d - Quantidade de colisões por pacote: %.2f\n", i, (float) quantidadeColisoes / (float) quantidadePacotes);
			System.out.printf("Host %d - Taxa de erro: %.2f%%.\n", i, ((float) host.getPacotesPerdidos() / (float) quantidadePacotes) * 100.0);
			System.out.printf("Host %d - Delay médio: %.8fus.\n", i, host.getTempoMedioDelay() * pow(10, 6));
		}
	}

	private static void visualizarPacotes(List<Host> hosts) throws InterruptedException {
		List<Pacote> pacotes = new ArrayList<>();

		for (Host host : hosts) {
			pacotes.addAll(host.getPacotesTransmitidos());
		}

		pacotes.sort(Pacote::compareTo);

		BigDecimal tempoAtual = BigDecimal.ZERO;

		String[] barramento = new String[COMPRIMENTO_BARRAMENTO];

		while (!pacotes.isEmpty()) {
			List<Pacote> pacotesSendoTransmitidos = new ArrayList<>();

			for (Pacote pacote : pacotes) {
				if (pacote.getTempoInicio().compareTo(tempoAtual) <= 0) {
					if (pacote.getTempoConclusao().compareTo(tempoAtual) <= 0) {
						// transmissão já concluída
						pacotes.remove(pacote);
					} else {
						pacotesSendoTransmitidos.add(pacote);
					}
				}
			}

			String emojiVazio = "⬜";
			Arrays.fill(barramento, emojiVazio);

			for (Pacote pacote : pacotesSendoTransmitidos) {
				BigDecimal distanciaPercorrida = tempoAtual.subtract(pacote.getTempoInicio()).multiply(VELOCIDADE_DE_PROPAGACAO_DO_MEIO, DECIMAL128);

				Host host = pacote.getHost();

				// deslocamento é positivo para host 1 e negativo para host 2
				double direcao = host.getId() == 1 ? 1 : -1;

				int posicao = (int) (host.getPosicaoBarramento() + (distanciaPercorrida.doubleValue() * direcao));

				if (posicao >= 0 && posicao < barramento.length) {
					barramento[posicao] = "❇️";

					// da esquerda para direita
					if (direcao == 1) {
						for (int i = 0; i < posicao; i++) {
							barramento[i] = barramento[i].equals(emojiVazio) ? "❇️":  "✴️";
						}
					} else {
						for (int i = barramento.length - 1; i > posicao; i--) {
							barramento[i] = barramento[i].equals(emojiVazio) ? "❇️":  "✴️";
						}
					}
				}
			}

			System.out.printf("Tempo: %.3fms | 💻%s💻%n", tempoAtual.multiply(valueOf(10e3)).doubleValue(), String.join("", barramento));

			if (!pacotesSendoTransmitidos.isEmpty()) {
				Thread.sleep(50);
			}

			// tempo de deslocamento de 1m
			tempoAtual = tempoAtual.add(BigDecimal.ONE.divide(VELOCIDADE_DE_PROPAGACAO_DO_MEIO, DECIMAL128));
		}

	}

}
