import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Pacote implements Comparable<Pacote> {

	private final BigDecimal tempoPrevisto;
	private final List<BigDecimal> temposInicio = new ArrayList<>();
	private final List<BigDecimal> temposConclusao = new ArrayList<>();
	private final List<Tentativa> tentativas = new ArrayList<>();
	private BigDecimal tempoColisao;
	private BigDecimal tempo;
	private final Host host;
	private final int id;

	public Pacote(double tempoPrevisto, Host host, int id) {
		this.tempoPrevisto = BigDecimal.valueOf(tempoPrevisto);
		this.tempo = BigDecimal.valueOf(tempoPrevisto);
		this.host = host;
		this.id = id;
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

	public int getId() {
		return id;
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
