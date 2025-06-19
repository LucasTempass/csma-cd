import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static java.lang.Math.abs;

public class Simulacao {

	static final int COMPRIMENTO_BARRAMENTO = 100;
	static final double VELOCIDADE_DA_LUZ = 3 * Math.pow(10, 8);
	static final double VELOCIDADE_DE_PROPAGACAO_DO_MEIO = 0.66 * VELOCIDADE_DA_LUZ;
	static final double DURACAO_EM_SEGUNDOS = 1000;

	private static List<Host> gerarHosts(int quantidade, double taxaDePacotes) {
		List<Host> hosts = new ArrayList<>();
		double distanciaEntreHosts = COMPRIMENTO_BARRAMENTO / Math.pow(2, quantidade - 2);
		for (int i = 0; i < quantidade; i++) {
			hosts.add(new Host(i * distanciaEntreHosts, taxaDePacotes, DURACAO_EM_SEGUNDOS));
		}
		return hosts;
	}

	private static void csmaCd(int quantidadeHosts, double taxaDePacotes, double larguraDeBanda, double bitsPorPacote) {
		List<Host> hosts = gerarHosts(quantidadeHosts, taxaDePacotes);

		double tempoProximoPacote = 0;

		int pacotesTotal = 0;
		int pacotesTransmitidosTotal = 0;

		while (true) {
			Host hostProximoPacote = getHostProximoPacote(hosts);

			// não há mais pacotes a serem transmitidos
			if (hostProximoPacote == null) break;

			pacotesTotal++;

			tempoProximoPacote = hostProximoPacote.getPacotes().peek();

			boolean hasColisao = false;

			for (Host host : hosts) {
				if (host == hostProximoPacote || host.getPacotes().isEmpty()) {
					// não há possibilidade de colisão
					continue;
				}

				double distancia = abs(hostProximoPacote.getPosicaoBarramento() - host.getPosicaoBarramento());
				// tempo que um símbolo demora a chegar até o host
				double tempoPropagacao = distancia / Simulacao.VELOCIDADE_DE_PROPAGACAO_DO_MEIO;
				// tempo necessário para transmitir o frame por completo
				double tempoTransmissao = bitsPorPacote / larguraDeBanda;
				// chegada ao host especificado
				double tempoChegadaProximoPacote = tempoProximoPacote + tempoPropagacao + tempoTransmissao;

				double tempoPacoteDestinatario = host.getPacotes().peek();

				if (tempoProximoPacote + tempoPropagacao < tempoPacoteDestinatario && tempoPacoteDestinatario < tempoChegadaProximoPacote) {
					// host vai ser capaz de identificar que meio está ocupado e vai atrasar envio
					List<Double> pacotesAtualizados = new ArrayList<>();
					for (double tempo : host.getPacotes()) {
						if (tempoProximoPacote + tempoPropagacao < tempo && tempo < tempoChegadaProximoPacote) {
							pacotesAtualizados.add(tempoChegadaProximoPacote);
						}
						else {
							pacotesAtualizados.add(tempo);
						}
					}
					host.setPacotes(new ArrayDeque<>(pacotesAtualizados));
				}

				// host não será capaz de identificar pacote
				if (tempoPacoteDestinatario <= (tempoProximoPacote + tempoPropagacao)) {
					hasColisao = true;
					pacotesTotal++;
					host.onColisao(larguraDeBanda);
				}
			}

			if (!hasColisao) {
				pacotesTransmitidosTotal++;
				hostProximoPacote.onSucesso();
			} else {
				hostProximoPacote.onColisao(larguraDeBanda);
			}
		}

		System.out.printf("Eficiência: %.2f\n", (double) pacotesTransmitidosTotal / pacotesTotal);
	}

	private static Host getHostProximoPacote(List<Host> hosts) {
		Host hostProximoPacote = null;
		double tempoProximoPacote = Double.POSITIVE_INFINITY;

		for (Host host : hosts) {
			Deque<Double> pacotes = host.getPacotes();
			if (!pacotes.isEmpty() && pacotes.peek() < tempoProximoPacote) {
				tempoProximoPacote = pacotes.peek();
				hostProximoPacote = host;
			}
		}

		return hostProximoPacote;
	}

	public static void main(String[] args) {
		// 10 megabits por segundo
		double larguraDeBanda = 1e7;
		int numeroDeHosts = 2;
		int bitsPorPacote = 1500;
		int pacotesPorSegundo = 100;
		csmaCd(numeroDeHosts, pacotesPorSegundo, larguraDeBanda, bitsPorPacote);
	}

}
