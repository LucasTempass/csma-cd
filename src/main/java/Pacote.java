import java.math.BigDecimal;

public class Pacote implements Comparable<Pacote> {

	private final BigDecimal tempoPrevisto;
	private BigDecimal tempo;
	private final int id;

	public Pacote(double tempoPrevisto, int id) {
		this.tempoPrevisto = BigDecimal.valueOf(tempoPrevisto);
		this.tempo = BigDecimal.valueOf(tempoPrevisto);
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

}
