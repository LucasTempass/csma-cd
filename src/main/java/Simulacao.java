import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.pow;
import static java.math.BigDecimal.valueOf;

public class Simulacao {

	private static final int COMPRIMENTO_BARRAMENTO = 100;
	private static final BigDecimal VELOCIDADE_DA_LUZ = new BigDecimal("3E8");
	private static final BigDecimal VELOCIDADE_DE_PROPAGACAO_DO_MEIO = VELOCIDADE_DA_LUZ.multiply(new BigDecimal("0.66"));
	private static final double DURACAO_EM_SEGUNDOS = 1;
	private static final MathContext PRECISAO = MathContext.DECIMAL128;
	public static final int BITS_POR_PACOTE = 512;
	public static final int BITS_INTERFRAME = 96;
	private static final double VAZAO = 1e7;
	private static final int DISTANCIA_ENTRE_HOSTS = 100;

	// tempo que um símbolo demora a chegar até o host
	private static final BigDecimal TEMPO_PROPAGACAO = valueOf(DISTANCIA_ENTRE_HOSTS).divide(VELOCIDADE_DE_PROPAGACAO_DO_MEIO, PRECISAO);
	// tempo necessário para transmitir o frame por completo
	private static final BigDecimal TEMPO_TRANSMISSAO = valueOf(Simulacao.BITS_POR_PACOTE).divide(valueOf(VAZAO), PRECISAO);
	private static final BigDecimal TEMPO_INTERFRAME = valueOf(BITS_INTERFRAME).divide(valueOf(VAZAO), PRECISAO);

	private static BigDecimal tempoDeConclusao = BigDecimal.ZERO;
	private static double quantidadesDePacotes = 0.0;

	private static List<Host> gerarHosts(double taxaDePacotes) {
		List<Host> hosts = new ArrayList<>();
		double distanciaEntreHosts = COMPRIMENTO_BARRAMENTO / Math.pow(2, 0);
		for (int i = 0; i < 2; i++) {
			hosts.add(new Host(i * distanciaEntreHosts, taxaDePacotes, DURACAO_EM_SEGUNDOS));
		}
		return hosts;
	}

	private static List<Host> simularCsmaCd(double taxaDePacotes) {
		List<Host> hosts = gerarHosts(taxaDePacotes);

		while (true) {
			Host hostProximoPacote = getHostProximoPacote(hosts);

			// não há mais pacotes a serem transmitidos
			if (hostProximoPacote == null) break;

			Pacote proximoPacote = hostProximoPacote.getPacotes().peek();

			// não há mais pacotes a serem transmitidos
			if (proximoPacote == null) break;

			BigDecimal tempoProximoPacote = proximoPacote.getTempo();

			Host host = getOutroHost(hosts, hostProximoPacote);

			BigDecimal tempoColisao = validarColisao(host, tempoProximoPacote);

			if (tempoColisao == null) {
				hostProximoPacote.onSucesso();

				BigDecimal tempoMinimoProximosPacotes = tempoProximoPacote.add(TEMPO_TRANSMISSAO).add(TEMPO_INTERFRAME);

				// atrasa os próximos pacotes para contemplar tempo inteframe
				atualizarPacote(hostProximoPacote, tempoMinimoProximosPacotes);
			} else {
				hostProximoPacote.onColisao(VAZAO, tempoColisao);
			}

			// apenas para métricas
			tempoDeConclusao = tempoProximoPacote.add(TEMPO_TRANSMISSAO);
			quantidadesDePacotes++;
		}

		return hosts;
	}

	private static Host getOutroHost(List<Host> hosts, Host hostProximoPacote) {
		return hosts.stream().filter(
				h -> h.getPosicaoBarramento() != hostProximoPacote.getPosicaoBarramento()
		).findFirst().orElseThrow();
	}

	private static BigDecimal validarColisao(Host host, BigDecimal tempoProximoPacote) {
		Pacote pacoteHost = host.getPacotes().peek();

		// não há possibilidade de colisão quando sem pacotes
		if (pacoteHost == null) return null;

		// tempo de chegada do primeiro símbolo de informação
		BigDecimal tempoDeteccao = tempoProximoPacote.add(TEMPO_PROPAGACAO);
		BigDecimal tempoConclusaoPacote = tempoDeteccao.add(TEMPO_TRANSMISSAO);

		BigDecimal tempoPacoteHost = pacoteHost.getTempo();

		// host vai ser capaz de identificar que meio está ocupado e vai atrasar envio, bufferizando pacotes
		if (isDetectavelAndHasInterseccao(tempoPacoteHost, tempoConclusaoPacote, tempoDeteccao)) {
			for (Pacote pacote : host.getPacotes()) {
				if (isDetectavelAndHasInterseccao(pacote.getTempo(), tempoConclusaoPacote, tempoDeteccao)) {
					pacote.setTempo(tempoConclusaoPacote);
				}
			}
		}

		// host não será capaz de identificar pacote
		if (tempoPacoteHost.compareTo(tempoDeteccao) <= 0) {
			host.onColisao(VAZAO, tempoDeteccao);
			return tempoPacoteHost.add(TEMPO_TRANSMISSAO);
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

	public static void main(String[] args) {
		// 10 megabits por segundo
		int pacotesPorSegundo = 9000;
		List<Host> hosts = simularCsmaCd(pacotesPorSegundo);

		for (int i = 0; i < hosts.size(); i++) {
			Host host = hosts.get(i);
			int quantidadePacotes = host.getQuantidadePacotes();
			int quantidadeColisoes = host.getQuantidadeColisoes();
			System.out.printf("Host %d - Quantidade de pacotes: %d.\n", i, quantidadePacotes);
			System.out.printf("Host %d - Quantidade de colisões: %d.\n", i, quantidadeColisoes);
			System.out.printf("Host %d - Quantidade de colisões por pacote: %.2f\n", i, (float) quantidadeColisoes / (float) quantidadePacotes);
			System.out.printf("Host %d - Taxa de erro: %.2f%%.\n", i, ((float) host.getPacotesPerdidos() / (float) quantidadePacotes) * 100.0);
			System.out.printf("Host %d - Delay médio: %.8fs.\n", i, host.getTempoMedioDelay() * pow(10, 6));
		}

		BigDecimal bitsPorSegundo = valueOf(BITS_POR_PACOTE).multiply(valueOf(quantidadesDePacotes)).divide(tempoDeConclusao, PRECISAO);
		System.out.printf("Throughput: %.2f Mbps\n", bitsPorSegundo.multiply(valueOf(pow(10, -6))).doubleValue());
		System.out.println("tempoDeConclusao = " + tempoDeConclusao);
	}

}
