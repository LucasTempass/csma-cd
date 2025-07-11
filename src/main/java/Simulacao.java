import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.pow;
import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;
import static java.math.MathContext.DECIMAL128;

public class Simulacao {

	private static final int COMPRIMENTO_BARRAMENTO = 100;
	private static final BigDecimal VELOCIDADE_DA_LUZ = new BigDecimal("3E8");
	private static final BigDecimal VELOCIDADE_DE_PROPAGACAO_DO_MEIO = VELOCIDADE_DA_LUZ.multiply(new BigDecimal("0.66"));
	private static final BigDecimal TEMPO_PROPAGACAO_1M = BigDecimal.ONE.divide(VELOCIDADE_DE_PROPAGACAO_DO_MEIO, DECIMAL128);
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

	private static List<Host> gerarHosts(double taxaDePacotes, double duracao) {
		List<Host> hosts = new ArrayList<>();
		double distanciaEntreHosts = COMPRIMENTO_BARRAMENTO / pow(2, 0);
		for (int i = 0; i < 2; i++) {
			hosts.add(new Host(i * distanciaEntreHosts, taxaDePacotes, duracao, i + 1));
		}
		return hosts;
	}

	private static List<Host> simularCsmaCd(double taxaDePacotes, double duracao) {
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
				proximoPacote.adicionarFalha(tempoInicioTransmissao, tempoColisao);
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
			BigDecimal tempoColisao = tempoDeteccao.add(TEMPO_JAM);
			host.onColisao(VAZAO, tempoColisao);
			pacoteHost.adicionarFalha(tempoPacoteHost, tempoColisao);
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
		int pacotesPorSegundo = 2000;
		double duracao = 0.5;
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

		BigDecimal tempoAtual = getPrimeroPacoteComColisao(pacotes).subtract(TEMPO_PROPAGACAO_1M.multiply(BigDecimal.TEN));

		String[] barramento = new String[COMPRIMENTO_BARRAMENTO];

		BigDecimal comprimentoFrame = TEMPO_TRANSMISSAO.multiply(VELOCIDADE_DE_PROPAGACAO_DO_MEIO, PRECISAO);

		while (!pacotes.isEmpty()) {
			List<Pacote> pacotesSendoTransmitidos = new ArrayList<>();
			List<Pacote> pacotesRemovidos = new ArrayList<>();

			for (Pacote pacote : pacotes) {
				Tentativa tentativa = pacote.getTentativa();
				if (tentativa.tempoInicio().compareTo(tempoAtual) <= 0) {
					if (tentativa.tempoConclusao().compareTo(tempoAtual) <= 0) {
						if (pacote.getTentativas().size() == 1) {
							pacotesRemovidos.add(pacote);
						} else {
							pacote.removerTentativa();
						}
					} else {
						pacotesSendoTransmitidos.add(pacote);
					}
				}
			}

			pacotes.removeAll(pacotesRemovidos);

			String emojiVazio = "⬜";
			Arrays.fill(barramento, emojiVazio);

			for (Pacote pacote : pacotesSendoTransmitidos) {
				Tentativa tentativa = pacote.getTentativa();

				BigDecimal distanciaPercorrida = tempoAtual.subtract(tentativa.tempoInicio()).multiply(VELOCIDADE_DE_PROPAGACAO_DO_MEIO);

				Host host = pacote.getHost();

				// deslocamento é positivo para host 1 e negativo para host 2
				double direcao = host.getId() == 1 ? 1 : -1;

				// converter para inteiros
				int headPos = (int) (host.getPosicaoBarramento() + distanciaPercorrida.doubleValue() * direcao);
				int tailPos = (int) (host.getPosicaoBarramento() + (distanciaPercorrida.subtract(comprimentoFrame).doubleValue()) * direcao);

				// preenche entre cabeça e cauda
				int inicio = Math.min(headPos, tailPos);
				int fim = Math.max(headPos, tailPos);

				for (int i = inicio; i <= fim; i++) {
					if (i >= 0 && i < barramento.length) {
						if (barramento[i].equals(emojiVazio)) {
							barramento[i] = "❇️";
						} else {
							barramento[i] = "✴️";
						}
					}
				}
			}

			System.out.printf("Tempo: %.3fms | 💻%s💻%n", tempoAtual.multiply(valueOf(10e3)).doubleValue(), String.join("", barramento));

			tempoAtual = tempoAtual.add(TEMPO_PROPAGACAO_1M);
		}
	}

	private static BigDecimal getPrimeroPacoteComColisao(List<Pacote> pacotes) {
		return pacotes.stream().filter(p -> p.getTentativas().stream().anyMatch(t -> !t.hasColisao()))
				.map(p -> p.getTentativa().tempoInicio())
				.min(BigDecimal::compareTo)
				.orElse(ZERO);
	}

}
