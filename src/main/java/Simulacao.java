import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.math.BigDecimal.valueOf;

public class Simulacao {

	private static final int COMPRIMENTO_BARRAMENTO = 100;
	private static final BigDecimal VELOCIDADE_DA_LUZ = new BigDecimal("3E8");
	private static final BigDecimal VELOCIDADE_DE_PROPAGACAO_DO_MEIO = VELOCIDADE_DA_LUZ.multiply(new BigDecimal("0.66"));
	private static final double DURACAO_EM_SEGUNDOS = 1;
	private static final MathContext PRECISAO = MathContext.DECIMAL128;
	public static final int BITS_POR_PACOTE = 512;

	private static double tempoDeConclusao = 0.0;
	private static double quantidadesDePacotes = 0.0;

	private static List<Host> gerarHosts(int quantidade, double taxaDePacotes) {
		List<Host> hosts = new ArrayList<>();
		double distanciaEntreHosts = COMPRIMENTO_BARRAMENTO / Math.pow(2, quantidade - 2);
		for (int i = 0; i < quantidade; i++) {
			hosts.add(new Host(i * distanciaEntreHosts, taxaDePacotes, DURACAO_EM_SEGUNDOS));
		}
		return hosts;
	}

	private static List<Host> simularCsmaCd(int quantidadeHosts, double taxaDePacotes, double larguraDeBanda) {
		List<Host> hosts = gerarHosts(quantidadeHosts, taxaDePacotes);

		while (true) {
			Host hostProximoPacote = getHostProximoPacote(hosts);

			// não há mais pacotes a serem transmitidos
			if (hostProximoPacote == null) break;

			Pacote proximoPacote = hostProximoPacote.getPacotes().peek();

			// não há mais pacotes a serem transmitidos
			if (proximoPacote == null) break;

			BigDecimal tempoProximoPacote = proximoPacote.getTempo();

			boolean hasColisao = false;

			for (Host host : hosts) {
				if (host == hostProximoPacote) continue;

				Pacote pacoteHost = host.getPacotes().peek();

				// não há possibilidade de colisão quando sem pacotes
				if (pacoteHost == null) continue;

				double distancia = abs(hostProximoPacote.getPosicaoBarramento() - host.getPosicaoBarramento());

				// tempo que um símbolo demora a chegar até o host
				BigDecimal tempoPropagacao = valueOf(distancia).divide(VELOCIDADE_DE_PROPAGACAO_DO_MEIO, PRECISAO);
				// tempo necessário para transmitir o frame por completo
				BigDecimal tempoTransmissao = valueOf(Simulacao.BITS_POR_PACOTE).divide(valueOf(larguraDeBanda), PRECISAO);

				BigDecimal tempoPacoteHost = pacoteHost.getTempo();
				BigDecimal tempoChegadaProximoPacoteAoHost = tempoProximoPacote.add(tempoPropagacao).add(tempoTransmissao);

				// host vai ser capaz de identificar que meio está ocupado e vai atrasar envio, bufferizando pacotes
				if (tempoProximoPacote.add(tempoPropagacao).compareTo(tempoPacoteHost) < 0 && tempoPacoteHost.compareTo(tempoChegadaProximoPacoteAoHost) < 0) {
					int contador = 0;
					for (Pacote pacote : host.getPacotes()) {
						if (tempoProximoPacote.add(tempoPropagacao).compareTo(pacote.getTempo()) < 0 && pacote.getTempo().compareTo(tempoChegadaProximoPacoteAoHost) < 0) {
							pacote.setTempo(tempoChegadaProximoPacoteAoHost.add(tempoTransmissao.multiply(valueOf(contador))));
							contador++;
						}
					}
				}

				// host não será capaz de identificar pacote
				if (tempoPacoteHost.compareTo(tempoProximoPacote.add(tempoPropagacao)) <= 0) {
					hasColisao = true;
					host.onColisao(larguraDeBanda);
				}
			}

			// apenas para métricas
			tempoDeConclusao = tempoProximoPacote.byteValue();
			quantidadesDePacotes++;

			if (!hasColisao) {
				hostProximoPacote.onSucesso();
			} else {
				hostProximoPacote.onColisao(larguraDeBanda);
			}
		}
		return hosts;
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
		double larguraDeBanda = 1e7;
		int numeroDeHosts = 2;
		int pacotesPorSegundo = 4000;
		List<Host> hosts = simularCsmaCd(numeroDeHosts, pacotesPorSegundo, larguraDeBanda);

		for (int i = 0; i < hosts.size(); i++) {
			Host host = hosts.get(i);
			int quantidadePacotes = host.getQuantidadePacotes();
			int quantidadeColisoes = host.getQuantidadeColisoes();
			System.out.printf("Host %d - Quantidade de pacotes: %d.\n", i, quantidadePacotes);
			System.out.printf("Host %d - Quantidade de colisões: %d.\n", i, quantidadeColisoes);
			System.out.printf("Host %d - Quantidade de colisões por pacote: %.2f\n", i, (float) quantidadeColisoes / (float) quantidadePacotes);
			System.out.printf("Host %d - Taxa de erro: %.2f%%.\n", i, ((float) host.getPacotesPerdidos() / (float) quantidadePacotes) * 100.0);
			System.out.printf("Host %d - Delay médio: %.8fs.\n", i, host.getTempoMedioDelay());
		}

		double bitsPorSegundo = (BITS_POR_PACOTE * quantidadesDePacotes) / tempoDeConclusao;
		System.out.printf("Throughput: %.2f Mbps\n", bitsPorSegundo * pow(10, -6));
	}

}
