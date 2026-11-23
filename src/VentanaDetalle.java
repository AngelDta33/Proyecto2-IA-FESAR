import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import java.awt.Font;

/**
 * Ventana que muestra por escrito lo que hizo el AG.
 * Una pestaña por generación (las primeras 5 y las últimas 5), más un resumen y la población final.
 */
public class VentanaDetalle extends JDialog {

    public VentanaDetalle(JFrame ventanaPrincipal, AlgoritmoGenetico ag) {
        super(ventanaPrincipal, "Detalle de la ejecución del AG", false);

        JTabbedPane pestanas = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        pestanas.addTab("Resumen", crearAreaDeTexto(ag.getResumen()));
        for (AlgoritmoGenetico.DetalleGeneracion generacion : ag.getDetalles()) {
            pestanas.addTab("Gen " + generacion.numero, crearAreaDeTexto(generacion.texto));
        }
        pestanas.addTab("Final", crearAreaDeTexto(ag.getTextoFinal()));

        add(pestanas);
        setSize(1000, 700);
        setLocationRelativeTo(ventanaPrincipal);
    }

    private JScrollPane crearAreaDeTexto(String texto) {
        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setCaretPosition(0); // mostrar desde arriba
        return new JScrollPane(area);
    }
}
