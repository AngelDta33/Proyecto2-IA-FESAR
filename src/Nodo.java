/**
 * Un punto de la matriz 20x20.
 * Se guarda como (fila, columna), igual que en el enunciado: Inicio (18,2) = fila 18, columna 2.
 */
public class Nodo {

    public final int fila;
    public final int columna;

    public Nodo(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
    }

    /** Distancia euclidiana (en línea recta) entre este nodo y otro. */
    public double distanciaA(Nodo otro) {
        int dFila = this.fila - otro.fila;
        int dColumna = this.columna - otro.columna;
        return Math.sqrt(dFila * dFila + dColumna * dColumna);
    }

    public boolean estaEn(int fila, int columna) {
        return this.fila == fila && this.columna == columna;
    }

    @Override
    public String toString() {
        return "(" + fila + "," + columna + ")";
    }
}
