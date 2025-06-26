import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Pacote implements Comparable<Pacote> {

	private final BigDecimal tempoPrevisto;
	private final List<Tentativa> tentativas = new ArrayList<>();
	private BigDecimal tempo;
	private final Host host;

	public Pacote(double tempoPrevisto, Host host) {
		this.tempoPrevisto = BigDecimal.valueOf(tempoPrevisto);
		this.tempo = BigDecimal.valueOf(tempoPrevisto);
		this.host = host;
	}

	public double getDelay() {
		return tempo.subtract(tempoPrevisto).doubleValue();
	}

	public BigDecimal getTempo() {
		return tempo;
	}

	public void setTempo(BigDecimal tempo) {
		this.tempo = tempo;
	}

	@Override
	public int compareTo(Pacote o) {
		return this.tempo.compareTo(o.tempo);
	}

	public Host getHost() {
		return host;
	}

	public void removerTentativa() {
		if (tentativas.isEmpty()) return;
		tentativas.removeFirst();
	}

	public void adicionarSucesso(BigDecimal tempoInicio, BigDecimal tempoConclusao) {
		this.tentativas.add(new Tentativa(tempoInicio, tempoConclusao, false));
	}

	public void adicionarFalha(BigDecimal tempoInicio, BigDecimal tempoConclusao) {
		this.tentativas.add(new Tentativa(tempoInicio, tempoConclusao, true));
	}

	public List<Tentativa> getTentativas() {
		return tentativas;
	}

	public Tentativa getTentativa() {
		return tentativas.stream().findFirst().orElse(null);
	}

}
