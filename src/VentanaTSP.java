import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Random;

/**
 * Ventana principal.
 * Uso: 1) clic en la matriz para poner el inicio, 2) clic para poner cada nodo,
 *      3) ajustar parámetros y presionar "Resolver".
 */
public class VentanaTSP extends JFrame {

    private final PanelMatriz matriz = new PanelMatriz();

    // ---- Problema ----
    private final JRadioButton opcionInicio = new JRadioButton("Colocar inicio", true);
    private final JRadioButton opcionNodos = new JRadioButton("Colocar nodos");
    private final JSpinner campoNumNodos = new JSpinner(new SpinnerNumberModel(7, 1, 50, 1));
    private final JLabel etiquetaEstado = new JLabel();

    // ---- Parámetros del AG ----
    // Nota: la teoría sugiere mutación de 1-5%, pero con reducción elitista la población
    // converge muy rápido y se atora en rutas no óptimas. Con 20% encuentra el óptimo casi siempre.
    private final JSpinner campoPoblacion = new JSpinner(new SpinnerNumberModel(100, 2, 500, 2));
    private final JSpinner campoCruza = new JSpinner(new SpinnerNumberModel(80, 0, 100, 1));
    private final JSpinner campoMutacion = new JSpinner(new SpinnerNumberModel(20, 0, 100, 1));
    private final JSpinner campoMaxGeneraciones = new JSpinner(new SpinnerNumberModel(500, 1, 10000, 50));
    private final JSpinner campoConvergencia = new JSpinner(new SpinnerNumberModel(90, 1, 100, 1));

    // ---- Resultados ----
    private final JTextArea areaResultados = new JTextArea(16, 40);
    private VentanaDetalle ventanaDetalle; // la ventana con el detalle de la última ejecución

