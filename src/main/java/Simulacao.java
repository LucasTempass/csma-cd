import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

public class Simulacao {

	private static final int COMPRIMENTO_BARRAMENTO = 100;
	private static final double VELOCIDADE_DA_LUZ = 3 * Math.pow(10, 8);
	private static final double VELOCIDADE_DE_PROPAGACAO_DO_MEIO = 0.66 * VELOCIDADE_DA_LUZ;
	private static final double DURACAO_EM_SEGUNDOS = 1000;

	private static List<Host> gerarHosts(int quantidade, double taxaDePacotes) {
		List<Host> hosts = new ArrayList<>();
		double distanciaEntreHosts = COMPRIMENTO_BARRAMENTO / Math.pow(2, quantidade - 2);
		for (int i = 0; i < quantidade; i++) {
			hosts.add(new Host(i * distanciaEntreHosts, taxaDePacotes, DURACAO_EM_SEGUNDOS));
		}
		return hosts;
	}

	private static List<Host> simularCsmaCd(int quantidadeHosts, double taxaDePacotes, double larguraDeBanda, double bitsPorPacote) {
		List<Host> hosts = gerarHosts(quantidadeHosts, taxaDePacotes);

		while (true) {
			Host hostProximoPacote = getHostProximoPacote(hosts);

			// não há mais pacotes a serem transmitidos
			if (hostProximoPacote == null) break;

			Pacote proximoPacote = hostProximoPacote.getPacotes().peek();

			// não há mais pacotes a serem transmitidos
			if (proximoPacote == null) break;

			double tempoProximoPacote = proximoPacote.getTempo();

			boolean hasColisao = false;

			for (Host host : hosts) {
				if (host == hostProximoPacote) continue;

				Pacote pacoteHost = host.getPacotes().peek();

				// não há possibilidade de colisão quando sem pacotes
				if (pacoteHost == null) continue;

				double distancia = abs(hostProximoPacote.getPosicaoBarramento() - host.getPosicaoBarramento());
				// tempo que um símbolo demora a chegar até o host
				double tempoPropagacao = distancia / Simulacao.VELOCIDADE_DE_PROPAGACAO_DO_MEIO;
				// tempo necessário para transmitir o frame por completo
				double tempoTransmissao = bitsPorPacote / larguraDeBanda;

				double tempoPacoteHost = pacoteHost.getTempo();
				double tempoChegadaProximoPacoteAoHost = tempoProximoPacote + tempoPropagacao + tempoTransmissao;

				// host vai ser capaz de identificar que meio está ocupado e vai atrasar envio, bufferizando pacotes
				if (tempoProximoPacote + tempoPropagacao < tempoPacoteHost && tempoPacoteHost < tempoChegadaProximoPacoteAoHost) {
					for (Pacote pacote : host.getPacotes()) {
						if (tempoProximoPacote + tempoPropagacao < pacote.getTempo() && pacote.getTempo() < tempoChegadaProximoPacoteAoHost) {
							pacote.setTempo(tempoChegadaProximoPacoteAoHost);
						}
					}
				}

				// host não será capaz de identificar pacote
				if (tempoPacoteHost <= (tempoProximoPacote + tempoPropagacao)) {
					hasColisao = true;
					host.onColisao(larguraDeBanda);
				}
			}

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
		// inicializa com valor mínimo
		double tempoProximoPacote = -1;

		for (Host host : hosts) {
			Pacote pacote = host.getPacotes().peek();
			if (pacote != null && pacote.getTempo() < tempoProximoPacote) {
				tempoProximoPacote = pacote.getTempo();
				hostProximoPacote = host;
			}
		}

		return hostProximoPacote;
	}

	public static void main(String[] args) {
		// 10 megabits por segundo
		double larguraDeBanda = 1e7;
		int numeroDeHosts = 3;
		int bitsPorPacote = 1500;
		int pacotesPorSegundo = 1000;
		List<Host> hosts = simularCsmaCd(numeroDeHosts, pacotesPorSegundo, larguraDeBanda, bitsPorPacote);

		for (int i = 0; i < hosts.size(); i++) {
			Host host = hosts.get(i);
			int quantidadePacotes = host.getQuantidadePacotes();
			int quantidadeColisoes = host.getQuantidadeColisoes();
			System.out.printf("Host %d - Quantidade de pacotes: %d.\n", i, quantidadePacotes);
			System.out.printf("Host %d - Quantidade de colisões: %d.\n", i, quantidadeColisoes);
			System.out.printf("Host %d - Quantidade de colisões por pacote: %.2f\n", i, (float) quantidadeColisoes / (float) quantidadePacotes);
			System.out.printf("Host %d - Taxa de erro: %.2f%%.\n", i, ((float) host.getPacotesPerdidos() / (float) quantidadePacotes) * 100.0);
		}
	}

}
