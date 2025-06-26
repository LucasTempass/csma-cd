import java.math.BigDecimal;

public class Pacote implements Comparable<Pacote> {

	private final BigDecimal tempoPrevisto;
	private BigDecimal tempoInicio;
	private BigDecimal tempoConclusao;
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

	public BigDecimal getTempoInicio() {
		return tempoInicio;
	}

	public void setTempoInicio(BigDecimal tempoInicio) {
		this.tempoInicio = tempoInicio;
	}

	public BigDecimal getTempoConclusao() {
		return tempoConclusao;
	}

	public void setTempoConclusao(BigDecimal tempoConclusao) {
		this.tempoConclusao = tempoConclusao;
	}

	public Host getHost() {
		return host;
	}

	public BigDecimal getTempoColisao() {
		return tempoColisao;
	}

	public void setTempoColisao(BigDecimal tempoColisao) {
		this.tempoColisao = tempoColisao;
	}

}
