import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Dibuja la matriz de 20x20, el inicio, los nodos y la ruta encontrada.
 * También guarda los nodos que el usuario va colocando.
 */
public class PanelMatriz extends JPanel {

    public static final int TAMANO = 20;   // matriz de 20x20
    private static final int CELDA = 30;   // pixeles por celda
    private static final int MARGEN = 30;  // espacio para los números de fila y columna
    private static final int RADIO = 11;   // radio de los círculos de los nodos

    private Nodo inicio;
    private final List<Nodo> nodos = new ArrayList<>();
    private int[] ruta; // null mientras no se haya resuelto

    public PanelMatriz() {
        int lado = MARGEN + TAMANO * CELDA + 10;
        setPreferredSize(new Dimension(lado, lado));
        setBackground(Color.WHITE);
    }

    // =====================================================================
    //  Datos
    // =====================================================================

    public Nodo getInicio() {
        return inicio;
    }

    public void setInicio(Nodo inicio) {
        this.inicio = inicio;
        borrarRutaYRepintar();
    }

    public List<Nodo> getNodos() {
        return nodos;
    }

    public void agregarNodo(Nodo nodo) {
        nodos.add(nodo);
        borrarRutaYRepintar();
    }

    public void limpiarNodos() {
        nodos.clear();
        borrarRutaYRepintar();
    }

    public void setRuta(int[] ruta) {
        this.ruta = ruta;
        repaint();
    }

    private void borrarRutaYRepintar() {
        ruta = null;
        repaint();
    }

    /** true si en esa celda ya está el inicio o algún nodo. */
    public boolean estaOcupada(int fila, int columna) {
        if (inicio != null && inicio.estaEn(fila, columna)) {
            return true;
        }
        for (Nodo nodo : nodos) {
            if (nodo.estaEn(fila, columna)) {
                return true;
            }
        }
        return false;
    }

    /** Convierte un clic (x, y) en {fila, columna}, o null si fue fuera de la matriz. */
    public int[] celdaEn(int x, int y) {
        if (x < MARGEN || y < MARGEN) {
            return null;
        }
        int columna = (x - MARGEN) / CELDA + 1;
        int fila = (y - MARGEN) / CELDA + 1;
        if (fila > TAMANO || columna > TAMANO) {
            return null;
        }
        return new int[]{fila, columna};
    }

    // =====================================================================
    //  Dibujo
    // =====================================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        dibujarCuadricula(g2);
        dibujarRuta(g2);
        dibujarNodos(g2);
    }

    private void dibujarCuadricula(Graphics2D g2) {
        // Fondo de las celdas
        g2.setColor(new Color(220, 232, 245));
        g2.fillRect(MARGEN, MARGEN, TAMANO * CELDA, TAMANO * CELDA);

        // Líneas
        g2.setColor(Color.GRAY);
        for (int i = 0; i <= TAMANO; i++) {
            int pos = MARGEN + i * CELDA;
            g2.drawLine(MARGEN, pos, MARGEN + TAMANO * CELDA, pos); // horizontal
            g2.drawLine(pos, MARGEN, pos, MARGEN + TAMANO * CELDA); // vertical
        }

        // Números de columna (arriba) y de fila (izquierda)
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int i = 1; i <= TAMANO; i++) {
            int centro = MARGEN + (i - 1) * CELDA + CELDA / 2;
            dibujarTextoCentrado(g2, String.valueOf(i), centro, MARGEN / 2);
            dibujarTextoCentrado(g2, String.valueOf(i), MARGEN / 2, centro);
        }
    }

    private void dibujarRuta(Graphics2D g2) {
        if (ruta == null || inicio == null) {
            return;
        }
        g2.setColor(new Color(30, 90, 140));
        g2.setStroke(new BasicStroke(2));

        Nodo anterior = inicio;
        for (int id : ruta) {
            Nodo siguiente = nodos.get(id - 1);
            dibujarFlecha(g2, anterior, siguiente);
            anterior = siguiente;
        }
        dibujarFlecha(g2, anterior, inicio); // regreso al inicio

        g2.setStroke(new BasicStroke(1));
    }

    private void dibujarNodos(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));

        for (int i = 0; i < nodos.size(); i++) {
            dibujarCirculo(g2, nodos.get(i), Color.YELLOW, String.valueOf(i + 1));
        }
        if (inicio != null) {
            dibujarCirculo(g2, inicio, new Color(70, 150, 230), "I");
        }
    }

    private void dibujarCirculo(Graphics2D g2, Nodo nodo, Color relleno, String texto) {
        int x = centroX(nodo);
        int y = centroY(nodo);
        g2.setColor(relleno);
        g2.fillOval(x - RADIO, y - RADIO, RADIO * 2, RADIO * 2);
        g2.setColor(Color.BLACK);
        g2.drawOval(x - RADIO, y - RADIO, RADIO * 2, RADIO * 2);
        dibujarTextoCentrado(g2, texto, x, y);
    }

    /** Línea de 'desde' a 'hasta' con una punta de flecha al final. */
    private void dibujarFlecha(Graphics2D g2, Nodo desde, Nodo hasta) {
        double x1 = centroX(desde);
        double y1 = centroY(desde);
        double angulo = Math.atan2(centroY(hasta) - y1, centroX(hasta) - x1);

        // La flecha termina en el borde del círculo destino, no en su centro
        double x2 = centroX(hasta) - RADIO * Math.cos(angulo);
        double y2 = centroY(hasta) - RADIO * Math.sin(angulo);
        g2.draw(new Line2D.Double(x1, y1, x2, y2));

        // Punta: dos líneas cortas inclinadas hacia atrás
        int largoPunta = 9;
        double apertura = 0.4; // radianes
        g2.draw(new Line2D.Double(x2, y2,
                x2 - largoPunta * Math.cos(angulo - apertura), y2 - largoPunta * Math.sin(angulo - apertura)));
        g2.draw(new Line2D.Double(x2, y2,
                x2 - largoPunta * Math.cos(angulo + apertura), y2 - largoPunta * Math.sin(angulo + apertura)));
    }

    private int centroX(Nodo nodo) {
        return MARGEN + (nodo.columna - 1) * CELDA + CELDA / 2;
    }

    private int centroY(Nodo nodo) {
        return MARGEN + (nodo.fila - 1) * CELDA + CELDA / 2;
    }

    private void dibujarTextoCentrado(Graphics2D g2, String texto, int x, int y) {
        int ancho = g2.getFontMetrics().stringWidth(texto);
        int alto = g2.getFontMetrics().getAscent();
        g2.drawString(texto, x - ancho / 2, y + alto / 2 - 1);
    }
}
