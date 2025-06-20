import java.math.BigDecimal;

public class Pacote implements Comparable<Pacote> {

	private final BigDecimal tempoPrevisto;
	private BigDecimal tempo;

	public Pacote(double tempoPrevisto) {
		this.tempoPrevisto = BigDecimal.valueOf(tempoPrevisto);
		this.tempo = BigDecimal.valueOf(tempoPrevisto);
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

}
