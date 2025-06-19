public class Pacote implements Comparable<Pacote> {

	private final double tempoPrevisto;

	private double tempo;

	public Pacote(double tempoPrevisto) {
		this.tempoPrevisto = tempoPrevisto;
		this.tempo = tempoPrevisto;
	}

	public double getDelay() {
		return tempo - tempoPrevisto;
	}

	public double getTempo() {
		return tempo;
	}

	public void setTempo(double tempo) {
		this.tempo = tempo;
	}

	@Override
	public int compareTo(Pacote o) {
		return Double.compare(this.tempo, o.tempo);
	}

}