    public VentanaTSP() {
        super("TSP con Algoritmo Genético");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        add(matriz, BorderLayout.CENTER);
        add(crearPanelDerecho(), BorderLayout.EAST);

        matriz.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                alHacerClicEnMatriz(e.getX(), e.getY());
            }
        });

        actualizarEstado();
        pack();
        setLocationRelativeTo(null);
    }

    // =====================================================================
    //  Construcción de la interfaz
    // =====================================================================

    private JPanel crearPanelDerecho() {
        JPanel arriba = new JPanel();
        arriba.setLayout(new BoxLayout(arriba, BoxLayout.Y_AXIS));
        arriba.add(crearSeccionProblema());
        arriba.add(Box.createVerticalStrut(8));
        arriba.add(crearSeccionParametros());
        arriba.add(Box.createVerticalStrut(8));

        JButton botonResolver = new JButton("Resolver");
        botonResolver.setFont(botonResolver.getFont().deriveFont(Font.BOLD, 14f));
        botonResolver.addActionListener(e -> resolver());
        JPanel panelBoton = new JPanel(new GridLayout(1, 1));
        panelBoton.add(botonResolver);
        arriba.add(panelBoton);

        areaResultados.setEditable(false);
        areaResultados.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(areaResultados);
        scroll.setBorder(BorderFactory.createTitledBorder("3. Resultados"));

        JPanel derecho = new JPanel(new BorderLayout(0, 8));
        derecho.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
        derecho.add(arriba, BorderLayout.NORTH);
        derecho.add(scroll, BorderLayout.CENTER);
        return derecho;
    }

    private JPanel crearSeccionProblema() {
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(opcionInicio);
        grupo.add(opcionNodos);

        JButton botonAzar = new JButton("Nodos al azar");
        botonAzar.addActionListener(e -> colocarNodosAlAzar());
        JButton botonEjemplo = new JButton("Cargar ejemplo");
        botonEjemplo.addActionListener(e -> cargarEjemplo());
        JButton botonLimpiar = new JButton("Limpiar");
        botonLimpiar.addActionListener(e -> limpiar());

        JPanel rejilla = new JPanel(new GridLayout(0, 2, 5, 5));
        rejilla.add(opcionInicio);
        rejilla.add(opcionNodos);
        rejilla.add(new JLabel("Número de nodos:"));
        rejilla.add(campoNumNodos);
        rejilla.add(botonAzar);
        rejilla.add(botonEjemplo);
        rejilla.add(botonLimpiar);

        JPanel seccion = new JPanel(new BorderLayout(0, 5));
        seccion.setBorder(BorderFactory.createTitledBorder("1. Problema"));
        seccion.add(rejilla, BorderLayout.CENTER);
        seccion.add(etiquetaEstado, BorderLayout.SOUTH);
        return seccion;
    }

    private JPanel crearSeccionParametros() {
        JPanel seccion = new JPanel(new GridLayout(0, 2, 5, 5));
        seccion.setBorder(BorderFactory.createTitledBorder("2. Parámetros del AG"));
        seccion.add(new JLabel("Tamaño de población:"));
        seccion.add(campoPoblacion);
        seccion.add(new JLabel("% de cruza (PMX):"));
        seccion.add(campoCruza);
        seccion.add(new JLabel("% de mutación (DM):"));
        seccion.add(campoMutacion);
        seccion.add(new JLabel("Máx. generaciones:"));
        seccion.add(campoMaxGeneraciones);
        seccion.add(new JLabel("% de convergencia:"));
        seccion.add(campoConvergencia);
        return seccion;
    }

    // =====================================================================
    //  Acciones del usuario
    // =====================================================================

    private void alHacerClicEnMatriz(int x, int y) {
        int[] celda = matriz.celdaEn(x, y);
        if (celda == null) {
            return; // clic fuera de la matriz
        }
        int fila = celda[0];
        int columna = celda[1];

        if (matriz.estaOcupada(fila, columna)) {
            return;
        }

        if (opcionInicio.isSelected()) {
            matriz.setInicio(new Nodo(fila, columna));
            opcionNodos.setSelected(true); // lo siguiente normalmente es poner nodos
        } else {
            if (matriz.getNodos().size() >= numeroDeNodos()) {
                mostrarMensaje("Ya colocaste los " + numeroDeNodos() + " nodos.\n"
                        + "Aumenta el número de nodos o presiona Limpiar.");
                return;
            }
            matriz.agregarNodo(new Nodo(fila, columna));
        }
        actualizarEstado();
    }

    /** Coloca el número de nodos elegido en celdas libres al azar (y el inicio, si falta). */
    private void colocarNodosAlAzar() {
        Random azar = new Random();
        matriz.limpiarNodos();

        if (matriz.getInicio() == null) {
            matriz.setInicio(new Nodo(azar.nextInt(20) + 1, azar.nextInt(20) + 1));
        }
        while (matriz.getNodos().size() < numeroDeNodos()) {
            int fila = azar.nextInt(PanelMatriz.TAMANO) + 1;
            int columna = azar.nextInt(PanelMatriz.TAMANO) + 1;
            if (!matriz.estaOcupada(fila, columna)) {
                matriz.agregarNodo(new Nodo(fila, columna));
            }
        }
        opcionNodos.setSelected(true);
        actualizarEstado();
    }

    /** Carga el ejemplo del enunciado (solución esperada: 2-7-3-1-6-4-5). */
    private void cargarEjemplo() {
        matriz.limpiarNodos();
        matriz.setInicio(new Nodo(18, 2));
        matriz.agregarNodo(new Nodo(8, 12));   // 1
        matriz.agregarNodo(new Nodo(10, 5));   // 2
        matriz.agregarNodo(new Nodo(2, 20));   // 3
        matriz.agregarNodo(new Nodo(17, 19));  // 4
        matriz.agregarNodo(new Nodo(18, 7));   // 5
        matriz.agregarNodo(new Nodo(11, 16));  // 6
        matriz.agregarNodo(new Nodo(3, 4));    // 7
        campoNumNodos.setValue(7);
        opcionNodos.setSelected(true);
        actualizarEstado();
    }

    private void limpiar() {
        matriz.setInicio(null);
        matriz.limpiarNodos();
        opcionInicio.setSelected(true);
        areaResultados.setText("");
        actualizarEstado();
    }

    private void resolver() {
        Nodo inicio = matriz.getInicio();
        List<Nodo> nodos = matriz.getNodos();

        if (inicio == null) {
            mostrarMensaje("Primero coloca el inicio en la matriz.");
            return;
        }
        if (nodos.size() != numeroDeNodos()) {
            mostrarMensaje("Hay " + nodos.size() + " nodos colocados, pero elegiste " + numeroDeNodos() + ".\n"
                    + "Coloca los que faltan o ajusta el número de nodos.");
            return;
        }

        AlgoritmoGenetico ag = new AlgoritmoGenetico(
                inicio,
                nodos,
                (Integer) campoPoblacion.getValue(),
                (Integer) campoCruza.getValue() / 100.0,
                (Integer) campoMutacion.getValue() / 100.0,
                (Integer) campoMaxGeneraciones.getValue(),
                (Integer) campoConvergencia.getValue() / 100.0);

        int[] mejorRuta = ag.ejecutar();
        matriz.setRuta(mejorRuta);
        mostrarResultados(ag, inicio, nodos, mejorRuta);
        abrirVentanaDetalle(ag);
    }

    /** Abre la ventana con todo lo que pasó dentro del AG (cierra la de la ejecución anterior). */
    private void abrirVentanaDetalle(AlgoritmoGenetico ag) {
        if (ventanaDetalle != null) {
            ventanaDetalle.dispose();
        }
        ventanaDetalle = new VentanaDetalle(this, ag);
        ventanaDetalle.setVisible(true);
    }

    // =====================================================================
    //  Auxiliares
    // =====================================================================

    private void mostrarResultados(AlgoritmoGenetico ag, Nodo inicio, List<Nodo> nodos, int[] mejorRuta) {
        StringBuilder texto = new StringBuilder();
        texto.append("Inicio: ").append(inicio).append("\n");
        for (int i = 0; i < nodos.size(); i++) {
            texto.append(i + 1).append(": ").append(nodos.get(i)).append("\n");
        }
        texto.append("\nSOLUCIÓN: ").append(AlgoritmoGenetico.rutaATexto(mejorRuta)).append("\n");
        texto.append(String.format("Distancia total: %.2f%n", ag.distanciaTotal(mejorRuta)));
        texto.append(String.format("Generaciones: %d (convergencia %.0f%%)%n",
                ag.getGeneracionesUsadas(), ag.getConvergenciaFinal() * 100));
        texto.append("\n--- Evolución ---\n");
        texto.append(ag.getBitacora());

        areaResultados.setText(texto.toString());
        areaResultados.setCaretPosition(0); // mostrar desde arriba
    }

    private void actualizarEstado() {
        String inicio = matriz.getInicio() == null ? "sin colocar" : matriz.getInicio().toString();
        etiquetaEstado.setText("Inicio: " + inicio + "   |   Nodos: "
                + matriz.getNodos().size() + " / " + numeroDeNodos());
    }

    private int numeroDeNodos() {
        return (Integer) campoNumNodos.getValue();
    }

    private void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaTSP().setVisible(true));
    }
}
